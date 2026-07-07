# Cluster-Index: Energy-Effects in Android Compose

Alles rund um sichtbare Elektrizität, Stromfluss, Funken, Glühen und animierte
Energie-Effekte in Jetpack-Compose-basierten Android-Apps.

## Anlass für diesen Cluster

Frank wollte für die BestJournal-Frank-Tagebuch-App ein **Energy Board** —
eine Timeline-Seitenleiste, durch die sichtbar Strom fließt, mit Blitzen, Funken,
glühenden Eintrags-Karten und Touch-Reaktion. Etwas, das man so in normalen Apps
nicht sieht.

Am 2026-05-01 wurden fünf Researcher parallel losgeschickt, jeder mit einem
eigenen Recherche-Winkel. Die Berichte unten sind das Ergebnis.

## Einträge

| # | Datum | Eintrag | Kern-Ergebnis |
|---|-------|---------|--------------|
| 1 | 2026-05-01 | [Stromfluss in Compose Canvas](2026-05-01-stromfluss-canvas-shader.md) | Empfohlen: 2-Schicht-Strategie (DashPathEffect + Brush.linearGradient), AGSL Shader optional für Android 13+ |
| 2 | 2026-05-01 | [Particle-Spark-Systeme](2026-05-01-particle-funken-systeme.md) | Goldene Architektur: SnapshotStateList + withFrameMillis + drawBehind + Object Pool. Max 20-40 Funken |
| 3 | 2026-05-01 | [Energy-UI-Inspirationen & DNA-Analyse](2026-05-01-energy-ui-inspirationen.md) | 5 Eigenschaften aller überzeugenden Energy-Effekte + 3 Farbpaletten. Geheimtipp: Lichtenberg-Muster + Biolumineszenz-Puls |
| 4 | 2026-05-01 | [Multi-Layer-Glow & Bloom](2026-05-01-multi-layer-glow-bloom.md) | 6 Schichten statt 3, Schwarzkörperstrahlung erklärt warum Kern weiß, Saber/PS Outer Glow Geheimnisse |
| 5 | 2026-05-01 | [LazyColumn-Overlay-Performance](2026-05-01-overlay-lazycolumn-performance.md) | withFrameNanos, layoutInfo nur in Draw-Phase lesen, drawWithCache, NestedScrollConnection für Touch |

## Schnell-Zusammenfassung der Erkenntnisse

Wenn man die fünf Berichte als ein Bild liest, ergibt sich folgender Plan
für ein wirklich überzeugendes Energy Board:

1. **Architektur**: `Box` mit `LazyColumn` + Canvas-Overlay (`matchParentSize`),
   `EnergyController`-State holt Punkt-Positionen aus `lazyListState.layoutInfo`
2. **Stromlinie**: Zwei Schichten — DashPathEffect mit animiertem Phase-Offset
   als Grundlinie, drüber ein Brush.linearGradient mit wandernder helle Stelle
   als Energie-Paket
3. **Funken**: SnapshotStateList mit Object-Pool, withFrameMillis-Loop, max 30
   gleichzeitig, Spawn an Fokus-Position, gelegentlich rekursive Verzweigungen
4. **Glow**: 6-Schichten-Stack mit BlurMaskFilter-Radien 2.5/8/16/28/48,
   Alpha-Kurve 0.08/0.15/0.35/0.65/0.90/1.0, BlendMode.Plus für innere Halos,
   BlendMode.Screen für äußeren Bloom
5. **Pulsing**: Drei überlagerte Sinuswellen mit irrationalen Frequenzen
   (z.B. goldener Schnitt 1.618), `pow(0.7f)` für Atemschwere
6. **Touch**: NestedScrollConnection als Beobachter, `pointerInput` mit
   `detectDragGestures`, `Offset.Zero` zurückgeben damit Scroll nicht blockiert
7. **Farbpalette**: Empfehlung Cyber-Amber (warm-elektrisch, passt zum Frank-Theme
   `#D36B00 Primary` + `#2C3930 Hintergrund`) — Kern Bernstein-Weiß `#FFF5E0`,
   Halo Amber `#FFB300`, Saum Deep Orange `#D36B00`

## Empfehlungen für Geheimtipps (kaum jemand kennt)

- **Lichtenberg-Muster** als Mikro-Animation an besonderen Knoten (100. Eintrag,
  Streak-Ende). Fraktale Verzweigungen wie eingefrorener Blitz. Auge erkennt
  intuitiv "echt".
- **Biolumineszenz-Puls** mit Atemfrequenz (~0,25 Hz, alle 4 Sekunden). Kaum
  bewusst wahrnehmbar — aber unterbewusst spürt der Nutzer dass die App "lebt".

## Verwandte Cluster (zukünftig)

Wenn folgende Cluster existieren, werden sie hier verlinkt:
- `android-compose/lazy-list-tricks/` — Performance-Patterns rund um LazyColumn
- `algorithmen-konzepte/fraktale-und-rauschen/` — Lichtenberg, Perlin-Noise, Midpoint-Displacement

## Tags in diesem Cluster

`compose` · `canvas` · `android` · `animation` · `performance` ·
`particles` · `glow` · `bloom` · `agsl` · `lazycolumn` · `inspiration`
