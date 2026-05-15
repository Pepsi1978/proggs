// ============================================================================
// TVO Auto-Enter Stream Deck Plugin — v5 (debounce + lastSeen-everywhere)
//
// v5-Aenderungen (2026-05-15, nach State-Drift-Vorfall):
//   * KEY-DEBOUNCE: handleKeyDown ignoriert keyDowns die weniger als
//     250 ms nach dem letzten kamen. Schuetzt vor Hardware-Bouncing und
//     doppelten OS-Events die sonst parallele Toggles erzeugen wuerden.
//   * lastSeen wird jetzt bei JEDEM Plugin-Event aktualisiert
//     (willAppear, keyDown, erfolgreichem Poll) — verhindert dass
//     Ghost-Cleanup einen legitimen Context killt der nur lange nicht
//     gedrueckt wurde.
//
// v4-Aenderungen (2026-05-15):
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
const KEY_DEBOUNCE_MS = 250;
const HEARTBEAT_INTERVAL_MS = 5000;

let websocket = null;
let pluginUUID = null;
let registerEvent = null;
let wsPort = null;
let heartbeatTimer = null;
let heartbeatCounter = 0;
let pollCounter = 0;

// context -> { timer, lastOn, toggleUntil, lastSeen, lastKeyDownTs }
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
	startHeartbeat();
}

// Heartbeat-Logger: alle 5 Sekunden eine "ALIVE"-Zeile in TVO-hotkey.log.
// Wenn der Plugin-Code lebt, sieht Frank diese Zeilen kontinuierlich.
// Wenn sie aufhoeren, ist der Plugin-Webview tot oder im Stream-Deck-
// Software-Editor pausiert. Damit ist sofort sichtbar wo das System
// "haengt" — keine Devtools noetig.
function startHeartbeat() {
	if (heartbeatTimer) clearInterval(heartbeatTimer);
	heartbeatTimer = setInterval(() => {
		heartbeatCounter++;
		const wsState = websocket ? websocket.readyState : -1;
		const ctxCount = actionContexts.size;
		log(
			"ALIVE i=" +
				heartbeatCounter +
				" polls=" +
				pollCounter +
				" ws=" +
				wsState +
				" ctxs=" +
				ctxCount,
		);
	}, HEARTBEAT_INTERVAL_MS);
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

		// lastSeen jetzt fuer ALLE Events aktualisiert — verhindert dass
		// ein Context als Ghost geraetselt wird, nur weil Frank ihn lange
		// nicht gedrueckt hat. Polling-Aktivitaet allein zaehlt.
		const stateForLastSeen = actionContexts.get(context);
		if (stateForLastSeen) stateForLastSeen.lastSeen = Date.now();

		if (event === "willAppear") {
			log("willAppear ctx=" + shortCtx(context));
			cleanupGhostContexts(context);
			startPolling(context);
		} else if (event === "willDisappear") {
			log("willDisappear ctx=" + shortCtx(context));
			stopPolling(context);
		} else if (event === "keyDown") {
			log("keyDown ctx=" + shortCtx(context));
			handleKeyDown(context);
		}
	};

	websocket.onerror = (ev) =>
		log("WS error type=" + (ev && ev.type) + " message=" + (ev && ev.message));

	websocket.onclose = (ev) => {
		log(
			"WS CLOSED code=" +
				ev.code +
				" reason=" +
				(ev.reason || "(none)") +
				" wasClean=" +
				ev.wasClean +
				" — reconnecting in " +
				WS_RECONNECT_DELAY_MS +
				"ms",
		);
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
		lastKeyDownTs: 0,
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
	pollCounter++;
	try {
		const res = await fetch(TVO_STATUS_URL, {
			method: "GET",
			cache: "no-store",
		});
		if (!res.ok) throw new Error("HTTP " + res.status);
		const data = await res.json();
		// Erfolgreicher Poll = lebendiger Context. Schuetzt ihn vor
		// Ghost-Cleanup, auch wenn er lange nicht gedrueckt wurde.
		state.lastSeen = Date.now();
		applyStateIfChanged(context, !!data.on);
	} catch (err) {
		log(
			"POLL ERROR ctx=" +
				shortCtx(context) +
				" name=" +
				(err && err.name) +
				" msg=" +
				(err && err.message),
		);
		applyStateIfChanged(context, false, "offline");
	}
}

function applyStateIfChanged(context, on, statusText) {
	const state = actionContexts.get(context);
	if (!state) return;
	// v1.0.5: DEDUP WIEDER eingebaut. v1.0.4 hatte es entfernt, was bei
	// stabilem TVO-State zu setState-Spam (1.4/s) fuehrte. Das hat im
	// Stream-Deck-Editor die State-Konfigurations-UI eingefroren — der
	// Editor konnte nie eine stabile Render-Pause haben.
	//
	// Mit DisableAutomaticStates=true (manifest v1.0.4+) kann die Anzeige
	// nicht mehr von der SDK in den falschen State gebracht werden — Plugin
	// ist die einzige Wahrheit. Force-Re-Sync per Poll-Tick ist deshalb
	// nicht mehr noetig; Dedup ist sicher.
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
	// v1.0.4: setTitle nur bei Status-Text (z.B. "offline"). Vorher haben wir
	// bei jedem setState einen leeren Title gesendet — das ueberschreibt
	// den Title den der Benutzer im Editor selbst gesetzt hat. Jetzt bleibt
	// Frank's Custom-Title erhalten.
	if (statusText) {
		sendToStreamDeck({
			event: "setTitle",
			context: context,
			payload: { title: statusText, target: 0 },
		});
	}
}

async function handleKeyDown(context) {
	const state = actionContexts.get(context);
	if (!state) return;
	const now = Date.now();
	// DEBOUNCE: wenn der letzte keyDown weniger als 250 ms her ist,
	// ignorieren. Schuetzt vor Hardware-Bouncing (manche Stream-Deck-
	// Tasten erzeugen zwei keyDowns) und davor dass parallele Toggles
	// den State-Sync zwischen Stream Deck und TVO durcheinander bringen.
	if (now - (state.lastKeyDownTs || 0) < KEY_DEBOUNCE_MS) {
		log(
			"keyDown DEBOUNCED (delta=" +
				(now - state.lastKeyDownTs) +
				"ms < " +
				KEY_DEBOUNCE_MS +
				"ms)",
		);
		return;
	}
	state.lastKeyDownTs = now;
	// Polling fuer 800 ms pausieren, damit kein In-Flight-Poll den
	// Toggle ueberschreibt.
	state.toggleUntil = now + TOGGLE_PAUSE_MS;
	state.lastSeen = now;
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
