# M1.1 — Sicherung

Sicherung des eigenen Bestands als Datei in einen selbst gewählten Ordner — mit
Auswahl, was gesichert wird, selbsttätiger Sicherung nach Ruhezeit, Vorschau vor
dem Einspielen und Zurücknehmen.

- **Stand:** v2
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

## Der Schnitt

> **Das Modul besitzt den Umschlag und den Ablauf. Die App besitzt den Inhalt.**

Das Modul kennt keine Tabelle und keinen Datensatz der App. Es schreibt Kopf und
Fußzeile, die App schreibt die benannten Nutzlast-Felder dazwischen. Dadurch
bleibt eine bestehende Sicherungsdatei **Byte für Byte** wie bisher.

**Die Oberfläche und die Knopflogik liegen in der App.** Das Modul bietet den
`SicherungsDienst` an — sichern, zurücklesen, prüfen, einspielen, zurücknehmen —
und lässt offen, wie das bedient und gezeichnet wird. So sind die Funktionen
überall dieselben, während sich das Aussehen der jeweiligen App anpasst.

Als Vorlage für die Knopflogik dient der Sicherungsanteil von
`KompassKern/src/main/java/de/frank/kompass/vm/EinstellungenViewModel.kt`
(Zeilen ~550–900) — er läuft in drei Apps. Siehe `BEISPIEL.md`.

## Host muss liefern

| Schnittstelle | Wofür |
|---|---|
| `SicherungsInhalt` | Welche Teile es gibt, wie ein Satz geschrieben und gelesen wird, Produktname, Datenmodell-Fassung, Größenschätzung, Zusammenfassung |
| `SicherungsNamen` | Dateipräfix, frühere Präfixe, **Name der `SharedPreferences` und des Ordner-Schlüssels** |
| `umfangGeber` | Rückruf: was gerade angehakt ist. Vorbelegt mit „alles" |
| `SicherungsRuecknahme` | Optional. Fehlt sie, entfällt das Zurücknehmen |
| `SicherungsProtokoll` | Optional. Vorbelegt mit `StillesProtokoll` |

> ⚠️ **`SicherungsNamen.einstellungenDatei` und `.ordnerSchluessel` bei einer
> bestehenden App unverändert übernehmen.** Bei Kompass sind das
> `kompass_backup_status` und `sicherungs_ordner`. Ein anderer Name heißt: Der
> Benutzer hat seinen eingestellten Sicherungsordner verloren, ohne Meldung.

## Braucht Module

- —

## Gespeicherter Zustand

Diese Namen werden in einer **bestehenden** App unverändert übernommen — ein
anderer Name heißt: Der Benutzer hat seine Einstellung verloren, ohne Meldung.

| Was | Bei Kompass |
|---|---|
| `SharedPreferences`-Datei | `kompass_backup_status` |
| Schlüssel des Sicherungsordners | `sicherungs_ordner` |
| Dateiformat | JSON mit Kopf, benannten Nutzlast-Feldern, Prüfsumme und Fußzeile |
| Dateiname | `<dateiPraefix>-JJJJ-MM-TT-HHMMSS.json`, frühere Präfixe werden mitgelesen |

## Mindestens

- Android mit `androidx.lifecycle` (DefaultLifecycleObserver), `kotlinx.coroutines`
- Für die Oberfläche: keine Vorgabe — das Modul enthält kein Compose

## Konsumenten

| App | Stand | Pfad der Kopie |
|---|---|---|
| ClaudeKompass | v2 | `KompassKern/src/main/java/de/frank/module/sicherung/` |
| CodexKompass | v2 | `KompassKern/src/main/java/de/frank/module/sicherung/` |
| OCodeKompass | v2 | `KompassKern/src/main/java/de/frank/module/sicherung/` |

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
| v2 | 12.09.2026 | `SicherungsSteuerung` entfernt — kein Konsument hat sie je aufgerufen. Mit ihr fallen `SicherungsZustand`, `SicherungsEintrag`, `UmfangSpeicher`, `BackupStatus.formatiere` und `SicherungsDienst.kannZurueckNehmen`. Die Knopflogik liegt bewiesen im `EinstellungenViewModel` und wird beim zweiten Konsumenten von dort gehoben. | — (nichts davon wurde benutzt) | `b757cbe6d` |
