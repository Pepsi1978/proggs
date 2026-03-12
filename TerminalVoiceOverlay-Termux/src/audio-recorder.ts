import { execSync, spawn, type ChildProcess } from "node:child_process";
import { existsSync, unlinkSync } from "node:fs";
import { join } from "node:path";
import { tmpdir } from "node:os";

export class AudioRecorder {
  private process: ChildProcess | null = null;
  private filePath: string = "";
  private _isRecording = false;

  get isRecording(): boolean {
    return this._isRecording;
  }

  start(): string {
    const filename = `recording_${Date.now()}.wav`;
    this.filePath = join(
      process.env["TMPDIR"] || tmpdir(),
      filename
    );

    // Stop any previous recording
    try {
      execSync("termux-microphone-record -q", { stdio: "ignore" });
    } catch {
      // Ignore — no recording was running
    }

    // Start recording: 16kHz mono WAV
    this.process = spawn("termux-microphone-record", [
      "-f", this.filePath,
      "-l", "0",        // unlimited duration
      "-r", "16000",     // 16kHz sample rate
      "-c", "1",         // mono
      "-e", "pcm_16bit", // 16-bit PCM
    ], { stdio: "ignore" });

    this._isRecording = true;
    return this.filePath;
  }

  stop(): string | null {
    if (!this._isRecording) return null;

    this._isRecording = false;

    // Signal termux-microphone-record to stop
    try {
      execSync("termux-microphone-record -q", { stdio: "ignore" });
    } catch {
      // Ignore
    }

    if (this.process) {
      this.process.kill();
      this.process = null;
    }

    // Small delay to let the file flush
    execSync("sleep 0.3");

    if (existsSync(this.filePath)) {
      return this.filePath;
    }
    return null;
  }

  cleanup(filePath: string): void {
    try {
      if (existsSync(filePath)) unlinkSync(filePath);
    } catch {
      // Ignore
    }
  }
}
