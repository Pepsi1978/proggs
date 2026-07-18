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
  pricingModel?: any
}

export type UsageCostBreakdown = {
  inputUsd: number
  cacheReadUsd: number
  cacheWriteUsd: number
  cacheUsd: number
  outputUsd: number
  reasoningUsd: number
  usd: number
}

let catalogPromise: Promise<any> | undefined

const OPENAI_PRIORITY_COST: Record<string, Record<string, number>> = {
  "gpt-5.6-sol": { input: 10, output: 60, cache_read: 1, cache_write: 12.5 },
  "gpt-5.6-terra": { input: 5, output: 30, cache_read: 0.5, cache_write: 6.25 },
  "gpt-5.6-luna": { input: 2, output: 12, cache_read: 0.2, cache_write: 2.5 },
  "gpt-5.5": { input: 12.5, output: 75, cache_read: 1.25 },
}

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

function contextualPrice(model: any, contextTokens: number): any {
  const base = priceSource(model) ?? {}
  const tiers = Array.isArray(base?.tiers) ? base.tiers : []
  const tier = tiers
    .filter((item: any) => item?.tier?.type === "context" && contextTokens > safeNumber(item?.tier?.size))
    .sort((a: any, b: any) => safeNumber(b?.tier?.size) - safeNumber(a?.tier?.size))[0]
  return tier ? { ...base, ...tier } : base
}

export function readPricing(model: any, contextTokens = 0) {
  const price = contextualPrice(model, contextTokens)

  return {
    input: normalizePrice(price?.input ?? price?.prompt),
    output: normalizePrice(price?.output ?? price?.completion),
    reasoning: normalizePrice(price?.reasoning ?? price?.output ?? price?.completion),
    cacheRead: normalizePrice(price?.cache_read ?? price?.cacheRead ?? price?.cache?.read),
    cacheWrite: normalizePrice(price?.cache_write ?? price?.cacheWrite ?? price?.cache?.write),
  }
}

export function readPricingPerMillion(model: any, contextTokens = 0) {
  const price = readPricing(model, contextTokens)
  return {
    input: price.input * 1_000_000,
    output: price.output * 1_000_000,
    reasoning: price.reasoning * 1_000_000,
    cacheRead: price.cacheRead * 1_000_000,
    cacheWrite: price.cacheWrite * 1_000_000,
  }
}

export function readAvailablePricingPerMillion(model: any, contextTokens = 0) {
  const price = contextualPrice(model, contextTokens)
  const output = price?.output ?? price?.completion
  const available = (value: unknown) => hasPrice(value) ? safeNumber(value) : undefined
  return {
    input: available(price?.input ?? price?.prompt),
    output: available(output),
    reasoning: available(price?.reasoning ?? output),
    cacheRead: available(price?.cache_read ?? price?.cacheRead ?? price?.cache?.read),
    cacheWrite: available(price?.cache_write ?? price?.cacheWrite ?? price?.cache?.write),
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

function hasPrice(value: unknown): boolean {
  if (typeof value === "number") return Number.isFinite(value) && value >= 0
  if (typeof value === "string" && value !== "") {
    const parsed = Number(value)
    return Number.isFinite(parsed) && parsed >= 0
  }
  return false
}

function hasUsagePricing(model: any, usage: TokenUsage): boolean {
  const price = contextualPrice(model, usage.input + usage.cacheRead + usage.cacheWrite)
  const input = price?.input ?? price?.prompt
  const output = price?.output ?? price?.completion
  const reasoning = price?.reasoning ?? output
  const cacheRead = price?.cache_read ?? price?.cacheRead ?? price?.cache?.read
  const cacheWrite = price?.cache_write ?? price?.cacheWrite ?? price?.cache?.write
  return (
    (usage.input === 0 || hasPrice(input)) &&
    (usage.output === 0 || hasPrice(output)) &&
    (usage.reasoning === 0 || hasPrice(reasoning)) &&
    (usage.cacheRead === 0 || hasPrice(cacheRead)) &&
    (usage.cacheWrite === 0 || hasPrice(cacheWrite))
  )
}

export function calculateUsageCostBreakdown(model: any, usage: TokenUsage): UsageCostBreakdown {
  const contextTokens = usage.input + usage.cacheRead + usage.cacheWrite
  const price = readPricing(model, contextTokens)
  const inputUsd = usage.input * price.input
  const cacheReadUsd = usage.cacheRead * price.cacheRead
  const cacheWriteUsd = usage.cacheWrite * price.cacheWrite
  const cacheUsd = cacheReadUsd + cacheWriteUsd
  const outputUsd = usage.output * price.output
  const reasoningUsd = usage.reasoning * price.reasoning
  return {
    inputUsd,
    cacheReadUsd,
    cacheWriteUsd,
    cacheUsd,
    outputUsd,
    reasoningUsd,
    usd: inputUsd + cacheUsd + outputUsd + reasoningUsd,
  }
}

export function calculateUsageCost(model: any, usage: TokenUsage): number {
  return calculateUsageCostBreakdown(model, usage).usd
}

export function calculateSessionCostBreakdown(model: any, records: UsageRecord[]) {
  let pricingAvailable = hasKnownPricing(model)
  let breakdownAvailable = true
  let usedRecorded = false
  let usedCalculated = false
  let missingUnpriced = false
  let inputUsd = 0
  let cacheReadUsd = 0
  let cacheWriteUsd = 0
  let cacheUsd = 0
  let outputUsd = 0
  let reasoningUsd = 0
  let usd = 0

  for (const record of records) {
    const recordModel = record.pricingModel ?? model
    const recordPricingAvailable = hasKnownPricing(recordModel)
    pricingAvailable = pricingAvailable || recordPricingAvailable
    if (recordPricingAvailable && hasUsagePricing(recordModel, record.usage)) {
      const cost = calculateUsageCostBreakdown(recordModel, record.usage)
      const hasUsage = Object.values(record.usage).some((value) => value > 0)
      const staleZeroPricing = hasUsage && cost.usd === 0 && record.recordedCostUsd > 0
      if (!staleZeroPricing) {
        inputUsd += cost.inputUsd
        cacheReadUsd += cost.cacheReadUsd
        cacheWriteUsd += cost.cacheWriteUsd
        cacheUsd += cost.cacheUsd
        outputUsd += cost.outputUsd
        reasoningUsd += cost.reasoningUsd
        usd += cost.usd
        usedCalculated = true
        continue
      }
    }

    breakdownAvailable = false
    if (record.recordedCostUsd > 0) {
      usd += record.recordedCostUsd
      usedRecorded = true
    } else {
      missingUnpriced = true
    }
  }

  return {
    usd,
    inputUsd,
    cacheReadUsd,
    cacheWriteUsd,
    cacheUsd,
    outputUsd,
    reasoningUsd,
    usedRecorded,
    usedCalculated,
    missingUnpriced,
    pricingAvailable,
    breakdownAvailable,
  }
}

export function calculateSessionCost(model: any, records: UsageRecord[]) {
  let pricingAvailable = hasKnownPricing(model)
  let usedRecorded = false
  let usedCalculated = false
  let missingUnpriced = false
  let usd = 0

  for (const record of records) {
    const recordModel = record.pricingModel ?? model
    const recordPricingAvailable = hasKnownPricing(recordModel)
    pricingAvailable = pricingAvailable || recordPricingAvailable
    if (record.recordedCostUsd > 0) {
      usd += record.recordedCostUsd
      usedRecorded = true
    } else if (recordPricingAvailable) {
      usd += calculateUsageCost(recordModel, record.usage)
      usedCalculated = true
    } else {
      missingUnpriced = true
    }
  }

  return { usd, usedRecorded, usedCalculated, missingUnpriced, pricingAvailable }
}

export function findCatalogModel(catalog: any, providerID?: string, modelID?: string): any {
  if (!providerID || !modelID) return undefined
  const models = catalog?.[providerID]?.models
  for (const candidate of catalogModelCandidates(providerID, modelID)) {
    const match = models?.[candidate]
    if (match) return match
  }
  return undefined
}

export function withOpenAIPriorityPricing(model: any, providerID: string, modelID: string, serviceTier?: string): any {
  if (providerID !== "openai" || serviceTier !== "priority") return model
  const baseID = modelID.endsWith("-fast") ? modelID.slice(0, -"-fast".length) : modelID
  const cost = OPENAI_PRIORITY_COST[baseID]
  if (!cost) return model
  return {
    ...model,
    cost: { ...cost },
    pricingServiceTier: "priority",
    pricingUnsupportedAbove: 272_000,
  }
}

export function resolveOpenAIServiceTier(
  authType: "oauth" | "api" | undefined,
  configuredServiceTier?: string,
  responseServiceTier?: string,
): string | undefined {
  if (authType === "oauth") return configuredServiceTier
  if (authType === "api") return responseServiceTier ?? configuredServiceTier
  return configuredServiceTier ?? responseServiceTier
}

export function catalogModelCandidates(providerID: string, modelID: string): string[] {
  if (providerID === "openai" && modelID.endsWith("-fast")) {
    return [modelID.slice(0, -"-fast".length), modelID]
  }
  return [modelID]
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
