---
name: direktiven-recherche
description: >
  Startet eine tiefe Internet-Recherche zu den drei Hauptdirektiven (Superintelligenz,
  Selbstbeobachtung, Resilient Bugfixing). Liest die vollstaendigen Direktiven-Texte,
  spawnt 5 parallele Researcher-Agenten, gleicht Ergebnisse gegen den Ist-Zustand ab
  und liefert nur NEUE oder VERBESSERBARE Vorschlaege.
  Deutsche Trigger: "Direktiven recherchieren", "recherchiere die Direktiven",
  "Direktiven-Recherche", "wie kann ich die Direktiven besser umsetzen",
  "Forschung zu den Direktiven", "suche nach Verbesserungen fuer die Direktiven",
  "Direktiven-Agent starten", "starte den Direktiven-Skill", "Direktiven-Forschung",
  "was gibt es Neues fuer die Direktiven", "Direktiven aktualisieren".
invocation: user
---

# Direktiven-Recherche starten

Starte den `direktiven-recherche` Agenten mit dem Agent-Tool:

```
Agent-Tool aufrufen:
  subagent_type: "general-purpose"
  name: "direktiven-recherche"
  model: "opus"
  prompt: [Den vollstaendigen Agenten-Prompt aus ~/.claude/agents/direktiven-recherche.md lesen und als Prompt uebergeben]
```

**WICHTIG:** Der Agent ist ein Custom Agent in `~/.claude/agents/direktiven-recherche.md`.
Custom Agents werden mit `subagent_type: "general-purpose"` gestartet — NICHT mit
`subagent_type: "direktiven-recherche"` (das wuerde fehlschlagen).

**Ablauf:**
1. Lies `~/.claude/agents/direktiven-recherche.md` komplett ein
2. Starte den Agenten mit dem Text als Prompt
3. Der Agent liest selbststaendig die drei Direktiven, den Ist-Zustand, recherchiert im Internet
4. Ergebnis landet in `~/proggs/DIREKTIVEN-RECHERCHE-[DATUM].md`

**Erwartete Dauer:** 5-10 Minuten (5 parallele Researcher)

**Der Benutzer will:**
- Nur NEUE Vorschlaege sehen (nichts was schon implementiert ist)
- Ausfuehrliche Erklaerungen auf Deutsch (kein Programmierer)
- Konkrete Umsetzungsvorschlaege (nicht "man koennte")
- Aktuelle Quellen (2025-2026 bevorzugt)
