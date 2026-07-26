import { createHash } from "node:crypto"
import { homedir } from "node:os"
import { join } from "node:path"

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

export function shouldAuditAgent(agent) {
  return agent !== "title"
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
      source: launcher.OPENLAUNCHER_SOURCE ?? null,
      model: launcher.OPENLAUNCHER_MODEL ?? null,
      serviceTier: request?.serviceTier ?? launcher.OPENLAUNCHER_SERVICE_TIER ?? "standard",
      requestedServiceTier: request?.serviceTierLauncher ?? launcher.OPENLAUNCHER_SERVICE_TIER ?? null,
    },
    request: {
      startedAt: request?.startedAt,
      sequence: request?.sequence,
      serviceTierOption: request?.serviceTierOption ?? null,
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
      responseServiceTier: part?.metadata?.openai?.serviceTier ?? null,
      serviceTierSemantics: "raw-response-metadata",
      rawUsage: telemetry.rawUsage ?? part?.metadata?.openai ?? null,
    },
  }
}
