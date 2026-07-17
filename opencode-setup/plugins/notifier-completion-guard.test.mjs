import assert from "node:assert/strict"
import { readFile } from "node:fs/promises"
import test from "node:test"

import { createNotifierCompletionGuard } from "./notifier-completion-guard.js"

const sessionEvent = (type, id, properties = {}) => ({
  type,
  properties: { sessionID: id, ...properties },
})

async function createHarness() {
  const calls = { events: [], tools: [] }
  const notifier = async () => ({
    event: async ({ event }) => calls.events.push(event.type),
    "tool.execute.before": async ({ tool }) => calls.tools.push(tool),
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

  assert.deepEqual(calls.events, ["message.updated", "session.status"])
})

test("idle is forwarded only after real work in the current turn", async () => {
  const { calls, hooks } = await createHarness()
  const id = "session-2"

  await hooks.event({ event: sessionEvent("session.status", id, { status: { type: "busy" } }) })
  await hooks["tool.execute.before"]({ sessionID: id, tool: "apply_patch" }, {})
  await hooks.event({ event: sessionEvent("session.idle", id) })

  assert.deepEqual(calls.tools, ["apply_patch"])
  assert.deepEqual(calls.events, ["session.status", "session.idle"])
})

test("questions and errors remain actionable alerts", async () => {
  const { calls, hooks } = await createHarness()
  const id = "session-3"

  await hooks["tool.execute.before"]({ sessionID: id, tool: "question" }, {})
  await hooks.event({ event: sessionEvent("session.error", id) })

  assert.deepEqual(calls.tools, ["question"])
  assert.deepEqual(calls.events, ["session.error"])
})

test("setup has one guarded notifier and no intermediate-event sounds", async () => {
  const setupDir = new URL("../", import.meta.url)
  const [config, windowsInstaller, unixInstaller] = await Promise.all([
    readFile(new URL("opencode.jsonc", setupDir), "utf8"),
    readFile(new URL("install.ps1", setupDir), "utf8"),
    readFile(new URL("install.sh", setupDir), "utf8"),
  ])

  assert.doesNotMatch(config, /^\s*"@mohak34\/opencode-notifier@/m)
  assert.match(windowsInstaller, /npm install[^\r\n]*'@mohak34\/opencode-notifier@0\.2\.8'/)
  assert.match(unixInstaller, /npm install[^\r\n]*'@mohak34\/opencode-notifier@0\.2\.8'/)

  for (const source of [windowsInstaller, unixInstaller]) {
    assert.match(source, /["']?subagent_complete["']?\s*[=:]\s*(?:\$false|false)/)
    assert.match(source, /["']?interrupted["']?\s*[=:]\s*(?:\$false|false)/)
    assert.match(source, /["']?user_cancelled["']?\s*[=:]\s*(?:\$false|false)/)
  }
})
