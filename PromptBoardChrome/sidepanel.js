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

function uid() {
	return "p" + Date.now() + Math.floor(Math.random() * 1000);
}

function load() {
	chrome.storage.sync.get({ prompts: null }, (res) => {
		if (Array.isArray(res.prompts)) {
			prompts = res.prompts;
		} else {
			prompts = DEFAULT_PROMPTS.slice();
			save(); // seed defaults on first run
		}
		render();
	});
}

function save() {
	chrome.storage.sync.set({ prompts });
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
		chrome.tabs.sendMessage(tab.id, { type: "insertPrompt", text }, (resp) => {
			if (chrome.runtime.lastError || !resp || !resp.ok) {
				showHint(
					"Kein Textfeld gefunden – klicke zuerst in ein Eingabefeld auf der Seite (Seite ggf. neu laden).",
					true,
				);
			} else {
				showHint("Eingefügt ✓", false);
			}
		});
	} catch (e) {
		showHint("Fehler: " + (e && e.message), true);
	}
}

function render() {
	listEl.innerHTML = "";
	prompts.forEach((p) => {
		const row = document.createElement("div");
		row.className = "pb-row";

		const btn = document.createElement("button");
		btn.className = "pb-prompt";
		btn.textContent = p.label;
		btn.title = p.text;
		btn.addEventListener("click", () => sendPrompt(p.text));
		row.appendChild(btn);

		if (editMode) {
			const del = document.createElement("button");
			del.className = "pb-del";
			del.textContent = "✕";
			del.title = "Löschen";
			del.addEventListener("click", () => {
				prompts = prompts.filter((x) => x.id !== p.id);
				save();
				render();
			});
			row.appendChild(del);
		}

		listEl.appendChild(row);
	});
}

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

load();
