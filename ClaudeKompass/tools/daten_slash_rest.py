# -*- coding: utf-8 -*-
"""Restliche Slash-Befehle: versteckte, Arbeitsablaeufe und weitere mitgelieferte Skills."""

SLASH_REST = [
 ("/babysit-prs", "Zusammenarbeit", "Mitgelieferter Skill",
  "Geht die offenen Pull Requests durch und kuemmert sich um das, was haengt.",
  "Watch open pull requests and handle what is stuck",
  "Offene Pull Requests bleiben gern liegen: eine Pruefung ist rot, ein Kommentar wartet auf "
  "Antwort, ein Zweig ist veraltet.\n\n"
  "Dieser Befehl geht sie der Reihe nach durch und kuemmert sich um genau solche Faelle. Was "
  "er selbst erledigen kann, erledigt er; den Rest meldet er dir.\n\n"
  "Zusammen mit `/loop` laesst er sich regelmaessig wiederholen, etwa `/loop 15m /babysit-prs`.\n\n"
  "Er setzt voraus, dass das GitHub-Kommandozeilenwerkzeug eingerichtet ist."),

 ("/design", "Darstellung", "Mitgelieferter Skill",
  "Erstellt eine Entwurfsflaeche mit mehreren Bildschirmen als veroeffentlichte Seite.",
  "Create a design canvas: a multi-artboard visual design published as an Artifact",
  "Wenn du eine Oberflaeche entwerfen willst, ist Code der falsche erste Schritt. `/design` "
  "legt eine Entwurfsflaeche an, auf der mehrere Bildschirme nebeneinander liegen.\n\n"
  "Du kannst hineinzoomen, verschieben und — wenn dein Konto das erlaubt — die Elemente direkt "
  "anklicken und aendern, ohne Code anzufassen.\n\n"
  "Geeignet fuer App-Oberflaechen, Bildschirmabfolgen, Landeseiten, Plakate und Handzettel.\n\n"
  "Der Befehl legt eine Flaeche NEU an. Eine vorhandene bearbeitest du direkt in der "
  "veroeffentlichten Seite."),

 ("/heapdump", "Fehlersuche", "Eingebaut (versteckt)",
  "Schreibt eine Momentaufnahme des Arbeitsspeichers zur Fehlersuche.",
  "Write a JavaScript heap snapshot for memory diagnosis",
  "Wenn Claude Code ueber die Zeit immer mehr Arbeitsspeicher belegt, braucht man eine "
  "Momentaufnahme, um zu sehen, was sich ansammelt.\n\n"
  "`/heapdump` schreibt genau die in eine Datei. Fachleute koennen sie danach mit einem "
  "Werkzeug oeffnen und die Ursache suchen.\n\n"
  "Der Befehl steht nicht in der normalen Liste — er ist fuer die Fehlersuche gedacht, nicht "
  "fuer den Alltag.\n\n"
  "Die Datei kann sehr gross werden und Bruchstuecke deiner Gespraeche enthalten. Gib sie "
  "nicht unbesehen weiter."),

 ("/run", "Arbeitsweise", "Mitgelieferter Skill",
  "Startet die Anwendung des Projekts, um eine Aenderung wirklich laufen zu sehen.",
  "Launch and drive the project's app to see a change working",
  "Tests sind gut, aber sie beweisen nicht, dass die Anwendung fuer einen Menschen richtig "
  "aussieht und sich richtig anfuehlt.\n\n"
  "`/run` startet die Anwendung — je nachdem, was fuer ein Projekt es ist: ein Server, ein "
  "Kommandozeilenprogramm, eine Oberflaeche im Browser.\n\n"
  "Gibt es im Projekt einen eigenen Skill zum Starten, wird der genommen. Sonst greift ein "
  "eingebautes Muster fuer den jeweiligen Projekttyp.\n\n"
  "Der richtige Griff, wenn du „zeig mir, dass es wirklich geht“ meinst."),

 ("/schedule", "Automatisierung", "Mitgelieferter Skill",
  "Legt Cloud-Agenten an, die nach einem Zeitplan laufen.",
  "Create, update, list, or run scheduled cloud agents",
  "Waehrend `/loop` nur laeuft, solange deine Sitzung offen ist, laeuft ein Zeitplan-Agent in "
  "der Cloud — auch wenn dein Rechner aus ist.\n\n"
  "`/schedule` legt solche Agenten an, aendert sie, listet sie auf oder startet sie sofort.\n\n"
  "Der Zeitplan wird wie bei einer klassischen Zeitsteuerung angegeben, zum Beispiel „jeden "
  "Werktag um acht Uhr“.\n\n"
  "Auch ein einmaliger spaeterer Lauf ist moeglich, etwa „morgen um drei nachsehen, ob die "
  "Auslieferung durch ist“."),

 ("/update-config", "Einstellungen", "Mitgelieferter Skill",
  "Aendert die Einstellungsdatei — vor allem, wenn etwas automatisch passieren soll.",
  "Configure the Claude Code harness via settings.json",
  "Wenn du sagst „ab jetzt soll jedes Mal, wenn X passiert, Y laufen“, reicht ein Merksatz "
  "nicht. Solche Automatik braucht einen Hook in der Einstellungsdatei.\n\n"
  "`/update-config` kuemmert sich darum: Es traegt Hooks ein, setzt Berechtigungen, legt "
  "Umgebungsvariablen an und sucht Fehler in bestehenden Hooks.\n\n"
  "Wichtig zu verstehen: Das Programm fuehrt Hooks aus, nicht Claude. Deshalb hilft es nichts, "
  "sich so etwas nur zu merken.\n\n"
  "Fuer einfache Sachen wie Erscheinungsbild oder Modell ist `/config` der schnellere Weg."),

 ("/upgrade", "Entwicklung", "Eingebaut (versteckt)",
  "Hebt die Anthropic-Programmbibliothek im Projekt auf eine neuere Fassung.",
  "Upgrade the Anthropic SDK dependency",
  "Wenn dein Projekt die Anthropic-Bibliothek benutzt, kommen regelmaessig neue Fassungen mit "
  "neuen Moeglichkeiten heraus.\n\n"
  "`/upgrade` hebt die eingetragene Fassung an und passt den Code an, wo sich etwas geaendert "
  "hat.\n\n"
  "Der Befehl steht nicht in der normalen Liste. Er richtet sich an Leute, die selbst gegen "
  "die Schnittstelle programmieren.\n\n"
  "Lass danach unbedingt die Tests laufen — bei einem Sprung ueber mehrere Fassungen aendert "
  "sich erfahrungsgemaess mehr, als man denkt."),

 ("/workflows", "Automatisierung", "Eingebaut",
  "Zeigt die laufenden Arbeitsablaeufe mit ihrem Fortschritt.",
  "Watch live progress of running workflows",
  "Ein Arbeitsablauf ist ein Skript, das viele Agenten in einer festen Ordnung steuert — zum "
  "Beispiel: erst alle Dateien pruefen, dann jeden Fund nachpruefen.\n\n"
  "`/workflows` zeigt, was gerade laeuft, in welcher Stufe es steht und wie weit es ist.\n\n"
  "Das ist wichtig, weil Arbeitsablaeufe im Hintergrund laufen und sonst nichts von sich hoeren "
  "lassen, bis sie fertig sind.\n\n"
  "Arbeitsablaeufe koennen sehr viele Agenten starten und entsprechend viel kosten — sie werden "
  "deshalb nur auf ausdrueckliche Anweisung gestartet."),
]
