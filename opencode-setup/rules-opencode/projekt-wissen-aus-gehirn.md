Projekt-Wissen aus dem Gehirn zuerst lesen (Kategorie Projekte)

## Die eine Regel
BEVOR du an einem Projekt zu arbeiten beginnst, pruefst du IMMER zuerst, ob es im zweiten Gehirn
(MCP `second-brain`) unter der Kategorie **Projekte** Informationen zu genau DIESEM Projekt gibt.
Wenn ja: diese Infos KOMPLETT durchlesen, BEVOR du Code aenderst, baust oder deployst. Wenn nein:
ganz normal weiterarbeiten (nichts erzwingen).

## Ablauf (jedes Mal, kurz)
1. **Projekt erkennen** — woran arbeite ich gerade? Aus dem Ordnernamen unter `~/proggs/<projekt>/`
   bzw. dem Marken-/Projektnamen (z.B. `second-brain-server`/„Cortex", „Entropie Reductor").
2. **Im Gehirn nachsehen** — gibt es zu diesem Projekt einen Eintrag in der Kategorie **Projekte**?
   - Sicherster Weg: `second-brain_recall` mit dem Projektnamen als Suchtext (semantisch, findet auch
     ohne exakte Kategorie-Schreibweise). Bei klarem Titel: `second-brain_get_by_title`.
   - Kategorie durchgehen: `second-brain_get_by_category` für „Projekte" — aber **einzeln** lesen
     (nie eine grosse Kategorie als Block laden, sonst abgeschnitten/halluziniert).
3. **Treffer? → ZUERST komplett lesen.** Das Projekt-Wissen erklaert oft genau das, was sonst lange
   gesucht wird — z.B. **wie man auf den Server deployt**, wo Secrets/Configs liegen, Stolperfallen.
   Erst nach dem Lesen mit der eigentlichen Arbeit anfangen.
4. **Kein Treffer? → normal weiterarbeiten.** Keine Pflicht, nichts erfinden.

## Warum
Verhindert, dass du Bekanntes neu zusammensuchst oder falsch machst (z.B. den Deploy-Weg raten).
Das „So geht das hier" steht im Gehirn unter Projekte — einmal lesen spart langes Suchen.

## NIEMALS
- Ein Projekt bauen/deployen/groesser aendern, OHNE vorher zu pruefen, ob es dazu einen Eintrag im
  Gehirn (Kategorie Projekte) gibt.
- Eine grosse Gehirn-Kategorie als EINEN Block laden — immer einzeln (sonst truncation/Halluzination).
- Den Projekt-Eintrag nur ueberfliegen — bei Treffer wird er KOMPLETT gelesen, vor der Arbeit.
