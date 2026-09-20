#!/usr/bin/env bash
set -Eeuo pipefail

: "${MINIO_URL:?MINIO_URL is required}"
: "${MINIO_ACCESS_KEY:?MINIO_ACCESS_KEY is required}"
: "${MINIO_SECRET_KEY:?MINIO_SECRET_KEY is required}"

# GoJudge starts a second mount namespace for every run. Keep the locale and Java
# home explicit because the child namespace does not inherit the host shell setup.
export LANG="${LANG:-en_US.UTF-8}"
export LANGUAGE="${LANGUAGE:-en_US:en}"
export LC_ALL="${LC_ALL:-en_US.UTF-8}"
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}"
export PATH="${JAVA_HOME}/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"

printf '%s:%s\n' "${MINIO_ACCESS_KEY}" "${MINIO_SECRET_KEY}" > /etc/passwd-s3fs
chmod 600 /etc/passwd-s3fs
mkdir -p /mnt/minio/case /tmp/cache

if ! mountpoint -q /mnt/minio/case; then
  s3fs case /mnt/minio/case \
    -o passwd_file=/etc/passwd-s3fs \
    -o url="${MINIO_URL}" \
    -o allow_other \
    -o use_path_request_style \
    -o use_cache=/tmp/cache \
    -o parallel_count=10
fi

mountpoint -q /mnt/minio/case || {
  echo '判题沙箱无法挂载 MinIO case 存储，已停止启动以避免产生误导性的判题错误。' >&2
  exit 1
}

test -r "${JAVA_HOME}/conf/security/java.security" || {
  echo "Java security 配置不存在: ${JAVA_HOME}/conf/security/java.security" >&2
  exit 1
}

exec /opt/go-judge --http-addr=:5050 --silent=true --file-timeout=5m
