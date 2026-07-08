# 🛡️ DRITTE DIREKTIVE: RESILIENT BUGFIXING (KRITISCH)

> Kern, gilt AUTOMATISCH bei JEDEM Bugfix. **1:1-Volltext: `claude-code-setup/docs/rules/resilient-bugfixing.md`.**

## Auf Zuruf den Volltext laden
Sagt Frank "nach Direktive 1/2/3" (oder "alle drei") → Volltext(e) per `Read` aus
`claude-code-setup/docs/rules/`: `superintelligence.md` (#1), `self-observation.md` (#2), `resilient-bugfixing.md` (#3).

## Grundprinzip
Ein Fix ist nicht fertig bis er zukunftssicher ist — der Fehler darf NIE zweimal auftreten. Nicht nur das
Symptom, die ganze Fehlerklasse eliminieren (Immunisierung, nicht Pflaster).

## Funktionalitaets-Erhaltungspflicht (KRITISCH)
Ein Fix darf NIEMALS eine Funktion entfernen/deaktivieren/mit leerem `catch {}` schlucken. Feature wirft
Fehler → REPARIEREN, nicht entfernen. Entfernen nur auf EXPLIZITEN Benutzer-Wunsch.

## Pflicht-Ablauf
1. **Root Cause** (5-Warum, function-level). 2. **Verwandte Fehlerquellen** (Klasse/Komponente/
Abhaengigkeit). 3. **Zukunftssicherer Fix** (self-healing, defensiv, funktionserhaltend). 4.
**Fix-Induced-Failure (8 Punkte) + Funktionalitaets-Diff** VOR Commit. 5. **Defense in Depth** (≥2
Schichten: praeventiv + reaktiv mit Logging+Fallback). 6. **Poka-Yoke** (Stufe 3 > 2 > 1). 7. **Memory** speichern.

## Was NIEMALS
- Nur das Symptom fixen · Funktionalitaet entfernen/schlucken statt zu reparieren · Fix ohne 8-Punkte-
  Pruefung deployen · gleichen Fehler zweimal · diese Direktive entfernen/abschwaechen.
