# Hook-Registrierung — neuer/erweiterter Almanach + Best-Practices in die 3 Almanach-Hooks

> Detail-Anleitung zu **Schritt 9** des `research`-Skills. Wird ausgefuehrt, NACHDEM ein
> Almanach-Bereich (`bugs/<kategorie>/<bereich>.md`) und/oder seine Best-Practices-Gegenseite
> (`best-practices/<kategorie>/<bereich>.md`) durch die Recherche NEU entstanden oder erweitert
> wurde. Wer persistiert (Schritt 8), registriert auch in den Hooks.
>
> **Warum ueberhaupt:** Das Bug-Almanach-System hat drei Schutz-Hooks. Ein neuer Almanach nuetzt
> nur, wenn diese Hooks ihn auch kennen. Sonst entsteht totes Wissen: die Datei liegt im Repo,
> aber kein Hook blendet sie ein, blockiert passende Edits oder erkennt sie im Prompt — der
> Compound-Intelligence-Effekt bleibt aus.

## Die drei Hooks — was bei jedem zu tun ist

| Hook | Event | Mechanik | Aktion bei neuem Bereich |
|------|-------|----------|--------------------------|
| `bug-almanac-index` | SessionStart | listet **rekursiv** alle `bugs/**/*.md` | **Automatisch** — nur verifizieren, dass die Datei in `bugs/<kategorie>/` liegt (kein `/`-loser Top-Level). Keine Code-Aenderung. |
| `bug-almanac-hint.py` | UserPromptSubmit | **kuratierte `AREAS`-Dict** (praezise Stichwoerter) + dynamischer Dateinamen-Fallback | **Kuratierten Eintrag hinzufuegen** mit Synonym-Stichwoertern — sonst triggert nur die exakte Dateinamen-Phrase. |
| `bug-almanac-guard` | PreToolUse (Edit/Write) | **Datei-Muster → Bereich** | Nur wenn der Bereich ein **klares Datei-Signal** hat (z.B. `.kt`, `manifest.json`). Konzept-Bereiche ohne Datei-Muster (wie `agents/…`) werden bewusst NICHT erzwungen — dann nur dokumentieren. |

## Ablauf (Schritt fuer Schritt)

### 1. Index (verifizieren, meist nichts zu tun)
Der Index ist rekursiv — eine Datei in `bugs/<kategorie>/<bereich>.md` erscheint ab der naechsten
Session automatisch. Nur pruefen, dass sie nicht faelschlich direkt in `bugs/` liegt.

### 2. Hint — kuratierten AREAS-Eintrag hinzufuegen (der Kern-Schritt)
Datei: `~/.claude/hooks/bug-almanac-hint.py`. Im `AREAS`-Dict eine Zeile ergaenzen. Schema:
```python
    "<kategorie>/<bereich>":  ("<Anzeigename>", ["stichwort1", "stichwort-2", ...]),
```
- **Key** = Almanach-Relpfad ab `bugs/` OHNE `.md` (gleiche Form wie die anderen Keys).
- **Stichwoerter** lowercase, Mehrwort/eindeutig. **Substring-Matching** (`k in low`) — darum:
  - **Leerzeichen- UND Bindestrich-Variante** beider Schreibweisen aufnehmen
    (`"ralph loop"` UND `"ralph-loop"`), weil deutsche Eingaben oft Bindestriche nutzen.
  - **Kollisionen vermeiden:** keine Stichwoerter, die schon einem anderen Bereich gehoeren
    (vorher pruefen, ob das Wort dort steht). Doppel-Treffer sind unschoen, aber nicht fatal.
  - Zu kurze/generische Woerter (<4 Zeichen) meiden — der Fallback filtert die ohnehin.
- **Testen** (Pflicht, beide Schreibweisen + ein Negativ-Fall):
  ```bash
  python3 -m py_compile ~/.claude/hooks/bug-almanac-hint.py && echo OK
  echo '{"prompt":"<synonym mit Bindestrich>","session_id":"t1"}' | python3 ~/.claude/hooks/bug-almanac-hint.py
  echo '{"prompt":"<synonym mit Leerzeichen>","session_id":"t2"}' | python3 ~/.claude/hooks/bug-almanac-hint.py
  echo '{"prompt":"voellig irrelevant","session_id":"t3"}' | python3 ~/.claude/hooks/bug-almanac-hint.py  # muss LEER sein
  ```
  Jeder Treffer gibt das `hookSpecificOutput`-JSON aus; der Negativ-Fall gibt nichts.

### 3. Guard — nur bei klarem Datei-Signal
Datei: `~/.claude/hooks/bug-almanac-guard.{ps1,sh}` (beide Varianten). Der Guard ordnet Edits ueber
**Datei-Muster** einem Bereich zu. Nur erweitern, wenn der neue Bereich eindeutige Dateien hat,
die NICHT schon einem anderen Bereich gehoeren. Konzept-/Querschnitts-Bereiche (`agents/…`,
viele `apis/…`) haben kein sauberes Muster → **bewusst nicht erzwingen** (der `hint`-Trigger +
Index decken sie ab). Diese Entscheidung im Persistenz-Bericht kurz festhalten
(„Konzept-Bereich, Guard nicht anwendbar — wie orchestrator-agent").

### 4. Spiegeln + committen (harness-mirror-on-change-Pflicht)
Jede geaenderte Hook-Datei in BEIDE Spiegel-Orte kopieren und 1:1 verifizieren:
```bash
cp ~/.claude/hooks/bug-almanac-hint.py ~/proggs/claude-code-setup/hooks/bug-almanac-hint.py
cp ~/.claude/hooks/bug-almanac-hint.py ~/proggs/Umgebung/Hooks/bug-almanac-hint.py
diff ~/.claude/hooks/bug-almanac-hint.py ~/proggs/claude-code-setup/hooks/bug-almanac-hint.py   # leer = identisch
diff ~/.claude/hooks/bug-almanac-hint.py ~/proggs/Umgebung/Hooks/bug-almanac-hint.py
```
- Bei `.ps1`/`.sh`-Aenderungen (Guard) BEIDE Varianten spiegeln. `__pycache__` NIE mitnehmen. LF + UTF-8 ohne BOM.
- Dann nur die eigenen Dateien namentlich stagen, `#NNN`-Commit, fetch+rebase, push.

## Wichtig: Hooks editieren = Harness-Arbeit
Vor dem Editieren von `~/.claude/hooks/*` blockiert der `bug-almanac-guard` selbst, bis der
Hooks-Almanach (Stufe C → Volltext) + Best-Practices gelesen sind. Das ist gewollt — kurz lesen,
dann editieren. Der Guard kann den Edit auch dem Bereich `server/ai-agent-frameworks` zuordnen,
wenn die `AREAS`-Stichwoerter Agent/Loop-Vokabular enthalten; dann zusaetzlich dessen Kurzcheck lesen.

## Checkliste vor „Hook-Registrierung erledigt"
- [ ] Index: Datei liegt in `bugs/<kategorie>/` (rekursiv erfasst) — verifiziert.
- [ ] Hint: kuratierter `AREAS`-Eintrag mit Leerzeichen- UND Bindestrich-Synonymen; `py_compile` OK;
      Positiv-Tests (beide Schreibweisen) triggern; Negativ-Test leer.
- [ ] Guard: Datei-Muster ergaenzt (bei klarem Signal) ODER bewusst als Konzept-Bereich dokumentiert.
- [ ] Alle geaenderten Hook-Dateien in `claude-code-setup/hooks` UND `Umgebung/Hooks` gespiegelt (1:1).
- [ ] Committet + gepusht (nur eigene Dateien, fetch+rebase vor Push).
