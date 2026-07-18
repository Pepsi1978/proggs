// Tokenverbrauch-Audit v1.4.0 - 18.07.2026, 20:48 Uhr

import { appendFile, mkdir } from "node:fs/promises"
import { dirname } from "node:path"
import {
  createAuditRecord,
  hashValue,
  resolveTokenUsageLogPath,
  shouldAuditAgent,
  shortSummary,
  systemIdentity,
} from "./lib/token-usage-audit-core.js"

function modelIdentity(model) {
  return {
    providerID: model?.providerID ?? model?.provider?.id ?? model?.provider,
    modelID: model?.modelID ?? model?.id,
  }
}

export const TokenUsageAudit = async ({ client, directory, worktree }) => {
  const logPath = resolveTokenUsageLogPath(worktree)
  const requests = new Map()
  const messages = new Map()
  const systems = new Map()
  const pendingSystems = new Map()
  const users = new Map()
  const tools = new Map()
  const sessions = new Map()
  const compacting = new Set()
  const previousRequests = new Map()
  const sequences = new Map()
  const seenParts = new Set()
  let appendQueue = Promise.resolve()

  const logFailure = (error) => client.app.log({
    body: {
      service: "token-usage-audit",
      level: "warn",
      message: `Tokenverbrauch konnte nicht geschrieben werden: ${error instanceof Error ? error.message : String(error)}`,
      extra: { logPath },
    },
  }).catch(() => undefined)

  const append = (record) => {
    appendQueue = appendQueue
      .then(async () => {
        await mkdir(dirname(logPath), { recursive: true })
        await appendFile(logPath, `${JSON.stringify(record)}\n`, "utf8")
      })
      .catch(logFailure)
    return appendQueue
  }

  const findMessage = async (sessionID, messageID) => {
    const cached = messages.get(messageID)
    if (cached) return cached
    try {
      const response = await client.session.messages({
        path: { id: sessionID },
        query: { directory },
      })
      const match = (response.data ?? []).find((item) => (item?.info ?? item)?.id === messageID)
      if (match) messages.set(messageID, match)
      return match
    } catch {
      return undefined
    }
  }

  return {
    "experimental.chat.system.transform": async (input, output) => {
      if (!input.sessionID) return
      pendingSystems.set(input.sessionID, {
        ...modelIdentity(input.model),
        system: [...output.system],
      })
    },
    "chat.message": async (input, output) => {
      const text = (output.parts ?? [])
        .filter((part) => part?.type === "text")
        .map((part) => part.text ?? "")
        .join(" ")
      users.set(input.sessionID, {
        userMessageID: output.message?.id ?? input.messageID,
        userSummary: shortSummary(text),
        userPromptHash: hashValue(text),
      })
    },
    "chat.params": async (input, output) => {
      const model = modelIdentity(input.model)
      const pendingSystem = pendingSystems.get(input.sessionID)
      pendingSystems.delete(input.sessionID)
      if (!shouldAuditAgent(input.agent)) return
      const previousSystem = systems.get(input.sessionID)
      const matchingSystem = pendingSystem
        && pendingSystem.providerID === model.providerID
        && pendingSystem.modelID === model.modelID
      const currentSystem = matchingSystem
        ? systemIdentity(pendingSystem.system, previousSystem)
        : previousSystem
          ? { ...previousSystem, systemChanged: false }
          : undefined
      if (matchingSystem) systems.set(input.sessionID, currentSystem)
      const sequence = (sequences.get(input.sessionID) ?? 0) + 1
      const previous = previousRequests.get(input.sessionID)
      const promptCacheKeyHash = hashValue(output.options?.promptCacheKey ?? input.sessionID)
      const pendingTools = [...new Set(tools.get(input.sessionID) ?? [])]
      sequences.set(input.sessionID, sequence)
      const request = {
        ...model,
        agent: input.agent,
        startedAt: new Date().toISOString(),
        sequence,
        serviceTier: output.options?.serviceTier ?? process.env.OPENCODE_LAUNCHER_SERVICE_TIER ?? "standard",
        serviceTierOption: output.options?.serviceTier ?? null,
        serviceTierLauncher: process.env.OPENCODE_LAUNCHER_SERVICE_TIER ?? null,
        promptCacheKeyHash,
        promptCacheKeyChanged: Boolean(previous?.promptCacheKeyHash && previous.promptCacheKeyHash !== promptCacheKeyHash),
        modelChanged: Boolean(previous && (previous.providerID !== model.providerID || previous.modelID !== model.modelID)),
        tools: pendingTools,
        compaction: compacting.delete(input.sessionID),
        parentSessionID: sessions.get(input.sessionID)?.parentID,
        isSubagent: Boolean(sessions.get(input.sessionID)?.parentID),
        ...users.get(input.sessionID),
        ...currentSystem,
      }
      requests.set(input.sessionID, request)
      previousRequests.set(input.sessionID, request)
      tools.set(input.sessionID, [])
    },
    "tool.execute.after": async (input) => {
      const current = tools.get(input.sessionID) ?? []
      current.push(input.tool)
      tools.set(input.sessionID, current)
    },
    "experimental.session.compacting": async (input) => {
      compacting.add(input.sessionID)
    },
    event: async ({ event }) => {
      if (event?.type === "session.created") {
        const info = event.properties?.info
        if (info?.id) sessions.set(info.id, { parentID: info.parentID })
        return
      }
      if (event?.type === "message.updated") {
        const info = event.properties?.info
        if (info?.id) messages.set(info.id, info)
        return
      }
      if (event?.type !== "message.part.updated") return
      const part = event.properties?.part
      if (part?.type !== "step-finish" || !part.id || seenParts.has(part.id)) return
      seenParts.add(part.id)
      const sessionID = event.properties?.sessionID ?? part.sessionID
      const message = await findMessage(sessionID, part.messageID)
      const request = requests.get(sessionID)
      const agent = (message?.info ?? message)?.agent ?? request?.agent
      if (!request || !shouldAuditAgent(agent)) return
      await append(createAuditRecord({ part, message, request }))
    },
  }
}
