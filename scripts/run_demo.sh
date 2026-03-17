#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMMAND="${1:-run-all}"

cd "$ROOT_DIR"

usage() {
  echo "Usage: ./scripts/run_demo.sh [full-load|cdc|run-all]" >&2
}

log_step() {
  echo "[run_demo] $1"
}

case "$COMMAND" in
  full-load|cdc|run-all)
    ;;
  *)
    usage
    exit 1
    ;;
esac

docker compose up -d

wait_for_container() {
  local container_name="$1"
  local retries=30

  until [ "$retries" -eq 0 ]; do
    status="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container_name" 2>/dev/null || true)"
    if [ "$status" = "healthy" ] || [ "$status" = "running" ]; then
      return 0
    fi
    sleep 2
    retries=$((retries - 1))
  done

  echo "Timed out waiting for $container_name to become ready" >&2
  return 1
}

log_step "Waiting for MySQL and PostgreSQL containers to become ready"
wait_for_container "mini-migration-mysql"
wait_for_container "mini-migration-postgres"

log_step "Starting Spring Boot replication service with command '$COMMAND'"
mvn spring-boot:run -Dspring-boot.run.arguments="$COMMAND"
