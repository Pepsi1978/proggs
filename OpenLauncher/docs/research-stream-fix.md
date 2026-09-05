# Echte Webrecherche: SSE-Auswertung und Provider-Refresh

Stand: 05.09.2026, 11:53 Uhr. Windows 1.24.25, macOS 1.24.18.

## Ursachen

1. Luna/Max erledigte echte Websuchen, wurde aber vom pauschalen Drei-Minuten-Limit
   vor der Schlussantwort abgebrochen. Der sehr offene Auftrag verlangte aufwendige
   wortgetreue Zitate auch dann, wenn ein exakter aktueller Modellkatalog vorlag.
2. Der Codex-Stream lieferte Suchabschluss und Antwortinhalt in eigenen Ereignissen.
   Das letzte response.completed-Paket enthielt diese Ausgaben nicht nochmals.
   Allein dessen output-Liste auszuwerten verwarf echte Sucherfolge und Antworten.
3. Dem WPF-ComboBox-Template fehlte ItemTemplateSelector fuer die Auswahlbox.
   Daher erschienen Objekt-Darstellungen statt DisplayMemberPath-Beschriftungen.
4. Provider-Aktualisieren setzte nur den Katalog-Refresh. Bei vorhandenem Katalog
   wurde der KI-Fallback gar nicht aufgerufen, insbesondere im manuellen Modus.

## Fix und Grenzen

- Kurzer gezielter Suchauftrag, sechs Minuten Gesamtbudget, Benutzer-Effort bleibt.
- Independenter exakter Modellkatalog als Vergleichsbeleg im Auftrag. Ein echter
  abgeschlossener Suchlauf, offizielle Quellen und exakte Stufengleichheit mit dem
  Katalog sind Pflicht. Ohne Katalog bleiben nachgepruefte Quellenzitate Pflicht.
- response.web_search_call.completed sowie response.output_item.done auswerten,
  mit finalen Ausgaben anhand Item-ID deduplizieren; Commentary nicht als JSON lesen.
- Unterschiedliche Meldungen fuer Timeout, fehlenden Inhalt und fehlende Belege.
- Rechercheberichte persistent; aktueller KI-Bericht vor allgemeinen Katalogmeldungen.
- Provider-Refresh fordert explizit Webrecherche an, auch im Modus Nur manuell.
  Ausgeschaltete KI bleibt ausgeschaltet; Katalogdaten bleiben bei KI-Fehlern erhalten.
- WPF-Template zeigt Label bzw. Modell-ID statt Objekt-Darstellung.
- Community-/Forum-Subdomains gelten nicht als offizielle Belegquellen.
- KI kann weiterhin keine vorhandenen Stufen loeschen. Nur bestaetigte aktuelle
  Katalogaufzaehlungen duerfen das. Keine Shell-/Dateitools fuer den Agenten.

## Live-Diagnose

Mit der vorhandenen Launcher-Anmeldung und den unveraenderten Einstellungen
Recherchemodell gpt-5.6-luna, Effort max, Modus Nur manuell:

- Urspruenglicher Lauf: echte Suchereignisse, dann Timeout nach drei Minuten.
- Nach Auftragseingrenzung: Abschluss nach rund 25 Sekunden; Fehler durch alleinige
  Auswertung des finalen Pakets lokalisiert.
- Nach SSE-Fix: echte Webrecherche erfolgreich fuer openai/gpt-6-astra [opencode].
- Separater Aufruf des Produktionsdienstes EffortRefreshService.RefreshAsync mit
  force=true: ebenfalls erfolgreich mit Webrecherche, nicht nur Katalog-Fallback.
- Ergebnis beider erfolgreicher Laeufe: low, medium, high, xhigh, max.
- Belege unter anderem https://developers.openai.com/api/docs/models/gpt-6-astra
  und https://openai.com/index/gpt-6-astra/ sowie exakter models.dev-Abgleich.

Aufrufe erfolgten ueber einen temporaeren Diagnose-Runner ausserhalb des Repositorys,
der die Produktionsdienste referenziert. Keine Zugangsdaten ausgegeben oder geaendert.
Kein macOS-Build auf diesem Windows-Rechner moeglich. Die WPF-Korrektur wird mitgebaut;
die Sichtkontrolle im Fenster bleibt beim Benutzer.
