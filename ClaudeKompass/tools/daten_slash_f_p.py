# -*- coding: utf-8 -*-
"""Slash-Befehle F bis P."""

SLASH_F_P = [
 ("/fast", "Modell und Antworten", "Eingebaut",
  "Schaltet den schnellen Modus ein oder aus.",
  "Turn fast mode on or off",
  "Im schnellen Modus laeuft dasselbe Modell, gibt seine Antwort aber zuegiger aus. Es wird "
  "also nicht auf ein kleineres, schwaecheres Modell umgeschaltet — das ist ein haeufiges "
  "Missverstaendnis.\n\n"
  "`/fast on` schaltet ein, `/fast off` wieder aus. Ohne Zusatz wird umgeschaltet.\n\n"
  "Der Modus steht nicht bei jedem Modell zur Verfuegung. Bei den Opus-Modellen ab Version 4.8 "
  "ist er vorhanden.\n\n"
  "Dauerhaft laesst er sich ueber die Einstellung `fastMode` festlegen."),

 ("/feedback", "Rueckmeldung", "Eingebaut",
  "Schickt eine Rueckmeldung zu Claude Code an Anthropic.",
  "Send product feedback about Claude Code",
  "Hier sagst du, was dir fehlt, was dich stoert oder was du dir wuenschst. Anders als `/bug` "
  "geht es nicht um einen konkreten Fehler, sondern um das Produkt insgesamt.\n\n"
  "Die Rueckmeldung wird als Entwurf angelegt und erst nach deiner ausdruecklichen Zustimmung "
  "abgeschickt. Nichts geht ohne dein Zutun raus.\n\n"
  "Nuetzlich sind konkrete Angaben: Was wolltest du tun, was ist passiert, was haettest du "
  "erwartet.\n\n"
  "Wie oft eine Zufriedenheitsfrage erscheint, steuerst du ueber `feedbackSurveyRate`."),

 ("/fewer-permission-prompts", "Berechtigungen", "Mitgelieferter Skill",
  "Erstellt aus deinen bisherigen Sitzungen eine Freigabeliste fuer haeufige Befehle.",
  "Scan transcripts and add a prioritized allowlist for common tool calls",
  "Wenn Claude staendig nachfragt, ob es `git status` ausfuehren darf, kostet das Nerven. "
  "Dieser Befehl schaut sich an, welche harmlosen Befehle du immer wieder freigibst.\n\n"
  "Daraus baut er eine Freigabeliste, die in die Projekt-Einstellungen wandert. Diese Befehle "
  "laufen danach ohne Rueckfrage.\n\n"
  "Wichtig: Es werden nur lesende, ungefaehrliche Aufrufe vorgeschlagen. Etwas, das Dateien "
  "loescht, landet nicht darauf.\n\n"
  "Sieh die vorgeschlagene Liste trotzdem durch, bevor du sie uebernimmst — sie nimmt dir "
  "kuenftig eine Kontrolle ab."),

 ("/focus", "Darstellung", "Eingebaut",
  "Blendet alles aus bis auf deine Eingabe, eine kurze Werkzeugzeile und die Antwort.",
  "Toggle focus view: prompt, tool summary, and response only",
  "Claude Code zeigt normalerweise viel: jeden Werkzeugaufruf, jede gelesene Datei, jede "
  "Zwischenmeldung. Das ist beim Nachvollziehen gut, beim Lesen stoerend.\n\n"
  "`/focus` raeumt auf. Uebrig bleiben deine Eingabe, eine knappe Zusammenfassung dessen, was "
  "getan wurde, und die eigentliche Antwort.\n\n"
  "Ein zweiter Aufruf schaltet wieder zurueck.\n\n"
  "Praktisch, wenn du jemandem ueber die Schulter schauen laesst oder einen Bildschirm teilst."),

 ("/fork", "Agenten und Sitzungen", "Eingebaut",
  "Kopiert das laufende Gespraech in eine neue Sitzung im Hintergrund.",
  "Copy the current conversation into a new background session",
  "`/fork` erzeugt eine Zwillingssitzung: Sie kennt alles, was bisher besprochen wurde, laeuft "
  "aber getrennt weiter — und zwar im Hintergrund.\n\n"
  "Damit kannst du einen zweiten Weg verfolgen, ohne dein Hauptgespraech zu unterbrechen. Der "
  "Zwilling arbeitet, waehrend du weitermachst.\n\n"
  "Du kannst gleich einen Auftrag mitgeben: `/fork probiere die Loesung mit der zweiten "
  "Bibliothek`.\n\n"
  "Der Unterschied zu `/branch`: `/branch` bleibt in deinem Fenster, `/fork` laeuft als eigene "
  "Hintergrundsitzung."),

 ("/goal", "Arbeitsweise", "Eingebaut",
  "Setzt ein Ziel, an dem Claude ueber mehrere Schritte hinweg arbeitet.",
  "Set a goal that Claude works toward across turns until a condition is met",
  "Normalerweise antwortet Claude auf deine Eingabe und wartet dann. Mit `/goal` gibst du ein "
  "Ziel vor, an dem es selbstaendig weiterarbeitet, bis die Bedingung erfuellt ist.\n\n"
  "Ein Beispiel: `/goal alle Tests laufen gruen`. Claude wiederholt dann Aendern und Testen so "
  "lange, bis das erreicht ist.\n\n"
  "Die Bedingung muss ueberpruefbar sein. „Mach den Code schoen“ ist kein Ziel, „der Linter "
  "meldet keine Warnung mehr“ schon.\n\n"
  "Mit `/goal clear` nimmst du das Ziel wieder zurueck."),

 ("/help", "Hilfe", "Eingebaut",
  "Zeigt die Hilfe und alle verfuegbaren Befehle.",
  "Show help and available commands",
  "`/help` ist der Einstieg, wenn du nicht weiterweisst. Es listet die eingebauten Befehle "
  "und die verfuegbaren Skills mit einer kurzen Beschreibung auf.\n\n"
  "Die Liste passt sich an: Eigene Skills und Erweiterungen, die du installiert hast, stehen "
  "mit drin.\n\n"
  "Ausserdem findest du dort die wichtigsten Tastenkuerzel.\n\n"
  "Genau diese Liste hat auch diese App als Grundlage — nur ausfuehrlicher erklaert und "
  "auf Deutsch."),

 ("/hooks", "Automatisierung", "Eingebaut",
  "Zeigt, welche eigenen Befehle bei welchen Ereignissen automatisch laufen.",
  "View hook configurations for tool events",
  "Ein Hook ist ein eigener Befehl, den Claude Code an einer festgelegten Stelle automatisch "
  "ausfuehrt: bevor ein Werkzeug laeuft, nachdem eine Datei geschrieben wurde, beim Start "
  "einer Sitzung.\n\n"
  "Damit kannst du Dinge erzwingen, die Claude sonst vergessen koennte — zum Beispiel nach "
  "jeder Aenderung die Formatierung laufen zu lassen oder gefaehrliche Befehle zu blockieren.\n\n"
  "`/hooks` zeigt dir, welche Hooks eingerichtet sind und wann sie greifen.\n\n"
  "Eingerichtet werden sie in der Einstellungsdatei unter `hooks`. Wichtig: Ein Hook laeuft "
  "als echter Befehl auf deinem Rechner — er kann also auch Schaden anrichten."),

 ("/ide", "Integrationen", "Eingebaut",
  "Verwaltet die Verbindung zu deiner Entwicklungsumgebung.",
  "Manage IDE integrations and show status",
  "Claude Code kann sich mit VS Code oder einer JetBrains-Umgebung verbinden. Dann sieht es, "
  "welche Datei du offen hast, und kann Unterschiede direkt dort anzeigen.\n\n"
  "`/ide` zeigt den Stand der Verbindung und laesst dich sie herstellen oder trennen.\n\n"
  "Startest du Claude Code aus dem eingebauten Terminal deiner Entwicklungsumgebung, verbindet "
  "es sich meist von allein.\n\n"
  "Die dazugehoerigen Einstellungen heissen `autoConnectIde` und `autoInstallIdeExtension`."),

 ("/import", "Einstellungen", "Eingebaut",
  "Uebernimmt die Einrichtung eines anderen Programmier-Assistenten.",
  "Import configuration from other coding agents",
  "Wenn du vorher mit einem anderen Assistenten gearbeitet hast, musst du deine Einrichtung "
  "nicht von Hand nachbauen. `/import codex` oder `/import gemini` uebernimmt sie.\n\n"
  "Uebernommen werden zum Beispiel Projektanweisungen, Regeln und angeschlossene Dienste, "
  "soweit sie sich uebertragen lassen.\n\n"
  "Mit `--dry-run` siehst du zuerst, was passieren wuerde, ohne dass etwas geaendert wird. "
  "Das ist der empfohlene erste Schritt.\n\n"
  "Mit `--yes` laeuft die Uebernahme ohne Rueckfragen durch."),

 ("/init", "Projekt", "Eingebaut",
  "Legt fuer das Projekt eine `CLAUDE.md` mit den wichtigsten Angaben an.",
  "Initialize the project with a CLAUDE.md guide",
  "Die Datei `CLAUDE.md` ist der Spickzettel fuer Claude: Wie ist das Projekt aufgebaut, "
  "welche Befehle bauen und testen es, welche Regeln gelten.\n\n"
  "`/init` schaut sich dein Projekt an und schreibt einen ersten Entwurf dieser Datei. Damit "
  "musst du nicht bei null anfangen.\n\n"
  "Den Entwurf solltest du danach anpassen. Am wertvollsten sind die Dinge, die man dem Code "
  "nicht ansieht — Absprachen, Eigenheiten, Stolperfallen.\n\n"
  "Die Datei wird bei jeder Sitzung mitgelesen. Halte sie deshalb kurz: Was drinsteht, kostet "
  "in jeder einzelnen Sitzung Platz."),

 ("/insights", "Auswertung", "Eingebaut",
  "Erstellt einen Bericht darueber, wie du Claude Code benutzt.",
  "Generate an HTML report about usage patterns in local sessions",
  "`/insights` wertet deine gespeicherten Sitzungen aus und macht daraus eine Berichtsseite. "
  "Du siehst, womit du deine Zeit verbringst, welche Werkzeuge oft laufen und wo es haengt.\n\n"
  "Die Auswertung passiert auf deinem Rechner. Deine Gespraeche werden dafuer nicht "
  "verschickt.\n\n"
  "Nuetzlich, um Muster zu erkennen: Wenn zum Beispiel immer wieder derselbe Fehler auftritt, "
  "faellt das hier auf.\n\n"
  "Wie lange die Sitzungen ueberhaupt aufbewahrt werden, legt `cleanupPeriodDays` fest."),

 ("/install-github-app", "Zusammenarbeit", "Eingebaut",
  "Richtet die Claude-Anwendung fuer ein GitHub-Projekt ein.",
  "Install the Claude GitHub App for a repository",
  "Mit dieser Anwendung kann Claude in deinem GitHub-Projekt mitarbeiten: Pull Requests "
  "durchsehen, auf Kommentare antworten, bei fehlgeschlagenen Pruefungen nachbessern.\n\n"
  "`/install-github-app` fuehrt dich durch die Einrichtung, einschliesslich der noetigen "
  "Berechtigungen.\n\n"
  "Du legst dabei fest, auf welche Projekte der Zugriff gilt. Weniger ist hier meist besser.\n\n"
  "Danach funktionieren Befehle wie `/autofix-pr` und `/babysit-prs` auch ohne dein Terminal."),

 ("/install-slack-app", "Zusammenarbeit", "Eingebaut",
  "Richtet Claude fuer einen Slack-Arbeitsbereich ein.",
  "Install the Claude Slack app",
  "Damit kannst du Claude in Slack ansprechen — in einem Kanal oder in einer direkten "
  "Nachricht — und es antwortet dort.\n\n"
  "`/install-slack-app` fuehrt durch die Einrichtung und die Freigaben im Arbeitsbereich.\n\n"
  "Praktisch fuer Teams: Eine Frage im Kanal, und die Antwort steht fuer alle sichtbar dabei.\n\n"
  "Bedenke, dass alles, was Claude dort schreibt, fuer den ganzen Kanal sichtbar ist."),

 ("/keybindings", "Darstellung", "Eingebaut",
  "Oeffnet die Datei mit den Tastenkuerzeln.",
  "Open the keyboard shortcuts file",
  "Claude Code bringt Tastenkuerzel mit, zum Beispiel zum Abschicken, Abbrechen oder Wechseln "
  "der Ansicht. Passen sie dir nicht, kannst du sie aendern.\n\n"
  "`/keybindings` oeffnet die Datei `~/.claude/keybindings.json`. Dort steht, welche Taste "
  "welche Aktion ausloest.\n\n"
  "Es gehen auch zweistufige Kuerzel — erst die eine Taste, dann die naechste, wie in manchen "
  "Editoren ueblich.\n\n"
  "Nach dem Speichern greifen die Aenderungen. Bei einem Tippfehler bleibt das alte "
  "Kuerzel bestehen."),

 ("/list-agents", "Agenten und Sitzungen", "Eingebaut",
  "Listet Unteragenten, Team-Mitglieder und deine anderen Claude-Sitzungen auf.",
  "List subagents, team teammates, and Claude Code sessions",
  "Bei mehreren gleichzeitig laufenden Sitzungen verliert man leicht den Ueberblick. Dieser "
  "Befehl zeigt alles auf einmal.\n\n"
  "Du siehst die Unteragenten der laufenden Sitzung, die Mitglieder eines Agenten-Teams sowie "
  "deine anderen Claude-Sitzungen — auch die in der Cloud.\n\n"
  "Die angezeigten Namen sind zugleich die Adresse: Unter diesem Namen kannst du einer Sitzung "
  "eine Nachricht schicken.\n\n"
  "Der Befehl `/peers` macht dasselbe."),

 ("/login", "Konto", "Eingebaut",
  "Meldet dich bei deinem Anthropic-Konto an.",
  "Log in with your Anthropic account",
  "Ohne Anmeldung kann Claude Code keine Anfragen stellen. `/login` oeffnet die Anmeldung im "
  "Browser und speichert danach den Zugang.\n\n"
  "Du kannst dich mit einem Abo bei claude.ai oder mit einem Konsolen-Konto anmelden. Welcher "
  "Weg erlaubt ist, kann eine Organisation ueber `forceLoginMethod` festlegen.\n\n"
  "Setzt du stattdessen die Umgebungsvariable `ANTHROPIC_API_KEY`, wird die genommen — auch "
  "wenn du angemeldet bist.\n\n"
  "Mit `CLAUDE_CONFIG_DIR` kannst du mehrere getrennte Anmeldungen nebeneinander haben."),

 ("/logout", "Konto", "Eingebaut",
  "Meldet dich von deinem Anthropic-Konto ab.",
  "Log out of your Anthropic account",
  "`/logout` loescht den gespeicherten Zugang von diesem Rechner. Danach musst du dich neu "
  "anmelden, bevor du weiterarbeiten kannst.\n\n"
  "Sinnvoll auf einem Rechner, den mehrere benutzen, oder wenn du das Konto wechseln willst.\n\n"
  "Deine Projektdateien und Einstellungen bleiben unangetastet — nur der Zugang ist weg.\n\n"
  "Am Mac liegt der Zugang im Schluesselbund und wird von dort entfernt."),

 ("/loop", "Automatisierung", "Mitgelieferter Skill",
  "Fuehrt eine Eingabe wiederholt in einem Abstand aus.",
  "Run a prompt or slash command on a recurring interval",
  "Manche Aufgaben muss man wiederholen: nachsehen, ob die Auslieferung durch ist, oder "
  "regelmaessig die offenen Pull Requests durchgehen.\n\n"
  "`/loop 5m /babysit-prs` fuehrt den genannten Befehl alle fuenf Minuten aus, solange die "
  "Sitzung offen ist.\n\n"
  "Laesst du den Abstand weg, bestimmt Claude ihn selbst — je nachdem, wie schnell sich das "
  "Beobachtete aendert.\n\n"
  "Nur fuer wirklich wiederkehrende Aufgaben nehmen. Fuer eine einmalige Sache waere das "
  "Verschwendung. Der Befehl `/proactive` macht dasselbe."),

 ("/mcp", "Integrationen", "Eingebaut",
  "Verwaltet die angeschlossenen Zusatzdienste und deren Anmeldung.",
  "Manage MCP server connections and OAuth authentication",
  "Ueber das Model-Context-Protocol, kurz MCP, kann Claude Code an fremde Dienste angeschlossen "
  "werden — eine Datenbank, ein Ticketsystem, ein Browser.\n\n"
  "`/mcp` zeigt, welche Dienste verbunden sind und ob sie antworten. Du kannst sie neu "
  "verbinden, ein- und ausschalten und die Anmeldung durchfuehren.\n\n"
  "Jeder Dienst bringt eigene Werkzeuge mit. Die kosten Platz im Gedaechtnis — schalte "
  "deshalb ab, was du nicht brauchst.\n\n"
  "Eingerichtet werden die Dienste in `.mcp.json` oder in den Einstellungen."),

 ("/memory", "Kontext und Gedaechtnis", "Eingebaut",
  "Bearbeitet die `CLAUDE.md`-Dateien und schaltet das automatische Gedaechtnis um.",
  "Edit CLAUDE.md files, and turn auto memory on or off",
  "Es gibt zwei Arten von Gedaechtnis. Die `CLAUDE.md`-Dateien schreibst du selbst — dort "
  "steht, was dauerhaft gelten soll.\n\n"
  "Das automatische Gedaechtnis fuellt Claude selbst: Es merkt sich Dinge ueber dich und deine "
  "Projekte und liest sie in spaeteren Sitzungen wieder ein.\n\n"
  "`/memory` oeffnet beides. Du kannst die Dateien bearbeiten und das automatische Gedaechtnis "
  "ein- oder ausschalten.\n\n"
  "Wichtig: Was hier steht, wird in JEDER Sitzung geladen. Zu viel davon macht Claude "
  "schlechter, nicht besser."),

 ("/mobile", "Integrationen", "Eingebaut",
  "Zeigt einen QR-Code zum Herunterladen der Claude-App fuers Handy.",
  "Show a QR code to download the Claude mobile app",
  "Mit der Handy-App kannst du eine laufende Sitzung von unterwegs weiterverfolgen und "
  "Rueckfragen beantworten.\n\n"
  "`/mobile` zeigt einen QR-Code direkt im Terminal. Du haeltst die Handy-Kamera darauf und "
  "landest im richtigen App-Verzeichnis.\n\n"
  "Zusammen mit der Fernsteuerung kannst du damit eine lange laufende Aufgabe vom Sofa aus "
  "begleiten.\n\n"
  "Die Befehle `/ios` und `/android` machen dasselbe."),

 ("/model", "Modell und Antworten", "Eingebaut",
  "Wechselt das benutzte Modell und merkt sich die Wahl.",
  "Switch the AI model and save it as the default",
  "Es gibt verschieden starke Modelle. Ein starkes denkt gruendlicher und kostet mehr, ein "
  "leichtes ist schnell und guenstig.\n\n"
  "`/model` zeigt die Auswahl. Du kannst den Namen auch direkt angeben, etwa `/model opus`.\n\n"
  "Die Wahl gilt fuer die laufende Sitzung und wird als Voreinstellung gemerkt. Sie greift "
  "allerdings nicht immer sofort — bei manchen Aenderungen ist ein Neustart noetig.\n\n"
  "Welche Modelle ueberhaupt zur Auswahl stehen, laesst sich ueber `availableModels` und "
  "`modelPicker` einschraenken."),

 ("/passes", "Konto", "Eingebaut",
  "Verschenkt eine kostenlose Woche Claude Code an Bekannte.",
  "Share a free week of Claude Code with friends",
  "Wenn dein Konto dazu berechtigt ist, kannst du Einladungen verschenken. Wer sie einloest, "
  "bekommt eine Woche Claude Code, ohne selbst zu bezahlen.\n\n"
  "`/passes` zeigt, wie viele Einladungen du noch hast, und erzeugt die Links.\n\n"
  "Ist dein Konto nicht berechtigt, erscheint der Befehl gar nicht erst.\n\n"
  "Die Einladungen laufen ab — verschenkte, aber nicht eingeloeste verfallen also irgendwann."),

 ("/permissions", "Berechtigungen", "Eingebaut",
  "Verwaltet, was ohne Rueckfrage erlaubt, was nachgefragt und was verboten ist.",
  "Manage allow, ask, and deny rules for tool permissions",
  "Es gibt drei Stufen. `allow` heisst: laeuft ohne Nachfrage. `ask` heisst: es wird jedes Mal "
  "gefragt. `deny` heisst: wird immer abgelehnt.\n\n"
  "`/permissions` zeigt die geltenden Regeln und laesst dich sie aendern. Regeln koennen "
  "Muster enthalten, etwa `Bash(git *)` fuer alle Git-Befehle.\n\n"
  "Wichtig zu wissen: `allow` ist KEINE abschliessende Liste. Was nicht darin steht, ist "
  "nicht automatisch verboten. Verbieten geht nur ueber `deny`.\n\n"
  "Ueber `deny` kannst du auch Dateien schuetzen, in denen Passwoerter stehen. Der Befehl "
  "`/allowed-tools` macht dasselbe."),

 ("/plan", "Arbeitsweise", "Eingebaut",
  "Schaltet in den Planungsmodus, in dem zuerst ein Vorgehen abgestimmt wird.",
  "Enter plan mode directly from the prompt",
  "Im Planungsmodus aendert Claude nichts. Es schaut sich das Problem an und legt dir einen "
  "Plan vor. Erst wenn du zustimmst, wird gearbeitet.\n\n"
  "Das ist bei groesseren Vorhaben Gold wert: Missverstaendnisse fallen auf, bevor Code "
  "entstanden ist.\n\n"
  "Du kannst dein Anliegen gleich mitgeben: `/plan die Anmeldung auf zwei Faktoren umstellen`.\n\n"
  "Im Plan siehst du, welche Dateien betroffen waeren und in welcher Reihenfolge vorgegangen "
  "wird."),

 ("/plugin", "Erweiterungen", "Eingebaut",
  "Verwaltet die Erweiterungen von Claude Code.",
  "Manage Claude Code plugins",
  "Eine Erweiterung bringt fertige Skills, Agenten, Hooks und Dienste in einem Paket mit. "
  "Statt alles einzeln einzurichten, installierst du ein Paket.\n\n"
  "`/plugin` zeigt die installierten Erweiterungen, laesst dich suchen, installieren, "
  "abschalten und entfernen.\n\n"
  "Erweiterungen kommen aus sogenannten Marktplaetzen. Welche erlaubt sind, laesst sich ueber "
  "`strictKnownMarketplaces` und `blockedMarketplaces` festlegen.\n\n"
  "Vorsicht: Eine Erweiterung kann Befehle auf deinem Rechner ausfuehren. Installiere nur, "
  "was du einordnen kannst."),

 ("/powerup", "Hilfe", "Eingebaut",
  "Fuehrt dich in kleinen Lektionen durch die Moeglichkeiten von Claude Code.",
  "Discover Claude Code features through interactive lessons",
  "Viele nuetzliche Funktionen kennt man einfach nicht. `/powerup` fuehrt dich in kurzen "
  "Lektionen an sie heran — nicht als trockene Liste, sondern zum Mitmachen.\n\n"
  "Du lernst zum Beispiel, wie der Planungsmodus funktioniert, wie Unteragenten arbeiten oder "
  "wie du Hooks einrichtest.\n\n"
  "Die Lektionen sind kurz und lassen sich jederzeit abbrechen.\n\n"
  "Ein guter Einstieg, wenn du Claude Code seit einiger Zeit benutzt, aber das Gefuehl hast, "
  "nur einen kleinen Teil davon zu nutzen."),

 ("/privacy-settings", "Konto", "Eingebaut",
  "Zeigt und aendert deine Datenschutz-Einstellungen.",
  "View and update privacy settings",
  "Hier legst du fest, was mit deinen Gespraechen geschieht — zum Beispiel, ob sie zur "
  "Verbesserung der Modelle verwendet werden duerfen.\n\n"
  "`/privacy-settings` oeffnet die Einstellungen direkt aus der Sitzung heraus.\n\n"
  "Der Befehl steht bei Pro- und Max-Konten zur Verfuegung. Bei Team- und "
  "Unternehmenskonten entscheidet das die Organisation.\n\n"
  "Es lohnt sich, hier einmal bewusst hinzusehen, statt es auf der Voreinstellung zu lassen."),
]
