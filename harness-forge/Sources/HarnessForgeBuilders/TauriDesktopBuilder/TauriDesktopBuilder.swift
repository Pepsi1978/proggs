// TauriDesktopBuilder.swift
// Generiert ein komplettes Tauri-v2-Projekt mit Rust-Backend und Svelte-5-Frontend.

import Foundation

public struct TauriDesktopBuilder: HarnessBuilder {

    public let type: HarnessType = .tauriDesktop

    public init() {}

    public func build(
        taskDescription: String,
        slug: String,
        decomposition: TaskDecomposition,
        outputParentDirectory: URL
    ) async throws -> BuildOutcome {
        let projectDir = try BuilderHelpers.createProjectDirectory(
            parent: outputParentDirectory,
            slug: slug
        )
        let srcDir = projectDir.appendingPathComponent("src")
        let tauriDir = projectDir.appendingPathComponent("src-tauri")
        let tauriSrcDir = tauriDir.appendingPathComponent("src")

        for dir in [srcDir, tauriDir, tauriSrcDir] {
            do {
                try FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
            } catch {
                throw BuilderError.cannotCreateDirectory(dir, underlyingError: error.localizedDescription)
            }
        }

        let crateName = TauriDesktopTemplates.rustCrateName(from: slug)

        let files: [(URL, String)] = [
            // Frontend
            (projectDir.appendingPathComponent("package.json"),
             TauriDesktopTemplates.packageJson(slug: slug)),
            (projectDir.appendingPathComponent("vite.config.js"),
             TauriDesktopTemplates.viteConfig()),
            (projectDir.appendingPathComponent("svelte.config.js"),
             TauriDesktopTemplates.svelteConfig()),
            (projectDir.appendingPathComponent("index.html"),
             TauriDesktopTemplates.indexHtml(slug: slug)),
            (srcDir.appendingPathComponent("main.js"),
             TauriDesktopTemplates.mainJs()),
            (srcDir.appendingPathComponent("App.svelte"),
             TauriDesktopTemplates.appSvelte(slug: slug, taskDescription: taskDescription)),
            (srcDir.appendingPathComponent("styles.css"),
             TauriDesktopTemplates.stylesCss()),

            // Rust-Backend
            (tauriDir.appendingPathComponent("Cargo.toml"),
             TauriDesktopTemplates.cargoToml(slug: slug, crateName: crateName)),
            (tauriDir.appendingPathComponent("build.rs"),
             TauriDesktopTemplates.buildRs()),
            (tauriDir.appendingPathComponent("tauri.conf.json"),
             TauriDesktopTemplates.tauriConfJson(slug: slug)),
            (tauriSrcDir.appendingPathComponent("main.rs"),
             TauriDesktopTemplates.mainRs()),
            (tauriSrcDir.appendingPathComponent("lib.rs"),
             TauriDesktopTemplates.libRs(crateName: crateName)),

            // Docs
            (projectDir.appendingPathComponent("README.md"),
             TauriDesktopTemplates.readmeMd(slug: slug, taskDescription: taskDescription, decomposition: decomposition)),
            (projectDir.appendingPathComponent("SYSTEM_PROMPT.md"),
             TauriDesktopTemplates.systemPromptMd(taskDescription: taskDescription, slug: slug, decomposition: decomposition)),
            (projectDir.appendingPathComponent(".gitignore"),
             TauriDesktopTemplates.gitignore()),
        ]

        for (url, content) in files {
            try BuilderHelpers.writeFile(at: url, content: content)
        }

        let totalBytes = files.reduce(0) { $0 + $1.1.utf8.count }

        return BuildOutcome(
            harnessType: type,
            slug: slug,
            projectDirectory: projectDir,
            createdFiles: files.map(\.0),
            summary: """
            Tauri-Desktop-Harness '\(slug)' erzeugt.
            - Rust-Crate: \(crateName)
            - 15 Dateien, \(totalBytes) Bytes total
            - Setup: `cd \(slug) && npm install`
            - Entwicklung: `npm run tauri dev`
            """
        )
    }
}
