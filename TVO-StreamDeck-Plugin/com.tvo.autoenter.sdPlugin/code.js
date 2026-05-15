// ============================================================================
// TVO Auto-Enter Stream Deck Plugin — v3 (stabil)
//
// v3-Aenderungen (2026-05-15, nach "10 Versuche dann tot"-Bug):
//   * WebSocket auto-reconnect bei onclose/onerror — ohne das verschluckt
//     das Plugin alle setState-Nachrichten nachdem die Stream-Deck-Software
//     den Socket schliesst (Inactivity-Timeout oder Service-Reload).
//   * Jeder setState wird UNCONDITIONAL geloggt (kein Throttling mehr), so
//     dass jede Plugin-Aktion live in TVO-hotkey.log sichtbar ist.
//   * sendToStreamDeck loggt explizit wenn der WebSocket NICHT offen ist —
//     damit "Taste reagiert aber Farbe wechselt nicht"-Faelle eindeutig in
//     den Logs auftauchen.
//   * keyDown loest nach dem Toggle ZWEI applyState-Calls aus (sofort +
//     200 ms spaeter), damit Stream Deck auch bei schnellen Mehrfach-Klicks
//     den letzten State garantiert anzeigt.
// ============================================================================

const TVO_STATUS_URL = "http://127.0.0.1:5723/autoenter/status";
const TVO_TOGGLE_URL = "http://127.0.0.1:5723/autoenter/toggle";
const TVO_LOG_URL = "http://127.0.0.1:5723/log";
const POLL_INTERVAL_MS = 500;
const WS_RECONNECT_DELAY_MS = 1000;

let websocket = null;
let pluginUUID = null;
let registerEvent = null;
let wsPort = null;

const actionContexts = new Map(); // context -> { timer }

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
		log(
			"WS construction failed: " +
				e.message +
				" — retry in " +
				WS_RECONNECT_DELAY_MS +
				"ms",
		);
		setTimeout(openWebSocket, WS_RECONNECT_DELAY_MS);
		return;
	}

	websocket.onopen = () => {
		websocket.send(
			JSON.stringify({
				event: registerEvent,
				uuid: pluginUUID,
			}),
		);
		log("WS open + registered (uuid=" + shortCtx(pluginUUID) + ")");
		// Beim Reconnect: alle bekannten Action-Instances einmal frisch
		// sync-en, damit die Tasten sofort die richtige Farbe haben.
		actionContexts.forEach((_state, ctx) => pollOnce(ctx));
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
			startPolling(context);
		} else if (event === "willDisappear") {
			log("willDisappear ctx=" + shortCtx(context));
			stopPolling(context);
		} else if (event === "keyDown") {
			log("keyDown ctx=" + shortCtx(context));
			handleKeyDown(context);
		}
	};

	websocket.onerror = (err) => {
		log("WS error");
	};

	websocket.onclose = (ev) => {
		log(
			"WS closed code=" +
				ev.code +
				" reason=" +
				(ev.reason || "(empty)") +
				" — reconnecting in " +
				WS_RECONNECT_DELAY_MS +
				"ms",
		);
		websocket = null;
		setTimeout(openWebSocket, WS_RECONNECT_DELAY_MS);
	};
}

function startPolling(context) {
	if (actionContexts.has(context)) {
		stopPolling(context);
	}
	const state = { timer: null };
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
	try {
		const res = await fetch(TVO_STATUS_URL, {
			method: "GET",
			cache: "no-store",
		});
		if (!res.ok) throw new Error("HTTP " + res.status);
		const data = await res.json();
		applyState(context, !!data.on);
	} catch (err) {
		applyState(context, false, "offline");
	}
}

// Sendet IMMER setState + setTitle, kein Caching. Loggt unconditional.
function applyState(context, on, statusText) {
	const desired = on ? 1 : 0;
	const wsState = websocket ? websocket.readyState : -1;
	log(
		"applyState ctx=" +
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
		payload: {
			title: statusText || "",
			target: 0,
		},
	});
}

async function handleKeyDown(context) {
	try {
		const res = await fetch(TVO_TOGGLE_URL, {
			method: "POST",
			cache: "no-store",
		});
		if (!res.ok) throw new Error("HTTP " + res.status);
		const data = await res.json();
		log("toggle response on=" + data.on);
		applyState(context, !!data.on);
		// Doppelte Sicherung: bei sehr schnellen Klicks kann Stream Deck
		// setState verschlucken. 200 ms spaeter pollen wir frisch.
		setTimeout(() => pollOnce(context), 200);
	} catch (err) {
		log("toggle failed: " + err.message);
		sendToStreamDeck({
			event: "showAlert",
			context: context,
		});
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

// Schreibt eine Zeile nach TVO-hotkey.log via Plugin-Diagnose-Endpunkt.
// Fire-and-forget, niemals blockieren.
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
