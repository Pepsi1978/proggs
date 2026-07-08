# Research-Persistenz: Recherchen in Best Practices & Bug-Almanache einarbeiten (KRITISCH)

> Franks Kernsatz: Recherchen duerfen nicht "verkommen" — wer spaeter im gleichen Bereich arbeitet,
> muss auf das schon recherchierte Wissen zurueckgreifen koennen.

## Grundregel

Nach JEDEM Web-Recherche-Einsatz pruefen, ob die Ergebnisse als Best Practices taugen. Wenn ja, IMMER
in die Best-Practices-Dateien einarbeiten — und enthaltene Bugs/Fallen zusaetzlich in die Bug-Almanache.
Jeweils Kurzversion (Kurzcheck) UND Langversion (Volltext). Erst dann ist die Aufgabe fertig.

Gilt fuer ALLE Recherche-Wege: `researcher`-Agents, `deep-research`/`superintelligenz`/
`direktiven-recherche`, eigene WebSearch/WebFetch, Subagent-Recherchen.

## Pflicht-Ablauf (nach jedem Recherche-Einsatz, vor Aufgabenabschluss)

1. **Tauglichkeits-Pruefung:** "Sind diese Findings ueber die aktuelle Aufgabe hinaus wiederverwendbar?"
2. **Wenn JA — Best Practices einarbeiten:** passende `best-practices/<kategorie>/<bereich>.md` ergaenzen
   oder neue in bestehendem Ordner anlegen (`android`, `apis`, `desktop`, `web`, `claude-tooling`, …).
   Beide Ebenen pflegen: Kurzcheck-Tabelle (Stufe A, erste 80 Zeilen) UND Volltext-Abschnitt. Format der
   bestehenden Dateien uebernehmen (Stand-Datum, Versions-Anker, Quellen-Links, `offiziell`/`extern`-Label).
3. **Wenn Bugs/Fallen enthalten — zusaetzlich Almanach:** in `bugs/<kategorie>/<bereich>.md` (Symptom,
   Ursache, Versionen, funktionserhaltender Fix, Quelle) — ebenfalls Kurzcheck UND Volltext; neuer Bereich →
   Datei anlegen + `bugs/README.md`-Index ergaenzen. Bugs gehoeren IMMER auch in den Almanach.
4. **Wenn NEIN:** in EINEM Satz begruenden, warum nicht persistiert wird.
5. **Committen + pushen** (nur eigene Dateien).

Der Text muss selbsterklaerend sein: eine spaetere Session ohne heutigen Kontext muss verstehen, was
recherchiert wurde, was gilt, fuer welche Versionen, woher.

## Tauglich oder nicht?

TAUGLICH: Patterns/APIs/Architektur, bekannte Bugs/Fallen/Workarounds, Library-Vergleiche mit Empfehlung,
Plattform-/Policy-Wissen, Harness-Wissen. NICHT: einmalige Faktenabfrage ohne Wiederverwendung, rein
projektspezifischer Zustand. Im Zweifel: einarbeiten.

## Arbeitsteilung

Researcher (Subagent) markiert am Ende `BEST-PRACTICES-KANDIDATEN:` + `BUG-KANDIDATEN:` (inkl. URLs +
Versionen; keine → `KEINE`). Hauptagent konsolidiert, prueft Tauglichkeit, arbeitet ein (Kurzcheck +
Volltext), committet. Einarbeitung IMMER der Hauptagent — nie parallel schreibende Researcher.

## Was NIEMALS passieren darf

- Researcher-Ergebnisse nach der Aufgabe verwerfen ohne Tauglichkeits-Pruefung
- Recherche "nur im Chat" lassen · nur Volltext ODER nur Kurzcheck pflegen
- gefundene Bugs nur in Best Practices statt auch im Almanach · Quellen/Versionen/Stand-Datum weglassen
- Persistenz auf spaeter verschieben oder uncommitted lassen
