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
dessen **Kurzcheck** ZUERST gelesen werden — als Pflicht-Vorstufe, noch vor der ersten
Code-Aenderung (Digest-Modell, siehe unten). So werden bekannte Fehler gar nicht erst
gemacht, statt sie hinterher zu debuggen.

Das ist der Unterschied zu `bug-cases.jsonl`: jene wird REAKTIV nach einem Fehler
durchsucht; der Almanach wird PROAKTIV vor der Arbeit gelesen.

---

## Das Digest-Modell: 3 Stufen (seit 2026-06-10)

Jeder Almanach und jede Best-Practices-Datei traegt oben eine kompakte
**"Kurzcheck"-Sektion** (Erkennungssignale + Sofort-Regeln als Tabelle, vollstaendig
innerhalb der ersten 80 Zeilen). Wie viel gelesen werden muss, haengt von der Stufe ab:

| Stufe | Wann | Was lesen | Erzwungen durch |
|-------|------|-----------|-----------------|
| **A — Kurzcheck vorab** | vor JEDER echten Arbeit im Bereich | NUR den Kurzcheck: `Read` mit `limit=80` auf den Almanach, danach ebenso auf die Best-Practices-Datei | `bug-almanac-guard` (read-Marker) |
| **B — Volltext bei Fehler** | ab dem ERSTEN Fehler im Bereich | SOFORT den VOLLTEXT des Almanachs (`Read` ohne `limit`) — der Kurzcheck reicht ab jetzt nicht mehr | `bug-case-auto-writer` (Stufe-B-Hinweis bei jedem Tool-Fehler) + Entropie-Reduktions-Regel |
| **C — Volltext bei Hochrisiko** | vor Arbeit in einem Hochrisiko-Bereich | den VOLLTEXT des Almanachs schon VORAB (`Read` ohne `limit`) | `bug-almanac-guard` (full-Marker; ein Read ohne `limit` bzw. mit `limit>=500` setzt ihn) |
| **D — Wiederkehrender Bug** | der Fehler ist SCHON EINMAL aufgetreten/gefixt worden (eigene oder frühere Session) UND tritt jetzt ERNEUT auf | Kurzcheck komplett ÜBERSPRINGEN — SOFORT den VOLLTEXT von Almanach UND Best-Practices-Datei lesen (beide, nicht nur den Almanach). Zeigt der Volltext KEINE passende Lösung: siehe „Recherche bei wiederkehrendem Bug" unten | diese Regel (keine Hook-Erzwingung — Erkennung ist Sache des Modells/Franks) |

**Hochrisiko-Bereiche (Stufe C):** `r8`, `firebase-billing`, `claude-hooks`, `claude-config` —
tickende/teure Fehlerklassen (Release-Crashes, Geld/Abos, Harness-Totalausfall). Die Liste lebt
im `bug-almanac-guard` (`$highRiskKeys` in der .ps1 bzw. `case`-Liste in der .sh) und hier —
beide synchron halten.

**Warum (Stand 2026-06-10):** Der fruehere Volltext-Zwang fuer ALLE Bereiche kostete
~16.000-23.000 Tokens pro Bereich und Session (plus Context-Rot). Der Kurzcheck (~500 Tokens)
erhaelt die Erkennungsfaehigkeit fuer stille Fehler — und die Erkennung ist das Entscheidende.
Verlustfrei nach dem Lossless-Prinzip: Der Volltext bleibt per Pfad jederzeit erreichbar und
wird bei Fehlern (Stufe B) und Hochrisiko (Stufe C) weiterhin erzwungen.

**Warum Stufe D noetig ist (Vorfall 2026-07-01):** Bei einem Bug, der bereits einmal (in dieser
oder einer frueheren Session) diagnostiziert und "gefixt" wurde und jetzt TROTZDEM wieder auftritt,
ist der Kurzcheck per Definition NICHT ausreichend — er hat die Ursache offenbar schon beim letzten
Mal nicht vollstaendig erfasst (sonst waere der Fix stabil gewesen). Direktive #3 (Resilient
Bugfixing) verlangt fuer wiederkehrende Fehler eine TIEFERE Analyse, nicht die gleiche
Kurzcheck-Ebene erneut. Konkreter Vorfall: Ein Almanach-Fix zu "OpenCode committet/pusht nicht
automatisch" wurde bereits einmal versucht (#47319); der Kurzcheck wurde daraufhin erneut nur als
Kurzcheck gelesen statt den Volltext zu pruefen — der wiederkehrende Bug traf trotzdem erneut auf,
weil die tiefere Root-Cause-Analyse fehlte.

### Recherche bei wiederkehrendem Bug ohne Loesung im Volltext (Stufe D, Fortsetzung)

Zeigt der VOLLTEXT von Almanach + Best Practices KEINE passende Loesung fuer den wiederkehrenden
Fehler: das ist ein starkes Signal, dass die Ursache noch nicht dokumentiert ist. In diesem Fall
NICHT einfach weiter im eigenen Wissen raten, sondern das Grundproblem gezielt recherchieren
(ob es dafuer bereits allgemein bekannte Loesungen/Best-Practices gibt) — ueber den `research`-Skill
nach dem Recherche-Protokoll (`research-strategy.md`: Empfehlung + Frage 1 A/B/C/D). Ergebnis danach
sowohl in den Almanach (Bug+Fix) als auch — falls zutreffend — in die Best-Practices-Datei einarbeiten
(`research-persistence.md`), damit der Bug beim naechsten Mal WIRKLICH nicht wiederkehrt.

---

## Drei Automatik-Schichten (sorgen dafuer, dass es in JEDER Session laeuft)

| Schicht | Mechanismus | Wann |
|---------|-------------|------|
| 1 — Praesenz | `bug-almanac-index` Hook (SessionStart) blendet die Almanach-Liste ein | jeder Session-Start |
| 1b — Prompt-Trigger | `bug-almanac-hint` Hook (UserPromptSubmit, seit 2026-06-15) scannt den Prompt auf Bereichs-Stichwoerter und injiziert EINMALIG pro Bereich/Session einen passiven Almanach-Hinweis — faengt Konzept-/Planungsarbeit VOR dem ersten Datei-Edit (kein Block) | bei jedem Prompt |
| 2 — Erzwingung | `bug-almanac-guard` Hook (PreToolUse Edit/Write) BLOCKIERT bereichstypische Edits. **Almanach vorhanden:** bis ZUERST der Almanach-Kurzcheck UND DANN (falls vorhanden) der Best-Practices-Kurzcheck gelesen wurde (Stufe A, `Read` mit `limit=80`); bei Hochrisiko-Bereichen zusaetzlich bis der Almanach-VOLLTEXT gelesen wurde (Stufe C, full-Marker). **KEIN Almanach (seit 2026-06-07):** bis eine bewusste Quittung gesetzt ist (siehe unten) — der "neuer Bereich"-Trigger ist damit nicht mehr zahnlos. Erkennt auch komplett neue Sprachen generisch (`.rs/.go/.rb/.java/.php/.lua/.c/.cpp/.dart/.vue/.svelte/.ex/.clj/.scala/.hs/.zig/.nim/.pl/.groovy`) | bei Datei-Beruehrung |
| 2b — Fehler-Bruecke | `bug-case-auto-writer` Hook (PostToolUseFailure) verweist bei einem neuen Fehler zusaetzlich auf den passenden Almanach und stoesst bei hartnaeckigem Fehler ohne Almanach eine Recherche an | bei jedem Tool-Fehler |
| 3 — Verhalten | diese Regel | immer geladen |

---

## Pflicht-Ablauf

1. Bereich (und, falls relevant, Software-Version) der anstehenden Aufgabe erkennen.
2. Greift es ueberhaupt? Bei trivialem Kleinkram (einzelner String, Doku, Kommentar,
   Versions-Bump) NEIN — weiter ohne Almanach. Sonst:
3. Im Index (`bugs/README.md`) pruefen, ob ein Almanach fuer den Bereich existiert.
4. **Almanach vorhanden** → Stufe waehlen (Digest-Modell, siehe oben):
   - **Normaler Bereich (Stufe A):** NUR den Kurzcheck lesen (`Read` mit `limit=80`) — die
     ersten 80 Zeilen enthalten auch den Stand-Header fuer den Versions-Abgleich (Version live
     ermitteln, z.B. `chrome --version`, `./gradlew --version`). Arbeite ich mit einer neueren
     Version als dokumentiert → Frank melden, OK fuer einen kurzen Re-Check einholen.
   - **Hochrisiko-Bereich (Stufe C: r8, firebase-billing, claude-hooks, claude-config):**
     den VOLLTEXT lesen (`Read` ohne `limit`) + `Versionen:`-Feld pro Bug abgleichen.
   **Dann — noch vor der ersten Code-Aenderung — die zugehoerige Best-Practices-Datei lesen
   (Kurzcheck mit `limit=80` reicht; zweite Seite, siehe unten), DANN mit dem Wissen arbeiten.**
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
6. Tritt waehrend der Arbeit ein Fehler auf → **Stufe B: SOFORT den VOLLTEXT des Almanachs
   lesen** (`Read` ohne `limit`) — ab dem ersten Fehler reicht der Kurzcheck nicht mehr.
   ZUERST pruefen, ob es ein bekannter Bug aus dem Almanach ist → dokumentierte Loesung
   sofort anwenden (schnellster Pfad).
   **Ist der Fehler ERKENNBAR eine WIEDERHOLUNG** (schon einmal in dieser oder einer frueheren
   Session gefixt, tritt jetzt trotzdem wieder auf) → **sofort Stufe D**: Kurzcheck NICHT
   erneut lesen, direkt Volltext von Almanach UND Best-Practices-Datei. Zeigt der Volltext keine
   Loesung → Recherche zum Grundproblem anstossen (siehe Stufe D oben), nicht weiter raten.
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
`best-practices/<kategorie>/<bereich>.md`. Der Almanach sagt *was
schiefgeht und wie man es loest*; die Best-Practices sagen *wie man es von vornherein richtig
macht, damit der Bug gar nicht erst entsteht*. **Beide Kurzchecks werden VOR der Arbeit gelesen —
in dieser Reihenfolge: erst Almanach, dann Best Practices, dann coden (Stufe A; bei
Hochrisiko-Bereichen gilt fuer den Almanach Volltext-Pflicht, Stufe C).**

Der `bug-almanac-guard` erzwingt genau diese Reihenfolge: er blockiert bereichstypische Edits, bis
ZUERST der Almanach UND DANN (falls vorhanden) die Best-Practices-Datei in dieser Session per Read
geoeffnet wurde (jedes Lesen gibt frei — Kurzcheck per `limit=80` genuegt ausser bei Stufe C;
gilt pro Bereich 1x/Session). Existiert keine
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
| CLAUDE.md, Regel, Settings, Skill, Command, Agent | `bugs/claude-tooling/claude-config.md` + `best-practices/claude-tooling/claude-config.md` |
| Einen MCP-Server | `bugs/claude-tooling/mcp-server.md` |
| Ein Python-Hilfsskript | `bugs/claude-tooling/python-windows.md` |

(`claude-hooks` und `claude-config` sind **Stufe-C-Bereiche** → VOLLTEXT lesen;
`mcp-server` und `python-windows`: Kurzcheck genuegt, Stufe A.)

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
| Weitere Bereiche (Android, WPF, Swift, TS, Hooks, Gradle …) | `~/proggs/bugs/<kategorie>/<bereich>.md` (bei erster echter Arbeit anlegen, siehe README) |

Erkennungssignale fuer "Chrome-Extension-Arbeit": eine `manifest.json` mit
`manifest_version`, Dateien in einem Extension-/Overlay-Ordner, `chrome.*`-APIs,
oder der Nutzer nennt Erweiterung/Extension/Overlay-Plugin.

---

## Zusammenspiel

| System | Rolle |
|--------|-------|
| `~/proggs/bugs/*.md` + `README.md` + `SYSTEM.md` | Proaktiver Almanach pro Bereich (VOR der Arbeit): was schiefgeht + Loesung |
| `~/proggs/best-practices/**` | Proaktive Best-Practices pro Bereich (zweite Seite, ebenfalls VOR der Arbeit gelesen): wie man es richtig macht |
| `bug-almanac-index` / `bug-almanac-guard` Hooks | Automatik: Liste einblenden / Almanach + Best-Practices erzwingen |
| `bug-cases.jsonl` | Reaktive Fall-Datenbank mit Auto-Match (NACH einem Fehler) |
| Direktive #3 (Resilient Bugfixing) | Jeder neue Bug → Almanach-Eintrag + bug-case; Loesungen IMMER funktionserhaltend |
| Direktive #1 (Superintelligenz/Harness) | Der Almanach ist Harness-Wissen, das jede Session erbt und mitwaechst |

---

## Was NIEMALS passieren darf

- ❌ An einem Bereich arbeiten, fuer den ein Almanach existiert, ohne vorher mindestens dessen Kurzcheck zu lesen (Stufe A)
- ❌ Nach einem Fehler im Bereich einfach weiterarbeiten, ohne den VOLLTEXT des Almanachs gelesen zu haben (Stufe B)
- ❌ In einem Hochrisiko-Bereich (r8, firebase-billing, claude-hooks, claude-config) nur den Kurzcheck lesen — Stufe C verlangt den Volltext
- ❌ Bei einem WIEDERKEHRENDEN Bug (schon einmal gefixt, tritt erneut auf) nur den Kurzcheck (erneut) lesen statt sofort den Volltext von Almanach UND Best Practices (Stufe D)
- ❌ Bei einem wiederkehrenden Bug ohne Loesung im Volltext einfach weiterraten, statt das Grundproblem gezielt zu recherchieren (ueber den `research`-Skill nach Protokoll)
- ❌ Beim Ergaenzen eines Almanachs die Kurzcheck-Sektion vergessen — ein neuer Top-Bug gehoert auch in den Kurzcheck, wenn er zu den wichtigsten zaehlt
- ❌ Den Kurzcheck-Almanach lesen, aber die zugehoerige Best-Practices-Datei (Kurzcheck) ueberspringen — beide gehoeren VOR die Arbeit (erst Almanach, dann Best Practices)
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
