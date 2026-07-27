import { describe, expect, it } from "vitest";
import { assertOpenRouterContext, normalizeOpenRouterModels, openRouterHttpError, openRouterRequest, parseOpenRouterEventStream } from "./openrouter.js";

describe("OpenRouter adapter", () => {
  it("normalizes text models and their supported reasoning efforts", () => {
    expect(normalizeOpenRouterModels({ data: [
      { id: "vendor/text", name: "Text", context_length: 128000, architecture: { output_modalities: ["text"] }, reasoning: { supported_efforts: ["none", "low", "high"], default_effort: "low", mandatory: true } },
      { id: "vendor/image", name: "Image", architecture: { output_modalities: ["image"] } }
    ] })).toEqual([{ provider: "openrouter", id: "vendor/text", name: "Text", contextLength: 128000, efforts: ["low", "high"], defaultEffort: "low" }]);
  });

  it("builds a provider request without inventing unsupported reasoning settings", () => {
    expect(openRouterRequest("vendor/model", "System", "Eingabe")).not.toHaveProperty("reasoning");
    expect(openRouterRequest("vendor/model", "System", "Eingabe", "high")).toMatchObject({ model: "vendor/model", reasoning: { effort: "high" } });
  });

  it("reassembles streamed chat completions and rejects in-stream errors", () => {
    const raw = 'data: {"choices":[{"delta":{"content":"Hallo "}}]}\n: keepalive\ndata: {"choices":[{"delta":{"content":"Welt"}}]}\ndata: [DONE]';
    expect(parseOpenRouterEventStream(raw)).toBe("Hallo Welt");
    expect(() => parseOpenRouterEventStream('data: {"error":{"message":"kaputt"}}')).toThrow("OpenRouter hat den KI-Lauf abgebrochen.");
  });

  it("rejects permanent stream errors without retrying them", () => {
    expect(() => parseOpenRouterEventStream('data: {"error":{"code":402}}')).toThrow(expect.objectContaining({ code: "OPENROUTER_CREDITS_REQUIRED", retryable: false }));
    expect(() => parseOpenRouterEventStream('data: {"error":{"metadata":{"error_type":"validation"}}}')).toThrow(expect.objectContaining({ code: "CHAT_UPSTREAM", retryable: false }));
    expect(() => parseOpenRouterEventStream('data: {"error":{"code":"server_error"}}')).toThrow(expect.objectContaining({ code: "CHAT_UPSTREAM", retryable: true }));
  });

  it("rejects oversized inputs before starting a paid model run", () => {
    const small = { provider: "openrouter" as const, id: "vendor/small", name: "Small", contextLength: 4_096, efforts: [] };
    expect(() => assertOpenRouterContext(small, "System", "x".repeat(10_000))).toThrow(expect.objectContaining({ code: "MODEL_CONTEXT_TOO_SMALL", retryable: false }));
    expect(() => assertOpenRouterContext({ ...small, contextLength: 128_000 }, "System", "x".repeat(10_000))).not.toThrow();
  });

  it("does not retry authentication or credit errors", () => {
    expect(openRouterHttpError(401)).toMatchObject({ code: "OPENROUTER_AUTH_INVALID", retryable: false });
    expect(openRouterHttpError(402)).toMatchObject({ code: "OPENROUTER_CREDITS_REQUIRED", retryable: false });
    expect(openRouterHttpError(503)).toMatchObject({ code: "CHAT_UPSTREAM", retryable: true });
    expect(openRouterHttpError(429, "7")).toMatchObject({ retryable: true, retryAfterMs: 7_000 });
  });
});
