export type CodexTransportError = Error & {
  code?: string;
  retryable?: boolean;
  statusCode?: number;
};

const retryableHttpStatuses = new Set([408, 409, 425, 429, 500, 502, 503, 504]);

export function parseCodexEventStream(raw: string): string {
  let outputText = "";
  for (const line of raw.split("\n")) {
    if (!line.startsWith("data:")) continue;
    try {
      const event = JSON.parse(line.slice(5).trim()) as { type?: string; delta?: string; response?: { output?: Array<{ content?: Array<{ text?: string }> }> } };
      if (event.type === "response.output_text.delta" && typeof event.delta === "string") outputText += event.delta;
      else if (event.type === "response.completed" && !outputText) outputText = (event.response?.output ?? []).flatMap((item) => item.content ?? []).map((part) => part.text ?? "").join("");
    } catch { /* Nicht-JSON-Zeilen im Stream ignorieren */ }
  }
  return outputText;
}

export function codexHttpError(status: number): CodexTransportError {
  return Object.assign(new Error(`OpenAI hat den KI-Lauf abgelehnt (HTTP ${status}).`), {
    code: "CHAT_UPSTREAM",
    statusCode: 502,
    retryable: retryableHttpStatuses.has(status)
  });
}

export function isRetryableCodexError(error: unknown): boolean {
  if (!error || typeof error !== "object") return false;
  const details = error as CodexTransportError;
  if (details.retryable === true) return true;
  if (details.retryable === false || details.code) return false;
  return error instanceof TypeError || details.name === "AbortError" || details.name === "TimeoutError";
}
