"""
Codex Desktop Voice Input - macOS Overlay
Schwebender Mikrofon-Button über der Codex Desktop App.

Nutzung:
    python voice_overlay_macos.py [--model base] [--lang de]

Tastenkürzel:
    Cmd+Shift+M  - Aufnahme starten/stoppen (global, benötigt pynput)

Hinweis:
    macOS erfordert Mikrofon- und Bedienungshilfen-Berechtigung.
    Beim ersten Start wirst du danach gefragt.
"""

import sys
import os
import threading
import argparse
import tkinter as tk
from tkinter import font as tkfont

# Eigenes Core-Modul
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from voice_core import AudioRecorder, transcribe_audio

# macOS-spezifisch: globaler Hotkey über pynput
try:
    from pynput import keyboard as pynput_keyboard
    HAS_PYNPUT = True
except ImportError:
    HAS_PYNPUT = False


def paste_to_codex_window(text):
    """Kopiert Text in die Zwischenablage und simuliert Cmd+V im Codex-Fenster."""
    import subprocess
    import time

    if not text.strip():
        return

    # Text in macOS Clipboard kopieren
    process = subprocess.Popen(["pbcopy"], stdin=subprocess.PIPE)
    process.communicate(text.encode("utf-8"))

    # Codex-Fenster über AppleScript fokussieren
    try:
        subprocess.run(
            [
                "osascript", "-e",
                'tell application "Codex" to activate'
            ],
            timeout=3,
            capture_output=True,
        )
        time.sleep(0.3)
    except Exception:
        time.sleep(0.3)

    # Cmd+V per AppleScript simulieren
    try:
        subprocess.run(
            [
                "osascript", "-e",
                'tell application "System Events" to keystroke "v" using command down'
            ],
            timeout=3,
            capture_output=True,
        )
    except Exception:
        # Fallback: pyautogui
        try:
            import pyautogui
            pyautogui.hotkey("command", "v")
        except ImportError:
            print("Konnte Cmd+V nicht simulieren. Installiere pyautogui als Fallback.")


class VoiceOverlayMacOS:
    """Schwebender Mikrofon-Button als Overlay über der Codex Desktop App."""

    # Farben
    COLOR_IDLE = "#2D2D2D"
    COLOR_RECORDING = "#E53935"
    COLOR_PROCESSING = "#FF9800"
    COLOR_HOVER = "#3D3D3D"
    COLOR_TEXT = "#FFFFFF"

    def __init__(self, model_size="base", language="de"):
        self.model_size = model_size
        self.language = language
        self.recorder = AudioRecorder()
        self.is_recording = False
        self.is_processing = False

        # Tkinter Setup
        self.root = tk.Tk()
        self.root.title("Codex Voice")
        self.root.overrideredirect(True)          # Kein Fensterrahmen
        self.root.attributes("-topmost", True)     # Immer im Vordergrund

        # macOS-spezifisch: Transparenz
        self.root.attributes("-alpha", 0.92)
        # macOS transparent background
        self.root.configure(bg="systemTransparent")

        # Fenstergröße und Position (rechts unten)
        self.btn_size = 56
        self.padding = 20
        screen_w = self.root.winfo_screenwidth()
        screen_h = self.root.winfo_screenheight()
        x = screen_w - self.btn_size - self.padding - 80
        y = screen_h - self.btn_size - self.padding - 120
        self.root.geometry(f"{self.btn_size + 40}x{self.btn_size + 60}+{x}+{y}")

        # Canvas für runden Button
        bg_color = "#1E1E1E"
        self.canvas = tk.Canvas(
            self.root,
            width=self.btn_size + 40,
            height=self.btn_size + 60,
            bg=bg_color,
            highlightthickness=0,
        )
        self.canvas.pack()

        # Runden Hintergrund zeichnen (um den "nicht-transparenten" Canvas zu kaschieren)
        cx = (self.btn_size + 40) // 2
        cy = self.btn_size // 2 + 5
        r = self.btn_size // 2

        # Äußerer Schatten-Kreis
        self.canvas.create_oval(
            cx - r - 3, cy - r - 3, cx + r + 3, cy + r + 3,
            fill="#111111",
            outline="",
        )

        # Haupt-Button
        self.btn_circle = self.canvas.create_oval(
            cx - r, cy - r, cx + r, cy + r,
            fill=self.COLOR_IDLE,
            outline="#555555",
            width=2,
        )

        # Mikrofon-Symbol
        # macOS hat guten Unicode-Support
        try:
            mic_font = tkfont.Font(family="Apple Color Emoji", size=20)
        except Exception:
            mic_font = tkfont.Font(size=20)

        self.mic_text = self.canvas.create_text(
            cx, cy,
            text="\U0001F3A4",
            font=mic_font,
            fill=self.COLOR_TEXT,
        )

        # Status-Text
        try:
            status_font = tkfont.Font(family="SF Pro Text", size=9)
        except Exception:
            status_font = tkfont.Font(size=9)

        self.status_text = self.canvas.create_text(
            cx, cy + r + 14,
            text="Bereit",
            font=status_font,
            fill="#AAAAAA",
        )

        # Events
        self.canvas.tag_bind(self.btn_circle, "<Button-1>", self._on_click)
        self.canvas.tag_bind(self.mic_text, "<Button-1>", self._on_click)
        self.canvas.tag_bind(self.btn_circle, "<Enter>", self._on_enter)
        self.canvas.tag_bind(self.btn_circle, "<Leave>", self._on_leave)

        # Drag zum Verschieben (Rechtsklick auf macOS = Ctrl+Click oder Button-2)
        self._drag_data = {"x": 0, "y": 0}
        self.canvas.bind("<ButtonPress-2>", self._on_drag_start)
        self.canvas.bind("<B2-Motion>", self._on_drag_motion)
        # Auch Ctrl+Click für macOS
        self.canvas.bind("<Control-ButtonPress-1>", self._on_drag_start)
        self.canvas.bind("<Control-B1-Motion>", self._on_drag_motion)

        # Globaler Hotkey (Cmd+Shift+M) über pynput
        self._hotkey_listener = None
        if HAS_PYNPUT:
            self._setup_global_hotkey()

        # Escape zum Beenden
        self.root.bind("<Escape>", lambda e: self._quit())

    def _setup_global_hotkey(self):
        """Richtet den globalen Hotkey Cmd+Shift+M ein."""
        pressed_keys = set()

        def on_press(key):
            pressed_keys.add(key)
            # Cmd+Shift+M prüfen
            if (
                pynput_keyboard.Key.cmd in pressed_keys
                and pynput_keyboard.Key.shift in pressed_keys
                and hasattr(key, "char") and key.char == "m"
            ):
                self.root.after(0, self._on_click)

        def on_release(key):
            pressed_keys.discard(key)

        self._hotkey_listener = pynput_keyboard.Listener(
            on_press=on_press,
            on_release=on_release,
        )
        self._hotkey_listener.daemon = True
        self._hotkey_listener.start()

    def _on_click(self, event=None):
        if self.is_processing:
            return
        if self.is_recording:
            self._stop_recording()
        else:
            self._start_recording()

    def _start_recording(self):
        self.is_recording = True
        self.canvas.itemconfig(self.btn_circle, fill=self.COLOR_RECORDING)
        self.canvas.itemconfig(self.status_text, text="Aufnahme...", fill="#E53935")
        self.recorder.start()
        self._pulse_animation()

    def _stop_recording(self):
        self.is_recording = False
        self.is_processing = True
        self.canvas.itemconfig(self.btn_circle, fill=self.COLOR_PROCESSING)
        self.canvas.itemconfig(self.status_text, text="Verarbeite...", fill="#FF9800")

        audio_data = self.recorder.stop()

        def do_transcribe():
            try:
                text = transcribe_audio(
                    audio_data,
                    language=self.language,
                    model_size=self.model_size,
                )
                if text.strip():
                    self.root.after(0, lambda: self._on_transcription_done(text))
                else:
                    self.root.after(0, lambda: self._on_transcription_done(None))
            except Exception as e:
                print(f"Transkription Fehler: {e}")
                self.root.after(0, lambda: self._on_transcription_done(None))

        threading.Thread(target=do_transcribe, daemon=True).start()

    def _on_transcription_done(self, text):
        self.is_processing = False
        self.canvas.itemconfig(self.btn_circle, fill=self.COLOR_IDLE)

        if text:
            self.canvas.itemconfig(self.status_text, text="Eingefügt!", fill="#4CAF50")
            paste_to_codex_window(text)
        else:
            self.canvas.itemconfig(
                self.status_text, text="Kein Text erkannt", fill="#FF5722"
            )

        self.root.after(3000, self._reset_status)

    def _reset_status(self):
        if not self.is_recording and not self.is_processing:
            self.canvas.itemconfig(self.status_text, text="Bereit", fill="#AAAAAA")

    def _pulse_animation(self):
        if not self.is_recording:
            return
        current = self.canvas.itemcget(self.btn_circle, "outline")
        new_color = "#FF6666" if current == "#555555" else "#555555"
        self.canvas.itemconfig(self.btn_circle, outline=new_color)
        self.root.after(500, self._pulse_animation)

    def _on_enter(self, event):
        if not self.is_recording and not self.is_processing:
            self.canvas.itemconfig(self.btn_circle, fill=self.COLOR_HOVER)

    def _on_leave(self, event):
        if not self.is_recording and not self.is_processing:
            self.canvas.itemconfig(self.btn_circle, fill=self.COLOR_IDLE)

    def _on_drag_start(self, event):
        self._drag_data["x"] = event.x
        self._drag_data["y"] = event.y

    def _on_drag_motion(self, event):
        x = self.root.winfo_x() + (event.x - self._drag_data["x"])
        y = self.root.winfo_y() + (event.y - self._drag_data["y"])
        self.root.geometry(f"+{x}+{y}")

    def _quit(self):
        if self.is_recording:
            self.recorder.stop()
        if self._hotkey_listener:
            self._hotkey_listener.stop()
        self.root.destroy()

    def run(self):
        print("Codex Voice Input - macOS")
        print("Klicke auf den Mikrofon-Button oder drücke Cmd+Shift+M")
        print("Ctrl+Click + Ziehen zum Verschieben")
        print("Escape zum Beenden")
        print()
        print("Hinweis: Erlaube Mikrofon- und Bedienungshilfen-Zugriff")
        print("unter Systemeinstellungen > Datenschutz & Sicherheit")
        self.root.mainloop()


def main():
    parser = argparse.ArgumentParser(
        description="Codex Desktop Voice Input Overlay (macOS)"
    )
    parser.add_argument(
        "--model",
        default="base",
        choices=["tiny", "base", "small", "medium", "large"],
        help="Whisper Modell-Größe (Standard: base)",
    )
    parser.add_argument(
        "--lang",
        default="de",
        help="Sprache für die Erkennung (Standard: de)",
    )
    args = parser.parse_args()

    app = VoiceOverlayMacOS(model_size=args.model, language=args.lang)
    app.run()


if __name__ == "__main__":
    main()
