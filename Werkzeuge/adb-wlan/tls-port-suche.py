#!/usr/bin/env python3
"""Findet den Port von „Debugging über WLAN“ (TLS) eines Geräts über den WireGuard-Tunnel.

Nach einem Neustart lauscht adbd nicht mehr auf 5555, sondern nur noch auf einem zufälligen TLS-Port
(Android-Bereich 32768–60999). mDNS reicht nicht durch den Tunnel, deshalb prüft dieses Skript die Ports
direkt. Ausgabe: offene Ports außer 5555, einer je Zeile. Exit 2, wenn das Gerät gar nicht antwortet
(aus oder Tunnel weg); dann wird nicht gesucht. Aufruf: tls-port-suche.py <ip>
"""
import asyncio
import errno
import socket
import sys

VON, BIS = 32768, 61000
GLEICHZEITIG = 800
WARTEN = 1.5


async def probe(ip: str, port: int, sperre: asyncio.Semaphore) -> bool:
    async with sperre:
        try:
            _, w = await asyncio.wait_for(asyncio.open_connection(ip, port), WARTEN)
            w.close()
            return True
        except Exception:
            return False


def erreichbar(ip: str) -> bool:
    """Gerät antwortet, wenn 5555 offen ist oder sofort abweist (RST); eine Zeitüberschreitung heißt: nicht da."""
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.settimeout(3)
    try:
        return s.connect_ex((ip, 5555)) in (0, errno.ECONNREFUSED)
    finally:
        s.close()


async def suche(ip: str) -> list[int]:
    sperre = asyncio.Semaphore(GLEICHZEITIG)
    ports = list(range(VON, BIS))
    treffer = await asyncio.gather(*(probe(ip, p, sperre) for p in ports))
    return [p for p, t in zip(ports, treffer) if t]


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
