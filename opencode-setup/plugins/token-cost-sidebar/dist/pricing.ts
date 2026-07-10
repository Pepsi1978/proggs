export type TokenUsage = {
  input: number
  output: number
  reasoning: number
  cacheRead: number
  cacheWrite: number
}

export type UsageRecord = {
  usage: TokenUsage
  recordedCostUsd: number
}

let catalogPromise: Promise<any> | undefined

function safeNumber(value: unknown): number {
  if (typeof value === "number" && Number.isFinite(value)) return value
  if (typeof value === "string" && value !== "") {
    const parsed = Number(value)
    if (Number.isFinite(parsed)) return parsed
  }
  return 0
}

function normalizePrice(value: unknown): number {
  const parsed = safeNumber(value)
  if (parsed <= 0) return 0
  return parsed / 1_000_000
}

function priceSource(model: any): any {
  return model?.cost ?? model?.pricing ?? model?.price ?? model?.info?.cost
}

export function readPricing(model: any, contextTokens = 0) {
  const base = priceSource(model) ?? {}
  const tiers = Array.isArray(base?.tiers) ? base.tiers : []
  const tier = tiers
    .filter((item: any) => item?.tier?.type === "context" && contextTokens > safeNumber(item?.tier?.size))
    .sort((a: any, b: any) => safeNumber(b?.tier?.size) - safeNumber(a?.tier?.size))[0]
  const price = tier ? { ...base, ...tier } : base

  return {
    input: normalizePrice(price?.input ?? price?.prompt),
    output: normalizePrice(price?.output ?? price?.completion),
    reasoning: normalizePrice(price?.reasoning ?? price?.output ?? price?.completion),
    cacheRead: normalizePrice(price?.cache_read ?? price?.cacheRead ?? price?.cache?.read),
    cacheWrite: normalizePrice(price?.cache_write ?? price?.cacheWrite ?? price?.cache?.write),
  }
}

export function hasPositivePricing(model: any): boolean {
  const price = readPricing(model)
  return (
    price.input > 0 ||
    price.output > 0 ||
    price.reasoning > 0 ||
    price.cacheRead > 0 ||
    price.cacheWrite > 0
  )
}

export function hasKnownPricing(model: any): boolean {
  const price = priceSource(model)
  if (!price || typeof price !== "object") return false
  return ["input", "prompt", "output", "completion", "reasoning", "cache_read", "cacheRead", "cache_write", "cacheWrite"].some(
    (key) => Object.hasOwn(price, key),
  )
}

export function calculateUsageCost(model: any, usage: TokenUsage): number {
  const contextTokens = usage.input + usage.cacheRead + usage.cacheWrite
  const price = readPricing(model, contextTokens)
  return (
    usage.input * price.input +
    usage.output * price.output +
    usage.reasoning * price.reasoning +
    usage.cacheRead * (price.cacheRead || price.input) +
    usage.cacheWrite * (price.cacheWrite || price.input)
  )
}

export function calculateSessionCost(model: any, records: UsageRecord[]) {
  const pricingAvailable = hasKnownPricing(model)
  let usedRecorded = false
  let usedCalculated = false
  let missingUnpriced = false
  let usd = 0

  for (const record of records) {
    if (record.recordedCostUsd > 0) {
      usd += record.recordedCostUsd
      usedRecorded = true
    } else if (pricingAvailable) {
      usd += calculateUsageCost(model, record.usage)
      usedCalculated = true
    } else {
      missingUnpriced = true
    }
  }

  return { usd, usedRecorded, usedCalculated, missingUnpriced, pricingAvailable }
}

export function findCatalogModel(catalog: any, providerID?: string, modelID?: string): any {
  if (!providerID || !modelID) return undefined
  return catalog?.[providerID]?.models?.[modelID]
}

export function selectPricingModel(embeddedModel: any, catalogModel: any): any {
  return catalogModel ?? embeddedModel
}

export function commitIfCurrent<T>(
  requestKey: string,
  currentKey: () => string,
  commit: (value: T) => void,
  value: T,
): boolean {
  if (requestKey !== currentKey()) return false
  commit(value)
  return true
}

async function fetchCatalog(fetcher: typeof fetch): Promise<any> {
  const response = await fetcher("https://models.dev/api.json")
  if (!response.ok) throw new Error(`models.dev: HTTP ${response.status}`)
  return response.json()
}

async function getCatalog(fetcher: typeof fetch): Promise<any> {
  if (fetcher !== fetch) return fetchCatalog(fetcher)
  catalogPromise ??= fetchCatalog(fetch).catch((error) => {
    catalogPromise = undefined
    throw error
  })
  return catalogPromise
}

export async function loadCatalogModel(
  providerID?: string,
  modelID?: string,
  fetcher: typeof fetch = fetch,
  retryDelayMs = 1_000,
): Promise<any> {
  if (!providerID || !modelID) return undefined
  for (let attempt = 0; attempt < 2; attempt++) {
    try {
      return findCatalogModel(await getCatalog(fetcher), providerID, modelID)
    } catch (error) {
      if (attempt === 1) throw error
      await new Promise((resolve) => setTimeout(resolve, retryDelayMs))
    }
  }
}
