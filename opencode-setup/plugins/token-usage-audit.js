// Tokenverbrauch-Audit v1.0.0 - 18.07.2026, 11:58 Uhr

import { appendFile, mkdir } from "node:fs/promises"
import { createHash } from "node:crypto"
import { homedir } from "node:os"
import { dirname, join } from "node:path"

const LOG_NAME = "Tokenverbrauch.jsonl"

function safeNumber(value) {
  const parsed = typeof value === "number" ? value : Number(value)
  return Number.isFinite(parsed) ? Math.max(0, parsed) : 0
}

export function hashValue(value) {
  if (value === undefined || value === null || value === "") return undefined
  return createHash("sha256").update(typeof value === "string" ? value : JSON.stringify(value)).digest("hex")
}

export function resolveTokenUsageLogPath(worktree, env = process.env, home = homedir()) {
  if (env.OPENCODE_TOKEN_USAGE_LOG) return env.OPENCODE_TOKEN_USAGE_LOG
  if (worktree) return join(worktree, "opencode-setup", LOG_NAME)
  return join(home, "proggs", "opencode-setup", LOG_NAME)
}

function modelIdentity(model) {
  return {
    providerID: model?.providerID ?? model?.provider?.id ?? model?.provider,
    modelID: model?.modelID ?? model?.id,
  }
}

function messageIdentity(message) {
  const info = message?.info ?? message
  return {
    providerID: info?.providerID,
    modelID: info?.modelID,
    agent: info?.agent,
  }
}

export function createAuditRecord({ part, message, request, launcher = process.env, timestamp = new Date() }) {
  const info = messageIdentity(message)
  const tokens = part?.tokens ?? {}
  const cache = tokens.cache ?? {}
  const telemetry = part?.metadata?.opencode ?? {}
  const read = safeNumber(cache.read)
  const write = safeNumber(cache.write)
  const readReported = typeof telemetry.cacheReadReported === "boolean" ? telemetry.cacheReadReported : read > 0 ? true : null
  const writeReported = typeof telemetry.cacheWriteReported === "boolean" ? telemetry.cacheWriteReported : write > 0 ? true : null
  return {
    schemaVersion: 1,
    timestamp: timestamp.toISOString(),
    date: timestamp.toISOString().slice(0, 10),
    event: "model-step",
    sessionID: part?.sessionID,
    messageID: part?.messageID,
    partID: part?.id,
    reason: part?.reason,
    providerID: request?.providerID ?? info.providerID,
    modelID: request?.modelID ?? info.modelID,
    agent: request?.agent ?? info.agent,
    launcher: {
      source: launcher.OPENCODE_LAUNCHER_SOURCE ?? null,
      model: launcher.OPENCODE_LAUNCHER_MODEL ?? null,
      serviceTier: request?.serviceTier ?? launcher.OPENCODE_LAUNCHER_SERVICE_TIER ?? "standard",
    },
    request: {
      startedAt: request?.startedAt,
      sequence: request?.sequence,
      promptCacheKeyHash: request?.promptCacheKeyHash,
      systemHash: request?.systemHash,
      systemCharacters: request?.systemCharacters,
    },
    usage: {
      total: safeNumber(tokens.total),
      input: safeNumber(tokens.input),
      output: safeNumber(tokens.output),
      reasoning: safeNumber(tokens.reasoning),
      cacheRead: read,
      cacheWrite: write,
      cacheReadReported: readReported,
      cacheWriteReported: writeReported,
    },
    recordedCostUsd: safeNumber(part?.cost),
    provider: {
      responseID: part?.metadata?.openai?.responseId ?? null,
      serviceTier: part?.metadata?.openai?.serviceTier ?? null,
      rawUsage: telemetry.rawUsage ?? part?.metadata?.openai ?? null,
    },
  }
}

export const TokenUsageAudit = async ({ client, directory, worktree }) => {
  const logPath = resolveTokenUsageLogPath(worktree)
  const requests = new Map()
  const messages = new Map()
  const systems = new Map()
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
      const system = output.system.join("\n")
      systems.set(input.sessionID, { systemHash: hashValue(system), systemCharacters: system.length })
    },
    "chat.params": async (input, output) => {
      const model = modelIdentity(input.model)
      const sequence = (sequences.get(input.sessionID) ?? 0) + 1
      sequences.set(input.sessionID, sequence)
      requests.set(input.sessionID, {
        ...model,
        agent: input.agent,
        startedAt: new Date().toISOString(),
        sequence,
        serviceTier: output.options?.serviceTier ?? process.env.OPENCODE_LAUNCHER_SERVICE_TIER ?? "standard",
        promptCacheKeyHash: hashValue(output.options?.promptCacheKey ?? input.sessionID),
        ...systems.get(input.sessionID),
      })
    },
    event: async ({ event }) => {
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
