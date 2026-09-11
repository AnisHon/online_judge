#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
command -v pnpm >/dev/null || { echo '需要 pnpm；本项目不使用 npm。' >&2; exit 1; }

cd "$ROOT_DIR/oj-vue"
echo '正在本机前台启动前端；按 Ctrl-C 停止 Vite。'
exec pnpm run dev -- --host 127.0.0.1
