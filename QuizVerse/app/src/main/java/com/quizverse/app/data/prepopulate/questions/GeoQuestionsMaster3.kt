package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun geoQuestionsMaster3(): List<Question> = listOf(

    // ── GEOMORPHOLOGIE, PLATTENTEKTONIK & EXTREMES WISSEN ────────────────────

    // Question 1 — Isostasie und Gletscherrebound  [correct: A → correctAnswer=0]
    Question(
        categoryId = 1,
        questionText = "Skandinavien und Kanada heben sich seit dem Ende der letzten Eiszeit noch immer aus dem Meer. Welches geophysikalische Prinzip beschreibt dieses Phaenomen, und wie erklaert die Viskositaet des Erdmantels die beobachteten Hebungsraten?",
        answerA = "Glazialisostatische Erholung (Glacial Isostatic Adjustment, GIA): Das Gewicht der Eisschilde drueckte die Kruste in den viskosen Mantel; nach dem Gletscherschwund steigt die Kruste mit einer durch die Mantelviskositaet (~10^21 Pa·s) bestimmten Zeitkonstante von Jahrtausenden zurueck — Fennoskandien hebt sich noch immer um bis zu 8 mm/Jahr",
        answerB = "Tektonische Expansion: Das Auseinanderdriften der eurasischen und nordamerikanischen Platte druckt Skandinavien von unten nach oben; die Hebungsrate ist proportional zur Spreizungsrate des Mittelatlantischen Rueckens",
        answerC = "Thermische Ausdehnung: Der Erdmantel unter Skandinavien ist waermer als im Durchschnitt, was durch thermische Expansion des Gesteins eine Hebung von oben bewirkt; dieser Effekt ist unabhaengig von der Eisbedeckung in der Vergangenheit",
        answerD = "Sedimentationsisostasie: Das Fehlen von Sedimentablagerungen nach dem Abschmelzen des Eises reduziert das Gesamtgewicht, was den Auftrieb des weniger dichten kontinentalen Kratons im dichten Mantelgestein verursacht",
        correctAnswer = 0, // A
        explanation = "Glazialisostatische Erholung (GIA) beruht auf dem Archimedischen Prinzip angewendet auf die Lithosphaere im Mantel. Das Inlandeis (bis 3 km dick) drueckte die kontinentale Kruste waehrend der letzten Eiszeit um bis zu 800 m nach unten. Nach dem Abschmelzen vor ca. 10.000 Jahren beginnt die Kruste sich zu erholen. Die Geschwindigkeit wird durch die Viskositaet des Oberen Erdmantels (~10^21 Pa·s) kontrolliert — ein hochviskoses 'Fliessen' des Mantelgesteins. Fennoskandien hebt sich mit 4–8 mm/Jahr, Kanada mit 1–3 mm/Jahr. Volle isostatische Erholung wird noch Tausende von Jahren dauern. GPS-Messungen zeigen diese Hebung mit Praezision von Millimetern pro Jahr.",
        difficulty = 5,
        funFact = "Wegen der GIA-Hebung Skandinaviens ist der Meeresspiegel relativ zur Kueste in Schweden und Finnland tatsaechlich sinkend — obwohl der absolute Meeresspiegel steigt. Staedte am Bottnischen Meerbusen, die vor Jahrhunderten Hafen-Staedte waren, liegen heute mehrere Kilometer vom Meer entfernt."
    ),

    // Question 2 — Wilson-Zyklus und Superkontinente  [correct: C → correctAnswer=2]
    Question(
        categoryId = 1,
        questionText = "Der Wilson-Zyklus beschreibt die periodische Entstehung und Schliessung von Ozeanen durch Plattentektonik. Welche Abfolge von Phasen und welche Zeitskala sind korrekt, und welcher naechste Superkontinent wird gemaess heutiger Modelle aus dem aktuellen Zustand entstehen?",
        answerA = "Wilson-Zyklus: Kontinentale Kollision → Riftbildung → Spreizung → Subduktion → erneute Kollision; Zeitskala ca. 5–10 Millionen Jahre; naechster Superkontinent: Pangaea Proxima durch Zusammenschluss Afrikas und Europas in ca. 50 Millionen Jahren",
        answerB = "Wilson-Zyklus: Eruption einer Flutbasalt-Provinz → Rifting → passive Randbildung → Ozeanbodenalterung → Subduktion → Kollision; Zeitskala ca. 50–100 Millionen Jahre; naechster Superkontinent: Amasia durch Vereinigung von Amerika und Asien ueber den Nordpol in ca. 50–100 Millionen Jahren",
        answerC = "Wilson-Zyklus: Rifting der Lithosphaere → junger Ozean → reifer Ozean mit Subduktionszonen → Ozeanschliessung → Gebirgsbildung (Orogenese) → Erosion und Plattformstadium; Zeitskala 300–500 Millionen Jahre; konkurrierendende Modelle prognostizieren entweder Amasia oder Aurica als naechsten Superkontinent",
        answerD = "Wilson-Zyklus: Hotspot-Aktivitaet → ozeanische Plateaubildung → kontinentale Anlagerung durch Terrane-Akkretion → Superkontinent-Bildung; Zeitskala 1–2 Milliarden Jahre; naechster Superkontinent: Novopangaea durch Zusammenschluss aller Kontinente am Suedpol",
        correctAnswer = 2, // C
        explanation = "Der Wilson-Zyklus (nach Tuzo Wilson, 1966) beschreibt einen vollstaendigen Zyklus in 6 Phasen: 1) Embryonalphase (kontinentales Rifting, z.B. Ostafrikanischer Grabenbruch), 2) Jugendphase (junges Ozeanbacken, z.B. Rotes Meer), 3) Reifephase (reifer Ozean mit passiven Raendern, z.B. Atlantik), 4) Abnahmephase (Subduktionsbeginn, z.B. Pazifik), 5) Terminalphase (Ozeanschliessung, z.B. Mittelmeer), 6) Restphase (Kontinent-Kontinent-Kollision, Gebirgsbildung). Zeitskala: 300–500 Ma. Fuer den naechsten Superkontinent gibt es zwei Hauptszenarien: Amasia (Amerika driftet nordwaerts zu Asien in ~100 Ma) und Aurica (Pazifik schliesst sich, neue Kontinentanordnung im heutigen Pazifikraum in ~200 Ma).",
        difficulty = 5,
        funFact = "Der letzte Superkontinent Pangaea brach vor 175 Millionen Jahren auseinander. Davor gab es Gondwana, Rodinia und noch aeltere Superkontinente. Der aelteste bekannte ist Columbia/Nuna (vor 1,8 Mrd. Jahren). Die Erde durchlaeuft diesen Superkontinent-Zyklus seit mindestens 2,5 Milliarden Jahren."
    ),

    // Question 3 — Subduktionszonen und Magmengenese  [correct: B → correctAnswer=1]
    Question(
        categoryId = 1,
        questionText = "An Subduktionszonen entstehen charakteristisch explosive Vulkane (Stratovulkane) mit siliziumreichen Magmen, waehrend Spreizungszonen fluidreiche Basaltlaven erzeugen. Welcher Mechanismus erklaert die Magmengenese an Subduktionszonen korrekt?",
        answerA = "Druckentlastungsschmelze: Die subduzierte Platte druckt den Mantelkeil von oben, verringert den Druck und erniedert den Schmelzpunkt des Mantelgesteins; das entstehende basaltische Magma steigt direkt auf und bereichert sich an der Kruste mit Silizium",
        answerB = "Flussmittelinduzierte Schmelze (flux melting): Hydrousse Minerale in der subduzierenden Platte geben bei Erwaermung Wasser ab; Wasser senkt den Schmelzpunkt von Mantelperidotit erheblich (hydrous solidus); der Mantelkeil schmilzt partiell und erzeugt Basalt, der beim Aufstieg in der kontinentalen Kruste differenziert und an SiO2 anreichert",
        answerC = "Reibungsschmelze: Die mechanische Reibung zwischen subduzierende und uberliegender Platte erzeugt genug Waerme, um die Grenzschicht zu schmelzen; dieses Reibungsmagma ist basaltisch und nimmt beim Aufstieg SiO2 aus der Kruste auf",
        answerD = "Radioaktiver Zerfall: Uran und Thorium konzentrieren sich in der subduzierenden ozeanischen Kruste; waehrend der Subduktion erhoehen diese radioaktiven Elemente die Temperatur lokal auf Schmelztemperatur des Mantels",
        correctAnswer = 1, // B
        explanation = "An Subduktionszonen ist der Hauptmechanismus die flussmittelinduzierte Schmelze (flux melting). Die subduzierte ozeanische Platte traegt wasserfuehrende Minerale (Amphibole, Serpentinite, Chlorite). Bei ca. 100–150 km Tiefe und 600–800°C werden diese Minerale instabil und geben Wasser ab (Dehydratation). Wasser senkt den Solidus von Mantelperidotit dramatisch (trockener Solidus ~1200°C, feuchter Solidus ~800°C). Der Mantelkeil ueber der subduzierenden Platte schmilzt partiell und bildet basaltisches Primaermagma. Dieses steigt auf, durchquert die kontinentale Kruste, assimiliert SiO2-reiches Krustenmaterial und differenziert zu andesitischen/rhyolithischen Magmen. Diese siliziumreichen Magmen sind hochviskoes und gasreich — daher die explosiven Ausbrueche (Vesuv, Pinatubo, Fuji).",
        difficulty = 5,
        funFact = "Die gesamte Pazifik-'Ring of Fire' entlang der Subduktionszonen ist fuer ca. 75% aller Vulkane weltweit und ca. 90% aller Erdbeben verantwortlich. Ohne Subduktionszonen gaebe es keinen Wasserkreislauf zwischen Ozeanboden und Mantel — ein essentieller Faktor fuer die langfristige Stabilisierung des Erdklimas."
    ),

    // Question 4 — Geomorphologie: Karstlandschaften  [correct: D → correctAnswer=3]
    Question(
        categoryId = 1,
        questionText = "Karstlandschaften entstehen durch chemische Verwitterung loeslicher Gesteine. Welcher biochemische Mechanismus treibt die Kalksteinaufloesung an, und welche weltweit bedeutendsten Karstregionen existieren?",
        answerA = "Hydrothermale Aufloesung: Heisses Wasser aus dem Erdinneren loest Kalkstein durch erhoehte Temperatur, bildet Thermalquellen-Kavernen; bedeutendste Regionen: Island, Yellowstone, Neuseeland",
        answerB = "Mechanische Erosion durch Flusssysteme: Schnell fliessendes Wasser erodiert Kalkstein physisch, wobei die Haerte des Gesteins irrelevant ist; bedeutendste Regionen: Amazon-Becken, Kongo-Becken",
        answerC = "Biologische Korrosion durch Cyanobakterien: Photosynthetisierende Bakterien setzen Saeuren frei, die Kalkstein auflosen; bedeutendste Regionen: Tropische Korallenriffe, Mangrovengebiete",
        answerD = "CO2-Karbonat-Gleichgewicht: Regenwasser absorbiert CO2 und bildet Kohlensaerue (H2CO3), die Kalziumkarbonat zu loeslichem Kalziumbikarbonat umwandelt (CaCO3 + H2CO3 → Ca(HCO3)2); bedeutende Karstregionen: Dinarische Alpen, Guilin/China, Yucatan/Mexiko, Postojna/Slowenien",
        correctAnswer = 3, // D
        explanation = "Die Kalksteinaufloesung laeuft ueber das CO2-Karbonat-Gleichgewicht ab: 1) CO2 loest sich in Wasser: CO2 + H2O → H2CO3 (Kohlensaeure). 2) Kohlensaeure reagiert mit CaCO3: CaCO3 + H2CO3 → Ca²⁺ + 2HCO3⁻ (loesliches Kalziumbikarbonat). Dieser Prozess laeuft langsam (Jahrtausende bis Millionen von Jahren) und erzeugt Dolines, Poljen, unterirdische Hoehlen, Tropfsteine und Karstquellen. Wenn CO2-armes Wasser (z.B. durch Erwaermung) aus dem Grundwasser entweicht, faellt CaCO3 wieder aus — so entstehen Stalaktiten und Stalagmiten. Grosste Karstlandschaften: Dinarischer Karst (Kroatien/Bosnien), Guilin-Gebirge (China, auf dem 20-Yuan-Schein), Yucatan-Halbinsel (Mexiko, mit Cenoten), Han Son Doong (Vietnam, groesste Hoehle der Welt).",
        difficulty = 5,
        funFact = "Die Han Son Doong in Vietnam, die groesste bekannte Hoehle der Welt, ist so gross, dass sie ein eigenes Klima mit Wolken und Nebel im Inneren hat. Sie ist 9 km lang, 200 m breit und 150 m hoch — gross genug, um einen 40-stoeckigen Wolkenkratzer aufzunehmen."
    ),

    // Question 5 — Meeresstroemungen und Thermohaline Zirkulation  [correct: A → correctAnswer=0]
    Question(
        categoryId = 1,
        questionText = "Die thermohaline Zirkulation (THC), auch 'Global Ocean Conveyor Belt' genannt, ist ein zentrales Element des globalen Klimasystems. Welcher physikalische Mechanismus treibt sie an, und welche Konsequenz haette eine Verlangsamung oder ein Zusammenbruch durch Suesswasserzufuhr fuer Europa?",
        answerA = "THC-Antrieb durch Dichtegradient (Temperatur + Salzgehalt): Polares Oberflaechenwasser kuehlt ab und verdunstet Salze, wird dichter und sinkt ab (Tiefenwasserbildung, z.B. Nordatlantik, Labradorsee); eine THC-Verlangsamung durch Suesswasserzufuhr aus schmelzenden Gletschern wuerde den Golfstrom abschwaechen und Nordwesteuropa um 5–10°C abkuehlen trotz globalem Temperaturanstieg",
        answerB = "THC-Antrieb durch Windschubspannung der Passate: Tropische Winde treiben Oberflaechenwasser polwaerts; eine Abkuehlung der Tropen durch mehr Verdunstung wuerde die Winde abschwaechenund die THC stoppen; Konsequenz fuer Europa: starkere Monsune und Ueberschwemmungen",
        answerC = "THC-Antrieb durch Gezeiten und Mondgravitation: Gezeitenkraefte pumpen Tiefenwasser an Schwellen-Strukturen des Meeresbodens auf; eine THC-Verlangsamung wuerde weltweit gleichmaessige Meerestemperaturerhoehungen bewirken",
        answerD = "THC-Antrieb durch Erdrotation (Coriolis-Effekt): Die Corioliskraft dirigiert Oberflaechenstroeme zu polaren Gebieten; eine THC-Verlangsamung durch Polschmelze wuerde die Erdrotation beschleunigen und Taglaengen um Sekunden verkuerzen",
        correctAnswer = 0, // A
        explanation = "Die thermohaline Zirkulation (THC) wird durch Dichteunterschiede im Meerwasser angetrieben: 1) Im tropischen Atlantik erwaermt sich Wasser und verdunstet, erhoehte Salinitat. 2) Dieses salzreiche Wasser fliesst nordwaerts (Golfstrom-System, AMOC). 3) Im Nordatlantik kuehlt es ab, wird durch beides (Kaelte + Salz) so dicht, dass es absinkt (Nordatlantische Tiefenwasserbildung, NADW). 4) Tiefenwasser fliesst suedwaerts bis Antarktis, dann in alle Ozeane. Bei schmelzenden Gletschern (Gronland) stroemt Suesswasser in den Nordatlantik, senkt Salzgehalt und verhindert das Absinken — AMOC verlangsamt sich. Europa, das den Golfstrom als natuerliche Heizung nutzt (~5°C waermer als vergleichbare Breitengrade), wuerde abkuehlen. Palaeoklima-Daten (Younger Dryas, 12.900–11.700 v. Chr.) zeigen, dass eine solche Abkuehlung innerhalb weniger Jahrzehnte eintreten kann.",
        difficulty = 5,
        funFact = "London liegt auf dem gleichen Breitengrad wie Calgary in Kanada — wo Winter mit -20°C gewoehlich sind. Dass London milde Winter hat, verdankt es fast ausschliesslich dem Golfstrom. Ohne ihn wuerden britische Winter kanadisch werden — bei nur einem Bruchteil der Sonnenenergie."
    ),

)
