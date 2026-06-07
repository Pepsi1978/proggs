# VoiceAgent — Baustein 1 (Hauptagent) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ein eigenständiger, sprachgesteuerter Hauptagent als native Windows-App: Mikrofon dauerhaft an → Groq-Whisper-Transkription → Gemini-Gehirn (Aufgabenerkennung + Rückfrage + Gesprächsantwort) → Google-Chirp3-HD-Sprachausgabe, plus Einstellungs-UI (System-Prompt, API-Schlüssel, Modell-Auswahl, Stimmenauswahl).

**Architecture:** WPF/.NET-8-App mit klar getrennten Service-Modulen hinter Schnittstellen. Audio (NAudio) → STT (Groq) → BossAgent-Kern → LLM-Provider-Schicht (Gemini default; Claude/OpenAI vorbereitet) → TTS (Google). STT- und Gemini-Clients werden aus dem bestehenden TerminalVoiceOverlay (TVO) übernommen; der TTS-Client wird in C# nach der EntropieReductor-Kotlin-Vorlage neu gebaut.

**Tech Stack:** C# / .NET 8 (`net8.0-windows`), WPF, NAudio, Microsoft.Extensions.DependencyInjection, System.Text.Json. Externe Dienste: Groq (`whisper-large-v3-turbo`), Google Generative Language (Gemini), Google Cloud Text-to-Speech (Chirp 3 HD).

---

## Test-Strategie (ehrlicher Hinweis)

Reines TDD passt hier nur teilweise: Echtzeit-Audio (NAudio), WPF-UI und Live-LLM-/TTS-APIs lassen sich nicht sinnvoll als Unit-Test-First bauen. Deshalb:

- **Unit-Tests (TDD)** für reine Logik ohne I/O: JSON-Parsing der Antworten, Gesprächsverlauf-Verwaltung, Config-Laden/-Speichern, Stille-Erkennungs-Schwellen.
- **Build-Verifikation** nach jeder Task: `dotnet build` muss fehlerfrei sein.
- **Manuelle Smoke-Tests** für Audio/UI/Live-API mit klar definiertem erwartetem Verhalten.

Jede Task endet mit Commit + Push (Repo-Regel: nur eigene Dateien stagen, fetch+rebase vor Push).

---

## Datei-Struktur (Decomposition)

Neues Projekt unter `~/proggs/VoiceAgent/`. Ein WPF-Hauptprojekt, ein Test-Projekt.

```
VoiceAgent/
  VoiceAgent.sln
  src/VoiceAgent/
    VoiceAgent.csproj
    app.manifest
    App.xaml / App.xaml.cs              # Lifecycle, DI-Container, Orchestrierung
    Services/
      Config.cs                         # Settings + API-Keys (SK-Ordner), JSON-Persistenz
      AppSettings.cs                    # Settings-Datenmodell (Prompt, Modelle, Stimme, Mic-Status)
      GroqWhisperClient.cs              # ÜBERNAHME aus TVO (STT)
      Llm/
        ILlmProvider.cs                 # Schnittstelle fürs Gehirn
        GeminiProvider.cs               # Default-Implementierung (Basis aus TVO-GeminiClient)
        ClaudeProvider.cs               # Vorbereitet (Stub mit klarer NotImplemented-Meldung)
        OpenAiProvider.cs               # Vorbereitet (Stub mit klarer NotImplemented-Meldung)
        LlmMessage.cs                   # Rollen+Text fürs Gesprächs-Modell
      GoogleTtsClient.cs                # NEU (nach EntropieReductor-Vorlage)
      GoogleTtsVoices.cs               # NEU — 30 Chirp3-HD-Stimmen (Portierung aus EntropieReductor)
      Audio/
        AlwaysOnListener.cs             # NEU — NAudio Dauer-Aufnahme + Stille-Chunking
        AudioPlayer.cs                  # NEU — NAudio MP3-Wiedergabe der TTS-Antwort
    Core/
      BossAgent.cs                      # Der Hauptagent: Verlauf + System-Prompt + Provider-Aufruf
      BossAgentPrompt.cs                # Standard-System-Prompt (Aufgabenerkennung, Rückfrage, TTS-Stil)
    Views/
      MainWindow.xaml / .cs             # Gesprächsanzeige + Mic-Status/Schalter
      SettingsWindow.xaml / .cs         # Sektionen: Prompt, API-Keys, Modelle, Stimme
  tests/VoiceAgent.Tests/
    VoiceAgent.Tests.csproj             # xUnit
    GoogleTtsClientTests.cs
    BossAgentTests.cs
    ConfigTests.cs
```

**Verifizierte externe Fakten (aus Code-Recherche 2026-06-07):**
- Groq STT-Endpoint (aus TVO): `https://api.groq.com/openai/v1/audio/transcriptions`, Modell `whisper-large-v3-turbo`, Form-Feld `language` (z. B. `de`), `response_format=text`.
- Gemini-Endpoint (aus TVO): `https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={apiKey}`, Body `{contents:[{role,parts:[{text}]}], generationConfig:{maxOutputTokens}}`.
- Google-TTS (aus EntropieReductor): Base `https://texttospeech.googleapis.com/`, `POST v1/text:synthesize?key={apiKey}` mit `{input:{text}, voice:{languageCode:"de-DE", name:"de-DE-Chirp3-HD-Kore"}, audioConfig:{audioEncoding:"MP3"}}` → Antwort `{audioContent: "<base64>"}`. Stimmen-Liste via `GET v1/voices?key=...`.
- Secrets-Ordner: `~/SK/VoiceAgent/` (Regel `secrets-in-sk-folder`).
- **Zu verifizieren bei Implementierung:** exakte Gemini-Model-ID für "Gemini 3.1 Flash Lite" gegen die aktuelle Google-API (Default-Konstante zentral in `AppSettings.cs`, an einer Stelle änderbar).

---

## Etappe 0 — Projekt-Gerüst (leere App startet)

### Task 0: WPF-Projekt + Solution + Test-Projekt anlegen

**Files:**
- Create: `VoiceAgent/VoiceAgent.sln`
- Create: `VoiceAgent/src/VoiceAgent/VoiceAgent.csproj`
- Create: `VoiceAgent/src/VoiceAgent/App.xaml` + `App.xaml.cs`
- Create: `VoiceAgent/src/VoiceAgent/Views/MainWindow.xaml` + `.cs`
- Create: `VoiceAgent/src/VoiceAgent/app.manifest`
- Create: `VoiceAgent/tests/VoiceAgent.Tests/VoiceAgent.Tests.csproj`
- Create: `VoiceAgent/.gitignore`

- [ ] **Step 1: Projekt erzeugen**

`.csproj` analog zu TVO (net8.0-windows, WPF), aber schlank:
```xml
<Project Sdk="Microsoft.NET.Sdk">
  <PropertyGroup>
    <OutputType>WinExe</OutputType>
    <TargetFramework>net8.0-windows</TargetFramework>
    <Nullable>enable</Nullable>
    <UseWPF>true</UseWPF>
    <RootNamespace>VoiceAgent</RootNamespace>
    <AssemblyName>VoiceAgent</AssemblyName>
  </PropertyGroup>
  <ItemGroup>
    <PackageReference Include="NAudio" Version="2.2.1" />
    <PackageReference Include="Microsoft.Extensions.DependencyInjection" Version="9.0.0" />
  </ItemGroup>
</Project>
```
`MainWindow.xaml`: leeres Fenster, Titel "VoiceAgent".

- [ ] **Step 2: `.gitignore`** — `bin/`, `obj/`, `*.user`, lokale Config.

- [ ] **Step 3: Build verifizieren**
Run: `dotnet build VoiceAgent/VoiceAgent.sln`
Expected: Build erfolgreich, 0 Fehler.

- [ ] **Step 4: App startet**
Run: `dotnet run --project VoiceAgent/src/VoiceAgent`
Expected: leeres Fenster "VoiceAgent" erscheint.

- [ ] **Step 5: Commit**
```bash
git add VoiceAgent/VoiceAgent.sln VoiceAgent/src VoiceAgent/tests VoiceAgent/.gitignore
git commit -m "#NNN - VoiceAgent: project scaffold (WPF .NET 8 + test project)"
git fetch origin && git rebase origin/main && git push
```

---

## Etappe 1 — Konfiguration & Einstellungs-Speicher

### Task 1: AppSettings-Modell + Config (Laden/Speichern + SK-Keys)

**Files:**
- Create: `src/VoiceAgent/Services/AppSettings.cs`
- Create: `src/VoiceAgent/Services/Config.cs`
- Test: `tests/VoiceAgent.Tests/ConfigTests.cs`

- [ ] **Step 1: Failing test (Config-Roundtrip)**
```csharp
[Fact]
public void SaveThenLoad_PreservesSystemPromptAndVoice() {
    var path = Path.GetTempFileName();
    var cfg = new AppSettings { SystemPrompt = "Test", TtsVoiceName = "de-DE-Chirp3-HD-Kore", LlmModel = "gemini-x" };
    Config.SaveTo(path, cfg);
    var loaded = Config.LoadFrom(path);
    Assert.Equal("Test", loaded.SystemPrompt);
    Assert.Equal("de-DE-Chirp3-HD-Kore", loaded.TtsVoiceName);
    Assert.Equal("gemini-x", loaded.LlmModel);
}
```

- [ ] **Step 2: Test schlägt fehl** — Run: `dotnet test` → FAIL (AppSettings/Config fehlen).

- [ ] **Step 3: AppSettings + Config implementieren**
`AppSettings.cs`: Properties `SystemPrompt`, `LlmProvider` (Default `"gemini"`), `LlmModel` (Default-Konstante `DefaultGeminiModel`), `TtsVoiceName` (Default `"de-DE-Chirp3-HD-Kore"`), `TtsLanguageCode` (`"de-DE"`), `SttModel` (`"whisper-large-v3-turbo"`), `SttLanguage` (`"de"`), `MicEnabled` (true).
`Config.cs`: `SaveTo/LoadFrom(path)` via `System.Text.Json` (UTF-8, `WriteIndented`); Settings-Pfad `%LOCALAPPDATA%\VoiceAgent\settings.json`. API-Keys getrennt: `ReadApiKey(provider)` liest aus `~/SK/VoiceAgent/keys.json` bzw. Umgebungsvariable — niemals in `settings.json`.

- [ ] **Step 4: Test grün** — Run: `dotnet test` → PASS.

- [ ] **Step 5: Commit** (`#NNN - VoiceAgent: settings model + config persistence (SK-folder keys)`)

---

## Etappe 2 — LLM-Provider-Schicht + BossAgent-Kern (per Text testbar)

### Task 2: LLM-Schnittstelle + Gemini-Provider

**Files:**
- Create: `src/VoiceAgent/Services/Llm/LlmMessage.cs` (`enum LlmRole { System, User, Assistant }`, `record LlmMessage(LlmRole Role, string Text)`)
- Create: `src/VoiceAgent/Services/Llm/ILlmProvider.cs` (`Task<string> ChatAsync(IReadOnlyList<LlmMessage> messages, CancellationToken ct)`)
- Create: `src/VoiceAgent/Services/Llm/GeminiProvider.cs`
- Create: `src/VoiceAgent/Services/Llm/ClaudeProvider.cs` (Stub)
- Create: `src/VoiceAgent/Services/Llm/OpenAiProvider.cs` (Stub)
- Test: `tests/VoiceAgent.Tests/` (Parsing-Test gegen ein gespeichertes Gemini-Antwort-JSON)

- [ ] **Step 1: Failing test (Antwort-Parsing)**
```csharp
[Fact]
public void ParseGeminiResponse_ExtractsText() {
    string json = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Hallo Frank\"}]}}]}";
    Assert.Equal("Hallo Frank", GeminiProvider.ExtractText(json));
}
```

- [ ] **Step 2: Test schlägt fehl** — Run: `dotnet test` → FAIL.

- [ ] **Step 3: GeminiProvider implementieren**
HTTP-/Retry-Basis + `ExtractText` aus TVO `GeminiClient.cs` übernehmen (Endpoint, SharedHttp, RetryableStatusCodes, ExtractText mit Thinking-Skip). Neu: `ChatAsync` baut `contents` aus der Nachrichtenliste — System-Message als erste `user`-part-Instruktion ODER `system_instruction`-Feld; `User`/`Assistant` → `role:"user"`/`role:"model"`. `ClaudeProvider`/`OpenAiProvider`: werfen `NotSupportedException("Claude/OpenAI ist vorbereitet, aber in Baustein 1 noch nicht aktiv.")`.

- [ ] **Step 4: Test grün** — Run: `dotnet test` → PASS.

- [ ] **Step 5: Commit** (`#NNN - VoiceAgent: LLM provider layer (Gemini default, Claude/OpenAI prepared)`)

### Task 3: BossAgent-Kern + System-Prompt

**Files:**
- Create: `src/VoiceAgent/Core/BossAgentPrompt.cs` (Standard-System-Prompt-Konstante)
- Create: `src/VoiceAgent/Core/BossAgent.cs`
- Test: `tests/VoiceAgent.Tests/BossAgentTests.cs`

- [ ] **Step 1: Failing test (Verlauf wächst, Provider wird aufgerufen)**
```csharp
[Fact]
public async Task Respond_AddsUserAndAssistantToHistory() {
    var fake = new FakeProvider("Antwort");           // ILlmProvider-Stub gibt feste Antwort
    var agent = new BossAgent(fake, "SYS");
    var reply = await agent.RespondAsync("Hallo", default);
    Assert.Equal("Antwort", reply);
    Assert.Equal(2, agent.History.Count);             // User + Assistant
    Assert.Equal(LlmRole.System, agent.BuildMessages()[0].Role);
}
```

- [ ] **Step 2: Test schlägt fehl** — Run: `dotnet test` → FAIL.

- [ ] **Step 3: BossAgent implementieren**
`BossAgent(ILlmProvider provider, string systemPrompt)`. Hält `List<LlmMessage> History`. `BuildMessages()` = `[System] + History`. `RespondAsync(userText, ct)`: User anhängen, `provider.ChatAsync(BuildMessages())`, Antwort anhängen, zurückgeben. `BossAgentPrompt.Default`: Instruiert das Modell, in natürlicher Gesprächssprache zu antworten, kurze TTS-freundliche Sätze zu nutzen, bei einer erkannten Aufgabe ZUERST kurz zurückzufragen ("Soll ich …?"), und ehrlich zu sagen, dass Ausführungs-Helfer (Unteragenten) noch nicht verfügbar sind.

- [ ] **Step 4: Test grün** — Run: `dotnet test` → PASS.

- [ ] **Step 5: Temporäres Text-Eingabefeld in MainWindow** (Smoke-Test-Hilfe): TextBox + Button → `BossAgent.RespondAsync` → Antwort in Gesprächsanzeige. (Wird in Etappe 5 durch Voice ersetzt, bleibt als Debug-Eingabe.)

- [ ] **Step 6: Manueller Smoke-Test mit echtem Gemini-Key**
Key in `~/SK/VoiceAgent/keys.json`. App starten, Text tippen → sinnvolle deutsche Antwort erscheint.
Expected: Antwort vom echten Gemini, Gesprächston.

- [ ] **Step 7: Commit** (`#NNN - VoiceAgent: BossAgent core + system prompt (text-driven, verified)`)

---

## Etappe 3 — Sprachausgabe (Google Chirp 3 HD)

### Task 4: GoogleTtsVoices (30 Stimmen) + GoogleTtsClient + AudioPlayer

**Files:**
- Create: `src/VoiceAgent/Services/GoogleTtsVoices.cs` (Portierung der 30 Chirp3-HD-Stimmen aus EntropieReductor `GoogleTtsVoices.kt`)
- Create: `src/VoiceAgent/Services/GoogleTtsClient.cs`
- Create: `src/VoiceAgent/Services/Audio/AudioPlayer.cs`
- Test: `tests/VoiceAgent.Tests/GoogleTtsClientTests.cs`

- [ ] **Step 1: Failing test (Request-Body + Response-Parsing)**
```csharp
[Fact]
public void BuildSynthesisBody_UsesVoiceAndMp3() {
    string body = GoogleTtsClient.BuildRequestJson("Hallo", "de-DE", "de-DE-Chirp3-HD-Kore");
    Assert.Contains("\"name\":\"de-DE-Chirp3-HD-Kore\"", body);
    Assert.Contains("\"audioEncoding\":\"MP3\"", body);
}
[Fact]
public void ParseAudioContent_DecodesBase64() {
    string json = "{\"audioContent\":\"" + Convert.ToBase64String(new byte[]{1,2,3}) + "\"}";
    Assert.Equal(new byte[]{1,2,3}, GoogleTtsClient.ParseAudio(json));
}
```

- [ ] **Step 2: Test schlägt fehl** — Run: `dotnet test` → FAIL.

- [ ] **Step 3: Implementieren**
`GoogleTtsVoices.cs`: `record GoogleTtsVoice(string Name, string DisplayName, VoiceGender Gender)`, `const DefaultVoiceName="de-DE-Chirp3-HD-Kore"`, `static IReadOnlyList<GoogleTtsVoice> All` (die 30 Stimmen aus der Kotlin-Vorlage).
`GoogleTtsClient.cs`: `BuildRequestJson(text, lang, voice)`, `ParseAudio(json)`, `Task<byte[]> SynthesizeAsync(text, lang, voice, ct)` gegen `https://texttospeech.googleapis.com/v1/text:synthesize?key={key}`; geteilter HttpClient + Retry wie bei Groq/Gemini.
`AudioPlayer.cs`: NAudio — Base64→MP3-Bytes → `Mp3FileReader`/`WaveOutEvent` abspielen; `PlayAsync(byte[] mp3)`.

- [ ] **Step 4: Test grün** — Run: `dotnet test` → PASS.

- [ ] **Step 5: Verdrahten** — `BossAgent`-Antwort wird durch `GoogleTtsClient.SynthesizeAsync` + `AudioPlayer.PlayAsync` vorgelesen.

- [ ] **Step 6: Manueller Smoke-Test** — Text tippen → Antwort wird in Chirp3-HD-Stimme vorgelesen.
Expected: hörbare deutsche Sprachausgabe.

- [ ] **Step 7: Commit** (`#NNN - VoiceAgent: Google Chirp3-HD TTS client + audio playback`)

---

## Etappe 4 — Spracheingabe (Groq Whisper + Dauer-Mikrofon)

### Task 5: GroqWhisperClient übernehmen + AlwaysOnListener

**Files:**
- Create: `src/VoiceAgent/Services/GroqWhisperClient.cs` (ÜBERNAHME aus TVO, Namespace anpassen)
- Create: `src/VoiceAgent/Services/Audio/AlwaysOnListener.cs`
- Test: `tests/VoiceAgent.Tests/` (Stille-Schwellen-Logik isoliert testen)

- [ ] **Step 1: GroqWhisperClient übernehmen**
Datei aus `TerminalVoiceOverlay-Windows/Services/GroqWhisperClient.cs` kopieren, `namespace VoiceAgent.Services`. Konstruktor-Args bleiben (`apiKey, model="whisper-large-v3-turbo", language="de", url="https://api.groq.com/openai/v1/audio/transcriptions"`).

- [ ] **Step 2: Failing test (Stille-Erkennung)**
```csharp
[Fact]
public void IsSilence_BelowThreshold_True() {
    Assert.True(AlwaysOnListener.IsSilence(new float[]{0.001f,0.0f}, 0.01f));
    Assert.False(AlwaysOnListener.IsSilence(new float[]{0.5f}, 0.01f));
}
```

- [ ] **Step 3: Test schlägt fehl** — Run: `dotnet test` → FAIL.

- [ ] **Step 4: AlwaysOnListener implementieren**
NAudio `WaveInEvent` (16 kHz mono). Puffert Samples; bei Stille > N ms wird das gepufferte Stück als WAV geschrieben und per Event `OnUtterance(wavPath)` gemeldet; bei `MicEnabled=false` pausiert die Aufnahme. `static bool IsSilence(float[] samples, float threshold)` als reine, testbare Funktion (RMS/Peak).

- [ ] **Step 5: Test grün** — Run: `dotnet test` → PASS.

- [ ] **Step 6: Voll verdrahten (der Voice-Loop)**
`AlwaysOnListener.OnUtterance` → `GroqWhisperClient.TranscribeAsync` → `BossAgent.RespondAsync` → `GoogleTtsClient` + `AudioPlayer`. Während die Antwort gesprochen wird, Aufnahme kurz pausieren (kein Selbst-Mithören).

- [ ] **Step 7: Manueller Smoke-Test (Endlos-Loop)** — Sprechen → wird transkribiert → Agent antwortet → Antwort wird vorgelesen. Mic-Aus-Schalter stoppt das Zuhören.
Expected: vollständige Sprach-Unterhaltung.

- [ ] **Step 8: Commit** (`#NNN - VoiceAgent: full voice loop (Groq STT + always-on mic + agent + TTS)`)

---

## Etappe 5 — Einstellungs-UI & Politur

### Task 6: SettingsWindow (Sektionen) + MainWindow-Politur

**Files:**
- Create: `src/VoiceAgent/Views/SettingsWindow.xaml` + `.cs`
- Modify: `src/VoiceAgent/Views/MainWindow.xaml` + `.cs`

- [ ] **Step 1: SettingsWindow mit klar getrennten Sektionen**
  1. **System-Prompt** — mehrzeilige TextBox (vorbelegt mit `BossAgentPrompt.Default`).
  2. **API-Schlüssel** — Felder für Groq, Google (TTS), Gemini (+ deaktivierte Felder Claude/OpenAI als "vorbereitet"). Speichern nach `~/SK/VoiceAgent/keys.json`.
  3. **Modell-Auswahl** — Dropdown Provider (Gemini aktiv; Claude/OpenAI vorbereitet/deaktiviert) + Modell-Textfeld (Default-Gemini-Modell).
  4. **Sprachausgabe** — Dropdown der 30 Chirp3-HD-Stimmen (`GoogleTtsVoices.All`, Default Kore) + Test-Button ("Probehören").

- [ ] **Step 2: MainWindow-Politur**
Gesprächsanzeige (Verlauf), gut sichtbarer Mic-Status (an/aus) + Schalter, Knopf zu den Einstellungen. Schlankes, ruhiges Layout (bewusst wenig "Entropie").

- [ ] **Step 3: Build + manueller Smoke-Test**
Run: `dotnet build` → 0 Fehler. App: alle Einstellungen ändern, schließen, neu öffnen → Werte bleiben erhalten; "Probehören" spielt die gewählte Stimme.

- [ ] **Step 4: Commit** (`#NNN - VoiceAgent: settings UI (prompt, keys, models, voices) + main window polish`)

---

## Etappe 6 — Release-Build (eine .exe)

### Task 7: Self-contained Publish

**Files:**
- Create: `VoiceAgent/publish.ps1` (analog TVO)

- [ ] **Step 1: publish.ps1**
`dotnet publish src/VoiceAgent -c Release -r win-x64 --self-contained -p:PublishSingleFile=true -o publish`

- [ ] **Step 2: Verifizieren** — `publish/VoiceAgent.exe` startet auf sauberem Stand; Keys aus `~/SK/VoiceAgent/`.

- [ ] **Step 3: Commit** (`#NNN - VoiceAgent: single-exe release publish script`)

---

## Definition of Done (Baustein 1)

1. Sprechen → zuverlässige Transkription (Groq Whisper).  
2. Hauptagent (Gemini) antwortet sinnvoll im Gesprächston.  
3. Erkannte Aufgabe → kurze, treffende Rückfrage.  
4. Antwort wird in wählbarer Chirp3-HD-Stimme flüssig vorgelesen.  
5. Mic an/aus funktioniert.  
6. System-Prompt, API-Keys, Modell, Stimme einstellbar + persistent.  
7. Auslieferbar als eine `.exe`.

---

## Spätere Bausteine (nicht Teil dieses Plans)

Feste + dynamische Unteragenten, Computer Use, proaktives Ansprechen + Enterprise-Sound, macOS-Port. Jeweils eigene Spec → eigener Plan.
