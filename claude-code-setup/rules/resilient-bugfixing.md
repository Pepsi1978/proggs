# 🛡️ DRITTE DIREKTIVE: RESILIENT BUGFIXING (KRITISCH)

> Kern der Direktive, gilt AUTOMATISCH bei JEDEM Bugfix (egal wie klein). **Vollstaendiger 1:1-Volltext:
> `claude-code-setup/docs/rules/resilient-bugfixing.md`.**

## Auf Zuruf den Volltext laden
Sagt Frank "nach Direktive 1/2/3" oder "nach allen drei Regeln/Direktiven" → den/die vollstaendigen
Volltext(e) per `Read` einlesen: #1 `claude-code-setup/docs/rules/superintelligence.md` · #2
`claude-code-setup/docs/rules/self-observation.md` · #3 `claude-code-setup/docs/rules/resilient-bugfixing.md`.

## Grundprinzip: jeder Bug wird zum permanenten Upgrade
Ein Fix ist nicht fertig bis er zukunftssicher ist — der Fehler darf NIE zweimal auftreten. Nicht nur das
Symptom, die ganze Fehlerklasse eliminieren (Immunisierung, nicht Pflaster).

## Funktionalitaets-Erhaltungspflicht (KRITISCH)
Ein Fix darf NIEMALS eine vorhandene Funktion entfernen/deaktivieren/auskommentieren/mit leerem `catch {}`
schlucken. Feature wirft Fehler → REPARIEREN, nicht entfernen. Entfernen nur auf EXPLIZITEN Benutzer-Wunsch.

## Pflicht-Ablauf
1. **Root Cause** (5-Warum, function-level lokalisieren, nicht nur Symptom). 2. **Verwandte Fehlerquellen**
suchen (gleiche Klasse/Komponente/Abhaengigkeit). 3. **Zukunftssicherer Fix** (self-healing, defensiv,
funktionserhaltend). 4. **Fix-Induced-Failure-Pruefung (8 Punkte)** + **Funktionalitaets-Diff** VOR Commit.
5. **Defense in Depth** (≥2 Schichten: praeventiv + reaktiv mit Logging+Fallback). 6. **Poka-Yoke** (Fehler
per Design unmoeglich — Stufe 3 Eliminierung > 2 Erzwingung > 1 Warnung). 7. **Memory** speichern (Root Cause + Muster).

## Was NIEMALS
- Nur das Symptom fixen · Funktionalitaet entfernen/schlucken statt zu reparieren · einen Fix deployen der
  neue Fehler einfuehrt (ohne 8-Punkte-Pruefung) · gleichen Fehler zweimal · diese Direktive entfernen/abschwaechen.
