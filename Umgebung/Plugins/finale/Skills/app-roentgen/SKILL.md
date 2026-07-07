---
name: app-roentgen
description: |
  Durchleuchtet eine Android-App systematisch wie ein Roentgengeraet — extrahiert ALLE Funktionen, Bildschirme, Klick-Pfade, Paywall-Stufen und Hidden Features aus dem Quellcode, damit Werbeaussagen 1:1 gegen die Code-Realitaet geprueft werden koennen (UWG §5, EU UCPD, Google Play Policy).

  Nutze diesen Skill IMMER bei: "App durchleuchten", "App roentgen", "Roentgen-Skill", "X-Ray", "Durchleuchten", "Werbeaussagen pruefen", "Werbeaussagen-Audit", "UWG-Audit", "Paywall-Audit", "Marketing-Compliance", "Feature-Audit", "Feature-Inventar", "App-Architektur extrahieren", "alle Funktionen einer App auflisten", "App komplett analysieren", "App-Analyse fuer Rechtssicherheit", "stimmt was wir versprechen mit der App ueberein", "was kann die App genau", "Compliance-Pruefung". Funktioniert fuer beliebige Android-Apps (Kotlin/Compose bevorzugt, Java/XML auch unterstuetzt). Erzeugt einen strukturierten Audit-Bericht mit 1:1-Wortlauten, Paywall-Inventar, Permission-Mapping und Werbeaussage-vs-Feature-Matrix.

  NICHT triggern fuer iOS-only-Apps, reine Flutter-/React-Native-/KMP-iOS-Builds, Web-Apps oder Backend-Services — der Skill liest Android-spezifische Patterns (AndroidManifest.xml, strings.xml, BillingClient, Compose) und liefert bei Nicht-Android-Codebases keine sinnvollen Ergebnisse. Bei Hybrid-Apps mit Android-Anteil (z.B. Flutter-Projekt mit `android/`-Ordner): nur den `android/`-Unterordner als Eingabe geben.
allowed-tools:
  - Bash
  - Read
  - Grep
  - Glob
  - Write
---

# App-Roentgen: Vollstaendiges Architektur- und Werbeaussagen-Audit fuer Android-Apps

## Abgrenzung zu anderen Skills

Dieser Skill ist eine GRUNDLAGEN-Schicht — sein Output wird von anderen Skills konsumiert. Nicht aufrufen wenn die Hauptaufgabe ein anderes Ziel hat:

| Anfragetyp | Richtiger Skill |
|-----------|----------------|
| Reine DSGVO-/Datenschutz-/Impressums-Pruefung | `rechtssicherheit` |
| Hardcoded Strings finden + neue Strings erstellen | `string-extraktor` |
| Bestehende deutsche Strings in andere Sprachen uebersetzen | `uebersetzung` |
| Monetarisierungs-/Conversion-Beratung (Paywall-Optimierung) | `app-monetizer` |
| UI/UX-Design-Audit (Farben, Typografie, Material 3) | `designer` |

Wenn Werbeaussagen-Audit, Feature-Inventar oder Paywall-Compliance gefragt sind: **dieser Skill**. Wenn die nachgelagerten Skills explizit den Roentgen-Output brauchen, ist es legitim diesen zuerst zu fahren und dann den anderen — aber nicht "DSGVO-Check" mit diesem Skill anfangen.

## Kernzweck

Der Skill durchleuchtet eine Android-App so gruendlich, dass am Ende eine 100-Prozent-vollstaendige Liste vorliegt was die App in jedem einzelnen Bildschirm, bei jedem Klick und in jedem Abbruchpfad genau tut. Ziel ist Rechtssicherheit: Wenn eine Werbeaussage behauptet "Unbegrenzte KI-Analysen" muss der Skill verifizieren ob das im Code wirklich stimmt — oder ob ein Limit von 150 pro Tag drin ist.

Der finale Bericht hat vier Teile:
1. **Architektur-Inventar** — alles was die App kann, in 15 Kategorien gegliedert
2. **Wortlaut-Mapping pro Bereich** — fuer JEDEN Bildschirm, Dialog, Bottom-Sheet, jedes Menue und jedes Untermenue (rekursiv, beliebige Tiefe), jedes Settings-Item, jede Notification, jeden Snackbar/Toast/Error-State werden die exakten 1:1-Wortlaute zitiert
3. **Translation-Context** — pro Wortlaut die Daten die ein Uebersetzer braucht (Laengen, xliff:g, Notes, Plurals, HTML, Format-Argumente, Glossar, Du/Sie-Konsistenz)
4. **Werbeaussage-vs-Feature-Matrix** — jede beworbene Aussage gegen die Code-Realitaet geprueft

Der Bericht dient zwei nachgelagerten Konsumenten:
- **Rechtssicherheits-Skill** — prueft Wortlaute gegen UWG, EU UCPD, DSGVO, Google Play Policy
- **Uebersetzungs-Skill** — uebersetzt Wortlaute mit voller Context-Information (Slot, Laenge, Plural-Regeln, Glossar, Argumente)

## Warum 1:1-Wortlaute (KRITISCH)

Der Rechtssicherheits-Skill, der nach dem Roentgen-Audit laeuft, prueft jede Formulierung gegen UWG, EU UCPD, Google Play Policy, DSGVO und Verbraucherschutzrecht. **Schon ein einziges falsches oder fehlendes Wort kann eine Abmahnung ausloesen** — Beispiele: "Geld zurueck" statt "Geld-zurueck-Garantie unter Bedingungen X", "unbegrenzt" statt "bis zu 150/Tag", "anonym" statt "pseudonym".

Deshalb gilt fuer ALLE Wortlaute im Bericht:

| Regel | Bedeutung |
|-------|-----------|
| **1:1 woertlich** | Exakte Zeichenfolge inkl. Satzzeichen, Gross-/Kleinschreibung, Sonderzeichen, Umlauten, Leerzeichen |
| **In Anfuehrungszeichen** | Jeder Wortlaut wird in `"..."` zitiert um sichtbar zu machen wo er anfaengt und endet |
| **Mit Quelle** | Datei + Zeile + String-Resource-Key (`strings.xml:42 → R.string.paywall_unlimited_ai`) |
| **Pro Sprache** | Wenn die App mehrsprachig ist, jeden Wortlaut in allen Sprachen zitieren |
| **Keine Zusammenfassung** | Niemals "der Text sagt sinngemaess..." — IMMER der Original-Text |
| **Plurals + Formate** | Auch `<plurals>` und Format-Strings mit `%s`/`%d` werden so wie sie im Code stehen zitiert |

**Verbotsliste:**
- ❌ "Headline lautet etwa 'Premium starten'"
- ❌ "Im Dialog steht eine Bestaetigungsfrage"
- ❌ Nur den String-Key zitieren ohne den Wortlaut
- ❌ Bei langen Texten kuerzen mit "..."

**Verpflichtend:**
- ✅ `Headline (paywall_title @ strings.xml:88): "Jetzt Premium starten und alle Vorteile sichern"`
- ✅ `Bestaetigung (delete_confirm_title): "Wirklich loeschen?" / Body (delete_confirm_body): "Diese Aktion kann nicht rueckgaengig gemacht werden."`

## Wann triggern

Frank sagt typischerweise Phrasen wie "Lass die BestJournal mal durchleuchten", "Roentgen-Audit fuer App XY", "Werbeaussagen-Pruefung fuer Paywall", "Was macht die App wirklich". Der Trigger ist absichtlich grosszuegig — lieber einmal zu oft als einmal zu wenig.

Der Skill wird typischerweise vor einem Marketing-Audit oder vor einem Release ausgefuehrt, oder wenn die Frage aufkommt ob die App das wirklich macht was sie verspricht.

## Optionaler Scope-Parameter (NEU 2026-05-22 — Frank-Direktive für Parallelisierung)

**Standard-Verhalten (UNVERAENDERT — Rueckwaertskompatibilitaet):**
Ohne Parameter laeuft der Skill in den 7 Schichten KOMPLETT durch. Output ist
der volle Layer-Bericht (`layer1_manifest` ... `layer7_marketing_claims_matrix`).
Bestehende Konsumenten (rechtssicherheits-skill, uebersetzungs-skill, finale-Plugin)
sehen KEINE Veraenderung — die volle Funktionalitaet ist 100% erhalten.

**Neuer optionaler Scope-Parameter:**
Wenn der Skill von einem PARALLEL-WORKER-Setup aufgerufen wird (z. B. das `finale`-Plugin
spawnt 15-20 Worker fuer einen Layer-Split), kann ein Scope-Parameter uebergeben werden:

| Scope-Wert | Welche Layer scannt der Skill |
|------------|-------------------------------|
| `permissions`     | Nur Layer 1 (Manifest, Permissions, Receiver, Activities) |
| `dependencies`    | Nur Layer 2 (Gradle-Dependencies, SDK-Versionen) |
| `architecture`    | Nur Layer 3 (Modul-Struktur, Hilt, Room-Schemas) |
| `screens`         | Nur Layer 4 + 4b + 4c (Bildschirme, Wortlaute, Translation-Context) |
| `legal-texts`     | Nur Layer 4d (Privacy/Imprint/AGB/Help-Inventar) |
| `external-content`| Nur Layer 4e (externe URLs, Webseiten-Referenzen) |
| `paywall`         | Nur Layer 5 (Paywall-Tiefenanalyse) |
| `hidden-features` | Nur Layer 6 (Hidden Features, Easter Eggs) |
| `marketing`       | Nur Layer 7 (Werbeaussagen-vs-Feature-Matrix) |
| `strings-bulk`    | Spezial-Scope: nur 1:1-Wortlaute aus strings.xml + Compose-Literalen, kein Architektur-Reasoning. Schnellster Scope, ideal fuer Mass-Translation. |
| `delta`           | Spezial-Scope: nur was sich seit `<previous-report>` geaendert hat (braucht zusaetzlich `--since-report <path>`). |
| (kein Parameter) | **Default — voller 7-Schichten-Scan wie bisher** |

**Aufruf-Form:**
```
# Standard (unveraendert):
Skill(skill="app-roentgen")  # voller Scan

# Mit Scope (parallel-worker-Modus):
Skill(skill="app-roentgen", args="--scope=paywall")
Skill(skill="app-roentgen", args="--scope=permissions")
# Optionaler Delta-Modus:
Skill(skill="app-roentgen", args="--scope=delta --since-report .android-shield/roentgen-report.json")
```

**Output bei Scope-Begrenzung:**
- Selbes JSON-Schema wie Vollscan, aber nur die betroffenen Layer-Keys sind gefuellt
- Andere Layer-Keys sind explizit `null` (nicht weggelassen — Konsumenten merken so dass es Teil-Output ist)
- Pflicht-Feld `_scope: "<scope-name>"` im Output, damit der Synthesizer beim Mergen
  mehrerer Worker-Outputs sauber zusammenfuehren kann
- Pflicht-Feld `_partialReport: true` bei Scope-Begrenzung

**Funktionalitaets-Diff (Direktive #3 Pflicht):**

| Bereich          | Vorher    | Nachher (mit Scope-Param) | Ohne Scope-Param |
|------------------|-----------|---------------------------|-------------------|
| Voller Layer-Scan| ✅ funktioniert | (nicht relevant)         | ✅ funktioniert (unveraendert) |
| Schichten-Reihenfolge | ✅ Layer 1→7 erzwungen | ✅ pro Scope einzelne Layer | ✅ Layer 1→7 (unveraendert) |
| Output-Schema    | ✅ alle Layer-Keys gefuellt | ⚠️ nicht angeforderte Keys = null | ✅ alle Keys gefuellt (unveraendert) |
| Konsumenten (recht, uebersetzung, finale) | ✅ funktioniert | ✅ funktioniert (sieht null und ignoriert) | ✅ funktioniert (unveraendert) |
| Parallel-Worker-Setup | ❌ unmoeglich (monolith) | ✅ moeglich (Layer-Split) | (nicht relevant) |

**KEINE bestehende Funktionalitaet wird entfernt oder eingeschraenkt.**

## Pflicht-Vorgehen: Die 7 Schichten der Durchleuchtung

Reihenfolge ist wichtig. Jede Schicht baut auf der vorherigen auf. KEINE Schicht ueberspringen — sonst entstehen Luecken im Audit und der Roentgen-Effekt geht verloren.

### Schicht 1 — Manifest-Analyse: Was darf die App?

Liest `app/src/main/AndroidManifest.xml` und extrahiert alles was deklariert ist: Permissions, Activities, Services, Receiver, ContentProvider, Intent-Filter, Deep-Links, Backup-Konfiguration. Jede Permission impliziert Features (CAMERA → Foto-Capture, RECORD_AUDIO → Voice-Input).

→ **Detail-Anleitung**: `references/layer-1-manifest.md`
→ **Permission-zu-Feature-Tabelle**: `references/permission-feature-map.md`

### Schicht 2 — Dependency-Analyse: Was kann die App technisch?

Liest `build.gradle.kts`, `libs.versions.toml`, `settings.gradle.kts`. Jede Bibliothek impliziert Capabilities (firebase-messaging → Push, play-billing → Paywall, mlkit-* → KI/ML, room-runtime → Datenbank). Auch Pruefung ob Dependencies aktiv genutzt werden oder tot sind.

→ **Detail-Anleitung**: `references/layer-2-dependencies.md`

### Schicht 3 — Architektur-Inventar: Wie ist die App gebaut?

Extrahiert alle ViewModels, Repositories, UseCases, Hilt-Module, Room-Entities, Workers (WorkManager). Das ergibt das Skelett der App: welche Daten existieren, welche Business-Logik laeuft, welche Background-Jobs gibt es. Jeder ViewModel = ein Feature-Cluster.

→ **Detail-Anleitung**: `references/layer-3-architecture.md`
→ **Optional — Goldstandard 2026 (Compose Compiler Reports + KSP-NavGraph + ast-grep + mobsfscan)**: `references/layer-3b-compose-compiler-reports.md`. Bei zugaenglichem Build empfohlen — liefert nachweislich vollstaendiges Composable-Inventar direkt vom Compiler statt grep-Heuristik.

### Schicht 4 — Bildschirm-Karte: Was sieht der Nutzer?

Extrahiert jeden einzelnen Compose-Screen, jede Navigation-Route, jeden Click-Handler, jeden Side-Effect (LaunchedEffect mit Navigation), jeden Dialog, jedes Bottom-Sheet. Erzeugt eine Bildschirm-Karte als Mermaid-Diagramm UND als Baum.

Fuer jeden Bildschirm wird dokumentiert:
- Wie kommt der Nutzer dorthin (alle Entry-Points)
- Welche Klicks/Aktionen sind moeglich
- Wohin fuehrt jeder Klick
- Welche Side-Effects gibt es
- Welche State-Klassen existieren (sealed class XxxState)
- **Alle 1:1-Wortlaute auf dem Screen** (siehe Schicht 4b)

→ **Detail-Anleitung**: `references/layer-4-screens-and-flows.md`

### Schicht 4b — Wortlaut-Mapping pro Bereich (PFLICHT — Grundlage fuer Rechtssicherheit)

Fuer JEDEN Bereich der App werden die exakten Wortlaute extrahiert und einer Tabelle zugeordnet: Screens, Dialoge, Bottom-Sheets, Snackbars, Toasts, Error-States, Empty-States, Loading-States, Push-Notifications, Menues, Settings-Items.

**Menues werden rekursiv aufgeloest, beliebige Tiefe.** Wenn Settings einen Eintrag "Konto" hat, der zu "Sicherheit" fuehrt, der wiederum "2FA" enthaelt mit Unter-Optionen "Backup-Codes" → JEDE dieser Ebenen bekommt eine eigene Wortlaut-Tabelle. Keine Abkuerzungen, kein "und so weiter".

→ **Detail-Anleitung**: `references/layer-4b-wortlaut-mapping.md`

### Schicht 4c — Translation-Context (PFLICHT — Grundlage fuer Uebersetzungs-Skill)

Pro Wortlaut werden die Daten erfasst die ein Uebersetzer braucht, damit eine korrekte Lokalisierung moeglich ist:

- **Slot-Laengen-Audit** — passt der Text in seinen UI-Slot, wird er nach der Uebersetzung noch passen?
- **`translatable="false"`** — welche Strings sind explizit gesperrt (Markennamen, Versionen, URLs)
- **`xliff:g`-Tags** — welche Inline-Teile sind nicht-uebersetzbar (Beispiele in Format-Strings)
- **XML-Kommentare als Uebersetzer-Notizen** — `<!-- %1$s = Benutzername -->`
- **CLDR-Plural-Vollstaendigkeit pro Sprache** — Russisch braucht `few`/`many`, Arabisch braucht `zero`/`two`
- **HTML/CDATA-Inhalte** — werden bei Uebersetzung oft zerstoert
- **Format-Argument-Semantik** — was bedeutet `%1$s`, was `%2$d`
- **Glossar-Auto-Erkennung** — haeufige Begriffe die konsistent uebersetzt werden muessen
- **Region-Differenzen** — pt-rBR vs pt-rPT, zh-rCN vs zh-rTW
- **Du/Sie-Konsistenz** (Deutsch) — Mischanrede wird geflaggt

→ **Detail-Anleitung**: `references/layer-4c-translation-context.md`

### Schicht 4d — Legal-Text-Inventar (PFLICHT — Grundlage fuer Rechtssicherheits-Skill)

12 rechtlich obligatorische Wortlaut-Bereiche werden vollstaendig erfasst:

- Permission-Rationale-Dialoge (pro Permission Title/Body/Allow/Deny-Verhalten)
- Consent-Banner (Analytics, Tracking, Marketing — gleichrangige Akzeptieren/Ablehnen-Buttons?)
- AGB-, Datenschutz- und Impressums-Links (mit Erreichbarkeit + Sprachvarianten)
- Health-Disclaimer (bei Fitness-/Mental-Health-Apps)
- AI-Disclaimer (EU AI Act + FTC)
- Werbe-Markierungen (UWG §5a — Schleichwerbung)
- Account-Deletion-Flow (DSGVO Art. 17 — Wort "unwiderruflich" Pflicht)
- Newsletter-/Marketing-Opt-In (UWG §7 Double-Opt-In)
- In-App-Kauf-Confirmation (Google Play Subscriptions Policy)
- Widerrufsbelehrung (BGB §312g — sonst Frist 12 Monate)
- Standort-Begruendung (Play Console seit 2024)
- Altersfreigabe-Anzeige (USK/PEGI/IARC)

→ **Detail-Anleitung**: `references/layer-4d-legal-text-inventory.md`

### Schicht 4e — Externe Inhalte (ergaenzend — Audit ueber die App hinaus)

Wortlaute die NICHT im Code-Repository leben aber genauso UWG-/Werberecht-relevant sind:

- Google Play Store Listing (Title, Short/Long Description, Screenshot-Texte) — Frank-Aufgabe oder Fastlane-Metadata
- Firebase Remote Config Defaults + Live-Werte
- Cloud Functions Notification-Templates (Trial-End, Subscription-Status)
- Email-Templates (Firebase Auth, Stripe, Sendgrid)
- WebView-Inhalte (HTML in assets/ oder externe URLs)
- PDF-Export-Vorlagen (oft DE-hardcoded)
- Customer-Support-System (Intercom, Zendesk)
- Marketing-Materialien (Webseite, Promo-Videos, Social-Media-Bios, Newsletter-Archiv)

→ **Detail-Anleitung**: `references/layer-4e-external-content.md`

### Schicht 5 — Paywall-Tiefenanalyse: Der WICHTIGSTE Bereich

Hier wird mit besonderer Sorgfalt gearbeitet, weil Werbeaussagen rund um die Paywall am haeufigsten rechtlich problematisch sind. Jeder einzelne Subscription-State, jeder Abbruchpfad, jeder Trial-Schritt, jeder Promo-Code, jeder Win-Back-Flow wird dokumentiert.

Ergebnis-Tabelle pro Paywall-Bildschirm:
- Trigger (wo wird er ausgeloest)
- Angezeigte Plaene (Monthly, Yearly, Promo, Trial)
- Pflichtangaben (Preis, Laufzeit, Kuendigung, Auto-Renewal) — pro Bildschirm pruefen
- Abbruchpfade und was bei jedem passiert
- Welcher Subscription-State zeigt welche UI

Auch alle 22 Real-Time-Developer-Notification-Typen werden dokumentiert mit ihrem UI-Verhalten.

→ **Detail-Anleitung**: `references/layer-5-paywall.md`
→ **Komplette Subscription-State-Machine**: `references/subscription-state-machine.md`

### Schicht 6 — Hidden Features aufdecken

Background-Jobs (WorkManager-Worker), Widget-Provider, Quick-Tile-Services, App-Shortcuts, Accessibility-Services, Print-Adapter, NFC-Handler, Boot-Receiver, Notification-Channels, Debug-Menus (Long-Click-Trigger), Feature-Flags via Remote-Config, A/B-Test-Varianten, Account-Deletion-Flows (DSGVO-Pflicht).

Diese werden oft in Audits uebersehen. Ohne sie ist das Inventar nicht vollstaendig.

→ **Detail-Anleitung**: `references/layer-6-hidden-features.md`

### Schicht 7 — Werbeaussagen-vs-Feature-Matrix

Jetzt werden die Werbeaussagen aus diesen Quellen zusammengetragen:
- `res/values/strings.xml` (alle User-facing Texte)
- `res/values-*/strings.xml` (alle Sprachen)
- Google Play Store Listing (Long Description, Short Description, Feature-Bullets)
- Onboarding-Texte
- Push-Notification-Templates
- Settings-Texte

Fuer jede Aussage wird gegen das Feature-Inventar geprueft. Output-Tabelle:
| Aussage | Quelle | Code-Realitaet | Luecke | Risiko (UWG/EU/Google) | Fix-Vorschlag |

Risiko-Stufen: KRITISCH / HOCH / MITTEL / NIEDRIG nach UWG §5, EU UCPD, Google Play Policy.

→ **Detail-Anleitung**: `references/layer-7-marketing-claim-audit.md`

## Master-Skript fuer den ersten Scan

Statt alle Greppable-Patterns einzeln auszufuehren, gibt es ein Master-Skript das den ersten Scan automatisiert macht und einen strukturierten Initial-Bericht erzeugt:

```bash
bash ~/.claude/skills/app-roentgen/scripts/feature-scan.sh <pfad-zur-android-app>
```

Das Skript schreibt einen Initial-Bericht ins App-Verzeichnis als `app-roentgen-initial-scan.md`. Dieser ist die Basis fuer die Tiefenanalyse durch Claude in den 7 Schichten.

## JSON-Schnittstelle fuer maschinelle Konsumenten

Parallel zum Markdown-Bericht kann ein strukturierter JSON-Export erzeugt werden, den der Rechtssicherheits-Skill und der Uebersetzungs-Skill konsumieren koennen:

```bash
python3 ~/.claude/skills/app-roentgen/scripts/export-json.py <pfad-zur-android-app>
```

Output: `<app-dir>/app-roentgen-export.json` mit Schema-Version 2.1.

Der Export enthaelt: alle Strings mit Hash + Translatable-Flag + Format-Args + xliff:g-IDs + Slot-Laenge, Plurals mit CLDR-Vollstaendigkeitspruefung pro Sprache, Glossar, Du/Sie-Konsistenz, SDK-Erkennung (AI/Ads/Billing/Health/WebView/Firebase), Permission-Liste.

Der JSON-Export ist optional — er wird nur erzeugt wenn der Konsument ihn explizit braucht. Das Markdown-Format reicht fuer den manuellen Audit.

## Output-Format des finalen Berichts

Der finale Bericht wird in der App-Wurzel als `app-roentgen-AUDIT-YYYY-MM-DD.md` gespeichert. Die Struktur folgt dem Template `assets/audit-report-template.md`. Pflichtbestandteile:

1. **Zusammenfassung** (3-4 Saetze fuer Frank in einfachem Deutsch)
2. **Schicht 1-7 Detail-Berichte** mit allen extrahierten Daten
3. **Komplette Bildschirm-Karte** (Mermaid + Baum)
4. **Paywall-Bildschirm-Inventar** (sehr detailliert, eigener Hauptabschnitt)
5. **15-Kategorien-Feature-Inventar** (siehe `references/layer-7-marketing-claim-audit.md`)
6. **Werbeaussage-vs-Feature-Matrix** (sortiert nach Risiko)
7. **Don't-Miss-Checkliste** mit Haken pro Punkt (siehe `references/dont-miss-checklist.md`)
8. **Empfohlene naechste Schritte** (was muss vor Release gefixt werden)

## Bekannte Limitierungen (Known Limitations)

> **FIX V6 + W5 (Audit 5+6):** Folgende Punkte sind dokumentierte Trade-Offs, keine Bugs:
>
> - **`<integer-array>` und `<bool>`/`<color>` in strings.xml werden NICHT exportiert** (FIX W5): Diese Resource-Typen enthalten keine UI-Texte und sind fuer Werbeaussagen-/Rechtsaudit irrelevant. `<string>`, `<plurals>` und `<string-array>` werden vollstaendig erfasst.
>
> - **audit-report-template.md ist ~1200 Zeilen** (Section-Skeleton mit allen 7 Schichten + 4 Sub-Layern + Checkliste). Beim Schreiben des finalen Berichts laedt Claude die komplette Datei in den Kontext. Bei sehr grossen Apps (>3000 Kotlin-Dateien) plus den Reference-Dateien kann Context-Druck entstehen. Loesung: Bericht inkrementell pro Schicht schreiben statt komplett auf einmal.
> - **17 Reference-Dateien** sind absichtlich granular (Progressive Disclosure) — Claude laedt nur was sie braucht. Wenn eine Schicht uebersprungen wird, bleibt die zugehoerige Reference ungeladen.
> - **`feature-scan.sh` ist ein Shell-Skript** — auf Windows nur in Git Bash, nicht PowerShell. JSON-Export per Python ist plattformunabhaengig.

## Checkpoint-Mechanik fuer lange Audits (KRITISCH bei grossen Apps)

Audits einer mittelgrossen App (500-1500 Kotlin-Dateien) dauern 30-90 Minuten. Vor JEDER neuen Schicht (1, 2, 3, 3b optional, 4, 4b-4e, 5, 6, 7) MUSS eine Checkpoint-Datei `<app-dir>/app-roentgen-checkpoint.json` geschrieben werden. Bei Wiederaufnahme: Checkpoint lesen, bei `current_phase` fortsetzen, erledigte Phasen NICHT erneut bearbeiten.

→ **Komplettes JSON-Schema, Phasen-Liste, Gitignore-Hinweise**: `references/checkpoint-format.md`

## Vollstaendigkeits-Validierung (PFLICHT)

Bevor der Bericht als fertig gilt, MUSS die `dont-miss-checklist.md` durchgegangen werden — alle 92 Punkte muessen geprueft sein. Jeder Punkt der nicht geprueft werden konnte wird mit "NICHT_VERIFIZIERT — Grund" markiert. Das ist die letzte Verteidigungslinie gegen unvollstaendige Audits.

→ **Liste**: `references/dont-miss-checklist.md`

## Verhalten bei Unklarheit

Wenn ein Code-Pfad mehrdeutig ist und der Skill nicht eindeutig sagen kann was die App macht, MUSS das im Bericht als "UNKLAR — bitte manuell pruefen" markiert werden. Lieber ehrlich Luecken zugeben als fadenscheinige Vermutungen abgeben — der Audit ist die Basis fuer Rechtssicherheit, da darf nichts verfaelscht sein.

## Greppable-Patterns als Werkzeugkasten

Alle systematischen Suchmuster sind in `references/greppable-patterns.md` gesammelt. Diese Datei ist die taktische Referenz — sie wird vom Master-Skript genutzt und kann auch manuell konsultiert werden wenn ein bestimmter Bereich noch tiefer untersucht werden soll.

→ **Patterns**: `references/greppable-patterns.md`

## Beispiel-Anwendung: BestJournalAndroid

Der typische erste Anwendungsfall fuer Frank ist `~/proggs/BestJournalAndroid/`. Beim ersten Lauf werden die Ergebnisse in `~/proggs/BestJournalAndroid/app-roentgen-AUDIT-YYYY-MM-DD.md` geschrieben. Das ist die Vorlage fuer den nachfolgenden Werbeaussagen-Audit (Memory `project_bestjournal_paywall_marketing_audit.md`).

## Schreibstil im Bericht

Da der Bericht spaeter als juristische Grundlage dienen kann, gilt:
- **Praezise**: Keine Vermutungen ohne Beleg. Jede Aussage mit Datei + Zeilennummer belegen.
- **Vollstaendig**: Lieber zu viele Details als zu wenige.
- **Strukturiert**: Tabellen wo immer moeglich, damit der Leser scannen kann.
- **Ehrlich**: Wenn etwas unklar ist, das ausdruecklich vermerken.
- **Auf Deutsch fuer die Zusammenfassungs-Bloecke**, technische Bezeichner und Code-Snippets bleiben in Originalsprache.

## Zusammenspiel mit anderen Skills

| Skill | Zusammenspiel |
|-------|--------------|
| `rechtssicherheit` | Liest Schicht 4b (Wortlaute) + Schicht 7 (Werbeaussagen-Matrix) und prueft gegen die Wissensbasis in `~/proggs/rechtssicherheit.md` |
| `uebersetzung` | Liest Schicht 4b (Original-Wortlaute) + Schicht 4c (Translation-Context: Slot, Laenge, Plurals, Glossar, Argumente) als Uebersetzungs-Grundlage |
| `string-extraktor` | Komplementaer zu Schicht 4c — der Extraktor findet hardcoded Strings, der Roentgen-Skill katalogisiert sie inklusive Slot-Zuordnung |
| `app-monetizer` | Konsumiert die Paywall-Tiefenanalyse (Schicht 5) als Input |
| `superintelligenz` | Bei sehr grossen Apps (>500 Kotlin-Dateien) parallele Researcher fuer einzelne Schichten spawnen |

## Was NIEMALS passieren darf

- ❌ Audit als "fertig" markieren ohne die Don't-Miss-Checkliste durchgegangen zu sein
- ❌ Werbeaussagen aus nur einer Sprache pruefen — IMMER alle uebersetzten strings.xml einbeziehen
- ❌ Paywall-Bildschirme oberflaechlich abhaken — jeder Bildschirm bekommt eine eigene Sub-Tabelle
- ❌ Vermutungen ohne Code-Beleg im Bericht stehen lassen
- ❌ Versteckte Features (Feature-Flags, Debug-Menus, Background-Jobs) wegen Aufwand auslassen
- ❌ Bei Unklarheit raten — stattdessen "UNKLAR" markieren und dem Benutzer melden
- ❌ Wortlaute zusammenfassen, paraphrasieren oder kuerzen — IMMER 1:1 in `"..."` zitieren
- ❌ Nur den String-Key nennen ohne den Wortlaut auszuschreiben
- ❌ Menue-Tiefen abkuerzen mit "und weitere Untermenues" — JEDE Ebene komplett ausrollen, egal wie tief
- ❌ Settings/Preferences als Sammelblock abhandeln — jedes Item bekommt eine eigene Zeile mit Label + Beschreibung + ggf. Switch-/Dropdown-Werten
- ❌ Dialog-Texte nur teilweise zitieren (z.B. nur Title) — IMMER Title + Body + alle Buttons komplett
- ❌ Plurals, Format-Strings (`%s`, `%d`) und Array-Resources ignorieren
