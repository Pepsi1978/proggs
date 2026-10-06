// Morgenlicht — der bewegte Hintergrund des Designs „Morgenruhe“ im GenialerWecker.
//
// Ein ruhiger Morgen: Am unteren Rand dämmert ein warmer Horizont, aus ihm fächern weiche
// Sonnenstrahlen auf, Nebelbänder ziehen langsam quer, und feiner Blütenstaub schwebt im Licht.
// Alles hängt an einem Fortschritt p ∈ [0, 1); jede Bewegung ist periodisch mit ganzzahliger
// Frequenz, deshalb läuft die Schleife ohne Sprung. Feste Zufallszahlen statt Math.random().

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

  window.morgenlicht = function (cfg) {
    const leinwand = document.getElementById(cfg.leinwandId);
    const ctx = leinwand.getContext("2d");
    const W = leinwand.width;
    const H = leinwand.height;
    const rnd = zufall(cfg.saat || 711);

    const strahlen = Array.from({ length: cfg.strahlAnzahl }, (_, i) => ({
      winkel: -168 + (i / (cfg.strahlAnzahl - 1)) * 96 + (rnd() - 0.5) * 6,
      breite: 3 + rnd() * 5,
      freq: 1 + Math.floor(rnd() * 3),
      phase: rnd() * TAU,
      hell: 0.5 + rnd() * 0.5,
    }));

    const staub = Array.from({ length: cfg.staubAnzahl }, () => ({
      x: rnd(),
      y: rnd(),
      r: 0.7 + rnd() * 2.0,
      umlaeufe: 1,
      seitwaerts: (rnd() - 0.5) * 0.25,
      schwanken: 8 + rnd() * 20,
      freq: 1 + Math.floor(rnd() * 3),
      phase: rnd() * TAU,
      hell: 0.3 + rnd() * 0.7,
    }));

    function weich(x, y, rx, ry, farbe, a) {
      ctx.save();
      ctx.translate(x, y);
      ctx.scale(1, ry / rx);
      const g = ctx.createRadialGradient(0, 0, 0, 0, 0, rx);
      g.addColorStop(0, rgba(farbe, a));
      g.addColorStop(0.5, rgba(farbe, a * 0.45));
      g.addColorStop(1, rgba(farbe, 0));
      ctx.fillStyle = g;
      ctx.fillRect(-rx, -rx, rx * 2, rx * 2);
      ctx.restore();
    }

    function zeichne(p) {
      ctx.globalCompositeOperation = "source-over";
      const himmel = ctx.createLinearGradient(0, 0, 0, H);
      himmel.addColorStop(0, cfg.himmelOben);
      himmel.addColorStop(0.62, cfg.grund);
      himmel.addColorStop(1, cfg.himmelUnten);
      ctx.fillStyle = himmel;
      ctx.fillRect(0, 0, W, H);

      ctx.globalCompositeOperation = cfg.mischung;

      // 1. Der Horizont atmet: ein warmes Leuchten am unteren Rand, rechts etwas heller.
      const atem = 0.85 + 0.15 * Math.sin(TAU * p);
      weich(W * 0.72, H * 1.02, W * 1.1, H * 0.34, cfg.glut, cfg.horizontAlpha * atem);
      weich(W * 0.25, H * 1.04, W * 0.8, H * 0.22, cfg.messing, cfg.horizontAlpha * 0.6 * (1.15 - 0.15 * atem));

      // 2. Sonnenstrahlen aus dem Horizont, die sich kaum merklich neigen.
      const sx = W * 0.8;
      const sy = H * 1.03;
      const neigung = 3.5 * Math.sin(TAU * p);
      strahlen.forEach((s) => {
        const w = ((s.winkel + neigung) * Math.PI) / 180;
        const halb = ((s.breite / 2) * Math.PI) / 180;
        const lang = H * 1.25;
        const a = cfg.strahlAlpha * s.hell * (0.55 + 0.45 * Math.sin(TAU * p * s.freq + s.phase));
        const g = ctx.createRadialGradient(sx, sy, 0, sx, sy, lang);
        g.addColorStop(0, rgba(cfg.licht, a));
        g.addColorStop(0.55, rgba(cfg.licht, a * 0.35));
        g.addColorStop(1, rgba(cfg.licht, 0));
        ctx.fillStyle = g;
        ctx.beginPath();
        ctx.moveTo(sx, sy);
        ctx.lineTo(sx + Math.cos(w - halb) * lang, sy + Math.sin(w - halb) * lang);
        ctx.lineTo(sx + Math.cos(w + halb) * lang, sy + Math.sin(w + halb) * lang);
        ctx.closePath();
        ctx.fill();
      });

      // 3. Nebelbänder ziehen quer; jedes läuft in ganzen Bildbreiten und kehrt nahtlos zurück.
      cfg.nebel.forEach((n) => {
        const versatz = (((p * n.lauf) % 1) + 1) % 1;
        for (let k = -1; k <= 1; k++) {
          const x = (n.x + versatz + k) * W * 1.6 - W * 0.3;
          weich(x, H * n.y + 18 * Math.sin(TAU * p + n.phase), W * n.breite, H * n.hoehe, cfg.nebelFarbe, n.alpha);
        }
      });

      // 4. Blütenstaub im Licht.
      ctx.globalCompositeOperation = cfg.staubMischung;
      staub.forEach((s) => {
        const yAnteil = (((s.y - p * s.umlaeufe) % 1) + 1) % 1;
        const y = -20 + yAnteil * (H + 40);
        const x = W * s.x + s.schwanken * Math.sin(TAU * p * s.freq + s.phase) + W * s.seitwaerts * Math.sin(TAU * p);
        const funkeln = 0.5 + 0.5 * Math.sin(TAU * p * s.freq * 2 + s.phase);
        // Näher am Horizont leuchtet der Staub heller.
        const naehe = 0.35 + 0.65 * (y / H);
        const a = cfg.staubAlpha * s.hell * naehe * (0.3 + 0.7 * funkeln);
        const g = ctx.createRadialGradient(x, y, 0, x, y, s.r * 4);
        g.addColorStop(0, rgba(cfg.staubFarbe, a));
        g.addColorStop(1, rgba(cfg.staubFarbe, 0));
        ctx.fillStyle = g;
        ctx.fillRect(x - s.r * 4, y - s.r * 4, s.r * 8, s.r * 8);
      });

      // 5. Vignette — dieselbe Formel wie `Modifier.vignette` in der App.
      ctx.globalCompositeOperation = "source-over";
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
