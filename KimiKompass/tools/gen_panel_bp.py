# -*- coding: utf-8 -*-
from gen_slash import E, write

P = []
A = "/config"
def p(name, kat, kurz, eng, erk, sort):
    P.append(E(name, kat, A, kurz, eng, erk, sort=sort))

# ---- Model ----
p("Modell wählen", "Model", "Wählt das Sprachmodell aus einer Liste mit Reitern je Anbieter.",
  "Model — Switch the active model and thinking mode.",
  "Unter „Model“ öffnet sich dieselbe Auswahl wie bei `/model`. Oben gibt es Reiter: „All“ zeigt alle Modelle, dazu kommt ein Reiter pro Anbieter. Mit Tab und Umschalt+Tab wechselst du den Reiter.\n\nDie Liste ist durchsuchbar. Tipp einfach einen Teil des Namens.\n\nEnter übernimmt das Modell als neuen Standard (`default_model` in der `config.toml`). Ein Wechsel mitten im Gespräch macht den Prompt-Cache ungültig. Kimi weist dann darauf hin, dass `/new` zusätzliche Token-Kosten vermeidet.",
  "model 1")
p("Denkstufe", "Model", "Stellt ein, wie gründlich das Modell vor der Antwort nachdenkt.",
  "Select thinking effort",
  "Zu jedem Modell wählst du in derselben Auswahl die Denkstufe, zum Beispiel Low, High oder Max. Du wechselst mit den Pfeiltasten links und rechts.\n\nModelle, die immer nachdenken, haben keine Stufe „Off“. Bei den anderen kannst du das Nachdenken abschalten. Ist nichts gewählt, nimmt Kimi `default_effort` des Modells, sonst die mittlere Stufe.\n\nAuch ein Stufenwechsel macht den Prompt-Cache ungültig.",
  "model 2")
p("Nur für diese Sitzung (Alt+S)", "Model", "Übernimmt Modell oder Denkstufe nur für die laufende Sitzung.",
  "Alt+S session-only",
  "Drückst du statt Enter die Tasten Alt+S, gilt die Wahl nur für diese Sitzung. Die `config.toml` bleibt unverändert.\n\nBeispiel: Für eine einzelne knifflige Fehlersuche willst du die höchste Denkstufe, sonst aber sparen.",
  "model 3")
# ---- Permission ----
for i,(lab,val,desc,erk) in enumerate([
 ("Always Ask","manual","Auto-read only; everything else needs your approval first.",
  "Kimi darf Dateien selbst lesen. Alles andere, also Schreiben und Befehle, braucht vorher dein Okay.\n\nDas ist der vorsichtigste Modus. Er passt, wenn du ein fremdes Projekt erkundest oder Kimi noch nicht gut kennst."),
 ("Ask When Needed","yolo","Routine edits and commands run automatically; risky actions, questions, and plans still ask.",
  "Alltägliche Änderungen und Befehle laufen automatisch. Riskante Aktionen, Rückfragen und Pläne legt Kimi dir weiterhin vor.\n\nDas entspricht `/yolo`. Nach der Wahl zeigt Kimi die Beschreibung als Warnung an, damit du weißt, worauf du dich einlässt."),
 ("Never Ask","auto","Never interrupts you; everything runs and is decided automatically.",
  "Kimi unterbricht dich gar nicht mehr. Alle Werkzeuge laufen, Rückfragen entscheidet Kimi selbst.\n\nDas entspricht `/auto`. Nutze es nur in einem Projekt, das in Git gesichert ist.")]):
    p(lab, "Permission", f"Freigabe-Modus „{lab}“ (intern: {val}).", f"{lab} — {desc}",
      erk + "\n\nDie Wahl im Menü gilt für die laufende Sitzung. Einen dauerhaften Standard setzt du mit `default_permission_mode` in der `config.toml`.", f"permission {i+1}")
# ---- Theme ----
for i,(lab,erk) in enumerate([
 ("Auto (match terminal)","Kimi richtet sich nach deinem Terminal. Ist es hell, wird Kimi hell. Ist es dunkel, wird Kimi dunkel."),
 ("Dark","Dunkles Farbschema, egal wie das Terminal eingestellt ist."),
 ("Light","Helles Farbschema, egal wie das Terminal eingestellt ist."),
 ("Custom: <Name>","Eigene Farbschemata erscheinen hier mit dem Vorsatz „Custom:“. Ein solches Schema legst du mit dem eingebauten Skill `/custom-theme` an. Lässt es sich nicht laden, meldet Kimi einen Fehler und behält das alte Schema.")]):
    p(lab, "Theme", f"Farbschema „{lab}“.", f"Theme — Change the terminal UI theme. / {lab}",
      erk + "\n\nDie Wahl wird als `theme` in der `tui.toml` gespeichert.", f"theme {i+1}")
# ---- TUI mode ----
p("Regular", "TUI mode", "Normale Darstellung im Verlauf des Terminals.",
  "Regular — Render into the terminal's native scrollback.",
  "Kimi schreibt ganz normal in das Terminal. Du scrollst mit den Mitteln deines Terminals zurück.\n\nGespeichert wird `tui_mode = \"regular\"` in der `tui.toml`. Eine Änderung wirkt erst nach einem Neustart von Kimi Code.", "tui mode 1")
p("Fullscreen (experimental)", "TUI mode", "Eigener Vollbildschirm mit Scrollen, Markieren und Suche.",
  "Fullscreen (experimental) — Alternate screen with in-app scrolling, selection, and transcript search.",
  "Kimi nimmt das ganze Terminalfenster ein. Scrollen, Text markieren und die Suche im Gesprächsverlauf erledigt Kimi dann selbst.\n\nDer Modus ist noch experimentell. Gespeichert wird `tui_mode = \"fullscreen\"`. Die Änderung wirkt erst nach einem Neustart.", "tui mode 2")
# ---- Mermaid ----
p("Mermaid diagrams: On", "Mermaid diagrams", "Zeichnet Mermaid-Codeblöcke als Diagramm im Terminal.",
  "On — Draw mermaid code blocks as diagrams in the terminal.",
  "Mermaid ist eine Textsprache für Diagramme, etwa Ablaufpläne. Ist „On“ gewählt, zeichnet Kimi solche Blöcke aus Textzeichen.\n\nGespeichert wird `markdown.mermaid = \"final\"` in der `tui.toml`.", "mermaid diagrams 1")
p("Mermaid diagrams: Off", "Mermaid diagrams", "Zeigt Mermaid-Codeblöcke als farbigen Quelltext.",
  "Off — Keep mermaid code blocks as highlighted source.",
  "Die Diagramme bleiben als Text stehen, mit Farbhervorhebung. Das ist praktisch, wenn du den Diagramm-Code kopieren willst.\n\nGespeichert wird `markdown.mermaid = \"off\"`.", "mermaid diagrams 2")
# ---- Editor ----
for i,(lab,val,erk) in enumerate([
 ("VS Code (code --wait)","code --wait","Öffnet Visual Studio Code. `--wait` sorgt dafür, dass Kimi wartet, bis du die Datei schließt."),
 ("Vim","vim","Öffnet den Terminal-Editor Vim."),
 ("Neovim","nvim","Öffnet Neovim, eine modernere Variante von Vim."),
 ("Nano","nano","Öffnet Nano, einen einfachen Terminal-Editor für Einsteiger."),
 ("Auto-detect ($VISUAL / $EDITOR)","","Kimi nimmt den Editor aus den Umgebungsvariablen `KIMI_CODE_EDITOR`, `$VISUAL` oder `$EDITOR`.")]):
    p(lab, "Editor", f"Externer Editor: {lab}.", f"Editor — Set the external editor command. / {lab}",
      erk + f"\n\nDer Editor öffnet sich mit Strg+G für lange Eingaben. Gespeichert wird `[editor] command = \"{val}\"` in der `tui.toml`.", f"editor {i+1}")
# ---- Feedback survey ----
p("Feedback survey: On", "Feedback survey", "Zeigt ab und zu eine Bewertungsfrage.",
  "On — Show the occasional rating prompt above the editor.",
  "Gelegentlich erscheint über dem Eingabefeld die Bitte, die Sitzung zu bewerten.\n\nGespeichert wird `disable_feedback_survey = false` in der `tui.toml`.", "feedback survey 1")
p("Feedback survey: Off", "Feedback survey", "Blendet die Bewertungsfrage dauerhaft aus.",
  "Off — Never show the rating prompt.",
  "Die Bewertungsfrage erscheint nie mehr.\n\nGespeichert wird `disable_feedback_survey = true`.", "feedback survey 2")
# ---- Experiments ----
for i,(title,desc,idd,env,dflt,erk) in enumerate([
 ("NotifyUser tool","Show live progress updates from the main agent and subagents in the TUI Updates panel.","notify_user","KIMI_CODE_EXPERIMENTAL_NOTIFY_USER","aus",
  "Fortschrittsmeldungen vom Hauptagenten und von Helfern erscheinen live in einem Bereich „Updates“. Wurde die Sitzung ohne die Funktion gestartet, brauchst du danach eine neue Sitzung."),
 ("Fork context for subagents","Let the Agent and AgentSwarm tools start a subagent with a snapshot of the calling agent's conversation history via the fork parameter.","subagent_fork","KIMI_CODE_EXPERIMENTAL_SUBAGENT_FORK","aus",
  "Helfer können mit einer Kopie des bisherigen Gesprächs starten. Sie müssen dann nicht erst alles neu erklärt bekommen."),
 ("WaitFor tool","Give the model the WaitFor tool so it can wait for background tasks inside the current turn instead of ending the turn and being re-invoked.","wait_for","KIMI_CODE_EXPERIMENTAL_WAIT_FOR","an",
  "Das Modell kann innerhalb einer Runde auf Hintergrundaufgaben warten, statt die Runde zu beenden."),
 ("Tower mode","Enable tower mode: coordinate multiple agents on a shared objective, toggled with the /tower command.","tower","KIMI_CODE_EXPERIMENTAL_TOWER","aus",
  "Mehrere Agenten arbeiten gemeinsam an einem Ziel. Erst mit diesem Schalter erscheint der Befehl `/tower`."),
 ("Tool select (progressive tool disclosure)","Keep MCP tool schemas out of the immutable top-level tools[]; the model loads them on demand via the select_tools tool. Only takes effect on models whose capability catalog declares dynamically loaded tools.","tool-select","KIMI_CODE_EXPERIMENTAL_TOOL_SELECT","aus",
  "Beschreibungen von MCP-Werkzeugen werden erst geladen, wenn das Modell sie braucht. Das spart Platz im Kontextfenster, wirkt aber nur bei Modellen mit der Fähigkeit `dynamically_loaded_tools`.")]):
    p(title, "Experiments", f"Experimentelle Funktion „{title}“ (Standard: {dflt}).", f"{title}: {desc}",
      erk + f"\n\nIm Menü schaltest du mit der Leertaste um und übernimmst mit Enter („Apply changes and reload“). Kimi speichert das als `{idd}` im Abschnitt `[experimental]` der `config.toml` und lädt die Sitzung neu.\n\nIst `{env}` oder `KIMI_CODE_EXPERIMENTAL_FLAG` gesetzt, ist der Schalter gesperrt („locked by …“).",
      f"experiments {i+1}")
# ---- Automatic updates ----
p("Automatic updates: On", "Automatic updates", "Installiert neue Versionen im Hintergrund.",
  "On — Install new versions in the background.",
  "Kimi lädt Updates selbst und installiert sie im Hintergrund.\n\nGespeichert wird `[upgrade] auto_install = true` in der `tui.toml`.", "automatic updates 1")
p("Automatic updates: Off", "Automatic updates", "Fragt vor jeder Installation.",
  "Off — Show the install prompt instead.",
  "Statt still zu installieren, zeigt Kimi eine Frage an. So bestimmst du selbst, wann das Update kommt.\n\nGespeichert wird `auto_install = false`. Ganz abschalten lässt sich die Update-Prüfung mit der Umgebungsvariablen `KIMI_CODE_NO_AUTO_UPDATE`.", "automatic updates 2")
# ---- Usage ----
p("Usage", "Usage", "Zeigt Token-Verbrauch, Kontextfenster und Abo-Kontingent.",
  "Usage — Show session tokens, context window, and plan quotas.",
  "Hier stellst du nichts ein, du schaust nur. Es erscheint dieselbe Übersicht wie bei `/usage`: verbrauchte Tokens der Sitzung, Füllstand des Kontextfensters und übriges Kontingent deines Abos.",
  "usage 1")

write('panel_eintraege.json', 'panel', P)
print('panel', len(P))

# ================= Best Practices =================
BP = []
def b(name, kat, nr, kurz, erk):
    BP.append(E(name, kat, "Best Practice", kurz, "", erk, sort=f"{kat.lower()} {nr:03d}"))

b("AGENTS.md ist das Gedächtnis deines Projekts", "Kontext", 1,
  "Was in der AGENTS.md steht, muss Kimi nicht jedes Mal neu herausfinden.",
  "Kimi Code liest bei jedem Start die Datei `AGENTS.md`. Darin steht, wie dein Projekt gebaut und getestet wird und welche Regeln gelten. Ohne diese Datei muss Kimi das in jeder Sitzung neu erkunden.\n\nDen Anfang macht `/init`. Kimi untersucht das Projekt und schreibt die Datei. Lies sie danach durch und korrigiere Fehler. Ein falscher Testbefehl darin wird sonst in jeder Sitzung falsch benutzt.\n\nKimi sammelt Anleitungen von mehreren Stellen: persönliche in `~/.kimi-code/AGENTS.md`, fürs Projekt in `AGENTS.md` und `.kimi-code/AGENTS.md`, auch in Unterordnern. Greift Kimi auf einen Unterordner mit eigener `AGENTS.md` zu, bekommt es diese nachgereicht.\n\nHalte die Dateien knapp. Ab zusammen 32 KB warnt Kimi, dass große Anleitungen die Kosten erhöhen und die Qualität senken können.")
b("Erst planen, dann ändern", "Arbeitsweise", 1,
  "Ein Missverständnis fällt im Plan in einer Minute auf, im fertigen Code erst nach Stunden.",
  "Im Planungsmodus darf Kimi Code nur lesen und in eine Plan-Datei schreiben. Erst wenn du den Plan annimmst, wird am Projekt gearbeitet.\n\nDu schaltest ihn mit `/plan` ein. Beim Start geht das mit `kimi --plan`. Wer immer so arbeiten will, setzt `default_plan_mode = true` in der `config.toml`.\n\nAchte im Plan auf zwei Dinge. Welche Dateien werden angefasst, und stimmt das mit deiner Erwartung? Und in welcher Reihenfolge? Beispiel: Beim Umbenennen einer Funktion muss erst die Definition, dann jeder Aufruf geändert werden.\n\nIst ein Plan veraltet, räumt `/plan clear` ihn weg.")
b("Den Freigabe-Modus zur Lage passend wählen", "Sicherheit", 1,
  "Je weniger Kimi fragt, desto wichtiger ist eine saubere Sicherung in Git.",
  "Kimi Code hat drei Freigabe-Modi. „Always Ask“ fragt vor jeder Änderung. „Ask When Needed“ (`/yolo`) fragt nur bei Riskantem. „Never Ask“ (`/auto`) fragt nie.\n\nEine gute Faustregel: In einem fremden oder wichtigen Projekt startest du mit „Always Ask“. Kennst du die Aufgabe und ist alles in Git gesichert, darfst du auf „Ask When Needed“ gehen. „Never Ask“ lohnt sich vor allem für lange, gut beschriebene Ziele mit `/goal`, wenn du nicht am Rechner sitzt.\n\nDer eingebaute Schutz vor gefährlichen Befehlen (`permission.dangerous_command_guard`) ist standardmäßig an. Lass ihn an. Er ist die letzte Bremse, wenn in einem lockeren Modus etwas schiefläuft.\n\nVor einem Durchlauf ohne Rückfragen hilft ein kurzer Commit. Dann kannst du jede Änderung mit Git zurückholen.")
b("Feste Regeln statt ständiger Einzelfreigaben", "Sicherheit", 2,
  "Was du immer erlaubst oder nie willst, gehört in eine Regel, nicht in hundert Klicks.",
  "Wer oft dieselben Befehle freigibt, wechselt schnell genervt in einen lockeren Modus. Besser sind feste Regeln in der `config.toml` unter `[permission]`.\n\nJede Regel hat eine Entscheidung: `allow` (erlauben), `deny` (verbieten) oder `ask` (fragen). Dazu kommt ein Muster in der Form `Werkzeug(Argument)`. So kannst du etwa den Testbefehl deines Projekts immer erlauben und das Löschen bestimmter Ordner immer verbieten.\n\nSo bleibt der vorsichtige Modus angenehm, und die wirklich gefährlichen Dinge sind trotzdem ausgeschlossen.")
b("Das Gedächtnis schlank halten", "Kontext", 2,
  "Ein frisches, kurzes Gespräch ist schneller, günstiger und genauer als ein vollgestopftes.",
  "Jedes Modell hat ein begrenztes Kontextfenster. Je voller es ist, desto teurer wird jede Antwort und desto eher gehen Einzelheiten unter.\n\nFängst du ein neues Thema an, nimm `/new`. Willst du am selben Thema weiterarbeiten, aber der Verlauf ist lang, nimm `/compact` mit einer Anweisung, etwa `/compact Behalte die Liste der offenen Fehler`. Kimi verdichtet auch von selbst, im Code voreingestellt ab 85 Prozent Füllstand.\n\nWie voll das Fenster ist, zeigt `/usage`. Und noch ein Tipp: Ein Modell- oder Stufenwechsel mitten im Gespräch macht den Zwischenspeicher ungültig. Kimi empfiehlt dann selbst `/new`, um Kosten zu sparen.")
b("Ziele so formulieren, dass Kimi weiß, wann Schluss ist", "Arbeitsweise", 2,
  "Ein gutes Ziel nennt den Endzustand, die Prüfung und die Grenzen.",
  "Mit `/goal` arbeitet Kimi über viele Runden selbstständig an einem Auftrag. Das klappt nur, wenn klar ist, wann der Auftrag erfüllt ist. „Mach den Code besser“ hat kein Ende. „Alle Tests in `app/` laufen grün, ohne dass Tests gelöscht werden“ schon.\n\nDer eingebaute Skill `/write-goal` hilft beim Formulieren. Bei größeren Aufgaben baut er mit dir einen kurzen Block: Endzustand, Prüfungen, Grenzen und eine Regel, wann Kimi aufhört.\n\nStartest du ein Ziel im Modus „Always Ask“, fragt Kimi nach einem passenderen Modus. Sonst bleibt das Ziel bei jeder Rückfrage stehen und wartet auf dich.\n\nWährend das Ziel läuft, prüfst du mit `/goal status` den Stand und hältst es mit `/goal pause` an.")
b("Nebenfragen abzweigen statt das Hauptgespräch zu stören", "Arbeitsweise", 3,
  "Zwischenfragen gehören in einen Nebenzweig, nicht in den laufenden Auftrag.",
  "Mitten in einer Aufgabe fällt dir oft eine Frage ein, die nichts damit zu tun hat. Stellst du sie im Hauptgespräch, lenkt sie Kimi ab und füllt das Kontextfenster.\n\nMit `/btw` stellst du die Frage an einen Nebenagenten. Der kennt das bisherige Gespräch, arbeitet aber getrennt. Das geht sogar, während Kimi antwortet.\n\nWillst du zwei Lösungswege vergleichen, zweig mit `/fork` ab. Du bleibst im Original und probierst den zweiten Weg in der Kopie. Über `/sessions` wechselst du hin und her.")
b("Wiederkehrende Abläufe als Skill ablegen", "Erweitern", 1,
  "Was du dreimal erklärt hast, gehört in einen Skill.",
  "Ein Skill ist eine Anleitung in einer Datei `SKILL.md`, die Kimi auf Abruf lädt. Beispiel: „So schreibe ich die Versionshinweise für unser Projekt.“\n\nLeg Skills im Projekt unter `.agents/skills` oder `.kimi-code/skills` ab, persönliche unter `~/.kimi-code/skills`. Weitere Ordner trägst du in `extra_skill_dirs` ein. Aufgerufen wird ein Skill mit `/skill:<name>`.\n\nKommst du von Claude Code oder Codex, holt `/import-from-cc-codex` vorhandene Skills, Anleitungen und MCP-Einträge herüber. Es zeigt vorher eine Vorschau. Werden es viele Skills, ordnet `/sub-skill` sie zu Bündeln.")
b("MCP-Server gezielt anbinden und prüfen", "Erweitern", 2,
  "Jeder MCP-Server bringt Werkzeuge mit, und jede Werkzeugbeschreibung kostet Platz.",
  "Über MCP bekommt Kimi zusätzliche Werkzeuge, etwa für eine Datenbank oder einen Browser. Eingerichtet wird das in `mcp.json`. Am einfachsten geht es mit dem eingebauten Skill `/mcp-config`, der auch die Anmeldung per OAuth erledigt.\n\nOb alles läuft, zeigt `/mcp`: verbunden, fehlgeschlagen oder Anmeldung nötig, dazu die Zahl der Werkzeuge. Braucht ein Server lange zum Start, erhöhst du `mcp.startup_timeout_ms`.\n\nBinde nur an, was du wirklich brauchst. Jede Werkzeugbeschreibung belegt Kontext. Die experimentelle Funktion „Tool select“ lädt MCP-Werkzeuge erst bei Bedarf, wirkt aber nur bei passenden Modellen.")
b("Für Helfer ein günstigeres Modell nehmen", "Kosten", 1,
  "Suchen und Zusammenfassen braucht selten das stärkste Modell.",
  "Kimi startet für Teilaufgaben Unteragenten, zum Beispiel für eine Suche im Projekt. Diese Helfer müssen nicht dasselbe Modell nutzen wie das Hauptgespräch.\n\nMit `/secondary-model` gibst du ihnen ein schnelleres Modell. Dauerhaft steht das in `[secondary_model]` der `config.toml`. Das Hauptgespräch denkt weiter gründlich, die Helfer arbeiten flink und günstig.\n\nGenauso bei der Denkstufe: Für einfache Umbenennungen reicht `/effort` auf einer niedrigen Stufe. Die höchste Stufe hebst du dir für schwere Fehler auf. Mit Alt+S gilt sie nur für die laufende Sitzung.")
b("Produktfragen aus der Doku beantworten lassen", "Handwerk", 1,
  "Einstellungsnamen raten kostet Zeit, nachschlagen nicht.",
  "Auch ein KI-Modell kennt nicht jede Einstellung seines eigenen Programms. Es kann Namen erfinden, die es gar nicht gibt.\n\nKimi Code bringt dafür zwei Skills mit. `/check-kimi-code-docs` liest bei Fragen die offizielle Dokumentation und nennt die Quelle. `/update-config` ändert `config.toml` oder `tui.toml` nur nach einem Blick in die Doku und in die Datei. Erfinden soll er ausdrücklich nichts.\n\nNach Änderungen von Hand hilft `kimi doctor`: Der Befehl prüft die Konfigurationsdateien. Danach übernimmt `/reload` die neuen Werte in die laufende Sitzung.")
b("Wiederkehrende Handgriffe mit Hooks automatisieren", "Automatisieren", 1,
  "Was nach jeder Änderung passieren muss, soll nicht vom Gedächtnis abhängen.",
  "Hooks sind kleine Programme, die Kimi bei bestimmten Ereignissen selbst startet. Du trägst sie in der `config.toml` als `[[hooks]]` mit `event`, `command` und optional `matcher` und `timeout` ein.\n\nBeispiel: Beim Ereignis `PostToolUse` läuft nach jeder Dateiänderung dein Code-Formatierer. Oder bei `Stop` bekommst du eine Nachricht, wenn Kimi fertig ist.\n\nEs gibt über zwanzig Ereignisse, von `SessionStart` bis `PreCompact`. Fang mit einem einzigen Hook an und prüfe ihn, bevor du weitere baust.")

write('best_practices.json', 'praxis', BP)
print('praxis', len(BP))
