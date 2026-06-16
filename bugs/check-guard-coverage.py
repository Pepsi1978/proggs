#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""check-guard-coverage.py — Selbsttest fuer das Bug-Almanach-System.

Prueft, ob jeder Almanach in ``bugs/`` vom ``bug-almanac-guard``-Hook ueber ein
Dateimuster/Inhalts-Signal ERZWUNGEN wird (Stufe-1-Erkennung). Ein Almanach ohne
Mapping wuerde zwar existieren, aber NIE automatisch ausgeloest werden, wenn man
im Bereich arbeitet — er rutscht still durch oder wird vom uebergeordneten
Sprach-Almanach verdeckt.

Drei Kategorien:
  [OK]      Almanach hat ein file=-Mapping im Guard -> wird erzwungen.
  [BEWUSST] Querschnitts-/Konzept-Almanach (apis/, agents/, ...) -> per Design
            NICHT datei-erzwungen, nur ueber Index + Stichworte (Allowlist unten).
  [LUECKE]  Almanach existiert, hat aber KEIN Mapping und ist NICHT in der
            Allowlist -> faellt durch (im Guard ein Signal ergaenzen ODER, falls
            wirklich Querschnitt, in die Allowlist aufnehmen).

Poka-Yoke Stufe 3 (Direktive #3): faellt sofort auf, wenn ein kuenftiger neuer
Almanach ohne Erzwingung angelegt wird — statt monatelang unbemerkt durchzurutschen.

Rein lesend. Immer exit 0 (blockiert nie eine Session) — analog check-coupling.py.
Manuell oder im Wartungslauf ausfuehren:  python3 bugs/check-guard-coverage.py
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

# Bewusst NICHT datei-erzwungene Almanache (Querschnitt/Konzept; in bugs/README.md so
# dokumentiert). Schluessel = Almanach-Dateiname ohne .md. Wer hier steht, wird als
# [BEWUSST] gewertet, nicht als Luecke. Bei einem neuen Querschnitts-Almanach hier ergaenzen.
INTENTIONALLY_UNMAPPED = {
    # apis/ — Querschnitt (kein sauberes Datei-Pattern; ueber Index + Stichworte gefunden)
    "api-integration-general", "openai-api", "anthropic-api", "google-gemini-api",
    "groq-api", "openrouter-api", "xai-grok-api", "mistral-api", "deepseek-api",
    "local-openai-compatible", "other-llm-apis", "oauth-device-code",
    "cli-impersonation-subscription-auth", "tts-provider",
    # android-build/ — Release-PROZESS (Play Console, kein Datei-Edit; AndroidManifest.xml/build.gradle
    # teilen sich android-platform/gradle — ein eigener Datei-Trigger wuerde diese kapern)
    "play-store-release",
    # agents/ — Konzept/Orchestrierung (kein sauberes Datei-Pattern)
    "orchestrator-agent",
    # claude-tooling/ — Konzept (Desktop-App-Tabs / Harness-Selbstverbesserung, kein Datei-Pattern)
    # cowork-git-push: Cowork-Git-Workflow-Querschnitt (kein Datei-Pattern; greift ueber cowork-git.sh-Nutzung)
    # cowork-scheduled-tasks: Cowork-Aufgabenplanung — Prozess/Workflow-Querschnitt (kein Datei-Pattern)
    "cowork", "claude-code-desktop-vs-cli", "agent-knowledge-system", "cowork-git-push",
    "cowork-scheduled-tasks",
    # desktop/ — laeuft bewusst ueber die wake-word/groq/dotnet-Zweige
    "voice-pipeline",
    # assets/ — engine-uebergreifendes Konzept (Maps/HDRs, kein eindeutiges Pattern)
    "3d-visual-quality",
}

# Nicht-Almanach-Dateien direkt in bugs/ (kategorielos) — vom Abgleich ausgenommen.
NON_ALMANAC = {"readme", "system", "offene-almanache-prompts"}


def find_repo_root() -> Path:
    """Skript liegt in <repo>/bugs/ -> Repo-Wurzel ist parent.parent."""
    return Path(__file__).resolve().parent.parent


def collect_almanacs(bugs_dir: Path) -> list[str]:
    """Alle echten Almanach-Schluessel (Dateiname ohne .md, lowercase) in bugs/<kategorie>/."""
    keys: set[str] = set()
    for p in bugs_dir.rglob("*.md"):
        rel = p.relative_to(bugs_dir).as_posix()
        if "/" not in rel:           # direkt in bugs/ (README/SYSTEM/...) -> kein Almanach
            continue
        key = p.stem.lower()
        if key in NON_ALMANAC:
            continue
        keys.add(key)
    return sorted(keys)


def collect_mapped_files(guard_path: Path) -> set[str]:
    """Jeden als file gesetzten Almanach-Dateinamen aus dem Guard-Hook extrahieren.

    Deckt beide Schreibweisen ab: PowerShell ``$file = 'x.md'`` und Bash ``file="x.md"``.
    Rueckgabe: Schluessel ohne .md, lowercase.
    """
    text = guard_path.read_text(encoding="utf-8")
    pat = re.compile(r"""file\s*=\s*["']([a-z0-9][a-z0-9._-]*\.md)["']""")
    return {m.group(1)[:-3].lower() for m in pat.finditer(text)}


def main() -> int:
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass

    root = find_repo_root()
    bugs_dir = root / "bugs"
    # Guard bevorzugt aus dem AKTIVEN Hook lesen, sonst die Repo-Spiegelung (beide Sprachen).
    candidates = [
        Path.home() / ".claude" / "hooks" / "bug-almanac-guard.ps1",
        root / "claude-code-setup" / "hooks" / "bug-almanac-guard.ps1",
        Path.home() / ".claude" / "hooks" / "bug-almanac-guard.sh",
        root / "claude-code-setup" / "hooks" / "bug-almanac-guard.sh",
    ]
    guard = next((p for p in candidates if p.exists()), None)
    if guard is None:
        print("[FEHLER] Kein bug-almanac-guard-Hook gefunden — nichts zu pruefen.")
        return 0
    if not bugs_dir.is_dir():
        print(f"[FEHLER] bugs/ nicht gefunden unter {bugs_dir}")
        return 0

    almanacs = collect_almanacs(bugs_dir)
    mapped = collect_mapped_files(guard)

    ok: list[str] = []
    bewusst: list[str] = []
    luecke: list[str] = []
    for key in almanacs:
        if key in mapped:
            ok.append(key)
        elif key in INTENTIONALLY_UNMAPPED:
            bewusst.append(key)
        else:
            luecke.append(key)

    print(f"Guard-Coverage-Check — Quelle: {guard.name}")
    print(
        f"Almanache gesamt: {len(almanacs)}  |  erzwungen: {len(ok)}  |  "
        f"bewusst nicht: {len(bewusst)}  |  LUECKEN: {len(luecke)}"
    )
    print("")
    for key in ok:
        print(f"  [OK]      {key}")
    for key in bewusst:
        print(f"  [BEWUSST] {key}")
    for key in luecke:
        print(f"  [LUECKE]  {key}  -> existiert, aber kein Mapping im Guard und nicht in der Allowlist")

    # Allowlist-Eintraege ohne zugehoerigen Almanach -> Allowlist veraltet (Hinweis, keine Luecke).
    stale = sorted(INTENTIONALLY_UNMAPPED - set(almanacs))
    if stale:
        print("")
        for key in stale:
            print(f"  [HINWEIS] Allowlist-Eintrag ohne Almanach: {key} (veraltet?)")

    print("")
    if luecke:
        print(f"ERGEBNIS: {len(luecke)} Luecke(n) — diese Almanache werden nie automatisch ausgeloest.")
        print("          Im bug-almanac-guard ein Dateimuster/Inhalts-Signal ergaenzen")
        print("          ODER (bei echtem Querschnitt) in INTENTIONALLY_UNMAPPED aufnehmen.")
    else:
        print("ERGEBNIS: Keine Luecken — jeder Almanach wird erzwungen oder ist bewusst als Querschnitt gelistet.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
