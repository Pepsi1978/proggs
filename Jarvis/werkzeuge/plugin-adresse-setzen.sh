#!/bin/bash
# Trägt die Jarvis-Plugin-Adresse aus ~/SK/Jarvis/plugin-url.txt in alle Programme am Mac ein (nach „Plugin-Adresse
# erneuern“ in Jarvis erneut ausführen): Claude Code (~/.claude.json und die Profile des OpenLauncherMac), Codex CLI und
# Codex Desktop (~/.codex/config.toml und das Codex-Zuhause des OpenLauncherMac), OpenCode und Claude Desktop.
# Andere Server bleiben unberührt; vor jeder Änderung eine Sicherung nach ~/SK/Jarvis/<programm>-mac-<datum>.bak.
# Die Adresse ist ein Geheimnis: Das Skript zeigt sie nur gekürzt. Mehrfach ausführbar.
set -euo pipefail
URL_DATEI="$HOME/SK/Jarvis/plugin-url.txt"
[ -f "$URL_DATEI" ] || { echo "FEHLER: $URL_DATEI fehlt (vom Windows-PC kopieren)."; exit 1; }
exec python3 -I - "$URL_DATEI" <<'PY'
import json, os, re, shutil, subprocess, sys, time
from pathlib import Path

url = Path(sys.argv[1]).read_text(encoding="utf-8").strip()
if not re.fullmatch(r"https://[^/\s]+/j/[^/\s]+/mcp", url):
    sys.exit("FEHLER: plugin-url.txt hat nicht die Form https://<host>/j/<Geheimnis>/mcp")
kurz = re.sub(r"/j/([^/]{4})[^/]*/mcp$", r"/j/\1…/mcp", url)
home = Path.home()
sk = home / "SK" / "Jarvis"
datum = time.strftime("%Y%m%d-%H%M%S")


def sichere(datei: Path, programm: str) -> None:
    if datei.exists():
        shutil.copy2(datei, sk / f"{programm}-mac-{datum}.bak")


# 1. Claude Code: Benutzerbereich jedes Config-Ordners (CLAUDE_CONFIG_DIR) und der Standard ~/.claude.
ordner = [None] + [str(home / "proggs/OpenLauncher/Profiles/ClaudeCodeMac" / p) for p in ("minimal", "standard", "strict")]
for nr, d in enumerate(ordner):
    umgebung = dict(os.environ)
    umgebung.pop("CLAUDE_CONFIG_DIR", None)
    if d:
        umgebung["CLAUDE_CONFIG_DIR"] = d
    json_datei = Path(d, ".claude.json") if d else home / ".claude.json"
    sichere(json_datei, f"claude-{('profil-' + Path(d).name) if d else 'home'}")
    subprocess.run(["claude", "mcp", "remove", "--scope", "user", "jarvis"], env=umgebung, capture_output=True)
    r = subprocess.run(["claude", "mcp", "add", "--transport", "http", "--scope", "user", "jarvis", url],
                       env=umgebung, capture_output=True, text=True)
    print(f"Claude Code {d or '~/.claude'}: {'ok' if r.returncode == 0 else 'FEHLER ' + r.stderr.strip()[:200].replace(url, kurz)}")

# 2. Codex CLI und Codex Desktop (~/.codex) sowie das Codex-Zuhause des OpenLauncherMac.
block = f'[mcp_servers.jarvis]\nstartup_timeout_sec = 30\ntool_timeout_sec = 120\nurl = "{url}"\n'
for toml in (home / ".codex/config.toml", home / "Library/Application Support/OpenLauncher/codex-home/config.toml"):
    if not toml.exists():
        continue
    sichere(toml, "codex" if ".codex" in str(toml) else "codex-openlauncher")
    text = toml.read_text(encoding="utf-8")
    # Vorhandenen Block samt Untertabellen [mcp_servers.jarvis.*] ersetzen; Leerzeilen und Kommentare direkt vor der
    # nächsten fremden Tabelle gehören zu ihr und bleiben. Ohne Block: anhängen.
    zeilen = text.split("\n")
    kopf = next((i for i, z in enumerate(zeilen) if z.strip() == "[mcp_servers.jarvis]"), None)
    if kopf is None:
        text = text.rstrip("\n") + "\n\n" + block
    else:
        ende = kopf + 1
        while ende < len(zeilen) and not (zeilen[ende].startswith("[") and not zeilen[ende].startswith("[mcp_servers.jarvis")):
            ende += 1
        while ende > kopf + 1 and ende < len(zeilen) and (not zeilen[ende - 1].strip() or zeilen[ende - 1].lstrip().startswith("#")):
            ende -= 1
        text = "\n".join(zeilen[:kopf] + block.rstrip("\n").split("\n") + [""] + zeilen[ende:])
    toml.write_text(text, encoding="utf-8")
    print(f"Codex {toml}: ok")

# 3. OpenCode (JSONC mit Kommentaren: nur den jarvis-Eintrag ersetzen bzw. hinter "mcp": { einfügen).
oc = home / ".config/opencode/opencode.jsonc"
if oc.exists():
    sichere(oc, "opencode")
    text = oc.read_text(encoding="utf-8")
    eintrag = f'"jarvis": {{ "type": "remote", "url": "{url}", "enabled": true, "oauth": false, "timeout": 120000 }}'
    # Vorhandenen Eintrag bis zur passenden schließenden Klammer ersetzen (auch mit verschachtelten Objekten wie headers).
    treffer = re.search(r'"jarvis"\s*:\s*\{', text)
    if treffer:
        tiefe, i, in_text = 0, treffer.end() - 1, False
        while i < len(text):
            zeichen = text[i]
            if in_text:
                if zeichen == "\\":
                    i += 1
                elif zeichen == '"':
                    in_text = False
            elif zeichen == '"':
                in_text = True
            elif zeichen == "{":
                tiefe += 1
            elif zeichen == "}":
                tiefe -= 1
                if tiefe == 0:
                    break
            i += 1
        text = text[:treffer.start()] + eintrag + text[i + 1:]
    elif re.search(r'"mcp"\s*:\s*\{', text):
        text = re.sub(r'("mcp"\s*:\s*\{)', lambda m: m.group(1) + "\n    " + eintrag + ",", text, count=1)
    else:
        sys.exit('FEHLER: opencode.jsonc ohne "mcp"-Block, bitte von Hand eintragen')
    oc.write_text(text, encoding="utf-8")
    print(f"OpenCode {oc}: ok")

# 4. Claude Desktop: stdio-Brücke über mcp-remote (absoluter npx-Pfad, die App kennt den Shell-PATH nicht).
cd = home / "Library/Application Support/Claude/claude_desktop_config.json"
if cd.parent.exists():
    sichere(cd, "claude-desktop")
    daten = json.loads(cd.read_text(encoding="utf-8")) if cd.exists() else {}
    npx = shutil.which("npx") or "/opt/homebrew/bin/npx"
    daten.setdefault("mcpServers", {})["jarvis"] = {"command": npx, "args": ["-y", "mcp-remote", url],
                                                     "env": {"NODE_OPTIONS": "--dns-result-order=ipv4first",
                                                             "PATH": f"{Path(npx).parent}:/usr/bin:/bin"}}
    cd.write_text(json.dumps(daten, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Claude Desktop: ok (neu starten, damit es greift)")

print(f"Plugin-Adresse {kurz} eingetragen.")
PY
