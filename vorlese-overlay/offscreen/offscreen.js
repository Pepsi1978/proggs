/*
 * offscreen.js — Audio-Wiedergabe im versteckten Offscreen-Dokument.
 *
 * Empfaengt fertige Audiodaten (als Data-URL) vom Service-Worker und spielt
 * sie ueber ein <audio>-Element ab. Mehrere Stuecke werden in einer Queue
 * nacheinander nahtlos abgespielt. STOP leert die Queue sofort.
 *
 * Meldet Start/Ende/Fehler zurueck an den Service-Worker.
 */
(function () {
	"use strict";

	const queue = [];
	let current = null;
	let playing = false;

	function send(payload) {
		try {
			chrome.runtime.sendMessage(
				Object.assign({ target: "background" }, payload),
				() => void chrome.runtime.lastError,
			);
		} catch (e) {
			/* Worker evtl. inaktiv */
		}
	}

	function playNext() {
		if (queue.length === 0) {
			playing = false;
			current = null;
			send({ type: "OFFSCREEN_ENDED" });
			return;
		}
		playing = true;
		const url = queue.shift();
		const audio = new Audio(url);
		current = audio;

		audio.onended = () => {
			if (current === audio) playNext();
		};
		audio.onerror = () => {
			if (current === audio) {
				send({
					type: "OFFSCREEN_ERROR",
					message: "Audio konnte nicht abgespielt werden.",
				});
				playNext();
			}
		};
		audio.play().catch((e) => {
			send({
				type: "OFFSCREEN_ERROR",
				message: String(e && e.message ? e.message : e),
			});
			if (current === audio) playNext();
		});
	}

	function stopAll() {
		queue.length = 0;
		if (current) {
			try {
				current.pause();
				current.src = "";
			} catch (e) {
				/* egal */
			}
		}
		current = null;
		playing = false;
	}

	chrome.runtime.onMessage.addListener((msg) => {
		if (!msg || msg.target !== "offscreen") return;
		switch (msg.type) {
			case "ENQUEUE":
				queue.push(msg.url);
				if (!playing) {
					send({ type: "OFFSCREEN_STARTED" });
					playNext();
				}
				break;
			case "STOP":
				stopAll();
				break;
		}
	});
})();
