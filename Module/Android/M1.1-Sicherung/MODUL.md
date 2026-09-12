# M1.1 — Sicherung

Sicherung des eigenen Bestands als Datei in einen selbst gewählten Ordner — mit
Auswahl, was gesichert wird, selbsttätiger Sicherung nach Ruhezeit, Vorschau vor
dem Einspielen und Zurücknehmen.

- **Stand:** v4
- **Plattform:** Android (Kotlin, Jetpack Compose)
- **Angelegt:** 12.09.2026 15:11
- **Zuletzt geändert:** 12.09.2026 21:15
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
| `AutoSicherung.kt` | Selbsttätig nach zwei Minuten Ruhe, spätestens nach zehn, beim Verlassen sofort, beim nächsten Start nachgeholt |
| `BackupStatus.kt` | Wann zuletzt, geprüft oder gescheitert, und ob noch etwas aussteht |

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
| `kopfAliase` | Optional. Früher anders benannte Kopf-Felder dieser App, alter Name → heutiger (`"schemaVersion" to "schema"`). Ohne das gilt eine vor dem Modul geschriebene Datei als „ohne Schema-Angabe" und wird abgelehnt |
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
| `SharedPreferences`-Datei (Ordner **und** Stand) | `kompass_backup_status` |
| Schlüssel des Sicherungsordners | `sicherungs_ordner` |
| Dateiformat | JSON mit Kopf, benannten Nutzlast-Feldern, Prüfsumme und Fußzeile |
| Dateiname | `<dateiPraefix>-JJJJ-MM-TT-HHMMSS.json`, frühere Präfixe werden mitgelesen |

## Mindestens

- Android mit `androidx.lifecycle` (DefaultLifecycleObserver), `kotlinx.coroutines`
- Für die Oberfläche: keine Vorgabe — das Modul enthält kein Compose

## Konsumenten

| App | Stand | Pfad der Kopie |
|---|---|---|
| ClaudeKompass | v3 | `KompassKern/src/main/java/de/frank/module/sicherung/` |
| CodexKompass | v3 | `KompassKern/src/main/java/de/frank/module/sicherung/` |
| OCodeKompass | v3 | `KompassKern/src/main/java/de/frank/module/sicherung/` |
| GenialeIdeen | **v4** | `GenialeIdeen/app/src/main/java/de/frank/module/sicherung/` |
| Gedankenspeicher | v3 | `Gedankenspeicher/app/src/main/java/de/frank/module/sicherung/` |

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
| v2 | 12.09.2026 | `SicherungsSteuerung` entfernt — kein Konsument hat sie je aufgerufen. Mit ihr fallen `SicherungsZustand`, `SicherungsEintrag`, `UmfangSpeicher`, `BackupStatus.formatiere` und `SicherungsDienst.kannZurueckNehmen`. Die Knopflogik liegt bewiesen im `EinstellungenViewModel` und wird beim zweiten Konsumenten von dort gehoben. | — (nichts davon wurde benutzt) | `11aa92a90` |
| v3 | 12.09.2026 | `BackupStatus` nimmt die `SharedPreferences`-Datei aus `SicherungsNamen`, statt `kompass_backup_status` fest verdrahtet zu haben — eine übersehene Nabelschnur zur Ursprungs-App. Neu: `SicherungsInhalt.kopfAliase`, damit eine App ihre früher anders benannten Kopf-Felder abbilden kann und vor dem Modul geschriebene Sicherungen einspielbar bleiben. | — (beides nach aussen unverändert; `BackupStatus` wird nur vom `SicherungsDienst` benutzt, `kopfAliase` hat einen Vorgabewert) | — |
| v4 | 12.09.2026 | **Drei Fehler in der selbsttätigen Sicherung.** (1) Der offene Stand lag nur im Arbeitsspeicher: Beendete Android den Vorgang, bevor die Sicherung beim Verlassen durch war, war die Änderung endgültig ungesichert — belegt im Protokoll von Geniale Ideen (21:07:38 Statuswechsel, 21:08:25 Neustart, keine Sicherung dazwischen). Jetzt steht er in der Ablage und wird in `onStart` nachgeholt. (2) `onStop` brach den wartenden Auftrag ab, auch wenn der gerade mitten im Schreiben steckte — die halbe Datei wurde gelöscht und von vorn begonnen, ausgerechnet kurz vor dem Einfrieren des Vorgangs. (3) Der angezeigte Stand wurde nur nach „Jetzt sichern" nachgeführt; nach einer selbsttätigen Sicherung stand dort weiter die Uhrzeit von vorhin, die lebende Sicherung sah aus wie eine tote. Neu dafür `SicherungsDienst.standFluss`/`geprueftFluss`. Ausserdem: `SPAETESTENS_MS` (10 min) — wer ununterbrochen arbeitet, setzte die Ruhezeit sonst beliebig lange zurück. | — (nur Ergänzungen: `standFluss`, `geprueftFluss`, `merkeOffen`, `istOffen`; Konstruktor und alle bisherigen Aufrufe unverändert) | — |
