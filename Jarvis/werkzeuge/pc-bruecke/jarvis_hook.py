"""Claude-Code-Hook für Programmieraufträge von Jarvis (nur in Sitzungen, die OpenLauncher --jarvis-auftrag gestartet hat).

Der OpenLauncher hängt diesen Hook nur an Jarvis-Sitzungen (über --settings) und setzt JARVIS_AUFTRAG_ID. Ohne die Variable
endet der Hook sofort ohne Ausgabe: In normalen Sitzungen passiert nie etwas.

Aufruf: python jarvis_hook.py <post|frage|stop>, die Hook-Eingabe kommt als JSON auf stdin.
  post   PostToolUse (Edit|Write|MultiEdit|NotebookEdit|Bash|PowerShell) und PostToolUseFailure (Bash|PowerShell, nur dort
         stehen fehlgeschlagene Befehle): geänderte Dateien, Bau, Commit/Push, adb install. Läuft async. Gedrosselt:
         höchstens eine Meldung je DROSSEL_S, Fehler sofort. Lesen und Suchen meldet niemand (anderer Matcher).
  frage  PreToolUse (AskUserQuestion): schickt Fragen und Optionen an Jarvis, wartet bis FRAGE_WARTEN_S auf Franks Antwort
         und gibt sie an Claude zurück. Ohne Antwort gibt der Hook frei, und der normale Dialog erscheint (auch per Remote Control).
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
import uuid
from pathlib import Path

AUFTRAG = os.environ.get("JARVIS_AUFTRAG_ID", "").strip()
DROSSEL_S = 20
FRAGE_WARTEN_S = 600
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
    daten = {"auftrag_id": AUFTRAG, "stand": stand, "schritt": schritt[:600]}
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


# ---------------------------------------------------------------- Zustand (Drosselung)

def lies_zustand() -> dict:
    try:
        return json.loads(ZUSTAND.read_text(encoding="utf-8"))
    except Exception:
        return {"zuletzt": 0, "dateien": [], "offen": []}


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


# ---------------------------------------------------------------- Ereignisse

def post(eingabe: dict) -> None:
    werkzeug = eingabe.get("tool_name", "")
    ein = eingabe.get("tool_input") or {}
    aus = eingabe.get("tool_response")
    z = lies_zustand()
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
            if fehlgeschlagen:
                sammel_und_sende(z, "🔴 Bau fehlgeschlagen", sofort=True)
            else:
                sammel_und_sende(z, "🔨 Bau erfolgreich")
        elif re.search(r"\bgit\b.*\bpush\b", befehl):
            sammel_und_sende(z, "🔴 Push fehlgeschlagen" if fehlgeschlagen else "⬆️ Gepusht", sofort=fehlgeschlagen)
        elif re.search(r"\bgit\b.*\bcommit\b", befehl):
            sammel_und_sende(z, "🔴 Commit fehlgeschlagen" if fehlgeschlagen else "📦 Committet")
        elif re.search(r"\badb\b.*\binstall\b", befehl):
            sammel_und_sende(z, "🔴 Installation fehlgeschlagen" if fehlgeschlagen else "📲 Auf dem Handy installiert", sofort=True)
        elif re.search(r"apk-update", befehl):
            sammel_und_sende(z, "🔴 APK-Update fehlgeschlagen" if fehlgeschlagen else "☁️ APK-Update für UpdateStation bereit")
        else:
            sammel_und_sende(z, None)
    schreib_zustand(z)


def frage(eingabe: dict) -> None:
    ein = eingabe.get("tool_input") or {}
    fragen = ein.get("questions") or []
    if not fragen:
        return
    z = lies_zustand()
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


def letzte_nachricht(eingabe: dict) -> str:
    text = str(eingabe.get("last_assistant_message") or "").strip()
    if text:
        return text
    pfad = eingabe.get("transcript_path")
    if not pfad:
        return ""
    try:
        zeilen = Path(pfad).read_text(encoding="utf-8").splitlines()[-200:]
    except Exception:
        return ""
    for zeile in reversed(zeilen):
        try:
            eintrag = json.loads(zeile)
        except Exception:
            continue
        nachricht = eintrag.get("message") or {}
        if eintrag.get("type") != "assistant" or nachricht.get("role") != "assistant":
            continue
        inhalt = nachricht.get("content")
        if isinstance(inhalt, str) and inhalt.strip():
            return inhalt.strip()
        texte = [b.get("text", "") for b in inhalt or [] if isinstance(b, dict) and b.get("type") == "text"]
        if any(t.strip() for t in texte):
            return "\n".join(texte).strip()
    return ""


def stop(eingabe: dict) -> None:
    z = lies_zustand()
    sammel_und_sende(z, None, sofort=True)
    schreib_zustand(z)
    text = letzte_nachricht(eingabe)
    if not text:
        return
    # Eine echte Rückfrage steht am Ende der Nachricht; nur dann wartet der Hook (und nie zweimal hintereinander).
    ende = text.rstrip().rstrip("*_ )\"“”").strip()
    if ende.endswith("?") and not eingabe.get("stop_hook_active"):
        frage_id = uuid.uuid4().hex[:12]
        melde("frage", "❓ " + text[-600:], {"frage_id": frage_id, "frei": True, "fragen": [{"question": text[-1500:], "options": []}]})
        antwort = warte_auf_antwort(frage_id)
        if antwort:
            inhalt = str(antwort.get("text", "")).strip() or "; ".join(str(v) for v in (antwort.get("antworten") or {}).values())
            melde("laeuft", "💬 Antwort von Frank: " + inhalt)
            sys.stdout.write(json.dumps({"decision": "block", "reason": f"Frank hat über Jarvis geantwortet: {inhalt}"}))
            return
        schreib("Freie Rückfrage ohne Antwort über Jarvis")
        return
    melde("info", "⏸️ " + text[:600])


def main() -> None:
    if not AUFTRAG:
        return
    art = sys.argv[1] if len(sys.argv) > 1 else ""
    try:
        eingabe = json.loads(sys.stdin.buffer.read().decode("utf-8") or "{}")
    except Exception:
        eingabe = {}
    try:
        {"post": post, "frage": frage, "stop": stop}.get(art, lambda _: None)(eingabe)
    except Exception as fehler:
        schreib(f"{art}: {type(fehler).__name__}: {fehler}")


if __name__ == "__main__":
    main()
    sys.exit(0)
