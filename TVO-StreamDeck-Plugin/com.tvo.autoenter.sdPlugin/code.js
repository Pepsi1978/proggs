// ============================================================================
// TVO Auto-Enter Stream Deck Plugin
//
// Spiegelt den Auto-Enter-Status des Terminal Voice Overlays (orange/grau)
// auf eine Stream-Deck-Taste UND erlaubt Toggle-Steuerung in die andere
// Richtung. Beide Seiten bleiben automatisch synchron — egal ob der User
// im Overlay per Maus klickt oder die Stream-Deck-Taste drueckt.
//
// Kommunikation:
//   - Plugin <-> Stream-Deck-Software: WebSocket (Elgato SDK v2)
//   - Plugin <-> TVO:                  HTTP (127.0.0.1:5723)
//
// Plugin-Verhalten:
//   * Pro action-Instanz wird beim "willAppear"-Event ein Polling-Loop
//     gestartet: alle 500 ms wird GET /autoenter/status abgefragt, der
//     Tastenzustand wird auf 0 (grau) oder 1 (orange) gesetzt.
//   * Bei "keyDown" wird POST /autoenter/toggle geschickt. Der Server
//     antwortet mit dem neuen Stand, den wir direkt zurueckschreiben —
//     keine Wartezeit bis zum naechsten Poll.
//   * "willDisappear" stoppt den Polling-Loop fuer die Instanz.
// ============================================================================

const TVO_STATUS_URL = "http://127.0.0.1:5723/autoenter/status";
const TVO_TOGGLE_URL = "http://127.0.0.1:5723/autoenter/toggle";
const POLL_INTERVAL_MS = 500;

// Wird von der Stream-Deck-Software beim Plugin-Start aufgerufen. Die
// Parameter teilen uns den WebSocket-Port und die UUIDs mit.
let websocket = null;
let pluginUUID = null;
let registerEvent = null;

// Pro action-context (einzigartig pro Stream-Deck-Taste mit dieser Action)
// halten wir den Polling-Timer und den zuletzt bekannten State.
const actionContexts = new Map(); // context -> { timer, lastOn }

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
		// Plugin bei der Stream-Deck-Software registrieren
		websocket.send(
			JSON.stringify({
				event: inRegisterEvent,
				uuid: inPluginUUID,
			}),
		);
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
			startPolling(context);
		} else if (event === "willDisappear") {
			stopPolling(context);
		} else if (event === "keyDown") {
			handleKeyDown(context);
		}
	};

	websocket.onerror = (err) => {
		// Nichts zu tun — Stream-Deck-Software macht den Reconnect alleine
		// wenn sie unsere HTML-Seite neu laedt.
		console.error("WebSocket error:", err);
	};
}

function startPolling(context) {
	if (actionContexts.has(context)) {
		stopPolling(context);
	}
	const state = { timer: null, lastOn: null };
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
		// TVO laeuft nicht oder Port belegt. Wir setzen die Taste auf
		// State 0 (grau) und schreiben dezent "offline" in den Title,
		// damit der User merkt dass das Plugin keine Verbindung hat.
		applyState(context, false, "offline");
	}
}

function applyState(context, on, statusText) {
	const state = actionContexts.get(context);
	const desired = on ? 1 : 0;

	// Nur senden wenn sich was geaendert hat — spart WebSocket-Traffic
	// und vermeidet Flackern.
	if (!state || state.lastOn !== on || statusText !== undefined) {
		if (state) state.lastOn = on;
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
	}
}

async function handleKeyDown(context) {
	try {
		const res = await fetch(TVO_TOGGLE_URL, {
			method: "POST",
			cache: "no-store",
		});
		if (!res.ok) throw new Error("HTTP " + res.status);
		const data = await res.json();
		applyState(context, !!data.on);
	} catch (err) {
		// TVO antwortet nicht — kurze visuelle Rueckmeldung: showAlert
		// (kleines gelbes Warndreieck auf der Taste).
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
