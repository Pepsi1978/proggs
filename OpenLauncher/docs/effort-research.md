# Effort-Aktualisierung und Codex-Recherche

Stand: 05.09.2026, 11:26 Uhr. Windows 1.24.24, macOS 1.24.17.

## Bedienung

Das Zahnrad neben Aktualisieren oeffnet die Recherche-Einstellungen. Mit OpenAI
anmelden zeigt einen kurzlebigen Geraetecode und die offizielle Anmeldeseite.
Anschliessend Kontomodell und Recherche-Effort waehlen und speichern.
Die eigene Anmeldung veraendert keine vorhandene Codex-CLI-Anmeldung.

Modi: Aus, automatisch bei Luecken/Widerspruechen, nur manuell oder regelmaessig.
Regelmaessig betrifft in dieser Launcher-Sitzung benutzte Modelle bei geoeffnetem
Launcher. KI wird auch dabei nur gebraucht, wenn Katalogquellen nicht ausreichen.
Die manuelle Recherche bezieht sich auf das im Launcher ausgewaehlte Modell und
dessen Zugangsweg. KI-Anfragen koennen das Kontingent des verbundenen Kontos belasten.

## Fehlerursache und Abwehr

Nach dem ersten Cache-Fix meldeten die Windows-Logs weiterhin Netzwerkabbrueche.
HTTP-Kompression allein loeste diese Abbrueche nicht. Vorher gab es nur einen
.NET-Netzwerkweg; Modelle waren ausserdem nicht nach CLI-Zugangsweg getrennt.
Der lokale Codex-Kontokatalog nennt fuer Astra auch ultra, der zuvor geladene
models.dev-Eintrag nicht. Ein gemeinsamer Cache kann daher falsche Stufen zeigen.

Oeffentliche Kataloge haben jetzt einen begrenzten .NET-/URLSession-Abruf und einen
unabhaengigen nativen curl-HTTPS-Fallback. Nur zwei fest erlaubte URLs, keine
Authentifizierung und keine Shell-Ausfuehrung. Modellwechsel bricht nur den
wartenden Aufruf ab, nicht den gemeinsam verwendeten Download. Cache und
Fehlerabkuehlung verhindern wiederholte Last bei schnellen Modellwechseln unter Windows.

Quellen: models.dev, OpenRouter-Parameter fuer OpenRouter, authentifizierter
Codex-Kontokatalog fuer den Codex-Zugangsweg, lokaler Codex-Katalog als datierter
Fallback und zuletzt Codex-Webrecherche. Quellenfehler werden unabhaengig behandelt.
Unbekannt ist niemals gleichbedeutend mit einer bestaetigten leeren Stufenliste.

## Sicherheitsgrenzen

- Persistente Efforts getrennt nach Modell, Provider und Zugangsweg.
- Nur explizite aktuelle Katalogaufzaehlungen duerfen Stufen entfernen.
- Abgeleitete OpenRouter-Parameter und KI-Ergebnisse sind nur additiv.
- KI besitzt ausschliesslich web_search, keinerlei Datei-/Shell-Werkzeuge.
- Zielkennung, Zugangsweg, erlaubte Stufen, echte Suchereignisse und Zitate validieren.
- Offizielle HTTPS-Quellen werden erneut geladen und Zitate abgeglichen.
- Kein belegbares Ergebnis bedeutet keine Aenderung. Fehler bleiben im Bericht sichtbar.
- Windows-Anmeldung: DPAPI CurrentUser unter LocalAppData; macOS: eigene Keychain.
- Keine Zugangsdaten oder Geraetecodes im Repository oder normalen Logs.
- Hintergrund- und Rechercheaufgaben werden beim Schliessen abgebrochen.

## Funktionsumfang und Grenzen

Die bisherige sofortige Auswahl, Profilstandards und normale Launcher-Startwege
bleiben erhalten. Auswahlen waehrend laufender Updates werden beim Anwenden erneut
gelesen. KI-Recherche kann alte Optionen nicht durch eine unvollstaendige Antwort
loeschen. Berichte enthalten Quelle, letzten Erfolg und Fehlermeldungen.

Die Codex-Backend-API ist kein stabil garantierter oeffentlicher API-Vertrag. Login,
Kontozugriff und Websuche benoetigen die persoenliche Anmeldung; ohne sie bleiben
Katalogaktualisierungen aktiv. Backend-Ablehnungen liefern keinen erfundenen Erfolg.
macOS hat weiterhin keinen neuen Codex-CLI-Startweg; die vorhandenen CLI-Wege bleiben.

Keine automatischen Funktions-/UI-Tests im Schnellmodus. Windows-Build und Start
ueber update-launcher.ps1. macOS kann auf diesem Windows-Rechner nicht gebaut oder
installiert werden. Die persoenliche Anmeldung und authentifizierte Webrecherche
sind ohne Benutzeranmeldung nicht end-to-end ausgefuehrt.
