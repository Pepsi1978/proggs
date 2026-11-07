---
name: advisormodel-alias-statt-modell-id
description: advisorModel in den Settings braucht den Alias, keine Modell-ID
metadata:
  type: feedback
---

`advisorModel` in den Claude-Code-Settings erwartet einen Alias (z. B. `opus`), keine volle Modell-ID; eine ID wie `claude-fable-5-1` schaltet den Advisor still ab.

**Why:** Originaldatei fehlte (nie im Git, auf diesem Rechner nicht vorhanden); am 07.11.2026 aus der Index-Zeile und nachprüfbaren Quellen neu angelegt. Mehr als die Index-Zeile ist nicht überliefert.

**How to apply:** Beim Ändern von `advisorModel` den Alias eintragen und danach prüfen, ob der Advisor in einer neuen Sitzung wirklich verfügbar ist.
