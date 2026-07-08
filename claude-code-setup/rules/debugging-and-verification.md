# Debugging & Verifikation: Systematisch statt Trial-and-Error (KRITISCH)

> Ergaenzt `resilient-bugfixing.md` (Direktive #3, fuer den eigentlichen Fix).

## 1. Confidence-Ampel: Unsicherheit erkennen

| Farbe | Bedeutung | Aktion |
|-------|-----------|--------|
| **Gruen** | in diesem Block gelesen/ausgefuehrt ODER max 5 Turns zurueck | sicher verwenden |
| **Gelb** | >5 Turns zurueck / fruehere Session / Training | im Zweifel nachschlagen |
| **Rot** | Vermutung/Schaetzung | STOP — genau 1 Nachschlage-Aufruf (Read/Grep/WebSearch); danach unklar → als Schaetzung markieren |

PFLICHT bei: Versionsnummern, Dateipfaden (Existenz pruefen!), API-Parametern, CLI-Flags, JSON/YAML-Keys.
NICHT noetig bei allgemeinen Konzepten/Architektur/Erklaerungen.

## 2. Inspect Before Guessing

IMMER den tatsaechlichen Zustand inspizieren bevor Code geaendert wird: Web → DevTools; API → echte
Response lesen (nicht Doku annehmen); Filesystem → `ls`/`stat`/Datei lesen; Prozesse → `ps`/Port pruefen.
Haeufige Falschannahmen nicht raten (contenteditable, Feldgroesse, aria-label-Sprache, Eltern-Element, CSS-Klassen).

## 3. Bug-Datenbank durchsuchen VOR dem Debuggen (CBR)

Vor jedem neuen Fehler ZUERST `~/proggs/.claude/agent-memory/shared/bug-cases.jsonl` durchsuchen
(Grep nach Symptom/Fehlermeldung). 4 Phasen: **Retrieve** (durchsuchen) → **Reuse** (alten Fix als
ersten Ansatz) → **Revise** (anpassen) → **Retain** (neuen Fall eintragen).
Format: `{"date","symptom","root_cause","fix","files","tags","severity"}`. Durchsuchen bei jedem
Build-Fehler/fehlgeschlagenen Befehl. Schreiben nach jedem Fix >5 Min ODER bei 2. Auftreten (ALARM).

## 4. Hypothesen-basiertes Debugging

**Stufenregel (Sonden VOR dem Raten):**

| Stufe | Situation | Aktion | Sonden? |
|-------|-----------|--------|---------|
| 1 | Fehlermeldung eindeutig (Compiler, Import, Tippfehler) | direkt fixen — die Meldung IST die Diagnose | NEIN |
| 2 | Root Cause nach 30 s unklar | SOFORT Logging-Sonden, NICHT raten | **JA** |
| 3 | erster Fix-Versuch gescheitert | Sonden Pflicht fuer jeden weiteren Versuch | **JA** |

Sonden bei Stufe 2 (~500-1000 Token) sind GUENSTIGER als ein gescheiterter Rateversuch (~2000-5000 Token).

**Sonden-Muster:** Funktion identifizieren die den Fehler ausloest (function-level) → Eingabewerte am
Eingang loggen → an Verzweigungen welcher Pfad → Rueckgabewert am Ausgang → LAUFEN lassen, Logs LESEN →
DANN Hypothese aus echten Daten.

**4-Schritte-Loop (Stufe 2+3):** 2-3 Hypothesen formulieren (jede benennt konkrete **Funktion**, nach
Wahrscheinlichkeit) → instrumentieren (`Log.d`/`console.log`/`print`/`Debug.WriteLine`) → Runtime-Daten
analysieren (max 2 Runden, dann Minimal-Repro anfordern) → gezielter Fix (Kommentar was beobachtet wurde,
Debug-Logging danach ENTFERNEN).

Was NIEMALS: Fix vorschlagen bevor Laufzeitdaten vorliegen · >3 Hypothesen/>2 Runden ohne Daten ·
Hypothese ohne konkrete Funktion · Debug-Logging nach dem Fix im Code lassen.
