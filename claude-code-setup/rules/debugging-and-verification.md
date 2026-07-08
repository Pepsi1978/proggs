# Debugging & Verifikation: Systematisch statt Trial-and-Error (KRITISCH)

> Ergaenzt `resilient-bugfixing.md` (Direktive #3).

## 1. Confidence-Ampel
**Gruen** (in diesem Block gelesen/ausgefuehrt ODER max 5 Turns) → sicher. **Gelb** (aelter/Training) →
nachschlagen. **Rot** (Vermutung) → STOP, 1 Nachschlage-Aufruf; unklar → als Schaetzung markieren. PFLICHT
bei: Versionen, Dateipfaden (Existenz!), API-Parametern, CLI-Flags, JSON/YAML-Keys. Nicht bei Konzepten.

## 2. Inspect Before Guessing
Tatsaechlichen Zustand inspizieren bevor Code geaendert wird: Web → DevTools; API → echte Response (nicht
Doku annehmen); Filesystem → `ls`/`stat`; Prozesse → `ps`/Port. Falschannahmen nicht raten.

## 3. Bug-Datenbank ZUERST (CBR)
Vor jedem neuen Fehler `bug-cases.jsonl` durchsuchen (Grep nach Symptom). 4 Phasen: Retrieve → Reuse →
Revise → Retain. Format `{date,symptom,root_cause,fix,files,tags,severity}`. Schreiben nach Fix >5 Min ODER 2. Auftreten.

## 4. Hypothesen-basiert (Sonden VOR dem Raten)
Stufe 1 (Meldung eindeutig) → direkt fixen. Stufe 2 (Root Cause nach 30 s unklar) → SOFORT Logging-Sonden.
Stufe 3 (erster Fix gescheitert) → Sonden Pflicht. Sonden (~500-1000 Token) < gescheiterter Rateversuch.
**Muster:** Funktion identifizieren → Eingaben/Verzweigungen/Rueckgabe loggen → LAUFEN lassen, Logs LESEN →
DANN Hypothese. Loop: 2-3 Hypothesen (je konkrete Funktion) → instrumentieren → Runtime-Daten (max 2
Runden) → gezielter Fix (Debug-Logging danach ENTFERNEN). NIEMALS: Fix vor Laufzeitdaten · Hypothese ohne Funktion.

## 5. Bei viel Entropie: reduzieren
Signal: 3+ wirkungslose Fixes · Flailing >15-30 Min · gleicher Fehler wiederholt. Reaktion: 1. STOPP.
2. Nachschlagen ob bekannt — ZUERST lokal (`bugs/` + `bug-cases.jsonl`), DANN Internet. 3. Einfachsten
dokumentierten Fix zuerst. 4. Vereinfachen. Praevention: bekannte Bugs vorher (`known-bugs-before-coding.md`).
