#!/usr/bin/env bash
set -Eeuo pipefail

# 定义要检测的文件路径
FILE_PATH="/var/lib/mysql/.oj-initialized"

# 数据卷已初始化时，必须只启动一次 MySQL。此前先后台启动、再 exec 启动，
# 会让两个 mysqld 同时竞争 ibdata1 锁。
if [ -f "$FILE_PATH" ]; then
  exec docker-entrypoint.sh mysqld
fi

# 首次初始化：只启动一个后台 mysqld，导入完成后等待该进程维持容器生命周期。
echo "Starting MySQL service..."
docker-entrypoint.sh mysqld &
MYSQL_PID=$!

stop_mysql() {
  kill -TERM "$MYSQL_PID" 2>/dev/null || true
  wait "$MYSQL_PID" 2>/dev/null || true
}
trap stop_mysql INT TERM

# 等待 MySQL 服务启动
echo "Waiting for MySQL to be ready..."
while ! mysqladmin ping -h"127.0.0.1" --silent; do
  sleep 1
done

echo "MySQL is ready. Running the SQL script..."
mysql -u root -p"$MYSQL_ROOT_PASSWORD" < "db_problem.sql"
mysql -u root -p"$MYSQL_ROOT_PASSWORD" < "db_content.sql"
mysql -u root -p"$MYSQL_ROOT_PASSWORD" < "db_user.sql"
mysql -u root -p"$MYSQL_ROOT_PASSWORD" < "nacos.sql"
echo "database initialization succeeded"

# 创建文件
touch "$FILE_PATH"

rm -f /opt/db_problem.sql
rm -f /opt/db_user.sql
rm -f /opt/nacos.sql
rm -f /opt/db_content.sql

# 由唯一的 mysqld 进程维持容器生命周期。
wait "$MYSQL_PID"
