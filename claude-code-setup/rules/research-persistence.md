# Research-Persistenz in Best Practices & Almanache (KRITISCH)

## Grundregel
Nach JEDER Web-Recherche pruefen, ob Ergebnisse taugen; wenn ja, IMMER in Best-Practices einarbeiten,
enthaltene Bugs/Fallen zusaetzlich in Bug-Almanache. Je Kurzcheck+Volltext. Gilt fuer ALLE Wege (Agents,
deep-research, WebSearch/WebFetch).

## Pflicht-Ablauf
1. Tauglich: "ueber die Aufgabe hinaus wiederverwendbar?"
2. JA -> `best-practices/<kat>/<bereich>.md` (Kurzcheck Stufe A + Volltext; Format: Stand-Datum,
   Versions-Anker, Quellen, `offiziell`/`extern`).
3. Bugs/Fallen -> zusaetzlich `bugs/<kat>/<bereich>.md` (Symptom, Ursache, Versionen, funktionserhaltender
   Fix, Quelle) -- Kurzcheck UND Volltext; neuer Bereich -> Datei + README-Index.
4. NEIN: in EINEM Satz begruenden. 5. Committen+pushen (nur eigene Dateien).

## Tauglich?
TAUGLICH: Patterns/APIs/Architektur, bekannte Bugs/Workarounds, Library-Vergleiche, Plattform-/Policy-,
Harness-Wissen. NICHT: einmalige Faktenabfrage, projektspezifisch. Im Zweifel: einarbeiten.

## Arbeitsteilung
Researcher markiert `BEST-PRACTICES-KANDIDATEN:` + `BUG-KANDIDATEN:` (URLs+Versionen; keine -> `KEINE`).
Hauptagent arbeitet ein + committet -- nie parallel schreibende Researcher.

## Was NIEMALS
- Ergebnisse verwerfen ohne Tauglichkeits-Pruefung; Recherche "nur im Chat" lassen; nur Volltext ODER nur
  Kurzcheck; Bugs nur in Best Practices statt auch im Almanach; Quellen/Versionen/Stand weglassen.
