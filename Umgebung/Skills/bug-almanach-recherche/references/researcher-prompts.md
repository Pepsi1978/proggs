# Researcher-Prompt-Vorlagen

Fertige Prompts fuer den Researcher-Schwarm. `[BEREICH]` durch den konkreten Bereich
ersetzen (z.B. "Chrome-Erweiterungen / Manifest V3", "Android Jetpack Compose",
"Claude Code Hooks"), `[VERSION]` durch die in Schritt 1 live ermittelte Version,
`[DATUM]` durch das heutige Datum. Alle Researcher laufen auf dem hoechsten Opus-Modell
(Modell-Policy) — `opts.model` NICHT setzen. Agent-Typ: `researcher`.

Pro Researcher gilt: max 15 Web-Fetches, max 10 Minuten, max ~40 Eintraege, KOMPAKTE
strukturierte Rueckgabe (~1500-2000 Token, kein Rohdaten-Dump). Pro Bug zurueckgeben:
**Titel · Symptom · Ursache · Loesung (funktionserhaltend!) · betroffene Versionen ·
Quelle (URL)**.

---

## Phase A — Breite Bug-Suche (Schritt 2): 3-5 Researcher parallel

### Gemeinsamer Prompt-Kopf (vor jeden Teilbereich setzen)

> Du recherchierst BUGS, Fallen und Workarounds fuer **[BEREICH]**. Ziel: ein
> kuratierter Bug-Almanach. Aktuell installierte Version: **[VERSION]** (heute [DATUM]).
> WICHTIG: Achte BESONDERS auf NEUE Versionen — recherchiere gezielt aktuelle
> Bugs/Changelog-Eintraege, nicht nur alte. Aber nimm ALLES mit, was du findest.
>
> Fuer JEDEN gefundenen Bug gib zurueck: Titel · Symptom · Ursache · Loesung (konkret
> und FUNKTIONSERHALTEND — niemals "Feature weglassen") · betroffene Versionen (ab/bis;
> gefixt ab? oder "per Design") · Quelle (URL).
>
> LIMITS (strikt): max 15 Web-Fetches, max 10 Minuten, max ~40 Eintraege. KOMPAKTE
> strukturierte Liste (~1500-2000 Token, KEIN Rohdaten-Dump). Wenn du zu einem Punkt
> nichts Konkretes findest, sag das ehrlich statt zu raten.
>
> DEIN TEILBEREICH:

### Teilbereich 1 — Offizielle Doku + Hersteller-Hilfen (Vorrang vor Foren)
> Offizielle Dokumentation, Changelog, Release-Notes UND offizielle Support-/Hilfe-Seiten
> von [BEREICH]. Welche Bugs wurden in welcher Version gemeldet/gefixt? Welche
> dokumentierten Einschraenkungen, Workarounds und "known issues" gibt es? Suche GEZIELT
> nach offiziellen Empfehlungen und DIREKTEN HILFEN ZUM KONKRETEN Bug — nicht nur
> allgemeine Doku. Bei Hersteller-eigenen Bugs direkt beim Hersteller schauen:
> Anthropic-Docs bei Claude, JetBrains bei Kotlin, Gradle-Docs bei Gradle, Apple-
> Developer bei Swift, Microsoft-Learn bei .NET, Chrome-Developer bei Erweiterungen.
> Offizielle Loesungen haben Vorrang vor Foren-Meinungen und gehoeren zuerst ins FIX-Feld.

### Teilbereich 2 — GitHub-Issues / Bugtracker
> Der offizielle Bugtracker (z.B. github.com/<org>/<repo>/issues). Hook-/bereichsbezogene
> Bug-Reports, OFFEN und kuerzlich GESCHLOSSEN. Echte gemeldete Bugs mit Symptom +
> Workaround/Fix-Status. (Falls WebFetch auf den Tracker blockiert ist: Such-Snippets
> nutzen und das ehrlich vermerken.)

### Teilbereich 3 — Community / Praxis
> Reddit, dev.to, Medium, persoenliche Blogs, Hacker News, Stack Overflow. Welche
> praktischen Stolperfallen erleben Entwickler bei [BEREICH]? Haeufige Fehler + ihre
> Loesungen. Was "funktioniert nicht wie erwartet"?

### Teilbereich 4 — Plattform-Fallen (Windows UND macOS/Linux)
> Plattform-spezifische Probleme bei [BEREICH]. Windows: Encoding/BOM, Pfade, Shell-
> Quoting, ExecutionPolicy, Tool-Versionen. macOS/Linux: Permissions (+x), Shell-Fallen
> (`set -e`), Pfad-Aufloesung, CRLF/LF, fehlende Tools (jq etc.). Echte, konkrete Bugs + Fixes.

### Teilbereich 5 — Mechanik / bereichsspezifisch
> Die typischen konzeptionellen Fehler von [BEREICH] (das, was Einsteiger und auch
> Fortgeschrittene immer wieder falsch machen). Pro Bereich anpassen — z.B. bei Hooks:
> Exit-Codes, JSON-Schema, Matcher, Events; bei Compose: Recomposition, State, Effects;
> bei Gradle: Konfigurations-Cache, R8, Dependency-Konflikte.

---

## Phase B — Fix-Status (Schritt 3): 2-3 Researcher parallel

> Diese Phase ist der Kern. Sie verhindert, dass laengst gefixte Bugs als aktiv gelten.

### Fix-Researcher 1 — Changelog der Versionen durchgehen
> Du recherchierst, welche [BEREICH]-Bugs in neueren Versionen bereits GEFIXT wurden.
> Aktuell installiert: [VERSION] (heute [DATUM]). Gehe das offizielle Changelog / die
> Release-Notes von der aeltesten relevanten Version bis [VERSION] durch und liste JEDEN
> Eintrag, der einen bereichsbezogenen BUG FIX oder eine Verhaltensaenderung beschreibt.
> Pro Fix: Version · was gefixt wurde (1 Satz) · welches frueher buggy Verhalten damit
> weg ist. LIMITS: max 12 Fetches, max 8 Min, ~1200 Token. Ehrlich sein, wenn eine
> Version nichts Bereichsbezogenes hatte.

### Fix-Researcher 2 — Issue-Status der konkreten Bugs pruefen
> Du pruefst den aktuellen STATUS (offen/geschlossen/gefixt) konkreter Bug-Tickets aus
> Phase A. [Liste der wichtigsten Issue-Nummern/URLs einfuegen.] Aktuell installiert:
> [VERSION].
>
> **Bei GitHub-Trackern die GitHub-CLI nutzen (installiert + authentifiziert), NICHT WebFetch
> (das liefert nur vage Snippets):**
> - Einzeln: `gh issue view <nr> --repo <org>/<repo> --json number,state,title,closedAt,stateReason`
>   → harter OPEN/CLOSED-Status + closedAt + stateReason (COMPLETED / NOT_PLANNED = Duplikat/won't-fix).
> - Mehrere: `gh issue list --repo <org>/<repo> --search "<stichwort>" --state all --json number,state,title`.
>
> Pro Issue: Nummer · Status (OFFEN/GESCHLOSSEN) · falls geschlossen: closedAt + stateReason ·
> 1 Satz Beleg. LIMITS: max 15 Aufrufe, max 9 Min. Nur wenn ein Tracker NICHT ueber gh
> erreichbar ist (GitLab/Bugzilla), auf WebFetch ausweichen — unklare Faelle ehrlich als
> "unklar" markieren statt zu raten.

### Fix-Researcher 3 — Sekundaerquellen als Gegenprobe (optional)
> Du suchst in Blogs, dev.to, Medium, Reddit, Changelog-Zusammenfassungen nach Aussagen,
> welche [BEREICH]-Bugs in den neuesten Versionen behoben wurden. Pro Fix: was · Version ·
> Quelle. LIMITS: max 12 Fetches, max 8 Min, ~1000 Token. Nicht raten — nur Belegtes.

---

## Auswertung der Fix-Recherche

Die Ergebnisse von Phase B gegen die Bug-Liste aus Phase A abgleichen und in zwei Toepfe sortieren:

| Topf | Kriterium | Im Almanach |
|------|-----------|-------------|
| **Gefixt** | Changelog-belegt in einer Version ≤ installierte Version behoben | `Versionen:`-Feld "gefixt ab vX"; in die Fix-Status-Tabelle |
| **Noch offen** | per Design ODER kein Fix belegt ODER Status unklar | bleibt aktiver Eintrag; in die "noch nicht gefixt"-Liste |

Im Zweifel: **noch offen**. Lieber einen gefixten Bug zu viel dokumentiert (mit Hinweis
"gefixt ab vX") als einen aktiven Bug uebersehen.
