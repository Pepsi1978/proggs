# 🛡️ DRITTE DIREKTIVE: Resilient Bugfixing & Proaktive Intelligenz-Steigerung (KRITISCH)
 
> **Diese Direktive ist die DRITTWICHTIGSTE im gesamten System — direkt nach Superintelligenz (#1)
> und Selbstbeobachtung (#2). Sie gilt AUTOMATISCH fuer JEDEN Bugfix — egal wie klein oder
> trivial er erscheint. Der Benutzer muss diese Direktive NICHT explizit erwaehnen oder
> aktivieren. Sobald ein Fehler erkannt, gemeldet oder behoben wird, tritt diese Direktive
> vollstaendig in Kraft.**
 
---
 
## Inhaltsverzeichnis
 
1. [Prioritaets-Hierarchie](#prioritaets-hierarchie-des-gesamtsystems)
2. [Grundprinzip](#grundprinzip-jeder-bug-wird-zum-permanenten-system-upgrade)
3. [Geltungsbereich](#geltungsbereich-automatisch-und-ausnahmslos)
4. [Funktionalitaets-Erhaltungspflicht](#funktionalitaets-erhaltungspflicht-kritisch)
5. [Pflicht-Ablauf bei jedem Bugfix](#pflicht-ablauf-bei-jedem-bugfix)
6. [Defense in Depth](#4-mehrere-absicherungsschichten-defense-in-depth)
7. [Poka-Yoke](#5-poka-yoke-fehler-durch-design-unmoeglich-machen)
8. [Proaktive Intelligenz-Vorschlaege](#proaktive-intelligenz-vorschlaege-pflicht-in-jeder-session)
9. [Plattform-spezifische Regeln](#plattform-spezifische-bugfix-regeln-windows-vs-macos)
10. [Verbotsliste](#was-niemals-passieren-darf)
11. [Referenz-Beispiele](#referenz-beispiele)
 
---
 
## Prioritaets-Hierarchie des Gesamtsystems
 
| Rang | Direktive | Datei |
|------|-----------|-------|
| **#1** | Superintelligenz | `~/.claude/rules/superintelligence.md` |
| **#2** | Selbstbeobachtung | `~/.claude/rules/self-observation.md` |
| **#3** | **Resilient Bugfixing** | `~/.claude/rules/resilient-bugfixing.md` (diese Datei) |
| #4+ | Alle anderen Regeln | `~/.claude/rules/*` |
 
---
 
## Grundprinzip: Jeder Bug wird zum permanenten System-Upgrade
 
Wenn ein Fehler gefunden und gefixt wird, ist der Fix NICHT fertig, bis er **zukunftssicher** ist.
Ein Fehler darf in dieser Programmierumgebung **niemals zweimal auftreten** — egal in welcher
Session, auf welcher Plattform, nach welchem Update. Der Fix muss den Fehler nicht nur
beseitigen, sondern die gesamte Fehlerklasse dauerhaft eliminieren.
 
**Kerngedanke**: Ein Bugfix ist kein Pflaster, sondern eine Immunisierung. Das System soll
nach jedem Fix SCHLUEER sein als vorher — nicht nur "repariert", sondern "gehaertet".
 
---
 
## Geltungsbereich: Automatisch und ausnahmslos
 
Diese Direktive gilt **automatisch** fuer:
 
| Situation | Gilt? | Begruendung |
|-----------|-------|-------------|
| Kritischer Produktionsfehler | ✅ JA | Offensichtlich |
| Kleiner Tippfehler im Code | ✅ JA | Auch Tippfehler haben Root Causes (z.B. fehlende Linter-Regel) |
| Warnungen die "eigentlich harmlos" sind | ✅ JA | Warnungen werden zu Fehlern wenn man sie ignoriert |
| Fehler der "nur einmal" auftrat | ✅ JA | Jeder Fehler der einmal auftritt, kann wieder auftreten |
| Fehler in Tests | ✅ JA | Instabile Tests untergraben das Vertrauen in die Testsuite |
| Fehler in Build-Skripten oder Hooks | ✅ JA | Infrastruktur-Fehler sind die teuersten |
| Fehler in Dokumentation oder Configs | ✅ JA | Falsche Doku fuehrt zu falschen Entscheidungen |
| Performance-Probleme | ✅ JA | Langsamer Code ist ein Bug wenn er Erwartungen verletzt |
| Zwischenfehler die "danach gefixt wurden" | ✅ JA | Muessen trotzdem analysiert und praventiv abgesichert werden |
 
**Es gibt KEINE Ausnahme.** Wenn es ein Fix ist, gilt diese Direktive.
 
Der Benutzer muss zu KEINEM Zeitpunkt sagen "bitte wende die Dritte Direktive an" oder
"nutze Resilient Bugfixing". Diese Direktive ist IMMER aktiv. Sie ist Teil des
Betriebssystems dieser Programmierumgebung.
 
---
 
## Funktionalitaets-Erhaltungspflicht (KRITISCH)
 
> **Ein Bugfix darf NIEMALS dazu fuehren, dass eine vorher vorhandene Funktion verschwindet,
> deaktiviert, auskommentiert oder stillschweigend geschluckt wird.**
 
Dies ist eines der wichtigsten Prinzipien dieser Direktive. Claude Code neigt dazu, bei
schwierigen Fehlern die betroffene Funktionalitaet zu entfernen statt zu reparieren. Das
ist KEIN Fix — das ist ein Feature-Verlust, der als Fix getarnt ist.
 
### Verbotene Fix-Strategien
 
| ❌ VERBOTEN | ✅ ERLAUBT |
|-------------|-----------|
| Feature auskommentieren | Feature mit Fehlerbehandlung absichern |
| Code-Block loeschen der Fehler wirft | Code-Block reparieren oder durch robustere Version ersetzen |
| Leeres `catch {}` das Fehler verschluckt | `catch` mit Logging, Fehlerweiterleitung und/oder Fallback-Verhalten |
| Import entfernen weil er Fehler wirft | Import-Fehler abfangen und Fallback-Modul laden |
| `try { feature() } catch { /* nichts */ }` | `try { feature() } catch(e) { log(e); fallback() }` |
| Funktion durch leere Funktion ersetzen | Funktion reparieren oder durch funktional aequivalente Version ersetzen |
| "Vereinfachung" die Features entfernt | Refactoring das ALLE Features erhaelt und Code vereinfacht |
| Konfigurationsoption still entfernen | Konfigurationsoption mit Validierung und Default-Wert absichern |
| Fehlerhafte API-Route deaktivieren | API-Route mit Eingabevalidierung und Fehlerbehandlung reparieren |
 
### Warum das so wichtig ist
 
Wenn eine Funktion Fehler wirft, bedeutet das:
1. Die Funktion wird GEBRAUCHT (sonst waere sie nicht da)
2. Die Funktion hat einen BUG (der behoben werden muss)
3. Das Entfernen der Funktion LOEST NICHT das Problem — es verschiebt es nur zum Benutzer
 
**Analogie**: Wenn ein Rad am Auto quietscht, schraubst du es nicht ab. Du findest heraus
WARUM es quietscht und reparierst es. Ein Auto ohne Rad ist schlimmer als eins mit
quietschendem Rad.
 
### Ausnahme
 
Funktionalitaet darf NUR entfernt werden, wenn der Benutzer EXPLIZIT und AUSDRUECKLICH
darum bittet. In diesem Fall:
1. Bestaetigung einholen: "Du moechtest [Feature X] komplett entfernen?"
2. Dokumentieren WARUM es entfernt wurde
3. Sicherstellen dass nichts anderes von diesem Feature abhaengt
 
---
 
## Bug-Case-Match dem Benutzer ANSAGEN (KRITISCH)

Wenn der `bug-case-auto-writer` Hook einen bekannten Fehler matcht und den Fix als
`additionalContext` injiziert (erkennbar am Text `BEKANNTER FEHLER`), MUSS Claude
das dem Benutzer **sofort auf Deutsch ansagen** — mitten in der Arbeit, nicht erst am Ende.

Wenn der `cross-platform-file-guard` Hook eine Cross-Platform-Warnung ausgibt
(erkennbar am Text `CROSS-PLATFORM`), MUSS Claude das dem Benutzer **sofort ansagen**:

**Pflicht-Format fuer Cross-Platform-Warnung:**
```
⚠️ Cross-Platform-Warnung: [Dateiname] wurde geaendert, aber das [macOS/Windows]-Gegenstueck fehlt/ist veraltet.
Ich erstelle/aktualisiere es jetzt.
```

**Pflicht-Format fuer Bug-Case-Match (Schild-Symbol):**
```
🛡️ Der Bug-Case Auto-Writer hat einen bekannten Fehler erkannt ([X]% Match).
Letztes Mal war die Ursache: [Root Cause aus dem Match]
Ich wende den bekannten Fix zuerst an.
```

**Pflicht-Format fuer Config-Guard-Blockierung (Stopp-Symbol):**
Wenn der `config-guard-preemptive` Hook eine Aenderung blockiert hat, MUSS Claude
das dem Benutzer sofort ansagen:
```
🚫 Config-Guard hat eine Aenderung blockiert: [Was blockiert wurde].
Die Aenderung wurde verhindert BEVOR sie wirksam wurde.
Wenn du das trotzdem willst, sag mir Bescheid.
```

Danach den Fix aus dem Match anwenden BEVOR eine eigene Hypothese versucht wird.
Der Benutzer will sehen dass das System sich erinnert — das macht den Compound
Intelligence Effect sichtbar.

---

## Pflicht-Ablauf bei JEDEM Bugfix
 
### 1. Root Cause finden (nicht nur Symptom fixen)
 
Den eigentlichen Ausloeser identifizieren, nicht nur das sichtbare Problem.
 
**Die "5-Warum"-Methode** — mindestens 3x "Warum?" fragen:
 
| Ebene | Frage | Beispiel |
|-------|-------|---------|
| Symptom | Was passiert? | "Hook blockiert die Session" |
| Warum 1 | Warum passiert das? | "Hook gibt exit 1 zurueck" |
| Warum 2 | Warum gibt er exit 1? | "Worker-Start schlaegt fehl" |
| Warum 3 | Warum schlaegt der Start fehl? | "Port ist belegt — Race Condition mit MCP" |
| **Root Cause** | **Was ist die tiefste Ursache?** | **"Kein Locking-Mechanismus fuer Service-Starts"** |
 
**Fehlerlokalisierung auf Funktions-Level** (arxiv 2604.00167): Zuerst die betroffene
FUNKTION benennen (nicht nur die Datei und nicht nur die exakte Zeile). Empirisch
hoechste Repair-Rate bei function-level Lokalisierung.
 
| Granularitaet | Trefferquote | Verwendung |
|---------------|-------------|------------|
| File-level | Niedrig | Zu ungenau — viele Funktionen pro Datei |
| **Function-level** | **Hoechste** | **Standard — immer zuerst die Funktion identifizieren** |
| Line-level | Mittel | Verliert Kontext — nutzen fuer den eigentlichen Patch |
 
### 2. Verwandte Fehlerquellen suchen (PFLICHT)
 
Jeder gefundene Fehler ist ein HINWEIS darauf, dass aehnliche Fehler existieren koennen.
Diese Suche ist PFLICHT — nicht optional.
 
**Drei Dimensionen der Suche:**
 
| Dimension | Frage | Beispiel |
|-----------|-------|---------|
| **Gleiche Klasse** | Kann der gleiche Fehlertyp an anderer Stelle auftreten? | Race Condition bei claude-mem → gibt es andere Plugins mit dem gleichen Muster? |
| **Gleiche Komponente** | Kann die betroffene Komponente auf andere Weise versagen? | Port-Konflikt → was passiert bei Timeout? Bei Crash? Bei Update? |
| **Gleiche Abhaengigkeit** | Welche anderen Teile haengen von der gleichen Sache ab? | Worker-Service → wer braucht den noch? Was wenn er spaeter crasht? |
 
**Ergebnis dokumentieren**: Auch wenn keine verwandten Fehler gefunden werden, MUSS das
dokumentiert werden: "Verwandte Fehlerquellen geprueft: [Klasse X, Komponente Y, Abhaengigkeit Z]
— keine weiteren Stellen betroffen."
 
### 3. Zukunftssicheren Fix implementieren
 
Der Fix muss ALLE diese Eigenschaften haben:
 
| Eigenschaft | Bedeutung | Prueffrage |
|-------------|-----------|------------|
| **Self-Healing** | Repariert sich selbst nach Updates, Neustarts, Plattformwechsel | "Funktioniert der Fix nach einem Plugin-Update?" |
| **Defensiv** | Faengt nicht nur den bekannten Fehler ab, sondern die ganze Fehlerklasse | "Was passiert bei einer Variante dieses Fehlers?" |
| **Ueberlebbar** | Uebersteht Plugin-Updates, Config-Aenderungen, System-Upgrades | "Was passiert nach `npm update`?" |
| **Erweiterbar** | Kann fuer zukuenftige aehnliche Faelle erweitert werden | "Kann ein neues Plugin den gleichen Mechanismus nutzen?" |
| **Dokumentiert** | Memory-Eintrag erklaert Ursache, Fix und wie man neue Faelle hinzufuegt | "Versteht die naechste Session was hier passiert?" |
| **Schadensfrei** | Der Fix selbst darf KEINE neuen Fehler einfuehren (siehe 3b) | "Habe ich die Fix-Induced-Failure-Pruefung gemacht?" |
| **Funktionserhaltend** | Der Fix darf KEINE bestehende Funktionalitaet entfernen (siehe oben) | "Funktioniert alles was vorher funktioniert hat?" |
 
### 3b. Fix-Induced-Failure-Pruefung (PFLICHT — VOR dem Commit)
 
> **Ein Fix der neue Probleme verursacht ist SCHLIMMER als kein Fix.**
 
Vor JEDEM Commit eines Bugfixes MUESSEN alle 8 Pruefungen durchlaufen werden:
 
| # | Pruefung | Frage | Beispiel |
|---|----------|-------|----------|
| 1 | **Abhaengigkeiten** | Was haengt vom geaenderten Code ab? | Launcher-Script aendern → launchd-plist testen |
| 2 | **Fehlszenarien** | Was passiert wenn der Fix-Code selbst fehlschlaegt? | Health-Check crasht → blockiert er die Session? (Muss exit 0 sein!) |
| 3 | **Zustandsaenderungen** | Aendert der Fix einen Systemzustand dauerhaft? | launchd-Agent → was wenn Plugin deinstalliert wird? (Crash-Loop!) |
| 4 | **Race Conditions** | Kann der Fix mit anderem Code kollidieren? | Guard + Plugin-Hook starten Worker → doppelter Start? |
| 5 | **Rueckwaertskompatibilitaet** | Bricht der Fix etwas das vorher funktionierte? | Neue Imports in index.ts → existieren die Module? |
| 6 | **Plattform-Effekte** | Funktioniert der Fix auf macOS UND Windows? | .sh-Hook → braucht es ein .ps1-Gegenstueck? |
| 7 | **Update-Resistenz** | Ueberlebt der Fix das naechste Plugin/CLI/OS-Update? | Hardcoded Pfade → dynamisch aufloesen |
| 8 | **Graceful Degradation** | Was wenn eine Voraussetzung fehlt? | Bun nicht installiert → auf Node.js zurueckfallen, nicht crashen |
 
**Faustregel**: Der Fix muss die Frage bestehen:
> "Was ist das Schlimmste das passieren kann wenn ich diesen Fix deploye
> und dann 6 Monate lang nicht hinschaue?"
 
Wenn die Antwort auf diese Frage IRGENDETWAS anderes als "nichts Schlimmes" ist,
muss der Fix ueberarbeitet werden.
 
### 3c. Funktionalitaets-Diff (PFLICHT — VOR dem Commit)
 
Vor dem Commit eines Bugfixes MUSS ein Funktionalitaets-Abgleich erstellt werden.
Dieser Abgleich stellt sicher, dass der Fix keine bestehende Funktionalitaet zerstoert.
 
**Format:**
 
```
Funktionalitaets-Diff fuer [Fix-Beschreibung]:
 
VORHER:
- [Feature A] ✅ funktionierte
- [Feature B] ✅ funktionierte
- [Feature C] ❌ FEHLER (das war der Bug)
- [Feature D] ✅ funktionierte
 
NACHHER:
- [Feature A] ✅ funktioniert (unveraendert)
- [Feature B] ✅ funktioniert (unveraendert)
- [Feature C] ✅ funktioniert (GEFIXT)
- [Feature D] ✅ funktioniert (unveraendert)
```
 
**Regeln:**
- Wenn ein Feature von ✅ auf ❌ wechselt → **Fix ist NICHT fertig** — Regression muss behoben werden
- Wenn ein Feature von ✅ auf ⚠️ wechselt (teilweise beeintraechtigt) → **Fix muss ueberarbeitet werden**
- ALLE Features die vom geaenderten Code BERUEHRT werden muessen aufgelistet werden
- "Funktioniert" bedeutet: getestet, nicht nur "sieht richtig aus"
- Bei komplexen Fixes: Features einzeln durchgehen und jeweils pruefen
 
**Der Diff muss nicht in die Commit-Message, aber er MUSS gedanklich durchgefuehrt werden.**
Bei kritischen Fixes kann er als Kommentar im Code oder in der Memory dokumentiert werden.
 
### 4. Mehrere Absicherungsschichten (Defense in Depth)
 
Nie nur EINE Absicherung. Immer mindestens 2-3 Schichten. Jede Schicht muss
UNABHAENGIG von den anderen funktionieren — wenn Schicht 1 versagt, faengt Schicht 2 ab.
 
| Schicht | Typ | Wann | Beispiel |
|---------|-----|------|---------|
| **1** | Praeventiv | Problem verhindern BEVOR es auftritt | Eingabevalidierung, Type-Checks, Linter-Regeln |
| **2** | Reaktiv | Problem abfangen WENN es doch auftritt | Try-Catch mit sinnvollem Fallback, Circuit Breaker |
| **3** | Selbstheilend | Fix automatisch wiederherstellen nach Updates | Auto-Patcher, Template-Systeme, Watchdog-Prozesse |
| **4** | Upstream | Bug beim Verursacher melden fuer permanenten Fix | GitHub Issue, Pull Request, Bug Report |
 
**Mindestanforderung**: Schicht 1 (Praeventiv) + Schicht 2 (Reaktiv) sind PFLICHT.
Schicht 3 und 4 sind empfohlen, aber bei kleinen Fixes nicht immer noetig.
 
**Wichtig**: Reaktive Schichten (Catch-Bloecke, Fallbacks) muessen IMMER:
1. Den Fehler LOGGEN (nicht still verschlucken)
2. Ein FUNKTIONIERENDES Fallback-Verhalten ausfuehren (nicht einfach abbrechen)
3. Den Benutzer INFORMIEREN wenn die Fallback-Qualitaet schlechter ist als das Original
 
### 5. Poka-Yoke: Fehler durch Design unmoeglich machen
 
> **Poka-Yoke** (jap. ポカヨケ, "Fehlervermeidung") ist ein Prinzip aus dem Toyota-Produktionssystem:
> Gestalte den Prozess so, dass Fehler gar nicht erst entstehen koennen — statt sie nachtraeglich
> zu finden und zu reparieren.
 
**Kernprinzip**: Wenn ein Fehler passiert ist, frage nicht nur "Wie fixe ich das?" sondern:
**"Wie gestalte ich den Prozess so um, dass dieser Fehler UNMOEGLICH wird?"**
 
### Die 3 Poka-Yoke-Stufen (von schwach zu stark)
 
| Stufe | Name | Beschreibung | Beispiel |
|-------|------|-------------|----------|
| **1** | **Warnung** | System warnt wenn ein Fehler droht, laesst ihn aber zu | `hook-exit0-guard` warnt bei fehlendem `exit 0` im Commit |
| **2** | **Erzwingung** | System verhindert den Fehler aktiv | Pre-Push-Hook rejected Push ohne vorheriges `fetch+rebase` |
| **3** | **Eliminierung** | Fehler kann konzeptionell nicht mehr auftreten | Hook-Template hat `exit 0` eingebaut — man muesste es aktiv ENTFERNEN um den Fehler zu machen |
 
**Ziel**: So viele Fehlerquellen wie moeglich auf **Stufe 3 (Eliminierung)** bringen.
Stufe 1 (Warnung) ist besser als nichts, aber Stufe 3 ist das Ideal.
 
### Wann Poka-Yoke anwenden (PFLICHT)
 
Bei JEDEM Bugfix diese drei Fragen in absteigender Reihenfolge pruefen:
 
1. **Kann ich den Fehler durch ein Template/Default eliminieren?** (Stufe 3)
   - Beispiel: Hook-Template mit eingebautem `exit 0` → Fehler kann nicht mehr passieren
   - Beispiel: JSON-Validierung direkt im Write-Workflow → kaputtes JSON unmoeglich
   - Beispiel: TypeScript statt JavaScript → Typfehler werden beim Kompilieren erkannt
 
2. **Kann ich den Fehler durch einen Guard erzwingen?** (Stufe 2)
   - Beispiel: Pre-Push-Hook erzwingt `fetch+rebase` → Push-Rejection unmoeglich
   - Beispiel: Config-Guard blockiert Aenderungen an `defaultMode` → Permission-Reset unmoeglich
   - Beispiel: Schema-Validierung vor dem Speichern → ungueltige Daten koennen nicht gespeichert werden
 
3. **Kann ich zumindest warnen?** (Stufe 1)
   - Beispiel: Disk-Guard warnt bei <5GB Speicher → Speicherueberlauf frueh erkannt
   - Beispiel: Session-Guard warnt bei falscher Permission → schnelle Korrektur
   - Beispiel: Deprecation-Warning bei veralteten API-Aufrufen → Migration wird angestossen
 
### Poka-Yoke bei neuen Features (nicht nur bei Bugfixes)
 
Auch bei NEUEN Features pruefen: "Welche Fehler koennte ein Benutzer oder Agent machen?"
und praeventive Mechanismen einbauen BEVOR der erste Fehler passiert. Das ist der Unterschied
zwischen reaktivem Bugfixing (Fehler passiert → Fix) und proaktivem Poka-Yoke (Fehler kann
gar nicht erst passieren).
 
### Bestehende Poka-Yoke-Mechanismen im System
 
| Mechanismus | Stufe | Was er verhindert |
|-------------|-------|------------------|
| `hook-forge` Skill (Templates) | 3 — Eliminierung | Hooks ohne exit 0, ohne try/catch, ohne Logging |
| `hook-exit0-guard` (Pre-Commit) | 1 — Warnung | Vergessenes exit 0 bei Hook-Commits |
| `poka-yoke-git-push` (Pre-Push) | 2 — Erzwingung | Push ohne fetch+rebase |
| `config-guard` (PostToolUse) | 2 — Erzwingung | Aenderungen an bypassPermissions |
| `session-guard` (SessionStart) | 3 — Eliminierung | Falsche Permission-Einstellungen (repariert automatisch) |
| `safety-gate` (PreToolUse) | 2 — Erzwingung | Destruktive Befehle (rm -rf, DROP TABLE) |
| `redact-secrets` (PreToolUse) | 2 — Erzwingung | Secrets in Tool-Ausgaben |
| Hook-Templates (ps1/sh) | 3 — Eliminierung | Fehlerhafte Hook-Grundstruktur |
| Python-Batch statt Agents (Regel) | 3 — Eliminierung | Inkonsistente Multi-Datei-Edits |
| 3-Dateien-Regel (Settings) | 1 — Warnung | Vergessene Settings-Synchronisation |
 
### 6. Memory speichern (PFLICHT)
 
Jeder Bugfix MUSS in einer Memory festgehalten werden, damit das Wissen ZWISCHEN
Sessions erhalten bleibt. Eine Memory ohne diese Informationen ist unvollstaendig:
 
| Feld | Inhalt | Beispiel |
|------|--------|---------|
| **Fehler** | Was war das sichtbare Problem? | "Hook blockiert Session-Start" |
| **Root Cause** | Was war die tiefste Ursache? | "Race Condition: MCP und Hook starten Worker gleichzeitig" |
| **Fix** | Was wurde konkret geaendert? | "Service Registry mit Locking, Auto-Patcher fuer hooks.json" |
| **Verwandte Pruefung** | Welche verwandten Stellen wurden geprueft? | "Alle Plugins mit Worker-Start geprueft, 2 weitere abgesichert" |
| **Poka-Yoke-Stufe** | Wurde der Prozess umgestaltet? Welche Stufe? | "Stufe 3 — Template verhindert den Fehler strukturell" |
| **Muster-Erkennung** | Woran erkennt man diesen Fehlertyp in Zukunft? | "Wenn ich [Muster X] sehe, muss ich [Check Y] machen" |
| **Funktions-Diff** | Wurde Funktionalitaet erhalten? | "Alle Features ✅ — keine Regression" |
 
---
 
## Proaktive Intelligenz-Vorschlaege (PFLICHT in jeder Session)
 
Claude muss **waehrend der Arbeit** kontinuierlich nach Verbesserungen suchen und dem Benutzer
Vorschlaege machen. Nicht nur wenn Fehler auftreten, sondern IMMER.
 
### Wann und wo Vorschlaege machen?
 
| Regel | Begruendung |
|-------|-------------|
| **Nur AM ENDE der Aufgabe** — NIEMALS mittendrin waehrend der Arbeit | Unterbrechungen stoeren den Flow |
| Der Vorschlag kommt NACH der Status-Meldung | Erst die Arbeit beenden, dann optimieren |
| **Nur wenn es einen ECHTEN Mehrwert gibt** | Keinen Vorschlag erzwingen wenn nichts auffaellt |
| Kein Vorschlag ist besser als ein banaler Vorschlag | Qualitaet vor Quantitaet |
 
### Wann ist ein Vorschlag sinnvoll?
 
- Zwischenfehler in der Session die spaeter gefixt wurden → **Praevention** vorschlagen
- Wiederkehrende manuelle Schritte → **Automatisierung** vorschlagen
- Fehlende Absicherung (keine Tests, kein Retry, kein Fallback) → **Haertung** vorschlagen
- Ineffizienzen (zu viele Versuche, zu lange Wartezeit) → **Optimierung** vorschlagen
- Neue Muster erkannt die das System schlauer machen koennten → **Muster-Upgrade** vorschlagen
 
### Grenzen der Proaktivitaet (Schutz vor Over-Engineering)
 
> **KISS-Prinzip**: Die vorgeschlagene Verbesserung darf NICHT komplexer sein als das
> Problem das sie loest.
 
| Regel | Begruendung |
|-------|-------------|
| Abstraktion nur wenn **mindestens 2 konkrete Faelle** existieren | Keine Abstraktion fuer hypothetische zukuenftige Faelle |
| Kein Refactoring das **mehr als 3 Dateien** gleichzeitig aendert | Solche Vorschlaege muessen als separates Projekt behandelt werden |
| Keine neuen Abhaengigkeiten wenn eine einfache Loesung existiert | `npm install` ist kein Fix fuer ein 5-Zeilen-Problem |
| Kein Framework vorschlagen wo ein Skript reicht | Frameworks haben eigene Bugs und Update-Zyklen |
| Vorschlag muss in **einem Satz** erklaerbar sein | Wenn der Vorschlag eine Seite Erklaerung braucht, ist er zu komplex |
 
### Format
 
Kurz, klar, am Ende der Antwort, maximal 2-3 Saetze:
```
💡 **Intelligenz-Vorschlag**: [Was verbessert werden kann]
   → [Konkreter Vorschlag] — Soll ich das umsetzen?
```
 
---
 
## Plattform-spezifische Bugfix-Regeln (Windows vs macOS)
 
### Windows: UTF-8 Encoding ist PFLICHT
 
Python auf Windows verwendet standardmaessig `cp1252` als Dateikodierung. Jede Datei
die Unicode-Zeichen enthalten koennte (Emojis, Umlaute, Sonderzeichen) MUSS mit
explizitem `encoding='utf-8'` geoeffnet werden:
 
```python
# FALSCH — crasht auf Windows bei Emojis/Sonderzeichen:
with open(path, 'w') as f:
    json.dump(data, f)
 
# RICHTIG — funktioniert auf Windows UND macOS:
with open(path, 'w', encoding='utf-8') as f:
    json.dump(data, f, ensure_ascii=False)
```
 
**Gilt fuer**: Alle Python-Skripte, Hooks, Build-Skripte und Einmal-Befehle die Dateien
schreiben oder lesen. Ohne `encoding='utf-8'` schlaegt das Schreiben von JSON mit
Emojis/Unicode fehl (UnicodeEncodeError: 'charmap' codec).
 
### Atomares Schreiben (Write-to-Temp-then-Rename)
 
Beim Schreiben von kritischen Konfigurationsdateien (settings.json, MEMORY.md, etc.)
MUSS das atomare Muster verwendet werden. Sonst bleibt bei einem Crash eine abgeschnittene
Datei zurueck die nicht mehr gelesen werden kann:
 
```python
import tempfile, os, json
 
def safe_json_write(path, data):
    """Atomic write: temp file → rename. Never corrupts the original."""
    dir_name = os.path.dirname(path)
    with tempfile.NamedTemporaryFile('w', dir=dir_name, suffix='.tmp',
                                      delete=False, encoding='utf-8') as tmp:
        json.dump(data, tmp, indent=2, ensure_ascii=False)
        tmp.write('\n')
        tmp_path = tmp.name
    # Atomic replace (on Windows: os.replace handles this)
    os.replace(tmp_path, path)
```
 
**Wann**: Bei JEDER Datei die bei Beschaedigung das System blockiert (settings.json,
MEMORY.md, session-scores.jsonl, etc.)
 
**Warum**: In frueheren Sessions ist genau das passiert — Python-Crash beim Schreiben hat
settings.json abgeschnitten und JSON unlesbar gemacht.
 
### Windows vs macOS Unterschiede (Referenz)
 
| Aspekt | macOS | Windows |
|--------|-------|---------|
| Default File Encoding | UTF-8 | cp1252 (ANSI) — IMMER `encoding='utf-8'` |
| Shell | zsh/bash nativ | Git Bash (bash-Emulation), pwsh fuer Hooks |
| Hook-Ausfuehrung | `bash ~/.claude/hooks/*.sh` | `pwsh -File "$USERPROFILE/.claude/hooks/*.ps1"` |
| Temp-Verzeichnis | `/tmp/` | `$env:TEMP/` (= `%LOCALAPPDATA%\Temp`) |
| Home-Variable | `$HOME` oder `~` | `$USERPROFILE` oder `$HOME` (Git Bash) |
| Pfad-Trenner | `/` (Forward Slash) | `\` nativ, aber `/` funktioniert in Git Bash |
| Line Endings | LF (`\n`) | CRLF (`\r\n`) — Git autocrlf=true |
| Bun-Pfad | `/opt/homebrew/bin/bun` | `$USERPROFILE/.bun/bin/bun.exe` |
| npx-Pfad | `/opt/homebrew/bin/npx` | npx via npm im PATH |
| Executable-Endung | keine | `.exe` |
| Symlinks | Native Unterstuetzung | Braucht Developer Mode oder Admin-Rechte |
| `grep -P` (PCRE) | Funktioniert (UTF-8 Locale) | CRASHT! Immer `awk`/`sed` verwenden |
| Default Locale | `en_US.UTF-8` | `German_Germany.1252` (kein UTF-8!) |
 
---
 
## Was NIEMALS passieren darf
 
Diese Liste ist ABSOLUT. Jeder einzelne Punkt ist ein Versagen der Dritten Direktive:
 
### Fehleranalyse-Versagen
 
- ❌ Nur das Symptom fixen ohne Root Cause zu verstehen
- ❌ Fix nur fuer den einen konkreten Fall, ohne aehnliche Faelle zu pruefen
- ❌ Einen Fehler ein ZWEITES Mal machen — jeder Fehler wird genau EINMAL gemacht
- ❌ Zwischenfehler in der Session ignorieren nur weil sie "danach" gefixt wurden
 
### Funktionalitaets-Versagen
 
- ❌ Funktionalitaet deaktivieren, auskommentieren, leeren oder mit leerem `try/catch`
  schlucken, um Fehlermeldungen zu unterdruecken — ein Fix MUSS die Funktion ERHALTEN,
  nicht entfernen
- ❌ Code-Bloecke loeschen die Fehler werfen, statt sie zu reparieren
- ❌ Features "vereinfachen" indem Funktionalitaet entfernt wird
- ❌ Imports oder Module entfernen weil sie Fehler verursachen, statt die Fehler zu beheben
- ❌ Fehlerhafte Funktionen durch leere Funktionen ersetzen (No-Op als Fix)
 
### Haltbarkeits-Versagen
 
- ❌ Fix der bei naechstem Plugin-Update oder Neustart kaputt geht
- ❌ "Funktioniert jetzt" sagen ohne zu pruefen ob es in 2 Wochen noch funktioniert
- ❌ Fix ohne Memory-Eintrag (Wissen geht verloren zwischen Sessions)
- ❌ Hardcoded Pfade oder Versionen die beim naechsten Update brechen
 
### Fix-Qualitaets-Versagen
 
- ❌ Fix deployen der selbst neue Fehler einfuehrt (Fix-Induced Failure)
- ❌ Fix ohne die 8-Punkte-Pruefung aus Schritt 3b durchzugehen
- ❌ Fix ohne Funktionalitaets-Diff aus Schritt 3c
 
### Proaktivitaets-Versagen
 
- ❌ Session beenden ohne Rueckblick auf Verbesserungsmoeglichkeiten
- ❌ Stumm arbeiten ohne proaktive Vorschlaege zur Intelligenz-Steigerung
- ❌ Over-Engineering vorschlagen das komplexer ist als das Problem selbst
 
---
 
## Referenz-Beispiele
 
### Beispiel 1: claude-mem Hook-Fehler (2026-03-22)
 
| Schritt | Was gemacht wurde |
|---------|-------------------|
| Root Cause | Race Condition: MCP-Server und Hook starten Worker gleichzeitig |
| Verwandte Fehler | Alle Plugins mit Daemon/Worker/Server-Start geprueft |
| Self-Healing | Auto-Patcher repariert hooks.json nach jedem Plugin-Update |
| Defensiv | Service Registry fuer ALLE zukuenftigen Service-Plugins |
| Mehrere Schichten | Guard-Hook + Auto-Patcher + Plugin-Patch + Upstream-Issue |
| Funktions-Diff | Alle Features ✅ — Worker startet, Hook blockiert nicht, MCP funktioniert |
| Memory | Feedback-Memory mit Muster-Erkennung gespeichert |
 
### Beispiel 2: Fix-Induced-Failure vermieden (2026-03-22)
 
| Pruefung | Problem erkannt | Massnahme |
|----------|----------------|-----------|
| Zustandsaenderung | launchd-Agent wuerde bei Plugin-Deinstallation Crash-Loop ausloesen | Launcher wartet geduldig statt exit 1, schlaeft 5min vor Retry |
| Graceful Degradation | Bun koennte fehlen oder Pfad sich aendern | Multi-Path-Suche + Fallback auf Node.js |
| Fehlszenario | Health-Check-Hook koennte crashen und Session blockieren | Immer exit 0, set +e, Fallback-Logger |
| Race Condition | Worker bereits laufend → doppelter Start | Pre-Flight-Check via Health-Endpoint |
| Rueckwaertskompatibilitaet | Neue Module (db-state.ts) muessen committed sein | Sofort ins Repo committed, nie nur lokal |
 
### Beispiel 3: Verbotener Fix vs. Korrekter Fix
 
**Szenario**: Ein Logger-Modul wirft einen Fehler weil der Log-Pfad nicht existiert.
 
| ❌ Verbotener Fix | ✅ Korrekter Fix |
|-------------------|-----------------|
| Logger-Aufruf auskommentieren | Pfad-Existenz pruefen und ggf. Verzeichnis erstellen |
| `try { log() } catch {}` (still) | `try { log() } catch(e) { console.error(e); logToFallback() }` |
| Logger-Import entfernen | Logger mit Fallback-Pfad absichern |
| "Logging ist optional" | "Logging ist wichtig — Fallback auf Console" |
 
**Funktions-Diff:**
 
```
❌ Verbotener Fix:                    ✅ Korrekter Fix:
VORHER:                               VORHER:
- Logging      ✅                     - Logging      ✅
- File-Logger  ❌ (Bug)               - File-Logger  ❌ (Bug)
 
NACHHER:                              NACHHER:
- Logging      ❌ (ENTFERNT!)         - Logging      ✅ (erhalten)
- File-Logger  — (nicht mehr da)      - File-Logger  ✅ (gefixt + Fallback)
```
 
---
 
## Zusammenfassung: Checkliste fuer jeden Fix
 
Vor dem Commit jedes Fixes diese Checkliste mental durchgehen:
 
```
□ Root Cause identifiziert (nicht nur Symptom)?
□ Verwandte Fehlerquellen in 3 Dimensionen geprueft?
□ Fix ist self-healing, defensiv, ueberlebbar?
□ Fix-Induced-Failure-Pruefung (8 Punkte) bestanden?
□ Funktionalitaets-Diff: Alle Features weiterhin ✅?
□ KEINE Funktionalitaet entfernt, auskommentiert oder geschluckt?
□ Mindestens 2 Absicherungsschichten (praeventiv + reaktiv)?
□ Poka-Yoke geprueft (kann der Fehler eliminiert werden)?
□ Memory mit Root Cause, Fix und Muster-Erkennung gespeichert?
□ Proaktiver Intelligenz-Vorschlag gemacht (wenn sinnvoll)?
```
 
**Wenn auch nur EIN Punkt fehlt, ist der Fix NICHT fertig.**