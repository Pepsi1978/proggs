# Cowork-Portierung: IMMER den cowork-portierung-Skill nutzen (MUSS-BEDINGUNG, KRITISCH)

> Franks Wortlaut: "Zum Portieren zu Cowork muss IMMER der Cowork-Portierungsskill genutzt werden,
> nichts anderes. Das ist eine Muss-Bedingung."

## Die eine Regel

Jede Umwandlung eines CLI-Skills in seine Cowork-Fassung laeuft AUSSCHLIESSLICH ueber den Skill
`cowork-portierung`. Niemals von Hand, ad hoc oder als "logische Abarbeitung" ersetzt. Trigger:
"portiere X zu cowork", "mach X cowork-tauglich", "Cowork-Version von X bauen", "bring X nach Cowork".

## Was "den Skill nutzen" heisst

1. Skill anwenden — selbst seinen Ablauf strikt befolgen ODER (bevorzugt) einen Worker pro Skill, der
   `cowork-portierung/SKILL.md` liest und Schritt fuer Schritt befolgt. Transformiert vom CLI-Original
   (`~/.claude/skills/<name>/`), nicht von einer Kopie.
2. ZIP mit dem gebuendelten `cowork-portierung/scripts/build-cowork-zip.py` bauen (garantiert
   Forward-Slash/LF/Wurzel=Ordnername) — nie von Hand zippen.
3. Hauptagent koordiniert: verifizieren, README-Tabelle pflegen, committen+pushen.

Auch eine Re-Portierung laeuft ueber den Skill (bestehende Fassung durch die frisch erzeugte ERSETZEN).

## Einzige Ausnahme: Cowork-exklusive Skills ohne CLI-Original

Hat ein Skill KEIN CLI-Original unter `~/.claude/skills/<name>/` (nur Cowork-Fassung, z.B. `research`):
der Skill kann ihn nicht "portieren". Dann ist die bestehende Cowork-Quelle die Wahrheit; bei Aenderung
nur das ZIP neu bauen (mit `build-cowork-zip.py`). Eng — greift NUR wenn wirklich kein CLI-Original existiert.

## Warum

Der Skill kapselt die Cowork-Restriktionen, die von Hand staendig vergessen werden: `description`
einzeilig ≤200 Zeichen, keine `<…>`/URLs, Titel `(Cowork-Fassung)`, Block 0 (relative Ablage) + Block 0a
(Mount-Fallen, ~45s-Limit, `cowork-git.sh`), feste `~/proggs`-Pfade → relativ, Begleitdateien mit, ZIP mit
richtiger Wurzel/LF.

## Was NIEMALS passieren darf

- Eine Cowork-Fassung von Hand schreiben/transformieren · ein Cowork-ZIP ohne `build-cowork-zip.py` bauen
- Eine bestehende Fassung per Edit "nachbessern" statt ueber den Skill neu zu erzeugen · den Skill
  ueberspringen ("geht schneller"/"alte Fassung ok") · die Ausnahme auf Skills mit CLI-Original anwenden
