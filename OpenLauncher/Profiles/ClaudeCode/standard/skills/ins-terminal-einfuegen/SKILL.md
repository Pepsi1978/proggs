---
name: ins-terminal-einfuegen
description: Trägt auf Nutzerauftrag Text in eine bereits geöffnete Terminal-Sitzung der Codex-Desktop-App unter Windows ein und sendet ihn bei beauftragtem Absenden mit Enter. Für laufende Claude-Code-, OpenCode- oder Shell-Eingaben; kein Start neuer Sitzungen und keine dauerhafte Überwachung.
---

# Ins Terminal einfügen

Nutze den vorhandenen Terminal-Tab des Nutzers. Ein frisch gestarteter Shell-Prozess ist kein Ersatz für dessen laufende Sitzung. Eine Bitte um Einfügen allein bedeutet nicht automatisch Absenden; „abschicken“, „absenden“ oder „mit Enter ausführen“ schließt Enter ein. Nutze den zuletzt eindeutig festgelegten Text und sende ihn genau einmal.

## Zugang

- Lade den verfügbaren Skill `computer-use:computer-use` und seine erforderlichen Referenzen einmal pro Gespräch. Seine Installation über den aktuellen Skillkatalog finden, keine Plugin-Versionsnummer fest einbauen. Die dortigen Vorgaben und die geltende Anweisungshierarchie beachten; dieser Skill erteilt keine zusätzlichen Berechtigungen.
- Windows-Steuerung: `mcp__node_repl__js` mit `@oai/sky`. Das ist ein anderer Zugang als die browserorientierte `mcp__cua_repl`-Sitzung. Ein dort deaktivierter nativer Zugang beweist nicht, dass `@oai/sky` fehlt.
- Den vorhandenen Node-Zustand weiterverwenden. Nur bei fehlendem `globalThis.sky` initialisieren:

```js
if (!globalThis.sky) {
  const { sky } = await import('@oai/sky');
  globalThis.sky = sky;
}
```

- Einmal `sky.list_windows()` aufrufen und genau das zurückgegebene Ziel auswählen; `sky.get_window({id, app})` liefert das Fensterobjekt. Bei mehreren passenden Fenstern den gewünschten Chat/Terminaltitel anhand des Zustands unterscheiden. IDs niemals raten oder dauerhaft speichern.
- `mcp__codex_app__read_thread_terminal` liest die Ausgabe des eingebauten Terminals dieser Aufgabe. `write_stdin` ist ausschließlich für die von den Ausführungswerkzeugen verwalteten Sitzungen geeignet; keine fremde Sitzungs-ID erfinden.

## Kurzer Ablauf bei bekannter Sitzung

### Schnell beginnen

- Bei Wiederholungsaufträgen direkt den vorhandenen `node_repl`-Zugang nutzen. Keine erneute Skill-/Dokumentationslektüre, Toolsuche, Paketinitialisierung oder `list_windows`-Abfrage, solange diese Sitzung die erforderlichen Informationen bereits enthält und das bekannte Fenster weiterhin gültig ist.
- Das bestätigte Fenster während der Node-Sitzung unter `globalThis.terminalInsertWindow` aufbewahren; die letzte Beobachtung unter `globalThis.terminalInsertState`. Diese Werte sind flüchtiger Arbeitszustand und werden nicht als dauerhafte Fenster-IDs gespeichert.
- Der erste Werkzeugaufruf bei einem vorbereiteten Wiederholungsauftrag ist unmittelbar die aktuelle Beobachtung:

```js
globalThis.terminalInsertState = await sky.get_window_state({
  window: terminalInsertWindow,
  include_screenshot: true,
  include_text: true,
});
globalThis.terminalInsertWindow = terminalInsertState.window;
nodeRepl.write(String(terminalInsertState.accessibility?.focused_element || ''));
```

- Bei unveränderter, sichtbar fokussierter Eingabe direkt mit Texteingabe und Sichtprüfung fortfahren. Je Aktion die folgende Zustandsprüfung in dieselbe Node-Zelle aufnehmen; Beobachtung und die darauf basierende nächste Aktion bleiben getrennt. Den Terminalpuffer erst zur Ergebnisprüfung abrufen, wenn der erste Screenshot Programm und Eingabebereitschaft bereits eindeutig zeigt.
- Bei fehlendem Node-Zustand einmal regulär initialisieren. Die Kaltstartzeit und die blaue Aktivitätsanzeige des Computer-Use-Runtimes lassen sich durch diesen Skill nicht abschalten. Keine Zusage fester Reaktionszeiten und keine Umgehung des sichtbaren Computer-Use-Modus.

### Eingeben und prüfen

1. **Aktuell beobachten.** `sky.get_window_state` für das bekannte Fenster mit Screenshot und bei Bedarf Accessibility-Text. Prüfen, ob Claude/OpenCode oder eine Shell auf Eingabe wartet, ob bereits Text vorhanden ist und ob wirklich das beauftragte Terminal sichtbar ist. Fremden Entwurf nicht überschreiben. Einen natürlichsprachlichen Agentenauftrag nicht als Shell-Befehl senden. Bei einer offenen Freigabe oder Rückfrage nicht blind Enter drücken.
2. **Fokus nur bei Bedarf setzen.** Ein bereits sichtbar fokussiertes Terminal mit Cursor benötigt keinen weiteren Klick. Sonst in die beobachtete Eingabezeile klicken, anschließend Zustand erneut abrufen. Vorhandene aktuelle Screenshot-ID verwenden; keine festen Bildschirmkoordinaten im Skill hinterlegen.
3. **Text eingeben.** In einer eigenen Aktion `sky.type_text({window, text})`; direkt danach Zustand erneut abrufen. Den Text im richtigen Eingabefeld visuell prüfen. `type_text` enthält keine abschließende neue Zeile; Enter ist eine separate Aktion.
4. **Beauftragtes Absenden.** Erst nach sichtbarer Prüfung einmal `sky.press_key({window, key: 'Return'})`; danach Zustand aktualisieren. Bei reinem Einfügeauftrag diesen Schritt auslassen.
5. **Ergebnis prüfen.** Den Terminalpuffer abrufen oder die neue Anzeige prüfen: Der Text muss als gesendete Nachricht bzw. ausgeführter Befehl erkennbar sein. „Abgeschickt“ erst dann melden. „Antwort erhalten“ erst nach tatsächlicher Antwort; eine laufende Verarbeitung genügt als Bestätigung des Absendeauftrags.

Keine zusätzlichen Testnachrichten, erneute Paketinstallation oder allgemeine Systemsuche bei einer bereits funktionierenden Verbindung. Nur bei Zustand-/Fokusänderung erneut das Fenster bestimmen. Erforderliche Sichtprüfungen bleiben auch beim schnellen Wiederholungsablauf erhalten.

## Beobachtete Besonderheit des eingebauten Terminals

Bei zwei erfolgreichen manuellen Ablaufprüfungen am 14.09.2026 erkannte die Accessibility-Schnittstelle `Terminal input`, meldete den Fokus aber nur als Dokument. Ein Klick über dessen Elementindex scheiterte mit `coordinate input geometry is unavailable`. Die funktionierende Alternative war ein Klick in die sichtbare Eingabezeile anhand des frischen Screenshots. Der folgende Screenshot bestätigte den eingegebenen Text am Terminal-Cursor; danach funktionierte Enter. Bei der zweiten Nachricht war kein erneuter Klick nötig.

Das sind Beobachtungen dieser Version, keine universellen Garantien. Nach fehlgeschlagenem Klick zuerst neu beobachten. Nach unklarem Schreib- oder Absendeergebnis niemals blind wiederholen: Screenshot und Terminalausgabe auf bereits eingefügten oder gesendeten Text prüfen. Bleibt Ziel oder Ergebnis unklar, anhalten und die konkrete Unsicherheit nennen.
