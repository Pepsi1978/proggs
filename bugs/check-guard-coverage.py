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
    "cli-impersonation-subscription-auth", "tts-provider", "firecrawl",
    # android-build/ — Release-PROZESS (Play Console, kein Datei-Edit; AndroidManifest.xml/build.gradle
    # teilen sich android-platform/gradle — ein eigener Datei-Trigger wuerde diese kapern)
    "play-store-release",
    # agents/ — Konzept/Orchestrierung/Loop-Design (kein sauberes Datei-Pattern)
    "orchestrator-agent", "loop-engineering",
    # claude-tooling/ — Konzept (Desktop-App-Tabs / Harness-Selbstverbesserung, kein Datei-Pattern)
    # cowork-git-push: Cowork-Git-Workflow-Querschnitt (kein Datei-Pattern; greift ueber cowork-git.sh-Nutzung)
    # cowork-scheduled-tasks: Cowork-Aufgabenplanung — Prozess/Workflow-Querschnitt (kein Datei-Pattern)
    "cowork", "claude-code-desktop-vs-cli", "agent-knowledge-system", "cowork-git-push",
    "cowork-scheduled-tasks",
    # openrouter-claude-code: Provider-Setup lebt im settings.json-env-Block (triggert bereits
    # claudeconfig) bzw. .claude-code-router/config.json — kein eigenes sauberes Datei-Pattern.
    # Bewusst Querschnitt (Einzelpruefung 2026-06-20).
    "openrouter-claude-code",
    # opencode/ — server-agent-remote-mcp: CLIENT-/Operator-Konzept (eigenes Gehirn als Remote-MCP +
    # server-only-Agent) ueber OpenCode UND Claude Code. Die betroffenen Dateien (opencode.json/.jsonc,
    # .mcp.json, settings.json) triggern bereits opencode-cli.md bzw. claudeconfig — ein eigener Trigger
    # wuerde die kapern. Auffindung ueber Index + Stichworte + Querverweis aus opencode-cli.md. Querschnitt
    # (Einzelpruefung 2026-06-24).
    "server-agent-remote-mcp",
    # server/ — vps-hosting ist reine Anbieter-/Prozess-Wahl (Hostinger/Hetzner, root, Docker),
    # kein editierbares Datei-Pattern -> Querschnitt (Einzelpruefung 2026-06-20). wireguard ist
    # dagegen ueber .conf-Inhalt im Guard registriert (kein Querschnitt).
    "vps-hosting",
    # server/ (Second-Brain-Stack, 2026-06-22): Betriebs-/Lib-Bereiche OHNE lokales Datei-Pattern.
    # Arbeit laeuft per SSH am Server bzw. im second-brain-server/-Code (compose.yaml/app.py triggern
    # andere Bereiche, nicht diese). Auffindung ueber Index + Stichworte. Bewusster Querschnitt.
    "self-hosted-ai-agent-server", "mem0", "qdrant", "samba-wireguard",
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
        if rel.endswith("-kurzcheck.md"):   # abgeleitete Kurzcheck-Datei, kein eigener Almanach
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


def collect_bp_keys(bp_dir: Path) -> dict[str, str]:
    """Alle Best-Practices-Schluessel in best-practices/<kategorie>/ sammeln.

    Schluessel = Dateiname ohne .md, ohne fuehrendes ``best-practices-`` (Abwaerts-
    kompatibilitaet zur alten Namensform), lowercase. Nur Dateien in Unterordnern;
    README/SYSTEM/_-Dateien werden ausgenommen. Rueckgabe: key -> relativer Pfad
    (fuer eine sprechende Ausgabe).
    """
    keys: dict[str, str] = {}
    for p in bp_dir.rglob("*.md"):
        rel = p.relative_to(bp_dir).as_posix()
        if "/" not in rel:           # direkt in best-practices/ (README/SYSTEM) -> kein Paar
            continue
        key = p.stem.lower()
        if key.startswith("best-practices-"):
            key = key[len("best-practices-"):]
        if key in NON_ALMANAC or key.startswith("_"):
            continue
        keys.setdefault(key, "best-practices/" + rel)
    return keys


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

    # ── Best-Practices-Coverage (zweite Seite der Medaille) ──────────────────────
    # Der Guard erzwingt eine best-practices-<key>.md NUR nach dem Lesen des gleichnamigen
    # Almanachs (erst Almanach, dann BP). Eine BP wird also automatisch getriggert, wenn ihr
    # Almanach datei-erzwungen ist (OK). Ist der Almanach eine LUECKE, wird auch die BP nie
    # ausgeloest. Eine BP ganz ohne gleichnamigen Almanach ist Zusatz-Referenz (kein Paar,
    # vom Guard bewusst nicht getriggert — kein Fehler).
    bp_dir = root / "best-practices"
    bp_luecke: list[tuple[str, str]] = []
    bp_zusatz: list[tuple[str, str]] = []
    if bp_dir.is_dir():
        ok_set, bewusst_set, luecke_set = set(ok), set(bewusst), set(luecke)
        bp_keys = collect_bp_keys(bp_dir)
        bp_ok: list[str] = []
        bp_bewusst: list[str] = []
        for key, rel in sorted(bp_keys.items()):
            if key in ok_set:
                bp_ok.append(key)
            elif key in bewusst_set:
                bp_bewusst.append(key)
            elif key in luecke_set:
                bp_luecke.append((key, rel))   # Almanach-Luecke -> BP wird nie getriggert
            else:
                bp_zusatz.append((key, rel))   # kein gleichnamiger Almanach -> Zusatz-Referenz

        print("")
        print("--- Best-Practices-Coverage (erst Almanach, dann Best Practices) ---")
        print(
            f"BP-Dateien gesamt: {len(bp_keys)}  |  getriggert: {len(bp_ok)}  |  "
            f"Querschnitt: {len(bp_bewusst)}  |  BP-LUECKEN: {len(bp_luecke)}  |  "
            f"Zusatz (kein Almanach-Paar): {len(bp_zusatz)}"
        )
        for key, rel in bp_luecke:
            print(f"  [BP-LUECKE]  {rel}  -> Almanach '{key}' wird nicht erzwungen, also wird auch diese BP nie ausgeloest")
        for key, rel in bp_zusatz:
            print(f"  [BP-ZUSATZ]  {rel}  -> Referenz ohne gleichnamigen Almanach (vom Guard bewusst nicht getriggert)")

    # ── Gesamt-Ergebnis ──────────────────────────────────────────────────────────
    print("")
    total_luecken = len(luecke) + len(bp_luecke)
    if total_luecken:
        if luecke:
            print(f"ERGEBNIS: {len(luecke)} Almanach-Luecke(n) — werden nie automatisch ausgeloest.")
            print("          Im bug-almanac-guard ein Dateimuster/Inhalts-Signal ergaenzen")
            print("          ODER (bei echtem Querschnitt) in INTENTIONALLY_UNMAPPED aufnehmen.")
        if bp_luecke:
            print(f"ERGEBNIS: {len(bp_luecke)} Best-Practices-Luecke(n) — meist Folge einer Almanach-Luecke;")
            print("          sobald der zugehoerige Almanach erzwungen wird, wird auch die BP getriggert.")
    else:
        print("ERGEBNIS: Keine Luecken — jeder Almanach (und seine Best-Practices) wird erzwungen oder ist bewusst Querschnitt.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
