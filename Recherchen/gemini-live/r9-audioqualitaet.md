# R9 — Einfluss der Audioqualität auf die Erkennungsgenauigkeit (Gemini Live API / Gemini-Transkription)

Recherchiert von Researcher 9 am 29.08.2026. Untersuchungsgegenstand: Muss man für `gemini-3.5-transcribe-live` (bzw. die Gemini Live API allgemein) lauter/näher sprechen, hilft Vorverarbeitung, und wie verhält sich das Modell bei Stille/Rauschen? Hintergrund ist ein Windows-Overlay, das per NAudio mit 16 kHz/16 bit/mono ohne Normalisierung oder Rauschunterdrückung aufnimmt und gelegentlich einzelne Wörter/Fachbegriffe falsch erkennt.

---

## a) Eingangspegel, Mikrofonabstand, Signal-Rausch-Abstand — offizieller Pegelbereich?

**BESTÄTIGT (offizielles Format, keine Pegelangabe):** Die Live API erwartet 16-bit-PCM-Audio, 16 kHz, mono, little-endian; jeder Sample-Rate wird intern resampelt, falls nötig. Eine Pegel-Empfehlung (dBFS/RMS) für den Input nennt Google in dieser Doku **nicht**. Quelle: [Live API capabilities guide](https://ai.google.dev/gemini-api/docs/live-api/capabilities)

**BESTÄTIGT (Best-Practices-Seite, ebenfalls ohne Pegelwert):** Die offizielle Best-Practices-Seite behandelt Chunking (20–40 ms Chunks, kein Puffern über 100 ms) und Resampling auf 16 kHz, aber **keine** Gain-/Volumen-/dBFS-Empfehlung, keine Mikrofonabstands-Angabe und keine SNR-Schwelle. Quelle: [Live API best practices](https://ai.google.dev/gemini-api/docs/live-api/best-practices)

**BESTÄTIGT (indirekter Hinweis auf Abstand):** Im Google-Cloud-Troubleshooting zur Live API wird empfohlen: „if the background noise level is too high, try to move the microphone closer to the user or use a microphone with better noise cancellation" — also eine qualitative, keine quantitative Empfehlung. Quelle: [Troubleshooting Gemini Live API (Google Cloud)](https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/live-api/troubleshooting)

**VERMUTUNG (Community/Praxis, nicht offiziell bestätigt):** Ein häufig zitierter Praxis-Richtwert bei Audio-Preprocessing-Pipelines (nicht Gemini-spezifisch) lautet, Segmente mit RMS-Amplitude < −45 dBFS oder Stille-Anteil > 82 % zu verwerfen, um Rauschen/Leere nicht unnötig zu verarbeiten — das ist aber eine allgemeine Preprocessing-Heuristik aus einem Blogartikel, keine Google-Vorgabe für Gemini. Quelle: [Google's Gemini 1.5 Pro Can Now Listen to Audio Files: Efficiency Analysis](https://lifetips.alibaba.com/tech-efficiency/googles-gemini-15-pro-can-now-listen-to-audio-files)

**BESTÄTIGT (allgemeine ASR-Forschung, als Vergleich, nicht Gemini-spezifisch):** Es gibt breite, gut belegte Evidenz aus der ASR-Forschung allgemein, dass SNR und Mikrofonabstand die Genauigkeit stark beeinflussen: Bei einem auf Nahbesprechung trainierten Erkenner sank die Worttreffergenauigkeit von 90,8 % auf 56,8 % auf 34,9 %, wenn die Sprecherentfernung zunahm. In Klassenzimmer-Settings (SNR typischerweise −7 dB bis +5 dB) stieg die Wortfehlerrate (WER) auf 0,78 gegenüber 0,54 im Labor. Unter ruhigen Bedingungen liegen aktuelle ASR-Systeme bei ca. 92 % Worttreffergenauigkeit (8 % WER), unter Störgeräuschen deutlich schlechter. Das ist allgemeine ASR-Physik (Signalabschwächung mit Distanz, Nachhall, additive Störgeräusche) und dürfte konzeptionell auch für Gemini gelten, ist aber **nicht** mit einer Gemini-spezifischen Messung belegt. Quellen: [Microphone Array Processing for Distant Speech Recognition](https://researchgate.net/publication/236134997_Microphone_Array_Processing_for_Distant_Speech_Recognition), [Estimate the noise effect on ASR accuracy (Mandarin)](https://www.sciencedirect.com/science/article/abs/pii/S0003682X23000154), [Challenges and Feasibility of Automatic Speech Recognition (Colorado)](https://www.colorado.edu/research/ai-institute/sites/default/files/attached-files/challengesfeasibility.pdf)

**Fazit zu a):** Google veröffentlicht für die Live API/Gemini-Transkription **keinen** konkreten dBFS-/RMS-Zielbereich. Offiziell wird nur qualitativ empfohlen, den Störgeräuschpegel niedrig zu halten bzw. das Mikrofon näher an den Sprecher zu bringen. Dass niedriger SNR und größerer Mikrofonabstand die Genauigkeit verschlechtern, ist durch allgemeine ASR-Forschung sehr gut belegt und lässt sich mit hoher Wahrscheinlichkeit auf Gemini übertragen — eine Gemini-eigene Messreihe dazu wurde in der Recherche nicht gefunden.

---

## b) Vorverarbeitung — Normalisierung, Verstärkung, Rauschunterdrückung, Hochpass, AGC: hilft oder schadet sie?

**BESTÄTIGT (offizielle Empfehlung zu AGC/Noise Suppression, Android-Kontext):** In einer Anleitung zum Bau einer Voice-AI-Android-App mit Gemini wird explizit empfohlen, `MediaRecorder.AudioSource.VOICE_COMMUNICATION` zu verwenden, weil das systemseitiges AGC und Noise Suppression aktiviert, „die die Lautstärke stabilisieren und Hintergrundgeräusche reduzieren, bevor das Audio das KI-Modell erreicht". Das ist zwar keine ai.google.dev-Kernseite, aber eine konkrete, technisch begründete Empfehlung **für** AGC/Rauschunterdrückung vor dem Senden. Quelle: [Prototyping a Voice AI Android App with Gemini 2.0 and WebSockets (WebRTC.ventures)](https://webrtc.ventures/2026/02/blog-voice-ai-android-app-gemini-prototype/)

**BESTÄTIGT (Gemini-3.5-Transcribe-Dokumentation, explizite Empfehlung):** Die Doku zur (Batch-)Audio-Transkription nennt ausdrücklich: „Provide clean audio: Ensure audio recordings have clear voice separation and avoid severe clipping." — also eine explizite Warnung vor Übersteuerung/Clipping, und die Empfehlung nach sauberer Sprachtrennung (z. B. bei mehreren Sprechern). Quelle: [Audio transcription | Gemini API](https://ai.google.dev/gemini-api/docs/transcribe)

**BESTÄTIGT (allgemein zu AGC/Normalisierung im Live-API-Kontext, nicht offiziell von Google, aber technisch fundiert):** Ein Artikel zu Gemini Live API nennt AGC als client-seitig empfohlene Technik zur Pegel-Normalisierung, gerade weil Mikrofone stark variieren: „Variations in microphones demand client-side audio processing like VAD to avoid streaming costly silence, and AGC to normalize volume." Quelle: [Gemini Live API: Build Low-Latency Voice AI Apps](https://i10x.ai/news/gemini-live-api-real-time-voice-conversations)

**VERMUTUNG (allgemeine ASR-Praxis, Community-Diskussion, nicht Gemini-spezifisch):** In einer GitHub-Issue-Diskussion zur Python-`speech_recognition`-Bibliothek wird berichtet, dass Lautstärke-Normalisierung vor dem Senden an ASR-APIs die Erkennung „fast immer" verbessert (gemessen an WER) — das bezieht sich aber nicht auf Gemini konkret, sondern auf ASR-Cloud-APIs allgemein, und ist Erfahrungsbericht, keine kontrollierte Studie. Quelle: [Volume Normalization before sending Audio to API Services · Issue #282 · Uberi/speech_recognition](https://github.com/Uberi/speech_recognition/issues/282)

**Wichtige Einschränkung/Gegenwarnung (BESTÄTIGT, allgemeines ASR-Wissen + Gemini-Doku):** Übersteuerung/Clipping ist klar als schädlich dokumentiert (siehe Zitat oben, „avoid severe clipping"). Aggressive Normalisierung/Verstärkung nach der Aufnahme kann genau das erzeugen, wenn bereits knapp unter Vollaussteuerung aufgenommen wurde, oder kann Rauschen mit anheben, wenn keine echte Rauschunterdrückung (sondern nur Gain) angewendet wird. Eine reine Peak-Normalisierung (Verstärkung bis knapp unter 0 dBFS, ohne Kompression/Limiting) hebt Nutzsignal UND Rauschboden gleichermaßen an und verbessert den SNR **nicht** — das ist Audio-Grundlagenwissen, keine Gemini-spezifische Quelle, aber unstrittig.

**Fazit zu b):** Es gibt keine Gemini-eigene A/B-Messung „Normalisierung ja/nein → WER", aber die vorhandenen offiziellen und community-basierten Hinweise zeigen ein konsistentes Bild: (1) Clipping/Übersteuerung aktiv vermeiden ist offiziell dokumentiert und schädlich für die Erkennung; (2) AGC/Noise Suppression VOR der Aufnahme (system-/treiberseitig, z. B. `VOICE_COMMUNICATION`-Audioquelle) wird empfohlen und ist sinnvoll, weil sie den SNR tatsächlich verbessert (Rauschunterdrückung entfernt Rauschen, statt es nur mit hochzuskalieren); (3) reine nachträgliche Lautstärke-Normalisierung ohne echte Rauschunterdrückung bringt vermutlich wenig bis nichts, solange der Originalpegel nicht schon zu leise für die interne Verarbeitung war und kein Clipping vorliegt.

---

## c) Offiziell empfohlenes Eingangsformat — hilft eine höhere Abtastrate (24/48 kHz)?

**BESTÄTIGT:** Offizielles Format ist 16-bit PCM, 16 kHz, mono, little-endian. Zitat: „Ensure your client application resamples microphone input (often 44.1kHz or 48kHz) to 16kHz before transmission." Wird ein höherer Sample-Rate gesendet, resampelt die API automatisch intern auf 16 kHz — man kann also technisch auch 44,1/48 kHz senden, es wird aber ohnehin auf 16 kHz heruntergerechnet. Quellen: [Live API capabilities guide](https://ai.google.dev/gemini-api/docs/live-api/capabilities), [Live API best practices](https://ai.google.dev/gemini-api/docs/live-api/best-practices)

**BESTÄTIGT (allgemeine ASR-Vergleichsmessung, nicht Gemini-spezifisch):** Ein Anbieter-Test (AmiVoice) fand „almost no difference in accuracy when recognizing data with a sampling rate of 48kHz or 16kHz", weil intern ohnehin auf 16-kHz-Äquivalent heruntergerechnet wird — Sprachenergie liegt größtenteils unter 8 kHz, was die Nyquist-Grenze von 16 kHz für Sprache ausreichend macht. Quelle: [How does sampling rate and compression rate affect speech recognition accuracy? (AmiVoice Techblog)](https://acp.amivoice.com/en/blog/2025-07-29/)

**Gegenläufige VERMUTUNG (ein Anbieter, Marketing-nah, nicht unabhängig verifiziert):** Ein anderer Anbieterartikel behauptet einen messbaren Genauigkeitsgewinn beim Wechsel von 16 kHz auf 48 kHz, mit Argument, dass Frequenzanteile über 8 kHz bei der Unterscheidung ähnlicher Laute (z. B. Zischlaute) helfen. Das steht im Widerspruch zur AmiVoice-Messung und wirkt wie Eigenwerbung für ein 48-kHz-Produkt; ohne unabhängige Prüfung als unbelegt einzustufen. Quelle: [The 48kHz Difference: Why Audio Fidelity Matters for Speech Recognition (Air)](https://www.tryair.ai/technology/48khz-audio-fidelity)

**Fazit zu c):** Für die Gemini Live API konkret ist die Sache eindeutig: Sie verarbeitet Sprache nativ bei 16 kHz und rechnet höhere Sample-Raten ohnehin herunter. Ob eine höhere Quell-Abtastrate (z. B. 48 kHz mit sauberem Antialiasing-Filter beim Downsampling) minimal genauer ist als eine native 16-kHz-Aufnahme, ist in der ASR-Literatur umstritten und für Gemini nicht spezifisch gemessen — der Effekt wäre, wenn überhaupt vorhanden, klein. Für das beschriebene NAudio-Setup (bereits nativ 16 kHz, 16 bit, mono) besteht hier vermutlich kein Hebel.

---

## d) Empfehlungen zum Sprechstil (Tempo, Aussprache, Pausen)

**Kein Gemini-spezifischer Beleg gefunden.** Google veröffentlicht in der Live-API-Doku keine Sprechstil-Empfehlungen.

**BESTÄTIGT (allgemeine ASR-/HCI-Forschung, als Vergleich):** Untersuchungen zu Sprechtempo und Spracherkennung zeigen: Optimal ist ein Bereich von ca. 0,5×–1,25× normaler Sprechgeschwindigkeit, mit bester Genauigkeit oft bei ca. 0,75× (also etwas langsamer als normal). Zu schnelles Sprechen verstärkt Koartikulationseffekte (Lautverschleifungen), zu langsames Sprechen führt zu unnatürlichen Pausen zwischen Phonemen, die ebenfalls Fehler begünstigen können. Nutzer passen sich empirisch beobachtet ohnehin an: Sowohl Muttersprachler als auch Nicht-Muttersprachler verlangsamen ihre Sprechgeschwindigkeit bei Interaktion mit Spracherkennung um 34–40 %, meist um die Trefferquote zu erhöhen. Klare, deutliche Aussprache („clear speech") verbessert die Erkennung nachweislich. Quellen: [Effects of Speaking Rate on Speech and Silent Speech Recognition (ACM)](https://dl.acm.org/doi/fullHtml/10.1145/3491101.3519611), [Clear speech promotes speaking rate normalization (PMC)](https://pmc.ncbi.nlm.nih.gov/articles/PMC11303017/), [Factors that impact the accuracy and quality of speech recognition (Fireflies)](https://guide.fireflies.ai/articles/7779872772-factors-that-impact-the-accuracy-and-quality-of-speech-recognition)

**Fazit zu d):** Es gibt keine Gemini-eigene Studie, aber die allgemeine ASR-Evidenz ist konsistent und dürfte übertragbar sein: mäßig langsames, deutliches Sprechen mit klarer Artikulation verbessert die Trefferquote; sehr schnelles oder stark verschlifenes Sprechen (typisch bei Fachbegriffen, die man „routiniert" schnell ausspricht) begünstigt Fehler — was zur beobachteten Symptomatik im Overlay passt (einzelne Wörter/Fachbegriffe falsch erkannt).

---

## e) Verhalten bei Stille/reinem Rauschen — Halluzination wie bei Whisper?

**BESTÄTIGT (Whisper-Vergleich, als Referenz, klar gekennzeichnet als Nicht-Gemini):** Whisper ist gut dokumentiert dafür, bei Stille oder Stille+Hintergrundmusik Floskeln wie „Thank you for watching!" zu halluzinieren. Ursache: Trainingsdaten aus YouTube-Untertiteln, bei denen Stille/Outro-Musik oft von Dank-Floskeln gefolgt wird — das Modell reproduziert dieses gelernte Muster. Eine Studie („Careless Whisper") fand, dass ca. 1 % der Transkriptionen komplett halluzinierte Sätze/Phrasen enthielten, häufiger bei längeren Stille-Passagen im Audio. Fix in der Praxis: Stille vor dem Senden trimmen, aber Pausenstruktur für Satzgrenzen erhalten. Quellen: [Whisper hallucinations ("Thank you for watching!") during silence — needs smarter VAD (GitHub)](https://github.com/OpenWhispr/openwhispr/issues/462), [Whisper's Broken Record: Why Silence Makes Speech-to-Text Talk to Itself](https://yage.ai/share/whisper-repetition-hallucination-en-20260526.html), [Hallucinations in speech recognition (Soniox Voice AI Wiki)](https://soniox.com/wiki/asr-hallucinations)

**BESTÄTIGT, aber als Werbeaussage eines Drittanbieters einzuordnen (mit Vorsicht zu behandeln):** Ein Blogartikel eines ASR-Anbietervergleichs behauptet, Gemini 3.5 Transcribe liefere „deterministic transcription with zero silent hallucinations" dank „explicit voice activity conditioning inside its Conformer-2 acoustic encoder", das die Token-Ausgabe unterdrückt, wenn die Audioenergie unter eine Basislinie fällt. Das ist eine **nicht unabhängig verifizierte** technische Behauptung von einer Drittseite (autointerviewai.com), keine offizielle Google-Aussage, und „zero hallucinations" ist eine sehr starke, wahrscheinlich übertriebene Behauptung. Als VERMUTUNG einzustufen, nicht als Beleg. Quelle: [Gemini 3.5 Transcribe vs OpenAI GPT Live Transcribe (Auto Interview AI)](https://www.autointerviewai.com/blog/gemini-3-5-transcribe-vs-openai-gpt-live-transcribe-2026)

**BESTÄTIGT (Gemini-eigene Probleme im Forum, andere Art von Fehlverhalten als Whisper):** Im Google AI Developer Forum berichten mehrere Nutzer von `inputTranscription`-Halluzinationen bei der Live API — allerdings anders gelagert als Whisper: Es werden spontane, dem Nutzer nicht zuzuordnende Transkriptionsereignisse gemeldet (z. B. Zeichen aus fremden Schriftsystemen wie Hindi bei englischer Sprache, oder zufällige Sprachen), teils ohne dass serverseitiges VAD überhaupt Sprache erkannt hätte („NO VAD logging is shown, so that it indicates that it is a Google API error/problem" — Zitat eines betroffenen Entwicklers). Eine offizielle Stellungnahme oder ein Preprocessing-bezogener Fix wird im Thread nicht genannt; der Thread bleibt als offene Frage stehen. Quelle: [Gemini Live API models 'inputTranscription' hallucinations (Google AI Developer Forum)](https://discuss.ai.google.dev/t/gemini-live-api-models-inputtranscription-hallucinations/107899)

**Fazit zu e):** Ob Gemini bei reiner Stille/Rauschen wie Whisper systematisch Floskeln halluziniert, ist **nicht eindeutig belegt** — die einzige Quelle, die „keine Stille-Halluzinationen" behauptet, ist eine nicht-offizielle Drittanbieterseite mit Marketing-Charakter. Belegt ist dagegen aus dem offiziellen Google-Forum, dass die Live API eine ANDERE Art von Fehlverhalten zeigt: spontane, teils sprachfremde Transkriptions-Artefakte auch ohne serverseitig erkanntes VAD-Ereignis. Das deutet eher auf gelegentliche Modell-/Pipeline-Artefakte als auf ein klassisches „Stille → Dankesfloskel"-Muster wie bei Whisper hin, ist aber insgesamt dünn belegt.

---

## Offen / unbelegt

- Kein offizieller dBFS-/RMS-Zielbereich von Google für Gemini-Live-Audio-Input gefunden.
- Keine Gemini-eigene, kontrollierte Messung (A/B-Test) zu Normalisierung/AGC vor dem Senden → WER-Effekt.
- Keine Gemini-eigene Messung zu 16 kHz vs. 24/48 kHz Quell-Sample-Rate bei identischem Downsampling-Pfad.
- Keine belastbare, unabhängig verifizierte Aussage darüber, ob `gemini-3.5-transcribe-live` bei Stille/Rauschen halluziniert oder nicht — die einzige klare "nein"-Aussage stammt von einer nicht-offiziellen, werbenden Drittquelle.
- Keine Gemini-spezifische Sprechtempo-/Aussprache-Studie; alle Aussagen zu Tempo/Aussprache sind aus allgemeiner ASR-Forschung übertragen.

## Quellenliste

- [Live API capabilities guide](https://ai.google.dev/gemini-api/docs/live-api/capabilities)
- [Live API best practices](https://ai.google.dev/gemini-api/docs/live-api/best-practices)
- [Audio transcription | Gemini API](https://ai.google.dev/gemini-api/docs/transcribe)
- [Troubleshooting Gemini Live API (Google Cloud)](https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/live-api/troubleshooting)
- [Gemini Live API models 'inputTranscription' hallucinations (Google AI Developer Forum)](https://discuss.ai.google.dev/t/gemini-live-api-models-inputtranscription-hallucinations/107899)
- [Prototyping a Voice AI Android App with Gemini 2.0 and WebSockets (WebRTC.ventures)](https://webrtc.ventures/2026/02/blog-voice-ai-android-app-gemini-prototype/)
- [Gemini Live API: Build Low-Latency Voice AI Apps (i10x.ai)](https://i10x.ai/news/gemini-live-api-real-time-voice-conversations)
- [Gemini 3.5 Transcribe vs OpenAI GPT Live Transcribe (Auto Interview AI)](https://www.autointerviewai.com/blog/gemini-3-5-transcribe-vs-openai-gpt-live-transcribe-2026)
- [Google's Gemini 1.5 Pro Can Now Listen to Audio Files: Efficiency Analysis](https://lifetips.alibaba.com/tech-efficiency/googles-gemini-15-pro-can-now-listen-to-audio-files)
- [Volume Normalization before sending Audio to API Services · Issue #282 · Uberi/speech_recognition (GitHub)](https://github.com/Uberi/speech_recognition/issues/282)
- [Whisper hallucinations ("Thank you for watching!") during silence (GitHub)](https://github.com/OpenWhispr/openwhispr/issues/462)
- [Whisper's Broken Record: Why Silence Makes Speech-to-Text Talk to Itself](https://yage.ai/share/whisper-repetition-hallucination-en-20260526.html)
- [Hallucinations in speech recognition (Soniox Voice AI Wiki)](https://soniox.com/wiki/asr-hallucinations)
- [Microphone Array Processing for Distant Speech Recognition](https://researchgate.net/publication/236134997_Microphone_Array_Processing_for_Distant_Speech_Recognition)
- [Estimate the noise effect on ASR accuracy for Mandarin (ScienceDirect)](https://www.sciencedirect.com/science/article/abs/pii/S0003682X23000154)
- [Challenges and Feasibility of Automatic Speech Recognition (Colorado)](https://www.colorado.edu/research/ai-institute/sites/default/files/attached-files/challengesfeasibility.pdf)
- [How does sampling rate and compression rate affect speech recognition accuracy? (AmiVoice Techblog)](https://acp.amivoice.com/en/blog/2025-07-29/)
- [The 48kHz Difference: Why Audio Fidelity Matters for Speech Recognition (Air)](https://www.tryair.ai/technology/48khz-audio-fidelity)
- [Effects of Speaking Rate on Speech and Silent Speech Recognition (ACM)](https://dl.acm.org/doi/fullHtml/10.1145/3491101.3519611)
- [Clear speech promotes speaking rate normalization (PMC)](https://pmc.ncbi.nlm.nih.gov/articles/PMC11303017/)
- [Factors that impact the accuracy and quality of speech recognition (Fireflies)](https://guide.fireflies.ai/articles/7779872772-factors-that-impact-the-accuracy-and-quality-of-speech-recognition)

---

## Konkrete Empfehlung: Lohnt sich Normalisierung vor dem Senden?

**Ja, mit Einschränkung.** Eine reine Peak-Normalisierung (Lautstärke anheben ohne Rauschunterdrückung) hat vermutlich wenig Nutzen, weil sie Nutzsignal und Rauschboden gleichermaßen anhebt und den SNR nicht verbessert — außer die Aufnahme ist tatsächlich zu leise für eine saubere interne Verarbeitung, was bei 16-bit/16-kHz-NAudio-Aufnahmen mit normalem Sprechpegel unwahrscheinlich ist. Sinnvoller als nachträgliche Normalisierung wäre:

1. **Clipping aktiv vermeiden** (offiziell dokumentiert als schädlich) — Eingangspegel so wählen, dass auch laute Passagen nicht übersteuern, eher mit Sicherheitsabstand nach unten.
2. **Echte Rauschunterdrückung/AGC vor bzw. während der Aufnahme** (treiber-/OS-seitig, z. B. eine dem `VOICE_COMMUNICATION`-Modus vergleichbare Windows-Audioquelle mit aktiviertem Noise Suppression/AGC), statt nachträglicher Software-Normalisierung — das verbessert den SNR tatsächlich, statt ihn nur zu skalieren.
3. **Sample-Rate/Format lassen wie es ist** (16 kHz/16 bit/mono passt exakt zum nativen Gemini-Format, kein Hebel zu erwarten).
4. Die beobachteten Fehler bei einzelnen Fachbegriffen sind nach der ASR-Literatur eher durch Sprechtempo/Artikulation und fehlende Sprachmodell-Priorisierung seltener Fachwörter erklärbar als durch reine Pegelprobleme — hier könnte ein Vokabular-/Kontext-Hinweis an das Modell (falls von der API unterstützt) mehr bringen als Audio-Preprocessing. Das ist aber Thema anderer Unterthemen in diesem Recherche-Schwarm (Vokabular/Sprache), nicht dieses Berichts.
