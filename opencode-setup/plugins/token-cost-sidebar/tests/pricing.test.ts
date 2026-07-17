import { describe, expect, test } from "bun:test"
import {
  calculateSessionCostBreakdown,
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
import {
  DEFAULT_WORK_MODE,
  readWorkMode,
  WORK_MODES,
  workModeInstruction,
  writeWorkMode,
} from "../dist/work-mode"
import {
  createSessionUsageLedger,
  observeAssistantMessage,
  observeAssistantMessages,
  serializeSessionUsage,
  sessionUsageSnapshot,
} from "../dist/usage"
import { mkdtemp, rm } from "node:fs/promises"
import { tmpdir } from "node:os"
import { join } from "node:path"

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

  test("uses GPT-5.6 Sol pricing for the Fast launcher alias", () => {
    const catalog = { openai: { models: { "gpt-5.6-sol": model } } }
    expect(findCatalogModel(catalog, "openai", "gpt-5.6-sol-fast")).toBe(model)
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
    expect(readPricingPerMillion(model)).toEqual({
      input: 5,
      output: 30,
      reasoning: 30,
      cacheRead: 0.5,
      cacheWrite: 6.25,
    })
    expect(readPricingPerMillion({ cost: { input: 0, output: 0 } })).toEqual({
      input: 0,
      output: 0,
      reasoning: 0,
      cacheRead: 0,
      cacheWrite: 0,
    })
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

  test("does not invent cache prices when the catalog omits them", () => {
    expect(
      calculateUsageCost({ cost: { input: 5, output: 30 } }, {
        input: 0,
        output: 0,
        reasoning: 0,
        cacheRead: 1_000_000,
        cacheWrite: 1_000_000,
      }),
    ).toBe(0)
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

  test("splits session cost into input, output, and reasoning without double-counting", () => {
    const cost = calculateSessionCostBreakdown(model, [{
      usage: {
        input: 100_000,
        output: 2_000,
        reasoning: 1_000,
        cacheRead: 100_000,
        cacheWrite: 10_000,
      },
      recordedCostUsd: 99,
    }])
    expect(cost).toMatchObject({
      usedRecorded: false,
      usedCalculated: true,
      missingUnpriced: false,
      pricingAvailable: true,
      breakdownAvailable: true,
    })
    expect(cost.inputUsd).toBeCloseTo(0.5)
    expect(cost.cacheUsd).toBeCloseTo(0.1125)
    expect(cost.outputUsd).toBeCloseTo(0.06)
    expect(cost.reasoningUsd).toBeCloseTo(0.03)
    expect(cost.usd).toBeCloseTo(0.7025)
  })

  test("falls back to the recorded total without inventing unavailable components", () => {
    expect(calculateSessionCostBreakdown({ cost: { input: 5, output: 30 } }, [{
      usage: { input: 0, output: 0, reasoning: 0, cacheRead: 1_000, cacheWrite: 0 },
      recordedCostUsd: 0.25,
    }])).toEqual({
      usd: 0.25,
      inputUsd: 0,
      cacheUsd: 0,
      outputUsd: 0,
      reasoningUsd: 0,
      usedRecorded: true,
      usedCalculated: false,
      missingUnpriced: false,
      pricingAvailable: true,
      breakdownAvailable: false,
    })
  })

  test("applies context tiers to each model step instead of their session sum", () => {
    const step = {
      usage: { input: 200_000, output: 0, reasoning: 0, cacheRead: 0, cacheWrite: 0 },
      recordedCostUsd: 0,
    }
    const cost = calculateSessionCostBreakdown(model, [step, step])
    expect(cost.inputUsd).toBeCloseTo(2)
    expect(cost.cacheUsd).toBe(0)
    expect(cost.usd).toBeCloseTo(2)
  })

  test("uses a recorded total when zero catalog prices are stale", () => {
    const cost = calculateSessionCostBreakdown({ cost: { input: 0, output: 0 } }, [{
      usage: { input: 1_000, output: 100, reasoning: 0, cacheRead: 0, cacheWrite: 0 },
      recordedCostUsd: 0.25,
    }])
    expect(cost.usd).toBe(0.25)
    expect(cost.usedRecorded).toBe(true)
    expect(cost.breakdownAvailable).toBe(false)
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

  test("prices missing costs with each message model", () => {
    expect(calculateSessionCost(undefined, [
      {
        usage: { input: 1_000_000, output: 0, reasoning: 0, cacheRead: 0, cacheWrite: 0 },
        recordedCostUsd: 0,
        pricingModel: { cost: { input: 1, output: 2 } },
      },
      {
        usage: { input: 1_000_000, output: 0, reasoning: 0, cacheRead: 0, cacheWrite: 0 },
        recordedCostUsd: 0,
        pricingModel: { cost: { input: 5, output: 10 } },
      },
    ]).usd).toBe(6)
  })

  test("keeps session usage monotonic across compaction and model switches", () => {
    const ledger = createSessionUsageLedger()
    const first = {
      id: "msg-1",
      sessionID: "session-1",
      role: "assistant",
      providerID: "openai",
      modelID: "model-a",
      tokens: { input: 100, output: 20, reasoning: 10, cache: { read: 5, write: 0 } },
      cost: 0.5,
    }
    const second = {
      id: "msg-2",
      sessionID: "session-1",
      role: "assistant",
      providerID: "other",
      modelID: "model-b",
      tokens: { input: 200, output: 30, reasoning: 15, cache: { read: 0, write: 0 } },
      cost: 0.75,
    }

    expect(observeAssistantMessages(ledger, [first])).toBe(true)
    expect(sessionUsageSnapshot(ledger).input).toBe(100)
    // A compacted TUI window no longer contains msg-1, but the ledger must retain it.
    expect(observeAssistantMessages(ledger, [second])).toBe(true)
    expect(sessionUsageSnapshot(ledger)).toMatchObject({
      input: 300,
      output: 50,
      reasoning: 25,
      matchedMessages: 2,
    })
    expect(sessionUsageSnapshot(ledger).records.reduce((sum, record) => sum + record.recordedCostUsd, 0)).toBe(1.25)
  })

  test("ignores regressive message updates and restores the ledger", () => {
    const ledger = createSessionUsageLedger()
    const message = {
      id: "msg-1",
      role: "assistant",
      tokens: { input: 100, output: 20, reasoning: 10, cache: { read: 5, write: 1 } },
      cost: 0.5,
    }
    expect(observeAssistantMessage(ledger, message)).toBe(true)
    expect(observeAssistantMessage(ledger, {
      ...message,
      tokens: { input: 10, output: 2, reasoning: 1, cache: { read: 0, write: 0 } },
      cost: 0.05,
    })).toBe(false)

    const restored = createSessionUsageLedger(serializeSessionUsage(ledger))
    expect(sessionUsageSnapshot(restored)).toEqual(sessionUsageSnapshot(ledger))
  })

  test("aggregates every completed model step instead of only the final message tokens", () => {
    const ledger = createSessionUsageLedger()
    expect(observeAssistantMessage(ledger, {
      info: {
        id: "msg-steps",
        role: "assistant",
        providerID: "openai",
        modelID: "model-a",
        tokens: { input: 20, output: 4, reasoning: 1, cache: { read: 0, write: 0 } },
        cost: 0.75,
      },
      parts: [
        {
          type: "step-finish",
          cost: 0.5,
          tokens: { input: 100, output: 20, reasoning: 10, cache: { read: 5, write: 1 } },
        },
        { type: "tool", tokens: { input: 9_999 } },
        {
          type: "step-finish",
          cost: 0.25,
          tokens: { input: 200, output: 30, reasoning: 15, cache: { read: 6, write: 2 } },
        },
      ],
    })).toBe(true)

    expect(sessionUsageSnapshot(ledger)).toMatchObject({
      input: 300,
      output: 50,
      reasoning: 25,
      cacheRead: 11,
      cacheWrite: 3,
      matchedMessages: 1,
    })
    expect(sessionUsageSnapshot(ledger).records[0]?.recordedCostUsd).toBe(0.75)
    expect(sessionUsageSnapshot(ledger).records[0]?.segments).toEqual([
      {
        recordedCostUsd: 0.5,
        usage: { input: 100, output: 20, reasoning: 10, cacheRead: 5, cacheWrite: 1 },
      },
      {
        recordedCostUsd: 0.25,
        usage: { input: 200, output: 30, reasoning: 15, cacheRead: 6, cacheWrite: 2 },
      },
    ])
    expect(sessionUsageSnapshot(createSessionUsageLedger(serializeSessionUsage(ledger)))).toEqual(
      sessionUsageSnapshot(ledger),
    )
  })

  test("renders regular token rows followed by a non-muted cache row", async () => {
    const source = await Bun.file(new URL("../dist/tui.tsx", import.meta.url)).text()
    expect(source).toContain('label="Input"')
    expect(source).toContain("value={formatInt(totals().input)}")
    expect(source).not.toContain("totals().input + totals().cacheRead + totals().cacheWrite")
    expect(source).toContain('label="Cache R/W"')
    expect(source).toContain("`${formatInt(totals().cacheRead)} / ${formatInt(totals().cacheWrite)}`")
    expect(source).toContain('label="Output"')
    expect(source).toContain('label="Reasoning"')
    expect(source.indexOf('label="Input"')).toBeLessThan(source.indexOf('label="Output"'))
    expect(source.indexOf('label="Output"')).toBeLessThan(source.indexOf('label="Reasoning"'))
    expect(source.indexOf('label="Reasoning"')).toBeLessThan(source.indexOf('label="Cache R/W"'))
    const cacheRowStart = source.indexOf('label="Cache R/W"')
    const cacheRowEnd = source.indexOf("</Show>", cacheRowStart)
    expect(source.slice(cacheRowStart, cacheRowEnd)).not.toContain("muted")
    expect(source).not.toContain('label="Reasoning" value={formatInt(totals().reasoning)} muted')
    expect(source).not.toContain('label="Gesamt"')
    expect(source).not.toContain('label="Cache"')
    expect(source).toContain('api.event.on("message.updated"')
    expect(source).toContain('api.event.on("message.part.updated"')
    expect(source).toContain("parts: props.api.state.part(info.id)")
    expect(source).toContain("t.records.flatMap((record)")
    expect(source).toContain("record.segments ??")
    expect(source).toContain("api.client.session.messages({")
    expect(source).not.toContain("providerOf(message) !== providerID")
    expect(source).not.toContain("modelOf(message) !== modelID")
  })

  test("shows the live model and Context as accented bold underlined headings", async () => {
    const source = await Bun.file(new URL("../dist/tui.tsx", import.meta.url)).text()
    expect(source).toContain('<span style={{ bold: true, underline: true }}>{shortLabel(modelMeta().label)}</span>')
    expect(source).toContain("const selected = liveModel(api)?.current?.()")
    expect(source).toContain("<box paddingBottom={1}>")
    expect(source).toContain("<text fg={theme().text}>")
    expect(source).toContain('<span style={{ fg: theme().textMuted }}>{` (${quotaDate()})`}</span>')
    expect(source).toContain('{ day: "numeric" }')
    expect(source).toContain('{ month: "long" }')
    expect(source).toContain('return `${day}. ${month}`')
    expect(source).toContain('return `Woche ${current.remainingPercent}%`')
    expect(source).not.toContain('<b>{` · ${quotaLabel()}`}</b>')
    expect(source).toContain('<text fg={theme().accent}><span style={{ bold: true, underline: true }}>Context (kumuliert)</span></text>')
    expect(source).toContain("<ModelLabel api={api} sessionID={props.session_id} quotaStore={quotaStore} />")
    expect(source.indexOf("<ModelLabel api={api} sessionID={props.session_id} quotaStore={quotaStore} />")).toBeLessThan(
      source.indexOf("<EffortSelector api={api} />"),
    )
    expect(source).toContain('api.event.on("message.updated"')
    expect(source).toContain("isCompletedOpenAIMessage(event.properties.info)")
    expect(source).toContain("QUOTA_RECHECK_DELAY_MS = 2_000")
    expect(source).toContain("QUOTA_POLL_MS = 60_000")
  })

  test("replaces the sidebar title with a live German date and time", async () => {
    const source = await Bun.file(new URL("../dist/tui.tsx", import.meta.url)).text()
    expect(source).toContain("sidebar_title()")
    expect(source).toContain("return <SidebarClock api={api} />")
    expect(source).toContain("const scheduleNextMinute = () =>")
    expect(source).toContain("60_000 - current.getSeconds() * 1_000 - current.getMilliseconds() + 25")
    expect(source).toContain("timer = setTimeout(() =>")
    expect(source).toContain("clearTimeout(timer)")
    expect(source).not.toContain("setInterval(")
    expect(source).toContain('<span style={{ fg: theme().accent, bold: true, underline: true }}>Session</span>{" "}{formatDateTime(now())}')
    expect(source).toContain('`${pad(value.getDate())}.${pad(value.getMonth() + 1)}.${value.getFullYear()} ${pad(value.getHours())}:${pad(value.getMinutes())} Uhr`')
  })

  test("renders clickable work modes in the leading sidebar block", async () => {
    const source = await Bun.file(new URL("../dist/tui.tsx", import.meta.url)).text()
    expect(source).toContain("order: 90")
    expect(source).toContain("<WorkModeSelector api={api} sessionID={props.session_id} />")
    expect(source).toContain("event.button === 0 && selectMode(item.id)")
    expect(source).toContain("theme().textMuted")
    expect(source).toContain("<text fg={theme().accent}><b>{item.label}</b></text>")
    expect(WORK_MODES.map((mode) => mode.label)).toEqual([
      "Schnellmodus",
      "Normalmodus",
      "Gründlichkeitsmodus",
    ])
  })

  test("persists work mode per session and creates binding model instructions", async () => {
    const directory = await mkdtemp(join(tmpdir(), "opencode-work-mode-"))
    try {
      expect(await readWorkMode("session-a", directory)).toBe(DEFAULT_WORK_MODE)
      writeWorkMode("session-a", "normal", directory)
      expect(await readWorkMode("session-a", directory)).toBe("normal")
      writeWorkMode("session-a", "gruendlich", directory)
      expect(await readWorkMode("session-a", directory)).toBe("gruendlich")
      expect(await readWorkMode("session-b", directory)).toBe(DEFAULT_WORK_MODE)
      expect(workModeInstruction("schnell")).toContain("kleinsten korrekten Eingriff")
      expect(workModeInstruction("schnell")).toContain("direkt betroffenen Aufrufer")
      expect(workModeInstruction("normal")).toContain("zum Risiko und Umfang passenden Eingriff")
      expect(workModeInstruction("normal")).toContain("höchstens zwei Durchläufe")
      expect(workModeInstruction("gruendlich")).toContain("verwandte Fehlerklassen")
      expect(workModeInstruction("gruendlich")).toContain("alle Prüfungen grün sind")
      for (const mode of WORK_MODES) {
        const instruction = workModeInstruction(mode.id)
        expect(instruction).toContain("Das aktive AGENTS.md-Profil gilt vollständig und unverändert.")
        expect(instruction).toContain("bei einem Widerspruch haben die Regeln aus AGENTS.md Vorrang")
        expect(instruction).not.toContain("überschreibt den Standardmodus")
      }
    } finally {
      await rm(directory, { recursive: true, force: true })
    }
  })

  test("makes every mode visible to the next model request before the click returns", async () => {
    const directory = await mkdtemp(join(tmpdir(), "opencode-work-mode-sync-"))
    try {
      for (const mode of WORK_MODES) {
        writeWorkMode("session-sync", mode.id, directory)
        expect(await readWorkMode("session-sync", directory)).toBe(mode.id)
        expect(workModeInstruction(mode.id)).toContain(`AKTIVER ARBEITSMODUS: ${mode.label}`)
      }

      const source = await Bun.file(new URL("../dist/tui.tsx", import.meta.url)).text()
      expect(source).toContain("const selectMode = (next: WorkModeId) =>")
      expect(source).toContain("writeWorkMode(props.sessionID, next)")
      expect(source).toContain("selectionRevision++")
      expect(source).toContain("if (selectionRevision === revision) setMode(savedMode)")
      expect(source).not.toContain("if (next === mode()) return")
      expect(source.indexOf("writeWorkMode(props.sessionID, next)")).toBeLessThan(source.indexOf("setMode(next)"))
    } finally {
      await rm(directory, { recursive: true, force: true })
    }
  })

  test("injects the selected work mode into every model request", async () => {
    const source = await Bun.file(new URL("../../work-mode.js", import.meta.url)).text()
    expect(source).toContain('"experimental.chat.system.transform"')
    expect(source).toContain("readWorkMode(input.sessionID)")
    expect(source).toContain("output.system.push(workModeInstruction(mode))")
    expect(source).not.toContain("console.")
  })

  test("shows every live model variant through the validated TUI variant API", async () => {
    const source = await Bun.file(new URL("../dist/tui.tsx", import.meta.url)).text()
    const patch = await Bun.file(new URL("../../../patches/opencode-1.17.18-windows-mouse.patch", import.meta.url)).text()

    expect(source).toContain('none: "None"')
    expect(source).toContain('minimal: "Minimal"')
    expect(source).toContain('xhigh: "XHigh"')
    expect(source).toContain('max: "Max"')
    expect(source).toContain('thinking: "Thinking"')
    expect(source).toContain("model()?.variant.list() ?? []).map((id)")
    expect(source).toContain("label: variantLabel(id)")
    expect(source).not.toContain("EFFORT_LEVELS")
    expect(source).toContain("model()?.variant.set(id)")
    expect(source).toContain("model()?.variant.current() === item.id")
    expect(source).toContain('<box flexDirection="row" paddingBottom={1}>')
    expect(source).toContain("<EffortSelector api={api} />")
    expect(source.indexOf("<EffortSelector api={api} />")).toBeLessThan(source.indexOf("<WorkModeSelector api={api}"))
    expect(patch).toContain("model?: TuiModel")
    expect(patch).toContain("current?: () => { providerID: string; modelID: string } | undefined")
    expect(patch).toContain("model: api.model")
    expect(patch).toContain("return input.local.model.current()")
    expect(patch).toContain("input.local.model.variant.set(value)")
    expect(patch).toContain("!input.local.model.variant.list().includes(value)")
    expect(patch).toContain("+const tui: TuiPlugin = async () => {}")
    expect(patch).toContain('+import { TextAttributes } from "@opentui/core"')
    expect(patch).toContain('+        <text fg={theme().accent} attributes={TextAttributes.BOLD | TextAttributes.UNDERLINE}>LSP</text>')
    expect(patch).toMatch(/\+          <text fg=\{theme\(\)\.accent\}>[\s\S]*?<span style=\{\{ bold: true, underline: true \}\}>MCP<\/span>/)
  })

  test("documents every component required for a portable installation", async () => {
    const readme = await Bun.file(new URL("../README.md", import.meta.url)).text()
    const packageJson = await Bun.file(new URL("../package.json", import.meta.url)).json()
    expect(readme).toContain("opencode-setup/plugins/token-cost-sidebar/")
    expect(readme).toContain("opencode-setup/plugins/work-mode.js")
    expect(readme).toContain("opencode-setup/tui.json")
    expect(readme).toContain("Abhängigkeiten")
    expect(readme).toContain("OpenCode vollständig beenden und neu starten")
    expect(readme).toContain(`v${packageJson.version}`)
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
    expect(source).toContain('<box flexDirection="row" backgroundColor={theme().backgroundElement}')
    expect(source).toContain('<text fg={theme().accent}><span style={{ bold: true, underline: true }}>Theme</span></text>')
    expect(source).toContain('<text fg={theme().accent}>{` ${selected()} v`}</text>')
    expect(source).not.toContain('<box flexGrow={1} />\n        <text fg={theme().accent}>{`${selected()} v`}</text>')
    expect(source).toContain('<text fg={theme().accent}><b>Dunkel</b></text>')
    expect(source).toContain('<text fg={theme().accent}><b>Hell</b></text>')
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
    expect(source).toContain('label="Cachepreis R/W"')
    expect(source.indexOf('label="Inputpreis"')).toBeLessThan(source.indexOf('label="Outputpreis"'))
    expect(source.indexOf('label="Outputpreis"')).toBeLessThan(source.indexOf('label="Cachepreis R/W"'))
    expect(source).toMatch(/label="Inputpreis"[\s\S]*?value=\{rateValue\("input"\)\}[\s\S]*?muted/)
    expect(source).toMatch(/label="Outputpreis"[\s\S]*?value=\{rateValue\("output"\)\}[\s\S]*?muted/)
    expect(source).toMatch(/label="Cachepreis R\/W" value=\{cacheRateValue\(\)\} muted/)
    expect(source).toContain('return `$${new Intl.NumberFormat("en-US"')
    expect(source).toContain("} / 1M`")
    expect(source).toContain('return "<$0.000001 / 1M"')
    expect(source).toContain('return `${withoutUnit(read)} R / ${withoutUnit(write)} W / 1M`')
    expect(source).not.toContain("MONEY_SOURCE_")
    expect(source).not.toContain("formatUsd(money().usd)")
  })

  test("renders total, input, output, and reasoning costs in euros", async () => {
    const source = await Bun.file(new URL("../dist/tui.tsx", import.meta.url)).text()
    expect(source).toContain('label="Gesamtkosten"')
    expect(source).toContain('label="Input-Kosten"')
    expect(source).toContain('label="Output-Kosten"')
    expect(source).toContain('label="Reasoning-Kosten"')
    expect(source).toContain('label="Cache-Kosten"')
    expect(source).toContain("formatEur(money().inputEur)")
    expect(source).toContain("formatEur(money().outputEur)")
    expect(source).toContain("formatEur(money().reasoningEur)")
    expect(source).toContain("formatEur(money().cacheEur)")
    expect(source).not.toContain('label="Kosten"')
    expect(source.indexOf('label="Gesamtkosten"')).toBeLessThan(source.indexOf('label="Input-Kosten"'))
    expect(source.indexOf('label="Input-Kosten"')).toBeLessThan(source.indexOf('label="Output-Kosten"'))
    expect(source.indexOf('label="Output-Kosten"')).toBeLessThan(source.indexOf('label="Reasoning-Kosten"'))
    expect(source.indexOf('label="Reasoning-Kosten"')).toBeLessThan(source.indexOf('label="Cache-Kosten"'))
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
