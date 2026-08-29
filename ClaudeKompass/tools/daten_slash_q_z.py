# -*- coding: utf-8 -*-
"""Slash-Befehle Q bis Z und die entfernten Befehle."""

SLASH_Q_Z = [
 ("/radio", "Sonstiges", "Eingebaut",
  "Oeffnet das Lo-Fi-Radio von Claude im Browser.",
  "Open Claude FM lo-fi radio in the browser",
  "Ein kleiner Spass am Rande: `/radio` oeffnet einen Radiostrom mit ruhiger "
  "Hintergrundmusik zum Arbeiten.\n\n"
  "Die Musik laeuft im Browser, nicht im Terminal. Claude Code selbst spielt nichts ab.\n\n"
  "Auf das Verhalten von Claude hat der Befehl keinerlei Einfluss.\n\n"
  "Zum Beenden schliesst du einfach den Browser-Tab."),

 ("/rate-limit-options", "Konto", "Eingebaut",
  "Zeigt, welche Moeglichkeiten du hast, wenn dein Kontingent aufgebraucht ist.",
  "Show options when you hit usage limits",
  "Wenn dein Kontingent erschoepft ist, geht erst einmal nichts mehr. Dieser Befehl zeigt, "
  "welche Wege es gibt: warten bis zum Zuruecksetzen, auf ein guenstigeres Modell wechseln "
  "oder das Kontingent erweitern.\n\n"
  "Er steht nicht in der normalen Befehlsliste, sondern taucht auf, wenn er gebraucht wird.\n\n"
  "Mit der Einstellung `autoContinueAtUsageLimit` kann Claude Code auch von allein warten und "
  "danach weitermachen.\n\n"
  "Wann dein Kontingent sich zuruecksetzt, siehst du in `/usage`."),

 ("/reload-plugins", "Erweiterungen", "Eingebaut",
  "Liest die Erweiterungen neu ein.",
  "Reload plugins",
  "Wenn du an einer eigenen Erweiterung arbeitest, willst du deine Aenderung sehen, ohne "
  "Claude Code jedes Mal neu zu starten.\n\n"
  "`/reload-plugins` liest alle Erweiterungen frisch ein. Neue Skills, geaenderte Hooks und "
  "angepasste Agenten sind danach sofort da.\n\n"
  "Das Gespraech bleibt dabei erhalten.\n\n"
  "Nuetzlich vor allem beim Entwickeln. Im normalen Betrieb braucht man den Befehl selten."),

 ("/remote-control", "Integrationen", "Eingebaut",
  "Erlaubt es, die Sitzung von einem anderen Geraet aus weiterzufuehren.",
  "Continue a local session from another device",
  "Mit der Fernsteuerung kannst du eine Sitzung, die auf deinem Rechner laeuft, vom Handy oder "
  "von claude.ai aus begleiten: mitlesen, Rueckfragen beantworten, Anweisungen geben.\n\n"
  "`/remote-control` schaltet das fuer die laufende Sitzung ein.\n\n"
  "Praktisch bei langen Aufgaben: Du gehst weg, und wenn Claude etwas wissen will, bekommst du "
  "eine Mitteilung aufs Handy.\n\n"
  "Automatisch ab Start geht das ueber `remoteControlAtStartup`. Ganz abschalten laesst es "
  "sich ueber `disableRemoteControl`."),

 ("/resume", "Gespraechsverlauf", "Eingebaut",
  "Kehrt zu einem frueheren Gespraech zurueck.",
  "Return to an earlier conversation",
  "Gespraeche sind nicht weg, wenn du Claude Code schliesst. `/resume` zeigt dir eine Liste "
  "der letzten und laesst dich eines auswaehlen.\n\n"
  "Du kannst auch direkt eine Nummer angeben: `/resume 2` nimmt das vorletzte.\n\n"
  "Nach dem Fortsetzen ist der ganze Verlauf wieder da. Claude weiss also noch, was ihr "
  "besprochen habt.\n\n"
  "Wie lange die Gespraeche aufbewahrt werden, legt `cleanupPeriodDays` fest. Auf der "
  "Kommandozeile macht `claude --continue` dasselbe fuer das zuletzt benutzte."),

 ("/rewind", "Gespraechsverlauf", "Eingebaut",
  "Setzt Code und Gespraech auf einen frueheren Stand zurueck.",
  "Roll back code and conversation to a checkpoint",
  "Claude Code legt unterwegs Sicherungspunkte an. `/rewind` bringt dich zu einem davon "
  "zurueck — und zwar mitsamt der Dateien, nicht nur dem Gespraech.\n\n"
  "Das ist die Rettung, wenn eine Aenderung alles kaputtgemacht hat und du nicht mehr weisst, "
  "was genau angefasst wurde.\n\n"
  "Du kannst auch nur einen Teil zusammenfassen lassen, statt ganz zurueckzuspringen.\n\n"
  "Die Sicherungspunkte lassen sich ueber `fileCheckpointingEnabled` abschalten — dann "
  "funktioniert das Zuruecksetzen der Dateien aber nicht mehr."),

 ("/rules", "Einstellungen", "Eingebaut",
  "Zeigt die geltenden Regeln der Sitzung oder oeffnet sie zum Bearbeiten.",
  "View or edit the rules for the session",
  "Regeln sind kurze, dauerhafte Anweisungen, die in jeder Sitzung mitgelten — zum Beispiel "
  "„antworte auf Deutsch“ oder „vor jedem Push wird rebased“.\n\n"
  "`/rules view` zeigt, welche gerade greifen. `/rules edit` oeffnet sie zum Aendern.\n\n"
  "Sie liegen als Dateien unter `.claude/rules/` im Projekt oder unter `~/.claude/rules/` fuer "
  "alle Projekte.\n\n"
  "Halte sie kurz. Regeln werden vollstaendig in jede Sitzung geladen — je mehr Text, desto "
  "schlechter werden alle Regeln befolgt."),

 ("/security-review", "Qualitaet", "Mitgelieferter Skill",
  "Prueft die Aenderungen auf Sicherheitsluecken.",
  "Review the diff for security vulnerabilities",
  "Waehrend `/code-review` nach Fehlern und Umstaendlichkeiten sucht, schaut dieser Befehl "
  "gezielt auf Sicherheit.\n\n"
  "Gesucht wird nach den bekannten Mustern: ungeprueft weitergereichte Eingaben, Passwoerter "
  "im Code, zu weit gefasste Berechtigungen, unsichere Verschluesselung.\n\n"
  "Der Befehl ersetzt keine echte Sicherheitspruefung, faengt aber die haeufigen Fehler ab, "
  "bevor sie ins Projekt gelangen.\n\n"
  "Sinnvoll immer dann, wenn du an Anmeldung, Berechtigungen oder Datenverarbeitung "
  "gearbeitet hast."),

 ("/simplify", "Qualitaet", "Mitgelieferter Skill",
  "Vereinfacht den geaenderten Code, ohne nach Fehlern zu suchen.",
  "Reduce code complexity in the changed code",
  "Dieser Befehl sucht nach Wiederholungen, unnoetigen Umwegen und Stellen, die kuerzer und "
  "verstaendlicher gehen — und baut die Verbesserung gleich ein.\n\n"
  "Er sucht ausdruecklich NICHT nach Fehlern. Dafuer ist `/code-review` da.\n\n"
  "Ueber `low`, `medium` und `high` steuerst du, wie tief eingegriffen wird.\n\n"
  "Am besten nach einer fertigen, laufenden Aenderung anwenden — nicht mittendrin."),

 ("/status", "Arbeitsumgebung", "Eingebaut",
  "Zeigt den Zustand der laufenden Sitzung.",
  "Show session status",
  "`/status` fasst zusammen, wie die Sitzung gerade steht: welches Modell laeuft, welcher "
  "Berechtigungsmodus gilt, welcher Ordner der Arbeitsordner ist, welche Dienste verbunden "
  "sind.\n\n"
  "Auch dein Anmeldestand und die aktive Denkstufe stehen dort.\n\n"
  "Der erste Griff, wenn sich Claude anders verhaelt als erwartet — oft ist eine Einstellung "
  "anders, als man denkt.\n\n"
  "Fuer Verbrauch und Kosten nimmst du `/usage`, fuer die Gedaechtnisbelegung `/context`."),

 ("/subtask", "Agenten und Sitzungen", "Eingebaut",
  "Gibt eine Nebenaufgabe an einen Unteragenten und holt nur das Ergebnis zurueck.",
  "Give a side task to a subagent; the result comes back into the conversation",
  "Manche Zwischenschritte erzeugen viel Ausgabe, die dich gar nicht interessiert — zum "
  "Beispiel das Durchsuchen von hundert Dateien.\n\n"
  "`/subtask` gibt so etwas an einen Unteragenten ab. Der macht die Arbeit in seinem eigenen "
  "Speicher und liefert dir nur das Ergebnis.\n\n"
  "Dein Gespraech bleibt dadurch schlank, und du sparst Gedaechtnis fuer das Wesentliche.\n\n"
  "Ein Beispiel: `/subtask finde alle Stellen, an denen die alte Schnittstelle noch benutzt wird`."),

 ("/tasks", "Agenten und Sitzungen", "Eingebaut",
  "Listet die Hintergrundaufgaben der laufenden Sitzung auf.",
  "List background tasks of the current session",
  "Manches laeuft nebenher: ein Server, ein langer Testlauf, ein Hintergrundagent. `/tasks` "
  "zeigt, was davon gerade aktiv ist.\n\n"
  "Du siehst den Zustand jeder Aufgabe und kannst sie ansehen oder beenden.\n\n"
  "Wichtig vor dem Schliessen der Sitzung: Was hier noch laeuft, wird beim Beenden "
  "abgebrochen.\n\n"
  "Auch die Beobachter fuer veroeffentlichte Artefakte stehen in dieser Liste."),

 ("/teleport", "Integrationen", "Eingebaut",
  "Holt eine im Browser laufende Sitzung in dieses Terminal.",
  "Pull a web session into this terminal",
  "Wenn du auf claude.ai/code angefangen hast und lieber im Terminal weiterarbeiten willst, "
  "musst du nicht neu anfangen.\n\n"
  "`/teleport` holt die laufende Web-Sitzung samt Verlauf hierher.\n\n"
  "Das ist die Gegenrichtung zu `/web`, das dich vom Terminal in den Browser bringt.\n\n"
  "Nuetzlich, wenn du unterwegs etwas angestossen hast und es am Rechner zu Ende bringen willst."),

 ("/theme", "Darstellung", "Eingebaut",
  "Stellt das Erscheinungsbild ein.",
  "Set the appearance theme",
  "`/theme` schaltet zwischen hellem und dunklem Erscheinungsbild um. Es gibt auch Varianten "
  "mit staerkerem Kontrast fuer schlechte Lichtverhaeltnisse.\n\n"
  "Du kannst den Namen direkt angeben, etwa `/theme dark`, oder mit `/theme default` zur "
  "Voreinstellung zurueck.\n\n"
  "Die Wahl wird gespeichert und gilt auch in kuenftigen Sitzungen.\n\n"
  "Manche Terminals ueberschreiben Farben selbst — dann sieht das Ergebnis anders aus als "
  "gedacht."),

 ("/usage", "Kosten", "Eingebaut",
  "Zeigt Verbrauch und Kosten der Sitzung.",
  "Show model token usage and cost",
  "`/usage` legt offen, wie viele Einheiten verbraucht wurden, was das gekostet hat und wie "
  "weit dein Kontingent noch reicht.\n\n"
  "Seit Version 2.1.251 gibt es zusaetzlich eine Anzeige des Ausgabenlimits fuer Nutzer hinter "
  "einem entsprechenden Zugang.\n\n"
  "Die Aufschluesselung nach Modell zeigt, wo das Geld hingeht — oft ueberrascht es, wie viel "
  "auf Hintergrundaufgaben entfaellt.\n\n"
  "`/cost` zeigt dasselbe."),

 ("/verify", "Qualitaet", "Mitgelieferter Skill",
  "Prueft nach, ob der Code wirklich das tut, was er soll.",
  "Verify code correctness",
  "Waehrend `/code-review` nach bekannten Fehlermustern sucht, geht `/verify` die Frage an: "
  "Stimmt das Ergebnis ueberhaupt?\n\n"
  "Dazu wird der Ablauf durchgedacht, es werden Randfaelle geprueft und Annahmen "
  "hinterfragt.\n\n"
  "Seit Version 2.1.215 laeuft der Befehl nur noch auf ausdrueckliche Anforderung und nicht "
  "mehr automatisch — er kostet viel Zeit und war nicht immer noetig.\n\n"
  "Sinnvoll bei Stellen, an denen ein Fehler teuer waere: Abrechnung, Berechtigungen, "
  "Datenumzuege."),

 ("/version", "Arbeitsumgebung", "Eingebaut",
  "Zeigt die installierte Claude-Code-Version.",
  "Show the Claude Code version",
  "`/version` nennt dir die genaue Versionsnummer, zum Beispiel 2.1.251.\n\n"
  "Die brauchst du bei Fehlermeldungen, beim Nachschlagen im Aenderungsprotokoll und beim "
  "Pruefen, ob eine Funktion bei dir ueberhaupt schon vorhanden ist.\n\n"
  "Genau diese Nummer nutzt auch der Aktualisieren-Knopf dieser App: Er vergleicht sie mit dem "
  "Stand der hier gespeicherten Erklaerungen.\n\n"
  "Auf der Kommandozeile bekommst du dasselbe mit `claude --version`."),

 ("/web", "Integrationen", "Eingebaut",
  "Wechselt zu Claude Code im Browser.",
  "Switch to Claude Code on the web",
  "`/web` bringt die laufende Sitzung nach claude.ai/code, also in den Browser.\n\n"
  "Dort kannst du an einem anderen Geraet weitermachen oder jemandem etwas zeigen, ohne dass "
  "er Zugriff auf dein Terminal braucht.\n\n"
  "Der Verlauf geht mit — du faengst nicht von vorne an.\n\n"
  "Die Gegenrichtung ist `/teleport`."),
]

# Befehle, die es einmal gab und die inzwischen entfernt wurden.
# Format: name, kategorie, kurz, entferntIn, ersatz, erklaerung
SLASH_ENTFERNT = [
 ("/pr-comments", "Zusammenarbeit",
  "Holte die Kommentare eines Pull Requests in die Sitzung.",
  "2.1.91",
  "Kein eigener Befehl mehr noetig: Frag Claude einfach direkt danach, zum Beispiel „hol die "
  "Kommentare von Pull Request 412 und arbeite sie ab“. Claude benutzt dafuer selbstaendig das "
  "GitHub-Kommandozeilenwerkzeug.",
  "Frueher gab es einen eigenen Befehl, um die Anmerkungen aus einem Pull Request zu holen. "
  "Er wurde in Version 2.1.91 entfernt.\n\n"
  "Der Grund: Claude kann das inzwischen von allein. Es erkennt die Absicht aus deiner "
  "normalen Formulierung und ruft das noetige Werkzeug selbst auf.\n\n"
  "Fuer dich aendert sich damit fast nichts — du schreibst es nur in eigenen Worten statt als "
  "Befehl.\n\n"
  "Das ist eine allgemeine Entwicklung: Befehle, die nur eine Abkuerzung fuer eine normale "
  "Bitte waren, verschwinden nach und nach."),

 ("/ultrareview", "Qualitaet",
  "Startete eine besonders tiefe Pruefung mit mehreren Agenten in der Cloud.",
  "",
  "Ersetzt durch `/code-review ultra`. Der neue Weg kann dasselbe und zusaetzlich noch mehr: "
  "Er nimmt einen Zweig oder eine Pull-Request-Nummer als Ziel und kann das Ergebnis auf "
  "Wunsch als Kommentar an den Pull Request schreiben.",
  "`/ultrareview` startete eine sehr gruendliche Pruefung, bei der mehrere Agenten in der "
  "Cloud gleichzeitig auf den Code schauen.\n\n"
  "Der Befehl gilt als veraltet. Er funktioniert noch als Zweitname, aber der richtige Weg ist "
  "heute `/code-review ultra`.\n\n"
  "Der Vorteil des neuen Wegs: Alle Pruefungen liegen unter einem Befehl, und die "
  "Gruendlichkeit ist nur noch eine Stufe davon.\n\n"
  "Wichtig: Diese Pruefung wird abgerechnet und muss von dir ausgeloest werden — Claude kann "
  "sie nicht selbst starten."),
]
