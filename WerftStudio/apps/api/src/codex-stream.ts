export type CodexTransportError = Error & {
  code?: string;
  retryable?: boolean;
  statusCode?: number;
};

const retryableHttpStatuses = new Set([408, 409, 425, 429, 500, 502, 503, 504]);

export type CodexStreamResult = { text: string; truncated: boolean };

// `response.incomplete` mit dem Grund `max_output_tokens` heisst: die Antwort bricht mitten im Satz
// ab. Wer das nicht auswertet, schickt ein halbes JSON in den Parser und meldet dem Benutzer
// „es wurde nichts geändert", obwohl das Modell brauchbare Arbeit geliefert hat.
export function parseCodexEventStream(raw: string): CodexStreamResult {
  let outputText = "";
  let truncated = false;
  for (const line of raw.split("\n")) {
    if (!line.startsWith("data:")) continue;
    try {
      const event = JSON.parse(line.slice(5).trim()) as { type?: string; delta?: string; response?: { status?: string; incomplete_details?: { reason?: string }; output?: Array<{ content?: Array<{ text?: string }> }> } };
      if (event.type === "response.output_text.delta" && typeof event.delta === "string") outputText += event.delta;
      else if ((event.type === "response.completed" || event.type === "response.incomplete") && !outputText) outputText = (event.response?.output ?? []).flatMap((item) => item.content ?? []).map((part) => part.text ?? "").join("");
      if (event.type === "response.incomplete" || event.response?.status === "incomplete") truncated = true;
    } catch { /* Nicht-JSON-Zeilen im Stream ignorieren */ }
  }
  return { text: outputText, truncated };
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
