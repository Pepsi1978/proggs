// Zwischenstand eines Rekonstruktionslaufs. Bisher lebten Analyseprotokolle, verdichtete Evidenz
// und fertige Bildschirme NUR im Arbeitsspeicher: brach der Lauf bei Schritt 27 von 55 ab, waren
// alle 27 bezahlten KI-Schritte verloren und „erneut starten" begann wieder bei null.
// Jeder teure Schritt wird deshalb einzeln abgelegt und beim naechsten Lauf wiederverwendet.

export type CheckpointScope = { projectId: string; revision: number; selectionHash: string; viewportKey: string };

export type CheckpointParts = {
  analyses: Array<{ number: number; text: string }>;
  evidence?: string;
  screens: Array<{ id: string; markup: string; css: string }>;
};

export const emptyCheckpoint: CheckpointParts = { analyses: [], screens: [] };

// Der Schluessel bindet den Zwischenstand an GENAU die Ausgangslage, aus der er entstanden ist.
// Wurde das Projekt neu importiert (andere Revision), ein anderes Modell gewaehlt oder fuer ein
// anderes Format gebaut, ist der alte Stand wertlos und darf nicht wiederverwendet werden.
export const checkpointRoot = ".werft-checkpoints/";

export function checkpointPrefix(scope: CheckpointScope): string {
  return `${checkpointRoot}${scope.projectId}/${scope.revision}-${scope.selectionHash}-${scope.viewportKey}/`;
}

export function analysisKey(prefix: string, batchNumber: number): string {
  return `${prefix}analysis-${String(batchNumber).padStart(4, "0")}.txt`;
}

export function evidenceKey(prefix: string): string {
  return `${prefix}evidence.txt`;
}

// Bildschirm-IDs kommen aus den Quellen und koennen alles enthalten, was ein Dateiname nicht darf.
// Der Klarname bleibt im Objekt selbst erhalten, der Schluessel wird beruhigt.
export function screenKey(prefix: string, screenId: string): string {
  return `${prefix}screen-${screenId.replace(/[^a-zA-Z0-9._-]/g, "_")}.json`;
}

export function parseCheckpointObject(key: string, prefix: string, body: string): Partial<CheckpointParts> | undefined {
  if (!key.startsWith(prefix)) return undefined;
  const name = key.slice(prefix.length);
  const analysis = /^analysis-(\d+)\.txt$/.exec(name);
  if (analysis) return { analyses: [{ number: Number(analysis[1]), text: body }] };
  if (name === "evidence.txt") return { evidence: body };
  if (/^screen-.+\.json$/.test(name)) {
    try {
      const value = JSON.parse(body) as { id?: unknown; markup?: unknown; css?: unknown };
      // Ein halb geschriebenes oder leeres Fragment ist schlimmer als keins: es wuerde einen
      // Bildschirm als „fertig" ausweisen, der im Design als weisse Flaeche erscheint.
      if (typeof value.id !== "string" || !value.id || typeof value.markup !== "string" || !value.markup.trim()) return undefined;
      return { screens: [{ id: value.id, markup: value.markup, css: typeof value.css === "string" ? value.css : "" }] };
    } catch { return undefined; }
  }
  return undefined;
}

export function mergeCheckpointPart(current: CheckpointParts, part: Partial<CheckpointParts> | undefined): CheckpointParts {
  if (!part) return current;
  const evidence = part.evidence ?? current.evidence;
  return {
    analyses: part.analyses ? [...current.analyses, ...part.analyses].sort((left, right) => left.number - right.number) : current.analyses,
    ...(evidence === undefined ? {} : { evidence }),
    screens: part.screens ? [...current.screens, ...part.screens] : current.screens
  };
}

// Analysepakete werden NUR als lueckenlose Folge ab Paket 1 uebernommen. Fehlt Paket 3, waere die
// Evidenz an dieser Stelle blind; ab dort wird neu gerechnet. Verlustfrei heisst hier: im Zweifel
// lieber noch einmal messen, als ein halbes Design ausliefern.
export function resumableAnalyses(parts: CheckpointParts): string[] {
  const byNumber = new Map(parts.analyses.map((entry) => [entry.number, entry.text] as const));
  const texts: string[] = [];
  for (let number = 1; byNumber.has(number); number += 1) texts.push(byNumber.get(number)!);
  return texts;
}

export function resumableScreens(parts: CheckpointParts, plannedIds: string[]): Map<string, { markup: string; css: string }> {
  const planned = new Set(plannedIds);
  const resumable = new Map<string, { markup: string; css: string }>();
  // Bildschirme sind unabhaengig voneinander: hier ist jeder fertige ein echter Gewinn, auch wenn
  // die uebrigen fehlen.
  for (const screen of parts.screens) if (planned.has(screen.id) && !resumable.has(screen.id)) resumable.set(screen.id, { markup: screen.markup, css: screen.css });
  return resumable;
}

export type CheckpointSummary = { analyses: number; screens: number; hasEvidence: boolean };

export function checkpointSummary(parts: CheckpointParts): CheckpointSummary {
  return { analyses: resumableAnalyses(parts).length, screens: parts.screens.length, hasEvidence: Boolean(parts.evidence?.trim()) };
}

export function checkpointIsUseful(summary: CheckpointSummary): boolean {
  return summary.analyses > 0 || summary.screens > 0 || summary.hasEvidence;
}

export function describeCheckpoint(summary: CheckpointSummary, plannedScreens?: number): string {
  const parts: string[] = [];
  if (summary.analyses) parts.push(`${summary.analyses} Analysepaket${summary.analyses === 1 ? "" : "e"}`);
  if (summary.hasEvidence) parts.push("die verdichtete Evidenz");
  if (summary.screens) parts.push(`${summary.screens}${plannedScreens ? ` von ${plannedScreens}` : ""} fertige${summary.screens === 1 ? "r" : ""} Bildschirm${summary.screens === 1 ? "" : "e"}`);
  if (!parts.length) return "Es liegt kein wiederverwendbarer Zwischenstand vor.";
  return `Übernommen aus dem letzten Lauf: ${parts.join(", ")}.`;
}
