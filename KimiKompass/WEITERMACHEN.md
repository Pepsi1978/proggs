# KimiKompass – hier weitermachen

Stand: 30.09.2026. Die App ist **angefangen, aber noch nicht baubar**. Ziel: 1:1 wie ClaudeKompass,
nur für **Kimi Code CLI** (installiert: 2.1.1, `C:\Users\barwa\.kimi-code\bin\kimi.exe`).
Reiter unten: Slash-Befehle · Config · /config (Settings-Panel) · Best Practices · Chat.

## Was schon erledigt ist

- Der Ordner ist eine Kopie von ClaudeKompass (0.6.15, mit dem neuen Panel-Reiter), ohne `build/`,
  `.gradle/`, `.kotlin/`, `local.properties`, `debug-shared.keystore` (Regel 15: kein eigener Key)
  und ohne die Claude-Datenskripte.
- Paketordner umbenannt: `app/src/main/java/de/frank/kimikompass/` (MainActivity) und
  `app/src/test/java/de/frank/kimikompass/`. **Der Inhalt der Dateien heisst aber noch
  `claudekompass`** (siehe Schritt 1).
- Wissensbasis fertig in `app/src/main/assets/` (aus dem Binary extrahiert, deutsch erklärt):
  - `slash_befehle.json` – 71 Einträge (43 Eingebaut, 19 Alias, 7 Mitgelieferter Skill, 2 Muster)
  - `config_einstellungen.json` – 129 (72 config.toml, 13 tui.toml, 44 Umgebungsvariable)
  - `panel_eintraege.json` – 29 (Settings-Panel: Model, Permission, Theme, TUI mode, Mermaid
    diagrams, Editor, Feedback survey, Experiments, Automatic updates, Usage)
  - `best_practices.json` – 12
  - `seit`/`seitBeleg` sind überall leer (im Binary nicht belegt). Fünf veraltete Einträge stehen
    mit „Veraltet:“ in `kurz` und Nachfolger in `ersatz`.
- Generator-Skripte liegen in `tools/` (`gen_slash.py`, `gen_config.py`, `gen_panel_bp.py`).
- `versionslog.json` ist noch der von ClaudeKompass → muss ersetzt werden.

## Schritte bis zur fertigen App

1. **Umbenennen** `claudekompass` → `kimikompass` in: `app/build.gradle.kts` (namespace,
   applicationId), `MainActivity.kt` (package), `AppProfil.kt` (BuildConfig-Import), alle Tests
   unter `app/src/test/.../kimikompass/`. `settings.gradle.kts`: `rootProject.name = "KimiKompass"`.
   `res/values/strings.xml`: App-Name „Kimi Kompass“. Kommentar in `build.gradle.kts` anpassen.
   `SEEDED_CLI_VERSION` = `"2.1.1"`.
2. **AppProfil.kt**: PRODUKT „Kimi Kompass“, WERKZEUG „Kimi Code CLI“, KURZNAME „Kimi“,
   HERSTELLER „Moonshot AI“, DB_NAME `kimi-kompass.db`, OFFENE_ABLAGE `kimi_kompass_prefs`,
   GEHEIME_ABLAGE `kimi_kompass_secure_prefs`, DATEI_PRAEFIX `kimi-kompass`,
   PANEL_TITEL „/config“, PANEL_TITEL_LANG „Settings-Panel“, PANEL_ART_NAME „Punkt aus dem
   Settings-Panel (/settings bzw. /config)“, PANEL_STAND „2.1.1“.
3. **versionslog.json neu**: `"app": "KimiKompass"`, ein Eintrag
   `{ "versionCode": 1, "versionName": "0.1.0", "stand": "<Get-Date>", "notiz": "Erste Fassung: Kimi Code CLI erklärt" }`.
4. **Launcher-Icon/Farbe** ggf. anpassen (res/mipmap, `ic_launcher_background.xml`), damit sie
   sich auf dem Handy von Claude Kompass unterscheidet.
5. **Aktualisierer auf Kimi umbauen** (`app/src/main/java/de/frank/kompass/update/`):
   - `DokuAbruf.kt` URLs (alle am 30.09.2026 mit 200 geprüft):
     - Version: `https://code.kimi.com/kimi-code/latest.json` → `{"version":"2.1.1",…}`
       (global: `code.kimi.ai`)
     - Befehle: `https://raw.githubusercontent.com/MoonshotAI/kimi-code/main/docs/en/reference/slash-commands.md`
     - Config: `…/docs/en/configuration/config-files.md`
     - Variablen: `…/docs/en/configuration/env-vars.md`
     - Änderungsprotokoll: `…/docs/en/release-notes/changelog.md`
       (technisch ausführlich: `…/main/apps/kimi-code/CHANGELOG.md`)
   - **Formate** (lokale Kopien im alten Scratchpad sind weg, einfach neu laden):
     - Befehle: mehrere Tabellen `| Command | Alias | Description | Always available |`, Zelle
       z. B. `` `/title [<text>]` `` → Name ohne Argumente; Alias-Spalte `` `/rename` `` oder `—`.
     - Config: Tabellen `| Field | Type | Default | Description |` je Abschnitt; Unterschlüssel
       stehen unter Überschriften (`## loop_control` o. ä.) → Präfix aus der Überschrift bilden.
       Seed-Namen sind snake_case mit Punkt, z. B. `loop_control.max_retries_per_step`.
     - Variablen: Überschriften `` ### `KIMI_CODE_HOME` `` plus Tabelle `| Key | Applicable provider | Default |`.
     - Änderungsprotokoll: `## 2.1.1 (2026-09-24)` – **mit Datum in Klammern**. Die
       Claude-Regex `^##\s+(x.y.z)\s*$` greift deshalb NICHT; in `DokuParser.leseNeuesteVersion`,
       `findeEinzug` und `ProtokollErnte.versionsZeile` auf `^##\s+([0-9]+\.[0-9]+\.[0-9]+)(?:\s+\(.*\))?\s*$` ändern.
       Zeilen beginnen mit „Add …“, nicht „Added …“.
   - `ProtokollErnte`: Slash/Settings/Variablen-Ernte ist Claude-spezifisch (`CLAUDE_`-Vorsilben,
     „Added“) → im Aktualisierer weglassen (leere Listen, Sonde entfernen). Die Panel-Ernte
     `leseConfigMenue` umstellen: Zeilen mit `` `/settings` `` oder `/config`, Namen aus
     „the X setting in `/settings`“ bzw. aus Anführungszeichen.
   - **Wichtig – falsche „Entfernt“-Meldungen vermeiden:** Beim ersten Lauf gilt jeder
     Seed-Eintrag, den der Parser nicht findet, als entfernt. Deshalb: Verschwunden-Regel nur für
     `art` „Eingebaut“ und „Alias“ im Slash-Bereich; Config und Panel nur ergänzen (add-only, so
     wie der Panel-Block in ClaudeKompass). Vorher einmal die geparsten Namen gegen die Seed-Namen
     abgleichen (kleines Python-Skript) und Abweichungen im Seed angleichen.
   - `MINDEST_SLASH` ≈ 25, `MINDEST_EINSTELLUNGEN` ≈ 30, `MINDEST_VARIABLEN` ≈ 10.
   - Tests `DokuParserTest`/`ProtokollErnteTest` auf Kimi-Beispiele umschreiben oder löschen.
6. **README.md** auf Kimi umschreiben (Aufbau wie ClaudeKompass).
7. **Abschluss (Regel 9, 19, 20):** `./gradlew :app:assembleDebug`, Versionslog, commit nur mit
   Pfad `KimiKompass/`, push, per WLAN installieren (`adb -s 192.168.0.155:5555 install -r …`),
   dann Skill `apk-update` mit `-Projekt KimiKompass` (erste Veröffentlichung legt den Ordner an;
   ggf. Eintrag in `projekte.json` nötig, falls der Skill das Paket nicht selbst findet).

## Stolperfalle beim Bauen (heute dreimal passiert)

Der erste Release-Build nach einer Änderung schlug fehl, obwohl der Code stimmte: gesperrtes
`classes.dex` bzw. „Unresolved reference“ auf vorhandene Klassen. Lösung:
`./gradlew --stop`, `app/build/kotlin/compileReleaseKotlin` und
`app/build/tmp/kotlin-classes/release` löschen, dann neu bauen.
