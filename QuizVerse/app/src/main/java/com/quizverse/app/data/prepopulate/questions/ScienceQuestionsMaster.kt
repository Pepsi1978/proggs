package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun scienceQuestionsMaster(): List<Question> = listOf(

    // ── MASTER (difficulty = 5) ── 50 questions ──────────────────────────────

    // Question 1 – Quantum Field Theory: Renormalization
    Question(
        categoryId = 2,
        questionText = "Welche physikalische Bedeutung hat der Renormierungsgruppen-Fluss in der Quantenfeldtheorie?",
        answerA = "Er beschreibt die Divergenz der Wellenfunktion bei hohen Energien im UV-Grenzwert",
        answerB = "Er zeigt, wie sich Kopplungskonstanten mit der Energieskala (Renormierungsgruppe) ändern",
        answerC = "Er quantifiziert die spontane Symmetriebrechung im Higgs-Mechanismus",
        answerD = "Er berechnet die anomale magnetische Dipolstärke des Elektrons",
        correctAnswer = 1, // B
        explanation = "Der Renormierungsgruppen-Fluss beschreibt die skalenabhängige Entwicklung von Kopplungskonstanten. Die Beta-Funktion β(g) = μ dg/dμ gibt an, wie sich eine Kopplung g mit dem Energiemaßstab μ ändert – ein zentrales Konzept von Wilson und Gell-Mann/Low.",
        difficulty = 5,
        funFact = "Die asymptotische Freiheit der Quantenchromodynamik – Quarks sind bei sehr hohen Energien quasi frei – wurde durch den Renormierungsgruppen-Fluss erklärt und brachte Gross, Politzer und Wilczek 2004 den Nobelpreis."
    ),

    // Question 2 – Quantum Field Theory: Feynman Diagrams
    Question(
        categoryId = 2,
        questionText = "Was repräsentiert ein Feynman-Propagator G_F(x-y) in der skalaren φ⁴-Theorie mathematisch?",
        answerA = "Den Erwartungswert des zeitgeordneten Produkts zweier Feldoperatoren im Vakuum: ⟨0|T{φ(x)φ(y)}|0⟩",
        answerB = "Die S-Matrix-Amplitude für elastische Zweikörperstreuung bei festem Impulsübertrag",
        answerC = "Den normierten Grundzustand des Hamiltonoperators im Fock-Raum",
        answerD = "Die Vertexfunktion dritter Ordnung in der störungstheoretischen Entwicklung",
        correctAnswer = 0, // A
        explanation = "Der Feynman-Propagator ist definiert als G_F(x-y) = ⟨0|T{φ(x)φ(y)}|0⟩, das zeitgeordnete Vakuums-Zweipunktkorrelationsfunktional. In Impulsraumdarstellung lautet er 1/(p²-m²+iε) und beschreibt die Ausbreitung virtueller Teilchen zwischen zwei Raum-Zeit-Punkten.",
        difficulty = 5,
        funFact = null
    ),

    // Question 3 – Quantum Field Theory: Gauge Invariance
    Question(
        categoryId = 2,
        questionText = "Was besagt das Slavnov-Taylor-Identität in nicht-abelschen Eichtheorien?",
        answerA = "Die Masse der Eichbosonen ist durch die Casimir-Invariante der Eichgruppe festgelegt",
        answerB = "Sie ist die verallgemeinerte Ward-Identität, die BRST-Symmetrie-Erhaltung garantiert und die Konsistenz der störungstheoretischen Renormierung sichert",
        answerC = "Die Kopplung zwischen Geistfeldern und Eichfeldern ist immer perturbativ klein",
        answerD = "Im Farbfreiheitsraum der QCD sind nur Singlett-Zustände physikalisch beobachtbar",
        correctAnswer = 1, // B
        explanation = "Die Slavnov-Taylor-Identitäten verallgemeinern die Ward-Identitäten der QED auf nicht-abelsche Eichtheorien. Sie folgen aus der BRST-Symmetrie (Becchi-Rouet-Stora-Tyutin) und garantieren, dass die Renormierung die Eichinvarianz erhält und unphysikalische Geistfreiheitsgrade aus dem S-Matrix-Bild verschwinden.",
        difficulty = 5,
        funFact = "Die BRST-Symmetrie wurde 1974–1975 unabhängig von Carlo Becchi, Alain Rouet, Raymond Stora und Tyutin entdeckt und ist heute ein Eckpfeiler der modernen Eichfeldtheorie."
    ),

    // Question 4 – Quantum Field Theory: Vacuum Fluctuations
    Question(
        categoryId = 2,
        questionText = "Worauf beruht der Casimir-Effekt mikroskopisch?",
        answerA = "Auf elektromagnetischen Dipol-Dipol-Wechselwirkungen polarisierter Metalloberflächen bei kurzen Abständen",
        answerB = "Auf der Modifikation der Nullpunktsenergie des elektromagnetischen Vakuums durch Randbedingungen leitender Platten",
        answerC = "Auf dem quantenmechanischen Tunneln von Elektronen zwischen nah benachbarten Leitern",
        answerD = "Auf der Van-der-Waals-Kraft, die durch Austausch virtueller Phononen zwischen Metalloberflächen entsteht",
        correctAnswer = 1, // B
        explanation = "Der Casimir-Effekt entsteht, weil leitende Platten in engem Abstand die erlaubten Moden des elektromagnetischen Quantenvakuums einschränken. Außerhalb der Platten gibt es mehr Vakuumfluktuationen als dazwischen, was zu einer attraktiven Kraft führt. Hendrik Casimir sagte diese Kraft 1948 voraus; sie wurde 1997 von Lamoreaux präzise gemessen.",
        difficulty = 5,
        funFact = "Der Casimir-Effekt ist einer der wenigen direkt messbaren Beweise für Quantenvakuumfluktuationen und spielt eine Rolle bei der Entwicklung von MEMS-Nanogeräten."
    ),

    // Question 5 – Quantum Field Theory: Anomalies
    Question(
        categoryId = 2,
        questionText = "Was ist eine chirale Anomalie (Adler-Bell-Jackiw-Anomalie) in der Quantenfeldtheorie?",
        answerA = "Ein nicht-perturbativer Instanton-Effekt, der CP-Verletzung im QCD-Vakuum erzeugt",
        answerB = "Eine klassische Symmetrie (chirale Symmetrie), die auf quantenmechanischer Ebene durch Regularisierung gebrochen wird und nicht renormierbar ist",
        answerC = "Die Brechung der chiralen Symmetrie durch das Kondensat ⟨ψ̄ψ⟩ im chiralen Grenzwert der QCD",
        answerD = "Eine Instabilität masseloser Fermionen gegenüber Paarbildung im starken Magnetfeld",
        correctAnswer = 1, // B
        explanation = "Die ABJ-Anomalie (Adler-Bell-Jackiw, 1969) bezeichnet die Verletzung einer klassisch erhaltenen chiralen Symmetrie auf Quantenebene. Sie tritt auf, weil keine Regularisierungsmethode existiert, die gleichzeitig Eichinvarianz und chirale Symmetrie bewahrt. Die Konsistenzbedingung 'Anomalie-Cancellation' ist eine fundamentale Beschränkung für physikalische Feldtheorien.",
        difficulty = 5,
        funFact = null
    ),

    // Question 6 – Advanced Astrophysics: Penrose Process
    Question(
        categoryId = 2,
        questionText = "Welche notwendige Bedingung muss für den Penrose-Prozess an einem rotierenden Kerr-Schwarzen Loch erfüllt sein?",
        answerA = "Das einfallende Teilchen muss die Hawking-Temperatur überschreiten und den Ereignishorizont durchqueren",
        answerB = "Das Teilchen muss in der Ergosphäre in zwei Fragmente zerfallen, wobei ein Fragment negativer Energie in das Schwarze Loch fällt",
        answerC = "Supersynchrone Orbits müssen im Bereich des letzten stabilen Kreisorbits existieren",
        answerD = "Die Schwerpunktsenergie der Kollision muss die Planck-Energie überschreiten",
        correctAnswer = 1, // B
        explanation = "Im Penrose-Prozess (Roger Penrose, 1969) zerfällt ein Teilchen innerhalb der Ergosphäre eines Kerr-Schwarzen Lochs. Ein Fragment kann einen Zustand mit negativer Energie (bezüglich eines fernen Beobachters) im Ergosphere-Bereich einnehmen und in das Schwarze Loch fallen. Das andere Fragment entweicht mit mehr Energie als das ursprüngliche Teilchen hatte – entnommen aus der Rotationsenergie des Schwarzen Lochs.",
        difficulty = 5,
        funFact = "Theoretisch könnte bis zu 20,7 % der Ruhemasse eines maximal rotierenden Kerr-Schwarzen Lochs durch den Penrose-Prozess extrahiert werden."
    ),

    // Question 7 – Advanced Astrophysics: Hawking Radiation
    Question(
        categoryId = 2,
        questionText = "Aus welchem physikalischen Mechanismus ergibt sich die Hawking-Strahlung eines Schwarzen Lochs?",
        answerA = "Thermische Emission von Photonen aus der überhitzten Akkretionsscheibe nahe dem Ereignishorizont",
        answerB = "Quantentunneln von Teilchen durch den Ereignishorizont als Bogoliubov-Transformation im gekrümmten Raumzeit-Hintergrund",
        answerC = "Virtuelle Paarerzeugung nahe dem Horizont, bei der ein Teilchen negativ-energetisch in das Schwarze Loch fällt und das andere entweicht",
        answerD = "Spontane Emission von Gravitonen durch die Massenverteilung des kollabierenden Sterns",
        correctAnswer = 2, // C
        explanation = "Stephen Hawking (1974) zeigte, dass an einem Ereignishorizont virtuelle Teilchen-Antiteilchen-Paare entstehen. Wenn eines der Teilchen mit negativer Energie hinter dem Horizont 'gefangen' wird, kann das andere als reales Teilchen entweichen. Formal präziser: Im Bogoliubov-Formalismus mischt die gekrümmte Raumzeit Positiv- und Negativfrequenzmoden, was ein thermisches Spektrum T_H = ℏc³/(8πGMk_B) ergibt.",
        difficulty = 5,
        funFact = "Ein Schwarzes Loch mit einer Sonnenmasse hätte eine Hawking-Temperatur von nur ~60 Nanokelvin – weit kälter als die kosmische Hintergrundstrahlung, weshalb es in der Praxis keine Hawking-Strahlung abstrahlt."
    ),

    // Question 8 – Advanced Astrophysics: Neutron Star Equations of State
    Question(
        categoryId = 2,
        questionText = "Welches Konzept beschreibt die Tolman-Oppenheimer-Volkoff (TOV)-Gleichung in der Neutronensternenphysik?",
        answerA = "Das hydrostatische Gleichgewicht eines sphärisch symmetrischen Sterns unter Einbeziehung der allgemein-relativistischen Korrekturen",
        answerB = "Die Masse-Radius-Beziehung eines weißen Zwergs mit entartetem Elektronengas",
        answerC = "Den Impulsaustausch zwischen Neutronen und Pionen in der Neutronenstern-Kruste",
        answerD = "Die Phasenübergangsgleichung von hadronischer zu Quark-Materie bei ultranuklearer Dichte",
        correctAnswer = 0, // A
        explanation = "Die TOV-Gleichung (Tolman 1939, Oppenheimer-Volkoff 1939) ist die relativistische Verallgemeinerung der Newton'schen Gleichgewichtsbedingung für kompakte Sterne: dP/dr = -(ρ+P/c²)(m+4πr³P/c²)G / (r²(1-2Gm/rc²)). Sie verknüpft die Zustandsgleichung der Kernmaterie mit dem maximalen Neutronensternradius und der Chandrasekhar-Oppenheimer-Volkoff-Massengrenze (~3 M_☉).",
        difficulty = 5,
        funFact = "Das NICER-Röntgenteleskop auf der ISS misst präzise Neutronensterndurchmesser, um die Zustandsgleichung ultradichter Materie einzugrenzen."
    ),

    // Question 9 – Advanced Astrophysics: Gravitational Lensing
    Question(
        categoryId = 2,
        questionText = "Was versteht man unter 'Mikrogravitationslinsen' (Microlensing) und wodurch entstehen Einstein-Ringe?",
        answerA = "Lichtablenkung durch dunkle Materie erzeugt konzentrische Ringe in Galaxienhaufen, die durch das NFW-Profil beschrieben werden",
        answerB = "Vollständige Ausrichtung von Quelle, Linsenobjekt und Beobachter erzeugt eine ringförmige Abbildung mit Winkelradius θ_E = √(4GM d_LS/(c² d_L d_S))",
        answerC = "Gravitationslinsen verstärken das Licht entfernter Quasare durch relativistische Beaming-Effekte",
        answerD = "Refraktion elektromagnetischer Strahlung am Gravitationspotenzial eines Neutronensterns erzeugt eine Kaustik",
        correctAnswer = 1, // B
        explanation = "Ein Einstein-Ring entsteht bei perfekter Ausrichtung von Quelle, Gravitationslinse und Beobachter. Der Einstein-Radius θ_E ist ein fundamentaler Parameter, der von der Masse M des Linsenobjekts und den Winkelabstandsverhältnissen d_LS/d_L/d_S abhängt. Beim Microlensing variiert die Helligkeit eines Hintergrundsterns, wenn ein kompaktes Objekt (Stern, Planet) den Sichtstrahl kreuzt.",
        difficulty = 5,
        funFact = "Microlensing-Beobachtungen haben freischwebende Planeten (rogue planets) ohne Mutterstern entdeckt – und könnten zukünftig die primordialen Schwarzen Löcher als Kandidaten für dunkle Materie bestätigen oder widerlegen."
    ),

    // Question 10 – Advanced Astrophysics: Baryon Asymmetry
    Question(
        categoryId = 2,
        questionText = "Welche drei Bedingungen müssen laut Andrei Sacharow (1967) erfüllt sein, um die Baryon-Asymmetrie des Universums zu erklären?",
        answerA = "Inflationsphase, kosmische Phasenübergänge und Grand-Unified-Theory-Instantonen",
        answerB = "Baryonzahlverletzung, C- und CP-Verletzung sowie thermisches Nicht-Gleichgewicht",
        answerC = "Proton-Zerfall, Leptogenese über schwere Neutrinos und elektroschwache Sphaleron-Prozesse",
        answerD = "Symmetriebrechung des Higgsfeldes, Seesaw-Mechanismus und kosmische Strings",
        correctAnswer = 1, // B
        explanation = "Sacharows drei Bedingungen (1967) für Baryogenese: 1) Baryon-Zahlverletzung (sonst kein Nettoüberschuss möglich), 2) C- und CP-Verletzung (damit Materie und Antimaterie unterschiedlich behandelt werden), 3) thermisches Nicht-Gleichgewicht (damit Rückwärtsreaktionen den Überschuss nicht ausgleichen). Diese Bedingungen sind notwendig, aber ihre genaue Realisierung im Standardmodell oder BSM-Theorien ist noch offen.",
        difficulty = 5,
        funFact = null
    ),

    // Question 11 – Advanced Molecular Genetics: RNA Interference
    Question(
        categoryId = 2,
        questionText = "Welche molekulare Abfolge beschreibt den kanonischen RNAi-Weg nach Einschleusung langer doppelsträngiger RNA?",
        answerA = "dsRNA → Drosha-Spaltung → pre-miRNA Export → Dicer-Prozessierung → RISC-Beladung → mRNA-Spaltung",
        answerB = "dsRNA → Dicer-Prozessierung zu siRNA → RISC-Beladung mit Ago2 → Antisinn-Strang-Führung → komplementäre mRNA-Spaltung",
        answerC = "dsRNA → RdRP-Amplifikation → RITS-Komplex → H3K9-Methylierung → transkriptionelles Silencing",
        answerD = "dsRNA → Exportin-5-Transport → Argonauten-Beladung → Translationsrepression durch 3'-UTR-Bindung",
        correctAnswer = 1, // B
        explanation = "Im kanonischen RNAi-Weg (Fire & Mello, 1998; Nobelpreis 2006) schneidet das Enzym Dicer lange dsRNA zu ~21 bp siRNA-Duplexen. Der Antisinn-Strang wird von Argonaute 2 (Ago2) im RISC-Komplex (RNA-induced silencing complex) gebunden und leitet diesen zur komplementären Ziel-mRNA, die endonukleolytisch gespalten wird.",
        difficulty = 5,
        funFact = "RNAi wurde zuerst in C. elegans entdeckt, als die Einführung von Sense-RNA paradoxerweise stärker silencte als Antisinn-RNA allein – das führte zur Entdeckung des dsRNA-Mechanismus."
    ),

    // Question 12 – Advanced Molecular Genetics: Chromatin Remodeling
    Question(
        categoryId = 2,
        questionText = "Welche Enzymklasse katalysiert die ATP-abhängige Nukleosomen-Repositionierung und welches ist ihr charakteristisches mechanistisches Prinzip?",
        answerA = "Histondeacetylasen (HDACs); sie entfernen Acetylgruppen von Histon-Lysinresten und erhöhen die Chromatin-Kompaktierung",
        answerB = "SWI/SNF-Familie (BAF-Komplexe); sie nutzen ATP-Hydrolyse um transiente DNA-Schleifen zu bilden und Nukleosome entlang der DNA zu verschieben",
        answerC = "Polycomb-Repressor-Komplexe (PRC1/PRC2); sie mono-ubiquitinieren H2A und trimethylieren H3K27 zur Heterochromatin-Bildung",
        answerD = "Topoisomerasen Typ II; sie lösen topologische Spannungen durch temporäre DNA-Doppelstrangbrüche im Chromatin",
        correctAnswer = 1, // B
        explanation = "SWI/SNF-Komplexe (Switching defective/Sucrose non-fermenting) sind ATP-abhängige Chromatin-Remodeler. Der Mechanismus basiert auf der Bildung einer DNA-Schlaufe durch Translokation: Das ATPase-Modul treibt DNA in einer Schleife über das Nukleosom, was die Position des Histonoctamers relativ zur DNA-Sequenz verändert. Dies reguliert die Zugänglichkeit von Promotoren und Enhancern für Transkriptionsfaktoren.",
        difficulty = 5,
        funFact = "Mutationen in SWI/SNF-Untereinheiten (v.a. SMARCB1/SNF5) sind in ~20 % aller menschlichen Krebsarten gefunden worden – Chromatin-Remodeling ist somit ein wichtiger Tumor-Suppressor-Mechanismus."
    ),

    // Question 13 – Advanced Molecular Genetics: Non-coding RNA
    Question(
        categoryId = 2,
        questionText = "Welche funktionelle Rolle spielen lange nicht-kodierende RNAs (lncRNAs) bei der X-Chromosom-Inaktivierung?",
        answerA = "HOTAIR rekrutiert PRC2 zur Trimethylierung von H3K27 auf dem inaktiven X-Chromosom und initiiert die Xist-Expression",
        answerB = "Xist wird vom X-Inaktivierungszentrum transkribiert, breitet sich in cis aus, rekrutiert PRC2 und andere Silencing-Faktoren, und führt zur Heterochromatinisierung eines X-Chromosoms",
        answerC = "RepA-lncRNA aktiviert den Tsix-Antisinn-Locus auf dem aktiven X-Chromosom und verhindert so die Xist-Bindung an PRC2",
        answerD = "NEAT1 bildet Paraspeckle, die CTCF-Bindestellen am X-Inaktivierungszentrum sequenzieren und topologisch isolieren",
        correctAnswer = 1, // B
        explanation = "Xist (X-inactive specific transcript) ist eine ~17 kb lncRNA, die vom X-Inaktivierungszentrum (XIC) auf dem zu inaktivierenden X-Chromosom transkribiert wird. Xist 'bedeckt' das gesamte X in cis (coat), rekrutiert Polycomb-Repressor-Komplex PRC2 (H3K27me3), PRC1 (H2AK119ub1) und andere Faktoren und leitet so die transkriptionelle Stilllegung ein, die als Barr-Körper sichtbar wird.",
        difficulty = 5,
        funFact = null
    ),

    // Question 14 – Advanced Molecular Genetics: Genome Editing
    Question(
        categoryId = 2,
        questionText = "Welcher molekulare Mechanismus liegt dem PAM-unabhängigen CRISPR-Cas12a (Cpf1)-System zugrunde, und worin unterscheidet es sich von Cas9?",
        answerA = "Cas12a nutzt eine einzelne RuvC-Domäne und erzeugt 5'-überhängende Doppelstrangbrüche; Cas9 erzeugt stumpfe Enden durch zwei Nuklease-Domänen (RuvC + HNH)",
        answerB = "Cas12a ist eine RNA-gesteuerte DNase die keine tracrRNA benötigt, einen 5'-TTTN-PAM erkennt und versetzt-schneidende sticky ends erzeugt; Cas9 benötigt tracrRNA und erzeugt stumpfe Schnitte",
        answerC = "Cas12a katalysiert Single-Strand-Nicks (Einzelstrangbrüche) für hochpräzise Base-Editing, während Cas9 ausschließlich Doppelstrangbrüche einführt",
        answerD = "Cas12a erzeugt durch kollaterale trans-Cleavage-Aktivität unspezifische ssDNA-Degradation ohne guide-RNA-Bindung im Gegensatz zu Cas9",
        correctAnswer = 1, // B
        explanation = "CRISPR-Cas12a (Cpf1) unterscheidet sich von Cas9 in mehreren Aspekten: Es erkennt einen T-reichen PAM (5'-TTTN-3') statt des NGG-PAMs von Cas9; es prozessiert seine eigene crRNA und benötigt keine separate tracrRNA; es erzeugt 5'-Überhänge von ~5 nt (sticky ends) statt stumpfer Enden; und es besitzt eine kollaterale ssDNA-Transcleavage-Aktivität, die in Diagnostik (DETECTR) genutzt wird.",
        difficulty = 5,
        funFact = "Die kollaterale Cleavage-Aktivität von Cas12a wird im DETECTR-Assay für ultrasensitive Virusdiagnostik (z.B. SARS-CoV-2) genutzt – ein direktes Kind der CRISPR-Grundlagenforschung."
    ),

    // Question 15 – Advanced Molecular Genetics: Synthetic Biology
    Question(
        categoryId = 2,
        questionText = "Was versteht man unter einem 'Repressilator' in der synthetischen Biologie?",
        answerA = "Ein Genregulations-Netzwerk mit drei Repressoren im Rückkopplungskreis, das oszillierende Genexpression mit definierter Periodendauer erzeugt",
        answerB = "Ein synthetischer Transkriptionsfaktor aus ZnF-Domänen, der endogene Promotoren reprimiert",
        answerC = "Ein CRISPR-basiertes Gen-Silencing-System, das epigenetische Markierungen dauerhaft schreibt",
        answerD = "Ein bistabiler Schalter mit zwei Genen die sich gegenseitig reprimieren und zwei stabile Gleichgewichte besitzen",
        correctAnswer = 0, // A
        explanation = "Der Repressilator (Elowitz & Leibler, Nature 2000) ist eines der ersten synthetischen Genkreise. Er besteht aus drei Repressor-Protein-Genen (lacI, tetR, cI) im zyklischen negativen Rückkopplungskreis: Protein A reprimiert Gen B, Protein B reprimiert Gen C, Protein C reprimiert Gen A. Dies erzeugt stabile Oszillationen mit einer Periode von ~150 Minuten in E. coli und ist ein Paradebeispiel für 'bottom-up' Design biologischer Schaltkreise.",
        difficulty = 5,
        funFact = "Der Repressilator und der 'Toggle Switch' von Gardner et al. (beide 2000) gelten als Gründungsdokumente der synthetischen Biologie – zwei Artikel die zeigten, dass lebende Zellen wie elektronische Schaltkreise ingenieurmäßig programmiert werden können."
    ),

    // Question 16 – Theoretical Physics: String Theory
    Question(
        categoryId = 2,
        questionText = "Warum erfordert die konsistente Formulierung der bosonischen Stringtheorie genau 26 Raumzeitdimensionen?",
        answerA = "Die Weyl-Anomalie des zweidimensionalen konformen Feldes verschwindet nur bei zentraler Ladung c = 26, was 26 Raumdimensionen entspricht",
        answerB = "Die Modularinvarianz der Stringamplituden erfordert einen 26-dimensionalen Torus-Kompaktifizierungsrahmen für chiralen Stringausbreitung",
        answerC = "Die BRST-Kohomologie des ersten-quantisierten Strings ist nur bei D = 26 anomaliefrei bezüglich der Kac-Moody-Algebra",
        answerD = "Supergravitations-Hintergrundfelder lassen nur für D = 26 konsistente Calabi-Yau-Kompaktifizierungen zu",
        correctAnswer = 0, // A
        explanation = "In der bosonischen Stringtheorie beschreibt jede transversale Raumdimension ein freies konformes Feld mit zentraler Ladung c = 1. Die Geister der Diffeomorphismus-Invarianz tragen c = -26 bei. Die Gesamtanomalie (Weyl-Anomalie) verschwindet nur wenn c_Materie = 26, d.h. 26 freie bosonische Felder (= 26 Raumdimensionen). Bei der Superstringtheorie ist es D = 10 (Fermionen tragen je 1/2 bei).",
        difficulty = 5,
        funFact = null
    ),

    // Question 17 – Theoretical Physics: Supersymmetry
    Question(
        categoryId = 2,
        questionText = "Was sagt das Haag-Lopuszanski-Sohnius-Theorem über Supersymmetrie aus?",
        answerA = "Supersymmetrie ist die einzige Erweiterung der Poincaré-Algebra, die Bosonen und Fermionen in gemeinsamen Multiplets vereint, wenn man Antikommutatoren zulässt",
        answerB = "Das Massenspektrum supersymmetrischer Theorien ist identisch zu nicht-supersymmetrischen bei gleicher Kopplungsstärke",
        answerC = "In jeder konsistenten Supersymmetrie muss die kosmologische Konstante exakt null sein",
        answerD = "Supersymmetrische Teilchen können nur paarweise erzeugt und vernichtet werden, weshalb das leichteste SUSY-Teilchen stabil ist",
        correctAnswer = 0, // A
        explanation = "Das Haag-Lopuszanski-Sohnius-Theorem (1975) ist eine Erweiterung des Coleman-Mandula-Theorems. Es besagt, dass die einzige konsistente Erweiterung der Poincaré-Symmetriegruppe einer relativistischen Quantenfeldtheorie durch Superladungen Q_α möglich ist, die den Antikommutatorrelationen {Q_α, Q̄_β} = 2σ^μ_{αβ}P_μ gehorchen. Somit ist Supersymmetrie die einzige Möglichkeit, innere Symmetrien und Raumzeitsymmetrien nicht-trivial zu vereinen.",
        difficulty = 5,
        funFact = "Das Coleman-Mandula-Theorem (1967) bewies zuvor, dass Raumzeit-Symmetrien und innere Symmetrien in relativistischen QFTs nur trivial (als direktes Produkt) kombiniert werden können – außer man lässt Superladungen zu."
    ),

    // Question 18 – Theoretical Physics: AdS/CFT Correspondence
    Question(
        categoryId = 2,
        questionText = "Was postuliert die Maldacena-Dualität (AdS/CFT) in ihrer ursprünglichen Formulierung?",
        answerA = "Typ-IIB-Superstringtheorie auf AdS₅ × S⁵ ist dual zur N=4 superkonformen Yang-Mills-Theorie mit Eichgruppe SU(N) auf dem vierdimensionalen Rand",
        answerB = "Jede konformale Feldtheorie in d Dimensionen lässt sich durch eine Gravitationstheorie in d+1 Dimensionen mit flachem Minkowski-Hintergrund beschreiben",
        answerC = "Holographische Renormierungsgruppen-Flüsse entsprechen topologischen Quantenfeldtheorien auf dem Rand des Anti-de-Sitter-Raumes",
        answerD = "Die Bekenstein-Hawking-Entropie eines Schwarzen Lochs im Bulk ist gleich der freien Energie der Randtheorie im thermischen Gleichgewicht",
        correctAnswer = 0, // A
        explanation = "Juan Maldacenas Vermutung (1997) lautet: Typ-IIB-Superstringtheorie auf AdS₅ × S⁵ mit Krümmungsradius L ist exakt dual zu N=4, d=4 superkonformer Yang-Mills-Theorie (SYM) mit Eichgruppe SU(N). Der String-Kopplungsparameter g_s ist mit der 't Hooft-Kopplung λ = g²_YM·N über g_s = λ/N verknüpft. Diese Dualität verbindet Gravitation im Bulk mit einer Randquantenfeldtheorie ohne Gravitation.",
        difficulty = 5,
        funFact = "Die AdS/CFT-Korrespondenz ist das meistzitierte Paper der Hochenergiephysik und hat über 22.000 Zitierungen – obwohl sie bis heute mathematisch nicht bewiesen ist."
    ),

    // Question 19 – Theoretical Physics: Topological Insulators
    Question(
        categoryId = 2,
        questionText = "Was charakterisiert einen topologischen Isolator der Z₂-Klasse und durch welche Invariante wird er klassifiziert?",
        answerA = "Durch einen nichtverschwindenden Chern-Zahl-Invarianten n ∈ ℤ, der das Integer Quantum Hall-Plateau bei ν = n beschreibt",
        answerB = "Durch eine Z₂-topologische Invariante ν₀ ∈ {0,1}, die die Parität der Zeitumkehr-invarianten Impulspunkte und die Robustheit von Oberflächenzuständen beschreibt",
        answerC = "Durch den Winding-Number-Invarianten der Brillouin-Zone, der Quantisierung des Anomalous Hall-Effekts vorhersagt",
        answerD = "Durch die Bulk-Zustandsdichte im Band-Gap und den Landau-Quantisierungseffekt an der Oberfläche",
        correctAnswer = 1, // B
        explanation = "Topologische Z₂-Isolatoren (Kane & Mele 2005, Fu & Kane 2007) werden durch einen Z₂-Invarianten ν₀ ∈ {0,1} klassifiziert, der aus den Zeitumkehr-invarianten Impulspunkten (TRIM) der Brillouin-Zone berechnet wird. Bei ν₀ = 1 existieren topologisch geschützte Oberflächen-Zustände, die immun gegen Rückstreuung durch nicht-magnetische Störungen sind, da sie durch Kramers-Degenerierung geschützt werden.",
        difficulty = 5,
        funFact = "Bi₂Se₃ und Bi₂Te₃ sind experimentell bestätigte topologische Isolatoren – ihre Oberflächen leiten Strom ohne Dissipation, während das Innere isoliert."
    ),

    // Question 20 – Theoretical Physics: Quantum Gravity
    Question(
        categoryId = 2,
        questionText = "Welches zentrale Problem der Schleifenquantengravitation (Loop Quantum Gravity) bezeichnet man als das 'Problem der Zeit'?",
        answerA = "Die Nicht-Renormierbarkeit der perturbativen Quantengravitation führt zu unendlichen Korrekturen bei der Planck-Energie",
        answerB = "Die Wheeler-DeWitt-Gleichung Ĥ|Ψ⟩ = 0 enthält keine explizite Zeit-Variable, weshalb die Entstehung einer klassischen Zeit aus Quantengravitation unklar ist",
        answerC = "Spin-Netzwerk-Zustände der diskretisierten Raumzeit erlauben keine unitäre Zeitentwicklung im Sinne des Schrödinger-Bildes",
        answerD = "Die Lorentz-Invarianz ist in LQG auf der Planck-Skala explizit gebrochen, was zeitliche Kausalität verletzt",
        correctAnswer = 1, // B
        explanation = "Das 'Problem der Zeit' in der kanonischen Quantengravitation (LQG, Quantenkosmologie) ergibt sich aus der Diffeomorphismus-Invarianz: In der Hamiltonschen Formulierung führt die Diffeomorphismus-Invarianz auf die Wheeler-DeWitt-Gleichung Ĥ|Ψ⟩ = 0, in der keine externe Zeit erscheint. Der Wellenfunction des Universums Ψ[g_ij] ist eine funktionale der 3-Metrik ohne Zeitabhängigkeit. Wie die klassische Zeit emergiert, ist ein ungelöstes Kernproblem.",
        difficulty = 5,
        funFact = null
    ),

    // Question 21 – Advanced Organic Synthesis: Total Synthesis
    Question(
        categoryId = 2,
        questionText = "Welcher Schlüsselschritt in der Woodward-Synthese von Strychnin (1954) demonstrierte erstmals die Leistungsfähigkeit der mechanistischen Organik zur Totalsynthese komplexer Alkaloide?",
        answerA = "Eine intramolekulare Diels-Alder-Reaktion zur Bildung des Indolkerns mit gleichzeitiger Stereokonturierung des Isostrychnin",
        answerB = "Die stereoselektive Reduktion des Strychnin-N-Oxids mittels Al(OiPr)₃ zum Nachweis der relativen Konfiguration aller 7 Stereozentren",
        answerC = "Eine Robinson-Anellierung zur Bildung des carbocyclischen Sechsrings im Strychnan-Gerüst als stereodirigierende Einheit",
        answerD = "Die kupferkatalysierte asymmetrische Konjugat-Addition zur Etablierung der C12-Konfiguration im Alkylaminsystem",
        correctAnswer = 0, // A
        explanation = "Woodwards Strychnin-Synthese (1954, 17 Schritte) gilt als Meilenstein der Totalsynthese. Der entscheidende Schlüsselschritt war eine intramolekulare Diels-Alder-Cycloaddition zur Bildung des Isostrychnin-Intermediats und des tetracyclischen Isostrychnin-Grundgerüsts, was die sterische Komplexität des Strychnan-Systems mit 7 Stereozentren adressierte. Dies war eine der ersten bewussten Anwendungen der Diels-Alder-Reaktion in der Totalsynthese.",
        difficulty = 5,
        funFact = "Strychnin galt über 100 Jahre als unerreichbar für die chemische Synthese – Woodwards Erfolg bei nur 17 Schritten erschütterte diese Ansicht fundamental und begründete das moderne Denken in der Naturstoffsynthese."
    ),

    // Question 22 – Advanced Organic Synthesis: Retrosynthetic Analysis
    Question(
        categoryId = 2,
        questionText = "Was versteht man in der Corey'schen Retrosynthese unter einem 'Synthon' und einem 'synthetischen Äquivalent'?",
        answerA = "Ein Synthon ist eine gedachte Bruchstück-Einheit einer Zielstruktur im retrosynthetischen Rückschnitt; das synthetische Äquivalent ist das tatsächlich verwendbare Reagenz, das das Synthon im Labor darstellt",
        answerB = "Ein Synthon ist ein polyvalentes Linker-Molekül für Festphasensynthese; das synthetische Äquivalent ist die geschützte Aminosäure-Vorstufe",
        answerC = "Ein Synthon beschreibt die Summenformel der Targetmolekül-Untereinheit; das synthetische Äquivalent ist das entsprechende Schutzgruppen-Schema",
        answerD = "Ein Synthon entspricht dem reaktiven Intermediat der eigentlichen Reaktion; das synthetische Äquivalent ist der Mechanismus-kompatible Übergangszustand",
        correctAnswer = 0, // A
        explanation = "E.J. Corey (Nobelpreis 1990) entwickelte die retrosynthetische Analyse: Beim 'Rückschnitt' (disconnection) eines Bindung im Zielmolekül entstehen Synthons – idealisierte Fragmente, die oft als Carbokationen oder Carbanionen vorliegen. Da solche Spezies nicht direkt als Reagenzien existieren, werden synthetische Äquivalente eingesetzt: z.B. ist das Acyl-Kation-Synthon (CH₃CO⁺) durch Acetylchlorid oder Acetanhydrid realisiert.",
        difficulty = 5,
        funFact = null
    ),

    // Question 23 – Advanced Organic Synthesis: Asymmetric Catalysis
    Question(
        categoryId = 2,
        questionText = "Welcher Mechanismus liegt der Sharpless-Epoxidierung primärer und sekundärer Allylalkohole zugrunde?",
        answerA = "Ti(IV)-tartrat-Komplex koordiniert den Allylalkohol über den Sauerstoff und überträgt ein Oxidatom von TBHP in einem metallacyclischen Übergangszustand mit definierter Facial-Selektivität nach dem Katsuki-Sharpless-Modell",
        answerB = "Ruthenium-BINAP-Komplex katalysiert die enantioselektive Peroxid-Übertragung durch σ-Bindungsmetathese am allylischen C-H-Bindung",
        answerC = "Organo-Oxazaborolidin reagiert mit TBHP durch nucleophilen Angriff auf den Allylalkohol und erzeugt ein chirales Bor-Epoxid-Intermediat",
        answerD = "Mangan-Salen-Komplex (Jacobsen-Epoxidierung) überträgt Sauerstoff über einen Oxo-Mangan(V)-Komplex auf die Allylalkohol-Doppelbindung",
        correctAnswer = 0, // A
        explanation = "Die Sharpless-Epoxidierung (Katsuki & Sharpless, 1980) nutzt Ti(IV)-isopropoxid, (+)- oder (-)-Diethyltartrat (DET) und tert-Butylhydroperoxid (TBHP). Der Ti(IV)-tartrat-Komplex bildet einen C₂-symmetrischen Metallacyclus, in dem der Allylalkohol über seinen Hydroxyl-Sauerstoff koordiniert. Das Modell von Katsuki-Sharpless (Orientierung des Substrats im Komplex) sagt die Facial-Selektivität korrekt voraus: L-(+)-DET liefert β-Epoxid, D-(-)-DET liefert α-Epoxid.",
        difficulty = 5,
        funFact = "Die Sharpless-Epoxidierung war die erste praktische Methode zur enantioselektiven Epoxidierung – K. Barry Sharpless erhielt dafür den Nobelpreis 2001 (den zweiten Nobelpreis für Sharpless, nach 2022 für Click-Chemie)."
    ),

    // Question 24 – Advanced Organic Synthesis: Click Chemistry
    Question(
        categoryId = 2,
        questionText = "Was ist das mechanistische Grundprinzip der kupferkatalysierten Azid-Alkin-Cycloaddition (CuAAC) und warum ist sie regioselektiv?",
        answerA = "Cu(I) aktiviert das terminale Alkin durch π-Koordination, bildet ein Cu-Acetylid-Intermediat und reagiert mit dem organischen Azid schrittweise über einen metallocyclischen 6-Membered-Übergangszustand zum 1,4-disubstituierten Triazol",
        answerB = "Cu(II) oxidiert das Azid zum Nitriliimin, das in einer 1,3-dipolaren Cycloaddition ohne Regiokontrolle mit dem Alkin reagiert",
        answerC = "Kupfer koordiniert als Lewis-Säure das Stickstoffatom des Azids und lenkt durch frontale Orbital-Kontrolle die Regioselektivität zum 1,5-Isomer",
        answerD = "Die thermische Huisgen-Reaktion läuft ohne Kupfer regiospezifisch wegen elektronischer Differenz zwischen N1 und N3 des Azids ab",
        correctAnswer = 0, // A
        explanation = "Bei der CuAAC (Sharpless & Meldal, 2002) bildet Cu(I) zunächst ein Kupfer-Acetylid durch Deprotonierung. Dieses reagiert mit dem organischen Azid über einen dinuklearen Cu-Cu-Zwischenprodukt-Mechanismus (Worrell et al., 2013) in einem Cu-metallacyclischen 6-gliedrigen Übergangszustand, der dann das 1,4-disubstituierte 1,2,3-Triazol liefert. Die Regioselektivität (1,4 vs. 1,5) ist durch die Cu-Koordination erzwungen; die unkatalysierte Huisgen-Reaktion liefert 1:1 Gemische.",
        difficulty = 5,
        funFact = "Karl Barry Sharpless nannte 'Click Chemistry' 2001 als Konzept modularer, robuster Reaktionen – 2022 erhielt er dafür den Nobelpreis für Chemie."
    ),

    // Question 25 – Advanced Organic Synthesis: Organometallic Reactions
    Question(
        categoryId = 2,
        questionText = "Welcher elementare Schritt ist bei Palladium-katalysierten Kreuzkupplungsreaktionen (Suzuki, Heck, Negishi) geschwindigkeitsbestimmend und stereochemisch charakteristisch?",
        answerA = "Die oxidative Addition des Arylhalogenids an Pd(0) unter Bildung des trans-konfigurierten Pd(II)-Komplexes",
        answerB = "Die Transmetallierung vom Boronsäure- oder Organozink-Reagenz auf den Pd(II)-Komplex",
        answerC = "Die reduktive Eliminierung vom cis-konfigurierten Pd(II)-Komplex zum Kreuzprodukt unter Regeneration von Pd(0)",
        answerD = "Die Beta-Hydrid-Eliminierung von Pd(II) unter Bildung des Olefin-Pd(0)-Hydrid-Komplexes",
        correctAnswer = 0, // A
        explanation = "In der Pd-katalysierten Kreuzkupplung ist die oxidative Addition des Aryl- oder Vinylhalogenids an Pd(0) häufig der geschwindigkeitsbestimmende Schritt. Sie erfolgt konzertiert für Arylchloride über eine dreigliedrige Übergangszustand und liefert einen cis- oder trans-Pd(II)-Komplex, abhängig vom Halogenid-Typ. Elektronenreiche Pd-Komplexe (mit σ-Donor-Liganden) begünstigen die oxidative Addition, was als Leitsatz für Katalysator-Design gilt.",
        difficulty = 5,
        funFact = null
    ),

    // Question 26 – Systems Biology: Gene Regulatory Networks
    Question(
        categoryId = 2,
        questionText = "Was versteht man unter einem 'Feed-Forward Loop' (FFL) in Genregulationsnetzwerken, und welcher FFL-Typ zeigt 'Zeichenwechsel-Persistenz-Filterfunktion'?",
        answerA = "Der koherente Typ-1-FFL (C1-FFL), bei dem X Y und Z aktiviert und Y Z aktiviert, zeigt eine Verzögerungsantwort: Y muss persistente X-Signale filtern bevor Z angeschaltet wird",
        answerB = "Der inkoherente Typ-1-FFL (I1-FFL) generiert eine Pulsgeneratorfunktion: X aktiviert Z direkt und Z wird durch Y reprimiert nach einer Verzögerung",
        answerC = "Der bidirektionale FFL zwischen X-Y mit negativer Rückkopplung erzeugt stabile Oszillationen mit genau zwei harmonischen Frequenzen",
        answerD = "Der Typ-4-FFL mit doppelter negativer Rückkopplung hat bistabiles Verhalten und fungiert als epigenetischer Gedächtnisspeicher",
        correctAnswer = 0, // A
        explanation = "Im kohärenten Typ-1-FFL (häufigster FFL in E. coli und S. cerevisiae) aktiviert Transkriptionsfaktor X sowohl Y als auch Z, und Y aktiviert ebenfalls Z. Diese Architektur ist ein Zeichenwechsel-Persistenzfilter: Z wird nur aktiviert, wenn X-Signal persistiert (≥ Dauer t_on), weil Y-Aktivierung Zeit braucht. Flüchtige X-Signale werden herausgefiltert. Dies ist ein wichtiges Netzwerkmotiv-Prinzip, analysiert von Uri Alon.",
        difficulty = 5,
        funFact = "Uri Alons Arbeit zu Netzwerkmotiven (Science 2002) zeigte, dass E. coli nur ~13 verschiedene wiederkehrende Netzwerkmotiv-Typen nutzt – evolution scheint Schaltkreis-Muster zu bevorzugen die robuste und nützliche Berechnungen durchführen."
    ),

    // Question 27 – Systems Biology: Metabolic Flux Analysis
    Question(
        categoryId = 2,
        questionText = "Was misst ¹³C-metabolischer Flussanalyse (¹³C-MFA) und welches mathematische Problem muss dafür gelöst werden?",
        answerA = "¹³C-MFA misst absolute Metabolit-Konzentrationen durch isotopenmarkierte Substrat-Verdünnung und löst ein lineares Gleichungssystem aus Bilanzgleichungen",
        answerB = "¹³C-MFA quantifiziert intrazelluläre Reaktionsflüsse durch Analyse der ¹³C-Isotopomer-Verteilung in Metaboliten nach Wachstum auf ¹³C-Substrat, gelöst als nichtlineares Optimierungsproblem",
        answerC = "¹³C-MFA bestimmt Enzymkinetiken in vitro und verknüpft diese mit Michaelis-Menten-Parametern in einem metabolischen Netzwerkmodell",
        answerD = "¹³C-MFA verwendet Positionen-spezifische ¹³C-NMR-Spektroskopie um Proteinturnover in Biosynthesewegen zu messen",
        correctAnswer = 1, // B
        explanation = "In der ¹³C-MFA wachsen Zellen auf ¹³C-markierten Substraten (z.B. 1-¹³C-Glukose), und die Isotopomer-Verteilung in zentralen Metaboliten (Aminosäuren aus Hydrolysat) wird per GC-MS oder LC-MS gemessen. Die Verteilung hängt von den tatsächlichen Flüssen durch das Stoffwechselnetz ab. Durch nichtlineares least-squares-Fitting von simulierten zu gemessenen Isotopomer-Mustern werden die Flüsse v bestimmt – das Herz-Kreislauf-Analogon des Energiestoffwechsels.",
        difficulty = 5,
        funFact = null
    ),

    // Question 28 – Systems Biology: Systems Pharmacology
    Question(
        categoryId = 2,
        questionText = "Was beschreibt das Konzept der 'Netzwerkpharmakologie' und welchen Vorteil hat es gegenüber dem 'one-target, one-drug' Paradigma?",
        answerA = "Netzwerkpharmakologie nutzt Proteomik-Daten für Off-Target-Profiling und identifiziert toxische Nebenwirkungen durch ADME-Simulationen",
        answerB = "Netzwerkpharmakologie analysiert Wirkstoff-Protein-Interaktionsnetzwerke (drug-target networks) um Polypharmakologie, Synergien und Resistenzmechanismen im Systemkontext zu verstehen",
        answerC = "Netzwerkpharmakologie ist synonym mit Pharmakogenomik und nutzt SNP-Profilierung zur Vorhersage individueller Arzneimittelresponse",
        answerD = "Netzwerkpharmakologie quantifiziert pharmakokinetische Kompartimentmodelle mit PBPK-Simulationen für populationsbasiertes Dosing",
        correctAnswer = 1, // B
        explanation = "Netzwerkpharmakologie (Hopkins, Nature Chemical Biology 2008) erkennt, dass die meisten Erkrankungen durch gestörte Netzwerke entstehen, nicht durch einzelne Zielproteine. Durch Konstruktion von Wirkstoff-Ziel-Krankheits-Netzwerken können Polypharmakologie (ein Wirkstoff trifft mehrere Ziele), synergistische Wirkstoffe und Resistenz-Mechanismen (z.B. Bypass-Signalwege) systematisch analysiert werden. Dies übertrifft das klassische 'magic bullet'-Paradigma besonders bei Krebs und Alzheimer.",
        difficulty = 5,
        funFact = "Die Idee der 'dirty drugs', die mehrere Ziele treffen, wurde lange als unerwünscht abgelehnt – Netzwerkpharmakologie rehabilitierte dieses Konzept wissenschaftlich."
    ),

    // Question 29 – Systems Biology: Multi-Omics Integration
    Question(
        categoryId = 2,
        questionText = "Was ist die methodische Herausforderung bei der Integration von Genomik, Transkriptomik, Proteomik und Metabolomik (Multi-Omics)?",
        answerA = "Die Hauptherausforderung ist die Probenentnahme: alle Omics-Messungen müssen aus identischen Zellen innerhalb von Millisekunden erfolgen",
        answerB = "Multi-Omics leidet an Dimensionalitäts-Ungleichgewicht (10⁶ Gene vs. 10³ Metabolite), unterschiedlichen Skalierungen, fehlenden Daten und technischen Batch-Effekten; Integration erfordert latente Faktor-Modelle oder Graphen-basierte Methoden",
        answerC = "Die größte Hürde ist die fehlende Standardisierung von MS-Spektren zwischen Proteomik und Metabolomik und die Unmöglichkeit absoluter Quantifizierung",
        answerD = "Epigenomik und Transkriptomik können nicht integriert werden, da Methylierungsdaten und RNA-seq-Daten in unterschiedlichen Zellstadien gemessen werden müssen",
        correctAnswer = 1, // B
        explanation = "Multi-Omics-Integration ist mathematisch anspruchsvoll wegen: 1) Massivem Dimensionalitätsunterschied (10⁵–10⁶ genomische Features vs. 10²–10³ Metabolite), 2) unterschiedlicher technischer Variabilität und Batch-Effekten je Omics-Plattform, 3) nicht-normalverteilter Datenstrukturen, 4) fehlenden Messwerten (missing data). Lösungsansätze: MOFA (Multi-Omics Factor Analysis), DIABLO (mixOmics), Graphen-neuronale Netze für Pathway-Integration.",
        difficulty = 5,
        funFact = null
    ),

    // Question 30 – Systems Biology: Synthetic Gene Circuits
    Question(
        categoryId = 2,
        questionText = "Welche mathematische Eigenschaft definiert ein bistabiles Genregulationnetzwerk und wie kann man Bistabilität aus der Zustandsraumanalyse erkennen?",
        answerA = "Bistabilität liegt vor, wenn die Jacobi-Matrix des dynamischen Systems zwei rein reelle negative Eigenwerte hat, was auf Dämpfung ohne Oszillation hinweist",
        answerB = "Bistabilität bedeutet zwei stabile Fixpunkte (Knoten oder Spiralen) und mindestens einen instabilen Sattelpunkt, die durch eine S-förmige Nullklinen-Schnittmenge im Phasenporträt erkennbar sind",
        answerC = "Bistabilität ist nur in Netzwerken mit gerader Anzahl positiver Rückkopplungsschleifen möglich und zeigt sich durch Grenzzyklus-Oszillationen im Phasenraum",
        answerD = "Ein bistabiles System hat genau zwei komplexe Eigenwertpaare mit positivem Realteil, was zu zwei unabhängigen Gleichgewichtspunkten führt",
        correctAnswer = 1, // B
        explanation = "Bistabilität in Genregulationsnetzwerken tritt auf, wenn das ODE-System zwei stabile Fixpunkte (attraktive Gleichgewichte) durch ein Sattelgebiet getrennt besitzt. Im Phasenporträt erkennt man dies an S-förmigen Nullklinenschnitten mit drei Schnittpunkten, wobei der mittlere instabil ist. Biologisch realisiert durch positive Rückkopplung (z.B. Toggle Switch von Gardner et al.: lacI reprimiert tetR, tetR reprimiert lacI). Bistabilität ermöglicht zelluläres 'Gedächtnis'.",
        difficulty = 5,
        funFact = "Bistabile Genkreise sind die molekulare Grundlage von Zelldifferenzierung: Stammzellen wählen zwischen zwei stabilen Attraktorzuständen im Genexpressionsraum."
    ),

    // Question 31 – Advanced Geoscience: Mantle Plume Dynamics
    Question(
        categoryId = 2,
        questionText = "Welche seismische Signatur unterscheidet einen 'aktiven' Mantelplume vom passiven Riftingkonzept für den Ursprung des Yellowstone-Hotspots?",
        answerA = "Aktive Plumes zeigen eine positive seismische Anomalie (schneller P-Wellen-Geschwindigkeit) unter dem Hotspot und eine Thinning-Signatur der Lithosphäre",
        answerB = "Aktive Plumes zeigen eine niedrige Seismizität unter dem Vulkanfeld; Passivmodelle sagen seismische Wellen mit normaler Mantelgeschwindigkeit vorher",
        answerC = "Seismische Tomographie zeigt beim aktiven Plume-Modell eine tiefreichende (>400 km) thermische Tiefgeschwindigkeits-Anomalie (low-velocity zone) im oberen und unteren Mantel unter dem Hotspot",
        answerD = "Aktive Plumes erzeugen einen charakteristischen Seismizitätsgürtel entlang der Plattengrenze, der in passiven Riftmodellen fehlt",
        correctAnswer = 2, // C
        explanation = "Die seismische Tomographie zeigt beim Yellowstone-Hotspot eine ausgeprägte Tiefgeschwindigkeits-Anomalie (niedriger V_p und V_s durch erhöhte Temperatur/partielle Schmelze), die bis in den Übergangszone des Mantels (~660 km) reicht. Aktive Plume-Modelle (Morgan 1971) verlangen eine Quelle im tiefen Mantel (>1000 km), während Passivmodelle (Foulger et al.) diese Tiefenwurzelung bestreiten. Die aktuelle Tomographie zeigt eine ~800 km tiefe Anomalie, was die Debatte weiterhin offen hält.",
        difficulty = 5,
        funFact = "Der Yellowstone-Supervolkan hat in den letzten 2,1 Millionen Jahren drei Mega-Eruptionen produziert – die letzte vor 640.000 Jahren legte eine 2.500 km³ Asche-Decke über Nordamerika."
    ),

    // Question 32 – Advanced Geoscience: Snowball Earth
    Question(
        categoryId = 2,
        questionText = "Welche geochemische Evidenz stützt die 'Snowball Earth'-Hypothese für die Neoproterozoische Epoche (~700 Ma)?",
        answerA = "Paläomagnetische Daten zeigen Glazialablagerungen in äquatorialen Breiten; Banded Iron Formations (BIFs) nach Interglazial deuten auf anoxischen Ozean unter dem Eis",
        answerB = "Stabile Sauerstoffisotopen-Anomalien (δ¹⁸O) in Karbonatgesteinen zeigen eine abrupte positive Exkursion, die globale Vereisung anzeigt",
        answerC = "Zirkon-U-Pb-Datierungen belegen weltweite gleichzeitige Tillite mit identischem Alter von 716 ± 0,4 Ma in allen Kontinenten",
        answerD = "Sedimentäre Ablagerungen aus dem Kryogenium zeigen Dropstones und Eisrafft-Strukturen ausschließlich in polaren Breiten",
        correctAnswer = 0, // A
        explanation = "Die Snowball Earth-Hypothese (Kirschvink 1992, Hoffman et al. 1998) stützt sich auf: 1) Paläomagnetische Daten zeigen glaziale Diamiktite (Tillite) in äquatorialen Breiten (~10° paläogeogr. Breite), was globale Vereisung impliziert; 2) Banded Iron Formations (BIFs) direkt über den Tilliten zeigen, dass der Ozean unter dem Eis anoxisch wurde (Eisenansammlung ohne Oxidation), gefolgt von abrupter Oxidation nach Schmelze; 3) Cap-Karbonate über den Tilliten zeigen ein CO₂-Treibhausreaktion der Schmelzphase an.",
        difficulty = 5,
        funFact = "Die Snowball Earth-Episoden könnten die Voraussetzung für die Kambrische Explosion gewesen sein, da der erhöhte Phosphat-Eintrag durch Verwitterung nach der Eisschmelze das Marine-Leben beflügelte."
    ),

    // Question 33 – Advanced Geoscience: Isotope Geochemistry
    Question(
        categoryId = 2,
        questionText = "Was gibt der εNd-Wert (Epsilon-Neodymium) in der Isotopen-Geochemie an und wie wird er berechnet?",
        answerA = "εNd = [(¹⁴³Nd/¹⁴⁴Nd)_Probe / (¹⁴³Nd/¹⁴⁴Nd)_CHUR - 1] × 10⁴; er beschreibt die Abweichung vom chondritischen uniform reservoir und gibt Auskunft über die Sm/Nd-Fraktionierung und das Alter der Krustenbildung",
        answerB = "εNd = [(¹⁴⁴Nd/¹⁴³Nd)_Probe - (¹⁴⁴Nd/¹⁴³Nd)_MORB] × 10⁶ und klassifiziert Nd-Reservoirs nach mantelischer vs. krustaler Herkunft",
        answerC = "εNd ist die normierte Abweichung des ¹⁴⁷Sm/¹⁴⁴Nd-Verhältnisses der Probe vom Bulk Silicate Earth Standard BSE",
        answerD = "εNd wird aus der Steigung der Nd-Isochrone im ¹⁴³Nd/¹⁴⁴Nd-vs-¹⁴⁷Sm/¹⁴⁴Nd-Diagramm bestimmt und ist äquivalent zum T_DM-Mantel-Modellalter",
        correctAnswer = 0, // A
        explanation = "εNd = [(¹⁴³Nd/¹⁴⁴Nd)_Probe/(¹⁴³Nd/¹⁴⁴Nd)_CHUR - 1] × 10⁴. CHUR (Chondritic Uniform Reservoir) ist das heutige ¹⁴³Nd/¹⁴⁴Nd-Verhältnis von Chondriten = 0,512638. Positive εNd-Werte (depleted mantle, MORB: +8 bis +12) zeigen Sm/Nd-Anreicherung durch partielle Schmelze an; negative Werte (alte kontinentale Kruste: -20 bis -50) zeigen Nd-Anreicherung gegenüber Sm durch Krustenbildung an.",
        difficulty = 5,
        funFact = null
    ),

    // Question 34 – Advanced Geoscience: Astrochemistry
    Question(
        categoryId = 2,
        questionText = "Welche Reaktion beschreibt die Bildung von Aminosäuren in interstellaren Molekülwolken und warum ist sie astrophysikalisch relevant?",
        answerA = "Strecker-Synthese auf Eiskörnern: HCN + H₂CO + NH₃ auf Eis-Oberflächen bei ~10 K, katalysiert durch UV-Strahlung, liefert Aminosäure-Vorläufer die in Meteoriten gefunden wurden",
        answerB = "Radikal-Rekombination von CO und NH₂ in der Gasphase einer Protoplaneten-Scheibe bei 200–300 K ergibt Glycin direkt ohne katalytische Oberfläche",
        answerC = "Proton-Übertragungsreaktion (PTR) zwischen interstellarem Methylamin und CO₂-Eis auf polycyclischen aromatischen Kohlenwasserstoffen (PAH-Eis)",
        answerD = "Photochemische Isomerisierung von interstellarem Formaldehyd (H₂CO) zu Ethanolamin durch Lyman-Alpha-Strahlung junger Sterne",
        correctAnswer = 0, // A
        explanation = "In interstellaren Eiskörnern (H₂O-Eis, CO-Eis, NH₃-Eis bei ~10 K) finden Straecker-ähnliche Reaktionen durch UV- und kosmische Strahlung statt: HCN + H₂CO + NH₃ → α-Amino-Nitril → Aminosäuren. Tatsächlich wurden >80 Aminosäuren im Murchison-Meteoriten nachgewiesen. Die Bernstein-Gruppe (NASA Ames) hat Glycin und Alanin-Vorläufer aus bestrahlten Eismischungen synthetisiert. Dies deutet auf extraterrestrischen Ursprung der präbiotischen Moleküle.",
        difficulty = 5,
        funFact = "Im Jahr 2009 wurde Glyzin erstmals in einem Kometen (Wild 2) nachgewiesen, gesammelt von der Stardust-Mission – ein Meilenstein für die Panspermie-Hypothese."
    ),

    // Question 35 – Advanced Geoscience: Paleoclimate Proxies
    Question(
        categoryId = 2,
        questionText = "Was misst das δ¹⁸O-Signal in benthischen Foraminiferen-Schalen aus Tiefseesedimenten und wie trennt man die zwei Haupt-Signalkomponenten?",
        answerA = "δ¹⁸O misst ausschließlich die Meerwasser-Temperatur beim Schalenwachstum; Eisvolumeneffekte werden durch ¹⁰Be-Konzentrationen in Eiskernen korrigiert",
        answerB = "δ¹⁸O in benthischen Foraminiferen enthält ein gemischtes Signal aus Meerwassertemperatur und globaler Eisvolumenveränderung (sea-level/ice effect); Trennung erfolgt durch Kombination mit Mg/Ca-Paläothermometrie",
        answerC = "δ¹⁸O misst nur das globale Eisvolumen; die Temperatursignatur wird durch die Clumped-Isotopen-Thermometrie (Δ47) separat bestimmt",
        answerD = "Benthische und planktonische δ¹⁸O-Signale sind komplementär: benthisch misst Tiefenwassertemperatur, planktonisch misst ausschließlich Eisvolumen",
        correctAnswer = 1, // B
        explanation = "Das δ¹⁸O-Signal in benthischen Foraminiferen (Emiliani 1955, Shackleton 1967) ist ein Mischsignal: 1) Temperatursignal: kälteres Tiefenwasser bewirkt höheres δ¹⁸O in der Schale (etwa -0,25‰/°C); 2) Eisvolumensignal: große Eisschilde entziehen ¹⁶O dem Ozean, heben das δ¹⁸O des Meerwassers um ~+1,0‰ pro 100m sea-level-Abfall. Mg/Ca-Paläothermometrie an denselben Schalen erlaubt Temperatur-unabhängige Bestimmung und damit Entkopplung beider Signale.",
        difficulty = 5,
        funFact = null
    ),

    // Question 36 – Biophysics: Protein Crystallography
    Question(
        categoryId = 2,
        questionText = "Was ist das 'Phase Problem' in der Proteinkristallographie und welche Hauptlösungsmethoden existieren?",
        answerA = "Das Phase Problem entsteht, weil Röntgendetektoren nur Intensitäten |F_hkl|² messen, nicht die Phasen φ_hkl; gelöst durch Multiple Isomorphe Replacement (MIR), Single-wavelength Anomalous Diffraction (SAD/MAD) oder Molecular Replacement",
        answerB = "Das Phase Problem beschreibt die Unmöglichkeit, Atompositionen direkt aus dem Beugungsmuster zu berechnen; gelöst durch Cryo-EM als komplementäre Methode die keine Kristallisation benötigt",
        answerC = "Das Phase Problem bezeichnet die Überlappung von Bragg-Peaks bei niedrig-symmetrischen Kristallen, was die Fourier-Transformation ambig macht",
        answerD = "Das Phase Problem entsteht durch Strahleninduzierte Strahlenschäden die die kristallographischen Phasenwerte verfälschen und durch low-dose Datensammlung gelöst wird",
        correctAnswer = 0, // A
        explanation = "Das Phasenproblem (crystallographic phase problem) ist fundamental: Die Fourier-Transformierte der Elektronendichte ρ(r) ist der Strukturfaktor F_hkl = |F_hkl|·exp(iφ_hkl). Detektoren messen nur |F_hkl|² (Intensitäten), nicht die Phase φ. Lösungswege: 1) MIR: schwere Atome verändern Intensitäten; Differenz liefert Schweratom-Positionen → Phasen; 2) SAD/MAD: anomale Streuung nahe Absorptionskante; 3) Molecular Replacement: bekannte Homologstruktur liefert Startphasen.",
        difficulty = 5,
        funFact = "Max Perutz löste 1959 die erste Proteinstruktur (Hämoglobin) mit MIR – ein Durchbruch der 22 Jahre Arbeit erforderte. Er erhielt 1962 den Nobelpreis."
    ),

    // Question 37 – Biophysics: Cryo-EM
    Question(
        categoryId = 2,
        questionText = "Welches physikalische Prinzip ermöglicht die Auflösungsrevolution in der Kryo-Elektronenmikroskopie (resolution revolution) seit ~2013?",
        answerA = "Verbesserte Elektronenquellen (Field-Emission-Guns) mit kohärenteren Elektronenstrahlen erlauben höhere Numerische Apertur und damit mehr Fourier-Terme",
        answerB = "Direkte Elektronendetektoren (DED) mit quantitativ korrekter Einzelelektron-Zählung, überlegener Detective Quantum Efficiency (DQE) und schneller Filmaufnahme erlauben Bewegungskorrektur und deutlich verbessertes SNR",
        answerC = "Verbesserte Proben-Vitrifikation durch Schnell-Cryo-Plunger eliminiert Eis-Artefakte und erlaubt dickere Proben mit weniger Strahlenschäden",
        answerD = "Phase-Platten (Volta Phase Plate) steigern den Phasenkontrast bei niedrigen Raumfrequenzen und verbessern dadurch die Auflösung in kritischen Bereichen",
        correctAnswer = 1, // B
        explanation = "Die 'Resolution Revolution' (Henderson, Bhaskara Rao; ~2013) wurde primär durch direkte Elektronendetektoren (DED) ermöglicht: Diese zählen einzelne Elektronen mit hoher DQE (>80% bei niedrigen Frequenzen vs. ~20% für Film/CCD). Durch schnelle Aufnahme einzelner Frames kann Strahlungsinduzierte Probenbewegung (Beam-induced motion) nachträglich korrigiert werden. Dies steigerte erreichbare Auflösungen von >4 Å auf unter 2 Å für viele Proben.",
        difficulty = 5,
        funFact = "Henderson, Dubochet und Frank erhielten 2017 den Nobelpreis für Chemie für die Entwicklung der Kryo-EM – die Methode hat seitdem die Strukturbiologie revolutioniert und erlaubt Strukturen die nie kristallisiert werden konnten."
    ),

    // Question 38 – Biophysics: Single-Molecule Techniques
    Question(
        categoryId = 2,
        questionText = "Was messen Einzelmolekül-FRET (smFRET)-Experimente und welche Forster-Gleichung beschreibt den Energietransfer?",
        answerA = "smFRET misst Abstände zwischen 2–10 nm durch Energietransfer E = 1/(1+(R/R₀)⁶) wo R₀ der Förster-Radius (typisch 4–7 nm) ist, bei dem E = 50%; ermöglicht Konformationsdynamik in Echtzeit",
        answerB = "smFRET misst Diffusionskoeffizienten einzelner fluoreszierender Moleküle in der Zellmembran durch Photobleich-Raten-Analyse",
        answerC = "smFRET quantifiziert die Quantum-Yield einzelner Fluorophore durch Vergleich von Absorptions- und Emissionsintensität bei definierter Anregungs-Geometrie",
        answerD = "smFRET misst zeitaufgelöste Fluoreszenz-Anisotropie um Rotationsdiffusion und Liganden-Bindungs-Affinitäten zu bestimmen",
        correctAnswer = 0, // A
        explanation = "Förster-Resonanzenergietransfer (FRET) basiert auf Dipol-Dipol-Wechselwirkung zwischen Donor- und Akzeptor-Fluorophor. Die Effizienz E = 1/(1+(R/R₀)⁶) hängt stark vom Abstand R ab, was FRET zum molekularen Lineal für 2–10 nm macht. Bei smFRET (single-molecule FRET, Weiss 1999) werden einzelne Moleküle immobilisiert oder in diffundierenden Bursts analysiert. Konformationszustände (z.B. RNA-Faltung, Protein-Dynamik) sind durch bimodale FRET-Histogramme erkennbar.",
        difficulty = 5,
        funFact = null
    ),

    // Question 39 – Biophysics: Membrane Biophysics
    Question(
        categoryId = 2,
        questionText = "Was ist der Gibbs-Duhem-Zusammenhang in der Membranmechanik und wie beschreibt das Helfrich-Modell die Krümmungsenergie biologischer Membranen?",
        answerA = "Das Helfrich-Modell definiert die Biegeelastiziät der Membran als F_curv = ∫(κ/2·(H-H₀)² + κ_G·K)dA, wobei κ die Biegesteifigkeit (~20 k_BT für DPPC), H die mittlere Krümmung und K die Gauß-Krümmung ist",
        answerB = "Das Helfrich-Modell beschreibt die Membranspannung als σ = κ/R² und quantifiziert die laterale Druckverteilung in einer planaren Lipiddoppelschicht",
        answerC = "Die Helfrich-Welligkeit (Helfrich undulations) beschreibt thermische Fluktuationen als harmonische Oszillatoren mit F ~ k_BT·ln(A/A₀) für Membranflächen über der Gleichgewichtsfläche",
        answerD = "Das Gibbs-Duhem-Modell gibt die osmotische Kraft zwischen zwei Membranen in wässriger Lösung durch die Disjoining-Pressure-Kurve als Funktion des Schichtabstands an",
        correctAnswer = 0, // A
        explanation = "Wolfgang Helfrichs Modell (1973) beschreibt die freie Biegeenergie einer elastischen Membran als F = ∫[(κ/2)(H-H₀)² + κ_G·K]dA, wobei κ die Biegesteifigkeit (Bending Modulus, ~10–50 k_BT typisch), H₀ die spontane Krümmung, H = (c₁+c₂)/2 die mittlere Krümmung und K = c₁·c₂ die Gauß-Krümmung ist. Dies ist die fundamentale Gleichung der weichen Materie-Biophysik für Vesikelformierung, Membrankrümmung durch Proteinen, und Lipid-Nanodomänen.",
        difficulty = 5,
        funFact = "Die spontane Krümmung H₀ ≠ 0 ist verantwortlich für asymmetrische Membranen – z.B. erzeugt das innere Blatt der roten Blutzellen-Membran die charakteristische Discocyt-Form."
    ),

    // Question 40 – Biophysics: Ion Channel Electrophysiology
    Question(
        categoryId = 2,
        questionText = "Was beschreibt der Goldman-Hodgkin-Katz (GHK)-Strom-Gleichung und welche Vereinfachung liegt ihr zugrunde?",
        answerA = "Die GHK-Gleichung setzt ein konstantes Feld (constant field assumption) voraus: dV/dx = konst. über die Membrandicke; sie gibt den Ionenstrom für Ionen i durch die Membran als Funktion von Permeabilität P_i, Konzentration und Membranpotential an",
        answerB = "Die GHK-Gleichung ist eine Nernst-Planck-Erweiterung die active transport einschließt und Maxwell-Boltzmann-Statistik für Ionen in einem elektrischen Feld anwendet",
        answerC = "Die GHK-Gleichung gilt nur für ideale Elektrolyte mit unendlich verdünnten Lösungen und versagt bei physiologischen Ionenkonzentrationen über 100 mM",
        answerD = "Die GHK-Gleichung beschreibt die Spannungsabhängigkeit der Ionenkanal-Leitfähigkeit durch Boltzmann-Verteilung der Gating-Ladung im elektrischen Feld",
        correctAnswer = 0, // A
        explanation = "Die GHK-Strom-Gleichung (Goldman 1943, Hodgkin & Katz 1949) beschreibt den elektrischen Strom I_j eines monovalenten Ions j: I_j = P_j·z²_j·F²V/RT · (c_j^in - c_j^out·exp(-zFV/RT))/(1 - exp(-zFV/RT)). Die Schlüsselannahme ist das 'constant field': das elektrische Feld ist uniform über die Membrandicke, also dV/dx = V_m/d. Die Lösungsionenflüsse sind voneinander unabhängig (kein Ionenkanal-Interaktion). Dies ergibt die GHK-Spannungsgleichung für das Ruhemembranpotential.",
        difficulty = 5,
        funFact = null
    ),

    // Question 41 – Nuclear Physics: Shell Model
    Question(
        categoryId = 2,
        questionText = "Welche Wechselwirkung erzeugt im Atomkern-Schalenmodell die 'magischen Zahlen' 28, 50, 82, 126?",
        answerA = "Die Coulomb-Wechselwirkung zwischen Protonen spaltet Energieniveaus und erzeugt Schalen-Lücken bei den magischen Zahlen",
        answerB = "Die starke Spin-Bahn-Kopplung (ℓ·s-Term) im Schalenmodell-Potential (Mayer-Jensen, 1949) erzeugt große Energielücken die die magischen Zahlen 28, 50, 82, 126 erklären",
        answerC = "Das Isospin-Tensor-Potential der Nukleon-Nukleon-Wechselwirkung erzeugt durch erhöhte Bindungsenergie die Schalen-Lücken",
        answerD = "Pairing-Korrelationen (BCS-ähnliche Seniority-Kopplung) in der Valenzschale erzeugen erhöhte Bindungsenergie bei geraden Proton/Neutronzahlen die den magischen Zahlen entsprechen",
        correctAnswer = 1, // B
        explanation = "Das Schalenmodell mit harmonischem Oszillator- oder Woods-Saxon-Potential erklärt die Zahlen 2, 8, 20 korrekt, aber nicht 28, 50, 82, 126. Erst der starke Spin-Bahn-Term H_sl = -ξ(r)·l·s (Goeppert Mayer und Jensen, Nobelpreis 1963) spaltet jede Schale ℓ in zwei Sub-Niveaus mit j = ℓ+1/2 und j = ℓ-1/2 mit großer Energiedifferenz. Dies verschiebt bestimmte Niveaus über die klassische Schalen-Grenze, was die experimentell beobachteten Schalen-Abschlüsse erklärt.",
        difficulty = 5,
        funFact = "Maria Goeppert Mayer und J. Hans D. Jensen erhielten 1963 den Nobelpreis für Physik für das Schalenmodell – Goeppert Mayer war erst die zweite Frau nach Marie Curie, die den Physik-Nobelpreis erhielt."
    ),

    // Question 42 – Nuclear Physics: Quark-Gluon Plasma
    Question(
        categoryId = 2,
        questionText = "Was ist ein Quark-Gluon-Plasma (QGP) und bei welchem Parameter quantifiziert man den Phasenübergang von hadronischer Materie zum QGP?",
        answerA = "QGP ist ein Zustand extrem heißer/dichter Materie (T > 150–170 MeV, entspricht ~2×10¹² K) in dem Quarks und Gluonen deconfiniert sind; der Phasenübergang wird durch den Polyakov-Loop als Ordnungsparameter beschrieben",
        answerB = "QGP entsteht ausschließlich bei sehr hoher Baryonendichte (µ_B > 1 GeV) bei niedrigen Temperaturen und ist ein Suprafluid aus Cooper-Quark-Paaren",
        answerC = "QGP ist definiert durch das Verschwinden der chiralen Symmetriebrechung bei T_c und wird durch das Chiral Condensate ⟨ψ̄ψ⟩ als einzigem Ordnungsparameter quantifiziert",
        answerD = "QGP entsteht durch Überschreitung der Hagedorn-Temperatur T_H ~ 165 MeV ab der die Zustandssumme des Hadron-Resonanzgases divergiert und keine neuen hadronischen Zustände mehr möglich sind",
        correctAnswer = 0, // A
        explanation = "Das Quark-Gluon-Plasma (QGP) ist ein Zustand stark wechselwirkender Materie bei T ≳ 155 MeV (Gitterquantenchromodynamik: T_c ≈ 155±9 MeV), in dem individuelle Hadronen sich auflösen und Quarks/Gluonen über makroskopische Distanzen frei sind (Deconfinement). Der Polyakov-Loop ⟨L⟩ ist der thermodynamische Ordnungsparameter für Deconfinement: ⟨L⟩ = 0 in der konfinierten Phase, ⟨L⟩ ≠ 0 im QGP. Das QGP wird am LHC (ALICE) und RHIC in Schwer-Ionen-Kollisionen erzeugt.",
        difficulty = 5,
        funFact = "Das QGP im frühen Universum (< 10 µs nach dem Urknall) verhielt sich eher wie eine perfekte Flüssigkeit mit extrem niedriger Viskosität als wie ein Gas – überraschend für ein System von Elementarteilchen."
    ),

    // Question 43 – Nuclear Physics: Heavy-Ion Collisions
    Question(
        categoryId = 2,
        questionText = "Was misst die 'elliptic flow'-Observable v₂ in Schwerionenkollisionen und welche physikalische Eigenschaft des QGP leitet sie ab?",
        answerA = "v₂ = ⟨cos(2φ)⟩ misst die Anisotropie der Teilchenproduktion im Azimutwinkel relativ zur Reaktionsebene; hohes v₂ belegt starke Kollektivität und niedrige Scherviskosität η/s ≈ 1/(4π) des QGP",
        answerB = "v₂ misst die Rapidity-Verteilung der produzierten Hadronen und gibt Auskunft über die longitudinale Expansion des thermischen Feuerballs",
        answerC = "v₂ quantifiziert das Strangeness-Enhancement-Verhältnis (Strange/Non-Strange) als Signal für vollständige Thermalisierung im QGP",
        answerD = "v₂ ist der zweite harmonische Koeffizient der femtoskopischen HBT-Korrelationsfunktion und misst die Quell-Geometrie bei Ausfriertemperatur",
        correctAnswer = 0, // A
        explanation = "Die elliptic flow-Observablen v_n = ⟨cos(n·(φ-ψ_n))⟩ sind die Fourier-Koeffizienten der Winkelverteilung der Hadronen relativ zur n-ten Symmetrieebene. v₂ > 0 zeigt, dass die räumliche Anisotropie der Kollisionszone (Mandelförmige Überlappzone) in eine Impulsraum-Anisotropie umgewandelt wird – was kollektives hydromechanisches Verhalten beweist. Das Verhältnis η/s = ℏ/(4πk_B) (KSS-Grenze) ist die untere Schranke; RHIC/LHC-Messungen deuten auf nahe minimal-viskoses QGP hin.",
        difficulty = 5,
        funFact = null
    ),

    // Question 44 – Nuclear Physics: Nuclear Astrophysics
    Question(
        categoryId = 2,
        questionText = "Welcher Prozess ist verantwortlich für die Synthese der Elemente jenseits von Eisen (A > 56) in Neutronenstern-Mergers und massereichen Sternen?",
        answerA = "Der r-Prozess (rapid neutron capture) in extremen Neutronenfluss-Umgebungen: schnelle Neutroneneinfänge gefolgt von Beta-Zerfall erzeugen neutronenarme Kerne bis U und Th",
        answerB = "Der s-Prozess (slow neutron capture) in AGB-Sternen ist der dominante Mechanismus für alle Kerne A > 56 einschließlich Gold und Platin",
        answerC = "Photodisintegration bei T > 5·10⁹ K in Supernova-Schocks erzeugt durch Gleichgewichts-Prozessierung (NSE) alle stabilen Nuklide oberhalb von Eisen",
        answerD = "Der p-Prozess (proton capture) in Typ-Ia-Supernovae synthetisiert die neutronendefizitären Isotope wie ⁹²Mo und ⁹⁴Mo durch (p,γ)-Reaktionskaskaden",
        correctAnswer = 0, // A
        explanation = "Der r-Prozess (rapid neutron capture process) erfordert extreme Neutronenflüsse (n > 10²⁰ cm⁻³) und kurze Zeitskalen (~1–10 s). Neutronen werden so schnell eingefangen, dass Kerne neutronenreiche Gebiete jenseits der Stabilitätslinie erreichen bevor Beta-Zerfall einsetzt. Die Gravitationswellen-Ereignis GW170817 (Neutronensternverschmelzung) zeigte r-Prozess-Nukleosynthese durch die Kilonova AT2017gfo – Gold und Platin kommen primär aus Neutronenstern-Mergers.",
        difficulty = 5,
        funFact = "Das Gold in deinen Schmuckstücken wurde bei einer Neutronenstern-Kollision vor Milliarden Jahren erzeugt – jedes Gramm Gold repräsentiert kosmische Katastrophen."
    ),

    // Question 45 – Nuclear Physics: Radioactive Decay Chains
    Question(
        categoryId = 2,
        questionText = "Was beschreibt das Bateman-Gleichungssystem und welche Vereinfachung gilt bei 'weltlichem Gleichgewicht' (secular equilibrium)?",
        answerA = "Die Bateman-Gleichungen beschreiben die Zeitentwicklung der Aktivitäten in einem Zerfall-Ketten-System N₁→N₂→...→Nₙ; bei secular equilibrium (λ₁ ≪ λ₂,λ₃,...) gilt A₁ = A₂ = A₃ = ... (alle Töchter haben gleiche Aktivität wie Mutter)",
        answerB = "Das Bateman-System beschreibt nur binäre α-Zerfallsketten; für β-Zerfälle gelten modifizierte Ingrowth-Gleichungen mit Isotopen-Abundanzfaktoren",
        answerC = "Bei secular equilibrium gilt λ₁N₁ = λ₂N₂ nur wenn die Halbwertszeit der Tochterkerne exakt das Doppelte der Mutter beträgt",
        answerD = "Die Bateman-Gleichungen beschreiben ausschließlich die Spaltprodukt-Akkumulation in Kernreaktoren und gelten nicht für natürliche Zerfallsreihen",
        correctAnswer = 0, // A
        explanation = "Die Bateman-Gleichungen (Harry Bateman, 1910) sind ein System linearer gewöhnlicher Differentialgleichungen für jede Nuklid-Population N_i(t) in einer Zerfallskette: dN_i/dt = λ_{i-1}N_{i-1} - λ_i·N_i. Analytische Lösung via Laplace-Transformation. Beim säkularen Gleichgewicht gilt λ₁ ≪ λ₂,λ₃,...: Die Mutter-Aktivität bleibt näherungsweise konstant auf langer Zeitskala, und alle Töchter stellen sich auf gleiche Aktivität ein: A₁ = A₂ = ... = Aₙ (gilt nach ~10 Halbwertszeiten der langsamsten Tochter).",
        difficulty = 5,
        funFact = null
    ),

    // Question 46 – Cutting-Edge Research: CRISPR Base Editing
    Question(
        categoryId = 2,
        questionText = "Welcher molekulare Mechanismus liegt dem Cytosin-Base-Editing (CBE) zugrunde und durch welche Enzymfusion wird er realisiert?",
        answerA = "CBE nutzt eine Cas9-Nickase (nCas9-D10A) fusioniert mit Cytidin-Desaminase (rAPOBEC1): Die Desaminase konvertiert Cytosin→Uracil in der R-Schleife des geöffneten ssDNA-Strangs; Uracil wird als Thymin gelesen → C·G-zu-T·A-Mutation ohne Doppelstrangbruch",
        answerB = "CBE nutzt Cas9-Nuclease die einen Doppelstrangbruch einführt, gefolgt von homologiegesteuerter Reparatur (HDR) mit einem Einzelstrang-DNA-Template das Thymin statt Cytosin enthält",
        answerC = "CBE fusioniert dCas9 mit TET-Methyltransferase die 5-Methylcytosin zu 5-Hydroxymethylcytosin oxidiert und damit epigenetische statt genetische Editierung durchführt",
        answerD = "CBE verwendet eine adenosine deaminase acting on RNA (ADAR) adaptiert für DNA-Substrate, die C-zu-I (Inosin) konvertiert was als Guanin gelesen wird",
        correctAnswer = 0, // A
        explanation = "Cytosin-Base-Editoren (CBE, David Liu 2016) bestehen aus: 1) nCas9 (D10A Nickase, schneidet nur Nicht-Ziel-Strang), 2) Cytidin-Desaminase (rAPOBEC1 oder evolvierte Variante), 3) UGI (Uracil-Glykosylase-Inhibitor zum Blockieren der DNA-Reparatur). Der Mechanismus: Cas9 öffnet die DNA und exponiert einen einzelsträngigen R-loop; Desaminase konvertiert Cytosin→Uracil im Bearbeitungsfenster (Position 4–8); der Nicks-Strang wird als Matrize für Strang-Synthese genutzt → T·A statt C·G. Kein DSB, keine HDR-Abhängigkeit, ~50-fold höhere Effizienz als HDR.",
        difficulty = 5,
        funFact = "Base Editing ist besonders wertvoll für Punktmutations-Krankheiten: ~58 % aller bekannten menschlichen Pathogenen Einzelnukleotid-Varianten sind theoretisch durch CBE oder ABE korrigierbar."
    ),

    // Question 47 – Cutting-Edge Research: Topological Quantum Computing
    Question(
        categoryId = 2,
        questionText = "Welche Quasiteilchen sollen die Basis für topologisches Quantencomputing bilden und warum sind sie fehlerresistent?",
        answerA = "Majorana-Fermionen (Majorana zero modes) an Enden topologischer supraleitender Drähte bilden nicht-abelsche Anyonen; Quanteninformation ist nicht-lokal im Fermionenparitäts-Hilbertraum gespeichert und daher topologisch geschützt gegen lokale Störungen",
        answerB = "Skyrmionen in magnetischen Materialien tragen topologische Winding-Numbers und speichern Qubits in ihrer Chiralität, die gegen thermische Fluktuationen bis 300 K stabil ist",
        answerC = "Fractional Quantum Hall-Zustände bei ν=5/2 beherbergen e/4-Anyonen die Majorana-Charakter besitzen; Braiding dieser Anyonen führt direkt zu universellen Quantum-Gates",
        answerD = "Topologische Qubits werden in Josephson-Junction-Arrays kodiert deren nicht-triviale Homotopiegruppen π₁(S¹) = ℤ die Qubit-Zustände quantisieren und gegen Fluxon-Rauschen schützen",
        correctAnswer = 0, // A
        explanation = "Topologisches Quantencomputing (Kitaev 2003) nutzt Majorana-Nullmoden (MZM) – exotische Quasiteilchen die ihr eigenes Antipartikel sind – an den Enden topologischer supraleitender Nanodrähte (z.B. InSb/InAs + Nb). Da die Quanteninformation nicht-lokal zwischen zwei räumlich getrennten MZMs kodiert ist (fermionisches Paritäts-Qubit), ist sie gegen lokale Fehler intrinsisch immun. Braiding der MZMs entspricht unitären nicht-abelschen Transformationen. Microsoft investiert stark in diesen Ansatz.",
        difficulty = 5,
        funFact = "2023 präsentierte Microsoft erste Berichte über Majorana-Qubits in InAs-Drähten – die Ergebnisse sind jedoch umstritten, da frühere Berichte zurückgezogen werden mussten."
    ),

    // Question 48 – Cutting-Edge Research: mRNA Vaccine Technology
    Question(
        categoryId = 2,
        questionText = "Welche chemische Modifikation der mRNA war entscheidend für die Entwicklung therapeutisch einsetzbarer mRNA-Vakzine, und von wem wurde sie entdeckt?",
        answerA = "N1-Methylpseudouridin (m1Ψ) als Ersatz für Uridin in mRNA-Sequenzen: Entdeckt von Karikó und Weissman ~2005–2008, reduziert Immunogenität durch TLR-Aktivierung und steigert Translationseffizienz drastisch",
        answerB = "5-Methylcytidin-Modifikation aller Cytidine in der coding sequence, entwickelt von Langer & Anderson am MIT um 2010 für Lipid-Nanopartikel-Formulierungen",
        answerC = "2'-O-Methyl-Modifikation an jedem zweiten Nukleotid, pioneert von Tuschl und Bhavesh um 2003 für antisense-RNA-Therapeutika",
        answerD = "Pseudouridin-Inkorporation am 5'-Cap-Bereich der mRNA durch Nukleosid-Austausch, ursprünglich aus tRNA-Modifikationschemie adaptiert",
        correctAnswer = 0, // A
        explanation = "Katalin Karikó und Drew Weissman (Nobelpreis 2023) zeigten 2005–2008, dass Ersatz von Uridin durch Pseudouridin (Ψ) oder N1-Methylpseudouridin (m1Ψ) die Erkennung durch Toll-like-Rezeptoren (TLR3, TLR7, TLR8) drastisch reduziert. Ohne Modifikation aktiviert in-vitro-transkribierte mRNA das angeborene Immunsystem und wird schnell degradiert. Mit m1Ψ-Modifikation ist mRNA 'stealth' für das Immunsystem und wird effizienter translatiert – die Grundlage der BNT162b2 (Pfizer/BioNTech) und mRNA-1273 (Moderna) COVID-19-Vakzine.",
        difficulty = 5,
        funFact = "Katalin Karikó wurde jahrelang von Fördergebern abgelehnt und von der Universität Pennsylvania degradiert – ihre persiste Arbeit an mRNA-Modifikationen rettete letztlich Millionen Leben durch COVID-19-Impfstoffe."
    ),

    // Question 49 – Cutting-Edge Research: Gravitational Wave Astronomy
    Question(
        categoryId = 2,
        questionText = "Welche Informationen über das kosmische Hubble-Paramter H₀ liefern Gravitationswellen-Ereignisse kombiniert mit elektromagnetischer Gegenstück-Beobachtung ('standard sirens')?",
        answerA = "Gravitationswellen von Binär-Neutronenstern-Fusionen liefern eine modellunabhängige Leuchtkraft-Distanz d_L aus der Wellenform-Amplitude; kombiniert mit Rotverschiebung z des elektromagnetischen Gegenparts ergibt sich H₀ = v_res/d_L ohne Entfernungsleiter-Kalibrierung",
        answerB = "LIGO-Detektionsraten von Schwarzen-Loch-Fusionen kalibrieren die Cepheid-Leuchtkraft-Beziehung und lösen damit die Hubble-Spannung durch direkte Distanzmessung im lokalen Universum",
        answerC = "Gravitationswellen von Supernoven messen kosmologische Expansion durch das Sach-Wolfe-Effekt und geben H₀ mit Präzision < 1 % aus einem einzigen Ereignis",
        answerD = "Standard-Sirenen nutzen die Tidal-Deformierbarkeit Λ der Neutronensterne um die Hubble-Konstante durch die Masse-Radius-Beziehung der Zustandsgleichung abzuleiten",
        correctAnswer = 0, // A
        explanation = "Schutz (1986) schlug Gravitationswellen-Quellen als 'standard sirens' vor: Die absolute Leuchtkraft-Distanz d_L ergibt sich direkt aus der GW-Wellenform-Amplitude (inkl. Inklinations-Entartung), ohne Kalibration durch Entfernungsleitern. GW170817 + elektromagnetisches Gegenstück (NGC 4993, z = 0,009) lieferte H₀ = 70 ⁺¹²₋₈ km/s/Mpc. Mit mehr Ereignissen wird Präzision <1% erwartet und könnte die Hubble-Spannung (Planck: 67,4 vs. Distanzleiter: 73,0 km/s/Mpc) klären.",
        difficulty = 5,
        funFact = null
    ),

    // Question 50 – Cutting-Edge Research: Room-Temperature Superconductivity
    Question(
        categoryId = 2,
        questionText = "Welche Verbindungsklasse zeigte bislang die höchsten dokumentierten kritischen Temperaturen und warum sind Wasserstoff-reiche Hochdruckverbindungen theoretisch vielversprechend?",
        answerA = "Nickelat-Basierte Oxide (Infinite-Layer Nickelate wie Nd₀.₈Sr₀.₂NiO₂) zeigen T_c > 300 K bei Raumtemperatur und Normaldruck durch starke Hund'sche Kopplung im Ni-d-Band",
        answerB = "Wasserstoffreiche Hydride wie LaH₁₀ und H₃S unter Megabar-Druck (>150 GPa) zeigen T_c bis ~250 K; hohe T_c folgt aus hoher Debye-Frequenz der leichten H-Atome und starker Elektron-Phonon-Kopplung im BCS-Bild",
        answerC = "Kuprat-Supraleitungen (YBCO, Bi₂Sr₂CaCu₂O₈) erreichen T_c bis 200 K bei Normaldruck; ihre Hochtemperatur-Supraleitung basiert auf resonanzvalenz-Bindungsgitter-Spinfluktuationen",
        answerD = "Topologische Supraleiter auf Basis von twisted bilayer graphene (Magic Angle TBG) zeigen T_c = 1,7 K, aber der theoretische Grenzwert liegt nach BCS im Hochdrucklimit bei 400 K",
        correctAnswer = 1, // B
        explanation = "Wasserstoffreiche Hochdruckhydride (H₃S: T_c ≈ 203 K bei 155 GPa, Drozdov et al. 2015; LaH₁₀: T_c ≈ 250 K bei 170 GPa, Drozdov/Somayazulu 2019) zeigen die bislang höchsten T_c. Der Mechanismus ist konventionelle Phonon-vermittelte BCS-Supraleitung: Wasserstoff hat die höchste Debye-Frequenz aller Elemente; unter Druck wird H metastabil-metallisch und zeigt extrem starke Elektron-Phonon-Kopplung λ > 2. Die McMillan-Allen-Dynes-Formel T_c ≈ ω_log/1,2·exp(-1,04(1+λ)/(λ-µ*(1+0,62λ))) sagt hohe T_c für großes λ voraus.",
        difficulty = 5,
        funFact = "Ranga Dias (Rochester) beanspruchte 2020–2023 mehrfach Raumtemperatur-Supraleitung in Carbonhydraten – alle Publikationen wurden wegen Daten-Manipulations-Vorwürfen zurückgezogen. Raumtemperatur-Supraleitung bei Normaldruck bleibt eines der größten offenen Probleme der Physik."
    )

)
