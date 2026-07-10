#!/usr/bin/env python3
"""Statischer Regressionstest fuer die ACL-Integration der Windows-Ersteinrichtung."""
from __future__ import annotations

import sys
from pathlib import Path


def check(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def main() -> int:
    root = Path(__file__).resolve().parents[1]
    setup = (root / "windows" / "wg-setup-elevated.ps1").read_text(encoding="utf-8")
    readme = (root / "windows" / "BACKUP-RESTORE-README.md").read_text(encoding="utf-8")

    check("Join-Path $PSScriptRoot 'set-cortex-ssh-key-acl.ps1'" in setup,
          "Ersteinrichtung muss das dedizierte ACL-Skript relativ zu sich finden")
    check("Join-Path $HOME 'SK\\second-brain\\id_ed25519'" in setup,
          "Ersteinrichtung muss den Standard-Cortex-Schluessel pruefen")
    check("& $aclSetup -KeyPath $cortexKey" in setup,
          "Ersteinrichtung muss das ACL-Skript wirklich ausfuehren")
    check("Cortex-SSH-Schluessel-ACL FEHLER" in setup and "uebersprungen" in setup,
          "Erfolgsaussage darf fehlenden Schluessel oder Skriptfehler nicht verschweigen")
    check("set-cortex-ssh-key-acl.ps1" in readme and "id_cortex" in readme and "-Server SERVER_IP" in readme,
          "Backup-Ersteinrichtung muss ihren eigenen Schluessel absichern und testen")

    print("PASS: Windows-Ersteinrichtung bindet das idempotente ACL-Skript ein")
    return 0


if __name__ == "__main__":
    sys.exit(main())
