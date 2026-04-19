// swift-tools-version: 6.0
// Harness Forge — Meta-Harness-Builder fuer macOS 14+.
//
// Modul-Topologie:
//
//   ForgeCLI (executable) ─┐
//                          ├─► HarnessForgeBuilders (library)
//   HarnessForgeApp (exe) ─┘          │
//                                     ▼
//                         HarnessForgeLayers (library)
//                                     │
//                                     ▼
//                         HarnessForgeCore (library)
//
// Jede Bibliothek ist einzeln testbar und austauschbar (Build-to-Delete-Prinzip).

import PackageDescription

let package = Package(
    name: "HarnessForge",
    platforms: [
        .macOS(.v14),
    ],
    products: [
        // Bibliotheken — wiederverwendbar in Drittprojekten
        .library(name: "HarnessForgeCore", targets: ["HarnessForgeCore"]),
        .library(name: "HarnessForgeLayers", targets: ["HarnessForgeLayers"]),
        .library(name: "HarnessForgeBuilders", targets: ["HarnessForgeBuilders"]),
        // Ausfuehrbare Programme
        .executable(name: "forge", targets: ["ForgeCLI"]),
        .executable(name: "HarnessForgeApp", targets: ["HarnessForgeApp"]),
    ],
    dependencies: [
        // Apple-offiziell: CLI-Argument-Parsing
        .package(url: "https://github.com/apple/swift-argument-parser", from: "1.5.0"),
        // Apple-offiziell: Strukturiertes Logging
        .package(url: "https://github.com/apple/swift-log", from: "1.6.0"),
    ],
    targets: [
        // MARK: - Libraries
        .target(
            name: "HarnessForgeCore",
            dependencies: [
                .product(name: "Logging", package: "swift-log"),
            ],
            path: "Sources/HarnessForgeCore",
            swiftSettings: [
                .enableExperimentalFeature("StrictConcurrency"),
            ]
        ),
        .target(
            name: "HarnessForgeLayers",
            dependencies: [
                "HarnessForgeCore",
                .product(name: "Logging", package: "swift-log"),
            ],
            path: "Sources/HarnessForgeLayers",
            swiftSettings: [
                .enableExperimentalFeature("StrictConcurrency"),
            ]
        ),
        .target(
            name: "HarnessForgeBuilders",
            dependencies: [
                "HarnessForgeCore",
                "HarnessForgeLayers",
                .product(name: "Logging", package: "swift-log"),
            ],
            path: "Sources/HarnessForgeBuilders",
            swiftSettings: [
                .enableExperimentalFeature("StrictConcurrency"),
            ]
        ),

        // MARK: - Executables
        .executableTarget(
            name: "ForgeCLI",
            dependencies: [
                "HarnessForgeCore",
                "HarnessForgeLayers",
                "HarnessForgeBuilders",
                .product(name: "ArgumentParser", package: "swift-argument-parser"),
            ],
            path: "Sources/ForgeCLI"
        ),
        .executableTarget(
            name: "HarnessForgeApp",
            dependencies: [
                "HarnessForgeCore",
                "HarnessForgeLayers",
                "HarnessForgeBuilders",
            ],
            path: "Sources/HarnessForgeApp"
        ),

        // MARK: - Tests (Swift Testing, nicht XCTest)
        .testTarget(
            name: "HarnessForgeCoreTests",
            dependencies: ["HarnessForgeCore"],
            path: "Tests/HarnessForgeCoreTests"
        ),
        .testTarget(
            name: "HarnessForgeLayersTests",
            dependencies: ["HarnessForgeLayers"],
            path: "Tests/HarnessForgeLayersTests"
        ),
        .testTarget(
            name: "HarnessForgeBuildersTests",
            dependencies: ["HarnessForgeBuilders"],
            path: "Tests/HarnessForgeBuildersTests"
        ),
    ]
)
