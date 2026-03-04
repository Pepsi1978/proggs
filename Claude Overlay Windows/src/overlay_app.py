from __future__ import annotations

import logging
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
# Logging (sicher fuer pythonw, wo sys.stderr = None)
# ---------------------------------------------------------------------------
_log_handlers: list[logging.Handler] = [
    logging.FileHandler(
        Path(__file__).resolve().parents[1] / "overlay.log",
        encoding="utf-8",
    ),
]
if sys.stderr is not None:
    _log_handlers.append(logging.StreamHandler(sys.stderr))

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s  %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
    handlers=_log_handlers,
)
log = logging.getLogger(__name__)

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
COLOR_ERASER_IDLE = "#444444"
COLOR_ERASER_HOVER = "#FF9800"
COLOR_CLOSE_IDLE = "#333333"
COLOR_CLOSE_HOVER = "#D73A49"
COLOR_GEMINI_ON = "#43A047"
COLOR_GEMINI_OFF = "#555555"
COLOR_TEXT = "#FFFFFF"
COLOR_STATUS = "#AAAAAA"
TRANSPARENT = "magenta"


class ClaudeOverlayApp:
    """Modernes rahmenloses Overlay mit Mikrofon-, Radiergummi- und Beenden-Button."""

    BTN_RADIUS = 28
    BTN_GAP = 16
    PADDING = 12
    STATUS_HEIGHT = 22
    CLOSE_RADIUS = 10

    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self.settings.validate()

        self.recorder = AudioRecorder(
            sample_rate=self.settings.audio_sample_rate,
            channels=self.settings.audio_channels,
        )
        self.is_recording = False
        self.is_processing = False
        self.gemini_enabled = self.settings.gemini_available
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
        screen_h = self.root.winfo_screenheight()
        x = screen_w - total_w - 24
        y = screen_h - total_h - 80
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

        # ----- Radiergummi-Button (Eingabefeld leeren) -----
        eraser_cx = mic_cx + d + self.BTN_GAP
        eraser_cy = mic_cy

        self.eraser_circle = self.canvas.create_oval(
            eraser_cx - r, eraser_cy - r, eraser_cx + r, eraser_cy + r,
            fill=COLOR_ERASER_IDLE, outline="#555555", width=2,
        )

        eraser_font = tkfont.Font(family="Segoe UI Emoji", size=16)
        self.eraser_text = self.canvas.create_text(
            eraser_cx, eraser_cy, text="\U0001F9F9", font=eraser_font, fill=COLOR_TEXT,
        )

        # ----- Gemini-Toggle (kleines "G" oben links) -----
        cr = self.CLOSE_RADIUS
        gemini_cx = cr + 6
        gemini_cy = cr + 6

        gemini_fill = COLOR_GEMINI_ON if self.gemini_enabled else COLOR_GEMINI_OFF
        gemini_text_fill = COLOR_TEXT if self.gemini_enabled else "#888888"

        self.gemini_circle = self.canvas.create_oval(
            gemini_cx - cr, gemini_cy - cr, gemini_cx + cr, gemini_cy + cr,
            fill=gemini_fill, outline="",
        )

        gemini_font = tkfont.Font(family="Segoe UI", size=8, weight="bold")
        self.gemini_text = self.canvas.create_text(
            gemini_cx, gemini_cy, text="G", font=gemini_font, fill=gemini_text_fill,
        )

        # ----- Beenden-Button (kleines X oben rechts) -----
        close_cx = total_w - cr - 6
        close_cy = cr + 6

        self.close_circle = self.canvas.create_oval(
            close_cx - cr, close_cy - cr, close_cx + cr, close_cy + cr,
            fill=COLOR_CLOSE_IDLE, outline="",
        )

        close_font = tkfont.Font(family="Segoe UI", size=8, weight="bold")
        self.close_text = self.canvas.create_text(
            close_cx, close_cy, text="\u2715", font=close_font, fill="#888888",
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
        self._eraser_bbox = (eraser_cx - r, eraser_cy - r, eraser_cx + r, eraser_cy + r)
        self._gemini_bbox = (gemini_cx - cr - 2, gemini_cy - cr - 2, gemini_cx + cr + 2, gemini_cy + cr + 2)
        self._close_bbox = (close_cx - cr - 2, close_cy - cr - 2, close_cx + cr + 2, close_cy + cr + 2)

        # ----- Events -----
        self.canvas.bind("<Button-1>", self._on_click)
        self.canvas.bind("<Motion>", self._on_motion)
        self.canvas.bind("<Leave>", self._on_leave)
        self.canvas.bind("<Button-3>", self._on_drag_start)
        self.canvas.bind("<B3-Motion>", self._on_drag_motion)

        self.root.bind("<Escape>", lambda _: self._quit())

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
        if self._in_bbox(event.x, event.y, self._close_bbox):
            self._quit()
        elif self._in_bbox(event.x, event.y, self._gemini_bbox):
            self._toggle_gemini()
        elif self._in_bbox(event.x, event.y, self._mic_bbox):
            self._toggle_recording()
        elif self._in_bbox(event.x, event.y, self._eraser_bbox):
            self._clear_input()

    def _on_motion(self, event: tk.Event) -> None:
        # Close-Button reagiert immer auf Hover
        if self._in_bbox(event.x, event.y, self._close_bbox):
            self.canvas.itemconfig(self.close_circle, fill=COLOR_CLOSE_HOVER)
            self.canvas.itemconfig(self.close_text, fill=COLOR_TEXT)
        else:
            self.canvas.itemconfig(self.close_circle, fill=COLOR_CLOSE_IDLE)
            self.canvas.itemconfig(self.close_text, fill="#888888")

        # Gemini-Toggle Hover
        if self._in_bbox(event.x, event.y, self._gemini_bbox):
            self.canvas.itemconfig(self.gemini_text, fill=COLOR_TEXT)
        else:
            base = COLOR_TEXT if self.gemini_enabled else "#888888"
            self.canvas.itemconfig(self.gemini_text, fill=base)

        if self.is_recording or self.is_processing:
            return
        if self._in_bbox(event.x, event.y, self._mic_bbox):
            self.canvas.itemconfig(self.mic_circle, fill=COLOR_HOVER)
            self.canvas.itemconfig(self.eraser_circle, fill=COLOR_ERASER_IDLE)
        elif self._in_bbox(event.x, event.y, self._eraser_bbox):
            self.canvas.itemconfig(self.eraser_circle, fill=COLOR_ERASER_HOVER)
            self.canvas.itemconfig(self.mic_circle, fill=COLOR_IDLE)
        else:
            self.canvas.itemconfig(self.mic_circle, fill=COLOR_IDLE)
            self.canvas.itemconfig(self.eraser_circle, fill=COLOR_ERASER_IDLE)

    def _on_leave(self, event: tk.Event) -> None:
        self.canvas.itemconfig(self.close_circle, fill=COLOR_CLOSE_IDLE)
        self.canvas.itemconfig(self.close_text, fill="#888888")
        if not self.is_recording and not self.is_processing:
            self.canvas.itemconfig(self.mic_circle, fill=COLOR_IDLE)
            self.canvas.itemconfig(self.eraser_circle, fill=COLOR_ERASER_IDLE)

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
    # Gemini-Toggle
    # ------------------------------------------------------------------
    def _toggle_gemini(self) -> None:
        if not self.settings.gemini_available:
            self._set_status("Gemini nicht konfiguriert", COLOR_ERROR)
            self.root.after(2000, self._restore_status_after_toggle)
            return
        self.gemini_enabled = not self.gemini_enabled
        if self.gemini_enabled:
            self.canvas.itemconfig(self.gemini_circle, fill=COLOR_GEMINI_ON)
            self.canvas.itemconfig(self.gemini_text, fill=COLOR_TEXT)
            self._set_status("Gemini AN", COLOR_GEMINI_ON)
        else:
            self.canvas.itemconfig(self.gemini_circle, fill=COLOR_GEMINI_OFF)
            self.canvas.itemconfig(self.gemini_text, fill="#888888")
            self._set_status("Gemini AUS", COLOR_STATUS)
        # Status nach 2s wiederherstellen: Aufnahme-/Verarbeitungsstatus beibehalten
        self.root.after(2000, self._restore_status_after_toggle)

    def _restore_status_after_toggle(self) -> None:
        """Stellt den passenden Status wieder her, je nach aktuellem Zustand."""
        if self.is_recording:
            self._set_status("Aufnahme...", COLOR_RECORDING)
        elif self.is_processing:
            self._set_status("Verarbeite...", COLOR_PROCESSING)
        else:
            self._reset_to_idle()

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
            log.error("Aufnahme konnte nicht gestartet werden: %s", exc)

    def _stop_recording(self) -> None:
        try:
            audio_path = self.recorder.stop()
        except Exception as exc:
            self.is_recording = False
            self.canvas.itemconfig(self.mic_circle, fill=COLOR_IDLE, outline="#555555")
            self._set_status("Aufnahmefehler", COLOR_ERROR)
            log.error("Aufnahme konnte nicht beendet werden: %s", exc)
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

    def _update_status_from_thread(self, text: str) -> None:
        """Aktualisiert den Status-Text thread-sicher."""
        self.root.after(0, lambda: self._set_status(text, COLOR_PROCESSING))

    def _process_audio_pipeline(self, audio_path: Path) -> None:
        transcript = None
        try:
            self.root.after(0, lambda: self._set_status("Transkribiere...", COLOR_PROCESSING))
            transcript = transcribe_with_grok(audio_path, self.settings)

            if self.gemini_enabled:
                self.root.after(0, lambda: self._set_status("Verbessere Text...", COLOR_PROCESSING))
                result = improve_text_with_gemini(
                    transcript,
                    self.settings,
                    status_callback=self._update_status_from_thread,
                )
                final_text = result["verbesserter_text"]
            else:
                final_text = transcript

            overlay_hwnd = self._get_overlay_hwnd()
            insert_text_into_claude(final_text, overlay_hwnd=overlay_hwnd, settings=self.settings)

            self.root.after(0, lambda: self._on_pipeline_success())
        except Exception as exc:
            err_msg = str(exc)
            # Transkript sichern, damit es nicht verloren geht
            if transcript:
                self._save_failed_transcript(transcript, err_msg)
            self.root.after(0, lambda: self._on_pipeline_error(err_msg))
        finally:
            try:
                audio_path.unlink(missing_ok=True)
            except Exception:
                pass

    def _save_failed_transcript(self, transcript: str, error: str) -> None:
        """Speichert ein fehlgeschlagenes Transkript in einer Datei."""
        try:
            backup_dir = Path(__file__).resolve().parents[1] / "failed_transcripts"
            backup_dir.mkdir(exist_ok=True)
            timestamp = time.strftime("%Y%m%d_%H%M%S")
            backup_file = backup_dir / f"transcript_{timestamp}.txt"
            backup_file.write_text(
                f"Fehler: {error}\n\n"
                f"Transkript:\n{transcript}\n",
                encoding="utf-8",
            )
            log.info("Transkript gesichert: %s", backup_file)
        except Exception as save_exc:
            log.error("Transkript konnte nicht gesichert werden: %s", save_exc)

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
        log.error("Pipeline: %s", msg)
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
            clear_claude_input(overlay_hwnd=self._get_overlay_hwnd(), settings=self.settings)
            self._set_status("Feld geleert", COLOR_SUCCESS)
            self.root.after(2000, self._reset_to_idle)
        except Exception as exc:
            self._set_status("Loeschen fehlgeschlagen", COLOR_ERROR)
            log.error("Eingabefeld leeren: %s", exc)
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
    log.info("GEMINI_MODEL=%r", settings.gemini_model)
    log.info("GEMINI_THINKING_LEVEL=%r", settings.gemini_thinking_level)

    # Auf Claude Desktop warten (max 5 Minuten beim Autostart)
    if not is_claude_running(settings):
        log.info("Warte auf Claude Desktop...")
        for _ in range(60):
            time.sleep(5)
            if is_claude_running(settings):
                break
        else:
            log.info("Claude Desktop nicht gestartet. Beende.")
            return

    app = ClaudeOverlayApp(settings)
    app.run()


if __name__ == "__main__":
    main()
