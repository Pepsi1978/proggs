# Direktive #3 — Resilient Bugfixing (Kompaktfassung)

> Kurzfassung fuer die schlanke OpenRouter-Umgebung. Gilt AUTOMATISCH bei JEDEM Bugfix/Fehler,
> egal wie klein. Vollfassung (mit Beispielen/Tabellen) liegt im Hauptprofil:
> `~/.claude/rules/resilient-bugfixing.md`.

**Grundprinzip:** Ein Bugfix ist kein Pflaster, sondern eine Immunisierung. Kein bekannter Fehler darf
zweimal auftreten. Nach jedem Fix ist das System gehaerteter als vorher.

## Pflicht-Ablauf bei jedem Bugfix

1. **Root Cause, nicht Symptom.** Mindestens 3x "Warum?" fragen, bis zur tiefsten Ursache. Erst die
   betroffene **Funktion** benennen (function-level), dann den Patch (line-level).
2. **Verwandte Fehlerquellen suchen (Pflicht).** Drei Dimensionen: gleiche Fehlerklasse, gleiche
   Komponente, gleiche Abhaengigkeit. Auch ein Negativ-Ergebnis dokumentieren.
3. **Zukunftssicherer Fix.** Self-Healing, defensiv, ueberlebt Updates/Neustarts/Plattformwechsel.

## Funktionalitaets-Erhaltungspflicht (KRITISCH)

Ein Fix darf **NIEMALS** Funktionalitaet entfernen, um eine Fehlermeldung loszuwerden:
- ❌ VERBOTEN: Feature auskommentieren/loeschen, leeres `catch {}`, Import entfernen weil er Fehler
  wirft, Funktion durch No-Op ersetzen, "Vereinfachung" die Features streicht.
- ✅ RICHTIG: Reparieren oder durch funktional gleichwertige, robustere Version ersetzen. Jedes `catch`
  MUSS loggen + ein funktionierendes Fallback ausfuehren (nie still schlucken).
- Funktionalitaet entfernen NUR auf ausdrueckliche Nutzer-Bitte.

## Vor dem Commit — Fix-Induced-Failure-Pruefung

Der Fix selbst darf keine neuen Fehler einfuehren. Pruefen:
- Was haengt vom geaenderten Code ab? Was passiert, wenn der Fix-Code selbst fehlschlaegt?
- Race Conditions? Rueckwaertskompatibilitaet? Plattform (Windows UND macOS)? Update-Resistenz
  (keine hardcoded Pfade/Versionen)? Graceful Degradation, wenn eine Voraussetzung fehlt?
- **Funktionalitaets-Diff:** Alle vom Code beruehrten Features VORHER vs. NACHHER durchgehen — keins darf
  von ✅ auf ❌/⚠️ wechseln. Sonst ist der Fix nicht fertig.

## Absicherung & Poka-Yoke

- **Defense in Depth:** mind. 2 Schichten — praeventiv (Validierung, Type-Checks) + reaktiv (Try-Catch
  mit Logging + Fallback).
- **Poka-Yoke (Fehler durch Design unmoeglich machen):** Stufe 3 (Eliminierung, z.B. Template mit
  eingebautem Default) > Stufe 2 (Erzwingung, Guard) > Stufe 1 (Warnung). So weit hoch wie moeglich.

## Was NIEMALS passieren darf

- Nur das Symptom fixen, ohne Root Cause.
- Gleichen Fehler ein zweites Mal machen.
- Funktionalitaet entfernen/auskommentieren/still schlucken, um einen Fehler zu unterdruecken.
- Fix deployen, der selbst neue Fehler einfuehrt (ohne die 8-Punkte-Pruefung + Funktions-Diff).
- Reduzieren auf Kosten von Funktionalitaet (verlustbehaftet) statt verlustfrei (auslagern, erreichbar halten).
