export class ApiError extends Error { constructor(readonly code: string, message: string, readonly status: number) { super(message); } }
export async function api<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`/api/v1${path}`, { ...init, credentials: "include", headers: { "content-type": "application/json", ...init?.headers } });
  if (!response.ok) { const body = await response.json().catch(() => ({ code: "NETWORK_ERROR", message: "Serverantwort konnte nicht gelesen werden." })) as { code: string; message: string }; throw new ApiError(body.code, body.message, response.status); }
  return response.json() as Promise<T>;
}
