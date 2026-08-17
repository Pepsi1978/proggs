# Designs

Version 1.1.0 (28.07.2026, 20:50 Uhr)

Design-Dateien sind nach Projekt in eigenen Unterordnern abgelegt:

- [Fisetin-Begleiter-Design-Update](./Fisetin-Begleiter-Design-Update/)

## Zwei Herkünfte

| Herkunft | Erkennungsmerkmal | Primäre Quelle für den `design-umsetzer` |
|----------|-------------------|------------------------------------------|
| Claude Designs | `*.dc.html` im Projektordner | die `.dc.html` |
| Werft Studio (ZIP-Download) | Unterordner `WERFT-DESIGN/` | `WERFT-DESIGN/design-tokens.json` + `WERFT-DESIGN/bildschirme/<erscheinung>/…` |

Ein Werft-Paket enthält jeden Bildschirm in jeder Erscheinung als eigene Datei sowie alle
gemessenen Farben, Maße, Schriften, Radien, Effekte, Assets und Texte maschinenlesbar.
Einstieg: `WERFT-DESIGN/LIESMICH.md`.
