# Jarvis, Gemini Live: Eigene Worte kommen als Franks Beitrag zurück (20.10.2026)

> Stand 20.10.2026, 17:31 Uhr. Behoben in Jarvis 1.8.194 (Echosperre bei Bluetooth). Am Gerät noch nicht gemessen.
> Markierungen: **[Beleg]** = in Daten nachgewiesen · **[Doku]** = von Google/Android dokumentiert · **[Nutzerbericht]** =
> Forum/GitHub · **[Schluss]** = gefolgert, nicht gemessen · **[offen]**.

## Kurzfassung

Im Auto (Bluetooth-Medienton, Echosperre an) hörte Gemini Live das **Ende der eigenen Antwort** über das Handy-Mikrofon
und lieferte es als Franks Worte zurück. Ursache in der App: Die Echosperre öffnete das Mikrofon, sobald der Mischer des
Handys den Ton abgeholt hatte, plus 400 ms. Über Bluetooth kommt der Ton aber deutlich später aus dem Autolautsprecher.
Die letzten 0,6–0,9 s jeder Antwort fielen so ins offene Mikrofon. Behoben: Bei Bluetooth wartet die Sperre jetzt die
Ausgabelatenz ab (gemessen, mindestens 1 s). Am Handy-Lautsprecher bleibt alles wie bisher.

## Befunde

### 1. Das Muster: immer das Satzende der eigenen Antwort [Beleg]

Aus Jarvis' Gesprächsverlauf („Aufgaben für morgen abfragen“, 20.10.2026, 17:18 Uhr), jeweils direkt hintereinander:

| Gemini sprach (Mitschrift im Chat) | Als „Frank“ erkannt |
|---|---|
| „Hallo Frank, ich bin für dich da.“ | „bin für dich da.“ |
| „Gibt es dazu noch etwas?“ | „noch etwas?“ |
| „Moment, ich schaue direkt nach.“ | „direkt nach“ |

Von Frank zusätzlich gemeldet (Wortlaut der Antworten nicht gesichert): „Garnelen hinzu“, danach „Garnelen meinst“ aus
der Rückfrage, „weiterleite“, „übermittelt“. Frank bestätigt, nichts davon gesagt zu haben. Möglicher weiterer Fall:
„Ich höre zu.“ am Anfang von Franks erster Frage [offen].

Alle Fälle sind die **letzten zwei bis vier Wörter** der unmittelbar vorher gesprochenen Antwort, etwa 0,6–0,9 s Sprache.
Zufällige Geräusch- oder Stille-Halluzinationen sähen anders aus (beliebige Wörter, nicht genau das eigene Satzende).
Fahrgeräusche erklären das Muster deshalb nicht.

### 2. Einstellungen und Tonweg [Beleg, nachträglich gelesen]

Live-Modell `gemini-3.8-live` (App-Name „Gemini 3.8 Live“; laut Google-Modellliste ein stabiles Modell [Doku]),
„Gemini denkt selbst“, Tonweg „Medienton, Handy-Mikrofon“, Echosperre an, Dazwischenreden an, VAD Start/Ende „gelassen“
(LOW), Pause 2 s. Gelesen um 17:2x Uhr, also nach dem Gespräch. Dass „Gemini denkt selbst“ auch um 17:18 galt, passt zu
„Moment, ich schaue direkt nach“ als hörbarer Gemini-Satz (mit „Jarvis direkt“ wäre er stumm geblieben).

### 3. Der App-Fehler: Sperre endet am Mischer, nicht am Lautsprecher [Beleg im Code, Ursache Schluss]

`LiveGespraech.kt` (bis 1.8.193): Das Mikrofon schickt Stille, solange `playbackHeadPosition < echtBisFrame` (Ton noch nicht
abgespielt), danach noch `ECHO_NACHLAUF_MS = 400`. Die Kopfposition zählt, was der Mischer abgeholt hat. Bei Bluetooth (A2DP)
folgen noch Codec, Funkstrecke und der Puffer des Autoradios; Android weist darauf hin, dass ein Zeitstempel Verzögerungen,
die der Implementierung unbekannt sind, nicht kennt [Doku: AudioTimestamp]. Die Messung von §14.4 (Sperre öffnet
591–1225 ms nach dem letzten Schreiben) gilt nur für den Handy-Lautsprecher; Bluetooth war nie gemessen.

Dazu kommt ein kleiner Versatz in der App: Über einen 100-ms-Mikrofonblock wird erst nach dem Lesen entschieden, sein
Inhalt ist bis zu 100 ms älter. Das schiebt die Öffnung zusätzlich in Richtung Antwortende.

Rechnung [Schluss]: Mit 400 ms Nachlauf kamen 0,6–0,9 s Antwortende durch. Das hörbare Ende lag also etwa 0,9–1,2 s hinter
der Kopfposition. Das passt zu Bluetooth mit Autoradio, nicht zum Handy-Lautsprecher.

### 4. Echo-Unterdrückung des Handys hilft hier nicht [Schluss mit Doku]

Die Aufnahme nutzt `VOICE_COMMUNICATION` mit `AcousticEchoCanceler` („aktiv=true“ im Log). Die AEC entfernt das Signal,
das sie als Referenz kennt [Doku]. Ton, der das Handy über Bluetooth Richtung Autolautsprecher verlässt, kommt mit unbekannter
Verzögerung zurück; dafür ist sie nicht gebaut. Google selbst schreibt im Cookbook-Beispiel zu `gemini-3.8-live`, ohne
Kopfhörer unterbreche sich das Modell selbst, weil die Systemaufnahme oft keine Echo-Unterdrückung hat [Doku: Google-Code].
Der Server unterdrückt kein Echo.

### 5. Keine App-Vermischung der Mitschriften [Beleg im Code]

Franks Mitschrift kommt nur aus `serverContent.inputTranscription`, Geminis aus `outputTranscription`; nirgends wird das eine
in das andere kopiert. Der Prüfsatz-Empfänger ist nur im Debug-Build und nur mit DUMP-Recht (adb) erreichbar. Ein internes
Loopback ohne Lautsprecher ist im Medienweg nicht möglich: Das Mikrofon ist fest das Handy-Mikrofon, die Ausgabe A2DP.

### 6. Was Google dazu bekannt ist

- [Nutzerbericht 21.09.2026] Genau dieses Symptom mit `gemini-3.8-live` am Smartphone: Die Worte des Modells erscheinen in
  der Eingabe-Mitschrift, als hätte der Nutzer gesprochen; Browser-AEC und LOW/LOW halfen nicht. Google hat nicht
  geantwortet. https://discuss.ai.google.dev/t/gemini-live-api-acoustic-echo-cancellation-needed-for-sip-telephony-deployments/144332
- [Nutzerbericht 20.03.2026] Bewährt: Mikrofon sperren, solange der Agent spricht („echo gate“).
  https://discuss.ai.google.dev/t/hard-won-patterns-for-building-voice-apps-with-gemini-live-march-2026/128155
- [Nutzerbericht 21.10.2025] Auf älteren Modellen kommen bei Stille auch frei erfundene Mitschriften („, 2, 3, 4, 5,“).
  Das ist eine **andere** Fehlerart als hier. https://discuss.ai.google.dev/t/gemini-live-api-models-inputtranscription-hallucinations/107899
- [Doku] Mitschrift wird unabhängig von anderen Servernachrichten geschickt, ohne garantierte Reihenfolge.
  https://ai.google.dev/api/live
- [Doku] Proactive Audio (bei 3.8 immer an) soll Hintergrundgespräche ignorieren. Das betrifft nur, ob Gemini antwortet;
  die Mitschrift kommt trotzdem. https://ai.google.dev/gemini-api/docs/models/gemini-3.8-live
- [Doku] `prefixPaddingMs` ist widersprüchlich beschrieben (Audio vor dem Sprachbeginn bzw. nötige Sprechdauer, bevor der
  Beginn gilt). Jarvis nutzt 20 ms. https://ai.google.dev/gemini-api/docs/live-guide, https://ai.google.dev/api/live

### Einordnung

| Art | Was |
|---|---|
| Belegter App-Fehler | Echosperre endet bei Bluetooth vor dem hörbaren Ende (Kopfposition statt Lautsprecher), plus 100-ms-Blockversatz |
| Dokumentierte Grenze | Kein serverseitiges Echo-Filter; Proactive Audio unterdrückt keine Mitschrift; Mitschrift ohne feste Reihenfolge |
| Unbestätigt | Wie groß die Bluetooth-Latenz in Franks Auto wirklich ist; ob `getTimestamp` sie dort meldet; ob Fahrgeräusche zusätzlich Fehlstarts auslösen |
| Ausgeschlossen | Vermischung der Mitschriften in der App; Stille-Halluzination als Erklärung für genau diese Fälle |

## Behebung in 1.8.194

- `live/EchoSperre.kt` (neu, ohne Android prüfbar): Bei Bluetooth bleibt das Mikrofon nach dem Kopf noch
  Ausgabelatenz + 100 ms Block + 400 ms Nachlauf zu. Die Latenz misst die App alle 2 s über `AudioTrack.getTimestamp()`
  (Kopf minus tatsächlich ausgegebener Frame), geglättet, Fehlmessungen über 2 s verworfen; mindestens 1 s, weil der
  Zeitstempel die Puffer im Autoradio nicht kennt. Nach dem Leeren (Antippen, Dazwischenreden) gilt dieselbe Wartezeit,
  denn was schon auf der Funkstrecke ist, holt `flush()` nicht zurück.
- Handy-Lautsprecher und Kabel: unverändert (400 ms).
- Diagnose: je Gemini-Beitrag eine Logbuch-Zeile „Live-Mitschrift (Gemini)“ ohne Inhalt: Wortzahl, Zustand der Sperre beim
  ersten Mitschrift-Stück, Zeit seit dem letzten Ton, Bluetooth ja/nein, gemessene Latenz, **Echoverdacht** (Mitschrift
  besteht aus den letzten eigenen Wörtern am Stück). Der Echoverdacht filtert nichts.
- Bewusst **nicht** eingebaut: ein Textfilter, der verdächtige Mitschrift verwirft. Er würde echte kurze Antworten
  schlucken („übermorgen“ auf „morgen oder übermorgen?“), und bei „Gemini denkt selbst“ hört Gemini das Echo ohnehin selbst
  und antwortet darauf („Garnelen meinst du?“); dagegen hilft nur Stille im Mikrofonstrom.

### Preis der Behebung (Abwägung)

Bei Bluetooth mit Echosperre hört Jarvis erst etwa 0,4 s nach dem tatsächlich hörbaren Ende wieder zu, wenn die gemessene
oder angenommene Latenz stimmt. Ist die echte Latenz kleiner als 1 s, gehen Franks erste Worte verloren, wenn er sehr
schnell antwortet. Die Logbuch-Zeile zeigt die gemessene Latenz; damit lässt sich die Mindestlatenz später anpassen.
Dazwischenreden per Stimme geht mit Echosperre weiterhin nicht (Antippen der Blase).

## Testplan (am Handy, Logbuch über `logbuch_lesen`, Suche „Live-Mitschrift“)

Jeweils im Auto, Tonweg Medienton, Echosperre an, 2–3 Minuten, mehrere Antworten:

| Szenario | So geht's | Erwartung nach der Behebung |
|---|---|---|
| Stille | Frage stellen, danach schweigen, Jarvis ausreden lassen, weitere 30 s schweigen | keine Zeile „Live-Mitschrift“ nach den Antworten; falls doch: Echoverdacht und Sperrzustand notieren |
| Reine Geräusche | Wie oben, aber Fahrgeräusch, Blinker, Radio leise | Zeilen höchstens mit Echoverdacht „nein“; dann Geräusch-Fehlstart, nicht Echo |
| Eigene Wiedergabe | Lange Antwort anfordern („Lies mir meine Aufgaben der Woche vor“), schweigen | keine Zeile mit Echoverdacht „ja“ |
| Echte Sprache | Nach Jarvis' Antwort normal (nicht hastig) antworten | Beitrag kommt vollständig an; Zeile mit „Echosperre auf seit …“ |
| Gegenprobe Loopback | Gleiches Gespräch mit Kabelkopfhörern (Medienton) | keine Phantome; bleiben sie, wäre es kein akustisches Echo |

Am PC zusätzlich: Logcat `JarvisLiveRouting` („Echosperre auf: … Latenz gemessen … genutzt …“, „Mitschrift-Diagnose“) und
die Rückfall-Prüfung aus `best-practices/apis/openai-live-realtime.md` §14.6 am Handy-Lautsprecher (muss unverändert sein).

## Offene Punkte und nächste Schritte

- [offen] Gemessene Bluetooth-Latenz in Franks Auto; ob `getTimestamp` dort plausible Werte liefert (sonst greift die
  Mindestlatenz von 1 s). Nach den ersten Fahrten im Logbuch nachsehen.
- [offen] Bleiben trotz Behebung Zeilen mit Echoverdacht „ja“ und „Echosperre auf seit < 1500 ms“, Mindestlatenz erhöhen;
  erst wenn das nicht reicht, über einen Textfilter für „Jarvis direkt“ nachdenken.
- [offen] Echosperre aus (Dazwischenreden per Stimme) ist im Auto über Bluetooth ohne Echo-Referenz nicht sauber machbar.
  Möglich wären: Freisprechen (SCO, Auto-Mikrofon mit dessen AEC, aber Telefonklang) oder eine eigene Software-AEC mit dem
  Wiedergabesignal als Referenz. Beides ungetestet.
- [offen] `prefixPaddingMs = 20` ist sehr niedrig; 200 ms würde kurze Fehlstarts durch Geräusche seltener machen, verzögert
  aber den erkannten Sprachbeginn. Nicht geändert, weil nicht als Ursache belegt.
