"""Baut watchface.bin aus entpackten Theme-Studio-Exporten nach und vergleicht Byte für Byte.

Aufruf: python tests/test_rundreise.py <entpackter Ordner mit watchface.bin und watchface/> ...
"""
import glob
import os
import re
import sys

if len(sys.argv) < 2:
    sys.exit("Fehlt: mindestens ein entpackter Theme-Studio-Export")

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
import hwt

# Schriftkennungen aus den Exporten; Bauen.py braucht sie nicht, es nutzt keine Textebenen.
SCHRIFTEN = {("100", "26"): 136, ("110", "20"): 146}

fehler = 0
for ordner in sys.argv[1:]:
    soll = open(f"{ordner}/watchface.bin", "rb").read()
    config = open(f"{ordner}/watchface/watch_face_config.xml", "rb").read()
    info = open(f"{ordner}/watchface/watch_face_info.xml", encoding="utf-8").read()
    titel = re.search(r"<title>(.*?)</title>", info).group(1)
    titel_cn = re.search(r"<title-cn>(.*?)</title-cn>", info).group(1)
    bilder = [open(f, "rb").read() for f in sorted(glob.glob(f"{ordner}/watchface/res/*.png"))]
    ist = hwt.watchface_bin(config, bilder, titel, titel_cn, SCHRIFTEN)
    gleich = ist == soll
    print(ordner, "gleich" if gleich else f"ABWEICHUNG bei Byte {next((i for i, (a, b) in enumerate(zip(ist, soll)) if a != b), min(len(ist), len(soll)))}", len(ist), len(soll))
    fehler += not gleich
sys.exit(1 if fehler else 0)
