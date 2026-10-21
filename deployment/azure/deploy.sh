#!/usr/bin/env bash

set -Eeuo pipefail

if [[ $# -ne 4 ]]; then
  echo "Usage: $0 <image-tag> <acr-name> <key-vault-name> <deploy-token-secret>" >&2
  exit 2
fi

IMAGE_TAG="$1"
ACR_NAME="$2"
KEY_VAULT_NAME="$3"
DEPLOY_TOKEN_SECRET="$4"
DEPLOY_ROOT="${DEPLOY_ROOT:-/opt/messageucolab}"
COMPOSE_FILE="$DEPLOY_ROOT/deployment/azure/docker-compose.yml"
CURRENT_TAG_FILE="$DEPLOY_ROOT/.current-image-tag"
PREVIOUS_TAG=""

if [[ ! -f "$COMPOSE_FILE" ]]; then
  echo "Compose file not found at $COMPOSE_FILE" >&2
  exit 1
fi

if [[ -f "$CURRENT_TAG_FILE" ]]; then
  PREVIOUS_TAG="$(<"$CURRENT_TAG_FILE")"
fi

az login --identity --allow-no-subscriptions --output none
export ACR_LOGIN_SERVER="${ACR_NAME}.azurecr.io"
export DOPPLER_TOKEN
DOPPLER_TOKEN="$(az keyvault secret show --vault-name "$KEY_VAULT_NAME" --name "$DEPLOY_TOKEN_SECRET" --query value --output tsv)"
export IMAGE_TAG

cleanup() {
  unset DOPPLER_TOKEN
}
trap cleanup EXIT

az acr login --name "$ACR_NAME" --output none
cd "$DEPLOY_ROOT"

doppler run -- docker compose --file "$COMPOSE_FILE" config --quiet
doppler run -- docker compose --file "$COMPOSE_FILE" pull
doppler run -- docker compose --file "$COMPOSE_FILE" up --detach redis surrealdb pulsar
doppler run -- docker compose --file "$COMPOSE_FILE" run --rm redis-init
doppler run -- docker compose --file "$COMPOSE_FILE" run --rm surrealdb-init
doppler run -- docker compose --file "$COMPOSE_FILE" up --detach messageucolab kong

healthy=false
for _ in {1..30}; do
  if curl --fail --silent http://localhost:8000/actuator/health >/dev/null; then
    healthy=true
    break
  fi
  sleep 10
done

if [[ "$healthy" == "true" ]]; then
  printf '%s' "$IMAGE_TAG" > "$CURRENT_TAG_FILE"
  echo "Deployment completed with image tag $IMAGE_TAG"
  exit 0
fi

echo "Health check failed for image tag $IMAGE_TAG" >&2
doppler run -- docker compose --file "$COMPOSE_FILE" logs --tail 100 messageucolab kong >&2 || true

if [[ -n "$PREVIOUS_TAG" && "$PREVIOUS_TAG" != "$IMAGE_TAG" ]]; then
  echo "Rolling back to image tag $PREVIOUS_TAG" >&2
  export IMAGE_TAG="$PREVIOUS_TAG"
  doppler run -- docker compose --file "$COMPOSE_FILE" up --detach --no-deps messageucolab
  for _ in {1..18}; do
    if curl --fail --silent http://localhost:8000/actuator/health >/dev/null; then
      echo "Rollback completed with image tag $PREVIOUS_TAG" >&2
      break
    fi
    sleep 10
  done
fi

exit 1
