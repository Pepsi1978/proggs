# Design: Selbst-Regeln fuer den Cortex-Chat-Agenten

> Stand: 08.07.2026, 20.59 Uhr · Projekt: second-brain-server (Cortex) · Status: abgenommen, bereit fuer Umsetzungsplanung

## Problem

Der Cortex-Chat-Agent kann sich aktuell **keine dauerhaften Verhaltensregeln** geben. Im Gespraech vom
08.07.2026 wollte Frank eine Dauerregel ("antworte immer als TTS-freundlicher Fliesstext"). Der Agent
bot stattdessen wiederholt an, den Satz als *Gedaechtnis-Eintrag* (Faktenwissen) zu speichern, und gab
schliesslich zu, kein Werkzeug zu haben, um in eine Regeldatei zu schreiben. Ergebnis: keine Regel
entstand, und das Dauer-Nachfragen ("soll ich speichern?") nervte.

Das Feature ist in `second-brain-server/CORTEX-AGENT-UMBAU-PLAN.md` §3.4 ("Regelpool / Regeldatei")
bereits geplant, aber nie gebaut worden (offenes To-Do #4). Es existiert eine perfekte 1:1-Vorlage:
das "Gelernte Regeln"-System des Nachtschicht-Bibliothekars.

## Ziel

1. Der Chat-Agent kann sich dauerhafte Verhaltensregeln geben, die in **jedem** kuenftigen Gespraech
   automatisch wirken.
2. Frank bestaetigt jede Regel genau einmal ueber einen Ja/Nein/Bearbeiten-Dialog im Chat.
3. Eine Regel-Chronik im Dashboard (Einstellungen) zeigt und verwaltet alle Regeln.

## Kernabgrenzung: Regel vs. Gedaechtnis-Eintrag

| | Regel (neu) | Gedaechtnis-Eintrag (bestehend) |
|---|---|---|
| Inhalt | WIE der Agent sich verhaelt/antwortet | Faktenwissen, Notizen |
| Ablage | `agent-data/agent-rules.json` | Qdrant (via brain-api) |
| Wirkung | in jeden System-Prompt injiziert | auf Suche/Abruf hin geladen |
| Werkzeug | `schreibe_regel(text)` | `speichere` / `remember` |

Diese Abgrenzung ist der Kern des Fixes: Die Tool-Beschreibung von `schreibe_regel` sagt klar, dass es
**nur** fuer dauerhafte Verhaltensanweisungen gilt, nicht fuer Faktenwissen. Damit verschwindet die
Verwechslung aus dem gezeigten Gespraech.

## Architektur-Kontext (Ist-Zustand)

- **Agent-Dienst** `second-brain-server/agent/app.py` (~6000 Zeilen, FastAPI, Port 8002). Enthaelt den
  Chat-Agenten. Persistentes Volume `./agent-data:/app/data` (`AGENT_DATA_DIR`).
- **System-Prompt der finalen Antwort**: `TOOLAGENT_SYSTEM` (Konstante, agent/app.py ~Z. 4059). Dieser
  Prompt erzeugt jede Chat-Antwort — hier muessen die Regeln hinein, damit sie ueberall greifen.
- **Tools**: `build_agent_tools()` (~Z. 4076) mit `tools`-Schema-Liste + `handlers`-Dict. TODO-Kommentar
  (~Z. 4056) haelt bereits fest, dass Schreib-Werkzeuge (`speichere`/`schreibe_regel`) folgen.
- **Bestaetigungs-Flow**: `session["pending"]` (dict mit `mode`-Diskriminator) + strukturiertes Antwort-
  Feld `options = [{label, send}]`. Der Speicher-Flow (`mode="save_confirm"`, ~Z. 5293-5386) rendert
  heute schon Buttons im Chat. Vorlage fuer den Regel-Dialog.
- **Vorlage Bibliothekar** `second-brain-server/librarian/app.py`: `lernregeln.json`, `load_learned()`/
  `save_learned()` (Z. 278-285) mit atomarem Schreiben (`_read_json`/`_write_json`, Z. 169-183),
  `_learned_block()`/`_with_rules()` (Z. 288-299) fuer die Prompt-Injektion, CRUD-Endpunkte
  `POST/PUT/DELETE /learn` (Z. 2290-2387), Anzeige `renderLibLearned()` im Dashboard (index.html
  Z. 4752-4826).
- **Dashboard** `second-brain-server/dashboard/`: Single-Page `static/index.html` (~7000 Zeilen,
  inline CSS+JS), `app.py` serviert HTML + Proxys. Chat-Proxy `POST /api/chat` (app.py ~Z. 910),
  Librarian-Proxy `/api/lib/{path}` mit Whitelist `_LIB_ALLOWED` (~Z. 1351).

## Komponenten

### K1 — Regel-Ablage (agent/app.py)

Neue Datei **`agent-data/agent-rules.json`**. Format 1:1 wie `lernregeln.json`:

```json
[{ "id": "<10-hex>", "text": "<Regeltext>", "titel": "<KI-Kurztitel>",
   "enabled": true, "created_at": "<ISO-8601>" }]
```

- `load_rules()` / `save_rules(list)` nach Muster `load_instructions` (agent/app.py ~Z. 2491) bzw.
  `_write_config_file` (~Z. 2753): atomar (tmp + `os.replace`), `threading.Lock`, fehlertolerant
  (kaputte Datei -> leere Liste, toetet den Dienst nie).
- **Limit**: max. ~40 Regeln. Beim Ueberschreiten wird das Anlegen abgelehnt mit klarer Meldung
  ("Regel-Limit erreicht, bitte zuerst eine Regel loeschen"). Kein stilles Verwerfen.

### K2 — Prompt-Injektion (agent/app.py)

- Neue Funktion `build_toolagent_system()` ersetzt die statische Konstante `TOOLAGENT_SYSTEM` an der
  Aufrufstelle (`_toolagent_answer` ~Z. 5209). Sie haengt an den Basis-Prompt einen Block:

  ```
  DAUERHAFTE REGELN VON FRANK (verbindlich, immer befolgen):
  - <text Regel 1>
  - <text Regel 2>
  ```

  gebildet nur aus `enabled`-Regeln (Vorbild `_learned_block()`). Ist die Liste leer, wird kein Block
  angehaengt (kein Kontext-Ballast).
- Der Block wird zusaetzlich gegen eine harte Zeichenobergrenze gekappt (Schutz vor Prompt-Ueberlauf),
  analog zum 6000-Zeichen-Cap des Bibliothekars.

### K3 — Prägnanz-Prinzip (Formulierung der Regeln)

Regeln muessen kurz, eindeutig und imperativ sein — **so wenig Text wie moeglich, so viel wie noetig**,
damit sie sicher befolgt werden. Keine Beispiele, keine Umschreibungen. Grund: Jede Regel steckt in
JEDEM Prompt; ausschweifende Regeln verbrauchen Kontext und senken die Befolgungsqualitaet aller Regeln
(Context Rot).

Umsetzung:
- Wenn der Agent aus Franks Aussage eine Regel formuliert, erhaelt der Erzeugungs-Prompt die Auflage:
  "Formuliere die Regel als eine praezise, imperative Anweisung in maximal 1-2 kurzen Saetzen. Keine
  Beispiele, keine Begruendung, keine Fuellwoerter."
- Weiche Zeichenobergrenze pro Regel (~240 Zeichen). Beim Ueberschreiten formuliert der Agent knapper,
  bevor er die Bestaetigungskarte zeigt.
- Die Karte zeigt Frank immer die finale Kurzfassung — er kann sie ueber "Bearbeiten" noch straffen.

### K4 — Zwei Agent-Tools (agent/app.py, build_agent_tools)

- **`lies_regeln()`** — Handler ruft `load_rules()`, gibt aktive Regeln (Titel + Text) zurueck. Damit
  kann der Agent auf Nachfrage sagen, welche Regeln er befolgt, und Dubletten vermeiden.
- **`schreibe_regel(text)`** — Handler legt **nicht** direkt an, sondern erzeugt ein Outcome mit
  `action="rule_confirm"` und `pending={"mode":"rule_confirm","rule_text":<kurzgefasster text>}` und die
  drei Buttons (siehe K5). Tool-Beschreibung grenzt klar gegen Gedaechtnis-Speicherung ab.

### K5 — Ja/Nein/Bearbeiten-Dialog im Chat

Neuer `pending`-Modus `rule_confirm` auf dem bestehenden `pending`+`options`-Muster.

**Backend (agent/app.py):**
- Vorschlag: Outcome `action="rule_confirm"`, `reply`=Regeltext, `options=[{"label":"Ja","send":"regel ja"},
  {"label":"Nein","send":"regel nein"},{"label":"Bearbeiten","send":"regel bearbeiten"}]`,
  `pending={"mode":"rule_confirm","rule_text":...}`.
- Verarbeitung (neuer Block analog Z. 5293ff.), deterministische Regex-Erkennung der Button-Woerter
  (Vorbild `\bersetz` Z. 5298), damit nicht das LLM raten muss:
  - `regel ja` / bestaetigend -> `save_rules(append)` -> `action="rule_saved"`, `pending=None`.
  - `regel nein` / ablehnend -> `action="rule_cancelled"`, `pending=None`.
  - `regel bearbeiten` -> Karte bleibt, Frontend schaltet in Editiermodus (kein Server-Roundtrip noetig).
  - `regel speichern: <neuer text>` -> `pending["rule_text"]` wird aktualisiert, Karte erneut mit
    Ja/Nein gerendert (`action="rule_confirm"`).
- **Feld-Erweiterung**: `ChatReq` (agent/app.py ~Z. 3877) erhaelt optionales `rule_text`; der
  Dashboard-Chat-Proxy (dashboard/app.py ~Z. 926) reicht es durch. (Saubere Variante statt Prefix-
  Parsing; falls sich das als aufwendig zeigt, Fallback: Prefix `"regel speichern: "` parsen.)

**Frontend (dashboard/static/index.html):**
- In `sendChat()` (~Z. 3985) Zweig `if(r.action==="rule_confirm") ruleCard(r.reply, r.options)` neben dem
  bestehenden `chatOptions`.
- Neue Funktion `ruleCard(text, options)` (analog `chatOptions` ~Z. 3774):
  - **Ja** / **Nein**: wie bestehende Buttons (`chatText.value=o.send; sendChat()`).
  - **Bearbeiten**: ersetzt die Text-Bubble durch ein `<textarea>` mit dem Regeltext; der Knopf wird zu
    **Speichern**. Klick auf Speichern: liest die textarea, zeigt den neuen Text als Bubble, blendet
    Ja/Nein wieder ein und sendet den geaenderten Text ans Backend (via `rule_text`-Feld bzw. Prefix).
- Karten sind ephemer (wie der Speicher-Flow) — nach Reload weg. Akzeptiert, da der Bestaetigungsschritt
  kurzlebig ist.

### K6 — Auslöser (Verhalten)

Der Agent schlaegt eine Regel **nur** vor, wenn Frank es verlangt oder klar eine dauerhafte
Verhaltensanweisung ausspricht ("mach daraus eine Regel", "ab jetzt immer …"). **Kein** proaktives
Regel-Erfinden bei beliebigem Feedback (sonst dieselbe Nerv-Dynamik wie beim Speicher-Nachfragen).

### K7 — Dashboard-Chronik (Einstellungen, "voll wie Bibliothekar")

Neue `<div class="card set-section">` in `#view-settings` (dashboard/static/index.html), Vorbild die
Bibliothekar-Karte (Z. 1152-1160). Enthaelt:
- `renderChatRules(list)` (Kopie von `renderLibLearned`, Z. 4752-4826): pro Regel Titel/Text +
  Erstelldatum (`created_at.slice(0,10)`), **An/Aus-Toggle** (`.lib-sw`), **Bearbeiten** (`.lib-own`
  Textarea + Speichern), **Loeschen** (`.lib-trash`). CSS-Klassen wiederverwenden, kein neues CSS.
- Feld zum **manuellen Hinzufuegen** einer Regel (Textarea + "Regel hinzufuegen"-Button).
- Laden beim Oeffnen des Einstellungen-Tabs (`loadSettings()`), ein GET-Aufruf, dann `renderChatRules`.

### K8 — Backend-Endpunkte (agent/app.py) + Proxy

Neue Routen im Agent-Dienst (Vorbild Librarian `POST/PUT/DELETE /learn`):
- `GET /rules` -> `{rules: load_rules()}`
- `POST /rules` `{text}` -> Regel anlegen (KI-Titel erzeugen, Limit pruefen), gibt neue Regel zurueck.
- `PUT /rules/{id}` `{enabled?, text?}` -> aktivieren/deaktivieren oder Text aendern.
- `DELETE /rules/{id}` -> loeschen.

Dashboard: entweder nativer Proxy `/api/rules/*` in dashboard/app.py (Muster `/api/config` ~Z. 576) an
den Agent-Dienst, oder Wiederverwendung des bestehenden Chat-Proxy-Backends. Whitelist/Route ergaenzen.

### K9 — Beobachtbarkeit & Version

- Turn-Logbuch protokolliert: (a) dass und wie viele Regeln in den Prompt injiziert wurden (Nachweis der
  Wirkung), (b) Anlegen/Aktivieren/Deaktivieren/Loeschen einer Regel.
- Agent-Dienst-Version hochzaehlen und sichtbar (Startlog + Dashboard-Anzeige, wie im Projekt ueblich).

## Datenfluss (Happy Path)

1. Frank: "Antworte ab jetzt immer als TTS-freundlicher Fliesstext, mach daraus eine Regel."
2. Agent erkennt Verhaltensanweisung -> ruft `schreibe_regel(...)` -> formuliert kurze Regel ->
   `rule_confirm`-Karte mit Regeltext + Ja/Nein/Bearbeiten.
3. Frank klickt **Ja** -> `save_rules(append)` -> Regel aktiv, Bestaetigung im Chat.
4. Ab dem naechsten Turn haengt `build_toolagent_system()` die Regel in jeden System-Prompt.
5. Chronik unter Einstellungen zeigt die Regel mit Datum, An/Aus, Bearbeiten, Loeschen.

## Fehlerbehandlung

- Kaputte/fehlende `agent-rules.json` -> leere Liste, Dienst laeuft normal weiter.
- Regel-Limit erreicht -> klare Ablehnung, keine stille Verwerfung.
- Injektions-Block ueber Zeichen-Cap -> Kappung mit Logeintrag (Funktionalitaet bleibt, nur aeltere
  Regeln fallen weg — daher Limit niedrig halten).
- Button-Woerter werden deterministisch erkannt; faellt die Erkennung aus, uebernimmt der bestehende
  LLM-Router (kein Hard-Fail).

## Testkriterien

- Regel per Chat anlegen (Ja) -> erscheint in `agent-rules.json` und in der Chronik.
- Bearbeiten-Pfad: Text im Chat aendern -> geaenderter Text landet in der Regel.
- Nein -> nichts wird gespeichert.
- Neue Chat-Session -> Regel wirkt nachweisbar (z.B. Formatierungsregel wird befolgt; Logbuch zeigt
  Injektion).
- Chronik: An/Aus schaltet Wirkung, Loeschen entfernt die Regel, manuelles Hinzufuegen funktioniert.
- Gedaechtnis-Abgrenzung: eine reine Faktenaussage loest KEINE Regelkarte aus.

## Betroffene Dateien

- `second-brain-server/agent/app.py` — Regel-Ablage, `build_toolagent_system()`, 2 Tools,
  `rule_confirm`-Flow, `ChatReq.rule_text`, 4 Endpunkte, Logbuch, Version.
- `second-brain-server/dashboard/app.py` — Proxy `/api/rules/*` (bzw. Whitelist) + `rule_text` im
  Chat-Proxy durchreichen.
- `second-brain-server/dashboard/static/index.html` — Chronik-Karte + `renderChatRules` + `ruleCard`.
- `second-brain-server/CORTEX-AGENT-UMBAU-PLAN.md` — To-Do #4 nach Umsetzung als erledigt markieren.

## Offene Punkte (in der Umsetzungsplanung zu entscheiden)

- `rule_text`-Feld sauber durchreichen (bevorzugt) vs. Prefix-Parsing (Fallback).
- Proxy nativ in dashboard/app.py vs. bestehendes Chat-Backend nutzen.
- KI-Titel fuer Regeln: eigener kleiner Prompt (Vorbild `_gen_rule_title`) oder erste ~40 Zeichen.
