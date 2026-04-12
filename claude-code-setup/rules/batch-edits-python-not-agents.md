# Batch-Edits: Python statt parallele Coder-Agents (KRITISCH)

## Regel

Wenn die GLEICHE Aenderung an mehreren Dateien gemacht werden muss (3+ Dateien),
MUSS ein Python-Batch-Script verwendet werden. NIEMALS parallele Coder-Agents.

## Warum

Am 2026-03-28 sollten 5 parallele Coder-Agents je 2 Tampermonkey-Skripte bearbeiten.
4 von 5 Agents vergassen UI_IDS-Eintraege, 4 vergassen Variablen-Deklarationen,
2 fuegten fehlerhafte Focus-Prevention hinzu. Ergebnis: 8 Korrektur-Commits statt 1.
Die Python-Batch-Methode (`glob` + `re.sub`) hat danach JEDEN Fix auf Anhieb korrekt
in alle 10 Dateien eingefuegt — 0 Fehler.

## Wann Python-Batch verwenden

- Gleiche Funktion/Variable in N Dateien hinzufuegen
- Gleichen Text in N Dateien ersetzen (Icons, Selektoren, Strings)
- Gleichen Code-Block in N Dateien einfuegen
- Version-Bumps ueber mehrere Dateien
- Jede mechanische Aenderung die in allen Dateien identisch ist

## Wann Coder-Agents OK sind

- Einmalige, datei-spezifische Aenderungen (1 Datei, eigene Logik)
- Komplexe Refactorings die Verstaendnis der Dateistruktur brauchen
- Neue Features die pro Datei unterschiedlichen Code erfordern

## Pattern

```python
import glob, re

for f in sorted(glob.glob('PFAD/*.EXTENSION')):
    with open(f, 'r', encoding='utf-8') as fh:
        content = fh.read()
    original = content
    # Aenderungen hier
    content = re.sub(r'PATTERN', 'REPLACEMENT', content)
    if content != original:
        with open(f, 'w', encoding='utf-8', newline='\n') as fh:
            fh.write(content)
        print(f'Fixed: {f}')
```

## Zusatzregel: sed auf Windows vermeiden

`sed` auf Windows Git Bash kann `\u`-Escape-Sequenzen nicht korrekt verarbeiten.
IMMER Python `str.replace()` oder `re.sub()` verwenden fuer Textersetzungen
die Unicode-Escapes enthalten.
