const CODEX_USER_AGENT = "codex_cli_rs/0.0.0 (OpenCode)"

export function isGpt56LunaModel(model) {
	if (model?.providerID !== "openai") return false
	return [model?.id, model?.api?.id]
		.filter((id) => typeof id === "string")
		.some((id) => id.toLowerCase().startsWith("gpt-5.6-luna"))
}

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
