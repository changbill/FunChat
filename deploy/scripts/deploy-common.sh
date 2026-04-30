#!/usr/bin/env bash

DOCKER_LOGGED_IN="${DOCKER_LOGGED_IN:-0}"

cleanup_deploy_runtime() {
  if [[ "$DOCKER_LOGGED_IN" == "1" ]]; then
    docker logout >/dev/null 2>&1 || true
  fi
  if [[ -n "${DOCKER_CREDS_FILE:-}" && "${DOCKER_CREDS_FILE}" == /tmp/* ]]; then
    rm -f -- "${DOCKER_CREDS_FILE}" || true
  fi
}

load_docker_credentials_file() {
  if [[ -z "${DOCKER_CREDS_FILE:-}" ]]; then
    return 0
  fi

  if [[ ! -f "$DOCKER_CREDS_FILE" ]]; then
    echo "DOCKER_CREDS_FILE을 찾을 수 없습니다: $DOCKER_CREDS_FILE"
    exit 1
  fi

  DOCKER_USER="$(sed -n '1p' "$DOCKER_CREDS_FILE")"
  DOCKER_PASS="$(sed -n '2p' "$DOCKER_CREDS_FILE")"
}

require_docker_credentials() {
  if [[ -z "${DOCKER_USER:-}" || -z "${DOCKER_PASS:-}" ]]; then
    echo "DOCKER_USER/DOCKER_PASS가 설정되지 않았습니다."
    exit 1
  fi
}

require_file() {
  local file_path="$1"
  local label="$2"

  if [[ ! -f "$file_path" ]]; then
    echo "${label}을 찾을 수 없습니다: ${file_path}"
    exit 1
  fi
}

docker_login_with_credentials() {
  echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
  DOCKER_LOGGED_IN=1
}

docker_logout_if_needed() {
  if [[ "$DOCKER_LOGGED_IN" == "1" ]]; then
    docker logout
    DOCKER_LOGGED_IN=0
  fi
}

http_get() {
  local url="$1"

  if command -v curl >/dev/null 2>&1; then
    curl -fsS --max-time 5 "$url" >/dev/null
  else
    wget -q -T 5 -O- "$url" >/dev/null
  fi
}
