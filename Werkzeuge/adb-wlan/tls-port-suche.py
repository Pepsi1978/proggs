#!/usr/bin/env python3
"""Findet den Port von „Debugging über WLAN“ (TLS) eines Geräts über den WireGuard-Tunnel.

Nach einem Neustart lauscht adbd nicht mehr auf 5555, sondern nur noch auf einem zufälligen TLS-Port
(Android-Bereich 32768–60999). mDNS reicht nicht durch den Tunnel, deshalb prüft dieses Skript die Ports
direkt. Ausgabe: offene Ports außer 5555, einer je Zeile. Exit 2, wenn das Gerät gar nicht antwortet
(aus oder Tunnel weg); dann wird nicht gesucht. Aufruf: tls-port-suche.py <ip>
"""
from __future__ import annotations

import asyncio
import errno
import socket
import sys

VON, BIS = 32768, 61000
WARTEN = 1.5


def gleichzeitig() -> int:
    """Höchstens 800 offene Verbindungen, aber unter der Dateigrenze (macOS unter launchd: 256)."""
    try:
        import resource
        weich, hart = resource.getrlimit(resource.RLIMIT_NOFILE)
        if hart == resource.RLIM_INFINITY or hart > weich:
            try:
                ziel = 4096 if hart == resource.RLIM_INFINITY else min(hart, 4096)
                resource.setrlimit(resource.RLIMIT_NOFILE, (max(weich, ziel), hart))
                weich = max(weich, ziel)
            except (ValueError, OSError):
                pass
        return max(16, min(800, weich - 64))
    except ImportError:  # Windows
        return 400


async def suche(ip: str) -> list[int]:
    ports = iter(range(VON, BIS))
    offen: list[int] = []

    async def arbeiter() -> None:
        for port in ports:
            try:
                _, w = await asyncio.wait_for(asyncio.open_connection(ip, port), WARTEN)
                w.close()
                offen.append(port)
            except Exception:
                pass

    await asyncio.gather(*(arbeiter() for _ in range(gleichzeitig())))
    return sorted(offen)


def erreichbar(ip: str) -> bool:
    """Gerät antwortet, wenn 5555 offen ist oder sofort abweist (RST); eine Zeitüberschreitung heißt: nicht da."""
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.settimeout(3)
    try:
        return s.connect_ex((ip, 5555)) in (0, errno.ECONNREFUSED)
    finally:
        s.close()


def main() -> int:
    if len(sys.argv) < 2:
        print("Aufruf: tls-port-suche.py <ip>", file=sys.stderr)
        return 1
    ip = sys.argv[1]
    if not erreichbar(ip):
        return 2
    for p in asyncio.run(suche(ip)):
        print(p)
    return 0


if __name__ == "__main__":
    sys.exit(main())
