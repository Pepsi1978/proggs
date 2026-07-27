import { describe, expect, it } from "vitest";
import { codexEfforts, codexRequestFields, decryptCredentials, encryptCredentials, tokenIdentity } from "./codex-auth.js";

describe("Codex credential protection", () => {
  it("encrypts and decrypts credentials without exposing token text", () => {
    const encrypted = encryptCredentials({ accessToken: "access-secret", refreshToken: "refresh-secret" }, "session-secret-with-at-least-32-characters");
    expect(encrypted).not.toContain("access-secret");
    expect(decryptCredentials(encrypted, "session-secret-with-at-least-32-characters")).toEqual({ accessToken: "access-secret", refreshToken: "refresh-secret" });
  });

  it("rejects modified ciphertext", () => {
    const encrypted = encryptCredentials({ accessToken: "secret" }, "session-secret-with-at-least-32-characters");
    const changedLastCharacter = encrypted.endsWith("A") ? "B" : "A";
    expect(() => decryptCredentials(`${encrypted.slice(0, -1)}${changedLastCharacter}`, "session-secret-with-at-least-32-characters")).toThrow();
  });

  it("reads the ChatGPT account and email claims", () => {
    const jwt = (payload: object) => `e30.${Buffer.from(JSON.stringify(payload)).toString("base64url")}.sig`;
    expect(tokenIdentity(jwt({ "https://api.openai.com/auth": { chatgpt_account_id: "acct_1" } }), jwt({ email: "frank@example.de" }))).toEqual({ accountId: "acct_1", email: "frank@example.de" });
  });

  it("sends every GPT-5.6 effort exactly as selected without a priority tier", () => {
    expect(codexEfforts).toEqual(["none", "low", "medium", "high", "xhigh", "max"]);
    expect(codexRequestFields("gpt-5.6-terra", "none")).toEqual({ model: "gpt-5.6-terra", reasoning: { effort: "none", summary: "auto" }, include: ["reasoning.encrypted_content"] });
    expect(codexRequestFields("gpt-5.6-luna", "max")).not.toHaveProperty("service_tier");
  });
});
