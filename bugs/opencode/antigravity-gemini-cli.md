# Antigravity CLI (agy) und Gemini CLI mit Google-AI-Abo — Bug-Almanach

> Stand 14.10.2026 · Anker: agy 1.3.5 (Windows x64), @google/gemini-cli 0.64.0 (latest) / 0.65.0-preview.0 ·
> Kurzcheck: `antigravity-gemini-cli-kurzcheck.md` · Best Practices: `best-practices/opencode/antigravity-cli.md`
> Quelle der Recherche: Sonnet-5.5-Schwarm (4 Researcher) + eigene Prüfung am installierten Bundle/Binary.

## #1 Gemini CLI: „This client is no longer supported for Gemini Code Assist for individuals“
- **Symptom:** Nach erfolgreichem „Sign in with Google“ (Token liegt in `~/.gemini/oauth_creds.json`) zeigt die CLI
  den Auth-Dialog erneut mit „Failed to sign in … migrate to the Antigravity suite“. Jeder Start fragt neu.
- **Ursache:** Serverseitige Abschaltung. Seit 18.06.2026 bedienen Gemini CLI und Code-Assist-IDE-Erweiterungen
  Google AI Free/Pro/Ultra (Individuals) nicht mehr (Ankündigung 19.05.2026). Weiter möglich nur mit bezahltem
  API-Key, Vertex AI oder Code Assist Standard/Enterprise (Neuverkauf endet 09.10.2026).
- **Betroffen:** alle Gemini-CLI-Versionen ab dem Stichtag (gemeldet ab 0.49.0, bestätigt 0.64.0).
- **Fix (funktionserhaltend):** Für das Privat-Abo die Antigravity CLI `agy` verwenden (OAuth mit demselben Konto).
- **Quellen:** https://developers.googleblog.com/an-important-update-transitioning-gemini-cli-to-antigravity-cli/ ·
  https://developers.google.com/gemini-code-assist/docs/deprecations/code-assist-individuals ·
  https://github.com/google-gemini/gemini-cli/issues/28229

## #2 Gemini CLI ignoriert System-Settings-Datei („Parent directory is insecure … Get-Acl“)
- **Symptom:** `⚠ Security Warning: Skipping system settings file '…\AppData\Local\Temp\….json': Parent directory
  '…\temp' is insecure: … Der Befehl "Get-Acl" wurde im Modul "Microsoft.PowerShell.Security" gefunden, das Modul
  konnte aber nicht geladen werden.` Alles aus der Datei (z. B. `context.fileName`, `modelConfigs`) fehlt still.
- **Ursache:** Die Datei per `GEMINI_CLI_SYSTEM_SETTINGS_PATH` wird vor dem Laden per ACL geprüft. Die Prüfung ruft
  Windows PowerShell 5.1 auf; die erbt aus pwsh 7 einen `PSModulePath`, dessen 7er-Modulpfade vorne stehen → 5.1
  lädt das inkompatible Security-Modul nicht → Prüfung scheitert → Datei wird übersprungen.
- **Betroffen:** gemini-cli 0.64.0 unter Windows, Start aus pwsh 7 (Store-Installation beobachtet).
- **Fix:** Keine System-Settings in `%TEMP%` ablegen; benötigte Schlüssel in `~/.gemini/settings.json` (User-Settings,
  keine ACL-Prüfung) schreiben. Alternativ vor dem Start den `PSModulePath` auf die 5.1-Standardpfade setzen.
- **Quelle:** eigene Beobachtung 14.10.2026 (OpenLauncher 1.24.84), Code-Stelle `Storage.getSystemSettingsPath` im Bundle.

## #3 `agy` nach Installation nicht gefunden
- **Symptom:** „agy: Die Benennung … wurde nicht erkannt“ in bereits offener PowerShell.
- **Ursache:** Installer ergänzt PATH nur für neue Sitzungen.
- **Fix:** Neue Sitzung oder Vollpfad `%LOCALAPPDATA%\agy\bin\agy.exe`. Quelle: https://github.com/google-antigravity/antigravity-cli/issues/109

## #4 Konsolen-Flackern/Fokusklau bei Start ohne Konsole
- **Symptom:** Wird agy aus einem GUI-Prozess ohne Konsole gestartet, öffnet sich kurz ein Fenster und stiehlt den Fokus;
  Hintergrundbefehle flackern.
- **Betroffen:** agy ≥ 1.1.25 (#1129 offen).
- **Fix:** Immer in einem eigenen Windows-Terminal-Reiter starten.
- **Quellen:** https://github.com/google-antigravity/antigravity-cli/issues/938 · https://github.com/google-antigravity/antigravity-cli/issues/1129

## #5 `--model`/`--effort` ignoriert oder Modellfehler
- **Ursache:** Bis 1.1.10 wurden die Schalter teils ignoriert; vor 1.3.4 brauchte `--model` den Slug mit Effort-Suffix
  (z. B. `gemini-3.8-flash-medium`).
- **Fix:** agy ≥ 1.3.4: Basisname (`gemini-3.8-flash`, `gemini-4-argon`) + `--effort low|medium|high|xhigh|max`.
- **Quelle:** https://github.com/google-antigravity/antigravity-cli/blob/main/CHANGELOG.md

## #6 `--mode auto` fehlt unter Windows
- **Betroffen:** agy 1.3.5 („Windows support coming later“). `--permission-mode` ist ungültig.
- **Fix:** `--dangerously-skip-permissions` oder dauerhaft `"toolPermission": "always-proceed"` in
  `~/.gemini/antigravity-cli/settings.json`. Quelle: https://antigravity.google/docs/cli/modes

## #7 Modell im Abo nicht verfügbar
- **Ursache:** Angebot hängt am Plan (https://antigravity.google/docs/models, 14.10.2026): Free/Plus/Pro → Gemini 3.8 Flash
  (+ Claude/GPT-OSS); Ultra zusätzlich Gemini 4 Argon; Gemini 3.1 Pro, 3.6/3.7 Flash nur Enterprise.
- **Fix:** `agy models` (nach Login) zeigt das tatsächliche Angebot; Launcher-Liste danach ausrichten.

## #8 Installer aktualisiert nicht
- **Symptom:** `install.ps1` bricht bei vorhandener `agy.exe` mit „already installed“ ab.
- **Fix:** `agy update` (agy aktualisiert sich sonst im Hintergrund selbst).
