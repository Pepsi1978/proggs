# Cowork-Portierung: IMMER den cowork-portierung-Skill nutzen (MUSS-BEDINGUNG, KRITISCH)

> Frank: Cowork-Portierung IMMER ueber den Skill (Muss-Bedingung).

## Die eine Regel
Jede Umwandlung eines CLI-Skills in seine Cowork-Fassung laeuft AUSSCHLIESSLICH ueber den Skill
`cowork-portierung` - nie von Hand, ad hoc oder als "logische Abarbeitung". Trigger: "portiere X zu
cowork", "mach X cowork-tauglich", "Cowork-Version von X bauen".

## Was "den Skill nutzen" heisst
1. Skill anwenden - ein Worker pro Skill liest `cowork-portierung/SKILL.md` und befolgt ihn Schritt fuer
   Schritt (transformiert vom CLI-Original `~/.claude/skills/<name>/`, nicht von einer Kopie).
2. ZIP mit gebuendeltem `build-cowork-zip.py` bauen (garantiert Forward-Slash/LF/Wurzel) - nie von Hand.
3. Hauptagent koordiniert: verifizieren, README-Tabelle pflegen, committen+pushen.

Auch Re-Portierung laeuft ueber den Skill (bestehende Fassung ERSETZEN).

## Einzige Ausnahme
Skill KEIN CLI-Original (nur Cowork-Fassung, z.B. `research`): kann nicht "portiert" werden - bestehende
Cowork-Quelle ist die Wahrheit, bei Aenderung nur ZIP neu bauen. Greift NUR ohne CLI-Original.

## Was NIEMALS
- Cowork-Fassung von Hand schreiben/transformieren - Cowork-ZIP ohne `build-cowork-zip.py` bauen -
  bestehende Fassung per Edit "nachbessern" statt ueber den Skill neu zu erzeugen - den Skill
  ueberspringen - die Ausnahme auf Skills mit CLI-Original anwenden.
