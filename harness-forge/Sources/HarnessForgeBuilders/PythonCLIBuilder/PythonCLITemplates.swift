// PythonCLITemplates.swift
// Alle Datei-Templates des generierten Python-CLI-Harness. Reine String-Funktionen.
// Raw-Strings (#"""..."""#) werden dort verwendet, wo Python-Docstrings """..."""
// sonst mit Swifts Multi-Line-Syntax kollidieren wuerden.

import Foundation

public enum PythonCLITemplates {

    /// Wandelt "mein-super-harness" in "mein_super_harness" (Python-paketkompatibel).
    public static func pythonPackageName(from slug: String) -> String {
        slug.replacingOccurrences(of: "-", with: "_")
    }

    // MARK: - pyproject.toml

    public static func pyprojectToml(slug: String, pyPackage: String, taskDescription: String) -> String {
        """
        [project]
        name = "\(slug)"
        version = "0.1.0"
        description = "\(escapeForToml(taskDescription))"
        requires-python = ">=3.10"
        dependencies = [
            "typer>=0.12.0",
            "rich>=13.7.0",
            "httpx>=0.27.0",
        ]

        [project.scripts]
        \(slug) = "\(pyPackage).cli:app"

        [build-system]
        requires = ["hatchling"]
        build-backend = "hatchling.build"

        [tool.hatch.build.targets.wheel]
        packages = ["src/\(pyPackage)"]
        """
    }

    // MARK: - README.md

    public static func readmeMd(slug: String, taskDescription: String, decomposition: TaskDecomposition) -> String {
        """
        # \(slug)

        > Automatisch generierter Python-CLI-Harness fuer folgende Aufgabe:
        >
        > \(taskDescription)

        ## Installation

        ```bash
        cd \(slug)
        pip install -e .
        ```

        Oder mit `uv` (empfohlen):

        ```bash
        uv pip install -e .
        ```

        ## Setup: API-Keys

        Je nachdem, welches Backend du nutzen willst, setze eine dieser Umgebungsvariablen:

        ```bash
        export ANTHROPIC_API_KEY="sk-ant-..."
        export OPENAI_API_KEY="sk-..."
        export GEMINI_API_KEY="..."
        ```

        ## Benutzung

        ```bash
        \(slug) ask "Deine Frage"
        \(slug) ask "Frage" --backend openai --model gpt-4.1
        \(slug) info
        ```

        ## Struktur

        ```
        \(slug)/
        ├── pyproject.toml        Abhaengigkeiten + Entry-Point
        ├── SYSTEM_PROMPT.md      Der optimierte Prompt fuer diese Aufgabe
        ├── README.md             Diese Datei
        └── src/\(pythonPackageName(from: slug))/
            ├── __init__.py
            ├── __main__.py       python -m \(pythonPackageName(from: slug))
            ├── cli.py            Typer-Commands
            └── llm_client.py     Multi-Backend-HTTP-Client
        ```

        ## Kontext

        - Zielumgebung: `\(decomposition.targetEnvironment.rawValue)`
        - Zielnutzer: `\(decomposition.targetUsers.rawValue)`
        - Begruendung: \(decomposition.rationale)

        ---

        *Generiert von Harness Forge.*
        """
    }

    // MARK: - SYSTEM_PROMPT.md (reused from PromptTemplate)

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

    // MARK: - .gitignore

    public static func gitignore() -> String {
        """
        __pycache__/
        *.py[cod]
        *$py.class
        .venv/
        venv/
        .env
        dist/
        build/
        *.egg-info/
        .pytest_cache/
        .mypy_cache/
        .ruff_cache/
        """
    }

    // MARK: - __init__.py

    public static func initPy(slug: String) -> String {
        #"""
        """\#(slug) - generiert von Harness Forge."""
        __version__ = "0.1.0"
        """#
    }

    // MARK: - __main__.py

    public static func mainPy() -> String {
        #"""
        """Entry-Point fuer `python -m <pypackage>`."""
        from .cli import app

        if __name__ == "__main__":
            app()
        """#
    }

    // MARK: - cli.py (Typer-Commands)

    public static func cliPy(slug: String, pyPackage: String, taskDescription: String) -> String {
        #"""
        """Command-Line-Interface."""
        from __future__ import annotations

        import typer
        from rich.console import Console
        from rich.markdown import Markdown

        from .llm_client import LLMClient, load_system_prompt

        app = typer.Typer(
            name="\#(slug)",
            help="\#(escapeForPython(taskDescription))",
            no_args_is_help=True,
        )
        console = Console()


        @app.command()
        def ask(
            question: str = typer.Argument(..., help="Deine Frage oder Aufgabe"),
            model: str = typer.Option("claude-opus-4-7", help="Modell-Name"),
            backend: str = typer.Option(
                "anthropic",
                help="Backend: anthropic | openai | gemini",
            ),
        ) -> None:
            """Stelle eine Frage und erhalte eine Antwort."""
            system_prompt = load_system_prompt()
            client = LLMClient(backend=backend, model=model)
            try:
                response = client.chat(system_prompt=system_prompt, user_message=question)
            except Exception as exc:
                console.print(f"[red]Fehler:[/red] {exc}")
                raise typer.Exit(code=1)
            console.print(Markdown(response))


        @app.command()
        def info() -> None:
            """Zeige Informationen ueber diesen Harness."""
            console.print(f"[bold]\#(slug)[/bold] (Version 0.1.0)")
            console.print("Generiert von Harness Forge.")
            console.print(f"System-Prompt: {len(load_system_prompt())} Zeichen")


        if __name__ == "__main__":
            app()
        """#
    }

    // MARK: - llm_client.py (Multi-Backend HTTP-Client)

    public static func llmClientPy() -> String {
        #"""
        """Minimale Multi-Backend LLM-Abstraktion via httpx."""
        from __future__ import annotations

        import os
        from pathlib import Path
        from typing import Literal

        import httpx

        Backend = Literal["anthropic", "openai", "gemini"]


        def load_system_prompt() -> str:
            """Laedt den System-Prompt aus SYSTEM_PROMPT.md (neben pyproject.toml)."""
            here = Path(__file__).resolve().parent.parent.parent
            prompt_file = here / "SYSTEM_PROMPT.md"
            if prompt_file.exists():
                return prompt_file.read_text(encoding="utf-8")
            return "Du bist ein hilfreicher Assistent."


        class LLMClient:
            """Minimal-Client fuer Anthropic, OpenAI, Gemini. Alle ueber httpx."""

            def __init__(self, backend: Backend = "anthropic", model: str = "claude-opus-4-7"):
                self.backend = backend
                self.model = model

            def chat(self, system_prompt: str, user_message: str) -> str:
                if self.backend == "anthropic":
                    return self._anthropic(system_prompt, user_message)
                if self.backend == "openai":
                    return self._openai(system_prompt, user_message)
                if self.backend == "gemini":
                    return self._gemini(system_prompt, user_message)
                raise ValueError(f"Unbekanntes Backend: {self.backend}")

            # -- Anthropic ---------------------------------------------------------

            def _anthropic(self, system: str, user: str) -> str:
                key = os.environ.get("ANTHROPIC_API_KEY")
                if not key:
                    raise RuntimeError("ANTHROPIC_API_KEY nicht gesetzt")
                response = httpx.post(
                    "https://api.anthropic.com/v1/messages",
                    headers={
                        "x-api-key": key,
                        "anthropic-version": "2023-06-01",
                        "content-type": "application/json",
                    },
                    json={
                        "model": self.model,
                        "max_tokens": 2048,
                        "system": system,
                        "messages": [{"role": "user", "content": user}],
                    },
                    timeout=60.0,
                )
                response.raise_for_status()
                data = response.json()
                return data["content"][0]["text"]

            # -- OpenAI ------------------------------------------------------------

            def _openai(self, system: str, user: str) -> str:
                key = os.environ.get("OPENAI_API_KEY")
                if not key:
                    raise RuntimeError("OPENAI_API_KEY nicht gesetzt")
                response = httpx.post(
                    "https://api.openai.com/v1/chat/completions",
                    headers={
                        "authorization": f"Bearer {key}",
                        "content-type": "application/json",
                    },
                    json={
                        "model": self.model,
                        "messages": [
                            {"role": "system", "content": system},
                            {"role": "user", "content": user},
                        ],
                    },
                    timeout=60.0,
                )
                response.raise_for_status()
                data = response.json()
                return data["choices"][0]["message"]["content"]

            # -- Gemini ------------------------------------------------------------

            def _gemini(self, system: str, user: str) -> str:
                key = os.environ.get("GEMINI_API_KEY")
                if not key:
                    raise RuntimeError("GEMINI_API_KEY nicht gesetzt")
                url = (
                    "https://generativelanguage.googleapis.com/v1beta/models/"
                    f"{self.model}:generateContent?key={key}"
                )
                response = httpx.post(
                    url,
                    headers={"content-type": "application/json"},
                    json={
                        "contents": [{"role": "user", "parts": [{"text": user}]}],
                        "systemInstruction": {"parts": [{"text": system}]},
                    },
                    timeout=60.0,
                )
                response.raise_for_status()
                data = response.json()
                return data["candidates"][0]["content"]["parts"][0]["text"]
        """#
    }

    // MARK: - Escape-Helfer

    private static func escapeForToml(_ s: String) -> String {
        s.replacingOccurrences(of: "\\", with: "\\\\")
            .replacingOccurrences(of: "\"", with: "\\\"")
            .replacingOccurrences(of: "\n", with: " ")
    }

    private static func escapeForPython(_ s: String) -> String {
        s.replacingOccurrences(of: "\\", with: "\\\\")
            .replacingOccurrences(of: "\"", with: "\\\"")
            .replacingOccurrences(of: "\n", with: " ")
    }
}
