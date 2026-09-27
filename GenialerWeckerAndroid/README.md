# Genialer Wecker (Android, Verkaufs-App)

Eigenständige Wecker-App für Google Play. Sie läuft parallel zur privaten App `GenialerWecker`
(anderes Paket: `de.frank.genialerwecker.app`) und teilt keine Daten, Konten oder Schlüssel mit ihr.

- Wecken komplett offline: Weckton, eigene Musik, vorgelesener Text, Foto-Aufgabe, Schlummern.
- Vorlesen ausschließlich mit einer auf dem Gerät installierten Android-Stimme (keine Cloud-Stimmen,
  keine INTERNET-Berechtigung). Die Ansage wird vor dem Wecken als Datei vorbereitet; fehlt eine
  deutsche Offline-Stimme, zeigt die App das an und bietet den Download der Sprachdaten an.
- Vier Designs: Schlicht, Morgenruhe, Traumraum, Orbit.

## Bauen und Signieren

Debug- und Release-Builds signieren mit dem eigenen Schlüssel unter `~/SK/GenialerWeckerAndroid`
(`keystore.properties` + `release.keystore`, nie im Git). Fehlt er, bricht der Build ab. Siehe
`best-practices/android/debug-signing.md` (Ausnahme 2).

```
./gradlew :app:assembleRelease :app:testDebugUnitTest
```

Version: nur über `app/src/main/assets/versionslog.json` (neuester Eintrag unten).
