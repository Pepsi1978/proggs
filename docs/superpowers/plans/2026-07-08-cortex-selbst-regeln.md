# Cortex Selbst-Regeln Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Der Cortex-Chat-Agent kann sich dauerhafte Verhaltensregeln geben (Ja/Nein/Bearbeiten-Dialog im Chat), die in jedem kuenftigen Gespraech wirken, extern auf Laufwerk Z gespeichert und im Dashboard verwaltbar sind.

**Architecture:** Neue reine Logik in `agent/rules.py` (Ablage, Regelblock, Trigger-Erkennung) — unit-testbar. `agent/app.py` bindet sie ein: zwei Tools (`lies_regeln`/`schreibe_regel`), ein `rule_confirm`-Bestaetigungs-Flow auf dem bestehenden `pending`+`options`-Muster, Prompt-Injektion in `build_toolagent_system()`, vier REST-Endpunkte. Dashboard bekommt einen Proxy und eine Chronik-Karte (Vorbild: Bibliothekar-"Gelernte Regeln"). Regeldatei liegt auf `Z:\Logbuch\Regeln\regeln.json` (im Agent-Container `/logbook/Regeln/regeln.json`, Mount existiert bereits).

**Tech Stack:** Python 3 / FastAPI (agent + dashboard Dienste, Docker auf VPS), Vanilla-JS Single-Page-Dashboard, JSON-Dateiablage auf Samba-Volume, pytest fuer Unit-Tests.

## Global Constraints

- Regeldatei-Pfad im Agent-Container: `/logbook/Regeln/regeln.json` (= `Z:\Logbuch\Regeln\` = VPS `/srv/samba/gedanken/Logbuch/Regeln/`). Kein neuer compose-Mount.
- Regel-Objekt-Format exakt: `{ "id": <10-hex>, "text": str, "titel": str, "enabled": bool, "created_at": <ISO-8601 sekundengenau> }`.
- Max. 40 Regeln. Ueberschreiten -> Anlegen ablehnen mit klarer Meldung, kein stilles Verwerfen.
- Regelblock in den System-Prompt auf max. 6000 Zeichen kappen (Context-Rot-Schutz), nur `enabled`-Regeln, leere Liste -> kein Block.
- Regel-Formulierung praegnant: 1-2 kurze imperative Saetze, keine Beispiele, weiche Obergrenze ~240 Zeichen pro Regeltext.
- Atomares Schreiben: tmp-Datei im SELBEN Ordner + `os.replace` (Samba-Mount-tauglich), `threading.Lock`, fehlertolerant (kaputte/fehlende Datei -> leere Liste, Dienst stirbt nie).
- Deutsche Umlaute in allen UI-Strings und Nutzer-Meldungen (ae/oe/ue/ss verboten). Code-Kommentare/Commits englisch.
- Version des Agent-Diensts bei Abschluss hochzaehlen und sichtbar (Startlog + Dashboard).
- Commit+Push nach jeder Task (fortlaufende `#NNNN`-Nummer, nur eigene Dateien namentlich, fetch+rebase vor Push). Deploy zum VPS ausschliesslich ueber `scripts/cortex-deploy.sh` (Deploy-Guard).

---

## File Structure

- **Create** `second-brain-server/agent/rules.py` — reine Regel-Logik: Ablage (load/save/add/update/delete), Regelblock-Bildung, Trigger-Erkennung Regel-vs-Speichern. Keine FastAPI-Abhaengigkeit.
- **Create** `second-brain-server/tests/rules_test.py` — Unit-Tests fuer `rules.py`.
- **Modify** `second-brain-server/agent/app.py` — importiert `rules`, Prompt-Injektion, 2 Tools, `rule_confirm`-Flow, `ChatReq.rule_text`, 4 Endpunkte, Logbuch-Sonden, Version.
- **Modify** `second-brain-server/dashboard/app.py` — Proxy `/api/rules/*` an den Agent-Dienst + `rule_text` im Chat-Proxy durchreichen.
- **Modify** `second-brain-server/dashboard/static/index.html` — Chronik-Karte (`renderChatRules`, manuelles Hinzufuegen) + `ruleCard`-Dialog im Chat.
- **Modify** `second-brain-server/CORTEX-AGENT-UMBAU-PLAN.md` — To-Do #4 (Regelpool) als erledigt markieren.

---

### Task 1: Regel-Ablage in `agent/rules.py`

Reine, testbare Kern-Logik fuer Speicherung. Vorbild: `librarian/app.py` `load_learned`/`save_learned` (Z. 278-285) + `_read_json`/`_write_json` (Z. 169-183).

**Files:**
- Create: `second-brain-server/agent/rules.py`
- Test: `second-brain-server/tests/rules_test.py`

**Interfaces:**
- Produces:
  - `rules_path() -> str` — Pfad der Regeldatei (aus Env `LOGBOOK_DIR`, Default `/logbook`, + `Regeln/regeln.json`).
  - `load_rules(path: str) -> list[dict]` — liest Liste, fehlertolerant (fehlt/kaputt -> `[]`).
  - `save_rules(path: str, rules: list[dict]) -> None` — atomar (tmp im selben Ordner + `os.replace`), legt Ordner an, `threading.Lock`.
  - `add_rule(path: str, text: str, titel: str, now_iso: str) -> dict` — haengt Regel an, wirft `RuleLimitError` bei >40. Gibt die neue Regel zurueck.
  - `update_rule(path, rule_id, *, enabled=None, text=None) -> dict|None` — aendert Felder, gibt aktualisierte Regel oder `None`.
  - `delete_rule(path, rule_id) -> bool` — entfernt, gibt Erfolg.
  - `MAX_RULES = 40`, `class RuleLimitError(Exception)`.
  - `new_rule_id() -> str` — `uuid.uuid4().hex[:10]`.

- [ ] **Step 1: Write the failing tests**

```python
# second-brain-server/tests/rules_test.py
import os, json, pytest
from agent import rules

def test_load_missing_file_returns_empty(tmp_path):
    assert rules.load_rules(str(tmp_path / "nope.json")) == []

def test_save_and_load_roundtrip(tmp_path):
    p = str(tmp_path / "Regeln" / "regeln.json")
    r = {"id": "abc1234567", "text": "Antworte kurz.", "titel": "Kurz",
         "enabled": True, "created_at": "2026-07-08T21:00:00"}
    rules.save_rules(p, [r])
    assert rules.load_rules(p) == [r]

def test_load_corrupt_file_returns_empty(tmp_path):
    p = tmp_path / "regeln.json"
    p.write_text("{ this is not json", encoding="utf-8")
    assert rules.load_rules(str(p)) == []

def test_add_rule_appends_and_returns(tmp_path):
    p = str(tmp_path / "regeln.json")
    new = rules.add_rule(p, "Antworte als Fliesstext.", "Fliesstext", "2026-07-08T21:00:00")
    assert new["text"] == "Antworte als Fliesstext."
    assert new["titel"] == "Fliesstext"
    assert new["enabled"] is True
    assert len(new["id"]) == 10
    assert rules.load_rules(p)[0]["id"] == new["id"]

def test_add_rule_enforces_limit(tmp_path):
    p = str(tmp_path / "regeln.json")
    for i in range(rules.MAX_RULES):
        rules.add_rule(p, f"Regel {i}", f"T{i}", "2026-07-08T21:00:00")
    with pytest.raises(rules.RuleLimitError):
        rules.add_rule(p, "eine zu viel", "X", "2026-07-08T21:00:00")

def test_update_rule_toggle_enabled(tmp_path):
    p = str(tmp_path / "regeln.json")
    new = rules.add_rule(p, "Regel A", "A", "2026-07-08T21:00:00")
    upd = rules.update_rule(p, new["id"], enabled=False)
    assert upd["enabled"] is False
    assert rules.load_rules(p)[0]["enabled"] is False

def test_update_rule_unknown_id_returns_none(tmp_path):
    p = str(tmp_path / "regeln.json")
    assert rules.update_rule(p, "does-not-ex", enabled=False) is None

def test_delete_rule(tmp_path):
    p = str(tmp_path / "regeln.json")
    new = rules.add_rule(p, "Regel A", "A", "2026-07-08T21:00:00")
    assert rules.delete_rule(p, new["id"]) is True
    assert rules.load_rules(p) == []
    assert rules.delete_rule(p, new["id"]) is False
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd second-brain-server && python -m pytest tests/rules_test.py -v`
Expected: FAIL (module `agent.rules` not found / functions missing). Falls `agent/__init__.py` fehlt, im selben Schritt eine leere `second-brain-server/agent/__init__.py` anlegen, damit `from agent import rules` importierbar ist (nur falls noetig — zuerst pruefen, ob agent bereits als Package importierbar ist).

- [ ] **Step 3: Write minimal implementation**

```python
# second-brain-server/agent/rules.py
"""Persistent self-authored behaviour rules for the Cortex chat agent.

Pure logic, no FastAPI import, so it is unit-testable in isolation.
Storage lives on Frank's Z: drive (Samba) at /logbook/Regeln/regeln.json.
"""
import os, json, uuid, threading

MAX_RULES = 40
_LOCK = threading.Lock()


class RuleLimitError(Exception):
    """Raised when adding a rule would exceed MAX_RULES."""


def rules_path() -> str:
    base = os.getenv("LOGBOOK_DIR", "/logbook")
    return os.path.join(base, "Regeln", "regeln.json")


def new_rule_id() -> str:
    return uuid.uuid4().hex[:10]


def load_rules(path: str) -> list:
    try:
        with open(path, "r", encoding="utf-8") as f:
            data = json.load(f)
        return data if isinstance(data, list) else []
    except (FileNotFoundError, json.JSONDecodeError, OSError):
        return []


def save_rules(path: str, rules: list) -> None:
    with _LOCK:
        folder = os.path.dirname(path)
        os.makedirs(folder, exist_ok=True)
        tmp = os.path.join(folder, f".regeln.{uuid.uuid4().hex}.tmp")
        with open(tmp, "w", encoding="utf-8") as f:
            json.dump(rules, f, ensure_ascii=False, indent=2)
        os.replace(tmp, path)


def add_rule(path: str, text: str, titel: str, now_iso: str) -> dict:
    rules = load_rules(path)
    if len(rules) >= MAX_RULES:
        raise RuleLimitError(f"Regel-Limit erreicht ({MAX_RULES}).")
    rule = {"id": new_rule_id(), "text": text.strip(), "titel": titel.strip(),
            "enabled": True, "created_at": now_iso}
    rules.append(rule)
    save_rules(path, rules)
    return rule


def update_rule(path: str, rule_id: str, *, enabled=None, text=None):
    rules = load_rules(path)
    hit = None
    for r in rules:
        if r.get("id") == rule_id:
            if enabled is not None:
                r["enabled"] = bool(enabled)
            if text is not None:
                r["text"] = text.strip()
            hit = r
            break
    if hit is None:
        return None
    save_rules(path, rules)
    return hit


def delete_rule(path: str, rule_id: str) -> bool:
    rules = load_rules(path)
    kept = [r for r in rules if r.get("id") != rule_id]
    if len(kept) == len(rules):
        return False
    save_rules(path, kept)
    return True
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd second-brain-server && python -m pytest tests/rules_test.py -v`
Expected: PASS (8 tests).

- [ ] **Step 5: Commit**

```bash
git -C ~/proggs add second-brain-server/agent/rules.py second-brain-server/tests/rules_test.py
# ggf. second-brain-server/agent/__init__.py mit anhaengen, falls neu erstellt
git -C ~/proggs commit -m "#NNNN - Cortex rules: persistent rule storage module + tests" -- second-brain-server/agent/rules.py second-brain-server/tests/rules_test.py
git -C ~/proggs fetch origin && git -C ~/proggs rebase origin/main && git -C ~/proggs push
```

---

### Task 2: Regelblock-Bildung (Prompt-Injektion-Logik)

Reine Funktion, die aus aktiven Regeln den Pflichtblock fuer den System-Prompt baut. Vorbild: `librarian` `_learned_block()` (Z. 288).

**Files:**
- Modify: `second-brain-server/agent/rules.py`
- Test: `second-brain-server/tests/rules_test.py`

**Interfaces:**
- Produces: `rules_block(rules: list[dict], max_chars: int = 6000) -> str` — gibt Textblock aus `enabled`-Regeln oder `""` bei keiner aktiven Regel; gekappt auf `max_chars`.

- [ ] **Step 1: Write the failing tests**

```python
# append to tests/rules_test.py
def test_rules_block_empty_when_no_active():
    assert rules.rules_block([]) == ""
    assert rules.rules_block([{"text": "X", "enabled": False}]) == ""

def test_rules_block_lists_active_rules():
    block = rules.rules_block([
        {"text": "Antworte als Fliesstext.", "enabled": True},
        {"text": "Sei knapp.", "enabled": True},
        {"text": "Ignoriere mich.", "enabled": False},
    ])
    assert "Antworte als Fliesstext." in block
    assert "Sei knapp." in block
    assert "Ignoriere mich." not in block
    assert "DAUERHAFTE REGELN" in block

def test_rules_block_caps_length():
    many = [{"text": "x" * 100, "enabled": True} for _ in range(200)]
    block = rules.rules_block(many, max_chars=500)
    assert len(block) <= 500
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd second-brain-server && python -m pytest tests/rules_test.py -k rules_block -v`
Expected: FAIL (`rules_block` not defined).

- [ ] **Step 3: Write minimal implementation**

```python
# append to agent/rules.py
def rules_block(rules: list, max_chars: int = 6000) -> str:
    active = [r.get("text", "").strip() for r in rules if r.get("enabled")]
    active = [t for t in active if t]
    if not active:
        return ""
    header = "DAUERHAFTE REGELN VON FRANK (verbindlich, immer befolgen):\n"
    body = "\n".join(f"- {t}" for t in active)
    block = header + body
    return block[:max_chars]
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd second-brain-server && python -m pytest tests/rules_test.py -k rules_block -v`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git -C ~/proggs add second-brain-server/agent/rules.py second-brain-server/tests/rules_test.py
git -C ~/proggs commit -m "#NNNN - Cortex rules: system-prompt rule block builder + tests" -- second-brain-server/agent/rules.py second-brain-server/tests/rules_test.py
git -C ~/proggs fetch origin && git -C ~/proggs rebase origin/main && git -C ~/proggs push
```

---

### Task 3: Trigger-Erkennung Regel-vs-Speichern (Kern-Fix)

Deterministische Erkennung, ob eine Nachricht eine **Verhaltensregel** meint (statt eines Gedaechtnis-Eintrags). Spec K3a. Reine Funktion, gut testbar.

**Files:**
- Modify: `second-brain-server/agent/rules.py`
- Test: `second-brain-server/tests/rules_test.py`

**Interfaces:**
- Produces: `is_rule_request(text: str) -> bool` — True bei klarem Regel-Signal (explizites "Regel" + Schreib/Merk-Verb, ODER dauerhaft-verhaltensbezogenes "ab jetzt/generell/grundsaetzlich/kuenftig/in Zukunft immer …"), sonst False.

- [ ] **Step 1: Write the failing tests**

```python
# append to tests/rules_test.py
@pytest.mark.parametrize("text", [
    "Mach daraus eine Regel.",
    "Schreib das bitte in deine Regeldatei.",
    "Das ist eine dauerhafte Regel fuer dich.",
    "Merk dir das als Regel.",
    "Antworte mir ab jetzt immer als Fliesstext.",
    "Verhalte dich generell immer hoeflich.",
    "Gib dir eine Regel, dass du immer kurz antwortest.",
])
def test_is_rule_request_true(text):
    assert rules.is_rule_request(text) is True

@pytest.mark.parametrize("text", [
    "Merk dir, dass mein Zahnarzttermin am Freitag ist.",
    "Speicher das bitte ab.",
    "Leg das in meinem Gedaechtnis ab.",
    "Wie spaet ist es?",
    "Was kannst du alles?",
])
def test_is_rule_request_false(text):
    assert rules.is_rule_request(text) is False
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd second-brain-server && python -m pytest tests/rules_test.py -k is_rule_request -v`
Expected: FAIL (`is_rule_request` not defined).

- [ ] **Step 3: Write minimal implementation**

```python
# append to agent/rules.py
import re as _re

# "Regel" zusammen mit einem Schreib-/Merk-Verb, ODER dauerhaft-verhaltensbezogenes "... immer ...".
_RULE_PATTERNS = [
    _re.compile(r"\bregel(datei|n)?\b", _re.IGNORECASE),  # Regel/Regeln/Regeldatei
    _re.compile(r"\b(ab jetzt|ab sofort|von nun an|kuenftig|in zukunft|generell|grunds[ae]tzlich)\b"
                r".{0,40}\bimmer\b", _re.IGNORECASE),
    _re.compile(r"\bimmer\b.{0,40}\b(ab jetzt|ab sofort|generell|grunds[ae]tzlich)\b", _re.IGNORECASE),
]

def is_rule_request(text: str) -> bool:
    t = (text or "").strip()
    if not t:
        return False
    # Explizites Wort "Regel" ist immer ein Regel-Signal.
    if _re.search(r"\bregel(datei|n)?\b", t, _re.IGNORECASE):
        return True
    # Dauerhaft-verhaltensbezogen: "... ab jetzt/generell ... immer ..." (Reihenfolge egal).
    for pat in _RULE_PATTERNS[1:]:
        if pat.search(t):
            return True
    return False
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd second-brain-server && python -m pytest tests/rules_test.py -v`
Expected: PASS (alle Tests der Datei).

- [ ] **Step 5: Commit**

```bash
git -C ~/proggs add second-brain-server/agent/rules.py second-brain-server/tests/rules_test.py
git -C ~/proggs commit -m "#NNNN - Cortex rules: deterministic rule-vs-save trigger detection + tests" -- second-brain-server/agent/rules.py second-brain-server/tests/rules_test.py
git -C ~/proggs fetch origin && git -C ~/proggs rebase origin/main && git -C ~/proggs push
```

---

### Task 4: Prompt-Injektion in `agent/app.py`

Aktive Regeln in den System-Prompt jeder Chat-Antwort haengen.

**Files:**
- Modify: `second-brain-server/agent/app.py` (bei `TOOLAGENT_SYSTEM` ~Z. 4059 und Aufrufstelle `_toolagent_answer` ~Z. 5202-5209)

**Interfaces:**
- Consumes: `rules.rules_path`, `rules.load_rules`, `rules.rules_block` (Task 1-2).
- Produces: `build_toolagent_system() -> str` — Basis `TOOLAGENT_SYSTEM` + (falls vorhanden) Regelblock.

**Vorgehen (kein pytest — Integration in die grosse Datei, per Grep+Read+Edit, nicht per Agent):**

- [ ] **Step 1:** Grep die genaue Definition und Verwendung: `TOOLAGENT_SYSTEM` und `_toolagent_answer`. Read die betroffenen Zeilenbereiche.

```bash
grep -n "TOOLAGENT_SYSTEM" second-brain-server/agent/app.py
grep -n "_toolagent_answer" second-brain-server/agent/app.py
```

- [ ] **Step 2:** Oben in `agent/app.py` (bei den anderen lokalen Imports) `from agent import rules` bzw. `import rules` ergaenzen — passend zum bestehenden Import-Stil des Diensts (pruefen, wie andere lokale Module importiert werden; im Container laeuft app.py evtl. als Top-Level-Modul, dann `import rules`).

- [ ] **Step 3:** Direkt nach der `TOOLAGENT_SYSTEM`-Konstante eine Funktion einfuegen:

```python
def build_toolagent_system() -> str:
    """TOOLAGENT_SYSTEM plus die aktiven Selbst-Regeln von Frank."""
    try:
        block = rules.rules_block(rules.load_rules(rules.rules_path()))
    except Exception as e:  # noqa: BLE001 - Regeln duerfen den Chat nie brechen
        log.warning("rules_block failed: %s", e)
        block = ""
    if not block:
        return TOOLAGENT_SYSTEM
    return TOOLAGENT_SYSTEM + "\n\n" + block
```

(Verwende den im Dienst vorhandenen Logger; Namen per Grep bestimmen, z.B. `log`/`logger`/`_log`.)

- [ ] **Step 4:** In `_toolagent_answer` die Stelle, die `TOOLAGENT_SYSTEM` als System-Prompt verwendet, auf `build_toolagent_system()` umstellen (nur die eine Verwendung im finalen Antwort-Call; andere Vorkommen unangetastet lassen).

- [ ] **Step 5:** Syntax-Check + Commit.

```bash
python -m py_compile second-brain-server/agent/app.py
git -C ~/proggs add second-brain-server/agent/app.py
git -C ~/proggs commit -m "#NNNN - Cortex agent: inject active self-rules into tool-agent system prompt" -- second-brain-server/agent/app.py
git -C ~/proggs fetch origin && git -C ~/proggs rebase origin/main && git -C ~/proggs push
```

---

### Task 5: Zwei Agent-Tools `lies_regeln` / `schreibe_regel`

**Files:**
- Modify: `second-brain-server/agent/app.py` (`build_agent_tools()` ~Z. 4076; `tools`-Liste ~Z. 4160, `handlers`-Dict ~Z. 4193)

**Interfaces:**
- Consumes: `rules.*`, `build_toolagent_system` (Task 4), `_process_turn`-Outcome-Muster (Task 6).
- Produces: Tools `lies_regeln()` und `schreibe_regel(text)` im Agent-Tool-Set.

- [ ] **Step 1:** Read `build_agent_tools()` (Z. 4076-4195) vollstaendig, um Schema- und Handler-Muster exakt zu treffen.

- [ ] **Step 2:** Neues Tool-Schema `lies_regeln` (keine Parameter) und `schreibe_regel` (`{text: string}`) in die `tools`-Liste einfuegen. Beschreibung von `schreibe_regel` grenzt klar ab:

```
"Lege eine DAUERHAFTE Verhaltensregel an, die bestimmt, WIE du kuenftig antwortest oder dich verhaeltst "
"(z.B. Antwortformat, Ton, Vorgehen). NICHT fuer Faktenwissen oder Notizen - dafuer ist 'speichere'. "
"Formuliere die Regel praezise und imperativ in maximal 1-2 kurzen Saetzen, ohne Beispiele. "
"Die Regel wird Frank zur Bestaetigung vorgelegt (Ja/Nein/Bearbeiten), nicht sofort aktiviert."
```

Die `lies_regeln`-Beschreibung: `"Gib die aktuell aktiven Verhaltensregeln zurueck (Titel + Text), z.B. um Frank zu zeigen, welche Regeln du befolgst, oder um Dubletten zu vermeiden."`

- [ ] **Step 3:** Handler ergaenzen:

```python
def _lies_regeln(args):
    rs = [r for r in rules.load_rules(rules.rules_path()) if r.get("enabled")]
    if not rs:
        return {"regeln": [], "hinweis": "Noch keine aktiven Regeln."}
    return {"regeln": [{"titel": r.get("titel"), "text": r.get("text")} for r in rs]}

def _schreibe_regel(args):
    text = (args.get("text") or "").strip()
    if not text:
        return {"fehler": "Kein Regeltext angegeben."}
    # Loest den Bestaetigungs-Flow aus (Task 6): der Turn-Handler wandelt dies in eine rule_confirm-Karte.
    state["rule_candidate"] = text
    return {"status": "vorschlag_bereit", "regel": text}
```

(`state` ist das bestehende Turn-State-Dict aus `build_agent_tools`, ~Z. 4081.)

- [ ] **Step 4:** Handler im `handlers`-Dict registrieren (`"lies_regeln": _lies_regeln, "schreibe_regel": _schreibe_regel`).

- [ ] **Step 5:** Syntax-Check + Commit.

```bash
python -m py_compile second-brain-server/agent/app.py
git -C ~/proggs add second-brain-server/agent/app.py
git -C ~/proggs commit -m "#NNNN - Cortex agent: add lies_regeln/schreibe_regel tools" -- second-brain-server/agent/app.py
git -C ~/proggs fetch origin && git -C ~/proggs rebase origin/main && git -C ~/proggs push
```

---

### Task 6: `rule_confirm`-Bestaetigungs-Flow (Backend)

Ja/Nein/Bearbeiten auf dem bestehenden `pending`+`options`-Muster. Vorbild: `save_confirm`-Block (~Z. 5293-5386).

**Files:**
- Modify: `second-brain-server/agent/app.py` (`ChatReq` ~Z. 3877; `_process_turn` ~Z. 5241; save_confirm-Block ~Z. 5293-5386; Antwort-JSON ~Z. 5496-5501; `_new_session` ~Z. 2227)

**Interfaces:**
- Consumes: `rules.add_rule`, `state["rule_candidate"]` (Task 5), `build_agent_tools`.
- Produces: Antwort mit `action="rule_confirm"` + `options=[Ja/Nein/Bearbeiten]`; `pending={"mode":"rule_confirm","rule_text":...}`; `ChatReq.rule_text` optional.

- [ ] **Step 1:** Read `_process_turn` und den save_confirm-Block (Z. 5241-5386) sowie den HTTP-Handler-Teil, der `session["pending"]` setzt (~Z. 5449-5482).

- [ ] **Step 2:** `ChatReq` (Z. 3877) um `rule_text: str | None = None` erweitern.

- [ ] **Step 3:** Nach dem Tool-Loop: wenn `state.get("rule_candidate")` gesetzt ist und keine hoeher-prioritaere Aktion ansteht, Outcome erzeugen:

```python
cand = state.get("rule_candidate")
if cand:
    return {"reply": cand, "action": "rule_confirm",
            "options": [{"label": "Ja", "send": "regel ja"},
                        {"label": "Nein", "send": "regel nein"},
                        {"label": "Bearbeiten", "send": "regel bearbeiten"}],
            "pending": {"mode": "rule_confirm", "rule_text": cand}}
```

- [ ] **Step 4:** Am Anfang von `_process_turn` (analog save_confirm, Z. 5293) einen Block fuer ausstehende Regel-Bestaetigung einfuegen, mit deterministischer Erkennung (Vorbild `\bersetz` Z. 5298). Bei `edited_text`/`rule_text` aus dem Request Vorrang:

```python
if pending and pending.get("mode") == "rule_confirm":
    low = user_text.strip().lower()
    # Bearbeiteter Text kam per Feld ODER per "regel speichern: <text>"
    edited = (req_rule_text or "").strip()
    m = re.match(r"^\s*regel speichern:\s*(.+)$", user_text, re.IGNORECASE | re.DOTALL)
    if m and not edited:
        edited = m.group(1).strip()
    if edited:
        pending["rule_text"] = edited
        return {"reply": edited, "action": "rule_confirm",
                "options": [{"label": "Ja", "send": "regel ja"},
                            {"label": "Nein", "send": "regel nein"},
                            {"label": "Bearbeiten", "send": "regel bearbeiten"}],
                "pending": pending}
    if re.search(r"\bregel ja\b|^\s*ja\b", low):
        try:
            new = rules.add_rule(rules.rules_path(), pending["rule_text"],
                                 _gen_rule_title(pending["rule_text"]), _now_iso())
            log.info("rule added via chat: id=%s", new["id"])
            return {"reply": f"Regel uebernommen: {new['titel']}", "action": "rule_saved", "pending": None}
        except rules.RuleLimitError:
            return {"reply": "Regel-Limit (40) erreicht - bitte zuerst eine Regel loeschen.",
                    "action": "error", "pending": None}
    if re.search(r"\bregel nein\b|^\s*nein\b", low):
        return {"reply": "Alles klar, ich lege keine Regel an.", "action": "rule_cancelled", "pending": None}
    if re.search(r"\bregel bearbeiten\b", low):
        return {"reply": pending["rule_text"], "action": "rule_confirm",
                "options": [{"label": "Ja", "send": "regel ja"},
                            {"label": "Nein", "send": "regel nein"},
                            {"label": "Bearbeiten", "send": "regel bearbeiten"}],
                "pending": pending}
    # sonst: altes pending verfaellt, normale Behandlung faellt durch
```

(`_gen_rule_title` als kleinen Helfer nach Vorbild librarian `_gen_rule_title` Z. 2262 anlegen, Fallback: erste ~40 Zeichen des Texts. `_now_iso()` per Grep suchen oder als `datetime.now(TZ).isoformat(timespec="seconds")` anlegen. `req_rule_text` aus dem ChatReq in `_process_turn` durchreichen.)

- [ ] **Step 5:** Im HTTP-Handler `options` und `action` bereits vorhanden (Z. 5496-5501) — sicherstellen, dass `rule_text` aus `ChatReq` an `_process_turn` uebergeben wird und `session["pending"]` wie beim save_confirm gesetzt/geloescht wird. `_new_session` (Z. 2227) hat `pending: None` bereits.

- [ ] **Step 6:** Syntax-Check + Commit.

```bash
python -m py_compile second-brain-server/agent/app.py
git -C ~/proggs add second-brain-server/agent/app.py
git -C ~/proggs commit -m "#NNNN - Cortex agent: rule_confirm yes/no/edit flow in chat" -- second-brain-server/agent/app.py
git -C ~/proggs fetch origin && git -C ~/proggs rebase origin/main && git -C ~/proggs push
```

---

### Task 7: REST-Endpunkte im Agent-Dienst + Dashboard-Proxy

**Files:**
- Modify: `second-brain-server/agent/app.py` (neue Routen, Vorbild librarian `/learn` Z. 2290-2387)
- Modify: `second-brain-server/dashboard/app.py` (Proxy, Vorbild `/api/config` Z. 576-592 bzw. `/api/lib` Z. 1356-1379)

**Interfaces:**
- Produces (Agent): `GET /rules`, `POST /rules {text}`, `PUT /rules/{id} {enabled?,text?}`, `DELETE /rules/{id}`.
- Produces (Dashboard): `GET/POST /api/rules`, `PUT/DELETE /api/rules/{id}` -> proxied an Agent.

- [ ] **Step 1:** Agent-Routen ergaenzen (mit dem im Dienst ueblichen `Depends(require_auth)`):

```python
class RuleCreateReq(BaseModel):
    text: str

class RuleUpdateReq(BaseModel):
    enabled: bool | None = None
    text: str | None = None

@app.get("/rules", dependencies=[Depends(require_auth)])
def api_get_rules():
    return {"rules": rules.load_rules(rules.rules_path())}

@app.post("/rules", dependencies=[Depends(require_auth)])
def api_add_rule(req: RuleCreateReq):
    text = (req.text or "").strip()
    if not text:
        raise HTTPException(400, "Regeltext fehlt.")
    try:
        return rules.add_rule(rules.rules_path(), text, _gen_rule_title(text), _now_iso())
    except rules.RuleLimitError:
        raise HTTPException(409, "Regel-Limit (40) erreicht.")

@app.put("/rules/{rule_id}", dependencies=[Depends(require_auth)])
def api_update_rule(rule_id: str, req: RuleUpdateReq):
    upd = rules.update_rule(rules.rules_path(), rule_id, enabled=req.enabled, text=req.text)
    if upd is None:
        raise HTTPException(404, "Regel nicht gefunden.")
    return upd

@app.delete("/rules/{rule_id}", dependencies=[Depends(require_auth)])
def api_delete_rule(rule_id: str):
    if not rules.delete_rule(rules.rules_path(), rule_id):
        raise HTTPException(404, "Regel nicht gefunden.")
    return {"ok": True}
```

- [ ] **Step 2:** Dashboard-Proxy `/api/rules*` an den Agent-Dienst ergaenzen (Muster `/api/config`, das `_aget`/`_aput` an `BRAIN_URL`/Agent nutzt). GET/POST auf `/api/rules`, PUT/DELETE auf `/api/rules/{id}`; falls der bestehende Chat/Agent-Proxy eine Whitelist hat, `rules` dort eintragen.

- [ ] **Step 3:** `rule_text` im Chat-Proxy `/api/chat` (dashboard/app.py Z. 926-941) in die Weiterleitungs-Whitelist aufnehmen, damit der Bearbeiten-Text durchkommt.

- [ ] **Step 4:** Syntax-Check beider Dateien + Commit.

```bash
python -m py_compile second-brain-server/agent/app.py second-brain-server/dashboard/app.py
git -C ~/proggs add second-brain-server/agent/app.py second-brain-server/dashboard/app.py
git -C ~/proggs commit -m "#NNNN - Cortex: /rules REST endpoints + dashboard proxy" -- second-brain-server/agent/app.py second-brain-server/dashboard/app.py
git -C ~/proggs fetch origin && git -C ~/proggs rebase origin/main && git -C ~/proggs push
```

---

### Task 8: Dashboard-Chronik-Karte (Anzeige/Verwaltung)

**Files:**
- Modify: `second-brain-server/dashboard/static/index.html` (Karte in `#view-settings` ~ab Z. 1232; Render-Funktion nach Vorbild `renderLibLearned` Z. 4752-4826; Laden in `loadSettings`)

**Interfaces:**
- Consumes: `GET/POST/PUT/DELETE /api/rules` (Task 7); vorhandene Helfer `escapeHtml`, `apiSend`, CSS-Klassen `card set-section`, `lib-taskrow`, `lib-sw`, `lib-trash`, `lib-own`, `btn`.

- [ ] **Step 1:** Read die Bibliothekar-Karte (Z. 1152-1160) und `renderLibLearned` (Z. 4752-4826) als exakte Vorlage.

- [ ] **Step 2:** Neue `<div class="card set-section">` in `#view-settings` einfuegen mit Ueberschrift "Selbst-Regeln des Agenten", `set-desc`-Erklaerung, Container `<div id="chatRulesList"></div>` und einem Hinzufuege-Feld (`<textarea id="chatRuleNew">` + `<button id="chatRuleAdd" class="btn btn-sm">Regel hinzufuegen</button>`).

- [ ] **Step 3:** `renderChatRules(list)` als Adaption von `renderLibLearned` schreiben: pro Regel `lib-taskrow` mit Titel/Text (`b`), Meta `created_at.slice(0,10)` (`span`), `lib-sw`-Toggle (Change -> `PUT /api/rules/{id} {enabled}`), `lib-trash`-Loeschen (confirm -> `DELETE`), Bearbeiten via `lib-own`-Textarea (Speichern -> `PUT {text}`). Leerzustand: `<p class="set-desc">Noch keine Regeln.</p>`.

- [ ] **Step 4:** `loadChatRules()` (`GET /api/rules` -> `renderChatRules`) und `chatRuleAdd`-Handler (`POST /api/rules {text}` -> Feld leeren -> `loadChatRules`) schreiben; `loadChatRules()` in `loadSettings()` aufrufen.

- [ ] **Step 5:** Commit (Dashboard braucht spaeter Image-Rebuild beim Deploy — Task 10).

```bash
git -C ~/proggs add second-brain-server/dashboard/static/index.html
git -C ~/proggs commit -m "#NNNN - Cortex dashboard: self-rules chronicle card in settings" -- second-brain-server/dashboard/static/index.html
git -C ~/proggs fetch origin && git -C ~/proggs rebase origin/main && git -C ~/proggs push
```

---

### Task 9: `ruleCard`-Dialog im Chat (Frontend)

Ja/Nein/Bearbeiten-Karte im Chatverlauf. Vorbild: `chatOptions` (Z. 3774-3786), Einbindung in `sendChat` (Z. 3985).

**Files:**
- Modify: `second-brain-server/dashboard/static/index.html`

**Interfaces:**
- Consumes: Antwortfeld `action==="rule_confirm"` + `options` (Task 6); `sendChat`, `chatText`, `apiSend`.

- [ ] **Step 1:** In `sendChat` (Z. 3985) nach dem `chatOptions`-Zweig ergaenzen: `if(r.action==="rule_confirm"){ ruleCard(r.reply, r.options); } else if(r.options && r.options.length){ chatOptions(r.options); }`.

- [ ] **Step 2:** `ruleCard(text, options)` schreiben:
  - baut `.chat-opts`-Container mit den drei Buttons.
  - **Ja**/**Nein**: `chatText.value = o.send; sendChat();` (wie `chatOptions`).
  - **Bearbeiten**: ersetzt die zuletzt gerenderte Agent-Bubble durch ein `<textarea>` mit `text`; Button-Label wird "Speichern"; Klick auf Speichern liest die Textarea (`neu`), ruft `apiSend("/api/chat","POST",{text:"regel speichern", rule_text:neu, session_id:chatSession, ...})` bzw. setzt `chatText.value="regel speichern: "+neu; sendChat();` als Fallback, und rendert die zurueckkommende `rule_confirm`-Karte erneut.

- [ ] **Step 3:** Commit.

```bash
git -C ~/proggs add second-brain-server/dashboard/static/index.html
git -C ~/proggs commit -m "#NNNN - Cortex dashboard: yes/no/edit rule confirmation card in chat" -- second-brain-server/dashboard/static/index.html
git -C ~/proggs fetch origin && git -C ~/proggs rebase origin/main && git -C ~/proggs push
```

---

### Task 10: Router-/Tool-Abgrenzung, Version, Doku, Deploy + End-to-End-Verifikation

**Files:**
- Modify: `second-brain-server/agent/app.py` (Router-Prompt `DEFAULT_INSTRUCTIONS`/`ROUTER_SCHEMA` ~Z. 2244-2332: `rule`-Intent-Beschreibung + Kontrast-Beispiel; Version hochzaehlen + Startlog)
- Modify: `second-brain-server/CORTEX-AGENT-UMBAU-PLAN.md` (To-Do #4 erledigt)

- [ ] **Step 1:** Im Router-Prompt die Abgrenzung ergaenzen (Text, kein Code): eine kurze Anweisung, dass eine dauerhafte Verhaltensanweisung ("Regel", "ab jetzt immer …") ueber `schreibe_regel` laeuft und NICHT als Gedaechtnis-Speicherung; mit dem Kontrast-Beispielpaar aus Spec K3a. Zusaetzlich in `_process_turn` VOR der save-Behandlung `rules.is_rule_request(user_text)` pruefen und dann den Regel-Pfad statt des save-Pfads waehlen (deterministische Ebene).

- [ ] **Step 2:** Agent-Dienst-Version hochzaehlen (Versions-Konstante per Grep finden, z.B. `AGENT_VERSION`/`VERSION`) und beim Start ins Log schreiben; im Dashboard sichtbar (falls dort die Agent-Version angezeigt wird).

- [ ] **Step 3:** `CORTEX-AGENT-UMBAU-PLAN.md` To-Do #4 (Regelpool §3.4) als erledigt markieren (Datum via echter Uhr).

- [ ] **Step 4:** Volle Test-Suite lokal:

Run: `cd second-brain-server && python -m pytest tests/rules_test.py -v && python -m py_compile agent/app.py dashboard/app.py`
Expected: PASS + keine Syntaxfehler.

- [ ] **Step 5:** Commit + Deploy ueber den Guard:

```bash
git -C ~/proggs add second-brain-server/agent/app.py second-brain-server/CORTEX-AGENT-UMBAU-PLAN.md
git -C ~/proggs commit -m "#NNNN - Cortex: rule-vs-save router guidance, version bump, plan done" -- second-brain-server/agent/app.py second-brain-server/CORTEX-AGENT-UMBAU-PLAN.md
git -C ~/proggs fetch origin && git -C ~/proggs rebase origin/main && git -C ~/proggs push
bash second-brain-server/scripts/cortex-deploy.sh check    # Diff VPS vs git PRUEFEN
bash second-brain-server/scripts/cortex-deploy.sh deploy    # agent + dashboard (index.html -> Image-Rebuild)
```

- [ ] **Step 6: End-to-End-Verifikation (manuell im Dashboard):**
  1. Chat: "Gib dir eine Regel, dass du mir immer als Fliesstext antwortest." -> `rule_confirm`-Karte mit Ja/Nein/Bearbeiten erscheint.
  2. **Bearbeiten** -> Text aendern -> **Speichern** -> geaenderter Text steht in der Karte -> **Ja** -> "Regel uebernommen".
  3. Auf dem VPS pruefen: `cat /srv/samba/gedanken/Logbuch/Regeln/regeln.json` zeigt die Regel; auf Franks PC unter `Z:\Logbuch\Regeln\` sichtbar.
  4. Neue Chat-Session -> Regel wirkt (Antwort ist Fliesstext); Turn-Logbuch zeigt injizierten Regelblock.
  5. Kontrolltest Abgrenzung: "Merk dir, dass mein Zahnarzttermin am Freitag ist." -> KEINE Regelkarte, normaler Speicher-Flow.
  6. Dashboard -> Einstellungen -> Karte "Selbst-Regeln des Agenten": Regel mit Datum sichtbar; An/Aus schaltet Wirkung; Loeschen entfernt; manuelles Hinzufuegen legt an.

---

## Verifikations-Zusammenfassung

- **Unit (pytest):** `rules.py` — Ablage, Limit, Regelblock, Trigger-Erkennung (Tasks 1-3).
- **Syntax (py_compile):** `agent/app.py`, `dashboard/app.py` nach jeder Backend-Task.
- **Integration/manuell (nach Deploy):** rule_confirm-Chat-Flow, Z-Ablage, Prompt-Wirkung, Abgrenzung, Dashboard-Chronik (Task 10 Step 6).
