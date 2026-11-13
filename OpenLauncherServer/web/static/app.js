"use strict";
const $ = (id) => document.getElementById(id);
const api = async (pfad, daten) => {
  const antwort = await fetch(pfad, daten === undefined ? {} : { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(daten) });
  const inhalt = await antwort.json().catch(() => ({}));
  if (!antwort.ok) throw new Error(inhalt.fehler || `HTTP ${antwort.status}`);
  return inhalt;
};
const CLI_NAME = { claude: "Claude Code", codex: "Codex CLI", opencode: "OpenCode", kimi: "Kimi Code", antigravity: "Antigravity CLI" };
const MODUS_NAME = { frei: "Freimodus", schnell: "Schnellmodus", normal: "Normalmodus", gruendlich: "Gründlichkeitsmodus" };
const PROFIL_NAME = { minimal: "Minimal", standard: "Standard", strict: "Strikt" };
const element = (art, klasse, text) => { const e = document.createElement(art); if (klasse) e.className = klasse; if (text !== undefined) e.textContent = text; return e; };
const dauer = (s) => s < 60 ? `${s} s` : s < 3600 ? `${Math.round(s / 60)} min` : `${Math.round(s / 3600)} h`;

// ---- Darstellung: Wahl und Sonnenstand stehen in index.html (window.Thema), hier nur Terminal und Anzeige
const TERM_SCHRIFT = '"Roboto Mono","Noto Sans Mono","Droid Sans Mono","DejaVu Sans Mono","Cascadia Mono",Menlo,Consolas,monospace';
const uhrzeit = (d) => d.toLocaleTimeString("de-DE", { timeZone: "Europe/Berlin", hour: "2-digit", minute: "2-digit" });
// Auf hellem Grund sind Gelb, Türkis und Hellgrau der Original-Paletten oft unter 3:1: xterm dunkelt sie dann selbst ab.
// Auf Dunkel 3: Einige Original-Paletten (Tokyo Night, One Dark, Dracula) legen ANSI-Schwarz fast auf den Hintergrund.
function termThema(s) {
  const d = s.design || Thema.designs.find((x) => x.id === "klassisch") || { term: {} };
  return { theme: { ...d.term }, minimumContrastRatio: s.aktiv === "hell" ? 4.5 : 3 };
}
const termSchluessel = (s) => `${s.design && s.design.id}/${s.aktiv}`;
let termDesign = null;
function themaAnwenden(s = Thema.anwenden()) {
  // xterm übernimmt ein neues Thema nur als neues Objekt, darum nur bei echtem Wechsel und immer frisch kopiert.
  const schluessel = termSchluessel(s);
  if (term && termDesign !== schluessel) { const t = termThema(s); term.options.theme = t.theme; term.options.minimumContrastRatio = t.minimumContrastRatio; termDesign = schluessel; }
  if (seite !== "system") return;
  const naechster = s.wahl !== "auto" ? "" : s.aktiv === "hell" ? ` Dunkles Design ab ${uhrzeit(s.unter)} Uhr.` : ` Helles Design ab Sonnenaufgang.`;
  $("thema-info").textContent = `Heute in Berlin: Sonnenaufgang ${uhrzeit(s.auf)} Uhr, Sonnenuntergang ${uhrzeit(s.unter)} Uhr. Aktiv: ${s.design ? s.design.name : ""} (${s.aktiv === "hell" ? "hell" : "dunkel"}).${naechster}`;
}
// Vorschau als kleines Terminal in den Farben des Designs: Ein gewähltes dunkles Design ist am Tag sonst nicht zu sehen.
function vorschau(art) {
  const d = Thema.designs.find((x) => x.id === Thema.design(art));
  const ziel = $(`vorschau-${art}`);
  ziel.replaceChildren();
  if (!d) return;
  const t = d.term, ui = d.ui;
  ziel.style.background = t.background;
  ziel.style.fontFamily = TERM_SCHRIFT;
  ziel.style.color = t.foreground || "#ffffff";
  ziel.style.borderColor = ui.rand;
  const teil = (text, farbe, fett) => { const e = element("span", "", text); if (farbe) e.style.color = farbe; if (fett) e.style.fontWeight = "700"; return e; };
  ziel.append(teil("❯ ", ui.akzent, true), teil("claude ", t.foreground), teil("--fertig", t.brightBlack || ui.leise), document.createElement("br"),
    teil("✓ grün ", t.green || ui.gut), teil("⚠ gelb ", t.yellow || ui.warn), teil("✗ rot ", t.red || ui.gefahr), teil("blau ", t.blue), teil("lila ", t.magenta), teil("türkis", t.cyan));
}
function designListe(art) {
  const wahl = $(`design-${art}`);
  for (const d of Thema.designs.filter((x) => x.art === art)) { const o = element("option", "", d.name); o.value = d.id; wahl.append(o); }
  wahl.value = Thema.design(art);
  wahl.addEventListener("change", () => { themaAnwenden(Thema.designWaehlen(art, wahl.value)); vorschau(art); });
  vorschau(art);
}

// ---- Reiter
let seite = "sitzungen";
// Tastenleiste STRG/ALT, siehe waehle().
const kombination = { strg: (b) => String.fromCharCode(b.charCodeAt(0) - 96), alt: (b) => `\x1b${b}` };
let tastenWahl = "";
function zeige(name) {
  waehle("");
  seite = name;
  document.querySelectorAll("#reiter button").forEach((b) => b.classList.toggle("an", b.dataset.seite === name));
  document.querySelectorAll(".seite").forEach((s) => s.classList.toggle("an", s.id === name));
  if (name === "terminal") { setTimeout(passen, 30); }
  if (name === "sitzungen") ladeSitzungen();
  if (name === "system") { ladeSystem(); themaAnwenden(); }
  if (name === "browser") browserOeffnen(); else browserLoesen();
  if (name !== "terminal") aufnahmeAbbrechen();
}
document.querySelectorAll("#reiter button").forEach((b) => b.addEventListener("click", () => zeige(b.dataset.seite)));

// ---- Sitzungen
// Zweimal tippen statt confirm(): Die WebView in Jarvis lehnt JavaScript-Dialoge ohne eigene Behandlung still ab.
let scharf = null;
function zweimal(knopf, aktion) {
  knopf.addEventListener("click", () => {
    if (scharf === knopf) { scharf = null; aktion(); return; }
    const text = knopf.textContent;
    scharf = knopf;
    knopf.textContent = "Wirklich beenden?";
    setTimeout(() => { if (scharf === knopf) { scharf = null; knopf.textContent = text; } }, 4000);
  });
}
async function ladeSitzungen() {
  if (scharf) return;
  let daten;
  try { daten = await api("/api/sitzungen"); } catch (f) { $("auslastung").textContent = f.message; return; }
  $("auslastung").textContent = daten.belegt ? "Der Server ist ausgelastet: Neue Jarvis-Aufträge gehen an den nächsten Rechner." : "";
  const liste = $("liste");
  liste.replaceChildren();
  if (!daten.sitzungen.length) liste.append(element("p", "leise", "Keine Sitzung offen. Unter OpenLauncher lässt sich eine starten."));
  for (const s of daten.sitzungen) {
    const karte = element("div", "karte");
    karte.append(element("h3", "", s.titel));
    const zeile = element("div", "leise");
    zeile.append(element("span", `punkt ${s.stand}`));
    const angaben = [s.stand];
    if (s.stand !== "arbeitet") angaben.push(`seit ${dauer(s.still_s)} still`);
    if (s.modell) angaben.push(s.modell);
    if (s.effort) angaben.push(s.effort);
    if (s.modus) angaben.push(MODUS_NAME[s.modus] || s.modus);
    zeile.append(angaben.join(" · "));
    karte.append(zeile);
    const aktionen = element("div", "aktionen");
    const oeffnen = element("button", "", "Öffnen");
    oeffnen.addEventListener("click", () => verbinde(s.name, s.titel));
    const zu = element("button", "gefahr", "Schließen");
    if (s.stand === "beendet") zu.addEventListener("click", () => schliesse(s.name));
    else zweimal(zu, () => schliesse(s.name));
    aktionen.append(oeffnen, zu);
    karte.append(aktionen);
    liste.append(karte);
  }
}
async function schliesse(name) {
  if (aktuell === name) trennen();
  await api("/api/schliessen", { name });
  ladeSitzungen();
}
$("neu-pwsh").addEventListener("click", async () => { const { name } = await api("/api/shell", {}); verbinde(name, "PowerShell"); });

// ---- Terminal
let term = null, fit = null, ws = null, aktuell = null, neuVersuch = null;
function aufbauen() {
  if (term) return;
  // Eigene Schriftliste: xterms Vorgabe „courier“ wird auf Android zu Cutive Mono, einer sehr breiten Schreibmaschinenschrift.
  term = new Terminal({ fontSize: 13, fontFamily: TERM_SCHRIFT, scrollback: 5000, cursorBlink: true, convertEol: false, ...termThema(Thema.anwenden()) });
  termDesign = termSchluessel(Thema.anwenden());
  fit = new FitAddon.FitAddon();
  term.loadAddon(fit);
  term.open($("xterm"));
  // Android holt die Tastatur bei jedem Tipper zurück, solange dieses Feld den Fokus hat, auch bei Esc oder STRG. Tippen am Handy geht nur übers Eingabefeld.
  term.textarea.inputMode = "none";
  // Auch xterm selbst antwortet hier auf Anfragen von tmux: Das darf kein offenes Menü schließen.
  term.onData((d) => schicke(d, false));
  new ResizeObserver(() => passen()).observe($("xterm"));
  wischenEinrichten();
}
// Mit dem Finger scrollen: tmux hält den Verlauf, das Terminal auf dem Handy nicht. Wischen wird zu Zeilen, ein Schwung läuft kurz nach.
// Die Fläche liegt über dem Terminal: Es ersetzt beim Neuzeichnen die Zeile unter dem Finger, und danach kämen die Berührungen nirgends mehr an.
let offeneZeilen = 0, rollZeit = null, schwung = null;
function rollenStoppen() { clearInterval(schwung); schwung = null; clearTimeout(rollZeit); rollZeit = null; offeneZeilen = 0; }
function rollen(zeilen) {
  $("nach-unten").hidden = false;
  offeneZeilen += zeilen;
  if (rollZeit) return;
  rollZeit = setTimeout(() => {
    rollZeit = null;
    if (offeneZeilen && ws && ws.readyState === 1) ws.send(JSON.stringify({ t: "s", n: offeneZeilen }));
    offeneZeilen = 0;
  }, 100);
}
// Lage in der Zeichenreihe des gesamten Puffers, damit die Länge auch über Zeilen hinweg stimmt.
let markierAnfang = 0, markierEnde = 0, markierFlaeche = null;
function zelle(x, y) {
  const r = markierFlaeche;
  const spalte = Math.max(0, Math.min(term.cols - 1, Math.floor((x - r.left) / r.width * term.cols)));
  const zeile = Math.max(0, Math.min(term.rows - 1, Math.floor((y - r.top) / r.height * term.rows)));
  return (term.buffer.active.viewportY + zeile) * term.cols + spalte;
}
function markierenBis(ziel) {
  const von = Math.min(markierAnfang, ziel);
  term.select(von % term.cols, Math.floor(von / term.cols), Math.abs(ziel - markierAnfang) + 1);
}
function markierenBeginnen(x, y) {
  markierFlaeche = term.element.querySelector(".xterm-screen").getBoundingClientRect();
  markierAnfang = markierEnde = zelle(x, y);
  markierenBis(markierAnfang);
}
function markierenZiehen(x, y) {
  const ziel = zelle(x, y);
  if (ziel !== markierEnde) { markierEnde = ziel; markierenBis(ziel); }
}
function wischenEinrichten() {
  const flaeche = $("xterm");
  const zeilenHoehe = () => Math.max(8, flaeche.clientHeight / (term ? term.rows : 24));
  let radRest = 0;
  flaeche.addEventListener("wheel", (e) => {
    e.preventDefault(); e.stopPropagation();
    radRest += e.deltaMode === 1 ? e.deltaY * zeilenHoehe() : e.deltaY;
    const zeilen = Math.trunc(radRest / zeilenHoehe());
    if (zeilen) { radRest -= zeilen * zeilenHoehe(); rollen(-zeilen); }
  }, { capture: true, passive: false });
  // Der Rückweg ohne Nebenwirkung: Jede Taste ginge zusätzlich an die CLI, und Esc würde sie unterbrechen.
  const nachUnten = $("nach-unten");
  nachUnten.addEventListener("mousedown", (e) => e.preventDefault());
  nachUnten.addEventListener("click", () => {
    rollenStoppen();
    nachUnten.hidden = true;
    if (ws && ws.readyState === 1) ws.send(JSON.stringify({ t: "e" }));
  });
  if (!("ontouchstart" in window)) return;
  const decke = element("div", "wisch");
  flaeche.append(decke);
  let y = null, rest = 0, tempo = 0, zuletzt = 0, weg = 0;
  // Lange drücken und ziehen markiert; die Markierung gilt bis zum nächsten Tipper oder einem Eintrag im Menü.
  let halten = null, markiert = false;
  decke.addEventListener("contextmenu", (e) => e.preventDefault());
  decke.addEventListener("touchstart", (e) => {
    // Ein zweiter Finger ist keine Haltegeste und bringt die laufende Bewegung nicht durcheinander.
    if (e.touches.length > 1) { clearTimeout(halten); return; }
    clearInterval(schwung); y = e.touches[0].clientY; rest = 0; tempo = 0; weg = 0; zuletzt = e.timeStamp;
    const { clientX: x0, clientY: y0 } = e.touches[0];
    markiert = false;
    clearTimeout(halten);
    halten = setTimeout(() => { markiert = true; navigator.vibrate?.(20); waehle(""); markierenBeginnen(x0, y0); }, 450);
  }, { passive: true });
  decke.addEventListener("touchmove", (e) => {
    if (y === null) return;
    e.preventDefault();
    if (markiert) { markierenZiehen(e.touches[0].clientX, e.touches[0].clientY); return; }
    const neu = e.touches[0].clientY, schritt = neu - y;
    if (e.timeStamp > zuletzt) tempo = schritt / (e.timeStamp - zuletzt);
    zuletzt = e.timeStamp; y = neu; rest += schritt; weg += Math.abs(schritt);
    if (weg >= 10) clearTimeout(halten);
    const zeilen = Math.trunc(rest / zeilenHoehe());
    if (zeilen) { rest -= zeilen * zeilenHoehe(); rollen(zeilen); }
  }, { passive: false });
  decke.addEventListener("touchend", (e) => {
    if (e.touches.length) return;
    y = null;
    clearTimeout(halten);
    if (markiert) { e.preventDefault(); markiert = false; terminalMenue(); return; }
    // Ein kurzer Tipper schließt nur, was offen ist. Die Tastatur kommt nie von hier: Frank spricht seine Eingaben, geschrieben wird im Eingabefeld.
    if (weg < 10) { e.preventDefault(); waehle(""); term.clearSelection(); if (tastaturAn) tastatur(false); return; }
    // Stand der Finger vor dem Loslassen still, läuft nichts nach.
    if (e.timeStamp - zuletzt > 100 || Math.abs(tempo) < 0.4) return;
    schwung = setInterval(() => {
      tempo *= 0.8;
      const zeilen = Math.trunc(tempo * 60 / zeilenHoehe());
      if (!zeilen) { clearInterval(schwung); return; }
      rollen(zeilen);
    }, 60);
  });
  decke.addEventListener("touchcancel", () => {
    y = null;
    clearTimeout(halten);
    // Ohne Menü ließe sich die halbe Markierung nicht mehr kopieren.
    if (markiert) { markiert = false; term.clearSelection(); }
  }, { passive: true });
}
function schicke(d, schliessen = true) {
  rollenStoppen();
  if (schliessen) waehle("");
  $("nach-unten").hidden = true;
  if (ws && ws.readyState === 1) ws.send(JSON.stringify({ t: "i", d }));
  // Sonst sieht ein Strg+C im Funkloch aus, als wäre es angekommen.
  // Bleibt bei einer offenen Sitzung stehen, bis die Verbindung wieder da ist.
  else { hinweis("Keine Verbindung, Eingabe nicht gesendet."); if (aktuell) clearTimeout(hinweisZeit); }
}
function passen() {
  if (!term || seite !== "terminal") return;
  try { fit.fit(); } catch { return; }
  if (ws && ws.readyState === 1) ws.send(JSON.stringify({ t: "r", c: term.cols, r: term.rows }));
}
let sitzungsTitel = "";
function trennen() {
  waehle("");
  $("nach-unten").hidden = true;
  rollenStoppen();
  aktuell = null;
  clearTimeout(neuVersuch);
  if (ws) { ws.onclose = null; ws.close(); ws = null; }
  $("titel").textContent = "Keine Sitzung gewählt";
  $("schliessen").hidden = true;
  aufnahmeAbbrechen();
  stimmeZeigen();
  $("links").replaceChildren();
  if (term) term.reset();
}
function verbinde(name, titel) {
  aufbauen();
  if (aktuell !== name) { trennen(); aktuell = name; }
  sitzungsTitel = titel || name;
  $("titel").textContent = sitzungsTitel;
  $("schliessen").hidden = false;
  stimmeZeigen();
  zeige("terminal");
  aufmachen();
}
function aufmachen() {
  const name = aktuell;
  if (!name) return;
  if (ws) { ws.onclose = null; ws.close(); }
  term.reset();
  ws = new WebSocket(`${location.protocol === "https:" ? "wss" : "ws"}://${location.host}/ws/${encodeURIComponent(name)}`);
  ws.binaryType = "arraybuffer";
  ws.onopen = () => { clearTimeout(hinweisZeit); $("titel").textContent = sitzungsTitel; passen(); };
  ws.onmessage = (e) => term.write(new Uint8Array(e.data));
  // Abriss (Mobilfunk, Tunnel): wieder anhängen. Die Sitzung läuft in tmux weiter.
  ws.onclose = (e) => {
    if (aktuell !== name) return;
    if (e.code === 4404) { trennen(); $("titel").textContent = "Diese Sitzung ist beendet."; ladeSitzungen(); return; }
    neuVersuch = setTimeout(aufmachen, 2000);
  };
}
zweimal($("schliessen"), () => { if (aktuell) { const n = aktuell; $("schliessen").textContent = "Sitzung schließen"; schliesse(n); zeige("sitzungen"); } });
// STRG und ALT: Ein Tipper öffnet die Leiste A bis Z zum Wischen, ein Buchstabe schickt die Tastenkombination und schließt sie.
function waehle(art) {
  tastenWahl = art;
  // Geht das Menü zu, gilt auch seine Markierung nicht mehr; ohne Menü ließe sie sich nicht mehr kopieren.
  if (!$("menue").hidden) { $("menue").hidden = true; term?.clearSelection(); }
  $("buchstaben").hidden = !art;
  if (art) $("buchstaben").scrollLeft = 0;
  // Das Mikrofon läge sonst halb unter der Leiste, ein Tipp darauf träfe einen Buchstaben.
  $("terminal").classList.toggle("wahl-offen", !!art);
  for (const name of Object.keys(kombination)) $(name).classList.toggle("an", name === art);
}
for (const b of "abcdefghijklmnopqrstuvwxyz") {
  const knopf = element("button", "", b.toUpperCase());
  knopf.addEventListener("click", () => { const art = tastenWahl; waehle(""); schicke(kombination[art](b)); });
  $("buchstaben").append(knopf);
}
for (const name of Object.keys(kombination)) $(name).addEventListener("click", () => waehle(tastenWahl === name ? "" : name));
document.querySelectorAll("#tasten button[data-t]").forEach((b) => b.addEventListener("click", () => schicke(b.dataset.t)));
// Der Fokus bleibt im Eingabefeld, sonst klappt die Handy-Tastatur bei jedem Tipper zu.
for (const name of ["tasten", "buchstaben", "menue", "stimme-tastatur"]) $(name).addEventListener("mousedown", (e) => e.preventDefault());
// Wer weiterschreibt statt einen Buchstaben zu wählen, braucht die Leiste nicht mehr; sie läge sonst über der Eingabezeile der CLI.
$("text").addEventListener("input", () => waehle(""));
// Zwischenablage: In Jarvis über die Schnittstelle der App, denn die Konsole läuft im Tunnel über HTTP und sieht navigator.clipboard dort nicht.
let hinweisZeit = null;
function hinweis(text) {
  $("titel").textContent = text;
  clearTimeout(hinweisZeit);
  hinweisZeit = setTimeout(() => { $("titel").textContent = aktuell ? sitzungsTitel : "Keine Sitzung gewählt"; }, 2500);
}
// Antwortet, ob der Text wirklich in der Zwischenablage liegt: Ausschneiden darf sonst nichts löschen.
async function kopieren(text) {
  if (!text) { hinweis("Nichts zum Kopieren."); return false; }
  let geklappt;
  if (window.JarvisAblage) geklappt = JarvisAblage.schreiben(text);
  else if (navigator.clipboard) geklappt = await navigator.clipboard.writeText(text).then(() => true, () => false);
  else { hinweis("Kopieren geht hier nur in Jarvis."); return false; }
  hinweis(geklappt ? "Kopiert." : "Kopieren hat nicht geklappt.");
  return geklappt;
}
async function einfuegen(ziel) {
  let text;
  if (window.JarvisAblage) text = JarvisAblage.lesen();
  else if (navigator.clipboard) text = await navigator.clipboard.readText().catch(() => null);
  else return hinweis("Einfügen geht hier nur in Jarvis.");
  if (text === null) hinweis("Die Zwischenablage ließ sich nicht lesen.");
  else if (text) ziel(text);
  else hinweis("Die Zwischenablage ist leer.");
}
function menue(eintraege) {
  waehle("");
  $("menue").replaceChildren(...eintraege.map(([name, aktion]) => {
    const knopf = element("button", "", name);
    knopf.addEventListener("click", () => { waehle(""); aktion(); });
    return knopf;
  }));
  $("menue").hidden = false;
  $("terminal").classList.add("wahl-offen");
}
function insTerminal(text) {
  if (!aktuell) return hinweis("Keine Sitzung offen.");
  // Ohne Bracketed Paste liefe jede Zeile sofort als eigener Befehl: Dann erst ins Eingabefeld, zum Ansehen.
  if (/[\r\n]/.test(text) && !term.modes.bracketedPasteMode) {
    insFeld(text);
    return hinweis("Mehrzeilig: erst im Eingabefeld, mit Senden abschicken.");
  }
  term.paste(text);
}
function feldAuswahl() {
  const t = $("text");
  // Ohne Fokus ist eine alte Auswahl nicht mehr zu sehen und darf nichts ersetzen.
  return document.activeElement === t ? [t.selectionStart, t.selectionEnd] : [t.value.length, t.value.length];
}
function insFeld(text) { const [von, bis] = feldAuswahl(); $("text").setRangeText(text, von, bis, "end"); }
function terminalMenue() {
  // Der Text wird jetzt festgehalten: Eine weiterlaufende CLI darf die Markierung auf dem Bildschirm überschreiben.
  const text = term.getSelection();
  menue([
    ["Kopieren", () => kopieren(text)],
    ["Alles Sichtbare", () => { term.selectAll(); terminalMenue(); }],
    ["Einfügen", () => einfuegen(insTerminal)],
    ["✕", () => {}],
  ]);
}
function feldMenue() {
  const t = $("text");
  const auswahl = () => t.value.slice(...feldAuswahl());
  menue([
    ["Einfügen", () => einfuegen(insFeld)],
    ["Kopieren", () => kopieren(auswahl() || t.value)],
    ["Ausschneiden", async () => {
      const [von, bis] = feldAuswahl();
      const teil = von !== bis;
      if (!await kopieren(teil ? t.value.slice(von, bis) : t.value)) return;
      if (teil) t.setRangeText("", von, bis, "end"); else t.value = "";
    }],
    ["Alles auswählen", () => { t.focus(); t.select(); feldMenue(); }],
    ["✕", () => {}],
  ]);
}
// Tastatur: Sie kommt nur beim Tipper ins Eingabefeld oder über den Knopf, nie bei anderen Tastern oder langem Drücken (inputmode none hält sie dann unten).
let tastaturAn = false, letzteHoehe = window.innerHeight;
function tastatur(an) {
  const t = $("text");
  tastaturAn = an;
  t.inputMode = an ? "text" : "none";
  $("stimme-tastatur").classList.toggle("an", an);
  t.blur();
  if (an) t.focus();
}
$("stimme-tastatur").addEventListener("click", () => tastatur(!tastaturAn));
// Per Zurück-Geste oder Senden geschlossen: Der Zustand muss folgen, sonst holt der nächste Tipper sie zurück. Jarvis meldet das sicher, siehe ServerKonsoleActivity.
function tastaturZu() { if (tastaturAn) tastatur(false); }
window.addEventListener("resize", () => {
  const h = window.innerHeight;
  if (!window.JarvisAblage && h > letzteHoehe + 100) tastaturZu();
  letzteHoehe = h;
});
{
  const t = $("text");
  let halten = null, gehalten = false, finger = false, x0 = 0, y0 = 0;
  const loslassen = () => { finger = false; clearTimeout(halten); };
  // Nur der Finger bekommt das eigene Menü: Der Rechtsklick am Rechner behält das des Browsers, und ohne nutzbare Zwischenablage bleibt auch das Handy beim eigenen.
  t.addEventListener("contextmenu", (e) => { if (finger && (window.JarvisAblage || navigator.clipboard)) e.preventDefault(); });
  t.addEventListener("touchstart", (e) => {
    finger = true; gehalten = false;
    x0 = e.touches[0].clientX; y0 = e.touches[0].clientY;
    clearTimeout(halten);
    if (window.JarvisAblage || navigator.clipboard) halten = setTimeout(() => { gehalten = true; navigator.vibrate?.(20); feldMenue(); }, 500);
  }, { passive: true });
  t.addEventListener("touchmove", (e) => { if (Math.hypot(e.touches[0].clientX - x0, e.touches[0].clientY - y0) > 10) clearTimeout(halten); }, { passive: true });
  t.addEventListener("touchend", loslassen, { passive: true });
  t.addEventListener("touchcancel", loslassen, { passive: true });
  t.addEventListener("click", () => {
    if (gehalten) { gehalten = false; return; }
    waehle("");
    if (!tastaturAn) tastatur(true);
  });
}
$("eingabe").addEventListener("submit", (e) => {
  e.preventDefault();
  const t = $("text");
  // Text und Eingabetaste getrennt: Sonst liest eine CLI im Einfügemodus das Enter als Zeilenumbruch.
  if (t.value) schicke(t.value);
  setTimeout(() => schicke("\r"), 60);
  t.value = "";
});
async function ladeLinks() {
  if (seite !== "terminal" || !aktuell) return;
  const { links } = await api(`/api/links/${encodeURIComponent(aktuell)}`).catch(() => ({ links: [] }));
  const ziel = $("links");
  ziel.replaceChildren();
  for (const l of links.filter((x) => /auth|login|oauth|device|claude\.ai|chatgpt|openai|google|kimi/i.test(x)).slice(-2)) {
    const a = element("a", "", l);
    a.href = l; a.target = "_blank"; a.rel = "noopener";
    ziel.append(a);
  }
}

// ---- OpenLauncher
let angebot = { modelle: [] };
function fuelle(id, werte, namen, wahl) {
  const s = $(id);
  s.replaceChildren();
  for (const w of werte) { const o = element("option", "", (namen && namen[w]) || w); o.value = w; s.append(o); }
  if (wahl && werte.includes(wahl)) s.value = wahl;
}
function modellGewaehlt() {
  const m = angebot.modelle[Number($("modell").value)];
  if (!m) return;
  fuelle("cli", m.clis, CLI_NAME, m.anbieter === "openai" ? "codex" : (m.clis.includes(m.cli) ? m.cli : m.clis[0]));
  fuelle("profil", angebot.profile, PROFIL_NAME, m.profil);
  fuelle("modus", angebot.modi, MODUS_NAME, m.modus);
  cliGewaehlt(m.effort);
}
function cliGewaehlt(wahl) {
  const m = angebot.modelle[Number($("modell").value)];
  // Während eines Updates kann der Server noch den Katalog der alten Fassung liefern, ohne efforts je Modell.
  const efforts = (m && m.efforts && m.efforts[$("cli").value]) || [];
  fuelle("effort", efforts, null, typeof wahl === "string" && efforts.includes(wahl) ? wahl : efforts.includes("high") ? "high" : efforts[efforts.length - 1]);
}
async function ladeKatalog() {
  angebot = await api("/api/katalog");
  const s = $("modell");
  s.replaceChildren();
  let gruppe = null, ort = s;
  angebot.modelle.forEach((m, i) => {
    if (m.gruppe !== gruppe) { gruppe = m.gruppe; ort = document.createElement("optgroup"); ort.label = gruppe; s.append(ort); }
    const o = element("option", "", m.name); o.value = String(i); ort.append(o);
  });
  const opus = angebot.modelle.findIndex((m) => m.slug.startsWith("claude-opus-5-5"));
  if (opus >= 0) s.value = String(opus);
  modellGewaehlt();
}
$("modell").addEventListener("change", modellGewaehlt);
$("cli").addEventListener("change", () => cliGewaehlt());
$("start").addEventListener("click", async () => {
  const m = angebot.modelle[Number($("modell").value)];
  if (!m) return;
  $("start-meldung").textContent = "Startet …";
  try {
    const titel = `${m.name} (${CLI_NAME[$("cli").value]})`;
    const { name } = await api("/api/starten", { cli: $("cli").value, modell: m.slug, anbieter: m.anbieter, effort: $("effort").value, profil: $("profil").value, modus: $("modus").value, titel, prompt: $("prompt").value });
    $("prompt").value = ""; $("start-meldung").textContent = "";
    verbinde(name, titel);
  } catch (f) { $("start-meldung").textContent = f.message; }
});

// ---- Browser
async function browserOeffnen() {
  const bild = $("browser-bild"), meldung = $("browser-meldung");
  if (!bild.hidden) return;
  meldung.hidden = false;
  meldung.textContent = "Der Browser startet …";
  try {
    // Auch nach einem Absturz oder der Pause bei Untätigkeit: Der Server startet ihn, wenn nötig.
    await api("/api/browser", {});
    if (seite !== "browser") return;
    bild.src = "/vnc/vnc.html?autoconnect=1&resize=scale&path=vnc-ws&reconnect=1";
    bild.hidden = false;
    meldung.hidden = true;
  } catch (f) { meldung.textContent = `Der Browser ließ sich nicht starten: ${f.message}`; }
}
// Beim Verlassen trennen: Sonst liefen Bilder weiter durch den Tunnel, und der Browser gälte nie als untätig.
function browserLoesen() {
  const bild = $("browser-bild");
  if (bild.hidden) return;
  bild.hidden = true;
  bild.removeAttribute("src");
}
document.addEventListener("visibilitychange", () => {
  if (document.hidden) { browserLoesen(); aufnahmeAbbrechen(); return; }
  themaAnwenden();
  if (seite === "browser") browserOeffnen();
});

// ---- Sprache (TVO): Nur in Jarvis, das Handy nimmt auf und wandelt um. Erscheint von selbst, sobald ein Terminal offen ist, wie das Overlay an den Rechnern.
const SPRACHE = window.JarvisSprache;
let automatisch = false, diktatSitzung = null, meldeZeit = null;
function stimmeZeigen() { if (SPRACHE) $("stimme").hidden = !aktuell; }
function stimmeMelden(text) {
  const m = $("stimme-meldung");
  m.textContent = text;
  m.hidden = !text;
  clearTimeout(meldeZeit);
  if (text) meldeZeit = setTimeout(() => { m.hidden = true; }, 5000);
}
function aufnahmeAbbrechen() {
  if (!SPRACHE || $("stimme-mikro").dataset.stand !== "nimmt") return;
  SPRACHE.abbrechen();
  mikroStand("bereit");
}
function mikroStand(stand) {
  const knopf = $("stimme-mikro");
  knopf.dataset.stand = stand;
  knopf.textContent = stand === "nimmt" ? "■" : stand === "wandelt" ? "…" : "🎤";
}
window.stimmeFertig = (text, fehler) => {
  mikroStand("bereit");
  if (fehler) { stimmeMelden(fehler); return; }
  if (!text) return;
  // Das Diktat gehört zu der Sitzung, in der es begann: nie mit automatischem Absenden in eine andere.
  if (diktatSitzung !== aktuell || !ws || ws.readyState !== 1) { stimmeMelden(`Nicht eingefügt, die Sitzung hat gewechselt: ${text.slice(0, 120)}`); return; }
  // Jedes Diktat endet mit dem Aufgabentrenner " ; " wie am Rechner (TVO, auch mit automatischem Absenden): Die CLI erkennt daran mehrere Aufgaben, die nächste Ansage schließt direkt an.
  const kern = text.trim().replace(/\s*;$/, "");
  if (!kern) return;
  schicke(kern + " ; ");
  const sitzung = aktuell;
  if (automatisch) setTimeout(() => { if (sitzung === aktuell) schicke("\r"); }, 60);
};
if (SPRACHE) {
  $("stimme-mikro").addEventListener("click", () => {
    const stand = $("stimme-mikro").dataset.stand;
    if (stand === "wandelt") return;
    if (stand === "nimmt") { mikroStand("wandelt"); SPRACHE.stop(); } else { diktatSitzung = aktuell; mikroStand("nimmt"); SPRACHE.start(); }
  });
  $("stimme-zeile").addEventListener("click", () => schicke("\u0015"));
  try { automatisch = localStorage.getItem("stimme-auto") === "1"; } catch { /* nur Komfort */ }
  const autoAnzeigen = () => { $("stimme-auto").textContent = automatisch ? "↵ an" : "↵ aus"; $("stimme-auto").classList.toggle("an", automatisch); };
  autoAnzeigen();
  $("stimme-auto").addEventListener("click", () => { automatisch = !automatisch; try { localStorage.setItem("stimme-auto", automatisch ? "1" : "0"); } catch { /* nur Komfort */ } autoAnzeigen(); });
}

// ---- System
async function ladeSystem() {
  const d = await api("/api/system").catch(() => null);
  if (!d) return;
  const ziel = $("programme");
  ziel.replaceChildren();
  for (const [schluessel, p] of Object.entries(d.programme || {})) {
    const karte = element("div", "karte");
    karte.append(element("h3", "", p.name));
    const zeile = element("div", "leise");
    zeile.append(element("span", `punkt ${p.anmeldung}`));
    const stand = { keine: "keine Anmeldung nötig", lokal: "Anmeldedaten vorhanden", angemeldet: "angemeldet", abgemeldet: "nicht angemeldet", unbekannt: "Anmeldung unbekannt", fehlt: "nicht installiert" }[p.anmeldung] || p.anmeldung;
    zeile.append(`${p.version || "–"} · ${stand}`);
    karte.append(zeile);
    if (p.meldung) karte.append(element("div", "leise", p.meldung));
    const aktionen = element("div", "aktionen");
    if (p.anmeldbar === false) { ziel.append(karte); continue; }
    const knopf = element("button", "", "Anmelden");
    knopf.addEventListener("click", async () => { const { name } = await api("/api/anmeldung", { cli: schluessel }); verbinde(name, `Anmeldung ${p.name}`); });
    aktionen.append(knopf);
    karte.append(aktionen);
    ziel.append(karte);
  }
  const speicher = d.speicher && d.speicher.gesamt_mb ? `Speicher frei: ${(d.speicher.frei_mb / 1024).toFixed(1)} von ${(d.speicher.gesamt_mb / 1024).toFixed(1)} GB. ` : "";
  const repo = d.repo && d.repo.ok === false ? ` Repo-Abgleich gescheitert (${d.repo.zeit}): ${d.repo.meldung}` : "";
  $("system-info").textContent = `${speicher}${repo} Geprüft: ${d.geprueft || "noch nie"}. PowerShell: ${d.powershell || "nicht installiert"}. OpenLauncher Server ${d.version || ""}`;
}
$("update").addEventListener("click", async () => { const { name } = await api("/api/aktualisieren", {}); verbinde(name, "CLI-Updates"); });

// Fehlgeschlagene Knöpfe und Abrufe sichtbar machen, statt dass beim Tippen nichts geschieht.
window.addEventListener("unhandledrejection", (e) => { const m = $("auslastung"); m.textContent = `Fehler: ${e.reason && e.reason.message || e.reason}`; zeige("sitzungen"); });
designListe("hell");
designListe("dunkel");
$("thema-wahl").value = Thema.wahl();
$("thema-wahl").addEventListener("change", (e) => themaAnwenden(Thema.waehlen(e.target.value)));
setInterval(themaAnwenden, 60000);
ladeSitzungen();
ladeKatalog();
setInterval(() => { if (seite === "sitzungen") ladeSitzungen(); else if (seite === "terminal") ladeLinks(); else if (seite === "system") ladeSystem(); }, 4000);
