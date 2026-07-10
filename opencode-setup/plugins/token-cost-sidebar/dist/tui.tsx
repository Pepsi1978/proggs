/** @jsxImportSource @opentui/solid */
import { createEffect, createMemo, createSignal, For, onCleanup, onMount, Show } from "solid-js"
import type { TuiPlugin, TuiPluginApi, TuiPluginModule } from "@opencode-ai/plugin/tui"
import {
  calculateSessionCost,
  commitIfCurrent,
  hasKnownPricing,
  loadCatalogModel,
  readPricingPerMillion,
  selectPricingModel,
  type TokenUsage,
} from "./pricing"
import {
  DEFAULT_WORK_MODE,
  readWorkMode,
  type WorkModeId,
  WORK_MODES,
  writeWorkMode,
} from "./work-mode"

const FALLBACK_EUR_PER_USD = 0.92
const THEME_PROFILES = [
  "aura",
  "ayu",
  "carbonfox",
  "catppuccin",
  "catppuccin-frappe",
  "catppuccin-macchiato",
  "cobalt2",
  "cursor",
  "dracula",
  "everforest",
  "flexoki",
  "github",
  "gruvbox",
  "kanagawa",
  "lucent-orng",
  "material",
  "matrix",
  "mercury",
  "monokai",
  "nightowl",
  "nord",
  "one-dark",
  "opencode",
  "orng",
  "osaka-jade",
  "palenight",
  "rosepine",
  "solarized",
  "synthwave84",
  "system",
  "tokyonight",
  "vercel",
  "vesper",
  "zenburn",
] as const

function safeNumber(value: unknown): number {
  if (typeof value === "number" && Number.isFinite(value)) return value
  if (typeof value === "string" && value !== "") {
    const parsed = Number(value)
    if (Number.isFinite(parsed)) return parsed
  }
  return 0
}

function firstNumber(...values: unknown[]): number {
  for (const value of values) {
    const parsed = safeNumber(value)
    if (parsed > 0) return parsed
  }
  return 0
}

function readCost(source: any): number {
  return firstNumber(source?.cost, source?.info?.cost, source?.usage?.cost, source?.metrics?.cost)
}

function tokensOf(message: any): TokenUsage {
  return {
    input: safeNumber(message?.tokens?.input ?? message?.info?.tokens?.input),
    output: safeNumber(message?.tokens?.output ?? message?.info?.tokens?.output),
    reasoning: safeNumber(message?.tokens?.reasoning ?? message?.info?.tokens?.reasoning),
    cacheRead: safeNumber(message?.tokens?.cache?.read ?? message?.info?.tokens?.cache?.read),
    cacheWrite: safeNumber(message?.tokens?.cache?.write ?? message?.info?.tokens?.cache?.write),
  }
}

function roleOf(message: any): string | undefined {
  return message?.role ?? message?.info?.role
}

function providerOf(message: any): string | undefined {
  return message?.providerID ?? message?.info?.providerID
}

function modelOf(message: any): string | undefined {
  return message?.modelID ?? message?.info?.modelID
}

function shortLabel(label: string): string {
  return label.length <= 34 ? label : `${label.slice(0, 31)}...`
}

function formatInt(value: number): string {
  return new Intl.NumberFormat("de-DE").format(Math.max(0, Math.round(value)))
}

function formatUsdPerMillion(value: number): string {
  if (value > 0 && value < 0.000001) return "<$0.000001 / 1M"
  const fractionDigits = value > 0 && value < 0.01 ? 6 : value < 1 ? 4 : 2
  return `$${new Intl.NumberFormat("en-US", {
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
  }).format(Math.max(0, value))} / 1M`
}

function formatEur(value: number): string {
  if (value <= 0) return "0,00 €"
  if (value < 0.01) return "<0,01 €"
  return new Intl.NumberFormat("de-DE", { style: "currency", currency: "EUR" }).format(value)
}

function Row(props: { label: string; value: string; muted?: boolean; api: TuiPluginApi }) {
  const theme = () => props.api.theme.current
  return (
    <box flexDirection="row">
      <text fg={props.muted ? theme().textMuted : theme().text}>{props.label}</text>
      <box flexGrow={1} />
      <text fg={props.muted ? theme().textMuted : theme().text}>{props.value}</text>
    </box>
  )
}

function formatDateTime(value: Date): string {
  const pad = (part: number) => String(part).padStart(2, "0")
  return `${pad(value.getDate())}.${pad(value.getMonth() + 1)}.${value.getFullYear()} ${pad(value.getHours())}:${pad(value.getMinutes())} Uhr`
}

function SidebarClock(props: { api: TuiPluginApi }) {
  const [now, setNow] = createSignal(new Date())
  const theme = () => props.api.theme.current
  let timer: ReturnType<typeof setInterval> | undefined

  onMount(() => {
    timer = setInterval(() => setNow(new Date()), 1000)
  })
  onCleanup(() => {
    if (timer) clearInterval(timer)
  })

  return (
    <text fg={theme().text}>
      <b>Session</b>{" "}{formatDateTime(now())}
    </text>
  )
}

function WorkModeSelector(props: { api: TuiPluginApi; sessionID: string }) {
  const [mode, setMode] = createSignal<WorkModeId>(DEFAULT_WORK_MODE)
  const theme = () => props.api.theme.current

  onMount(() => {
    void readWorkMode(props.sessionID)
      .then(setMode)
      .catch(() => props.api.ui.toast({
        variant: "error",
        title: "Arbeitsmodus",
        message: "Gespeicherter Arbeitsmodus konnte nicht gelesen werden.",
      }))
  })

  const selectMode = async (next: WorkModeId) => {
    if (next === mode()) return
    const previous = mode()
    setMode(next)
    try {
      await writeWorkMode(props.sessionID, next)
    } catch {
      setMode(previous)
      props.api.ui.toast({
        variant: "error",
        title: "Arbeitsmodus",
        message: "Arbeitsmodus konnte nicht gespeichert werden.",
      })
    }
  }

  return (
    <box>
      <For each={WORK_MODES}>
        {(item) => {
          const active = () => mode() === item.id
          return (
            <box onMouseUp={(event) => event.button === 0 && void selectMode(item.id)}>
              <Show
                when={active()}
                fallback={<text fg={theme().textMuted}>{item.label}</text>}
              >
                <text fg={theme().accent}><b>{item.label}</b></text>
              </Show>
            </box>
          )
        }}
      </For>
    </box>
  )
}

function applyTheme(api: TuiPluginApi, name: string): boolean {
  const changed = api.theme.set(name)
  if (!changed) {
    api.ui.toast({
      variant: "error",
      title: "Darstellung",
      message: `Theme ${name} konnte nicht aktiviert werden.`,
    })
    return false
  }

  return true
}

function ThemeSelect(props: { api: TuiPluginApi }) {
  const theme = () => props.api.theme.current
  const selected = () => props.api.theme.selected
  const mode = () => props.api.theme.mode()

  const open = () => {
    const initial = selected()
    let confirmed = false
    const options = THEME_PROFILES.filter((name) => props.api.theme.has(name)).map((name) => ({
      title: name,
      value: name,
      description: name === "system" ? "Folgt den Terminalfarben" : undefined,
    }))
    const DialogSelect = props.api.ui.DialogSelect
    props.api.ui.dialog.replace(() => {
      onCleanup(() => {
        if (!confirmed) applyTheme(props.api, initial)
      })
      return (
        <DialogSelect
          title="Theme auswählen"
          options={options}
          current={initial}
          onMove={(option) => applyTheme(props.api, option.value)}
          onSelect={(option) => {
            if (!applyTheme(props.api, option.value)) return
            confirmed = true
            props.api.ui.dialog.clear()
          }}
        />
      )
    })
  }

  const selectMode = (next: "dark" | "light") => {
    if (mode() === next) return
    props.api.keymap.dispatchCommand("theme.switch_mode")
  }

  return (
    <box paddingTop={1}>
      <box flexDirection="row" paddingX={1} backgroundColor={theme().backgroundElement} onMouseUp={(event) => event.button === 0 && open()}>
        <text fg={theme().text}>Theme</text>
        <box flexGrow={1} />
        <text fg={theme().accent}>{`${selected()} v`}</text>
      </box>
      <box flexDirection="row">
        <box
          flexGrow={1}
          paddingX={1}
          backgroundColor={mode() === "dark" ? theme().backgroundElement : undefined}
          onMouseUp={(event) => event.button === 0 && selectMode("dark")}
        >
          <text fg={mode() === "dark" ? theme().accent : theme().textMuted}>Dunkel</text>
        </box>
        <box
          flexGrow={1}
          paddingX={1}
          backgroundColor={mode() === "light" ? theme().backgroundElement : undefined}
          onMouseUp={(event) => event.button === 0 && selectMode("light")}
        >
          <text fg={mode() === "light" ? theme().accent : theme().textMuted}>Hell</text>
        </box>
      </box>
    </box>
  )
}

function View(props: { api: TuiPluginApi; sessionID: string }) {
  const [eurPerUsd, setEurPerUsd] = createSignal(FALLBACK_EUR_PER_USD)
  const [catalogModel, setCatalogModel] = createSignal<any>()
  const theme = () => props.api.theme.current
  const messages = createMemo(() => props.api.state.session.messages(props.sessionID) as any[])
  const configModel = createMemo(() => String((props.api.state.config as any)?.model ?? ""))

  onMount(() => {
    void fetch("https://api.frankfurter.app/latest?from=USD&to=EUR")
      .then((response) => (response.ok ? response.json() : undefined))
      .then((data) => {
        const rate = safeNumber(data?.rates?.EUR)
        if (rate > 0) setEurPerUsd(rate)
      })
      .catch(() => {})
  })

  const current = createMemo(() => {
    const list = messages()
    for (let i = list.length - 1; i >= 0; i--) {
      const message = list[i]
      if (roleOf(message) !== "assistant") continue
      const providerID = providerOf(message)
      const modelID = modelOf(message)
      if (providerID || modelID) return { providerID, modelID }
    }

    const configured = configModel()
    const splitAt = configured.indexOf("/")
    if (splitAt > 0) {
      return {
        providerID: configured.slice(0, splitAt),
        modelID: configured.slice(splitAt + 1),
      }
    }
    return { providerID: undefined, modelID: configured || undefined }
  })

  const modelMeta = createMemo(() => {
    const providerID = current().providerID
    const modelID = current().modelID
    const provider = props.api.state.provider.find((item: any) => item?.id === providerID)
    const model = provider?.models?.[modelID ?? ""]
    const fullID = providerID && modelID ? `${providerID}/${modelID}` : modelID || configModel() || "unbekannt"
    return {
      provider,
      model,
      fullID,
      label: model?.name ? `${model.name}` : fullID,
    }
  })

  let catalogRequestKey = ""
  createEffect(() => {
    const providerID = current().providerID
    const modelID = current().modelID
    const key = `${providerID ?? ""}/${modelID ?? ""}`
    if (key === catalogRequestKey) return
    catalogRequestKey = key
    setCatalogModel(undefined)
    void loadCatalogModel(providerID, modelID)
      .then((model) => commitIfCurrent(key, () => catalogRequestKey, setCatalogModel, model))
      .catch(() => commitIfCurrent(key, () => catalogRequestKey, setCatalogModel, undefined))
  })

  const totals = createMemo(() => {
    const providerID = current().providerID
    const modelID = current().modelID
    const seen = new Set<string>()
    const result = {
      input: 0,
      output: 0,
      reasoning: 0,
      cacheRead: 0,
      cacheWrite: 0,
      recordedCostUsd: 0,
      matchedMessages: 0,
      records: [] as Array<{ usage: TokenUsage; recordedCostUsd: number }>,
    }

    for (const message of messages()) {
      if (roleOf(message) !== "assistant") continue
      if (providerID && providerOf(message) && providerOf(message) !== providerID) continue
      if (modelID && modelOf(message) && modelOf(message) !== modelID) continue

      const id = message?.id ?? message?.info?.id
      if (typeof id === "string" && seen.has(id)) continue
      if (typeof id === "string") seen.add(id)

      const t = tokensOf(message)
      result.input += t.input
      result.output += t.output
      result.reasoning += t.reasoning
      result.cacheRead += t.cacheRead
      result.cacheWrite += t.cacheWrite
      const recordedCostUsd = readCost(message)
      result.recordedCostUsd += recordedCostUsd
      result.matchedMessages++
      result.records.push({ usage: t, recordedCostUsd })
    }
    return result
  })

  const pricedModel = createMemo(() => selectPricingModel(modelMeta().model, catalogModel()))

  const rates = createMemo(() => {
    const model = pricedModel()
    return hasKnownPricing(model) ? readPricingPerMillion(model) : undefined
  })

  const rateValue = (kind: "input" | "output") => {
    const currentRates = rates()
    return currentRates ? formatUsdPerMillion(currentRates[kind]) : "nicht verfügbar"
  }

  const money = createMemo(() => {
    const t = totals()
    const cost = calculateSessionCost(pricedModel(), t.records)

    return {
      usd: cost.usd,
      eur: cost.usd * eurPerUsd(),
      available: !cost.missingUnpriced && (cost.usedRecorded || cost.pricingAvailable),
    }
  })

  const hasAnything = createMemo(() => modelMeta().fullID !== "unbekannt" || totals().matchedMessages > 0)

  return (
    <Show when={hasAnything()}>
      <box>
        <text fg={theme().accent}>{shortLabel(modelMeta().label)}</text>

        <Row
          api={props.api}
          label="Inputpreis"
          value={rateValue("input")}
          muted
        />
        <Row
          api={props.api}
          label="Outputpreis"
          value={rateValue("output")}
          muted
        />

        <Row api={props.api} label="Input" value={formatInt(totals().input)} />
        <Row api={props.api} label="Output" value={formatInt(totals().output)} />
        <Row api={props.api} label="Reasoning" value={formatInt(totals().reasoning)} />
        <Row
          api={props.api}
          label="Gesamt"
          value={formatInt(totals().input + totals().output + totals().reasoning)}
        />
        <Row api={props.api} label="Kosten" value={money().available ? formatEur(money().eur) : "nicht verfügbar"} />
        <ThemeSelect api={props.api} />
      </box>
    </Show>
  )
}

const tui: TuiPlugin = async (api) => {
  api.slots.register({
    order: 90,
    slots: {
      sidebar_content(_ctx, props) {
        return <WorkModeSelector api={api} sessionID={props.session_id} />
      },
    },
  })

  api.slots.register({
    order: 110,
    slots: {
      sidebar_title() {
        return <SidebarClock api={api} />
      },
      sidebar_content(_ctx, props) {
        return <View api={api} sessionID={props.session_id} />
      },
    },
  })
}

const plugin: TuiPluginModule & { id: string } = {
  id: "frank.token-cost-sidebar",
  tui,
}

export default plugin
