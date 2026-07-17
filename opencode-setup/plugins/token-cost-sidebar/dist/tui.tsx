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
} from "./pricing"
import {
  DEFAULT_WORK_MODE,
  readWorkMode,
  type WorkModeId,
  WORK_MODES,
  writeWorkMode,
} from "./work-mode"
import {
  createSessionUsageLedger,
  observeAssistantMessage,
  observeAssistantMessages,
  serializeSessionUsage,
  sessionUsageSnapshot,
  type SessionUsageLedger,
} from "./usage"
import { isCompletedOpenAIMessage, loadOpenAIWeeklyQuota, type WeeklyQuota } from "./openai-quota"

const FALLBACK_EUR_PER_USD = 0.92
const QUOTA_POLL_MS = 60_000
const QUOTA_RECHECK_DELAY_MS = 2_000
const EFFORT_LEVELS = [
  { id: "low", label: "Low" },
  { id: "medium", label: "Medium" },
  { id: "high", label: "High" },
  { id: "xhigh", label: "X-High" },
] as const
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

type LiveModelApi = {
  current?: () => { providerID: string; modelID: string } | undefined
  variant: {
    current: () => string | undefined
    list: () => ReadonlyArray<string>
    set: (value: string | undefined) => boolean
  }
}

function liveModel(api: TuiPluginApi): LiveModelApi | undefined {
  return (api as TuiPluginApi & { model?: LiveModelApi }).model
}

function resolveModelMeta(api: TuiPluginApi, sessionID: string) {
  const messages = api.state.session.messages(sessionID) as any[]
  const configured = String((api.state.config as any)?.model ?? "")
  const selected = liveModel(api)?.current?.()
  let providerID = selected?.providerID
  let modelID = selected?.modelID

  if (!providerID && !modelID) {
    for (let i = messages.length - 1; i >= 0; i--) {
      const message = messages[i]
      if (roleOf(message) !== "assistant") continue
      providerID = providerOf(message)
      modelID = modelOf(message)
      if (providerID || modelID) break
    }
  }

  if (!providerID && !modelID) {
    const splitAt = configured.indexOf("/")
    if (splitAt > 0) {
      providerID = configured.slice(0, splitAt)
      modelID = configured.slice(splitAt + 1)
    } else {
      modelID = configured || undefined
    }
  }

  const provider = api.state.provider.find((item: any) => item?.id === providerID)
  const model = provider?.models?.[modelID ?? ""]
  const fullID = providerID && modelID ? `${providerID}/${modelID}` : modelID || configured || "unbekannt"
  return {
    providerID,
    modelID,
    provider,
    model,
    fullID,
    label: model?.name ? `${model.name}` : fullID,
  }
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
  let timer: ReturnType<typeof setTimeout> | undefined

  const scheduleNextMinute = () => {
    const current = new Date()
    const delay = 60_000 - current.getSeconds() * 1_000 - current.getMilliseconds() + 25
    timer = setTimeout(() => {
      setNow(new Date())
      scheduleNextMinute()
    }, delay)
  }

  onMount(() => {
    scheduleNextMinute()
  })
  onCleanup(() => {
    if (timer) clearTimeout(timer)
  })

  return (
    <text fg={theme().text}>
      <span style={{ fg: theme().accent, bold: true, underline: true }}>Session</span>{" "}{formatDateTime(now())}
    </text>
  )
}

function WorkModeSelector(props: { api: TuiPluginApi; sessionID: string }) {
  const [mode, setMode] = createSignal<WorkModeId>(DEFAULT_WORK_MODE)
  const theme = () => props.api.theme.current
  let selectionRevision = 0

  onMount(() => {
    const revision = selectionRevision
    void readWorkMode(props.sessionID)
      .then((savedMode) => {
        if (selectionRevision === revision) setMode(savedMode)
      })
      .catch(() => props.api.ui.toast({
        variant: "error",
        title: "Arbeitsmodus",
        message: "Gespeicherter Arbeitsmodus konnte nicht gelesen werden.",
      }))
  })

  const selectMode = (next: WorkModeId) => {
    try {
      writeWorkMode(props.sessionID, next)
      selectionRevision++
      setMode(next)
    } catch {
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
            <box onMouseUp={(event) => event.button === 0 && selectMode(item.id)}>
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

function EffortSelector(props: { api: TuiPluginApi }) {
  const theme = () => props.api.theme.current
  const model = () => liveModel(props.api)
  const levels = createMemo(() => {
    const available = new Set(model()?.variant.list() ?? [])
    return EFFORT_LEVELS.filter((level) => available.has(level.id))
  })

  const selectEffort = (id: string, label: string) => {
    if (model()?.variant.set(id)) {
      props.api.ui.toast({ title: "Effort", message: `${label} ist ab dem nächsten Modellaufruf aktiv.` })
      return
    }
    props.api.ui.toast({
      variant: "error",
      title: "Effort",
      message: `${label} wird vom aktuellen Modell nicht unterstützt.`,
    })
  }

  return (
    <Show when={levels().length > 0}>
      <box flexDirection="row" paddingBottom={1}>
        <For each={levels()}>
          {(item, index) => {
            const active = () => model()?.variant.current() === item.id
            return (
              <box
                paddingRight={index() < levels().length - 1 ? 1 : 0}
                onMouseUp={(event) => event.button === 0 && selectEffort(item.id, item.label)}
              >
                <Show when={active()} fallback={<text fg={theme().textMuted}>{item.label}</text>}>
                  <text fg={theme().accent}><b>{item.label}</b></text>
                </Show>
              </box>
            )
          }}
        </For>
      </box>
    </Show>
  )
}

type OpenAIQuotaStore = {
  quota: () => WeeklyQuota | null | undefined
}

function createOpenAIQuotaStore(api: TuiPluginApi): OpenAIQuotaStore {
  const [quota, setQuota] = createSignal<WeeklyQuota | null | undefined>()
  const delayedRefreshes = new Set<ReturnType<typeof setTimeout>>()
  let pollTimer: ReturnType<typeof setTimeout> | undefined
  let disposed = false
  let inFlight = false
  let refreshPending = false

  const logFailure = (error: unknown) => {
    void api.client.app.log({
      body: {
        service: "frank.token-cost-sidebar",
        level: "warn",
        message: "Das OpenAI-Wochenkontingent konnte nicht aktualisiert werden.",
        extra: { error: String(error) },
      },
      query: { directory: api.state.path.directory },
    })
  }

  const refresh = async () => {
    if (disposed) return
    if (inFlight) {
      refreshPending = true
      return
    }

    inFlight = true
    try {
      const value = await loadOpenAIWeeklyQuota(api.state.path.state)
      if (!disposed) setQuota(value ?? null)
    } catch (error) {
      if (!disposed) {
        if (quota() === undefined) setQuota(null)
        logFailure(error)
      }
    } finally {
      inFlight = false
      if (refreshPending && !disposed) {
        refreshPending = false
        void refresh()
      }
    }
  }

  const refreshAfterServerUpdate = () => {
    void refresh()
    const timer = setTimeout(() => {
      delayedRefreshes.delete(timer)
      void refresh()
    }, QUOTA_RECHECK_DELAY_MS)
    delayedRefreshes.add(timer)
  }

  const schedulePoll = () => {
    if (disposed) return
    pollTimer = setTimeout(() => {
      void refresh().finally(schedulePoll)
    }, QUOTA_POLL_MS)
  }

  const stopMessageUpdates = api.event.on("message.updated", (event) => {
    if (isCompletedOpenAIMessage(event.properties.info)) refreshAfterServerUpdate()
  })
  void refresh()
  schedulePoll()

  api.lifecycle.onDispose(() => {
    disposed = true
    stopMessageUpdates()
    if (pollTimer) clearTimeout(pollTimer)
    for (const timer of delayedRefreshes) clearTimeout(timer)
    delayedRefreshes.clear()
  })

  return { quota }
}

function ModelLabel(props: { api: TuiPluginApi; sessionID: string; quotaStore: OpenAIQuotaStore }) {
  const theme = () => props.api.theme.current
  const modelMeta = createMemo(() => resolveModelMeta(props.api, props.sessionID))

  const quotaLabel = () => {
    const current = props.quotaStore.quota()
    if (current === undefined) return "Woche ..."
    if (current === null) return "Woche n/v"
    const reset = new Date(current.resetAt * 1_000)
    const date = new Intl.DateTimeFormat("de-DE", { day: "numeric", month: "long" }).format(reset)
    return `Woche ${current.remainingPercent}% · ${date}`
  }

  return (
    <text fg={theme().accent}>
      <span style={{ bold: true, underline: true }}>{shortLabel(modelMeta().label)}</span>
      <Show when={modelMeta().providerID === "openai"}>
        <span style={{ fg: theme().text }}>{` · ${quotaLabel()}`}</span>
      </Show>
    </text>
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
      <box flexDirection="row" backgroundColor={theme().backgroundElement} onMouseUp={(event) => event.button === 0 && open()}>
        <text fg={theme().accent}><span style={{ bold: true, underline: true }}>Theme</span></text>
        <text fg={theme().accent}>{` ${selected()} v`}</text>
      </box>
      <box flexDirection="row">
        <box
          flexGrow={1}
          paddingX={1}
          backgroundColor={mode() === "dark" ? theme().backgroundElement : undefined}
          onMouseUp={(event) => event.button === 0 && selectMode("dark")}
        >
          <Show
            when={mode() === "dark"}
            fallback={<text fg={theme().textMuted}>Dunkel</text>}
          >
            <text fg={theme().accent}><b>Dunkel</b></text>
          </Show>
        </box>
        <box
          flexGrow={1}
          paddingX={1}
          backgroundColor={mode() === "light" ? theme().backgroundElement : undefined}
          onMouseUp={(event) => event.button === 0 && selectMode("light")}
        >
          <Show
            when={mode() === "light"}
            fallback={<text fg={theme().textMuted}>Hell</text>}
          >
            <text fg={theme().accent}><b>Hell</b></text>
          </Show>
        </box>
      </box>
    </box>
  )
}

type SessionUsageStore = {
  merge: (sessionID: string, messages: readonly any[]) => void
  snapshot: (sessionID: string) => ReturnType<typeof sessionUsageSnapshot>
  hydrateHistory: (sessionID: string) => void
  revision: () => number
}

function createSessionUsageStore(api: TuiPluginApi): SessionUsageStore {
  const prefix = "frank.token-cost-sidebar.usage."
  const ledgers = new Map<string, SessionUsageLedger>()
  const historyRequests = new Set<string>()
  const [revision, setRevision] = createSignal(0)

  const ledgerFor = (sessionID: string) => {
    let ledger = ledgers.get(sessionID)
    if (ledger) return ledger
    ledger = createSessionUsageLedger(api.kv.get(`${prefix}${sessionID}`, []))
    ledgers.set(sessionID, ledger)
    return ledger
  }

  const commit = (sessionID: string, ledger: SessionUsageLedger) => {
    api.kv.set(`${prefix}${sessionID}`, serializeSessionUsage(ledger))
    setRevision((value) => value + 1)
  }

  const merge = (sessionID: string, messages: readonly any[]) => {
    const ledger = ledgerFor(sessionID)
    if (observeAssistantMessages(ledger, messages)) commit(sessionID, ledger)
  }

  const stopMessageUpdates = api.event.on("message.updated", (event) => {
    const message = event.properties.info
    const ledger = ledgerFor(message.sessionID)
    if (observeAssistantMessage(ledger, message)) commit(message.sessionID, ledger)
  })
  const stopSessionDeletes = api.event.on("session.deleted", (event) => {
    const sessionID = event.properties.info.id
    ledgers.delete(sessionID)
    historyRequests.delete(sessionID)
    api.kv.set(`${prefix}${sessionID}`, [])
    setRevision((value) => value + 1)
  })
  api.lifecycle.onDispose(stopMessageUpdates)
  api.lifecycle.onDispose(stopSessionDeletes)

  return {
    merge,
    revision,
    snapshot(sessionID) {
      return sessionUsageSnapshot(ledgerFor(sessionID))
    },
    hydrateHistory(sessionID) {
      if (historyRequests.has(sessionID)) return
      historyRequests.add(sessionID)
      void api.client.session.messages({
        path: { id: sessionID },
        query: { directory: api.state.path.directory },
      }).then((response) => merge(sessionID, response.data ?? [])).catch((error) => {
        void api.client.app.log({
          body: {
            service: "frank.token-cost-sidebar",
            level: "warn",
            message: "Der vollständige Session-Verlauf konnte nicht für die Kostenanzeige geladen werden.",
            extra: { sessionID, error: String(error) },
          },
          query: { directory: api.state.path.directory },
        })
      })
    },
  }
}

function View(props: { api: TuiPluginApi; sessionID: string; usageStore: SessionUsageStore }) {
  const [eurPerUsd, setEurPerUsd] = createSignal(FALLBACK_EUR_PER_USD)
  const [catalogModel, setCatalogModel] = createSignal<any>()
  const theme = () => props.api.theme.current
  const messages = createMemo(() => props.api.state.session.messages(props.sessionID) as any[])
  const modelMeta = createMemo(() => resolveModelMeta(props.api, props.sessionID))

  onMount(() => {
    props.usageStore.hydrateHistory(props.sessionID)
    void fetch("https://api.frankfurter.app/latest?from=USD&to=EUR")
      .then((response) => (response.ok ? response.json() : undefined))
      .then((data) => {
        const rate = safeNumber(data?.rates?.EUR)
        if (rate > 0) setEurPerUsd(rate)
      })
      .catch(() => {})
  })

  let catalogRequestKey = ""
  createEffect(() => {
    const providerID = modelMeta().providerID
    const modelID = modelMeta().modelID
    const key = `${providerID ?? ""}/${modelID ?? ""}`
    if (key === catalogRequestKey) return
    catalogRequestKey = key
    setCatalogModel(undefined)
    void loadCatalogModel(providerID, modelID)
      .then((model) => commitIfCurrent(key, () => catalogRequestKey, setCatalogModel, model))
      .catch(() => commitIfCurrent(key, () => catalogRequestKey, setCatalogModel, undefined))
  })

  const totals = createMemo(() => {
    props.usageStore.revision()
    props.usageStore.merge(props.sessionID, messages())
    return props.usageStore.snapshot(props.sessionID)
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
    const cost = calculateSessionCost(pricedModel(), t.records.map((record) => {
      const provider = props.api.state.provider.find((item: any) => item?.id === record.providerID)
      const embeddedModel = provider?.models?.[record.modelID ?? ""]
      const currentModel = record.providerID === modelMeta().providerID && record.modelID === modelMeta().modelID
        ? pricedModel()
        : embeddedModel
      return { usage: record.usage, recordedCostUsd: record.recordedCostUsd, pricingModel: currentModel }
    }))

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
        <text fg={theme().accent}><span style={{ bold: true, underline: true }}>Context</span></text>

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
  const usageStore = createSessionUsageStore(api)
  const quotaStore = createOpenAIQuotaStore(api)
  api.slots.register({
    order: 90,
    slots: {
      sidebar_content(_ctx, props) {
        return (
          <box>
            <ModelLabel api={api} sessionID={props.session_id} quotaStore={quotaStore} />
            <EffortSelector api={api} />
            <WorkModeSelector api={api} sessionID={props.session_id} />
          </box>
        )
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
        return <View api={api} sessionID={props.session_id} usageStore={usageStore} />
      },
    },
  })
}

const plugin: TuiPluginModule & { id: string } = {
  id: "frank.token-cost-sidebar",
  tui,
}

export default plugin
