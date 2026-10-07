// Goldseide — der bewegte Hintergrund des Designs „Schlicht“ im GenialerWecker.
//
// Alles hängt an einem einzigen Fortschritt p ∈ [0, 1) über die ganze Schleife. Jede Bewegung ist
// eine periodische Funktion von p mit ganzzahliger Frequenz; dadurch ist das letzte Bild exakt das
// Vorbild des ersten und die Schleife läuft in der App ohne sichtbaren Sprung.
// HyperFrames verbietet Math.random(): Die Teilchen kommen aus einem festen Zufallsgenerator.

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

  window.goldseide = function (cfg) {
    const leinwand = document.getElementById(cfg.leinwandId);
    const ctx = leinwand.getContext("2d");
    const W = leinwand.width;
    const H = leinwand.height;
    const rnd = zufall(cfg.saat || 1978);

    // Goldstaub: wenige Funken, die an ihrem Platz schweben. Jeder blendet ein, steigt in seinem
    // ganzen Leben nur ein paar Dutzend Pixel (rund 3 px pro Sekunde statt früher 90 bis 180) und
    // blendet wieder aus. Der Neubeginn liegt im unsichtbaren Moment, die Schleife bleibt nahtlos.
    // Früher stieg jeder Funke in 16 s einmal oder zweimal durchs ganze Bild — das sah aus wie
    // Luftblasen in einem Handy, das ins Wasser gefallen ist.
    const staub = Array.from({ length: cfg.staubAnzahl }, () => ({
      x: rnd(),
      y: 0.04 + rnd() * 0.92,
      r: 0.8 + rnd() * 2.4,
      leben: 1 + Math.floor(rnd() * 2),
      steigen: 24 + rnd() * 40,
      schwanken: 4 + rnd() * 10,
      funkelFreq: 1 + Math.floor(rnd() * 3),
      phase: rnd(),
      hell: 0.35 + rnd() * 0.65,
    }));

    // Wenige große, weiche Lichtkreise (Bokeh) für Tiefe.
    const bokeh = Array.from({ length: cfg.bokehAnzahl }, () => ({
      x: rnd(),
      y: rnd(),
      r: 26 + rnd() * 60,
      phase: rnd() * TAU,
      bahn: 20 + rnd() * 50,
      hell: 0.4 + rnd() * 0.6,
    }));

    // Seidenbänder: jeweils ein Bündel feiner Fäden entlang einer wogenden Kurve.
    const baender = cfg.baender;

    function schein(x, y, r, farbe, a) {
      const g = ctx.createRadialGradient(x, y, 0, x, y, r);
      g.addColorStop(0, rgba(farbe, a));
      g.addColorStop(0.55, rgba(farbe, a * 0.35));
      g.addColorStop(1, rgba(farbe, 0));
      ctx.fillStyle = g;
      ctx.fillRect(x - r, y - r, r * 2, r * 2);
    }

    function band(b, p) {
      const schritte = 72;
      // Die Mittellinie des Bandes, einmal gerechnet; die Fäden liegen quer dazu verteilt.
      const mitte = [];
      for (let i = 0; i <= schritte; i++) {
        const u = i / schritte;
        mitte.push({
          x: -0.1 * W + u * 1.2 * W,
          y:
            H * (b.y + b.neigung * (u - 0.5)) +
            b.welle * Math.sin(TAU * (u * b.freq + p * b.lauf) + b.phase) +
            b.welle * 0.45 * Math.sin(TAU * (u * b.freq * 2.1 - p * b.lauf * 2) + b.phase * 1.7),
          dicke: b.dicke * (0.55 + 0.45 * Math.sin(TAU * (u * 0.8 + p * b.atmen) + b.phase)),
        });
      }
      function faden(anteil) {
        ctx.beginPath();
        mitte.forEach((m, i) => {
          const y = m.y + anteil * m.dicke;
          if (i === 0) ctx.moveTo(m.x, y);
          else ctx.lineTo(m.x, y);
        });
      }

      // 1. Ein weicher Schein unter dem Band: Die Seide leuchtet von innen und schimmert durch.
      if (b.schein) {
        faden(0);
        const s = ctx.createLinearGradient(0, 0, W, 0);
        s.addColorStop(0, rgba(b.licht, 0));
        s.addColorStop(0.5, rgba(b.licht, b.schein));
        s.addColorStop(1, rgba(b.licht, 0));
        ctx.strokeStyle = s;
        ctx.lineCap = "round";
        [1.1, 0.7, 0.4].forEach((breite, k) => {
          ctx.globalAlpha = 0.35 + k * 0.25;
          ctx.lineWidth = b.dicke * breite;
          ctx.stroke();
        });
        ctx.globalAlpha = 1;
      }

      // 2. Die Fäden.
      for (let f = 0; f < b.faeden; f++) {
        const anteil = f / (b.faeden - 1) - 0.5;
        faden(anteil);
        const verlauf = ctx.createLinearGradient(0, 0, W, 0);
        const kern = b.alpha * (1 - Math.abs(anteil) * 1.4);
        verlauf.addColorStop(0, rgba(b.farbe, 0));
        verlauf.addColorStop(0.25, rgba(b.farbe, kern * 0.7));
        verlauf.addColorStop(0.55, rgba(b.licht, kern));
        verlauf.addColorStop(0.85, rgba(b.farbe, kern * 0.6));
        verlauf.addColorStop(1, rgba(b.farbe, 0));
        ctx.strokeStyle = verlauf;
        ctx.lineWidth = b.faden;
        ctx.stroke();
      }

      // 3. Ein Glanzlicht wandert das Band entlang, wie Licht, das über Seide gleitet. Es läuft in
      //    ganzen Durchgängen und beginnt außerhalb des Bildes neu — kein Sprung an der Nahtstelle.
      if (b.glanz) {
        const cx = (-0.3 + 1.6 * ((p * (b.glanzLauf || 1) + b.phase / TAU) % 1)) * W;
        for (let f = 0; f < b.faeden; f += 2) {
          const anteil = f / (b.faeden - 1) - 0.5;
          faden(anteil);
          const g = ctx.createLinearGradient(cx - W * 0.2, 0, cx + W * 0.2, 0);
          const a = b.glanz * (1 - Math.abs(anteil) * 1.6);
          g.addColorStop(0, rgba(b.glanzFarbe || b.licht, 0));
          g.addColorStop(0.5, rgba(b.glanzFarbe || b.licht, a));
          g.addColorStop(1, rgba(b.glanzFarbe || b.licht, 0));
          ctx.strokeStyle = g;
          ctx.lineWidth = b.faden * 1.6;
          ctx.stroke();
        }
      }
    }

    function zeichne(p) {
      ctx.globalCompositeOperation = "source-over";
      ctx.fillStyle = cfg.grund;
      ctx.fillRect(0, 0, W, H);

      // 1. Die drei wandernden Lichtfelder — dieselbe Bahn wie der bisherige Hintergrund der App,
      //    nur mit einem dritten, hellen Feld in der Mitte.
      ctx.globalCompositeOperation = cfg.mischung;
      schein(W * (0.3 + 0.16 * Math.cos(TAU * p)), H * (0.22 + 0.07 * Math.sin(TAU * p)), W * 0.95, cfg.gold, cfg.feldAlpha[0]);
      schein(W * (0.76 - 0.15 * Math.sin(TAU * p)), H * (0.8 + 0.07 * Math.cos(TAU * p)), W * 0.8, cfg.warm, cfg.feldAlpha[1]);
      schein(W * (0.5 + 0.22 * Math.sin(TAU * 2 * p + 1)), H * (0.52 + 0.1 * Math.cos(TAU * p + 2)), W * 0.62, cfg.hell, cfg.feldAlpha[2]);

      // 2. Seidenbänder.
      baender.forEach((b) => band(b, p));

      // 3. Ein breiter Lichtschleier, der einmal pro Schleife schräg durchs Bild zieht.
      const lauf = (p - 0.5) / 0.4;
      if (lauf > 0 && lauf < 1) {
        const mitte = -0.4 * W + lauf * 1.8 * W;
        const g = ctx.createLinearGradient(mitte - W * 0.35, 0, mitte + W * 0.35, H * 0.25);
        const a = cfg.schleierAlpha * Math.sin(Math.PI * lauf);
        g.addColorStop(0, rgba(cfg.hell, 0));
        g.addColorStop(0.5, rgba(cfg.hell, a));
        g.addColorStop(1, rgba(cfg.hell, 0));
        ctx.fillStyle = g;
        ctx.fillRect(0, 0, W, H);
      }

      // 4. Bokeh.
      bokeh.forEach((k) => {
        const x = W * k.x + k.bahn * Math.cos(TAU * p + k.phase);
        const y = H * k.y + k.bahn * Math.sin(TAU * p + k.phase);
        const a = cfg.bokehAlpha * k.hell * (0.6 + 0.4 * Math.sin(TAU * p * 2 + k.phase));
        // Weicher Rand über einen Verlauf statt eines Unschärfefilters — gleiches Bild, ein Bruchteil der Rechenzeit.
        const g = ctx.createRadialGradient(x, y, k.r * 0.7, x, y, k.r);
        g.addColorStop(0, rgba(cfg.bokehFarbe, a));
        g.addColorStop(1, rgba(cfg.bokehFarbe, 0));
        ctx.fillStyle = g;
        ctx.beginPath();
        ctx.arc(x, y, k.r, 0, TAU);
        ctx.fill();
      });

      // 5. Goldstaub.
      staub.forEach((s) => {
        const lauf = (p * s.leben + s.phase) % 1;
        const y = H * s.y - s.steigen * lauf;
        const x = W * s.x + s.schwanken * Math.sin(TAU * (p + s.phase));
        const funkeln = 0.5 + 0.5 * Math.sin(TAU * (p * s.funkelFreq + s.phase));
        const a = cfg.staubAlpha * s.hell * Math.sin(Math.PI * lauf) * (0.4 + 0.6 * funkeln * funkeln);
        schein(x, y, s.r * 5, cfg.staubFarbe, a * 0.45);
        ctx.fillStyle = rgba(cfg.staubKern, a);
        ctx.beginPath();
        ctx.arc(x, y, s.r * 0.6, 0, TAU);
        ctx.fill();
      });

      // 6. Vignette — dieselbe Formel wie `Modifier.vignette` in der App.
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
