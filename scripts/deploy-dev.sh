#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

COMPOSE=(docker compose -p oj-dev -f docker-compose.yaml -f docker-compose.dev.yaml --env-file .env.dev)

if [[ ! -f .env.dev ]]; then
  cp .env.dev.example .env.dev
  echo '已生成 .env.dev，请按需修改开发环境密码后重新执行 ./scripts/deploy-dev.sh'
  exit 1
fi

command -v docker >/dev/null || { echo '需要 Docker' >&2; exit 1; }
docker compose version >/dev/null || { echo '需要 Docker Compose v2' >&2; exit 1; }
command -v pnpm >/dev/null || { echo '需要 pnpm；此脚本不会调用 npm' >&2; exit 1; }
command -v curl >/dev/null || { echo '需要 curl' >&2; exit 1; }

echo '[1/4] 编译后端'
docker run --rm -v "$ROOT_DIR:/workspace" -w /workspace \
  maven:3.9-eclipse-temurin-17 mvn -B -DskipTests package

echo '[2/4] 编译前端'
(cd oj-vue && pnpm install --frozen-lockfile && pnpm run build)

echo '[3/4] 构建并启动开发环境（纯 HTTP）'
"${COMPOSE[@]}" up -d --build

echo '[4/4] 等待 Nacos 并导入配置'
for _ in $(seq 1 60); do
  curl -fsS "http://127.0.0.1:${DEV_NACOS_PORT:-18848}/nacos/" >/dev/null 2>&1 && break
  sleep 2
done
curl -fsS "http://127.0.0.1:${DEV_NACOS_PORT:-18848}/nacos/" >/dev/null || { "${COMPOSE[@]}" ps; exit 1; }
for file in resources/config/*.yaml; do
  curl -fsS -X POST "http://127.0.0.1:${DEV_NACOS_PORT:-18848}/nacos/v1/cs/configs" \
    --data-urlencode "dataId=$(basename "$file")" \
    --data-urlencode 'group=DEFAULT_GROUP' --data-urlencode 'type=yaml' \
    --data-urlencode "content@$file" >/dev/null
done

"${COMPOSE[@]}" ps
echo "开发环境已启动：http://127.0.0.1:${DEV_WEB_PORT:-8080}/（无 HTTPS、无 SSL 证书）"
