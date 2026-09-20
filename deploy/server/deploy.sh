#!/usr/bin/env bash
set -Eeuo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

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

for config_file in config/*.yaml; do
  curl -fsS -X POST http://127.0.0.1:8848/nacos/v1/cs/configs \
    --data-urlencode "dataId=$(basename "$config_file")" \
    --data-urlencode 'group=DEFAULT_GROUP' \
    --data-urlencode 'type=yaml' \
    --data-urlencode "content@$config_file" >/dev/null
done
echo "Nacos configuration synchronized."

docker exec -i oj-mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot' \
  < migration/V20260912__judge_pipeline.sql
echo "Database migration applied."

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
