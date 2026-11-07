---
name: anthropic-schluessel-praefix
description: "Franks gültiger Anthropic-API-Schlüssel beginnt mit sk-ant-user, nicht sk-ant-api; nie auf ein Präfix prüfen"
metadata:
  node_type: memory
  type: feedback
  originSessionId: 8f3bb927-806a-44c0-9426-c79ceb1abf75
  modified: 2026-11-01T21:13:32.277Z
---

Franks Anthropic-API-Schlüssel beginnt mit `sk-ant-user`, nicht mit `sk-ant-api`. Schlüsselprüfungen verlangen `sk-ant-` (damit kein fremdes Geheimnis an Anthropic geht), legen das Präfix danach aber nicht fest und schließen nur Nicht-API-Zugänge (`sk-ant-oat`, `-ort`, `-sid`, `-admin`) und Nicht-ASCII aus.

**Why:** Eine Prüfung auf `sk-ant-api` hat in Perfect Moment 0.1.134 seinen funktionierenden Schlüssel abgelehnt.

**How to apply:** Gilt für jede App mit Anthropic-Schlüsselfeld; Vorlage ist `istApiSchluessel` in `PerfectMoment/.../auth/AnthropicKlient.kt`.
