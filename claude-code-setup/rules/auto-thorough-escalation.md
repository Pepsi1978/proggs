# Auto-Thorough-Eskalation bei Qualitaets-Einbruch (KRITISCH)

## Regel

Wenn /self-improve im Standard-Modus laeuft und der evolution-analyst
einen KOLLAPS in einer Kerndimension meldet, MUSS automatisch auf
Thorough-Modus umgeschaltet werden.

## Eskalations-Schwellen

| Dimension | Schwelle | Bedeutung |
|-----------|----------|-----------|
| Meta-Intelligence | < 20% | System verbessert sich nicht mehr selbst |
| Quality Score | < 7.0 | Code-Qualitaet sinkt |
| Corrections | > 5 pro Session | Benutzer muss zu oft korrigieren |
| IQ-Score | Abfall > 15 Punkte | Systemweiter Rueckgang |

## Was passiert bei Eskalation

Standard-Modus → Thorough-Modus:
- Stufe 1: Quick → Full (inkl. Android, Agent-Tiers)
- Stufe 2: R1+R5+R6+R8 → ALLE R1-R8 (kein Cache)
- Stufe 3: Top 5 Fehler → ALLE Fehler fixen
- Stufe 6: Quick (6A,6B,6E) → Komplett (6A-6F)

## Warum

Am 2026-04-02 lief ein Meta-Intelligence-Kollaps (50%→10%) ueber 10 Sessions
(ca. 2 Tage) bevor /self-improve ihn im Standard-Modus entdeckte. Im
Thorough-Modus waeren ALLE Diagnostik-Tools gelaufen und der Bug waere
1-2 Tage frueher gefunden worden.

## Implementierung

Der evolution-analyst gibt bereits Empfehlungen. /self-improve Stufe 0
MUSS diese Empfehlungen lesen und bei einem der oben genannten Schwellenwerte
automatisch eskalieren — ohne den Benutzer zu fragen.
