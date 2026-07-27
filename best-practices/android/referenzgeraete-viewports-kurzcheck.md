# Referenzgeräte: logische Bildschirmflächen (Kurzcheck)

Stand: 27.07.2026 · Quellen: GSMArena (Auflösungen), blisk.io (S23 Ultra), yesviz + Samsung
Galaxy-Emulator-Skins (Fold-Dichte)

## Die eine Regel

Für Design und Vorschau zählt **nicht die Pixelauflösung**, sondern die **logische Fläche in
dp/CSS-Pixeln** = Auflösung ÷ Anzeigedichte. Nur damit sieht ein Entwurf so aus wie auf dem Gerät.

| Gerät | Auflösung | Dichte | Logische Fläche (hoch) |
|---|---|---|---|
| Galaxy S23 Ultra | 1440 × 3088 | 3,75× | **384 × 824** |
| Galaxy Z Fold 8 · zugeklappt | 1248 × 1972 | 2,75× | **454 × 717** |
| Galaxy Z Fold 8 · aufgeklappt | 1828 × 2448 | 2,0× | **914 × 1224** |
| Galaxy Z Fold 6 · zugeklappt | 968 × 2376 | 2,625× | **369 × 905** |
| Galaxy Z Fold 6 · aufgeklappt | 1856 × 2160 | 2,0× | **928 × 1080** |
| Galaxy Z Fold 7 · aufgeklappt | 1968 × 2184 | 2,0× | **984 × 1092** (belegt, yesviz) |

Querformat = Werte tauschen. Aufgeklappt/zugeklappt und hoch/quer sind **zwei getrennte Achsen** —
ein Foldable hat vier Formate.

## Prüfregel für neue Geräte

Android definiert 160 dp je Zoll. Gegenprobe: dp-Diagonale ÷ Zoll sollte nahe 160 liegen
(Fold-Außenbildschirme ~155; Samsung-Innenbildschirme ~185, Galaxy-S-Reihe ~134 — Samsung wählt dort
bewusst gröber bzw. feiner). Weicht ein Wert stark ab, stimmt die angenommene Dichte nicht.

## Was NIEMALS

- Die Pixelauflösung als Design-Viewport verwenden (ein 1440er Entwurf ist auf dem Gerät 384 breit).
- Fold-Innen- und Außenbildschirm dieselbe Dichte geben (2,0× innen, 2,625–2,75× außen).
- Annehmen, ein für ein Format gebautes Design fülle ein anderes von selbst aus — dafür braucht es
  einen eigenen Aufbau je Format (siehe WerftStudio `design-variants.ts`).

Galaxy Z Fold 8 und Fold 8 Ultra wurden am 22.07.2026 vorgestellt; das Fold 8 hat ein breiteres,
kürzeres Außendisplay (5,5″, 4:3-Innenbildschirm) als seine Vorgänger.
