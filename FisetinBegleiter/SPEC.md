# Fisetin-Begleiter Design-Update

## 1. Übersicht

Dieses SPEC beschreibt die neuen Bereiche des Redesigns aus
`../Designs/Fisetin-Begleiter-Design-Update/Fisetin Begleiter Prototyp.dc.html`.
Die bestehende App bleibt fachlich maßgeblich; der Entwurf ist für Darstellung,
Themes, Abstände, Typografie, Effekte und Motion die einzige Wahrheit.

Umsetzungsfall: **B - Redesign mit neuen Bereichen**.

Neu gegenüber der bestehenden App sind:

- die Theme-Familien Aurora, Vital und Ember, jeweils hell und dunkel,
- eine persistierte Auswahl der Theme-Familie,
- der im Entwurf gezeigte Hell-/Dunkel-Umschalter,
- der animierte Countdown-Fortschrittsring auf dem Heute-Screen.

Die fünf bestehenden Bereiche Heute, Ablauf, Stack, Verlauf und System sowie alle
vorhandenen Dialoge werden ohne fachliche Erweiterung optisch 1:1 angeglichen.

## 2. Design-Tokens

### 2.1 Effektive Farbpaletten

Die Werte berücksichtigen bereits die CSS-Kaskade aus Basis-, Dark- und
Variantenselektoren. RGBA-Werte bleiben wortwörtlich erhalten; bei Compose wird der
Alpha-Kanal mit `round(alpha * 255)` gebildet.

| Token | Aurora hell | Aurora dunkel | Vital hell | Vital dunkel | Ember hell | Ember dunkel |
|---|---|---|---|---|---|---|
| `bg0` | `#f7f6fe` | `#0f0d20` | `#f2faf6` | `#08130e` | `#fbf6ef` | `#160f08` |
| `bg1` | `#eceffb` | `#191434` | `#e7f3ec` | `#0e211a` | `#f6ece0` | `#231710` |
| `card` | `rgba(255,255,255,.72)` | `rgba(33,28,64,.6)` | `rgba(255,255,255,.72)` | `rgba(16,42,33,.6)` | `rgba(255,255,255,.72)` | `rgba(48,31,20,.58)` |
| `cardBrd` | `rgba(255,255,255,.9)` | `rgba(255,255,255,.09)` | `rgba(255,255,255,.9)` | `rgba(255,255,255,.09)` | `rgba(255,255,255,.9)` | `rgba(255,255,255,.09)` |
| `text` | `#191634` | `#efedfc` | `#132019` | `#e8f6ef` | `#261a10` | `#f9efe7` |
| `sub` | `#615e80` | `#a29fc8` | `#5a6d62` | `#8fab9d` | `#7c6a58` | `#b5a190` |
| `line` | `rgba(25,22,52,.09)` | `rgba(255,255,255,.09)` | `rgba(25,22,52,.09)` | `rgba(255,255,255,.09)` | `rgba(25,22,52,.09)` | `rgba(255,255,255,.09)` |
| `chip` | `rgba(25,22,52,.06)` | `rgba(255,255,255,.07)` | `rgba(25,22,52,.06)` | `rgba(255,255,255,.07)` | `rgba(25,22,52,.06)` | `rgba(255,255,255,.07)` |
| `acc` | `#6a5cff` | `#8f7bff` | `#0c8f74` | `#35d9ae` | `#df5a2c` | `#ff8a56` |
| `acc2` | `#b04df0` | `#c66bff` | `#39b788` | `#7ce3c3` | `#f0a03a` | `#ffc058` |
| `onAcc` | `#ffffff` | `#120e2c` | `#ffffff` | `#042019` | `#ffffff` | `#2b1204` |
| `accSoft` | `rgba(106,92,255,.13)` | `rgba(143,123,255,.18)` | `rgba(12,143,116,.13)` | `rgba(53,217,174,.16)` | `rgba(223,90,44,.12)` | `rgba(255,138,86,.16)` |
| `glow` | `rgba(106,92,255,.32)` | `rgba(143,123,255,.5)` | `rgba(12,143,116,.3)` | `rgba(53,217,174,.42)` | `rgba(223,90,44,.3)` | `rgba(255,138,86,.42)` |
| `ok` | `#0d7a50` | `#43e09c` | `#0d7a50` | `#43e09c` | `#0d7a50` | `#43e09c` |
| `okBg` | `rgba(16,150,96,.12)` | `rgba(67,224,156,.13)` | `rgba(16,150,96,.12)` | `rgba(67,224,156,.13)` | `rgba(16,150,96,.12)` | `rgba(67,224,156,.13)` |
| `warn` | `#8f6400` | `#ffcb57` | `#8f6400` | `#ffcb57` | `#8f6400` | `#ffcb57` |
| `warnBg` | `rgba(242,178,0,.16)` | `rgba(255,203,87,.13)` | `rgba(242,178,0,.16)` | `rgba(255,203,87,.13)` | `rgba(242,178,0,.16)` | `rgba(255,203,87,.13)` |
| `bad` | `#c22f3e` | `#ff6e7e` | `#c22f3e` | `#ff6e7e` | `#c22f3e` | `#ff6e7e` |
| `badBg` | `rgba(226,62,80,.11)` | `rgba(255,110,126,.14)` | `rgba(226,62,80,.11)` | `rgba(255,110,126,.14)` | `rgba(226,62,80,.11)` | `rgba(255,110,126,.14)` |
| `navBg` | `rgba(255,255,255,.78)` | `rgba(24,19,50,.72)` | `rgba(255,255,255,.78)` | `rgba(11,29,23,.72)` | `rgba(255,255,255,.78)` | `rgba(36,23,15,.72)` |

Theme-Schatten:

| Modus | Wert |
|---|---|
| Hell | `0 14px 40px rgba(56,48,140,.10)` |
| Dunkel | `0 18px 48px rgba(0,0,0,.45)` |

### 2.2 Typografie

- Schriftfamilie: `Outfit`, Fallback `system-ui, sans-serif`.
- Eingebundene Gewichte: 300, 400, 500, 600, 700 und 800.
- Exakte Schriftgrößen des App-Entwurfs: 10, 10.5, 11, 11.5, 12, 12.5,
  13, 13.5, 14, 14.5, 15, 16, 17, 18, 19, 26 und 28 px; in Compose 1:1 als sp.
- Verwendete Gewichte: 500, 600, 700 und 800; regulärer Text verwendet 400.
- Zeilenhöhen: 1.4, 1.45, 1.5 und 1.55 relativ zur jeweiligen Schriftgröße.
- Letter-Spacing: -0.5, -0.2, 0.2, 0.3, 0.4, 0.5, 0.6 und 1.6 px.
- Screen-Titel: 26 sp, 700, -0.5 sp.
- Dialogtitel und Hero-Titel: 19 sp, 700.
- Abschnittstitel: 17 sp, 700.
- Standard-Kartentitel: 14 bis 14.5 sp, 700.
- Standard-Body: 12.5 bis 13.5 sp, 400 bis 600, Zeilenhöhe 1.4 bis 1.55.
- Bottom-Navigation: 10 sp, 700, 0.2 sp.

### 2.3 Abstände, Größen und Formen

- App-Inhalt: horizontal 18 dp, oben 6 dp, unten 130 dp.
- App-Header: horizontal 20 dp, oben 14 dp, unten 8 dp; Logo 36 dp,
  Radius 12 dp; Modusschalter 38 dp rund.
- Screen-Abstände: 14 dp, Stack/System 12 dp.
- Hauptkarte Heute: horizontal 20 dp, vertikal 24 dp, Radius 24 dp.
- Standardkarten: Radius 18, 20 oder 22 dp gemäß jeweiligem Screen.
- Hinweise und CTA: Radius 18 dp.
- Bottom-Navigation: links/rechts 14 dp, unten 12 dp, Innenabstand 8 dp,
  Radius 26 dp; Nav-Item Radius 18 dp, vertikal 9/7 dp.
- Pills/Chips: Radius 999 dp beziehungsweise 99 dp.
- Fortschrittsring: 152 x 152 dp, Radius 64 dp, Strich 10 dp,
  Umfang 402 dp, runde Linienenden.
- Hero-Zustandsicon: 96 x 96 dp; Hintergrundring zusätzlich 10 dp.
- Dialoge: 24 dp Radius; Bottom-Sheets oben 28 dp Radius,
  horizontal 20 dp, oben 22 dp, unten 26 dp.
- Primärbuttons: horizontal 20 dp, vertikal 15 bis 16 dp, Radius 18 dp.
- Eingaben: horizontal 14 dp, vertikal 13 dp, Radius 14 dp.
- Checkbox: 24 dp, Radius 8 dp, Rand 2 dp.
- Toggle: 46 x 27 dp, Thumb 22 dp bei 2.5 dp Offset.

### 2.4 Gradienten, Blur, Glow und weitere Schatten

- App-Hintergrund: `linear-gradient(170deg, bg0 0%, bg1 100%)`.
- Akzentflächen: `linear-gradient(135deg, acc, acc2)`.
- Nächster-Schritt-Karte: `linear-gradient(135deg, accSoft, transparent)`.
- In-App-Glow: 300 x 300 dp, oben -90 dp, rechts -80 dp,
  `radial-gradient(circle, glow, transparent 70%)`, Blur 50 dp.
- Hero-Glow: 240 x 140 dp, oben -70 dp, horizontal zentriert,
  radial elliptisch bis transparent bei 70 %, Blur 30 dp.
- Glass-Karten verwenden je nach Komponente Blur 12, 14, 16, 18, 20,
  24 oder 28 dp, plus 1 dp `cardBrd`.
- Logo: `0 6px 18px glow`; CTA: `0 12px 28px glow`;
  Dialog-CTA: `0 10px 24px glow`; aktiver Nav-Eintrag: `0 6px 18px glow`.
- Fortschrittsring: Drop-Shadow `0 0 8px glow`.
- Toast: `0 12px 30px rgba(0,0,0,.3)`.
- Modal-Overlay: `rgba(8,6,20,.45)` mit Blur 8 dp.

## 3. Effekte und Animationen

| Name | Original | Compose-Entsprechung |
|---|---|---|
| `fbIn` | Opacity 0 und Y +14 px nach sichtbar/Y 0; 400 ms; `cubic-bezier(.22,1,.36,1)` | Screenwechsel mit Fade und vertikalem Offset |
| `fbUp` | Opacity 0 und Y +70 px nach sichtbar/Y 0; 400 ms; gleiche Kurve | Bottom-Sheet-Einblendung |
| `fbPop` | Opacity 0 und Scale .92 nach sichtbar/1; 350 ms; gleiche Kurve | zentrierte Dialoge |
| `fbFloat` | Y -10 px nach +14 px; 8 s; ease-in-out; unendlich alternierend | Hintergrund-Glow |
| `fbPulse` | Opacity .55 nach 1 nach .55; 2.4 s; ease-in-out; unendlich | Drink-Zustandsicon |
| Ring | `stroke-dashoffset` 1 s linear | animierter Fortschritt |
| Theme | Hintergrund 500 ms | Palette/Gradient animieren |
| Navigation | alle Eigenschaften 300 ms | Farbe, Hintergrund und Schatten |
| Buttons | Transform 200 ms, Opacity 250 bis 300 ms | Druck-/Aktivierungszustand |
| Toggle | Hintergrund und Thumb-Position 300 ms | Theme-Modusschalter |

Die CSS-Bezierkurve wird als `CubicBezierEasing(0.22f, 1f, 0.36f, 1f)` abgebildet.

## 4. Themes und Umschaltung

`AppThemeVariant` enthält `AURORA`, `VITAL` und `EMBER`. `AppThemeMode` enthält
`LIGHT` und `DARK`. Jede der sechs Kombinationen besitzt eine vollständige
`AppColors`-Palette; Material-3-Rollen werden daraus abgeleitet, ohne die zusätzlichen
Designrollen für Gradient, Glass, Linie, Chip, Glow und Statusfarben zu verlieren.

Die Theme-Familie wird im System-Screen mit drei Pills im Stil der Variantenwahl des
Entwurfs ausgewählt. Der runde Button rechts im App-Header schaltet wie im Entwurf
zwischen hell und dunkel. Beide Werte werden in appweiten `SharedPreferences`
persistiert und beim Start vor dem ersten Frame wiederhergestellt. Unbekannte oder
fehlende Werte fallen defensiv auf `EMBER` und `LIGHT` zurück, den Defaults des Entwurfs.

## 5. Screen-Spezifikation der neuen Bereiche

### 5.1 Theme-Auswahl im System-Screen

- Abschnittslabel und Abstände entsprechen den vorhandenen System-Abschnitten.
- Drei 999-dp-Pills: Aurora, Vital und Ember.
- Jede Pill enthält den 10-dp-Farbpunkt des Entwurfs (`#6a5cff`, `#0c8f74`,
  `#e0562f`) mit 8-dp Glow in derselben Farbe.
- Aktiv: `accSoft`-Hintergrund, `acc`-Text und `acc`-Rand.
- Inaktiv: `chip`-Hintergrund, `sub`-Text und `line`-Rand.
- Innenabstand 9 dp vertikal und 16 dp horizontal, Inhaltsspalt 8 dp,
  14 sp und Gewicht 600, Übergang 250 ms.

### 5.2 Hell-/Dunkel-Umschalter im Header

- 38 x 38 dp, rund, 1-dp-`line`-Rand, `card`-Hintergrund, Glass-Blur 12 dp.
- Mond-Icon im hellen Modus, Sonnen-Icon im dunklen Modus; 18 dp,
  Strichbreite 1.8 dp, Farbe `text`.
- Bei Aktivierung Wechsel auf die andere feste Helligkeit; kein zusätzlicher
  Systemmodus, da der Entwurf ausschließlich Hell und Dunkel vorgibt.

### 5.3 Countdown-Fortschrittsring

- Sichtbar nur bei aktivem Sperrfenster.
- Grundkreis `chip`, Fortschritt als 135-Grad-Verlauf `acc` nach `acc2`.
- Fortschritt bildet `verstrichene Zeit / Sperrfensterdauer` ab und wird auf 0 bis 1 begrenzt.
- Innen: Countdown 26 sp/700/-0.5 sp; `VERBLEIBEND` 10 sp/400/1.6 sp;
  Freigabezeit 12 sp/700 in `acc`.
- Dash-Offset animiert linear über 1 Sekunde; verbleibende Zeit aktualisiert sich sekündlich.

## 6. Funktionen und Verhalten

- Theme-Auswahl wird sofort auf alle Screens, Dialoge, Navigation und Systemleisten angewandt.
- Theme-Familie und Modus überleben Prozessneustart und Geräte-Neustart.
- Persistenzfehler dürfen den App-Start nicht blockieren; Default ist Ember hell.
- Der Fortschrittsring verwendet ausschließlich den vorhandenen Kur-/Countdown-State
  und verändert keine Kur-, Alarm- oder Abschlusslogik.
- Bestehende Room-Daten, Alarme, Notizen, Zutaten und Kurzustände bleiben unverändert.

## 7. Navigation

Die bestehende lokale Navigation bleibt erhalten und wird visuell auf die fünf
Entwurfsziele abgebildet:

| Route | Label |
|---|---|
| `today` | Heute |
| `timeline` | Ablauf |
| `stack` | Stack |
| `history` | Verlauf |
| `more` | System |

Die Theme-Auswahl ist Teil des System-Screens. Der Modusschalter bleibt auf jedem
Hauptscreen im App-Header erreichbar.

## 8. Datei-Mapping

| Designbereich | Geplante Datei(en) |
|---|---|
| Paletten, Typography, Shapes, CompositionLocals | `ui/theme/Theme.kt` sowie kleine thematische Ergänzungsdateien bei Bedarf |
| Theme-Persistenz und appweiter State | `MainActivity.kt` |
| Gradient, Glow, Header, Bottom-Navigation, Modusschalter | `ui/FisetinApp.kt` |
| Wiederverwendbare Glass-/Status-Komponenten | `ui/UiComponents.kt` |
| Heute, Ring, Setup und Drink | `ui/TodayAndTimelineScreens.kt` |
| Stack und Verlauf | `ui/StackHistoryScreens.kt` |
| System und Theme-Familienwahl | `ui/MoreScreen.kt` |
| Sichtbare Version | `app/build.gradle.kts` |

## 9. Offene Fragen

Keine. Der Entwurf und die bestehende Fachlogik bestimmen alle für die Umsetzung
erforderlichen Zustände. Die Schrift wird als Android-Google-Font `Outfit` mit einem
System-Fallback eingebunden; dadurch bleibt die App bei fehlendem Font-Provider nutzbar.
