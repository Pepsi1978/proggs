import Foundation

// Prueft, dass die vom Launcher erzeugten Startskripte gueltiges zsh sind.
// Gegenstueck zu den PowerShell-Quell-Waechtern unter Windows (OpenLauncher/tests/*.ps1).
//
// Besonders heikel: in das OpenCode-Skript ist Python eingebettet (LM-Studio-Vorbereitung) -
// einmal per `python3 -c "..."` und einmal als Heredoc. Beides bricht, sobald die Zeilen
// eingerueckt landen: `python3 -c` wirft dann IndentationError, und ein Heredoc-Ende mit
// fuehrenden Leerzeichen wird gar nicht als Ende erkannt.

/// Schneidet die eingebetteten Python-Abschnitte aus dem Startskript heraus:
/// den `python3 -c "..."`-Block und den `<<'PYEOF' ... PYEOF`-Heredoc.
func extractPythonBlocks(_ script: String) -> [String] {
    var blocks: [String] = []
    let lines = script.components(separatedBy: "\n")

    var current: [String] = []
    var inHeredoc = false
    var inMinusC = false
    for line in lines {
        if inHeredoc {
            if line == "PYEOF" { blocks.append(current.joined(separator: "\n")); current = []; inHeredoc = false }
            else { current.append(line) }
            continue
        }
        if inMinusC {
            if line.hasPrefix("\" ") || line == "\"" {
                blocks.append(current.joined(separator: "\n")); current = []; inMinusC = false
            } else { current.append(line) }
            continue
        }
        if line.contains("<<'PYEOF'") { inHeredoc = true; continue }
        if line.contains("python3 -c \"") { inMinusC = true; continue }
    }
    return blocks
}

@MainActor
func run() -> Int32 {
    var failures = 0

    func check(_ name: String, _ path: String) {
        let result = Shell.run("/bin/zsh", ["-n", path], timeout: 15)
        if result.exitCode == 0 {
            print("  ✅ \(name): zsh-Syntax in Ordnung")
        } else {
            failures += 1
            print("  ❌ \(name): zsh meldet einen Syntaxfehler")
            print(result.stderr.trimmingCharacters(in: .whitespacesAndNewlines))
        }
    }

    let color = TerminalTabColor(name: "green", hex: "#13A10E")

    do {
        let claude = try OpenLauncherService.buildClaudeCodeStartScript(
            modelId: "claude-opus-5[1m]", workDir: NSHomeDirectory() + "/proggs",
            effortLevel: "xhigh", colorName: "green",
            claudeConfigDir: NSHomeDirectory() + "/proggs/OpenLauncher/Profiles/ClaudeCodeMac/standard",
            tabColor: color, title: "Claude-green-xhigh")
        check("Claude Code", claude)
        try? FileManager.default.removeItem(atPath: claude)

        // Drei Varianten: normal, mit NVIDIA-Schluesselblock, mit LM-Studio-Vorbereitung
        // (nur dort steckt das eingebettete Python).
        for (label, model) in [("OpenCode (OpenRouter)", "openrouter/z-ai/glm-5.2"),
                               ("OpenCode (NVIDIA)", "nvidia/nvidia/nemotron-3-nano-30b-a3b"),
                               ("OpenCode (LM Studio)", "lmstudio/mistralai/devstral-small-2-2512")] {
            let script = try OpenLauncherService.buildOpenCodeStartScript(
                modelString: model, workDir: NSHomeDirectory() + "/proggs",
                thinkingLevel: "high", profileConfigPath: "/tmp/opencode-profile.json",
                workMode: "normal", tabColor: color, title: "OpenCode-green-high")
            check(label, script)
            try? FileManager.default.removeItem(atPath: script)
        }
    } catch {
        print("  ❌ Skript konnte nicht erzeugt werden: \(error.localizedDescription)")
        failures += 1
    }

    // Das LM-Studio-Skript traegt eingebettetes Python (einmal per `python3 -c`, einmal als
    // Heredoc). Beides bricht still, sobald die Zeilen eingerueckt herauskommen - deshalb wird der
    // Python-Anteil hier getrennt gegen den Compiler gehalten.
    if let lms = try? OpenLauncherService.buildOpenCodeStartScript(
        modelString: "lmstudio/mistralai/devstral-small-2-2512",
        workDir: NSHomeDirectory(), thinkingLevel: nil,
        profileConfigPath: "/tmp/opencode-profile.json", workMode: "frei",
        tabColor: color, title: "t") {
        let text = (try? String(contentsOfFile: lms, encoding: .utf8)) ?? ""
        try? FileManager.default.removeItem(atPath: lms)

        // a) Heredoc-Ende muss am Zeilenanfang stehen, sonst endet das Heredoc nie.
        if text.contains("\nPYEOF\n") {
            print("  ✅ Heredoc-Ende (PYEOF) steht am Zeilenanfang")
        } else {
            failures += 1
            print("  ❌ Heredoc-Ende (PYEOF) ist eingerueckt - das Heredoc wuerde nie enden")
        }

        // b) Beide Python-Bloecke muessen fuer sich uebersetzbar sein.
        for (index, block) in extractPythonBlocks(text).enumerated() {
            let file = NSTemporaryDirectory() + "openlauncher-pytest-\(index).py"
            try? block.write(toFile: file, atomically: true, encoding: .utf8)
            let result = Shell.run("/usr/bin/env", ["python3", "-m", "py_compile", file], timeout: 20)
            if result.exitCode == 0 {
                print("  ✅ eingebettetes Python #\(index + 1): uebersetzbar")
            } else {
                failures += 1
                print("  ❌ eingebettetes Python #\(index + 1): \(result.stderr.trimmingCharacters(in: .whitespacesAndNewlines))")
            }
            try? FileManager.default.removeItem(atPath: file)
        }
    }

    print(failures == 0 ? "\nAlle Startskripte sind syntaktisch gueltig."
                        : "\n\(failures) Startskript(e) fehlerhaft.")
    return failures == 0 ? 0 : 1
}

exit(MainActor.assumeIsolated { run() })
