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

# Die Quelle finden — ohne Annahme darueber, wer das HTML erzeugt hat.
# Bevorzugt der Werft-Aufbau (Bildschirme je Erscheinung in eigenen Ordnern); sonst jede
# gefundene HTML-Datei als ein Bildschirm, alle unter der Erscheinung "standard".
$bildschirme = Join-Path $Design "WERFT-DESIGN\bildschirme"
$werftAufbau = Test-Path $bildschirme
if (-not $werftAufbau) {
    $treffer = Get-ChildItem $Design -Recurse -Filter *.html -File |
        Where-Object { $_.FullName -notmatch '\\\.werft-generated\\' }
    if (-not $treffer) { throw "Keine HTML-Datei unter $Design gefunden." }
    Write-Host "Kein Werft-Aufbau — $($treffer.Count) HTML-Datei(en) gefunden, jede gilt als ein Bildschirm."
}
$cssPfad = if ($werftAufbau) { (Join-Path $bildschirme "design.css") -replace '\\', '/' } else { $null }

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
  // "1" darf hier NICHT stehen: es wuerde `flexGrow: 1` mitloeschen — also die Aussage
  // "dieses Element fuellt den Raum". `opacity: 1` wird stattdessen gezielt gefiltert.
  const LEER = new Set(["none","normal","auto","0px","rgba(0, 0, 0, 0)","static","visible","0s"]);
  const istLeer = (p, v) => LEER.has(v) || (p === "opacity" && v === "1") ||
                            (p === "textAlign" && v === "start") || (p === "flexGrow" && v === "0");

  const wurzel = document.querySelector("[data-werft-screen],[data-screen-id]") || document.body;

  // Versteckte Elemente zeigen einen Nullkasten — dann fehlt genau den Zustaenden
  // (Antwortkarte, Fehlerkarte, Ladezustand) ihre Anordnung. Deshalb werden sie fuer die
  // Messung sichtbar gemacht und danach wieder zurueckgesetzt.
  const versteckte = Array.from(wurzel.querySelectorAll("[hidden]"));
  const warVersteckt = new Set(versteckte);   // vor dem Aufdecken merken
  const alteAnzeige = new Map();
  for (const v of versteckte) {
    alteAnzeige.set(v, v.style.display);
    v.removeAttribute("hidden");
    v.style.display = "";
  }
  void wurzel.offsetHeight;   // Neuberechnung des Layouts erzwingen

  const null0 = wurzel.getBoundingClientRect();
  const elemente = [];

  const gehe = (el, pfad) => {
    if (el.tagName === "SCRIPT" || el.tagName === "STYLE") return;
    const s = getComputedStyle(el), r = el.getBoundingClientRect();
    const stil = {};
    for (const p of WICHTIG) { const v = s[p]; if (v && !istLeer(p, v)) stil[p] = v; }
    const vorher = {}, nachher = {};
    for (const [pseudo, ziel] of [["::before", vorher], ["::after", nachher]]) {
      const ps = getComputedStyle(el, pseudo);
      if (ps.content && ps.content !== "none") {
        for (const p of WICHTIG) { const v = ps[p]; if (v && !istLeer(p, v)) ziel[p] = v; }
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
    // Der WERT eines Bedienelements steht nicht im Text, sondern im Feld. Ohne ihn bleibt
    // im gebauten Bildschirm "08:00" leer, obwohl der Entwurf es zeigt.
    if (typeof el.value === "string" && el.value) e.wert = el.value;
    if (el.tagName === "SELECT" && el.selectedOptions && el.selectedOptions[0]) {
      e.wert = el.selectedOptions[0].textContent.trim();
    }
    if (warVersteckt.has(el)) e.versteckt = true;
    if (Object.keys(vorher).length) e.vorher = vorher;
    if (Object.keys(nachher).length) e.nachher = nachher;

    // Symbole. Ohne die Pfade baut die Umsetzung aehnliche Symbole aus einer
    // Standardsammlung — und die sehen anders aus. Also die echten Umrisse mitnehmen.
    if (el.tagName.toLowerCase() === "svg") {
      const pfade = [];
      el.querySelectorAll("path,rect,circle,line,polygon,polyline,ellipse").forEach(f => {
        const t = f.tagName.toLowerCase();
        const eintrag = { form: t };
        if (t === "path") eintrag.d = f.getAttribute("d");
        else for (const a of f.attributes) eintrag[a.name] = a.value;
        const fuellung = getComputedStyle(f).fill;
        if (fuellung && fuellung !== "rgb(0, 0, 0)") eintrag.fuellung = fuellung;
        pfade.push(eintrag);
      });
      // <use href="#id"> zeigt auf ein <symbol> weiter oben im Dokument.
      el.querySelectorAll("use").forEach(u => {
        const ref = (u.getAttribute("href") || u.getAttribute("xlink:href") || "").replace("#", "");
        const quelle = ref && document.getElementById(ref);
        if (quelle) {
          quelle.querySelectorAll("path,rect,circle,polygon").forEach(f => {
            const t = f.tagName.toLowerCase();
            const eintrag = { form: t, ausSymbol: ref };
            if (t === "path") eintrag.d = f.getAttribute("d");
            else for (const a of f.attributes) eintrag[a.name] = a.value;
            pfade.push(eintrag);
          });
          if (!el.getAttribute("viewBox") && quelle.getAttribute("viewBox")) {
            e.sichtfeld = quelle.getAttribute("viewBox");
          }
        }
      });
      if (pfade.length) e.symbol = pfade;
      const vb = el.getAttribute("viewBox"); if (vb) e.sichtfeld = vb;
    }
    elemente.push(e);

    Array.from(el.children).forEach((k, i) =>
      gehe(k, pfad + "/" + k.tagName.toLowerCase() + "[" + i + "]"));
  };
  gehe(wurzel, wurzel.tagName.toLowerCase());

  for (const v of versteckte) { v.setAttribute("hidden", ""); v.style.display = alteAnzeige.get(v) || ""; }

  // Die Messung oben sieht nur den Ausgangszustand. Alles, was erst beim Druecken,
  // Aufnehmen oder Auswaehlen entsteht — und damit der groesste Teil der Bewegung —
  // steht in Regeln mit Zustands-Selektoren. Die werden hier mitgenommen.
  const keyframes = [], zustaende = [], reduziert = [];
  const ZUSTAND = /:hover|:active|:focus|:checked|:disabled|\[data-[a-z-]+=|\.is-[a-z-]+|\[aria-(pressed|selected|current|expanded)=|\[hidden\]/i;

  const sammle = (regeln, inMedia) => {
    for (const regel of regeln) {
      if (regel.type === CSSRule.KEYFRAMES_RULE) {
        keyframes.push({ name: regel.name, text: regel.cssText });
      } else if (regel.type === CSSRule.MEDIA_RULE) {
        const istReduziert = /prefers-reduced-motion/.test(regel.conditionText || "");
        if (istReduziert) {
          for (const r of regel.cssRules) reduziert.push(r.cssText);
        }
        sammle(regel.cssRules, inMedia || istReduziert);
      } else if (regel.type === CSSRule.STYLE_RULE && !inMedia) {
        const sel = regel.selectorText || "";
        if (!ZUSTAND.test(sel)) continue;
        // Nur Regeln behalten, die Elemente DIESES Bildschirms betreffen.
        let trifft = false;
        try { trifft = !!wurzel.querySelector(sel.replace(/::?(before|after)/g, "")) ; } catch (e) { trifft = false; }
        if (!trifft) {
          try { trifft = wurzel.matches(sel.split(",")[0].trim()); } catch (e) { /* ignorieren */ }
        }
        if (trifft) zustaende.push({ selektor: sel, text: regel.cssText });
      }
    }
  };
  for (const blatt of document.styleSheets) {
    let regeln; try { regeln = blatt.cssRules; } catch (e) { continue; }
    sammle(regeln, false);
  }

  return JSON.stringify({
    bildschirm: wurzel.getAttribute("data-screen-id") || null,
    elemente, keyframes, zustaende, reduzierteBewegung: reduziert
  });
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

$arbeit = Join-Path $env:TEMP ("werft-messung-" + [Guid]::NewGuid().ToString("N").Substring(0, 8))
New-Item -ItemType Directory -Force -Path $arbeit | Out-Null

# Die Arbeitsliste: je Eintrag eine Erscheinung und eine HTML-Datei.
$aufgaben = if ($werftAufbau) {
    Get-ChildItem $bildschirme -Directory | ForEach-Object {
        $e = $_.Name
        Get-ChildItem $_.FullName -Filter *.html | ForEach-Object {
            [pscustomobject]@{ Erscheinung = $e; Datei = $_ }
        }
    }
} else {
    Get-ChildItem $Design -Recurse -Filter *.html -File |
        Where-Object { $_.FullName -notmatch '\\\.werft-generated\\' } |
        ForEach-Object { [pscustomobject]@{ Erscheinung = "standard"; Datei = $_ } }
}

# --- Die Breite kommt aus der DATEI, nicht aus einer Annahme --------------------------
#
# Warum das wichtig ist: Media Queries fragen die FENSTERbreite. Wird bei 412 px gemessen,
# obwohl der Entwurf fuer 360 px gilt, greifen andere Regeln — und die Messung beschreibt eine
# Anordnung, die es im Entwurf nie gab. Genau so ist ein Einstellungsbildschirm mit
# "Beschriftung ueber dem Feld" als "Beschriftung neben dem Feld" im Code gelandet, ohne dass
# irgendwo ein Fehler auftauchte.
#
# Der Export schreibt die gueltige Breite deshalb in jede Bildschirmdatei:
#   <meta name="werft-render-width" content="412">
# Sie wird hier gelesen und gilt. Der Parameter -Breite ist nur noch der Rueckfall fuer Dateien
# ohne diese Angabe.
foreach ($a in $aufgaben) {
    $kopf = Get-Content $a.Datei.FullName -Raw -Encoding UTF8
    $treffer = [regex]::Match($kopf, '<meta\s+name="werft-render-width"\s+content="(\d+)"', 'IgnoreCase')
    $a | Add-Member -NotePropertyName Breite -NotePropertyValue ([int]$(if ($treffer.Success) { $treffer.Groups[1].Value } else { $Breite }))
    $a | Add-Member -NotePropertyName BreiteAusDatei -NotePropertyValue $treffer.Success
}

$ohneAngabe = @($aufgaben | Where-Object { -not $_.BreiteAusDatei })
if ($ohneAngabe.Count -gt 0) {
    Write-Warning ("$($ohneAngabe.Count) Datei(en) ohne <meta name=""werft-render-width""> — dort gilt der Rueckfall $Breite px. " +
                   "Stimmt der nicht, ist die Messung dieser Bildschirme nicht belastbar.")
}
$breiten = @($aufgaben | Select-Object -ExpandProperty Breite -Unique | Sort-Object)
Write-Host ("Renderbreiten in diesem Design: " + ($breiten -join ", ") + " px")

# --- Messen, je Breite ein eigener Browser ---------------------------------------------
#
# Der Browser wird PRO BREITE neu gestartet, damit die Fensterbreite beim ERZEUGEN des Kontexts
# feststeht. `setViewportSize` bzw. ein nachtraegliches Vergroessern loest kein `resize`-Event aus
# (dokumentiertes Verhalten, Playwright-Issue #36084) — nachtraeglich gesetzte Breiten koennen
# alte Layoutwerte liefern. Ein Neustart je Breite kostet drei Sekunden und ist dafuer eindeutig.
$gemessen = 0
foreach ($breite in $breiten) {
$Breite = $breite
$gruppe = @($aufgaben | Where-Object { $_.Breite -eq $breite })
Write-Host ""
Write-Host ("Breite $breite px — $($gruppe.Count) Bildschirm(e)")

# Der Rahmen der Einzeldatei ist 1440 px breit — auf Geraetebreite zwingen, sonst misst man
# einen Bildschirm, den es auf dem Geraet nie gibt.
$rahmen = "<style>html,body{margin:0}.werft-screens{width:${Breite}px!important}" +
          ".werft-screen{width:${Breite}px!important;height:${Hoehe}px!important;overflow:hidden!important}</style>"

$chrome = Start-Process $browser -PassThru -ArgumentList `
    "--headless=new", "--disable-gpu", "--hide-scrollbars", "--remote-debugging-port=$Port",
    "--allow-file-access-from-files", "--force-device-scale-factor=1",
    "--user-data-dir=$arbeit\profil-$Breite", "--no-first-run", "--window-size=$Breite,$Hoehe", "about:blank"
Start-Sleep -Seconds 3

try {
    $gruppe | ForEach-Object {
        $erscheinung = $_.Erscheinung
        $zielMessung = Join-Path $Ziel "messung\$erscheinung"
        $zielBild = Join-Path $Ziel "bilder\$erscheinung"
        New-Item -ItemType Directory -Force -Path $zielMessung, $zielBild | Out-Null

        $_.Datei | ForEach-Object {
            $name = $_.BaseName
            $html = Get-Content $_.FullName -Raw -Encoding UTF8
            if ($cssPfad) { $html = $html -replace 'href="\.\./design\.css"', "href=""file:///$cssPfad""" }
            $html = $html -replace '</head>', "$rahmen</head>"
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
                # Die Breite gehoert IN die Messung — sonst weiss niemand mehr, fuer welche
                # Fensterbreite diese Kaesten gelten, und eine Koordinate ohne ihre Breite ist
                # nicht nachpruefbar.
                $json = $json -replace '^\{', ('{"breite":' + $Breite + ',')
                [IO.File]::WriteAllText((Join-Path $zielMessung "$name.json"), $json, [Text.UTF8Encoding]::new($false))
                $anzahl = ([regex]::Matches($json, '"pfad":')).Count
                Write-Host ("  {0,-26} {1,4} Elemente  @{2}px" -f "$erscheinung/$name", $anzahl, $Breite)
                $script:gemessen++
            } else {
                Write-Warning "  $erscheinung/$name — keine Messung zurueckbekommen"
            }
        }
    }
} finally {
    Stop-Process -Id $chrome.Id -Force -ErrorAction SilentlyContinue
    Start-Sleep -Milliseconds 300
}
}   # Ende der Breiten-Schleife

Remove-Item $arbeit -Recurse -Force -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "$gemessen Bildschirme vermessen."
Write-Host "  Messung: $Ziel\messung\   (verbindliche Bauvorlage fuer Stufe 3)"
Write-Host "  Bilder:  $Ziel\bilder\"
