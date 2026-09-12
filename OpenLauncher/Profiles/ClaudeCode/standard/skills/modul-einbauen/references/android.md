# Einbau auf Android — Kotlin / Jetpack Compose (M1.x)

## Wohin die Dateien kommen

```
app/src/main/java/de/frank/module/<kurzname>/
├── <Modul>.kt       ← byte-identische Kopie
└── Anbindung.kt     ← app-eigen
```

Gradle findet beides ohne Zutun — kein Eintrag in `build.gradle.kts` nötig,
kein `sourceSets`, kein `srcDir`. Das ist der Vorteil der Kopie-Strategie.

Das Paket in der Kopie bleibt `de.frank.module.<kurzname>`, obwohl die App
`de.frank.<appname>` heißt. Kotlin verlangt keine Übereinstimmung, und nur so
bleibt die Datei byte-identisch. Die Anbindung benutzt dasselbe Paket.

## Mindestversion prüfen (Phase 1)

```
grep -rn "composeBom\|compose-bom\|kotlin" app/build.gradle.kts gradle/libs.versions.toml
```

Gegen `Mindestens:` aus `MODUL.md` halten. Liegt die App darunter, melden statt
kopieren — der Build bricht sonst mit einer Meldung über eine unbekannte
Funktion, und die Ursache sucht man an der falschen Stelle.

## Aussehen aus der Ziel-App holen

In die Anbindung, nie ins Modul:

| Was | Woher |
|---|---|
| Farben | `MaterialTheme.colorScheme.*` der Ziel-App |
| Schrift | `MaterialTheme.typography.*` |
| Radien | `MaterialTheme.shapes.*` |
| Abstände | die Abstandswerte der Ziel-App, sonst `8/12/16.dp` |
| Texte | `stringResource(R.string.…)` der Ziel-App |
| Bewegung reduziert | das CompositionLocal bzw. die Einstellung der Ziel-App |

## Häufiger Stolperstein

Benutzt die App noch Material 2 (`androidx.compose.material`) und das Modul
Material 3 (`androidx.compose.material3`), fehlt `MaterialTheme.colorScheme`.
Dann in Phase 1 melden: Entweder die App zieht nach, oder die Anbindung setzt
feste Werte. Das ist eine Entscheidung des Benutzers, keine stille Reparatur.

## Abnahme

```
diff -r Module/Android/<Ordner>/src/de/frank/module/<kurzname> app/src/main/java/de/frank/module/<kurzname> --exclude=Anbindung.kt
```

Muss leer sein. Danach die App im Debug-Profil über den Gradle-Wrapper
durchbauen (`:app:assembleDebug`), dann Regel 9: `versionName` **und**
`VERSION_BUMPED_AT` bumpen, committen, pushen, `adb install -r`.
