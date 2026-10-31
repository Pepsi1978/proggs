// docx-schreiber: erzeugt eine echte Word-Datei aus Titel und Absätzen. Nur Text, keine Bilder und keine Tabellen.

var MAX_ZEICHEN = 1000000;
var MAX_ABSAETZE = 20000;
var MAX_TITEL = 500;
// Umbrüche, Tabs und Maskierungen vervielfachen den Text; die Grenze schützt den Speicher, bevor das Archiv entsteht.
var MAX_XML = 6000000;
var ERLAUBTE_ANGABEN = { titel: true, absaetze: true, dateiname: true };
var W_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
var XML_KOPF = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>\n';
// Aus Zeichencodes gebaut, damit im Quelltext keine unsichtbaren Zeichen oder Escapes stehen. Vertikaler Tab und Seitenvorschub gelten als Zeilenende.
var Z = String.fromCharCode;
var XML_VERBOTEN = new RegExp("[" + Z(0) + "-" + Z(8) + Z(0x0E) + "-" + Z(0x1F) + Z(0xFFFE) + Z(0xFFFF) + "]", "g");
var ZEILENENDE = new RegExp(Z(0x0D, 0x0A) + "|[" + Z(0x0A, 0x0B, 0x0C, 0x0D, 0x85, 0x2028, 0x2029) + "]");
var SURROGATE = new RegExp("[" + Z(0xD800) + "-" + Z(0xDBFF) + "][" + Z(0xDC00) + "-" + Z(0xDFFF) + "]|[" + Z(0xD800) + "-" + Z(0xDFFF) + "]", "g");
var XML_MASKE = /[&<>]/;
var NICHT_LEER = new RegExp("[^\\s" + Z(0x85) + "]");

// Zeichen, die in XML 1.0 verboten sind, würden die Datei unlesbar machen. Tab und Zeilenumbrüche bleiben.
function saeubern(text) {
  // Gültige Paare (Emoji) bleiben, einzelne Surrogate entfallen; gültiger Text kostet so keine Schleife je Zeichen.
  return text.replace(XML_VERBOTEN, "").replace(SURROGATE, function (treffer) { return treffer.length === 2 ? treffer : ""; });
}

function xmlText(text) {
  if (!XML_MASKE.test(text)) return text;
  return text.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}

function textStueck(text) {
  return text === "" ? "" : '<w:t xml:space="preserve">' + xmlText(text) + "</w:t>";
}

// Zeilenumbrüche und Tabs sind in Word eigene Elemente, im Textknoten gingen sie verloren.
function lauf(text) {
  var zeilen = text.split(ZEILENENDE);
  var teile = [];
  for (var i = 0; i < zeilen.length; i++) {
    if (i > 0) teile.push("<w:br/>");
    var spalten = zeilen[i].split("\t");
    for (var j = 0; j < spalten.length; j++) {
      if (j > 0) teile.push("<w:tab/>");
      teile.push(textStueck(spalten[j]));
    }
  }
  return "<w:r>" + teile.join("") + "</w:r>";
}

function dokumentRumpf(titel, absaetze) {
  var teile = ['<w:p><w:pPr><w:pStyle w:val="Heading1"/></w:pPr>' + lauf(titel) + "</w:p>"];
  for (var i = 0; i < absaetze.length; i++) teile.push("<w:p>" + lauf(absaetze[i]) + "</w:p>");
  return teile.join("");
}

function archivInhalt(rumpf) {
  return {
    "[Content_Types].xml": XML_KOPF +
      '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">' +
      '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>' +
      '<Default Extension="xml" ContentType="application/xml"/>' +
      '<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>' +
      '<Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>' +
      '<Override PartName="/word/settings.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.settings+xml"/>' +
      "</Types>",
    "_rels/.rels": XML_KOPF +
      '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">' +
      '<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>' +
      "</Relationships>",
    "word/_rels/document.xml.rels": XML_KOPF +
      '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">' +
      '<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>' +
      '<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/settings" Target="settings.xml"/>' +
      "</Relationships>",
    "word/document.xml": XML_KOPF +
      '<w:document xmlns:w="' + W_NS + '"><w:body>' + rumpf +
      '<w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1417" w:right="1417" w:bottom="1134" w:left="1417" w:header="708" w:footer="708" w:gutter="0"/></w:sectPr>' +
      "</w:body></w:document>",
    "word/styles.xml": XML_KOPF +
      '<w:styles xmlns:w="' + W_NS + '">' +
      '<w:docDefaults><w:rPrDefault><w:rPr><w:rFonts w:ascii="Calibri" w:hAnsi="Calibri" w:eastAsia="Calibri" w:cs="Calibri"/><w:sz w:val="22"/><w:szCs w:val="22"/><w:lang w:val="de-DE"/></w:rPr></w:rPrDefault>' +
      '<w:pPrDefault><w:pPr><w:spacing w:after="160" w:line="259" w:lineRule="auto"/></w:pPr></w:pPrDefault></w:docDefaults>' +
      '<w:style w:type="paragraph" w:default="1" w:styleId="Normal"><w:name w:val="Normal"/><w:qFormat/></w:style>' +
      '<w:style w:type="paragraph" w:styleId="Heading1"><w:name w:val="heading 1"/><w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:qFormat/>' +
      '<w:pPr><w:keepNext/><w:spacing w:before="240" w:after="120"/><w:outlineLvl w:val="0"/></w:pPr><w:rPr><w:b/><w:bCs/><w:sz w:val="32"/><w:szCs w:val="32"/></w:rPr></w:style>' +
      "</w:styles>",
    "word/settings.xml": XML_KOPF +
      '<w:settings xmlns:w="' + W_NS + '"><w:compat><w:compatSetting w:name="compatibilityMode" w:uri="http://schemas.microsoft.com/office/word" w:val="15"/></w:compat></w:settings>'
  };
}

function abschneiden(text, laenge) {
  if (text.length <= laenge) return text;
  var kurz = text.substring(0, laenge);
  var c = kurz.charCodeAt(kurz.length - 1);
  return c >= 0xD800 && c <= 0xDBFF ? kurz.substring(0, kurz.length - 1) : kurz;
}

// Pfadzeichen, Steuerzeichen und unsichtbare Format- und Schreibrichtungszeichen (täuschen sonst andere Namen oder Endungen vor).
function imNamenVerboten(zeichen) {
  var c = zeichen.charCodeAt(0);
  return '\\/:*?"<>|'.indexOf(zeichen) >= 0 || c < 0x20 || (c >= 0x7F && c <= 0x9F) || c === 0xAD || c === 0x61C ||
    (c >= 0x200B && c <= 0x200F) || (c >= 0x2028 && c <= 0x202E) || (c >= 0x2060 && c <= 0x206F) || c === 0xFEFF;
}

// Verbotene Zeichen werden zu Leerzeichen, damit etwa „3/4“ und „34“ nicht denselben Namen ergeben.
function dateinameAusTitel(titel) {
  var teile = [];
  for (var i = 0; i < titel.length; i++) teile.push(imNamenVerboten(titel.charAt(i)) ? " " : titel.charAt(i));
  var stamm = teile.join("").replace(/\.docx\s*$/i, "").replace(/\s+/g, " ").replace(/\.{2,}/g, ".").replace(/^[\s.]+/, "");
  stamm = abschneiden(stamm, 60).replace(/[\s.]+$/, "");
  return (stamm === "" ? "dokument" : stamm) + ".docx";
}

function dateinamePruefen(wert) {
  if (typeof wert !== "string" || wert.trim() === "") throw new Error("Der Dateiname muss ein nicht leerer Text sein.");
  var name = wert.trim();
  var ungueltig = name.indexOf("..") >= 0 || name.charAt(0) === "." || name.charAt(name.length - 1) === "." || saeubern(name) !== name;
  for (var i = 0; i < name.length && !ungueltig; i++) ungueltig = imNamenVerboten(name.charAt(i));
  if (ungueltig) throw new Error("Der Dateiname darf keine Pfadangaben oder Sonderzeichen enthalten und nicht mit einem Punkt beginnen oder enden.");
  if (!/\.docx$/i.test(name)) name += ".docx";
  // Dateisysteme begrenzen Bytes, nicht Zeichen. Der Name ist hier frei von einzelnen Surrogaten.
  if (encodeURIComponent(name).replace(/%[0-9A-F]{2}/g, "x").length > 200) throw new Error("Der Dateiname ist zu lang.");
  return name;
}

function zuLang() {
  return new Error("Der Text ist zu lang, höchstens " + MAX_ZEICHEN + " Zeichen.");
}

// Leerer Inhalt zählt nicht als Absatz. Die Summe läuft mit, damit Übergroßes sofort scheitert statt erst an der Zeitgrenze.
function absaetzeLesen(wert, schonVerbraucht) {
  if (wert === undefined || wert === null) return [];
  if (!Array.isArray(wert)) throw new Error("absaetze muss eine Liste von Texten sein.");
  if (wert.length > MAX_ABSAETZE) throw new Error("Zu viele Absätze, höchstens " + MAX_ABSAETZE + ".");
  var ergebnis = [];
  var summe = schonVerbraucht;
  for (var i = 0; i < wert.length; i++) {
    if (typeof wert[i] !== "string") throw new Error("Absatz " + (i + 1) + " ist kein Text.");
    summe += wert[i].length;
    if (summe > MAX_ZEICHEN) throw zuLang();
    var absatz = saeubern(wert[i]);
    if (NICHT_LEER.test(absatz)) ergebnis.push(absatz);
  }
  return ergebnis;
}

function ausfuehren(eingabe) {
  var e = eingabe || {};
  // Ein Tippfehler wie "absatz" würde sonst still ein Dokument ohne den Text ergeben.
  for (var schluessel in e) {
    if (!Object.prototype.hasOwnProperty.call(ERLAUBTE_ANGABEN, schluessel)) throw new Error("Unbekannte Angabe: " + abschneiden(schluessel, 40) + ". Erlaubt sind titel, absaetze und dateiname.");
  }
  if (typeof e.titel !== "string") throw new Error("Der Titel fehlt.");
  if (e.titel.length > MAX_ZEICHEN) throw zuLang();
  var titel = saeubern(e.titel).split(ZEILENENDE).join(" ").replace(/\s+/g, " ").trim();
  if (titel === "") throw new Error("Der Titel fehlt.");
  if (titel.length > MAX_TITEL) throw new Error("Der Titel ist zu lang, höchstens " + MAX_TITEL + " Zeichen.");

  var absaetze = absaetzeLesen(e.absaetze, titel.length);
  var name = e.dateiname === undefined || e.dateiname === null ? dateinameAusTitel(titel) : dateinamePruefen(e.dateiname);
  var rumpf = dokumentRumpf(titel, absaetze);
  if (rumpf.length > MAX_XML) throw new Error("Der Text enthält zu viele Zeilenumbrüche, Tabs oder Sonderzeichen für ein Dokument.");
  var inhalt = archivInhalt(rumpf);
  var pfad = jarvis.zipDatei(name, inhalt);

  return {
    text: name + " erstellt: Überschrift „" + abschneiden(titel, 80) + "“, " + absaetze.length + (absaetze.length === 1 ? " Absatz." : " Absätze."),
    // teile und xml_auszug belegen in der Prüfung auf dem Gerät, was wirklich ins Archiv ging.
    daten: { datei: name, absaetze: absaetze.length, teile: Object.keys(inhalt), xml_auszug: abschneiden(rumpf, 400) },
    dateien: [{ name: name, pfad: pfad }]
  };
}
