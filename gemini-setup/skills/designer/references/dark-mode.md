# Dark Mode — Best Practices Referenz

Ein guter Dark Mode ist NICHT einfach "Farben invertieren". Er erfordert ein
eigenes Farbsystem das auf dunklen Oberflaechen optimiert ist. Google's Material
Design Team hat umfangreiche Forschung zu diesem Thema betrieben.

---

## Grundprinzipien

### 1. Kein reines Schwarz (#000000)

| Empfehlung | Wert | Warum |
|------------|------|-------|
| Surface (Basis) | #121212 | Reines Schwarz erzeugt zu starken Kontrast mit hellen Elementen ("Halo-Effekt") |
| Surface Container | #1E1E1E | Leicht heller fuer Container |
| Background | #121212 | Konsistent mit Surface |

### 2. Elevation = Helligkeit

Im Dark Mode wird Elevation NICHT durch Schatten dargestellt (Schatten auf dunklem
Hintergrund sind unsichtbar). Stattdessen werden hoeher-elevierte Oberflaechen HELLER:

| Elevation Level | Surface Overlay Opacity | Resultierende Farbe (bei #121212 Basis) |
|-----------------|------------------------|----------------------------------------|
| 0dp | 0% | #121212 |
| 1dp | 5% | #1E1E1E |
| 2dp | 7% | #222222 |
| 3dp | 8% | #242424 |
| 4dp | 9% | #272727 |
| 6dp | 11% | #2C2C2C |
| 8dp | 12% | #2E2E2E |
| 12dp | 14% | #333333 |
| 16dp | 15% | #353535 |
| 24dp | 16% | #383838 |

### 3. Reduzierte Saettigung

Hoch-gesaettigte Farben auf dunklen Oberflaechen verursachen:
- Vibrieren/Flimmern (besonders bei Rot und Gruen)
- Augen-Ermuedung bei laengerem Lesen
- Schlechte Lesbarkeit

| Farbe | Light Mode (gesaettigt) | Dark Mode (entsaettigt) |
|-------|------------------------|------------------------|
| Primary | #6750A4 (gesaettigt) | #D0BCFF (aufgehellt, entsaettigt) |
| Error | #B3261E (kraeftiges Rot) | #F2B8B5 (weiches Rosa-Rot) |
| Tertiary | #7D5260 (dunkles Rosa) | #EFB8C8 (helles Rosa) |

**Faustregel:** Im Dark Mode werden die Farben um 200-300 Stufen in der Tonal Palette
nach oben verschoben (z.B. Primary von Tone 40 auf Tone 80).

---

## Farb-Transformation Light → Dark

### Material 3 Tone-Mapping

| Color Role | Light Mode Tone | Dark Mode Tone |
|-----------|----------------|---------------|
| Primary | 40 | 80 |
| On Primary | 100 | 20 |
| Primary Container | 90 | 30 |
| On Primary Container | 10 | 90 |
| Secondary | 40 | 80 |
| Secondary Container | 90 | 30 |
| Tertiary | 40 | 80 |
| Tertiary Container | 90 | 30 |
| Surface | 99 | 6 |
| On Surface | 10 | 90 |
| Surface Variant | 90 | 30 |
| On Surface Variant | 30 | 80 |
| Outline | 50 | 60 |

### Praxis-Tipps

1. **Weisse Texte/Icons auf dunklem Hintergrund:** Nicht #FFFFFF (100% Weiss) verwenden.
   Empfehlung: #E6E0E9 (ca. 87% Deckkraft) — reduziert Blendung.

2. **Surface-Hierarchie aufbauen:**
   ```
   Background (#121212) → Surface (#1E1E1E) → Card (#242424) → Elevated Card (#2C2C2C)
   ```
   Jede Ebene ist leicht heller als die darunter.

3. **Schatten NICHT entfernen:** Auch wenn Schatten im Dark Mode weniger sichtbar sind,
   geben sie dennoch raeumliche Tiefe. Nur die Staerke reduzieren.

4. **Divider/Trennlinien:** Nicht #FFFFFF mit niedriger Opacity (wirkt flach).
   Empfehlung: #49454F (On Surface Variant) mit 12-15% Opacity.

---

## Dark Mode Checkliste

### Farbsystem

- [ ] Keine reinen Schwarztoene (#000000) als Hintergrund
- [ ] Surface-Hierarchie mit steigender Helligkeit bei hoeherer Elevation
- [ ] Primary/Secondary/Tertiary Farben entsaettigt (Tone 80 statt 40)
- [ ] On-Farben angepasst (Tone 20 statt 100)
- [ ] Container-Farben angepasst (Tone 30 statt 90)
- [ ] Error-Farbe entsaettigt (#F2B8B5 statt #B3261E)
- [ ] Outline-Farbe angepasst (#938F99 statt #79747E)

### Bilder und Icons

- [ ] Bilder leicht abgedunkelt (8-12% schwarzes Overlay) um Blendung zu reduzieren
- [ ] Illustrationen in gedeckteren Toenen
- [ ] Icons verwenden On-Surface-Farbe, nicht hartcodiertes Schwarz/Weiss
- [ ] App-Icon bleibt unveraendert (System-Level, nicht App-Level)

### Komponenten

- [ ] Cards: Surface Container (nicht Surface) als Hintergrund
- [ ] Dialoge: Surface Container High als Hintergrund
- [ ] Bottom Sheets: Surface Container Low als Hintergrund
- [ ] Snackbars: Inverse Surface als Hintergrund
- [ ] TextFields: Outline oder Filled mit korrekten Dark-Farben
- [ ] Switches/Toggles: Korrekte On/Off-Farben

### Text

- [ ] Haupttext: On Surface (#E6E0E9), NICHT reines Weiss
- [ ] Sekundaertext: On Surface Variant (#CAC4D0)
- [ ] Disabled-Text: On Surface mit 38% Opacity
- [ ] Kontrast-Check: Alle Text-Kombinationen >= 4.5:1

### Navigation

- [ ] Status Bar: Transparent oder Surface-Farbe (NICHT Schwarz)
- [ ] Navigation Bar: Surface oder Surface Container
- [ ] Top App Bar: Surface (bei Scroll: Surface Container)
- [ ] Bottom Navigation: Surface

---

## Haeufige Dark-Mode-Fehler

| Fehler | Problem | Loesung |
|--------|---------|---------|
| `Color.Black` als Background | Zu starker Kontrast, "OLED-Schwarz"-Look wirkt billig | `#121212` oder Material Surface |
| `Color.White` fuer Text | Blendet auf dunklem Hintergrund | On Surface (#E6E0E9) |
| Gleiche Farben wie Light Mode | Zu gesaettigt, vibriert | Tone-Mapping anwenden (40→80) |
| Elevation via Shadow | Schatten auf Schwarz unsichtbar | Tonal Elevation (heller = hoeher) |
| Kein Dark Mode fuer Custom Views | Views bleiben hell im Dark Mode | `isSystemInDarkTheme()` pruefen |
| Hardcoded Farben in XML/Compose | Ueberschreiben das Theme | Immer `MaterialTheme.colorScheme.X` |
| Dark Mode vergessen bei neuen Screens | Neuer Screen hat Light-Farben | Standard-Workflow: beide Themes testen |

---

## Compose: Dark Mode korrekt implementieren

```kotlin
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme(
            primary = Color(0xFFD0BCFF),
            onPrimary = Color(0xFF381E72),
            primaryContainer = Color(0xFF4F378B),
            // ... alle Dark-Farben definieren
            surface = Color(0xFF141218),
            onSurface = Color(0xFFE6E0E9),
        )
        else -> lightColorScheme(
            primary = Color(0xFF6750A4),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFEADDFF),
            // ... alle Light-Farben definieren
            surface = Color(0xFFFEF7FF),
            onSurface = Color(0xFF1D1B20),
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

### Farben richtig referenzieren

```kotlin
// FALSCH: Hardcoded Farbe — ignoriert Dark Mode
Text(
    text = "Hallo",
    color = Color.Black  // Im Dark Mode unsichtbar!
)

// RICHTIG: Theme-Farbe — passt sich automatisch an
Text(
    text = "Hallo",
    color = MaterialTheme.colorScheme.onSurface
)
```
