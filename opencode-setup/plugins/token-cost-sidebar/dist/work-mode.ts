import { mkdirSync, renameSync, rmSync, writeFileSync } from "node:fs"
import { readFile } from "node:fs/promises"
import { homedir } from "node:os"
import { join } from "node:path"

export const WORK_MODES = [
  { id: "schnell", label: "Schnellmodus" },
  { id: "normal", label: "Normalmodus" },
  { id: "gruendlich", label: "Gründlichkeitsmodus" },
] as const

export type WorkModeId = (typeof WORK_MODES)[number]["id"]

export const DEFAULT_WORK_MODE: WorkModeId = "schnell"
export const WORK_MODE_DIRECTORY = join(homedir(), ".local", "state", "opencode", "work-modes")

function isWorkMode(value: string): value is WorkModeId {
  return WORK_MODES.some((mode) => mode.id === value)
}

function stateFile(sessionID: string, directory = WORK_MODE_DIRECTORY): string {
  const safeSessionID = sessionID.replace(/[^a-zA-Z0-9_-]/g, "_")
  return join(directory, `${safeSessionID}.txt`)
}

export async function readWorkMode(sessionID: string, directory = WORK_MODE_DIRECTORY): Promise<WorkModeId> {
  try {
    const value = (await readFile(stateFile(sessionID, directory), "utf8")).trim()
    return isWorkMode(value) ? value : DEFAULT_WORK_MODE
  } catch (error) {
    const code = typeof error === "object" && error !== null && "code" in error ? error.code : undefined
    if (code === "ENOENT") return DEFAULT_WORK_MODE
    throw error
  }
}

export function writeWorkMode(
  sessionID: string,
  mode: WorkModeId,
  directory = WORK_MODE_DIRECTORY,
): void {
  mkdirSync(directory, { recursive: true })
  const target = stateFile(sessionID, directory)
  const temporary = `${target}.${process.pid}.${Date.now()}.tmp`
  writeFileSync(temporary, mode, "utf8")
  try {
    renameSync(temporary, target)
  } finally {
    rmSync(temporary, { force: true })
  }
}

export function workModeInstruction(mode: WorkModeId): string {
  const instructions: Record<WorkModeId, string> = {
    schnell: "Kleinster korrekter Fix, fokussierte Tests, keine allgemeine Härtung und kein Quality Gate.",
    normal: "Proportionaler Fix, relevante Regressionstests und höchstens zwei auftragsbezogene Quality-Gate-Durchläufe.",
    gruendlich: "Verwandte Fehlerklassen und sinnvolle Härtung mitprüfen; Quality Gates ohne feste Obergrenze wiederholen, bis alles grün ist.",
  }
  const label = WORK_MODES.find((item) => item.id === mode)?.label ?? "Schnellmodus"
  return `AKTIVER ARBEITSMODUS: ${label}. Diese Laufzeitwahl aus der Sidebar überschreibt den Standardmodus aus AGENTS.md für diesen Modellaufruf. ${instructions[mode]}`
}
