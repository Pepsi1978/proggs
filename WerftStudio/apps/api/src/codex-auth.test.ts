import { describe, expect, it } from "vitest";
import { codexModelFields, decryptCredentials, encryptCredentials, tokenIdentity } from "./codex-auth.js";

describe("Codex credential protection", () => {
  it("encrypts and decrypts credentials without exposing token text", () => {
    const encrypted = encryptCredentials({ accessToken: "access-secret", refreshToken: "refresh-secret" }, "session-secret-with-at-least-32-characters");
    expect(encrypted).not.toContain("access-secret");
    expect(decryptCredentials(encrypted, "session-secret-with-at-least-32-characters")).toEqual({ accessToken: "access-secret", refreshToken: "refresh-secret" });
  });

  it("rejects modified ciphertext", () => {
    const encrypted = encryptCredentials({ accessToken: "secret" }, "session-secret-with-at-least-32-characters");
    expect(() => decryptCredentials(`${encrypted.slice(0, -1)}A`, "session-secret-with-at-least-32-characters")).toThrow();
  });

  it("reads the ChatGPT account and email claims", () => {
    const jwt = (payload: object) => `e30.${Buffer.from(JSON.stringify(payload)).toString("base64url")}.sig`;
    expect(tokenIdentity(jwt({ "https://api.openai.com/auth": { chatgpt_account_id: "acct_1" } }), jwt({ email: "frank@example.de" }))).toEqual({ accountId: "acct_1", email: "frank@example.de" });
  });

  it("maps Fast to the priority service tier without changing the model", () => {
    expect(codexModelFields("gpt-5.6-terra", true)).toEqual({ model: "gpt-5.6-terra", service_tier: "priority" });
    expect(codexModelFields("gpt-5.6-terra", false)).toEqual({ model: "gpt-5.6-terra" });
  });
});
