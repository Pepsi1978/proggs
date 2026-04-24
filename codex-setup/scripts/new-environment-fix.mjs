#!/usr/bin/env node

import crypto from "node:crypto";
import { spawnSync } from "node:child_process";

const fixedAt = new Date().toISOString();
const id = `envfix-${crypto.randomUUID()}`;
const args = process.argv.slice(2);

const result = spawnSync(
  process.execPath,
  [
    "codex-setup/scripts/register-environment-fix.mjs",
    "--id", id,
    "--fixedAt", fixedAt,
    ...args
  ],
  { stdio: "inherit", cwd: process.cwd() }
);

process.exit(result.status ?? 1);
