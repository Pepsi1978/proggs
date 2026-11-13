# 04 – DSGVO- und deutsche Rechtsmatrix

**Prüfgegenstand:** Best Journal Android (`com.bestjournal.app`), 0.21.17 (298) · **Projektpfad:** `C:\Users\barwa\proggs\BestJournalAndroid`
**Prüfstand:** Commit `01b200085a7067123e5e21aba8021bc9193033a6`, `main` · **Prüfdatum:** 04.09.2026 · **Modell:** Claude Fable 5.1
**Beweisklassen:** [T] technisch bestätigt · [D] dokumentierte Anbieteraussage · [R] rechtliche Bewertung · [A] plausible Annahme · [U] ungeklärte externe Tatsache

Hinweis: Gesetzeswortlaute von EUR-Lex konnten am Prüftag technisch nicht abgerufen werden (siehe 15); die Subsumtion [R] stützt sich auf den bekannten Normtext; § 38 BDSG, DSK-Liste, WP248 wurden am Prüftag gelesen [D].

---

## 1. DSGVO

| Norm | Anwendbar? | Ergebnis | Begründung / Befund |
|---|---|---|---|
| Art. 5 Grundsätze | ja | **NICHT BESTANDEN** | Rechtmäßigkeit/Transparenz verletzt durch DS-001/002/RT-001; Datenminimierung: gesamte Historie an Gemini ohne Begrenzung (DS-005); Integrität: Logging (SEC-002). |
| Art. 6 Rechtsgrundlagen je Verarbeitung | ja | teilweise | Lokale Speicherung: DSE nennt keine Rechtsgrundlage (Empfehlung: Art. 6 Abs. 1 lit. b, Vertrag über digitales Produkt). Cloud-KI/Backup/TTS/Analytics: Einwilligung. App Check/Remote Config/Review: lit. f mit Abwägung (vorhanden). Kauf: lit. b. Feedback: lit. a/f. |
| Art. 7 Einwilligung | ja | **NICHT BESTANDEN** | Granular, freiwillig, gleichwertige Buttons, Widerruf so einfach wie Erteilung: erfüllt [T]. Nachweis (Zeitstempel/Version): lokal erfüllt. **Aber:** Einwilligung wird technisch nicht durchgesetzt (DS-001/002), Information teils unzutreffend (RT-001), keine Re-Consent bei Versionswechsel (DS-004). |
| Art. 8 Kinder | ja (Dienst der Informationsgesellschaft, einwilligungsbasiert) | **NICHT BESTANDEN** | Texte „ab 13“, Elternzustimmung verlangt, aber keine Mechanik (ALT-001). Empfehlung 16+. |
| Art. 9 besondere Kategorien | ja (freie Texte/Aufnahmen, Stimmungswert, KI wertet Gefühle/Belastung aus) | teilweise | Ausdrückliche Einwilligung im Gemini-Gate vorhanden („Ausdrücklich zustimmen“, Art.-9-Hinweis) [T]; Groq-Gate ohne Art.-9-Formulierung, TTS-Gate ohne Art.-9-Hinweis, und automatische Pfade umgehen das Gate. Sprachaufnahme ist **kein** biometrisches Datum (keine Identifizierung, keine Emotionsableitung aus der Stimme; `WavSpeechAnalyzer` misst nur Lautstärke) [T]. |
| Art. 11 Verarbeitung ohne Identifizierung | ja | BESTANDEN mit Empfehlung | Entwickler kann Nutzer nicht identifizieren; Rechte werden in-App erfüllt. DSE sollte das erklären (08). |
| Art. 12–14 Information | ja | **NICHT BESTANDEN** | Zeitpunkt/Erreichbarkeit gut (vor Onboarding, ohne Konto); Inhalte teils falsch/unvollständig (RT-001, DS-005, RT-003). |
| Art. 15–22 Betroffenenrechte | ja | BESTANDEN mit Auflagen | Auskunft/Portabilität: PDF-Export (ohne Fotos optional), Drive-Backup; Löschung: Eintrag/alle Daten/Konto/Drive; Widerruf: Einstellungen. Art. 22: keine automatisierte Entscheidung mit rechtlicher Wirkung (KI-Ausgaben sind Vorschläge) – **nicht anwendbar**, korrekt in DSE 12. Analytics-Reset fehlt (DS-010). |
| Art. 25 Privacy by Design/Default | ja | teilweise | Consent-Schalter Standard aus [T]; aber Auto-Dashboard Standard an, Galerie-Kopie, Foto-/Video-Backup Standard an, Groq-Standard in Testphase. |
| Art. 28 Auftragsverarbeitung | ja | **NICHT VERIFIZIERBAR / teilweise NICHT BESTANDEN** | Google: DPA-Annahme [U]; Groq: DPA/SCC [U]; Microsoft: kein Vertrag [T] → kein Auftragsverarbeiter, Übermittlung an Dritten (DS-003). |
| Art. 30 Verzeichnis | ja (Ausnahme Abs. 5 greift nicht: Art.-9-Daten, nicht nur gelegentlich) | NICHT BESTANDEN → Entwurf in 16 | Kein VVT vorhanden. |
| Art. 32 Sicherheit | ja | teilweise | TLS erzwungen, Sandbox, Biometrie optional, App Check; Schwächen: Groq-Key im Client (SEC-001), Logging (SEC-002), Function ohne App Check (SEC-003), unverschlüsselte DB/Backup (DS-009), deprecated Crypto (TECH-001). |
| Art. 35 DSFA | ja | **NICHT BESTANDEN** | Erforderlich (17_DPIA_SCREENING.md). |
| Art. 37 i. V. m. § 38 Abs. 1 S. 2 BDSG DSB | ja | **NICHT BESTANDEN** | Bei DSFA-pflichtiger Verarbeitung Benennungspflicht unabhängig von Beschäftigtenzahl [D]. DSE Abschnitt 1 falsch. Für einen Einzelunternehmer: externer DSB (Interessenkonflikt bei Selbstbenennung). |
| Art. 44–49 Drittland | ja | teilweise | Google (Gemini, Drive, Analytics, Firebase): DPF-Zertifizierung Google LLC + SCC im DPA [U, plausibel]; Groq: SCC laut Doku möglich, Annahme [U]; Microsoft: nur Art. 49 Abs. 1 lit. a (DS-003). DPF-Beschluss gilt fort; Rechtsmittel C-703/25 P anhängig, kein Urteil auffindbar [U]. TIA nicht dokumentiert → 12. |

## 2. Deutsches Recht

| Norm | Anwendbar? | Ergebnis | Begründung |
|---|---|---|---|
| BDSG § 38 | ja | NICHT BESTANDEN | siehe oben. § 22 BDSG (Art.-9-Verarbeitung) nicht einschlägig (keine der dort genannten Zwecke). |
| TDDDG § 25 | ja | BESTANDEN mit Auflage | Speichern/Auslesen von Kennungen (Firebase-Installations-ID, App-Check, Remote Config): unbedingt erforderlich für den vom Nutzer gewünschten Dienst → Ausnahme Abs. 2 Nr. 2 vertretbar [R]; Analytics/Werbe-ID: Einwilligung vorhanden. DSE nennt § 25 nur bei Analytics; für die anderen Kennungen die Ausnahme benennen (08). |
| DDG § 5 Impressum | ja | BESTANDEN mit Restrisiko | Name, Anschrift (c/o), E-Mail, USt-Hinweis, in App und (nach Umzug) Web erreichbar. c/o-Anschrift: Zustellfähigkeit [U] (RT-004). |
| BGB §§ 305–310 AGB | ja | MITTEL | Einbeziehung: Consent-Screen verlinkt AGB vor Vertragsschluss (§ 305 Abs. 2 erfüllt für den Nutzungsvertrag; Kaufvertrag über Google). Klauseln: RT-002 (DSE als Bestandteil, Zustimmungsfiktion, Gerichtsstand). § 309 Nr. 7: Haftungsklausel 8.1/8.2 korrekt (Vorsatz/grobe Fahrlässigkeit/Körperschäden ausgenommen). |
| BGB §§ 312 ff. Fernabsatz/Widerruf, § 312j Button | ja (Kauf) | MITTEL | Kaufabschluss bei Google Commerce Ltd. (Play-Terms [D]) → Button-Lösung und Widerrufsbelehrung sind Googles Checkout-Sache; eigene Belehrung irreführend (VR-001). |
| BGB §§ 327 ff. digitale Produkte | ja | BESTANDEN mit Hinweis | Kostenloser Basisumfang: Vertrag nach § 327 Abs. 3 (Daten als Gegenleistung)? Nur wenn Daten über das Erforderliche hinaus verarbeitet werden – bei Opt-in-Analytics ja → Aktualisierungspflicht § 327f auch für Free-Nutzer; AGB 10a deckt das ab [T]. Abo: Aktualisierungspflicht genannt. TTS-Ausfallrisiko (DS-003) = Mangelrisiko. |
| BGB § 356a Widerrufsbutton | ja (seit 19.06.2026 für Fernabsatzverträge über Online-Benutzeroberfläche) | teilweise | In-App-Button vorhanden, aber an den falschen Adressaten (Entwickler statt Google) für den Kaufvertrag (VR-001). |
| UWG §§ 5, 5a | ja | HOCH-Risiko bis Korrektur | Unzutreffende Angaben (RT-001) sind irreführende geschäftliche Handlungen; typische Abmahngründe: Impressum, DSE-Fehler, Gesundheitsaussagen (keine gefunden). |
| HWG §§ 1, 3 | **nicht anwendbar** | – | Keine Werbung für Arzneimittel/Medizinprodukte/Verfahren mit Krankheitsbezug; Texte bleiben bei Reflexion/Stress; Disclaimer vorhanden. Wachsamkeit bei künftigen Marketingtexten. |
| JuSchG / JMStV | teilweise | NIEDRIG | Kein jugendgefährdender Inhalt; KI-Ausgaben ohne Alterskennzeichnungspflicht; Altersfreigabe über IARC/USK; § 24a JuSchG Vorsorgemaßnahmen gilt nur für Plattformen mit nutzergenerierten Inhalten → **nicht anwendbar**. |
| BFSG | **nicht anwendbar** (§ 3 Abs. 3 Kleinstunternehmen) | – | Freiwillige Verbesserungen A11Y-001. |
| VSBG § 36 | ja (Unternehmer mit Website/AGB; Ausnahme ≤10 Beschäftigte für die Hinweispflicht) | BESTANDEN | Hinweis „nicht bereit und nicht verpflichtet“ vorhanden; keine OS-Plattform-Verweise [T]. |
| MStV § 18 | **nicht anwendbar** | – | Kein journalistisch-redaktionelles Angebot; Absatz im Impressum überflüssig (RT-004). |
| ProdHaftG / RL (EU) 2024/2853 | künftig | Watchlist | Umsetzung bis 09.12.2026; RegE im Bundestag (Stand April 2026 [D], danach [U]). |
| MDR / MDCG 2019-11 | **nicht anwendbar** | – | Keine medizinische Zweckbestimmung (siehe 05 Abschnitt 6). |

## 3. Anforderungen an elektronische Einwilligungen (Protokollierung)
Zeitpunkt, Policy-Version und Umfang werden lokal gespeichert [T]; kein serverseitiges Protokoll (nicht erforderlich mangels Nutzerkonto). Nach Kontolöschung/Deinstallation ist der Nachweis weg – vertretbar, da dann keine Verarbeitung mehr stattfindet [R].
