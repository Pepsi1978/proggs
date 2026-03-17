package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun scienceQuestionsMaster2(): List<Question> = listOf(

    // ── MASTER 2 (difficulty = 5) ── 50 questions ────────────────────────────

    // ── BLOCK 1: Quantum Information Theory ──────────────────────────────────

    // Question 1 – Quantum Error Correction: Stabilizer Codes
    Question(
        categoryId = 2,
        questionText = "Was ist die minimale Anzahl physischer Qubits im Steane-Code [[7,1,3]], und welche Fehlertypen kann er exakt korrigieren?",
        answerA = "5 physische Qubits; er korrigiert beliebige Einzelqubit-Fehler (X, Z und Y gleichzeitig)",
        answerB = "7 physische Qubits; er kodiert 1 logisches Qubit und korrigiert alle Einzelqubit-Pauli-Fehler (X, Z, Y) mit Abstand d=3",
        answerC = "9 physische Qubits; er basiert auf dem Shor-Code und korrigiert nur Phasenfehler",
        answerD = "7 physische Qubits; er kodiert 2 logische Qubits und korrigiert ausschließlich Bitflip-Fehler",
        correctAnswer = 1, // B
        explanation = "Der Steane-Code [[7,1,3]] verwendet 7 physische Qubits um 1 logisches Qubit zu kodieren (daher [[n=7, k=1, d=3]]). Der Code-Abstand d=3 garantiert Korrektur aller Einzelqubit-Fehler. Er basiert auf dem klassischen Hamming-Code [7,4,3] und ist ein CSS-Code (Calderbank-Shor-Steane). Seine Stabilisatorgruppe wird von 6 Generatoren (3 X-Typ, 3 Z-Typ) erzeugt.",
        difficulty = 5,
        funFact = "Andrew Steane entwickelte diesen Code 1996. Der Steane-Code ist der kleinste fehlerkorrigierende CSS-Code und spielt eine Schlüsselrolle in der Theorie topologischer Codes."
    ),

    // Question 2 – Bell Inequalities / CHSH
    Question(
        categoryId = 2,
        questionText = "Die CHSH-Ungleichung lautet |⟨AB⟩ + ⟨AB'⟩ + ⟨A'B⟩ − ⟨A'B'⟩| ≤ 2 für klassische lokale verborgene Variablen. Welchen Maximalwert kann der linke Ausdruck für Quantenzustände annehmen, und wie heißt diese Schranke?",
        answerA = "Maximalwert 3, bekannt als Clauser-Horne-Shimony-Holt-Grenze",
        answerB = "Maximalwert 2√2 ≈ 2.828, bekannt als Tsirelson-Schranke",
        answerC = "Maximalwert 4, bekannt als No-Signaling-Schranke (Popescu-Rohrlich-Box)",
        answerD = "Maximalwert π, bekannt als Bell-Hardy-Schranke für verschränkte Triplettzustände",
        correctAnswer = 1, // B
        explanation = "Für Quantenzustände kann die CHSH-Größe maximal 2√2 ≈ 2.828 erreichen – dies ist die Tsirelson-Schranke (Boris Tsirelson, 1980). Sie wird vom maximalen Bell-Zustand (|00⟩+|11⟩)/√2 bei optimal gewählten Messrichtungen im 45°-Winkel zueinander erreicht. Die algebraische Maximum ist 4 (Popescu-Rohrlich No-Signaling Box), die jedoch physikalisch nicht realisierbar ist.",
        difficulty = 5,
        funFact = "Die ersten loopholefreien Bell-Tests wurden erst 2015 von Hensen et al. (Delft) und Giustina et al. (Wien) sowie Shalm et al. (NIST) durchgeführt – 51 Jahre nach Bells ursprünglichem Aufsatz."
    ),

    // Question 3 – Quantum Teleportation Protocol
    Question(
        categoryId = 2,
        questionText = "Im Standard-Quantenteleportationsprotokoll (Bennett et al. 1993): Warum zerstört die klassische Kommunikation von 2 Bits nach Alices Bell-Messung nicht das No-Cloning-Theorem?",
        answerA = "Weil die 2 klassischen Bits verschlüsselt übertragen werden und keine Quanteninformation enthalten",
        answerB = "Weil Alices Bell-Messung den ursprünglichen Zustand unweigerlich kollabiert; das Original wird vernichtet, während Bob mit den 2 Bits die richtige Pauli-Korrektur anwenden kann – es existiert niemals eine Kopie",
        answerC = "Weil das No-Cloning-Theorem nur für reine Zustände gilt, nicht für gemischte Zustände im Bell-Paar",
        answerD = "Weil die 2 klassischen Bits keine vollständige Zustandsinformation enthalten und Bob den Zustand ohne Alices Messbasisdaten nicht rekonstruieren kann",
        correctAnswer = 1, // B
        explanation = "Das No-Cloning-Theorem verbietet das Kopieren unbekannter Quantenzustände. Bei der Teleportation kollabiert Alices Bell-Messung ihren Teil des verschränkten Paars und vernichtet dabei Bobs Qubit-Kandidaten im unbekannten Zustand. Die 2 klassischen Bits kodieren Alices Messergebnis (00, 01, 10, 11) und sagen Bob, welche von vier Pauli-Operationen {I, X, Z, XZ} er anwenden soll. Das Original existiert nach der Messung nicht mehr – Teleportation, keine Duplikation.",
        difficulty = 5,
        funFact = null
    ),

    // Question 4 – Quantum Key Distribution
    Question(
        categoryId = 2,
        questionText = "Was ist der fundamentale Sicherheitsbeweis von BB84-QKD, und gegen welchen Angreifertyp gilt er uneingeschränkt?",
        answerA = "BB84 ist informationstheoretisch sicher gegen einen computationell unbeschränkten Angreifer (Eavesdropper), solange die Fehlerrate unter ~11% (QBER) bleibt und authentifizierte klassische Kanäle genutzt werden",
        answerB = "BB84 ist nur computationell sicher und beruht auf der Schwierigkeit des diskreten Logarithmus in elliptischen Kurven",
        answerC = "BB84 ist informationstheoretisch sicher, aber nur wenn der Quantenkanal vollständig verlustfrei ist und keine PNS-Angriffe möglich sind",
        answerD = "BB84 ist sicher gegen Lauschangriffe durch das No-Cloning-Theorem, erfordert aber Quantenspeicher auf Bobs Seite",
        correctAnswer = 0, // A
        explanation = "Der Sicherheitsbeweis von BB84 (Mayers 1996, Lo-Chau 1999, Shor-Preskill 2000) zeigt informationstheoretische Sicherheit: Ein Angreifer mit unbeschränkter Rechenleistung gewinnt keine Information, wenn die Quantum Bit Error Rate (QBER) unter ~11% liegt und Privacy Amplification angewendet wird. Die Sicherheit beruht auf dem No-Cloning-Theorem und dem Störungs-Detektion-Prinzip. Authentifizierung des klassischen Kanals ist notwendig, um Man-in-the-Middle-Angriffe auszuschließen.",
        difficulty = 5,
        funFact = "BB84 wurde 1984 von Charles Bennett und Gilles Brassard am IBM Research Center entwickelt – daher der Name. Peter Shor (des Shor-Algorithmus) lieferte 2000 den elegantesten Sicherheitsbeweis."
    ),

    // Question 5 – Topological Qubits (non-Majorana)
    Question(
        categoryId = 2,
        questionText = "In welchem physikalischen System werden nicht-abelsche Anyonen für topologische Quantencomputer vorgeschlagen, die NICHT auf Majorana-Fermionen basieren?",
        answerA = "Im ν=5/2 fraktionalen Quanten-Hall-System mit Moore-Read-Zustand (Ising-Anyonen)",
        answerB = "In Josephson-Junction-Arrays unterhalb der Supraleiter-Sprungtemperatur mit Cooper-Paar-Box-Qubits",
        answerC = "In photonischen Kristallresonatoren mit Kerr-Nichtlinearität bei kryogenen Temperaturen",
        answerD = "In supraleitenden Transmon-Qubits mit dispersiver Kopplung an Mikrowellen-Resonatoren",
        correctAnswer = 0, // A
        explanation = "Der fraktionale Quanten-Hall-Zustand bei Füllfaktor ν=5/2 wird durch den Moore-Read (Pfaffian)-Zustand beschrieben, der Ising-Anyonen (nicht-abelsche Anyonen) als Quasiteilchen-Anregungen besitzt. Diese nicht-abelschen Anyonen erlauben topologisch geschützte Quantengatter durch Braiding-Operationen. Da die Quanteninformation in der nicht-lokalen Topologie kodiert ist, ist sie gegen lokale Störungen immun. Dies ist konzeptuell verschieden von Majorana-Zero-Modes in supraleitenden Nanodrähten.",
        difficulty = 5,
        funFact = null
    ),

    // ── BLOCK 2: Advanced Cosmology ──────────────────────────────────────────

    // Question 6 – Inflation Models
    Question(
        categoryId = 2,
        questionText = "Was unterscheidet das chaotische Inflationsmodell (Linde 1983) vom Starobinsky-R²-Modell konzeptionell?",
        answerA = "Chaotische Inflation nutzt ein monomial Potential V(φ)∝φⁿ mit einem Skalarfeld in einem ungeordneten Anfangszustand; Starobinsky-Inflation modifiziert die Einstein-Hilbert-Wirkung durch einen R²-Term, der einem effektiven Skalarfeld (Staroboson) entspricht",
        answerB = "Chaotische Inflation erfordert Super-Planck-Skalen-Feldamplituden, während Starobinsky-Inflation ausschließlich im Bereich sub-Planck-Feldern operiert und kein Inflaton benötigt",
        answerC = "Beide Modelle sind mathematisch äquivalent durch eine konforme Transformation, erzeugen aber unterschiedliche Primordial-Gravitationswellen-Amplituden",
        answerD = "Starobinsky-Inflation ist ein Zweifeld-Modell mit Phantom-Skalarfeld, chaotische Inflation ist ein Ein-Feld-Modell mit negativem kinetischem Term",
        correctAnswer = 0, // A
        explanation = "Lindes chaotische Inflation (1983) verwendet Potentiale wie V(φ) = λφ⁴/4 oder V = m²φ²/2, wobei das Inflaton mit chaotischen Anfangsbedingungen (auch über der Planck-Skala) startet. Das Starobinsky-Modell (1980) modifiziert die Gravitation durch die Wirkung S = ∫√(-g)(R + R²/6M²)d⁴x. Durch konforme Transformation in den Einstein-Frame entsteht ein effektives Inflaton-Potential V = (3M²/4)(1-e^(-√(2/3)φ/Mpl))², das flach für große φ ist. Beide sagen unterschiedliche spektrale Indizes ns und tensor-zu-skalar Verhältnisse r voraus.",
        difficulty = 5,
        funFact = "Das Starobinsky-Modell ist derzeit das am besten mit Planck-Satellitendaten übereinstimmende Inflationsmodell: r < 0.056 bei ns ≈ 0.965."
    ),

    // Question 7 – Primordial Gravitational Waves
    Question(
        categoryId = 2,
        questionText = "Welcher B-Moden-Polarisationsparameter des CMB ist die Primärsignatur primordialer Gravitationswellen aus der Inflation, und was bestimmt seine Amplitude?",
        answerA = "Das Tensor-zu-Skalar-Verhältnis r = P_t/P_s, wobei r direkt proportional zur Energieskala der Inflation H_inf ist: r ≈ 16ε mit dem Slow-Roll-Parameter ε",
        answerB = "Der Spektralindex der B-Moden n_B, der durch das primordiale Spektrum der Skalarfluktuationen ΔR² bei k=0.05 Mpc⁻¹ festgelegt ist",
        answerC = "Der Tensor-Spektralindex n_T = -2ε, dessen Messung eine direkte Bestimmung des Hubble-Parameters während der Inflation erlaubt",
        answerD = "Das Verhältnis TT/TE des CMB-Leistungsspektrums, das bei Multipol ℓ ≈ 100 einen charakteristischen Knick durch Gravitationswellen zeigt",
        correctAnswer = 0, // A
        explanation = "Primordiale Gravitationswellen aus der Inflation erzeugen B-Moden-Polarisation im CMB. Das Tensor-zu-Skalar-Verhältnis r = P_t/P_s = A_t/A_s misst die relative Amplitude der Tensor- (Gravitationswellen) zu Skalar- (Dichteschwankungs-)Perturbationen. In slow-roll Inflation gilt r ≈ 16ε, wobei ε = -Ḣ/H² der erste slow-roll Parameter ist. Da H_inf ∝ V^(1/4) und r ≈ (H_inf/Mpl)², bestimmt r direkt die Inflations-Energieskala. Planck+BICEP legen r < 0.036 (95% CL) fest.",
        difficulty = 5,
        funFact = "Das BICEP2-Experiment (2014) berichtete zunächst r ≈ 0.2, was eine Sensation gewesen wäre – später stellte sich heraus, dass das Signal von galaktischem Staub dominiert wurde."
    ),

    // Question 8 – Dark Energy Equation of State
    Question(
        categoryId = 2,
        questionText = "Im Chevallier-Polarski-Linder (CPL)-Parametrisierungsschema der Dunklen Energie: Was bedeutet w_a ≠ 0 physikalisch, und was impliziert w₀ < −1?",
        answerA = "w_a ≠ 0 bedeutet zeitliche Evolution der Zustandsgleichung w(z) = w₀ + w_a·z/(1+z); w₀ < -1 impliziert 'Phantom-Energie' mit negativer kinetischer Energie, die zum Big Rip führen kann",
        answerB = "w_a ≠ 0 zeigt an, dass Dunkle Energie räumlich inhomogen ist; w₀ < -1 bedeutet, dass die kosmologische Konstante Λ mit der Zeit wächst",
        answerC = "w_a parametrisiert die Kopplung zwischen Dunkler Energie und Dunkler Materie; w₀ < -1 verletzt das Null-Energie-Kondition und ist physikalisch ausgeschlossen",
        answerD = "w_a ≠ 0 bedeutet, dass das Quintessenz-Feld einen Massenterm hat; w₀ = -1 entspricht einem kosmologischen Konstante-Universum",
        correctAnswer = 0, // A
        explanation = "Die CPL-Parametrisierung w(a) = w₀ + w_a(1-a) = w₀ + w_a·z/(1+z) beschreibt dynamische Dunkle Energie mit zwei Parametern: w₀ ist der heutige Wert und w_a die Entwicklungsrate. Für ΛCDM gilt w₀ = -1, w_a = 0. w₀ < -1 (Phantom-Energie) impliziert negativen kinetischen Term im Lagrangian (L ~ -∂μφ∂^μφ - V), was zur Null-Energie-Konditions-Verletzung führt; die Energiedichte ρ wächst mit der Expansion und kann zu einem 'Big Rip' führen. Neueste DESI 2024-Daten deuten auf w₀ ≈ -0.827, w_a ≈ -0.75 hin (Abweichung von ΛCDM auf ~2.5σ).",
        difficulty = 5,
        funFact = null
    ),

    // Question 9 – Cosmic Web Simulations
    Question(
        categoryId = 2,
        questionText = "Welches numerische Verfahren liegt dem Illustris-TNG-Simulationsprojekt zugrunde, und welche baryon-physikalischen Rückkopplungsprozesse sind für die Unterdrückung der Sternentstehung in massereichen Galaxien entscheidend?",
        answerA = "Reine N-Körper-SPH-Simulation mit isotroper Druckkraft; die Sternentstehung wird durch Supernova-Winde unterdrückt",
        answerB = "Adaptives Gitter-AMR-Verfahren mit Euler-Gleichungen; AGN-Feedback in Low-Kinetic-Mode unterdrückt Sternentstehung in Ellipsen",
        answerC = "Moving-Mesh-Hydrodynamik (AREPO-Code) mit magnetohydrodynamischen Gleichungen; AGN-Feedback in kinetic-wind-mode und radio-bubble-mode unterdrückt Star Formation Quenching in massereichen Halos",
        answerD = "Gitter-Boltzmann-Methode mit SPH-Kopplung; molekulare Kühlung und UV-Hintergrundstrahlung sind die dominanten Quenching-Mechanismen",
        correctAnswer = 2, // C
        explanation = "IllustrisTNG verwendet den AREPO-Code (Springel 2010) mit einer Moving-Mesh-Methode, die die Vorteile von SPH und festen Gittern kombiniert und MHD-Felder einschließt. Das Sub-Gitter-Modell für AGN-Feedback ist entscheidend für massereiche Galaxien (M* > 10¹¹ M☉): Im 'thermal/quasar mode' (hohes AGN-Accretion-Rate) heizt isotropes Feedback das Halo-Gas; im 'kinetic/wind mode' (niedriges Accretion) injiziert das AGN gerichtete kinetische Energie, die das Circum-Galactic-Medium aufheizt und Kühlung für Star Formation verhindert.",
        difficulty = 5,
        funFact = "Die IllustrisTNG-Simulationen umfassen Boxgrößen bis 300 Mpc pro Seite mit bis zu 20 Milliarden Auflösungselemente – ein kosmisches Web in einem Computer."
    ),

    // Question 10 – Recombination Physics
    Question(
        categoryId = 2,
        questionText = "Warum kann die kosmische Rekombination (z ≈ 1100) nicht durch einfache Saha-Gleichgewicht-Rechnung genau beschrieben werden, und welcher Mechanismus dominiert die tatsächliche H-Rekombinationsrate?",
        answerA = "Die Saha-Gleichung überschätzt die Rekombinationsrate, weil Lyman-α-Photonen das Medium optisch dicht machen und die 2p→1s Cascade blockieren; Zweiphotonen-Zerfall (2s→1s) mit A_{2s}=8.22 s⁻¹ ist der Flaschenhals",
        answerB = "Die Saha-Gleichung unterschätzt die Rekombination, weil hochenergetische Neutrinos das Plasma ionisieren; der dominante Kanal ist die Drei-Körper-Rekombination H+ + e⁻ + e⁻ → H + e⁻",
        answerC = "Die Saha-Gleichung ist ungenau wegen nicht-thermischen Photonen aus Quasaren bei z~1100; die dominante Rate ist die freie-freie Emission (Bremsstrahlung) ins CMB",
        answerD = "Die kosmische Rekombination erfolgt instantan und die Saha-Gleichung ist eine gute Näherung; Abweichungen kommen nur von Helium-Rekombination bei z~1800",
        correctAnswer = 0, // A
        explanation = "Die kosm. Rekombination ist kein Gleichgewichtsprozess: Lyman-α-Photonen (n=2→1 Übergang) werden sofort reabsorbiert (optische Dicke τ_Lyα >> 1), sodass der Hauptkanal H(2p)→H(1s)+γ blockiert ist. Der effektive Flaschenhals ist der verbotene Zweiphotonen-Zerfall H(2s)→H(1s)+2γ mit Rate A_{2s} ≈ 8.22 s⁻¹. Peebles (1968) und Zel'dovich-Kurt-Sunyaev (1968) entwickelten unabhängig die korrekte Drei-Niveau-Behandlung (1s, 2s, 2p + Kontinuum). Moderne Codes (HyRec, CosmoRec) inkludieren Korrekturen durch Helium, Lyman-α-Radiationstransfer und quanteninterferenzen für CMB-Präzisionskosmologie.",
        difficulty = 5,
        funFact = "Die genaue Berechnung der Rekombinationsgeschichte ist für die Interpretation von Planck-CMB-Daten kritisch: Fehler von 0.1% in der Rekombinationshistorie führen zu messbaren Fehlern in kosmologischen Parametern."
    ),

    // ── BLOCK 3: Epigenomics & Chromatin ─────────────────────────────────────

    // Question 11 – Histone Code Beyond H3K27me3
    Question(
        categoryId = 2,
        questionText = "Welche biochemische Beziehung besteht zwischen H3K4me3 und H3K27me3 an sogenannten 'bivalenten' Chromatin-Domänen, und in welchem Zelltyp sind sie besonders charakteristisch?",
        answerA = "H3K4me3 und H3K27me3 schließen sich gegenseitig aus (Crosstalk-Inhibition) und kommen nie auf demselben Nukleosom vor; sie sind typisch für terminal differenzierte Neuronen",
        answerB = "H3K4me3 (aktive Markierung) und H3K27me3 (repressive Markierung) koexistieren auf denselben Gen-Promotoren; diese 'Bivalenz' hält Entwicklungsgene in embryonalen Stammzellen in einem poisierten, potenziell aktivierbaren Zustand",
        answerC = "H3K4me3 wird durch EZH2 geschrieben und aktiviert Enhancer; H3K27me3 wird durch MLL-Komplexe geschrieben und reprimiert Promotoren in hämatopoetischen Vorläufern",
        answerD = "Bivalente Domänen bestehen aus H3K4me1 und H3K27ac und markieren aktive Enhancer in induzierten pluripotenten Stammzellen",
        correctAnswer = 1, // B
        explanation = "Bivalente Chromatin-Domänen (Bernstein et al., Cell 2006) tragen gleichzeitig die aktivierende Histon-Methylierung H3K4me3 (durch MLL/SET1-Komplexe) und die repressive Markierung H3K27me3 (durch Polycomb-Repressor-Komplex 2, PRC2/EZH2). Dieser scheinbar paradoxe Zustand hält Entwicklungsgene (z.B. Hox-Gene, Transkriptionsfaktoren) in embryonalen Stammzellen (ESCs) transienter Repression – sie sind 'gecancelt', können aber bei Differenzierungssignalen schnell aktiviert werden durch Entfernen von H3K27me3.",
        difficulty = 5,
        funFact = "Die Entdeckung bivalenter Domänen 2006 war eine Revolution im Epigenomik-Feld und erklärt die Pluripotenz embryonaler Stammzellen auf mechanistischer Ebene."
    ),

    // Question 12 – CTCF/Cohesin Loop Extrusion
    Question(
        categoryId = 2,
        questionText = "Welches molekulare Modell erklärt die Bildung von Chromatin-Schleifen durch Cohesin, und welche Rolle spielt CTCF dabei?",
        answerA = "Cohesin bildet aktiv Schleifen durch Motor-ATPase-Aktivität (Loop Extrusion); CTCF fungiert als direktionaler Barriereprotein, das Schleifen-Extrusion stoppt, wenn es in konvergenter Orientierung auf dem Genom lokalisiert ist",
        answerB = "CTCF dimerisiert über seinen N-Terminus und zieht zwei entfernte Genomregionen zusammen; Cohesin stabilisiert diese Brücken durch SMC-Komplex-Ringformation",
        answerC = "Cohesin gleitet passiv entlang der DNA durch Brownsche Bewegung; CTCF rekrutiert Cohesin und erhöht seine Halbwertszeit durch Phosphorylierung von SA1/SA2",
        answerD = "Loop Extrusion ist ein rein thermodynamischer Prozess ohne aktiven Motor; CTCF und Cohesin co-lokalisieren an Topologically Associating Domain (TAD)-Grenzen durch Phasenseparation",
        correctAnswer = 0, // A
        explanation = "Das Loop-Extrusion-Modell (Fudenberg et al. 2016; Sanborn et al. 2015) postuliert: Cohesin (ein SMC-Komplex) extrudiert aktiv Chromatin-Schleifen mit ~1 kb/s ATP-abhängig, indem es als molekularer Motor DNA durch seinen Ring zieht. CTCF (CCCTC-binding factor) mit seinen 11 Zink-Finger-Domänen bindet gerichtet an ~20 bp Motive. Wenn zwei CTCF-Moleküle in konvergenter Orientierung (→...←) auf zwei DNA-Regionen sitzen, stoppt die Extrusion und eine stabile Schleife entsteht. Dies erklärt TAD-Grenzen und reguliert Enhancer-Promotor-Kontakte.",
        difficulty = 5,
        funFact = null
    ),

    // Question 13 – TADs (Topologically Associating Domains)
    Question(
        categoryId = 2,
        questionText = "Welchen funktionellen Effekt hat die Deletion einer TAD-Grenze in Säugerzellen, und welches bekannte Krankheitsbeispiel illustriert dies?",
        answerA = "TAD-Grenzdeletionen führen zu verstärkter Heterochromatin-Bildung; als Beispiel dient die Deletion von H3K9me3-Islands in BRCA1-assoziierten Mammakarzinomen",
        answerB = "Deletion einer TAD-Grenze erlaubt ektopische Enhancer-Promotor-Kontakte zwischen benachbarten TADs ('Enhancer Hijacking'); dies führt z.B. bei EPHA4-Locus-Rearrangements zu Extremitäten-Malformationen oder bei präsumierten Onkogen-Aktivierungen in Leukämien",
        answerC = "TAD-Grenzdeletionen destabilisieren Cohesin-NIPBL-Interaktionen und führen zu Cornelia-de-Lange-Syndrom durch haploinsuffizienz",
        answerD = "Deletion einer TAD-Grenze verhindert die Replikations-Timing-Synchronisation; als Modell dient die ICF-Erkrankung durch DNMT3B-Mutationen",
        correctAnswer = 1, // B
        explanation = "TADs sind ~1 Mb große genomische Domänen, in denen Chromatinregionen bevorzugt miteinander wechselwirken. TAD-Grenzen verhindern ektopische Enhancer-Promotor-Kontakte zwischen benachbarten Domänen. Lupiáñez et al. (Nature 2015) zeigten: Deletionen, Inversionen oder Duplikationen der TAD-Grenzen am EPHA4-Locus führen zu 'Enhancer-Hijacking' und polydaktylischen Extremitätenmalformationen in Mäusen/Menschen. In Tumoren werden TAD-Grenzdeletionen genutzt, um Proto-Onkogene (z.B. MYC, TAL1) unter die Kontrolle aktiver Enhancer zu bringen.",
        difficulty = 5,
        funFact = "Das 3D-Genom-Forschungsfeld explodierte nach Entwicklung der Hi-C-Methode (Lieberman-Aiden et al., Science 2009) – TADs wurden erst 2012 durch Dixon et al. und Nora et al. beschrieben."
    ),

    // Question 14 – Single-Cell ATAC-seq
    Question(
        categoryId = 2,
        questionText = "Welches molekulare Prinzip liegt scATAC-seq zugrunde, und welche biologische Information wird damit erfasst, die Bulk-ATAC-seq nicht liefern kann?",
        answerA = "Hyperaktive Tn5-Transposase insertiert Sequenzierungs-Adapter bevorzugt in nukleosomenfreie (offene) Chromatinregionen; scATAC-seq kartiert Chromatinzugänglichkeit einzelner Zellen und ermöglicht Identifikation seltener Zellpopulationen und Trajektorien in heterogenen Geweben",
        answerB = "Biotin-markierte DNase I schneidet offenes Chromatin einzelner Zellen; scATAC-seq misst Histon-Acetylierungsmuster auf Einzelzellebene und liefert Transkriptionsfaktor-Bindestellen",
        answerC = "FAIRE-seq wird auf einzelne Zellen adaptiert durch Microfluidik-Isolation; scATAC-seq erfasst Methylierungs-Muster und laminassoziierte Domänen in einzelnen Zellkernen",
        answerD = "Anti-H3K27ac-ChIP auf Einzelzellebene mittels CUT&TAG-Variante; scATAC-seq erfasst aktive Enhancer-Repertoires und erlaubt Vorhersage von Zelltypidentitäten",
        correctAnswer = 0, // A
        explanation = "ATAC-seq (Assay for Transposase-Accessible Chromatin with sequencing, Buenrostro et al. 2013) nutzt hyperaktive Tn5-Transposase, die präferenziell in nukleosomenfreies, offenes Chromatin inseriert und dabei Sequenzier-Adapter einbaut. Single-cell ATAC-seq (scATAC-seq) führt diese Reaktion in einzelnen Zellen durch (Droplet-Microfluidik, z.B. 10x Chromium, oder Kombi-Index-Barcoding). Im Gegensatz zu Bulk-ATAC-seq, das Durchschnittssignale liefert, ermöglicht scATAC-seq: Identifikation seltener Zelltypen (<1%), Rekonstruktion von Differenzierungstrajektorien (Pseudotime), Beschreibung transkriptionsfaktor-regulatorischer Netzwerke in heterogenen Geweben.",
        difficulty = 5,
        funFact = null
    ),

    // Question 15 – Liquid-Liquid Phase Separation in Nucleus
    Question(
        categoryId = 2,
        questionText = "Welche Eigenschaften der intrinsisch ungeordneten Regionen (IDRs) von Transkriptionsfaktoren und Co-Aktivatoren treiben die Bildung transkriptioneller Kondensate durch Flüssig-Flüssig-Phasenseparation?",
        answerA = "Regelmäßige amphipathische Helices mit hydrophoben Leucin-Zippern und coiled-coil-Domänen; Kondensate entstehen durch Disulfidbrücken-Quervernetzung nach oxidativem Stress",
        answerB = "IDRs reich an aromatischen Aminosäuren (Tyr, Phe) und polaren Resten (Ser, Gln), die π-π-Stacking-, Kation-π- und elektrostatische Wechselwirkungen eingehen; schwache multivalente Interaktionen überschreiten den Saturationspunkt und treiben Kondensation",
        answerC = "N-terminale Transmembran-Helices und C-terminale ATP-bindende Kassetten; Kondensate entstehen durch Mg²⁺-abhängige Dimerisierung an nukleären Poren",
        answerD = "HEAT-Repeat-Domänen mit WD40-β-Propeller-Faltung; hydrophobe Cluster-Bildung durch Glycin-reiche Linker treibt Phasenseparation unter Hitze-Stress",
        correctAnswer = 1, // B
        explanation = "Flüssig-Flüssig-Phasenseparation (LLPS) wird durch IDRs getrieben, die reich an bestimmten Aminosäurensequenzmotiven sind (Boija et al., Cell 2018; Sabari et al., Science 2018). Aromatische Reste (Tyr in YxxQ-Motiven, Phe) bilden π-π-Stacking und Kation-π-Interaktionen; Ser/Gln-reiche low-complexity domains ermöglichen Wasserstoffbrücken. Multivalenz ist entscheidend: Viele schwache Bindungen (K_D ~ mM) überschreiten den Saturationspunkt und treiben Kondensation. Super-Enhancers rekrutieren BRD4 und Mediator in Kondensate, die ~100 nm große 'Hubs' für Transkriptionsfaktoren (OCT4, MYC) bilden.",
        difficulty = 5,
        funFact = "Transkriptionelle Kondensate lösen sich innerhalb von Sekunden auf, wenn RNA-Pol-II-CTD phosphoryliert wird (Ser2-Phosphorylierung während Elongation) – ein dynamischer Schalter für Gen-Aktivierung."
    ),

    // ── BLOCK 4: Mathematical Physics ────────────────────────────────────────

    // Question 16 – Gauge Theory Fiber Bundles
    Question(
        categoryId = 2,
        questionText = "In der differentialgeometrischen Formulierung einer Yang-Mills-Eichtheorie: Was ist ein Zusammenhang (Connection) auf einem Prinzipal-Faserbündel, und wie entspricht er dem Eichpotential?",
        answerA = "Ein Zusammenhang ist eine Lie-Algebra-wertige 1-Form A auf dem Prinzipal-G-Bündel P→M, die jedes tangentiale Vektorbündelelement in horizontale und vertikale Teile zerlegt; lokal entspricht A^a_μ dem Eichpotential, dessen Krümmung F = dA + A∧A dem Feldstärketensor entspricht",
        answerB = "Ein Zusammenhang ist eine G-äquivariante Abbildung vom Basisraum M in die Liegruppe G; lokal entspricht er dem kovarianten Ableitungsoperator D_μ = ∂_μ + igA_μ",
        answerC = "Ein Zusammenhang ist eine Riemannsche Metrik auf dem assoziierten Vektorbündel E = P ×_ρ V; die Holonomiegruppe des Zusammenhangs bestimmt die Eichgruppe G",
        answerD = "Ein Zusammenhang ist ein Schnitt (Section) des Jet-Bündels J¹(P); seine Kristallfläche (crystalline section) entspricht dem Eich-Ghost-Feld c^a in der BRST-Formulierung",
        correctAnswer = 0, // A
        explanation = "In der Atiyah-Singer-Formulierung ist eine Eichtheorie mit Gruppe G auf einem Prinzipal-G-Bündel π: P → M formuliert. Ein Zusammenhang (Ehresmann-Zusammenhang) ist eine g-wertige 1-Form ω ∈ Ω¹(P, g), die die tangentialen Vektorräume T_p P in horizontale H_p (physikalisch relevante) und vertikale V_p (Eichfreiheitsgrade) Unterräume aufteilt. Ein lokaler Schnitt s: U → P über einem offenen U ⊂ M zieht ω zurück auf das Eichpotential A = s*ω ∈ Ω¹(U, g). Die Krümmungsform F = dA + [A,A]/2 ∈ Ω²(U, g) entspricht dem Yang-Mills-Feldstärketensor F^a_{μν}.",
        difficulty = 5,
        funFact = null
    ),

    // Question 17 – Noether's Theorem Deep Form
    Question(
        categoryId = 2,
        questionText = "Was besagt Noethers zweites Theorem (1918) – im Unterschied zum ersten – und welche physikalische Konsequenz hat es für Eichtheorien?",
        answerA = "Das zweite Theorem gilt für diskrete Symmetrien (CPT); es impliziert die Quantisierung von Ladungen und liefert die Dirac-Quantisierungsbedingung für magnetische Monopole",
        answerB = "Das zweite Theorem betrifft unendlich-dimensionale kontinuierliche Symmetriegruppen (lokale/Eich-Symmetrien); es zeigt, dass die zugehörigen Noether-Ströme identisch verschwinden (off-shell) und liefert Bianchi-Identitäten sowie Zwangsbedingungen (Constraints) in der Hamiltonschen Formulierung",
        answerC = "Das zweite Theorem verallgemeinert das erste auf nicht-konservative Systeme mit Reibung; es liefert die Euler-Lagrange-Gleichungen für dissipativer Mechanik in nicht-kommutativer Geometrie",
        answerD = "Das zweite Theorem gilt für Symmetrien, die vom Feld und seinen Ableitungen abhängen; es liefert die Ward-Identitäten der Quantenfeldtheorie als klassische Analoga",
        correctAnswer = 1, // B
        explanation = "Emmy Noethers zweites Theorem (Math. Ann. 1918) betrifft Symmetrien mit unendlich-vielen Parametern – lokale/Gauge-Symmetrien. Für eine Eichsymmetrie G_loc mit Parametern ε^a(x) folgt: die variationellen Identitäten δS/δφ_i · R^i_a ≡ 0 gelten identisch (off-shell), nicht nur auf Lösungen. Dies führt zu: (1) Bianchi-Identitäten (D_μ F^μν = 0 in Yang-Mills); (2) Nicht-Invertierbarkeit der kinetischen Operatoren (Eichfreiheitsgrade müssen fixiert werden); (3) Zwangsbedingungen in Diracs Hamiltonscher Formalismus. Noethers zweites Theorem ist das mathematische Fundament aller modernen Eichtheorien.",
        difficulty = 5,
        funFact = "Emmy Noether bewies ihre zwei Theoreme in einem einzigen Aufsatz von 1918 – ein Werk, das Einstein als 'das bedeutendste kreative Werk der mathematischen Genies bisher produziert' bezeichnete."
    ),

    // Question 18 – Yang-Mills Mass Gap
    Question(
        categoryId = 2,
        questionText = "Was ist das Yang-Mills-Massenabstands-Problem (Mass Gap Problem) des Clay Millennium Prize, und warum ist es mathematisch außergewöhnlich schwierig?",
        answerA = "Es muss bewiesen werden, dass die quantisierte Yang-Mills-Theorie mit Eichgruppe SU(N) in 4D eine Masselücke Δ > 0 besitzt (das leichteste Glueball-Massenspektrum ist positiv), während die klassische Yang-Mills-Gleichung masselose Lösungen hat; die Schwierigkeit liegt in der nichtperturbativen Natur des Problems bei starker Kopplung",
        answerB = "Es muss gezeigt werden, dass die Renormierungsgruppe der Yang-Mills-Theorie einen IR-Fixpunkt besitzt, der zur Confinement-Phase führt; das Problem ist schwierig wegen der Landau-Pole bei niedrigen Energien",
        answerC = "Das Mass-Gap-Problem erfordert den Beweis der asymptotischen Freiheit in 4D, was Gross, Politzer und Wilczek 1973 leisteten; offen bleibt nur der rigorose mathematische Beweis für SU(3)",
        answerD = "Es muss bewiesen werden, dass Quarks durch Strings mit konstantem Spannungskoeffizienten (String-Spannung σ) gebunden sind; die Schwierigkeit ist die Regularisierung des Polyakov-Pfadintegrals",
        correctAnswer = 0, // A
        explanation = "Das Yang-Mills-Massenabstands-Problem (Jaffe-Witten, Clay Math 2000) verlangt: (1) Die rigorose mathematische Konstruktion der quantisierten Yang-Mills-Theorie in 4D als konformen Quantenfeldtheorie; (2) Beweis einer Masselücke Δ > 0 im Spektrum des Hamiltonoperators. Klassisch hat Yang-Mills masselose Felder (Gluonen); quantenmechanisch soll das Spektrum gebundene Zustände (Glueballs) mit Masse m ≥ Δ > 0 haben. Die Schwierigkeit: Renormierung, Confinement, und nichtperturbative Effekte sind nicht kontrollierbar durch Störungstheorie; Gitterberechnungen geben numerische Hinweise (m_{Glueball} ≈ 1.5–1.7 GeV), aber kein analytischer Beweis existiert.",
        difficulty = 5,
        funFact = null
    ),

    // Question 19 – Conformal Field Theory
    Question(
        categoryId = 2,
        questionText = "Was ist der Operator-Produkt-Entwicklungs-Koeffizient (OPE) C_{ijk} in einer 2D konformen Feldtheorie, und welche Daten bestimmen vollständig die CFT?",
        answerA = "Die OPE-Koeffizienten C_{ijk} beschreiben die Singularität beim Zusammenführen zweier Operatoren O_i(z)O_j(w); zusammen mit den konformen Dimensionen {Δ_i} bilden sie die 'CFT-Daten' die alle Korrelationsfunktionen bestimmen",
        answerB = "C_{ijk} sind die Strukturkonstanten der Virasoro-Algebra; sie bestimmen die zentrale Ladung c der CFT und gelten nur für minimale Modelle mit c < 1",
        answerC = "Die OPE-Koeffizienten sind identisch zu den 3-Punkt-Kopplungen im holographisch dualen AdS-Gravitationstheorie (AdS₃/CFT₂); sie sind alle rational für rationalen CFTs",
        answerD = "C_{ijk} parametrisieren die Crossing-Symmetrie des 4-Punkt-Blocks; sie werden durch die Zametolodchikov-Rekursionsrelation für beliebige zentrale Ladung c berechnet",
        correctAnswer = 0, // A
        explanation = "In 2D CFT (und allgemein d-dimensionaler CFT) lautet die OPE: O_i(x)O_j(0) = Σ_k C^k_{ij} |x|^{Δ_k-Δ_i-Δ_j} O_k(0) + Nachfolger. Die CFT-Daten bestehen aus: (1) Spektrum der primären Operatoren mit konformen Dimensionen (Δ, spin l); (2) OPE-Koeffizienten C_{ijk}. Diese Daten sind vollständig: Alle n-Punkt-Korrelationsfunktionen sind durch iterative OPE auf 2- und 3-Punkt-Funktionen reduzierbar. Crossing-Symmetrie (Bootstrap-Gleichungen) schränkt die CFT-Daten stark ein – dieses Conformal-Bootstrap-Programm (Polyakov 1970, Rattazzi et al. 2008) hat in letzter Zeit exakte Ergebnisse für kritische Exponenten des 3D Ising-Modells geliefert.",
        difficulty = 5,
        funFact = "Der Conformal Bootstrap lieferte 2014 den bisher präzisesten Wert für den kritischen Exponenten des 3D Ising-Modells: η = 0.036298(2) – genauer als Monte-Carlo-Simulationen."
    ),

    // Question 20 – Topological Quantum Field Theories
    Question(
        categoryId = 2,
        questionText = "Was ist ein Atiyah-Segal Axiom-System für eine TQFT, und welches physikalische Modell realisiert die Chern-Simons-TQFT konkret?",
        answerA = "Atiyah (1988) axiomatisierte TQFTs als monoidale Funktoren Z: Cob_n → Vect_k; die Chern-Simons-Theorie mit Wirkung S = (k/4π)∫Tr(A∧dA + 2/3 A∧A∧A) in 3D ist eine TQFT und ihre Zustandsräume sind Konformalblöcke der WZW-CFT",
        answerB = "Atiyah-Axiome beschreiben TQFTs als lokale Quantenfeldtheorien ohne Metrik-Abhängigkeit; die BF-Theorie in 4D mit Wirkung ∫B∧F ist die einzige bekannte 4D-TQFT mit endlich-dimensionalen Hilbert-Räumen",
        answerC = "TQFTs sind durch Seiberg-Witten-Invarianten definiert; die Donaldson-TQFT klassifiziert glatte 4-Mannigfaltigkeiten durch Berechnung von Instanton-Moduli-Räumen",
        answerD = "Atiyah-Axiome fordern Diffeomorphismus-Invarianz und Faktorisierung über Schnitte; Chern-Simons ist eine 2D-TQFT mit Zustandsräumen, die Darstellungen der Braid-Gruppe sind",
        correctAnswer = 0, // A
        explanation = "Michael Atiyah (1988) axiomatisierte TQFTs: Eine n-dim TQFT ist ein symmetrisch monoidaler Funktor Z: Cob_n → Vect_k, der (n-1)-dim Mannigfaltigkeiten Σ Vektorräume Z(Σ) (Hilbert-Räume) zuordnet und n-dim Kobordismen W: Σ₁ → Σ₂ lineare Abbildungen Z(W): Z(Σ₁) → Z(Σ₂). Chern-Simons-Theorie (Witten 1989) mit halber-ganzer Kopplungskonstante k ist eine 3D-TQFT: Ihre Partitionsfunktionen berechnen topologische Invarianten von 3-Mannigfaltigkeiten (Witten-Reshetikhin-Turaev-Invarianten), und die Zustandsräume auf einer Riemann-Fläche Σ_g sind die Konformalblöcke der G_k-WZW-CFT.",
        difficulty = 5,
        funFact = "Ed Witten erhielt 1990 die Fields-Medaille – die höchste Auszeichnung der Mathematik – als erster und bisher einziger Physiker, primär für seine Arbeit über topologische Quantenfeldtheorien."
    ),

    // ── BLOCK 5: Catalysis & Surface Science ─────────────────────────────────

    // Question 21 – Sabatier Principle
    Question(
        categoryId = 2,
        questionText = "Was besagt das Sabatier-Prinzip in der heterogenen Katalyse, und wie erklärt es die 'Volcano-Kurve' beim Ammoniaksynthese-Katalysator?",
        answerA = "Das Sabatier-Prinzip: Ein optimaler Katalysator bindet Intermediate weder zu stark noch zu schwach; für die Ammoniaksynthese zeigt die Volcano-Plot (Aktivität vs. N-Adsorptionsenergie ΔE_N) ein Maximum bei Fe und Ru, weil zu starke N-Bindung die Desorption limitiert, zu schwache die Aktivierung",
        answerB = "Das Sabatier-Prinzip besagt, dass die Reaktionsrate maximal ist, wenn die Aktivierungsenergie dem Thermodynamischen ΔG° entspricht; Eisen ist optimal weil ΔG°_N2dissoc ≈ 0 eV auf Fe(111)",
        answerC = "Das Sabatier-Prinzip beschreibt das optimale Verhältnis von Promotor zu Katalysator-Oberfläche; Ammoniaksynthese ist optimal bei Fe mit K₂O-Promotor durch Brønsted-Evans-Polanyi-Relation",
        answerD = "Das Sabatier-Prinzip gilt nur für Langmuir-Hinshelwood-Mechanismen; Ammoniaksynthese folgt einem Eley-Rideal-Mechanismus auf Fe(111) mit Dissoziation als RDS",
        correctAnswer = 0, // A
        explanation = "Das Sabatier-Prinzip (Paul Sabatier, ~1900) besagt: Ideale Katalysatoren binden Reaktionsintermediate weder zu stark (Vergiftung/Sättigung der Oberfläche) noch zu schwach (keine Aktivierung). Die Volcano-Kurve (Nørskov et al., ~2000) trägt die katalytische Aktivität gegen einen Deskriptor (z.B. N-Adsorptionsenergie ΔE_N) auf. Für Ammoniaksynthese N₂ + 3H₂ → 2NH₃: Metalle wie Mo und W binden N zu stark (linke Seite, N₂-Dissoziation schnell aber N-Desorption limitiert); Pd, Pt binden zu schwach (rechte Seite, Dissoziation limitierend). Fe und Ru sitzen nahe am Optimum (Volcano-Gipfel). Die Brønsted-Evans-Polanyi (BEP)-Relation verbindet Aktivierungsenergien mit thermodynamischen Bindungsenergien.",
        difficulty = 5,
        funFact = "Das Haber-Bosch-Verfahren auf Eisenkatalysator ernährt heute ~50% der Weltbevölkerung – ohne synthetischen Stickstoffdünger würde die Erde nur ~4 Milliarden Menschen ernähren können."
    ),

    // Question 22 – Surface Plasmon Resonance
    Question(
        categoryId = 2,
        questionText = "Welcher physikalische Mechanismus liegt der Oberflächenplasmonresonanz (SPR) zugrunde, und warum erfordert die Anregung propagierender Oberflächenplasmonen einen Prisma oder ein Gitter?",
        answerA = "SPR entsteht durch kollektive Oszillation freier Elektronen an der Metall-Dielektrikum-Grenzfläche, gekoppelt an evaneszente elektromagnetische Wellen; zur Anregung muss der In-Plane-Impuls des Photons k_|| = (ω/c)n sinθ dem Oberflächenplasmon-Wellenvektor k_SP angepasst werden, da k_SP > ω/c (Impulsanpassungsbedingung)",
        answerB = "SPR entsteht durch Totalreflexion an der Metall-Glas-Grenzfläche; das Prisma ist notwendig um Totalreflexion zu erzeugen, welche die Plasmonen-Welle induziert",
        answerC = "Oberflächenplasmonen sind stehende Wellen freier Elektronen in Metallnanopartikeln; Prisma-Kopplung (Kretschmann-Geometrie) erlaubt Anregung durch Brechungsindex-Matching des Prismas mit dem Metall",
        answerD = "SPR erfordert Prisma-Kopplung weil Oberflächenplasmonen nur bei p-Polarisation des Lichts anregbar sind; s-polarisiertes Licht kann keine transversale Metallgitteroszillation erzeugen",
        correctAnswer = 0, // A
        explanation = "Propagierende Oberflächenplasmonen-Polaritonen (SPPs) sind hybride Elektron-Photon-Moden an Metall-Dielektrikum-Grenzflächen. Die SPP-Dispersion k_SP = (ω/c)√(ε_m ε_d/(ε_m+ε_d)) liegt immer rechts der Lichtlinie k = (ω/c)√ε_d – Oberflächenplasmonen haben mehr Impuls als Photonen gleicher Energie. Freie Photonen können daher keine SPPs direkt anregen (Impuls-Mismatch). Die Kretschmann-Geometrie nutzt ein dichteres Prisma (n_p > n_d): Durch interne Totalreflexion entsteht ein evaneszentes Feld, das bei k_|| = k_SP resonant an das Plasmon koppelt. Gitter-Kopplung funktioniert durch reziproker Gittervektor G: k_|| + nG = k_SP.",
        difficulty = 5,
        funFact = null
    ),

    // Question 23 – Metal-Organic Frameworks
    Question(
        categoryId = 2,
        questionText = "Was ist die 'sekundäre Baueinheit' (SBU, Secondary Building Unit) in Metal-Organic Frameworks, und wie beeinflusst die Wahl der SBU die topologische Vernetzung des MOFs?",
        answerA = "SBUs sind vordefinierte molekulare Fragmente – meist polynukleare Metallcluster (z.B. Zn₄O-Tetraeder im MOF-5, Cu₂-Paddlewheel in HKUST-1) mit definierten Bindungspunkten; die SBU-Geometrie (tetraedrisch, quadratisch-planar, oktaedrisch) bestimmt die Topologie des resultierenden Netzwerks (z.B. pcu, nbo, rht)",
        answerB = "SBUs sind die organischen Linker-Moleküle (Dicarbonsäuren, Triazole) in MOFs; ihre Länge bestimmt die Porengröße, während die Metallionen nur die Vernetzungspunkte bilden",
        answerC = "SBUs bezeichnen die kleinste Wiederholungseinheit in der Elementarzelle; für MOF-5 (Zn₄O(BDC)₃) ist die SBU identisch mit der asymmetrischen Einheit der Kristallstruktur",
        answerD = "SBUs sind reaktive Vorläufermoleküle für die solvothermale Synthese; Zn(NO₃)₂ und H₂BDC bilden bei 120°C eine SBU, die spontan zum kubischen MOF-5 kristallisiert",
        correctAnswer = 0, // A
        explanation = "Das SBU-Konzept (Yaghi et al., Nature 2003) ist zentral für das rationale Design von MOFs. SBUs sind vordefinierte anorganische Cluster mit definierten Direktionalitäten der Bindungspunkte: (1) Zn₄O(CO₂)₆ in MOF-5: oktaedrische 6-verbundene SBU → pcu (primitive cubic) Topologie; (2) Cu₂(CO₂)₄ Paddlewheel: quadratisch-planare 4-verbundene SBU → nbo-Topologie mit HKUST-1; (3) Zr₆O₄(OH)₄(CO₂)₁₂ in UiO-66: 12-verbundene SBU → fcu-Topologie. Die Kombination von SBU-Geometrie und Linker-Topologie (ditopisch, tritopisch) nach der Retikular-Chemie-Strategie erlaubt Vorhersage und Synthese neuer MOF-Topologien.",
        difficulty = 5,
        funFact = "MOF-210 (Yaghi, 2010) hat eine BET-Oberfläche von 6240 m²/g – mehr als ein Fußballfeld in einem Gramm Material."
    ),

    // Question 24 – Electrocatalysis for CO2 Reduction
    Question(
        categoryId = 2,
        questionText = "Warum ist Kupfer unter Übergangsmetall-Elektrokatalysatoren für die elektrochemische CO₂-Reduktion einzigartig, und welche mechanistischen Schritte kontrollieren die Produktselektivität für C₂-Verbindungen?",
        answerA = "Kupfer ist einzigartig weil es der einzige Elektrokatalysator mit CO-Bindungsenergie nahe am Sabatier-Optimum ist; C₂-Selektivität wird durch CO-Deckungsgrad und Dimerisierung von *CO-Intermediaten auf Cu(100) bestimmt",
        answerB = "Kupfer ist einzigartig wegen seines hohen Sauerstoff-Affinität; Cu₂O-Oberflächen katalysieren die C-C-Kupplungsreaktion durch Mars-van-Krevelen-Mechanismus",
        answerC = "Kupfer zeigt eine mittlere CO-Bindungsenergie (nicht zu stark wie Mo, nicht zu schwach wie Au/Ag), was CO-Akkumulation und C-C-Kupplung erlaubt; C₂-Produktselektivität (Ethylen, Ethanol) wird durch *CO-*CO-Dimerisation auf Cu(100)-Facetten und pH-abhängige Protonierungsschritte kontrolliert",
        answerD = "Kupfer katalysiert CO₂-Reduktion zu Formiat über einen einzigartigen Carbid-Mechanismus; C₂-Produkte entstehen durch Kettenelongation analog zum Fischer-Tropsch-Prozess",
        correctAnswer = 2, // C
        explanation = "Kupfer ist das einzige Übergangsmetall, das CO₂ elektrochemisch zu C₂+-Verbindungen (Ethylen C₂H₄, Ethanol C₂H₅OH, Propanol) reduziert. Der Grund: Auf der CO-Volcano-Kurve hat Cu mittlere CO-Bindungsenergie (~0.67 eV); schwach bindende Metalle (Au, Ag) akkumulieren kein *CO, stark bindende (Mo, Ru) werden vergiftet. Auf Cu(100) dimerisieren zwei *CO zu *OC-CO* (C-C-Kupplungsschritt bei ~-0.35 V vs. RHE) – dies ist der selektivitätsbestimmende Schritt für C₂-Produkte. Die Protonierungssequenz danach bestimmt Ethylen vs. Ethanol. Facettenkontrolle (Cu(100) > C₂, Cu(111) > CH₄) und Oxidzustands-Kontrolle sind aktive Forschungsgebiete.",
        difficulty = 5,
        funFact = "Peter Jørgensen und Jens Nørskov (DTU/Stanford) entwickelten die computergestützte Volcano-Methode für Elektrokatalysatoren – sie beschleunigt die Entdeckung neuer CO₂-Reduktionskatalysatoren drastisch."
    ),

    // Question 25 – Heterogeneous Catalysis: Mechanisms
    Question(
        categoryId = 2,
        questionText = "Was unterscheidet den Langmuir-Hinshelwood (LH)- vom Eley-Rideal (ER)-Mechanismus in der heterogenen Katalyse, und für welche industriell relevante Reaktion wurde der ER-Mechanismus auf Metalloberflächen rigorös nachgewiesen?",
        answerA = "LH: Beide Reaktanden adsorbieren auf der Oberfläche, diffundieren und reagieren; ER: Ein Reaktand adsorbiert, der andere reagiert direkt aus der Gasphase; ER wurde für die Eley-Rideal-Hydrierung von Ethen auf Pd(111) bei Tieftemperatur (80 K) mit H-Atomen nachgewiesen",
        answerB = "LH: Sequentielle Adsorption und Reaktion im Fließgleichgewicht; ER: Termolekulare Reaktion an Stufenkanten; ER ist relevant für NO-Dissoziation auf Pt in Dreiwegekatalysatoren",
        answerC = "LH und ER sind kinetisch äquivalent und nur durch Isotopen-Markierung unterscheidbar; für CO-Oxidation auf Ru(0001) wurde LH durch STM bei 300 K direkt visualisiert",
        answerD = "LH erfordert hohe Drücke (>1 bar), ER dominiert im Ultrahochvakuum; dies erklärt die Drucklücke in der heterogenen Katalyse-Forschung",
        correctAnswer = 0, // A
        explanation = "Im Langmuir-Hinshelwood-Mechanismus adsorbieren beide Reaktanden (A* + B*) auf der Oberfläche, diffundieren und reagieren: A* + B* → P* + *. Im Eley-Rideal-Mechanismus reagiert ein adsorbiertes Molekül A* direkt mit einem nicht-adsorbierten Molekül B aus der Gasphase: A* + B(g) → P* (oder P + *). Der ER-Mechanismus wurde für die Reaktion von adsorbierten H-Atomen H* mit gasförmigem H*(D*) auf verschiedenen Metallober­flächen und für die Hydrierung von sp²-hybridisierten Kohlenstoffverbindungen (Vinyl-Gruppen) auf Pd(111) bei T < 100 K rigorös durch He-Atom-Streuung und Molecular-Beam-Experimente nachgewiesen. Die meisten thermischen Katalyse-Reaktionen folgen LH.",
        difficulty = 5,
        funFact = null
    ),

    // ── BLOCK 6: Neuroscience – Computation ──────────────────────────────────

    // Question 26 – Neural Coding Theories
    Question(
        categoryId = 2,
        questionText = "Was ist der Unterschied zwischen Rate-Coding und Temporal-Coding in der Neurocodierungs-Theorie, und welcher experimentelle Befund an Retina-Ganglienzellen unterstützt Temporal-Coding?",
        answerA = "Rate-Coding: Information in mittlerer Feuerrate (spikes/s); Temporal-Coding: Information in präzisen Spike-Zeitpunkten relativ zu einem Referenzsignal; Retina-Ganglienzellen zeigen phasengenaue Spikes (Jitter <1 ms) auf natürliche Bilder, die durch Rate-Coding allein nicht erklärt werden – unterstützt Temporal-Coding",
        answerB = "Rate-Coding ist auf primäre Sensorik beschränkt; Temporal-Coding (theta-gamma-Koppelung) tritt nur im Hippocampus auf; Retina-Ganglienzellen verwenden ausschließlich Rate-Coding mit Webers-Gesetz-Kompression",
        answerC = "Rate-Coding wurde durch Sherringtons motorische Entladungsfrequenz-Studien definiert; Temporal-Coding ist ein rein theoretisches Konzept ohne experimentellen Befund in der Retina",
        answerD = "Rate-Coding und Temporal-Coding sind mathematisch äquivalent durch die Wiener-Khinchin-Zerlegung des Spike-Trains; der Unterschied ist nur für Kohärenzzeiten > 100 ms relevant",
        correctAnswer = 0, // A
        explanation = "Rate-Coding (Adrian 1926): Information kodiert in mittlerer Spikerate r(t) über Zeitfenster ~100 ms. Temporal-Coding: Information in präzisen Spike-Zeitpunkten oder inter-spike-intervals (ISIs). Bialek et al. (1991, Science) zeigten an Frosch-Retina-Ganglienzellen: Spike-Zeitpunkte auf natürliche Bilder sind hochpräzise reproduzierbar (Jitter <1 ms), während Rate-Coding-Vorhersagen signifikant weniger Information trägt. Strong et al. (1998) demonstrierten mit entropischen Methoden, dass temporale Präzision >2× mehr Information trägt als Rate allein. Dies unterstützt Temporal-Coding, obwohl die Debatte Rate- vs. Temporal-Coding noch nicht vollständig entschieden ist.",
        difficulty = 5,
        funFact = null
    ),

    // Question 27 – Predictive Coding Framework
    Question(
        categoryId = 2,
        questionText = "Was ist der Kernmechanismus des Predictive-Coding-Frameworks (Rao & Ballard 1999) in der kortikalen Hierarchie, und welche kortikale Lamina-Spezifizität wird für Vorhersage-Signale vs. Fehler-Signale postuliert?",
        answerA = "Höhere kortikale Areale senden Vorhersagen (predictions) über Feedback-Verbindungen zu tieferen Arealen; Diskrepanzen erzeugen Vorhersagefehler (prediction errors), die feedforward nach oben weitergeleitet werden; Vorhersagen verlaufen über Schicht 1 und 6 (apikale Dendriten der Pyramidenzellen), Fehler-Signale über Schicht 4 (Inputs aus unteren Arealen)",
        answerB = "Predictive Coding postuliert, dass der Thalamus alle Vorhersagen generiert und der Kortex nur Fehler verarbeitet; thalamokortikale Projektionen in Schicht 4 tragen Vorhersagen, kortikothalamische Schicht 6-Projektionen tragen Fehler",
        answerC = "Alle kortikalen Schichten tragen gleichermaßen zu Vorhersage und Fehler bei; die Lamina-Spezifizität betrifft nur die zeitliche Verarbeitungssequenz (Gamma für Vorhersage, Beta für Fehler)",
        answerD = "Predictive Coding bezieht sich ausschließlich auf den visuellen Kortex (V1-V5); Vorhersagen werden in Schicht 2/3 der höheren Areale generiert und in Schicht 5 der niederen Areale integriert",
        correctAnswer = 0, // A
        explanation = "Rao & Ballard (Nature Neuroscience 1999) formalisierte Predictive Coding für visuellen Kortex: Jedes kortikale Areal enthält 'Repräsentations-Einheiten' die Vorhersagen über Input von niedrigerem Areal generieren und 'Fehler-Einheiten' die Diskrepanz zwischen Vorhersage und tatsächlichem Input codieren. Die anatom. Hypothese (Bastos et al. 2012): Feedback-Verbindungen (Vorhersagen) projizieren in Schicht 1 und apikale Dendriten von Schicht 5-Pyramidenzellen; Feedforward-Verbindungen (Fehler) aus Schicht 2/3 projizieren in Schicht 4 höherer Areale. Diese Lamina-Spezifizität ist konsistent mit bekannter kortikaler Wiring-Anatomie. Gamma-Oszillationen (>30 Hz) korrelieren mit Feedforward/Fehlern, Alpha/Beta (<20 Hz) mit Feedback/Vorhersagen.",
        difficulty = 5,
        funFact = "Karl Friston hat das Predictive-Coding-Framework zum 'Free Energy Principle' verallgemeinert, das Wahrnehmung, Handlung und sogar Autopoiesis unter einem mathematischen Prinzip (Minimierung freier Energie) vereinigt."
    ),

    // Question 28 – Grid Cells / Moser
    Question(
        categoryId = 2,
        questionText = "Was sind die charakteristischen Eigenschaften von Grid-Zellen im medialen entorhinalen Kortex (MEC), und welche Kodierungseigenschaft macht sie zu einem effizienten neuronalen Positionscode?",
        answerA = "Grid-Zellen feuern in einem einzigen räumlichen Feld pro Umgebung; sie sind die direkte Input-Quelle für Place-Zellen im CA3 und kodieren Raumrichtungen durch head-direction-Modulation",
        answerB = "Grid-Zellen feuern in regelmäßigen hexagonalen Gittermuster (triangular lattice) über den gesamten Raum; verschiedene Grid-Zellen haben unterschiedliche Skalierungen, Orientierungen und Phasen des Gitters; das Gesamtensemble bildet einen effizienten metrischen Ortscode durch Populationsvektorcodierung",
        answerC = "Grid-Zellen sind ausschließlich im Layer 2 des MEC und kodieren nur in 1D-Umgebungen (Linearlabyrinthe); in 2D gibt es keine Hexagonalität, nur irreguläre Feuerfelder",
        answerD = "Grid-Zellen feuern in quadratischen Gittern deren Skalierung durch die Theta-Präzessions-Frequenz des Hippocampus bestimmt wird; die Gitter-Periode entspricht immer der mittleren Schrittlänge des Tieres",
        correctAnswer = 1, // B
        explanation = "Moser, Moser et al. (Hafting et al., Nature 2005) entdeckten Grid-Zellen im medialen entorhinalen Kortex (MEC). Grid-Zellen feuern wenn sich das Tier an Orten befindet, die ein regelmäßiges hexagonales (triangulares) Gitter im 2D-Raum bilden – mehrere Feuerfelder pro Zelle im Gegensatz zu Place-Zellen (1 Feld). Grid-Module haben unterschiedliche räumliche Skalierungen (Periode ~25 cm bis mehrere Meter), Orientierungen und Phasen; innerhalb eines Moduls sind alle Grid-Zellen skalierungs-kohärent. Fiete et al. zeigten: Durch kombinierte Nutzung mehrerer Grid-Module mit unterschiedlichen Skalen (CRT-Prinzip) kann der Raum exponentiell feiner aufgelöst werden als durch einzelne Module – ein effizienter Populationscode.",
        difficulty = 5,
        funFact = "John O'Keefe (Place-Zellen, 1971) und Edvard und May-Britt Moser (Grid-Zellen, 2005) teilten sich 2014 den Nobelpreis für Physiologie/Medizin für die Entdeckung des 'GPS-Systems' des Gehirns."
    ),

    // Question 29 – Connectomics
    Question(
        categoryId = 2,
        questionText = "Welches Volumendarstellungs-Verfahren liegt modernen Konnektomik-Projekten (z.B. H01, MICrONS) zugrunde, und was ist die primäre Herausforderung bei der Rekonstruktion synaptischer Konnektivität im menschlichen Kortex?",
        answerA = "Serial Section Electron Microscopy (ssSEM) oder Focused Ion Beam SEM (FIB-SEM) mit ~4–8 nm Auflösung für Synapsen; die Hauptherausforderung ist die automatisierte Segmentierung der Neuritenbäume aus petabyte-großen EM-Datensätzen mit Deep-Learning (z.B. Flood-Filling-Netzwerke, FFN)",
        answerB = "X-ray Computed Tomography (microCT) mit Osmium-Staining; die Hauptherausforderung ist die Abgrenzung von inhibitorischen und exzitatorischen Synapsen bei 50 nm Voxelgröße",
        answerC = "Multi-Photonen-Lichtblattmikroskopie mit EXPANSION-Mikroskopie (ExM); die Hauptherausforderung ist die Staining-Penetration in dicke (>100 μm) Gewebeschnitte mit konventionellen Antikörpern",
        answerD = "STED-PALM/STORM-Nanoskopie kombiniert mit mGreenLantern-GRASP-Synapsen-Reportern; die Hauptherausforderung ist die axiale Auflösung von <50 nm für Dornfortsatz-Synapsen in vivo",
        correctAnswer = 0, // A
        explanation = "Konnektomik-Projekte (H01: Shapson-Coe et al. 2021, 1 mm³ menschlicher Kortex; MICrONS: ~1 mm³ Maus-Kortex) verwenden Serial Section TEM/SEM: Das Gewebe wird in ultradünne Scheiben (~30–50 nm) geschnitten, jede mit EM abgebildet, dann 3D-rekonstruiert. Die Auflösung (~4–8 nm/px in xy, 30–40 nm in z) ist nötig um Synapsen (postsynaptische Dichten ~40 nm) und kleine Axone (<100 nm) zu erkennen. Die Hauptherausforderung: automatisierte Segmentierung der ineinander verschachtelten Neuriten aus 1.4 Petabyte großen Datensätzen (H01). Flood-Filling-Networks (Januszewski et al. 2018, Google) sind der Stand der Technik, erfordern aber noch manuelles Proofreading für ~5–10% der Fehler.",
        difficulty = 5,
        funFact = null
    ),

    // Question 30 – Bayesian Brain Hypothesis
    Question(
        categoryId = 2,
        questionText = "Was ist die mathematische Formulierung der Bayes'schen Gehirnhypothese für Wahrnehmung, und welche experimentellen Befunde sprechen für probabilistische Inferenz im menschlichen Gehirn?",
        answerA = "Das Gehirn berechnet den Maximum-Likelihood-Schätzer P(Stimulus|Sensordaten); die Bayes-Koinzidenz-Regel erklärt Ernst & Banks' (2002) visuo-haptische Kalibrierung durch additive Gewichtung unabhängiger sensorischer Kanäle",
        answerB = "Das Gehirn berechnet die Posterior-Verteilung P(Ursache|Sensordaten) ∝ P(Sensordaten|Ursache)·P(Ursache) durch probabilistische Inferenz; Ernst & Banks (2002) zeigten optimale Kombination visueller und haptischer Tiefencues gemäß MLE-Schätzer (Maximum-Likelihood-Kombination): σ²_comb = σ²_vis·σ²_hap/(σ²_vis + σ²_hap)",
        answerC = "Die Bayes'sche Gehirnhypothese postuliert, dass Apriori-Wissen ausschließlich im präfrontalen Kortex gespeichert ist; Sensorkortizes berechnen nur Likelihoods",
        answerD = "Bayes'sche Inferenz im Gehirn ist nachgewiesen durch single-unit recordings in V1 die exakt der probabilistischen Populationsvektorcodierung folgen; Weiss et al. (2002) zeigten optimales Bayesianisches Rauschen in Bewegungswahrnehmung",
        correctAnswer = 1, // B
        explanation = "Die Bayesian-Brain-Hypothese (Helmholtz, formalisiert durch Knill & Pouget 2004, Weiss et al. 2002) besagt: Das Gehirn implementiert probabilistische Inferenz, indem es sensorische Likelihoods P(Daten|Ursache) mit internen Priors P(Ursache) nach Bayes' Theorem kombiniert: P(Ursache|Daten) ∝ L·P. Ernst & Banks (Nature 2002) demonstrierten: Menschen kombinieren visuelle und haptische Größenschätzungen exakt gemäß dem statistisch optimalen MLE-Schätzer (Umkehrung der Varianzen gewichtet), wobei σ²_comb = (1/σ²_vis + 1/σ²_hap)⁻¹. Weiss et al. (2002) erklärten Bewegungsillusionen (z.B. Apertur-Problem) durch Bayes'sche Inferenz mit slow-motion Prior P(v).",
        difficulty = 5,
        funFact = "Hermann von Helmholtz beschrieb Wahrnehmung bereits im 19. Jahrhundert als 'unbewussten Schluss' – eine frühzeitige Vorformulierung des Bayesian-Brain-Konzepts."
    ),

    // ── BLOCK 7: Advanced Atmospheric Physics ────────────────────────────────

    // Question 31 – Chapman Mechanism
    Question(
        categoryId = 2,
        questionText = "Welche vier Reaktionen bilden den Chapman-Zyklus der stratosphärischen Ozonchemie, und warum ist dieser Zyklus allein nicht ausreichend zur Erklärung der beobachteten Ozonverteilung?",
        answerA = "O₂+hν→2O, O+O₂+M→O₃+M, O₃+hν→O+O₂, O+O₃→2O₂; der Chapman-Zyklus überschätzt die Ozonkonzentration um Faktor 2, weil katalytische Abbauprozesse durch HOₓ, NOₓ und ClOₓ fehlen",
        answerB = "N₂+hν→2N, N+O₂→NO+O, O+O₃→NO₂+O₂, NO₂+hν→NO+O; der Chapman-Zyklus unterschätzt Ozon, weil er Reaktionen des Sauerstoff-Singuletts ¹D fehlt",
        answerC = "Der Chapman-Zyklus umfasst nur drei Schritte (Photolyse von O₂, O₃-Bildung, O₃-Photolyse); katalytische ClOₓ-Zyklen wurden 1975 von Molina und Rowland als vierter Schritt hinzugefügt",
        answerD = "Der Chapman-Zyklus ist vollständig und beschreibt die troposphärische Ozonchemie; in der Stratosphäre sind Heterogenreaktionen auf PSC-Oberflächen der primäre Unterschied",
        correctAnswer = 0, // A
        explanation = "Der Chapman-Zyklus (Sidney Chapman, 1930) besteht aus: (1) O₂ + hν (λ<242 nm) → 2O; (2) O + O₂ + M → O₃ + M (drei-Körper-Reaktion, M=N₂/O₂); (3) O₃ + hν (λ<320 nm) → O + O₂; (4) O + O₃ → 2O₂ (der langsame Vernichtungsschritt). Dieser Zyklus überschätzt die beobachtete Stratosphären-Ozonmenge um Faktor ~2, weil katalytische Abbauprozesse fehlen. Bates & Nicolet (1950) erkannten HOₓ-Katalyse; Crutzen (1970) NOₓ-Katalyse (NO + O₃ → NO₂ + O₂, NO₂ + O → NO + O₂); Molina & Rowland (1974) ClOₓ-Katalyse durch FCKW. Diese drei Entdeckungen führten zu Nobelpreisen (Crutzen, Molina, Rowland, 1995).",
        difficulty = 5,
        funFact = "Paul Crutzen berechnete 1970, dass Überschallflugzeuge (SST-Projekte Concorde/SST) die Stratosphäre mit NOₓ belasten würden – seine Ozon-Warnungen verhinderten möglicherweise ein globales SST-Programm."
    ),

    // Question 32 – Stratospheric Sudden Warming
    Question(
        categoryId = 2,
        questionText = "Was ist ein stratosphärischer Plötzlicher Erwärmungsereignis (Sudden Stratospheric Warming, SSW), welcher mechanismus erzeugt ihn, und wie beeinflusst er das arktische Oberflächenwetter?",
        answerA = "Ein SSW ist eine spontane Verstärkung des Polarwirbels durch Strahlungserwärmung im Polarnacht; er erzeugt AO/NAM-Absenkung und verstärkt arktische Kältepole über Eurasien und Nordamerika",
        answerB = "SSWs entstehen durch aufwärtspropagierte planetare Rossby-Wellen aus der Troposphäre, die den stratosphärischen Polarwirbel (SPV) durch Wave Breaking schwächen oder umkehren; der geschwächte SPV koppelt nach unten und führt zu negativem AO/NAM-Index, was arktische Kaltluftausbrüche in mittlere Breiten begünstigt (z.B. 'Polar-Vortex-Ereignisse')",
        answerC = "SSWs entstehen durch Meridionaltransport tropischer Stratosphärenluft durch die Brewer-Dobson-Zirkulation; ihre Häufigkeit nimmt mit dem Klimawandel ab wegen verstärkter tropischer Auftriebszone",
        answerD = "Plötzliche stratosphärische Erwärmungen sind thermische Wellen durch Vulkanasche in 20-50 km Höhe; sie zerstören die polare Vortex-Stabilität durch Albedo-Änderungen des stratosphärischen Aerosols",
        correctAnswer = 1, // B
        explanation = "SSWs (Scherhag 1952, Matsuno 1971) entstehen wenn troposphärische planetare Wellen (Rossby-Wellen, Wellenzahlen 1–2) ausreichend Aktivität in die Stratosphäre propagieren: Die EP-Flux-Konvergenz bremst den Zonalwind bis zur Umkehr (w > 0 m/s, Ostwind statt Westwind). Der Stratosphärische Polarwirbel bricht zusammen (Split-SSW oder Displacement-SSW). Baldwin & Dunkerton (2001) zeigten: Der anomale Polarwirbel propagiert nach unten zur Tropopause und präkonditioniert die Troposphäre für ~60 Tage. Negativer AO/NAM führt zu äquatorwärtigem Jet-Stream und ermöglicht Kaltluftausbrüche in Europa und Nordamerika ('Polar Vortex disruption').",
        difficulty = 5,
        funFact = null
    ),

    // Question 33 – Gravity Waves in Atmosphere
    Question(
        categoryId = 2,
        questionText = "Was sind atmosphärische Schwerewellen, welche Dispersionsrelation beschreibt sie, und warum sind sie für die allgemeine Zirkulation der mittleren Atmosphäre entscheidend?",
        answerA = "Atmosphärische Schwerewellen sind Druckwellen deren Rückstellkraft die Gravitationsbeschleunigung g ist; Dispersionsrelation: ω² = gk² (Tiefwasser-Approximation); sie tragen Energie aus dem Boden bis in die Mesosphäre",
        answerB = "Schwerewellen (Buoyancy waves) haben Rückstellkraft Auftrieb (N²k_h²)/(k²), Dispersionsrelation ω² = N²k_h²/(k_h²+m²) mit Brunt-Väisälä-Frequenz N; ihre kritischen Level und Brechung deponieren Zonal-Impuls in der mittleren Atmosphäre und treiben die mesosphärische Zirkulation (Sommer-Kältepol, Winter-Wärmepol) gegen die strahlungsgetriebene Erwartung",
        answerC = "Atmosph. Schwerewellen haben ω = N = √(-g/ρ dρ/dz) und propagieren nur horizontal; sie erzeugen die beobachteten Schichten im Ozonprofil durch welleneinduzierte Meridionalzirkulation",
        answerD = "Schwerewellen in der Atmosphäre sind identisch mit Oberflächenwellen auf Wasser (ω = √(gk)); sie entstehen nur durch Topographie und haben keine Auswirkung auf die Zirkulation oberhalb von 30 km",
        correctAnswer = 1, // B
        explanation = "Atmosphärische Schwerewellen (Gravity Waves, nicht zu verwechseln mit Gravitationswellen) haben die Auftriebskraft (Buoyancy) als Rückstellkraft. Die Dispersionsrelation lautet: ω² = N²·k_h²/(k_h² + m²) + f²·m²/(k_h² + m²), mit Brunt-Väisälä-Frequenz N = √(g/θ · dθ/dz) und Trägheitsfrequenz f. Schwerewellen werden durch Orographie (Leewellen), Konvektion und Jetstreaks generiert. Bei der Propagation nach oben (Amplitudenzunahme ∝ 1/√ρ) brechen sie am kritischen Level und deponieren Zonal-Impuls. Dieser Non-Orographic-GW-Flux ist unverzichtbar für: Antrieb der Quasi-Biennial-Oscillation (QBO), Umkehr der Meridionalzirkulation in der Mesosphäre (Sommerpol kalt ~-130°C trotz Sonneneinstrahlung).",
        difficulty = 5,
        funFact = "Der Sommer-Messosphären-Kältepol (~130°C bei 80 km) ist der kälteste Ort in der Erdatmosphäre – kälter als die Wintermesosphäre – wegen Schwerewellen-getriebener Aufwärtsbewegung und adiabatischer Abkühlung."
    ),

    // Question 34 – Mesospheric Chemistry
    Question(
        categoryId = 2,
        questionText = "Welche ionischen Clusterreaktionen sind für die Bildung von Meteoritic Smoke Particles (MSPs) und Meteoric Metal-Ionen in der Mesosphäre verantwortlich, und warum ist das Na-Schicht-Ablationsschema relevant?",
        answerA = "Meteoriten ablieren Metallionen (Fe⁺, Mg⁺, Na⁺, Ca⁺) zwischen 75–110 km; Na-Atome entstehen durch Photodissoziation von NaHCO₃, dann Na + O₃ → NaO + O₂, NaO + O → Na + O₂ – dieser Chemische-Na-Zyklus erklärt die permanente Na-Schicht; MSPs entstehen durch homogene Nukleation aus SiO₂/MgO-Kondensation",
        answerB = "Meteoritisches Ablationsmaterial sublimiert zu Atomen (z.B. Na, Fe, Mg); Na reagiert mit O₃ und O₂ zu Natriumbisulfat NaHSO₄ als primärem Nucleationspartner; MSPs enthalten vorwiegend Eisenoxid aus Chondrit-Ablation",
        answerC = "Meteoritic Smoke Particles entstehen ausschließlich durch Zerfallsreaktionen hochenergetischer Meteoriten (>10 km/s); Na-Schicht bei 90 km wird durch Photoionisation von Natriumchlorid NaCl aufrechterhalten",
        answerD = "Meteoritische Ionen (Mg⁺, Fe⁺) bilden Cluster mit Wassermolekülen zu Mg⁺(H₂O)_n; Dissoziation durch UV-Photolyse liefert freie Mg-Atome für die Metallschichten; Na ist nicht meteoritischen Ursprungs",
        correctAnswer = 0, // A
        explanation = "Täglich ~50–100 Tonnen Meteoriten-Material ablieren in der Mesosphäre (75–110 km). Meteoritic Metal-Ionen (Na⁺, Mg⁺, Fe⁺, Ca⁺) entstehen durch Impact-Ionisation. Natrium-Chemie: Na + O₃ → NaO + O₂ (Verlust), NaO + O → Na + O₂ (schnelle Regeneration), NaO + O₃ → NaO₂ + O₂ (weitere Reaktionen), NaHCO₃ als Reservoir. Shooting & Plane (2002) etablierten den detaillierten Natrium-Chemiezyklus. MSPs (Meteoric Smoke Particles, <10 nm) entstehen durch homogene Nukleation aus kondensierbaren Metalloxiden (SiO, MgO, FeO) nach Repolymerisation; sie dienen als Kondensationskeime für Noctilucent-Clouds (NLCs) und als Nukleationskeime in der polaren Stratosphäre (PSC-Typ Ib).",
        difficulty = 5,
        funFact = null
    ),

    // Question 35 – Noctilucent Clouds
    Question(
        categoryId = 2,
        questionText = "Wie bilden sich Nachtleuchtende Wolken (NLCs/PMCs), und warum zeigen sie eine polwärtige und klimatisch bedingte Häufigkeitszunahme in den letzten Jahrzehnten?",
        answerA = "NLCs bestehen aus Eiswasserkristallen (~20 nm bis 1 μm) bei ~82 km Höhe und ~-125°C; sie bilden sich in der extrem kalten Sommermesosphäre durch Heterogenkondensation auf MSPs; ihre Häufigkeitszunahme hängt mit erhöhtem CH₄ zusammen (→ H₂O in der Mesosphäre via OH-Oxidation) und CO₂-induzierter Mesosphärenabkühlung",
        answerB = "NLCs sind Stickstoffoxid-Aerosole die durch galaktische Kosmische Strahlung gebildet werden; ihre Zunahme korreliert mit dem Sonnenaktivitätszyklus und stratosphärischen Aerosolen nach Vulkanausbrüchen",
        answerC = "Nachtleuchtende Wolken entstehen durch Mikrometeoritenstaub auf polaren Orbits; ihre Zunahme reflektiert erhöhten Meteoriten-Flux durch den inneren Asteroidengürtel",
        answerD = "NLCs bilden sich bei 120 km in der Thermosphäre durch Ionenrekombination; ihre polare Häufung erklärt sich durch den magnetischen Feldlinien-Transport geladener Teilchen zu den Polen",
        correctAnswer = 0, // A
        explanation = "NLCs (Noctilucent Clouds / Polar Mesospheric Clouds, PMCs) sind Eiswolken in ~82 km Höhe bei <-120°C, entstehend in der kältesten Region der Erdatmosphäre (Sommermesopause). Bildungsmechanismus: Wasserdampf kondensiert auf MSPs (Meteoric Smoke Particles als Kondensationskeime) zu Eispartikeln 10–100 nm. Voraussetzungen: T < 150 K, ausreichend H₂O-Partialdruck (typisch 2–4 ppmv). Die Klimatrend-Hypothese (Thomas et al.): (1) CO₂-Anstieg kühlt die Mesosphäre (CO₂ emittiert im IR) um ~0.5–1 K/Dekade; (2) CH₄-Anstieg liefert zusätzliches H₂O in der Mesosphäre (CH₄ + OH → ... → H₂O). Beide Effekte begünstigen NLC-Bildung und erklären Häufigkeitstrends seit 1885.",
        difficulty = 5,
        funFact = "NLCs wurden erstmals 1885 – zwei Jahre nach dem Krakatau-Ausbruch – beobachtet; ob Krakatau-Aerosole die ersten NLCs triggerten oder nur die Aufmerksamkeit auf den Abendhimmel lenkten, ist umstritten."
    ),

    // ── BLOCK 8: Synthetic Biology Advanced ──────────────────────────────────

    // Question 36 – Minimal Genome Organisms
    Question(
        categoryId = 2,
        questionText = "Was ist JCVI-syn3.0 (Hutchison et al. 2016), wie groß ist sein Genom, und welche überraschende Entdeckung wurde über die Funktion eines großen Teils seiner Gene gemacht?",
        answerA = "JCVI-syn3.0 ist ein synthetischer Organismus mit 531 kb Genom (473 Gene), der minimale Zelle mit bewiesenem Überleben; überraschend: ~149 Gene (~31%) haben keine annotierte Funktion, sind aber für das Leben essentiell – ein fundamentales Wissenslücke in der Biologie",
        answerB = "JCVI-syn3.0 hat 1023 Gene und 1.08 Mb Genom (vollständige Mycoplasma mycoides-Sequenz); alle Gene haben bekannte Funktionen, was das Mycoplasma-Proteom als vollständig versteht",
        answerC = "JCVI-syn3.0 ist identisch mit JCVI-syn1.0 aber mit 25% kleineren Genom; die Hälfte der reduzierten Gene waren RNA-Polymerase-Untereinheiten deren Deletion durch mRNA-Reinjection kompensiert wurde",
        answerD = "JCVI-syn3.0 hat 473 Gene aber nur 149 sind essentiell; die übrigen 324 Gene sind in redundanten Signalwegen und können einzeln ohne Wachstumsphänotyp deletiert werden",
        correctAnswer = 0, // A
        explanation = "Hutchison et al. (Science 2016) bauten ein minimales synthetisches Bakterium JCVI-syn3.0 durch iteratives Deletieren von Genen aus Mycoplasma mycoides: Das resultierende Genom hat 531 kb und 473 Protein-kodierende Gene (kleinstes selbst-replizierendes Lebewesen im Labor). Die verblüffende Entdeckung: 149 von 473 Genen (~31%) haben keine zuordenbare biologische Funktion – obwohl jedes einzelne essentiell für das Leben ist. Diese 'Dunkelmaterie' der Genomfunktion zeigt fundamentale Lücken im Verständnis zellulärer Prozesse. JCVI-syn3A (Pelletier et al. 2021) verbesserte Zellteilung durch Hinzufügen von 19 Genen und zeigte normale Zellteilungsmorphologie.",
        difficulty = 5,
        funFact = "J. Craig Venter proklamierte 2010 die 'Erschaffung synthetischen Lebens' mit JCVI-syn1.0 – tatsächlich war das Genom-Design und die Transplantation in eine enukleierte Empfängerzelle eine technische Meisterleistung."
    ),

    // Question 37 – Directed Evolution Beyond
    Question(
        categoryId = 2,
        questionText = "Was ist PACE (Phage-Assisted Continuous Evolution, Liu-Lab 2011), und wie löst es das fundamentale Problem der Evolution-Selections-Kopplung in traditionellen directed-evolution-Runden?",
        answerA = "PACE koppelt die kontinuierliche Propagation von M13-Phagenpartikeln (die das Zielprotein als pIII-Fusion tragen) an die Aktivität des evolvierten Proteins: Nur bei Funktionserfüllung (z.B. Polymerase-Aktivität) wird pIII produziert, was infektiöse Phagen erzeugt; ein Überströmungssystem mit E.coli-Wirt ermöglicht 100–200 Generationen/24 Stunden ohne manuellen Eingriff",
        answerB = "PACE verwendet eine kontinuierliche Durchfluss-Zellkultivierung (Chemostat) mit autokatalytischer Reportergen-Expression; Selektionsdruck entsteht durch metabolische Belastung – leistungsstarke Varianten wachsen schneller",
        answerC = "PACE steht für Photo-Activated Continuous Evolution: UV-Bestrahlung erzeugt mutagene Basenmodifikationen die mit CRISPR-basierter Selection kombiniert werden; die Phagenvervielfältigung dient nur als Amplifikationsschritt",
        answerD = "PACE nutzt Retroelemente in Säugerzellen: RNA-Zwischenprodukte werden durch Reverse Transkriptase reinkorporiert und selektiert durch FACS (fluorescence-activated cell sorting) auf Proteinaktivität",
        correctAnswer = 0, // A
        explanation = "PACE (Phage-Assisted Continuous Evolution, Esvelt et al., Nature 2011, Liu Lab) löst das Bottleneck traditioneller Directed-Evolution-Runden (Transform → Screen → Select → Amplify, ~1 Woche/Runde) durch kontinuierliche Kopplung von Mutation und Selektion. Setup: Lagoon (kontinuierlich befülltes Reaktionsgefäß mit E.coli als Wirtszellen) plus M13-Phagen die das Zielgen tragen. Selektion: Phagengen pIII (notwendig für Infektivität) ist auf einem Accessory Plasmid codiert, das nur transkribiert wird, wenn das evolierte Protein die gewünschte Aktivität zeigt. Mutagenese durch Mutator-Plasmid (MP6). Ergebnis: Bis zu 100 Phagengenerationen/24 h ohne manuellen Eingriff → Millionen von Generationen in Wochen (statt Jahren bei Runden-Ansatz).",
        difficulty = 5,
        funFact = "Mit PACE wurden innerhalb weniger Wochen T7-RNA-Polymerasa-Varianten entwickelt, die T3-Promotoren erkennen – eine Spezifitätsänderung, die mit traditionellen Methoden Monate gedauert hätte."
    ),

    // Question 38 – Xenobiology (XNA)
    Question(
        categoryId = 2,
        questionText = "Was sind XNAs (Xenonucleic Acids), welche strukturellen Modifikationen machen sie genetisch orthogonal zur DNA/RNA, und was zeigte die Studie von Pinheiro et al. (2012, Science)?",
        answerA = "XNAs haben unnatürliche Nukleobasen (z.B. Isoguanin/Isocytosin); Pinheiro et al. zeigten, dass XNAs alle 4 Informationsspeicher-Eigenschaften der DNA besitzen: Speicherung, Replikation, Evolution und Katalyse",
        answerB = "XNAs haben modifizierte Zuckerphosphat-Rückgrate (z.B. HNA: Hexitol-NA, CeNA, LNA, ANA, FANA, TNA) statt Desoxyribose/Ribose; Pinheiro et al. (2012) zeigten durch polymerase engineering, dass 6 verschiedene XNAs Informationsspeicherung und -übertragung (Informationstransfer DNA→XNA→DNA) sowie Aptamer-Evolution durch HNA demonstrieren, was die Nichtexklusivität der natürlichen Nucleinsäuren als Informationsmedien belegt",
        answerC = "XNAs sind vollständig synthetische Polymere ohne Phosphatgruppen; Pinheiro et al. zeigten In-vitro-Transkription von XNA-Genen in E.coli durch orthogonale Ribosomen",
        answerD = "XNAs verwenden L-Enantiomere der natürlichen D-Ribose/D-Desoxyribose; Spiegelmeer-DNA (L-DNA) wurde als erstes XNA von Pinheiro et al. in lebenden Zellen repliziert",
        correctAnswer = 1, // B
        explanation = "XNAs (Xenonucleic Acids) sind synthetische Nucleinsäure-Analoga mit modifiziertem Zuckerphosphat-Rückgrat, das nicht in der Natur vorkommt: HNA (1,5-anhydrohexitol NA), CeNA (cyclohexenyl NA), LNA (locked NA), ANA (arabinonucleic acid), FANA (2'-fluoroarabinonucleic acid), TNA (threose NA). Pinheiro et al. (Science 2012, MRC LMB) nutzen engineered Polymerasen, um: (1) DNA→XNA-Transkription, (2) XNA→DNA-Rücktranskription zu ermöglichen. Sie zeigten Aptamer-Selektion aus HNA-Bibliotheken, was demonstriert, dass XNAs evolvierbar sind. Dies beweist, dass die Natur der DNA/RNA-Chemie nicht einzigartig notwendig für genetische Informationssysteme ist – Grundlage für Xenobiologie als synthetisch-biologische Disziplin.",
        difficulty = 5,
        funFact = "XNAs sind von natürlichen Enzymen und Nukleasen nicht erkannt – XNA-basierte Therapeutika wären theoretisch unzerstörbar durch zelluläre Nukleasen und immun gegen biologische Kontamination."
    ),

    // Question 39 – Genetic Circuit Oscillators
    Question(
        categoryId = 2,
        questionText = "Was sind die mathematischen Bedingungen für stabile Schwingungen im Repressilator-Modell (Elowitz & Leibler 2000), und welche biochemischen Nichtlinearitäten sind entscheidend?",
        answerA = "Der Repressilator benötigt mindestens 3 Komponenten im negativen Rückkopplungsring; Hill-Koeffizienten n ≥ 2 in der Repressionskinetik f(x) = α/(1+(x/K)ⁿ) sind notwendig für Instabilität des Fixpunkts und Entstehung von Grenzzyklen (Hopf-Bifurkation); Proteinstabilitäts-Unterschiede und Verzögerungen durch mRNA-Translation beeinflussen Periode und Amplitude",
        answerB = "Repressilator-Schwingungen erfordern positive Rückkopplung und negative Rückkopplung gleichzeitig; ein MichaelisMenten-Kinetik-Term n=1 ist ausreichend wenn Degradationsraten γ > 2·Produktionsraten β",
        answerC = "Stabile Repressilator-Schwingungen entstehen nur wenn alle drei Repressoren identische biochemische Parameter (γ_mRNA = γ_Protein, gleiche K_d) haben; Asymmetrien führen zu Quenching und Fixpunkt-Konvergenz",
        answerD = "Der Repressilator folgt einer Lotka-Volterra-Dynamik mit Koexistenz-Gleichgewicht; Schwingungen entstehen ausschließlich durch stochastische Genexpressions-Noise in kleinen Zellpopulationen (<100 Moleküle)",
        correctAnswer = 0, // A
        explanation = "Der Repressilator (Elowitz & Leibler, Nature 2000) ist ein synthetisch-biologischer Oszillator aus drei gegenseitig reprimierenden Transkriptionsfaktoren (lacI, tetR, cI im Rückkopplungsring). Mathematische Analyse (Goodwin-Oszillator): Das System dx_i/dt = -γ_x x_i + f(z_i), dz_i/dt = -γ_z z_i + βx_i mit f(x) = α/(1+(x/K)^n). Bedingungen für Schwingungen: (1) Mindestens 3 Komponenten im negativen Rückkopplungsring (2 sind nicht ausreichend); (2) Kooperativität n > nkrit (typisch n > 2 für 3-Knoten-Ring); (3) Hopf-Bifurkation wenn der imaginäre Teil der Eigenwerte der Jacobi-Matrix die Realteile bei Parametervariationen kreuzt. Verzögerungen durch mRNA→Protein-Translationsschritte (τ) verlängern die Periode und senken den kritischen n-Wert.",
        difficulty = 5,
        funFact = "Der Repressilator 2.0 (Potvin-Trottier et al. 2016) mit optimierten Degradationstags zeigte hochreguläre Schwingungen in nahezu jeder E.coli-Zelle – das erste präzise synthetic oscillator-Werkzeug für Biologie."
    ),

    // Question 40 – Cell-Free Systems
    Question(
        categoryId = 2,
        questionText = "Was sind PURE-System (Protein synthesis Using Recombinant Elements) cell-free Transkriptions-/Translationssysteme, und welche konzeptionellen Vorteile haben sie gegenüber Zellextrakt-basierten Systemen?",
        answerA = "PURE-System (Shimizu et al. 2001) ist ein vollständig definiertes In-vitro-Translationssystem aus gereinigten E.coli-Komponenten: 107 Faktoren rekombinant exprimiert (alle Ribosomen-Proteine, 20 Aminoacyl-tRNA-Synthetasen, Elongations-/Initiationsfaktoren, Energieregeneration); im Gegensatz zu Rohextrakten hat PURE keine undefinierten Nukleasen/Proteasen, erlaubt systematische Kompositionsmanipulation und Co-translationale Analytik",
        answerB = "PURE-Systeme sind auf Weizenkeimextrakt (wheat germ) basierend mit purifizierter T7-Polymerase; sie sind effizienter als E.coli-Extrakte wegen fehlender Endotoxine und werden für therapeutische Proteine bevorzugt",
        answerC = "PURE bezeichnet zellfreie Systeme die in giant unilamellar vesicles (GUVs) eingeschlossen sind; der Vorteil ist räumliche Begrenzung die Diffusion-Limitierung reproduziert und Enkapsulations-Effizienz von >95% erlaubt",
        answerD = "PURE-Systeme kombinieren prokaryontische (E.coli) und eukaryontische (Hefe) Translationskomponenten für Proteine mit gemischten Faltungsanforderungen; sie erfordern keine ATP-Regeneration wegen Phosphokreatin-Energiepuffer",
        correctAnswer = 0, // A
        explanation = "Das PURE-System (Protein synthesis Using Recombinant Elements, Shimizu et al. Nature Biotechnology 2001) ist eine vollständig rekombinante Zellfreie Proteinsynthese-Plattform: Alle ~107 notwendigen Faktoren wurden individuell gereinigt und in optimalen Verhältnissen zusammengemischt. Komponenten: Alle 3 Ribosomen-Untereinheiten, 20 aaRS, 10 Elongationsfaktoren (EF-Tu, EF-Ts, EF-G), Initiations-Faktoren (IF1, IF2, IF3), Terminations-Faktoren, T7-RNA-Polymerase, ATP/GTP-Regeneration (Pyruvat-Kinase/Kreatin-Kinase). Vorteile gegenüber Zellextrakten: definierte Zusammensetzung (no black box), keine unerwünschten Nebenaktivitäten (Nukleasen, Proteasen), Möglichkeit zur systematischen Komponentenmanipulation, Einsatz für Biosynthese nicht-natürlicher Aminosäuren.",
        difficulty = 5,
        funFact = null
    ),

    // ── BLOCK 9: Condensed Matter – Exotic ───────────────────────────────────

    // Question 41 – Fractional Quantum Hall Effect
    Question(
        categoryId = 2,
        questionText = "Was ist der Laughlin-Wellenfunktions-Ansatz für den fraktionalen Quanten-Hall-Zustand bei ν=1/3, und welche Eigenschaften haben die Quasiteilchen-Anregungen?",
        answerA = "Die Laughlin-Wellenfunktion ψ_m = ∏_{i<j}(z_i-z_j)^m · exp(-Σ|z_i|²/4l_B²) mit ungeradzahligem m=3 für ν=1/3 beschreibt einen inkommensurablen Zustand mit Quasiteilchen-Ladung e* = e/3 und fraktionaler Statistik (Anyonen mit θ = π/3)",
        answerB = "Die Laughlin-Wellenfunktion ist ein Slater-Determinanten-Ansatz mit m=3 Landau-Niveaus besetzt; Quasiteilchen haben ganzzahlige Ladung e und Boson-Statistik da sie Cooper-Paaren ähneln",
        answerC = "Die Laughlin-Wellenfunktion gilt für ν=1/2 mit m=2; Quasiteilchen bei ν=1/3 werden durch Composite-Fermion-Wellenfunktionen von Jain beschrieben und haben Ladung e* = e/3 aber fermionische Statistik",
        answerD = "Laughlins Ansatz beschreibt nur den Integer-QHE (ν=1); für ν=1/3 ist die Composite-Fermion-Theorie von Jain notwendig die effektive Magnetfelder B* = B - 2nΦ₀ einführt",
        correctAnswer = 0, // A
        explanation = "Robert Laughlin (Nobel 1998) schlug 1983 für ν=p/q (mit ungeradem q) die Wellenfunktion ψ_m = ∏_{i<j}(z_i-z_j)^m · exp(-Σ|z_k|²/4l_B²) vor, mit z_k = x_k - iy_k komplexer Koordinate und m = 1/ν = 3 für ν=1/3. Sie beschreibt stark korrelierte Elektronen im niedrigsten Landau-Niveau (LLL). Quasiteilchen-Anregungen: (1) Ladung e* = e/3 (fraktional!), nachgewiesen durch Rauschen-Experimente (de-Picciotto 1997, Reznikov 1997); (2) Anyon-Statistik: Beim Austausch zweier Quasiteilchen sammelt die Wellenfunktion Phase e^{iθ} mit θ = π/m = π/3 (weder Boson θ=0 noch Fermion θ=π, sondern Anyon).",
        difficulty = 5,
        funFact = "Der fraktionale QHE wurde 1982 von Tsui, Störmer und Gossard zufällig bei Suche nach Wigner-Kristall entdeckt – alle drei erhielten 1998 den Nobelpreis zusammen mit Laughlin."
    ),

    // Question 42 – Weyl Semimetals
    Question(
        categoryId = 2,
        questionText = "Was sind Weyl-Fermionen in einem Festkörper, welche topologische Invariante charakterisiert Weyl-Punkte, und welche einzigartige Oberflächenzustand-Signatur wurde in TaAs gemessen?",
        answerA = "Weyl-Fermionen sind masselose chirale Fermionen in Materialien wo Zeit-Inversions- oder Inversions-Symmetrie gebrochen ist; Weyl-Punkte sind geschützte Kreuzungspunkte zweier nicht-entarteter Bänder mit Chern-Zahl C = ±1; in TaAs wurden offene Fermi-Arcs auf der Oberfläche gemessen, die zwei Weyl-Punkte entgegengesetzter Chiralität verbinden",
        answerB = "Weyl-Fermionen entstehen in Supraleitern wo Cooper-Paare chirale Symmetrie brechen; Weyl-Punkte haben Chern-Zahl C = 0 und Null-Energie-Zustände an Oberflächen; in TaAs sind negative Magnetowiderstand-Messungen die Signatur",
        answerC = "Weyl-Semimetalle sind Übergangsmetall-Dikhalchogenide mit starker Spin-Bahn-Kopplung; die topologische Invariante ist das Z₂-Invariant (Kane-Mele); Oberflächen-Fermi-Arcs verbinden Time-Reversal-invariante Momente",
        answerD = "Weyl-Punkte entstehen nur in nicht-zentrosymmetrischen Materialien mit Spin-Bahn-Kopplung > 0.5 eV; die topologische Invariante ist die Spin-Hall-Leitfähigkeit σ_xy^s = e/4π; TaAs zeigt Fermi-Arcs die durch einen einzelnen Weyl-Punkt propagieren",
        correctAnswer = 0, // A
        explanation = "Weyl-Semimetalle haben Weyl-Punkte: isolierte Kreuzungspunkte zweier nicht-entarteter Bänder im 3D Impulsraum, wo ein effektives Weyl-Hamiltonian H_W = ±v_F σ·k gilt (chirale Fermionen). Topologische Invariante: Chern-Zahl C = ±1 (Monopol/Antimonopol der Berry-Krümmung im k-Raum). Weyl-Punkte treten in Paaren (Nielsen-Ninomiya, 1981) mit entgegengesetzter Chiralität auf. Vorhergesagt in TaAs (Huang et al. 2015, Weng et al. 2015), bestätigt durch ARPES (Xu et al. Science 2015, Lv et al. PRL 2015): Oberflächenzustände bilden offene Fermi-Arcs – ein Merkmale von Weyl-Semimetallen im Gegensatz zu geschlossenen Fermi-Flächen normaler Metalle. Zusätzliche Signatur: Negativer Längsmagnetwiderstand durch chirale Anomalie (Adler-Bell-Jackiw-Anomalie im Festkörper).",
        difficulty = 5,
        funFact = null
    ),

    // Question 43 – Skyrmions in Magnetics
    Question(
        categoryId = 2,
        questionText = "Was ist ein magnetischer Skyrmion, welche topologische Zahl charakterisiert ihn, und welche Wechselwirkung stabilisiert ihn in dünnen magnetischen Schichten?",
        answerA = "Ein magnetischer Skyrmion ist eine wirbel-artige topologisch-geschützte Spinstruktur; die topologische Ladung Q = (1/4π)∫m·(∂_x m × ∂_y m) d²r = ±1 (ganzzahlig); Dzyaloshinskii-Moriya-Wechselwirkung (DMI) aus Spin-Bahn-Kopplung in Systemen ohne Inversionssymmetrie konkurriert mit Austausch und Zeeman-Energie und stabilisiert Skyrmionen",
        answerB = "Magnetische Skyrmionen sind Bloch-Domänenwände mit besonderer Geometrie; topologische Zahl Q = 2 für Bloch-Typ und Q = 1 für Néel-Typ; RKKY-Wechselwirkung zwischen Schichten stabilisiert sie in Multilagen-Systemen",
        answerC = "Skyrmionen haben Q = 0 und sind topologisch trivial; sie werden durch dipolare Magnetwechselwirkungen (magnetostatic energy) stabilisiert; die charakteristische Längenskala ist die magnetische Austauschlänge l_ex = √(A/K)",
        answerD = "Magnetische Skyrmionen sind ausschließlich in ferrimagnetischen Oxiden stabil (z.B. YIG/Pt-Grenzflächen); ihre topologische Zahl ist die Skyrmion-Zahl N_sk = winding number der Spin-Textur und beträgt typisch N_sk = 3–5",
        correctAnswer = 0, // A
        explanation = "Magnetische Skyrmionen (nach Tony Skyrme, Kernphysik) sind topologisch-geschützte Spin-Wirbel in 2D Magnetsystemen. Die topologische Ladung Q = (1/4π)∫m·(∂_x m × ∂_y m) d²r nimmt ganzzahlige Werte (Q = ±1) an, da die Magnetisierung m̂ eine kontinuierliche Abbildung S²→S² ist – Skyrmionen können nicht kontinuierlich in den uniformen Zustand deformiert werden (topologischer Schutz). Die Dzyaloshinskii-Moriya-Interaktion (DMI): H_DM = D·(S_i × S_j), entstehend aus Spin-Bahn-Kopplung in Systemen ohne Inversionssymmetrie (z.B. Mn/Si-Grenzfläche, MnSi-Kristall), favorisiert chirale Spin-Texturen. Erste Beobachtung in MnSi (Mühlbauer et al. Science 2009). Skyrmionen können elektrisch durch Spin-Transfer-Torque bewegt werden – Kandidaten für ultrakleine Datenspeicher.",
        difficulty = 5,
        funFact = "Ein einzelner Skyrmion in einer 100 nm dicken Schicht kann auf nur ~5 nm Durchmesser schrumpfen – ein Bit Daten in einem Bereich kleiner als ein Virus."
    ),

    // Question 44 – Josephson Junction Physics
    Question(
        categoryId = 2,
        questionText = "Was sind die zwei Josephson-Gleichungen (stationärer und nicht-stationärer Josephson-Effekt), und wie wird die makroskopische Quantenphase im Josephson-Kontakt verankert?",
        answerA = "Stationärer Josephson-Effekt: I = I_c·sin(φ), wobei φ = φ₁-φ₂ die Phasendifferenz der Wellenfunktionen beider Supraleiter ist; nicht-stationärer Josephson-Effekt: dφ/dt = 2eV/ℏ (Spannungsfrequenz-Relation), was zur Josephson-Frequenz f_J = (2e/h)·V = 483.6 MHz/μV führt; die makroskopische Phase ist durch die BCS-Ordnungsparameter Ψ_i = √n_s e^{iφ_i} definiert",
        answerB = "Stationärer Josephson-Effekt: I = I_c·cos(φ), nicht-stationär: dφ/dt = eV/ℏ (Einzelelektron-Strom); die Josephson-Frequenz ist f_J = (e/h)·V = 241.8 MHz/μV",
        answerC = "Die Josephson-Gleichungen gelten nur für SIS-Kontakte (Supraleiter-Isolator-Supraleiter) unter 1 K; bei SNS-Kontakten (mit Normal-Metall) gibt es nur resistive AC-Komponente ohne Phasenkoheränz",
        answerD = "Josephson-Effekte erfordern Magnetfelder in der Kontaktzone; ohne Feld gibt es nur Cooper-Paar-Tunneln ohne definierte Phasendifferenz; f_J = 2·f_Plasma der LC-Resonator-Frequenz des Kontakts",
        correctAnswer = 0, // A
        explanation = "Brian Josephson (Nobel 1973, mit damals 22 Jahren als PhD-Student) sagte 1962 voraus: (1) Stationärer Josephson-Effekt: Gleichstrom I = I_c sin(φ) fließt ohne angelegte Spannung durch den Tunnelkontakt, wobei φ = φ_1 - φ_2 die makroskopische Phasendifferenz der BCS-Ordnungsparameter Ψ_i = Δe^{iφ_i} ist. (2) Nicht-stationärer (AC-) Josephson-Effekt: Bei Spannung V entwickelt sich φ mit dφ/dt = 2eV/ℏ → Wechselstrom mit Josephson-Frequenz f_J = 2eV/h = (2e/h)V ≈ 483.6 GHz/mV (Volt-Frequenz-Quotient, definiert Spannungsstandard!). Die Konstante K_J = 2e/h = 483597.9 GHz/V ist heute eine exakte SI-Definitionskonstante.",
        difficulty = 5,
        funFact = "Der Josephson-Effekt bildet die Basis des weltweiten Volt-Standards: Alle nationalen Metrologieinstitute definieren die Spannung über f_J = K_J · V mit Josephson-Josephson-Arrays."
    ),

    // Question 45 – Heavy Fermion Systems
    Question(
        categoryId = 2,
        questionText = "Was sind Heavy-Fermion-Materialien, welcher Mechanismus erzeugt die massiv erhöhte effektive Elektronenmasse, und was macht die Quantenphasenübergänge in CeCu₆₋ₓAuₓ wissenschaftlich außergewöhnlich?",
        answerA = "Heavy-Fermion-Materialien (z.B. CeAl₃, UBe₁₃, CeCu₆) haben effektive Elektronenmassen m* ~ 100–1000 m_e; der Kondo-Effekt beschreibt die Hybridisierung lokaler f-Elektronen (Ce, U) mit konduction band-Elektronen und bildet ein schweres Quasi-Teilchen-Band nahe der Fermi-Energie; CeCu₆₋ₓAuₓ zeigt einen quantenkritischen Punkt (QCP) bei x=0.1 mit Nicht-Fermi-Flüssigkeits-Verhalten (ΔC/T = γ + A·ln(T), ρ ∝ T, A₁/²∝|x-x_c|⁻¹) das durch 2D-Spinfluktuationen (Freiheitsgrade des QCP) erklärt wird",
        answerB = "Heavy Fermions entstehen durch polaron-artiges Trapping von Elektronen durch Gitterverzerrungen in Seltenerden-Oxiden; m* ist erhöht wegen phonon-renormierter Dispersion; CeCu₆Au zeigt einen metal-to-insulator transition bei x_c = 0.1 durch Anderson-Lokalisierung",
        answerC = "Die effektive Masse in Heavy-Fermion-Systemen ist durch Hund'sche Kopplung J erhöht: m* = m_e(1 + J²/W²) mit Bandbreite W; CeCu₆₋ₓAuₓ hat einen ferromagnetischen QCP mit Hertz-Millis-Theorie-Vorhersagen für ρ ∝ T^{5/3}",
        answerD = "Heavy Fermions haben m* ~ 1000 m_e durch Wigner-Seitz-Lokalisierung in enger d-Band-Systemen; der QCP in CeCu₆Au wird durch Druck, nicht durch Au-Dotierung, erreicht und zeigt Supraleitungs-Domes",
        correctAnswer = 0, // A
        explanation = "Heavy-Fermion-Materialien (Seltenerden- und Actinid-Verbindungen: Ce, Yb, U) haben m* = 100–1000 m_e (gemessen durch spezifische Wärmekoeffizienten γ = π²k_B²m*/(3ℏ²) bis ~1600 mJ/mol K²). Mechanismus: Kondo-Gitter-Effekt – periodisch angeordnete Ce/U-Ionen mit lokalisierten f-Elektronen koppeln antiferromagnetisch an Leitungsbandelektronen (Kondo-Kopplung J_K); unterhalb der Kondo-Temperatur T_K hybridisieren lokale f-Zustände mit dem Konduktionsband und bilden ein schweres Band (Wilson 1975, Doniach 1977). CeCu₆₋ₓAuₓ (Schröder et al. Nature 2000): Au-Substitution komprimiert das Gitter, reduziert J_K und bringt bei x_c ≈ 0.1 einen antiferromagnetischen QCP. Die kritischen Fluktuationen zeigen anomales Non-Fermi-Liquid-Verhalten, das die Hertz-Millis-Theorie bricht – mögliche Lösung: lokaler QCP mit Zusammenbruch der Kondo-Abschirmung.",
        difficulty = 5,
        funFact = null
    ),

    // ── BLOCK 10: Astroparticle Physics ──────────────────────────────────────

    // Question 46 – Neutrino Oscillation Details
    Question(
        categoryId = 2,
        questionText = "Was ist die MSW-Resonanz (Mikheyev-Smirnov-Wolfenstein-Effekt) in der Sonne, und warum führt sie zu einem resonanten Umwandlung von ν_e in ν_μ/ν_τ auch für kleine Vakuum-Mischungswinkel?",
        answerA = "Die MSW-Resonanz entsteht wenn die effektive Neutrino-Masse in Materie (durch kohärente Vorwärtsstreuung an Elektronen, V_e = √2 G_F n_e) die Vakuumsmassenquadratdifferenz Δm²cos(2θ)/2E kompensiert; am Resonanzpunkt verhält sich das System wie ein 2-Niveau-System im Resonanz-Zustand und der adiabatische Übergang wandelt ν_e komplett in ν_2 um",
        answerB = "MSW-Resonanz tritt nur für antineutrinos (ν̄_e) auf wegen der antiparallelen Spinprojektion; sie erklärt die Reaktor-Antineutrino-Anomalie bei ~5 MeV durch Materie-Unterdrückung",
        answerC = "Der MSW-Effekt beschreibt kohärente Neutrino-Selbstenergie in dichten Neutronensternatmosphären; er erhöht den Mischungswinkel auf θ_eff = 45° unabhängig vom Vakuum-θ für E_ν > 10 MeV",
        answerD = "MSW-Resonanz in der Sonne tritt bei Elektronendichte n_e ~ 10³² cm⁻³ im Sonnenkern auf; der Effekt ist unabhängig von der Neutrinoenergie und erklärt das solare Neutrinoproblem für alle Energien gleichermaßen",
        correctAnswer = 0, // A
        explanation = "Der MSW-Effekt (Wolfenstein 1978, Mikheyev & Smirnov 1985) beschreibt Materie-induzierte Modifikation der Neutrino-Oszillation. In Materie mit Elektronendichte n_e haben ν_e eine effektive Potenzial V_CC = √2 G_F n_e (charged current coherent forward scattering) relativ zu ν_μ/ν_τ. Die effektive 2×2-Hamiltonmatrix hat einen Resonanzpunkt wenn V_CC = Δm²cos(2θ)/2E – hier ist der effektive Mischungswinkel θ_m = 45° (maximale Mischung), unabhängig vom Vakuumwinkel θ. Für adiabatische Propagation (Bedingung: γ = Δm²sin²(2θ)/2E·|dn_e/dx|⁻¹ >> 1) wird ν_e bei Produktion im Sonnenkern (hohe n_e) adiabatisch in ν₂ transformiert und verlässt die Sonne als Masse-Eigenstate ν₂ mit P(ν_e→ν_e) ≈ sin²θ << 1. Dies erklärt das solare Neutrinoproblem für E_ν > 2 MeV.",
        difficulty = 5,
        funFact = "Die MSW-Lösung des solaren Neutrinoproblems wurde durch SNO (Sudbury Neutrino Observatory) 2002 bestätigt, das erstmals sowohl NC- als auch CC-Neutrinoflusses maß – Art McDonald erhielt dafür 2015 den Nobelpreis."
    ),

    // Question 47 – IceCube Detector Physics
    Question(
        categoryId = 2,
        questionText = "Welches Detektionsprinzip nutzt IceCube am Südpol, und wie wurden 2022 die ersten hochenergetischen Neutrinos aus NGC 1068 (Seyfert-Galaxie) identifiziert?",
        answerA = "IceCube detektiert Cherenkov-Licht von Myonen, Elektron-Schauern und Hadronen-Kaskaden die durch Neutrino-Wechselwirkungen (CC: ν+N→l+X, NC: ν+N→ν+X) im antarktischen Eis entstehen; NGC 1068-Neutrinos wurden durch Track-Events (Myon-Neutrino CC) identifiziert die auf NGC 1068 zurückweisen, bestätigt durch 4.2σ-Überschuss in Richtungsanalyse",
        answerB = "IceCube nutzt akustische Signale ultraenergetischer Neutrino-Interaktionen; NGC 1068-Identifikation basierte auf Radio-Cherenkov (Askaryan-Effekt) in der ARA-Erweiterung",
        answerC = "IceCube detektiert Neutrinos durch Neutron-Rückstoß-Signale wie Borexino; NGC 1068-Nachweis gelang durch Koinzidenz-Messung mit dem Fermi-LAT Gamma-Ray-Teleskop in 4 MeV-Fenster",
        answerD = "IceCube verwendet 5160 digitale optische Module (DOMs) die Szintillationslicht in Bleiglas-Kristallen messen; die NGC 1068-Analyse nutzte PeV-Cascade-Events mit shower-like Topologie",
        correctAnswer = 0, // A
        explanation = "IceCube ist ein 1 km³ großer Neutrino-Detektor im antarktischen Eis (1.45–2.45 km Tiefe), bestehend aus 5160 digitalen optischen Modulen (DOMs) auf 86 Strings. Detektionsprinzip: Hochenergetische Neutrinos (GeV–PeV) wechselwirken durch CC und NC mit Eis-Nukleonen. CC-Myon-Neutrinos erzeugen Myonen mit charakteristischen elongierten Tracks, die Cherenkov-Licht (blau, ~400 nm) emittieren, winkelauflösend bis ~0.3°. Hochenergetische ν_e/ν_τ bilden Hadronen+EM-Kaskaden (Cascade-Topologie). IceCube Collaboration (Science 2022): NGC 1068, eine Seyfert-II-Galaxie in 47 Mpc Entfernung, zeigt einen 4.2σ-Überschuss an Myon-Neutrino-Track-Events, was auf eine aktive Neutrino-Quelle im AGN-Kern hindeutet – erste identifizierte extragalaktische diffuse Neutrino-Quelle.",
        difficulty = 5,
        funFact = "IceCubes größte Herausforderung: Das gesamte 86-String-Array wurde in 2 antarktischen Saisons durch 'Hot Water Drilling' in das Eis eingebaut – bei -50°C außen und mit perfekt transparentem Eis in 2 km Tiefe."
    ),

    // Question 48 – Cosmic Ray Spectrum Knee
    Question(
        categoryId = 2,
        questionText = "Was bezeichnet das 'Knie' im Energiespektrum der kosmischen Strahlung bei ~3×10¹⁵ eV (3 PeV), und welche physikalischen Erklärungen werden für diesen spektralen Knick diskutiert?",
        answerA = "Das Knie beschreibt einen Übergang von Photonen zu Hadronen in der kosmischen Strahlung; es liegt bei 3 PeV weil die Photoproduktions-Schwelle für Pion-Erzeugung (GZK-analog) erreicht wird",
        answerB = "Das Knie ist ein spektraler Knick wo der Spektralindex von γ ≈ -2.7 auf γ ≈ -3.1 steiler wird; Erklärungen: (1) obere Energiegrenze galaktischer Beschleuniger (Supernova-Schockwellen, R_max = ZeBL für Rigidität); (2) Propagationseffekte (diffusive Escape-Zeit aus der Galaxis energieabhängig); (3) Übergang zwischen verschiedenen Quellpopulationen",
        answerC = "Das Knie bei 3 PeV markiert den Übergang von galaktischer zu extragalaktischer kosmischer Strahlung (analog zum Übergang bei ~EeV); es entspricht der maximalen Energie von Pulsarwind-Nebeln als Beschleunigern",
        answerD = "Das Knie entsteht durch Absorption von kosmischer Strahlung an der CMB-Hintergrundstrahlung (kosmischer GZK-Effekt) für Protonen; der analoge GZK-Cutoff für Eisenkerne liegt bei 3 PeV statt 50 EeV",
        correctAnswer = 1, // B
        explanation = "Das Knie ('knee') im Energiespektrum kosmischer Strahlung (CR) bei ~3 PeV ist eine Änderung des Spektralindex von γ ≈ -2.7 zu γ ≈ -3.1 (Steiler-Werden). Wichtigste Erklärungsmodelle: (1) Maximale Beschleunigerenergie: Supernova-Schockwellen (SNR) können Teilchen bis E_max = Z·e·B·L (Hillas-Kriterium) beschleunigen; für Protonen E_max ~ 3·10¹⁵ eV typisch. Das Knie ist dann galaktisch mit Z-abhängiger Struktur (Proton-Knie bei 3 PeV, He-Knie bei 6 PeV, Fe-Knie bei ~80 PeV). (2) Diffusiver Escape: Die Diffusionskoeffizienz D(E) ∝ E^δ (δ ~ 0.3-0.7) führt zu energieabhängiger Escape-Zeit T_esc ∝ E^{-δ}; höherenergetische CR entkommen leichter aus der Galaxis → effektives Quellspektrum wird steiler. TIBET AS-γ und KASCADE-Grande messungen unterstützen die Rigidität-abhängige Knie-Interpretation.",
        difficulty = 5,
        funFact = null
    ),

    // Question 49 – Gamma-Ray Bursts Models
    Question(
        categoryId = 2,
        questionText = "Was unterscheidet das 'Internal Shock Model' vom 'ICMART Model' für die Prompt-Emission von Gamma-Ray Bursts (GRBs), und welche Beobachtungseigenschaft motivierte das ICMART-Modell?",
        answerA = "Internal Shock Model: Unstetige Jet-Ausflüsse (shells) mit unterschiedlichen Lorentz-Faktoren kollisionieren intern bei R_IS ~ 10¹⁴ cm; ICMART (Internal Collision-Induced Magnetic Reconnection and Turbulence): Magnetisch dominierter Jet (σ >> 1) wo Magnetic Reconnection statt Schockdissipation Energie freisetzt; motiviert durch beobachtete hohe lineare Polarisation (Π ~ 40–80%) in Prompt-Emissionen die durch Synchrotron in Schocken nicht erklärbar ist",
        answerB = "Internal Shock erzeugt nicht-thermische Spektren, ICMART erzeugt Quasi-Blackbody-Spektren; das ICMART-Modell wurde motiviert durch Quasi-Thermal-Komponente im Fermi/GBM-Spektrum bei E_peak ~ 300 keV",
        answerC = "Beide Modelle sind äquivalent in der Energiedissipation; der Unterschied liegt nur in der Emissionsgeometrie: Internal Shocks = isotropisch, ICMART = kollimiert mit strukturiertem Jet (spine-sheath-Modell)",
        answerD = "Internal Shock Model gilt für Long-GRBs (z > 1 s), ICMART für Short-GRBs (< 2 s) wegen der unterschiedlichen Magnetfeld-Topologie in Neutronensternen vs. kollabierenden Sternen",
        correctAnswer = 0, // A
        explanation = "Das Internal Shock Model (Rees & Mèszàros 1994): Ein unregelmäßiger GRB-Zentralmotor (Kollapsarr/Neutronenstern-Merger) emittiert Jet-Shells mit variierenden Lorentz-Faktoren Γ ~ 100–1000; schnellere Shells holen langsamere ein bei R ~ 10¹³–10¹⁵ cm und erzeugen relativistische Schocks, die Elektronen auf nicht-thermische Synchrotron-Emission beschleunigen. Problem: niedrige Effizienz (ε < 10%), Spektren zu weich. Das ICMART-Modell (Zhang & Yan 2011): Jet ist primär magnetisch-dominiert (σ ~ 10–100); interne Kollisionen triggern Magnetic Reconnection und Turbulenz statt Schocks, was effizientere Energieabgabe erlaubt. Motivation: ISGRI/BATSE-Polarisationsmessungen zeigter Π ~ 40–80% Grad linearer Polarisation – Synchrotron in geordnetem (post-Rekonnexions) B-Feld erklärt hohe Polarisation, Schockturbulenz nicht.",
        difficulty = 5,
        funFact = "GRB 221009A ('BOAT' – Brightest Of All Time, Oktober 2022) war der hellste je detektierte Gamma-Ray-Burst – so hell, dass er Erdionosphären-Instrumente übersättigte und terrestrische Detektoren in der Nacht auf Tagmodus schaltete."
    ),

    // Question 50 – Direct Dark Matter Detection
    Question(
        categoryId = 2,
        questionText = "Was ist das 'Neutrino Floor' (oder Neutrino Fog) in Direktnachweisexperimenten für Dunkle Materie, und welches experimentelle Konzept ermöglicht es, über dieses Limit hinaus zu operieren?",
        answerA = "Der Neutrino Floor ist die irreducible Background-Rate durch kohärente elastische Neutrino-Kernstreuung (CEνNS) von solaren, atmosphärischen und diffusen supernova-background Neutrinos, die WIMP-ähnliche Kernrückstoßsignale erzeugt; Direktionssensitivität (Neutrino-Richtungsbestimmung durch z.B. Xe-TPC-Drifttiming oder directionaler Recoil-Detektor) erlaubt statistische Trennung von WIMP (isotrop aus Galaxis) und Neutrino-Untergrund (gerichtet von Sonne/Atmosphäre)",
        answerB = "Der Neutrino Floor entsteht durch Cherenkov-Emission von solaren Neutrinos in flüssigem Xenon; er ist bei Energien < 5 keV dominant und kann durch Pulse-Shape-Discrimination (PSD) in NaI-Detektoren unterschieden werden",
        answerC = "Der Neutrino Floor bezeichnet die theoretische Mindest-WIMP-Wirkungsquerschnitt-Grenze durch Unitaritäts-Verletzung bei m_WIMP > 100 TeV; er ist unabhängig vom Detektor und kann nicht durch verbesserte Technologie überwunden werden",
        answerD = "Der Neutrino-Floor ist eine apparative Grenze durch Untergrundstrahlung aus Radon-Emanation der Detektorgefäße; er kann durch verbesserte Materialreinigung (<1 μBq/kg Rn) eliminiert werden",
        correctAnswer = 0, // A
        explanation = "Mit steigender Expositionsgröße (Masse × Zeit) in WIMP-Direktnachweis-Experimenten (LUX-ZEPLIN, XENONnT, PandaX-4T) wird der Untergrund durch kohärente elastische Neutrino-Kernstreuung (CEνNS, Freedman 1974, erst 2017 nachgewiesen) dominant. Solare pp- und ⁸B-Neutrinos, atmosph. Neutrinos und die diffuse supernova neutrino background (DSNB) erzeugen Kernrückstoße im gleichen Energiebereich wie WIMPs (1–100 keV). Diese Grenze ist kein harter Cutoff sondern statistisches 'Fog' (O'Hare 2021): Bei genug Statistik kann man WIMPs noch von CEνNS trennen. Die Lösung: Direktionssensitive Detektoren (z.B. directionaler Recoil-Track in Niedrigdruck-Gasdetektoren CYGNUS, CF₄-Emulsionen NEWAGE) nutzen das jährliche und tägliche Modulationsmuster des galaktischen WIMP-Windes (aus Cygnus-Richtung) vs. isotroper Neutrino-Untergrund.",
        difficulty = 5,
        funFact = "Das LUX-ZEPLIN-Experiment (2022, 5.5 Tonnen flüssiges Xenon im Homestake-Mine, 1.5 km unter der Erde) ist 1000× empfindlicher als der erste XENON10-Detektor von 2006 – und hat bisher noch kein WIMP-Signal gefunden."
    )

)
