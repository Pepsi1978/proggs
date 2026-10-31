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

// ---- Reiter
let seite = "sitzungen";
function zeige(name) {
  seite = name;
  document.querySelectorAll("#reiter button").forEach((b) => b.classList.toggle("an", b.dataset.seite === name));
  document.querySelectorAll(".seite").forEach((s) => s.classList.toggle("an", s.id === name));
  if (name === "terminal") { setTimeout(passen, 30); }
  if (name === "sitzungen") ladeSitzungen();
  if (name === "system") ladeSystem();
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
  term = new Terminal({ fontSize: 13, scrollback: 5000, cursorBlink: true, convertEol: false, theme: { background: "#000000" } });
  fit = new FitAddon.FitAddon();
  term.loadAddon(fit);
  term.open($("xterm"));
  term.onData((d) => schicke(d));
  new ResizeObserver(() => passen()).observe($("xterm"));
  wischenEinrichten();
}
// Mit dem Finger scrollen: tmux hält den Verlauf, das Terminal auf dem Handy nicht. Wischen wird zu Zeilen, ein Schwung läuft kurz nach.
// Die Fläche liegt über dem Terminal: Es ersetzt beim Neuzeichnen die Zeile unter dem Finger, und danach kämen die Berührungen nirgends mehr an.
let offeneZeilen = 0, rollZeit = null;
function rollen(zeilen) {
  offeneZeilen += zeilen;
  if (rollZeit) return;
  rollZeit = setTimeout(() => {
    rollZeit = null;
    if (offeneZeilen && ws && ws.readyState === 1) ws.send(JSON.stringify({ t: "s", n: offeneZeilen }));
    offeneZeilen = 0;
  }, 50);
}
function wischenEinrichten() {
  const flaeche = $("xterm");
  const zeilenHoehe = () => Math.max(8, flaeche.clientHeight / (term ? term.rows : 24));
  flaeche.addEventListener("wheel", (e) => { e.preventDefault(); e.stopPropagation(); rollen(-Math.sign(e.deltaY) * 3); }, { capture: true, passive: false });
  $("ende").addEventListener("click", () => { if (ws && ws.readyState === 1) ws.send(JSON.stringify({ t: "e" })); });
  if (!("ontouchstart" in window)) return;
  const decke = element("div", "wisch");
  flaeche.append(decke);
  let y = null, rest = 0, tempo = 0, zuletzt = 0, weg = 0, schwung = null;
  decke.addEventListener("touchstart", (e) => { clearInterval(schwung); y = e.touches[0].clientY; rest = 0; tempo = 0; weg = 0; zuletzt = e.timeStamp; }, { passive: true });
  decke.addEventListener("touchmove", (e) => {
    if (y === null) return;
    e.preventDefault();
    const neu = e.touches[0].clientY, schritt = neu - y;
    if (e.timeStamp > zuletzt) tempo = schritt / (e.timeStamp - zuletzt);
    zuletzt = e.timeStamp; y = neu; rest += schritt; weg += Math.abs(schritt);
    const zeilen = Math.trunc(rest / zeilenHoehe());
    if (zeilen) { rest -= zeilen * zeilenHoehe(); rollen(zeilen); }
  }, { passive: false });
  decke.addEventListener("touchend", (e) => {
    y = null;
    // Ein kurzer Tipper öffnet wie gewohnt die Tastatur des Terminals.
    if (weg < 10) { e.preventDefault(); if (term) term.focus(); return; }
    if (Math.abs(tempo) < 0.4) return;
    schwung = setInterval(() => {
      tempo *= 0.8;
      const zeilen = Math.trunc(tempo * 60 / zeilenHoehe());
      if (!zeilen) { clearInterval(schwung); return; }
      rollen(zeilen);
    }, 60);
  });
  decke.addEventListener("touchcancel", () => { y = null; }, { passive: true });
}
function schicke(d) { if (ws && ws.readyState === 1) ws.send(JSON.stringify({ t: "i", d })); }
function passen() {
  if (!term || seite !== "terminal") return;
  try { fit.fit(); } catch { return; }
  if (ws && ws.readyState === 1) ws.send(JSON.stringify({ t: "r", c: term.cols, r: term.rows }));
}
function trennen() {
  aktuell = null;
  clearTimeout(neuVersuch);
  if (ws) { ws.onclose = null; ws.close(); ws = null; }
  $("titel").textContent = "Keine Sitzung gewählt";
  $("schliessen").hidden = true;
  $("links").replaceChildren();
  if (term) term.reset();
}
function verbinde(name, titel) {
  aufbauen();
  if (aktuell !== name) { trennen(); aktuell = name; }
  $("titel").textContent = titel || name;
  $("schliessen").hidden = false;
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
  ws.onopen = () => passen();
  ws.onmessage = (e) => term.write(new Uint8Array(e.data));
  // Abriss (Mobilfunk, Tunnel): wieder anhängen. Die Sitzung läuft in tmux weiter.
  ws.onclose = (e) => {
    if (aktuell !== name) return;
    if (e.code === 4404) { trennen(); $("titel").textContent = "Diese Sitzung ist beendet."; ladeSitzungen(); return; }
    neuVersuch = setTimeout(aufmachen, 2000);
  };
}
zweimal($("schliessen"), () => { if (aktuell) { const n = aktuell; $("schliessen").textContent = "Sitzung schließen"; schliesse(n); zeige("sitzungen"); } });
document.querySelectorAll("#tasten button[data-t]").forEach((b) => b.addEventListener("click", () => schicke(b.dataset.t)));
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
let angebot = { modelle: [], efforts: {} };
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
  const efforts = angebot.efforts[$("cli").value] || [];
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
    const stand = { lokal: "Anmeldedaten vorhanden", angemeldet: "angemeldet", abgemeldet: "nicht angemeldet", unbekannt: "Anmeldung unbekannt", fehlt: "nicht installiert" }[p.anmeldung] || p.anmeldung;
    zeile.append(`${p.version || "–"} · ${stand}`);
    karte.append(zeile);
    if (p.meldung) karte.append(element("div", "leise", p.meldung));
    const aktionen = element("div", "aktionen");
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
ladeSitzungen();
ladeKatalog();
setInterval(() => { if (seite === "sitzungen") ladeSitzungen(); else if (seite === "terminal") ladeLinks(); else if (seite === "system") ladeSystem(); }, 4000);
