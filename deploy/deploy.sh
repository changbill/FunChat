#!/usr/bin/env bash
set -euo pipefail

ENV_FILE="${ENV_FILE:-/tmp/.funchat.env}"
APP_REPLICAS="${APP_REPLICAS:-3}"
MAX_APP_REPLICAS="${MAX_APP_REPLICAS:-3}"
MAX_ROLLING_SLOTS="${MAX_ROLLING_SLOTS:-4}"
DOCKER_CREDS_FILE="${DOCKER_CREDS_FILE:-}"
DOCKER_USER="${DOCKER_USER:-}"
DOCKER_PASS="${DOCKER_PASS:-}"
SMOKE_BASE_URL="${SMOKE_BASE_URL:-http://127.0.0.1}"
SMOKE_PATHS="${SMOKE_PATHS:-/health /}"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/scripts/deploy-common.sh"

trap cleanup_deploy_runtime EXIT

load_docker_credentials_file
require_docker_credentials
require_file "$ENV_FILE" "ENV_FILE"

if ! [[ "$APP_REPLICAS" =~ ^[0-9]+$ ]] || [[ "$APP_REPLICAS" -lt 1 || "$APP_REPLICAS" -gt "$MAX_APP_REPLICAS" ]]; then
  echo "APP_REPLICAS는 1부터 ${MAX_APP_REPLICAS} 사이의 정수여야 합니다. 현재 값: ${APP_REPLICAS}"
  exit 1
fi

SURGE_SLOT=$((APP_REPLICAS + 1))
if [[ "$SURGE_SLOT" -gt "$MAX_ROLLING_SLOTS" ]]; then
  echo "surge slot ${SURGE_SLOT}이 MAX_ROLLING_SLOTS(${MAX_ROLLING_SLOTS})를 초과합니다."
  exit 1
fi

cd "$SCRIPT_DIR"

router_compose() {
  docker compose --env-file "$ENV_FILE" -f docker-compose.router.yml "$@"
}

rolling_compose() {
  docker compose --env-file "$ENV_FILE" -p funchat-rolling -f docker-compose.rolling.yml "$@"
}

slot_range() {
  seq 1 "$APP_REPLICAS"
}

write_upstream_servers() {
  local exclude_app_slot="${1:-}"
  local exclude_web_slot="${2:-}"
  local include_app_surge_slot="${3:-}"
  local include_web_surge_slot="${4:-}"
  local slot
  local app_count=0
  local web_count=0

  {
    for slot in $(slot_range); do
      if [[ "$slot" == "$exclude_web_slot" ]]; then
        continue
      fi
      echo "server funchat-web-${slot}:80 resolve;"
      web_count=$((web_count + 1))
    done
    if [[ -n "$include_web_surge_slot" ]]; then
      echo "server funchat-web-${include_web_surge_slot}:80 resolve;"
      web_count=$((web_count + 1))
    fi
  } > nginx/upstream.web.servers.conf.tmp

  {
    for slot in $(slot_range); do
      if [[ "$slot" == "$exclude_app_slot" ]]; then
        continue
      fi
      echo "server funchat-app-${slot}:8081 resolve;"
      app_count=$((app_count + 1))
    done
    if [[ -n "$include_app_surge_slot" ]]; then
      echo "server funchat-app-${include_app_surge_slot}:8081 resolve;"
      app_count=$((app_count + 1))
    fi
  } > nginx/upstream.app.servers.conf.tmp

  if [[ "$app_count" -lt 1 || "$web_count" -lt 1 ]]; then
    rm -f nginx/upstream.web.servers.conf.tmp nginx/upstream.app.servers.conf.tmp
    echo "Nginx upstream에는 app/web 서버가 각각 1개 이상 필요합니다."
    return 1
  fi

  cat nginx/upstream.web.servers.conf.tmp > nginx/upstream.web.servers.conf
  cat nginx/upstream.app.servers.conf.tmp > nginx/upstream.app.servers.conf
  rm -f nginx/upstream.web.servers.conf.tmp nginx/upstream.app.servers.conf.tmp
}

test_nginx_config() {
  router_compose exec -T router nginx -t
}

reload_nginx() {
  router_compose exec -T router nginx -s reload
}

apply_upstream() {
  local exclude_app_slot="${1:-}"
  local exclude_web_slot="${2:-}"
  local include_app_surge_slot="${3:-}"
  local include_web_surge_slot="${4:-}"

  write_upstream_servers "$exclude_app_slot" "$exclude_web_slot" "$include_app_surge_slot" "$include_web_surge_slot"
  test_nginx_config
  reload_nginx
}

run_smoke_tests() {
  local path

  for path in $SMOKE_PATHS; do
    echo "Smoke test: ${SMOKE_BASE_URL}${path}"
    http_get "${SMOKE_BASE_URL}${path}"
  done
}

wait_app_health() {
  local slot="$1"
  local container="funchat-app-${slot}"
  local status

  echo "Waiting for ${container} health..."
  for _ in $(seq 1 60); do
    status="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container" 2>/dev/null || echo unknown)"
    if [[ "$status" == "healthy" ]]; then
      echo "${container} healthy."
      return 0
    fi
    sleep 2
  done

  echo "${container} did not become healthy."
  rolling_compose logs --no-color --tail=200 "app-${slot}" || true
  return 1
}

wait_web_running() {
  local slot="$1"
  local container="funchat-web-${slot}"
  local status

  echo "Waiting for ${container} running..."
  for _ in $(seq 1 30); do
    status="$(docker inspect -f '{{.State.Status}}' "$container" 2>/dev/null || echo unknown)"
    if [[ "$status" == "running" ]]; then
      echo "${container} running."
      return 0
    fi
    sleep 2
  done

  echo "${container} did not become running."
  rolling_compose logs --no-color --tail=200 "web-${slot}" || true
  return 1
}

ensure_slot_started() {
  local slot="$1"

  if ! docker inspect "funchat-app-${slot}" >/dev/null 2>&1; then
    rolling_compose up -d --no-deps "app-${slot}"
  fi
  wait_app_health "$slot"

  if ! docker inspect "funchat-web-${slot}" >/dev/null 2>&1; then
    rolling_compose up -d --no-deps "web-${slot}"
  fi
  wait_web_running "$slot"
}

stop_unused_slots() {
  local slot

  if [[ "$APP_REPLICAS" -ge "$MAX_ROLLING_SLOTS" ]]; then
    return 0
  fi

  for slot in $(seq $((APP_REPLICAS + 1)) "$MAX_ROLLING_SLOTS"); do
    rolling_compose rm -f -s "web-${slot}" "app-${slot}" >/dev/null 2>&1 || true
  done
}

start_app_surge() {
  local surge_slot="$SURGE_SLOT"

  echo "Starting backend surge slot ${surge_slot}..."
  rolling_compose up -d --no-deps --force-recreate "app-${surge_slot}"
  wait_app_health "$surge_slot"
  apply_upstream "" "" "$surge_slot" ""
  run_smoke_tests
}

stop_app_surge() {
  local surge_slot="$SURGE_SLOT"

  apply_upstream "" "" "" ""
  rolling_compose rm -f -s "app-${surge_slot}" >/dev/null 2>&1 || true
}

update_app_slot() {
  local slot="$1"
  local surge_slot="$SURGE_SLOT"

  apply_upstream "$slot" "" "$surge_slot" ""
  rolling_compose stop "app-${slot}" >/dev/null 2>&1 || true

  rolling_compose up -d --no-deps --force-recreate "app-${slot}"
  wait_app_health "$slot"

  apply_upstream "" "" "$surge_slot" ""
  run_smoke_tests
}

start_web_surge() {
  local surge_slot="$SURGE_SLOT"

  echo "Starting frontend surge slot ${surge_slot}..."
  rolling_compose up -d --no-deps --force-recreate "web-${surge_slot}"
  wait_web_running "$surge_slot"
  apply_upstream "" "" "" "$surge_slot"
  run_smoke_tests
}

stop_web_surge() {
  local surge_slot="$SURGE_SLOT"

  apply_upstream "" "" "" ""
  rolling_compose rm -f -s "web-${surge_slot}" >/dev/null 2>&1 || true
}

update_web_slot() {
  local slot="$1"
  local surge_slot="$SURGE_SLOT"

  apply_upstream "" "$slot" "" "$surge_slot"
  rolling_compose stop "web-${slot}" >/dev/null 2>&1 || true

  rolling_compose up -d --no-deps --force-recreate "web-${slot}"
  wait_web_running "$slot"

  apply_upstream "" "" "" "$surge_slot"
  run_smoke_tests
}

docker_login_with_credentials

mkdir -p nginx

# 1) infra는 항상 유지한다. 최초 실행 시 네트워크와 데이터 저장소를 만든다.
docker compose --env-file "$ENV_FILE" -f docker-compose.infra.yml up -d

# 2) 새 이미지 pull
rolling_compose pull

# 3) 롤링 슬롯이 없거나 부족하면 먼저 현재 이미지로 전체 슬롯을 준비한다.
for slot in $(slot_range); do
  ensure_slot_started "$slot"
done

# 4) 라우터는 재생성하지 않고 유지한다. 없으면 최초 1회만 기동한다.
write_upstream_servers "" ""
router_compose up -d
test_nginx_config
reload_nginx
run_smoke_tests
stop_unused_slots

echo "Surge rolling deploy uses steady replicas=${APP_REPLICAS}, surge slot=${SURGE_SLOT}."

# 5) backend app을 한 슬롯씩 교체한다.
APP_SURGE_STARTED=0
CURRENT_APP_SLOT=""
cleanup_app_surge() {
  if [[ "$APP_SURGE_STARTED" == "1" ]]; then
    echo "Keeping backend surge slot ${SURGE_SLOT} in upstream after deployment failure."
    apply_upstream "$CURRENT_APP_SLOT" "" "$SURGE_SLOT" "" || true
  fi
}
trap 'cleanup_app_surge; cleanup_deploy_runtime' EXIT

start_app_surge
APP_SURGE_STARTED=1
for slot in $(slot_range); do
  echo "Rolling update backend app slot ${slot}/${APP_REPLICAS}..."
  CURRENT_APP_SLOT="$slot"
  update_app_slot "$slot"
  CURRENT_APP_SLOT=""
done
stop_app_surge
APP_SURGE_STARTED=0
trap cleanup_deploy_runtime EXIT

# 6) frontend web을 한 슬롯씩 교체한다.
WEB_SURGE_STARTED=0
CURRENT_WEB_SLOT=""
cleanup_web_surge() {
  if [[ "$WEB_SURGE_STARTED" == "1" ]]; then
    echo "Keeping frontend surge slot ${SURGE_SLOT} in upstream after deployment failure."
    apply_upstream "" "$CURRENT_WEB_SLOT" "" "$SURGE_SLOT" || true
  fi
}
trap 'cleanup_web_surge; cleanup_deploy_runtime' EXIT

start_web_surge
WEB_SURGE_STARTED=1
for slot in $(slot_range); do
  echo "Rolling update frontend web slot ${slot}/${APP_REPLICAS}..."
  CURRENT_WEB_SLOT="$slot"
  update_web_slot "$slot"
  CURRENT_WEB_SLOT=""
done
stop_web_surge
WEB_SURGE_STARTED=0
trap cleanup_deploy_runtime EXIT

docker_logout_if_needed
docker image prune -f
