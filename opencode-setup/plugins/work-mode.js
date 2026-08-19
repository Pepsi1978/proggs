import {
  initialWorkMode,
  readWorkMode,
  resolveWorkModeInstruction,
  workModeInstruction,
} from "./token-cost-sidebar/dist/work-mode.ts"

/**
 * Modelle wie Qwen3.8 lehnen ueber ihr Jinja-Chat-Template jede System-Nachricht ab, die nicht die
 * erste ist ("System message must be at the beginning"). Bei lokalen LM-Studio-Modellen wird der
 * Modus-Text deshalb in den vorhandenen System-Block eingefuegt statt als zweiter angehaengt.
 * Cloud-Modelle behalten getrennte Bloecke: dort haelt der stabile erste Block den Prompt-Cache.
 */
function istLokalesModell(input) {
  const kandidaten = [
    input?.providerID,
    input?.provider?.id,
    input?.model?.providerID,
    input?.model?.provider,
    process.env.OPENLAUNCHER_MODEL,
  ]
  return kandidaten.some((wert) => typeof wert === "string" && wert.toLowerCase().startsWith("lmstudio"))
}

function applySystemInstruction(input, output, instruction) {
  if (!instruction) return
  if (!istLokalesModell(input)) {
    output.system.push(instruction)
    return
  }
  const zusammengefasst = [...output.system, instruction].filter(Boolean).join("\n\n")
  output.system.length = 0
  output.system.push(zusammengefasst)
}

export const WorkModePlugin = async ({ client }) => ({
  "experimental.chat.system.transform": async (input, output) => {
    let mode = initialWorkMode()
    try {
      if (input.sessionID) mode = await readWorkMode(input.sessionID)
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error)
      await client.app.log({
        body: {
          service: "work-mode",
          level: "error",
          message: `Arbeitsmodus konnte nicht gelesen werden; der gewählte Startmodus wird verwendet: ${message}`,
        },
      }).catch(() => undefined)
    }
    // Der Prompt kommt aus der im OpenLauncher bearbeitbaren Datei (Profiles/WorkModes/<id>.md) und
    // wird bei JEDEM Modellaufruf frisch gelesen -- Änderungen und Moduswechsel in der TUI wirken
    // damit sofort. Nur wenn die Datei nicht lesbar ist, greift der eingebaute Text.
    let instruction
    try {
      instruction = await resolveWorkModeInstruction(mode)
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error)
      instruction = workModeInstruction(mode)
      await client.app.log({
        body: {
          service: "work-mode",
          level: "error",
          message: `Modus-Prompt konnte nicht gelesen werden; der eingebaute Text wird verwendet: ${message}`,
        },
      }).catch(() => undefined)
    }
    if (instruction) applySystemInstruction(input, output, instruction)
  },
})
