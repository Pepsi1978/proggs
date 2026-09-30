# -*- coding: utf-8 -*-
import json, os
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'kimi-assets')
os.makedirs(OUT, exist_ok=True)

def E(name, kat, art, kurz, eng, erk, entfernt=False, entferntIn="", ersatz="", sort=None):
    return {"name": name, "kategorie": kat, "art": art, "kurz": kurz, "englisch": eng,
            "erklaerung": erk, "seit": "", "seitBeleg": "",
            "sortierName": sort if sort is not None else name.lower().lstrip('/'),
            "entfernt": entfernt, "entferntIn": entferntIn, "ersatz": ersatz}

def write(fname, bereich, eintraege):
    with open(os.path.join(OUT, fname), 'w', encoding='utf-8', newline='\n') as f:
        json.dump({"bereich": bereich, "standVersion": "2.1.1", "eintraege": eintraege}, f, ensure_ascii=False, indent=2)
        f.write('\n')

S = []
B = "Eingebaut"
# ---------- Berechtigungen ----------
S.append(E("/yolo", "Berechtigungen", B,
 "Schaltet in den Modus „Ask When Needed“: Alltägliches läuft ohne Rückfrage.",
 "Ask When Needed mode: routine edits and commands run automatically; risky actions, questions, and plans still ask.",
 "Kimi Code fragt dich normalerweise, bevor es eine Datei ändert oder einen Befehl ausführt. Das ist sicher, kann aber nerven, wenn du viele kleine Änderungen erwartest.\n\nMit `/yolo` schaltest du in den Modus „Ask When Needed“ (auf Deutsch: fragen, wenn nötig). Normale Änderungen und Befehle laufen dann automatisch. Riskante Aktionen, Rückfragen an dich und Pläne landen aber weiterhin bei dir.\n\nBeispiel: Du lässt Kimi zwanzig Tippfehler in einem Projekt korrigieren. Mit `/yolo` musst du nicht zwanzigmal „Ja“ drücken. Die Kurzform `/yes` macht dasselbe. Beim Start geht das auch mit `kimi --yolo`."))
S.append(E("/auto", "Berechtigungen", B,
 "Schaltet in den Modus „Never Ask“: Kimi arbeitet ohne jede Unterbrechung.",
 "Never Ask mode: never interrupts you; everything runs and is decided automatically.",
 "Im Modus „Never Ask“ (auf Deutsch: nie fragen) unterbricht dich Kimi Code gar nicht mehr. Alle Werkzeuge werden automatisch erlaubt, und Kimi entscheidet Rückfragen selbst.\n\nDas ist praktisch, wenn du den Rechner eine Weile allein arbeiten lässt, zum Beispiel bei einem langen Ziel mit `/goal`. Es ist aber auch der riskanteste Modus: Niemand schaut vor einem Befehl noch einmal drauf.\n\nBenutze ihn am besten nur in einem Projekt, das in Git gesichert ist. Dann kannst du jede Änderung zurückholen. Beim Start geht das auch mit `kimi --auto`."))
S.append(E("/permission", "Berechtigungen", B,
 "Öffnet die Auswahl der drei Freigabe-Modi.",
 "Select permission mode",
 "Kimi Code kennt drei Freigabe-Modi. „Always Ask“ liest Dateien selbstständig, fragt aber vor allem anderen. „Ask When Needed“ erledigt Alltägliches allein und fragt nur bei Riskantem. „Never Ask“ fragt gar nicht mehr.\n\n`/permission` zeigt dir diese drei zur Auswahl. Du musst dir also die Befehle `/yolo` und `/auto` nicht merken.\n\nDie Wahl gilt für die laufende Sitzung. Willst du einen anderen Standard beim Start, trägst du `default_permission_mode` in die `config.toml` ein."))
# ---------- Einstellungen ----------
S.append(E("/settings", "Einstellungen", B,
 "Öffnet das Einstellungsmenü (Kurzform: /config).",
 "Open TUI settings",
 "`/settings` öffnet ein kleines Menü im Terminal. Dort findest du zehn Bereiche: Model, Permission, Theme, TUI mode, Mermaid diagrams, Editor, Feedback survey, Experiments, Automatic updates und Usage.\n\nSo musst du keine Konfigurationsdatei von Hand öffnen. Du wählst zum Beispiel unter „Theme“ ein helles Farbschema, und Kimi speichert das selbst in der Datei `tui.toml`.\n\nDer Befehl hat die Kurzform `/config`. Beide öffnen genau dasselbe Menü."))
S.append(E("/experiments", "Einstellungen", B,
 "Schaltet experimentelle Funktionen ein oder aus (Kurzform: /experimental).",
 "Manage experimental features",
 "Manche Funktionen sind noch in der Erprobung. Sie sind deshalb erst einmal ausgeschaltet oder nur für Mutige gedacht. `/experiments` zeigt dir eine Liste davon.\n\nMit der Leertaste schaltest du eine Funktion um, mit Enter übernimmst du alle Änderungen. Danach lädt Kimi die Sitzung neu. In Version 2.1.1 gibt es zum Beispiel „Tower mode“ (mehrere Agenten an einem Ziel) und „WaitFor tool“.\n\nDie Auswahl landet in der `config.toml` im Abschnitt `[experimental]`. Der Befehl geht nur, wenn Kimi gerade nicht antwortet."))
S.append(E("/reload", "Einstellungen", B,
 "Lädt die Sitzung neu und übernimmt Änderungen aus config.toml und tui.toml.",
 "Reload session and apply config.toml settings plus tui.toml UI preferences",
 "Wenn du die `config.toml` von Hand bearbeitet hast, weiß die laufende Sitzung davon noch nichts. `/reload` lädt die Sitzung neu und liest dabei beide Dateien wieder ein.\n\nBeispiel: Du hast in der `config.toml` ein neues Modell eingetragen. Nach `/reload` kannst du es sofort mit `/model` auswählen, ohne Kimi neu zu starten.\n\nDer Befehl geht nur, wenn Kimi gerade nicht antwortet."))
S.append(E("/reload-tui", "Einstellungen", B,
 "Lädt nur die Darstellungs-Einstellungen aus tui.toml neu.",
 "Reload only tui.toml UI preferences",
 "Die Datei `tui.toml` enthält nur Dinge, die das Aussehen betreffen: Farbschema, Editor, Statuszeile und Ähnliches.\n\n`/reload-tui` liest nur diese Datei neu ein. Die Sitzung selbst bleibt unberührt. Deshalb geht der Befehl auch, während Kimi gerade arbeitet.\n\nBeispiel: Du baust dir eine eigene Statuszeile und willst das Ergebnis sofort sehen."))
S.append(E("/editor", "Einstellungen", B,
 "Legt fest, welcher externe Editor sich mit Strg+G öffnet.",
 "Set the external editor for Ctrl-G",
 "Lange Nachrichten tippt man ungern in eine einzelne Terminalzeile. Mit Strg+G öffnet Kimi Code deshalb einen richtigen Texteditor. Dort schreibst du in Ruhe, speicherst, und der Text landet im Eingabefeld.\n\nMit `/editor` wählst du diesen Editor aus. Zur Wahl stehen VS Code (`code --wait`), Vim, Neovim, Nano oder die automatische Erkennung über die Umgebungsvariablen `$VISUAL` und `$EDITOR`.\n\nDie Wahl wird in der `tui.toml` unter `[editor] command` gespeichert."))
S.append(E("/theme", "Darstellung", B,
 "Wählt das Farbschema des Terminals.",
 "Set the terminal UI theme",
 "Mit `/theme` stellst du ein, wie Kimi Code im Terminal aussieht. „Auto“ passt sich an dein Terminal an. Außerdem gibt es „Dark“ und „Light“.\n\nHast du eigene Farbschemata angelegt, erscheinen sie als „Custom: Name“ in derselben Liste. Ein eigenes Schema baust du am einfachsten mit dem eingebauten Skill `/custom-theme`.\n\nDie Wahl steht danach in der `tui.toml` unter `theme`."))
# ---------- Arbeitsweise ----------
S.append(E("/plan", "Arbeitsweise", B,
 "Schaltet den Planungsmodus ein oder aus.",
 "Toggle plan mode",
 "Im Planungsmodus darf Kimi Code nichts am Projekt ändern. Es liest nur und schreibt einen Plan in eine eigene Plan-Datei. Erst wenn du den Plan annimmst, wird gearbeitet.\n\n`/plan` allein schaltet um. `/plan on` und `/plan off` schalten gezielt. `/plan clear` löscht den aktuellen Plan, aber nur, wenn Kimi gerade nicht antwortet.\n\nBeispiel: Vor einem großen Umbau lässt du dir erst zeigen, welche Dateien Kimi anfassen will. Ein Missverständnis fällt so auf, bevor Code geschrieben wird. Beim Start geht das mit `kimi --plan`."))
S.append(E("/goal", "Arbeitsweise", B,
 "Startet ein Ziel, an dem Kimi selbstständig weiterarbeitet, oder verwaltet es.",
 "Start or manage an autonomous goal",
 "Ein Ziel (englisch „goal“) ist ein Auftrag, an dem Kimi Code über viele Runden hinweg selbstständig arbeitet. Du schreibst zum Beispiel `/goal Alle Tests sollen grün sein`.\n\nMit Zusätzen steuerst du das Ziel: `status` zeigt den Stand, `pause` und `resume` halten es an und setzen es fort, `cancel` bricht ab, `replace` ersetzt es, und `next` stellt ein weiteres Ziel in die Warteschlange.\n\nIst der Freigabe-Modus „Always Ask“ aktiv, fragt Kimi vor dem Start, ob du lieber in „Never Ask“ oder „Ask When Needed“ wechseln willst. Sonst bleibt das Ziel bei jeder Rückfrage stehen. Beim Formulieren hilft der eingebaute Skill `/write-goal`."))
S.append(E("/swarm", "Arbeitsweise", B,
 "Schaltet den Schwarm-Modus um oder startet eine Aufgabe im Schwarm.",
 "Toggle swarm mode or run one task in swarm mode",
 "Im Schwarm-Modus (englisch „swarm“) verteilt Kimi Code eine Aufgabe auf mehrere Unteragenten, die parallel arbeiten.\n\n`/swarm on` und `/swarm off` schalten den Modus. Schreibst du stattdessen eine Aufgabe dahinter, etwa `/swarm Übersetze alle Hilfetexte`, läuft nur diese eine Aufgabe im Schwarm.\n\nWie bei `/goal` fragt Kimi im Modus „Always Ask“ vorher nach einem passenderen Freigabe-Modus. Der Befehl geht nur, wenn Kimi gerade nicht antwortet."))
S.append(E("/tower", "Arbeitsweise", B,
 "Steuert den experimentellen Tower-Modus (nur sichtbar, wenn eingeschaltet).",
 "Report tower status, toggle tower mode, or turn it on with a base branch",
 "Der Tower-Modus lässt mehrere Agenten gemeinsam an einem Ziel arbeiten. Er ist in Version 2.1.1 experimentell. Der Befehl erscheint erst, wenn du unter `/experiments` „Tower mode“ eingeschaltet hast.\n\nZusätze: `status` zeigt den Stand, `teardown` baut den Tower wieder ab, `on` und `off` schalten um. Du kannst auch einen Git-Zweig als Grundlage angeben, zum Beispiel `/tower main`.\n\nWeil die Funktion noch in Erprobung ist, solltest du sie nur in einem gesicherten Projekt ausprobieren."))
S.append(E("/btw", "Arbeitsweise", B,
 "Stellt eine Nebenfrage an einen abgezweigten Helfer, ohne das Hauptgespräch zu stören.",
 "Ask a forked side agent a question",
 "„btw“ steht für „by the way“, also „übrigens“. Mit `/btw` stellst du eine Zwischenfrage an einen Nebenagenten. Der bekommt eine Kopie des bisherigen Gesprächs.\n\nBeispiel: Kimi baut gerade eine Funktion um, und dir fällt ein, dass du wissen willst, wo eine Konstante definiert ist. Mit `/btw Wo steht MAX_RETRIES?` fragst du nebenher, ohne die laufende Arbeit zu unterbrechen.\n\nDer Befehl geht auch, während Kimi antwortet."))
S.append(E("/init", "Kontext und Gedächtnis", B,
 "Untersucht das Projekt und schreibt eine AGENTS.md.",
 "Analyze the codebase and generate AGENTS.md",
 "Die Datei `AGENTS.md` ist eine Anleitung für KI-Agenten. Kimi Code liest sie bei jedem Start und weiß dann, wie dein Projekt aufgebaut ist.\n\n`/init` lässt Kimi das Projekt erkunden und die Datei im Projektordner schreiben. Typische Abschnitte sind: Projektüberblick, Befehle zum Bauen und Testen, Code-Stil, Tests und Sicherheit. Gibt es die Datei schon, liest Kimi sie zuerst und übernimmt, was noch stimmt.\n\nPrüf das Ergebnis danach selbst. Was falsch drinsteht, wird Kimi in jeder Sitzung falsch machen."))
S.append(E("/compact", "Kontext und Gedächtnis", B,
 "Fasst das bisherige Gespräch zusammen, damit wieder Platz im Gedächtnis ist.",
 "Compact the conversation context",
 "Jedes Sprachmodell hat ein begrenztes Kurzzeitgedächtnis, das Kontextfenster. Wird ein Gespräch sehr lang, ist es irgendwann voll. `/compact` fasst das Gespräch zusammen und ersetzt den langen Verlauf durch die Zusammenfassung.\n\nDu kannst eine Anweisung mitgeben, worauf es ankommt. Beispiel: `/compact Behalte alle Dateinamen und offenen Fehler`.\n\nKimi verdichtet auch von selbst, wenn das Gedächtnis fast voll ist. Den Zeitpunkt steuerst du in der `config.toml` mit `loop_control.compaction_trigger_ratio`."))
# ---------- Modell ----------
S.append(E("/model", "Modell und Antworten", B,
 "Wechselt das Sprachmodell.",
 "Switch LLM model",
 "Mit `/model` wählst du, welches Sprachmodell antwortet. Die Liste hat Reiter: „All“ zeigt alle Modelle, dazu kommt je ein Reiter pro Anbieter. Mit Tab springst du zwischen den Reitern.\n\nIn derselben Auswahl stellst du auch die Denktiefe ein. Enter speichert die Wahl als neuen Standard. Alt+S gilt nur für die laufende Sitzung.\n\nAchtung: Ein Modellwechsel mitten im Gespräch macht den Zwischenspeicher (Prompt-Cache) ungültig. Das kostet mehr Tokens. Kimi empfiehlt dann einen Neustart mit `/new`."))
S.append(E("/secondary-model", "Modell und Antworten", B,
 "Legt das Modell für Unteragenten fest (Kurzform: /subagent-model).",
 "Configure the secondary model for subagents",
 "Unteragenten sind Helfer, die Kimi Code für Teilaufgaben startet, zum Beispiel für eine Suche im Projekt. Sie müssen nicht dasselbe Modell benutzen wie das Hauptgespräch.\n\nMit `/secondary-model` wählst du ein eigenes Modell für diese Helfer. Ein schnelleres, günstigeres Modell reicht für Suchaufträge oft völlig aus.\n\nDauerhaft steht die Einstellung in der `config.toml` im Abschnitt `[secondary_model]`."))
S.append(E("/effort", "Modell und Antworten", B,
 "Wechselt die Denktiefe des Modells (Kurzform: /thinking).",
 "Switch thinking effort",
 "Viele Modelle können vor der Antwort „nachdenken“. Die Denktiefe (englisch „effort“) bestimmt, wie gründlich. Typische Stufen sind `low`, `high` und `max`.\n\nMehr Denken hilft bei kniffligen Fehlern, dauert aber länger und kostet mehr. Für einfache Umbenennungen reicht meist eine niedrige Stufe.\n\nDu wählst mit den Pfeiltasten links und rechts. Enter übernimmt, Alt+S gilt nur für diese Sitzung. Auch hier gilt: Ein Wechsel macht den Prompt-Cache ungültig."))
S.append(E("/provider", "Modell und Antworten", B,
 "Verwaltet KI-Anbieter: hinzufügen, löschen, aktualisieren (Kurzform: /providers).",
 "Manage AI providers (add / delete / refresh)",
 "Ein Anbieter (englisch „provider“) ist der Dienst, der das Sprachmodell bereitstellt, zum Beispiel Kimi selbst oder ein anderer Dienst mit API-Schlüssel.\n\nMit `/provider` fügst du Anbieter hinzu, löschst sie oder lässt die Modellliste neu laden. Die Anbieter stehen danach in der `config.toml` unter `[providers.…]`.\n\nOhne Terminal-Oberfläche geht dasselbe mit dem Unterbefehl `kimi provider`."))
# ---------- Sitzungen ----------
S.append(E("/new", "Agenten und Sitzungen", B,
 "Beginnt eine frische Sitzung im selben Projekt (Kurzform: /clear).",
 "Start a fresh session in the current workspace",
 "`/new` beendet das aktuelle Gespräch nicht endgültig, sondern legt es ab und startet ein leeres. Kimi Code vergisst dabei alles, was im alten Gespräch stand.\n\nDas lohnt sich, sobald du ein neues Thema anfängst. Ein frisches Gespräch ist schneller, günstiger und genauer als ein vollgestopftes.\n\nDas alte Gespräch findest du später über `/sessions` wieder. Die Kurzform `/clear` macht dasselbe."))
S.append(E("/sessions", "Agenten und Sitzungen", B,
 "Zeigt frühere Sitzungen und setzt eine davon fort (Kurzform: /resume).",
 "Browse and resume sessions",
 "Kimi Code speichert jede Sitzung. Mit `/sessions` blätterst du durch die Liste und öffnest eine alte Sitzung wieder. Das Gespräch geht dann genau dort weiter, wo es aufgehört hat.\n\nBeispiel: Gestern hast du mitten in einer Fehlersuche Feierabend gemacht. Heute holst du die Sitzung zurück, statt alles neu zu erklären.\n\nBeim Start geht das mit `kimi -S` oder `kimi --continue` für die letzte Sitzung im Ordner."))
S.append(E("/fork", "Agenten und Sitzungen", B,
 "Kopiert die aktuelle Sitzung, ohne zu ihr zu wechseln.",
 "Fork the current session into a copy without switching to it",
 "Eine Abzweigung (englisch „fork“) ist eine Kopie des Gesprächs bis zum jetzigen Punkt. Du bleibst dabei in der ursprünglichen Sitzung.\n\nKimi zeigt dir danach einen fertigen Befehl, mit dem du die Kopie in einem zweiten Terminal öffnen kannst, und legt ihn in die Zwischenablage. Alternativ wechselst du über `/sessions`.\n\nBeispiel: Du willst zwei Lösungswege ausprobieren. Du zweigst ab und testest den zweiten Weg in der Kopie."))
S.append(E("/undo", "Agenten und Sitzungen", B,
 "Nimmt deine letzte Nachricht aus dem Gesprächsverlauf zurück.",
 "Withdraw the last prompt from the transcript",
 "Manchmal schickt man eine Nachricht ab und merkt sofort, dass sie falsch war. `/undo` entfernt deine letzte Nachricht wieder aus dem Verlauf.\n\nMit einer Zahl nimmst du mehrere zurück, etwa `/undo 3`. Der Befehl geht nur, wenn Kimi gerade nicht antwortet. Drücke vorher Esc oder Strg+C.\n\nWichtig: Der Befehl räumt den Gesprächsverlauf auf. Für Dateiänderungen verlässt du dich besser auf Git."))
S.append(E("/title", "Agenten und Sitzungen", B,
 "Zeigt oder setzt den Titel der Sitzung (Kurzform: /rename).",
 "Set or show session title",
 "Jede Sitzung hat einen Titel. Er erscheint in der Liste unter `/sessions`.\n\nOhne Zusatz zeigt `/title` den aktuellen Titel. Mit Zusatz setzt du einen neuen, zum Beispiel `/title Login-Fehler Android`.\n\nEin guter Titel spart später Suchzeit, wenn du viele Sitzungen hast."))
S.append(E("/tasks", "Agenten und Sitzungen", B,
 "Zeigt laufende und fertige Hintergrundaufgaben (Kurzform: /task).",
 "Browse background tasks",
 "Manche Arbeiten laufen im Hintergrund weiter, zum Beispiel ein langer Build oder ein Unteragent. `/tasks` zeigt dir eine Liste dieser Hintergrundaufgaben.\n\nDort kannst du die Ausgabe einer Aufgabe ansehen und prüfen, ob sie noch läuft.\n\nWie viele Aufgaben gleichzeitig laufen dürfen, regelst du in der `config.toml` unter `[task] max_running_tasks`."))
S.append(E("/exit", "Agenten und Sitzungen", B,
 "Beendet Kimi Code (Kurzformen: /quit, /q).",
 "Exit the application",
 "`/exit` schließt das Programm. Die Sitzung bleibt gespeichert.\n\nDu kannst sie später mit `/sessions` oder `kimi --continue` wieder aufnehmen."))
# ---------- Arbeitsumgebung ----------
S.append(E("/add-dir", "Arbeitsumgebung", B,
 "Gibt Kimi Zugriff auf einen weiteren Ordner oder listet die zusätzlichen Ordner.",
 "Add or list an additional workspace directory",
 "Normalerweise arbeitet Kimi Code nur in dem Ordner, in dem du es gestartet hast. Mit `/add-dir ../gemeinsame-bibliothek` erlaubst du einen weiteren Ordner.\n\nBeim Tippen des Pfads schlägt Kimi passende Ordner vor. `/add-dir list` zeigt alle zusätzlichen Ordner.\n\nDer Befehl geht nur, wenn Kimi gerade nicht antwortet. Beim Start geht dasselbe mit `kimi --add-dir <Ordner>`."))
# ---------- Hilfe / Info ----------
S.append(E("/help", "Hilfe", B,
 "Zeigt alle Befehle und Tastenkürzel (Kurzformen: /h, /?).",
 "Show available commands and shortcuts",
 "`/help` öffnet eine Übersicht aller Slash-Befehle und Tastenkürzel.\n\nTipp: Schon ein einzelnes `/` im Eingabefeld zeigt eine Liste mit Vorschlägen. Tippst du weiter, wird sie gefiltert."))
S.append(E("/version", "Hilfe", B,
 "Zeigt die installierte Version.",
 "Show version information",
 "`/version` zeigt, welche Version von Kimi Code läuft, zum Beispiel 2.1.1.\n\nDas brauchst du, wenn du einen Fehler meldest oder prüfen willst, ob ein Update angekommen ist. Im Terminal geht dasselbe mit `kimi -V`."))
S.append(E("/status", "Hilfe", B,
 "Zeigt den Zustand der Sitzung und der Laufzeitumgebung.",
 "Show current session and runtime status",
 "`/status` zeigt Angaben zur laufenden Sitzung und zur Laufzeitumgebung von Kimi Code.\n\nDas ist der schnellste Weg, um zu prüfen, ob eine Einstellung wirklich angekommen ist."))
S.append(E("/usage", "Kosten", B,
 "Zeigt verbrauchte Tokens, Kontextfenster und Kontingente deines Abos.",
 "Show session tokens + context window + plan quotas",
 "Tokens sind die kleinen Textbausteine, nach denen Sprachmodelle abrechnen. `/usage` zeigt, wie viele diese Sitzung schon verbraucht hat.\n\nDazu siehst du, wie voll das Kontextfenster ist und wie viel von deinem Abo-Kontingent übrig ist.\n\nIst das Kontextfenster fast voll, hilft `/compact` oder ein neuer Anfang mit `/new`."))
# ---------- Integrationen ----------
S.append(E("/mcp", "Integrationen", B,
 "Zeigt den Zustand der MCP-Server.",
 "Show MCP server status",
 "MCP (Model Context Protocol) ist ein Standard, über den Kimi Code zusätzliche Werkzeuge anbindet, etwa einen Datenbankzugang oder einen Browser.\n\n`/mcp` zeigt jeden eingerichteten MCP-Server mit Zustand: verbunden, fehlgeschlagen, Anmeldung nötig oder abgeschaltet. Dazu steht, wie viele Werkzeuge er liefert.\n\nZum Einrichten und für die Anmeldung gibt es den eingebauten Skill `/mcp-config`."))
S.append(E("/plugins", "Integrationen", B,
 "Verwaltet Plugins.",
 "Manage plugins",
 "Plugins sind Erweiterungspakete. Sie können Befehle, Skills und weitere Funktionen mitbringen.\n\n`/plugins` öffnet die Plugin-Verwaltung. Befehle aus Plugins rufst du danach als `/plugin-name:befehl` auf."))
S.append(E("/web", "Integrationen", B,
 "Öffnet die aktuelle Sitzung in der Web-Oberfläche.",
 "Open the current session in the Web UI by starting a new server",
 "Kimi Code hat neben dem Terminal auch eine Oberfläche im Browser. `/web` startet dafür einen lokalen Server und öffnet die laufende Sitzung dort.\n\nDas ist angenehm, wenn du lange Ausgaben lesen oder Bilder ansehen willst. Im Terminal geht dasselbe mit `kimi web`."))
S.append(E("/desktop", "Integrationen", B,
 "Öffnet die Seite der Desktop-App im Browser (Kurzform: /install-desktop).",
 "Open the Kimi Code desktop app page in your browser",
 "Es gibt Kimi Code auch als Desktop-Programm. `/desktop` öffnet die Download-Seite dafür im Browser.\n\nDu musst also nicht selbst danach suchen."))
S.append(E("/remote-control", "Integrationen", B,
 "Macht die Sitzung über Kimi Remote Control erreichbar (Kurzform: /rc).",
 "Open the current session through Kimi Remote Control",
 "Mit Remote Control (Fernsteuerung) erreichst du eine laufende Sitzung von einem anderen Gerät aus, etwa vom Handy.\n\n`/remote-control` öffnet die Sitzung dafür über den Relay-Dienst von Kimi. Im Terminal geht dasselbe mit `kimi rc`."))
# ---------- Konto ----------
S.append(E("/login", "Konto", B,
 "Wählt eine Plattform und meldet dich an.",
 "Select a platform and authenticate",
 "Bevor Kimi Code antworten kann, musst du angemeldet sein. `/login` fragt zuerst nach der Plattform und startet dann die Anmeldung.\n\nIm Terminal geht dasselbe mit `kimi login`. Dort läuft die Anmeldung über einen Gerätecode, den du im Browser bestätigst."))
S.append(E("/logout", "Konto", B,
 "Meldet dich bei einem eingerichteten Anbieter ab (Kurzform: /disconnect).",
 "Log out of a configured provider",
 "`/logout` entfernt die Anmeldung bei einem Anbieter. Danach kann Kimi über diesen Anbieter keine Modelle mehr nutzen, bis du dich wieder anmeldest.\n\nDas ist sinnvoll, wenn du den Rechner abgibst oder das Konto wechseln willst."))
# ---------- Ausgabe ----------
S.append(E("/export-md", "Ausgabe und Teilen", B,
 "Speichert die Sitzung als Markdown-Datei (Kurzform: /export).",
 "Export current session as a Markdown file",
 "Markdown ist ein einfaches Textformat mit Überschriften und Listen. `/export-md` schreibt das ganze Gespräch in so eine Datei.\n\nBeispiel: Du willst einer Kollegin zeigen, wie du einen Fehler gefunden hast. Die Datei kannst du einfach weiterschicken."))
S.append(E("/export-debug-zip", "Fehlersuche", B,
 "Packt die Sitzung als ZIP-Archiv zur Fehleranalyse.",
 "Export current session as a debug ZIP archive",
 "Wenn Kimi Code sich merkwürdig verhält, braucht das Entwicklerteam oft die genauen Daten der Sitzung. `/export-debug-zip` packt sie in ein ZIP-Archiv.\n\nSchau vor dem Weitergeben hinein. Das Archiv kann Inhalte aus deinem Projekt enthalten. Im Terminal geht dasselbe mit `kimi export`."))
S.append(E("/copy", "Ausgabe und Teilen", B,
 "Kopiert die letzte Antwort in die Zwischenablage.",
 "Copy the last assistant message to the clipboard",
 "`/copy` legt die letzte Antwort von Kimi in die Zwischenablage. Du kannst sie dann in eine Mail, ein Ticket oder ein Dokument einfügen.\n\nDas ist bequemer, als lange Terminalausgaben mit der Maus zu markieren."))
S.append(E("/feedback", "Rückmeldung", B,
 "Schickt eine Rückmeldung an das Kimi-Team (Kurzform: /bug).",
 "Send feedback to make Kimi Code better",
 "Mit `/feedback` schreibst du dem Team hinter Kimi Code direkt aus dem Terminal. Das kann ein Lob, ein Wunsch oder eine Fehlermeldung sein.\n\nDie Kurzform `/bug` passt, wenn du einen Fehler melden willst."))

# ---------- Aliase ----------
ALIAS = [("/yes","/yolo"),("/config","/settings"),("/subagent-model","/secondary-model"),("/thinking","/effort"),
 ("/providers","/provider"),("/h","/help"),("/?","/help"),("/clear","/new"),("/resume","/sessions"),("/task","/tasks"),
 ("/experimental","/experiments"),("/rename","/title"),("/bug","/feedback"),("/disconnect","/logout"),
 ("/export","/export-md"),("/install-desktop","/desktop"),("/rc","/remote-control"),("/quit","/exit"),("/q","/exit")]
haupt = {e["name"]: e for e in S}
for a, z in ALIAS:
    h = haupt[z]
    S.append(E(a, h["kategorie"], "Alias", f"Kurzform von {z}.", h["englisch"],
     f"`{a}` ist ein anderer Name für `{z}`. Beide machen genau dasselbe.\n\n{h['kurz']}",
     sort=a.lower().lstrip('/') or '?'))

# ---------- Eingebaute Skills ----------
SK = "Mitgelieferter Skill"
S.append(E("/check-kimi-code-docs", "Hilfe", SK,
 "Beantwortet Fragen zu Kimi Code aus der offiziellen Dokumentation.",
 "Answer questions about the Kimi Code product using the official documentation — CLI usage, configuration, slash commands, features, membership and quota, API onboarding, third-party tool setup, and error codes.",
 "Ein Skill ist eine fertige Arbeitsanleitung, die Kimi Code auf Abruf lädt. Dieser Skill sorgt dafür, dass Kimi Produktfragen nicht aus dem Gedächtnis beantwortet. Stattdessen holt es die passende Seite der offiziellen Dokumentation unter `www.kimi.com/code/docs/en/`.\n\nBeispiel: `/check-kimi-code-docs Was bedeutet Fehler 401?` Kimi sucht die Seite mit den Fehlercodes, liest sie und nennt dir am Ende die Quelle.\n\nFindet es nichts, sagt es das ausdrücklich. Er gehört zu den Produkt-Skills, die man mit `builtin_product_skills = false` abschalten kann."))
S.append(E("/custom-theme", "Darstellung", SK,
 "Erstellt oder bearbeitet ein eigenes Farbschema.",
 "Create or edit a kimi-code custom color theme",
 "Mit diesem Skill baust du dir ein eigenes Farbschema für das Terminal. Du beschreibst, was du willst, etwa „dunkel mit grünen Akzenten“, und Kimi legt die Schema-Datei an.\n\nDanach erscheint das Schema unter `/theme` als „Custom: Name“. Der Skill ist nur in der Terminal-Oberfläche verfügbar."))
S.append(E("/import-from-cc-codex", "Einrichtung", SK,
 "Übernimmt Anleitungen, Skills und MCP-Einstellungen aus Claude Code und Codex.",
 "Import Claude Code and Codex instructions, skills, and MCP settings into Kimi Code.",
 "Wer vorher mit Claude Code oder Codex gearbeitet hat, hat dort oft schon Anleitungen (`CLAUDE.md`, `AGENTS.md`), Skills und MCP-Server eingerichtet. Dieser Skill holt sie zu Kimi Code herüber.\n\nEr fragt zuerst, was du übernehmen willst: Anleitungen, Skills, MCP oder alles. Dann zeigt er dir eine Vorschau mit Quell- und Zielpfaden. Erst nach deinem Okay schreibt er etwas.\n\nVorhandene Skills überschreibt er nicht, und eine vorhandene `AGENTS.md` oder `mcp.json` ersetzt er nicht komplett. Anleitungen landen zum Beispiel in `~/.kimi-code/AGENTS.md` oder im Projekt unter `.kimi-code/AGENTS.md`."))
S.append(E("/mcp-config", "Integrationen", SK,
 "Richtet MCP-Server ein und erledigt die MCP-Anmeldung.",
 "Configure MCP servers and handle MCP OAuth login.",
 "MCP-Server werden in einer Datei namens `mcp.json` eingetragen. Dieser Skill hilft dir dabei, sie zu bearbeiten, ohne die Syntax auswendig zu kennen.\n\nEr kümmert sich auch um die Anmeldung, wenn ein MCP-Server eine OAuth-Anmeldung verlangt. OAuth ist das bekannte Verfahren „Mit Konto anmelden“ im Browser.\n\nBeispiel: `/mcp-config Füge den GitHub-Server hinzu`."))
S.append(E("/update-config", "Einstellungen", SK,
 "Liest oder ändert Kimis eigene Konfiguration mit Blick in die Doku.",
 "Inspect or edit kimi-code's own config",
 "Dieser Skill ändert `config.toml` oder `tui.toml` für dich. Er hält sich dabei an feste Regeln: erst die offizielle Doku lesen, dann die Zieldatei lesen, dann ändern.\n\nSchlüsselnamen erfindet er nicht. Ist die Doku nicht erreichbar, sagt er dir das und fragt nach, statt blind zu ändern.\n\nBeispiel: `/update-config Schalte die Benachrichtigungen aus`."))
S.append(E("/write-goal", "Arbeitsweise", SK,
 "Hilft dir, einen guten Auftrag für /goal zu formulieren.",
 "Help the user craft a well-specified `/goal` objective for goal mode",
 "Ein Ziel für `/goal` muss klar sein. Kimi soll erkennen können, wann es fertig ist. Dieser Skill schreibt mit dir zusammen einen Entwurf.\n\nBei einfachen Aufgaben sind das ein oder zwei Sätze. Bei größeren entsteht ein kurzer Block mit Endzustand, Prüfungen, Grenzen und einer Regel, wann Schluss ist.\n\nKimi erklärt dir jede Wahl im Entwurf. Ihr überarbeitet ihn so lange, bis er passt."))
S.append(E("/sub-skill", "Erweiterungen", SK,
 "Ordnet viele Skills in übersichtliche Bündel mit Unter-Skills.",
 "Discover and reorganize the skill inventory into hierarchical sub-skill bundles. Use when the user asks to review, group, or consolidate skills into a parent bundle.",
 "Wer viele Skills sammelt, verliert schnell den Überblick. Dieser Skill fasst verwandte Skills zu einem Bündel mit Unter-Skills zusammen.\n\nEr arbeitet in zwei Schritten. „review“ schaut nur und schlägt Gruppen vor, ändert aber nichts. „consolidate“ setzt einen freigegebenen Vorschlag um und legt vorher von jedem geänderten Ordner eine Sicherung mit Zeitstempel an."))
# ---------- Muster ----------
S.append(E("/skill:<name>", "Erweiterungen", "Muster",
 "Ruft einen deiner eigenen Skills direkt auf.",
 "",
 "Eigene Skills legst du als Ordner mit einer Datei `SKILL.md` ab. Kimi Code sucht sie zum Beispiel in `.agents/skills` oder `.kimi-code/skills` im Projekt und in `~/.kimi-code/skills`.\n\nJeder eigene Skill erscheint als Befehl mit dem Vorsatz `skill:`. Heißt dein Skill `release-notes`, rufst du ihn mit `/skill:release-notes` auf. Oft reicht auch `/release-notes`, solange kein eingebauter Befehl so heißt.\n\nAlles, was du hinter den Befehl schreibst, bekommt der Skill als Auftrag mit.", sort="skill"))
S.append(E("/<plugin>:<befehl>", "Erweiterungen", "Muster",
 "Ruft einen Befehl auf, den ein Plugin mitbringt.",
 "",
 "Plugins können eigene Befehle mitbringen. Damit sich Namen nicht überschneiden, steht der Plugin-Name davor, getrennt durch einen Doppelpunkt.\n\nBeispiel: Ein Plugin namens `docs` mit dem Befehl `sync` rufst du mit `/docs:sync` auf. Solche Befehle gehen nur, wenn Kimi gerade nicht antwortet.", sort="plugin"))

write('slash_befehle.json', 'slash', S)
print('slash', len(S))
