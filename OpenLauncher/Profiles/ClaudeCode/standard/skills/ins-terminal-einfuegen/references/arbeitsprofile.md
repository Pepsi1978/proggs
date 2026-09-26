# Arbeitsprofile: schnell, normal, gründlich (Windows)

Gilt für beauftragte **Programmierung von Apps und Software** im CLI-Dialog unter Windows,
mit Claude, OpenCode oder Codex CLI als Implementierer, über den bestätigten
[Windows-tmux](windows-tmux.md)-Weg oder den [Windows-Fensterweg](windows-fenster.md).
Das Profil legt Umfang pro Update, Plantiefe und Prüfbreite fest. Es ändert weder
Autorisierung noch Transport, Zielbindung, Eingabeschutz, Stopp oder die Abschlusskette.

## Geltung und Rangfolge

1. Verbindliche Projektpflichten aus AGENTS.md und Projektregeln sowie ausdrückliche
   Nutzerregeln (inklusive Abschlusskette, Freigaben, Version und Commit-Regeln).
2. Transport-, Identitäts- und Eingabeschutz des gewählten Windows-Wegs; die beauftragte
   laufende CLI-Sitzung bleibt dieselbe.
3. Das aktive Arbeitsprofil, im Auftrag an die CLI genannt, in dieser Reihenfolge: die
   jüngste ausdrückliche Nutzerwahl, sonst `profil` im Zielvertrag, sonst der Startwert
   (siehe „Wahl und Wechsel“).
4. Der Text eines bestätigten OpenLauncher-Startmodus der CLI-Sitzung
   (`Profiles/WorkModes/<id>.md`), gleich wo er eingespeist ist.

**Herkunft des Startmodus:** Der Launcher hängt den Modustext bei Claude an die
Sitzungs-CLAUDE.md im Launcher-Profilordner (`CLAUDE_CONFIG_DIR`), bei Codex CLI an die
Profil-AGENTS.md im Arbeitsordner (Kopfzeile „# Open-Code-Profil:“, Modusblock ab „AKTIVER
ARBEITSMODUS:“) und friert ihn dort beim Start ein. Der Launcher schreibt diese Dateien bei
jedem Start des Profils neu. **Bestätigt** ist ein Startmodus nur aus dem
aktuellen Startkontext der Zielsitzung: Die Datei trägt die Launcher-Kennung ihres Orts,
ihr Modusblock stimmt nach Trimmen genau mit einer `Profiles/WorkModes/<id>.md` überein,
und sie ist nicht jünger als der Start der Zielsitzung (etwa tmux `session_created` oder
Prozessstart). Fehlt ein Blockanhang und passt `frei.md` (leer), ist der Startmodus `frei`.
**OpenCode** friert nichts ein: Sein Modus-Plugin
(`opencode-setup/plugins/work-mode.js`) bestimmt den Modus bei **jedem Modellaufruf** neu
über die Zustandsdatei `~/.local/state/opencode/work-modes/<sessionID>.txt` (schreibt die
TUI beim Umschalten; im Dateinamen ist jedes Zeichen der Sitzungs-ID außerhalb
`[a-zA-Z0-9_-]` durch `_` ersetzt). Enthält sie einen gültigen Modus, gilt er; ist sie
vorhanden, aber ungültig, gilt direkt der Plugin-Standard `schnell`, **nicht** der
Env-Wert. Nur wenn sie fehlt, gilt `OPENLAUNCHER_WORK_MODE` aus dem Launcher-Startskript,
bei fehlendem oder ungültigem Wert ebenfalls `schnell`. Der Modustext kommt jeweils frisch
aus `WorkModes/<id>.md`. Bestätigt ist der OpenCode-Startmodus nur, wenn genau die
Zielsitzung gebunden ist: ihre Sitzungs-ID, daraus abgebildet genau ihre Zustandsdatei,
oder, solange diese fehlt, der Wert im Startskript genau dieser Sitzung
(`%TEMP%\openlauncher-opencode-run-<GUID>.ps1`, vom Wrapper der Zielsitzung aufgerufen).
Nie die jüngste oder irgendeine Zustandsdatei nehmen. Ohne diese Bindung unbestätigt,
obwohl das Plugin womöglich den Block `schnell` einspeist: Auftrag und Zwischenstand
nennen das. Weil der Modus live wechseln kann, bei OpenCode vor jedem Umsetzungsauftrag
und an jedem sicheren Meilenstein die Zustandsdatei erneut lesen. Ein geänderter Wert ist
ein Hinweis, noch keine Zieländerung: Nur wenn der Nutzer den Modus erkennbar umgeschaltet
hat, um die Arbeitsweise zu ändern, ist das neue Steuerung und landet bei aktivem
Zielvertrag wie jeder Profilwechsel in `profil`, `ziel_rev` und Steuerdatei-`rev`. Ein
bloßer Dateiwechsel ohne belegte Nutzerabsicht bleibt Befund und wird bei Unklarheit
kurz geklärt, bevor er das Profil ändert.
Eine bloße Überschrift „AKTIVER ARBEITSMODUS“ in einer beliebigen AGENTS.md genügt
nicht: Ohne bestätigte Launcher-Herkunft bleibt deren Text AGENTS.md-Regel auf Rang 1 und
wird nicht eigenmächtig herabgestuft; bei Widerspruch zum Profil gilt er, und Auftrag und
Zwischenstand nennen die Abweichung, vor folgenreichen Schritten kurz klären.

Ein bestätigter, in AGENTS.md eingespeister Block bleibt Rang 4 und wird nicht allein
durch seinen Speicherort zur Projektpflicht. Bei einem echten Widerspruch zur aktuellen
ausdrücklichen Profilwahl gilt die Profilwahl, etwa „keine Tests“ im Startmodus gegen den Wirknachweis des Profils schnell
oder „ohne Nachfragen deployen“ gegen den Review-Checkpoint. Die übrigen AGENTS.md-Pflichten
bleiben vollständig bestehen und werden nie still gestrichen. Auftrag und Zwischenstand
nennen die Herkunft, zum Beispiel „gründlich; Launcher-Startmodus schnell aus AGENTS.md
übersteuert“.

Das Profil wirkt über den Auftragstext. Der Skill selbst ändert Modell, Effort, Start- und
Launcher-Profil, einen CLI- oder Launcher-Modus und die Rechte der Sitzung nicht: kein
Neustart und kein eigenes Umschalten in der CLI-Oberfläche, damit die laufende Sitzung
erhalten bleibt. Ein bestätigter abweichender Startmodus wird für die Runde ausdrücklich
übersteuert, soweit Rang 1 nichts anderes verlangt. Schaltet der Nutzer den Modus in der CLI selbst um,
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

- **Startwert:** Ohne ausdrückliche Nutzerwahl und ohne gespeichertes `profil` bestimmt ein
  bestätigter Launcher-Startmodus `schnell`, `normal` oder `gruendlich` das anfängliche
  Profil (`gruendlich` ergibt gründlich). Bei `frei`, unbestätigtem, unbekanntem oder
  widersprüchlichem Startmodus gilt `normal`; betrifft die Unklarheit einen folgenreichen
  Schritt, vorher kurz klären. Den abgeleiteten Startwert beim Anlegen von `ziel.json` als
  `profil` speichern und im ersten Auftrag samt Herkunft nennen, etwa „Profil: schnell
  (Startwert aus bestätigtem Launcher-Startmodus)“. Eine spätere ausdrückliche Nutzerwahl
  hat Vorrang; ein späterer Launcher-Neustart ändert ein gespeichertes `profil` nicht.
- **Nur eindeutige Formulierungen** wählen oder wechseln das Profil, etwa „Profil schnell“,
  „Schnellmodus“, „Normalmodus“, „Standardmodus“, „in den Gründlichkeitsmodus“, jeweils
  bezogen auf die Arbeitsweise. Bloße Dringlichkeit („mach schnell“, „zügig bitte“) oder
  Sorgfaltswünsche zu einem Einzelpunkt sind kein Wechsel. Bei echter Mehrdeutigkeit kurz
  nachfragen.
- **„Launcher-Modus“** meint ausdrücklich den Startmodus im OpenLauncher, kein Profil. Der
  Skill schaltet ihn nicht um und startet nichts neu; kurz melden, dass er erst beim nächsten
  Start der Sitzung wirkt und das Arbeitsprofil unverändert bleibt.
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

> Profil: schnell (gilt für diese Runde vor einem bestätigten Launcher-Startmodus;
> verbindliche Projektpflichten aus AGENTS.md und Projektregeln haben Vorrang).

Der Zwischenstand nennt das effektiv angewandte Profil samt Herkunft und Abweichung, zum
Beispiel „Profil: schnell (Nutzerwahl R5); Build und Installation nach AGENTS.md“ oder
„Profil: normal (Nutzerwahl R2); bestätigter Launcher-Startmodus schnell übersteuert“ oder
„Profil: normal (Standard); Startmodus unbestätigt“. Der Koordinator prüft, dass Umfang
und Prüfbelege zum Profil passen, und stuft bei Risiko hoch.
