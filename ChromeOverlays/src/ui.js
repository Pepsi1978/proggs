// ============================================================
// ui.js — Button-Renderer aus der Registry + Watchdog
// ------------------------------------------------------------
// CATALOG: was jeder Button-Schluessel bedeutet (Aussehen + Aktion).
// buildOverlay(profile): baut genau die in profile.buttons gelisteten
// Knoepfe, von unten nach oben gestapelt. Der Watchdog stellt sie nach
// SPA-Navigation wieder her.
// ============================================================
(() => {
	window.__chromeOverlays__ = window.__chromeOverlays__ || {};
	const OV = window.__chromeOverlays__;
	const A = () => OV.actions;

	const BTN_SIZE = 42;
	const GAP = 52; // vertikaler Abstand zwischen Buttons

	function setColors(btn, bg, fg) {
		btn.style.setProperty("background", bg, "important");
		btn.style.setProperty("color", fg, "important");
	}
	function emojiRender(text, bg, fg, title) {
		return (btn) => {
			btn.textContent = text;
			btn.style.fontSize = "18px";
			btn.style.fontWeight = "normal";
			setColors(btn, bg, fg);
			btn.title = title;
		};
	}

	// Der Katalog: ein Eintrag pro Button-Schluessel. Neue Buttons hier ergaenzen.
	const CATALOG = {
		mic: { special: "mic", title: "Spracheingabe (Start/Stop)" },
		enter: {
			render: (b) => A().renderAutoEnter(b),
			onClick: (b) => A().toggleAutoEnter(b),
		},
		paste: {
			render: emojiRender("📋", "white", "black", "Zwischenablage einfuegen"),
			onClick: () => A().paste(),
		},
		copy: {
			render: emojiRender("📎", "white", "black", "Text kopieren"),
			onClick: () => A().copy(),
		},
		clear: {
			render: emojiRender("❌", "white", "#c40000", "Feld leeren"),
			onClick: () => A().clear(),
		},
		gemini: {
			render: (b) => A().renderGeminiToggle(b),
			onClick: (b) => A().toggleGemini(b),
		},
		promptFrank: {
			render: emojiRender(
				"✨",
				"white",
				"black",
				"Prompt (fuer Frank) einbetten",
			),
			onClick: (b) => A().promptFrank(b),
		},
		promptGeneral: {
			render: emojiRender(
				"🪄",
				"white",
				"black",
				"Prompt (allgemein / 10. Klasse) einbetten",
			),
			onClick: (b) => A().promptGeneral(b),
		},
		memory: {
			render: emojiRender("💾", "white", "black", "Memory-Prompt einfuegen"),
			onClick: (b) => A().memory(b),
		},
	};

	let currentProfile = null;
	let domObserver = null;
	let uiInterval = null;

	function btnId(profileId, key) {
		return `ov-${profileId}-${key}`;
	}

	function preventFocusSteal(btn) {
		if (btn.dataset.ovFocusGuard === "1") return;
		btn.addEventListener("pointerdown", (e) => e.preventDefault(), true);
		btn.addEventListener("mousedown", (e) => e.preventDefault(), true);
		btn.dataset.ovFocusGuard = "1";
	}

	function styleRoundButton(btn, profile, index) {
		const pos = profile.uiPos || {};
		const right = (pos.right ?? 16) + (pos.shiftLeft ?? 0);
		const bottom = (pos.bottom ?? 96) + index * GAP;
		btn.type = "button";
		btn.tabIndex = -1;
		const s = btn.style;
		s.setProperty("position", "fixed", "important");
		s.setProperty("z-index", "2147483647", "important");
		s.setProperty("width", `${BTN_SIZE}px`, "important");
		s.setProperty("height", `${BTN_SIZE}px`, "important");
		s.setProperty("right", `${right}px`, "important");
		s.setProperty("bottom", `${bottom}px`, "important");
		s.setProperty("left", "auto", "important");
		s.setProperty("top", "auto", "important");
		s.setProperty("border-radius", "50%", "important");
		s.setProperty("border", "1px solid rgba(0,0,0,0.2)", "important");
		s.setProperty("background", "white", "important");
		s.setProperty("box-shadow", "0 6px 18px rgba(0,0,0,0.18)", "important");
		s.setProperty("cursor", "pointer", "important");
		s.setProperty("display", "flex", "important");
		s.setProperty("align-items", "center", "important");
		s.setProperty("justify-content", "center", "important");
		s.setProperty("padding", "0", "important");
		s.setProperty("margin", "0", "important");
		s.setProperty("line-height", "1", "important");
		s.setProperty("user-select", "none", "important");
		s.setProperty(
			"transition",
			"transform 0.15s ease, box-shadow 0.25s ease",
			"important",
		);
		btn.onmouseenter = () =>
			s.setProperty("transform", "scale(1.15)", "important");
		btn.onmouseleave = () =>
			s.setProperty("transform", "scale(1)", "important");
	}

	function makeButton(profile, key, index) {
		const entry = CATALOG[key];
		if (!entry) {
			console.warn("[Overlays] Unbekannter Button-Schluessel:", key);
			return null;
		}
		const id = btnId(profile.id, key);
		let btn = document.getElementById(id);
		if (!btn || btn.tagName !== "BUTTON") {
			btn?.remove?.();
			btn = document.createElement("button");
			btn.id = id;
		}
		btn.classList.add("ov-btn");
		btn.dataset.ovKey = key;
		styleRoundButton(btn, profile, index);
		preventFocusSteal(btn);

		if (entry.special === "mic") {
			OV.stt.attachMic(btn);
		} else {
			btn.__ovRender = () => entry.render(btn);
			entry.render(btn);
			btn.onclick = () => entry.onClick(btn);
		}

		const mount = document.body || document.documentElement;
		if (!btn.isConnected || btn.parentNode !== mount) mount.appendChild(btn);
		return btn;
	}

	function isRenderable(btn) {
		if (!btn || !btn.isConnected) return false;
		const cs = window.getComputedStyle(btn);
		if (!cs || cs.display === "none" || cs.visibility === "hidden")
			return false;
		const rect = btn.getBoundingClientRect();
		return rect.width >= 10 && rect.height >= 10;
	}

	function buildOverlay(profile) {
		currentProfile = profile;
		if (!document.body) return;
		profile.buttons.forEach((key, index) => {
			makeButton(profile, key, index);
		});
	}

	function needsRepair() {
		if (!currentProfile) return false;
		return currentProfile.buttons.some((key) => {
			const btn = document.getElementById(btnId(currentProfile.id, key));
			return !btn || !isRenderable(btn);
		});
	}

	let ensureScheduled = false;
	function scheduleEnsure() {
		if (ensureScheduled || !currentProfile) return;
		ensureScheduled = true;
		setTimeout(() => {
			ensureScheduled = false;
			try {
				buildOverlay(currentProfile);
			} catch (e) {
				console.warn("[Overlays] buildOverlay:", e);
			}
		}, 250);
	}

	function startWatchdog() {
		try {
			if (domObserver) domObserver.disconnect();
			let throttle = 0;
			domObserver = new MutationObserver(() => {
				if (throttle) return;
				throttle = setTimeout(() => {
					throttle = 0;
					if (needsRepair()) scheduleEnsure();
				}, 1000);
			});
			domObserver.observe(document.body || document.documentElement, {
				childList: true,
				subtree: true,
			});
		} catch (e) {
			console.warn("[Overlays] Observer:", e);
		}
		try {
			const _push = history.pushState;
			const _replace = history.replaceState;
			history.pushState = function (...args) {
				const r = _push.apply(this, args);
				scheduleEnsure();
				return r;
			};
			history.replaceState = function (...args) {
				const r = _replace.apply(this, args);
				scheduleEnsure();
				return r;
			};
			window.addEventListener("popstate", scheduleEnsure, true);
		} catch (e) {
			console.warn("[Overlays] History hooks:", e);
		}
		if (uiInterval) clearInterval(uiInterval);
		uiInterval = setInterval(() => {
			if (needsRepair()) scheduleEnsure();
		}, 3000);
	}

	OV.ui = { buildOverlay, startWatchdog };
})();
