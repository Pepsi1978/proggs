// Ein markierter Bereich mit einer Anweisung ist ein Arbeitsauftrag. Bisher lief immer nur EINER:
// das Absenden war gesperrt, solange die KI arbeitete, und ein neuer Auftrag haette den laufenden
// abgebrochen. Wer fuenf Stellen im Design verbessern wollte, musste fuenfmal warten. Jetzt wandern
// die Auftraege in eine Warteschlange: absetzen geht IMMER sofort, mehrere arbeiten gleichzeitig,
// und das Einarbeiten ins Design reiht sich von selbst hintereinander ein — der Server haelt dafuer
// beim Schreiben eine Sperre auf dem Projekt und liest den Stand darin frisch, so dass ein spaeter
// fertiger Auftrag auf dem Ergebnis des frueheren aufsetzt statt es zu ueberschreiben.

export type AuftragStatus = "wartet" | "läuft" | "angewendet" | "ohne Änderung" | "fehlgeschlagen" | "abgebrochen";

/** Wie viele Auftraege hoechstens gleichzeitig bei der KI liegen. */
export const parallelGrenze = 5;

/** Ein Auftrag ist fertig, wenn er weder wartet noch laeuft. */
export const istOffen = (status: AuftragStatus): boolean => status === "wartet" || status === "läuft";

export const laufende = <T extends { status: AuftragStatus }>(auftraege: readonly T[]): number =>
  auftraege.filter((auftrag) => auftrag.status === "läuft").length;

export const wartende = <T extends { status: AuftragStatus }>(auftraege: readonly T[]): number =>
  auftraege.filter((auftrag) => auftrag.status === "wartet").length;

/**
 * Welche wartenden Auftraege jetzt starten duerfen — in der Reihenfolge, in der sie abgesetzt
 * wurden. Nie mehr als die Grenze insgesamt, damit weder der Anbieter noch die Anzeige ueberlaeuft.
 */
export function startbereit<T extends { status: AuftragStatus }>(auftraege: readonly T[], grenze: number = parallelGrenze): T[] {
  const frei = Math.max(0, grenze - laufende(auftraege));
  if (!frei) return [];
  return auftraege.filter((auftrag) => auftrag.status === "wartet").slice(0, frei);
}

/** Kurzfassung fuer die Anzeige: „2 laufen · 3 warten". */
export function warteschlangenText<T extends { status: AuftragStatus }>(auftraege: readonly T[]): string {
  const teile: string[] = [];
  const aktiv = laufende(auftraege);
  const offen = wartende(auftraege);
  if (aktiv) teile.push(`${aktiv} ${aktiv === 1 ? "läuft" : "laufen"}`);
  if (offen) teile.push(`${offen} ${offen === 1 ? "wartet" : "warten"}`);
  return teile.join(" · ");
}
