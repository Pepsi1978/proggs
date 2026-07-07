# Home Screen Design-Recherche: Tagebuch-App

> Erstellt am 2026-04-13 durch den Designer-Skill.
> Basierend auf Analyse von 12 Top-Journal-Apps, Dribbble/Behance-Trends,
> Material Design 3, UX-Studien und aktuellen Animation-Libraries.

---

## 1. Was MUSS auf den Home Screen?

### Pflicht-Elemente (in dieser Reihenfolge, top-down)

| # | Element | Warum | Beispiel-Apps |
|---|---------|-------|---------------|
| 1 | **Personalisierte Begruessung** | +20% Engagement laut Duolingo-Studie. "Guten Morgen, Max" statt nur "Guten Morgen" | Five Minute Journal, Reflectly, Stoic |
| 2 | **Datum + Tageszeit** | Gibt dem Eintrag sofort Kontext, senkt die Hemmschwelle zum Schreiben | Alle Top-Apps |
| 3 | **Mood-Check-In** | Schnellster Einstieg (1 Klick), macht die App sofort interaktiv | Daylio, Reflectly, Stoic |
| 4 | **Hero-Element** | EIN starkes visuelles Element als Blickfang (Zitat ODER Illustration ODER Stimmungsring) | Reflectly, Five Minute Journal |
| 5 | **Neuer-Eintrag-Button (CTA)** | Primaere Aktion der App, muss sofort erreichbar sein | Alle Apps |
| 6 | **Letzte Eintraege (Timeline)** | Zeigt dem Nutzer seinen Fortschritt, laedt zum Weiterlesen ein | Day One, Journey, Momento |
| 7 | **Sanfte Statistik** | "Diese Woche 4x geschrieben" motiviert ohne Druck | Daylio, Stoic, Gratitude |

### Optionale Elemente (Nice-to-have)

| Element | Wann sinnvoll |
|---------|--------------|
| Quick Actions (Text/Foto/Audio/Stimmung) | Wenn die App mehrere Eintragstypen unterstuetzt |
| "Heute vor einem Jahr" Rueckblick | Ab 1 Jahr Nutzungsdauer — starkes Retention-Feature |
| Tages-Schreibprompt / Frage des Tages | Gegen "Writer's Block", besonders fuer neue Nutzer |
| Wetter-Integration | Automatischer Kontext ohne Nutzer-Aufwand |
| Suchleiste | Als Icon oben rechts — da wenn noetig, nicht dominant |

### Was NICHT auf den Home Screen gehoert

- Mehr als 5 Informationstypen gleichzeitig (Hick's Law: mehr Auswahl = laengere Entscheidung)
- Komplexe Statistik-Dashboards (gehoeren in eigenen Tab)
- Einstellungen oder Account-Infos (gehoeren in Profil/Settings)
- Werbung oder Upselling (zerstoert die intime Atmosphaere)

---

## 2. Aufbau des Home Screens (Top-Down-Struktur)

```
+--------------------------------------------------+
|                                                  |
|  "Guten Abend, Frank"           [Avatar] [Suche] |
|  Sonntag, 13. April 2026                         |
|                                                  |
+--------------------------------------------------+
|                                                  |
|  [Animierter Gradient-Hintergrund]               |
|                                                  |
|     "Wie fuehlst du dich gerade?"                |
|                                                  |
|  [Super] [Gut] [Okay] [Meh] [Schlecht]           |
|   Mood-Chips mit Icons + Farben                  |
|                                                  |
+--------------------------------------------------+
|                                                  |
|  +--------------------------------------------+  |
|  | Hero-Card (Glassmorphism)                   |  |
|  |                                             |  |
|  |  "Das Leben besteht nicht aus den           |  |
|  |   Momenten, in denen wir atmen,             |  |
|  |   sondern aus denen, die uns den            |  |
|  |   Atem rauben."                             |  |
|  |                                             |  |
|  |  Playfair Display, Serif, 20sp              |  |
|  +--------------------------------------------+  |
|                                                  |
+--------------------------------------------------+
|                                                  |
|  Diese Woche: 4 Eintraege    Streak: 7 Tage     |
|  [####____] 4/7              [Flamme-Icon]       |
|                                                  |
+--------------------------------------------------+
|                                                  |
|  Deine letzten Eintraege                         |
|                                                  |
|  +--------------------------------------------+  |
|  | [Foto] Heute, 14:30                         |  |
|  |        "Der Nachmittag im Park war..."      |  |
|  |        Stimmung: Gut  |  3 Fotos            |  |
|  +--------------------------------------------+  |
|                                                  |
|  +--------------------------------------------+  |
|  | [Foto] Gestern, 22:15                       |  |
|  |        "Endlich das Buch fertig..."          |  |
|  |        Stimmung: Super  |  Audionotiz       |  |
|  +--------------------------------------------+  |
|                                                  |
|  +--------------------------------------------+  |
|  | [Foto] 11. April, 08:00                     |  |
|  |        "Fruehstueck mit der Familie..."      |  |
|  |        Stimmung: Gut  |  2 Fotos            |  |
|  +--------------------------------------------+  |
|                                                  |
+--------------------------------------------------+
|                                                  |
|  [Home]  [Kalender]  [+FAB]  [Stats]  [Profil]  |
|                                                  |
+--------------------------------------------------+
```

---

## 3. Farbpaletten (3 Optionen)

### Option A: "Warm Sunset" (empfohlen fuer Journal)

| Rolle | Light | Dark | Verwendung |
|-------|-------|------|------------|
| Background | #FAFAF7 (Cream) | #1A1208 (Warm Dark Brown) | Haupt-Hintergrund |
| Surface | #FFFFFF | #2D2418 (Dark Amber) | Cards, Sheets |
| Primary | #C4704F (Terracotta) | #E8A882 (Soft Peach) | Buttons, Links, Akzente |
| On Primary | #FFFFFF | #3B1A08 | Text auf Primary |
| Secondary | #87A878 (Sage Green) | #A8C898 (Light Sage) | Sekundaere Aktionen |
| Accent | #C9B8E8 (Lavendel) | #D4C8F0 | Highlights, Badges |
| On Background | #1C1B1F | #E8E0D0 (Off-White) | Haupttext |
| On Surface | #49454F | #CAC4B8 | Sekundaertext |
| Outline | #79747E | #938F88 | Rahmen, Divider |
| Error | #B3261E | #F2B8B5 | Fehlermeldungen |

**Tageszeit-Gradient fuer Header:**
- Morgen (5-11h): `#F5E6C8` -> `#F2C4CE` (Butter Yellow -> Blush Pink)
- Mittag (11-17h): `#E8F0F8` -> `#B8E0D4` (Light Blue -> Mint)
- Abend (17-22h): `#C9B8E8` -> `#F2C4CE` (Lavendel -> Blush)
- Nacht (22-5h): `#0D0D2B` -> `#2D1B2E` (Deep Indigo -> Aubergine)

### Option B: "Ocean Calm"

| Rolle | Light | Dark |
|-------|-------|------|
| Background | #F0F4F8 | #0A1628 |
| Surface | #FFFFFF | #1A2A3E |
| Primary | #2279A9 (Teal) | #5CB8D8 |
| Secondary | #0D924D (Forest Green) | #4DC88D |
| Accent | #F5C542 (Gold) | #FFD966 |

### Option C: "Purple Dream"

| Rolle | Light | Dark |
|-------|-------|------|
| Background | #FAF8FF | #0D0D2B |
| Surface | #FFFFFF | #1E1B3A |
| Primary | #6750A4 (MD3 Purple) | #D0BCFF |
| Secondary | #625B71 | #CCC2DC |
| Accent | #7D5260 (Pink) | #FFB4AB |

---

## 4. Typografie

### Empfohlene Font-Kombination

| Rolle | Font | Gewicht | Groesse | Zeilenhoehe | Verwendung |
|-------|------|---------|---------|-------------|------------|
| Hero / Zitate | **Playfair Display** | Bold (700) | 22-28sp | 32-36sp | Tageszitat, Begruessung bei leerem Zustand |
| Screen-Titel | **Playfair Display** | SemiBold (600) | 20sp | 28sp | "Deine letzten Eintraege" |
| Begruessung | **Inter** | Medium (500) | 18sp | 26sp | "Guten Abend, Frank" |
| Card-Titel | **Inter** | SemiBold (600) | 16sp | 22sp | Eintragstitel auf Cards |
| Body Text | **Inter** | Regular (400) | 14sp | 20sp | Eintragvorschau, Beschreibungen |
| Label / Meta | **Inter** | Medium (500) | 12sp | 16sp | Datum, Stimmung, Tags |
| Statistik-Zahl | **Inter** | Bold (700) | 24sp | 32sp | "7" bei Streak, "4/7" bei Wochenfortschritt |

**Warum Playfair Display + Inter:**
- Playfair Display gibt Waerme und Persoenlichkeit (Tagebuch-Feeling, handgeschrieben-nah)
- Inter ist die lesbarste Sans-Serif fuer Mobile (Google empfohlen)
- Der Kontrast Serif (emotional) + Sans-Serif (klar) ist der dominierende Trend bei Journal-Apps 2024-2026

### Alternative: Merriweather + Nunito
- Waermer, klassischer, weniger modern
- Gut fuer aeltere Zielgruppe

---

## 5. Animationen & Special Effects

### 5.1 Beim App-Start (Splash -> Home)

**Stagger-Animation der Home-Elemente:**
Jedes Element blendet nacheinander ein (80ms Versatz pro Element):

```kotlin
// Konzept:
LaunchedEffect(index) {
    delay(index * 80L)
    visible = true
}
AnimatedVisibility(
    visible = visible,
    enter = fadeIn(tween(300)) + slideInVertically(
        initialOffsetY = { it / 3 },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    )
)
```

Reihenfolge: Begruessung (0ms) -> Mood-Chips (80ms) -> Hero-Card (160ms) -> Statistik (240ms) -> Timeline-Cards (320ms+)

### 5.2 Animierter Gradient-Hintergrund (Tageszeit)

Der Header-Hintergrund wechselt sanft die Farben basierend auf Tageszeit:

```kotlin
// Konzept:
val infiniteTransition = rememberInfiniteTransition()
val color1 by infiniteTransition.animateColor(
    initialValue = morningColor1,
    targetValue = morningColor2,
    animationSpec = infiniteRepeatable(
        tween(4000, easing = FastOutSlowInEasing),
        RepeatMode.Reverse
    )
)
Box(Modifier.background(Brush.verticalGradient(listOf(color1, color2))))
```

**Tageszeit-Erkennung:**
```kotlin
val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
val (color1, color2) = when (hour) {
    in 5..10  -> Color(0xFFF5E6C8) to Color(0xFFF2C4CE)  // Morgen
    in 11..16 -> Color(0xFFE8F0F8) to Color(0xFFB8E0D4)  // Mittag
    in 17..21 -> Color(0xFFC9B8E8) to Color(0xFFF2C4CE)  // Abend
    else      -> Color(0xFF0D0D2B) to Color(0xFF2D1B2E)   // Nacht
}
```

### 5.3 Glassmorphism-Cards (Hero-Card + Eintragskarten)

**Library:** `haze` von Chris Banes (beste Loesung, rueckwaertskompatibel bis API 21)

```kotlin
// Konzept:
val hazeState = rememberHazeState()
Box {
    // Hintergrund (Gradient)
    GradientBackground(Modifier.hazeSource(hazeState))

    // Glassmorphism-Card
    Card(
        modifier = Modifier
            .hazeEffect(hazeState, HazeMaterials.ultraThin())
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.15f)
        )
    ) {
        // Card-Inhalt
    }
}
```

### 5.4 Mood-Chip-Auswahl-Animation

Beim Tippen auf einen Mood-Chip:
1. **Scale-Bounce:** Chip skaliert kurz auf 1.15x und federt zurueck
2. **Farb-Uebergang:** Hintergrund faerbt sich in Stimmungsfarbe
3. **Haptic Feedback:** `HapticFeedbackType.LongPress`
4. **Confetti** (bei positiver Stimmung): ConfettiKit-Explosion

```kotlin
// Konzept:
val scale by animateFloatAsState(
    targetValue = if (selected) 1.0f else if (pressed) 0.92f else 1.0f,
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
)
```

**Stimmungsfarben:**

| Stimmung | Farbe | Icon |
|----------|-------|------|
| Super | #4CAF50 (Gruen) | Strahlendes Gesicht |
| Gut | #8BC34A (Hellgruen) | Laechelndes Gesicht |
| Okay | #FFC107 (Amber) | Neutrales Gesicht |
| Meh | #FF9800 (Orange) | Leicht trauriges Gesicht |
| Schlecht | #F44336 (Rot) | Trauriges Gesicht |

### 5.5 Parallax-Scrolling (Header)

Der Header-Gradient scrollt langsamer als der Content:

```kotlin
// Konzept:
val scrollState = rememberScrollState()
Box {
    // Header scrollt mit halber Geschwindigkeit
    HeaderGradient(
        Modifier.graphicsLayer {
            translationY = scrollState.value * 0.3f
        }
    )

    // Content scrollt normal
    Column(Modifier.verticalScroll(scrollState)) {
        Spacer(Modifier.height(headerHeight))
        // Timeline-Cards...
    }
}
```

### 5.6 Kollabierende TopAppBar

Beim Scrollen schrumpft der Header elegant:

- Oben: Grosser Header mit Begruessung + Gradient + Mood-Chips (200dp)
- Nach Scrollen: Kompakter Header mit nur Name + Datum (56dp)
- Material3 nativ: `TopAppBarDefaults.enterAlwaysScrollBehavior()`

### 5.7 Eintragskarte -> Detailansicht (Shared Element Transition)

```kotlin
// Compose 1.7+ nativ:
SharedTransitionLayout {
    AnimatedContent(targetState = showDetail) { isDetail ->
        if (!isDetail) {
            EntryCard(
                modifier = Modifier.sharedElement(
                    rememberSharedContentState(key = "entry_${entry.id}"),
                    animatedVisibilityScope = this
                )
            )
        } else {
            EntryDetail(
                modifier = Modifier.sharedElement(
                    rememberSharedContentState(key = "entry_${entry.id}"),
                    animatedVisibilityScope = this
                )
            )
        }
    }
}
```

### 5.8 Streak-Meilenstein-Feier

Bei 7, 14, 30, 60, 100 Tagen Streak:

1. **Confetti-Explosion:** ConfettiKit mit goldenen + bunten Partikeln
2. **Lottie-Animation:** Pokal oder Sterne die aufsteigen
3. **Haptic Heavy:** Starkes haptisches Feedback
4. **Badge-Einblendung:** Neues Achievement erscheint mit Scale-Bounce

### 5.9 Shimmer/Skeleton Loading

Waehrend die Eintraege aus der Datenbank laden:

```kotlin
// Library: compose-shimmer
Box(Modifier.shimmer()) {
    Column {
        Box(Modifier.fillMaxWidth(0.6f).height(20.dp)
            .background(Color.LightGray, RoundedCornerShape(4.dp)))
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(14.dp)
            .background(Color.LightGray, RoundedCornerShape(4.dp)))
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth(0.8f).height(14.dp)
            .background(Color.LightGray, RoundedCornerShape(4.dp)))
    }
}
```

### 5.10 Pull-to-Refresh (thematisch)

Statt generischem Spinner eine thematische Animation:
- Ein Buch das sich oeffnet (Lottie)
- Oder eine Pflanze die waechst
- Laedt: Neues Tageszitat + "Heute vor einem Jahr" Rueckblick

---

## 6. Leerer Zustand (Empty State)

Wenn noch keine Eintraege existieren — DER wichtigste Screen fuer neue Nutzer
(bis zu 40% Abbruchrate ohne guten Empty State laut Toptal-Studie):

```
+--------------------------------------------------+
|                                                  |
|  "Guten Morgen!"                                 |
|  Sonntag, 13. April 2026                         |
|                                                  |
+--------------------------------------------------+
|                                                  |
|        [Lottie: Stift schreibt in Buch]          |
|        (sanfte Loop-Animation)                    |
|                                                  |
|  "Dein Tagebuch wartet auf dich"                 |
|  (Playfair Display, 24sp, Bold)                  |
|                                                  |
|  Halte deine Gedanken, Erinnerungen              |
|  und Gefuehle fest. Jeden Tag ein                 |
|  kleines Stueck von dir.                          |
|  (Inter, 14sp, Regular)                           |
|                                                  |
|  +--------------------------------------------+  |
|  |   Ersten Eintrag schreiben                 |  |
|  |   (Extended FAB, Terracotta, 48dp hoch)    |  |
|  +--------------------------------------------+  |
|                                                  |
|  Oder starte mit einer Frage:                    |
|                                                  |
|  [Wofuer bin ich heute dankbar?]                 |
|  [Was war das Highlight meines Tages?]           |
|  [Was beschaeftigt mich gerade?]                 |
|                                                  |
+--------------------------------------------------+
```

**Regeln:**
- Positiv formulieren: "Dein Tagebuch wartet" statt "Noch keine Eintraege"
- Illustration + Headline + Body + CTA (alle 4 Pflicht)
- Starter-Prompts senken die Hemmschwelle enorm
- Lottie-Animation macht den Screen lebendig statt leer

---

## 7. Gamification (sanft, ohne Strafmechanik)

### Was die Forschung sagt

Streak-Mechaniken erzeugen "Streak Anxiety" (Loss Aversion, Kahneman & Tversky).
Fuer eine Wohlbefinden-App: **Sanfte Gamification** ohne Bestrafung.
Eine PMC-Studie (PMC7467300) bestaetigt: Gamifizierte Mental-Health-Apps
reduzieren Angst — WENN wohlwollend gestaltet.

### Empfohlenes Modell

| Element | Umsetzung | Warum |
|---------|-----------|-------|
| **Wochen-Fortschritt** | "Diese Woche: 4 von 7 Tagen" als Fortschrittsbalken | Zeigt Erfolg ohne Strafe fuer fehlende Tage |
| **Meilenstein-Badges** | Bei 10, 25, 50, 100, 250, 500 Eintraegen | Kumulative Leistung, nichts geht verloren |
| **Streak (optional)** | Flammen-Icon mit Tageszahl, ABER mit "Streak Freeze" | Earn-Back-Mechanismus wie Duolingo |
| **Monatsrueckblick** | "Im Maerz hast du 18x geschrieben" | Positiver Rueckblick statt Druck |
| **"Heute vor einem Jahr"** | Zufaelliger alter Eintrag | Staerkstes Retention-Feature |

### Was NICHT

- Keine taegliche Verpflichtung die bestraft wird
- Kein Leaderboard oder Vergleich mit anderen
- Kein "Du hast gestern nicht geschrieben!" Push

---

## 8. Dark Mode: "Warm Dark" statt "Cold Dark"

### Grundprinzip

Ein Journal-Dark-Mode soll sich anfuehlen wie ein gemaetlicher Lesesaal bei Kerzenlicht,
NICHT wie ein Terminal. Warme Dunkeltoene statt reines Schwarz.

### Konkrete Werte

| Element | Light | Dark |
|---------|-------|------|
| Background | #FAFAF7 | #1A1208 (Warm Dark Brown) |
| Surface (Cards) | #FFFFFF | #2D2418 (Dark Amber) |
| Text (Primary) | #1C1B1F | #E8E0D0 (Off-White, NICHT #FFFFFF) |
| Text (Secondary) | #49454F | #CAC4B8 |
| Divider | #E0E0E0 | #3D3528 |

**Warum Off-White (#E8E0D0) statt reines Weiss (#FFFFFF)?**
Reines Weiss auf Schwarz erzeugt "Halation" (Lichthof-Effekt) und Augenbelastung.
Off-White reduziert Augenbelastung um bis zu 40% (Material Design Guidelines).

**Auto-Dimm nach 22 Uhr (Optional):**
Unabhaengig von System-Setting kann die App abends automatisch in ein besonders
warmes, blaulicht-armes Theme wechseln — ideal fuer Abend-Journaling.

---

## 9. Abstands-System (4dp Raster)

| Token | Wert | Verwendung |
|-------|------|------------|
| `xs` | 4dp | Minimaler Abstand zwischen verwandten Elementen |
| `sm` | 8dp | Standard-Abstand innerhalb von Komponenten |
| `md` | 12dp | Abstand zwischen Komponenten in einer Gruppe |
| `lg` | 16dp | Standard-Seitenabstand (horizontal padding) |
| `xl` | 24dp | Abstand zwischen Sektionen |
| `2xl` | 32dp | Grosser Sektions-Abstand |
| `3xl` | 48dp | Screen-Level-Abstand, Hero-Bereiche |

### Eckenradien

| Komponente | Radius |
|-----------|--------|
| Mood-Chips | 999dp (Pill) |
| Entry-Cards | 20dp |
| Hero-Card (Glassmorphism) | 24dp |
| Buttons | 12dp |
| Bottom Sheet | 28dp (oben) |
| FAB | 16dp (oder 28dp fuer Large FAB) |
| Avatar | 50% (Kreis) |

---

## 10. Navigation & FAB-Platzierung

### Bottom Navigation (5 Tabs)

| Tab | Icon | Label |
|-----|------|-------|
| Home | Home (Filled wenn aktiv) | Home |
| Kalender | CalendarMonth | Kalender |
| **Neuer Eintrag** | **Add (im FAB)** | — |
| Statistik | BarChart | Statistik |
| Profil | Person | Profil |

**Der FAB sitzt IN der Bottom Navigation (Mitte):**
- Groesser als die anderen Icons (56dp vs 24dp)
- Primaerfarbe (Terracotta)
- Leicht erhoeht (Elevation 6dp)
- Beim Tippen: Speed Dial mit 3-4 Quick Actions (Text, Foto, Audio, Stimmung)

### Thumb-Zone-Optimierung

Der untere Bildschirmbereich ist fuer Rechthaender am bequemsten erreichbar
(Steven Hoober Studie, 84% der Nutzer). Deshalb:
- FAB + Bottom Nav unten
- Mood-Chips in der Mitte des Screens (gut erreichbar)
- Begruessung oben (wird gelesen, nicht getippt)

---

## 11. Benoetigte Libraries

| Library | Version | Zweck |
|---------|---------|-------|
| `com.airbnb.android:lottie-compose` | 6.x | Lottie-Animationen (Empty State, Streak, Loading) |
| `dev.chrisbanes.haze:haze` | latest | Glassmorphism / Frosted Glass Effekt |
| `io.github.vinceglb:confettikit-compose` | 0.8+ | Confetti bei Streak-Meilensteinen |
| `com.valentinilk.shimmer:compose-shimmer` | 1.3.3+ | Skeleton Loading |
| `io.github.om252345:composemeshgradient` | 0.1.0 | Mesh-Gradient (Spotify-Stil, optional) |
| Google Fonts: Playfair Display | — | Serif-Headline-Font |
| Google Fonts: Inter | — | Sans-Serif-Body-Font |

---

## 12. Vergleich: Top-Apps und was wir uebernehmen

| Feature | Day One | Daylio | Reflectly | Stoic | **Unsere App** |
|---------|---------|--------|-----------|-------|----------------|
| Begruessung | Nein | Nein | Ja | Ja | **Ja, personalisiert + tageszeit** |
| Mood-Check | Nein | Ja (zentral) | Ja (Slider) | Ja | **Ja, Chip-Leiste** |
| Hero-Element | Nein | Nein | Zitat-Card | Tages-Prompt | **Glassmorphism Zitat-Card** |
| Streak | Memories | Flamme+Badges | Implizit | Badges | **Sanft, Wochen-Fortschritt** |
| Timeline | Cards | Mood-Bubbles | Feed | Trends | **Cards mit Foto + Mood** |
| Animationen | Minimal | Minimal | Hochwertig | Minimal | **Stagger + Parallax + Glass** |
| Dark Mode | Ja | Ja | Ja | Ja (Standard) | **Warm Dark mit Auto-Dimm** |
| Schriften | Sans-Serif | Sans-Serif | Sans-Serif | Sans-Serif | **Serif + Sans-Serif Mix** |
| FAB | Oben rechts | Unten Mitte | Unten Mitte | Kein FAB | **Unten Mitte in BottomNav** |

---

## 13. Top-12 Journal-Apps: Detailanalyse

### Day One
- Timeline + Cards als Standard, alternativ Kalender/Karte/Medien
- Bleistift-Icon oben rechts (kein FAB)
- AI-Prompts und "On This Day" Erinnerungen
- Dezent, viel Weissraum, frei waehlbare Journal-Akzentfarbe

### Journey
- Material Design 3 Basis, 14 waehlbare Farbthemen
- FAB unten rechts (klassisches Material Pattern)
- Mood-Tracking ueber 30 Tage visualisiert
- Cards mit Foto-Thumbnail, Datum, Stimmungs-Icon

### Daylio
- Mood-Picker mit 5 Stimmungen als zentrale Interaktion
- "Year in Pixels": Jeder Tag als farbiger Pixel — mosaikartiges Jahresbild
- Tages-Streak mit Flammen-Icon + Achievements-System
- Grosse, runde Icons — "cute/friendly" Aesthetik

### Reflectly
- Card-basiertes Dashboard mit AI-generierten Prompts
- "Magic Color Change": gesamte App-Farbe wechselt mit Theme
- Weiches Gradient-Design, sanfte Pastell-Toene
- Hochwertige Animationen (Flutter-basiert)

### Stoic
- Anpassbares Dashboard mit Favoriten-Uebungen
- Dunkel-Palette dominant (Tiefblau/Schwarz mit hellen Akzenten)
- Streak mit Badges, woechtentliche Themes
- CBT-basierte Fragen fuer Emotionsverstaendnis

### Five Minute Journal
- Morgens 3 Prompts, Abends 2 Prompts — maximale Fokussierung
- Warme, positive Farben (Gold, Creme, Warmweiss)
- Tageszitat prominent auf Home Screen
- Kein klassischer Home Screen — direkt Einstieg in den Tageseintrag

### Grid Diary
- Signatur: 9-Felder-Grid (Mandala-inspiriert) pro Tageseintrag
- Fortschrittsanzeige: Wie viele Grid-Felder heute ausgefuellt?
- Eingebaute Frage-Bibliothek (anpassbar)
- Sauber, minimal, pastelfarben

### Gratitude Journal
- Entry-Cards im Feed mit Foto-Thumbnails
- 7 Tag-Farben (Rose, Amber, Gold, Sage, Ocean, Lavender, Berry)
- Woechentlicher Recap mit AI-Insights
- Daily Quote Widget fuer den Homescreen

### Momento
- Unified Timeline: Social Media + Fotos + manuelle Eintraege
- Automatischer Import senkt Huerden erheblich
- "On This Day" Navigation fuer Jahresrueckblick

### Monnday (Design-Konzept, Behance)
- 8 "Emotional Monsters" als Charakter-Avatare
- Primary Blues + Orange/Gruen-Akzente
- Gamification durch Charakter-Begleiter macht Emotionen greifbar

---

## 14. Design-Inspirationen (Referenzen)

| Quelle | Was daraus uebernehmen |
|--------|----------------------|
| **Freud v2** (Dribbble, 18.4k Views) | Dunkles UI mit warmen Akzenten, Glassmorphism-Cards |
| **Monnday** (Behance, 649 Appreciations) | Emotionale Charakter-Avatare, Blues + Orange |
| **AI Journal & Diary** (Behance, 916 Appreciations) | AI-Prompts als zentrales Feature |
| **Reflectly** | Sanfte Gradient-Uebergaenge, "Magic Color Change" |
| **Five Minute Journal** | Fokussierter CTA, Tageszitat, Serif-Aesthetik |
| **Daylio** | Mood-Chip-System, "Year in Pixels", Badge-System |
| **Headspace / Calm** | Warme, beruhigende Farbpalette, Illustrations-Stil |
| **Spotify** | Mesh-Gradient-Hintergrund, Micro-Interactions |

---

## 15. Accessibility-Checkliste

| Pruefpunkt | Standard | Unsere Umsetzung |
|-----------|----------|------------------|
| Touch-Target-Groesse | Min. 48x48dp | Alle Buttons, Chips, Icons >= 48dp |
| Text-Kontrast (normal) | Min. 4.5:1 (WCAG AA) | Berechnet fuer alle Farbkombinationen |
| Text-Kontrast (gross) | Min. 3:1 | Headlines >= 18sp geprueft |
| Farbenblindheit | Nie nur Farbe als Info | Stimmungen haben Icon + Farbe + Label |
| Screen-Reader | Logische Reihenfolge | Begruessung -> Mood -> Hero -> Stats -> Timeline -> Nav |
| Schrift-Skalierung | Alle Texte in sp | Kein dp fuer Text |
| Dark Mode | Kein reines Schwarz | Off-White Text, warme Dunkeltoene |

---

## 16. Quellen

### Journal-App-Analyse
- Day One: dayoneapp.com/features, /blog/new-navigation-layout-2024-11
- Daylio: Wikipedia, App Store Screenshots
- Reflectly: screensdesign.com, developer.android.com/stories/apps/reflectly
- Stoic: getstoic.com/features, screensdesign.com
- Five Minute Journal: screensdesign.com, App Store
- Journey, Grid Diary, Penzu, Momento, Bear, Gratitude: diverse Reviews

### UX-Studien
- Steven Hoober: "How Do Users Really Hold Mobile Devices" (Thumb Zone)
- Kahneman & Tversky: Loss Aversion (Streak Anxiety)
- PMC7467300: Gamification in Mental Health Apps
- Toptal: Empty State UX (40% Abbruchrate ohne guten Empty State)
- Duolingo: +20% DAU durch personalisierte Home-Screens
- WCAG 2.1 AA / 2.2: Kontrast- und Touch-Target-Standards

### Design-Trends
- Dribbble: journal-app, diary-app, wellness-app Tags
- Behance: Monnday (649 Appr.), AI Journal (916 Appr.), Freud v2 (18.4k Views)
- Fuzzy Math: "Color Palettes of Mental Healthcare UI"

### Animation-Libraries
- Lottie: lottiefiles.com/free-animations/diary
- Haze: github.com/chrisbanes/haze
- ConfettiKit: github.com/vinceglb/ConfettiKit
- compose-shimmer: github.com/valentinilk/compose-shimmer
- Compose Mesh Gradient: composemeshgradient Library
- Android Developers: Parallax Scrolling, Shared Element Transitions
