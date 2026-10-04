#!/usr/bin/env bash
set -Eeuo pipefail

MIGRATION_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MIGRATION_MANIFEST="$MIGRATION_DIR/manifest.tsv"

migration_manifest_validate() {
  [[ -f "$MIGRATION_MANIFEST" ]] || { echo "Missing migration manifest: $MIGRATION_MANIFEST" >&2; return 1; }
  local line_no=0 id file expected mode phase schema order actual
  declare -A seen=()
  while IFS=$'\t' read -r id file expected mode phase schema order; do
    line_no=$((line_no + 1))
    [[ -z "$id" || "$id" == \#* ]] && continue
    [[ -n "$order" && "$id" != *$'\r'* ]] || { echo "Invalid manifest row $line_no" >&2; return 1; }
    [[ "$file" == "$(basename "$file")" && "$file" == V*.sql ]] || { echo "Unsafe migration path on row $line_no" >&2; return 1; }
    [[ "$expected" =~ ^[a-f0-9]{64}$ ]] || { echo "Missing/invalid SHA-256 for $file" >&2; return 1; }
    [[ "$mode" == ALL || "$mode" == LEGACY || "$mode" == FRESH ]] || { echo "Invalid source mode on row $line_no" >&2; return 1; }
    [[ "$phase" == standard || "$phase" == source-normalize || "$phase" == content-prepare || "$phase" == copy ]] || { echo "Invalid phase on row $line_no" >&2; return 1; }
    [[ "$order" =~ ^[0-9]+$ ]] || { echo "Invalid order on row $line_no" >&2; return 1; }
    [[ -z "${seen[$id]+x}" ]] || { echo "Duplicate migration id: $id" >&2; return 1; }
    seen[$id]=1
    [[ -f "$MIGRATION_DIR/$file" ]] || { echo "Manifest file is missing: $file" >&2; return 1; }
    actual="$(sha256sum "$MIGRATION_DIR/$file" | awk '{print $1}')"
    [[ "$actual" == "$expected" ]] || { echo "Migration checksum mismatch: $file" >&2; return 1; }
  done < "$MIGRATION_MANIFEST"
}

migration_manifest_files() {
  local requested_phase="$1" requested_mode="$2"
  local id file checksum mode phase schema order
  while IFS=$'\t' read -r id file checksum mode phase schema order; do
    [[ -z "$id" || "$id" == \#* ]] && continue
    [[ "$phase" == "$requested_phase" ]] || continue
    [[ "$mode" == ALL || "$mode" == "$requested_mode" ]] || continue
    printf '%s\n' "$file"
  done < "$MIGRATION_MANIFEST"
}
