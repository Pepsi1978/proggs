/*
 * insights.js — wertet die gesammelten Diagnose-Logs aus und leitet daraus
 * konkrete VERBESSERUNGSVORSCHLAEGE FUER DIE APP ab (nicht nur Fehler).
 *
 * Reine Analyse-Funktion: bekommt den JSONL-Text (aus diag.readAllJsonl()),
 * greift NICHT auf chrome-APIs zu — dadurch leicht testbar. Im Service-Worker
 * als globalThis.__voInsights() verfuegbar (siehe service-worker.js).
 *
 * Die Heuristiken sind bewusst konservativ (Mindest-Fallzahlen), damit aus
 * wenigen Datenpunkten keine voreiligen Schluesse gezogen werden.
 */

function avg(arr) {
	return arr.length
		? Math.round(arr.reduce((a, b) => a + b, 0) / arr.length)
		: 0;
}

export function analyze(jsonlText) {
	const zeilen = String(jsonlText || "")
		.split("\n")
		.filter(Boolean);
	const ev = [];
	for (const z of zeilen) {
		try {
			ev.push(JSON.parse(z));
		} catch (e) {
			/* defekte Zeile ueberspringen */
		}
	}

	const mitEreignis = (name) => ev.filter((e) => e.ereignis === name);
	const zahl = (arr, feld) =>
		arr
			.map((e) => e.inhalt && e.inhalt[feld])
			.filter((n) => typeof n === "number");

	const latenzen = zahl(mitEreignis("synthese_latenz"), "ms");
	const dauern = zahl(mitEreignis("wiedergabe_dauer"), "ms");
	const stops = mitEreignis("manueller_stop");
	const frueh = stops.filter((e) => e.inhalt && e.inhalt.frueh_abgebrochen);
	// Echte Vorlese-Vorgaenge: der Klick im Overlay (Tests des Runners zaehlen mit,
	// sind aber im Normalbetrieb selten).
	const speaks = ev.filter((e) => e.ereignis === "speaker_klick:start");
	const fehler = ev.filter((e) => e.stufe === "ERROR" || e.stufe === "FATAL");
	const edgeFehler = fehler.filter((e) => /edge/i.test(e.ereignis));
	const googleFehler = fehler.filter((e) => /google/i.test(e.ereignis));
	const edgeErfolg = mitEreignis("edge.synthesize:erfolg");
	const googleErfolg = mitEreignis("google.synthesize:erfolg");
	const layoutWarn = ev.filter(
		(e) =>
			e.kategorie === "LAYOUT" &&
			/teilweise_ausserhalb|ueberlauf/.test(e.ereignis),
	);
	const webaudioFallback = mitEreignis("offscreen.webaudio_fallback");

	const engine_nutzung = {};
	const stimmen_nutzung = {};
	for (const e of speaks) {
		const eng = e.inhalt && e.inhalt.engine;
		if (eng) engine_nutzung[eng] = (engine_nutzung[eng] || 0) + 1;
		const v = e.inhalt && e.inhalt.voice;
		if (v) stimmen_nutzung[v] = (stimmen_nutzung[v] || 0) + 1;
	}

	const kennzahlen = {
		eintraege: ev.length,
		vorlese_vorgaenge: speaks.length,
		synthese_latenz_schnitt_ms: avg(latenzen),
		synthese_latenz_max_ms: latenzen.length ? Math.max(...latenzen) : 0,
		wiedergabe_dauer_schnitt_ms: avg(dauern),
		manuelle_stops: stops.length,
		frueh_abgebrochen: frueh.length,
		fehler_gesamt: fehler.length,
		edge: { erfolg: edgeErfolg.length, fehler: edgeFehler.length },
		google: { erfolg: googleErfolg.length, fehler: googleFehler.length },
		engine_nutzung,
		stimmen_nutzung,
		layout_warnungen: layoutWarn.length,
		webaudio_fallbacks: webaudioFallback.length,
	};

	const vorschlaege = [];
	const add = (prioritaet, thema, befund, vorschlag) =>
		vorschlaege.push({ prioritaet, thema, befund, vorschlag });

	// 1) Synthese-Latenz
	if (kennzahlen.synthese_latenz_schnitt_ms > 2500)
		add(
			"hoch",
			"Latenz",
			`Synthese braucht im Schnitt ${kennzahlen.synthese_latenz_schnitt_ms} ms bis zum ersten Ton (max ${kennzahlen.synthese_latenz_max_ms} ms).`,
			"Das erste Text-Stueck kleiner chunken (schneller hoerbar) oder eine deutlichere Ladeanzeige zeigen.",
		);

	// 2) Frueher Abbruch
	const stopRate = stops.length ? frueh.length / stops.length : 0;
	if (frueh.length >= 3 && stopRate > 0.4)
		add(
			"hoch",
			"Abbruch",
			`${frueh.length} von ${stops.length} Stopps erfolgten in den ersten 2 Sekunden.`,
			"Default-Stimme/Tempo ueberdenken — der Nutzer bricht haeufig sofort ab.",
		);

	// 3) Edge-Stabilitaet
	const edgeGesamt = edgeErfolg.length + edgeFehler.length;
	if (edgeGesamt >= 5 && edgeFehler.length / edgeGesamt > 0.2)
		add(
			"hoch",
			"Edge-Stabilitaet",
			`Edge schlug in ${edgeFehler.length} von ${edgeGesamt} Faellen fehl.`,
			"Automatischen Fallback auf Google anbieten oder den Edge-Retry erhoehen.",
		);

	// 4) Google-Fehler
	const googleGesamt = googleErfolg.length + googleFehler.length;
	if (googleGesamt >= 3 && googleFehler.length / googleGesamt > 0.3)
		add(
			"mittel",
			"Google-Fehler",
			`Google schlug in ${googleFehler.length} von ${googleGesamt} Faellen fehl.`,
			"Key-/API-Setup im Onboarding verstaendlicher machen (haeufigste Ursache: ungueltiger Key oder nicht aktivierte API).",
		);

	// 5) Web-Audio-Fallback
	if (webaudioFallback.length > 0)
		add(
			"mittel",
			"Audio",
			`Die Wiedergabe ist ${webaudioFallback.length}x von Web Audio auf das HTMLAudioElement zurueckgefallen.`,
			"Ursache pruefen — sonst greift der Live-Caption-Workaround nicht und es koennen unerwuenschte Untertitel erscheinen.",
		);

	// 6) Layout
	if (kennzahlen.layout_warnungen > 0)
		add(
			"hoch",
			"Layout",
			`${kennzahlen.layout_warnungen} Layout-Markierung(en): Overlay oder Panel ragt aus dem sichtbaren Bereich.`,
			"Positionierung/Clamping fuer die betroffene Bildschirm-/Fenstergroesse korrigieren.",
		);

	// 7) Wiedergabe-Dauer (sehr kurz = hoeren nicht zu Ende)
	if (dauern.length >= 5 && kennzahlen.wiedergabe_dauer_schnitt_ms < 3000)
		add(
			"mittel",
			"Wiedergabe",
			`Im Schnitt wird nur ${kennzahlen.wiedergabe_dauer_schnitt_ms} ms vorgelesen.`,
			"Pruefen, ob Stimme/Tempo passen oder ob die Texterkennung zu kurze Stuecke liefert.",
		);

	// 8) Stimmen-Nutzung
	const genutzteStimmen = Object.keys(stimmen_nutzung);
	if (genutzteStimmen.length === 1 && speaks.length >= 5)
		add(
			"niedrig",
			"Stimmen",
			`Es wird ausschliesslich "${genutzteStimmen[0]}" genutzt.`,
			"Diese Stimme als Default setzen; selten genutzte Stimmen in der Liste nach hinten sortieren.",
		);

	// 9) Engine-Nutzung
	const engines = Object.keys(engine_nutzung);
	if (engines.length === 1 && engines[0] === "edge" && speaks.length >= 5)
		add(
			"niedrig",
			"Engine",
			"Es wird ausschliesslich Edge genutzt (Google nie).",
			"Pruefen, ob die Google-Einrichtung zu kompliziert ist — ggf. Onboarding verbessern.",
		);

	if (!vorschlaege.length)
		add(
			"info",
			"Allgemein",
			"Keine auffaelligen Muster in den vorliegenden Daten.",
			"Weiter Daten sammeln — bei mehr Nutzung werden die Vorschlaege aussagekraeftiger.",
		);

	const rang = { hoch: 0, mittel: 1, niedrig: 2, info: 3 };
	vorschlaege.sort(
		(a, b) => (rang[a.prioritaet] ?? 9) - (rang[b.prioritaet] ?? 9),
	);

	return { kennzahlen, vorschlaege };
}
