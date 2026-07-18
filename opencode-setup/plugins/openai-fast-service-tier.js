// OpenAI Fast Service Tier v1.0.0 - 18.07.2026, 20:48 Uhr

const FAST_ALIASES = new Set([
  "gpt-5.5-fast",
  "gpt-5.6-sol-fast",
  "gpt-5.6-terra-fast",
  "gpt-5.6-luna-fast",
])

function modelID(model) {
  return model?.modelID ?? model?.id ?? ""
}

export const OpenAIFastServiceTier = async () => ({
  "chat.params": async (input, output) => {
    if (input.model?.providerID !== "openai" || !FAST_ALIASES.has(modelID(input.model))) return
    output.options.serviceTier = "priority"
  },
})
