FROM node:24.18.0-bookworm-slim AS build
WORKDIR /app
ENV PNPM_HOME=/pnpm
ENV PATH=$PNPM_HOME:$PATH
RUN corepack enable
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml turbo.json tsconfig.base.json ./
COPY apps/web ./apps/web
COPY packages/ui ./packages/ui
COPY packages/contracts ./packages/contracts
RUN corepack pnpm install --frozen-lockfile
RUN corepack pnpm --filter @werft/web build

FROM caddy:2.11.4-alpine
COPY docker/Caddyfile /etc/caddy/Caddyfile
COPY --from=build /app/apps/web/dist /srv
