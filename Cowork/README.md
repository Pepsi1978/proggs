# Cowork

Ablage-Ordner für Ergebnisse aus der **Claude-Cowork-Sandbox**.

## Wozu dieser Ordner

Claude Cowork arbeitet in einer Sandbox, die mit dem `proggs`-Repo verbunden ist
und direkt committen + pushen kann (siehe `../scripts/COWORK-PUSH-SETUP.md`). Alles,
was in einer Cowork-Session entsteht und ins Repo soll, wird hier abgelegt.

## Warum dieser Ordner eine Datei braucht

Git lädt **keine leeren Ordner** hoch — es versioniert nur Dateien. Ein komplett
leerer `Cowork/`-Ordner würde auf GitHub deshalb nie erscheinen. Diese `README.md`
ist die Platzhalter-Datei, die den Ordner dauerhaft im Repo und auf GitHub sichtbar
hält. Sobald weitere Dateien hinzukommen, werden sie ganz normal mit committet und
gepusht (der Ordner ist **nicht** in `.gitignore` ausgeschlossen).

## Verwandtes

- Push-Setup aus der Sandbox: `../scripts/COWORK-PUSH-SETUP.md`
- Commit-Skript: `../scripts/cowork-commit.sh`
- Bug-Almanach: `../bugs/claude-tooling/cowork.md`
- Best Practices: `../best-practices/projekt-code/claude-tooling/best-practices-cowork.md`
