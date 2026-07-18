// Tokenverbrauch-Audit v1.1.1 - 18.07.2026, 12:18 Uhr

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

export function shortSummary(value, maxLength = 180) {
  const compact = String(value ?? "")
    .replace(/\bsk-[A-Za-z0-9_-]{8,}\b/g, "[SECRET]")
    .replace(/((?:api|access|auth)[_-]?(?:key|token)\s*[:=]\s*)\S+/gi, "$1[SECRET]")
    .replace(/Bearer\s+\S+/gi, "Bearer [SECRET]")
    .replace(/\s+/g, " ")
    .trim()
  if (compact.length <= maxLength) return compact
  return `${compact.slice(0, Math.max(0, maxLength - 3))}...`
}

export function systemIdentity(system, previous) {
  const text = (system ?? []).join("\n")
  const systemHash = hashValue(text)
  return {
    systemHash,
    systemCharacters: text.length,
    systemChanged: Boolean(previous?.systemHash && previous.systemHash !== systemHash),
  }
}

export function classifyAttribution(request, usage) {
  const read = safeNumber(usage?.cacheRead)
  const write = safeNumber(usage?.cacheWrite)
  let code = "no_cache_activity"
  let confidence = "high"
  if (request?.isSubagent && request?.sequence === 1) code = "new_subagent"
  else if (request?.sequence === 1) code = "cold_session"
  else if (request?.compaction) code = "compaction"
  else if (request?.modelChanged) code = "model_or_variant_changed"
  else if (request?.promptCacheKeyChanged) code = "prompt_cache_key_changed"
  else if (request?.systemChanged) code = "stable_prefix_changed"
  else if (request?.tools?.length) code = "post_tool_continuation"
  else if (write > 0 && read > 0) code = "partial_hit"
  else if (write > 0) {
    code = "normal_turn_growth"
    confidence = "medium"
  } else if (read > 0) code = "cache_hit"

  const cause = {
    new_subagent: "neue Subagent-Session mit eigenem Kontext",
    cold_session: "erster Modellaufruf der Session",
    compaction: "Compress beziehungsweise Kontextkomprimierung",
    model_or_variant_changed: "Modell- oder Variantenwechsel",
    prompt_cache_key_changed: "geänderter Prompt-Cache-Key",
    stable_prefix_changed: "geänderter Systemprompt-Präfix",
    post_tool_continuation: `Fortsetzung nach Tools: ${(request?.tools ?? []).join(", ")}`,
    partial_hit: "teilweise wiederverwendeter und teilweise neuer Prompt-Präfix",
    normal_turn_growth: "normal gewachsener Gesprächskontext",
    cache_hit: "wiederverwendeter stabiler Prompt-Präfix",
    no_cache_activity: "keine gemeldete Cache-Aktivität",
  }[code]

  return {
    code,
    confidence,
    responsible: {
      userRequest: request?.userSummary || null,
      tools: request?.tools ?? [],
      systemChanged: Boolean(request?.systemChanged),
      modelChanged: Boolean(request?.modelChanged),
      promptCacheKeyChanged: Boolean(request?.promptCacheKeyChanged),
      compaction: Boolean(request?.compaction),
      parentSessionID: request?.parentSessionID ?? null,
    },
    readSummary: read > 0
      ? `${read} Cache-Read-Tokens durch ${cause}.`
      : "Keine Cache-Read-Tokens gemeldet.",
    writeSummary: write > 0
      ? `${write} Cache-Write-Tokens durch ${cause}.`
      : "Keine Cache-Write-Tokens gemeldet.",
  }
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
  const usage = {
    total: safeNumber(tokens.total),
    input: safeNumber(tokens.input),
    output: safeNumber(tokens.output),
    reasoning: safeNumber(tokens.reasoning),
    cacheRead: read,
    cacheWrite: write,
    cacheReadReported: readReported,
    cacheWriteReported: writeReported,
  }
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
      userMessageID: request?.userMessageID,
      userSummary: request?.userSummary,
      userPromptHash: request?.userPromptHash,
    },
    usage,
    attribution: classifyAttribution(request, usage),
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
