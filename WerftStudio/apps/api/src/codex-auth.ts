import { createCipheriv, createDecipheriv, createHash, randomBytes } from "node:crypto";

export const codexAuth = {
  clientId: "app_EMoamEEZ73f0CkXaXp7hrann",
  userCodeUrl: "https://auth.openai.com/api/accounts/deviceauth/usercode",
  deviceTokenUrl: "https://auth.openai.com/api/accounts/deviceauth/token",
  verificationUrl: "https://auth.openai.com/codex/device",
  redirectUrl: "https://auth.openai.com/deviceauth/callback",
  tokenUrl: "https://auth.openai.com/oauth/token",
  responsesUrl: "https://chatgpt.com/backend-api/codex/responses"
} as const;

export const codexModels = ["gpt-5.6-sol", "gpt-5.6-terra", "gpt-5.6-luna"] as const;
export const codexEfforts = ["none", "low", "medium", "high", "xhigh", "max"] as const;

export function codexRequestFields(model: typeof codexModels[number], effort: typeof codexEfforts[number]) {
  return { model, reasoning: { effort, summary: "auto" as const }, include: ["reasoning.encrypted_content"] };
}

export type CodexCredentials = { accessToken: string; refreshToken?: string; idToken?: string };

const keyFromSecret = (secret: string) => createHash("sha256").update(`werft:provider-credentials:${secret}`).digest();

export function encryptCredentials(credentials: object, secret: string): string {
  const iv = randomBytes(12);
  const cipher = createCipheriv("aes-256-gcm", keyFromSecret(secret), iv);
  const encrypted = Buffer.concat([cipher.update(JSON.stringify(credentials), "utf8"), cipher.final()]);
  return ["v1", iv.toString("base64url"), cipher.getAuthTag().toString("base64url"), encrypted.toString("base64url")].join(".");
}

export function decryptCredentials<T = CodexCredentials>(value: string, secret: string): T {
  const [version, iv, tag, encrypted] = value.split(".");
  if (version !== "v1" || !iv || !tag || !encrypted) throw new Error("Ungültiges Credential-Format");
  const decipher = createDecipheriv("aes-256-gcm", keyFromSecret(secret), Buffer.from(iv, "base64url"));
  decipher.setAuthTag(Buffer.from(tag, "base64url"));
  return JSON.parse(Buffer.concat([decipher.update(Buffer.from(encrypted, "base64url")), decipher.final()]).toString("utf8")) as T;
}

export function jwtPayload(token?: string): Record<string, unknown> | undefined {
  if (!token) return undefined;
  try { return JSON.parse(Buffer.from(token.split(".")[1]!, "base64url").toString("utf8")) as Record<string, unknown>; }
  catch { return undefined; }
}

export function tokenIdentity(accessToken: string, idToken?: string): { accountId?: string; email?: string } {
  const access = jwtPayload(accessToken), identity = jwtPayload(idToken);
  const auth = access?.["https://api.openai.com/auth"] as Record<string, unknown> | undefined;
  return {
    ...(typeof auth?.chatgpt_account_id === "string" ? { accountId: auth.chatgpt_account_id } : {}),
    ...(typeof identity?.email === "string" ? { email: identity.email } : typeof access?.email === "string" ? { email: access.email } : {})
  };
}
