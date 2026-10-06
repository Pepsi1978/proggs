# Bewegte Hintergründe (HyperFrames)

Jedes der vier Designs hat eine eigene, nahtlose 16-Sekunden-Schleife, gebaut mit
[HyperFrames](https://hyperframes.heygen.com) (HTML + GSAP → MP4). Die App spielt sie stumm hinter allen
Bildschirmen und dem Weckbildschirm ab (`app/src/main/java/de/frank/wecker/design/Bewegung.kt`,
`BewegtbildHintergrund`). Bei „Animationen entfernen“ oder einem Abspielfehler bleibt der bisherige,
ruhige Hintergrund des Designs.

| Ordner | Design | Inhalt | Video in der App |
|---|---|---|---|
| `goldseide/` | Schlicht | goldene Seidenbänder, Goldstaub, Bokeh, Lichtschleier | `goldseide_{dunkel,hell}.mp4` |
| `morgenlicht/` | Morgenruhe | dämmernder Horizont, Sonnenstrahlen, Nebelbänder, Blütenstaub | `morgenlicht_{dunkel,hell}.mp4` |
| `glutnebel/` | Traumraum | kreisende Glutwolken, aufsteigende Funken, Sternschnuppen (Funkelsterne zeichnet die App darüber) | `glutnebel_{dunkel,hell}.mp4` |
| `orbitalgitter/` | Orbit | Satellitenbahnen, Radarstrahl, perspektivisches Gitter, Abtastlinie (Zeichenregen zeichnet die App darüber) | `orbitalgitter_{dunkel,hell}.mp4` |

Jeder Ordner: `<name>.js` zeichnet ein Bild aus dem Fortschritt p ∈ [0, 1) (alle Bewegungen periodisch
mit ganzzahliger Frequenz), `index.html` ist die dunkle, `compositions/hell.html` die helle Fassung mit
den exakten Palettenfarben, `render.sh` rendert beide nach `app/src/main/res/raw/`.

```bash
cd GenialerWecker/design/hyperframes/<ordner>
npx --yes hyperframes@0.8.138 lint .   # muss 0 Fehler melden
bash render.sh                           # einige Minuten, schreibt direkt in res/raw
```

Videos: 720 × 1440, H.264 Main, **ohne Tonspur** (`-an`) — in einer Wecker-App darf ein Video nie dem
Weckton in die Quere kommen.
