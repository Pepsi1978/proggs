# -*- coding: utf-8 -*-
"""Slash-Befehle A bis E. Reihenfolge: name, kategorie, art, kurz, englisch, erklaerung."""

SLASH_A_E = [
 ("/add-dir", "Arbeitsumgebung", "Eingebaut",
  "Gibt Claude Zugriff auf einen weiteren Ordner ausserhalb des Projekts.",
  "Add a working directory for file access in the current session",
  "Normalerweise darf Claude Code nur in dem Ordner arbeiten, in dem du es gestartet hast. "
  "Das ist eine Schutzmassnahme: So kann es nicht versehentlich in fremden Projekten herumschreiben.\n\n"
  "Manchmal brauchst du aber mehr. Zum Beispiel liegt deine App in einem Ordner und eine "
  "gemeinsam genutzte Bibliothek in einem zweiten. Mit `/add-dir ../meine-bibliothek` erlaubst "
  "du Claude, auch dort zu lesen und zu schreiben.\n\n"
  "Die Erlaubnis gilt nur fuer die laufende Sitzung. Startest du Claude Code neu, ist sie weg. "
  "Willst du sie dauerhaft, traegst du den Ordner unter `permissions.additionalDirectories` in "
  "die Einstellungsdatei ein.\n\n"
  "Praktisch ist der Befehl auch, wenn du eine Datei aus dem Download-Ordner einlesen lassen "
  "willst, ohne sie erst ins Projekt zu kopieren."),

 ("/advisor", "Modell und Antworten", "Eingebaut",
  "Schaltet einen zweiten Ratgeber dazu, den Claude bei kniffligen Stellen fragen kann.",
  "Enable or disable the advisor tool, which lets Claude consult a second model",
  "Der Ratgeber ist ein zweites KI-Modell, das im Hintergrund bereitsteht. Wenn Claude an "
  "einer schweren Stelle unsicher ist, kann es dieses zweite Modell um eine Einschaetzung "
  "bitten, bevor es weitermacht.\n\n"
  "Das ist wie beim Lernen: Wenn du eine Aufgabe nicht loesen kannst, fragst du jemanden, der "
  "sich damit auskennt. Genau das darf Claude hier auch.\n\n"
  "Mit `/advisor` schaltest du die Funktion ein, mit `/advisor off` wieder aus. Du kannst auch "
  "ein bestimmtes Modell nennen, zum Beispiel `/advisor opus`.\n\n"
  "Der Ratgeber kostet zusaetzliche Rechenzeit, weil eine zweite Anfrage laeuft. Bei einfachen "
  "Aufgaben lohnt er sich nicht, bei schwierigen Fehlersuchen dagegen oft schon."),

 ("/agents", "Agenten und Sitzungen", "Eingebaut",
  "Zeigt und verwaltet die Unteragenten, die Claude fuer Teilaufgaben starten kann.",
  "Create and manage subagents",
  "Ein Unteragent ist ein zweiter Claude, der eine abgegrenzte Teilaufgabe uebernimmt, zum "
  "Beispiel „durchsuche das ganze Projekt nach dieser Funktion“. Er hat einen eigenen "
  "Arbeitsspeicher und stoert den Hauptablauf nicht.\n\n"
  "Mit `/agents` siehst du alle vorhandenen Agenten, kannst neue anlegen und bestehende "
  "aendern. Fuer jeden legst du fest, wie er beschrieben ist, welche Werkzeuge er benutzen "
  "darf und mit welchem Modell er arbeitet.\n\n"
  "Der grosse Vorteil: Ein Unteragent kann eine riesige Menge Dateien durchsehen und dir am "
  "Ende nur das Ergebnis zurueckgeben. Der Hauptablauf bleibt dadurch uebersichtlich.\n\n"
  "Agenten liegen als Dateien unter `.claude/agents/` im Projekt oder unter `~/.claude/agents/` "
  "fuer alle deine Projekte."),

 ("/artifacts", "Ausgabe und Teilen", "Eingebaut",
  "Listet die veroeffentlichten Artefakte auf und oeffnet oder kopiert deren Link.",
  "List artifacts, attach one, open it, or copy its link",
  "Ein Artefakt ist eine fertige Seite, die Claude fuer dich ins Netz stellt — zum Beispiel ein "
  "Bericht, eine kleine Web-Anwendung oder eine Uebersicht. Sie ist zuerst privat und nur du "
  "siehst sie.\n\n"
  "`/artifacts` zeigt dir alle Artefakte, die du bisher erzeugt hast. Du kannst eines auswaehlen, "
  "im Browser oeffnen oder den Link in die Zwischenablage kopieren, um ihn weiterzugeben.\n\n"
  "Praktisch ist das, wenn du eine Seite aus einer frueheren Sitzung wiederfinden willst und "
  "den Link nicht mehr hast.\n\n"
  "Ueber die Tastenkombination Strg und die schliessende eckige Klammer kommst du zum zuletzt "
  "veroeffentlichten Artefakt der laufenden Sitzung zurueck."),

 ("/auto-mode-setup", "Berechtigungen", "Eingebaut",
  "Erzeugt aus deinem Projekt und deinen Sitzungen die Umgebungsvariablen fuer den Auto-Modus.",
  "Generate auto mode environment variables from your project and sessions",
  "Im Auto-Modus entscheidet Claude Code bei vielen Befehlen selbst, ob es nachfragen muss "
  "oder einfach weitermachen darf. Damit das sicher funktioniert, braucht es Regeln, die zu "
  "deinem Projekt passen.\n\n"
  "Dieser Befehl schaut sich an, was du bisher in deinen Sitzungen gemacht hast, und schlaegt "
  "daraus passende Einstellungen vor. Er nimmt dir also das muehsame Zusammensuchen ab.\n\n"
  "Das Ergebnis sind Umgebungsvariablen, die du danach uebernehmen kannst. Sie legen fest, "
  "welche Befehle als harmlos gelten und welche weiterhin eine Rueckfrage ausloesen.\n\n"
  "Du solltest das Ergebnis vor dem Uebernehmen durchlesen — es geht schliesslich darum, wo "
  "die Sicherheitsabfrage kuenftig wegfaellt."),

 ("/autocompact", "Kontext und Gedaechtnis", "Eingebaut",
  "Legt fest, wie voll das Gedaechtnis werden darf, bevor automatisch zusammengefasst wird.",
  "Set the auto-compact window: how full the context gets before Claude Code compacts",
  "Claude Code hat ein begrenztes Gedaechtnis fuer das laufende Gespraech. Wird es zu voll, "
  "fasst die App den bisherigen Verlauf automatisch zusammen und macht wieder Platz. Das nennt "
  "man Verdichten.\n\n"
  "Mit `/autocompact` bestimmst du, wann das passiert. `auto` ueberlaesst die Entscheidung der "
  "App. Du kannst stattdessen auch eine Zahl angeben, etwa `/autocompact 200k`.\n\n"
  "Ein kleineres Fenster bedeutet: haeufiger zusammenfassen, dafuer schnellere und guenstigere "
  "Antworten. Ein grosses Fenster behaelt mehr Einzelheiten, kostet aber mehr.\n\n"
  "Wenn du merkst, dass Claude Dinge vom Anfang des Gespraechs vergisst, war der letzte "
  "Verdichtungsschritt die Ursache."),

 ("/autofix-pr", "Zusammenarbeit", "Eingebaut",
  "Startet eine Sitzung in der Cloud, die einen Pull Request ueberwacht und Fehler selbst repariert.",
  "Start a cloud session that watches a pull request and pushes fixes when CI fails",
  "Ein Pull Request ist ein Vorschlag, Aenderungen in ein Projekt aufzunehmen. Vorher laeuft "
  "meist eine automatische Pruefung, die sogenannte CI. Faellt die durch, muss jemand "
  "nachbessern.\n\n"
  "`/autofix-pr` uebernimmt genau das. Es startet eine Sitzung in der Cloud, die den Pull "
  "Request beobachtet. Sobald die Pruefung fehlschlaegt, schaut sich Claude die Fehlermeldung "
  "an, baut eine Korrektur und schiebt sie hoch.\n\n"
  "Du musst dafuer nicht am Rechner sitzen. Die Sitzung laeuft weiter, auch wenn du dein "
  "Terminal schliesst.\n\n"
  "Du kannst einen zusaetzlichen Hinweis mitgeben, etwa welche Art von Korrektur du dir "
  "wuenschst."),

 ("/background", "Agenten und Sitzungen", "Eingebaut",
  "Loest die laufende Sitzung ab und laesst sie im Hintergrund weiterarbeiten.",
  "Detach the session and let it run as a background agent",
  "Manchmal dauert eine Aufgabe lange und du willst dein Terminal fuer etwas anderes benutzen. "
  "Mit `/background` loest du die Sitzung ab: Sie arbeitet weiter, aber nicht mehr vor deinen "
  "Augen.\n\n"
  "Du bekommst eine Kennung zurueck. Mit dieser Kennung kannst du dich spaeter wieder "
  "dazuschalten, das Protokoll ansehen oder die Sitzung beenden.\n\n"
  "Das ist wie ein Hintergrundprogramm auf dem Rechner: Es laeuft, ohne dass du zusehen musst.\n\n"
  "Ueber `/tasks` siehst du, was gerade im Hintergrund laeuft. Mit dem Kommandozeilenbefehl "
  "`claude agents` bekommst du die Liste auch ausserhalb einer Sitzung."),

 ("/batch", "Grosse Aenderungen", "Mitgelieferter Skill",
  "Verteilt eine grosse, gleichfoermige Aenderung auf mehrere Unteragenten.",
  "Parallelize a large change across the codebase using subagents",
  "Wenn du dieselbe Aenderung an sehr vielen Dateien brauchst, dauert das nacheinander ewig. "
  "`/batch` teilt die Arbeit auf mehrere Unteragenten auf, die gleichzeitig loslegen.\n\n"
  "Du beschreibst einmal, was passieren soll, zum Beispiel „ersetze ueberall den alten "
  "Bibliotheksnamen durch den neuen“. Die Aufteilung uebernimmt der Befehl.\n\n"
  "Das spart viel Zeit. Es hat aber auch eine Tuecke: Mehrere Agenten koennen dieselbe Stelle "
  "unterschiedlich anfassen. Bei sehr einheitlichen Aenderungen ist ein einfaches Suchen-und-"
  "Ersetzen-Skript oft sicherer.\n\n"
  "Sieh dir das Ergebnis deshalb hinterher als Ganzes an, bevor du es uebernimmst."),

 ("/branch", "Gespraechsverlauf", "Eingebaut",
  "Erzeugt einen Abzweig im Gespraech, um eine andere Richtung auszuprobieren.",
  "Create a conversation branch to try a new direction",
  "Stell dir das Gespraech als Weg vor. Mit `/branch` setzt du eine Weiche: Ab hier laeuft ein "
  "zweiter Weg parallel zum ersten.\n\n"
  "Das ist nuetzlich, wenn du einen anderen Loesungsansatz durchspielen willst, ohne das "
  "bisherige Gespraech kaputtzumachen. Faellt der neue Weg durch, ist der alte unveraendert da.\n\n"
  "Du kannst dem Abzweig einen Namen geben, damit du ihn spaeter wiederfindest.\n\n"
  "Der Unterschied zu `/clear`: `/clear` faengt komplett von vorne an, `/branch` nimmt den "
  "bisherigen Verlauf mit und legt nur einen zweiten Strang daneben."),

 ("/btw", "Gespraechsverlauf", "Eingebaut",
  "Stellt eine Zwischenfrage, ohne dass sie im Gespraechsverlauf haengen bleibt.",
  "Ask a side question without adding it to the conversation history",
  "Manchmal willst du nur schnell etwas wissen, das mit der eigentlichen Aufgabe nichts zu tun "
  "hat. Wuerdest du es normal fragen, stuende es fuer immer im Verlauf und wuerde Gedaechtnis "
  "verbrauchen.\n\n"
  "`/btw` loest das. Die Frage wird beantwortet, danach aber nicht im Verlauf gespeichert. Der "
  "Name kommt vom englischen „by the way“, also „uebrigens“.\n\n"
  "Ein Beispiel: `/btw was macht dieser Befehl in der Fehlermeldung nochmal?` Du bekommst die "
  "Antwort, und die eigentliche Arbeit geht unveraendert weiter.\n\n"
  "Das haelt das Gespraech schlank und spart damit auch Kosten."),

 ("/bug", "Rueckmeldung", "Eingebaut",
  "Meldet einen Fehler an Anthropic oder teilt das Gespraech zur Analyse.",
  "Report a bug or share the conversation, with optional history selection",
  "Wenn Claude Code sich falsch verhaelt, kannst du das direkt aus der Sitzung heraus melden. "
  "`/bug` sammelt die noetigen Angaben und schickt sie an die Entwickler.\n\n"
  "Du kannst dabei auswaehlen, wie viel vom Gespraech mitgeschickt wird. Achte darauf, keine "
  "Passwoerter oder Zugangsdaten mitzusenden.\n\n"
  "Je genauer du beschreibst, was du erwartet hast und was stattdessen passiert ist, desto "
  "eher wird der Fehler gefunden.\n\n"
  "Fuer Wuensche und allgemeine Rueckmeldungen gibt es stattdessen `/feedback`."),

 ("/cd", "Arbeitsumgebung", "Eingebaut",
  "Verlegt die Sitzung in einen anderen Arbeitsordner.",
  "Move the session to a new working directory",
  "Der Arbeitsordner ist der Ort, auf den sich alle Datei-Angaben beziehen. Mit `/cd` "
  "verschiebst du die laufende Sitzung dorthin, wo du gerade weiterarbeiten willst.\n\n"
  "Das ist etwas anderes als `/add-dir`. `/add-dir` erlaubt zusaetzlichen Zugriff, `/cd` "
  "verlegt den Mittelpunkt der Arbeit.\n\n"
  "Nach dem Wechsel liest Claude Code die Projektregeln des neuen Ordners ein, zum Beispiel "
  "eine dort liegende `CLAUDE.md`.\n\n"
  "Der Gespraechsverlauf bleibt erhalten — du faengst also nicht von vorne an."),

 ("/chrome", "Integrationen", "Eingebaut",
  "Richtet die Verbindung zwischen Claude und dem Chrome-Browser ein.",
  "Configure the Claude in Chrome integration",
  "Mit dieser Verbindung kann Claude in deinem Browser mitarbeiten: eine Seite ansehen, auf "
  "Schaltflaechen klicken oder pruefen, ob deine Web-Anwendung richtig aussieht.\n\n"
  "`/chrome` oeffnet die Einstellungen dazu. Dort schaltest du die Verbindung ein und legst "
  "fest, was erlaubt ist.\n\n"
  "Das ist besonders praktisch beim Bauen von Webseiten: Claude kann selbst nachsehen, ob die "
  "Aenderung so aussieht wie gedacht, statt dich danach zu fragen.\n\n"
  "Weil der Browser Zugriff auf deine angemeldeten Konten hat, solltest du genau ueberlegen, "
  "welche Seiten du freigibst."),

 ("/claude-api", "Entwicklung", "Mitgelieferter Skill",
  "Laedt Nachschlagewissen zur Claude-Programmierschnittstelle und hilft beim Umstellen von Code.",
  "Load Claude API and managed agents reference material, and update code",
  "Wenn du selbst ein Programm schreibst, das Claude benutzt, brauchst du genaue Angaben: "
  "Welche Modellnamen gibt es, was kosten sie, welche Einstellungen sind moeglich.\n\n"
  "`/claude-api` holt genau dieses Nachschlagewissen in die Sitzung. Damit antwortet Claude "
  "aus der aktuellen Unterlage statt aus dem Gedaechtnis — das verhindert veraltete Angaben.\n\n"
  "Der Befehl kann ausserdem beim Umstellen helfen, etwa von einem alten auf ein neues Modell, "
  "oder deinen Code auf unnoetige Kosten durchsehen.\n\n"
  "Nutze ihn immer, bevor du Fragen zu Modellnamen oder Preisen beantwortest."),

 ("/clear", "Gespraechsverlauf", "Eingebaut",
  "Beginnt ein neues Gespraech mit leerem Gedaechtnis.",
  "Start a new conversation with an empty context",
  "`/clear` wischt den Tisch ab. Der bisherige Verlauf wird beiseitegelegt und Claude startet "
  "ohne Vorwissen aus diesem Gespraech.\n\n"
  "Das brauchst du, wenn du zu einem ganz anderen Thema wechselst. Sonst schleppt Claude den "
  "alten Zusammenhang mit und wird dadurch langsamer und ungenauer.\n\n"
  "Deine Dateien und Projektregeln bleiben unangetastet — nur das Gespraech ist neu. Du kannst "
  "dem neuen Gespraech gleich einen Namen geben.\n\n"
  "Die Befehle `/reset` und `/new` machen dasselbe. Ueber `/resume` kommst du zu einem frueheren "
  "Gespraech zurueck."),

 ("/code-review", "Qualitaet", "Mitgelieferter Skill",
  "Prueft die Aenderungen auf Fehler und auf Stellen, die einfacher gehen.",
  "Review the current diff for correctness bugs and simplification opportunities",
  "Der Befehl sieht sich an, was du zuletzt geaendert hast, und sucht nach zwei Dingen: echten "
  "Fehlern und Stellen, die unnoetig kompliziert sind.\n\n"
  "Du kannst die Gruendlichkeit steuern. `low` und `medium` melden nur wenige, dafuer sichere "
  "Funde. `high` und `max` schauen breiter hin und melden auch Unsicheres. `ultra` startet "
  "eine besonders tiefe Pruefung in der Cloud.\n\n"
  "Mit `--fix` werden die Funde gleich eingebaut, mit `--comment` als Anmerkungen an den Pull "
  "Request geschrieben.\n\n"
  "Als Ziel kannst du eine Pull-Request-Nummer, einen Zweig oder einen Pfad angeben."),

 ("/color", "Darstellung", "Eingebaut",
  "Faerbt die Eingabezeile der laufenden Sitzung ein.",
  "Set the prompt bar color for the current session",
  "Wenn du mehrere Sitzungen gleichzeitig offen hast, verwechselt man sie leicht. `/color` gibt "
  "jeder Sitzung eine eigene Farbe fuer die Eingabezeile.\n\n"
  "Ein Beispiel: `/color yellow` faerbt die Zeile gelb. Mit `/color default` nimmst du die "
  "Faerbung wieder zurueck.\n\n"
  "Die Farbe gilt nur fuer die laufende Sitzung und wird nicht gespeichert.\n\n"
  "Das ist reine Orientierungshilfe — am Verhalten von Claude aendert sich nichts."),

 ("/compact", "Kontext und Gedaechtnis", "Eingebaut",
  "Fasst das bisherige Gespraech zusammen und schafft damit wieder Platz.",
  "Free up context by summarizing the conversation",
  "Wird das Gespraech lang, ist irgendwann das Gedaechtnis voll. `/compact` schreibt eine "
  "Zusammenfassung des bisherigen Verlaufs und ersetzt damit die vielen Einzelheiten.\n\n"
  "Die wichtigen Punkte bleiben erhalten, die Nebensaechlichkeiten fallen weg. Danach hast du "
  "wieder Luft fuer neue Arbeit.\n\n"
  "Du kannst mitgeben, worauf es dir ankommt, zum Beispiel `/compact behalte alle Entscheidungen "
  "zur Datenbank`. Dann achtet die Zusammenfassung genau darauf.\n\n"
  "Normalerweise passiert das von allein. `/compact` ist der Griff, wenn du es sofort willst."),

 ("/config", "Einstellungen", "Eingebaut",
  "Oeffnet die Einstellungen oder setzt einzelne Werte direkt.",
  "Open the settings interface, or set individual settings",
  "`/config` zeigt dir eine Uebersicht der Einstellungen, in der du dich durchklicken kannst — "
  "Erscheinungsbild, Modell, Verhalten.\n\n"
  "Du kannst auch direkt setzen, ohne die Uebersicht: `/config theme=dark` schaltet zum "
  "Beispiel auf das dunkle Erscheinungsbild.\n\n"
  "Was du hier aenderst, landet in deiner persoenlichen Einstellungsdatei und gilt damit auch "
  "in kuenftigen Sitzungen.\n\n"
  "Der Befehl `/settings` macht dasselbe. Manche Werte, etwa das Modell, greifen erst nach "
  "einem Neustart."),

 ("/context", "Kontext und Gedaechtnis", "Eingebaut",
  "Zeigt als farbiges Raster, wodurch das Gedaechtnis gerade belegt ist.",
  "Visualize current context usage as a colored grid",
  "Das Gedaechtnis von Claude Code ist begrenzt, und man sieht ihm nicht an, wodurch es voll "
  "ist. `/context` macht das sichtbar.\n\n"
  "Du bekommst ein farbiges Raster. Jede Farbe steht fuer einen Anteil: die Systemanweisungen, "
  "deine Projektregeln, gelesene Dateien, der Gespraechsverlauf, die Werkzeugbeschreibungen.\n\n"
  "So erkennst du sofort, wenn etwas unerwartet viel Platz frisst — zum Beispiel eine sehr "
  "grosse `CLAUDE.md` oder ein Werkzeugsatz, den du gar nicht brauchst.\n\n"
  "Mit `/context all` bekommst du die ausfuehrliche Aufschluesselung."),

 ("/copy", "Ausgabe und Teilen", "Eingebaut",
  "Kopiert die letzte Antwort in die Zwischenablage.",
  "Copy the last assistant response to the clipboard",
  "Wenn du eine Antwort in eine E-Mail, ein Dokument oder einen Chat uebernehmen willst, "
  "musst du sie nicht muehsam markieren. `/copy` legt sie direkt in die Zwischenablage.\n\n"
  "Mit einer Zahl greifst du weiter zurueck: `/copy 3` nimmt die drittletzte Antwort.\n\n"
  "Das ist im Terminal besonders praktisch, weil das Markieren mit der Maus dort oft die "
  "Zeilenumbrueche zerstoert.\n\n"
  "Fuer das ganze Gespraech nimmst du stattdessen `/export`."),

 ("/cost", "Kosten", "Eingebaut",
  "Zeigt, wie viel die Sitzung bisher gekostet hat.",
  "Alias for /usage: show token usage and cost",
  "Jede Anfrage an das Modell kostet Geld oder verbraucht ein Kontingent. `/cost` legt offen, "
  "wo du gerade stehst.\n\n"
  "Du siehst die verbrauchten Einheiten, die daraus entstandenen Kosten und — seit Version "
  "2.1.251 — auch, wie gut der Zwischenspeicher gegriffen hat.\n\n"
  "Der Zwischenspeicher ist wichtig fuers Sparen: Wiederholt gesendeter Text wird billiger "
  "abgerechnet, wenn er noch im Speicher liegt. Eine niedrige Trefferquote bedeutet also "
  "unnoetige Kosten.\n\n"
  "`/usage` zeigt dasselbe."),

 ("/dataviz", "Darstellung", "Mitgelieferter Skill",
  "Holt die Gestaltungsregeln fuer Diagramme und Auswertungen in die Sitzung.",
  "Design guidance for charts, graphs, and dashboards",
  "Diagramme sehen schnell unprofessionell aus: schlecht gewaehlte Farben, unleserliche "
  "Beschriftungen, ein Diagrammtyp, der nicht zu den Daten passt.\n\n"
  "`/dataviz` laedt eine Anleitung, die genau das verhindert. Sie enthaelt eine gepruefte "
  "Farbpalette, Regeln zur Auswahl des richtigen Diagrammtyps und Vorgaben fuer Achsen und "
  "Legenden.\n\n"
  "Die Anleitung gilt unabhaengig davon, womit du zeichnest — ob mit einer Programmbibliothek, "
  "als Bild oder direkt im Browser.\n\n"
  "Lade sie, bevor die erste Zeile Diagramm-Code entsteht, nicht hinterher."),

 ("/debug", "Fehlersuche", "Mitgelieferter Skill",
  "Schaltet die ausfuehrliche Protokollierung ein und hilft bei Problemen mit Claude Code selbst.",
  "Enable debug logging and troubleshoot session problems",
  "Wenn Claude Code selbst zickt — Werkzeuge schlagen fehl, eine Verbindung bricht ab, ein "
  "Hook laeuft nicht — brauchst du mehr Einblick als die normale Ausgabe bietet.\n\n"
  "`/debug` schaltet die ausfuehrliche Protokollierung ein und fuehrt dich durch die Suche. "
  "Du kannst dabei beschreiben, was schiefgeht.\n\n"
  "Wichtig: Es geht um Probleme mit dem Werkzeug, nicht um Fehler in deinem eigenen Programm. "
  "Fuer die suchst du besser mit einer normalen Beschreibung des Fehlers.\n\n"
  "Die Protokolle koennen Pfade und Befehle enthalten — sieh sie durch, bevor du sie weitergibst."),

 ("/deep-research", "Recherche", "Arbeitsablauf",
  "Recherchiert eine Frage im Netz und liefert einen Bericht mit Quellenangaben.",
  "Run web searches, fetch and cross-check sources, and synthesize a cited report",
  "Bei einer schwierigen Frage reicht eine einzelne Suche selten. `/deep-research` startet "
  "mehrere Suchen, holt die gefundenen Seiten, vergleicht sie miteinander und schreibt daraus "
  "einen zusammenhaengenden Bericht.\n\n"
  "Jede Aussage bekommt eine Quellenangabe. Damit kannst du nachpruefen, woher etwas stammt — "
  "das ist der wichtigste Unterschied zu einer schnellen Antwort aus dem Gedaechtnis.\n\n"
  "Der Vorgang dauert laenger und verbraucht deutlich mehr Kontingent als eine normale Frage.\n\n"
  "Sinnvoll bei Fragen, bei denen Aktualitaet und Belegbarkeit zaehlen."),

 ("/design-login", "Integrationen", "Eingebaut",
  "Gibt den Zugriff auf das Design-System frei, den `/design-sync` braucht.",
  "Authorize design system access for /design-sync",
  "Bevor Claude dein Design-System hochladen darf, musst du das einmal erlauben. Genau dafuer "
  "ist dieser Befehl da.\n\n"
  "Er oeffnet die Anmeldung und speichert die Freigabe. Danach kannst du `/design-sync` "
  "benutzen, ohne dich jedes Mal neu anzumelden.\n\n"
  "Ein Design-System ist die Sammlung deiner Farben, Schriften, Abstaende und Bausteine — "
  "also das, was deine Oberflaechen einheitlich aussehen laesst.\n\n"
  "Ohne diese Freigabe bricht `/design-sync` mit einer Fehlermeldung ab."),

 ("/design-sync", "Integrationen", "Mitgelieferter Skill",
  "Laedt dein React-Design-System zu Claude Design hoch.",
  "Upload your React design system to Claude Design",
  "Damit Entwuerfe so aussehen wie deine echte Anwendung, muss Claude deine Bausteine kennen: "
  "welche Knoepfe es gibt, welche Farben, welche Abstaende.\n\n"
  "`/design-sync` liest dein React-Projekt aus und uebertraegt diese Bausteine zu Claude "
  "Design. Danach entstehen Entwuerfe in deinem eigenen Stil statt in einem beliebigen.\n\n"
  "Du kannst einen Hinweis mitgeben, wo die Bausteine liegen, wenn sie nicht am ueblichen Ort "
  "sind.\n\n"
  "Vorher muss `/design-login` gelaufen sein."),

 ("/desktop", "Integrationen", "Eingebaut",
  "Setzt die laufende Sitzung in der Claude-Code-Desktop-Anwendung fort.",
  "Continue the current session in the Claude Code desktop app",
  "Terminal und Desktop-Anwendung sind zwei Fenster auf dieselbe Arbeit. `/desktop` uebergibt "
  "die laufende Sitzung an das Desktop-Fenster.\n\n"
  "Der Verlauf geht dabei nicht verloren: Du machst genau dort weiter, wo du aufgehoert hast, "
  "nur mit einer grafischen Oberflaeche statt der Textzeile.\n\n"
  "Sinnvoll, wenn du Unterschiede zwischen Dateien ansehen oder mehrere Sitzungen "
  "nebeneinander verwalten willst.\n\n"
  "Der Befehl `/app` macht dasselbe. Er setzt eine installierte Desktop-Anwendung voraus."),

 ("/diff", "Qualitaet", "Eingebaut",
  "Oeffnet eine Ansicht, die alle noch nicht eingecheckten Aenderungen zeigt.",
  "Interactive diff viewer for uncommitted changes and per-turn diffs",
  "Bevor du Aenderungen festschreibst, willst du sehen, was sich wirklich geaendert hat. "
  "`/diff` zeigt genau das: Zeile fuer Zeile, was dazugekommen und was weggefallen ist.\n\n"
  "Du kannst dich durch die Dateien bewegen und dir auch ansehen, was in einem einzelnen "
  "Arbeitsschritt passiert ist.\n\n"
  "Das ist die wichtigste Kontrolle vor einem Commit. Wer sie ueberspringt, schreibt "
  "irgendwann etwas fest, das er nicht wollte.\n\n"
  "Mit der Einstellung `diffTool` kannst du die Ansicht stattdessen in deiner "
  "Entwicklungsumgebung oeffnen lassen."),

 ("/doctor", "Fehlersuche", "Mitgelieferter Skill",
  "Prueft deine Einrichtung durch und behebt gefundene Probleme.",
  "Run a setup checkup, diagnose issues, and fix them",
  "`/doctor` ist die Durchsicht beim Arzt, nur fuer deine Claude-Code-Einrichtung. Es prueft "
  "die Einstellungsdateien, die Hooks, die angeschlossenen Dienste und die installierten "
  "Erweiterungen.\n\n"
  "Gefundene Probleme werden benannt und, wo moeglich, gleich behoben. Zum Beispiel eine "
  "kaputte Einstellungsdatei oder ein Dienst, der nicht mehr antwortet.\n\n"
  "Es prueft auch dein Beschreibungs-Budget: Hast du sehr viele Skills, passen ihre "
  "Beschreibungen irgendwann nicht mehr alle ins Gedaechtnis.\n\n"
  "Der erste Griff, wenn irgendetwas an der Einrichtung nicht stimmt. `/checkup` macht dasselbe."),

 ("/effort", "Modell und Antworten", "Eingebaut",
  "Legt fest, wie gruendlich Claude nachdenkt, bevor es antwortet.",
  "Set the effort level: low through xhigh, max, ultracode, or auto",
  "Nachdenken kostet Zeit und Geld, bringt aber bessere Ergebnisse. Mit `/effort` bestimmst du, "
  "wie viel davon eingesetzt wird.\n\n"
  "`low` ist schnell und guenstig und passt fuer einfache Handgriffe. `high` und `xhigh` "
  "lohnen sich bei kniffligen Fehlern oder Entwuerfen. `max` ist die tiefste Stufe.\n\n"
  "`auto` ueberlaesst Claude die Entscheidung je nach Aufgabe — das ist meist der beste "
  "Ausgangspunkt.\n\n"
  "`/effort status` zeigt die aktuelle Stufe. Dauerhaft stellst du sie ueber die Einstellung "
  "`effortLevel` ein, nicht ueber eine Umgebungsvariable."),

 ("/exit", "Arbeitsumgebung", "Eingebaut",
  "Beendet Claude Code.",
  "Exit the CLI",
  "`/exit` schliesst die Sitzung und bringt dich zurueck in deine normale Kommandozeile.\n\n"
  "Der Verlauf geht dabei nicht verloren. Ueber `/resume` oder den Kommandozeilenschalter "
  "`--continue` kannst du spaeter weitermachen.\n\n"
  "Laufen noch Hintergrundaufgaben, werden sie beim Beenden abgebrochen — sieh vorher mit "
  "`/tasks` nach.\n\n"
  "`/quit` macht dasselbe. Auch zweimal Strg und D beendet die Sitzung."),

 ("/export", "Ausgabe und Teilen", "Eingebaut",
  "Speichert das ganze Gespraech als reinen Text.",
  "Export the current conversation as plain text",
  "Wenn du ein Gespraech aufheben oder weitergeben willst, schreibt `/export` es in eine "
  "Textdatei.\n\n"
  "Du kannst einen Dateinamen angeben. Ohne Angabe waehlt Claude Code selbst einen.\n\n"
  "Praktisch fuer die Dokumentation: Was wurde entschieden, welche Wege wurden verworfen und "
  "warum.\n\n"
  "Denk daran, die Datei vor dem Weitergeben durchzusehen. In einem langen Gespraech steht "
  "schnell einmal ein Pfad oder ein Schluessel, der niemanden etwas angeht."),
]
