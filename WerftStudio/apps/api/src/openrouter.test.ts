import { describe, expect, it } from "vitest";
import { assertOpenRouterContext, normalizeOpenRouterModelDetails, normalizeOpenRouterModels, openRouterHttpError, openRouterRequest, parseOpenRouterEventStream } from "./openrouter.js";

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

  it("normalizes and sorts only the providers of the requested model", () => {
    const details = normalizeOpenRouterModelDetails({ data: {
      id: "vendor/model", name: "Model", context_length: 128_000,
      reasoning: { supported_efforts: ["low", "high"], default_effort: "low" },
      endpoints: [
        { id: "expensive", provider_name: "Provider B", tag: "provider-b/model", context_length: 64_000, status: -2, pricing: { prompt: "0.000002", completion: "0.000004" } },
        { id: "cheap", provider_name: "Provider A", tag: "provider-a/model", context_length: 128_000, status: 0, throughput_last_30m: { p50: 84.5 }, uptime_last_5m: 99.9, pricing: { prompt: "0.000001", completion: "0.000003", input_cache_read: "0.0000001" } }
      ]
    } }, "vendor/model");
    expect(details?.model).toMatchObject({ id: "vendor/model", name: "Model", efforts: ["low", "high"], defaultEffort: "low" });
    expect(details?.endpoints.map((endpoint) => endpoint.providerName)).toEqual(["Provider A", "Provider B"]);
    expect(details?.endpoints[0]).toMatchObject({ providerSlug: "provider-a", promptPerToken: 0.000001, cacheReadPerToken: 0.0000001, throughputLast30m: 84.5, uptimeLast5m: 99.9 });
  });

  it("pins OpenRouter requests to the saved provider without fallbacks", () => {
    expect(openRouterRequest("vendor/model", "System", "Eingabe", "high", "provider-a")).toMatchObject({
      provider: { order: ["provider-a"], allow_fallbacks: false, require_parameters: true }
    });
  });

  it("reassembles streamed chat completions and rejects in-stream errors", () => {
    const raw = 'data: {"choices":[{"delta":{"content":"Hallo "}}]}\n: keepalive\ndata: {"choices":[{"delta":{"content":"Welt"}},{"finish_reason":"stop"}]}\ndata: [DONE]';
    expect(parseOpenRouterEventStream(raw)).toMatchObject({ text: "Hallo Welt", truncated: false });
    expect(() => parseOpenRouterEventStream('data: {"error":{"message":"kaputt"}}')).toThrow("OpenRouter hat den KI-Lauf abgebrochen.");
  });

  it("reports a stream that was cut off by the output limit", () => {
    // Genau dieser Fall lief bisher unbemerkt in den JSON-Parser und endete mit „nichts geändert".
    const raw = 'data: {"choices":[{"delta":{"content":"{\\"reply\\":\\"halb"},"finish_reason":"length"}]}\ndata: [DONE]';
    expect(parseOpenRouterEventStream(raw)).toMatchObject({ finishReason: "length", truncated: true });
  });

  it("asks the provider for enough output room instead of accepting its default cap", () => {
    expect(openRouterRequest("vendor/model", "System", "Eingabe", undefined, undefined, 32_000)).toMatchObject({ max_tokens: 32_000 });
    expect(openRouterRequest("vendor/model", "System", "Eingabe")).not.toHaveProperty("max_tokens");
  });

  it("rejects permanent stream errors without retrying them", () => {
    expect(() => parseOpenRouterEventStream('data: {"error":{"code":402}}')).toThrow(expect.objectContaining({ code: "OPENROUTER_CREDITS_REQUIRED", retryable: false }));
    expect(() => parseOpenRouterEventStream('data: {"error":{"metadata":{"error_type":"validation"}}}')).toThrow(expect.objectContaining({ code: "CHAT_UPSTREAM", retryable: false }));
    expect(() => parseOpenRouterEventStream('data: {"error":{"code":"server_error"}}')).toThrow(expect.objectContaining({ code: "CHAT_UPSTREAM", retryable: true }));
  });

  it("rejects oversized inputs before starting a paid model run", () => {
    const small = { provider: "openrouter" as const, id: "vendor/small", name: "Small", contextLength: 4_096, efforts: [] };
    expect(() => assertOpenRouterContext(small, "System", "x".repeat(30_000))).toThrow(expect.objectContaining({ code: "MODEL_CONTEXT_TOO_SMALL", retryable: false }));
    expect(() => assertOpenRouterContext({ ...small, contextLength: 128_000 }, "System", "x".repeat(10_000))).not.toThrow();
  });

  it("measures the input in tokens, not in bytes", () => {
    // Ein 400-KB-Design passt in ein 200k-Modell (rund 133k Token). Die alte Byte-Rechnung wies es ab
    // („Die Eingabe ist für Z.ai: GLM 5.2 zu groß"), obwohl reichlich Platz war.
    const model = { provider: "openrouter" as const, id: "z-ai/glm", name: "GLM", contextLength: 200_000, efforts: [] };
    expect(() => assertOpenRouterContext(model, "System", "x".repeat(400_000), 32_000)).not.toThrow();
    expect(() => assertOpenRouterContext(model, "System", "x".repeat(900_000), 32_000)).toThrow(expect.objectContaining({ code: "MODEL_CONTEXT_TOO_SMALL" }));
  });

  it("does not retry authentication or credit errors", () => {
    expect(openRouterHttpError(401)).toMatchObject({ code: "OPENROUTER_AUTH_INVALID", retryable: false });
    expect(openRouterHttpError(402)).toMatchObject({ code: "OPENROUTER_CREDITS_REQUIRED", retryable: false });
    expect(openRouterHttpError(503)).toMatchObject({ code: "CHAT_UPSTREAM", retryable: true });
    expect(openRouterHttpError(429, "7")).toMatchObject({ retryable: true, retryAfterMs: 7_000 });
  });
});
