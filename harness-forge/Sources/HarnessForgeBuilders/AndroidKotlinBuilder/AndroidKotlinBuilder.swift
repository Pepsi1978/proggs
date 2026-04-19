// AndroidKotlinBuilder.swift
// Generiert ein minimales Android-Projekt mit Compose + Material 3 + OkHttp.

import Foundation

public struct AndroidKotlinBuilder: HarnessBuilder {

    public let type: HarnessType = .androidKotlin

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

        let androidPkg = AndroidKotlinTemplates.androidPackage(from: slug)
        let pkgPath = AndroidKotlinTemplates.packagePath(from: androidPkg)

        let appDir = projectDir.appendingPathComponent("app")
        let manifestDir = appDir.appendingPathComponent("src/main")
        let javaDir = manifestDir.appendingPathComponent("java/\(pkgPath)")
        let resValuesDir = manifestDir.appendingPathComponent("res/values")

        for dir in [appDir, manifestDir, javaDir, resValuesDir] {
            do {
                try FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
            } catch {
                throw BuilderError.cannotCreateDirectory(dir, underlyingError: error.localizedDescription)
            }
        }

        let files: [(URL, String)] = [
            // Root
            (projectDir.appendingPathComponent("build.gradle.kts"),
             AndroidKotlinTemplates.rootBuildGradle()),
            (projectDir.appendingPathComponent("settings.gradle.kts"),
             AndroidKotlinTemplates.settingsGradle(slug: slug)),
            (projectDir.appendingPathComponent("gradle.properties"),
             AndroidKotlinTemplates.gradleProperties()),

            // app-Modul
            (appDir.appendingPathComponent("build.gradle.kts"),
             AndroidKotlinTemplates.appBuildGradle(pkg: androidPkg)),
            (manifestDir.appendingPathComponent("AndroidManifest.xml"),
             AndroidKotlinTemplates.androidManifest(slug: slug)),

            // Kotlin-Quellen
            (javaDir.appendingPathComponent("MainActivity.kt"),
             AndroidKotlinTemplates.mainActivityKt(pkg: androidPkg)),
            (javaDir.appendingPathComponent("MainScreen.kt"),
             AndroidKotlinTemplates.mainScreenKt(pkg: androidPkg, taskDescription: taskDescription)),
            (javaDir.appendingPathComponent("LLMClient.kt"),
             AndroidKotlinTemplates.llmClientKt(pkg: androidPkg)),

            // Ressourcen
            (resValuesDir.appendingPathComponent("strings.xml"),
             AndroidKotlinTemplates.stringsXml(slug: slug)),
            (resValuesDir.appendingPathComponent("themes.xml"),
             AndroidKotlinTemplates.themesXml()),

            // Docs
            (projectDir.appendingPathComponent("README.md"),
             AndroidKotlinTemplates.readmeMd(slug: slug, taskDescription: taskDescription, decomposition: decomposition)),
            (projectDir.appendingPathComponent("SYSTEM_PROMPT.md"),
             AndroidKotlinTemplates.systemPromptMd(taskDescription: taskDescription, slug: slug, decomposition: decomposition)),
            (projectDir.appendingPathComponent(".gitignore"),
             AndroidKotlinTemplates.gitignore()),
        ]

        for (url, content) in files {
            try BuilderHelpers.writeFile(at: url, content: content)
        }

        return BuildOutcome(
            harnessType: type,
            slug: slug,
            projectDirectory: projectDir,
            createdFiles: files.map(\.0),
            summary: """
            Android-Kotlin-Harness '\(slug)' erzeugt.
            - Package: \(androidPkg)
            - \(files.count) Dateien, Compose + Material 3
            - Oeffne in Android Studio → Gradle-Sync → Run
            """
        )
    }
}
