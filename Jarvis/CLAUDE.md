# Jarvis – Regeln für dieses Projekt

## Neue Einstellungen immer auch für Jarvis selbst
Jeder neue Schalter, jede Auswahl oder Zahl in den Einstellungen (`data/Einstellungen.kt`, `data/settings/SecureSettings.kt`)
kommt im selben Commit zusätzlich in den `EinstellungsKatalog` (`faehigkeit/EinstellungenFaehigkeit.kt`):
Kennung, Bereich (wie der Reiter/Bereich in der App), verständlicher Titel, ein Satz Beschreibung und dieselben
Folgeaktionen wie in der Oberfläche (Dienst abgleichen, Zeitplan neu stellen, Weckwort neu laden …).
Nur so kann Frank sie per Sprache abfragen („Was hast du beim Vorlesen für Einstellungen?“) und umstellen.
Nicht hinein gehören Zugangsschlüssel, Passwörter und Server-Adressen.

## Installation aufs Handy nur über das Skript
Jarvis nie mit nacktem `adb install` installieren, sondern über `Werkzeuge/jarvis-install/jarvis-install.sh <apk>` (Windows: `.ps1`).
Es fragt per Overlay nach dem Zeitpunkt (`ui/InstallFrageDienst.kt`), damit eine laufende Aufnahme nicht abbricht; ohne Antwort installiert es nach 30 Sekunden.

## Live-Gespräch: erst lesen, danach messen
Vor jeder Änderung an `live/` (vor allem `LiveGespraech.kt`: Polster, Füll-Stille, Echosperre, Fadenprioritäten, Tonweg)
`best-practices/apis/openai-live-realtime.md` §14 lesen: Ursachen der früheren Abhacker, bewährte Werte, offene Punkte.
Nach der Änderung die Rückfall-Prüfung aus §14.6 durchführen (Logcat-Tag `JarvisLiveRouting`, Grenzwerte gegen die
Referenzmessung von 1.8.108). Der aktive Live-Sprachmodus hat Vorrang vor Hintergrundarbeit; neue Benachrichtigungen,
die während eines Gesprächs feuern können, bekommen `setSilent(GespraechsFokus.stilleHinweise)`.
