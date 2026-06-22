#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Gemeinsame Quelle fuer die Trivial-Klassifikation von Code-Aenderungen (DRY, Direktive #1).

EINE Definition, zwei Konsumenten — damit "was ist trivial" (besonders Version-Bump) NIE zwischen
Auswertung und Durchsetzung auseinanderlaeuft:

  - ~/.claude/skills/almanach-trigger-auswertung/scripts/aggregate.py  -> importiert classify()
  - ~/.claude/hooks/bug-almanac-guard.{ps1,sh}  -> ruft die CLI 'is-version-bump'

Konservativ: im Zweifel "logic" (berechtigt) — lieber eine Unterbrechung zu viel als ein
verpasster Bug. Die Version-Bump-Erkennung ist bewusst ENG (nur echte App-Versionsfelder), damit
z.B. ein `minSdkVersion = 26`-Edit NICHT faelschlich als trivialer Bump durchgewunken wird.

CLI:
  echo "<excerpt>"        | python3 trivial_classify.py classify             -> Klasse auf stdout (default)
  echo "<excerpt>"        | python3 trivial_classify.py is-version-bump       -> exit 0 wenn ja, sonst 1
  echo "<tool_input-JSON>"| python3 trivial_classify.py is-version-bump-json  -> wie oben, liest das
                            PreToolUse-stdin-JSON direkt (extrahiert content/new_string/edits selbst)
"""
import json
import re
import sys

# Echte App-Versionsfelder (eng gefasst):
#   - gradle:        versionCode / versionName
#   - package.json:  "version":
#   - Cargo/TOML:    version = ... (am Zeilenanfang, NICHT minSdkVersion/compileSdkVersion etc.)
#   - MSBuild/.csproj: <Version>1.2.3</Version> (+ Assembly/File/Package/AssemblyInformational),
#                    NUR mit reiner Versionsnummer als Inhalt -> <TargetFramework>net8.0</...> faellt NICHT darunter.
_VERSION_NAMED = re.compile(r'version(Code|Name)', re.IGNORECASE)
_VERSION_JSON = re.compile(r'"version"\s*:', re.IGNORECASE)
_VERSION_TOML = re.compile(r'^\s*version\s*=', re.IGNORECASE)
_VERSION_MSBUILD = re.compile(
    r'<\s*(?:Assembly|File|Package|AssemblyInformational)?Version\s*>\s*[\d.]+\s*'
    r'<\s*/\s*(?:Assembly|File|Package|AssemblyInformational)?Version\s*>',
    re.IGNORECASE,
)
# Info.plist (macOS/iOS): zweizeiliger Aufbau — <key>CFBundleVersion</key> bzw.
# <key>CFBundleShortVersionString</key> gefolgt von <string>1.15</string>. Beide Zeilenarten zaehlen
# als Versionszeile, damit ein reiner Bundle-Version-Bump (auch nur die <string>-Wertzeile) trivial ist.
# Der String-Wert MUSS rein numerisch (Ziffern+Punkte) sein -> ein <string>Foo</string> faellt NICHT darunter.
_VERSION_PLIST_KEY = re.compile(r'<\s*key\s*>\s*CFBundle(?:Short)?Version(?:String)?\s*<\s*/\s*key\s*>', re.IGNORECASE)
_VERSION_PLIST_VAL = re.compile(r'^\s*<\s*string\s*>\s*[\d.]+\s*<\s*/\s*string\s*>\s*$', re.IGNORECASE)

_STRING_RE = re.compile(r'(<string\b|</string>|<plurals\b|getString\(|stringResource\(|R\.string\.)', re.IGNORECASE)
_IMPORT_RE = re.compile(r'^\s*(import\s|package\s|using\s|#include|from\s+\S+\s+import\s)')
_COMMENT_RE = re.compile(r'^\s*(//|#|<!--|-->|/\*|\*/|\*)')


def _sig_lines(excerpt):
    return [ln.strip() for ln in (excerpt or "").splitlines() if ln.strip()]


def _is_version_line(ln):
    return bool(
        _VERSION_NAMED.search(ln)
        or _VERSION_JSON.search(ln)
        or _VERSION_TOML.match(ln)
        or _VERSION_MSBUILD.search(ln)
        or _VERSION_PLIST_KEY.search(ln)
        or _VERSION_PLIST_VAL.match(ln)
    )


def is_version_bump(excerpt):
    """True, wenn JEDE nicht-leere Zeile ein echtes App-Versionsfeld setzt (reiner Bump)."""
    lines = _sig_lines(excerpt)
    return bool(lines) and all(_is_version_line(ln) for ln in lines)


def excerpt_from_tool_input(tool_input):
    """Den change_excerpt aus einem PreToolUse-tool_input ziehen (content/new_string/edits[].new_string).

    EINE Quelle auch fuer die Extraktion, damit der Guard sie nicht pro Sprache nachbauen muss.
    """
    ti = tool_input or {}
    ex = ti.get("content") or ti.get("new_string") or ""
    if not ex and ti.get("edits"):
        ex = "\n".join((e.get("new_string") or "") for e in ti["edits"])
    return ex


def classify(excerpt):
    """Konservative Trivial-Einstufung eines change_excerpt. Im Zweifel 'logic' (berechtigt)."""
    lines = _sig_lines(excerpt)
    if not lines:
        return "leer"
    if all(_is_version_line(ln) for ln in lines):
        return "version-bump"
    if all(_STRING_RE.search(ln) for ln in lines):
        return "string-only"
    if all(_IMPORT_RE.match(ln) for ln in lines):
        return "import-only"
    if all(_COMMENT_RE.match(ln) for ln in lines):
        return "comment-whitespace"
    return "logic"


def main(argv):
    try:
        sys.stdout.reconfigure(encoding="utf-8")
    except Exception:
        pass
    mode = argv[1] if len(argv) > 1 else "classify"
    raw = sys.stdin.read()
    if mode == "is-version-bump-json":
        # stdin = PreToolUse-tool_input-JSON (wie der Guard es hat) -> excerpt selbst extrahieren.
        try:
            d = json.loads(raw or "{}")
        except Exception:
            return 1
        return 0 if is_version_bump(excerpt_from_tool_input(d.get("tool_input"))) else 1
    if mode == "is-version-bump":
        return 0 if is_version_bump(raw) else 1
    print(classify(raw))
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
