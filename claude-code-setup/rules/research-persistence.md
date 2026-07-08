# Research-Persistenz: Recherchen in Best Practices & Bug-Almanache einarbeiten (KRITISCH)

> Recherchen duerfen nicht "verkommen" — wer spaeter im gleichen Bereich arbeitet, muss auf das schon
> recherchierte Wissen zurueckgreifen koennen.

## Grundregel
Nach JEDEM Web-Recherche-Einsatz pruefen, ob die Ergebnisse taugen. Wenn ja, IMMER in Best-Practices
einarbeiten — enthaltene Bugs/Fallen zusaetzlich in die Bug-Almanache. Jeweils Kurzcheck (Stufe A) UND
Volltext. Erst dann fertig. Gilt fuer ALLE Recherche-Wege (Agents, deep-research, eigene WebSearch/WebFetch).

## Pflicht-Ablauf (vor Aufgabenabschluss)
1. Tauglichkeit: "ueber die aktuelle Aufgabe hinaus wiederverwendbar?"
2. Wenn JA → `best-practices/<kat>/<bereich>.md` ergaenzen (Kurzcheck-Tabelle Stufe A + Volltext; Format
   der bestehenden Dateien: Stand-Datum, Versions-Anker, Quellen, `offiziell`/`extern`).
3. Bugs/Fallen enthalten → zusaetzlich `bugs/<kat>/<bereich>.md` (Symptom, Ursache, Versionen,
   funktionserhaltender Fix, Quelle) — Kurzcheck UND Volltext; neuer Bereich → Datei + README-Index.
4. Wenn NEIN: in EINEM Satz begruenden. 5. Committen+pushen (nur eigene Dateien).

## Tauglich oder nicht?
TAUGLICH: Patterns/APIs/Architektur, bekannte Bugs/Workarounds, Library-Vergleiche, Plattform-/Policy-,
Harness-Wissen. NICHT: einmalige Faktenabfrage, rein projektspezifischer Zustand. Im Zweifel: einarbeiten.

## Arbeitsteilung
Researcher markiert am Ende `BEST-PRACTICES-KANDIDATEN:` + `BUG-KANDIDATEN:` (inkl. URLs + Versionen;
keine → `KEINE`). Hauptagent konsolidiert, prueft, arbeitet ein (Kurzcheck + Volltext), committet — nie
parallel schreibende Researcher.

## Was NIEMALS
- Ergebnisse verwerfen ohne Tauglichkeits-Pruefung · Recherche "nur im Chat" lassen · nur Volltext ODER
  nur Kurzcheck · Bugs nur in Best Practices statt auch im Almanach · Quellen/Versionen/Stand weglassen.
