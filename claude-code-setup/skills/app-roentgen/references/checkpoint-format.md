# Checkpoint-Format fuer lange Audits

## Zweck

Ein vollstaendiger Roentgen-Audit einer mittelgrossen App (500-1500 Kotlin-Dateien) dauert typisch 30-90 Minuten und durchlaeuft mehrere Schichten. Wenn die Session unterbrochen wird (Kontext-Kompaktierung, Crash, Token-Limit, manueller Stop), startet ohne Checkpoint alles von vorn — bei einem zweistuendigen Audit kostet das mehrere tausend Token unnoetig.

Diese Datei beschreibt das Checkpoint-Schema das vor jeder Layer-Phase geschrieben wird, damit das Audit bei der naechsten Session genau dort weitergehen kann wo es unterbrochen wurde.

## Pflicht-Verhalten

Vor JEDER neuen Schicht (1, 2, 3, 3b optional, 4, 4b, 4c, 4d, 4e, 5, 6, 7) eine Checkpoint-Datei in der App-Wurzel schreiben:

```
<app-dir>/app-roentgen-checkpoint.json
```

## Schema (Version 1.0)

```json
{
  "schema": "1.0",
  "audit_started_at": "2026-05-18T10:30:00Z",
  "audit_app_dir": "/path/to/app",
  "audit_app_version": "0.10.2",
  "current_phase": "4b_wortlaut_mapping",
  "completed_phases": [
    "1_manifest",
    "2_dependencies",
    "3_architektur",
    "3b_compose_compiler_reports",
    "4_screens"
  ],
  "screens_inventoried": 47,
  "paywall_screens_found": 7,
  "permissions_extracted": 18,
  "subscription_states_covered": 5,
  "wortlaute_zitiert": 234,
  "billing_version_detected": "v8",
  "last_updated_at": "2026-05-18T11:12:33Z"
}
```

## Pflicht-Felder pro Checkpoint

| Feld | Typ | Bedeutung |
|------|-----|-----------|
| `schema` | string | Schema-Version dieses Checkpoint-Formats (aktuell "1.0") |
| `audit_started_at` | ISO8601 | Beginn der Audit-Session — bleibt ueber alle Phasen konstant |
| `audit_app_dir` | string | Absoluter Pfad zur App-Wurzel |
| `audit_app_version` | string | Erfasste App-Version (aus build.gradle versionName) |
| `current_phase` | string | Aktuelle Phase (siehe Liste unten) |
| `completed_phases` | array | Bisher abgeschlossene Phasen in Reihenfolge |
| `last_updated_at` | ISO8601 | Letzte Aktualisierung des Checkpoints |

## Optionale Statistik-Felder

Diese helfen Claude beim Wiederaufnahmen besser einzuschaetzen wie weit der Scan ist:

- `screens_inventoried` (int)
- `paywall_screens_found` (int)
- `permissions_extracted` (int)
- `subscription_states_covered` (int)
- `wortlaute_zitiert` (int)
- `billing_version_detected` (string: "v6", "v7", "v8")
- `kotlin_files_total` (int)
- `cldr_plural_languages_audited` (int)

## Mögliche `current_phase`-Werte

```
1_manifest
2_dependencies
3_architektur
3b_compose_compiler_reports   (optional, nur wenn Build-Zugriff)
4_screens
4b_wortlaut_mapping
4c_translation_context
4d_legal_text_inventory
4e_external_content
5_paywall
6_hidden_features
7_marketing_claim_audit
done
```

## Bei Wiederaufnahme

1. Checkpoint-Datei einlesen
2. `current_phase` finden und dort FORTSETZEN — bereits erledigte Phasen NICHT erneut bearbeiten
3. Bei jedem Phase-Abschluss `completed_phases` ergaenzen und `current_phase` auf die naechste setzen
4. Nach finalem Bericht: Checkpoint-Datei loeschen ODER mit `"current_phase": "done"` markieren

## Gitignore

Die Checkpoint-Datei gehoert NICHT ins Repo. Wenn die App-Wurzel keine `.gitignore` hat oder den Eintrag nicht enthaelt, am Anfang des Audits ergaenzen:

```
# app-roentgen Audit-Artefakte
app-roentgen-checkpoint.json
app-roentgen-initial-scan.md
app-roentgen-export.json
```

Der finale Bericht `app-roentgen-AUDIT-YYYY-MM-DD.md` DARF dagegen ins Repo — er ist das Audit-Ergebnis.

## Versionierung

Wenn das Schema in Zukunft erweitert wird (z.B. neue Statistik-Felder fuer Layer 4f oder 4g), wird die `schema`-Version erhöht. Ältere Checkpoints bleiben dabei lesbar (additive Änderungen) — Claude prüft beim Einlesen `schema` und fällt bei unbekannten zukünftigen Versionen sauber zurück auf Schicht 1 mit Benutzer-Hinweis.

## Quellen

Anthropic Skills-Konzept und Progressive Disclosure: siehe offizielle Claude Code Dokumentation unter `code.claude.com/docs` (Skills-Sektion). Das Multi-Phase-Checkpoint-Pattern ist eine projekteigene Konvention dieses Skills — keine externe Referenz, die Logik ist hier in dieser Datei vollstaendig dokumentiert.
