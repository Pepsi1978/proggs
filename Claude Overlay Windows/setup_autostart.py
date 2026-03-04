"""Erstellt einen Windows-Autostart-Eintrag und eine Desktop-Verknuepfung fuer den Overlay-Watcher."""

import os
import sys
from pathlib import Path

try:
    import win32com.client
except ImportError:
    print("Fehler: pywin32 nicht installiert. Bitte 'pip install pywin32' ausfuehren.")
    sys.exit(1)


def create_shortcut(shortcut_path: Path, target: Path, working_dir: Path, description: str, icon: str = "") -> None:
    """Erstellt eine Windows .lnk Verknuepfung."""
    shell = win32com.client.Dispatch("WScript.Shell")
    shortcut = shell.CreateShortCut(str(shortcut_path))
    shortcut.TargetPath = str(target)
    shortcut.WorkingDirectory = str(working_dir)
    shortcut.Description = description
    if icon:
        shortcut.IconLocation = icon
    shortcut.WindowStyle = 7  # Minimiert
    shortcut.Save()


def main() -> None:
    project_dir = Path(__file__).resolve().parent
    vbs_path = project_dir / "start_overlay.vbs"

    if not vbs_path.exists():
        print(f"Fehler: {vbs_path} nicht gefunden.")
        sys.exit(1)

    # Windows-Autostart-Ordner
    autostart_dir = Path(os.environ["APPDATA"]) / "Microsoft" / "Windows" / "Start Menu" / "Programs" / "Startup"
    autostart_link = autostart_dir / "Claude Overlay Watcher.lnk"

    # Desktop
    desktop_dir = Path(os.environ["USERPROFILE"]) / "Desktop"
    desktop_link = desktop_dir / "Claude Overlay Watcher.lnk"

    # WScript.exe als Target (um .vbs auszufuehren)
    wscript = Path(os.environ.get("WINDIR", r"C:\Windows")) / "System32" / "wscript.exe"

    print("=== Claude Overlay Watcher Setup ===\n")
    print(f"Projekt: {project_dir}")
    print(f"Startskript: {vbs_path}\n")

    # --- Autostart ---
    print(f"1) Autostart-Verknuepfung: {autostart_link}")
    try:
        shell = win32com.client.Dispatch("WScript.Shell")
        shortcut = shell.CreateShortCut(str(autostart_link))
        shortcut.TargetPath = str(wscript)
        shortcut.Arguments = f'"{vbs_path}"'
        shortcut.WorkingDirectory = str(project_dir)
        shortcut.Description = "Claude Overlay Watcher - startet Overlay automatisch mit Claude Desktop"
        shortcut.WindowStyle = 7
        shortcut.Save()
        print("   -> Erstellt! Der Watcher startet jetzt automatisch mit Windows.\n")
    except Exception as exc:
        print(f"   -> Fehler: {exc}\n")

    # --- Desktop-Verknuepfung ---
    print(f"2) Desktop-Verknuepfung: {desktop_link}")
    try:
        shell = win32com.client.Dispatch("WScript.Shell")
        shortcut = shell.CreateShortCut(str(desktop_link))
        shortcut.TargetPath = str(wscript)
        shortcut.Arguments = f'"{vbs_path}"'
        shortcut.WorkingDirectory = str(project_dir)
        shortcut.Description = "Claude Overlay Watcher - startet Overlay automatisch mit Claude Desktop"
        shortcut.WindowStyle = 7
        shortcut.Save()
        print("   -> Erstellt! Du kannst den Watcher jetzt per Doppelklick starten.\n")
    except Exception as exc:
        print(f"   -> Fehler: {exc}\n")

    print("Fertig! Der Watcher startet automatisch mit Windows und ueberwacht Claude Desktop.")


if __name__ == "__main__":
    main()
