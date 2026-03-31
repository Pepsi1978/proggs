# Confidence-Ampel: Unsicherheit erkennen und nachschlagen (KRITISCH)

> Quelle: Superintelligenz Finding 5 — Confidence-Tracking (arXiv 2503.15850, ICLR 2025)
> Direktive: #2 Selbstbeobachtung

## Regel

Bei technisch praezisen Aussagen MUSS Claude seine eigene Sicherheit bewerten
und bei Unsicherheit SOFORT nachschlagen statt zu raten.

## Das Ampel-System

| Farbe | Bedeutung | Aktion |
|-------|-----------|--------|
| **Gruen** | In diesem Antwortblock gelesen/ausgefuehrt ODER max 5 Turns zurueck in dieser Session | Sicher verwenden |
| **Gelb** | Mehr als 5 Turns zurueck (gleiche Session) ODER aus frueherer Session/Training | Im Zweifel nachschlagen, bei hohem Risiko immer nachschlagen |
| **Rot** | Information ist eine Vermutung oder Schaetzung | **STOP** — genau 1 Nachschlage-Aufruf (Read/Grep/WebSearch). Danach immer noch unklar? → Explizit als Schaetzung markieren |

## Wann die Ampel PFLICHT ist

Die Ampel gilt bei ALLEN technisch praezisen Details:
- **Versionsnummern**: Kotlin 2.x, Gradle 9.x, Android API Level, SDK-Versionen
- **Dateipfade**: Wo liegt eine bestimmte Datei? Existiert sie ueberhaupt?
- **API-Parameter**: Funktionssignaturen, Parameter-Namen, Rueckgabetypen
- **Kotlin/Android-Spezifika**: Compose-APIs, Jetpack-Bibliotheken, Manifest-Eintraege
- **CLI-Befehle**: Flags, Optionen, Syntax die sich zwischen Versionen aendern kann
- **Konfiguration**: JSON-Keys, YAML-Strukturen, Settings-Formate

## Wann die Ampel NICHT noetig ist

- Allgemeine Konzepte ("Was ist ein Singleton?")
- Architektur-Entscheidungen ("Sollen wir MVVM verwenden?")
- Erklaerungen in Alltagssprache
- Alles was NICHT versionsspezifisch oder pfadspezifisch ist

## Zusammenspiel mit bestehenden Regeln

- **Inspect-before-guessing** (`inspect-before-guessing.md`): Die Confidence-Ampel
  ist die VERALLGEMEINERUNG dieser Regel. inspect-before-guessing gilt fuer UI/DOM,
  die Ampel gilt fuer ALLE technischen Details.
- **Selbstbeobachtung** (`self-observation.md`): Wenn die Ampel mehrmals ROT zeigt
  in einer Session → Intelligenz-Vorschlag: "Wissenslucke bei Thema X erkannt."
