# Bekannte Bugs: Lottie, Rive & SVG-Animationen im Web

> **PFLICHT-LESEN VOR DER ARBEIT.** Kuratierter Bug-Almanach für Lottie/Bodymovin,
> Rive Web (`@rive-app/canvas`, `@rive-app/react-canvas`), SVG-/SMIL-/CSS-Animationen,
> animierte Assets, Reduced Motion, Accessibility und Vite-Asset-Fallen. Stand recherchiert
> **2026-06-26**. Lokale Versionsanker: `lottie-web 5.13.0`, `@rive-app/canvas 2.38.3`,
> `@rive-app/react-canvas 4.29.3`; für diese konkreten Versionen wurde **kein belastbarer
> Fixstatus** gefunden. Gegenseite: `best-practices/web/lottie-rive-svg-animationen.md`.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Lottie Canvas zeigt rote/dünne Outline bei Alpha-Matte | SVG-Renderer testen; Matte-RGB auf `0,0,0` und Alpha auf `0`; Issue #3051 offen | §1 |
| 2 | AE-Masken fehlen im Lottie-Export | Maskierten Inhalt in Pre-Comp legen; Matte pro Layer duplizieren | §2 |
| 3 | Lottie-Farben wirken ausgewaschen | Canvas kann Farbprofile verlieren; SVG-Renderer oder AE-Color-Management prüfen | §3 |
| 4 | Rive crasht nach langer Laufzeit mit `RuntimeError: Aborted()` | Betroffene alte 3.x-Versionen aktualisieren; Cleanup und Pointer-Setup prüfen; kein Beleg für 4.29.3 | §4 |
| 5 | Rive lädt falsches WASM | JS- und WASM-Version koppeln; `RuntimeLoader.setWasmUrl`/Bundler-Asset sauber setzen | §5 |
| 6 | Rive-State-Machine reagiert nicht | Exakte Namen prüfen; Trigger `fire()`, Boolean/Number `value = ...`; `rive` kann initial `null` sein | §6 |
| 7 | SVG `<animate>` bewegt `transform` nicht | Für Transform immer `<animateTransform>` verwenden | §7 |
| 8 | SVG-CSS-Animation funktioniert als `<img>` in Chrome, nicht Firefox | Für eingebettete SVG-Animationen SMIL oder inline SVG nutzen | §8 |
| 9 | Firefox ruckelt bei CSS-transformierten SVG-Masken/ClipPaths | SMIL-Transform oder CSS-mask-Data-URI-Workaround nutzen | §9 |
| 10 | Path-Morphing zerreißt | Pfade mit gleicher Punkt-/Command-Struktur; CSS kann `d`/`points` nicht animieren | §10 |
| 11 | Animation verletzt WCAG 2.2.2 | Autoplay/Loop stoppen <5 s oder Pause/Stop/Hide anbieten; Reduced Motion vorher respektieren | §11 |
| 12 | Lottie/Rive/SVG trägt Bedeutung, wird aber bei Reduced Motion entfernt | Statisches Bild/erster oder letzter Frame mit gleicher Information ausliefern | §12 |
| 13 | Screenreader bekommen bei Canvas nichts | `role="img"`, `aria-labelledby`, Beschreibung und Tastatur-/Kontrollbuttons bereitstellen | §13 |
| 14 | Vite lädt 10.000 SVGs / White Screen im Dev | Dynamische `new URL()`-Assetpfade vermeiden; Assets außerhalb des Bundles/CDN hosten | §14 |
| 15 | SVG-React-Import in Vite scheitert | `vite-plugin-svgr`; für Ref `svgrOptions: { ref: true }` | §15 |

---

## §1 — Lottie Canvas: Alpha-Matte-Outline / Artefakte

- **Symptom:** Dünne rote Linie/Outline um animierte Shapes; im SVG-Renderer nicht sichtbar.
- **Ursache:** Nichttransparenter Fill-Layer dient als Alpha Matte; Canvas-Renderer zeichnet die Matte-Kante sichtbar.
- **Versionen:** Belegt für Chrome 119/Safari/AE 24.0.3 und betroffene Player wie `lottie-react`/`@dotlottie/react-player`; Issue `airbnb/lottie-web#3051` laut Recherche offen. Kein Beleg, ob `lottie-web 5.13.0` gefixt ist.
- **FIX:** Renderer auf `svg` testen. Wenn Canvas nötig: Matte-Layer im JSON auf RGB `(0,0,0)` setzen und Alpha im `k`-Array von `1` auf `0` ändern; visuell regressions-testen.
- **Quelle:** https://github.com/airbnb/lottie-web/issues/3051, Researcher 7.

## §2 — Lottie-Masken/Track-Mattes fehlen oder exportieren falsch

- **Symptom:** Maske sieht in After Effects korrekt aus, fehlt aber in Lottie-Vorschau/Export; Track Matte auf mehrere Layer exportiert falsch.
- **Ursache:** Bodymovin/Lottie unterstützt nicht jeden AE-Masken-/Matte-Aufbau direkt; Luma Matte laut Quelle problematisch.
- **FIX:** Maskierten Inhalt in Pre-Composition legen; Matte-Layer pro Ziel-Layer duplizieren und einzeln anwenden; Alpha Matte gegenüber Luma Matte bevorzugen, wenn Export stabil sein muss.
- **Quellen:** LottieFiles Forum `Masks not rendering in Lottie`; LottieFiles YouTube Track-Matte-Workaround, Researcher 7.

## §3 — Lottie-Farben ausgewaschen

- **Symptom:** Farben aus After Effects wirken im Lottie-Output flach/blass.
- **Ursache:** Canvas-Renderer/Farbprofil-Verhalten; Browser-Canvas unterstützt Farbprofile nicht so wie SVG.
- **FIX:** SVG-Renderer prüfen; AE Color Management testweise deaktivieren; Zielbrowser mit Screenshot-Vergleich testen.
- **Quelle:** Adobe Community Bodymovin/Lottie Color Loss, Researcher 7.

## §4 — Rive Long-Runtime-Crash `RuntimeError: Aborted()`

- **Symptom:** Nach ca. 25-60 Minuten Laufzeit crasht die WASM-Runtime, Stack mit `mouseCallback`.
- **Ursache:** In der Quelle nicht endgültig bestätigt; Pointer-/Mouse-Verarbeitung und dauerhaftes Autoplay/komplexe Szene sind im Stack/Setup sichtbar.
- **Versionen:** Belegt für `@rive-app/react-canvas@3.0.25` bis `3.0.33`; ein Nutzer meldet Stabilität nach Update auf `3.0.38`. Kein Beleg für `@rive-app/react-canvas 4.29.3` oder `@rive-app/canvas 2.38.3`.
- **FIX:** Alte 3.x-Versionen aktualisieren; `rive?.cleanup()` beim Unmount; globale Pointer-Listener entfernen; lange Autoplay-Szenen mit Laufzeittest prüfen.
- **Quelle:** Rive-GitHub-Issue aus Researcher 8.

## §5 — Rive JS/WASM-Versionsdrift

- **Symptom:** Stacktrace zeigt `https://unpkg.com/@rive-app/canvas@1.0.95/rive.wasm`, obwohl App-Code andere Pakete nutzt.
- **Ursache:** WASM separat über CDN/URL geladen; JS- und WASM-Runtime können auseinanderlaufen.
- **FIX:** WASM als Paket-Asset bundeln und mit `RuntimeLoader.setWasmUrl(riveWasmUrl)` setzen oder sicherstellen, dass automatisch geladenes WASM exakt zur Paketversion passt. Optional preloaden:
  ```html
  <link rel="preload" href="/assets/rive.wasm" as="fetch" crossorigin="anonymous">
  ```
- **Quelle:** Rive-Doku/Issue-Auswertung, Researcher 8.

## §6 — Rive State-Machine/Input reagiert nicht

- **Symptom:** Hover/Klick löst in Rive nichts aus; Trigger scheinbar „kaputt“.
- **Ursache:** `rive` ist initial `null`; State-Machine-/Input-Namen stimmen nicht exakt; falsche Methode für Input-Typ; HTML-Overlay blockiert Pointer.
- **FIX:** Inputs erst nach Rive-Load verwenden; Namen exakt aus Editor übernehmen; Trigger mit `input.fire()`, Boolean/Number mit `input.value = ...`; Overlay-Pointer bewusst routen oder in JS setzen.
- **Quellen:** Rive React/API-Doku, Researcher 8.

## §7 — SVG `<animate>` animiert `transform` nicht

- **Symptom:** SVG bleibt statisch, obwohl `<animate attributeName="transform">` gesetzt ist.
- **Ursache:** Für Transform gibt es in SMIL ein eigenes Element.
- **FIX:**
  ```xml
  <animateTransform attributeName="transform" type="rotate" from="0 60 70" to="360 60 70" dur="10s" />
  ```
- **Quellen:** MDN SVG Animation with SMIL; Practical SVG, Researcher 9.

## §8 — CSS-Animation in eingebettetem SVG unterscheidet sich nach Browser

- **Symptom:** SVG animiert als `<img>`/`background-image` in Chrome, aber nicht in Firefox.
- **Ursache:** CSS in extern eingebundenen SVGs wird browserunterschiedlich ausgeführt; SMIL funktioniert in den Quellen robuster.
- **FIX:** Für eingebettete animierte SVGs SMIL nutzen oder SVG inline ins DOM setzen. Für wichtige UI-Zustände nicht auf CSS im `<img>` verlassen.
- **Quellen:** Practical SVG, Researcher 9.

## §9 — Firefox ruckelt bei CSS-transformierten SVG-Masken/ClipPaths

- **Symptom:** CSS-Transform auf `<mask>`/`<clipPath>` läuft in Chrome flüssig, in Firefox ruckelig; DevTools zeigen teils kein Problem.
- **Ursache:** Mozilla-Bug 1693016 / unvollständige Transform-Implementierung auf Mask/ClipPath laut Quelle.
- **FIX:** Maskenbewegung mit SMIL/SVG-Transform bauen oder Maske als CSS-`mask`/`-webkit-mask` Data-URI einbinden und HTML-/CSS-Properties animieren.
- **Quelle:** StackOverflow + Mozilla Bug 1693016, Researcher 9.

## §10 — SVG-Path-Morphing/Path-Drawing-Fallen

- **Symptom:** Morphing springt, verzerrt oder bricht; Path-Drawing-Länge ist unvorhersehbar.
- **Ursache:** CSS kann `d`/`points` nicht direkt animieren; JS-Morphing braucht gleiche Pfadstruktur; `stroke-dasharray` hängt von Pfadlänge ab.
- **FIX:** Pfade vorher normalisieren (gleiche Command-/Punktzahl); für Linienzeichnung `pathLength` nutzen; für Form-Morphing SMIL oder JS/Motion mit validierten Pfaden.
- **Quellen:** Practical SVG; Motion/Framer-Motion-Pfadbeispiele, Researcher 9.

## §11 — Autoplay/Loop verletzt WCAG 2.2.2

- **Symptom:** Animation läuft automatisch/endlos; Nutzer mit Vestibularproblemen, Migräne oder Konzentrationsbedarf werden belastet.
- **Ursache:** Lottie/GIF/Rive/SVG-Animationen werden häufig als Dekoration mit Autoplay und Loop integriert.
- **FIX:** Automatische Bewegung entweder binnen 5 Sekunden stoppen oder Pause/Stop/Hide anbieten. Besser: vor dem Start `prefers-reduced-motion` und optional einen UI-Toggle respektieren.
- **Quellen:** WCAG 2.2.2; LottieFiles WCAG; GSAP/Reduced-Motion-Recherche, Researcher 10.

## §12 — Reduced Motion entfernt Inhalt statt Bewegung

- **Symptom:** Nutzer mit Reduced Motion sehen gar keine Status-/Instruktionsinformation mehr.
- **Ursache:** Animation wird einfach ausgeblendet, obwohl sie Bedeutung trägt.
- **FIX:** Statisches Bild, erster/letzter Frame oder textliche Alternative mit gleicher Bedeutung. Für Lottie explizit `autoplay: false`, `goToAndStop(...)` oder statischen Ersatz verwenden; CSS-Media-Query allein stoppt JS-Lottie nicht.
- **Quellen:** tempertemper/Hidde de Vries; bodymovin-Maintainer-Kommentar, Researcher 10.

## §13 — Canvas-/Text-Splitting-Accessibility

- **Symptom:** Screenreader liest nichts oder Buchstabe für Buchstabe; Canvas ist nicht fokussierbar.
- **Ursache:** Lottie/Rive rendern in Canvas; Text-Splitting zerlegt Wörter in DOM-Fragmente.
- **FIX:** Canvas mit `role="img"`, `aria-labelledby`/`aria-label`, versteckter Beschreibung und Tastatur-Fokus/Controls versehen. Bei SplitText GSAP SplitText ≥ 3.13.0 oder eigenes `aria-label` am Parent und `aria-hidden="true"` an Split-Kindern.
- **Quellen:** LottieFiles WCAG Guide; GSAP SplitText Accessibility, Researcher 10.

## §14 — Vite dynamischer Assetpfad lädt alle SVGs

- **Symptom:** Dev-Server lädt plötzlich tausende SVGs; Netzwerk-Tab zeigt ~10.000 Requests, White Screen ~40 s.
- **Ursache:** Dynamisches `new URL()`-Template im Vite-Assetverzeichnis kann den Asset-Resolver auf das ganze Verzeichnis ausweiten.
- **FIX:** Große/dynamische SVG-Sammlungen außerhalb des Vite-Bundles hosten (Cloud/CDN/Public-Pfad) oder statische Import-Map/Whitelist bauen, statt beliebige Template-Pfade auf `src/assets` zu zeigen.
- **Quelle:** Vite-Issue/Fallauswertung, Researcher 11.

## §15 — SVG als React-Komponente in Vite importiert nicht

- **Symptom:** `import { ReactComponent as X } from './x.svg'` schlägt fehl; `ref` bleibt `null`.
- **Ursache:** CRA-Importsyntax ist nicht automatisch Vite-Syntax; SVGR-Ref nicht aktiviert.
- **FIX:** `vite-plugin-svgr` installieren und konfigurieren; für `ref`:
  ```ts
  svgr({ svgrOptions: { ref: true } })
  ```
- **Quelle:** Vite-Migrationsrecherche, Researcher 11.

---

## Bezug zu Best Practices

| Dieser Almanach | Gegenstück |
|-----------------|------------|
| `bugs/web/lottie-rive-svg-animationen.md` | `best-practices/web/lottie-rive-svg-animationen.md` |

---

## Quellen und offene Versionslücken

- `airbnb/lottie-web#3051`, LottieFiles Forum/YouTube, Adobe Community Bodymovin, Researcher 7, 2026-06-26.
- Rive React/Canvas Issue-/Doku-Auswertung, Researcher 8 und 12, 2026-06-26.
- MDN SVG Animation with SMIL, Practical SVG, Mozilla Bug 1693016, GSAP-Forum, Researcher 9, 2026-06-26.
- WCAG 2.2.2/2.3.1, LottieFiles WCAG Guide, GSAP Reduced Motion, tempertemper/CSS-Tricks-Recherche, Researcher 10, 2026-06-26.
- Vite Asset-/SVG-Import-Fälle, Researcher 11, 2026-06-26.
- **Nicht belegt:** spezifischer Fixstatus für `lottie-web 5.13.0`, `@rive-app/canvas 2.38.3`, `@rive-app/react-canvas 4.29.3`, Chrome 149 und Edge 149. Keine Aussage wird hier als „fixed“ behauptet, wenn die Quelle das nicht trägt.
