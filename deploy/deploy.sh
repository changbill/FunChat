#!/usr/bin/env bash
set -euo pipefail

ENV_FILE="${ENV_FILE:-/tmp/.funchat.env}"
APP_REPLICAS="${APP_REPLICAS:-3}"
DOCKER_USER="${DOCKER_USER:-}"
DOCKER_PASS="${DOCKER_PASS:-}"

cleanup() {
  if [[ "${ENV_FILE}" == /tmp/* ]]; then
    rm -f -- "${ENV_FILE}" || true
  fi
}
trap cleanup EXIT

if [[ -z "$DOCKER_USER" || -z "$DOCKER_PASS" ]]; then
  echo "DOCKER_USER/DOCKER_PASS가 설정되지 않았습니다."
  exit 1
fi

if [[ ! -f "$ENV_FILE" ]]; then
  echo "ENV_FILE을 찾을 수 없습니다: $ENV_FILE"
  exit 1
fi

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin

# 0) 기본 폴더/상태 파일 준비
mkdir -p nginx
if [[ ! -f nginx/upstream.conf ]]; then
  cp nginx/upstream.blue.conf nginx/upstream.conf
  echo blue > active_color
fi

ACTIVE="$(cat active_color 2>/dev/null || echo blue)"
if [[ "$ACTIVE" == "blue" ]]; then INACTIVE=green; else INACTIVE=blue; fi

# 1) infra는 항상 유지(처음만 띄우고 이후엔 변경 최소화)
docker compose --env-file "$ENV_FILE" -f docker-compose.infra.yml up -d

# 2) 새 이미지 pull
docker compose --env-file "$ENV_FILE" -p "funchat-${INACTIVE}" -f "docker-compose.${INACTIVE}.yml" pull

# 3) 비활성(=새) 스택 기동
docker compose --env-file "$ENV_FILE" -p "funchat-${INACTIVE}" -f "docker-compose.${INACTIVE}.yml" up -d --remove-orphans --scale "app=${APP_REPLICAS}"

# 4) 헬스체크 대기(app 컨테이너가 여러 개여도 동작)
echo 'Waiting for new stack health...'
APP_IDS="$(docker compose --env-file "$ENV_FILE" -p "funchat-${INACTIVE}" -f "docker-compose.${INACTIVE}.yml" ps -q app)"
if [[ -z "$APP_IDS" ]]; then
  echo 'No app containers found.'
  docker compose --env-file "$ENV_FILE" -p "funchat-${INACTIVE}" -f "docker-compose.${INACTIVE}.yml" ps
  exit 1
fi

OK=0
for _ in $(seq 1 60); do
  ALL_HEALTHY=1
  for id in $APP_IDS; do
    STATUS="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}nohealth{{end}}' "$id" 2>/dev/null || echo unknown)"
    if [[ "$STATUS" != "healthy" ]]; then
      ALL_HEALTHY=0
      break
    fi
  done

  if [[ "$ALL_HEALTHY" == "1" ]]; then
    OK=1
    echo 'New stack healthy.'
    break
  fi
  sleep 2
done

if [[ "$OK" != "1" ]]; then
  echo 'New stack did not become healthy.'
  docker compose --env-file "$ENV_FILE" -p "funchat-${INACTIVE}" -f "docker-compose.${INACTIVE}.yml" logs --no-color --tail=200
  exit 1
fi

# 5) 라우터(Nginx) upstream 전환 + 재생성
if [[ "$INACTIVE" == "blue" ]]; then
  cp nginx/upstream.blue.conf nginx/upstream.conf
else
  cp nginx/upstream.green.conf nginx/upstream.conf
fi

docker compose --env-file "$ENV_FILE" -f docker-compose.router.yml up -d --force-recreate
echo "$INACTIVE" > active_color

# 6) 구 스택 정리
# project name에 '_'가 들어가면 Docker DNS에서 이름 해석이 불안정할 수 있어
# 새 배포부터는 funchat-blue/green 형태로 전환하되, 기존 funchat_blue/green도 함께 정리 시도
docker compose --env-file "$ENV_FILE" -p "funchat-${ACTIVE}" -f "docker-compose.${ACTIVE}.yml" down \
  || docker compose --env-file "$ENV_FILE" -p "funchat_${ACTIVE}" -f "docker-compose.${ACTIVE}.yml" down \
  || true

docker logout
docker image prune -f
