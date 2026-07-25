import { describe, expect, it } from "vitest";
import { codexHttpError, isRetryableCodexError, parseCodexEventStream } from "./codex-stream.js";

describe("Codex stream transport", () => {
  it("reassembles streamed output and ignores non-JSON keepalives", () => {
    const raw = [
      "data: {\"type\":\"response.output_text.delta\",\"delta\":\"Hallo \"}",
      "data: keepalive",
      "data: {\"type\":\"response.output_text.delta\",\"delta\":\"Welt\"}"
    ].join("\n");
    expect(parseCodexEventStream(raw)).toBe("Hallo Welt");
  });

  it("uses the completed response when no delta events arrived", () => {
    const raw = 'data: {"type":"response.completed","response":{"output":[{"content":[{"text":"fertig"}]}]}}';
    expect(parseCodexEventStream(raw)).toBe("fertig");
  });

  it("retries terminated streams and transient HTTP responses only", () => {
    expect(isRetryableCodexError(new TypeError("terminated"))).toBe(true);
    expect(isRetryableCodexError(Object.assign(new Error("timeout"), { name: "TimeoutError" }))).toBe(true);
    expect(isRetryableCodexError(codexHttpError(503))).toBe(true);
    expect(isRetryableCodexError(codexHttpError(401))).toBe(false);
    expect(isRetryableCodexError(Object.assign(new Error("invalid"), { code: "CHAT_INVALID", retryable: false }))).toBe(false);
  });
});
