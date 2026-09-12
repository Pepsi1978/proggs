# Android — Kotlin / Jetpack Compose (Kreis M1)

## Namensraum

`de.frank.module.<kurzname>` — klein geschrieben, z. B. `de.frank.module.dragreorder`.

Belegt durch den Bestand: App-Code liegt unter `de.frank.<appname>`, gemeinsamer
Code unter `de.frank.kompass` (KompassKern). Der Modul-Namensraum hält sich davon
fern, damit eine Kopie in einer fremden App nie mit deren Paketen kollidiert.

## Ablage

| | Pfad |
|---|---|
| Modul | `Module/Android/<Ordnername>/src/de/frank/module/<kurzname>/` |
| Kopie in der App | `app/src/main/java/de/frank/module/<kurzname>/` |

`<Ordnername>` ist `Mx.y-Anzeigename` in werkzeugfester Fassung, `<kurzname>`
der kleingeschriebene ASCII-Rest ohne Trennzeichen — beides entsteht in Phase 1
aus dem Namen, den der Benutzer vergeben hat.

**Beispiel:** Anzeigename `Drag & Drop Modul` →
Ordner `M1.8-Drag-und-Drop-Modul`, Paket `de.frank.module.dragunddropmodul`.

Der Pfad in der App spiegelt den Namensraum, dadurch bleibt die Datei
byte-identisch und der Ordnerbaum trotzdem sauber. Kotlin verlangt keine
Übereinstimmung von Paket und Ordner, aber Werkzeuge und Menschen lesen sich
leichter, wenn beides zusammenpasst.

### Sonderfall: mehrere Apps teilen sich einen Quellordner

Bindet mehr als eine App denselben Ordner über `sourceSets.srcDir` ein — bei
diesem Benutzer `KompassKern` für ClaudeKompass, CodexKompass und OCodeKompass —
dann liegen Kopie **und** Anbindung dort, nicht je App:

| | Pfad |
|---|---|
| Kopie in der App | `KompassKern/src/main/java/de/frank/module/<kurzname>/` |
| Anbindung | derselbe Ordner, `Anbindung.kt` |

In der Konsumententabelle steht **eine Zeile je App**, alle mit demselben Pfad.
Beim Nachziehen wird dieser eine Pfad einmal überschrieben und danach werden
alle beteiligten Apps gebaut.

Erkennen lässt sich der Fall so:

```bash
grep -rn "srcDir" --include=build.gradle.kts ~/proggs | grep -v "/build/"
```

## Typische Nabelschnüre

| Was | Woran erkennbar | Empfohlener Schnitt |
|---|---|---|
| CompositionLocal der App | `LocalXyz.current` mit App-Import | Parameter (`reducedMotion: Boolean`) |
| Theme-Farben | `MaterialTheme.colorScheme` reicht; `AppFarben.x` nicht | Parameter mit Vorgabewert aus `MaterialTheme` |
| String-Ressourcen | `stringResource(R.string.x)` | Parameter `text: String` — `R` existiert in fremden Apps nicht |
| Drawables / Icons | `R.drawable.x`, `painterResource` | Parameter, oder `Icons.Default.*` aus Compose |
| Hilt / DI | `@HiltViewModel`, `@Inject` | Abhängigkeit als Konstruktorparameter, keine Annotation im Modul |
| Room | `@Entity`, `@Dao`, `AppDatabase` | Schnittstelle im Modul, Umsetzung in der App |
| ViewModel der App | `viewModel<AppViewModel>()` | Zustand und Rückrufe als Parameter |
| Navigation | `navController.navigate("route")` | Rückruf `onXyz: () -> Unit` |

Compose-eigene Importe (`androidx.compose.*`, `kotlinx.coroutines.*`) sind keine
Nabelschnur — die hat jede Compose-App.

## Mindestversion feststellen

```bash
grep -rn "composeBom\|compose-bom\|kotlin(\"android\")\|kotlinCompilerExtensionVersion" app/build.gradle.kts gradle/libs.versions.toml 2>/dev/null
```

Trag die gefundene Compose-BOM- und Kotlin-Version als `Mindestens:` ins Manifest.
Benutzt das Modul eine API, die erst in einer neueren Fassung existiert, bricht
sonst eine ältere App beim Einbau — und zwar erst beim Kompilieren, nicht beim
Kopieren.

## Abnahme

```bash
cd <App-Ordner> && ./gradlew :app:assembleDebug
```

Bei geteiltem Quellordner **jede** beteiligte App bauen — ein Fehler dort trifft
alle gleichzeitig:

```bash
for a in ClaudeKompass CodexKompass OCodeKompass; do
  (cd ~/proggs/$a && ./gradlew :app:assembleDebug) || echo "ROT: $a"
done
```

Grün heißt: Modul steht. Danach Regel 9 für die App — Version bumpen
(`versionName` **und** `VERSION_BUMPED_AT`), committen, pushen,
`adb install -r` auf das angeschlossene Gerät.
