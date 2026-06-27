# Lottie, Rive & SVG-Animationen im Web Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
