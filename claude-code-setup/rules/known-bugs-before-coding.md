# Bekannte Bugs ZUERST lesen, bevor an einer Technologie gearbeitet wird (KRITISCH)

> Dauerhafte Regel (2026-06-01), Poka-Yoke Stufe 3. System: `~/proggs/bugs/SYSTEM.md`,
> Index: `~/proggs/bugs/README.md`. Ausloeser: Chrome-Extension-Bug — ~1h verschwendet, weil der
> bekannte Workaround nicht vorher nachgeschlagen wurde.

## Grundregel

Pro Technologie-Bereich gibt es einen kuratierten Bug-Almanach `~/proggs/bugs/<kategorie>/<bereich>.md`.
**BEVOR** an einem Bereich mit Almanach gearbeitet wird, MUSS dessen **Kurzcheck** ZUERST gelesen werden
— noch vor der ersten Code-Aenderung. So werden bekannte Fehler gar nicht erst gemacht. (Unterschied zu
`bug-cases.jsonl`: die wird REAKTIV nach einem Fehler durchsucht; der Almanach PROAKTIV vor der Arbeit.)

## Digest-Modell: 4 Stufen

Jeder Almanach + jede Best-Practices-Datei traegt oben eine **Kurzcheck**-Sektion (Signale +
Sofort-Regeln, erste 80 Zeilen).

| Stufe | Wann | Was lesen |
|-------|------|-----------|
| **A — Kurzcheck vorab** | vor JEDER echten Bereichsarbeit | NUR Kurzcheck: `Read limit=80` auf Almanach, DANN ebenso Best-Practices. Vom `bug-almanac-guard` erzwungen. |
| **B — Volltext bei Fehler** | ab dem ERSTEN Fehler im Bereich | SOFORT Almanach-VOLLTEXT (`Read` ohne limit). |
| **C — Volltext vorab** | vor Arbeit in Hochrisiko-Bereich | Almanach-VOLLTEXT schon VORAB (Guard erzwingt). |
| **D — Wiederkehrender Bug** | Fehler wurde schon gefixt, tritt ERNEUT auf | Kurzcheck ueberspringen → sofort VOLLTEXT von Almanach UND Best-Practices; keine Loesung darin → Grundproblem per `research`-Skill recherchieren. |

**Hochrisiko-Bereiche (Stufe C):** `r8`, `firebase-billing`, `claude-hooks`, `claude-config` — Liste im
`bug-almanac-guard` (`$highRiskKeys`) + hier, synchron halten. Warum Kurzcheck statt Volltext-Zwang:
Volltext fuer alle kostete ~16-23k Token/Bereich/Session (Context-Rot); der Kurzcheck (~500 Token)
erhaelt die Erkennung, der Volltext bleibt per Pfad erreichbar und wird bei B/C/D erzwungen (verlustfrei).

## Reihenfolge & Automatik

Erst Almanach (was schiefgeht + Loesung), dann Best-Practices (`best-practices/**`, wie man es richtig
macht), dann coden — der `bug-almanac-guard` erzwingt genau diese Reihenfolge (blockiert bereichstypische
Edits bis beide Kurzchecks gelesen sind; bei Stufe C Almanach-Volltext). Keine Best-Practices-Datei →
nur der Almanach zaehlt. Automatik je Session: `bug-almanac-index` (SessionStart) · `bug-almanac-hint`
(UserPromptSubmit) · `bug-almanac-guard` (PreToolUse, BLOCKIERT) · `bug-case-auto-writer` (PostToolUseFailure).

## Pflicht-Ablauf

1. Bereich (+ ggf. Software-Version live) erkennen. 2. Trivialer Kleinkram (String, Doku, Kommentar,
   Versions-Bump)? → weiter ohne Almanach. 3. Im Index pruefen ob ein Almanach existiert.
4. **Almanach vorhanden** → Stufe waehlen (Normal = Kurzcheck `limit=80`; Hochrisiko = Volltext), danach
   Best-Practices-Kurzcheck, dann arbeiten. 5. **Kein Almanach** → Guard blockiert, Frank melden, auf
   Entscheidung warten: **(a) Recherche** (Standard) — nach Franks OK den Skill `bug-almanach-recherche`
   STARTEN (nicht selbst ad hoc recherchieren); **(b) Quittung** (nur Kleinkram) — leere Datei
   `bug-almanac-ack-<slug>.flag` im TEMP. Notaus bei Fehlalarm: `bug-almanac-disable.flag` im TEMP.
6. **Fehler waehrend der Arbeit** → Stufe B (Volltext); ist es eine Wiederholung → Stufe D. 7. **Nach**
   der Aufgabe: jeden NEU erlebten Bug im Almanach ergaenzen (Bug + Loesung + Versionen, Stand-Header).
"Erst Franks OK" gilt NUR fuer die gezielte Almanach-Recherche; ein kurzes Web-Lookup beim Debuggen bleibt frei.

## Gilt AUCH fuer Claude-eigene Harness-Arbeit

| Ich aendere … | Zuerst lesen | Stufe |
|---------------|--------------|-------|
| Hook (`*.ps1`/`*.sh`) | `bugs/claude-tooling/claude-hooks.md` | C |
| CLAUDE.md, Regel, Settings, Skill, Command, Agent | `bugs/claude-tooling/claude-config.md` + Best-Practices | C |
| MCP-Server | `bugs/claude-tooling/mcp-server.md` | A |
| Python-Hilfsskript | `bugs/claude-tooling/python-windows.md` | A |

Weitere: Chrome-Extension → `bugs/web/chrome-extensions.md`; Android/WPF/Swift/TS/Gradle →
`bugs/<kategorie>/<bereich>.md`. Komplett neue Sprache → Guard blockt generisch → Recherche oder Quittung.

## Was NIEMALS passieren darf

- An einem Bereich mit Almanach arbeiten ohne mindestens den Kurzcheck (Stufe A)
- Nach einem Fehler weiterarbeiten ohne Volltext (B); in Hochrisiko nur Kurzcheck statt Volltext (C)
- Bei wiederkehrendem Bug nur den Kurzcheck statt Volltext von Almanach + Best-Practices (D)
- Almanach-Kurzcheck lesen, aber die Best-Practices-Datei ueberspringen (Reihenfolge: erst Almanach, dann BP)
- Einen erlebten Bug fixen ohne ihn danach im Almanach zu ergaenzen
- Almanach-Recherche ohne Franks OK starten, oder bei neuem Bereich selbst ad hoc recherchieren; Quittung reflexhaft setzen
- Einen Bug "loesen", indem Funktionalitaet entfernt wird (Direktive #3 — funktionserhaltend)
