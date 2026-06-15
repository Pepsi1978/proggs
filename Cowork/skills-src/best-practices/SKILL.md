---
name: best-practices
description: "Recherchiert und pflegt Best Practices fuer Harness-Werkzeuge und Projekt-Software (Kotlin, Swift, Gradle), speichert sie im Arbeitsordner. Trigger: Best-Practices recherchieren, was ist neu in X."
---

# Best-Practices (Cowork-Fassung) — Harness- & Projekt-Wissen aktuell halten

Diese Cowork-Fassung recherchiert und pflegt Best Practices in zwei Bereichen und speichert sie
dauerhaft im Arbeitsordner. Sie läuft in der **Claude-Cowork-Desktop-App** und ist auf deren
Umgebung zugeschnitten. Sie ändert nie ein Werkzeug — sie recherchiert, speichert und schlägt vor.

---

## 0. ZUERST LESEN — Ablage-Ort & Ordner anlegen (Cowork)

**Alle Ergebnisse werden RELATIV im aktuell verbundenen Arbeitsordner gespeichert** (üblicherweise
der gemountete `proggs`-Ordner) — NICHT in einen fest verdrahteten `~/proggs`-Pfad. Struktur relativ
zum Arbeitsordner:

```
best-practices/
├── README.md              ← Inhaltsverzeichnis
├── _state.json            ← {"last_version": null, "last_checked": null}
├── _changelog-archiv.md   ← VOLLSTAENDIGER Claude-Code-Changelog, verbatim
├── 01-hooks/ … 12-neues/best-practices.md     ← Harness-Kategorien
└── projekt-code/<kategorie>/best-practices-<software>.md   ← Projekt-Software
```

**Ordner-anlegen ist Pflicht und erlaubt:** Fehlt die Struktur oder ein Kategorie-Ordner → ERST
anlegen (Datei-Werkzeug bzw. `mkdir -p`, falls Shell verfügbar), DANN schreiben. NIEMALS abbrechen,
weil ein Ordner fehlt. Nennt der Benutzer einen anderen Basis-Ordner, dort hinein (gleiche Struktur).

## 0a. Cowork-Umgebung — Schreib- & Git-Fallen (PFLICHT beachten)

> Volltext: `bugs/claude-tooling/cowork.md` + `bugs/claude-tooling/cowork-git-push.md` im Arbeitsordner.

- **Mount-Schreibfalle:** Die Cowork-Mount-Brücke kann das **Dateiende abschneiden**. Nach JEDEM
  Schreiben das Dateiende prüfen (`tail -1`, `wc -l`). Besonders kritisch für das große
  `_changelog-archiv.md` — lieber das Script (unten) nutzen statt von Hand zu schreiben.
- **~45s-Shell-Limit:** Ein Cowork-Shell-Aufruf läuft max ~45 Sekunden; Hintergrundprozesse
  überleben den Wechsel zwischen Aufrufen NICHT. Researcher laufen als **Agenten** (unkritisch);
  jeder Git-/Script-Schritt muss in EINEM Aufruf durchlaufen.
- **Git NIEMALS nackt:** Aus Cowork IMMER über `bash ~/proggs/cowork-git.sh` committen/pushen
  (fängt Mount-Fallen + Datenverlust-Wächter ab). NIE direktes `git commit`/`git push`.

---

## Zwei Bereiche, ein Ordner

1. **Harness** (Kategorien `01-hooks` … `12-neues`): die Claude-Code-Werkzeuge. Quelle ist der
   offizielle **Claude-Code**-Changelog (via `scripts/update-changelog`).
2. **Projekt-Code** (`projekt-code/<kategorie>/best-practices-<software>.md`): Kotlin, Swift, Gradle,
   .NET/WPF, TypeScript, Rust … Quelle ist der **eigene** Changelog der jeweiligen Software (KEIN
   Claude-Script); die installierte Version wird live ermittelt und ist der Versions-Anker. Das ist
   die zweite Seite der Medaille zum Bug-Almanach (`bugs/`): dort *was schiefgeht*, hier *wie man es
   von vornherein richtig macht*.

## Fokus auf einen Bereich (statt Volllauf)
Standard: ein Lauf deckt alle Kategorien ab. Einschränkbar auf EINEN Bereich — "nur für Kotlin",
"nur für Hooks", "nur für Chrome-Erweiterungen". Das nutzt u.a. die `bug-almanach-recherche`, wenn
sie gezielt eine Software aufrollen lässt.

## Ablauf eines Laufs
1. **Stand lesen:** `best-practices/_state.json` + `README.md` → welche Version/welches Datum zuletzt?
2. **Changelog verbatim holen + Delta:** Den KOMPLETTEN offiziellen Changelog wortwörtlich holen
   (Script, siehe unten) und mit `last_version` vergleichen → neue Versionen erkennen. (Volllauf
   gewünscht → Delta-Schritt überspringen, alle Kategorien neu aufrollen.)
3. **Nichts Relevantes neu?** → "Nichts Neues seit Version X (Stand: Datum)" melden, fertig.
4. **Delta vorhanden?** → parallele Researcher (Regeln unten). Jeder Researcher recherchiert pro
   Kategorie: **WAS** hat sich geändert (offizielles Changelog), **WIE** wendet man es am besten an
   (Anthropic-Docs/Blog), **Alternativen** von außen (`extern` gelabelt, sekundär). Was in keine
   Kategorie passt → Kategorie 12 (Neues, bleibt immer die letzte).
5. **Speichern:** Kategorie-`best-practices.md` (jeder Eintrag mit Quelle + Datum + `offiziell`/
   `extern`-Flag), `_changelog-archiv.md` inkrementell, `README.md` + `_state.json` aktualisieren.
   Bei **Projekt-Code**-Läufen zusätzlich die **Bug-Almanach-Rückkopplung** (siehe unten).
6. **Auswertung ausgeben** (Format unten).

## Changelog-Archiv — vollständig & verbatim (KRITISCH)
`_changelog-archiv.md` ist die Rohdaten-Grundlage — eine Zusammenfassung ist wertlos. Strikt:
- **Verbatim, ungekürzt:** kompletter offizieller Changelog JEDER Version, alle Bullets wortwörtlich.
- **Kanonische Quelle:** `https://raw.githubusercontent.com/anthropics/claude-code/main/CHANGELOG.md`.
- **Download-Methode:** Direkter Datei-Download — NIEMALS WebFetch/Researcher (die fassen zusammen
  und zerstören die Vollständigkeit).
- **Datum je Version:** aus den npm-Publish-Zeitstempeln (`@anthropic-ai/claude-code`, Feld `time`).
  Format `## X.Y.Z — YYYY-MM-DD`.

### Ausführung: das Script (nicht von Hand nachbauen!)
Die komplette Mechanik (Download + npm-Datum + Erstlauf/inkrementell + Verifikation) steckt in einem
deterministischen Script unter `scripts/`. NICHT als Prosa rekonstruieren — aufrufen.

> **Cowork-Delta (WICHTIG):** Der Default-Zielordner (`DataDir`) der Scripts ist auf den
> **aktuellen Arbeitsordner relativ** gesetzt (`./best-practices`), NICHT auf einen festen
> `~/proggs`-Pfad — sonst schreibt das Script am verbundenen Ordner vorbei. Anderen Zielordner
> per Flag übergeben.

| Plattform | Befehl (im verbundenen Arbeitsordner) |
|-----------|----------------------------------------|
| macOS/Linux (Cowork-VM) | `bash scripts/update-changelog.sh` |
| Windows | `pwsh scripts/update-changelog.ps1` |
| Anderer Zielordner | `… --data-dir <pfad>` (sh) bzw. `-DataDir <pfad>` (ps1) |

- **Ohne Flag = inkrementell:** nur Versionen neuer als `_state.json.last_version` werden oben
  angehängt; alte Einträge + Hand-Notizen bleiben unangetastet.
- **Mit `--first-run` (sh) / `-FirstRun` (ps1):** kompletter Neu-Aufbau (Erstlauf/Reparatur).
- Nach dem Lauf das **Dateiende** von `_changelog-archiv.md` prüfen (`tail -1`, `wc -l`) — Mount-Truncation-Schutz.

## Taxonomie (12 Kategorien, selbst-erweiternd)
1 Hooks · 2 Skills · 3 Agents · 4 Plugins · 5 MCP-Server · 6 Slash-Commands · 7 Settings & Konfig ·
8 Kontext-Management · 9 Token-/Kosten-Effizienz · 10 Arbeitsweise/Verhalten · 11 Researcher &
Internet-Recherche · 12 Neues/Horizont-Scan. **"Neues" bleibt IMMER die letzte Kategorie**; kommt eine
neue definierte Kategorie dazu, wird sie VOR "Neues" eingefügt und "Neues" rückt eine Nummer nach hinten.

### Projekt-Code-Sektion (`projekt-code/<kategorie>/best-practices-<software>.md`)
Nach **Kategorie** gruppiert (android, android-build, desktop, web, peripherie, claude-tooling …),
je eine selbst-identifizierende Datei direkt im Kategorie-Ordner (z.B. `android/best-practices-kotlin.md`;
KEIN Software-Unterordner). Mechanik-Unterschied: Changelog-Quelle ist der **eigene** Changelog der
Software (Kotlin bei JetBrains, Swift bei Apple, Gradle …), NICHT das Claude-Script. Versions-Anker =
LIVE ermittelte installierte Version (`kotlinc -version`, `swift --version`, `./gradlew --version`,
`dotnet --version` …).

## Kopplung zum Bug-Almanach (beide Richtungen — PFLICHT bei Projekt-Code)
- **A — Bug-Fund zurückschreiben:** Fördert die Recherche einen echten BUG zutage (nicht nur eine
  positive Empfehlung) → in `bugs/<kategorie>/<bereich>.md` als Eintrag ergänzen (Format: Titel /
  Symptom / Ursache / Versionen / FIX / Quelle), gegen bestehende DEDUPLIZIEREN, Stand-Header aktuell.
  **Existiert KEIN Almanach** → NICHT im Vorbeigehen einen halben anlegen (ihm fehlt die gh-Fix-Status-
  Prüfung); dem Benutzer melden und die `bug-almanach-recherche` vorschlagen (erst sein OK), die Bugs
  kompakt mitliefern.
- **B — Bezugs-Tabellen synchron halten:** Existieren BEIDE Dateien, in jeder eine wechselseitige
  Abschnitts-Bezugs-Tabelle „Best-Practice-Abschnitt ↔ Bug-Abschnitt" pflegen.

## Researcher-Regeln (KRITISCH — Absturz-Schutz)
- **Modell:** höchstes Opus (1M), `opts.model` NICHT setzen. **Agent-Typ:** `researcher` (läuft als
  Agent, nicht als Shell-Hintergrund → vom ~45s-Cowork-Limit unberührt).
- **7 GLEICHZEITIG, dann Continuous-Spawning:** mit 7 auf einmal beginnen (nicht erst 4, dann 3); wird
  einer fertig → sofort den nächsten starten, bis alle Themen abgedeckt sind. Konstant 7. Empirisch:
  5 sicher, 7 läuft, 12 → Absturz. Obergrenze ~7 (RPM-Limit, NICHT Kontextfenster).
- **KEIN Findings-Cap:** ALLE Funde dokumentieren (Opus 1M; Kappen wäre lossy). Sehr viele → verlustfrei
  in Kategorie-Datei + kompakte Summary.
- **429-Backoff (PFLICHT):** bei Rate-Limit-Absturz sofort melden + mit exponential backoff neu starten
  (`retry-after` beachten), nie still aufgeben.
- **Scope:** ~15 Websuchen/Fetches, ~10 Min pro Researcher (begrenzt die ANFRAGE-Rate, nicht die Findings).
- **Header-Format erzwingen:** jede Kategorie-Datei beginnt mit
  `# [Kategorie] — Best Practices (Stand JJJJ-MM-TT, Claude Code X.Y.Z)` — im Prompt verlangen UND
  nach dem Lauf prüfen. Keine `_writeback.json`-Artefakte im Ordner lassen.

## Quellen-Rangordnung
1. Offizielle Quellen (Claude-Code-Changelog, code.claude.com/docs, Anthropic-Engineering-Blog) =
   **Grundwahrheit**. 2. Externe/Community (Blogs, Profis, GitHub-Diskussionen) = abwägbare
   **Alternative**, klar als `extern` gelabelt — überstimmt NIE das Offizielle. Jeder Eintrag: Quelle + Datum.

## Auswertungs-Format (Schritt 6)
- **Teil A — Ausführliche Auswertung je Kategorie:** Neuerungen (Version + Quelle) · Best Practices
  (wie nutzt man es heute am besten) · Betrifft eigene Werkzeuge? (Hinweis).
- **Teil B — Kurz-Header:** `## Best-Practices-Lauf — [Datum] | Version [alt]→[neu] | Kategorien: N | Quellen: N`.
- **Teil C — UMSETZBARE VERBESSERUNGSVORSCHLÄGE (Herzstück, PFLICHT):** nummerierte Liste konkreter,
  sofort abnickbarer Vorschläge — NUR research-gestützt mit belegtem Vorteil. Pro Vorschlag:
  `Aktion · Vorteil (belegt) + Quelle · Betrifft/Aufwand/Risiko`. **Umgebungs-Gegencheck VOR dem Listen:**
  (1) Existieren die betroffenen Dateien? (2) Nutzt das Werkzeug das Feature wirklich? (3) Echter Nutzen
  in GENAU dieser Umgebung? Nur Vorschläge, die alle drei bestehen. Lieber 3 starke als 10 schwache.
  Danach auf die Auswahl warten und NUR Bestätigtes umsetzen.

## W3-Mechanismen (seit 2026-06-15 — beim Speichern befolgen)
- **Versions-Anker (W3-1):** Wird ein SOFTWARE-gebundener Almanach neu angelegt/zurückgekoppelt, trägt
  er das Feld `> **Anker:** <label>=<version>` (unter dem Stand-Header) + ggf. einen
  `bugs/check-version-anchor.py`-Eintrag (Details: Skill `bug-almanach-recherche`, Schritt 6).
- **Self-Test (PFLICHT vor Commit, falls Shell + Python verfügbar):** `python bugs/health.py` — alle
  VIER Checks (coupling, guard-coverage, version-anchor, Stand-Verfall) müssen grün sein. Die
  coupling-Prüfung fängt fehlende/asymmetrische Bezugs-Tabellen sofort. Kein Python in Cowork? → die
  Bezugs-Tabellen + Header manuell gegenprüfen, ehrlich vermerken.

## Sichern (Cowork-Git)
Git-Repo verbunden → committen + pushen über das Cowork-Skript (nur die eigenen Pfade namentlich):
```bash
bash ~/proggs/cowork-git.sh setup                 # warten auf "Push-Zugang OK"
bash ~/proggs/cowork-git.sh push-files "#NNN - best-practices <bereich>: recherchiert + W3" \
  best-practices/<...>/best-practices.md best-practices/_changelog-archiv.md best-practices/_state.json
```
Kein Git-Repo → nur speichern und dem Benutzer den Ablage-Pfad nennen.

## Was NIEMALS passieren darf
- Aus Cowork mit nacktem `git commit`/`git push` arbeiten (immer `cowork-git.sh`).
- `_changelog-archiv.md` per WebFetch/Researcher zusammenfassen lassen (zerstört die Vollständigkeit) —
  immer das Script + Dateiende prüfen.
- Eine externe Behauptung als offiziell darstellen; Vorschläge ohne belegten Vorteil listen (Teil C).
- Ein eigenes Hook/Skill/Agent/MCP/Setting ändern (dieser Skill schreibt nur Wissensdateien + Almanach-Rückkopplung).
- Bei Projekt-Code die Bug-Almanach-Rückkopplung oder die Bezugs-Tabellen vergessen.

## Referenzen
- `scripts/update-changelog.{sh,ps1}` — holt den Claude-Code-Changelog verbatim (Cowork-DataDir-Default).
- `bugs/SYSTEM.md`, `bugs/health.py` — Almanach-System + Self-Test im Arbeitsordner.
- Gegenstück: Skill `bug-almanach-recherche` (was schiefgeht ↔ wie man es richtig macht).
