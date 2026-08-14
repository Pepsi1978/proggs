# Entwurf nach Durchgang 1 — StackLabor

Stand: 14.08.2026, 12:28 · Stufe: v1 (Entwurf, noch kein fertiges Spec) · Plattform: Android

> Zwischenstand des Grillings. Wird von der `spec-schmiede` fortgeschrieben und am Ende durch
> die vier Spec-Dateien nach `Specs/FORMAT.md` ersetzt.

## Block A — entschieden

| Punkt | Wert |
|---|---|
| Anzeigename / Kurzname | **StackLabor** |
| Ordner | `Specs/StackLabor/` · Quellcode `~/proggs/StackLabor/` · ZIP `Designs/Inbox/StackLabor-SPEC-v1.zip` |
| Verhältnis zu NEMS | Eigenständige neue App. NEMS (Einnahme-Tracker) bleibt unangetastet. StackLabor ist ein Komponier-Werkzeug, kein Tracker. |
| Plattform / Technik | Android · Kotlin + Jetpack Compose |
| Zielgerät | Galaxy Z Fold 8 (SM-F971B). **Leitgröße Cover zugeklappt: 1248×1972 px @ 420 dpi (~297×469 dp).** Zweites Layout Innendisplay 1848×2448 @ 420 dpi, 120 Hz (~440×583 dp), zweispaltig: links Ziele, rechts Stack + Auswertung. Systemschrift steht auf 90 %. |
| Für wen | Nur Frank selbst. Kein Onboarding, kein Recht-Spec, keine Store-Vorgaben, kein Mehrbenutzer. |
| Sprache | Deutsch, einsprachig |
| Ohne Netz | Stacks/Ziele/Fragen bearbeiten und alle Ampeln rechnen offline. Nur neue KI-Auswertungen brauchen Netz. |
| Anmeldung | Codex-OAuth (Geräte-Flow) wie in PerfectMoment. Sonst kein Konto. |
| Datenhaltung | Room-Datenbank auf dem Gerät + Export/Import als Datei |
| Startbestand | `C:\Users\barwa\Meine Ablage\Dokumente\KI\Backup\Stack.docx` — 6 Stacks, 72 NEM-Einträge |

## Die 13 Entscheidungen aus Durchgang 1

| Nr | Frage | Entscheidung |
|----|-------|--------------|
| 1 | Was ist ein Stack? | **Ein Zeitpunkt.** Zwecke wie „Senolytika" werden zu Zielen, nicht zu Stacks. Jedes NEM steht genau einmal je Stack. |
| 2 | Ziele global oder je Stack? | **Einmal anlegen, Reihenfolge je Stack.** Ein Ziel wird einmal getippt; je Stack wird angehakt, welche gelten, und dort per Drag & Drop unabhängig priorisiert. |
| 3 | Bedeutung des Häkchens | **Gespeicherter Zustand „aktiv in diesem Stack".** Häkchen weg = abgeschaltet, ausgegraut, aber sichtbar; bleibt beim Schließen erhalten. Endgültiges Entfernen per Wischen. |
| 3b | Grundlage der Ampeln | **Bewertungstabelle NEM × Ziel** von der KI (stützt / neutral / stört + Begründung), gespeichert. Ampeln rechnet die App lokal daraus — Häkchen und Priorität ändern sie in Millisekunden, kostenlos, offline. Neue KI-Abfrage nur bei inhaltlicher Änderung. |
| 4 | Sortierung der NEM-Liste | **Umschaltbar.** Gespeichert wird die Einnahme-Reihenfolge (per Drag & Drop änderbar). Umschalter oben: „Löslichkeit" (Standard, erst wasserlöslich dann fettlöslich) und „Einnahme". |
| 5 | Frequenz und Alternierung | **Als Felder am NEM**, kein Kalender. Feld „Frequenz" (täglich / alle N Tage) und Feld „alterniert mit …". Die KI berücksichtigt beides → keine Fehlalarme bei den alternierenden Paaren. |
| 6 | Ampel am NEM | **Schlimmster Fall, gewichtet nach Zielpriorität.** Stört ein Top-Ziel → rot; nur ein niedriges → gelb; nichts → grün. Zweite Zeile nennt „stört Ziel 3, 7". Tipp öffnet die Aufschlüsselung. Ziel-Umsortieren ändert die Ampeln ohne KI-Abfrage. |
| 7 | Dosis-Erfassung | **Stückzahl × Menge je Stück**, Anzeige „2 × 80 mg = 160 mg". Einheiten mg/µg/g/ml/**IE**. Darreichungsform (Kapsel/Tablette/Löffel/Tasse/Pulver) als eigenes Feld. Sonderfälle wie „4,2 : 1,8" in ein Zusatz-Textfeld, das die KI mitliest. |
| 8 | Darstellung am Eintrag | **Zweizeilig** (einzeilig passt rechnerisch nicht: 265 dp verfügbar, 264 dp verbraucht). Löslichkeit bleibt als **Punkt** (grün = wasserlöslich, weiß **mit dünnem Rand** = fettlöslich, beide = beides). Die **Ampel wird ein 3 dp Farbbalken an der linken Kante**. „Pulver" als Wort in Zeile 2. |
| 9 | Erscheinungen | **Hellmodus ist Standard**, Dunkelmodus vollwertig daneben, beide für die gesamte App. **Umschalter direkt auf dem Hauptbildschirm.** |
| 10 | Spezialeffekte | **Volles Programm** — Glasflächen, Verlaufsränder, atmende Aura an roten Ampeln, Schimmer beim Laden, animierter Kopf-Verlauf, Parallax, gestaffeltes Einblenden, Tiefenschatten, Haptik, wortweises Aufblenden des KI-Texts. **Weichzeichner nur auf festen Flächen, nie über der scrollenden Liste** (einziger 120-Hz-Killer). |
| 11 | Umfang der Auswertung | **Zwei getrennte Knöpfe.** Im Stack: „Diesen Stack auswerten". Auf dem Hauptbildschirm: „Alle Stacks zusammen prüfen" mit Tagesgesamtdosis je Wirkstoff und stackübergreifenden Konkurrenzen. |
| 12 | Sicherung | **Export/Import als Datei** (Stacks, NEM, Ziele, eigene Fragen, Bewertungen). Startbestand liegt als Datei in der App, per Knopf neu einlesbar — nicht fest einprogrammiert. Kein Google Drive. |
| 13 | Konkurrenzprüfung beim Hinzufügen | **NEM wird immer sofort aufgenommen, Prüfung läuft nach.** Danach Hinweis mit „Behalten" / „Doch entfernen". Kein Blockieren, keine Wartezeit beim Eintippen. |

## Vorhandene Bausteine im Repo (verbindlich als Vorbild)

**Codex** — `~/proggs/PerfectMoment/app/src/main/java/de/frank/perfectmoment/auth/`
- `CodexAuthManager.kt` (50.758 B), `CodexModels.kt`
- OAuth-Geräte-Flow gegen das ChatGPT-Konto:
  `auth.openai.com/api/accounts/deviceauth/usercode` → `…/deviceauth/token` → Refresh über
  `auth.openai.com/oauth/token`; Anfragen an `chatgpt.com/backend-api/codex/responses`
- Modelle: `gpt-5.6-sol` (Sol), `gpt-5.6-terra` (Terra, Standard), `gpt-5.6-luna` (Luna)
- Denkstufen: low / medium / high / xhigh / max
- Fehlerklassen: `AuthErrorKind.REAUTH | QUOTA | NETWORK`, mit `retryable`-Kennzeichen

**Vorlesefunktion** — `~/proggs/PerfectMoment/.../tts/` und `~/proggs/EntropieReductor/.../tts/`
- Drei Anbieter (`TtsProvider`): `EDGE` (Microsoft Edge), `GOOGLE_CLOUD` (Google Chirp 3 HD),
  `QWEN_CLONE` („Meine Stimme" — geklonte eigene Stimme)
- Stimmen: 6 deutsche Edge-Stimmen (Standard `de-DE-SeraphinaMultilingualNeural`),
  30 deutsche Chirp-3-HD-Stimmen (Standard `de-DE-Chirp3-HD-Kore`)
- Bausteine: `EdgeTtsPlayer`, `GoogleCloudTtsPlayer`, `QwenTtsPlayer`, `TtsManager`, `TtsCatalog`,
  `QwenVoiceDirectory`, `QwenVoiceEnrollment`, `SpeechLoudness`, `VoiceScreens`
- Aus EntropieReductor zusätzlich: `TtsPlayer`, `TtsUsageStore` (Verbrauchszählung),
  `TtsUsageBackup`, `TtsPlaybackService`, `GoogleTtsApi`, `GoogleTtsVoices`
- **Wichtig:** Die Technik wird 1:1 übernommen, der Einstellungs-Bildschirm von EntropieReductor
  NICHT — er ist auf dessen Domäne (Mentals, Gewohnheiten) zugeschnitten. StackLabor bekommt
  einen eigenen Einstellungs-Bildschirm aus denselben Bausteinen.

## Datenbestand (aus Stack.docx)

6 Stacks als Zeit-Slots, 72 NEM-Einträge. Vollständige Liste siehe
`Specs/StackLabor/v1/STARTBESTAND.md` (wird beim Schreiben des Spec-Pakets angelegt).

| id | Name | Zeitpunkt | Einnahme-Hinweis |
|---|---|---|---|
| morning1 | Morgen-Stack Teil 1 | Direkt nach dem Aufstehen | nur mit Wasser |
| morning2 | Morgen-Stack Teil 2 | 60 Minuten nach dem Aufstehen | mit Olivenöl und Wasser |
| presport | Pre-Sport-Stack | 45 Minuten vor dem Sport | mit Olivenöl und Wasser |
| evening1 | Abend-Stack Teil 1 | 2 Stunden vor dem Schlafen | mit Wasser |
| evening2 | Abend-Stack Teil 2 | 60 Minuten vor dem Schlafen | mit 1 EL Olivenöl und Wasser |
| evening3 | Abend-Stack Teil 3 | Direkt vor dem Schlafen | mit Wasser |

Zusätzliche Merkmale aus der Quelle, die abgebildet werden:
Durchfallrisiko (13 NEM) · Darreichungsform Pulver · Frequenzen (täglich bis alle 7 Tage) ·
alternierende Paare (Pterostilben ⇄ Trans-Resveratrol · Brokkoli ⇄ Grüntee ·
Citicolin ↔ Uridin + Phosphatidylserin) · Hersteller (Greenfood, ZENement, Thorne) ·
kontextabhängige Dosis (Venlafaxin 50 mg Frei / 75 mg Dienst) · Beistoffe ·
Kombi-Einträge („zusammen einnehmen") · Mehrfachvorkommen desselben NEM in mehreren Stacks

## Noch offen (Durchgang 2)

- Bildschirmliste mit Kennungen `B-01 …` und die Wege dazwischen
- Funktionsliste mit Kennungen `F-01 …`, je mit Auslöser/Ablauf/Daten/Ergebnis/Fehlerfall/Grenzen
- Wo die eigenen KI-Fragen verwaltet werden und wie sie in die Anfrage eingehen
- Wo und wie vorgelesen wird; welche Stimme voreingestellt ist
- Ob Codex-Modell und Denkstufe in den Einstellungen wählbar sind
- Verhalten bei Codex-Fehlern (REAUTH / QUOTA / NETWORK) im Bild
- Bewegungs-Spec mit Kennungen `M-01 …` (Vorschläge liegen vor, noch nicht bestätigt)
- Was der Hauptbildschirm zeigt und was der Startbildschirm ist
- Abnahmekriterien `A-01 …`
- Suche/Filter bei 72 NEM; Verschieben eines NEM zwischen Stacks; Stack duplizieren
