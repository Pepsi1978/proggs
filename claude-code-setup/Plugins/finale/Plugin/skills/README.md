# skills/

Dieser Ordner enthält **nur Symlinks** zu den vier echten Skills die in
`~/.claude/skills/` leben. Die Skills selbst liegen im Bundle separat unter
`../../Skills/` (grosses S) — von dort werden sie beim Einspielen nach
`~/.claude/skills/` kopiert.

## Erwartete Symlinks

| Symlink-Name (hier) | Erwartetes Ziel (~/.claude/skills/) |
|---------------------|--------------------------------------|
| `roentgen-skill`         | `app-roentgen`         |
| `rechtssicherheits-skill`| `rechtssicherheit`     |
| `strings-skill`          | `string-extraktor`     |
| `uebersetzer-skill`      | `übersetzung`          |

## Wenn die Symlinks fehlen

**Bug-Klasse:** Git auf Windows tracked Symlinks unzuverlaessig (haengt vom
`core.symlinks`-Setting + Developer-Mode ab). Beim Klonen / nach Plattformwechsel
koennen die Symlinks verloren gehen. Ausserdem legt `ln -s` in Git-Bash auf
Windows oft **Kopien** statt Symlinks an — diese erscheinen "ok" sind aber stale.

**Auto-Repair (seit Plugin v2.1 / FIN-030, 2026-05-22):**
`scripts/verify-skills.sh` repariert fehlende Symlinks AUTOMATISCH bevor es
aufgibt. Das funktioniert auf:
- **macOS/Linux:** via `ln -s` (Standard-POSIX-Verhalten)
- **Windows Git Bash:** via `cygpath` + `cmd //c mklink /D` (der einzige zuverlaessige Weg)

Wenn das Auto-Repair fehlschlaegt (z. B. die Skill-Quelle in `~/.claude/skills/`
existiert auch nicht), siehe `../../INSTALL.md` fuer den manuellen Wiederherstellungs-
Ablauf.

## Manueller Wiederherstellungs-Befehl

```bash
# macOS/Linux:
cd <plugin-root>/skills
ln -s ~/.claude/skills/app-roentgen     roentgen-skill
ln -s ~/.claude/skills/rechtssicherheit rechtssicherheits-skill
ln -s ~/.claude/skills/string-extraktor strings-skill
ln -s ~/.claude/skills/übersetzung      uebersetzer-skill
```

```powershell
# Windows PowerShell (Admin/Developer-Mode):
$base = '<plugin-root>\skills'
$targets = @{
  'roentgen-skill'         = "$env:USERPROFILE\.claude\skills\app-roentgen"
  'rechtssicherheits-skill'= "$env:USERPROFILE\.claude\skills\rechtssicherheit"
  'strings-skill'          = "$env:USERPROFILE\.claude\skills\string-extraktor"
  'uebersetzer-skill'      = "$env:USERPROFILE\.claude\skills\übersetzung"
}
foreach ($n in $targets.Keys) {
  New-Item -ItemType SymbolicLink -Path (Join-Path $base $n) -Target $targets[$n] -Force | Out-Null
}
```

## Verifikation dass es ECHTE Symlinks sind (nicht Kopien)

```bash
# macOS/Linux/Git Bash:
ls -la <plugin-root>/skills | grep ' -> '   # echte Symlinks haben ' -> ZIEL' am Ende

# PowerShell:
Get-ChildItem '<plugin-root>\skills' | Where-Object LinkType -eq 'SymbolicLink' | Select-Object Name, Target
```

Wenn die Datei kein "->" zeigt (Bash) oder LinkType leer ist (PowerShell): es ist
eine Kopie, nicht ein Symlink. Loeschen + neu via Anleitung oben anlegen.

---

Anleitung zum kompletten Einspielen siehe `../../INSTALL.md`.
