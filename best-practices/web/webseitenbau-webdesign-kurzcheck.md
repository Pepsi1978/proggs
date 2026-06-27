# Webseitenbau & visuelles Webdesign Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Neue Website planen | Erst Ziel, Nutzer, Stimmung und Conversion-Pfad klären; Schönheit dient der Aufgabe | §1 |
| 2 | „Schönes“ Layout | Starkes Grid als Basis; organische/Anti-Grid-Brüche nur gezielt als Akzent | §2 |
| 3 | Hero-Bereich | Eine klare Aussage, starke Typografie, ein dominantes visuelles Motiv, ein primärer CTA | §2 |
| 4 | Typografie | Große Headlines, variable/custom Fonts sparsam; Lesbarkeit und Hierarchie zuerst | §2 |
| 5 | Farben | Eine Signaturfarbe + neutrale Fläche; Neon/Gradient nur mit Kontrastprüfung | §2, §8 |
| 6 | Spacing | Großzügige Abstände gegen kognitive Überlastung; Tokens statt Einzelwerte | §2, §9 |
| 7 | Buttons/Schalter | Default, hover, active, focus, disabled, loading und toggle als eigene States designen | §3 |
| 8 | Focus-State | Nie entfernen; `:focus-visible` mit sichtbarem Ring und ausreichendem Kontrast | §3, §8 |
| 9 | Disabled Controls | Möglichst vermeiden; besser aktiv lassen und mit Hilfetext/Validierung erklären | §3 |
| 10 | Toggle-Button | Status visuell UND semantisch setzen (`aria-pressed`, klare On/Off-Unterscheidung) | §3 |
| 11 | Microinteraction | 100-250 ms, meist `opacity` + `transform`; Zweck vor Effekt | §4 |
| 12 | Komplexe Animation | Einfachstes Werkzeug wählen: CSS Transition → Keyframes → WAAPI/View Transitions → Library | §4 |
| 13 | Route/Page Transition | View Transitions API als Progressive Enhancement, Fallback ohne Animation | §4 |
| 14 | Scroll-Reveal | Native CSS Scroll-Driven Animations (`view()`) bevorzugen, JS nur als Fallback | §5 |
| 15 | Parallax | Dezent, transform-basiert, Reduced-Motion respektieren; nicht als Inhaltsträger | §5, §8 |
| 16 | Spezialeffekte | Gradients, masks, blend modes, backdrop-filter, shader und particles als Layer, nicht als Kern-UI | §6 |
| 17 | Glass/Blur | `backdrop-filter` braucht transparente Fläche; Backdrop-Roots können Wirkung begrenzen | §6 |
| 18 | Blend-Modes | Mit `isolation: isolate` kontrollieren; `plus-lighter` für Crossfades prüfen | §6 |
| 19 | Shader/WebGL | Nur für Hero/Showcase/Produkt-Erlebnis; Fallback-Bild oder CSS-Variante einplanen | §6 |
| 20 | Bilder | LCP-Bild nicht lazy-loaden; `width`/`height`, moderne Formate und Priorität setzen | §7 |
| 21 | Fonts | Wenige Schnitte, Fallback-Metriken prüfen, `preconnect` statt `@import` bei Google Fonts | §7 |
| 22 | Core Web Vitals | LCP/INP/CLS als harte Qualitätsgrenzen; Animation darf INP nicht verschlechtern | §7 |
| 23 | Reduced Motion | `prefers-reduced-motion` in CSS, JS und animierten Medien respektieren | §8 |
| 24 | Accessibility | Tastatur, Screenreader, Kontrast, Zielgröße und Formular-Markup vor visueller Spielerei | §8 |
| 25 | Design-System | Farben, Typo, Radius, Schatten, Easing, Dauer, Z-Index und Breakpoints als Tokens führen | §9 |
