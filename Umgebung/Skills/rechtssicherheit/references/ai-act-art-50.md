# EU AI Act Art. 50 — Konkrete Transparenz-Pflichten ab 02.08.2026

> **Hinweis:** Dieser Inhalt ist Teil des `rechtssicherheit`-Skills — eine **technische
> Pruefhilfe**, KEINE anwaltliche Beratung. Vor Release MUSS ein Fachanwalt fuer IT-Recht
> konsultiert werden.

> **Skill-Stand dieser Datei:** 2026-05-17 (siehe SKILL.md fuer den globalen Stand).
> Wenn der Skill-Stand >30 Tage alt ist, neue Recherche durchfuehren.
> **Wann diese Datei lesen:** Wenn die App KI/GenAI-Features enthaelt (Chatbot, Bild-/Audio-/Video-
> Generierung, Deepfake, Automated Decisions). Diese Datei beschreibt die Transparenz-Pflichten
> nach Art. 50 EU AI Act und das Zusammenspiel mit der Google Play AI-Content-Policy.

## Inkrafttreten

| Datum | Was gilt |
|--|--|
| 02.08.2025 | GPAI-Pflichten (General-Purpose AI) |
| **02.08.2026** | **Art. 50 Transparenzpflichten verbindlich** |
| 2027 | Vollstaendige Hochrisiko-KI-Pflichten (App-Chatbots koennen darunter fallen) |

## Konkrete Pflichten ab 02.08.2026

### 1. Chatbot-Kennzeichnung
Jede KI-Interaktion muss als solche erkennbar sein. Nutzer muessen wissen, dass sie mit KI
kommunizieren — es sei denn, es ist offensichtlich.

**UI-Beispiele:**
- "Du sprichst gerade mit einer KI."
- Avatar mit "KI"-Label
- Footer "Powered by [KI-Modell]"

### 2. Deepfake-Kennzeichnung
KI-generierte oder -manipulierte Bilder, Audio, Video MUESSEN als "kuenstlich erzeugt"
offengelegt werden.

**UI-Beispiele:**
- Wasserzeichen mit "KI-generiert"
- Caption "Dieses Bild wurde von einer KI erstellt"
- Provenance-Metadaten

### 3. Maschinenlesbare Markierung
Generative KI-Systeme (Text, Bild, Audio, Video) MUESSEN Outputs in maschinenlesbarer Form
kennzeichnen.

**Technische Umsetzung:**
- C2PA / Content Authenticity Initiative-Metadaten
- EXIF-Tags fuer Bilder
- Sichtbare + unsichtbare Wasserzeichen

### 4. Generelle GenAI-Hinweise
Bei jeder KI-Antwort sollte erkennbar sein, dass sie von einer KI stammt — UI-Hinweis
"von KI generiert".

## Ausnahmen

- Offensichtlich kuenstlerische / satirische / fiktive Werke
- Internes Anwendungsfeld (B2B, intern) hat z.T. erleichterte Pflichten

## Pflicht-UI-Pruefung im Skill

| App-Feature | Pflichtkennzeichnung | Wo pruefen |
|--|--|--|
| Chatbot (LLM-Conversation) | "Du sprichst mit einer KI" oder Aequivalent | Onboarding + jeder Chat-Screen |
| KI-Bildgenerierung | "Bild von KI erstellt" + Wasserzeichen | Bild-Output |
| KI-Audio (Voice Cloning, TTS) | Hoerbare oder visuelle Kennzeichnung | Audio-Player |
| KI-Video (Deepfake) | Sichtbare Kennzeichnung | Video-Player |
| KI-Textgenerierung (Zusammenfassung, Vorschlaege) | "Vorschlag von KI" o.ae. | Output-UI |

## Risikoklassifizierung (Art. 6)

Pro KI-Feature pruefen welche Klasse:

| Klasse | Beispiel | Pflichten |
|--|--|--|
| **Verbotene KI** | Social Scoring durch Behoerden | KOMPLETT verboten |
| **Hochrisiko-KI** | Kreditwuerdigkeit, Personalentscheidung, kritische Infrastruktur | Konformitaetsbewertung, Risikomanagement (Art. 9, 13) |
| **Begrenztes Risiko** | Chatbots, GenAI | Transparenzpflichten Art. 50 |
| **Minimales Risiko** | Spamfilter, Game-AI | Keine speziellen Pflichten |

App-Chatbots mit Endnutzer-Interaktion fallen meist unter "begrenztes Risiko" (Art. 50).
Wenn der Chatbot z.B. medizinische oder finanzielle Empfehlungen gibt: ggf. Hochrisiko.

## Sanktionen
- Bis 15 Mio. EUR oder 3% des Jahresumsatzes

## AI-System-Risikoklassifizierung als internes Compliance-Dokument

Pflicht intern zu fuehren:
- Pro KI-Feature: Risikoklasse, Anwender-/Anbieter-Rolle, Konformitaetsbewertung, Transparenzhinweise
- AI-Act-Risikoklassifizierung gehoert zu den **Pflicht-Internen-Artefakten** (siehe `pflichtdokumente.md`)

## Google Play AI-Generated Content Policy (parallel zu AI Act)

Google Play verlangt **schon heute** (seit 2024):
- **In-App-Flagging-Pflicht:** Apps mit KI-Content-Generierung MUESSEN In-App-Meldefunktion haben
  (Nutzer muss offensive Inhalte melden koennen ohne App zu verlassen)
- Kein "Restricted Content" generierbar (Child Safety, Deception)
- "Rigorous testing" der KI-Modelle vorausgesetzt
- Gilt fuer Text-to-text Chatbots, Text/Voice-to-Image, KI-generierte Audio/Video

## Im Skill-Bericht zu pruefen

1. Wird das KI-Feature im Code/Compose-UI als "KI" gekennzeichnet?
2. Sind Outputs (Bilder/Audio/Video) als kuenstlich erzeugt gekennzeichnet (Wasserzeichen, Caption)?
3. Existiert ein In-App-Reporting-Button fuer KI-Content (Google Play Policy + DSA Art. 16)?
4. Ist die AI-System-Risikoklassifizierung intern dokumentiert?
5. Falls Hochrisiko: ist Risikomanagement-System nach Art. 9 etabliert?

## Quellen
- `artificialintelligenceact.eu` (EU AI Act Volltext + Artikel)
- caralegal.eu Transparenzpflichten KI-Verordnung
- nemko digital GPAI 2025 Update
- support.google.com/googleplay AI-Generated Content Policy
