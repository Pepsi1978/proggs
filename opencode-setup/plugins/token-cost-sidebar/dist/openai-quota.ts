import { readFile } from "node:fs/promises"
import { homedir } from "node:os"
import { basename, dirname, join } from "node:path"

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

export function isCompletedOpenAIMessage(message: any): boolean {
  return message?.role === "assistant"
    && message?.providerID === "openai"
    && finiteNumber(message?.time?.completed) !== undefined
}

export function openAIAuthFileCandidates(
  stateDirectory: string,
  homeDirectory = homedir(),
  xdgDataHome = process.env.XDG_DATA_HOME,
): string[] {
  const candidates = [join(stateDirectory, "auth.json")]
  const stateParent = dirname(stateDirectory)
  if (basename(stateParent) === "state") {
    candidates.push(join(dirname(stateParent), "share", basename(stateDirectory), "auth.json"))
  }
  if (xdgDataHome) candidates.push(join(xdgDataHome, "opencode", "auth.json"))
  candidates.push(join(homeDirectory, ".local", "share", "opencode", "auth.json"))
  return [...new Set(candidates)]
}

export async function loadOpenAIWeeklyQuota(
  stateDirectory: string,
  fetcher: typeof fetch = fetch,
): Promise<WeeklyQuota | undefined> {
  let auth: any
  for (const authFile of openAIAuthFileCandidates(stateDirectory)) {
    try {
      auth = JSON.parse(await readFile(authFile, "utf8"))?.openai
      if (auth) break
    } catch (error: any) {
      if (error?.code !== "ENOENT") throw error
    }
  }
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
