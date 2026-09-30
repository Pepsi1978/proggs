# Kimi Kompass

Android-App, die **Kimi Code CLI** (Moonshot AI) erklärt — auf Deutsch, auf dem Niveau einer
zehnten Klasse Realschule, mit Vorlesen und Rückfragen per Mikrofon. Schwester-App von
Claude Kompass; Aufbau, Bedienung und Code sind gleich, nur Wissensbasis und Abgleich sind auf
Kimi zugeschnitten.

**Zielgerät:** Galaxy Z Fold 8 (SM-F971B). Das Cover-Display ist der Normalfall; aufgeklappt
wird der Gesprächsbereich zweispaltig.

---

## Die fünf Bereiche

| Bereich | Was drinsteht |
|---|---|
| **Slash-Befehle** | Alle Befehle, Aliase und mitgelieferten Skills, je mit ausführlicher Erklärung |
| **Config** | Schlüssel aus `config.toml`, `tui.toml`, `.kimi-code/local.toml` und die Umgebungsvariablen |
| **/config** | Jeder Punkt des Settings-Panels, das `/settings` (Alias `/config`) öffnet |
| **Best Practices** | Artikel zur Arbeitsweise mit Kimi Code CLI |
| **Chat** | Mehrere Gespräche nebeneinander; Antworten beziehen Befehle und Einstellungen mit ein |

Unter jedem Eintrag sitzen dieselben vier Knöpfe: **Vorlesen**, **Fragen**, **Mehr**, **Zurück**.
Ganz unten steht ein Klapp-Bereich **Entfernte Einträge**.

## Wissensbasis

Die mitgelieferten Daten stehen in `app/src/main/assets/` und stammen aus dem Programm selbst
(Kimi Code CLI **2.1.1**, `kimi.exe`), deutsch erklärt. Generator-Skripte liegen in `tools/`.

- `slash_befehle.json` – 71 Einträge (Eingebaut, Alias, Mitgelieferter Skill, Muster)
- `config_einstellungen.json` – 129 (config.toml, tui.toml, Umgebungsvariablen)
- `panel_eintraege.json` – 29 Punkte des Settings-Panels
- `best_practices.json` – 12 Artikel

## Der Aktualisieren-Knopf

Oben in der Kopfleiste. Er holt die offizielle Doku aus
`github.com/MoonshotAI/kimi-code` (`docs/en`) und gleicht sie mit dem Bestand ab:

| Unterlage | Datei |
|---|---|
| Befehle | `reference/slash-commands.md` (Tabellen `Command \| Alias \| Description \| Always available`) |
| Einstellungen | `configuration/config-files.md` (Tabellen `Field \| Type \| Default \| Description` je Abschnitt) |
| Variablen | `configuration/env-vars.md` (Überschriften `` ### `KIMI_…` `` plus Tabellen) |
| Fassung | `release-notes/changelog.md` (Köpfe wie `## 2.1.1 (2026-09-24)`) |

Bewusst so gebaut:

- **Namen werden ohne Modell gelesen.** Tabellen werden direkt ausgewertet, Spalten über ihren
  Namen gefunden. Erklärt wird erst danach, über Codex.
- **Nur Befehle können „entfernt“ werden.** Die Wissensbasis stammt aus dem Programm und kennt
  mehr als die Doku-Seiten (etwa `/tower`, `/effort`, `/remote-control`). Config und Panel
  werden deshalb nur ergänzt; bei Befehlen sind die nur im Programm bekannten Namen von der
  Entfernt-Regel ausgenommen (`Aktualisierer.NUR_IM_PROGRAMM`).
- **Untergrenzen schützen den Bestand.** Kommen zu wenige Einträge zurück (Befehle < 25,
  Einstellungen < 30), bricht der Lauf ab, statt den Bestand zu leeren.
- **Panel-Neuerungen** kommen aus dem Änderungsprotokoll: Zeilen, die `` `/settings` `` nennen,
  etwa „the TUI mode setting in `/settings`“ oder „`/settings` → Mermaid diagrams“.
- **Jeder Zwischenstand wird sofort gespeichert**, jede Erklärung einzeln.

## Schlüssel

Alle in den Einstellungen, verschlüsselt abgelegt (`EncryptedSharedPreferences`), mit Prüfknopf:
Google Cloud TTS (Vorlesen), Alibaba DashScope (eigene Stimme), Groq Whisper (Spracheingabe).
KI-Antworten laufen über Codex (Anmeldung per Gerätecode).

## Bauen

```
./gradlew :app:assembleDebug           # Debug-Paket
./gradlew :app:testDebugUnitTest       # Tests
./gradlew :app:assembleRelease         # Release inkl. R8
```

Version steht ausschliesslich in `app/src/main/assets/versionslog.json`.

**Stolperfalle:** Schlägt der erste Release-Build nach einer Änderung mit gesperrtem
`classes.dex` oder „Unresolved reference“ fehl: `./gradlew --stop`,
`app/build/kotlin/compileReleaseKotlin` und `app/build/tmp/kotlin-classes/release` löschen,
neu bauen.

## Aufbau

```
observability/   Protokoll (JSON-Zeilen), globaler Fehlerfänger, Logik-Sonden
data/            Room, Einstellungen, Wissensbasis-Lader, Sicherung
update/          Doku-Abruf, Tabellen-Auswertung, Abgleich (Kimi-spezifisch)
ai/              Codex: Anmeldung, Anfragen, Anweisungstexte
audio/           Aufnahme, Groq, Filterschichten, WAV-Schnitt
tts/             Drei Vorlese-Dienste, eigene Stimme, Absatz-Pipeline
ui/              Theme, Bausteine, Bildschirme
vm/              Ein Modell je Aufgabe
AppProfil.kt     Alles, worin sich Kimi Kompass von Claude Kompass unterscheidet
```
