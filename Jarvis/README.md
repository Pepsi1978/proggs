# Jarvis

Persönlicher Assistent auf dem Handy (Android, Kotlin, Jetpack Compose). Paket `de.frank.jarvis`.
Zielbild und Gespräche dazu: `Datenbank/JARVIS/`.

Version 1 kann zwei Dinge:

1. **ChatGPT-Plugin (MCP).** Jarvis ist ein MCP-Server, der auf dem Handy läuft. In ChatGPT wird er als eigener
   Konnektor „Jarvis“ eingetragen. Danach genügt im Chat oder im Sprachmodus „Jarvis, speichere …“.
2. **Eigener Chat.** Jarvis hat ein eigenes Modell aus der ChatGPT-Anmeldung (Codex-Gerätecode), Modell und
   Denkstufe sind wählbar. Es bedient dieselben Werkzeuge selbstständig.

Angebundene Apps: **Geniale Aufgaben** (`de.frank.aufgaben`, lesen und schreiben) und der Biomarker-Bereich von
**Entropie Reductor** (`de.frank.entropyreducer`, nur lesen) , **Geniale Ideen** (`de.frank.genialeideen`, lesen und schreiben), **Genialer Wecker**
(`de.frank.genialerwecker`, lesen und stellen), **Stack-Labor** (`de.frank.stacklabor.werftstudio`, Stacks und Mittel lesen und
bearbeiten) sowie der **Kalender** des Handys (alle synchronisierten
Kalender, also auch Google; nur lesen, Android-Erlaubnis `READ_CALENDAR`).

## Aufbau

```
ChatGPT (Cloud) ──HTTPS──> Jarvis-Relay (eigener Server) ──WebSocket──> Jarvis-App ──ContentProvider──> Geniale Aufgaben
                           srv1774016.hstgr.cloud                       └─ MCP-Server
```

- **Relay** (`server/`): kleiner Dienst auf dem Hostinger-Server, die öffentliche Gegenstelle. Er nimmt
  `POST /j/<Geheimnis>/mcp` an und reicht den Aufruf über eine WebSocket ans Handy weiter. Der MCP-Server selbst
  läuft auf dem Handy; der Relay kennt keine Werkzeuge. Ist das Handy kurz weg, wartet er 8 Sekunden, beantwortet
  `initialize` und `tools/list` aus dem Zwischenspeicher und meldet bei Werkzeugaufrufen „Handy nicht erreichbar“.
- **Verbindung** (`tunnel/Tunnel.kt`): Die App baut die WebSocket von sich aus auf (`wss://<Server>/geraet/ws`),
  weist sich mit dem Server-Schlüssel aus und nennt ihr Geheimnis. Bricht sie ab, wird sie neu aufgebaut
  (2 bis 30 Sekunden Pause, sofort bei zurückkehrendem Netz). Das Handy braucht keinen offenen Port.
- **MCP-Server** (`mcp/McpServer.kt`): zustandslos, nur JSON-Antworten (kein Server-Stream, keine Sitzungen).
- **Dienst** (`dienst/JarvisDienst.kt`): Vordergrunddienst (Typ `specialUse`), hält die Verbindung auch bei
  gesperrtem Handy, startet nach dem Einschalten und nach Updates von selbst.
- **Werkzeuge** (`faehigkeit/`): Jede angebundene App ist eine `Faehigkeit` mit einer Liste von `Werkzeug`en.
  Plugin, eigener Chat und Oberfläche lesen alle dasselbe `Register`.
- **Agent** (`agent/JarvisAgent.kt`): Schleife Modell → Werkzeug → Ergebnis, höchstens so viele Schritte wie unter Einstellungen → Jarvis → „Schrittlimit pro Anfrage“ gewählt (10 bis 100 oder unbegrenzt, Vorgabe 10). Werkzeugaufrufe
  laufen über ein festes JSON-Format im Antworttext, damit jedes Text-Modell Jarvis antreiben kann.

## Zugangsschutz

- **Plugin-Adresse:** `https://<Server>/j/<Geheimnis>/mcp`. Das Geheimnis (40 Zeichen) erzeugt die App; nur das
  zuletzt vom Handy gemeldete gilt. Alles andere beantwortet der Server mit 404. ChatGPT-Konnektoren arbeiten
  ohne Anmeldung, deshalb ist diese Adresse der Schlüssel. Erneuern: Einstellungen → Plugin-Adresse erneuern.
- **Server-Schlüssel:** Nur ein Handy mit dem Schlüssel aus `~/SK/Jarvis/relay.properties` (`host`, `token`) darf
  sich am Relay anmelden. Der Bau am PC backt beides in die App, und seit 1.8.3 speichert die App alles, was ein PC-Bau
  mitbringt (Server, Tavily, Google-Zugang), dauerhaft in den verschlüsselten Einstellungen; spätere Cloud-Bauten (ohne
  `~/SK`) verlieren es dann nicht mehr. Fehlt es trotzdem, trägt man den Schlüssel in der App unter Einstellungen ein.
- Am Server ist dafür Port 443 öffentlich (IPv4 und IPv6), aber nur dieser eine Dienst hängt daran. Second Brain,
  Werft und Dashboard bleiben an der WireGuard-Adresse.

## Werkzeuge im Plugin

Knapp 50 Werkzeuge. Verwandte Aufgaben einer App teilen sich ein Werkzeug (Helfer `als` und `mit` in `faehigkeit/Faehigkeit.kt`);
welche Einzelfunktion arbeitet, entscheidet die Eingabe. So bleibt die Auswahl für ChatGPT überschaubar.

| Werkzeug | Zweck |
|---|---|
| `tagesauswertung_lesen` | Die fertige Tagesauswertung samt frisch gelesenen Aufgaben; `neu_erstellen` stößt eine neue an |
| `tagesdaten_lesen` | Die Tagesdatenbank: alle Daten der letzten Synchronisierung, ganz oder je Bereich |
| `aufgaben_lesen`, `aufgabe_anlegen`, `aufgabe_aendern`, `aufgabe_loeschen` | Geniale Aufgaben; Abhaken über `aufgabe_aendern` mit `erledigt` |
| `kalender_lesen`, `dienstplan_lesen`, `kalender_eintragen`, `kalender_loeschen` | Termine und ausgewerteter Dienstplan; eintragen ganztägig oder mit Uhrzeit, auf Wunsch mit Farbe |
| `wetter_lesen` | Wetter für sieben Tage; mit Datum und Uhrzeit zugleich die Prüfung gegen Dienst, Schlaf und Termine |
| `fahrzeit_lesen`, `abfahrt_einstellen` | Fahrzeit mit Verkehr über Google Maps vom Standort; Losfahr-Meldung vor dem Dienst einstellen |
| `biomarker_auswertung`, `biomarker_tag`, `biomarker_verlauf`, `trainings_lesen`, `biomarker_aktualisieren` | Biodaten; vor jedem Lesen holt Jarvis frische Daten (wenn älter als 30 Minuten), Störungen beim Abruf stehen als „ACHTUNG“ vorn; `biomarker_aktualisieren` stößt den Abgleich an und nennt den Zustand jeder Quelle; `biomarker_verlauf` ohne Messgrößen liefert den Katalog, `trainings_lesen` mit `id` ein Training im Detail |
| `wecker_lesen`, `wecker_stellen`, `wecker_loeschen` | Genialer Wecker; Ändern, Schalten und Auslassen über `wecker_stellen` |
| `ideen_lesen`, `idee_speichern`, `idee_loeschen` | Geniale Ideen; `ideen_lesen` mit `id` liefert den Volltext, `idee_speichern` legt an oder ändert |
| `epigenetik_lesen`, `epigenetik_recherchieren`, `epigenetik_schalter`, `epigenetik_uebersicht` | App Epigenetik (`de.frank.epigenetik.jarvis`); `epigenetik_lesen` liefert die fertige Auswertung zu Mittel, Wirkung, Ziel oder Frage und startet fehlende Recherchen (wartet bis 50 s, sonst läuft sie in der App weiter); `epigenetik_schalter` zeigt ohne Recherche, welche Mittel auf einen Schalter wirken; `epigenetik_uebersicht` listet Bibliothek oder Stack mit Analyse |
| `stacks_lesen` | Stack-Labor lesen: alle Stacks, ein Stack vollständig, alle vollständig, ein Mittel, der Katalog, die Ziele |
| `stack_mittel_setzen`, `stack_speichern`, `mittel_speichern` | Stack-Labor ändern: Mittel in einem Stack (hinzufügen, Dosis, aktiv/inaktiv, kopieren, verschieben), Stacks, Mittel-Katalog |
| `stacklabor_loeschen` | Stack-Labor: Mittel aus Stack, Stack, Mittel aus Katalog, Ziel, Frage löschen oder Mittel zusammenführen; nur mit Bestätigung |
| `tagebuch_lesen` | Tagebucheinträge für einen Tag, einen Zeitraum oder ein Suchwort |
| `wissen_lesen` | Wissens-Datenbank: Inhaltsverzeichnis, Datei im Volltext oder Suche |
| `ablage_lesen`, `ablage_schreiben`, `ablage_loeschen` | Ablage von Jarvis (Texte und Dateien); `ablage_lesen` ohne Titel liefert die Liste mit Dateitypen, laufenden und fehlgeschlagenen Übertragungen |
| `ablage_datei_speichern` | Dateien in einen Ablage-Eintrag übernehmen: ChatGPT-Dateiverweise (`chatgpt_dateien`, `openai/fileParams`), https-Adresse, Text oder kleines Base64 |
| `bild_erzeugen` | Bild oder Infografik über das Codex-Bildwerkzeug erzeugen und als PNG (auf Wunsch auch DIN-A4-PDF) ablegen |
| `agenten`, `agent_starten` | Agenten ansehen, anlegen, löschen und beauftragen; `agent_starten` kehrt sofort mit einer Vorgangskennung zurück. Der Recherche-Agent startet nur auf Franks ausdrücklichen Wunsch (`faehigkeit/SuchRegel.kt`) |
| `vorgaenge`, `vorgang_abbrechen` | Hintergrundaufträge (Agenten und Programmieraufträge): Liste, `lesen` mit Ergebnis, `mitgeteilt`; Abbrechen nur über das eigene, schreibende Werkzeug |
| `mail_senden`, `mail_lesen` | Gmail; `mail_lesen` mit `nr` liefert eine Mail vollständig |
| `jarvis_status` | Erreichbarkeit, Datum, Dienst heute, angebundene Apps, Version und Integrationsversion |
| `jarvis_kontext` | Startkontext (`kurz`, Vorgabe), einzelne Bereiche (`bereiche`), nur Geändertes (`seit`) oder das ganze Gesamtbild (`umfang=voll`) |
| `gedaechtnis_suchen` | Gezielte Suche im ganzen Gedächtnis: Tagebuch, Gespräche, Notizen, Gemerktes, Ideen, Ablage, frühere Auswertungen |
| `logbuch_lesen` | Logbuch der App: Fehler, Dauer, Aktionen, mit Zusammenfassung |
| `web_suche`, `web_seite_lesen` | Jarvis' direkte Websuche (seit 1.8.111 auch im Plugin, seit 1.8.195 auch im eigenen Chat und Live-Gespräch): aktuelle Fragen sofort im Gespräch beantworten; im Gespräch zuerst die Websuche des ChatGPT-Modells, sonst Tavily |
| `sprachsession_beenden` | Jarvis' eigenes Zuhören bzw. Live-Gespräch auf dem Handy beenden (seit 1.8.111 auch im Plugin) |

Fehlt beim Anlegen der Tag, obwohl eine Uhrzeit genannt wurde, fragt ChatGPT nach; ruft es trotzdem auf, lehnt die
Aufgaben-App mit einem Hinweis ab. Mehrere Treffer bei einem Suchwort führen zu einer Rückfrage statt zu einer
geratenen Änderung. Ein wiederholter identischer Anlege-Aufruf innerhalb von 90 Sekunden legt nichts doppelt an.

## Symbiose mit ChatGPT Live (seit 1.8.113)

- **Eine Regeldatei für beide Seiten** (`Symbiose` in `faehigkeit/RegelnFaehigkeit.kt`, `filesDir/symbiose.json`): Die Regeln
  der Zusammenarbeit nach Wichtigkeit (Rollen, Start, wer macht was, Ehrlichkeit, Gedächtnis, lange Arbeit, Programmierer,
  Auslöser je App, Sprechen, Abgleich). Beim ersten Lesen mit der Vorgabe angelegt, danach bearbeitet Frank sie in der App
  unter Einstellungen → Gedächtnis → „Zusammenarbeit mit ChatGPT“ (älteste Regel oben = wichtigste; neue kommen ans Ende).
- **Derselbe Text in ChatGPT**: „Text für ChatGPT kopieren“ legt Überschrift (Stand, Fassung) und Regeln in die Zwischenablage,
  für ChatGPT → Einstellungen → Personalisierung → Benutzerdefinierte Anweisungen (höchstens 4.000 Zeichen; die Vorgabe hat 3.932).
- **In jedem Kontext zuerst**: Der Block steht vor allen anderen Merkdateien (Plugin-Anleitung, `jarvis_kontext`, eigener Chat,
  Agenten; nicht beim Schreiben der Tagesauswertung, die ist für Frank persönlich), mit einem Kopf, der für beide Leser trägt („Du“ = ChatGPT Live, „Jarvis“ = die App).
- **Fassung**: vier Zeichen Prüfsumme über die Regeln, in beiden Texten gleich. Sieht ChatGPT im Startkontext eine andere Fassung
  als in seinen Anweisungen, soll es Frank einmal ans Neukopieren erinnern (Regel 10).
- Bewusst keine `Merkart`: Kein Modell schreibt die Regeln über `jarvis_merken` um, sonst wiche die Kopie bei ChatGPT still ab.
- **Ids [S1] …**: Die Regeln tragen ein eigenes Kennzeichen, damit sie nie mit den ids von FRANKS REGELN verwechselt werden
  (die jarvis_merken ändert). Der Block steht im Kontext zuletzt (gekürzte Anleitungen behalten Franks eigene Regeln); einem
  Nachtrag an Werkzeug-Ergebnisse liegt er nicht bei. Wurde die Datei nach einem Defekt weggeräumt, legt Jarvis die Vorgabe
  nicht still neu an (Franks Fassung liegt in der Sicherung).
- **Grenze**: Ob das Feld in ChatGPT wirklich 4.000 Zeichen fasst, hängt von der ChatGPT-Version ab; nach dem Einfügen prüfen,
  ob Regel [S10] am Ende angekommen ist.
- **Programmierauftrag aus ChatGPT**: bekommt in Jarvis eine eigene Programmier-Session „Programmieren: <App> (über ChatGPT/MCP)“
  mit dem Auftrag als erster Nachricht; Schritte und Rückfragen landen dort (Kennzeichen `ProgrammierChat.AusChatGpt`, gesetzt in
  `McpServer.rufe`). Die Session entsteht erst nach dem gelungenen Start.

## Zusammenarbeit mit ChatGPT (Integration v2, seit 1.8.113 v3: Regeln im Symbiose-Block, hier nur noch die Technik)

Ziel: Frank spricht in der ChatGPT-App, Jarvis ist Gedächtnis, Datenzentrale und ausführende Stelle. Alles, was Jarvis
selbst kann, geht auch über das Plugin (alle Fähigkeiten stehen seit 1.8.111 im Plugin).

- **Grundregeln** (`mcp/Integration.kt`, versioniert): stehen vorn in der Anleitung (`initialize`), in der Beschreibung
  von `jarvis_kontext` und am Anfang jedes `jarvis_kontext`-Ergebnisses. Sicher beim Modell kommen nur
  Werkzeugbeschreibungen und -ergebnisse an; ob ChatGPT die `initialize`-Anleitung nutzt, ist nicht belegt.
- **Startkontext** (`Lagebild.kurz`): Regeln, Fähigkeiten, Hintergrundaufträge, Gemerktes, Neues der letzten 24 Stunden
  und die Kernbereiche gedeckelt, je Bereich mit Datenstand; dazu die Liste der übrigen Bereiche mit Stand und Größe.
  Liest nur schnelle, lokale Bereiche nach, nie eine externe Synchronisation. Jede Antwort trägt eine `STAND-MARKE`;
  `seit=<Marke>` liefert danach nur Neuigkeiten und geänderte Bereiche.
- **Gedächtnisrückfluss** (`jarvis_merken`): neue Art `entscheidung` (`filesDir/entscheidungen.json`). Gleiche Einträge
  werden nicht doppelt angelegt, sehr ähnliche (≥ 60 % gemeinsame tragende Wörter) nur mit `neu_erzwingen`; sonst soll
  das Modell den alten Eintrag über seine id ersetzen. `quelle=vorschlag` wird abgewiesen: Nur Franks eigene oder
  bestätigte Angaben, offene Gedanken gehören in Geniale Ideen.
- **Vorgänge** (`agent/Vorgaenge.kt`, `filesDir/vorgaenge.json`): Jeder Agentenauftrag bekommt sofort eine Kennung
  (`V` + 4 Zeichen) mit Stand `angenommen → laeuft → fertig | fehlgeschlagen | abgebrochen`; Programmieraufträge zeigt
  `vorgaenge` mit ihrem Stand (auch Rückfrage). Gleicher Auftrag, solange offen oder innerhalb von 10 Minuten: dieselbe
  Kennung, nichts startet doppelt. Nach einem App-Neustart gelten offene Läufe als abgebrochen (der Lauf ist tot),
  ein nie angelaufener nach 3 Minuten als gescheitert. `abgerufen` (Ergebnis ausgeliefert) und `mitgeteilt` (ChatGPT
  hat bestätigt, es Frank gesagt zu haben) sind getrennt; erst `mitgeteilt` nimmt einen Vorgang aus der Liste.
  Statuswechsel mit Dauer stehen im Logbuch (Art system), ohne Zugangsdaten.
- **Zustellung**: Der MCP-Server ist zustandslos und antwortet nur auf Anfragen (kein Server-Stream, `listChanged=false`),
  der Relay reicht nur Anfrage und Antwort weiter. Jarvis kann ChatGPT deshalb nicht von sich aus ansprechen. Fertige,
  noch nicht mitgeteilte Vorgänge hängen als „VORGANG FERTIG“ an Werkzeug-Ergebnissen (je Vorgang höchstens alle
  5 Minuten), dazu die Benachrichtigung auf dem Handy. Kein Dauerabfragen.
- **Wiederholschutz** (`Wiederholschutz` in `mcp/Integration.kt`): Schreibende Werkzeuge, bei denen Doppeltes schadet
  (anlegen, löschen, Mail, Agent, Programmieren, Merken …), laufen bei identischen Angaben nicht zweimal: Läuft der erste
  Aufruf noch, wartet die Wiederholung auf sein Ergebnis; war er in den letzten 45 Sekunden erfolgreich und kam seither
  kein erfolgreicher Aufruf eines anderen Werkzeugs, bekommt sie dasselbe Ergebnis mit „WIEDERHOLUNG ERKANNT“. Grenze:
  Was Frank in der App selbst löscht und binnen 45 Sekunden mit genau denselben Angaben neu anlegen lässt, gilt als Wiederholung.
- **Fremde Sitzungen**: „VORGANG FERTIG“ hängt nicht an Werkzeugen der Programmier-Sitzungen (`programmier_*`,
  `programmierauftraege`, `pc_status`, `repo_lesen`, `tagesauswertung_lesen`), damit eine Claude-Code-Sitzung kein
  Ergebnis für ChatGPT „verbraucht“. `mitgeteilt` soll nur bestätigen, wer es Frank im Gespräch wirklich gesagt hat.

## Tagesauswertung

`auswertung/Tagesauswertung.kt` und `auswertung/Zeitplan.kt`. Die Tagesauswertungs-Synchronisation schreibt die
Auswertung zu jeder vollen Stunde neu (Abstand einstellbar: 1, 2, 3, 4 oder 6 Stunden) und legt sie unter
`filesDir/tagesauswertung/` ab (die letzten 100). Solange Frank laut Dienstplan schläft, ruht sie
(`KalenderFaehigkeit.schlafzeiten`: vor einem Tagdienst 20 bis 4 Uhr, nach einem Nachtdienst 6 bis 15 Uhr); die erste
volle Stunde nach dem Schlaf läuft immer. An freien Tagen läuft sie durch. Ablauf:

1. Abgleich in Entropie Reductor anstoßen (frische Daten von Whoop, Oura, Waage; höchstens 90 Sekunden; abschaltbar).
2. Daten nach festen Regeln sammeln: Rahmen der nächsten sechs Tage (Arbeitstag oder frei, Schlaf- und freie
   Zeitfenster, `KalenderFaehigkeit.rahmen`), Termine, Wetter, Wecker, Biodaten des Tages, Vergleich gegen 7 Tage, den
   letzten Monat und alle bisherigen Tage, Verlauf von Schlaf und Erholung der letzten 7 Tage, Dienste und Termine der
   letzten 4 Tage, Trainings der letzten 14 Tage, Tagebuch, Ideen, offene Aufgaben.
3. Das eigene Modell schreibt daraus die Auswertung in drei Teilen (RÜCKBLICK, AKTUELL, AUSBLICK) und bezieht die
   Biodaten auf den Dienst (Tagschlaf nach Nachtdienst). Franks Regeln (`faehigkeit/RegelnFaehigkeit.kt`) liegen bei und
   gehen vor. Ist das Modell nicht erreichbar oder die Deutung ausgeschaltet, bleibt der Datenbericht.

Zeitplan: immer genau ein exakter Wecker für den nächsten Lauf (`setExactAndAllowWhileIdle`); neu gestellt nach jedem
Lauf, beim Start, nach dem Einschalten und nach Zeit- oder Zeitzonenänderung. Ein verpasster Lauf wird beim nächsten
Dienststart nachgeholt, wenn er höchstens sechs Stunden zurückliegt. Beim Abruf werden die Aufgaben zusätzlich frisch
gelesen; liegt nur eine veraltete Fassung vor, liefert der Abruf sofort einen frischen Datenbericht und stößt die volle
Auswertung an.

Grenzen: Nach „Stopp erzwingen“ gehen Wecker verloren, bis die App einmal geöffnet wird. Die Vergleiche trennen
nicht zwischen Nacht- und Tagschlaf; das Modell ordnet das über Rahmen und Dienste der letzten Tage ein. E-Mails,
Ablage und Repo fließen nicht in die Auswertung ein.

## Live-Gedächtnis und Gesamtbild

`gedaechtnis/Lagebild.kt`, `gedaechtnis/Waechter.kt`, `gedaechtnis/Gespraechsgedaechtnis.kt`, `agent/Gehirn.kt`,
`agent/Lernen.kt`, `faehigkeit/RegelnFaehigkeit.kt`, Oberfläche `ui/GedaechtnisBildschirm.kt`.

**Ziel:** Bei jeder Frage, egal über welchen Weg (eigener Chat, „Hey Jarvis“, ChatGPT-Plugin, Agenten), liegt der
aktuelle Stand aus allen angebundenen Apps schon fertig im Kontext. Das Gesamtbild wird nicht erst beim Fragen gebaut.

**Lagebild** (`filesDir/lagebild.json`, überlebt Neustarts): 18 Bereiche, je Bereich Text, Lese-Zeitpunkt, Zeitpunkt
und Anlass der letzten inhaltlichen Änderung: Gemerktes (Regeln, Ziele, Wissen über Frank), Rahmen der nächsten 7 Tage,
Termine der nächsten 14 Tage, Aufgaben (heute/überfällig und morgen vollständig, demnächst und Eingang knapp), Wecker,
Stacks aus Stack-Labor (alle Stacks vollständig, siehe Abschnitt Stack-Labor),
Gespräche mit Jarvis, Biodaten live (neuester Tag, Kerngrößen gegen den Monat, Verlauf 10 Tage; gelesen ohne neuen
Abgleich), Tagesauswertung (die neueste gedeutete der letzten 24 Stunden), alle offenen Ideen (25 neueste mit Text,
ältere mit Titel), Trainings 14 Tage, Wetter, Rückblick der letzten 10 Tage (Dienste/Termine und das Fazit jeder
Tagesauswertung aus dem Tagesarchiv), Ablage, ungelesene Mails (nur Absender und Betreff), Wissens-Inhaltsverzeichnis,
Tagebuch. Eine Störung überschreibt nie einen guten Stand (der alte Text bleibt mit Hinweis). Änderungen an Elementen
(neue Idee, erledigte Aufgabe, neuer Termin, neuer Tagebucheintrag …) landen als Neuigkeit im Abschnitt „NEU IN DEN
LETZTEN 24 STUNDEN“ (72 Stunden aufbewahrt).

**Wächter** (läuft im Dienst rund um die Uhr):
- Änderungssignale: Geniale Aufgaben, Geniale Ideen, Genialer Wecker, Stack-Labor und Entropie Reductor rufen bei jeder
  Datenänderung `notifyChange("content://<paket>.jarvis")` auf (Room-InvalidationTracker bzw. zentraler Speicher in den
  Apps, entprellt 0,7 s). Jarvis beobachtet diese Adressen und den Kalender des Handys (`ContentObserver`, entprellt
  1,5 s) und liest genau den betroffenen Bereich neu. Fehlt eine App, versucht er die Anmeldung alle 10 Minuten neu.
- Eigene Schreibaktionen (Plugin, Chat, Agenten): `Lagebild.nachAktion` liest den betroffenen Bereich sofort neu.
- Sicherheitsnetz: jede Minute alle Bereiche, deren Abstand abgelaufen ist (Aufgaben 2 Min., Ideen/Ablage 3, Wecker, Stacks,
  Kalender 5, Biodaten 10, Trainings 15, Wetter/Tagebuch/Mail/Rückblick 30, Wissen 6 Std.); nach Mitternacht alles.
- Beim Fragen werden zusätzlich die schnellen, lokalen Bereiche (älter als 30 s) nachgelesen, höchstens 2,5 Sekunden
  gewartet; was dann noch läuft, landet für die nächste Frage im Speicher.

**Kontextregel** (damit es über die Jahre nicht anwächst, Obergrenze rund 65.000 Zeichen): die letzten 10 Tage
vollständig (Tagebuch je Tag bis 2.500 Zeichen, Gespräche, Dienste, Tagesfazits, Biodaten-Verlauf). Davor je
Tagebuch-Tag eine Kurzfassung in Stichpunkten (vom Lernlauf geschrieben, `filesDir/tagebuch_kurz/`, mit Prüfsumme:
ändert Frank einen Eintrag, wird sie neu geschrieben), Monate ganz vor den letzten fünf Wochen als Monats-Kurzfassung
(`filesDir/tagebuch_verdichtet/`, ebenfalls mit Prüfsumme). Monate und letzte Tage haben feste Teilbudgets; es fällt
immer das Älteste weg, nie mitten im Text.

**Tiefes Gedächtnis:** `gedaechtnis_suchen` sucht gezielt über alles, was Jarvis je gesehen hat: jeden
Tagebucheintrag, verdichtete Monate, alle Gespräche, Gesprächsnotizen, Gemerktes, Ideen (auch umgesetzte), Ablage und
das Tagesarchiv der Auswertungen (`filesDir/tagesauswertung_archiv/<Datum>.txt`, die letzte gedeutete Fassung je Tag,
ohne Löschfrist). Lokal, deshalb schnell.

**Lernen:**
- Merkdateien `regeln.json`, `ziele.json`, `ueber_frank.json`; Werkzeuge `jarvis_merken`, `jarvis_vergessen`,
  `jarvis_gemerktes_lesen`. Schreiben dürfen nur das Gespräch mit Frank und die Lernläufe; Agenten und die
  Tagesauswertung lesen nur.
- Aus dem Tagebuch (`Lernen.laufe`, nach jeder Auswertung, abschaltbar): einmal am Tag bis zu drei Ziele oder
  Tatsachen aus den Einträgen seit dem zuletzt verarbeiteten Tag (Ziele werden nur angelegt, nie ersetzt; ersetzte
  Infos stehen mit alter Fassung in `merk_verlauf.jsonl`), danach Tages-Stichpunkte, danach ein Monat verdichten.
  Jeder Schritt hat sein eigenes Zeitlimit; ein Monat, der dreimal scheitert, bekommt eine Notfassung.
- Aus den Gesprächen (`Gespraechsgedaechtnis.lerne`): Ruht ein Gespräch zehn Minuten, liest Jarvis Franks neue
  Aussagen (Jarvis' Antworten nur als Zusammenhang) und die Aktionen über ChatGPT und übernimmt Ziele, Tatsachen,
  Regeln (nur wenn Frank ausdrücklich „immer“, „nie“, „ab jetzt“ … sagt) und kurzlebige Gesprächsnotizen mit
  Ablaufdatum (`gespraechsnotizen.json`, Vorhaben, Zusagen, offene Themen; höchstens 30 Tage). Alles Gelernte steht
  unter Aktivität.

**Plugin:** `jarvis_kontext` liefert das Lagebild. Weil der Relay Gespräche nicht unterscheiden kann, hängt Jarvis an
jedes Werkzeug-Ergebnis eine kurze Erinnerung an `jarvis_kontext`, reicht Gedächtnis und Neuigkeiten genau einmal nach
einer Änderung (sonst höchstens stündlich) nach, und `tagesauswertung_lesen` nennt, was seit der Auswertung neu ist.

**Oberfläche:** Startbildschirm → Karte „Live-Gedächtnis“: alle Bereiche mit Stand und Inhalt, Neuigkeiten,
Gesprächsnotizen (löschbar), „Jetzt aus den Gesprächen lernen“, Logbuch herunterladen.

## Stack-Labor

`faehigkeit/StackLaborFaehigkeit.kt` in Jarvis, die Fachlogik in Stack-Labor unter `bruecke/` (`JarvisBruecke.kt` als
ContentProvider `content://de.frank.stacklabor.werftstudio.jarvis`, `JarvisStackDienst.kt`, `Namensaufloesung.kt`,
`DosisEingabe.kt`). Die Brücke spricht über das Repository der App, also mit denselben Prüfungen und Transaktionen wie die
Oberfläche, und liefert fertige deutsche Texte.

- **Lesen und Schreiben getrennt:** `stacks_lesen` liest nur; `stack_mittel_setzen`, `stack_speichern`, `mittel_speichern`
  schreiben; `stacklabor_loeschen` ist das einzige löschende Werkzeug. Agenten und die Tagesauswertung bekommen nur
  `stacks_lesen` (`StackLaborFaehigkeit.nurLesend`), ändern darf nur das Gespräch mit Frank.
- **Namen:** Frank spricht Stack-Namen frei („Sportmodus 2“, „Sport-Stacks 2“, „zweiter Sport-Stack“ → „Sport 2“, „nach dem
  Aufstehen 1“ → „Nach Aufstehen 1“). Zahlen gelten streng, bei mehreren Treffern kommt eine Rückfrage mit den Kandidaten.
  Mittel werden zuerst im Stack, dann im Katalog gesucht; kleine Hörfehler („Glyzin“) werden erkannt.
- **Dosis:** `menge` ist die Gesamtmenge je Einnahme, wie gesagt: „2 Gramm, zwei Kapseln“ wird 2 × 1 g, „6 Gramm“ wird 1 × 6 g.
  Einheiten werden nie umgerechnet (g bleibt g, mg bleibt mg); nur zum Vergleich gelten 6 g und 6000 mg als gleich. Fehlt die
  Einheit oder die Dosis, geht die Teilung nicht glatt auf oder ist beim Ändern nur die Stückzahl genannt, fragt die Brücke nach.
  Getrennte Dosen für freie Tage und Diensttage (`variante`).
- **Doppel-Schutz:** Gleicher Name = gleiches Mittel bzw. gleicher Stack; ein schon vorhandener Eintrag mit gleicher Dosis
  bleibt unverändert, mit anderer Dosis kommt eine Rückfrage statt einer stillen Änderung. Ein neues Mittel entsteht nur mit
  `neues_mittel` (nach Franks Ja); bei sehr ähnlichem Namen fragt die Brücke nach. Alle Schreibaufrufe laufen nacheinander.
- **Sicherheit:** Signatur-Erlaubnis `de.frank.jarvis.permission.BRUECKE`. Gesperrte Stacks (Schloss in der App) ändert Jarvis
  nie und entsperrt sie nie. Löschen nur mit `bestaetigt: true`; ohne nennt die Brücke, was verloren ginge, danach liefert sie
  den alten Stand zum Wiederherstellen.
- **Bestätigung:** Nach jeder Änderung liest die Brücke frisch nach; erst „Gespeichert und nachgelesen“ gilt als erledigt.
  Ist die App nicht erreichbar, steht das so im Ergebnis.
- **Gedächtnis:** Bereich „Stacks“ im Lagebild: alle Stacks in App-Reihenfolge mit Zeitpunkt und Einnahmehinweis (die
  verbindliche Beschreibung), Zielen und allen Mitteln mit Dosis, Kapselanzahl, Frequenz, Notiz und aktiv/INAKTIV. Stack-Labor
  meldet jede Änderung (auch aus der eigenen Oberfläche) über `notifyChange`, Jarvis liest den Bereich dann neu; eigene
  Änderungen frischen ihn sofort auf. Regeln für die Modelle (in `hinweise`): nur aktive Mittel zählen zur geplanten Routine,
  ein geplanter Stack belegt keine Einnahme, Aufstehen/Sport/Schlafen-Zuordnung und 1 vor 2.
- **Tests:** `StackLabor/app/src/test/.../bruecke/` prüft die Beispielbefehle („Füge Ashwagandha in Sport 2 hinzu, 2 Gramm, zwei
  Kapseln“, „Füge Glycin in Sport 2 hinzu, 6 Gramm“) mit erfundenen Daten im Speicher, nie mit Franks echten Daten.
- Neue Werkzeuge: In ChatGPT die Werkzeugliste des Konnektors einmal neu laden.

## Logbuch

`observability/Logbuch.kt`: Mitschreiber der ganzen App für spätere Verbesserungen. Je Tag
`filesDir/logbuch/JJJJ-MM-TT.jsonl`, eine JSON-Zeile je Ereignis (`z` Zeit, `k` Art: aktion, fehler, dauer,
gedaechtnis, netz, sprache, system; `e` Ereignis; `d` Einzelheiten; `ms` Dauer; `v` Version), 14 Tage, höchstens 6 MB
je Tag, geschrieben auf eigenem Faden. Erfasst: jede Protokoll-Aktion, Fehler und Abstürze (mit Stapel), Dauer jeder
Antwort (Gesamtbild, Modellschritte, Werkzeuge), Plugin-Aufrufe, Gedächtnis-Auffrischungen und Änderungssignale,
Verbindungswechsel, Tagesauswertung, Lernläufe, Sprache. Nie: Schlüssel, Passwörter, Token, Mail-Inhalte, Prompts.

Abholen: `pwsh Jarvis/werkzeuge/logbuch-holen.ps1` (adb per WLAN, legt Logbuch, logcat und `zusammenfassung.txt`
unter `Jarvis/logs/<Zeit>/` ab, nicht im Git). In der App: Live-Gedächtnis → Logbuch speichern (Download/Jarvis).
Werkzeug `logbuch_lesen` (Zusammenfassung, häufigste Fehler, langsamste Abläufe, Einträge gefiltert).

## Tagesdatenbank

Bei jeder Tagesauswertung (also bei jeder Synchronisation) legt Jarvis die gesammelten Daten aller Apps ab: Rahmen der
nächsten Tage, Termine, Biodaten des Tages und im Vergleich, Trainings und alle offenen Ideen (gekürzt). Das Werkzeug
`tagesdaten_lesen` gibt sie ganz oder je Bereich zurück. Aufgaben sind nicht enthalten, sie werden immer frisch gelesen.
Die einzelnen Werkzeuge der Apps lesen weiterhin live vom Handy; die Tagesdatenbank spart die vielen Einzelaufrufe.

## Ablage und Agenten

- **Ablage** (`ablage/`, Werkzeuge in `faehigkeit/AblageFaehigkeit.kt`, Oberfläche `ui/AblageBildschirm.kt`): Einträge aus
  einem lesbaren Text und beliebig vielen Dateien, siehe Abschnitt „Ablage: Dateien und Medien“.
- **Agenten** (`agent/Agenten.kt`): Ein Agent ist ein Name plus eine Rolle. Eingebaut sind „Recherche“ und
  „Machbarkeit“; mit `agent_anlegen` entstehen weitere (Dateien unter `filesDir/agenten/`). `agent_starten` kehrt
  sofort zurück; der Lauf (höchstens 14 Schritte, 12 Minuten) passiert im Dienst mit dem eigenen Modell, den
  Werkzeugen aller Apps und der Internet-Suche. Das Ergebnis landet in der Ablage, auf Wunsch zusätzlich per Mail,
  und eine Benachrichtigung meldet es.
- **Internet-Suche** (`faehigkeit/WebFaehigkeit.kt`): Tavily, wenn ein Schlüssel da ist (`~/SK/Tavily/tavily-api-key.txt`,
  beim Bau eingebacken); sonst die eingebaute Websuche des ChatGPT-Modells (Codex-Werkzeug `web_search`, wie News Kompass),
  Seiten liest Jarvis dann selbst. Nur für Jarvis und seine Agenten, nicht im ChatGPT-Plugin.

## Ablage: Dateien und Medien

Ein Eintrag hat einen Titel, optional einen Text (Markdown) und beliebig viele Anhänge, etwa Bericht + PDF + Bilder.

- **Speicher** (`ablage/AblageSpeicher.kt`): Texte wie bisher unter `filesDir/ablage/<Titel>.md`, Dateien unter
  `filesDir/ablage-dateien/<Eintrag>/<Anhang>.<endung>` (Speichername aus der ID, gleichnamige Dateien überschreiben sich nie),
  Verzeichnis `filesDir/ablage-index.json` (atomar geschrieben, Vorgängerfassung `.bak`). Je Datei: ID, Eintrag, Titel,
  ursprünglicher Name, Endung, MIME-Typ, Größe, Zeitpunkt, Herkunft, SHA-256, Beschreibung, Vorschaubild, Kennung.
  Migration: Vorhandene Textdateien werden beim ersten Laden zu Einträgen; nichts wird verschoben oder gelöscht.
- **Übernahme**: Text, Base64 (bis etwa 700 kB, Grenze des Relays: 1 MB je Aufruf) oder https-Download
  (`ablage/Uebertragung.kt`). Downloads laufen erst in `ablage-dateien/.teil/`, werden auf Länge geprüft und erst danach
  übernommen; Abbrüche lassen sich fortsetzen (HTTP Range), offene Aufträge überstehen einen Neustart. Adressen erscheinen
  nur als Server-Name in Texten, Protokoll und Oberfläche. Doppelschutz über `kennung` (ChatGPT: `file_id`) und gleichen Inhalt.
- **ChatGPT-Dateien**: `ablage_datei_speichern` erklärt `_meta["openai/fileParams"] = ["chatgpt_dateien"]` mit Dateiobjekten
  (`download_url`, `file_id` Pflicht; `mime_type`, `file_name`) nach der Plugin-Referenz. Ob ChatGPT darüber auch selbst
  erzeugte Bilder übergibt, ist am Gerät noch nicht geprüft.
- **Bilder** (`bild_erzeugen`): derselbe Weg wie in News Kompass, Werkzeug `image_generation` am Codex-Endpunkt
  (`CodexAuthManager.generateImages`); Größe 1024×1536 für DIN A4 (lehnt der Dienst die Größe ab, ein Versuch ohne).
- **Dauer**: Ein Plugin-Aufruf wartet höchstens 40 Sekunden (der Relay bricht nach 75 ab). Läuft es länger, meldet das
  Werkzeug „läuft noch“, die Arbeit geht im Hintergrund weiter und eine Benachrichtigung meldet erst die fertige Speicherung.
  Agenten warten bis zu 10 Minuten; Dateien eines Agentenlaufs landen im selben Eintrag wie sein Bericht.
- **Vorschau** (`ui/AblageVorschau.kt`): PNG, JPEG, WebP, BMP (gekachelt mit SubsamplingScaleImageView: Zoom, Doppeltipp,
  Verschieben, „An Bildschirm anpassen“), GIF (animiert ab Android 9), SVG (WebView ohne Skripte), PDF (PdfRenderer, Seiten,
  Zoom; Passwort und Beschädigung mit Meldung), TXT, Markdown, Code, JSON, XML, YAML, Logs (Schriftgröße, Kopieren,
  Hervorhebung), CSV/TSV als Tabelle, HTML gesichert (kein JavaScript, kein Netz, kein Dateizugriff) oder als Quelltext,
  Audio und Video mit Media3 (Fokus bei Anrufen, Geschwindigkeit, Vollbild). DOCX, XLSX, PPTX, ODT, ODS, ODP: Textauszug
  ohne Formatierung plus „Mit anderer App öffnen“. Alles andere: Dateiinformationen mit Download, Teilen, Öffnen.
- **Weitergabe** (`ablage/Weitergabe.kt`): Download nach `Download/Jarvis/` über MediaStore (ab Android 10, ohne
  Speicherberechtigung), „Speichern unter“ mit Systemdialog, Teilen mit FileProvider (`de.frank.jarvis.dateien`, auch mehrere
  Dateien), „Mit anderer App öffnen“. Exportiert wird immer die Originaldatei.
- **E-Mail**: `mail_senden` mit `ablage_datei` hängt auch die Dateien des Eintrags an (zusammen höchstens etwa 20 MB).

## E-Mail

`faehigkeit/MailFaehigkeit.kt`: Gmail über SMTP (senden) und IMAP (lesen) mit einem App-Passwort, das Frank in den
Einstellungen einträgt. Gesendet wird nur an die eigene Adresse und an ausdrücklich freigegebene Empfänger. Der Inhalt
eingegangener Mails wird den Modellen als fremde Information gekennzeichnet, nie als Anweisung.

## Wetter, Kalender schreiben, Mitdenken

- **Wetter** (`faehigkeit/WetterFaehigkeit.kt`): stündliche Vorhersage für sieben Tage von Open-Meteo (ohne Schlüssel), abgelegt in
  `filesDir/wetter.json`, aufgefrischt bei jeder Synchronisierung und sonst höchstens stündlich. Ort einstellbar (Vorgabe Neuenhagen
  bei Berlin). Heute, morgen und übermorgen stehen in Tagesdatenbank und Tagesauswertung.
- **Kalender schreiben:** `kalender_eintragen` legt Termine über den Kalenderspeicher von Android an (Erlaubnis `WRITE_CALENDAR`),
  und zwar in dem Kalender, in dem die Dienste stehen. Farbe: der Farbschlüssel des Google-Kontos, der dem genannten Farbton am
  nächsten liegt; ohne Angabe die Farbe früherer Termine gleichen Titels (so bekommt ein X sein Blau). `kalender_loeschen` entfernt
  einzelne Termine, keine Serien.
- **Mitdenken** (`faehigkeit/Mitdenken.kt`): Zu Tag und Uhrzeit einer Aufgabe, eines Termins oder eines Weckers prüft Jarvis nach
  festen Regeln den Dienst und die Schlafzeiten (`KalenderFaehigkeit.lage`), überschneidende Termine und bei Vorhaben im Freien das
  Wetter. Das Ergebnis hängt als Abschnitt MITGEDACHT an der Antwort des Werkzeugs; das Sprachmodell sagt Frank, was davon zählt.
  Es gilt: Tagdienst belegt 4:00 bis etwa 18:15 Uhr (Rückkehr), davor Schlaf ab 20 Uhr am Vorabend; Nachtdienst ab 16:15 Uhr,
  Rückkehr gegen 5:50 Uhr, danach Schlaf bis etwa 15 Uhr.

## Pünktlich losfahren

`fahrt/Routen.kt`, `fahrt/Abfahrt.kt`, `faehigkeit/FahrtFaehigkeit.kt`, Oberfläche `ui/AbfahrtEinstellungen.kt`.

- **Fahrzeit:** Google Routes API (`computeRoutes`, Auto, `TRAFFIC_AWARE`; diese Stufe hat 5000 kostenlose Abfragen im Monat).
  Jarvis zählt die Abfragen je Monat mit und hört bei 4800 auf. `TRAFFIC_AWARE_OPTIMAL` wäre eine teurere Stufe mit
  kleinerem Freikontingent und wird nicht benutzt. Schlüssel: `~/SK/Jarvis/maps-api-key.txt`, ersatzweise der von
  Entropie Reductor (gleiches Google-Projekt `entropie-reductor-495614`, dort muss die „Routes API“ aktiviert sein);
  beim Bau eingebacken, sonst in den Einstellungen einzutragen.
- **Start:** der Standort des Handys (`LocationManager`, Erlaubnis „Immer zulassen“, weil Jarvis im Hintergrund arbeitet),
  ersatzweise die Zuhause-Adresse.
- **Losfahr-Meldung** (`Abfahrt`): Für den nächsten Arbeitstag laut `KalenderFaehigkeit.schichten` (Tag 1–4, Nacht 1–4,
  ohne X, F, U) gilt die eingestellte Ankunft (Nachtdienst 17:00, Tagdienst 05:00). Prüfungen: 150 Minuten vor der
  Ankunft (mit dem für die Abfahrt erwarteten Verkehr), dann 30 und 8 Minuten vor der Meldung und zur Meldung selbst;
  die Meldung kommt 10 Minuten (einstellbar) vor der nötigen Abfahrt als Benachrichtigung (Kanal „Losfahren“, mit
  „Navigation starten“ und „Heute still“) und wird mit der eingerichteten Stimme vorgelesen. Kurz vor der Abfahrt prüft
  Jarvis noch einmal; ist die Fahrt mehr als 5 Minuten länger geworden, folgt eine zweite Meldung. Ist das Handy schon
  näher als 1,5 km an der Arbeit, bleibt Jarvis still. Immer genau ein exakter Wecker (Anfragenummer 8), der Stand liegt
  in den Einstellungen (`abfahrt_zustand`), damit ein Neustart nichts doppelt meldet. Etwa 5 bis 7 Abfragen je Dienst.
- **Werkzeuge:** `fahrzeit_lesen` (Ziel „arbeit“, „zuhause“ oder Adresse; mit `ankunft` die nötige Abfahrt) und
  `abfahrt_einstellen` (Ankunftszeiten, Vorlauf, Adressen, an/aus).

## Tagebuch

Die Einträge liegen als `JJJJ-MM-TT_Tagebucheintrag.md` in einem Google-Drive-Ordner. Der Relay auf dem Server holt sie
mit rclone über einen eigenen NUR-LESE-Zugang (`scope = drive.readonly`, als Wurzel genau dieser Ordner; Konfiguration
unter `/data/rclone.conf` im Volume des Containers, Kopie in `~/SK/rclone/gdrive-lesen.conf`) und gibt sie unter
`/geraet/tagebuch` nur mit dem Geräte-Token heraus. Jarvis holt sie bei jeder Synchronisierung nach
`filesDir/tagebuch/` (`faehigkeit/TagebuchFaehigkeit.kt`) und liest dann lokal. Die letzten sieben Tage stehen in der
Tagesdatenbank; die Tagesauswertung fasst sie im Abschnitt RÜCKBLICK in Stichpunkten zusammen.

Zugang erneuern (falls Google ihn widerruft): in Jarvis unter Einstellungen → Tagebuch auf „Berechtigung erneuern“ tippen
(`tunnel/DriveFreigabe.kt`: Zustimmungsseite von Google im Browser, Rückleitung an 127.0.0.1 auf dem Handy, Weitergabe an
den Relay unter `/geraet/drive-zugang`). Ersatzweg am PC: `rclone config reconnect gdrive-lesen: --config ~/SK/rclone/gdrive-lesen.conf`,
dann die Datei per `scp` auf den Server und mit `docker cp` nach `jarvis-relay:/data/rclone.conf` (Besitzer `relay`).

## Sprache

Übernommen aus Geniale Ideen (Pakete `audio/`, `tts/`, `speech/`, `text/`, `observability/`, `data/settings/SecureSettings.kt`):

- **Mikrofon:** Solange das Eingabefeld leer ist, ist der Knopf daneben das Mikrofon. Aufnahme mit `MicRecorder`, Mitschrift
  über Groq (`whisper-large-v3-turbo`) mit den vier Filtern gegen erfundene Texte bei Stille; lange Aufnahmen werden geteilt.
  Der Text geht direkt an Jarvis, die Antwort wird vorgelesen (abschaltbar).
- **Vorlesen:** `speech/Vorleser.kt` liest absatzweise und lässt die nächsten zwei Absätze schon synthetisieren
  (Edge ohne Schlüssel, Google Chirp 3 HD, Alibaba, eigene Stimme). `VorleseDienst` hält die Wiedergabe im Hintergrund.
  Lautsprecher-Knöpfe gibt es an Antworten, an der Tagesauswertung und in der Ablage; Agenten-Ergebnisse werden auf
  Wunsch von selbst vorgelesen (Schalter in den Einstellungen oder `vorlesen` bei `agent_starten`).
- **Hey Jarvis** (`weckwort/`): Der Dienst `WeckwortDienst` (Vordergrunddienst Typ Mikrofon) lauscht dauerhaft, auch bei
  gesperrtem Handy. Erkennung offline mit den vortrainierten openWakeWord-Modellen (`assets/weckwort/`, Lizenz dort:
  das Weckwort-Modell ist nur nicht-kommerziell nutzbar) über ONNX Runtime, nachgebaut in `WeckwortErkenner` und gegen
  die Python-Vorlage geprüft. Ablauf: Treffer → Mikrofon frei → `MainActivity` mit `EXTRA_WECKWORT` → Aufnahme, Piepton,
  automatisches Ende nach 1,6 Sekunden Stille (`SprechEnde`), Mitschrift, Antwort wird vorgelesen. Bei gesperrtem Handy
  erscheint Jarvis über dem Sperrbildschirm, zeigt nur den Status (das Gespräch bleibt hinter der App-Sperre) und gibt
  den Bildschirm nach der Antwort wieder an die Sperre zurück. Während Jarvis selbst aufnimmt oder vorliest, lauscht
  der Dienst nicht (`Weckwort.belegt`), so weckt er sich nicht selbst. Nach vorn holen darf sich Jarvis aus dem
  Hintergrund nur mit „Über anderen Apps einblenden“; ohne kommt eine Benachrichtigung zum Antippen. Starten darf
  Android den Mikrofon-Dienst nur, während Jarvis sichtbar ist: aus der App, nach Neustart und Update über die
  unsichtbare `WeckwortDienst.Starter`-Seite. Empfindlichkeit (Schwelle 0,7 / 0,5 / 0,3) unter Einstellungen → Hey Jarvis.
- Schlüssel (Groq, Google, Alibaba) trägt man in den Einstellungen ein. Sie liegen in `jarvis_sprache_prefs`, getrennt von
  den übrigen Einstellungen, damit das übernommene Modul unverändert bleibt.

## App-Sperre

Mit eingeschalteter Sperre (Vorgabe) verlangt Jarvis beim Öffnen und nach 30 Sekunden außerhalb der App den Fingerabdruck
oder die Gerätesperre (`MainActivity`, `androidx.biometric`). Der Hintergrunddienst, das Plugin und die Tagesauswertung
laufen davon unberührt weiter. Ohne eingerichtete Gerätesperre bleibt die App offen.

## Dienstplan

Der Dienstplan steht als Ganztagstermine im Kalender und wird in `faehigkeit/KalenderFaehigkeit.kt` ausgewertet:
„Nacht 1“ bis „Nacht 4“ = Nachtdienst (Abfahrt etwa 16:15 Uhr), „Tag 1“ bis „Tag 4“ = Tagdienst (Abfahrt etwa 4:30 Uhr).
Steht am selben Tag „X“ oder „F“, ist frei, bei „U“ Urlaub; der Diensteintrag bleibt im Kalender stehen und entfällt.
Tage ohne Diensteintrag sind frei. Die Abfahrtszeiten stehen als Konstanten in derselben Datei.

## Auswertungen: wer rechnet

Die Biomarker-Auswertung rechnet Entropie Reductor selbst (Durchschnitte, Abweichung, Einordnung) und gibt nur
Kennzahlen zurück. Das Sprachmodell in ChatGPT deutet und formuliert. So bleibt eine Antwort im Sprachmodus unter
einer Sekunde Werkzeugzeit, und es geht nie die ganze Historie über die Leitung. Der eigene Agent von Jarvis
(`jarvis_auftrag`) ist für mehrstufige Aufträge gedacht und für Sprache zu langsam.

Nach neuen Werkzeugen muss ChatGPT die Werkzeugliste neu laden (Plugin öffnen → aktualisieren bzw. neu verbinden).

## Einrichten

1. Jarvis öffnen, mit ChatGPT verbinden (Gerätecode), Modell wählen.
2. Unter Einstellungen die Plugin-Adresse kopieren. Am PC auf chatgpt.com: Einstellungen → Apps und Konnektoren →
   Erweitert → Entwicklermodus → Erstellen: Name „Jarvis“, MCP-Server-URL = Plugin-Adresse, Authentifizierung „Keine“.
3. Akku-Sparen für Jarvis ausschalten (Knopf in der App); bei Samsung Jarvis zusätzlich unter
   „Nie in Standby versetzen“ eintragen.

## Server ausrollen

Wie beim Second Brain liegt auf dem Server kein Git. Dateien hochladen und neu bauen:

```
scp -i ~/SK/second-brain/id_ed25519 server/compose.yaml server/Caddyfile root@168.231.83.205:/opt/jarvis/
scp -i ~/SK/second-brain/id_ed25519 server/relay/relay.py server/relay/Dockerfile root@168.231.83.205:/opt/jarvis/relay/
ssh -i ~/SK/second-brain/id_ed25519 root@168.231.83.205 "cd /opt/jarvis && docker compose up -d --build"
```

`/opt/jarvis/.env` enthält `JARVIS_GERAET_TOKEN` (Kopie: `~/SK/Jarvis/server.env`). Für IPv6 gibt es die
UFW-Regel „Jarvis-Relay (IPv6)“; IPv4 läuft über Dockers Portfreigabe an der Firewall vorbei.
Prüfen von außen: `curl -4 -o /dev/null -w "%{http_code}" https://srv1774016.hstgr.cloud/` muss 404 liefern.

## Eine weitere App anbinden

1. In der Ziel-App einen ContentProvider nach dem Muster `Aufgaben/.../bruecke/JarvisBruecke.kt` anlegen, geschützt
   mit der Signatur-Erlaubnis.
2. In Jarvis eine Klasse `XyFaehigkeit : Faehigkeit` schreiben und in `Register.alle` eintragen, dazu den
   `<queries>`-Eintrag im Manifest.

## Signierung

Jarvis und die angebundenen Apps müssen mit demselben Schlüssel signiert sein (gemeinsamer Debug-Key aus
`~/SK/Android/`), sonst verweigert Android den Zugriff auf die Brücke. Beide Apps erklären die Erlaubnis
`de.frank.aufgaben.permission.JARVIS`, deshalb ist die Installationsreihenfolge egal.

## Noch offen

- Ob der ChatGPT-Sprachmodus eigene Konnektoren aufruft und ob er Schreibaktionen ohne Bestätigungsdialog ausführt,
  muss am Gerät geprüft werden. Falls nicht: GPT Live direkt in Jarvis einbauen (Werkzeuge und Agent stehen bereit).
- Bei tiefem Stromsparmodus ohne Ladekabel kann Android die Verbindung trotz Dienst kappen.

## Programmieren per Sprache (`programmieren`)

„Jarvis, programmiere in App … ein, dass …“ → Werkzeug `programmieren` (`faehigkeit/ProgrammierFaehigkeit.kt`). Zwei Ziele, beide
über Franks Claude-Abo (keine API-Kosten):

1. **Rechner** (zuerst der Mac, siehe „Mac“ unten, sonst der Windows-PC): Läuft am PC die Brücke `werkzeuge/pc-bruecke/jarvis-pc-bruecke.ps1` (Aufgabe „JarvisPcBruecke“ bei der
   Anmeldung, eingerichtet mit `einrichten.ps1`), holt sie den Auftrag über den Relay (`/pc/auftrag`, Long-Polling) ab und startet
   über `OpenLauncher.exe --jarvis-auftrag` eine Sitzung wie der Startknopf: Profil Minimal, Schnellmodus, Opus 5.5 (1M),
   Effort High, dazu Remote Control „Jarvis: <App>“. Die Sitzung steht in der Claude-App unter Code. Ob der PC an ist, erkennt
   der Relay an der Brücke selbst (fragt sie 45 s nicht ab, gilt der PC als aus), nicht an offenen Remote-Control-Sitzungen.
   Protokoll: `%LOCALAPPDATA%\JarvisPcBruecke\bruecke.log`.
2. **Cloud** (wenn der PC nicht abfragt, Relay antwortet 409): Claude-Code-Routine „Jarvis: Programmieren“
   (`trig_01HkrhPAiYAJ7msGLn5UUcyo`), Auslösen per API-Trigger; Token in den Einstellungen oder `~/SK/Jarvis/routine.properties`.
   Danach `apk-update-cloud`. Doku: `best-practices/claude-tooling/claude-code-cloud.md` §8.

Agenten und die Tagesauswertung bekommen das Werkzeug nicht (`JarvisAgent.versuche`).

**Mac:** eigene Brücke, Vorrangrechner vor dem Windows-PC, siehe Abschnitt „Mac“ unten. Einrichtung per Prompt `JARVIS-MACOS-PROMPT.md`.

### Fortschritt und Rückfragen im Jarvis-Chat

- Startet Jarvis einen Auftrag im eigenen Chat, merkt sich das Auftragsbuch das Gespräch (`ProgrammierChat.aktivesGespraech`).
  Jede Meldung erscheint dort als Nachricht (⚙️ läuft, ✅ fertig, ❌ Problem, ⏸️ Zwischenstand), dazu die mitwachsende Benachrichtigung.
  Aus ChatGPT gestartete Aufträge haben kein Gespräch: dort Benachrichtigung und `programmierauftraege`.
- **PC:** Die Brücke reicht die Auftragsnummer an `OpenLauncher.exe --jarvis-auftrag … --auftrag-id <Nr>`. Nur dann setzt der
  OpenLauncher `JARVIS_AUFTRAG_ID` und hängt über `--settings` die Hooks `werkzeuge/pc-bruecke/jarvis_hook.py` an diese eine Sitzung:
  PostToolUse/PostToolUseFailure (Dateien, Bau, Commit, Push, adb, async, höchstens alle 20 s), PreToolUse `AskUserQuestion`
  (Rückfrage mit Optionen), Stop (Zwischenstand, freie Frage am Zugende). Protokoll `%LOCALAPPDATA%\JarvisPcBruecke\hook.log`.
- **Rückfragen:** Hook → Relay `POST /pc/meldung` (mit `frage`) → Signal `programmieren` ans Handy → Karte im Chat mit Knöpfen A–D,
  Mehrfachauswahl und Freitext. Antwort per Knopf oder Sprache (`programmier_antwort`) → Relay `POST /geraet/antwort` → der Hook
  holt sie mit `GET /pc/antwort` ab (Long-Polling) und gibt sie per `updatedInput.answers` an Claude (freie Fragen: Stop-Hook mit
  `decision: block`). Ohne Antwort nach 10 Minuten erscheint der normale Dialog (auch per Remote Control in der Claude-App).
- **Was im Chat ankommt (PC):** jeder Zwischentext der KI (💬, aus dem Transkript, ohne Doppelte), 🧠 Advisor, 🧩 Skill, 🤖 Agent,
  Bau/Commit/Push/Installation und am Zugende die vollständige letzte Nachricht (🏁, bis 4000 Zeichen). Dazu ein Puls bei jedem
  Werkzeugaufruf (höchstens alle 10 s, nur live): Solange die Sitzung arbeitet, steht unter der letzten Nachricht der Claude-Spinner
  (`· ✢ ✳ ✶ ✻ ✽` in Orange, Wort wie „Drizzling…“) mit der Zeit seit der letzten Aktivität (über 3 Minuten rot).
- **Cloud:** keine Hooks; die Sitzung meldet über `programmier_fortschritt` (Connector Jarvis, nur dieses Werkzeug), Rückfragen
  beantwortet Frank in der Claude-App.

### Nachträge in dieselbe Sitzung

- `programmier_nachtrag(auftrag_id, text)` schickt einen freien Nachtrag an genau einen bestehenden Auftrag, nie in eine neue Sitzung.
  Über das Plugin ist die `auftrag_id` Pflicht; im Jarvis-Chat darf sie fehlen, wenn das Gespräch genau einen (offenen) Auftrag hat.
  Mehrdeutig, unbekannt, offene Rückfrage (→ `programmier_antwort`) oder beendete Cloud-Sitzung: Fehler mit Auswahl, nichts gesendet.
- `programmieren` startet neben einem Auftrag desselben Gesprächs (12 h) oder derselben App (offen, oder vor ≤ 2 h fertig) nur mit `neu=true`.
- Auftragsbuch: je Auftrag `nachtraege` mit Stand `angenommen` → `zugestellt` (Sitzung hat ihn) → `bearbeitet` (nächstes
  Zugende bzw. fertig), `fehler` = Server nicht erreicht. Gleicher Text binnen 30 min = derselbe Nachtrag (keine Doppelzustellung).
- **PC:** Handy → Relay `POST /geraet/nachtrag` (Warteschlange je Auftrag, `/data/nachtraege.json`, 7 Tage, nachtrag_id gegen
  Doppelte; wartet die Sitzung, bis 8 s auf die Quittung). Der Hook holt mit `GET /pc/nachtrag` ab: im Stop-Hook am Zugende
  (`decision: block`) und im Warter (`warte`, SessionStart und Stop mit `asyncRewake`, einer je Auftrag, endet mit der Claude-Sitzung),
  der eine ruhende Sitzung mit Exit 2 weckt. Quittung: Meldung `stand: nachtrag` mit `nachtrag_ids`. Der Warter-Eintrag kommt aus
  `OpenLauncherService.JarvisHooks` (ab OpenLauncher 1.24.95); ältere Sitzungen bekommen Nachträge nur am nächsten Zugende.
- **Cloud:** Nachträge reisen mit der Antwort auf `programmier_fortschritt` zurück („NACHTRAG VON FRANK“); nach „fertig“ ist
  die Sitzung beendet, dann meldet Jarvis „nicht zustellbar“ statt eine neue zu starten.

## Mac

Stand 17.10.2026 (OpenLauncherMac 1.24.39). Voraussetzung: `~/SK/Jarvis/` (`relay.properties`, `routine.properties`,
`plugin-url.txt`) vom Windows-PC kopiert, per USB-Stick oder AirDrop, nie über Git oder Cloud.

- **Brücke** `werkzeuge/pc-bruecke/jarvis_pc_bruecke.py` (Python 3, Gegenstück zur .ps1): gleiche Schleife, Endpunkte, Wartezeiten,
  Auftragstext und Quittung, nur IPv4, Rechnername aus `scutil --get ComputerName`, eine Brücke je Benutzer (`fcntl.flock`),
  PATH und JAVA_HOME vor jedem Auftrag frisch aus der Login-Shell. Startet die Sitzung über
  `/Applications/OpenLauncher.app/Contents/MacOS/OpenLauncher --jarvis-auftrag …` (ab 1.24.39, `Services/JarvisAuftrag.swift`):
  Terminal.app, Profil Minimal (`Profiles/ClaudeCodeMac/minimal`), Schnellmodus, Opus 5.5 (1M), Effort High, Remote Control
  „Jarvis: <App>“, mit Nummer dieselben Hooks wie unter Windows (`python3 …/jarvis_hook.py`). Notweg ohne passenden Launcher:
  `claude` direkt in Terminal.app mit eigenem `--settings`.
- **Vorrang Mac** (Franks Wahl vom 25.10.2026, vorher Vorrang Windows): Die Mac-Brücke fragt mit `X-Jarvis-Rang: ersatz`
  (der Name blieb), die Windows-Brücke ohne Rang. Opus-Aufträge holt der Mac; die Windows-Brücke legt sie zurück, solange der Mac
  eine lebende Abfrage hat oder seine letzte vor weniger als 3 s endete. Sol 6.1 holt nur die Windows-Brücke. `GET /geraet/pc` nennt unter `rechner_alle` beide Brücken mit Rang und Alter, `rechner` ist der
  Rechner, der den nächsten Auftrag bekäme.
- **Hook** `jarvis_hook.py` ist eine Datei für beide Systeme: auf dem Mac Protokoll und Zustand in `~/Library/Logs/JarvisPcBruecke/`,
  Claude-Prozess über die Elternkette (`ps -o ppid=,comm=`), Lebenszeichen über `os.kill(pid, 0)`; so endet der Warter mit der Sitzung.
- **LaunchAgents:** `de.frank.jarvis.pc-bruecke` (`werkzeuge/pc-bruecke/einrichten.sh`, RunAtLoad + KeepAlive) und
  `de.frank.adb-wlan.wachhund` (`Werkzeuge/adb-wlan/wachhund-einrichten.sh`, alle 2 Minuten, richtet `adb-wlan.sh` beim ersten
  Lauf selbst ein). Logs: `~/Library/Logs/JarvisPcBruecke/` (`bruecke.log`, `hook.log`) und `~/Library/Logs/adb-wlan/adb-wlan.log`.
- **Plugin-Einträge:** `werkzeuge/plugin-adresse-setzen.sh` trägt die Adresse aus `plugin-url.txt` ein: Claude Code (`~/.claude.json`
  und die Profile `ClaudeCodeMac/minimal|standard|strict`, HTTP), Codex (`~/.codex/config.toml` und das Codex-Zuhause des
  OpenLauncherMac), OpenCode (`~/.config/opencode/opencode.jsonc`, remote) und Claude Desktop (`npx mcp-remote`, IPv4 zuerst).
  Nach „Plugin-Adresse erneuern“ erneut ausführen. ChatGPT hängt am Konto, nicht am Rechner.
- **WireGuard:** eigener Peer „Mac“ = 10.8.0.6 (`~/SK/second-brain/wireguard/mac.conf`, in der WireGuard-App importieren). Der frühere
  Tunnel „second-brain“ der App nutzte die Adresse des Windows-PCs (10.8.0.2) und verdrängte ihn; nicht mehr verwenden.
  `adb-wlan.sh` hat denselben Tunnelschritt wie die .ps1 (`10.8.0.7:5555`, `~/.adb-wlan-tunnel-ip`); die Direktverbindung über
  den PC-Hotspot gibt es am Mac nicht.
- **apk-update:** dasselbe `apk-update.ps1` mit `pwsh`; der Drive-Ordner wird unter `~/Library/CloudStorage/GoogleDrive-*/Meine Ablage/Dokumente/Updates` gefunden.
- **Ruhezustand:** Schläft der Mac (beim MacBook Air immer bei zugeklapptem Deckel ohne externen Bildschirm), fragt die Brücke nicht ab
  und Aufträge gehen an den PC oder in die Cloud; gewollt.
