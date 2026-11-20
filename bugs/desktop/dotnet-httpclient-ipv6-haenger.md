# Netzabfragen hängen 20–30 s: IPv6-Adresse ohne Verbindung (.NET HttpClient, node, winget)

**Stand:** 08.10.2026 · Windows 11 · .NET 8 · betrifft UpdateZentrale, grundsätzlich jedes Programm mit HttpClient

## Symptom
- UpdateZentrale: Claude Code CLI, Codex CLI und Kimi CLI zeigen „Registry-Abfrage fehlgeschlagen:
  The request was canceled due to the configured HttpClient.Timeout of 30 seconds elapsing“.
- `claude update` meldet „Unable to fetch latest version from npm registry“, beim zweiten Versuch klappt es.
- `winget list --id X --exact` braucht 45–95 s pro Programm.
- PowerShell 7 `Invoke-WebRequest` läuft ins Zeitlimit, `curl.exe` antwortet in 1,5 s.
- Sieht nach fehlenden Administratorrechten aus, ist es aber nicht: Prüfen braucht keine Rechte.

## Ursache
Das Netz verteilt IPv6-Adressen, leitet IPv6 aber nicht weiter. Gemessen:

| Aufruf | Ergebnis |
|---|---|
| `curl.exe -4 https://registry.npmjs.org/...` | 0,5 s |
| `curl.exe -6 ...` | keine Verbindung (Abbruch nach 8 s) |
| `curl.exe ...` (probiert beide gleichzeitig) | 1,5 s |
| .NET `HttpClient`, pwsh `Invoke-WebRequest` | Zeitlimit |

.NET (`SocketsHttpHandler`) geht die DNS-Adressen der Reihe nach durch, IPv6 zuerst, ohne Frist
pro Adresse. Der erste Versuch frisst das ganze Zeitlimit; IPv4 kommt nie an die Reihe. node verhält
sich ähnlich. winget fragt ohne `--source` zusätzlich die Store-Quelle live ab und hängt dort.

## Lösung
1. **HttpClient:** eigenen `ConnectCallback` setzen. Siehe `UpdateZentrale/Services/Netzdienst.cs`.
   Dazu ein Wiederholversuch bei Transportfehlern.
   - **Korrektur 20.11.2026 (v1.3.90):** Die erste Fassung probierte die Adressen *nacheinander* mit je 4 s
     Frist. Das ist selbst eine Falle: Schon 4 tote Adressen × 4 s überschreiten das Zeitlimit von 15 s, und
     keine gute Adresse kommt an die Reihe. Richtig sind Adressen aus drei Abfragen (ohne Familie,
     nur IPv4, nur IPv6), ein **gestaffelt-paralleler** Verbindungsaufbau (alle 250 ms die nächste Adresse,
     die erste Verbindung gewinnt) und DNS-over-HTTPS (1.1.1.1) als Ausweichquelle, wenn keine Adresse
     antwortet. Auslöser war ein filternder WLAN-DNS mit Sperr-Adressen:
     `bugs/bug-cases/2026-11-20-updatezentrale-deno-dns-sperradresse.md`.
2. **node-Kindprozesse (npm-Shims `*.cmd`):** `NODE_OPTIONS` um `--dns-result-order=ipv4first`
   ergänzen (anhängen, nicht ersetzen). Siehe `Kommandozeile.AusfuehrenKernAsync`.
3. **Fremde Werkzeuge, die selbst nachladen** (`claude update`, `kimi update`): bei Netzfehler im
   Ausgabetext bis zu zweimal wiederholen (`CliAktualisierer.IstNetzfehler`).
4. **winget:** immer `--source winget` mitgeben (46 s → 1 s). Rückfall auf die Abfrage über alle
   Quellen nur, wenn das Programm laut Dateisystem installiert ist, die Quelle es aber nicht kennt.

## Erkennen
Scheitern mehrere, voneinander unabhängige Netzabfragen mit **genau** dem eingestellten Zeitlimit,
während der Browser normal lädt: zuerst `curl.exe -4` gegen `curl.exe -6` messen.

## Nicht tun
IPv6 am Adapter oder in der Registry abschalten, um ein Programm zu reparieren – das ist eine
Systemänderung mit Nebenwirkungen. Das Programm muss mit einem halben IPv6 umgehen können.

## Fundstellen
- `UpdateZentrale/Services/Netzdienst.cs`, `Providers/CliAktualisierer.cs`,
  `Providers/WingetAktualisierer.cs`, `Services/Kommandozeile.cs` (v1.3.17)
- Protokoll des Vorfalls: `%LOCALAPPDATA%\UpdateZentrale\logs\updates-2026-10-08.log`, 12:50–12:55
- Fremdprozess (Go, `agy update`): Download bricht über IPv6 ab, Zentrale lädt per `curl -4` aus dem Manifest nach — `bugs/bug-cases/2026-11-06-updatezentrale-agy-download-ipv6-abbruch.md`.
