from __future__ import annotations

import sys
import threading
import time
from pathlib import Path

import ctypes
import tkinter as tk
import tkinter.font as tkfont

from api_clients import improve_text_with_gemini, transcribe_with_grok
from audio_capture import AudioRecorder
from claude_window import clear_claude_input, insert_text_into_claude, is_claude_running
from config import Settings

# ---------------------------------------------------------------------------
# Farben
# ---------------------------------------------------------------------------
COLOR_BG = "#1E1E1E"
COLOR_IDLE = "#2D2D2D"
COLOR_HOVER = "#3D3D3D"
COLOR_RECORDING = "#E53935"
COLOR_PROCESSING = "#FF9800"
COLOR_SUCCESS = "#43A047"
COLOR_ERROR = "#E53935"
COLOR_X_IDLE = "#444444"
COLOR_X_HOVER = "#D73A49"
COLOR_TEXT = "#FFFFFF"
COLOR_STATUS = "#AAAAAA"
TRANSPARENT = "magenta"


class ClaudeOverlayApp:
    """Modernes rahmenloses Overlay mit Mikrofon- und X-Button."""

    BTN_RADIUS = 28
    BTN_GAP = 16
    PADDING = 12
    STATUS_HEIGHT = 22

    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self.settings.validate()

        self.recorder = AudioRecorder(
            sample_rate=self.settings.audio_sample_rate,
            channels=self.settings.audio_channels,
        )
        self.is_recording = False
        self.is_processing = False
        self._drag_data = {"x": 0, "y": 0}

        # ----- Fenster -----
        self.root = tk.Tk()
        self.root.title("Mic Overlay")
        self.root.overrideredirect(True)
        self.root.attributes("-topmost", True)
        self.root.attributes("-alpha", 0.93)
        self.root.configure(bg=TRANSPARENT)
        self.root.attributes("-transparentcolor", TRANSPARENT)

        r = self.BTN_RADIUS
        d = r * 2
        total_w = d + self.BTN_GAP + d + self.PADDING * 2
        total_h = d + self.STATUS_HEIGHT + self.PADDING * 2

        screen_w = self.root.winfo_screenwidth()
        x = screen_w - total_w - 24
        y = 24
        self.root.geometry(f"{total_w}x{total_h}+{x}+{y}")

        # ----- Canvas -----
        self.canvas = tk.Canvas(
            self.root,
            width=total_w,
            height=total_h,
            bg=TRANSPARENT,
            highlightthickness=0,
        )
        self.canvas.pack(fill="both", expand=True)

        # Hintergrund-Rechteck (abgerundet nachgeahmt)
        self._draw_rounded_rect(
            2, 2, total_w - 2, total_h - 2, radius=16, fill=COLOR_BG, outline="#333333"
        )

        # ----- Mikrofon-Button -----
        mic_cx = self.PADDING + r
        mic_cy = self.PADDING + r

        self.mic_circle = self.canvas.create_oval(
            mic_cx - r, mic_cy - r, mic_cx + r, mic_cy + r,
            fill=COLOR_IDLE, outline="#555555", width=2,
        )

        mic_font = tkfont.Font(family="Segoe UI Emoji", size=18)
        self.mic_text = self.canvas.create_text(
            mic_cx, mic_cy, text="\U0001F3A4", font=mic_font, fill=COLOR_TEXT,
        )

        # ----- X-Button -----
        x_cx = mic_cx + d + self.BTN_GAP
        x_cy = mic_cy

        self.x_circle = self.canvas.create_oval(
            x_cx - r, x_cy - r, x_cx + r, x_cy + r,
            fill=COLOR_X_IDLE, outline="#555555", width=2,
        )

        x_font = tkfont.Font(family="Segoe UI", size=16, weight="bold")
        self.x_text = self.canvas.create_text(
            x_cx, x_cy, text="\u2715", font=x_font, fill=COLOR_TEXT,
        )

        # ----- Statustext -----
        status_font = tkfont.Font(family="Segoe UI", size=9)
        status_cy = mic_cy + r + self.STATUS_HEIGHT // 2 + 4
        self.status_text = self.canvas.create_text(
            total_w // 2, status_cy, text="Bereit",
            font=status_font, fill=COLOR_STATUS,
        )

        # ----- Hitboxen speichern -----
        self._mic_bbox = (mic_cx - r, mic_cy - r, mic_cx + r, mic_cy + r)
        self._x_bbox = (x_cx - r, x_cy - r, x_cx + r, x_cy + r)

        # ----- Events -----
        self.canvas.bind("<Button-1>", self._on_click)
        self.canvas.bind("<Motion>", self._on_motion)
        self.canvas.bind("<Leave>", self._on_leave)
        self.canvas.bind("<Button-3>", self._on_drag_start)
        self.canvas.bind("<B3-Motion>", self._on_drag_motion)

        self.root.bind("<Escape>", lambda _: self._quit())

        # Rechtsklick-Kontextmenue
        self.menu = tk.Menu(self.root, tearoff=0)
        self.menu.add_command(label="Beenden", command=self._quit)

        # Claude-Prozess-Watcher
        self.root.after(2000, self._watch_claude_process)

    # ------------------------------------------------------------------
    # Zeichenhilfe
    # ------------------------------------------------------------------
    def _draw_rounded_rect(
        self, x1: int, y1: int, x2: int, y2: int,
        radius: int = 16, **kwargs,
    ) -> int:
        points = [
            x1 + radius, y1,
            x2 - radius, y1,
            x2, y1,
            x2, y1 + radius,
            x2, y2 - radius,
            x2, y2,
            x2 - radius, y2,
            x1 + radius, y2,
            x1, y2,
            x1, y2 - radius,
            x1, y1 + radius,
            x1, y1,
        ]
        return self.canvas.create_polygon(points, smooth=True, **kwargs)

    # ------------------------------------------------------------------
    # Hit-Tests
    # ------------------------------------------------------------------
    def _in_bbox(self, ex: int, ey: int, bbox: tuple) -> bool:
        return bbox[0] <= ex <= bbox[2] and bbox[1] <= ey <= bbox[3]

    # ------------------------------------------------------------------
    # Maus-Events
    # ------------------------------------------------------------------
    def _on_click(self, event: tk.Event) -> None:
        if self._in_bbox(event.x, event.y, self._mic_bbox):
            self._toggle_recording()
        elif self._in_bbox(event.x, event.y, self._x_bbox):
            self._clear_input()

    def _on_motion(self, event: tk.Event) -> None:
        if self.is_recording or self.is_processing:
            return
        if self._in_bbox(event.x, event.y, self._mic_bbox):
            self.canvas.itemconfig(self.mic_circle, fill=COLOR_HOVER)
            self.canvas.itemconfig(self.x_circle, fill=COLOR_X_IDLE)
        elif self._in_bbox(event.x, event.y, self._x_bbox):
            self.canvas.itemconfig(self.x_circle, fill=COLOR_X_HOVER)
            self.canvas.itemconfig(self.mic_circle, fill=COLOR_IDLE)
        else:
            self.canvas.itemconfig(self.mic_circle, fill=COLOR_IDLE)
            self.canvas.itemconfig(self.x_circle, fill=COLOR_X_IDLE)

    def _on_leave(self, event: tk.Event) -> None:
        if not self.is_recording and not self.is_processing:
            self.canvas.itemconfig(self.mic_circle, fill=COLOR_IDLE)
            self.canvas.itemconfig(self.x_circle, fill=COLOR_X_IDLE)

    def _on_drag_start(self, event: tk.Event) -> None:
        self._drag_data["x"] = event.x
        self._drag_data["y"] = event.y

    def _on_drag_motion(self, event: tk.Event) -> None:
        dx = event.x - self._drag_data["x"]
        dy = event.y - self._drag_data["y"]
        x = self.root.winfo_x() + dx
        y = self.root.winfo_y() + dy
        self.root.geometry(f"+{x}+{y}")

    # ------------------------------------------------------------------
    # Statustext
    # ------------------------------------------------------------------
    def _set_status(self, text: str, color: str = COLOR_STATUS) -> None:
        self.canvas.itemconfig(self.status_text, text=text, fill=color)

    # ------------------------------------------------------------------
    # Puls-Animation waehrend Aufnahme
    # ------------------------------------------------------------------
    def _pulse_animation(self) -> None:
        if not self.is_recording:
            return
        current = self.canvas.itemcget(self.mic_circle, "outline")
        new_color = "#FF6666" if current == "#555555" else "#555555"
        self.canvas.itemconfig(self.mic_circle, outline=new_color)
        self.root.after(500, self._pulse_animation)

    # ------------------------------------------------------------------
    # Aufnahme
    # ------------------------------------------------------------------
    def _toggle_recording(self) -> None:
        if self.is_processing:
            return
        if self.is_recording:
            self._stop_recording()
        else:
            self._start_recording()

    def _start_recording(self) -> None:
        try:
            self.recorder.start()
            self.is_recording = True
            self.canvas.itemconfig(self.mic_circle, fill=COLOR_RECORDING)
            self._set_status("Aufnahme...", COLOR_RECORDING)
            self._pulse_animation()
        except Exception as exc:
            self._set_status("Mikrofon-Fehler", COLOR_ERROR)
            print(f"[Fehler] Aufnahme konnte nicht gestartet werden: {exc}", file=sys.stderr)

    def _stop_recording(self) -> None:
        try:
            audio_path = self.recorder.stop()
        except Exception as exc:
            self.is_recording = False
            self.canvas.itemconfig(self.mic_circle, fill=COLOR_IDLE, outline="#555555")
            self._set_status("Aufnahmefehler", COLOR_ERROR)
            print(f"[Fehler] Aufnahme konnte nicht beendet werden: {exc}", file=sys.stderr)
            return

        self.is_recording = False
        self.is_processing = True
        self.canvas.itemconfig(self.mic_circle, fill=COLOR_PROCESSING, outline="#555555")
        self._set_status("Verarbeite...", COLOR_PROCESSING)

        worker = threading.Thread(
            target=self._process_audio_pipeline,
            args=(audio_path,),
            daemon=True,
        )
        worker.start()

    def _process_audio_pipeline(self, audio_path: Path) -> None:
        try:
            self.root.after(0, lambda: self._set_status("Transkribiere...", COLOR_PROCESSING))
            transcript = transcribe_with_grok(audio_path, self.settings)

            self.root.after(0, lambda: self._set_status("Verbessere Text...", COLOR_PROCESSING))
            result = improve_text_with_gemini(transcript, self.settings)

            final_text = result["verbesserter_text"]
            overlay_hwnd = self._get_overlay_hwnd()
            insert_text_into_claude(final_text, overlay_hwnd=overlay_hwnd)

            self.root.after(0, lambda: self._on_pipeline_success())
        except Exception as exc:
            err_msg = str(exc)
            self.root.after(0, lambda: self._on_pipeline_error(err_msg))
        finally:
            try:
                audio_path.unlink(missing_ok=True)
            except Exception:
                pass

    def _on_pipeline_success(self) -> None:
        self.is_processing = False
        self.canvas.itemconfig(self.mic_circle, fill=COLOR_SUCCESS)
        self._set_status("Eingefuegt!", COLOR_SUCCESS)
        self.root.after(3000, self._reset_to_idle)

    def _on_pipeline_error(self, msg: str) -> None:
        self.is_processing = False
        self.canvas.itemconfig(self.mic_circle, fill=COLOR_ERROR)
        # Kurze Fehlermeldung im Overlay, volle Meldung im Terminal
        if "Rate Limit" in msg or "429" in msg:
            short = "API ueberlastet - warte kurz"
        elif "503" in msg or "nicht erreichbar" in msg:
            short = "API nicht erreichbar"
        elif "Fenster wurde nicht gefunden" in msg:
            short = "Claude nicht gefunden"
        else:
            short = "Fehler"
        self._set_status(short, COLOR_ERROR)
        print(f"[Fehler] Pipeline: {msg}", file=sys.stderr)
        self.root.after(4000, self._reset_to_idle)

    def _reset_to_idle(self) -> None:
        if not self.is_recording and not self.is_processing:
            self.canvas.itemconfig(self.mic_circle, fill=COLOR_IDLE)
            self._set_status("Bereit", COLOR_STATUS)

    # ------------------------------------------------------------------
    # X-Button: Eingabefeld leeren
    # ------------------------------------------------------------------
    def _clear_input(self) -> None:
        try:
            clear_claude_input(overlay_hwnd=self._get_overlay_hwnd())
            self._set_status("Feld geleert", COLOR_SUCCESS)
            self.root.after(2000, self._reset_to_idle)
        except Exception as exc:
            self._set_status("Loeschen fehlgeschlagen", COLOR_ERROR)
            print(f"[Fehler] Eingabefeld leeren: {exc}", file=sys.stderr)
            self.root.after(3000, self._reset_to_idle)

    # ------------------------------------------------------------------
    # Claude-Prozess-Watcher
    # ------------------------------------------------------------------
    def _watch_claude_process(self) -> None:
        if not is_claude_running(self.settings):
            self._quit()
            return
        self.root.after(2000, self._watch_claude_process)

    # ------------------------------------------------------------------
    # Beenden
    # ------------------------------------------------------------------
    def _quit(self) -> None:
        if self.recorder.is_recording:
            try:
                self.recorder.stop()
            except Exception:
                pass
        self.root.destroy()

    def _get_overlay_hwnd(self) -> int | None:
        """Gibt das native HWND des Tkinter-Overlay-Fensters zurueck."""
        try:
            return ctypes.windll.user32.GetParent(self.root.winfo_id())
        except Exception:
            return None

    def run(self) -> None:
        self.root.mainloop()


def main() -> None:
    settings = Settings.load()

    # Auf Claude Desktop warten (max 5 Minuten beim Autostart)
    if not is_claude_running(settings):
        print("Warte auf Claude Desktop...", file=sys.stderr)
        for _ in range(60):
            time.sleep(5)
            if is_claude_running(settings):
                break
        else:
            print("Claude Desktop nicht gestartet. Beende.", file=sys.stderr)
            return

    app = ClaudeOverlayApp(settings)
    app.run()


if __name__ == "__main__":
    main()
