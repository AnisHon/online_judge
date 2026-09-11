#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
echo '开发环境现已改为本机构建：此兼容入口只启动 Docker 基础设施。'
exec "$ROOT_DIR/scripts/start-dev-infra.sh"
