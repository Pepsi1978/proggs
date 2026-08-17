# Werft-Studio-Designpaket — Experimente-SPEC-v1

Dieses Paket enthält das **vollständige** Design: jeden Bildschirm in **jeder** Erscheinung,
dazu alle gemessenen Farben, Maße, Schriften, Radien, Effekte, Assets und Texte.

## Inhalt

| Pfad | Inhalt |
|------|--------|
| `design.html` | Das durchklickbare Gesamtdesign — alle 9 Bildschirme, mit Umschalter für Bildschirm **und** Erscheinung (oben rechts). |
| `bildschirme/<erscheinung>/<nr>-<name>.html` | Jeder Bildschirm einzeln, fest in dieser Erscheinung. 9 Bildschirme × 2 Erscheinungen = 18 Dateien. |
| `bauplan/<erscheinung>/<nr>-<name>.json` | **Die Layout-Hierarchie jedes Bildschirms — die verbindliche Bauvorlage.** Sagt je Bauteil, wie es seine Kinder anordnet (Spalte, Zeile, Raster, Abstand, Ausrichtung, Innenabstand), in welcher Reihenfolge sie stehen, welche Funktion daran hängt, wohin es führt, welche Einträge eine Auswahlliste hat und welche Grenzen ein Schieberegler. Breitenunabhängig gültig. |
| `bildschirme/design.css` | Das gemeinsame Stylesheet aller Bildschirme. |
| `design-tokens.json` | Alle gemessenen Werte maschinenlesbar (Erscheinungen mit vollständigen Token-Tabellen, Bildschirme, Farben, Maße, Typografie, **Schriften mit Herkunft**, Formen, Effekte, Assets, Texte). |
| `DESIGN-SPEC.md` | Dieselben Werte als lesbare Spezifikation inklusive Bildschirm-Tabelle. |
| Übriger ZIP-Inhalt | Das unveränderte Originalprojekt mit allen Begleitdateien (Bilder, Fonts, Audio, Daten). |

## Erscheinungen

- **2.1 Dunkel (Standard)** — `21dunkelstandard` (dark), 13 Farbtoken
- **2.2 Hell** — `22hell` (light), 13 Farbtoken

## Für den Design-Umsetzer

1. **Baue nach `bauplan/<erscheinung>/<nr>-<name>.json`, nicht nach Koordinaten.** Der Bauplan sagt, wie die Teile zueinander stehen — eine Hierarchie gilt bei jeder Breite, eine Koordinate nur bei der Breite, bei der sie gemessen wurde. Widersprechen sich Bauplan und ein gemessener Kasten, **gewinnt der Bauplan**.
2. `design-tokens.json` ist die verbindliche Quelle für alle **Werte** — Farben, Maße, Schriften, Radien, Effekte. Nichts daraus schätzen oder runden. Sie sagt *womit* gebaut wird, der Bauplan sagt *wie*.
3. **Ersetze kein Bedienelement durch ein anderes.** Steht im Bauplan `tag: "select"` mit `eintraege`, wird eine Auswahlliste gebaut — kein Feld, das bei jedem Druck weiterschaltet. Steht dort `bereich` mit `von`/`bis`/`schritt`, wird ein Schieberegler gebaut — kein Knopfsatz.
4. `versteckt: true` heißt **Zustand**, nicht Wegfall: Ladezustand, Fehlerkarte, leerer Zustand. Alle diese Zustände werden gebaut.
5. Die Bildschirmdateien in `bildschirme/` tragen im Kopf `<meta name="werft-render-width">` und einen aufgelösten Style-Block: sie sehen damit überall so aus wie im Studio. Wer sie vermisst, setzt die Fensterbreite **vor** dem Laden auf diesen Wert.
6. `bildschirme/<erscheinung>/` zeigt, wie **jeder** Bildschirm in **jeder** Erscheinung aussehen muss.
7. `design.html` zeigt den Klickweg: `data-werft-navigate="<ziel-id>"` ist die Navigation.
8. Alle Erscheinungen werden als umschaltbare Themes umgesetzt, nicht nur die zuerst sichtbare.
9. **Die Schriften aus `design-tokens.json` → `schriften` sind verbindlich.** Steht dort `quelle: "verzeichnis"` mit einer URL, ist die Familie samt `gewichte` genau so einzurichten (auf Android als heruntergeladene oder mitgelieferte Schrift, nicht als Systemschrift-Ersatz). Steht dort `quelle: "system"`, bringt das Paket keine Quelle mit — dann muss die Familie beschafft werden, bevor gebaut wird. Eine ersetzte Schrift ist der auffälligste Unterschied zum Entwurf, den es gibt.
10. Vollständig ist die Umsetzung erst, wenn jeder Bildschirm aus der Tabelle in `DESIGN-SPEC.md` im Code nachweisbar ist.

> **Achtung:** 1 erkannte Bildschirme wurden beim Aufbau nicht erzeugt: Experimente-SPEC-v1. Sie fehlen in diesem Paket.
