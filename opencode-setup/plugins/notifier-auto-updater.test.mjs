import assert from "node:assert/strict"
import { mkdtemp, readFile, rm } from "node:fs/promises"
import { tmpdir } from "node:os"
import { join } from "node:path"
import test from "node:test"

import { compareVersions, runNotifierUpdate, shouldDeferUpdate } from "./notifier-auto-updater.mjs"

test("version comparison never downgrades a newer installed notifier", () => {
  assert.equal(compareVersions("0.2.8", "0.2.8"), 0)
  assert.equal(compareVersions("0.2.9", "0.2.8"), 1)
  assert.equal(compareVersions("1.0.0", "0.99.99"), 1)
  assert.equal(compareVersions("0.2.8-beta.1", "0.2.8"), -1)
})

test("daily cooldown applies only while the recorded installed version still matches", () => {
  const now = Date.parse("2026-07-17T14:00:00Z")
  const state = {
    installedVersion: "0.2.8",
    nextCheckNotBeforeUtc: "2026-07-18T14:00:00Z",
  }
  assert.equal(shouldDeferUpdate(state, "0.2.8", now), true)
  assert.equal(shouldDeferUpdate(state, "0.2.7", now), false)
  assert.equal(shouldDeferUpdate(state, "0.2.8", Date.parse("2026-07-19T14:00:00Z")), false)
})

test("a staged real package is promoted only after its contract passes", {
  skip: process.env.NOTIFIER_UPDATE_INTEGRATION !== "1",
  timeout: 300000,
}, async () => {
  const configDirectory = await mkdtemp(join(tmpdir(), "notifier-updater-integration-"))
  try {
    const installed = await runNotifierUpdate({
      configDirectory,
      candidateVersion: "0.2.8",
      force: true,
      forceReinstall: true,
    })
    assert.equal(installed.status, "installed-restart-required")
    const packageJson = JSON.parse(await readFile(join(
      configDirectory,
      "node_modules",
      "@mohak34",
      "opencode-notifier",
      "package.json",
    ), "utf8"))
    assert.equal(packageJson.version, "0.2.8")

    const deferred = await runNotifierUpdate({ configDirectory })
    assert.equal(deferred.status, "deferred")

    const current = await runNotifierUpdate({
      configDirectory,
      candidateVersion: "0.2.8",
      force: true,
    })
    assert.equal(current.status, "up-to-date")
  } finally {
    await rm(configDirectory, { recursive: true, force: true })
  }
})
