# VoiceAgent — Design-Spec: Baustein 1 (Hauptagent)

- **Datum:** 2026-06-07
- **Status:** Freigegeben durch Frank (Design-Abnahme erfolgt)
- **Arbeitstitel:** VoiceAgent (Name änderbar)
- **Plattform (Baustein 1):** Windows
- **Autor:** Frank + Claude (Brainstorming-Session)

---

## 1. Zweck & Gesamtvision

Ein **persönlicher, sprachgesteuerter Assistent**, vollständig in Frank-eigener Hand:
eigene Sprache (Deutsch), eigene Transkriptions-/Sprechmodelle, schlank und intuitiv —
bewusst als Gegenentwurf zu fremden Agenten-Tools (Hermes etc.), die zu viel "Entropie"
und keine Kontrolle über Sprache/Modelle bieten.

**Grundidee:** Frank spricht ein → Text wird transkribiert → ein **Hauptagent ("Boss-Agent")**
bewertet die Eingabe, erkennt Aufgaben/Fragen, fragt bei Bedarf kurz zurück und antwortet
wie in einem natürlichen Gespräch (für flüssiges Vorlesen optimiert). Später kann der
Hauptagent spezialisierte **Unteragenten** ansteuern und selbst neue bauen; jeder Agent
bekommt **Computer Use**. Der Hauptagent kann Frank auch **proaktiv** ansprechen
(Signalton wie ein Enterprise-Funkspruch).

Die Gesamtvision besteht aus mehreren Bausteinen. **Dieses Dokument spezifiziert ausschließlich
Baustein 1: den vollständigen Hauptagenten mit Voice und Einstellungs-UI — ohne Unteragenten.**

---

## 2. Scope von Baustein 1

### Enthalten (der komplette Hauptagent)

| Funktion | Beschreibung |
|---|---|
| Mikrofon dauerhaft an | Hört durchgehend zu, transkribiert automatisch — außer Mic ist abgeschaltet |
| Mic-An/Aus-Schalter | Frank kann das Zuhören jederzeit pausieren |
| Transkription (STT) | Groq **Whisper large-v3 turbo** über API |
| Gehirn (LLM) | **Gemini 3.1 Flash Lite** als Standard; Architektur offen für Claude + OpenAI |
| Aufgaben-/Frage-Erkennung | Hauptagent erkennt aus dem Gesagten, ob eine Aufgabe/Frage gemeint ist |
| Rückfrage zum Verständnis | Bei erkannter Aufgabe kurze Rückfrage: "Soll ich das machen?" |
| Natürliche Antwort | Gesprächston, **für TTS optimiert** (flüssig vorlesbar, kurze Sätze) |
| Sprachausgabe (TTS) | **Google Cloud TTS**, HD-Stimmen, Stimmauswahl |
| Windows-UI | System-Prompt + Sektion API-Schlüssel + Sektion Modell-Auswahl + Stimmenauswahl + Gesprächsanzeige + Mic-Status |

### Bewusst NICHT enthalten (eigene spätere Bausteine)

| Funktion | Warum später |
|---|---|
| Unteragenten (feste + dynamisch gebaute) | Eigenes großes Thema — erst muss der Hauptagent stabil stehen |
| Computer Use (Rechner real steuern) | Hängt an den Unteragenten, sicherheitskritisch, eigene Runde |
| Proaktives Ansprechen + Enterprise-Sound | Sinnvoll erst, wenn Unteragenten im Hintergrund arbeiten und etwas zu melden haben |
| macOS-Version | Frank startet bewusst mit Windows; macOS später als eigener Swift-Port |

**Verhalten bei erkannter Aufgabe in Baustein 1:** Der Hauptagent erkennt die Aufgabe und
fragt zurück. Sagt Frank "ja", antwortet der Agent ehrlich, dass die Ausführungs-Helfer
(Unteragenten) noch im nächsten Baustein gebaut werden. Wissensfragen beantwortet das LLM
direkt im Gespräch.

---

## 3. Technologie-Stack

- **Sprache/Framework:** C# / .NET, **WPF** (native Windows-App)
- **Auslieferung:** eine einzelne `.exe` (keine Endnutzer-Abhängigkeiten)
- **Begründung:** entspricht Franks Regeln (C# bevorzugt, kein Python für GUI); maximale
  Wiederverwendung der bestehenden **TerminalVoiceOverlay (TVO)**-Audiotechnik; schlank,
  nativ, gut geeignet für späteres Computer Use auf Windows.

---

## 4. Architektur / Komponenten

Wenige, klar getrennte Module — jedes mit genau einer Aufgabe:

```
┌─────────────────────────────────────────────────────────────┐
│  VoiceAgent (C# / WPF)                                        │
│                                                               │
│   [Audio-Modul]  ──Audio──►  [STT-Client: Groq Whisper]       │
│   (Basis aus TVO)                     │                       │
│   Mic dauerhaft an,                   ▼ Text                  │
│   Stille-Chunking,            [AGENT-KERN / Gehirn]           │
│   Wiedergabe                  - System-Prompt                 │
│        ▲                      - Gesprächsverlauf              │
│        │ Audio                - Aufgabe/Frage erkennen        │
│   [TTS-Client] ◄──Text──      - Rückfrage-Logik               │
│   (Google Cloud TTS)          - Antwort (TTS-optimiert)       │
│                                       │                       │
│                               [LLM-Provider-Schicht]          │
│                               Gemini (Standard)               │
│                               Claude · OpenAI (vorbereitet)   │
│                                                               │
│   [Einstellungen/Config]  API-Keys · Modelle · Stimme · Prompt│
└─────────────────────────────────────────────────────────────┘
```

| Modul | Aufgabe | Abhängigkeiten |
|---|---|---|
| **Audio-Modul** | Mikrofon dauerhaft aufnehmen, Stille-basiertes Chunking, Audio-Wiedergabe | Basis aus TVO übernehmen |
| **STT-Client** | Audio-Chunk → Groq Whisper API → Text | Groq API-Key |
| **Agent-Kern** | Text + System-Prompt + Verlauf → LLM; Aufgabenerkennung; Rückfrage-Entscheidung; Antwort | LLM-Schicht |
| **LLM-Provider-Schicht** | Gemeinsame Schnittstelle hinter Gemini/Claude/OpenAI (umschaltbar) | jeweilige API-Keys |
| **TTS-Client** | Antworttext → Google Cloud TTS → Audio → Wiedergabe | Google API-Key, Audio-Modul |
| **Config/Settings** | API-Keys, Modellwahl, Stimme, System-Prompt persistieren | SK-Ordner |
| **UI (WPF)** | Gesprächsanzeige, Mic-Status, Einstellungs-Sektionen | alle obigen |

**Designprinzip:** Jedes Modul ist eigenständig verständlich und über eine klare
Schnittstelle ansprechbar. Der Agent-Kern hängt nicht an einem konkreten LLM, sondern an
der Provider-Schicht — so ist das spätere Umschalten auf Claude/OpenAI nur eine
Implementierung mehr hinter derselben Schnittstelle.

---

## 5. Externe Dienste & Modelle

| Zweck | Dienst/Modell | Anbindung | Referenz / Hinweis |
|---|---|---|---|
| Transkription (STT) | Groq **whisper-large-v3-turbo** | REST-API | Frank nutzt das in anderen Apps; dortige Anbindung als Vorlage |
| Gehirn (LLM, Standard) | **Gemini 3.1 Flash Lite** | REST-API | Genaue Model-ID bei Implementierung verifizieren |
| Gehirn (vorbereitet) | Anthropic Claude, OpenAI/ChatGPT | hinter Provider-Schicht | Franks Standard-Modelle; in UI auswählbar |
| Sprachausgabe (TTS) | **Google Cloud TTS**, HD-Stimmen | REST-API | Integration aus **EntropieReductor** als Vorlage; genaue HD-Stimmen ("3D-Stimmen") bei Implementierung klären |

---

## 6. Datenfluss (der Voice-Loop)

1. **Zuhören:** Audio-Modul nimmt Mikrofon dauerhaft auf (sofern Mic an), schneidet an
   Sprechpausen (Stille-Chunking) sinnvolle Stücke.
2. **Transkribieren:** STT-Client schickt das Stück an Groq Whisper → bekommt Text.
3. **Bewerten:** Agent-Kern fügt Text + System-Prompt + bisherigen Gesprächsverlauf
   zusammen und schickt das an das LLM (Gemini). Das LLM entscheidet:
   - reine Konversation/Wissensfrage → direkt antworten,
   - erkannte Aufgabe → Rückfrage "Soll ich das machen?" formulieren.
4. **Antworten:** Antworttext wird TTS-optimiert (kurze, vorlesbare Sätze) an den
   TTS-Client gegeben.
5. **Sprechen:** Google Cloud TTS erzeugt Audio in der gewählten HD-Stimme; Audio-Modul
   spielt es ab.
6. **Verlauf:** Eingabe + Antwort wandern in den Gesprächsverlauf (für Kontext der nächsten
   Runde).

---

## 7. Benutzeroberfläche (WPF, Windows)

**Hauptfenster**
- Gesprächsanzeige (was du gesagt hast / was der Agent geantwortet hat)
- Mic-Status + An/Aus-Schalter
- Zugang zu den Einstellungen

**Einstellungen (klar getrennte Sektionen)**
1. **System-Prompt** — der Prompt des Hauptagenten (frei editierbar)
2. **API-Schlüssel** — Eingabefelder für Groq, Google Cloud, Gemini (und vorbereitet Claude, OpenAI)
3. **Modell-Auswahl** — welches LLM das Gehirn antreibt (Standard: Gemini 3.1 Flash Lite)
4. **Sprachausgabe** — TTS-Anbieter + Stimmauswahl (HD-Stimmen)
5. **(implizit) Transkription** — Groq Whisper large-v3 turbo als gesetzter Standard

---

## 8. Konfiguration & Secrets

- **API-Schlüssel werden NIEMALS im Repo gespeichert.** Sie liegen in Franks zentralem
  Secrets-Ordner: `~/SK/VoiceAgent/` (Regel: `secrets-in-sk-folder`). Die App liest die
  Keys von dort bzw. aus einer lokalen, gitignorierten Config — niemals aus dem Quellcode.
- Übrige Einstellungen (System-Prompt, Modellwahl, Stimme) werden lokal persistiert
  (z. B. unter `%LOCALAPPDATA%\VoiceAgent\`).

---

## 9. Erfolgskriterien für Baustein 1

Baustein 1 gilt als fertig, wenn:
1. Frank spricht → wird zuverlässig transkribiert (Groq Whisper).
2. Der Hauptagent (Gemini) antwortet sinnvoll und im Gesprächston.
3. Erkannte Aufgaben lösen eine kurze, treffende Rückfrage aus.
4. Antworten werden in einer wählbaren Google-HD-Stimme flüssig vorgelesen.
5. Mic lässt sich an-/abschalten.
6. In der UI lassen sich System-Prompt, API-Schlüssel, Modell und Stimme einstellen und
   bleiben erhalten.
7. Auslieferbar als eine `.exe`.

---

## 10. Bewusst ausgeschlossen / spätere Bausteine

- Baustein 2+: feste Unteragenten, dynamisch vom Hauptagenten gebaute Helfer,
  Computer Use, proaktives Ansprechen mit Enterprise-Sound, macOS-Port.

---

## 11. Offene Detailpunkte (bei Implementierung zu verifizieren)

- Genaue Gemini-Model-ID ("3.1 Flash Lite") gegen aktuelle Google-API prüfen.
- Genaue Google-HD-Stimmen ("3D-Stimmen") festlegen — Referenz: EntropieReductor.
- Exaktes Groq-STT-Endpoint/Modellkürzel aus Franks bestehenden Apps übernehmen.
- Wiederverwendbarkeit der TVO-Audiokomponenten konkret prüfen (gemeinsame Bibliothek vs. Kopie).
