# Python-Pfade auf Windows: NIEMALS /c/Users/... (KRITISCH)

## Problem

Git Bash uebersetzt Windows-Pfade in Unix-Stil: `C:\Users\barwa` wird zu `/c/Users/barwa`.
Python auf Windows versteht dieses Format NICHT — es fuehrt zu `FileNotFoundError`.

## Regel: Immer os.path.expanduser() oder os.environ verwenden

```python
# FALSCH — crasht auf Windows via Git Bash:
with open('/c/Users/barwa/.claude/settings.json') as f: ...
with open('C:/Users/barwa/.claude/settings.json') as f: ...

# RICHTIG — funktioniert ueberall:
import os
path = os.path.expanduser('~/.claude/settings.json')
with open(path, 'r', encoding='utf-8') as f: ...

# Oder mit os.environ:
home = os.environ.get('USERPROFILE', os.path.expanduser('~'))
path = os.path.join(home, '.claude', 'settings.json')
```

## Warum

Am 2026-04-03 scheiterte ein Python-Einzeiler weil der Pfad als
`/c/Users/barwa/.claude/settings.json` uebergeben wurde. Python konnte
die Datei nicht finden, obwohl sie existierte. Fix: `os.path.expanduser('~/')`.

## Faustregel

- `Bash` und `Git Bash`: `/c/Users/...` Pfade funktionieren
- `Python`, `Node.js`, `PowerShell`: Brauchen native Windows-Pfade oder `expanduser()`
- Bei Python-Einzeilern (`python3 -c "..."`) in Git Bash: IMMER `os.path.expanduser()` verwenden
