#!/bin/sh
# Server-first deployment; never modifies the Cortex stack.
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

if ! grep -q '^WERFT_ADMIN_EMAIL=' .env; then
  printf 'WERFT_ADMIN_EMAIL=frank@example.de\n' >> .env
fi
if ! grep -q '^WERFT_ADMIN_PASSWORD=' .env; then
  printf 'WERFT_ADMIN_PASSWORD=%s\n' "$(openssl rand -hex 24)" >> .env
fi

chmod 600 .env
mkdir -p /var/lib/werft-studio-maintenance
docker compose -f compose.server.yaml config --quiet
docker compose -f compose.server.yaml up -d --build
install -m 0644 docker/werft-docker-cache-prune.service docker/werft-docker-cache-prune.timer docker/werft-docker-cache-prune.path /etc/systemd/system/
systemctl daemon-reload
systemctl enable --now werft-docker-cache-prune.timer werft-docker-cache-prune.path
systemctl start werft-docker-cache-prune.service
docker compose -f compose.server.yaml ps
