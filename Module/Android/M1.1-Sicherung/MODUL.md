# M1.1 — Sicherung

Sicherung des eigenen Bestands als Datei in einen selbst gewählten Ordner — mit
Auswahl, was gesichert wird, selbsttätiger Sicherung nach Ruhezeit, Vorschau vor
dem Einspielen und Zurücknehmen.

- **Stand:** v1
- **Plattform:** Android (Kotlin, Jetpack Compose)
- **Angelegt:** 12.09.2026 15:11
- **Ordner:** `Module/Android/M1.1-Sicherung/`
- **Kurzname:** `sicherung` — steckt im Namensraum `de.frank.module.sicherung`

## Herkunft

- **App:** KompassKern (der geteilte Kern von ClaudeKompass, CodexKompass, OCodeKompass)
- **Commit:** `bd4850985` — einschließlich `5a67dc8e5` „acht Leistungsbremsen beseitigt"
- **Dateien:**
  - `KompassKern/src/main/java/de/frank/kompass/backup/{AutoSicherung,BackupStatus,DateiSicherung,SicherungsDienst}.kt`
  - `KompassKern/src/main/java/de/frank/kompass/data/Sicherung.kt` (nur der Rahmen)
  - `KompassKern/src/main/java/de/frank/kompass/vm/EinstellungenViewModel.kt` (nur der Sicherungsanteil)

## Was drin ist

| Datei | Was |
|---|---|
| `Vertrag.kt` | Die Schnittstellen zur App — was sie liefern muss |
| `Sicherungsrahmen.kt` | Kopf, Fußzeile, Prüfsumme, Vollständigkeit, satzweises Streamen |
| `DateiSicherung.kt` | Ordner merken (SAF), schreiben, lesen, alte Dateien wegräumen |
| `SicherungsDienst.kt` | Der Ablauf: sichern, zurücklesen, prüfen, einspielen, zurücknehmen |
| `AutoSicherung.kt` | Selbsttätig nach zwei Minuten Ruhe, beim Verlassen sofort |
| `BackupStatus.kt` | Wann zuletzt, geprüft oder gescheitert |
| `SicherungsSteuerung.kt` | Zustand und Aktionen für die Oberfläche — **ohne Compose** |

## Der Schnitt

> **Das Modul besitzt den Umschlag und den Ablauf. Die App besitzt den Inhalt.**

Das Modul kennt keine Tabelle und keinen Datensatz der App. Es schreibt Kopf und
Fußzeile, die App schreibt die benannten Nutzlast-Felder dazwischen. Dadurch
bleibt eine bestehende Sicherungsdatei **Byte für Byte** wie bisher.

**Die Oberfläche zeichnet die App selbst.** Das Modul liefert nur
`SicherungsZustand` und die Aktionen — so sind die Funktionen überall dieselben,
während sich das Aussehen der jeweiligen App anpasst.

## Host muss liefern

| Schnittstelle | Wofür |
|---|---|
| `SicherungsInhalt` | Welche Teile es gibt, wie ein Satz geschrieben und gelesen wird, Produktname, Datenmodell-Fassung, Größenschätzung, Zusammenfassung |
| `UmfangSpeicher` | Wo steht, was angehakt ist und ob selbsttätig gesichert wird |
| `SicherungsNamen` | Dateipräfix, frühere Präfixe, **Name der `SharedPreferences` und des Ordner-Schlüssels** |
| `SicherungsRuecknahme` | Optional. Fehlt sie, entfällt das Zurücknehmen |
| `SicherungsProtokoll` | Optional. Vorbelegt mit `StillesProtokoll` |

> ⚠️ **`SicherungsNamen.einstellungenDatei` und `.ordnerSchluessel` bei einer
> bestehenden App unverändert übernehmen.** Bei Kompass sind das
> `kompass_backup_status` und `sicherungs_ordner`. Ein anderer Name heißt: Der
> Benutzer hat seinen eingestellten Sicherungsordner verloren, ohne Meldung.

## Braucht Module

- —

## Mindestens

- Android mit `androidx.lifecycle` (DefaultLifecycleObserver), `kotlinx.coroutines`
- Für die Oberfläche: keine Vorgabe — das Modul enthält kein Compose

## Konsumenten

| App | Stand | Pfad der Kopie |
|---|---|---|
| ClaudeKompass | v1 | `KompassKern/src/main/java/de/frank/module/sicherung/` |
| CodexKompass | v1 | `KompassKern/src/main/java/de/frank/module/sicherung/` |
| OCodeKompass | v1 | `KompassKern/src/main/java/de/frank/module/sicherung/` |

> **Sonderfall:** Die drei Apps teilen sich `KompassKern` per `sourceSets.srcDir`.
> Die Modulkopie und die Anbindung liegen deshalb **einmal** dort, nicht dreimal
> unter `app/src/`. Beim Nachziehen wird dieser eine Pfad überschrieben und
> danach werden alle drei Apps gebaut.

## Änderungen

Eine Zeile je Version. Ändert sich eine öffentliche Funktion, ein Parameter
oder eine Schnittstelle, **muss** „bricht Anbindung" dabeistehen — daran
erkennt `modul-einbauen`, dass Nachziehen mehr ist als Überschreiben. Bauen
andere Module auf diesem auf, hier ebenfalls nennen.

| Version | Datum | Was | Bricht Anbindung | Commit |
|---|---|---|---|---|
| v1 | 12.09.2026 | aus KompassKern herausgelöst | — | — |
