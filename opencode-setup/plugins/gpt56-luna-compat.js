// GPT-5.6 Luna Compatibility v1.1.0 - 18.07.2026, 12:54 Uhr

import { isGpt56LunaModel } from "./lib/gpt56-luna-compat-core.js"

const CODEX_USER_AGENT = "codex_cli_rs/0.0.0 (OpenCode)"

export const Gpt56LunaCompatibility = async () => ({
	"chat.headers": async (input, output) => {
		if (!isGpt56LunaModel(input.model)) return

		for (const key of Object.keys(output.headers)) {
			if (["user-agent", "originator"].includes(key.toLowerCase())) delete output.headers[key]
		}
		output.headers["User-Agent"] = CODEX_USER_AGENT
		output.headers.originator = "codex_cli_rs"
	},
})
