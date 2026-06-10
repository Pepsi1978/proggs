# finale-Plugin — Crash- & Stresstest-Protokoll 2026-06-01

> Auftrag Frank: Kompletter Verifikations-Durchlauf BestJournalAndroid v0.21.9.
> Stresstest-Modus: **über 7 gleichzeitige Agenten erlaubt** (Frank-Direktive 2026-06-01).
> Ziel: testen wie viele parallele opus[1m]-Worker der Server wirklich vertraegt,
> nachdem die opus[1m]-Migration (1M Kontext) den FIN-048-Kontextueberlauf behoben hat.
> Jeder Crash / jedes Rate-Limit wird hier mit Ursache protokolliert.

## Rahmenbedingungen
- CPU: 32 logische Kerne → Workflow-Auto-Cap = min(16, 30) = **16 gleichzeitig**
- Modell aller Worker: opus[1m] (Session-Modell, 1M Kontext)
- App: BestJournalAndroid v0.21.9 (versionCode 290), 27 Locales, 1098 DE-Strings, 143 .kt
- FIN-051 (max 7) bewusst AUSGESETZT fuer diesen Test.

## Crash-/Rate-Limit-Ereignisse

| # | Phase | Worker/Scope | Symptom | gleichzeitig aktiv | Retry-Ergebnis |
|---|-------|--------------|---------|--------------------|----------------|
| 1 | A Roentgen (Versuch 1) | ALLE 16 Worker | `API Error: Server is temporarily limiting requests (not your usage limit) · Rate limited` → Worker mitten in Arbeit abgewuergt → "StructuredOutput not called" → Workflow-TypeError | **16** | Versuch 1 komplett gescheitert. Neustart mit 7+Continuous geplant. |

### BEFUND (Root Cause, Frank-bestaetigt 2026-06-01)
- **Es war NICHT Kontext-Ueberlauf (FIN-048).** Worker liefen normal an (Bash `wc -l` erfolgreich),
  Kontext war mit opus[1m] (1M) nie das Problem. `subagent_tokens:0` + 24s Dauer = Server-Drossel,
  nicht Token-Limit.
- **Es war SERVER-SEITIGES RATE-LIMITING.** 16 gleichzeitige Opus-Anfragen ueberschreiten die
  momentane Rechenkapazitaet, die Anthropic bereitstellt ("not your usage limit"). Frank-Zitat:
  "Die koennen so viel nicht gleichzeitig verarbeiten. Rechenkapazitaet."
- **opus[1m]-Migration funktioniert wie geplant**: der Kontext-Crash-Grund ist beseitigt; das
  Server-Rate-Limit ist ein SEPARATES, variables Limit ("die machen das einfach mal").
- **Konsequenz**: FIN-051 (Server-Cap) ist die echte Obergrenze, NICHT das Kontextfenster.
  16 gleichzeitig = zu viel. 7 mit Continuous-Spawning = stabil (bestaetigt 2026-05-26). Neustart so.
- **Folge-Bug im Workflow-Script (selbst verschuldet)**: `crashed.map(r => r._key)` crasht bei
  null-Eintraegen → Script muss null-sicher sein. Im Neustart behoben.

## Beobachtungen pro Workflow

### Workflow A — Röntgen (geplant 7 Worker gleichzeitig)
(wird befuellt)

### Workflow B — Rechtssicherheit (geplant ~7-8 gleichzeitig)
(wird befuellt)

### Workflow C — Cross-Lingual 26 Sprachen (Stresstest: bis 16 gleichzeitig)
(wird befuellt)

## 🔴 KRITISCHER AUDIT-BEFUND (Selbst-Audit, da Worker gedrosselt)

**GUJARATI (values-gu) IST ZERSTOERT — RELEASE-BLOCKER:**
- 153 Strings enthalten das Korruptions-Symbol `♻` statt Text (z.B. `ai_limit_title: '♻'`,
  `paywall_monthly_plan: '♻, %1$s/♻'`, `churn_auto_renew_note: 'Google Play ♻'`).
- 139 substanzielle Strings haben KEIN einziges Gujarati-Zeichen.
- ~26 weitere (retro_*) sind nur Satzzeichen-Fragmente ('.', '. .).').
- Betrifft rechtskritische Strings: Auto-Renewal-Hinweis, Paywall, Limit-Anzeige.
- **NUR gu betroffen** — `♻` kommt in keiner anderen der 25 Sprachen vor (verifiziert).
- Root Cause: vermutlich frueher fehlgeschlagener gu-Uebersetzungslauf (Encoding/Tool ersetzte
  Gujarati-Zeichen durch `♻`-Platzhalter). Nie entdeckt, weil gu in keiner Audit-Stichprobe war
  (letzter Lauf: auditedLanguages=[de] + 5-Sprachen-Stichprobe FR/IT/ES/EN/DE).
- Erkennungsmethode: Laengen-/Script-Coverage-Check (Vollstaendigkeits- + Unuebersetzt-Check
  sahen es NICHT, da Keys vorhanden + != DE).

**Sekundaer (MITTEL):** `settings_delete_account_confirm_body` — fr/es/it/nl/pt nennen
"Firebase-Konto" + listen keine Audio-Aufnahmen; DE/EN sind aktueller (Audio + "nicht dein
Google-Konto"-Klarstellung). Aktualitaets-Inkonsistenz, kein Blocker.

**Positiv verifiziert (Ist-Zustand):** R8 nur Release ✓, Legal-Links vorhanden ✓,
Marketing-Claims (unbegrenzt Profile/Nachträge) durch Code gedeckt ✓, Format-Crash 0 ✓,
Vollständigkeit 26/26 ✓, Widerruf/Health-Disclaimer/Auto-Renewal in lesbaren Sprachen korrekt ✓,
Debug-Bypässe sauber an BuildConfig.DEBUG gebunden ✓.

## Fazit Stresstest (Worker-Parallelitaet 2026-06-01)
- 16 gleichzeitig: Server-Rate-Limit in <25s, 0 Output.
- 7 gleichzeitig: ebenfalls Rate-Limit (Anthropic-Compute akut knapp, "not your usage limit").
- opus[1m] verhindert Kontext-Crash (FIN-048 geloest), ABER Server-Drossel ist die echte Grenze.
- An diesem Tag: parallele Opus-Worker praktisch nicht nutzbar → Hauptagent-Selbst-Audit ist
  der robuste Fallback (1 Strang, keine Drossel). Empfehlung FIN-051 bleibt: ≤7, mit
  Continuous-Spawning + Retry; bei akuter Drossel ganz auf Selbst-Scan ausweichen.
