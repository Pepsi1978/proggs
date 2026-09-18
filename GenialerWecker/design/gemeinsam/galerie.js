/* Baut die Vergleichsgalerie: je Variante hell und dunkel nebeneinander.
   Steuerung ausschließlich über die iframe-Adresse — unter file:// ist Skriptzugriff
   über iframe-Grenzen gesperrt, deshalb kein postMessage. */
(function () {
  "use strict";

  var VARIANTEN = [
    {
      id: "a-nachtatelier",
      titel: "A · Nachtatelier",
      text: "Redaktionell, einspaltig. Serifen-Display, Haarlinien, viel Weißraum. Marineblau/Elfenbein mit Kupfer."
    },
    {
      id: "b-morgenruhe",
      titel: "B · Morgenruhe",
      text: "Der Tag als vertikaler Ablauf an einer Achse; Karten sind Stationen. Salbei/Creme, Tannengrün, Terrakotta."
    },
    {
      id: "c-orbit",
      titel: "C · Orbit",
      text: "Instrumententafel im festen Raster, Ring-Countdown, Monospace-Zahlen, dichte Weckerzeilen. Eisblau/Limette."
    },
    {
      id: "d-traumraum",
      titel: "D · Traumraum",
      text: "Skulptural: geschwungene Kuppel, runde Perle, Editor als Bottom-Sheet. Pflaume/Rosé/Perlmutt."
    }
  ];

  var zustand = { seite: "start", breite: "412" };
  var galerie = document.getElementById("galerie");

  var SPALTE_MAX = 620;   // so breit darf eine Vorschauspalte in der Galerie werden
  var RAHMEN_HOEHE = 720;  // sichtbare Höhe einer Vorschauspalte

  /* Der iframe wird IMMER in seiner echten Breite gerendert, damit das responsive CSS
     wirklich diese Breite sieht. Ist sie größer als die Spalte, wird das fertige Bild
     rein optisch verkleinert — der Viewport im iframe bleibt dabei unverändert. */
  function massstab() {
    var echt = parseInt(zustand.breite, 10);
    var skala = echt > SPALTE_MAX ? SPALTE_MAX / echt : 1;
    return { echt: echt, skala: skala, spalte: Math.round(echt * skala) };
  }

  function adresse(id, theme) {
    return "varianten/" + id + "/index.html?seite=" + zustand.seite +
      "&theme=" + theme + "&breite=" + zustand.breite + "&eingebettet=1";
  }

  function zeichnen() {
    galerie.innerHTML = "";
    var mass = massstab();
    VARIANTEN.forEach(function (v) {
      var karte = document.createElement("section");
      karte.className = "variante";

      var kopf = document.createElement("div");
      kopf.className = "titelzeile";
      var links = document.createElement("div");
      var h2 = document.createElement("h2");
      h2.textContent = v.titel;
      var p = document.createElement("p");
      p.className = "beschreibung";
      p.textContent = v.text;
      links.appendChild(h2);
      links.appendChild(p);
      var link = document.createElement("a");
      link.className = "oeffnen";
      link.href = adresse(v.id, "dunkel").replace("&eingebettet=1", "");
      link.target = "_blank";
      link.rel = "noopener";
      link.textContent = "Einzeln öffnen ↗";
      kopf.appendChild(links);
      kopf.appendChild(link);
      karte.appendChild(kopf);

      var paar = document.createElement("div");
      paar.className = "paar";
      [["dunkel", "Dunkel"], ["hell", "Hell"]].forEach(function (t) {
        var spalte = document.createElement("div");
        spalte.className = "seite";
        var etikett = document.createElement("p");
        etikett.className = "etikett";
        etikett.textContent = t[1] + " · " + mass.echt + " px" +
          (mass.skala < 1 ? " (auf " + Math.round(mass.skala * 100) + " % verkleinert)" : "");
        var rahmen = document.createElement("div");
        rahmen.className = "rahmen";
        rahmen.style.width = mass.spalte + "px";
        rahmen.style.height = RAHMEN_HOEHE + "px";
        var frame = document.createElement("iframe");
        frame.src = adresse(v.id, t[0]);
        frame.title = v.titel + " – " + t[1] + " – " + mass.echt + " px breit";
        frame.loading = "lazy";
        // Echte Breite im iframe, Höhe so gewählt, dass nach dem Verkleinern die Spaltenhöhe stimmt.
        frame.style.width = mass.echt + "px";
        frame.style.height = Math.round(RAHMEN_HOEHE / mass.skala) + "px";
        if (mass.skala < 1) {
          frame.style.transformOrigin = "top left";
          frame.style.transform = "scale(" + mass.skala + ")";
        }
        rahmen.appendChild(frame);
        spalte.appendChild(etikett);
        spalte.appendChild(rahmen);
        paar.appendChild(spalte);
      });
      karte.appendChild(paar);
      galerie.appendChild(karte);
    });
  }

  document.querySelectorAll(".wahl").forEach(function (gruppe) {
    gruppe.addEventListener("click", function (e) {
      var knopf = e.target.closest("button");
      if (!knopf) return;
      gruppe.querySelectorAll("button").forEach(function (b) { b.classList.remove("aktiv"); });
      knopf.classList.add("aktiv");
      zustand[gruppe.getAttribute("data-gruppe")] = knopf.getAttribute("data-wert");
      zeichnen();
    });
  });

  zeichnen();
})();
