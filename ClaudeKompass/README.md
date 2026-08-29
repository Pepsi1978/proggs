# Claude Kompass

Android-App, die Claude Code erklärt — auf Deutsch, auf dem Niveau einer zehnten Klasse
Realschule, mit Vorlesen und Rückfragen per Mikrofon.

**Zielgerät:** Galaxy Z Fold 8 (SM-F971B). Das Cover-Display ist der Normalfall, nach dem
gestaltet wird; aufgeklappt wird der Gesprächsbereich zweispaltig.

---

## Die vier Bereiche

| Bereich | Was drinsteht |
|---|---|
| **Slash-Befehle** | 91 Befehle, alphabetisch, je mit ausführlicher Erklärung und der Version, in der sie dazukamen |
| **Config** | 186 Einträge: Einstellungen aus `settings.json` und Umgebungsvariablen |
| **Best Practices** | 26 Artikel zur Arbeitsweise mit der aktuellen Fassung |
| **Chat** | Mehrere Gespräche nebeneinander; Antworten beziehen Befehle und Einstellungen mit ein |

Unter jedem Eintrag sitzen dieselben vier Knöpfe:

- **Vorlesen** — Absatz für Absatz, mit vorausschauender Synthese
- **Fragen** — Frage sprechen, Antwort landet im Klapp-Menü unter dem Eintrag
- **Mehr** — die Erklärung wird ausführlicher; jede Stufe wird gezählt
- **Zurück** — holt die vorherige, kürzere Fassung wieder (erscheint erst, wenn es etwas zurückzunehmen gibt)

Ganz unten in den beiden Nachschlage-Bereichen steht ein Klapp-Bereich **Entfernte Einträge**:
was es einmal gab, in welcher Version es wegfiel und was seine Aufgabe übernommen hat.

## Der Aktualisieren-Knopf

Oben in der Kopfleiste. Er holt die offiziellen Unterlagen und das Änderungsprotokoll, gleicht
sie mit dem Bestand ab und spielt die Unterschiede ein.

Zwei Dinge, die dabei bewusst so gebaut sind:

- **Namen werden ohne Modell gelesen.** Die Markdown-Tabellen werden direkt ausgewertet. Ein
  Modell könnte einen Befehl erfinden oder einen echten übersehen — beim Nachschlagen wäre
  beides schlimm. Erklärt wird erst danach.
- **Eine Untergrenze schützt den Bestand.** Kommen aus den Unterlagen weniger als 40 Einträge
  zurück, bricht der Lauf ab, statt jeden vorhandenen Eintrag als verschwunden zu behandeln.
  Das ist die Sicherung gegen einen stillen Totalschaden, wenn sich der Aufbau der Doku-Seiten
  ändert.

Neu dazugekommene Einträge sind golden umrandet und mit **NEU** markiert — bis zum nächsten
Lauf. Danach gehören sie zum Bestand.

## Wissensbasis

Die mitgelieferten Daten stehen in `app/src/main/assets/`. Sie werden aus den Quellen unter
`tools/` erzeugt; die Angabe „seit Version X" stammt aus dem offiziellen Änderungsprotokoll und
trägt die Belegzeile mit, auf die sie sich stützt.

Auslieferungsstand: **Claude Code 2.1.251**.

## Sprache und Ton

- Alles auf Deutsch mit echten Umlauten — auch Bedienhinweise, Fehlermeldungen und Protokoll.
- Erklärungen auf dem Niveau einer zehnten Klasse Realschule: kurze Hauptsätze, keine
  Fachbegriffe ohne Erklärung, ein Beispiel statt einer Definition.
- Kein stiller Fehlschlag: Jede Meldung sagt, was nicht ging, warum, und was man tun kann.

## Schlüssel

Drei Stück, alle in den Einstellungen, alle verschlüsselt abgelegt
(`EncryptedSharedPreferences`) und alle mit einem Prüfknopf, der einen echten kleinen Aufruf macht:

| Wofür | Dienst |
|---|---|
| Vorlesen (Standard) | Google Cloud Text-to-Speech, Chirp-3-HD-Stimmen |
| Eigene Stimme | Alibaba Model Studio (DashScope, internationaler Endpunkt) |
| Spracheingabe | Groq, `whisper-large-v3-turbo` |

Die KI-Antworten laufen über Codex. Die Anmeldung geschieht über einen Gerätecode (vier plus
fünf Zeichen) im Browser — ein Passwort wird in der App nie eingegeben.

**Lässt sich die verschlüsselte Ablage auf einem Gerät nicht öffnen, werden Schlüssel NICHT
ersatzweise im Klartext gespeichert.** Die App sagt es stattdessen. Ein stiller Rückfall auf
Klartext wäre die Art Fehler, die man erst bemerkt, wenn der Schlüssel schon abgeflossen ist.

## Vorlesen

Die Absatz-Pipeline nach dem Vorbild von CortexAndroid:

1. Ein Absatz ist eine Vorlese-Einheit. Absätze werden weder zusammengelegt noch mitten drin
   geteilt — nur ein Absatz über 1000 Zeichen wird an Satzgrenzen aufgeteilt, weil die Dienste
   sonst ablehnen.
2. Während Absatz *n* gesprochen wird, sind *n+1* und *n+2* schon in Arbeit. Der erste Ton kommt
   dadurch nach Bruchteilen einer Sekunde statt nach dem Synthetisieren des ganzen Textes.
3. Zwischen zwei Absätzen liegt rund eine Sekunde — hörbarer Atem, kein Loch.
4. Ein abgelehnter Schlüssel hält die ganze Reihe an und wird im Klartext gemeldet. Ein
   einzelner abgelehnter Absatz wird übersprungen, damit der Rest weiterläuft.

## Spracheingabe

`whisper-large-v3-turbo` über Groq, mit `response_format=verbose_json` — ohne dieses Format
fehlen die Kennzahlen, auf denen die Filter stehen.

Vier Schichten gegen erfundene Sätze, alle einzeln abschaltbar:

1. **Stille vorab erkennen** — eine Aufnahme ohne Sprache wird gar nicht erst hochgeladen.
2. **Kennzahlen prüfen** — Abschnitte mit den typischen Werten einer Erfindung fallen weg.
3. **Zeitstempel abgleichen** — Abschnitte in stillen Zeitfenstern fallen weg. Würden *alle*
   fallen, bleibt das Ergebnis von Schicht 2 stehen (dann liegt eher ein Zeitversatz vor).
4. **Floskeln sperren** — nur wenn die Ausgabe kurz ist UND ringsum Stille war. Ein bewusst
   gesagtes „Vielen Dank" bleibt.

Aufnahmen über 20 MB werden **vor** dem Senden an einer Sprechpause geteilt. Der Ablehnungsfehler
413 lässt sich nicht wiederholen — ohne dieses Teilen wäre ein langes Diktat vollständig verloren.

## Bauen

```
./gradlew :app:assembleDebug           # Debug-Paket
./gradlew :app:testDebugUnitTest       # Tests
./gradlew :app:assembleRelease         # Release inkl. R8
```

Die Wissensbasis neu erzeugen (setzt eine lokale Kopie des Änderungsprotokolls voraus):

```
python tools/baue_assets.py app/src/main/assets
```

## Aufbau

```
observability/   Protokoll (JSON-Zeilen), globaler Fehlerfänger, Logik-Sonden
data/            Room, Einstellungen, Wissensbasis-Lader, Sicherung
update/          Doku-Abruf, Tabellen-Auswertung, Abgleich
ai/              Codex: Anmeldung, Anfragen, Anweisungstexte
audio/           Aufnahme, Groq, die vier Filterschichten, WAV-Schnitt
tts/             Drei Vorlese-Dienste, eigene Stimme, Absatz-Pipeline
ui/              Theme (Gold, hell und dunkel), Bausteine, vier Bildschirme
vm/              Ein Modell je Aufgabe
```
