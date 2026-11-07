---
name: bash-commit-heredoc-syntax
description: "Im Bash-Tool NIE PowerShell-Heredoc @'...'@ für git commit -m nutzen — landet als literales @ im Titel"
metadata: 
  node_type: memory
  type: feedback
  originSessionId: 9fd40b29-2cd9-4d33-a8a8-8c92992cac2c
  modified: 2026-11-07T13:54:57.323Z
---

Im **Bash-Tool** (Git Bash) für mehrzeilige Commit-Messages NIE die PowerShell-Here-String-Syntax `git commit -m @'...'@`: `@` ist dort kein Sonderzeichen und landet als **literales `@ ` am Anfang des Commit-Titels**.

**Why:** Zweimal passiert (Commits `9f1cfe1a7` und `7106e5a1b`), weil PowerShell- und Bash-Tool unterschiedliche Heredoc-Syntax haben.

**How to apply:** Im Bash-Tool immer `git commit -F-` mit echtem Shell-Heredoc:
```
git commit -F- <<'EOF'
Titelzeile

Body ...
EOF
```
Im **PowerShell-Tool** ist `@'...'@` korrekt. Syntax am aktiven Tool ausrichten. Verwandt: [[repo-branch-und-sync-setup]] (Force-Push auf shared main vermeiden).
