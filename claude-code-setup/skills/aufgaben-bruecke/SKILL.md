---
name: aufgaben-bruecke
description: Bruecke zwischen Sessions/Konten — setzt unterbrochene Arbeit nach Token-Aus oder Konto-Wechsel an genau der Stelle fort. Liest den Active-Task-Ledger (`~/proggs/.claude/agent-memory/shared/active-tasks.jsonl`), zeigt offene Aufgaben mit Status, Files und Commits, und rekonstruiert den Stand. Nutze diesen Skill IMMER und SOFORT wenn der Benutzer sagt "ich habe das Konto gewechselt mache weiter", "Konto gewechselt mache weiter mit der letzten Aufgabe", "mache mit der letzten Aufgabe weiter", "letzte Aufgabe fortsetzen", "weiter machen an der Stelle", "was war offen", "mache weiter wo wir aufgehoert haben", "Bruecke vom letzten Konto", "neues Konto weiter machen", "Token war aus mache weiter", "Tokens waren weg mache weiter", "kontinuiere die Aufgabe", "setze die Aufgabe fort", "wir machen weiter", "weiter mit dem was wir vorher hatten", "an welcher Aufgabe waren wir", "Aufgaben-Bruecke", "Brueckenagent", "neue Session weiter machen". Auch wenn der Benutzer nur sagt "Konto geaendert" oder "Konto gewechselt" und es klar ist dass eine Fortsetzung gemeint ist. NICHT triggern bei "neue Aufgabe", "neu starten", "vergiss das alte" — das ist explizit ein Neuanfang.
---

# Aufgaben-Bruecke — Skill

Dieser Skill ist die Konto-Wechsel-Bruecke. Er ist der erste Reflex wenn der
Benutzer nach Token-Aus oder Konto-Wechsel sagt "mache weiter".

## Warum dieser Skill existiert

Wenn das Token-Limit eines Kontos erreicht ist und Frank das Konto wechseln muss,
hat die neue Session NULL Kontext aus dem Chatverlauf — claude.ai-Konversationen
sind kontogebunden. Was bleibt: lokale Dateien. Der Active-Task-Ledger
(`~/proggs/.claude/agent-memory/shared/active-tasks.jsonl`) wird bei jeder
Aufgabe automatisch von drei Hooks gepflegt und ueberlebt JEDEN Konto-Wechsel.

## Ablauf — KEIN Schritt darf ausgelassen werden

### Schritt 0: Zuerst nach einem Session-Backup schauen (PFLICHT, vor dem Ledger)

Der `session`-Skill schreibt bei einem kontrollierten Schnitt (vor `/clear`) eine
**kuratierte** Handoff-Notiz. Die ist hochwertiger als der automatisch gepflegte Ledger,
weil Claude sie bewusst geschrieben hat (Ziel, Status, fehlgeschlagene Ansaetze, naechste
Schritte). Beide Systeme teilen sich die Trigger-Phrase "mache weiter wo wir waren" — also
hat das Session-Backup VORRANG, wenn es existiert.

```bash
LOCAL="$HOME/.claude/session-backup.md"
REPO="$HOME/proggs/.claude/session-backup.md"
found=""
for f in "$LOCAL" "$REPO"; do [ -s "$f" ] && found="$f" && break; done
[ -n "$found" ] && echo "SESSION-BACKUP vorhanden: $found" || echo "kein Session-Backup"
```

- **Nicht-leeres Backup gefunden:** NICHT den Ledger lesen. Stattdessen dem Benutzer sagen:
  "Es gibt ein kuratiertes Session-Backup vom <Timestamp aus Zeile 1 der Datei>. Das ist
  praeziser als der Aufgaben-Ledger — ich stelle damit wieder her." Dann den `session`-Skill
  im RESTORE-Modus uebernehmen lassen (er liest die Notiz, fasst zusammen, leert danach beide
  Backups). Dieser Skill (aufgaben-bruecke) endet hier.
- **Kein Backup vorhanden:** normal mit Schritt 1 (Ledger) weitermachen.

### Schritt 1: Ledger lesen

```bash
PYTHONIOENCODING=utf-8 python3 ~/.claude/hooks/task-ledger-helper.py resume
```

Das gibt JSON mit den letzten 10 Kandidaten zurueck (alle nicht-done, max 30 Tage alt,
sortiert nach `timestamp_last_update` absteigend).

### Schritt 2: Tabelle anzeigen

Praesentiere die Treffer als kompakte Tabelle mit den wichtigsten Spalten:

| # | Letzte Aktivitaet | Status | Aufgabe (Wortlaut-Auszug) | Files | Commits |
|---|------------------|--------|---------------------------|-------|---------|

Status-Symbol:
- `open` = 🔴 nichts gestartet
- `in_progress` = 🟡 mittendrin
- `paused` = ⏸️ Stop ohne Commit
- `committed` = 🟢 lokal committed, evtl. nicht gepusht
- `done` = ✅ committed + gepusht (taucht hier eigentlich nicht auf, weil resume nur nicht-done liefert)

Der Wortlaut-Auszug ist die erste Zeile des `prompt_text` (max 100 Zeichen).

### Schritt 3: Den Top-Kandidaten ausfuehrlich praesentieren

Standard: der oberste Eintrag der Tabelle ist der wahrscheinlich gemeinte.
Zeige fuer diesen Eintrag:

- **Voller prompt_text** des Benutzers (so wie er es damals gesagt hat)
- **Liste der `files_changed`** (Pfade)
- **Liste der `commits`** mit `git log --oneline <hash>` Ausgabe je Hash
- **`pushed`-Flag** (true/false)
- **`cwd`** in dem die Arbeit lief
- **Zeitdifferenz** zum jetzigen Zeitpunkt (z.B. "vor 2 Stunden", "gestern")

### Schritt 4: Stand rekonstruieren und Vorschlag machen

Aus den Daten ableiten was schon erledigt ist und was noch fehlt:

- `git log` der Commits zeigt was im Repo gelandet ist
- `git status` in `cwd` zeigt was noch uncommitted ist
- Die `files_changed`-Liste zeigt was beruehrt wurde

Dann ein klarer Vorschlag an den Benutzer:

```
Letzte Aufgabe vom <Datum, Zeit>:

  > "<prompt_text gekuerzt>"

Bereits erledigt:
  - <Commit-Beschreibungen aus git log>
  - <evtl. weitere Files mit Status>

Noch offen / vermutete Restarbeit:
  - <Punkte die nach Lesen des prompt_text noch zu tun sind>
  - <evtl. uncommitted Aenderungen aus git status>

Ich mache da weiter. Sag Bescheid wenn etwas anderes gemeint war —
ich kann dir auch die naechsten <N> Treffer aus dem Ledger zeigen.
```

### Schritt 5: Auf Bestaetigung warten

NICHT sofort losarbeiten. Frank kann sagen:

- "Ja, mache weiter" — dann gehts los
- "Nein, ich meinte Aufgabe 3" — anderen Treffer auswaehlen, Schritt 3+4 wiederholen
- "Zeig mir die ganze Liste" — alle 10 Kandidaten als Tabelle
- "Vergiss das, neue Aufgabe" — Skill endet, normaler Modus

## Wann der Skill versagt (graceful)

| Problem | Reaktion |
|---------|----------|
| Ledger leer / nicht vorhanden | "Ich habe keine offenen Eintraege im Ledger gefunden. Entweder ist das System frisch oder die Hooks haben nicht gefeuert. Was war die letzte Aufgabe?" |
| Ledger korrupt | "Der Ledger ist nicht lesbar. Ich oeffne `~/proggs/.claude/agent-memory/shared/active-tasks.jsonl` und repariere ihn." |
| Top-Kandidat ist alt (>7 Tage) | Trotzdem zeigen, aber explizit darauf hinweisen: "Der letzte Eintrag ist von vor X Tagen — sicher dass das noch gemeint ist?" |

## Was NIEMALS passieren darf

- ❌ Den Ledger ueberschreiben statt zu lesen
- ❌ Den Top-Kandidaten als sicher ausgewaehlt behandeln ohne Bestaetigung
- ❌ Mit der Arbeit beginnen bevor der Benutzer den vorgeschlagenen Task bestaetigt hat
- ❌ Den `prompt_text` paraphrasieren statt wortwoertlich zu zitieren (Frank muss seinen eigenen Wortlaut wiedererkennen)
- ❌ Den Skill auslassen wenn Frank Konto-Wechsel-Signal-Phrasen verwendet

## Compound Intelligence Effect

Dieser Skill arbeitet zusammen mit:

- **Skill `session`** — kuratiertes Backup/Restore. Hat VORRANG (Schritt 0): existiert ein
  nicht-leeres Session-Backup, uebernimmt `session restore` statt des Ledgers
- **Active-Task-Ledger Hooks** (`task-ledger-prompt/tool/stop`) — schreiben die Daten
- **Skill `aufgaben-visualizer`** — komplette Ledger-Uebersicht (nicht nur Resume-Kandidaten)
- **MEMORY.md** und **claude-mem Observations** — ergaenzendes Langzeitgedaechtnis
- **Direktive #2 (Selbstbeobachtung)** — der Skill IST gelebte Selbstbeobachtung ueber Sessions hinweg
