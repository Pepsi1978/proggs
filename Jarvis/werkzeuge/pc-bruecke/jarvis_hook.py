"""Claude-Code-Hook für Programmieraufträge von Jarvis (nur in Sitzungen, die OpenLauncher --jarvis-auftrag gestartet hat).

Der OpenLauncher hängt diesen Hook nur an Jarvis-Sitzungen (über --settings) und setzt JARVIS_AUFTRAG_ID. Ohne die Variable
endet der Hook sofort ohne Ausgabe: In normalen Sitzungen passiert nie etwas.

Aufruf: python jarvis_hook.py <post|frage|stop|puls|warte|fehlschlag> (macOS: python3), die Hook-Eingabe kommt als JSON auf stdin.
  melde  Kein Hook: python jarvis_hook.py melde <laeuft|fertig|fehler> "<Satz>" meldet aus einer Sitzung ohne Hooks (Codex CLI
         mit Sol 6.1, gestartet mit OpenLauncher --cli codex) einen Schritt an Jarvis.
Eine Datei für Windows und macOS; der Mac-Start (OpenLauncherMac --jarvis-auftrag) setzt dieselben Hooks.
OpenCode (Sol 6.1, OpenLauncher --cli opencode) hat keine Claude-Hooks: Das Plugin opencode-setup/plugins/jarvis-auftrag.js ruft
post, puls, frage, stop und warte mit derselben Eingabe auf (Werkzeugnamen wie bei Claude, statt transcript_path die Liste
„texte“ mit den Zwischentexten der KI) und setzt JARVIS_CLI=opencode und JARVIS_ELTERN_PID (der OpenCode-Prozess für den Warter).
  post   PostToolUse (alle Werkzeuge) und PostToolUseFailure (Bash|PowerShell, nur dort stehen fehlgeschlagene Befehle).
         Läuft async. Neue Zwischentexte der KI (aus dem Transkript) gehen sofort als eigene Meldung raus, dazu Advisor,
         Skills und Agenten; geänderte Dateien, Bau, Commit/Push und adb install werden gedrosselt:
         höchstens eine Meldung je DROSSEL_S, Fehler sofort. Lesen und Suchen meldet niemand (anderer Matcher).
  frage  PreToolUse (AskUserQuestion): schickt Fragen und Optionen an Jarvis, wartet bis FRAGE_WARTEN_S auf Franks Antwort
         und gibt sie an Claude zurück. Tippt Frank in Jarvis „In der Claude-App beantworten“, gibt der Hook frei, und der
         normale Dialog erscheint (auch per Remote Control). Kommt bis zum Ablauf keine Antwort oder ist Jarvis nicht
         erreichbar, antwortet der Hook selbst (ohne_antwort: Review = Nein, sonst selbst entscheiden), denn ein offener
         Dialog ließe die Sitzung stehen (Franks Vorgabe vom 23.10.2026: nie). Solange der Hook wartet, zeigt die
         Claude-App „Running PreToolUse hooks 1 of 2“ und nimmt keine Antwort an: Claude Code öffnet seinen Dialog erst nach dem Hook.
         Jeder Ausgang geht als stand „frage_status“ an Jarvis (beantwortet, am_pc, erledigt), damit die Frage dort
         überall als erledigt gilt, auch wenn sie am Rechner beantwortet wurde (post erkennt das am AskUserQuestion-Ergebnis).
  puls   PreToolUse (alle Werkzeuge, async): Lebenszeichen für die drehende Anzeige im Jarvis-Chat, höchstens alle PULS_S.
  stop   Stop: meldet den Zwischenstand (letzte Nachricht). Endet sie mit einer Frage, wartet der Hook auf Franks Antwort und
         lässt Claude damit weiterarbeiten (decision block). Liegen Nachträge zu diesem Auftrag bereit, gehen sie danach
         ebenso als decision block in dieselbe Sitzung.
  fehlschlag  StopFailure (Claude Code ab 2.1.x, Feld „error“): Der Zug endete an einem API-Fehler statt mit Stop. Ein
         Anmeldefehler (ANMELDE_FEHLER, etwa „Login expired · Please run /login“) geht als stand „anmeldung“ an Jarvis, jeder
         andere API-Fehler als „stoerung“. Ausgabe und Exit-Code ignoriert Claude Code.
  warte  SessionStart und Stop mit asyncRewake: wartet im Hintergrund am Relay auf Nachträge zu diesem Auftrag (höchstens
         einer je Auftrag, bis WARTER_MAX_S oder bis die Claude-Sitzung endet) und weckt die Sitzung mit Exit 2 und dem
         Nachtrag, auch wenn sie gerade nichts tut. Jeder Nachtrag geht genau einmal hinein (nachtrag_id im Zustand) und
         wird mit stand „nachtrag“ quittiert; erst dann nimmt der Relay ihn aus der Warteschlange.
         Dazu prüft er bei jeder Runde (höchstens alle 25 s, nur eine Datei lesen) das Transkript auf API-Fehler: Steht die
         Sitzung an einem Anmeldefehler, läuft weder ein Werkzeug- noch der Stop-Hook, nur noch er (Vorfall vom 21.10.2026:
         „Login expired“ um 10:41, gemeldet erst nach Franks /login um 11:01).

Lokale Sitzungen (am Rechner direkt gestartet, nicht über Jarvis): Die Claude-Code-Profile des OpenLaunchers rufen in jeder
Sitzung python jarvis_hook.py lokal <art> auf. In Jarvis-Sitzungen (JARVIS_AUFTRAG_ID gesetzt) und ohne interaktive CLI
(claude -p, SDK) endet das sofort. Sonst bekommt die Sitzung eine feste Nummer aus ihrer session_id (lokale_nummer, gleich
auch nach --resume) und meldet wie ein Jarvis-Auftrag, mit dem Feld „lokal“ (Name, Ordner, Rechner, Claude-App-Link).
Abweichungen: Eine Auswahlfrage (frage) geht immer an Jarvis; berührt Frank am Rechner nach FRAGE_SCHONFRIST_S Tastatur
oder Maus, und nach Ablauf, erscheint der normale Dialog am Rechner, nie eine Antwort an Franks Stelle. Stop läuft
async: Eine freie Rückfrage am Zugende wartet nicht, Franks Antwort in Jarvis kommt als Nachtrag über den Warter. prompt
(UserPromptSubmit) zeigt in Jarvis, was am Rechner eingegeben wurde. Entfernt Frank die Sitzung in Jarvis, antwortet der
Relay „entfernt“: Die Marke entfernt-<Nummer> lässt danach jeden Hook still enden; die Sitzung selbst läuft weiter.
Sitzungen ohne diese Hooks (vor dem Update gestartet) liest jarvis_lokal.py über das Sitzungsregister nur mit.

Anmeldung: Ein Anmeldefehler sperrt den Auftrag in Jarvis („Anmeldung erforderlich“), bis die Sitzung beweist, dass sie
wieder arbeitet: „Login successful“ von /login im Transkript, ein Werkzeug oder ein Stop, das nach dem Fehler begann. Dann
geht stand „anmeldung_ok“ (bzw. „weiter“ nach einer Störung) an Jarvis. Tokens und Zugangsdaten liest der Hook nie.

Wege: Relay https://<host>/pc/meldung und /pc/antwort mit dem Geräte-Token aus ~/SK/Jarvis/relay.properties, nur IPv4
(auf diesem Netz hängt IPv6 zeitweise, siehe bugs/desktop/dotnet-httpclient-ipv6-haenger.md).
Protokoll: %LOCALAPPDATA%/JarvisPcBruecke/hook.log, macOS ~/Library/Logs/JarvisPcBruecke/hook.log
"""
from __future__ import annotations

import json
import os
import re
import socket
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import hashlib
import uuid
from pathlib import Path

AUFTRAG = os.environ.get("JARVIS_AUFTRAG_ID", "").strip()
# Am Rechner gestartete Sitzung (siehe Kopf): setze_auftrag(lokale_nummer(session_id)), dazu ihre Angaben für Jarvis.
LOKAL = False
LOKAL_INFO: dict = {}
# Gesetzt vom OpenCode-Plugin: Diese Sitzung ist kein Claude Code und beweist nichts über die Claude-Anmeldung des Rechners.
FREMDE_CLI = os.environ.get("JARVIS_CLI", "").strip()
START = time.time()        # Beginn dieses Hooks: Nur was danach begann, beweist eine wieder gültige Anmeldung
# Werte des Felds „error“ (Transkript und StopFailure, Claude Code 2.1.306), die heißen: hier ist niemand gültig angemeldet.
ANMELDE_FEHLER = {"authentication_failed", "oauth_org_not_allowed", "account_on_hold", "verification_required"}
DROSSEL_S = 20
FRAGE_WARTEN_S = 300      # Franks Wahl vom 18.10.2026: höchstens fünf Minuten nur in Jarvis
# Werkzeugbau-Aufträge (Nummer beginnt mit W0, die 0 kommt in normalen Nummern nie vor): Die 150 Sekunden zählt das Handy ab der Anzeige.
# Hier nur das Sicherheitsnetz, falls das Handy aus ist. Alle anderen Aufträge bleiben bei fünf Minuten.
WERKZEUGBAU_WARTEN_S = 900


def frage_warten_s(nummer: str) -> int:
    return WERKZEUGBAU_WARTEN_S if nummer.startswith("W0") else 300


FRAGE_WARTEN_S = frage_warten_s(AUFTRAG)
PULS_S = 10               # Lebenszeichen höchstens so oft
MAX_TEXT = 4000           # so lang darf eine Zwischenmeldung werden (sichtbar gekürzt, siehe kuerze)
MAX_BERICHT = 60000       # der Abschlussbericht am Zugende („info“); früher galt auch hier 4000 und er endete mitten im Wort
GEKUERZT = "… [gekürzt, der vollständige Text steht in der Sitzung]"
if os.environ.get("LOCALAPPDATA"):
    ORDNER = Path(os.environ["LOCALAPPDATA"]) / "JarvisPcBruecke"
elif sys.platform == "darwin":
    ORDNER = Path.home() / "Library" / "Logs" / "JarvisPcBruecke"
else:
    ORDNER = Path.home() / "JarvisPcBruecke"
PROTOKOLL = ORDNER / "hook.log"
ZUSTAND = ORDNER / f"hook-{AUFTRAG or 'ohne'}.json"


def schreib(text: str) -> None:
    try:
        ORDNER.mkdir(parents=True, exist_ok=True)
        # Wie bruecke.log: ab 1 MB eine Generation zurück, sonst wächst das Protokoll mit jeder Meldung ohne Grenze.
        if PROTOKOLL.exists() and PROTOKOLL.stat().st_size > 1024 * 1024:
            os.replace(PROTOKOLL, PROTOKOLL.with_name("hook.log.alt"))
        with PROTOKOLL.open("a", encoding="utf-8") as f:
            f.write(f"{time.strftime('%Y-%m-%d %H:%M:%S')} [{AUFTRAG}] {text}\n")
    except Exception:
        pass


# Nur IPv4: getaddrinfo liefert sonst zuerst IPv6, und die Verbindung hängt bis zum Zeitlimit.
_original = socket.getaddrinfo


def _nur_ipv4(host, port, family=0, *args, **kwargs):
    return _original(host, port, socket.AF_INET, *args, **kwargs)


socket.getaddrinfo = _nur_ipv4


def zugang() -> tuple[str, str]:
    werte = {}
    for zeile in (Path.home() / "SK" / "Jarvis" / "relay.properties").read_text(encoding="utf-8").splitlines():
        m = re.match(r"^\s*([^#=]+?)\s*=\s*(.*)$", zeile)
        if m:
            werte[m.group(1)] = m.group(2).strip()
    return werte["host"], werte["token"]


def rufe(pfad: str, daten: dict | None = None, zeitlimit: float = 5) -> tuple[int, dict]:
    host, token = zugang()
    anfrage = urllib.request.Request(f"https://{host}{pfad}", method="POST" if daten is not None else "GET",
                                     headers={"X-Jarvis-Token": token, "Content-Type": "application/json; charset=utf-8"},
                                     data=json.dumps(daten, ensure_ascii=False).encode("utf-8") if daten is not None else None)
    try:
        with urllib.request.urlopen(anfrage, timeout=zeitlimit) as antwort:
            roh = antwort.read().decode("utf-8")
            return antwort.status, (json.loads(roh) if roh.strip() else {})
    except urllib.error.HTTPError as fehler:
        return fehler.code, {}


_RECHNER = []


def rechner() -> str:
    """Derselbe Name, mit dem sich die Brücke beim Relay meldet (X-Jarvis-Rechner)."""
    if not _RECHNER:
        _RECHNER.append(_rechnername())
    return _RECHNER[0]


def _rechnername() -> str:
    if os.environ.get("JARVIS_RECHNER"):
        return os.environ["JARVIS_RECHNER"]
    if os.name == "nt":
        return os.environ.get("COMPUTERNAME") or socket.gethostname()
    if sys.platform == "darwin":
        import subprocess
        try:
            return subprocess.run(["scutil", "--get", "ComputerName"], capture_output=True, text=True, timeout=5).stdout.strip() or "Mac"
        except Exception:
            pass
    return socket.gethostname()


def kuerze(text: str, grenze: int) -> str:
    """Höchstens [grenze] Zeichen, nie ein unmarkiertes Satzfragment: am letzten Zeilen-, Satz- oder Wortende, dahinter GEKUERZT.
    Gleiche Regel wie ProgrammierAbschluss.kuerzen in der App."""
    if len(text) <= grenze:
        return text
    platz = max(1, grenze - len(GEKUERZT) - 2)
    kopf = text[:platz]
    mindestens = platz * 6 // 10
    satz = max((m.end() for m in re.finditer(r"[.!?](?=\s)", kopf)), default=-1)
    schnitt = max(kopf.rfind("\n"), satz)
    if schnitt < mindestens:
        schnitt = kopf.rfind(" ")
    if schnitt < mindestens:
        schnitt = platz
    return kopf[:schnitt].rstrip() + "\n" + GEKUERZT


def installweg(befehl: str) -> str:
    """Wie adb installiert: „ (Tunnel)“ über den WireGuard-Tunnel (10.8.0.0/24 oder die Adresse in ~/.adb-wlan-tunnel-ip, wie
    Werkzeuge/adb-wlan), „ (WLAN)“ bei einer anderen privaten Adresse, „ (Kabel)“ bei einer Seriennummer; ohne -s (oder über
    Variablen) oder bei einer unbekannten Adresse bleibt der Weg offen."""
    # Nur das -s des adb-Aufrufs selbst (nicht etwa „gradlew … -s“ davor), bis zum nächsten &&, ; oder |.
    m = re.search(r"\badb(?:\.exe)?[\"']?\s(?:[^&;|]*?\s)?-s\s+[\"']?([^\s\"']+)", befehl)
    if not m or "$" in m.group(1) or "%" in m.group(1):
        return ""
    ziel = m.group(1)
    if "._adb-tls-connect." in ziel:
        return " (WLAN)"   # Drahtloses Debugging per mDNS
    host = ziel.rsplit(":", 1)[0] if ":" in ziel else ""
    if not host:
        return " (Kabel)"
    tunnel = ""
    try:
        tunnel = (Path.home() / ".adb-wlan-tunnel-ip").read_text(encoding="utf-8").strip()
    except OSError:
        pass
    if host.startswith("10.8.0.") or (tunnel and host == tunnel):
        return " (Tunnel)"
    if re.match(r"^(192\.168\.|10\.|172\.(1[6-9]|2\d|3[01])\.)", host):
        return " (WLAN)"
    return ""


def melde(stand: str, schritt: str, frage: dict | None = None, nachtrag_ids: list[str] | None = None, extra: dict | None = None) -> int:
    # rechner und hook_beginn: Der Relay führt damit den Anmeldestand je Rechner (nur was nach einem Fehler begann, bestätigt ihn).
    daten = {"auftrag_id": AUFTRAG, "stand": stand, "schritt": kuerze(schritt, MAX_BERICHT if stand == "info" else MAX_TEXT),
             "rechner": "" if FREMDE_CLI else rechner(), "hook_beginn": int(START), **(extra or {})}
    if frage:
        daten["frage"] = frage
    if nachtrag_ids:
        daten["nachtrag_ids"] = nachtrag_ids
    if LOKAL:
        daten["lokal"] = LOKAL_INFO
    code, antwort = rufe("/pc/meldung", daten)
    schreib(f"Meldung {stand}: {schritt[:120]} -> {code}")
    # Frank hat die Sitzung in Jarvis entfernt: ab jetzt still (die Sitzung selbst läuft weiter).
    if LOKAL and antwort.get("entfernt"):
        merke_entfernt()
    return code


def melde_still(stand: str, schritt: str) -> None:
    """Melden, wenn die eigentliche Ausgabe schon steht: Ein Netzfehler wird nur protokolliert und bricht nichts ab."""
    try:
        melde(stand, schritt)
    except Exception as fehler:
        schreib(f"Meldung {stand} nicht gesendet: {fehler}")


def melde_sicher(stand: str, schritt: str, frage: dict | None = None, versuche: int = 5) -> bool:
    """Für Rückfragen und ihren Ausgang: mehrmals versuchen (Relay-Neustart, Funkloch). Doppelt schadet nicht, Jarvis und
    der Relay erkennen dieselbe frage_id wieder."""
    for versuch in range(versuche):
        try:
            if melde(stand, schritt, frage) == 200:
                return True
        except Exception as fehler:
            schreib(f"Meldung {stand} Versuch {versuch + 1}: {fehler}")
        time.sleep(min(8, 1 + 2 * versuch))
    schreib(f"Meldung {stand} nach {versuche} Versuchen aufgegeben")
    return False


def frage_status(frage_id: str, status: str, antwort: str = "", grund: str = "", versuche: int = 3) -> bool:
    """Ausgang einer Rückfrage an Relay und Jarvis: beantwortet (in Jarvis), am_pc (Dialog am Rechner), erledigt."""
    return melde_sicher("frage_status", "", {"frage_id": frage_id, "status": status, "antwort": antwort[:2000], "grund": grund}, versuche=versuche)


def warte_auf_antwort(frage_id: str, neu_melden=None, am_pc=None) -> dict | None:
    """Franks Antwort, {"freigeben": True}, {"geschlossen": True} (in Jarvis ohne Antwort geschlossen) oder None (abgelaufen).
    404 heißt nicht „abgelaufen“: Der Relay hat die Frage verloren (Neustart) oder kannte sie noch nicht. Dann meldet
    [neu_melden] sie höchstens alle 30 s erneut, und es wird bis zur Frist weiter gewartet; Frank hat die Frage ja vor sich.
    [am_pc] (lokale Sitzung): Liefert es True, sitzt Frank wieder am Rechner, und es kommt {"freigeben": True, "am_pc": True};
    dann fragt der Hook in kurzen Stücken (der Relay hält eine Abfrage sonst bis zu 25 s)."""
    frist = time.monotonic() + FRAGE_WARTEN_S
    zuletzt_neu = time.monotonic()
    while time.monotonic() < frist:
        # Lokal: Frank sitzt wieder am Rechner, oder er hat die Sitzung in Jarvis entfernt (dann fragt dort niemand mehr).
        if am_pc and (am_pc() or ist_entfernt()):
            return {"freigeben": True, "am_pc": True}
        try:
            code, daten = rufe("/pc/antwort?frage_id=" + urllib.parse.quote(frage_id), zeitlimit=8 if am_pc else 35)
        except Exception as fehler:
            if not (am_pc and isinstance(fehler, (TimeoutError, socket.timeout))):
                schreib(f"Antwort abholen: {fehler}")
                time.sleep(3)
            continue
        if code == 200:
            if daten.get("freigeben"):
                return {"freigeben": True}
            return daten
        if code == 410:
            return {"geschlossen": True}
        if code == 404:
            if neu_melden and time.monotonic() - zuletzt_neu >= 30:
                zuletzt_neu = time.monotonic()
                schreib("Rückfrage beim Relay unbekannt: erneut gemeldet")
                try:
                    neu_melden()
                except Exception as fehler:
                    schreib(f"Erneut melden: {fehler}")
            time.sleep(3)
            continue
        # 204 = noch keine Antwort (der Relay hat schon gewartet); alles andere (etwa 502 beim Neustart des Relays) kommt
        # sofort zurück und darf die Schleife nicht ohne Pause gegen den Server laufen lassen.
        if code != 204:
            time.sleep(3)
    return None


# ---------------------------------------------------------------- Zustand (Drosselung, gesehene Texte)

# Neuer Name (.lock statt .sperre): Die frühere Sperrdatei-Logik noch laufender Warter kommt dieser nie in die Quere.
SPERRE = ORDNER / f"hook-{AUFTRAG or 'ohne'}.lock"
SPERRE_WARTEN_S = 10


class SperreBelegt(Exception):
    pass


class Sperre:
    """
    Async-Hooks laufen gleichzeitig: Zustand und Transkript-Stelle nur einer nach dem anderen. Eine echte Dateisperre des
    Betriebssystems: Endet ein Hook, auch hart an seinem Zeitlimit, gibt das System sie frei, und nie löscht ein Hook die
    Sperre eines anderen. Darin nur Zustand und Transkript lesen und schreiben, nie Netz; gesendet wird danach. Ist sie
    nach SPERRE_WARTEN_S noch belegt, kommt SperreBelegt: lieber diesen Hook auslassen (der nächste holt die Texte nach)
    als ohne Sperre doppelt melden oder einen Nachtrag zweimal übergeben.
    """

    def __enter__(self):
        ORDNER.mkdir(parents=True, exist_ok=True)
        self.fd = os.open(SPERRE, os.O_RDWR | os.O_CREAT)
        frist = time.monotonic() + SPERRE_WARTEN_S
        while True:
            try:
                if os.name == "nt":
                    import msvcrt
                    os.lseek(self.fd, 0, os.SEEK_SET)
                    msvcrt.locking(self.fd, msvcrt.LK_NBLCK, 1)
                else:
                    import fcntl
                    fcntl.flock(self.fd, fcntl.LOCK_EX | fcntl.LOCK_NB)
                # Frisch halten: Das Aufräumen der Brücken löscht nur Sperrdateien, die 14 Tage niemand benutzt hat.
                try:
                    os.utime(SPERRE)
                except OSError:
                    pass
                return self
            except OSError:
                if time.monotonic() >= frist:
                    os.close(self.fd)
                    raise SperreBelegt(f"Sperre nach {SPERRE_WARTEN_S} s noch belegt")
                time.sleep(0.05)

    def __exit__(self, *_):
        try:
            if os.name == "nt":
                import msvcrt
                os.lseek(self.fd, 0, os.SEEK_SET)
                msvcrt.locking(self.fd, msvcrt.LK_UNLCK, 1)
            else:
                import fcntl
                fcntl.flock(self.fd, fcntl.LOCK_UN)
        except OSError:
            pass
        finally:
            os.close(self.fd)


def lies_zustand() -> dict:
    try:
        z = json.loads(ZUSTAND.read_text(encoding="utf-8"))
    except Exception:
        z = {}
    for k, v in (("zuletzt", 0), ("dateien", []), ("offen", []), ("gesehen", []), ("stelle", 0), ("beginn", time.time()), ("nachtraege", [])):
        z.setdefault(k, v)
    return z


def dauer_text(sekunden: float) -> str:
    s = int(max(0, sekunden))
    if s < 60:
        return f"{s} s"
    if s < 3600:
        return f"{s // 60} min {s % 60} s"
    return f"{s // 3600} h {s % 3600 // 60} min"


def schreib_zustand(z: dict) -> None:
    try:
        ORDNER.mkdir(parents=True, exist_ok=True)
        # Atomar: Ein gleichzeitig lesender Hook darf nie eine halb geschriebene Datei sehen (sonst gälte alles als ungesehen
        # und die ganze Sitzung ginge noch einmal an Jarvis).
        teil = ZUSTAND.with_name(ZUSTAND.name + f".{os.getpid()}.tmp")
        teil.write_text(json.dumps(z, ensure_ascii=False), encoding="utf-8")
        os.replace(teil, ZUSTAND)
    except Exception:
        pass


def sammel(z: dict, neu: str | None, sofort: bool = False, stand: str = "laeuft") -> list[tuple[str, str]]:
    """
    Hängt [neu] an die offenen Punkte; fällig wird eine Meldung, wenn DROSSEL_S um sind oder [sofort]. Liefert die zu
    sendenden Meldungen und vermerkt sie im Zustand schon als gesendet: Gesendet wird erst nach der Sperre (sende).
    """
    if neu:
        z["offen"].append(neu)
    jetzt = time.time()
    if not (sofort or jetzt - z.get("zuletzt", 0) >= DROSSEL_S):
        return []
    teile = []
    if z["dateien"]:
        namen = list(dict.fromkeys(z["dateien"]))
        teile.append("✏️ Geändert: " + ", ".join(namen[:6]) + (f" und {len(namen) - 6} weitere" if len(namen) > 6 else ""))
    teile += z["offen"]
    if not teile:
        return []
    z.update({"zuletzt": jetzt, "dateien": [], "offen": []})
    return [(stand, " · ".join(teile))]


def sende(meldungen: list[tuple[str, str]]) -> None:
    """Sendet außerhalb der Sperre. Höchstens einmal: Ist der Relay weg, fällt der Rest dieses Hooks aus, statt dass
    später alles doppelt kommt."""
    for stand, text in meldungen:
        try:
            melde(stand, text)
        except Exception as fehler:
            schreib(f"Meldung {stand} nicht gesendet, Rest ausgelassen: {fehler}")
            return


def lesbar(text: str) -> str:
    """Code-Zeichen weg, Zeilen bleiben. **Fett** bleibt: Jarvis zeigt es im Chat fett an."""
    return text.replace("`", "").strip()


def schon_gesehen(z: dict, text: str) -> bool:
    """Merkt sich die letzten 200 gemeldeten Texte, damit Zwischentext und Schlussmeldung nicht doppelt kommen."""
    schluessel = hashlib.sha1(text.strip().encode("utf-8")).hexdigest()[:16]
    if schluessel in z["gesehen"]:
        return True
    z["gesehen"] = (z["gesehen"] + [schluessel])[-200:]
    return False


def neue_texte(eingabe: dict, z: dict) -> list[str]:
    """Zwischentexte der KI, die seit dem letzten Lesen im Transkript dazugekommen sind (nur vollständige Zeilen)."""
    pfad = eingabe.get("transcript_path")
    if not pfad:
        # OpenCode: Das Plugin liefert die fertigen Texte seit dem letzten Aufruf selbst mit.
        texte = []
        for text in eingabe.get("texte") or []:
            text = lesbar(str(text))
            if text and not schon_gesehen(z, text):
                texte.append(text)
        return texte
    texte = []
    try:
        with open(pfad, "rb") as f:
            groesse = os.fstat(f.fileno()).st_size
            stelle = z["stelle"] if 0 <= z["stelle"] <= groesse else 0
            f.seek(stelle)
            roh = f.read()
    except Exception:
        return []
    ende = roh.rfind(b"\n")
    if ende < 0:
        return []
    z["stelle"] = stelle + ende + 1
    for zeile in roh[:ende].decode("utf-8", "replace").splitlines():
        try:
            eintrag = json.loads(zeile)
        except Exception:
            continue
        nachricht = eintrag.get("message") or {}
        # API-Fehler („Login expired · Please run /login“) sind kein Text der KI: Die meldet pruefe_transkript als Sperre.
        if eintrag.get("type") != "assistant" or nachricht.get("role") != "assistant" or eintrag.get("isApiErrorMessage"):
            continue
        inhalt = nachricht.get("content")
        bloecke = [inhalt] if isinstance(inhalt, str) else [b.get("text", "") for b in inhalt or [] if isinstance(b, dict) and b.get("type") == "text"]
        for text in bloecke:
            text = lesbar(text)
            if text and not schon_gesehen(z, text):
                texte.append(text)
    return texte


# ---------------------------------------------------------------- Anmeldung und API-Fehler

def _zeit(stempel) -> float:
    """Zeitstempel des Transkripts („2026-10-21T08:41:46.210Z“, UTC) in Sekunden seit 1970; ohne lesbaren jetzt."""
    from datetime import datetime, timezone
    try:
        return datetime.strptime(str(stempel)[:19], "%Y-%m-%dT%H:%M:%S").replace(tzinfo=timezone.utc).timestamp()
    except Exception:
        return time.time()


def sperre_melden(z: dict, fehler: str, text: str, seit: float) -> list[tuple[str, str, dict]]:
    """
    Ein API-Fehler hat den Zug beendet: Anmeldefehler als „anmeldung“, alle anderen als „stoerung“. Nur einmal je Sperre
    (Claude Code wiederholt, und Transkript und StopFailure sehen denselben Fehler).
    """
    art = "anmeldung" if fehler in ANMELDE_FEHLER else "stoerung"
    if (z.get("sperre") or {}).get("art") == art:
        return []
    z["sperre"] = {"art": art, "seit": seit, "fehler": fehler}
    uhr = time.strftime("%H:%M", time.localtime(seit))
    name = rechner()
    extra = {"seit": int(seit), "fehler": fehler}
    if art == "anmeldung":
        return [("anmeldung", f"🔐 Anmeldung erforderlich: Claude Code auf {name} ist seit {uhr} Uhr nicht angemeldet "
                              f"(„{text.strip()[:120] or fehler}“). Die Sitzung arbeitet nicht weiter. Claude Code auf dem Rechner öffnen und "
                              f"/login ausführen; danach setzt sie den Auftrag erfahrungsgemäß von selbst fort (sonst dort „weiter“ schreiben).", extra)]
    return [("stoerung", f"⚠️ Angehalten: Der Zug endete um {uhr} Uhr an einem API-Fehler ({fehler}): {text.strip()[:300]}", extra)]


def sperre_aufheben(z: dict, beweis: str, zeit: float = START) -> list[tuple[str, str, dict]]:
    """Die Sitzung arbeitet nachweislich wieder ([beweis] um [zeit], nach dem Fehler): Sperre aufheben und melden."""
    sperre = z.get("sperre") or {}
    if not sperre:
        return []
    z.pop("sperre", None)
    # hook_beginn = Zeitpunkt des Beweises: Der Warter, der /login sieht, lief schon vor dem Fehler.
    extra = {"seit": int(time.time()), "hook_beginn": int(zeit)}
    if sperre.get("art") == "anmeldung":
        return [("anmeldung_ok", f"🔓 Wieder angemeldet ({beweis}).", extra)]
    return [("weiter", f"▶️ Die Sitzung arbeitet wieder ({beweis}).", extra)]


def pruefe_transkript(eingabe: dict, z: dict) -> list[tuple[str, str, dict]]:
    """
    API-Fehler (isApiErrorMessage) und eine gelungene Anmeldung (/login → „Login successful“) seit der eigenen Stelle im
    Transkript. Eigene Stelle („pruef_stelle“), nie die von neue_texte: Sonst fehlten dort Zwischentexte. Nur Zeilen mit
    passendem Merkmal werden als JSON gelesen; das Transkript trägt große Anfrage-Blöcke.
    """
    pfad = eingabe.get("transcript_path")
    if not pfad:
        return []
    try:
        with open(pfad, "rb") as f:
            groesse = os.fstat(f.fileno()).st_size
            # Sitzungen, die ein älterer Hook schon gelesen hat: ab dessen Stelle, alte Fehler gelten als gemeldet.
            stelle = z.get("pruef_stelle", z.get("stelle", 0))
            if not 0 <= stelle <= groesse:
                stelle = 0
            # Erster Lauf in einer langen Sitzung (älterer Hook vorher): nur das Ende, nie das ganze Transkript.
            if stelle == 0 and groesse > 2_000_000:
                stelle = groesse - 2_000_000
            f.seek(stelle)
            roh = f.read()
    except Exception:
        return []
    # Was beim letzten Mal nicht beim Relay ankam, zuerst (sonst bliebe eine Sperre für Jarvis unsichtbar).
    raus = [tuple(m) for m in z.pop("sperr_offen", []) if isinstance(m, list) and len(m) == 3]
    ende = roh.rfind(b"\n")
    if ende < 0:
        return raus
    z["pruef_stelle"] = stelle + ende + 1
    for zeile in roh[:ende].split(b"\n"):
        if b"isApiErrorMessage" not in zeile and b"Login successful" not in zeile:
            continue
        try:
            eintrag = json.loads(zeile)
        except Exception:
            continue
        if eintrag.get("type") == "assistant" and eintrag.get("isApiErrorMessage") is True:
            inhalt = (eintrag.get("message") or {}).get("content")
            text = inhalt if isinstance(inhalt, str) else " ".join(b.get("text", "") for b in inhalt or [] if isinstance(b, dict))
            raus += sperre_melden(z, str(eintrag.get("error") or "unknown"), text, _zeit(eintrag.get("timestamp")))
        elif eintrag.get("type") == "system" and eintrag.get("subtype") == "local_command" and \
                "<local-command-stdout>Login successful" in str(eintrag.get("content", "")):
            angemeldet = _zeit(eintrag.get("timestamp"))
            if (z.get("sperre") or {}).get("art") == "anmeldung" and angemeldet >= z["sperre"].get("seit", 0):
                raus += sperre_aufheben(z, "/login am Rechner", angemeldet)
    return raus


def nach_fehler_begonnen(z: dict) -> bool:
    """Begann dieser Hook nach dem Fehler, der die Sitzung gesperrt hat? Ein Werkzeug- oder Stop-Hook von vorher (async,
    spät dran) beweist nichts."""
    sperre = z.get("sperre") or {}
    return bool(sperre) and START > float(sperre.get("seit", 0)) + 1


def sende_sperre(meldungen: list[tuple[str, str, dict]]) -> None:
    """Sendet Sperr-Meldungen (zwei Versuche). Was nicht ankommt, merkt sich der Zustand; die nächste Prüfung (spätestens der
    Warter nach 25 s) schickt es erneut."""
    offen = []
    for stand, text, extra in meldungen:
        for versuch in range(2):
            try:
                if melde(stand, text, extra=extra) == 200:
                    break
            except Exception as fehler:
                schreib(f"Meldung {stand} Versuch {versuch + 1}: {fehler}")
            time.sleep(1)
        else:
            offen.append([stand, text, extra])
    if offen:
        try:
            with Sperre():
                z = lies_zustand()
                z["sperr_offen"] = (z.get("sperr_offen") or []) + offen
                schreib_zustand(z)
        except SperreBelegt as fehler:
            schreib(f"Sperr-Meldung verloren: {fehler}")


def fehlschlag(eingabe: dict) -> None:
    """StopFailure: Der Zug endete an einem API-Fehler (Feld „error“). Nur melden, Ausgabe wird ignoriert."""
    fehler = str(eingabe.get("error") or "unknown")
    text = str(eingabe.get("last_assistant_message") or eingabe.get("error_details") or "")
    with Sperre():
        z = lies_zustand()
        raus = pruefe_transkript(eingabe, z)
        if not raus:
            raus = sperre_melden(z, fehler, text, time.time())
        schreib_zustand(z)
    sende_sperre(raus)


# ---------------------------------------------------------------- Freie Rückfragen am Zugende

# Ein Aufzählungspunkt mit eindeutigem Trenner: „A)“, „A.“, „A:“, „(A)“, „**A**“, „**A)**“ oder „A — “; nie „Antworte …“.
_OPTION = re.compile(r"^\s*(?:[-*•]\s*)?(?:(?:\*\*|__)\(?(?P<a>[A-H]|[1-9])[).:]?(?:\*\*|__)|\(?(?P<b>[A-H]|[1-9])[).:]|(?P<c>[A-H]|[1-9])\s+[—–-])\s*(?:[—–:-]\s*)?(?P<text>\S.*?)\s*$")


def optionen_im_text(text: str) -> tuple[list[dict], bool]:
    """
    Die Auswahl, die im letzten Absatz einer freien Rückfrage wörtlich aufgezählt ist (A) …, **B** …, 1. …), nur wenn sie
    lückenlos bei A bzw. 1 beginnt und mindestens zwei Einträge hat; nichts wird erfunden. Liefert die Optionen und, ob
    nach der Aufzählung nur noch eine kurze Schlusszeile steht (dann ist die Nachricht eine Auswahlfrage, auch ohne „?“ am Ende).
    """
    zeilen = text.rstrip().splitlines()
    ende = len(zeilen)
    # Höchstens eine kurze Schlusszeile („Antworte mit A, B oder C.“) und Leerzeilen hinter der Aufzählung.
    while ende and not zeilen[ende - 1].strip():
        ende -= 1
    schluss = ende
    if ende and not _OPTION.match(zeilen[ende - 1]) and len(zeilen[ende - 1]) <= 200:
        schluss = ende - 1
        while schluss and not zeilen[schluss - 1].strip():
            schluss -= 1
    gefunden = []
    i = schluss
    while i and _OPTION.match(zeilen[i - 1]):
        m = _OPTION.match(zeilen[i - 1])
        gefunden.insert(0, (m.group("a") or m.group("b") or m.group("c"), m.group("text")))
        i -= 1
    if len(gefunden) < 2:
        return [], False
    start = "A" if gefunden[0][0].isalpha() else "1"
    if any(kennung != chr(ord(start) + nr) for nr, (kennung, _) in enumerate(gefunden)):
        return [], False
    # Die Frage steht unmittelbar davor (letzte nicht leere Zeile vor der Aufzählung endet mit „?“ oder „?:“) oder in der
    # kurzen Schlusszeile danach. Eine Liste in einem Bericht („Verbesserungsvorschläge:“) ist keine Auswahlfrage.
    vorher = next((z.strip() for z in reversed(zeilen[:i]) if z.strip()), "")
    nachher = "\n".join(zeilen[schluss:ende]).strip()
    if not re.search(r"\?\**\s*:?\s*$", vorher) and not nachher.endswith("?"):
        return [], False
    optionen = [{"label": text.replace("**", "").strip(), "kennung": kennung} for kennung, text in gefunden]
    return optionen, True


# ---------------------------------------------------------------- Nachträge

# So lange wartet ein Warter höchstens: zehn Minuten unter dem Hook-Zeitlimit (OpenLauncher und Notwege: 43200 s), damit er
# selbst endet und seine pid-Datei aufräumt, statt am Zeitlimit hart beendet zu werden.
WARTER_MAX_S = 12 * 3600 - 600
WARTER = ORDNER / f"warter-{AUFTRAG or 'ohne'}.pid"


def hole_nachtraege(warten: int) -> list[dict]:
    """Nachträge zu diesem Auftrag vom Relay; mit [warten] Long-Polling. Leer = keiner (oder Relay nicht erreichbar)."""
    code, daten = rufe(f"/pc/nachtrag?auftrag_id={urllib.parse.quote(AUFTRAG)}&warten={warten}", zeitlimit=warten + 10)
    if code == 410 and LOKAL:
        # In Jarvis entfernt: Es kommt nie wieder ein Nachtrag.
        merke_entfernt()
        return []
    if code != 200:
        # Beim Long-Polling nie ohne Pause neu fragen: 404 (Relay ohne Endpunkt, falsches Token) käme sofort zurück und liefe
        # sonst bis zu zwölf Stunden im Kreis gegen den Server.
        if warten and code != 204:
            time.sleep(3)
        return []
    return [n for n in daten.get("nachtraege") or [] if isinstance(n, dict) and n.get("nachtrag_id") and n.get("text")]


def beanspruche(liste: list[dict]) -> list[dict]:
    """
    Nimmt nur Nachträge, die noch kein anderer Hook dieser Sitzung übergeben hat (Stop und Warter laufen gleichzeitig).
    Schon übergebene, die der Relay noch anbietet (Quittung kam nicht an), werden still nachquittiert.
    """
    with Sperre():
        z = lies_zustand()
        neu = [n for n in liste if n["nachtrag_id"] not in z["nachtraege"]]
        alt = [n["nachtrag_id"] for n in liste if n["nachtrag_id"] in z["nachtraege"]]
        z["nachtraege"] = (z["nachtraege"] + [n["nachtrag_id"] for n in neu])[-200:]
        schreib_zustand(z)
    # Die Quittung darf die Übergabe nie verhindern: Die neuen sind schon als übergeben vermerkt und kämen sonst nie an.
    # Scheitert sie, bietet der Relay sie wieder an, und die nächste Abfrage quittiert sie still nach.
    if alt:
        try:
            melde("nachtrag", "", nachtrag_ids=alt)
        except Exception as fehler:
            schreib(f"Nachquittung: {fehler}")
    return neu


def uebergib(neu: list[dict]) -> str:
    """Quittiert die Übergabe (Relay und Jarvis) und liefert den Text, mit dem die Sitzung weiterarbeitet."""
    # Nachrichten aus dem Gespräch mit Jarvis (programmierer_sprechen, Nummer „G…“) tragen ihre Anleitung selbst: Frage beantworten
    # bzw. Wunsch bestätigen und umsetzen, danach weiterarbeiten. Sie sind kein Nachtrag mit Abschlusspflicht.
    gespraech = all(n["nachtrag_id"].startswith("G") for n in neu)
    kurz = " · ".join((n["text"].split("\n\n")[0] if n["nachtrag_id"].startswith("G") else n["text"]).replace("\n", " ")[:300] for n in neu)
    try:
        melde("nachtrag", "✓ Beim Programmierer angekommen." if gespraech else "📨 Nachtrag in der Sitzung angekommen: " + kurz,
              nachtrag_ids=[n["nachtrag_id"] for n in neu])
    except Exception as fehler:
        schreib(f"Quittung der Übergabe: {fehler}")
    if gespraech:
        teile = "\n\n".join(n["text"] for n in neu)
        return (f"Jarvis, Franks Sprachassistent, spricht über das Handy mit dir (Frank hört mit). Reagiere genau wie in der Nachricht "
                f"beschrieben und arbeite danach an deiner bisherigen Aufgabe weiter. AUFTRAG-ID {AUFTRAG}.\n\n{teile}")
    teile = "\n\n".join(f"NACHTRAG {n['nachtrag_id']}:\n{n['text']}" for n in neu)
    if LOKAL:
        # Antworten auf eine freie Rückfrage kommen als Nachtrag „antwort-<frage_id>“: in Jarvis überall beantwortet.
        for n in neu:
            if n["nachtrag_id"].startswith("antwort-"):
                frage_status(n["nachtrag_id"].removeprefix("antwort-"), "beantwortet", antwort=n["text"], versuche=2)
        return (f"Frank hat dir über Jarvis (Handy) {'eine Nachricht' if len(neu) == 1 else str(len(neu)) + ' Nachrichten'} zu dieser Sitzung "
                f"geschickt. Behandle sie wie eine Eingabe von ihm am Rechner und arbeite damit hier weiter.\n\n{teile}")
    return (f"Frank hat über Jarvis {'einen Nachtrag' if len(neu) == 1 else str(len(neu)) + ' Nachträge'} zu diesem Programmierauftrag "
            f"(AUFTRAG-ID {AUFTRAG}) geschickt. Er gehört zu genau diesem Auftrag und dieser Sitzung: Setze ihn hier um, als Teil des "
            f"Auftrags, und schließe danach wieder nach den Profilregeln ab (bauen, Version, Commit, Push, Installation) und melde "
            f"{'mit jarvis_hook.py melde fertig' if os.environ.get('JARVIS_SERVER') else 'stand=fertig mit programmier_fortschritt'}. Ist er unklar, frag mit {'dem Werkzeug question' if FREMDE_CLI else 'AskUserQuestion'}.\n\n{teile}")


def _windows_prozesse() -> dict[int, tuple[int, str]]:
    """pid -> (Eltern-pid, Programmname) aller Prozesse (nur Windows)."""
    import ctypes
    from ctypes import wintypes

    class Eintrag(ctypes.Structure):
        _fields_ = [("dwSize", wintypes.DWORD), ("cntUsage", wintypes.DWORD), ("th32ProcessID", wintypes.DWORD),
                    ("th32DefaultHeapID", ctypes.c_size_t), ("th32ModuleID", wintypes.DWORD), ("cntThreads", wintypes.DWORD),
                    ("th32ParentProcessID", wintypes.DWORD), ("pcPriClassBase", ctypes.c_long), ("dwFlags", wintypes.DWORD),
                    ("szExeFile", ctypes.c_wchar * 260)]

    k = ctypes.windll.kernel32
    k.CreateToolhelp32Snapshot.restype = wintypes.HANDLE
    schnappschuss = k.CreateToolhelp32Snapshot(2, 0)
    e = Eintrag()
    e.dwSize = ctypes.sizeof(Eintrag)
    alle = {}
    weiter = k.Process32FirstW(schnappschuss, ctypes.byref(e))
    while weiter:
        alle[e.th32ProcessID] = (e.th32ParentProcessID, e.szExeFile.lower())
        weiter = k.Process32NextW(schnappschuss, ctypes.byref(e))
    k.CloseHandle(schnappschuss)
    return alle


def _posix_eltern(pid: int) -> tuple[int, str]:
    """(Eltern-pid, Programmname) eines Prozesses über ps (macOS, Linux); (0, "") wenn er fehlt."""
    import subprocess
    try:
        aus = subprocess.run(["ps", "-o", "ppid=,comm=", "-p", str(pid)], capture_output=True, text=True, timeout=5).stdout.strip()
        eltern, _, name = aus.partition(" ")
        return int(eltern), os.path.basename(name.strip()).lower()
    except Exception:
        return 0, ""


def claude_prozess() -> int | None:
    """Der Claude-Code-Prozess, der diesen Hook gestartet hat (nächster Vorfahr claude*), oder None."""
    if os.name != "nt":
        # Ohne diesen Zweig merkte der Warter das Ende der Sitzung nicht und wartete bis zu zwölf Stunden weiter.
        pid = os.getpid()
        for _ in range(12):
            eltern, _ = _posix_eltern(pid)
            if eltern <= 1:
                return None
            if _posix_eltern(eltern)[1].startswith("claude"):
                return eltern
            pid = eltern
        return None
    try:
        alle = _windows_prozesse()
        pid = os.getpid()
        for _ in range(12):
            eltern = alle.get(pid, (0, ""))[0]
            if not eltern or eltern not in alle:
                return None
            if alle[eltern][1].startswith("claude"):
                return eltern
            pid = eltern
    except Exception as fehler:
        schreib(f"Claude-Prozess nicht gefunden: {fehler}")
    return None


def startzeit(pid: int) -> str | None:
    """Startzeit eines laufenden Prozesses als Text; None, wenn er nicht (mehr) läuft, "" wenn sie sich nicht ermitteln
    lässt. Zusammen mit der PID kennzeichnet sie den Prozess eindeutig, denn beide Systeme vergeben PIDs neu."""
    if os.name != "nt":
        import subprocess
        try:
            aus = subprocess.run(["ps", "-o", "lstart=", "-p", str(pid)], capture_output=True, text=True, timeout=5).stdout.strip()
        except Exception:
            return ""
        return aus or None
    import ctypes
    from ctypes import wintypes
    k = ctypes.windll.kernel32
    k.OpenProcess.restype = wintypes.HANDLE
    griff = k.OpenProcess(0x1000, False, pid)  # PROCESS_QUERY_LIMITED_INFORMATION
    if not griff:
        return None
    try:
        code = wintypes.DWORD()
        if not k.GetExitCodeProcess(wintypes.HANDLE(griff), ctypes.byref(code)) or code.value != 259:  # STILL_ACTIVE
            return None
        zeiten = [wintypes.FILETIME() for _ in range(4)]
        if not k.GetProcessTimes(wintypes.HANDLE(griff), *[ctypes.byref(z) for z in zeiten]):
            return ""
        return str((zeiten[0].dwHighDateTime << 32) | zeiten[0].dwLowDateTime)
    finally:
        k.CloseHandle(wintypes.HANDLE(griff))


def lebt(pid: int, start: str = "") -> bool:
    """Läuft der Prozess noch, und ist es noch derselbe ([start] wie von startzeit)? Ohne diesen Abgleich gälte ein
    fremder Prozess mit wiederverwendeter PID als alter Warter, und kein neuer Warter stellte Nachträge mehr zu."""
    jetzt = startzeit(pid)
    if jetzt is None:
        return False
    return not start or not jetzt or jetzt == start


def nachtraege_im_stop() -> bool:
    """Stop: Liegen Nachträge bereit, gehen sie als decision block in die Sitzung. True = übergeben."""
    try:
        neu = beanspruche(hole_nachtraege(0))
    except Exception as fehler:
        schreib(f"Nachträge im Stop: {fehler}")
        return False
    if not neu:
        return False
    sys.stdout.write(json.dumps({"decision": "block", "reason": uebergib(neu)}, ensure_ascii=False))
    return True


def warte(eingabe: dict) -> int:
    """asyncRewake: wartet auf Nachträge; Exit 2 mit dem Nachtrag weckt die Sitzung. Nur ein Warter je Auftrag."""
    ORDNER.mkdir(parents=True, exist_ok=True)
    # Die pid-Datei trägt PID und Startzeit („1234 133…“); ältere Fassungen schrieben nur die PID.
    try:
        teile = WARTER.read_text(encoding="utf-8").split()
        alter, alter_start = (int(teile[0]), teile[1] if len(teile) > 1 else "") if teile else (0, "")
    except Exception:
        alter, alter_start = 0, ""
    if alter and alter != os.getpid() and lebt(alter, alter_start):
        return 0
    WARTER.write_text(f"{os.getpid()} {startzeit(os.getpid()) or ''}".strip(), encoding="utf-8")
    # OpenCode: Das Plugin nennt seinen Prozess, einen claude*-Vorfahren gibt es dort nicht. Claude Code nennt sich selbst
    # (CLAUDE_PID, ab 2.1.x); der Vorfahr bleibt der Ersatz für ältere Fassungen.
    claude = int(os.environ.get("JARVIS_ELTERN_PID") or 0) or int(os.environ.get("CLAUDE_PID") or 0) or claude_prozess()
    claude_start = (startzeit(claude) or "") if claude else ""
    schreib(f"Warter gestartet ({eingabe.get('hook_event_name', '')}, Claude-Prozess {claude})")
    try:
        ende = time.monotonic() + WARTER_MAX_S
        while time.monotonic() < ende:
            if claude and not lebt(claude, claude_start):
                schreib("Warter: Claude-Sitzung beendet")
                return 0
            if LOKAL:
                if ist_entfernt():
                    schreib("Warter: in Jarvis entfernt")
                    return 0
                hook_marke()
                # Am Rechner mit Esc abgebrochen: Claude Code ruft dann keinen Stop-Hook, Jarvis zeigte sonst weiter „arbeitet“.
                pfad = str(eingabe.get("transcript_path") or "")
                if pfad and zugstand(pfad) == "abbruch":
                    try:
                        groesse = os.path.getsize(pfad)
                        with Sperre():
                            z = lies_zustand()
                            neu = z.get("abbruch_stelle") != groesse
                            z["abbruch_stelle"] = groesse
                            schreib_zustand(z)
                        if neu:
                            melde_still_leer("⏸️ Am PC abgebrochen, die Sitzung wartet auf die nächste Eingabe.")
                    except (OSError, SperreBelegt):
                        pass
            # Steht die Sitzung an einem Anmeldefehler, ist der Warter der einzige Hook, der noch läuft.
            try:
                with Sperre():
                    z = lies_zustand()
                    sperr = pruefe_transkript(eingabe, z)
                    schreib_zustand(z)
                sende_sperre(sperr)
            except SperreBelegt:
                pass
            try:
                liste = hole_nachtraege(25)
            except Exception as fehler:
                schreib(f"Warter: {fehler}")
                time.sleep(5)
                continue
            if not liste:
                continue
            try:
                neu = beanspruche(liste)
            except SperreBelegt as fehler:
                # Nicht übernommen: Der Relay bietet sie gleich wieder an.
                schreib(f"Warter: {fehler}")
                time.sleep(3)
                continue
            if not neu:
                # Schon übergeben (Stop war schneller), Quittung noch unterwegs: nicht im Kreis fragen.
                time.sleep(3)
                continue
            sys.stderr.write(uebergib(neu))
            schreib(f"Warter: {len(neu)} Nachtrag/Nachträge übergeben, Sitzung wird geweckt")
            return 2
        return 0
    finally:
        try:
            if WARTER.read_text(encoding="utf-8").split()[:1] == [str(os.getpid())]:
                WARTER.unlink()
        except Exception:
            pass


# ---------------------------------------------------------------- Lokale Sitzungen (siehe Kopf)

ZEICHEN = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"   # wie Auftragsbuch.neueNummer in der App
# So lange nach dem Stellen einer Rückfrage zählt eine Eingabe am Rechner noch nicht als „Frank antwortet am Rechner“.
FRAGE_SCHONFRIST_S = 15
FRAGE_HALTEN_LOKAL_S = 24 * 3600  # so lange ist eine freie Rückfrage einer lokalen Sitzung in Jarvis beantwortbar


def setze_auftrag(nummer: str) -> None:
    """Nummer der Sitzung und die Dateien, die daran hängen (Zustand, Sperre, Warter)."""
    global AUFTRAG, ZUSTAND, SPERRE, WARTER, FRAGE_WARTEN_S
    AUFTRAG = nummer
    FRAGE_WARTEN_S = frage_warten_s(nummer)
    ZUSTAND = ORDNER / f"hook-{nummer}.json"
    SPERRE = ORDNER / f"hook-{nummer}.lock"
    WARTER = ORDNER / f"warter-{nummer}.pid"


def lokale_nummer(sitzung: str) -> str:
    """Feste Nummer einer am Rechner gestarteten Sitzung aus ihrer session_id: Hook und jarvis_lokal.py kommen immer auf
    dieselbe, auch nach --resume. So entsteht in Jarvis nie ein zweiter Eintrag für dieselbe Sitzung."""
    zahl = int(hashlib.sha1(sitzung.encode("utf-8")).hexdigest(), 16)
    return "".join(ZEICHEN[(zahl >> (5 * i)) & 31] for i in range(5))


def _beruehre(datei: Path) -> None:
    try:
        ORDNER.mkdir(parents=True, exist_ok=True)
        datei.touch()
    except OSError:
        pass


def ist_entfernt() -> bool:
    return (ORDNER / f"entfernt-{AUFTRAG}").exists()


def merke_entfernt() -> None:
    if not ist_entfernt():
        schreib("In Jarvis entfernt: keine Meldungen mehr")
    _beruehre(ORDNER / f"entfernt-{AUFTRAG}")


def hook_marke() -> None:
    """Diese Sitzung hat die Hooks: jarvis_lokal.py liest sie dann nicht zusätzlich mit (sonst käme alles doppelt)."""
    _beruehre(ORDNER / f"lokal-{AUFTRAG}.hook")


def pc_ruhe_s() -> float:
    """Sekunden seit der letzten Eingabe an Tastatur oder Maus dieses Rechners. Unbekannt = 0: Dann gilt Frank als am
    Rechner, und eine Frage erscheint wie gewohnt dort."""
    # Auf dem Hostinger-Server sitzt nie jemand am Rechner: Die Frage gehört aufs Handy.
    if os.environ.get("JARVIS_SERVER"):
        return 86400.0
    try:
        if os.name == "nt":
            import ctypes

            class Eingabe(ctypes.Structure):
                _fields_ = [("cbSize", ctypes.c_uint), ("dwTime", ctypes.c_uint)]

            info = Eingabe()
            info.cbSize = ctypes.sizeof(Eingabe)
            if not ctypes.windll.user32.GetLastInputInfo(ctypes.byref(info)):
                return 0
            return ((ctypes.windll.kernel32.GetTickCount() - info.dwTime) & 0xFFFFFFFF) / 1000
        if sys.platform == "darwin":
            import subprocess
            aus = subprocess.run(["ioreg", "-c", "IOHIDSystem", "-d", "4"], capture_output=True, text=True, timeout=5).stdout
            m = re.search(r'"HIDIdleTime"\s*=\s*(\d+)', aus)
            return int(m.group(1)) / 1e9 if m else 0
    except Exception:
        pass
    return 0


def zugstand(pfad: str) -> str:
    """Wie der letzte Zug im Transkript endete: „ende“ (fertige Antwort), „abbruch“ (am Rechner mit Esc abgebrochen oder
    an einem API-Fehler stehen geblieben; dann kommt kein Stop-Hook) oder „“ (arbeitet noch bzw. unbekannt)."""
    try:
        with open(pfad, "rb") as f:
            groesse = os.fstat(f.fileno()).st_size
            f.seek(max(0, groesse - 400_000))
            zeilen = f.read().split(b"\n")
    except OSError:
        return ""
    for zeile in reversed(zeilen):
        if b'"type":"user"' not in zeile and b'"type":"assistant"' not in zeile:
            continue
        try:
            eintrag = json.loads(zeile)
        except Exception:
            continue
        if eintrag.get("isSidechain") or eintrag.get("type") not in ("user", "assistant"):
            continue
        nachricht = eintrag.get("message") or {}
        if eintrag.get("type") == "assistant":
            if eintrag.get("isApiErrorMessage"):
                return "abbruch"
            return "ende" if nachricht.get("stop_reason") == "end_turn" else ""
        return "abbruch" if "[Request interrupted" in json.dumps(nachricht.get("content"), ensure_ascii=False) else ""
    return ""


def lokal_angaben(sitzung: str, ordner: str, mit_hook: bool, name: str = "", link: str = "") -> dict:
    """Was Jarvis zum Anlegen und Anzeigen des Eintrags braucht. Name und Claude-App-Link stehen im Sitzungsregister von
    Claude Code (<Profil>/sessions/<pid>.json); ohne Register gelten Ordnername und kein Link."""
    if not name or not link:
        try:
            pid = os.environ.get("CLAUDE_PID", "")
            basis = Path(os.environ.get("CLAUDE_CONFIG_DIR") or Path.home() / ".claude")
            eintrag = json.loads((basis / "sessions" / f"{pid}.json").read_text(encoding="utf-8")) if pid else {}
            name = name or str(eintrag.get("name") or "")
            link = link or str(eintrag.get("bridgeSessionId") or "")
        except Exception:
            pass
        link = link or os.environ.get("CLAUDE_CODE_BRIDGE_SESSION_ID", "")
    return {"sitzung": sitzung, "name": (name or Path(ordner or ".").name or "Sitzung")[:60], "ordner": ordner[:200],
            "rechner": rechner(), "link": f"https://claude.ai/code/{link}" if link.startswith("session_") else "", "hook": mit_hook}


def registriere(transkript: str) -> bool:
    """Erste Meldung dieser Sitzung an Jarvis? Dann ab dem jetzigen Ende des Transkripts lesen: Eine lange (fortgesetzte)
    Sitzung schickt ihren ganzen Verlauf sonst noch einmal. True = neu, die Anmeldung ist zu senden."""
    with Sperre():
        z = lies_zustand()
        if z.get("lokal_seit"):
            return False
        try:
            groesse = os.path.getsize(transkript) if transkript else 0
        except OSError:
            groesse = 0
        z.update({"lokal_seit": time.time(), "beginn": time.time(), "stelle": groesse, "pruef_stelle": groesse})
        schreib_zustand(z)
        return True


def merke_freie_frage(frage_id: str) -> None:
    """Offene freie Rückfrage einer lokalen Sitzung: Tippt Frank am Rechner weiter, gilt sie dort als beantwortet (prompt)."""
    try:
        with Sperre():
            z = lies_zustand()
            z["frei_offen"] = (z.get("frei_offen") or [])[-9:] + [frage_id]
            schreib_zustand(z)
    except SperreBelegt as fehler:
        schreib(f"merke_freie_frage: {fehler}")


def prompt(eingabe: dict) -> None:
    """UserPromptSubmit (lokal, async): Was Frank am Rechner eingibt, steht auch in Jarvis; offene freie Rückfragen sind damit beantwortet."""
    text = lesbar(str(eingabe.get("prompt") or ""))
    if not text:
        return
    with Sperre():
        z = lies_zustand()
        offen = z.pop("frei_offen", [])
        erster = not z.get("auftrag_gemeldet")
        z["auftrag_gemeldet"] = True
        schreib_zustand(z)
    # Die erste Eingabe ist der Auftrag der Sitzung (Jarvis zeigt ihn über Rückfragen, wie bei eigenen Aufträgen).
    if erster:
        LOKAL_INFO["auftrag"] = text[:1500]
    for frage_id in offen:
        frage_status(frage_id, "beantwortet_am_pc", antwort=text, versuche=2)
    melde_still("laeuft", "👤 Frank am PC: " + kuerze(text, 1500))


def ende(_: dict) -> None:
    """SessionEnd (lokal): Die Sitzung wurde am Rechner geschlossen. Sie bleibt dort fortsetzbar (claude --resume)."""
    melde_still_leer("⏹️ Am PC beendet. Fortsetzen geht dort mit claude --resume; Jarvis begleitet sie dann wieder.")


def melde_still_leer(schritt: str) -> None:
    """Eine Meldung, nach der die Sitzung nicht arbeitet (Anmeldung, Ende): Jarvis zeigt dann keine drehende Anzeige."""
    try:
        melde("laeuft", schritt, extra={"leerlauf": True})
    except Exception as fehler:
        schreib(f"Meldung nicht gesendet: {fehler}")


def lokal(art: str) -> int:
    """Einstieg der Profil-Hooks (siehe Kopf): nur in interaktiven, nicht von Jarvis gestarteten Claude-Code-Sitzungen."""
    global LOKAL
    try:
        eingabe = json.loads(sys.stdin.buffer.read().decode("utf-8") or "{}")
    except Exception:
        eingabe = {}
    sitzung = str(eingabe.get("session_id") or "")
    if AUFTRAG:
        # Jarvis-Sitzung: Ihre eigenen Hooks melden schon. Nur vermerken, damit jarvis_lokal.py sie nie als lokal zeigt.
        if sitzung and art == "warte":
            _beruehre(ORDNER / f"jarvis-{sitzung}.sitzung")
        return 0
    if FREMDE_CLI or not sitzung or os.environ.get("CLAUDE_CODE_ENTRYPOINT", "cli") != "cli":
        return 0
    if not (Path.home() / "SK" / "Jarvis" / "relay.properties").exists():
        return 0
    setze_auftrag(lokale_nummer(sitzung))
    LOKAL = True
    if ist_entfernt():
        return 0
    hook_marke()
    LOKAL_INFO.update(lokal_angaben(sitzung, str(eingabe.get("cwd") or os.getcwd()), True))
    try:
        if registriere(str(eingabe.get("transcript_path") or "")):
            melde_still_leer(f"💻 Gestartet auf dem PC ({rechner()}): Jarvis begleitet diese Sitzung. Rückfragen beantwortest du hier, "
                             "Nachträge gehen direkt in die Sitzung. Löschen entfernt sie nur aus Jarvis, am PC läuft sie weiter.")
        return {"post": post, "frage": frage, "stop": stop, "puls": puls, "warte": warte, "fehlschlag": fehlschlag,
                "prompt": prompt, "ende": ende}.get(art, lambda _: None)(eingabe) or 0
    except Exception as fehler:
        schreib(f"lokal {art}: {type(fehler).__name__}: {fehler}")
        return 0


# ---------------------------------------------------------------- Ereignisse

def post(eingabe: dict) -> None:
    werkzeug = eingabe.get("tool_name", "")
    ein = eingabe.get("tool_input") or {}
    aus = eingabe.get("tool_response")
    erledigt = []
    with Sperre():
        z = lies_zustand()
        sperr = pruefe_transkript(eingabe, z)
        if nach_fehler_begonnen(z):
            sperr += sperre_aufheben(z, f"Werkzeug {werkzeug} lief")
        # Zuerst, was die KI seit dem letzten Werkzeug geschrieben hat: sofort, jeder Text als eigene Meldung.
        raus = [("laeuft", "💬 " + text) for text in neue_texte(eingabe, z)]
        # Eine Rückfrage, die am Rechner (Terminal, Claude-App) beantwortet wurde: AskUserQuestion ist gelaufen. Läuft danach
        # ein anderes Werkzeug, ist jede noch am Rechner offene Frage ebenfalls erledigt (abgebrochen oder anders beantwortet).
        if werkzeug == "AskUserQuestion":
            erledigt = [(v["frage_id"], "beantwortet_am_pc", antwort_text(aus)) for _, v in offene_am_pc(z, str(eingabe.get("tool_use_id") or ""))]
        else:
            erledigt = [(v["frage_id"], v.get("ausgang") or "erledigt", v.get("text", "")) for _, v in offene_am_pc(z)]
        if werkzeug in ("Edit", "Write", "MultiEdit", "NotebookEdit"):
            pfad = ein.get("file_path") or ein.get("notebook_path") or ""
            if pfad:
                z["dateien"].append(Path(pfad).name)
            raus += sammel(z, None)
        elif werkzeug in ("Bash", "PowerShell"):
            befehl = str(ein.get("command", ""))
            text = json.dumps(aus, ensure_ascii=False) if not isinstance(aus, str) else aus
            # Ein Exit-Code ungleich 0 kommt nur als PostToolUseFailure an; dazu Fehler, die mit Exit 0 enden.
            fehlgeschlagen = eingabe.get("hook_event_name") == "PostToolUseFailure" or \
                bool(re.search(r"BUILD FAILED|INSTALL_FAILED|! \[rejected\]|APK_UPDATE_STATUS=fehler|JARVIS_INSTALL_STATUS=fehler", text)) or \
                (isinstance(aus, dict) and bool(aus.get("interrupted")))
            if re.search(r"gradlew(\.bat)?\b.*\b(assemble|install|build|bundle)", befehl):
                raus += sammel(z, "🔴 Bau fehlgeschlagen" if fehlgeschlagen else "🔨 Bau erfolgreich", sofort=True)
            elif re.search(r"\bgit\b.*\bpush\b", befehl):
                raus += sammel(z, "🔴 Push fehlgeschlagen" if fehlgeschlagen else "⬆️ Gepusht", sofort=True)
            elif re.search(r"\bgit\b.*\bcommit\b", befehl):
                raus += sammel(z, "🔴 Commit fehlgeschlagen" if fehlgeschlagen else "📦 Committet", sofort=True)
            elif "jarvis-install" in befehl:
                # Das Skript wählt das Gerät selbst und nennt es erst in seiner Rückmeldung.
                m = re.search(r"JARVIS_INSTALL_STATUS=installiert \(([^)]+)\)", text)
                weg = installweg(f"adb -s {m.group(1)}") if m else ""
                raus += sammel(z, "🔴 Installation fehlgeschlagen" if fehlgeschlagen else "📲 Auf dem Handy installiert" + weg, sofort=True)
            elif re.search(r"\badb\b.*\binstall\b", befehl):
                raus += sammel(z, "🔴 Installation fehlgeschlagen" if fehlgeschlagen else "📲 Auf dem Handy installiert" + installweg(befehl), sofort=True)
            elif re.search(r"apk-update", befehl):
                raus += sammel(z, "🔴 APK-Update fehlgeschlagen" if fehlgeschlagen else "☁️ APK-Update für UpdateStation bereit", sofort=True)
            else:
                raus += sammel(z, None)
        else:
            raus += sammel(z, None)
        schreib_zustand(z)
    sende_sperre(sperr)
    sende(raus)
    # post läuft async mit 15 s Zeitlimit: höchstens zwei Versuche (je bis 5 s). Ging der Ausgang nicht durch, bleibt die Frage
    # vorgemerkt, und das nächste Werkzeug oder der Stop meldet ihn erneut.
    for frage_id, status, text in erledigt:
        if not frage_status(frage_id, status, antwort=text, versuche=2):
            merke_frage(f"nachmelden-{frage_id}", frage_id, "am_pc", ausgang=status, text=text)


def antwort_text(antwort) -> str:
    """Die Antworten aus dem Ergebnis von AskUserQuestion als lesbarer Text (Format laut Doku: Frage -> label, mehrere mit Komma)."""
    if isinstance(antwort, dict):
        werte = antwort.get("answers") if isinstance(antwort.get("answers"), dict) else None
        if werte:
            return "; ".join(f"{v}" for v in werte.values() if str(v).strip())
        return "; ".join(str(v) for k, v in antwort.items() if isinstance(v, str) and v.strip() and k not in ("questions",))[:2000]
    if isinstance(antwort, list):
        return " ".join(antwort_text(t) if not isinstance(t, dict) or t.get("type") != "text" else str(t.get("text", "")) for t in antwort)[:2000]
    return str(antwort or "")[:2000]


def merke_frage(tool_use_id: str, frage_id: str, status: str, ausgang: str = "", text: str = "") -> None:
    """Welche frage_id zu welchem AskUserQuestion-Aufruf gehört und wo sie steht (jarvis, am_pc): post meldet dann den Ausgang."""
    try:
        with Sperre():
            z = lies_zustand()
            fragen = z.setdefault("fragen", {})
            fragen[tool_use_id or frage_id] = {"frage_id": frage_id, "status": status, "ausgang": ausgang, "text": text}
            z["fragen"] = dict(list(fragen.items())[-20:])
            schreib_zustand(z)
    except SperreBelegt as fehler:
        schreib(f"merke_frage: {fehler}")


def offene_am_pc(z: dict, tool_use_id: str | None = None) -> list[tuple[str, dict]]:
    """Fragen, die am Rechner offen sind; mit [tool_use_id] nur diese (auch eine, die noch in Jarvis wartete). Nimmt sie aus
    dem Zustand (einmal melden). Ohne [tool_use_id] nie eine, auf die der Frage-Hook gerade noch wartet."""
    fragen = z.get("fragen") or {}
    erlaubt = ("am_pc", "jarvis") if tool_use_id else ("am_pc",)
    treffer = [(k, v) for k, v in fragen.items() if v.get("status") in erlaubt and (tool_use_id is None or k == tool_use_id)]
    for k, _ in treffer:
        fragen.pop(k, None)
    return treffer


def ohne_antwort(ein: dict, fragen: list, grund: str) -> dict:
    """
    Antwort an Franks Stelle, wenn keine kam: Die Sitzung darf nie an einem offenen Dialog stehen bleiben (Vorgabe vom
    23.10.2026). Eine Review-Frage gilt als „Nein“, jede andere bekommt die Anweisung, selbst zu entscheiden und weiterzuarbeiten.
    Als updatedInput, damit es auch das OpenCode-Plugin übernimmt (das wertet nur answers aus).
    """
    antworten = {}
    for f in fragen:
        frage_text = str(f.get("question", ""))
        labels = [str(o.get("label", "")) for o in f.get("options") or [] if isinstance(o, dict)]
        # Werkzeugbau: Eine Freigabe (Kosten, Zugriff, Konto, Löschen, Installation) wird nie durch Schweigen erteilt.
        if AUFTRAG.startswith("W0") and (str(f.get("header", "")).lower().startswith("freigabe") or frage_text.upper().startswith("FREIGABE")):
            antworten[frage_text] = (f"Keine Freigabe von Frank ({grund}). Führe den betroffenen Schritt NICHT aus. Melde ihn als blockiert, "
                                     "nenne den kleinsten nötigen nächsten Schritt und erledige die übrigen sicheren Teile.")
            continue
        nein = next((l for l in labels if l.strip().lower().startswith("nein")), "")
        # Nur das Angebot eines Code-Reviews gilt als Nein, nie etwa „Soll ich die Review-Befunde beheben?“.
        if re.search(r"(?i)code[- ]?review", frage_text) and not re.search(r"(?i)befund|beheb|fix", frage_text):
            antworten[frage_text] = nein or "Nein"
        else:
            empfohlen = next((l for l in labels if re.search(r"(?i)recommended|empfohlen", l)), "")
            antworten[frage_text] = (f"Keine Antwort von Frank ({grund}). Halte fest, dass er nicht geantwortet hat. Nicht erneut fragen "
                                     "und nicht warten: Überlege selbst die bestmögliche Lösung"
                                     + (f" (empfohlen war „{empfohlen}“)" if empfohlen else "")
                                     + ", baue sie ein, arbeite weiter und nenne die Entscheidung in der Abschlussmeldung.")
    return {"hookSpecificOutput": {
        "hookEventName": "PreToolUse",
        "permissionDecision": "allow",
        "permissionDecisionReason": grund + ".",
        "updatedInput": {**ein, "answers": antworten},
    }}


def weiter_ohne_antwort(grund: str) -> str:
    """Anweisung an die Sitzung, wenn eine freie Rückfrage am Zugende ohne Antwort blieb (Stop-Hook, decision block)."""
    return (f"{grund}: Frank hat auf deine Frage nicht geantwortet. Halte das fest und frag nicht erneut. Geht es um das Angebot "
            "eines Code-Reviews, gilt Nein. Ist es nur ein Angebot oder Vorschlag für Zusatzarbeit, setze nichts davon um und beende "
            "den Zug mit einem Satz. Sonst überlege selbst die bestmögliche Lösung, baue sie ein, arbeite weiter und nenne die "
            "Entscheidung in der Abschlussmeldung.")


def frage(eingabe: dict) -> None:
    ein = eingabe.get("tool_input") or {}
    fragen = ein.get("questions") or []
    if not fragen:
        return
    raus = []
    sperr = []
    try:
        with Sperre():
            z = lies_zustand()
            sperr = pruefe_transkript(eingabe, z)
            if nach_fehler_begonnen(z):
                sperr += sperre_aufheben(z, "Rückfrage gestellt")
            raus = [("laeuft", "💬 " + text) for text in neue_texte(eingabe, z)]
            raus += sammel(z, None, sofort=True)
            schreib_zustand(z)
    except SperreBelegt as fehler:
        # Die Frage selbst ist wichtiger als die Zwischentexte: Die holt der nächste Hook nach.
        schreib(f"frage: {fehler}")
    sende_sperre(sperr)
    sende(raus)
    tool_use_id = str(eingabe.get("tool_use_id") or "")
    frage_id = uuid.uuid4().hex[:12]
    kurz = " / ".join(str(f.get("question", "")) for f in fragen)
    # Mit Frist (Sekunden seit 1970): Jarvis zeigt, wie lange die Frage dort noch beantwortbar ist.
    daten = {"frage_id": frage_id, "fragen": fragen, "bis": int(time.time() + FRAGE_WARTEN_S), "tool_use_id": tool_use_id}
    # Franks Vorgabe vom 24.10.2026: Auch eine am Rechner gestartete Sitzung schickt ihre Rückfrage immer als Ja/Nein-Fenster an
    # Jarvis, wie ein Jarvis-Auftrag (früher nur eine stille Zeile „Rückfrage am PC“, sobald er in den letzten fünf Minuten am
    # Rechner etwas getippt hatte). Der Dialog am Rechner kommt, sobald er dort nach einer kurzen Schonfrist Tastatur oder Maus
    # berührt, nach Ablauf der Frist oder mit „In der Claude-App beantworten“.
    gestellt = time.monotonic()

    def am_rechner() -> bool:
        # Die Schonfrist fängt Eingaben ab, die beim Erscheinen der Frage noch liefen (Diktat, Tippen in einem anderen Fenster).
        frei = time.monotonic() - gestellt - FRAGE_SCHONFRIST_S
        return frei > 0 and pc_ruhe_s() < min(10, frei)

    if not melde_sicher("frage", "❓ " + kurz, daten) or (LOKAL and ist_entfernt()):
        if LOKAL:
            # Eine am Rechner gestartete Sitzung entscheidet nie an Franks Stelle: Der Dialog erscheint dort.
            schreib("Rückfrage nicht an Jarvis zugestellt: Dialog am Rechner")
            return
        # Ohne Jarvis wartete der Dialog am Rechner ohne Ende, und die Sitzung stünde (Franks Vorgabe vom 23.10.2026: nie).
        schreib("Rückfrage nicht an Jarvis zugestellt: Sitzung entscheidet selbst")
        sys.stdout.write(json.dumps(ohne_antwort(ein, fragen, "Jarvis war nicht erreichbar")))
        return
    merke_frage(tool_use_id, frage_id, "jarvis")
    antwort = warte_auf_antwort(frage_id, lambda: melde("frage", "❓ " + kurz, daten), am_pc=am_rechner if LOKAL else None)
    if antwort and antwort.get("geschlossen"):
        antwort = None
        grund = "Die Rückfrage wurde in Jarvis ohne Antwort geschlossen"
    else:
        grund = f"Frank hat in {FRAGE_WARTEN_S // 60} Minuten nicht geantwortet"
    if LOKAL and not antwort:
        # Lokal nie selbst entscheiden: abgelaufen oder in Jarvis geschlossen, dann steht der Dialog am Rechner.
        antwort = {"freigeben": True, "abgelaufen": True}
    if antwort and antwort.get("freigeben"):
        # Frank hat in Jarvis „In der Claude-App beantworten“ getippt (oder sitzt wieder am Rechner): der normale Dialog erscheint.
        warum = "am_rechner" if antwort.get("am_pc") else "abgelaufen" if antwort.get("abgelaufen") else "freigegeben"
        schreib(f"Keine Antwort über Jarvis ({warum}): normaler Dialog")
        merke_frage(tool_use_id, frage_id, "am_pc")
        frage_status(frage_id, "am_pc", grund=warum)
        return
    if not antwort:
        # Abgelaufen: Früher öffnete jetzt der Dialog am Rechner und die Sitzung stand, bis jemand dort antwortete
        # (Vorfälle 22./23.10.2026). Jetzt gilt: kein Review, sonst selbst entscheiden und weiterarbeiten.
        schreib("Keine Antwort über Jarvis (abgelaufen): Sitzung entscheidet selbst und arbeitet weiter")
        ausgabe = ohne_antwort(ein, fragen, grund)
        text = "; ".join(ausgabe["hookSpecificOutput"]["updatedInput"]["answers"].values())
        # Zuerst die Antwort: Ist der Relay weg (ein Grund für das Ausbleiben), darf keine Meldung sie verhindern.
        sys.stdout.write(json.dumps(ausgabe))
        sys.stdout.flush()
        merke_frage(tool_use_id, frage_id, "beantwortet")
        frage_status(frage_id, "erledigt", antwort=text, grund="abgelaufen")
        melde_still("laeuft", f"⏱️ Keine Antwort in {FRAGE_WARTEN_S // 60} Minuten, die Sitzung arbeitet weiter: " + text)
        return
    antworten = {str(k): str(v) for k, v in (antwort.get("antworten") or {}).items()}
    zusatz = str(antwort.get("text", "")).strip()
    if zusatz and not antworten:
        # Nur Freitext (Sprache): jede Frage bekommt ihn als „Sonstiges“, Claude ordnet ihn selbst ein.
        antworten = {str(f.get("question", "")): zusatz for f in fragen}
    text = "; ".join(f"{v}" for v in antworten.values())
    # Angekommen: Jarvis zeigt die Frage überall als beantwortet (auch auf einem zweiten Weg, etwa per ChatGPT).
    ausgabe = {"hookSpecificOutput": {
        "hookEventName": "PreToolUse",
        "permissionDecision": "allow",
        "permissionDecisionReason": "Frank hat über Jarvis geantwortet.",
        "updatedInput": {**ein, "answers": antworten},
    }}
    # Zuerst die Antwort: Ein Netzfehler beim Melden ließ sie sonst verloren gehen, und der Dialog stünde offen.
    sys.stdout.write(json.dumps(ausgabe))
    sys.stdout.flush()
    merke_frage(tool_use_id, frage_id, "beantwortet")
    frage_status(frage_id, "beantwortet", antwort=text)
    melde_still("laeuft", ("🤖 Entscheidung von Jarvis: " if re.search(r"\((automatisch von Jarvis|von Jarvis entschieden)", text) else "💬 Antwort von Frank: ") + text)


def stop(eingabe: dict) -> None:
    letzte = lesbar(str(eingabe.get("last_assistant_message") or ""))
    raus = []
    erledigt = []
    sperr = []
    try:
        with Sperre():
            z = lies_zustand()
            # Stop kommt nur nach einem gelungenen Zug (sonst StopFailure): Beweis, dass die Anmeldung wieder gilt.
            sperr = pruefe_transkript(eingabe, z)
            if nach_fehler_begonnen(z):
                sperr += sperre_aufheben(z, "Zug beendet")
            erledigt = [(v["frage_id"], v.get("ausgang") or "erledigt", v.get("text", "")) for _, v in offene_am_pc(z)]
            # Zwischentexte dieses Zuges, die noch kein Werkzeug-Hook gemeldet hat. Die letzte Nachricht steht oft schon im
            # Transkript und wird dabei als gesehen markiert: Sie kommt trotzdem gleich gesondert (Fehler vom 15.10.2026).
            texte = neue_texte(eingabe, z)
            raus = [("laeuft", "💬 " + text) for text in texte if text != letzte]
            raus += sammel(z, None, sofort=True)
            neu = bool(letzte) and (letzte in texte or not schon_gesehen(z, letzte))
            dauer = dauer_text(time.time() - z["beginn"])
            schreib_zustand(z)
    except SperreBelegt as fehler:
        # Der Abschluss darf nie ausfallen: nur lesen, nichts vermerken (die Zwischentexte holt der nächste Hook nach).
        schreib(f"stop: {fehler}")
        z = lies_zustand()
        neu = bool(letzte) and not schon_gesehen(dict(z, gesehen=list(z["gesehen"])), letzte)
        dauer = dauer_text(time.time() - z["beginn"])
    sende_sperre(sperr)
    sende(raus)
    for frage_id, ausgang, text in erledigt:
        frage_status(frage_id, ausgang, antwort=text)
    if not neu:
        # Lokal läuft Stop async: Eine Ausgabe nähme niemand an, Nachträge stellt dort nur der Warter zu.
        if not LOKAL:
            nachtraege_im_stop()
        return
    # Eine echte Rückfrage steht am Ende der Nachricht (Fragezeichen oder eine wörtlich aufgezählte Auswahl A/B/C … bzw.
    # 1/2/3 nach einer Frage, etwa Frage 1 des Research-Skills ohne AskUserQuestion); nur dann wartet der Hook (nie zweimal).
    ende = letzte.rstrip().rstrip("*_ )\"“”").strip()
    optionen, auswahl = optionen_im_text(letzte)
    # Am Rechner gestartet: nie warten, am Rechner kann Frank sofort tippen. Die Rückfrage geht trotzdem immer an Jarvis wie bei
    # einem Jarvis-Auftrag (Franks Vorgabe vom 24.10.2026); antwortet er dort, macht der Relay daraus einen Nachtrag
    # (als_nachtrag), und der Warter weckt die Sitzung damit.
    if LOKAL and (ende.endswith("?") or auswahl):
        frage_id = uuid.uuid4().hex[:12]
        daten = {"frage_id": frage_id, "frei": True, "als_nachtrag": True, "bis": int(time.time() + FRAGE_HALTEN_LOKAL_S),
                 "fragen": [{"question": letzte[-3000:], "options": optionen}]}
        schritt = "❓ " + (letzte if len(letzte) <= MAX_TEXT - 2 else "[… Anfang gekürzt] " + letzte[-(MAX_TEXT - 40):])
        if melde_sicher("frage", schritt, daten, versuche=2):
            merke_freie_frage(frage_id)
        return
    if (ende.endswith("?") or auswahl) and not eingabe.get("stop_hook_active") and not LOKAL:
        frage_id = uuid.uuid4().hex[:12]
        daten = {"frage_id": frage_id, "frei": True, "bis": int(time.time() + FRAGE_WARTEN_S),
                 "fragen": [{"question": letzte[-3000:], "options": optionen}]}
        # Bei sehr langer Nachricht zählt das Ende (dort steht die Frage); vorn sichtbar markiert statt still abgeschnitten.
        # Platz für Zeichen und Hinweis lassen, sonst kürzte melde() hinten, wo die Frage steht.
        schritt = "❓ " + (letzte if len(letzte) <= MAX_TEXT - 2 else "[… Anfang gekürzt] " + letzte[-(MAX_TEXT - 40):])
        if not melde_sicher("frage", schritt, daten):
            # Ohne Jarvis bliebe die Sitzung stehen, bis jemand am Rechner antwortet (Franks Vorgabe vom 23.10.2026: nie).
            schreib("Freie Rückfrage nicht an Jarvis zugestellt: Sitzung arbeitet weiter")
            sys.stdout.write(json.dumps({"decision": "block", "reason": weiter_ohne_antwort("Jarvis war nicht erreichbar")}, ensure_ascii=False))
            sys.stdout.flush()
            return
        antwort = warte_auf_antwort(frage_id, lambda: melde("frage", schritt, daten))
        if antwort and not antwort.get("freigeben") and not antwort.get("geschlossen"):
            inhalt = str(antwort.get("text", "")).strip() or "; ".join(str(v) for v in (antwort.get("antworten") or {}).values())
            # Zuerst die Antwort: Ein Netzfehler beim Melden ließ sie sonst verloren gehen.
            sys.stdout.write(json.dumps({"decision": "block", "reason": f"Frank hat über Jarvis geantwortet: {inhalt}"}, ensure_ascii=False))
            sys.stdout.flush()
            frage_status(frage_id, "beantwortet", antwort=inhalt)
            melde_still("laeuft", "💬 Antwort von Frank: " + inhalt)
            return
        if antwort and antwort.get("freigeben"):
            # Freigegeben: Frank antwortet am Rechner oder in der Claude-App wie auf jede Nachricht.
            frage_status(frage_id, "am_pc", grund="freigegeben")
            schreib("Freie Rückfrage ohne Antwort über Jarvis (freigegeben)")
            nachtraege_im_stop()
            return
        # Abgelaufen oder in Jarvis ohne Antwort geschlossen: nie stehen bleiben. Einmal weiterarbeiten lassen;
        # stop_hook_active verhindert beim nächsten Stop eine zweite Runde.
        grund = "Die Rückfrage wurde in Jarvis ohne Antwort geschlossen" if antwort else f"Frank hat in {FRAGE_WARTEN_S // 60} Minuten nicht geantwortet"
        schreib(f"Freie Rückfrage ohne Antwort über Jarvis ({grund}): Sitzung arbeitet weiter")
        sys.stdout.write(json.dumps({"decision": "block", "reason": weiter_ohne_antwort(grund)}, ensure_ascii=False))
        sys.stdout.flush()
        frage_status(frage_id, "erledigt", antwort="keine Antwort, Sitzung entscheidet selbst", grund="abgelaufen")
        melde_still("laeuft", "⏱️ Keine Antwort, die Sitzung entscheidet selbst und arbeitet weiter.")
        return
    # Zugende: die ganze letzte Nachricht, so wie sie im Terminal steht (am Schluss die Zusammenfassung), dazu die Dauer
    # seit dem Start des Auftrags (wie „Cooked for …“ im Terminal). Der Stand „info“ ist das Merkmal des ausführlichen
    # Abschlussberichts: Jarvis zeigt ihn mit voller Aktionsleiste, alle anderen Stände als kurze Statusmeldung.
    melde("info", "🏁 " + letzte + ("" if LOKAL else f"\n\n⏱️ Dauer seit Start: {dauer}"))
    # Danach (nicht davor: „info“ markiert in Jarvis die zuvor übergebenen Nachträge als bearbeitet) die neuen Nachträge.
    if not LOKAL:
        nachtraege_im_stop()


def puls(eingabe: dict) -> None:
    """
    PreToolUse (alle Werkzeuge, async), also VOR dem Werkzeug: neue Zwischentexte der KI sofort melden (nicht erst, wenn das
    Werkzeug fertig ist), Advisor, Skills und Agenten beim Start ankündigen, und ein Lebenszeichen für die drehende Anzeige
    in Jarvis (höchstens alle PULS_S).
    """
    werkzeug = str(eingabe.get("tool_name", ""))
    ein = eingabe.get("tool_input") or {}
    with Sperre():
        z = lies_zustand()
        sperr = pruefe_transkript(eingabe, z)
        if nach_fehler_begonnen(z):
            sperr += sperre_aufheben(z, f"Werkzeug {werkzeug} gestartet")
        raus = [("laeuft", "💬 " + text) for text in neue_texte(eingabe, z)]
        schreib_zustand(z)
    sende_sperre(sperr)
    if werkzeug == "advisor":
        raus.append(("laeuft", "🧠 Advisor wird befragt …"))
    elif werkzeug == "Skill":
        raus.append(("laeuft", f"🧩 Skill {ein.get('skill', '')} geladen".replace("  ", " ")))
    elif werkzeug in ("Agent", "Task"):
        raus.append(("laeuft", "🤖 Agent: " + str(ein.get("description") or ein.get("subagent_type") or "gestartet")))
    sende(raus)
    datei = ORDNER / f"puls-{AUFTRAG}.txt"
    try:
        if time.time() - datei.stat().st_mtime < PULS_S:
            return
    except FileNotFoundError:
        pass
    ORDNER.mkdir(parents=True, exist_ok=True)
    datei.write_text(str(time.time()), encoding="utf-8")
    melde("puls", str(eingabe.get("tool_name", "")))


def main() -> int:
    art = sys.argv[1] if len(sys.argv) > 1 else ""
    if art == "lokal":
        return lokal(sys.argv[2] if len(sys.argv) > 2 else "")
    if not AUFTRAG:
        return 0
    if art == "melde":
        # Von Hand aus einer Sitzung ohne Hooks (Codex CLI, Sol 6.1): python jarvis_hook.py melde <laeuft|fertig|fehler> "<Satz>".
        # Ohne stdin: Die Sitzung ruft das als normalen Befehl auf.
        stand = sys.argv[2] if len(sys.argv) > 2 else ""
        schritt = " ".join(sys.argv[3:]).strip()
        if stand not in ("laeuft", "fertig", "fehler") or not schritt:
            print('Aufruf: python jarvis_hook.py melde <laeuft|fertig|fehler> "<Satz>"')
            return 1
        # Ohne rechner: Eine Codex-Sitzung beweist nichts über die Claude-Anmeldung des Rechners (der Relay wertet sie dann nicht aus).
        ok = False
        for versuch in range(3):
            try:
                if melde(stand, schritt, extra={"rechner": ""}) == 200:
                    ok = True
                    break
            except Exception as fehler:
                schreib(f"melde {stand} Versuch {versuch + 1}: {fehler}")
            time.sleep(1 + 2 * versuch)
        print("Gemeldet." if ok else "Meldung an Jarvis fehlgeschlagen (siehe hook.log).")
        return 0 if ok else 1
    try:
        eingabe = json.loads(sys.stdin.buffer.read().decode("utf-8") or "{}")
    except Exception:
        eingabe = {}
    try:
        # Nur warte liefert einen Exit-Code (2 = Sitzung wecken); alle anderen enden mit 0.
        return {"post": post, "frage": frage, "stop": stop, "puls": puls, "warte": warte, "fehlschlag": fehlschlag}.get(art, lambda _: None)(eingabe) or 0
    except Exception as fehler:
        schreib(f"{art}: {type(fehler).__name__}: {fehler}")
        return 0


if __name__ == "__main__":
    sys.exit(main())
