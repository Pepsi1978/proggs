// TauriDesktopTemplates.swift
// Alle Datei-Templates fuer ein Tauri-v2-Projekt: Rust-Backend + Svelte-Frontend.
// Die Templates nutzen durchweg Raw-Strings, weil JS/Rust/JSON/HTML diverse
// Delimiter enthalten, die sich mit Swifts Multi-Line-Syntax beissen wuerden.

import Foundation

public enum TauriDesktopTemplates {

    /// Rust-crate-Namen brauchen Underscores statt Hyphens.
    public static func rustCrateName(from slug: String) -> String {
        slug.replacingOccurrences(of: "-", with: "_")
    }

    // MARK: - Frontend

    public static func packageJson(slug: String) -> String {
        #"""
        {
          "name": "\#(slug)",
          "version": "0.1.0",
          "description": "Harness-Forge-generated Tauri app",
          "type": "module",
          "scripts": {
            "dev": "vite",
            "build": "vite build",
            "preview": "vite preview",
            "tauri": "tauri"
          },
          "dependencies": {
            "@tauri-apps/api": "^2.0.0"
          },
          "devDependencies": {
            "@sveltejs/vite-plugin-svelte": "^4.0.0",
            "@tauri-apps/cli": "^2.0.0",
            "svelte": "^5.0.0",
            "vite": "^5.4.0"
          }
        }
        """#
    }

    public static func viteConfig() -> String {
        #"""
        import { defineConfig } from "vite";
        import { svelte } from "@sveltejs/vite-plugin-svelte";

        // Tauri erwartet, dass der Dev-Server auf Port 1420 laeuft.
        // https://v2.tauri.app/start/frontend/vite/

        export default defineConfig({
          plugins: [svelte()],
          clearScreen: false,
          server: {
            port: 1420,
            strictPort: true,
          },
          envPrefix: ["VITE_", "TAURI_"],
          build: {
            target: ["es2021", "chrome100", "safari13"],
            minify: !process.env.TAURI_DEBUG ? "esbuild" : false,
            sourcemap: !!process.env.TAURI_DEBUG,
          },
        });
        """#
    }

    public static func svelteConfig() -> String {
        #"""
        import { vitePreprocess } from "@sveltejs/vite-plugin-svelte";

        export default {
          preprocess: vitePreprocess(),
        };
        """#
    }

    public static func indexHtml(slug: String) -> String {
        #"""
        <!doctype html>
        <html lang="de">
          <head>
            <meta charset="UTF-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0" />
            <title>\#(slug)</title>
          </head>
          <body>
            <div id="app"></div>
            <script type="module" src="/src/main.js"></script>
          </body>
        </html>
        """#
    }

    public static func mainJs() -> String {
        #"""
        import { mount } from "svelte";
        import App from "./App.svelte";
        import "./styles.css";

        const app = mount(App, { target: document.getElementById("app") });
        export default app;
        """#
    }

    public static func stylesCss() -> String {
        #"""
        :root {
          --bg: #1c1c1e;
          --fg: #f5f5f7;
          --accent: #0a84ff;
          --border: #3a3a3c;
        }
        * { box-sizing: border-box; }
        html, body, #app { height: 100%; margin: 0; font-family: -apple-system, system-ui, sans-serif; }
        body { background: var(--bg); color: var(--fg); }
        .container { padding: 24px; max-width: 800px; margin: 0 auto; }
        h1 { font-size: 28px; margin: 0 0 16px; }
        textarea {
          width: 100%; min-height: 120px; padding: 12px;
          font: inherit; color: inherit;
          background: #2c2c2e; border: 1px solid var(--border); border-radius: 8px;
        }
        button {
          padding: 10px 20px; font: inherit;
          background: var(--accent); color: white; border: 0; border-radius: 8px;
          cursor: pointer; margin-top: 12px;
        }
        button:disabled { opacity: 0.5; cursor: not-allowed; }
        pre {
          background: #2c2c2e; padding: 16px; border-radius: 8px;
          white-space: pre-wrap; word-wrap: break-word; margin-top: 16px;
        }
        """#
    }

    public static func appSvelte(slug: String, taskDescription: String) -> String {
        #"""
        <script>
          import { invoke } from "@tauri-apps/api/core";

          let userInput = $state("");
          let response = $state("");
          let loading = $state(false);
          let errorMsg = $state("");

          async function ask() {
            loading = true;
            errorMsg = "";
            response = "";
            try {
              response = await invoke("chat_with_llm", { userMessage: userInput });
            } catch (err) {
              errorMsg = String(err);
            } finally {
              loading = false;
            }
          }
        </script>

        <div class="container">
          <h1>\#(slug)</h1>
          <p>\#(taskDescription)</p>

          <textarea bind:value={userInput} placeholder="Deine Frage oder Aufgabe…"></textarea>
          <button onclick={ask} disabled={loading || !userInput.trim()}>
            {loading ? "Wird verarbeitet…" : "Fragen"}
          </button>

          {#if errorMsg}
            <pre style="color: #ff453a">Fehler: {errorMsg}</pre>
          {/if}
          {#if response}
            <pre>{response}</pre>
          {/if}
        </div>
        """#
    }

    // MARK: - Rust-Backend (src-tauri/)

    public static func cargoToml(slug: String, crateName: String) -> String {
        #"""
        [package]
        name = "\#(crateName)"
        version = "0.1.0"
        description = "Harness-Forge-generated Tauri app"
        edition = "2021"

        [lib]
        name = "\#(crateName)_lib"
        crate-type = ["staticlib", "cdylib", "rlib"]

        [build-dependencies]
        tauri-build = { version = "2", features = [] }

        [dependencies]
        serde_json = "1"
        serde = { version = "1", features = ["derive"] }
        tauri = { version = "2", features = [] }
        reqwest = { version = "0.12", features = ["json", "rustls-tls"], default-features = false }
        tokio = { version = "1", features = ["full"] }

        [profile.release]
        panic = "abort"
        codegen-units = 1
        lto = true
        opt-level = "s"
        strip = true
        """#
    }

    public static func buildRs() -> String {
        #"""
        fn main() {
            tauri_build::build()
        }
        """#
    }

    public static func tauriConfJson(slug: String) -> String {
        #"""
        {
          "$schema": "https://schema.tauri.app/config/2",
          "productName": "\#(slug)",
          "version": "0.1.0",
          "identifier": "com.harness-forge.\#(slug)",
          "build": {
            "beforeDevCommand": "npm run dev",
            "devUrl": "http://localhost:1420",
            "beforeBuildCommand": "npm run build",
            "frontendDist": "../dist"
          },
          "app": {
            "windows": [
              {
                "title": "\#(slug)",
                "width": 900,
                "height": 650,
                "resizable": true
              }
            ],
            "security": {
              "csp": null
            }
          },
          "bundle": {
            "active": true,
            "targets": "all",
            "category": "DeveloperTool"
          }
        }
        """#
    }

    public static func mainRs() -> String {
        #"""
        // Tauri v2 Entry-Point. Die App-Logik liegt in lib.rs.
        #![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

        fn main() {
            harness_app_lib::run()
        }
        """#
    }

    /// Baut lib.rs mit korrekt eingefuegtem crateName (der Lib-Name-Prefix).
    public static func libRs(crateName: String) -> String {
        #"""
        // Tauri-v2-Library-Target. Enthaelt die `chat_with_llm`-Command-Implementierung.
        use serde::{Deserialize, Serialize};

        #[derive(Serialize, Deserialize)]
        struct AnthropicRequest {
            model: String,
            max_tokens: u32,
            system: String,
            messages: Vec<Msg>,
        }

        #[derive(Serialize, Deserialize)]
        struct Msg {
            role: String,
            content: String,
        }

        /// Tauri-Command: ruft Claude (Anthropic) mit einer Benutzer-Nachricht auf.
        /// API-Key kommt aus der Umgebungsvariable ANTHROPIC_API_KEY.
        #[tauri::command]
        async fn chat_with_llm(user_message: String) -> Result<String, String> {
            let api_key = std::env::var("ANTHROPIC_API_KEY")
                .map_err(|_| "ANTHROPIC_API_KEY nicht gesetzt".to_string())?;

            let system_prompt = include_str!("../../SYSTEM_PROMPT.md");

            let req = AnthropicRequest {
                model: "claude-opus-4-7".to_string(),
                max_tokens: 2048,
                system: system_prompt.to_string(),
                messages: vec![Msg {
                    role: "user".to_string(),
                    content: user_message,
                }],
            };

            let client = reqwest::Client::new();
            let response = client
                .post("https://api.anthropic.com/v1/messages")
                .header("x-api-key", api_key)
                .header("anthropic-version", "2023-06-01")
                .header("content-type", "application/json")
                .json(&req)
                .send()
                .await
                .map_err(|e| format!("Netzwerk-Fehler: {}", e))?;

            if !response.status().is_success() {
                let status = response.status();
                let body = response.text().await.unwrap_or_default();
                return Err(format!("HTTP {}: {}", status, body));
            }

            let json: serde_json::Value = response
                .json()
                .await
                .map_err(|e| format!("JSON-Parse-Fehler: {}", e))?;
            let text = json["content"][0]["text"]
                .as_str()
                .unwrap_or("<keine Antwort>")
                .to_string();
            Ok(text)
        }

        #[cfg_attr(mobile, tauri::mobile_entry_point)]
        pub fn run() {
            tauri::Builder::default()
                .invoke_handler(tauri::generate_handler![chat_with_llm])
                .run(tauri::generate_context!())
                .expect("Error while running tauri application");
        }
        """#
    }

    // MARK: - Docs

    public static func readmeMd(slug: String, taskDescription: String, decomposition: TaskDecomposition) -> String {
        """
        # \(slug)

        > Tauri-v2-Desktop-App, automatisch generiert von Harness Forge.
        > Rust-Backend + Svelte-5-Frontend.

        ## Aufgabe

        \(taskDescription)

        ## Voraussetzungen

        - Node.js 20+
        - Rust (via rustup)
        - Tauri-Voraussetzungen je Betriebssystem:
          - macOS: Xcode Command Line Tools
          - Windows: MS Visual Studio C++ Build Tools, WebView2
          - Linux: `webkit2gtk`, `libayatana-appindicator3-dev`

        ## Installation

        ```bash
        cd \(slug)
        npm install
        ```

        ## Setup: API-Key

        Vor dem Start:

        ```bash
        export ANTHROPIC_API_KEY="sk-ant-..."
        ```

        ## Entwicklung

        ```bash
        npm run tauri dev
        ```

        ## Release-Build

        ```bash
        npm run tauri build
        ```

        Das Ergebnis liegt in `src-tauri/target/release/bundle/`.

        ## Struktur

        ```
        \(slug)/
        ├── package.json, vite.config.js, svelte.config.js
        ├── index.html
        ├── src/
        │   ├── main.js, App.svelte, styles.css
        ├── src-tauri/
        │   ├── Cargo.toml, build.rs, tauri.conf.json
        │   └── src/main.rs, src/lib.rs
        ├── SYSTEM_PROMPT.md
        └── README.md
        ```

        ## Kontext

        - Zielnutzer: `\(decomposition.targetUsers.rawValue)`
        - Begruendung: \(decomposition.rationale)

        ---

        *Generiert von Harness Forge.*
        """
    }

    public static func systemPromptMd(
        taskDescription: String,
        slug: String,
        decomposition: TaskDecomposition
    ) -> String {
        PromptTemplate.render(
            taskDescription: taskDescription,
            slug: slug,
            decomposition: decomposition
        )
    }

    public static func gitignore() -> String {
        #"""
        # Dependencies
        node_modules/

        # Build-Outputs
        dist/
        build/

        # Tauri-Build
        src-tauri/target/
        src-tauri/gen/

        # IDE
        .vscode/
        .idea/

        # OS
        .DS_Store
        Thumbs.db

        # Env
        .env
        .env.local
        """#
    }
}
