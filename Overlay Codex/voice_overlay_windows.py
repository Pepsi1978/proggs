"""
Codex Desktop Voice Input - Windows Overlay
Schwebender Mikrofon-Button über der Codex Desktop App.

Nutzung:
    python voice_overlay_windows.py [--model base] [--lang de]

Tastenkürzel:
    Strg+Shift+M  - Aufnahme starten/stoppen (global)
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

# Windows-spezifisch: globaler Hotkey
try:
    import keyboard  # pip install keyboard
    HAS_KEYBOARD = True
except ImportError:
    HAS_KEYBOARD = False


def paste_to_codex_window(text):
    """Kopiert Text in die Zwischenablage und simuliert Ctrl+V im Codex-Fenster."""
    import pyperclip
    import pyautogui
    import time

    if not text.strip():
        return

    pyperclip.copy(text)

    # Codex Desktop Fenster finden und fokussieren
    try:
        import pygetwindow as gw
        windows = gw.getWindowsWithTitle("Codex")
        if windows:
            codex_win = windows[0]
            if codex_win.isMinimized:
                codex_win.restore()
            codex_win.activate()
            time.sleep(0.3)
    except Exception:
        # Falls pygetwindow nicht verfügbar, einfach Ctrl+V senden
        time.sleep(0.3)

    pyautogui.hotkey("ctrl", "v")


class VoiceOverlayWindows:
    """Schwebender Mikrofon-Button als Overlay über der Codex Desktop App."""

    # Farben
    COLOR_IDLE = "#2D2D2D"
    COLOR_RECORDING = "#E53935"
    COLOR_PROCESSING = "#FF9800"
    COLOR_HOVER = "#3D3D3D"
    COLOR_TEXT = "#FFFFFF"
    COLOR_MIC = "#FFFFFF"

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
        self.root.attributes("-alpha", 0.92)       # Leicht transparent

        # Windows-spezifisch: transparente Farbe
        self.root.configure(bg="magenta")
        self.root.attributes("-transparentcolor", "magenta")

        # Fenstergröße und Position (rechts unten)
        self.btn_size = 56
        self.padding = 20
        screen_w = self.root.winfo_screenwidth()
        screen_h = self.root.winfo_screenheight()
        x = screen_w - self.btn_size - self.padding - 80
        y = screen_h - self.btn_size - self.padding - 100
        self.root.geometry(f"{self.btn_size + 40}x{self.btn_size + 60}+{x}+{y}")

        # Canvas für runden Button
        self.canvas = tk.Canvas(
            self.root,
            width=self.btn_size + 40,
            height=self.btn_size + 60,
            bg="magenta",
            highlightthickness=0,
        )
        self.canvas.pack()

        # Runder Button zeichnen
        cx = (self.btn_size + 40) // 2
        cy = self.btn_size // 2 + 5
        r = self.btn_size // 2

        self.btn_circle = self.canvas.create_oval(
            cx - r, cy - r, cx + r, cy + r,
            fill=self.COLOR_IDLE,
            outline="#555555",
            width=2,
        )

        # Mikrofon-Symbol (Unicode)
        mic_font = tkfont.Font(family="Segoe UI Emoji", size=20)
        self.mic_text = self.canvas.create_text(
            cx, cy,
            text="\U0001F3A4",  # Mikrofon Emoji
            font=mic_font,
            fill=self.COLOR_MIC,
        )

        # Status-Text unter dem Button
        status_font = tkfont.Font(family="Segoe UI", size=8)
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

        # Drag zum Verschieben
        self._drag_data = {"x": 0, "y": 0}
        self.canvas.bind("<ButtonPress-3>", self._on_drag_start)
        self.canvas.bind("<B3-Motion>", self._on_drag_motion)

        # Rechtsklick-Menü
        self.menu = tk.Menu(self.root, tearoff=0)
        self.menu.add_command(label="Beenden", command=self._quit)

        # Globaler Hotkey (Strg+Shift+M)
        if HAS_KEYBOARD:
            keyboard.add_hotkey("ctrl+shift+m", self._on_hotkey)

        # Escape zum Beenden
        self.root.bind("<Escape>", lambda e: self._quit())

    def _on_click(self, event=None):
        if self.is_processing:
            return
        if self.is_recording:
            self._stop_recording()
        else:
            self._start_recording()

    def _on_hotkey(self):
        """Globaler Hotkey Handler (wird aus anderem Thread aufgerufen)."""
        self.root.after(0, self._on_click)

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

        # Transkription im Hintergrund
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

        # Status nach 3 Sekunden zurücksetzen
        self.root.after(3000, self._reset_status)

    def _reset_status(self):
        if not self.is_recording and not self.is_processing:
            self.canvas.itemconfig(self.status_text, text="Bereit", fill="#AAAAAA")

    def _pulse_animation(self):
        """Pulsierende Animation während der Aufnahme."""
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
        if HAS_KEYBOARD:
            keyboard.unhook_all()
        self.root.destroy()

    def run(self):
        print("Codex Voice Input - Windows")
        print("Klicke auf den Mikrofon-Button oder drücke Strg+Shift+M")
        print("Rechtsklick + Ziehen zum Verschieben")
        print("Escape zum Beenden")
        self.root.mainloop()


def main():
    parser = argparse.ArgumentParser(
        description="Codex Desktop Voice Input Overlay (Windows)"
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

    app = VoiceOverlayWindows(model_size=args.model, language=args.lang)
    app.run()


if __name__ == "__main__":
    main()
