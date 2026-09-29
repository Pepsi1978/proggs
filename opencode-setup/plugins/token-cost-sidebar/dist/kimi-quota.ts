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

export type KimiMonthlyQuota = {
  usedPercent: number
  resetAt?: number
}

export function parseKimiMonthlyQuota(payload: any): KimiMonthlyQuota | undefined {
  // Never substitute limit_month_code, a weekly window, or Extra Usage for the shared total.
  const monthly = payload?.usages?.limit_month_total
  const raw = monthly?.used_ratio
  if (typeof raw !== "number" && (typeof raw !== "string" || raw.trim() === "")) return undefined
  const ratio = Number(raw)
  if (!Number.isFinite(ratio) || ratio < 0) return undefined
  const reset = typeof monthly?.reset_time === "string" ? Date.parse(monthly.reset_time) : NaN
  return {
    usedPercent: Math.min(100, ratio * 100),
    ...(Number.isFinite(reset) ? { resetAt: reset / 1_000 } : {}),
  }
}

export async function loadKimiMonthlyQuota(
  stateDirectory: string,
  providerID: string,
  fetcher: typeof fetch = fetch,
): Promise<KimiMonthlyQuota | undefined> {
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
  return parseKimiMonthlyQuota(await response.json())
}
