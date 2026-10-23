#!/bin/bash
set -euo pipefail

export MYSQL_DATABASE="${MYSQL_DATABASE:-cyberops_arena}"
export MYSQL_USER="${MYSQL_USER:-cyberops}"
export MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-$(head -c 48 /dev/urandom | base64 | tr -d '\n')}"
export MYSQL_PASSWORD="${MYSQL_PASSWORD:-$(head -c 48 /dev/urandom | base64 | tr -d '\n')}"

docker-entrypoint.sh mysqld \
  --daemonize \
  --pid-file=/var/run/mysqld/mysqld.pid \
  --bind-address=127.0.0.1 \
  --mysqlx=0 \
  --performance-schema=OFF \
  --innodb-buffer-pool-size=64M \
  --max-connections=24

for attempt in $(seq 1 60); do
  if mysqladmin ping --host=127.0.0.1 --silent; then
    break
  fi
  sleep 1
done

if ! mysqladmin ping --host=127.0.0.1 --silent; then
  echo "MySQL did not become ready" >&2
  exit 1
fi

export DB_HOST=127.0.0.1
export DB_PORT=3306
export DB_NAME="$MYSQL_DATABASE"
export DB_USERNAME="$MYSQL_USER"
export DB_PASSWORD="$MYSQL_PASSWORD"
export SERVER_PORT="${PORT:-8080}"
export JWT_SECRET="${JWT_SECRET:-$(head -c 48 /dev/urandom | base64 | tr -d '\n')}"

exec java -Xms64m -Xmx192m -XX:MaxMetaspaceSize=96m -Duser.timezone=UTC -jar /app/app.jar
