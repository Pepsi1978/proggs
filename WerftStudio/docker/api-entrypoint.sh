#!/bin/sh
# Migrations and seed are idempotent before API startup.
set -eu
node_modules/.bin/pnpm db:migrate
node_modules/.bin/pnpm db:seed
exec node_modules/.bin/pnpm --filter @werft/api exec tsx src/server.ts
