import { describe, expect, test } from "bun:test"
import { Gpt56LunaCompatibility } from "./gpt56-luna-compat.js"
import { isGpt56LunaModel } from "./lib/gpt56-luna-compat-core.js"

describe("GPT-5.6 Luna compatibility headers", () => {
	test("recognizes Luna model and API aliases only for OpenAI", () => {
		expect(isGpt56LunaModel({ providerID: "openai", id: "gpt-5.6-luna-fast" })).toBe(true)
		expect(isGpt56LunaModel({ providerID: "openai", id: "alias", api: { id: "gpt-5.6-luna" } })).toBe(true)
		expect(isGpt56LunaModel({ providerID: "openai", id: "gpt-5.6-terra-fast" })).toBe(false)
		expect(isGpt56LunaModel({ providerID: "openrouter", id: "gpt-5.6-luna-fast" })).toBe(false)
	})

	test("replaces client headers for Luna without changing other headers", async () => {
		const hooks = await Gpt56LunaCompatibility()
		const output = { headers: { "user-agent": "opencode/1.17.18", Originator: "opencode", Accept: "application/json" } }

		await hooks["chat.headers"]({ model: { providerID: "openai", id: "gpt-5.6-luna-fast" } }, output)

		expect(output.headers).toEqual({
			Accept: "application/json",
			"User-Agent": "codex_cli_rs/0.0.0 (OpenCode)",
			originator: "codex_cli_rs",
		})
	})
})
