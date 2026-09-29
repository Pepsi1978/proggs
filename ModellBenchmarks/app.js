// Rendering der Benchmark-Seite. Alle Inhalte kommen aus data.js (window.BENCH) und
// erklaerungen.js (window.ERKLAERUNGEN). Neue Modelle oder Benchmarks nur dort eintragen.
(function () {
  "use strict";
  const D = window.BENCH;
  const EXPL = window.ERKLAERUNGEN || {};
  const $ = s => document.querySelector(s);
  const esc = s => String(s ?? "").replace(/[&<>"]/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c]));
  const de = (x, d) => Number(x).toLocaleString("de-DE", { minimumFractionDigits: d, maximumFractionDigits: d });
  const store = {
    get(k, f) { try { const v = localStorage.getItem("mb-" + k); return v == null ? f : JSON.parse(v); } catch (e) { return f; } },
    set(k, v) { try { localStorage.setItem("mb-" + k, JSON.stringify(v)); } catch (e) {} }
  };

  const MODELS = D.models;
  const MBY = Object.fromEntries(MODELS.map(m => [m.id, m]));
  const CATS = D.categories;
  const SRC = D.sources;

  // ── Farben als CSS-Variablen (hell + dunkel) ───────────────────────────────
  (function injectColors() {
    const light = MODELS.map(m => `--m-${m.id}:${m.color[0]};`).join("");
    const dark = MODELS.map(m => `--m-${m.id}:${m.color[1]};`).join("");
    const st = document.createElement("style");
    st.textContent = `:root{${light}}@media (prefers-color-scheme: dark){:root:not([data-theme="light"]){${dark}}}:root[data-theme="dark"]{${dark}}`;
    document.head.appendChild(st);
  })();

  // Markerformen (zweiter Kanal neben der Farbe)
  const SHAPES = {
    circle: (x, y, r) => `<circle cx="${x}" cy="${y}" r="${r}"/>`,
    square: (x, y, r) => `<rect x="${x - r * .88}" y="${y - r * .88}" width="${r * 1.76}" height="${r * 1.76}" rx="1.5"/>`,
    diamond: (x, y, r) => `<path d="M${x},${y - r * 1.2} L${x + r * 1.2},${y} L${x},${y + r * 1.2} L${x - r * 1.2},${y} Z"/>`,
    triangle: (x, y, r) => `<path d="M${x},${y - r * 1.2} L${x + r * 1.15},${y + r * .9} L${x - r * 1.15},${y + r * .9} Z"/>`,
    tridown: (x, y, r) => `<path d="M${x},${y + r * 1.2} L${x + r * 1.15},${y - r * .9} L${x - r * 1.15},${y - r * .9} Z"/>`,
    hexagon: (x, y, r) => { let p = ""; for (let i = 0; i < 6; i++) { const a = Math.PI / 3 * i; p += (i ? "L" : "M") + (x + r * 1.1 * Math.cos(a)) + "," + (y + r * 1.1 * Math.sin(a)); } return `<path d="${p}Z"/>`; },
    star: (x, y, r) => { let p = ""; for (let i = 0; i < 10; i++) { const a = Math.PI / 5 * i - Math.PI / 2, rr = i % 2 ? r * .55 : r * 1.3; p += (i ? "L" : "M") + (x + rr * Math.cos(a)) + "," + (y + rr * Math.sin(a)); } return `<path d="${p}Z"/>`; },
    cross: (x, y, r) => { const a = r * .42, b = r * 1.15; return `<path d="M${x - a},${y - b}h${2 * a}v${b - a}h${b - a}v${2 * a}h${-(b - a)}v${b - a}h${-2 * a}v${-(b - a)}h${-(b - a)}v${-2 * a}h${b - a}Z"/>`; }
  };
  const shapeSvg = (m, size = 14) => `<svg class="shp" width="${size}" height="${size}" viewBox="0 0 16 16" aria-hidden="true"><g fill="var(--m-${m.id})">${SHAPES[m.shape](8, 8, 5.2)}</g></svg>`;

  // ── Zustand ────────────────────────────────────────────────────────────────
  const defaultActive = MODELS.filter(m => m.default !== false).map(m => m.id);
  let active = store.get("active", defaultActive).filter(id => MBY[id]);
  if (!active.length) active = defaultActive.slice();
  let cat = store.get("cat", "alle");
  if (cat !== "alle" && !CATS.some(c => c.id === cat)) cat = "alle";
  let view = store.get("view", "diagramm");
  let theme = store.get("theme", "auto");

  const activeModels = () => MODELS.filter(m => active.includes(m.id));

  // ── Werte-Helfer ───────────────────────────────────────────────────────────
  const val = (s, id) => { const x = s.values[id]; return x == null ? null : (typeof x === "object" ? x.v : x); };
  const meta = (s, id) => { const x = s.values[id]; return x && typeof x === "object" ? x : null; };
  const unitDigits = s => s.digits ?? (s.unit === "elo" || s.unit === "n" || s.unit === "tps" ? 0 : s.unit === "usd" ? 2 : 1);
  function fmt(s, x) {
    const d = unitDigits(s);
    switch (s.unit) {
      case "%": return de(x, d) + " %";
      case "elo": return de(x, 0);
      case "usd": return de(x, 2) + " $";
      case "tps": return de(x, 0) + " t/s";
      default: return de(x, d);
    }
  }
  const unitLabel = s => ({ "%": "Prozent", elo: "Elo-Punkte", usd: "US-Dollar", tps: "Token pro Sekunde", n: "Anzahl", pkt: "Indexpunkte" }[s.unit] || "");
  function axis(s) {
    const all = MODELS.map(m => val(s, m.id)).filter(v => v != null);
    const max = Math.max(...all), min = Math.min(...all);
    if (s.unit === "elo") { const lo = Math.floor((min - 60) / 50) * 50, hi = Math.ceil((max + 30) / 50) * 50; return { lo, hi, dot: true }; }
    if (s.unit === "%" && (s.scale100 !== false) && max > 20) return { lo: 0, hi: 100 };
    return { lo: 0, hi: (max * 1.12) || 1 };
  }
  const better = (s, a, b) => s.lower ? a < b : a > b;
  function ranking(s, ids) {
    return ids.map(id => ({ id, v: val(s, id) })).filter(r => r.v != null).sort((a, b) => s.lower ? a.v - b.v : b.v - a.v);
  }

  // ── Kopf: Legende, Theme, Tabs ─────────────────────────────────────────────
  function renderLegend() {
    const vendors = [...new Set(MODELS.map(m => m.vendor))];
    $("#legend").innerHTML = vendors.map(v => `<div class="vgroup"><span class="vname">${esc(v)}</span>${MODELS.filter(m => m.vendor === v).map(m =>
      `<button type="button" class="mdl" data-id="${m.id}" aria-pressed="${active.includes(m.id)}" title="${esc(m.name)} ein- oder ausblenden">${shapeSvg(m)}<span>${esc(m.name)}</span></button>`).join("")}</div>`).join("")
      + `<div class="vgroup quick"><button type="button" class="lnk" data-q="alle">Alle</button><button type="button" class="lnk" data-q="keine">Keine</button></div>`;
  }
  $("#legend").addEventListener("click", e => {
    const b = e.target.closest("button"); if (!b) return;
    if (b.dataset.q === "alle") active = MODELS.map(m => m.id);
    else if (b.dataset.q === "keine") active = [];
    else { const id = b.dataset.id; active = active.includes(id) ? active.filter(x => x !== id) : [...active, id]; }
    store.set("active", active); renderAll();
  });

  function applyTheme() {
    if (theme === "auto") document.documentElement.removeAttribute("data-theme");
    else document.documentElement.setAttribute("data-theme", theme);
    const lbl = { auto: "Automatisch", light: "Hell", dark: "Dunkel" }[theme];
    const btn = $("#theme");
    btn.querySelector(".tl").textContent = lbl;
    btn.setAttribute("aria-label", "Farbschema: " + lbl + ". Klicken zum Wechseln");
    btn.dataset.mode = theme;
  }
  $("#theme").addEventListener("click", () => {
    theme = { auto: "light", light: "dark", dark: "auto" }[theme];
    store.set("theme", theme); applyTheme(); drawScatter();
  });

  function renderTabs() {
    const tabs = [{ id: "alle", label: "Alle" }, ...CATS];
    $("#tabs").innerHTML = tabs.map(t => `<button type="button" data-c="${t.id}" aria-pressed="${t.id === cat}">${esc(t.label)}</button>`).join("");
    $("#views").innerHTML = [["diagramm", "Diagramm"], ["tabelle", "Tabelle"]].map(([k, l]) => `<button type="button" data-v="${k}" aria-pressed="${k === view}">${l}</button>`).join("");
  }
  $("#tabs").addEventListener("click", e => { const b = e.target.closest("button"); if (!b) return; cat = b.dataset.c; store.set("cat", cat); renderTabs(); renderBody(); });
  $("#views").addEventListener("click", e => { const b = e.target.closest("button"); if (!b) return; view = b.dataset.v; store.set("view", view); renderTabs(); renderBody(); });

  // ── Übersicht: wer führt wo ────────────────────────────────────────────────
  function renderLeaders() {
    const ids = active;
    const el = $("#leaders");
    if (ids.length < 2) { el.innerHTML = `<p class="empty">Wähle oben mindestens zwei Modelle aus, dann steht hier, wer in welchem Bereich vorn liegt.</p>`; return; }
    el.innerHTML = D.focus.map(cid => {
      const c = CATS.find(x => x.id === cid);
      const ser = D.series.filter(s => s.cat === cid && !s.noRank);
      const wins = {}; let base = 0;
      ser.forEach(s => { const r = ranking(s, ids); if (r.length >= 2) { base++; wins[r[0].id] = (wins[r[0].id] || 0) + 1; } });
      const top = Object.entries(wins).sort((a, b) => b[1] - a[1]);
      if (!base) return `<div class="lead"><span class="eyebrow">${esc(c.label)}</span><p class="empty">Keine gemeinsamen Messungen der gewählten Modelle.</p></div>`;
      const [w, n] = top[0], m = MBY[w];
      const rest = top.slice(1, 3).map(([id, k]) => `${esc(MBY[id].name)} ${k}`).join(" · ");
      return `<div class="lead"><span class="eyebrow">${esc(c.label)}</span>
        <div class="lw">${shapeSvg(m, 16)}<b>${esc(m.name)}</b></div>
        <span class="ls">vorn in <b class="num">${n} von ${base}</b> Benchmarks</span>
        ${rest ? `<span class="lr">danach: ${rest}</span>` : ""}</div>`;
    }).join("");
  }

  // ── Zeilen ─────────────────────────────────────────────────────────────────
  function badges(s) {
    const src = SRC[s.source];
    const b = [`<span class="bdg src" title="${esc(src.desc || "")}">${esc(src.short)}</span>`];
    if (s.version) b.push(`<span class="bdg">v${esc(s.version)}</span>`);
    if (s.effort) b.push(`<span class="bdg">Effort ${esc(s.effort)}</span>`);
    if (s.jur) b.push(`<span class="bdg jur">${esc(s.jur)}</span>`);
    if (s.lower) b.push(`<span class="bdg low">↓ niedriger ist besser</span>`);
    return b.join("");
  }

  function vizBars(s, ms, ax) {
    return ms.map(m => {
      const v = val(s, m.id), mt = meta(s, m.id);
      const tag = `<span class="tag">${shapeSvg(m, 12)}<span>${esc(m.short || m.name)}</span></span>`;
      if (v == null) return `<div class="b na">${tag}<div class="trk"><span class="val" style="left:0">nicht gemessen</span></div></div>`;
      const pct = Math.max(0, Math.min(100, (v - ax.lo) / (ax.hi - ax.lo) * 100));
      const extra = mt && mt.variant ? ` <em>${esc(mt.variant)}</em>` : "";
      const tip = `${m.name}: ${fmt(s, v)}${mt && mt.variant ? " (" + mt.variant + ")" : ""}${mt && mt.note ? " – " + mt.note : ""}`;
      if (ax.dot) return `<div class="b dot" data-tip="${esc(tip)}">${tag}<div class="trk"><span class="pin" style="left:${pct}%;background:var(--m-${m.id})"></span><span class="val" style="left:calc(${pct}% + 12px)">${fmt(s, v)}${extra}</span></div></div>`;
      const inside = pct > 74;
      const pos = inside ? `right:calc(${100 - pct}% + 6px);color:var(--on-fill)` : `left:calc(${pct}% + 6px)`;
      return `<div class="b" data-tip="${esc(tip)}">${tag}<div class="trk"><span class="fill" style="width:${pct}%;background:var(--m-${m.id})"></span><span class="val" style="${pos}">${fmt(s, v)}${extra}</span></div></div>`;
    }).join("") + (ax.dot || ax.hi !== 100 ? `<span class="scale-note">Skala ${fmt(s, ax.lo)} bis ${fmt(s, ax.hi)}</span>` : "");
  }

  function vizStrip(s, ms, ax) {
    const W = 600, H = 34, pad = 12;
    const x = v => pad + (Math.max(ax.lo, Math.min(ax.hi, v)) - ax.lo) / (ax.hi - ax.lo) * (W - 2 * pad);
    const rs = ranking(s, ms.map(m => m.id));
    let g = `<line x1="${pad}" x2="${W - pad}" y1="17" y2="17" stroke="var(--track)" stroke-width="3" stroke-linecap="round"/>`;
    [...rs].reverse().forEach(r => {
      const m = MBY[r.id], mt = meta(s, r.id);
      const tip = `${m.name}: ${fmt(s, r.v)}${mt && mt.variant ? " (" + mt.variant + ")" : ""}`;
      g += `<g class="mk" data-tip="${esc(tip)}" fill="var(--m-${m.id})" stroke="var(--bg)" stroke-width="1.6">${SHAPES[m.shape](x(r.v), 17, 7)}</g>`;
    });
    const chips = rs.map((r, i) => { const m = MBY[r.id], mt = meta(s, r.id);
      return `<span class="rk${i === 0 ? " first" : ""}" data-tip="${esc(m.name + ": " + fmt(s, r.v))}">${shapeSvg(m, 11)}<span class="rn">${i + 1}.</span> ${esc(m.short || m.name)} <b class="num">${fmt(s, r.v)}</b>${mt && mt.variant ? ` <em>${esc(mt.variant)}</em>` : ""}</span>`; }).join("");
    const miss = ms.filter(m => val(s, m.id) == null);
    return `<svg class="strip" viewBox="0 0 ${W} ${H}" preserveAspectRatio="none" role="img" aria-label="${esc(s.name)}: Werte aller gewählten Modelle">${g}</svg>
      <div class="axl"><span>${fmt(s, ax.lo)}</span><span>${fmt(s, ax.hi)}</span></div>
      <div class="rks">${chips}${miss.length ? `<span class="rk miss">nicht gemessen: ${miss.map(m => esc(m.short || m.name)).join(", ")}</span>` : ""}</div>`;
  }

  function sideHtml(s, ms) {
    const ids = ms.map(m => m.id);
    const have = ids.filter(id => val(s, id) != null);
    const cov = `<small>${have.length} von ${ids.length} gemessen</small>`;
    if (ids.length === 2 && have.length === 2) {
      const [a, b] = ms; const va = val(s, a.id), vb = val(s, b.id);
      const diff = vb - va, same = Math.abs(diff) < 1e-9, good = better(s, vb, va);
      const cls = same ? "flat" : good ? "up" : "down";
      const abs = Math.abs(diff);
      const txt = (diff > 0 ? "+" : diff < 0 ? "−" : "±") + (s.unit === "%" ? de(abs, abs < 1 ? 2 : 1) + " Pkt." : fmt(s, abs));
      return `<div class="side"><span class="d ${cls}">${same ? "" : good ? "▲ " : "▼ "}${txt}</span><small>${esc(b.short || b.name)} gegenüber ${esc(a.short || a.name)}</small></div>`;
    }
    const r = ranking(s, ids);
    if (!r.length) return `<div class="side">${cov}</div>`;
    const w = MBY[r[0].id];
    return `<div class="side"><span class="win">${shapeSvg(w, 12)}${esc(w.short || w.name)}</span>${cov}</div>`;
  }

  function explHtml(s) {
    const key = s.info || s.bench;
    const e = EXPL[key];
    if (!e) return "";
    const paras = (Array.isArray(e) ? e : [e]).map(p => `<p>${p}</p>`).join("");
    return `<details class="expl"><summary>Was misst ${esc(s.name)}?</summary><div class="expl-body">${paras}</div></details>`;
  }

  function rowHtml(s, ms) {
    const ax = axis(s);
    const viz = ms.length <= 4 ? `<div class="bars">${vizBars(s, ms, ax)}</div>` : `<div class="stripw">${vizStrip(s, ms, ax)}</div>`;
    const notes = (s.note ? `<p class="note">${s.note}</p>` : "");
    return `<article class="row">
      <div class="name"><strong>${esc(s.name)}</strong><small>${esc(s.desc || "")}</small><div class="bdgs">${badges(s)}</div></div>
      ${viz}
      ${sideHtml(s, ms)}
      ${notes}${explHtml(s)}
    </article>`;
  }

  function heatHtml(list, ms) {
    const head = `<tr><th>Benchmark</th>${ms.map(m => `<th class="mh"><span>${shapeSvg(m, 11)}${esc(m.short || m.name)}</span></th>`).join("")}</tr>`;
    const body = list.map(s => {
      const r = ranking(s, ms.map(m => m.id));
      const n = r.length;
      const pos = Object.fromEntries(r.map((x, i) => [x.id, i]));
      return `<tr><td class="bn"><b>${esc(s.name)}</b><span>${esc(SRC[s.source].short)}${s.version ? " · v" + esc(s.version) : ""}${s.effort ? " · " + esc(s.effort) : ""}${s.lower ? " · ↓" : ""}</span></td>${ms.map(m => {
        const v = val(s, m.id);
        if (v == null) return `<td class="na">–</td>`;
        const p = n > 1 ? 1 - pos[m.id] / (n - 1) : 1;
        const mix = Math.round(8 + p * 34);
        return `<td class="${pos[m.id] === 0 && n > 1 ? "best" : ""}" style="background:color-mix(in srgb, var(--heat) ${mix}%, transparent)" data-tip="${esc(m.name + ": " + fmt(s, v))}">${fmt(s, v)}</td>`;
      }).join("")}</tr>`;
    }).join("");
    return `<div class="tbl-wrap"><table class="heat"><thead>${head}</thead><tbody>${body}</tbody></table></div>`;
  }

  function renderBody() {
    const ms = activeModels();
    const body = $("#body");
    if (!ms.length) { body.innerHTML = `<p class="empty big">Kein Modell ausgewählt. Klicke oben auf ein Modell, um es einzublenden.</p>`; return; }
    const cats = cat === "alle" ? CATS : CATS.filter(c => c.id === cat);
    let hidden = 0;
    body.innerHTML = cats.map(c => {
      const all = D.series.filter(s => s.cat === c.id);
      const list = all.filter(s => ms.some(m => val(s, m.id) != null));
      hidden += all.length - list.length;
      if (!list.length) return "";
      const srcs = [...new Set(list.map(s => s.source))];
      const inner = view === "tabelle" ? heatHtml(list, ms)
        : srcs.map(sid => `<div class="srcgroup"><div class="srch"><span class="bdg src">${esc(SRC[sid].short)}</span><span>${esc(SRC[sid].label)}</span></div>${list.filter(s => s.source === sid).map(s => rowHtml(s, ms)).join("")}</div>`).join("");
      return `<section class="cat" id="cat-${c.id}">
        <div class="cat-head"><h2>${esc(c.label)}</h2><span class="hint">${list.length} ${list.length === 1 ? "Benchmark" : "Benchmarks"}</span></div>
        ${c.intro ? `<p class="cat-intro">${c.intro}</p>` : ""}
        ${inner}</section>`;
    }).join("") + (hidden ? `<p class="empty">${hidden} weitere Benchmarks ausgeblendet, weil keines der gewählten Modelle dort gemessen wurde.</p>` : "");
  }

  // ── Preis/Leistung: Intelligence Index gegen Kosten pro Aufgabe ─────────────
  function drawScatter() {
    const svg = $("#scatter"); if (!svg) return;
    const ms = activeModels().filter(m => D.aa[m.id] && D.aa[m.id].length);
    const W = Math.max(560, svg.parentElement.clientWidth), H = 360;
    const mg = { t: 16, r: 110, b: 44, l: 44 };
    const pts = ms.flatMap(m => D.aa[m.id]);
    if (!pts.length) { svg.setAttribute("viewBox", `0 0 ${W} 60`); svg.setAttribute("width", W); svg.setAttribute("height", 60); svg.innerHTML = `<text x="0" y="30" font-size="13" fill="var(--ink-3)">Für die gewählten Modelle liegen keine Artificial-Analysis-Daten vor.</text>`; return; }
    const ux = pts.map(p => p.usd), uy = pts.map(p => p.idx);
    const x0 = Math.log10(Math.min(...ux) * .8), x1 = Math.log10(Math.max(...ux) * 1.25);
    const y0 = Math.max(0, Math.floor((Math.min(...uy) - 5) / 10) * 10), y1 = Math.ceil((Math.max(...uy) + 3) / 10) * 10;
    const X = v => mg.l + (Math.log10(v) - x0) / (x1 - x0) * (W - mg.l - mg.r);
    const Y = v => mg.t + (1 - (v - y0) / (y1 - y0)) * (H - mg.t - mg.b);
    let s = "";
    for (let t = y0; t <= y1; t += 10) s += `<line x1="${mg.l}" x2="${W - mg.r}" y1="${Y(t)}" y2="${Y(t)}" stroke="var(--line)"/><text x="${mg.l - 8}" y="${Y(t) + 4}" text-anchor="end" font-size="11" fill="var(--ink-3)">${t}</text>`;
    [0.1, 0.2, 0.5, 1, 2, 5, 10, 20, 50].filter(t => Math.log10(t) >= x0 && Math.log10(t) <= x1).forEach(t => {
      s += `<line x1="${X(t)}" x2="${X(t)}" y1="${mg.t}" y2="${H - mg.b}" stroke="var(--line)" stroke-dasharray="2 4"/><text x="${X(t)}" y="${H - mg.b + 16}" text-anchor="middle" font-size="11" fill="var(--ink-3)">${de(t, t < 1 ? 1 : 0)} $</text>`;
    });
    s += `<text x="${(mg.l + W - mg.r) / 2}" y="${H - 6}" text-anchor="middle" font-size="11.5" fill="var(--ink-2)">Kosten pro Index-Aufgabe (logarithmisch)</text>`;
    s += `<text x="12" y="${mg.t + 4}" font-size="11.5" fill="var(--ink-2)" transform="rotate(-90 12 ${mg.t + 4})" text-anchor="end">Intelligence Index</text>`;
    const labels = [];
    ms.forEach(m => {
      const p = [...D.aa[m.id]].sort((a, b) => a.usd - b.usd);
      s += `<polyline points="${p.map(q => X(q.usd) + "," + Y(q.idx)).join(" ")}" fill="none" stroke="var(--m-${m.id})" stroke-width="2" stroke-linejoin="round" opacity=".85"/>`;
      p.forEach(q => { s += `<g data-tip="${esc(m.name)} · ${esc(q.effort)}: Index ${q.idx}, ${de(q.usd, 2)} $ pro Aufgabe${q.tps ? ", " + q.tps + " t/s" : ""}" fill="var(--m-${m.id})" stroke="var(--panel)" stroke-width="1.5">${SHAPES[m.shape](X(q.usd), Y(q.idx), 5.5)}</g>`; });
      const top = p.reduce((a, b) => (b.idx > a.idx ? b : a));
      labels.push({ y: Y(top.idx), x: X(top.usd), m });
    });
    labels.sort((a, b) => a.y - b.y);
    let last = -99;
    labels.forEach(l => { const y = Math.max(l.y, last + 14); last = y; s += `<text x="${Math.min(l.x + 10, W - mg.r + 6)}" y="${y + 4}" font-size="11.5" style="font-family:var(--font-body)" fill="var(--ink)">${esc(l.m.short || l.m.name)}</text>`; });
    svg.setAttribute("viewBox", `0 0 ${W} ${H}`); svg.setAttribute("width", W); svg.setAttribute("height", H);
    svg.innerHTML = s;
  }

  function renderModels() {
    const ms = activeModels();
    const rows = [["Hersteller", m => m.vendor], ["Erschienen", m => m.released || "–"], ["Modell-ID", m => m.apiId ? `<code>${esc(m.apiId)}</code>` : "–"],
      ["Input / Output je 1 Mio. Token", m => m.priceIn != null ? `${de(m.priceIn, m.priceIn % 1 ? 2 : 0)} $ / ${de(m.priceOut, m.priceOut % 1 ? 2 : 0)} $` : "–"],
      ["Kontextfenster", m => m.context || "–"], ["Offene Gewichte", m => m.openWeights ? "ja" : "nein"], ["Hinweis", m => m.remark || ""]];
    $("#models").innerHTML = ms.length ? `<div class="tbl-wrap"><table class="plain"><thead><tr><th></th>${ms.map(m => `<th class="mh"><span>${shapeSvg(m, 11)}${esc(m.name)}</span></th>`).join("")}</tr></thead><tbody>${rows.map(([l, f]) => `<tr><td>${l}</td>${ms.map(m => `<td>${f(m)}</td>`).join("")}</tr>`).join("")}</tbody></table></div>` : `<p class="empty">Kein Modell ausgewählt.</p>`;
  }

  function renderSources() {
    $("#sources").innerHTML = Object.values(SRC).map(s => `<li><b>${esc(s.short)}</b> – ${s.url ? `<a href="${esc(s.url)}" target="_blank" rel="noopener">${esc(s.label)}</a>` : esc(s.label)}${s.desc ? ". " + esc(s.desc) : ""}</li>`).join("");
    $("#stand").textContent = D.stand;
    $("#ver").textContent = `v${D.version} · Stand ${D.stand}`;
  }

  function renderAll() { renderLegend(); renderLeaders(); renderBody(); renderModels(); drawScatter(); }

  // ── Tooltip ────────────────────────────────────────────────────────────────
  const tip = $("#tip");
  document.addEventListener("mousemove", e => {
    const t = e.target.closest && e.target.closest("[data-tip]");
    if (!t) { tip.style.opacity = 0; return; }
    tip.textContent = t.getAttribute("data-tip");
    tip.style.left = Math.min(e.clientX + 14, innerWidth - tip.offsetWidth - 8) + "px";
    tip.style.top = (e.clientY + 16) + "px"; tip.style.opacity = 1;
  });

  applyTheme(); renderTabs(); renderSources(); renderAll();
  let rt; addEventListener("resize", () => { clearTimeout(rt); rt = setTimeout(drawScatter, 120); });
})();
