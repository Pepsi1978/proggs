"""Liest am Mac die macOS-Version und den freien Speicherplatz, nur lesend und ohne Konten."""
import json
import re
import shutil
import subprocess
import sys

SW_VERS = "/usr/bin/sw_vers"


class HelferFehler(Exception):
    pass


def eingabe_pruefen(roh):
    try:
        eingabe = json.loads(roh)
    except (ValueError, RecursionError):
        raise HelferFehler("Die Eingabe ist kein gültiges JSON.")
    if not isinstance(eingabe, dict):
        raise HelferFehler("Die Eingabe muss ein JSON-Objekt sein.")
    if eingabe:
        namen = ", ".join(json.dumps(k, ensure_ascii=False) for k in sorted(eingabe))[:200]
        raise HelferFehler("Unbekannte Eingabe: " + namen + ". Dieser Helfer nimmt keine Angaben entgegen.")


def macos_lesen():
    try:
        lauf = subprocess.run(
            [SW_VERS],
            stdin=subprocess.DEVNULL,
            stdout=subprocess.PIPE,
            stderr=subprocess.DEVNULL,
            timeout=10,
        )
    except (OSError, subprocess.SubprocessError) as fehler:
        raise HelferFehler("sw_vers ließ sich nicht ausführen: " + str(fehler))
    if lauf.returncode != 0:
        raise HelferFehler("sw_vers endete mit Exit-Code " + str(lauf.returncode) + ".")
    werte = {}
    for zeile in lauf.stdout.decode("utf-8", "replace").splitlines():
        name, trenner, wert = zeile.partition(":")
        if trenner:
            werte[name.strip()] = wert.strip()
    version = werte.get("ProductVersion", "")
    if not re.fullmatch(r"[0-9]+(\.[0-9]+)*", version):
        raise HelferFehler("sw_vers lieferte keine lesbare Version.")
    # Schnelle Sicherheitsnachträge von Apple stehen getrennt, etwa „(a)“.
    return version, werte.get("ProductVersionExtra", ""), werte.get("BuildVersion", "")


def speicher_lesen():
    try:
        # Auf APFS meldet "/" den gemeinsamen Container, also auch den Platz für Benutzerdaten.
        return shutil.disk_usage("/")
    except OSError as fehler:
        raise HelferFehler("Der Speicherplatz ließ sich nicht lesen: " + str(fehler))


def gb_text(bytes_anzahl):
    # Dezimale Einheit wie bei macOS; die Menge kann wegen löschbarer Daten vom Finder abweichen.
    return ("%.1f" % (bytes_anzahl / 1e9)).replace(".", ",") + " GB"


def main():
    eingabe_pruefen(sys.stdin.buffer.read())
    version, zusatz, build = macos_lesen()
    speicher = speicher_lesen()
    anzeige = version + (" " + zusatz if zusatz else "") + (" (Build " + build + ")" if build else "")
    text = (
        "Auf dem Mac läuft macOS %s. Sofort frei sind %s von %s; "
        "macOS kann zusätzlich löschbare Daten freigeben, der Finder zeigt deshalb oft mehr."
    ) % (anzeige, gb_text(speicher.free), gb_text(speicher.total))
    ergebnis = {
        "text": text,
        "daten": {
            "macos_version": version,
            "macos_version_zusatz": zusatz,
            "macos_build": build,
            "speicher_frei_bytes": speicher.free,
            "speicher_gesamt_bytes": speicher.total,
        },
        "dateien": [],
    }
    # ensure_ascii hält die Ausgabe auch ohne gesetzte Sprachumgebung sauber.
    sys.stdout.write(json.dumps(ergebnis) + "\n")


if __name__ == "__main__":
    try:
        main()
    except HelferFehler as fehler:
        sys.stderr.buffer.write((str(fehler) + "\n").encode("utf-8", "replace"))
        sys.exit(1)
    except Exception as fehler:
        sys.stderr.buffer.write(("Die Systemangaben ließen sich nicht lesen (%s).\n" % type(fehler).__name__).encode("utf-8"))
        sys.exit(1)
