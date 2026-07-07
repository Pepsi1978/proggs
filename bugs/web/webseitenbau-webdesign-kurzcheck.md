# Webseitenbau, Webdesign & visuelle Effekte Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Mobile Seite wirkt herausgezoomt | `<meta name="viewport" content="width=device-width, initial-scale=1">` setzen | §1 |
| 2 | Horizontaler Scroll auf Mobile | Keine festen Breiten; Bilder `max-width:100%`, Layout mit Grid/Flex/relativen Einheiten | §2 |
| 3 | Layout springt beim Laden | Bilder/Embeds mit `width`/`height` oder `aspect-ratio` reservieren | §3 |
| 4 | Container Query greift nicht | Explizit `container-type` am Vorfahren setzen; Container kann sich nicht selbst queryen | §4 |
| 5 | Custom-Button nicht per Tastatur bedienbar | Native `<button>`/`<a>` bevorzugen; sonst Rolle, `tabindex`, Enter+Space-Handler | §5 |
| 6 | Fokus nicht sichtbar | `outline: none` nur mit sichtbarem `:focus-visible`-Ersatz | §6 |
| 7 | Status nur per Farbe/Hover | Text/Icon/Outline ergänzen; Hover nie als einziges Signal | §7 |
| 8 | GSAP-Animation springt/ruckelt | Keine CSS-Transitions auf demselben Element wie GSAP | §8 |
| 9 | ScrollTrigger/ScrollSmoother verhält sich falsch | Kein globales `scroll-behavior: smooth` neben GSAP-Scroll-Tools | §9 |
| 10 | `animation-timeline` wirkt nicht | `animation-timeline` **nach** dem `animation`-Shorthand deklarieren | §10 |
| 11 | Scroll-Driven Animation bleibt aus | Scroller muss wirklich overflowen; bei `view()`-Problemen `overflow: clip` statt `hidden` prüfen | §10 |
| 12 | Backdrop-Blur unsichtbar | Element braucht Transparenz; Backdrop-Root/Stacking Context prüfen | §11 |
| 13 | Blend-Mode verfärbt unerwartet | `isolation: isolate`; Blend auf Pseudo-Element auslagern, wenn Text lesbar bleiben muss | §12 |
| 14 | LCP ist schlecht trotz schöner Hero-Grafik | Hero/LCP-Bild nicht lazy-loaden; preload + `fetchpriority="high"`; JS im Head `defer`/`async` | §13 |
| 15 | INP schlecht trotz „fertiger“ Seite | Long Tasks, Drittanbieter-Scripts und DOM-Bloat reduzieren; DevTools Performance messen | §14 |
| 16 | Lab grün, Field rot | PageSpeed/Lighthouse-Lab nicht mit CrUX/RUM verwechseln; Field-Daten priorisieren | §15 |
