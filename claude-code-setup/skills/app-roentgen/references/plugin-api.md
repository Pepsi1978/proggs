# Plugin-API — Wie konsumierende Skills den Roentgen-Bericht nutzen

## Zweck

Der app-roentgen-Skill erzeugt zwei Output-Formate parallel:

| Format | Datei | Zielgruppe |
|--------|-------|------------|
| **Markdown** | `app-roentgen-AUDIT-YYYY-MM-DD.md` | Frank zum Lesen; Claude zum Analysieren |
| **JSON** | `app-roentgen-export.json` | Plugins / Skills die programmatisch konsumieren |

Diese Datei beschreibt die JSON-API fuer Konsumenten wie den Rechtssicherheits-Skill, den Uebersetzungs-Skill und kuenftige Plugins.

## Schema-Versionierung

Jeder Bericht traegt eine `schema`-Version (aktuell `"2.0"`). Plugins MUESSEN diese Version pruefen bevor sie den Bericht konsumieren:

```python
import json
data = json.load(open("app-roentgen-export.json"))
if data["schema"] != "2.0":
    raise SystemExit(f"Unbekanntes Schema {data['schema']}, dieser Plugin unterstuetzt nur 2.0")
```

## Erzeugen der JSON-Datei

```bash
python3 ~/.claude/skills/app-roentgen/scripts/export-json.py <pfad-zur-android-app>
```

Output: `<app-dir>/app-roentgen-export.json`

## Top-Level-Struktur

```jsonc
{
  "schema": "2.0",
  "generated_at": "2026-05-16T11:30:00+00:00",
  "app": {
    "package_name": "de.frank.bestjournal",
    "manifest_path": "app/src/main/AndroidManifest.xml",
    "strings_xml_path": "app/src/main/res/values/strings.xml",
    "build_gradle_path": "app/build.gradle.kts",
    "audited_directory": "/c/Users/barwa/proggs/BestJournalAndroid"
  },
  "consumers": ["rechtssicherheit", "uebersetzung"],
  "permissions": ["CAMERA", "RECORD_AUDIO", "POST_NOTIFICATIONS", ...],
  "sdks": { ... },
  "strings": { "total": 1093, "default_language": "de", "items": [...] },
  "plurals": { "total": 6, "items": [...] },
  "arrays": { "total": 5, "items": [...] },
  "translations": { "en": {...}, "fr": {...}, ... },
  "audits": { ... }
}
```

## Strings-Block (das Herzstueck)

Jeder String ist ein Objekt mit folgenden Feldern:

```jsonc
{
  "key": "paywall_cta_primary",
  "value": "Jetzt Premium starten",          // Original-Wert (mit eventuellen Inline-Tags)
  "plain": "Jetzt Premium starten",          // Ohne Inline-Tags
  "length": 21,                              // Plain-Laenge in Zeichen
  "translatable": true,                      // Vom <string translatable="false"> gesetzt
  "xliff_ids": ["amount"],                   // Liste der xliff:g-IDs im String
  "format_args": ["%1$s"],                   // Liste der Format-Argumente
  "has_html": false,                         // Enthaelt HTML-Tags (b, i, br, a, ...)
  "has_cdata": false,                        // Enthaelt CDATA-Bereich
  "line": 142,                               // Zeilennummer in strings.xml
  "hash": "a3f8c2d1"                         // SHA1[:8] des Original-Werts (Inkremental-Check)
}
```

### Anwendungsfall: Uebersetzungs-Skill

```python
import json
data = json.load(open("app-roentgen-export.json"))

# Nur uebersetzbare Strings
to_translate = [s for s in data["strings"]["items"] if s["translatable"]]

# Strings mit Format-Args ohne xliff:g (Wrap-Kandidaten)
needs_xliff = [
    s for s in to_translate
    if s["format_args"] and not s["xliff_ids"]
]

# Untransable-Strings (NICHT uebersetzen)
do_not_translate = data["audits"]["untranslatable_strings"]
```

### Anwendungsfall: Rechtssicherheits-Skill

```python
# Strings mit "Premium", "kostenlos", "unbegrenzt" pruefen
risk_keywords = ["unbegrenzt", "kostenlos", "lebenslang", "garantiert"]
risky = [
    s for s in data["strings"]["items"]
    if any(kw in s["plain"].lower() for kw in risk_keywords)
]

# Hat die App AI-SDK + Disclaimer?
if data["sdks"]["ai"] and not any("disclaimer" in s["key"] for s in data["strings"]["items"]):
    raise AuditFinding("EU AI Act: kein Disclaimer trotz AI-SDK")
```

### Inkremental-Updates via Hash

Beim zweiten Audit-Lauf koennen Plugins die `hash`-Werte vergleichen:

```python
import json
old = json.load(open("app-roentgen-export.json.previous"))
new = json.load(open("app-roentgen-export.json"))

old_hashes = {s["key"]: s["hash"] for s in old["strings"]["items"]}
new_hashes = {s["key"]: s["hash"] for s in new["strings"]["items"]}

changed = [k for k in new_hashes if old_hashes.get(k) != new_hashes[k]]
added = [k for k in new_hashes if k not in old_hashes]
removed = [k for k in old_hashes if k not in new_hashes]

print(f"Changed: {len(changed)}, Added: {len(added)}, Removed: {len(removed)}")
```

## Plurals-Block mit Vollstaendigkeits-Audit

```jsonc
{
  "key": "plural_entries_count",
  "items": [
    {"quantity": "one", "value": "%d Eintrag", "hash": "ab12cd34"},
    {"quantity": "other", "value": "%d Eintraege", "hash": "ef56gh78"}
  ],
  "quantities": ["one", "other"],
  "line": 580
}
```

Im `audits.plural_audit` steht pro Plural-Key + Sprache welche Quantitaeten fehlen:

```jsonc
{
  "key": "plural_entries_count",
  "per_language": {
    "ru": {
      "quantities": ["one", "other"],
      "required": ["one", "few", "many", "other"],
      "missing": ["few", "many"],
      "complete": false
    }
  }
}
```

## SDK-Erkennung

```jsonc
"sdks": {
  "ai": true,
  "ai_providers": ["Gemini"],
  "ads": false,
  "ad_providers": [],
  "billing": true,
  "health": false,
  "health_providers": [],
  "firebase_analytics": true,
  "firebase_remote_config": true,
  "firebase_messaging": true,
  "firebase_crashlytics": true,
  "webview": false
}
```

Plugins koennen daraus ableiten welche zusaetzlichen Pruefungen noetig sind:

| Wenn... | Dann pruefen... |
|---------|----------------|
| `sdks.ai == true` | AI-Disclaimer-Pflicht (EU AI Act + FTC) |
| `sdks.ads == true` | Werbe-Markierungen (UWG §5a) |
| `sdks.billing == true` | Subscriptions-Policy-Pflichtangaben |
| `sdks.health == true` | Health-Disclaimer + Art. 9 DSGVO |
| `sdks.webview == true` | Cookie-Banner-Pflicht (TDDDG) |
| `sdks.firebase_analytics == true` | Consent-Banner-Pflicht (DSGVO) |

## Audits-Block

```jsonc
"audits": {
  "untranslatable_strings": ["app_name", "version_string", ...],
  "untranslatable_count": 4,
  "plural_audit": [ ... ],
  "glossary_top30": [
    {"term": "Eintrag", "count": 142},
    {"term": "Premium", "count": 89},
    ...
  ],
  "du_sie_consistency": {
    "du_count": 287,
    "sie_count": 4,
    "mixed": true,
    "dominant": "du"
  }
}
```

## Stable Area-IDs

Die `area_id`-Konvention (siehe Layer 4b) verbindet die Markdown-Tabellen mit JSON-Eintraegen. Plugins koennen so gezielt einen Bereich referenzieren:

```python
# Beispiel: Wortlaut fuer den Loesch-Dialog suchen
target_area = "dialog_delete_entry"
# Im MD-Bericht steht: ### Dialog: Eintrag loeschen
#                      **Area-ID:** dialog_delete_entry
#                      | Sub-Area-ID | ... |
#                      | dialog_delete_entry__confirm_button | ... |

# Im JSON-Audits-Block (kuenftig — Welle 4):
# audits.areas[target_area].slots[*].string_key  # liefert die zugehoerigen String-Keys
```

> Hinweis: Die vollstaendige Verknuepfung Area-ID ↔ String-Keys wird vom Audit-Bericht (nicht vom Export-Skript) erstellt — Claude trifft die Zuordnungen aus dem Code. Das JSON-Skript liefert die Roh-Daten, der Audit-Bericht (.md) verknuepft Area-IDs mit String-Keys.

## Konsumenten-Filter

Im Schema steht welche Konsumenten der Bericht adressiert:

```jsonc
"consumers": ["rechtssicherheit", "uebersetzung"]
```

Plugins koennen pruefen ob sie als Konsument akzeptiert werden:

```python
if "uebersetzung" not in data["consumers"]:
    print("Warnung: Bericht ist nicht fuer den Uebersetzungs-Skill optimiert")
```

## Kuenftige Erweiterungen

| Version | Geplante Erweiterungen |
|---------|------------------------|
| 2.1 | Area-IDs als eigene Top-Level-Sektion (`areas`) mit Verknuepfung zu String-Keys |
| 2.2 | Diff-Format `app-roentgen-diff.json` zwischen zwei Audit-Laeufen |
| 2.3 | Slot-Laengen-Audit als strukturiertes JSON-Feld pro String |
| 3.0 | Maschinenlesbares Subscription-State-Machine-Modell |

## Was die JSON-Datei NICHT enthaelt

- **Wortlaute aus dem Markdown-Audit-Bericht** (Dialog-Slots, Settings-Hierarchien) — die werden von Claude im Markdown-Bericht erstellt, nicht vom Python-Exporter (zu kontextabhaengig)
- **Externe Inhalte** (Play-Store-Listing, Email-Templates) — die kommen aus dem MD-Bericht weil Frank sie manuell beitraegt
- **Werbeaussagen-Risiko-Klassifizierung** (KRIT/HOCH/MITTEL) — die entsteht erst durch den Rechtssicherheits-Skill der den Bericht konsumiert

## Beispiel-Plugin (Skeleton)

```python
#!/usr/bin/env python3
"""Mini-Plugin das den Roentgen-Bericht konsumiert."""
import json, sys

with open(sys.argv[1], encoding="utf-8") as f:
    data = json.load(f)

assert data["schema"].startswith("2."), f"Unbekanntes Schema: {data['schema']}"

# Beispiel: Strings die laenger als 30 Zeichen sind
long_strings = [s for s in data["strings"]["items"] if s["length"] > 30]
print(f"{len(long_strings)} Strings > 30 Zeichen")

# Beispiel: Strings mit AI-bezogenen Begriffen
ai_strings = [
    s for s in data["strings"]["items"]
    if "ki" in s["key"].lower() or "ai" in s["key"].lower()
]
print(f"{len(ai_strings)} Strings mit AI/KI im Key")
```
