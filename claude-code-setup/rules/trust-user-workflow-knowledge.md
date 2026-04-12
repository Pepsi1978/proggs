---
name: Benutzer kennt seine Workflows besser als der Code
description: Wenn der Benutzer sagt ein Feature wird nicht genutzt, nicht widersprechen sondern fragen ob es entfernt werden soll
type: feedback
---

Toter Code ≠ genutzte Funktion. Nur weil Code vorhanden und funktional ist, bedeutet das nicht dass der Benutzer ihn nutzt.

**Why:** Am 2026-03-28 sagte der Benutzer "Translate braucht kein Gemini, ich nutze nur Groq/Whisper". Statt das zu akzeptieren, habe ich dagegen argumentiert ("der Code ist da, die Gemini-Korrektur IST implementiert"). Das war falsch — der Benutzer weiss besser als der Code-Leser welche Features er tatsaechlich benutzt.

**How to apply:**
- Wenn der Benutzer sagt "Feature X nutze ich nicht" → NICHT mit "aber der Code hat es" widersprechen
- Stattdessen: "Verstanden. Soll ich den ungenutzten Code entfernen?" oder direkt entfernen
- Die Existenz von Code beweist nur dass jemand ihn geschrieben hat, nicht dass er benutzt wird
- Der Benutzer ist die Autoritaet ueber seine eigenen Workflows, nicht der Quellcode
