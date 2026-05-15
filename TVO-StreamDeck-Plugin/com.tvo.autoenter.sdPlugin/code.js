// ============================================================================
// TVO Auto-Enter Stream Deck Plugin
//
// Spiegelt den Auto-Enter-Status des Terminal Voice Overlays (orange/grau)
// auf eine Stream-Deck-Taste UND erlaubt Toggle-Steuerung in die andere
// Richtung. Beide Seiten bleiben automatisch synchron — egal ob der User
// im Overlay per Maus klickt oder die Stream-Deck-Taste drueckt.
//
// v2 (2026-05-15): Caching weg. Frueher cachte das Plugin den letzten
// State und ueberspratzte gleiche-State-Updates — das fuehrte dazu dass
// die Taste nur EINMAL korrekt ihre Farbe wechselte und dann nicht mehr.
// Jetzt schickt jeder Poll-Tick ein setState/setTitle, Stream Deck
// dedupliziert intern. Zusaetzlich POST /log fuer Diagnose.
// ============================================================================

const TVO_STATUS_URL = "http://127.0.0.1:5723/autoenter/status";
const TVO_TOGGLE_URL = "http://127.0.0.1:5723/autoenter/toggle";
const TVO_LOG_URL = "http://127.0.0.1:5723/log";
const POLL_INTERVAL_MS = 500;

let websocket = null;
let pluginUUID = null;
let registerEvent = null;

// Pro action-context (eine pro Stream-Deck-Taste mit dieser Action)
// halten wir den Polling-Timer. lastOn wurde absichtlich entfernt —
// siehe Header-Kommentar.
const actionContexts = new Map(); // context -> { timer }

function connectElgatoStreamDeckSocket(
	inPort,
	inPluginUUID,
	inRegisterEvent,
	inInfo,
) {
	pluginUUID = inPluginUUID;
	registerEvent = inRegisterEvent;

	websocket = new WebSocket("ws://127.0.0.1:" + inPort);

	websocket.onopen = () => {
		websocket.send(
			JSON.stringify({
				event: inRegisterEvent,
				uuid: inPluginUUID,
			}),
		);
		log("WS open, registered as " + inPluginUUID);
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
		console.error("WebSocket error:", err);
	};
}

function startPolling(context) {
	if (actionContexts.has(context)) {
		stopPolling(context);
	}
	const state = { timer: null };
	actionContexts.set(context, state);

	// Sofort einmal pollen, danach im Intervall
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

// Sendet IMMER ein setState + setTitle — kein Caching mehr. Stream Deck
// dedupliziert intern und das spart uns die Bug-Klasse "lastOn ist
// nicht mehr synchron mit dem echten Tastenzustand".
function applyState(context, on, statusText) {
	const desired = on ? 1 : 0;
	sendToStreamDeck({
		event: "setState",
		context: context,
		payload: { state: desired },
	});
	sendToStreamDeck({
		event: "setTitle",
		context: context,
		payload: {
			title: statusText || "Auto-Enter",
			target: 0, // hardware + software
		},
	});
	// Diagnose-Log — nur jeden 4. Aufruf damit das Log nicht zumuellt.
	pollCounter++;
	if (pollCounter % 4 === 0) {
		log(
			"applyState ctx=" +
				shortCtx(context) +
				" on=" +
				on +
				" desired=" +
				desired +
				(statusText ? " status=" + statusText : ""),
		);
	}
}

let pollCounter = 0;

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
