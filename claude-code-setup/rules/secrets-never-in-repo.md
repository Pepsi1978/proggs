# Secrets: NIEMALS in Repo-Dateien — NUR in lokalen Dateien (KRITISCH)

> **Poka-Yoke Stufe 3 (Eliminierung): Secrets koennen konzeptionell nicht mehr ins Repo gelangen.**
> **Direktive #3: Dieser Fehler ist 4-5x aufgetreten. Ab jetzt wird er durch Design verhindert.**

## Regel

Secrets (API-Keys, Tokens, Passwoerter) duerfen AUSSCHLIESSLICH in Dateien stehen
die NICHT im Git-Repository sind. Repo-Dateien enthalten NUR Platzhalter/Verweise.

## Wo Secrets leben duerfen (NICHT im Repo)

| Datei | Zweck | Im Repo? |
|-------|-------|----------|
| `~/.claude/settings.json` | Aktive Claude-Settings mit echten Tokens | **NEIN** (Home-Verzeichnis) |
| `~/.claude/settings.local.json` | Lokale Overrides | **NEIN** |
| `.env`-Dateien | Projekt-Secrets | **NEIN** (gitignored) |
| System-Umgebungsvariablen | OS-level Secrets | **NEIN** |

## Wo Secrets NIEMALS stehen duerfen (IM Repo)

| Datei | Was stattdessen drinsteht |
|-------|--------------------------|
| `claude-code-setup/settings-reference.json` | `"REDACTED — set locally in ~/.claude/settings.json"` |
| `claude-code-setup/settings.json` (macOS) | `"REDACTED — set locally in ~/.claude/settings.json"` |
| `claude-code-setup/settings.local.json` | `"REDACTED — set locally"` |
| Jede `.json`, `.md`, `.yaml` im Repo | Verweis auf lokale Datei statt echtem Secret |

## Pflicht-Ablauf beim Kopieren von Settings ins Repo

Wenn `~/.claude/settings.json` ins Setup-Repo kopiert wird (3-Dateien-Regel):

1. Kopieren: `cp ~/.claude/settings.json claude-code-setup/settings-reference.json`
2. **SOFORT danach redaktieren** — BEVOR staged/committed wird:
   ```bash
   python3 -c "
   import os, re
   path = os.path.expanduser('~/proggs/claude-code-setup/settings-reference.json')
   with open(path, 'r', encoding='utf-8') as f:
       content = f.read()
   # Alle bekannten Secret-Patterns redaktieren
   content = re.sub(r'gho_[A-Za-z0-9]{30,}', 'REDACTED — set locally in ~/.claude/settings.json', content)
   content = re.sub(r'ghp_[A-Za-z0-9]{30,}', 'REDACTED — set locally in ~/.claude/settings.json', content)
   content = re.sub(r'sk-[A-Za-z0-9]{20,}', 'REDACTED — set locally in ~/.claude/settings.json', content)
   content = re.sub(r'AIza[A-Za-z0-9_-]{30,}', 'REDACTED — set locally in ~/.claude/settings.json', content)
   with open(path, 'w', encoding='utf-8') as f:
       f.write(content)
   print('Secrets redacted')
   "
   ```
3. Erst DANN: `git add` + `git commit`

## Was NIEMALS passieren darf

- ❌ `cp settings.json settings-reference.json` ohne anschliessendes Redaktieren
- ❌ Echte Tokens in IRGENDEINER Datei unter `~/proggs/` committen
- ❌ "Ich redaktiere das spaeter" — SOFORT, vor dem naechsten git add
- ❌ Secret-Patterns die der Regex nicht abdeckt manuell stehenlassen
