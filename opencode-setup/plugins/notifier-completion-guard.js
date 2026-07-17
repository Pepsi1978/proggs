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
      }

      if (event.type === "session.status" && event.properties?.status?.type === "busy" && id) {
        busySessions.add(id)
      }

      if (event.type === "session.idle") {
        const wasBusy = id && busySessions.delete(id)
        const didWork = id && workedSessions.delete(id)
        if (!wasBusy || !didWork) return
      }

      if ((event.type === "session.error" || event.type === "session.deleted") && id) {
        busySessions.delete(id)
        workedSessions.delete(id)
      }

      await handleEvent?.(args)
    },
    "tool.execute.before": async (toolInput, output) => {
      const id = toolInput?.sessionID
      if (id && WORK_TOOLS.has(toolInput.tool)) {
        workedSessions.add(id)
      }
      await handleToolBefore?.(toolInput, output)
    },
  }
}

export default async function NotifierCompletionGuard(input) {
  const { default: NotifierPlugin } = await import("@mohak34/opencode-notifier")
  return createNotifierCompletionGuard(input, NotifierPlugin)
}
