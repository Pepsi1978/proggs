---
name: anthropic-schluessel-praefix
description: "Franks gültiger Anthropic-API-Schlüssel beginnt mit sk-ant-user, nicht sk-ant-api; nur sk-ant- prüfen, nie sk-ant-api verlangen"
metadata:
  node_type: memory
  type: feedback
  originSessionId: 8f3bb927-806a-44c0-9426-c79ceb1abf75
  modified: 2026-11-07T13:55:05.326Z
---

Franks Anthropic-API-Schlüssel beginnt mit `sk-ant-user`, nicht `sk-ant-api`. Schlüsselprüfungen verlangen `sk-ant-` (damit kein fremdes Geheimnis an Anthropic geht), legen das Präfix danach nicht fest und schließen nur Nicht-API-Zugänge (`sk-ant-oat`, `-ort`, `-sid`, `-admin`) und Nicht-ASCII aus.

**Why:** Eine Prüfung auf `sk-ant-api` lehnte in Perfect Moment 0.1.134 seinen funktionierenden Schlüssel ab.

**How to apply:** Jede App mit Anthropic-Schlüsselfeld; Vorlage `istApiSchluessel` in `PerfectMoment/.../auth/AnthropicKlient.kt`.
