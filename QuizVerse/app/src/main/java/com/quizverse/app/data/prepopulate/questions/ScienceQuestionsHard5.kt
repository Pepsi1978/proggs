package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun scienceQuestionsHard5(): List<Question> = listOf(

    // ── 1. EPIGENETICS APPLIED ──────────────────────────────────────────────

    // Question 1 – DNA Methylation  [correct: C → correctAnswer=2]
    Question(
        categoryId = 2,
        questionText = "An welchem Basenmotiv findet die DNA-Methylierung durch DNMT3A/3B im Säugergenom hauptsächlich statt?",
        answerA = "ApG-Dinukleotide (Adenin-Guanin)",
        answerB = "GpC-Dinukleotide (Guanin-Cytosin)",
        answerC = "CpG-Dinukleotide (Cytosin-Guanin)",
        answerD = "TpA-Dinukleotide (Thymin-Adenin)",
        correctAnswer = 2, // C
        explanation = "Im Säugergenom methyliert DNMT3A/3B hauptsächlich den Cytosylring an CpG-Dinukleotiden (5-Methylcytosin). Diese Methylierung ist ein repressives epigenetisches Signal: methylierte CpG-reiche Promotorregionen (CpG-Inseln) sind transkriptionell meist inaktiv. DNMT1 erhält dieses Muster während der Replikation aufrecht.",
        difficulty = 3,
        funFact = "Obwohl CpG-Dinukleotide im Genom unterrepräsentiert sind (da 5-Methylcytosin spontan zu Thymin desaminiert), konzentrieren sich unmethylierte CpG-Inseln an aktiven Genpromotoren und wirken als epigenetische Schalter."
    ),

    // Question 2 – Histone Modifications  [correct: Trimethylierung → correctAnswer=3]
    Question(
        categoryId = 2,
        questionText = "Die Histonmodifikation H3K27me3 wird durch den Polycomb Repressive Complex 2 (PRC2) katalysiert. Welche biochemische Reaktion liegt dabei vor?",
        answerA = "Acetylierung von Lysin 27 am Histon H3",
        answerB = "Phosphorylierung von Serin 27 am Histon H3",
        answerC = "Ubiquitinierung von Lysin 27 am Histon H3",
        answerD = "Trimethylierung von Lysin 27 am Histon H3",
        correctAnswer = 3, // D
        explanation = "PRC2 katalysiert über seine katalytische Untereinheit EZH2 die Mono-, Di- und Trimethylierung von Lysin 27 am Histon H3 (H3K27me3). Diese Modifikation ist ein zentrales repressives Chromatin-Signal, das bei der Entwicklung ganze Gencluster (z.B. HOX-Gene) stillschaltet. H3K27me3 rekrutiert PRC1, das weiterhin H2A ubiquitiniert.",
        difficulty = 3,
        funFact = null
    ),

    // Question 3 – Chromatin Remodeling  [correct: ATP-Hydrolyse → correctAnswer=0]
    Question(
        categoryId = 2,
        questionText = "Welche molekulare Energie-Quelle nutzen ATP-abhängige Chromatin-Remodeling-Komplexe wie SWI/SNF?",
        answerA = "ATP-Hydrolyse durch eine Helicase-ähnliche ATPase-Domäne",
        answerB = "NAD⁺-Hydrolyse",
        answerC = "GTP-Hydrolyse durch eine Ras-ähnliche ATPase",
        answerD = "Protonengradient über der Kernmembran",
        correctAnswer = 0, // A
        explanation = "SWI/SNF und verwandte Remodeling-Komplexe besitzen eine zentrale ATPase-Untereinheit (z.B. BRG1/BRM beim BAF-Komplex) mit einer Helicase-ähnlichen ATPase-Domäne. Die Energie der ATP-Hydrolyse wird genutzt, um Histonoktamere entlang der DNA zu verschieben (sliding), zu eviktieren oder DNA-Schleifen einzuführen, was Nukleosomenpositionen verändert und Transkriptionsfaktor-Bindestellen freilegt.",
        difficulty = 3,
        funFact = "Mutationen in BRG1 (SMARCA4) finden sich in etwa 20% aller Lungenkarzinome — SWI/SNF-Remodeling-Defekte sind somit ein häufiger epigenetischer Krebs-Mechanismus."
    ),

    // Question 4 – Imprinting Disorders  [correct: maternale UPD → correctAnswer=1]
    Question(
        categoryId = 2,
        questionText = "Das Prader-Willi-Syndrom entsteht durch den Verlust paternaler Genexpression in der Region 15q11-q13. Durch welchen Mechanismus kann es ursächlich entstehen?",
        answerA = "Deletion der maternalen Region 15q11-q13 (maternal UPD)",
        answerB = "Uniparentale Disomie: Beide Chromosom-15-Kopien stammen von der Mutter",
        answerC = "Hypomethylierung des SNRPN-Promotors auf dem maternalen Allel",
        answerD = "Trisomie 15 mit Verlust des paternalen Chromosoms",
        correctAnswer = 1, // B
        explanation = "Prader-Willi-Syndrom (PWS) entsteht, wenn die normalerweise paternalen, maternell imprinted Gene in 15q11-q13 nicht exprimiert werden. Dies kann durch: (1) paternale Deletion (~70%), (2) maternale uniparentale Disomie/UPD (~25%) — beide Chromosomen 15 von der Mutter, kein aktives paternales Allel — oder (3) Imprinting-Fehler (~5%) ausgelöst werden. Bei maternaler UPD liefert die Mutter beide Kopien.",
        difficulty = 3,
        funFact = null
    ),

    // Question 5 – Environmental Epigenetics  [correct: veränderte Methylierung → correctAnswer=2]
    Question(
        categoryId = 2,
        questionText = "Welches Phänomen beschreibt die transgenerationale epigenetische Vererbung am Beispiel der niederländischen Hungersnot (Hongerwinter 1944/45)?",
        answerA = "Kinder der Überlebenden zeigten ausschließlich genetische Mutationen in Stoffwechselgenen",
        answerB = "Epigenetische Veränderungen wurden ausschließlich auf Töchter übertragen, nicht auf Söhne",
        answerC = "Nachkommen der betroffenen Männer zeigten veränderte DNA-Methylierungsmuster und erhöhtes Metabolisches-Syndrom-Risiko",
        answerD = "Die Effekte waren auf die unmittelbaren Kinder beschränkt und nicht in der zweiten Generation nachweisbar",
        correctAnswer = 2, // C
        explanation = "Studien zum Hongerwinter zeigten, dass Kinder von Vätern, die in utero die Hungersnot erlebt hatten, erhöhte Raten von Adipositas, Typ-2-Diabetes und kardiovaskulären Erkrankungen aufwiesen. Diese Effekte waren mit veränderten DNA-Methylierungsmustern assoziiert (z.B. am IGF2-Gen). Dies ist ein Schlüsselbeispiel für transgenerationale Epigenetik durch Umweltstress.",
        difficulty = 3,
        funFact = "Der Hungerwinter betraf etwa 4,5 Millionen Menschen in den besetzten Niederlanden. Die epigenetischen Folgen sind noch drei Generationen später nachweisbar."
    ),

    // ── 2. ELECTROCHEMISTRY ────────────────────────────────────────────────

    // Question 6 – Nernst Equation  [correct: steigt ~59 mV → correctAnswer=3]
    Question(
        categoryId = 2,
        questionText = "Die Nernst-Gleichung lautet E = E° − (RT/nF)·ln Q. Wie ändert sich das Elektrodenpotential einer Halbzelle, wenn die Konzentration des oxidierten Redox-Partners bei 25 °C um den Faktor 10 steigt (n = 1)?",
        answerA = "E bleibt unverändert, da E° konstant ist",
        answerB = "E sinkt um ca. 59 mV",
        answerC = "E steigt um ca. 118 mV",
        answerD = "E steigt um ca. 59 mV",
        correctAnswer = 3, // D
        explanation = "Bei 25 °C gilt RT/F ≈ 25,7 mV. Die Nernst-Gleichung für eine Reduktionshalbreaktion (Ox + ne⁻ → Red) lautet E = E° − (59,16/n)·log([Red]/[Ox]) mV. Steigt [Ox] um Faktor 10 bei n=1, sinkt [Red]/[Ox] um Faktor 10, log([Red]/[Ox]) sinkt um 1, und E steigt um 59,16 mV ≈ 59 mV. Mehr Oxidationsmittel erhöht das Reduktionspotential.",
        difficulty = 3,
        funFact = "Der Faktor 59,16 mV/Dekade bei 25 °C ist der Ursprung der pH-Abhängigkeit von Elektroden: Pro pH-Einheit ändert sich das Potential einer Glaselektrode um genau 59 mV."
    ),

    // Question 7 – Electrolysis  [correct: Überspannung → correctAnswer=0]
    Question(
        categoryId = 2,
        questionText = "Bei der Elektrolyse von Wasser beträgt die theoretische Zersetzungsspannung ca. 1,23 V. In der Praxis sind jedoch 1,8–2,0 V nötig. Woher stammt dieser Mehraufwand hauptsächlich?",
        answerA = "Überspannung (Overpotential) durch kinetische Hemmung der Elektrodenreaktionen",
        answerB = "Ohmscher Widerstand des Elektrolyten und der Elektroden",
        answerC = "Thermodynamische Korrekturen durch den Aktivitätskoeffizienten",
        answerD = "Rückdiffusion der entstehenden Gase zur Gegenelektrode",
        correctAnswer = 0, // A
        explanation = "Die Differenz zwischen theoretischer Zersetzungsspannung (1,23 V) und praktisch benötigter Spannung heißt Überspannung (Overpotential). Sie entsteht durch kinetische Hemmungen: Aktivierungsenergie für Bindungsbruch/-bildung, Adsorptions-/Desorptionsprozesse und Ladungsübertragungskinetik. Besonders die Sauerstoffentwicklung (OER) hat ein hohes Overpotential (~0,3–0,5 V), da O–O-Bindungsbildung kinetisch schwierig ist.",
        difficulty = 3,
        funFact = null
    ),

    // Question 8 – Fuel Cells  [correct: Protonen durch Membran → correctAnswer=2]
    Question(
        categoryId = 2,
        questionText = "In einer Protonenaustauschmembran-Brennstoffzelle (PEMFC) wird Nafion® als Membran verwendet. Welche Funktion hat diese Membran primär?",
        answerA = "Sie katalysiert die Oxidation von Wasserstoff an der Anode",
        answerB = "Sie leitet Elektronen vom Anoderaum zum Kathodenraum",
        answerC = "Sie lässt selektiv Protonen (H⁺) passieren, ist aber elektronenundurchlässig",
        answerD = "Sie speichert Wasserstoff in ihren Polymerketten zwischen",
        correctAnswer = 2, // C
        explanation = "Nafion® ist ein sulfoniertes Tetrafluorethylen-Polymer (PTFE-basiert) mit Sulfonsäuregruppen (-SO₃H). Es ist ein Protonenleiter: H⁺-Ionen wandern durch die hydratisierten Sulfonsäurekanäle von der Anode zur Kathode. Gleichzeitig ist Nafion® elektronenundurchlässig, sodass Elektronen den äußeren Stromkreis nehmen müssen — das ist der Grundprinzip der Stromerzeugung.",
        difficulty = 3,
        funFact = "Nafion® wurde 1966 von Walther Grot bei DuPont entwickelt. Es war ursprünglich für den Einsatz in Chlor-Alkali-Elektrolysezellen gedacht und wurde erst später für Brennstoffzellen entdeckt."
    ),

    // Question 9 – Corrosion Electrochemistry  [correct: Cl⁻ penetriert Passivschicht → correctAnswer=3]
    Question(
        categoryId = 2,
        questionText = "Welcher elektrochemische Mechanismus liegt der Lochfraßkorrosion (Pitting) von rostfreiem Stahl in chloridhaltigen Medien zugrunde?",
        answerA = "Chloridionen erhöhen den pH-Wert und lösen die Passivschicht alkalisch auf",
        answerB = "Cl⁻ fungiert als Kathodenschutzmittel und macht Bereiche kathodisch aktiv",
        answerC = "Die erhöhte Ionenstärke durch NaCl senkt den Korrosionsschutz durch Änderung der Elektrolytleitfähigkeit",
        answerD = "Chloridionen penetrieren die Cr₂O₃-Passivschicht lokal, bilden lösliche Chromchloride und ermöglichen autokatalytische anodische Auflösung",
        correctAnswer = 3, // D
        explanation = "Bei Lochfraßkorrosion dringen Chloridionen in Fehlstellen der passiven Cr₂O₃-Schicht ein, verdrängen Sauerstoffionen und bilden lösliche Metallchloride (z.B. CrCl₃). Die lokale Hydrolyse senkt den pH auf 1–3 und verhindert Repassivierung. Der Prozess ist autokatalytisch: Das entstehende HCl zerstört die Passivschicht weiter, während der restliche Stahl als Kathode dient.",
        difficulty = 3,
        funFact = null
    ),

    // Question 10 – Batteries  [correct: Li⁺ interkaliert in Graphit → correctAnswer=0]
    Question(
        categoryId = 2,
        questionText = "In einem Lithium-Ionen-Akku mit LiCoO₂-Kathode und Graphit-Anode: Was passiert beim Ladevorgang an der Graphitanode?",
        answerA = "Li⁺-Ionen interkalieren in die Graphitschichten und werden dort elektrochemisch zu Li⁰ reduziert",
        answerB = "Li⁺-Ionen werden oxidiert und als Li-Metall abgeschieden",
        answerC = "Graphit wird zu CO₂ oxidiert und gibt dabei Elektronen ab",
        answerD = "Li⁺-Ionen bilden an der Graphitoberfläche LiC₆-Komplexe durch kovalente Bindungen",
        correctAnswer = 0, // A
        explanation = "Beim Laden wandern Li⁺-Ionen von der LiCoO₂-Kathode durch den Elektrolyt zur Graphitanode. Dort werden sie zwischen die Graphenschichten eingelagert (Interkalation) und durch die zugeführten Elektronen zu Li⁰ reduziert: C₆ + Li⁺ + e⁻ → LiC₆. Bei Entladung kehrt der Prozess um: LiC₆ → C₆ + Li⁺ + e⁻. Die Interkalation ist reversibel und verändert die Graphitstruktur nur minimal.",
        difficulty = 3,
        funFact = "LiC₆ (vollständig lithiiertes Graphit) hat eine goldgelbe Farbe und eine Kapazität von 372 mAh/g — das ist die theoretische Maximalkapazität von Graphit als Anodenmaterial."
    ),

    // ── 3. DEVELOPMENTAL NEUROSCIENCE ──────────────────────────────────────

    // Question 11 – Neural Tube Formation  [correct: Rhombenzephalon → correctAnswer=3]
    Question(
        categoryId = 2,
        questionText = "Der Neuralrohrschluss beim menschlichen Embryo beginnt ca. am 22. Entwicklungstag. An welchem Punkt beginnt der Schluss zuerst?",
        answerA = "Am kaudalen Ende (Neuroporus posterior) im Lumbalbereich",
        answerB = "Am rostralen Ende (Neuroporus anterior) im Prosenzephalon",
        answerC = "Im Bereich des zukünftigen Okzipitallappens",
        answerD = "Bilateral im Bereich des Rhombenzephalons (zukünftiges Kleinhirn)",
        correctAnswer = 3, // D
        explanation = "Der Neuralrohrschluss ist ein bidirektionaler Prozess, der nicht an einem, sondern an mehreren Initiationspunkten gleichzeitig beginnt. Der erste Schluss erfolgt im Rhombenzephalon-Bereich (zukünftiges Hinterhirn/Kleinhirnanlage) ca. am Tag 22 und schreitet dann sowohl rostral als auch kaudal fort. Der Neuroporus anterior schließt ca. am Tag 25, der Neuroporus posterior ca. am Tag 27.",
        difficulty = 3,
        funFact = "Folsäuremangel ist ein Hauptrisikofaktor für Neuralrohrdefekte. Durch Folsäuresupplementierung perikonzeptionell lässt sich das Risiko um 50–70% reduzieren."
    ),

    // Question 12 – Brain Development Stages  [correct: präfrontaler Kortex → correctAnswer=1]
    Question(
        categoryId = 2,
        questionText = "Welche der folgenden Hirnstrukturen entwickelt sich zuletzt und zeigt die prolongierteste postnatale Reifung beim Menschen?",
        answerA = "Basalganglien (Striatum und Pallidum)",
        answerB = "Präfrontaler Kortex (PFC)",
        answerC = "Hippocampus (limbisches System)",
        answerD = "Okzipitalkortex (primärer visueller Kortex)",
        correctAnswer = 1, // B
        explanation = "Der präfrontale Kortex (PFC) ist die phylogenetisch jüngste und ontogenetisch am spätesten ausgereifte Hirnstruktur. Synaptogenese, Myelinisierung und synaptic pruning im PFC setzen sich bis in das dritte Lebensjahrzehnt fort (ca. Mitte 20). Dies erklärt die späte Reifung von Impulskontrolle, Planung und abstraktem Denken. Der Okzipitalkortex hingegen reift innerhalb der ersten Lebensjahre.",
        difficulty = 3,
        funFact = null
    ),

    // Question 13 – Myelination  [correct: Oligodendrozyten → correctAnswer=2]
    Question(
        categoryId = 2,
        questionText = "Durch welche Zellen wird die Myelinisierung von Axonen im zentralen Nervensystem (ZNS) durchgeführt?",
        answerA = "Schwann-Zellen, die jeweils ein Axonsegment ummanteln",
        answerB = "Astrozyten, die Myelinproteine sezernieren",
        answerC = "Oligodendrozyten, die mehrere Axonsegmente gleichzeitig myelinisieren können",
        answerD = "Mikroglia, die Myelinvorläufer phagozytieren und umstrukturieren",
        correctAnswer = 2, // C
        explanation = "Im ZNS bilden Oligodendrozyten die Myelinscheiden. Ein einzelner Oligodendrozyt kann 20–60 verschiedene Axonsegmente gleichzeitig myelinisieren. Im peripheren Nervensystem (PNS) übernehmen Schwann-Zellen diese Funktion, aber jede Schwann-Zelle myelinisiert nur EIN Axonsegment. Myelin erhöht die Leitungsgeschwindigkeit von <1 m/s auf bis zu 120 m/s durch saltatorische Erregungsleitung.",
        difficulty = 3,
        funFact = "Bei Multipler Sklerose zerstört das Immunsystem Myelinscheiden im ZNS. Die dabei freiliegenden Ranvier-Schnürringe können die Signalübertragung dramatisch verlangsamen oder blockieren."
    ),

    // Question 14 – Critical Periods  [correct: synaptische Plastizität → correctAnswer=0]
    Question(
        categoryId = 2,
        questionText = "Was versteht man in der Entwicklungsneurowissenschaft unter einem 'kritischen Fenster' (critical period) am Beispiel des visuellen Systems?",
        answerA = "Eine Phase erhöhter synaptischer Plastizität, in der sensorische Erfahrungen die Schaltkreisarchitektur dauerhaft festlegen",
        answerB = "Den Zeitraum, in dem Retinaganglienzellen sterben, falls kein Licht einfällt",
        answerC = "Den Zeitraum der maximalen Myelinisierung der Sehbahn",
        answerD = "Die Phase, in der Astrozyten beginnen, überschüssige Synapsen zu eliminieren (Pruning)",
        correctAnswer = 0, // A
        explanation = "Kritische Perioden sind zeitlich begrenzte Entwicklungsphasen, in denen neuronale Schaltkreise besonders plastisch auf Erfahrungen reagieren. Im visuellen Kortex der Katze (Hubel & Wiesel, Nobelpreis 1981) führt Monokulardeprivation zur dauerhaften Dominanz des offenen Auges in Kortex-Schaltkreisen. Diese Plastizität ist nach der kritischen Periode stark reduziert. Molekulare Bremsen (PNN, Arc, etc.) begrenzen das Fenster.",
        difficulty = 3,
        funFact = "David Hubel und Torsten Wiesel erhielten 1981 den Nobelpreis für Physiologie für ihre Entdeckung der kritischen Perioden und der Verarbeitung visueller Information im Kortex."
    ),

    // Question 15 – Neuroplasticity  [correct: Ca²⁺ durch NMDA → correctAnswer=3]
    Question(
        categoryId = 2,
        questionText = "Welches Molekül gilt als zentraler Mediator der Langzeitpotenzierung (LTP) und synaptischen Plastizität im Hippocampus?",
        answerA = "GABA (γ-Aminobuttersäure) durch Aktivierung von GABA-A-Rezeptoren",
        answerB = "Dopamin-Ausschüttung aus dem ventralen tegmentalen Areal",
        answerC = "cAMP, produziert durch AMPA-Rezeptor-Aktivierung",
        answerD = "Ca²⁺-Einstrom durch NMDA-Rezeptoren, der nachgeschaltete Kinasen aktiviert",
        correctAnswer = 3, // D
        explanation = "LTP im Hippocampus hängt entscheidend von NMDA-Rezeptoren ab, die als 'Koinzidenzdetektor' fungieren: Sie öffnen nur, wenn gleichzeitig präsynaptisch Glutamat bindet UND die postsynaptische Membran depolarisiert ist (hebt Mg²⁺-Block auf). Der folgende Ca²⁺-Einstrom aktiviert CaMKII, PKC und andere Kinasen, die AMPA-Rezeptoren phosphorylieren und einbauen — die synaptische Übertragungsstärke steigt dauerhaft.",
        difficulty = 3,
        funFact = null
    ),

    // ── 4. SPACE WEATHER ───────────────────────────────────────────────────

    // Question 16 – Solar Flares  [correct: Röntgen/UV → correctAnswer=2]
    Question(
        categoryId = 2,
        questionText = "Welche Strahlungsart bei Sonneneruptionen (Solar Flares) erreicht die Erde als Erstes und verursacht Kurzwellenausfall auf der Tagseite?",
        answerA = "Geladene Protonen aus dem solaren Energiespektrum",
        answerB = "Koronale Massenauswürfe mit Plasmawolken",
        answerC = "Röntgen- und UV-Strahlung (Photonenfluss, Lichtgeschwindigkeit)",
        answerD = "Neutrinos aus der Kernfusion in der Chromosphäre",
        correctAnswer = 2, // C
        explanation = "Da Photonen mit Lichtgeschwindigkeit reisen, erreicht die Röntgen- und UV-Strahlung eines Flares die Erde in ~8 Minuten. Diese Strahlung ionisiert die D-Schicht der Ionosphäre auf der Tagseite, was Kurzwellenradio (HF) stark dämpft oder vollständig absorbiert (Shortwave Blackout). Geladene Teilchen (Protonen) folgen erst Stunden später, CMEs erst nach 1–3 Tagen.",
        difficulty = 3,
        funFact = "Der stärkste jemals aufgezeichnete Sonnensturm war das Carrington-Ereignis 1859. Wäre es heute eingetreten, würde es nach Schätzungen Schäden von bis zu 2 Billionen Dollar an der globalen Infrastruktur verursachen."
    ),

    // Question 17 – Coronal Mass Ejections  [correct: magnetische Rekonnektion → correctAnswer=0]
    Question(
        categoryId = 2,
        questionText = "Ein geoeffektiver koronaler Massenauswurf (CME) mit südwärts gerichtetem Bz-Magnetfeld ist besonders gefährlich. Warum ist die Bz-Südkomponente entscheidend?",
        answerA = "Sie ermöglicht magnetische Rekonnektion an der Magnetopause, was Energie und Plasma in die Magnetosphäre überträgt",
        answerB = "Sie erhöht den Sonnenwind-Druck und komprimiert die Magnetosphäre stärker",
        answerC = "Sie erzeugt Wirbelströme in der Ionosphäre, die Satelliten zerstören",
        answerD = "Südwärts gerichtete Felder beschleunigen Elektronen in der Plasmasphäre",
        correctAnswer = 0, // A
        explanation = "Ein CME mit negativem Bz (südwärts gerichtet, antiparallel zum erdmagnetischen Feld am Äquator) ermöglicht magnetische Rekonnektion an der Tagesseiten-Magnetopause. Dabei verschmelzen IMF-Feldlinien mit geomagnetischen Feldlinien, öffnen die Magnetosphäre und übertragen Energie und Plasma. Je stärker und längerfristiger das südwärts gerichtete Bz, desto heftiger der geomagnetische Sturm (Dst-Index).",
        difficulty = 3,
        funFact = null
    ),

    // Question 18 – Magnetosphere Interactions  [correct: Protonen innen, Elektronen außen → correctAnswer=1]
    Question(
        categoryId = 2,
        questionText = "Was versteht man unter dem 'Van-Allen-Strahlungsgürtel' und welche Teilchen dominieren im inneren Gürtel?",
        answerA = "Magnetisch eingefangene Elektronen im inneren und Protonen im äußeren Gürtel",
        answerB = "Magnetisch eingefangene energiereiche Protonen im inneren Gürtel (10–100 MeV) und Elektronen im äußeren",
        answerC = "Kosmische Strahlung aus der Galaxis, die durch das Erdfeld abgelenkt wird",
        answerD = "Ionosphärische Ionen, die durch Sonnenwind-Interaktion aufgeheizt werden",
        correctAnswer = 1, // B
        explanation = "Die Van-Allen-Gürtel sind toroide Regionen im Erdmagnetfeld, in denen geladene Teilchen durch magnetische Spiegelung gefangen sind. Der innere Gürtel (1,5–2,5 Erdradien) enthält hauptsächlich hochenergetische Protonen (10–100 MeV) aus kosmischer Strahlung und Neutronen-Zerfall. Der äußere Gürtel (4–7 Erdradien) enthält hauptsächlich MeV-Elektronen und ist dynamisch variabel.",
        difficulty = 3,
        funFact = "Die Van-Allen-Gürtel wurden 1958 durch die erste amerikanische Raumsonde Explorer 1 entdeckt. Das Gerät wurde von James Van Allen entwickelt, nach dem die Gürtel benannt sind."
    ),

    // Question 19 – Space Radiation Effects  [correct: HZE-Ionen → correctAnswer=3]
    Question(
        categoryId = 2,
        questionText = "Welche biologische Wirkung hat hochenergetische galaktische kosmische Strahlung (GCR) auf Astronauten auf dem Weg zum Mars besonders?",
        answerA = "Hauptsächlich Hautverbrennungen durch UV-Anteil der GCR",
        answerB = "Niederenergetische Photonen induzieren Radikale, die Proteinoxidation verursachen",
        answerC = "GCR wirkt hauptsächlich durch thermische Effekte auf Zellmembranen",
        answerD = "Schwere HZE-Ionen (hochgeladene schwere Kerne) erzeugen dichte Ionisationsspuren und schädigen DNA irreparabel durch Doppelstrangbrüche",
        correctAnswer = 3, // D
        explanation = "Galaktische kosmische Strahlung enthält HZE-Teilchen (Heavy ions with high charge Z and high Energy): vollständig ionisierte schwere Atomkerne wie Fe-56 mit Energien von GeV/Nukleon. Beim Durchqueren biologischen Gewebes erzeugen sie dichte Ionisationsspuren (hohe lineare Energieübertragung, LET), die Doppelstrangbrüche in DNA und Clusterschäden erzeugen, welche fehlerhafte oder keine Reparatur auslösen. Dies erhöht das Krebsrisiko langfristig.",
        difficulty = 3,
        funFact = null
    ),

    // Question 20 – Auroral Substorms  [correct: magnetische Rekonnektion Magnetoschweif → correctAnswer=2]
    Question(
        categoryId = 2,
        questionText = "Was ist der physikalische Auslöser eines polaren Substorms (auroral substorm), der zu intensiver Nordlichtaktivität führt?",
        answerA = "Direkte Sonnenstrahlung ionisiert die Atmosphäre und erzeugt Stickstoff-Emission",
        answerB = "Überhitzung der Plasmapause durch Protonenregen aus dem Sonnenwind",
        answerC = "Explosive magnetische Rekonnektion im Geomagnetoschwanz entlädt gespeicherte Energie in die Ionosphäre",
        answerD = "Instabilitäten in der E-Schicht der Ionosphäre erzeugen Alfvén-Wellen",
        correctAnswer = 2, // C
        explanation = "Substorms beginnen mit einer Aufladungsphase (growth phase), in der Energie im Magnetoschweif gespeichert wird. In der Expansionsphase entlädt sich diese plötzlich durch explosive magnetische Rekonnektion im Magnetoschweif (bei ~15–20 Erdradien). Feldausgerichtete Ströme (Birkeland-Ströme) transportieren Energie zur Ionosphäre, beschleunigen Elektronen, die beim Einschlag in die Atmosphäre Nordlicht (Aurora) erzeugen.",
        difficulty = 3,
        funFact = "Birkeland-Ströme transportieren während eines starken Substorms bis zu 1 Million Ampere zwischen Magnetosphäre und Ionosphäre — das entspricht der Leistung von etwa 1.000 Kernkraftwerken."
    ),

    // ── 5. PROTEIN CHEMISTRY ──────────────────────────────────────────────

    // Question 21 – Amino Acid Structure  [correct: Prolin → correctAnswer=3]
    Question(
        categoryId = 2,
        questionText = "Welche Aminosäure besitzt als einzige ein Pyrrolidinring-System als Seitenkette und unterbricht dadurch Alpha-Helices?",
        answerA = "Glycin (kleinste Aminosäure ohne echte Seitenkette)",
        answerB = "Tryptophan (Indolring-haltige Seitenkette)",
        answerC = "Histidin (Imidazolring als Seitenkette)",
        answerD = "Prolin (sekundäre Aminogruppe im Ring, keine freie NH-Gruppe)",
        correctAnswer = 3, // D
        explanation = "Prolin enthält eine sekundäre Aminogruppe, deren Stickstoff Teil des Pyrrolidinrings ist. Dadurch fehlt das H am Stickstoff, das normalerweise Wasserstoffbrücken in Alpha-Helices bildet. Prolin verursacht außerdem eine Knickung der Peptidkette (Einschränkung des φ-Winkels) und unterbricht so die reguläre Helix-Geometrie. Es wirkt als 'Helix-Brecher' und kommt häufig an Wendepunkten in Proteinstrukturen vor.",
        difficulty = 3,
        funFact = "In Kollagen ist das Triplehelix-Muster auf Gly-Pro-Hyp-Wiederholungen aufgebaut. Hydroxyprolin (Hyp) ist keine gencodierte Standardaminosäure, sondern entsteht posttranslational durch Prolyl-4-Hydroxylase — eine Vitamin-C-abhängige Reaktion."
    ),

    // Question 22 – Peptide Bonds  [correct: planar, rigide → correctAnswer=0]
    Question(
        categoryId = 2,
        questionText = "Peptidbindungen (-CO-NH-) haben partiellen Doppelbindungscharakter. Welche Konsequenz hat dies für die Proteinstruktur?",
        answerA = "Die sechs Atome der Peptidbindungsebene liegen planar und die Bindung ist rigide (cis/trans-Isomerie möglich)",
        answerB = "Freie Rotation um die Peptidbindung macht Proteine extrem flexibel",
        answerC = "Peptidbindungen können durch Hitze zu kovalenten Quervernetzungen werden",
        answerD = "Doppelbindungscharakter erhöht die Hydrolyseresistenz gegen alle Proteasen",
        correctAnswer = 0, // A
        explanation = "Durch Resonanz zwischen C=O und C-N entsteht partieller Doppelbindungscharakter (Bindungsordnung ~1,4). Dies macht die Peptidbindung planar (alle 6 Atome: Cα-CO-NH-Cα in einer Ebene) und verhindert freie Rotation. Fast immer liegt die trans-Konfiguration vor (ω ≈ 180°), nur Pro-Peptidbindungen zeigen häufiger cis (~10%). Freiheitsgrade entstehen durch φ (N-Cα) und ψ (Cα-C).",
        difficulty = 3,
        funFact = null
    ),

    // Question 23 – Protein Secondary Structure  [correct: H-Brücken senkrecht → correctAnswer=1]
    Question(
        categoryId = 2,
        questionText = "In einem β-Faltblatt (β-sheet): Welche Art von Wasserstoffbrückenbindungen stabilisiert die Struktur und wo verlaufen die Bindungen relativ zur Kettenrichtung?",
        answerA = "Intramolekulare H-Brücken parallel zur Strangrichtung zwischen je zwei benachbarten Carbonyl-Gruppen",
        answerB = "Senkrecht zur Strangrichtung, zwischen NH-Gruppen und C=O-Gruppen benachbarter Stränge",
        answerC = "Diagonal zwischen Seitenketten benachbarter Aminosäuren",
        answerD = "Zwischen Stickstoff und Schwefel der Cysteinreste in Disulfidbrücken",
        correctAnswer = 1, // B
        explanation = "Im β-Faltblatt verlaufen die Wasserstoffbrückenbindungen senkrecht (oder leicht schräg) zur Richtung der β-Stränge: NH-Gruppen eines Strangs bilden H-Brücken zu C=O-Gruppen des benachbarten Strangs. Die Seitenketten zeigen alternierend über und unter die Ebene. Parallele Faltblätter (gleichläufige Stränge) und antiparallele Faltblätter (gegensätzliche Stränge) unterscheiden sich in Geometrie und H-Brücken-Stärke.",
        difficulty = 3,
        funFact = "Spinnenseide besteht zu großen Teilen aus β-Faltblattstrukturen. Diese Anordnung verleiht ihr eine Zugfestigkeit, die das 5-fache von Stahl erreicht — bei einem Bruchteil des Gewichts."
    ),

    // Question 24 – Denaturation  [correct: hydrophober Effekt + H-Brücken → correctAnswer=2]
    Question(
        categoryId = 2,
        questionText = "Chaotrope Agenzien wie Harnstoff (8 M) und Guanidiniumchlorid denaturieren Proteine. Durch welchen Hauptmechanismus?",
        answerA = "Kovalente Modifikation von Cysteinen durch Oxidation der Thiolgruppen",
        answerB = "Ansäuerung der Lösung unter den isoelektrischen Punkt des Proteins",
        answerC = "Aufbrechung des hydrophoben Effekts und Abschwächung von Wasserstoffbrücken durch Konkurrenz mit Wassermolekülen",
        answerD = "Chelierung von Metallionen, die für die Tertiärstruktur essentiell sind",
        correctAnswer = 2, // C
        explanation = "Chaotrope Agenzien denaturieren durch mehrere Mechanismen: (1) Sie stören die Wasserstruktur und schwächen den hydrophoben Effekt, der hydrophobe Kernbereiche zusammenhält. (2) Sie bilden H-Brücken mit Peptidrückgrat und Seitenketten, konkurrieren mit intramolekularen H-Brücken. Bei 8 M Harnstoff ist das Wasser-zu-Harnstoff-Verhältnis so, dass fast jede NH/CO-Gruppe mit Harnstoff interagiert statt intramolekular.",
        difficulty = 3,
        funFact = null
    ),

    // Question 25 – Enzyme Kinetics  [correct: kompetitiv bindet aktives Zentrum → correctAnswer=3]
    Question(
        categoryId = 2,
        questionText = "Ein kompetitiver Inhibitor erhöht scheinbar den Km-Parameter ohne Veränderung von Vmax. Was ist die molekulare Erklärung?",
        answerA = "Er verringert kcat, sodass weniger Produkt pro Zeiteinheit gebildet wird",
        answerB = "Er bindet am allosterischen Zentrum und verändert die Konformation des aktiven Zentrums dauerhaft",
        answerC = "Er erhöht die Aktivierungsenergie der Reaktion durch Bindung am Substrat",
        answerD = "Er bindet reversibel am aktiven Zentrum und reduziert die Substratbindungsaffinität scheinbar, wird aber bei hoher Substratkonzentration verdrängt",
        correctAnswer = 3, // D
        explanation = "Kompetitive Inhibitoren binden reversibel am aktiven Zentrum und konkurrieren mit dem Substrat. Bei [S] >> [I] kann das Substrat den Inhibitor verdrängen — deshalb bleibt Vmax unverändert (bei gesättigender Substratkonzentration). Allerdings ist mehr Substrat nötig, um halbmaximale Geschwindigkeit zu erreichen: Km,app = Km(1 + [I]/Ki) steigt. Lineweaver-Burk-Diagramm zeigt erhöhte Steigung (=Km/Vmax) bei gleichem y-Achsenabschnitt.",
        difficulty = 3,
        funFact = "Statine (z.B. Atorvastatin) sind kompetitive Inhibitoren der HMG-CoA-Reduktase. Sie wirken als Substrat-Analoga und senken so die Cholesterinbiosynthese — eine der erfolgreichsten Wirkstoffklassen der Geschichte."
    ),

    // ── 6. HYDROLOGY ──────────────────────────────────────────────────────

    // Question 26 – Aquifer Types  [correct: Überdruck → correctAnswer=0]
    Question(
        categoryId = 2,
        questionText = "Was unterscheidet einen gespannten Grundwasserleiter (artesischer Aquifer) von einem ungespannten?",
        answerA = "Im gespannten Aquifer steht das Grundwasser unter hydrostatischem Überdruck und der Wasserspiegel im Brunnen liegt über der Aquifer-Oberkante",
        answerB = "Gespannte Aquifere liegen tiefer als 100 m unter der Erdoberfläche",
        answerC = "Gespannte Aquifere haben keine Deckschicht und stehen in direktem Kontakt mit der Atmosphäre",
        answerD = "Gespannte Aquifere enthalten ausschließlich fossiles Grundwasser ohne aktive Neubildung",
        correctAnswer = 0, // A
        explanation = "Ein gespannter (artesischer) Aquifer ist durch eine undurchlässige Deckschicht (Aquitard) nach oben abgedichtet. Das Grundwasser steht unter hydrostatischem Druck — der piezometrische Spiegel liegt über der Aquifer-Oberkante. Bei artesischen Brunnen kann Wasser spontan aufsteigen (artesischer Überlauf). Ungespannte Aquifere haben eine freie Oberfläche (Grundwasserspiegel) in Kontakt mit Porenwasser in der Vadoszone.",
        difficulty = 3,
        funFact = "Das Große Artesische Becken in Australien ist das größte artesische Aquifer-System der Welt (1,7 Millionen km²). Seit 1878 fließt Wasser ohne Pumpen aus Bohrlöchern — aus bis zu 3.000 m Tiefe."
    ),

    // Question 27 – Watershed Dynamics  [correct: verzögerter Grundwasseranteil → correctAnswer=2]
    Question(
        categoryId = 2,
        questionText = "Was versteht man unter dem Konzept des 'Basisabflusses' (baseflow) in der Einzugsgebietshydrologie?",
        answerA = "Den maximalen Abfluss während eines Hochwasserereignisses",
        answerB = "Die Verdunstungsmenge aus einem Einzugsgebiet während der Trockenperiode",
        answerC = "Den Anteil des Flussdurchflusses, der durch verzögerte Grundwasserspende und Zwischenabfluss gespeist wird",
        answerD = "Den Mindestabfluss, der für Ökosysteme gesetzlich festgelegt sein muss",
        correctAnswer = 2, // C
        explanation = "Basisabfluss (baseflow) ist die Komponente des Flussdurchflusses, die nicht aus direktem Oberflächenabfluss nach Niederschlag stammt, sondern durch langsame Entleerung von Grundwasserleitern und Bodenwasserspeichern gespeist wird. Er hält Flüsse auch in Trockenperioden wasserführend. Die Trennung von Direktabfluss und Basisabfluss erfolgt durch Hydrographtrennung (z.B. nach der Methode der Wendepunkte).",
        difficulty = 3,
        funFact = null
    ),

    // Question 28 – Groundwater Contamination  [correct: LNAPL schwimmt, DNAPL sinkt → correctAnswer=1]
    Question(
        categoryId = 2,
        questionText = "LNAPL (Light Non-Aqueous Phase Liquids) wie Benzin verhalten sich im Untergrund anders als DNAPL (Dense NAPL wie Chloroform). Was ist der entscheidende Unterschied?",
        answerA = "LNAPL sinkt durch die gesättigte Zone bis zum Grundwasserleiter-Boden, DNAPL schwimmt auf dem Grundwasser",
        answerB = "LNAPL schwimmt auf dem Grundwasserspiegel, DNAPL sinkt durch die gesättigte Zone bis zu Aquitarden",
        answerC = "LNAPL ist wasserlöslich und bildet keine freie Phase, DNAPL ist unlöslich",
        answerD = "Beide verhalten sich gleich, der Unterschied liegt nur in der Viskosität",
        correctAnswer = 1, // B
        explanation = "LNAPLs (Dichte < 1 g/cm³, z.B. Benzin, Heizöl) schwimmen auf dem Grundwasserspiegel und bilden eine Linse in der Kapillarzone. DNAPLs (Dichte > 1 g/cm³, z.B. chlorierte Lösungsmittel wie TCE, PCE) sinken durch die gesättigte Zone und akkumulieren an Aquitarden oder in Hohlräumen tief im Grundwasserleiter — was die Sanierung extrem schwierig macht.",
        difficulty = 3,
        funFact = "TCE (Trichlorethylen) ist als DNAPL einer der häufigsten Grundwasserkontaminanten an alten Industriestandorten. Die Sanierung solcher 'Quellzonen' kann Jahrzehnte dauern und Kosten von Millionen Euro verursachen."
    ),

    // Question 29 – Karst Hydrology  [correct: schnelle turbulente Strömung → correctAnswer=3]
    Question(
        categoryId = 2,
        questionText = "Was ist das hydrologische Charakteristikum von Karstaquiferen im Vergleich zu porösen Grundwasserleitern?",
        answerA = "Karstaquifere haben höhere Porosität aber niedrigere hydraulische Durchlässigkeit",
        answerB = "In Karstaquiferen findet ausschließlich laminare Grundwasserströmung nach dem Darcy-Gesetz statt",
        answerC = "Karstaquifere haben keine Verbindung zu Quellen, da alles Wasser im Gestein verbleibt",
        answerD = "Karstaquifere zeigen schnelle, turbulente Strömung durch Klüfte und Hohlräume mit heterogener Fließverteilung und hohen Durchfluss-Variabilitäten",
        correctAnswer = 3, // D
        explanation = "Karstaquifere entstehen durch Auflösung löslicher Gesteine (Kalk, Dolomit, Gips). Sie zeigen doppelte Porosität: Matrix-Porosität (langsam, laminar) und bevorzugte Fließwege durch Klüfte, Höhlensysteme und Conduits (schnell, oft turbulent). Das Darcy-Gesetz gilt nicht für Conduit-Strömung. Karstquellen reagieren innerhalb von Stunden auf Niederschlag, haben hohe Abfluss-Variabilität und sind anfällig für schnelle Kontamination.",
        difficulty = 3,
        funFact = null
    ),

    // Question 30 – River Discharge  [correct: Q = A · v → correctAnswer=0]
    Question(
        categoryId = 2,
        questionText = "Mit welcher Formel berechnet man den Abfluss Q (Discharge) eines Flusses und welche physikalischen Größen sind darin enthalten?",
        answerA = "Q = A · v, wobei A der Querschnitt des Flussbetts und v die mittlere Fließgeschwindigkeit ist",
        answerB = "Q = ρ · g · h, wobei ρ die Dichte, g die Erdbeschleunigung und h die Wassertiefe ist",
        answerC = "Q = P − ET, wobei P der Niederschlag und ET die Evapotranspiration ist",
        answerD = "Q = n · R^(2/3) · S^(1/2), wobei n der Manning-Koeffizient ist",
        correctAnswer = 0, // A
        explanation = "Der Abfluss (Discharge) Q [m³/s] berechnet sich als Q = A · v, wobei A [m²] die benetzte Querschnittsfläche des Flussbetts und v [m/s] die mittlere Fließgeschwindigkeit ist. In der Praxis wird Q durch Flügelmessungen, ADCP (Acoustic Doppler) oder Pegelkurven (rating curves) bestimmt. Die Manning-Gleichung (Antwort D) berechnet die Fließgeschwindigkeit v und nicht direkt Q.",
        difficulty = 3,
        funFact = "Der Amazonas transportiert etwa 20% des gesamten globalen Süßwasserabflusses in die Ozeane — ca. 209.000 m³/s im Jahresmittel."
    ),

    // ── 7. ACOUSTICS ADVANCED ─────────────────────────────────────────────

    // Question 31 – Room Acoustics  [correct: -60 dB Abfall → correctAnswer=1]
    Question(
        categoryId = 2,
        questionText = "Die Sabine-Formel lautet T60 = 0,161 · V / (α · S). Was beschreibt T60 in der Raumakustik?",
        answerA = "Die Frequenz, bei der ein Raum seine maximale Nachhallzeit erreicht",
        answerB = "Die Zeit in Sekunden, bis der Schalldruckpegel um 60 dB nach Abschalten der Schallquelle abgefallen ist",
        answerC = "Die Zeitkonstante für den Aufbau des stationären Schalldrucks im Raum",
        answerD = "Den Zeitunterschied zwischen Direktschall und erstem Reflexionsecho",
        correctAnswer = 1, // B
        explanation = "T60 (Nachhallzeit, Reverberation Time) ist die Zeit in Sekunden, die der Schalldruckpegel nach dem Abschalten einer Schallquelle benötigt, um um 60 dB abzufallen (auf 10⁻⁶ der ursprünglichen Energie). In der Sabine-Formel ist V das Raumvolumen [m³], α der mittlere Absorptionsgrad und S die Gesamtoberfläche [m²]. Die optimale T60 für Sprache liegt bei 0,3–0,6 s, für Orchestermusik bei 1,5–2,0 s.",
        difficulty = 3,
        funFact = "Wallace Clement Sabine entdeckte die Nachhallformel 1900 im Fogg Art Museum der Harvard University. Er trug Sitzkissen aus dem Auditorium in den Raum, um die Absorption zu variieren — ein frühes Experiment der quantitativen Raumakustik."
    ),

    // Question 32 – Impedance Matching  [correct: Fläche + Hebeleffekt kombiniert → correctAnswer=2]
    Question(
        categoryId = 2,
        questionText = "Das Mittelohr fungiert als Impedanz-Anpassungsglied zwischen Luft und Perilymphe. Durch welche mechanischen Prinzipien wird die Impedanz angepasst?",
        answerA = "Ausschließlich durch den Hebeleffekt der Gehörknöchelchen (Hammer, Amboss, Steigbügel)",
        answerB = "Resonanzabsorption durch die Eustachische Röhre",
        answerC = "Flächenverhältnis Trommelfell/Steigbügel-Fußplatte (~17:1) und Hebeleffekt der Gehörknöchelchen (~1,3:1) kombiniert",
        answerD = "Aktive Verstärkung durch Haarzellen im Mittelohr",
        correctAnswer = 2, // C
        explanation = "Luft hat eine akustische Impedanz von ~415 Pa·s/m, Perilymphe von ~1,5 MRayl. Ohne Anpassung würden ~99,9% der Schallenergie reflektiert (-30 dB). Das Mittelohr gleicht dies aus durch: (1) Flächenverhältnis Trommelfell (~55 mm²) zu Steigbügelfußplatte (~3,2 mm²) ≈ 17-fache Druckerhöhung; (2) Hebeleffekt der Ossikeln ≈ 1,3-fach. Kombination ≈ 22-fache Druckverstärkung, was ~26 dB entspricht.",
        difficulty = 3,
        funFact = null
    ),

    // Question 33 – Acoustic Metamaterials  [correct: lokale Resonatoren gegenphasig → correctAnswer=3]
    Question(
        categoryId = 2,
        questionText = "Akustische Metamaterialien können negative effektive Massendichte oder negativen Kompressionsmodul aufweisen. Was ermöglicht eine negative effektive Massendichte?",
        answerA = "Materialien mit extrem niedriger Dichte wie Aerogel",
        answerB = "Poröse Medien mit negativem Brechungsindex für sichtbares Licht",
        answerC = "Flüssigkeiten mit negativer Kompressibilität bei Überdruck",
        answerD = "Subwellenlänge-Resonatoren (local resonance), bei denen innere Massen bei der Anregungsfrequenz gegenphasig zur äußeren Bewegung schwingen",
        correctAnswer = 3, // D
        explanation = "Negative effektive Massendichte tritt in akustischen Metamaterialien auf, die lokal resonante Subwellenlängen-Einheiten enthalten (z.B. Kugeln mit weicher Beschichtung). Bei Frequenzen oberhalb der lokalen Resonanzfrequenz schwingen innere Massen phasenverschoben (gegenphasig) zur äußeren Erregung. Das System reagiert dann auf äußere Kraft mit entgegengesetzter Beschleunigung — die effektive Dichte wird negativ. Dies ermöglicht vollständige Schallreflexion ohne Energiedissipation.",
        difficulty = 3,
        funFact = "Akustische Metamaterialien wurden 2000 von Zhengyou Liu et al. an der Hong Kong University of Science and Technology entdeckt. Seither werden sie für schallisolierende Barrieren erforscht, die dünn aber hocheffektiv sind."
    ),

    // Question 34 – Ultrasonic Imaging  [correct: räumliche Pulslänge → correctAnswer=0]
    Question(
        categoryId = 2,
        questionText = "Das axiale Auflösungsvermögen eines Ultraschallsystems hängt von welchem Parameter primär ab?",
        answerA = "Von der räumlichen Pulslänge (Wellenlänge × Anzahl der Schwingungen im Puls)",
        answerB = "Von der Apertur des Schallkopfs (je größer, desto besser)",
        answerC = "Von der Schallgeschwindigkeit im Medium (höhere Geschwindigkeit = bessere Auflösung)",
        answerD = "Von der Eindringtiefe: tiefe Strukturen werden immer schlechter aufgelöst als flache",
        correctAnswer = 0, // A
        explanation = "Das axiale Auflösungsvermögen (entlang der Schallausbreitungsrichtung) ist ~λ/2 bei einer Schwingung (oder genauer: SPL/2, wobei SPL = räumliche Pulslänge). SPL = Wellenlänge × Anzahl der Schwingungszyklen im Puls. Kurze Pulse (wenige Zyklen) und hohe Frequenzen (kurze Wellenlänge) verbessern die axiale Auflösung. Lateral hängt die Auflösung von der Apertur und Fokussierung ab.",
        difficulty = 3,
        funFact = null
    ),

    // ── 8. GENETICS & DISEASE ─────────────────────────────────────────────

    // Question 35 – Cystic Fibrosis  [correct: fehlerhafte Faltung → correctAnswer=1]
    Question(
        categoryId = 2,
        questionText = "Mukoviszidose (Cystische Fibrose) wird durch Mutationen im CFTR-Gen verursacht. Die häufigste Mutation ΔF508 führt zu welchem primären Defekt?",
        answerA = "Frühzeitiges Stoppcodon und kompletter CFTR-Expressionsverlust",
        answerB = "Fehlerhafte Proteinfaltung → CFTR wird im ER zurückgehalten und proteasomal abgebaut, nicht zur Plasmamembran transportiert",
        answerC = "Frameshift-Mutation, die zu einem verkürzten, nicht-funktionellen Protein führt",
        answerD = "Spleißfehler, der zu fehlerhafter mRNA-Prozessierung und Exon-Skipping führt",
        correctAnswer = 1, // B
        explanation = "ΔF508 ist eine In-Frame-Deletion von Phenylalanin an Position 508 der Nukleotidbindungsdomäne 1 (NBD1). Das entstehende Protein ist falsch gefaltet, erkennt es die ER-Qualitätskontrolle (Calnexin/Calreticulin, ERQC) und wird durch ERAD (ER-associated Degradation) via dem Ubiquitin-Proteasom-System abgebaut. Nur ~1% gelangt zur Plasmamembran. CFTR-Korrektoren (z.B. Lumacaftor) beheben die Fehlfaltung.",
        difficulty = 3,
        funFact = "Modulatoren wie Trikafta (Elexacaftor/Tezacaftor/Ivacaftor) korrigieren ΔF508-CFTR auf mehreren Ebenen gleichzeitig: Korrektoren verbessern das Trafficking, Potenziatoren öffnen den Kanal — mit dramatischer klinischer Wirksamkeit."
    ),

    // Question 36 – Sickle Cell Disease  [correct: GAG→GTG, Glu→Val → correctAnswer=2]
    Question(
        categoryId = 2,
        questionText = "Sichelzellanämie entsteht durch eine Punktmutation im β-Globin-Gen (HBB). Welche genaue Veränderung liegt auf DNA- und Proteinebene vor?",
        answerA = "Codon 17: AAG → TAG — Vorzeitiges Stoppcodon verkürzt β-Globin",
        answerB = "Codon 6: GAA → GTA (Glutaminsäure → Valin) mit identischer klinischer Wirkung",
        answerC = "Codon 6: GAG → GTG — Glutaminsäure (E) wird zu Valin (V); negative Ladung fehlt, hydrophobes Valin tritt auf",
        answerD = "Spleißstellen-Mutation in Intron 1 — reduzierte β-Globin-mRNA-Menge",
        correctAnswer = 2, // C
        explanation = "Die klassische Sichelzellmutation im HBB-Gen: Codon 6 ändert sich von GAG (Glutaminsäure, E) zu GTG (Valin, V) — ein einzelner Nukleotidaustausch (A→T im zweiten Codon-Basenpaar). Das entstehende HbS polymerisiert bei Desoxigenation durch hydrophobe Valin-Valin-Kontakte zwischen β-Ketten benachbarter Tetramere und bildet Fasern, die Erythrozyten in Sichelform verformen.",
        difficulty = 3,
        funFact = "Heterozygote Träger des HbS-Allels (Sichelzelltrait) sind relativ resistent gegen Plasmodium falciparum-Malaria. Dies erklärt, warum die Mutation in Malaria-endemischen Regionen Afrikas unter Selektionsdruck auf hohe Frequenzen stieg."
    ),

    // Question 37 – Huntington's Disease  [correct: Gain-of-function, Aggregate → correctAnswer=0]
    Question(
        categoryId = 2,
        questionText = "Huntington-Krankheit entsteht durch eine CAG-Trinukleotid-Expansion im HTT-Gen. Welcher pathologische Mechanismus steht im Vordergrund der Neurodegeneration?",
        answerA = "Gain-of-function: Polyglutamin-expandiertes Huntingtin bildet toxische Aggregate und stört Transkription, axonalen Transport und Mitochondrien",
        answerB = "Loss-of-function: Huntingtin-Protein verliert seine normale Funktion in der vesikulären Transportkette",
        answerC = "Haploinsuffizienz: Ein normales Allel reicht nicht aus",
        answerD = "Dominant-negativ: Mutantes Huntingtin hemmt das Wildtyp-Huntingtin durch direkte Protein-Protein-Interaktion",
        correctAnswer = 0, // A
        explanation = "Die CAG-Expansion kodiert einen polyGlutamin-Stretch (polyQ > 36 Glutamine = pathologisch). Gain-of-toxic-function dominiert: PolyQ-Huntingtin aggregiert zu Einschlusskörperchen, bindet und sequestriert Transkriptionsfaktoren (z.B. CBP), stört den axonalen Transport durch Kinesin/Dynein-Inhibition, beeinträchtigt Mitochondriendynamik und aktiviert Caspase-abhängige Apoptose in Striatum-Neuronen.",
        difficulty = 3,
        funFact = null
    ),

    // Question 38 – Down Syndrome  [correct: Non-disjunction mütterlich → correctAnswer=3]
    Question(
        categoryId = 2,
        questionText = "Trisomie 21 (Down-Syndrom) entsteht in ~95% der Fälle durch welchen Mechanismus?",
        answerA = "Chromosomale Translokation (Robertsonsche Translokation) zwischen Chromosom 21 und 14",
        answerB = "Mosaizismus durch somatische Non-disjunction in frühen Furchungsteilungen",
        answerC = "Uniparentale Disomie (beide Chromosom-21-Kopien vom Vater)",
        answerD = "Non-disjunction während Meiose I oder II, meist mütterlichen Ursprungs, mit zunehmendem Risiko bei steigendem Alter",
        correctAnswer = 3, // D
        explanation = "In ~95% der Down-Syndrom-Fälle liegt eine freie Trisomie 21 durch Non-disjunction vor — meist in der Meiose I der Mutter (~90% mütterlicher Ursprung). Bei Non-disjunction in Meiose I trennen sich homologe Chromosomen nicht, bei Meiose II trennen sich Schwesterchromatide nicht. Das mütterliche Altersrisiko steigt drastisch: 1:1.500 bei 20 Jahren, 1:100 bei 40 Jahren, 1:30 bei 45 Jahren.",
        difficulty = 3,
        funFact = "Das Down-Syndrom ist die häufigste chromosomale Erkrankung bei Lebendgeborenen weltweit (~1:700). Überraschend ist, dass die Trisomie-21-Rate bei Konzeptionen nicht mit dem Alter steigt — aber ältere Embryonen überleben häufiger bis zur Geburt."
    ),

    // ── 9. ORGANIC REACTIONS ──────────────────────────────────────────────

    // Question 39 – Grignard Reaction  [correct: 1-Phenylethanol → correctAnswer=2]
    Question(
        categoryId = 2,
        questionText = "Bei der Grignard-Reaktion von Methylmagnesiumbromid (CH₃MgBr) mit Benzaldehyd (C₆H₅CHO), gefolgt von Hydrolyse, entsteht welches Produkt?",
        answerA = "Benzylalkohol (primärer Alkohol)",
        answerB = "Acetophenon (Keton)",
        answerC = "1-Phenylethanol (sekundärer Alkohol)",
        answerD = "2-Phenylessigsäure (Carbonsäure)",
        correctAnswer = 2, // C
        explanation = "Das Grignard-Reagenz CH₃MgBr liefert einen Methylcarbanion-Äquivalent (CH₃⁻). Dieser nukleophile Angriff auf den elektrophilen Carbonyl-Kohlenstoff des Benzaldehyds bildet ein Magnesiummethylat-Intermediat: C₆H₅CH(O⁻MgBr⁺)CH₃. Nach Hydrolyse mit wässriger Säure entsteht 1-Phenylethanol (C₆H₅CH(OH)CH₃), ein sekundärer Alkohol. Aus Aldehyden entstehen durch Grignard immer sekundäre Alkohole.",
        difficulty = 3,
        funFact = "Victor Grignard erhielt 1912 den Nobelpreis für Chemie — als 32-Jähriger für seine Entdeckung der nach ihm benannten Reagenzien, die er 1900 als Doktorand bei Philippe Barbier entwickelte."
    ),

    // Question 40 – Diels-Alder Reaction  [correct: s-cis-Konformation, EWG am Dienophil → correctAnswer=1]
    Question(
        categoryId = 2,
        questionText = "Welche stereoelektronischen Anforderungen muss ein Dien für eine Diels-Alder-Reaktion erfüllen?",
        answerA = "Das Dien muss in der transoid-Konformation (s-trans) vorliegen und durch EWG aktiviert werden",
        answerB = "Das Dien muss konjugiert und bevorzugt in der s-cis-Konformation (cisoidal) vorliegen; EWG am Dienophil aktivieren die Reaktion",
        answerC = "Unconjugierte Diene reagieren schneller, da sie mehr Elektronendichte zur Verfügung stellen",
        answerD = "Das Dien muss konjugiert und in s-trans-Konformation sein; EDG am Dienophil sind nötig",
        correctAnswer = 1, // B
        explanation = "Für die Diels-Alder-[4+2]-Cycloaddition muss das Dien: (1) konjugiert sein (4π-System), (2) die s-cis-Konformation einnehmen können (die beiden C=C-Bindungen müssen cisoidal angeordnet sein, damit der sechsgliedrige Übergangszustand möglich ist). S-trans-Diene reagieren nicht. EDG am Dien (erhöhen Elektronendichte im HOMO) und EWG am Dienophil (senken LUMO-Energie) erniedrigen die HOMO-LUMO-Lücke und beschleunigen die Reaktion.",
        difficulty = 3,
        funFact = null
    ),

    // Question 41 – Friedel-Crafts Reaction  [correct: –M-Effekt, meta-Lenkung → correctAnswer=3]
    Question(
        categoryId = 2,
        questionText = "Warum deaktiviert eine Nitrogruppe (-NO₂) am Benzolring den Ring für elektrophile aromatische Substitution (EAS) und lenkt in meta-Position?",
        answerA = "Sterisch verhindert die Nitrogruppe ortho/para-Angriffe durch Größeneffekt",
        answerB = "Nitro ist ein -I-Substituent und zieht Elektronen durch Hyperkonjugation ab",
        answerC = "Die Nitrogruppe erhöht die Ringspannung im ortho/para-Wheland-Intermediat",
        answerD = "Die Nitrogruppe zieht durch starken -M-Effekt (Mesomerie) und -I-Effekt Elektrondichte aus dem Ring; ortho/para-Positionen werden besonders elektronenarm, sodass EAS bevorzugt in meta erfolgt",
        correctAnswer = 3, // D
        explanation = "Die Nitrogruppe (-NO₂) ist ein starker Elektronenakzeptor durch Mesomerie (-M) und Induktion (-I). Im Resonanzbild entzieht sie Elektrondichte bevorzugt aus ortho- und para-Positionen (diese Formen zeigen positive Partialladungen am Ring). Meta-Position ist weniger direkt betroffen. Das elektrophile Reagenz greift preferentiell meta an, weil dort das kationische Wheland-Intermediat die positiver geladene Ringposition vermeidet.",
        difficulty = 3,
        funFact = "Nitrobenzol ist das klassische Beispiel für einen meta-dirigierenden Deaktivator. Trotz der langsameren Reaktion (gegenüber Benzol ~10⁶-fach verlangsamt) findet die EAS noch statt — aber ausschließlich unter erzwungenen Bedingungen (rauchende H₂SO₄/HNO₃)."
    ),

    // Question 42 – Aldol Condensation  [correct: konjugiertes System, Mesomerie → correctAnswer=0]
    Question(
        categoryId = 2,
        questionText = "Bei der Aldolkondensation (Dehydratation nach Aldoladdition) entsteht ein α,β-ungesättigtes Carbonylprodukt. Durch welche Kräfte ist dieses Produkt thermodynamisch bevorzugt?",
        answerA = "Durch das konjugierte System (C=C-C=O) ermöglicht ausgedehnte π-Elektronendelokalisation und Mesomeriestabilisierung",
        answerB = "Durch Entropiegewinn bei der Wasserabspaltung als kleines Molekül",
        answerC = "Durch günstige cis-Geometrie der neu entstandenen Doppelbindung",
        answerD = "Durch Bildung eines sechsgliedrigen aromatischen Rings im Produkt",
        correctAnswer = 0, // A
        explanation = "Das α,β-ungesättigte Carbonylprodukt (Enon) ist thermodynamisch stabiler als das Aldolprodukt (β-Hydroxycarbonylverbindung). Der Hauptgrund ist die Konjugation zwischen C=C und C=O: Das π-System ist über beide Doppelbindungen delokalisiert, was zu Mesomeriestabilisierung führt. Zusätzlich treibt die Abspaltung von Wasser (kleines stabiles Molekül, Entropiegewinn) das Gleichgewicht auf die Seite des Kondensationsprodukts.",
        difficulty = 3,
        funFact = null
    ),

    // ── 10. COSMOCHEMISTRY ────────────────────────────────────────────────

    // Question 43 – Meteorite Types  [correct: kugelförmige Schmelztröpfchen → correctAnswer=2]
    Question(
        categoryId = 2,
        questionText = "Chondrite sind die häufigsten Meteoriten und enthalten Chondren. Was sind Chondren und welche kosmochemische Bedeutung haben sie?",
        answerA = "Feinkörnige Mineralaggregiate, die durch langsame Abkühlung in Asteroiden entstanden",
        answerB = "Karbonatminerale, die auf Wasseraktivität im Mutterkörper hinweisen",
        answerC = "Kugelförmige Silikatkügelchen (mm-Größe), die durch schnelle Abkühlung geschmolzener Tröpfchen im frühen Sonnensystem entstanden — älteste bekannte Schmelzobjekte (~4,567 Ga)",
        answerD = "Metallische Nickel-Eisen-Einschlüsse, die die Kern-Differenzierung des Mutterkörpers belegen",
        correctAnswer = 2, // C
        explanation = "Chondren sind sphärische bis subphärische Silikatkügelchen (Olivin, Pyroxen, Glas) mit Durchmessern von 0,1–10 mm. Sie entstanden durch rasches Aufschmelzen und Wiedererstarren von Staubpartikeln oder Aggregaten im frühen Sonnensystem (vor ~4,567 Ga), möglicherweise durch Schockwellen, blitzartige Entladungen oder bipolare Outflows. Sie sind die ältesten Schmelzprodukte des Sonnensystems und konservieren präsolare Isotopen-Signaturen.",
        difficulty = 3,
        funFact = "Der Allende-Meteorit, gefallen 1969 in Mexiko, enthält Calcium-Aluminium-reiche Einschlüsse (CAIs) — die ältesten festen Objekte des Sonnensystems mit einem Alter von 4,5672 Milliarden Jahren."
    ),

    // Question 44 – Stellar Abundances  [correct: höchste Bindungsenergie bei Fe → correctAnswer=3]
    Question(
        categoryId = 2,
        questionText = "Die kosmische Häufigkeit der Elemente zeigt eine charakteristische 'Eisenspitze' (Iron Peak). Was ist ihr physikalischer Ursprung?",
        answerA = "Eisen ist das häufigste Element, das im Big Bang nukleosynthetisiert wurde",
        answerB = "Eisen und seine Nachbarelemente sind Produkte des r-Prozesses in Neutronensternverschmelzungen",
        answerC = "Die Eisenspitze entsteht durch bevorzugte Spallation schwerer Elemente durch kosmische Strahlung",
        answerD = "Eisen und Nachbarelemente (Ni, Co, Cr) haben die höchste Bindungsenergie pro Nukleon — Kernfusion bis Eisen setzt Energie frei, danach verschlingt sie Energie",
        correctAnswer = 3, // D
        explanation = "⁵⁶Fe (und ⁵⁶Ni, das zu ⁵⁶Fe zerfällt) besitzt die höchste Bindungsenergie pro Nukleon (~8,8 MeV/Nukleon) aller Kerne. Sternfusion von Elementen leichter als Fe setzt Energie frei (exotherm). Fusion zu schwereren Elementen würde Energie benötigen (endotherm). Massereiche Sterne können bis zur Eisen-Kern-Synthese Energie gewinnen — dann kollabiert der Kern. Dies erklärt das kosmische Häufungsmaximum bei Fe, Co, Ni.",
        difficulty = 3,
        funFact = "Das Eisen in unserem Blut (Häm-Eisen) wurde vor Milliarden Jahren in massereichen Sternen synthetisiert, die als Supernovae explodierten. Wir sind buchstäblich aus Sternenstaub gemacht."
    ),

    // Question 45 – Isotope Ratios  [correct: massenabhängig Steigung ~0,52, MIF davon abweichend → correctAnswer=0]
    Question(
        categoryId = 2,
        questionText = "Was misst das Sauerstoffisotopen-Drei-Isotopen-Diagramm (δ¹⁷O vs. δ¹⁸O) in Meteoriten, und warum liegt die terrestrische Fraktionierungslinie (TFL) anders als Massenunabhängige Fraktionierung (MIF)?",
        answerA = "Massenabhängige Fraktionierung erzeugt δ¹⁷O ≈ 0,52·δ¹⁸O (TFL); MIF weicht davon durch nukleosynthetische Restvariationen oder photochemische Reaktionen ab",
        answerB = "Die TFL hat eine Steigung von 1, während MIF eine Steigung von 0,5 hat",
        answerC = "δ¹⁷O und δ¹⁸O sind identisch für alle Meteoriten, nur Kometen zeigen Abweichungen",
        answerD = "MIF entsteht durch radioaktiven Zerfall von ¹⁷O zu ¹⁶O in heißen Sternen",
        correctAnswer = 0, // A
        explanation = "Massenabhängige kinetische und Gleichgewichtsfraktionierung folgt der Beziehung δ¹⁷O ≈ 0,52·δ¹⁸O (TFL, Steigung ~0,52). Massenunabhängige Fraktionierung (MIF, Δ¹⁷O ≠ 0) weicht davon ab: Chondren und CAIs verschiedener Klassen liegen auf einer Linie der Steigung ~1 im Drei-Isotopen-Diagramm, was präsolare nukleosynthetische Heterogenitäten oder UV-Photolyse von CO im frühen Sonnensystem widerspiegelt.",
        difficulty = 3,
        funFact = null
    ),

    // Question 46 – Meteorite Types (Pallasite)  [correct: Olivin in Nickel-Eisen-Matrix → correctAnswer=1]
    Question(
        categoryId = 2,
        questionText = "Pallasitische Meteoriten gelten als Fenster in die Kern-Mantel-Grenze differenzierter Asteroiden. Aus welchen zwei Hauptkomponenten bestehen sie?",
        answerA = "Carbonat-Matrix mit eingeschlossenen Chondren",
        answerB = "Olivin-Kristalle eingebettet in eine Nickel-Eisen-Metallmatrix",
        answerC = "Pyroxen und Plagioklas (wie Basalt) mit metallischen Einschlüssen",
        answerD = "Silikatglas mit organischen Makromolekülen (wie CI-Chondrite)",
        correctAnswer = 1, // B
        explanation = "Pallasiten bestehen aus schönen Olivin-Einkristallen (Mg,Fe)₂SiO₄ eingebettet in eine Nickel-Eisen-Metallmatrix (Kamacit/Taenit). Sie entstammen vermutlich der Grenzregion zwischen dem Metallkern und dem Silikatmantel differenzierter Asteroiden, wo Metallschmelze mit Olivin coexistierte. Der Hauptgürtel-Asteroid (4) Vesta gilt als möglicher Ursprung einiger Pallasiten.",
        difficulty = 3,
        funFact = "Der Esquel-Pallasit aus Argentinien ist bekannt für seine ausnehmend großen, transparenten Olivin-Kristalle. Dünngeschliffene Scheiben sehen im Durchlicht wie Buntglas-Mosaike aus und werden als Edelsteine verkauft."
    ),

    // Question 47 – Cosmochemistry (Nucleosynthesis)  [correct: r-Prozess, Neutronensternverschmelzung → correctAnswer=2]
    Question(
        categoryId = 2,
        questionText = "Welcher astrophysikalische Prozess ist hauptverantwortlich für die Synthese der schweren Elemente jenseits von Eisen bis Uran, wie Gold (Au) und Platin (Pt)?",
        answerA = "Der s-Prozess (slow neutron capture) in AGB-Sternen bildet alle Elemente von Fe bis U",
        answerB = "Der p-Prozess (proton capture) in Typ-Ia-Supernovae erzeugt alle stabilen Isotope über Eisen",
        answerC = "Der r-Prozess (rapid neutron capture) bei Neutronensternverschmelzungen oder kollabierenden Supernovae produziert neutronenreiche schwere Kerne bis Uran",
        answerD = "Kosmische Strahlung spaltet Eisenkerne und baut dadurch schwere Elemente auf",
        correctAnswer = 2, // C
        explanation = "Der r-Prozess (rapid neutron capture) erfordert extreme Neutronenflüsse (>10²⁰ n/cm²/s) und kurze Zeitskalen. Er läuft in Neutronenstern-Neutronenstern-Verschmelzungen (Kilonovae) und möglicherweise bestimmten Supernovae ab. Kerne fangen in Millisekunden viele Neutronen ein und zerfallen dann β⁻ zu stabilen neutronenreichen Isotopen. Der Gravitationswellen-Event GW170817 (2017) lieferte starke Evidenz für Kilonova-Nukleosynthese: Sr, Ba und möglicherweise Au wurden spektroskopisch nachgewiesen.",
        difficulty = 3,
        funFact = "Die Neutronensternverschmelzung GW170817 (2017) produzierte in Sekundenbruchteilen mehr Gold als die Masse unserer Erde. Das gesamte Gold auf der Erde stammt aus solchen kosmischen Ereignissen, die vor der Sonnensystementstehung stattfanden."
    )

)
