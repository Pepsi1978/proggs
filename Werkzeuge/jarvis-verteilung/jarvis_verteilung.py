#!/usr/bin/env python3
"""Jarvis-Verteilung: hält Jarvis auf HP-Laptop, Power-PC und Mac zentral und automatisch aktuell.

Gemeinsame Logik für beide Update-Zentralen (Windows: UpdateZentrale, Mac: UpdaterZentrale-macOS). Drehscheibe ist
origin/main des Monorepos: Jeder Rechner holt dort den neuen Stand, darum holt ein Rechner, der offline war, alles beim
nächsten Lauf nach.

Ablauf je Lauf (`auto`, alle 30 Minuten als Hintergrundaufgabe und bei jeder Prüfung der Karte):
1. Repo holen und (nur vorspulend) angleichen.
2. Portierungsbedarf für die Plattform dieses Rechners bestimmen: jeder Commit seit `basis`, der eine andere
   Jarvis-Plattform ändert (Handy/Tablet `Jarvis/app`, Windows `JarvisWindows`, Mac `JarvisMac`), aber nicht diese.
   Commits mit dem Trailer `Jarvis-Portierung:` sind Portierungsergebnisse und nie Quelle (keine Schleifen).
   Besteht ein Commit nur aus Dateien, die nie portiert werden (konfig.json `nichtPortieren`), wird er ohne CLI mit
   Grund übersprungen.
3. Den Rest beansprucht der Rechner atomar über einen Push ins Verteil-Buch (`daten/`, eine Datei je Quelle und
   Plattform): Wird der Push abgelehnt, hat ein anderer Rechner gewonnen. Danach startet genau eine Claude-Code-Sitzung
   (OpenLauncher, Opus 5.5, Minimal, Standardmodus, Effort High) mit Prüfung, Portierung, Review und Tests.
   Die Sitzung meldet ihr Ergebnis mit `melden` zurück: freigegeben (nur mit Review ok und Tests ok), übersprungen
   (mit Grund) oder blockiert.
4. Installieren, wenn der Quellstand neuer ist als der installierte und nichts sperrt (laufende oder blockierte
   Portierung, aktiver Live-Sprachmodus, abweichender lokaler Stand). Vorher Sicherung, danach Versions- und
   Startprüfung, bei Fehlschlag Wiederherstellung der letzten funktionierenden Fassung.

Befehle: auto, pruefen (wie auto ohne Installation), installieren, status, melden, zuruecksetzen, einrichten.
Ausgabe mit --json maschinenlesbar für die Karten; die letzte Zeile JARVIS_VERTEILUNG_STATUS=… fasst zusammen.
"""
from __future__ import annotations

import argparse
import datetime as dt
import fnmatch
import json
import os
import platform
import shutil
import socket
import subprocess
import sys
import tempfile
import time
from pathlib import Path

HIER = Path(__file__).resolve().parent
REPO = Path(os.environ.get("JARVIS_VERTEILUNG_REPO") or HIER.parent.parent).resolve()
DATEN = "Werkzeuge/jarvis-verteilung/daten"
SIM = os.environ.get("JARVIS_VERTEILUNG_SIM") or ""
IST_WINDOWS = os.name == "nt"
OHNE_FENSTER = 0x08000000 if IST_WINDOWS else 0
TRAILER = "Jarvis-Portierung:"


def konfig() -> dict:
    k = json.loads((HIER / "konfig.json").read_text(encoding="utf-8"))
    if os.environ.get("JARVIS_VERTEILUNG_BASIS"):
        k["basis"] = os.environ["JARVIS_VERTEILUNG_BASIS"]
    return k


K = konfig()


def jetzt() -> str:
    return dt.datetime.now().astimezone().isoformat(timespec="seconds")


def alter_stunden(iso: str | None) -> float:
    if not iso:
        return 1e9
    try:
        return (dt.datetime.now().astimezone() - dt.datetime.fromisoformat(iso)).total_seconds() / 3600
    except ValueError:
        return 1e9


def host() -> str:
    return os.environ.get("JARVIS_VERTEILUNG_HOST") or socket.gethostname()


def zustand_ordner() -> Path:
    if SIM:
        p = Path(SIM) / "zustand" / eigenes_ziel()
    elif IST_WINDOWS:
        p = Path(os.environ.get("LOCALAPPDATA", str(Path.home()))) / "JarvisVerteilung"
    else:
        p = Path.home() / "Library/Application Support/JarvisVerteilung"
    p.mkdir(parents=True, exist_ok=True)
    return p


def log(text: str) -> None:
    zeile = f"{jetzt()} [{eigenes_ziel()}] {text}"
    print(zeile, file=sys.stderr)
    try:
        with open(zustand_ordner() / "verlauf.log", "a", encoding="utf-8") as f:
            f.write(zeile + "\n")
    except OSError:
        pass


def eigenes_ziel() -> str:
    z = os.environ.get("JARVIS_VERTEILUNG_ZIEL")
    if z:
        return z
    if sys.platform == "darwin":
        return "mac"
    if IST_WINDOWS:
        lokal = Path(os.environ.get("LOCALAPPDATA", ""))
        if (lokal / "Programs/JarvisWindowsPower").exists() or "POWER" in os.environ.get("COMPUTERNAME", "").upper():
            return "power"
        return "hp"
    return "unbekannt"


def plattform_von(ziel: str) -> str:
    return K["ziele"][ziel]["plattform"]


# ---------------------------------------------------------------- git

def git(*args: str, eingabe: str | None = None, env: dict | None = None, pruefen: bool = True, zeit: int = 120) -> str:
    umgebung = dict(os.environ)
    umgebung["GIT_TERMINAL_PROMPT"] = "0"
    if env:
        umgebung.update(env)
    r = subprocess.run(["git", "-C", str(REPO), *args], input=eingabe, capture_output=True, text=True,
                       encoding="utf-8", errors="replace", env=umgebung, timeout=zeit, creationflags=OHNE_FENSTER)
    if pruefen and r.returncode != 0:
        raise RuntimeError(f"git {' '.join(args[:3])}: {(r.stderr or r.stdout).strip()[:400]}")
    return r.stdout


def holen() -> bool:
    try:
        git("fetch", "--quiet", "origin", "main", zeit=180)
        return True
    except Exception as e:  # offline: mit dem letzten bekannten Stand weiterarbeiten
        log(f"fetch fehlgeschlagen: {e}")
        return False


def vorspulen() -> str:
    """Nur vorspulen, nie mergen. Klappt es nicht (lokale Änderungen im Weg), bleibt der Arbeitsbaum unberührt."""
    r = subprocess.run(["git", "-C", str(REPO), "merge", "--ff-only", "--quiet", "origin/main"], capture_output=True,
                       text=True, encoding="utf-8", errors="replace", creationflags=OHNE_FENSTER, timeout=180)
    return "" if r.returncode == 0 else (r.stderr or r.stdout).strip()[:300]


def buch_lesen() -> dict[str, dict]:
    """Das Verteil-Buch so, wie es auf origin/main steht (für alle Rechner derselbe Stand)."""
    namen = [n for n in git("ls-tree", "-r", "--name-only", "origin/main", "--", DATEN + "/").splitlines()
             if n.endswith(".json")]
    if not namen:
        return {}
    roh = subprocess.run(["git", "-C", str(REPO), "cat-file", "--batch"], input="".join(f"origin/main:{n}\n" for n in namen).encode(),
                         capture_output=True, creationflags=OHNE_FENSTER, timeout=120).stdout
    buch, pos = {}, 0
    for n in namen:
        kopf_ende = roh.index(b"\n", pos)
        teile = roh[pos:kopf_ende].split()
        pos = kopf_ende + 1
        if len(teile) < 3 or teile[1] != b"blob":
            continue
        laenge = int(teile[2])
        try:
            buch[n[len(DATEN) + 1:]] = json.loads(roh[pos:pos + laenge].decode("utf-8"))
        except ValueError:
            pass
        pos += laenge + 1
    return buch


def buch_schreiben(aenderungen: dict[str, dict | None], nachricht: str, bedingung=None) -> tuple[bool, str]:
    """Schreibt Einträge ins Verteil-Buch, ohne Arbeitsbaum oder Index des Benutzers anzufassen: eigener Index,
    commit-tree auf origin/main, Push. Abgelehnt = jemand war schneller -> neu holen, Bedingung neu prüfen."""
    for versuch in range(5):
        if versuch:
            time.sleep(1 + versuch)
        if not holen() and versuch == 0:
            return False, "offline"
        basis = git("rev-parse", "origin/main").strip()
        if bedingung is not None:
            frei, grund = bedingung(buch_lesen())
            if not frei:
                return False, grund
        fd, index = tempfile.mkstemp(prefix="jv-index-")
        os.close(fd)
        os.unlink(index)
        env = {"GIT_INDEX_FILE": index}
        try:
            git("read-tree", basis, env=env)
            for rel, inhalt in aenderungen.items():
                pfad = f"{DATEN}/{rel}"
                if inhalt is None:
                    git("update-index", "--force-remove", "--", pfad, env=env)
                    continue
                blob = git("hash-object", "-w", "--stdin", eingabe=json.dumps(inhalt, ensure_ascii=False, indent=1) + "\n").strip()
                git("update-index", "--add", "--cacheinfo", f"100644,{blob},{pfad}", env=env)
            baum = git("write-tree", env=env).strip()
        finally:
            try:
                os.unlink(index)
            except OSError:
                pass
        if baum == git("rev-parse", f"{basis}^{{tree}}").strip():
            return True, "unverändert"
        commit = git("commit-tree", baum, "-p", basis, "-m", nachricht).strip()
        r = subprocess.run(["git", "-C", str(REPO), "push", "--quiet", "origin", f"{commit}:refs/heads/main"],
                           capture_output=True, text=True, encoding="utf-8", errors="replace", creationflags=OHNE_FENSTER,
                           timeout=180, env={**os.environ, "GIT_TERMINAL_PROMPT": "0"})
        if r.returncode == 0:
            git("fetch", "--quiet", "origin", "main", pruefen=False)
            return True, "ok"
        log(f"Push ins Verteil-Buch abgelehnt (Versuch {versuch + 1}): {(r.stderr or '').strip()[:200]}")
    return False, "Push ins Verteil-Buch fünfmal abgelehnt"


# ---------------------------------------------------------------- Commits und Portierungsbedarf

def commits() -> list[dict]:
    """Alle Commits seit basis auf origin/main, älteste zuerst, mit Dateien und Portierungs-Trailer."""
    bereich = f"{K['basis']}..origin/main"
    roh = git("log", "--reverse", "--no-merges", "--format=%H%x1f%s%x1f%B%x1e", bereich)
    liste, nach_sha = [], {}
    for block in roh.split("\x1e"):
        block = block.strip("\n")
        if not block:
            continue
        sha, titel, text = (block.split("\x1f") + ["", ""])[:3]
        portiert = []
        for zeile in text.splitlines():
            if zeile.strip().startswith(TRAILER):
                portiert += [s.strip()[:12] for s in zeile.split(":", 1)[1].replace(";", ",").split(",") if s.strip()]
        eintrag = {"sha": sha.strip(), "titel": titel.strip(), "portiert": portiert, "dateien": []}
        liste.append(eintrag)
        nach_sha[eintrag["sha"]] = eintrag
    aktuell = None
    for zeile in git("log", "--reverse", "--no-merges", "--name-only", "--format=@@@%H", bereich).splitlines():
        if zeile.startswith("@@@"):
            aktuell = nach_sha.get(zeile[3:].strip())
        elif zeile.strip() and aktuell is not None:
            aktuell["dateien"].append(zeile.strip())
    return liste


def quellplattformen(dateien: list[str]) -> dict[str, list[str]]:
    treffer: dict[str, list[str]] = {}
    for q, ordner in K["quellen"].items():
        d = [f for f in dateien if any(f.startswith(o) for o in ordner)]
        if d:
            treffer[q] = d
    return treffer


def nie_portieren(datei: str) -> str | None:
    for regel in K["nichtPortieren"]:
        if fnmatch.fnmatchcase(datei, regel["muster"]):
            return regel["grund"]
    return None


def kandidaten(plattform: str, alle: list[dict]) -> list[dict]:
    """Commits, die für diese Plattform geprüft werden müssen, mit Vorentscheidung ohne CLI, wo sie eindeutig ist."""
    ziel_ordner = K["plattformen"][plattform]["ordner"]
    portierte = {s for c in alle if any(f.startswith(ziel_ordner) for f in c["dateien"]) for s in c["portiert"]}
    erg = []
    for c in alle:
        if c["portiert"]:
            continue  # Portierungsergebnis: nie wieder Quelle
        q = quellplattformen(c["dateien"])
        if plattform in q or not q:
            continue  # hier schon mitgeändert oder kein Jarvis-Commit
        dateien = [f for qp, fs in q.items() for f in fs]
        gruende = [nie_portieren(f) for f in dateien]
        vor = None
        if all(gruende):
            vor = "; ".join(sorted(set(gruende)))
        erg.append({**c, "quellen": sorted(q), "relevant": dateien, "vorentscheid": vor,
                    "schonPortiert": c["sha"][:12] in portierte})
    return erg


def schluessel(plattform: str, sha: str) -> str:
    return f"{plattform}/{sha[:12]}.json"


# ---------------------------------------------------------------- Live-Modus, Versionen, Installation

def pid_lebt(pid: int) -> bool:
    if pid <= 0:
        return False
    if IST_WINDOWS:
        r = subprocess.run(["tasklist", "/FI", f"PID eq {pid}", "/NH"], capture_output=True, text=True,
                           creationflags=OHNE_FENSTER)
        return str(pid) in r.stdout
    try:
        os.kill(pid, 0)
        return True
    except ProcessLookupError:
        return False
    except PermissionError:
        return True


def live_aktiv() -> str | None:
    """Läuft gerade ein Live-Sprachgespräch? Die Jarvis-Apps schreiben live.json bei jedem Zustandswechsel."""
    if SIM:
        datei = Path(SIM) / f"{eigenes_ziel()}-live.json"
    elif IST_WINDOWS:
        datei = Path(os.environ.get("APPDATA", "")) / "JarvisWindows" / "live.json"
    else:
        datei = Path.home() / "Library/Application Support/JarvisMac/live.json"
    try:
        d = json.loads(datei.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return None
    if d.get("zustand", "aus") == "aus" or alter_stunden(d.get("zeit")) > 3:
        return None
    if not SIM and not pid_lebt(int(d.get("pid", 0))):
        return None
    return f"Live-Sprachmodus aktiv ({d.get('zustand')})"


def vtupel(v: str) -> tuple:
    try:
        return tuple(int(x) for x in v.strip().split("+")[0].split("."))
    except ValueError:
        return ()


def quellversion(plattform: str, ref: str | None = None) -> str:
    """Version im Repo; mit ref aus git (origin/main), sonst aus dem Arbeitsbaum."""
    if plattform == "windows":
        pfad = "JarvisWindows/JarvisWindows.csproj"
        text = git("show", f"{ref}:{pfad}", pruefen=False) if ref else (REPO / pfad).read_text(encoding="utf-8")
        import re
        m = re.search(r"<Version>([^<]+)</Version>", text)
        return m.group(1).strip() if m else ""
    pfad = "JarvisMac/Info.plist"
    text = git("show", f"{ref}:{pfad}", pruefen=False) if ref else (REPO / pfad).read_text(encoding="utf-8")
    import re
    m = re.search(r"<key>CFBundleShortVersionString</key>\s*<string>([^<]+)</string>", text)
    return m.group(1).strip() if m else ""


def win_ordner(ziel: str) -> Path:
    return Path(os.environ.get("LOCALAPPDATA", "")) / "Programs" / ("JarvisWindowsPower" if ziel == "power" else "JarvisWindowsHP")


def pwsh(skript: str, zeit: int = 120) -> subprocess.CompletedProcess:
    exe = shutil.which("pwsh") or shutil.which("powershell") or "powershell"
    return subprocess.run([exe, "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", skript], capture_output=True,
                          text=True, encoding="utf-8", errors="replace", timeout=zeit, creationflags=OHNE_FENSTER)


def installierte_version(ziel: str) -> str:
    if SIM:
        f = Path(SIM) / f"{ziel}-installiert.txt"
        return f.read_text().strip() if f.exists() else ""
    if ziel == "mac":
        plist = "/Applications/Jarvis.app/Contents/Info.plist"
        if not os.path.exists(plist):
            return ""
        r = subprocess.run(["plutil", "-extract", "CFBundleShortVersionString", "raw", plist], capture_output=True, text=True)
        return r.stdout.strip()
    exe = win_ordner(ziel) / "JarvisWindows.exe"
    if not exe.exists():
        return ""
    r = pwsh(f"(Get-Item -LiteralPath '{exe}').VersionInfo.ProductVersion")
    return r.stdout.strip().split("+")[0]


def laeuft(ziel: str) -> bool:
    if SIM:
        return not (Path(SIM) / f"{ziel}-startfehler").exists()
    if ziel == "mac":
        return subprocess.run(["pgrep", "-x", "Jarvis"], capture_output=True).returncode == 0
    exe = win_ordner(ziel) / "JarvisWindows.exe"
    r = pwsh(f"@(Get-Process JarvisWindows -EA 0 | Where-Object {{ $_.Path -ieq '{exe}' }}).Count")
    return r.stdout.strip() not in ("", "0")


def sichern(ziel: str) -> Path | None:
    s = zustand_ordner() / f"sicherung-{ziel}"
    if s.exists():
        shutil.rmtree(s, ignore_errors=True)
    if SIM:
        quelle = Path(SIM) / f"{ziel}-installiert.txt"
        if not quelle.exists():
            return None
        s.mkdir(parents=True)
        shutil.copy2(quelle, s / "installiert.txt")
        return s
    if ziel == "mac":
        if not os.path.exists("/Applications/Jarvis.app"):
            return None
        s.mkdir(parents=True)
        subprocess.run(["ditto", "/Applications/Jarvis.app", str(s / "Jarvis.app")], check=True)
        return s
    ordner = win_ordner(ziel)
    if not ordner.exists():
        return None
    shutil.copytree(ordner, s / ordner.name)
    return s


def wiederherstellen(ziel: str, s: Path) -> str:
    if SIM:
        shutil.copy2(s / "installiert.txt", Path(SIM) / f"{ziel}-installiert.txt")
        return "wiederhergestellt"
    if ziel == "mac":
        subprocess.run(["pkill", "-x", "Jarvis"], capture_output=True)
        time.sleep(2)
        shutil.rmtree("/Applications/Jarvis.app", ignore_errors=True)
        subprocess.run(["ditto", str(s / "Jarvis.app"), "/Applications/Jarvis.app"], check=True)
        subprocess.run(["open", "/Applications/Jarvis.app"])
        return "wiederhergestellt"
    ordner = win_ordner(ziel)
    exe = ordner / "JarvisWindows.exe"
    r = pwsh(
        f"$exe='{exe}'; if (Test-Path $exe) {{ $b = Start-Process -FilePath $exe -ArgumentList '--beenden' -PassThru; $null = $b.WaitForExit(5000) }};"
        f"Get-Process JarvisWindows -EA 0 | Where-Object {{ $_.Path -ieq $exe }} | Stop-Process -Force;"
        f"robocopy '{s / ordner.name}' '{ordner}' /MIR /R:2 /W:1 /NFL /NDL /NJH /NJS /NP | Out-Null;"
        f"Start-Process -FilePath $exe", zeit=300)
    return "wiederhergestellt" if r.returncode in (0, 1) else f"Wiederherstellung unklar ({r.returncode})"


def bauen_und_installieren(ziel: str) -> tuple[bool, str]:
    if SIM:
        if (Path(SIM) / f"{ziel}-installfehler").exists():
            (Path(SIM) / f"{ziel}-installiert.txt").write_text("kaputt")
            return False, "simulierter Baufehler"
        (Path(SIM) / f"{ziel}-installiert.txt").write_text(quellversion(plattform_von(ziel)))
        return True, "simuliert gebaut"
    if ziel == "mac":
        befehl = ["bash", str(REPO / "JarvisMac/build.sh"), "install"]
    else:
        exe = shutil.which("pwsh") or "powershell"
        befehl = [exe, "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", str(REPO / "JarvisWindows/publish.ps1")]
        if ziel == "power":
            befehl += ["-Geraet", "power"]
    try:
        r = subprocess.run(befehl, capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=1800,
                           cwd=str(REPO), creationflags=OHNE_FENSTER)
    except subprocess.TimeoutExpired:
        return False, "Bau/Installation nach 30 Minuten abgebrochen"
    (zustand_ordner() / f"bau-{ziel}.log").write_text((r.stdout or "") + "\n" + (r.stderr or ""), encoding="utf-8")
    if r.returncode != 0:
        letzte = [z for z in (r.stderr or r.stdout or "").splitlines() if z.strip()][-3:]
        return False, "Bau/Installation fehlgeschlagen: " + " | ".join(letzte)[:300]
    return True, "gebaut und installiert"


# ---------------------------------------------------------------- Sperren

def lauf_sperre() -> Path | None:
    s = zustand_ordner() / "lauf.lock"
    try:
        if s.exists() and time.time() - s.stat().st_mtime > 3600:
            s.unlink()
        fd = os.open(str(s), os.O_CREAT | os.O_EXCL | os.O_WRONLY)
        os.write(fd, str(os.getpid()).encode())
        os.close(fd)
        return s
    except FileExistsError:
        return None


def ist_erhoeht() -> bool:
    if not IST_WINDOWS or SIM:
        return False
    try:
        import ctypes
        return bool(ctypes.windll.shell32.IsUserAnAdmin())
    except Exception:
        return False


def installations_sperre(plattform: str, buch: dict, alle: list[dict]) -> str | None:
    """Warum dieser Rechner jetzt nicht installieren darf (None = frei). Nur geprüfte, freigegebene Stände gehen raus."""
    ausstehend = [e for k, e in buch.items() if k.startswith(plattform + "/") and e.get("status") == "in_arbeit"
                  and alter_stunden(e.get("zeit")) < K["claimStunden"]]
    if ausstehend:
        return f"Portierung läuft ({len(ausstehend)} Änderung(en) auf {ausstehend[0].get('host')}), Verteilung wartet auf Review und Tests"
    ordner = K["plattformen"][plattform]["ordner"]
    portierte = {s: c for c in alle if any(f.startswith(ordner) for f in c["dateien"]) for s in c["portiert"]}
    for k, e in buch.items():
        if k.startswith(plattform + "/") and e.get("status") == "blockiert" and e.get("quelle", "")[:12] in portierte:
            c = portierte[e["quelle"][:12]]
            return f"blockiert: Portierung {c['sha'][:8]} ist ohne Freigabe auf main ({e.get('grund', '')})"
    for c in alle:
        if not c["portiert"] or not any(f.startswith(K["plattformen"][plattform]["ordner"]) for f in c["dateien"]):
            continue
        offen = [s for s in c["portiert"] if (buch.get(schluessel(plattform, s)) or {}).get("status") != "freigegeben"]
        if offen:
            return f"Portierung {c['sha'][:8]} ist noch nicht freigegeben (Review und Tests fehlen)"
    return None


# ---------------------------------------------------------------- Portierungsaufträge

def auftrag_text(plattform: str, faelle: list[dict], ziel: str) -> str:
    pn = K["plattformen"][plattform]["name"]
    ordner = K["plattformen"][plattform]["ordner"].rstrip("/")
    skript = "python" if IST_WINDOWS else "python3"
    melden = f"{skript} Werkzeuge/jarvis-verteilung/jarvis_verteilung.py melden --plattform {plattform}"
    tests = ("`dotnet build JarvisWindows/JarvisWindows.csproj -c Release` und `pwsh -File JarvisWindows/publish.ps1 -NurBauen`, "
             "danach die gebaute App einmal starten und prüfen, dass sie läuft" if plattform == "windows"
             else "`bash JarvisMac/build.sh check` und `bash JarvisMac/build.sh` (Bau), dazu ein Start der gebauten App")
    zeilen = [
        f"Auftrag von der Jarvis-Verteilung der Update-Zentrale ({K['ziele'][ziel]['name']}): Portierungsprüfung für Jarvis {pn} ({ordner}).",
        "REVIEW: ja (das Standard-Code-Review ist festgelegt: ohne Rückfrage ausführen, keine Review- oder Tablet-Frage stellen).",
        "",
        "Diese Commits haben Jarvis auf einer anderen Plattform geändert. Prüfe jeden einzeln:",
    ]
    for c in faelle:
        zeilen.append(f"- {c['sha'][:12]} ({', '.join(c['quellen'])}): {c['titel']}")
        zeilen.append(f"  Dateien: {', '.join(c['relevant'][:25])}{' …' if len(c['relevant']) > 25 else ''}")
    zeilen += [
        "",
        "Gehe so vor (Arbeitsordner ~/proggs, direkt auf main, Regeln der CLAUDE.md und " + ordner + "/CLAUDE.md, Port-Regeln in JarvisWindows/PORT-VERTRAG.md):",
        f"1. Lies je Commit den Diff (`git show <sha>`). Entscheide: Hat die Funktion auf {pn} einen Nutzen und ist sie dort umsetzbar? "
        f"Fehlt sie in {ordner} schon oder ist rein mobil/plattformfremd (Sensoren, Android-Oberfläche, Widgets, Health Connect, Handy-Tunnel …), "
        f"dann nicht portieren und melden: `{melden} --quelle <sha> --ergebnis uebersprungen --grund \"<kurzer Grund>\"`.",
        f"2. Sonst portiere die Funktion nach {ordner} (gleiche Namen und Texte wie im Ursprung). Live-Sprachmodus und bestehende Abläufe dürfen nicht gestört werden.",
        f"3. Plattformtests: {tests}. Danach das Standard-Code-Review (`/code-review`) auf deine Änderung und alle Befunde beheben, dann Tests erneut.",
        "4. Nur wenn Review und Tests grün sind: Version bumpen (echte Systemzeit), funktionen.md der Zielplattform knapp nachziehen, committen mit der "
        f"Commit-Nachricht `{ordner}: <was portiert wurde>` und dem Trailer `{TRAILER} <sha12>[, <sha12>]` als eigene letzte Zeile vor Co-Authored-By, "
        f"rebasen, pushen. Dann `{melden} --quelle <sha>[,<sha>] --ergebnis freigegeben --review ok --tests ok`.",
        f"5. Bleibt das Review oder ein Test rot, NICHT pushen (Änderung verwerfen oder lokal lassen) und melden: `{melden} --quelle <sha> --ergebnis blockiert --review <ok|fehler> --tests <ok|fehler> --grund \"<Grund>\"`. "
        "Die Verteilung blockiert dann diese Änderung, die letzte funktionierende Version bleibt installiert.",
        "6. Jeder aufgeführte Commit braucht genau eine Meldung. Installiert wird nicht von dir, sondern von der Verteilung "
        f"(`{skript} Werkzeuge/jarvis-verteilung/jarvis_verteilung.py auto` darfst du am Ende einmal starten).",
        "7. Keine fremden geänderten Dateien mitcommitten, nur die eigenen Pfade stagen. Kein Android-Bau, kein APK-Update: diese Sitzung ändert nur "
        + ordner + ". Neustart-Hinweise und Freigaben für CLI-kritische Updates nicht anfassen.",
    ]
    return "\n".join(zeilen) + "\n"


def launcher_starten(datei: Path, name: str) -> tuple[bool, str]:
    l = K["launcher"]
    args = ["--jarvis-auftrag", str(datei), "--name", name, "--profil", l["profil"], "--modus", l["modus"],
            "--modell", l["modell"], "--effort", l["effort"], "--ordner", str(REPO)]
    if SIM:
        ziel = Path(SIM) / "gestartet"
        ziel.mkdir(exist_ok=True)
        shutil.copy2(datei, ziel / f"{eigenes_ziel()}-{len(list(ziel.iterdir())) + 1}.md")
        return True, "simuliert gestartet"
    if IST_WINDOWS:
        exe = REPO / "OpenLauncher/bin/Release/net8.0-windows10.0.19041.0/win-x64/OpenLauncher.exe"
    else:
        exe = Path("/Applications/OpenLauncher.app/Contents/MacOS/OpenLauncher")
    if not exe.exists():
        return False, f"OpenLauncher nicht gefunden: {exe}"
    try:
        p = subprocess.Popen([str(exe), *args], stdin=subprocess.DEVNULL, stdout=subprocess.DEVNULL,
                             stderr=subprocess.DEVNULL, cwd=str(exe.parent), creationflags=OHNE_FENSTER)
        code = p.wait(timeout=120)
    except subprocess.TimeoutExpired:
        return True, "OpenLauncher antwortete nicht in zwei Minuten, die Sitzung läuft vermutlich"
    return (code == 0), ("Sitzung gestartet" if code == 0 else f"OpenLauncher-Code {code}")


def portierung(plattform: str, ziel: str, buch: dict, alle: list[dict], darf_starten: bool) -> list[str]:
    meldungen = []
    kand = kandidaten(plattform, alle)
    neu_ueber = {}
    for c in kand:
        k = schluessel(plattform, c["sha"])
        if k in buch:
            continue
        if c["schonPortiert"]:
            continue  # Ergebnis-Commit liegt schon vor; die Freigabe kommt von der Sitzung
        if c["vorentscheid"]:
            neu_ueber[k] = {"quelle": c["sha"], "plattform": plattform, "titel": c["titel"], "quellen": c["quellen"],
                            "status": "uebersprungen", "grund": c["vorentscheid"], "automatisch": True,
                            "host": host(), "zeit": jetzt()}
    if neu_ueber:
        ok, grund = buch_schreiben(neu_ueber, f"Jarvis-Verteilung: {len(neu_ueber)} Änderung(en) für {plattform} ohne Portierung verbucht")
        meldungen.append(f"{len(neu_ueber)} Änderung(en) ohne Portierungsbedarf verbucht" + ("" if ok else f" (nicht gespeichert: {grund})"))
        if ok:
            buch = buch_lesen()

    # abgelaufene Ansprüche: neuer Versuch oder nach maxVersuche blockiert
    abgelaufen = {}
    for k, e in buch.items():
        if k.startswith(plattform + "/") and e.get("status") == "in_arbeit" and alter_stunden(e.get("zeit")) >= K["claimStunden"]:
            if e.get("versuch", 1) >= K["maxVersuche"]:
                abgelaufen[k] = {**e, "status": "blockiert", "grund": f"Portierung {e.get('versuch')}× ohne Ergebnis abgebrochen", "zeit": jetzt()}
    if abgelaufen:
        buch_schreiben(abgelaufen, f"Jarvis-Verteilung: {len(abgelaufen)} hängende Portierung(en) für {plattform} blockiert")
        buch = buch_lesen()

    offen = []
    for c in kand:
        e = buch.get(schluessel(plattform, c["sha"]))
        if c["schonPortiert"] or c["vorentscheid"]:
            continue
        if e is None or (e.get("status") == "in_arbeit" and alter_stunden(e.get("zeit")) >= K["claimStunden"]):
            offen.append(c)
    if not offen:
        return meldungen
    laufend = [e for k, e in buch.items() if k.startswith(plattform + "/") and e.get("status") == "in_arbeit"
               and alter_stunden(e.get("zeit")) < K["claimStunden"]]
    if laufend:
        meldungen.append(f"{len(offen)} Änderung(en) warten, Portierungssitzung läuft bereits auf {laufend[0].get('host')}")
        return meldungen
    if not darf_starten:
        meldungen.append(f"{len(offen)} Änderung(en) brauchen eine Portierungsprüfung (Start im Hintergrundlauf)")
        return meldungen
    if (live := live_aktiv()):
        meldungen.append(f"{len(offen)} Änderung(en) brauchen eine Portierungsprüfung, Start wartet: {live}")
        return meldungen

    paket = offen[:K["maxJeAuftrag"]]
    anspruch = {}
    for c in paket:
        k = schluessel(plattform, c["sha"])
        alt = buch.get(k) or {}
        anspruch[k] = {"quelle": c["sha"], "plattform": plattform, "titel": c["titel"], "quellen": c["quellen"],
                       "status": "in_arbeit", "host": host(), "ziel": ziel, "zeit": jetzt(), "versuch": alt.get("versuch", 0) + 1}

    def frei(b: dict) -> tuple[bool, str]:
        for k in anspruch:
            e = b.get(k)
            if e and not (e.get("status") == "in_arbeit" and alter_stunden(e.get("zeit")) >= K["claimStunden"]):
                return False, f"schon vergeben an {e.get('host')} ({e.get('status')})"
        return True, ""

    ok, grund = buch_schreiben(anspruch, f"Jarvis-Verteilung: Portierungsprüfung {plattform} beansprucht von {host()}", frei)
    if not ok:
        meldungen.append(f"Portierungsprüfung nicht übernommen: {grund}")
        return meldungen
    auftraege = zustand_ordner() / "auftraege"
    auftraege.mkdir(exist_ok=True)
    datei = auftraege / f"portierung-{plattform}-{dt.datetime.now():%Y%m%d-%H%M%S}.md"
    datei.write_text(auftrag_text(plattform, paket, ziel), encoding="utf-8")
    gestartet, info = launcher_starten(datei, f"Jarvis-Verteilung: Portierung {plattform}")
    log(f"Portierungssitzung {plattform}: {info} ({len(paket)} Commits, {datei})")
    if not gestartet:
        buch_schreiben({k: None for k in anspruch}, f"Jarvis-Verteilung: Anspruch {plattform} zurückgegeben ({info})")
        meldungen.append(f"Portierungssitzung nicht gestartet: {info}")
    else:
        meldungen.append(f"Portierungssitzung gestartet für {len(paket)} Änderung(en)")
    return meldungen


# ---------------------------------------------------------------- Installation

def installieren(ziel: str, buch: dict, alle: list[dict], erzwingen_pruefen: bool = True) -> dict:
    plattform = plattform_von(ziel)
    ordner = K["plattformen"][plattform]["ordner"]
    vorher = installierte_version(ziel)
    soll = quellversion(plattform)
    ergebnis = {"ziel": ziel, "vorher": vorher, "soll": soll}
    if not vorher:
        return {**ergebnis, "ergebnis": "nicht_installiert", "grund": "Jarvis ist auf diesem Rechner nicht installiert"}
    if vtupel(soll) <= vtupel(vorher):
        return {**ergebnis, "ergebnis": "aktuell", "grund": f"installiert {vorher} ist aktuell"}
    if (abw := git("diff", "--name-only", "origin/main", "--", ordner, pruefen=False).strip()):
        return {**ergebnis, "ergebnis": "wartet", "grund": f"lokaler Stand von {ordner} weicht von origin/main ab ({abw.splitlines()[0]} …)"}
    if (sperre := installations_sperre(plattform, buch, alle)):
        return {**ergebnis, "ergebnis": "blockiert" if sperre.startswith("blockiert") else "wartet", "grund": sperre}
    if (live := live_aktiv()):
        return {**ergebnis, "ergebnis": "wartet", "grund": live}
    log(f"Installiere Jarvis {soll} (bisher {vorher})")
    sicherung = sichern(ziel)
    ok, info = bauen_und_installieren(ziel)
    nachher = installierte_version(ziel)
    if ok:
        for _ in range(20 if not SIM else 1):
            if laeuft(ziel):
                break
            time.sleep(1.5)
        ok = nachher == soll and laeuft(ziel)
        if not ok:
            info = f"Prüfung nach der Installation fehlgeschlagen (Version {nachher or '?'} statt {soll}, läuft: {'ja' if laeuft(ziel) else 'nein'})"
    if ok:
        e = {**ergebnis, "ergebnis": "installiert", "nachher": nachher, "grund": info}
    else:
        rueck = wiederherstellen(ziel, sicherung) if sicherung else "keine Sicherung vorhanden"
        e = {**ergebnis, "ergebnis": "fehlgeschlagen", "nachher": installierte_version(ziel), "grund": f"{info}; letzte Version {rueck}"}
    log(f"Installation: {e['ergebnis']} – {e['grund']}")
    eintrag = {"ziel": ziel, "version": e.get("nachher") or vorher, "versucht": soll, "ergebnis": e["ergebnis"],
               "grund": e["grund"], "host": host(), "zeit": jetzt()}
    buch_schreiben({f"installiert/{ziel}.json": eintrag}, f"Jarvis-Verteilung: {ziel} {e['ergebnis']} {soll}")
    return e


# ---------------------------------------------------------------- Status

def status(buch: dict, alle: list[dict], lokal: dict | None = None) -> dict:
    ziele = []
    for ziel, zk in K["ziele"].items():
        pl = zk["plattform"]
        eintraege = [e for k, e in buch.items() if k.startswith(pl + "/")]
        def liste(st):
            return [{"sha": e.get("quelle", "")[:12], "titel": e.get("titel", ""), "grund": e.get("grund", ""), "host": e.get("host", "")}
                    for e in sorted(eintraege, key=lambda x: x.get("zeit", ""), reverse=True) if e.get("status") == st]
        inst = buch.get(f"installiert/{ziel}.json") or {}
        installiert = installierte_version(ziel) if ziel == eigenes_ziel() else inst.get("version", "")
        bereit = quellversion(pl, "origin/main")
        sperre = installations_sperre(pl, buch, alle)
        offen = [c for c in kandidaten(pl, alle) if schluessel(pl, c["sha"]) not in buch and not c["schonPortiert"]]
        if sperre and sperre.startswith("blockiert"):
            zustand = "blockiert"
        elif not installiert:
            zustand = "unbekannt" if ziel != eigenes_ziel() else "nicht_installiert"
        elif vtupel(bereit) > vtupel(installiert):
            zustand = "wartet" if sperre else "bereitgestellt"
        else:
            zustand = "aktuell"
        ziele.append({
            "ziel": ziel, "name": zk["name"], "plattform": pl, "lokal": ziel == eigenes_ziel(),
            "installiert": installiert, "bereitgestellt": bereit, "zustand": zustand, "sperre": sperre,
            "letzteInstallation": inst or None,
            "portierung": {"geprueft": len(eintraege), "offen": len(offen), "in_arbeit": liste("in_arbeit"),
                           "freigegeben": liste("freigegeben")[:5], "uebersprungen": liste("uebersprungen")[:5],
                           "blockiert": liste("blockiert")},
        })
    return {"zeit": jetzt(), "host": host(), "ziel": eigenes_ziel(), "ziele": ziele, "lokal": lokal or {}}


def text_zeile(z: dict) -> str:
    p = z["portierung"]
    teile = [f"{z['name']}: installiert {z['installiert'] or '–'}, bereitgestellt {z['bereitgestellt'] or '–'} ({z['zustand']})"]
    if z["plattform"] == "mac" or z["ziel"] == "hp":
        teile.append(f"Portierung {z['plattform']}: {p['geprueft']} geprüft, {len(p['freigegeben'])} freigegeben, "
                     f"{len(p['uebersprungen'])} übersprungen, {len(p['in_arbeit'])} in Arbeit, {len(p['blockiert'])} blockiert, {p['offen']} offen")
        for u in p["uebersprungen"][:2]:
            teile.append(f"  übersprungen {u['sha'][:8]}: {u['grund']}")
        for b in p["blockiert"][:2]:
            teile.append(f"  blockiert {b['sha'][:8]}: {b['grund']}")
    if z["sperre"]:
        teile.append(f"  Sperre: {z['sperre']}")
    li = z.get("letzteInstallation")
    if li:
        teile.append(f"  zuletzt {li.get('ergebnis')} {li.get('versucht', '')} am {li.get('zeit', '')[:16].replace('T', ' ')}: {li.get('grund', '')}")
    return "\n".join(teile)


def ausgeben(daten: dict, als_json: bool, kurz: str) -> None:
    if daten.get("ziele"):
        daten["text"] = "\n".join([text_zeile(z) for z in daten["ziele"]] + ["• " + m for m in daten.get("lokal", {}).get("meldungen", [])])
        daten["kurz"] = kurz
    if os.environ.get("JARVIS_VERTEILUNG_JSON"):
        Path(os.environ["JARVIS_VERTEILUNG_JSON"]).write_text(json.dumps(daten, ensure_ascii=False, indent=1), encoding="utf-8")
    if als_json:
        print(json.dumps(daten, ensure_ascii=False, indent=1))
    else:
        for z in daten.get("ziele", []):
            print(text_zeile(z))
        for m in daten.get("lokal", {}).get("meldungen", []):
            print("• " + m)
    print("JARVIS_VERTEILUNG_STATUS=" + kurz)


# ---------------------------------------------------------------- Befehle

def befehl_auto(args) -> int:
    ziel = eigenes_ziel()
    if ziel not in K["ziele"]:
        print("JARVIS_VERTEILUNG_STATUS=fehler Unbekannter Rechner")
        return 2
    if ist_erhoeht():
        # Erhöht gestartet (Update-Zentrale als Administrator): Sitzungen und Jarvis sollen nicht erhöht laufen.
        r = subprocess.run(["schtasks", "/Run", "/TN", "JarvisVerteilung"], capture_output=True, text=True, creationflags=OHNE_FENSTER)
        lokal = {"meldungen": ["an die Hintergrundaufgabe übergeben" if r.returncode == 0 else "Hintergrundaufgabe fehlt, bitte einrichten"]}
        holen()
        alle, buch = commits(), buch_lesen()
        ausgeben(status(buch, alle, lokal), args.json, "uebergeben")
        return 0
    sperre = lauf_sperre()
    if sperre is None:
        print("JARVIS_VERTEILUNG_STATUS=laeuft_schon")
        return 0
    try:
        online = holen()
        vorspul = vorspulen()
        alle, buch = commits(), buch_lesen()
        lokal = {"meldungen": [] if online else ["offline: letzter bekannter Stand, wird nachgeholt"]}
        if vorspul:
            lokal["meldungen"].append(f"Repo nicht vorgespult: {vorspul}")
        if online:
            lokal["meldungen"] += portierung(plattform_von(ziel), ziel, buch, alle, darf_starten=not args.ohne_sitzung)
            buch = buch_lesen()
        kurz = "geprueft"
        if not args.ohne_installation:
            inst = installieren(ziel, buch, alle) if online or SIM else {"ergebnis": "wartet", "grund": "offline"}
            lokal["installation"] = inst
            lokal["meldungen"].append(f"Installation hier: {inst['ergebnis']} – {inst['grund']}")
            kurz = inst["ergebnis"]
            buch = buch_lesen()
        ausgeben(status(buch, alle, lokal), args.json, kurz)
        return 0 if kurz != "fehlgeschlagen" else 1
    finally:
        sperre.unlink(missing_ok=True)


def befehl_status(args) -> int:
    if not args.ohne_holen:
        holen()
    ausgeben(status(buch_lesen(), commits()), args.json, "status")
    return 0


def befehl_melden(args) -> int:
    holen()
    plattform = args.plattform
    alle = commits()
    shas = [s.strip() for s in args.quelle.replace(";", ",").split(",") if s.strip()]
    ergebnis = args.ergebnis
    grund = args.grund or ""
    if ergebnis == "freigegeben":
        if args.review != "ok" or args.tests != "ok":
            ergebnis = "blockiert"
            grund = (grund + "; " if grund else "") + f"Freigabe verweigert: Review {args.review or 'fehlt'}, Tests {args.tests or 'fehlen'}"
        else:
            portiert = {s for c in alle for s in c["portiert"]
                        if any(f.startswith(K["plattformen"][plattform]["ordner"]) for f in c["dateien"])}
            fehlt = [s for s in shas if s[:12] not in portiert]
            if fehlt:
                print(f"Kein Portierungs-Commit mit '{TRAILER} {fehlt[0][:12]}' für {plattform} auf origin/main. Erst committen und pushen.")
                print("JARVIS_VERTEILUNG_STATUS=fehler")
                return 2
    aend = {}
    voll = {c["sha"][:12]: c for c in alle}
    for s in shas:
        c = voll.get(s[:12])
        if c is None:
            print(f"Commit {s} ist nicht seit der Basis auf origin/main.")
            print("JARVIS_VERTEILUNG_STATUS=fehler")
            return 2
        aend[schluessel(plattform, c["sha"])] = {"quelle": c["sha"], "plattform": plattform, "titel": c["titel"],
                                                 "quellen": sorted(quellplattformen(c["dateien"])), "status": ergebnis,
                                                 "grund": grund, "review": args.review or "", "tests": args.tests or "",
                                                 "host": host(), "zeit": jetzt()}
    ok, info = buch_schreiben(aend, f"Jarvis-Verteilung: {plattform} {ergebnis} ({', '.join(s[:8] for s in shas)})")
    print(f"{ergebnis}: {', '.join(s[:12] for s in shas)} – {info}")
    print("JARVIS_VERTEILUNG_STATUS=" + (ergebnis if ok else "fehler"))
    return 0 if ok else 1


def befehl_zuruecksetzen(args) -> int:
    holen()
    buch = buch_lesen()
    shas = [s.strip() for s in args.quelle.split(",") if s.strip()]
    aend = {k: None for k in buch if k.startswith(args.plattform + "/") and any(k.endswith(f"/{s[:12]}.json") for s in shas)}
    ok, info = buch_schreiben(aend, f"Jarvis-Verteilung: {args.plattform} zurückgesetzt") if aend else (False, "nichts gefunden")
    print(info)
    print("JARVIS_VERTEILUNG_STATUS=" + ("zurueckgesetzt" if ok else "fehler"))
    return 0 if ok else 1


def befehl_einrichten(args) -> int:
    """Hintergrundlauf alle 30 Minuten und bei der Anmeldung: Windows-Aufgabe ohne erhöhte Rechte, Mac-LaunchAgent."""
    skript = str(Path(__file__).resolve())
    if IST_WINDOWS:
        py = Path(sys.executable)
        pyw = py.with_name("pythonw.exe")
        exe = str(pyw if pyw.exists() else py)
        r = subprocess.run(["schtasks", "/Create", "/F", "/TN", "JarvisVerteilung", "/SC", "MINUTE", "/MO", "30", "/RL", "LIMITED",
                            "/TR", f'"{exe}" "{skript}" auto'], capture_output=True, text=True, creationflags=OHNE_FENSTER)
        print((r.stdout or r.stderr).strip())
        print("JARVIS_VERTEILUNG_STATUS=" + ("eingerichtet" if r.returncode == 0 else "fehler"))
        return r.returncode
    if sys.platform == "darwin":
        name = "de.frank.jarvis-verteilung"
        plist = Path.home() / f"Library/LaunchAgents/{name}.plist"
        inhalt = f"""<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
<key>Label</key><string>{name}</string>
<key>ProgramArguments</key><array><string>/usr/bin/python3</string><string>{skript}</string><string>auto</string></array>
<key>StartInterval</key><integer>1800</integer>
<key>RunAtLoad</key><true/>
<key>EnvironmentVariables</key><dict><key>PATH</key><string>/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin</string>
<key>DEVELOPER_DIR</key><string>/Library/Developer/CommandLineTools</string></dict>
<key>StandardOutPath</key><string>{zustand_ordner() / 'hintergrund.log'}</string>
<key>StandardErrorPath</key><string>{zustand_ordner() / 'hintergrund.log'}</string>
</dict></plist>
"""
        alt = plist.read_text() if plist.exists() else ""
        if alt != inhalt:
            plist.parent.mkdir(parents=True, exist_ok=True)
            plist.write_text(inhalt)
            uid = str(os.getuid())
            subprocess.run(["launchctl", "bootout", f"gui/{uid}", str(plist)], capture_output=True)
            subprocess.run(["launchctl", "bootstrap", f"gui/{uid}", str(plist)], capture_output=True)
        print("JARVIS_VERTEILUNG_STATUS=eingerichtet")
        return 0
    print("JARVIS_VERTEILUNG_STATUS=fehler Plattform")
    return 2


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    sub = ap.add_subparsers(dest="befehl", required=True)
    a = sub.add_parser("auto")
    a.add_argument("--json", action="store_true")
    a.add_argument("--ohne-installation", action="store_true")
    a.add_argument("--ohne-sitzung", action="store_true")
    p = sub.add_parser("pruefen")
    p.add_argument("--json", action="store_true")
    i = sub.add_parser("installieren")
    i.add_argument("--json", action="store_true")
    s = sub.add_parser("status")
    s.add_argument("--json", action="store_true")
    s.add_argument("--ohne-holen", action="store_true")
    m = sub.add_parser("melden")
    m.add_argument("--plattform", required=True, choices=sorted(K["plattformen"]))
    m.add_argument("--quelle", required=True)
    m.add_argument("--ergebnis", required=True, choices=["freigegeben", "uebersprungen", "blockiert"])
    m.add_argument("--grund", default="")
    m.add_argument("--review", choices=["ok", "fehler"])
    m.add_argument("--tests", choices=["ok", "fehler"])
    z = sub.add_parser("zuruecksetzen")
    z.add_argument("--plattform", required=True, choices=sorted(K["plattformen"]))
    z.add_argument("--quelle", required=True)
    sub.add_parser("einrichten")
    args = ap.parse_args()
    if args.befehl == "pruefen":
        args.ohne_installation, args.ohne_sitzung = True, False
        return befehl_auto(args)
    if args.befehl == "installieren":
        args.ohne_installation, args.ohne_sitzung = False, True
        return befehl_auto(args)
    return {"auto": befehl_auto, "status": befehl_status, "melden": befehl_melden,
            "zuruecksetzen": befehl_zuruecksetzen, "einrichten": befehl_einrichten}[args.befehl](args)


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as fehler:  # noqa: BLE001 – die Karte braucht immer eine Statuszeile
        print(f"Fehler: {fehler}")
        print("JARVIS_VERTEILUNG_STATUS=fehler")
        sys.exit(1)
