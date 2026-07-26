import { mkdirSync, renameSync, rmSync, writeFileSync } from "node:fs"
import { readFile } from "node:fs/promises"
import { homedir } from "node:os"
import { join } from "node:path"

export const WORK_MODES = [
  { id: "frei", label: "Freimodus" },
  { id: "schnell", label: "Schnellmodus" },
  { id: "normal", label: "Normalmodus" },
  { id: "gruendlich", label: "Gründlichkeitsmodus" },
] as const

export type WorkModeId = (typeof WORK_MODES)[number]["id"]

export const DEFAULT_WORK_MODE: WorkModeId = "schnell"
export const WORK_MODE_DIRECTORY = join(homedir(), ".local", "state", "opencode", "work-modes")
export const WORK_MODE_ENVIRONMENT_VARIABLE = "OPENLAUNCHER_WORK_MODE"

function isWorkMode(value: string): value is WorkModeId {
  return WORK_MODES.some((mode) => mode.id === value)
}

export function initialWorkMode(environment: Record<string, string | undefined> = process.env): WorkModeId {
  const value = environment[WORK_MODE_ENVIRONMENT_VARIABLE]?.trim().toLowerCase()
  return value && isWorkMode(value) ? value : DEFAULT_WORK_MODE
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
    if (code === "ENOENT") return initialWorkMode()
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

export function workModeInstruction(mode: WorkModeId): string | undefined {
  if (mode === "frei") return undefined

  const instructions: Record<Exclude<WorkModeId, "frei">, string> = {
    schnell: "Bearbeite nur die ausdrücklich verlangte Änderung und wähle dafür den kleinsten korrekten Eingriff. Prüfe die direkt betroffenen Aufrufer und führe fokussierte Tests für das geänderte Verhalten aus. Vermeide allgemeine Refactorings, zusätzliche Härtung und themenfremde Verbesserungen. Starte kein zusätzliches Quality Gate, außer der Auftrag oder das aktive AGENTS.md-Profil verlangt es.",
    normal: "Löse den Auftrag vollständig mit einem zum Risiko und Umfang passenden Eingriff. Prüfe betroffene Aufrufer, naheliegende Regressionen und relevante Randfälle und führe die passenden Tests oder Builds aus. Kleine, direkt auftragsbezogene Härtungen sind erlaubt; vermeide themenfremde Refactorings. Für durch diesen Modus zusätzlich veranlasste Quality Gates gelten höchstens zwei Durchläufe, sofern der Auftrag oder das aktive AGENTS.md-Profil nicht mehr verlangt.",
    gruendlich: "Untersuche neben der konkreten Änderung auch betroffene Aufrufer, Abhängigkeiten, relevante Randfälle und verwandte Fehlerklassen. Nimm sinnvolle, auftragsnahe Härtungen vor und verifiziere das Ergebnis mit den vollständigen relevanten Tests oder Builds. Wiederhole erforderliche Quality Gates ohne feste Obergrenze, bis alle Befunde behoben und alle Prüfungen grün sind. Melde verbleibende Unsicherheiten ausdrücklich.",
  }
  const label = WORK_MODES.find((item) => item.id === mode)?.label ?? "Schnellmodus"
  return `AKTIVER ARBEITSMODUS: ${label}. Das aktive AGENTS.md-Profil gilt vollständig und unverändert. Diese Laufzeitwahl ergänzt es für diesen Modellaufruf nur um die Arbeitstiefe; bei einem Widerspruch haben die Regeln aus AGENTS.md Vorrang. ${instructions[mode]}`
}
