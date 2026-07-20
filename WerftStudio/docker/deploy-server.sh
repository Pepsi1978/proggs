#!/bin/sh
set -eu

cd /opt/werft-studio
umask 077

if [ ! -f .env ]; then
  postgres_password="$(openssl rand -hex 24)"
  session_secret="$(openssl rand -hex 48)"
  s3_access_key="$(openssl rand -hex 16)"
  s3_secret_key="$(openssl rand -hex 32)"
  {
    printf 'WERFT_POSTGRES_PASSWORD=%s\n' "$postgres_password"
    printf 'WERFT_SESSION_SECRET=%s\n' "$session_secret"
    printf 'WERFT_S3_ACCESS_KEY=%s\n' "$s3_access_key"
    printf 'WERFT_S3_SECRET_KEY=%s\n' "$s3_secret_key"
  } > .env
fi

chmod 600 .env
docker compose -f compose.server.yaml config --quiet
docker compose -f compose.server.yaml up -d --build
docker compose -f compose.server.yaml ps
