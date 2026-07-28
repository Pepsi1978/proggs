export const openRouterApi = {
  keyUrl: "https://openrouter.ai/api/v1/key",
  modelsUrl: "https://openrouter.ai/api/v1/models",
  chatUrl: "https://openrouter.ai/api/v1/chat/completions",
  frontendUrl: "https://openrouter.ai/api/frontend"
} as const;

export type OpenRouterEndpoint = {
  providerName: string;
  providerSlug: string;
  tag: string;
  endpointId: string;
  promptPerToken: number;
  completionPerToken: number;
  cacheReadPerToken: number;
  contextLength: number;
  maxCompletionTokens?: number;
  quantization: string;
  throughputLast30m?: number;
  uptimeLast5m?: number;
  status: number;
};

export type OpenRouterModel = {
  provider: "openrouter";
  id: string;
  name: string;
  contextLength?: number;
  efforts: string[];
  defaultEffort?: string;
  endpoint?: OpenRouterEndpoint;
};

export type OpenRouterModelDetails = { model: OpenRouterModel; endpoints: OpenRouterEndpoint[]; permaslug: string };

type RawOpenRouterModel = {
  id?: unknown;
  name?: unknown;
  context_length?: unknown;
  architecture?: { output_modalities?: unknown };
  reasoning?: { supported_efforts?: unknown; default_effort?: unknown; mandatory?: unknown };
};

const numberValue = (value: unknown): number | undefined => {
  const parsed = typeof value === "number" ? value : typeof value === "string" && value.trim() ? Number(value) : NaN;
  return Number.isFinite(parsed) ? parsed : undefined;
};

const recordValue = (value: unknown): Record<string, unknown> | undefined => value && typeof value === "object" && !Array.isArray(value) ? value as Record<string, unknown> : undefined;

function endpointThroughput(value: unknown): number | undefined {
  const direct = numberValue(value);
  if (direct !== undefined) return direct;
  const metrics = recordValue(value);
  return metrics ? numberValue(metrics.p50) ?? numberValue(metrics.median) ?? numberValue(metrics.value) : undefined;
}

function providerSlug(providerName: string, tag: string): string {
  return (tag.split("/")[0] || providerName.toLowerCase().replace(/[ .]/g, "")).trim().toLowerCase();
}

export function normalizeOpenRouterModelDetails(value: unknown, requestedId: string): OpenRouterModelDetails | undefined {
  const root = recordValue(value), data = recordValue(root?.data);
  if (!data) return undefined;
  const id = typeof data.id === "string" && data.id.trim() ? data.id.trim() : requestedId;
  if (!id || id.toLowerCase() !== requestedId.toLowerCase()) return undefined;
  const architecture = recordValue(data.architecture), outputs = Array.isArray(architecture?.output_modalities) ? architecture.output_modalities : [];
  if (outputs.length && !outputs.includes("text")) return undefined;
  const reasoning = recordValue(data.reasoning);
  const efforts = Array.isArray(reasoning?.supported_efforts) ? reasoning.supported_efforts.filter((entry): entry is string => typeof entry === "string") : [];
  const defaultEffort = typeof reasoning?.default_effort === "string" ? reasoning.default_effort : undefined;
  const endpoints = (Array.isArray(data.endpoints) ? data.endpoints : []).flatMap((entry): OpenRouterEndpoint[] => {
    const endpoint = recordValue(entry);
    if (!endpoint || typeof endpoint.provider_name !== "string" || !endpoint.provider_name.trim()) return [];
    const pricing = recordValue(endpoint.pricing), tag = typeof endpoint.tag === "string" ? endpoint.tag : "";
    const contextLength = numberValue(endpoint.context_length) ?? 0;
    const maxCompletionTokens = numberValue(endpoint.max_completion_tokens);
    const throughputLast30m = endpointThroughput(endpoint.throughput_last_30m);
    const uptimeLast5m = numberValue(endpoint.uptime_last_5m);
    return [{
      providerName: endpoint.provider_name,
      providerSlug: providerSlug(endpoint.provider_name, tag),
      tag,
      endpointId: typeof endpoint.id === "string" ? endpoint.id : "",
      promptPerToken: numberValue(pricing?.prompt) ?? 0,
      completionPerToken: numberValue(pricing?.completion) ?? 0,
      cacheReadPerToken: numberValue(pricing?.input_cache_read) ?? 0,
      contextLength,
      ...(maxCompletionTokens === undefined ? {} : { maxCompletionTokens }),
      quantization: typeof endpoint.quantization === "string" ? endpoint.quantization : "",
      ...(throughputLast30m === undefined ? {} : { throughputLast30m }),
      ...(uptimeLast5m === undefined ? {} : { uptimeLast5m }),
      status: numberValue(endpoint.status) ?? 0
    }];
  }).sort((a, b) => a.promptPerToken - b.promptPerToken || a.completionPerToken - b.completionPerToken || b.contextLength - a.contextLength);
  if (!endpoints.length) return undefined;
  const namedEndpoint = (Array.isArray(data.endpoints) ? data.endpoints : []).map(recordValue).find((endpoint) => typeof endpoint?.name === "string");
  const endpointName = typeof namedEndpoint?.name === "string" ? namedEndpoint.name : undefined;
  const separator = typeof endpointName === "string" ? endpointName.lastIndexOf("|") : -1;
  const permaslug = separator >= 0 ? endpointName!.slice(separator + 1).trim().replace(/:free$/i, "") : requestedId.replace(/:free$/i, "");
  const contextLength = numberValue(data.context_length) ?? Math.max(...endpoints.map((endpoint) => endpoint.contextLength));
  return {
    model: {
      provider: "openrouter",
      id,
      name: typeof data.name === "string" && data.name.trim() ? data.name : id,
      ...(contextLength ? { contextLength } : {}),
      efforts: reasoning?.mandatory ? efforts.filter((effort) => effort !== "none") : efforts,
      ...(defaultEffort ? { defaultEffort } : {})
    },
    endpoints,
    permaslug
  };
}

export function normalizeOpenRouterModels(data: unknown): OpenRouterModel[] {
  const rows = data && typeof data === "object" && Array.isArray((data as { data?: unknown }).data) ? (data as { data: RawOpenRouterModel[] }).data : [];
  return rows.flatMap((row) => {
    if (typeof row.id !== "string" || !row.id.trim()) return [];
    const outputs = Array.isArray(row.architecture?.output_modalities) ? row.architecture.output_modalities : [];
    if (outputs.length && !outputs.includes("text")) return [];
    const efforts = Array.isArray(row.reasoning?.supported_efforts) ? row.reasoning.supported_efforts.filter((value): value is string => typeof value === "string") : [];
    const defaultEffort = typeof row.reasoning?.default_effort === "string" ? row.reasoning.default_effort : undefined;
    return [{
      provider: "openrouter" as const,
      id: row.id,
      name: typeof row.name === "string" && row.name.trim() ? row.name : row.id,
      ...(typeof row.context_length === "number" ? { contextLength: row.context_length } : {}),
      efforts: row.reasoning?.mandatory ? efforts.filter((effort) => effort !== "none") : efforts,
      ...(defaultEffort ? { defaultEffort } : {})
    }];
  }).sort((a, b) => a.name.localeCompare(b.name));
}

// `max_tokens` wurde bisher NICHT gesetzt. Viele OpenRouter-Anbieter deckeln die Ausgabe dann auf
// ihren eigenen, oft winzigen Standardwert — die JSON-Antwort brach mitten in einem Edit ab und der
// gesamte Lauf galt als „kein gültiges JSON". Der Wert wird jetzt bewusst hoch angesetzt und, wo der
// Anbieter ein Maximum meldet, daran ausgerichtet.
// `allow_fallbacks: false` nagelt jeden Versuch auf denselben Anbieter. Fällt der aus — und einer der
// gespeicherten Anbieter liegt bei 82 % Verfügbarkeit — scheitern alle drei Versuche an derselben
// kaputten Gegenstelle. Der gewählte Anbieter bleibt deshalb erste Wahl, aber ab dem zweiten Versuch
// darf OpenRouter auf einen anderen ausweichen, statt den Lauf verloren zu geben.
export function openRouterRequest(model: string, instructions: string, input: string, effort?: string, selectedProvider?: string, maxTokens?: number, allowFallbacks = false) {
  return {
    model,
    messages: [{ role: "system", content: instructions }, { role: "user", content: input }],
    stream: true,
    ...(maxTokens && maxTokens > 0 ? { max_tokens: Math.floor(maxTokens) } : {}),
    ...(effort ? { reasoning: { effort } } : {}),
    ...(selectedProvider ? { provider: { order: [selectedProvider], allow_fallbacks: allowFallbacks, require_parameters: true } } : {})
  };
}

// Wie viele Token trägt ein Zeichen? Für Markup, CSS und Quellcode sind es drei bis vier Zeichen je
// Token. Die frühere Annahme „ein Byte = ein Token" war um den Faktor drei zu pessimistisch und hat
// Designs abgewiesen, die bequem in das Kontextfenster passen (gemeldet für GLM 5.2).
export const estimatedTokens = (text: string) => Math.ceil(Buffer.byteLength(text) / 3);

export function assertOpenRouterContext(model: OpenRouterModel, instructions: string, input: string, reservedOutputTokens?: number): void {
  if (!model.contextLength) return;
  const inputTokens = estimatedTokens(instructions) + estimatedTokens(input);
  const reserved = reservedOutputTokens && reservedOutputTokens > 0
    ? Math.min(reservedOutputTokens, Math.floor(model.contextLength * 0.4))
    : Math.max(1_024, Math.min(16_384, Math.floor(model.contextLength * 0.25)));
  if (inputTokens <= model.contextLength - reserved) return;
  throw Object.assign(new Error(`Die Eingabe ist für ${model.name} zu groß (rund ${Math.round(inputTokens / 1000)}k von ${Math.round(model.contextLength / 1000)}k Token). Wähle ein Modell mit größerem Kontextfenster oder markiere den Bereich, der geändert werden soll.`), { code: "MODEL_CONTEXT_TOO_SMALL", statusCode: 400, retryable: false });
}

export type OpenRouterStreamResult = { text: string; finishReason?: string; truncated: boolean; servedBy?: string };

// `finish_reason: "length"` heisst: der Anbieter hat mitten im Satz abgeschaltet. Bisher wurde das
// nicht ausgewertet — die abgeschnittene Antwort lief unbemerkt in den JSON-Parser und der ganze
// Lauf endete mit „es wurde nichts geändert", obwohl brauchbare Edits darin standen.
export function parseOpenRouterEventStream(raw: string): OpenRouterStreamResult {
  let output = "";
  let finishReason: string | undefined;
  // Welcher Anbieter den Lauf wirklich bedient hat — wichtig, wenn auf einen anderen ausgewichen wurde.
  let servedBy: string | undefined;
  for (const line of raw.split("\n")) {
    if (!line.startsWith("data:")) continue;
    const payload = line.slice(5).trim();
    if (!payload || payload === "[DONE]") continue;
    try {
      const event = JSON.parse(payload) as { error?: { code?: number | string; message?: unknown; metadata?: { error_type?: string; provider_name?: string; raw?: unknown } }; provider?: unknown; choices?: Array<{ delta?: { content?: unknown }; message?: { content?: unknown }; finish_reason?: unknown; native_finish_reason?: unknown }> };
      if (event.error) {
        // Der Wortlaut des Anbieters wird MITGENOMMEN. Ohne ihn stand beim Benutzer nur „Die Anfrage
        // konnte nicht abgeschlossen werden." — ohne jeden Hinweis, dass der Anbieter schuld war.
        const providerName = typeof event.error.metadata?.provider_name === "string" ? event.error.metadata.provider_name : undefined;
        const detail = typeof event.error.message === "string" && event.error.message.trim() ? event.error.message.trim().slice(0, 300) : undefined;
        if (typeof event.error.code === "number") throw openRouterHttpError(event.error.code, undefined, detail, providerName);
        const errorType = `${event.error.code ?? ""} ${event.error.metadata?.error_type ?? ""}`.toLowerCase();
        const retryable = ["rate_limit", "server", "provider", "timeout", "overloaded", "unavailable"].some((type) => errorType.includes(type));
        throw Object.assign(new Error(`${providerName ? `Der Anbieter ${providerName}` : "OpenRouter"} hat den KI-Lauf abgebrochen${detail ? `: ${detail}` : "."}`), { code: "CHAT_UPSTREAM", statusCode: 502, retryable, expose: true, ...(providerName ? { providerName } : {}) });
      }
      const choice = event.choices?.[0];
      const content = choice?.delta?.content ?? choice?.message?.content;
      if (typeof content === "string") output += content;
      const reason = choice?.finish_reason ?? choice?.native_finish_reason;
      if (typeof reason === "string" && reason) finishReason = reason;
      if (typeof event.provider === "string" && event.provider) servedBy = event.provider;
    } catch (error) {
      if (error && typeof error === "object" && "code" in error) throw error;
    }
  }
  return { text: output, ...(finishReason ? { finishReason } : {}), ...(servedBy ? { servedBy } : {}), truncated: finishReason === "length" || finishReason === "MAX_TOKENS" };
}

export function openRouterHttpError(status: number, retryAfter?: string | null, detail?: string, providerName?: string) {
  const retryAfterSeconds = retryAfter ? Number(retryAfter) : NaN;
  const retryAfterMs = Number.isFinite(retryAfterSeconds) && retryAfterSeconds >= 0 ? retryAfterSeconds * 1_000 : undefined;
  if (status === 401) return Object.assign(new Error("Der OpenRouter-API-Key ist ungültig oder wurde deaktiviert."), { code: "OPENROUTER_AUTH_INVALID", statusCode: 401, retryable: false, expose: true });
  if (status === 402) return Object.assign(new Error("Das OpenRouter-Konto verfügt nicht über ausreichendes Guthaben."), { code: "OPENROUTER_CREDITS_REQUIRED", statusCode: 402, retryable: false, expose: true });
  const wer = providerName ? `Der Anbieter ${providerName}` : "OpenRouter";
  return Object.assign(new Error(`${wer} hat den KI-Lauf abgelehnt (HTTP ${status})${detail ? `: ${detail}` : "."}`), { code: "CHAT_UPSTREAM", statusCode: 502, retryable: [408, 409, 425, 429, 500, 502, 503, 504].includes(status), expose: true, ...(providerName ? { providerName } : {}), ...(retryAfterMs === undefined ? {} : { retryAfterMs }) });
}
import { Buffer } from "node:buffer";
