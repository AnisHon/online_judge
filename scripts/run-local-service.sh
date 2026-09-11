#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SERVICE_NAME="${1:-}"

case "$SERVICE_NAME" in
  user-service|content-service|problem-service|judge-server|gateway-server) ;;
  *)
    echo '用法：./scripts/run-local-service.sh {user-service|content-service|problem-service|judge-server|gateway-server}' >&2
    exit 2
    ;;
esac

command -v mvn >/dev/null || {
  echo '未找到本机 Maven。请在系统中安装 Maven 3.9+，或在 IDEA 中直接运行对应 Spring Boot 模块。' >&2
  exit 1
}
[[ -f "$ROOT_DIR/.env.dev" ]] || {
  echo '缺少 .env.dev，请先执行 ./scripts/start-dev-infra.sh。' >&2
  exit 1
}

cd "$ROOT_DIR"
set -a
# shellcheck disable=SC1091
source .env.dev
set +a

# 开发 Compose 直接映射项目原有默认端口；Spring Boot 会将这些环境变量绑定到对应属性。
export SPRING_CLOUD_NACOS_SERVER_ADDR=127.0.0.1:8848
export OJ_DB_HOST=127.0.0.1
export OJ_DB_PORT=3306
export OJ_DB_PW="$MYSQL_ROOT_PASSWORD"
export SPRING_REDIS_HOST=127.0.0.1
export OJ_REDIS_HOST=127.0.0.1
export SPRING_REDIS_PORT=6379
export OJ_MQ_HOST=127.0.0.1
export SPRING_RABBITMQ_HOST=127.0.0.1
export SPRING_RABBITMQ_PORT=5672
export OJ_MQ_V_HOST=/judge
export OJ_MQ_USERNAME="$RABBITMQ_DEFAULT_USER"
export OJ_MQ_PASSWORD="$RABBITMQ_DEFAULT_PASS"
export OJ_MINIO_URL=http://127.0.0.1:9000
export JUDGE_URL=http://127.0.0.1:5050

if [[ "$SERVICE_NAME" == gateway-server ]]; then
  export SERVER_PORT=18000
fi

echo "正在本机前台启动 ${SERVICE_NAME}；按 Ctrl-C 停止该服务。"
echo "先编译该模块及其本地依赖。"
mvn -pl "$SERVICE_NAME" -am -DskipTests package
cd "$ROOT_DIR/$SERVICE_NAME"
exec mvn spring-boot:run
