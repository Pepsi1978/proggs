# UpdateZentrale: `agy update` bricht seinen Download über IPv6 ab (06.11.2026)

> Behoben in UpdateZentrale 1.3.78 und Updater-Zentrale macOS 1.0.47 (macOS ungebaut, ungetestet), nach dem Code-Review geschärft in 1.3.79 / 1.0.48.
> Der Ersatzweg ist am echten Fall bestätigt: 1.5.2 per `curl -4` geladen, SHA-512 passte zum Manifest, `agy --version` danach 1.5.2.

## Symptom

Im Sammellauf lief `agy.exe update` (Antigravity CLI 1.5.1 → 1.5.2) drei Minuten und endete mit Code 1:

```
Download interrupted: read tcp [2a02:…]:63722->[2600:1901:4023:1b00::]:443: wsarecv: Ein Verbindungsversuch ist fehlgeschlagen, da die Gegenstelle nach einer bestimmten Zeitspanne nicht richtig reagiert hat …
Error: download interrupted
```

Schon am 28.10.2026 scheiterte derselbe Download mit `context deadline exceeded (Client.Timeout or context cancellation while reading body)`, danach dreimal `update already in progress`.

## Ursache

- `agy` ist ein Go-Programm und lädt seine rund 220 MB große Programmdatei selbst. Go wählt die Route per Happy Eyeballs und nimmt IPv6, wenn der Handshake klappt. In diesem Netz bricht der Datenstrom über IPv6 danach ab (dieselbe Netzlage wie in `bugs/desktop/dotnet-httpclient-ipv6-haenger.md`, nur im Fremdprozess, den die Zentrale nicht umstellen kann).
- `CliAktualisierer.IstNetzfehler` erkannte den Fehler nicht: `NetzHinweise` enthielt nur englische Wörter, die Winsock-Meldung kommt aber auf Deutsch, und „download interrupted“ fehlte. Es gab deshalb nur einen Aufruf und keine Wiederholung.
- Eine Wiederholung allein hätte wenig geholfen: jeder Versuch nimmt wieder dieselbe IPv6-Route und kostet Minuten.

## Behebung

- Neues Katalogfeld `ersatzDownload` (Windows `ProgrammEintrag.ErsatzDownload`, macOS `ersatzDownload`): `versionsUrl` ist dann ein JSON-Manifest mit `url` und `sha512`/`sha256`. Scheitert das eigene Update des Werkzeugs (Code ≠ 0 oder Zeitlimit, nicht bei Abbruch und nicht bei `already in progress`), lädt die Zentrale die Datei selbst mit `curl -4 --retry 2`, prüft die Prüfsumme gegen das Manifest, benennt die alte Programmdatei in `<exe>.alt` um (Windows erlaubt das auch bei laufender Sitzung), legt die neue an ihren Platz und prüft mit `--version`. Startet die neue nicht, kommt die alte zurück.
- Ohne Prüfsumme im Manifest wird nichts eingesetzt. Windows nimmt nur eine fertige Programmdatei mit derselben Endung; macOS packt `.tar.gz` aus (das darwin-Manifest liefert `cli_mac_arm64.tar.gz`) und nimmt die Datei gleichen Namens.
- Mit `ersatzDownload` wiederholt die Zentrale den eigenen Download des Werkzeugs nicht mehr, sondern geht sofort auf den IPv4-Weg.
- `NetzHinweise` ergänzt um `download interrupted`, `wsarecv`, `wsasend`, `while reading body`, `verbindung` — für alle CLI-Einträge ohne Manifest greift damit wenigstens die Wiederholung.
- `programs.json` (Windows und macOS): `antigravity-cli` trägt `"ersatzDownload": true`.

## Schärfung nach dem Code-Review (1.3.79 / 1.0.48)

- Der Ersatzweg greift nur bei Download-Abbruch (`download interrupted`, `while reading body`, `wsarecv`, `deadline exceeded`, `connection reset`), eigener Download-Frist oder Zeitlimit, nie bei anderen Fehlern, nie nach Abbruch durch den Nutzer und nie, wenn sich der alte Prozessbaum nicht beenden ließ (`BeendenProblem`).
- Das Manifest muss `version` nennen, und die muss echt neuer sein als die installierte. Ein veraltetes CDN-Manifest kann so nie ein Downgrade einsetzen.
- Kurze Netzfehler beim Abruf der Release-Liste werden weiter dreimal wiederholt; nur ein abgerissener Download geht sofort auf den IPv4-Weg.
- `verbindung` war als Netzhinweis zu breit, jetzt nur `verbindungsversuch` (Winsock-Text).
- Reste `<exe>.alt*` früherer Tauschvorgänge räumt jeder neue Lauf weg.
- macOS: Manifest auch per `curl -4` (nicht URLSession), Symlinks werden aufgelöst und ihr Ziel ersetzt, Prüfsumme per CryptoKit, Abbruch-Marke wird geachtet.

## Erkennen

Im Tagesprotokoll steht bei einer CLI `Download interrupted`, `wsarecv`, `while reading body` oder eine IPv6-Adresse in eckigen Klammern. Ab 1.3.78 folgt `Eigener Download gescheitert – die UpdateZentrale lädt die neue Version selbst (IPv4) …`. Andere CLIs mit Manifest (url + Prüfsumme) bekommen den Weg mit `"ersatzDownload": true` im Katalog, ohne Neubau.

## Dateien

- `UpdateZentrale/Providers/CliAktualisierer.cs` (`ErsatzDownloadAsync`, `ManifestAuswerten`, `NetzHinweise`)
- `UpdateZentrale/Models/ProgrammEintrag.cs`, `UpdateZentrale/programs.json`
- `UpdateZentrale/Providers/WingetAktualisierer.cs` (`CurlLadenAsync` jetzt `internal`, gemeinsam genutzt)
- `UpdaterZentrale-macOS/UpdaterZentrale/Providers/CliAktualisierer.swift`, `Models/Models.swift`, `programs.json`
