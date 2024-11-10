# 启动 MySQL 服务
echo "Starting MySQL service..."
#service mysql start
docker-entrypoint.sh mysqld &

# 等待 MySQL 服务启动
echo "Waiting for MySQL to be ready..."
while ! mysqladmin ping -h"127.0.0.1" --silent; do
  sleep 1
done

echo "MySQL is ready. Running the SQL script..."
mysql -u root -p"$MYSQL_ROOT_PASSWORD" < "db_problem.sql"
mysql -u root -p"$MYSQL_ROOT_PASSWORD" < "db_user.sql"
mysql -u root -p"$MYSQL_ROOT_PASSWORD" < "nacos.sql"

# shellcheck disable=SC2181
if [ $? -eq 0 ]; then
  echo "SQL script executed successfully."
else
  echo "Failed to execute SQL script."
  exit 1
fi

# 保持 MySQL 服务运行
tail -f /dev/null
