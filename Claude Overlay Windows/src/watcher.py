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
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s  %(message)s",
        datefmt="%Y-%m-%d %H:%M:%S",
        handlers=[
            logging.FileHandler(LOG_PATH, encoding="utf-8"),
            logging.StreamHandler(sys.stderr),
        ],
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

        if claude_running and overlay_process is not None:
            if overlay_process.poll() is not None:
                logging.info(
                    "Overlay beendet (Code %s) – Neustart",
                    overlay_process.returncode,
                )
                if overlay_log_fh:
                    overlay_log_fh.close()
                overlay_log_fh = open(overlay_log, "a", encoding="utf-8")
                overlay_process = start_overlay(overlay_script, overlay_log_fh)

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
    main()
