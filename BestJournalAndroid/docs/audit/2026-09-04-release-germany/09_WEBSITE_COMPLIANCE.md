# 09 – Internetseite

**Prüfgegenstand:** Best Journal Android (`com.bestjournal.app`), 0.21.17 (298) · **Projektpfad:** `C:\Users\barwa\proggs\BestJournalAndroid`
**Prüfstand:** Commit `01b200085a7067123e5e21aba8021bc9193033a6`, `main` · **Prüfdatum:** 04.09.2026 · **Modell:** Claude Fable 5.1
**Beweisklassen:** [T] technisch bestätigt · [D] dokumentierte Anbieteraussage · [R] rechtliche Bewertung · [A] plausible Annahme · [U] ungeklärte externe Tatsache

---

## 1. Ist-Zustand [T]
- Verlinkte URLs (`strings.xml:1475-1486`): `https://pepsi1978.github.io/proggs/bestjournal/` + `privacy-de|en|ko.html`, `agb-de|en|ko.html`, `imprint-de|en|ko.html`, `account-deletion.html`. Alle sechs geprüften URLs: **HTTP 404** (curl 04.09.2026 17:11, TLS in Ordnung). Der Auftraggeber bestätigt: GitHub-Pages-Seite ist tot; Umzug auf Hostinger geplant.
- Quelldateien: `proggs/docs/bestjournal/*.html` (11 Dateien; statisch, ohne JavaScript, ohne externe Skripte oder Web-Schriften laut Text-Extraktion; genaue Ressourcenprüfung erst nach Veröffentlichung möglich).
- Hosting-Angabe im Auftrag („Google Sites“) trifft nicht zu.

## 2. Anforderungen an die neue Seite (Hostinger) [R]/[D]
| Punkt | Anforderung | Umsetzung |
|---|---|---|
| HTTPS | Pflicht (Play, DSGVO Art. 32) | Hostinger-SSL aktivieren, HTTP→HTTPS-Weiterleitung |
| Erreichbarkeit | öffentlich, ohne Login, nicht geogesperrt, kein PDF, nicht editierbar durch Dritte | statische HTML-Dateien |
| Stabile URLs | keine brüchigen Weiterleitungen; URLs stehen in der App und in der Play Console | z. B. `https://<domain>/bestjournal/datenschutz.html`, `/agb.html`, `/impressum.html`, `/konto-loeschen.html`, EN-Varianten |
| Datenschutzerklärung der Website | eigener Abschnitt oder eigene Seite: Hoster (Hostinger International Ltd., Zypern; Serverstandort laut Vertrag [U]), Serverlogfiles (IP, Zeit, URL; Speicherdauer laut Hoster [U]), Rechtsgrundlage Art. 6 Abs. 1 lit. f; keine Cookies/Analytics (statisch) | in `privacy-de.html` Abschnitt „Diese Website“ ergänzen |
| Impressum | § 5 DDG, § 36 VSBG (vorhanden) | `imprint-de.html` |
| Kontolöschung | Anleitung in-App + E-Mail-Weg (vorhanden in `account-deletion.html`, Play-Anforderung nur bei In-App-Kontoerstellung) | beibehalten |
| Externe Ressourcen | keine Google Fonts/CDNs/Skripte einbinden (sonst Drittlandübermittlung ohne Einwilligung) | Quelltext nach Veröffentlichung prüfen (`curl` + grep `<script|<link`) |
| Cookies/Consent-Management | nicht nötig, solange keine Cookies/Tracker | – |
| Mobile Darstellung | Viewport-Meta vorhanden [U] | prüfen |
| Übereinstimmung mit App | gleiche Quelldateien wie Assets (Hash-Vergleich) | Build-Schritt/Skript zum Synchronisieren |

## 3. Bewertung
**NICHT BESTANDEN** (WEB-001), da keine erreichbare Datenschutz-URL existiert. Nach Umzug: erneut prüfen (HTTP 200, Quelltext ohne Fremdressourcen, Website-Datenschutzabschnitt, mobile Darstellung) und Nachweis in `evidence/website-check.txt` ablegen.
