#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if [[ ! -f .env.dev ]]; then
  cp .env.dev.example .env.dev
  echo '已生成 .env.dev。请先修改其中的开发环境密码，再重新执行本脚本。' >&2
  exit 1
fi

command -v docker >/dev/null || { echo '需要 Docker。' >&2; exit 1; }
command -v curl >/dev/null || { echo '需要 curl。' >&2; exit 1; }

# .env.dev 是本项目维护的 KEY=VALUE 配置；加载后用于端口和服务凭据。
set -a
# shellcheck disable=SC1091
source .env.dev
set +a

NACOS_HTTP_PORT="${DEV_NACOS_PORT:-8848}"

COMPOSE=(docker compose -p oj-dev -f docker-compose.dev.yaml --env-file .env.dev)
INFRA=(oj-mysql oj-redis oj-mq oj-minio oj-nacos oj-sandbox)

echo '[1/4] 准备可复用判题环境镜像（已存在时跳过）'
JUDGE_ENV_FILE=.env.dev "$ROOT_DIR/scripts/build-judge-environment.sh"

echo '[2/4] 启动常驻基础设施（不会启动任何 Java 服务或前端容器）'
"${COMPOSE[@]}" up -d "${INFRA[@]}"

echo '[3/4] 等待 MySQL 并应用开发环境数据库迁移'
for _ in $(seq 1 60); do
  if docker exec -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" oj-dev-mysql mysqladmin ping -uroot --silent >/dev/null 2>&1; then
    break
  fi
  sleep 2
done
docker exec -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" oj-dev-mysql mysqladmin ping -uroot --silent >/dev/null || {
  "${COMPOSE[@]}" ps oj-mysql
  echo 'MySQL 未在预期时间内就绪。' >&2
  exit 1
}
docker exec -i -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" oj-dev-mysql mysql -uroot \
  < "$ROOT_DIR/resources/sql/migration/V20260912__judge_pipeline.sql"

echo '[4/4] 等待 Nacos 并同步开发配置'
for _ in $(seq 1 60); do
  curl -fsS "http://127.0.0.1:${NACOS_HTTP_PORT}/nacos/" >/dev/null 2>&1 && break
  sleep 2
done
curl -fsS "http://127.0.0.1:${NACOS_HTTP_PORT}/nacos/" >/dev/null || {
  "${COMPOSE[@]}" ps
  echo 'Nacos 未在预期时间内就绪。' >&2
  exit 1
}

for file in resources/config/*.yaml; do
  curl -fsS -X POST "http://127.0.0.1:${NACOS_HTTP_PORT}/nacos/v1/cs/configs" \
    --data-urlencode "dataId=$(basename "$file")" \
    --data-urlencode 'group=DEFAULT_GROUP' \
    --data-urlencode 'type=yaml' \
    --data-urlencode "content@$file" >/dev/null
done

"${COMPOSE[@]}" ps "${INFRA[@]}"
echo '基础设施会由 Docker 长期保持；本机服务请另开终端运行 ./scripts/run-local-service.sh <模块名>。'
