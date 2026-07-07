# Lottie, Rive & SVG-Animationen im Web Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
