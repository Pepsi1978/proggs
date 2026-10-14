# Antigravity CLI (agy) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Für Details → `antigravity-cli.md`. Stand 14.10.2026, Anker: agy 1.3.5.

| # | Regel |
|---|-------|
| 1 | Gemini mit privatem Google-AI-Abo (Free/Pro/Ultra) im Terminal **nur über die Antigravity CLI `agy`** — die Gemini CLI nimmt Privat-Abos seit 18.06.2026 nicht mehr an (nur noch API-Key/Enterprise). |
| 2 | Installation Windows: `irm https://antigravity.google/cli/install.ps1 \| iex` → `%LOCALAPPDATA%\agy\bin\agy.exe`. Kein npm, kein offizielles winget. Update: `agy update`. |
| 3 | Start: `agy --model <basisname> --effort <low\|medium\|high\|xhigh\|max> --dangerously-skip-permissions`. Basisname ohne Effort-Suffix erst ab 1.3.4. |
| 4 | Modelle nach Plan: Pro → `gemini-3.8-flash`; Ultra zusätzlich `gemini-4-argon`; 3.1 Pro / 3.6 / 3.7 Flash nur Enterprise. Echtes Angebot: `agy models` (nach Login). |
| 5 | Regeln: agy liest `AGENTS.md` **und** `GEMINI.md` (Projekt + `~/.gemini/`). Eine Profil-AGENTS.md reicht für Codex, Kimi und agy. |
| 6 | Konfig: `~/.gemini/antigravity-cli/settings.json` (getrennt von Gemini-CLI-`~/.gemini/settings.json`). Kein Env-Override für das Konfig-Verzeichnis. |
| 7 | Anmeldung: Browser-OAuth beim ersten Start, Token im Windows-Anmeldeinformationsspeicher (nicht als Datei). |
| 8 | Aus GUI-Programmen immer in eigenem Terminal-Reiter starten (Fokusklau-Bug). |
