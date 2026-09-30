---
name: github-codex-mails-participating
description: "Codex-Review-Mails von notifications@github.com kommen über \"Participating\" (Grund author), nicht über Watching"
metadata:
  node_type: memory
  type: project
  originSessionId: fdcd9ae9-bbd2-480f-9e17-2aa33a35d73e
  modified: 2026-09-27T16:39:35.783Z
---

Cloud-Sitzungen öffnen PRs unter dem Konto Pepsi1978, deshalb ist Frank Autor jedes PR-Threads (Benachrichtigungsgrund `author`). Das Repo nicht zu beobachten („Unwatch“) stoppt diese Mails NICHT. Am 27.09.2026 wurde unter github.com/settings/notifications „Participating, @mentions and custom“ auf nur „on GitHub“ gestellt (E-Mail aus).

**Why:** Der erste Abschaltversuch (Repo nicht mehr beobachten) wirkte nicht, die Codex-Mails kamen weiter.
**How to apply:** Kommen trotzdem wieder PR- oder Codex-Mails, zuerst diese Einstellung prüfen; Codex-Review selbst NICHT abschalten, apk-update-cloud braucht es.
