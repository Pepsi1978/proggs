import assert from "node:assert/strict"
import { mkdir, readFile, rm, writeFile } from "node:fs/promises"
import { dirname, join, resolve } from "node:path"
import { pathToFileURL } from "node:url"

function parseArguments(argv) {
  const values = {}
  for (let index = 0; index < argv.length; index += 2) {
    values[argv[index]?.replace(/^--/, "")] = argv[index + 1]
  }
  for (const required of ["candidate", "guard", "work-dir"]) {
    if (!values[required]) throw new Error(`Missing --${required}`)
  }
  return values
}

async function readMarker(path) {
  try {
    return (await readFile(path, "utf8")).split(/\r?\n/).filter(Boolean)
  } catch {
    return []
  }
}

async function waitForMarker(path, expectedCount) {
  const deadline = Date.now() + 5000
  while (Date.now() < deadline) {
    const values = await readMarker(path)
    if (values.length >= expectedCount) return values
    await new Promise((resolvePromise) => setTimeout(resolvePromise, 100))
  }
  return readMarker(path)
}

async function main() {
  const args = parseArguments(process.argv.slice(2))
  const workDirectory = resolve(args["work-dir"])
  const configPath = join(workDirectory, "contract-config.json")
  const markerPath = join(workDirectory, "events.log")
  const markerScript = join(dirname(resolve(args.guard)), "notifier-event-marker.mjs")
  await rm(workDirectory, { recursive: true, force: true })
  await mkdir(workDirectory, { recursive: true })

  const disabled = { sound: false, notification: false, command: false }
  const enabled = { sound: false, notification: false, command: true }
  const config = {
    sound: false,
    notification: false,
    suppressWhenFocused: false,
    minDuration: 0,
    command: {
      enabled: true,
      path: process.execPath,
      args: [markerScript, markerPath, "{event}"],
    },
    events: {
      permission: disabled,
      complete: enabled,
      subagent_complete: disabled,
      error: disabled,
      question: enabled,
      interrupted: disabled,
      user_cancelled: disabled,
      plan_exit: disabled,
      session_started: disabled,
      user_message: disabled,
      client_connected: disabled,
    },
  }
  await writeFile(configPath, `${JSON.stringify(config, null, 2)}\n`, "utf8")
  process.env.OPENCODE_CLIENT = "cli"
  process.env.OPENCODE_NOTIFIER_CONFIG_PATH = configPath

  const { createNotifierCompletionGuard } = await import(pathToFileURL(resolve(args.guard)).href)
  const { default: candidate } = await import(`${pathToFileURL(resolve(args.candidate)).href}?contract=${Date.now()}`)
  assert.equal(typeof candidate, "function", "candidate must export a default plugin function")

  const client = {
    session: {
      get: async () => ({ data: { title: "Notifier contract" } }),
      messages: async () => ({ data: [] }),
    },
  }
  const hooks = await createNotifierCompletionGuard({ client, directory: workDirectory }, candidate)
  assert.equal(typeof hooks.event, "function")
  assert.equal(typeof hooks["tool.execute.before"], "function")
  assert.equal(typeof hooks["permission.ask"], "function")

  const event = (type, id, properties = {}) => hooks.event({
    event: { type, properties: { sessionID: id, ...properties } },
  })

  await event("message.updated", "silent", { info: { role: "user", sessionID: "silent" } })
  await event("session.status", "silent", { status: { type: "busy" } })
  await event("session.idle", "silent")
  await event("permission.asked", "silent")
  await hooks["permission.ask"]()
  await hooks["tool.execute.before"]({ sessionID: "silent", tool: "plan_exit" }, {})
  await event("session.error", "silent", { error: { name: "ContractError" } })
  await event("session.deleted", "silent", { info: { id: "silent" } })

  await hooks["tool.execute.before"]({ sessionID: "question", tool: "question" }, {})

  await event("session.status", "complete", { status: { type: "busy" } })
  await hooks["tool.execute.before"]({ sessionID: "complete", tool: "bash" }, {})
  await event("session.idle", "complete")

  const marker = await waitForMarker(markerPath, 2)
  assert.deepEqual(marker, ["question", "complete"])
}

try {
  await main()
  process.exit(0)
} catch (error) {
  console.error(error.stack ?? error.message)
  process.exit(1)
}
