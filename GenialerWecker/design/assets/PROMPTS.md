# Bildmotive der vier Designvorschauen

Erstellt mit dem eingebauten `imagegen`-Werkzeug. Alle vier Motive sind dekorative Rasterbilder; Beschriftungen, Uhrzeiten und Bedienelemente werden separat als HTML dargestellt. Die originalen PNG-Dateien wurden unverändert übernommen.

## A · Nachtatelier — `a-nachtatelier/mond.png`

Use case: stylized-concept. Asset type: decorative illustration for a sophisticated German alarm and sleep app concept called Nachtatelier. Create a premium editorial still life of one elegant warm ivory crescent moon, finely brushed copper inner edge, suspended above a small midnight-blue disc with three tiny brass stars. Sculptural matte ceramic, subtle paper grain, refined museum product photography, intimate nocturnal atmosphere. Square composition; object compact at center with generous dark navy negative space around it, solid deep midnight navy background, no harsh glow. This is a decorative asset placed beside real readable HTML text, not a user interface. No letters, numbers, clocks, weather symbols, clouds, sun, gradients with neon glow, watermarks or logos. High design restraint, soft precise rim lighting, exquisite material detail.

## B · Morgenruhe — `b-morgenruhe/bett.png`

Use case: stylized-concept. Asset type: decorative illustration for a calm modern alarm and sleep app concept called Morgenruhe. A small beautifully crafted layered paper sculpture of an inviting neatly made bed with a cream linen duvet, sage green pillow, tiny terracotta crescent moon hanging above it. Soft organic cut-paper curves, tactile fibers, warm editorial illustration, sophisticated minimal Scandinavian craft aesthetic. Compact centered vignette with generous pale warm cream negative space, square composition. Colors sage, ivory, warm peach and terracotta, gentle directional shadows, modest detail, instantly associated with sleep. No people, no text, no numbers, no UI, no weather symbols, no sun, no clouds, no logos, no watermark. This is an illustrative accent beside functional HTML, not a screenshot.

## C · Orbit — `c-orbit/sternbahn.png`

Use case: stylized-concept. Asset type: abstract decorative sleep/alarm illustration for a precise modern app concept called Orbit. A single beautifully proportioned crescent moon made of frosted pale-blue glass, surrounded by two thin intersecting orbital rings and exactly three tiny star points; graphic museum object against solid charcoal-black background. Crisp scientific instrument quality, elegant restraint, matte dark base and small lime accent on one orbital point. Square composition, centered compact object with generous clean negative space. Distinctive modern industrial design with clean silhouette, no complex planetary scene. No text, numbers, clocks, interface controls, planets, sun, weather/clouds, purple neon, logos or watermark. Not an app mockup, decorative asset only.

## D · Traumraum — `d-traumraum/kissen.png`

Use case: stylized-concept. Asset type: decorative sleep illustration for a warm welcoming modern alarm app concept called Traumraum. A charming but sophisticated sculptural pair: a plush pearl-colored pillow gently supporting a small dusty-rose ceramic crescent moon, two small champagne-colored four-point star objects floating discreetly above. Beautiful contemporary clay-render design, smooth rounded forms, subtle woven pillow texture, warm peach rim light, soft grounded shadows. Deep muted plum solid background. Centered compact arrangement in square composition with generous negative space. Quiet, cozy and premium, not childish. No faces, people, clouds, sun/weather, text, numbers, buttons, UI, logos or watermark. This decorates real HTML and must not contain a rendered interface.


## Orbit – freigestelltes Motiv für den nativen Uhrkopf

Datei: `c-orbit/sternbahn-freigestellt.png`. Bearbeitet mit dem integrierten ImageGen-Werkzeug; Referenz: `c-orbit/sternbahn.png`. Das Original bleibt erhalten.

Exakter Bearbeitungsauftrag:

> Use case: background-extraction. Asset type: transparent decorative PNG for the native Android alarm app's Orbit design. Edit the supplied image: remove the entire black rectangular background and ground/background shading, making all space surrounding and inside the orbital wire loops genuinely transparent (real alpha channel, not painted checkerboard and not black). Preserve the beautiful recognizable icy blue crescent sculpture, metallic orbital wires, small lime sphere, three tiny stars, and its compact dark display base, maintaining their position, proportions and premium 3D materials. Keep natural clean anti-aliased edges and subtle object shading. The result must blend directly onto BOTH pure white/light gray and dark navy-charcoal UI surfaces, so avoid a dark halo or matte rectangle. Center the complete object on a square transparent canvas with narrow safe padding, no cropping. No text, numerals, UI, border, background plate, checkerboard, or new objects. This is the same existing Orbit motif extracted for placement where a round twelve-hour clock used to be.


## Morgenruhe und Traumraum – freigestellte Motive für die native App

Dateien: `app/src/main/res/drawable-nodpi/design_bett_frei.png` und `design_kissen_frei.png`.
Die Originale `b-morgenruhe/bett.png` und `d-traumraum/kissen.png` bleiben hier unverändert erhalten.

Beide Entwurfsbilder hatten einen **deckenden farbigen Hintergrund** (Bett: warmes Creme,
Kissen: gedeckte Pflaume). In der App wurden sie deshalb beschnitten dargestellt — das Bett in
einer gerahmten Kachel, das Kissen als runde Scheibe. Im Dunkelmodus stand dadurch eine helle
Fläche mitten im dunklen Raum, und nach dem Palettenwechsel auf Leinen/Tinte beziehungsweise
warmes Schwarz/Orange hätte der eingebrannte Hintergrund erst recht nicht mehr gepasst.

Bearbeitung: echte Hintergrundentfernung mit Pillow, **keine** Tönung oder Einfärbung.
Flood-Fill von allen vier Rändern mit Farbtoleranz (kein globaler Farbschlüssel, sonst
verschwinden gleichfarbige Stellen im Objekt selbst), weiche Alphakante, und an den
halbtransparenten Kantenpixeln wurde die alte Hintergrundfarbe herausgerechnet, damit auf
dunklen wie hellen Flächen kein Farbsaum stehen bleibt. Objektfarben, Materialien, Schattierung
und Rim-Light sind unverändert — der plastische Eindruck bleibt vollständig erhalten.

Geprüft wurde durch Komposition auf vier Untergründe: `#0E0B09`, `#F7F1EA`, `#F4F1EC`, `#171D21`.
Kein Halo feststellbar; Randpixel voll transparent (Alpha 0), Alpha-Extrema 0..255.

Ergebnis: je 768×768 px mit echtem Alphakanal, 253 KB und 472 KB. Zusammen mit dem Entfernen des
seit der Orbit-Freistellung unreferenzierten `design_sternbahn.png` sank das Debug-APK von
26 MB auf 21 MB.

**Offen:** Das Bettmotiv zeigt Salbeigrün und Terrakotta und stammt damit noch aus der
abgelösten Morgenruhe-Farbwelt. Zur neuen Palette aus Leinen, Tintenblau und Messing passt es
nur teilweise. Eine Neugenerierung braucht ein Bildgenerierungswerkzeug und ist nicht Teil
dieser Bearbeitung.
