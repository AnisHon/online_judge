#!/usr/bin/env bash
# Read-only, fail-closed preflight. Never applies migrations, policies or cache cleanup.
set -Eeuo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$ROOT_DIR/resources/sql/migration/manifest-lib.sh"
migration_manifest_validate
if [[ "${1:-}" == --manifest-only && $# == 1 ]]; then
  echo 'PASS migration manifest/checksums (no database contacted)'; exit 0
fi
[[ $# == 0 ]] || { echo 'Usage: check-community-deployment.sh [--manifest-only]' >&2; exit 2; }
for command_name in mysql curl jq docker; do command -v "$command_name" >/dev/null || { echo "Missing prerequisite: $command_name" >&2; exit 2; }; done
required=(OJ_PREFLIGHT_MYSQL_HOST OJ_PREFLIGHT_MYSQL_PORT OJ_PREFLIGHT_MYSQL_USER OJ_PREFLIGHT_MYSQL_PASSWORD OJ_PREFLIGHT_RABBIT_URL OJ_PREFLIGHT_RABBIT_USER OJ_PREFLIGHT_RABBIT_PASSWORD OJ_PREFLIGHT_RABBIT_VHOST)
for setting in "${required[@]}"; do [[ -n "${!setting:-}" ]] || { echo "Missing preflight setting: $setting" >&2; exit 2; }; done
[[ "$OJ_PREFLIGHT_MYSQL_PORT" =~ ^[0-9]+$ ]] || exit 2
USER_SCHEMA="${OJ_PREFLIGHT_USER_SCHEMA:-db_user}"
PROBLEM_SCHEMA="${OJ_PREFLIGHT_PROBLEM_SCHEMA:-db_problem}"
CONTENT_SCHEMA="${OJ_PREFLIGHT_CONTENT_SCHEMA:-db_content}"
[[ "$USER_SCHEMA" == db_user || "$USER_SCHEMA" == oj_it_user ]] || exit 2
[[ "$PROBLEM_SCHEMA" == db_problem || "$PROBLEM_SCHEMA" == oj_it_problem ]] || exit 2
[[ "$CONTENT_SCHEMA" == db_content || "$CONTENT_SCHEMA" == oj_it_content ]] || exit 2
query() {
  MYSQL_PWD="$OJ_PREFLIGHT_MYSQL_PASSWORD" mysql --protocol=tcp -h"$OJ_PREFLIGHT_MYSQL_HOST" -P"$OJ_PREFLIGHT_MYSQL_PORT" -u"$OJ_PREFLIGHT_MYSQL_USER" --batch --skip-column-names --default-character-set=utf8mb4 --connect-timeout=5 -e "$1" 2>/dev/null || { echo 'FAIL SQL preflight (schema/permissions/connectivity); no migrations performed' >&2; return 1; }
}
version="$(query 'SELECT VERSION()')"
[[ "$version" == 8.* ]] || { echo 'FAIL MySQL 8 required' >&2; exit 1; }
[[ "$(query "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name IN ('$USER_SCHEMA','$PROBLEM_SCHEMA','$CONTENT_SCHEMA') AND default_character_set_name='utf8mb4'")" == 3 ]] || { echo 'FAIL missing schema/utf8mb4' >&2; exit 1; }
assert_zero() { local count; count="$(query "$2")"; [[ "$count" == 0 ]] || { echo "FAIL $1 count=$count" >&2; exit 1; }; echo "PASS $1"; }
assert_zero active_contests "SELECT COUNT(*) FROM $PROBLEM_SCHEMA.contest WHERE type=0 AND del_flag=0 AND start_time<=NOW() AND end_time>NOW()"
assert_zero pending_judges "SELECT COUNT(*) FROM $PROBLEM_SCHEMA.submit_log WHERE status IN ('queue','compiling','running')"
assert_zero unapplied_results "SELECT COUNT(*) FROM $PROBLEM_SCHEMA.submit_log WHERE result_applied=0"
assert_zero pending_attempts "SELECT COUNT(*) FROM $PROBLEM_SCHEMA.contest_attempt WHERE state='PENDING'"
assert_zero invalid_schedules "SELECT COUNT(*) FROM $PROBLEM_SCHEMA.contest WHERE del_flag=0 AND (start_time IS NULL OR end_time IS NULL OR start_time>=end_time)"
assert_zero cross_domain_foreign_keys "SELECT COUNT(*) FROM information_schema.key_column_usage WHERE table_schema='$CONTENT_SCHEMA' AND referenced_table_schema IS NOT NULL AND referenced_table_schema<>'$CONTENT_SCHEMA'"
assert_zero incompatible_text_collations "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema IN ('$USER_SCHEMA','$PROBLEM_SCHEMA','$CONTENT_SCHEMA') AND character_set_name='utf8mb4' AND collation_name NOT IN ('utf8mb4_unicode_ci','utf8mb4_0900_ai_ci')"
# Explicit indexes, not only table names; missing indexes fail rather than quietly broad-scanning.
for entry in "$USER_SCHEMA:user_follow:idx_user_follow_followee" "$USER_SCHEMA:user_notification:idx_user_notification_recipient_unread" "$CONTENT_SCHEMA:solution_comment:idx_solution_comment_solution_root_created" "$PROBLEM_SCHEMA:contest_rank_entry:idx_contest_rank_entry_page"; do
  IFS=: read -r schema table_name index_name <<< "$entry"
  count="$(query "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema='$schema' AND table_name='$table_name' AND index_name='$index_name'")"
  [[ "$count" -gt 0 ]] || { echo "FAIL missing index $schema.$table_name.$index_name" >&2; exit 1; }
done
deadline_column="$(query "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='$PROBLEM_SCHEMA' AND table_name='supplement_contest' AND column_name='deadline'")"
[[ "$deadline_column" == 1 ]] || { echo 'FAIL supplement schema is not understood' >&2; exit 1; }
assert_zero active_homework_extensions "SELECT COUNT(*) FROM $PROBLEM_SCHEMA.supplement_contest s JOIN $PROBLEM_SCHEMA.contest c ON c.contest_id=s.contest_id JOIN $PROBLEM_SCHEMA.user_contest uc ON uc.contest_id=s.contest_id AND uc.user_id=s.user_id LEFT JOIN $PROBLEM_SCHEMA.user_submit us ON us.contest_id=s.contest_id AND us.user_id=s.user_id WHERE c.type=1 AND c.del_flag=0 AND c.end_time<=NOW() AND s.deadline>NOW() AND us.user_id IS NULL"
for service in user-service problem-service content-service judge-server; do
  bindings="$(docker inspect --format '{{json .NetworkSettings.Ports}}' "$service" 2>/dev/null)" || { echo "FAIL cannot inspect private service $service" >&2; exit 1; }
  jq -e '[.[]? // [] | .[] | select(.HostIp != "127.0.0.1" and .HostIp != "::1")] | length == 0' <<< "$bindings" >/dev/null || { echo "FAIL publicly bound internal service $service" >&2; exit 1; }
done
[[ "$OJ_PREFLIGHT_RABBIT_URL" =~ ^https?://[^/]+$ ]] || { echo 'FAIL Rabbit management URL must contain only origin' >&2; exit 2; }
vhost="$(jq -rn --arg value "$OJ_PREFLIGHT_RABBIT_VHOST" '$value|@uri')"
user="$(jq -rn --arg value "$OJ_PREFLIGHT_RABBIT_USER" '$value|@uri')"
auth_dir="$(mktemp -d)"
chmod 0700 "$auth_dir"
trap 'rm -rf "$auth_dir"' EXIT
auth_file="$auth_dir/curl.conf"
printf 'user = %s\n' "$(jq -Rn --arg value "$OJ_PREFLIGHT_RABBIT_USER:$OJ_PREFLIGHT_RABBIT_PASSWORD" '$value|@json')" > "$auth_file"
chmod 0600 "$auth_file"
rabbit_get() { curl --silent --show-error --fail --max-time 10 --config "$auth_file" "$OJ_PREFLIGHT_RABBIT_URL/api/$1"; }
permissions="$(rabbit_get "permissions/$vhost/$user")"
jq -e '.configure!="" and .write!="" and .read!=""' <<< "$permissions" >/dev/null || { echo 'FAIL Rabbit declaration/read/write permissions' >&2; exit 1; }
policies="$(rabbit_get "policies/$vhost")"
jq -e 'any(.[]; .pattern=="^judge-info-queue$" and .definition["dead-letter-exchange"]=="judge-exchange.dlx.v1" and .definition["dead-letter-routing-key"]=="judge-info.dead.v1" and ((.["apply-to"]=="queues") or (.["apply-to"]=="all")))' <<< "$policies" >/dev/null || { echo 'FAIL missing exact judge dead-letter policy' >&2; exit 1; }
echo 'PASS deployment preflight. This report grants no migration/deployment authorization.'
