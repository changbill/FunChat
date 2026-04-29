#!/usr/bin/env bash
set -euo pipefail

require_env() {
  local name="$1"

  if [[ -z "${!name:-}" ]]; then
    echo "${name} 환경변수가 설정되지 않았습니다."
    exit 1
  fi
}

require_env DEPLOY_USER
require_env DEPLOY_HOST
require_env ENV_FILE
require_env DOCKER_USER
require_env DOCKER_PASS

APP_REPLICAS="${APP_REPLICAS:-3}"
REMOTE="${DEPLOY_USER}@${DEPLOY_HOST}"
SSH_OPTS=(-o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null)
REMOTE_ENV_FILE=""
REMOTE_CREDS_FILE=""

ssh_remote() {
  ssh "${SSH_OPTS[@]}" "$REMOTE" "$@"
}

cleanup_remote() {
  if [[ -n "$REMOTE_ENV_FILE" || -n "$REMOTE_CREDS_FILE" ]]; then
    ssh_remote "rm -f '$REMOTE_ENV_FILE' '$REMOTE_CREDS_FILE'" >/dev/null 2>&1 || true
  fi
}
trap cleanup_remote EXIT

sync_deploy_directory() {
  ssh_remote "mkdir -p ~/funchat && rm -rf ~/funchat/deploy.next"
  ssh_remote "mkdir -p ~/funchat/deploy.next"
  scp "${SSH_OPTS[@]}" -r deploy/. "${REMOTE}:~/funchat/deploy.next/"
  ssh_remote "rm -rf ~/funchat/deploy.prev && if [ -d ~/funchat/deploy ]; then mv ~/funchat/deploy ~/funchat/deploy.prev; fi && mv ~/funchat/deploy.next ~/funchat/deploy"
}

sync_monitoring_directory() {
  ssh_remote "mkdir -p ~/funchat && rm -rf ~/funchat/monitoring.next"
  ssh_remote "mkdir -p ~/funchat/monitoring.next"
  scp "${SSH_OPTS[@]}" -r monitoring/. "${REMOTE}:~/funchat/monitoring.next/"
  ssh_remote "rm -rf ~/funchat/monitoring.prev && if [ -d ~/funchat/monitoring ]; then mv ~/funchat/monitoring ~/funchat/monitoring.prev; fi && mv ~/funchat/monitoring.next ~/funchat/monitoring"
}

create_remote_secret_files() {
  REMOTE_ENV_FILE="$(ssh_remote 'umask 077 && mktemp /tmp/funchat-env.XXXXXX')"
  REMOTE_CREDS_FILE="$(ssh_remote 'umask 077 && mktemp /tmp/funchat-docker-creds.XXXXXX')"

  ssh_remote "cat > '$REMOTE_ENV_FILE'" < "$ENV_FILE"
  printf '%s\n%s\n' "$DOCKER_USER" "$DOCKER_PASS" | ssh_remote "cat > '$REMOTE_CREDS_FILE'"
}

run_remote_deploy() {
  ssh_remote "chmod +x ~/funchat/deploy/deploy.sh && ENV_FILE='$REMOTE_ENV_FILE' DOCKER_CREDS_FILE='$REMOTE_CREDS_FILE' APP_REPLICAS='$APP_REPLICAS' ~/funchat/deploy/deploy.sh"
}

run_remote_monitoring() {
  ssh_remote "cd ~/funchat && docker compose --env-file '$REMOTE_ENV_FILE' -f monitoring/docker-compose.yml up -d prometheus grafana mysql-exporter"
}

sync_deploy_directory
sync_monitoring_directory
create_remote_secret_files
run_remote_deploy
run_remote_monitoring
