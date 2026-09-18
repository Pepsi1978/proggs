/* Steuerung aller vier Entwürfe ausschließlich über URL-Parameter.
   Unter file:// ist Skriptzugriff über iframe-Grenzen gesperrt, deshalb kein postMessage:
   ?seite=start|editor|einstellungen|alarm   ?theme=hell|dunkel   ?breite=412|360|840

   Die Entwürfe zeigen eine FESTE Momentaufnahme (Freitag, 18.09.2026, 22:41) — bewusst keine
   lebende Uhr, damit Uhrzeit, Datum, Restzeit und Weckerkarten nicht auseinanderlaufen.
   Es wird nichts geplant, gespeichert oder ausgelöst; alle Knöpfe sind reine Darstellung. */
(function () {
  "use strict";

  var p = new URLSearchParams(location.search);
  var seite = p.get("seite") || "start";
  var theme = p.get("theme") === "hell" ? "hell" : "dunkel";
  var breite = p.get("breite") || "412";
  var eingebettet = p.get("eingebettet") === "1";

  document.documentElement.setAttribute("data-theme", theme);
  document.documentElement.setAttribute("data-breite", breite);
  document.body.setAttribute("data-eingebettet", eingebettet ? "true" : "false");

  function zeige(name) {
    var gefunden = false;
    document.querySelectorAll("[data-seite]").forEach(function (el) {
      var treffer = el.getAttribute("data-seite") === name;
      el.classList.toggle("aktiv", treffer);
      if (treffer) gefunden = true;
    });
    if (!gefunden) {
      var erste = document.querySelector("[data-seite]");
      if (erste) erste.classList.add("aktiv");
    }
    document.querySelectorAll(".demoleiste [data-zu-seite]").forEach(function (b) {
      b.setAttribute("aria-current", b.getAttribute("data-zu-seite") === name ? "page" : "false");
    });
    aktuelleSeite = name;
  }
  var aktuelleSeite = seite;
  zeige(seite);

  function setze(schluessel, wert) {
    var q = new URLSearchParams(location.search);
    q.set(schluessel, wert);
    location.search = q.toString();
  }

  /* Innerhalb der Vorschau wird ohne Neuladen gewechselt, damit auch im eingebetteten
     Vergleich ein Tippen auf eine Karte sofort die passende Seite zeigt. */
  document.querySelectorAll("[data-zu-seite]").forEach(function (b) {
    b.addEventListener("click", function () {
      var ziel = b.getAttribute("data-zu-seite");
      if (b.closest(".demoleiste")) setze("seite", ziel); else zeige(ziel);
    });
  });
  document.querySelectorAll("[data-zu-theme]").forEach(function (b) {
    b.addEventListener("click", function () { setze("theme", b.getAttribute("data-zu-theme")); });
  });

  /* Kurze Rückmeldung statt stiller Knöpfe — ausdrücklich als Vorschau gekennzeichnet. */
  var melder = document.createElement("div");
  melder.className = "demomeldung";
  melder.setAttribute("role", "status");
  melder.setAttribute("aria-live", "polite");
  document.body.appendChild(melder);
  var melderTimer = null;
  function melde(text) {
    melder.textContent = text;
    melder.classList.add("sichtbar");
    if (melderTimer) clearTimeout(melderTimer);
    melderTimer = setTimeout(function () { melder.classList.remove("sichtbar"); }, 2600);
  }
  document.querySelectorAll("[data-demo]").forEach(function (b) {
    b.addEventListener("click", function () { melde(b.getAttribute("data-demo")); });
  });

  /* Fehlende Bildassets als Platzhalter markieren statt ein kaputtes Bild zu zeigen. */
  document.querySelectorAll(".asset img").forEach(function (bild) {
    function leer() {
      bild.style.display = "none";
      bild.parentElement.setAttribute("data-leer", "true");
    }
    bild.addEventListener("error", leer);
    if (bild.complete && bild.naturalWidth === 0) leer();
  });

  /* Schalter, Regler und Auswahlreihen sind reine Darstellung. */
  document.querySelectorAll("[data-schalter]").forEach(function (s) {
    s.addEventListener("click", function () {
      var an = s.getAttribute("aria-checked") === "true";
      s.setAttribute("aria-checked", an ? "false" : "true");
      var name = s.getAttribute("aria-label") || "Dieser Schalter";
      melde("Vorschau: " + name + (an ? " ausgeschaltet." : " eingeschaltet.") + " Es wird nichts geplant.");
    });
  });
  document.querySelectorAll("[data-regler]").forEach(function (r) {
    var ziel = document.getElementById(r.getAttribute("data-regler"));
    function anzeigen() {
      if (!ziel) return;
      var v = parseInt(r.value, 10);
      ziel.textContent = v === 0 ? "Vorlauf: zur Schlafenszeit" : "Vorlauf: " + v + " Min. vorher";
    }
    r.addEventListener("input", anzeigen);
    r.addEventListener("change", function () {
      var v = parseInt(r.value, 10);
      melde("Vorschau: " + (v === 0 ? "Erinnerung genau zur Schlafenszeit." : "Erinnerung " + v + " Min. vorher.") + " Nichts gespeichert.");
    });
    anzeigen();
  });
  document.querySelectorAll("[data-wahl] button").forEach(function (b) {
    b.addEventListener("click", function () {
      b.parentElement.querySelectorAll("button").forEach(function (x) { x.setAttribute("aria-pressed", "false"); });
      b.setAttribute("aria-pressed", "true");
    });
  });
})();
