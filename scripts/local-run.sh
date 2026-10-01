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
./gradlew bootRun --args='--spring.profiles.active=local'
