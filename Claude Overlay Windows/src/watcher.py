from __future__ import annotations

import subprocess
import sys
import time
from pathlib import Path

from claude_window import is_claude_running
from config import Settings


def start_overlay(script_path: Path) -> subprocess.Popen:
    """Startet das Overlay als eigenen Prozess."""
    return subprocess.Popen(
        [sys.executable, str(script_path)],
        creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0),
    )


def main() -> None:
    settings = Settings.load()
    overlay_script = Path(__file__).resolve().parent / "overlay_app.py"
    overlay_process: subprocess.Popen | None = None

    while True:
        claude_running = is_claude_running(settings)

        if claude_running and overlay_process is None:
            overlay_process = start_overlay(overlay_script)

        if claude_running and overlay_process is not None:
            if overlay_process.poll() is not None:
                overlay_process = start_overlay(overlay_script)

        if not claude_running and overlay_process is not None:
            if overlay_process.poll() is None:
                overlay_process.terminate()
                try:
                    overlay_process.wait(timeout=3)
                except subprocess.TimeoutExpired:
                    overlay_process.kill()
            overlay_process = None

        time.sleep(2)


if __name__ == "__main__":
    main()
