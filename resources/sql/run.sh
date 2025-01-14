# 启动 MySQL 服务
echo "Starting MySQL service..."
#service mysql start
docker-entrypoint.sh mysqld &

# 定义要检测的文件路径
FILE_PATH="/opt/initialized"

## 检测文件是否存在
#if [ -f "$FILE_PATH" ]; then
#  echo "File $FILE_PATH exists, open Mysql"
#  # 保持 MySQL 服务运行
#  tail -f /dev/null
#end if

# 等待 MySQL 服务启动
echo "Waiting for MySQL to be ready..."
while ! mysqladmin ping -h"127.0.0.1" --silent; do
  sleep 1
done

echo "MySQL is ready. Running the SQL script..."
mysql -u root -p"$MYSQL_ROOT_PASSWORD" < "db_problem.sql"

# shellcheck disable=SC2181
if [ $? -eq 0 ]; then
  mysql -u root -p"$MYSQL_ROOT_PASSWORD" < "db_user.sql"

  if [ $? -eq 0 ]; then
    mysql -u root -p"$MYSQL_ROOT_PASSWORD" < "nacos.sql"
    echo "success!!!!!"
  else
    echo "Databases exists skip scripts."
  fi

else
  echo "Databases exists skip scripts."
fi

# 创建文件
touch "$FILE_PATH"

rm -f /opt/db_problem.sql
rm -f /opt/db_user.sql
rm -f /opt/nacos.sql

# 保持 MySQL 服务运行
tail -f /dev/null
