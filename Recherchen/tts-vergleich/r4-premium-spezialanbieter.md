# R4 — Premium-Spezialanbieter für deutsches TTS (ElevenLabs, Cartesia, Inworld, Hume)

Recherchezeitpunkt: 21.10.2026. Alle Links wurden am 21.10.2026 abgerufen; Ausnahmen sind einzeln datiert. Methode: nur WebSearch/WebFetch. Die Seiten wurden über ein kleines Zusammenfassungsmodell gelesen, deshalb stehen Zahlen aus Tabellen mit dem Vermerk „laut Abruf“. Unbelegtes ist mit **[unbelegt]** oder **[Widerspruch]** markiert. Es wurde nichts angehört und nichts gegen die APIs getestet; alle Aussprache-Aussagen sind Dokumentationslage, nicht Messung.

Bezug: bestehender Stand in `best-practices/apis/tts-provider.md` §17 (Stand 28.09.2026). Dieser Bericht ersetzt ihn dort, wo Zahlen abweichen (siehe Abschnitt „Abgrenzung zu §17").

---

## 1. Kurzfazit

1. **Hume (Octave) scheidet aus.** Hume schaltet die TTS- und EVI-APIs ab; Zugang endet am 13.11.2026, 00:01 Uhr EST, danach werden Kontodaten gelöscht ([Hume Changelog](https://dev.hume.ai/voice/changelog), Eintrag vom 02.10.2026; [Hume TTS-Doku](https://dev.hume.ai/docs/text-to-speech-tts/overview)).
2. **ElevenLabs hat mit Eleven v4 / v4 Turbo (28.09.2026) die Spitze der Artificial-Analysis-Arena übernommen** (Provider Voice: v4 Turbo 1335, v4 1329, Sonic 3.6 1289, Inworld Realtime TTS-2 1267). §17 nennt Cartesia/Inworld als Spitze; das ist überholt, zumindest für die englisch dominierte Gesamtarena.
3. **Deutsch-spezifisch bleibt die Lage unscharf.** Das deutsche Controlled-Voice-Board existiert (seit September), die Live-Werte konnten nicht abgerufen werden. Belegt ist nur der Stand vom 22.09.2026 (Sonic 3.6 vorn, Inworld TTS-2 1261 Elo, 22 Punkte dahinter) — vor dem Start von v4. Eine Zweitquelle behauptet, v4 Turbo führe inzwischen auf Deutsch; das ist **nicht gegen das Live-Board geprüft**.
4. **Freikontingent: keiner der drei reicht auch nur annähernd an Google Chirp 3 HD (1 Mio Zeichen/Monat) heran.** ElevenLabs 10.000 Credits/Monat, Cartesia 20.000 Credits/Monat, Inworld „bis zu 70 Minuten“ (ob wiederkehrend, ist **nicht belegt**). Free-Pläne von ElevenLabs sind ausdrücklich nicht kommerziell; bei Cartesia ist Kommerzielles im Free-Plan nicht aufgeführt und die AGB verbieten es standardmäßig.
5. **Preislich ist Inworld der günstigste Premium-Anbieter** (Pay-as-you-go ohne Grundgebühr: 25 $ bzw. Flash 15 $ je 1 Mio Zeichen; Plan Creator 20 $/10 $). ElevenLabs v4 Turbo 40 $, v4 80 $; Cartesia 38–65 $ je 1 Mio Credits je nach Stufe.
6. **Kein Anbieter hat ein offizielles Kotlin/Java-TTS-SDK.** ElevenLabs' Kotlin-SDK ist nur für Agents; alle drei sind per REST/WebSocket ansprechbar. Inworld bietet seit 11.09.2026 einen OpenAI-kompatiblen Endpunkt.

---

## 2. Vergleichstabelle

| Kriterium | ElevenLabs | Cartesia | Inworld | Hume |
|---|---|---|---|---|
| Aktuelle Modell-IDs | `eleven_v4`, `eleven_v4_turbo`, `eleven_v3`, `eleven_flash_v2_5`, `eleven_multilingual_v2` (`eleven_turbo_v2_5` veraltet) | `sonic-3.6` (Alias auf neuesten Snapshot), `sonic-3.6-2026-08-27`, `sonic-3.5`, `sonic-3` | `inworld-tts-2`, `inworld-tts-2-flash` (TTS-1.5 veraltet, TTS-1/-1-Max seit 15.06.2026 eingestellt) | Octave 1/2 (Preview) — **API endet 13.11.2026** |
| Deutsch | ja, nativ (v4: 90+ Sprachen; Flash 2.5: 32; Multilingual v2: 29) | ja (`de`), Sonic 3.6 44 Sprachen | ja, „Tier 1“ (`de`/`de-DE`) | nur Octave 2 |
| AA Provider Voice Elo (Okt. 2026) | v4 Turbo 1335, v4 1329, v3 Conversational 1214, v3 1183 | Sonic 3.6 1289, Sonic 3.5 1191 | Realtime TTS-2 1267, TTS-2 Flash 1218 | nicht gelistet |
| Preis je 1 Mio Zeichen (API) | v4 80 $, v4 Turbo 40 $, v3 80 $, Flash/Turbo 40 $ | 1 Credit = 1 Zeichen; 38–65 $ je 1 Mio Credits (Überziehung) bzw. 37–50 $ effektiv im Abo | TTS-2 25 $, Flash 15 $ (On-Demand); 12,50/7 $ ab Growth | — |
| Einstiegsabo | Starter 6 $/Monat | Pro 5 $/Monat (100K Credits) | kein Abo nötig (On-Demand 0 $); Creator 25 $/Monat | — |
| Freikontingent | 10.000 Credits/Monat; API-Preisseite: 10.000 v4-Zeichen bzw. 20.000 Flash/Turbo | 20.000 Credits/Monat | „bis zu 70 Minuten TTS“ (Wiederkehr **[unbelegt]**) | — |
| Kommerziell im Free-Plan | nein (ausdrücklich) | nicht aufgeführt; AGB: ohne Tarif-Freigabe nur nicht-kommerziell | auf der Preisseite unter On-Demand „Commercial license“ aufgeführt (Zuordnung der Tabelle unklar) | — |
| Namensnennung (Free) | ja („elevenlabs.io“ oder „11.ai“ im Titel) | keine in den AGB | **[unbelegt]** | — |
| API im Free-Plan | **[unbelegt]**; Free + Pay-as-you-go erlaubt API | implizit ja (TTS-Concurrency 2 im Free-Plan gelistet) | ja (On-Demand mit 5 gleichzeitigen Anfragen) | — |
| Kreditkarte nötig | Free: nein (nicht ausdrücklich belegt); PAYG: Zahlungsmittel nötig | **[unbelegt]** | Anmeldung: **[unbelegt]** | — |
| Concurrency (Free) | 2 (Free) bzw. 3 mit PAYG | 2 | 5 | — |
| Kostenbremse | PAYG ist vorausbezahlt, „no overage charges“; Altmodell „usage based billing“ mit einstellbarer Obergrenze | Überziehung standardmäßig aus, sonst „new requests will be blocked“ | Credits kaufen (min. 10 $) + optionales Auto-Reload; Verhalten bei leerem Guthaben **[unbelegt]** | — |
| SSML | nein bei v3/v4 (`<break>` nur ältere Modelle); `<phoneme>` nur `eleven_flash_v2` | kein SSML im engeren Sinn; Tags `speed`, `volume`, `emotion` (Beta), `break`, `spell` | nur `<break>` (max. 20, max. 10 s), `<say-as interpret-as="verbatim">` | — |
| Aussprache | Aussprachewörterbücher (PLS, Alias + Phonem), v4: IPA in `/…/` | Wörterbücher per `pronunciation_dict_id`, Inline-IPA `<<a\|b\|c>>` (englisch dokumentiert) | Inline-IPA `/…/` (englisch optimiert), kein Wörterbuch | — |
| Textnormalisierung | `apply_text_normalization` auto/on/off + `language_code` | `normalization` auto/off/Sprach- bzw. Locale-Code | `applyTextNormalization` ON/OFF/unspecified + `language` | — |
| Audio-/Stil-Tags | `[laughs]`, `[whispers]` usw. (v4) | `emotion` (Beta) | `[whisper]`, `[laugh]`, `[sigh]` (nur TTS-2); Anweisungen englisch | — |
| Streaming | REST-Stream; WebSocket (`stream-input` nicht für v3/v4, dort „Text-to-Dialogue-WebSocket“) | WebSocket mit Kontexten, `max_buffer_delay_ms` | HTTP-Stream, WebSocket, OpenAI-kompatibler Endpunkt | — |
| Latenz (Anbieterangabe) | v4 Turbo ~100 ms Inferenz, ~150 ms bis zur ersten Sprache; Flash ~75 ms | „sub-90 ms“ (Marketing) | P90 TTFB 100 ms (TTS-2), 20 ms (Flash), serverseitig | — |
| Android-SDK | keines für TTS (Kotlin-SDK = nur Agents) | keines (nur JS/Python) | keines (Python, Node, OpenAI-kompatibel) | — |
| EU-Region / ZDR | EU-Residenz und Zero Retention nur Enterprise | EU-Region und ZDR nur Enterprise | EU-Region und ZDR nur Enterprise (ZDR als Zusatz bei Growth) | — |
| Training mit Kundendaten | **[nicht geprüft]** | laut AGB möglich, Opt-out per Formular | **[unbelegt]** | — |

---

## 3. Profil ElevenLabs

### 3.1 Modelle und Deutsch
- Modellliste ([Modelle](https://elevenlabs.io/docs/overview/models)): `eleven_v4` (aktuelles Flaggschiff, 90+ Sprachen, Deutsch inklusive, 10.000 Zeichen je Anfrage), `eleven_v4_turbo` (~100 ms mediane Inferenz, 90+ Sprachen), `eleven_v3` („previous generation“, 70+ Sprachen, 5.000 Zeichen), `eleven_multilingual_v2` (29 Sprachen), `eleven_flash_v2_5` (32 Sprachen inkl. Deutsch, ~75 ms, 40.000 Zeichen), `eleven_turbo_v2_5` (veraltet, durch Flash zu ersetzen), `eleven_flash_v2` (nur Englisch).
- Release Eleven v4 und v4 Turbo: 28.09.2026 laut [Blogpost](https://elevenlabs.io/blog/create-realistic-transatlantic-accent-text-to-speech) und [Windows Report](https://windowsreport.com/?p=1510396); [OpenRouter](https://openrouter.ai/elevenlabs/eleven-v4) nennt dagegen 07.10.2026 **[Widerspruch, für die Studie unerheblich]**.
- Zeichenlimit für v4: [Modellseite](https://elevenlabs.io/docs/overview/models) 10.000, der Blogpost nennt im Modellkasten 5.000 **[Widerspruch]**.
- Deutsche Stimmen: Offiziell sind alle vorgefertigten und generierten Stimmen englischbasiert und können im Deutschen einen falschen Akzent haben; deutschsprachige Stimmen kommen als Bibliotheksstimmen mit Sprach-Tag, filterbar nach Deutsch ([Hilfe](https://help.elevenlabs.io/hc/en-us/articles/19450861739409-Which-voices-in-the-voice-library-are-native-to-a-specific-language)). Eine belastbare Anzahl deutscher Stimmen liegt nicht vor **[unbelegt]**; eine Drittseite zeigt exemplarisch 40 deutsch markierte Stimmen ([Famulor](https://www.famulor.io/voices/elevenlabs/languages/de), nur Ausschnitt). v4 behält den Akzent der Referenzstimme, wenn die Sprache passt, und strebt Muttersprachlichkeit an, wenn sie abweicht ([Eleven v4](https://elevenlabs.io/docs/overview/capabilities/text-to-speech/eleven-v4.md)).
- Lizenz für Bibliotheksstimmen im kommerziellen Einsatz: nicht geprüft **[unbelegt]**.

### 3.2 Freikontingent im Wortlaut
- Free-Plan: „10k credits per month“ ([Preise](https://elevenlabs.io/pricing)). Die Seite nennt als Concurrency 2 (Free), 3 (Starter), 5 (Creator), 10 (Pro), 15 (Scale); die [API-Preisseite](https://elevenlabs.io/pricing/api) nennt in einer anderen Tabelle 4/6/10/20/30/40 (gehört laut Abruf zu Realtime/Speech Engine, nicht zu TTS-Einzelanfragen) **[Widerspruch / unklar]**.
- Umrechnung: Basis 1 Credit je Zeichen (UI); über die API günstiger, Flash/Turbo v2.5 halbe Credits je Zeichen ([FAQ](https://elevenlabs.io/docs/help-center/technical/what-models-do-you-offer-and-what-is-the-difference-between-them/llms.txt), [Blogpost v4](https://elevenlabs.io/blog/create-realistic-transatlantic-accent-text-to-speech)). Die [API-Preisseite](https://elevenlabs.io/pricing/api) führt für die Free-/PAYG-Zeile 10.000 v4-Zeichen bzw. 20.000 Zeichen für v4 Turbo und Flash/Turbo, Starter 75.000/150.000, Creator 275.000/550.000. Ob diese Zeile ein Monatskontingent ist, steht dort nicht **[unbelegt]**. Der Planseite zufolge hat Starter nur 30.000 Credits (Creator 121.000); die beiden Seiten sind nicht deckungsgleich **[Widerspruch, API-Seite scheint eigene ElevenAPI-Kontingente zu führen]**.
- Kommerziell: „The free plan does not include a commercial license and cannot be used for any commercial purpose.“ Namensnennung: Inhalte aus dem Free-Plan oder ohne Anmeldung müssen mit „elevenlabs.io“ oder „11.ai“ im Titel gekennzeichnet werden. „All paid plans include a commercial license, provided you're not using Beta Services.“ ([Rechts-FAQ](https://elevenlabs.io/docs/help-center/legal/can-i-publish-the-content-i-generate-on-the-platform)).
- API im Free-Plan: weder Preis-, Abrechnungs- noch Schnellstartseite sagen es ausdrücklich **[unbelegt]**. Pay-as-you-go dient dazu, auf Free-Konten gesperrte APIs wie Music und Sound Effects freizuschalten; daraus folgt indirekt, dass TTS-API nicht zu diesen gesperrten gehört ([PAYG](https://elevenlabs.io/docs/overview/administration/pay-as-you-go.md)). Free-Konten werden bei VPN/Proxy oder Mehrfachkonten von einer Missbrauchserkennung gesperrt, bezahlte Konten nicht ([Hilfe](https://elevenlabs.io/docs/help-center/account/general/what-does-unusual-activity-detected-free-tier-usage-disabled-if-you-are-using-proxy-vpn-you-might-need-to-purchase-a-paid-plan-to-not-trigger-our-abuse-detectors-mean.md)) — für einen Dienst, der über einen Server-Proxy läuft, relevant.
- Kreditkarte: für den Free-Plan nicht belegt; für PAYG Zahlungsmittel nötig.

### 3.3 Bezahlstufen
- Planseite ([Preise](https://elevenlabs.io/pricing)): Starter 6 $/Monat, Creator 22 $ (erster Monat 11 $), Pro 99 $, Scale 299 $, Business 990 $. API-Preisseite ([API](https://elevenlabs.io/pricing/api)): v4 0,08 $ je 1.000 Zeichen (= 80 $ je 1 Mio), v4 Turbo 0,04 $ (40 $), v3 0,08 $, v3 Conversational 0,04 $, Multilingual v2 0,08 $, Flash/Turbo 0,04 $. Die 1-Mio-Werte sind Umrechnung.
- Überziehung: Für neue Self-Serve-Abos gibt es kein Usage-Based-Billing mehr („legacy feature“). Ersatz ist Pay-as-you-go: vorausbezahlt, „PAYG is a prepaid model with no overage charges“, Mindestaufladung 5 $, Guthaben verfällt nach 12 Monaten, Auto-Top-up optional (Standard 20 $ bei Schwelle 10 $). Free-Konten mit PAYG erhalten Starter-Concurrency (3) ([PAYG](https://elevenlabs.io/docs/overview/administration/pay-as-you-go.md), [Billing](https://elevenlabs.io/docs/overview/administration/billing)). Bis zu zwei Monate ungenutzte Abo-Credits wandern in den Folgemonat.
- Kostenbremse: PAYG ohne Auto-Top-up ist die harte Bremse (Dienst pausiert bei 0 $).

### 3.4 Aussprachekontrolle
- SSML: `<break time="x.xs" />` nur ältere Modelle, **v3 und v4 unterstützen es nicht**; `<phoneme>` (CMU Arpabet/IPA) nur mit `eleven_flash_v2` und einzelnen Wörtern; v4 liest IPA in Schrägstrichen `/…/` ([Best Practices](https://elevenlabs.io/docs/overview/capabilities/text-to-speech/best-practices)). `eleven_v4` unterstützt laut [v4-Seite](https://elevenlabs.io/docs/overview/capabilities/text-to-speech/eleven-v4.md) kein SSML und keine Style-/Speed-Regler.
- Aussprachewörterbücher: PLS/TXT, Phonem- oder Alias-Einträge, bis zu 3 Wörterbücher je Anfrage, groß-/kleinschreibungsabhängig, nur der erste Treffer zählt ([Best Practices](https://elevenlabs.io/docs/overview/capabilities/text-to-speech/best-practices), [API-Referenz](https://elevenlabs.io/docs/api-reference/text-to-speech/convert)). Alias-Einträge sind für Deutsch das praktikable Mittel (z. B. „WLAN“ → „Weh-Lan“); eigene Deutsch-Beispiele nennt die Doku nicht.
- Textnormalisierung: `apply_text_normalization` = `auto` (Standard), `on`, `off`; `language_code` erzwingt Sprache und Normalisierung, wird bei nicht unterstützten Modellen ignoriert und ist für `multilingual_v2` nicht unterstützt ([API-Referenz](https://elevenlabs.io/docs/api-reference/text-to-speech/stream)). Die Hilfe bestätigt: „11“ könne je nach Sprache „eleven“, „once“ oder „Elf“ sein; `language_code` helfe vor allem bei kurzem, mehrdeutigem Text ([FAQ](https://elevenlabs.io/docs/help-center/product/core-capabilities/text-to-speech/why-are-numbers-dates-symbols-and-acronyms-not-properly-pronounced-or-spoken-in-the-correct-language.md)). Normalisierung kostet Latenz. Größere Modelle lesen Zahlen besser; Flash v2.5 kann Währungen falsch lesen ([Best Practices](https://elevenlabs.io/docs/overview/capabilities/text-to-speech/best-practices)). Eine ältere Drittquelle meldet, `apply_text_normalization=on` gehe bei v2.5-Modellen nur im Enterprise-Plan; die aktuelle API-Referenz nennt diese Einschränkung nicht **[Widerspruch, ungeklärt]**.
- Audio-Tags (v4): `[laughs]`, `[whispers]`, `[sighs]`, `[excited]`, `[sarcastic]` usw.; zuverlässig sei die Befolgung noch nicht.
- Wörtlich gesprochene Satzzeichen: keine Doku, **[unbelegt]**; wäre Testfall B per Prompt, nicht per Schalter.

### 3.5 Streaming und Android
- Drei Wege: REST komplett, REST-Stream, WebSocket `wss://api.elevenlabs.io/v1/text-to-speech/{voice_id}/stream-input` ([Latenz-Guide](https://elevenlabs.io/docs/eleven-api/guides/how-to/best-practices/latency-optimization.md), [WebSocket](https://elevenlabs.io/docs/eleven-api/guides/how-to/websockets/realtime-tts)). **Dieser WebSocket unterstützt `eleven_v3` und `eleven_v4` nicht**; für sie verweist die Doku auf den Text-to-Dialogue-WebSocket. Für Echtzeit mit Text-Stream von einem LLM wird `eleven_flash_v2_5` empfohlen.
- Belegte Zeiten: Flash über WebSocket TTFB 100–150 ms in Nordamerika und Europa ([Latenz-Guide](https://elevenlabs.io/docs/eleven-api/guides/how-to/best-practices/latency-optimization.md)); v4 Turbo ~100 ms Inferenz und ~150 ms bis zur ersten Sprache, ohne Netzwerk ([Blogpost](https://elevenlabs.io/blog/create-realistic-transatlantic-accent-text-to-speech)). Backends: USA, Niederlande, Singapur; `x-region`-Header zeigt das.
- SDKs: Python und TypeScript. Das [Kotlin-SDK](https://elevenlabs.io/docs/eleven-agents/libraries/kotlin.md) (`io.elevenlabs:elevenlabs-android`, minSdk 21) deckt nur ElevenAgents ab. Für Jarvis heißt das: REST/WebSocket selbst anbinden, Schlüssel nie in der APK (Proxy).

### 3.6 Datenschutz, Lizenz, Zuverlässigkeit
- Standardmäßig US-Hosting; EU/Indien/Singapur als isolierte Umgebungen **nur Enterprise**, Zero Retention muss zusätzlich aktiviert werden; DPA und SOC 2 vorhanden ([Data Residency](https://elevenlabs.io/docs/overview/administration/data-residency)). Zero Retention (`enable_logging=false`) nur Enterprise ([ZRM](https://elevenlabs.io/docs/eleven-api/resources/zero-retention-mode.md)). Für Selbstbedienungskonten gibt es also keine vertraglich zugesicherte EU-Verarbeitung.
- Ausgabelizenz: bezahlte Pläne mit kommerzieller Lizenz, die nach Kündigung für bereits erzeugte Inhalte bestehen bleibt ([Hilfe](https://help.elevenlabs.io/hc/en-us/articles/15993008593297-What-happens-to-my-content-after-my-subscription-ends)).
- Statusseite ([Status](https://status.elevenlabs.io), [Verlauf](https://status.elevenlabs.io/history)): im Fenster Juli–Oktober 2026 u. a. 12.10. TTS-Ausfälle und erhöhte Latenz („heavy traffic spike“), 13.10. Zeitüberschreitungen in mehreren US-Endpunkten, 27.08. erhöhte TTS/STT-Latenz, 03.08. erhöhte TTS/STT-Fehlerraten. Zuordnung zu TTS aus Titeln abgeleitet, keine Dauer- oder Schweregradangaben. Keine Verfügbarkeitsprozente veröffentlicht.

---

## 4. Profil Cartesia

### 4.1 Modelle und Deutsch
- `sonic-3.6` (Alias, folgt dem neuesten stabilen Snapshot), `sonic-3.6-2026-08-27` (gepinnt), `sonic-preview` (Beta); 44 Sprachen inklusive Deutsch ([Sonic 3.6](https://docs.cartesia.ai/build-with-cartesia/tts-models/latest)). Sonic 3 und 3.5: 42 Sprachen ([ältere Modelle](https://docs.cartesia.ai/build-with-cartesia/tts-models/older-models)).
- **Sonic-2- und Sonic-Turbo-Snapshots sowie `sonic-3-2025-10-27` enden am 20.10.2026**, also gestern; Migration auf Sonic 3.6 ([ältere Modelle](https://docs.cartesia.ai/build-with-cartesia/tts-models/older-models)). `sonic-latest` und `sonic-3-latest` sind veraltete Aliase.
- Sonic 3.6 gilt als rückwärtskompatibel zu 3.5. Jede Stimme kann jede unterstützte Sprache sprechen; hat eine Stimme keinen deutschen Akzent, spricht sie Deutsch mit ihrem Originalakzent. Sonic 3.6 klingt in Fremdsprachen muttersprachlicher; für bewusst akzentuiertes Deutsch empfiehlt die Doku `sonic-3.5`. Pro Anfrage eine Sprache; Mischtext (Deutsch mit englischen Fachwörtern) kann akzentbehaftet klingen ([Mehrsprachige Stimmen](https://docs.cartesia.ai/build-with-cartesia/capability-guides/multilingual-voices.md)). Für Franks Testtext (eingedeutschte Fremdwörter wie Meeting, Office) relevant.
- Deutsche Stimmen: Anzahl und Namen **[unbelegt]**.

### 4.2 Freikontingent im Wortlaut
- Laut [Preisseite](https://cartesia.ai/pricing): Free 0 $, 20K Credits/Monat, Concurrency TTS/STT/Agent 2/8/8, Überziehung für Free nicht angeboten, „The free plan has $1/mo of prepaid dollars to use towards voice agents.“ „1 credit equals 1 character“; eine Minute Audio koste „750-800 credits“. Ohne Überziehung: „new requests will be blocked if all your credits have been consumed.“
- Kommerziell: In der Free-Spalte als „Not listed“; Pro nennt „Commercial use license“. [AGB](https://cartesia.ai/terms) §4.1: „personal, non-commercial use only (unless commercial use is expressly permitted by your subscription tier)“, §5.3(b) verbietet die Monetarisierung der Ausgaben ohne Freigabe. Namensnennung: keine in den AGB.
- API im Free-Plan: nicht ausdrücklich, aber die Preisseite nennt TTS-Concurrency für Free **[implizit]**. Kreditkarte: **[unbelegt]** (Drittseiten behaupten „keine“, nicht verifiziert).
- Training: AGB §5.3(c): Eingaben und Ausgaben „may be used by Cartesia to train, enhance, evolve and improve its machine learning and artificial intelligence models“; Opt-out per Formular wirkt nur für die Zukunft. Das gilt nicht nur für Free. Für eine DSGVO-Bewertung ein Minuspunkt.

### 4.3 Bezahlstufen
- Pro 5 $/Monat, 100K Credits, Überziehung 65 $ je 1 Mio Credits; Startup 49 $, 1,25 Mio Credits, 45 $; Scale 299 $, 8 Mio, 38 $; Enterprise individuell ([Preise](https://cartesia.ai/pricing)). Effektiver Preis je 1 Mio Zeichen im Abo: Pro 50 $, Startup ca. 39 $, Scale ca. 37 $ (eigene Rechnung). Artificial Analysis setzt Sonic 3.6 mit 49 $ je 1 Mio an.
- Überziehung nur bei bezahlten Plänen und nur auf Wunsch aktivierbar, das ist die Kostenbremse (standardmäßig harte Sperre).

### 4.4 Aussprachekontrolle
- Tags ([SSML-Tags](https://docs.cartesia.ai/build-with-cartesia/sonic-3/ssml-tags)): `speed` 0,6–1,5, `volume` 0,5–2,0, `emotion` (Beta, „highly experimental“), `break` (`<break time="1s"/>`), `spell` (`<spell>ABC123</spell>`). Eine Laughter-Fähigkeit ist trotz Seitentitel nicht dokumentiert.
- Wörterbücher: Einträge mit IPA `<<ˈ|b|aɪ|u>>` oder „sounds-like“-Schreibweise, Parameter `pronunciation_dict_id`; groß-/kleinschreibungsunabhängiges Matching erst ab Sonic 3.6 ([Eigene Aussprache](https://docs.cartesia.ai/build-with-cartesia/sonic-3/custom-pronunciations)). Inline-Phoneme `<<k|ɑ|ɹ|ˈ|t|i|ʒ|ə>>` für die Sonic-3-Familie ([Phoneme](https://docs.cartesia.ai/build-with-cartesia/sonic-3/phonemes)); die dokumentierte Phonemtabelle ist **englisch** (charsiu-Phonemsatz). Ob deutsche IPA-Symbole gelten, ist **[unbelegt]**. „Sounds-like“-Einträge sind für Deutsch der sichere Weg.
- Textnormalisierung: `normalization` = `auto` (Standard), `off` oder Sprach-/Locale-Code; „Normalized categories use the conventions of the request's `language` or `locale`“; `locale` erst ab Sonic 3.6 ([Normalisierung](https://docs.cartesia.ai/build-with-cartesia/capability-guides/text-normalization.md), [Mehrsprachige Stimmen](https://docs.cartesia.ai/build-with-cartesia/capability-guides/multilingual-voices.md)). Konkrete deutsche Regeln (Datum, Währung, Tausenderpunkt) sind **nicht dokumentiert**; der [Prompting-Leitfaden](https://docs.cartesia.ai/build-with-cartesia/sonic-3/prompting-tips) nennt als Beispiele nur US-Formate (`$19.99`, `04/20/2025`, `7:00 PM`) und rät als Rückfall zum Ausschreiben.
- Weitere Hinweise: Text mit Satzzeichen abschließen, keine Großschreibung zur Betonung, Füllwörter in Standardschreibung.
- Wörtlich gesprochene Satzzeichen: nicht dokumentiert **[unbelegt]**.

### 4.5 Streaming und Android
- WebSocket `wss://api.cartesia.ai/tts/websocket` mit Pflichtparameter `cartesia_version` (Beispiel `2026-08-14`), Kontexte, `continue`, `flush`, `cancelRequest`, Wort- und Phonem-Zeitstempel, `max_buffer_delay_ms` (0–5000, Standard 3000, bei Echtzeit senken). Authentifizierung: `X-API-Key` serverseitig oder **kurzlebiges `access_token` für Client-Apps** ([WebSocket](https://docs.cartesia.ai/api-reference/tts/websocket)). Das ist für Android ein echter Vorteil: kein dauerhafter Schlüssel im Gerät, nur ein Token-Broker.
- Latenz: „sub-90ms TTS“ als Marketing ([Launch](https://www.cartesia.ai/launch/)), ohne Messverfahren. Ein Suchauszug (Quelle dort nicht eindeutig zuzuordnen, vermutlich eine der Vergleichsseiten aus der Suche vom 21.10.2026) nennt eine öffentliche Produktionsmessung mit Median 188 ms, vermutlich für ältere Modelle **[schwach belegt, nicht verifiziert]**. Unabhängige TTFA-Messung für Sonic 3.6 liegt nicht vor.
- SDKs: nur JavaScript/TypeScript und Python ([Client Libraries](https://docs.cartesia.ai/tools/client-libraries.md)). Kein Kotlin/Java-SDK.

### 4.6 Datenschutz, Lizenz, Zuverlässigkeit
- Regionale Endpunkte (USA, EU, UK, Indien, Australien) nur Enterprise ([Regionale Endpunkte](https://docs.cartesia.ai/enterprise/regional-endpoints.md)); Zero Data Retention nur Enterprise, gilt für TTS/STT-Inferenz, nicht für Voice Cloning ([ZDR](https://docs.cartesia.ai/enterprise/zero-data-retention.md)).
- Ausgabe: „Cartesia does not claim ownership of your Inputs or Outputs“, aber keine Gewähr, dass der Nutzer rechtlicher Eigentümer ist (§5.3, §7.1(4)) ([AGB](https://cartesia.ai/terms)).
- Status ([Status](https://status.cartesia.ai)), Fenster Juli–Oktober 2026: TTS 99,77 % gesamt (USA 99,77 %, EU 99,98 %, APAC 99,98 %, AU 100 %), Voice Cloning 99,88 %. Einzelne Vorfälle nicht abgerufen. Das ist die einzige veröffentlichte Verfügbarkeitszahl der drei Anbieter.

---

## 5. Profil Inworld

### 5.1 Modelle und Deutsch
- `inworld-tts-2` (Realtime TTS-2, aktuelles Flaggschiff, 05.05.2026), `inworld-tts-2-flash` (09.08.2026, 20 ms P90 TTFB serverseitig, kein Steering, keine Pro-Stimmklonung); `inworld-tts-1.5-max`/`-mini` veraltet; `inworld-tts-1`/`-1-max` seit 15.06.2026 eingestellt, Anfragen werden umgeleitet ([Modelle](https://docs.inworld.ai/tts/tts-models), [Release Notes](https://docs.inworld.ai/release-notes/tts)).
- Sprachen: Die Modellseite nennt „200+ languages and locales“, die Release Notes bei Start „production-quality“ in 15 Sprachen plus experimentell 90+ weitere. Deutsch ist **Tier 1** (höchste Qualität, am gründlichsten bewertet), Code `de` = `de-DE`; ohne `language` wird aus dem Text erkannt, was bei sehr kurzen Texten (Zahlen, Daten) scheitern kann ([Mehrsprachigkeit](https://docs.inworld.ai/tts/capabilities/multilingual)). Mehrsprachiger Text: Sprach-Tags, auch mitten im Satz. Für beste Aussprache einer fremdsprachig geklonten Stimme empfiehlt die Doku „Voice Localization“, die derzeit nur englische Ausgangsstimmen unterstützt.
- Deutsche Stimmen der Bibliothek: Namen und Anzahl **[unbelegt]** (Stimmenseite hinter Login).

### 5.2 Freikontingent
- Preisseite ([Preise](https://inworld.ai/pricing)): On-Demand 0 $, „Up to 70 min TTS included“, FAQ: „up to 70 minutes of TTS or 400 minutes of STT for free“. Doku ([Billing TTS](https://docs.inworld.ai/tts/resources/billing)) ebenfalls 70 Minuten; die Seite sagt nicht, ob das monatlich wiederkehrt **[unbelegt]**. Eine Suchmaschinen-Zusammenfassung nannte 40 Minuten und „Freiguthaben zum Monatsbeginn“ aus einer anderen Billing-Seite **[Widerspruch, nicht verifiziert]**. Dauerhaft wiederkehrend ist nicht belegt.
- Umrechnung Minuten → Zeichen: nicht angegeben für TTS-2. Aus der älteren Doku-Angabe „1 ¢/Minute bei 10 $ je 1 Mio“ (TTS-1.5 Max) ergäben sich grob 1.000 Zeichen je Minute, also etwa 70.000 Zeichen — **nur Schätzung, für TTS-2 nicht belegt**.
- Kommerziell: „Commercial license“ steht unter On-Demand; die Zuordnung in der Vergleichstabelle ist laut Abruf unklar. Namensnennung **[unbelegt]**. Kreditkarte: für die Anmeldung nicht belegt; eine Karte ist nur nötig für Professional Voice Cloning und bestimmte Premium-LLMs.
- Concurrency On-Demand: 5 gleichzeitige Anfragen (ca. 20 Nutzersitzungen); Creator 10, Builder 50, Developer 150, Growth 500. Verbindliche WebSocket-Werte stehen nur in der Konsole **[unbelegt]**.

### 5.3 Bezahlstufen
- TTS je 1 Mio Zeichen ([Preise](https://inworld.ai/pricing)): On-Demand TTS-2 25 $ / Flash 15 $; Creator (25 $/Monat) 20/10 $; Builder (100 $) 17,50/9 $; Developer (300 $) 15/8 $; Growth (1.500 $) 12,50/7 $; Enterprise ab 5 $. Jahresabrechnung „2 months free“. Artificial Analysis setzt TTS-2 mit 20,8 $ und Flash mit 10,4 $ an (Mischpreis).
- Paid-Pläne enthalten Credits in Höhe des Planpreises je Zyklus, ungenutzte verfallen nach 3 Monaten bei aktivem Abo; zusätzliche Credits ab 10 $, bis 1 Jahr gültig, Auto-Reload optional ([Billing](https://docs.inworld.ai/portal/billing.md)). „Overage usage is charged at the same rate as your plan.“ Ein Ausgabenlimit nennt die Doku nicht **[unbelegt]**. Scheitert die Zahlung nach 7 Tagen, fällt das Konto auf On-Demand zurück und die API-Nutzung wird deaktiviert.
- Batch-Synthese 20 % günstiger als Echtzeit.

### 5.4 Aussprachekontrolle
- SSML: nur `<break time="1s" />` (max. 20 je Anfrage, max. 10 s je Pause) und `<say-as interpret-as="verbatim">`/`<verbatim>` für Alphanumerisches ([Pausen](https://docs.inworld.ai/tts/capabilities/pause-controls.md), [Verbatim](https://docs.inworld.ai/tts/capabilities/verbatim.md)). Verbatim liest Zeichen einzeln und Symbole mit Namen (`@` = „at“), erlaubt nur `( ) - # : . , + @ / _ *`, ist englisch geprägt und braucht aktive Normalisierung. Das ist **kein** Mechanismus für Testtext B (beliebige Satzzeichen wörtlich sprechen), geht aber in die Richtung.
- Aussprache: Inline-IPA eines einzelnen Wortes in Schrägstrichen (`/kriːt/`), „optimized for English IPA symbols“, keine ARPAbet- und keine fremden IPA-Sätze, **kein Wörterbuch** ([Eigene Aussprache](https://docs.inworld.ai/tts/capabilities/custom-pronunciation.md)). Für deutsche Akronyme (WLAN, HRV) bleibt als Weg Umschreibung im Text.
- Normalisierung: `applyTextNormalization` mit `ON`, `OFF`, unspecified (automatisch); „Turning this off may reduce latency, but the speech output will read the text exactly as written“ ([Synthese-API](https://docs.inworld.ai/api-reference/ttsAPI/texttospeech/synthesize-speech.md)). Die Normalisierung läuft in der Zielsprache; `language` setzen, damit Zahlen und Daten nicht an der Spracherkennung scheitern ([Mehrsprachigkeit](https://docs.inworld.ai/tts/capabilities/multilingual)). Konkrete deutsche Regeln: **nicht dokumentiert**.
- Steering (nur `inworld-tts-2`): Anweisungen in eckigen Klammern wie `[whisper]`, `[say excitedly]`, `[reset]`; nichtverbale Laute `[laugh]`, `[breathe]`, `[sigh]` (auch bei Flash); **Anweisungen müssen englisch sein**, der Text darf deutsch sein ([Steering](https://docs.inworld.ai/tts/capabilities/steering.md)). Seit 06.08.2026 gelten Tags dauerhaft bis `[reset]` ([Release Notes](https://docs.inworld.ai/release-notes/tts)). `deliveryMode`: STABLE/BALANCED/CREATIVE nur TTS-2.
- Wörtlich gesprochene Satzzeichen: **[unbelegt]**.

### 5.5 Streaming und Android
- Endpunkte ([Synthese-API](https://docs.inworld.ai/api-reference/ttsAPI/texttospeech/synthesize-speech.md)): `POST https://api.inworld.ai/tts/v1/voice` (bis 2.000 UTF-16-Einheiten, gesamte Audio-Antwort als JSON), HTTP-Streaming (bis 4.000), WebSocket mit Kontexten, `autoMode`, `flushContext` (je `sendText` bis 2.000) ([WebSocket](https://docs.inworld.ai/tts/synthesize-speech-websocket.md)). Seit 11.09.2026 zusätzlich OpenAI-kompatibler `POST /v1/audio/speech` mit mp3/opus/flac/wav/pcm und SSE, nutzbar mit den offiziellen OpenAI-SDKs ([Release Notes](https://docs.inworld.ai/release-notes/tts)). Audioformate u. a. MP3, OGG_OPUS, FLAC, LINEAR16, PCM, WAV. `speakingRate` 0,5–1,5.
- Latenz: P90 TTFB 100 ms (TTS-2) und 20 ms (Flash), serverseitig ohne Netzwerk ([Modelle](https://docs.inworld.ai/tts/tts-models)). Eine Drittquelle nennt dagegen „sub-200ms“ im Median ([TestingCatalog](https://www.testingcatalog.com/inworld-ai-launches-realtime-tts-2-model-for-live-conversations/), nur Suchauszug) **[Widerspruch Anbieter/Dritte]**.
- SDKs: Python und Node.js, CLI; kein Android-SDK. WebSocket-Authentifizierung für Mobile-Clients nicht belegt **[unbelegt]**, daher Proxy einplanen.

### 5.6 Datenschutz, Lizenz, Zuverlässigkeit
- SOC 2 Typ II, „HIPAA and GDPR compliant“; DPA nur in Enterprise-Plänen; EU- und Indien-Region als isolierte Deployments **nur Enterprise**; Zero Data Retention nur Enterprise, bei Growth als Zusatz ([Security](https://inworld.ai/security), [ZDR](https://docs.inworld.ai/portal/zero-data-retention.md)). Nicht von ZDR erfasst: Nutzungsmetadaten, Stimmklon-Audioproben, Voice-Design-Beschreibungen. Zum Training mit Kundendaten gibt die Security-Seite keine Auskunft **[unbelegt]**; Verweis auf trust.inworld.ai.
- Ausgabelizenz: nicht geprüft **[unbelegt]**.
- Zuverlässigkeit: Die Statusseite ([Status](https://status.inworld.ai)) lieferte keinen auswertbaren Inhalt; keine Verfügbarkeitszahl und keine Vorfälle für 2026 **[unbelegt]**.

---

## 6. Hume (Octave): eliminiert

- Octave 2 (Preview, Veröffentlichung 01./03.10.2025) unterstützt Deutsch (nur Octave 2; Octave 1 nur Englisch und Spanisch), ~100 ms Latenz laut Doku ([TTS-Doku](https://dev.hume.ai/docs/text-to-speech-tts/overview)).
- **„Hume is sunsetting the TTS and EVI APIs.“ Zugang endet am 13.11.2026, 00:01 Uhr EST; Kontodaten werden danach dauerhaft gelöscht** ([Hume-Doku](https://dev.hume.ai/docs/text-to-speech-tts/overview), [Changelog 02.10.2026](https://dev.hume.ai/voice/changelog)). Hume konzentriert sich auf VoiceEQ; Stimmen-IDs und selbst gebaute Stimmen sind nicht exportierbar. Bestätigung durch eine Drittquelle (Wettbewerberblog, daher mit Vorsicht): [Cartesia-Blog](https://www.cartesia.ai/learn/top-hume-ai-alternatives).
- Preise und Freikontingent wurden nicht ermittelt (Preisseite lieferte keinen Inhalt) und sind wegen des Abschalttermins irrelevant.
- Octave erscheint nicht in der Artificial-Analysis-Rangliste der Provider-Voice-Arena (Top 30).

---

## 7. Unabhängige Qualitätsbelege (Artificial Analysis)

### 7.1 Provider Voice Arena, Stand 21.10.2026
Quelle: [Leaderboard](https://artificialanalysis.ai/text-to-speech/leaderboard) (blinde Paarvergleiche, Standardstimmen der Anbieter; keine Sprachauswahl, nur Filter US/UK-Akzent und Kategorie).

| Rang | Modell | Elo | Preis je 1 Mio Zeichen |
|---|---|---|---|
| 1 | Eleven v4 Turbo | 1335 | 40 $ |
| 2 | Eleven v4 | 1329 | 80 $ |
| 3 | Qwen-Audio-3.1-TTS-Plus | 1301 | 19,3 $ |
| 4 | Sonic 3.6 (Cartesia) | 1289 | 49 $ |
| 5 | Gemini 3.8 Flash TTS | 1278 | 16,5 $ |
| 6 | Realtime TTS-2 (Inworld) | 1267 | 20,8 $ |
| 13 | Realtime TTS-2 Flash (Inworld) | 1218 | 10,4 $ |
| 15 | v3 Conversational | 1214 | 50 $ |
| 18 | Sonic 3.5 | 1191 | 49 $ |
| 21 | Eleven v3 | 1183 | 100 $ |

Die Zusammenfassung des kleineren Lesemodells wich von Sekundärquellen um wenige Punkte ab (v4 1321–1329, Turbo 1333–1335, Sonic 3.6 1278–1289), weil die Arena fortlaufend neu rechnet ([AlphaSignal 06.10.2026](https://alphasignal.ai/news/elevenlabs-eleven-v4-turbo-tops-tts-leaderboard-at-half-the-price), [OrcaRouter 21.10.2026](https://orcarouter.ai/blog/voxtral-3-tts-vs-elevenlabs)).

### 7.2 Controlled Voice Arena (geklonte Referenzstimmen)
- **Englisch (US/UK)** ([Controlled Voice](https://artificialanalysis.ai/text-to-speech/leaderboard/controlled-voice)): HeyGen Voice 1195, Qwen-Audio-3.1-TTS-Plus 1184, Eleven v4 Turbo 1156, Eleven v4 1156, Sonic 3.6 1151, Realtime TTS-2 1145.
- **Deutsch:** Die Methodik nennt Deutsch ausdrücklich als Controlled-Voice-Sprache (je eine männliche und weibliche Profi-Referenzstimme, Rater mit Deutsch als Erstsprache, eigene Wertung, Board erst bei genug Vergleichen) ([Methodik](https://artificialanalysis.ai/methodology/text-to-speech)). Der Abruf der Live-Seite lieferte nur das englische Board; **das deutsche Board konnte nicht abgerufen werden**.
  - Ankündigung der mehrsprachigen Boards, nur als Suchauszug (X-Beitrag liefert HTTP 402): Cartesia #1 in den vier europäischen Sprachen, Sonic 3.6 führt Deutsch, Inworld Realtime TTS-2 auf Deutsch zweiter mit 1.261 Elo, 22 Punkte dahinter, Stand 22.09.2026 laut Suchzusammenfassung. Das ergibt ca. 1.283 für Sonic 3.6 und deckt sich mit §17. Dieser Stand liegt **vor dem Start von Eleven v4 (28.09.2026)**.
  - Eine Zweitquelle ([AlphaSignal 06.10.2026](https://alphasignal.ai/news/elevenlabs-eleven-v4-turbo-tops-tts-leaderboard-at-half-the-price)) schreibt, v4 Turbo liege in der Controlled Voice Arena auf Platz 1 in Arabisch, Deutsch, Hindi und UK-Englisch und übertreffe dort v4. Einen Elo-Wert nennt sie nicht, und es ist nicht gegen das Live-Board geprüft. **Behandle „v4 Turbo führt auf Deutsch“ als Hinweis, nicht als Beleg.**
- OrcaRouter hält fest, dass „neither vendor publishes per-language Elo“ in den Aggregaten; die Gesamtwerte sagen also nichts über Deutsch.
- Die „Pronunciation Robustness“-Messung (v4 91,7 %, v4 Turbo 90,1 %, Gemini 3.8 Flash TTS 89,5 %) ist englisch ausgewiesen und taugt nicht für deutsche Zahlen und Daten ([AlphaSignal](https://alphasignal.ai/news/elevenlabs-eleven-v4-turbo-tops-tts-leaderboard-at-half-the-price)).

### 7.3 Weitere Belege
- Unabhängige deutsche Hörtests zu Zahlen, Datum, Abkürzungen: **keine gefunden** (zwei Suchen, 21.10.2026). Drittvergleiche beschränken sich auf Latenz, Preis, Sprachanzahl.
- TTS Arena (Hugging Face/TTS-Arena): in dieser Teilstudie nicht ausgewertet **[offen]**.

---

## 8. Abgleich mit dem Testtext (reine Dokumentationslage)

| Phänomen (Testtext A/B) | ElevenLabs | Cartesia | Inworld |
|---|---|---|---|
| Datum 21.10.2026, Uhrzeit 18:47 Uhr | Normalisierung auto/on + `language_code=de`; Datumsformat „locale-abhängig“, kein deutsches Beispiel | `normalization`/`locale=de`; Doku nur mit US-Beispielen; Deutsch **[unbelegt]** | Normalisierung in Zielsprache bei gesetztem `language=de`; Deutsch-Regeln **[unbelegt]** |
| Dezimalkomma, Prozent, 1.249,99 € | wie oben; Währungssymbole werden zu Währungsnamen (englische Doku); Flash v2.5 anfälliger | wie oben | wie oben |
| Abkürzungen ca./z. B./Dr./Nov. | „Dr.“, „St.“ werden expandiert (englische Doku); deutsche Abkürzungen **[unbelegt]**; Alias-Wörterbuch als Rückfall | `spell`-Tag für Buchstabieren; Abkürzungen nur „common acronyms“; Wörterbuch (sounds-like) | `verbatim` für Alphanumerisches; kein Wörterbuch; Umschreibung im Text |
| Akronyme HRV/WLAN/USB | Alias-Wörterbuch („WLAN“ → „Weh-Lan“) | `<spell>HRV</spell>` bzw. Wörterbuch | `<verbatim>`-Tag (englisch geprägt) oder Umschreibung |
| Eingedeutschte Fremdwörter (Update, Meeting, gecancelt) | v4/Flash mehrsprachig; Verhalten **[ungetestet]** | Doku warnt vor Mischtext, ein Sprachcode je Anfrage | Sprach-Tags auch mitten im Satz möglich; deutsches Tier 1 |
| Gedankenstrich, ?, !, Apostroph | Interpunktion formt Rhythmus; Auslassungspunkte = Pause | Satzende-Zeichen empfohlen, Kommas/Punkte erzeugen Pausen | `<break>` oder Satzzeichen; Steering englisch |
| Testtext B: Satzzeichen wörtlich lesen | nur per Prompt/Text-Umschreibung, kein Schalter **[unbelegt]** | nicht dokumentiert **[unbelegt]** | `verbatim` liest einige Symbole mit Namen, aber nur in Zeichenketten; sonst **[unbelegt]** |

Alle Zellen sind Dokumentationslage; die tatsächliche Aussprache ist nur per Hörprobe zu klären. Empfehlung für den Hörtest: ElevenLabs `eleven_v4_turbo` und `eleven_flash_v2_5` mit `language_code=de`, Cartesia `sonic-3.6` mit `language=de`, `normalization=de` (bzw. `locale=de-DE` plus `normalization=de`), Inworld `inworld-tts-2` und `-flash` mit `language=de-DE`, `applyTextNormalization=ON`. Dazu je Anbieter einmal mit und einmal ohne Normalisierung, um zu sehen, ob die Zahlen vom Dienst oder von Franks eigener Vorverarbeitung kommen.

---

## 9. Kostenbeispiele (eigene Rechnung, Listenpreise, ohne Steuern)

| Monatsmenge | ElevenLabs v4 Turbo / Flash (40 $ je 1 Mio) | ElevenLabs v4 (80 $) | Cartesia Sonic 3.6 | Inworld TTS-2 (On-Demand 25 $) | Inworld Flash (15 $) |
|---|---|---|---|---|---|
| 20.000 Zeichen | 0 $ (Free: 20.000 Turbo/Flash-Zeichen laut API-Preisseite, nur nicht kommerziell) | Free deckt nur 10.000 v4-Zeichen, Rest über PAYG (ca. 0,80 $) | 0 $ (Free, 20K) | ca. 0 $ (70 Min. Freiminuten, einmalig oder monatlich ungeklärt) | ca. 0 $ |
| 200.000 Zeichen | Starter 6 $ (150K Turbo) reicht nicht, Creator 22 $ (550K) | Creator 22 $ (275K) | Startup 49 $ (1,25 Mio) oder Pro 5 $ + Überziehung ca. 6,50 $ | ca. 5 $ | ca. 3 $ |
| 1 Mio Zeichen (= Google-Freikontingent) | ca. 40 $ (PAYG) | ca. 80 $ | ca. 37–50 $ | ca. 25 $ | ca. 15 $ |

Google Chirp 3 HD: 1 Mio Zeichen/Monat gratis (siehe §17 der Best-Practices-Datei, Stand 28.09.2026; hier nicht neu geprüft).

---

## 10. Abgrenzung zu §17 (Stand 28.09.2026)

| Aussage §17 | Befund 21.10.2026 |
|---|---|
| „Cartesia Sonic 3.6 = DE-Arena Platz 1, Inworld TTS-2 Platz 2“ | Stand 22.09.2026 (vor v4) laut Suchzusammenfassung plausibel. Seit 28.09. führen ElevenLabs v4/v4 Turbo die Provider-Voice-Arena; für Deutsch behauptet eine Zweitquelle dasselbe, ungeprüft. |
| „ElevenLabs Flash v2.5 ~50 $, Multilingual v2/v3 ~100 $“ | Überholt: Flash/Turbo 40 $, v3 80 $, v4 80 $, v4 Turbo 40 $ je 1 Mio (API-Preisseite). AA führt noch 50/100 $ für ältere Modelle (Flash 2.5 50 $, v3 100 $). |
| „Inworld TTS-2 25 $ (Growth 12,50 $)“ | bestätigt; zusätzlich Flash (15 $/7 $) und Zwischenstufen Creator/Builder/Developer. |
| „Inworld Flash 15 $“ | bestätigt (`inworld-tts-2-flash`). |
| „Cartesia Sonic 3.6 creditbasiert“ | konkretisiert: 1 Credit = 1 Zeichen; Preise je Tarif oben. |
| „ElevenLabs kommerziell erst ab Paid-Plan“ | bestätigt, plus Namensnennungspflicht im Free-Plan. |
| Hume nicht erwähnt | Hume-TTS endet 13.11.2026. |

---

## 11. Offene Punkte und Unbelegtes

- Live-Elo des deutschen Controlled-Voice-Boards (alle Modelle) — Seite nicht abrufbar; manuell auf artificialanalysis.ai prüfen.
- ElevenLabs: Free-Plan-API ausdrücklich; Gültigkeit der API-Preisseiten-Kontingente (monatlich?); Concurrency TTS (2 vs. 4); Anzahl deutscher Bibliotheksstimmen und deren kommerzielle Lizenz; Training mit Kundendaten.
- Cartesia: Kreditkarte für Free; deutsche Stimmen; deutsche Normalisierungsregeln; deutsche Phonemtabelle; unabhängige TTFA.
- Inworld: Free-Minuten wiederkehrend?; Verhalten bei leerem Guthaben; deutsche Stimmen; Statusseite; Training mit Kundendaten; Ausgabelizenz; WebSocket-Auth für Mobile.
- Hörprobe: Keine einzige Aussprache-Aussage (Zahlen, Daten, Fremdwörter) wurde gehört oder per API geprüft.
- TTS Arena (Hugging Face) nicht ausgewertet.

---

BEST-PRACTICES-KANDIDATEN:
- tts-provider.md §17 aktualisieren: ElevenLabs v4/v4 Turbo (80/40 $), Eleven v3 nicht mehr „Spitze“; AA-Provider-Voice-Reihenfolge Oktober 2026; Hume-Abschaltung 13.11.2026; Cartesia-Sunset 20.10.2026 (sonic-2, sonic-turbo, sonic-3-2025-10-27).
- „Freikontingente der Premium-TTS-Anbieter (Okt. 2026)“: ElevenLabs 10K Credits (nicht kommerziell, Namensnennung), Cartesia 20K Credits/Monat, Inworld 70 Min. — Faustregel: Free reicht nur zum Testen, ein Assistent mit Dauerbetrieb braucht Bezahlstufe oder Cache.
- ElevenLabs v3/v4: kein SSML-`<break>`, WebSocket `stream-input` ohne v3/v4; für Echtzeit Flash v2.5 oder v4 Turbo über Text-to-Dialogue-WebSocket.
- Kostenbremsen im Vergleich: ElevenLabs PAYG vorausbezahlt ohne Überziehung, Cartesia harte Sperre ohne aktivierte Überziehung, Inworld Auto-Reload.
- Android-Architektur: Cartesia-WebSocket mit kurzlebigem `access_token` statt Dauerschlüssel; sonst Token-Broker/Proxy für ElevenLabs und Inworld (Inworld bietet einen OpenAI-kompatiblen Endpunkt, ElevenLabs-Free-Konten werden bei VPN/Proxy-IPs gesperrt).
- Deutsche Aussprache: Wörterbuch/Alias statt IPA für deutsche Akronyme (Cartesia- und Inworld-IPA-Doku ist englisch).
- Datenschutz: EU-Residenz und Zero Retention bei allen drei nur Enterprise; Cartesia-AGB erlauben Training mit Ein- und Ausgaben (Opt-out per Formular).
- Anbieter-Docs hinter Login (Cartesia-Billing, Inworld-Stimmenliste) lassen sich per WebFetch nicht lesen; Suchauszüge nur als schwache Quelle behandeln.

BUG-KANDIDATEN:
- Cartesia: `sonic-2`, `sonic-turbo`, `sonic-3-2025-10-27` laufen am 20.10.2026 aus; Apps mit festen Snapshot-IDs oder `sonic-latest`/`sonic-3-latest` brechen. Gegenmittel: `sonic-3.6` oder gepinnter Snapshot, Alias-Migration prüfen.
- ElevenLabs: `eleven_turbo_v2_5` veraltet; `eleven_v3`/`eleven_v4` über `stream-input`-WebSocket nicht unterstützt (Verbindungsfehler/Falschmodell); `<break>` und Style/Speed bei v4 wirkungslos; `apply_text_normalization`/`language_code` je nach Modell ignoriert oder (bei Multilingual v2) nicht unterstützt.
- ElevenLabs Free: Konto wird bei VPN/Proxy/gemeinsamer IP als „unusual activity“ gesperrt und ist dann nur mit Bezahlplan nutzbar.
- Inworld: `inworld-tts-1`/`-1-max` seit 15.06.2026 stillschweigend auf neuere Modelle umgeleitet (Klang ändert sich); SDK-Standard-Modell kann veraltet sein, `modelId` immer explizit setzen; Steering-Tags wirken seit 06.08.2026 dauerhaft (`[reset]` nötig), `inworld-tts-2-flash` ignoriert Steering; Kurztexte (Zahlen/Daten) ohne gesetztes `language` scheitern an der Spracherkennung.
- Hume: TTS- und EVI-API enden am 13.11.2026; danach Löschung der Kontodaten, Stimmen nicht exportierbar.
- Cartesia: `normalization`/`locale` und Groß-/Kleinschreibungs-unabhängige Wörterbuchtreffer erst ab Sonic 3.6; `language` und `locale` nie gleichzeitig setzen (Fehler).
