package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun scienceQuestionsMaster3(): List<Question> = listOf(

    // ── MASTER 3 (difficulty = 5) ── 50 questions ────────────────────────────

    // ── BLOCK 1: Quantum Gravity Approaches ──────────────────────────────────

    // Question 1 – Loop Quantum Gravity: Spin Networks
    Question(
        categoryId = 2,
        questionText = "In der Schleifenquantengravitation (LQG) sind Spin-Netzwerke die kinematischen Quantenzustände des Gravitationsfeldes. Was repräsentieren die Knoten und Kanten eines Spin-Netzwerks physikalisch?",
        answerA = "Knoten repräsentieren Raum-Zeit-Ereignisse; Kanten kodieren die Kausalstruktur zwischen ihnen und tragen keine Quantenzahlen",
        answerB = "Knoten repräsentieren Volumenpakete des Raumes mit zugehörigen Volumeneigenwerten; Kanten repräsentieren Flächen zwischen benachbarten Volumina und tragen SU(2)-Spindarstellungen j, die den Flächeninhalt quantisieren",
        answerC = "Knoten sind Partonen des Gravitons; Kanten kodieren Graviton-Propagatoren im störungstheoretischen Grenzfall",
        answerD = "Knoten repräsentieren Materiefelder; Kanten sind gravitative Wechselwirkungslinien analog zu Feynman-Diagrammen",
        correctAnswer = 1, // B
        explanation = "In LQG (Rovelli, Smolin, Ashtekar) entsprechen Spin-Netzwerk-Knoten Volumenelementen des Raumes; der Volumeneigenwert hängt von den intertwiner-Darstellungen ab. Kanten tragen halbzahlige Spindarstellungen j und quantisieren den Flächeninhalt: A = 8πγℓ_P² Σ √(j(j+1)). Der Barbero-Immirzi-Parameter γ erscheint explizit. Diese diskrete Raumgeometrie ist Träger des Hilbert-Raums der kinematischen LQG.",
        difficulty = 5,
        funFact = "Der minimale nicht-triviale Flächeneigenwert in LQG beträgt A_min = 4π√3 γ ℓ_P² ≈ 10⁻⁷⁰ m² – eine der kleinsten sinnvollen physikalischen Flächen überhaupt."
    ),

    // Question 2 – Causal Dynamical Triangulations
    Question(
        categoryId = 2,
        questionText = "Kausale Dynamische Triangulierungen (CDT) unterscheiden sich von euklidischen Quantengravitations-Ansätzen durch eine entscheidende Einschränkung. Welche ist das, und welches Ergebnis hat dieser Ansatz für die großskalige Raumzeitdimension erbracht?",
        answerA = "CDT verbietet Topologieänderungen der Raumzeit; die effektive Hausdorff-Dimension bleibt bei allen Skalen konstant bei 4",
        answerB = "CDT erzwingt eine kausale Ordnungsstruktur (Zeitschichtung), die Kausalitätsverletzungen verhindert; bei großen Skalen emergiert eine 4-dimensionale de-Sitter-artige Geometrie, während die spektrale Dimension bei Planck-Skalen auf etwa 2 abfällt",
        answerC = "CDT quantisiert nur die Raumteile der Metrik; die Zeitkoordinate bleibt klassisch und die emergente Dimension ist stets ganzzahlig",
        answerD = "CDT verwendet Penrose-Twistoren statt Simplizes und reproduziert die Schwarzschild-Metrik im Niedrigenergielimes",
        correctAnswer = 1, // B
        explanation = "CDT (Ambjørn, Jurkiewicz, Loll) schreibt vor, dass simpliziale Raumzeit-Bausteine kausal geordnet sind – Foliation-invariante Zeitschichten verhindern 'baby universes' und Kausalitätsverletzungen. Monte-Carlo-Simulationen zeigen: Bei großen Skalen emergiert eine 4D de-Sitter-Geometrie (positive kosmologische Konstante). Die spektrale Dimension (aus Diffusion auf der Geometrie gemessen) beträgt bei Planck-Skalen ~2, was auf eine fundamentale 2D-Quantengravitation hindeutet – ein Resultat, das mit LQG übereinstimmt.",
        difficulty = 5,
        funFact = null
    ),

    // Question 3 – Spin Foam Models
    Question(
        categoryId = 2,
        questionText = "Das EPRL-Spin-Foam-Modell (Engle-Pereira-Rovelli-Livine) ist der führende kovariante Quantengravitationsansatz. Welche Einschränkung (Simplizitätsbedingung) implementiert es, und warum ist sie für den Zusammenhang mit klassischer Gravitation entscheidend?",
        answerA = "Das EPRL-Modell projiziert auf flache Simplizes; die Simplizitätsbedingung erzwingt verschwindende Krümmung und verbindet so mit der linearisierten Einsteinschen Gravitation",
        answerB = "Die EPRL-Simplizitätsbedingung reduziert die BF-Theorie (topologische Feldtheorie) auf Einsteingravitation, indem der B-Bivektorf als Keilprodukt zweier Tetradenvektoren geschrieben wird (B = e∧e); dies erzeugt korrekten Vertexamplituden aus SL(2,C)-Darstellungen",
        answerC = "Die Simplizitätsbedingung identifiziert BF-Theorie mit Yang-Mills-Theorie; der Kontinuumslimes ergibt die Palatini-Wirkung mit topologischem θ-Term",
        answerD = "EPRL verwendet SU(3)-Darstellungen und implementiert die Simplizitätsbedingung als Casimir-Gleichung; der klassische Grenzwert liefert Randall-Sundrum-Gravitation",
        correctAnswer = 1, // B
        explanation = "BF-Theorie ist eine topologische Feldtheorie ohne lokale Freiheitsgrade. Einsteingravitation entsteht daraus durch die Simplizitätsbedingung B^{IJ} = ε^{IJKL}(e_K∧e_L), d.h. B muss ein Keilprodukt zweier Vierbeinen sein. Das EPRL-Modell implementiert dies auf quantenmechanischer Ebene durch lineare Simplizitätsbedingungen für den Normalenvektor der Dreiecke. Die Vertexamplitude faktorisiert in SL(2,C)-Darstellungen mit dem Immirzi-Parameter γ, der SU(2)-LQG-Zustände in SL(2,C)-Spin-Foams einbettet.",
        difficulty = 5,
        funFact = "Spin-Foam-Modelle wurden ursprünglich von John Baez als höher-kategoriale Verallgemeinerung von Feynman-Diagrammen konzipiert – wo Feynman-Graphen 1D-Netzwerke sind, sind Spin-Foams 2D-Zellkomplexe."
    ),

    // Question 4 – Planck-Scale Physics
    Question(
        categoryId = 2,
        questionText = "Das Generalized Uncertainty Principle (GUP) ist eine quantengravitativ motivierte Modifikation der Heisenbergschen Unschärferelation. In seiner einfachsten Form lautet es Δx Δp ≥ ℏ/2 (1 + β(Δp)²/m_P²c²). Was ist die physikalische Konsequenz für Messungen bei Planck-Skalen?",
        answerA = "GUP impliziert, dass Impulsmessungen bei Planck-Skalen beliebig präzise werden, da die Ortsunschärfe divergiert und keinen physikalischen Inhalt mehr trägt",
        answerB = "GUP impliziert eine minimale Ortsmesslänge der Größenordnung ℓ_P = √(ℏG/c³) ≈ 1.6×10⁻³⁵ m; Δx hat ein absolutes Minimum und kann nicht beliebig verkleinert werden, selbst bei beliebig großem Δp",
        answerC = "GUP modifiziert nur die Energieschalen subatomarer Teilchen; Ortsmessungen bleiben klassisch beliebig präzise",
        answerD = "GUP führt zu einer minimalen Zeitauflösung (Planck-Zeit), schränkt aber Ortsmessungen nicht ein",
        correctAnswer = 1, // B
        explanation = "Minimierung von Δx bzgl. Δp ergibt Δx_min = ℏ√β/m_Pc = √β ℓ_P. Dies ist ein fundamentales Messbarkeitsargument: Jede hochenergetische Sonde zur Ortsbestimmung erzeugt so viel Energie, dass sie eine kleine Schwarz-Loch-ähnliche Struktur bildet, die die Messgenauigkeit limitiert. Die GUP-Modifikation beeinflusst schwarze Löcher (Hagedorn-Temperatur, Planck-Reste), Thermodynamik und das Phasenraumvolumen (modifizierte Zustandsdichten).",
        difficulty = 5,
        funFact = null
    ),

    // Question 5 – Holographic Principle / Bekenstein Bound
    Question(
        categoryId = 2,
        questionText = "Das holographische Prinzip (t'Hooft, Susskind) besagt, dass die maximale Information in einem Raumvolumen durch seine Grenzfläche kodiert wird. Was ist die Bekenstein-Schranke für die maximale Entropie S einer Kugel mit Radius R und Energie E?",
        answerA = "S ≤ 2πRE/(ℏc), was bedeutet dass die Entropie proportional zum Volumen R³ skaliert",
        answerB = "S ≤ 2πkBRE/(ℏc), was die Entropie auf einen Wert proportional zur Oberfläche R² begrenzt; schwarze Löcher saturieren diese Schranke mit S_BH = kB A/(4ℓ_P²)",
        answerC = "S ≤ πkBR²E/(ℏc)²; die Schranke impliziert, dass Informationsdichte im Volumen beliebig groß werden kann",
        answerD = "S ≤ 4kBR³E/(3ℏc³); schwarze Löcher unterschreiten diese Schranke um einen Faktor 4π",
        correctAnswer = 1, // B
        explanation = "Bekensteins universelle Entropieschranke lautet S ≤ 2πkBRE/(ℏc). Schwarze Löcher saturieren exakt die verwandte Bekenstein-Hawking-Entropie S_BH = kB c³A/(4Gℏ) = kBA/(4ℓ_P²), wobei A die Horizontfläche ist. Die holographische Schranke (t'Hooft, Bousso) besagt allgemeiner: Die maximale Information in einem Raumgebiet beträgt A/(4ℓ_P²) Bits, wobei A die umhüllende Fläche ist – Information wird also 2D-'holographisch' gespeichert, nicht 3D-volumetrisch.",
        difficulty = 5,
        funFact = "Jacob Bekenstein wurde ursprünglich von Stephen Hawking kritisiert, als er 1972 Schwarzen Löchern Entropie zuschrieb. Hawkings eigene Entdeckung der Hawking-Strahlung 1974 bewies dann Bekensteins Recht – ein berühmter Fall wissenschaftlicher Selbstkorrektur."
    ),

    // ── BLOCK 2: Advanced Epigenetics ────────────────────────────────────────

    // Question 6 – Pioneer Transcription Factors
    Question(
        categoryId = 2,
        questionText = "Pioneer-Transkriptionsfaktoren wie FOXA1 können kompaktes Heterochromatin binden und öffnen. Welcher molekulare Mechanismus unterscheidet sie von konventionellen Transkriptionsfaktoren bezüglich Nukleosomenzugang?",
        answerA = "Pioneer-Faktoren rekrutieren immer zunächst den SWI/SNF-Komplex, der erst das Nukleosom entfernt; danach kann der Pioneer-Faktor binden",
        answerB = "Pioneer-Faktoren besitzen Winged-Helix- oder Forkhead-Domänen mit intrinsischer Fähigkeit, partielle Nukleosomenmotive auf kompaktierter DNA direkt zu erkennen und zu binden, ohne vorherige Chromatinöffnung; sie wirken als epigenetische 'Wegbereiter' für nachfolgende kooperative Faktoren",
        answerC = "Pioneer-Faktoren binden ausschließlich nackte linker-DNA zwischen Nukleosomen und entfalten ihre Wirkung indirekt durch Rekrutierung von HAT-Komplexen",
        answerD = "Pioneer-Faktoren sind identisch mit Chromoregulator-Proteinen und binden H3K27me3-markierte Nukleosomen mit höherer Affinität als unmarkierte",
        correctAnswer = 1, // B
        explanation = "FOXA1 und ähnliche Pioneer-Faktoren (OCT4, SOX2, GATA) besitzen strukturelle Merkmale, die Kontakt mit der DNA auf der Nukleosomenoberfläche ermöglichen. Die Forkhead-Domäne von FOXA1 ähnelt dem Linker-Histon H1 und kann ins kompakte Chromatin eindringen, ohne den Histonoktamer zu verdrängen. Dieser erste Bindungsschritt destabilisiert das Nukleosom lokal, ermöglicht H3-H4-Tetramer-Rotation und schafft so Zugang für nachfolgende Faktor-Bindung. Dies ist mechanistisch verschieden von ATP-abhängigem Chromatin-Remodeling.",
        difficulty = 5,
        funFact = "FOXA1 spielt eine Schlüsselrolle bei der Reprogrammierung zu induzierten pluripotenten Stammzellen (iPSC) zusammen mit OCT4, SOX2 und KLF4 (Yamanaka-Faktoren), die 2012 den Nobelpreis einbrachten."
    ),

    // Question 7 – Phase Separation in Gene Regulation
    Question(
        categoryId = 2,
        questionText = "Transkriptionelle Kondensate (Super-Enhancer-Kondensate) entstehen durch Flüssig-Flüssig-Phasentrennung (LLPS). Welche molekularen Treiber und physikalischen Parameter bestimmen das Phasendiagramm solcher Transkriptions-Kondensate?",
        answerA = "LLPS wird ausschließlich durch histonmodifikations-abhängige Valenzänderungen getrieben; Proteinkonzentration und Temperatur spielen keine Rolle",
        answerB = "Treiber sind intrinsisch unstrukturierte Regionen (IDRs) mit π-π-Wechselwirkungen (Tyrosin/Phenylalanin), elektrostatische Wechselwirkungen und RNA-Gehalt; das Phasendiagramm zeigt einen kritischen Punkt (ρ_c, T_c) mit Koexistenzregion; Sättigungskonzentration c_sat bestimmt den Kondensateintritt",
        answerC = "LLPS in Super-Enhancern wird durch Zink-Finger-Domänen vermittelt; das Phasendiagramm folgt klassischer Flory-Huggins-Theorie für Polymerlösungen ohne spezifische Proteinwechselwirkungen",
        answerD = "Transkriptionskondensate entstehen durch Protein-Aggregation (Amyloid-ähnliche Fibrillen) und sind irreversibel; sie unterscheiden sich grundlegend von reversiblem LLPS",
        correctAnswer = 1, // B
        explanation = "LLPS in Transkriptionskondensaten (Boija, Sharp, Bhatt et al., Science 2018) wird durch niederkomplexe IDR-Regionen in Aktivatoren (MED1, BRD4) und RNA-Pol II CTD getrieben. π-π-Stacking zwischen aromatischen IDR-Resten und kation-π-Wechselwirkungen sind zentrale molekulare Kräfte. Das Phasendiagramm folgt einem Flory-Huggins-ähnlichen Modell mit Binodal- und Spinodal-Kurve; die Sättigungskonzentration c_sat trennt disperse von kondensierter Phase. RNA kann das Phasendiagramm durch RNA-Proteinkontakte verschieben (RNA-vermitteltes Kondensatauflösung bei hoher RNA-Konzentration).",
        difficulty = 5,
        funFact = null
    ),

    // Question 8 – Enhancer RNA (eRNA)
    Question(
        categoryId = 2,
        questionText = "Enhancer-RNAs (eRNAs) werden von aktiven Enhancer-Elementen bidirektional transkribiert. Welche funktionellen Evidenzen sprechen dafür, dass eRNAs kausale Rollen bei der Genaktivierung spielen, und nicht lediglich Nebenprodukte offenen Chromatins sind?",
        answerA = "eRNAs sind immer polyadenyliert und nukleosomisch verpackt; ihre Funktion ist identisch mit langen nicht-kodierenden RNAs (lncRNAs) an Promotoren",
        answerB = "Acute Depletion-Experimente (Auxin-inducible Degron, dCas13-Knockdown, ASO-Knockdown) zeigen Reduktion von Zielgen-Expression ohne gleichzeitige Chromatinöffnung; eRNAs facilitieren Enhancer-Promotor-Kontakt durch Stabilisierung von Cohesin/Mediator an CTCF-Anker-Punkten und direkte Interaktion mit dem Mediator-Komplex",
        answerC = "eRNAs wirken ausschließlich als konkurrierende endogene RNAs (ceRNAs) durch microRNA-Sequestration; ihre Wirkung ist vollständig durch miRNA-Sponging erklärbar",
        answerD = "eRNAs aktivieren Gene ausschließlich durch direkte Basenpaarung mit Promotor-DNA im R-Loop-Format und haben keine Protein-Interaktionsdomänen",
        correctAnswer = 1, // B
        explanation = "Mehrere Mechanismen unterstützen kausale eRNA-Funktion: (1) Cohesin-Rekrutierung/-Stabilisierung durch eRNA-Cohesin-Direktbindung fördert chromosomale Loops; (2) eRNAs binden direkt MED12/MED1 im Mediator-Komplex; (3) Pol II-Pause-Release wird durch eRNA-P-TEFb-Rekrutierung gefördert; (4) Acute Depletion (nicht nur Chromatinschließung) reduziert Zielgen-Expression. Entscheidend: eRNA-Knockdown kann bei erhaltener Histonacetylierung am Enhancer die Zielgen-Expression reduzieren, was die direkte RNA-Funktion jenseits der Chromatin-Zugänglichkeit belegt.",
        difficulty = 5,
        funFact = "Der Begriff 'eRNA' wurde 2010 von Lim et al. und Kim et al. (Science/Cell) geprägt – die Entdeckung war überraschend, da Enhancer bis dahin als reine DNA-Elemente ohne eigene RNA-Produktion galten."
    ),

    // Question 9 – Hi-C and 3D Genome Organization
    Question(
        categoryId = 2,
        questionText = "In-situ Hi-C-Experimente zeigen, dass Chromosomen in Topologically Associating Domains (TADs) organisiert sind. Welche biochemischen und biophysikalischen Mechanismen erklären TAD-Entstehung, und was passiert bei CTCF-Motiv-Deletion experimentell?",
        answerA = "TADs entstehen ausschließlich durch Histonmodifikationen (H3K27ac-Grenzen); CTCF-Motiv-Deletion hat keinen messbaren Einfluss auf TAD-Grenzen",
        answerB = "TADs entstehen durch Loop-Extrusion (Cohesin extrudiert Chromatinschleifen bis es auf konvergierende CTCF-Bindestellen trifft); CTCF-Motiv-Inversion verursacht TAD-Fusion oder -Umstrukturierung und kann ektopische Enhancer-Promotor-Kontakte erzeugen, die zu Fehlregulation führen",
        answerC = "TADs sind reine Artefakte der Hi-C-Quervernetzung durch Formaldehyd; in lebenden Zellen existieren sie nicht und entstehen durch fixierungsbedingte Aggregation aktiver Chromosomenregionen",
        answerD = "TADs entstehen durch Polymerphysik (entropische Abstoßung zwischen Chromatindomänen) und sind vollständig CTCF-unabhängig; Cohesin hat ausschließlich DNA-Reparatur-Funktionen",
        correctAnswer = 1, // B
        explanation = "Das Loop-Extrusions-Modell (Fudenberg et al., Alipour & Marko) erklärt TADs mechanistisch: Cohesin-Komplexe extrudieren Chromatinschleifen prozessiv; CTCF als Barriere stoppt Extrusion bei konvergierenden (→←) CTCF-Motiven. Experimentell (Rao et al. 2014, Nora et al. 2017): CTCF-Depletion verursacht TAD-Auflösung; CTCF-Motiv-Inversion fusioniert benachbarte TADs (Lupianez et al. 2015, Nature); ektopische Kontakte aktivieren Proto-Onkogene. Wapl-Depletion (Cohesin-Entfernungsfaktor) verlängert Schleifen über TAD-Grenzen hinaus.",
        difficulty = 5,
        funFact = "Die Deletion einer einzelnen CTCF-Bindestelle im Limb-Enhancer-Cluster kann beim Maus-Embryo zu schweren Polydaktylie-Fehlbildungen führen – ein eindrucksvolles Beispiel für die medizinische Relevanz von 3D-Genomorganisation."
    ),

    // Question 10 – Single-Cell Multi-Omics
    Question(
        categoryId = 2,
        questionText = "SHARE-seq und ähnliche Einzelzell-Multiomics-Methoden messen gleichzeitig Chromatin-Zugänglichkeit (scATAC-seq) und Genexpression (scRNA-seq) in derselben Zelle. Welches bioinformatische Problem macht die Integration beider Modalitäten besonders herausfordernd?",
        answerA = "Das Hauptproblem ist die unterschiedliche Sequenziertiefe; es wird durch einfache Normalisierung auf Reads per Million gelöst",
        answerB = "Die Modalitäten liegen in verschiedenen Merkmalsräumen (Peaks vs. Gene) mit stark unterschiedlichen Sparsity-Profilen; Integration erfordert Methoden wie Latent Dirichlet Allocation, Weighted Nearest Neighbor (WNN, Seurat v4), oder Variational Autoencoders (MOFA+, scVI), die gemeinsame Latentzustände lernen, ohne biologisch bedeutsame Ko-Variation zu verlieren",
        answerC = "Das Problem ist technisch unlösbar; scATAC-seq und scRNA-seq können niemals in einer Einzelzelle gleichzeitig gemessen werden, da RNA-Extraktion Chromatin zerstört",
        answerD = "Integration erfordert ausschließlich kanonische Korrelationsanalyse (CCA); alle anderen Ansätze sind methodisch nicht valide und werden in der Literatur abgelehnt",
        correctAnswer = 1, // B
        explanation = "Einzelzell-Multiomics-Integration kämpft mit: (1) Hoher Sparsity in scATAC-seq (~1–5% detektierte Peaks) vs. moderater Sparsity in scRNA-seq; (2) inkompatiblen Merkmalsräumen (Hunderttausende Peaks vs. ~20.000 Gene); (3) Batch-Effekten beider Modalitäten gleichzeitig. WNN (Hao et al. 2021) gewichtet Modalitäten zelltypspezifisch. MOFA+ lernt gemeinsame latente Faktoren durch faktorielle Variationsanalyse. BABEL und ArchR-CICERO ermöglichen cross-modalitäts-Vorhersagen. Scinstinct und scMM nutzen Multi-View-Autoencoder für maximale biologische Interpretierbarkeit.",
        difficulty = 5,
        funFact = null
    ),

    // ── BLOCK 3: Advanced Nuclear Structure ──────────────────────────────────

    // Question 11 – Nuclear Deformation
    Question(
        categoryId = 2,
        questionText = "Rotationsanden in schweren Kernen werden mit der Nilsson-Modell-Beschreibung deformierter Kerne erklärt. Welcher Parameter β₂ charakterisiert quadrupolare Deformation, und was bedeuten positive bzw. negative Werte physikalisch?",
        answerA = "β₂ ist der Massenquadrupolmoment in atomaren Masseneinheiten; positive Werte beschreiben Kerne mit mehr Neutronen als Protonen",
        answerB = "β₂ ist der dimensionslose Quadrupoldeformationsparameter (Hill-Wheeler-Parameter); β₂ > 0 entspricht prolater Deformation (zigarrenförmig, lang entlang der Symmetrieachse), β₂ < 0 oblater Deformation (scheibenförmig, abgeflacht). Rotationsbanden entstehen bei starker Deformation durch kollektive Rotation des deformierten Kerns",
        answerC = "β₂ beschreibt die Neutronenhaut-Dicke; positive Werte indizieren Neutronenüberschuss, negative Protonenüberschuss",
        answerD = "β₂ ist der Deformationsparameter aus dem Liquid-Drop-Modell und beschreibt ausschließlich Spaltungsdeformation nahe dem Sattelpunkt",
        correctAnswer = 1, // B
        explanation = "Im kollektiven Modell (Bohr-Mottelson) wird die Kernoberfläche durch R(θ,φ) = R₀[1 + β₂Y₂⁰(θ,φ) + ...] beschrieben. Der Hill-Wheeler-Parameter β₂ quantifiziert quadrupolare Deformation: β₂ ≈ +0.3 bei ¹⁷⁶Hf (prolat), β₂ ≈ -0.25 bei ¹⁹²Hg (oblat). Rotationsbanden haben Energie E_I = (ℏ²/2I)·I(I+1) mit dem Trägheitsmoment I. Das Nilsson-Modell beschreibt Einteilchenzustände im anisotropen harmonischen Oszillator-Potenzial und erklärt die Häufung von Zuständen bei Deformationsquantenzahlen Ω.",
        difficulty = 5,
        funFact = "Der Kern ¹⁷⁸Hf im isomeren Zustand ¹⁷⁸ᵐ²Hf speichert bei 2.4 MeV Anregungsenergie mehr Energie pro Gramm als TNT – was zu kontroversen Diskussionen über Kernisomer-Waffen geführt hat, die jedoch physikalisch nicht realisierbar sind."
    ),

    // Question 12 – Superheavy Elements
    Question(
        categoryId = 2,
        questionText = "Superschwere Elemente (SHE, Z > 104) werden durch 'Insel der Stabilität' nahe magischen Zahlen stabilisiert. Welche Protonenzahl Z und Neutronenzahl N sagen theoretische Schalenmodellrechnungen als nächste doppelt-magische Konfiguration voraus?",
        answerA = "Z = 114, N = 184 ist der einzige theoretisch vorhergesagte stabilisierende Schalenschluss; höhere Elemente sind nicht untersucht",
        answerB = "Relativistische Mean-Field-Rechnungen und Makro-Mikro-Modelle sagen Z = 114 (oder 120/126) mit N = 184 als mögliche doppelt-magische Kombination voraus; die genaue Lage ist modellabhängig. Copernicium (Z=112) und Flerovium (Z=114) zeigen erste experimentelle Anzeichen erhöhter Stabilität",
        answerC = "Die Insel der Stabilität existiert ausschließlich bei Z = 100 (Fermium) mit N = 152; alle schwereren Elemente haben keine Schalenabschlüsse mehr",
        answerD = "Superschwere Elemente werden durch N = 184, Z = 132 stabilisiert; alle Kerne mit Z > 120 sind instantan instabil ohne messbare Halbwertszeiten",
        correctAnswer = 1, // B
        explanation = "Relativistische Hartree-Fock-Bogoliubov (RHF-B)- und Makro-Mikro-(Möller-Nix-)Rechnungen zeigen unterschiedliche Voraussagen: Nicht-relativistische Modelle bevorzugen Z=114, relativistische Modelle teils Z=120 oder Z=126. N=184 ist konsistent als magische Neutronenzahl vorhergesagt. Experimentell: ²⁸⁵Cn (Z=112) hat t₁/₂ ≈ 29s; ²⁸⁹Fl (Z=114) t₁/₂ ≈ 2.65s – deutlich länger als extrapolierte Trends. Die synthetischen Elemente bis Z=118 (Oganesson) wurden alle am JINR Dubna und GSI Darmstadt hergestellt.",
        difficulty = 5,
        funFact = "Oganesson (Og, Z=118) ist das schwerste bekannte Element und wurde nach dem Kernphysiker Yuri Oganessian benannt – einer von sehr wenigen lebenden Wissenschaftlern, die diese Ehre erfahren haben."
    ),

    // Question 13 – Nuclear Pasta
    Question(
        categoryId = 2,
        questionText = "Nukleares Pasta bezeichnet exotische Kernstrukturen in Neutronenstern-Krusten. Bei welchen Dichten entstehen diese Strukturen, und welche physikalischen Kräfte bestimmen die Phasenmorphologie?",
        answerA = "Nukleares Pasta entsteht bei Dichten nahe der normalen Kerndichte ρ₀ ≈ 2.3×10¹⁷ kg/m³; es besteht ausschließlich aus flüssigem Protonenmaterial",
        answerB = "Bei Dichten ~0.1–0.5 ρ₀ (Neutronensternkruste) konkurrieren Coulomb-Abstoßung (Protonen) und starke Kernkraft (Anziehung); die resultierende Gleichgewichtsmorphologie folgt der Kompetition beider Kräfte in verschiedenen Geometrien: Gnocchi (Kugeln) → Spaghetti (Zylinder) → Lasagne (Platten) → Antispaghetti → Antignocchi mit zunehmender Dichte",
        answerC = "Nukleares Pasta existiert nur in Neutronenstern-Kernen bei Dichten über 10 ρ₀ und besteht aus gebundenen Quark-Gluon-Clustern",
        answerD = "Pasta-Phasen entstehen bei allen Neutronenstern-Dichten gleichmäßig; die Form ist ausschließlich durch Temperatur (T > 10⁹ K) bestimmt, nicht durch Dichte",
        correctAnswer = 1, // B
        explanation = "In der Neutronensternkruste (0.001–0.5 ρ_sat) bestimmt das Verhältnis von Kernkraft-Reichweite (fm-Skala) zu Coulomb-Energie die Phasenmorphologie. Das Frustrated-System-Konzept (Ravenhall, Pethick, Wilson 1983; Hashimoto, Seki, Yamada): Protonenanteile in Kernmaterie minimieren Coulomb + Oberflächenenergie. Bei zunehmender Dichte: sphärische Nuclei → Zylinder ('Spaghetti') → Platten ('Lasagne') → inverse Zylinder ('Antispaghetti') → inverse Kugeln. Pasta-Phasen beeinflussen Neutronenstern-Kühlrate, elektrische Leitfähigkeit und Quake-Mechanismen gravitationswellen-relevanter Natur.",
        difficulty = 5,
        funFact = null
    ),

    // Question 14 – Neutron Skin Thickness
    Question(
        categoryId = 2,
        questionText = "Die Neutronenhaut-Dicke Δr_np = r_n - r_p in neutronenreichen Kernen ist direkt mit der nuklearen Symmetrie-Energie L verknüpft. Welche experimentelle Methode hat PREX-II (Jefferson Lab) genutzt, und welches Ergebnis für ²⁰⁸Pb ergab sich?",
        answerA = "PREX-II nutzte elastische Proton-Streuung bei 800 MeV; Δr_np(²⁰⁸Pb) = 0.05 ± 0.01 fm wurde bestimmt",
        answerB = "PREX-II nutzte paritätsverletzende elastische Elektronenstreuung (parity-violating electron scattering, PVES), bei der die schwache Wechselwirkung bevorzugt an Neutronen koppelt; Δr_np(²⁰⁸Pb) = 0.283 ± 0.071 fm – größer als viele Kernmodelle voraussagen, mit Implikationen für neutronenreiche Neutronensternmaterien",
        answerC = "PREX-II nutzte Antiproton-Annihilation an der Kernoberfläche; das Ergebnis Δr_np = 0.15 ± 0.02 fm ist konsistent mit allen Kernkraftmodellen",
        answerD = "PREX-II ist ein theoretisches Programm ohne experimentelle Komponente; Neutronenhäute werden ausschließlich durch Isotopieverschiebungsmessungen in der Atomspektroskopie bestimmt",
        correctAnswer = 1, // B
        explanation = "PVES nutzt die Tatsache, dass das Z-Boson (schwache Wechselwirkung) primär an Neutronen koppelt (schwache Ladung Q_W^n ≈ -1 vs. Q_W^p ≈ +0.07), während Photonen an Ladung koppeln. Die Paritätsverletzungs-Asymmetrie A_PV bei vorwärts-elastischer Streuung ist direkt proportional zum Formfaktor des Neutronenradius. PREX-2 (2021): Δr_np = 0.283±0.071 fm impliziert einen steifen Symmetrieenergie-Slope L ≈ 106 MeV, der in Spannung mit Neutronenstern-Radius-Messungen von NICER (L < 70 MeV) steht – ein ungelöstes Spannungsfeld.",
        difficulty = 5,
        funFact = "Die Auflösung des PREX-II/NICER-Widerspruchs könnte erfordern, dass die nuklearen Symmetrieenergie-Parameter stark isospin-abhängig sind – was tiefgreifende Konsequenzen für das Zustandsgleichungsmodell dichter Kernmaterie hätte."
    ),

    // Question 15 – Ab Initio Nuclear Theory
    Question(
        categoryId = 2,
        questionText = "Die No-Core Shell Model (NCSM)-Methode und Coupled-Cluster (CC)-Theorie ermöglichen ab-initio-Kernberechnungen ausgehend von Grundprinzipien. Welche Wechselwirkungsdarstellung ist dabei entscheidend, und warum musste die 'bare' Nukleon-Nukleon-Kraft transformiert werden?",
        answerA = "Ab-initio-Rechnungen verwenden nackte QCD-Gitter-Wechselwirkungen direkt; keine Transformation ist nötig, da moderne Prozessoren QCD-Berechnungen in Echtzeit lösen können",
        answerB = "Die starke kurzreichweitige Repulsion ('hard core') der nackten NN-Wechselwirkung (Argonne v18, CD-Bonn) verhindert Perturbationstheorie. Similarity Renormalization Group (SRG) oder Vlowk-Transformation bringen die Wechselwirkung in einen Niedrig-Impuls-Raum, der das Modell-Raumtrunkierungsproblem löst und schnellere Konvergenz ermöglicht; Dreikörperkräfte (3NF) sind dabei essenziell für korrekte Bindungsenergien und Radien",
        answerC = "Ab-initio-Kerntheorie verwendet ausschließlich phänomenologische Skyrme-Potenziale; der Begriff 'ab initio' bezieht sich auf das Ab-initio-Fitting der Skyrme-Parameter an experimentelle Daten",
        answerD = "Die SRG-Transformation ist nur für Leichtkerne (A < 4) notwendig; für mittelschwere und schwere Kerne ist das nackte Argonne-Potenzial direkt anwendbar",
        correctAnswer = 1, // B
        explanation = "Bare NN-Potenziale (χEFT N³LO, Argonne v18) haben starke Hoch-Impuls-Komponenten durch den kurzreichweitigen Kern. SRG-Evolution (Bogner, Schwenk, Furnstahl) unitär transformiert H zu H_λ mit Fluss-Parameter λ, der Kopplung zwischen Hoch- und Niedrig-Impuls-Zuständen unterdrückt: dH_λ/dλ = [η_λ, H_λ] mit Anti-hermitischem Generator η_λ = [T_rel, H_λ]. Dreikörperkräfte (3NF, entstehen ab N²LO in Chiral EFT) sind für Kernbindungsenergien, Saturation-Eigenschaft dichter Kernmaterie und Spin-Orbit-Aufspaltungen unverzichtbar – sie können nicht vernachlässigt werden.",
        difficulty = 5,
        funFact = null
    ),

    // ── BLOCK 4: Photochemistry & Femtochemistry ──────────────────────────────

    // Question 16 – Zewail Femtosecond Spectroscopy
    Question(
        categoryId = 2,
        questionText = "Ahmed Zewail erhielt 1999 den Chemie-Nobelpreis für die Echtzeitmessung chemischer Reaktionen auf der Femtosekundenzeitskala. Mit welcher experimentellen Technik und an welchem Modellsystem demonstrierte er erstmals die direkte Beobachtung eines Übergangszustands?",
        answerA = "Zewail verwendete Röntgen-Femtosekundenspektroskopie an Wasser-Protonentransfer; der Übergangszustand wurde durch EXAFS-Spektren nachgewiesen",
        answerB = "Zewail nutzte Pump-Probe-Femtosekundenspektroskopie (Pump: UV-Anregung, Probe: verzögertes Abfrageimpuls) mit Femtosekunden-Laserpulsen an der Dissoziation von Natriumiodid (NaI); transiente Absorptionsspektren zeigten die kohärente Wellenpaketdynamik auf dem dissoziativen Potenzial zwischen gebundenem und ionischem Zustand",
        answerC = "Zewail entwickelte CARS-Spektroskopie (Coherent Anti-Stokes Raman) mit Attosekundenpulsen und beobachtete erstmals die Bindungsbruchdynamik in HF-Molekülen",
        answerD = "Zewail verwendete NMR-Spektroskopie mit Pikosekunden-RF-Pulsen und zeigte Übergangszustände in Enzymreaktionen mit atomarer Auflösung",
        correctAnswer = 1, // B
        explanation = "Zewails Schlüsselexperiment (1987–1988): NaI-Dissoziation als Modell für ionisch-kovalente Kurvenkreuzung. UV-Pump-Puls (310 nm) erzeugt Na*···I Wellenpaket auf kovalenter Fläche. LIF-Probe-Puls beobachtet periodisches Erscheinen/Verschwinden freier Na-Atome: Wellenpaket oszilliert zwischen kovalenter (Na···I) und ionischer (Na⁺I⁻) Kurve, entweicht bei Jeder Kreuzungspassage partiell. Zeitkonstanten: Schwingungsperiode ~500 fs, Dissoziations-Tunnelzeit ~1 ps. Dies war die erste direkte Zeitaufnahme eines Übergangszustands – der 'Heilige Gral' der Chemie.",
        difficulty = 5,
        funFact = "Zewail verglich seine Femtochemie-Methodik mit einer ultra-hochgeschwindigkeitskamera, die Ereignisse filmt, die milliardenmal schneller sind als das Blinzeln eines Auges – ein Stroboskop für molekulare Bewegungen."
    ),

    // Question 17 – Conical Intersections
    Question(
        categoryId = 2,
        questionText = "Konische Durchschneidungen (Conical Intersections, CIs) sind entartet geometrische Punkte, an denen zwei Potenzialenergieflächrn berühren. Welche topologische Eigenschaft der elektronischen Wellenfunktion macht sie für die nichtadiabatische Photodynamik so bedeutend?",
        answerA = "An CIs haben beide Flächen identische Energie und Gradienten; Reaktionen vermeiden daher immer CIs durch adiabatische Umgehung",
        answerB = "CIs sind Punkte (in (3N-8)-dimensionalem Seam) mit degenerierter Energie; beim Umlaufen des CI-Punktes im Koordinatenraum akkumuliert die adiabatische elektronische Wellenfunktion eine geometrische Phase (Berry-Phase) von π, was antisymmetrisch macht und als 'geometric phase effect' reale Interferenzphänomene in Photodissoziations-Winkelverteilungen erzeugt",
        answerC = "CIs sind ausschließlich in linearen Molekülen in Form von Renner-Teller-Kopplung relevant; für nicht-lineare Moleküle existieren keine echten CIs",
        answerD = "Die Berry-Phase an CIs beträgt immer 2π (ganzzahliger Umlauf), was im Gegensatz zur geometrischen Phase bei magnetischen Monopolen steht und kein experimentell messbares Phänomen erzeugt",
        correctAnswer = 1, // B
        explanation = "Die CI-Geometrie (Branching-Raum bestehend aus Gradient-Differenz-Vektor g und Nichtadiabatischer-Kopplungs-Vektor h) bildet den Doppelkegel. Der Seam hat Dimensionalität (3N-8) für nicht-lineare N-atomige Moleküle. Die geometrische Phase (Longuet-Higgins, Berry): adiabatische Wellenfunktion ψ_el(r;R) ändert Vorzeichen beim Umlaufen des CI-Punktes im Kernkoordinatenraum R. Dies erzeugt Interferenzringe in Photodissoziations-Produktverteilungen (z.B. H + HD, gemessen von Althorpe et al. 2008) – direkter experimenteller Nachweis quantenmechanischer Interferenz durch geometrische Phase.",
        difficulty = 5,
        funFact = null
    ),

    // Question 18 – Photodynamic Therapy Mechanisms
    Question(
        categoryId = 2,
        questionText = "Photodynamische Therapie (PDT) verwendet Photosensibilisatoren (PS), Licht und molekularen Sauerstoff zur Tumorzerstörung. Was sind die beiden Reaktionsmechanismen (Typ I und Typ II), und welcher dominiert bei klinischen Photosensibilisatoren der 2. Generation?",
        answerA = "Typ-I: Direkte DNA-Photolyse durch UV-Absorption; Typ-II: Thermische Koagulation durch Infrarotabsorption. Klinische PS der 2. Generation nutzen Typ-I",
        answerB = "Typ-I: Radikalische Elektronen-/Wasserstofftransfer vom angeregten PS zu Substrat → Superoxid (O₂•⁻), H₂O₂, OH•; Typ-II: Energietransfer vom Triplett-PS auf O₂ → Singulett-Sauerstoff (¹O₂). Klinische 2.-Generation-PS (Porfimer, Foscan, Visudyne) arbeiten primär über Typ-II (¹O₂), da ¹O₂-Quantenausbeute bei diesen Verbindungen >0.5 und höchst reaktiv mit Biomolekülen (Lipiden, Proteinen, DNA)",
        answerC = "Beide Typ-I und Typ-II erzeugen identische ROS-Spektren; die Unterscheidung ist rein akademisch ohne therapeutische Relevanz",
        answerD = "Typ-I nutzt ausschließlich FRET (Förster-Resonanzenergietransfer) zur Schadensinduktion; Typ-II bezeichnet photosensibilisierte Photoisomerisierungen ohne Sauerstoffbeteiligung",
        correctAnswer = 1, // B
        explanation = "PS-Grundzustand (S₀) → S₁ (Absorption) → T₁ (intersystem crossing, rate k_ISC). Typ-II (dominanter Mechanismus in 2.-Generation-PS): T₁ + ³O₂ → S₀ + ¹O₂ (Energietransfer). ¹O₂ reagiert mit Tryptophan, Methionin, Guanin, ungesättigten Lipiden (Dien-ene Oxidation). ¹O₂-Lebensdauer in biologischen Systemen: ~3 µs (wässrig), Diffusionsradius ~10-150 nm → lokalisierende subzellulare Wirkung. Foscan (Temoporfin) hat ΦΔ > 0.89 (¹O₂-Quantenausbeute). Typ-I wird bei Hypoxie wichtiger (O₂-unabhängig), weshalb kombinierte PS entwickelt werden.",
        difficulty = 5,
        funFact = "Die erste photodynamische Therapie wurde 1903 vom dänischen Arzt Niels Finsen für Lupus vulgaris (Hauttuberkulose) entwickelt – wofür er den Nobelpreis für Medizin erhielt, noch bevor der Mechanismus über Singulett-Sauerstoff verstanden war."
    ),

    // Question 19 – Photocatalytic Water Splitting
    Question(
        categoryId = 2,
        questionText = "Photokatalytische Wasserspaltung für solare Wasserstoffgewinnung erfordert ein Halbleiter-Photokatalysator-System. Was sind die vier Grundvoraussetzungen (Bandlücken- und kinetische Kriterien) für effizienten Photokatalysator?",
        answerA = "Bandlücke zwischen 0.5 und 1.0 eV; Fermi-Niveau auf halber Höhe der Bandlücke; keine Oberflächenrekombination; stabiler Quantenwirkungsgrad > 99%",
        answerB = "Bandlücke 1.8–3.0 eV für ausreichende Solarspektrum-Absorption; Leitungsband-Minimum (CBM) negativer als H⁺/H₂ (-4.44 eV vs. Vakuum); Valenzband-Maximum (VBM) positiver als O₂/H₂O (+5.67 eV vs. Vakuum); und lange Ladungsträger-Lebensdauer bzw. Nutzung von Cocatalyst-Oberflächen (Pt, RuO₂) zur Kinetik-Beschleunigung der Redox-Halbreaktionen",
        answerC = "Jeder Halbleiter mit Bandlücke > 0 eV ist prinzipiell geeignet; entscheidend ist ausschließlich die Lichtintensität und Katalysatoroberfläche",
        answerD = "Photokatalytische Wasserspaltung erfordert immer pH 7 und ist nur mit TiO₂ als Photokatalysator industriell realisierbar",
        correctAnswer = 1, // B
        explanation = "Thermodynamik: ΔG° = +237 kJ/mol (1.23 eV pro Elektron) für H₂O → H₂ + ½O₂. Überspannung ~0.3–0.5 eV je Halbreaktion → Mindestbandlücke ~1.8 eV. Bandpositionen müssen: CBM > H⁺/H₂ Redoxpotenzial, VBM < O₂/H₂O Redoxpotenzial. GaN:ZnO-Festlösung (Domen), BiVO₄ (VBM-Anpassung), g-C₃N₄ (Bandlücke 2.7 eV) sind aktuelle Materialien. Vier-Photonen-Mechanismus Z-Scheme (analog Photosystem I+II) umgeht Bandlücken-Kompromiß: zwei Halbreaktions-Photokatalysatoren durch Redoxmediator verknüpft. Strahlungslose Rekombination ist der Hauptfeindesparadigma; Cocatalysts senken Überspannung und erhöhen Reaktionsrate.",
        difficulty = 5,
        funFact = null
    ),

    // Question 20 – Singlet Fission
    Question(
        categoryId = 2,
        questionText = "Singulett-Spaltung (Singlet Fission, SF) ist ein Photonenkonversionsprozess in organischen Molekülen. Welche spinstatistische Bedingung muss erfüllt sein, und welche Anwendung macht SF für Solarzellen interessant?",
        answerA = "SF erfordert Triplettenergie > Singulettenergie und ermöglicht es, ein Photon in zwei Infrarotphotonen umzuwandeln, was UV-empfindliche Solarzellen schützt",
        answerB = "SF erfordert 2E(T₁) ≤ E(S₁) (Energiebedingung); aus einem S₁-Zustand entstehen zwei Triplett-Exzitonen (T₁+T₁) über einen Zwischenkanal ¹(TT) mit Spin-0-Gesamt-Charakteristik. Dies überwindet prinzipiell die Shockley-Queisser-Grenze (~33%), da ein hochenergetisches Photon zwei Elektron-Loch-Paare erzeugt → Theoretische PCE-Grenze ~44%",
        answerC = "SF ist identisch mit Auger-Rekombination und reduziert immer die Solarzelleneffizienz; es gibt keine photovoltaischen Anwendungen",
        answerD = "SF erfordert E(S₁) ≤ E(T₁) und ist damit das energetische Gegenteil von Triplettfusion (TTA); beide Prozesse haben identische Quantenausbeuten",
        correctAnswer = 1, // B
        explanation = "SF: S₁ + S₀ → ¹(TT) → T₁ + T₁. Energiebedingung: E(S₁) ≥ 2E(T₁), exotherm oder thermoneutral. ¹(TT)-Zustand ist ein Correlated-Triplet-Paar mit Gesamtspin 0 (spinerlaubt). TIPS-Pentacen (E(S₁) ≈ 1.83 eV, 2E(T₁) ≈ 1.72 eV), Tetracen, Rubrene sind SF-aktive Materialien. Quantenausbeute von Triplett-Exzitonen: bis zu 200% (2 Tripletts pro Photon). Integration in Silizium-Tandem-Solarzellen (Downconversion): SF-Organikschicht absorbiert hochenergetische Photonen und sendet zwei Niederenergiephotonen an Si-Zelle → thermalisierungsverluste reduziert.",
        difficulty = 5,
        funFact = "Singlett-Spaltung wurde erstmals 1965 von Singh, Jones, Siebrand et al. in Acen-Kristallen beobachtet – mehr als 40 Jahre bevor das photovoltaische Potenzial erkannt wurde."
    ),

    // ── BLOCK 5: Advanced Polymer Physics ────────────────────────────────────

    // Question 21 – Reptation Theory
    Question(
        categoryId = 2,
        questionText = "Die Reptationstheorie (de Gennes 1971, Doi-Edwards) beschreibt die Dynamik verschlaufelter Polymerschmelzen. Was ist die charakteristische Zeitskala τ_rep und wie skaliert die Viskosität η mit dem Polymerisationsgrad N?",
        answerA = "τ_rep ~ N² (Rouse-Zeit); η ~ N² – identisch mit unverzweigten Ketten in verdünnter Lösung",
        answerB = "τ_rep ~ N³ ist die Reptationszeit, die für die Kette benötigt wird, um ihr eigenes 'Röhren'-Volumen zu verlassen; die Viskosität η ~ N³ (bzw. experimentell η ~ N³·⁴ durch Contour-Length-Fluctuations und Constraint-Release-Korrekturen). Daraus folgt der bekannte Skalenexponent ≈ 3.4 für η(N) in Polymerschmelzen",
        answerC = "τ_rep ~ N⁰·⁵ (subdiffusiv); η ist unabhängig von N oberhalb der Verschlaufungslänge N_e",
        answerD = "Reptationstheorie sagt τ_rep ~ N⁴ und η ~ N⁴ voraus; der experimentelle Exponent 3.4 ist ein Beweis dafür, dass die Theorie fundamental falsch ist",
        correctAnswer = 1, // B
        explanation = "De Gennes Reptationsröhren-Modell: Kette diffundiert eindimensional (Kriechbewegung) durch virtuelle Röhre aus Nachbarketten mit Röhrendiffusionskoeffizient D_c ~ 1/N. Röhrenlänge L ~ N. Erneuerungszeit: τ_rep = L²/D_c ~ N³. Doi-Edwards erweitern zu vollständiger rheologischer Theorie. Experimentell: η ~ N^{3.4} – Abweichung von N³ durch: (1) Contour-Length-Fluctuations (CLF): Kette fluktuiert entlang Röhre, effektiv verkürzt Pfad; (2) Constraint-Release (CR): Nachbarketten reptieren selbst, Röhre fluktuiert. Beide Korrekturterme erhöhen effektiven Skalenexponenten von 3 auf ~3.4.",
        difficulty = 5,
        funFact = "Pierre-Gilles de Gennes erhielt 1991 den Physiknobelpreis u.a. für die Reptationstheorie – und war für seine Analogiemethodik berühmt: er übertrug Konzepte aus Supraleitung auf Flüssigkristalle und Polymere."
    ),

    // Question 22 – Polymer Scaling Laws (de Gennes / Flory)
    Question(
        categoryId = 2,
        questionText = "Das Skalengesetz für den Gyrationsradius R_G eines Polymers in gutem Lösungsmittel lautet R_G ~ N^ν. Welchen Wert hat der Flory-Exponent ν in 3D, und wie ergibt er sich aus dem Flory-Argument (Minimierung der freien Energie)?",
        answerA = "ν = 0.5 in allen Dimensionen; das Flory-Argument liefert immer gaussisches Verhalten wegen entropischangepasster Statistik",
        answerB = "ν = 3/5 = 0.6 in 3D; das Flory-Argument minimiert F = F_rep + F_el = k_BT(N²b³/R³) + k_BT(R²/Nb²), was R ~ N^{3/5} ergibt. Exakter Renormierungsgruppen-Wert: ν ≈ 0.588",
        answerC = "ν = 3/4 in 3D; dieser Wert gilt universell für alle Polymere unabhängig von Lösungsmittelqualität und wird experimentell immer bestätigt",
        answerD = "ν = 1 in 3D (vollständig gestreckte Kette); das Flory-Argument berücksichtigt keine Entropie und gilt nur für steife Polymere (Persistenzlänge >> Konturlänge)",
        correctAnswer = 1, // B
        explanation = "Flory freie Energie: F_rep ≈ k_BT·v·N²/R³ (ausgeschlossenes Volumen v, Monomerdichte N/R³) + F_el ≈ k_BT·R²/(Nb²) (Entropieelastizität). Minimierung: ∂F/∂R = 0 → R* ~ N^{3/(d+2)} mit Raumdimension d. In 3D: ν_Flory = 3/5 = 0.6. Genaue RG-Rechnung (ε-Entwicklung): ν = 0.5876 (4-loop). Experimentell: Lichtstreuung, SAXS bestätigen ν ≈ 0.588. In 2D: ν = 3/4; in 1D: ν = 1; für d ≥ 4 gilt ν = 1/2 (Gaussische Kette, ausgeschlossenes Volumen irrelevant → obere kritische Dimension d_c = 4).",
        difficulty = 5,
        funFact = null
    ),

    // Question 23 – Block Copolymer Self-Assembly
    Question(
        categoryId = 2,
        questionText = "Block-Copolymere aus zwei inkompatiblen Blöcken A und B bilden Mikrophasenseparation. Welche Parameter steuern die Gleichgewichtsmorphologie (Lamellen, Zylinder, BCC-Kugeln, Gyroid), und was beschreibt das Phasendiagramm?",
        answerA = "Ausschließlich die Gesamtmolmasse M bestimmt die Morphologie; chemische Inkompatibilität spielt keine Rolle oberhalb der LCST",
        answerB = "Das Phasendiagramm wird durch χN (Produkt aus Flory-Huggins-Parameter χ und Polymerisationsgrad N) und den Volumenbruch f_A der A-Komponente parametrisiert. Schwache Segregation: χN ≈ 10.5 (kritischer Punkt für f=0.5); starke Segregation: χN >> 10.5. Morphologie-Sequenz: Kugeln (f < 0.15) → Zylinder (0.15 < f < 0.3) → Gyroid (0.3 < f < 0.35) → Lamellen (f ≈ 0.5) mit Spiegelsymmetrie um f=0.5",
        answerC = "Block-Copolymere bilden ausschließlich lamellare Morphologie; andere Strukturen (Zylinder, Gyroid, Kugeln) entstehen nur in ternären ABC-Triblock-Systemen",
        answerD = "Die Morphologie wird durch den Debye-Hückel-Parameter κ und die Ionenstärke der Lösung bestimmt; Block-Copolymere können nur in Elektrolytlösungen Mikrophasenseparation zeigen",
        correctAnswer = 1, // B
        explanation = "Leibler (1980) berechnete das Block-Copolymer-Phasendiagramm im Rahmen der Landau-Theorie. Ordnungsparameter: Dichtewelle δφ(r). Für symmetrischen Diblock (f=0.5): ODT-Übergang bei (χN)_ODT ≈ 10.495 (Mean-Field). Gyroid (Ia3̄d-Symmetrie) ist eine doppelt-periodische minimale Fläche mit exzellenter mechanischer Steifigkeit. Experimentell: SAXS bestimmt Periodizität L ~ N^(2/3) (schwache Segregation) oder L ~ N^(2/3)χ^(1/6) (starke Segregation). Anwendungen: Nanolithographie (DRAM, 14-nm-Knoten mit PS-b-PMMA), Nanoporen-Membranen, Photovoltaik.",
        difficulty = 5,
        funFact = "IBM verwendete Block-Copolymer-Self-Assembly als 'Directed Self-Assembly' (DSA)-Methode, um DRAM-Kondensatorlöcher mit Abständen unter 25 nm herzustellen – eine Schlüsseltechnologie für Halbleiterproduktion jenseits optischer Lithographiegrenzern."
    ),

    // Question 24 – Polymer Brushes
    Question(
        categoryId = 2,
        questionText = "Polymer-Bürsten entstehen durch dichte Pfropfung von Ketten auf Oberflächen. Das Alexander-de-Gennes-Modell beschreibt die osmotische Abstoßung zwischen zwei bürsten-beschichteten Oberflächen. Wie skaliert die Gleichgewichts-Bürstenhöhe h mit Pfropfdichte σ und Polymerisationsgrad N?",
        answerA = "h ~ N (vollständig gestreckte Ketten); Pfropfdichte σ hat keinen Einfluss auf die Bürstenhöhe in gutem Lösungsmittel",
        answerB = "h ~ N σ^(1/3) in gutem Lösungsmittel (Flory-Exponent ν=3/5 eingesetzt in Alexander-Modell); starke Abhängigkeit von σ: dichtere Bürste = stärker gestreckte Ketten. Kurze Abstandsabhängigkeit der Abstoßungskraft: F/A ~ (h/D)^(9/4) für D < 2h (Daumen-Finger-Kompression)",
        answerC = "h ~ N^(1/2) unabhängig von σ (gaussisches Verhalten); Polymer-Bürsten verhalten sich immer wie ideale Ketten ohne Wechselwirkungen",
        answerD = "h ~ N² σ für semidilute Bürsten; Abstoßungskraft skaliert linear mit Kompression (hookesches Gesetz wie feste Polymere)",
        correctAnswer = 1, // B
        explanation = "Alexander-Modell (1977) und de-Gennes-Blob-Theorie: Bürstenkette ist Folge von 'Blobs' mit Größe ξ ~ σ^(-1/2) (Abstand zwischen Pfropfpunkten). Innerhalb eines Blobs: ideale Statistik, N_blob ~ ξ^(1/ν) Monomere. Kette besteht aus N/N_blob Blobs → h ~ N/N_blob · ξ ~ N σ^{(2ν-1)/(2ν)} ~ N σ^{1/3} (für ν=3/5). Abstoßungspotenzial zwischen zwei Bürsten (Abstand D): U/A ~ k_BT/σ² · (2h/D)^(5/4) + ... Die Skalierung h ~ N σ^(1/3) wurde durch Neutronenreflektometrie und AFM-Kraftmessungen bestätigt. Anwendungen: Anti-Fouling-Beschichtungen (PEG-Bürsten), Steric-Stabilisation kolloidaler Dispersionen.",
        difficulty = 5,
        funFact = null
    ),

    // Question 25 – Conjugated Polymer Optoelectronics
    Question(
        categoryId = 2,
        questionText = "In konjugierten Polymeren (z.B. P3HT, PTB7) wird Ladungstransport durch Polaron-Hüpfen und intermolekularen π-π-Stacking beeinflusst. Was ist die entscheidende strukturelle Eigenschaft für hohen Löcher-Mobilität in OFETs und was beschreibt das energetische Unordnungs-Modell?",
        answerA = "Ladungstransport in konjugierten Polymeren folgt klassischem Metallleitungsmodell (Drude); π-Konjugation erzeugt Valenz- und Leitungsband analog zu Metallen",
        answerB = "Hohe Mobilität erfordert 'edge-on' oder 'face-on' π-π-Stacking-Orientierung der konjugierten Ebenen relativ zur Elektrode (face-on für OPV, edge-on für OFET); das Gaussian Disorder Model (GDM, Bässler) beschreibt Hopping-Transport in einem gaußförmig verteilten Zustandsdichte-DOS mit Unordnungsparameter σ; Mobilität steigt mit Temperatur als ln(μ) ~ -(2σ/3k_BT)²",
        answerC = "Konjugierte Polymere haben ausschließlich Elektronen-Transport (n-Typ); Löcher werden durch intrinsische p-Dotierung mit Luftsauerstoff transportiert, was nicht kontrollierbar ist",
        answerD = "π-π-Stacking ist für Ladungstransport kontraproduktiv; maximale Mobilität wird durch vollständig amorphe, π-konjugationsunterbrochene Polymere erreicht",
        correctAnswer = 1, // B
        explanation = "Konjugierte Polymere sind ungeordnete halbleitende Materialien mit energetischer (σ) und positionaler (Σ) Unordnung. GDM (Bässler 1993): Hüpfen zwischen lokalisierten Zuständen (Segmenten) mit Miller-Abrahams-Rate k_{ij} = ν₀exp(-2γΔr_{ij}) × exp(-ΔE_{ij}/k_BT). Morphologie-Kontrolle: face-on-Orientierung (π-Ebene parallel zu Substrat) maximiert π-π-Überlapp entlang Ladungstransportrichtung. P3HT: Regioregularität > 95% → Lamellar-π-Stacking, μ_h > 0.1 cm²/(Vs). OFET-relevante Orientierung: edge-on (π-Ebene senkrecht zur Schicht, π-π-Stacking in source-drain-Richtung). In-situ GIWAXS zeigt Orientierungskorrelation mit Mobilität.",
        difficulty = 5,
        funFact = "Alan Heeger, Alan MacDiarmid und Hideki Shirakawa erhielten 2000 den Chemie-Nobelpreis für die Entdeckung leitfähiger Polymere (Polyacetylen, 1977) – die erste konjugierte Polymermaterialklasse, die einen Metall-Isolator-Übergang durch Dotierung zeigte."
    ),

    // ── BLOCK 6: Computational Neuroscience ──────────────────────────────────

    // Question 26 – Hodgkin-Huxley Model
    Question(
        categoryId = 2,
        questionText = "Das Hodgkin-Huxley-Modell (1952) beschreibt Aktionspotenziale mit Gating-Variablen m, h (Na⁺) und n (K⁺). Welche biophysikalischen Mechanismen repräsentieren m, h und n, und warum erzeugt die Na⁺-Kanal-Inaktivierung (h-Variable) die Refraktärperiode?",
        answerA = "m, h, n sind reine Anpassungsparameter ohne biophysikalische Bedeutung; die Refraktärperiode entsteht durch vollständige K⁺-Kanal-Deaktivierung",
        answerB = "m (Aktivierung, 3. Potenz): Öffnungswahrscheinlichkeit des Na⁺-Aktivierungsgitters (schnell, depolarisierungsabhängig); h (Inaktivierung): Inaktivierungspartikel blockiert offenen Na⁺-Kanal bei anhaltender Depolarisierung (langsamer als m); n (K⁺-Aktivierung, 4. Potenz): verzögerte K⁺-Kanalöffnung. h sinkt bei Depolarisation auf 0 → Na⁺-Kanäle inaktivieren → absolute Refraktärperiode; h erholt sich nur bei Repolarisation",
        answerC = "m, h, n repräsentieren Ionenkonzentrations-Gradienten für Na⁺, K⁺ und Cl⁻; die Refraktärperiode entsteht durch Cl⁻-Influx der die Membranspannung negativiert",
        answerD = "Die HH-Gating-Variablen beschreiben Spannungsabhängige Phosphorylierungszustände der Kanalproteine; m ist die ATP-Bindungswahrscheinlichkeit",
        correctAnswer = 1, // B
        explanation = "HH-Formal: I_Na = ḡ_Na·m³h·(V-E_Na), I_K = ḡ_K·n⁴·(V-E_K). Gating: dm/dt = α_m(V)(1-m) - β_m(V)·m; analog für h und n mit verschiedenen Zeitkonstanten τ_m << τ_n < τ_h. Strukturelle Basis: m ~ Aktivierungs-Arginin-Helices (S4-Segment), h ~ N-terminaler Inaktivierungsball (ball-and-chain, Zagotta et al.). Absolute Refraktärperiode: h ≈ 0 nach AP; Na⁺-Kanäle in inaktiviertem Zustand, kein weiteres AP möglich unabhängig von Reizstärke. Relative Refraktärperiode: h teilweise erholt, K⁺-Leitfähigkeit noch erhöht → erhöhte Reizschwelle.",
        difficulty = 5,
        funFact = "Hodgkin und Huxley führten ihre Ableitungen 1952 an Tintenfisch-Riesenaxonen durch (Durchmesser ~1 mm), die groß genug für direkte Elektroden-Einführung waren. Die 1963 erhaltenen Nobelpreise waren für Messungen, die ohne Computertomographie, Patch-Clamp oder Molekularbiologie gemacht wurden."
    ),

    // Question 27 – Integrate-and-Fire Neurons
    Question(
        categoryId = 2,
        questionText = "Das Leaky Integrate-and-Fire (LIF) Modell ist die meistgenutzte vereinfachte Neuronenbeschreibung. Welches Problem des einfachen LIF-Modells löst das Adaptive Exponential Integrate-and-Fire (AdEx)-Modell, und welche biologische Phänomenologie wird damit reproduzierbar?",
        answerA = "AdEx löst das Problem fehlender synaptischer Zeitkonstanten; es reproduziert ausschließlich Bursting-Verhalten in Interneuronen",
        answerB = "LIF hat keine spike-initiierenden Mechanismus (sofortige Schwellenwert-Überschreitung) und keine Adaptation. AdEx fügt: (1) exponentiellen Na⁺-Aktivierungsterm (Δ_T·exp((V-V_T)/Δ_T)) für realistischen AP-Aufschwung; (2) Adaptationsstromvariable w (w̃-Dynamik) für spike-frequency-Adaptation, Bursting und verschiedene Feuerklassen (Regular-Spiking, Bursting, Fast-Spiking). Dies reproduziert Renaud-Brette-Klassen-Taxonomie kortikaler Neurone",
        answerC = "AdEx unterscheidet sich von LIF nur durch eine modifizierte Reset-Bedingung; es gibt keine dynamische Adaptationsvariable und keine Änderung der Spike-Form",
        answerD = "Das AdEx-Modell ersetzt alle Ionenkanal-Variablen des Hodgkin-Huxley-Modells durch eine einzige Variable und ist damit weniger realistisch als LIF",
        correctAnswer = 1, // B
        explanation = "AdEx (Brette-Gerstner 2005): C_m dV/dt = -g_L(V-E_L) + g_L·Δ_T·exp((V-V_T)/Δ_T) - w + I_ext; τ_w·dw/dt = a(V-E_L) - w. Die Exponentialterm (Δ_T, Slope-Faktor) modelliert kollektive Na⁺-Kanal-Öffnung und erzeugt realistischen AP-Aufschwung. Adaptationsstrom w wird bei jedem Spike um b erhöht (spike-triggered adaptation) und zerfällt mit τ_w. Parameter-Exploration reproduziert: Tonic-Spiking, Adaptation, Bursting, Initial-Burst, Irregular-Spiking → alle wichtigen kortikalen Neuronentypen. Numerisch effizient: 10.000× schneller als HH, biologisch realistischer als einfaches LIF.",
        difficulty = 5,
        funFact = null
    ),

    // Question 28 – Hebbian Plasticity and STDP
    Question(
        categoryId = 2,
        questionText = "Spike-Timing-Dependent Plasticity (STDP) ist eine biologisch realisierte Form Hebb'scher Plastizität. Welche mathematische Lernregel beschreibt STDP, und welche Konsequenz hat kausale vs. akausale Spike-Reihenfolge für die Synapsenstärke?",
        answerA = "STDP ändert Synapsengewichte proportional zur Feuerrate des Postsynaptischen Neurons; zeitliche Reihenfolge von Prä- und Postsynaptischem Feuern ist irrelevant",
        answerB = "STDP-Lernregel: Δw = A_+·exp(-|Δt|/τ_+) wenn Δt = t_post - t_pre > 0 (kausal, Potenzierung/LTP); Δw = -A_-·exp(-|Δt|/τ_-) wenn Δt < 0 (akausal, Abschwächung/LTD). Biologisch: präsynaptisches Feuern kurz vor postsynaptischem Feuern (Δt ∈ 0–20 ms) → LTP; umgekehrte Reihenfolge → LTD. Dies implementiert einen zeitlichen Kausalitätsdetektor",
        answerC = "STDP gilt nur für GABAerge Synapsen; glutamaterge Synapsen zeigen Frequenz-abhängige Plastizität (BCM-Regel), aber keine Timing-Abhängigkeit",
        answerD = "Die STDP-Zeitkonstanten τ_+ und τ_- sind immer identisch; das asymmetrische STDP-Fenster entsteht ausschließlich durch postsynaptische Ca²⁺-Diffusions-Verzögerungen",
        correctAnswer = 1, // B
        explanation = "STDP (Bi & Poo 1998, Magee & Johnston 1997) wurde in hippokampalen Neuronen experimentell entdeckt: Zeitfenster ±40 ms um Koinzidenz. Mechanistisch: NMDA-Rezeptor als Koinzidenzdetektor – bei kausaler Reihenfolge (prä vor post) ist Mg²⁺-Block bei postsynaptischer Depolarisierung aufgehoben → Ca²⁺-Influx → CaMKII-Aktivierung → AMPAR-Einbau (LTP). Akausale Reihenfolge: niedrige Ca²⁺-Konzentration → Phosphatase-Aktivierung (PP2B) → AMPAR-Endozytose (LTD). STDP-Regel implementiert Hebbs Postulat 'neurons that fire together, wire together' mit zeitlicher Präzision.",
        difficulty = 5,
        funFact = "Donald Hebb formulierte seine berühmte Synapsen-Lernregel 1949 in 'The Organization of Behavior', ohne einen einzigen direkten experimentellen Beweis zu haben – rein auf Basis neuroanatomischer Überlegungen. STDP lieferte 50 Jahre später die biophysikalische Grundlage."
    ),

    // Question 29 – Attractor Networks
    Question(
        categoryId = 2,
        questionText = "Das Hopfield-Netzwerk ist ein rekurrentes neuronales Netzwerk mit Attraktor-Dynamik. Welche Energiefunktion beschreibt die Netzwerkdynamik, und wie viele Muster können theoretisch gespeichert werden?",
        answerA = "Hopfield-Netzwerke haben keine Energiefunktion; sie konvergieren durch stochastischen Gradient-Descent zu gespeicherten Mustern. Maximale Kapazität: N² Muster bei N Neuronen",
        answerB = "Energie: E = -½ Σ_{ij} w_{ij}·s_i·s_j - Σ_i θ_i·s_i mit w_{ij} = (1/N)Σ_μ ξ^μ_i·ξ^μ_j (Hebb'sches Lernen). Kapazität: maximal ~0.138N Muster ohne Kreuz-Muster-Übersprechen (Amit-Gutfreund-Sompolinsky); Speicherkapazität skaliert linear mit N",
        answerC = "Energiefunktion: E = Σ_i s_i·(Σ_j w_ij·s_j); Netzwerk maximiert E durch asynchrones Update. Kapazität: 2N Muster durch binäre Darstellung",
        answerD = "Hopfield-Netzwerke sind vorwärtsgekoppelt (feed-forward); Attraktoren entstehen durch sequenzielle Verarbeitung, nicht durch rekurrente Dynamik",
        correctAnswer = 1, // B
        explanation = "Hopfield (1982): synchrones/asynchrones s_i ∈ {±1} Update → Energie E monoton sinkend → Konvergenz zu Attraktoren. E = -½Σw_{ij}s_is_j garantiert Konvergenz. Hebb-Lernen: w_{ij} = (1/N)Σ_μ ξ^μ_i ξ^μ_j (ξ^μ: gespeichertes Muster). Kapazitätsgrenze (AGS 1985): P_max ≈ 0.138N mit Fehlerquote → 0. Darüber: Phasenübergang zu 'Mischzuständen' und Gedächtnisfehlern. Modern: Dense Associative Memory (Krotov-Hopfield 2016) mit Exponenzial-Aktivierungsfunktionen erhöht Kapazität auf N^(K-1) für K-hot-Aktivierung.",
        difficulty = 5,
        funFact = null
    ),

    // Question 30 – Cortical Column Models
    Question(
        categoryId = 2,
        questionText = "Das kanonische kortikale Mikrozirkuit-Modell nach Potjans & Diesmann (2014) beschreibt vier laminäre Schichten (L2/3, L4, L5, L6) mit exzitatorischen und inhibitorischen Populationen. Welcher synaptische Eingangs-Pfad dominiert für sensorische Aufmerksamkeits-Modulation zwischen Feedforward- und Feedback-Projektionen?",
        answerA = "Feedforward-Input terminiert hauptsächlich in L1 (Feedforward) und Feedback in L6 (Feedback); L4 erhält ausschließlich intrakortikalen Input",
        answerB = "Feedforward-thalamokortikale Projektionen terminieren bevorzugt in L4 und L6; Feedback-kortikokortikale Projektionen terminieren bevorzugt in L1 und L2/3. Dies implementiert eine hierarchische Vorhersage-Fehler-Schaltung: L4 kodiert Bottom-up-Vorhersagefehler, L2/3 und L5 kodieren Top-down-Vorhersagen (Laminar Predictive Coding nach Bastos et al.)",
        answerC = "Alle kortikalen Projektionen terminieren gleichmäßig in allen Schichten; laminäre Spezifität ist ein Mythos ohne funktionelle Konsequenzen",
        answerD = "Feedforward-Projektionen enden ausschließlich in L5 (Pyramidenzellen); Feedback-Projektionen sind rein inhibitorisch und enden in L2/3-Interneuronen",
        correctAnswer = 1, // B
        explanation = "Anatomisch (Felleman & Van Essen 1991, Markov et al. 2014): Feedforward (FF): dünne Axone, L4-Zielschicht (und L6); Feedback (FB): dicke Axone, L1/2/3/5. Potjans-Diesmann-Modell: 4×2 Populationen (Exc/Inh in L2/3, L4, L5, L6), ~80k Neurone, sparsam verbunden. L4-Exc → L2/3-Exc (FF innerhalb Kolumne); L5/6 → höhere Areale (Ausgang). Bastos et al. (2012) Predictive-Coding-Laminar-Modell: oberflächliche Schichten (L2/3) projizieren Vorhersage-Fehler in FF-Richtung; tiefe Schichten (L5/6) projizieren Vorhersagen in FB-Richtung. β-Oszillationen kodieren FB, γ-Oszillationen FF (Richter et al. 2017).",
        difficulty = 5,
        funFact = "Die kortikale Kolumne als Funktionseinheit wurde 1957 von Vernon Mountcastle durch systematische Penetrationsmessungen im somatosensorischen Kortex der Katze entdeckt – eine Entdeckung, die das Paradigma der modularen Hirnorganisation begründete."
    ),

    // ── BLOCK 7: Deep Earth Geophysics ────────────────────────────────────────

    // Question 31 – D'' Layer
    Question(
        categoryId = 2,
        questionText = "Die D''-Schicht (D-doppelprime) ist die unterste ~200 km der unteren Mantelschicht direkt über dem äußeren Kern. Welche seismische Diskontinuität und Mineralphasenumwandlung charakterisiert sie?",
        answerA = "D'' zeigt eine S-Wellen-Geschwindigkeitsabnahme durch Perowskit-Auflösung; P-Wellen sind nicht betroffen",
        answerB = "D'' zeigt eine seismische Diskontinuität mit S-Wellen-Geschwindigkeitserhöhung (~1–3%), die durch den Phasenübergang von Magnesiumperovskit (Bridgmanit, MgSiO₃ Pv) zur Post-Perowskit-Phase (PPv, CaIrO₃-Struktur) bei ~125 GPa und ~2500 K erklärt wird; 'ULVZ' (Ultra Low Velocity Zones) mit 10–30% Vp-Reduktion nahe dem CMB deuten auf partielle Schmelze oder Fe-reiche Mineralien hin",
        answerC = "D'' ist eine vollständig isotrope Schicht ohne seismische Anisotropie; Anisotropie entsteht erst im äußeren Kern durch Konvektion",
        answerD = "Die D''-Diskontinuität ist identisch mit dem 660-km-Übergang und bezeichnet ausschließlich den Ringwoodit-Perowskit-Übergang in der Transition-Zone",
        correctAnswer = 1, // B
        explanation = "Post-Perowskit (PPv) Entdeckung: Murakami et al. (2004) erbrachten Laser-Heated DAC (Diamond Anvil Cell)-Evidenz bei 125 GPa / 2500 K. PPv-Struktur: Schichtstruktur (Cmcm-Symmetrie), stärker anisotrop als Pv. Seismisch: S-Wellen-Diskontinuität in D'' durch Pv→PPv erklärt (ULVZ-Regionen: ΔVs ~ -30%, ΔVp ~ -10% über wenige km Dicke). ULVZ-Hypothesen: (1) Eisenreiche Oxide, (2) partielle Schmelze, (3) subozeanische Slabs. Azimutale und radiale S-Wellen-Anisotropie in D'' durch Scherflusstexturierung von PPv-Kristallen. LLSVP (Large Low Shear Velocity Provinces, 'TUZO' und 'JASON') sind thermochemische Anomalien.",
        difficulty = 5,
        funFact = null
    ),

    // Question 32 – Post-Perovskite Phase
    Question(
        categoryId = 2,
        questionText = "Das Clausius-Clapeyron-Gefälle des Perowskit-zu-Post-Perowskit-Übergangs ist ungewöhnlich steil und positiv. Welche geodynamische Konsequenz ergibt sich daraus für die Temperaturstruktur an der CMB und für subduzierende Platten?",
        answerA = "Das positive Clapeyron-Gefälle bedeutet, dass PPv in heißen Upwelling-Regionen stabil ist und kalte Downwelling-Regionen PPv destabilisieren – das Gegenteil des 660-km-Übergangs",
        answerB = "Das sehr steile positive Clapeyron-Gefälle (dP/dT ≈ +10 MPa/K) bedeutet: PPv ist bei gleicher Tiefe in kalten Regionen (Subduktionszonen) eher stabil als in heißen Regionen. 'Double crossing': An einem heißen Plume kann die Temperatur so hoch sein, dass Pv→PPv→zurück zu Pv zwei Mal gequert wird, was zwei seismische Diskontinuitäten erzeugt ('double-crossing' beobachtet in einigen CMB-Regionen)",
        answerC = "Das Clapeyron-Gefälle des PPv-Übergangs ist negativ (analog zum 660-km-Übergang); dies hemmt Konvektion im unteren Mantel",
        answerD = "Der PPv-Übergang hat kein messbares Clapeyron-Gefälle; der Phasenübergang ist rein kompositionell und druckabhängig, aber temperaturunabhängig",
        correctAnswer = 1, // B
        explanation = "PPv Clausius-Clapeyron: dP/dT ≈ +9–13 MPa/K (extrem steil, positiv), gemessen durch Oganov, Ono et al. und Tateno et al. Geodynamik: Pv→PPv bei 125±5 GPa / 2500±500 K. In kalten subduzierenden Platten (T < 2500 K am CMB) tritt PPv-Transition bei etwas geringerer Tiefe auf als im heißen Hintergrundmantel. 'Double-crossing'-Phänomen (Hernlund, Thomas, Tackley 2005): Wenn thermische Grenzschicht über der CMB eine Temperaturinversion zeigt, kann ein Temperaturprofil den Übergang zweimal schneiden → zwei Diskontinuitäten im S-Wellen-Profil, tatsächlich in einigen Regionen beobachtet.",
        difficulty = 5,
        funFact = "Die Entdeckung der Post-Perowskit-Phase (2004) wurde als die bedeutendste Entdeckung in der Mineralphysik seit 20 Jahren bezeichnet – sie löste das lange unverstandene Phänomen der D''-Seismologie auf einen Schlag."
    ),

    // Question 33 – Inner Core Anisotropy
    Question(
        categoryId = 2,
        questionText = "Der innere Erdkern zeigt ausgeprägte seismische Anisotropie: P-Wellen reisen entlang der Erdrotationsachse (~3–4%) schneller als in der Äquatorebene. Welche Mineralogien und Texturen erklären diese Anisotropie?",
        answerA = "Innere-Kern-Anisotropie entsteht durch Ausrichtung flüssigen Eisens; da der innere Kern teilweise flüssig ist, richten sich Eis-Cluster entlang der Magnetfeldlinien aus",
        answerB = "ε-Eisen (hexagonal dichteste Packung, hcp) ist die stabile Hochdruckphase des inneren Kerns bei ~330 GPa / ~5500 K. Seismische Anisotropie entsteht durch texturierte Ausrichtung hcp-Eisen-Kristalle mit c-Achse parallel zur Rotationsachse, vermutlich durch Magneto-hydraulisches Ausrichten beim Erstarren oder durch Plastizitätsdeformation durch Maxwell-Spannungen der Erde",
        answerC = "Der innere Kern besteht aus bcc-Eisen (kubisch raumzentriert), das intrinsisch isotrop ist; Anisotropie entsteht durch orientierte Einschlüsse von Nickel-Legierungen",
        answerD = "Innere-Kern-Anisotropie ist ein Artefakt des PKIKP-Wellenpfades; tatsächlich ist der innere Kern vollständig isotrop und die scheinbare Anisotropie entsteht durch Mantel-Heterogenitäten",
        correctAnswer = 1, // B
        explanation = "Bei innere-Kern-Bedingungen (~330 GPa, ~5500 K) ist hcp-ε-Eisen die thermodynamisch stabile Phase (nach Belonoshko, Vocadlo et al. – obwohl bcc-δ-Eisen bei sehr hoher T auch diskutiert wird). hcp-Eisen ist seismisch stark anisotrop: c/a-Achsenverhältnis ≠ ideal → unterschiedliche elastische Moduln entlang c-Achse vs. Basalebene. Texturierungsmechanismen: (1) Magneto-hydraulisches Wachstum (Karato 1993); (2) Viskoplastische Deformation durch thermischer Konvektion; (3) Anisotropes Erstarren. Die Anisotropie zeigt hemispärische Asymmetrie (östliche vs. westliche Hemisphäre), was auf innere Kern-Super-Rotation oder -Konvektion hindeutet.",
        difficulty = 5,
        funFact = null
    ),

    // Question 34 – Core-Mantle Boundary Topography
    Question(
        categoryId = 2,
        questionText = "Die Kern-Mantel-Grenze (CMB) weist topographische Variationen von mehreren Kilometern auf. Welche geophysikalischen Beobachtungsmethoden und geodynamischen Prozesse sind für CMB-Topographie relevant?",
        answerA = "CMB-Topographie wird ausschließlich durch Erdrotationsmessungen bestimmt; seismische Methoden haben keine ausreichende Auflösung",
        answerB = "CMB-Topographie wird durch: (1) PcP/ScS-Reflexions-Reisezeiten (seismische Tomographie); (2) CMB-sensitive Normalmoden (freie Schwingungen der Erde, toroidal/sphäroidal); (3) Satellitengeodäsie (indirekt über Geoid) kartiert. Geodynamisch: Mantelkonvektions-Hochdruckregionen drücken CMB nach unten; LLSVP-Regionen (heiß, niedrige Viskosität) können CMB nach oben wölben. Amplitude: ±5 km",
        answerC = "CMB-Topographie beträgt nur wenige Zentimeter und ist seismisch nicht detektierbar; die scheinbaren Variationen sind Mantel-Heterogenitäten",
        answerD = "Die CMB ist perfekt flach aufgrund der hydrostatischen Gleichgewichtsbedingung; Topographie entsteht nur an der Oberfläche und der 660-km-Diskontinuität",
        correctAnswer = 1, // B
        explanation = "Seismische Methoden: (1) PcP-Reflexionen (P-Welle auf CMB reflektiert) – lokale Tiefenvariationen. (2) Sp/Ps-Konversionen an CMB. (3) Normalmoden (0S_l) sind sensitiv für großräumige CMB-Topographie und CMB-Impedanzkontrast. CMB-Topographie aus Tomographie (Dziewoński, Forte, Moucha et al.): Korrelation mit LLSVP-Grenzen (Africa, Pacific LLSVP: +5 km CMB-Erhöhung). Geodynamische Kopplung: Manteldynamik, elektromagnetische Drehmomente und CMB-Topographie koppeln Mantel- und Kernrotation. CMB-Topographie beeinflusst Geomagneto-Konvektion und damit Paläo-Magnetfeld-Muster.",
        difficulty = 5,
        funFact = "Die zwei 'Superplumes' (LLSVP, Large Low-Shear-Velocity Provinces) unter Afrika und dem Pazifik sind so groß wie Kontinente und erstreckend sich vom CMB fast bis zum 660-km-Übergang – sie könnten thermochemische 'Friedhöfe' antiker subduzierter Ozeankruste sein."
    ),

    // Question 35 – Geoid Anomalies
    Question(
        categoryId = 2,
        questionText = "Das Geoid ist die Äquipotenzialfläche des Erdgravitationsfeldes auf Meereshöhe. Warum erzeugen heiße Mantelplumes trotz geringerer Dichte positive Geoidanomalien (Geoid-Erhöhungen)?",
        answerA = "Heiße Mantelplumes erzeugen immer negative Geoidanomalien (Geoid-Tiefs), weil geringere Dichte zu geringerer Gravitationsanziehung führt",
        answerB = "Positive Geoidanomalien über heißen Plumes entstehen durch dynamische Topographie: Der aufsteigende Plume wölbt die Erdoberfläche und die CMB nach oben; die Gravitationsanomalie der nach oben gewölbten Oberfläche (positiv) überwiegt den Massendefizit-Effekt des warmen, leichten Plume-Materials. Das Verhältnis Geoid/Topographie (Admittanz) ist diagnostisch für Mantelviskositätsstruktur",
        answerC = "Geoidanomalien über Mantelplumes entstehen ausschließlich durch Krustendicken-Variationen (Isostasie); der Mantel trägt nicht zur langwelligen Geoidstruktur bei",
        answerD = "Das Geoid ist vollständig durch die Erdrotation bestimmt und zeigt keine Korrelation mit Mantelstruktur; Abweichungen vom Rotationsellipsoid sind statistisches Rauschen",
        correctAnswer = 1, // B
        explanation = "Geoid-Beiträge haben drei Komponenten: (1) Direkte Massenanomalien (Dichte×Volumen); (2) Dynamische Topographie der Oberfläche; (3) Dynamische CMB-Topographie. Für Mantelkonvektion: Humberto Richards & Hager (1984) zeigten, dass dynamische Topographie die Geoidanomalie dominiert. Geoid über heißem Plume: Oberflächen-Uplift → positives Geoid (Geoid hebt durch erhöhte Massenoberfläche); Massendefizit des heißen Plumes → negatives Geoid. Bei Mantelviskositätsprofil mit viskosem unteren Mantel überwiegt Topographie-Term → positive Geoidanomalie. Geoid/Topographie-Admittanz: Z = ΔN/Δh ≈ 6 mGal/km für kompensierte Ozeankruste.",
        difficulty = 5,
        funFact = null
    ),

    // ── BLOCK 8: Advanced Immunogenomics ─────────────────────────────────────

    // Question 36 – V(D)J Recombination
    Question(
        categoryId = 2,
        questionText = "V(D)J-Rekombination erzeugt die enorme Diversität von Antikörpern und T-Zell-Rezeptoren. Welche molekularen Mechanismen tragen zur kombinatorischen und junktionalen Diversität bei, und warum ist RAG1/RAG2 als 'domestiziertes Transposon' beschreibbar?",
        answerA = "V(D)J-Rekombination ist rein transkriptionell und erfordert keine DNA-Doppelstrangbrüche; RAG-Proteine wirken als RNA-Editasen",
        answerB = "Kombinatorische Diversität: Auswahl aus V, D, J-Gensegmenten (H-Kette: ~40 V × 25 D × 6 J = ~6000 Kombinationen). Junktionale Diversität: RAG1/2 führt Doppelstrangbrüche an RSS-Sequenzen ein; P-Nukleoticde (Palindrom-Fill-in), N-Nukleotide (TdT-vermittelte zufällige Addition) und Exonuklease-Resektion an Verbindungsstellen erzeugen >10¹² theoretisch mögliche CDR3-Sequenzen. RAG1/2 Mechanismus (Schneiden und Ligation) ist verwandt mit Transposons der DD(E)-Transposase-Superfamilie – tatsächlich kann RAG in vitro Transpositions-Reaktionen katalysieren",
        answerC = "V(D)J-Rekombination erzeugt maximal 500 verschiedene Antikörper; höhere Diversität entsteht erst durch somatische Hypermutation und Klassenwechsel nach Antigenkontakt",
        answerD = "RAG1/RAG2 ist ein spezifisches Vertebraten-Enzym ohne evolutionäre Verwandtschaft zu Transposons; V(D)J-Rekombination ist mechanistisch vollständig verschieden von prokaryotischer Transposition",
        correctAnswer = 1, // B
        explanation = "RAG1-Kerndomäne enthält das DDE-Motiv (Asp, Asp, Glu) charakteristisch für Typ-II-Transposase/Retroviral-Integrase-Superfamilie (Kapitonov & Jurka 2005). RAG1/2 katalysiert: (1) Nickung am 5'-Ende der RSS-Heptamer-Sequenz; (2) Transesterifikation erzeugt Haarnadelstruktur + Signal-Ende; (3) NHEJ (Ku70/80, DNA-PKcs, Artemis-Nuclease) öffnet Haarnadelstruktur → P-Nukleotide; (4) TdT addiert N-Nukleotide zufällig. Junktionale Diversität ~10¹⁰-10¹² (theoretisch). Evolutionäre Domestizierung: RAG-Vorläufer-Transposon (RAG1/2-Transposon 'Transib') wurde ~500 Mya ins Vertebraten-Genom integriert und adaptiert.",
        difficulty = 5,
        funFact = "Das menschliche Immunsystem kann über 10¹⁸ verschiedene Antikörper-Sequenzen erzeugen – damit gibt es theoretisch mehr verschiedene Antikörper als Atome in der Milchstraße (etwa 10⁶⁷), weit mehr als für jeden möglichen Erreger nötig wäre."
    ),

    // Question 37 – HLA Diversity
    Question(
        categoryId = 2,
        questionText = "Das Humane Leukozyten-Antigen (HLA)-System ist das polym­orphste Gensystem des menschlichen Genoms. Was ist der evolutionäre Selektionsdruck hinter HLA-Polymorphismus, und welche populationsgenetische Signatur deutet auf Balancing-Selektion hin?",
        answerA = "HLA-Polymorphismus entsteht durch zufälligen Drift; alle HLA-Allele sind selektiv neutral und ihre Häufigkeiten folgen Hardy-Weinberg-Gleichgewicht ohne Selektion",
        answerB = "Negative frequenzabhängige Selektion und Heterozygoten-Vorteil (Overdominance) begünstigen seltene HLA-Allele, da Pathogene sich an häufige MHC-Allele adaptieren ('Red Queen'-Hypothese). Populationsgenetische Signaturen: (1) Antiker Allel-Polymorphismus (trans-species polymorphism: HLA-Allele teilen sich mit Schimpansen gemeinsame Vorfahren); (2) Extended Haplotype Homozygosity (EHH) – Verringerung von LD um HLA-Gene; (3) dN/dS > 1 (positive Selektion) an Peptid-Bindungsreste des MHC",
        answerC = "HLA-Diversität entsteht durch erhöhte Mutationsrate (hot spots); Balancing-Selektion spielt keine nachweisbare Rolle",
        answerD = "HLA-Polymorphismus ist vollständig durch Migration und Admixture erklärbar; jede Weltbevölkerung hat genau 2 HLA-Allele pro Genlocus",
        correctAnswer = 1, // B
        explanation = "HLA Klasse I (A, B, C) und II (DR, DQ, DP) sind die polymorphsten humanen Gene: >30,000 bekannte HLA-Allele (IMGT/HLA-Datenbank). Balancing-Selektion-Evidenzen: (1) Trans-species polymorphism (TSP): HLA-DRB1-Allel-Cluster teilen Vorfahren mit Pan troglodytes (>5 Mya alt); (2) dN/dS >> 1 an Antigen-Bindungsresidue (ABS) im α1/α2-Domänen; (3) Negative Linkage Disequilibrium zwischen nahen HLA-Loci; (4) Erhöhte Heterozygotie in zentralen MHC-Regionen. HIV-Selektion auf HLA-B57 zeigt akute pathogen-getriebene Selektion in Echtzeit.",
        difficulty = 5,
        funFact = null
    ),

    // Question 38 – Immunoglobulin Class Switching
    Question(
        categoryId = 2,
        questionText = "Klassenwechsel-Rekombination (Class Switch Recombination, CSR) wechselt die Antikörper-Effektorfunktion ohne Veränderung der Antigenspezifität. Welche DNA-Desaminase katalysiert CSR, und welcher DNA-Reparaturweg wird genutzt?",
        answerA = "CSR wird durch V(D)J-Rekombinase (RAG1/2) katalysiert; die Reaktion ist identisch zur primären V(D)J-Rekombination und verwendet NHEJ",
        answerB = "Activation-Induced Cytidine Deaminase (AID, AICDA) deaminiert Cytosin zu Uracil in einzelsträngiger DNA der Switch-Regionen (S-Regionen), die durch RNA-Pol-II-Transkription exponiert werden. Uracil-Processing durch UNG (Uracil-N-Glycosylase) und APE1 erzeugt Einzelstrangbrüche; wenn auf gegenüberliegenden DNA-Strängen konvergent → Doppelstrangbrüche. End-Joining (klassisches NHEJ oder alternatives NHEJ/MMEJ) ligiert upstream-S mit downstream-S-Region → deletive CSR",
        answerC = "CSR erfolgt durch Homologe Rekombination zwischen S-Regionen; AID ist an somatischer Hypermutation, aber nicht an CSR beteiligt",
        answerD = "CSR wird durch RNA-Editierung (ADAR-Enzyme) katalysiert; IgM→IgG-Wechsel erfordert ausschließlich alternative RNA-Spleißvarianten ohne DNA-Änderungen",
        correctAnswer = 1, // B
        explanation = "AID (Muramatsu et al. 2000) ist eine Deaminase der APOBEC-Superfamilie. CSR-Mechanismus: (1) Zytokine (IL-4, IFN-γ, TGF-β) induzieren spezifische Igh-Germ-Line-Transkription → Exposition von ssDNA in S-Regionen (Switch-Sequenzen, 1–10 kb, tandem-repetitiv); (2) AID deaminiert C→U in ssDNA der Transkriptions-Bubbles; (3) UNG entfernt U → abasische Stelle + APE1-Nick → SSB; (4) Zwei konvergente SSBs auf Gegensträngen = DSB; (5) DSB-Repair durch NHEJ (Ku70/80, DNA-PKcs, Artemis) oder A-NHEJ (PARP1, CtIP); (6) Deletion zwischen S_μ und S_γ/α/ε → IgG/IgA/IgE. AID-Fehlfunktion verursacht Hyper-IgM-Syndrom und B-Zell-Lymphome.",
        difficulty = 5,
        funFact = "AID (Activation-Induced Cytidine Deaminase) ist so eine 'gefährliche' Enzymaktivität, dass sie bei Fehlfunktion zahlreiche Krebsarten verursacht – gleichzeitig ist sie aber für eine funktionierende Immunantwort absolut unentbehrlich: ein perfektes Beispiel für das immunologische Paradoxon zwischen Schutz und Selbstschädigung."
    ),

    // Question 39 – T-Cell Receptor Repertoire
    Question(
        categoryId = 2,
        questionText = "Der T-Zell-Rezeptor (TCR)-Repertoire umfasst beim Menschen schätzungsweise 10⁷–10⁸ verschiedene Klone. Welche Hochdurchsatz-Sequenzierungsansätze ermöglichen Repertoire-Analyse, und welches bioinformatische Maß beschreibt die Repertoire-Diversität?",
        answerA = "TCR-Repertoireanalyse ist unmöglich, da TCR-Gene nicht im Referenzgenom enthalten sind; alle TCR-Sequenzen müssen von Fall zu Fall neu assembliert werden ohne standardisierte Methoden",
        answerB = "High-throughput sequencing (HTS) der CDR3β/CDR3α-Regionen nach reverser Transkription (bulk TCR-seq: Adaptive Biotechnologies immunoSEQ; Einzelzell: 10x Genomics Chromium mit TCR-Anreicherung). Bioinformatische Tools: IMGT/GENE-DB für V/J-Zuweisung, MiXCR, TRUST4 für Assemblierung. Diversitätsmaße: Shannon-Entropie H = -Σ p_i·log(p_i), Clonality = 1 - (H/log N), D50-Index (minimale Klonanzahl für 50% der Sequenzen), Gini-Koeffizient für Klonexpansion",
        answerC = "TCR-Repertoire-Sequenzierung misst ausschließlich Vβ-Familien-Nutzung durch Flow-Zytometrie mit Antikörpern; CDR3-Sequenz-Information ist nicht zugänglich",
        answerD = "Repertoire-Diversität wird ausschließlich durch die Anzahl detektierter V-Gene beschrieben; CDR3-Längenvariation und N-Nukleotid-Diversität werden in Standard-Analysen ignoriert",
        correctAnswer = 1, // B
        explanation = "Moderne TCR-Repertoire-Analyse: Bulk-Methoden (Adaptive Biotechnologies, Illumina): Milliarden Reads pro Probe, CDR3β-Amplifikation aus genomischer DNA oder cDNA; Einzelzell: 10x VDJ-Enrichment ko-registriert TCR mit Transkriptom (scTCR-seq). Assemblierung-Herausforderungen: V(D)J-Spleiß-Übergänge, Somatic Hypermutation in Memory-Zellen (minimal bei TCR im Gegensatz zu B-Zellen). Diversitätsmaße: True Diversity D^q = (Σ p_i^q)^{1/(1-q)} (Hill-Zahl-Rahmen); bei q=0: Artenreichtum; q=1: exp(Shannon-H); q=2: Simpson-Inverse. Klinisch: T-Zell-Klonexpansion korreliert mit Immunantwort auf Tumorantigene (Neoepitope), CAR-T-Zell-Persistenz, und Autoimmun-Krankheits-Rückfall.",
        difficulty = 5,
        funFact = null
    ),

    // Question 40 – Immune Checkpoint Molecular Mechanisms
    Question(
        categoryId = 2,
        questionText = "Anti-PD-1/PD-L1-Checkpoint-Inhibitoren revolutionieren die Krebsimmuntherapie. Welcher intrazelluläre Signalweg wird durch PD-1/PD-L1-Interaktion gehemmt, und warum führt dies zu T-Zell-Erschöpfung (Exhaustion)?",
        answerA = "PD-1 aktiviert direkt die PI3K-Kaskade und führt über mTOR-Aktivierung zu übermäßiger T-Zell-Proliferation, die durch negativem Feedback erschöpft wird",
        answerB = "PD-1-Ligation rekrutiert SHP-2-Phosphatase (und SHP-1) über ITIMs/ITSMs im PD-1-zytoplasmatischen Schwanz. SHP-2 dephosphoryliert CD28 (T-Zell-Kostimulationsrezeptor), ZAP-70 und Lck, was PI3K-Rekrutierung blockiert (Akt/mTOR-Hemmung, verminderter Zucker/Fettstoffwechsel) und ERK-Signaling reduziert. Chronische PD-1-Signalisierung induziert Erschöpfungs-Transkriptionsfaktoren TOX und TOX2, die epigenetisch stabile Erschöpfungs-Chromatinzustände (z.B. veränderte ATAC-Peaks in TCF7, TBX21-Loci) etablieren",
        answerC = "PD-1/PD-L1-Interaktion wirkt ausschließlich durch Blockierung des TCR-MHC-Kontakts (sterische Hemmung); keine intrazellulären Signalwege sind beteiligt",
        answerD = "PD-1-Signalisierung aktiviert CTLA-4, das dann B7-Liganden von APCs bindet; die T-Zell-Erschöpfung ist vollständig durch CTLA-4-abhängige CD28-Blockade erklärbar",
        correctAnswer = 1, // B
        explanation = "PD-1 (CD279) trägt im zytoplasmatischen Schwanz ITSM (Immunoreceptor Tyrosine-based Switch Motif) und ITIM. Nach PD-L1-Bindung: Lck phosphoryliert ITSM/ITIM → Rekrutierung von SHP-2 (primär) und SHP-1. SHP-2 dephosphoryliert: pY319-ZAP-70, pY315/320-CD28 (blockiert PI3K-Bindung), pY394-Lck. Downstream-Effekte: verminderte Akt-Phosphorylierung → reduzierter Glykolyse-Flux (Warburg-Effekt gehemmt) → metabolische Einschränkung. TOX (T-cell exhaustion-specific transcription factor, Scott et al. 2019, Khan et al. 2019): IRF4, NFAT-abhängige TOX-Induktion bei chronischer Antigen-Stimulation → TOX öffnet Erschöpfungs-spezifisches Chromatinprogramm (ATAC-seq Peaks in Lag3, Tim3, CD39-Loci).",
        difficulty = 5,
        funFact = "James Allison (für CTLA-4) und Tasuku Honjo (für PD-1) erhielten 2018 den Nobelpreis für Medizin – die Checkpoint-Inhibitor-Therapie hatte zu diesem Zeitpunkt bereits Tausende von Krebspatienten mit vorher als unheilbar geltenden Erkrankungen geheilt."
    ),

    // ── BLOCK 9: Exotic States of Matter ─────────────────────────────────────

    // Question 41 – Time Crystals
    Question(
        categoryId = 2,
        questionText = "Zeitkristalle (Time Crystals) wurden 2021 experimentell in einem Quantenprozessor (Google Sycamore) realisiert. Was ist das definierendes Merkmal eines diskreten Zeitkristalls (DTC), und warum verletzt er nicht den zweiten Hauptsatz der Thermodynamik?",
        answerA = "DTC-Systeme zeigen spontane Symmetriebrechung der kontinuierlichen Zeittranslationssymmetrie im Gleichgewicht; dies verletzt den zweiten Hauptsatz, weshalb DTCs experimentell nicht stabil sind",
        answerB = "Ein DTC bricht diskrete Zeittranslationssymmetrie (DTTS) in einem periodisch getriebenen (Floquet) System: das System antwortet mit Periode 2T auf ein Treiben der Periode T (Subharmonic Response). Dies ist möglich ohne den zweiten Hauptsatz zu verletzen, da DTCs Nicht-Gleichgewichts-Systeme sind (Many-Body-Lokalisierung, MBL verhindert Thermalisierung); die Entropie kann im vollen System mit Bath steigen, während die sub-harmonische Ordnung durch Lokalisierung erhalten bleibt",
        answerC = "Zeitkristalle sind reine theoretische Konstrukte; das Google-Experiment zeigte einen klassischen Oszillator, keinen Quantenzeitkristall",
        answerD = "DTCs beanspruchen Energie aus dem Quantenvakuum und verletzen tatsächlich lokale Energieerhaltung; sie können deshalb als Perpetuum-Mobile-artige Energiequellen genutzt werden",
        correctAnswer = 1, // B
        explanation = "Frank Wilczek postulierte 2012 Zeitkristalle im Gleichgewicht – Watanabe & Oshikawa 2015 zeigten, dass echte Gleichgewichts-Zeitkristalle unmöglich sind. DTCs (Else, Bauer, Nayak 2016; Khemani et al. 2016): Floquet-Hamiltonian H(t) = H(t+T); DTC-Phase: Ordnungsparameter ⟨σ^z(t)⟩ hat Periode 2T trotz Treiben mit T. Stabilisierung durch MBL (Many-Body-Lokalisierung, Anderson-Lokalisierung mit Wechselwirkungen): Thermalisierung unterbunden durch ungeordnete Wechselwirkungen. Google-Experiment (Mi et al. Nature 2022): 20-Qubit-Kette, Floquet-Dynamik, subharmonische Antwort >100 Zyklen stabil – erster robuster DTC-Nachweis in offenen Systemen mit 'error-mitigated' Tomographie.",
        difficulty = 5,
        funFact = null
    ),

    // Question 42 – Supersolids
    Question(
        categoryId = 2,
        questionText = "Ein Suprafestkörper (Supersolid) zeigt gleichzeitig langreichweitige kristalline Ordnung und Suprafluidität. In welchem experimentellen System wurde dieser Zustand 2019 eindeutig nachgewiesen, und welcher Mechanismus ermöglicht die Koexistenz beider Ordnungsphänomene?",
        answerA = "Supersolide wurden zuerst in festem Helium-4 (Kim-Chan-Experiment 2004) eindeutig nachgewiesen; der Mechanismus ist quantenmechanische Delokalisierung von ⁴He-Atomen entlang Versetzungslinien",
        answerB = "2019 zeigten drei Gruppen (ETH Zürich, Stuttgart, CNR Pisa) Supersolide in ultrakalten Dipol-BEC-Systemen (Erbium, Dysprosium und ¹⁶⁴Dy-BEC): Rotoninstabilität durch starke anisotrope Dipol-Dipol-Wechselwirkungen führt zu dichtemodulierter Kristallstruktur bei gleichzeitig erhaltener globaler BEC-Kohärenz; Phasenkohärenz zwischen Tröpfchen-Array belegt Suprafluidität",
        answerC = "Supersolide wurden nur theoretisch in Helium-4 vorhergesagt; alle experimentellen Supersolid-Beobachtungen von 2004 wurden als Artefakte widerlegt und das Phänomen gilt als experimentell nicht zugänglich",
        answerD = "Supersolide entstehen in Rydberg-Atome-Gittern bei T > 1 K und zeigen klassische Kristallordnung mit bosonischer Supraleitung (Josephson-Tunneln) entlang von Kristallachsen",
        correctAnswer = 1, // B
        explanation = "Kim-Chan (2004) beobachteten scheinbare Supersolidität in ⁴He, aber Balibar und andere zeigten 2012: Kim-Chan-Signal ist elastisch (viskoelastisches Artefakt, kein echter Supersolid). Echter Supersolid-Durchbruch 2019: Tanzi et al. (Pisa), Böttcher et al. (Stuttgart), Chomaz et al. (Innsbruck) in ⁸⁷Rb-⁴⁰K- oder ¹⁶⁴Dy-BEC mit starken magnetischen Dipolmomenten. Mechanismus: Dipol-Dipol-Wechselwirkung (anisotrop, langreichweitig) induziert Roton-Minimum im Phonon-Spektrum; bei ausreichender Stärke → Rotoninstabilität → spontane Dichtemodulation. Gleichzeitige BEC-Phasenkohärenz (Josephson-Strom zwischen Dichte-Tröpfchen) = Superfluidität. Gemessen durch Interferenz-Freiheit und superfluidem Trägheitsmoment.",
        difficulty = 5,
        funFact = "Die Suche nach dem Supersolid begann 1969 mit Andreev und Lifshitz' Theorie von Quantenvakanzwellen in Festkörpern – 50 Jahre vergingen, bis ein echter Supersolid in einem vollständig anderen System (ultrakalten Dipolgasen) realisiert wurde."
    ),

    // Question 43 – Strange Metals / Non-Fermi Liquids
    Question(
        categoryId = 2,
        questionText = "Seltsame Metalle (Strange Metals) in Hochtemperatur-Supraleitern zeigen T-linearen elektrischen Widerstand ρ ~ T in einem breiten Temperaturbereich, was durch die Fermi-Flüssigkeitstheorie nicht erklärbar ist (ρ_FL ~ T²). Welches theoretische Konzept erklärt diese Abweichung?",
        answerA = "T-linearer Widerstand entsteht durch klassischen Phonon-Streuung (Bloch-T⁵ zu Bloch-T bei hohen T); Fermi-Flüssigkeit ist überall gültig",
        answerB = "Seltsame Metalle sind Nicht-Fermi-Flüssigkeiten nahe einem quantenkritischen Punkt (QCP): Quantenfluktuationen eines Ordnungsparameters (z.B. antiferromagnetische Ordnung, Mott-Übergang) divergieren im Planckian-Dissipations-Regime. Streuzeit τ ~ ℏ/(k_BT) (Planck-Einheit), minimal nach Zaanen: ρ ~ ℏ/(k_BT·e²/h)·(Anzahl Ladungsträger)⁻¹ erklärt T-Linearität. AdS/CFT-Dualität (holographische Metalle) liefert einen theoretischen Rahmen für Nicht-Fermi-Flüssigkeiten ohne quasiteilchen-basierte Beschreibung",
        answerC = "T-linearer Widerstand entsteht durch Anderson-Lokalisierung bei tiefen Temperaturen; der Effekt hat keinen Zusammenhang mit Hochtemperatur-Supraleitung oder Quantenkritikalität",
        answerD = "Seltsame Metalle sind vollständig durch anisotrope Elektron-Phonon-Wechselwirkung in Perowskit-Strukturen erklärbar; Quantenkritikalität ist eine Spekulation ohne experimentelle Evidenz",
        correctAnswer = 1, // B
        explanation = "Fermi-Flüssigkeit: ρ ~ T² (Quasipartikel-Lebensdauer τ ~ T⁻² durch Quasipartikel-Quasipartikel-Streuung). Strange-Metal-Phase: ρ = A + B·T (linear über weite T-Bereiche, einschließlich T << Debye-Temperatur). Planckian-Dissipation-Hypothese (Zaanen 2004): τ = ℏ/(k_BT) ist die schnellstmögliche Quasiteilchen-Relaxationsrate (kommt an quantenmechanische 'Zeitenergieunschärfe-Grenze'). QCP-Theorien: Hertz-Millis-Theorien für SDW-QCP, non-Gaussian-Korrekturen geben NFL-Behavior. AdS/CFT: (2+1)-dimensionales strange metal korrespondiert zu (3+1)-D AdS-Schwarzes-Loch; T-linearer Widerstand emergiert aus Hawking-Temperatur-Dynamik. Sachdev-Ye-Kitaev (SYK)-Modell: exactly lösbar, maximales Chaos (Lyapunov-Exponent = 2πk_BT/ℏ), strange metal Grenzfall.",
        difficulty = 5,
        funFact = null
    ),

    // Question 44 – Non-Fermi Liquids / Luttinger Liquids
    Question(
        categoryId = 2,
        questionText = "Luttinger-Flüssigkeiten (TLL, Tomonaga-Luttinger Liquid) sind der universelle Nicht-Fermi-Flüssigkeits-Fixpunkt in einer Raumdimension (1D). Welche charakteristischen experimentellen Observablen zeigen Abweichungen vom Fermi-Flüssigkeits-Verhalten, und wie erscheint die Spin-Ladungs-Trennung?",
        answerA = "1D-Systeme zeigen immer Fermi-Flüssigkeit; Luttinger-Flüssigkeit ist ein theoretisches Konzept das experimentell nicht realisierbar ist, da echte 1D-Leiter nicht existieren",
        answerB = "TLL-Observablen: (1) Photoemissions-Spektrum (PES): kein scharfer Quasipartikel-Peak an k_F sondern Potenzgesetz-Unterdrückung ρ(ω) ~ |ω-μ|^α mit TLL-Exponent α = (g+g⁻¹-2)/4; (2) Spin-Ladungs-Trennung: Injektion eines Elektrons in 1D-Leiter zerfällt in Spinon (Spin ½, keine Ladung) und Holon (Ladung e, kein Spin) mit unterschiedlichen Geschwindigkeiten v_s ≠ v_c; (3) Tunnelleitfähigkeit G ~ T^{2α} (Power-Law-Suppression bei tiefen T)",
        answerC = "Luttinger-Flüssigkeiten zeigen ausschließlich Supraleitung als charakteristisches Observable; Spin-Ladungs-Trennung ist in 1D nicht möglich da Spin und Ladung immer am selben Ort lokalisiert sind",
        answerD = "TLL-Verhalten entsteht nur bei T > 10 K; bei tiefen Temperaturen gehen alle 1D-Leiter in die Fermi-Flüssigkeitsphase über",
        correctAnswer = 1, // B
        explanation = "1D-Systeme: Fermi-Oberfläche reduziert auf zwei Punkte ±k_F; beliebig kleine Wechselwirkungen erzeugen Instabilitäten und Bosonisierung. Bosonisierung (Mattis-Lieb, Haldane 1981): Elektronen-Operatoren durch Bose-Felder ausgedrückt → exakt lösbar. Spin-Ladungs-Trennung: In freiem Elektronensystem v_s = v_c = v_F; TLL: getrennte Phonon-artige Moden mit v_s (Spinon-Geschwindigkeit durch J-Austausch bestimmt) ≠ v_c (Holon-Geschwindigkeit durch t-Hüpfen bestimmt). Experimentell: SrCuO₂ -Ketten (Kim et al. 2006, PRL): ARPES zeigt zwei separate Moden. Carbon-Nanotubes (Bockrath et al. 1999): G ~ T^{0.3} Power-Law. Quantum-Wire-Phasensystem: Ausagaard et al. in GaAs-Quantendrähten.",
        difficulty = 5,
        funFact = "Die theoretische Beschreibung von Luttinger-Flüssigkeiten erfordert Bosonisierung – eine elegante Methode, bei der Fermi-Felder exakt durch Bose-Felder ausgedrückt werden, was in einer Dimension (aber nur dort) möglich ist."
    ),

    // Question 45 – Bose Glass
    Question(
        categoryId = 2,
        questionText = "Der Bose-Glas ist ein inkompressibler, aber nicht suprafluider Zustand schwach wechselwirkender Bosonen in einem ungeordneten Potential. Wie unterscheidet er sich vom Mott-Isolator, und welches experimentelle System realisiert ihn?",
        answerA = "Bose-Glas und Mott-Isolator sind identisch; beide entstehen durch Wechselwirkung und beide zeigen identische Kompressibilität κ = ∂n/∂μ = 0",
        answerB = "Mott-Isolator: inkompressibel (κ = 0) mit Energielücke, entsteht durch Wechselwirkung (Mott-Hubbard-Physik) bei ganzzahliger Füllung. Bose-Glas: kompressibel (κ ≠ 0, keine Lücke) aber nicht suprafluid (ρ_s = 0); entsteht durch Unordnung in bosonischen Systemen (Fischer et al. 1989). Experimentell: ultrakalte Atome in optischen Gittern mit superimposiertem ungeordnetem Potenzial (bichromatic optical lattice, Damski et al., Fallani et al. 2007 in Florenz: ⁸⁷Rb im 1D-Gitter mit Unordnung)",
        answerC = "Bose-Glas ist suprafluid mit unendlicher Kohärenzlänge; Unordnung verstärkt die Suprafluidität durch Anderson-Bosonisierung",
        answerD = "Bose-Glas entsteht ausschließlich in 2D-Systemen; in 1D und 3D geht das System bei Unordnung direkt in die Fermi-Glas-Phase über",
        correctAnswer = 1, // B
        explanation = "Bosonen in Unordnung (Fisher, Weichman, Grinstein, Fisher 1989, Phys. Rev. B): Suprafluid (SF) – Mott-Isolator (MI) – Bose-Glas (BG)-Phasendiagramm. SF: ρ_s > 0, κ > 0. MI: ρ_s = 0, κ = 0 (Energielücke, kommensurable Füllung). BG: ρ_s = 0, κ > 0 (keine Lücke, inkompressibler Dreck nur in Unordnungs-induzierten Potenzial-Minima-Inseln). Kritischer Exponenten der BG-SF-Transition: ν z ≥ d+2 (Harris-Kriterium generalisierto). Fallani et al. (2007): Bichromatic quasipériodic potential (λ₁/λ₂ = irrational ratio) in 1D BEC → Beobachtung von SF→BG-Übergang durch kohärente Bragg-Spektroskopie und Expansions-Interferenz.",
        difficulty = 5,
        funFact = null
    ),

    // ── BLOCK 10: Advanced Astrochemistry ────────────────────────────────────

    // Question 46 – Interstellar Medium Chemistry
    Question(
        categoryId = 2,
        questionText = "In dichten Molekülwolken (n_H > 10⁴ cm⁻³) werden komplexe organische Moleküle (COMs) beobachtet. Durch welchen Mechanismus entstehen COMs wie Methanol (CH₃OH) und Dimethylether ((CH₃)₂O) unter ISM-Bedingungen (T ≈ 10 K)?",
        answerA = "COMs entstehen ausschließlich durch Gasphasenionenchemie; Reaktionen auf Eismanteln spielen keine Rolle bei Temperaturen unter 50 K",
        answerB = "COMs entstehen hauptsächlich durch radikalische Oberflächenchemie auf Eiskörnern: (1) Hydrogenierung von CO auf H₂O-Eisoberflächen → HCO → H₂CO → CH₃O/CH₂OH → CH₃OH (sequentielle H-Addition); (2) UV-Photolyse oder kosmische Strahlung erzeugt Radikale (CH₃, OH, HCO) im Eis; (3) Radikale diffundieren bei moderate Erwärmung (T > 20 K) → rekombinieren zu COMs; (4) Desorption durch protostellare Wärme → COMs in Gasphasendetektion",
        answerC = "COMs entstehen durch quantenmechanisches Tunneln von C-Atomen durch Aktivierungsbarrieren in der Gasphase bei T = 10 K; Tunnelraten für C + O₂ → CO₂ sind ausreichend für beobachtete Häufigkeiten",
        answerD = "Komplexe organische Moleküle im ISM sind interplanetare Artefakte aus Kometen-Einschlägen; sie können nicht in situ in Molekülwolken synthetisiert werden",
        correctAnswer = 1, // B
        explanation = "ISM-Chemie bei 10 K: Gasphasen-Ionenchemie (H₃⁺ + CO → HCO⁺ etc.) und Kornoberflächenchemie koexistieren. Methanol-Route: CO physisorption auf H₂O-Eis → H-Addition (tunnelabhängig) → HCO• → H₂CO → CH₃O• → CH₃OH; experimentell bestätigt in Laborversuchen (Watanabe & Kouchi 2002, Fuchs et al. 2009). COMs bei Hot Corinos (L483, IRAS 16293-2422): dimethylether, methyl formate, glycolaldehyde detektiert mit ALMA/IRAM. Nicht-thermische Desorption: UV-Photodesorption, kosmische-Strahlen-Desorption und reaktive Desorption (chemische Energie bei Radikalrekombination treibt Desorption) sind kritisch für COM-Gasphasenabundanzen.",
        difficulty = 5,
        funFact = "Glycolaldehyd (HOCH₂CHO), ein einfacher Zucker, wurde 2000 im Galaktischen Zentrum (Sagittarius B2(N)) mit dem GBT-Teleskop entdeckt – der erste extraterrestrische Nachweis eines Zuckermoleküls war ein Hinweis auf die chemische Komplexität, die dem Leben vorausgeht."
    ),

    // Question 47 – PAH Hypothesis
    Question(
        categoryId = 2,
        questionText = "Polyzyklische aromatische Kohlenwasserstoffe (PAHs) sollen 10–20% des interstellaren Kohlenstoffs binden. Welche Beobachtungssignatur unterstützt die PAH-Hypothese, und wie verhalten sich PAHs in photodissoziativen Regionen (PDRs)?",
        answerA = "PAHs werden durch ihr charakteristisches Molekülions-Massenspektrum (GC-MS) im ISM direkt detektiert; sie sind thermisch stabil bei allen ISM-Temperaturen und zeigen keine UV-Photoionisation",
        answerB = "Unidentifizierte Infrarotemissionen (UIE/AIBs) bei 3.3, 6.2, 7.7, 8.6, 11.3 μm sind die PAH-Signatur (Léger & Puget 1984, Allamandola et al. 1985). In PDRs: UV-Photonen (6–13.6 eV) ionisieren PAHs (PAH → PAH⁺ + e⁻); PAH⁺ zeigt verschobene IR-Banden; PAH⁰/PAH⁺-Verhältnis ist Maß für lokales UV-Feld. Bei sehr hohem UV-Flux: photodissoziation von C₂H₂/C₂H von PAH-Rand (top-down-Chemie) liefert kleine Kohlenwasserstoffe",
        answerC = "PAHs werden ausschließlich durch Radio-Maser-Emission detektiert; ihre IR-Banden sind nicht unterscheidbar von polykristallinem Graphit",
        answerD = "PAHs in PDRs reagieren sofort mit H zu gesättigten Kohlenwasserstoffen; alle PAH-Emission stammt aus geheizten Staubkörnern ohne molekularen PAH-Charakter",
        correctAnswer = 1, // B
        explanation = "UIE/AIBs (Unidentified Infrared Emission Bands / Aromatic Infrared Bands): 3.3 μm (aromat. C-H Streckschwingung), 6.2/7.7 μm (C=C Strecken), 8.6 μm (C-H in-plane bend), 11.3 μm (C-H out-of-plane). Spitzer-Weltraumteleskop kartierte UIE in galaktischen Ebenen, Reflexionsnebeln, HII-Regionen. PAH-Ionisationsfraktion als UV-Felddetektor: G₀-UV-Parameer aus PAH⁺/PAH⁰-Verhältnis. Pyrene, coronen, coronene und größere Fullerene (C₆₀ bei 17.4 μm, Cami et al. 2010, Science) wurden detektiert. 'Top-down-Chemie': sehr große PAHs → fragmentieren zu kleinen Kohlenwasserstoffen → chemisches Recycling des ISM-Kohlenstoffs.",
        difficulty = 5,
        funFact = null
    ),

    // Question 48 – Ice Mantle Chemistry
    Question(
        categoryId = 2,
        questionText = "Eismantel auf interstellaren Staubkörnern bestehen hauptsächlich aus H₂O, CO, CO₂ und NH₃. Welche experimentellen Labor-Simulationen simulieren ISM-Photochemie, und welche prebiotisch relevanten Moleküle wurden dabei hergestellt?",
        answerA = "Eismantelchemie wurde ausschließlich theoretisch untersucht; Laborversuche mit ISM-analogen Eisgemischen bei 10 K sind technisch nicht realisierbar",
        answerB = "MALDI/TOF-Laborexperimente (Bernstein et al. 2002, Science; Munoz-Caro et al. 2002, Nature): H₂O:CH₃OH:HCN:NH₃-Eisgemische bei 15 K werden mit UV-Licht (Ly-α) bestrahlt und anschließend Raumtemperatur erwärmt → flüssige organische Residuen enthalten Aminosäuren (Glycin, Alanin), Hexamethylentetramin (HMT), Nukleosid-Analoga, dipeptide-Vorläufer – übereinstimmend mit Meteoriten-Organika (Murchison, Murray)",
        answerC = "Eismantelexperimente zeigen ausschließlich Bildung einfacher Moleküle (CO₂, H₂O₂); komplexe prebiotische Moleküle entstehen nicht, da Reaktionen bei 15 K kinetisch gehemmt sind",
        answerD = "ISM-Eisexperimente bei 10 K erfordern radioaktive Quellen (γ-Strahlung); UV-Bestrahlung ist zu energiearm für signifikante Chemie im Eis",
        correctAnswer = 1, // B
        explanation = "Bernstein et al. (2002) und Munoz-Caro et al. (2002) publizierten simultan in Science/Nature: H₂O:MeOH:HCN:NH₃-Eis (15 K, UV-Bestrahlung, dann warm aufgetaut) → GC-MS/LC-MS zeigte 16 Aminosäuren. Mechanistisch: Strecker-Synthese-analoge Reaktionen: Aldehyde + NH₃ + HCN → α-Aminonitril → Aminosäure. HMT-Bildung: NH₃ + HCHO → Hexamethylentetramin (bekannte hydrolytische Quelle von Aminosäuren). Murchison-Meteorit-Vergleich: über 70 Aminosäuren, davon viele enantiomere Überschüsse in nicht-proteinogenen L-Aminosäuren (δ¹³C-Isotopenanomalien zeigen außerterrestrische Herkunft). JWST (2022): erste direkte Eismantel-Detektion in den Taurus-Molekülwolken (CO₂, CH₄, CH₃OH-Eis).",
        difficulty = 5,
        funFact = "Der Murchison-Meteorit (gefallen 1969 in Australien) enthält über 14.000 verschiedene organische Moleküle – darunter Aminosäuren, Nucleobasen, Lipidvorläufer und sogar Diamanten mit presolaren Isotopenanomalien, die älter als das Sonnensystem sind."
    ),

    // Question 49 – Prebiotic Molecules in Space
    Question(
        categoryId = 2,
        questionText = "Phosphor ist ein essentielles Element für Nukleinsäuren und Phospholipide. Warum ist das kosmische P/H-Verhältnis trotz seiner Häufigkeit an der Erdoberfläche im ISM schwer zu messen, und wo wurde PN (Phosphormonooxid) und CP detektiert?",
        answerA = "Phosphor ist im ISM vollständig an Silikatstaubeisen gebunden und zeigt keine Gasphasenemission; alle Gasphasen-Phosphormoleküle sind theoretische Vorhersagen ohne Beobachtungsbestätigung",
        answerB = "Phosphor ist im ISM zu >99% in refraktären Phasen (Silikat-/Sulfid-Körner) depletiert, was Gasphase-P sehr selten macht (Depletion-Faktor >100×). PN wurde 1987 (Turner & Bally, ApJL) in IRC+10216 und Orion-KL in der Millimeter-Radioastronomie detektiert (J=2→1 Übergang bei 93.98 GHz); CP wurde ebenfalls in IRC+10216 entdeckt. In jüngster Zeit: ALMNO-Detektion von PO (Phosphormonooxid), PN und PH₃ in zirkumstellaren Hüllen sauerstoffreicher AGB-Sterne und in Massiv-Sternentstehungsregionen (z.B. W51, G+0.693-0.027 im Galakt. Zentrum)",
        answerC = "PN wurde ausschließlich in planetaren Atmosphären detektiert (Venus 2020); im ISM gibt es keine Phosphorchemie, da P₄-Moleküle zu stabil für chemische Reaktionen im kalten ISM sind",
        answerD = "Phosphormoleküle wie PN emittieren ausschließlich im sichtbaren Bereich (Elektronenübergänge); Radioteleskope sind für P-Detektion ungeeignet, da P keine Rotationsübergänge im Mikrowellenbereich hat",
        correctAnswer = 1, // B
        explanation = "Kosmische Häufigkeit P/H ≈ 2.6×10⁻⁷ (solar), aber im Diffusen ISM ist P zu ~99% depletiert in Staub. Phosphor-Astrochemie-Netzwerk: P + OH → PO; P + CN → CP; PN-Bildung unklar (dissoziativer Rekombination von PNH⁺?). IRAM 30m und ALMA Detektionen: PN (93.98 GHz), PO (108.99 GHz), CP in C-reichen Hüllen und zirkumstellaren Schalen. Galaktisches Zentrum-Wolke G+0.693-0.027: PN, PO und auch tentativ PH₃ (Rivilla et al. 2020 ApJL). Wichtig: Verhältnis PO/PN > 1 in O-reichen Sternen, < 1 in C-reichen Sternen – chemische Differenzierung. Phosphor-Kreis: Sternenatmosphären → ISM → protosolare Nebel → Planeten: relevanter astrobiologischer Pathway.",
        difficulty = 5,
        funFact = null
    ),

    // Question 50 – Deuterium Fractionation
    Question(
        categoryId = 2,
        questionText = "Deuterium-Fraktionierung im ISM führt zu D/H-Verhältnissen in Molekülen, die das kosmische D/H ≈ 2.5×10⁻⁵ um Faktoren von 10³–10⁴ übersteigen. Welcher Schlüsselprozess treibt diese extreme Fraktionierung in kalten dichten Wolken?",
        answerA = "Deuterium-Fraktionierung entsteht durch radioaktiven Zerfall von Tritium; ³H → D + β⁻ akkumuliert D in Eiskörnern",
        answerB = "Haupttreiber: Isotopengleichgewichts-Reaktion H₃⁺ + HD ⇌ H₂D⁺ + H₂ mit ΔE/k_B ≈ +232 K (Nullpunktenergiedifferenz). Bei T = 10–15 K liegt das Gleichgewicht stark auf der H₂D⁺-Seite (Boltzmann-Faktor exp(232/10) >> 1); H₂D⁺ überträgt D⁺ auf andere Moleküle (CO → DCO⁺, N₂H⁺ → N₂D⁺) mit kaskadierten D-Anreicherungen bis zu H₂D⁺/H₃⁺ ~ 0.1–1 und ND₃/NH₃ > 10⁻² beobachtet in L1544",
        answerC = "Deuterium-Fraktionierung entsteht durch Photoionisation bevorzugt von H gegenüber D; da D mehr Elektronen als H hat, wird D deutlich langsamer ionisiert und akkumuliert in Molekülen",
        answerD = "Extreme D-Fraktionierung ist ein Artefakt der Radioteleskopmessungen; tatsächliche D/H-Verhältnisse im ISM sind immer nahe dem kosmischen Wert von 2.5×10⁻⁵",
        correctAnswer = 1, // B
        explanation = "Zero-Point-Energy (ZPE) von H₂D⁺ ist geringer als von H₃⁺ + HD (D ist schwerer → niedrigere ZPE): ΔE/k_B = 232 K (experimentell: Gerlich, Schlemmer). Bei T << 232 K: K_eq >> 1 → H₂D⁺ massiv angereichert. Kaskade bei T=10K: H₂D⁺/H₃⁺ ~ 1; D₂H⁺/H₂D⁺ ~ 0.1; D₃⁺/D₂H⁺ detektierbar. Downstream: DCO⁺, N₂D⁺, HDCO, D₂CO, CHD₂OH, CD₃OH beobachtet. Pre-stellar Core L1544 (Caselli et al., Crapsi et al.): N₂D⁺/N₂H⁺ ~ 0.05–0.2; H₂D⁺/H₃⁺ ~ 0.1–1. Bedeutung: D-Fraktionierung als Thermometer/Dichtemesser für kalte Wolken; Verbindung zu kometen-D/H-Verhältnissen gibt Einblick in presolare Erbe im Sonnensystem.",
        difficulty = 5,
        funFact = "Das D/H-Verhältnis im Erdozean (~1.56×10⁻⁴) ist sechsmal höher als das kosmische Primordial-Verhältnis – ein Hinweis, dass ein bedeutender Teil des terrestrischen Wassers durch Kometen oder D-angereicherte Eismeteorite geliefert wurde, die ihre Deuterium-Signatur aus kalten ISM-Wolken ererbt haben."
    )

)
