import { describe, expect, test } from "bun:test"
import { readdir } from "node:fs/promises"
import {
  classifyAttribution,
  createAuditRecord,
  hashValue,
  resolveTokenUsageLogPath,
  shortSummary,
  systemIdentity,
} from "../../lib/token-usage-audit-core.js"

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
      request: {
        sequence: 2,
        startedAt: "2026-07-18T09:59:59.000Z",
        promptCacheKeyHash: "abc",
        userPromptHash: "user-hash",
      },
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
      request: { userPromptHash: "user-hash" },
      usage: { cacheRead: 100, cacheWrite: 15, cacheReadReported: true, cacheWriteReported: true },
      recordedCostUsd: 1.25,
      provider: { responseID: "resp_1", serviceTier: "priority" },
      attribution: { code: "partial_hit" },
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

  test("redacts secrets and records only a bounded user summary", () => {
    const summary = shortSummary("Nutze api_key=abcdef123456 und Bearer secret-token für diesen langen Auftrag", 60)
    expect(summary).not.toContain("abcdef123456")
    expect(summary).not.toContain("secret-token")
    expect(summary.length).toBeLessThanOrEqual(60)
  })

  test("classifies structural write causes before heuristic causes", () => {
    expect(classifyAttribution({ sequence: 1, isSubagent: true }, { cacheWrite: 100 }).code).toBe("new_subagent")
    expect(classifyAttribution({ sequence: 1 }, { cacheWrite: 100 }).code).toBe("cold_session")
    expect(classifyAttribution({ sequence: 2, compaction: true }, { cacheWrite: 100 }).code).toBe("compaction")
    expect(classifyAttribution({ sequence: 2, modelChanged: true }, { cacheWrite: 100 }).code).toBe("model_or_variant_changed")
    expect(classifyAttribution({ sequence: 2, systemChanged: true }, { cacheWrite: 100 }).code).toBe("stable_prefix_changed")
    expect(classifyAttribution({ sequence: 2, tools: ["read", "bash"] }, { cacheWrite: 100 }).code).toBe("post_tool_continuation")
    expect(classifyAttribution({ sequence: 2 }, { cacheRead: 100, cacheWrite: 10 }).code).toBe("partial_hit")
    expect(classifyAttribution({ sequence: 2 }, { cacheRead: 100, cacheWrite: 0 }).code).toBe("cache_hit")
  })

  test("attributes a changed system prefix to the request created after the transform", () => {
    const initial = systemIdentity(["stable prefix"])
    expect(initial.systemChanged).toBeFalse()
    expect(systemIdentity(["stable prefix"], initial).systemChanged).toBeFalse()
    expect(systemIdentity(["changed prefix"], initial)).toMatchObject({ systemChanged: true })
  })

  test("auto-loaded plugin entry points export exactly one plugin function", async () => {
    const pluginsDirectory = new URL("../../", import.meta.url)
    const entries = (await readdir(pluginsDirectory)).filter((name) => name.endsWith(".js"))
    entries.push("windows/terminal-task-title.js")

    for (const entry of entries) {
      const source = await Bun.file(new URL(entry, pluginsDirectory)).text()
      const exports = source.match(/^export\s+(?:(?:default\s+)?(?:async\s+)?function|const)\s+/gm) ?? []
      expect(exports, entry).toHaveLength(1)
    }
  })

  test("terminal title integration is inert in headless runs", async () => {
    const { default: terminalTaskTitle } = await import("../../windows/terminal-task-title.js")
    expect(await terminalTaskTitle({})).toEqual({})
  })
})
