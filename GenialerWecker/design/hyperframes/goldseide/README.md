# Goldseide — bewegter Hintergrund für „Schlicht“

Eine 16 Sekunden lange, nahtlose Schleife aus goldenen Seidenbändern, Goldstaub, weichem Bokeh und
einem Lichtschleier, gebaut mit [HyperFrames](https://hyperframes.heygen.com) (HTML + GSAP → MP4).
Die App spielt sie im Design „Schlicht“ stumm hinter allen Bildschirmen ab, auch auf dem
Weckbildschirm (`app/src/main/java/de/frank/wecker/design/Bewegung.kt`).

| Datei | Inhalt |
|---|---|
| `goldseide.js` | Zeichnet ein Bild aus dem Fortschritt p ∈ [0, 1). Alle Bewegungen sind periodisch in p, daher kein Sprung an der Nahtstelle. |
| `index.html` | Fassung **dunkel** (#121212, Gold #E3B341, Akzent #C25E00). |
| `compositions/hell.html` | Fassung **hell** (#FAF7F0, Gold #8B6914, Akzent #A34F00). |
| `render.sh` | Rendert beide und legt sie als `app/src/main/res/raw/goldseide_dunkel.mp4` / `goldseide_hell.mp4` ab. |

## Neu rendern

Voraussetzungen: Node.js ≥ 22, FFmpeg, Chrome (HyperFrames lädt es selbst).

```bash
cd GenialerWecker/design/hyperframes/goldseide
npx --yes hyperframes@0.8.138 lint .   # muss 0 Fehler melden
bash render.sh                           # ~3 Minuten, schreibt direkt in res/raw
```

Die Videos sind 720 × 1440, H.264 Main, **ohne Tonspur** (`-an`) — ein Video mit Ton dürfte
in einer Wecker-App dem Weckton nicht in die Quere kommen. Zusammen unter 2 MB.

## Gestalten

Farben, Stärke und Anzahl stehen als Werte in `index.html` bzw. `compositions/hell.html`
(`feldAlpha`, `baender`, `staubAnzahl`, `bokehAlpha`, `vignette`). Die Vignette folgt derselben Formel
wie `Modifier.vignette` in der App. HyperFrames verbietet `Math.random()`; die Teilchen kommen aus
einem festen Zufallsgenerator (`saat`), jedes Rendern sieht deshalb gleich aus.
