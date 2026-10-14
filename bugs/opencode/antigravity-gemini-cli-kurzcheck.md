# Antigravity CLI / Gemini CLI Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Trifft ein Punkt zu oder tritt ein Fehler auf → VOLLTEXT
> `antigravity-gemini-cli.md` lesen. Stand 14.10.2026, Anker: agy 1.3.5 · gemini-cli 0.64.0 / 0.65.0-preview.0.

## ⚡ Kurzcheck

| # | Signal | Sofort-Regel | Volltext |
|---|--------|--------------|----------|
| 1 | Gemini CLI: „This client is no longer supported for Gemini Code Assist for individuals … migrate to Antigravity“, Anmeldung bei jedem Start erneut | Kein lokaler Fehler: Login with Google für Privat-Abos (Free/Pro/Ultra) ist seit 18.06.2026 serverseitig abgeschaltet. **Antigravity CLI (`agy`) nehmen.** Neuinstallation/`~/.gemini` löschen hilft nicht. | #1 |
| 2 | Gemini CLI: „Skipping system settings file … Parent directory is insecure … Get-Acl … Modul konnte nicht geladen werden“ | ACL-Prüfung startet Windows PowerShell 5.1 mit geerbtem pwsh-7-`PSModulePath` → Security-Modul lädt nicht → Datei wird **still ignoriert**. Keine System-Settings in `%TEMP%`; Einstellungen in `~/.gemini/settings.json`. | #2 |
| 3 | `agy` nach Installation „nicht erkannt“ | PATH gilt erst in neuen Sitzungen. Vollpfad `%LOCALAPPDATA%\agy\bin\agy.exe`. | #3 |
| 4 | agy aus GUI-Prozess gestartet: Konsolenfenster flackert, Fokus weg | Immer in eigenem Terminal (wt.exe) starten (#938/#1129). | #4 |
| 5 | `--model` wird ignoriert / Fehler ohne Effort-Suffix | Mindestens agy 1.3.4 (Basisname + `--effort`). `agy update`. | #5 |
| 6 | `--mode auto` geht unter Windows nicht | In 1.3.5 nur Linux/macOS. `--dangerously-skip-permissions` nehmen. | #6 |
| 7 | Modell „nicht verfügbar“ in agy | Plan-abhängig: Argon nur AI Ultra; 3.1 Pro, 3.6/3.7 Flash nur Enterprise. `agy models` zeigt das echte Angebot. | #7 |
| 8 | install.ps1 meldet nur „already installed“ | Installer aktualisiert nicht → `agy update`. | #8 |
