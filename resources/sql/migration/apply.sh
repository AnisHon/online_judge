#!/usr/bin/env bash
set -Eeuo pipefail

# 在线上已有数据卷执行：MYSQL_HOST=127.0.0.1 MYSQL_ROOT_PASSWORD=... ./apply.sh
MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD is required}"

mysql --protocol=tcp -h"$MYSQL_HOST" -P"$MYSQL_PORT" -uroot -p"$MYSQL_ROOT_PASSWORD" \
  < "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/V20260912__judge_pipeline.sql"

mysql --protocol=tcp -h"$MYSQL_HOST" -P"$MYSQL_PORT" -uroot -p"$MYSQL_ROOT_PASSWORD" \
  < "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/V20260920__judge_admin_pages.sql"

mysql --protocol=tcp -h"$MYSQL_HOST" -P"$MYSQL_PORT" -uroot -p"$MYSQL_ROOT_PASSWORD" \
  < "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/V20260921__user_profile.sql"

mysql --protocol=tcp -h"$MYSQL_HOST" -P"$MYSQL_PORT" -uroot -p"$MYSQL_ROOT_PASSWORD" \
  < "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/V20260922__backend_access_permission.sql"

echo "OJ judge pipeline, admin pages, user profile and backend access migrations applied."
