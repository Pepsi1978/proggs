#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""check-version-anchor.py — Versions-Anker-Konsistenz der software-gebundenen Almanache.

Zweck (Welle 3, 2026-06-15): Das Datums-basierte Stand-Verfall (health.py, 180d) erkennt
ALTE Almanache. Dieser Check ergaenzt die VERSIONS-Sicht: Ein Almanach kann < 180d frisch
sein, aber die Software hat schon eine neue Major/Minor-Version → Re-Recherche faellig,
obwohl das Datum noch gruen ist (z.B. Claude Code: 18 Patches in 14 Tagen).

Zwei Pruefungen je Eintrag der kuratierten Tabelle ANCHORS:
  1. ANKER-VOLLSTAENDIGKEIT: traegt der Almanach ein maschinenlesbares Feld
     `> **Anker:** <label>=<version>` (Konvention, siehe bugs/SYSTEM.md)? Fehlt es → WARN.
  2. LIVE-ABGLEICH (nur wo installiert == projekt-relevant, z.B. claude-code/python):
     Live-Version per Befehl ermitteln, Major.Minor mit dem Anker vergleichen. Abweichung → WARN.
     Bei projekt-gebundenen Bereichen (kotlin/gradle/compose/billing/dotnet) ist die INSTALLIERTE
     Tool-Version NICHT die relevante (das Projekt pinnt eine eigene via Gradle/.csproj) → KEIN
     Live-Abgleich, nur Anker-Vollstaendigkeit. Ehrlichkeit vor Falschalarm.

Graceful: Tool fehlt → Live-Abgleich uebersprungen (kein Fehler). Exit 0 immer (Report-Tool).
Wird von bugs/health.py mitgefuehrt (SessionStart via invariant-check).
"""
import os
import re
import subprocess
import sys

BUGS_DIR = os.path.dirname(os.path.abspath(__file__))

# Kuratierte Tabelle der SOFTWARE-gebundenen Almanache.
#   rel        : Pfad relativ zu bugs/
#   label      : Anker-Label (muss im `Anker:`-Feld vor dem '=' stehen)
#   live       : (cmd-Liste, regex) fuer Live-Abgleich ODER None (nur Anker-Vollstaendigkeit)
# live NUR setzen, wo die INSTALLIERTE Version == die fuer den Almanach relevante ist.
ANCHORS = [
    # installiert == relevant → Live-Abgleich
    {"rel": "claude-tooling/claude-hooks.md",  "label": "claude-code", "live": (["claude", "--version"], r"(\d+\.\d+\.\d+)")},
    {"rel": "claude-tooling/claude-config.md", "label": "claude-code", "live": (["claude", "--version"], r"(\d+\.\d+\.\d+)")},
    {"rel": "claude-tooling/python-windows.md","label": "python",      "live": None},  # Almanach-Horizont CPython 3.13/3.14; lokaler python-Shim kann projektfremd sein
    {"rel": "claude-tooling/openrouter-claude-code.md","label": "claude-code", "live": (["claude", "--version"], r"(\d+\.\d+\.\d+)")},
    {"rel": "opencode/opencode-cli.md",        "label": "opencode",    "live": (["opencode", "--version"], r"(\d+\.\d+\.\d+)")},
    {"rel": "web/typescript.md",               "label": "typescript",  "live": None},  # tsc-Version != node; projekt-gepinnt
    {"rel": "server/docker.md",                "label": "docker",       "live": None},  # server-gebunden (VPS), lokal kein docker -> kein Live-Abgleich
    {"rel": "server/reverse-proxy-tls.md",     "label": "caddy",        "live": None},  # geplant/server-gebunden, lokal kein caddy -> kein Live-Abgleich
    {"rel": "server/fastapi.md",               "label": "fastapi",      "live": None},  # projekt-gepinnt in requirements.txt -> kein Live-Abgleich
    {"rel": "opencode/server-agent-remote-mcp.md", "label": "opencode", "live": None},  # CLI-Client-Konzept; Live-Abgleich macht opencode-cli.md, hier nur Anker-Vollstaendigkeit
    # projekt-gebunden (Gradle/.csproj/Info.plist pinnt die Version) → nur Anker-Vollstaendigkeit
    {"rel": "android/kotlin.md",               "label": "kotlin",      "live": None},
    {"rel": "android-build/gradle.md",         "label": "gradle",      "live": None},
    {"rel": "android/jetpack-compose.md",      "label": "compose-bom", "live": None},
    {"rel": "android/firebase-billing.md",     "label": "billing",     "live": None},
    {"rel": "desktop/dotnet-csharp.md",        "label": "dotnet",      "live": None},
    {"rel": "desktop/swift-appkit.md",         "label": "swift",       "live": None},
]


def read_head(path, n=80):
    try:
        with open(path, "r", encoding="utf-8-sig") as f:
            return "".join([next(f, "") for _ in range(n)])
    except Exception:
        return None


def parse_anchor(head, label):
    """Liest `> **Anker:** label=version [, label2=version2]` und gibt die Version zu label."""
    if head is None:
        return None
    m = re.search(r"\*\*Anker:\*\*\s*([^\n]+)", head)
    if not m:
        return None
    field = m.group(1)
    # label=version, kommagetrennt
    for part in field.split(","):
        kv = part.split("=", 1)
        if len(kv) == 2 and kv[0].strip().lower() == label.lower():
            mv = re.search(r"(\d+(?:\.\d+){1,3})", kv[1])
            return mv.group(1) if mv else kv[1].strip()
    return None


def live_version(cmd, regex):
    try:
        out = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8",
                             errors="replace", timeout=20)
        text = (out.stdout or "") + (out.stderr or "")
        m = re.search(regex, text)
        return m.group(1) if m else None
    except FileNotFoundError:
        return "__MISSING__"
    except Exception:
        return None


def major_minor(v):
    parts = v.split(".")
    return ".".join(parts[:2]) if len(parts) >= 2 else v


def main():
    warns = []
    oks = 0
    skipped_tool = []
    for a in ANCHORS:
        path = os.path.join(BUGS_DIR, a["rel"])
        if not os.path.isfile(path):
            warns.append("%s: Almanach-Datei fehlt" % a["rel"])
            continue
        head = read_head(path)
        anchor_v = parse_anchor(head, a["label"])
        if anchor_v is None:
            warns.append("%s: Anker-Feld fehlt oder ohne '%s=' (Konvention: '> **Anker:** %s=<version>')"
                         % (a["rel"], a["label"], a["label"]))
            continue
        if a["live"] is None:
            oks += 1  # projekt-gebunden: Anker vorhanden genuegt
            continue
        cmd, regex = a["live"]
        lv = live_version(cmd, regex)
        if lv == "__MISSING__":
            skipped_tool.append("%s (%s nicht im PATH)" % (a["label"], cmd[0]))
            continue
        if lv is None:
            skipped_tool.append("%s (Version nicht parsebar)" % a["label"])
            continue
        if major_minor(lv) != major_minor(anchor_v):
            warns.append("%s: Anker %s=%s, aber LIVE %s -> Re-Recherche pruefen (Major/Minor weicht ab)"
                         % (a["rel"], a["label"], anchor_v, lv))
        else:
            oks += 1

    print("=== Versions-Anker-Check (software-gebundene Almanache) ===")
    print("Geprueft: %d  |  Anker OK: %d  |  Tool uebersprungen: %d  |  WARN: %d"
          % (len(ANCHORS), oks, len(skipped_tool), len(warns)))
    for w in warns:
        print("  [WARN]  " + w)
    for s in skipped_tool:
        print("  [SKIP]  Live-Abgleich nicht moeglich: " + s)
    if not warns:
        print("ERGEBNIS: Keine Anker-Drift - alle software-gebundenen Almanache haben einen Anker"
              + (", Live-Versionen passen." if not skipped_tool else " (einige Live-Abgleiche uebersprungen)."))
    else:
        print("ERGEBNIS: %d Anker-Problem(e) - Anker-Feld ergaenzen ODER (bei Live-Drift) Re-Recherche pruefen." % len(warns))
    return 0


if __name__ == "__main__":
    sys.exit(main())
