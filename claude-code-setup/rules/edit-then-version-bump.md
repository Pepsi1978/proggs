---
name: Edit + Version-Bump atomar ausfuehren
description: Nach einem Edit-Tool-Aufruf muss die Datei neu gelesen werden bevor ein zweiter Edit (z.B. Version-Bump) moeglich ist — immer in einem Durchgang planen
type: feedback
---

Bei Tampermonkey-Skripten (und generell bei Batch-Edits ueber mehrere Dateien) muessen Edit + Version-Bump als zusammengehoeriger Schritt geplant werden, nicht als zwei getrennte Durchlaeufe.

**Why:** Am 2026-03-28 wurden 9 Tampermonkey-Skripte parallel editiert (Key-Management hinzugefuegt). Der anschliessende Version-Bump in einem zweiten Batch schlug fehl, weil alle Dateien nach dem ersten Edit "stale" waren und neu gelesen werden mussten. Das kostete einen Extra-Durchgang (9 Reads + 9 Edits statt nur 9 Edits).

**How to apply:**
- Bei Batch-Edits ueber mehrere Dateien: Pro Datei ALLE Aenderungen in einem Edit zusammenfassen wenn moeglich
- Wenn mehrere Edits pro Datei noetig: Sequentiell pro Datei arbeiten (Edit 1 → Read → Edit 2), nicht alle Edit-1 parallel und dann alle Edit-2 parallel
- Alternativ: Coder-Agent pro Datei verwenden — der kann mehrere Edits hintereinander machen ohne das Stale-Problem
