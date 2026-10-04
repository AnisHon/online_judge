#!/usr/bin/env bash
set -Eeuo pipefail

MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${OJ_MIGRATION_MYSQL_USER:-root}"
MYSQL_PASSWORD="${OJ_MIGRATION_MYSQL_PASSWORD:-${MYSQL_ROOT_PASSWORD:-}}"
SOURCE_MODE="${OJ_MIGRATION_SOURCE_MODE:-LEGACY}"
PHASE="standard"

while (($#)); do
  case "$1" in
    --phase)
      [[ $# -ge 2 ]] || { echo '--phase requires a value' >&2; exit 2; }
      PHASE="$2"; shift 2 ;;
    --source-mode)
      [[ $# -ge 2 ]] || { echo '--source-mode requires LEGACY or FRESH' >&2; exit 2; }
      SOURCE_MODE="$2"; shift 2 ;;
    *) echo "Unknown option: $1" >&2; exit 2 ;;
  esac
done

[[ "$PHASE" == standard || "$PHASE" == content-prepare ]] || {
  echo 'apply.sh only runs schema phases (standard/content-prepare); copy/verify/cutover require the explicit maintenance tool.' >&2
  exit 2
}
[[ "$SOURCE_MODE" == LEGACY || "$SOURCE_MODE" == FRESH ]] || { echo 'Invalid source mode' >&2; exit 2; }
[[ -n "$MYSQL_PASSWORD" ]] || { echo 'OJ_MIGRATION_MYSQL_PASSWORD or MYSQL_ROOT_PASSWORD is required' >&2; exit 2; }
command -v mysql >/dev/null || { echo 'mysql client is required' >&2; exit 1; }

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/manifest-lib.sh"
migration_manifest_validate

while IFS= read -r migration_file; do
  [[ -n "$migration_file" ]] || continue
  echo "Applying $PHASE migration: $migration_file"
  MYSQL_PWD="$MYSQL_PASSWORD" mysql --protocol=tcp -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" \
    --default-character-set=utf8mb4 < "$MIGRATION_DIR/$migration_file"
done < <(migration_manifest_files "$PHASE" "$SOURCE_MODE")

echo "Manifest phase $PHASE applied. Copy/verify/cutover require the dedicated maintenance tool."
