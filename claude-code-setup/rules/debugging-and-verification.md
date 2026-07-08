# Debugging & Verifikation: Systematisch statt Trial-and-Error (KRITISCH)

> Ergaenzt `resilient-bugfixing.md` (Direktive #3, fuer den eigentlichen Fix).

## 1. Confidence-Ampel: Unsicherheit erkennen

| Farbe | Bedeutung | Aktion |
|-------|-----------|--------|
| **Gruen** | in diesem Block gelesen/ausgefuehrt ODER max 5 Turns zurueck | sicher verwenden |
| **Gelb** | >5 Turns zurueck / fruehere Session / Training | im Zweifel nachschlagen |
| **Rot** | Vermutung/Schaetzung | STOP — genau 1 Nachschlage-Aufruf (Read/Grep/WebSearch); danach unklar → als Schaetzung markieren |

PFLICHT bei: Versionsnummern, Dateipfaden (Existenz pruefen!), API-Parametern, CLI-Flags, JSON/YAML-Keys.
NICHT bei allgemeinen Konzepten/Architektur/Erklaerungen.

## 2. Inspect Before Guessing

IMMER den tatsaechlichen Zustand inspizieren bevor Code geaendert wird: Web → DevTools; API → echte
Response lesen (nicht Doku annehmen); Filesystem → `ls`/`stat`/Datei lesen; Prozesse → `ps`/Port pruefen.
Haeufige Falschannahmen nicht raten (contenteditable, Feldgroesse, aria-label-Sprache, Eltern-Element, CSS-Klassen).

## 3. Bug-Datenbank durchsuchen VOR dem Debuggen (CBR)

Vor jedem neuen Fehler ZUERST `~/proggs/.claude/agent-memory/shared/bug-cases.jsonl` durchsuchen (Grep
nach Symptom/Fehlermeldung). 4 Phasen: **Retrieve** (durchsuchen) → **Reuse** (alten Fix als ersten
Ansatz) → **Revise** (anpassen) → **Retain** (neuen Fall eintragen). Format:
`{"date","symptom","root_cause","fix","files","tags","severity"}`. Durchsuchen bei jedem
Build-Fehler/fehlgeschlagenen Befehl. Schreiben nach jedem Fix >5 Min ODER bei 2. Auftreten (ALARM).

## 4. Hypothesen-basiertes Debugging

Stufenregel (Sonden VOR dem Raten):

| Stufe | Situation | Aktion | Sonden? |
|-------|-----------|--------|---------|
| 1 | Fehlermeldung eindeutig (Compiler, Import, Tippfehler) | direkt fixen — die Meldung IST die Diagnose | NEIN |
| 2 | Root Cause nach 30 s unklar | SOFORT Logging-Sonden, NICHT raten | **JA** |
| 3 | erster Fix-Versuch gescheitert | Sonden Pflicht fuer jeden weiteren Versuch | **JA** |

Sonden bei Stufe 2 (~500-1000 Token) sind GUENSTIGER als ein gescheiterter Rateversuch (~2000-5000).
**Sonden-Muster:** Funktion identifizieren, die den Fehler ausloest → Eingabewerte am Eingang loggen → an
Verzweigungen welcher Pfad → Rueckgabewert am Ausgang → LAUFEN lassen, Logs LESEN → DANN Hypothese.
**4-Schritte-Loop (Stufe 2+3):** 2-3 Hypothesen (jede benennt konkrete Funktion, nach Wahrscheinlichkeit)
→ instrumentieren (`Log.d`/`console.log`/`print`) → Runtime-Daten analysieren (max 2 Runden, dann
Minimal-Repro) → gezielter Fix (Kommentar was beobachtet, Debug-Logging danach ENTFERNEN).
NIEMALS: Fix vorschlagen bevor Laufzeitdaten vorliegen · >3 Hypothesen/>2 Runden ohne Daten · Hypothese
ohne konkrete Funktion · Debug-Logging nach dem Fix im Code lassen.

## 5. Bei viel Entropie: entropie-reduzierend reagieren

**Signal** (Chaos, kein Fortschritt): 3+ wirkungslose Fixes (= 3-Iterationen-Stop) · Flailing am selben
Symptom >15-30 Min · gleicher Fehler wiederholt mit immer neuen ungetesteten Fixes. Hohe Entropie ist das
SIGNAL, die Strategie zu wechseln — NICHT weiter Chaos hinzufuegen.
**Reaktion:** 1. STOPP (aufhoeren zu raten). 2. Nachschlagen ob der Bug bekannt ist — ZUERST lokal
(Bug-Almanach `~/proggs/bugs/` + `bug-cases.jsonl`), DANN Internet (der Workaround existiert meist schon).
3. Den EINFACHSTEN dokumentierten Fix zuerst. 4. Vereinfachen statt verkomplizieren (sauberer
Ausgangszustand, eine Variable nach der anderen). Beste Reduktion = Praevention: bekannte Bugs VOR der
Arbeit nachschlagen (`known-bugs-before-coding.md`).
