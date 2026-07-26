#!/usr/bin/env python3
"""
app-roentgen JSON-Exporter

Erzeugt eine strukturierte JSON-Ausgabe aus einem Android-App-Repo, parallel zum
Markdown-Audit-Bericht. Nachgelagerte Skills (Rechtssicherheits-Skill,
Uebersetzungs-Skill) koennen diese JSON-Datei programmatisch lesen anstatt
die Markdown-Datei parsen zu muessen.

Aufruf:
    python3 export-json.py <pfad-zur-android-app>

Output:
    <app-dir>/app-roentgen-export.json
"""

from __future__ import annotations

import sys

# Verhindert dass scripts/__pycache__/ angelegt wird (Skill-Verzeichnis bleibt sauber).
# Muss VOR den anderen Imports stehen damit kein .pyc fuer das Hauptscript entsteht.
sys.dont_write_bytecode = True

import hashlib
import json
import os
import re
from datetime import datetime, timezone
from pathlib import Path

SCHEMA_VERSION = "2.1"

# FIX W4 (Audit 6) + X9 (Audit 7): DEFAULT_LANG ist konfigurierbar via --default-lang.
# Wird in main() ggf. ueberschrieben. Hier nur einmalige Definition (vorher gab es
# eine doppelte Zuweisung — X9-Fix entfernt das redundante "global DEFAULT_LANG = de").
DEFAULT_LANG = "de"


def short_hash(text: str) -> str:
    """SHA1 trunkiert auf 8 Zeichen — fuer Inkremental-Pruefung."""
    return hashlib.sha1(text.encode("utf-8", errors="replace")).hexdigest()[:8]


def find_app_root(app_dir: Path) -> dict:
    """Findet Manifest, strings.xml, build.gradle."""
    info = {"manifest": None, "strings_xml": None, "build_gradle": None, "package_name": None}

    # FIX X3 (Audit 7) + Z5 (Audit 9): Statische Kandidatenliste deckte nur app/ und
    # src/ ab — Custom-Module wie shared/, core/, feature-auth/ wurden uebersehen
    # auch wenn sie src/main bzw. src/androidMain enthielten. Jetzt: statische Liste
    # zuerst (fuer Single-Module-Apps am schnellsten), dann rglob-Fallback projektweit
    # mit Praeferenz fuer app/-Module (haeufigste Konvention).
    static_manifest_candidates = [
        app_dir / "app/src/main/AndroidManifest.xml",
        app_dir / "src/main/AndroidManifest.xml",
        app_dir / "app/src/androidMain/AndroidManifest.xml",
        app_dir / "src/androidMain/AndroidManifest.xml",
        app_dir / "AndroidManifest.xml",
    ]
    for candidate in static_manifest_candidates:
        if candidate.is_file():
            info["manifest"] = str(candidate.relative_to(app_dir))
            break
    # Z5-Fallback: projektweit suchen wenn statische Kandidaten leer (KMP-only, Custom-Module)
    if not info["manifest"]:
        for candidate in sorted(app_dir.rglob("AndroidManifest.xml")):
            cp = str(candidate).replace("\\", "/")
            if "/build/" in cp or "/test/" in cp:
                continue
            if "/src/main/" in cp or "/src/androidMain/" in cp:
                info["manifest"] = str(candidate.relative_to(app_dir))
                break

    static_strings_candidates = [
        app_dir / "app/src/main/res/values/strings.xml",
        app_dir / "src/main/res/values/strings.xml",
        app_dir / "app/src/androidMain/res/values/strings.xml",
        app_dir / "src/androidMain/res/values/strings.xml",
    ]
    for candidate in static_strings_candidates:
        if candidate.is_file():
            info["strings_xml"] = str(candidate.relative_to(app_dir))
            break
    # Z5-Fallback: projektweit suchen wenn statische Kandidaten leer
    if not info["strings_xml"]:
        for candidate in sorted(app_dir.rglob("strings.xml")):
            cp = str(candidate).replace("\\", "/")
            if "/build/" in cp or "/test/" in cp:
                continue
            # Nur Default-Sprache (values/), nicht values-XX/
            if candidate.parent.name != "values":
                continue
            if "/src/main/res/" in cp or "/src/androidMain/res/" in cp:
                info["strings_xml"] = str(candidate.relative_to(app_dir))
                break

    for candidate in [
        app_dir / "app/build.gradle.kts",
        app_dir / "app/build.gradle",
        app_dir / "build.gradle.kts",
        app_dir / "build.gradle",
    ]:
        if candidate.is_file():
            info["build_gradle"] = str(candidate.relative_to(app_dir))
            break

    # Package-Name aus Gradle extrahieren
    # FIX V5 (Audit 5): vorher 'applicationId|namespace' — bei Groovy DSL mit beidem
    # nahm der Regex das erste Vorkommen (oft namespace im android{}-Block oben).
    # Jetzt: zuerst applicationId suchen (das ist der echte Package-Name am Play Store),
    # dann fallback auf namespace (R-Class-Namespace, oft = applicationId).
    if info["build_gradle"]:
        try:
            content = (app_dir / info["build_gradle"]).read_text(encoding="utf-8", errors="replace")
            # 1. Versuch: applicationId (verbindlicher Package-Name fuer Distribution)
            m = re.search(r'\bapplicationId\s*=?\s*["\']([^"\']+)["\']', content)
            if m:
                info["package_name"] = m.group(1)
            else:
                # 2. Fallback: namespace (oft identisch, aber technisch was anderes)
                m = re.search(r'\bnamespace\s*=?\s*["\']([^"\']+)["\']', content)
                if m:
                    info["package_name"] = m.group(1)
        except Exception:
            pass

    return info


def parse_strings_xml(path: Path) -> dict:
    """Parst eine strings.xml und gibt ein dict mit allen Resources zurueck."""
    if not path or not path.is_file():
        return {"strings": [], "plurals": [], "arrays": []}

    try:
        content = path.read_text(encoding="utf-8", errors="replace")
    except Exception:
        return {"strings": [], "plurals": [], "arrays": []}

    result = {"strings": [], "plurals": [], "arrays": []}

    # <string name="..." [translatable="false"]>VALUE</string>
    string_pat = re.compile(
        r'<string\s+name="([^"]+)"([^>]*)>(.*?)</string>',
        re.DOTALL,
    )
    for m in string_pat.finditer(content):
        key, attrs, value = m.group(1), m.group(2), m.group(3)
        translatable = "translatable=\"false\"" not in attrs
        # Inline-Tags entfernen fuer Plain-Text (aber Original behalten)
        plain = re.sub(r"<[^>]+>", "", value).strip()
        # Line-Nummer ermitteln
        line_no = content[: m.start()].count("\n") + 1
        # xliff:g extrahieren
        xliff_ids = re.findall(r'<xliff:g\s+id="([^"]+)"', value)
        # Format-Args extrahieren
        format_args = re.findall(r"%[0-9]+\$[sdf]", value)
        # HTML-Tags erkennen
        has_html = bool(re.search(r"<(b|i|u|br|a|font|strong|em|span|p|tt)\b", value))
        has_cdata = "<![CDATA[" in value
        # Hash fuer Inkremental-Updates
        h = short_hash(value)

        result["strings"].append(
            {
                "key": key,
                "value": value.strip(),
                "plain": plain,
                "length": len(plain),
                "translatable": translatable,
                "xliff_ids": xliff_ids,
                "format_args": format_args,
                "has_html": has_html,
                "has_cdata": has_cdata,
                "line": line_no,
                "hash": h,
            }
        )

    # <plurals name="...">...</plurals>
    plural_pat = re.compile(
        r'<plurals\s+name="([^"]+)"[^>]*>(.*?)</plurals>',
        re.DOTALL,
    )
    item_pat = re.compile(r'<item\s+quantity="([^"]+)"[^>]*>(.*?)</item>', re.DOTALL)
    for m in plural_pat.finditer(content):
        key, body = m.group(1), m.group(2)
        line_no = content[: m.start()].count("\n") + 1
        items = []
        for im in item_pat.finditer(body):
            qty, value = im.group(1), im.group(2)
            items.append(
                {
                    "quantity": qty,
                    "value": value.strip(),
                    "hash": short_hash(value),
                }
            )
        result["plurals"].append(
            {
                "key": key,
                "items": items,
                "quantities": sorted({i["quantity"] for i in items}),
                "line": line_no,
            }
        )

    # <string-array name="...">...</string-array>
    arr_pat = re.compile(
        r'<string-array\s+name="([^"]+)"[^>]*>(.*?)</string-array>',
        re.DOTALL,
    )
    arr_item_pat = re.compile(r"<item[^>]*>(.*?)</item>", re.DOTALL)
    for m in arr_pat.finditer(content):
        key, body = m.group(1), m.group(2)
        line_no = content[: m.start()].count("\n") + 1
        items = [im.group(1).strip() for im in arr_item_pat.finditer(body)]
        result["arrays"].append(
            {
                "key": key,
                "items": items,
                "count": len(items),
                "line": line_no,
            }
        )

    return result


def find_translations(app_dir: Path) -> dict:
    """Findet alle Sprach-Varianten und parst sie.

    FIX V2 (Audit 5): vorher nur 'app/src/main/res/values-*/strings.xml' — das war
    Single-Module-blind. Bei Multi-Module-Apps mit eigenen res-Verzeichnissen
    (z.B. 'feature/journal/src/main/res/values-de/strings.xml') wurden alle
    Translations aus Feature-Modulen ignoriert. Jetzt projektweit via rglob,
    Build-Verzeichnisse gefiltert. Analog zu den Shell-Helpern find_translated_strings_xml.

    Bei Multi-Module mit GLEICHER Locale in mehreren Modulen (z.B. values-de in app/
    UND in feature/) werden die Strings MERGED — das App-Modul gewinnt bei
    Schluessel-Konflikten (Standard-Build-Verhalten).
    """
    translations = {}
    files_per_lang = {}
    # rglob findet ueberall, /build/ explizit ausschliessen
    for f in app_dir.rglob("strings.xml"):
        # Nur values-XX Verzeichnisse, nicht values/ (Hauptsprache)
        parent_name = f.parent.name
        if not parent_name.startswith("values-"):
            continue
        # Build-Output ueberspringen
        if "/build/" in str(f).replace("\\", "/"):
            continue
        # Nur Production-Resource-Pfade akzeptieren (vermeidet Test-Resources, Sample-Code etc.)
        # FIX W1 (Audit 6): vorher nur "src/main/res" — KMP-Apps mit androidMain/res
        # wurden komplett ignoriert. Jetzt beide Konventionen unterstuetzt.
        path_str = str(f).replace("\\", "/")
        if "src/main/res" not in path_str and "src/androidMain/res" not in path_str:
            continue
        lang = parent_name.replace("values-", "")
        files_per_lang.setdefault(lang, []).append(f)

    # Pro Sprache: alle Dateien parsen, dann mergen (app/ gewinnt bei Konflikten)
    for lang, files in files_per_lang.items():
        # app/ vor anderen Modulen sortieren
        files.sort(key=lambda p: (0 if "/app/" in str(p).replace("\\", "/") else 1, str(p)))
        merged = {"strings": [], "plurals": [], "arrays": []}
        seen_keys = {"strings": set(), "plurals": set(), "arrays": set()}
        for f in files:
            parsed = parse_strings_xml(f)
            for kind in ("strings", "plurals", "arrays"):
                for item in parsed[kind]:
                    if item["key"] not in seen_keys[kind]:
                        merged[kind].append(item)
                        seen_keys[kind].add(item["key"])
        translations[lang] = merged
    # Sortiert zurueckgeben fuer stabile Reihenfolge im JSON-Export
    return dict(sorted(translations.items()))


def scan_permissions(manifest_path: Path) -> list:
    """Extrahiert alle Permissions aus dem Manifest."""
    if not manifest_path or not manifest_path.is_file():
        return []
    content = manifest_path.read_text(encoding="utf-8", errors="replace")
    perms = re.findall(r'android\.permission\.([A-Z_]+)', content)
    return sorted(set(perms))


def detect_sdks(app_dir: Path) -> dict:
    """Erkennt verwendete SDKs anhand von Code-Patterns."""
    sdks = {
        "ai": False,
        "ai_providers": [],
        "ads": False,
        "ad_providers": [],
        "billing": False,
        "health": False,
        "health_providers": [],
        "firebase_analytics": False,
        "firebase_remote_config": False,
        "firebase_messaging": False,
        "firebase_crashlytics": False,
        "webview": False,
    }

    patterns = {
        "ai_gemini": (r"GenerativeModel|GeminiClient", "ai", "Gemini"),
        "ai_openai": (r"\bOpenAI\b|openai\.", "ai", "OpenAI"),
        "ai_anthropic": (r"\bAnthropic\b|anthropic\.", "ai", "Anthropic"),
        "ads_admob": (r"\bAdMob\b|InterstitialAd|RewardedAd|BannerAd", "ads", "AdMob"),
        "billing": (r"BillingClient|ProductDetails", "billing", None),
        "health_connect": (r"HealthConnectClient", "health", "Health Connect"),
        "health_googlefit": (r"GoogleFit", "health", "Google Fit"),
        "firebase_analytics": (r"FirebaseAnalytics", "firebase_analytics", None),
        "firebase_remote_config": (r"FirebaseRemoteConfig|remoteConfig\.", "firebase_remote_config", None),
        "firebase_messaging": (r"FirebaseMessaging", "firebase_messaging", None),
        "firebase_crashlytics": (r"\bCrashlytics\b", "firebase_crashlytics", None),
        "webview": (r"\bWebView\b|loadUrl\(", "webview", None),
    }

    # Suche in allen .kt und .java Dateien
    code_files = []
    for ext in ("*.kt", "*.java"):
        code_files.extend(
            f
            for f in app_dir.rglob(ext)
            if "/build/" not in str(f) and "/test/" not in str(f)
        )

    for f in code_files[:5000]:  # Cap fuer sehr grosse Apps
        try:
            text = f.read_text(encoding="utf-8", errors="ignore")
        except Exception:
            continue
        for label, (pat, flag_key, provider) in patterns.items():
            if re.search(pat, text):
                sdks[flag_key] = True
                if provider:
                    list_key = flag_key + "_providers" if flag_key in ("ai", "ads", "health") else None
                    if list_key and provider not in sdks[list_key]:
                        sdks[list_key].append(provider)

    return sdks


def cldr_required_quantities(lang: str) -> list:
    """Gibt zurueck welche Plural-Quantitaeten eine Sprache benoetigt.

    Basis: Unicode CLDR Plural Rules (https://www.unicode.org/cldr/charts/latest/
    supplemental/language_plural_rules.html). Deckt mindestens die 30 Android-Locales
    aus dem uebersetzung-Skill ab plus die wichtigsten Welt-Sprachen.

    FIX M3: Vorherige Tabelle hatte nur 21 Eintraege und fehlte u.a. id/th/vi/lt/lv/
    ro/fil/hi/bn/ka/he-via-iw etc. — Folge: Plural-Audit lieferte fuer diese Sprachen
    den Falschwert ["one", "other"] (Fallback), uebersah also tatsaechliche CLDR-Luecken.
    """
    table = {
        # === one, other (Germanic, Romance, Turkic, Indo-Aryan einfach) ===
        "de": ["one", "other"],
        "en": ["one", "other"],
        "es": ["one", "other"],
        "it": ["one", "other"],
        "nl": ["one", "other"],
        "pt": ["one", "other"],         # pt-PT, allgemein
        "sv": ["one", "other"],
        "no": ["one", "other"],
        "nb": ["one", "other"],
        "nn": ["one", "other"],
        "da": ["one", "other"],
        "fi": ["one", "other"],
        "et": ["one", "other"],
        "hu": ["one", "other"],
        "tr": ["one", "other"],         # CLDR offiziell nur "other", aber DE-Faktum: one+other
        "el": ["one", "other"],
        "bg": ["one", "other"],
        "mk": ["one", "other"],
        "sq": ["one", "other"],
        "is": ["one", "other"],
        "ca": ["one", "other"],
        "eu": ["one", "other"],
        "af": ["one", "other"],
        "sw": ["one", "other"],
        "am": ["one", "other"],
        "ka": ["one", "other"],
        "hy": ["one", "other"],
        "ne": ["one", "other"],
        # Indo-Arische / Dravidische
        "hi": ["one", "other"],
        "bn": ["one", "other"],
        "gu": ["one", "other"],
        "kn": ["one", "other"],
        "ml": ["one", "other"],
        "mr": ["one", "other"],
        "ta": ["one", "other"],
        "te": ["one", "other"],
        "ur": ["one", "other"],
        "fa": ["one", "other"],
        # === nur "other" (keine Plural-Unterscheidung) ===
        "zh": ["other"],                # zh-rCN, zh-rTW
        "ja": ["other"],
        "ko": ["other"],
        "th": ["other"],
        "vi": ["other"],
        "id": ["other"],                # Android-Locale: "in" (legacy)
        "in": ["other"],                # Android-legacy fuer Indonesisch
        "ms": ["other"],
        "fil": ["one", "other"],        # Filipino: laut CLDR doch Plural-Unterscheidung
        "tl": ["one", "other"],
        "my": ["other"],
        "lo": ["other"],
        "km": ["other"],
        # === one, many, other (Romanisch komplex) ===
        "fr": ["one", "many", "other"],
        "pt-rBR": ["one", "many", "other"],
        # === one, few, other ===
        "ro": ["one", "few", "other"],
        "hr": ["one", "few", "other"],
        "sr": ["one", "few", "other"],
        "bs": ["one", "few", "other"],
        # === one, few, many, other (Slavisch komplex) ===
        "ru": ["one", "few", "many", "other"],
        "uk": ["one", "few", "many", "other"],
        "be": ["one", "few", "many", "other"],
        "pl": ["one", "few", "many", "other"],
        "cs": ["one", "few", "many", "other"],
        "sk": ["one", "few", "many", "other"],
        "lt": ["one", "few", "many", "other"],
        "mt": ["one", "few", "many", "other"],
        # === Baltisch (zero, one, other) ===
        "lv": ["zero", "one", "other"],
        # === Slowenisch (eigene Regel: one, two, few, other) ===
        "sl": ["one", "two", "few", "other"],
        # === Hebraeisch ===
        "he": ["one", "two", "many", "other"],
        "iw": ["one", "two", "many", "other"],  # Android-Legacy-Code fuer Hebraeisch
        # === Arabisch (alle sechs) ===
        "ar": ["zero", "one", "two", "few", "many", "other"],
        # === Walisisch (alle sechs) ===
        "cy": ["zero", "one", "two", "few", "many", "other"],
        # === Irisch (fuenf) ===
        "ga": ["one", "two", "few", "many", "other"],
    }
    # Region-Variante zuerst pruefen (z.B. pt-rBR), dann Base-Sprache (pt -> pt)
    base = lang.split("-")[0]
    return table.get(lang, table.get(base, ["one", "other"]))


def main():
    if len(sys.argv) < 2:
        print("Aufruf: python3 export-json.py <pfad-zur-android-app> [--default-lang LANG]", file=sys.stderr)
        print("  --default-lang LANG  Setzt die Default-Sprache (Standard: de)", file=sys.stderr)
        sys.exit(1)

    app_dir = Path(sys.argv[1]).resolve()
    if not app_dir.is_dir():
        print(f"Fehler: {app_dir} ist kein Verzeichnis", file=sys.stderr)
        sys.exit(1)

    # FIX W4 + X6/X9 (Audit 6+7): --default-lang via CLI. X6 fixt silent fallback bei
    # fehlendem Wert (jetzt: exit 1 mit Fehlermeldung). X9 entfernt die doppelte
    # Zuweisung DEFAULT_LANG = "de" — sie steht jetzt nur noch oben am Modul.
    global DEFAULT_LANG
    if "--default-lang" in sys.argv:
        idx = sys.argv.index("--default-lang")
        if idx + 1 >= len(sys.argv) or sys.argv[idx + 1].startswith("--"):
            print("Fehler: --default-lang braucht einen Sprach-Code (z.B. --default-lang en)", file=sys.stderr)
            sys.exit(1)
        DEFAULT_LANG = sys.argv[idx + 1]

    info = find_app_root(app_dir)
    if not info["manifest"]:
        print("Warnung: kein Manifest gefunden", file=sys.stderr)

    # Default-Strings
    default_strings = (
        parse_strings_xml(app_dir / info["strings_xml"])
        if info["strings_xml"]
        else {"strings": [], "plurals": [], "arrays": []}
    )

    # Uebersetzungen
    translations = find_translations(app_dir)

    # Permissions
    permissions = (
        scan_permissions(app_dir / info["manifest"]) if info["manifest"] else []
    )

    # SDK-Detection
    sdks = detect_sdks(app_dir)

    # Plural-Vollstaendigkeitspruefung pro Sprache
    plural_audit = []
    plural_keys = [p["key"] for p in default_strings["plurals"]]
    for key in plural_keys:
        per_lang = {}
        for lang, data in translations.items():
            quantities = []
            for p in data["plurals"]:
                if p["key"] == key:
                    quantities = p["quantities"]
                    break
            required = cldr_required_quantities(lang)
            missing = [q for q in required if q not in quantities]
            per_lang[lang] = {
                "quantities": quantities,
                "required": required,
                "missing": missing,
                "complete": len(missing) == 0,
            }
        plural_audit.append({"key": key, "per_language": per_lang})

    # Untranslatable-Strings
    untranslatable = [s for s in default_strings["strings"] if not s["translatable"]]

    # Top-Glossar (haeufige deutsche Substantive)
    # FIX K1: Der vorherige Regex [A-ZAEOUE]/[a-zaeoeueszsz] war broken — er enthielt
    # nur die ASCII-Buchstaben "A,E,O,U,E" bzw. "a,e,o,u,e,s,z,s,z" und matchte
    # damit KEINE deutschen Umlaute. Folge: alle deutschen Substantive mit ä/ö/ü/ß
    # ("Pruefung", "Loeschung", "Erweiterung", "Groesse") fehlten im Glossar.
    # Jetzt korrekt mit Ä-Ü und ä-ü + ß.
    glossary = {}
    glossary_re = re.compile(r"\b[A-ZÄÖÜ][a-zäöüß]{3,}\b")
    for s in default_strings["strings"]:
        for word in glossary_re.findall(s["plain"]):
            glossary[word] = glossary.get(word, 0) + 1
    top_glossary = sorted(glossary.items(), key=lambda x: -x[1])[:30]

    # Du/Sie-Konsistenz
    du_count = sum(
        len(re.findall(r"\b(du|dein|deine|deinem|deinen|deiner|dir|dich)\b", s["plain"], re.IGNORECASE))
        for s in default_strings["strings"]
    )
    sie_count = sum(
        len(re.findall(r"\b(Sie|Ihr|Ihre|Ihrem|Ihren|Ihrer|Ihnen)\b", s["plain"]))
        for s in default_strings["strings"]
    )

    # Resultat zusammenbauen
    export = {
        "schema": SCHEMA_VERSION,
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "app": {
            "package_name": info["package_name"],
            "manifest_path": info["manifest"],
            "strings_xml_path": info["strings_xml"],
            "build_gradle_path": info["build_gradle"],
            "audited_directory": str(app_dir),
        },
        "consumers": ["rechtssicherheit", "uebersetzung"],
        "permissions": permissions,
        "sdks": sdks,
        "strings": {
            "total": len(default_strings["strings"]),
            # FIX W4 (Audit 6): vorher hardcoded "de". Jetzt CLI-konfigurierbar via
            # --default-lang ODER fallback auf "de" (Project-Konvention: Hauptsprache
            # in values/ ist immer DE bei den geprueften Apps). Apps mit anderer
            # Default-Sprache: python3 export-json.py <dir> --default-lang en
            "default_language": DEFAULT_LANG,
            "items": default_strings["strings"],
        },
        "plurals": {
            "total": len(default_strings["plurals"]),
            "items": default_strings["plurals"],
        },
        "arrays": {
            "total": len(default_strings["arrays"]),
            "items": default_strings["arrays"],
        },
        "translations": {
            lang: {
                "strings_count": len(data["strings"]),
                "plurals_count": len(data["plurals"]),
            }
            for lang, data in translations.items()
        },
        "audits": {
            "untranslatable_strings": [s["key"] for s in untranslatable],
            "untranslatable_count": len(untranslatable),
            "plural_audit": plural_audit,
            "glossary_top30": [{"term": t, "count": c} for t, c in top_glossary],
            "du_sie_consistency": {
                "du_count": du_count,
                "sie_count": sie_count,
                "mixed": du_count > 0 and sie_count > 0,
                "dominant": "du" if du_count > sie_count else ("sie" if sie_count > du_count else "balanced"),
            },
        },
    }

    out_path = app_dir / "app-roentgen-export.json"
    out_path.write_text(json.dumps(export, indent=2, ensure_ascii=False), encoding="utf-8")

    print(f"JSON-Export geschrieben: {out_path}")
    print(f"  Strings: {export['strings']['total']}")
    print(f"  Plurals: {export['plurals']['total']}")
    print(f"  Arrays: {export['arrays']['total']}")
    print(f"  Untranslatable: {len(untranslatable)}")
    print(f"  Sprachen: {len(translations)}")
    print(f"  Permissions: {len(permissions)}")
    print(f"  AI-SDK: {sdks['ai']} ({', '.join(sdks['ai_providers']) or '-'})")
    print(f"  Du/Sie Mix: {export['audits']['du_sie_consistency']['mixed']}")


if __name__ == "__main__":
    main()
