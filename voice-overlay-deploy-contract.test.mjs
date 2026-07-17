import assert from "node:assert/strict"
import { readFile } from "node:fs/promises"
import test from "node:test"

const root = new URL("./", import.meta.url)
const read = (path) => readFile(new URL(path, root), "utf8")

test("all Windows build and update entrypoints reserve before build or kill", async () => {
  for (const path of [
    "TerminalVoiceOverlay-Windows/publish.ps1",
    "TerminalVoiceOverlay-Windows/update.ps1",
    "ClaudeVoiceOverlay-Windows/publish.ps1",
    "ClaudeVoiceOverlay-Windows/update.ps1",
  ]) {
    const source = await read(path)
    assert.match(source, /voice-overlay-deploy-guard\.ps1/)
    assert.ok(source.indexOf("Enter-VoiceOverlayDeploymentWindow") < source.indexOf("dotnet publish"), path)
    assert.match(source, /Exit-VoiceOverlayDeploymentWindow/)
    if (path.endsWith("update.ps1")) {
      assert.ok(source.match(/Enter-VoiceOverlayDeploymentWindow/g)?.length >= 2, path)
    }
  }

  const rebuild = await read("rebuild-overlay.ps1")
  assert.ok(rebuild.indexOf("$reservationHeld = Wait-OverlayIdle") < rebuild.indexOf("Stop-Overlay -O $O"))
  assert.match(rebuild, /Exit-VoiceOverlayDeploymentWindow/)
  assert.doesNotMatch(rebuild, /fahre trotzdem fort/)
})

test("both Windows apps atomically reserve idle state and block new recordings", async () => {
  for (const base of ["TerminalVoiceOverlay-Windows", "ClaudeVoiceOverlay-Windows"]) {
    const server = await read(`${base}/Services/AutoEnterStatusServer.cs`)
    const window = await read(`${base}/Views/OverlayWindow.xaml.cs`)
    assert.match(server, /\/deployment\/prepare/)
    assert.match(server, /\/deployment\/release/)
    assert.match(window, /getBusyState: IsVoicePipelineBusy/)
    assert.match(window, /tryBeginDeployment: TryBeginDeployment/)
    assert.ok(window.match(/if \(_deploymentPending\)/g)?.length >= 2, base)
  }
})

test("both macOS apps use the same reservation contract before swiftc", async () => {
  for (const [base, app, port] of [
    ["TerminalVoiceOverlay-macOS", "TerminalVoiceOverlay", "5723"],
    ["ClaudeCodexVoiceOverlay-macOS", "ClaudeCodexVoiceOverlay", "5724"],
  ]) {
    const build = await read(`${base}/build.sh`)
    const server = await read(`${base}/${app}/AutoEnterStatusServer.swift`)
    const delegate = await read(`${base}/${app}/AppDelegate.swift`)
    assert.ok(build.indexOf("reserve_voice_overlay_deployment") < build.indexOf("swiftc"), base)
    assert.match(build, new RegExp(`release_voice_overlay_deployment \\"\\$APP_NAME\\" ${port}`))
    assert.match(server, /\/deployment\/prepare/)
    assert.match(delegate, /guard !deploymentPending/)
  }
})
