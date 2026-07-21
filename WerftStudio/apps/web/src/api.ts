export class ApiError extends Error { constructor(readonly code: string, message: string, readonly status: number) { super(message); } }
export async function api<T>(path: string, init?: RequestInit): Promise<T> {
  // content-type nur setzen wenn wirklich ein Body mitgeht: Fastify lehnt "application/json ohne Body"
  // sonst mit FST_ERR_CTP_EMPTY_JSON_BODY ab (traf alle DELETE-Aufrufe).
  const response = await fetch(`/api/v1${path}`, { ...init, credentials: "include", headers: { ...(init?.body != null ? { "content-type": "application/json" } : {}), ...init?.headers } });
  if (!response.ok) { const body = await response.json().catch(() => ({ code: "NETWORK_ERROR", message: "Serverantwort konnte nicht gelesen werden." })) as { code: string; message: string }; throw new ApiError(body.code, body.message, response.status); }
  return response.json() as Promise<T>;
}

export async function apiForm<T>(path: string, body: FormData): Promise<T> {
  const response = await fetch(`/api/v1${path}`, { method: "POST", body, credentials: "include" });
  if (!response.ok) { const error = await response.json().catch(() => ({ code: "NETWORK_ERROR", message: "Serverantwort konnte nicht gelesen werden." })) as { code: string; message: string }; throw new ApiError(error.code, error.message, response.status); }
  return response.json() as Promise<T>;
}
