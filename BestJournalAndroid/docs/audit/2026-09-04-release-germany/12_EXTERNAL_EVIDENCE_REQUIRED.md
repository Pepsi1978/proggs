# 12 – Externe Nachweise, die nur der Auftraggeber beschaffen kann

**Prüfgegenstand:** Best Journal Android (`com.bestjournal.app`), 0.21.17 (298) · **Prüfstand:** `01b200085a7067123e5e21aba8021bc9193033a6`, `main` · **Prüfdatum:** 04.09.2026 · **Modell:** Claude Fable 5.1
**Beweisklassen:** [T] technisch bestätigt · [D] dokumentierte Anbieteraussage · [R] rechtliche Bewertung · [A] plausible Annahme · [U] ungeklärte externe Tatsache

Alle Nachweise bitte als Screenshot mit sichtbarem Datum unter `docs/audit/2026-09-04-release-germany/evidence/` ablegen (Ordner wird in Phase 2 angelegt).

| Nr. | Was | Wo prüfen | Was bestätigt werden soll | Ablage |
|---|---|---|---|---|
| E-01 | Play-Konto: Kontotyp (privat/Organisation), Erstellungsdatum, Verifizierungsstatus, Produktionszugang | play.google.com/console → Einstellungen → Entwicklerkonto → Kontodetails | Ob geschlossener Test (12 Tester, 14 Tage) nötig ist; ob Identitätsprüfung abgeschlossen ist | `evidence/play-account.png` |
| E-02 | Händlerangaben (DSA) | Console → Einstellungen → Entwicklerkonto → Kontaktdaten/„Händlerstatus“ | Welche Adresse/Telefon/E-Mail öffentlich angezeigt werden; ob c/o-Anschrift akzeptiert wurde | `evidence/play-trader.png` |
| E-03 | Groq: Vertrag | console.groq.com → Settings → Legal/Terms (oder trust.groq.com) | DPA/Services Agreement mit SCC akzeptiert (Datum) | `evidence/groq-dpa.png` |
| E-04 | Groq: Zero Data Retention | console.groq.com → Settings → Data Controls | ZDR aktiv (dann DSE „keine Speicherung über die Verarbeitung hinaus“), sonst „bis zu 30 Tage“ | `evidence/groq-zdr.png` |
| E-05 | Google: Data Processing Addendum | console.cloud.google.com → IAM & Admin → Datenschutz/Compliance bzw. Firebase → Projekteinstellungen → Terms | DPA (Google Cloud/Firebase) akzeptiert; Subprozessorenliste abrufbar | `evidence/google-dpa.png` |
| E-06 | Firebase-Tarif/Gemini-Kontingent | console.firebase.google.com/project/bestjurnal-a15b9/usage/details | Blaze-Plan (aktives Cloud-Billing) → Datenregeln der bezahlten Dienste; Backend Gemini Developer API vs. Vertex AI; Region | `evidence/firebase-plan.png` |
| E-07 | OAuth-Consent-Screen | console.cloud.google.com/apis/credentials/consent?project=bestjurnal-a15b9 | Verifizierungsstatus für Scope `gmail.send` (sensitiv) und `drive.appdata`; Veröffentlichungsstatus „In Produktion“; Nutzerlimit | `evidence/oauth-consent.png` |
| E-08 | Firebase-API-Key-Einschränkung | console.cloud.google.com/apis/credentials?project=bestjurnal-a15b9 | Android-Key auf Paketname `com.bestjournal.app` + SHA-256 beschränkt; alter Key (AIzaSyCY…) gelöscht | `evidence/api-keys.png` |
| E-09 | App Check | console.firebase.google.com → App Check | Play-Integrity-Anbieter für Release-Signatur (Play-App-Signing-SHA-256) registriert; Erzwingung für Firebase AI Logic und Functions aktiv | `evidence/appcheck.png` |
| E-10 | DPF-Listung | dataprivacyframework.gov/list → Suche „Google LLC“ und „Groq“ | Google LLC aktiv gelistet; Groq vermutlich nicht (dann SCC) | `evidence/dpf-list.png` |
| E-11 | Impressumservice-Vertrag | Vertragsunterlagen „Dein-Impressum“ | Zustellungsannahme und Weiterleitung von Post (ladungsfähige Anschrift) | `evidence/impressumservice.pdf` |
| E-12 | Markenrecherche | register.dpma.de, euipo.europa.eu/eSearch, Play-Store-Suche „Best Journal“ | Keine ältere verwechslungsfähige Marke für Klasse 9/42 | `evidence/marken.md` |
| E-13 | Hosting-Vertrag/Serverstandort | Hostinger-Kundenkonto | Anbieter, Serverstandort (EU?), Logfile-Speicherdauer → für Website-Datenschutz | `evidence/hostinger.png` |
| E-14 | Versicherung | eigene Unterlagen | Berufs-/Produkthaftpflicht für Software (empfohlen, keine Pflicht) | `evidence/versicherung.md` |
| E-15 | Supportprozess | eigene Notiz | Postfach `dev.app.support@gmail.com`: Bearbeitung von Betroffenenanfragen binnen 30 Tagen; Vertretung bei Abwesenheit | `docs/SUPPORT-PROZESS.md` |
| E-16 | Datenschutzbeauftragter | Angebot eines externen DSB | Bestellung + Meldung an HBDI (nach DS-006) | `evidence/dsb.pdf` |
| E-17 | EuGH C-703/25 P | curia.europa.eu → Suche „C-703/25 P“ (im Browser) | Kein Urteil, das den DPF-Beschluss aufhebt (Stand am Veröffentlichungstag) | `evidence/dpf-status.md` |
| E-18 | Produkthaftungsgesetz | dip.bundestag.de → „Modernisierung des Produkthaftungsrechts“ | Verfahrensstand (Verkündung?) → Watchlist | `evidence/prodhaft.md` |
| E-19 | Play-Console-Deklarationen | Console → Richtlinien → App-Inhalte | Screenshots aller ausgefüllten Formulare (Data Safety, Health, Werbe-ID, Zielgruppe, KI) | `evidence/play-app-content/` |
| E-20 | Store-Assets | eigene Dateien | Icon 512×512 PNG, Feature-Grafik 1024×500, ≥2 Telefon-Screenshots ohne Fremdmarken/erfundene Funktionen; Beschreibung ohne Superlative | `play-store-metadata/listing-de.md` + Bilder |
| E-21 | Gemini-Region/Backend | Firebase AI Logic-Konfiguration | Entscheidung Gemini Developer API (USA) vs. Vertex AI (EU-Region möglich) | `evidence/gemini-backend.md` |
