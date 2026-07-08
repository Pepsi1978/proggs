# Cowork-Portierung: IMMER den cowork-portierung-Skill nutzen (MUSS-BEDINGUNG, KRITISCH)

> Franks Wortlaut: "Zum Portieren zu Cowork muss IMMER der Cowork-Portierungsskill genutzt werden,
> nichts anderes. Das ist eine Muss-Bedingung."

## Die eine Regel

Jede Umwandlung eines CLI-Skills in seine Cowork-Fassung laeuft AUSSCHLIESSLICH ueber den Skill
`cowork-portierung` — nie von Hand, ad hoc oder als "logische Abarbeitung". Trigger: "portiere X zu
cowork", "mach X cowork-tauglich", "Cowork-Version von X bauen", "bring X nach Cowork".

## Was "den Skill nutzen" heisst

1. Skill anwenden — seinen Ablauf strikt befolgen ODER (bevorzugt) ein Worker pro Skill, der
   `cowork-portierung/SKILL.md` liest + Schritt fuer Schritt befolgt (transformiert vom CLI-Original
   `~/.claude/skills/<name>/`, nicht von einer Kopie).
2. ZIP mit dem gebuendelten `build-cowork-zip.py` bauen (garantiert Forward-Slash/LF/Wurzel) — nie von Hand.
3. Hauptagent koordiniert: verifizieren, README-Tabelle pflegen, committen+pushen.

Auch eine Re-Portierung laeuft ueber den Skill (bestehende Fassung ERSETZEN).

## Einzige Ausnahme

Hat ein Skill KEIN CLI-Original (nur Cowork-Fassung, z.B. `research`): der Skill kann ihn nicht
"portieren" — die bestehende Cowork-Quelle ist die Wahrheit; bei Aenderung nur das ZIP neu bauen. Eng —
greift NUR wenn wirklich kein CLI-Original existiert.

## Was NIEMALS

- Eine Cowork-Fassung von Hand schreiben/transformieren · ein Cowork-ZIP ohne `build-cowork-zip.py` bauen
  · eine bestehende Fassung per Edit "nachbessern" statt ueber den Skill neu zu erzeugen · den Skill
  ueberspringen · die Ausnahme auf Skills mit CLI-Original anwenden.
