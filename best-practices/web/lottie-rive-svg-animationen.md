# Lottie, Rive & SVG-Animationen im Web — Best Practices (Stand 2026-06-26)

> Zielbild: animierte Web-Assets, die schön aussehen, kontrolliert laden, barrierefrei bleiben,
> Reduced Motion respektieren und in Build-Systemen reproduzierbar funktionieren. Gegenstück zum
> Bug-Almanach: `bugs/web/lottie-rive-svg-animationen.md`.
>
> **Versionsanker:** `lottie-web 5.13.0`, `@rive-app/canvas 2.38.3`,
> `@rive-app/react-canvas 4.29.3`; kein belastbarer Bug-Fixstatus für diese konkreten Versionen gefunden.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Asset-Technik wählen | Lottie für AE-Illustration, Rive für interaktive State-Machines, SVG/SMIL/CSS für kleine Icons/Logos | §1 |
| 2 | Lottie-Renderer wählen | SVG-Renderer als Default für Schärfe/Farbtreue; Canvas nur bei Performance-/Mengenbedarf und mit Regressionstest | §2 |
| 3 | AE nach Lottie exportieren | Masken/Matten bewusst vereinfachen, Pre-Comps nutzen, Export in Browsern prüfen | §2 |
| 4 | Rive integrieren | WASM versioniert aus demselben Paket laden, preloaden und `cleanup()` beim Unmount | §3 |
| 5 | Rive State-Machine | Input-Namen aus Editor exakt übernehmen; Trigger/Boolean/Number unterschiedlich setzen | §3 |
| 6 | SVG animieren | Inline SVG oder SMIL für eingebettete SVGs; `<animateTransform>` für Transform | §4 |
| 7 | Path-Morphing | Pfade normalisieren; gleiche Command-/Punktstruktur erzwingen | §4 |
| 8 | Accessibility | Bedeutungsvolle Animationen beschreiben; dekorative aus dem Accessibility Tree nehmen | §5 |
| 9 | Reduced Motion | Vor Autoplay prüfen; statischer Ersatz mit gleicher Bedeutung | §5 |
| 10 | Autoplay/Loop | Endlosbewegung vermeiden oder Pause/Stop/Hide anbieten; keine harten Flashes | §5 |
| 11 | Build/Assets | Dynamische Vite-Assetpfade vermeiden; `.riv`/WASM/SVG bewusst hosten und cachen | §6 |
| 12 | Testing | Browser, Reduced Motion, Tastatur, Screenreader, Performance und Langlauf testen | §7 |

---

## §1 — Entscheidungsregel: Lottie, Rive oder SVG?

- **Lottie:** After-Effects-/Bodymovin-Illustrationen, Onboarding, leichtere Markenanimationen, nicht für komplexe interaktive Logik.
- **Rive:** Interaktive Animationen mit State Machine, Hover-/Pointer-/Input-Zuständen, spielerische UI, animierte Produktzustände.
- **SVG/SMIL/CSS:** Kleine Icons, Logos, Stroke-Draw, einfache Morphs, eingebettete Vektorbewegung ohne großen Runtime-Player.

Die beste Animation ist die kleinste Technik, die den Zweck erfüllt. Jede Animation bekommt einen statischen Fallback, ein Performance-Budget und eine Reduced-Motion-Variante.

## §2 — Lottie sauber exportieren und rendern

- SVG-Renderer zuerst testen, weil Canvas in den recherchierten Fällen bei Masken/Farbprofilen anfälliger war.
- Masken/Matten in After Effects exportfreundlich bauen: Pre-Comp für maskierten Inhalt, Matte pro Ziel-Layer statt eine Matte für viele Layer.
- Lottie-JSON als Artefakt behandeln: versionieren, komprimieren, visuell regressions-testen, nicht manuell patchen ohne Kommentar/Quelle.
- Bei Canvas-Pflicht: Alpha-Matte-/Farbprofil-Fälle in Chrome, Safari und Firefox screenshot-testen.

## §3 — Rive-Web-Integration

- WASM nicht aus zufälliger CDN-Version laden. Paket-Asset importieren, optional `RuntimeLoader.setWasmUrl(...)` setzen und preloaden.
- React-Unmount immer aufräumen:
  ```tsx
  useEffect(() => {
    return () => rive?.cleanup();
  }, [rive]);
  ```
- Globale Pointer-/Mouse-Listener in Cleanup entfernen.
- State-Machine-Inputs erst verwenden, wenn `rive` existiert. Trigger mit `fire()`, Boolean/Number über `value` setzen.
- Overlay-HTML kann Pointer-States blockieren; wenn HTML über dem Canvas liegt, Pointer bewusst in JS an Rive-Inputs weitergeben.

## §4 — SVG-Animationsstrategie

- Transform in SMIL mit `<animateTransform>`, nicht `<animate attributeName="transform">`.
- Wenn animiertes SVG als `<img>` oder `background-image` eingebunden wird, CSS-Animationen browserübergreifend nicht voraussetzen; SMIL oder inline SVG bevorzugen.
- Für Masken/ClipPaths in Firefox CSS-Transforms vermeiden; SMIL oder CSS-mask-Data-URI-Workaround prüfen.
- Path-Morphing nur mit normalisierten Pfaden: gleiche Commands, gleiche Punktzahl, gleiche Richtung.
- Für Stroke-Drawing `pathLength` nutzen, damit Dash-Werte deterministischer werden.

## §5 — Accessibility und Reduced Motion

- Animationen, die Bedeutung tragen, brauchen Textalternative und statischen Ersatz mit gleicher Information.
- Dekorative Animationen: `aria-hidden="true"`, keine Fokusfalle, kein Screenreader-Lärm.
- Canvas/Lottie/Rive mit Bedeutung: `role="img"`, `aria-label`/`aria-labelledby`, Beschreibung und sichtbare Kontrollen.
- `prefers-reduced-motion` in JS abfragen; CSS-Media-Query stoppt Lottie/Rive nicht automatisch.
- Autoplay/Loop: WCAG 2.2.2 einhalten — Stopp binnen 5 Sekunden oder Pause/Stop/Hide. Keine schnellen harten Farbflashes (>3/s).

## §6 — Build, Hosting und Cache

- Vite: keine großen dynamischen `new URL(`/assets/${id}.svg`, import.meta.url)`-Sammlungen im Bundle. Besser statische Import-Map oder Public/CDN-Pfad.
- SVG als React-Komponente in Vite über `vite-plugin-svgr`; bei Refs `svgrOptions: { ref: true }`.
- `index.html` nicht langfristig cachen; gehashte Assets dagegen lange cachen. Für animierte Assets/WASM klare Cache-Header setzen.
- CSP/CORS/MIME für WASM, JSON, `.riv`, `.lottie` und SVG vor Deployment prüfen.

## §7 — Testmatrix

Vor Release mindestens prüfen:

- Chrome, Edge, Firefox, Safari soweit Zielplattform.
- Reduced Motion aktiv und inaktiv.
- Tastaturbedienung und sichtbarer Fokus.
- Screenreader/Accessibility Tree für bedeutungsvolle Canvas/SVG.
- Performance-Profil für Main Thread, GPU/Canvas, Langlauf bei Rive.
- Netzwerkpfade: WASM, `.riv`, Lottie-JSON, SVG, Cache-Invalidierung.

---

## Bezug zum Bug-Almanach

| Best-Practice | Bug-Almanach |
|---------------|--------------|
| `best-practices/web/lottie-rive-svg-animationen.md` | `bugs/web/lottie-rive-svg-animationen.md` |

---

## Quellen

- Lottie/LottieFiles/Adobe-Community-Recherche, Researcher 7, 2026-06-26.
- Rive-Web-/React-/WASM-Recherche, Researcher 8 und 12, 2026-06-26.
- MDN SVG Animation with SMIL, Practical SVG, Mozilla Bug 1693016, Researcher 9, 2026-06-26.
- WCAG/LottieFiles Accessibility/GSAP Reduced-Motion-Recherche, Researcher 10, 2026-06-26.
- Vite Asset-/SVG-Import-Recherche, Researcher 11, 2026-06-26.
