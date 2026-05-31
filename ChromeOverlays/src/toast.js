// ============================================================
// toast.js — geteilte Toast-Anzeige (robust gegen DOM-Rebuild der SPA)
// ============================================================
(() => {
	window.__chromeOverlays__ = window.__chromeOverlays__ || {};
	const OV = window.__chromeOverlays__;

	const TOAST_ID = "ov-toast";
	let toast = null;
	let toastTimer = null;

	function ensureToast() {
		let t = document.getElementById(TOAST_ID);
		if (!t) {
			t = document.createElement("div");
			t.id = TOAST_ID;
			if (document.body) document.body.appendChild(t);
		}
		toast = t;
		Object.assign(t.style, {
			position: "fixed",
			bottom: "14px",
			left: "14px",
			zIndex: "2147483647",
			maxWidth: "760px",
			padding: "10px 12px",
			borderRadius: "10px",
			font: "12px/1.35 system-ui, -apple-system, Segoe UI, Roboto, Arial",
			boxShadow: "0 6px 24px rgba(0,0,0,0.18)",
			background: "rgba(20,20,20,0.92)",
			color: "white",
			display: "none",
			whiteSpace: "pre-wrap",
			pointerEvents: "none",
		});
		return t;
	}

	OV.toast = function showToast(msg, ms = 5500) {
		try {
			if (!document.body) return;
			ensureToast();
			clearTimeout(toastTimer);
			toast.textContent = msg;
			toast.style.display = "block";
			toastTimer = setTimeout(() => {
				if (toast) toast.style.display = "none";
			}, ms);
		} catch (e) {
			console.debug("[Overlays] toast:", e);
		}
	};

	OV.sleep = (ms) => new Promise((r) => setTimeout(r, ms));
})();
