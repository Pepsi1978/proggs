# Bekannte Bugs ZUERST lesen, bevor an einer Technologie gearbeitet wird (KRITISCH)

> Dauerhafte Regel vom Benutzer gesetzt am 2026-06-01.
> Ausloeser: Chrome-Extension-Verschwind-Bug — ~1h verschwendet, weil der bekannte
> Workaround (Ordner wechseln) nicht vorher nachgeschlagen wurde. Frank: "Wenn du
> vor der Arbeit an Chrome-Erweiterungen immer erst alle bekannten Bugs durchliest,
> waere der Fehler nie aufgefallen." Poka-Yoke Stufe 3: Fehler gar nicht erst machen.
>
> Vollstaendige Systembeschreibung: `~/proggs/bugs/SYSTEM.md`. Index: `~/proggs/bugs/README.md`.

---

## Grundregel

Es gibt pro Technologie-Bereich einen **kuratierten Bug-Almanach** unter:

```
~/proggs/bugs/<kategorie>/<bereich>.md
```

Das Inhaltsverzeichnis aller Almanache (sortiert nach Plattform, mit Erkennungs-Triggern)
liegt in `~/proggs/bugs/README.md`.

**BEVOR** mit einem Bereich gearbeitet wird, fuer den ein Almanach existiert, MUSS
dieser ZUERST komplett gelesen werden — als Pflicht-Vorstufe, noch vor der ersten
Code-Aenderung. So werden bekannte Fehler gar nicht erst gemacht, statt sie
hinterher zu debuggen.

Das ist der Unterschied zu `bug-cases.jsonl`: jene wird REAKTIV nach einem Fehler
durchsucht; der Almanach wird PROAKTIV vor der Arbeit gelesen.

---

## Drei Automatik-Schichten (sorgen dafuer, dass es in JEDER Session laeuft)

| Schicht | Mechanismus | Wann |
|---------|-------------|------|
| 1 — Praesenz | `bug-almanac-index` Hook (SessionStart) blendet die Almanach-Liste ein | jeder Session-Start |
| 2 — Erzwingung | `bug-almanac-guard` Hook (PreToolUse Edit/Write) BLOCKIERT bereichstypische Edits. **Almanach vorhanden:** bis ZUERST der Almanach UND DANN (falls vorhanden) die Best-Practices-Datei gelesen wurde. **KEIN Almanach (seit 2026-06-07):** bis eine bewusste Quittung gesetzt ist (siehe unten) — der "neuer Bereich"-Trigger ist damit nicht mehr zahnlos. Erkennt auch komplett neue Sprachen generisch (`.rs/.go/.rb/.java/.php/.lua/.c/.cpp/.dart/.vue/.svelte/.ex/.clj/.scala/.hs/.zig/.nim/.pl/.groovy`) | bei Datei-Beruehrung |
| 2b — Fehler-Bruecke | `bug-case-auto-writer` Hook (PostToolUseFailure) verweist bei einem neuen Fehler zusaetzlich auf den passenden Almanach und stoesst bei hartnaeckigem Fehler ohne Almanach eine Recherche an | bei jedem Tool-Fehler |
| 3 — Verhalten | diese Regel | immer geladen |

---

## Pflicht-Ablauf

1. Bereich (und, falls relevant, Software-Version) der anstehenden Aufgabe erkennen.
2. Greift es ueberhaupt? Bei trivialem Kleinkram (einzelner String, Doku, Kommentar,
   Versions-Bump) NEIN — weiter ohne Almanach. Sonst:
3. Im Index (`bugs/README.md`) pruefen, ob ein Almanach fuer den Bereich existiert.
4. **Almanach vorhanden** → komplett lesen, das `Versionen:`-Feld pro Bug gegen die
   aktuell benutzte Version abgleichen (Version live ermitteln, z.B. `chrome --version`,
   `./gradlew --version`). Arbeite ich mit einer neueren Version als im Stand-Header
   dokumentiert → Frank melden, OK fuer einen kurzen Re-Check einholen. **Dann — noch vor
   der ersten Code-Aenderung — die zugehoerige Best-Practices-Datei lesen (zweite Seite,
   siehe unten), DANN mit dem Wissen arbeiten.**
5. **Kein Almanach** → der Guard BLOCKIERT jetzt den Edit (seit 2026-06-07, Quittungs-Mechanismus
   — frueher nur ein leicht zu uebersehender Hinweis). Reaktion: Frank melden ("neuer Bereich X,
   kein Almanach"), auf seine Entscheidung warten, dann EINEN der beiden Wege gehen:
   - **(a) Recherche** (Standard, wenn es echte Bereichsarbeit ist): nach Franks **OK** den Skill
     `bug-almanach-recherche` STARTEN — NICHT selbst ad hoc recherchieren. Der Skill ist der
     vorgeschriebene, vollstaendige Weg (Version live ermitteln → Researcher-Schwarm → Fix-Status →
     Best-Practices-Abgleich (lesen+schreiben) → Almanach + `README.md` + Hook-Mapping + Commit).
     Eine Ad-hoc-Recherche von Hand ueberspringt diese Schritte und ist deshalb unvollstaendig.
   - **(b) Quittung** (nur bei trivialem Kleinkram ODER wenn Frank gegen eine Recherche entscheidet):
     die leere Datei `bug-almanac-ack-<slug>.flag` im TEMP-Verzeichnis anlegen (der Block-Text nennt
     den exakten Pfad). Das ist eine **bewusste** Geste nach dem Gespraech mit Frank — niemals
     reflexhaft, nur um den Block loszuwerden. Danach ist der Bereich fuer die Session frei.
   Notaus bei echtem Fehlalarm des Guards: leere Datei `bug-almanac-disable.flag` im TEMP.
6. Tritt waehrend der Arbeit ein Fehler auf → ZUERST pruefen, ob es ein bekannter Bug
   aus dem Almanach ist → dokumentierte Loesung sofort anwenden (schnellster Pfad).
   Der `bug-case-auto-writer`-Hook erinnert bei einem neuen Fehler aktiv daran (Schicht 2b,
   "ALMANACH-BRUECKE"-Hinweis). Ist der Fehler **hartnaeckig** (taucht >=2x auf) UND existiert
   fuer den Bereich noch KEIN Almanach: das ist ein starkes Signal fuer eine Recherche —
   Frank kurz um OK fragen und dann `bug-almanach-recherche` starten (statt weiter zu raten;
   deckt sich mit der Entropie-Reduktions-Regel).
7. **Nach** der Aufgabe: jeden NEU erlebten Bug als Eintrag im Almanach ergaenzen
   (Bug + Loesung + Versionen, Stand-Header aktualisieren). Compound Intelligence —
   der Almanach waechst.

Wichtig: Das "erst Franks OK" gilt NUR fuer die gezielte Almanach-Recherche
(Researcher-Schwarm fuer einen neuen Bereich / Re-Check). Ein normales kurzes
Web-Lookup mitten im Debuggen einer laufenden Aufgabe bleibt frei.

---

## Zwei Seiten einer Medaille: erst Almanach, dann Best Practices

Zu (fast) jedem Almanach `bugs/<kategorie>/<bereich>.md` gibt es eine Best-Practices-Datei
`best-practices/projekt-code/<kategorie>/best-practices-<bereich>.md`. Der Almanach sagt *was
schiefgeht und wie man es loest*; die Best-Practices sagen *wie man es von vornherein richtig
macht, damit der Bug gar nicht erst entsteht*. **Beide werden VOR der Arbeit gelesen — in dieser
Reihenfolge: erst Almanach, dann Best Practices, dann coden.**

Der `bug-almanac-guard` erzwingt genau diese Reihenfolge: er blockiert bereichstypische Edits, bis
ZUERST der Almanach UND DANN (falls vorhanden) die Best-Practices-Datei in dieser Session per Read
geoeffnet wurde (jedes Lesen gibt frei, gilt pro Bereich 1x/Session). Existiert keine
Best-Practices-Datei fuer den Bereich, zaehlt nur der Almanach. Notaus bei Fehlalarm:
leere Datei `bug-almanac-disable.flag` im TEMP-Verzeichnis anlegen.

---

## Gilt AUCH fuer Claude-eigene Harness-Arbeit (KRITISCH — leicht vergessen)

Diese Regel betrifft nicht nur Projekt-Code, sondern **auch jede Aenderung am eigenen
Werkzeugkasten (Harness)**: Hooks, Regeln, `settings.json`, Skills, Commands, Agents,
MCP-Server. Wenn ich selbst eine Regel oder einen Hook baue/aendere, MUSS ich ZUERST den
passenden Almanach lesen und sein Wissen nutzen — nicht aus dem Gedaechtnis arbeiten:

| Ich aendere … | Zuerst lesen |
|---------------|--------------|
| Einen Hook (`*.ps1`/`*.sh`) | `bugs/claude-tooling/claude-hooks.md` |
| CLAUDE.md, Regel, Settings, Skill, Command, Agent | `bugs/claude-tooling/claude-config.md` + `best-practices/projekt-code/claude-tooling/best-practices-claude-config.md` |
| Einen MCP-Server | `bugs/claude-tooling/mcp-server.md` |
| Ein Python-Hilfsskript | `bugs/claude-tooling/python-windows.md` |

Der `bug-almanac-guard` erzwingt das ohnehin (er blockiert Edits an `/hooks/*`, `/rules/*.md`,
`settings.json` etc.), aber das Prinzip gilt bewusst: das dokumentierte Bug-Wissen verhindert,
dass beim Bauen der eigenen Werkzeuge dieselben Fallen erneut auftreten (z.B. `exit 2` blockt
Write/Edit nicht → `permissionDecision:deny`; kein `jq` in `.sh`; `additionalContext` nur nested).

## Wann diese Regel greift (Beispiele)

| Arbeit an … | Almanach |
|-------------|----------|
| Chrome-/Browser-Erweiterung (manifest.json, content scripts, service worker, chrome.storage, Overlays) | `~/proggs/bugs/web/chrome-extensions.md` |
| Eigene Hooks / Regeln / Settings / Skills / Agents / MCP (Harness) | `~/proggs/bugs/claude-tooling/*.md` (+ Best-Practices) |
| Komplett neue Programmiersprache (erste `.rs`/`.go`/`.rb`/`.java`/`.cpp`/… Datei) | noch keiner — Guard blockt generisch, dann Recherche oder Quittung |
| Weitere Bereiche (Android, WPF, Swift, TS, Tampermonkey, Hooks, Gradle …) | `~/proggs/bugs/<kategorie>/<bereich>.md` (bei erster echter Arbeit anlegen, siehe README) |

Erkennungssignale fuer "Chrome-Extension-Arbeit": eine `manifest.json` mit
`manifest_version`, Dateien in einem Extension-/Overlay-Ordner, `chrome.*`-APIs,
`*.user.js`, oder der Nutzer nennt Erweiterung/Extension/Overlay-Plugin.

---

## Zusammenspiel

| System | Rolle |
|--------|-------|
| `~/proggs/bugs/*.md` + `README.md` + `SYSTEM.md` | Proaktiver Almanach pro Bereich (VOR der Arbeit): was schiefgeht + Loesung |
| `~/proggs/best-practices/projekt-code/**` | Proaktive Best-Practices pro Bereich (zweite Seite, ebenfalls VOR der Arbeit gelesen): wie man es richtig macht |
| `bug-almanac-index` / `bug-almanac-guard` Hooks | Automatik: Liste einblenden / Almanach + Best-Practices erzwingen |
| `bug-cases.jsonl` | Reaktive Fall-Datenbank mit Auto-Match (NACH einem Fehler) |
| Direktive #3 (Resilient Bugfixing) | Jeder neue Bug → Almanach-Eintrag + bug-case; Loesungen IMMER funktionserhaltend |
| Direktive #1 (Superintelligenz/Harness) | Der Almanach ist Harness-Wissen, das jede Session erbt und mitwaechst |

---

## Was NIEMALS passieren darf

- ❌ An einem Bereich arbeiten, fuer den ein Almanach existiert, ohne ihn vorher zu lesen
- ❌ Den Almanach lesen, aber die zugehoerige Best-Practices-Datei ueberspringen — beide gehoeren VOR die Arbeit (erst Almanach, dann Best Practices)
- ❌ Einen erlebten Bug fixen, ohne ihn anschliessend im passenden Almanach zu ergaenzen
- ❌ Den Almanach als "nice to have" behandeln — das Vorab-Lesen ist Pflicht
- ❌ Eine gezielte Almanach-Recherche ohne Franks OK starten
- ❌ Bei einem neuen Bereich selbst ad hoc recherchieren, statt den Skill `bug-almanach-recherche`
  zu starten — der Skill ist der vorgeschriebene, vollstaendige Weg (sonst fehlen Fix-Status,
  Best-Practices-Abgleich, Hook-Mapping etc.)
- ❌ Die Quittung (`bug-almanac-ack-<slug>.flag`) reflexhaft setzen, nur um den Block loszuwerden —
  sie ist eine BEWUSSTE Geste NACH Franks Entscheidung (Recherche oder bewusst verzichten)
- ❌ Den Block bei einem neuen/generisch erkannten Bereich ignorieren/umgehen, statt sich zu
  entscheiden (Recherche mit Franks OK ODER Quittung bei Kleinkram)
- ❌ Eine eigene Regel/Hook/Config bauen, ohne ZUERST den passenden `claude-tooling`-Almanach
  (+ Best-Practices) zu lesen — auch der Harness faellt unter diese Regel
- ❌ Einen Bug "loesen", indem Funktionalitaet entfernt wird (Direktive #3 — funktionserhaltend)
