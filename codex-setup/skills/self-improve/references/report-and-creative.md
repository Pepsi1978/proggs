# Report and Creative Template for Codex Self-Improve

## Report Template

```markdown
## Codex Self-Improve Report — [Datum]

| Metrik | Wert |
|--------|------|
| Dateien geaendert | [N] |
| Skripte validiert | [N] |
| Whiteboard-Eintraege | [N] |
| Environment fixes logged | [N] |
| Skill-Deployment | [Status] |
| MCP geprueft | [Ja/Nein] |
| Selbstbeobachtung aktiv | [Ja/Nein] |
| Claude-Delta-Audit | [Nicht noetig / Geprueft / Portiert] |

### Scan-Ergebnis
[Kurzfassung von Stage 1]

### Whiteboard-Status
[Status des Codex-Whiteboards und der Bruecke]

### Oberste Direktive
[Kurz bestaetigen, dass die Oberste Direktive gelesen wurde und welche Auswirkung sie in dieser Session hatte]

### Intelligenz-Dimensionen
[Welche der 8 Intelligenz-Dimensionen wurden in dieser Session konkret verbessert?]

### Selbstbeobachtung
[Kurz: welche Reibung, Fehler, Umwege oder Benutzerhinweise wurden fuer Codex nutzbar gemacht]

### Selbstbeobachtungskategorien
[Welche der 6 Kategorien waren relevant: Fehler, Umwege, Effizienz, Wissensluecken, Muster, Benutzer-Hinweise?]

### Claude-Delta-Status
[Wenn benutzt: welche Kandidaten waren ADD, ADAPT oder REPLACE]

### Gemini-Delta-Status
[Wenn benutzt: welche Kandidaten waren ADD, ADAPT oder REPLACE]

### Recherche-Ergebnis
| Researcher | Wichtigste Erkenntnis | Aktion |
|------------|----------------------|--------|

### Updates angewandt
| Was | Warum |
|-----|-------|

### Environment-Fix-Log
| Fix-ID | Kontext | Was | Warum | Verifikation |
|--------|---------|-----|-------|-------------|

### Resilient Bugfixing
[Wenn ein Umgebungsfix Teil der Session war: Root Cause, Why-Chain, verwandte Fehlerquellen, Resilienz und Failure-Review kurz bestaetigen]

### Nicht geaendert
[Bewusst nicht geaenderte Dinge mit Grund]

### Offene Punkte
| Thema | Grund | Naechster Schritt |

### Intelligenzhebel dieser Session
| Hebel | Wirkung | Status |
|-------|---------|--------|

### Gesamtstatus
[1-2 Saetze]
```

## Meta-Improve

Always produce exactly 3 suggestions in this format:

```markdown
Intelligenz-Vorschlag: [Was verbessert werden kann] — [Konkreter Vorschlag] — Soll ich das umsetzen?
```

Never auto-apply these three suggestions without user approval.
These suggestions should be easy to understand and focused on the strongest next intelligence gain.
If one suggestion would replace existing Codex rules or behavior, say so explicitly.

## Creative Minimum

Every substantial run should produce:
- 1 artifact or concrete implementation
- 3 action proposals
- 3 simple intelligence opportunities
- 1 creativity reflection

## Action Proposal Format

```markdown
### Handlungsvorschlag N: [Titel]
**Was wuerde sich aendern?** [...]
**Wie umsetzen?** [...]
**Vorteile:** [...]
**Nachteile/Risiken:** [...]
**Empfehlung:** [Jetzt / Naechster Lauf / Nur auf Anfrage]
```

Prioritize proposals that strengthen compounding intelligence, whiteboard alignment, verification, memory, or tooling leverage.
