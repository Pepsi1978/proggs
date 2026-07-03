# start-wt-common.ps1 — Gemeinsame Robust-Start-Funktion für alle CLI-Launcher
# (start-claude-wt.ps1, start-opencode-wt.ps1, start-codex-wt.ps1, start-gemini-wt.ps1).
#
# WARUM ES DIESE DATEI GIBT (Root Cause, Diagnose 2026-07-03):
# Windows Terminal nutzt intern eine Monarch/Peasant-Architektur: neue Tabs werden
# per COM-Handshake beim "Monarch"-Prozess angemeldet. Beim Windows-Kaltstart existiert
# oft ein halbtoter WT-Prozess (im Hochfahren oder Sterben). Jeder Andock-Versuch an
# diesen Prozess lässt das neue Fenster kurz aufblitzen und sofort wieder schließen.
# BEWEIS (start-claude-wt.log, 2026-07-03 12:11): Bei 5 Klicks in 14 Sekunden hatte der
# einzige WT-Prozess JEDES MAL eine andere PID (25412→44972→36184→28036→15304) — die
# Prozesse starben fortlaufend, jeder Klick dockte an den Sterbenden des Vorgängers an.
# Kein Crash im Ereignisprotokoll: die Fenster schließen "regulär" nach dem
# gescheiterten Handshake.
#
# Zwei frühere Fixes (2026-06-11, 2026-06-24) versuchten VORHER zu erraten, ob das
# Andocken klappt (Prozess-Existenz bzw. MainWindowHandle-Check). Beide scheiterten,
# weil (a) ein sterbender Prozess trotzdem ein Fensterhandle haben kann und
# (b) windowingBehavior=useAnyExisting in den WT-Settings JEDEN Start andocken lässt,
# auch ohne -w 0 — der "frisches Fenster"-Fallback war dadurch wirkungslos.
#
# NEUE ARCHITEKTUR — PRÜFEN STATT RATEN (3 Schichten, Direktive #3):
#   Schicht 1 (präventiv):  Zombie-WT-Prozesse (ohne Fenster, älter als 15 s) beenden.
#   Schicht 2 (verifizierend): Nach jedem wt-Start bis zu 8 s prüfen, ob das innere
#                            CLI-pwsh wirklich läuft UND 3 s stabil bleibt. Wenn nicht:
#                            automatisch neu versuchen — ab Versuch 2 mit "-w new"
#                            (erzwungenes neues Fenster, überschreibt useAnyExisting).
#   Schicht 3 (Fallback):   Scheitern alle wt-Versuche, startet das CLI garantiert in
#                            einem klassischen Konsolenfenster (pwsh direkt, ohne WT).
# Ergebnis: EIN Klick genügt immer — das frühere 5-6x-Klicken übernimmt das Skript.
#
# Verhalten im Normalfall bleibt unverändert: existiert ein gesundes WT-Fenster,
# landet der Tab wie bisher dort (mit Farbe und Titel).

function Start-WtCliRobust {
    param(
        # Log-Datei des aufrufenden Launchers (dort landen alle Diagnose-Zeilen)
        [Parameter(Mandatory)][string]$LogFile,
        # Voller Pfad zu wt.exe
        [Parameter(Mandatory)][string]$WtPath,
        # wt-Argumente AB 'new-tab' (ohne -w ...); das -w-Ziel setzt diese Funktion selbst
        [Parameter(Mandatory)][string[]]$TabArgs,
        # Regex, das die CommandLine des INNEREN CLI-pwsh eindeutig erkennt
        # (z.B. 'start-claude\.ps1'). Damit wird der Erfolg verifiziert.
        [Parameter(Mandatory)][string]$InnerMatch,
        # pwsh-Argumente für den Konsolen-Fallback (Schicht 3), ohne wt
        [Parameter(Mandatory)][string[]]$FallbackPwshArgs,
        # Arbeitsverzeichnis für den Fallback
        [string]$FallbackWorkDir = $env:USERPROFILE,
        # Maximale wt-Versuche, bevor der Konsolen-Fallback greift
        [int]$MaxWtAttempts = 3
    )

    function _Log([string]$msg) {
        $stamp = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'
        Add-Content -Path $LogFile -Value "[$stamp] [robust] $msg" -ErrorAction SilentlyContinue
    }

    # Wartet bis zu 8 s auf einen NEU entstandenen inneren CLI-Prozess und prüft,
    # ob er 3 s überlebt (das Kaltstart-Race tötet Fenster binnen ~2-3 s).
    # Rückgabe: ProcessId bei stabilem Erfolg, 0 bei Fehlschlag.
    function _WaitInnerAlive([datetime]$since) {
        $deadline = (Get-Date).AddSeconds(8)
        while ((Get-Date) -lt $deadline) {
            $cands = @(Get-CimInstance Win32_Process -Filter "Name='pwsh.exe'" -ErrorAction SilentlyContinue |
                Where-Object { $_.CommandLine -match $InnerMatch -and $_.CreationDate -gt $since })
            if ($cands.Count -gt 0) {
                $innerPid = $cands[0].ProcessId
                _Log "inneres CLI-pwsh erschienen (PID $innerPid) - pruefe 3 s Stabilitaet"
                Start-Sleep -Seconds 3
                if (Get-Process -Id $innerPid -ErrorAction SilentlyContinue) { return $innerPid }
                _Log "inneres CLI-pwsh (PID $innerPid) ist binnen 3 s gestorben - Fenster-Race erkannt"
                return 0
            }
            Start-Sleep -Milliseconds 400
        }
        return 0
    }

    for ($attempt = 1; $attempt -le $MaxWtAttempts; $attempt++) {
        # Schicht 1: Zombie-Cleanup. Ein WT-Prozess ohne Fenster, der älter als 15 s ist,
        # ist kein startendes Terminal mehr, sondern ein hängender Monarch-Kandidat.
        # Er trägt keine Benutzerarbeit (kein Fenster = keine sichtbaren Tabs) und wird
        # beendet, damit er neue Starts nicht mit in den Tod reißt.
        $zombies = @(Get-Process WindowsTerminal -ErrorAction SilentlyContinue | Where-Object {
            $_.MainWindowHandle -eq 0 -and ((Get-Date) - $_.StartTime).TotalSeconds -gt 15
        })
        foreach ($z in $zombies) {
            $age = [int]((Get-Date) - $z.StartTime).TotalSeconds
            _Log "Zombie-WT ohne Fenster (PID $($z.Id), Alter ${age}s) wird beendet"
            Stop-Process -Id $z.Id -Force -ErrorAction SilentlyContinue
        }

        # Andocken (-w 0) nur beim 1. Versuch und nur an ein GESUNDES Fenster
        # (Handle vorhanden UND Prozess antwortet). Ab Versuch 2 immer -w new:
        # erzwingt ein frisches Fenster und überschreibt useAnyExisting wirklich.
        $healthy = @(Get-Process WindowsTerminal -ErrorAction SilentlyContinue |
            Where-Object { $_.MainWindowHandle -ne 0 -and $_.Responding })
        $useAttach = ($attempt -eq 1 -and $healthy.Count -gt 0)
        $windowTarget = if ($useAttach) { @('-w', '0') } else { @('-w', 'new') }
        _Log "Versuch $attempt/${MaxWtAttempts}: gesunde WT-Fenster=$($healthy.Count) -> $(if ($useAttach) { '-w 0 (andocken)' } else { '-w new (frisches Fenster)' })"

        $since = Get-Date
        try {
            Start-Process -FilePath $WtPath -ArgumentList ($windowTarget + $TabArgs) -ErrorAction Stop
        } catch {
            _Log "ERROR beim wt-Start (Versuch $attempt): $_"
            continue
        }

        $alivePid = _WaitInnerAlive $since
        if ($alivePid) {
            _Log "ERFOLG: CLI laeuft stabil (pwsh PID $alivePid, Versuch $attempt)"
            return $true
        }
        _Log "Versuch $attempt fehlgeschlagen - CLI nicht (stabil) gestartet, wiederhole automatisch"
    }

    # Schicht 3: Garantierter Fallback ohne Windows Terminal. Ein klassisches
    # Konsolenfenster (conhost) kennt keinen Monarch-Handshake und startet immer.
    # Keine Tabfarbe, aber das CLI ist da — besser als ein toter Klick.
    _Log "Alle $MaxWtAttempts wt-Versuche fehlgeschlagen -> Fallback: klassisches Konsolenfenster"
    $since = Get-Date
    try {
        $pwshExe = Join-Path $PSHOME 'pwsh.exe'
        if (-not (Test-Path $pwshExe)) { $pwshExe = 'pwsh.exe' }
        Start-Process -FilePath $pwshExe -ArgumentList $FallbackPwshArgs -WorkingDirectory $FallbackWorkDir -ErrorAction Stop
        $alivePid = _WaitInnerAlive $since
        if ($alivePid) {
            _Log "FALLBACK ERFOLG: CLI laeuft im klassischen Konsolenfenster (PID $alivePid)"
        } else {
            _Log "FALLBACK gestartet, Verifikation offen - Konsolenfenster sollte sichtbar sein"
        }
        return $true
    } catch {
        _Log "FATAL: auch der Konsolen-Fallback schlug fehl: $_"
        return $false
    }
}
