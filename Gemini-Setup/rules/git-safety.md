# 🛠️ Git-Sicherheitsregel: Fetch vor jedem Push (PFLICHT)

> **Diese Regel dient zur Vermeidung von Git-Konflikten und Fehlversuchen.**
> **Sie gilt fuer JEDE Git-Operation, die das Remote-Repository (`origin`) modifiziert.**

## Die Regel

Bevor ein `git push` ausgefuehrt wird, MUSS der Agent den Remote-Status prüfen.

**Schritte:**
1. **Fetch**: `git fetch origin` ausfuehren.
2. **Status**: `git status` prüfen (z.B. "Your branch is behind 'origin/main'").
3. **Reaktion**:
   - Wenn der Branch hinterherhinkt: `git pull --rebase` ausfuehren (vorher uncommittete Änderungen stashen/committen).
   - Nur wenn der Status "up to date" oder "ahead" ist, darf `git push` ausgefuehrt werden.

## Warum

In komplexen Multi-Plattform-Umgebungen (macOS, Windows, Codex) ist das Risiko von parallelen Änderungen hoch. Ein blinder Push führt zu Fehlern, Zeitverlust und ineffizienten Tool-Turns. Durch `fetch` wird der Konflikt erkannt, BEVOR er beim Push fehlschlägt.

---
*Status: Operative Sicherheitsregel für Gemini CLI. Verifiziert am 27.03.2026.*
