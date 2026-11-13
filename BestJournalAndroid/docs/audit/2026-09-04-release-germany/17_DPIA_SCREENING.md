# 17 – DSFA-Screening (Datenschutz-Folgenabschätzung, Art. 35 DSGVO)

**Prüfgegenstand:** Best Journal Android (`com.bestjournal.app`), 0.21.17 (298) · **Prüfstand:** `01b200085a7067123e5e21aba8021bc9193033a6`, `main` · **Prüfdatum:** 04.09.2026 · **Modell:** Claude Fable 5.1
**Beweisklassen:** [T] technisch bestätigt · [D] dokumentierte Anbieteraussage · [R] rechtliche Bewertung · [A] plausible Annahme · [U] ungeklärte externe Tatsache

Begriff: *DSFA* = vorab dokumentierte Bewertung der Risiken einer Verarbeitung für die Betroffenen mit Abhilfemaßnahmen.

---

## 1. Ergebnis
**Gesetzlich erforderlich** (nicht nur empfehlenswert). Konsequenz: Benennung eines Datenschutzbeauftragten nach § 38 Abs. 1 S. 2 BDSG [D] unabhängig von der Beschäftigtenzahl; für einen Einzelunternehmer nur als externer DSB umsetzbar; Kontaktdaten in DSE und Meldung an den HBDI. Eine vorherige Konsultation der Aufsichtsbehörde (Art. 36) ist nur nötig, wenn nach Abhilfemaßnahmen ein hohes Restrisiko bleibt – bei Umsetzung der Maßnahmen unten nicht zu erwarten [R].

## 2. Prüfung gegen die DSK-Muss-Liste (V1.1, Wortlaut am Prüftag extrahiert [D])
| Nr. | Wortlaut (gekürzt) | Einschlägig? | Begründung |
|---|---|---|---|
| 7 | Umfangreiche Erhebung/Übermittlung personenbezogener Daten zur Bewertung des Verhaltens … die von Dritten zur Entscheidung mit Rechtswirkung genutzt werden können (Bewertungsportale, Inkasso) | **nein** | Auswertung dient nur dem Nutzer selbst; keine Dritten treffen Entscheidungen |
| 9 | Erstellung umfassender Profile über Interessen, Beziehungsnetz oder Persönlichkeit (Dating-/Kontaktportale, soziale Netzwerke) | **teilweise** | Die Selbsterkenntnis-/Belastungsanalyse erstellt persönlichkeitsbezogene Profile („verborgene Muster, unbewusste Überzeugungen, emotionale Reaktionsmuster“, `ai_prompt_insight_intro`), allerdings ausschließlich für den Betroffenen selbst; kein Vergleich, keine Nutzung durch Dritte |
| 11 | Einsatz von KI zur Verarbeitung personenbezogener Daten zur Steuerung der Interaktion mit Betroffenen oder zur Bewertung persönlicher Aspekte (Beispiel: System, das mit Kunden durch Konversation interagiert; Stimmungslage auswerten) | **ja** | KI bewertet persönliche Aspekte (Gefühle, Belastung, Ziele, Muster) und steuert Ausgaben/Empfehlungen an den Betroffenen |
| 13 | Automatisierte Auswertung von Video-/Audioaufnahmen zur Bewertung der Persönlichkeit | **nein** | Audio wird nur transkribiert; keine Persönlichkeitsbewertung aus dem Audiosignal (`WavSpeechAnalyzer` misst Lautstärke) [T] |
| 16 (ergänzend) | Verarbeitung von Art.-9-Daten mittels mobiler Anwendungen, die von einer zentralen Stelle empfangen und aufbereitet werden (Telemedizin) | **nein** | Der Verantwortliche betreibt keine zentrale Stelle; Verarbeitung bei Auftragsverarbeitern ist flüchtig/zeitlich begrenzt |

## 3. Prüfung gegen WP248 rev.01 (neun Kriterien; Faustregel: ab zwei Kriterien DSFA) [D]
| Kriterium | Erfüllt? | Begründung |
|---|---|---|
| 1 Bewertung/Scoring, Profiling (Gesundheit, Vorlieben, Verhalten) | **ja** | KI bewertet Gefühle, Belastung, Verhaltensmuster |
| 2 Automatisierte Entscheidung mit rechtlicher/ähnlicher Wirkung | nein | keine |
| 3 Systematische Überwachung | nein | Nutzer schreibt freiwillig; keine Beobachtung |
| 4 Sensible Daten / hochpersönliche Daten | **ja** | Tagebuch = hochpersönlich; Art.-9-Inhalte möglich; Stimmungswert |
| 5 Großer Umfang | nein (pro Nutzer umfassend, aber kein Massendatensatz beim Verantwortlichen) | Art. 11-Situation |
| 6 Verknüpfung von Datensätzen | nein | keine Zusammenführung mit fremden Quellen |
| 7 Schutzbedürftige Betroffene | teilweise | Personen in psychischen Krisen; Minderjährige ab 16 |
| 8 Innovative Nutzung/Technologie | **ja** | generative KI zur Persönlichkeitsreflexion |
| 9 Verhinderung der Ausübung von Rechten/Vertragsschluss | nein | – |
→ **drei bis vier Kriterien erfüllt** → DSFA erforderlich.

**Entlastende Faktoren:** keine Speicherung auf eigenen Servern, keine Identifizierung durch den Verantwortlichen (Art. 11), Verarbeitung nur für den Betroffenen, granulare Einwilligung, Offline-Kernfunktion. Sie senken das Risiko, heben die Pflicht aber nicht auf, weil DSK Nr. 11 und WP248-Kriterien 1/4/8 auf die Art der Verarbeitung abstellen.

## 4. Screening-Dokument (Basis für die vollständige DSFA)
**4.1 Beschreibung der Verarbeitung**: Nutzer schreibt/diktiert Tagebucheinträge (freie Texte, optional Stimmungswert, Fotos). Auf Einwilligung werden Texte an Google Gemini (USA) gesendet, das Zusammenfassungen, Belastungs-/Selbsterkenntnis-/Ziel-Analysen und Wochen-/Monats-/Jahresrückblicke erzeugt; Sprachaufnahmen an Groq (USA) zur Transkription; Texte an Microsoft (USA) zur Sprachausgabe; Backups in das eigene Google Drive. Ergebnisse werden lokal gespeichert. Kein Nutzerkonto beim Verantwortlichen.
**4.2 Notwendigkeit und Verhältnismäßigkeit**: Zweck (Selbstreflexion) legitim; Einwilligung granular; Datenminimierung verbesserungsfähig (gesamte Historie statt Zeitraum; DS-005); Speicherbegrenzung beim Anbieter (Groq 30 Tage; Gemini Missbrauchs-Log); Alternativen: On-Device-Verarbeitung (für Transkription vorhanden; für LLM-Analyse derzeit nicht praktikabel), EU-Region (Vertex-AI-Backend prüfbar), Pseudonymisierung (Namen im Text nicht ersetzbar ohne Qualitätsverlust; optionaler lokaler Namensfilter denkbar).
**4.3 Risiken für die Betroffenen**: (a) unbefugter Zugriff auf Tagebuchinhalte beim Anbieter oder in Transit (Vertraulichkeit; Art.-9-Inhalte); (b) Zugriff US-Behörden (FISA 702) auf Daten bei US-Anbietern; (c) fehlerhafte/verletzende KI-Ausgaben mit psychischer Wirkung (Krisensituationen); (d) Verlust der Kontrolle über Kopien (Drive, System-Backup, Galerie-Kopie, Teilen); (e) unrechtmäßige Verarbeitung ohne wirksame Einwilligung (aktuelle Blocker); (f) Missbrauch des Groq-Schlüssels (Verfügbarkeit/Kosten, indirekt Nutzer); (g) Minderjährige.
**4.4 Vorhandene Maßnahmen**: TLS; App Check; lokale Speicherung; granularer Consent + Per-Use-Gates; Diagnoseverbote und Krisenhilfe; Kontolöschung inkl. Drive; Backup-Ausschlüsse; Biometrie-Sperre; keine Trainingsnutzung (Google EWR-Regel, Groq-Doku).
**4.5 Fehlende/geplante Maßnahmen**: Durchsetzung der Einwilligung in allen Codepfaden (DS-001/002); korrekte Information (RT-001, DS-005); Re-Consent (DS-004); Groq über EU-Proxy (SEC-001); TTS On-Device (DS-003); Log-Bereinigung (SEC-002); Zeitraumbegrenzung als Voreinstellung; Mindestalter 16 (ALT-001); Meldefunktion (KI-001); Groq-ZDR aktivieren; Transfer Impact Assessment für Google/Groq dokumentieren (Nachweise E-03–E-06, E-10); DSB bestellen.
**4.6 Restrisiko nach Maßnahmen**: mittel-niedrig; keine Konsultation nach Art. 36 erforderlich [R].
**4.7 Gestaltung, die die DSFA-Pflicht vermeiden würde**: vollständige lokale KI-Verarbeitung (On-Device-Modell) und Verzicht auf Persönlichkeits-/Belastungsbewertung (nur neutrale Zusammenfassung) – wirtschaftlich/produktseitig nicht realistisch; daher DSFA durchführen.
