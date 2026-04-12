# Memories und Regeln: Immer transparent erklaeren (KRITISCH)

## Regel

Wenn ein Memory, eine Regel oder eine Konfigurationsaenderung gespeichert wird,
MUSS dem Benutzer auf Deutsch erklaert werden was gespeichert wurde und warum.
Keine stillen Aenderungen am Wissenssystem.

## Format

Nach jedem Speichervorgang eine kurze Erklaerung:

```
Gespeichert: **[Titel]** — [1-2 Saetze was das bedeutet und warum es wichtig ist].
```

Bei mehreren gleichzeitigen Speichervorgaengen als kurze Liste:

```
Gespeichert:
- **[Titel 1]** — [Erklaerung]
- **[Titel 2]** — [Erklaerung]
```

## Gilt fuer

- Feedback-Memories (Verhaltensregeln)
- Reference-Memories (Verweise auf externe Systeme)
- Project-Memories (Projektzustand)
- User-Memories (Benutzer-Praeferenzen)
- Neue Regeln in `~/.claude/rules/`
- Aenderungen an `settings.json` oder anderen Konfigurationsdateien
- Eintraege im Shared Knowledge Hub (Whiteboard)

## Warum

Der Benutzer ist kein Programmierer und will verstehen was in seinem System passiert.
Stille Aenderungen am Wissenssystem widersprechen der Sichtbarkeitsregel. Wenn der
Benutzer nicht weiss was gespeichert wurde, kann er Fehler in der Logik nicht korrigieren.
Falsche Memories verfestigen sich dann ueber viele Sessions und werden immer schwerer
zu finden und zu reparieren.

Am 2026-03-27 wurden drei Memories still gespeichert ohne Erklaerung. Der Benutzer
hat das bemerkt und die Transparenzregel eingefuehrt.

## Nicht uebertreiben

Die Erklaerung soll kurz sein (1-2 Saetze pro Memory), nicht ausfuehrlich (keine 10 Zeilen).
Der Benutzer will wissen WAS und WARUM, nicht jeden technischen Detail.
