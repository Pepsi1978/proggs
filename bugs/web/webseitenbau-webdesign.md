# Bekannte Bugs: Webseitenbau, Webdesign & visuelle Effekte

> **PFLICHT-LESEN VOR DER ARBEIT.** Kuratierter Bug-Almanach für moderne Websites,
> responsive Layouts, Interaktionszustände, Scroll-/Motion-Effekte, visuelle CSS-Effekte
> und produktionsreife Core-Web-Vitals. Stand recherchiert **2026-06-26** für Web Platform
> 2026, Chrome/Edge 149 als lokale Browser-Anker ohne belegten versionsspezifischen Bugstatus.
> Gegenseite (Best Practices, das WIE-richtig): `best-practices/web/webseitenbau-webdesign.md`.

---

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

---

## §1 — Mobile Seite herausgezoomt: fehlender/falscher Viewport

- **Symptom:** Mobile Browser rendern die Seite in Desktop-Breite; Text wirkt winzig, Nutzer müssen zoomen.
- **Ursache:** Ohne Viewport-Meta-Tag nimmt der Browser eine breite Layout-Viewport-Annahme statt der Gerätebreite.
- **Versionen:** Generell mobile Browser; keine konkrete Chrome-/Edge-/Safari-Version in den Quellen belegt.
- **FIX:**
  ```html
  <meta name="viewport" content="width=device-width, initial-scale=1">
  ```
  Lighthouse-Audit für fehlenden Viewport aktiv lassen.
- **Quelle:** web.dev `Responsive web design basics`, `[offiziell]`, Researcher 1.

## §2 — Feste Breiten/Bilder erzwingen Horizontal-Scroll

- **Symptom:** Auf schmalen Viewports entsteht horizontaler Scroll; Inhalte werden abgeschnitten.
- **Ursache:** `width: 800px`, feste Spalten oder Bilder ohne Begrenzung überschreiten den Viewport.
- **Versionen:** Alle Browser, keine versionsspezifische Quelle.
- **FIX:** Mobile-first bauen, Layout mit Grid/Flex/relativen Einheiten, Bilder defensiv:
  ```css
  img, svg, video { max-width: 100%; height: auto; display: block; }
  ```
  Breakpoints in `rem`/`em` nach Inhalt wählen, nicht nach Geräte-Liste.
- **Quellen:** web.dev Responsive Basics; MDN Responsive Design, `[offiziell]`, Researcher 1.

## §3 — CLS durch Bilder, Fonts und spät injizierte Inhalte

- **Symptom:** Text/Bilder springen beim Laden; CLS > 0,1.
- **Ursache:** Browser kennt Größe von Bildern/Embeds nicht; Web-Font-Metriken passen nicht; Banner/Popups werden nachträglich in den Flow eingefügt.
- **FIX:** `width`/`height` oder `aspect-ratio` setzen, Embed-Slots reservieren, Font-Swap mit passenden Fallback-Metriken testen, späte UI in reservierte Container rendern.
- **Quellen:** web.dev Responsive Basics; Core-Web-Vitals-Recherche, Researcher 1 und 6.

## §4 — Container-Query-Fallen

- **Symptom:** `@container`-Regeln greifen nicht oder Komponenten verhalten sich unerwartet.
- **Ursachen:** Kein expliziter Container; Container versucht sich selbst zu queryen; `container-type: size` kappt intrinsische Größe; `container`-Shorthand ohne Namen ist ungültig.
- **FIX:**
  ```css
  .card-list { container: cards / inline-size; }
  @container cards (width > 40rem) { .card { grid-template-columns: 1fr 2fr; } }
  ```
  Für Komponenten meist `inline-size`, nicht `size`; Media Queries für Viewport-/User-Preference-Logik behalten.
- **Quellen:** Chrome/LogRocket/MDN-nahe Container-Query-Recherche, Researcher 1.

## §5 — Custom-Controls ohne Tastatur/Semantik

- **Symptom:** Visueller Button/Toggle/Tooltip ist per Tab nicht erreichbar, Enter/Space lösen nichts aus, Screenreader meldet keinen Namen/Rolle/Wert.
- **Ursache:** `<div>`/`<span>` mit `onclick` statt nativer Controls.
- **FIX:** Native `<button>`, `<a>`, `<input>`, `<select>` bevorzugen. Falls Custom unvermeidbar: `role`, `tabindex="0"`, `aria-*`, Click sowie Enter- und Space-Handler; bei komplexen Komponenten WAI-ARIA-APG-Pattern nutzen.
- **Quellen:** WCAG 2.1 SC 2.1.1/4.1.2; Intopia Accessibility Audit; NN/g Button States, Researcher 2.

## §6 — Fokus-Indikator entfernt oder zu schwach

- **Symptom:** Beim Tabben ist nicht sichtbar, welches Element aktiv ist.
- **Ursache:** CSS-Reset mit `outline: none` ohne Ersatz; Fokus nur per Farbwechsel.
- **FIX:** `:focus-visible` mit kontrastreichem Ring/Outline als Design-Token. Custom-Indikatoren brauchen mindestens 3:1 Kontrast zum Hintergrund; Feedback innerhalb von 100-150 ms.
- **Quellen:** WCAG 2.4.7; Intopia; NN/g Button States, Researcher 2.

## §7 — Hover/Farbe als einziges Signal

- **Symptom:** Touch- oder Tastaturnutzer erkennen Klickbarkeit/Status nicht; farbsehschwache Nutzer erkennen Fehler/Links/Selected State nicht.
- **Ursache:** Affordance nur per Hover oder Farbe.
- **FIX:** Enabled-State, Text, Icon, Unterstreichung, Outline oder Pattern ergänzen. Hover ist Zusatz, nicht Informationsträger. Tooltips auch per Fokus/Touch erreichbar machen und mit `aria-describedby` verknüpfen.
- **Quellen:** NN/g; WCAG-Kontrast/Farbe; Researcher 2.

## §8 — GSAP und CSS-Transitions kämpfen gegeneinander

- **Symptom:** Elemente springen zur Startposition zurück, Nachbarkarten bewegen sich unerwartet, Hover wirkt ruckelig.
- **Ursache:** GSAP schreibt laufend Werte; CSS-Transition versucht dieselben Properties gleichzeitig zu interpolieren.
- **FIX:** Auf Elementen, die GSAP kontrolliert, keine CSS-Transitions für dieselben Properties. Hover/Rollover ebenfalls mit GSAP oder rein mit CSS bauen, nicht mischen.
- **Quelle:** GSAP-Forum/GreenSock-Empfehlung, Researcher 3.

## §9 — `scroll-behavior: smooth` stört GSAP-Scroll-Tools

- **Symptom:** ScrollTrigger/ScrollSmoother-Setups wirken zeitversetzt oder falsch.
- **Ursache:** Browser-Smooth-Scroll und JS-Smooth-Scroll kontrollieren denselben Scrollzustand.
- **FIX:** Bei GSAP ScrollTrigger/ScrollSmoother kein globales `scroll-behavior: smooth`; Smoothness ausschließlich über ein System steuern.
- **Quelle:** GSAP-Forum, Researcher 3.

## §10 — Scroll-Driven-Animation-Fallen

- **Symptom:** `animation-timeline: view()` oder `scroll()` greift nicht.
- **Ursachen:** `animation`-Shorthand setzt `animation-timeline` zurück; Scroller overflowt nicht; Vorfahre mit `overflow: hidden` stört View-Timeline.
- **FIX:**
  ```css
  .reveal {
    animation: fade-in linear both;
    animation-timeline: view();
    animation-range: entry 20% cover 40%;
  }
  ```
  Timeline nach Shorthand deklarieren; echten Overflow sicherstellen; bei `view()`-Ausfall `overflow: clip` statt `hidden` testen.
- **Quellen:** MDN Scroll-Driven Animations; StackOverflow-Fall `overflow: clip`, Researcher 4.

## §11 — `backdrop-filter` sieht aus, als wäre es kaputt

- **Symptom:** Glass/Blur-Effekt ist unsichtbar oder begrenzt sich unerwartet.
- **Ursache:** Element ist nicht transparent genug; Backdrop Root wird durch `filter`, `opacity < 1`, `mask`, `clip-path`, `backdrop-filter`, `mix-blend-mode` oder passendes `will-change` begrenzt.
- **FIX:** Halbtransparente Surface setzen, Backdrop-Root-Kette prüfen, Effekt-Layer isolieren, Fallback-Surface ohne Blur bereitstellen.
- **Quellen:** MDN/Spec-Recherche `backdrop-filter`, Researcher 5.

## §12 — `mix-blend-mode` verfärbt mehr als geplant

- **Symptom:** Text wird unleserlich, Blend reagiert anders in Safari/Chrome/Firefox, Hintergrundelemente werden unerwartet einbezogen.
- **Ursache:** Blend arbeitet im Stacking Context und betrifft die gesamte Box inklusive Text.
- **FIX:** `isolation: isolate` am passenden Vorfahren setzen. Blend auf `::before`/`::after` auslagern, wenn Text lesbar bleiben muss. `plus-lighter` für Crossfades prüfen.
- **Quellen:** MDN `mix-blend-mode`; Stacking-Context-Recherche, Researcher 5.

## §13 — LCP-Fails durch Hero- und Head-Setup

- **Symptom:** Hero-Bild erscheint spät; LCP > 2,5 s.
- **Ursachen:** Render-blocking Script im `<head>`; LCP-Bild wird nicht vorgeladen; Hero-Bild zu groß; LCP-Bild fälschlich lazy-loaded.
- **FIX:** Nichtkritisches JS `defer`/`async`; Hero/LCP-Bild preloaden und `fetchpriority="high"`; responsive `srcset`; LCP-Bild niemals `loading="lazy"`.
- **Quellen:** Core-Web-Vitals-Recherche, web.dev/PageSpeed/DevTools-Fälle, Researcher 6.

## §14 — INP-Fails durch Long Tasks, Third-Party und DOM-Bloat

- **Symptom:** Seite sieht fertig aus, reagiert aber 500+ ms verzögert auf Klicks.
- **Ursache:** JS-Tasks > 50 ms, Page-Builder-/Plugin-Bloat, Analytics/Chat/Ads auf Main Thread, große DOM-Bäume.
- **FIX:** DevTools Performance-Profil; schwere Tasks splitten; nichtkritische Drittanbieter-Skripte nach Interaktion laden; DOM verschlanken; bedingtes Plugin-/Asset-Loading.
- **Quellen:** Core-Web-Vitals-Recherche, Researcher 6.

## §15 — Lab-vs-Field-Diagnosefalle

- **Symptom:** Lighthouse/PageSpeed-Lab ist grün, echte Nutzerwerte oder Search-Console-CWV sind rot.
- **Ursache:** Lab simuliert einen Lauf; Field/CrUX misst echte Geräte, Netze, Caches und Drittanbieter.
- **FIX:** Lab für Debugging, Field/RUM für Priorisierung. Wenn keine CrUX-Daten existieren: Lab als Proxy kennzeichnen, nicht als Beweis für echte Nutzerqualität.
- **Quellen:** PageSpeed/CrUX-Recherche, Researcher 6.

---

## Bezug zu Best Practices

| Dieser Almanach | Gegenstück |
|-----------------|------------|
| `bugs/web/webseitenbau-webdesign.md` | `best-practices/web/webseitenbau-webdesign.md` |

---

## Quellen

- web.dev: `Responsive web design basics`, `How to create high-performance CSS animations`, `prefers-reduced-motion`, Core Web Vitals, `[offiziell]`, abgerufen/recherchiert 2026-06-26.
- MDN: Responsive Design, Scroll-Driven Animations, `backdrop-filter`, `mix-blend-mode`, `[offiziell]`, abgerufen 2026-06-26.
- WCAG/WAI, Intopia Accessibility Audits, NN/g Button-State-Guidance, Researcher 2, 2026-06-26.
- GSAP/GreenSock-Forum-Fälle zu CSS-Transition-Interferenz und ScrollSmoother, Researcher 3, 2026-06-26.
- Core-Web-Vitals-/PageSpeed-/DevTools-Fallauswertung, Researcher 6, 2026-06-26.
- Hinweis: Die Recherche fand **keine belastbaren Quellen** für konkrete „fixed vs. still active“-Aussagen zu Chrome 149 oder Edge 149. Diese Datei markiert solche Lücken bewusst, statt Versionsstatus zu erfinden.
