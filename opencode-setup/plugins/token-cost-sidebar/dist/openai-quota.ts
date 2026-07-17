import { readFile } from "node:fs/promises"
import { join } from "node:path"

const WEEK_SECONDS = 7 * 24 * 60 * 60
const WEEKLY_WINDOW_MIN_SECONDS = 6 * 24 * 60 * 60
const USAGE_URL = "https://chatgpt.com/backend-api/wham/usage"

export type WeeklyQuota = {
  remainingPercent: number
  resetAt: number
}

type RateLimitWindow = {
  used_percent?: unknown
  limit_window_seconds?: unknown
  reset_at?: unknown
}

function finiteNumber(value: unknown): number | undefined {
  if (typeof value !== "number" || !Number.isFinite(value)) return undefined
  return value
}

export function parseWeeklyQuota(payload: any): WeeklyQuota | undefined {
  const windows: RateLimitWindow[] = [
    payload?.rate_limit?.primary_window,
    payload?.rate_limit?.secondary_window,
  ].filter(Boolean)
  const weekly = windows.find((window) => {
    const seconds = finiteNumber(window.limit_window_seconds)
    return seconds !== undefined && seconds >= WEEKLY_WINDOW_MIN_SECONDS && seconds <= WEEK_SECONDS
  })
  const usedPercent = finiteNumber(weekly?.used_percent)
  const resetAt = finiteNumber(weekly?.reset_at)
  if (usedPercent === undefined || resetAt === undefined) return undefined

  return {
    remainingPercent: Math.round(Math.max(0, Math.min(100, 100 - usedPercent))),
    resetAt,
  }
}

export async function loadOpenAIWeeklyQuota(
  stateDirectory: string,
  fetcher: typeof fetch = fetch,
): Promise<WeeklyQuota | undefined> {
  const auth = JSON.parse(await readFile(join(stateDirectory, "auth.json"), "utf8"))?.openai
  if (auth?.type !== "oauth" || typeof auth.access !== "string" || typeof auth.accountId !== "string") {
    return undefined
  }

  const response = await fetcher(USAGE_URL, {
    headers: {
      Authorization: `Bearer ${auth.access}`,
      "ChatGPT-Account-Id": auth.accountId,
    },
  })
  if (!response.ok) throw new Error(`OpenAI usage request failed with status ${response.status}`)
  return parseWeeklyQuota(await response.json())
}
