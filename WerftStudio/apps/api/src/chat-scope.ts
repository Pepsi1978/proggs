// Bevor ein Modell überhaupt läuft, wird die Aufgabe deterministisch eingeordnet: Gilt der Wunsch
// EINEM Bildschirm oder dem GANZEN Design? Welche Dateien passen in das Kontextfenster genau dieses
// Modells? Und welche Bildschirme und Bausteine gibt es überhaupt?
//
// Bisher fehlte diese Stufe komplett: der Wunsch ging roh an das Modell, das Kontextfenster wurde mit
// „ein Byte = ein Token" geschätzt (Faktor 3 zu pessimistisch, deshalb „Eingabe zu groß" bei Designs,
// die problemlos hineinpassen), und eine Landkarte des Designs gab es nie — das Modell musste raten,
// welche Bildschirme existieren, und fragte deshalb nach, statt zu arbeiten.

export type DesignScreen = { id: string; name: string };
export type ChatFileCandidate = { path: string; size: number; mime: string };

// Wörter, mit denen ein Wunsch das GESAMTE Design meint. Trifft eines zu, darf sich die Änderung
// nicht auf den gerade sichtbaren Bildschirm beschränken.
const globalWords = /\b(?:gesamt\w*|ganz\w*|komplett\w*|überall|ueberall|durchgehend|durchgängig|durchgaengig|generell|grundsätzlich|grundsaetzlich|global|app-weit|appweit|einheitlich|konsistent|jede[nrms]?|alle[nrms]?|sämtliche\w*|saemtliche\w*|whole|entire|everywhere)\b/i;
// … in Verbindung mit dem, worauf sie sich beziehen. „alle Ränder auf diesem Bildschirm" ist NICHT global.
const scopeNouns = /\b(?:design|app|anwendung|oberfläche|oberflaeche|bildschirm\w*|screen\w*|seite\w*|ansicht\w*|view\w*|fenster|dialog\w*|theme\w*|erscheinung\w*|modus|darstellung|layout\w*|stil\w*|style\w*|dunkeldesign|dunkelmodus|hellmodus)\b/i;
// Zeigt der Wunsch ausdrücklich auf das, was gerade zu sehen ist, bleibt er dort — auch wenn darin
// „alle Ecken" steht. Nur ganze Wendungen zählen: ein blosses „diesem" kommt in jedem zweiten Satz vor.
const singleScreenWords = /\b(?:nur hier|hier(?:\s+(?:im|auf|in))?\b|auf dieser seite|auf diesem bildschirm|in diesem bildschirm|dieser bildschirm|this screen|only here)\b/i;
// Unmissverständlich global — das sticht jeden Hinweis auf den sichtbaren Bildschirm.
const wholeDesignWords = /(?:gesamt|ganz|komplett|sämtlich|saemtlich)\w*\s+\w*\s*(?:design|app|anwendung|oberfläche|oberflaeche)|\balle[nr]?\s+(?:bildschirm\w*|screens?|seiten|ansichten|unterbildschirm\w*)|\büberall\b|\bueberall\b|\bapp-weit\b/i;

export function globalScope(message: string): boolean {
  if (wholeDesignWords.test(message)) return true;
  if (singleScreenWords.test(message)) return false;
  // Das Bezugswort muss vorkommen — sonst wäre „mach alle Ecken runder" schon ein Vollumbau.
  return globalWords.test(message) && scopeNouns.test(message);
}

// Die Bildschirme stehen wörtlich im aufgebauten Dokument. Sie als Liste mitzugeben ist der
// Unterschied zwischen „auf welchem Bildschirm meinst du das?" und sofortiger Umsetzung.
export function designScreens(html: string): DesignScreen[] {
  const screens: DesignScreen[] = [];
  const seen = new Set<string>();
  for (const match of html.matchAll(/<section\b[^>]*\bclass="[^"]*\bwerft-screen\b[^"]*"[^>]*>/gi)) {
    const tag = match[0];
    const id = /\bdata-screen-id="([^"]*)"/i.exec(tag)?.[1];
    if (!id || seen.has(id)) continue;
    seen.add(id);
    screens.push({ id, name: /\bdata-screen-name="([^"]*)"/i.exec(tag)?.[1] || id });
  }
  return screens;
}

// Die wiederverwendeten Bausteine eines Designs: Klassen, die auf MEHREREN Bildschirmen vorkommen.
// Genau sie sind bei einem Wunsch „gesamtes Design" der richtige Angriffspunkt — eine geänderte
// gemeinsame Regel wirkt überall, statt achtzehnmal dasselbe zu ersetzen.
export function sharedClasses(html: string, limit = 40): string[] {
  const counts = new Map<string, number>();
  for (const match of html.matchAll(/\bclass="([^"]*)"/gi)) {
    for (const name of match[1]!.split(/\s+/)) {
      if (!name || name.startsWith("werft-")) continue;
      counts.set(name, (counts.get(name) ?? 0) + 1);
    }
  }
  return [...counts].filter(([, count]) => count > 1).sort((left, right) => right[1] - left[1]).slice(0, limit).map(([name]) => name);
}

export function designMap(html: string, entryPath: string, visibleScreen?: string): string {
  const screens = designScreens(html);
  const shared = sharedClasses(html);
  const lines = [`Startdatei des Designs: ${entryPath}`];
  if (screens.length) {
    lines.push(`Bildschirme in dieser Datei (${screens.length}), jeder als <section class="werft-screen" data-screen-id="…">:`);
    lines.push(screens.map((screen) => `- ${screen.name} (data-screen-id="${screen.id}")`).join("\n"));
  }
  if (shared.length) lines.push(`Mehrfach verwendete CSS-Klassen (eine Regeländerung hier wirkt auf allen Bildschirmen): ${shared.join(", ")}`);
  if (visibleScreen) lines.push(`Gerade sichtbar: ${visibleScreen}`);
  return lines.join("\n");
}

// Die Bildschirm-Abschnitte eines aufgebauten Dokuments, mit ihrer genauen Lage im Text. Verschachtelte
// <section>-Elemente werden mitgezählt, sonst endete ein Bildschirm beim erstbesten </section>.
export function designSections(html: string): Array<{ start: number; end: number; id: string; name: string }> {
  const sections: Array<{ start: number; end: number; id: string; name: string }> = [];
  const tags = [...html.matchAll(/<section\b[^>]*>|<\/section\s*>/gi)];
  let depth = 0, openIndex = -1, openTag = "";
  for (const tag of tags) {
    if (tag[0].startsWith("</")) {
      depth = Math.max(0, depth - 1);
      if (depth === 0 && openIndex >= 0) {
        const id = /\bdata-screen-id="([^"]*)"/i.exec(openTag)?.[1];
        if (id) sections.push({ start: openIndex, end: tag.index + tag[0].length, id, name: /\bdata-screen-name="([^"]*)"/i.exec(openTag)?.[1] || id });
        openIndex = -1;
        openTag = "";
      }
      continue;
    }
    if (depth === 0 && /\bclass="[^"]*\bwerft-screen\b/i.test(tag[0])) { openIndex = tag.index; openTag = tag[0]; }
    depth += 1;
  }
  return sections;
}

// Notausgang für Designs, die als Ganzes nicht in das Kontextfenster des gewählten Modells passen:
// Stile, Kopf und Skripte bleiben VOLLSTÄNDIG erhalten (dort spielt sich Gestaltung ab), von den
// Bildschirmen bleibt der gefragte stehen, die übrigen werden durch einen Hinweis ersetzt. Damit
// arbeitet auch ein kleines Modell an einem großen Design weiter, statt mit „Eingabe zu groß"
// abzubrechen. Die Edits treffen die echte Datei trotzdem, weil sie über Textstellen arbeiten.
export function excerptDesign(html: string, keepScreens: string[]): { text: string; omittedScreens: string[] } {
  const sections = designSections(html);
  if (!sections.length) return { text: html, omittedScreens: [] };
  const wanted = new Set(keepScreens.filter(Boolean));
  const keep = sections.filter((section) => wanted.has(section.id) || wanted.has(section.name));
  const kept = keep.length ? keep : [sections[0]!];
  const keptIds = new Set(kept.map((section) => section.id));
  const omittedScreens: string[] = [];
  let text = "";
  let cursor = 0;
  for (const section of sections) {
    text += html.slice(cursor, section.start);
    const body = html.slice(section.start, section.end);
    if (keptIds.has(section.id)) text += body;
    else {
      omittedScreens.push(section.name);
      const openTag = /^<section\b[^>]*>/i.exec(body)?.[0] ?? "<section>";
      text += `${openTag}\n<!-- Inhalt dieses Bildschirms wurde ausgelassen, damit die Datei in dein Kontextfenster passt. Ändere ihn nicht direkt. -->\n</section>`;
    }
    cursor = section.end;
  }
  return { text: text + html.slice(cursor), omittedScreens };
}

// Wie viele ZEICHEN Eingabe verträgt ein Modell? Für Markup, CSS und Quellcode trägt ein Token
// erfahrungsgemäss drei bis vier Zeichen; drei ist der sichere, aber realistische Ansatz. Die alte
// Annahme „ein Byte = ein Token" verwarf Designs, die problemlos hineinpassen.
export const charsPerToken = 3;
export function inputCharBudget(contextLength: number | undefined, reservedOutputTokens: number): number {
  const fallback = 700_000;
  if (!contextLength || contextLength <= 0) return fallback;
  const usableTokens = contextLength - reservedOutputTokens;
  return Math.max(20_000, Math.floor(usableTokens * charsPerToken));
}

// Wie viele Ausgabe-Token muss man freihalten? Eine Design-Änderung liefert viele Edits; zu knapp
// bemessen bricht die Antwort mitten im JSON ab (genau der Fall „kein gültiges JSON").
export function reservedOutputTokens(contextLength: number | undefined, endpointMaximum?: number): number {
  const wanted = 32_000;
  const byContext = contextLength ? Math.floor(contextLength * 0.35) : wanted;
  const limit = endpointMaximum && endpointMaximum > 0 ? endpointMaximum : wanted;
  return Math.max(4_000, Math.min(wanted, byContext, limit));
}

export type FileSelection = { selected: ChatFileCandidate[]; omitted: ChatFileCandidate[] };

// Die Startdatei ist unverzichtbar — sie enthält das Design. Danach wird nach Größe aufgefüllt, damit
// möglichst VIELE Begleitdateien mitkommen. Ausgelassene Dateien werden gemeldet statt still
// verschluckt: das Modell soll wissen, was es nicht sehen kann.
export function selectChatFiles(candidates: ChatFileCandidate[], entryPath: string, budgetChars: number): FileSelection {
  const entry = candidates.find((file) => file.path === entryPath);
  const rest = candidates.filter((file) => file.path !== entryPath).sort((left, right) => left.size - right.size);
  const selected: ChatFileCandidate[] = [];
  const omitted: ChatFileCandidate[] = [];
  let budget = budgetChars;
  if (entry) {
    // Die Startdatei kommt auch dann mit, wenn sie das Budget fast aufbraucht: ohne sie ist der
    // ganze Lauf sinnlos. Passt sie gar nicht, meldet der Aufrufer das als klaren Fehler.
    selected.push(entry);
    budget -= entry.size;
  }
  for (const file of rest) {
    if (file.size <= budget) { selected.push(file); budget -= file.size; }
    else omitted.push(file);
  }
  return { selected, omitted };
}
