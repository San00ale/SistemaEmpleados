#!/bin/bash
set -e

DB_HOST="${DB_HOST:-mysql}"
DB_PORT="${DB_PORT:-3306}"

echo "Esperando MySQL en ${DB_HOST}:${DB_PORT}..."
while ! nc -z "${DB_HOST}" "${DB_PORT}" 2>/dev/null; do
  sleep 2
done
echo "MySQL disponible."

exec java -Xmx512m \
  -Djava.awt.headless="${JAVA_AWT_HEADLESS:-false}" \
  -jar /app/dist/SistemaEmpleados.jar
