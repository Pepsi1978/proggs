# 14 – Abschließende Release-Checkliste Deutschland

**Prüfgegenstand:** Best Journal Android (`com.bestjournal.app`), 0.21.17 (298) · **Prüfstand:** `01b200085a7067123e5e21aba8021bc9193033a6`, `main` · **Prüfdatum:** 04.09.2026 · **Modell:** Claude Fable 5.1
**Beweisklassen:** [T] technisch bestätigt · [D] dokumentierte Anbieteraussage · [R] rechtliche Bewertung · [A] plausible Annahme · [U] ungeklärte externe Tatsache

| # | Punkt | Wert | Befund |
|---|---|---|---|
| 1 | Richtiges Projekt, „Best Journal Frank“ ausgeschlossen | BESTANDEN | 00 |
| 2 | Release-Build erfolgreich | BESTANDEN | 10 |
| 3 | Keine geheimen Schlüssel im Code/Historie | BESTANDEN (Firebase-Android-Key historisch, bestimmungsgemäß; SEC-005) | 10 |
| 4 | Kein ungeschützter KI-API-Schlüssel in der App | NICHT BESTANDEN (Groq via Remote Config) | SEC-001 |
| 5 | Ziel-API-Level 36 | BESTANDEN | 02 |
| 6 | 16-KB-Kompatibilität | BESTANDEN | 02 |
| 7 | Vollständiger Datenfluss | BESTANDEN | 03 |
| 8 | Korrekte Datenschutzerklärung | NICHT BESTANDEN | RT-001, DS-005, RT-003 |
| 9 | Korrektes Impressum, in App und Web erreichbar | NICHT BESTANDEN (Web 404; App ok) | WEB-001, RT-004 |
| 10 | Nutzungsbedingungen passend, wirksam einbezogen | NICHT BESTANDEN | RT-001(c), VR-001, RT-002 |
| 11 | Nachvollziehbare Rechtsgrundlagen | NICHT BESTANDEN (Einwilligung nicht durchgesetzt) | DS-001/002 |
| 12 | Besondere Kategorien, ausdrückliche Einwilligung vor KI-Übermittlung | NICHT BESTANDEN | DS-001/002 |
| 13 | Internationale Übermittlungen geprüft | NICHT VERIFIZIERBAR (Google/Groq-Verträge) / NICHT BESTANDEN (Microsoft) | DS-003, DS-011 |
| 14 | Auftragsverarbeitung geprüft | NICHT VERIFIZIERBAR | DS-011 |
| 15 | Löschung und Widerruf | BESTANDEN mit Auflagen | DS-010, DS-008 |
| 16 | Altersfreigabe bestätigt (16+) | NICHT BESTANDEN (Texte 13+) | ALT-001 |
| 17 | Play-Zielgruppe (16–17, 18+) | NICHT VERIFIZIERBAR (Console) | GP-005 |
| 18 | KI-Hinweis (Zeitpunkt, Sichtbarkeit) | BESTANDEN | 05 |
| 19 | Meldefunktion für KI-Inhalte | NICHT BESTANDEN | KI-001 |
| 20 | Krisenhinweise Deutschland korrekt | BESTANDEN (Ergänzung 116 111/116 123 empfohlen) | 05 |
| 21 | Data-Safety-Erklärung bestätigt | NICHT VERIFIZIERBAR (Console); Tabelle liegt vor | 07 |
| 22 | Health-Apps-Deklaration vorbereitet | BESTANDEN | 07 |
| 23 | Medizinprodukt/Gesundheitsversprechen | BESTANDEN | 05 |
| 24 | Öffentliche Datenschutz-URL | NICHT BESTANDEN | WEB-001 |
| 25 | Website geprüft | NICHT BESTANDEN | 09 |
| 26 | Keine Debug-Logs mit Nutzerinhalten im Release | NICHT BESTANDEN | SEC-002 |
| 27 | Backup- und Löschkonzept konsistent | BESTANDEN mit Auflagen | DS-009, DS-008 |
| 28 | Verzeichnis von Verarbeitungstätigkeiten (Entwurf) | BESTANDEN (Entwurf 16) | 16 |
| 29 | DSFA-Entscheidung dokumentiert | BESTANDEN (Screening 17: erforderlich) | 17 |
| 30 | Keine veralteten Rechtsverweise (TMG, TTDSG, OS-Plattform) | BESTANDEN | 08 |
| 31 | Testpflicht des Entwicklerkontos geklärt | NICHT VERIFIZIERBAR | GP-005 |
| 32 | Deutschland als einziges Produktionsland | NICHT VERIFIZIERBAR (Console) – Anleitung in 07 | 07 |
| 33 | Keine offenen Release-Blocker | NICHT BESTANDEN (4 offen) | 01 |

## Antworten in zehn Sätzen
1. „Best Journal Android“ kann **jetzt nicht** in Deutschland veröffentlicht werden; vier Release-Blocker sind offen.
2. Empfohlenes Mindestalter: **16 Jahre**.
3. In der Play Console sind die Altersgruppen **16–17** und **18 und älter** auszuwählen, nicht 13–15.
4. Vor der Veröffentlichung müssen zwingend erledigt werden: Consent-Durchsetzung für Groq und Gemini (DS-001/002), Textkorrekturen (RT-001) und das Hosting der Rechtstexte (WEB-001).
5. Dringend, aber nicht blockierend, sind Groq-Proxy, TTS-Umstellung, Re-Consent, Übermittlungsumfang, DSFA/DSB, AD_ID, Alter 16 und die Meldefunktion am KI-Output.
6. Als freiwillige spätere Optimierung verbleiben Bibliotheks-Updates, FLAG_SECURE, Analytics-Reset, Alarm-Codepfad, Dokument-Bereinigung, Impressum-Feinschliff, API-Key-Einschränkung, Barrierefreiheit und Asset-Pack für das Whisper-Modell.
7. Selbst beschaffen müssen Sie die Nachweise E-01 bis E-21 aus Bericht 12, insbesondere Play-Kontotyp/Testpflicht, Groq-DPA/ZDR, Google-DPA/Blaze-Plan, OAuth-Verifizierung und den Impressumservice-Vertrag.
8. Nach Umsetzung der Blocker und der HOCH-Punkte ist mit Entscheidung „B – release-fähig mit nicht blockierenden Punkten“ zu rechnen, sofern die externen Nachweise vorliegen.
9. Eine anwaltliche Freigabe der geänderten Rechtstexte (insbesondere Widerrufs- und Vertragskonstruktion über Google Commerce) wird empfohlen.
10. Vollständige Rechtssicherheit oder Abmahnfreiheit kann nicht garantiert werden.
