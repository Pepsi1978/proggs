# Karteikarten Lernen

Version 0.1.38 - 18.07.2026, 02:02 Uhr

## 1. Übersicht

Neue Android-App in `KarteikartenLernen` mit Package
`de.frank.karteikartenlernen`. Verbindliche Designquelle ist
`Designs/KI-Karteikarten-Lern-App/Phone.dc.html` (interaktiver Prototyp v2).
Der Unterordner `v1/` ist eine ältere Fassung und wird nicht umgesetzt.

Die App verbindet sprach- oder textbasierte KI-Recherche mit automatisch erzeugten
Karteikarten, Session-Verwaltung und einer animierten Lernrunde. Oberfläche,
Zustände, Navigation, Themes, Effekte und Motion folgen dem v2-Entwurf 1:1.

## 2. Design-Tokens

### Schriften

| Rolle | Familie | Gewichte | Verwendung |
|---|---|---|---|
| UI | Schibsted Grotesk | 400, 500, 600, 700, 800 | Navigation, Labels, Buttons, Formulare, Werte |
| Editorial | Newsreader | 400, 500, 600, kursiv 400 | Überschriften, Antworttexte, Kartenfragen und -antworten |

Vorkommende Größen werden 1 px zu 1 sp übertragen: `10.5`, `11`, `11.5`, `12`,
`12.5`, `13`, `13.5`, `14`, `14.5`, `15`, `15.5`, `16`, `16.5`, `17`, `18`,
`19`, `20`, `22`, `24`, `26`, `28`, `30`, `32`, `34`, `46`, `60` und `118`.
Vorkommende Zeilenhöhen: `1.1`, `1.35`, `1.42`, `1.5`, `1.55`, `1.6`, `1.62`.
Letter-Spacing: `1`, `1.2`, `1.4` und `1.5` px; Großbuchstaben-Labels nutzen
die im Entwurf angegebenen Abstände.

### Basisfarben

| Token | Dunkel | Hell |
|---|---|---|
| surface | `rgba(30,32,46,0.62)` | `rgba(255,255,255,0.72)` |
| surfaceSolid | `#191b28` | `#ffffff` |
| raised | `rgba(42,45,64,0.80)` | `rgba(255,255,255,0.90)` |
| border | `rgba(255,255,255,0.09)` | `rgba(24,26,48,0.08)` |
| borderHi | `rgba(255,255,255,0.16)` | `rgba(24,26,48,0.14)` |
| text | `#f4f5fb` | `#171930` |
| muted | `#a3a7bd` | `#5b5f74` |
| faint | `#6a6e85` | `#9195a8` |
| green | `#43e79d` | `#0fae72` |
| red | `#ff5d74` | `#e23a54` |
| neutral | `#5b5f76` | `#a7abbd` |
| field | `rgba(255,255,255,0.05)` | `rgba(20,22,45,0.04)` |
| tab | `rgba(14,15,24,0.72)` | `rgba(255,255,255,0.82)` |
| chip | `rgba(255,255,255,0.06)` | `rgba(24,26,48,0.05)` |
| onAccent | `#0b0c12` | `#ffffff` |
| shadow | `0 24px 60px rgba(0,0,0,0.55)` | `0 24px 60px rgba(40,44,80,0.18)` |

### Farbprofile

| Modus | Profil | accent | accent2 | bg0 | bg1 |
|---|---|---|---|---|---|
| Dunkel | Mitternacht | `#d9a95b` | `#7fa8c9` | `#0c0e14` | `#171b28` |
| Dunkel | Bordeaux | `#d98098` | `#c9a06b` | `#130c11` | `#251622` |
| Dunkel | Tanne | `#8fbf9f` | `#c9b06b` | `#0b100d` | `#15221b` |
| Hell | Elfenbein | `#a87c2f` | `#5b7fa6` | `#efeadf` | `#faf7ef` |
| Hell | Salbei | `#537f64` | `#c07a4a` | `#e8efe9` | `#f5faf5` |
| Hell | Porzellan | `#4a5fb5` | `#8a6fc0` | `#e9ecf2` | `#f7f8fb` |

Aurora-Blobs je Profil:

| Profil | blobA | blobB | blobC |
|---|---|---|---|
| Mitternacht | `rgba(217,169,91,0.20)` | `rgba(127,168,201,0.16)` | `rgba(160,120,200,0.10)` |
| Bordeaux | `rgba(217,128,152,0.20)` | `rgba(201,160,107,0.14)` | `rgba(120,110,200,0.10)` |
| Tanne | `rgba(143,191,159,0.18)` | `rgba(201,176,107,0.13)` | `rgba(110,150,200,0.09)` |
| Elfenbein | `rgba(184,134,47,0.16)` | `rgba(91,127,166,0.12)` | `rgba(160,120,200,0.08)` |
| Salbei | `rgba(93,138,111,0.16)` | `rgba(201,138,91,0.12)` | `rgba(110,130,190,0.08)` |
| Porzellan | `rgba(74,95,181,0.14)` | `rgba(138,111,192,0.11)` | `rgba(60,150,170,0.09)` |

### Maße, Formen und Tiefe

- Referenzfläche: `412 x 892` px; App rendert edge-to-edge und responsiv.
- Hauptseitenabstand: `18` dp; Lernkarten-Seitenabstand: `22` dp.
- Häufige Abstände: `2`, `3`, `4`, `5`, `6`, `8`, `9`, `10`, `12`, `14`,
  `15`, `16`, `18`, `20`, `22`, `24`, `26` und `28` dp.
- Die Mikrofon-Aura nutzt ungebundene Blur-Kanten und läuft rundum weich aus,
  ohne an ihrer Renderfläche rechteckig abgeschnitten zu werden.
- Radien: `2`, `3`, `4`, `8`, `10`, `11`, `12`, `14`, `16`, `18`, `22`,
  `26`, `28`, `44` und pillenförmig `99` dp.
- Karten: transluzente Flächen, 1-dp-Rand, Blur-Äquivalent und Profil-Schatten.
- Hintergrund: radialer Verlauf `bg1` bei 30 %/0 % nach `bg0` bei 70 %.
- Aurora: drei weichgezeichnete Kreise mit `60` dp Blur und profilspezifischen Farben.
- Lernkarte: maximal `330 x 432` dp, Radius `28` dp, Perspektive `1400`,
  radial geschichtete Akzent-Glows und zusätzlicher 50-dp-Glow.
- Bottom-Navigation: `72` dp inklusive `10/12/12` dp Innenabständen, aktiver
  Indikator `64 x 37` dp.

## 3. Effekte und Animationen

| Effekt | Vorgabe | Compose-Entsprechung |
|---|---|---|
| auroraA/B/C | 18 s, ease-in-out, unendlich, Translation und Skalierung | InfiniteTransition |
| pulseRing | 2,4 s, ease-out, Skalierung 0,75 bis 2,1, Alpha 0,55 bis 0 | InfiniteTransition mit 0/0,8/1,6 s Versatz |
| micGlow | 3,4 s, ease-in-out, unendlich | animierter Glow/Shadow |
| waveBar | 0,6 bis 1,0 s plus 0 bis 0,32 s Versatz | neun animierte Pegelbalken |
| shimmer | 1,1 s, linear, unendlich | Brush-Offset während Transkription |
| sparkle | 2,4 s, ease-in-out, Rotation 22 Grad, Skalierung 1,18 | InfiniteTransition |
| spin | 0,7/0,8/9 s, linear | Rotationsanimation für Loader bzw. Hintergrund |
| floatUp | 0,3 bis 0,4 s, ease, 14 dp nach 0 dp und Fade-in | AnimatedVisibility |
| cardShake | fünf horizontale Ausschläge bis 9 dp | Keyframes für Fehlerhinweis |
| confettiFall | 1,4 bis 2,4 s, individuelle Delays/Rotationen, unendlich | 46 Partikel |
| ripplePop | Skalierung 0,4 bis 3,2 und Fade-out | Transition |
| breathe | 0,8/2/4,5 s, ease-in-out, Skalierung 1 bis 1,06 | InfiniteTransition |
| Kartenflip | 0,5 s, cubic-bezier(.22,1,.36,1), Y-Rotation 180 Grad | Animatable mit FastOutSlowIn-Ersatz |
| Kartenwertung | 0,52/0,56 s, 3D-Flug rechts/links mit Rotation und Fade | Animatable/graphicsLayer |

Die kurzen Soundeffekte für Umdrehen, Gewusst, Nicht gewusst, Kartenübergang und
Rundenabschluss werden über `SoundPool` abgespielt. Lautstärke und einzelne Ereignisse
sind einstellbar. Textausgabe nutzt Android `TextToSpeech`.

## 4. Themes

Alle sechs Kombinationen sind umschaltbar und werden lokal gespeichert:

- Dunkel: Mitternacht, Bordeaux, Tanne.
- Hell: Elfenbein, Salbei, Porzellan.

Eine eigene `AppPalette`-CompositionLocal bewahrt die Designrollen, die nicht sauber
auf Material-3-Rollen passen. Systemleisten folgen dem Hell-/Dunkelmodus. Das
ausgewählte Farbprofil wird getrennt je Erscheinungsbild gespeichert.

## 5. Screens und Zustände

1. **Recherche, leer:** Kopfzeile, animiertes Mikrofon und Eingabefeld,
   Sprachaufnahme, Deutsch-Verbesserung, Rückgängig und Absenden. Der Modell-Chip
   liegt fest unten rechts über der App-Navigationsleiste und niemals im Eingabefeld.
   Das Eingabefeld wächst von `102` bis maximal `306` dp und scrollt danach per Touch
   ohne sichtbare Scrollleiste.
2. **Recherche, Aufnahme:** Pulsringe, neun Pegelbalken, roter Mikrofonkern und Timer.
3. **Recherche, Transkription:** Spinner, Whisper-Text und Shimmer über dem Eingabefeld.
   Vier Schutzschichten verhindern Whisper-Stillehalluzinationen funktionserhaltend:
   PCM-Sprachgehalt vor dem Upload, segmentweises Confidence-/Repetitions-Gate,
   Zeitfensterabgleich mit der Sprach-Timeline samt Drift-Sicherung und eine nur bei
   messbarem Stille-Kontext greifende exakte Floskel-Blocklist.
4. **Recherche, Formulierung:** animierter Loader; neue Formulierung kann rückgängig
   gemacht werden.
5. **Recherche, Generierung:** Während die Antwort erzeugt wird, zeigt eine animierte
   Eieruhr deutlich „Bitte warten, gründliche Recherche läuft“. Danach folgen die vollständige
   gegliederte Antwort, Karten-Loader, Erfolgskarte und Aktion „Jetzt lernen“.
   Der Vorlesen-Schalter ist ab dem ersten Streaming-Text verfügbar. Bleibt er orange
   aktiviert, startet TTS nach dem ersten vollständig abgeschlossenen Absatz. Weitere
   vollständige Überschrift-Absatz-Blöcke werden quellenbereinigt an eine offene
   Edge-TTS-Warteschlange angehängt und vorbereitet, ohne den laufenden Block
   abzubrechen. Ausschalten stoppt die gesamte Warteschlange sofort; ausgeschaltet ist
   das Symbol weiß und durchgestrichen.
6. **Zuordnungs-Bottom-Sheet:** passende Karten zu bestehenden Sessions kopieren oder
   ablehnen; jeder Eintrag zeigt seinen Ergebniszustand.
7. **Modell-Bottom-Sheet:** GPT 5.6 Sol/Terra/Luna und Reasoning Niedrig/Mittel/Hoch.
   Das Fenster fährt sichtbar von unten ein und wieder aus. Es lässt sich am oberen
   Griff mit einer Abwärtsbewegung schließen; Inhalt und Hintergrund folgen der
   Ziehbewegung ohne abrupten Sichtbarkeitswechsel.
8. **Profile:** Suchfeld und Sessionkarten mit Datum, Kartenanzahl, Fortschritt und
   Komplett-Status.
9. **Session-Detail, Karten:** Titel, Fortschritt, Alle lernen, Nur schwierige,
   Lernstand zurücksetzen, Kartenliste mit Status und Löschaktion.
10. **Session-Detail, Recherche:** vollständige gespeicherte Frage und Antwort samt
    typografisch getrennten Überschriften, Absätzen, anklickbaren Quellen und Vorlesen.
    Langes Drücken aktiviert die native Textauswahl; Auswahlgriffe können über Frage
    und Antwort bewegt und der gewählte Text über das Systemmenü kopiert werden.
    Das Drei-Punkte-Menü bietet „Lernstand zurücksetzen“ und „Lernsession löschen“.
    Nach einer Sicherheitsabfrage löscht die zweite Aktion Recherche, Karten,
    Lernfortschritt und Session gemeinsam in einer Datenbanktransaktion.
    Der Vorlesen-Schalter bleibt nach dem Einschalten orange, bis er erneut manuell
    gedrückt wird. Ausgeschaltet ist er weiß und durchgestrichen; ein erneuter Tipp
    stoppt laufendes TTS sofort.
11. **Lernrunde, Frage:** Fortschritt, Kartenindikatoren, Vorlesen, 3D-Karte und Flip.
12. **Lernrunde, Antwort:** Antwort plus Erklärung, Nicht gewusst und Gewusst.
    Der Erklärungskasten bleibt als visuelle Gliederung erhalten und besitzt rundum
    ausreichend Innenabstand, damit Kontur und Buchstaben sich nicht berühren.
13. **Lernrunde, Abschluss:** Konfetti, Ergebniszahlen, Nochmal und Fertig.
14. **Einstellungen:** GPT-Modell, Reasoning, Kartenlimit, Verbindungen,
    Erscheinungsbild, Farbprofil, Kartenschriftgröße, globale/einzelne Sounds,
    Lautstärke, TTS-Stimme, Stimmtest und Sprechgeschwindigkeit. Am Seitenende
    stehen die generierte App-Version und der genaue Build-Zeitstempel.
15. **OpenAI-OAuth-Dialog:** Browserähnlicher Sicherheitsdialog mit Fortfahren und
    Abbrechen; nach Erfolg zeigt die Verbindung Mailadresse und Trennen.
16. **Stimmen-Bottom-Sheet:** Ein Tipp auf die gewählte Edge-Stimme öffnet die
    Stimmenliste sichtbar von unten in derselben Optik wie die Modellauswahl. Das
    Fenster kann am oberen Griff nach unten gezogen und damit animiert geschlossen
    werden; die aktive Stimme ist mit Akzentrahmen und Haken markiert.

## 6. Funktionen und Daten

### Architektur

- Single-Module-App mit UI-, Domain- und Data-Paketen; keine unnötige frühe
  Multi-Modul-Komplexität.
- Unidirectional Data Flow über einen `AppViewModel` und immutable `UiState`.
- Room ist lokale Source of Truth für Sessions, Recherchen, Karten und Lernstatus.
- Neue Session, vollständige Recherche und alle erzeugten Karten werden unmittelbar
  nach Empfang gemeinsam in einer Room-Transaktion gespeichert. Die UI verzögert die
  Speicherung nicht durch eine künstliche Schreibanimation.
- DataStore speichert Theme, Profile, Modell, Reasoning, Kartenlimit, Audio- und
  TTS-Einstellungen.
- Groq- und Gemini-Schlüssel werden ausschließlich beim lokalen Build aus dem externen
  Secret-Store geladen und nicht im Repository abgelegt. Die Einstellungen unterscheiden
  ehrlich zwischen `Konfiguriert` und `Fehlt`; sie behaupten keine ungeprüfte Verbindung.
- Services kapseln Spracheingabe, KI-Generierung, OAuth/Schlüsselzugriff, TTS und Audio.

### Verhalten

- Spracheingabe fordert Mikrofonberechtigung kontextbezogen an und zeichnet PCM-WAV
  mit `16 kHz`, Mono und 16 Bit auf. Nur ein erneuter Mikrofon-Tipp beendet die
  Aufnahme; Sprechpausen stoppen sie nicht. Danach transkribiert Groq mit dem exakten
  Modell `whisper-large-v3-turbo`. Weitere Diktate werden an vorhandenen Text angehängt.
  Stop, Abbruch und ViewModel-Ende schließen Writer, Stream und `AudioRecord` genau einmal.
- Textverbesserung nutzt das lokal konfigurierte Gemini-Modell mit einer
  bedeutungstreuen Redaktionsanweisung: Intention, Satzbau, Wortstellung,
  Verständlichkeit und Stil werden aktiv verbessert, ohne Fakten hinzuzuerfinden.
  Das Original bleibt in der Versionshistorie erhalten und unterstützt Rückgängig.
- Absenden nutzt die Codex-Websuche und erzeugt eine gegliederte Antwort mit 1.500 bis
  5.000 Wörtern sowie automatisch 30 bis 70 Verständnis-Karteikarten. Antwort,
  Erklärungen und Beschreibungen verwenden einfaches Deutsch auf dem Niveau der
  10. Klasse; notwendige Fach- und Fremdwörter werden beim ersten Auftreten erklärt.
  Die Antwort nennt die tatsächlich verwendeten Webquellen und speichert alles in einer
  Session. Dasselbe GPT-Modell prüft für jede neue Karte den Titel, die letzte Frage und
  einen Antwortauszug aller bestehenden Sessions. Nur fachlich klar passende Karten
  werden über ihre gespeicherten IDs zur optionalen, transaktionalen Kopie angeboten;
  schwache Ähnlichkeiten erzeugen keinen Vorschlag.
- Session-Suche filtert Titel lokal; Karten können gelöscht und Lernstände
  zurückgesetzt werden.
- „Nur schwierige“ nimmt alle Karten außer Status `known`.
- Bei „Nicht gewusst“ wird die Karte ans Ende der aktuellen Queue gehängt; „Gewusst“
  schließt sie für die Runde ab. Die Abschlussansicht zeigt beide Zähler.
- Alle sichtbaren Einstellungen sind interaktiv und persistent.
- Das offizielle Modell heißt überall `GPT 5.6 Sol`. Der frühere falsche gespeicherte
  Anzeigename `GPT 5.6 Soul` wird beim Laden automatisch und verlustfrei migriert.
- Vorlesen verwendet den Microsoft-Edge-TTS-WebSocket aus Best Journal Android.
  Die sechs deutschen Neural-Stimmen werden mit ihren echten Microsoft-Voice-IDs
  in einem Dropdown angeboten; bestehende Anzeigenamen werden automatisch migriert.
  Ein Watchdog beendet hängende Streams, erneutes Vorlesen stoppt die vorige Ausgabe,
  und die Sprechgeschwindigkeit wird als begrenzte SSML-Prosodie übertragen. Lange
  Texte werden UTF-8-sicher aufgeteilt. Bei gegliederten Recherchen liest Edge TTS jede
  Überschrift zusammen mit ihrem Absatz als inhaltliche Einheit. Während eine Einheit
  läuft, wird die nächste in einer eindeutigen Cachedatei vorbereitet; zwischen
  inhaltlichen Einheiten liegt etwa eine Sekunde Pause. Quellen-URLs werden nicht
  vorgelesen. Stopp, Fehler und verspätete Netzwerk-Callbacks räumen alle temporären
  Dateien auf, ohne einen neueren Vorlesevorgang zu beeinflussen.
- Der Lautsprecher in der Lernrunde ist ein dauerhafter Vorlesemodus. Nach dem
  Einschalten bleibt er orange und liest die aktuelle Frage. Währenddessen werden
  Antwort und Erklärung bereits in eine eigene Edge-TTS-Cachedatei synthetisiert.
  Beim Umdrehen startet dieser vorbereitete Audioteil ohne erneuten Netzwerkwarteweg;
  die Frage wird nicht doppelt gelesen. Bei jeder nächsten Karte wird automatisch
  die neue Frage gelesen und deren Rückseite erneut im Hintergrund vorbereitet.
  Der Modus bleibt auch nach dem Ende einer einzelnen Audioausgabe aktiv. Nur ein
  erneuter manueller Tipp oder das Schließen der Lernrunde schaltet ihn aus und
  stoppt laufendes Audio; ausgeschaltet ist das Symbol weiß und durchgestrichen.
- Die Vorlesen-Aktionen der aktuellen Recherche und des Profil-Recherche-Details
  entfernen abschließende Quellenabschnitte vor dem TTS-Aufruf. Im Profil wird nur
  die Antwort vorgelesen, niemals die darüber angezeigte ursprüngliche Frage. Der
  zentrale TTS-Parser überspringt Quellen zusätzlich als zweite Schutzschicht.
- Das Antwortfeld enthält keine Quellenangaben zwischen Überschriften und Absätzen.
  Prompt und deterministischer Filter entfernen Inline-URLs, Markdown-Links,
  OpenAI-Zitatmarker, numerische Fußnoten und Quellenhinweise in Klammern. Alle Belege
  erscheinen ausschließlich ganz unten unter der unveränderlichen Überschrift
  `Quellen` als anklickbare Links. TTS endet vor dieser Überschrift und liest weder
  diese Überschrift noch die darunterliegenden Links vor.

### Datenmodell

- `StudySession(id, title, createdAt)`
- `Research(id, sessionId, question, answer, createdAt)`
- `Flashcard(id, sessionId, researchId, question, answer, explanation, status)`
- `LearningResult(id, flashcardId, known, reviewedAt)`
- Statuswerte: `NEW`, `KNOWN`, `UNKNOWN`.

## 7. Navigation

- Bottom-Navigation: `Recherche`, `Profile`, `Lernen`, `Mehr`.
- `Recherche` öffnet Modell-Sheet über den Modell-Chip, startet Aufnahme über das
  Mikrofon, generiert über Absenden, öffnet nach Abschluss das Zuordnungs-Sheet und
  über „Jetzt lernen“ die Lernrunde.
- `Profile` öffnet per Sessionkarte das Session-Detail. Dessen Tabs wechseln zwischen
  Karten und Recherche; Lernbuttons öffnen die passende Lernrunde.
- `Lernen` öffnet die Karten der zuletzt aktiven Session direkt als Lernrunde.
- `Mehr` zeigt Einstellungen; OpenAI-Anmelden öffnet den OAuth-Dialog.
- Overlays schließen per Zurück-/Schließen-Aktion; es gibt keine toten Buttons oder
  Sackgassen.

## 8. Festgelegte Integrationen

- Persönliche Einzelnutzer-App, nicht für Veröffentlichung oder API-Resale.
- OpenAI-Anmeldung über den Codex-Gerätecode-Flow mit der öffentlichen Codex-Client-ID.
  Die App fordert bei `/api/accounts/deviceauth/usercode` einen gruppierten Code an,
  zeigt ihn exakt und sichtbar an und öffnet `https://auth.openai.com/codex/device`.
- Die App pollt `/api/accounts/deviceauth/token` im vom Server vorgegebenen Intervall.
  `403/404` bleiben ausstehend, `429/5xx` werden fehlertolerant weitergepollt und
  `slow_down` erhöht das Intervall dauerhaft. Der Code läuft nach 15 Minuten ab.
- Nach der Bestätigung wartet der Token-Austausch, bis die App wieder im Vordergrund
  und ein validiertes Netz aktiv ist. Reine DNS-Fehler werden begrenzt mit Backoff
  wiederholt; OAuth- und sonstige HTTP-Fehler werden nicht blind erneut gesendet.
  `ACCESS_NETWORK_STATE` ist deklariert; kann der Zustand dennoch nicht gelesen werden,
  dient der reale HTTP-Aufruf als funktionserhaltender Konnektivitäts-Fallback.
- Tokens werden nicht aus `~/.codex` übernommen oder dorthin zurückgeschrieben.
- App-eigener, verschlüsselter Token-Store; Refresh-Antworten werden in bestehende
  Credentials gemergt. Rotierte Refresh-Tokens ersetzen nur das entsprechende Feld.
- Inferenz läuft über `https://chatgpt.com/backend-api/codex/responses` mit Bearer-Token,
  `originator: codex_cli_rs`, Codex-User-Agent und `ChatGPT-Account-ID` aus dem JWT.
  `input` ist eine Liste aus User-Items, `stream=true` und `store=false`. Der Body wird
  als echter SSE-Stream über einen geteilten OkHttp-Client verarbeitet und nicht mehr
  bis zum Ende gepuffert. Ein inkrementeller JSON-Decoder extrahiert das Top-Level-Feld
  `answer` escape- und Unicode-sicher; das erste Antwortstück erscheint sofort, weitere
  UI-Updates werden begrenzt. Nach dem Antwortfeld zeigt die UI die Kartenphase, während
  das Modell die Karten fertigstellt. Die Terminalmeldung `response.completed` ist
  Pflicht; ihr vollständiger Text hat Vorrang, andernfalls wird der bis dahin komplett
  zusammengesetzte Output validiert und transaktional gespeichert.
- Sol, Terra und Luna werden mit Niedrig, Mittel und Hoch deterministisch auf alle neun
  gültigen Modell-/Effort-Kombinationen abgebildet. Der Priority-Service-Tier reduziert
  Warteschlangenzeiten ohne Modell- oder Qualitätswechsel. Connect-, Idle- und
  Gesamt-Timeout sind getrennt; Reset bricht den zugrunde liegenden OkHttp-Call ab.
- Feste Nachlaufwartezeiten von insgesamt 2,4 Sekunden entfallen. `CodexPerf`
  protokolliert ohne Inhalte oder Tokens Headerzeit, erstes Antwortdelta, Ende des
  Antwortfelds und Gesamtzeit für reale Messungen.
- Fehler werden getrennt behandelt: `429` als Kontingentgrenze ohne Relogin-Schleife,
  `401/403/invalid_grant` als erneute Anmeldung und `refresh_token_reused` als eigener
  Konflikthinweis. Das Modell wird bei jedem Request explizit übergeben.
- Diese Codex-Impersonation ist absichtlich auf die bestätigte persönliche Nutzung
  begrenzt. Sie ist driftanfällig und kann durch OpenAI gesperrt oder widerrufen werden.
- Spracheingabe nutzt Groq `whisper-large-v3-turbo` mit lokal gebautem PCM-WAV.
- Vorlesen nutzt Microsoft Edge TTS mit den ausgewählten Neural-Stimmen.
- Schibsted Grotesk und Newsreader werden als lokale Font-Ressourcen gebündelt.
