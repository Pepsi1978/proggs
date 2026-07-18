// Tokenverbrauch-Audit v1.2.0 - 18.07.2026, 12:54 Uhr

import { appendFile, mkdir } from "node:fs/promises"
import { dirname } from "node:path"
import {
  createAuditRecord,
  hashValue,
  resolveTokenUsageLogPath,
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
      const previous = systems.get(input.sessionID)
      const current = systemIdentity(output.system, previous)
      systems.set(input.sessionID, current)
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
        promptCacheKeyHash,
        promptCacheKeyChanged: Boolean(previous?.promptCacheKeyHash && previous.promptCacheKeyHash !== promptCacheKeyHash),
        modelChanged: Boolean(previous && (previous.providerID !== model.providerID || previous.modelID !== model.modelID)),
        tools: pendingTools,
        compaction: compacting.delete(input.sessionID),
        parentSessionID: sessions.get(input.sessionID)?.parentID,
        isSubagent: Boolean(sessions.get(input.sessionID)?.parentID),
        ...users.get(input.sessionID),
        ...systems.get(input.sessionID),
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
      await append(createAuditRecord({ part, message, request: requests.get(sessionID) }))
    },
  }
}
