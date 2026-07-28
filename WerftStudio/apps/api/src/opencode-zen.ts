import { Buffer } from "node:buffer";

export const zenApi = {
  modelsUrl: "https://opencode.ai/zen/v1/models",
  chatUrl: "https://opencode.ai/zen/v1/chat/completions",
  metadataUrl: "https://models.dev/api.json"
} as const;

export type ZenModel = {
  provider: "opencode-zen";
  id: string;
  name: string;
  contextLength?: number;
  inputTokenLimit?: number;
  maxOutputTokens?: number;
  efforts: string[];
  reasoning: boolean;
  reasoningControl?: "toggle" | "effort";
};

const recordValue = (value: unknown): Record<string, unknown> | undefined => value && typeof value === "object" && !Array.isArray(value) ? value as Record<string, unknown> : undefined;
const numberValue = (value: unknown): number | undefined => typeof value === "number" && Number.isFinite(value) && value > 0 ? value : undefined;
const fallbackName = (id: string) => id.split("-").map((part) => part ? part[0]!.toUpperCase() + part.slice(1) : part).join(" ");
// Zen exposes these controls even though models.dev currently omits or narrows them.
// Keep aliases such as DeepSeek low/medium hidden because they do not add a distinct tier.
const verifiedThinkingToggles = new Set(["big-pickle", "mimo-v2.5-free", "nemotron-3-ultra-free", "north-mini-code-free"]);
const verifiedThinkingDisable = new Set(["laguna-s-2.1-free", "ling-3.0-flash-free"]);

export function withVerifiedZenReasoning(model: ZenModel): ZenModel {
  const { reasoningControl: _oldControl, ...base } = model;
  const reasoningControl = verifiedThinkingToggles.has(model.id) ? "toggle" as const : "effort" as const;
  const efforts = model.reasoning && reasoningControl === "toggle" ? ["none", "high"] : [...model.efforts];
  if (model.reasoning && verifiedThinkingDisable.has(model.id) && !efforts.includes("none")) efforts.unshift("none");
  return { ...base, efforts, ...(model.reasoning && efforts.length ? { reasoningControl } : {}) };
}

export function normalizeZenFreeModels(availableValue: unknown, metadataValue: unknown): ZenModel[] {
  const availableRoot = recordValue(availableValue);
  const available = Array.isArray(availableRoot?.data) ? availableRoot.data : [];
  const metadataRoot = recordValue(metadataValue);
  const provider = recordValue(metadataRoot?.opencode);
  const metadataModels = recordValue(provider?.models) ?? {};
  const ids = [...new Set(available.flatMap((entry) => {
    const row = recordValue(entry);
    return typeof row?.id === "string" && row.id.trim() ? [row.id.trim()] : [];
  }))];

  return ids.flatMap((id): ZenModel[] => {
    const metadata = recordValue(metadataModels[id]);
    const modelProvider = recordValue(metadata?.provider);
    const transport = modelProvider?.npm ?? provider?.npm;
    const cost = recordValue(metadata?.cost);
    const free = id.endsWith("-free") || id === "big-pickle" || (cost?.input === 0 && cost?.output === 0);
    if (!free || !metadata || transport !== "@ai-sdk/openai-compatible" || metadata.status === "deprecated") return [];
    const modalities = recordValue(metadata?.modalities);
    if (Array.isArray(modalities?.output) && !modalities.output.includes("text")) return [];
    const reasoningOptions = Array.isArray(metadata?.reasoning_options) ? metadata.reasoning_options : [];
    const efforts = [...new Set(reasoningOptions.flatMap((entry) => {
      const option = recordValue(entry);
      return option?.type === "effort" && Array.isArray(option.values) ? option.values.filter((value): value is string => typeof value === "string" && value.length > 0) : [];
    }))];
    const limit = recordValue(metadata?.limit);
    const contextLength = numberValue(limit?.context);
    const inputTokenLimit = numberValue(limit?.input);
    const maxOutputTokens = numberValue(limit?.output);
    return [withVerifiedZenReasoning({
      provider: "opencode-zen",
      id,
      name: typeof metadata?.name === "string" && metadata.name.trim() ? metadata.name.trim() : fallbackName(id),
      ...(contextLength ? { contextLength } : {}),
      ...(inputTokenLimit ? { inputTokenLimit } : {}),
      ...(maxOutputTokens ? { maxOutputTokens } : {}),
      efforts,
      reasoning: metadata?.reasoning === true
    })];
  }).sort((left, right) => left.name.localeCompare(right.name));
}

export function zenRequest(model: string, instructions: string, input: string, effort?: string, maxTokens?: number, stream = true) {
  return {
    model,
    messages: [{ role: "system", content: instructions }, { role: "user", content: input }],
    stream,
    ...(maxTokens && maxTokens > 0 ? { max_tokens: Math.floor(maxTokens) } : {}),
    ...(model === "big-pickle" && effort === "none" ? { thinking: { type: "disabled" } } : {}),
    ...(model === "big-pickle" && effort === "high" ? { thinking: { type: "enabled" } } : {}),
    ...(model !== "big-pickle" && effort ? { reasoning_effort: effort } : {})
  };
}

export type ZenStreamResult = { text: string; finishReason?: string; truncated: boolean };

export function parseZenEventStream(raw: string): ZenStreamResult {
  let text = "";
  let finishReason: string | undefined;
  for (const line of raw.split("\n")) {
    if (!line.startsWith("data:")) continue;
    const payload = line.slice(5).trim();
    if (!payload || payload === "[DONE]") continue;
    try {
      const event = JSON.parse(payload) as { error?: { type?: unknown; code?: unknown; message?: unknown }; choices?: Array<{ delta?: { content?: unknown }; message?: { content?: unknown }; finish_reason?: unknown }> };
      if (event.error) {
        const detail = typeof event.error.message === "string" ? event.error.message.trim().slice(0, 300) : undefined;
        const type = `${event.error.type ?? ""} ${event.error.code ?? ""}`.toLowerCase();
        throw Object.assign(new Error(`OpenCode Zen hat den KI-Lauf abgebrochen${detail ? `: ${detail}` : "."}`), { code: "CHAT_UPSTREAM", statusCode: 502, retryable: ["rate", "timeout", "server", "overload", "unavailable"].some((value) => type.includes(value)) || [408, 409, 425, 429, 500, 502, 503, 504].includes(Number(event.error.code)), expose: true });
      }
      const choice = event.choices?.[0];
      const content = choice?.delta?.content ?? choice?.message?.content;
      if (typeof content === "string") text += content;
      if (typeof choice?.finish_reason === "string" && choice.finish_reason) finishReason = choice.finish_reason;
    } catch (error) {
      if (error && typeof error === "object" && "code" in error) throw error;
    }
  }
  return { text, ...(finishReason ? { finishReason } : {}), truncated: finishReason === "length" || finishReason === "MAX_TOKENS" };
}

function errorDetail(raw?: string): string | undefined {
  if (!raw) return undefined;
  try {
    const root = recordValue(JSON.parse(raw));
    const error = recordValue(root?.error);
    const message = error?.message ?? root?.message;
    return typeof message === "string" && message.trim() ? message.trim().slice(0, 300) : undefined;
  } catch { return undefined; }
}

export function zenHttpError(status: number, retryAfter?: string | null, raw?: string) {
  const detail = errorDetail(raw);
  const retryAfterSeconds = retryAfter ? Number(retryAfter) : NaN;
  const retryAfterMs = Number.isFinite(retryAfterSeconds) && retryAfterSeconds >= 0 ? retryAfterSeconds * 1_000 : undefined;
  if (status === 401) return Object.assign(new Error(`OpenCode Zen hat den Zugriff abgelehnt${detail ? `: ${detail}` : "."}`), { code: "ZEN_AUTH_INVALID", statusCode: 401, retryable: false, expose: true });
  if (status === 402) return Object.assign(new Error("Das OpenCode-Zen-Konto verfügt nicht über ausreichendes Guthaben."), { code: "ZEN_CREDITS_REQUIRED", statusCode: 402, retryable: false, expose: true });
  return Object.assign(new Error(`OpenCode Zen hat den KI-Lauf abgelehnt (HTTP ${status})${detail ? `: ${detail}` : "."}`), { code: "CHAT_UPSTREAM", statusCode: 502, retryable: [408, 409, 425, 429, 500, 502, 503, 504].includes(status), expose: true, ...(retryAfterMs === undefined ? {} : { retryAfterMs }) });
}

export function assertZenContext(model: ZenModel, instructions: string, input: string, reservedOutputTokens?: number): void {
  if (!model.contextLength) return;
  const inputTokens = Math.ceil((Buffer.byteLength(instructions) + Buffer.byteLength(input)) / 3);
  const reserved = reservedOutputTokens && reservedOutputTokens > 0 ? Math.min(reservedOutputTokens, Math.floor(model.contextLength * 0.4)) : Math.max(1_024, Math.min(16_384, Math.floor(model.contextLength * 0.25)));
  const limit = Math.min(model.contextLength - reserved, model.inputTokenLimit ?? Number.POSITIVE_INFINITY);
  if (inputTokens <= limit) return;
  throw Object.assign(new Error(`Die Eingabe ist für ${model.name} zu groß (rund ${Math.round(inputTokens / 1000)}k von ${Math.round(limit / 1000)}k Eingabe-Token). Wähle ein Modell mit größerem Kontextfenster oder markiere den Bereich, der geändert werden soll.`), { code: "MODEL_CONTEXT_TOO_SMALL", statusCode: 400, retryable: false });
}
