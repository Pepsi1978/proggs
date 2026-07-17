import { describe, expect, test } from "bun:test"
import { mkdtemp, rm, writeFile } from "node:fs/promises"
import { tmpdir } from "node:os"
import { join } from "node:path"
import { isCompletedOpenAIMessage, loadOpenAIWeeklyQuota, parseWeeklyQuota } from "../dist/openai-quota"

describe("OpenAI weekly quota", () => {
  test("selects the weekly window and converts used to remaining percent", () => {
    expect(parseWeeklyQuota({
      rate_limit: {
        primary_window: { used_percent: 10, limit_window_seconds: 18_000, reset_at: 1 },
        secondary_window: { used_percent: 34, limit_window_seconds: 604_800, reset_at: 1_784_796_030 },
      },
    })).toEqual({ remainingPercent: 66, resetAt: 1_784_796_030 })
  })

  test("supports plans whose primary window is weekly", () => {
    expect(parseWeeklyQuota({
      rate_limit: {
        primary_window: { used_percent: 34.4, limit_window_seconds: 604_800, reset_at: 1_784_796_030 },
        secondary_window: null,
      },
    })).toEqual({ remainingPercent: 66, resetAt: 1_784_796_030 })
  })

  test("refreshes only after completed OpenAI assistant messages", () => {
    expect(isCompletedOpenAIMessage({
      role: "assistant",
      providerID: "openai",
      time: { completed: 1_784_300_000 },
    })).toBe(true)
    expect(isCompletedOpenAIMessage({
      role: "assistant",
      providerID: "openai",
      time: { created: 1_784_300_000 },
    })).toBe(false)
    expect(isCompletedOpenAIMessage({
      role: "assistant",
      providerID: "anthropic",
      time: { completed: 1_784_300_000 },
    })).toBe(false)
  })

  test("reads OpenCode OAuth credentials without exposing them", async () => {
    const directory = await mkdtemp(join(tmpdir(), "opencode-quota-"))
    try {
      await writeFile(join(directory, "auth.json"), JSON.stringify({
        openai: { type: "oauth", access: "secret", accountId: "account" },
      }))
      const fetcher = (async (_url: string, init?: RequestInit) => {
        expect(init?.headers).toEqual({
          Authorization: "Bearer secret",
          "ChatGPT-Account-Id": "account",
        })
        return new Response(JSON.stringify({
          rate_limit: {
            primary_window: { used_percent: 34, limit_window_seconds: 604_800, reset_at: 1_784_796_030 },
          },
        }))
      }) as typeof fetch

      expect(await loadOpenAIWeeklyQuota(directory, fetcher)).toEqual({
        remainingPercent: 66,
        resetAt: 1_784_796_030,
      })
    } finally {
      await rm(directory, { recursive: true, force: true })
    }
  })
})
