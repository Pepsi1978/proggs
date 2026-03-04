from __future__ import annotations

import logging
import subprocess
import sys
import time
from pathlib import Path

from claude_window import is_claude_running
from config import Settings

LOG_PATH = Path(__file__).resolve().parents[1] / "watcher.log"


def _setup_logging() -> None:
    handlers: list[logging.Handler] = [
        logging.FileHandler(LOG_PATH, encoding="utf-8"),
    ]
    # pythonw.exe setzt sys.stderr auf None — StreamHandler wuerde abstuerzen
    if sys.stderr is not None:
        handlers.append(logging.StreamHandler(sys.stderr))

    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s  %(message)s",
        datefmt="%Y-%m-%d %H:%M:%S",
        handlers=handlers,
    )


def start_overlay(script_path: Path, log_file) -> subprocess.Popen:
    """Startet das Overlay als eigenen Prozess."""
    logging.info("Starte Overlay: %s", script_path)
    return subprocess.Popen(
        [sys.executable, str(script_path)],
        stdout=log_file,
        stderr=log_file,
        creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0),
    )


def main() -> None:
    _setup_logging()
    logging.info("=== Watcher gestartet (Python: %s) ===", sys.executable)

    try:
        settings = Settings.load()
    except Exception:
        logging.exception("Fehler beim Laden der Einstellungen")
        return

    logging.info(
        "Ueberwache Prozesse: %s", ", ".join(settings.claude_process_names)
    )

    overlay_script = Path(__file__).resolve().parent / "overlay_app.py"
    overlay_log = LOG_PATH.with_name("overlay.log")
    overlay_process: subprocess.Popen | None = None
    overlay_log_fh = None
    rapid_crash_count = 0
    last_start_time = 0.0

    while True:
        try:
            claude_running = is_claude_running(settings)
        except Exception:
            logging.exception("Fehler bei Prozessabfrage")
            time.sleep(2)
            continue

        if claude_running and overlay_process is None:
            logging.info("Claude erkannt – starte Overlay")
            overlay_log_fh = open(overlay_log, "a", encoding="utf-8")
            overlay_process = start_overlay(overlay_script, overlay_log_fh)
            last_start_time = time.time()

        if claude_running and overlay_process is not None:
            if overlay_process.poll() is not None:
                uptime = time.time() - last_start_time
                logging.info(
                    "Overlay beendet (Code %s, Laufzeit %.0fs)",
                    overlay_process.returncode,
                    uptime,
                )
                if overlay_log_fh:
                    overlay_log_fh.close()

                if uptime < 10:
                    rapid_crash_count += 1
                else:
                    rapid_crash_count = 0

                if rapid_crash_count >= 3:
                    logging.error(
                        "Overlay 3x in Folge sofort abgestuerzt. "
                        "Warte 30s. Siehe overlay.log fuer Details."
                    )
                    overlay_process = None
                    time.sleep(30)
                    rapid_crash_count = 0
                    continue

                overlay_log_fh = open(overlay_log, "a", encoding="utf-8")
                overlay_process = start_overlay(overlay_script, overlay_log_fh)
                last_start_time = time.time()

        if not claude_running and overlay_process is not None:
            logging.info("Claude nicht mehr aktiv – beende Overlay")
            if overlay_process.poll() is None:
                overlay_process.terminate()
                try:
                    overlay_process.wait(timeout=3)
                except subprocess.TimeoutExpired:
                    overlay_process.kill()
            overlay_process = None
            if overlay_log_fh:
                overlay_log_fh.close()
                overlay_log_fh = None

        time.sleep(2)


if __name__ == "__main__":
    try:
        main()
    except Exception:
        # Fehler auch bei pythonw (kein stderr) in die Log-Datei schreiben
        import traceback

        with open(LOG_PATH, "a", encoding="utf-8") as f:
            f.write(traceback.format_exc())
        raise
