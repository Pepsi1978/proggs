# Jarvis – Regeln für dieses Projekt

## Neue Einstellungen immer auch für Jarvis selbst
Jeder neue Schalter, jede Auswahl oder Zahl in den Einstellungen (`data/Einstellungen.kt`, `data/settings/SecureSettings.kt`)
kommt im selben Commit zusätzlich in den `EinstellungsKatalog` (`faehigkeit/EinstellungenFaehigkeit.kt`):
Kennung, Bereich (wie der Reiter/Bereich in der App), verständlicher Titel, ein Satz Beschreibung und dieselben
Folgeaktionen wie in der Oberfläche (Dienst abgleichen, Zeitplan neu stellen, Weckwort neu laden …).
Nur so kann Frank sie per Sprache abfragen („Was hast du beim Vorlesen für Einstellungen?“) und umstellen.
Nicht hinein gehören Zugangsschlüssel, Passwörter und Server-Adressen.

## Tätigkeitsanzeige für neue Werkzeuge und Apps
Was Jarvis gerade tut, steht im Chat, in der Sprachblase und bei Agenten als kurzer Satz aus `agent/Taetigkeit.kt`.
Neue Werkzeuge und Apps erscheinen dort von selbst (nach App und Art: „schaut in …“, „arbeitet in …“, „löscht etwas in …“).
Passt das nicht gut, bekommt das neue Werkzeug im selben Commit `taetigkeit = "…"` (ohne Subjekt, für Menschen lesbar,
kein Werkzeugname: „stellt einen Wecker“). Nur echte Tätigkeiten, keine erfundenen Zwischenschritte.

## Installation aufs Handy nur über das Skript
Jarvis nie mit nacktem `adb install` installieren, sondern über `Werkzeuge/jarvis-install/jarvis-install.sh <apk>` (Windows: `.ps1`).
Es fragt per Overlay nach dem Zeitpunkt (`ui/InstallFrageDienst.kt`), damit eine laufende Aufnahme nicht abbricht; ohne Antwort installiert es nach 30 Sekunden.
Technischer Schutz: Das Manifest verlangt den Pflicht-Teil `freigabe` (`requiredSplitTypes`). Gradle legt ihn signiert als `assets/freigabe-teil.apk` in die APK; nur jarvis-install und UpdateStation holen ihn heraus und installieren ihn mit. Nacktes `adb install` und `installDebug` scheitern daher mit `INSTALL_FAILED_MISSING_SPLIT`. UpdateStation ist die Rückfallebene für den Notfall: Es installiert den Teil mit, stellt aber bewusst keine Frage.
Der Drive-Upload ist für Jarvis derzeit ausgesetzt (`driveUploadAusgesetzt` im Skill `apk-update`): Die Rückfallebene zeigt dann nur noch Cloud- und Server-Bauten; `APK_UPDATE_STATUS=ausgesetzt` ist kein Fehler.

## Live-Modus
Der Live-Knopf (orange Blase unten rechts) startet dieselbe Gesprächsschleife wie „Hey Jarvis“ in der geöffneten App
(`AppViewModel.weckwortStart(perKnopf = true)`): zuhören, antworten, vorlesen, wieder zuhören. Es gibt keine zweite
Gesprächsvariante mehr; das frühere Echtzeit-Live-Gespräch (OpenAI/Gemini, Paket `live/`) ist entfernt (1.8.633).

## Sprachkette messen, ohne zu sprechen
`adb shell am broadcast -a de.frank.jarvis.SPRACH_MESSLAUF -p de.frank.jarvis --es frage "…"` startet die Kette wie nach dem Weckwort mit Text statt Mitschrift (nur Shell und System dürfen das). Der Lauf beginnt ein neues Gespräch und spricht laut, also vorher fragen; ist Jarvis vorn oder läuft schon ein Sprachgespräch, lehnt er ab. Ergebnis im Logbuch: „Kette Mikrofon“, „Kette Weckwort“, „Blase“.
