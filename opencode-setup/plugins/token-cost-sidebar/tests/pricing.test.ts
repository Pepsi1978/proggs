import { describe, expect, test } from "bun:test"
import {
  calculateUsageCost,
  calculateSessionCost,
  commitIfCurrent,
  findCatalogModel,
  hasKnownPricing,
  hasPositivePricing,
  loadCatalogModel,
  readPricing,
  readPricingPerMillion,
  selectPricingModel,
} from "../dist/pricing"

const model = {
  cost: {
    input: 5,
    output: 30,
    cache_read: 0.5,
    cache_write: 6.25,
    tiers: [
      {
        input: 10,
        output: 45,
        cache_read: 1,
        cache_write: 12.5,
        tier: { type: "context", size: 272_000 },
      },
    ],
  },
}

describe("models.dev pricing", () => {
  test("finds the exact provider and model", () => {
    const catalog = { openai: { models: { "gpt-5.6-sol": model } } }
    expect(findCatalogModel(catalog, "openai", "gpt-5.6-sol")).toBe(model)
    expect(findCatalogModel(catalog, "openai", "other")).toBeUndefined()
  })

  test("normalizes per-million base prices", () => {
    expect(readPricing(model, 100_000)).toEqual({
      input: 0.000005,
      output: 0.00003,
      reasoning: 0.00003,
      cacheRead: 0.0000005,
      cacheWrite: 0.00000625,
    })
  })

  test("exposes input and output rates in USD per million tokens", () => {
    expect(readPricingPerMillion(model)).toEqual({ input: 5, output: 30 })
    expect(readPricingPerMillion({ cost: { input: 0, output: 0 } })).toEqual({ input: 0, output: 0 })
  })

  test("uses the context tier for a large individual request", () => {
    expect(readPricing(model, 300_000)).toEqual({
      input: 0.00001,
      output: 0.000045,
      reasoning: 0.000045,
      cacheRead: 0.000001,
      cacheWrite: 0.0000125,
    })
  })

  test("calculates cache internally without exposing it as a total", () => {
    expect(
      calculateUsageCost(model, {
        input: 100_000,
        output: 2_000,
        reasoning: 1_000,
        cacheRead: 100_000,
        cacheWrite: 0,
      }),
    ).toBeCloseTo(0.64)
  })

  test("normalizes very small models.dev prices per million tokens", () => {
    expect(readPricing({ cost: { input: 0.001, output: 0.002 } }).input).toBe(0.000000001)
  })

  test("uses a dedicated reasoning price", () => {
    const reasoningModel = { cost: { input: 1, output: 2, reasoning: 8 } }
    expect(
      calculateUsageCost(reasoningModel, {
        input: 0,
        output: 0,
        reasoning: 1_000_000,
        cacheRead: 0,
        cacheWrite: 0,
      }),
    ).toBe(8)
  })

  test("distinguishes known free pricing from missing and stale zero pricing", () => {
    const freeModel = { cost: { input: 0, output: 0 } }
    expect(hasKnownPricing(freeModel)).toBe(true)
    expect(hasPositivePricing(freeModel)).toBe(false)
    expect(hasKnownPricing({})).toBe(false)
  })

  test("fills only missing recorded costs from model pricing", () => {
    const usage = {
      input: 100_000,
      output: 0,
      reasoning: 0,
      cacheRead: 0,
      cacheWrite: 0,
    }
    expect(
      calculateSessionCost(model, [
        { usage, recordedCostUsd: 1.25 },
        { usage, recordedCostUsd: 0 },
      ]),
    ).toEqual({
      usd: 1.75,
      usedRecorded: true,
      usedCalculated: true,
      missingUnpriced: false,
      pricingAvailable: true,
    })
  })

  test("renders the requested token rows without a cache row", async () => {
    const source = await Bun.file(new URL("../dist/tui.tsx", import.meta.url)).text()
    expect(source).toContain('label="Input"')
    expect(source).toContain('label="Output"')
    expect(source).toContain('label="Reasoning"')
    expect(source).not.toContain('label="Reasoning" value={formatInt(totals().reasoning)} muted')
    expect(source).toContain('label="Gesamt"')
    expect(source).not.toContain('label="Cache"')
  })

  test("shows only the accented model name without redundant headings", async () => {
    const source = await Bun.file(new URL("../dist/tui.tsx", import.meta.url)).text()
    expect(source).toContain('<text fg={theme().accent}>{shortLabel(modelMeta().label)}</text>')
    expect(source).not.toContain("Modellkosten")
    expect(source).not.toContain("modelMeta().label !== modelMeta().fullID")
  })

  test("replaces the sidebar title with a live German date and time", async () => {
    const source = await Bun.file(new URL("../dist/tui.tsx", import.meta.url)).text()
    expect(source).toContain("sidebar_title()")
    expect(source).toContain("return <SidebarClock api={api} />")
    expect(source).toContain("setInterval(() => setNow(new Date()), 1000)")
    expect(source).toContain('<b>Session</b>{" "}{formatDateTime(now())}')
    expect(source).toContain('`${pad(value.getDate())}.${pad(value.getMonth() + 1)}.${value.getFullYear()} ${pad(value.getHours())}:${pad(value.getMinutes())} Uhr`')
  })

  test("offers built-in themes in a mouse-controlled dropdown", async () => {
    const source = await Bun.file(new URL("../dist/tui.tsx", import.meta.url)).text()
    const profileBlock = source.match(/const THEME_PROFILES = \[([\s\S]*?)\] as const/)?.[1] ?? ""
    const profiles = [...profileBlock.matchAll(/"([^"]+)"/g)].map((match) => match[1])
    expect(source).toContain("DialogSelect")
    expect(source).toContain('title="Theme auswählen"')
    expect(source).toContain("api.theme.set(name)")
    expect(profiles).toHaveLength(34)
    expect(profiles).toContain("aura")
    expect(profiles).toContain("dracula")
    expect(profiles).toContain("github")
    expect(profiles).toContain("solarized")
    expect(profiles).toContain("system")
    expect(source).toContain("const selected = () => props.api.theme.selected")
    expect(source).toContain("onMove={(option) => applyTheme(props.api, option.value)}")
    expect(source).toContain("if (!confirmed) applyTheme(props.api, initial)")
    expect(source).toContain("confirmed = true")
    expect(source).toContain('dispatchCommand("theme.switch_mode")')
    expect(source).toContain(">Dunkel</text>")
    expect(source).toContain(">Hell</text>")
    expect(source).not.toContain("PLUGIN_VERSION")
    expect(source).not.toContain("Version ${")
    expect(source).not.toContain("frank.theme")
    expect(source).not.toContain("frank-dark")
    expect(source).not.toContain("frank-light")
  })

  test("renders separate dollar input and output model prices with an explicit unit", async () => {
    const source = await Bun.file(new URL("../dist/tui.tsx", import.meta.url)).text()
    expect(source).toContain('label="Inputpreis"')
    expect(source).toContain('label="Outputpreis"')
    expect(source).toMatch(/label="Inputpreis"[\s\S]*?value=\{rateValue\("input"\)\}[\s\S]*?muted/)
    expect(source).toMatch(/label="Outputpreis"[\s\S]*?value=\{rateValue\("output"\)\}[\s\S]*?muted/)
    expect(source).toContain('return `$${new Intl.NumberFormat("en-US"')
    expect(source).toContain("} / 1M`")
    expect(source).toContain('return "<$0.000001 / 1M"')
    expect(source).not.toContain("MONEY_SOURCE_")
    expect(source).not.toContain("formatUsd(money().usd)")
  })

  test("prefers the complete live model over incomplete embedded pricing", () => {
    const embedded = { cost: { input: 1, output: 2 } }
    const live = { cost: { input: 1, output: 2, reasoning: 8 } }
    const selected = selectPricingModel(embedded, live)
    expect(calculateUsageCost(selected, {
      input: 0,
      output: 0,
      reasoning: 1_000_000,
      cacheRead: 0,
      cacheWrite: 0,
    })).toBe(8)
  })

  test("retries a temporary catalog failure for the same model", async () => {
    let calls = 0
    const fetcher = (async () => {
      calls++
      if (calls === 1) return { ok: false, status: 503 }
      return {
        ok: true,
        json: async () => ({ openai: { models: { "gpt-5.6-sol": model } } }),
      }
    }) as unknown as typeof fetch

    expect(await loadCatalogModel("openai", "gpt-5.6-sol", fetcher, 0)).toBe(model)
    expect(calls).toBe(2)
  })

  test("ignores a delayed catalog result after a model switch", async () => {
    let currentKey = "openai/model-a"
    let selected = ""
    let releaseA!: (value: string) => void
    const delayedA = new Promise<string>((resolve) => { releaseA = resolve })

    const resultA = delayedA.then((value) =>
      commitIfCurrent("openai/model-a", () => currentKey, (model) => { selected = model }, value),
    )
    currentKey = "openai/model-b"
    commitIfCurrent("openai/model-b", () => currentKey, (model) => { selected = model }, "model-b")
    releaseA("model-a")

    expect(await resultA).toBe(false)
    expect(selected).toBe("model-b")
  })
})
