# Claude Code Prompt: „Entropy Journal" – Android-App

## Projektübersicht

Erstelle eine vollständige native Android-App in **Kotlin** mit **Jetpack Compose** namens **„Entropy Journal"**. Die App ist ein persönliches, KI-gestütztes Tagebuch, das Spracheinträge aufnimmt, transkribiert, optional verbessert und in einer chronologischen Timeline darstellt. Ein separater KI-Analyse-Bereich erkennt automatisch Muster persönlicher Entropie (wiederkehrende Probleme, Stressoren, Blockaden) und generiert daraus ein kohärentes, thematisch strukturiertes Ratschlags-Dashboard, das sich mit jedem neuen Eintrag automatisch aktualisiert.

Die App richtet sich an eine technisch versierte Zielgruppe und soll optisch auf absolutem Premium-Niveau sein: modernes, dunkles Design mit 3D-Effekten, Glasmorphismus, flüssigen Übergangsanimationen und einer durchdachten Typografie. Sie soll sich anfühlen wie eine App aus der Zukunft – inspiriert von Science-Fiction-Interfaces, aber funktional und klar strukturiert.

---

## Technologie-Stack

```
Sprache:               Kotlin
UI-Framework:          Jetpack Compose (Material 3)
Mindest-SDK:           API 26 (Android 8.0)
Ziel-SDK:              API 35 (Android 15)
Build-System:          Gradle (Kotlin DSL)
Architektur:           MVVM + Clean Architecture (UI → ViewModel → UseCase → Repository)
Dependency Injection:  Hilt (Dagger)
Lokale Datenbank:      Room (SQLite)
Cloud-Sync:            Google Drive API v3 (App-Daten-Ordner)
Authentifizierung:     Google Sign-In (Credential Manager API)
Netzwerk:              Retrofit 2 + OkHttp + Moshi (JSON)
Sprachaufnahme:        Android MediaRecorder / AudioRecord
Transkription:         Groq API (Whisper Large v3 Turbo)
Textverbesserung:      Google Gemini API (gemini-2.0-flash-lite)
KI-Analyse:            Google Gemini API (gemini-2.0-flash)
Navigation:            Jetpack Navigation Compose
Animationen:           Compose Animation APIs + gemeinsame Elementübergänge
Bilder/Icons:          Material Icons Extended + benutzerdefinierte SVG-Icons
```

---

## Projektstruktur

```
app/src/main/java/com/entropyjournal/
├── di/                          # Hilt-Module
│   ├── AppModule.kt
│   ├── NetworkModule.kt
│   ├── DatabaseModule.kt
│   └── AuthModule.kt
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt
│   │   ├── dao/
│   │   │   ├── JournalEntryDao.kt
│   │   │   └── AdviceDashboardDao.kt
│   │   └── entity/
│   │       ├── JournalEntryEntity.kt
│   │       └── AdviceBlockEntity.kt
│   ├── remote/
│   │   ├── groq/
│   │   │   ├── GroqApi.kt
│   │   │   ├── GroqTranscriptionRequest.kt
│   │   │   └── GroqTranscriptionResponse.kt
│   │   ├── gemini/
│   │   │   ├── GeminiApi.kt
│   │   │   ├── GeminiRequest.kt
│   │   │   └── GeminiResponse.kt
│   │   └── googledrive/
│   │       ├── DriveBackupManager.kt
│   │       └── DriveRestoreManager.kt
│   └── repository/
│       ├── JournalRepository.kt
│       ├── TranscriptionRepository.kt
│       ├── AdviceRepository.kt
│       └── AuthRepository.kt
├── domain/
│   ├── model/
│   │   ├── JournalEntry.kt
│   │   ├── AdviceBlock.kt
│   │   ├── AdviceCategory.kt
│   │   └── UserProfile.kt
│   └── usecase/
│       ├── RecordAudioUseCase.kt
│       ├── TranscribeAudioUseCase.kt
│       ├── ImproveTextUseCase.kt
│       ├── SaveJournalEntryUseCase.kt
│       ├── AnalyzeEntropyUseCase.kt
│       ├── GenerateAdviceUseCase.kt
│       ├── SyncWithDriveUseCase.kt
│       └── SignInWithGoogleUseCase.kt
├── ui/
│   ├── theme/
│   │   ├── Theme.kt
│   │   ├── Color.kt
│   │   ├── Typography.kt
│   │   ├── Shape.kt
│   │   └── GlassmorphismModifiers.kt
│   ├── components/
│   │   ├── GlassCard.kt
│   │   ├── AnimatedMicButton.kt
│   │   ├── PulsingOrb.kt
│   │   ├── TimelineItem.kt
│   │   ├── AdviceCategoryCard.kt
│   │   ├── NeonDivider.kt
│   │   ├── ParticleBackground.kt
│   │   ├── ShimmerLoadingEffect.kt
│   │   ├── FloatingActionButtonAnimated.kt
│   │   ├── ThreeDCard.kt
│   │   └── WaveformVisualizer.kt
│   ├── screens/
│   │   ├── splash/
│   │   │   ├── SplashScreen.kt
│   │   │   └── SplashViewModel.kt
│   │   ├── login/
│   │   │   ├── LoginScreen.kt
│   │   │   └── LoginViewModel.kt
│   │   ├── journal/
│   │   │   ├── JournalScreen.kt
│   │   │   ├── JournalViewModel.kt
│   │   │   └── RecordingOverlay.kt
│   │   ├── dashboard/
│   │   │   ├── DashboardScreen.kt
│   │   │   ├── DashboardViewModel.kt
│   │   │   └── AdviceDetailSheet.kt
│   │   ├── settings/
│   │   │   ├── SettingsScreen.kt
│   │   │   └── SettingsViewModel.kt
│   │   └── entrydetail/
│   │       ├── EntryDetailScreen.kt
│   │       └── EntryDetailViewModel.kt
│   └── navigation/
│       ├── AppNavGraph.kt
│       └── BottomNavBar.kt
└── util/
    ├── AudioRecorderHelper.kt
    ├── DateTimeFormatter.kt
    ├── NetworkUtils.kt
    └── Constants.kt
```

---

## Detaillierte Feature-Spezifikationen

### 1. Google Sign-In (Authentifizierung)

**Ablauf:**
- Beim ersten App-Start erscheint ein Splash-Screen mit dem App-Logo (animierte Entropie-Spirale, die sich zu einem Punkt zusammenzieht – symbolisch für Entropiereduktion).
- Danach wird der Login-Screen angezeigt mit einem prominenten „Mit Google anmelden"-Button im offiziellen Google-Design (weißer Button, Google-„G"-Logo in Original-Farben, Text „Mit Google anmelden").
- Verwende die neue **Credential Manager API** (empfohlen seit 2024, ersetzt das veraltete Google Sign-In SDK).
- Nach erfolgreicher Anmeldung navigiert die App direkt zum Tagebuch-Screen.
- Der Nutzer bleibt dauerhaft angemeldet (Token wird in EncryptedSharedPreferences gespeichert), bis er aktiv den Logout-Button in den Einstellungen drückt.
- Ein „Abmelden"-Button befindet sich ausschließlich im Settings-Screen, deutlich beschriftet, mit einem Bestätigungs-Dialog.

**Google Drive Synchronisation:**
- Nutze den **Google Drive App Data Folder** (appDataFolder), der nur für die eigene App sichtbar ist.
- Die gesamte Room-Datenbank wird als verschlüsselte Datei in den App-Data-Ordner gesichert.
- Synchronisation erfolgt automatisch nach jedem neuen Tagebucheintrag (debounced: frühestens 30 Sekunden nach dem letzten Eintrag) und beim App-Start.
- Ein manueller „Jetzt synchronisieren"-Button befindet sich im Settings-Screen.
- Bei der Wiederherstellung (z. B. Neuinstallation oder neues Gerät) wird beim Login automatisch geprüft, ob ein Backup existiert, und angeboten, es wiederherzustellen.
- Zeige einen dezenten Sync-Status-Indikator in der oberen rechten Ecke des Tagebuch-Screens (kleines Cloud-Icon: grün = synchronisiert, orange = wird synchronisiert, rot = Fehler).

---

### 2. Sprachaufnahme und Transkription

**Aufnahme-Mechanik:**
- Ein großer, runder, animierter Mikrofon-Button schwebt als Floating Action Button am unteren Rand des Tagebuch-Screens.
- **Drücken** startet die Aufnahme – der Button pulsiert mit einem leuchtenden Ring (Farbverlauf von Cyan zu Violett), und eine Echtzeit-Wellenform-Visualisierung erscheint über dem Button.
- **Erneutes Drücken** stoppt die Aufnahme.
- Die Aufnahme wird als WAV-Datei (16 kHz, Mono) im internen App-Speicher zwischengespeichert.
- Maximale Aufnahmedauer: 5 Minuten pro Eintrag.
- Während der Aufnahme wird die Dauer live angezeigt (Format: MM:SS).

**Transkription (Groq API):**
- Sofort nach dem Stoppen der Aufnahme wird die Audio-Datei an die Groq-API gesendet.
- Endpoint: `https://api.groq.com/openai/v1/audio/transcriptions`
- Modell: `whisper-large-v3-turbo`
- Sprache: `de` (Deutsch fest vorgeben)
- Die Transkription wird als Multipart-Request gesendet (Datei + Modell-Parameter).
- Während der Transkription zeigt die App einen schimmernden Lade-Effekt mit dem Text „Transkribiere..." an.
- Der API-Key wird im Settings-Screen konfiguriert und in EncryptedSharedPreferences gespeichert.

**Request-Format an Groq:**
```
POST /openai/v1/audio/transcriptions
Content-Type: multipart/form-data

file: [audio.wav]
model: whisper-large-v3-turbo
language: de
response_format: json
```

**Textverbesserung (Gemini API – optional):**
- Nach der Transkription erscheint der Rohtext in einem Vorschau-Dialog.
- Ein Toggle-Button „Text verbessern" (standardmäßig AUS) ist über dem Text platziert.
- Wenn aktiviert, wird der transkribierte Text an die Gemini-API gesendet mit folgendem System-Prompt:

```
Du bist ein deutscher Textoptimierer. Deine einzige Aufgabe ist es, den folgenden
gesprochenen und transkribierten deutschen Text sprachlich zu verbessern.

Regeln:
- Korrigiere Grammatik, Rechtschreibung und Zeichensetzung.
- Formuliere unklare oder abgehackte Sätze flüssiger, aber behalte den Inhalt,
  die Bedeutung und den persönlichen Stil des Sprechers vollständig bei.
- Füge NICHTS hinzu, das nicht im Original enthalten ist.
- Entferne Füllwörter (ähm, also, sozusagen, quasi) nur dann, wenn sie den
  Lesefluss stören.
- Behalte die Ich-Perspektive bei.
- Gib NUR den verbesserten Text zurück, ohne Erklärungen, ohne Anführungszeichen,
  ohne Einleitungen.
```

- Gemini-Modell: `gemini-2.0-flash-lite` (für schnelle Antworten)
- Der API-Key wird ebenfalls im Settings-Screen konfiguriert.
- Während der Verbesserung wird ein Lade-Indikator angezeigt: „Optimiere Text..."
- Der Nutzer sieht nach der Verbesserung beide Versionen (Original und verbessert) und kann per Tab zwischen ihnen wechseln, bevor er auf „Speichern" drückt.

---

### 3. Tagebuch-Timeline

**Datenmodell (Room-Entity):**
```kotlin
@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,              // Unix-Timestamp in Millisekunden
    val rawText: String,              // Original-Transkription
    val improvedText: String?,        // Verbesserter Text (nullable, wenn nicht verwendet)
    val isImproved: Boolean,          // Flag, ob Verbesserung angewendet wurde
    val displayText: String,          // Der angezeigte Text (raw oder improved)
    val audioDurationSeconds: Int,    // Dauer der Aufnahme
    val moodTag: String?,             // Optional: automatisch erkanntes Stimmungstag
    val entropyScore: Float?,         // Optional: KI-bewerteter Entropie-Score (0.0–1.0)
    val adviceCategoryTags: String?,  // Komma-separierte Kategorie-Tags
    val isSynced: Boolean = false     // Sync-Status mit Google Drive
)
```

**Timeline-Darstellung:**
- Vertikale, scrollbare Liste mit dem neuesten Eintrag ganz oben.
- Jeder Eintrag ist eine **Glas-Karte** (Glasmorphismus: halbtransparenter Hintergrund mit Blur-Effekt, feiner weißer Randlinie).
- Layout jeder Karte:
  - **Oben links:** Zeitstempel im Format „Sa, 28. März 2026 · 14:32 Uhr" (relative Zeitangabe darunter: „vor 2 Stunden").
  - **Oben rechts:** Kleines farbiges Stimmungs-Emoji (automatisch von der KI zugeordnet, z. B. 🟢 positiv, 🟡 neutral, 🔴 belastend).
  - **Mitte:** Textvorschau (maximal 4 Zeilen, danach „... mehr anzeigen").
  - **Unten:** Kleine farbige Kategorie-Chips (z. B. „Schlaf", „Fitness", „Arbeit") – automatisch von der KI zugeordnet.
- Antippen einer Karte öffnet den vollständigen Eintrag mit einer flüssigen Shared-Element-Transition (die Karte „wächst" zum Vollbild-Detail).
- Detail-Ansicht zeigt den vollständigen Text, den Zeitstempel, das Stimmungs-Tag, Kategorie-Tags und den Entropie-Score als visuellen Indikator (z. B. farbiger Ring von Grün bis Rot).
- In der Detail-Ansicht kann der Nutzer zwischen Original- und verbessertem Text umschalten (falls vorhanden).
- Swipe-to-Delete mit Bestätigungs-Dialog: „Diesen Eintrag unwiderruflich löschen?"

**Suchfunktion:**
- Oben auf dem Tagebuch-Screen befindet sich eine ausklappbare Suchleiste.
- Volltextsuche über alle Einträge.
- Filterung nach Kategorie-Tags und Zeitraum möglich.

---

### 4. KI-Analyse und Ratschlags-Dashboard

**Grundkonzept:**
Das Dashboard ist der zweite Hauptbereich der App, erreichbar über die untere Navigationsleiste. Die KI analysiert das gesamte Tagebuch als Ganzes und identifiziert wiederkehrende Muster, Probleme und Stressoren – also Quellen persönlicher Entropie. Daraus generiert sie ein kohärentes, thematisch strukturiertes Ratschlags-Dashboard, in dem alle Ratschläge untereinander verknüpft sind und ein ganzheitliches Bild ergeben.

**KI-Analyse (Gemini API):**
- Modell: `gemini-2.0-flash` (für komplexere Analysen)
- Die Analyse wird ausgelöst:
  1. Automatisch, sobald ein neuer Tagebucheintrag gespeichert wird (mit 60 Sekunden Debounce).
  2. Manuell über einen „Analyse aktualisieren"-Button im Dashboard.
- Bei der Analyse werden **alle** Tagebucheinträge als Kontext an die KI übergeben.

**System-Prompt für die Entropie-Analyse:**

```
Du bist ein empathischer, hochintelligenter Lebensberater und Muster-Analyst.
Deine Aufgabe ist es, aus den Tagebucheinträgen eines Nutzers wiederkehrende Quellen
persönlicher Entropie zu identifizieren und daraus ein kohärentes Ratschlags-Dashboard
zu erstellen.

Definition: „Persönliche Entropie" umfasst alles, was Unordnung, Stress, Energieverlust,
Schmerz, Schlafprobleme, emotionale Belastung oder Kontrollverlust im Leben des Nutzers
erzeugt.

Analysiere die Tagebucheinträge und erstelle ein JSON-Objekt mit folgendem Schema:

{
  "gesamtanalyse": "Ein kurzer, empathischer Absatz (3–5 Sätze), der das aktuelle
                     Gesamtbild der persönlichen Entropie beschreibt und die wichtigsten
                     Hebel zur Reduktion benennt.",
  "kategorien": [
    {
      "name": "Kategoriename",
      "icon": "material_icon_name",
      "farbe": "#HEX",
      "entropie_level": 0.0 bis 1.0,
      "zusammenfassung": "Kurze Zusammenfassung der erkannten Muster in dieser Kategorie.",
      "ratschlaege": [
        {
          "titel": "Kurzer, prägnanter Titel",
          "beschreibung": "Ausführliche, konkrete Empfehlung (3–5 Sätze) mit
                           Bezug auf konkrete Tagebucheinträge.",
          "prioritaet": "hoch" | "mittel" | "niedrig",
          "verknuepfung": "Beschreibung, wie dieser Ratschlag mit anderen Kategorien
                           zusammenhängt."
        }
      ]
    }
  ]
}

Verwende folgende Standard-Kategorien (erweitere sie bei Bedarf automatisch):
- Schlaf (icon: bedtime, farbe: #6C63FF)
- Arbeit (icon: work, farbe: #FF6B6B)
- Fitness (icon: fitness_center, farbe: #4ECDC4)
- Ernährung (icon: restaurant, farbe: #FFE66D)
- Mentale Gesundheit (icon: psychology, farbe: #A78BFA)
- Beziehungen (icon: people, farbe: #F472B6)
- Zuhause (icon: home, farbe: #34D399)
- Persönliche Entwicklung (icon: trending_up, farbe: #60A5FA)

Regeln:
- Schreibe auf Deutsch, klar und direkt, ohne Floskeln.
- Jeder Ratschlag muss sich auf konkrete Muster aus den Tagebucheinträgen beziehen.
- Die Ratschläge müssen untereinander kohärent sein – Widersprüche vermeiden.
- Verknüpfe Ratschläge über Kategorie-Grenzen hinweg (z. B. „Besserer Schlaf wird
  auch deine Fitness verbessern, weil...").
- Maximal 3 Ratschläge pro Kategorie, priorisiert nach Wirkung.
- Der Entropie-Level pro Kategorie wird auf einer Skala von 0.0 (keine Entropie)
  bis 1.0 (maximale Entropie) angegeben.
- Antworte ausschließlich mit dem JSON-Objekt, ohne Markdown-Formatierung,
  ohne Einleitungen, ohne Erklärungen.
```

**Dashboard-Darstellung:**

- **Oben:** Gesamtanalyse-Karte – eine große Glas-Karte mit dem Gesamtüberblick. Darin ein animierter „Entropie-Orbiter": ein kleines Partikel-System, das die aktuelle Gesamt-Entropie visualisiert (viele chaotische Partikel = hohe Entropie, wenige, geordnete Partikel = niedrige Entropie). Darunter der Gesamtanalyse-Text.

- **Darunter:** Horizontale Scroll-Leiste mit Kategorie-Karten. Jede Karte zeigt:
  - Kategorie-Icon (Material Icon)
  - Kategoriename
  - Entropie-Level als farbiger Halbbogen (Grün → Gelb → Rot)
  - Anzahl der Ratschläge

- **Hauptbereich:** Wenn eine Kategorie ausgewählt wird, werden die Ratschläge darunter als ausklappbare Karten angezeigt:
  - Jeder Ratschlag ist eine Glas-Karte mit:
    - Titel (fett)
    - Prioritäts-Indikator (farbiger Punkt: Rot = hoch, Orange = mittel, Blau = niedrig)
    - Beschreibungstext
    - „Verknüpfung"-Bereich: ein kleinerer Text, der die Querverbindung zu anderen Kategorien zeigt
  - Karten sind antippen-fähig für eine Detail-Ansicht mit sanftem Expand-Effekt.

- **Aktualisierungs-Indikator:** Wenn das Dashboard gerade aktualisiert wird (KI arbeitet), erscheint ein dezenter Shimmer-Effekt über dem gesamten Dashboard mit dem Text „Dashboard wird aktualisiert..."

**Datenmodell für Ratschläge (Room-Entity):**
```kotlin
@Entity(tableName = "advice_blocks")
data class AdviceBlockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: String,
    val entropyLevel: Float,
    val categorySummary: String,
    val adviceJson: String,          // JSON-Array der Ratschläge in dieser Kategorie
    val overallAnalysis: String,     // Gesamtanalyse-Text (in jeder Zeile identisch)
    val lastUpdated: Long,           // Zeitpunkt der letzten Aktualisierung
    val basedOnEntryCount: Int       // Anzahl der Einträge, auf denen die Analyse basiert
)
```

---

### 5. Automatische Kategorie- und Stimmungszuordnung für Tagebucheinträge

Jeder neue Tagebucheintrag wird nach dem Speichern automatisch von der KI kategorisiert. Dies geschieht durch einen separaten, schnellen Gemini-Aufruf:

**System-Prompt für Eintragskategorisierung:**

```
Analysiere den folgenden Tagebucheintrag und gib ein JSON-Objekt zurück:

{
  "stimmung": "positiv" | "neutral" | "belastend",
  "kategorien": ["Kategorie1", "Kategorie2"],
  "entropie_score": 0.0 bis 1.0
}

Verwende die Kategorien: Schlaf, Arbeit, Fitness, Ernährung, Mentale Gesundheit,
Beziehungen, Zuhause, Persönliche Entwicklung.
Ein Eintrag kann mehreren Kategorien zugeordnet werden.
Der Entropie-Score zeigt, wie viel persönliche Entropie (Stress, Probleme, Unordnung)
in diesem Eintrag erkennbar ist.
Antworte ausschließlich mit dem JSON-Objekt.
```

---

### 6. Einstellungen (Settings-Screen)

Der Settings-Screen enthält folgende Abschnitte:

**Konto:**
- Angemeldetes Google-Konto anzeigen (Avatar, Name, E-Mail)
- „Abmelden"-Button mit Bestätigungs-Dialog
- „Backup jetzt erstellen"-Button
- „Backup wiederherstellen"-Button
- Letzter Sync-Zeitstempel

**API-Schlüssel:**
- Groq-API-Key Eingabefeld (Passwort-Feld, mit Auge-Icon zum Anzeigen/Verbergen)
- Gemini-API-Key Eingabefeld (gleiche Logik)
- „Verbindung testen"-Button für jeden API-Key (sendet eine Mini-Anfrage und zeigt Ergebnis an)
- Alle Keys werden in EncryptedSharedPreferences gespeichert.

**Textverbesserung:**
- Toggle „Textverbesserung standardmäßig aktivieren" (Standard: AUS)

**Aufnahme:**
- Maximale Aufnahmedauer (Slider: 1–10 Minuten, Standard: 5)

**Dashboard:**
- Toggle „Dashboard automatisch aktualisieren" (Standard: AN)
- „Dashboard jetzt aktualisieren"-Button

**Über die App:**
- App-Version
- Lizenz-Informationen
- Link zum Quellcode (falls Open Source)

---

## Design-Spezifikation: „Neon Cosmos"

### Designphilosophie

Das Design-Thema heißt **„Neon Cosmos"** – eine Verbindung aus kosmischer Tiefe und futuristischer Eleganz. Die App soll sich anfühlen wie eine Kontrollkonsole auf einer Raumstation: dunkel, präzise, mit leuchtenden Akzenten, die in der Tiefe zu schweben scheinen.

### Farbpalette

```kotlin
// Primäre Hintergrundfarben
val CosmosBlack = Color(0xFF0A0A0F)          // Tiefster Hintergrund
val CosmosDeep = Color(0xFF12121A)            // Karten-Hintergrund (Basis)
val CosmosLayer = Color(0xFF1A1A2E)           // Erhöhte Flächen
val CosmosSurface = Color(0xFF232340)         // Aktive Elemente

// Akzentfarben (Neon)
val NeonCyan = Color(0xFF00E5FF)              // Primärer Akzent
val NeonViolet = Color(0xFF7C4DFF)            // Sekundärer Akzent
val NeonMagenta = Color(0xFFFF00E5)           // Tertiärer Akzent
val NeonEmerald = Color(0xFF00FF9C)           // Erfolg/Positiv
val NeonAmber = Color(0xFFFFAB00)             // Warnung
val NeonRed = Color(0xFFFF1744)               // Fehler/Hohe Entropie

// Glasmorphismus
val GlassWhite = Color(0x1AFFFFFF)            // 10% Weiß
val GlassBorder = Color(0x33FFFFFF)           // 20% Weiß (Ränder)
val GlassHighlight = Color(0x0DFFFFFF)        // 5% Weiß (dezenter Glanz)

// Text
val TextPrimary = Color(0xFFE8E8F0)           // Haupttext
val TextSecondary = Color(0xFFA0A0B8)         // Sekundärtext
val TextMuted = Color(0xFF6B6B80)             // Gedimmter Text

// Gradient-Paare
val GradientCyanToViolet = listOf(NeonCyan, NeonViolet)
val GradientVioletToMagenta = listOf(NeonViolet, NeonMagenta)
val GradientEmeraldToCyan = listOf(NeonEmerald, NeonCyan)
```

### Typografie

```kotlin
// Google Fonts importieren
val JetBrainsMono = FontFamily(/* JetBrains Mono – für Zeitstempel und technische Daten */)
val Exo2 = FontFamily(/* Exo 2 – für Überschriften, futuristisch aber lesbar */)
val SourceSansPro = FontFamily(/* Source Sans Pro – für Fließtext */)

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Exo2,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Exo2,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = Exo2,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        letterSpacing = 0.15.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Exo2,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        letterSpacing = 0.15.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = SourceSansPro,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.25.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = SourceSansPro,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp
    )
)
```

### Glasmorphismus-Komponente (wiederverwendbar)

```kotlin
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    glowColor: Color = NeonCyan,
    glowIntensity: Float = 0.15f,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        GlassWhite,
                        GlassHighlight
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(GlassBorder, Color.Transparent)
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .shadow(
                elevation = 0.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = glowColor.copy(alpha = glowIntensity),
                spotColor = glowColor.copy(alpha = glowIntensity)
            )
            .padding(16.dp)
    ) {
        content()
    }
}
```

### 3D-Karten-Effekt

Implementiere einen 3D-Tilt-Effekt für die Kategorie-Karten im Dashboard. Bei Berührung und Ziehen neigt sich die Karte leicht in Richtung des Fingers (maximal 8° Neigung). Nutze `graphicsLayer` mit `rotationX` und `rotationY` basierend auf der Berührungsposition:

```kotlin
@Composable
fun ThreeDCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var rotationX by remember { mutableFloatStateOf(0f) }
    var rotationY by remember { mutableFloatStateOf(0f) }

    val animatedRotationX by animateFloatAsState(
        targetValue = rotationX,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f)
    )
    val animatedRotationY by animateFloatAsState(
        targetValue = rotationY,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f)
    )

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, _ ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        rotationY = ((change.position.x - centerX) / centerX * 8f)
                            .coerceIn(-8f, 8f)
                        rotationX = (-(change.position.y - centerY) / centerY * 8f)
                            .coerceIn(-8f, 8f)
                    },
                    onDragEnd = {
                        rotationX = 0f
                        rotationY = 0f
                    }
                )
            }
            .graphicsLayer {
                this.rotationX = animatedRotationX
                this.rotationY = animatedRotationY
                cameraDistance = 12f * density
            }
    ) {
        GlassCard { content() }
    }
}
```

### Animierter Mikrofon-Button

```kotlin
@Composable
fun AnimatedMicButton(
    isRecording: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        )
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (isRecording) 0.8f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        )
    )

    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing)
        )
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(72.dp)
            .scale(pulseScale)
    ) {
        // Äußerer leuchtender Ring (dreht sich bei Aufnahme)
        if (isRecording) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(NeonCyan, NeonViolet, NeonMagenta, NeonCyan)
                    ),
                    startAngle = ringRotation,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                    alpha = glowAlpha
                )
            }
        }

        // Button-Körper
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(64.dp),
            containerColor = if (isRecording) NeonRed else CosmosSurface,
            contentColor = TextPrimary,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 8.dp
            )
        ) {
            Icon(
                imageVector = if (isRecording)
                    Icons.Rounded.Stop else Icons.Rounded.Mic,
                contentDescription = if (isRecording)
                    "Aufnahme stoppen" else "Aufnahme starten",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
```

### Partikel-Hintergrund

Der Hintergrund des Dashboard-Screens zeigt ein subtiles Partikel-System:
- 30–50 kleine leuchtende Punkte (2–4dp), die sich langsam und zufällig bewegen.
- Farben: abgeschwächte Versionen der Neon-Akzentfarben (10–20% Opazität).
- Bewegung: Langsam, sanft, leicht zufällig (Perlin Noise oder Sinuswellen).
- Zeichne mit `Canvas` in einem `Box` hinter dem Hauptinhalt.

### Splash-Screen Animation

- Schwarzer Hintergrund.
- In der Mitte: Viele kleine Partikel (100+) sind anfangs chaotisch über den gesamten Bildschirm verteilt (= Entropie).
- Über 2 Sekunden fliegen alle Partikel spiralförmig zusammen und formen das App-Logo (ein stilisiertes „E" in einem Kreis, oder ein Spiralen-Symbol).
- Der App-Name „Entropy Journal" faded darunter ein.
- Nach insgesamt 3 Sekunden wird zum Login- oder Tagebuch-Screen navigiert.

### Untere Navigationsleiste

```
┌──────────────────────────────────────────┐
│   📓 Tagebuch   │   🧠 Dashboard   │   ⚙️  │
└──────────────────────────────────────────┘
```

- 3 Tabs: Tagebuch, Dashboard, Einstellungen.
- Aktiver Tab hat einen leuchtenden Unterstrich im Gradient (Cyan → Violett).
- Icons sind Material Icons Rounded.
- Hintergrund: semi-transparentes CosmosDeep mit Blur.
- Der Mikrofon-Button überlappt die Navigationsleiste leicht (cut-out style) – nur auf dem Tagebuch-Tab sichtbar.

---

## Übergangs-Animationen

### Screen-Übergänge:
- Zwischen Tabs: Horizontales Gleiten mit leichtem Fade (300ms, FastOutSlowIn).
- Zum Entry-Detail: Shared-Element-Transition – die angetippte Karte wächst zum Vollbild.
- Login → Tagebuch: Fade-In mit leichtem Scale-Up (vom Zentrum).

### Micro-Interactions:
- **Speichern eines Eintrags:** Die Karte fällt von oben in die Timeline ein mit einem leichten Bounce-Effekt.
- **Löschen eines Eintrags:** Die Karte schrumpft, wird transparent und verschwindet nach links.
- **Dashboard-Update:** Shimmer-Effekt über alle Karten, gefolgt von einem kurzen Pulse der Akzentfarbe.
- **Sync-Indikator:** Das Cloud-Icon dreht sich sanft während der Synchronisation.
- **API-Key gespeichert:** Kurzer grüner Flash-Effekt um das Eingabefeld.

---

## API-Konfiguration

### Groq API
```
Base-URL:     https://api.groq.com/openai/v1/
Endpoint:     /audio/transcriptions
Methode:      POST (multipart/form-data)
Header:       Authorization: Bearer {GROQ_API_KEY}
Parameter:    file (audio), model (whisper-large-v3-turbo), language (de)
```

### Gemini API
```
Base-URL:     https://generativelanguage.googleapis.com/v1beta/
Endpoints:    /models/gemini-2.0-flash-lite:generateContent (Textverbesserung)
              /models/gemini-2.0-flash:generateContent (Dashboard-Analyse)
Methode:      POST (JSON)
Header:       Content-Type: application/json
Parameter:    key={GEMINI_API_KEY} (als URL-Parameter)
Body:         { "contents": [{ "parts": [{ "text": "..." }] }],
                "systemInstruction": { "parts": [{ "text": "..." }] } }
```

---

## Build- und Laufanweisungen

### Voraussetzungen:
1. Android Studio Ladybug oder neuer (alternativ: Gradle-Build in Termux)
2. Android SDK 35
3. Google Cloud Console Projekt mit aktivierter Google Drive API und OAuth 2.0 Client-ID (Typ: Android)
4. SHA-1-Fingerabdruck des Signing-Keys in der Google Cloud Console hinterlegen

### Gradle-Abhängigkeiten (libs.versions.toml):
```toml
[versions]
kotlin = "2.0.21"
compose-bom = "2024.12.01"
hilt = "2.51.1"
room = "2.6.1"
retrofit = "2.11.0"
moshi = "1.15.1"
credentials = "1.5.0-alpha06"
google-drive = "v3-rev20241206-2.0.0"

[libraries]
# Compose
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-animation = { group = "androidx.compose.animation", name = "animation" }
compose-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
compose-navigation = { group = "androidx.navigation", name = "navigation-compose", version = "2.8.5" }

# Hilt
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version = "1.2.0" }

# Room
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

# Network
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-moshi = { group = "com.squareup.retrofit2", name = "converter-moshi", version.ref = "retrofit" }
moshi-kotlin = { group = "com.squareup.moshi", name = "moshi-kotlin", version.ref = "moshi" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version = "4.12.0" }

# Auth & Drive
credentials-play = { group = "androidx.credentials", name = "credentials-play-services-auth", version.ref = "credentials" }
credentials = { group = "androidx.credentials", name = "credentials", version.ref = "credentials" }
google-id = { group = "com.google.android.libraries.identity.googleid", name = "googleid", version = "1.1.1" }
google-drive-api = { group = "com.google.apis", name = "google-api-services-drive", version.ref = "google-drive" }
google-api-client-android = { group = "com.google.api-client", name = "google-api-client-android", version = "2.7.0" }

# Security
security-crypto = { group = "androidx.security", name = "security-crypto", version = "1.1.0-alpha06" }

# Google Fonts für Compose
compose-google-fonts = { group = "androidx.compose.ui", name = "ui-text-google-fonts", version = "1.7.6" }
```

---

## Wichtige Implementierungshinweise

1. **Fehlerbehandlung:** Jeder API-Aufruf (Groq, Gemini, Google Drive) muss robust mit try/catch und Retry-Logik (maximal 3 Versuche mit exponentiellem Backoff) umgeben sein. Bei Fehlern wird dem Nutzer eine verständliche deutsche Fehlermeldung in einem Snackbar angezeigt.

2. **Offline-Fähigkeit:** Die App muss vollständig offline funktionieren (Einträge speichern, Timeline anzeigen, altes Dashboard anzeigen). Nur Transkription, Textverbesserung, Dashboard-Aktualisierung und Sync benötigen Internet. Prüfe die Netzwerkverfügbarkeit vor jedem API-Aufruf.

3. **Performance:** Die Tagebuch-Timeline verwendet LazyColumn mit Keys für optimales Recycling. Bilder und Animationen werden bei Bedarf pausiert, wenn sie nicht sichtbar sind.

4. **Accessibility:** Alle interaktiven Elemente haben contentDescription-Texte auf Deutsch. Mindest-Touch-Target: 48dp. Kontrastverhältnisse einhalten (WCAG AA).

5. **Datenschutz:** Keinerlei Nutzerdaten werden an Server gesendet außer an die konfigurierten APIs (Groq, Gemini, Google Drive). Audio-Dateien werden nach erfolgreicher Transkription sofort gelöscht. API-Keys werden verschlüsselt gespeichert.

6. **Sprache:** Die gesamte App-Oberfläche ist auf Deutsch. Alle Texte, Labels, Fehlermeldungen und Platzhalter auf Deutsch.

7. **Versionierung:** Implementiere eine Datenbank-Migrations-Strategie für Room (fallback: destructive Migration vermeiden, immer Migrations-Pfade definieren).

---

## Schritt-für-Schritt-Umsetzung

Beginne die Implementierung in dieser Reihenfolge:

**Phase 1 – Fundament:**
1. Projekt-Setup (Gradle, Abhängigkeiten, Paketstruktur)
2. Theme und Design-System (Farben, Typografie, GlassCard, ThreeDCard)
3. Navigation mit BottomNavBar (3 Tabs: Tagebuch, Dashboard, Einstellungen)
4. Room-Datenbank mit Entities und DAOs
5. Hilt-Dependency-Injection-Setup

**Phase 2 – Authentifizierung und Sync:**
6. Google Sign-In mit Credential Manager
7. Login-Screen
8. Google Drive Backup/Restore
9. Settings-Screen (API-Keys, Logout, Sync)

**Phase 3 – Kernfunktion Tagebuch:**
10. Audio-Aufnahme (MediaRecorder)
11. Groq-API-Anbindung (Transkription)
12. Gemini-API-Anbindung (Textverbesserung)
13. Aufnahme-Overlay mit Wellenform
14. Vorschau-Dialog (Raw/Improved Toggle)
15. Tagebuch-Timeline mit Glas-Karten
16. Entry-Detail-Screen mit Shared-Element-Transition
17. Suche und Filterung

**Phase 4 – KI-Dashboard:**
18. Gemini-Analyse-Integration (Entropie-Erkennung)
19. Automatische Kategorisierung neuer Einträge
20. Dashboard-UI mit Partikel-Hintergrund
21. Kategorie-Karten mit 3D-Effekt
22. Ratschlags-Karten mit Expand-Effekt
23. Automatische Dashboard-Aktualisierung

**Phase 5 – Polish:**
24. Splash-Screen-Animation
25. Mikrofon-Button-Animationen
26. Alle Übergangs-Animationen
27. Shimmer- und Lade-Effekte
28. Fehlerbehandlung und Snackbars
29. Offline-Modus-Hinweise
30. Finaler UI-Feinschliff und Testen

---

## Zusammenfassung

Diese App – **Entropy Journal** – ist ein persönlicher KI-Assistent zur Identifikation und Reduktion persönlicher Entropie. Sie kombiniert Sprachaufnahme, automatische Transkription, optionale Textverbesserung, eine chronologische Tagebuch-Timeline und ein intelligentes Ratschlags-Dashboard, das alle Einträge ganzheitlich analysiert. Das Design folgt dem „Neon Cosmos"-Thema: dunkel, futuristisch, mit Glasmorphismus, 3D-Effekten und leuchtenden Neon-Akzenten. Alle Daten werden über Google Drive synchronisiert und die App funktioniert auch offline.

Erstelle nun das vollständige Projekt mit allen beschriebenen Dateien, beginnend mit Phase 1.
