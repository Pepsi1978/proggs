# Accessibility — Barrierefreiheits-Referenz

Barrierefreiheit ist kein Nice-to-have — sie ist eine Pflicht. Google Play
bewertet Apps mit guter Accessibility hoeher, und ca. 15% der Weltbevoelkerung
haben irgendeine Form von Behinderung. Eine barrierefreie App ist automatisch
auch eine besser benutzbare App fuer ALLE Nutzer.

---

## WCAG 2.1 Kontrast-Anforderungen

### Level AA (PFLICHT — Mindeststandard)

| Element | Mindest-Kontrast | Berechnung |
|---------|-----------------|------------|
| Normaler Text (<18sp oder <14sp bold) | **4.5:1** | On Surface vs Surface |
| Grosser Text (>=18sp oder >=14sp bold) | **3:1** | Headline auf Background |
| UI-Komponenten (Icons, Borders, Inputs) | **3:1** | Icon-Farbe vs Hintergrund |
| Dekorative Elemente | Kein Minimum | Rein visuell, nicht informativ |
| Disabled-Zustaende | Kein Minimum | Aber Disabled-Status muss erkennbar sein |
| Placeholder-Text | **4.5:1** empfohlen | Viele Apps versagen hier |

### Level AAA (Empfohlen — bestes Ergebnis)

| Element | Mindest-Kontrast |
|---------|-----------------|
| Normaler Text | **7:1** |
| Grosser Text | **4.5:1** |

### Kontrast berechnen (Python)

```python
def relative_luminance(hex_color):
    """Berechnet die relative Luminanz nach WCAG 2.1."""
    hex_color = hex_color.lstrip('#')
    r, g, b = int(hex_color[0:2], 16)/255, int(hex_color[2:4], 16)/255, int(hex_color[4:6], 16)/255
    r = r/12.92 if r <= 0.03928 else ((r+0.055)/1.055)**2.4
    g = g/12.92 if g <= 0.03928 else ((g+0.055)/1.055)**2.4
    b = b/12.92 if b <= 0.03928 else ((b+0.055)/1.055)**2.4
    return 0.2126*r + 0.7152*g + 0.0722*b

def contrast_ratio(hex1, hex2):
    """Berechnet das Kontrastverhaeltnis zweier Farben (1:1 bis 21:1)."""
    l1, l2 = relative_luminance(hex1), relative_luminance(hex2)
    if l1 < l2: l1, l2 = l2, l1
    ratio = (l1 + 0.05) / (l2 + 0.05)
    return round(ratio, 2)

# Beispiele:
# contrast_ratio('#1D1B20', '#FEF7FF') = 16.37 (WCAG AAA)
# contrast_ratio('#6750A4', '#FFFFFF') = 5.28 (WCAG AA)
# contrast_ratio('#79747E', '#FEF7FF') = 4.68 (WCAG AA)
```

### Haeufige Kontrast-Fehler

| Fehler | Warum problematisch | Loesung |
|--------|--------------------|---------| 
| Hellgrauer Text auf weissem Hintergrund | Kontrast oft unter 3:1 | Dunkleres Grau verwenden (#49454F statt #CCCCCC) |
| Bunte Buttons mit weissem Text | Kontrast kann unter 4.5:1 fallen | Kontrast rechnerisch pruefen |
| Placeholder in TextFields | Fast immer unter 4.5:1 | Dunkleren Placeholder-Ton waehlen |
| Subtiler Outline/Border | Unter 3:1 gegen Hintergrund | Outline Variant (#CAC4D0) mindestens |
| Dark Mode: Bunter Text auf #121212 | Gesaettigte Farben blenden | Saettigung reduzieren fuer Dark Mode |

---

## Touch Targets

### Mindestgroessen

| Guideline | Mindestgroesse | Empfohlen |
|-----------|---------------|----------|
| Material Design 3 | **48dp x 48dp** | 48dp x 48dp |
| Google Play Accessibility | **48dp x 48dp** | — |
| WCAG 2.5.5 (AAA) | 44x44 CSS px | — |
| Apple HIG (zum Vergleich) | 44pt x 44pt | — |

### Warum 48dp und nicht 44dp?

Google/Material empfiehlt 48dp weil:
- Der durchschnittliche Finger-Touchbereich ist ~7mm (ca. 40dp)
- 48dp gibt 4dp Puffer auf jeder Seite
- Groessere Targets reduzieren Fehl-Taps signifikant
- Insbesondere wichtig fuer aeltere Nutzer und Nutzer mit motorischen Einschraenkungen

### Haeufige Touch-Target-Fehler

| Element | Problem | Loesung |
|---------|---------|---------|
| Icon-Buttons (24dp Icon) | Icon ist 24dp, Tap-Bereich auch 24dp | `Modifier.size(48.dp)` mit `padding` |
| Checkbox/Radio ohne Padding | Nur die Box ist tappbar (16dp) | `Modifier.clickable()` auf Container |
| Links in Fliesstext | Nur der Text ist tappbar | `Modifier.padding(vertical = 12.dp)` |
| Navigations-Icons in BottomBar | Zu dicht beieinander | Mindestens 48dp Abstand zwischen Icons |
| "X" Schliessen-Buttons | Oft nur 24dp | Mindestens 48dp Touch-Target |

### Compose-Pattern fuer korrekte Touch Targets

```kotlin
// FALSCH: Touch Target ist nur 24dp (Icon-Groesse)
Icon(
    imageVector = Icons.Default.Close,
    modifier = Modifier.size(24.dp).clickable { onClose() }
)

// RICHTIG: Touch Target ist 48dp, Icon bleibt 24dp
IconButton(onClick = onClose) { // IconButton ist automatisch 48dp
    Icon(
        imageVector = Icons.Default.Close,
        contentDescription = "Schliessen"
    )
}
```

---

## Schriftgroessen-Skalierung

### sp vs dp — Warum sp fuer Text PFLICHT ist

| Einheit | Skaliert mit System-Schriftgroesse | Verwenden fuer |
|---------|-----------------------------------|---------------|
| **sp** | JA — passt sich an | Text (IMMER!) |
| **dp** | NEIN — bleibt fix | Abstande, Groessen, Layouts |

### System-Schriftgroessen-Stufen

| Stufe | Skalierungsfaktor | 14sp wird zu |
|-------|------------------|-------------|
| Klein | 0.85 | 11.9sp |
| Standard | 1.0 | 14sp |
| Gross | 1.15 | 16.1sp |
| Groesser | 1.3 | 18.2sp |
| Am groessten | 2.0 | 28sp |

### Haeufige Fehler

- `fontSize = 14.dp` statt `14.sp` → Text skaliert NICHT mit System-Einstellung
- Feste Container-Hoehen die bei grosser Schrift abschneiden
- `maxLines = 1` ohne `overflow = TextOverflow.Ellipsis`
- Layouts die bei 200% Schriftgroesse umbrechen oder ueberlappen

---

## Content Descriptions (TalkBack)

### Regeln fuer contentDescription

| Element | contentDescription | Beispiel |
|---------|-------------------|---------|
| Dekorative Icons/Bilder | `null` | Hintergrund-Muster, Trennlinien |
| Informative Icons | Beschreibend | "Favorit hinzufuegen", "Loeschen" |
| Bilder mit Inhalt | Beschreibung des Inhalts | "Profilbild von [Name]" |
| Buttons mit nur Icon | Aktion beschreiben | "Menue oeffnen", "Zurueck" |
| Buttons mit Text | Nicht noetig (Text wird gelesen) | — |
| Toggle/Switch | Zustand beschreiben | "Benachrichtigungen: An/Aus" |

### Compose-Pattern

```kotlin
// Dekorativ — TalkBack ignoriert es
Icon(
    imageVector = Icons.Default.Star,
    contentDescription = null // Bewusst null!
)

// Informativ — TalkBack liest es vor
Icon(
    imageVector = Icons.Default.Delete,
    contentDescription = "Eintrag loeschen"
)

// Bild mit Inhalt
Image(
    painter = painterResource(id = R.drawable.hero),
    contentDescription = "Sonnenuntergang ueber dem Meer"
)
```

---

## Farbenblindheit

Ca. 8% der Maenner und 0.5% der Frauen haben eine Farbsehschwaeche.

### Die drei haeufigsten Typen

| Typ | Haeufigkeit | Betroffene Farben | Vermeiden |
|-----|------------|-------------------|----------|
| Deuteranopie (Gruenschwaeche) | ~6% Maenner | Gruen/Rot nicht unterscheidbar | Rot+Gruen als einziger Unterschied |
| Protanopie (Rotschwaeche) | ~2% Maenner | Rot erscheint dunkel/braun | Rote Warnungen ohne zweites Signal |
| Tritanopie (Blau-Gelb) | ~0.01% | Blau/Gelb verwechselbar | Sehr selten, niedrige Prioritaet |

### Regeln

1. **Farbe NIEMALS als einziges Signal verwenden**
   - Fehler: Rot + Text. NICHT: nur roten Rand
   - Status: Icon + Farbe. NICHT: nur Farbpunkt
   - Pflichtfelder: Stern + Text. NICHT: nur rotes Label

2. **Icons oder Muster zusaetzlich zur Farbe**
   - Erfolg: Gruener Haken (Icon zeigt Bedeutung auch ohne Farbe)
   - Fehler: Rotes Ausrufezeichen (Icon zeigt Bedeutung auch ohne Farbe)
   - Warnung: Gelbes Dreieck (Form zeigt Bedeutung auch ohne Farbe)

3. **Kontrast zwischen benachbarten Farben**
   - Nicht nur Kontrast zum Hintergrund, sondern auch zwischen nebeneinanderliegenden farbigen Elementen (z.B. in Charts)
