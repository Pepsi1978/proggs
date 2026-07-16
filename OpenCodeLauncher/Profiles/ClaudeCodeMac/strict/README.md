# macOS · Strikt-Profil (Geruest)

Wie das macOS-Standard-Profil (versionierte `skills/ rules/ agents/ commands/`), plus spaeter die
Hook-Aktivierung mit **macOS-Pfaden**.

Auf macOS zu befuellen:
- Inhalte wie beim Standard-Profil.
- `settings.json` mit Hook-Konfiguration, die auf die macOS-Hooks zeigt (z. B. `bash`/`zsh` unter
  `/Users/<name>/.claude/hooks/...` oder — voll autark — in einen `hooks/`-Ordner hier).
- `GITHUB_PERSONAL_ACCESS_TOKEN` und andere Secrets NIEMALS ins Repo (bleiben lokal, per .gitignore).
