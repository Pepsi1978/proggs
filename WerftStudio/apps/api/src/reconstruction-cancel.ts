// Ein Aufbau aus den Quellen laeuft Minuten und kostet bei jedem Bildschirm einen teuren
// KI-Aufruf. Bisher gab es keinen Weg zurueck: einmal gestartet, lief er bis zum Ende durch —
// auch wenn schon nach dem ersten Bildschirm klar war, dass etwas nicht stimmt. Der Abbruch
// braucht zwei Wege, die zusammenwirken:
//
//   1. Den **Wunsch in der Datenbank** (`cancel_requested`). Er ist die Wahrheit und wirkt auch
//      dann, wenn der Lauf in einem anderen Prozess haengt oder der Server zwischendurch neu
//      gestartet wurde. Der Lauf schreibt ohnehin alle zwei Sekunden seinen Fortschritt — genau
//      dort faellt ihm der Wunsch auf.
//   2. Den **Abbruch im laufenden Prozess**. Er wirkt sofort statt erst beim naechsten
//      Fortschrittsschritt und beendet den offenen Modellaufruf, statt ihn noch zu Ende bezahlen
//      zu lassen.
//
// Ohne (1) wuerde ein Abbruch verpuffen, sobald der Lauf woanders laeuft; ohne (2) liefe der
// teure Aufruf noch bis zu zwei Sekunden — und die Antwort komplett — weiter.

export const cancelledCode = "RECONSTRUCT_CANCELLED";

/** Die im Prozess laufenden Aufbauten, damit ein Abbruch sie sofort erreicht. */
const laufende = new Map<string, AbortController>();

export function registerReconstruction(jobId: string): AbortController {
  // Ein zweiter Lauf zur selben Kennung ersetzt den ersten — der alte hat ohnehin seine
  // Berechtigung verloren (Lease) und soll nicht weiterlaufen.
  laufende.get(jobId)?.abort(cancelReason());
  const controller = new AbortController();
  laufende.set(jobId, controller);
  return controller;
}

export function releaseReconstruction(jobId: string, controller: AbortController): void {
  // Nur den EIGENEN Eintrag entfernen: hat inzwischen ein neuer Lauf uebernommen, gehoert der
  // Eintrag ihm, und ein spaetes Aufraeumen des alten Laufs wuerde seinen Abbruchweg kappen.
  if (laufende.get(jobId) === controller) laufende.delete(jobId);
}

export function cancelReason(): Error & { code: string } {
  return Object.assign(new Error("Der Aufbau wurde abgebrochen."), { code: cancelledCode });
}

/** Bricht einen im Prozess laufenden Aufbau sofort ab. Antwort: lief hier einer? */
export function abortReconstruction(jobId: string): boolean {
  const controller = laufende.get(jobId);
  if (!controller || controller.signal.aborted) return false;
  controller.abort(cancelReason());
  return true;
}

export const isCancelled = (code: unknown): boolean => code === cancelledCode;

/** Abbruch und verlorene Berechtigung enden beide sofort — nur die Meldung unterscheidet sich. */
export const endsRun = (code: unknown): boolean => code === cancelledCode || code === "RECONSTRUCT_LEASE_LOST";

/**
 * Was der Benutzer nach einem Abbruch sieht. Ein Abbruch ist KEIN Fehler: er war gewollt, und was
 * bis dahin gesichert wurde, bleibt liegen und ist der Ausgangspunkt fuer den naechsten Lauf.
 */
export function cancelMessage(checkpointHint: string): string {
  return `Der Aufbau wurde abgebrochen.${checkpointHint ? ` ${checkpointHint}` : ""}`;
}
