---
paths:
  - "**/*.cs"
  - "**/*.csproj"
  - "**/*.sln"
  - "**/*.xaml"
---

# C#/WPF Windows Development Rules

- Use .NET 10+ and C# 14 features (primary constructors, collection expressions)
- Target Windows 10+ with WPF for desktop apps
- Use MVVM pattern with CommunityToolkit.Mvvm
- Always include proper app.manifest for DPI awareness and admin elevation
- Build with: `dotnet build -c Release`
- Publish self-contained: `dotnet publish -c Release -r win-x64 --self-contained`
- Follow Fluent Design System for modern Windows UI
- Format code: `csharpier .` (opinionated formatter, like Prettier for C#) or `dotnet format`
- Lint/analyze code: `dotnet format analyzers` (style and quality checks)
- Run tests: `dotnet test` (or `dotnet test -c Release` for release config)
- Check NuGet vulnerabilities: `dotnet list package --vulnerable --include-transitive`
- Check outdated packages: `dotnet outdated` (global tool)
- Sign executables: `signtool sign /fd SHA256 /a MyApp.exe` (uses dev certificate)
