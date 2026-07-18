// Notifier Completion Guard v1.1.0 - 18.07.2026, 12:54 Uhr

import { spawn } from "node:child_process"
import { dirname } from "node:path"
import { fileURLToPath } from "node:url"
import { createNotifierCompletionGuard } from "./lib/notifier-completion-guard-core.js"

export default async function NotifierCompletionGuard(input) {
  const { default: NotifierPlugin } = await import("@mohak34/opencode-notifier")
  const guardedNotifier = await createNotifierCompletionGuard(input, NotifierPlugin)
  const configDirectory = dirname(dirname(fileURLToPath(import.meta.url)))
  const updater = fileURLToPath(new URL("./notifier-auto-updater.mjs", import.meta.url))
  try {
    const child = spawn("node", [updater, "--config-dir", configDirectory], {
      detached: true,
      stdio: "ignore",
      windowsHide: true,
    })
    child.on("error", async (error) => {
      try {
        await input.client?.app?.log?.({
          body: { service: "notifier-auto-updater", level: "warn", message: error.message },
        })
      } catch {
        // An unavailable updater must never affect the notifier or the OpenCode session.
      }
    })
    child.unref()
  } catch (error) {
    try {
      await input.client?.app?.log?.({
        body: { service: "notifier-auto-updater", level: "warn", message: error.message },
      })
    } catch {
      // An unavailable updater must never affect the notifier or the OpenCode session.
    }
  }
  return guardedNotifier
}
