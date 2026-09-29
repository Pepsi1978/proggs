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
  const CBY = Object.fromEntries(CATS.map(c => [c.id, c]));
  const SRC = D.sources;
  const VENDORS = [...new Set(MODELS.map(m => m.vendor))];
  const ORDER = Object.fromEntries(D.series.map((s, i) => [s.id, i]));

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
  const starsHtml = n => n ? `<span class="stars" title="Wichtigkeit in der Branche: ${n} von 5" aria-label="Wichtigkeit ${n} von 5"><span class="on">${"★".repeat(n)}</span><span class="off">${"★".repeat(5 - n)}</span></span>` : "";

  // ── Zustand ────────────────────────────────────────────────────────────────
  const VIEWS = [
    ["balken", "Balken", `<svg viewBox="0 0 16 16" width="15" height="15" aria-hidden="true"><rect x="1" y="2" width="11" height="3" rx="1"/><rect x="1" y="6.5" width="14" height="3" rx="1"/><rect x="1" y="11" width="7" height="3" rx="1"/></svg>`],
    ["linie", "Linie", `<svg viewBox="0 0 16 16" width="15" height="15" aria-hidden="true"><rect x="1" y="7.2" width="14" height="1.6" rx=".8"/><circle cx="4" cy="8" r="2.3"/><rect x="8.3" y="5.8" width="4.4" height="4.4" rx="1"/><path d="M13.5 5.2l2 2.8-2 2.8-2-2.8z"/></svg>`],
    ["tabelle", "Tabelle", `<svg viewBox="0 0 16 16" width="15" height="15" aria-hidden="true"><path d="M1.5 2h13a.5.5 0 0 1 .5.5v11a.5.5 0 0 1-.5.5h-13a.5.5 0 0 1-.5-.5v-11a.5.5 0 0 1 .5-.5zm.5 1v2.5h12V3zm0 3.5v3h5.5v-3zm6.5 0v3H14v-3zM2 10.5V13h5.5v-2.5zm6.5 0V13H14v-2.5z"/></svg>`]
  ];
  const defaultActive = MODELS.filter(m => m.default !== false).map(m => m.id);
  let active = store.get("active", defaultActive).filter(id => MBY[id]);
  let cat = store.get("cat", "alle");
  let view = store.get("view", "balken");
  if (!VIEWS.some(v => v[0] === view)) view = "balken";
  let theme = store.get("theme", "auto");
  let minStars = store.get("minStars", 0);
  let query = "";
  try { const h = decodeURIComponent(location.hash.slice(1)); if (h === "alle" || CBY[h]) cat = h; } catch (e) {}
  if (cat !== "alle" && !CBY[cat]) cat = "alle";

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
  const unitWord = s => ({ elo: "Elo-Punkte", pkt: "Punkte", n: "Anzahl", tps: "Token pro Sekunde", usd: "US-Dollar" }[s.unit] || "");
  function axis(s) {
    const all = MODELS.map(m => val(s, m.id)).filter(v => v != null);
    const max = Math.max(...all), min = Math.min(...all);
    if (s.unit === "elo") { const lo = Math.floor((min - 60) / 50) * 50, hi = Math.ceil((max + 30) / 50) * 50; return { lo, hi, dot: true }; }
    if (s.unit === "%" && max > 20) return { lo: 0, hi: 100 };
    return { lo: 0, hi: (max * 1.12) || 1 };
  }
  const better = (s, a, b) => s.lower ? a < b : a > b;
  const ranking = (s, ids) => ids.map(id => ({ id, v: val(s, id) })).filter(r => r.v != null).sort((a, b) => s.lower ? a.v - b.v : b.v - a.v);
  const byImportance = (a, b) => (b.stars || 0) - (a.stars || 0) || ORDER[a.id] - ORDER[b.id];
  const matchesQuery = s => !query || (s.name + " " + (s.desc || "") + " " + SRC[s.source].short + " " + (CBY[s.cat]?.label || "")).toLowerCase().includes(query);

  // ── Modellwahl ─────────────────────────────────────────────────────────────
  function renderLegend() {
    $("#legend").innerHTML = VENDORS.map(v => `<div class="vgroup"><button type="button" class="vname" data-vendor="${esc(v)}" title="Alle Modelle von ${esc(v)} ein- oder ausblenden">${esc(v.replace(" AI", ""))}</button>${MODELS.filter(m => m.vendor === v).map(m =>
      `<button type="button" class="mdl" data-id="${m.id}" aria-pressed="${active.includes(m.id)}" title="${esc(m.name)} ${active.includes(m.id) ? "ausblenden" : "einblenden"}"><span class="chk" aria-hidden="true"></span>${shapeSvg(m)}<span>${esc(m.short || m.name)}</span></button>`).join("")}</div>`).join("");
    $("#mcount").textContent = `${active.length} von ${MODELS.length} gewählt · antippen zum Ein- und Ausblenden`;
    const presets = [["alle", "Alle"], ["keine", "Keine"]];
    $("#presets").innerHTML = presets.map(([k, l]) => `<button type="button" class="lnk" data-q="${esc(k)}">${esc(l)}</button>`).join("");
  }
  function setActive(list) { active = list; store.set("active", active); renderAll(); }
  $("#legend").addEventListener("click", e => {
    const b = e.target.closest("button"); if (!b) return;
    if (b.dataset.vendor) {
      const ids = MODELS.filter(m => m.vendor === b.dataset.vendor).map(m => m.id);
      const allOn = ids.every(id => active.includes(id));
      setActive(allOn ? active.filter(id => !ids.includes(id)) : [...new Set([...active, ...ids])]);
    } else if (b.dataset.id) {
      const id = b.dataset.id;
      setActive(active.includes(id) ? active.filter(x => x !== id) : [...active, id]);
    }
  });
  $("#presets").addEventListener("click", e => {
    const b = e.target.closest("button"); if (!b) return;
    const q = b.dataset.q;
    if (q === "alle") setActive(MODELS.map(m => m.id));
    else if (q === "keine") setActive([]);
    else if (q.startsWith("v:")) setActive(MODELS.filter(m => m.vendor === q.slice(2)).map(m => m.id));
  });

  // ── Farbschema ─────────────────────────────────────────────────────────────
  function applyTheme() {
    if (theme === "auto") document.documentElement.removeAttribute("data-theme");
    else document.documentElement.setAttribute("data-theme", theme);
    $("#theme").querySelectorAll("button").forEach(b => b.setAttribute("aria-pressed", String(b.dataset.t === theme)));
  }
  $("#theme").addEventListener("click", e => {
    const b = e.target.closest("button"); if (!b) return;
    theme = b.dataset.t; store.set("theme", theme); applyTheme(); drawScatter();
  });

  // ── Bereich, Ansicht, Filter ───────────────────────────────────────────────
  function renderTabs() {
    const tabs = [{ id: "alle", label: "Alle" }, ...CATS];
    const count = id => D.series.filter(s => (id === "alle" || s.cat === id) && (s.stars || 0) >= minStars).length;
    $("#tabs").innerHTML = tabs.map(t => `<button type="button" data-c="${t.id}" aria-pressed="${t.id === cat}">${esc(t.label)} <span class="cnt">${count(t.id)}</span></button>`).join("");
    $("#views").innerHTML = VIEWS.map(([k, l, ic]) => `<button type="button" data-v="${k}" aria-pressed="${k === view}" title="Ansicht: ${l}">${ic}<span>${l}</span></button>`).join("");
    $("#minstars").value = String(minStars);
    const act = $("#tabs button[aria-pressed=true]");
    if (act && act.scrollIntoView) act.scrollIntoView({ block: "nearest", inline: "nearest" });
  }
  function setCat(c, scroll) {
    cat = c; store.set("cat", cat);
    try { history.replaceState(null, "", "#" + c); } catch (e) {}
    renderTabs(); renderBody();
    if (scroll) { const top = $("#body").getBoundingClientRect().top + scrollY - $("#controls").offsetHeight - 8; scrollTo({ top, behavior: "smooth" }); }
  }
  $("#tabs").addEventListener("click", e => { const b = e.target.closest("button"); if (b) setCat(b.dataset.c, true); });
  $("#views").addEventListener("click", e => { const b = e.target.closest("button"); if (!b) return; view = b.dataset.v; store.set("view", view); renderTabs(); renderBody(); });
  $("#minstars").addEventListener("change", e => { minStars = +e.target.value; store.set("minStars", minStars); renderTabs(); renderLeaders(); renderBody(); });
  let qt;
  $("#search").addEventListener("input", e => { clearTimeout(qt); qt = setTimeout(() => { query = e.target.value.trim().toLowerCase(); renderBody(); }, 120); });

  // ── Übersicht: wer führt wo ────────────────────────────────────────────────
  function renderLeaders() {
    const ids = active;
    const el = $("#leaders");
    if (ids.length < 2) { el.innerHTML = `<p class="empty">Wähle oben mindestens zwei Modelle aus. Dann steht hier, wer in welchem Bereich vorn liegt.</p>`; return; }
    el.innerHTML = D.focus.map(cid => {
      const c = CBY[cid];
      const ser = D.series.filter(s => s.cat === cid && !s.noRank && (s.stars || 0) >= minStars);
      const wins = {}; let base = 0;
      ser.forEach(s => { const r = ranking(s, ids); if (r.length >= 2) { base++; wins[r[0].id] = (wins[r[0].id] || 0) + 1; } });
      const top = Object.entries(wins).sort((a, b) => b[1] - a[1]);
      if (!base) return `<button type="button" class="lead" data-c="${cid}"><span class="eyebrow">${esc(c.label)}</span><span class="empty">Keine gemeinsamen Messungen</span><span class="go">Ansehen →</span></button>`;
      const [w, n] = top[0], m = MBY[w];
      const rest = top.slice(1, 3).map(([id, k]) => `${esc(MBY[id].short || MBY[id].name)} ${k}`).join(" · ");
      return `<button type="button" class="lead" data-c="${cid}" title="${esc(c.label)} ansehen">
        <span class="eyebrow">${esc(c.label)}</span>
        <span class="lw">${shapeSvg(m, 16)}<b>${esc(m.short || m.name)}</b></span>
        <span class="ls">vorn in <b class="num">${n} von ${base}</b> Benchmarks</span>
        ${rest ? `<span class="lr">danach: ${rest}</span>` : ""}
        <span class="go">Ansehen →</span></button>`;
    }).join("");
  }
  $("#leaders").addEventListener("click", e => { const b = e.target.closest(".lead"); if (b) setCat(b.dataset.c, true); });

  // ── Zeilen ─────────────────────────────────────────────────────────────────
  function badges(s, showCat) {
    const src = SRC[s.source];
    const b = [];
    if (showCat) b.push(`<span class="bdg cat">${esc(CBY[s.cat].label)}</span>`);
    b.push(`<span class="bdg src" title="${esc(src.desc || "")}">${esc(src.short)}</span>`);
    if (s.version) b.push(`<span class="bdg">v${esc(s.version)}</span>`);
    if (s.effort) b.push(`<span class="bdg" title="Denkaufwand, mit dem gemessen wurde">Effort ${esc(s.effort)}</span>`);
    if (s.jur) b.push(`<span class="bdg jur">${esc(s.jur)}</span>`);
    if (s.lower) b.push(`<span class="bdg low">↓ niedriger ist besser</span>`);
    if (unitWord(s)) b.push(`<span class="bdg">${unitWord(s)}</span>`);
    return b.join("");
  }

  function missLine(s, ms) {
    const miss = ms.filter(m => val(s, m.id) == null);
    return miss.length ? `<p class="miss">Nicht gemessen: ${miss.map(m => `<span>${shapeSvg(m, 10)}${esc(m.short || m.name)}</span>`).join("")}</p>` : "";
  }

  function vizBars(s, ms, ax) {
    const measured = ms.filter(m => val(s, m.id) != null);
    const best = ranking(s, measured.map(m => m.id))[0];
    const bars = measured.map(m => {
      const v = val(s, m.id), mt = meta(s, m.id);
      const isBest = measured.length > 1 && best && best.id === m.id;
      const tag = `<span class="tag">${shapeSvg(m, 12)}<span>${esc(m.short || m.name)}</span></span>`;
      const pct = Math.max(0, Math.min(100, (v - ax.lo) / (ax.hi - ax.lo) * 100));
      const extra = mt && mt.variant ? ` <em>${esc(mt.variant)}</em>` : "";
      const tip = `${m.name}: ${fmt(s, v)}${mt && mt.variant ? " (" + mt.variant + ")" : ""}${mt && mt.note ? " – " + mt.note : ""}`;
      const label = `${isBest ? "<b>" : ""}${fmt(s, v)}${isBest ? "</b>" : ""}${extra}`;
      if (ax.dot) return `<div class="b dot${isBest ? " best" : ""}" data-tip="${esc(tip)}">${tag}<div class="trk"><span class="pin" style="left:${pct}%;background:var(--m-${m.id})"></span><span class="val" style="left:calc(${pct}% + 12px)">${label}</span></div></div>`;
      const inside = pct > (mt && mt.variant ? 56 : 72);
      const pos = inside ? `right:calc(${100 - pct}% + 6px);color:var(--on-fill)` : `left:calc(${pct}% + 6px)`;
      return `<div class="b${isBest ? " best" : ""}" data-tip="${esc(tip)}">${tag}<div class="trk"><span class="fill" style="width:${pct}%;background:var(--m-${m.id})"></span><span class="val${inside ? " in" : ""}" style="${pos}">${label}</span></div></div>`;
    }).join("");
    const scale = ax.dot || ax.hi !== 100 ? `<span class="scale-note">Skala ${fmt(s, ax.lo)} bis ${fmt(s, ax.hi)}</span>` : "";
    return `<div class="bars">${bars || `<p class="miss">Keines der gewählten Modelle wurde hier gemessen.</p>`}${scale}${missLine(s, ms)}</div>`;
  }

  function vizStrip(s, ms, ax) {
    const W = 600, H = 34, pad = 12;
    const x = v => pad + (Math.max(ax.lo, Math.min(ax.hi, v)) - ax.lo) / (ax.hi - ax.lo) * (W - 2 * pad);
    const rs = ranking(s, ms.map(m => m.id));
    let g = `<line x1="${pad}" x2="${W - pad}" y1="17" y2="17" stroke="var(--track)" stroke-width="3" stroke-linecap="round"/>`;
    [...rs].reverse().forEach(r => {
      const m = MBY[r.id], mt = meta(s, r.id);
      g += `<g class="mk" data-tip="${esc(m.name + ": " + fmt(s, r.v) + (mt && mt.variant ? " (" + mt.variant + ")" : ""))}" fill="var(--m-${m.id})" stroke="var(--bg)" stroke-width="1.6">${SHAPES[m.shape](x(r.v), 17, 7)}</g>`;
    });
    const chips = rs.map((r, i) => { const m = MBY[r.id], mt = meta(s, r.id);
      return `<span class="rk${i === 0 ? " first" : ""}">${shapeSvg(m, 11)}<span class="rn">${i + 1}.</span> ${esc(m.short || m.name)} <b class="num">${fmt(s, r.v)}</b>${mt && mt.variant ? ` <em>${esc(mt.variant)}</em>` : ""}</span>`; }).join("");
    return `<div class="stripw"><svg class="strip" viewBox="0 0 ${W} ${H}" preserveAspectRatio="none" role="img" aria-label="${esc(s.name)}: Werte der gewählten Modelle">${g}</svg>
      <div class="axl"><span>${fmt(s, ax.lo)}</span><span>${fmt(s, ax.hi)}</span></div>
      <div class="rks">${chips}</div>${missLine(s, ms)}</div>`;
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
    const e = EXPL[s.info || s.bench];
    if (!e) return "";
    return `<details class="expl"><summary>Was misst ${esc(s.name)}?</summary><div class="expl-body">${(Array.isArray(e) ? e : [e]).map(p => `<p>${p}</p>`).join("")}</div></details>`;
  }

  function rowHtml(s, ms, showCat) {
    const ax = axis(s);
    const viz = view === "linie" ? vizStrip(s, ms, ax) : vizBars(s, ms, ax);
    return `<article class="row" id="r-${s.id}">
      <div class="name"><button type="button" class="title" data-expl="${s.id}" title="Erklärung auf- oder zuklappen"><strong>${esc(s.name)}</strong>${starsHtml(s.stars)}</button><small>${esc(s.desc || "")}</small><div class="bdgs">${badges(s, showCat)}</div></div>
      ${viz}
      ${sideHtml(s, ms)}
      ${s.note ? `<p class="note">${s.note}</p>` : ""}${explHtml(s)}
    </article>`;
  }

  function heatHtml(list, ms, showCat) {
    const head = `<tr><th class="stick">Benchmark</th>${ms.map(m => `<th class="mh"><span>${shapeSvg(m, 11)}${esc(m.short || m.name)}</span></th>`).join("")}</tr>`;
    const body = list.map(s => {
      const r = ranking(s, ms.map(m => m.id));
      const n = r.length;
      const pos = Object.fromEntries(r.map((x, i) => [x.id, i]));
      return `<tr><td class="bn stick"><b>${esc(s.name)} ${starsHtml(s.stars)}</b><span>${showCat ? esc(CBY[s.cat].label) + " · " : ""}${esc(SRC[s.source].short)}${s.version ? " · v" + esc(s.version) : ""}${s.effort ? " · " + esc(s.effort) : ""}${s.lower ? " · ↓ niedriger ist besser" : ""}</span></td>${ms.map(m => {
        const v = val(s, m.id);
        if (v == null) return `<td class="na">–</td>`;
        const p = n > 1 ? 1 - pos[m.id] / (n - 1) : 1;
        return `<td class="${pos[m.id] === 0 && n > 1 ? "best" : ""}" style="background:color-mix(in srgb, var(--heat) ${Math.round(6 + p * 34)}%, transparent)" data-tip="${esc(m.name + ": " + fmt(s, v))}">${fmt(s, v)}</td>`;
      }).join("")}</tr>`;
    }).join("");
    return `<div class="tbl-wrap"><table class="heat"><thead>${head}</thead><tbody>${body}</tbody></table></div>`;
  }

  function filteredSeries(filterCat) {
    const ms = activeModels();
    return D.series.filter(s => (filterCat === "alle" || s.cat === filterCat) && (s.stars || 0) >= minStars && matchesQuery(s) && ms.some(m => val(s, m.id) != null)).sort(byImportance);
  }

  function renderBody() {
    const ms = activeModels();
    const body = $("#body");
    if (!ms.length) { body.innerHTML = `<div class="empty big">Kein Modell ausgewählt. <button type="button" class="btn" data-reset="modelle">Alle Modelle einblenden</button></div>`; return; }
    const list = filteredSeries(cat);
    const total = D.series.filter(s => cat === "alle" || s.cat === cat).length;
    const showCat = cat === "alle";
    const c = CBY[cat];
    const filters = [];
    if (minStars) filters.push(`ab ${"★".repeat(minStars)}`);
    if (query) filters.push(`Suche „${esc(query)}“`);
    const head = `<div class="cat-head"><h2>${showCat ? "Alle Benchmarks" : esc(c.label)}</h2><span class="hint">${list.length} von ${total} Benchmarks · wichtigste zuerst${filters.length ? ` · Filter: ${filters.join(", ")} <button type="button" class="lnk" data-reset="filter">zurücksetzen</button>` : ""}</span></div>`;
    const intro = !showCat && c.intro ? `<p class="cat-intro">${c.intro}</p>` : "";
    let inner;
    if (!list.length) inner = `<div class="empty big">Keine Benchmarks passen zu den Filtern. <button type="button" class="btn" data-reset="filter">Filter zurücksetzen</button></div>`;
    else if (view === "tabelle") inner = heatHtml(list, ms, showCat);
    else inner = list.map(s => rowHtml(s, ms, showCat)).join("");
    body.innerHTML = `<section class="cat">${head}${intro}${inner}</section>`;
  }
  $("#body").addEventListener("click", e => {
    const r = e.target.closest("[data-reset]");
    if (r) {
      if (r.dataset.reset === "modelle") setActive(MODELS.map(m => m.id));
      else { minStars = 0; query = ""; store.set("minStars", 0); $("#search").value = ""; renderTabs(); renderLeaders(); renderBody(); }
      return;
    }
    const t = e.target.closest("[data-expl]");
    if (t) { const d = t.closest(".row").querySelector("details.expl"); if (d) d.open = !d.open; }
  });

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
      p.forEach(q => { s += `<g data-tip="${esc(m.name)} · Effort ${esc(q.effort)}: Index ${de(q.idx, 1)}, ${de(q.usd, 2)} $ pro Aufgabe${q.tps ? ", " + q.tps + " Token/s" : ""}" fill="var(--m-${m.id})" stroke="var(--panel)" stroke-width="1.5">${SHAPES[m.shape](X(q.usd), Y(q.idx), 5.5)}</g>`; });
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
    const rows = [["Hersteller", m => esc(m.vendor)], ["Erschienen", m => m.released || "–"], ["Modell-ID", m => m.apiId ? `<code>${esc(m.apiId)}</code>` : "–"],
      ["Input / Output je 1 Mio. Token", m => m.priceIn != null ? `${de(m.priceIn, m.priceIn % 1 ? 2 : 0)} $ / ${de(m.priceOut, m.priceOut % 1 ? 2 : 0)} $` : "–"],
      ["Kontextfenster", m => m.context || "–"], ["Offene Gewichte", m => m.openWeights ? "ja" : "nein"], ["Hinweis", m => esc(m.remark || "")]];
    $("#models").innerHTML = ms.length ? `<div class="tbl-wrap"><table class="plain"><thead><tr><th class="stick"></th>${ms.map(m => `<th class="mh"><span>${shapeSvg(m, 11)}${esc(m.short || m.name)}</span></th>`).join("")}</tr></thead><tbody>${rows.map(([l, f]) => `<tr><td class="stick">${l}</td>${ms.map(m => `<td>${f(m)}</td>`).join("")}</tr>`).join("")}</tbody></table></div>` : `<p class="empty">Kein Modell ausgewählt.</p>`;
  }

  function renderSources() {
    $("#sources").innerHTML = Object.values(SRC).map(s => `<li><b>${esc(s.short)}</b> – ${s.url ? `<a href="${esc(s.url)}" target="_blank" rel="noopener">${esc(s.label)}</a>` : esc(s.label)}${s.desc ? ". " + esc(s.desc) : ""}</li>`).join("");
    $("#stand").textContent = D.stand;
    $("#ver").textContent = `v${D.version} · Stand ${D.stand}`;
  }

  function renderAll() { renderLegend(); renderLeaders(); renderBody(); renderModels(); drawScatter(); }

  // ── Tooltip (Maus) ─────────────────────────────────────────────────────────
  const tip = $("#tip");
  document.addEventListener("mousemove", e => {
    const t = e.target.closest && e.target.closest("[data-tip]");
    if (!t) { tip.style.opacity = 0; return; }
    tip.textContent = t.getAttribute("data-tip");
    tip.style.left = Math.min(e.clientX + 14, innerWidth - tip.offsetWidth - 8) + "px";
    tip.style.top = (e.clientY + 16) + "px"; tip.style.opacity = 1;
  });

  // ── Nach oben ──────────────────────────────────────────────────────────────
  const toTop = $("#totop");
  addEventListener("scroll", () => { toTop.hidden = $("#modelbar").getBoundingClientRect().bottom > 0; }, { passive: true });
  toTop.addEventListener("click", () => { const c = $("#modelbar"); scrollTo({ top: Math.max(0, c.getBoundingClientRect().top + scrollY - 8), behavior: "smooth" }); });

  applyTheme(); renderTabs(); renderSources(); renderAll();
  let rt; addEventListener("resize", () => { clearTimeout(rt); rt = setTimeout(drawScatter, 120); });
})();
