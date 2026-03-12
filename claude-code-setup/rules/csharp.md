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
- Format code: `dotnet format` (or `dotnet format whitespace` for whitespace only)
- Analyze code: `dotnet format analyzers` (style and quality checks)
