# Material Design 3 — Referenz-Spezifikationen

Diese Referenz enthaelt die exakten Spezifikationen aus Material Design 3,
die fuer das Design-Audit benoetigt werden. Alle Werte sind aus der offiziellen
Material Design Dokumentation (material.io/design).

---

## Color System

### Color Roles (Baseline — Purple Theme)

| Rolle | Light | Dark | Zweck |
|-------|-------|------|-------|
| Primary | #6750A4 | #D0BCFF | Hauptfarbe fuer Buttons, FAB, aktive Icons |
| On Primary | #FFFFFF | #381E72 | Text/Icons auf Primary-Flaechen |
| Primary Container | #EADDFF | #4F378B | Chips, Toggle-Buttons, ausgewaehlte Items |
| On Primary Container | #21005D | #EADDFF | Text auf Primary Container |
| Secondary | #625B71 | #CCC2DC | Weniger prominente Elemente |
| On Secondary | #FFFFFF | #332D41 | Text auf Secondary |
| Secondary Container | #E8DEF8 | #4A4458 | Tonal-Buttons, Filter-Chips |
| On Secondary Container | #1D192B | #E8DEF8 | Text auf Secondary Container |
| Tertiary | #7D5260 | #EFB8C8 | Akzente, Kontrast zu Primary |
| On Tertiary | #FFFFFF | #492532 | Text auf Tertiary |
| Tertiary Container | #FFD8E4 | #633B48 | Akzent-Container |
| On Tertiary Container | #31111D | #FFD8E4 | Text auf Tertiary Container |
| Error | #B3261E | #F2B8B5 | Fehlermeldungen |
| On Error | #FFFFFF | #601410 | Text auf Error |
| Error Container | #F9DEDC | #8C1D18 | Fehler-Hintergrund |
| On Error Container | #410E0B | #F9DEDC | Text auf Error Container |
| Surface | #FEF7FF | #141218 | Haupt-Hintergrund |
| On Surface | #1D1B20 | #E6E0E9 | Haupttext |
| Surface Variant | #E7E0EC | #49454F | Sekundaerer Hintergrund |
| On Surface Variant | #49454F | #CAC4D0 | Sekundaerer Text |
| Outline | #79747E | #938F99 | Rahmen, Divider |
| Outline Variant | #CAC4D0 | #49454F | Dezente Rahmen |
| Surface Container Lowest | #FFFFFF | #0F0D13 | Niedrigste Elevation |
| Surface Container Low | #F7F2FA | #1D1B20 | Niedrige Elevation |
| Surface Container | #F3EDF7 | #211F26 | Standard-Container |
| Surface Container High | #ECE6F0 | #2B2930 | Hohe Elevation |
| Surface Container Highest | #E6E0E9 | #36343B | Hoechste Elevation |

### Dynamic Color (Material You)

- Android 12+ generiert Farben automatisch aus dem Wallpaper
- `dynamicLightColorScheme()` / `dynamicDarkColorScheme()` in Compose
- Fallback auf statische Farben bei aelteren Android-Versionen
- IMMER pruefen ob die App Dynamic Color unterstuetzt

### Kontrast-Anforderungen

| Kombination | Mindest-Kontrast | Standard |
|-------------|-----------------|----------|
| Text auf Surface | 4.5:1 (AA) | On Surface auf Surface |
| Groesser Text (>18sp) auf Surface | 3:1 (AA) | |
| UI-Elemente (Icons, Borders) | 3:1 (AA) | On Surface Variant |
| Dekorative Elemente | Kein Minimum | |

---

## Typography — Type Scale

Material Design 3 definiert 15 Type-Stufen in 5 Rollen:

| Rolle | Stufe | Groesse (sp) | Zeilenhoehe (sp) | Gewicht | Letter-Spacing (sp) |
|-------|-------|-------------|-----------------|---------|---------------------|
| Display | Large | 57 | 64 | 400 | -0.25 |
| Display | Medium | 45 | 52 | 400 | 0 |
| Display | Small | 36 | 44 | 400 | 0 |
| Headline | Large | 32 | 40 | 400 | 0 |
| Headline | Medium | 28 | 36 | 400 | 0 |
| Headline | Small | 24 | 32 | 400 | 0 |
| Title | Large | 22 | 28 | 400 | 0 |
| Title | Medium | 16 | 24 | 500 | 0.15 |
| Title | Small | 14 | 20 | 500 | 0.1 |
| Body | Large | 16 | 24 | 400 | 0.5 |
| Body | Medium | 14 | 20 | 400 | 0.25 |
| Body | Small | 12 | 16 | 400 | 0.4 |
| Label | Large | 14 | 20 | 500 | 0.1 |
| Label | Medium | 12 | 16 | 500 | 0.5 |
| Label | Small | 11 | 16 | 500 | 0.5 |

### Empfohlene Schriftarten

- **Roboto** (Standard, immer verfuegbar)
- **Google Sans** / **Product Sans** (moderner, fuer Display/Headline)
- **Inter** (beliebt bei modernen Apps)
- **Manrope** / **Space Grotesk** (Trend 2025/2026)

---

## Component Specifications

### Buttons

| Typ | Hoehe | Min-Breite | Eckenradius | Padding (horizontal) |
|-----|-------|-----------|-------------|---------------------|
| Filled | 40dp | 48dp | 20dp (full) | 24dp |
| Outlined | 40dp | 48dp | 20dp (full) | 24dp |
| Text | 40dp | 48dp | 20dp (full) | 12dp |
| Elevated | 40dp | 48dp | 20dp (full) | 24dp |
| Tonal | 40dp | 48dp | 20dp (full) | 24dp |
| FAB Small | 40dp | 40dp | 12dp | — |
| FAB | 56dp | 56dp | 16dp | — |
| FAB Large | 96dp | 96dp | 28dp | — |
| Extended FAB | 56dp | — | 16dp | 16dp |

### Cards

| Typ | Eckenradius | Elevation | Padding |
|-----|-------------|-----------|---------|
| Elevated | 12dp | Level 1 (1dp shadow) | 16dp |
| Filled | 12dp | Level 0 (kein Shadow) | 16dp |
| Outlined | 12dp | Level 0 + 1dp Border | 16dp |

### Text Fields

| Typ | Hoehe | Eckenradius | Label-Position |
|-----|-------|-------------|---------------|
| Filled | 56dp | 4dp (oben), 0dp (unten) | Im Feld (animated) |
| Outlined | 56dp | 4dp (alle) | Auf dem Rand (animated) |

### Top App Bar

| Typ | Hoehe | Elevation |
|-----|-------|-----------|
| Center-aligned | 64dp | Scroll: Surface Container |
| Small | 64dp | Scroll: Surface Container |
| Medium | 112dp (collapsed: 64dp) | Surface Container |
| Large | 152dp (collapsed: 64dp) | Surface Container |

### Navigation Bar (Bottom)

| Eigenschaft | Wert |
|-------------|------|
| Hoehe | 80dp |
| Icon-Groesse | 24dp |
| Indikator | Pill-Form, 64x32dp, Primary Container |
| Label | Label Medium (12sp) |
| Max Items | 5 |
| Padding (oben) | 12dp |
| Padding (unten) | 16dp |

### Navigation Drawer

| Eigenschaft | Wert |
|-------------|------|
| Breite | 360dp (max) |
| Eckenradius | 0dp (Standard), 16dp (modal) |
| Item-Hoehe | 56dp |
| Active Indicator | 56x336dp, Secondary Container |
| Padding (horizontal) | 12dp |

---

## Elevation System (Tonal Elevation)

MD3 ersetzt Schatten weitgehend durch **Tonal Elevation**: Hoehere Elevation =
leicht hellere Oberflaeche (im Dark Mode besonders sichtbar).

| Level | Shadow | Surface Tint Opacity |
|-------|--------|---------------------|
| Level 0 | 0dp | 0% |
| Level 1 | 1dp | 5% |
| Level 2 | 3dp | 8% |
| Level 3 | 6dp | 11% |
| Level 4 | 8dp | 12% |
| Level 5 | 12dp | 14% |

---

## Shape System

| Kategorie | Eckenradius | Beispiele |
|-----------|-------------|----------|
| None | 0dp | — |
| Extra Small | 4dp | TextFields |
| Small | 8dp | Chips, Snackbars |
| Medium | 12dp | Cards, Dialoge |
| Large | 16dp | FAB, Navigation Drawer |
| Extra Large | 28dp | Bottom Sheets, Dialoge (fullscreen) |
| Full | 50% / 999dp | FAB (klein), Badges, Avatare |

---

## Motion / Animation

### Standard Easing

| Name | Cubic Bezier | Verwendung |
|------|-------------|------------|
| Emphasized | (0.2, 0, 0, 1) | Uebergaenge, wichtige Bewegungen |
| Emphasized Decelerate | (0.05, 0.7, 0.1, 1) | Elemente die erscheinen |
| Emphasized Accelerate | (0.3, 0, 0.8, 0.15) | Elemente die verschwinden |
| Standard | (0.2, 0, 0, 1) | Layout-Aenderungen |
| Standard Decelerate | (0, 0, 0, 1) | Einfache Erscheinungen |
| Standard Accelerate | (0.3, 0, 1, 1) | Einfaches Verschwinden |

### Dauer

| Typ | Dauer | Verwendung |
|-----|-------|------------|
| Short 1 | 50ms | Ripple, Hover |
| Short 2 | 100ms | Selection, Toggle |
| Short 3 | 150ms | Menü oeffnen |
| Short 4 | 200ms | Snackbar erscheinen |
| Medium 1 | 250ms | Page-Uebergang |
| Medium 2 | 300ms | Dialog oeffnen |
| Medium 3 | 350ms | Drawer oeffnen |
| Medium 4 | 400ms | Bottom Sheet |
| Long 1 | 450ms | Navigation-Uebergang |
| Long 2 | 500ms | Komplexe Uebergaenge |
| Long 3 | 550ms | Shared Element |
| Long 4 | 600ms | Hero-Animationen |
| Extra Long 1-4 | 700-1000ms | Seltene, komplexe Animationen |
