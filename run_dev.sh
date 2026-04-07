#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEV_DIR="${ROOT_DIR}/.dev"
REPO_DIR="${ROOT_DIR}/.dev/carabassa"

mkdir -p "${DEV_DIR}"

export CARABASSA_REPO_DIR="${REPO_DIR}"
export CARABASSA_BASE_URL=http://localhost:8080
export CARABASSA_API_URL="${CARABASSA_BASE_URL}/api"
export CARABASSA_H2_CONSOLE=true

RESET_DB=false
STOP_ONLY=false
for arg in "$@"; do
  if [[ "$arg" == "--reset" ]]; then
    RESET_DB=true
  fi
  if [[ "$arg" == "--stop" ]]; then
    STOP_ONLY=true
  fi
done

if [[ "${RESET_DB}" == "true" ]]; then
  echo "Resetting repo dir at ${CARABASSA_REPO_DIR}..."
  rm -rf "${CARABASSA_REPO_DIR}"
fi

mkdir -p "${CARABASSA_REPO_DIR}"

backend_log="${REPO_DIR}/backend.log"
frontend_log="${REPO_DIR}/frontend.log"

start_detached() {
  local pid_file="$1"
  shift

  # Detach from the parent shell so services keep running after this script exits.
  # This is important when run under runners that send SIGHUP/kill the process group.
  if command -v setsid >/dev/null 2>&1; then
    setsid "$@" < /dev/null &
  else
    nohup "$@" < /dev/null &
  fi
  echo "$!" > "${pid_file}"
}

stop_if_running() {
  local name="$1"
  local port="$2"

  local pids
  # fuser outputs pids to stdout and port info to stderr
  pids=$(fuser -n tcp "${port}" 2>/dev/null || true)

  if [[ -n "${pids}" ]]; then
    echo "Stopping ${name} on port ${port} (pids:${pids})..."
    for pid in ${pids}; do
      kill -9 "${pid}" 2>/dev/null || true
    done
  fi
}

stop_if_running "backend" 8080
stop_if_running "frontend" 3000

if [[ "${STOP_ONLY}" == "true" ]]; then
  echo "Services stopped."
  exit 0
fi

echo "Starting backend..."
start_detached "${DEV_DIR}/backend.pid" bash -lc "
  set -euo pipefail
  cd \"${ROOT_DIR}/backend\"
  mvn -pl :carabassa-boot -am -DskipTests install
  exec mvn -Dcarabassa.repodir=\"${CARABASSA_REPO_DIR}\" -pl :carabassa-boot -am spring-boot:run
" > "${backend_log}" 2>&1

echo "Starting frontend..."
(cd "${ROOT_DIR}/frontend" && yarn install > "${REPO_DIR}/frontend-install.log" 2>&1)
start_detached "${DEV_DIR}/frontend.pid" bash -lc "
  set -euo pipefail
  cd \"${ROOT_DIR}/frontend\"
  exec yarn dev --host --port 3000
" > "${frontend_log}" 2>&1

echo "Waiting for backend to be ready..."
backend_ready=false
for i in $(seq 1 60); do
  if curl -sf "${CARABASSA_BASE_URL}/actuator/health" > /dev/null; then
    echo "Backend is ready."
    backend_ready=true
    break
  fi
  sleep 1
done

if [[ "${backend_ready}" != "true" ]]; then
  echo "Backend did not become ready. Showing last 80 log lines:"
  tail -n 80 "${backend_log}" || true
  stop_if_running "backend" 8080
  exit 1
fi

frontend_ready=false
for i in $(seq 1 60); do
  if curl -sf "http://localhost:3000/" > /dev/null; then
    frontend_ready=true
    break
  fi
  sleep 1
done

if [[ "${frontend_ready}" != "true" ]]; then
  echo "Frontend did not become ready. Showing last 80 log lines:"
  tail -n 80 "${frontend_log}" || true
  exit 1
fi

if [[ "${RESET_DB}" == "true" ]]; then
  echo "Creating dataset and uploading sample data..."
  
  echo "Logging in as admin..."
  TOKEN=$(curl -s -X POST "${CARABASSA_BASE_URL}/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"admin", "password":"changeme"}' | jq -r .token)
  
  if [[ "${TOKEN}" == "null" || -z "${TOKEN}" ]]; then
    echo "Failed to obtain authentication token. Check backend logs."
    exit 1
  fi
  
  echo "Importing items using the CLI..."
  (
    cd "${ROOT_DIR}/cli"
    # Ensure CLI is up-to-date
    mvn -DskipTests install > /dev/null 2>&1
    
    export CARABASSA_AUTH_TOKEN="${TOKEN}"
    export CARABASSA_API_URL="${CARABASSA_API_URL}"
    
    mvn spring-boot:run -Dspring-boot.run.arguments="create --dataset=test"
    mvn spring-boot:run -Dspring-boot.run.arguments="upload --dataset=test --path=../backend/engine/indexer/rdbms/src/test/resources/images"
    mvn spring-boot:run -Dspring-boot.run.arguments="upload --dataset=test --path=../backend/engine/indexer/rdbms/src/test/resources/videos"
  )
  echo "Dataset 'test' created and items imported."
fi

echo
echo "Dev environment ready:"
echo "- Backend: http://localhost:8080 (log: ${backend_log})"
echo "- Frontend: http://localhost:3000 (log: ${frontend_log})"
echo
echo "To stop services:"
echo "  ./run_dev.sh --stop"
