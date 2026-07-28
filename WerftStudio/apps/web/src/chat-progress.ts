// Ein Lauf am Design dauert je nach Modell eine knappe bis zwei Minuten. Vorher stand in dieser Zeit
// nur ein Spinner — man wusste nicht, ob überhaupt gearbeitet wird. Aus den Ereignissen des Servers
// wird hier ein Satz, den man beim Warten wirklich lesen mag.

// Pfade wie „.werft-generated/019fa36f-…/1/design.html" sagen niemandem etwas. Im Gespräch steht
// deshalb, WAS geändert wurde, nicht wo es technisch liegt.
export function designFileLabel(path: string): string {
  if (path.startsWith(".werft-generated/")) return "Design";
  return path.split("/").at(-1) || path;
}

// Ein Modell ist nur so verlässlich wie der Anbieter dahinter. Genau diese Zahl stand bisher zwar in
// den Daten, aber nirgends auf dem Bildschirm: das gespeicherte MiMo hing an einem Anbieter mit 82 %
// Verfügbarkeit, und der Lauf brach ab, ohne dass man je einen Hinweis darauf gehabt hätte.
export function providerHealth(endpoint?: { providerName: string; uptimeLast5m?: number; status: number }): string | undefined {
  if (!endpoint) return undefined;
  const uptime = typeof endpoint.uptimeLast5m === "number" ? endpoint.uptimeLast5m : undefined;
  if ((uptime === undefined || uptime >= 95) && endpoint.status >= 0) return undefined;
  const teile = [endpoint.providerName];
  if (uptime !== undefined) teile.push(`${Math.round(uptime)} % Verfügbarkeit`);
  if (endpoint.status < 0) teile.push("von OpenRouter herabgestuft");
  return `${teile.join(" · ")} — dort brechen Läufe öfter ab. Bei einem Ausfall wird automatisch auf einen anderen Anbieter ausgewichen.`;
}

const zahl = (value: unknown) => (typeof value === "number" && Number.isFinite(value) ? value : 0);
const mehrzahl = (count: number, eins: string, viele: string) => `${count} ${count === 1 ? eins : viele}`;

export function chatStepText(event: string, data: Record<string, unknown>): string {
  const runde = zahl(data.round);
  const vorspann = runde > 1 ? `Runde ${runde}: ` : "";
  if (event === "start") {
    const umfang = data.marked
      ? "markierter Bereich"
      : data.scope === "global"
        ? "gesamtes Design, alle Bildschirme"
        : typeof data.screen === "string" && data.screen ? `Bildschirm „${data.screen}“` : "gesamtes Design";
    return `Auftrag verstanden — ${umfang}.`;
  }
  if (event === "denkt") {
    const teile = [mehrzahl(zahl(data.files), "Datei", "Dateien")];
    if (zahl(data.screens)) teile.push(mehrzahl(zahl(data.screens), "Bildschirm", "Bildschirme"));
    if (data.excerpt) teile.push("fürs Kontextfenster gekürzt");
    return `${vorspann}Das Modell liest ${teile.join(", ")} und plant die Änderungen …`;
  }
  if (event === "uebernimmt") return `${vorspann}Antwort ist da (${(Math.round(zahl(data.outputChars) / 100) / 10).toLocaleString("de-DE")}k Zeichen) — Änderungen werden übernommen …`;
  if (event === "runde") {
    const teile = [`${mehrzahl(zahl(data.editsApplied), "Änderung", "Änderungen")} übernommen`];
    if (zahl(data.editsFailed)) teile.push(`${zahl(data.editsFailed)} offen`);
    if (data.salvaged) teile.push("aus abgebrochener Antwort gerettet");
    return `Runde ${runde} fertig: ${teile.join(", ")}.`;
  }
  if (event === "nacharbeit") {
    return `Es wird nachgearbeitet: ${data.truncated ? "die Antwort war abgeschnitten, der Rest folgt" : `${mehrzahl(zahl(data.offen), "Stelle wird", "Stellen werden")} nachgebessert`} …`;
  }
  return "Wird bearbeitet …";
}
