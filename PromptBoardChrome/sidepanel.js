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

const STORE_KEY = "prompts";

function load() {
	// chrome.storage.local: zuverlaessig + grosszuegig (~5 MB). Frueher wurde
	// chrome.storage.sync genutzt — das hat aber nur 8 KB PRO EINTRAG; ein langer
	// Prompt (z.B. PDF) sprengte das Limit, set() schlug LAUTLOS fehl und der
	// Eintrag war nach dem Neuladen weg. Darum local + Migration aus sync.
	chrome.storage.local.get({ [STORE_KEY]: null }, (res) => {
		if (Array.isArray(res[STORE_KEY])) {
			prompts = res[STORE_KEY];
			render();
			return;
		}
		// Noch nichts in local -> evtl. liegen aeltere Prompts in sync. Uebernehmen.
		chrome.storage.sync.get({ prompts: null }, (s) => {
			if (Array.isArray(s.prompts) && s.prompts.length) {
				prompts = s.prompts;
			} else {
				prompts = DEFAULT_PROMPTS.slice();
			}
			save(); // nach local schreiben (Migration bzw. Defaults setzen)
			render();
		});
	});
}

function save() {
	chrome.storage.local.set({ [STORE_KEY]: prompts }, () => {
		if (chrome.runtime.lastError) {
			// Speichern fehlgeschlagen -> sichtbar warnen, nicht lautlos verlieren.
			showHint(
				"Konnte nicht speichern: " + chrome.runtime.lastError.message,
				true,
			);
		}
	});
}

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

load();
