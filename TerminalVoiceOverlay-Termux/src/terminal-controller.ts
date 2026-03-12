import { execSync, execFileSync } from "node:child_process";

export class TerminalController {
  static setClipboard(text: string): void {
    try {
      execSync("termux-clipboard-set", {
        input: text,
        stdio: ["pipe", "ignore", "ignore"],
      });
    } catch {
      // Fallback: ignore clipboard errors
    }
  }

  static getClipboard(): string {
    try {
      return execSync("termux-clipboard-get", { encoding: "utf-8" }).trim();
    } catch {
      return "";
    }
  }

  static notify(title: string, content: string): void {
    try {
      execFileSync("termux-notification", [
        "--title", title,
        "--content", content,
        "--priority", "high",
        "--vibrate", "200",
        "--id", "voice-overlay",
      ], { stdio: "ignore" });
    } catch {
      // Ignore notification errors
    }
  }

  static vibrate(ms = 200): void {
    try {
      execFileSync("termux-vibrate", ["-d", String(Math.max(0, Math.floor(ms)))], {
        stdio: "ignore",
      });
    } catch {
      // Ignore
    }
  }

  static toast(text: string): void {
    try {
      execFileSync("termux-toast", ["-g", "middle", text], {
        stdio: "ignore",
      });
    } catch {
      // Ignore
    }
  }
}
