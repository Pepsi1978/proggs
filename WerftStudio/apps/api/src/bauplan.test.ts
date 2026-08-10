import { describe, expect, it } from "vitest";
import { anordnungAus, baumAus, layoutFuer, layoutFuerKette } from "./bauplan.js";
import { breitenRegelnFuer } from "./export-package.js";

/**
 * Der Fall, der die ganze Kette verdorben hat — hier festgenagelt.
 *
 * Eine Feldzeile ist zweispaltig definiert (Beschriftung NEBEN dem Feld) und wird in einer
 * Media Query fuer schmale Fenster auf einspaltig umgestellt (Beschriftung UEBER dem Feld). Im
 * Geraeterahmen des Studios greift die Media Query, im breiten Browserfenster nicht. Wer die
 * Datei oeffnet oder vermisst, sieht deshalb die falsche Anordnung — und genau die landete im Code.
 */
const css = `
.feld { min-height: 48px; display: grid; grid-template-columns: 156px minmax(0, 1fr); align-items: center; column-gap: 12px; }
@media (max-width: 480px) {
  .feld { grid-template-columns: minmax(0, 1fr); align-items: start; row-gap: 6px; }
}
@media (min-width: 900px) {
  .feld { grid-template-columns: 240px minmax(0, 1fr); }
}
@media (prefers-color-scheme: dark) {
  .feld { background: #201B17; }
}
`;

describe("breitenRegelnFuer", () => {
  it("nimmt die Regel fuer schmale Fenster mit, wenn die Zielbreite darunter liegt", () => {
    const aufgeloest = breitenRegelnFuer(css, 412);
    expect(aufgeloest).toContain("grid-template-columns: minmax(0, 1fr)");
    expect(aufgeloest).toContain("row-gap: 6px");
  });

  it("laesst Regeln fuer breite Fenster weg, wenn die Zielbreite darunter liegt", () => {
    expect(breitenRegelnFuer(css, 412)).not.toContain("240px");
  });

  it("nimmt bei grosser Zielbreite die Regel fuer breite Fenster", () => {
    const aufgeloest = breitenRegelnFuer(css, 1200);
    expect(aufgeloest).toContain("240px");
    expect(aufgeloest).not.toContain("row-gap: 6px");
  });

  it("laesst Bedingungen unangetastet, die nicht an der Breite haengen", () => {
    // `prefers-color-scheme` muss beim Betrachter weiter geprueft werden — nicht aufloesen.
    expect(breitenRegelnFuer(css, 412)).not.toContain("#201B17");
  });

  it("behaelt eine zusaetzliche Bedingung neben der Breite", () => {
    const gemischt = "@media (max-width: 480px) and (hover: hover) { .a { color: red; } }";
    const aufgeloest = breitenRegelnFuer(gemischt, 412);
    expect(aufgeloest).toContain("(hover: hover)");
    expect(aufgeloest).toContain("color: red");
  });

  it("rechnet em-Grenzen gegen die Grundschrift von 16px", () => {
    const inEm = "@media (max-width: 30em) { .a { color: red; } }"; // 30em = 480px
    expect(breitenRegelnFuer(inEm, 412)).toContain("color: red");
    expect(breitenRegelnFuer(inEm, 600)).not.toContain("color: red");
  });
});

describe("layoutFuer und anordnungAus", () => {
  it("erkennt bei Zielbreite 412 eine Spalte — die Beschriftung steht UEBER dem Feld", () => {
    const fuerBreite = `${css}\n${breitenRegelnFuer(css, 412)}`;
    const anordnung = anordnungAus(layoutFuer(fuerBreite, ["feld"]));
    expect(anordnung?.art).toBe("spalte");
    expect(anordnung?.abstand).toBe("6px");
  });

  it("erkennt ohne die aufgeloeste Fassung faelschlich ein Raster — der alte Fehler", () => {
    const anordnung = anordnungAus(layoutFuer(css, ["feld"]));
    expect(anordnung?.art).toBe("raster");
  });

  it("erkennt eine Zeile bei flex-direction: row", () => {
    const zeile = ".z { display: flex; flex-direction: row; gap: 8px; }";
    expect(anordnungAus(layoutFuer(zeile, ["z"]))?.art).toBe("zeile");
  });

  it("zaehlt ein Raster mit einer Spalte als Spalte", () => {
    const eins = ".e { display: grid; grid-template-columns: minmax(0, 1fr); }";
    expect(anordnungAus(layoutFuer(eins, ["e"]))?.art).toBe("spalte");
  });
});

describe("baumAus", () => {
  const html = `
<section class="screen">
  <label class="feld">
    <span class="label">Modell</span>
    <span class="shell">
      <select class="select" data-werft-funktion="F-22" aria-label="Modell fuer Experimente">
        <option>GPT 5.6 Sol</option>
        <option selected>GPT 5.6 Terra</option>
      </select>
    </span>
  </label>
  <input class="regler" type="range" min="0.7" max="1.3" step="0.05" value="1">
  <div class="fehler" hidden>Da war nichts zu hoeren.</div>
  <a class="weg" data-werft-navigate="B-09">Selbstbild</a>
</section>`;

  it("gibt die Reihenfolge des Entwurfs wieder", () => {
    const baum = baumAus(html, css);
    const feld = baum[0]?.kinder?.[0];
    expect(feld?.klassen).toBe("feld");
    expect(feld?.kinder?.[0]?.text).toBe("Modell");
    expect(feld?.kinder?.[1]?.klassen).toBe("shell");
  });

  it("bringt die Eintraege einer Auswahlliste mit — sonst wird ein anderes Bauteil daraus", () => {
    const auswahl = baumAus(html, css)[0]?.kinder?.[0]?.kinder?.[1]?.kinder?.[0];
    expect(auswahl?.tag).toBe("select");
    expect(auswahl?.eintraege).toEqual(["GPT 5.6 Sol", "GPT 5.6 Terra"]);
    expect(auswahl?.funktion).toBe("F-22");
    expect(auswahl?.beschriftung).toBe("Modell fuer Experimente");
  });

  it("gibt einen Schieberegler mit seinen Grenzen weiter, damit kein Knopfsatz daraus wird", () => {
    const regler = baumAus(html, css)[0]?.kinder?.find((k) => k.klassen === "regler");
    expect(regler?.bereich).toEqual({ von: "0.7", bis: "1.3", schritt: "0.05", wert: "1" });
  });

  it("behaelt verstecktes als Zustand, nicht als Wegfall", () => {
    const fehler = baumAus(html, css)[0]?.kinder?.find((k) => k.klassen === "fehler");
    expect(fehler?.versteckt).toBe(true);
    expect(fehler?.text).toBe("Da war nichts zu hoeren.");
  });

  it("haelt das Navigationsziel fest", () => {
    const weg = baumAus(html, css)[0]?.kinder?.find((k) => k.klassen === "weg");
    expect(weg?.fuehrtZu).toBe("B-09");
  });
});

/**
 * Die Faelle, an denen der Bauplan bisher STILL Layout verloren hat.
 *
 * Gemessen an einem echten Werft-Export: 42 von 371 Layout-Angaben (11,3 %) fielen heraus, weil
 * `layoutFuer` jeden Selektor mit `[`, `>` oder `:` verworfen hat — und genau die sind die
 * spezifischeren, die im Browser gewinnen. Werft Studio schreibt seine Bildschirm-Regeln selbst so.
 *
 * Bei einem Uebergabepaket von Claude Design ist es noch deutlicher: dort steht das Layout in
 * `style`-Attributen (nachgezaehlt an einem echten Buendel: 234 `style`-Attribute, keine einzige
 * CSS-Klasse). Ohne Inline-Stile waere der Bauplan dort vollstaendig leer.
 */
describe("layoutFuerKette — die Schichten, die im Browser gewinnen", () => {
  const gescopt = `
.karte { display: block; }
.werft-screen[data-screen-id="B-08"] .karte { display: flex; flex-direction: column; gap: 12px; padding: 20px; }
nav.leiste { display: flex; justify-content: space-between; }
.a > .b { display: grid; grid-template-columns: 1fr 1fr; }
.c:hover { display: none; }
`;

  it("liest eine Regel, die ueber einen Attribut-Selektor am Vorfahren haengt", () => {
    const layout = layoutFuerKette(gescopt, [
      { tag: "section", klassen: ["werft-screen"], attribute: { class: "werft-screen", "data-screen-id": "B-08" } },
      { tag: "div", klassen: ["karte"], attribute: { class: "karte" } }
    ]);
    expect(layout["display"]).toBe("flex");
    expect(layout["gap"]).toBe("12px");
    expect(layout["padding"]).toBe("20px");
  });

  it("laesst die schwaechere Grundschicht gewinnen, wenn der Vorfahre NICHT passt", () => {
    const layout = layoutFuerKette(gescopt, [
      { tag: "section", klassen: ["werft-screen"], attribute: { class: "werft-screen", "data-screen-id": "B-01" } },
      { tag: "div", klassen: ["karte"], attribute: { class: "karte" } }
    ]);
    expect(layout["display"]).toBe("block");
    expect(layout["gap"]).toBeUndefined();
  });

  it("liest einen Selektor mit Tag-Anteil", () => {
    const layout = layoutFuerKette(gescopt, [{ tag: "nav", klassen: ["leiste"], attribute: { class: "leiste" } }]);
    expect(layout["justify-content"]).toBe("space-between");
  });

  it("achtet beim Kind-Kombinator auf die direkte Elternschaft", () => {
    const kette = (tags: string[]) => tags.map((klasse) => ({ tag: "div", klassen: [klasse], attribute: { class: klasse } }));
    expect(layoutFuerKette(gescopt, kette(["a", "b"]))["display"]).toBe("grid");
    // `.a > .b` darf NICHT greifen, wenn zwischen a und b noch ein Element steht.
    expect(layoutFuerKette(gescopt, kette(["a", "x", "b"]))["display"]).toBeUndefined();
  });

  it("laesst Zustands-Regeln aus dem Grundaufbau heraus", () => {
    expect(layoutFuerKette(gescopt, [{ tag: "div", klassen: ["c"], attribute: { class: "c" } }])["display"]).toBeUndefined();
  });

  it("laesst den Inline-Stil ueber jede Regel aus dem Stylesheet gewinnen", () => {
    const layout = layoutFuerKette(gescopt, [
      { tag: "div", klassen: ["karte"], attribute: { class: "karte", style: "display:flex;flex-direction:row;gap:8px" } }
    ]);
    expect(layout["display"]).toBe("flex");
    expect(layout["flex-direction"]).toBe("row");
    expect(layout["gap"]).toBe("8px");
  });

  it("respektiert !important gegen eine spaetere Regel", () => {
    const css = `.x { gap: 4px !important; }\n.y.x { gap: 99px; }`;
    const layout = layoutFuerKette(css, [{ tag: "div", klassen: ["x", "y"], attribute: { class: "x y" } }]);
    expect(layout["gap"]).toBe("4px");
  });
});

/**
 * Ein Uebergabepaket von Claude Design traegt sein Layout inline. Genau dieser Fall — nachgebaut
 * aus einer echten `.dc.html` — muss im Bauplan ankommen, sonst ist er dort wertlos.
 */
describe("baumAus — Layout aus einem Claude-Design-artigen Paket", () => {
  it("leitet die Anordnung aus dem style-Attribut ab, auch ohne jede CSS-Klasse", () => {
    const inline = `<div style="display:flex;flex-direction:column;align-items:center;gap:16px;padding:28px 20px 60px;">
<span style="width:10px;height:10px;">Punkt</span>
</div>`;
    const baum = baumAus(inline, "");
    expect(baum[0]?.anordnung).toEqual({ art: "spalte", abstand: "16px", quer: "center", innen: "28px 20px 60px" });
  });
});

describe("baumAus — jeder Container sagt, wie er anordnet", () => {
  it("gibt einem Container ohne flex/grid ausdruecklich 'fluss', statt das Feld weglassen", () => {
    // Ohne diese Angabe muesste der Umsetzer raten, ob die Kinder untereinander stehen.
    const baum = baumAus("<main><h1>A</h1><p>B</p><p>C</p></main>", "");
    expect(baum[0]?.anordnung).toEqual({ art: "fluss" });
  });

  it("laesst Blatt-Elemente ohne Anordnung — sie ordnen nichts an", () => {
    const baum = baumAus("<main><h1>A</h1></main>", "");
    expect(baum[0]?.kinder?.[0]?.tag).toBe("h1");
    expect(baum[0]?.kinder?.[0]?.anordnung).toBeUndefined();
  });

  it("ueberschreibt eine echte Anordnung NICHT mit 'fluss'", () => {
    const baum = baumAus('<div class="k"><span>A</span></div>', ".k{display:flex;gap:8px}");
    expect(baum[0]?.anordnung?.art).toBe("zeile");
    expect(baum[0]?.anordnung?.abstand).toBe("8px");
  });
});
