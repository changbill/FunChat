#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/deploy-common.sh"

trap cleanup_deploy_runtime EXIT

if [[ "$#" -lt 1 ]]; then
  echo "push할 Docker image를 1개 이상 전달해야 합니다."
  exit 1
fi

load_docker_credentials_file
require_docker_credentials
docker_login_with_credentials

for image in "$@"; do
  docker push "$image"
done

docker_logout_if_needed
