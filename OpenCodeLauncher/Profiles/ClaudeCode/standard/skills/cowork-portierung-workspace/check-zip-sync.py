#!/usr/bin/env python3
"""Prueft fuer jeden Cowork-Skill, ob die committe ZIP mit der aktuellen skills-src synchron ist
und ob die committe ZIP noch ALTE best-practices-Pfade enthaelt (projekt-code/, best-practices-).
"""
import hashlib
import io
import os
import re
import subprocess
import sys
import zipfile

for _s in (sys.stdout, sys.stderr):
    try:
        _s.reconfigure(encoding="utf-8")
    except Exception:
        pass

REPO = "C:/Users/barwa/proggs"
SRC_BASE = os.path.join(REPO, "Cowork", "skills-src")
OLD_RE = re.compile(r"projekt-code|best-practices-[a-z]|<NN-kategorie>")


def src_hashes(name):
    """Datei-Hashes der aktuellen skills-src (wie das ZIP sie enthalten wuerde)."""
    d = os.path.join(SRC_BASE, name)
    out = {}
    for root, dirs, files in os.walk(d):
        dirs.sort()
        for fn in sorted(files):
            full = os.path.join(root, fn)
            rel = os.path.relpath(full, d).replace(os.sep, "/")
            with open(full, "rb") as f:
                data = f.read().replace(b"\r\n", b"\n").replace(b"\r", b"\n")
                if data.startswith(b"\xef\xbb\xbf"):
                    data = data[3:]
            out[f"{name}/{rel}"] = hashlib.sha256(data).hexdigest()[:12]
    return out


USE_WORKTREE = "--worktree" in sys.argv


def zip_committed(name):
    """(Datei-Hashes, alte_pfade) der ZIP — aus HEAD oder (mit --worktree) aus der Datei."""
    if USE_WORKTREE:
        p = os.path.join(REPO, "Cowork", f"{name}.zip")
        raw = open(p, "rb").read() if os.path.isfile(p) else b""
    else:
        raw = subprocess.run(["git", "-C", REPO, "show", f"HEAD:Cowork/{name}.zip"],
                             capture_output=True).stdout
    if not raw:
        return None, None
    z = zipfile.ZipFile(io.BytesIO(raw))
    hashes, old = {}, set()
    for n in z.namelist():
        content = z.read(n)
        hashes[n] = hashlib.sha256(content).hexdigest()[:12]
        if n.endswith(".md"):
            old |= set(OLD_RE.findall(content.decode("utf-8", "replace")))
    return hashes, old


names = sorted(d for d in os.listdir(SRC_BASE) if os.path.isdir(os.path.join(SRC_BASE, d)))
print(f"{'Skill':<26} {'ZIP synchron?':<16} {'alte BP-Pfade in ZIP?'}")
print("-" * 70)
stale = []
for name in names:
    sh = src_hashes(name)
    zh, old = zip_committed(name)
    if zh is None:
        print(f"{name:<26} {'KEINE ZIP':<16} -")
        stale.append(name)
        continue
    synced = sh == zh
    if not synced:
        stale.append(name)
    old_str = ", ".join(sorted(old)) if old else "nein"
    print(f"{name:<26} {('JA' if synced else 'NEIN (veraltet)'):<16} {old_str}")

print("-" * 70)
if stale:
    print(f"VERALTET / neu zu bauen: {stale}")
else:
    print("Alle ZIPs sind synchron.")
