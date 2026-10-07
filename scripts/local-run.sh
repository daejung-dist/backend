#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

if [ ! -f ".env" ]; then
  echo "Missing .env file in project root."
  exit 1
fi

set -a
source .env
set +a

docker compose up -d

echo "Waiting for MySQL to become ready..."
for _ in {1..60}; do
  if docker compose exec -T mysql mysqladmin ping -h 127.0.0.1 -uroot -p"${MYSQL_ROOT_PASSWORD}" --silent >/dev/null 2>&1; then
    echo "MySQL is ready."
    break
  fi
  sleep 2
done

if ! docker compose exec -T mysql mysqladmin ping -h 127.0.0.1 -uroot -p"${MYSQL_ROOT_PASSWORD}" --silent >/dev/null 2>&1; then
  echo "MySQL did not become ready in time. Check: docker compose logs mysql"
  exit 1
fi

./gradlew bootRun --args='--spring.profiles.active=local'
