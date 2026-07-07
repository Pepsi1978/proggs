Debugging & Verifikation: Systematisch statt Trial-and-Error

## 1. Confidence-Ampel (bei technisch praezisen Aussagen)
- **Gruen:** in diesem Block gelesen/ausgefuehrt oder max. 5 Turns zurueck -> sicher verwenden.
- **Gelb:** ueber 5 Turns zurueck oder aus frueherer Session/Training -> im Zweifel nachschlagen.
- **Rot:** Vermutung/Schaetzung -> STOP, genau 1 Nachschlag (Read/Grep/WebSearch); danach noch unklar -> explizit als Schaetzung markieren.

Pflicht bei: Versionsnummern, Dateipfaden (Existenz pruefen!), API-Parametern, CLI-Flags, Config-Keys. Nicht noetig bei allgemeinen Konzepten/Erklaerungen.

## 2. Inspect before guessing
IMMER den tatsaechlichen Zustand inspizieren bevor Code geaendert wird: DevTools (Web/Electron), echte API-Response lesen (nicht Doku annehmen), `ls`/`stat`/Datei lesen (Filesystem), Prozess/Port pruefen. Nicht raten — CSS-Klassen, aria-label, Feldgroessen, Eltern-Elemente koennen anders sein als angenommen.

## 3. Bug-Datenbank ZUERST durchsuchen (CBR)
Vor dem Debuggen `~/proggs/.claude/agent-memory/shared/bug-cases.jsonl` per Grep nach Symptom/Fehlermeldung durchsuchen. 4 Phasen: Retrieve (suchen) -> Reuse (alten Fix anwenden) -> Revise (anpassen) -> Retain (neuen Fall eintragen). Durchsuchen bei JEDEM Build-Fehler/unklaren Fehler. Schreiben nach jedem Fix der ueber 5 Min dauerte UND bei jedem Fehler der zum 2. Mal auftrat.

## 4. Hypothesen-Debugging (Sonden-Stufen)
- **Stufe 1:** Fehlermeldung eindeutig (Compiler, falscher Import, Tippfehler) -> direkt fixen, keine Sonden.
- **Stufe 2:** Root Cause nach 30 Sekunden noch unklar -> SOFORT Logging-Sonden einbauen (PFLICHT), NICHT raten. Ein Sonden-Durchlauf ist guenstiger als ein Fehlversuch.
- **Stufe 3:** erster Fix-Versuch gescheitert -> Sonden PFLICHT fuer jeden weiteren Versuch.

Ablauf: 2-3 Hypothesen formulieren (jede benennt eine konkrete FUNKTION) -> instrumentieren (Eingaben/Verzweigungen/Rueckgabe loggen) -> Code laufen lassen + Logs LESEN -> gezielter Fix auf Basis echter Daten. Max 2 Runden. Debug-Logging nach dem Fix ENTFERNEN.

## NIEMALS
- Fix vorschlagen bevor Laufzeitdaten vorliegen (Stufe 2+3).
- Mehr als 3 Hypothesen oder eine Hypothese ohne konkrete Funktion.
- Debug-Logging im Code lassen nach dem Fix.
