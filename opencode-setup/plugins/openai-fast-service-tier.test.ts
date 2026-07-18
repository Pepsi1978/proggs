import { describe, expect, test } from "bun:test"
import { OpenAIFastServiceTier } from "./openai-fast-service-tier.js"

describe("OpenAI Fast service tier", () => {
  test("enforces Priority at the final chat.params hook for every Launcher Fast alias", async () => {
    const hooks = await OpenAIFastServiceTier()
    for (const id of ["gpt-5.5-fast", "gpt-5.6-sol-fast", "gpt-5.6-terra-fast", "gpt-5.6-luna-fast"]) {
      const output = { options: {} as Record<string, unknown> }
      await hooks["chat.params"]({ model: { providerID: "openai", id } }, output)
      expect(output.options.serviceTier).toBe("priority")
    }
  })

  test("does not alter normal OpenAI or non-OpenAI models", async () => {
    const hooks = await OpenAIFastServiceTier()
    for (const model of [
      { providerID: "openai", id: "gpt-5.6-sol" },
      { providerID: "openrouter", id: "gpt-5.6-sol-fast" },
    ]) {
      const output = { options: { serviceTier: "default" } as Record<string, unknown> }
      await hooks["chat.params"]({ model }, output)
      expect(output.options.serviceTier).toBe("default")
    }
  })
})
