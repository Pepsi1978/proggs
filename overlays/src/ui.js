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

	// "Overlay verschiebbar" (1:1 wie beim Vorlese-Overlay): aus = feste
	// Standardposition; an = die ganze Button-Gruppe per Maus verschiebbar, bleibt
	// wo losgelassen. offRight/offBottom = gespeicherter Versatz (px) zum Standard.
	const DRAG = { on: false, offRight: 0, offBottom: 0 };
	const DRAG_THRESHOLD = 4;
	let dragActive = null;
	let suppressClick = false;

	// Material-Icon im farbigen Kreis (weisses Icon) — Stil wie BestJournal Android.
	// Trusted-Types-sicher ueber OV.setSvgIcon, Emoji nur als allerletzter Notfall.
	function iconRender(iconKey, bg, title) {
		return (btn) => {
			OV.setSvgIcon(btn, OV.ICONS[iconKey], OV.ICON_FALLBACK[iconKey]);
			btn.style.setProperty("background", bg, "important");
			btn.style.setProperty("color", "#fff", "important");
			btn.style.setProperty("border-color", bg, "important");
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
			render: iconRender("paste", "#7c3aed", "Zwischenablage einfuegen"),
			onClick: () => A().paste(),
		},
		copy: {
			render: iconRender("copy", "#0891b2", "Text kopieren"),
			onClick: () => A().copy(),
		},
		clear: {
			render: iconRender("clear", "#dc2626", "Feld leeren"),
			onClick: () => A().clear(),
		},
		gemini: {
			render: (b) => A().renderGeminiToggle(b),
			onClick: (b) => A().toggleGemini(b),
		},
		promptFrank: {
			render: iconRender(
				"sparkles",
				"#4f46e5",
				"Prompt (fuer Frank) einbetten",
			),
			onClick: (b) => A().promptFrank(b),
		},
		promptGeneral: {
			render: iconRender(
				"wand",
				"#db2777",
				"Prompt (allgemein / 10. Klasse) einbetten",
			),
			onClick: (b) => A().promptGeneral(b),
		},
		memory: {
			render: iconRender("save", "#ea580c", "Memory-Prompt einfuegen"),
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

	function styleRoundButton(btn, profile, col, row) {
		const pos = profile.uiPos || {};
		// col 0 = rechte Spalte, col 1 = linke Spalte (eine Button-Breite weiter
		// links). row 0 = unten. Die genaue Anordnung steht in registry.js (layout).
		const right =
			(pos.right ?? 16) + (pos.shiftLeft ?? 0) + col * GAP + DRAG.offRight;
		const bottom = (pos.bottom ?? 96) + row * GAP + DRAG.offBottom;
		btn.dataset.ovCol = String(col);
		btn.dataset.ovRow = String(row);
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

	function makeButton(profile, key, col, row) {
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
		styleRoundButton(btn, profile, col, row);
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

	function profileKeys(profile) {
		const L = profile.layout || { right: [], left: [] };
		return [...(L.right || []), ...(L.left || [])];
	}

	// Alle aktuellen Buttons mit dem aktuellen Versatz neu positionieren (waehrend
	// des Ziehens und beim Umschalten der Einstellung).
	function repositionAll() {
		if (!currentProfile) return;
		const pos = currentProfile.uiPos || {};
		const baseRight = (pos.right ?? 16) + (pos.shiftLeft ?? 0);
		const baseBottom = pos.bottom ?? 96;
		profileKeys(currentProfile).forEach((key) => {
			const btn = document.getElementById(btnId(currentProfile.id, key));
			if (!btn) return;
			const col = Number(btn.dataset.ovCol) || 0;
			const row = Number(btn.dataset.ovRow) || 0;
			btn.style.setProperty(
				"right",
				baseRight + col * GAP + DRAG.offRight + "px",
				"important",
			);
			btn.style.setProperty(
				"bottom",
				baseBottom + row * GAP + DRAG.offBottom + "px",
				"important",
			);
		});
	}

	function loadDragSettings() {
		try {
			chrome.storage.local.get(
				["ovDraggable", "ovOffRight", "ovOffBottom"],
				(r) => {
					if (chrome.runtime.lastError) return;
					DRAG.on = !!r.ovDraggable;
					DRAG.offRight = DRAG.on ? Number(r.ovOffRight) || 0 : 0;
					DRAG.offBottom = DRAG.on ? Number(r.ovOffBottom) || 0 : 0;
					repositionAll();
				},
			);
		} catch (e) {
			/* egal */
		}
	}

	// Einmalige Initialisierung der Drag-Mechanik (Gruppen-Verschiebung).
	let dragInit = false;
	function ensureDragInit() {
		if (dragInit) return;
		dragInit = true;
		loadDragSettings();

		document.addEventListener(
			"mousedown",
			(e) => {
				if (!DRAG.on) return;
				const btn = e.target.closest && e.target.closest(".ov-btn");
				if (!btn) return;
				if (e.button !== 0 && e.button !== 2) return; // links ODER rechts
				dragActive = {
					startX: e.clientX,
					startY: e.clientY,
					startRight: DRAG.offRight,
					startBottom: DRAG.offBottom,
					moved: false,
				};
				e.preventDefault();
			},
			true,
		);

		document.addEventListener(
			"mousemove",
			(e) => {
				if (!dragActive) return;
				const dx = e.clientX - dragActive.startX;
				const dy = e.clientY - dragActive.startY;
				if (!dragActive.moved && Math.hypot(dx, dy) > DRAG_THRESHOLD)
					dragActive.moved = true;
				if (dragActive.moved) {
					// Maus nach rechts -> right kleiner; nach unten -> bottom kleiner.
					DRAG.offRight = dragActive.startRight - dx;
					DRAG.offBottom = dragActive.startBottom - dy;
					repositionAll();
				}
			},
			true,
		);

		document.addEventListener(
			"mouseup",
			() => {
				if (!dragActive) return;
				const wasDrag = dragActive.moved;
				dragActive = null;
				if (wasDrag) {
					try {
						chrome.storage.local.set({
							ovOffRight: DRAG.offRight,
							ovOffBottom: DRAG.offBottom,
						});
					} catch (e) {
						/* egal */
					}
					suppressClick = true; // den folgenden Klick schlucken (war ein Zug)
				}
			},
			true,
		);

		document.addEventListener(
			"click",
			(e) => {
				if (suppressClick && e.target.closest && e.target.closest(".ov-btn")) {
					e.stopImmediatePropagation();
					e.preventDefault();
				}
				suppressClick = false;
			},
			true,
		);

		document.addEventListener(
			"contextmenu",
			(e) => {
				if (DRAG.on && e.target.closest && e.target.closest(".ov-btn"))
					e.preventDefault();
			},
			true,
		);

		try {
			chrome.storage.onChanged.addListener((changes, area) => {
				if (area !== "local" || !changes.ovDraggable) return;
				DRAG.on = !!changes.ovDraggable.newValue;
				if (!DRAG.on) {
					// Haekchen aus -> zurueck an den Standardort.
					DRAG.offRight = 0;
					DRAG.offBottom = 0;
					repositionAll();
				} else {
					loadDragSettings(); // gespeicherten Versatz wieder anwenden
				}
			});
		} catch (e) {
			/* egal */
		}
	}

	function buildOverlay(profile) {
		currentProfile = profile;
		if (!document.body) return;
		const L = profile.layout || { right: [], left: [] };
		(L.right || []).forEach((key, row) => {
			makeButton(profile, key, 0, row); // col 0 = rechte Spalte
		});
		(L.left || []).forEach((key, row) => {
			makeButton(profile, key, 1, row); // col 1 = linke Spalte
		});
		ensureDragInit();
	}

	function needsRepair() {
		if (!currentProfile) return false;
		return profileKeys(currentProfile).some((key) => {
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

	function removeOverlay(profile) {
		const target = profile || currentProfile;
		if (!target) return;
		profileKeys(target).forEach((key) => {
			document.getElementById(btnId(target.id, key))?.remove?.();
		});
		document.getElementById("stt-live-preview")?.remove?.();
		if (currentProfile && currentProfile.id === target.id) currentProfile = null;
		if (domObserver) {
			domObserver.disconnect();
			domObserver = null;
		}
		if (uiInterval) {
			clearInterval(uiInterval);
			uiInterval = null;
		}
	}

	OV.ui = { buildOverlay, startWatchdog, removeOverlay };
})();
