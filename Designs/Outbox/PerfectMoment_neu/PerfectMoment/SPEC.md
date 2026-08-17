# Perfect Moment - Implementierungs-SPEC

Stand: 19. Juli 2026

## 1. Verbindliche Quellen

1. Sichtbares Design: `../Designs/Perfect Moment Design-Brief/Perfect Moment.dc.html`.
2. Funktionsumfang: `02-PROGRAMMIER-SPEC.md`.
3. Hintergrund: `01-DESIGN-BRIEF.md`.

Bei sichtbaren Widersprüchen gewinnt das HTML. Nicht funktionsfähige Demonstrations-
Aktionen des HTML werden nach `02-PROGRAMMIER-SPEC.md` vollständig implementiert.

## 2. Design-Tokens

| Rolle | Dunkel | Hell |
|---|---|---|
| Hintergrund | `#181209` | `#FBF6EC` |
| Fläche | `#251C10` | `#F3EAD9` |
| Fläche erhöht | `#332717` | `#EDE1CA` |
| Gold | `#D4A24C` | `#A87A2A` |
| Gold hell | `#F0C97A` | `#7A5518` |
| Gold gedämpft | `#9A7C40` | `#C7AE7E` |
| Bernstein | `#E8873B` | `#C4661F` |
| Text 1 | `#F5EEE2` | `#241D12` |
| Text 2 | `#B3A68F` | `#6B5D48` |
| Text 3 | `#786A57` | `#A2947C` |
| Warnung | `#C4634A` | `#A33F28` |
| Atemlicht | `rgba(212,162,76,0.13)` | `rgba(168,122,42,0.07)` |
| Erfolg | `#6FA860` | `#6FA860` |

Schriften: Newsreader 300/400 für Fragen und Zitate, Inter 400/500/600 für UI,
JetBrains Mono 400/500 für technische Inhalte. Größen: 10, 11, 12, 13, 14, 15,
16, 17, 18, 20, 24, 26, 28, 32 und 40 sp. Abschnittslabels verwenden 0,8 sp
Laufweite und Großbuchstaben. Standardradien: 16 und 20 dp, Bottom Sheets 28 dp,
Pillen 22 bis 30 dp. Aufhänger 168 x 168 dp. Fortschrittsring 64 dp, Radius 30 dp,
Strich 2,5 dp. Slider-Track 6 dp, Thumb 26 dp.

Schatten und Verläufe: Im Dunkelmodus keine Kartenschatten. Im Hellmodus
`0 2px 24px rgba(120,84,24,0.08)`. Primärflächen verlaufen horizontal von Gold zu
Gold hell. Der atmende Hintergrund ist ein radialer Verlauf mit 60-Prozent-Stopp.

## 3. Motion

| Element | Dauer | Kurve |
|---|---:|---|
| Hintergrund | 20 s, Sitzung 30 s | ease-in-out, endlos |
| Screen | 450 ms | cubic-bezier(0.2,0,0,1) |
| Sitzung | 700 ms | cubic-bezier(0.2,0,0,1) |
| Bottom Sheet | 400 ms | cubic-bezier(0.2,0,0,1) |
| Fragewechsel | 700 ms | cubic-bezier(0.4,0,0.2,1) |
| Lautsprecher | 350 ms | ease-out |
| Lautsprecherpuls | 2,4 s | ease-in-out, endlos |
| Aufnahmeringe | 1,6 s | ease-out, dreifach versetzt |
| Aufnahmeglühen | 3,2 s | ease-in-out, endlos |
| Aufnahmeshimmer | 4,5 s | linear, endlos |
| Dimmung / Aufwachen | 4 s / 300 ms | linear / ease-out |

Bei systemweit entfernten Animationen entfallen Endlosschleifen und Übergänge dauern
höchstens 200 ms.

## 4. Vollständiges Screen-Inventar

1. Start: Kopfzeile, Theme-Schnellwahl, Aufhänger, eigene Frage, Aufnahme in drei
   Zuständen, drei Parameterkarten, gesperrter und aktiver Startknopf.
2. Sitzung: stummer Start, Intro-Frage, Fragenliste, Netzpunkt, Lautsprecher,
   Fortschrittsring, Stopp, Nachschub, Offline, Dimmung und Abschluss.
3. Verlauf: gefüllte Liste, Swipe-Löschen und leerer Zustand.
4. Verlauf-Detail: Replay, Parameterkarten, Zufalls-Toggle und vollständige Fragenliste.
5. Einstellungen: Ablauf, Inhalt, Stimme, KI-Verbindung, Sicherheit, Darstellung, Über.
6. Gesprächsaufhänger: Liste, Drag-Sortierung und Neu-Anlage.
7. Aufhänger-Editor: Emoji, Text, Speichern und Löschen.
8. Skills: Auswahl genau eines Skills, Neu-Anlage und Bearbeitung.
9. Skill-Editor: Name, Mono-Text, editierbarer Betriebsmodus, Speichern und Löschen.
10. Stimme: Edge-/Google-Tabs, 36 Stimmen, Favoriten, Probehören und Schlüsselfehler.
11. ChatGPT: getrennt, Code wartend, Code abgelaufen und verbunden.
12. Rohdaten: lokale Sitzungen, Fragen, Skills und Aufhänger als lesbare Ansicht.
13. App-Sperre: vollständig opaker Sperrbildschirm vor jedem anderen App-Inhalt.

Sheets: beide Pausen, Wiederholungen, Dauer, Anbieter, Modell, Denkstärke und Intro-
Antwort. Jede sichtbare Aktion des Musters erhält eine funktionierende Navigation.

## 5. Zusätzliche Sicherheit

Unter `Einstellungen > Sicherheit` liegt der Schalter `App mit Fingerabdruck sperren`.
Ist er aus, öffnet die App ohne Prüfung. Ist er an, wird bei jedem Wechsel des Prozesses
in den Vordergrund ausschließlich `BIOMETRIC_STRONG` verlangt. Geräte-PIN, Muster und
Passwort sind ausdrücklich kein Fallback. Bis zum Erfolg ist
der gesamte App-Inhalt opak verdeckt und nicht bedienbar. Abbruch oder Fehler lässt die
App gesperrt. Der Entsperrstatus wird nie persistiert. Bei aktivem Schutz setzt die
Activity `FLAG_SECURE`.

## 6. Daten und Funktionen

Room speichert Sitzungen, Fragen, Skills und Aufhänger. Verschlüsselte Preferences
speichern API-Schlüssel, TTS-, Ablauf-, Theme-, Modell-, Denkstärke- und Sperrwerte.
Codex-Login verwendet Device Code und SSE. Groq Whisper verwendet den vierstufigen
Filter. Edge und Google TTS verwenden MediaPlayer. `SessionEngine` ist UI-unabhängig,
startet stumm, verwaltet Takt, Nachschub, Offline-Wiederholung, Timer und Replay.
Ein Foreground Service hält Sitzung, Audio Focus und MediaSession.

## 7. Version

Version und Stand werden ausschließlich aus `BuildConfig.VERSION_NAME` und
`BuildConfig.VERSION_BUMPED_AT` angezeigt. Es gibt keinen zweiten statischen Timestamp.
