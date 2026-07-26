# ⚠️ TODO auf JEDEM Rechner: lokalen Branch `master` → `main` umbenennen

**Warum:** Das Repo `proggs` nutzt als Standard-Branch `main`. Auf einzelnen Rechnern
hieß der lokale Arbeitsbranch historisch `master` (trackte aber `origin/main`). Jedes
`git push origin master` erzeugt dann auf GitHub einen abweichenden Branch `origin/master`,
der von `main` divergiert. Am 13.07.2026 wurde das auf dem Windows-Hauptrechner bereinigt
und `origin/master` gelöscht — aber **jeder andere Rechner, dessen lokaler Branch noch
`master` heißt, bringt `origin/master` beim nächsten Push zurück.**

Siehe Hintergrund: `OpenLauncher/Profiles/ClaudeCode/minimal/projects/C--Users-barwa-proggs/memory/repo-branch-und-sync-setup.md`

## Schritte (auf jedem Rechner einmal ausführen)

```bash
cd <pfad-zum-proggs-klon>

# 1. Wie heißt der lokale Branch?
git branch --show-current
#    -> Wenn "main": nichts zu tun, fertig. Wenn "master": weiter mit Schritt 2.

# 2. Umbenennen + Upstream auf origin/main setzen
git fetch origin
git branch -m master main
git branch --set-upstream-to=origin/main main

# 3. Kontrolle: sollte "main ... [origin/main]" zeigen
git branch -vv

# 4. Künftig NICHT mehr "git push origin master" verwenden.
#    Einfach "git push" (geht automatisch nach origin/main).
```

## Wenn `origin/master` doch wieder auftaucht

```bash
# Sicherstellen, dass nichts Exklusives drin ist (muss 0 sein):
git rev-list --count origin/main..origin/master
# Dann löschen:
git push origin --delete master
```

## Nachdem ALLE Rechner umgestellt sind

Diese Datei löschen und den Lösch-Commit pushen — dann ist die Migration abgeschlossen.

---
_Erstellt am 13.07.2026. Übergib diese Datei auf jedem Rechner deiner CLI (Claude Code /
OpenCode / Gemini) mit „führe die Schritte in MIGRATION-master-nach-main.md aus", oder
arbeite sie manuell ab._
