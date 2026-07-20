#!/bin/sh
# Migrations and seed are idempotent before API startup.
set -eu
corepack pnpm db:migrate
corepack pnpm db:seed
exec corepack pnpm --filter @werft/api exec tsx src/server.ts
