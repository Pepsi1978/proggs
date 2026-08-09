# Verlustfreie Vermessung eines Werft-Designs.
#
# Warum es das gibt: Ein Spec, das den Entwurf in Prosa zusammenfasst, verliert bei jeder
# Zusammenfassung etwas — und was das Spec nicht sagt, erfindet die Umsetzung. Dieses Skript
# fasst nichts zusammen. Es oeffnet jeden Bildschirm im Browser und liest fuer JEDES Element
# die tatsaechlich berechneten Werte aus. Damit sind Vererbung, `var()`, `color-mix()` und
# die gestaffelten Regelschichten bereits aufgeloest — das Ergebnis ist vollstaendig durch
# Konstruktion, nicht durch Sorgfalt.
#
#   .\messe-design.ps1 -Design <Design-Ordner> -Ziel <Spec-v2-Ordner>
#
# Erzeugt je Erscheinung und Bildschirm:
#   <Ziel>\messung\<erscheinung>\<name>.json   die Messung — verbindliche Bauvorlage
#   <Ziel>\bilder\<erscheinung>\<name>.png     das Bild desselben Zustands
#
# Technik: Chrome im Headless-Modus mit DevTools-Protokoll. `--dump-dom` gibt es im neuen
# Headless nicht mehr, deshalb der Weg ueber `Runtime.evaluate` per WebSocket.

param(
    [Parameter(Mandatory = $true)][string]$Design,
    [Parameter(Mandatory = $true)][string]$Ziel,
    [int]$Breite = 412,
    [int]$Hoehe = 915,
    [int]$Port = 9333
)

$ErrorActionPreference = "Stop"

$browser = @(
    "C:\Program Files\Google\Chrome\Application\chrome.exe",
    "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
) | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $browser) { throw "Weder Chrome noch Edge gefunden — ohne Browser keine Messung." }

$bildschirme = Join-Path $Design "WERFT-DESIGN\bildschirme"
if (-not (Test-Path $bildschirme)) { throw "Nicht gefunden: $bildschirme" }
$cssPfad = (Join-Path $bildschirme "design.css") -replace '\\', '/'

# --- Der Messfuehler ------------------------------------------------------------------
# Laeuft IM Browser und liefert die Messung als JSON-Zeichenkette zurueck.
$messJs = @'
(() => {
  const WICHTIG = [
    "display","position","top","right","bottom","left","zIndex","overflow",
    "width","height","minWidth","minHeight","maxWidth","boxSizing",
    "flexDirection","justifyContent","alignItems","alignSelf","flexGrow","flexBasis","gap",
    "gridTemplateColumns","gridTemplateRows",
    "marginTop","marginRight","marginBottom","marginLeft",
    "paddingTop","paddingRight","paddingBottom","paddingLeft",
    "color","backgroundColor","backgroundImage","opacity",
    "borderTopWidth","borderRightWidth","borderBottomWidth","borderLeftWidth",
    "borderTopColor","borderTopStyle",
    "borderTopLeftRadius","borderTopRightRadius","borderBottomRightRadius","borderBottomLeftRadius",
    "boxShadow","backdropFilter","filter","transform","transformOrigin",
    "fontFamily","fontSize","fontWeight","fontStyle","lineHeight","letterSpacing",
    "textAlign","textTransform","textOverflow","whiteSpace",
    "transitionProperty","transitionDuration","transitionTimingFunction","transitionDelay",
    "animationName","animationDuration","animationTimingFunction","animationIterationCount",
    "animationDirection","animationFillMode"
  ];
  const LEER = new Set(["none","normal","auto","0px","rgba(0, 0, 0, 0)","static","visible","0s","1","start"]);

  const wurzel = document.querySelector("[data-werft-screen],[data-screen-id]") || document.body;
  const null0 = wurzel.getBoundingClientRect();
  const elemente = [];

  const gehe = (el, pfad) => {
    if (el.tagName === "SCRIPT" || el.tagName === "STYLE") return;
    const s = getComputedStyle(el), r = el.getBoundingClientRect();
    const stil = {};
    for (const p of WICHTIG) { const v = s[p]; if (v && !LEER.has(v)) stil[p] = v; }
    const vorher = {}, nachher = {};
    for (const [pseudo, ziel] of [["::before", vorher], ["::after", nachher]]) {
      const ps = getComputedStyle(el, pseudo);
      if (ps.content && ps.content !== "none") {
        for (const p of WICHTIG) { const v = ps[p]; if (v && !LEER.has(v)) ziel[p] = v; }
      }
    }
    const text = Array.from(el.childNodes).filter(n => n.nodeType === 3)
      .map(n => n.textContent.trim()).filter(Boolean).join(" ");

    const e = {
      pfad, tag: el.tagName.toLowerCase(),
      kasten: {
        x: Math.round((r.left - null0.left) * 100) / 100,
        y: Math.round((r.top - null0.top) * 100) / 100,
        breite: Math.round(r.width * 100) / 100,
        hoehe: Math.round(r.height * 100) / 100
      },
      stil
    };
    if (el.id) e.id = el.id;
    if (typeof el.className === "string" && el.className) e.klassen = el.className;
    if (text) e.text = text;
    const f = el.getAttribute("data-werft-funktion"); if (f) e.funktion = f;
    const n = el.getAttribute("data-werft-navigate"); if (n) e.fuehrtZu = n;
    const a = el.getAttribute("aria-label"); if (a) e.beschriftung = a;
    const ph = el.getAttribute("placeholder"); if (ph) e.platzhalter = ph;
    if (el.hasAttribute("hidden")) e.versteckt = true;
    if (Object.keys(vorher).length) e.vorher = vorher;
    if (Object.keys(nachher).length) e.nachher = nachher;
    elemente.push(e);

    Array.from(el.children).forEach((k, i) =>
      gehe(k, pfad + "/" + k.tagName.toLowerCase() + "[" + i + "]"));
  };
  gehe(wurzel, wurzel.tagName.toLowerCase());

  const keyframes = [];
  for (const blatt of document.styleSheets) {
    let regeln; try { regeln = blatt.cssRules; } catch (e) { continue; }
    for (const regel of regeln) {
      if (regel.type === CSSRule.KEYFRAMES_RULE) keyframes.push({ name: regel.name, text: regel.cssText });
    }
  }
  return JSON.stringify({ bildschirm: wurzel.getAttribute("data-screen-id") || null, elemente, keyframes });
})()
'@

# --- WebSocket-Anfrage ans DevTools-Protokoll -------------------------------------------
function Invoke-Cdp {
    param([string]$WsUrl, [string]$Ausdruck)
    $ws = [System.Net.WebSockets.ClientWebSocket]::new()
    $ws.Options.KeepAliveInterval = [TimeSpan]::FromSeconds(10)
    $ws.ConnectAsync([Uri]$WsUrl, [Threading.CancellationToken]::None).Wait(10000) | Out-Null

    $anfrage = @{ id = 1; method = "Runtime.evaluate"; params = @{
        expression = $Ausdruck; returnByValue = $true; awaitPromise = $true } } | ConvertTo-Json -Depth 6 -Compress
    $bytes = [Text.Encoding]::UTF8.GetBytes($anfrage)
    $ws.SendAsync([ArraySegment[byte]]::new($bytes), 'Text', $true, [Threading.CancellationToken]::None).Wait(10000) | Out-Null

    $puffer = [byte[]]::new(1048576)
    $sb = [Text.StringBuilder]::new()
    do {
        $t = $ws.ReceiveAsync([ArraySegment[byte]]::new($puffer), [Threading.CancellationToken]::None)
        $t.Wait(30000) | Out-Null
        [void]$sb.Append([Text.Encoding]::UTF8.GetString($puffer, 0, $t.Result.Count))
    } while (-not $t.Result.EndOfMessage)
    $ws.Dispose()
    ($sb.ToString() | ConvertFrom-Json).result.result.value
}

# Der Rahmen der Einzeldatei ist 1440 px breit — auf Geraetebreite zwingen, sonst misst man
# einen Bildschirm, den es auf dem Geraet nie gibt.
$rahmen = "<style>html,body{margin:0}.werft-screens{width:${Breite}px!important}" +
          ".werft-screen{width:${Breite}px!important;height:${Hoehe}px!important;overflow:hidden!important}</style>"

$arbeit = Join-Path $env:TEMP ("werft-messung-" + [Guid]::NewGuid().ToString("N").Substring(0, 8))
New-Item -ItemType Directory -Force -Path $arbeit | Out-Null

$chrome = Start-Process $browser -PassThru -ArgumentList `
    "--headless=new", "--disable-gpu", "--hide-scrollbars", "--remote-debugging-port=$Port",
    "--allow-file-access-from-files",
    "--user-data-dir=$arbeit\profil", "--no-first-run", "--window-size=$Breite,$Hoehe", "about:blank"
Start-Sleep -Seconds 3

$gemessen = 0
try {
    Get-ChildItem $bildschirme -Directory | ForEach-Object {
        $erscheinung = $_.Name
        $zielMessung = Join-Path $Ziel "messung\$erscheinung"
        $zielBild = Join-Path $Ziel "bilder\$erscheinung"
        New-Item -ItemType Directory -Force -Path $zielMessung, $zielBild | Out-Null

        Get-ChildItem $_.FullName -Filter *.html | ForEach-Object {
            $name = $_.BaseName
            $html = (Get-Content $_.FullName -Raw -Encoding UTF8) `
                -replace 'href="\.\./design\.css"', "href=""file:///$cssPfad""" `
                -replace '</head>', "$rahmen</head>"
            $datei = Join-Path $arbeit "$name.html"
            [IO.File]::WriteAllText($datei, $html, [Text.UTF8Encoding]::new($false))
            $url = "file:///$($datei -replace '\\','/')"

            # 1. Das Bild — derselbe Zustand, den auch die Messung sieht.
            & $browser --headless=new --disable-gpu --hide-scrollbars --force-device-scale-factor=2 `
                --window-size="$Breite,$Hoehe" --screenshot="$(Join-Path $zielBild "$name.png")" $url 2>&1 | Out-Null

            # 2. Die Messung ueber das DevTools-Protokoll.
            $seite = Invoke-RestMethod "http://127.0.0.1:$Port/json/new?$([Uri]::EscapeDataString($url))" -Method Put
            Start-Sleep -Milliseconds 900
            $json = Invoke-Cdp -WsUrl $seite.webSocketDebuggerUrl -Ausdruck $messJs
            Invoke-RestMethod "http://127.0.0.1:$Port/json/close/$($seite.id)" | Out-Null

            if ($json) {
                [IO.File]::WriteAllText((Join-Path $zielMessung "$name.json"), $json, [Text.UTF8Encoding]::new($false))
                $anzahl = ([regex]::Matches($json, '"pfad":')).Count
                Write-Host ("  {0,-26} {1,4} Elemente" -f "$erscheinung/$name", $anzahl)
                $script:gemessen++
            } else {
                Write-Warning "  $erscheinung/$name — keine Messung zurueckbekommen"
            }
        }
    }
} finally {
    Stop-Process -Id $chrome.Id -Force -ErrorAction SilentlyContinue
    Start-Sleep -Milliseconds 300
    Remove-Item $arbeit -Recurse -Force -ErrorAction SilentlyContinue
}

Write-Host ""
Write-Host "$gemessen Bildschirme vermessen."
Write-Host "  Messung: $Ziel\messung\   (verbindliche Bauvorlage fuer Stufe 3)"
Write-Host "  Bilder:  $Ziel\bilder\"
