# Android-Scrolloptimierung, 07.09.2026

## Umfang und Ergebnis

Installierte eigene Android-Pakete per ADB dem lokalen Quellcode zugeordnet. BestJournalAndroid einschließlich seiner Debug-Installation bleibt ausgeschlossen. BestJournalFrank wurde nach ausdrücklicher Ergänzung des Nutzers aufgenommen.

13 Apps wurden gezielt im UI-/Rendering-Code optimiert, versioniert, erfolgreich gebaut und per `adb -s R3GL7073MLM install -r` auf Samsung SM_F971B aktualisiert. Keine Deinstallation, kein Clear und keine Datenmigration. BestJournalFrank wurde anschließend gestartet. Die anderen Apps wurden nicht für einen Laufzeittest geöffnet.

Die Befunde sind statisch hergeleitet. Im Schnellmodus wurden keine Funktions-/UI-Tests, Screenshotvergleiche oder Performance-Messungen ausgeführt. Der erfolgreiche Build und die Installation belegen weder Ruckelfreiheit noch vollständige Funktions- oder Pixelgleichheit. AGP führte bei Entropies Performance-Build automatisch LintVital aus.

## Installierte Updates

| Projekt | Version | Build | Wesentliche Änderungen |
|---|---|---|---|
| BestJournalFrank | 0.23.3 | Debug | Mikrofon, Schimmer, Trennlinien, Splash und Vorhang entlastet; Theme-/Tag-Caches; Such-Flow stabilisiert |
| ClaudeKompass | 0.4.9 | Debug | Suchgruppen und Stimmenauswahl gecacht; Nachrichten-Flow nur bei Sitzungswechsel neu abonniert |
| CodexKompass | 0.4.9 | Debug | Suchgruppen und Stimmenauswahl gecacht; Suchlisten-Recycling typisiert |
| CortexAndroid | 0.10.13 | Debug | VPN-Puls-Reads in Draw/Layer; Dashboard-Elementtypen getrennt |
| EntropieReductor | 0.29.3 | Benchmark/Performance | Mikrofon, Briefing, Markdown, vier Verlaufskarten, Trainings-/Journal-/Settingslisten, Triggerdialog und Diagnosezeitstempel entlastet |
| Experimente | 0.17.2 | Debug | Puls/Swipe/Denkanzeige entlastet; Auswertungstext und Theme gecacht; Vorschlagsindex direkt; Glasschatten-Zeichenobjekte gecacht |
| FisetinBegleiter | 1.0.28 | Debug | Hintergrund, Buttons und Schalter entlastet; Zutaten-/Stackgruppen, Theme und Listen-Recycling optimiert |
| Gedankenspeicher | 0.6.4 | Debug | Gezielte Theme-Invalidierung, Animationen, Suche, Profile und Bibliotheksgruppen entlastet |
| GenialeIdeen | 1.5.13 | Schnell/R8 | Gemeinsame Animationen/Schatten, Theme, Stimmen-/Logfilter und Kategorie-/Reorderaufbereitung entlastet |
| KarteikartenLernen | 0.1.41 | Debug | Aurora, Mikrofon, Lernkartenflip und Abschlussanimationen entlastet; Artikel-/Profil-/Theme-Caches |
| PerfectMoment | 0.1.113 | Debug | Theme-Farbschema gecacht; Sessiondimmung nur noch beim Schwellenwechsel in Composition |
| StackLaborWerftStudio | 0.3.29 | Debug | Gemeinsame Schatten/Brushes/Themes, Gradient-Header, Karten-Aura, FAB, Sheets und Drag-Index entlastet; feste Regexe wiederverwendet |
| VoiceKey | 0.7.2 | Debug | Orb-State erst im Layer gelesen, Sprachlisten gecacht; sichtbarer Versionszeitstempel ergänzt |

Versionszeit für diesen Commit live mit `Get-Date -Format "dd.MM.yyyy HH:mm"` ermittelt: **07.09.2026, 14:18 Uhr**. Jede App erhielt genau einen Patch-Bump und einen erhöhten versionCode.

## Abdeckung und Grenzen

Statische Analyse nach App getrennt über Hauptseiten, Unterseiten, Einstellungen, Such-/Auswahlbereiche, Dialoge/Sheets, Themes, Animationen und gemeinsame Komponenten. Große Screens wurden teilweise anhand ihrer Struktur und relevanter Codeabschnitte analysiert; dies ist kein vollständiger Zeile-für-Zeile-Audit aller Projekte. Geänderte gemeinsame Komponenten wirken auch in unveränderten Screens, die sie verwenden.

- **QwenTtsBench:** vollständige kleine Activity analysiert. Inferenz bereits außerhalb des UI-Threads, klassische ScrollView statt Compose. Kein ausreichend begründeter neutraler Änderungskandidat; unverändert, nicht neu installiert.
- **TerminalVoiceOverlay für Android** (`com.pepsi1978.terminalvoiceoverlay`): installiert, aber kein passender Android-Quellcode im Workspace gefunden. Desktopprojekte gleichen Namens sind kein Ersatz; unverändert.
- **NEMS:** nicht auf dem angeschlossenen Handy installiert; außerhalb des Auftrags.
- **BestJournalAndroid:** ausdrücklich ausgeschlossen, weder Quellcode geändert noch installiert.
- Externe Material-/Maps-/WebView-Implementierungen, Homescreen-Widgets und sämtliche Backend-/Audio-/Sync-Pipelines wurden nicht vollständig auditiert.

## Bewusst nicht umgebaut

- Große eager Einstellungs-/Editor-/Dialogbäume: Ein Lazy-Wechsel kann lokale Entwürfe, Fokus, Effekte und Medienlebensdauer verändern. Keine pauschale Konvertierung vorgenommen.
- Animationen wurden nicht entfernt, verlangsamt oder mit neuen Laufzeiten versehen. Unsichtbar weiterlaufende Phasen nicht pauschal neu gestartet. Bestehende Effekt-/Bewegungsoptionen bleiben erhalten.
- Keine Dependency-Upgrades, keine pauschalen Stabilitätsannotation-Verträge, keine Änderungen an Datenformaten, API-Schlüsseln oder Berechtigungen.
- Streaming-Autoscroll und Mikrofon-/Vorlesezustand bleiben teilweise breit gekoppelt; Drosselung könnte Anzeigezeitpunkte verändern.
- Gesundheitsspezifische Berechnungs-/Zeitfenster-Semantik bleibt unverändert; zeitabhängige Werte nicht allein nach einer Liste gecacht.

## Weitere konkrete Ansatzpunkte

Diese Funde wurden nicht als Teil der ausgelieferten Änderungen umgesetzt:

- Cortex: Tippindikator/Leerlaufanimation und Prompt-Titelaufbereitung in `ChatScreen.kt`.
- Gedankenspeicher: Bildzoom-Reads in `Anhaenge.kt`, Karten-Vorleseindex, Anhangs-JSON im Menü, stabiler Notizzahl-Flow sowie Nachtragszeit-Parsing bei Sortierungen.
- GenialeIdeen: Suchhervorhebung und Detail-Streaming-/Denkpunkte; animierter Schrift-Brush der Kopfleiste bleibt compositiongetrieben.
- Kompass-Apps: Sitzungsabbildung und weitere heterogene Chat-/Referenzlisten; Claude-Referenzaufbereitung bei Bedienzustandswechseln.
- Experimente: weitere `indexOf`-Schleifen in `Listen.kt`, Animationsreads in großen Komponenten/Settings; Sensorzustand erst nach expliziter reaktiver Aufgabenanbindung aus Composition lösen.
- PerfectMoment: Vorschautext-Aufbereitung und weitere gemischte Detail-Listen.
- Fisetin: Hero-Puls in `TodayAndTimelineScreens.kt`.
- Entropie: Trainingschart-Statistiken/Zeichenpfad, Oura-Detailaufbereitung, Gewohnheiten-Rangberechnung, Timeline-/Detailtexte und Pointer-/Drag-State-Trennung.
- StackLabor: Katalogfilter, Arbeitsband, Sheet-Aufbereitung, gruppierte Stack-Auswertungen und breiter globaler Datenfluss.
- BestJournalFrank: Dashboard-Ratschlagssortierung, Eintragsdetail-Aufbereitung und weitere animierte Hintergründe; Rückblick-Gruppierung nur mit beachteter Systemzeitzone cachen.

Für eine belegte Aussage zur Gesamtperformance wären als nächster getrennter Schritt reproduzierbare Scroll-Journeys in passenden Release-/Performance-Builds nötig. Diese wurden in diesem Schnellmodus-Lauf nicht ausgeführt.
