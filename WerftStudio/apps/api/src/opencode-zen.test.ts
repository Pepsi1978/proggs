import { describe, expect, it } from "vitest";
import { assertZenContext, normalizeZenFreeModels, parseZenEventStream, withVerifiedZenReasoning, zenHttpError, zenRequest } from "./opencode-zen.js";

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
      { provider: "opencode-zen", id: "big-pickle", name: "Big Pickle", contextLength: 200_000, inputTokenLimit: 160_000, maxOutputTokens: 32_000, efforts: ["none", "high"], reasoning: true, reasoningControl: "toggle" },
      { provider: "opencode-zen", id: "reasoner-free", name: "Reasoner Free", contextLength: 200_000, maxOutputTokens: 32_000, efforts: ["low", "high"], reasoning: true, reasoningControl: "effort" }
    ]);
  });

  it("does not guess capabilities or transport when metadata lags behind", () => {
    expect(normalizeZenFreeModels({ data: [{ id: "future-free" }] }, metadata)).toEqual([]);
  });

  it("adds only live-verified thinking controls missing from upstream metadata", () => {
    const models = {
      "mimo-v2.5-free": { ...metadata.opencode.models["big-pickle"], name: "MiMo" },
      "laguna-s-2.1-free": { ...metadata.opencode.models["reasoner-free"], name: "Laguna", reasoning_options: [{ type: "effort", values: ["low", "medium", "high"] }] }
    };
    expect(normalizeZenFreeModels({ data: Object.keys(models).map((id) => ({ id })) }, { opencode: { ...metadata.opencode, models } })).toEqual([
      expect.objectContaining({ id: "laguna-s-2.1-free", efforts: ["none", "low", "medium", "high"], reasoningControl: "effort" }),
      expect.objectContaining({ id: "mimo-v2.5-free", efforts: ["none", "high"], reasoningControl: "toggle" })
    ]);
  });

  it("normalizes every current model, including stored legacy catalog rows", () => {
    const model = (id: string, efforts: string[]) => withVerifiedZenReasoning({ provider: "opencode-zen", id, name: id, efforts, reasoning: true });
    expect([
      model("big-pickle", []), model("mimo-v2.5-free", []), model("nemotron-3-ultra-free", []), model("north-mini-code-free", ["none", "high"]),
      model("laguna-s-2.1-free", ["low", "medium", "high"]), model("ling-3.0-flash-free", ["low", "medium", "high"]), model("deepseek-v4-flash-free", ["high", "max"])
    ]).toEqual([
      expect.objectContaining({ efforts: ["none", "high"], reasoningControl: "toggle" }),
      expect.objectContaining({ efforts: ["none", "high"], reasoningControl: "toggle" }),
      expect.objectContaining({ efforts: ["none", "high"], reasoningControl: "toggle" }),
      expect.objectContaining({ efforts: ["none", "high"], reasoningControl: "toggle" }),
      expect.objectContaining({ efforts: ["none", "low", "medium", "high"], reasoningControl: "effort" }),
      expect.objectContaining({ efforts: ["none", "low", "medium", "high"], reasoningControl: "effort" }),
      expect.objectContaining({ efforts: ["high", "max"], reasoningControl: "effort" })
    ]);
  });

  it("excludes free models that require another Zen protocol", () => {
    const incompatible = { opencode: { ...metadata.opencode, models: { "claude-free": { ...metadata.opencode.models["reasoner-free"], provider: { npm: "@ai-sdk/anthropic" } } } } };
    expect(normalizeZenFreeModels({ data: [{ id: "claude-free" }] }, incompatible)).toEqual([]);
  });

  it("sends only a selected, supported reasoning effort", () => {
    expect(zenRequest("reasoner-free", "System", "Eingabe")).not.toHaveProperty("reasoning_effort");
    expect(zenRequest("reasoner-free", "System", "Eingabe", "high", 32_000)).toMatchObject({ reasoning_effort: "high", max_tokens: 32_000, stream: true });
    expect(zenRequest("big-pickle", "System", "Eingabe", "none")).toMatchObject({ thinking: { type: "disabled" } });
    expect(zenRequest("big-pickle", "System", "Eingabe", "high")).toMatchObject({ thinking: { type: "enabled" } });
    expect(zenRequest("big-pickle", "System", "Eingabe", "none")).not.toHaveProperty("reasoning_effort");
    expect(zenRequest("big-pickle", "System", "Eingabe", "high")).not.toHaveProperty("reasoning_effort");
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
