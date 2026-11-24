// Webfoto im Chat: lädt ein öffentliches Foto (per Adresse oder als Hauptfoto des passenden Wikipedia-Artikels) über den Bildproxy wsrv.nl als unkomprimiertes PNG (Base64-Text),
// dekodiert es hier und schreibt es als BMP, dessen Bytes alle unter 0x80 liegen. Nur so übersteht es textDatei
// (die schreibt UTF-8): 32 Bit je Pixel mit drei 7-Bit-Masken, die Bildanzeige von Android skaliert sie auf 8 Bit hoch.

var PROXY = "https://wsrv.nl/";
var WIKIPEDIA_API = "https://en.wikipedia.org/w/api.php";
var UA = { "User-Agent": "JarvisHelfer/1.0 (ein-oeffentlich-verfuegbares-f)" };
var KANTE_STANDARD = 512;
var KANTE_MIN = 64;
var KANTE_MAX = 600;
var BMP_KOPF = 14 + 40 + 12;
var ERLAUBT = { suche: 1, url: 1, titel: 1, max_kante: 1 };

function fehler(t) { throw new Error(t); }

// Jede Zahl im BMP-Kopf wird als 4 Bytes geschrieben; keines darf 0x80 oder mehr sein.
function siebenBit(n) { return n >= 0 && n < 0x80000000 && (n & 0x80808080) === 0; }
function bmpPasst(w, h) { return siebenBit(w) && siebenBit(h) && siebenBit(w * h * 4) && siebenBit(BMP_KOPF + w * h * 4); }

// Gültige Seitenlängen (jedes Byte < 0x80) bis 639: 1–127, 256–383, 512–639.
var GRENZEN = [1, 127, 256, 383, 512, 639];
function gueltigeNahe(x) {
    var r = [Math.round(x), Math.floor(x), Math.ceil(x)].concat(GRENZEN), aus = [];
    for (var i = 0; i < r.length; i++) if (r[i] >= 1 && r[i] <= 639 && siebenBit(r[i])) aus.push(r[i]);
    return aus;
}

// Die Brücke kürzt jede HTTP-Antwort bei 2.000.000 Zeichen; das Base64-PNG muss sicher darunter bleiben.
var MAX_BASE64 = 1900000;
function passtInAntwort(w, h, bytesJePixel) { return Math.ceil((w * bytesJePixel + 1) * h * 1.0002 / 3) * 4 + 2000 < MAX_BASE64; }

/**
 * Zielgröße mit gültigen Bytes: längste Seite möglichst nah an [kante], Seitenverhältnis höchstens 15 % beschnitten
 * (fit=cover schneidet den Rest). Lieber etwas größer als viel kleiner, weil manche Längen gar nicht darstellbar sind.
 */
function zielMasse(w0, h0, kante, bytesJePixel) {
    var r = w0 / h0, lang = Math.max(w0, h0);
    var t = Math.min(kante, Math.max(lang, 256)), best = null, bestWert = Infinity;
    for (var w = 1; w <= 639; w++) {
        if (!siebenBit(w)) continue;
        var hs = gueltigeNahe(w / r);
        for (var i = 0; i < hs.length; i++) {
            var h = hs[i], abw = Math.abs((w / h) / r - 1);
            if (abw > 0.15 || !bmpPasst(w, h) || !passtInAntwort(w, h, bytesJePixel)) continue;
            var l = Math.max(w, h), wert = (l > t ? 2 * (l - t) : t - l) + abw * t * 4;
            if (wert < bestWert) { bestWert = wert; best = { w: w, h: h }; }
        }
    }
    if (!best) fehler("Für dieses Seitenverhältnis gibt es keine anzeigbare Größe.");
    return best;
}

function leer(v) { return v === undefined || v === null || v === ""; }

function pruefeEingabe(e) {
    if (e === null || typeof e !== "object" || Array.isArray(e)) fehler("Die Eingabe fehlt.");
    for (var k in e) if (Object.prototype.hasOwnProperty.call(e, k) && !Object.prototype.hasOwnProperty.call(ERLAUBT, k)) fehler("Unbekannte Eingabe: " + k);
    if (leer(e.suche) === leer(e.url)) fehler("Genau eines angeben: suche oder url.");
    if (!leer(e.url)) {
        if (typeof e.url !== "string") fehler("url muss Text sein.");
        if (e.url.length > 2000) fehler("url ist zu lang.");
        if (!/^https:\/\/[^\s\/?#@]+\.[^\s\/?#@]+(\/[^\s]*)?$/i.test(e.url.trim())) fehler("url muss eine vollständige HTTPS-Adresse sein.");
    }
    if (!leer(e.suche)) {
        if (typeof e.suche !== "string" || !e.suche.trim()) fehler("suche muss Text sein.");
        if (e.suche.length > 200) fehler("suche ist zu lang.");
    }
    if (!leer(e.titel) && typeof e.titel !== "string") fehler("titel muss Text sein.");
    var kante = KANTE_STANDARD;
    if (!leer(e.max_kante)) {
        if (typeof e.max_kante !== "number" || e.max_kante % 1 !== 0 || e.max_kante < KANTE_MIN || e.max_kante > KANTE_MAX) fehler("max_kante muss eine ganze Zahl von " + KANTE_MIN + " bis " + KANTE_MAX + " sein.");
        kante = e.max_kante;
    }
    return { url: leer(e.url) ? "" : e.url.trim(), suche: leer(e.suche) ? "" : e.suche.trim(), titel: leer(e.titel) ? "" : e.titel.trim(), kante: kante };
}

function wikipediaBild(suche, breite, seitenId) {
    var r = jarvis.http({
        url: WIKIPEDIA_API + "?action=query&format=json&formatversion=2&redirects=1&prop=pageimages&piprop=thumbnail%7Coriginal&pilicense=any&pithumbsize=" + breite +
            (seitenId ? "&pageids=" + seitenId : "&generator=search&gsrlimit=1&gsrsearch=" + encodeURIComponent(suche)),
        kopf: UA
    });
    if (r.code !== 200) fehler("Wikipedia antwortet nicht wie erwartet (HTTP " + r.code + ").");
    var j;
    try { j = JSON.parse(r.text); } catch (x) { fehler("Wikipedia lieferte keine lesbare Antwort."); }
    var seite = j && j.query && j.query.pages && j.query.pages[0];
    if (!seite) fehler("Zu „" + suche + "“ findet Wikipedia nichts.");
    return seite;
}

// Vorschaugrößen, die Wikimedia vorhält.
var STUFEN = [120, 250, 330, 500, 960];

/** Suche → Hauptfoto des passenden Wikipedia-Artikels. Vorschaubilder statt Originale: Die Originale drosselt Wikimedia für den Bilddienst. */
function suchFoto(suche, kante) {
    var gross = kante <= 330 ? 330 : (kante <= 500 ? 500 : 960);
    var seite = wikipediaBild(suche, gross, 0);
    var t = seite.thumbnail, o = seite.original;
    if (!t || !t.source || !(t.width > 0 && t.height > 0)) fehler("Zu „" + seite.title + "“ hat Wikipedia kein Foto.");
    // Ist das Original kleiner als die Stufe, kommt das Original selbst: die nächstkleinere echte Vorschau derselben Seite holen.
    if (t.source.indexOf("/thumb/") < 0 && o && o.width > STUFEN[0]) {
        var stufe = STUFEN[0];
        for (var i = 0; i < STUFEN.length; i++) if (STUFEN[i] < o.width && STUFEN[i] < gross) stufe = STUFEN[i];
        var nochmal = wikipediaBild(suche, stufe, seite.pageid);
        var klein = nochmal && nochmal.pageid === seite.pageid ? nochmal.thumbnail : null;
        if (klein && klein.source && klein.width > 0 && klein.height > 0) t = klein;
    }
    return { adresse: t.source, breite: t.width, hoehe: t.height, titel: seite.title };
}

function proxyAdresse(adresse, zusatz) { return PROXY + "?url=" + encodeURIComponent(adresse) + zusatz; }

function proxyFehler(r) {
    var grund = "";
    try { grund = JSON.parse(r.text).message || ""; } catch (x) { grund = ""; }
    return "Das Foto ließ sich nicht laden (HTTP " + r.code + (grund ? ", " + String(grund).slice(0, 200) : "") + "). Ist die Adresse ein öffentlich erreichbares Bild?";
}

function proxyMasse(adresse) {
    var r = jarvis.http({ url: proxyAdresse(adresse, "&output=json"), kopf: UA });
    if (r.code !== 200) fehler(proxyFehler(r));
    var j;
    try { j = JSON.parse(r.text); } catch (x) { fehler("Der Bilddienst lieferte keine Bildmaße."); }
    if (!j || !(j.width > 0 && j.height > 0)) fehler("Der Bilddienst lieferte keine Bildmaße.");
    // Die Maße stehen vor der EXIF-Drehung; das Bild selbst kommt gedreht. Bei 5 bis 8 Breite und Höhe tauschen.
    var gedreht = j.orientation >= 5 && j.orientation <= 8;
    var kanaele = j.channels >= 1 && j.channels <= 4 ? j.channels : 4;
    return { breite: gedreht ? j.height : j.width, hoehe: gedreht ? j.width : j.height, bytesJePixel: kanaele * (/^(ushort|short|uint|int|float|double)$/.test(String(j.depth)) ? 2 : 1) };
}

var B64 = (function () {
    var t = {}, a = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    for (var i = 0; i < 64; i++) t[a.charCodeAt(i)] = i;
    return t;
})();

function base64Bytes(s) {
    var aus = [], puffer = 0, bits = 0;
    for (var i = 0; i < s.length; i++) {
        var c = s.charCodeAt(i);
        if (c === 61) break;
        var v = B64[c];
        if (v === undefined) { if (c === 10 || c === 13 || c === 32) continue; fehler("Die Bilddaten sind beschädigt."); }
        puffer = ((puffer << 6) | v) & 0xFFFFFF; bits += 6;
        if (bits >= 8) { bits -= 8; aus.push((puffer >> bits) & 0xFF); }
    }
    return aus;
}

function u32(b, i) { return ((b[i] << 24) >>> 0) + (b[i + 1] << 16) + (b[i + 2] << 8) + b[i + 3]; }

/** PNG mit unkomprimierten Deflate-Blöcken (wsrv.nl mit l=0) → Pixel als RGBA. */
function pngPixel(b) {
    var sig = [137, 80, 78, 71, 13, 10, 26, 10];
    for (var s = 0; s < 8; s++) if (b[s] !== sig[s]) fehler("Der Bilddienst lieferte kein PNG.");
    var i = 8, kopf = null, palette = null, trns = null, idat = [], ende = false;
    while (i + 8 <= b.length) {
        var n = u32(b, i), typ = String.fromCharCode(b[i + 4], b[i + 5], b[i + 6], b[i + 7]), d = i + 8;
        if (d + n + 4 > b.length) break;
        if (typ === "IHDR") kopf = { w: u32(b, d), h: u32(b, d + 4), tiefe: b[d + 8], farbe: b[d + 9], verschachtelt: b[d + 12] };
        else if (typ === "PLTE") palette = b.slice(d, d + n);
        else if (typ === "tRNS") trns = b.slice(d, d + n);
        else if (typ === "IDAT") idat.push([d, n]);
        else if (typ === "IEND") { ende = true; break; }
        i = d + n + 4;
    }
    if (!kopf || !ende) fehler("Die Bilddaten sind unvollständig angekommen.");
    if (kopf.verschachtelt !== 0) fehler("Verschachtelte PNG werden nicht unterstützt.");
    var kanaele = { 0: 1, 2: 3, 3: 1, 4: 2, 6: 4 }[kopf.farbe];
    if (!kanaele || (kopf.tiefe !== 8 && !(kopf.tiefe === 16 && kopf.farbe !== 3))) fehler("Dieses PNG-Format wird nicht unterstützt (Tiefe " + kopf.tiefe + ", Farbtyp " + kopf.farbe + ").");
    if (kopf.farbe === 3 && !palette) fehler("Dem PNG fehlt die Farbpalette.");
    var bpp = kanaele * kopf.tiefe / 8, zeile = kopf.w * bpp, roh = [];
    // zlib-Strom über alle IDAT-Teile hinweg lesen; nur Speicherblöcke (BTYPE 0).
    var teil = 0, pos = idat.length ? idat[0][0] : 0, rest = idat.length ? idat[0][1] : 0;
    function naechstes() {
        while (rest === 0) { teil++; if (teil >= idat.length) fehler("Die Bilddaten sind unvollständig angekommen."); pos = idat[teil][0]; rest = idat[teil][1]; }
        rest--; return b[pos++];
    }
    var cmf = naechstes(), flg = naechstes();
    if ((cmf & 0x0F) !== 8 || ((cmf << 8) | flg) % 31 !== 0 || (flg & 0x20)) fehler("Die Bilddaten sind beschädigt.");
    var erwartet = (zeile + 1) * kopf.h, letzter = false;
    while (!letzter) {
        var bk = naechstes();
        letzter = (bk & 1) === 1;
        if (((bk >> 1) & 3) !== 0) fehler("Der Bilddienst hat das PNG komprimiert; erwartet war unkomprimiert.");
        var len = naechstes() | (naechstes() << 8), nlen = naechstes() | (naechstes() << 8);
        if ((len ^ 0xFFFF) !== nlen) fehler("Die Bilddaten sind beschädigt.");
        if (roh.length + len > erwartet) fehler("Die Bilddaten sind größer als angegeben.");
        while (len > 0) {
            while (rest === 0) { teil++; if (teil >= idat.length) fehler("Die Bilddaten sind unvollständig angekommen."); pos = idat[teil][0]; rest = idat[teil][1]; }
            var stueck = Math.min(len, rest);
            for (var k = 0; k < stueck; k++) roh.push(b[pos + k]);
            pos += stueck; rest -= stueck; len -= stueck;
        }
    }
    if (roh.length !== erwartet) fehler("Die Bilddaten sind unvollständig angekommen.");
    // Zeilenfilter rückgängig machen, an Ort und Stelle.
    var vorher = -1;
    for (var y = 0; y < kopf.h; y++) {
        var start = y * (zeile + 1), f = roh[start], a = start + 1;
        if (f > 4) fehler("Die Bilddaten sind beschädigt.");
        if (f !== 0) for (var x = 0; x < zeile; x++) {
            var links = x >= bpp ? roh[a + x - bpp] : 0, oben = vorher >= 0 ? roh[vorher + x] : 0;
            var lo = x >= bpp && vorher >= 0 ? roh[vorher + x - bpp] : 0, v;
            if (f === 1) v = links;
            else if (f === 2) v = oben;
            else if (f === 3) v = (links + oben) >> 1;
            else { var p = links + oben - lo, pa = Math.abs(p - links), pb = Math.abs(p - oben), pc = Math.abs(p - lo); v = pa <= pb && pa <= pc ? links : (pb <= pc ? oben : lo); }
            roh[a + x] = (roh[a + x] + v) & 0xFF;
        }
        vorher = a;
    }
    return { w: kopf.w, h: kopf.h, roh: roh, zeile: zeile, farbe: kopf.farbe, schritt: kopf.tiefe / 8, palette: palette, trns: trns };
}

function le32(n) { return String.fromCharCode(n & 0xFF, (n >>> 8) & 0xFF, (n >>> 16) & 0xFF, (n >>> 24) & 0xFF); }
function le16(n) { return String.fromCharCode(n & 0xFF, (n >>> 8) & 0xFF); }

/** BMP mit 32 Bit je Pixel und den Masken R 0x7F, G 0x7F00, B 0x7F0000: jedes Byte < 0x80. Transparenz auf Weiß. */
function bmpText(p) {
    var w = p.w, h = p.h;
    if (!bmpPasst(w, h)) fehler("Das Bild hat eine nicht darstellbare Größe (" + w + " × " + h + ").");
    var groesse = w * h * 4;
    var teile = ["BM" + le32(BMP_KOPF + groesse) + le16(0) + le16(0) + le32(BMP_KOPF) +
        le32(40) + le32(w) + le32(h) + le16(1) + le16(32) + le32(3) + le32(groesse) + le32(0) + le32(0) + le32(0) + le32(0) +
        le32(0x7F) + le32(0x7F00) + le32(0x7F0000)];
    var s = p.schritt, roh = p.roh, pal = p.palette, trns = p.trns, codes = new Array(w * 4);
    // tRNS bei Grau und RGB: ein Farbschlüssel aus 16-Bit-Werten (je Kanal Hoch- und Niedrigbyte); dieser Wert wird transparent.
    var schluessel = null, schluesselHoch = null, kn = p.farbe === 2 ? 3 : (p.farbe === 0 ? 1 : 0);
    if (trns && kn && trns.length >= kn * 2) {
        schluessel = []; schluesselHoch = [];
        for (var q = 0; q < kn; q++) { schluesselHoch.push(trns[q * 2]); schluessel.push(trns[q * 2 + 1]); }
    }
    for (var y = h - 1; y >= 0; y--) {
        var a = y * (p.zeile + 1) + 1;
        for (var x = 0; x < w; x++) {
            var r, g, bl, al = 255;
            if (p.farbe === 2) {
                r = roh[a + x * 3 * s]; g = roh[a + (x * 3 + 1) * s]; bl = roh[a + (x * 3 + 2) * s];
                if (schluessel && roh[a + x * 3 * s + s - 1] === schluessel[0] && roh[a + (x * 3 + 1) * s + s - 1] === schluessel[1] && roh[a + (x * 3 + 2) * s + s - 1] === schluessel[2] &&
                    (s === 1 || (r === schluesselHoch[0] && g === schluesselHoch[1] && bl === schluesselHoch[2]))) al = 0;
            }
            else if (p.farbe === 6) { r = roh[a + x * 4 * s]; g = roh[a + (x * 4 + 1) * s]; bl = roh[a + (x * 4 + 2) * s]; al = roh[a + (x * 4 + 3) * s]; }
            else if (p.farbe === 0) {
                r = g = bl = roh[a + x * s];
                if (schluessel && roh[a + x * s + s - 1] === schluessel[0] && (s === 1 || r === schluesselHoch[0])) al = 0;
            }
            else if (p.farbe === 4) { r = g = bl = roh[a + x * 2 * s]; al = roh[a + (x * 2 + 1) * s]; }
            else {
                var ix = roh[a + x];
                if (ix * 3 + 2 >= pal.length) fehler("Die Bilddaten sind beschädigt.");
                r = pal[ix * 3]; g = pal[ix * 3 + 1]; bl = pal[ix * 3 + 2];
                if (trns && ix < trns.length) al = trns[ix];
            }
            if (al < 255) { r = (r * al + 255 * (255 - al)) / 255; g = (g * al + 255 * (255 - al)) / 255; bl = (bl * al + 255 * (255 - al)) / 255; }
            var o = x * 4;
            codes[o] = r >> 1; codes[o + 1] = g >> 1; codes[o + 2] = bl >> 1; codes[o + 3] = 0;
        }
        var reihe = "";
        for (var c = 0; c < codes.length; c += 4096) reihe += String.fromCharCode.apply(null, codes.slice(c, c + 4096));
        teile.push(reihe);
    }
    return teile.join("");
}

function dateiname(titel) {
    var n = String(titel || "Foto").replace(/[^A-Za-z0-9\u00C0-\u00D6\u00D8-\u00F6\u00F8-\u024F _().,-]+/g, " ").replace(/\.{2,}/g, ".").replace(/\s+/g, " ").replace(/^[\s.]+|[\s.]+$/g, "").slice(0, 60);
    return (n || "Foto") + ".bmp";
}

function ausfuehren(eingabe) {
    var e = pruefeEingabe(eingabe);
    var herkunft;
    if (e.suche) herkunft = suchFoto(e.suche, e.kante);
    else {
        var m = proxyMasse(e.url);
        herkunft = { adresse: e.url, breite: m.breite, hoehe: m.hoehe, titel: "", bytesJePixel: m.bytesJePixel };
    }
    var ziel = zielMasse(herkunft.breite, herkunft.hoehe, e.kante, herkunft.bytesJePixel || 4);
    var r = jarvis.http({ url: proxyAdresse(herkunft.adresse, "&w=" + ziel.w + "&h=" + ziel.h + "&fit=cover&output=png&l=0&encoding=base64"), kopf: UA });
    if (r.code !== 200) fehler(proxyFehler(r));
    var text = String(r.text);
    if (text.indexOf("data:image/png;base64,") !== 0) fehler("Der Bilddienst lieferte kein Bild.");
    var pixel = pngPixel(base64Bytes(text.slice(text.indexOf(",") + 1)));
    if (pixel.w !== ziel.w || pixel.h !== ziel.h) fehler("Der Bilddienst lieferte eine andere Größe als angefragt (" + pixel.w + " × " + pixel.h + ").");
    var titel = e.titel || herkunft.titel || "Foto";
    var name = dateiname(titel);
    jarvis.textDatei(name, bmpText(pixel));
    return {
        text: "Foto „" + titel + "“ geladen (" + pixel.w + " × " + pixel.h + " Pixel) und als Bild " + name + " übergeben.",
        daten: { datei: name, breite: pixel.w, hoehe: pixel.h, titel: titel }
    };
}
