# Codex Selbstbeobachtung

Diese Regel ist die zweite Systemdirektive unter der `## Oberste Direktive`.
Sie gilt fuer Codex in diesem Workspace, nicht fuer Claude Code.

## Zweck

Codex soll waehrend der Arbeit nicht nur liefern, sondern sich selbst beobachten:

- Fehler, Fehlstarts und falsche Annahmen
- Umwege, Rediscovery und unnötige Dateisuche
- Geschwindigkeitsverluste und zu späte Verifikation
- Benutzerhinweise und Korrekturen
- wiederkehrende Reibung in Regeln, Tooling, MCP, Whiteboard oder Skill-Flows

Das Ziel ist, dass jede Session nicht nur Arbeit erledigt, sondern die Codex-Programmierumgebung dauerhaft besser macht.

## Timing

- Beobachten: waehrend der Arbeit
- Auswertung: am Ende der Aufgabe oder beim erkennbaren System-Fix
- Intelligenzvorschlaege: am Ende, nicht als dauernde Unterbrechung mitten in der Ausfuehrung

## Was aus Selbstbeobachtung folgen soll

- ein direkter Fix in derselben Session, wenn Codex ihn sicher selbst bauen kann
- oder ein klarer `Intelligenzvorschlag`, wenn fuer den Fix zunaechst Benutzerfreigabe noetig ist
- oder eine neue Validierung, Regel, Referenz oder Workflow-Haertung, die denselben Fehler spaeter verhindert

## Beobachtungsklassen

### 1. Fehler

- falsche Pfade
- falsche Tool-Annahmen
- Whiteboard-, MCP- oder Validierungsfehler
- Missverstaendnisse ueber Benutzerwunsch oder Setup-Grenzen

### 2. Umwege

- dieselbe Information zweimal suchen
- unnötig viele Dateien oeffnen
- bekannte Setup-Regeln nicht frueh genug anwenden

### 3. Geschwindigkeit

- zu langsamer Startpfad
- fehlende Standardpruefung fuer bekannte Reibung
- manuelle Vergleiche, die skriptbar waeren

### 4. Benutzerkorrekturen

- Hinweise wie "nur lesen, nicht schreiben"
- Praeferenzen zu Ueberschreiben versus additive Integration
- Formulierungen, die fuer kuenftige Trigger oder Schutzregeln wichtig sind

### 5. Wiederholungsmuster

- derselbe Fehlertyp taucht erneut auf
- dieselbe Rueckfrage haette durch bessere Regeln vermeidbar sein koennen
- dieselbe Claude-zu-Codex-Uebersetzung wird wiederholt manuell gemacht

## Rueckgaberegel

Wenn Selbstbeobachtung etwas Reales zeigt, soll Codex am Ende mindestens eines davon liefern:

- ein eingebauter Schutz
- ein neuer Validierungsschritt
- ein portabler Workflow-Gewinn
- ein approval-faehiger `Intelligenzvorschlag`

## Bewaehrungsphase fuer neue Regeln

- Neue oder portierte Codex-Regeln gelten zunaechst als in Bewaehrung.
- Erst nach 5 realen Anwendungen ohne inhaltliche Ruecknahme gelten sie als robust.
- Bis dahin soll Codex bei Folgesessions weiter beobachten, ob Randfaelle, Konflikte oder zu starke Ueberschneidungen sichtbar werden.

## Was nie passieren soll

- ein Fehler wird sichtbar und Codex ignoriert ihn still
- derselbe Umweg wird ein zweites Mal genommen, ohne ihn abzusichern
- ein Benutzerhinweis wird nicht in Codex-Regeln, Whiteboard oder Workflow-Denken aufgenommen
- eine neue Idee aus Claude Code ueberschreibt gute bestehende Codex-Intelligenz, obwohl additive Integration moeglich waere

