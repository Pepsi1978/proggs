# Antigravity Kompass

Android-App, die **Google Antigravity CLI** (`agy`) erklärt — auf Deutsch, auf dem Niveau einer
zehnten Klasse Realschule, mit Vorlesen und Rückfragen per Mikrofon. Schwester-App von
Claude Kompass, Codex Kompass und Kimi Kompass; Aufbau, Bedienung und Code sind gleich, nur
Wissensbasis und Abgleich sind auf Antigravity zugeschnitten.

**Zielgerät:** Galaxy Z Fold 8 (SM-F971B). Das Cover-Display ist der Normalfall; aufgeklappt
wird der Gesprächsbereich zweispaltig.

---

## Die fünf Bereiche

| Bereich | Was drinsteht |
|---|---|
| **Slash-Befehle** | Alle Slash-Befehle, Aliase, mitgelieferten Skills, Startoptionen (`--model`, `-p` …), Unterbefehle (`agy mcp` …) und Tastenkürzel |
| **Config** | Alle Schlüssel aus `~/.gemini/antigravity-cli/settings.json`, Umgebungsvariablen und Konfigurationsdateien (`mcp_config.json`, `hooks.json`, Rules …) |
| **/config** | Jeder Punkt des Settings Editors, den `/config` (Alias `/settings`) öffnet |
| **Best Practices** | Artikel zur Arbeitsweise mit Antigravity CLI |
| **Chat** | Mehrere Gespräche nebeneinander; Antworten beziehen Befehle und Einstellungen mit ein |

Unter jedem Eintrag sitzen dieselben vier Knöpfe: **Vorlesen**, **Fragen**, **Mehr**, **Zurück**.
Ganz unten steht ein Klapp-Bereich **Entfernte Einträge**.

## Wissensbasis

Die mitgelieferten Daten stehen in `app/src/main/assets/` und stammen aus Antigravity CLI
**1.5.15** (`agy --help`, eingebautes Änderungsprotokoll `agy changelog`, mitgelieferte Skills unter
`~/.gemini/antigravity-cli/builtin/skills`) und der offiziellen Doku `antigravity.google/docs`,
deutsch erklärt.

## Der Aktualisieren-Knopf

Oben in der Kopfleiste. Er holt die offizielle Doku als Markdown und gleicht sie mit dem Bestand ab:

| Unterlage | Adresse |
|---|---|
| Befehle und settings.json | `https://antigravity.google/docs/cli/reference.md` (Tabellen `Command \| Category \| Alias \| Execution Purpose` und `Option Key Name \| Value Type \| System Default \| …`) |
| Variablen | `https://antigravity.google/docs/settings.md` (verzichtbar) |
| Fassung | `https://antigravity.google/docs/changelog.md`, nur Abschnitt `## Antigravity CLI` (Köpfe `### [v1.5.12](…)`) |

- Nur eingebaute Befehle und Aliase können „entfernt“ werden; Startoptionen, Unterbefehle,
  Tastenkürzel, Config und Panel werden nur ergänzt. Befehle, die nur im Programm bekannt sind,
  stehen in `Aktualisierer.NUR_IM_PROGRAMM`.
- Untergrenzen (Befehle < 25, Einstellungen < 20) schützen den Bestand.

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
update/          Doku-Abruf, Tabellen-Auswertung, Abgleich (Antigravity-spezifisch)
ai/              Codex: Anmeldung, Anfragen, Anweisungstexte
audio/           Aufnahme, Groq, Filterschichten, WAV-Schnitt
tts/             Drei Vorlese-Dienste, eigene Stimme, Absatz-Pipeline
ui/              Theme, Bausteine, Bildschirme
vm/              Ein Modell je Aufgabe
AppProfil.kt     Alles, worin sich Antigravity Kompass von Claude Kompass unterscheidet
```
