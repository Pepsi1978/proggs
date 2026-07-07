// Side panel logic: load/save/edit prompts and send the chosen prompt to the
// active tab's content script for insertion.

const DEFAULT_PROMPTS = [
	{
		id: "p1",
		label: "Einfach erklären",
		text: "Erkläre das bitte einfach und verständlich, als wäre ich kein Experte.",
	},
	{
		id: "p2",
		label: "Zusammenfassen",
		text: "Fasse den folgenden Text kurz und prägnant zusammen:\n\n",
	},
	{
		id: "p3",
		label: "Auf Englisch übersetzen",
		text: "Übersetze den folgenden Text ins Englische:\n\n",
	},
	{
		id: "p4",
		label: "Rechtschreibung korrigieren",
		text: "Korrigiere Rechtschreibung und Grammatik, ohne den Stil zu verändern:\n\n",
	},
	{
		id: "p5",
		label: "Kürzer machen",
		text: "Mach den folgenden Text deutlich kürzer, ohne wichtige Informationen zu verlieren:\n\n",
	},
	{
		id: "p6",
		label: "Schritt für Schritt",
		text: "Erkläre die Lösung Schritt für Schritt und begründe jeden Schritt.",
	},
];

const listEl = document.getElementById("list");
const hintEl = document.getElementById("hint");
const editArea = document.getElementById("editArea");
const editToggle = document.getElementById("editToggle");

let prompts = [];
let editMode = false;
let hintTimer = null;
let reorderDrag = null; // aktiver Umsortier-Zug (rechte Maustaste)

function uid() {
	return "p" + Date.now() + Math.floor(Math.random() * 1000);
}

// Doppelte Speicherung:
// (1) chrome.storage.local = ZUVERLAESSIGE Quelle fuer DIESES Geraet (schnell,
//     kein Sync-Timing/Flakiness) -> beim Neuladen NIE Datenverlust.
// (2) chrome.storage.sync = Chromes Google-Backup fuer den Abgleich zwischen
//     Geraeten; jeder Prompt als eigener Eintrag (umgeht das 8-KB-pro-Eintrag-Limit),
//     feste Extension-ID (key) sorgt fuer denselben Sync-Speicher auf Win/macOS.
// Ein Zeitstempel entscheidet bei Unterschieden, welche Seite neuer ist (last write wins).
const LOCAL_KEY = "prompts"; // ganze Liste in local
const LOCAL_TS = "prompts_ts";
const ORDER_KEY = "pb_order";
const ITEM_PREFIX = "pb_p_";
const TS_KEY = "pb_ts";
let applyingRemote = false; // verhindert Rueckschreiben bei Remote-Aenderungen
let currentTs = 0; // Zeitstempel des aktuell geladenen Standes

function promptsFromStore(all) {
	const order = all && Array.isArray(all[ORDER_KEY]) ? all[ORDER_KEY] : null;
	if (!order) return null;
	return order
		.map((id) => all[ITEM_PREFIX + id])
		.filter((p) => p && typeof p === "object" && p.id);
}

function load() {
	chrome.storage.local.get({ [LOCAL_KEY]: null, [LOCAL_TS]: 0 }, (loc) => {
		const localList =
			Array.isArray(loc[LOCAL_KEY]) && loc[LOCAL_KEY].length
				? loc[LOCAL_KEY]
				: null;
		const localTs = Number(loc[LOCAL_TS]) || 0;
		chrome.storage.sync.get(null, (all) => {
			const syncList = !chrome.runtime.lastError ? promptsFromStore(all) : null;
			const syncTs = (all && Number(all[TS_KEY])) || 0;

			if (syncList && syncList.length && syncTs > localTs) {
				// Cloud (anderes Geraet) ist neuer -> uebernehmen + lokal spiegeln.
				prompts = syncList;
				currentTs = syncTs;
				writeLocal(currentTs);
			} else if (localList) {
				// Lokaler Stand ist die zuverlaessige Quelle dieses Geraets.
				prompts = localList;
				currentTs = localTs;
				if (syncTs < localTs) writeSync(currentTs); // Cloud nachziehen
			} else if (syncList && syncList.length) {
				prompts = syncList;
				currentTs = syncTs;
				writeLocal(currentTs);
			} else {
				prompts = DEFAULT_PROMPTS.slice();
				save();
			}
			render();
		});
	});
}

function writeLocal(ts) {
	chrome.storage.local.set({ [LOCAL_KEY]: prompts, [LOCAL_TS]: ts }, () => {
		if (chrome.runtime.lastError) {
			showHint(
				"Speichern (lokal) fehlgeschlagen: " + chrome.runtime.lastError.message,
				true,
			);
		}
	});
}

function writeSync(ts) {
	chrome.storage.sync.get(null, (all) => {
		if (chrome.runtime.lastError) return; // lokal ist bereits gesichert
		const obj = { [ORDER_KEY]: prompts.map((p) => p.id), [TS_KEY]: ts };
		for (const p of prompts) obj[ITEM_PREFIX + p.id] = p;
		const keep = new Set(prompts.map((p) => p.id));
		const remove = Object.keys(all || {}).filter(
			(k) =>
				k.startsWith(ITEM_PREFIX) && !keep.has(k.slice(ITEM_PREFIX.length)),
		);
		chrome.storage.sync.set(obj, () => {
			if (chrome.runtime.lastError) {
				// Lokal ist schon sicher gespeichert -> nur Hinweis, kein Datenverlust.
				showHint(
					"Cloud-Sync fehlgeschlagen (lokal gespeichert): " +
						chrome.runtime.lastError.message,
					true,
				);
			}
		});
		if (remove.length) chrome.storage.sync.remove(remove);
	});
}

function save() {
	if (applyingRemote) return; // gerade Remote-Stand uebernommen
	currentTs = Date.now();
	writeLocal(currentTs); // zuerst zuverlaessig lokal sichern
	writeSync(currentTs); // dann ins Google-Backup spiegeln
}

// Aenderungen von einem anderen Geraet live uebernehmen (nicht waehrend des
// Bearbeitens, um laufende Eingaben nicht zu ueberschreiben).
chrome.storage.onChanged.addListener((changes, area) => {
	if (area !== "sync" || editMode) return;
	const touched = Object.keys(changes).some(
		(k) => k === ORDER_KEY || k === TS_KEY || k.startsWith(ITEM_PREFIX),
	);
	if (!touched) return;
	chrome.storage.sync.get(null, (all) => {
		if (chrome.runtime.lastError) return;
		const list = promptsFromStore(all);
		const syncTs = (all && Number(all[TS_KEY])) || 0;
		// Nur uebernehmen, wenn die Cloud NEUER ist als unser aktueller Stand.
		if (!list || !list.length || syncTs < currentTs) return;
		applyingRemote = true;
		prompts = list;
		currentTs = syncTs;
		writeLocal(syncTs); // lokal spiegeln
		render();
		applyingRemote = false;
	});
});

function showHint(msg, isError) {
	hintEl.textContent = msg;
	hintEl.classList.toggle("pb-error", !!isError);
	hintEl.hidden = false;
	clearTimeout(hintTimer);
	hintTimer = setTimeout(() => {
		hintEl.hidden = true;
	}, 3500);
}

// Try to deliver the insert message to the content script. Resolves true on
// success, false if no frame answered (e.g. the script isn't present yet).
function trySend(tabId, text) {
	return new Promise((resolve) => {
		chrome.tabs.sendMessage(tabId, { type: "insertPrompt", text }, (resp) => {
			resolve(!chrome.runtime.lastError && resp && resp.ok);
		});
	});
}

async function sendPrompt(text) {
	try {
		const [tab] = await chrome.tabs.query({
			active: true,
			currentWindow: true,
		});
		if (!tab || !tab.id) {
			showHint("Keine aktive Seite gefunden.", true);
			return;
		}

		// First attempt: content script is usually already present.
		if (await trySend(tab.id, text)) {
			showHint("Eingefügt ✓", false);
			return;
		}

		// Self-heal: the tab may have been open before the extension loaded, so
		// no content script is running yet. Inject it now and retry once.
		try {
			await chrome.scripting.executeScript({
				target: { tabId: tab.id, allFrames: true },
				files: ["content.js"],
			});
			await new Promise((r) => setTimeout(r, 120));
			if (await trySend(tab.id, text)) {
				showHint("Eingefügt ✓", false);
				return;
			}
		} catch (_) {
			/* page disallows injection (chrome://, Web Store, PDF) */
		}

		showHint(
			"Kein Textfeld gefunden – klicke in ein Eingabefeld auf der Seite und versuche es erneut.",
			true,
		);
	} catch (e) {
		showHint("Fehler: " + (e && e.message), true);
	}
}

// --- Rendering -------------------------------------------------------------

function renderPromptButton(p) {
	const row = document.createElement("div");
	row.className = "pb-row";
	row.dataset.id = p.id;

	const btn = document.createElement("button");
	btn.className = "pb-prompt";
	btn.textContent = p.label;
	btn.title =
		p.text +
		"\n\n(Linksklick: einfügen · rechte Maustaste gedrückt halten und ziehen: Reihenfolge ändern)";
	// Linksklick = einfügen.
	btn.addEventListener("click", () => sendPrompt(p.text));
	// Rechte Maustaste gedrückt halten + ziehen = Reihenfolge ändern (kein Einfügen,
	// kein Kontextmenü). Bewusst eine andere Geste als der Linksklick.
	btn.addEventListener("mousedown", (e) => {
		if (e.button !== 2) return; // nur rechte Maustaste
		e.preventDefault();
		startReorder(row);
	});
	btn.addEventListener("contextmenu", (e) => e.preventDefault());

	row.appendChild(btn);
	return row;
}

// --- Umsortieren per Rechts-Drag (nur im normalen Modus) -------------------

function startReorder(rowEl) {
	const startOrder = Array.from(listEl.querySelectorAll(".pb-row")).map(
		(r) => r.dataset.id,
	);
	reorderDrag = { rowEl, startOrder };
	rowEl.classList.add("pb-dragging");
	document.addEventListener("mousemove", onReorderMove, true);
	document.addEventListener("mouseup", onReorderUp, true);
}

function onReorderMove(e) {
	if (!reorderDrag) return;
	e.preventDefault();
	const rows = Array.from(listEl.querySelectorAll(".pb-row"));
	// Erste Zeile finden, deren Mitte unterhalb des Mauszeigers liegt -> davor
	// einsortieren; sonst ans Ende. Das verschiebt die gezogene Zeile live.
	const after = rows.find((r) => {
		if (r === reorderDrag.rowEl) return false;
		const rect = r.getBoundingClientRect();
		return e.clientY < rect.top + rect.height / 2;
	});
	if (after) {
		listEl.insertBefore(reorderDrag.rowEl, after);
	} else {
		listEl.appendChild(reorderDrag.rowEl);
	}
}

function onReorderUp() {
	document.removeEventListener("mousemove", onReorderMove, true);
	document.removeEventListener("mouseup", onReorderUp, true);
	if (!reorderDrag) return;
	const rowEl = reorderDrag.rowEl;
	const startOrder = reorderDrag.startOrder;
	reorderDrag = null;
	rowEl.classList.remove("pb-dragging");

	const order = Array.from(listEl.querySelectorAll(".pb-row")).map(
		(r) => r.dataset.id,
	);
	if (order.join("|") !== startOrder.join("|")) {
		// Neue Reihenfolge in die Prompt-Liste uebernehmen + speichern.
		prompts.sort((a, b) => order.indexOf(a.id) - order.indexOf(b.id));
		save();
		showHint("Reihenfolge gespeichert ✓", false);
	}
}

function renderEditCard(p, index) {
	const card = document.createElement("div");
	card.className = "pb-edit-card";

	const head = document.createElement("div");
	head.className = "pb-edit-card-head";

	const labelInput = document.createElement("input");
	labelInput.type = "text";
	labelInput.className = "pb-edit-label";
	labelInput.maxLength = 40;
	labelInput.value = p.label;
	labelInput.placeholder = "Kurzname";
	// Save on blur/change so editing the text field keeps focus (no live re-render).
	labelInput.addEventListener("change", () => {
		const v = labelInput.value.trim();
		prompts[index].label = v || prompts[index].label;
		labelInput.value = prompts[index].label;
		save();
		showHint("Gespeichert ✓", false);
	});

	const del = document.createElement("button");
	del.className = "pb-del";
	del.textContent = "✕ Löschen";
	del.title = "Diesen Prompt löschen";
	del.addEventListener("click", () => {
		prompts = prompts.filter((x) => x.id !== p.id);
		save();
		render();
	});

	head.appendChild(labelInput);
	head.appendChild(del);

	const textArea = document.createElement("textarea");
	textArea.className = "pb-edit-text";
	textArea.rows = 3;
	textArea.value = p.text;
	textArea.placeholder = "Einzufügender Prompt-Text";
	textArea.addEventListener("change", () => {
		prompts[index].text = textArea.value;
		save();
		showHint("Gespeichert ✓", false);
	});

	card.appendChild(head);
	card.appendChild(textArea);
	return card;
}

function render() {
	listEl.innerHTML = "";
	prompts.forEach((p, index) => {
		listEl.appendChild(
			editMode ? renderEditCard(p, index) : renderPromptButton(p),
		);
	});
}

// --- Edit mode + add -------------------------------------------------------

editToggle.addEventListener("click", () => {
	editMode = !editMode;
	editArea.hidden = !editMode;
	editToggle.textContent = editMode ? "Fertig" : "Bearbeiten";
	render();
});

document.getElementById("addBtn").addEventListener("click", () => {
	const labelEl = document.getElementById("newLabel");
	const textEl = document.getElementById("newText");
	const label = labelEl.value.trim();
	const text = textEl.value;
	if (!label || !text.trim()) {
		showHint("Bitte Kurzname und Prompt-Text ausfüllen.", true);
		return;
	}
	prompts.push({ id: uid(), label, text });
	save();
	render();
	labelEl.value = "";
	textEl.value = "";
	showHint("Prompt hinzugefügt ✓", false);
});

// --- Refresh button: reload the extension AND the active page --------------
// runtime.reload() reloads the unpacked extension from disk (picks up new code)
// but destroys this side panel, so we hand the page reload to the background
// service worker via a stored flag (see background.js).

document.getElementById("reloadBtn").addEventListener("click", async () => {
	showHint("Aktualisiere Erweiterung & Seite …", false);
	try {
		const [tab] = await chrome.tabs.query({
			active: true,
			currentWindow: true,
		});
		if (tab && typeof tab.id === "number") {
			await chrome.storage.local.set({ pendingReloadTabId: tab.id });
		}
	} catch (_) {
		/* ignore – still reload the extension */
	}
	chrome.runtime.reload();
});

// Versions-Stempel unten anzeigen (aus dem Manifest).
const versionEl = document.getElementById("version");
if (versionEl) {
	versionEl.textContent = "Version " + chrome.runtime.getManifest().version;
}

load();
