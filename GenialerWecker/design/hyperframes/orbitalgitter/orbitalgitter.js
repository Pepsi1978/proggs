// Orbitalgitter — der bewegte Hintergrund des Designs „Orbit“ im GenialerWecker.
//
// Eine Instrumententafel im Matrix-Grün: Unten fließt ein perspektivisches Gitter auf den
// Betrachter zu, oben kreisen Satelliten auf geneigten Bahnen um einen glimmenden Planeten,
// ein Radarstrahl dreht seine Runde, und eine Abtastlinie fährt einmal pro Schleife durchs Bild.
// Den Zeichenregen zeichnet die App selbst darüber. Alles periodisch in p ∈ [0, 1).

(function () {
  const TAU = Math.PI * 2;

  function zufall(saat) {
    let s = saat >>> 0;
    return function () {
      s = (s + 0x6d2b79f5) >>> 0;
      let t = s;
      t = Math.imul(t ^ (t >>> 15), t | 1);
      t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
      return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
    };
  }

  function rgba(hex, a) {
    const n = parseInt(hex.slice(1), 16);
    return `rgba(${(n >> 16) & 255},${(n >> 8) & 255},${n & 255},${Math.max(0, Math.min(1, a))})`;
  }

  window.orbitalgitter = function (cfg) {
    const leinwand = document.getElementById(cfg.leinwandId);
    const ctx = leinwand.getContext("2d");
    const W = leinwand.width;
    const H = leinwand.height;
    const rnd = zufall(cfg.saat || 1999);

    const mitte = { x: W * 0.5, y: H * 0.3 };
    const bahnen = [
      { rx: W * 0.26, neigung: 0.32, kipp: -0.18, umlaeufe: 2, satelliten: 2 },
      { rx: W * 0.38, neigung: 0.26, kipp: 0.22, umlaeufe: 1, satelliten: 3 },
      { rx: W * 0.5, neigung: 0.2, kipp: -0.08, umlaeufe: -1, satelliten: 2 },
    ];
    const versatz = bahnen.map((b) => Array.from({ length: b.satelliten }, () => rnd()));

    // Datenpunkte auf dem Gitter, die nacheinander aufleuchten.
    const punkte = Array.from({ length: cfg.punktAnzahl }, () => ({
      spalte: Math.floor(rnd() * 13) - 6,
      reihe: Math.floor(rnd() * 8),
      phase: rnd(),
    }));

    function schein(x, y, r, farbe, a) {
      const g = ctx.createRadialGradient(x, y, 0, x, y, r);
      g.addColorStop(0, rgba(farbe, a));
      g.addColorStop(1, rgba(farbe, 0));
      ctx.fillStyle = g;
      ctx.fillRect(x - r, y - r, r * 2, r * 2);
    }

    function bahnPunkt(b, winkel) {
      const x0 = Math.cos(winkel) * b.rx;
      const y0 = Math.sin(winkel) * b.rx * b.neigung;
      return {
        x: mitte.x + x0 * Math.cos(b.kipp) - y0 * Math.sin(b.kipp),
        y: mitte.y + x0 * Math.sin(b.kipp) + y0 * Math.cos(b.kipp),
        vorn: Math.sin(winkel) > 0,
      };
    }

    function gitter(p) {
      const horizont = H * 0.6;
      const fx = W * 0.5;
      ctx.lineWidth = 1;
      // Waagerechte Linien laufen auf den Betrachter zu: z wandert in ganzen Linienabständen.
      const linien = 14;
      for (let i = 0; i < linien; i++) {
        const z = ((i + p * cfg.gitterLauf) % linien) / linien;
        const tiefe = Math.pow(z, 2.2);
        const y = horizont + (H - horizont) * tiefe;
        ctx.strokeStyle = rgba(cfg.linie, cfg.gitterAlpha * (0.15 + 0.85 * tiefe));
        ctx.beginPath();
        ctx.moveTo(0, y);
        ctx.lineTo(W, y);
        ctx.stroke();
      }
      // Senkrechte Linien laufen im Fluchtpunkt zusammen.
      for (let k = -9; k <= 9; k++) {
        const unten = fx + k * W * 0.16;
        const g = ctx.createLinearGradient(0, horizont, 0, H);
        g.addColorStop(0, rgba(cfg.linie, 0));
        g.addColorStop(1, rgba(cfg.linie, cfg.gitterAlpha * 0.9));
        ctx.strokeStyle = g;
        ctx.beginPath();
        ctx.moveTo(fx + k * W * 0.012, horizont);
        ctx.lineTo(unten, H);
        ctx.stroke();
      }
      // Leuchtpunkte auf Schnittpunkten.
      punkte.forEach((pt) => {
        const t = (((p * 2 + pt.phase) % 1) + 1) % 1;
        if (t > 0.18) return;
        const z = (pt.reihe + 0.5) / 8;
        const tiefe = Math.pow(z, 2.2);
        const y = horizont + (H - horizont) * tiefe;
        const x = fx + pt.spalte * (W * 0.012 + (W * 0.16 - W * 0.012) * tiefe);
        schein(x, y, 6 + 10 * tiefe, cfg.signal, cfg.punktAlpha * Math.sin((Math.PI * t) / 0.18));
      });
      // Kein leuchtendes Horizontband mehr: Es stand als fester waagerechter Strich mitten im
      // Bild und sah aus wie eine stehengebliebene Abtastlinie. Einzig die Abtastlinie wandert.
    }

    function zeichne(p) {
      ctx.globalCompositeOperation = "source-over";
      ctx.fillStyle = cfg.grund;
      ctx.fillRect(0, 0, W, H);
      ctx.globalCompositeOperation = cfg.mischung;

      // 1. Kühles Leuchten hinter dem Planeten.
      schein(mitte.x, mitte.y, W * 0.75, cfg.linie, cfg.feldAlpha * (0.85 + 0.15 * Math.sin(TAU * p)));

      // 2. Radarstrahl: ein Kegel, der einmal pro Schleife umläuft.
      const radar = TAU * p - Math.PI / 2;
      const kegel = ctx.createConicGradient(radar - 0.9, mitte.x, mitte.y);
      kegel.addColorStop(0, rgba(cfg.linie, 0));
      kegel.addColorStop(0.142, rgba(cfg.linie, cfg.radarAlpha));
      kegel.addColorStop(0.145, rgba(cfg.linie, 0));
      kegel.addColorStop(1, rgba(cfg.linie, 0));
      ctx.fillStyle = kegel;
      ctx.beginPath();
      ctx.arc(mitte.x, mitte.y, W * 0.62, 0, TAU);
      ctx.fill();

      // 3. Bahnen und Satelliten — hinter dem Planeten gedämpft.
      bahnen.forEach((b, i) => {
        ctx.strokeStyle = rgba(cfg.linie, cfg.bahnAlpha);
        ctx.lineWidth = 1;
        ctx.setLineDash([4, 7]);
        ctx.beginPath();
        for (let s = 0; s <= 96; s++) {
          const q = bahnPunkt(b, (s / 96) * TAU);
          if (s === 0) ctx.moveTo(q.x, q.y);
          else ctx.lineTo(q.x, q.y);
        }
        ctx.stroke();
        ctx.setLineDash([]);
        versatz[i].forEach((v) => {
          const q = bahnPunkt(b, TAU * (p * b.umlaeufe + v));
          const a = cfg.satellitAlpha * (q.vorn ? 1 : 0.35);
          schein(q.x, q.y, 14, cfg.signal, a * 0.7);
          ctx.fillStyle = rgba(cfg.signal, a);
          ctx.fillRect(q.x - 2, q.y - 2, 4, 4);
        });
      });

      // 4. Der Planet: ein glimmender Kern mit dunkler Nachtseite.
      const kern = ctx.createRadialGradient(mitte.x - 14, mitte.y - 14, 4, mitte.x, mitte.y, 46);
      kern.addColorStop(0, rgba(cfg.linie, cfg.planetAlpha));
      kern.addColorStop(0.7, rgba(cfg.linie, cfg.planetAlpha * 0.35));
      kern.addColorStop(1, rgba(cfg.linie, 0));
      ctx.fillStyle = kern;
      ctx.beginPath();
      ctx.arc(mitte.x, mitte.y, 46, 0, TAU);
      ctx.fill();

      // 5. Das Gitter unten.
      gitter(p);

      // 6. Abtastlinie, einmal pro Schleife von oben nach unten.
      const scanY = -40 + p * (H + 80);
      const sg = ctx.createLinearGradient(0, scanY - 60, 0, scanY + 4);
      sg.addColorStop(0, rgba(cfg.linie, 0));
      sg.addColorStop(1, rgba(cfg.linie, cfg.scanAlpha));
      ctx.fillStyle = sg;
      ctx.fillRect(0, scanY - 60, W, 64);

      // 7. Feine Zeilenstruktur wie auf einem Röhrenschirm.
      ctx.globalCompositeOperation = "source-over";
      ctx.fillStyle = `rgba(0,0,0,${cfg.zeilenAlpha})`;
      for (let y = 0; y < H; y += 4) ctx.fillRect(0, y, W, 1);

      // 8. Vignette — dieselbe Formel wie `Modifier.vignette` in der App.
      const v = ctx.createRadialGradient(W / 2, H / 2, 0, W / 2, H / 2, Math.max(W, H) * 0.75);
      v.addColorStop(0, "rgba(0,0,0,0)");
      v.addColorStop(1, `rgba(0,0,0,${cfg.vignette})`);
      ctx.fillStyle = v;
      ctx.fillRect(0, 0, W, H);
    }

    const zustand = { p: 0 };
    const tl = gsap.timeline({ paused: true });
    tl.to(zustand, { p: 1, duration: cfg.dauer, ease: "none", onUpdate: () => zeichne(zustand.p % 1) }, 0);
    zeichne(0);
    return tl;
  };
})();
