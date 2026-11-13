#!/usr/bin/env python3
"""Findet den Port von „Debugging über WLAN“ (TLS) eines Geräts über den WireGuard-Tunnel.

Nach einem Neustart lauscht adbd nicht mehr auf 5555, sondern nur noch auf einem zufälligen TLS-Port
(Android-Bereich 32768–60999). mDNS reicht nicht durch den Tunnel, deshalb prüft dieses Skript die Ports
direkt: erst welche offen sind, dann, welche wie adbd antworten (auf ein CNXN-Paket folgt STLS, CNXN oder AUTH).
Ausgabe: nur diese adb-Ports, einer je Zeile. Exit 2, wenn das Gerät gar nicht antwortet (aus oder Tunnel weg);
dann wird nicht gesucht. Aufruf: tls-port-suche.py <ip>
"""
from __future__ import annotations

import asyncio
import errno
import socket
import struct
import sys

VON, BIS = 32768, 61000
WARTEN = 3.0          # Mobilfunk-Tunnel: SYN-ACK kommt unter Last oft erst nach über einer Sekunde
ANTWORT_WARTEN = 4.0
KNAPP = (errno.EMFILE, errno.ENFILE, errno.ENOBUFS)


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


async def offen(ip: str, port: int) -> bool:
    for versuch in range(3):
        try:
            _, w = await asyncio.wait_for(asyncio.open_connection(ip, port), WARTEN)
            w.close()
            return True
        except OSError as e:
            if e.errno in KNAPP and versuch < 2:   # Dateigrenze kurz erreicht: nicht als „zu“ werten
                await asyncio.sleep(0.5)
                continue
            return False
        except Exception:
            return False
    return False


def cnxn_paket() -> bytes:
    nutz = b"host::\0"
    befehl = 0x4E584E43  # "CNXN"
    return struct.pack("<6I", befehl, 0x01000001, 256 * 1024, len(nutz), sum(nutz) & 0xFFFFFFFF,
                       befehl ^ 0xFFFFFFFF) + nutz


async def ist_adb(ip: str, port: int) -> bool:
    """adbd antwortet auf CNXN mit STLS (WLAN-Debugging), CNXN oder AUTH; andere Dienste nicht."""
    try:
        r, w = await asyncio.wait_for(asyncio.open_connection(ip, port), WARTEN)
    except Exception:
        return False
    try:
        w.write(cnxn_paket())
        await w.drain()
        kopf = await asyncio.wait_for(r.readexactly(4), ANTWORT_WARTEN)
        return kopf in (b"STLS", b"CNXN", b"AUTH")
    except Exception:
        return False
    finally:
        w.close()


async def suche(ip: str) -> list[int]:
    ports = iter(range(VON, BIS))
    gefunden: list[int] = []

    async def arbeiter() -> None:
        for port in ports:
            if await offen(ip, port):
                gefunden.append(port)

    await asyncio.gather(*(arbeiter() for _ in range(gleichzeitig())))
    gefunden = sorted(p for p in gefunden if p != 5555)
    pruefung = await asyncio.gather(*(ist_adb(ip, p) for p in gefunden))
    return [p for p, ok in zip(gefunden, pruefung) if ok]


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
