# StackLabor Werft Studio - Bau-SPEC

Stand: 14.08.2026

## 1. Projektgrenze

- Projektordner: `C:\Users\barwa\proggs\StackLaborWerftStudio`
- Gradle-Projektname: `StackLaborWerftStudio`
- Android-Paket und Application-ID: `de.frank.stacklabor.werftstudio`
- App-Name: `StackLabor Werft Studio`
- Das parallele Projekt `C:\Users\barwa\proggs\StackLabor` wird weder verändert noch als Ziel verwendet.
- Verbindliche Designquelle ist ausschließlich
  `C:\Users\barwa\proggs\Designs\Outbox\StackLabor-SPEC-v1-SPEC-v2`.
- Die Vorführbühne des Werft-Prototyps mit Bildschirm- und Theme-Umschaltern gehört nicht zur App.

## 2. Ziel und Plattform

StackLabor Werft Studio verwaltet Nahrungsergänzungsmittel-Stacks, Ziele und eigene Fragen.
Codex erzeugt eine gespeicherte, dünn besetzte Bewertungstabelle. Alle Ampeln werden daraus
lokal berechnet, sodass Aktivieren, Deaktivieren und Priorisieren offline und sofort wirken.

- Android, Kotlin, Jetpack Compose und Material 3
- `compileSdk` und `targetSdk` 36, `minSdk` 26, Java 17
- Leitgröße geschlossenes Galaxy Z Fold 8: 297 x 469 dp, nutzbar etwa 297 x 421 dp
- Aufgeklapptes Galaxy Z Fold 8: etwa 440 x 583 dp, zweispaltig
- Deutsch, einsprachig, echte Umlaute
- Hell ist Standard; Dunkel ist vollständig gleichwertig
- Kein Einnahme-Tracker, kein Kalender, keine Cloud-Sicherung, kein Mehrbenutzerbetrieb

## 3. Verbindliche Quellen

| Priorität | Quelle | Verbindlich für |
|---|---|---|
| 1 | `WERFT-DESIGN/bildschirme/21hell/*.html` und `22dunkel/*.html` | Aufbau, sichtbare Inhalte und Zustände |
| 2 | `WERFT-DESIGN/bildschirme/design.css` | Exakte Maße, Farben, Schatten, Verläufe, Blur und Selector-Motion |
| 3 | `WERFT-DESIGN/bauplan/<theme>/*.json` | Elementinventar und gemessene Geometrie |
| 4 | `01-FUNKTIONS-SPEC.md` | Verhalten, Datenmodell, Fehlerfälle und Geschäftsregeln |
| 5 | `02-UI-SPEC.md` | Bildschirm-, Theme-, Typografie- und Navigationsinventar |
| 6 | `03-MOTION-SPEC.md` | Dauer, Verzögerung, Easing und Wiederholung jeder Bewegung |
| 7 | `STARTBESTAND.md` | Inhalt der zu erzeugenden Asset-Datei `startbestand.json` |

Bei Widersprüchen gewinnt für das Aussehen die konkrete HTML/CSS-Quelle und für das Verhalten
die Funktions-Spec. Veraltete Gold-Metadaten aus `design.html` werden ignoriert, weil sämtliche
Bildschirmdateien, Baupläne und Design-Tokens die Indigo/Cyan-Themes verwenden.

## 4. Design-Tokens

### 4.1 Hell `21hell`

| Rolle | Wert |
|---|---|
| Grund | `#F5F7FA` |
| Karte | `#FFFFFF` |
| Erhöhte Fläche | `#F1F5F9` |
| Rand | `#E2E8F0` |
| Text stark | `#0F172A` |
| Text schwach | `#64748B` |
| Akzent | `#4F46E5` |
| Ampel grün | `#047857` |
| Ampel gelb | `#D97706` |
| Ampel gelber Text | `#B45309` |
| Ampel rot | `#DC2626` |
| Ampel rot kräftig | `#B91C1C` |
| Ampel grau | `#94A3B8` |
| Wasserlöslich | `#059669` |
| Fettlöslich | `#FFFFFF`, Rand `#64748B`, 1,5 dp |
| Deaktiviert | `#CBD5E1` |

### 4.2 Dunkel `22dunkel`

| Rolle | Wert |
|---|---|
| Grund | `#0B0E14` |
| Karte | `#141A24` |
| Erhöhte Fläche | `#1B2330` |
| Rand | `#243040` |
| Text stark | `#E6EAF2` |
| Text schwach | `#9AA6B8` |
| Akzent | `#22D3EE` |
| Ampel grün | `#34D399` |
| Ampel gelb | `#FBBF24` |
| Ampel rot | `#F87171` |
| Ampel grau | `#64748B` |
| Wasserlöslich | `#34D399` |
| Fettlöslich | `#FFFFFF`, ohne Rand |
| Deaktiviert | `#334155` |

### 4.3 Typografie

Die mit dem Design verlangte Schrift ist Inter. Sie wird als lokale Variable-Font-Ressource
eingebunden, nicht zur Laufzeit geladen.

| Rolle | Größe | Gewicht | Zeilenhöhe | Laufweite |
|---|---:|---:|---:|---:|
| Bildschirmtitel B-01 | 22 sp | 600 | 28 sp | 0,2 sp |
| Bildschirmkopf | 17 sp | 600 | 22 sp | 0 sp |
| Stack-Name | 16 sp | 600 | 20 sp | 0 sp |
| Mittelname | 15 sp | 500 | 20 sp | 0 sp |
| Auswertungs-Fließtext | 15 sp | 400 | 22 sp | 0 sp |
| Zieltext | 14 sp | 500 | 18 sp | 0 sp |
| Dosis und Frequenz | 12 sp | 400 | 16 sp | 0,1 sp |
| Zielnummer | 11 sp | 600 | CSS/Bauplan | 0 sp |
| Gerätecode | 40 sp | 600 | 48 sp | 2 sp |

### 4.4 Formen und Größen

- Kartenradius: 12 dp
- Bottom-Sheet-Radius: 20 dp an den oberen Ecken
- Chips: vollrund
- Ampel-Kantenbalken: 2 dp links
- Löslichkeitspunkt: 8 dp Durchmesser
- Zielnummernkreis: 20 dp Durchmesser
- Mindesttippfläche: 44 x 44 dp
- Alle weiteren Paddings, Gaps, Größen und Offsets werden selectorweise unverändert aus
  `design.css` und den Bauplan-JSONs übernommen. CSS-Pixel aus `style` gelten direkt als dp/sp.

### 4.5 Tiefe und Spezialeffekte

- Animierte lineare Kopfverläufe
- Rotierende und wandernde Randverläufe
- Kartenglanz und Schimmer
- Statusabhängige Auren und farbige Schatten
- Glaskopfzeilen, feste Sockel und Bottom Sheets mit bis zu 24 dp Blur
- Kein Blur auf scrollenden Listen
- Pulsierende Ampeln, Ladeampeln und FABs
- Skelettflächen mit laufendem Schimmer
- Theme-Farbüberblendung über alle offenen Flächen einschließlich Sheets
- Material Symbols Rounded oder exakt entsprechende Compose-Symbole

## 5. Motion

Die 283 maschinell inventarisierten CSS-Einträge aus `03-MOTION-SPEC.md` werden nicht als
283 unabhängige Effekte interpretiert, sondern selectorgetreu auf folgende gemeinsamen
Compose-Spezifikationen abgebildet. Jeder dort genannte Selector bleibt im jeweiligen Screen
nachweisbar berücksichtigt.

| Familie | Dauer | Easing | Wiederholung |
|---|---:|---|---|
| Theme/Farbe/Rand/Schatten | 420 ms | `cubic-bezier(0.4,0,0.2,1)` | einmal |
| Kurzer Zustandswechsel | 180 ms | `cubic-bezier(0.2,0,0,1)` | einmal |
| Standard-Interaktion | 220 ms | `cubic-bezier(0.2,0,0,1)` | einmal |
| Drückzustand | 120 ms | `cubic-bezier(0.2,0,0,1)` | einmal |
| Sheet und Scrim | 300 ms | `cubic-bezier(0.05,0.7,0.1,1)` | einmal |
| Auf-/Zuklappen | 200 bis 220 ms | `cubic-bezier(0.2,0,0,1)` | einmal |
| Kopfverlauf | 30.000 ms | linear | endlos |
| Kartenglanz | 8.000 ms | linear; rote Variante ease-in-out mit 2.400 ms Verzögerung | endlos |
| FAB-Atmen | 3.200 ms | ease-in-out | endlos |
| Rote Aura | 2.400 ms | ease-in-out | endlos |
| Auswertungs-/Ladepuls | 1.600 ms | `cubic-bezier(0.4,0,0.6,1)` | endlos |
| Skelettschimmer | 1.400 ms | linear | endlos |

Zusätzlich funktional umzusetzen:

- Aufnahme eines Ziehelements nach 300 ms
- Ausweichen der anderen Einträge, live mitlaufende Nummern und automatisches Randscrollen
- Einrasten beim Loslassen und Rückflug beim Abbruch
- gestaffelte Ampelüberblendung; nur tatsächlich geänderte Ampeln pulsieren einmal
- wortweiser Aufbau der sichtbaren Auswertung während des Streamings
- Absatzhervorhebung und drei Pegelbalken beim Vorlesen
- Wischentfernung mit sechs Sekunden Undo
- Bei reduzierter Bewegung entfallen Endlosschmuck, Auren, Schimmer und FAB-Atmen;
  funktionale Zustandswechsel, Ampelüberblendung und Drag-Ausweichen bleiben aktiv.

## 6. Bildschirm- und Navigationsinventar

1. B-01 Hauptbildschirm: Theme, Frei/Dienst, alle Stacks, Ziel-Katalog, Einstellungen,
   Gesamtauswertung, Mittel-Katalog und Stack anlegen.
2. B-02 Stack-Detail: Ziele, Sortierung, Suche, Zustandskarten, Mittelliste, Hinweisschnipsel,
   Auswertung, Untermenü, eigene Fragen und Historie.
3. B-03 Ziel-Katalog: Suche, Anlegen, Umbenennen, Löschen und Verwendungszahl.
4. B-04 Ziele dieses Stacks: Auswahl, Begründungen, Ampeln, Aufschlüsselung, Ordnen und Fertig.
5. B-05 Mittel bearbeiten: globale Stammdaten und stackbezogene Dosis-/Frequenzdaten.
6. B-06 Aufschlüsselung: Mittel-zu-Zielen oder Ziel-zu-Mitteln, optional neutrale Paare.
7. B-07 Auswertung im Vollbild: Metadaten, Markdown-Fließtext, Vorlesen/Pause/Stopp.
8. B-08 Eigene Fragen: Anlegen, Ändern, Löschen, Wisch-Undo.
9. B-09 Alle Stacks zusammen: Tagesgesamtdosen, stackübergreifende Konkurrenzen, Auswertung.
10. B-10 Einstellungen: TTS, Codex, Daten, Darstellung, Importwarnung und Versionsanzeige.
11. B-11 Codex-Anmeldung: Gerätecode, Browser öffnen, Polling, Ablauf und Fehlerzustände.
12. B-12 Ziele ordnen: nummerierte Prioritätsliste, Drag & Drop und Begründungen.
13. B-13 Stack bearbeiten: Name, Zeitpunkt, Einnahmehinweis, Sichern und Löschen-Dialog.
14. B-14 Mittel-Katalog: Suche, Anlegen, Bearbeiten, Löschen, Zusammenführen, Auswahl und Undo.
15. B-15 Auswertungs-Historie: letzte fünf Läufe, Öffnen, Zweifachauswahl und Vergleich.

Navigation verwendet typisierte Routen und ausschließlich IDs. B-05, B-07, B-11 und B-14
erhalten einen Aufrufkontext, damit Zurück immer zum tatsächlichen Ursprung führt. Bottom Sheets
liegen über dem echten vorherigen Screen; die im HTML enthaltene Unterlage wird nicht dupliziert.

## 7. Funktionsumfang

Vollständig umzusetzen sind F-01 bis F-24 und F-27 bis F-72, soweit die Elemente Teil des
Werft-Designs sind. Die als „offen“ exportierten Eingabeelemente erhalten die bereits in der
Funktions-Spec festgelegte Aufgabe:

- F-62 ist der interne System-Dateiauswahlauslöser für F-20.
- F-63 bis F-65 bearbeiten Name, Zeitpunkt und Einnahmehinweis des Stacks.
- F-66 schließt das Ziel-Sheet über Scrim/Zurück.
- F-67 bis F-71 wählen das jeweilige Ziel für den Stack an oder ab.
- F-72 aktiviert und bearbeitet die zweite Dosisvariante.
- Durchfallrisiko, Kombi-Gruppe und „Auch neutrale zeigen“ werden entsprechend F-03, F-15 und
  F-28 funktional verdrahtet.

F-25 „Mittel verschieben/kopieren“ und F-26 „Stack duplizieren“ bleiben gemäß Quellspec spätere
Funktionen und werden nicht als sichtbare tote Bedienelemente eingebaut.

## 8. Datenmodell und lokales Backend

Architektur: Single-Module-App mit UI-, Data- und Service-Schicht, UDF, immutablem UI-State,
`StateFlow`, `collectAsStateWithLifecycle()` und einem schlanken `AppContainer`.

Room ist die lokale Single Source of Truth. Tabellen:

- `mittel`
- `wirkstoffkomponente`
- `stack`
- `stack_eintrag`
- `stack_eintrag_dosis`
- `alternations_partner`
- `ziel`
- `stack_ziel`
- `eigene_frage`
- `bewertung`
- `bewertungszelle`
- `konkurrenz`
- `frage_antwort`
- `stack_sortieransicht`

DataStore speichert Darstellung, Dosisvariante, TTS-Anbieter, Stimme, Tempo, Absatzpause,
Abschaltzeit, TTS-Verbrauch, Codex-Modell, Denkstufe und reduzierte Bewegung. OAuth-Tokens werden
verschlüsselt und niemals exportiert.

Das gegenüber dem Quellmodell normalisierte Dosis-Modell erlaubt:

- optionale Menge für Mittel ohne numerische Dosis
- Einheiten mg, µg, g, ml, IE, Stück und Tasse
- Darreichungsformen Kapsel, Tablette, Löffel, Tasse, Pulver, Tropfen und Sonstige
- mehrere Wirkstoffkomponenten pro Mittel, etwa D3 plus K2 oder Eisen plus Vitamin C
- mehrere Alternationspartner für den Dreierzyklus
- getrennte Dosisvarianten Frei und Dienst

`startbestand.json` wird aus `STARTBESTAND.md` erzeugt und enthält alle 6 Stacks, 72 Einträge
und 63 Mittel. Es wird beim ersten Start transaktional importiert und bleibt später manuell
erneut einlesbar.

Room exportiert Schemas. Jede spätere Schemaänderung benötigt eine Migration; es gibt keinen
destruktiven Produktions-Fallback. Export und Import verwenden ein versioniertes JSON-Format.
Vor Import wird eine lokale Rückfallsicherung geschrieben. Import läuft vollständig in einer
Transaktion und unterstützt Ersetzen oder Zusammenführen.

## 9. Ampel- und Prüfsummenlogik

- Zielgewicht: Rang 1 bis 3 = 3, Rang 4 bis 7 = 2, ab Rang 8 = 1.
- Mittel: Rot bei maximal `gewicht * staerke >= 6`, Gelb ab 2, sonst Grün, wenn keine Störung.
- Ziel: `S = Summe(stützend) - Summe(störend)`.
- Ziel Rot bei Störung Stärke 3 oder `S <= -1`.
- Ziel Gelb bei `S` von 0 bis 2.
- Ziel Grün bei `S >= 3` und keiner Störung ab Stärke 2.
- Ziel Grau, wenn kein aktives Mittel stützt.
- Stack-Sammelampel ist die schlechteste Zielampel; Grau zählt dabei wie Gelb.
- Häkchen und Zielrang rechnen lokal neu und machen eine Bewertung nicht veraltet.
- Inhaltsänderungen gemäß F-23 ändern die Prüfsumme und markieren den Stand als veraltet,
  ohne alte Ampeln zu entfernen.

## 10. Netzwerk-Backend und Codex

Es wird kein eigener Server benötigt. Die App arbeitet offline-first und spricht nur die in der
Quellspec benannten externen Dienste an.

- Codex-OAuth-Geräteflow über die OpenAI-Endpunkte
- verschlüsselte Tokenablage, Refresh und Abmelden
- Codex Responses über `chatgpt.com/backend-api/codex/responses`
- Modelle Sol, Terra und Luna; Standard `gpt-5.6-terra`
- Denkstufen low, medium, high, xhigh und max; Standard high
- dünn besetztes, strikt validiertes JSON gemäß F-12
- maximal zwölf Ziele je Teilanfrage; Ergebnisse werden lokal vereinigt
- einmaliger automatischer Netz-Retry nach drei Sekunden
- JSON-Reparaturversuch; bei erneutem Fehler bleiben Ampeln grau und der Text erhalten
- Fehlerklassen REAUTH, QUOTA und NETWORK mit den im Design vorhandenen Aktionen
- laufende Anfrage überlebt Backgrounding innerhalb des Prozesses; explizites Abbrechen ist möglich
- Konkurrenzprüfung neuer Mittel wird nach drei Sekunden Ruhefenster gebündelt und persistent
  als offener Hinweis gespeichert.

„Später erneut“ plant eine eindeutige `OneTimeWorkRequest` mit Netzbedingung. Sie versucht die
betroffene Auswertung beim nächsten verfügbaren Netz einmal erneut, sofern die Codex-Anmeldung
gültig ist; Doppelplanung wird verhindert.

## 11. TTS

- Microsoft Edge TTS als Standard, `de-DE-SeraphinaMultilingualNeural`
- Google Cloud TTS / Chirp 3 HD
- Qwen-Stimmklon
- Anbieter- und Stimmenkatalog, Tempo, Absatzpause, Abschaltzeit und Verbrauch
- Media3-Wiedergabe mit Pause, Fortsetzen und Stopp
- Vordergrunddienst mit Medienbenachrichtigung und Stopp-Aktion
- nur Fließtext, absatzweise; der gesprochene Absatz wird markiert
- kein Netz oder fehlender Schlüssel deaktiviert die jeweilige Wiedergabe mit Klartext

Zugangsdaten werden ausschließlich aus `C:\Users\barwa\SK` übernommen und nie ins Repository
geschrieben.

## 12. Fold- und Fensterlayout

- Bis 399 dp Breite: exakte einspaltige Werft-Anordnung.
- Ab 400 dp Breite: zweispaltige Darstellung, damit das spezifizierte Fold-Innendisplay mit
  etwa 440 dp die breite Fassung erhält.
- Listen-/Detailkombinationen verwenden links etwa 42 Prozent und rechts 58 Prozent.
- B-01 zeigt Stack-Liste links und ausgewählte/gesamte Auswertung rechts.
- B-02 zeigt Mittel und Werkzeuge links, Ziel-/Auswertungsdetails rechts.
- Kataloge zeigen Liste links und Auswahl/Bearbeitung rechts.
- Reine Text- und Einstellungsseiten bleiben in einer zentrierten Spalte mit designgetreuer
  Maximalbreite, sofern kein sinnvoller zweiter Bereich vorhanden ist.
- Scrollposition, ausgewählte IDs, offene Sheets und laufende Vorgänge bleiben beim Falten erhalten.

Da das Werft-Paket keine breite Bildschirmvorlage enthält, ist dies die minimale funktionale
Übertragung der ausdrücklich geforderten Zweispaltigkeit; Farben, Komponenten und Effekte bleiben
unverändert aus dem jeweiligen Werft-Screen.

## 13. Tests und Abnahme

- Unit-Tests für Ampelgrenzen, Prüfsummen, Frequenzrechnung, Dosisvarianten und Import-Migration
- Room-Tests für Seed-Import, Kaskaden, Zusammenführen und Historienbegrenzung
- Repository-Tests für alle CRUD- und Reorder-Aktionen
- API-Tests mit MockWebServer für Streaming, Teilanfragen, Retry und Fehlerklassen
- Compose-Tests für jeden B-Screen und jede Navigation
- Tests für Hell, Dunkel, reduzierte Bewegung, geschlossen und aufgeklappt
- Visueller Screenshot-Abgleich jedes Screens gegen beide HTML-Varianten
- Prüfung sämtlicher F-Kennungen auf eine sichtbare Wirkung; kein toter Knopf
- Build von Debug und Release
- Installation und Funktionsprüfung auf dem Fold-8-Emulator beziehungsweise verbundenen Gerät

## 14. Offene Bestätigung

Vor Beginn des Codes sind folgende aus den fehlenden oder widersprüchlichen Quelldetails
abgeleiteten Festlegungen zu bestätigen:

1. Das normalisierte Mehrkomponenten-Dosis-Modell ersetzt die zu enge Ursprungstabelle, ohne
   sichtbare Designänderung.
2. Wiederkehrende Einnahmen `alle N Tage` werden in der Tagesgesamtdosis als Durchschnitt
   `Dosis / N` ausgewiesen; zusätzlich bleibt die Originalfrequenz sichtbar.
3. „Später erneut“ nutzt eine einmalige, netzgebundene WorkManager-Aufgabe.
4. Das Fold-Innendisplay verwendet ab 400 dp die oben beschriebene funktionale Zweispaltigkeit,
   da der Entwurf nur die einspaltige 412-x-915-Vorlage enthält.
5. Der sichtbare Streamingfortschritt besteht aus Fortschrittserzählung und wachsendem
   Fließtextbereich; das endgültige, erst vollständig parsebare JSON wird danach gespeichert.
