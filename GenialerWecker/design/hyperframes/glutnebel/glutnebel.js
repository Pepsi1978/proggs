// Glutnebel — der bewegte Hintergrund des Designs „Traumraum“ im GenialerWecker.
//
// Ein warmer Traumhimmel: Glutfarbene Nebel kreisen langsam unter der Kuppel, Funken steigen
// wie aus einem Lagerfeuer auf und ziehen kurze Schweife, und zweimal pro Schleife fällt eine
// Sternschnuppe. Die funkelnden Sterne zeichnet die App selbst darüber (FunkelHimmel).
// Alles hängt an p ∈ [0, 1) mit ganzzahligen Frequenzen — die Schleife springt nicht.

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

  window.glutnebel = function (cfg) {
    const leinwand = document.getElementById(cfg.leinwandId);
    const ctx = leinwand.getContext("2d");
    const W = leinwand.width;
    const H = leinwand.height;
    const rnd = zufall(cfg.saat || 2404);

    const wolken = Array.from({ length: cfg.wolkenAnzahl }, (_, i) => ({
      x: 0.15 + rnd() * 0.7,
      y: 0.1 + rnd() * 0.8,
      bahnX: 0.06 + rnd() * 0.14,
      bahnY: 0.03 + rnd() * 0.08,
      richtung: rnd() < 0.5 ? 1 : -1,
      r: 0.35 + rnd() * 0.4,
      farbe: cfg.wolkenFarben[i % cfg.wolkenFarben.length],
      phase: rnd() * TAU,
      atem: 1 + Math.floor(rnd() * 2),
    }));

    const funken = Array.from({ length: cfg.funkenAnzahl }, () => ({
      x: 0.05 + rnd() * 0.9,
      y: rnd(),
      r: 0.8 + rnd() * 1.8,
      umlaeufe: 1 + Math.floor(rnd() * 2),
      schwanken: 10 + rnd() * 30,
      freq: 1 + Math.floor(rnd() * 3),
      phase: rnd() * TAU,
      hell: 0.4 + rnd() * 0.6,
      farbe: rnd() < 0.6 ? cfg.funkeHell : cfg.funkeTief,
    }));

    function schein(x, y, r, farbe, a) {
      const g = ctx.createRadialGradient(x, y, 0, x, y, r);
      g.addColorStop(0, rgba(farbe, a));
      g.addColorStop(0.5, rgba(farbe, a * 0.4));
      g.addColorStop(1, rgba(farbe, 0));
      ctx.fillStyle = g;
      ctx.fillRect(x - r, y - r, r * 2, r * 2);
    }

    function funkeBei(f, p) {
      const yAnteil = (((f.y - p * f.umlaeufe) % 1) + 1) % 1;
      return {
        x: W * f.x + f.schwanken * Math.sin(TAU * p * f.freq + f.phase),
        y: -30 + yAnteil * (H + 60),
        yAnteil,
      };
    }

    function sternschnuppe(p, start, x0, y0, winkel) {
      const dauer = 0.06;
      const t = (p - start) / dauer;
      if (t <= 0 || t >= 1) return;
      const lang = W * 0.9;
      const kopfX = x0 + Math.cos(winkel) * lang * t;
      const kopfY = y0 + Math.sin(winkel) * lang * t;
      const schweif = W * 0.28;
      const a = cfg.schnuppeAlpha * Math.sin(Math.PI * t);
      const g = ctx.createLinearGradient(kopfX, kopfY, kopfX - Math.cos(winkel) * schweif, kopfY - Math.sin(winkel) * schweif);
      g.addColorStop(0, rgba(cfg.schnuppe, a));
      g.addColorStop(1, rgba(cfg.schnuppe, 0));
      ctx.strokeStyle = g;
      ctx.lineWidth = 2;
      ctx.lineCap = "round";
      ctx.beginPath();
      ctx.moveTo(kopfX, kopfY);
      ctx.lineTo(kopfX - Math.cos(winkel) * schweif, kopfY - Math.sin(winkel) * schweif);
      ctx.stroke();
      schein(kopfX, kopfY, 10, cfg.schnuppe, a);
    }

    function zeichne(p) {
      ctx.globalCompositeOperation = "source-over";
      ctx.fillStyle = cfg.grund;
      ctx.fillRect(0, 0, W, H);

      ctx.globalCompositeOperation = cfg.mischung;

      // 1. Die Glutkuppel oben atmet — derselbe Ort wie der feste Schein der App.
      schein(W * 0.5, -H * 0.06, W * 1.15, cfg.kuppel, cfg.kuppelAlpha * (0.8 + 0.2 * Math.sin(TAU * p)));

      // 2. Nebelwolken auf Ellipsenbahnen, abwechselnd links und rechts herum.
      wolken.forEach((w) => {
        const winkel = TAU * p * w.richtung + w.phase;
        const x = W * (w.x + w.bahnX * Math.cos(winkel));
        const y = H * (w.y + w.bahnY * Math.sin(winkel));
        const a = cfg.wolkenAlpha * (0.7 + 0.3 * Math.sin(TAU * p * w.atem + w.phase));
        schein(x, y, W * w.r, w.farbe, a);
      });

      // 3. Aufsteigende Funken mit kurzem Schweif.
      funken.forEach((f) => {
        const flackern = 0.55 + 0.45 * Math.sin(TAU * p * f.freq * 3 + f.phase);
        for (let k = 4; k >= 0; k--) {
          const q = funkeBei(f, p - k * 0.004);
          const aus = 0.25 + 0.75 * q.yAnteil;
          const a = cfg.funkenAlpha * f.hell * flackern * aus * (1 - k / 5);
          if (k === 0) schein(q.x, q.y, f.r * 6, f.farbe, a * 0.6);
          ctx.fillStyle = rgba(f.farbe, a);
          ctx.beginPath();
          ctx.arc(q.x, q.y, f.r * (1 - k * 0.15), 0, TAU);
          ctx.fill();
        }
      });

      // 4. Zwei Sternschnuppen pro Schleife.
      sternschnuppe(p, 0.18, W * 0.1, H * 0.08, 0.42);
      sternschnuppe(p, 0.66, W * 0.55, H * 0.04, 0.62);

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
