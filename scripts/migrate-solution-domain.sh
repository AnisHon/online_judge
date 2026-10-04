#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
command -v python3 >/dev/null || { echo 'python3 is required' >&2; exit 1; }
command -v mysql >/dev/null || { echo 'mysql client is required' >&2; exit 1; }
exec python3 "$SCRIPT_DIR/migrate_solution_domain.py" "$@"
