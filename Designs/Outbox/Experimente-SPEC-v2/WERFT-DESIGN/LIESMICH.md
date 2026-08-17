# Werft-Studio-Designpaket — Experimente-SPEC-v1

Dieses Paket enthält das **vollständige** Design: jeden Bildschirm in **jeder** Erscheinung,
dazu alle gemessenen Farben, Maße, Schriften, Radien, Effekte, Assets und Texte.

## Inhalt

| Pfad | Inhalt |
|------|--------|
| `design.html` | Das durchklickbare Gesamtdesign — alle 9 Bildschirme, mit Umschalter für Bildschirm **und** Erscheinung (oben rechts). |
| `bildschirme/<erscheinung>/<nr>-<name>.html` | Jeder Bildschirm einzeln, fest in dieser Erscheinung. 9 Bildschirme × 2 Erscheinungen = 18 Dateien. |
| `bildschirme/design.css` | Das gemeinsame Stylesheet aller Bildschirme. |
| `design-tokens.json` | Alle gemessenen Werte maschinenlesbar (Erscheinungen mit vollständigen Token-Tabellen, Bildschirme, Farben, Maße, Typografie, Formen, Effekte, Assets, Texte). |
| `DESIGN-SPEC.md` | Dieselben Werte als lesbare Spezifikation inklusive Bildschirm-Tabelle. |
| Übriger ZIP-Inhalt | Das unveränderte Originalprojekt mit allen Begleitdateien (Bilder, Fonts, Audio, Daten). |

## Erscheinungen

- **2.1 Dunkel (Standard)** — `21dunkelstandard` (dark), 13 Farbtoken
- **2.2 Hell** — `22hell` (light), 13 Farbtoken

## Für den Design-Umsetzer

1. `design-tokens.json` ist die **verbindliche Quelle** für alle Werte — nichts daraus schätzen oder runden.
2. `bildschirme/<erscheinung>/` zeigt, wie **jeder** Bildschirm in **jeder** Erscheinung aussehen muss.
3. `design.html` zeigt den Klickweg: `data-werft-navigate="<ziel-id>"` ist die Navigation.
4. Alle Erscheinungen werden als umschaltbare Themes umgesetzt, nicht nur die zuerst sichtbare.
5. Vollständig ist die Umsetzung erst, wenn jeder Bildschirm aus der Tabelle in `DESIGN-SPEC.md` im Code nachweisbar ist.

> **Achtung:** 1 erkannte Bildschirme wurden beim Aufbau nicht erzeugt: Experimente-SPEC-v1. Sie fehlen in diesem Paket.
