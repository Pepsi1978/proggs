# Arbeitsprofile: schnell, normal, gründlich (Windows)

Gilt für beauftragte **Programmierung von Apps und Software** im CLI-Dialog unter Windows,
mit Claude, OpenCode oder Codex CLI als Implementierer, über den bestätigten
[Windows-tmux](windows-tmux.md)-Weg oder den [Windows-Fensterweg](windows-fenster.md).
Das Profil legt Umfang pro Update, Plantiefe und Prüfbreite fest. Es ändert weder
Autorisierung noch Transport, Zielbindung, Eingabeschutz, Stopp oder die Abschlusskette.

## Geltung und Rangfolge

1. AGENTS.md, Projektregeln und ausdrückliche Nutzerregeln (inklusive Abschlusskette).
2. Transport-, Identitäts- und Eingabeschutz des gewählten Windows-Wegs; die beauftragte
   laufende CLI-Sitzung bleibt dieselbe.
3. Das aktive Arbeitsprofil, im Auftrag an die CLI genannt.
4. Ein beim Start mitgegebener OpenLauncher-Arbeitsmodus der CLI-Sitzung, soweit vorhanden
   (etwa `WorkModes/*.md` in der CLAUDE.md oder über das OpenCode-Plugin).

Das Profil wirkt über den Auftragstext. Der Skill selbst ändert Modell, Effort, Start- und
Launcher-Profil, einen CLI- oder Launcher-Modus und die Rechte der Sitzung nicht: kein
Neustart und kein eigenes Umschalten in der CLI-Oberfläche, damit die laufende Sitzung
erhalten bleibt. Ein abweichender Startmodus wird für die Runde ausdrücklich übersteuert,
soweit Rang 1 nichts anderes verlangt. Schaltet der Nutzer den Modus in der CLI selbst um,
etwa in OpenCode, ist das neue Steuerung: am nächsten sicheren Meilenstein frisch lesen
und im effektiven Profil berücksichtigen.

**Effektives Profil ehrlich benennen:** Verlangt eine Regel aus Rang 1 mehr oder weniger als
das gewählte Profil (etwa Pflichtbuild und Installation trotz schnell, oder ein vom Nutzer
fest vorgegebener Modus), gilt diese Regel. Auftrag und Zwischenstand nennen dann das
effektiv angewandte Profil samt Abweichung, zum Beispiel „schnell, Build und Installation
nach AGENTS.md“.

**Wissensaustausch** (Fragen, Erklärungen, Konzeptdiskussion ohne Umsetzungsauftrag) läuft
ohne Profil: keine Programmierplanung, keine Tests, keine Lieferkette, kein Zielvertrag.
Erst ein ausdrücklicher Umsetzungsauftrag aktiviert das Profil; eine Diskussion wird nicht
still zur Umsetzung. Wissensfragen während eines laufenden Programmierloops werden
beantwortet, ohne eine neue Runde oder Lieferkette auszulösen.

## Profile

| | schnell | normal (Standard) | gründlich |
|---|---|---|---|
| Umfang pro Update | möglichst viele sinnvolle, vom Auftrag gedeckte Verbesserungen in einem zusammenhängenden Update | ein zusammenhängender Änderungssatz | kleinster nutzbarer vertikaler Teil |
| Planung | Abdeckungsliste nur bei mehreren Varianten; direkt gebündelter Auftrag, Diskussion nur bei echtem Risiko | kurzer Plan, Konsens bei Abwägungen | Plan mit Abdeckungsliste, Alternativen und Risiken vor der Umsetzung |
| Prüfung durch die CLI | Build plus je gebündelter Verbesserung ein knapper Wirknachweis des geänderten Pfads, soweit Rang 1 ihn zulässt oder verlangt | passende Tests, naheliegende Regressionen, relevante Randfälle | vollständige relevante Tests, Randfälle, Aufrufer, verwandte Fehlerklassen, Gerätepfad |
| Mindest-`review_depth` des Koordinators | compact | compact, nach Risiko targeted | targeted, nach Risiko full |
| Advisor | bestehende Auslöser | bestehende Auslöser | bestehende Auslöser, zusätzlich folgenreiche Architekturentscheidungen mit echter Unsicherheit |

Advisor meint den in [Kontingent sparsam nutzen](kontingent-sparen.md) beschriebenen
konfigurierten Advisor der Claude-Sitzung. Kein Profil fügt automatisch weitere Advisors,
Ersatzmodelle oder Agenten hinzu; fehlt der Advisor (etwa bei OpenCode oder Codex CLI),
`advisor_used=false` mit Grund.

**Prüfumfang und Stoppkriterium, alle Profile:** Das Profil bestimmt die Breite, das Risiko
die Mindesttiefe. Eine nach Risiko, Projektregel oder Kriterium erforderliche Prüfung
entfällt nie wegen des Profils oder einer Zahl. Wiederholt wird nur nach einem konkreten
Fehler, einer neuen Änderung an den geprüften Pfaden oder einem benannten Risiko, nicht
routinemäßig. Die Prüfung ist beendet, wenn alle erforderlichen Prüfungen für den
aktuellen Stand grün sind und kein Befund offen ist. Nach zwei Fehlversuchen ohne neue
Evidenz Strategiewechsel gemäß [Programmierloop](programmier-loop.md); ein Blocker wird
gemeldet, nicht durch weitere Wiederholungen verdeckt.

**Schnell heißt Menge bei guter Qualität:** tatsächlich implementierte, nutzerwirksame
Verbesserungen innerhalb des Auftrags. Keine rein kosmetischen Änderungen, keine
Scheinfunktionen und keine halben Umbauten, um die Menge zu erhöhen. Ein begrenzt
riskanter, klar abgrenzbarer Kandidat darf nach kurzer, risikogerechter Absicherung ins
Bündel; `full`-Auslöser gelten dabei unverändert. Zurückgestellt wird nur, was sich im
verfügbaren Update nicht sauber implementieren und angemessen absichern lässt oder nicht
eindeutig vom Auftrag gedeckt ist; es kommt als Vorschlag in den Bericht, statt das
Bündel zu blockieren. Der Zwischenstand listet umgesetzte Verbesserungen je mit Beleg und
zurückgestellte getrennt. Mehr Menge rechtfertigt kein Absenken der Kriterien.

## In jedem Profil unverändert

- Abschlusskette nach Projektregel: Die geltenden Nutzer- und Projektregeln bestimmen, welche
  Schritte für die Art der Änderung Pflicht sind. Bei Codeänderungen vollständig: Build,
  Versionsbump mit echter Zeit, Commit, Rebase/Push, autorisierte Installation
  beziehungsweise Deployment. Wissensantworten haben keine Lieferkette; reine
  Dokumentationsänderungen, etwa an Skills, lösen keinen OpenLauncher-App-Build oder
  App-Update ohne technische Notwendigkeit oder ausdrückliche Regel aus.
- Die `full`-Auslöser aus [Kontingent sparsam nutzen](kontingent-sparen.md) gelten immer;
  ebenso „Kriterien nie absenken“ und das Stoppkriterium oben.
- Review-Checkpoint, Steuerdatei, `STOP`, Zustellungsledger, `submit`-Beschränkung auf
  geprüfte Claude-Eingaberahmen sowie Fokus- und Eingabeprüfung des Fensterwegs.

## Wahl und Wechsel

- **Standard:** `normal`, soweit weder der Nutzer noch eine höherrangige Regel oder der
  Zielvertrag (`profil`) ein anderes Profil festlegt.
- **Nur eindeutige Formulierungen** wählen oder wechseln das Profil, etwa „Profil schnell“,
  „Schnellmodus“, „Standardmodus“, „in den Gründlichkeitsmodus“. Bloße Dringlichkeit
  („mach schnell“, „zügig bitte“) oder Sorgfaltswünsche zu einem Einzelpunkt sind kein
  Wechsel. Bei echter Mehrdeutigkeit kurz nachfragen.
- Ein Wechsel ist eine verbindliche Nutzerkorrektur: `profil` in `ziel.json` setzen,
  `ziel_rev` erhöhen, Steuerdatei atomar ersetzen. Das Profil steht im Zielvertrag,
  nicht in `arbeitsstand.json` (dessen fünf Schlüssel bleiben). Begleitete
  Softwareprogrammierung nutzt auf **beiden** Windows-Wegen den Zielvertrag gemäß
  [Programmierloop](programmier-loop.md). Nach Kontextverdichtung `ziel.json` einmal lesen,
  bevor der nächste Umsetzungsauftrag ein Profil nennt; `resume` ist eine tmux-Funktion und
  liefert den Zielvertrag nicht mit. Nur kurze Einzelübergaben ohne diesen Loop tragen das
  Profil im flüchtigen Kontext und im Auftragstext.
- Wirksam ab der **nächsten sicheren Runde**, zugestellt am nächsten freien Prompt, nie in
  eine laufende Generation. Eine laufende oder bereits geprüfte Runde behält die erreichte
  Prüftiefe bis zu ihrem Abschluss. Eine spätere neue Runde arbeitet nach dem ausdrücklich
  gewählten schwächeren Profil, soweit Risiko und Projektregeln es erlauben. Den Wechsel
  knapp bestätigen: „Profil ab R5: schnell“.

## Im Auftrag an die CLI

Jeder Umsetzungsauftrag nennt eine Profilzeile, zum Beispiel:

> Profil: schnell (gilt für diese Runde vor deinem Startmodus; AGENTS.md und Projektregeln
> haben Vorrang).

Der Zwischenstand nennt das effektiv angewandte Profil. Der Koordinator prüft, dass Umfang
und Prüfbelege zum Profil passen, und stuft bei Risiko hoch.
