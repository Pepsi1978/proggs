# Epigenetik – Weitermachen

Stand 15.10.2026, 14:09 Uhr, Version 1.0.0 (vc1). Was der Code nicht verrät: Wünsche, Entscheidungen, offene Punkte.
Technik und Bedienung stehen in `README.md`.

## Stand

- Erste Version gebaut, per WLAN auf dem Fold 8 installiert, per apk-update in UpdateStation.
- **Ungetestet** (Schnellmodus). Frank prüft die App selbst; seine Rückmeldung ist der nächste Schritt.
- Jarvis 1.8.53 (vc64) ist angebunden: `Jarvis/app/src/main/java/de/frank/jarvis/faehigkeit/EpigenetikFaehigkeit.kt`.

## Was Frank am Handy noch tun muss

1. Epigenetik → Einstellungen → „Mit ChatGPT verbinden“. Ohne Anmeldung scheitert jede Recherche, auch die von Jarvis.
2. In ChatGPT den Jarvis-Connector neu laden: Der Relay speichert `tools/list` zwischen, sonst fehlen die
   vier `epigenetik_*`-Werkzeuge.
3. Optional: Groq-Schlüssel eintragen (bessere Fachbegriffe beim Diktat).

## Franks Wünsche (Auftrag vom 15.10.2026)

- Absolute Benutzerfreundlichkeit, schnelle Wege, sehr schöne Optik wie „Geniale Aufgaben“ (Glas, Schatten, 3D-Knöpfe).
- Mittel eingeben → genaue Internet-Recherche, welche Gene/Schalter an- oder abgehen, danach Auswertung.
- Umgekehrt: Wirkung eingeben (z. B. „Aktivierung der Sirtuine“) → welche Mittel, Nahrung, Verhalten das bewirken.
- Gewichtung: 1. Nahrungsergänzungsmittel, 2. Nahrung, 3. Verhalten.
- KI: OpenAI Codex über Gerätecode-Anmeldung wie in den anderen Apps.
- Vorlesen und Spracheingabe mit Einstellungen wie in Aufgaben; Hell/Dunkel, Standard ist das Orange-Design.
- Ziele einsprechen (Langlebigkeit, Gesundheit, Krebsprävention …).
- Jarvis-Anbindung: vorhandene Auswertung sofort nutzen, sonst Recherche anstoßen.
- War als Grundgerüst gedacht; Frank entwickelt die App danach mit mir weiter.

## Entscheidungen

- Festes Vokabular aus 47 Schaltern in 12 Gruppen plus 11 Zielvorlagen (`data/Schalter.kt`), von Fable vorgeschlagen.
  Die KI ordnet ihre Befunde diesen Schlüsseln zu; nur so funktionieren Atlas und Stack-Abdeckung offline.
- Evidenzregeln im Prompt (`ki/EpigenetikKi.kt`): Stärke ohne Humanstudie höchstens 40, nur Zellkultur höchstens 25,
  keine erfundenen Quellen.
- Standardmodell ASTRA, Gründlichkeit „Gründlich“ (HIGH), Websuche an, 3 Recherchen parallel (WorkManager).
- Mittel-Abgleich ist tippfehlertolerant (`Repository.aehnlich`), hält aber „Vitamin D“ und „Vitamin K“ auseinander.
- Eigene Idee „Stack“ (was Frank nimmt → Abdeckung, Lücken, Risiken, Tagesplan) kam ohne ausdrücklichen Wunsch dazu.

## Offen / Ideen

- Kleiner Fehler: Die Schnellstart-Chips filtern nur nach `schluessel`, nicht nach `aliase`. Korrigiert die KI einen
  Namen, erscheint der alte Chip wieder.
- Recherchen älter als 12 Monate markieren und per Tipp auffrischen.
- Epigenetik in Jarvis' Live-Gedächtnis aufnehmen (Lagebild.Bereich, Waechter.QUELLEN, GedaechtnisFaehigkeit);
  die App sendet `notifyChange` schon.

## Bauen auf diesem PC

`JAVA_HOME` zeigt (Stand 15.10.2026) auf ein gelöschtes JDK. Pro Aufruf setzen:
`$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"`, dann `./gradlew :app:assembleDebug`.
