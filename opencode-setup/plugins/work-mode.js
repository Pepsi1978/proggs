import {
  initialWorkMode,
  readWorkMode,
  resolveWorkModeInstruction,
  workModeInstruction,
} from "./token-cost-sidebar/dist/work-mode.ts"

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
    if (instruction) output.system.push(instruction)
  },
})
