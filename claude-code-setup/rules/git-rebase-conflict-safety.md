# Git Rebase Konflikte: NIEMALS blind --ours/--theirs (KRITISCH)

## Regel: Bei Rebase-Konflikten IMMER manuell pruefen

Bei `git rebase` ist die Semantik von `--ours` und `--theirs` **invertiert** gegenueber `git merge`:

| Kontext | `--ours` | `--theirs` |
|---------|----------|------------|
| `git merge` | Eigener Branch (lokal) | Fremder Branch (remote) |
| `git rebase` | **Upstream/Remote** (worauf rebaset wird) | **Eigener Commit** (der rebaset wird) |

### Was NIEMALS passieren darf
- ❌ `git checkout --ours` oder `--theirs` ohne die Datei vorher zu lesen
- ❌ Annehmen dass `--theirs` die Remote-Version ist (das stimmt bei Rebase NICHT!)
- ❌ Konflikte "schnell" loesen um weiterzumachen

### Pflicht-Ablauf bei Rebase-Konflikten
1. Konflikt-Datei oeffnen und LESEN (beide Versionen zwischen `<<<<<<<` und `>>>>>>>`)
2. Verstehen welche Version was ist: HEAD = Upstream, Commit-Hash = eigener Commit
3. Die RICHTIGE Version waehlen oder manuell mergen
4. `git add` + `git rebase --continue`

### Warum diese Regel existiert
Am 2026-03-26 wurde bei einem Rebase-Konflikt `git checkout --theirs` verwendet in der Annahme
es waehlt die Remote-Version. Tatsaechlich waehlte es die eigene (minimale) Version und
verwarf die reichhaltige Remote-Version mit 2700+ Zeilen (mirror-ledger.md BASELINE).
Der Fehler kostete einen Extra-Commit und Debugging-Zeit.
