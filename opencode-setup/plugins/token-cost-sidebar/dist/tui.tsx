/** @jsxImportSource @opentui/solid */
import { createMemo, createSignal, onMount, Show } from "solid-js"
import type { TuiPlugin, TuiPluginApi, TuiPluginModule } from "@opencode-ai/plugin/tui"

const FALLBACK_EUR_PER_USD = 0.92
const MONEY_SOURCE_RECORDED = "OpenCode"
const MONEY_SOURCE_CALCULATED = "Modellpreis"

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

function normalizePrice(value: unknown): number {
  const parsed = safeNumber(value)
  if (parsed <= 0) return 0
  // models.dev exposes USD per 1M tokens; custom opencode config commonly uses USD per token.
  return parsed > 0.001 ? parsed / 1_000_000 : parsed
}

function readPricing(model: any) {
  const price = model?.cost ?? model?.pricing ?? model?.price ?? model?.info?.cost ?? {}
  return {
    input: normalizePrice(price?.input ?? price?.prompt),
    output: normalizePrice(price?.output ?? price?.completion),
    cacheRead: normalizePrice(price?.cache_read ?? price?.cacheRead ?? price?.cache?.read),
    cacheWrite: normalizePrice(price?.cache_write ?? price?.cacheWrite ?? price?.cache?.write),
  }
}

function tokensOf(message: any) {
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

function formatUsd(value: number): string {
  if (value <= 0) return "0,00 $"
  if (value < 0.01) return "<0,01 $"
  return new Intl.NumberFormat("de-DE", { style: "currency", currency: "USD" }).format(value)
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

function View(props: { api: TuiPluginApi; sessionID: string }) {
  const [eurPerUsd, setEurPerUsd] = createSignal(FALLBACK_EUR_PER_USD)
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
      result.recordedCostUsd += readCost(message)
      result.matchedMessages++
    }
    return result
  })

  const money = createMemo(() => {
    const t = totals()
    const pricing = readPricing(modelMeta().model)
    const calculated =
      t.input * pricing.input +
      t.output * pricing.output +
      t.reasoning * pricing.output +
      t.cacheRead * (pricing.cacheRead || pricing.input) +
      t.cacheWrite * (pricing.cacheWrite || pricing.input)
    const useRecorded = t.recordedCostUsd > 0
    const usd = useRecorded ? t.recordedCostUsd : calculated
    return {
      usd,
      eur: usd * eurPerUsd(),
      source: useRecorded ? MONEY_SOURCE_RECORDED : MONEY_SOURCE_CALCULATED,
    }
  })

  const hasAnything = createMemo(() => modelMeta().fullID !== "unbekannt" || totals().matchedMessages > 0)

  return (
    <Show when={hasAnything()}>
      <box>
        <text fg={theme().text}>
          <b>Modellkosten</b>
        </text>
        <text fg={theme().accent}>{shortLabel(modelMeta().label)}</text>
        <Show when={modelMeta().label !== modelMeta().fullID}>
          <text fg={theme().textMuted}>{shortLabel(modelMeta().fullID)}</text>
        </Show>

        <Row api={props.api} label="Input" value={formatInt(totals().input)} />
        <Row api={props.api} label="Output" value={formatInt(totals().output)} />
        <Show when={totals().reasoning > 0}>
          <Row api={props.api} label="Reasoning" value={formatInt(totals().reasoning)} muted />
        </Show>
        <Show when={totals().cacheRead + totals().cacheWrite > 0}>
          <Row api={props.api} label="Cache" value={`${formatInt(totals().cacheRead)} / ${formatInt(totals().cacheWrite)}`} muted />
        </Show>
        <Row api={props.api} label="Kosten" value={formatEur(money().eur)} />
        <text fg={theme().textMuted}>{`${formatUsd(money().usd)} · ${money().source}`}</text>
      </box>
    </Show>
  )
}

const tui: TuiPlugin = async (api) => {
  api.slots.register({
    order: 110,
    slots: {
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
