# PromptBoard

**PromptBoard** ist eine native Windows-Desktop-App, die Prompts nach Kategorien,
Projekten und einer KI-Verbesserungs-Bibliothek organisiert und sie per Klick
ohne Fokusverlust in die aktive CLI-Eingabezeile einfuegt.

Die App dockt als schmale Leiste am oberen Bildschirmrand an, bleibt immer im
Vordergrund und stiehlt beim Klicken keinen Fokus von der darunterliegenden
Kommandozeile (Claude Code CLI, Windows Terminal, PowerShell, VS Code Terminal).

## Status

Phase 0 — Setup und "Hello World"-Bar. Die eigentlichen Features
(Kategorien, Prompts, Diktat, KI-Verbesserung, Google-Drive-Backup) folgen in
den naechsten Phasen.

## Tech-Stack

- .NET 10 (LTS), C# 14
- WinUI 3 mit XAML (Windows App SDK, unpackaged)
- MVVM via CommunityToolkit.Mvvm
- Dependency Injection via Microsoft.Extensions.DependencyInjection
- Persistenz: SQLite via Microsoft.EntityFrameworkCore.Sqlite
- Logging: Serilog (Datei- und Konsolen-Sink)
- Tests: xUnit + Moq

## Projektstruktur

```
PromptBoard.slnx
├── PromptBoard.App          WinUI-3-App, Views, DI-Host, Serilog
├── PromptBoard.Core         Models, Interfaces, Enums (plattformneutral)
├── PromptBoard.Services     Groq, Gemini, GoogleDrive, TextInjection (Windows)
├── PromptBoard.Data         EF Core DbContext, Repositories (plattformneutral)
├── PromptBoard.ViewModels   MVVM (ObservableObjects, RelayCommands)
└── PromptBoard.Tests        xUnit
```

## Installation (fuer Endbenutzer)

### Windows 10 22H2 und Windows 11

1. Die neueste Release-ZIP von GitHub herunterladen.
2. Die ZIP entpacken, z.B. nach `C:\Programme\PromptBoard`.
3. `PromptBoard.App.exe` per Doppelklick starten.

Voraussetzung: Das Windows App SDK Runtime 1.7+ muss installiert sein (`winget
install Microsoft.WindowsAppSDK`). Wenn nicht, liefert die App beim Start eine
klare Fehlermeldung mit Installationshinweis.

## Entwicklung

### Voraussetzungen

- Windows 10 22H2 oder Windows 11
- .NET 10 SDK (`dotnet --list-sdks` muss 10.x enthalten)
- WinUI-3-Templates (`dotnet new install Microsoft.WindowsAppSDK.WinUI.CSharp.Templates`)
- Optional: Visual Studio 2022/2026 mit "Windows Application Development" Workload

### Erster Build

```pwsh
cd PromptBoard
dotnet restore
dotnet build
dotnet run --project PromptBoard.App
```

### Wo liegen Benutzerdaten?

`%LOCALAPPDATA%\PromptBoard\`

- `promptboard.db` — SQLite-Datenbank mit allen Kategorien, Prompts und Einstellungen
- `logs\` — Serilog-Logdateien (rollend, 14 Tage Aufbewahrung)

API-Keys werden **nicht** dort gespeichert, sondern im Windows Credential
Manager (DPAPI) via `Windows.Security.Credentials.PasswordVault`.

## Lizenz

Privat — Teil des `Pepsi1978/proggs`-Repositories.
