# Jarvis – Regeln für dieses Projekt

## Neue Einstellungen immer auch für Jarvis selbst
Jeder neue Schalter, jede Auswahl oder Zahl in den Einstellungen (`data/Einstellungen.kt`, `data/settings/SecureSettings.kt`)
kommt im selben Commit zusätzlich in den `EinstellungsKatalog` (`faehigkeit/EinstellungenFaehigkeit.kt`):
Kennung, Bereich (wie der Reiter/Bereich in der App), verständlicher Titel, ein Satz Beschreibung und dieselben
Folgeaktionen wie in der Oberfläche (Dienst abgleichen, Zeitplan neu stellen, Weckwort neu laden …).
Nur so kann Frank sie per Sprache abfragen („Was hast du beim Vorlesen für Einstellungen?“) und umstellen.
Nicht hinein gehören Zugangsschlüssel, Passwörter und Server-Adressen.
