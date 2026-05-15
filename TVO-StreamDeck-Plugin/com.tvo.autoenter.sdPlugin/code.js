// ============================================================================
// TVO Auto-Enter Stream Deck Plugin — v4 (race-condition-free)
//
// v4-Aenderungen (2026-05-15, nach forensischer Log-Analyse):
//   * POLLING-PAUSE WAEHREND TOGGLE: Direkt nach keyDown wird das Polling
//     fuer 800 ms ausgesetzt. Verhindert dass ein Poll-Tick im Flug den
//     gerade erfolgten Toggle mit dem alten State ueberschreibt.
//   * DEDUPE: setState wird nur geschickt wenn sich der State wirklich
//     geaendert hat — kein Bombardieren mehr. Bei keyDown wird gezielt
//     "force"-gesendet damit die Bestaetigung des Toggles sofort am
//     Stream Deck sichtbar ist.
//   * GHOST-CONTEXT-CLEANUP: Wenn ein neuer willAppear-context auftaucht
//     waehrend ein alter noch in unserer Map ist UND der alte laenger als
//     5 Sekunden keine Stream-Deck-Aktivitaet hatte, wird er als Ghost
//     entfernt.
//   * Polling auf 700 ms verlangsamt — Stream Deck bekommt Luft.
// ============================================================================

const TVO_STATUS_URL = "http://127.0.0.1:5723/autoenter/status";
const TVO_TOGGLE_URL = "http://127.0.0.1:5723/autoenter/toggle";
const TVO_LOG_URL = "http://127.0.0.1:5723/log";
const POLL_INTERVAL_MS = 700;
const WS_RECONNECT_DELAY_MS = 1000;
const TOGGLE_PAUSE_MS = 800;
const GHOST_CONTEXT_TIMEOUT_MS = 5000;

let websocket = null;
let pluginUUID = null;
let registerEvent = null;
let wsPort = null;

// context -> { timer, lastOn, toggleUntil, lastSeen }
const actionContexts = new Map();

function connectElgatoStreamDeckSocket(
	inPort,
	inPluginUUID,
	inRegisterEvent,
	inInfo,
) {
	pluginUUID = inPluginUUID;
	registerEvent = inRegisterEvent;
	wsPort = inPort;
	openWebSocket();
}

function openWebSocket() {
	log("WS connecting to port " + wsPort);
	try {
		websocket = new WebSocket("ws://127.0.0.1:" + wsPort);
	} catch (e) {
		log("WS construction failed: " + e.message);
		setTimeout(openWebSocket, WS_RECONNECT_DELAY_MS);
		return;
	}

	websocket.onopen = () => {
		websocket.send(JSON.stringify({ event: registerEvent, uuid: pluginUUID }));
		log("WS open + registered uuid=" + shortCtx(pluginUUID));
		// Beim Reconnect: alle Contexts frisch sync-en, lastOn ruecksetzen
		// damit der erste setState garantiert durchgeht.
		actionContexts.forEach((state, ctx) => {
			state.lastOn = null;
			pollOnce(ctx);
		});
	};

	websocket.onmessage = (msg) => {
		let payload;
		try {
			payload = JSON.parse(msg.data);
		} catch {
			return;
		}

		const event = payload.event;
		const context = payload.context;

		if (event === "willAppear") {
			log("willAppear ctx=" + shortCtx(context));
			cleanupGhostContexts(context);
			startPolling(context);
		} else if (event === "willDisappear") {
			log("willDisappear ctx=" + shortCtx(context));
			stopPolling(context);
		} else if (event === "keyDown") {
			const state = actionContexts.get(context);
			if (state) state.lastSeen = Date.now();
			log("keyDown ctx=" + shortCtx(context));
			handleKeyDown(context);
		}
	};

	websocket.onerror = () => log("WS error");

	websocket.onclose = (ev) => {
		log("WS closed code=" + ev.code + " — reconnecting");
		websocket = null;
		setTimeout(openWebSocket, WS_RECONNECT_DELAY_MS);
	};
}

// Wenn ein NEUER context auftaucht und in unserer Map noch alte Contexts
// stehen die laenger als 5 Sek keine Stream-Deck-Aktivitaet hatten,
// behandeln wir die als Ghost und entfernen sie. Schuetzt vor dem
// "zwei Contexts pollen parallel"-Bug nach Plugin-Reinstall.
function cleanupGhostContexts(newContext) {
	const now = Date.now();
	const toRemove = [];
	actionContexts.forEach((state, ctx) => {
		if (ctx === newContext) return;
		const age = now - (state.lastSeen || 0);
		if (age > GHOST_CONTEXT_TIMEOUT_MS) {
			toRemove.push(ctx);
		}
	});
	toRemove.forEach((ctx) => {
		log(
			"GHOST cleanup ctx=" +
				shortCtx(ctx) +
				" (no activity > " +
				GHOST_CONTEXT_TIMEOUT_MS +
				"ms)",
		);
		stopPolling(ctx);
	});
}

function startPolling(context) {
	if (actionContexts.has(context)) {
		stopPolling(context);
	}
	const state = {
		timer: null,
		lastOn: null,
		toggleUntil: 0,
		lastSeen: Date.now(),
	};
	actionContexts.set(context, state);
	pollOnce(context);
	state.timer = setInterval(() => pollOnce(context), POLL_INTERVAL_MS);
}

function stopPolling(context) {
	const state = actionContexts.get(context);
	if (state && state.timer) {
		clearInterval(state.timer);
	}
	actionContexts.delete(context);
}

async function pollOnce(context) {
	const state = actionContexts.get(context);
	if (!state) return;
	// Toggle laeuft gerade — Poll-Update unterdruecken, sonst kommt der
	// alte HTTP-Response nach dem Toggle und ueberschreibt den frischen State.
	if (Date.now() < state.toggleUntil) {
		return;
	}
	try {
		const res = await fetch(TVO_STATUS_URL, {
			method: "GET",
			cache: "no-store",
		});
		if (!res.ok) throw new Error("HTTP " + res.status);
		const data = await res.json();
		applyStateIfChanged(context, !!data.on);
	} catch (err) {
		applyStateIfChanged(context, false, "offline");
	}
}

function applyStateIfChanged(context, on, statusText) {
	const state = actionContexts.get(context);
	if (!state) return;
	if (state.lastOn === on && !statusText) return;
	state.lastOn = on;
	sendSetState(context, on, statusText);
}

function forceSetState(context, on) {
	const state = actionContexts.get(context);
	if (!state) return;
	state.lastOn = on;
	sendSetState(context, on, undefined);
}

function sendSetState(context, on, statusText) {
	const desired = on ? 1 : 0;
	const wsState = websocket ? websocket.readyState : -1;
	log(
		"setState ctx=" +
			shortCtx(context) +
			" on=" +
			on +
			" desired=" +
			desired +
			" ws=" +
			wsState +
			(statusText ? " status=" + statusText : ""),
	);
	sendToStreamDeck({
		event: "setState",
		context: context,
		payload: { state: desired },
	});
	sendToStreamDeck({
		event: "setTitle",
		context: context,
		payload: { title: statusText || "", target: 0 },
	});
}

async function handleKeyDown(context) {
	const state = actionContexts.get(context);
	if (!state) return;
	// Polling fuer 800 ms pausieren, damit kein In-Flight-Poll den
	// Toggle ueberschreibt.
	state.toggleUntil = Date.now() + TOGGLE_PAUSE_MS;
	state.lastSeen = Date.now();
	try {
		const res = await fetch(TVO_TOGGLE_URL, {
			method: "POST",
			cache: "no-store",
		});
		if (!res.ok) throw new Error("HTTP " + res.status);
		const data = await res.json();
		const on = !!data.on;
		log("toggle response on=" + on);
		forceSetState(context, on);
	} catch (err) {
		log("toggle failed: " + err.message);
		sendToStreamDeck({ event: "showAlert", context: context });
	}
}

function sendToStreamDeck(message) {
	if (websocket && websocket.readyState === WebSocket.OPEN) {
		websocket.send(JSON.stringify(message));
	} else {
		log(
			"SEND-SKIPPED ws=" +
				(websocket ? websocket.readyState : "null") +
				" event=" +
				message.event,
		);
	}
}

function shortCtx(ctx) {
	if (!ctx) return "?";
	return ctx.length > 8 ? ctx.substring(0, 8) + ".." : ctx;
}

function log(message) {
	try {
		fetch(TVO_LOG_URL, {
			method: "POST",
			body: message,
			cache: "no-store",
		}).catch(() => {});
	} catch {
		/* ignore */
	}
}
