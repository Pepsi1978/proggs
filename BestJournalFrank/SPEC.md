# Entropy Journal - Design-Differenz-Spezifikation

## Grundlage

- Zielentwurf: `Designs/Best Journal Frank-App Design-Überarbeitung/Goldener Faden Prototyp.dc.html`
- Bestehende App: `BestJournalFrank`
- Umsetzungsfall: B - bestehende App mit neuen Designbereichen
- Maßgeblich ist der klickbare Prototyp "Goldener Faden". `Aktuelle UI.dc.html` beschreibt nur den vorherigen Ist-Zustand; `Redesign.dc.html` dokumentiert die verworfenen Richtungen und die Theme-Herkunft.

## Neue Bereiche

### Unabhängige Schriftwahl

- Überschriften: Playfair Display, Great Vibes, Caveat, Lora, Sora, Space Grotesk, Nunito.
- Fließtext: Source Sans 3, Manrope, IBM Plex Sans, Nunito Sans, Lora, Caveat.
- Standard: Playfair Display und Source Sans 3.
- Beide Listen sind unabhängig und ergeben 42 Kombinationen.
- Es ist immer nur eine Liste aufgeklappt.
- Eine Auswahl wird sofort, app-weit und persistent wirksam.
- Bestehende gespeicherte Schriftpaare werden einmalig in Überschriften- und Fließtextwahl überführt.

### Zentrales Teilen-Unterfenster

- Modal Bottom Sheet mit Radius 26 dp oben, 20 dp horizontalem Innenabstand und 28 dp unterem Innenabstand.
- Kopfzeile: Teilen-Icon, Titel `Teilen`, kontextbezogener Untertitel und Schließen-Aktion.
- Goldene animierte Haarlinie.
- Formate: `Als Text`, `Als Bild`, `Als PDF`; aktives Format als goldene Pill.
- Ziele: WhatsApp, E-Mail, Speichern, Kopieren, Mehr.
- Das Unterfenster wird aus Eintrag-Detail, Dashboard und Rückblick geöffnet.
- Bestehende Auswahl von Eintrag, Nachträgen und Medien beziehungsweise Rückblick-Medien bleibt erhalten.
- Bild und PDF werden aus dem ausgewählten Text erzeugt; Speichern nutzt den Android-Dokumentdialog.
- WhatsApp und E-Mail werden gezielt geöffnet, `Mehr` nutzt den System-Chooser, Kopieren die Zwischenablage.

## Design-Tokens

- Hintergrund dunkel: radial `#1E1712 -> #16110D -> #100C09`.
- Karte dunkel: `#221A13`, Rand `rgba(232,181,71,.28)`.
- Akzent: `#E8B547`, Sekundärakzent `#DF741E`, Kupfer `#FFB689`.
- Haupttext dunkel: `#EFE7DB`, Sekundärtext `#D8CCBA`, gedämpft `#9C8D77`.
- Share-Sheet-Scrim: `rgba(10,7,4,.55)`.
- Format-Pills: Radius 999 dp, Höhe durch 9 dp vertikalen Innenabstand.
- Zielkreise: 52 dp, Icons 24 dp, Labels 11 sp.
- Karten 18 dp, Dialoge 22 dp, Bottom Sheet oben 26 dp.

## Effekte

- Goldene Haarlinie: 5 Sekunden, linear, unendlich.
- Fünf Goldstaubpunkte: 3,6 bis 5 Sekunden, Ease-in-out, phasenversetzt.
- Bestehende Effekte bleiben erhalten: atmender Login- und Mikrofonbutton, rotierender Aufnahmering, Waveform, Hero-Zoom, Glows und Bottom-Navigation.

## Funktionserhaltung

- Keine vorhandene Tagebuch-, Dashboard-, Rückblick-, Medien-, TTS-, Backup- oder Einstellungsfunktion wird entfernt.
- Teilen berücksichtigt weiterhin die vom Nutzer ausgewählten Inhalte und Medien.
- Original/Verbessert im Eintragsdetail bestimmt die tatsächlich geteilte Textfassung.
- Foto-, Video- und gemischte Anhänge erhalten einen passenden MIME-Typ und URI-Berechtigungen.

## Navigation

- Eintrag-Detail -> Teilen-Unterfenster.
- Dashboard-Überblick -> Teilen-Unterfenster.
- Rückblick-Zusammenfassung -> Teilen-Unterfenster.
- Schließen, erfolgreicher Versand, Speichern und Kopieren schließen das Unterfenster.

## Begleitdateien

- `assets/splash_hero_book.png` ist bereits bytegleich als `app/src/main/res/drawable/splash_hero_book.png` eingebunden.
- Die Referenzbilder und `.thumbnail` sind nur Abgleichsquellen und keine Laufzeitressourcen.
- Das Design enthält keine Audio-Begleitdateien.
