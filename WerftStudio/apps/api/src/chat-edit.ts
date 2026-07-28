// Die Übernahme einer KI-Antwort in echte Projektdateien war bisher ein Alles-oder-nichts-Vorgang:
// ein einziges fehlendes Zeichen im JSON, ein abgeschnittener Ausgabestrom oder ein Leerzeichen zu
// viel in einer gesuchten Textstelle — und der komplette Lauf verpuffte ohne eine einzige Änderung.
// Dieser Baustein macht daraus einen belastbaren Vorgang:
//   1. Antworten werden auch dann ausgewertet, wenn sie mitten im JSON abbrechen (Rettung).
//   2. Textstellen werden in vier Stufen gesucht, von zeichengenau bis leerraumtolerant.
//   3. Jede geschriebene Datei wird vorher auf Unversehrtheit geprüft (kein Bildschirm darf verloren gehen).
//   4. Was nicht geklappt hat, wird als konkrete Rückmeldung für den nächsten Lauf formuliert.

export type ChatEdit = { find: string; replace: string; all?: boolean };
export type ChatChange = { path: string; edits: ChatEdit[] };
export type ChatFileWrite = { path: string; content: string };
export type ParsedChatResponse = {
  reply: string;
  /** Konkrete nächste Design-Schritte, die die Oberfläche als anklickbare Vorschläge anbietet. */
  naechste: string[];
  changes: ChatChange[];
  files: ChatFileWrite[];
  /** Der Ausgabestrom endete mitten im JSON — das Modell wurde von seiner Ausgabegrenze gestoppt. */
  truncated: boolean;
  /** Das JSON war unbrauchbar; die Edits stammen aus der Einzelrettung vollständiger Teilobjekte. */
  salvaged: boolean;
};

const isText = (value: unknown): value is string => typeof value === "string";
const record = (value: unknown): Record<string, unknown> | undefined =>
  value && typeof value === "object" && !Array.isArray(value) ? value as Record<string, unknown> : undefined;

// Ein zeichengenauer Lauf über die Antwort: er kennt Zeichenketten und Maskierungen und liefert
// deshalb JEDES vollständig geschlossene Objekt — auch aus einer Antwort, die danach abbricht.
// `open` zählt die Klammern, die nie geschlossen wurden: genau daran erkennt man den Abbruch.
export function balancedObjects(text: string): { spans: Array<{ start: number; end: number }>; open: number } {
  const spans: Array<{ start: number; end: number }> = [];
  const stack: number[] = [];
  let inString = false, escaped = false;
  for (let index = 0; index < text.length; index += 1) {
    const char = text[index]!;
    if (inString) {
      if (escaped) escaped = false;
      else if (char === "\\") escaped = true;
      else if (char === '"') inString = false;
      continue;
    }
    if (char === '"') inString = true;
    else if (char === "{") stack.push(index);
    else if (char === "}") { const start = stack.pop(); if (start !== undefined) spans.push({ start, end: index + 1 }); }
  }
  // Aussen zuerst: so wird ein `{path, edits}`-Block als Ganzes übernommen und seine inneren
  // Edit-Objekte nicht ein zweites Mal einzeln gezählt.
  spans.sort((left, right) => left.start - right.start || right.end - left.end);
  return { spans, open: stack.length };
}

// Modelle liefern das JSON mal nackt, mal in einem Codeblock, mal mit Vorrede. Ein Antworttext, der
// selbst mit `{` beginnt, wird nie angefasst — sonst zerschnitte man ein gültiges Dokument.
export function unwrapResponse(raw: string): string {
  const text = raw.trim();
  if (text.startsWith("{")) return text;
  const blocks = [...text.matchAll(/```(?:json|jsonc)?\s*([\s\S]*?)(?:```|$)/gi)]
    .map((match) => match[1]!.trim())
    .filter((block) => block.includes("{"));
  const best = blocks.sort((left, right) => right.length - left.length)[0];
  return best ?? text;
}

const validEdits = (value: unknown): ChatEdit[] => (Array.isArray(value) ? value : []).flatMap((entry) => {
  const edit = record(entry);
  if (!edit || !isText(edit.find) || !edit.find.length || !isText(edit.replace)) return [];
  return [{ find: edit.find, replace: edit.replace, ...(edit.all === true ? { all: true } : {}) }];
});

const maxFileWriteChars = 20 * 1024 * 1024;

const validNext = (value: unknown): string[] => (Array.isArray(value) ? value : [])
  .filter((entry): entry is string => typeof entry === "string" && entry.trim().length > 3)
  .map((entry) => entry.trim().slice(0, 160)).slice(0, 4);

function normalizeResponse(value: Record<string, unknown>): { reply: string; naechste: string[]; changes: ChatChange[]; files: ChatFileWrite[] } {
  const changes = (Array.isArray(value.changes) ? value.changes : []).flatMap((entry): ChatChange[] => {
    const change = record(entry);
    if (!change || !isText(change.path)) return [];
    const edits = validEdits(change.edits);
    return edits.length ? [{ path: change.path, edits }] : [];
  });
  const files = (Array.isArray(value.files) ? value.files : []).flatMap((entry): ChatFileWrite[] => {
    const file = record(entry);
    return file && isText(file.path) && isText(file.content) && file.content.length <= maxFileWriteChars ? [{ path: file.path, content: file.content }] : [];
  });
  return { reply: isText(value.reply) ? value.reply.trim() : "", naechste: validNext(value.naechste), changes, files };
}

const tryParse = (text: string): Record<string, unknown> | undefined => {
  try { return record(JSON.parse(text)); }
  catch { /* Nachsichtiger Zweitversuch: nachgestellte Kommas sind der häufigste Modellfehler. */ }
  try { return record(JSON.parse(text.replace(/,\s*([}\]])/g, "$1"))); }
  catch { return undefined; }
};

const stringField = (text: string, key: string): string | undefined => {
  const match = new RegExp(`"${key}"\\s*:\\s*"((?:[^"\\\\]|\\\\.)*)"`).exec(text);
  if (!match) return undefined;
  try { return JSON.parse(`"${match[1]!}"`) as string; }
  catch { return undefined; }
};

// Die Rettung: aus einer abgebrochenen oder mit Fließtext vermischten Antwort wird jedes VOLLSTÄNDIG
// geschlossene Teilobjekt einzeln geborgen. Genau hier lagen bisher Dutzende fertiger Edits, die
// wegen des fehlenden Schlusszeichens allesamt verworfen wurden.
function salvageResponse(text: string, scan: ReturnType<typeof balancedObjects>): ParsedChatResponse {
  const paths = [...text.matchAll(/"path"\s*:\s*"((?:[^"\\]|\\.)*)"/g)]
    .flatMap((match) => { try { return [{ index: match.index, path: JSON.parse(`"${match[1]!}"`) as string }]; } catch { return []; } });
  const pathAt = (index: number) => [...paths].reverse().find((entry) => entry.index < index)?.path;
  const changes = new Map<string, ChatEdit[]>();
  const files: ChatFileWrite[] = [];
  const addEdits = (path: string, edits: ChatEdit[]) => { if (edits.length) changes.set(path, [...(changes.get(path) ?? []), ...edits]); };
  let consumedUntil = -1;
  for (const span of scan.spans) {
    if (span.start < consumedUntil) continue;
    const value = tryParse(text.slice(span.start, span.end));
    if (!value) continue;
    if (isText(value.path) && Array.isArray(value.edits)) { addEdits(value.path, validEdits(value.edits)); consumedUntil = span.end; continue; }
    if (isText(value.path) && isText(value.content) && value.content.length <= maxFileWriteChars) { files.push({ path: value.path, content: value.content }); consumedUntil = span.end; continue; }
    if (isText(value.find) && value.find.length && isText(value.replace)) {
      const path = pathAt(span.start);
      if (path) addEdits(path, [{ find: value.find, replace: value.replace, ...(value.all === true ? { all: true } : {}) }]);
      consumedUntil = span.end;
    }
  }
  return {
    reply: stringField(text, "reply")?.trim() ?? "",
    naechste: [],
    changes: [...changes].map(([path, edits]) => ({ path, edits })),
    files,
    truncated: scan.open > 0,
    salvaged: true
  };
}

export function parseChatResponse(raw: string): ParsedChatResponse {
  const text = unwrapResponse(raw);
  const scan = balancedObjects(text);
  const first = text.indexOf("{");
  if (first >= 0) {
    const whole = scan.spans.find((span) => span.start === first);
    const value = whole ? tryParse(text.slice(whole.start, whole.end)) : undefined;
    // Ein vollständiges Objekt zählt nur, wenn wirklich etwas darin steht — sonst ist die Rettung
    // (die auch Teilobjekte hinter dem ersten Block findet) die bessere Auswertung.
    if (value && (isText(value.reply) || Array.isArray(value.changes) || Array.isArray(value.files))) {
      return { ...normalizeResponse(value), truncated: scan.open > 0, salvaged: false };
    }
  }
  return salvageResponse(text, scan);
}

export type EditStrategy = "zeichengenau" | "zeilenenden" | "leerraum" | "zeilenweise";
export type EditOutcome = {
  find: string;
  status: "applied" | "not-found" | "ambiguous" | "unchanged";
  strategy?: EditStrategy;
  occurrences: number;
  replaced: number;
};

const escapeRegExp = (text: string) => text.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");

// Vier Stufen, von streng nach nachsichtig. Ein Modell gibt Einrückungen, Zeilenenden und
// Leerzeilen selten zeichengenau wieder — das war der häufigste Grund für „Textstelle nicht gefunden".
function matchers(find: string): Array<{ strategy: EditStrategy; pattern: RegExp; strict: boolean }> {
  const escaped = escapeRegExp(find);
  const lines = find.split(/\r?\n/).map((line) => line.trim()).filter(Boolean);
  const byLine = lines.map((line) => escapeRegExp(line).replace(/[ \t]+/g, "[ \\t]+")).join("\\s*");
  return [
    { strategy: "zeichengenau", pattern: new RegExp(escaped, "g"), strict: false },
    { strategy: "zeilenenden", pattern: new RegExp(escaped.replace(/\r?\n/g, "\\r?\\n"), "g"), strict: false },
    { strategy: "leerraum", pattern: new RegExp(escaped.replace(/\s+/g, "\\s+"), "g"), strict: true },
    ...(lines.length ? [{ strategy: "zeilenweise" as const, pattern: new RegExp(byLine, "g"), strict: true }] : [])
  ];
}

// Ein nachsichtiger Treffer darf nie raten: findet er die Stelle mehrfach, wird NICHT geändert.
// Ein zeichengenauer Treffer ist dagegen eindeutig genug, um die erste Fundstelle zu nehmen.
export function applyChatEdits(content: string, edits: ChatEdit[]): { content: string; outcomes: EditOutcome[] } {
  let current = content;
  const outcomes: EditOutcome[] = [];
  for (const edit of edits) {
    if (edit.find === edit.replace) { outcomes.push({ find: edit.find, status: "unchanged", occurrences: 0, replaced: 0 }); continue; }
    let done = false;
    for (const { strategy, pattern, strict } of matchers(edit.find)) {
      pattern.lastIndex = 0;
      const occurrences = [...current.matchAll(pattern)].length;
      if (!occurrences) continue;
      if (strict && occurrences > 1 && !edit.all) { outcomes.push({ find: edit.find, status: "ambiguous", strategy, occurrences, replaced: 0 }); done = true; break; }
      let replaced = 0;
      pattern.lastIndex = 0;
      // Die Ersetzung läuft über eine FUNKTION: `$&`, `$1` und `$'` im neuen Text bleiben dadurch
      // wörtlich stehen, statt vom Ersetzer als Rückverweis ausgewertet zu werden.
      const next = current.replace(pattern, (match) => {
        if (replaced > 0 && !edit.all) return match;
        replaced += 1;
        return edit.replace;
      });
      current = next;
      outcomes.push({ find: edit.find, status: replaced ? "applied" : "unchanged", strategy, occurrences, replaced });
      done = true;
      break;
    }
    if (!done) outcomes.push({ find: edit.find, status: "not-found", occurrences: 0, replaced: 0 });
  }
  return { content: current, outcomes };
}

const countMatches = (text: string, pattern: RegExp) => [...text.matchAll(pattern)].length;

// Ein Fix darf niemals Funktionalität entfernen. Vor JEDEM Schreiben wird deshalb geprüft, ob die
// Datei strukturell heil bleibt — sonst wird sie gar nicht erst geschrieben.
export function verifyFileWrite(path: string, before: string, after: string): string[] {
  const issues: string[] = [];
  if (!after.trim()) return ["Die Datei wäre danach leer."];
  if (before.length > 2_000 && after.length < before.length * 0.6) {
    issues.push(`Die Datei würde von ${before.length} auf ${after.length} Zeichen schrumpfen — mehr als ein Drittel des Inhalts ginge verloren.`);
  }
  if (/\.html?$/i.test(path)) {
    const screenPattern = /<section\b[^>]*\bclass="[^"]*\bwerft-screen\b/gi;
    const screensBefore = countMatches(before, screenPattern), screensAfter = countMatches(after, screenPattern);
    if (screensAfter < screensBefore) issues.push(`${screensBefore - screensAfter} von ${screensBefore} Bildschirm(en) würden aus dem Dokument verschwinden.`);
    for (const tag of ["style", "script", "section", "div"] as const) {
      const opened = countMatches(after, new RegExp(`<${tag}\\b`, "gi")), closed = countMatches(after, new RegExp(`</${tag}\\s*>`, "gi"));
      const openedBefore = countMatches(before, new RegExp(`<${tag}\\b`, "gi")), closedBefore = countMatches(before, new RegExp(`</${tag}\\s*>`, "gi"));
      // Verglichen wird gegen den VORHER-Stand: ein Dokument, das schon vorher unsauber war, soll
      // nicht plötzlich jede Änderung blockieren — nur eine NEUE Unwucht zählt.
      if (opened - closed !== openedBefore - closedBefore) issues.push(`Die <${tag}>-Elemente sind nach der Änderung nicht mehr paarweise geschlossen.`);
    }
  }
  if (/\.json$/i.test(path)) {
    try { JSON.parse(after); }
    catch { issues.push("Die Datei wäre danach kein gültiges JSON mehr."); }
  }
  return issues;
}

export type FileReport = { path: string; outcomes: EditOutcome[]; written: boolean; issues: string[] };

// Was nicht geklappt hat, geht als PRÄZISE Rückmeldung in den nächsten Lauf. Ein Modell, das erfährt,
// welche Textstelle es falsch zitiert hat, trifft sie im zweiten Anlauf fast immer.
export function repairBriefing(reports: FileReport[], truncated: boolean): string | undefined {
  const lines: string[] = [];
  if (truncated) lines.push("Deine letzte Antwort wurde von der Ausgabegrenze abgeschnitten. Die bis dahin vollständigen Edits sind bereits angewendet. Liefere jetzt NUR die noch fehlenden Änderungen, und zwar in kleineren Häppchen (höchstens 8 Edits pro Antwort).");
  for (const report of reports) {
    for (const issue of report.issues) lines.push(`${report.path}: NICHT geschrieben — ${issue} Formuliere die Änderung so, dass die Struktur erhalten bleibt.`);
    for (const outcome of report.outcomes) {
      const snippet = outcome.find.replace(/\s+/g, " ").slice(0, 120);
      if (outcome.status === "not-found") lines.push(`${report.path}: Die Textstelle „${snippet}…" kommt so NICHT in der Datei vor. Zitiere einen kürzeren, wirklich vorhandenen Ausschnitt.`);
      if (outcome.status === "ambiguous") lines.push(`${report.path}: Die Textstelle „${snippet}…" kommt ${outcome.occurrences}-mal vor. Nimm mehr Kontext davor/danach mit — oder setze "all": true, wenn wirklich alle Vorkommen gemeint sind.`);
    }
  }
  return lines.length ? lines.join("\n") : undefined;
}
