"""Claude-Code-Hook für Programmieraufträge von Jarvis (nur in Sitzungen, die OpenLauncher --jarvis-auftrag gestartet hat).

Der OpenLauncher hängt diesen Hook nur an Jarvis-Sitzungen (über --settings) und setzt JARVIS_AUFTRAG_ID. Ohne die Variable
endet der Hook sofort ohne Ausgabe: In normalen Sitzungen passiert nie etwas.

Aufruf: python jarvis_hook.py <post|frage|stop>, die Hook-Eingabe kommt als JSON auf stdin.
  post   PostToolUse (alle Werkzeuge) und PostToolUseFailure (Bash|PowerShell, nur dort stehen fehlgeschlagene Befehle).
         Läuft async. Neue Zwischentexte der KI (aus dem Transkript) gehen sofort als eigene Meldung raus, dazu Advisor,
         Skills und Agenten; geänderte Dateien, Bau, Commit/Push und adb install werden gedrosselt:
         höchstens eine Meldung je DROSSEL_S, Fehler sofort. Lesen und Suchen meldet niemand (anderer Matcher).
  frage  PreToolUse (AskUserQuestion): schickt Fragen und Optionen an Jarvis, wartet bis FRAGE_WARTEN_S auf Franks Antwort
         und gibt sie an Claude zurück. Ohne Antwort gibt der Hook frei, und der normale Dialog erscheint (auch per Remote Control).
  puls   PreToolUse (alle Werkzeuge, async): Lebenszeichen für die drehende Anzeige im Jarvis-Chat, höchstens alle PULS_S.
  stop   Stop: meldet den Zwischenstand (letzte Nachricht). Endet sie mit einer Frage, wartet der Hook auf Franks Antwort und
         lässt Claude damit weiterarbeiten (decision block).

Wege: Relay https://<host>/pc/meldung und /pc/antwort mit dem Geräte-Token aus ~/SK/Jarvis/relay.properties, nur IPv4
(auf diesem Netz hängt IPv6 zeitweise, siehe bugs/desktop/dotnet-httpclient-ipv6-haenger.md).
Protokoll: %LOCALAPPDATA%/JarvisPcBruecke/hook.log
"""
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
DROSSEL_S = 20
FRAGE_WARTEN_S = 600
PULS_S = 10               # Lebenszeichen höchstens so oft
MAX_TEXT = 4000           # so lang darf eine Meldung werden (die Schlusszusammenfassung kommt vollständig)
ORDNER = Path(os.environ.get("LOCALAPPDATA", Path.home())) / "JarvisPcBruecke"
PROTOKOLL = ORDNER / "hook.log"
ZUSTAND = ORDNER / f"hook-{AUFTRAG or 'ohne'}.json"


def schreib(text: str) -> None:
    try:
        ORDNER.mkdir(parents=True, exist_ok=True)
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


def melde(stand: str, schritt: str, frage: dict | None = None) -> None:
    daten = {"auftrag_id": AUFTRAG, "stand": stand, "schritt": schritt[:MAX_TEXT]}
    if frage:
        daten["frage"] = frage
    code, _ = rufe("/pc/meldung", daten)
    schreib(f"Meldung {stand}: {schritt[:120]} -> {code}")


def warte_auf_antwort(frage_id: str) -> dict | None:
    frist = time.monotonic() + FRAGE_WARTEN_S
    while time.monotonic() < frist:
        try:
            code, daten = rufe("/pc/antwort?frage_id=" + urllib.parse.quote(frage_id), zeitlimit=35)
        except Exception as fehler:
            schreib(f"Antwort abholen: {fehler}")
            time.sleep(3)
            continue
        if code == 200:
            return daten
        if code == 404:
            return None
    return None


# ---------------------------------------------------------------- Zustand (Drosselung, gesehene Texte)

SPERRE = ORDNER / f"hook-{AUFTRAG or 'ohne'}.sperre"


class Sperre:
    """Async-Hooks laufen gleichzeitig: Zustand und Transkript-Stelle nur einer nach dem anderen."""

    def __enter__(self):
        ORDNER.mkdir(parents=True, exist_ok=True)
        for _ in range(100):
            try:
                os.close(os.open(SPERRE, os.O_CREAT | os.O_EXCL))
                return self
            except FileExistsError:
                try:
                    if time.time() - SPERRE.stat().st_mtime > 20:
                        SPERRE.unlink()
                except Exception:
                    pass
                time.sleep(0.1)
        return self

    def __exit__(self, *_):
        try:
            SPERRE.unlink()
        except Exception:
            pass


def lies_zustand() -> dict:
    try:
        z = json.loads(ZUSTAND.read_text(encoding="utf-8"))
    except Exception:
        z = {}
    for k, v in (("zuletzt", 0), ("dateien", []), ("offen", []), ("gesehen", []), ("stelle", 0)):
        z.setdefault(k, v)
    return z


def schreib_zustand(z: dict) -> None:
    try:
        ORDNER.mkdir(parents=True, exist_ok=True)
        ZUSTAND.write_text(json.dumps(z, ensure_ascii=False), encoding="utf-8")
    except Exception:
        pass


def sammel_und_sende(z: dict, neu: str | None, sofort: bool = False, stand: str = "laeuft") -> None:
    """Hängt [neu] an die offenen Punkte; gesendet wird, wenn DROSSEL_S um sind oder [sofort]."""
    if neu:
        z["offen"].append(neu)
    jetzt = time.time()
    if not (sofort or jetzt - z.get("zuletzt", 0) >= DROSSEL_S):
        return
    teile = []
    if z["dateien"]:
        namen = list(dict.fromkeys(z["dateien"]))
        teile.append("✏️ Geändert: " + ", ".join(namen[:6]) + (f" und {len(namen) - 6} weitere" if len(namen) > 6 else ""))
    teile += z["offen"]
    if not teile:
        return
    melde(stand, " · ".join(teile))
    z.update({"zuletzt": jetzt, "dateien": [], "offen": []})


def lesbar(text: str) -> str:
    """Markdown fürs Handy entschärfen: Fett- und Code-Zeichen weg, Zeilen bleiben."""
    return re.sub(r"\*\*|`", "", text).strip()


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
        return []
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
        if eintrag.get("type") != "assistant" or nachricht.get("role") != "assistant":
            continue
        inhalt = nachricht.get("content")
        bloecke = [inhalt] if isinstance(inhalt, str) else [b.get("text", "") for b in inhalt or [] if isinstance(b, dict) and b.get("type") == "text"]
        for text in bloecke:
            text = lesbar(text)
            if text and not schon_gesehen(z, text):
                texte.append(text)
    return texte


# ---------------------------------------------------------------- Ereignisse

def post(eingabe: dict) -> None:
    werkzeug = eingabe.get("tool_name", "")
    ein = eingabe.get("tool_input") or {}
    aus = eingabe.get("tool_response")
    with Sperre():
        z = lies_zustand()
        # Zuerst, was die KI seit dem letzten Werkzeug geschrieben hat: sofort, jeder Text als eigene Meldung.
        for text in neue_texte(eingabe, z):
            melde("laeuft", "💬 " + text)
        if werkzeug in ("Edit", "Write", "MultiEdit", "NotebookEdit"):
            pfad = ein.get("file_path") or ein.get("notebook_path") or ""
            if pfad:
                z["dateien"].append(Path(pfad).name)
            sammel_und_sende(z, None)
        elif werkzeug in ("Bash", "PowerShell"):
            befehl = str(ein.get("command", ""))
            text = json.dumps(aus, ensure_ascii=False) if not isinstance(aus, str) else aus
            # Ein Exit-Code ungleich 0 kommt nur als PostToolUseFailure an; dazu Fehler, die mit Exit 0 enden.
            fehlgeschlagen = eingabe.get("hook_event_name") == "PostToolUseFailure" or \
                bool(re.search(r"BUILD FAILED|INSTALL_FAILED|! \[rejected\]|APK_UPDATE_STATUS=fehler", text)) or \
                (isinstance(aus, dict) and bool(aus.get("interrupted")))
            if re.search(r"gradlew(\.bat)?\b.*\b(assemble|install|build|bundle)", befehl):
                sammel_und_sende(z, "🔴 Bau fehlgeschlagen" if fehlgeschlagen else "🔨 Bau erfolgreich", sofort=True)
            elif re.search(r"\bgit\b.*\bpush\b", befehl):
                sammel_und_sende(z, "🔴 Push fehlgeschlagen" if fehlgeschlagen else "⬆️ Gepusht", sofort=True)
            elif re.search(r"\bgit\b.*\bcommit\b", befehl):
                sammel_und_sende(z, "🔴 Commit fehlgeschlagen" if fehlgeschlagen else "📦 Committet", sofort=True)
            elif re.search(r"\badb\b.*\binstall\b", befehl):
                sammel_und_sende(z, "🔴 Installation fehlgeschlagen" if fehlgeschlagen else "📲 Auf dem Handy installiert", sofort=True)
            elif re.search(r"apk-update", befehl):
                sammel_und_sende(z, "🔴 APK-Update fehlgeschlagen" if fehlgeschlagen else "☁️ APK-Update für UpdateStation bereit", sofort=True)
            else:
                sammel_und_sende(z, None)
        elif werkzeug == "advisor":
            sammel_und_sende(z, "🧠 Advisor befragt", sofort=True)
        elif werkzeug == "Skill":
            sammel_und_sende(z, f"🧩 Skill {ein.get('skill', '')} geladen".replace("  ", " "), sofort=True)
        elif werkzeug in ("Agent", "Task"):
            sammel_und_sende(z, "🤖 Agent: " + str(ein.get("description") or ein.get("subagent_type") or "gestartet"), sofort=True)
        else:
            sammel_und_sende(z, None)
        schreib_zustand(z)


def frage(eingabe: dict) -> None:
    ein = eingabe.get("tool_input") or {}
    fragen = ein.get("questions") or []
    if not fragen:
        return
    with Sperre():
        z = lies_zustand()
        for text in neue_texte(eingabe, z):
            melde("laeuft", "💬 " + text)
        sammel_und_sende(z, None, sofort=True)
        schreib_zustand(z)
    frage_id = uuid.uuid4().hex[:12]
    kurz = " / ".join(str(f.get("question", "")) for f in fragen)
    melde("frage", "❓ " + kurz, {"frage_id": frage_id, "fragen": fragen})
    antwort = warte_auf_antwort(frage_id)
    if not antwort:
        schreib("Keine Antwort über Jarvis: normaler Dialog")
        return
    antworten = {str(k): str(v) for k, v in (antwort.get("antworten") or {}).items()}
    zusatz = str(antwort.get("text", "")).strip()
    if zusatz and not antworten:
        # Nur Freitext (Sprache): jede Frage bekommt ihn als „Sonstiges“, Claude ordnet ihn selbst ein.
        antworten = {str(f.get("question", "")): zusatz for f in fragen}
    melde("laeuft", "💬 Antwort von Frank: " + "; ".join(f"{v}" for v in antworten.values()))
    ausgabe = {"hookSpecificOutput": {
        "hookEventName": "PreToolUse",
        "permissionDecision": "allow",
        "permissionDecisionReason": "Frank hat über Jarvis geantwortet.",
        "updatedInput": {**ein, "answers": antworten},
    }}
    sys.stdout.write(json.dumps(ausgabe))


def stop(eingabe: dict) -> None:
    letzte = lesbar(str(eingabe.get("last_assistant_message") or ""))
    with Sperre():
        z = lies_zustand()
        # Zwischentexte dieses Zuges, die noch kein Werkzeug-Hook gemeldet hat (die letzte Nachricht kommt gleich gesondert).
        for text in neue_texte(eingabe, z):
            if text != letzte:
                melde("laeuft", "💬 " + text)
        sammel_und_sende(z, None, sofort=True)
        neu = bool(letzte) and not schon_gesehen(z, letzte)
        schreib_zustand(z)
    if not neu:
        return
    # Eine echte Rückfrage steht am Ende der Nachricht; nur dann wartet der Hook (und nie zweimal hintereinander).
    ende = letzte.rstrip().rstrip("*_ )\"“”").strip()
    if ende.endswith("?") and not eingabe.get("stop_hook_active"):
        frage_id = uuid.uuid4().hex[:12]
        melde("frage", "❓ " + letzte[-MAX_TEXT:], {"frage_id": frage_id, "frei": True, "fragen": [{"question": letzte[-1500:], "options": []}]})
        antwort = warte_auf_antwort(frage_id)
        if antwort:
            inhalt = str(antwort.get("text", "")).strip() or "; ".join(str(v) for v in (antwort.get("antworten") or {}).values())
            melde("laeuft", "💬 Antwort von Frank: " + inhalt)
            sys.stdout.write(json.dumps({"decision": "block", "reason": f"Frank hat über Jarvis geantwortet: {inhalt}"}))
            return
        schreib("Freie Rückfrage ohne Antwort über Jarvis")
        return
    # Zugende: die ganze letzte Nachricht, so wie sie im Terminal steht (am Schluss die Zusammenfassung).
    melde("info", "🏁 " + letzte)


def puls(eingabe: dict) -> None:
    """PreToolUse (alle Werkzeuge, async): Lebenszeichen für die drehende Anzeige in Jarvis, höchstens alle PULS_S."""
    datei = ORDNER / f"puls-{AUFTRAG}.txt"
    try:
        if time.time() - datei.stat().st_mtime < PULS_S:
            return
    except FileNotFoundError:
        pass
    ORDNER.mkdir(parents=True, exist_ok=True)
    datei.write_text(str(time.time()), encoding="utf-8")
    melde("puls", str(eingabe.get("tool_name", "")))


def main() -> None:
    if not AUFTRAG:
        return
    art = sys.argv[1] if len(sys.argv) > 1 else ""
    try:
        eingabe = json.loads(sys.stdin.buffer.read().decode("utf-8") or "{}")
    except Exception:
        eingabe = {}
    try:
        {"post": post, "frage": frage, "stop": stop, "puls": puls}.get(art, lambda _: None)(eingabe)
    except Exception as fehler:
        schreib(f"{art}: {type(fehler).__name__}: {fehler}")


if __name__ == "__main__":
    main()
    sys.exit(0)
