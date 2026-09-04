# Token-Cost-Sidebar für OpenCode

> **Installationsregel für Agenten:** Diese Datei vor jeder Installation oder Aktualisierung
> vollständig lesen. Nicht nur diesen Ordner kopieren, weil Arbeitsmodus-Auswahl und
> Prompt-Injektion aus mehreren gemeinsam benötigten Bestandteilen bestehen.

Stand: v1.14.8 – 04.09.2026 21:19

## Funktionen

- Klickbare Auswahl zwischen Freimodus, Schnellmodus, Normalmodus und Gründlichkeitsmodus.
- Der Freimodus fügt dem Modellaufruf keinen zusätzlichen Arbeitsmodus-Prompt hinzu.
- Klickbare Auswahl aller Varianten, die OpenCode für das aktuelle Modell tatsächlich bereitstellt.
  Dazu gehören modellabhängig unter anderem None, Minimal, Low, Medium, High, XHigh, Max und Thinking.
- OpenAI-, OpenRouter- und andere Provider-Modelle verwenden dieselbe effektive Laufzeitliste; neue
  oder benutzerdefinierte Varianten erscheinen ohne Plugin-Update und bleiben auswählbar.
- Anzeige des live ausgewählten Modells direkt oberhalb der Effort-Auswahl.
- Anzeige des verbleibenden wöchentlichen OpenAI-Kontingents und des Reset-Datums direkt unter OpenAI-Modellen.
- Das Modell erscheint orange, fett und unterstrichen. Darunter steht das Kontingent in der normalen Theme-Textfarbe, beispielsweise `Woche 62% (23. Juli)`; nur das Klammerdatum ist hellgrau. Kontingent, Effort-Auswahl und Arbeitsmodi folgen ohne Leerzeilen direkt aufeinander.
- Sofortige Kontingentaktualisierung nach abgeschlossenen OpenAI-Modellaufrufen, zusätzlicher Nachabruf nach zwei Sekunden und minütlicher Abgleich für andere Codex-Sitzungen.
- Orange, fette und unterstrichene Sidebar-Überschriften für Session, Modell, Context, Theme, MCP und LSP.
- Versionszeile im Format `TUI <Version> (<Datum>, <Uhrzeit>)` unten zwischen Arbeitsverzeichnis und
  OpenCode-Version. Die vollständige Zeile verwendet einheitlich die normale Theme-Textfarbe ohne Hervorhebung.
- Effort-Klicks ändern unmittelbar OpenCodes aktive Model-Variante für den nächsten Modellaufruf.
- Sitzungsbezogene Speicherung des gewählten Arbeitsmodus.
- Injektion der passenden Arbeitsanweisung in jeden neuen Modellaufruf.
- Kompakte Context-Anzeige mit drei Preisen (`Inputpreis`, `Outputpreis`, `Cachepreis`), den
  grau dargestellten Tokenwerten `Cache Token` (`Read | Write`), `Input Token`, `Output Token`,
  `Reasoning Token` sowie direkt anschließend den Einzelposten `Cachekosten` samt live aktualisiertem Verhältnis zu den
  Inputkosten im Format `(3,5 zu 1)`, `Inputkosten`, `Outputkosten`,
  `Reasoningkosten` und abschließend den vollständig fett und rot hervorgehobenen `Gesamtkosten`
  einschließlich Dollarwert.
- Preise und Gesamtkosten werden einheitlich in US-Dollar angezeigt; eine Wechselkursabfrage oder
  EUR-Umrechnung findet nicht statt.
- Die Preise werden live aus `models.dev` geladen. Alle vom Launcher verwendeten OpenAI-Fast-Aliase
  werden auf ihr Basismodell aufgelöst; fehlende Cachepreise werden nicht als Nullpreis erfunden.
- GPT-6 Astra verwendet lokale offizielle Tarife (Stand 04.09.2026), auch ohne Katalogeintrag:
  Standard je 1M Tokens Input/Output/Cache-Read/Cache-Write = 10/50/1/12,50 USD;
  oberhalb von 272.000 Input-Tokens = 20/75/2/25 USD für den gesamten Modellschritt.
  Fast/Priority verdoppelt diese Tarife; Batch/Flex halbiert sie. Reasoning kostet den Outputtarif.
  Der bestehende lokale Cache-Read-Aufschlag von 20 % wird anschließend auch auf Astra angewendet.
  Quelle: https://developers.openai.com/api/docs/pricing . Dies sind API-Vergleichskosten,
  keine Berechnung des ChatGPT-Abopreises; regionale Zuschläge sind nicht enthalten.
- OpenAI-Fast-Aliase verwenden den vom Launcher gesetzten Priority-Service-Tier und dessen offiziellen
  Tarif. Bei ChatGPT-OAuth ist das finale Response-Feld laut OpenAI kein verlaesslicher Nachweis des
  serverseitigen Fast-Routings; dort bleibt deshalb die konfigurierte Fast-Auswahl massgeblich und wird
  intern für die Preisberechnung verwendet. Bei API-Key-Auth wird weiterhin der vom Provider
  bestaetigte Response-Tier verwendet; GPT-5.5-Write-Preise werden nicht erfunden.
- Kumulative Session-Summen für Input, Output, Reasoning und Gesamtkosten, die durch Compress,
  ausgeblendete ältere Messages oder Modellwechsel nicht zurückgesetzt oder verkleinert werden.
- Linksbündige, orange und fette Theme-Auswahl mit direkt folgendem Theme-Namen sowie nebeneinanderliegender Dunkel-/Hell-Umschaltung; der aktive Modus ist fett.
- Beim Theme `orng` bleibt die Akzentfarbe auch im Dunkelmodus orange, statt auf Weiß zu wechseln.
- Das aktive `AGENTS.md`-Profil bleibt vollständig gültig und hat bei Widersprüchen Vorrang.

## Vollständiger Installationsumfang

Für eine funktionsfähige Installation werden immer alle folgenden Bestandteile benötigt:

| Bestandteil | Repo-Quelle | Installationsziel | Zweck |
|---|---|---|---|
| TUI-Plugin | `opencode-setup/plugins/token-cost-sidebar/` | `~/.config/opencode/plugins/token-cost-sidebar/` | Sidebar und Modusauswahl |
| System-Plugin | `opencode-setup/plugins/work-mode.js` | `~/.config/opencode/plugins/work-mode.js` | Prompt-Injektion pro Modellaufruf |
| TUI-Konfiguration | `opencode-setup/tui.json` | `~/.config/opencode/tui.json` | Registriert die Sidebar |
| Abhängigkeiten | `package.json` und Installer | `~/.config/opencode/node_modules/` | OpenCode-, OpenTUI- und Solid-Laufzeit |

Die maßgebliche Quelle der Modusbezeichnungen und Prompts ist
`dist/work-mode.ts`. Eine zweite Kopie der Prompttexte soll nicht gepflegt werden.

## Kostenformel

Die Sidebar rechnet jeden abgeschlossenen Modellschritt mit dessen Modell und Kontextstufe ab:

```text
Input-Kosten     = regulärer Input × Inputpreis
Cache-Kosten R   = Cache-Read × Cache-Read-Preis
Cache-Kosten W   = Cache-Write × Cache-Write-Preis
Output-Kosten    = Output ohne Reasoning × Outputpreis
Reasoning-Kosten = Reasoning × Reasoningpreis, sonst Outputpreis
Gesamtkosten     = Input-Kosten + Output-Kosten + Reasoning-Kosten + Cache-Kosten
Cache-Verhältnis = Cache-Kosten / Input-Kosten zu 1
```

Für OpenAI-Modelle wird der Cache-Read-Preis vor der Berechnung um 20 % erhöht. Andere Provider
verwenden unverändert ihre eigenen Cache-Read- und Cache-Write-Preise.

Der sichtbare Wert `Input` enthält nur regulär bepreisten Input. Cache-Read und Cache-Write werden
separat im Format `Read | Write` angezeigt. `Cachepreis` verwendet die R/W-Reihenfolge; fehlende
Cachepreise erscheinen als `n/v` statt als erfundene Null.
Effort-Stufen haben keinen eigenen Multiplikator; sie beeinflussen nur die tatsächlich erzeugte
Reasoning-Menge.
Die unter `Context` gezeigten Werte enthalten jeden API-Modellaufruf der Session; wiederverwendete
Kontexttokens können daher über viele Aufrufe deutlich größer als das aktuelle Kontextfenster werden.
Fehlt ein erforderlicher Preis, zeigt die Sidebar für die nicht belastbar aufteilbaren Werte
`nicht verfügbar`, statt einen Preis zu erfinden.
Sind keine positiven Inputkosten vorhanden oder ist die Kostenaufteilung nicht verfügbar, erscheint
das Verhältnis als `(n/v)`.

## Empfohlene Installation

Auf einem neuen Rechner zuerst das Repository nach `~/proggs` klonen oder aktualisieren.
Danach den plattformgerechten Setup-Installer ausführen; er installiert alle oben genannten
Bestandteile gemeinsam.

### Windows

```powershell
pwsh ~/proggs/opencode-setup/install.ps1
```

### macOS oder Linux

```bash
bash ~/proggs/opencode-setup/install.sh
```

Die Installer richten zusätzlich die übrige versionierte OpenCode-Umgebung ein. Soll nur dieses
Plugin installiert werden, müssen trotzdem alle vier Bestandteile aus der Tabelle kopiert und die
Abhängigkeiten aus `package.json` installiert werden.

## Pflicht nach Installation

OpenCode vollständig beenden und neu starten. Plugin- und Konfigurationsdateien werden beim Start
geladen und in einer bereits laufenden Session nicht automatisch aktualisiert.

## Verifikation

1. Die Sidebar beginnt mit `Session` und Zeitstempel; unter dem Session-Titel stehen nach einer Leerzeile das aktive Modell und direkt darunter alle von OpenCode für dieses Modell bereitgestellten Varianten. Unten steht die Plugin-Version zwischen Arbeitsverzeichnis und OpenCode-Version.
2. Die rechte Sidebar zeigt darunter `Freimodus`, `Schnellmodus`, `Normalmodus` und `Gründlichkeitsmodus`.
3. Ein Effort-Klick ändert denselben validierten Model-Variant-State wie OpenCodes eigener Varianten-Picker.
4. Ein Modus-Klick markiert den gewählten Modus und die Auswahl bleibt in derselben Session erhalten.
5. Beim nächsten Modellaufruf beginnt die zusätzliche Systemanweisung mit
   `AKTIVER ARBEITSMODUS: <gewählter Modus>`; im Freimodus wird keine solche Anweisung ergänzt.
6. Die Anweisung bestätigt, dass das aktive `AGENTS.md`-Profil vollständig und unverändert gilt.
7. Die Plugin-Version in `package.json` entspricht der Version dieser README.
8. Bei einem OpenAI-Modell steht neben dem Modell beispielsweise `Woche 66 % · 23.07.`; bei anderen Providern erscheint keine Kontingentanzeige.
9. Unter `Context` stehen `Inputpreis`, `Outputpreis`, `Cachepreis` und die vier grauen Tokenzeilen; direkt auf `Reasoning Token` folgen `Cachekosten (<Verhältnis> zu 1)`, `Inputkosten`, `Outputkosten`, `Reasoningkosten` und die ganz unten fett und rot dargestellte Zeile `Gesamtkosten`.

Tests im Repo:

```bash
cd ~/proggs/opencode-setup/plugins/token-cost-sidebar
bun test
```

## Zustände und Fehlerverhalten

- Der Modus wird pro Session unter `~/.local/state/opencode/work-modes/` gespeichert.
- Neue Sessions starten standardmäßig im Schnellmodus. Der OpenLauncher kann den Startmodus
  pro Prozess über `OPENLAUNCHER_WORK_MODE` vorauswählen.
- Kann der gespeicherte Modus nicht gelesen werden, verwendet das System-Plugin den gewählten
  Startmodus (ohne Launcher: Schnellmodus) und schreibt den Fehler in das OpenCode-App-Log.
