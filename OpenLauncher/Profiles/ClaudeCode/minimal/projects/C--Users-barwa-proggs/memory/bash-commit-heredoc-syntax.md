---
name: bash-commit-heredoc-syntax
description: "Im Bash-Tool NIE PowerShell-Heredoc @'...'@ für git commit -m nutzen — landet als literales @ im Titel"
metadata: 
  node_type: memory
  type: feedback
  originSessionId: 9fd40b29-2cd9-4d33-a8a8-8c92992cac2c
---

Beim Committen über das **Bash-Tool** (Git Bash) darf für mehrzeilige Commit-Messages
NICHT die PowerShell-Here-String-Syntax `git commit -m @'...'@` verwendet werden — im
Bash-Tool ist `@` kein Sonderzeichen, sondern landet als **literales `@ ` am Anfang des
Commit-Titels**.

**Why:** In dieser Session ist der Fehler ZWEIMAL passiert (Commits `9f1cfe1a7` und
`7106e5a1b`), weil das PowerShell- und das Bash-Tool unterschiedliche Heredoc-Syntax haben.

**How to apply:** Im Bash-Tool immer `git commit -F-` mit echtem Shell-Heredoc:
```
git commit -F- <<'EOF'
Titelzeile

Body ...
EOF
```
Im **PowerShell-Tool** dagegen ist `@'...'@` korrekt. Also: Syntax am aktiven Tool
ausrichten. Verwandt: [[repo-branch-und-sync-setup]] (Force-Push auf shared main vermeiden).
