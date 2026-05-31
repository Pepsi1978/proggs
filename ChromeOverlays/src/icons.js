// ============================================================
// icons.js — zentraler Icon-Satz (Material Design) + sichere Setzer
// ------------------------------------------------------------
// Schoene, einheitliche Material-Icons (fill=currentColor, 24x24) statt
// Standard-Emojis. Dazu die Trusted-Types-sicheren Helfer an EINER Stelle,
// die alle Module nutzen (Seiten wie grok.com/claude.ai verbieten
// "el.innerHTML = string"). Faellt notfalls auf ein Emoji zurueck, damit
// IMMER ein Symbol sichtbar ist.
// Wird frueh geladen (vor editable/stt/ui/actions).
// ============================================================
(() => {
	window.__chromeOverlays__ = window.__chromeOverlays__ || {};
	const OV = window.__chromeOverlays__;

	function svg(path, vb = "0 0 24 24") {
		return `<svg viewBox="${vb}" width="22" height="22" fill="currentColor" aria-hidden="true"><path d="${path}"/></svg>`;
	}

	// Material Design Icons (Apache 2.0). Weisses Icon auf farbigem Kreis.
	OV.ICONS = {
		// Mic-Zustaende
		mic: svg(
			"M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3zm5-3c0 2.76-2.24 5-5 5s-5-2.24-5-5H5c0 3.53 2.61 6.43 6 6.92V21h2v-3.08c3.39-.49 6-3.39 6-6.92h-2z",
		),
		stop: svg("M6 6h12v12H6z"),
		spinner: svg(
			"M12 4V1L8 5l4 4V6c3.31 0 6 2.69 6 6 0 1.01-.25 1.97-.7 2.8l1.46 1.46C19.54 15.03 20 13.57 20 12c0-4.42-3.58-8-8-8zm0 14c-3.31 0-6-2.69-6-6 0-1.01.25-1.97.7-2.8L5.24 7.74C4.46 8.97 4 10.43 4 12c0 4.42 3.58 8 8 8v3l4-4-4-4v3z",
		),
		error: svg(
			"M11 15h2v2h-2zm0-8h2v6h-2zm.99-5C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8z",
		),
		// Tool-Buttons
		send: svg("M2.01 21 23 12 2.01 3 2 10l15 2-15 2z"),
		enter: svg("M19 7v4H5.83l3.58-3.59L8 6l-6 6 6 6 1.41-1.41L5.83 13H21V7z"),
		paste: svg(
			"M19 2h-4.18C14.4.84 13.3 0 12 0c-1.3 0-2.4.84-2.82 2H5c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-7 0c.55 0 1 .45 1 1s-.45 1-1 1-1-.45-1-1 .45-1 1-1zm7 18H5V4h2v3h10V4h2v16z",
		),
		copy: svg(
			"M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z",
		),
		clear: svg(
			"M19 6.41 17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z",
		),
		// Gemini-Korrektur (Rechtschreibpruefung)
		spellcheck: svg(
			"M12.45 16h2.09L9.43 3H7.57L2.46 16h2.09l1.12-3h5.64l1.14 3zm-6.02-5L8.5 5.48 10.57 11H6.43zm15.16.59-8.09 8.09L9.83 16l-1.41 1.41 5.09 5.09L23 13z",
		),
		// Prompt-Builder
		sparkles: svg(
			"M19 9l1.25-2.75L23 5l-2.75-1.25L19 1l-1.25 2.75L15 5l2.75 1.25zm-7.5.5L9 4 6.5 9.5 1 12l5.5 2.5L9 20l2.5-5.5L17 12zM19 15l-1.25 2.75L15 19l2.75 1.25L19 23l1.25-2.75L23 19l-2.75-1.25z",
		),
		wand: svg(
			"M7.5 5.6 10 7 8.6 4.5 10 2 7.5 3.4 5 2l1.4 2.5L5 7zm12 9.8L17 14l1.4 2.5L17 19l2.5-1.4L22 19l-1.4-2.5L22 14zM22 2l-2.5 1.4L17 2l1.4 2.5L17 7l2.5-1.4L22 7zm-7.63 5.29c-.39-.39-1.02-.39-1.41 0L1.29 18.96c-.39.39-.39 1.02 0 1.41l2.34 2.34c.39.39 1.02.39 1.41 0L16.7 11.05c.39-.39.39-1.02 0-1.41zm-1.03 5.49-2.12-2.12 2.44-2.44 2.12 2.12z",
		),
		// Memory (Gehirn)
		brain: svg(
			"M13 3a1 1 0 0 0-1 1v15.17l-2.59-2.58L8 18l5 5 5-5-1.41-1.41L14 19.17V4a1 1 0 0 0-1-1zm-7.5 4.5a3.5 3.5 0 0 0 0 7 .5.5 0 0 0 0-1 2.5 2.5 0 0 1 0-5 .5.5 0 0 0 0-1zm13 0a.5.5 0 0 0 0 1 2.5 2.5 0 0 1 0 5 .5.5 0 0 0 0 1 3.5 3.5 0 0 0 0-7z",
		),
		save: svg(
			"M17 3H5c-1.11 0-2 .9-2 2v14c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V7l-4-4zm-5 16c-1.66 0-3-1.34-3-3s1.34-3 3-3 3 1.34 3 3-1.34 3-3 3zm3-10H5V5h10v4z",
		),
	};

	// Emoji-Notfall (nur falls SVG ueber innerHTML UND DOMParser scheitert)
	OV.ICON_FALLBACK = {
		mic: "🎙️",
		stop: "⏹️",
		spinner: "⏳",
		error: "⚠️",
		send: "➤",
		enter: "↵",
		paste: "📋",
		copy: "📄",
		clear: "✕",
		spellcheck: "✓",
		sparkles: "✨",
		wand: "🪄",
		brain: "🧠",
		save: "💾",
	};

	// Trusted-Types-sicher: innerHTML versuchen, sonst DOMParser, sonst Emoji.
	OV.setSvgIcon = function setSvgIcon(el, svgStr, emoji) {
		if (!el) return;
		try {
			el.innerHTML = svgStr;
			return;
		} catch {}
		try {
			el.textContent = "";
			const doc = new DOMParser().parseFromString(svgStr, "image/svg+xml");
			const node = doc.documentElement;
			if (node && node.tagName && node.tagName.toLowerCase() === "svg") {
				el.appendChild(document.importNode(node, true));
				return;
			}
		} catch {}
		el.textContent = emoji || "";
	};

	OV.setSafeInner = function setSafeInner(el, htmlStr) {
		if (!el) return;
		try {
			el.innerHTML = htmlStr;
			return;
		} catch {}
		try {
			el.textContent = "";
			const doc = new DOMParser().parseFromString(htmlStr, "text/html");
			for (const n of [...doc.body.childNodes])
				el.appendChild(document.importNode(n, true));
		} catch {
			el.textContent = htmlStr.replace(/<[^>]+>/g, "");
		}
	};
})();
