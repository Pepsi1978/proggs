package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun scienceQuestionsMaster4(): List<Question> = listOf(

    // ── MASTER 4 (difficulty = 5) ── 50 questions ────────────────────────────

    // ── BLOCK 1: Topological Quantum Matter ──────────────────────────────────

    // Question 1 – Majorana Bound States
    Question(
        categoryId = 2,
        questionText = "Majorana-gebundene Zustände in topologischen Supraleitern zeichnen sich durch eine fundamentale Eigenschaft aus, die sie von gewöhnlichen fermionischen Quasiteilchen unterscheidet. Welche ist das, und warum ist diese Eigenschaft für fehlertolerantes Quantencomputing entscheidend?",
        answerA = "Majorana-Zustände besitzen halbzahligen Spin und leben ausschließlich an Schichtgrenzen von Topoisomeren; ihre Spin-Kohärenz überlebt thermische Fluktuationen bis 77 K",
        answerB = "Majorana-Zustände sind ihre eigenen Antiteilchen (γ = γ†) und treten als nicht-lokale Paare an räumlich getrennten Defekten auf; ihre nicht-abelsche Austauschstatistik ermöglicht topologisch geschützte Qubit-Operationen durch Flechten (Braiding)",
        answerC = "Majorana-Zustände haben exakt verschwindende effektive Masse in der Bandstruktur; die zugehörige Topologie schützt sie vor Dekohärenz durch beliebige lokale Störungen bei Temperaturen unter der Debye-Temperatur",
        answerD = "Majorana-Zustände entstehen durch spontane Symmetriebrechung der Zeitumkehrinvarianz und sind auf Kristalloberflächen mit C₄-Symmetrie beschränkt; ihr nicht-trivialer Z-Index verhindert Streuprozesse",
        correctAnswer = 1, // B
        explanation = "Majorana-Fermionen sind identisch mit ihrer eigenen Antiteilchen-Entsprechung: γ = γ†. In topologischen Supraleitern der Klasse D (z.B. p-Wellen-Supraleiter, Fu-Kane-Heterostruktur) erscheinen sie als Nullmoden an den Enden von Vortex-Kernen oder Nanodraht-Enden. Da das Qubit-Zustand nicht-lokal in zwei räumlich getrennten Majoranas kodiert ist, können lokale Störungen das Qubit nicht dekorieren. Das Braiding zweier Majoranas erzeugt unitäre Operationen, die zur Kliff-Gruppe gehören und topologisch geschützt sind.",
        difficulty = 5,
        funFact = "Microsoft hat ein ganzes Quantencomputing-Programm (Station Q) auf Majorana-Qubits aufgebaut. Die experimentelle Realisierung in InAs/Al-Heterostrukturen ist jedoch bis heute umstritten — ein 2021er Nature-Paper wurde nach Integritätsbedenken zurückgezogen."
    ),

    // Question 2 – Non-Abelian Anyons
    Question(
        categoryId = 2,
        questionText = "Im fraktionalen Quanten-Hall-Zustand bei Füllfaktor ν = 5/2 (Moore-Read-Zustand) werden Quasiteilchen mit nicht-abelscher Anyonenstatistik vermutet. Was unterscheidet nicht-abelsche von abelschen Anyonen grundlegend?",
        answerA = "Abelsche Anyonen akkumulieren beim Umkreisen eine komplexe Phase e^{iθ} im Wellenvektor; nicht-abelsche Anyonen akkumulieren stattdessen eine reelle Phase ±1 und wechseln dadurch ihre Ladung",
        answerB = "Bei nicht-abelschen Anyonen hängt der Endzustand nach mehreren Austauschoperationen von der Reihenfolge der Operationen ab: Die zugehörigen Braid-Matrizen kommutieren nicht miteinander, und das System besitzt einen entarteten Grundzustandsraum, der als topologischer Qubit-Raum dient",
        answerC = "Nicht-abelsche Anyonen sind bosonisch und gehorchen der Bose-Einstein-Statistik nur im thermodynamischen Grenzfall; ihre Quantenzahlen transformieren unter der SO(3)-Gruppe und nicht unter U(1)",
        answerD = "Abelsche Anyonen existieren nur in 2+1 Dimensionen als Gittersolitonen; nicht-abelsche Anyonen sind hingegen auf 3+1-dimensionale topologische Feldtheorien (Chern-Simons) beschränkt",
        correctAnswer = 1, // B
        explanation = "Im Moore-Read-Pfaffian-Zustand (ν = 5/2) tragen Quasiteilchen Ladung e/4 und sind nicht-abelsche Anyonen: Das Vertauschen zweier Anyonen entspricht einer unitären Matrix-Transformation im entarteten Grundzustandsraum. Zwei verschiedene Braiding-Operationen ergeben im Allgemeinen unterschiedliche Endzustände, je nach Reihenfolge. Dieser nichtkommutative Charakter ist die Grundlage für topologisches Quantencomputing nach Kitaev. Die experimentelle Herausforderung ist das direkte Messen dieser nicht-abelschen Statistik.",
        difficulty = 5,
        funFact = null
    ),

    // Question 3 – Topological Entanglement Entropy
    Question(
        categoryId = 2,
        questionText = "Die topologische Verschränkungsentropie γ eines 2D topologischen Phasenzustands ist eine universelle Konstante, die durch welche physikalische Größe bestimmt wird, und wie wird sie aus der Entanglement-Entropie S(A) einer Region A extrahiert?",
        answerA = "γ ist proportional zur Grundzustandsenergie pro Fläche; sie wird durch S(A) = α|∂A| + γ extrahiert, wobei |∂A| der Umfang ist — γ entspricht dem Achsenabschnitt im thermodynamischen Limes",
        answerB = "γ = ln(D), wobei D = √(Σᵢ dᵢ²) die totale Quantendimension der topologischen Theorie ist; sie wird durch eine spezielle lineare Kombination S(A) + S(B) + S(C) - S(AB) - S(BC) - S(AC) + S(ABC) aus überlappenden Regionen extrahiert (Kitaev-Preskill-Konstruktion)",
        answerC = "γ entspricht der Anzahl der Randmoden des topologischen Isolators dividiert durch 2π; sie ist messbar durch Raman-Streuung an der Grenzfläche zwischen trivialer und topologischer Phase",
        answerD = "γ ist identisch mit dem Chern-Zahl-Invarianten der Bulk-Bandstruktur; sie divergiert logarithmisch bei der topologischen Phasenübergangstemperatur und ist damit als kritischer Exponent klassifiziert",
        correctAnswer = 1, // B
        explanation = "Kitaev und Preskill (2006) sowie Levin und Wen (2006) zeigten unabhängig, dass die Entanglement-Entropie einer Region A in einem 2D topologischen Zustand der Form S(A) = α|∂A| - γ + ... folgt, wobei γ = ln(D) die topologische Verschränkungsentropie ist. D = √(Σᵢ dᵢ²) ist die totale Quantendimension, dᵢ die Quantendimension jedes Anyonen-Typs. Für abelsche topologische Zustände mit N Grundzustandsentartung gilt D = √N. Die Kitaev-Preskill-Konstruktion subtrahiert Rand-Beiträge durch eine spezifische inklusions-exklusions-artige Kombination.",
        difficulty = 5,
        funFact = "Für den Toric Code von Kitaev gilt D = 2 und damit γ = ln(2). Beim Moore-Read-Zustand für ν = 5/2 mit nicht-abelschen Anyonen hat man D = 2√2 — die irrationale Dimension ist eine direkte Konsequenz der nicht-abelschen Statistik."
    ),

    // Question 4 – Kitaev Honeycomb Model
    Question(
        categoryId = 2,
        questionText = "Das Kitaev-Honigwaben-Modell ist ein exakt lösbares Spin-1/2-Modell auf dem Honigwaben-Gitter. Welche topologischen Phasen enthält es, und durch welchen mathematischen Trick wird es lösbar?",
        answerA = "Das Modell zeigt eine einzige topologische Phase mit Chern-Zahl C=1; es wird durch Jordan-Wigner-Transformation in ein freies Fermion-Problem überführt, das durch Diagonalisierung einer 2×2-Matrix pro k-Punkt gelöst wird",
        answerB = "Das Modell enthält eine gapped abelsche Phase (toric-code-artig) und eine gapless Phase; im Magnetfeld öffnet sich eine Lücke und die nicht-abelsche Phase mit Chern-Zahl C=±1 und Majorana-Randmoden entsteht; die Lösbarkeit kommt durch Majorana-Darstellung σ = iγ^x γ^y γ^z γ^0 mit einem Z₂-Eichfeld",
        answerC = "Das Modell besitzt drei ferromagnetische Phasen, getrennt durch Kosterlitz-Thouless-Übergänge; es ist durch eine Bosonisierung der Spinoperatoren in ein Heisenberg-Antiferromagneten überführbar",
        answerD = "Das Modell ist nur im Grenzfall J_x = J_y = J_z lösbar und zeigt dort eine U(1)-Symmetrie; die topologische Phase wird durch eine spontane Z₂-Symmetriebrechung bei T < J/k_B charakterisiert",
        correctAnswer = 1, // B
        explanation = "Kitaev (2006) schrieb jeden Spin σᵃ als Produkt von vier Majorana-Fermionen: σᵃ = iγᵃγ⁰. Dies führt auf ein freies Majorana-Fermion-Problem, das an einen statischen Z₂-Eichfeld koppelt. Das Phasendiagramm zeigt: Drei gapped A-Phasen (abelsche topologische Ordnung, äquivalent zum Toric Code) und eine gapless B-Phase. Mit einem externen Magnetfeld öffnet die B-Phase eine Lücke zur nicht-abelschen Phase mit Chern-Zahl ±1 und chiralen Majorana-Randmoden. α-RuCl₃ und Na₂IrO₃ sind experimentelle Kandidaten.",
        difficulty = 5,
        funFact = null
    ),

    // Question 5 – Measurement-Based Topological QC
    Question(
        categoryId = 2,
        questionText = "Beim messungsbasierten topologischen Quantencomputing im Rahmen des Raussendorf-Harrington-Goyal-(RHG)-Codes wird Fehlerkorrektur durch Messung von Stabilisatoren implementiert. Welche geometrische Struktur und welcher Schwellenwert-Fehlerwert charakterisieren diesen Ansatz?",
        answerA = "Der RHG-Code verwendet einen 2D-Cluster-Zustand auf einem quadratischen Gitter; der Fehlerschwellenwert liegt bei ca. 1 % pro Gate-Operation, und die Syndrome werden durch klassische Minimum-Weight-Matching-Algorithmen dekodiert",
        answerB = "Der RHG-Code basiert auf einem 3D primitiven kubischen Gitter von verschränkten Qubits (Raussendorf-Gitter); Fehler erzeugen Strings von Anyonen, deren Syndrome durch klassische Minimum-Weight-Perfect-Matching (MWPM) dekodiert werden; der Fehlerschwellenwert liegt bei ~0,75 % für depolarisierende Fehler",
        answerC = "Der RHG-Code nutzt einen topologischen Cluster-Zustand auf dem Kagome-Gitter und besitzt einen Fehlerschwellenwert von ~3 %; die Syndrome entsprechen geschlossenen Schleifen in der Homologiegruppe H₁ des Gitters",
        answerD = "Der RHG-Code implementiert Stabilisatormessungen auf einem hyperbolischen Gitter mit negativer Krümmung; der Fehlerschwellenwert divergiert im thermodynamischen Limes wegen der exponentiell wachsenden Randlänge",
        correctAnswer = 1, // B
        explanation = "Raussendorf, Harrington und Goyal (2007) konstruierten einen topologischen Cluster-Zustand auf dem 3D primitiven kubischen Gitter ('Raussendorf-Gitter'). Logische Qubits sind durch nicht-triviale Homologieklassen des Gitters kodiert. Pauli-Fehler erzeugen Anyonen-Paare, die durch String-Operatoren verbunden sind. Das klassische Dekodieren (MWPM) sucht die kürzeste Fehler-Konfiguration. Der Schwellenwert ~0,75 % ist mit realistischen physischen Qubits erreichbar. Dieser Ansatz ist eine der am intensivsten verfolgten Architekturen für fehlertolerante Quantencomputer.",
        difficulty = 5,
        funFact = "Google's Willow-Quantenprozessor (2024) demonstrierte einen Schwellenwert unterhalb der Fehlerkorrekturgrenze für den Surface Code — eine direkte Verwandte des RHG-Codes — mit 105 supraleitenden Qubits."
    ),

    // ── BLOCK 2: Advanced Proteomics ─────────────────────────────────────────

    // Question 6 – Native Mass Spectrometry
    Question(
        categoryId = 2,
        questionText = "Native Massenspektrometrie ermöglicht die Analyse von Proteinkomplexen im nativen Zustand. Welche experimentellen Bedingungen sind entscheidend, um die native Stöchiometrie eines heteromeren Komplexes zu erhalten, und welche Information liefert das Spektrum direkt?",
        answerA = "Native MS erfordert denaturierende SDS-Bedingungen (0,1 % SDS, pH 2) zur vollständigen Protonierung; das Spektrum liefert Sekundärstrukturinformationen durch Circular-Dichroismus-Peaks bei m/z < 500",
        answerB = "Native MS verwendet flüchtige Ammoniumacetat-Puffer (typisch 100–500 mM, pH 6,8–7,5) statt konventioneller Nicht-flüchtiger Puffer; ESI erzeugt niedrig geladene, native Ionen (charge states z ≤ 50 für MDa-Komplexe); das Spektrum liefert direkt die Gesamtmasse und Stöchiometrie der Untereinheiten sowie Ligandenbindung",
        answerC = "Native MS arbeitet ausschließlich mit MALDI (Matrix-Unterstützte Laser-Desorption), da nur dieser Ionisierungsprozess Proteinkomplexe unversehrt lässt; die typische Massgenauigkeit beträgt ±10 kDa für Komplexe über 500 kDa",
        answerD = "Native MS kombiniert immer mit Röntgenkristallographie zur Phasierung; ohne Kristallstruktur können native Komplexe massenspektrometrisch nicht zugeordnet werden, da die Ionen keine definierten charge states aufweisen",
        correctAnswer = 1, // B
        explanation = "Native MS (Entwickelt durch Robinson-Gruppe, Oxford) bedarf spezieller Bedingungen: Ammoniumacetat als flüchtiger Puffer (nicht-flüchtige Salze wie NaCl würden Addukte bilden), milde ESI-Bedingungen mit niedrigen Desolvationsenergien. MDa-große Komplexe erscheinen als Serie eng beieinanderliegender Peaks bei m/z 3.000–20.000. Die Stöchiometrie wird durch MS/MS bestimmt: Subkomplex-Dissoziation durch Kollisionsaktivierung erlaubt die Zusammensetzung direkt abzulesen. Ribosomen (2,3 MDa), Proteasom (700 kDa) und virale Kapseln wurden so charakterisiert.",
        difficulty = 5,
        funFact = "Carol Robinson (Oxford) erhielt 2018 den Kavli-Preis für Nanowissenschaft für die Entwicklung der nativen Massenspektrometrie. Ihr Labor zeigte erstmals, dass intakte Membranprotein-Detergenz-Micell-Komplexe im Hochvakuum der MS stabil sind."
    ),

    // Question 7 – Hydrogen-Deuterium Exchange MS
    Question(
        categoryId = 2,
        questionText = "Wasserstoff-Deuterium-Austausch-Massenspektrometrie (HDX-MS) misst Proteindynamik und Konformationsänderungen. Welcher chemische Prozess liegt zugrunde, und wie unterscheidet HDX-MS zwischen zugänglichen und strukturierten Regionen?",
        answerA = "HDX-MS misst den Austausch von Cα-H-Atomen gegen Deuterium bei Temperaturen über 80°C; zugängliche Loops erscheinen als scharfe Peaks, strukturierte β-Faltblätter als breite Peaks im MALDI-Spektrum",
        answerB = "Amidwasserstoffe (N-H) in Peptidbindungen tauschen mit D₂O; die Austauschrate hängt von lokaler Flexibilität und Lösungsmittelzugänglichkeit ab — schnell in unstrukturierten Loops, langsam (Stunden-Tage) in H-gebundenen Sekundärstrukturelementen; nach Quenching bei pH 2,5/0°C und Pepsinverdau misst LC-MS den Deuterierungsgrad peptidaufgelöst",
        answerC = "HDX-MS misst den Austausch von Lysin-Ε-NH₂-Gruppen mit D₂O bei physiologischem pH; die Halbwertszeit des Austauschs ist direkt proportional zur Zugänglichkeit der Oberfläche, messbar durch SASA-Berechnungen aus der Kristallstruktur",
        answerD = "HDX-MS basiert auf dem nuklearen Overhauser-Effekt zwischen ¹H und ²H; es ist eine NMR-MS-Hybridmethode, die simultane Strukturinformation und Masseninformation liefert und ausschließlich an magentischen Sektorfeldgeräten durchführbar ist",
        correctAnswer = 1, // B
        explanation = "Backbone-Amidprotonen (NH) tauschen mit lösungsmittelzugänglichem D₂O; die EX2-Kinetik dominiert unter physiologischen Bedingungen: k_ex = k_op·k_int/(k_cl + k_int). Strukturierte, H-verbrückte Regionen (α-Helices, β-Sheets) haben extrem niedrige Öffnungsraten k_op und damit langsame HDX. Das Experiment: Protein in D₂O zu verschiedenen Zeitpunkten inkubieren → Quench (pH 2,5, 0°C stoppt Rückaustausch) → Pepsinproteolyse → LC-MS. Masseverschiebung ΔM/Charge = Einbau von Deuteriumatomen. Anwendung: Epitop-Kartierung, Allosterie, Komplex-Interfaces.",
        difficulty = 5,
        funFact = null
    ),

    // Question 8 – Crosslinking Mass Spectrometry
    Question(
        categoryId = 2,
        questionText = "Crosslinking-Massenspektrometrie (XL-MS) definiert räumliche Abstands-Constraints für Proteinkomplexe. Welche Crosslinker werden bevorzugt eingesetzt, und wie werden XL-Peptide in LC-MS/MS identifiziert?",
        answerA = "XL-MS verwendet UV-photoaktivierbare Diazirene mit einem Crosslink-Abstandslimit von 2 Å; die Identifikation erfolgt durch Neutralverlust von Diazomethan (m/z 42) im MS³-Spektrum",
        answerB = "NHS-Ester-Crosslinker (z.B. DSS, Abstand ~11 Å zwischen Cα der verknüpften Lys-Reste) verbinden primäre Amine; im LC-MS/MS-Experiment erzeugen XL-Peptide beim Fragmentieren charakteristische Neutralverlust-Peaks und b/y-Ionen beider verknüpften Peptidketten; spezialisierte Software (pLink, Kojak, XlinkX) durchsucht den quadratischen Suchraum aller Peptidpaare",
        answerC = "XL-MS verwendet ausschließlich kovalente Disulfid-Bridges zwischen Cysteinen als natürliche Crosslinks; das Experiment wird unter reduzierenden Bedingungen durchgeführt, um artifizielle Disulfide zu minimieren",
        answerD = "Bei XL-MS werden Proteine durch EDC (Carbodiimid) über ihre gesamte Oberfläche vernetzt; die Masseverschiebung von exakt 18 Da pro Crosslink erlaubt eine eindeutige Identifikation ohne aufwändige Datenbanksuche",
        correctAnswer = 1, // B
        explanation = "DSS (Disuccinimidylsuberat) und BS3 (wasserlösliches Analogon) sind Standard-NHS-Ester-Crosslinker, die Lysin-Lysin-Abstände bis ~30 Å Cβ-Cβ brücken. Im MS/MS fragmentieren XL-Peptide zu b/y-Ionen beider Peptidketten. Das Hauptproblem ist die kombinatorische Datenbanksuche: Statt N Peptide zu durchsuchen müssen N² Peptidpaare geprüft werden. Cleavable Crosslinker (DSSO: MS-cleavable Sulfoxid) vereinfachen die Identifikation durch charakteristischen Neutralverlust. XL-MS liefert Cα-Cα-Abstandsbeschränkungen (<30 Å), die für Cryo-EM-gestützte Strukturmodellierung genutzt werden.",
        difficulty = 5,
        funFact = "Mit XL-MS wurde die Architektur des menschlichen 26S-Proteasoms (2,5 MDa, 33 Untereinheiten) in Lösung kartiert — eine Leistung, die zeigt, dass XL-MS Strukturdaten für Komplexe liefert, für die Kristallisation unpraktisch ist."
    ),

    // Question 9 – Ion Mobility Separation
    Question(
        categoryId = 2,
        questionText = "Ionenmobilitäts-Massenspektrometrie (IM-MS) trennt Ionen nach ihrer Form und Größe in der Gasphase. Welche physikalische Größe wird direkt gemessen, und wie wird sie mit der Proteinstruktur verknüpft?",
        answerA = "IM-MS misst die dielektrische Relaxationszeit von Proteinionen im elektrischen Wechselfeld; der Zusammenhang mit der Struktur erfolgt über die Debye-Hückel-Theorie für geladene Makromoleküle in der Gasphase",
        answerB = "IM-MS misst die Driftzeit (Ankunftszeit) von Ionen durch ein Puffergas im elektrischen Gleichfeld; daraus wird der kollisionsinduzierte Querschnitt (CCS, Ω) berechnet — ein direktes Maß für die 3D-Ausdehnung des Proteins; verschiedene Konformere desselben Proteins erscheinen als getrennte Peaks im Mobilitätsspektrum",
        answerC = "IM-MS misst den magnetischen Dipolmoment von Proteinionen im inhomogenen Magnetfeld; die Massenauflösung erhöht sich dadurch auf 500.000 (bei Quadrupol-IM-FTICR), und die Wasserstoffbrückenanzahl ist aus dem CCS ableitbar",
        answerD = "IM-MS nutzt die Zentrifugalkraft in einem rotierenden Elektrodenring (FAIMS); die effektive Masse wird durch den CV/DV-Quotienten (compensation voltage / dispersion voltage) bestimmt und ist mit molekularen Radien über die Stokes-Einstein-Gleichung verknüpft",
        correctAnswer = 1, // B
        explanation = "In der Driftröhren-Ionenmobilität (DTIM) wandern Ionen unter einem homogenen elektrischen Feld durch ein Inertgas (typisch N₂ oder He). Die Driftgeschwindigkeit v = K·E ist proportional zur Mobilität K. Der CCS (collision cross section) Ω wird über die Mason-Schamp-Gleichung berechnet: K = (3q/16N) · √(2π/μk_BT) · (1/Ω), wobei N die Puffergas-Zahlendichte ist. CCS-Werte aus Experimenten werden direkt mit berechneten CCS-Werten aus Proteinkristallstrukturen (MOBCAL, IMPACT) verglichen. Konformationsisomere (gefaltete vs. entfaltete Proteine) werden als separate Peaks im arrival time distribution sichtbar.",
        difficulty = 5,
        funFact = null
    ),

    // Question 10 – Data-Independent Acquisition
    Question(
        categoryId = 2,
        questionText = "Datenunabhängige Akquisition (DIA) im Gegensatz zur datenabhängigen Akquisition (DDA) verändert fundamental die Probenauswahl in LC-MS/MS-Proteomik. Was ist das Kernprinzip von DIA, und welche bioinformatische Herausforderung bringt es mit sich?",
        answerA = "DIA wählt den intensivsten Precursor-Ion in jedem MS1-Scan zur Fragmentierung aus; die bioinformatische Herausforderung ist die Rückverfolgung von Vorläuferionen bei sehr hoher Ionendichte über mehrere Chromatographieläufe",
        answerB = "DIA fragmentiert alle Ionen innerhalb breiter Precursor-Isolationsfenster (typisch 4–25 Th) ohne Vorauswahl, wodurch alle Peptide in jedem Spektrum co-fragmentiert werden; die bioinformatische Herausforderung ist die Entfaltung von gemischten Fragmentspektren — gelöst durch spektrale Bibliotheken und maschinelles Lernen (DIA-NN, Spectronaut) zur In-silico-Rekonstitution von virtuellen DDA-Spektren",
        answerC = "DIA verwendet ausschließlich CID-Fragmentierung mit sehr niedrigen Kollisionsenergien (5 eV), um Primärstrukturinformation zu erhalten; die bioinformatische Aufgabe ist die De-novo-Sequenzierung aller Peptide ohne Proteindatenbank-Referenz",
        answerD = "DIA basiert auf selektiver Reaktionsüberwachung (SRM) mit bis zu 500 vordefinierten Übergängen pro Experiment; die bioinformatische Herausforderung besteht in der Optimierung der Übergangs-Kollisionsenergien für jedes individuelle Peptid",
        correctAnswer = 1, // B
        explanation = "Bei DDA (traditionell) wählt der MS-Controller die Top-N intensivsten Precursor-Ionen für MS/MS aus — peptidreiche Proben haben hohe Unterabtastung (stochastic sampling). DIA umgeht dies: Alle Ionen im gesamten m/z-Bereich werden systematisch in überlappenden Fenstern fragmentiert. Dies erzeugt hochkomplexe chimäre Spektren. DIA-NN (2022) nutzt neuronale Netze und Deep-Learning-generierte spektrale Bibliotheken (in silico predicted), um DIA-Rohdaten direkt auszuwerten. Ergebnis: vollständigere Proteomabdeckung, bessere Reproduzierbarkeit zwischen Läufen und quantitative Konsistenz — unverzichtbar für klinische Biomarker-Studien.",
        difficulty = 5,
        funFact = "DIA-NN v1.8 (2022) kann mit in-silico-generierten Bibliotheken ohne experimentelle Spektralbibliothek arbeiten und erreicht dabei eine Proteomabdeckung, die frühere Bibliotheks-basierte Methoden übertrifft — ein Paradigmenwechsel in der Hochdurchsatz-Proteomik."
    ),

    // ── BLOCK 3: Extreme Astrophysics ────────────────────────────────────────

    // Question 11 – Magnetar Physics
    Question(
        categoryId = 2,
        questionText = "Magnetare sind Neutronensterne mit extremen Magnetfeldern (B ~ 10¹⁴–10¹⁵ G). Durch welchen Mechanismus entstehen diese Felder, und welche physikalischen Prozesse erklären die charakteristischen hochenergetischen Ausbrüche (Giant Flares)?",
        answerA = "Magnetar-Felder entstehen durch Akkretion von Materie aus dem Interstellarmedium; Giant Flares entstehen durch Kernverschmelzung von Deuterium im inneren Kern, der bei Temperatures über 10¹¹ K Fusionszünden erreicht",
        answerB = "Die starken Felder entstehen wahrscheinlich durch einen α-Ω-Dynamo in der heißen turbulenten Protonen-Neutronenstern-Phase kurz nach dem Kollaps (Drehperiode < 3 ms, konvektive Turbulenz); Giant Flares entstehen durch plötzliche magnetische Rekonnexion in der Kruste oder Magnetfeldinstabilitäten — globale Krustenbewegungen lösen riesige Magnetosphären-Neuordnungen aus, die Energie von ~10⁴⁶ erg in Sekunden freisetzen",
        answerC = "Magnetfelder in Magnetaren entstehen durch Wiedervereinigung benachbarter Magnetfeldlinien im Weißen Zwerg-Vorläufer; Giant Flares sind durch das Aufheizen des Magnetars durch einfallende Meteoriten erklärt, die beim Einschlag Röntgenstrahlung emittieren",
        answerD = "Magnetare entstehen durch Neutronensterffusionen (Doppelstern-Merger) und erben das kombinierte Magnetfeld beider Vorläufer; Giant Flares sind periodisch wiederkehrende Ereignisse mit exaktem 27-Tage-Zyklus entsprechend der Umlaufzeit des Begleitsterns",
        correctAnswer = 1, // B
        explanation = "Die Dynamo-Theorie (Thompson & Duncan, 1993) erklärt Magnetarfelder durch einen Turbulenz-getriebenen α-Ω-Dynamo im ersten Sekunden nach dem Kollaps, wenn der Neutronenstern noch flüssig ist und schnell rotiert. Magnetfeldstärken bis 10¹⁵ G sind möglich. Oster-Flares (Giant Flares wie SGR 1806-20 am 27.12.2004, E~10⁴⁶ erg) werden durch eine plötzliche globale Neuordnung des magnetosphärischen Magnetfelds erklärt — das entspricht einem 'Starquake' der Neutronensternnkruste, der die Magnetfeldgeometrie destabilisiert. Das Hauptspektrum zeigt sich als harte Röntgen/Gamma-Emission.",
        difficulty = 5,
        funFact = "Der Giant Flare von SGR 1806-20 am 27. Dezember 2004 setzte in 0,2 Sekunden mehr Energie frei als die Sonne in 250.000 Jahren — aus einer Entfernung von 50.000 Lichtjahren ionisierte er die Erdatmosphäre messbar."
    ),

    // Question 12 – Fast Radio Bursts
    Question(
        categoryId = 2,
        questionText = "Schnelle Radioausbrüche (FRBs) zeigen Dispersionsmessungen, die auf extragalaktischen Ursprung hinweisen. Welche Information liefert das Dispersionsmass (DM) physikalisch, und welches Emissionsmodell gilt für den wiederholenden FRB 121102 als führend?",
        answerA = "Das DM quantifiziert die Rotverschiebung der FRB-Quelle; für FRB 121102 gilt das Modell der Kernkollaps-Supernova, bei der der Kollaps eines massereichen Sterns eine einmalige Radioimpuls erzeugt",
        answerB = "Das DM = ∫n_e dl (Elektronen-Säulendichte in pc/cm³) misst die integrierte freie Elektronen-Dichte entlang der Sichtlinie — höheres DM bedeutet größere kosmologische Distanz; für FRB 121102 gilt ein Magnetar-Modell als führend: kohärente Curvature-Strahlung von relativistischen Teilchen in der verdrillten Magnetosphäre eines jungen Magnetars",
        answerC = "Das DM ist äquivalent zur neutralen Wasserstoffsäulendichte N_HI in 10²⁰ cm⁻² Einheiten; FRB 121102 entsteht durch Gezeitenkräfte eines supermassiven schwarzen Lochs, das periodisch Sterne zerreißt (tidal disruption events)",
        answerD = "Das DM beschreibt die spektrale Intensitätsverteilung (Jy/MHz) des FRB; der einzige gesicherte Entstehungsmechanismus für alle FRBs sind Neutronensternverschmelzungen mit anschließendem magnetisch getriebenen Jet",
        correctAnswer = 1, // B
        explanation = "Das Dispersionsmass DM = ∫₀ᴸ n_e(l) dl entsteht dadurch, dass Radiowellen in einem Plasma dispersiv propagieren: Niederfrequente Anteile kommen später an (ν⁻² Abhängigkeit). Da galaktische DM-Werte gut modelliert sind (NE2001, YMW16), kann der extragalaktische DM-Beitrag isoliert werden und auf kosmologische Entfernungen hinweisen. FRB 121102 ist der erste wiederholende FRB (Spitler et al. 2016), lokalisiert in einer Zwerggalaxie bei z=0,19. Die periodische Aktivität, der persistente Radiogegenpart und das hohe Rotationsmass (RM ~ 10⁵ rad/m²) sprechen für einen jungen Magnetar in einer dichten, magnetisierten Umgebung (z.B. Supernovaüberreste).",
        difficulty = 5,
        funFact = "CHIME (Canadian Hydrogen Intensity Mapping Experiment) hat seit 2019 über 1000 FRBs katalogisiert und gezeigt, dass FRBs kosmologisch ubiquitär sind — eine neue Klasse von transienten astrophysikalischen Phänomenen, die potenziell als kosmologische Standardkerzen dienen könnten."
    ),

    // Question 13 – Kilonova Nucleosynthesis
    Question(
        categoryId = 2,
        questionText = "Kilonovae bei Neutronensternverschmelzungen sind der Ort des r-Prozesses der Nukleosynthese. Welche Beobachtung von GW170817/AT2017gfo bestätigte die r-Prozess-Hypothese, und warum läuft der r-Prozess bevorzugt in Neutronenstern-Ejecta ab?",
        answerA = "GW170817 zeigte eine blaue Kilonova, erklärt durch ⁵⁶Ni-Zerfall (wie bei Supernovae); der r-Prozess bevorzugt Neutronenstern-Ejecta wegen der hohen Temperatur (T > 10¹² K), die freie Neutronen durch Kernfission erzeugt",
        answerB = "AT2017gfo zeigte eine langsam abkühlende optisch-infrarote Transiente mit Opazität von κ ~ 10 cm²/g, charakteristisch für Lanthanid-Produkte des r-Prozesses; Neutronenstern-Ejecta eignen sich wegen ihres extremen Neutronenüberschusses (Y_e < 0,25) — der r-Prozess erfordert n/p-Verhältnisse >> 1, um schnell schwere Kerne durch aufeinanderfolgende Neutroneneinfänge über die Stabilitätsgrenze hinaus aufzubauen",
        answerC = "AT2017gfo zeigte extrem blaue UV-Emission, entstanden durch Sr-Ba-Ionen-Emissionslinien bei 400 nm; der r-Prozess bevorzugt diese Umgebung wegen der niedrigen metallischen Opazität, die freie Elektronen in der Ejecta-Hülle minimiert",
        answerD = "GW170817 zeigte eine röntgenstarke Kilonova, erklärt durch Akkretionsscheibe-Wind-Emission; der r-Prozess läuft in Neutronenstern-Ejecta bevorzugt ab, weil die hohe Elektronendichte n_e > 10³⁵ cm⁻³ Beta-Zerfälle vollständig unterdrückt",
        correctAnswer = 1, // B
        explanation = "AT2017gfo (optisches Gegenstück zu GW170817, dem ersten Neutronenstern-Merger mit GW+EM-Beobachtung) zeigte charakteristische Merkmale von Lanthanid-reicher Materie: eine rote Komponente mit hoher Opazität κ ~ 10 cm²/g (verglichen mit κ ~ 0,1 für Fe-reiche Supernovae). Lanthanide (Z=57-71) und Actinide haben f-Schalen-Elektronen mit zahlreichen Energieniveaus, die eine hohe Linienopazität im Optisch-NIR erzeugen. Das neutronenreiche Ejecta (Y_e ~ 0,1–0,4) ist ideal für den r-Prozess: Neutronen werden so schnell eingefangen, dass β-Zerfall kaum konkurriert, und die Kern-Chart wird jenseits der Stabilitätsgrenze traversiert. Sr wurde spektroskopisch in AT2017gfo identifiziert (Watson et al. 2019).",
        difficulty = 5,
        funFact = "Durch GW170817 wurde geschätzt, dass Neutronensternverschmelzungen ausreichen, um den gesamten r-Prozess-Bedarf der Milchstraße zu decken — inklusive des Goldes in deinem Schmuck, das wahrscheinlich in einer solchen kosmischen Kollision synthetisiert wurde."
    ),

    // Question 14 – Tidal Disruption Events
    Question(
        categoryId = 2,
        questionText = "Gezeitenzerreißungsereignisse (TDEs) entstehen, wenn ein Stern einem massereichen schwarzen Loch zu nahe kommt. Welcher Parameter bestimmt den Gezeitenzerreißungsradius, und welches charakteristische Lichtkurven-Verhalten unterscheidet TDEs von Supernovae?",
        answerA = "Der Gezeitenzerreißungsradius hängt ausschließlich von der Sternmasse ab: r_t = M_star^{1/3}; TDEs zeigen eine plateauförmige Lichtkurve über mehrere Jahre ohne charakteristischen Peak",
        answerB = "Der Gezeitenzerreißungsradius r_t ≈ R_star (M_BH/M_star)^{1/3} skaliert mit der Sternausdehnung und dem Masseverhältnis; TDEs zeigen eine charakteristische t^{-5/3}-Zeitentwicklung der Fallbackrate von Materie auf das schwarze Loch, was zu einem spezifischen Abklingverhalten der Lichtkurve führt",
        answerC = "Der Gezeitenzerreißungsradius liegt immer innerhalb des Schwarzschild-Radius: r_t < r_s; für TDEs ist daher keine elektromagnetische Emission möglich und die Ereignisse sind nur durch Gravitationswellen (LISA) nachweisbar",
        answerD = "Der Gezeitenzerreißungsradius ist universell konstant bei r_t = 10³ AU, unabhängig von Stern- oder schwarzer Lochmasse; TDEs werden ausschließlich durch Röntgensatelliten detektiert und zeigen immer eine Periode von exakt 100 Tagen zwischen Zerriss und Akkretions-Peak",
        correctAnswer = 1, // B
        explanation = "Die Gezeitenkraft F_tidal ~ GM_BH R_star/r³ überwiegt die Selbstgravitation des Sterns GM_star/R_star², wenn r < r_t = R_star (M_BH/M_star)^{1/3}. Für ein 10⁶ M_☉-Schwarzes Loch und einen sonnenähnlichen Stern ergibt sich r_t ~ 1 AU — deutlich außerhalb des Schwarzschildradius. Nach dem Zerreißen kehrt ca. die Hälfte der Sternmaterie auf das schwarze Loch zurück; die Fallbackrate ṁ ~ t^{-5/3} (Rees 1988) ergibt sich aus der spezifischen Orbitalenergieverteilung der Trümmer. TDEs sind optisch, UV und in Röntgen sichtbar; ihre weiche Röntgenstrahlung und UV-Dominanz unterscheiden sie von Supernovae.",
        difficulty = 5,
        funFact = null
    ),

    // Question 15 – Ultra-High-Energy Cosmic Rays
    Question(
        categoryId = 2,
        questionText = "Ultra-hochenergetische kosmische Strahlung (UHECRs, E > 10¹⁸ eV) verliert Energie durch das GZK-Limit. Was ist das GZK-Limit physikalisch, und welche Konsequenz ergibt sich für die maximale Reichweite von UHECRs?",
        answerA = "Das GZK-Limit (Greisen-Zatsepin-Kuzmin) ist die Obergrenze der Energiedichte des Interstellarmediums; es begrenzt die Ausbreitung kosmischer Strahlung auf galaktische Distanzen < 3 kpc",
        answerB = "Das GZK-Limit resultiert aus der Photonenproduktion durch Wechselwirkung von UHECRs (hauptsächlich Protonen) mit CMB-Photonen: p + γ_CMB → Δ⁺ → p + π⁰ (oder n + π⁺); die Energieschwelle liegt bei E_GZK ~ 6×10¹⁹ eV; oberhalb dieser Energie verlieren Protonen schnell Energie und die Reichweite wird auf ~50–100 Mpc begrenzt",
        answerC = "Das GZK-Limit ist ein Quantengravitationseffekt, der die maximale Energie von Teilchen in der Lorentz-invarianten Raumzeit beschränkt; es entspricht der Planck-Energie und ist für alle Teilchenarten gleich",
        answerD = "Das GZK-Limit entsteht durch Comptonstreuung von UHECRs an Infrarot-Hintergrundphotonen (CIB); die Hauptreaktion ist e⁺e⁻-Paarerzeugung durch γ + γ_CIB → e⁺ + e⁻, die alle Gamma-Quanten über 100 TeV absorbiert",
        correctAnswer = 1, // B
        explanation = "Greisen (1966) und Zatsepin & Kuzmin (1966) berechneten unabhängig, dass Protonen mit E > 5×10¹⁹ eV effizient durch die CMB-Photonen gebremst werden: p + γ_CMB → Δ⁺(1232) → p + π⁰. Die Pionproduktionsschwelle liegt bei γ_p > 5×10¹⁹ eV/c². Die mittlere freie Weglänge ist ~6 Mpc, was bedeutet, dass UHECRs aus Distanzen > 100 Mpc ihren Flux stark reduzieren. Das Auger-Observatorium (Argentinien, 3000 km² Fläche) und Telescope Array (Utah) beobachten eine Unterdrückung bei E ~ 5×10¹⁹ eV — konsistent mit GZK, aber auch mit schwerer Kernzusammensetzung vereinbar.",
        difficulty = 5,
        funFact = "Das Pierre-Auger-Observatorium in Argentinien ist mit 3000 km² der größte Teilchendetektor der Welt. Es konnte zeigen, dass UHECRs bei höchsten Energien eher aus Quellen in der lokalen Großraumstruktur des Universums (< 100 Mpc) stammen — möglicherweise aktive Galaxienkerne."
    ),

    // ── BLOCK 4: Advanced Catalysis ──────────────────────────────────────────

    // Question 16 – Single-Atom Catalysis
    Question(
        categoryId = 2,
        questionText = "Einzelatom-Katalysatoren (SACs) bestehen aus isolierten Metallatomen auf Trägermaterialien. Welche elektronische Eigenschaft unterscheidet SACs fundamental von Nanoclustern, und welche Koordinationsumgebung ist für CO-Oxidation auf Fe₁/FeOₓ entscheidend?",
        answerA = "SACs haben kontinuierliche Bänder wie Metall-Bulk; auf FeOₓ sind Fe-Atome in 4-facher Koordination durch O und N bevorzugt, und die CO-Aktivierung erfolgt durch dissociative Chemisorption ähnlich wie auf Fe(110)-Oberflächen",
        answerB = "SACs besitzen quantisierte molekülorbitalähnliche Energieniveaus statt Bänder; auf FeOₓ sitzen Fe₁-Atome bevorzugt in Sauerstofflücken (5-fach koordiniert mit O), wo das Fe-Atom elektrophil ist und Sauerstoff aus Gitterpositionen aktiviert; die hohe Atomdispersion maximiert die katalytisch aktive Oberfläche bei minimalem Metalleinsatz",
        answerC = "SACs unterscheiden sich von Clustern durch ihre Spindichte: Einzelatome haben immer S=0, Cluster haben S>0; das CO-Oxidationsmechanism auf Fe₁/FeOₓ verläuft über Mars-van-Krevelen mit Fe²⁺/Fe³⁺-Redox-Zyklus ausschließlich bei Temperaturen > 400°C",
        answerD = "SACs sind durch ihre fehlende d-d-Überlappung charakterisiert, was zu 100× niedrigerer Aktivierungsenergie als Cluster führt; Fe₁/FeOₓ-SACs sind ausschließlich für Wassergas-Shift-Reaktionen aktiv und für CO-Oxidation praktisch inaktiv",
        correctAnswer = 1, // B
        explanation = "Im SAC-Regime fehlen die kollektiven metallischen Eigenschaften (Bänder, planare d-Orbitale, Ensembleeffekte). Stattdessen verhalten sich Einzelatome wie stark ligandenfeldgestörte Metallkomplexe. Auf FeOₓ bevorzugen Fe₁-Atome Substitutionspositionen in Sauerstoffvakanzumgebungen (Koordinationszahl 4-5 mit O). XANES/EXAFS-Charakterisierung zeigt typisch Fe(III)-artige Oxidationszustände. Der Mechanismus kann Mars-van-Krevelen-Charakter (Gitter-O-Transfer) oder Langmuir-Hinshelwood (beide Reaktanden adsorben) folgen. Zhang et al. (2014) zeigten, dass Pt₁/FeOₓ bei Raumtemperatur CO oxidiert — seither ist SAC-Forschung explodiert.",
        difficulty = 5,
        funFact = null
    ),

    // Question 17 – Enzymatic Catalysis: Serine Proteases
    Question(
        categoryId = 2,
        questionText = "Serinproteasen nutzen eine katalytische Triade (Ser-His-Asp). Welcher Schritt im Katalysemechanismus ist geschwindigkeitsbestimmend für unkatalysierte vs. enzymkatalysierte Hydrolyse, und welche Rolle spielt das Oxyanionloch?",
        answerA = "Bei der unkatalysierten Hydrolyse ist die Protonierung des Stickstoffs geschwindigkeitsbestimmend; das Oxyanionloch destabilisiert das negativ geladene Übergangszustand-Sauerstoffatom durch elektrostatische Abstoßung und erhöht so die Aktivierungsenergie",
        answerB = "Bei unkatalysierter Hydrolyse ist die nucleophile Addition des Wassers an die Carbonylgruppe langsam (tetraedrisches Intermediat ohne Stabilisierung); das Oxyanionloch (zwei NH-Gruppen des Proteinrückgrats) stabilisiert den tetraedrischen Übergangszustand durch H-Brücken an den negativen Carbonylsauerstoff und senkt ΔG‡ um ~10–20 kJ/mol",
        answerC = "Der geschwindigkeitsbestimmende Schritt ist bei Serinproteasen immer die Abgangsgruppenspaltung (Aminseite des Substrats); das Oxyanionloch bindet kovalent den Acyl-Enzym-Komplex und erhöht dessen Stabilität um Faktor 10³",
        answerD = "Die katalytische Triade funktioniert durch simultane Protonentransfer-Koordination: Asp deprotoniert His, das sofort Ser deprotoniert, das sofort das Substrat angreift; das Oxyanionloch ist für die Spaltung der Amid-Bindung irrelevant, da diese spontan nach der Acylierung verläuft",
        correctAnswer = 1, // B
        explanation = "Im zweistufigen Mechanismus: (1) Acylierung: Ser-OH greift nucleophil das Carbonyl an → tetraedrisches Intermediat (His übernimmt H von Ser, Asp stabilisiert His-Ladung per H-Brücke) → Acyl-Enzym + Aminogruppen-Abgang. (2) Deacylierung: H₂O greift das Acyl-Enzym an. Das Oxyanionloch (in Chymotrypsin: Backbone-NH von Gly193 und Ser195) stabilisiert den negativ-geladenen tetraedrischen Übergangszustand durch zwei spezifische H-Brücken zum Carbonylsauerstoff. Mutationen im Oxyanionloch (Gly193Ala) reduzieren k_cat um Faktor 10²-10³. Enzymkatalyse beschleunigt die Hydrolyse um Faktor ~10¹⁰ gegenüber unkatalysierter Reaktion.",
        difficulty = 5,
        funFact = "Das Oxyanionloch in Serinproteasen ist strukturell konserviert von Bakterien bis zum Menschen — ein Musterbeispiel für konvergente Evolution, da Subtilisin (Bakterien) und Chymotrypsin (Säugetiere) dieselbe Triade durch unabhängige Evolution entwickelten."
    ),

    // Question 18 – Photocatalytic Hydrogen Evolution
    Question(
        categoryId = 2,
        questionText = "Photokatalytische Wasserstoffentwicklung über Halbleiter erfordert das Überwinden mehrerer fundamentaler Limitierungen. Was ist das Shockley-Queisser-Limit für die Einzelhalbleiter-Effizienz, und warum sind Z-Schema-Systeme vorteilhafter für die Gesamtwassersspaltung?",
        answerA = "Das Shockley-Queisser-Limit beschränkt photokatalytische Effizienz auf 33,7 % aufgrund der Boltzmann-Statistik der Ladungsträgerrekombination; Z-Schema-Systeme verdoppeln die maximale Effizienz durch serielle Absorption zweier Photonen pro Reaktionsweg",
        answerB = "Ein Einzelhalbleiter benötigt eine Bandlücke von ~1,23 eV (thermodynamisches Minimum für H₂O-Spaltung) plus Überspannungen → optimale Bandlücke ~1,8–2,4 eV, was nur UV/vis-Photonen nutzt; Z-Schemata (zwei komplementäre Halbleiter verbunden über Redox-Mediator oder direkte Z-Schnittstelle) ermöglichen breite Solarspektrum-Nutzung, da jeder Halbleiter einen Teil des Spektrums absorbiert, während das Gesamt-Redoxpotential erhalten bleibt",
        answerC = "Das Shockley-Queisser-Limit gilt nicht für Photokatalyse, da keine p-n-Übergänge vorhanden sind; Z-Schema-Systeme sind vorteilhaft, weil zwei Halbleiter gemeinsam einen einzigen Proton kooperativ aktivieren können, was die Aktivierungsenergie auf null senkt",
        answerD = "Das Shockley-Queisser-Limit bestimmt den maximalen photokatalytischen Quantenwirkungsgrad zu 44,4 %; Z-Schema-Systeme können dieses Limit durch Mehrexzitonengeneration überwinden, da jedes Photon zwei Elektron-Loch-Paare erzeugt",
        correctAnswer = 1, // B
        explanation = "Ein Einzelhalbleiter für Gesamtwasserspaltung braucht: Leitungsband-Position negativer als H⁺/H₂ (0 V vs. NHE), Valenzband-Position positiver als O₂/H₂O (+1,23 V). Mit Überspannungen für HER (~0,2 V) und OER (~0,4 V) ist eine effektive Bandlücke > 1,8 eV nötig — begrenzt auf UV. Im Z-Schema (Honda, Domen) absorbiert Halbleiter 1 (PS I analog) im roten Bereich für die H₂-Halbreaktion und Halbleiter 2 (PS II analog) im blauen Bereich für die O₂-Halbreaktion. Der Redox-Mediator überträgt die Elektronen. Gesamtes Redoxpotential wird durch Addition der Einzelpotentiale erreicht. Die STH (solar-to-hydrogen) Effizienz von Z-Schemata übertrifft theoretisch Einzelhalbleiter erheblich.",
        difficulty = 5,
        funFact = null
    ),

    // Question 19 – Electrocatalytic Nitrogen Reduction
    Question(
        categoryId = 2,
        questionText = "Die elektrochemische Stickstoffreduktion (eNRR) zur Ammoniaksynthese bei Umgebungsbedingungen ist Gegenstand intensiver Forschung. Was ist die Hauptherausforderung der eNRR im Vergleich zur konkurrierenden Wasserstoffentwicklungsreaktion (HER)?",
        answerA = "Die Hauptherausforderung ist die extreme Überspannung für N₂-Adsorption auf allen bekannten Metalloberflächen; der Haber-Bosch-Prozess ist elektrochemisch genau reproduzierbar, wenn Elektrodenpotentiale > +2 V vs. SHE angelegt werden",
        answerB = "N₂ hat eine sehr starke Dreifachbindung (N≡N, 945 kJ/mol) und ist chemisch extrem inert; in wässrigen Elektrolyten konkurriert die eNRR mit der deutlich schnelleren HER um Protonen und Elektronen — die meisten Katalysatoren reduzieren bevorzugt H⁺ (FE für NH₃ oft < 1 %); außerdem ist die Kontamination durch Stickstoffverunreinigungen in Reagenzien ein massives Reproduzierbarkeitsproblem",
        answerC = "N₂-Reduktion ist elektrochemisch verboten wegen des Spin-Verbots (N₂ ist ein Singulett-Grundzustand, NH₃ ein Triplett); nur durch Photonenabsorption kann das Spin-Verbot aufgehoben werden, was eNRR ohne Lichtbestrahlung unmöglich macht",
        answerD = "Die Hauptherausforderung ist die benötigte extreme Alkalinität (pH > 14); da Ammoniak nur bei pH > 14 stabil ist, zerfällt es sofort in N₂ und H₂O in neutralen oder sauren Elektrolyten, was den Nachweis unmöglich macht",
        correctAnswer = 1, // B
        explanation = "N₂ ist mit D(N≡N) = 945 kJ/mol eine der stabilsten Verbindungen — die erste Elektronenübertragung (N₂ + e⁻ → N₂⁻) hat ein Standardpotential von -3,37 V vs. NHE, extrem ungünstig. Gleichzeitig läuft HER in protischer Lösung mit hoher Austauschstromdichte ab. Die Faradayische Effizienz (FE) für NH₃ ist auf den meisten Metallen < 1 %. Ein kritisches Problem wurde 2019 identifiziert: Stickstoffverunreinigungen in Reagenzien (N₂-Blasen, Aminverunreinigungen) führten zu falsch-positiven NH₃-Nachweisen — viele frühe Berichte von hohen FE sind nicht reproduzierbar. Ru-Komplexe, MoS₂ und Lithium-Vermittlung (LiPF₆/THF) zeigen vielversprechendere Selektivität.",
        difficulty = 5,
        funFact = "Ein bahnbrechender Ansatz verwendet Lithium als Shuttle: Li reduziert N₂ zu Li₃N (läuft spontan), das dann mit H₂O hydrolysiert wird. Dies trennt N₂-Aktivierung von Protonentransfer — und umgeht das HER-Konkurrenzproblem elegant."
    ),

    // Question 20 – MOF Catalysis
    Question(
        categoryId = 2,
        questionText = "Metallorganische Gerüstverbindungen (MOFs) als Katalysatoren nutzen ihre Porosität und Funktionalisierbarkeit. Welche katalytischen Aktivzentren sind in MOFs möglich, und wie ermöglicht die Confinement-Effekt in MOF-Poren eine Reaktionskontrolle?",
        answerA = "MOFs katalysieren ausschließlich durch ihre Metallknoten; der Confinement-Effekt erhöht die Temperatur im Poreninneren durch adiabatische Kompression von Reaktanden, was Reaktionen bei 300°C ermöglicht, die ohne MOF erst bei 500°C ablaufen",
        answerB = "Aktivzentren in MOFs umfassen: ungesättigte Metallkoordinationsstellen (open metal sites, z.B. Cu₂-Paddlewheel in HKUST-1), Linker-gebundene funktionelle Gruppen, eingebrachte Homogenkatalysatoren (post-synthetische Modifikation), und enkapsulierte Nanopartikel; der Confinement-Effekt in engen Poren erhöht die lokale Reaktandenkonzentration, erzwingt spezifische geometrische Übergangszustände und ermöglicht Größen-/Formselektivität analog zu Zeolithen",
        answerC = "MOFs sind als heterogene Katalysatoren nur bei tiefkryogenen Temperaturen (< -100°C) stabil, da die koordinativen Metallbindungen thermolabil sind; oberhalb von 0°C kollabieren MOF-Strukturen irreversibel und verlieren ihre katalytische Aktivität",
        answerD = "Ausschließlich die Linker-Moleküle bilden die Aktivzentren; die Metallknoten dienen nur der strukturellen Verankerung. Der Confinement-Effekt in MOFs ist nicht signifikant, da die typische Porengröße (1–3 nm) deutlich größer als Übergangszustände ist",
        correctAnswer = 1, // B
        explanation = "MOF-Katalyse nutzt: (1) Open metal sites: Nach Aktivierung (Desolvatation) sind Metallknoten wie Cu₂, Cr₃, Zr₆ koordinativ ungesättigt und können als Lewis-Säuren oder Redoxzentren wirken. (2) Linker-Funktionalisierung: Organische Linker mit NH₂, SO₃H oder Metallkomplexen (z.B. porphyrinische Linker mit Mn, Fe). (3) Post-synthetische Modifikation: Einbau von Organokatalysatoren oder Enzyme. Der Confinement-Effekt in sub-nm-Poren: Die Reaktanden werden in definierten Orientierungen fixiert (wie Enzyme), was Regioselektivität und Enantioselektivität ermöglicht. MIL-101(Cr) und UiO-66(Zr) gehören zu den stabilsten und meist-untersuchten MOF-Katalysatoren.",
        difficulty = 5,
        funFact = "UiO-66 (University of Oslo 2008) ist einer der stabilsten MOFs überhaupt — er übersteht Wasser, Säuren und Temperaturen bis 540°C. Sein Zr₆O₄(OH)₄-Cluster bindet 12 Linker und bildet ein außergewöhnlich robustes Gerüst, das inzwischen in Hunderten von Katalysestudien untersucht wurde."
    ),

    // ── BLOCK 5: Neurogenomics ───────────────────────────────────────────────

    // Question 21 – GWAS for Psychiatric Disorders
    Question(
        categoryId = 2,
        questionText = "Genomweite Assoziationsstudien (GWAS) für Schizophrenie haben über 270 signifikante Loci identifiziert. Was erklärt die 'missing heritability' in psychiatrischen GWAS, und was zeigt die hohe genomische SNP-Heritabilität h²_SNP von ~0,23 für Schizophrenie?",
        answerA = "Missing heritability entsteht ausschließlich durch seltene CNVs (> 500 kb), die nicht auf Standard-GWAS-Arrays erfasst werden; h²_SNP = 0,23 bedeutet, dass häufige SNPs 23 % der Bevölkerungsvarianz erklären, der Rest durch seltene Varianten erklärt wird",
        answerB = "Missing heritability resultiert aus mehreren Faktoren: unentdeckten seltenen Varianten (MAF < 0,1 %), Gen×Gen-Interaktionen (Epistasie), Gen×Umwelt-Interaktionen und unvollständigem LD der genotypisierten SNPs mit Kausalvarianten; h²_SNP ~ 0,23 für Schizophrenie zeigt, dass häufige SNPs substanzielle polygene Architektur erklären, obwohl die klinische Heritabilität (aus Zwillingsstudien) ~0,8 beträgt — die 'Lücke' ist real und methodisch bedingt",
        answerC = "Missing heritability ist ein statistisches Artefakt der GWAS-Korrekturen für multiple Tests (Bonferroni); würde man p < 0,01 statt p < 5×10⁻⁸ als Schwellenwert setzen, wären alle Varianten erklärt. h²_SNP = 0,23 entspricht der tatsächlichen biologischen Heritabilität ohne Umweltbeitrag",
        answerD = "Missing heritability existiert nicht für psychiatrische Erkrankungen: Die gesamte Heritabilität ist durch epigenetische Methylierungsmuster erklärt, die nicht in SNP-GWAS erfasst werden; h²_SNP = 0,23 ist ein Messfehler durch Populations-Stratifikation",
        correctAnswer = 1, // B
        explanation = "GWAS-Heritabilität h²_SNP (geschätzt via GCTA-GREML) misst den Anteil der phänotypischen Varianz, der durch alle genotypisierten SNPs gemeinsam erklärt wird — auch wenn die meisten einzelnen Effekte das Signifikanzlevel nicht erreichen. Für Schizophrenie: h²_SNP ~ 0,23, aber klinische Heritabilität (Zwillingsstudien) ~ 0,79. Die Lücke: (1) Seltene Varianten (MAF < 1 %) nicht gut getaggt; (2) Strukturelle Varianten (CNVs) separat; (3) Non-additive Effekte; (4) GxE-Interaktionen; (5) Fehler in klinischen Heritabilitätsschätzungen. Der PGC (Psychiatric Genomics Consortium) koordiniert Mega-GWAS mit > 500.000 Fällen und hat polygenische Risikoschätzungen (PRS) klinisch validiert.",
        difficulty = 5,
        funFact = null
    ),

    // Question 22 – Connectome Genetics
    Question(
        categoryId = 2,
        questionText = "Konnektom-Genetik untersucht den genetischen Einfluss auf Gehirnnetzwerk-Topologie. Was ergab die UK Biobank Konnektom-GWAS, und welcher methodische Ansatz ermöglicht die Verknüpfung von Netzwerktopologie mit genetischen Varianten?",
        answerA = "Die UK Biobank Konnektom-GWAS identifizierte ausschließlich Varianten im APOE-Gen; Netzwerktopologie wird durch einen einzigen polygenetischen Score erfasst, der direkt aus fMRI-Aktivierungsmustern extrahiert wird",
        answerB = "UK Biobank Konnektom-GWAS (> 30.000 Probanden mit Struktur-/Funktionell-MRT) identifizierte hunderte Loci für Traktographie-Maßnahmen und funktionelle Konnektivität (z.B. white matter microstructure FA/MD); der Ansatz verknüpft fMRI-abgeleitete ICA-Komponenten oder Graph-Theoretik-Metriken (Clustering, Pfadlänge) mit SNP-Daten via univariater GWAS oder multivariater GENIE-Methoden",
        answerC = "Konnektomgenetik zeigte, dass Hirnkonnektivität vollständig durch mitochondriale DNA bestimmt wird; nukleäre SNPs haben keinen signifikanten Einfluss (< 5 %) auf die Netzwerkarchitektur, was aus der matrilinearen Vererbung des Konnektoms geschlossen wurde",
        answerD = "Die UK Biobank GWAS für Konnektom-Phänotypen ist nicht möglich, da fMRI-Messungen zu starke Messungenauigkeiten haben (ICC < 0,1); genetische Konnektomstudien verwenden ausschließlich post-mortem-Daten mit Golgi-Färbung",
        correctAnswer = 1, // B
        explanation = "Smith et al. (2021) und nachfolgende UK Biobank GWAS-Studien analysierten strukturelle MRT (Diffusions-Traktographie: FA, MD, ICVF) und ruhezustandliche fMRT-Konnektivität von > 30.000 Probanden. Methodik: ICA (Independent Component Analysis) zerlegt fMRT in funktionelle Netzwerke; partielle Korrelationen bilden die 'functional connectivity matrix'; SNP-GWAS werden auf diese kontinuierlichen Phänotypen angewendet. Ergebnis: Zahlreiche Loci für White-Matter-Mikrostruktur (MBP, SEMA3 etc.), wenige für funktionelle Konnektivität (höheres Rauschen). Die genetische Korrelation zwischen Konnektom-Phänotypen und psychiatrischen Erkrankungen wurde quantifiziert (r_g > 0,3 für Schizophrenie-FA).",
        difficulty = 5,
        funFact = "Die UK Biobank plant, bis 2027 über 100.000 Hirnscans zu sammeln — damit wird sie die weltgrößte Neuroimaging-Genetik-Ressource, die gezielte genomweite Studien für subtile Gehirnnetzwerk-Phänotypen ermöglicht."
    ),

    // Question 23 – Single-Cell Transcriptomics in Brain
    Question(
        categoryId = 2,
        questionText = "Einzelzell-RNA-Sequenzierung (scRNA-seq) des Gehirns hat die Diversität von Neuronen und Gliazellen enthüllt. Welche technische Limitation betrifft Neurotransmitter-definierende Gene in scRNA-seq, und was hat die Allen Brain Cell Atlas scRNA-seq Studie für kortikale Zelltypen gezeigt?",
        answerA = "Die Hauptlimitation ist die geringe Sequenziertiefe: Neurotransmitter-Gene werden nur in > 50 % der Zellen detektiert, wenn min. 1 Million Reads pro Zelle sequenziert werden. Der Allen Atlas zeigte 5 kortikale Hauptzelltypen, alle mit bimodaler Genexpression",
        answerB = "Scherere in der Generfassung (Dropout-Ereignisse) durch niedrige mRNA-Kopienzahl führen dazu, dass niedrig-exprimierte Gene wie VGLUT1, VGLUT2 oder GAD1 in vielen Zellen als Null erscheinen (technisches Rauschen); der Allen Brain Cell Atlas (> 1 Million menschliche Kortex-Zellen) identifizierte > 100 transkriptomisch distinkte Zelltypen und Subtypen, inklusive seltener Zelltypen wie Chandelier-Zellen und Layer-1-Interneurone",
        answerC = "scRNA-seq kann keine myelinisierten Neuronen erfassen, da die lipidreiche Myelinscheide die RNA-Extraktion verhindert; daher zeigt der Allen Atlas ausschließlich Gliazell-Diversität, während Neuronendiversität durch FISH-Methoden separat erfasst wird",
        answerD = "Die Limitation ist das Splicing-Problem: scRNA-seq erfasst nur pre-mRNA, nicht gespleißte mRNA; dadurch erscheinen alle Neuronen-spezifischen Gene als Intron-enthaltende Vorläufer, was eine Klassifikation nach Neurotransmitter-Phänotyp unmöglich macht",
        correctAnswer = 1, // B
        explanation = "scRNA-seq (10x Genomics Droplet-Methode) erfasst pro Zelle typisch 1000–5000 Transkripte von ~20.000 möglichen Genen — das ist 'shallow sequencing'. Niedrig exprimierte Gene (< 5 Kopien/Zelle) werden häufig als 0 gemessen (dropout). Imputation-Methoden (MAGIC, scImpute) und tiefere Sequenzierung (Smart-seq2, > 500.000 Reads/Zelle) mildern das Problem. Der Allen Brain Cell Atlas (https://portal.brain-map.org) mit > 4 Millionen menschlichen und Maushirnzellen definierte eine hierarchische Zelltypentaxonomie: 24 Supertypen → 100+ Typen → hunderte Untertypen. Besonders überraschend: Viele Neuronen-Subtypen waren zuvor histologisch nicht unterscheidbar.",
        difficulty = 5,
        funFact = null
    ),

    // Question 24 – Spatial Transcriptomics
    Question(
        categoryId = 2,
        questionText = "Räumliche Transkriptomik kombiniert Genexpressionsprofile mit Gewebepositionen. Welche technischen Ansätze ermöglichen Einzelzellauflösung bei räumlicher Transkriptomik, und was zeigten MERFISH/seqFISH+ Studien im Mausgehirn?",
        answerA = "Einzelzellauflösung bei räumlicher Transkriptomik wird durch FACS-Sortierung mit anschließender Koordinatenmarkierung erreicht; MERFISH-Studien am Mausgehirn zeigten ausschließlich Astrozyten-Diversität, da Neuronensignale durch Myelin blockiert werden",
        answerB = "MERFISH (Multiplexed Error-Robust FISH) und seqFISH+ nutzen sequentielle FISH-Hybridisierungsrunden mit binären Barcodes, um hunderte Gene simultan mit subzellulärer Auflösung in situ zu messen; Studien am Mausgehirn (Zhuang-Labor, Harvard) kartierten über 10.000 Zellen in Hypothalamus und Cortex, zeigten räumliche Zelltyp-Gradienten, neuroanatomisch-transkriptomische Korrelationen und ermöglichten die Kartierung von Zelltypgrenzen ohne apriori-Wissen über Hirnregionen",
        answerC = "Einzelzellauflösung in räumlicher Transkriptomik wird nur durch Electron-Microscopy-korrelierte RNA-Seq (EM-FISH) erreicht; MERFISH-Studien zeigten, dass Zelltyp-Identität ausschließlich durch Zellgröße bestimmt wird, ohne molekulare Heterogenität",
        answerD = "MERFISH und seqFISH+ messen Proteome, nicht Transkriptome, durch antikörperbasierte Proximity Ligation Assays mit DNA-Barcodes; räumliche Proteinverteilungen im Mausgehirn zeigten, dass alle Neuronen-Subtypen durch einzige Protein-Konzentrationsgradienten definiert werden",
        correctAnswer = 1, // B
        explanation = "MERFISH (Chen et al., Zhuang Lab 2015) nutzt sequentielle smFISH-Runden mit Fehlerkorrektur-Codes: Jedes Gen bekommt einen binären 16-Bit-Barcode; über 16 Hybridisierungsrunden wird der Barcode abgelesen. Einzelmoleküle werden fluoreszenz-mikroskopisch lokalisiert. seqFISH+ (Cai Lab, Caltech) erreicht > 10.000 Gene simultan. Mausgehirn-Studien (Moffitt et al., Science 2018, Hypothalamus): 73 distinkte Neuronen-Subtypen mit spezifischen anatomischen Lagen; Zell-Cluster, die im scRNA-seq nicht räumlich interpretierbar waren, zeigen nun klare topographische Organisation. Visium (10x Genomics), Slide-seq und HDST sind arraybasierte Ansätze mit niedrigerer Auflösung (~ 10 µm), aber hohem Durchsatz.",
        difficulty = 5,
        funFact = "seqFISH+ kann über 10.000 Gene simultan in einem einzelnen Gewebeschnitt messen — damit ist das gesamte proteinkodierendes Transkriptom einer Zelle in situ zugänglich, ohne die räumliche Information zu verlieren."
    ),

    // Question 25 – Optogenetic Circuit Mapping
    Question(
        categoryId = 2,
        questionText = "Optogenetische Schaltkreiskartierung nutzt Lichtaktivierung, um neuronale Schaltkreise zu disseziieren. Welche molekularen Eigenschaften der Kanalrhodopsine ChR2 und ChRmine unterscheiden sich, und wie ermöglicht Cre-abhängige virale Injektion zelltyp-spezifische Kontrolle?",
        answerA = "ChR2 und ChRmine sind identische Kanalproteine mit unterschiedlicher Wellenlängenempfindlichkeit; Cre-abhängige Viren verwenden ein FLEX-SWITCH-Prinzip, bei dem der Promotor von Cre-Rekombinase invertiert und damit stillgelegt wird, sodass nur Cre-negative Zellen das Opsin exprimieren",
        answerB = "ChR2 (Blaulicht, λmax~470 nm, Kanalkinetik ~10 ms) eignet sich für präzise Spike-Kontrolle bei niedrigen Lichtintensitäten; ChRmine (Rotlicht, λmax~585 nm, aus Rhodopsin-3, gruppen-1-Pump) ermöglicht tiefere Gewebeexzitation wegen reduzierter Lichtstreuung und ist sensitiver; Cre-abhängige (FLEX/DIO) AAV-Vektoren enthalten das Opsin-Gen in invertierter Orientierung, flankiert von inversen loxP-Sites — nur in Cre-exprimierenden Zellen wird das Opsin invertiert und transkribiert",
        answerC = "ChR2 ist ein exzitatorisches Opsin; ChRmine ist ein inhibitorisches Chloridkanal, das Neuronen bei Blaulicht hyperpolarisiert; Cre-abhängige Viren aktivieren ausschließlich glutamaterge Neuronen, unabhängig von der Promotorspezifität",
        answerD = "ChR2 und ChRmine unterscheiden sich ausschließlich in der Proteingröße (ChR2: 737 AA, ChRmine: 248 AA); Cre-abhängige Injektion erfolgt durch retrograde Viren, die alle Neuronen infizieren, die synaptisch mit dem injizierten Zielgebiet verbunden sind",
        correctAnswer = 1, // B
        explanation = "ChR2 (Nagel 2003) ist ein Kation-Kanal (Na⁺, K⁺, H⁺, Ca²⁺) mit λmax 470 nm und Off-Kinetik τ ~ 10 ms → limitiert auf ~20 Hz Spike-Fidelity. ChRmine (Kim et al. 2022, Deisseroth Lab) ist ein Rhodomonas-abgeleitetes Kanalrhodopsin mit λmax 585 nm und hoher Photostrom-Dichte: tieferes Eindringen in Gewebe (rot), kompatibel mit Miniscopen. FLEX/DIO-AAV: Das Transgen ist von zwei Paaren invers-orientierter lox-Sites (loxP + lox2722) flankiert und antisense zur Transkriptionsrichtung. In Cre+ Zellen katalysiert Cre-Rekombinase zwei Inversionen → Transgen steht sense → Expression. Standard-Cre-Linien (CaMKII-Cre, PV-Cre, SOM-Cre) ermöglichen subtypspezifische Kontrolle.",
        difficulty = 5,
        funFact = "Karl Deisseroth (Stanford) und Ed Boyden (MIT) erhielten für die Optogenetik zahlreiche Preise — darunter den Lasker Award 2021. Die Methode wird als eine der einflussreichsten neurowissenschaftlichen Entwicklungen des 21. Jahrhunderts eingestuft, da sie Kausalität in Schaltkreisen beweisbar macht."
    ),

    // ── BLOCK 6: Planetary Interiors ─────────────────────────────────────────

    // Question 26 – Jupiter's Metallic Hydrogen
    Question(
        categoryId = 2,
        questionText = "Im Inneren Jupiters wird bei Drücken > 1–3 Mbar flüssig-metallischer Wasserstoff vorausgesagt. Welcher Quantenmechanismus erzeugt den metallischen Zustand, und welchen Bezug hat dieser Zustand zum Magnetfeld Jupiters?",
        answerA = "Metallischer Wasserstoff entsteht durch Dissoziation von H₂ zu atomarem H unter Druck, gefolgt von ionischer Kristallisation; das Magnetfeld wird durch piezoelektrische Effekte in der kristallinen H-Schicht erzeugt",
        answerB = "Bei P > 1–3 Mbar überlappt die 1s-Wellenfunktion benachbarter Wasserstoffatome so stark, dass das elektronische Band dispergiert und die verbotene Zone (Bandlücke) auf null abfällt — metallische Leitfähigkeit entsteht; die konvektive Durchmischung des elektrisch leitfähigen flüssig-metallischen H-He-Gemisches erzeugt über einen Geodynamo das starke Magnetfeld Jupiters (B ≈ 10× Erdfeld)",
        answerC = "Metallischer Wasserstoff bei Jupiter entsteht ausschließlich durch Hochtemperatur-Plasma-Ionisierung (T > 10⁵ K); die Leitfähigkeit ist temperatur- und nicht druckbedingt. Das Magnetfeld wird durch den zirkular-polarisierten Infrarot-Strahlungsdruck auf ionisiertes Helium erzeugt",
        answerD = "Jupiter zeigt keinen metallischen Wasserstoff — der Übergang existiert nur theoretisch. Jupiters Magnetfeld wird durch einen festen Eisenkern analog zur Erde erzeugt; dieser Kern hat eine Masse von ~15 Erdmassen und eine Temperatur > 50.000 K",
        correctAnswer = 1, // B
        explanation = "Wigner & Huntington (1935) sagten metallischen Wasserstoff bei P ~ 25 GPa vorher — experimentell noch nicht stabil erzeugt (Dias 2017 Behauptung zurückgezogen). Im Jupiter-Inneren (P > 100 GPa, T > 10.000 K) ist H₂ dissoziiert und die Elektronen sind delokalisiert. Die Juno-Mission (NASA, 2016–) misst Jupiters Gravitationsfeld und Magnetfeld präzise: Das Magnetfeld ist komplex (nicht-dipolär), was auf einen tieferen Dynamo-Bereich hinweist. Die Leitfähigkeit σ_metallic-H ~ 10⁶ S/m bei 1 Mbar (Laser-Schockexperimente) ist gut etabliert. Der Übergang von molekularem zu metallischem H ist wahrscheinlich kontinuierlich (kein scharfer Phasenübergang) in Jupiters Druckregime.",
        difficulty = 5,
        funFact = null
    ),

    // Question 27 – Saturn's Helium Rain
    Question(
        categoryId = 2,
        questionText = "Saturns interne Wärmequelle übersteigt seine solare Einstrahlung. Welches Phänomen erklärt diesen Energieüberschuss, und wie beeinflusst der Heliumregen Saturns Magnetfeld im Vergleich zu Jupiter?",
        answerA = "Saturns Überschusswärme entsteht durch radioaktiven Zerfall von ⁴⁰K im felsigen Kern; Heliumregen hat keine Bedeutung, da Helium in Saturns Innenbereich vollständig mit Wasserstoff mischibel ist",
        answerB = "Bei Saturns tieferen Temperaturen (verglichen mit Jupiter) entmischt sich Helium aus metallischem Wasserstoff (Phasentrennung) und fällt als Heliumtröpfchen gravitativ nach innen — dabei wird Gravitationsenergie in Wärme umgewandelt (Kelvin-Helmholtz-Prozess); gleichzeitig reduziert der Heliumregenentzug die elektrische Leitfähigkeit der äußeren metallischen H-Schicht und erklärt Saturns auffällig axialsymmetrisches, schwächeres Magnetfeld (B_sat ~ 0,2 × B_earth)",
        answerC = "Saturns Überschusswärme stammt aus Gezeitenreibung mit Titan; Heliumregen erhöht die Konvektionsrate im flüssig-metallischen H-Bereich und führt zu Saturns hochvariablem Magnetfeld mit täglichen Schwankungen von > 10 %",
        answerD = "Heliumregen erhöht die effektive Viskosität von Saturns innerem Kern, was Konvektion unterdrückt und zur Wärmestauung führt; das axiale Magnetfeld Saturns ist stärker als das Jupiters, weil der kompaktere Kern einen effizienteren Dynamo antreibt",
        correctAnswer = 1, // B
        explanation = "Stevenson & Salpeter (1977) und später Lorenzen et al. (2011, 2018) zeigten durch ab-initio-Rechnungen: In Jupiters heißerem Inneren bleibt He-H miscibel; bei Saturns kälterem Regime (gleiche Drücke, aber T um ~3000 K kühler) unterschreitet das System die Mischungsgrenze → He-Entmischung und gravitatives Absinken. CASSINI-Messungen der Helium-Häufigkeit in Saturns Atmosphäre (Y_He ~ 0,18-0,25 vs. solares Y_He ~ 0,28) bestätigen He-Verarmung in der Atmosphäre durch Regen ins Innere. Das axiale Magnetfeld (Sn' = 0 für n ≥ 2 im Dipolterm) ist möglicherweise durch eine differentielle Rotation und He-Entmischungsschicht axisymmetrisiert.",
        difficulty = 5,
        funFact = "Saturns Magnetfeld ist das am stärksten achsensymmetrische Planetenmagnetfeld im Sonnensystem — die Neigung zwischen magnetischer und Rotationsachse beträgt weniger als 0,01°, was nach dem Cowling-Theorem ein reines Dipol-Dynamo unmöglich machen würde."
    ),

    // Question 28 – Mars Core Composition
    Question(
        categoryId = 2,
        questionText = "Die InSight-Mission hat Seismologie zur Bestimmung des Marsinneren eingesetzt. Welche unerwarteten Ergebnisse zur Kernzusammensetzung lieferten die Marsquake-Daten, und was impliziert ein flüssiger äußerer Kern für Marsmagnetsgeschichte?",
        answerA = "InSight zeigte, dass der Marskern vollständig fest und aus reinem Eisen besteht; der feste Kern erklärt das fehlende aktive Magnetfeld, da Konvektion im festen Kern unmöglich ist",
        answerB = "InSight-Seismologie (SEIS-Instrument) ergab einen flüssigen äußeren Kern mit Radius ~1830 km und deutlich geringerer Dichte als erwartet — was auf einen hohen Anteil an leichten Elementen (H, C, O, S) hindeutet (~20 Gew% S + weitere leichte Elemente); ein ehemals aktiver Dynamo (vor ~3,9 Ga) erklärt die krustalen Remanenzmagnetisierungen, der heutige erloschene Dynamo wahrscheinlich durch Abkühlung des Kerns",
        answerC = "InSight fand einen unerwarteten festen inneren Kern aus Eisensulfid (FeS) mit Radius 900 km, umgeben von einem geschmolzenen Fe-Ni-Mantel; das aktuelle Magnetfeld von Mars (B ~ 10 nT) wird durch Remanenz im festen FeS-Kern erklärt",
        answerD = "InSight konnte keinen Kern detektieren, weil zu wenige Marsquakes für Kernphasen-Analyse aufgezeichnet wurden; die Kernzusammensetzung basiert ausschließlich auf Meteoritenanalyse (Shergottites), die einen reinen Fe-Ni-Kern mit S < 2 Gew% zeigt",
        correctAnswer = 1, // B
        explanation = "Stähler et al. (Science, 2021) analysierten P-Wellenphasen (ScS, Kernreflexionen) von Marsquakes bis M5: Kernradius 1830±40 km, konsistent mit einem vollständig flüssigen Außenkern. Die Kerndichte (6.0–6.7 g/cm³) ist signifikant niedriger als für reines Fe oder Fe-Ni erwartet (~7.5 g/cm³) — Schwefel allein reicht nicht aus, weitere leichte Elemente (H, C, O) müssen vorhanden sein. Allerdings ergaben 2023 neue Analysen (van Driel, Khan) möglicherweise einen festen inneren Kern oder eine Re-interpretation der Daten. Die Marsquake-Kruste ist ~20-25 km dick im Messgebiet. Das Fehlen eines aktiven Dynamos bei vorhandenem flüssigem Kern ist rätselhaft — möglicherweise fehlt der Wärmefluss aus dem Kern für Konvektion.",
        difficulty = 5,
        funFact = "InSight's SEIS-Seismometer ist empfindlich genug, um Marsquakes der Magnitude 1 zu messen. Das Instrument muss vor Wind geschützt werden — Windgeschwindigkeit von 5 m/s erzeugt Störsignal vergleichbar mit einem M2-Erdbeben auf der Erde."
    ),

    // Question 29 – Venus Internal Dynamics
    Question(
        categoryId = 2,
        questionText = "Venus und Erde haben ähnliche Größe und Dichte, aber Venus fehlt eine Plattentektonik. Welches Modell erklärt Veneras Oberfläche als relativ jung (~500 Ma), und warum hat Venus kein starkes Magnetfeld?",
        answerA = "Venera-Oberfläche ist jung durch kontinuierliche Plattensubduktion analog zur Erde; Venus fehlt ein Magnetfeld wegen des langsameren Rotationsdynamos — aber MESSENGER-Messungen zeigten ein Restfeld von ~1 µT",
        answerB = "Das 'episodische Wiederverkrustungs'-Modell postuliert, dass Venus Phasen globaler Vulkan-Überflutung alle ~500–700 Ma durchläuft (Mantel-Overturn): Die Lithosphäre verdickt sich, bis sie kataklastisch absinkt und eine globale vulkanische Erneuerung auslöst; fehlendes Magnetfeld erklärt sich durch die sehr langsame Rotation Venusses (243 Erdtage/Venustag) und möglicherweise keinen inneren festen Kern — beides reduziert Dynamo-Effizienz",
        answerC = "Veneras junge Oberfläche entsteht durch Impact-Resurfacing: Jede ~500 Ma trifft ein Mars-großer Impakt Venus und schmilzt die Oberfläche komplett; das fehlende Magnetfeld ist durch die hohe CO₂-Konzentration der Atmosphäre erklärt, die magnetische Flusslinien absorbiert",
        answerD = "Venus hat kontinuierliche Plattentektonik (erkennbar an linksgerichteten Riftgrabens auf der Oberfläche), aber die Subduktionszonen sind im Radar nicht sichtbar wegen der dichten SO₂-Bewölkung; das Magnetfeld Venusses hat B ~ 0,001 Erdfeld und wurde von VENUS EXPRESS gemessen",
        correctAnswer = 1, // B
        explanation = "Das 'Stagnant lid'-Regime (im Gegensatz zur mobilen Plattentektonik der Erde) mit periodischem Mantel-Overturn (Turcotte 1993, Moresi & Solomatov 1998) erklärt die einheitliche Oberfläche (~94 % < 1 Ga, aber kein klares Muster wie Plattenstruktur). MAGELLAN-Radar-Altimetrie zeigte Tesserae (alte deformierte Terrain), Coronae (vulkanische Strukturen) und global verteilte Vulkane. Veneras langsame Rotation (Ωvenus ~ 0,004 Ωerde) und möglicherweise fehlender innerer Kern unterdrücken den Geodynamo. EnVision (ESA) und VERITAS (NASA, geplant) werden Veneras Innenleben durch Seismologie und Gravimetrie enthüllen.",
        difficulty = 5,
        funFact = null
    ),

    // Question 30 – Mercury's Large Core
    Question(
        categoryId = 2,
        questionText = "Merkur hat einen ungewöhnlich großen Eisenkern (~ 85 % des Planetenradius). Welche Entstehungsmodelle wurden vorgeschlagen, und was zeigten MESSENGER-Magnetfelddaten über Merkurs Dynamo-Eigenschaften?",
        answerA = "Merkurs großer Kern entstand durch selektive Eisenakkumulation aus dem Sonnenwind; MESSENGER zeigte kein Magnetfeld — Merkurs Kern ist vollständig fest und magnetisch inaktiv",
        answerB = "Konkurrierende Modelle: (1) Ärodinamic Fractionation: Im frühen Sonnensystem verdampfte silikatreiches Material durch intensive Sonneneinstrahlung; (2) Giant Impact: Kollision entfernte den Großteil des Mantels; (3) Differenzierter Akkretionsprozess im inneren Sonnensystem. MESSENGER (2011-2015) detektierte ein intrinsisches Dipolfeld (B ~ 1 % Erdfeld), das axial und polhochgelegen ist (höhere Stärke in Nordpolarregion) — erklärt durch einen Dynamo mit tiefliegendem äußerem Flüssigkern und stabiler Dichteschichtung ('flux imprisonment'-Modell)",
        answerC = "Merkurs Kern entstand aus einem unterschiedlich zusammengesetzten Vorläufer-Planetesimal (chondritisch Si, nicht Fe); der große Fe-Kern ist ein Messfehler, basierend auf zu geringer Kerndichte-Schätzung — MESSENGER zeigte, dass der Kern aus FeS besteht (dichte ~5 g/cm³)",
        answerD = "MESSENGER-Daten zeigten Merkurs Magnetfeld als rein externen (induziertes) Feld durch Solaren-Wind; kein Dynamo ist aktiv, da Merkurs Kern bei 3000 K unter der Erstarrungstemperatur von Fe-S-Legierungen liegt",
        correctAnswer = 1, // B
        explanation = "Merkurs Kern-Mantel-Radius-Verhältnis ist ~0,83 (vs. Erde ~0,55). Drei Entstehungsmodelle: (1) Vaporisierung durch Sonnennähe (Cameron 1985): Frühe intensive Sonnenphase verdampfte silikatischen Mantel; (2) Giant Impact (Benz et al. 1988): Kollision entfernte Mantel; (3) Aerodynamische Fraktionierung (Weidenschilling 1978): Eisenteilchen konzentrierten sich im inneren Sonnensystem. MESSENGER (Anderson et al. 2011, Science) zeigte: Dipolfeld ~300 nT (vs. Erde 30.000 nT), stark achsensymmetrisch aber polhochgelegen. Stevenson's 'flux expulsion' Modell: Stabile Dichteschichtung im äußeren Kern hindert Konvektion im Äquatorbereich, konzentriert Dynamo-Aktivität nahe dem inneren Kern. BepiColombo (2025 Ankunft) wird detailliertes Magnetfeld-Kartierung liefern.",
        difficulty = 5,
        funFact = "Merkurs Kern hat eine Dichte, die auf einen signifikanten Schwefelanteil (~ 6 Gew% S) hindeutet — S senkt den Schmelzpunkt von Eisen erheblich und erklärt, warum Merkurs Kern trotz seiner Größe und Abkühlung noch flüssig-extern ist."
    ),

    // ── BLOCK 7: Non-Equilibrium Thermodynamics ──────────────────────────────

    // Question 31 – Fluctuation Theorem
    Question(
        categoryId = 2,
        questionText = "Das Fluktuationstheorem von Evans, Cohen und Morriss (1993) macht eine präzise Aussage über Entropieproduktion in kleinen Systemen. Was besagt es mathematisch, und wie verhält es sich zum zweiten Hauptsatz?",
        answerA = "Das Fluktuationstheorem besagt, dass in Systemen unter N Teilchen die Entropieproduktion negativ werden muss; für N > 10²³ gilt der zweite Hauptsatz wieder. Die mathematische Aussage: P(S > 0)/P(S < 0) = exp(-S/k_B)",
        answerB = "Das Evans-Cohen-Morriss-Fluktuationstheorem (FT): P(Σ = A) / P(Σ = -A) = exp(A·τ), wobei Σ die mittlere Entropieproduktionsrate und τ die Beobachtungsdauer ist; es zeigt, dass negative Entropiefluktuationen möglich (aber exponentiell unterdrückt) sind — im Grenzfall τ → ∞ oder großer Systeme wird der zweite Hauptsatz im statistischen Mittel garantiert",
        answerC = "Das Fluktuationstheorem ersetzt den zweiten Hauptsatz für nanoskalige Systeme vollständig; es besagt: ΔS_total kann nur positive Werte annehmen, und Fluktuationen unter 10⁻²³ J/K sind physikalisch verboten",
        answerD = "Das Fluktuationstheorem gilt nur im Gleichgewicht und reduziert sich auf die Fluktuations-Dissipations-Beziehung; für nicht-gleichgewichts Systeme ist das Theorem nicht anwendbar, was durch Verletzungen in optischen-Falle-Experimenten gezeigt wurde",
        correctAnswer = 1, // B
        explanation = "Das transient FT (Evans et al.) und das stationäre FT (Gallavotti-Cohen 1995) besagen: Das Verhältnis der Wahrscheinlichkeit einer bestimmten Entropieproduktionsrate A zur negativen Rate -A wächst exponentiell mit A·τ. Dies ist eine exakte Aussage — keine Näherung. Konsequenz: Für kleine Systeme (Nanoteilchen, einzelne Moleküle) und kurze Zeiten sind Verletzungen des zweiten Hauptsatzes möglich; die Wahrscheinlichkeit ist exp(-A·τ). Wang et al. (2002) bestätigten das FT in einem optischen-Falle-Experiment mit Kolloidal-Partikeln. Das FT impliziert: Im Mittel bleibt ⟨ΔS⟩ ≥ 0 (zweiter Hauptsatz), aber Fluktuationen gegen ihn sind messbar.",
        difficulty = 5,
        funFact = "Das Fluktuationstheorem war die erste strenge mathematische Aussage über negative Entropiefluktuationen und hat gezeigt, dass der zweite Hauptsatz ein statistischer, kein absoluter Satz ist — eine fundamentale Erkenntnis für Biophysik und Nanomaschinerie."
    ),

    // Question 32 – Jarzynski Equality
    Question(
        categoryId = 2,
        questionText = "Die Jarzynski-Gleichung verknüpft nichtgleichgewichts Arbeit mit gleichgewichts freier Energie. Was ist die exakte mathematische Form, und welche Voraussetzungen gelten für ihre Anwendbarkeit?",
        answerA = "Die Jarzynski-Gleichung lautet ΔF = ⟨W⟩ und gilt nur für quasi-statische Prozesse; für schnelle Prozesse gilt stattdessen ΔF < ⟨W⟩ (Clausius-Ungleichung), weshalb die Jarzynski-Gleichung keine Verbesserung über die klassische Thermodynamik bietet",
        answerB = "⟨exp(-βW)⟩ = exp(-βΔF), wobei β = 1/(k_BT), W die in einem einzelnen nichtgleichgewichts Experiment geleistete Arbeit ist und der Erwartungswert über viele Wiederholungen gebildet wird; gilt für beliebig schnelle Prozesse bei konstantem T, solange das System anfangs im kanonischen Gleichgewicht ist und am Ende dieselbe Kontrollvariable-Endposition hat",
        answerC = "Jarzynskis Gleichung lautet P(W > ΔF) = exp(-ΔF/k_BT) und gilt nur für lineare Systeme nahe dem Gleichgewicht; für biologische Systeme (Proteine, Motoren) ist sie nicht anwendbar, da nicht-harmonische Potentiale die Berechnung unmöglich machen",
        answerD = "Die Jarzynski-Gleichung gilt nur im Grenzfall unendlicher Systemgröße (N → ∞) und reduziert sich dann zu ΔF = ⟨W⟩_q.s. ; für finite Systeme gibt es eine Korrekturgröße von Ordnung 1/N, die in biologischen Experimenten immer dominiert",
        correctAnswer = 1, // B
        explanation = "Jarzynski (1997): ⟨e^{-βW}⟩ = e^{-βΔF}. Dies ist bemerkenswert: Aus vielen irreversiblen Nicht-Gleichgewichtsmessungen der Arbeit W kann die Gleichgewichts-Freie-Energie-Differenz ΔF exakt bestimmt werden. Die Jensen-Ungleichung e^{⟨x⟩} ≤ ⟨e^x⟩ ergibt ΔF ≤ ⟨W⟩ — der zweite Hauptsatz folgt direkt. Voraussetzungen: (1) Isothermische Kopplung an Wärmebad konstanter Temperatur T; (2) Anfangszustand im Gleichgewicht; (3) korrekte statistische Stichprobennahme (Rare-Event-Problem bei weiten Auslenkungen). Liphardt et al. (Science 2002) nutzten Jarzynski für RNA-Faltungs-ΔG-Messungen mit optischer Pinzette.",
        difficulty = 5,
        funFact = null
    ),

    // Question 33 – Onsager Reciprocal Relations
    Question(
        categoryId = 2,
        questionText = "Onsagers reziproke Relationen (1931) sind ein fundamentales Ergebnis der linearen Nicht-Gleichgewichts-Thermodynamik. Auf welchem mikroskopischen Prinzip basieren sie, und welche praktische Konsequenz haben sie für thermoelektrische Effekte?",
        answerA = "Onsager-Relationen basieren auf der Quantenmechanik des Spin-Bahn-Kopplung; ihre Konsequenz für Thermoelektrik ist die Identität von Seebeck- und Peltier-Koeffizient, die aus der Wellenfunktions-Unitarität folgt",
        answerB = "Onsager-Relationen beruhen auf dem Prinzip der mikroskopischen Zeitumkehrsymmetrie (detailed balance): Im Gleichgewicht sind Vorwärts- und Rückwärts-Mikrofluktuationen gleich häufig; daraus folgt für die phänomenologischen Koeffizienten L_{ij} = L_{ji}; für Thermoelektrik: Peltier-Koeffizient Π = T · S (Seebeck-Koeffizient), verbindet Wärmetransport durch Strom mit elektromotorischer Kraft durch Temperaturgradient",
        answerC = "Onsager-Relationen folgen aus dem Extremalprinzip der maximalen Entropieproduktion; ihre Konsequenz ist, dass alle Kreuzkoeffizienten verschwinden (L_{ij} = 0 für i ≠ j) und Effekte nur als lineare Kombination gleichnamiger Triebkräfte und Flüsse auftreten",
        answerD = "Onsager-Relationen gelten nur für flüssige Elektrolyte und wurden für Thermoelektrik nie experimentell bestätigt; Kelvin bewies bereits 1850, dass Π ≠ T·S für metallische Leiter gilt, was die Onsager-Theorie widerlegt",
        correctAnswer = 1, // B
        explanation = "Onsagers Beweis: Im Gleichgewicht gilt ⟨α_i(0) α_j(τ)⟩ = ⟨α_j(0) α_i(τ)⟩ (time-reversal symmetry). Da phänomenologische Koeffizienten aus diesen Korrelationsfunktionen folgen (Green-Kubo), gilt L_{ij} = L_{ji}. Die Kelvin-Relationen der Thermoelektrik folgen: Seebeck-Koeffizient S_AB (thermoelektrische EMK pro Temperaturgradient) und Peltier-Koeffizient Π_AB (Wärmestrom pro elektrischem Strom) sind durch Π_AB = T · S_AB verbunden. Dies war vor Onsager nur phänomenologisch bekannt — Onsager gab die mikroskopische Begründung. Nobel-Preis für Onsager 1968 (für irreversible Thermodynamik).",
        difficulty = 5,
        funFact = "Onsager erhielt den Chemie-Nobelpreis 1968 fast 37 Jahre nach seinen grundlegenden Arbeiten von 1931 — eines der längsten Zeitintervalle zwischen Entdeckung und Nobelpreis in der Geschichte."
    ),

    // Question 34 – Dissipative Structures
    Question(
        categoryId = 2,
        questionText = "Ilya Prigogines Konzept der dissipativen Strukturen erklärt die Selbstorganisation in weit-vom-Gleichgewicht-Systemen. Was ist die Bedingung für das Entstehen dissipativer Strukturen, und welches chemische System dient als kanonisches Beispiel?",
        answerA = "Dissipative Strukturen entstehen, wenn die Energiezufuhr exakt der Entropieproduktion entspricht (ΔG_input = TΔS_internal); die Belousov-Zhabotinsky-Reaktion ist das kanonische Beispiel und zeigt stehende Konzentrationsoszillationen ohne Ausbreitung",
        answerB = "Dissipative Strukturen entstehen in offenen Systemen fern vom Gleichgewicht, wenn nichtlineare Reaktionen und Diffusion kooperieren und das System einen Bifurkationspunkt überschreitet — jenseits dem die homogene stationäre Lösung instabil wird; die Belousov-Zhabotinsky-Reaktion (BZ, Bromat-Malonat-Cerium-Katalysator) zeigt Turing-ähnliche raum-zeitliche Muster, spiralförmige Wellenfronten und Zeitoszillationen",
        answerC = "Dissipative Strukturen sind ausschließlich auf biologische Systeme beschränkt; in rein chemischen Systemen führen Nicht-Gleichgewicht-Zustände immer zur Maximalentropie ohne Strukturbildung. Die BZ-Reaktion ist kein dissipatives System, sondern ein reversibler Gleichgewichts-Oszillator",
        answerD = "Die Bedingung für dissipative Strukturen ist negative Wärmekapazität des Reaktionsgemisches; Prigogines Theorie gilt nur bei T < 0 K (Spintemperatur in kernmagnetischen Resonanzsystemen) und ist auf klassische Thermodynamik nicht übertragbar",
        correctAnswer = 1, // B
        explanation = "Prigogine (Nobel 1977) zeigte: In offenen Systemen (Materie/Energie-Austausch mit Umgebung) kann Entropie lokal abnehmen (ΔS_intern < 0, wenn Zufuhr > Produktion). Bedingungen: Offenes System, fern vom Gleichgewicht, nichtlineare Reaktionen, Bifurkationsmechanismus. BZ-Reaktion: Gesamtreaktion ist Bromierung von Malonat durch Bromat; die Inhibitor/Aktivator-Nichtlinearität (Ce³⁺/Ce⁴⁺-Redox) führt zu Hopf-Bifurkation → Zeitoszillationen in RHR (rührendem Reaktor) oder Turing-Diffusionsinstabilität → Raum-Zeit-Muster (spiralförmige Wellen) in 2D. Alan Turing (1952) prägte das mathematische Framework für Musterbildung durch Reaktions-Diffusions-Systeme.",
        difficulty = 5,
        funFact = "Ilya Prigogine erhielt den Nobelpreis für Chemie 1977 für seine Theorie der dissipativen Strukturen. Er war ein glühender Verfechter der Irreversibilität in der Natur und schrieb das philosophische Buch 'The End of Certainty' über die Rolle von Zeit und Zufall in der modernen Physik."
    ),

    // Question 35 – Maximum Entropy Production
    Question(
        categoryId = 2,
        questionText = "Das Prinzip der maximalen Entropieproduktion (MEP) wird als variationales Prinzip für stationäre Nicht-Gleichgewichts-Zustände vorgeschlagen. Welche physikalischen Systeme zeigen MEP-Verhalten, und was sind die theoretischen Grenzen und Kontroversen?",
        answerA = "MEP gilt als bewiesenes Theorem für alle thermodynamischen Systeme und ist äquivalent zur zweiten Hauptsatz-Ableitung; es wurde von Ziegler (1963) rigoros aus der Hamiltonschen Mechanik abgeleitet und ist experimentell fehlerlos verifiziert",
        answerB = "MEP-Verhalten wurde in Klimasystemen (meridionaler Wärmetransport maximiert globale Entropieproduktion), biologischen Systemen und chemischen Reaktionsnetzwerken beobachtet; jedoch ist MEP kein allgemeines Theorem — für nahe-Gleichgewicht gilt Minimale Entropieproduktion (Prigogine), und MEP-Anwendbarkeit ist auf bestimmte Klassen von Systemen mit spezifischen Freiheitsgrad-Strukturen beschränkt; die zugrundeliegende mikrophysikalische Ableitung ist noch Gegenstand der Forschung",
        answerC = "MEP gilt universell nur für biologische Organismen (lebende Systeme) und wurde von Schrödinger in 'What is Life?' als thermodynamische Definition des Lebens eingeführt; physikalische Systeme (Fluide, Chemie) unterliegen dem Gegenteilen: dem Prinzip der minimalen Entropieproduktion",
        answerD = "MEP ist experimentell widerlegt: Vergleiche zwischen Klimamodellen und MEP-Vorhersagen zeigen systematische Abweichungen von > 40 %; das Prinzip hat keine prädiktive Kraft und ist ein historisches Artefakt des frühen 20. Jahrhunderts",
        correctAnswer = 1, // B
        explanation = "Ziegler (1983) postulierte MEP als variationales Prinzip: Das System wählt den Zustand mit maximaler Entropieproduktionsrate unter gegebenen Zwangsbedingungen. Paltridge (1975, 1978) zeigte, dass MEP die meridionale Wärmeumverteilung des Erdklimas gut beschreibt. Dewar (2003) versuchte eine statistische Mechanik-Ableitung aus Maximum-Entropie-Prinzipien (MaxEnt) — aber diese Ableitung ist umstritten. Das Problem: Nahe Gleichgewicht gilt Minimum-Entropieproduktion (Prigogine 1947, für stationäre Zustände lineare Thermodynamik), weit vom Gleichgewicht MEP — aber kein einheitliches Theorem verbindet beide Regime. MEP bleibt ein fruchtbares Konzept ohne rigorosen allgemeinen Beweis.",
        difficulty = 5,
        funFact = null
    ),

    // ── BLOCK 8: Advanced Virology ───────────────────────────────────────────

    // Question 36 – Quasispecies Theory
    Question(
        categoryId = 2,
        questionText = "Quasispezies-Theorie (Eigen, 1971) beschreibt die Populationsdynamik von RNA-Viren mit hohen Mutationsraten. Was ist der 'Error Catastrophe'-Schwellenwert, und welche therapeutische Konsequenz hat er?",
        answerA = "Error Catastrophe tritt auf, wenn Mutationsrate > 1 Mutation pro Replikation; oberhalb dieses Schwellenwerts sterben alle Viren sofort ab. Therapeutisch wird dies durch antivirale Mittel genutzt, die die Replikationsrate auf null reduzieren",
        answerB = "Eigen's Quasispezies-Gleichgewicht: Eine Mutanten-Cloud um einen dominanten Wildtyp-Master-Sequenz; Error Catastrophe tritt auf, wenn die Mutationsrate μ > μ_c = 1 - (1/a), wobei a die selektive Überlegenheit des Wildtyps ist — bei μ > μ_c verliert der Wildtyp seine dominante Position und die Sequenzinformation geht verloren; therapeutische Konsequenz: Mutagen-Therapie (Ribavirin, Favipiravir) erhöht μ über den Schwellenwert und induziert letale Mutagenese von RNA-Viren",
        answerC = "Error Catastrophe ist nur für DNA-Viren relevant, da RNA-Viren keine Proofreading-Aktivität haben und daher immer oberhalb des Katastrophen-Schwellenwerts arbeiten; der Quasispezies-Formalismus ist für RNA-Viren nicht anwendbar, da er Gleichgewichtsbedingungen voraussetzt",
        answerD = "Die Quasispezies-Theorie gilt nur für Viren mit Genomlängen < 1000 Nukleotide; für HIV und Influenza (> 10 kb) ist die Bevölkerungsgenetik-Theorie (Fisher-Wright-Modell) anzuwenden, die Error Catastrophe nicht kennt",
        correctAnswer = 1, // B
        explanation = "Eigen's Quasispezies-Modell: Jedes Genom in einer Population ist Teil einer 'Mutanten-Cloud'. Die Gleichgewichtsverteilung (Quasispezies) ist kein einzelner Genotyp, sondern eine Verteilung. Die mittlere Fitness der Population hängt vom gesamten Fitnesslandschaft-Integral ab. Bei μ > μ_c = 1 - 1/W_m (W_m = Fitness-Vorteil des Wildtyps) kollabiert die Quasispezies. In vitro: QΒRNA-Phage. Therapeutisch: Ribavirin und Favipiravir sind Mutagen-Analoga, die Fehlerrate erhöhen. Schwierig: RNA-Viren arbeiten nahe μ_c (evolutionäre Optimierung), sodass kleine Erhöhungen der Fehlerrate ausreichen — aber auch das Virus dadurch schwächen könnten.",
        difficulty = 5,
        funFact = "Manfred Eigen erhielt den Nobelpreis für Chemie 1967, noch vor der Entwicklung seiner Quasispezies-Theorie in den 1970ern. Seine Arbeiten über ultrakurze chemische Reaktionen und RNA-Evolution sind zwei der einflussreichsten Beiträge zur modernen Biochemie."
    ),

    // Question 37 – Viral Recombination Mechanisms
    Question(
        categoryId = 2,
        questionText = "Virale Rekombination erzeugt neue Varianten durch Neukombination von genetischem Material. Welche mechanistischen Unterschiede bestehen zwischen Reassortment (Influenza) und Template-Switching-Rekombination (Coronaviren), und welche Bedeutung haben sie für die Pandemieentstehung?",
        answerA = "Reassortment und Template-Switching sind mechanistisch identisch — beide beruhen auf Homologer Rekombination durch RecA-ähnliche Proteine; Coronaviren zeigen Reassortment ihrer 5 Genomsegmente, während Influenza Template-Switching in der kontinuierlichen RNA-Genomkette durchführt",
        answerB = "Influenza-Reassortment: Bei Ko-Infektion zweier Influenza-Stämme werden die 8 RNA-Segmente unabhängig verpackt — neue Virionen können gemischte Segmentkombinationen (z.B. H1N1+H3N2 → H1N2) tragen; Template-Switching in CoV: Die RdRp wechselt während der negativstrangigen RNA-Synthese zwischen dem genomischen RNA-Template und subgenomischen Sequenzen, erzeugt Chimären-Genome; beide Mechanismen ermöglichen Antigenshift (diskrete antigene Sprünge) — kritisch für Pandemieentstehung (Influenza 1918, 1957, 1968, 2009)",
        answerC = "Reassortment ist auf positive-Sense-RNA-Viren beschränkt; Template-Switching auf negative-Sense-RNA. Influenza besitzt kein Reassortment, da die RNA-Segmente kovalent verbunden sind; SARS-CoV-2 zeigt Reassortment der Spike-Genabschnitte mit bat-CoV bei direkter Doppelstrang-DNA-Bildung",
        answerD = "Virale Rekombination betrifft nur DNA-Viren (Herpesviridae, Adenoviridae); RNA-Viren sind durch die geringe Prozessivität ihrer RdRp vor jeglicher Rekombination geschützt, was erklärt, warum RNA-Viren konservativere Genome als DNA-Viren haben",
        correctAnswer = 1, // B
        explanation = "Influenza A: 8 einzelsträngige negative-Sense-RNA-Segmente (PB2, PB1, PA, HA, NP, NA, M, NS) werden unabhängig in Virionen verpackt. Bei Ko-Infektion einer Zelle durch zwei Stämme: Neue Virionen erhalten zufällige Segmentkombinationen (Reassortment). H3N2 (1968 Pandemie) entstand durch Reassortment von H2N2 mit aviärem H3. CoV-Rekombination: RdRp (nsp12) springt bei negativstrangiger Synthese zwischen TRS (Transcription Regulatory Sequences) — normal für subgenomische mRNAs — aber kann auch zwischen Co-infizierten Genomen Chimären erzeugen. SARS-CoV-2-Spike-Rekombinationen aus multiplen bat-CoV erkannt (Andersen et al. 2020).",
        difficulty = 5,
        funFact = null
    ),

    // Question 38 – Defective Interfering Particles
    Question(
        categoryId = 2,
        questionText = "Defective Interfering Particles (DIPs) sind unvollständige Viruspartikel mit partiell deletierten Genomen. Welcher Mechanismus erklärt ihre Entstehung, und wie interferieren sie mit der Wildtyp-Virus-Replikation?",
        answerA = "DIPs entstehen durch Insertion von Wirtssequenzen in das Virusgenom; sie interferieren mit dem Wildtyp durch kompetitive Bindung an Antivirus-Rezeptoren auf der Zelloberfläche, was die Wildtyp-Infektion um Faktor 10⁶ reduziert",
        answerB = "DIPs entstehen durch Polymerase-Slippage/-Template-Switching während der Replikation — interne Deletionen erzeugen kürzere Genome, die die cis-wirkenden RNA-Signale für Replikation und Verpackung behalten; in Ko-Infektion mit Wildtyp nutzen DIPs die trans-wirkenden Proteine des Wildtyps als 'Helfer' und replizieren schneller (kürzeres Genom); sie inhibieren Wildtyp durch Konkurrenz um Replikationsenzyme/Verpackungssignale und können die Infektion modulieren oder dämpfen",
        answerC = "DIPs entstehen ausschließlich durch CRISPR-ähnliche Selbst-Editierung des Virusgenoms; sie interferieren durch Aktivierung der RIG-I/MDA5-Signalkaskade, die Apoptose in infizierten Zellen auslöst — ein aktiver Antivirusmechanismus des Virus gegen sich selbst",
        answerD = "DIPs sind ausschließlich ein artifizielles Laborartefakt ohne in-vivo-Relevanz; sie entstehen nur bei hohen MOI (> 100) und inaktivieren das Wildtypvirus durch kovalente Vernetzung von Genomsegmenten via Formaldehyd-ähnliche Seitenprodukte der Replikation",
        correctAnswer = 1, // B
        explanation = "DIP-Entstehung: Virale Polymerasen können vom Template 'springen', was interne Deletionen erzeugt. Die deletierten Genome sind kürzer, replizieren daher schneller (kinetischer Vorteil). Sie behalten Verpackungssignale (packaging signals) und 5'/3'-UTRs für Replikation. Im Ko-Infektionssystem: Wildtyp liefert trans-Faktoren (RdRp, Kapsidproteine) — DIPs sind Parasiten des Parasiten ('molecular cheating'). DIPs können Infektionsdynamik modulieren: Reduzierte Wildtyp-Produktion, verlängerte Persistenz, gedämpfte Pathogenität. In vivo bei Influenza, VSV, Polio gut dokumentiert. Potenziell therapeutisch nutzbar (externe DIP-Verabreichung).",
        difficulty = 5,
        funFact = "Defective Interfering Particles wurden erstmals 1945 von Salvador Luria beim Influenza-Virus beschrieben und 'von Magens-Lunch' genannt. Erst Jahrzehnte später wurde ihre molekulare Grundlage verstanden — sie sind heute als natürliche Modulatoren von Virusinfektionen anerkannt."
    ),

    // Question 39 – Viral Tropism Determinants
    Question(
        categoryId = 2,
        questionText = "Virale Gewebsselektivität (Tropismus) bestimmt Pathogenese und Übertragbarkeit. Welche molekularen Faktoren bestimmen den Lungentropismus von hochpathogenen Influenza H5N1, und warum ist es bisher nicht auf Mensch-zu-Mensch-Übertragung übergegangen?",
        answerA = "H5N1-Lungentropismus wird durch den PB1-F2-Protein-Cytotoxizitätsmechanismus bestimmt; die fehlende Mensch-zu-Mensch-Übertragung liegt an der zu geringen Replikationsrate in Nasen-Rachenraum-Temperaturen (32°C)",
        answerB = "H5N1-Lungentropismus: Bindung an α-2,3-verknüpfte Sialinsäure-Rezeptoren (predominant in menschlichen Unterlungenalveolen und aviärem Respirationstrakt), aktivierbar nur durch TMPRSS2/Tryptase in der Lunge (Multibase-Cleavage-Site im HA); fehlende Mensch-zu-Mensch-Übertragung durch a) präferente α-2,3 vs. α-2,6 Bindung (oberer Atemwegt hat α-2,6), b) geringe Replikation im oberen Atemweg, c) fehlende Anpassung des PB2 (E627K fehlt in meisten H5N1-Stämmen für optimale Replikation bei 33°C)",
        answerC = "H5N1-Tropismus wird ausschließlich durch HA-Kalzium-Bindungsaffinität bestimmt — höhere Ca²⁺-Konzentration in der Lunge verstärkt HA-Trimerisierung; Mensch-zu-Mensch-Übertragung ist bereits dokumentiert in Indonesien 2006 (WHO-Cluster H5N1, 8 Fälle), aber durch H3N2-Hintergrundimmunität limitiert",
        answerD = "H5N1 kann keine Mensch-zu-Mensch-Übertragung entwickeln, da das Virus grundsätzlich nur aviäre Polymerase-Untereinheiten kodiert (keine Säugertier-kompatiblen PA, PB1, PB2); diese Eigenschaft ist genetisch fest und durch keine Mutation überbrückbar",
        correctAnswer = 1, // B
        explanation = "Sialinsäure-Bindung ist zentral: α-2,3-Gal-SA (aviärer Rezeptortyp) dominiert in menschlichem Unterlungen-Alveolarepithel und GI-Trakt; α-2,6-Gal-SA dominiert im oberen Atemweg (Trachea, Bronchien). H5N1 bindet präferentiell α-2,3 → repliziert tief in der Lunge, schlecht im oberen Atemweg → geringe Aerosolübertragung. Für Mensch-zu-Mensch-Übertragung benötigt: (1) HA-Mutation für α-2,6-Bindung, (2) PB2 E627K oder D701N für Replikation bei 33°C (oberer Atemweg), (3) NA-Stabilität. Ferret-Studien (Fouchier 2012, Kawaoka 2012) zeigten: 4-5 Mutationen reichen für Tröpfchenübertragung — kontrovers diskutiert wegen Dual-Use-Forschungsbedenken.",
        difficulty = 5,
        funFact = "Die 2011-2012 Kontroverse um H5N1-Gain-of-Function-Experimente (Fouchier/Kawaoka) führte zu einem freiwilligen Moratorium der Forschung und einer globalen Debatte über Dual-Use-Forschung — ein Musterbeispiel für den Konflikt zwischen wissenschaftlicher Transparenz und Biosicherheit."
    ),

    // Question 40 – Endogenous Retroviruses
    Question(
        categoryId = 2,
        questionText = "Endogene Retroviren (ERVs) machen ~8 % des menschlichen Genoms aus. Welche evolutionären Prozesse führen zur Fixierung von ERVs im Keimbahn-Genom, und welche funktionellen Rollen haben domestizierte ERV-Proteine beim Menschen?",
        answerA = "ERVs werden durch aktive Transposition auf allen Chromosomen gleichzeitig fixiert; domestizierte ERV-Proteine dienen ausschließlich als Tumorsuppressoren und werden bei Krebserkrankungen durch Methylierung inaktiviert",
        answerB = "ERV-Integration in Keimzellen wird durch zufällige retrovirale Infektion von Gameten oder frühen Embryonalzellen ausgelöst; Fixierung erfolgt durch genetische Drift (neutraler Einbau) oder positive Selektion wenn der ERV einen Fitnessvorteil bietet; domestizierte ERV-Proteine beim Menschen: Syncytin-1 und -2 (fusogene Glykoproteine, abgeleitet von Env-Proteinen, essentiell für Synzytiotrophoblasten-Bildung in der Plazenta), Arc-Protein (Gag-Derivat, beteiligt an synaptischer Plastizität durch RNA-Transport in Exosomen)",
        answerC = "ERVs werden durch Horizontal-Transfer zwischen Wirtsspezies über Vektoren (Mücken, Zecken) fixiert; alle ERV-Elemente sind transkriptionell dauerhaft silenziert durch epigenetische Methylierung und spielen keine funktionelle Rolle im menschlichen Organismus",
        answerD = "ERV-Fixierung ist ausschließlich durch Insertions-Mutagenese proto-onkogener Loci erklärbar (insertional activation); funktionell aktive ERV-Proteine sind auf Mäuse beschränkt, beim Menschen sind alle ERV-abgeleiteten ORFs durch Stop-Codons unterbrochen",
        correctAnswer = 1, // B
        explanation = "Retrovirale Integration in Keimzellen → vertikale Vererbung als provirales Element. Fixierung: (1) Genetische Drift in kleinen Populationen (selektiv neutral); (2) Positive Selektion durch Wirt-Domestizierung. Syncytine: Menschliche Syncytin-1 (HERV-W Env) und Syncytin-2 (HERV-FRD Env) werden für Synzytiotrophoblasten-Fusion benötigt — Knockout-Mäuse für Sync1 sterben in utero. Dies ist ein Paradebeispiel für 'exaptation' (Funktionswechsel). Arc/Arg3.1: Gag-Domänenähnlichkeit; bildet Kapsid-ähnliche Strukturen, die mRNA transportieren — postsynaptische Plastizitätsfunktion. HERV-K(HML-2) ist die jüngste aktive ERV-Familie; einige Stämme können infektiöse Retrovirionen produzieren.",
        difficulty = 5,
        funFact = "Syncytin-Gene für die Plazentabildung haben sich durch Retrovirus-Domestizierung mindestens 7-mal unabhängig in verschiedenen Säugetierlinien entwickelt — ein erstaunliches Beispiel für konvergente Nutzung von viralen Werkzeugen durch Wirtorganismen."
    ),

    // ── BLOCK 9: Metamaterials & Plasmonics ──────────────────────────────────

    // Question 41 – Negative Refractive Index
    Question(
        categoryId = 2,
        questionText = "Negativbrechende Metamaterialien haben sowohl negative Permittivität (ε < 0) als auch negative Permeabilität (μ < 0). Welches physikalische Phänomen entsteht bei negativem n, und welche strukturellen Designs ermöglichen simultanes ε < 0 und μ < 0?",
        answerA = "Negativer Brechungsindex führt zu totaler Reflexion an der Oberfläche analog zu Total Internal Reflection; simulanes ε < 0 und μ < 0 wird durch Mischung von Ferritten (μ < 0) und Metallen (ε < 0) in nano-Granulat-Kompositen erreicht",
        answerB = "Negativer Brechungsindex n = -√(εμ) < 0 führt zu Rückwärtsbrechung (negative Snell-Brechung, Strahlsatz auf der 'falschen' Seite des Normalenvektors), umgekehrtem Doppler-Effekt und umgekehrter Cherenkov-Strahlung; ε < 0 durch metallische Drähte (Plasma-Frequenz im GHz-Bereich), μ < 0 durch Split-Ring-Resonatoren (SRRs, magnetische LC-Kreise), beide in periodischem Gitter: das erste 'linkes Medium' (LHM, Smith et al. 2000)",
        answerC = "Negativer Brechungsindex erlaubt Supertransmission: Licht passiert durch Löcher viel kleiner als die Wellenlänge mit Transmission > 1 (bezogen auf die Lochfläche); simulanes ε < 0 und μ < 0 wird durch quantenmechanische Interferenz in Photonenkristallen erzeugt, ohne metallische oder magnetische Komponenten",
        answerD = "Metamaterialien mit n < 0 müssen die Kramers-Kronig-Kausalitätsbedingungen nicht erfüllen, da ε und μ unabhängige Responsefunktionen sind; die Struktur erfordert optisch aktive chiral-nematische Flüssigkristall-Schichten mit periodischem Twist-Winkel von 90°",
        correctAnswer = 1, // B
        explanation = "Veselago (1968) theoretisierte LHMs ('left-handed materials' mit simultanem ε, μ < 0): k, E, H bilden ein linkshändiges Tripel statt rechtshändiges. Smith et al. (2000, UCSD) realisierten das erste LHM im Mikrowellenbereich: Kupfer-SRRs (Split-Ring-Resonatoren) liefern μ(ω) < 0 nahe der magnetischen Resonanz; metallische Stäbe liefern ε(ω) < 0 unterhalb der Plasmafrequenz. Im Überlappbereich: n = -1. Konsequenzen: Negativer Snell: θ_r = -θ_i; Flachlinse ohne Wölbung; Supertransmission von evaneszenten Wellen (Pendry's Perfect Lens 2000, theoretisch). Verluste sind die praktische Haupteinschränkung.",
        difficulty = 5,
        funFact = "John Pendry's theoretische 'Perfect Lens' (2000) aus negativem Brechungsindex-Material sollte Beugungsgrenzen überwinden und perfekte Bilder erzeugen — ein revolutionäres Konzept, das trotz praktischer Verlustprobleme eine ganze Forschungsrichtung ausgelöst hat."
    ),

    // Question 42 – Cloaking Devices
    Question(
        categoryId = 2,
        questionText = "Elektromagnetisches Tarnkappen-Design ('cloaking') basiert auf Transformationsoptik. Welches mathematische Prinzip liegt zugrunde, und welche physikalischen Einschränkungen verhindern eine perfekte Breitband-Tarnkappe?",
        answerA = "Tarnkappen basieren auf Bragg-Reflektion in photonischen Kristallen: Licht wird in einem Stoppband vollständig absorbiert und innerhalb der Tarnkappe geführt. Der einzige physikalische Grenzwert ist die Bandbreite des photonischen Stoppbandes",
        answerB = "Transformationsoptik: Raumkoordinaten werden so deformiert, dass elektromagnetische Felder um ein Objekt herumgebogen werden — die Maxwell-Gleichungen sind forminvariant unter Koordinatentransformationen, sodass eine Koordinatenquetschung um eine Region in spezifische ε(r) und μ(r)-Profile übersetzt wird; Einschränkungen: Kausale Kramers-Kronig-Bedingungen erzwingen starke Dispersion in jedem Material mit n < 1 (Phasengeschwindigkeit > c), was Breitband-Cloaking durch Gruppengeschwindigkeits-Bandbreite-Produkt-Limit verhindert",
        answerC = "Tarnkappen funktionieren durch destruktive Interferenz aller gestreuten Wellen außerhalb des Tarnkappen-Radius; das Mie-Scattering der Tarnkappe selbst überlagert das Rückstreufeld des Objekts mit exakt π-Phasenverschiebung; Einschränkung: Funktioniert nur für Objekte mit Radius < λ/100",
        answerD = "Elektromagnetische Tarnkappen sind thermodynamisch verboten: Cloaking würde die Entropie lokal verringern und verletzt den zweiten Hauptsatz; das Earnshaw-Theorem verhindert stabile magnetische Feldkonfigurationen um ausgedehnte 3D-Objekte",
        correctAnswer = 1, // B
        explanation = "Pendry, Schurig & Smith (Science 2006) und Leonhardt (Science 2006) entwickelten Transformationsoptik unabhängig. Kernidee: Maxwell-Gleichungen sind kovariant unter Koordinatentransformationen → Koordinatentransformation → anisotropes, inhomogenes ε(r), μ(r)-Profil, das Licht um die Tarnzone lenkt. Smith et al. (2006) demonstrierten Mikrowellen-Cloaking mit SRR-basiertem Metamaterial (schmal bandbreite). Fundamentale Einschränkung: Foster-Reaktanztheorem und Kramers-Kronig: Jedes Material mit |n| < 1 in einem Frequenzband zeigt starke Dispersion außerhalb → Gruppengeschwindigkeitsdispersion → Cloaking für verschiedene Frequenzen in verschiedenen Raumzeitverzögerungen → kein Breitband-Cloaking möglich.",
        difficulty = 5,
        funFact = null
    ),

    // Question 43 – Plasmonic Nanoparticles
    Question(
        categoryId = 2,
        questionText = "Lokalisierte Oberflächenplasmonenresonanz (LSPR) in Gold-Nanopartikeln erzeugt intensive Nahfelder. Von welchen Parametern hängt die LSPR-Wellenlänge ab, und welches Phänomen ermöglicht Surface-Enhanced Raman Scattering (SERS) mit Einzelmolekül-Empfindlichkeit?",
        answerA = "LSPR-Wellenlänge hängt ausschließlich von der Elektronendichte des Metalls ab (Drude-Modell) und ist für alle Gold-Nanopartikel identisch bei 520 nm; SERS-Verstärkung kommt durch direkte Kopplung von Molekülschwingungen an Quantenplasmonen",
        answerB = "LSPR-Wellenlänge hängt von Partikelgröße, -form (Kugel, Stab, Stern), Dielektrizitätskonstante des Mediums und Interpartikelabstand ab (kollektive Kopplung); SERS-Einzelmolekül-Empfindlichkeit (EF ~ 10⁸–10¹⁰) entsteht durch extreme Nahfeld-Verstärkung |E|⁴ in Nanospalt-'Hotspots' zwischen eng benachbarten Nanopartikeln oder an scharfen Spitzen (Lightning-Rod-Effekt) — elektromagnetische Verstärkung + chemische Verstärkung",
        answerC = "LSPR-Wellenlänge ist temperaturunabhängig und kann nur durch Legierung mit Silber (Au/Ag-Alloy) verändert werden; SERS entsteht durch Resonanz-Raman-Verstärkung wenn die Laserfrequenz mit der Nanopartikel-LSPR zusammenfällt — der Verstärkungsfaktor ist exakt e^4 = 54,6 für alle Nanopartikel",
        answerD = "LSPR tritt nur an planaren Gold-Oberflächen auf, nicht an Nanopartikeln; der Begriff 'lokalisiert' ist irreführend, da Plasmonen in Nanopartikeln über das gesamte Metall delokalisiert sind. SERS-Verstärkung kommt durch Fluoreszenz-Unterdrückung: Moleküle nahe Gold emittieren keine Fluoreszenz, was Raman-Signale erst messbar macht",
        correctAnswer = 1, // B
        explanation = "LSPR entsteht durch kollektive Elektronen-Oszillation, die durch die Partikelgeometrie eingeschlossen ist. Mie-Theorie (exakt für Kugeln): LSPR-Frequenz bei εm(ω) = -2ε_medium. Goldnanokugeln (d~20 nm): λ_LSPR ~ 520 nm; Goldnanostäbe: transversale und longitudinale LSPR (longitudinal tunable von 600-1200 nm je nach Aspect Ratio). SERS: Raman-Streuquerschnitt σ_SERS = σ_Raman · |E_local/E_0|⁴ → |E|⁴-Abhängigkeit wegen Zwei-Photonen-Prozess (Einstrahlung + Raman-Streuung). In Nanolücken (d < 2 nm) zwischen zwei Au-Partikeln: |E|² ~ 10⁴ → |E|⁴ ~ 10⁸ Verstärkung. Nie & Emory (1997) zeigten erstmals SERS an Einzelmolekülen.",
        difficulty = 5,
        funFact = "Römer verwendeten bereits im 4. Jahrhundert n. Chr. Gold-Nanopartikel unbewusst für das Lycurgus-Becher-Glas — das Glas erscheint im Durchlicht rot (Goldnanokugeln absorbieren Blau/Grün) und im Auflicht grün. Es ist die älteste bekannte Nanopartikel-Anwendung der Menschheitsgeschichte."
    ),

    // Question 44 – Hyperbolic Metamaterials
    Question(
        categoryId = 2,
        questionText = "Hyperbolische Metamaterialien (HMMs) haben eine anisotrope Dielektrizitätstensor mit entgegengesetzten Vorzeichen von ε∥ und ε⊥. Welche optische Eigenschaft folgt aus dieser Anisotropie, und warum sind HMMs für Spontanemissionserhöhung (Purcell-Effekt) interessant?",
        answerA = "HMMs haben in beiden Richtungen gleiche Gruppengeschwindigkeit; der Purcell-Faktor ist identisch zum Faktor in isotropen Metallen und wird ausschließlich durch die Impedanz-Anpassung an den Dipol-Emitter bestimmt",
        answerB = "Die HMM-Isofrequenzfläche (k-Raum) ist hyperbolisch: kx²/ε∥ + kz²/ε⊥ = (ω/c)², für ε∥ · ε⊥ < 0 ergibt das eine offene Hyperbel statt einer geschlossenen Ellipse → unbegrenzt hohe k-Vektoren sind propagierend statt evaneszent; der Purcell-Faktor F_P ~ ρ_DOS ist für hohe k-Vektoren massiv vergrößert, da HMMs eine divergierende photonische Zustandsdichte besitzen (Bragg-Singularität) — praktisch limitiert durch Absorption",
        answerC = "Hyperbolische Metamaterialien zeigen negative Gruppengeschwindigkeit in beiden Polarisationsrichtungen; die Spontanemissionserhöhung kommt durch Anderson-Lokalisierung von Photonen in der HMM-Schichtstruktur — angrenzende Emitter werden durch lokalisierte Photoneninterferenz vollständig gekoppelt",
        answerD = "HMMs haben isotrope dielektrische Eigenschaften bei hohen Frequenzen (ω > ωp) und zeigen hyperbolisches Verhalten ausschließlich bei ω < ωp; die Spontanemission wird durch HMMs um exakt Faktor π verglichen mit Vakuum erhöht, unabhängig von Geometrie und Material",
        correctAnswer = 1, // B
        explanation = "Isotrop: Isofrequenz-Fläche = Kugel; anisotropisch (ε∥ ≠ ε⊥, gleiche Vorzeichen): Ellipsoid. Bei ε∥ · ε⊥ < 0 (hyperbolic dispersion): Isofrequenzfläche ist eine zweischalige Hyperboloide → für jeden Wellenzahlvektor k-Betrag (auch sehr groß) gibt es propagierende Wellen. In normalen Medien sind hohe-k-Wellen evaneszent (exponentiell abklingend). HMMs: AlGaN/GaN-Supergitter, Ag/TiO₂-Schichtsysteme, Graphen-Gitter. Purcell-Faktor F_P = (3/4π²)(λ/n)³(Q/V) → für HMMs: effektives modales Volumen V → 0, Q/V → ∞ theoretisch. Dazu: Nanolaser, Spontanemissions-Ingenieurswesen, Bio-SERS.",
        difficulty = 5,
        funFact = null
    ),

    // Question 45 – Acoustic Metamaterials
    Question(
        categoryId = 2,
        questionText = "Akustische Metamaterialien besitzen ungewöhnliche Dichte- und Kompressibilitätseigenschaften. Welche Struktur ermöglicht negative effektive Massendichte, und welches Design erzeugt ein akustisches Analogon zum elektromagnetischen Tarnkappen-Effekt?",
        answerA = "Negative Massendichte in akustischen Metamaterialien entsteht durch Schallabsorption in porösen Aluminiumschaum; akustische Tarnkappen werden durch Anordnung von Hartmetall-Zylindern in Dreiecksgittern realisiert",
        answerB = "Negative effektive Massendichte entsteht, wenn lokale Resonatoren (Membran-Resonatoren, Federmassen-Systeme) bei bestimmten Frequenzen mit dem externen Schallfeld gegenphasig schwingen — der sichtbare Schwerpunkt des Resonators bewegt sich entgegengesetzt zur anregenden Kraft; akustische Tarnkappen nutzen Transformation-Akustik (analog zu Transformationsoptik): periodische Anordnung von Zylindern mit spezifischer Dichte/Kompressibilität-Anisotropie lenkt Schallwellen um ein Objekt herum",
        answerC = "Negative effektive Massendichte ist thermodynamisch unmöglich, da negative Massenträgheit das Newtonsche Bewegungsgesetz verletzen würde; akustische Metamaterialien können nur positive aber stark anisotrope Massendichte realisieren",
        answerD = "Akustische Metamaterialien mit negativer Massendichte bestehen aus Flüssigkristall-gefüllten Hohlkugeln; bei elektrischer Ausrichtung der Flüssigkristalle wechselt die effektive Massendichte spontan von positiv zu negativ; Tarnkappen-Effekt entsteht durch piezoelektrische Aktoren, die aktiv Gegenschall emittieren",
        correctAnswer = 1, // B
        explanation = "Liu et al. (Science 2000) zeigten: Gummibeschichtete Blei-Kugeln in Epoxymatrix → lokale Resonanzen unterhalb der Durchgangsfrequenz. Bei Resonanzfrequenz: Federmassenglied schwingt 180° phasenverschoben → mittlere Reaktionskraft entgegengesetzt zu Nettobeschleunigung → ρ_eff < 0 in einem Frequenzband. Zweites Konzept: negative effektive Kompressibilität κ_eff < 0 durch seitliche Helmholtz-Resonatoren. Gleichzeitig negative ρ und κ → negative Gruppengeschwindigkeit (doppelt-negatives akustisches Medium), analog zum LHM. Akustische Tarnkappen (Cummer, Pendry 2007): Transformation-Akustik mit anisotroper Massendichtetensor, realisiert durch Schichtung von Materialien.",
        difficulty = 5,
        funFact = "Das erste akustische Metamaterial (Liu et al., Science 2000) hatte eine Einheitszelle von 15,8 mm, arbeitete bei Schallfrequenzen um 400 Hz und war damit das erste experimentelle Metamaterial überhaupt — zwei Jahre vor den elektromagnetischen Metamaterialien."
    ),

    // ── BLOCK 10: Origin of Life ──────────────────────────────────────────────

    // Question 46 – RNA World Hypothesis
    Question(
        categoryId = 2,
        questionText = "Die RNA-Welt-Hypothese postuliert eine frühe Evolutionsphase mit selbstreplizierenden RNA-Molekülen. Welche experimentellen Befunde stützen diese Hypothese, und welches ungelöste 'Chicken-and-Egg'-Problem besteht?",
        answerA = "RNA-Welt wird gestützt durch die Entdeckung von DNA-Ribozymen (DNA-Enzymen); das ungelöste Problem ist die Herkunft der Ribose-Zucker, da Ribose in der Miller-Urey-Synthese nie nachgewiesen wurde",
        answerB = "Stützende Befunde: (1) Ribozyme (katalytische RNAs): selbst-schneidende Introns, RNase P, Ribosomen-Peptidyltransferase-Aktivität liegt in der 23S-rRNA (nicht im Protein); (2) in vitro-Evolution von RNA-Replikasen durch SELEX; (3) Energie-Währung ATP ist ein Ribonukleotid; das Chicken-and-Egg-Problem: RNA-Synthese benötigt aktivierte Nukleotide und RNA-Polymerase (typisch Protein), aber Proteine benötigen RNA-Information — ein Zirkelschluss für den Ursprung",
        answerC = "RNA-Welt wird ausschließlich durch das Ribozym in der Signalerkennungspartikel (SRP) gestützt; das ungelöste Problem ist die Stabilität von RNA unter den vorbiologischen Bedingungen, die durchgehend basisch waren (pH > 10), was RNA sofort hydrolysiert",
        answerD = "RNA-Welt ist durch molekulare Phylogenie widerlegt: Die universelle Stammbaum-Analyse zeigt, dass alle modernen Ribozyme jünger sind als Proteine; der Ursprung des genetischen Codes durch Aminosäure-Nucleotid-Wechselwirkungen hat RNA überflüssig gemacht",
        correctAnswer = 1, // B
        explanation = "Cech (Nobel 1989) und Altman (Nobel 1989) entdeckten Ribozyme, was die Idee einer RNA-only-Welt (Walter Gilbert, 1986) konkretisierte. Schlüsselbefund: Peptidyltransferasezentrum des Ribosoms liegt im RNA-Teil (23S/28S rRNA), nicht im Protein — Ribosomen sind Ribozyme. Ribozyme-Katalyse: Selbst-Splicing-Introns, Ribonuklease P (rRNA-Anteil aktiv), Hammerhead/HDV-Ribozyme (Virolzyme). In vitro-Evolution: Bartel Lab erzeugte RNA-Polymerase-Ribozyme (tR2, 2001) die RNA aus Nukleotiden synthetisieren. Das Chicken-Egg-Problem: Sufferfield/Sutherland 2015 zeigte chemische Synthese von Pyrimidinribonukleosiden unter plausiblen prä-biotischen Bedingungen — teilweise gelöst.",
        difficulty = 5,
        funFact = "Walter Gilbert prägte den Begriff 'RNA World' in einem Nature-Kommentar 1986 — genau drei Jahre bevor Cech und Altman den Nobelpreis für die Entdeckung der Ribozyme erhielten, die seine Hypothese experimentell unterstützten."
    ),

    // Question 47 – Iron-Sulfur World
    Question(
        categoryId = 2,
        questionText = "Günter Wächtershäusers Eisen-Schwefel-Welt-Hypothese postuliert chemolithotrophe Urchemie an Pyrit-Oberflächen. Welcher Energiemechanismus treibt die präbiotische Biosynthese an, und wie unterscheidet sich das von der Miller-Urey-Approach?",
        answerA = "Eisen-Schwefel-Welt nutzt UV-Strahlung als Energiequelle, genau wie Miller-Ureys Blitz-Simulation; der einzige Unterschied ist die Oberfläche (Pyrit vs. Wasser) und die anorganische Katalyse statt gasförmiger Reaktionen",
        answerB = "Wächtershäusers Modell nutzt die exotherme Pyrit-Bildungsreaktion (FeS + H₂S → FeS₂ + 2H⁺ + 2e⁻, ΔG = -38 kJ/mol) als Elektronendonor für reduktive Kohlenstofffixierung — ein 'surface metabolism' an positiv geladenen FeS/FeS₂-Oberflächen; Miller-Urey verwendet spark discharge (atmosph. Energie) um organische Moleküle in der Gasphase/Wasser zu synthetisieren — keine Oberfläche, keine kontinuierliche Energie",
        answerC = "Eisen-Schwefel-Welt basiert ausschließlich auf hypothetischen Alkali-hydrothermalen Vents; Wächtershäuser postuliert osmotische Energie durch die Salz-Konzentrationsgradient zwischen ozeanischem Wasser und hydrothermalen Fluiden als einzige Energiequelle für präbiotische Reaktionen",
        answerD = "Miller-Urey und Eisen-Schwefel-Welt sind komplementäre Ansätze ohne grundlegenden Unterschied: Beide synthetisieren Aminosäuren durch Stickstoffplasmatolyse; der einzige experimentelle Unterschied ist die Reaktionstemperatur (Miller-Urey: 25°C, Eisen-Schwefel-Welt: 350°C)",
        correctAnswer = 1, // B
        explanation = "Wächtershäuser (1988, 1990): FeS + H₂S → FeS₂ + 2[H] ist stark exotherm und liefert reduzierende Äquivalente. Die positiv geladene Pyrit-Oberfläche bindet negativ geladene Biomoleküle (Thioacetate, Pyruvat-Analoga) und wirkt als präbiotisches 2D-Reaktionsgefäß. Vorteile: Kontinuierliche Energiequelle, konzentrierende Oberfläche, milde Bedingungen. Experimentelle Stützen: Huber & Wächtershäuser (1997, 1998, Science) synthetisierten Methanthiol und Acetylmethylsulfid aus CO + FeS/NiS — mögliche Acetyl-CoA-Vorläufer. Modernisiert: Russell/Martin's 'chemiosmotic origin' at alkaline hydrothermal vents (H₂-Gradienten, FeS-Membranen als proto-Mitochondrien).",
        difficulty = 5,
        funFact = null
    ),

    // Question 48 – Protocell Formation
    Question(
        categoryId = 2,
        questionText = "Protokellen als erste zelluläre Einheiten werden durch selbstassemblierende Fettsäuremembranen modelliert. Welche Eigenschaften machen Fettsäure-Vesikel gegenüber modernen Phospholipid-Membranen für den Lebensursprung attraktiv, und welcher Mechanismus ermöglicht ihre Wachstum ohne enzymatische Kontrolle?",
        answerA = "Fettsäure-Vesikel sind stabiler als Phospholipid-Vesikel bei pH 2-3 (ozeanische Früherde-Bedingungen); Wachstum erfolgt durch pH-gesteuerte Fusion mit neu synthetisierten Fettsäuren aus Fischer-Tropsch-Synthese in der Atmosphäre",
        answerB = "Fettsäure-Vesikel (z.B. Decanoat pH 7-9) haben dynamische Membranen mit schnellem Flip-Flop und hoher Permeabilität für kleine Moleküle (Nukleotide können passieren, anders als bei modernen low-permeability Phospholipidmembranen); Wachstum durch kompetitive Verdrängung — Vesikel mit eingeschlossener Nukleinsäure (osmotischer Druck) nehmen bevorzugt Fettsäuremizellen aus der Umgebung auf und wachsen auf Kosten leerer Vesikel",
        answerC = "Fettsäure-Protokellen sind ausschließlich bei Temperaturen > 80°C stabil und benötigen Mg²⁺-Ionen > 100 mM; ihr Vorteil ist die spontane Enkapsulierung von RNA-Polymeraseaktivität durch nicht-kovalente Wechselwirkung mit Mineraloberflächenpeptiden",
        answerD = "Protokellen aus Fettsäuren können keine genetischen Polymere enkapsulieren, da RNA und DNA negativ geladen sind und Fettsäure-Membranen ebenfalls negativ geladen sind (elektrostatische Abstoßung); daher ist das Szientario-Protokell-Modell zugunsten viraler RNA-Kapsidprotein-Evolution aufgegeben worden",
        correctAnswer = 1, // B
        explanation = "Szostak-Labor (Harvard/MGH) hat Protokell-Modelle intensiv entwickelt. Decansäure/Decanol (1:2) bei pH 7-9 bildet stabile Vesikel. Entscheidende Eigenschaft: Hohe Membranpermeabilität für Nukleotide (~100-fach höher als in Phospholipid-Bilayern) → Ribozyme im Inneren können Substrate (NTPs) aufnehmen. Wachstumsmechanismus: Osmotischer Druck durch eingeschlossene RNA → Membranspannung → bevorzugte Fettsäure-Aufnahme aus Micellen-reicher Umgebung (Pearce et al. 2017). Template-gesteuerte RNA-Polymerisation innerhalb von Vesikeln ohne Enzyme: möglich unter Mg²⁺ und bestimmten Kofaktoren. Kopierung und Wachstum ohne Proteine ist der Kernbeweis für RNA-Welt-Kompatibilität.",
        difficulty = 5,
        funFact = "Jack Szostak erhielt 2009 den Nobelpreis für Physiologie/Medizin (für Telomere), forscht aber seit Jahrzehnten hauptsächlich zum Ursprung des Lebens — sein Labor ist das produktivste der Welt in der Protokell- und RNA-Welt-Forschung."
    ),

    // Question 49 – Chirality Origin
    Question(
        categoryId = 2,
        questionText = "Die Homochiralität des Lebens (L-Aminosäuren, D-Zucker) ist ein fundamentales Rätsel. Welche physikalischen und chemischen Mechanismen wurden für die Entstehung von chiraler Überschuss vorgeschlagen, und welches Experiment demonstrierte chemische Amplifikation von kleinen chiralen Überschüssen?",
        answerA = "Homochiralität entstand durch Zufall (statistische Fluktuation) in einer Population von chiralen Monomeren; das Soai-Experiment zeigte, dass 2-Propylzinkiodid-Reaktionen stabile D/L-Überschüsse von 50:50 erzeugen können",
        answerB = "Vorgeschlagene Mechanismen für chirale Überschüsse: (1) Paritätsverletzung in der schwachen Wechselwirkung (PVED: parity-violating energy difference ~ 10⁻¹⁷ kJ/mol, extrem klein); (2) zirkular-polarisiertes UV-Licht aus dem Sternenumfeld (asymmetrische Photolyse); (3) meteoritische Aminosäuren zeigen L-Überschuss bis 15 % (Murchison, Ivuna); das Soai-Reaktion (1995) zeigt chemische Amplifikation: autokatalytische Alkylierung eines Pyrimidinaldehyds amplificiert ee von 0,1 % auf > 99 % in wenigen Cyclen",
        answerC = "Homochiralität ist kein Rätsel: Unter prä-biotischen Bedingungen bilden Aminosäuren ausschließlich L-Formen, da Glycin (das einzige achirale Protein-Aminosäure) als chiraler Template für alle anderen Aminosäure-Synthesen dient; die D-Zucker entstanden aus L-Aminosäuren durch Retro-Aldol-Kondensation",
        answerD = "Der einzig nachgewiesene Mechanismus für Homochiralität ist die Rankine-Wellen-Polarisation kosmischer Hintergrundstrahlung: Zirkular polarisierte Mikrowellen-Strahlung des CMB katalysiert L-Aminosäure-Synthese spezifisch in Bereichen mit CMB-Fluss > 10⁻²² W/cm²",
        correctAnswer = 1, // B
        explanation = "Das PVED (Mason & Tranter, 1985): Elektron-Schwachkraft-Wechselwirkung macht L-Aminosäuren um ~10⁻¹⁷ kJ/mol stabiler als D-Antipoden — zu klein für direkten Einfluss. Zirkular-polarisiertes UV (Griesbeck): Asymmetrische Photolyse kann bis zu 2% ee erzeugen. Meteoriten: L-Isovalin (nicht proteinogen) in Murchison bis 18% ee (Cronin & Pizzarello 1997) — kosmischer Beitrag möglich. Soai-Reaktion (Science 1995): Diisopropylzink + Pyrimidinaldehyd mit winzigem ee-Initiator → autokatalytische Amplifikation auf > 99% ee in 5 Cyclen. Erdapothekenmechanismus: Eutectic-Kristallisation und Evaporation können ee von 50% auf >99% verstärken (Blackmond 2004).",
        difficulty = 5,
        funFact = "Die Soai-Reaktion ist die einzige bekannte chemische Reaktion, die ihre eigenen chiralen Katalysatoren autokatalytisch aufbaut und dabei winzige chirale Überschüsse (< 0,001%) auf nahezu enantiomerreine Produkte (> 99%) amplifiziert — ein chemisches Gedächtnis-Phänomen."
    ),

    // Question 50 – Last Universal Common Ancestor
    Question(
        categoryId = 2,
        questionText = "Der letzte universelle gemeinsame Vorfahre (LUCA) aller heutigen Lebewesen war kein primitiver Protobiont, sondern möglicherweise ein bereits komplexer Organismus. Welche Genomanalysen stützen diesen Befund, und welche Lebensbedingungen werden für LUCA rekonstruiert?",
        answerA = "LUCA war ein einfaches RNA-Molekül ohne Proteinsynthese; phylogenetische Analyse zeigt, dass LUCA nur 3 universelle Gene kodierte (rRNA, DNA-Replikase, Proteinsynthese-Kernenzym); LUCA lebte in flachen warmen Meeresgebieten bei pH 7 und 25°C",
        answerB = "Phylogenomische Analysen (Weiss et al. 2016, Nature Microbiology) rekonstruierten ~355 LUCA-Proteinfamilien aus dem letzten gemeinsamen Vorfahren von Bacteria und Archaea: LUCA kodierte für Holzgas-Stoffwechsel (CO₂ + H₂ → Acetyl-CoA, Wood-Ljungdahl-Weg), Wolframabhängige Enzyme, Eisen-Schwefel-Cluster, gerichteten Ionengradienten (Na⁺ oder H⁺) und war wahrscheinlich ein anaerober chemo-lithotropher Organismus in heißen hydrothermalen Quellen",
        answerC = "LUCA-Rekonstruktion durch Genomanalyse ist unmöglich, da horizontaler Gentransfer (HGT) alle phylogenetischen Signale verwischt hat; die einzige verlässliche Schlussfolgerung über LUCA ist die universelle Verwendung des Standardgenetischen Codes, was auf gemeinsamen Ursprung hinweist",
        answerD = "LUCA war ein photosynthetischer Organismus, wie aus der universellen Konservierung der Chlorophyll-Synthesegene in allen drei Domänen folgt; LUCA lebte an der Oberfläche eines frühen Ozeans mit intensiver UV-Strahlung, die den Photosyntheseapparat antrieb und gleichzeitig zur DNA-Schäden-Reparatur-Konservierung führte",
        correctAnswer = 1, // B
        explanation = "Weiss et al. (Nature Microbiology, 2016) analysierten 6,1 Millionen Gensequenzen aus 1981 Genomen; ~355 Proteinfamilien sind in vertikal-vererbten Phylogenien strikt auf LUCA zurückführbar. Ergebnis: LUCA hatte Wood-Ljungdahl-CO₂-Fixierungsweg (acetogens + methanogens teilen ihn — er ist urzeitlich), Eisen-Schwefel-Cluster-Proteine (Ferredoxine), Nitrogenase-Homologe, Wolframhaltige Enzyme (thermophile Marker), NADH-Dehydrogenase (Ionengradient), Translationsmaschinerie. Kein Glykolyse, kein Citratzyklus, kein Malonyl-CoA — diese entstanden später. Schlussfolgerung: LUCA war ein anaerober, thermophiler (80-100°C), H₂-abhängiger Lithotroph — konsistent mit alkalin-hydrothermalen Quellen (Lost City-Typ).",
        difficulty = 5,
        funFact = "Das 'Lost City'-Hydrothermalfeld (im Atlantik, entdeckt 2000) produziert H₂ und CH₄ durch Serpentinisierung ohne vulkanische Wärme — es ist heute das beste Modell für LUCAs Geburtsort und zeigt, dass Leben möglicherweise auf der Basis von H₂-Chemie in alkalischen Quellen entstanden ist."
    )
)
