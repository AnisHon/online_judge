#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

command -v docker >/dev/null || { echo '需要 Docker。' >&2; exit 1; }

ENV_FILE="${JUDGE_ENV_FILE:-}"
if [[ -z "$ENV_FILE" ]]; then
  if [[ -f .env.dev ]]; then
    ENV_FILE=.env.dev
  elif [[ -f .env ]]; then
    ENV_FILE=.env
  fi
fi
if [[ -n "$ENV_FILE" && -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi

IMAGE="${JUDGE_ENVIRONMENT_IMAGE:-oj-judge-environment:2026.09}"
FORCE=false
if [[ "${1:-}" == "--force" ]]; then
  FORCE=true
fi

if [[ "$FORCE" != true ]] && docker image inspect "$IMAGE" >/dev/null 2>&1; then
  echo "判题环境镜像已存在：$IMAGE（跳过编译器和运行时重建）"
  exit 0
fi

echo "构建可复用判题环境镜像：$IMAGE"
docker build \
  --tag "$IMAGE" \
  --file resources/judge-environment/Dockerfile \
  resources/sandbox
echo "判题环境镜像已准备：$IMAGE"
