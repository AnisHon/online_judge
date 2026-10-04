#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$SCRIPT_DIR"

# Invoking this script alone never mutates a server. Explicit execution still requires
# a maintenance window and a successful read-only preflight before the first write.
case "${1:---preflight-only}" in
  --preflight-only) exec bash "$ROOT_DIR/scripts/check-community-deployment.sh" ;;
  --execute)
    [[ $# == 1 && "${OJ_DEPLOY_MAINTENANCE_CONFIRMED:-}" == 1 ]] || {
      echo 'Explicit --execute and OJ_DEPLOY_MAINTENANCE_CONFIRMED=1 are required.' >&2; exit 2;
    }
    bash "$ROOT_DIR/scripts/check-community-deployment.sh"
    ;;
  *) echo 'Usage: deploy.sh [--preflight-only|--execute]' >&2; exit 2 ;;
esac

required=(docker docker-compose gzip)
for command_name in "${required[@]}"; do
  command -v "$command_name" >/dev/null || {
    echo "Missing required command: $command_name" >&2
    exit 1
  }
done

for container_name in oj-mysql oj-redis oj-mq oj-minio; do
  docker inspect "$container_name" >/dev/null 2>&1 || {
    echo "Required infrastructure container is missing: $container_name" >&2
    exit 1
  }
done

# 沙箱镜像独立发布。默认复用现有沙箱镜像；需要升级沙箱时可在执行脚本前
# 显式传入 OJ_SANDBOX_IMAGE=<完整镜像名>，不会因为应用的新 RELEASE_TAG
# 触发不必要的远程拉取。
if [[ -z "${OJ_SANDBOX_IMAGE:-}" ]]; then
  OJ_SANDBOX_IMAGE="$(docker inspect -f '{{.Config.Image}}' oj-sandbox 2>/dev/null || true)"
fi
export OJ_SANDBOX_IMAGE
[[ -n "$OJ_SANDBOX_IMAGE" ]] || {
  echo "Unable to determine sandbox image; set OJ_SANDBOX_IMAGE explicitly" >&2
  exit 1
}

mkdir -p backups /opt/nacos/logs /opt/logs/{judge-server,problem-service,user-service,content-service}
chmod 0750 backups
install -d -m 0755 /opt/nginx/conf.d
install -m 0644 nginx/default.conf /opt/nginx/conf.d/default.conf

python3 seed-env.py .env "${RELEASE_TAG:?RELEASE_TAG is required}"

backup_file="backups/mysql-before-${RELEASE_TAG}.sql.gz"
docker exec oj-mysql sh -c \
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysqldump -uroot --single-transaction --routines --events --all-databases' \
  | gzip -1 > "$backup_file"
gzip -t "$backup_file"
echo "Database backup created: $backup_file"

# Nacos configuration lives in MySQL. Recreating this container removes its
# multi-gigabyte writable protocol-log layer while preserving configuration.
docker rm -f oj-nacos >/dev/null 2>&1 || true
docker-compose -p online_judge --env-file .env -f docker-compose.yml up -d --no-deps oj-nacos

for attempt in $(seq 1 60); do
  if curl -fsS http://127.0.0.1:8848/nacos/ >/dev/null 2>&1; then
    break
  fi
  if [ "$attempt" -eq 60 ]; then
    docker logs --tail 120 oj-nacos >&2
    exit 1
  fi
  sleep 2
done

for config_file in "$ROOT_DIR"/resources/config/*.yaml; do
  curl -fsS -X POST http://127.0.0.1:8848/nacos/v1/cs/configs \
    --data-urlencode "dataId=$(basename "$config_file")" \
    --data-urlencode 'group=DEFAULT_GROUP' \
    --data-urlencode 'type=yaml' \
    --data-urlencode "content@$config_file" >/dev/null
done
echo "Nacos configuration synchronized."

source "$ROOT_DIR/resources/sql/migration/manifest-lib.sh"
migration_manifest_validate
SOURCE_MODE="${OJ_MIGRATION_SOURCE_MODE:-LEGACY}"
[[ "$SOURCE_MODE" == FRESH || "$SOURCE_MODE" == LEGACY ]] || exit 2
for schema_phase in standard content-prepare; do
while IFS= read -r migration_file; do
  [[ -n "$migration_file" ]] || continue
  echo "Applying standard database migration: $migration_file"
  docker exec -i oj-mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot' \
    < "$MIGRATION_DIR/$migration_file"
done < <(migration_manifest_files "$schema_phase" "$SOURCE_MODE")
done
echo "All standard manifest migrations applied. Content prepare/copy remains an explicit maintenance operation."

services=(oj-sandbox judge-server problem-service user-service content-service gateway-server oj-vue)
for service in "${services[@]}"; do
  docker rm -f "$service" >/dev/null 2>&1 || true
done
docker-compose -p online_judge --env-file .env -f docker-compose.yml up -d --no-deps "${services[@]}"

for attempt in $(seq 1 90); do
  if curl -kfsS https://127.0.0.1/ >/dev/null 2>&1; then
    break
  fi
  if [ "$attempt" -eq 90 ]; then
    docker-compose -p online_judge --env-file .env -f docker-compose.yml ps
    exit 1
  fi
  sleep 2
done

docker-compose -p online_judge --env-file .env -f docker-compose.yml ps
echo "Deployment completed: ${RELEASE_TAG}"
