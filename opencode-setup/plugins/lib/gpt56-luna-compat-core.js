export function isGpt56LunaModel(model) {
  if (model?.providerID !== "openai") return false
  return [model?.id, model?.api?.id]
    .filter((id) => typeof id === "string")
    .some((id) => id.toLowerCase().startsWith("gpt-5.6-luna"))
}
