export class ApiError extends Error { constructor(readonly code: string, message: string, readonly status: number) { super(message); } }
export async function api<T>(path: string, init?: RequestInit): Promise<T> {
  // content-type nur setzen wenn wirklich ein Body mitgeht: Fastify lehnt "application/json ohne Body"
  // sonst mit FST_ERR_CTP_EMPTY_JSON_BODY ab (traf alle DELETE-Aufrufe).
  const response = await fetch(`/api/v1${path}`, { ...init, credentials: "include", headers: { ...(init?.body != null ? { "content-type": "application/json" } : {}), ...init?.headers } });
  if (!response.ok) { const body = await response.json().catch(() => ({ code: "NETWORK_ERROR", message: "Serverantwort konnte nicht gelesen werden." })) as { code: string; message: string }; throw new ApiError(body.code, body.message, response.status); }
  return response.json() as Promise<T>;
}

// Ein Lauf am Design dauert je nach Modell eine knappe bis zwei Minuten. Statt einer stillen Wartezeit
// liest diese Funktion den Ereignisstrom des Servers mit und meldet jeden Schritt sofort weiter.
// Ein `signal` bricht den Lauf ab — der Server beendet dann auch den laufenden Modellaufruf.
export async function apiEventStream<T>(path: string, body: unknown, onEvent: (event: string, data: Record<string, unknown>) => void, signal?: AbortSignal): Promise<T> {
  const response = await fetch(`/api/v1${path}`, { method: "POST", credentials: "include", ...(signal ? { signal } : {}), headers: { "content-type": "application/json", accept: "text/event-stream" }, body: JSON.stringify(body) });
  if (!response.ok || !response.body) {
    const failure = await response.json().catch(() => ({ code: "NETWORK_ERROR", message: "Serverantwort konnte nicht gelesen werden." })) as { code: string; message: string };
    throw new ApiError(failure.code, failure.message, response.status);
  }
  const reader = response.body.getReader(), decoder = new TextDecoder();
  let buffer = "", result: T | undefined, failure: ApiError | undefined;
  for (;;) {
    const { done, value } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });
    for (let cut = buffer.indexOf("\n\n"); cut >= 0; cut = buffer.indexOf("\n\n")) {
      const frame = buffer.slice(0, cut);
      buffer = buffer.slice(cut + 2);
      const lines = frame.split("\n");
      const event = lines.find((line) => line.startsWith("event:"))?.slice(6).trim();
      const payload = lines.filter((line) => line.startsWith("data:")).map((line) => line.slice(5).trim()).join("");
      if (!event || !payload) continue; // Pulszeilen (": puls …") halten nur die Verbindung offen.
      let data: Record<string, unknown>;
      try { data = JSON.parse(payload) as Record<string, unknown>; }
      catch { continue; }
      if (event === "done") result = data as T;
      else if (event === "error") failure = new ApiError(String(data.code ?? "INTERNAL_ERROR"), String(data.message ?? "Der Lauf ist fehlgeschlagen."), 500);
      else onEvent(event, data);
    }
  }
  if (failure) throw failure;
  if (!result) throw new ApiError("CHAT_STREAM_INCOMPLETE", "Die Verbindung ist während des Laufs abgerissen. Bereits übernommene Änderungen bleiben erhalten.", 0);
  return result;
}

// Der Upload eines Projekts dauert bei grossen Paketen Minuten. Er behandelte einen Abbruch schon
// immer sauber — es fehlte nur der Weg dorthin: ohne `signal` konnte ihn niemand von aussen beenden.
export function apiFormProgress<T>(path: string, body: FormData, onProgress: (loaded: number, total: number) => void, signal?: AbortSignal): Promise<T> {
  return new Promise((resolve, reject) => {
    if (signal?.aborted) { reject(new ApiError("UPLOAD_CANCELLED", "Der Upload wurde abgebrochen.", 0)); return; }
    const request = new XMLHttpRequest();
    const stop = () => request.abort();
    signal?.addEventListener("abort", stop, { once: true });
    request.addEventListener("loadend", () => signal?.removeEventListener("abort", stop));
    request.open("POST", `/api/v1${path}`);
    request.withCredentials = true;
    request.upload.addEventListener("progress", (event) => onProgress(event.loaded, event.lengthComputable ? event.total : 0));
    request.addEventListener("load", () => {
      let payload: unknown;
      try { payload = JSON.parse(request.responseText); }
      catch { payload = { code: "NETWORK_ERROR", message: "Serverantwort konnte nicht gelesen werden." }; }
      if (request.status >= 200 && request.status < 300) resolve(payload as T);
      else {
        const error = payload as { code?: string; message?: string };
        reject(new ApiError(error.code ?? "NETWORK_ERROR", error.message ?? "Der Upload ist fehlgeschlagen.", request.status));
      }
    });
    request.addEventListener("error", () => reject(new ApiError("NETWORK_ERROR", "Der Server ist während des Uploads nicht erreichbar.", 0)));
    request.addEventListener("abort", () => reject(new ApiError("UPLOAD_CANCELLED", "Der Upload wurde abgebrochen.", 0)));
    request.send(body);
  });
}
