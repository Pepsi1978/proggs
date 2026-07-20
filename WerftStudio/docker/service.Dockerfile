FROM node:24.18.0-bookworm-slim
WORKDIR /app
ENV PNPM_HOME=/pnpm
ENV PATH=$PNPM_HOME:$PATH
RUN corepack enable
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml turbo.json tsconfig.base.json ./
COPY apps ./apps
COPY packages ./packages
COPY docker ./docker
RUN corepack pnpm install --frozen-lockfile
USER node
CMD ["node_modules/.bin/pnpm", "--filter", "@werft/api", "exec", "tsx", "src/server.ts"]
