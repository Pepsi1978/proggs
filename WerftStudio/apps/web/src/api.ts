export class ApiError extends Error { constructor(readonly code: string, message: string, readonly status: number) { super(message); } }
export async function api<T>(path: string, init?: RequestInit): Promise<T> {
  // content-type nur setzen wenn wirklich ein Body mitgeht: Fastify lehnt "application/json ohne Body"
  // sonst mit FST_ERR_CTP_EMPTY_JSON_BODY ab (traf alle DELETE-Aufrufe).
  const response = await fetch(`/api/v1${path}`, { ...init, credentials: "include", headers: { ...(init?.body != null ? { "content-type": "application/json" } : {}), ...init?.headers } });
  if (!response.ok) { const body = await response.json().catch(() => ({ code: "NETWORK_ERROR", message: "Serverantwort konnte nicht gelesen werden." })) as { code: string; message: string }; throw new ApiError(body.code, body.message, response.status); }
  return response.json() as Promise<T>;
}

export function apiFormProgress<T>(path: string, body: FormData, onProgress: (loaded: number, total: number) => void): Promise<T> {
  return new Promise((resolve, reject) => {
    const request = new XMLHttpRequest();
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
