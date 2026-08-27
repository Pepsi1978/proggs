#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""claude-login-sync: Spiegelt den Claude-Code-Login auf ALLE Profil-Konfigurationsordner.

Warum es das gibt
-----------------
OpenLauncher startet Claude Code je Profil mit einem eigenen ``CLAUDE_CONFIG_DIR``
(``OpenLauncher/Profiles/ClaudeCodeMac/<id>``). Claude Code legt den OAuth-Login
pro Konfigurationsordner getrennt ab:

* macOS  -> Schlüsselbund, Dienstname ``Claude Code-credentials-<sha256(configdir)[:8]>``
            (der Standardordner ``~/.claude`` nutzt den Namen ohne Suffix).
* Windows -> Datei ``<configdir>\\.credentials.json``.

Zusätzlich merkt sich Claude Code die Kontoidentität in ``<configdir>/.claude.json``
(``oauthAccount``). Fehlt eines von beidem, erscheint beim Start der Anmeldebildschirm --
genau das passierte bei jedem Profilwechsel und bei jedem erstmals genutzten Profil.

Dieses Skript sucht den frischesten gültigen Login und trägt ihn in jeden Profilordner
ein, dem einer fehlt.

Konservativ und funktionserhaltend (Direktive #3)
------------------------------------------------
Geschrieben wird NUR, wenn im Ziel

* gar kein Login liegt, ODER
* der Login vollständig tot ist (Zugriffs- UND Auffrischungs-Token abgelaufen).

Ein Profil mit einem gültigen (auch bald ablaufenden) Login wird NIE angefasst -- so
bleibt ein bewusst anderes Konto pro Profil erhalten und ein laufender Token-Refresh
wird nicht überschrieben. Dasselbe gilt für ``.claude.json``: fehlende Felder werden
ergänzt, vorhandene bleiben unverändert.

Aufruf
------
    python3 claude-login-sync.py            # Abgleich durchführen
    python3 claude-login-sync.py --dry-run  # nur zeigen, was passieren würde

Endet IMMER mit Code 0. Tokens landen weder im Log noch in der Prozessliste
(Schlüsselbund-Schreibzugriff läuft über ``security -i`` statt über argv).
"""

from __future__ import annotations

import hashlib
import json
import os
import subprocess
import sys
import tempfile
import time
from pathlib import Path

IS_WINDOWS = os.name == "nt"
HOME = Path.home()
LOG_PATH = HOME / ".claude" / "logs" / "claude-login-sync.jsonl"
KEYCHAIN_BASE_SERVICE = "Claude Code-credentials"

# Felder aus .claude.json, die die Kontoidentität tragen. Ohne sie zeigt Claude Code
# den Onboarding-/Anmeldebildschirm, selbst wenn der Token im Schlüsselbund liegt.
IDENTITY_KEYS = ("oauthAccount", "userID", "hasCompletedOnboarding", "lastOnboardingVersion")


# --------------------------------------------------------------------------- Protokoll

def log(event: str, **felder) -> None:
    """Eine JSON-Zeile ins Hook-Log. Enthält NIE Token-Material."""
    try:
        LOG_PATH.parent.mkdir(parents=True, exist_ok=True)
        satz = {"ts": time.strftime("%Y-%m-%dT%H:%M:%S%z"), "module": "claude-login-sync", "msg": event}
        satz.update(felder)
        with LOG_PATH.open("a", encoding="utf-8", newline="\n") as fh:
            fh.write(json.dumps(satz, ensure_ascii=False) + "\n")
    except Exception:
        pass  # Protokollieren darf den Abgleich nie zu Fall bringen.


# ------------------------------------------------------------------- Konfigurationsordner

def profil_wurzeln() -> list[Path]:
    """Profilordner DIESER Plattform.

    Die Windows-Profile (``ClaudeCode``) tragen PowerShell-Hooks und ``C:``-Pfade und werden
    auf einem Mac nie gestartet -- sie hier mitzunehmen legte nur tote Schlüsselbund-Einträge
    an (und umgekehrt auf Windows).
    """
    proggs = HOME / "proggs" / "OpenLauncher" / "Profiles"
    return [proggs / ("ClaudeCode" if IS_WINDOWS else "ClaudeCodeMac")]


def config_dirs() -> list[Path]:
    """Jeder Konfigurationsordner, der einen eigenen Login trägt -- ohne Doppelte."""
    gefunden: list[Path] = []

    def dazu(p: Path) -> None:
        try:
            aufgeloest = p.resolve()
        except OSError:
            return
        if aufgeloest not in gefunden:
            gefunden.append(aufgeloest)

    dazu(HOME / ".claude")

    # Der Ordner der laufenden Sitzung -- er ist die wahrscheinlichste Quelle eines
    # frischen Tokens und darf auch dann mitlaufen, wenn er ausserhalb der Profile liegt.
    aktuell = os.environ.get("CLAUDE_CONFIG_DIR", "").strip()
    if aktuell:
        dazu(Path(aktuell))

    for wurzel in profil_wurzeln():
        if not wurzel.is_dir():
            continue
        for eintrag in sorted(wurzel.iterdir()):
            # "sources" enthält Regeltexte, keine Konfiguration.
            if eintrag.is_dir() and eintrag.name != "sources":
                dazu(eintrag)

    return gefunden


# ------------------------------------------------------------------- Speicher (Plattform)

def keychain_service(config_dir: Path) -> str:
    """Dienstname im Schlüsselbund -- so bildet Claude Code ihn selbst."""
    if config_dir == (HOME / ".claude").resolve():
        return KEYCHAIN_BASE_SERVICE
    hashwert = hashlib.sha256(str(config_dir).encode("utf-8")).hexdigest()[:8]
    return f"{KEYCHAIN_BASE_SERVICE}-{hashwert}"


def keychain_konto() -> str:
    return os.environ.get("USER") or os.environ.get("LOGNAME") or HOME.name


def token_lesen(config_dir: Path) -> dict | None:
    """Hinterlegten Login lesen -- None, wenn keiner da oder unlesbar."""
    try:
        if IS_WINDOWS:
            datei = config_dir / ".credentials.json"
            if not datei.is_file():
                return None
            return json.loads(datei.read_text(encoding="utf-8"))

        ergebnis = subprocess.run(
            ["security", "find-generic-password", "-w",
             "-s", keychain_service(config_dir), "-a", keychain_konto()],
            capture_output=True, text=True, timeout=15,
        )
        if ergebnis.returncode != 0 or not ergebnis.stdout.strip():
            return None
        return json.loads(ergebnis.stdout.strip())
    except Exception:
        return None


def token_schreiben(config_dir: Path, daten: dict) -> bool:
    """Login hinterlegen. Der Token geht NIE über argv (sichtbar in der Prozessliste)."""
    roh = json.dumps(daten, separators=(",", ":"))
    try:
        if IS_WINDOWS:
            config_dir.mkdir(parents=True, exist_ok=True)
            return atomar_schreiben(config_dir / ".credentials.json", roh)

        if "'" in roh:  # security -i trennt Argumente shell-artig; einfache Anführungszeichen
            return False  # kämen aus dem Tritt. Token enthalten sie nicht -- Sicherheitsnetz.
        befehl = 'add-generic-password -U -s "{s}" -a "{a}" -w \'{w}\'\n'.format(
            s=keychain_service(config_dir), a=keychain_konto(), w=roh)
        ergebnis = subprocess.run(["security", "-i"], input=befehl,
                                  capture_output=True, text=True, timeout=20)
        return ergebnis.returncode == 0
    except Exception:
        return False


def atomar_schreiben(ziel: Path, inhalt: str) -> bool:
    """Erst in eine Nachbardatei schreiben, dann umbenennen -- nie halbfertig hinterlassen."""
    tmp = ""
    try:
        ziel.parent.mkdir(parents=True, exist_ok=True)
        fd, tmp = tempfile.mkstemp(dir=str(ziel.parent), prefix=".tmp-", suffix=".json")
        with os.fdopen(fd, "w", encoding="utf-8", newline="\n") as fh:
            fh.write(inhalt)
        os.replace(tmp, ziel)
        tmp = ""
        try:
            os.chmod(ziel, 0o600)
        except OSError:
            pass
        return True
    except Exception:
        if tmp:
            try:
                os.unlink(tmp)
            except OSError:
                pass
        return False


# ------------------------------------------------------------------------ Bewertung

def oauth_teil(daten: dict | None) -> dict:
    """Claude Code verpackt den Token in 'claudeAiOauth' -- ältere Fassungen flach."""
    if not isinstance(daten, dict):
        return {}
    innen = daten.get("claudeAiOauth")
    return innen if isinstance(innen, dict) else daten


def ablauf(daten: dict | None, feld: str) -> float:
    wert = oauth_teil(daten).get(feld)
    try:
        return float(wert) / 1000.0
    except (TypeError, ValueError):
        return 0.0


def ist_tot(daten: dict | None) -> bool:
    """Tot = weder nutzbar noch auffrischbar. Nur solche Logins werden ersetzt."""
    if not oauth_teil(daten).get("accessToken"):
        return True
    jetzt = time.time()
    if ablauf(daten, "expiresAt") > jetzt:
        return False
    return ablauf(daten, "refreshTokenExpiresAt") <= jetzt


def ist_brauchbar(daten: dict | None) -> bool:
    return bool(oauth_teil(daten).get("accessToken")) and not ist_tot(daten)


# ------------------------------------------------------------------ Kontoidentität

def identitaet_pfad(config_dir: Path) -> Path:
    """Wo die Kontodatei liegt.

    Beim Standardordner ``~/.claude`` liegt sie als ``~/.claude.json`` DANEBEN (im Home),
    bei jedem per ``CLAUDE_CONFIG_DIR`` gesetzten Ordner dagegen DARIN.
    """
    if config_dir == (HOME / ".claude").resolve():
        return HOME / ".claude.json"
    return config_dir / ".claude.json"


def identitaet_lesen(config_dir: Path) -> dict:
    datei = identitaet_pfad(config_dir)
    try:
        inhalt = json.loads(datei.read_text(encoding="utf-8"))
        return inhalt if isinstance(inhalt, dict) else {}
    except Exception:
        return {}


def identitaet_saeen(config_dir: Path, quelle: dict, dry_run: bool) -> str:
    """Fehlende Identitätsfelder ergänzen. Vorhandene bleiben unverändert."""
    ziel_datei = identitaet_pfad(config_dir)
    vorhanden = identitaet_lesen(config_dir)

    fehlend = [k for k in IDENTITY_KEYS if k in quelle and not vorhanden.get(k)]
    if not fehlend:
        return "identitaet-vollstaendig"
    if dry_run:
        return "identitaet-wuerde-ergaenzen:" + ",".join(fehlend)

    # Sicherungskopie, falls eine parallele Sitzung gleichzeitig schreibt (Direktive #3).
    if ziel_datei.is_file():
        try:
            sicherung = (HOME / ".claude" / "backups"
                         if config_dir == (HOME / ".claude").resolve()
                         else config_dir / "backups") / ".claude.json.login-sync-backup"
            sicherung.parent.mkdir(parents=True, exist_ok=True)
            sicherung.write_bytes(ziel_datei.read_bytes())
        except Exception:
            pass

    for schluessel in fehlend:
        vorhanden[schluessel] = quelle[schluessel]

    if not atomar_schreiben(ziel_datei, json.dumps(vorhanden, ensure_ascii=False, indent=2)):
        return "identitaet-schreibfehler"
    return "identitaet-ergaenzt:" + ",".join(fehlend)


# ---------------------------------------------------------------------------- Ablauf

def main() -> int:
    dry_run = "--dry-run" in sys.argv
    ziele = config_dirs()

    # Bestand einsammeln: je Ordner Login + Identität.
    bestand: list[tuple[Path, dict | None, dict]] = []
    for ordner in ziele:
        bestand.append((ordner, token_lesen(ordner), identitaet_lesen(ordner)))

    # Quelle = brauchbarer Login mit dem spätesten Ablauf.
    quellen = [(o, t) for o, t, _ in bestand if ist_brauchbar(t)]
    if not quellen:
        log("kein brauchbarer Login gefunden -- nichts zu spiegeln", geprueft=len(ziele))
        print("claude-login-sync: kein gültiger Login vorhanden — nichts zu spiegeln.")
        return 0
    quell_ordner, quell_token = max(quellen, key=lambda p: ablauf(p[1], "expiresAt"))

    # Identitäts-Quelle = neueste .claude.json mit oauthAccount.
    ident_quelle: dict = {}
    for ordner, _, ident in bestand:
        if ident.get("oauthAccount"):
            if ordner == quell_ordner or not ident_quelle:
                ident_quelle = ident
            if ordner == quell_ordner:
                break

    berichte: list[str] = []
    for ordner, token, _ in bestand:
        if ordner == quell_ordner:
            berichte.append(f"{ordner.name}: Quelle")
            continue

        schritte: list[str] = []
        if ist_tot(token):
            if dry_run:
                schritte.append("login-wuerde-gesetzt")
            elif token_schreiben(ordner, quell_token):
                schritte.append("login-gesetzt")
            else:
                schritte.append("login-schreibfehler")
        else:
            schritte.append("login-vorhanden")

        if ident_quelle:
            schritte.append(identitaet_saeen(ordner, ident_quelle, dry_run))

        berichte.append(f"{ordner.name}: " + " ".join(schritte))

    log("Abgleich fertig", quelle=str(quell_ordner), dry_run=dry_run, ergebnis=berichte)
    print("claude-login-sync: " + " | ".join(berichte))
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as fehler:  # Ein Abgleich darf niemals eine Sitzung stören.
        log("unerwarteter Fehler", fehler=str(fehler))
        sys.exit(0)
