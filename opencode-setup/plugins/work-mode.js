import {
  initialWorkMode,
  readWorkMode,
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
    const instruction = workModeInstruction(mode)
    if (instruction) output.system.push(instruction)
  },
})
