---
name: thesenloop
description: Setzt das ThesenLoop-Konzept (Thesen prüfen, belegen, testen, mit Alternativen vergleichen, verfeinern, bewerten, in Runden bis zum Ergebnis) als App um oder erklärt es. Nutze diesen Skill IMMER wenn der Benutzer sagt "implementiere das ThesenLoop-Konzept", "implementiere ThesenLoop", "wir programmieren einen ThesenLoop für Android/Windows/Swift/macOS/iOS", "bau den ThesenLoop", "ThesenLoop-App", "Thesen-Loop", "Thesenloop", "ThesenLoop in die App einbauen", "wie funktioniert der ThesenLoop", "zeig mir das ThesenLoop-Konzept", "Thesenforscher". Auch wenn nur "ThesenLoop" zusammen mit einer Plattform oder einem App-Namen fällt. Die verbindliche Quelle ist KONZEPT.md neben dieser Datei — nie aus dem Gedächtnis bauen.
---

# ThesenLoop umsetzen

## 1. Quelle lesen, nicht raten

Lies zuerst vollständig `KONZEPT.md` in diesem Ordner. Teil 1 ist die Erklärung für
Menschen, Teil 2 die Bauanleitung. Alles, was du baust, folgt Teil 2. Abschnittsnummern
unten beziehen sich auf diese Datei.

## 2. Ziel bestimmen

Aus der Anfrage ermitteln, sonst genau eine Rückfrage:

- **Plattform**: Android (Kotlin/Compose), Windows (C#/WPF) oder Apple (Swift/SwiftUI).
- **Neu oder Einbau**: Eigenständige App oder Modul in einer bestehenden App? Bei Einbau
  das bestehende Projekt zuerst lesen (CLAUDE.md, Architektur, Speicher, Versionsregel).
- **Umfang**: Standard ist der volle Loop (F-01 bis F-11). F-12 (Unter-Helfer) nur auf Wunsch.

## 3. Bauen nach Abschnitt 22

Reihenfolge einhalten:

1. Kern ohne Plattform-Abhängigkeit: Datenmodell (Abschnitt 15), Zustandsautomat (14),
   Halteregeln (18), Bewertungs-Summe. Rollen zunächst als Stubs mit festen JSON-Antworten.
2. Modell-Anschluss: JSON-Prüfung gegen die Ausgabe-Schemas (16), bei Schema-Fehler genau
   eine Wiederholung mit Fehlerhinweis, danach Protokoll-Eintrag.
3. Rollen R-02, R-03, R-04, R-07 mit den Prompts aus Abschnitt 17, wörtlich übernommen.
4. R-05 Tester mit Bildschirm B-04 und Wartezustand WARTET_TEST.
5. R-06 Erfinder mit Wechsel-Logik (WARTET_WECHSEL, Schwelle 10 Punkte).
6. Ergebnis (B-06) mit den fünf Ausgängen und „Was ist neu?", Verlauf (B-07).
7. Fortsetzen nach Neustart (F-10), Einstellungen (B-09).

## 4. Unverhandelbare Regeln

- Kennungen R-xx, F-xx, B-xx, H-xx aus KONZEPT.md tauchen im Quellcode auf (Klassennamen,
  Kommentare, Protokoll-Einträge), damit man nachweisen kann, dass nichts fehlt.
- Der Leiter (R-01) ist Code, kein Modell-Aufruf. Nur „Was ist neu?" darf das Modell nutzen.
- Rollen sind getrennt: Der Skeptiker sucht keine Belege, der Bewerter formuliert nicht um.
- Gesamtpunkte sind immer die Summe der vier Kategorien. Keine eigene „Wahrscheinlichkeit".
- Quellen werden nie erfunden. Ohne Websuche ist jeder Beleg Stufe C „nicht online geprüft".
- Alle Texte an den Nutzer in einfachem Deutsch mit echten Umlauten (Niveau Teil 1).
- Modell-IDs und Websuche-Werkzeug aus der aktuellen API-Doku, nicht aus KONZEPT.md.
  Bei Fragen zur Claude-API vorher den Skill `claude-api` laden.
- Plattform-Zuordnung aus Abschnitt 21 (Speicher, Hintergrund, Schlüssel-Ablage) einhalten.

## 5. Abschluss

Nach dem Bau gelten die Profil-Regeln: bauen, Version bumpen mit echter Systemzeit,
committen, pushen, auf dem Gerät installieren. Dann kurz melden, welche Funktionen
F-01 bis F-12 umgesetzt sind und welche nicht.

## 6. Nur erklären

Fragt der Benutzer nur, wie der ThesenLoop funktioniert, antworte mit Teil 1 von
KONZEPT.md in eigenen Worten, auf demselben Sprachniveau, mit dem Handy-Schlaf-Beispiel.
