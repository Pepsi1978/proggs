# German Trigger Routing for Codex Setup

Codex environment trigger phrases:

- "starte self-improve"
- "self-improve ausfuehrlich"
- "optimiere deine Umgebung"
- "pruef dein Setup"
- "starte die Claude-Delta-Pruefung"
- "starte die Gemini-Delta-Pruefung"
- "logge diesen Umgebungsfix"
- "dokumentiere diesen Intelligenzvorschlag"

Routing rules:
- self-improvement requests go to `codex-setup/skills/self-improve/`
- Claude setup comparison requests use `codex-setup/rules/claude-delta-sync.md`
- Gemini setup comparison requests use `codex-setup/rules/gemini-delta-sync.md`
