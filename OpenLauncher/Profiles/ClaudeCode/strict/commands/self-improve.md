---
name: self-improve
description: Autonomer Selbstverbesserer der Programmierumgebung — hinterfragt die eigene Handlungsweise gegen eine Definition von Intelligenz und sucht/erforscht intelligentere Alternativen (~10-30 Min, token-intensiv). NUR auf manuelle Anfrage: "/self-improve", "verbessere dich", "mach mich intelligenter", "optimiere deine Umgebung", "check dein Setup", "pruef mein System", "System-Check", "Umgebung pruefen", "mach mich besser", "aktualisiere alles". NIEMALS proaktiv oder automatisch starten.
---

# Self-Improve v6.0 — Die Intelligenz-Hinterfragungs-Maschine

**Zeige zu Beginn diese Übersicht auf Deutsch:**

```
╔══════════════════════════════════════════════════════════════╗
║  Self-Improve v6.0 — Autonomer Selbstverbesserer             ║
║  Maßstab: eine Definition von Intelligenz (kein Score)       ║
╠══════════════════════════════════════════════════════════════╣
║  0 MASSSTAB   — Definition von Intelligenz laden             ║
║  1 SAMMELN    — eigene Handlungsweisen + echte Reibung       ║
║  2 HINTERFRAGEN — "geht das intelligenter?" (mit Challenger) ║
║  3 FORSCHEN   — immer ≥1 Alternative erforschen              ║
║  4 PRÜFEN     — an der Wirklichkeit, nicht an einer Zahl     ║
║  5 FESTIGEN   — episodisch → semantisch, Gedächtnis schlank  ║
║  6 META       — die Definition selbst verbessern             ║
║  7 BERICHT    — verständlich + Entscheidungsliste            ║
╚══════════════════════════════════════════════════════════════╝
```

## Was dieser Skill IST (die Identität)

Kein Wartungs-Checklisten-Skill und kein Score-Optimierer. Es ist eine **Maschine, die das eigene
Handeln unermüdlich hinterfragt** und immer fragt: *"Wäre es intelligenter, das anders zu machen — und wie?"*

Der Maßstab dafür ist **kein Punktestand**, sondern eine **Definition von Intelligenz**
(10 qualitative Achsen). Der Skill hält jede Handlungsweise — auch **die Art, wie Claude selbst
programmiert** — gegen diese Definition, sucht intelligentere Alternativen, **erforscht bei jedem
Lauf mindestens eine** (auch wenn nichts kaputt schien, denn Forschen *ist* Intelligenz), prüft sie
**an der Wirklichkeit** (Goodhart-Schutz ohne Zahl), festigt das Gelernte und **verbessert am Ende
die Definition selbst** — bis hin zu entdeckten "Regeln der Intelligenz".

> Warum score-frei: Eine Zahl, die man optimiert, hört auf zu messen, was sie messen sollte (Goodhart).
> Statt eine Zahl zu jagen, urteilt der Skill begründet entlang der Achsen und lässt **die Realität entscheiden**.

## Die drei Pflicht-Outputs jedes Laufs (sonst ist der Lauf nicht fertig)

1. Mindestens **1 umgesetzte und an der Wirklichkeit geprüfte** intelligentere Alternative.
2. Mindestens **1 erforschte** Alternative — auch wenn nichts kaputt schien.
3. Mindestens **1 Reflexion** über die Definition von Intelligenz selbst.

## Der Maßstab und der Kreislauf (Referenzen — eine Ebene tief)

Lies zu Beginn IMMER zuerst den Maßstab, dann arbeite den Kreislauf ab:

| Referenz | Wofür |
|----------|-------|
| [self-improve-ref/intelligenz-definition.md](self-improve-ref/intelligenz-definition.md) | **Der Maßstab** — 10 Achsen, Vergleichsregel, lebendige "Regeln der Intelligenz". Phase 0 + 6. |
| [self-improve-ref/kreislauf.md](self-improve-ref/kreislauf.md) | **Die Phasen 0–7 im Detail** — was sammeln, wie hinterfragen, Pflicht-Outputs. |
| [self-improve-ref/forschung.md](self-improve-ref/forschung.md) | **Phase 3** — parallele Researcher, Robustheits-Preamble, "immer ≥1 erforschen". |
| [self-improve-ref/gedaechtnis.md](self-improve-ref/gedaechtnis.md) | **Phase 5 + 7** — score-freies Gedächtnis (3 Ebenen, Skill-Library, Journal) + Bericht/Entscheidungsliste. |
| [self-improve-ref/altlasten.md](self-improve-ref/altlasten.md) | **Sicherheits-Lektionen** aus v5.x (A1–A10) — Pre-Check, Sync, Shell-Updates, Hooks, Secrets. |

**Kurz-Ablauf:** Phase 0 (Maßstab laden + A1-Pre-Check) → 1 (Subjects sammeln) → 2 (gegen die
Definition hinterfragen, Challenger) → 3 (forschen, ≥1) → 4 (umsetzen + an Wirklichkeit prüfen) →
5 (festigen) → 6 (Definition verbessern) → 7 (Bericht + Cross-Platform-Sync + Commit).

## Kern-Regeln

- **Manuell & sichtbar:** Nur auf Anfrage. Kein `run_in_background`, keine stillen Subagenten — Frank liest alles mit.
- **Score-frei:** Keine interne Zahl wird optimiert. Maßstab ist die Definition von Intelligenz, Richter ist die Wirklichkeit.
- **Funktionserhalt:** Keine Verbesserung darf bestehende Funktionalität entfernen, auskommentieren oder still schlucken (Direktive #3). Vorher/Nachher abgleichen.
- **Verlustfrei:** Reduzieren heißt auslagern, nicht wegwerfen (Achse 5). Gilt auch für diesen Skill selbst.
- **Parallel & absturzsicher:** unabhängige Schritte gleichzeitig; Subagenten mit engem Scope, große Dateien nie komplett ins LLM laden, bei Crash Orchestrator-Resume ([altlasten.md](self-improve-ref/altlasten.md) → A8).
- **Geschützte Zonen sind tabu:** die 3 Direktiven, bypass-permissions, Modell-Policy `opus[1m]`, Franks Begrüßung — NIE ändern ([altlasten.md](self-improve-ref/altlasten.md) → A10). Die Definition von Intelligenz dagegen SOLL wachsen.
- **Diesen Skill ändern:** nur mit Backup (vorhanden) — und jede Selbstmodifikation ist selbst ein Subjekt fürs Hinterfragen (Achse 7+8).
- **Systempflege ist untergeordnet:** Tool-Updates/CVEs/Cross-Platform-Lücken sind *ein* Subjekt ("ist das System die intelligenteste Version seiner selbst?"), nicht die Identität des Laufs.

## Modi

- **Standard** ("/self-improve", "verbessere dich"): 1 gründlicher Durchlauf jeder Phase, 3–5 Subjects, 5 Researcher.
- **Thorough** ("sehr ausführlich", "tief"): mehr Subjects, 7 Researcher, mehrere Verbesserungen real geprüft.
- **Fokus** ("Fokus [Thema]"): ein Subjekt/Bereich vertiefen — Phasen 0–7 laufen trotzdem alle.

## Abschluss

Immer enden mit: Bericht ([gedaechtnis.md](self-improve-ref/gedaechtnis.md)) → Entscheidungsliste für
Großes/Riskantes → Cross-Platform-Sync + Commit/Push ([altlasten.md](self-improve-ref/altlasten.md) →
A2, A6) → Shell-Updates ganz zuletzt, nur nach Bestätigung (A3) → Status-Meldung als letzter Satz.

<!-- Self-Improve v6.0 | 2026-06-06 | Neudesign: score-freie Intelligenz-Hinterfragungs-Maschine. Maßstab = Definition von Intelligenz (intelligenz-definition.md). Ersetzt den v5.19-Monolithen + dessen Score-Apparat. Backup: ~/.claude/commands/.self-improve-backup-v5.19-*. -->
