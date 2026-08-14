# Best Practices: KI-Designstudio (Spec → Design)

Stand: 14.08.2026 · Bereich: KI-gestützte Design-Erzeugung, Multi-Screen-Konsistenz, Vorschau-Rendering,
parallele Änderungsaufträge, Geschwindigkeit
Kurzcheck: `ki-designstudio-kurzcheck.md` · Gegenseite (Bugs): `bugs/design/design-export-und-messung.md`
Quellenlage: Schwarm-Recherche mit sieben parallelen Researchern, 14.08.2026, rund 120 Quellen
(offizielle Anthropic-Doku, arXiv-Papers, Browser-Bugtracker, Engineering-Blogs, Nutzerberichte).

---

## §1 — Erst der Kontrakt, dann die Bildschirme

Die am häufigsten belegte Technik gegen auseinanderlaufende Bildschirme: **zuerst** eine
Design-Token-Datei bzw. einen Component-Contract erzeugen, **danach** die Bildschirme. Nicht
umgekehrt aus fertigen Bildschirmen ableiten.

Der Kontrakt enthält nicht nur Farben, Abstände und Typografie, sondern ausdrücklich auch die Maße
der wiederkehrenden Bauteile: Höhe der Navigationsleiste, Symbolgröße, Schriftgröße der Einträge,
Effekte. Genau diese Werte laufen sonst auseinander.

- Der Contract-Ansatz hebt in der zitierten Messung den Konsistenz-Score von 69 auf 100 von 100.
- Ein **Golden Screen** (ein bereits fertiger Bildschirm als Beispiel im Prompt) ist der zweite
  große Hebel: ihn wegzulassen senkt die Konsistenz-Metriken um **38–42 %**.

**Wie Claude es selbst macht:** Der eingebaute Design-Skill sucht *zuerst* nach einem vorhandenen
Design-System im Projekt und trifft erst dann eigene Entscheidungen. Die Präzedenz ist wörtlich
dokumentiert: *„Claude treats your design system as higher precedence than its own choices, and your
prompt as higher precedence than both."* Tokens liegen als CSS Custom Properties am `:root`, das
Theme-Modell ist dreistufig (hell / dunkel / System über `data-theme` + `prefers-color-scheme`).

Quellen: `code.claude.com/docs/en/artifacts` · https://christinevallaure.substack.com/p/design-system-contracts-the-component ·
https://hvpandya.com/llm-design-systems · https://arxiv.org/html/2607.28645

---

## §2 — Wiederkehrende Leisten einmal bauen, dann einsetzen

Das Navigations-Chrome wird **einmal** als eigene Komponente erzeugt und danach per Vorlage
(Slot/Injection) in jeden Bildschirm eingesetzt — nie erneut vom Modell frei texten lassen. Das
MobileForge-Paper benennt Cross-Page-Consistency als größte Schwachstelle solcher Verfahren und
führt sie genau darauf zurück.

Liegt das Chrome bereits als Quelle vor (fertige Bildschirmdatei, gemeinsame Komponente im
Quellcode), wird **abgeschrieben statt beschrieben**. Eine Beschreibung in Prosa lässt jedem Aufruf
Spielraum; eine Datei nicht.

---

## §3 — Ein Aufruf je Bildschirm, gemeinsamer fester Kontext

Es gibt keinen Beleg dafür, dass ein einzelner Mega-Aufruf für alle Bildschirme überlegen wäre. Im
Gegenteil: v0, Lovable, Bolt und Figma Make arbeiten alle mit **geteiltem Kontext plus Einzelaufruf
je Bildschirm**. Ein Mega-Aufruf läuft zudem in die Ausgabegrenze und lässt Bildschirme weg.

Der geteilte Kontext ist dabei kein loser Hinweis, sondern derselbe wörtliche Text in jedem Aufruf —
das ist zugleich die Voraussetzung für den Zwischenspeicher (§7).

---

## §4 — Konsistenz messen, nicht nur anweisen

Eine Prompt-Anweisung allein genügt nicht: Kein Aufruf kann prüfen, was die anderen getan haben.
Deshalb gehört nach dem Zusammensetzen eine **programmatische** Prüfung dazu.

Bewährte Stufen:
1. **Statisches Audit** — scannt den erzeugten Code auf fest eingetragene Werte, wo ein Token stehen
   müsste; blockiert die Übernahme bei Abweichung.
2. **Vergleich der gemeinsamen Bauteile** — dieselbe Leiste über alle Bildschirme hinweg auf Höhe,
   Innenabstand, Hintergrund, Radius, Schrift- und Symbolgrößen und Beschriftungen vergleichen,
   Referenz ist der Startbildschirm. Abweichungen als benannte Punkte in den Korrekturlauf geben.
3. **Visuelle Regressionstests je Komponente** (nicht nur je Bildschirm), mit KI-gestütztem Diffing,
   damit Rendering-Rauschen nicht als Fehler durchschlägt.
4. **Cross-Page-Consistency als eigene, gemessene Kennzahl** im Qualitätstor führen — nicht nur die
   Qualität einzelner Bildschirme bewerten.

Der aktuellste öffentliche LLM-UI-Benchmark (gendesigns.ai, Aug. 2026) prüft Multi-Screen-Konsistenz
**gar nicht** — wer sie messen will, muss es selbst tun.

---

## §5 — Kein Wert ohne Ort

Ein Effekt, dem die Ortsangabe fehlt, wird beim Nachbauen weggelassen — lautlos. Wird ein Schatten
als `design.html:shadow(3)` geführt, ist der Wert zwar da, aber niemand weiß, an welches Element er
gehört.

**Regel:** Beim Auslesen den Ort mitschreiben — den CSS-Selektor, in dessen Block der Effekt steht,
oder bei einer Angabe direkt am Element dessen Tag und Klasse (`div.karte.gross (shadow)`). Dasselbe
gilt für Radien und Schriften.

**Regel:** Bringt ein Paket die fertigen Bildschirmdateien mit, sind sie die Quelle des Nachbaus —
nicht die Beschreibung. Pfadangaben in Übergabetabellen deshalb **vollständig** schreiben, nie
abgekürzt (`…-heute.html` trifft keine Datei).

---

## §6 — Vorschau: was Effekte kostet

**Backdrop-Root.** `backdrop-filter` sammelt seinen Hintergrund nur bis zum nächsten Backdrop-Root.
`filter`, `opacity` unter 1, `mask`, `mix-blend-mode`, `will-change` oder ein weiteres
`backdrop-filter` auf einem **Vorfahren** beenden die Sammlung — der Effekt bleibt wirkungslos.
`transform` auf einem Vorfahren erzeugt zusätzlich einen neuen Bezugsrahmen. Safari braucht 2026
weiterhin `-webkit-backdrop-filter`.

**Sanitizing.** DOMPurify und sanitize-html entfernen `style`-Attribute und `style`-Tags in der
Voreinstellung. Wer Design-Effekte erhalten will, braucht **CSSOM-basiertes Property-Whitelisting**;
Regex-Filter führen entweder zu einer XSS-Lücke oder zum Effektverlust.

**CSP.** Das `csp`-Attribut eines iframes kann eine restriktivere Eltern-CSP **nicht** lockern (vom
W3C als „wontfix" geführt). Wer generiertes HTML vollständig rendern will, braucht eine **eigene
Preview-Origin mit eigener CSP** statt `srcdoc` plus `csp`-Attribut.

**Skalierung.** `transform: scale()` mit gebrochenem Faktor erzeugt Randartefakte (Chromium 600120)
und kostet Schärfe bei Schatten und Filtern (WebKit 133801). Gegenmittel: die Fläche unter dem
Rahmen in der Farbe des Designs führen, dazu einen gleichfarbigen Saum von einem Pixel; `isolation:
isolate` und `backface-visibility: hidden` am Rahmen. Chrome DevTools umgeht das Problem ganz, indem
es einen echten Device-Pixel-Ratio setzt statt per CSS zu skalieren — der sauberste Weg, wenn er
verfügbar ist.

Alle Einzelfälle mit Bug-Nummern: `bugs/design/design-export-und-messung.md`, B-09 bis B-13.

---

## §7 — Geschwindigkeit ohne Qualitätsverlust

**Zwischenspeicher des Anbieters.** Der stabile Teil des Prompts gehört an den **Anfang** und muss
Wort für Wort identisch sein. Cache-Read kostet rund 90 % weniger und antwortet deutlich schneller.
Anthropic verlangt eine ausdrückliche Markierung (`cache_control`) und mindestens 512–4096 Token je
nach Modell, TTL 5 Minuten oder 1 Stunde; OpenAI cacht ab 1024 Token automatisch (50 % Rabatt);
OpenRouter unterstützt Sticky Routing, damit die Anfragen denselben Anbieter treffen.
**Falle:** Ein Breakpoint auf einem Block, der sich ändert (Zeitstempel!), führt zu dauerhaftem
Cache-Miss trotz identischem Präfix — den Breakpoint auf den letzten *stabilen* Block setzen.

**Nicht die ganze Datei neu erzeugen.** Fast-Apply schlägt Vollausgabe deutlich: Cursor Speculative
Edits erreicht rund 1000 Token/s (13-facher Durchsatz), Morph 10.500 Token/s bei 98 % Genauigkeit,
insgesamt über 90 % weniger Latenz als das Neuschreiben ganzer Dateien. Bei den Diff-Formaten schlägt
Unified Diff das Suchen/Ersetzen-Format deutlich (Laziness-Score 20 % → 61 %).
**Falle:** Von Modellen erfundene Zeilennummern lassen striktes `git apply` scheitern — fehlertolerant
anwenden (`git am -3` oder Patch-Korrektur), oder gleich suchbasiert arbeiten.

**Wahrgenommene Zeit.** Streaming senkt die Zeit bis zum ersten Zeichen von 5–30 s auf 200–500 ms und
wirkt rund 40 % schneller. Skeleton-of-Thought (erst das Gerüst, dann die Teile parallel füllen)
bringt bis zu 2,39-fache Beschleunigung — dasselbe Muster wie §1 plus §3.

**Arbeitsteilung.** Ein großes Modell plant und prüft, ein kleines, spezialisiertes führt die
mechanische Änderung aus. Speculative Decoding bringt allgemein das Zwei- bis Dreifache ohne
Qualitätsverlust.

Quellen: `platform.claude.com/docs/en/build-with-claude/prompt-caching` · https://www.morphllm.com/fast-apply-model ·
https://fireworks.ai/blog/cursor · https://aider.chat/docs/unified-diffs.html · arXiv 2307.15337

---

## §8 — Mehrere Aufträge auf einem Dokument

Der Branchenkonsens (Cursor, Copilot Workspace, Devin, Claude Code) ist einheitlich:
**parallel erzeugen, seriell anwenden.** Niemand setzt für KI-Aufträge volles OT oder CRDT ein.

- Während des Erzeugens: Scope- bzw. Worktree-Isolation.
- Beim Anwenden: ein serielles Tor mit Sperre und Versions-Token (optimistische Nebenläufigkeit).
  Entscheidend ist, dass der Stand **innerhalb** der Sperre frisch gelesen wird — dann setzt ein
  später fertiger Auftrag auf dem Ergebnis des früheren auf, statt es zu überschreiben.
- Suchbasierte Änderungen (Suchen/Ersetzen) sind hier im Vorteil: sie finden ihre Stelle im frischen
  Text oder melden ehrlich, dass es sie nicht mehr gibt.
- **Konflikte niemals automatisch mergen.** Ein automatischer Löser optimiert Syntax, nicht Absicht —
  das Ergebnis ist übersetzbar und trotzdem falsch. Konflikt anzeigen oder neu erzeugen lassen.
- Umsetzung im Node-Umfeld: zwei Warteschlangen (erzeugen mit Nebenläufigkeit 5, anwenden mit 1),
  über einen Flow verbunden; Live-Status über WebSocket.
- Statusschema für die Oberfläche: wartet / läuft / wird eingearbeitet / fertig / fehlgeschlagen /
  Konflikt, mit Abbrechen und Wiederholen je Auftrag.

Quellen: https://docs.bullmq.io/guide/flows · https://database.guide/what-is-optimistic-concurrency-control/ ·
arXiv 2606.17182 · https://agentmarketcap.ai/blog/2026/04/10/devin-parallel-sessions-multi-agent-concurrency

---

## §9 — Was moderne Designwerkzeuge können (Stand 2026)

**Etablierter Standard:** visuelles Auswählen in der Vorschau, Eigenschaften-Bereich für Abstand,
Typografie, Farbe, Radius und Schatten (ohne Prompt-Zwang), Ebenen-/Hierarchie-Bereich, Code-Export,
GitHub-Abgleich, Design-Token-System, Checkpoints.

**Neu und differenzierend:** Agenten-Modus mit mehreren parallelen Änderungen (Bolt, Tempo),
Planungsmodus vor der Codeänderung (Lovable), Vorher/Nachher-Vergleich und feingranulares Rücknehmen
(v0 Design Mode), Bearbeiten direkt in der Vorschau (Claude Design, Juni 2026).

**Markieren und beschreiben** wird auf zwei Wegen gelöst: Screenshot plus Text (v0) oder
strukturelles Ziel über den Code-Pfad (Onlook). Die **Kombination** ist am robustesten — ein reiner
CSS-Selektor bricht, sobald sich das Markup ändert.

**Marktlücke:** Kein Werkzeug hat Barrierefreiheits-Prüfungen (Kontrast, Tastaturbedienung) nativ im
Bearbeiten-Ablauf. Qualitätssicherung läuft überall über externe Werkzeuge.

**Häufigste Kritik:** unvorhersehbarer Verbrauch (man zahlt für Fehler, die die KI selbst verursacht
hat), Designtreue bricht bei Komplexität ein, beworbene Funktionen arbeiten unzuverlässig
(Tempo-DOM-Inspektor), keine native Ausgabe für mobile Anwendungen.

Quellen: https://v0.app/docs/design-mode · https://github.com/onlook-dev/onlook ·
https://www.subframe.com/design-systems · https://aiuxplayground.com/pattern/checkpoints-and-restore/
