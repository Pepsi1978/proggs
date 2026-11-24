#!/usr/bin/env python3
"""Ende-zu-Ende-Test der Jarvis-Verteilung mit echten Git-Repos: ein gemeinsames origin, je ein Klon für HP, Power-PC
und Mac, simulierte Installation (Versionsdatei), simulierter Live-Zustand und simulierter Sitzungsstart. Die
Portierungssitzungen spielt der Test selbst nach (Commit mit Trailer, Push, melden).

Aufruf: python3 Werkzeuge/jarvis-verteilung/test_jarvis_verteilung.py   (Ausgabe je Schritt, Exit 0 = alles grün)
"""
import json
import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

MOTOR = Path(__file__).resolve().parent / "jarvis_verteilung.py"
TMP = Path(tempfile.mkdtemp(prefix="jv-test-"))
ORIGIN = TMP / "origin.git"
FEHLER = []


def sh(cwd, *args, ok=True):
    r = subprocess.run(list(args), cwd=cwd, capture_output=True, text=True)
    if ok and r.returncode != 0:
        raise RuntimeError(f"{args}: {r.stderr}{r.stdout}")
    return r.stdout.strip()


def pruefe(bedingung, text):
    print(("  OK    " if bedingung else "  FEHLER ") + text)
    if not bedingung:
        FEHLER.append(text)


def schreibe(repo: Path, dateien: dict):
    for rel, inhalt in dateien.items():
        p = repo / rel
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(inhalt)


def csproj(v):
    return f"<Project><PropertyGroup><Version>{v}</Version></PropertyGroup></Project>\n"


def plist(v):
    return f"<plist><dict><key>CFBundleShortVersionString</key>\n<string>{v}</string></dict></plist>\n"


def commit(name, dateien, nachricht):
    repo = TMP / name
    sh(repo, "git", "pull", "--quiet", "--rebase", "origin", "main")
    schreibe(repo, dateien)
    sh(repo, "git", "add", "-A")
    sh(repo, "git", "commit", "--quiet", "-m", nachricht)
    sh(repo, "git", "push", "--quiet", "origin", "HEAD:main")
    return sh(repo, "git", "rev-parse", "HEAD")


def motor(ziel, *args):
    env = {**os.environ, "JARVIS_VERTEILUNG_REPO": str(TMP / ziel), "JARVIS_VERTEILUNG_SIM": str(TMP / f"sim-{ziel}"),
           "JARVIS_VERTEILUNG_ZIEL": ziel, "JARVIS_VERTEILUNG_HOST": ziel, "JARVIS_VERTEILUNG_BASIS": BASIS}
    r = subprocess.run([sys.executable, str(MOTOR), *args, "--json"] if args[0] in ("auto", "status") else
                       [sys.executable, str(MOTOR), *args], capture_output=True, text=True, env=env)
    zeilen = r.stdout.strip().splitlines()
    status = zeilen[-1].split("=", 1)[1] if zeilen and zeilen[-1].startswith("JARVIS_VERTEILUNG_STATUS=") else "?"
    daten = {}
    if args[0] in ("auto", "status"):
        try:
            daten = json.loads("\n".join(zeilen[:-1]))
        except ValueError:
            print(r.stdout, r.stderr)
    return status, daten, r


def installiert(ziel):
    return (TMP / f"sim-{ziel}" / f"{ziel}-installiert.txt").read_text().strip()


def sitzungen(ziel):
    d = TMP / f"sim-{ziel}" / "gestartet"
    return sorted(d.glob(f"{ziel}-*.md")) if d.exists() else []


def ziel_status(daten, ziel):
    return next(z for z in daten["ziele"] if z["ziel"] == ziel)


# ------------------------------------------------------------------ Aufbau
sh(TMP, "git", "init", "--quiet", "--bare", "-b", "main", str(ORIGIN))
saat = TMP / "saat"
sh(TMP, "git", "clone", "--quiet", str(ORIGIN), str(saat))
sh(saat, "git", "config", "user.email", "test@example.invalid")
sh(saat, "git", "config", "user.name", "Test")
schreibe(saat, {
    "JarvisWindows/JarvisWindows.csproj": csproj("1.0.1"), "JarvisWindows/Sources/A.cs": "// a\n",
    "JarvisMac/Info.plist": plist("1.0.1"), "JarvisMac/Sources/A.swift": "// a\n",
    "Jarvis/app/src/main/java/de/frank/jarvis/X.kt": "// x\n", "Jarvis/app/src/main/res/layout/a.xml": "<a/>\n",
    "Jarvis/app/src/main/assets/versionslog.json": "{}\n",
})
sh(saat, "git", "add", "-A")
sh(saat, "git", "commit", "--quiet", "-m", "Saat")
sh(saat, "git", "push", "--quiet", "origin", "HEAD:main")
BASIS = sh(saat, "git", "rev-parse", "HEAD")
for z in ("hp", "power", "mac"):
    sh(TMP, "git", "clone", "--quiet", str(ORIGIN), str(TMP / z))
    sh(TMP / z, "git", "config", "user.email", f"{z}@example.invalid")
    sh(TMP / z, "git", "config", "user.name", z)
    (TMP / f"sim-{z}").mkdir()
    (TMP / f"sim-{z}" / f"{z}-installiert.txt").write_text("1.0.1")

# ------------------------------------------------------------------ 1. Windows-Update vom HP auf Power-PC und Mac
print("1. Windows-Update vom HP → Power-PC und (kompatibel) Mac")
w1 = commit("hp", {"JarvisWindows/Sources/A.cs": "// a2 neue Funktion\n", "JarvisWindows/JarvisWindows.csproj": csproj("1.0.2")},
            "JarvisWindows: neue Funktion Wetterkarte (1.0.2)")
st, d, _ = motor("power", "auto")
pruefe(installiert("power") == "1.0.2", f"Power-PC installiert 1.0.2 (Status {st})")
pruefe(not sitzungen("power"), "Power-PC startet keine CLI (kein Portierungsbedarf)")
st, d, _ = motor("mac", "auto")
pruefe(len(sitzungen("mac")) == 1, "Mac startet genau eine Portierungssitzung")
auftrag = sitzungen("mac")[0].read_text() if sitzungen("mac") else ""
pruefe(w1[:12] in auftrag and "REVIEW: ja" in auftrag, "Auftrag nennt den Commit und legt das Review fest")
pruefe(ziel_status(d, "mac")["zustand"] in ("aktuell", "wartet"), "Mac installiert vor der Freigabe nichts Neues")
st, d, _ = motor("mac", "auto")
pruefe(len(sitzungen("mac")) == 1, "zweiter Lauf startet keine doppelte Sitzung")
# Sitzung spielt: Port, Review ok, Tests ok, Push, melden
commit("mac", {"JarvisMac/Sources/A.swift": "// a2 Wetterkarte\n", "JarvisMac/Info.plist": plist("1.0.2")},
       f"JarvisMac: Wetterkarte portiert (1.0.2)\n\nJarvis-Portierung: {w1[:12]}")
st, _, r = motor("mac", "melden", "--plattform", "mac", "--quelle", w1[:12], "--ergebnis", "freigegeben", "--review", "ok", "--tests", "ok")
pruefe(st == "freigegeben", f"Freigabe gemeldet ({st})")
st, d, _ = motor("mac", "auto")
pruefe(installiert("mac") == "1.0.2", f"Mac installiert die freigegebene Portierung 1.0.2 ({st})")
pruefe(len(sitzungen("mac")) == 1, "Portierungs-Commit löst keine neue Sitzung aus (keine Schleife)")
st, d, _ = motor("hp", "auto")
pruefe(not sitzungen("hp"), "HP startet für die Mac-Portierung keine Rückportierung")

# ------------------------------------------------------------------ 2. Handy/Tablet-Änderung auf alle drei Rechner, Power offline
print("2. Handy/Tablet-Änderung → alle drei Rechner, Power-PC ist offline und holt nach")
sh(TMP / "power", "git", "remote", "set-url", "origin", str(TMP / "gibt-es-nicht.git"))
m1 = commit("saat", {"Jarvis/app/src/main/java/de/frank/jarvis/X.kt": "// x2 Einkaufsliste teilen\n"},
            "Jarvis: Einkaufsliste teilen (1.8.950)")
st, d, _ = motor("hp", "auto")
pruefe(len(sitzungen("hp")) == 1, "HP übernimmt die Windows-Portierungsprüfung")
st, d, _ = motor("power", "auto")
pruefe(not sitzungen("power") and installiert("power") == "1.0.2", f"Power-PC offline: keine Sitzung, Stand bleibt ({st})")
st, d, _ = motor("mac", "auto")
pruefe(len(sitzungen("mac")) == 2, "Mac übernimmt die Mac-Portierungsprüfung")
commit("hp", {"JarvisWindows/Sources/A.cs": "// a3 Einkaufsliste teilen\n", "JarvisWindows/JarvisWindows.csproj": csproj("1.0.3")},
       f"JarvisWindows: Einkaufsliste teilen portiert (1.0.3)\n\nJarvis-Portierung: {m1[:12]}")
pruefe(motor("hp", "auto")[1] and installiert("hp") == "1.0.2", "HP installiert nicht vor der Freigabe (Portierung in Arbeit)")
motor("hp", "melden", "--plattform", "windows", "--quelle", m1[:12], "--ergebnis", "freigegeben", "--review", "ok", "--tests", "ok")
commit("mac", {"JarvisMac/Sources/A.swift": "// a3 Einkaufsliste teilen\n", "JarvisMac/Info.plist": plist("1.0.3")},
       f"JarvisMac: Einkaufsliste teilen portiert (1.0.3)\n\nJarvis-Portierung: {m1[:8]}")
motor("mac", "melden", "--plattform", "mac", "--quelle", m1[:12], "--ergebnis", "freigegeben", "--review", "ok", "--tests", "ok")
motor("hp", "auto")
motor("mac", "auto")
pruefe(installiert("hp") == "1.0.3" and installiert("mac") == "1.0.3", "HP und Mac installieren 1.0.3")
sh(TMP / "power", "git", "remote", "set-url", "origin", str(ORIGIN))
st, d, _ = motor("power", "auto")
pruefe(installiert("power") == "1.0.3", f"Power-PC wieder online: holt 1.0.3 nach ({st})")
pruefe(not sitzungen("power"), "Power-PC startet keine doppelte Windows-Sitzung")
hp = ziel_status(d, "hp")
pruefe(hp["installiert"] == "1.0.3" and hp["letzteInstallation"]["ergebnis"] == "installiert", "Status zeigt die HP-Installation auch am Power-PC")

# ------------------------------------------------------------------ 3. bewusst nicht portiert
print("3. Bewusst nicht portierte Änderungen")
n1 = commit("saat", {"Jarvis/app/src/main/res/layout/a.xml": "<a b='1'/>\n", "Jarvis/app/src/main/assets/versionslog.json": "{\"v\":2}\n"},
            "Jarvis: Knopf am Handy größer (1.8.951)")
n2 = commit("saat", {"Jarvis/app/src/main/java/de/frank/jarvis/X.kt": "// x3 Schrittzähler-Sensor\n"},
            "Jarvis: Schrittzähler-Sensor des Handys (1.8.952)")
vorher_hp, vorher_mac = len(sitzungen("hp")), len(sitzungen("mac"))
st, d, _ = motor("hp", "auto")
uebers = ziel_status(d, "hp")["portierung"]["uebersprungen"]
pruefe(any(u["sha"] == n1[:12] and "Android-Ressourcen" in u["grund"] for u in uebers), "rein mobile Ressourcen-Änderung ohne CLI übersprungen, mit Grund")
pruefe(len(sitzungen("hp")) == vorher_hp + 1, "für die Sensor-Änderung startet genau eine Prüfsitzung")
auftrag = sitzungen("hp")[-1].read_text()
pruefe(n2[:12] in auftrag and n1[:12] not in auftrag, "die Sitzung prüft nur die unklare Änderung")
motor("hp", "melden", "--plattform", "windows", "--quelle", n2[:9], "--ergebnis", "uebersprungen", "--grund", "Schrittzähler-Sensor gibt es nur am Handy")
st, d, _ = motor("mac", "auto")
motor("mac", "melden", "--plattform", "mac", "--quelle", n2[:12], "--ergebnis", "uebersprungen", "--grund", "Schrittzähler-Sensor gibt es nur am Handy")
st, d, _ = motor("power", "auto")
for z in ("hp", "power", "mac"):
    p = ziel_status(d, z)["portierung"]
    pruefe(any(u["sha"] == n2[:12] and "nur am Handy" in u["grund"] for u in p["uebersprungen"]), f"{z}: Grund der Nicht-Portierung sichtbar")
pruefe(installiert("hp") == installiert("power") == installiert("mac") == "1.0.3", "keine Installation durch nicht portierte Änderungen")

# ------------------------------------------------------------------ 4. Review/Test rot blockiert die Verteilung
print("4. Fehlgeschlagenes Review blockiert, fehlgeschlagener Test blockiert")
w4 = commit("hp", {"JarvisWindows/Sources/A.cs": "// a4 Kalender\n", "JarvisWindows/JarvisWindows.csproj": csproj("1.0.4")},
            "JarvisWindows: Kalenderansicht (1.0.4)")
motor("mac", "auto")
commit("mac", {"JarvisMac/Sources/A.swift": "// a4 Kalender kaputt\n", "JarvisMac/Info.plist": plist("1.0.4")},
       f"JarvisMac: Kalender portiert (1.0.4)\n\nJarvis-Portierung: {w4[:12]}")
st, _, _ = motor("mac", "melden", "--plattform", "mac", "--quelle", w4[:12], "--ergebnis", "freigegeben", "--review", "fehler", "--tests", "ok",
                 "--grund", "Review: Absturz bei leerem Kalender")
pruefe(st == "blockiert", f"Freigabe mit rotem Review wird zu blockiert ({st})")
st, d, _ = motor("mac", "auto")
pruefe(installiert("mac") == "1.0.3" and st == "blockiert", f"Mac verteilt den blockierten Stand nicht, 1.0.3 bleibt ({st})")
pruefe(ziel_status(d, "mac")["zustand"] == "blockiert", "Karte zeigt den Mac als blockiert")
st, d, _ = motor("power", "auto")
pruefe(installiert("power") == "1.0.4", "Windows-Rechner sind davon nicht betroffen (Power-PC 1.0.4)")
w5 = commit("hp", {"JarvisWindows/Sources/A.cs": "// a5 Notizen\n", "JarvisWindows/JarvisWindows.csproj": csproj("1.0.5")},
            "JarvisWindows: Notizen (1.0.5)")
motor("mac", "auto")
st, _, _ = motor("mac", "melden", "--plattform", "mac", "--quelle", w5[:12], "--ergebnis", "blockiert", "--review", "ok", "--tests", "fehler",
                 "--grund", "Test: Mac-Bau scheitert an fehlender API")
st, d, _ = motor("mac", "auto")
blo = ziel_status(d, "mac")["portierung"]["blockiert"]
pruefe(any(b["sha"] == w5[:12] and "Mac-Bau" in b["grund"] for b in blo), "roter Test: blockiert mit Grund, nichts verteilt")
st, _, r = motor("mac", "melden", "--plattform", "mac", "--quelle", w5[:12], "--ergebnis", "freigegeben", "--review", "ok", "--tests", "ok")
pruefe(st == "fehler", "Freigabe ohne gepushten Portierungs-Commit wird abgelehnt")

# ------------------------------------------------------------------ 5. Installation scheitert → letzte Version bleibt
print("5. Installation scheitert → Wiederherstellung, keine Dauerschleife")
VOR5 = installiert("hp")
(TMP / "sim-hp" / "hp-installfehler").write_text("1")
st, d, _ = motor("hp", "auto")
pruefe(st == "fehlgeschlagen" and installiert("hp") == VOR5, f"HP: Bau scheitert, {VOR5} wiederhergestellt ({st}, {installiert('hp')})")
(TMP / "sim-hp" / "hp-installfehler").unlink()
st, d, _ = motor("hp", "auto")
pruefe(st == "wartet" and installiert("hp") == VOR5, f"HP: dieselbe gescheiterte Version wird nicht alle 30 Minuten neu versucht ({st})")
commit("hp", {"JarvisWindows/Sources/A.cs": "// a6\n", "JarvisWindows/JarvisWindows.csproj": csproj("1.0.6")}, "JarvisWindows: Korrektur (1.0.6)")
(TMP / "sim-hp" / "hp-startfehler").write_text("1")
st, d, _ = motor("hp", "auto")
pruefe(st == "fehlgeschlagen" and installiert("hp") == VOR5, f"HP: neue Fassung startet nicht → {VOR5} wiederhergestellt")
(TMP / "sim-hp" / "hp-startfehler").unlink()

# ------------------------------------------------------------------ 6. Live-Modus wird nicht gestört
print("6. Live-Sprachmodus")
import datetime as dt
commit("hp", {"JarvisWindows/Sources/A.cs": "// a7\n", "JarvisWindows/JarvisWindows.csproj": csproj("1.0.7")}, "JarvisWindows: Korrektur (1.0.7)")
(TMP / "sim-hp" / "hp-live.json").write_text(json.dumps({"zustand": "hoert", "pid": os.getpid(), "zeit": dt.datetime.now().astimezone().isoformat()}))
st, d, _ = motor("hp", "auto")
pruefe(st == "wartet" and installiert("hp") == VOR5, f"HP wartet während Live ({st})")
(TMP / "sim-hp" / "hp-live.json").write_text(json.dumps({"zustand": "aus", "pid": os.getpid(), "zeit": dt.datetime.now().astimezone().isoformat()}))
st, d, _ = motor("hp", "auto")
pruefe(installiert("hp") == "1.0.7", f"HP installiert nach dem Live-Gespräch 1.0.7 ({st})")
(TMP / "sim-hp" / "hp-live.json").write_text(json.dumps({"zustand": "spricht", "pid": os.getpid(), "zeit": dt.datetime.now(dt.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")}))
commit("hp", {"JarvisWindows/Sources/A.cs": "// a8\n", "JarvisWindows/JarvisWindows.csproj": csproj("1.0.8")}, "JarvisWindows: Korrektur (1.0.8)")
st, d, _ = motor("hp", "auto")
pruefe(st == "wartet" and installiert("hp") == "1.0.7", f"Live-Zeit mit „Z“ (Mac in UTC) wird erkannt ({st})")
(TMP / "sim-hp" / "hp-live.json").unlink()

# ------------------------------------------------------------------ 7. Gleichzeitiger Anspruch zweier Windows-Rechner
print("7. Doppelte Aufträge")
m7 = commit("saat", {"Jarvis/app/src/main/java/de/frank/jarvis/X.kt": "// x7 Vorlesen schneller\n"}, "Jarvis: Vorlesetempo (1.8.953)")
vor_hp, vor_power = len(sitzungen("hp")), len(sitzungen("power"))
prozesse = []
for z in ("hp", "power"):
    env = {**os.environ, "JARVIS_VERTEILUNG_REPO": str(TMP / z), "JARVIS_VERTEILUNG_SIM": str(TMP / f"sim-{z}"),
           "JARVIS_VERTEILUNG_ZIEL": z, "JARVIS_VERTEILUNG_HOST": z, "JARVIS_VERTEILUNG_BASIS": BASIS}
    prozesse.append(subprocess.Popen([sys.executable, str(MOTOR), "pruefen"], env=env, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL))
for p in prozesse:
    p.wait()
neu = (len(sitzungen("hp")) - vor_hp) + (len(sitzungen("power")) - vor_power)
pruefe(neu == 1, f"gleichzeitig geprüft: genau eine Windows-Sitzung ({neu})")

# ------------------------------------------------------------------ 8. Laufsperre
print("8. Laufsperre")
sperre = TMP / "sim-mac" / "zustand" / "mac" / "lauf.lock"
sperre.parent.mkdir(parents=True, exist_ok=True)
sperre.write_text(str(os.getpid()))
st, d, _ = motor("mac", "auto")
pruefe(st == "laeuft_schon" and d.get("ziele"), f"belegte Sperre: Karte bekommt trotzdem den Stand ({st})")
sperre.write_text("999999")
st, d, _ = motor("mac", "auto")
pruefe(st != "laeuft_schon", f"Sperre eines toten Prozesses wird sofort frei ({st})")

print()
if FEHLER:
    print(f"{len(FEHLER)} FEHLER – Testordner bleibt: {TMP}")
    sys.exit(1)
print("Alle Prüfungen grün.")
shutil.rmtree(TMP, ignore_errors=True)
