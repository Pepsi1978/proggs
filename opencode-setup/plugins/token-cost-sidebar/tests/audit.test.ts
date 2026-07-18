import { describe, expect, test } from "bun:test"
import { createAuditRecord, hashValue, resolveTokenUsageLogPath } from "../../token-usage-audit.js"

describe("token usage audit", () => {
  test("writes one complete provider-reported model step", () => {
    const record = createAuditRecord({
      timestamp: new Date("2026-07-18T10:00:00.000Z"),
      launcher: {
        OPENCODE_LAUNCHER_SOURCE: "OpenCodeLauncher",
        OPENCODE_LAUNCHER_MODEL: "openai/gpt-5.6-sol-fast",
        OPENCODE_LAUNCHER_SERVICE_TIER: "priority",
      },
      message: { role: "assistant", providerID: "openai", modelID: "gpt-5.6-sol-fast", agent: "build" },
      request: { sequence: 2, startedAt: "2026-07-18T09:59:59.000Z", promptCacheKeyHash: "abc" },
      part: {
        id: "prt_1",
        sessionID: "ses_1",
        messageID: "msg_1",
        type: "step-finish",
        reason: "stop",
        cost: 1.25,
        tokens: { total: 150, input: 10, output: 20, reasoning: 5, cache: { read: 100, write: 15 } },
        metadata: {
          opencode: { cacheReadReported: true, cacheWriteReported: true, rawUsage: { input_tokens: 125 } },
          openai: { responseId: "resp_1", serviceTier: "priority" },
        },
      },
    })
    expect(record).toMatchObject({
      schemaVersion: 1,
      date: "2026-07-18",
      modelID: "gpt-5.6-sol-fast",
      launcher: { serviceTier: "priority" },
      usage: { cacheRead: 100, cacheWrite: 15, cacheReadReported: true, cacheWriteReported: true },
      recordedCostUsd: 1.25,
      provider: { responseID: "resp_1", serviceTier: "priority" },
    })
  })

  test("marks an absent zero-valued cache field as unknown", () => {
    const record = createAuditRecord({ part: { tokens: { cache: { read: 0, write: 0 } } } })
    expect(record.usage.cacheReadReported).toBeNull()
    expect(record.usage.cacheWriteReported).toBeNull()
  })

  test("keeps the requested repo path and hashes sensitive values", () => {
    expect(resolveTokenUsageLogPath("C:\\Users\\frank\\proggs", {}, "C:\\Users\\frank")).toEndWith(
      "proggs\\opencode-setup\\Tokenverbrauch.jsonl",
    )
    expect(hashValue("secret")).toHaveLength(64)
    expect(hashValue("secret")).not.toContain("secret")
  })
})
