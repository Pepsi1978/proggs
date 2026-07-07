Research-Persistenz: Recherchen in Best Practices & Bug-Almanache einarbeiten

Nach JEDEM Web-Recherche-Einsatz pruefen, ob die Ergebnisse als Best Practices taugen. Wenn ja: IMMER in `~/proggs/best-practices/` einarbeiten — und enthaltene Bugs/Fallen ZUSAETZLICH in den Bug-Almanach `~/proggs/bugs/`. Jeweils Kurzversion (Kurzcheck) UND Langversion (Volltext). Erst dann ist die Aufgabe fertig. Recherchen duerfen nicht "verkommen".

Gilt fuer ALLE Recherche-Wege: researcher-Agents, deep-research/Schwaerme, eigene `WebSearch`/`WebFetch` mitten in einer Aufgabe, Subagent-Recherchen.

## Pflicht-Ablauf (vor Aufgabenabschluss)
1. **Tauglichkeit pruefen:** Sind die Findings ueber die aktuelle Aufgabe hinaus wiederverwendbar?
2. **Wenn JA — Best Practices:** passende Datei `best-practices/<kategorie>/<bereich>.md` ergaenzen oder neu anlegen (bestehende Kategorien nutzen: android, apis, desktop, web, claude-tooling ...). BEIDE Ebenen pflegen: Kurzcheck-Tabelle (erste 80 Zeilen) UND Volltext.
3. **Wenn Bugs/Fallen dabei — zusaetzlich Almanach:** in `bugs/<kategorie>/<bereich>.md` (Symptom, Ursache, Versionen, funktionserhaltender Fix, Quelle), ebenfalls Kurzcheck + Volltext. Neuer Bereich -> Datei anlegen + `bugs/README.md`-Index ergaenzen.
4. **Wenn NEIN (nicht tauglich):** in 1 Satz begruenden (bewusste Entscheidung statt stilles Weglassen).
5. **Committen + pushen** (nur eigene Dateien).

Texte selbsterklaerend halten: Stand-Datum, Versions-Anker, Quellen-Links — eine spaetere Session muss es ohne heutigen Kontext verstehen. Im Zweifel: einarbeiten (eine zu viel persistierte Erkenntnis kostet wenig, eine verlorene Recherche kostet die ganze Wiederholung).

## NIEMALS
- Recherche-Ergebnisse nach der Aufgabe verwerfen ohne Tauglichkeits-Pruefung.
- Eine Recherche "nur im Chat" lassen, obwohl sie tauglich ist.
- Nur den Volltext ODER nur den Kurzcheck pflegen — IMMER beide.
- Gefundene Bugs nur in Best Practices ablegen statt auch im Almanach.
- Quellen-Links/Versions-Anker/Stand-Datum weglassen.
- Die Persistenz "auf spaeter" verschieben oder uncommitted liegen lassen.
