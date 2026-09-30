---
name: http-timeouts-fallback-wartezeit
description: Wo ein Fallback existiert, bestimmt der Gesamt-Timeout die Wartezeit des Nutzers — nicht die Geduld des Servers
metadata:
  type: feedback
---

Bei .NET-`HttpClient`: `ConnectTimeout` immer explizit setzen (ab Werk unendlich) und TCP-Keepalive über `ConnectCallback` aktivieren. Den Gesamt-Timeout am **gemessenen Median** wählen (Median × 5–6), nicht am Bauchgefühl.

**Why:** Am 30.08.2026 blieb im TerminalVoiceOverlay der Knopf zwei Minuten orange, dann kam der Text doch. Der Gemini-Aufruf hing bis zum 120-s-Timeout ohne je einen HTTP-Status zu bekommen; dieselbe Aufnahme lief danach in 3,5 s durch. Die 120 s waren als Puffer gedacht, waren aber eine Wartehalle: der Groq-Fallback braucht selbst nur 1 s, lief aber erst nach zwei Minuten an.

**How to apply:** `Services/ResilientHttp.cs` liegt in TerminalVoiceOverlay-Windows UND ClaudeVoiceOverlay-Windows (Schwesterprojekte, shared file — Änderungen immer in beiden). Details im Bug-Almanach `bugs/desktop/dotnet-csharp.md` §8.3/§8.4 und `bugs/desktop/voice-pipeline.md` §8.2. Siehe auch [[deploy-guard-port-unterscheidung]].

**Nachtrag 03.09.2026 (Drive-Sync):** Google.Apis baut seinen HttpClient selbst und umgeht `ResilientHttp`. In TVO/CVO haengt jetzt `DriveHttp.CreateService` (eigene `HttpClientFactory.CreateHandler`) den gehaerteten Handler ein und setzt 30 s Gesamt-Timeout. Muster: nach `new HttpClient(` auch nach `new XyzService(` greppen. Siehe [[vto-mikrofon-fehlt-stummer-klick]].
