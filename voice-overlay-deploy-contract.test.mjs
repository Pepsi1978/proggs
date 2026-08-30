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
  assert.match(rebuild, /pwsh -NoProfile -File \$publishScript \| Out-Host/)
  assert.match(rebuild, /\$publishExitCode = \$LASTEXITCODE/)
  assert.match(rebuild, /function Test-BuiltArtifact/)
  assert.match(rebuild, /PropertyGroup\.Version/)
  assert.match(rebuild, /recording\/status/)
  assert.match(rebuild, /for \(\$attempt = 1; \$attempt -le 20; \$attempt\+\+\)/)
  assert.match(rebuild, /Stop-ScheduledTask -TaskName \$O\.Task/)
  assert.match(rebuild, /Start-ScheduledTask -TaskName \$O\.Task/)
  assert.match(rebuild, /\$startupDeadline = \$startupStarted\.AddSeconds\(\$StartupTimeoutSeconds\)/)
})

// Poka-Yoke zum Vorfall 30.08.2026: ein Rebuild stand zehn Minuten still und
// lieferte nichts, weil zwei Warteschleifen auf einen Status-Port warteten, an
// dem niemand lauschte — der Guard 600 s, die Startup-Verifikation 10 min pro
// Versuch. Beide Wartezeiten standen stumm da, und der Test oben verlangte die
// zehn Minuten sogar ausdruecklich. Dieser Test macht den Fehlertyp
// strukturell unmoeglich: keine Minutenwartezeit ohne Port-Unterscheidung,
// und keine Warteschleife ohne Lebenszeichen.
test("no deploy path can stall silently for minutes", async () => {
  const guardPs = await read("voice-overlay-deploy-guard.ps1")
  const guardSh = await read("voice-overlay-deploy-guard.sh")
  const rebuild = await read("rebuild-overlay.ps1")

  // 1. Bevor irgendwo fail-closed gewartet wird, MUSS der Port unterscheiden,
  //    ob das Overlay den Schutz ueberhaupt anbietet.
  assert.match(guardPs, /\$PortProbe/, "PowerShell-Guard braucht die Port-Pruefung")
  assert.ok(
    guardPs.indexOf("& $PortProbe $Port") < guardPs.indexOf("das Overlay haengt"),
    "Der Port muss geprueft werden, BEVOR fail-closed abgebrochen wird",
  )
  assert.match(guardSh, /_voice_overlay_port_open/, "macOS-Guard braucht die Port-Pruefung")
  assert.match(rebuild, /PortOpen/, "Die Startup-Verifikation braucht die Port-Pruefung")

  // 2. Keine Wartefrist laenger als drei Minuten. Ein Diktat dauert kuerzer,
  //    und ein startendes Overlay ist in Sekunden oben.
  for (const [name, source] of [["rebuild-overlay.ps1", rebuild], ["voice-overlay-deploy-guard.ps1", guardPs]]) {
    for (const m of source.matchAll(/\.AddMinutes\((\d+)\)/g)) {
      assert.ok(Number(m[1]) <= 3, `${name}: Wartefrist von ${m[1]} Minuten ist zu lang`)
    }
    for (const m of source.matchAll(/(?:TimeoutSeconds|StartupTimeoutSeconds)\s*=\s*(\d+)/g)) {
      assert.ok(Number(m[1]) <= 180, `${name}: Timeout von ${m[1]} s ist zu lang`)
    }
  }
  for (const m of guardSh.matchAll(/timeout_seconds="\$\{\d+:-(\d+)\}"/g)) {
    assert.ok(Number(m[1]) <= 180, `voice-overlay-deploy-guard.sh: Timeout von ${m[1]} s ist zu lang`)
  }

  // 3. Jede Warteschleife meldet sich unterwegs, statt stumm dazustehen.
  assert.match(guardPs, /warte seit \$elapsed s auf \$waitingFor/, "PowerShell-Guard braucht Fortschrittsausgabe")
  assert.match(guardSh, /warte seit \$\(\(SECONDS - started\)\)s/, "macOS-Guard braucht Fortschrittsausgabe")
  assert.match(rebuild, /seit \$\(\[int\]\$waited\) s gestartet/, "Startup-Verifikation braucht Fortschrittsausgabe")

  // 4. Ein Overlay ohne Status-Server blockiert nichts — es wird ueber die
  //    Startzeit verifiziert, statt auf einen Endpunkt zu warten, der nie kommt.
  assert.match(rebuild, /VERIFIZIERT ueber die Startzeit/)
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
