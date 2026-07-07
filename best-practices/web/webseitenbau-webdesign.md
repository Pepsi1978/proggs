# Webseitenbau & visuelles Webdesign — Best Practices (Stand 2026-06-26, Web Platform 2026)

> Fokus: schöne, moderne Webseiten mit starken Layouts, klarer visueller Hierarchie,
> hochwertigen Interaktionen, Microinteractions, Scroll-Effekten, visuellen Spezialeffekten
> und produktionsreifer Performance/Accessibility. Diese Datei ergänzt
> `web/typescript.md` (Build-/Code-Strenge) und `web/3d-threejs-webgpu.md` (echte Web-3D-Szenen).
>
> **Anker:** Web Platform / CSS / Browser-Baseline 2026; MDN `backdrop-filter` Baseline 2024,
> MDN `mix-blend-mode` breit verfügbar seit 2020; Core Web Vitals: LCP < 2,5 s, INP < 200 ms,
> CLS < 0,1. Es gibt keinen einzelnen installierten Versionsanker, weil dieser Bereich
> querschnittlich über Browser, CSS, UI-Design und Frontend-Frameworks läuft.
>
> **Quellen-Rangordnung:** offizielle/nahe Primärquellen zuerst: web.dev, MDN, W3C/WCAG,
> U.S. Web Design System (USWDS), Material Design / NN/g-nahe Interaction-Guides. Trend- und
> Showcase-Quellen (Awwwards, Figma, Codrops, WebGL-Showcases) sind Inspiration, aber sie
> überstimmen niemals Accessibility, Performance oder klare Nutzerführung.

---

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

---

## §1 — Grundsatz: Schön ist nicht gleich beliebig spektakulär

Eine schöne Website entsteht nicht durch möglichst viele Effekte, sondern durch eine klare
Design-Absicht. Vor der ersten Komponente werden vier Dinge festgelegt: Ziel der Seite, primäre
Nutzerhandlung, Markenstimmung und Performance-/Accessibility-Grenzen. USWDS formuliert das als
„start with real user needs“, „earn trust“, „embrace accessibility“, „promote continuity“ und
„listen“; auf private/kommerzielle Websites übertragen heißt das: echte Nutzerbedürfnisse,
Vertrauen, Barrierefreiheit, Konsistenz und Feedback-Schleifen sind die Grundlage jeder visuellen
Entscheidung. Quelle: USWDS Design Principles, 2025-02-21, `[offiziell]`.

**Praktische Leitregel:** Jede visuelle Idee braucht eine Antwort auf „welches Verhalten oder Gefühl
verbessert das?“ Wenn die Antwort nur „sieht cool aus“ lautet, wird der Effekt zu einem optionalen
Layer mit Budget, Fallback und Reduced-Motion-Variante.

---

## §2 — Visuelle Basis: Layout, Typografie, Farbe, Spacing

### Layout

- **Starkes Grid als Fundament.** Auch organische oder Anti-Grid-Layouts wirken nur hochwertig,
  wenn sie auf einem erkennbaren Rhythmus beruhen. Nutze CSS Grid für Seitenstruktur,
  Flexbox für lokale Ausrichtung, und brich das Raster bewusst für Hero-Motive, Editorial-Looks
  oder Portfolio-Szenen.
- **Whitespace ist ein aktives Gestaltungsmittel.** Großzügige Abstände reduzieren kognitive Last
  und machen Scanning leichter. Die Recherche fand Whitespace wiederholt als Qualitätsmerkmal bei
  modernen SaaS-/Produktseiten.
- **Responsive Design ist nicht nur Breakpoint-Wechsel.** Moderne Seiten verändern Hierarchie:
  Hero-Bild, CTA, Trust-Signale und Navigation müssen auf Mobilgeräten in einer anderen Priorität
  erscheinen als auf Desktop.

### Typografie

- **Hero-Typografie darf mutig sein.** Große, fette Headlines, variable Fonts, kinetische Schrift
  und Layering über Bild/Gradient funktionieren gut für erste Wirkung.
- **Lesbarkeit bleibt Pflicht.** Body-Text braucht ruhigen Rhythmus, ausreichende Zeilenhöhe,
  echte Kontraste und ein klar begrenztes Schriftsystem. Maximal 1-2 Familien und wenige Schnitte
  als Startregel.
- **Typografie als Storytelling.** Für Portfolios, Kampagnen und starke Marken darf Schrift ein
  Interaktionselement sein; für Formulare, Pricing und Dokumentation ist Klarheit wichtiger.

### Farbe

- **Signaturfarbe statt Regenbogen.** Eine dominante Markenfarbe plus neutrale Basis wirkt reifer
  als viele gleichlaute Akzente. Vibrant/Neon/Y2K und „Dopamine Design“ funktionieren bei Lifestyle,
  Entertainment und jungen Marken, sind aber kein Universalstil.
- **Dark Mode ist ein Produktzustand, nicht nur invertierte Farben.** Beide Themes brauchen eigene
  Kontrast-, Schatten-, Blur- und Surface-Tokens.
- **Gradients hochwertig bauen.** Große, weiche Gradients brauchen kontrollierte Stopps, leichte
  Noise-Textur gegen Banding und genug Kontrast für Text darüber.

### Spacing / Tokens

Alle wiederkehrenden Werte als Tokens führen: `space`, `radius`, `shadow`, `font-size`, `line-height`,
`color`, `easing`, `duration`, `z-index`. Einzelwerte direkt in Komponenten führen schnell zu einem
„zusammengeklickten“ Aussehen.

---

## §3 — Komponenten, Schalter, Buttons, Forms und Interaktionszustände

Jede interaktive Komponente braucht eigene, konsistente States: Default, hover, active/pressed,
focus, disabled, loading, selected/toggle und error/invalid. Material/NN-g-nahe Quellen betonen,
dass States den Status eines Elements kommunizieren und nicht rein dekorativ sind.

**Buttons:**

- Default-State mit eindeutigem Kontrast und klarer Priorität: Primary, Secondary, Tertiary.
- Hover nur als Zusatzsignal; wesentliche Information darf nie nur im Hover erscheinen, weil Touch
  keinen Hover hat.
- Active/Pressed muss sofort wirken, ungefähr innerhalb von 100-150 ms.
- Loading-State verhindert Doppel-Submits; bei langen Aktionen Fortschritt oder Status erklären.
- Toggle-State semantisch mit `aria-pressed="true/false"` und visuell mit Fill/Icon/Text klar trennen.

**Focus:**

- `outline: none` ohne Ersatz ist ein Fehler. Verwende `:focus-visible`, sichtbare Outline/Ring,
  nicht nur Farbwechsel.
- Focus ist essenziell für Tastatur, Screenreader, Smart-TV und Gamepad-Navigation. Hover ist nur
  ein Maus-Komfortsignal.

**Disabled:**

- USWDS empfiehlt, disabled Form-Inputs möglichst zu vermeiden: niedriger Kontrast, kein Fokus,
  schlechte Screenreader-Kommunikation und Frust. Besser: Feld aktiv lassen, Hilfetext, Tooltip,
  Inline-Validierung oder `aria-disabled="true"` plus JS-Blockade verwenden, wenn der Zustand sichtbar
  und fokussierbar bleiben soll. Quelle: USWDS Form Component, 2026-06-22, `[offiziell]`.

**Forms:**

- Visuelle Reihenfolge muss HTML-Reihenfolge entsprechen; CSS darf Form-Control-Reihenfolge nicht
  „umarrangieren“.
- Thematisch zusammengehörige Controls in `fieldset` + `legend` gruppieren.
- Validierung direkt am Feld ausrichten; Fehler zusätzlich im Seitentitel/Alert für Screenreader
  verfügbar machen.
- Vertikale Formlayouts sind meist zugänglicher als horizontales Scannen.

---

## §4 — Animation & Microinteractions: Werkzeugleiter

**Leitprinzip:** mit dem einfachsten Werkzeug starten und nur bei echtem Bedarf tiefer gehen.

| Bedarf | Werkzeug | Regel |
|--------|----------|-------|
| Hover, Pressed, Expanded, Error | CSS Transitions | 100-250 ms, `ease-out`, `opacity`/`transform` |
| Mehrphasige Bewegung mit Overshoot | CSS Keyframes | kurz, interruptierbar oder unkritisch |
| Sequenzen, Pause, Reverse, Await | Web Animations API | `element.animate()`, `animation.finished`, abbrechbar |
| Route-/State-Kontinuität | View Transitions API | Progressive Enhancement mit Fallback |
| React/Vue komplexe UI-Motion | Motion/Framer Motion | Layout-Animation, Springs, interruptierbar |
| Timeline, SVG Path, spezielle Showcases | GSAP | stark, aber Lizenz-/Bundle-/Architektur bewusst prüfen |

**Timing:** Microinteractions meist 100-250 ms; lange Page-Transitions nur, wenn sie Orientierung
geben. Über 300 ms fühlt sich für Power-User schnell träge an.

**Easing:** `ease-out` als Default; spring-artige Kurven nur für spielerische/dekorative Elemente.
Ein gutes Motion-System dokumentiert Dauer und Easing als Tokens.

**Interruption:** Animationen müssen abbrechbar sein. Nutzer dürfen sich nie in einer Sequenz
gefangen fühlen. CSS Transitions und Motion/WAAPI sind hier oft sicherer als starr durchlaufende
Keyframes.

**View Transitions:** Für Thumbnail → Detail, Karten → Seite, Filter-Reordering und Page-Transitions
`document.startViewTransition()` nutzen, aber immer so bauen, dass die UI ohne API sofort korrekt
aktualisiert. Quelle: Web Animations/View-Transitions-Recherche, 2026-06-26; web.dev/MDN-nahe Quellen.

---

## §5 — Scroll-, Parallax-, Reveal- und Storytelling-Effekte

Native CSS Scroll-Driven Animations binden Keyframes an Scroll-Fortschritt statt Zeit. `scroll()`
koppelt an einen Scrollcontainer; `view()` koppelt an die Position eines Elements im Viewport.
Das ersetzt viele alte `scroll`-Listener/IntersectionObserver-Klassen-Toggle-Muster.

**Best Practices:**

- Reveal-on-scroll: `animation-timeline: view()` und `animation-range: entry ...` nutzen.
- Lesefortschritt: `animation-timeline: scroll()` für Progress-Bar.
- Parallax: nur `transform` animieren, keine Layout-Properties.
- Horizontal sections: echte horizontale Scroller mit `scroll-snap-type` bevorzugen, nicht Scrolljacking.
- Sticky Storytelling: Sticky-Elemente für Orientierung, aber Inhalt darf nicht nur per Animation
  verständlich sein.
- Fallback: Progressive Enhancement; Polyfills laufen oft auf dem Main Thread und bringen nicht den
  nativen Performance-Vorteil.

**Wann vermeiden:** wenn Effekt Navigation ersetzt, Lesefluss stört, Nutzer seekrank macht, Tastatur-
Navigation verschlechtert oder mobile Performance sichtbar leidet.

---

## §6 — High-End-Effekte: Gradients, Blur, Blend, Masken, Shader, Particles

### CSS-Effekte

- `backdrop-filter` erzeugt Glas-/Blur-/Frosted-Layer. Es wirkt auf Pixel **hinter** dem Element;
  das Element braucht transparente/teiltransparente Fläche. Achtung Backdrop-Root: Eltern mit
  `filter`, `opacity < 1`, `mask`, `clip-path`, `backdrop-filter`, `mix-blend-mode` oder passendem
  `will-change` begrenzen den Effektbereich. Quelle: MDN `backdrop-filter`, 2026-04-20, `[offiziell]`.
- `mix-blend-mode` mischt ein Element mit dem Backdrop im gleichen Stacking Context. Für kontrollierte
  Effekte oft `isolation: isolate` setzen. `plus-lighter` kann Crossfades stabilisieren, weil es
  unerwünschtes Blinken bei gegenläufiger Opacity-Animation reduziert. Quelle: MDN `mix-blend-mode`,
  2026-06-08, `[offiziell]`.
- `background-blend-mode`, CSS `filter`, `clip-path`, SVG-Filter und CSS masks sind gute Layer für
  Editorial/Portfolio-Looks, sollten aber einzeln testbar und abschaltbar bleiben.

### Shader / WebGL / WebGPU

Shader, Fluid-Gradients, Particles, 2.5D-Parallax, refractive glass, SDF-Text und WebGPU-Compute
erzeugen den „wow“-Effekt, sind aber eigene Render-Systeme. Setze sie ein für Hero, Kampagnen,
Portfolio, Produktvisualisierung oder emotionale Brand-Momente, nicht für Basiskomponenten.

**Produktionsregeln:**

- Canvas-Effekt als dekorativen Layer kapseln; HTML-Inhalt bleibt semantisch und nutzbar.
- Fallback-Bild oder CSS-Gradient bereithalten.
- Pointer-/Scroll-Effekte drosseln und bei `prefers-reduced-motion` deaktivieren.
- Mobile/GPU-Budget testen; Particles/Fluid nie ungeprüft auf Low-End-Geräten ausrollen.
- WebGL-Ressourcen freigeben (`dispose()` bei Three.js; siehe `web/3d-threejs-webgpu.md`).

### Lottie, Rive, SVG

Für Lottie/Rive/SVG gibt es nach dem Detail-Lauf ein eigenes Gegenstück:
`best-practices/web/lottie-rive-svg-animationen.md` und
`bugs/web/lottie-rive-svg-animationen.md`. Kurzregel: Lottie für After-Effects-artige
Illustrationsanimationen, Rive für interaktive State-Machines, SVG/SMIL/CSS für kleine Icons,
Logos und Stroke-/Morph-Animationen. Jede dieser Animationen braucht einen statischen Fallback,
Reduced-Motion-Verhalten und einen Browser-/Accessibility-Test.

---

## §7 — Performance: Effekte dürfen die Seite nicht „teuer“ machen

Core Web Vitals bleiben harte Grenzen: LCP < 2,5 s, INP < 200 ms, CLS < 0,1. Quelle: web.dev / CWV,
2026-Recherche, `[offiziell/nah]`.

**Animation Performance:**

- Bevorzugt `transform` und `opacity`; vermeide `width`, `height`, `padding`, `margin`, `top`, `left`,
  weil sie Layout/Paint/Composite auslösen können. Quelle: web.dev `How to create high-performance CSS animations`, `[offiziell]`.
- `will-change` nur kurz und gezielt; es ist ein Hint, kein Allheilmittel, und kostet Speicher.
- Layout-Thrash vermeiden: erst alle Reads, dann Writes.
- Bei `requestAnimationFrame` Delta-Time nutzen, nicht „pro Frame“ rechnen; 120-Hz-Displays sonst falsch.

**LCP / Bilder:**

- Hero-/LCP-Bild nicht lazy-loaden.
- `width`/`height` oder `aspect-ratio` setzen, damit kein CLS entsteht.
- Above-the-fold-Bild priorisieren (`fetchpriority="high"`, Framework-Äquivalent, z. B. Next `priority`).
- Moderne Formate (AVIF/WebP), responsive `srcset`, saubere Kompression.

**Fonts:**

- Wenige Schnitte laden, lokale/systemnahe Fallbacks mit ähnlichen Metriken wählen.
- Google Fonts per `<link rel="preconnect">`/Stylesheet, nicht `@import`.
- Font-Swap auf CLS prüfen.

**Testing:** Lighthouse, PageSpeed Insights, Chrome DevTools Performance/Network/Rendering,
DebugBear oder vergleichbares RUM/Lab-Werkzeug. Immer throttled testen, nicht nur auf dem schnellen
Entwicklungsrechner.

---

## §8 — Accessibility: schöne Effekte nur mit Ausweg

WCAG ist Mindeststandard, nicht Luxus. Für visuell reiche Websites besonders wichtig:

- Tastaturbedienung vollständig erhalten.
- Fokus sichtbar und kontrastreich.
- Textkontrast in jedem State prüfen, nicht nur im Default.
- Zielgrößen mobil ausreichend groß halten; 44x44 px ist eine robuste Startregel aus UI-Guides.
- Farbänderung nie als einziges Signal verwenden; Form, Icon, Text oder Outline ergänzen.
- Bewegung aus Interaktion deaktivierbar machen, wenn sie nicht wesentlich ist.

`prefers-reduced-motion` ist Pflicht. web.dev erklärt, dass Parallax, Zoom, Reveal, Autoplay-Videos
und bewegte Dekoration Nutzer ablenken oder vestibuläre Symptome auslösen können. CSS, JS und Medien
müssen die Präferenz respektieren. Quelle: web.dev `prefers-reduced-motion`, 2019-03-11,
`[offiziell/grundlegend]`.

```css
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    animation-duration: 1ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 1ms !important;
    scroll-behavior: auto !important;
  }
}
```

Für JS-Animationen zusätzlich `matchMedia('(prefers-reduced-motion: reduce)')` beobachten und laufende
Animationen abbrechen oder gar nicht starten.

---

## §9 — Produktionsarchitektur: Design-System statt Einzelkunstwerk

Eine schöne Seite bleibt nur wartbar, wenn sie als System gebaut wird:

- **Tokens:** Farbe, Typografie, Spacing, Radius, Schatten, Easing, Dauer, Breakpoints,
  Container-Größen, Z-Index.
- **Komponenten:** Button, Link, Card, Nav, Modal, Form-Field, Toggle, Tabs, Accordion, Toast,
  Tooltip jeweils mit vollständigen States und Accessibility-Verhalten.
- **Patterns:** Hero, Feature-Section, Social Proof, Pricing, FAQ, Footer, Portfolio-Case, Product-Detail,
  Storytelling-Scroll, Gallery, Contact/Form.
- **Effekt-Layer:** Motion, scroll-driven effects, WebGL/Shader, Lottie/Rive/SVG getrennt von Inhalt
  und Semantik halten.
- **Qualitäts-Gates:** Lighthouse/CWV, axe/Accessibility, Tastatur-Test, Reduced-Motion-Test,
  mobile Low-End-Test, Dark/Light-Test, forced-colors/high-contrast-Test.

Framework-Wahl als Faustregel:

| Ziel | Gute Wahl |
|------|-----------|
| Content-/Marketing-Seite mit Top-Performance | Astro, statisch/SSR, Inseln nur wo nötig |
| App-artige Interaktion | React/Next, SvelteKit oder Vue/Nuxt mit Design-System |
| Schnelle visuelle Prototypen | Webflow/Framer/Figma Sites, danach Performance prüfen |
| 3D/Shader-heavy Showcase | Three.js/OGL/Babylon; Details in `web/3d-threejs-webgpu.md` |

Tailwind kann sehr schnell schöne Oberflächen liefern, muss aber durch Tokens/Komponenten diszipliniert
werden. Sonst entstehen inkonsistente Utility-Sammlungen. CSS Custom Properties bleiben die beste
gemeinsame Schicht zwischen Design Tokens, Themes und Frameworks.

---

## §10 — Praktische Bau-Rezepte

**Premium-Hero:** große Headline, kurzer Subtext, ein Primary CTA, ein Secondary Link, ein einziges
Hero-Motiv. Motion: Text fade+translate 150-220 ms, Motiv 220-300 ms, Reduced-Motion = statisch.

**Schöner Schalter:** Track + Thumb, klare On/Off-Farbe, Icon/Text optional, `role="switch"` oder
native Checkbox mit Label, Fokus-Ring, `aria-checked`, 150 ms `transform`.

**Card-Hover:** keine Layout-Verschiebung; `transform: translateY(-2px)` + Shadow/Border-Token,
180 ms `ease-out`, Touch-Alternative ohne Hover-Abhängigkeit.

**Scroll-Reveal:** Fade + kleiner Translate, `view()` wenn verfügbar; nur Content enthüllen, nicht
Content verstecken. Reduced-Motion zeigt Elemente sofort.

**Glass Panel:** halbtransparente Surface, `backdrop-filter: blur(...)`, Border-Highlight, genug
Kontrast für Text, Fallback mit solid Surface.

**Particle/Shader-Hero:** Canvas `aria-hidden="true"`, statisches Poster als Fallback, FPS/CPU/GPU
budgetieren, auf Mobilgeräten degradieren.

---

## §11 — Typische Fallen (Gegenstück zum Bug-Almanach)

Für diesen Querschnittsbereich existiert jetzt ein eigenes Bug-Almanach-Pendant:
`bugs/web/webseitenbau-webdesign.md`. Die wichtigsten wiederverwendbaren Fallen aus der Recherche:

| Falle | Ursache | Funktionserhaltende Sofortregel |
|-------|---------|----------------------------------|
| Effekt-Suppe macht Seite unbenutzbar | Zu viele Motion-/Shader-/Scroll-Layer ohne Zweck | Effekt-Budget pro Seite, Reduced Motion, Performance-Test |
| Hover-only UI | Information steckt nur in Desktop-Hover | Touch- und Tastaturzustand gleichwertig bauen |
| Focus entfernt | `outline: none` ohne Ersatz | `:focus-visible`-Ring als Token |
| Scrolljacking | Animation kontrolliert Scroll statt ihn zu begleiten | Native Scroll/Snap/Timeline, kein blockierender Scroll-Loop |
| CLS durch Bilder/Fonts | Keine reservierte Fläche, Font-Swap | `width`/`height`, `aspect-ratio`, Fallback-Metriken |
| Backdrop-Blur wirkt nicht | Backdrop-Root durch Parent mit `opacity/filter/mask/...` | Stacking/Backdrop-Root prüfen, Effekt isolieren |
| Blend-Mode verfärbt unerwartet | Fehlende Isolation im Stacking Context | `isolation: isolate` und Hintergrund explizit setzen |
| Animation ruckelt | Layout-Properties oder Main-Thread-Scrolllistener | `transform`/`opacity`, native Scroll-Animation, DevTools-Profil |

---

## Bezug zum Bug-Almanach

| Best-Practice | Bug-Almanach |
|---------------|--------------|
| `best-practices/web/webseitenbau-webdesign.md` | `bugs/web/webseitenbau-webdesign.md` |

---

## §12 — Quellen

- web.dev: `How to create high-performance CSS animations`, `[offiziell]`, abgerufen 2026-06-26.
- web.dev: `prefers-reduced-motion: Sometimes less movement is more`, `[offiziell]`, abgerufen 2026-06-26.
- MDN: `backdrop-filter CSS property`, `[offiziell]`, zuletzt geändert 2026-04-20, abgerufen 2026-06-26.
- MDN: `mix-blend-mode CSS property`, `[offiziell]`, zuletzt geändert 2026-06-08, abgerufen 2026-06-26.
- W3C/WAI: WCAG 2.2 Quick Reference und WCAG 2.2, `[offiziell]`, abgerufen 2026-06-26.
- USWDS: Design Principles, `[offiziell/Design-System]`, aktualisiert 2025-02-21, abgerufen 2026-06-26.
- USWDS: Form Component, `[offiziell/Design-System]`, veröffentlicht/aktualisiert 2026-06-22, abgerufen 2026-06-26.
- Engine-A-Researcher-Lauf 2026-06-26: 6 Teilbereiche zu Layout/Trends, Interaction States,
  Animation/Microinteraction, Scroll Effects, High-End Visual Effects und Performance/Architecture;
  danach Bug-Almanach-Rückkopplung in `bugs/web/webseitenbau-webdesign.md`.
