package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun scienceQuestionsExpert4(): List<Question> = listOf(

    // ── Computational Chemistry 1 ──────────────────────────────────────────
    // Question 1 — Ab initio methods: Hartree-Fock
    Question(
        categoryId = 2,
        questionText = "Was ist die fundamentale Näherung in der Hartree-Fock-Methode (HF) bezüglich der Elektronenkorrelation?",
        answerA = "Jedes Elektron bewegt sich in einem gemittelten Feld aller anderen Elektronen; instantane Elektron-Elektron-Korrelation wird vernachlässigt",
        answerB = "Die kinetische Energie der Elektronen wird vollständig ignoriert",
        answerC = "Nur Valenz-Elektronen werden berücksichtigt, Rumpfelektronen werden eingefroren",
        answerD = "Die Born-Oppenheimer-Näherung wird aufgehoben und Kernbewegung explizit einbezogen",
        correctAnswer = 0, // A
        explanation = "HF behandelt jedes Elektron in einem mittleren Feld (mean field) der übrigen. Die dadurch fehlende instantane Elektron-Elektron-Wechselwirkung nennt man Korrelationsenergie; sie wird erst durch Post-HF-Methoden wie MP2, CCSD oder CI erfasst.",
        difficulty = 4,
        funFact = "Die Korrelationsenergie beträgt typischerweise nur ~1 % der Gesamtenergie, ist aber für Bindungsenergien und Reaktionsbarrieren entscheidend."
    ),

    // Question 2 — Basis sets
    Question(
        categoryId = 2,
        questionText = "Was beschreibt der Begriff 'Polarisationsfunktionen' in einem Basissatz (z. B. 6-31G*)?",
        answerA = "Funktionen höherer Drehimpulsquantenzahl, die die anisotrope Deformation der Elektronendichte bei Bindungsbildung beschreiben",
        answerB = "Zusätzliche s-Typ-Gaussfunktionen für eine bessere Beschreibung der Kernregion",
        answerC = "Diffuse Funktionen mit kleinen Exponenten für anionische Systeme",
        answerD = "Spinorbitalkopplungsterme für schwere Atome",
        correctAnswer = 0, // A
        explanation = "Polarisationsfunktionen (z. B. d-Funktionen auf Nicht-Wasserstoff-Atomen, p-Funktionen auf H) erlauben der Elektronendichte, sich asymmetrisch zu verformen, was für korrekte Bindungswinkel und Dipolmomente unerlässlich ist.",
        difficulty = 4,
        funFact = null
    ),

    // Question 3 — Solvation models
    Question(
        categoryId = 2,
        questionText = "Welches Prinzip liegt dem PCM-Solvationsmodell (Polarizable Continuum Model) zugrunde?",
        answerA = "Das Lösungsmittel wird als dielektrisches Kontinuum modelliert, und induzierte Oberflächenladungen auf einer Kavität beschreiben die Reaktion des Mediums auf den gelösten Stoff",
        answerB = "Einzelne Lösungsmittelmoleküle werden explizit quantenmechanisch behandelt",
        answerC = "Die Solvatationsenergie wird durch empirische Born-Gleichungen für Ionenradien berechnet",
        answerD = "Molekulardynamik-Simulationen werden zur Erzeugung des Solvat-Ensembles herangezogen",
        correctAnswer = 0, // A
        explanation = "Im PCM wird das Lösungsmittel durch seine Dielektrizitätskonstante ε repräsentiert. Auf einer molekularen Kavitätsoberfläche entstehen durch das elektrische Feld des Soluts induzierte Ladungen, die die elektrostatische Solvatationsenergie korrekt wiedergeben.",
        difficulty = 4,
        funFact = "Das PCM wurde 1981 von Tomasi und Mitarbeitern entwickelt und ist heute in praktisch jedem quantenchemischen Programmpaket implementiert."
    ),

    // Question 4 — Molecular dynamics force fields
    Question(
        categoryId = 2,
        questionText = "Welcher Term im AMBER-Kraftfeld beschreibt die Torsionswinkelenergie?",
        answerA = "Eine Fourier-Reihe in Abhängigkeit des Dihedralwinkels φ: V(φ) = ΣVₙ/2·[1 + cos(nφ − γ)]",
        answerB = "Ein harmonisches Potential V = ½k(φ − φ₀)²",
        answerC = "Ein Lennard-Jones-12-6-Potential zwischen 1,4-Atomen",
        answerD = "Eine Morse-Kurve mit Dissoziationsenergie Dₑ",
        correctAnswer = 0, // A
        explanation = "Torsionswinkel werden in molekularen Kraftfeldern wie AMBER, CHARMM oder GROMOS als Fourier-Reihe modelliert, um die Periodizität der Rotation um Bindungen korrekt abzubilden. n ist die Multiplizität, γ die Phasenverschiebung.",
        difficulty = 4,
        funFact = null
    ),

    // Question 5 — Quantum Monte Carlo
    Question(
        categoryId = 2,
        questionText = "Was ist der wesentliche Vorteil der Diffusion-Quantum-Monte-Carlo-Methode (DMC) gegenüber DFT?",
        answerA = "DMC behandelt Elektronenkorrelation explizit und stochastisch; die Energie konvergiert mit steigendem Rechenaufwand systematisch gegen den exakten Wert (Festzeichen-Näherung aside)",
        answerB = "DMC ist schneller als DFT für große Moleküle wegen linearer Skalierung",
        answerC = "DMC benötigt keine Basis­sätze und arbeitet rein analytisch",
        answerD = "DMC vermeidet das Elektronenkorrelationsproblem durch Pseudopotentiale vollständig",
        correctAnswer = 0, // A
        explanation = "DMC löst die zeitunabhängige Schrödinger-Gleichung stochastisch durch Imaginärzeitpropagation. Die Methode skaliert günstig (~N³) und liefert sehr genaue Korrelationsenergien, leidet aber unter dem Vorzeichenproblem bei Fermionen.",
        difficulty = 4,
        funFact = "Quantum-Monte-Carlo-Rechnungen gehören zu den rechenintensivsten Methoden in der Chemie und nutzen oft Tausende von CPU-Kernen."
    ),

    // ── Cellular Signaling 2 ──────────────────────────────────────────────
    // Question 6 — MAPK cascade
    Question(
        categoryId = 2,
        questionText = "Welche Sequenz beschreibt korrekt die kanonische Ras-MAPK-Signalkaskade nach Rezeptortyrosinkinase-Aktivierung?",
        answerA = "RTK → Grb2/SOS → Ras-GTP → Raf → MEK → ERK → Genexpression",
        answerB = "RTK → PI3K → Akt → mTOR → ERK → Genexpression",
        answerC = "RTK → PLC-γ → DAG → PKC → JNK → Genexpression",
        answerD = "RTK → JAK → STAT → Raf → MEK → Genexpression",
        correctAnswer = 0, // A
        explanation = "Nach RTK-Autophosphorylierung rekrutiert Grb2 den GEF SOS, der Ras von GDP→GTP aktiviert. Ras-GTP aktiviert Raf (MAP3K), das MEK (MAP2K) phosphoryliert, das wiederum ERK (MAPK) aktiviert. ERK transloziert in den Kern und phosphoryliert Transkriptionsfaktoren.",
        difficulty = 4,
        funFact = "Mutiertes, konstitutiv aktives KRAS findet sich in etwa 25 % aller menschlichen Krebserkrankungen."
    ),

    // Question 7 — Wnt pathway
    Question(
        categoryId = 2,
        questionText = "Welche Rolle spielt β-Catenin im kanonischen Wnt-Signalweg?",
        answerA = "Im inaktiven Zustand wird β-Catenin durch den Destruktionskomplex (APC/Axin/GSK-3β/CK1) phosphoryliert und proteasomal abgebaut; Wnt-Signal hemmt diesen Komplex, β-Catenin akkumuliert und aktiviert TCF/LEF-Zielgene",
        answerB = "β-Catenin phosphoryliert direkt den Frizzled-Rezeptor und aktiviert Dishevelled",
        answerC = "β-Catenin bindet LRP5/6 und verhindert die Dimerisierung von Wnt-Liganden",
        answerD = "β-Catenin fungiert als Ligand für den Frizzled-Rezeptor und ersetzt Wnt-Proteine",
        correctAnswer = 0, // A
        explanation = "Ohne Wnt-Signal phosphorylieren CK1 und GSK-3β β-Catenin, was seine Ubiquitinierung und proteasomalen Abbau auslöst. Wnt bindet Frizzled/LRP5-6, aktiviert Dishevelled, der den Destruktionskomplex hemmt, sodass β-Catenin akkumuliert, in den Kern transloziert und mit TCF/LEF Zielgene aktiviert.",
        difficulty = 4,
        funFact = "Mutationen in APC, das β-Catenin abbaut, sind die häufigste Ursache für familiären Dickdarmkrebs (FAP)."
    ),

    // Question 8 — Notch signaling
    Question(
        categoryId = 2,
        questionText = "Was ist der ungewöhnliche Aspekt der Notch-Signalübertragung bezüglich des Transkriptionsaktivators?",
        answerA = "Der Notch-Rezeptor ist selbst der Transkriptionsaktivator: nach zweifacher Proteolyse (durch ADAM-Metalloprotease und γ-Sekretase) wird die intrazelluläre Domäne (NICD) direkt in den Kern freigesetzt",
        answerB = "Notch aktiviert einen G-Protein-gekoppelten Rezeptor, der cAMP produziert",
        answerC = "Der Notch-Ligand Delta/Jagged wird nach Bindung in den Kern der sendenden Zelle transportiert",
        answerD = "Notch signalisiert ausschließlich über den JAK-STAT-Weg ohne nukleäre Translokation",
        correctAnswer = 0, // A
        explanation = "Nach Ligandenbindung (Delta-like/Jagged) schneidet zuerst ADAM10/17 extrazellulär, dann γ-Sekretase intramembranös. Die freigesetzte NICD-Domäne transloziert in den Kern, verdrängt Ko-Repressoren von CSL/RBPJκ und aktiviert Hes/Hey-Zielgene.",
        difficulty = 4,
        funFact = null
    ),

    // Question 9 — Hedgehog pathway
    Question(
        categoryId = 2,
        questionText = "Welche Funktion hat das Transmembranprotein Smoothened (SMO) im Hedgehog-Signalweg?",
        answerA = "SMO ist ein GPCR-ähnliches Protein, das im Ruhezustand durch Patched (PTCH) gehemmt wird; Hedgehog-Ligand inaktiviert PTCH, SMO wird aktiv und stabilisiert den Gli-Transkriptionsfaktor in seiner Aktivatorform",
        answerB = "SMO phosphoryliert direkt den Hedgehog-Liganden und verhindert seine Diffusion",
        answerC = "SMO ist eine Proteinkinase, die Gli-Transkriptionsfaktoren für den proteasomalen Abbau markiert",
        answerD = "SMO fungiert als Ko-Rezeptor für Wnt-Liganden und interagiert mit Frizzled",
        correctAnswer = 0, // A
        explanation = "PTCH inhibiert SMO durch Cholesterin-Umverteilung. Wenn Hedgehog (Shh, Ihh, Dhh) PTCH bindet, wird dieser Inhibitionsmechanismus aufgehoben. Aktives SMO akkumuliert im primären Cilium und hemmt die proteolytische Prozessierung von Gli3 zu seinem Repressor, sodass Voll-Längen-Gli-Aktivatoren Zielgene (Ptch1, Gli1, Cyclin D) aktivieren.",
        difficulty = 4,
        funFact = "Der SMO-Inhibitor Vismodegib ist das erste zugelassene Medikament, das den Hedgehog-Signalweg blockiert, und wird bei Basalzellkarzinom eingesetzt."
    ),

    // Question 10 — NF-κB activation
    Question(
        categoryId = 2,
        questionText = "Wie wird NF-κB durch den kanonischen Weg aktiviert?",
        answerA = "IκB wird durch den IKK-Komplex (IKKβ/γ/α) phosphoryliert, ubiquitiniert und proteasomal abgebaut; freigesetztes NF-κB (p65/p50) transloziert in den Kern und aktiviert Zielgene",
        answerB = "NF-κB-Dimere werden durch Caspasen gespalten und dadurch aus dem Zytoplasma freigesetzt",
        answerC = "Der TNF-Rezeptor phosphoryliert NF-κB direkt auf Ser536 ohne IκB-Beteiligung",
        answerD = "NF-κB wird durch nukleären Export von IκB aktiviert, der NF-κB im Kern zurücklässt",
        correctAnswer = 0, // A
        explanation = "Proinflammatorische Stimuli (TNF, IL-1, LPS) aktivieren den IKK-Komplex. IKKβ phosphoryliert IκBα auf Ser32/36, was Lysinkette-48-Ubiquitinierung und proteasomalen Abbau auslöst. Das freigesetzte p65:p50-Heterodimer transloziert, bindet κB-Enhancer und induziert Zytokin-/Antiapoptosegene.",
        difficulty = 4,
        funFact = null
    ),

    // ── Geomorphology 3 ──────────────────────────────────────────────────
    // Question 11 — River meander formation
    Question(
        categoryId = 2,
        questionText = "Welcher hydrodynamische Mechanismus treibt die Mäanderbildung in Flüssen an?",
        answerA = "Helikoide Sekundärströmung (Spiralströmung) erzeugt Erosion am Prallhang und Sedimentation am Gleithang, was die Kurvenamplitude progressiv vergrößert",
        answerB = "Gleichmäßig hohe Fließgeschwindigkeiten erodieren beide Ufer parallel, was zur Kurvenbildung führt",
        answerC = "Tektonische Kippprozesse lenken das Flussbett periodisch um 90° ab",
        answerD = "Gezeitenströmungen erzeugen wechselseitige Erosion, die Mäanderformen erzeugt",
        correctAnswer = 0, // A
        explanation = "In Flusskurven zwingt die Zentrifugalkraft das Oberflächenwasser nach außen, während bodennah eine Rückströmung nach innen entsteht (Helikoidströmung). Das erzeugt Unterspülung am konkaven Außengeifer (Prallhang) und Sandablagerung am konvexen Innengeifer (Gleithang/Point bar), was Kurven verstärkt.",
        difficulty = 4,
        funFact = "Mäander können sich durch Mäanderdurchbrüche selbst abschnüren und dabei Altarme (Oxbow Lakes) hinterlassen."
    ),

    // Question 12 — Karst topography
    Question(
        categoryId = 2,
        questionText = "Was ist der primäre chemische Prozess, der zur Ausbildung von Karsttopographie führt?",
        answerA = "Lösungsverwitterung: CO₂-gesättigtes Wasser bildet Kohlensäure, die Calcit in CaCO₃ + H₂O + CO₂ → Ca²⁺ + 2HCO₃⁻ auflöst",
        answerB = "Hydrothermale Verwitterung durch saure Schwefeldämpfe aus der Tiefe",
        answerC = "Mechanische Sprengwirkung von gefrierendem Porenwasser in Kalksteinspalten",
        answerD = "Oxidation von Sulfidmineralen zu Schwefelsäure, die Carbonatgestein angreift",
        correctAnswer = 0, // A
        explanation = "CO₂ löst sich in Niederschlagswasser zu H₂CO₃ (Kohlensäure), die Calcit (CaCO₃) inkongruent löst. Entlang von Klüften und Schichtflächen entstehen zunächst Karren, dann Höhlen, Dolinen und Poljen. Rückgängige Reaktion (Ausgasung von CO₂) fällt Calcit in Tropfsteinhöhlen aus.",
        difficulty = 4,
        funFact = "Die weltweit größte Karsthöhle, Son Doong in Vietnam, ist so groß, dass in ihr Dschungel mit eigenem Mikroklima wächst."
    ),

    // Question 13 — Glacial landforms
    Question(
        categoryId = 2,
        questionText = "Durch welchen Mechanismus entstehen glaziale Trogtäler (U-Täler) aus vorherigen V-förmigen Flusstälern?",
        answerA = "Gletschererosion durch Abrasion (Schleifwirkung subglazialem Schuttmaterials) und Detraktion (Herausbrechen von Fels durch Gefriersprengung und Scherspannungen) erodiert die Talflanken und den Talboden lateral und vertikal",
        answerB = "Schmelzwasserströme unter dem Gletscher erodieren ausschließlich den Talboden und schaffen das U-Profil",
        answerC = "Periglaziale Solifluktion transportiert Material die Hänge hinunter und füllt das V-Tal zu einer U-Form auf",
        answerD = "Isostatische Hebung nach dem Gletscherschwund hebt den Talboden an und formt das charakteristische U-Profil",
        correctAnswer = 0, // A
        explanation = "Gletscher erodieren breiter als Flüsse, weil sie die gesamte Talflanke belasten. Abrasion durch mitgeführten Schutt schleift Fels glatt (Gletscherschliffe), während Detraktion/Pflücken scharfkantige Felsstufen erzeugt. Das Ergebnis ist das charakteristische U-Profil mit hängenden Seitentälern.",
        difficulty = 4,
        funFact = null
    ),

    // Question 14 — Aeolian processes
    Question(
        categoryId = 2,
        questionText = "Welcher Transportmechanismus dominiert die äolische Sedimentbewegung in Sandwüsten?",
        answerA = "Saltation: Sandkörner werden von Windturbulenzen angehoben, beschreiben eine parabolische Flugbahn und treffen beim Aufprall andere Körner, die ihrerseits wieder angehoben werden (Splash-Effekt)",
        answerB = "Suspension: Sandkörner bleiben dauerhaft in der Luftsäule schwebend und werden über Tausende Kilometer transportiert",
        answerC = "Traktion: Windreibung schiebt Sandkörner ausschließlich entlang der Oberfläche ohne jeglichen Kontaktverlust",
        answerD = "Kreechion: Elektrisch geladene Körner folgen Feldlinien parallel zur Geländeoberfläche",
        correctAnswer = 0, // A
        explanation = "Saltation macht typischerweise 70–80 % des äolischen Sandtransports aus. Körner (0,1–2 mm) werden durch Turbulenzen in Höhen von 1–2 m gehoben und landen 10–30 cm weiter. Der Aufprall setzt Kriechbewegung (Reptation) von größeren Körnern frei. Feinstaub (< 0,02 mm) wird hingegen in Suspension transportiert.",
        difficulty = 4,
        funFact = "Durch Saltation wandern große Sanddünen in der Sahara bis zu 30 Meter pro Jahr."
    ),

    // Question 15 — Coastal erosion
    Question(
        categoryId = 2,
        questionText = "Welcher Prozess trägt am stärksten zur Klifferosion an felsigen Küsten bei?",
        answerA = "Hydraulische Wirkung (wave quarrying): eingeschlossene Luft in Felsspalten wird durch Wellendruck komprimiert und beim Rückzug schlagartig entspannt, was Fels heraussprengt; ergänzt durch abrasive Wirkung von wellengetragenem Schuttmaterial",
        answerB = "Chemische Verwitterung durch Meersalz löst Silikate und Carbonate gleichzeitig auf",
        answerC = "Biologische Verwitterung durch bore-Organismen dominiert gegenüber physikalischen Prozessen",
        answerD = "Gravitatives Abgleiten ganzer Kliffblöcke ohne Vorverwitterung durch Welleneinwirkung",
        correctAnswer = 0, // A
        explanation = "Hydraulische Wirkung ist der Haupttreiber bei Hartgesteinsküsten: Wellen komprimieren in Klüften eingeschlossene Luft auf bis zu mehrere Atmosphären Überdruck; beim Wellenrückzug dehnt sich Luft explosionsartig aus und sprengt Gestein heraus. Abrasion durch wellengetragenen Sand/Geröll schleift die Kliffbasis (Brandungshohlkehle).",
        difficulty = 4,
        funFact = null
    ),

    // ── Laser Physics 4 ──────────────────────────────────────────────────
    // Question 16 — Stimulated emission
    Question(
        categoryId = 2,
        questionText = "Was unterscheidet stimulierte Emission von spontaner Emission bezüglich der Kohärenzeigenschaften des emittierten Photons?",
        answerA = "Das durch stimulierte Emission erzeugte Photon ist phasen-, frequenz-, polarisations- und richtungsgleich mit dem Anregungsphoton; spontan emittierte Photonen haben zufällige Phase und Richtung",
        answerB = "Stimulierte Emission erzeugt Photonen mit doppelter Frequenz des Anregungsphotons (Frequenzverdopplung)",
        answerC = "Bei stimulierter Emission wird die Energie auf zwei Photonen aufgeteilt, jedes mit halber Frequenz",
        answerD = "Stimulierte Emission ist identisch mit spontaner Emission; nur der Zeitpunkt wird durch das Anregungsphoton gesteuert",
        correctAnswer = 0, // A
        explanation = "Einstein (1917) zeigte, dass stimulierte Emission ein Klonphoton erzeugt, das in allen Quantenzahlen (Phase, Frequenz, Polarisation, Wellenvektor) mit dem Triggerphoton übereinstimmt. Diese Kohärenzeigenschaft ist die Grundlage der Laser-Verstärkung (Light Amplification by Stimulated Emission of Radiation).",
        difficulty = 4,
        funFact = "Die Theorie der stimulierten Emission von Albert Einstein aus dem Jahr 1917 war der Grundstein für die Laserphysik, aber erst 1960 baute Theodore Maiman den ersten funktionsfähigen Laser."
    ),

    // Question 17 — Population inversion
    Question(
        categoryId = 2,
        questionText = "Warum ist eine Besetzungsinversion in einem Zwei-Niveau-System thermodynamisch unmöglich?",
        answerA = "Im Gleichgewicht erzeugt optisches Pumpen gleich schnell Absorption wie stimulierte Emission; beide Prozesse halten sich exakt die Waage, sodass N₂/N₁ = 1 das Maximum ist",
        answerB = "Ein Zwei-Niveau-System hat keine angeregten Zustände, da alle Elektronen im Grundzustand bleiben",
        answerC = "Die spontane Lebensdauer im Zwei-Niveau-System ist unendlich, weshalb keine Inversion möglich ist",
        answerD = "Zwei-Niveau-Systeme zeigen immer stimulierte Emission stärker als Absorption",
        correctAnswer = 0, // A
        explanation = "Bei gleicher Entartung sind Einsteinkoeffizienten B₁₂ = B₂₁. Starkes Pumpen erhöht Absorption und stimulierte Emission gleichzeitig. Das Maximum ist transparentes Medium (N₁ = N₂), nie Inversion (N₂ > N₁). Reale Laser brauchen mindestens Drei-Niveau-Systeme, um Besetzungsinversion zu erreichen.",
        difficulty = 4,
        funFact = null
    ),

    // Question 18 — Mode-locking
    Question(
        categoryId = 2,
        questionText = "Was ist das physikalische Prinzip hinter der Modenkopplung (mode-locking) in einem Ultrakurzpuls-Laser?",
        answerA = "Alle longitudinalen Moden des Resonators werden phasensynchronisiert; ihre konstruktive Interferenz erzeugt einen extrem kurzen, intensiven Puls, der mit der Resonatorumlauffrequenz im Resonator kreist",
        answerB = "Nur eine einzige longitudinale Mode wird selektiert; die Pulsdauer entspricht der Resonatorlänge",
        answerC = "Transversale Moden werden räumlich überlagert, um Pulsdauern im Femtosekundenbereich zu erreichen",
        answerD = "Ein externer elektrooptischer Modulator wählt zufällig einzelne Photonen aus und unterdrückt alle anderen",
        correctAnswer = 0, // A
        explanation = "Ein Laserresonator unterstützt N longitudinale Moden. Im freien Lauf haben diese zufällige Phasen. Modenkopplung (aktiv oder passiv via sättigbarer Absorber/Kerr-Linsen-Effekt) erzwingt Phasenkohärenz: Die N Moden addieren sich konstruktiv zu einem Puls der Dauer ≈ 1/(N·Δν), erreichbar im Femtosekundenbereich.",
        difficulty = 4,
        funFact = "Titan-Saphir-Laser erzeugen mit Modenkopplung Pulse unter 5 Femtosekunden — in dieser Zeit legt Licht nur 1,5 Mikrometer zurück."
    ),

    // Question 19 — Fiber lasers
    Question(
        categoryId = 2,
        questionText = "Welcher Vorteil von Ytterbium-dotierten Faserlasern gegenüber Festkörperlasern erklärt ihre industrielle Dominanz?",
        answerA = "Das große Oberfläche-zu-Volumen-Verhältnis der Faser erlaubt exzellente Wärmeabfuhr; hohe Konversionswirkungsgrade (>80 %), diffraktionsbegrenzte Strahlqualität (M²≈1) und Kompaktheit werden gleichzeitig erreicht",
        answerB = "Faserlaser arbeiten ausschließlich im Dauerstrichbetrieb und können daher höhere mittlere Leistungen als gepulste Systeme erzeugen",
        answerC = "Ytterbium hat eine breitere Emissionsbandbreite als jedes andere Lasermedium und erlaubt deshalb tiefere Modenkopplung",
        answerD = "Die Faser fungiert als optischer Wellenleiter und verhindert parasitäre Oszillation vollständig ohne Bedarf an Resonatorspiegeln",
        correctAnswer = 0, // A
        explanation = "Faserlaser nutzen die Faser selbst als Wellenleitungsstruktur. Das extrem große Oberfläche-zu-Volumen-Verhältnis dissipiert Abwärme effizient. Yb³⁺ hat ein kleines Quantendefizit (~Stokes-Shift von 1030/976 nm), was thermische Last minimiert. Ergebnis: Multi-kW-Systeme mit exzellenter Strahlqualität.",
        difficulty = 4,
        funFact = null
    ),

    // Question 20 — Ultrafast lasers
    Question(
        categoryId = 2,
        questionText = "Was beschreibt der Begriff 'chirped pulse amplification' (CPA) in der Ultrakurzpuls-Lasertechnik?",
        answerA = "Ein kurzer Puls wird zeitlich gedehnt (gechirpt), dann im verstärkenden Medium amplifiziert und anschließend rerekomprimiert — so werden Spitzenleistungen von Petawatt erreicht, ohne das Verstärkermedium zu zerstören",
        answerB = "Die Wellenlänge eines Laserpulses wird während der Verstärkung kontinuierlich blauverschoben (chirped), um höhere Photonenenergien zu erreichen",
        answerC = "Mehrere Laserpulse unterschiedlicher Wellenlänge werden zeitlich überlagert und interferieren konstruktiv zu einem intensiveren Einzelpuls",
        answerD = "Ein Gitter streckt den Puls räumlich aus, sodass verschiedene Frequenzkomponenten gleichzeitig unterschiedliche Verstärkerbereiche durchlaufen",
        correctAnswer = 0, // A
        explanation = "CPA (Strickland & Mourou, Nobelpreis 2018): Strecker dehnt fs-Puls auf ns-Bereich (Spitzenleistung sinkt um 10⁶). Verstärker amplifiziert ohne nichtlineare Schäden. Kompressor rekonstruiert den Kurzpuls. Resultierende Spitzenleistungen: Petawatt (10¹⁵ W).",
        difficulty = 4,
        funFact = "Donna Strickland erhielt 2018 zusammen mit Gérard Mourou für die Erfindung der CPA-Technik den Nobelpreis für Physik — sie war erst die dritte Frau überhaupt, die diesen Preis erhielt."
    ),

    // ── Membrane Biology 5 ──────────────────────────────────────────────
    // Question 21 — Lipid bilayer dynamics
    Question(
        categoryId = 2,
        questionText = "Was unterscheidet laterale Diffusion von Flip-Flop (transversale Diffusion) in der Lipiddoppelschicht bezüglich der Kinetik?",
        answerA = "Laterale Diffusion ist schnell (D ≈ 10⁻⁸ cm²/s, t₁/₂ Sekunden), Flip-Flop ist extrem langsam (t₁/₂ Stunden bis Tage), da der hydrophile Kopf die hydrophobe Kernzone passieren muss",
        answerB = "Flip-Flop ist schneller als laterale Diffusion, weil nur eine kurze Strecke zurückgelegt wird",
        answerC = "Beide Prozesse sind bei 37 °C gleich schnell und werden identisch durch Membranfluidität kontrolliert",
        answerD = "Laterale Diffusion erfordert ATP-abhängige Flippase-Enzyme, Flip-Flop ist spontan und schnell",
        correctAnswer = 0, // A
        explanation = "Laterale Diffusion benötigt nur Überwindung von Van-der-Waals-Wechselwirkungen mit Nachbarlipiden (niedrige Aktivierungsenergie). Beim Flip-Flop muss der hydrophile Kopf durch den hydrophoben Kern — die Aktivierungsenergie ist enorm. In Erythrozyten katalysieren Flippase (ATP8A1) und Scramblase aktiv den Phospholipid-Transfer für Membranasymmetrie.",
        difficulty = 4,
        funFact = null
    ),

    // Question 22 — Ion channel selectivity
    Question(
        categoryId = 2,
        questionText = "Welcher Strukturbereich des Kaliumkanals KcsA ist für die hohe K⁺/Na⁺-Selektivität verantwortlich?",
        answerA = "Der Selektivitätsfilter (TVGYG-Sequenz): Carbonylsauerstoffe koordinieren K⁺ (r=1,33 Å) exakt wie Wassermoleküle; Na⁺ (r=0,95 Å) ist zu klein für optimale Koordination und wird daher nicht desolviert",
        answerB = "Negativ geladene Glutamatreste am Kanaleingang stoßen Na⁺ spezifisch ab",
        answerC = "Der Selektivitätsfilter hat eine feste Porengröße von exakt 1,33 Å, durch die nur K⁺ passt",
        answerD = "Hydrophobe Reste im Kanal-Lumen stabilisieren ausschließlich K⁺ durch Van-der-Waals-Kräfte",
        correctAnswer = 0, // A
        explanation = "Roderick MacKinnon (Nobelpreis 2003) zeigte: Die Carbonylgruppen der TVGYG-Aminosäuresequenz bilden vier Bindungsstellen, die K⁺ perfekt koordinieren und seine Hydratationshülle ersetzen. Für Na⁺ ist die Koordinationsgeometrie unpassend — der Energiegewinn durch Kanalkoordination übertrifft die Dehydratationskosten für Na⁺ nicht.",
        difficulty = 4,
        funFact = "K⁺-Kanäle sind >10.000-fach selektiv für K⁺ gegenüber Na⁺, obwohl K⁺ größer ist als Na⁺."
    ),

    // Question 23 — Receptor internalization
    Question(
        categoryId = 2,
        questionText = "Welche molekulare Sequenz vermittelt die Clathrin-abhängige Endozytose von GPCRs nach Ligandenstimulation?",
        answerA = "β-Arrestin bindet den phosphorylierten Rezeptor (durch GRK), rekrutiert Clathrin und AP-2, bildet eine Clathrin-coated pit, die sich abschnürt (Dynamin-GTPase) und einen Early Endosome bildet",
        answerB = "Der Ligand-Rezeptor-Komplex diffundiert passiv in präformierte Clathrin-Grübchen ohne aktive Rekrutierung von Adaptorproteinen",
        answerC = "Caveolin-1 polymerisiert um den aktivierten Rezeptor und bildet Caveolae, die durch Aktin-Polymerisation internalisiert werden",
        answerD = "Rab7 markiert den aktivierten Rezeptor direkt für lysosomale Degradation ohne Endosomenstufe",
        correctAnswer = 0, // A
        explanation = "Nach GPCR-Aktivierung phosphorylieren GRKs (G-protein-coupled receptor kinases) den Rezeptor. β-Arrestine binden, entkoppeln G-Proteine (Desensitisierung) und fungieren als Gerüst für Clathrin und Adaptorprotein AP-2. Dynamin-GTPase schnürt die Vesikel ab. Im Early Endosome entscheidet der pH-Wert über Recycling vs. lysosomalen Abbau.",
        difficulty = 4,
        funFact = null
    ),

    // Question 24 — Exocytosis mechanisms
    Question(
        categoryId = 2,
        questionText = "Welche Rolle spielen SNARE-Proteine beim Mechanismus der regulierten Exozytose?",
        answerA = "v-SNAREs (Vesikel, z. B. Synaptobrevin) bilden mit t-SNAREs (Zielmembran: Syntaxin + SNAP-25) einen stabilen, helicalen trans-SNARE-Komplex (Zippering), der die Membranen so nähert, dass Fusion spontan erfolgt",
        answerB = "SNAREs fungieren als Ca²⁺-Sensoren, die direkt Ca²⁺ binden und daraufhin die Vesikelmembran auflösen",
        answerC = "SNARE-Proteine katalysieren die Lipidmischung als Phospholipasen, ohne direkte mechanische Kraft zu erzeugen",
        answerD = "v-SNAREs verankern das Vesikel an Aktin-Filamenten; t-SNAREs steuern den Zeitpunkt durch Phosphorylierung",
        correctAnswer = 0, // A
        explanation = "Die SNARE-Hypothese (Rothman, Schekman, Südhof — Nobelpreis 2013): Zippering des ternären SNARE-Komplexes (Synaptobrevin/VAMP + Syntaxin + SNAP-25) setzt Energie frei, die die Membranfusion treibt. Synaptotagmin fungiert als Ca²⁺-Sensor und löst die Fusion bei neuronalem Aktionspotential aus. NSF/α-SNAP disassemblieren den Komplex für Recycling.",
        difficulty = 4,
        funFact = "Botulinum- und Tetanustoxin spalten spezifische SNARE-Proteine und blockieren so die Neurotransmitter-Freisetzung."
    ),

    // Question 25 — Membrane potential generation
    Question(
        categoryId = 2,
        questionText = "Wie erzeugt die Na⁺/K⁺-ATPase das Ruhemembranpotential in Neuronen?",
        answerA = "Die Pumpe transportiert 3 Na⁺ hinaus und 2 K⁺ hinein pro ATP (elektrogen, -1 Ladung netto); dies erhält die Konzentrationsgradienten, die über Leckleitfähigkeiten (vor allem K⁺-Kanäle) das negative Ruhepotential (~−70 mV) aufrechterhalten",
        answerB = "Die Pumpe erzeugt das Potential direkt durch ihren elektrogenen Ladungstransfer von −30 mV",
        answerC = "Die Pumpe kontrolliert die intrazelluläre Ca²⁺-Konzentration und erzeugt dadurch das Membranpotential",
        answerD = "Das Ruhepotential entsteht ausschließlich durch Cl⁻-Kanäle; die Na⁺/K⁺-ATPase spielt keine elektrische Rolle",
        correctAnswer = 0, // A
        explanation = "Die Na⁺/K⁺-ATPase hält [K⁺]innen hoch und [Na⁺]innen niedrig. Über K⁺-Leckkanäle diffundiert K⁺ entlang des Konzentrationsgradienten nach außen, hinterlässt negative Ladungen — das Nernst-Potential für K⁺ (≈−90 mV) dominiert das Ruhepotential (~−70 mV). Der direkte elektroge Beitrag der Pumpe ist mit ~−3 mV klein.",
        difficulty = 4,
        funFact = null
    ),

    // ── Stellar Nucleosynthesis 6 ──────────────────────────────────────────
    // Question 26 — pp chain
    Question(
        categoryId = 2,
        questionText = "Welcher Schritt der Proton-Proton-Kette (pp-I-Kette) in der Sonne ist der geschwindigkeitsbestimmende und warum?",
        answerA = "Die Fusion zweier Protonen zu Deuterium (p+p→d+e⁺+νₑ) ist der langsamste Schritt, da er den schwachen Zerfall eines Quarks erfordert; die Reaktionsrate ist extrem niedrig (~10⁻²⁰ s⁻¹ pro Proton)",
        answerB = "Die Fusion von Deuterium mit Proton (d+p→³He+γ) ist der langsamste Schritt wegen coulombscher Abstoßung",
        answerC = "Die ³He-³He-Reaktion (³He+³He→⁴He+2p) ist geschwindigkeitsbestimmend, weil drei Teilchen gleichzeitig zusammentreffen müssen",
        answerD = "Das Photon-Entweichen aus der Sonne ist der langsamste Schritt (>10⁵ Jahre Wanderungszeit)",
        correctAnswer = 0, // A
        explanation = "p+p→²H+e⁺+νₑ erfordert gleichzeitig Tunneln durch die Coulomb-Barriere UND Umwandlung p→n über den schwachen W⁺-Boson-Austausch. Die extrem geringe Wahrscheinlichkeit des schwachen Zerfalls macht diese Reaktion milliardenmal langsamer als nukleare Kernreaktionen und bestimmt die Lebenszeit der Sonne (~10¹⁰ a).",
        difficulty = 4,
        funFact = "Ein Proton in der Sonnenmitte muss im Durchschnitt etwa 9 Milliarden Jahre warten, bevor es an der p+p-Fusionsreaktion teilnimmt."
    ),

    // Question 27 — CNO cycle
    Question(
        categoryId = 2,
        questionText = "Ab welcher Sternmasse dominiert der CNO-Zyklus gegenüber der pp-Kette und warum?",
        answerA = "Ab ~1,3 Sonnenmassen (T_Kern > ~1,7×10⁷ K), weil der CNO-Zyklus eine steilere Temperaturabhängigkeit hat (ε ∝ T¹⁶⁻²⁰ vs. T⁴ für pp), sodass er bei hohen Temperaturen überwiegt",
        answerB = "Ab 0,5 Sonnenmassen, weil massereichere Sterne mehr CNO-Elemente synthetisieren können",
        answerC = "Der CNO-Zyklus dominiert nur in Roten Riesen mit degenerierten He-Kernen und Wasserstoffschalenverbrennung",
        answerD = "Der CNO-Zyklus ist immer gleich schnell wie die pp-Kette; die Dominanz hängt nur von der CNO-Häufigkeit ab",
        correctAnswer = 0, // A
        explanation = "Der CNO-Zyklus (p+¹²C→¹³N→¹³C→¹⁴N→¹⁵O→¹⁵N+⁴He+Cyclus) nutzt C, N, O als Katalysatoren. Seine Reaktionsrate hängt von den CN-Kernquerschnitten und der Coulomb-Barriere von Proton-auf-¹²C ab. Die steilere Temperaturabhängigkeit (~T¹⁶⁻²⁰) macht ihn in massereicheren, heißeren Kernen dominierend.",
        difficulty = 4,
        funFact = null
    ),

    // Question 28 — Triple-alpha process
    Question(
        categoryId = 2,
        questionText = "Was ist die Besonderheit des 'Hoyle-Zustands' beim Triple-Alpha-Prozess?",
        answerA = "Fred Hoyle sagte 1953 einen angeregten Zustand von ¹²C bei ~7,65 MeV vorher, der die Resonanzbedingung für ³He→¹²C erfüllt; ohne diese Resonanz wäre die ¹²C-Häufigkeit im Universum zu gering für das Leben",
        answerB = "Der Hoyle-Zustand ist der Grundzustand von ⁸Be, der die Lebensdauer verlängert und ³α-Reaktion ermöglicht",
        answerC = "Hoyle entdeckte, dass zwei α-Teilchen zuerst zu ⁸Be fusionieren, das stabil ist und dann mit α fusioniert",
        answerD = "Der Hoyle-Zustand beschreibt das Verhalten von Helium bei Degeneration in Weißen Zwergen",
        correctAnswer = 0, // A
        explanation = "⁸Be ist extrem kurzlebig (t₁/₂ ≈ 8,2×10⁻¹⁷ s). Trotzdem kann ⁸Be+α→¹²C* funktionieren, wenn ¹²C einen angeregten Zustand nahe der Energiescheide besitzt. Hoyle sagte diesen 0⁺-Resonanzzustand bei 7,6542 MeV theoretisch vorher — er wurde experimentell bestätigt. Ohne ihn wäre Kohlenstoff im Universum extrem selten.",
        difficulty = 4,
        funFact = "Hoyles anthropisches Argument — 'Die Physik muss diese Resonanz haben, sonst gäbe es kein Leben' — war eines der ersten anthropischen Argumente in der Wissenschaft."
    ),

    // Question 29 — s-process
    Question(
        categoryId = 2,
        questionText = "In welchem stellaren Umfeld läuft der s-Prozess (slow neutron capture) hauptsächlich ab und was bestimmt seine charakteristischen Häufigkeitspeaks?",
        answerA = "Im TP-AGB-Phasenstadium massearmer Sterne (Thermisch pulsierender AGB); magische Neutronenzahlen (N=50, 82, 126) führen zu kleinen Neutroneneinfangquerschnitten und damit zu Häufigkeitspeaks bei Sr/Y, Ba/La, Pb",
        answerB = "Im konvektiven Kern von Hauptreihensterne mit >8 M☉; die Häufigkeitspeaks entstehen durch Coulomb-Abstoßung",
        answerC = "In Neutronensternen direkt nach dem Kollaps; magische Protonenbalken erzeugen die Häufigkeitspeaks",
        answerD = "Im Zentrum von Roten Riesen mit heliumbrennendem Kern; Peaks entstehen durch α-Teilchenabsorption",
        correctAnswer = 0, // A
        explanation = "Im s-Prozess werden Neutronen langsam eingefangen (zwischen Einfängen ausreichend Zeit für β-Zerfall). Hauptquelle sind TP-AGB-Sterne: ¹³C(α,n)¹⁶O liefert Neutronen. An Kernen mit magischen N sind die Neutroneneinfangquerschnitte klein → Häufung. Dies erklärt Sr, Ba, Pb-Peaks in der solaren Häufigkeitsverteilung.",
        difficulty = 4,
        funFact = null
    ),

    // Question 30 — r-process
    Question(
        categoryId = 2,
        questionText = "Welches astrophysikalische Ereignis gilt heute als Hauptquelle des r-Prozesses (rapid neutron capture)?",
        answerA = "Neutronensternverschmelzungen (Kilonova): extrem hohe Neutronendichten (~10²⁴ cm⁻³) erlauben schnelle Mehrfach-Neutroneneinfänge weit jenseits der β-Stabilitätslinie, gefolgt von β-Zerfällen nach Abtrennung der Neutronenquelle",
        answerB = "Kernkollaps-Supernovae Typ II: die Schockwelle erzeugt ein neutronenreiches Windgebiet, das alle r-Prozess-Elemente produziert",
        answerC = "Gamma-Ray-Bursts langer Dauer in massereichen Sternen sind die einzige r-Prozessquelle",
        answerD = "Akkretierende Weiße Zwerge (Typ Ia) erzeugen Neutronendichten hoch genug für den r-Prozess",
        correctAnswer = 0, // A
        explanation = "Die Gravitationswellendetektion GW170817 (LIGO 2017) zusammen mit der elektromagnetischen Kilonova AT2017gfo lieferte den ersten direkten Beweis für r-Prozess-Nukleosynthese in NS-Verschmelzungen. Spektrallinien von Lanthaniden und Actiniden (Sr, Ba, Au, Pt) wurden identifiziert. Ob auch Supernovae beitragen, ist noch diskutiert.",
        difficulty = 4,
        funFact = "Das bei der Neutronensternverschmelzung GW170817 synthetisierte Gold entspricht schätzungsweise mehreren Erdmassen."
    ),

    // ── Statistical Thermodynamics 7 ──────────────────────────────────────
    // Question 31 — Partition function applications
    Question(
        categoryId = 2,
        questionText = "Wie lässt sich die mittlere innere Energie U eines kanonischen Ensembles aus der Zustandssumme Z berechnen?",
        answerA = "U = kT² (∂ ln Z / ∂T)_V — die logarithmische Ableitung der Zustandssumme nach der Temperatur bei konstantem Volumen, multipliziert mit kT²",
        answerB = "U = k ln Z — die mittlere Energie ist direkt proportional zum Logarithmus der Zustandssumme",
        answerC = "U = Z·kT — die Energie ist das Produkt aus Zustandssumme und thermischer Energie",
        answerD = "U = −(∂Z/∂β)_V / Z mit β=1/kT, was identisch mit U = −∂ ln Z/∂β ist",
        correctAnswer = 3, // D
        explanation = "Im kanonischen Ensemble gilt Z = Σ exp(−βεᵢ). Die mittlere Energie ist U = −∂ ln Z/∂β = kT²(∂ ln Z/∂T). Antwort A und D sind mathematisch äquivalent (da dβ = −dT/(kT²)); Antwort D beschreibt die eleganteste Form. Die Zustandssumme kodiert alle thermodynamischen Größen.",
        difficulty = 4,
        funFact = null
    ),

    // Question 32 — Fermi-Dirac statistics
    Question(
        categoryId = 2,
        questionText = "Was besagt die Fermi-Dirac-Verteilung bei T=0 K bezüglich der Besetzung von Energieniveaus?",
        answerA = "Alle Zustände unterhalb der Fermi-Energie EF sind vollständig besetzt (f=1), alle Zustände oberhalb EF sind vollständig leer (f=0) — eine scharfe Stufenfunktion",
        answerB = "Alle Elektronen besetzen den niedrigsten Energiezustand gemäß dem Bose-Einstein-Kondensat-Prinzip",
        answerC = "Die Besetzung folgt der Maxwell-Boltzmann-Verteilung, die bei T=0 eine Exponentialfunktion ergibt",
        answerD = "Jedes Energieniveau hat bei T=0 K genau ein Elektron durch das Pauli-Prinzip, unabhängig von der Energie",
        correctAnswer = 0, // A
        explanation = "f(E) = 1/(exp((E−μ)/kT)+1). Bei T→0 gilt: f(E<EF)=1, f(E>EF)=0. Diese scharfe Fermi-Kante ergibt sich direkt aus dem Pauli-Prinzip (max. 1 Fermion pro Zustand) und der Thermodynamik. Bei endlicher Temperatur weicht die Kante auf einer Breite ~kT auf.",
        difficulty = 4,
        funFact = "Die Fermi-Energie in Kupfer beträgt ~7 eV, was einer äquivalenten Temperatur von ~80.000 K entspricht — Elektronen in Metallen sind hochgradig entartet."
    ),

    // Question 33 — Bose-Einstein statistics
    Question(
        categoryId = 2,
        questionText = "Was ist die physikalische Bedingung für das Auftreten von Bose-Einstein-Kondensation in einem idealen Bosegas?",
        answerA = "Die thermische de-Broglie-Wellenlänge λdB wird vergleichbar mit dem mittleren Teilchenabstand (nλdB³ ≥ 2,612): Wellenfunktionen überlappen makroskopisch, eine makroskopische Zahl von Teilchen besetzt den k=0-Grundzustand",
        answerB = "Die Temperatur muss exakt 0 K betragen, damit alle Bosonen den Grundzustand besetzen können",
        answerC = "Das Bosegas muss auf Suprafluidität komprimiert werden; der Druck bestimmt den BEC-Übergang",
        answerD = "Bosonen müssen Spins 0 haben; Spin-1-Teilchen können kein BEC bilden",
        correctAnswer = 0, // A
        explanation = "Beim kritischen Phasenübergang (Tc = (2πħ²/mkB)·(n/2,612)^(2/3)) überlappen die de-Broglie-Wellenpakete. Ein makroskopischer Anteil der Teilchen besetzt denselben Quantenzustand (k=0), was zur kohärenten Materiewelle des BEC führt. Erstmals realisiert 1995 von Cornell, Wieman und Ketterle (Nobelpreis 2001) mit ⁸⁷Rb-Atomen.",
        difficulty = 4,
        funFact = null
    ),

    // Question 34 — Debye model
    Question(
        categoryId = 2,
        questionText = "Welche Verbesserung gegenüber dem Einstein-Modell bietet das Debye-Modell für die Wärmekapazität von Festkörpern?",
        answerA = "Debye nimmt eine kontinuierliche Verteilung von Phonon-Frequenzen bis zu einer maximalen Debye-Frequenz ωD an (statt nur einer Frequenz); dies liefert korrekte T³-Abhängigkeit bei tiefen Temperaturen (Debye-T³-Gesetz)",
        answerB = "Debye berücksichtigt Anharmonizität der Schwingungen, während Einstein harmonisch rechnet",
        answerC = "Das Debye-Modell verwendet Fermionen statt Bosonen für Gitterschwingungen und liefert dadurch eine T²-Abhängigkeit",
        answerD = "Debye addiert elektronische und phononische Beiträge; Einstein ignoriert Elektronen vollständig",
        correctAnswer = 0, // A
        explanation = "Einstein (1907) modelliert alle 3N Moden mit gleicher Frequenz → bei T<ΘE fällt Cv exponentiell ab (zu schnell). Debye (1912) nimmt g(ω)∝ω² bis ωD an (akustische Näherung). Für T≪ΘD: Cv = (12π⁴/5)NkB(T/ΘD)³, das experimentell beobachtete T³-Gesetz für Isolatoren.",
        difficulty = 4,
        funFact = "Die Debye-Temperatur ΘD ist ein Maß für die Steifigkeit eines Kristalls: Diamant hat ΘD≈2230 K, weiches Blei nur ΘD≈88 K."
    ),

    // Question 35 — Einstein model
    Question(
        categoryId = 2,
        questionText = "Was sagt das Einstein-Modell der spezifischen Wärme korrekt vorher, was das klassische Dulong-Petit-Gesetz nicht erklären kann?",
        answerA = "Das Abfallen der Wärmekapazität bei tiefen Temperaturen unterhalb der Einstein-Temperatur ΘE = ħωE/kB, weil die thermische Energie kleiner wird als das Quant ħωE der Gitterschwingung",
        answerB = "Die Einstein-Temperatur erlaubt Vorhersage der Schmelztemperatur aller Festkörper",
        answerC = "Das Modell erklärt die Wärmeleitfähigkeit korrekt durch quantisierte Phononen",
        answerD = "Das Dulong-Petit-Gesetz gilt nur für Metalle; Einstein korrigierte es für Isolatoren auf 2R statt 3R",
        correctAnswer = 0, // A
        explanation = "Klassisch gilt Cv=3R für alle Temperaturen (Dulong-Petit). Einstein (1907) quantisiert die 3N Oszillatoren: Cv = 3NkB·(ΘE/T)²·eΘE/T/(eΘE/T−1)². Für T≫ΘE: Cv→3NkB (Dulong-Petit). Für T≪ΘE: Cv→0 exponentiell. Dies erklärte erstmals das experimentelle Abfallen, obwohl das T³-Verhalten nur Debye korrekt wiedergibt.",
        difficulty = 4,
        funFact = null
    ),

    // ── Genetic Engineering 8 ──────────────────────────────────────────────
    // Question 36 — TALEN technology
    Question(
        categoryId = 2,
        questionText = "Welches Prinzip ermöglicht die sequenzspezifische DNA-Bindung von TALENs (Transcription Activator-Like Effector Nucleases)?",
        answerA = "Tandem-Arrays aus 34-Aminosäure-Wiederholungsmodulen, von denen jedes durch zwei variable Aminosäuren (RVD) eine bestimmte DNA-Base erkennt (HD=C, NI=A, NG=T, NN=G/A); an den Array ist FokI-Nuklease fusioniert",
        answerB = "TALE-Proteine erkennen DNA wie CRISPR durch komplementäre RNA-Hybridisierung mit einer Führungssequenz",
        answerC = "Zinkfinger-Domänen erkennen jeweils drei Basen; TALE steht für eine neue Generation mit Vier-Basen-Erkennung",
        answerD = "TALENs binden doppelsträngige DNA nicht sequenzspezifisch und werden durch einen Promotorsequenz-Antikörper geleitet",
        correctAnswer = 0, // A
        explanation = "TALEs aus Xanthomonas-Bakterien nutzen modulare 34-AS-Repeats. Jede Einheit erkennt über Repeat Variable Diresidue (RVD) an Positionen 12 und 13 eine DNA-Base. Arrays werden programmiert, indem man die RVD-Sequenz der Zielsequenz entsprechend zusammensetzt. Zwei TALENs flankieren einen Schnittbereich, an dem das FokI-Heterodimer die DNA schneidet.",
        difficulty = 4,
        funFact = "TALENs wurden 2011 als Werkzeug zur Genomeditierung etabliert und wurden wenig später durch CRISPR-Cas9 teilweise verdrängt, werden aber weiterhin für Anwendungen genutzt, die Off-Target-Spezifität erfordern."
    ),

    // Question 37 — Base editing
    Question(
        categoryId = 2,
        questionText = "Welchen enzymatischen Mechanismus nutzt ein Cytosin-Base-Editor (CBE) für die C→T-Konversion?",
        answerA = "nCas9 (Nickase) öffnet die doppelsträngige DNA; fusionierte APOBEC-Desaminase konvertiert einzelsträngiges C im Bearbeitungsfenster (Position 4-8 des Spacers) zu U, das beim Repair als T abgelesen wird",
        answerB = "Der CBE schneidet beide DNA-Stränge, integriert eine neue Cytosin-Base und religiert",
        answerC = "Eine fusionierte Methyltransferase methyliert C zu 5-mC, das dann spontan zu T deaminiert",
        answerD = "APOBEC-Desaminase wirkt auf doppelsträngige DNA und konvertiert C→T in beiden Strängen gleichzeitig",
        correctAnswer = 0, // A
        explanation = "CBEs (David Liu, 2016) kombinieren nCas9(D10A) mit APOBEC1/AID. nCas9 erzeugt einen Nick im nicht-editierten Strang und hält Einzelstrang-DNA im Bearbeitungsfenster (4-8 nt vom PAM). APOBEC desaminiert C→U; Basen-Exzisionsreparatur oder Replikation liest U als T. UGI (Uracil-DNA-Glycosylase-Inhibitor) verhindert U-Exzision und erhöht Effizienz.",
        difficulty = 4,
        funFact = null
    ),

    // Question 38 — Prime editing
    Question(
        categoryId = 2,
        questionText = "Was unterscheidet Prime Editing von klassischem HDR (Homologie-gesteuerter Reparatur) als Methode für präzise Genomeditierung?",
        answerA = "Prime Editing benötigt keine Donorvorgabe und kein DSB: pegRNA kodet Zielsequenz + editierte Sequenz; nCas9-RT-Fusion erzeugt Nick im Nicht-Zielstrang und schreibt die neue Sequenz per Reverse Transkriptase direkt ins Genom",
        answerB = "Prime Editing erzeugt wie HDR einen Doppelstrangbruch, ist aber unabhängig von der Zellzyklusphase",
        answerC = "Prime Editing nutzt zwei separate Cas9-Proteine und zwei verschiedene Guide-RNAs für gleichzeitige Flankenschnitte",
        answerD = "HDR und Prime Editing sind identisch; der einzige Unterschied ist, dass Prime Editing eine RNA-Matrize statt einer DNA-Matrize verwendet",
        correctAnswer = 0, // A
        explanation = "Prime Editing (Liu-Lab, 2019) macht alle 12 Punkt-Mutationstypen sowie kleine Insertionen/Deletionen ohne DSB oder Donortemplate möglich. Der pegRNA-Teil bindet Ziel (spacer) und enthält 3'-RT-Matrize + primer binding site. nCas9-MMLV-RT-Fusion schreibt die neue Sequenz in den Nick-3'-Überhang; Flap-Einbau und MMR liefern permanente Editierung.",
        difficulty = 4,
        funFact = "Prime Editing kann theoretisch 89 % aller pathogenen Punktmutationen im menschlichen Genom korrigieren."
    ),

    // Question 39 — Gene drives
    Question(
        categoryId = 2,
        questionText = "Wie propagiert ein CRISPR-basierter Gene Drive ein Merkmal durch eine wild-lebende Population?",
        answerA = "Das Drive-Allel kodiert Cas9 + Guide-RNA gegen den Wild-Typ-Locus; in Heterozygoten schneidet Cas9 das Wildtypallel und HDR kopiert das Drive-Allel auf den homologen Chromosom → Allelhäufigkeit steigt supermendelisch",
        answerB = "Gene Drives nutzen Transposons, die zufällig im Genom inserieren und sich durch höhere Replikationsrate verbreiten",
        answerC = "Das Drive-Allel codiert einen Wachstumsvorteil für Träger; natürliche Selektion verbreitet es in Generationen",
        answerD = "CRISPR-Drive-Konstrukte aktivieren endogene Retroviren, die das Merkmal horizontal übertragen",
        correctAnswer = 0, // A
        explanation = "In Nachkommen eines Drive×Wildtyp-Kreuzungsexperiments liegt das Drive-Allel heterozygot vor. Cas9 (kodiert im Drive) schneidet das Wildtyp-Allel an der Guide-RNA-Sequenz. HDR repariert anhand des Drive-Allels als Matrize → homozygot. Die Häufigkeit des Drive-Allels steigt mit jeder Generation schnell gegen 100 %, unabhängig vom Fitness-Effekt.",
        difficulty = 4,
        funFact = null
    ),

    // Question 40 — Optogenetics
    Question(
        categoryId = 2,
        questionText = "Welches Protein ermöglichte die bahnbrechende Umsetzung der Optogenetik zur Depolarisation von Neuronen mit Licht?",
        answerA = "Channelrhodopsin-2 (ChR2) aus der Grünalge Chlamydomonas reinhardtii: ein lichtgesteuerter Kationenkanal, der nach Expression in Neuronen durch Blaulicht (~470 nm) Na⁺/Ca²⁺-Einstrom und Aktionspotentiale auslöst",
        answerB = "Bacteriorhodopsin aus Halobacterium salinarium: eine lichtgetriebene Protonenpumpe, die Neuronen durch Ansäuern depolarisiert",
        answerC = "Halorhodopsin: ein Chloridpumpen-Protein, das Neuronen nach Expression mit gelbem Licht depolarisiert",
        answerD = "Phytochrom B aus Arabidopsis: ein Rotlicht-Rezeptor, der nach Lichtaktivierung Ionenkanäle über Second-Messenger öffnet",
        correctAnswer = 0, // A
        explanation = "Boyden, Zhang, Deisseroth et al. (2005) exprimierten ChR2 in kultivierten Neuronen: Blaulicht-Pulse (473 nm) öffnen den Kanal, Na⁺ strömt ein, Membran depolarisiert, Aktionspotential ausgelöst — mit Millisekunden-Präzision. Halorhodopsin (eNpHR) und Archaerhodopsin (Arch) ermöglichen komplementäre Hemmung. Deisseroth erhielt 2021 den Lasker-Award.",
        difficulty = 4,
        funFact = "Optogenetik wurde 2010 von Nature Methods zur Methode des Jahrzehnts ernannt."
    ),

    // ── Tectonophysics 9 ──────────────────────────────────────────────────
    // Question 41 — Slab pull mechanisms
    Question(
        categoryId = 2,
        questionText = "Welches ist nach heutigem Verständnis die dominante treibende Kraft der Plattentektonik?",
        answerA = "Slab Pull: abtauchende ozeanische Lithosphäre ist dichter (Eklogit-Facies) als der umgebende Mantel und zieht die gesamte Platte nach unten; dieser Mechanismus liefert ~90 % der treibenden Kraft",
        answerB = "Ridge Push: horizontaler Druckgradient vom ozeanischen Rücken schiebt Platten auseinander und dominiert die Plattendynamik",
        answerC = "Mantelkonvektion: thermische Konvektionsrolleströmungen tragen Platten passiv wie ein Förderband",
        answerD = "Gezeitenkräfte von Mond und Sonne erzeugen den dominierenden Drehimpuls für Plattenbewegung",
        correctAnswer = 0, // A
        explanation = "Subduktionsplatten durchlaufen gabbro→eklogit-Umwandlung bei ~60 km Tiefe (Dichte ~3,5 g/cm³ vs. Mantel ~3,3 g/cm³). Diese negative Schwimmfähigkeit erzeugt Slab Pull. Korrelationsstudien zeigen, dass Platten mit Subduktionszonen schneller sind (8–10 cm/a) als Platten ohne (2–4 cm/a). Ridge Push ist real aber sekundär (~10 %).",
        difficulty = 4,
        funFact = null
    ),

    // Question 42 — Ridge push
    Question(
        categoryId = 2,
        questionText = "Welcher physikalische Effekt erzeugt die Ridge-Push-Kraft an mittelozeanischen Rücken?",
        answerA = "Frisch aufgestiegenes, heißes Magma erzeugt eine thermisch bedingte Topographierhöhe; die Druckdifferenz zwischen hoher, warmer Rückenregion und tiefer, kühler Ozeanbodenregion erzeugt einen horizontalen Druckgradienten, der Platten vom Rücken wegdrückt",
        answerB = "Explosive Eruptionen an Rücken erzeugen Rückstoßimpulse, die Platten lateralmente beschleunigen",
        answerC = "Differenzielle Kühlungsrate erzeugt Bimetall-artiges Verbiegen der Platte, das sie vom Rücken wegwölbt",
        answerD = "Hydrothermale Systeme an Rücken erzeugen Überdruck durch Verdampfung, der Platten abstoßt",
        correctAnswer = 0, // A
        explanation = "Ozeanische Lithosphäre kühlt mit der Entfernung vom Rücken, kontrahiert, verdickt und versinkt (Bathymetrie: Tiefe ∝ √Alter). Der isostatisch erhöhte Rücken steht unter höherem lithosphärischem Druck als die angrenzenden tieferen Bereiche. Der Druckgradient (~2–3 × 10¹² N/m nach Lichtenbelt) drückt Platten seitlich auseinander (Ridge Push ≈ 3 × 10¹² N/m).",
        difficulty = 4,
        funFact = "Obwohl Ridge Push kleiner als Slab Pull ist, kann er allein bei Platten ohne Subduktionszonen (wie der Afrikanischen Platte) die Bewegung antreiben."
    ),

    // Question 43 — Mantle wedge
    Question(
        categoryId = 2,
        questionText = "Welcher Prozess führt zu Vulkanismus im Mantelvulkankeil (mantle wedge) über Subduktionszonen?",
        answerA = "Entwässerung der subduzierenden Platte (Dehydration von Serpentinit, Amphibol, Chlorit) setzt H₂O frei, das in den Mantelkeil aufsteigt, den Solidus des peridotitischen Mantels herabsetzt und Schmelze erzeugt (fluxing melting)",
        answerB = "Adiabatische Dekompression des aufsteigenden Asthenosphärenmaterials schmilzt den Mantelkeil ohne Flüssigkeitszufuhr",
        answerC = "Reibungswärme zwischen subduzierender Platte und Mantelkeil schmilzt den Mantelkeil direkt an der Plattenoberfläche",
        answerD = "Radioaktiver Zerfall von U, Th, K in der subduzierenden Kruste erhitzt den Mantelkeil über den Solidus",
        correctAnswer = 0, // A
        explanation = "Ozeanische Kruste trägt Serpentinite, Amphibole und andere hydratisierte Minerale. Bei ~80–150 km Tiefe und erhöhtem Druck wird H₂O freigesetzt. Wasser senkt den Peridotit-Solidus von ~1300°C auf ~1000°C. Partialschmelzen entstehen, steigen auf und erzeugen calcalkalinen Vulkanismus (Andesit, Dacit) im Inselbogens oder Kontinentalrand.",
        difficulty = 4,
        funFact = null
    ),

    // Question 44 — Forearc basin
    Question(
        categoryId = 2,
        questionText = "Welcher strukturelle Mechanismus kontrolliert die Entstehung von Forearc-Becken (Vorlandbecken vor dem Akkretionskeil)?",
        answerA = "Zwischen dem akkretionären Prisma (Schüttungspuffer der subduzierenden Sedimente) und dem vulkanischen Inselbogen entsteht durch Subsidenz des Forearc-Bodens ein Sedimentationsbecken; seine Geometrie wird durch Slab-Dip und Erosion vs. Akkretion kontrolliert",
        answerB = "Extension im Rücken des Inselbogens erzeugt Graben, die mit Sedimenten aus dem Inselbogen gefüllt werden",
        answerC = "Forearc-Becken entstehen durch Kompression zwischen Inselbogen und Kontinentalkruste bei obliquen Subduktionswinkeln",
        answerD = "Isostasie: das Gewicht des akkretionären Prismas drückt die ozeanische Kruste nach unten und erzeugt das Becken",
        correctAnswer = 0, // A
        explanation = "Forearc-Becken liegen zwischen dem Akkretionsprisma (seewärts) und dem magmatischen Bogen (landwärts). Subsidenz entsteht durch: (1) Zug der abtauchenden Platte auf die Forearc-Lithosphäre, (2) Erosion an der Plattenoberfläche, (3) sedimentäre Belastung. Sie sind wichtige Lagerstätten für Kohlenwasserstoffe (z. B. Mentawai-Becken, Seram-Becken).",
        difficulty = 4,
        funFact = "Das Forearc-Becken von Sumatra ist eine der produktivsten Erdölregionen Indonesiens."
    ),

    // Question 45 — Back-arc spreading
    Question(
        categoryId = 2,
        questionText = "Welcher Mechanismus erklärt das Back-Arc-Spreading hinter Inselbögen wie im Mariana-Trog?",
        answerA = "Rollback des Subduktionsslab (trenchward migration): der Subduktionsgraben wandert seewärts von der überfahrenden Platte weg, der Forearc wird gedehnt und ein Spreading-Zentrum entsteht im Back-Arc-Bereich",
        answerB = "Mantelplumes unter dem Inselbogen erzeugen extensiven Rifting, identisch mit hotspot-induziertem Rifting",
        answerC = "Hohe Subduktionsrate erzeugt einen Überdruck im Mantelkeil, der das Inselbogen-Segment nach hinten drückt",
        answerD = "Back-Arc-Spreading entsteht durch Rotationskräfte der Erdrotation, die Inselbögen in Subduktionsrichtung voran treiben",
        correctAnswer = 0, // A
        explanation = "Slab Rollback: die negative Schwimmfähigkeit der subduzierenden Platte bewirkt, dass der Graben sich relativ zur überfahrenden Platte seewärts bewegt. Die überfahrende Lithosphäre kann nicht nachfolgen; Extension und Rifting folgen im Back-Arc. Ähnlicher Mechanismus wie mid-ocean spreading, jedoch mit stärkerem Wasser-Fluss und charakteristisch tholeiitischen bis bonitischen Schmelzen.",
        difficulty = 4,
        funFact = null
    ),

    // ── Radiochemistry 10 ──────────────────────────────────────────────────
    // Question 46 — Technetium-99m production
    Question(
        categoryId = 2,
        questionText = "Wie wird ⁹⁹mTc (Technetium-99m) für medizinische Nuklearmedizin erzeugt?",
        answerA = "Im Mo/Tc-Generator: ⁹⁸Mo wird in einem Kernreaktor durch Neutronenbestrahlung (n,γ) zu ⁹⁹Mo aktiviert; dieses zerfällt mit t₁/₂=66h zu ⁹⁹mTc (t₁/₂=6h), das täglich per Elution aus dem Al₂O₃-Säulengenerator gewonnen wird",
        answerB = "⁹⁹mTc wird direkt in einem Zyklotron durch Protonenbestrahlung von ⁹⁹Ru erzeugt",
        answerC = "Kernspaltung von ²³⁵U in Reaktoren erzeugt ⁹⁹mTc als direktes Spaltprodukt ohne Generatorsystem",
        answerD = "⁹⁹Mo wird durch α-Bestrahlung von ⁹⁶Mo im Van-de-Graaff-Generator produziert und zerfällt zu ⁹⁹mTc",
        correctAnswer = 0, // A
        explanation = "Der Mo/Tc-Cow-Generator (Molybdän-Kühe) enthält ⁹⁹Mo, adsorbiert auf Al₂O₃ als Molybdat MoO₄²⁻. ⁹⁹Mo (t₁/₂=66h) zerfällt durch β⁻ zu ⁹⁹mTc (Isomerübergang, t₁/₂=6h, γ=140 keV). Tägliche Kochsalzlösung-Elution wäscht selektiv Pertechnetat TcO₄⁻ aus. 140 keV ist ideal für SPECT-Detektoren.",
        difficulty = 4,
        funFact = "Über 80 % aller nuklearmedizinischen Untersuchungen weltweit verwenden ⁹⁹mTc-markierte Radiopharmaka."
    ),

    // Question 47 — Radiolabeling methods
    Question(
        categoryId = 2,
        questionText = "Was ist das Prinzip der Radioiodierung (¹²⁵I) von Proteinen nach der Chloramin-T-Methode?",
        answerA = "Chloramin-T oxidiert I⁻ zu reaktivem I⁺ (Iodoniumkation), das elektrophil aromatische Substitution an Tyrosinresten des Proteins eingeht und monoiodierte Tyrosine bildet",
        answerB = "Chloramin-T aktiviert Lysinreste durch NHS-Ester, die dann mit Iodid-Ionen reagieren",
        answerC = "¹²⁵I bindet durch Komplexierung an Histidinreste; Chloramin-T denaturiert das Protein für besseren Zugang",
        answerD = "Chloramin-T katalysiert den Einbau von Iod in Disulfidbrücken durch Reduktion und Wiederoxidation",
        correctAnswer = 0, // A
        explanation = "Hunter & Greenwood (1962): Chloramin-T (N-Chloro-p-toluensulfonamid) wirkt als mildes Oxidationsmittel, das Na¹²⁵I zu elektrophilem ¹²⁵I⁺ oxidiert. Elektrophile aromatische Substitution erfolgt bevorzugt an Tyrosin (aktivierter Ring), seltener Histidin. Metabisulfit stoppt die Reaktion. Alternative: Iodogen (festphasig, weniger Proteinschäden), IODO-GEN, Bolton-Hunter-Reagenz (für Proteine ohne Tyr).",
        difficulty = 4,
        funFact = null
    ),

    // Question 48 — Autoradiography
    Question(
        categoryId = 2,
        questionText = "Welcher physikalische Detektor hat in der modernen Autoradiographie konventionellen Röntgenfilm weitgehend abgelöst und warum?",
        answerA = "Phosphor-Imaging-Platten (storage phosphor screens): BaFBr:Eu²⁺-Kristalle speichern Strahlungsenergie als metastabile Farbzentren; Laserauslesung erzeugt lineares Signal über 5 Dekaden, 10–100× empfindlicher als Film, wiederverwendbar und digital",
        answerB = "CCD-Detektoren haben Film ersetzt, da sie direkt ionisierende Teilchen durch Ladungssammlung detektieren",
        answerC = "Szintillationsplatten mit TlI(Cs)-Kristallen liefern schnellere Auslesung als Film bei gleichem Dynamikbereich",
        answerD = "Geiger-Müller-Zähler wurden miniaturisiert und als 2D-Array für Autoradiographie-Platten genutzt",
        correctAnswer = 0, // A
        explanation = "Phosphor-Imaging (Fuji BAS, Typhoon, Amersham): Strahlung (β, γ, α) hebt Eu²⁺→Eu³⁺ und speichert Elektronen in F-Zentren. Roter Helium-Neon-Laser stimuliert Photolumineszenz (blau). PMT detektiert Signal proportional zur Strahlungsdosis. Linearer Dynamikbereich: 5 Dekaden (vs. 1,5 für Film). Expositionszeit: Minuten statt Tage. Digital und quantitativ.",
        difficulty = 4,
        funFact = "Phosphor-Imaging-Platten können nach UV-Beleuchtung vollständig gelöscht und Tausende Male wiederverwendet werden."
    ),

    // Question 49 — Scintillation counting
    Question(
        categoryId = 2,
        questionText = "Warum wird ¹⁴C im Flüssigszintillationszähler nicht in einem Geiger-Müller-Zähler gezählt?",
        answerA = "¹⁴C emittiert sehr weiche β-Partikel (E_max=156 keV), die Fenstermaterial von GM-Zählern nicht durchdringen; im LSC ist die Probe direkt im Szintillationscocktail gelöst, das 4π-Geometrie und maximale Effizienz (~95 %) gewährleistet",
        answerB = "¹⁴C emittiert γ-Strahlung, die zu intensiv für GM-Zähler ist und Photomultiplier-Röhren benötigt",
        answerC = "GM-Zähler können keine β-Strahlung nachweisen; dafür sind ausschließlich Proportionalzähler geeignet",
        answerD = "¹⁴C-Halbwertszeit (5730 a) ist zu lang für zeitaufgelöste GM-Messungen mit akzeptabler Statistik",
        correctAnswer = 0, // A
        explanation = "¹⁴C: β⁻-Strahler, E_max=156 keV (sehr weich). Selbst 1 mg/cm² Eintrittsfenster absorbiert diese β-Teilchen vollständig. Im LSC ist die Probe im organischen Szintillationscocktail (Toluol/PPO/POPOP oder DIN-basiert): Lösungsmittelmoleküle absorbieren Energie → Szintillatormoleküle → Fluoreszenz → PMT. Quench-Korrektur mit Trichromat- oder externes Standardmethode.",
        difficulty = 4,
        funFact = null
    ),

    // Question 50 — Accelerator mass spectrometry
    Question(
        categoryId = 2,
        questionText = "Welcher Aspekt macht AMS (Accelerator Mass Spectrometry) der konventionellen Zerfallszählung für ¹⁴C-Datierung überlegen?",
        answerA = "AMS zählt ¹⁴C-Atome direkt, nicht ihre seltenen Zerfallsereignisse (t₁/₂=5730 a): Nachweisgrenzen von 0,001 pMC mit ~1 mg Probe statt >1 g, Messzeit Minuten statt Tage, unempfindlich gegen Hintergrundstrahlung",
        answerB = "AMS verwendet radioaktive Beschleuniger statt passiver Detektoren und erzielt dadurch höhere Messraten",
        answerC = "Konventionelles Zerfallszählen misst Massenzahl direkt durch Massenspektrometrie; AMS misst nur Häufigkeiten",
        answerD = "AMS benötigt keine Probenaufbereitung, da Beschleuniger alle chemischen Verbindungen im Ionenstrahl trennen",
        correctAnswer = 0, // A
        explanation = "In 1 mg Kohlenstoff gibt es ~5×10¹⁰ ¹⁴C-Atome (moderne Probe), aber nur ~0,3 Zerfälle pro Sekunde. GM/LSC müssen Zerfälle detektieren. AMS ionisiert die Probe (C⁻-Ionen aus Graphit), beschleunigt auf MeV, schneidet Isobaren (¹⁴N verliert Elektronen anders) und zählt ¹⁴C-Ionen direkt im Detektor. Ergebnis: 1000× weniger Probe, 10× bessere Genauigkeit, Datierung bis ~50.000 a.",
        difficulty = 4,
        funFact = "Mit AMS können Radiokarbon-Datierungen an einzelnen Samen, Pollen oder einem einzigen Haar durchgeführt werden."
    )
)
