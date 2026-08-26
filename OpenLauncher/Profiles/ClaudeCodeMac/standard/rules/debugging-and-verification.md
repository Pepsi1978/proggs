# Debugging & Verifikation: Systematisch statt Trial-and-Error (KRITISCH)

> Ergaenzt `resilient-bugfixing.md` (Direktive #3).

## 1. Confidence-Ampel
**Gruen** (in diesem Block gelesen ODER max 5 Turns) → sicher. **Gelb** (aelter/Training) → nachschlagen.
**Rot** (Vermutung) → STOP, 1 Nachschlag, sonst als Schaetzung markieren. PFLICHT bei: Versionen, Pfaden
(Existenz!), API-Parametern, CLI-Flags, JSON-Keys.

## 2. Inspect Before Guessing
Zustand inspizieren bevor Code geaendert wird: API → echte Response (nicht Doku annehmen); Filesystem →
`ls`/`stat`; Prozesse → `ps`/Port. Nicht raten.

## 3. Bug-Datenbank ZUERST (CBR)
Vor jedem neuen Fehler `bug-cases.jsonl` durchsuchen (Grep nach Symptom). 4 Phasen: Retrieve → Reuse →
Revise → Retain. Schreiben nach Fix >5 Min / 2. Auftreten.

## 4. Hypothesen-basiert (Sonden VOR dem Raten)
Stufe 1 (Meldung eindeutig) → direkt fixen. Stufe 2 (Root Cause nach 30 s unklar) → SOFORT Logging-Sonden.
Stufe 3 (Fix gescheitert) → Sonden Pflicht. **Muster:** Funktion identifizieren → Eingaben/Verzweigungen/
Rueckgabe loggen → LAUFEN lassen, Logs LESEN → DANN Hypothese (2-3, je konkrete Funktion; max 2 Runden;
Debug-Logging danach ENTFERNEN).

## 5. Bei viel Entropie: reduzieren
Signal: 3+ wirkungslose Fixes · Flailing >15 Min · gleicher Fehler wiederholt. Reaktion: STOPP →
nachschlagen ob bekannt (ZUERST lokal `bugs/`+`bug-cases.jsonl`, DANN Internet) → einfachsten Fix zuerst →
vereinfachen. Praevention: `known-bugs-before-coding.md`.
