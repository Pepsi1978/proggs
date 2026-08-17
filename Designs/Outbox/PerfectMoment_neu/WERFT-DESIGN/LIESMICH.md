# Werft-Studio-Designpaket — PerfectMoment

Dieses Paket enthält das **vollständige** Design: jeden Bildschirm in **jeder** Erscheinung,
dazu alle gemessenen Farben, Maße, Schriften, Radien, Effekte, Assets und Texte.

## Inhalt

| Pfad | Inhalt |
|------|--------|
| `design.html` | Das durchklickbare Gesamtdesign — alle 18 Bildschirme, mit Umschalter für Bildschirm **und** Erscheinung (oben rechts). |
| `bildschirme/<erscheinung>/<nr>-<name>.html` | Jeder Bildschirm einzeln, fest in dieser Erscheinung. 18 Bildschirme × 2 Erscheinungen = 36 Dateien. |
| `bildschirme/design.css` | Das gemeinsame Stylesheet aller Bildschirme. |
| `design-tokens.json` | Alle gemessenen Werte maschinenlesbar (Erscheinungen mit vollständigen Token-Tabellen, Bildschirme, Farben, Maße, Typografie, Formen, Effekte, Assets, Texte). |
| `DESIGN-SPEC.md` | Dieselben Werte als lesbare Spezifikation inklusive Bildschirm-Tabelle. |
| Übriger ZIP-Inhalt | Das unveränderte Originalprojekt mit allen Begleitdateien (Bilder, Fonts, Audio, Daten). |

## Erscheinungen

- **DarkPmColors (Dunkel)** — `darkpmcolors` (dark), 12 Farbtoken
- **LightPmColors** — `lightpmcolors` (light), 12 Farbtoken

## Für den Design-Umsetzer

1. `design-tokens.json` ist die **verbindliche Quelle** für alle Werte — nichts daraus schätzen oder runden.
2. `bildschirme/<erscheinung>/` zeigt, wie **jeder** Bildschirm in **jeder** Erscheinung aussehen muss.
3. `design.html` zeigt den Klickweg: `data-werft-navigate="<ziel-id>"` ist die Navigation.
4. Alle Erscheinungen werden als umschaltbare Themes umgesetzt, nicht nur die zuerst sichtbare.
5. Vollständig ist die Umsetzung erst, wenn jeder Bildschirm aus der Tabelle in `DESIGN-SPEC.md` im Code nachweisbar ist.
