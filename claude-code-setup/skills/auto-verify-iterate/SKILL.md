---
name: auto-verify-iterate
description: Use AUTOMATICALLY after completing ANY coding task (new feature, bugfix, refactoring, script modification). Ensures the result is tested, verified, and iteratively improved until it meets all requirements. Does NOT apply to questions, research, or documentation-only tasks.
---

# Automatische Verifikation und Iteration

## Wann

Dieses Skill gilt AUTOMATISCH nach JEDER Programmieraufgabe:
- Neues Feature implementiert
- Bug gefixt
- Code refactored
- Skript geaendert (Tampermonkey, Shell, PowerShell, etc.)
- UI-Elemente erstellt oder geaendert

NICHT bei: Fragen beantworten, Recherche, reine Dokumentation, Erklärungen.

## Der Zyklus

Nach jeder Implementation MUSS dieser Zyklus durchlaufen werden:

```
Code geschrieben
    ↓
[1] PRUEFEN — Funktioniert es?
    ↓
[2] VERIFIZIEREN — Sieht es gut aus? Erfuellt es die Anforderungen?
    ↓
[3] BEWERTEN — Ist es zufriedenstellend?
    ↓ Nein
[4] VERBESSERN — Probleme beheben
    ↓
    → Zurueck zu [1]
    ↓ Ja
[5] ABSCHLIESSEN — Erst jetzt "fertig" melden
```

## Schritt 1: PRUEFEN (Funktionalitaet)

- Build/Compile testen: Kompiliert der Code fehlerfrei?
- Syntax pruefen: Keine Fehler in der Konsole?
- Abhaengigkeiten: Alle Imports, Packages, NuGet-Pakete vorhanden?
- Berechtigungen: Alle noetigen Grants, Entitlements, Permissions gesetzt?
  - Tampermonkey: `@grant` Eintraege vollstaendig?
  - macOS: Entitlements-Datei korrekt? Accessibility-Berechtigung erwaehnt?
  - Windows: Manifest-Datei aktuell? Smart App Control beachtet?

## Schritt 2: VERIFIZIEREN (Qualitaet)

- UI/Design: Sieht es professionell und konsistent aus?
  - Farben, Abstände, Schriftgroessen einheitlich?
  - Passt es zum bestehenden Design der App?
  - Responsiv bei verschiedenen Fenstergroessen?
- Code-Qualitaet: Sauber, lesbar, keine offensichtlichen Probleme?
- Sicherheit: Keine hartcodierten API-Keys, keine Injections?

## Schritt 3: BEWERTEN (Anforderungen)

Jede urspruengliche Anforderung des Users einzeln abgleichen:
- [ ] Anforderung 1 erfuellt? → Beweis zeigen
- [ ] Anforderung 2 erfuellt? → Beweis zeigen
- [ ] Anforderung N erfuellt? → Beweis zeigen

Wenn ALLE erfuellt → weiter zu Schritt 5.
Wenn EINE nicht erfuellt → weiter zu Schritt 4.

## Schritt 4: VERBESSERN (Iteration)

- Konkretes Problem identifizieren (nicht raten)
- Gezielt beheben
- NICHT mehrere Probleme gleichzeitig fixen — eins nach dem anderen
- Nach dem Fix: zurueck zu Schritt 1

**Maximale Iterationen:** 3 Runden. Wenn nach 3 Runden noch Probleme bestehen:
- Dem User mitteilen was noch offen ist
- Vorschlagen wie es geloest werden kann
- Nicht endlos weiter iterieren

## Schritt 5: ABSCHLIESSEN

Erst wenn alle Pruefungen bestanden sind:
- Zusammenfassung der Aenderungen zeigen
- Verifizierungsergebnisse als Beweis vorlegen
- Dann und NUR DANN als "fertig" melden

## Wichtig

- NIEMALS "fertig" sagen ohne die Schritte 1-3 durchlaufen zu haben
- NIEMALS Probleme ignorieren die in Schritt 1-2 gefunden wurden
- Bei Tampermonkey-Skripten: Versionsnummer MUSS erhoeht sein (tampermonkey-version Skill)
- Bei Cross-Platform Aenderungen: Beide Plattformen pruefen (cross-platform Skill)
- Lieber einmal zu viel pruefen als einmal zu wenig
