import { readFile } from "node:fs/promises"
import { openAIAuthFileCandidates } from "./openai-quota"

// Official kimi-code managed-usage.ts: region-bound Coding credentials, not Open Platform keys.
const USAGE_URLS: Record<string, string> = {
  "kimi-code-plan-global": "https://api.kimi.ai/coding/v1/usages",
  "kimi-code-plan-cn": "https://api.kimi.com/coding/v1/usages",
  "kimi-for-coding": "https://api.kimi.com/coding/v1/usages",
}

export const KIMI_CODE_PROVIDERS = Object.keys(USAGE_URLS)

export function isKimiCodeProvider(providerID?: string): boolean {
  return !!providerID && Object.hasOwn(USAGE_URLS, providerID)
}

export type KimiQuotaWindow = {
  usedPercent: number
  resetAt?: number
}

export type KimiQuota = {
  monthly?: KimiQuotaWindow
  fiveHour?: KimiQuotaWindow
}

function nonNegativeNumber(raw: unknown): number | undefined {
  if (typeof raw !== "number" && (typeof raw !== "string" || raw.trim() === "")) return undefined
  const value = Number(raw)
  return Number.isFinite(value) && value >= 0 ? value : undefined
}

function parseQuotaWindow(window: any): KimiQuotaWindow | undefined {
  const ratio = nonNegativeNumber(window?.used_ratio)
  if (ratio === undefined) return undefined
  const reset = typeof window?.reset_time === "string" ? Date.parse(window.reset_time) : NaN
  return {
    usedPercent: Math.min(100, ratio * 100),
    ...(Number.isFinite(reset) ? { resetAt: reset / 1_000 } : {}),
  }
}

function parseLegacyFiveHour(payload: any, nowMs: number): KimiQuotaWindow | undefined {
  const units: Record<string, number> = {
    TIME_UNIT_SECOND: 1, TIME_UNIT_MINUTE: 60, TIME_UNIT_HOUR: 3600,
  }
  const candidates: KimiQuotaWindow[] = []
  for (const entry of Array.isArray(payload?.limits) ? payload.limits : []) {
    const duration = nonNegativeNumber(entry?.window?.duration)
    if (duration === undefined || duration * units[entry?.window?.timeUnit] !== 5 * 3600) continue
    const limit = nonNegativeNumber(entry?.detail?.limit)
    const used = nonNegativeNumber(entry?.detail?.used)
    const resetAt = typeof entry?.detail?.resetTime === "string" ? Date.parse(entry.detail.resetTime) / 1000 : NaN
    if (limit === undefined || limit === 0 || used === undefined || !Number.isFinite(resetAt) || resetAt * 1000 <= nowMs) continue
    candidates.push({ usedPercent: Math.min(100, used / limit * 100), resetAt })
  }
  // Never guess which limit is meant when a payload contains ambiguous 5h windows.
  return candidates.length === 1 ? candidates[0] : undefined
}

export function parseKimiQuota(payload: any, nowMs = Date.now()): KimiQuota | undefined {
  // Keep the shared monthly total and rolling 5h limit independent, even if one is absent.
  const monthly = parseQuotaWindow(payload?.usages?.limit_month_total)
  let fiveHour = parseQuotaWindow(payload?.usages?.limit_5h)
  const legacy = parseLegacyFiveHour(payload, nowMs)
  if (!fiveHour) {
    fiveHour = legacy
  } else if (fiveHour.usedPercent === 0 && legacy && legacy.usedPercent > 0) {
    // Live Kimi responses can contain a zero ratio alongside valid legacy usage.
    // Match the same window: timestamps differ by ~2s due to rounding/serialization.
    const sameReset = fiveHour.resetAt !== undefined && legacy.resetAt !== undefined
      && Math.abs(fiveHour.resetAt - legacy.resetAt) <= 5
    fiveHour = sameReset ? legacy : undefined
  }
  return monthly || fiveHour ? { monthly, fiveHour } : undefined
}

export function formatKimiResetCountdown(resetAt: number | undefined, nowMs: number): string {
  if (resetAt === undefined || !Number.isFinite(resetAt)) return "Reset n/v"
  const remainingMs = resetAt * 1_000 - nowMs
  if (remainingMs <= 0) return "Reset fällig"
  const minutes = Math.ceil(remainingMs / 60_000)
  const hours = Math.floor(minutes / 60)
  return hours > 0 ? `Reset in ${hours}h ${minutes % 60}min` : `Reset in ${minutes}min`
}

export async function loadKimiQuota(
  stateDirectory: string,
  providerID: string,
  fetcher: typeof fetch = fetch,
): Promise<KimiQuota | undefined> {
  if (!isKimiCodeProvider(providerID)) return undefined
  let token: string | undefined
  for (const authFile of openAIAuthFileCandidates(stateDirectory)) {
    try {
      const auth = JSON.parse(await readFile(authFile, "utf8"))?.[providerID]
      const value = auth?.type === "api" ? auth.key : auth?.type === "oauth" ? auth.access : undefined
      if (typeof value === "string" && value.trim()) {
        token = value.trim()
        break
      }
    } catch (error: any) {
      if (error?.code !== "ENOENT") throw new Error("Kimi-Anmeldedaten konnten nicht gelesen werden.")
    }
  }
  if (!token) return undefined
  const response = await fetcher(USAGE_URLS[providerID], {
    headers: { Authorization: `Bearer ${token}`, Accept: "application/json" },
    signal: AbortSignal.timeout(10_000),
    redirect: "error",
  })
  if (!response.ok) throw new Error(`Kimi-Kontingent: HTTP ${response.status}`)
  return parseKimiQuota(await response.json())
}
