import type { TokenUsage } from "./pricing"

export type SessionUsageRecord = {
  id: string
  providerID?: string
  modelID?: string
  usage: TokenUsage
  recordedCostUsd: number
}

export type SessionUsageSnapshot = {
  input: number
  output: number
  reasoning: number
  cacheRead: number
  cacheWrite: number
  matchedMessages: number
  records: SessionUsageRecord[]
}

export type SessionUsageLedger = Map<string, SessionUsageRecord>

function safeNumber(value: unknown): number {
  if (typeof value === "number" && Number.isFinite(value)) return Math.max(0, value)
  if (typeof value === "string" && value !== "") {
    const parsed = Number(value)
    if (Number.isFinite(parsed)) return Math.max(0, parsed)
  }
  return 0
}

function firstNumber(...values: unknown[]): number {
  for (const value of values) {
    const parsed = safeNumber(value)
    if (parsed > 0) return parsed
  }
  return 0
}

function messageInfo(message: any): any {
  return message?.info ?? message
}

function usageOf(message: any): TokenUsage {
  const info = messageInfo(message)
  return {
    input: safeNumber(info?.tokens?.input),
    output: safeNumber(info?.tokens?.output),
    reasoning: safeNumber(info?.tokens?.reasoning),
    cacheRead: safeNumber(info?.tokens?.cache?.read),
    cacheWrite: safeNumber(info?.tokens?.cache?.write),
  }
}

function storedRecord(value: unknown): SessionUsageRecord | undefined {
  const record = value as Partial<SessionUsageRecord> | undefined
  if (!record || typeof record.id !== "string" || record.id === "") return undefined
  return {
    id: record.id,
    providerID: typeof record.providerID === "string" ? record.providerID : undefined,
    modelID: typeof record.modelID === "string" ? record.modelID : undefined,
    usage: usageOf({ tokens: {
      input: record.usage?.input,
      output: record.usage?.output,
      reasoning: record.usage?.reasoning,
      cache: { read: record.usage?.cacheRead, write: record.usage?.cacheWrite },
    } }),
    recordedCostUsd: safeNumber(record.recordedCostUsd),
  }
}

export function createSessionUsageLedger(stored?: unknown): SessionUsageLedger {
  const ledger: SessionUsageLedger = new Map()
  if (!Array.isArray(stored)) return ledger
  for (const value of stored) {
    const record = storedRecord(value)
    if (record) ledger.set(record.id, record)
  }
  return ledger
}

export function observeAssistantMessage(ledger: SessionUsageLedger, message: any): boolean {
  const info = messageInfo(message)
  if (info?.role !== "assistant" || typeof info?.id !== "string" || info.id === "") return false

  const previous = ledger.get(info.id)
  const usage = usageOf(info)
  const next: SessionUsageRecord = {
    id: info.id,
    providerID: typeof info.providerID === "string" ? info.providerID : previous?.providerID,
    modelID: typeof info.modelID === "string" ? info.modelID : previous?.modelID,
    usage: {
      input: Math.max(previous?.usage.input ?? 0, usage.input),
      output: Math.max(previous?.usage.output ?? 0, usage.output),
      reasoning: Math.max(previous?.usage.reasoning ?? 0, usage.reasoning),
      cacheRead: Math.max(previous?.usage.cacheRead ?? 0, usage.cacheRead),
      cacheWrite: Math.max(previous?.usage.cacheWrite ?? 0, usage.cacheWrite),
    },
    recordedCostUsd: Math.max(
      previous?.recordedCostUsd ?? 0,
      firstNumber(info?.cost, info?.usage?.cost, info?.metrics?.cost),
    ),
  }

  if (previous && JSON.stringify(previous) === JSON.stringify(next)) return false
  ledger.set(next.id, next)
  return true
}

export function observeAssistantMessages(ledger: SessionUsageLedger, messages: readonly any[]): boolean {
  let changed = false
  for (const message of messages) changed = observeAssistantMessage(ledger, message) || changed
  return changed
}

export function sessionUsageSnapshot(ledger: SessionUsageLedger): SessionUsageSnapshot {
  const result: SessionUsageSnapshot = {
    input: 0,
    output: 0,
    reasoning: 0,
    cacheRead: 0,
    cacheWrite: 0,
    matchedMessages: ledger.size,
    records: [...ledger.values()],
  }
  for (const record of result.records) {
    result.input += record.usage.input
    result.output += record.usage.output
    result.reasoning += record.usage.reasoning
    result.cacheRead += record.usage.cacheRead
    result.cacheWrite += record.usage.cacheWrite
  }
  return result
}

export function serializeSessionUsage(ledger: SessionUsageLedger): SessionUsageRecord[] {
  return [...ledger.values()]
}
