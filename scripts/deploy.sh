#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

[[ -f .env ]] || { cp .env.example .env; echo "已生成 .env，请修改密码后重新执行 ./scripts/deploy.sh"; exit 1; }
command -v docker >/dev/null || { echo "需要 Docker" >&2; exit 1; }
docker compose version >/dev/null || { echo "需要 Docker Compose v2" >&2; exit 1; }
command -v pnpm >/dev/null || { echo "需要 pnpm；此脚本不会调用 npm" >&2; exit 1; }
command -v curl >/dev/null || { echo "需要 curl" >&2; exit 1; }

echo '[1/4] 编译后端'
docker run --rm -v "$ROOT_DIR:/workspace" -w /workspace \
  maven:3.9-eclipse-temurin-17 mvn -B -DskipTests package

echo '[2/4] 编译前端'
(cd oj-vue && pnpm install --no-frozen-lockfile && pnpm run build-only)

echo '[3/5] 准备可复用判题环境镜像（已存在时跳过）'
JUDGE_ENV_FILE=.env "$ROOT_DIR/scripts/build-judge-environment.sh"

echo '[4/5] 构建并启动服务'
docker compose up -d --build

echo '[5/5] 等待 Nacos 并导入配置'
for _ in $(seq 1 60); do
  curl -fsS http://127.0.0.1:8848/nacos/ >/dev/null 2>&1 && break
  sleep 2
done
curl -fsS http://127.0.0.1:8848/nacos/ >/dev/null || { docker compose ps; exit 1; }
for file in resources/config/*.yaml; do
  curl -fsS -X POST 'http://127.0.0.1:8848/nacos/v1/cs/configs' \
    --data-urlencode "dataId=$(basename "$file")" \
    --data-urlencode 'group=DEFAULT_GROUP' --data-urlencode 'type=yaml' \
    --data-urlencode "content@=$file" >/dev/null
done

docker compose ps
echo '部署完成。前端：http://服务器IP/；MinIO 控制台端口见 .env'
