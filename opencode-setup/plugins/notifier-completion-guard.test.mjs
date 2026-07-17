import assert from "node:assert/strict"
import { readFile } from "node:fs/promises"
import test from "node:test"

import { createNotifierCompletionGuard } from "./notifier-completion-guard.js"

const sessionEvent = (type, id, properties = {}) => ({
  type,
  properties: { sessionID: id, ...properties },
})

async function createHarness() {
  const calls = { events: [], tools: [], permissions: 0 }
  const notifier = async () => ({
    event: async ({ event }) => calls.events.push(event.type),
    "tool.execute.before": async ({ tool }) => calls.tools.push(tool),
    "permission.ask": async () => { calls.permissions += 1 },
  })
  const hooks = await createNotifierCompletionGuard({}, notifier)
  return { calls, hooks }
}

test("a submitted prompt does not become a completion alert", async () => {
  const { calls, hooks } = await createHarness()
  const id = "session-1"

  await hooks.event({ event: sessionEvent("message.updated", id, { info: { role: "user", sessionID: id } }) })
  await hooks.event({ event: sessionEvent("session.status", id, { status: { type: "busy" } }) })
  await hooks.event({ event: sessionEvent("session.idle", id) })

  assert.deepEqual(calls.events, ["session.status"])
})

test("idle is forwarded only after real work in the current turn", async () => {
  const { calls, hooks } = await createHarness()
  const id = "session-2"

  await hooks.event({ event: sessionEvent("session.status", id, { status: { type: "busy" } }) })
  await hooks["tool.execute.before"]({ sessionID: id, tool: "apply_patch" }, {})
  await hooks.event({ event: sessionEvent("session.idle", id) })

  assert.deepEqual(calls.tools, [])
  assert.deepEqual(calls.events, ["session.status", "session.idle"])
})

test("only an AI question reaches the notifier tool hook", async () => {
  const { calls, hooks } = await createHarness()
  const id = "session-3"

  await hooks["tool.execute.before"]({ sessionID: id, tool: "bash" }, {})
  await hooks["tool.execute.before"]({ sessionID: id, tool: "plan_exit" }, {})
  await hooks["tool.execute.before"]({ sessionID: id, tool: "question" }, {})

  assert.deepEqual(calls.tools, ["question"])
})

test("errors and permission requests stay silent", async () => {
  const { calls, hooks } = await createHarness()
  const id = "session-4"

  await hooks.event({ event: sessionEvent("session.error", id) })
  await hooks.event({ event: sessionEvent("permission.asked", id) })
  await hooks["permission.ask"]()

  assert.deepEqual(calls.events, [])
  assert.equal(calls.permissions, 0)
})

test("setup has one guarded, auto-updated notifier and no intermediate-event sounds", async () => {
  const setupDir = new URL("../", import.meta.url)
  const [config, windowsInstaller, unixInstaller, dirtyWatchdog, guard, updater, contract] = await Promise.all([
    readFile(new URL("opencode.jsonc", setupDir), "utf8"),
    readFile(new URL("install.ps1", setupDir), "utf8"),
    readFile(new URL("install.sh", setupDir), "utf8"),
    readFile(new URL("plugins/git-dirty-watchdog.js", setupDir), "utf8"),
    readFile(new URL("plugins/notifier-completion-guard.js", setupDir), "utf8"),
    readFile(new URL("plugins/notifier-auto-updater.mjs", setupDir), "utf8"),
    readFile(new URL("plugins/notifier-candidate-contract.mjs", setupDir), "utf8"),
  ])

  assert.doesNotMatch(config, /^\s*"@mohak34\/opencode-notifier@/m)
  assert.match(windowsInstaller, /notifier-auto-updater\.mjs[^]*--force --verbose/)
  assert.match(unixInstaller, /notifier-auto-updater\.mjs[^]*--force --verbose/)
  assert.match(windowsInstaller, /npm install --silent --save-exact/)
  assert.match(unixInstaller, /npm install --silent --save-exact/)
  assert.match(windowsInstaller, /-notlike '\*\.test\.mjs'/)
  assert.match(unixInstaller, /! -name '\*\.test\.mjs'/)
  assert.match(guard, /notifier-auto-updater\.mjs/)
  assert.match(updater, /24 \* 60 \* 60 \* 1000/)
  assert.match(updater, /notifier-candidate-contract\.mjs/)
  assert.match(contract, /\["question", "complete"\]/)

  const disabledEvents = ["permission", "subagent_complete", "error", "interrupted", "user_cancelled", "plan_exit", "session_started", "user_message", "client_connected"]
  for (const source of [windowsInstaller, unixInstaller]) {
    assert.match(source, /["']?complete["']?\s*[=:]\s*(?:\$true|true)/)
    assert.match(source, /["']?question["']?\s*[=:]\s*(?:\$true|true)/)
    for (const event of disabledEvents) {
      assert.match(source, new RegExp(`["']?${event}["']?\\s*[=:]\\s*(?:\\$false|false)`))
    }
  }
  assert.doesNotMatch(dirtyWatchdog, /SoundPlayer|playAlertSound|\.wav/)
})
