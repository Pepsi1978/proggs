# Entropy Journal — Learnings & Best Practices

## Scroll-Pattern: LazyRow statt Row+horizontalScroll
- `Row + Modifier.horizontalScroll()` funktioniert NICHT zuverlaessig innerhalb von `LazyColumn`
- **Loesung**: `LazyRow` mit `contentPadding` und `Arrangement.spacedBy()`
- KEINE Wrapper-Composables mit eigenen Gesten (`pointerInput`, `detectDragGestures`) um klickbare Items in scrollbaren Listen
- `Modifier.clickable` direkt auf der Card — kollidiert nicht mit LazyRow-Scroll

## Theme-Toggle: Separates SharedPreferences fuer Quick-Toggle
- `EncryptedSharedPreferences` kann nicht direkt aus Composables erstellt werden (Key-Mismatch mit Hilt-Instanz)
- **Loesung**: Ein einfaches `SharedPreferences("entropy_theme_quick")` fuer den Quick-Toggle
- `MainActivity` hoert auf BEIDE: encrypted (Einstellungen) + quick (Icon-Klicks)
- Quick-Toggle schreibt zurueck in encrypted Prefs damit Einstellungen synchron bleiben

## Google Drive Sync
- `GoogleAccountCredential.usingOAuth2()` funktioniert NICHT mit Credential Manager Sign-In
- **Loesung**: `GoogleAuthUtil.getToken()` mit `Account(email, "com.google")` holt OAuth-Token direkt
- `UserRecoverableAuthException` muss abgefangen werden — enthaelt Intent fuer Consent-Dialog
- Beim Restore: WAL + SHM Dateien loeschen VOR dem Download, sonst nutzt Room die alten Daten
- Nach Restore/Sign-Out: `Runtime.getRuntime().exit(0)` + Activity-Relaunch um Room-Cache zu leeren
- API-Keys vor Sign-Out sichern und danach wiederherstellen (gehoeren zum Geraet, nicht zum Konto)

## Hilt/Dagger
- `AppDatabase` NICHT direkt in UseCase injizieren wenn Hilt es nicht als Provider kennt → `NoSuchMethodError`
- Stattdessen `context.getDatabasePath()` verwenden
- Nach Hilt-Aenderungen: `./gradlew clean assembleDebug` um generierte Klassen neu zu erstellen

## Shimmer/Ladebalken
- Halbtransparente Highlight-Farben erzeugen sichtbare "Schnitt"-Artefakte im Gradient
- **Loesung**: `lerp(baseColor, Color.White, 0.35f)` fuer opaken Highlight ohne Transparenz-Probleme
- `colorStops` mit expliziten Positionen fuer kontrollierten Verlauf
- 35% White-Blend im Dunkelmodus, 15% Black-Blend im Hellmodus

## Kategorie-Icons
- 50+ Icon-Mappings fuer alle Lebensbereiche (Gesundheit, Psyche, Arbeit, Finanzen, Freizeit, Sucht, Spiritualitaet)
- Deutsche Aliase unterstuetzen: KI kann "schlaf", "arbeit", "finanzen" als Icon-Namen verwenden
- `else -> Icons.Rounded.Lightbulb` als Fallback fuer unbekannte Icons
- Karten: 110x100dp mit 10sp Schrift und maxLines=3 fuer lange Kategorienamen
- `fillMaxSize()` auf Column innerhalb der Card fuer korrekte Zentrierung

## Gemini API
- Modell dynamisch via `@Path("model")` in Retrofit: `@POST("models/{model}:generateContent")`
- Modellauswahl in SharedPreferences gespeichert, von ImproveTextUseCase UND AdviceRepository gelesen
- Prompt fuer Analyse: Eintraege nummerieren ("EINTRAG 1 von 5") damit KI keinen uebersieht
- Frische Analyse (manuell) OHNE vorherigen Kontext senden — sonst produziert KI identischen Text
- Textverbesserung: Tampermonkey-Style Intention-Prompt statt reiner Grammatik-Korrektur

## Sonnenauf-/untergang
- NOAA Solar Position Algorithmus — reine Mathematik, keine API noetig
- `ACCESS_COARSE_LOCATION` reicht aus (keine GPS-Genauigkeit noetig)
- Standort wird einmal geholt und in SharedPreferences gespeichert fuer Offline-Berechnung
- Drei Theme-Modi sind gegenseitig exklusiv: Manuell / System / Sonne
