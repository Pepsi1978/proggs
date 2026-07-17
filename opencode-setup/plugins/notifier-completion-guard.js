import { spawn } from "node:child_process"
import { dirname } from "node:path"
import { fileURLToPath } from "node:url"

const WORK_TOOLS = new Set(["apply_patch", "edit", "write", "bash"])

function sessionId(event) {
  return event?.properties?.sessionID ?? event?.properties?.info?.sessionID ?? event?.properties?.info?.id ?? null
}

export async function createNotifierCompletionGuard(input, NotifierPlugin) {
  const notifier = await NotifierPlugin(input)
  const handleEvent = notifier.event
  const handleToolBefore = notifier["tool.execute.before"]
  const busySessions = new Set()
  const workedSessions = new Set()

  return {
    ...notifier,
    event: async (args) => {
      const event = args.event
      const id = sessionId(event)

      if (event.type === "message.updated" && event.properties?.info?.role === "user" && id) {
        busySessions.delete(id)
        workedSessions.delete(id)
        return
      }

      if (event.type === "session.status" && event.properties?.status?.type === "busy" && id) {
        busySessions.add(id)
        await handleEvent?.(args)
        return
      }

      if (event.type === "session.idle") {
        const wasBusy = id && busySessions.delete(id)
        const didWork = id && workedSessions.delete(id)
        if (!wasBusy || !didWork) return
        await handleEvent?.(args)
        return
      }

      if ((event.type === "session.error" || event.type === "session.deleted") && id) {
        busySessions.delete(id)
        workedSessions.delete(id)
      }
    },
    "permission.ask": async () => {},
    "tool.execute.before": async (toolInput, output) => {
      const id = toolInput?.sessionID
      if (id && WORK_TOOLS.has(toolInput.tool)) {
        workedSessions.add(id)
      }
      if (toolInput.tool === "question") {
        await handleToolBefore?.(toolInput, output)
      }
    },
  }
}

export default async function NotifierCompletionGuard(input) {
  const { default: NotifierPlugin } = await import("@mohak34/opencode-notifier")
  const guardedNotifier = await createNotifierCompletionGuard(input, NotifierPlugin)
  const configDirectory = dirname(dirname(fileURLToPath(import.meta.url)))
  const updater = fileURLToPath(new URL("./notifier-auto-updater.mjs", import.meta.url))
  try {
    const child = spawn("node", [updater, "--config-dir", configDirectory], {
      detached: true,
      stdio: "ignore",
      windowsHide: true,
    })
    child.on("error", async (error) => {
      try {
        await input.client?.app?.log?.({
          body: { service: "notifier-auto-updater", level: "warn", message: error.message },
        })
      } catch {
        // An unavailable updater must never affect the notifier or the OpenCode session.
      }
    })
    child.unref()
  } catch (error) {
    try {
      await input.client?.app?.log?.({
        body: { service: "notifier-auto-updater", level: "warn", message: error.message },
      })
    } catch {
      // An unavailable updater must never affect the notifier or the OpenCode session.
    }
  }
  return guardedNotifier
}
