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

<!-- delegation-research-skill -->
> **Web-Recherche laeuft ueber den zentralen `research`-Skill (Delegation, seit 2026-06-21).**
> Nach Frage 1 (Policy `research-strategy.md`: Empfehlung + A/B/C/D) die Recherche NICHT selbst
> orchestrieren — den `research`-Skill laden und ihm diesen Research-Auftrag uebergeben (verlustfreie
> Bruecke; ALLE Felder ausfuellen, nichts erzaehlen):
> - **zweck:** direktive · **rueckgabe_schema:** `direktive` · **zerlegungs_modus:** `feste_liste`
> - **unterthemen[]:** die 3 Direktiven-Themen mit je einer 500-Wort-Zusammenfassung des Ist-Zustands (je 2-3 Saetze praezise — werden 1:1 an die Researcher gereicht)
> - **version_anker:** —
> - **engine:** C (Sonnet-5-Schwarm, `model:"sonnet"`) · **anzahl/wellen/cap:** 5 Researcher, Continuous-Spawning
> - **persistenz_ziel:** `DIREKTIVEN-RECHERCHE-[DATUM].md` · **dup_quelle:** Ist-Zustand der Direktiven (nur NEUE/verbesserbare Vorschlaege)
> - **nacharbeit_aufrufer:** KEINE Aenderung an geschuetzten Direktiven-Regeln ohne Franks Freigabe (ACE-Zone)
> Der research-Skill uebernimmt sichtbare beschriftete Researcher + Continuous-Spawning + Zwischenfazit
> pro Researcher + ruhige Auswertung und gibt das Ergebnis im `direktive`-Schema zurueck; damit
> hier weiterarbeiten. (Die A/B/C-Engine-Details unten bleiben als Referenz, werden aber vom research-Skill ausgefuehrt.)


# Direktiven-Recherche starten

> **Recherche-Weg (Regel `research-strategy.md`):** VOR dem Spawn zuerst eine kurze Empfehlung geben und
> per `AskUserQuestion` Frage 1 (A/B/C/D) stellen. **A/B** laufen ueber `mm-research.py`/`or-research.py`;
> der **5-Opus-Researcher-Schwarm dieses Agenten ist Option C** (nur auf explizite Opus-Wahl). Bei A nach
> Abschluss Frage 2 (Eskalation?).

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
