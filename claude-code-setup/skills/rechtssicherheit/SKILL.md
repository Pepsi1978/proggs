---
name: rechtssicherheit
description: >
  Prueft Android-Apps auf Abmahnungssicherheit fuer den internationalen Play-Store-Release.
  Recherchiert parallel aktuelle Pflichtangaben fuer DSGVO/GDPR (EU), CCPA (USA), UK-GDPR,
  PIPL (China), DPDP (Indien), APPI (Japan), PIPA (Korea), LGPD (Brasilien),
  Google Play Data Safety, TMG/DDG-Impressum, Widerrufsbelehrung und AGB.
  Gleicht Befunde gegen bestehende App-Dokumente (Datenschutzerklaerung, Nutzungsbedingungen,
  Impressum, Widerrufsbelehrung) und deren Platzierung/Uebersetzung in der App ab.
  Erstellt/pflegt `~/proggs/rechtssicherheit.md` als Wissensbasis.
  Deutsche Trigger: "starte den Skill Rechtssicherheit", "pruefe [App] auf Rechtssicherheit",
  "Rechtssicherheit fuer [App]", "DSGVO-Check fuer [App]", "ist [App] abmahnungssicher",
  "Abmahnungscheck [App]", "Play-Store rechtskonform pruefen", "Rechts-Audit [App]".
  Nutze diesen Skill immer wenn es um Datenschutz-Compliance, Abmahnungsrisiken, Play-Store-Release
  oder rechtliche Pflichttexte einer App geht — auch wenn der Benutzer den Skill-Namen
  nicht explizit nennt, aber von Datenschutzerklaerung, Impressum, AGB, Widerruf oder
  DSGVO-Konformitaet spricht.
invocation: user
---

# Skill: Rechtssicherheit

> **Wichtiger Disclaimer:** Dieser Skill ist eine **technische Pruefhilfe** und ersetzt
> KEINE anwaltliche Beratung. Er markiert fehlende Pflichtangaben und typische Fallstricke
> basierend auf oeffentlichen Quellen. Fuer eine verbindliche Rechtspruefung muss immer
> ein Fachanwalt fuer IT-Recht konsultiert werden. Den Benutzer am Anfang UND am Ende
> des Berichts darauf hinweisen.

---

## Ziel

Eine Android-App vor dem Play-Store-Release so pruefen, dass sie in den Ziel-Maerkten
so weit wie technisch ueberpruefbar **abmahnungssicher** ist:

1. Pflichtangaben vorhanden (Datenschutz, ToS, Impressum, Widerruf)
2. Platzierung in der App korrekt (Onboarding, Settings, Consent-Screen)
3. Uebersetzung in alle Play-Store-Sprachen der App
4. Inhalt aktuell (DSGVO-Stand, CCPA, PIPL, DPDP, etc.)
5. Google Play Data Safety Declaration stimmt mit App-Verhalten ueberein

---

## Ablauf (7 Schritte — strikt in dieser Reihenfolge)

### Schritt 1 — App-Namen klaeren

Wenn der Benutzer den App-Namen nicht genannt hat, **einmal kurz fragen** auf Deutsch:

> "Welche App soll ich pruefen? (z.B. BestJournal, EntropyJournal, PenAndPage)"

Wenn er die App genannt hat, direkt zu Schritt 2.

### Schritt 2 — Referenz-Datei laden

Pruefe ob `~/proggs/rechtssicherheit.md` existiert.

| Zustand | Aktion |
|---------|--------|
| **Existiert** | Komplett einlesen. Diese Datei ist die Wissensbasis aus frueheren Sessions und wird am Ende aktualisiert. |
| **Fehlt** | Nach der Recherche (Schritt 3) wird sie zum ersten Mal angelegt. |

Dem Benutzer kurz melden: *"Lese Referenz-Datei..."* oder *"Lege Referenz-Datei neu an."*

### Schritt 3 — Internet-Recherche (parallel)

Starte **5 Researcher-Agenten parallel** in EINER Nachricht mit mehreren `Agent`-Tool-Aufrufen.
Dies ist Pflicht — nie sequentiell. Jeder Researcher: **max 50 Ergebnisse, max 15 Web-Fetches,
max 10 Minuten Laufzeit, max 2000 Woerter Prompt**.

Dem Benutzer vor dem Start sagen:
> "Ich starte 5 parallele Researcher fuer DE/EU, US/UK, Asien, Play-Store und Abmahn-Trends.
> Laufzeit: ~5-8 Minuten."

**Researcher-Aufteilung (fix, nicht veraendern):**

| # | Agent | Fokus | Wichtige Quellen |
|---|-------|-------|------------------|
| 1 | `researcher` | **DE/EU** — DSGVO aktueller Stand, TMG/DDG-Impressum, BGH/EuGH-Rechtsprechung, TTDSG/ePrivacy, Widerrufsbelehrung-Muster, AGB-Pflichten | dsgvo-gesetz.de, datenschutz.org, gesetze-im-internet.de, haendlerbund.de, bundesjustizamt.de |
| 2 | `researcher` | **US/UK/CA/AU** — CCPA/CPRA, UK-GDPR, PIPEDA, Privacy Act, COPPA (wenn Kinder-Feature) | oag.ca.gov, ico.org.uk, priv.gc.ca, oaic.gov.au |
| 3 | `researcher` | **Asien** — PIPL (China), DPDP (Indien), APPI (Japan), PIPA (Korea) inklusive Cross-Border-Transfer-Regeln | cac.gov.cn, meity.gov.in, ppc.go.jp, pipc.go.kr |
| 4 | `researcher` | **Google Play Policies** — Data Safety Form (aktuell), User Data Policy, Permissions, Sensitive Permissions, Families Policy, AI-generated Content | support.google.com/googleplay/android-developer, play.google.com/console |
| 5 | `researcher` | **Aktuelle Abmahnwellen 2025/2026** — Google Fonts/Analytics-Integration, Cookie-Consent-Urteile, fehlendes Impressum, AI-Act-Pflichten, unvollstaendige Widerrufsbelehrung | it-recht-kanzlei.de, dr-bahr.com, wbs.legal (News-Bereich) |

**Prompt-Muster pro Researcher (anpassen pro Fokus):**

```
Recherchiere fuer [FOKUS] die aktuellen (Stand {Monat/Jahr}) rechtlichen Pflichtangaben
einer Android-App im Google Play Store.

Liefere strukturiert zurueck:
1. PFLICHTANGABEN-LISTE: Was muss zwingend in Datenschutz/ToS/Impressum/Widerruf stehen?
2. MUSTER-KLAUSELN: Offizielle oder weit verbreitete Formulierungen mit Quelle.
3. SPRACHANFORDERUNG: Muessen die Texte in der Landessprache vorliegen oder reicht Englisch?
4. SANKTIONEN: Bussgelder / Abmahnrisiko bei Verstoss.
5. AKTUELLE AENDERUNGEN: Was hat sich in den letzten 12 Monaten geaendert?
6. QUELLEN: Offizielle URLs mit Abrufdatum.

Limits: max 50 Ergebnisse, max 15 Web-Fetches, max 10 Minuten.
Bei Netzwerkfehlern: das zurueckgeben was da ist, nicht crashen.
```

**Wenn ein Researcher fehlschlaegt:** Sofort dem Benutzer auf Deutsch melden, die anderen
weiterlaufen lassen, nicht still weitermachen.

### Schritt 4 — Ergebnisse konsolidieren

Wenn alle 5 Researcher zurueck sind:

1. Ergebnisse zusammenfuehren in eine **Pflichtangaben-Matrix** (Markt × Dokumenttyp).
2. **Sprachmatrix** erstellen: Welche Maerkte verlangen die Landessprache zwingend?
   (Kurzantwort auf die Benutzer-Frage: "Reichen Deutsch+Englisch?" — die Recherche
   klaert das. Typische Regel 2026: DE/AT verlangen Deutsch, FR Franzoesisch,
   China/Korea zwingend Landessprache, UK/US Englisch ausreichend.)
3. **Abmahn-Hotspots** markieren (Google Fonts, Analytics ohne Consent, fehlende
   Cookie-Banner, unvollstaendige Widerrufsbelehrung).

### Schritt 5 — App-Pruefung

Die genannte App im Repo finden und systematisch pruefen.

**5a. Rechts-Dokumente im Projekt finden:**

```
Glob: **/res/raw/*privacy*, **/res/raw/*terms*, **/res/raw/*impressum*
Glob: **/assets/*privacy*, **/assets/*terms*, **/assets/*legal*
Glob: **/res/values*/strings.xml
Grep in strings.xml: "datenschutz|privacy|terms|impressum|widerruf|agb"
Grep in Code (Kotlin/Compose): "ConsentScreen|PrivacyPolicy|TermsOf|Impressum|Widerruf"
```

**5b. Inhalts-Pruefung:** Jedes gefundene Dokument gegen die Pflichtangaben-Matrix
aus Schritt 4 abgleichen. Fehlende Pflichtangaben als Befund notieren.

**5c. Platzierungs-Pruefung:** Ueber Grep/Compose-Suche pruefen:

| Pflicht-Platzierung | Typisches Muster |
|--------------------|------------------|
| **Onboarding/Consent-Screen** | `ConsentScreen.kt`, Link zur Datenschutzerklaerung VOR Datenerhebung |
| **Settings-Screen** | Menupunkte "Datenschutz", "Nutzungsbedingungen", "Impressum" dauerhaft erreichbar |
| **Ueber-/About-Screen** | Impressum-Link mit Kontaktdaten |
| **Consent-Widerruf** | Benutzer kann Zustimmung nachtraeglich widerrufen (DSGVO Art. 7 Abs. 3) |

**5d. Sprach-Pruefung:** `app/src/main/res/values-XX/strings.xml` auflisten. Welche
Locales hat die App? Abgleich mit Play-Store-Release-Sprachen. Fehlende Uebersetzungen
der Rechtstexte als Befund notieren.

**5e. Google Play Data Safety:** Falls vorhanden, `play-store-metadata/*` oder
Hinweise auf Data-Safety-Deklaration pruefen. Vergleich mit tatsaechlichen App-Permissions
(`AndroidManifest.xml`) und verwendeten SDKs (Firebase, Analytics, Ads, Crashlytics).

### Schritt 6 — Bericht erstellen

Befunde strukturiert ausgeben. Schweregrad nach diesem Schema:

| Grad | Bedeutung |
|------|-----------|
| 🔴 **KRITISCH** | Sofortige Abmahnungs- oder Bussgeldgefahr. Release blockieren. |
| 🟠 **HOCH** | Grosse Luecke, muss vor Release gefixt werden. |
| 🟡 **MITTEL** | Sollte gefixt werden, aber nicht release-blockierend. |
| 🟢 **NIEDRIG** | Kosmetik / Best Practice. |

**Pflicht-Struktur des Berichts:**

```markdown
# Rechtssicherheits-Audit: [App-Name]
Datum: YYYY-MM-DD
Geprueft gegen: DSGVO, CCPA, UK-GDPR, PIPL, DPDP, APPI, PIPA, LGPD, Google Play Policies

## Disclaimer
Keine anwaltliche Beratung. Fachanwalt konsultieren vor Release.

## Zusammenfassung
- Gesamtstatus: [Release-faehig | Nicht release-faehig | Bedingt release-faehig]
- Kritische Befunde: N
- Hohe Befunde: N
- Mittlere Befunde: N

## Befunde nach Schweregrad
### 🔴 KRITISCH
1. [Befund] — Quelle: [Datei:Zeile] — Fix: [konkret]
...

### 🟠 HOCH
...

## Sprachen-Matrix
| Markt | Sprache | Pflicht? | Vorhanden? |
|-------|---------|----------|------------|
| DE | Deutsch | Ja | ✅/❌ |
| ...

## Laender-Matrix (Release-Freigabe)
| Land | DSGVO/Lokal | Rechtstext | Consent | Freigabe |
|------|-------------|------------|---------|----------|
| DE | ✅ | ✅ | ✅ | 🟢 FREI |
| US | ⚠ | ✅ | ❌ | 🟡 BEDINGT |
| ...

## Google Play Data Safety Check
- Deklarierte Daten: ...
- Tatsaechlich gesammelte Daten: ...
- Diskrepanzen: ...

## TODO-Checkliste (in Reihenfolge abarbeiten)
- [ ] Kritisch: ...
- [ ] Hoch: ...
- [ ] Mittel: ...
```

### Schritt 7 — Referenz-Datei aktualisieren

`~/proggs/rechtssicherheit.md` updaten (oder neu anlegen).

**Struktur der Referenz-Datei:**

```markdown
# rechtssicherheit.md — Wissensbasis
Letzte Recherche: YYYY-MM-DD
Naechste Pflicht-Pruefung: YYYY-MM-DD (+90 Tage)

## Pflichtangaben-Matrix (Master)
### Datenschutzerklaerung — EU/DSGVO
[Liste mit Quelle + Abrufdatum]
### Datenschutzerklaerung — USA/CCPA
...
### Impressum — DE/TMG-DDG
...
### Widerrufsbelehrung — EU
...
### AGB/Nutzungsbedingungen
...

## Sprach-Anforderungen pro Markt
[Matrix]

## Aktuelle Abmahn-Hotspots (Stand YYYY-MM)
[Liste mit Datum und Quelle]

## Geprueftes Apps-Log
| Datum | App | Gesamtstatus | Kritisch | Hoch |
|-------|-----|-------------|----------|------|
| ... | BestJournal | ... | N | N |

## Muster-Klauseln (Sammlung, mit Quelle)
### DSGVO — Datenverarbeitung Art. 6 Abs. 1 lit. f
...

## Quellen-Register
[URL | Thema | Zuletzt abgerufen]
```

**Diff-Logik bei Update:**
- Neue Erkenntnisse gegenueber dem gespeicherten Stand hervorheben (Diff-Abschnitt
  "**Aenderungen seit letzter Recherche**").
- Veraltete Eintraege (>90 Tage alt) als "zu verifizieren" markieren.
- Jede Pflichtangabe mit **Quell-URL + Abrufdatum** hinterlegen (wichtig fuer Nachvollziehbarkeit).

Danach committen und pushen — `rechtssicherheit.md` gehoert ins Repo (`~/proggs/`).

---

## Offene Fragen an den Benutzer (nur wenn noetig)

Wenn nach der Recherche noch unklar ist, den Benutzer **einmal gebuendelt** fragen
(nicht mehrfach unterbrechen):

1. "Soll die App weltweit veroeffentlicht werden oder nur in bestimmten Laendern?"
2. "Gibt es ein Gewerbe/Impressum-Pflicht? (Firma/Einzelunternehmer oder privater Hobby-Entwickler?)"
3. "Werden In-App-Kaeufe oder Abos angeboten? (Widerrufsbelehrung pflicht)"
4. "Werden Nutzerdaten an Dritte (Firebase, Analytics, Ads) weitergegeben?"
5. "Gibt es KI-Features? (Dann AI-Act relevant)"

Die Antworten in die Referenz-Datei eintragen, damit sie in Folge-Sessions nicht
erneut gefragt werden muessen.

---

## Was NIEMALS passieren darf

- ❌ Rechtliche Garantien aussprechen ("die App ist jetzt 100% abmahnungssicher")
- ❌ Konkrete Rechtstexte generieren die wie eine Anwaltsdienstleistung wirken
  (max. Muster-Hinweise mit Quelle und Empfehlung "durch Anwalt pruefen lassen")
- ❌ Sequentielle Researcher statt parallel (kostet 5x so lange)
- ❌ Researcher ohne Limits (max 50 Ergebnisse / 15 Fetches / 10 Min)
- ❌ Referenz-Datei nicht updaten am Ende (dann lernt das System nicht dazu)
- ❌ App-Pruefung ohne vorherige Recherche (dann fehlen Pflichtangaben-Kriterien)
- ❌ Rechtstexte nur in Deutsch und Englisch belassen ohne Pruefung welche Laender
  die Landessprache zwingend verlangen

---

## Beispiel-Dialog

**Benutzer:** "starte den Skill Rechtssicherheit und pruefe BestJournal"

**Claude:**
> Starte Rechtssicherheits-Audit fuer BestJournal.
>
> 1. Lade `~/proggs/rechtssicherheit.md` (oder lege sie neu an)...
> 2. Starte 5 parallele Researcher (DE/EU, US/UK, Asien, Play-Store, Abmahn-Trends).
>    Laufzeit: ~5-8 Minuten.
> 3. Pruefe danach App-Dokumente in `BestJournalAndroid/` (Datenschutz, ToS, Impressum,
>    Widerruf) und deren Platzierung + Uebersetzungen.
> 4. Erstelle Befund-Bericht und aktualisiere Referenz-Datei.
>
> **Disclaimer:** Technische Pruefhilfe, ersetzt keine anwaltliche Beratung.

[Researcher laufen parallel → Konsolidierung → App-Pruefung → Bericht → Referenz-Update → Commit+Push]

---

## Zusammenspiel mit anderen Skills

- **`uebersetzung`**: Wenn fehlende Sprachversionen erkannt werden — Vorschlag den
  Uebersetzungs-Skill zu starten.
- **`string-extraktor`**: Wenn Rechtstexte hardcodiert im Code stehen statt in strings.xml.
- **`superintelligenz` / `selbstbeobachtung`**: Neue Erkenntnisse (z.B. neues
  Abmahn-Urteil) werden in `rechtssicherheit.md` persistiert — Compound Intelligence Effect.

---

## Abschluss-Meldung

Am Ende IMMER:
1. Bericht ausgeben
2. `~/proggs/rechtssicherheit.md` commit+push
3. Disclaimer wiederholen
4. Intelligenz-Vorschlaege (Direktive #2) falls Muster erkannt (z.B. "alle deine Apps
   haben denselben Impressum-Fehler — soll ich einen Hook bauen der das checkt?")
