export const openRouterApi = {
  keyUrl: "https://openrouter.ai/api/v1/key",
  modelsUrl: "https://openrouter.ai/api/v1/models",
  chatUrl: "https://openrouter.ai/api/v1/chat/completions"
} as const;

export type OpenRouterModel = {
  provider: "openrouter";
  id: string;
  name: string;
  contextLength?: number;
  efforts: string[];
  defaultEffort?: string;
};

type RawOpenRouterModel = {
  id?: unknown;
  name?: unknown;
  context_length?: unknown;
  architecture?: { output_modalities?: unknown };
  reasoning?: { supported_efforts?: unknown; default_effort?: unknown; mandatory?: unknown };
};

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

export function openRouterRequest(model: string, instructions: string, input: string, effort?: string) {
  return {
    model,
    messages: [{ role: "system", content: instructions }, { role: "user", content: input }],
    stream: true,
    ...(effort ? { reasoning: { effort } } : {})
  };
}

export function assertOpenRouterContext(model: OpenRouterModel, instructions: string, input: string): void {
  if (!model.contextLength) return;
  // Ein Token kann im unguenstigsten Fall nur ein UTF-8-Byte tragen. Die Bytezahl ist deshalb eine
  // sichere Obergrenze, ohne fuer jeden der dynamischen Provider einen eigenen Tokenizer einzubauen.
  const maximumInputTokens = Buffer.byteLength(instructions) + Buffer.byteLength(input);
  const reservedOutputTokens = Math.max(1_024, Math.min(16_384, Math.floor(model.contextLength * 0.25)));
  if (maximumInputTokens <= model.contextLength - reservedOutputTokens) return;
  throw Object.assign(new Error(`Die Eingabe ist für ${model.name} zu groß. Wähle ein Modell mit größerem Kontextfenster.`), { code: "MODEL_CONTEXT_TOO_SMALL", statusCode: 400, retryable: false });
}

export function parseOpenRouterEventStream(raw: string): string {
  let output = "";
  for (const line of raw.split("\n")) {
    if (!line.startsWith("data:")) continue;
    const payload = line.slice(5).trim();
    if (!payload || payload === "[DONE]") continue;
    try {
      const event = JSON.parse(payload) as { error?: { code?: number | string; metadata?: { error_type?: string } }; choices?: Array<{ delta?: { content?: unknown }; message?: { content?: unknown } }> };
      if (event.error) {
        if (typeof event.error.code === "number") throw openRouterHttpError(event.error.code);
        const errorType = `${event.error.code ?? ""} ${event.error.metadata?.error_type ?? ""}`.toLowerCase();
        const retryable = ["rate_limit", "server", "provider", "timeout", "overloaded", "unavailable"].some((type) => errorType.includes(type));
        throw Object.assign(new Error("OpenRouter hat den KI-Lauf abgebrochen."), { code: "CHAT_UPSTREAM", statusCode: 502, retryable });
      }
      const content = event.choices?.[0]?.delta?.content ?? event.choices?.[0]?.message?.content;
      if (typeof content === "string") output += content;
    } catch (error) {
      if (error && typeof error === "object" && "code" in error) throw error;
    }
  }
  return output;
}

export function openRouterHttpError(status: number, retryAfter?: string | null) {
  const retryAfterSeconds = retryAfter ? Number(retryAfter) : NaN;
  const retryAfterMs = Number.isFinite(retryAfterSeconds) && retryAfterSeconds >= 0 ? retryAfterSeconds * 1_000 : undefined;
  if (status === 401) return Object.assign(new Error("Der OpenRouter-API-Key ist ungültig oder wurde deaktiviert."), { code: "OPENROUTER_AUTH_INVALID", statusCode: 401, retryable: false });
  if (status === 402) return Object.assign(new Error("Das OpenRouter-Konto verfügt nicht über ausreichendes Guthaben."), { code: "OPENROUTER_CREDITS_REQUIRED", statusCode: 402, retryable: false });
  return Object.assign(new Error(`OpenRouter hat den KI-Lauf abgelehnt (HTTP ${status}).`), { code: "CHAT_UPSTREAM", statusCode: 502, retryable: [408, 409, 425, 429, 500, 502, 503, 504].includes(status), ...(retryAfterMs === undefined ? {} : { retryAfterMs }) });
}
import { Buffer } from "node:buffer";
