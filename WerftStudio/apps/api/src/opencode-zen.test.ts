import { describe, expect, it } from "vitest";
import { assertZenContext, normalizeZenFreeModels, parseZenEventStream, zenHttpError, zenRequest } from "./opencode-zen.js";

const available = { data: [
  { id: "paid-model" },
  { id: "reasoner-free" },
  { id: "big-pickle" },
  { id: "retired-free" }
] };
const metadata = { opencode: { npm: "@ai-sdk/openai-compatible", models: {
  "paid-model": { name: "Paid", cost: { input: 1, output: 2 }, reasoning: false, limit: { context: 10_000, output: 1_000 }, modalities: { output: ["text"] } },
  "reasoner-free": { name: "Reasoner Free", cost: { input: 0, output: 0 }, reasoning: true, reasoning_options: [{ type: "toggle" }, { type: "effort", values: ["low", "high", null] }], limit: { context: 200_000, output: 32_000 }, modalities: { output: ["text"] } },
  "big-pickle": { name: "Big Pickle", cost: { input: 0, output: 0 }, reasoning: true, reasoning_options: [], limit: { context: 200_000, input: 160_000, output: 32_000 }, modalities: { output: ["text"] } },
  "retired-free": { name: "Retired", status: "deprecated", cost: { input: 0, output: 0 }, reasoning: true, limit: { context: 10_000, output: 1_000 } }
} } };

describe("OpenCode Zen adapter", () => {
  it("combines the live catalog with free-model capability metadata", () => {
    expect(normalizeZenFreeModels(available, metadata)).toEqual([
      { provider: "opencode-zen", id: "big-pickle", name: "Big Pickle", contextLength: 200_000, inputTokenLimit: 160_000, maxOutputTokens: 32_000, efforts: [], reasoning: true },
      { provider: "opencode-zen", id: "reasoner-free", name: "Reasoner Free", contextLength: 200_000, maxOutputTokens: 32_000, efforts: ["low", "high"], reasoning: true }
    ]);
  });

  it("does not guess capabilities or transport when metadata lags behind", () => {
    expect(normalizeZenFreeModels({ data: [{ id: "future-free" }] }, metadata)).toEqual([]);
  });

  it("excludes free models that require another Zen protocol", () => {
    const incompatible = { opencode: { ...metadata.opencode, models: { "claude-free": { ...metadata.opencode.models["reasoner-free"], provider: { npm: "@ai-sdk/anthropic" } } } } };
    expect(normalizeZenFreeModels({ data: [{ id: "claude-free" }] }, incompatible)).toEqual([]);
  });

  it("sends only a selected, supported reasoning effort", () => {
    expect(zenRequest("reasoner-free", "System", "Eingabe")).not.toHaveProperty("reasoning_effort");
    expect(zenRequest("reasoner-free", "System", "Eingabe", "high", 32_000)).toMatchObject({ reasoning_effort: "high", max_tokens: 32_000, stream: true });
  });

  it("reassembles chat-completion streams and reports truncation", () => {
    const raw = 'data: {"choices":[{"delta":{"content":"Hallo "}}]}\ndata: {"choices":[{"delta":{"content":"Zen"},"finish_reason":"length"}]}\ndata: [DONE]';
    expect(parseZenEventStream(raw)).toEqual({ text: "Hallo Zen", finishReason: "length", truncated: true });
  });

  it("classifies authentication and transient upstream errors", () => {
    expect(zenHttpError(401, null, '{"error":{"message":"Invalid API key."}}')).toMatchObject({ code: "ZEN_AUTH_INVALID", retryable: false });
    expect(zenHttpError(429, "3")).toMatchObject({ code: "CHAT_UPSTREAM", retryable: true, retryAfterMs: 3_000 });
    expect(() => parseZenEventStream('data: {"error":{"type":"tokens","code":"rate_limit_exceeded","message":"slow down"}}')).toThrow(expect.objectContaining({ retryable: true }));
  });

  it("rejects oversized design prompts before inference", () => {
    const model = { provider: "opencode-zen" as const, id: "small-free", name: "Small", contextLength: 4_096, inputTokenLimit: 3_000, maxOutputTokens: 1_024, efforts: [], reasoning: false };
    expect(() => assertZenContext(model, "System", "x".repeat(30_000), 1_024)).toThrow(expect.objectContaining({ code: "MODEL_CONTEXT_TOO_SMALL", retryable: false }));
  });
});
