// Handgepflegte Studiendaten (Stand 21.10.2026). Quellen: Berichte r1–r10 im Ordner darüber.
// Preise in USD je Audiostunde. HF = Hugging Face Open ASR Leaderboard, Deutsch (multilingual_de.csv vom 19.10.2026),
// unabhängig gemessen, WER in Prozent: FLEURS (vorgelesen) / MCV = Common Voice (Crowd-Sätze, eher wie Diktat).
window.STUDIE = {
  stand: "21.10.2026",
  ausgangslage: {
    modell: "Groq whisper-large-v3-turbo",
    wo: ["Jarvis-Diktat (gemeinsames Transkriptionsmodul, GroqTranscriber + WhisperHallucinationFilter + VAD)", "TerminalVoiceOverlay / ClaudeVoiceOverlay (Standard-Engine laut transcription-engine.txt: groq; Gemini 3.5 Transcribe als Option)", "Jarvis Live: Mitschrift über gpt-transcribe"]
  },
  // Abschnitt „Integration“ (aus R9, mit Ergebnissen des Messplatzes)
  integration: `
    <h3>Jarvis (Modul M1.4 Transkriptionsmodul)</h3>
    <ul>
      <li><b>Heute:</b> <code>Diktat</code> hängt fest am <code>GroqTranscriber</code>; Aufnahme als fertige WAV nach 4,5 s Stille, davor VAD, danach <code>WhisperHallucinationFilter</code>.</li>
      <li><b>Kleinster Schritt (Groq large-v3):</b> nur <code>DEFAULT_MODEL</code> bzw. das Modellfeld ändern, nachdem das Modell in der Groq-Konsole freigeschaltet ist. Kein Umbau.</li>
      <li><b>Für einen zweiten Anbieter:</b> im Modul eine Schnittstelle <code>Transkribierer</code> einführen, <code>GroqTranscriber</code> implementiert sie, <code>Diktat</code> nimmt die Schnittstelle. <code>Anbindung.kt</code> wählt „primär neuer Anbieter, Rückfall Groq“. Änderungen nur in der Modul-Bibliothek, danach in die Apps nachziehen.</li>
      <li><b>Rückfall-Regeln</b> wie in den Overlays: Netz, 429, 5xx und leere 200-Antwort → Groq; 401/403 → Meldung; „keine Sprache“ → leer lassen.</li>
      <li><b>Vokabelliste:</b> dieselbe Liste wie <code>SK/VoiceOverlays/personal-vocabulary.txt</code>. Für Whisper als <code>prompt</code>, für Gemini als <code>custom_vocabulary</code>, für Deepgram je Begriff ein eigener <code>keyterm</code>-Parameter (nie mit Komma).</li>
      <li><b>Stille-Sperre ist Pflicht für jeden Anbieter:</b> Im Test gab Turbo mit Prompt bei Stille den Prompt aus, Gemini 3.8 Flash erfand ganze Sätze. Nur Aufnahmen mit erkannter Sprache weiterschicken.</li>
    </ul>
    <h3>TerminalVoiceOverlay / ClaudeVoiceOverlay</h3>
    <ul>
      <li><b>Schon vorhanden:</b> <code>SpeechToTextRouter</code> mit Groq, Gemini Transcribe (Datei) und Gemini Live, Rückfall auf Groq, <code>PersonalVocabulary</code> für beide Gemini-Wege.</li>
      <li><b>Wichtig:</b> Der kostenlose Transcribe-Schlüssel schafft nur <b>25 Anfragen pro Tag</b> und 3 pro Minute. Wer im Overlay auf Gemini steht, landet danach still bei Groq. Mit einem bezahlten Schlüssel (Prepay plus Limit) wäre Gemini Transcribe die genaueste gemessene Option.</li>
      <li><b>Groq large-v3:</b> <code>WHISPER_MODEL=whisper-large-v3</code> in der <code>.env</code>, nach Freischaltung in der Groq-Konsole.</li>
      <li><b>Neuer Anbieter:</b> neuer Wert in <code>TranscriptionEngineSetting</code>, neuer Zweig im Router; die Änderung gilt für TVO und CVO gemeinsam.</li>
    </ul>
    <h3>Streaming</h3>
    <ul>
      <li>Lohnt sich für Diktat kaum: Die größte Wartezeit sind die 4,5 s Stille am Ende, nicht die Erkennung. Günstiger ist eine vorgezogene Datei-Transkription nach etwa 1,2 s Stille (in <code>SprechEnde</code> vorgesehen), die verworfen wird, wenn weitergesprochen wird.</li>
      <li>Wenn doch Streaming: zuerst Gemini Live (Code existiert) mit Sitzungswächter, dann gpt-live-transcribe oder Deepgram/AssemblyAI. Nicht für Android: Chirp 3 (gRPC) und Voxtral Realtime (Protokoll nur im SDK).</li>
    </ul>
    <h3>Datenschutz in einem Satz je Anbieter</h3>
    <ul class="klein">
      <li><b>Groq:</b> kein Training, Verarbeitung und Speicherung in den USA. <b>Gemini API:</b> bezahlt kein Training; im Free Tier gelten für EWR-Nutzer dieselben Datenregeln, Apps an Dritte (z. B. Genialer Wecker) dürfen aber nur bezahlte Dienste nutzen. <b>Deepgram:</b> trainiert standardmäßig, <code>mip_opt_out=true</code> setzen. <b>AssemblyAI EU:</b> kein Training. <b>Mistral Free Mode:</b> Training erlaubt. <b>Lokal:</b> alles bleibt auf dem Gerät.</li>
    </ul>`,

  // Kostenrechner: Diktatminuten pro Tag × 30
  rechner: { profile: [["Wenig", 10], ["Mittel", 30], ["Intensiv", 120]] },

  anbieter: [
    {
      id: "groq-turbo", name: "Groq Whisper large-v3 Turbo", modell: "whisper-large-v3-turbo", art: "cloud", heute: true, urteil: "basis",
      status: "GA",
      frei: { typ: "dauerhaft", stundenMonat: 240, text: "dauerhaft: 20 Anfragen/Min., 2.000/Tag, 7.200 Audio-Sekunden/Stunde, 28.800/Tag (8 h Audio pro Tag), ohne Training mit deinen Daten" },
      preis: { proStunde: 0.04, text: "0,04 $/h (Batch 0,02 $/h), Mindestabrechnung 10 s je Anfrage" },
      hf: { fl: 3.67, mcv: 8.58, rang: "30 von 41" },
      deutsch: "Basislinie. Auf Common Voice fast doppelt so viele Fehler wie large-v3 (8,58 gegen 4,79). Der gekürzte Decoder verliert bei Deutsch und einfachen Mikrofonen überproportional.",
      messung: ["groq-turbo", "groq-turbo+prompt"],
      vokabular: "prompt (224 Tokens); bei Stille wird der Prompt selbst ausgegeben (eigene Messung)",
      streaming: "nein, nur Datei-Upload (multipart, OpenAI-kompatibel)",
      halluzination: "Whisper-typisch: Text bei Stille/Rauschen, Wiederholungen; Jarvis filtert mit WhisperHallucinationFilter + VAD",
      datenschutz: "kein Training laut Services Agreement, Logs bis 30 Tage, Verarbeitung in den USA",
      android: "läuft heute in Jarvis und den Overlays",
      pro: ["Größtes dauerhaftes Freikontingent aller Cloud-Anbieter", "Sehr schnell (unter 1 s in der eigenen Messung)", "Bereits überall eingebaut"],
      contra: ["Deutsch-Rangliste: Platz 30 von 41", "Eigennamen ohne Prompt schwach (Groq → „Grog“, Logcat → „Lockhead“)", "Prompt-Echo bei Stille"],
      quellen: ["r1", "r8"]
    },
    {
      id: "groq-v3", name: "Groq Whisper large-v3", modell: "whisper-large-v3", art: "cloud", urteil: "besser",
      status: "GA; in Franks Groq-Organisation gesperrt („blocked at the organization level“, in der Konsole freischaltbar)",
      frei: { typ: "dauerhaft", stundenMonat: 240, text: "wie Turbo, eigene Limit-Zeile (ob ein gemeinsames Audio-Budget gilt, ist unbelegt)" },
      preis: { proStunde: 0.111, text: "0,111 $/h (Batch 0,056 $/h)" },
      hf: { fl: 3.20, mcv: 4.79, rang: "19 von 41" },
      deutsch: "Belastbar besser als Turbo: HF, handy.computer und primeLine messen übereinstimmend weniger Fehler, auf Common Voice fast halbiert.",
      messung: [],
      vokabular: "prompt (224 Tokens), wie Turbo",
      streaming: "nein",
      halluzination: "wie Turbo, eher etwas mehr (40 % Text auf Nicht-Sprache in einer Studie)",
      datenschutz: "wie Turbo",
      android: "nur Modellname ändern: ein Wort im GroqTranscriber bzw. WHISPER_MODEL in der .env",
      pro: ["Kleinster möglicher Schritt: gleiche API, gleicher Filter, gleiches Freikontingent", "Unabhängig belegt besser auf Deutsch"],
      contra: ["Nicht gemessen: im eigenen Groq-Konto gesperrt", "Etwas langsamer, 2,8-facher Preis (nur relevant über dem Freikontingent)"],
      quellen: ["r1", "r8"]
    },
    {
      id: "cf-turbo", name: "Cloudflare Workers AI Whisper Turbo", modell: "@cf/openai/whisper-large-v3-turbo", art: "cloud", urteil: "gleich",
      status: "GA",
      frei: { typ: "dauerhaft", stundenMonat: 107, text: "10.000 Neurons pro Tag ≈ 214 Audiominuten pro Tag, ohne Zahlungsmittel; im Free-Plan Fehler statt Kosten" },
      preis: { proStunde: 0.03, text: "≈ 0,03 $/h" },
      hf: { fl: 3.67, mcv: 8.58, rang: "wie Turbo" },
      deutsch: "Dasselbe Modell wie bei Groq, also kein Qualitätsgewinn.",
      messung: [],
      vokabular: "initial_prompt, hallucination_silence_threshold",
      streaming: "nein (unbelegt)",
      halluzination: "wie Turbo, mit eingebautem Stille-Regler",
      datenschutz: "Cloudflare, Region nicht wählbar",
      android: "REST mit Account-ID und Token",
      pro: ["Zweites großes Gratis-Kontingent als Ausfall-Rückfall für Groq"],
      contra: ["Keine bessere Qualität"],
      quellen: ["r1", "r4"]
    },
    {
      id: "gemini-transcribe", name: "Google Gemini 3.5 Transcribe", modell: "gemini-3.5-transcribe (Interactions API, verbatim)", art: "cloud", urteil: "besser-gemessen",
      status: "GA laut Vertex, in der Gemini API ohne Statusangabe",
      frei: { typ: "klein", stundenMonat: 6, text: "Free Tier: nur 3 Anfragen/Min. und 25 Anfragen/Tag (429-Meldung im eigenen Test am 21.10.2026); Free-Tier-Bedingungen: im EWR für Apps mit Nutzern nur bezahlte Dienste" },
      preis: { proStunde: 0.30, text: "≈ 0,30 $/h bezahlt (25 Audio-Tokens je Sekunde)" },
      hf: null,
      deutsch: "Keine unabhängige Deutsch-Zahl. Artificial Analysis (englischlastig) 2,6 % gegen 4,6 % bei Turbo. Im eigenen Test zusammen mit Gemini 3.8 Flash am besten.",
      messung: ["gemini-3.5-transcribe", "gemini-3.5-transcribe+vokabular"],
      vokabular: "custom_vocabulary bis 1.000 Einträge (empfohlen etwa 100)",
      streaming: "nein; die Live-Variante gemini-3.5-transcribe-live ist ungenauer und langsamer",
      halluzination: "bei Stille leer (eigene Messung); Modus smart lässt Wörter weg, deshalb verbatim",
      datenschutz: "bezahlt kein Training; Free Tier: EWR-Klausel beachten",
      android: "ein HTTPS-Aufruf mit API-Key, Audio inline als Base64 (in den Overlays schon gebaut)",
      pro: ["Eigene Messung: fehlerfrei auf sauberem Testtext mit Vokabular, Eigennamen fast alle richtig", "Kein Halluzinieren bei Stille", "In den Overlays bereits eingebaut"],
      contra: ["Free Tier mit 25 Diktaten pro Tag praktisch unbrauchbar", "7,5-facher Turbo-Preis", "4–5 s statt unter 1 s"],
      quellen: ["r3", "r8"]
    },
    {
      id: "gemini-38", name: "Google Gemini 3.8 Flash (Audio-Prompt)", modell: "gemini-3.8-flash, generateContent mit Audio", art: "cloud", urteil: "riskant",
      status: "GA (Gemini API)",
      frei: { typ: "klein", stundenMonat: null, text: "Free Tier ohne veröffentlichte Menge; im eigenen Test nach wenigen Aufrufen „You exceeded your current quota“" },
      preis: { proStunde: null, text: "nach Audio-Tokens (32 je Sekunde), Preis je Stunde nicht in dieser Studie geprüft" },
      hf: null,
      deutsch: "Keine Deutsch-Rangliste. Eigene Messung sehr gut.",
      messung: ["gemini-3.8-flash", "gemini-3.8-flash+vokabular"],
      vokabular: "freier Prompt",
      streaming: "nein (eigene Live-Modelle)",
      halluzination: "ERFINDET bei Stille ganze Sätze: ohne Vokabular ein Bewerbungsdank an „Herrn Schneider“, mit Vokabular einen Satz über den Bug-Almanach (eigene Messung)",
      datenschutz: "wie Gemini API",
      android: "generateContent mit API-Key",
      pro: ["Eigene Messung: beste Eigennamen-Erkennung mit Vokabular"],
      contra: ["Halluziniert bei Stille, also ohne VAD-Sperre gefährlich", "Langsam (7–10 s)", "Kein dediziertes STT-Modell"],
      quellen: ["r3"]
    },
    {
      id: "gemini-lite", name: "Google Gemini 3.5 Flash-Lite (Audio-Prompt)", modell: "gemini-3.5-flash-lite", art: "cloud", urteil: "besser-gemessen",
      status: "GA (Gemini API)",
      frei: { typ: "freetier", stundenMonat: null, text: "Free Tier, laut Drittmessung etwa 15 Anfragen/Min. und 500/Tag; Daten im Free Tier, EWR-Klausel" },
      preis: { proStunde: 0.065, text: "≈ 0,06–0,07 $/h bezahlt (berechnet)" },
      hf: null,
      deutsch: "Keine Deutsch-Rangliste. Eigene Messung: weniger Fehler als Turbo auf sauberem Text, aber schwächer bei Eigennamen.",
      messung: ["gemini-3.5-flash-lite"],
      vokabular: "freier Prompt",
      streaming: "nein",
      halluzination: "bei Stille leer (eigene Messung); als LLM Risiko des Umformulierens",
      datenschutz: "wie Gemini API",
      android: "generateContent mit API-Key",
      pro: ["Billigste Gemini-Option", "Größeres Free-Tier-Limit als Transcribe"],
      contra: ["Eigennamen schlechter als Turbo mit Prompt", "Umformulierungsrisiko"],
      quellen: ["r3"]
    },
    {
      id: "chirp3", name: "Google Cloud STT v2 Chirp 3", modell: "chirp_3", art: "cloud", urteil: "nein",
      status: "GA",
      frei: { typ: "keins", stundenMonat: 0, text: "v2: keine Freiminuten (die 60 Min./Monat gelten nur für v1)" },
      preis: { proStunde: 0.96, text: "0,96 $/h (Dynamic Batch günstiger)" },
      hf: null,
      deutsch: "Keine unabhängige Deutsch-Zahl; Artificial Analysis 4,3 % gegen 4,6 % bei Turbo.",
      messung: [],
      vokabular: "Phrase Sets bis 1.000",
      streaming: "ja (gRPC)",
      halluzination: "Einzelbericht: Text bei Stille",
      datenschutz: "EU-Multiregion möglich",
      android: "OAuth nötig, kein API-Key; Franks Google-Schlüssel ist für die Speech-API gesperrt",
      pro: ["EU-Datenresidenz"],
      contra: ["Teuer, kein Freikontingent, kaum Vorsprung"],
      quellen: ["r3"]
    },
    {
      id: "gpt-transcribe", name: "OpenAI gpt-transcribe", modell: "gpt-transcribe (seit 29.07.2026)", art: "cloud", urteil: "besser",
      status: "GA; Vorgänger (whisper-1, gpt-4o-transcribe, gpt-4o-mini-transcribe) werden am 26.02.2027 abgeschaltet",
      frei: { typ: "keins", stundenMonat: 0, text: "kein Freikontingent, mindestens 5 $ Prepaid" },
      preis: { proStunde: 0.27, text: "0,27 $/h (Live-Variante gpt-live-transcribe 1,02 $/h)" },
      hf: { fl: 2.34, mcv: 3.34, rang: "11 von 41" },
      deutsch: "Laut HF deutlich besser als Turbo; nur eine unabhängige Quelle.",
      messung: [],
      vokabular: "prompt + keywords + languages",
      streaming: "Realtime-API (gpt-live-transcribe)",
      halluzination: "LLM-basiert; offene Bugs bei gpt-live-transcribe (Deltas brechen ab, Okt. 2026)",
      datenschutz: "kein Training; EU-Residenz nur mit Vertriebsfreigabe",
      android: "multipart wie Groq, nur Endpunkt und Modell tauschen",
      pro: ["Gleiche API-Form wie Groq", "keywords-Parameter für Eigennamen", "In Jarvis Live schon im Einsatz"],
      contra: ["Kein Freikontingent", "Nicht gemessen (PC-Schlüssel ungültig)"],
      quellen: ["r2", "r8"]
    },
    {
      id: "azure", name: "Microsoft Azure Speech", modell: "Echtzeit (F0/S0), Fast Transcription, LLM Speech, MAI-Transcribe-2", art: "cloud", urteil: "unklar",
      status: "GA; MAI-Transcribe-2 Preview seit 03.09.2026",
      frei: { typ: "dauerhaft", stundenMonat: 5, text: "F0: 5 h/Monat dauerhaft, aber NUR Echtzeit-STT, 1 gleichzeitige Anfrage; Fast/LLM/MAI ohne Gratisteil" },
      preis: { proStunde: 1.0, text: "Echtzeit 1,00 $/h; Fast Transcription 0,36 $/h; MAI-Transcribe-2 0,10 $/h befristet bis Jahresende, danach etwa 0,54 $/h" },
      hf: { fl: 1.83, mcv: 2.01, rang: "1 von 41 (azure-speech-07-2026)" },
      deutsch: "Widersprüchlich: HF-Platz 1, aber beim Mischen Deutsch-Englisch das schwächste System (17–21 % WER), und das Echtzeit-Standardmodell liegt laut AssemblyAI bei 9,1 % FLEURS-de. Welcher Azure-Modus bei HF gemessen wurde, ist offen.",
      messung: [],
      vokabular: "Phrase List bis ~2.000, LLM Speech mit Freitext-Prompt",
      streaming: "ja (Speech SDK)",
      halluzination: "unbelegt",
      datenschutz: "EU-Regionen; LLM/MAI nur northeurope",
      android: "Speech SDK für Android oder REST; Franks Azure-Schlüssel (TTS in Jarvis) liegt nur auf dem Handy",
      pro: ["5 h/Monat gratis", "Spitze in der HF-Rangliste"],
      contra: ["Gratis nur das schwächere Echtzeitmodell", "Schwach bei Anglizismen", "Nicht gemessen"],
      quellen: ["r4", "r8"]
    },
    {
      id: "deepgram", name: "Deepgram Nova-3", modell: "nova-3 (language=de)", art: "cloud", urteil: "unklar",
      status: "GA",
      frei: { typ: "einmalig", stundenMonat: null, einmaligStunden: 680, text: "200 $ Startguthaben einmalig, ohne Karte, ohne Ablauf ≈ 590–770 h" },
      preis: { proStunde: 0.26, text: "0,26 $/h (+0,08 $/h mit Keyterms)" },
      hf: null,
      deutsch: "Nicht in der HF-Rangliste. Hersteller: 8,8 % auf Produktionsaudio gegen 19,9 % Whisper; Kleinsttests 4,35 %.",
      messung: [],
      vokabular: "keyterm (je Begriff ein Parameter, KEIN Komma: sonst stiller Ausfall)",
      streaming: "ja, sehr schnell",
      halluzination: "unbelegt",
      datenschutz: "EU-Endpunkt api.eu.deepgram.com",
      android: "1 Request mit Binär-Audio",
      pro: ["Größtes Startguthaben (reicht bei 30 Min./Tag gut 3 Jahre)", "Schnellste API"],
      contra: ["Keine unabhängige Deutsch-Zahl", "Einmalig, nicht dauerhaft"],
      quellen: ["r5", "r10"]
    },
    {
      id: "assemblyai", name: "AssemblyAI Universal-3.5 Pro", modell: "universal-3-5-pro (Sync-API)", art: "cloud", urteil: "besser",
      status: "GA",
      frei: { typ: "einmalig", stundenMonat: null, einmaligStunden: 111, text: "50 $ einmalig, ohne Karte ≈ 111 h Sync bzw. 190–240 h Async" },
      preis: { proStunde: 0.45, text: "Sync 0,45 $/h, Async 0,21 $/h (+0,05 Keyterms)" },
      hf: { fl: 2.17, mcv: 2.69, rang: "4 von 41" },
      deutsch: "Belastbar besser: HF Platz 4 plus Medizin-Test (3,0 %, bestes von sechs).",
      messung: [],
      vokabular: "keyterms_prompt (100 Sync / 1.000 Async) + prompt",
      streaming: "ja (Leerlaufzeit wird mitberechnet)",
      halluzination: "unbelegt",
      datenschutz: "EU-Endpunkt ohne Training, ZDR bei Opt-out",
      android: "1 multipart-Request wie Groq",
      pro: ["Unabhängig belegt deutlich besser", "Gleiche Aufrufform wie Groq", "EU-Endpunkt"],
      contra: ["Nur 50 $ einmalig", "11-facher Turbo-Preis (Sync)"],
      quellen: ["r5", "r8"]
    },
    {
      id: "scribe", name: "ElevenLabs Scribe v2", modell: "scribe_v2", art: "cloud", urteil: "besser",
      status: "GA",
      frei: { typ: "klein", stundenMonat: 0.2, text: "Free-Plan 10.000 Credits ≈ 12 Min./Monat" },
      preis: { proStunde: 0.22, text: "0,22 $/h (+0,05 Keyterms); Realtime 0,39 $/h" },
      hf: { fl: 2.30, mcv: 2.19, rang: "3 von 41" },
      deutsch: "Belastbar besser: HF Platz 3 und bestes Modell beim Mischen Deutsch-Englisch.",
      messung: [],
      vokabular: "keyterms bis 1.000",
      streaming: "Scribe v2 Realtime (auf Common Voice deutlich schlechter, Text teils mehrfach)",
      halluzination: "unbelegt",
      datenschutz: "Training unbelegt, EU nur Enterprise",
      android: "1 multipart-Request",
      pro: ["Top-Qualität inklusive Anglizismen", "Moderater Preis"],
      contra: ["Freikontingent winzig"],
      quellen: ["r6", "r8"]
    },
    {
      id: "voxtral", name: "Mistral Voxtral", modell: "voxtral-mini-transcribe-2 / voxtral-small / *-transcribe-3-0", art: "cloud", urteil: "besser",
      status: "GA; Voxtral 3 Public Preview seit 20.10.2026",
      frei: { typ: "freetier", stundenMonat: null, text: "Free Mode (Experiment) ohne Karte; ob Audio enthalten ist und welche Limits gelten, ist unbelegt; Training im Free Mode erlaubt" },
      preis: { proStunde: 0.18, text: "0,18 $/h (Mini Transcribe V2), Voxtral 3: 0,21–0,24 $/h" },
      hf: { fl: 2.61, mcv: 3.19, rang: "12 von 41 (Small 24B)" },
      deutsch: "Belastbar besser (Small): HF, handy.computer und Mistral-Paper übereinstimmend.",
      messung: [],
      vokabular: "context_bias bis 100 Begriffe (für Englisch optimiert)",
      streaming: "Voxtral Realtime (auf Deutsch deutlich ungenauer)",
      halluzination: "unbelegt",
      datenschutz: "EU-Anbieter",
      android: "multipart, OpenAI-ähnlich",
      pro: ["EU-Anbieter", "Gute Deutsch-Werte zu niedrigem Preis", "Evtl. kostenlos im Free Mode"],
      contra: ["Free-Mode-Limits für Audio unbekannt", "Training im Free Mode"],
      quellen: ["r6", "r8", "r10"]
    },
    {
      id: "speechmatics", name: "Speechmatics Enhanced", modell: "enhanced", art: "cloud", urteil: "unklar",
      status: "GA",
      frei: { typ: "einmalig", stundenMonat: null, einmaligStunden: 133, text: "100 $ einmalig für Neukunden, 12 Monate; Monatsstunden seit 01.08.2026 abgeschafft" },
      preis: { proStunde: 0.75, text: "Enhanced 0,75 $/h (Melia 0,24 $/h)" },
      hf: { fl: 3.56, mcv: 1.99, rang: "8 von 41" },
      deutsch: "HF besser als Turbo, nur eine Quelle.",
      messung: [],
      vokabular: "Custom Dictionary mit sounds_like",
      streaming: "ja",
      halluzination: "unbelegt",
      datenschutz: "EU1, kein Training",
      android: "3 Schritte (Job, Abfrage, Ergebnis)",
      pro: ["Stark auf Common Voice"],
      contra: ["Teuer", "Umständlicher Ablauf"],
      quellen: ["r5", "r8"]
    },
    {
      id: "soniox", name: "Soniox v5", modell: "stt-async-v5", art: "cloud", urteil: "unklar",
      status: "GA",
      frei: { typ: "keins", stundenMonat: 0, text: "kein Startguthaben mehr seit 27.10.2025" },
      preis: { proStunde: 0.10, text: "≈ 0,10 $/h" },
      hf: { fl: 2.66, mcv: 4.62, rang: "16 von 41" },
      deutsch: "HF besser als Turbo, auf Common Voice nur knapp vor large-v3.",
      messung: [],
      vokabular: "context.terms (8.000 Tokens)",
      streaming: "ja",
      halluzination: "unbelegt",
      datenschutz: "kein Training, EU auf Antrag",
      android: "4–5 Schritte",
      pro: ["Sehr billig", "Mächtiger Kontext"],
      contra: ["Kein Freikontingent", "Umständlich"],
      quellen: ["r5", "r8"]
    },
    {
      id: "qwen", name: "Alibaba Qwen3-ASR-Flash", modell: "qwen3-asr-flash (DashScope International)", art: "cloud", urteil: "unklar",
      status: "GA",
      frei: { typ: "keins", stundenMonat: 0, text: "international kein Freikontingent belegt (nur Peking: 10 h)" },
      preis: { proStunde: 0.13, text: "≈ 0,13 $/h" },
      hf: null,
      deutsch: "Herstellerangabe FLEURS-de 3,03 / CV 3,53; die offene 1.7B-Variante steht bei HF auf Platz 18 (3,35 / 4,60).",
      messung: [],
      vokabular: "Kontext + Hotwords",
      streaming: "ja",
      halluzination: "unbelegt",
      datenschutz: "Singapur, keine EU",
      android: "Base64-Data-URL, max. 5 Min./10 MB",
      pro: ["Gute Herstellerzahlen, billig"],
      contra: ["Kein Freikontingent, kein EU-Standort"],
      quellen: ["r6", "r8"]
    },
    {
      id: "gladia", name: "Gladia Solaria-3", modell: "solaria-3", art: "cloud", urteil: "nein",
      status: "GA",
      frei: { typ: "einmalig", stundenMonat: null, einmaligStunden: 250, text: "50 € einmalig, 1 Jahr ≈ 185–312 h" },
      preis: { proStunde: 0.2, text: "0,16–0,27 €/h" },
      hf: null,
      deutsch: "FLEURS-de 10,9 % (Konkurrenzmessung), hinter dem Feld.",
      messung: [], vokabular: "Custom Vocabulary (Beta)", streaming: "ja", halluzination: "unbelegt",
      datenschutz: "Starter: Training an (Opt-out)", android: "2–3 Schritte",
      pro: ["Mittleres Startguthaben"], contra: ["Deutsch schwach", "Training standardmäßig an"],
      quellen: ["r5"]
    },
    {
      id: "aws", name: "Amazon Transcribe", modell: "de-DE", art: "cloud", urteil: "nein",
      status: "GA",
      frei: { typ: "klein", stundenMonat: 1, text: "60 Min./Monat für 12 Monate (Altkonten); Neukonten: Guthaben-Modell" },
      preis: { proStunde: 0.36, text: "Batch 0,36 $/h, Streaming 0,60 $/h (Frankfurt, AWS-Preisliste)" },
      hf: null,
      deutsch: "FLEURS-de 8,0 % (Konkurrenzmessung), deutlich hinter Whisper.",
      messung: [], vokabular: "Custom Vocabulary", streaming: "ja (SigV4)", halluzination: "unbelegt",
      datenschutz: "Region Frankfurt wählbar", android: "umständlich (S3 + Job oder signiertes Streaming)",
      pro: ["EU-Region"], contra: ["Schwach auf Deutsch", "Kaum gratis", "Umständlich"],
      quellen: ["r4"]
    },
    {
      id: "parakeet", name: "Lokal: NVIDIA Parakeet TDT 0.6B v3", modell: "nemo-parakeet-tdt-0.6b-v3 (int8, onnx-asr / sherpa-onnx)", art: "lokal", urteil: "gleich",
      status: "offen, CC-BY-4.0",
      frei: { typ: "lokal", stundenMonat: Infinity, text: "unbegrenzt, nur Strom" },
      preis: { proStunde: 0, text: "0 $" },
      hf: { fl: 4.16, mcv: 4.07, rang: "22 von 41" },
      deutsch: "Laut HF auf Common Voice besser als Turbo, auf FLEURS etwas schlechter. Eigene Messung: Zahlen schwach („30 5 Millisekunden“), bei Rauschen stark verschlechtert.",
      messung: ["lokal-parakeet-tdt-0.6b-v3"],
      vokabular: "nur im NeMo-Stack (Word Boosting)",
      streaming: "simuliert über VAD-Stücke",
      halluzination: "bei Stille leer (eigene Messung)",
      datenschutz: "bleibt auf dem Gerät",
      android: "sherpa-onnx (fertiges int8-Modell, ~640 MB); Echtgerät-Tempo auf dem Fold 8 unbelegt",
      pro: ["Unbegrenzt, offline, privat", "Eigennamen ohne Prompt besser als Turbo ohne Prompt"],
      contra: ["Zahlen und Rauschen schwach", "Auf dem Laptop-CPU etwa 12 s für 40 s Audio"],
      quellen: ["r7", "r8"]
    },
    {
      id: "canary", name: "Lokal: NVIDIA Canary-1B-v2", modell: "nemo-canary-1b-v2 (int8)", art: "lokal", urteil: "nein",
      status: "offen, CC-BY-4.0",
      frei: { typ: "lokal", stundenMonat: Infinity, text: "unbegrenzt" },
      preis: { proStunde: 0, text: "0 $" },
      hf: { fl: 3.43, mcv: 4.69, rang: "21 von 41" },
      deutsch: "HF besser als Turbo, eigene Messung aber die schwächste.",
      messung: ["lokal-canary-1b-v2"],
      vokabular: "nein", streaming: "nein", halluzination: "bei Stille leer (eigene Messung)",
      datenschutz: "lokal", android: "nicht belegt",
      pro: ["Offline"], contra: ["Eigene Messung schwach", "Langsam (17 s für 40 s Audio)"],
      quellen: ["r7"]
    },
    {
      id: "primeline", name: "Lokal: primeline Whisper Turbo German", modell: "primeline/whisper-large-v3-turbo-german (Apache-2.0)", art: "lokal", urteil: "unklar",
      status: "offen",
      frei: { typ: "lokal", stundenMonat: Infinity, text: "unbegrenzt, braucht eigene Hardware/Server" },
      preis: { proStunde: 0, text: "0 $ (eigener Betrieb)" },
      hf: null,
      deutsch: "Eigenangabe des Autors: −28 % WER gegenüber Turbo; kein unabhängiger Nachtest. Die Parakeet-Variante (parakeet-primeline) −19 %.",
      messung: [], vokabular: "initial_prompt", streaming: "nein", halluzination: "wie Whisper",
      datenschutz: "lokal", android: "whisper.cpp möglich, Tempo unbelegt",
      pro: ["Deutsch-Finetune, in jeder Whisper-Laufzeit nutzbar"], contra: ["Keine gehostete API", "Nicht gemessen"],
      quellen: ["r1", "r7"]
    },
    {
      id: "android", name: "Android On-Device-Erkennung", modell: "SpeechRecognizer (createOnDeviceSpeechRecognizer)", art: "lokal", urteil: "nein",
      status: "de-DE beta",
      frei: { typ: "lokal", stundenMonat: Infinity, text: "kostenlos, offline" },
      preis: { proStunde: 0, text: "0 $" },
      hf: null,
      deutsch: "Keine Qualitätsbelege; ML Kit Advanced nur auf Pixel 10/11.",
      messung: [], vokabular: "EXTRA_BIASING_STRINGS (API 33)", streaming: "ja", halluzination: "unbekannt",
      datenschutz: "Gerät", android: "Framework-API, Sprachpaket per triggerModelDownload holen",
      pro: ["Offline-Notnagel"], contra: ["Qualität unbelegt"],
      quellen: ["r3"]
    }
  ]
};
