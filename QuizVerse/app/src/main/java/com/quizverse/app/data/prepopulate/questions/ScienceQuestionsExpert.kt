package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun scienceQuestionsExpert(): List<Question> = listOf(

    // Question 1 — Particle Physics: Standard Model
    Question(
        categoryId = 2,
        questionText = "Welches Boson vermittelt im Standardmodell der Teilchenphysik die schwache Kernkraft?",
        answerA = "Photon",
        answerB = "Gluon",
        answerC = "Z⁰- und W±-Bosonen",
        answerD = "Graviton",
        correctAnswer = 2, // C
        explanation = "Die schwache Wechselwirkung wird durch drei Eichbosonen übertragen: das elektrisch neutrale Z⁰-Boson sowie die geladenen W⁺- und W⁻-Bosonen. Das Photon vermittelt den Elektromagnetismus, Gluonen die starke Kernkraft.",
        difficulty = 4,
        funFact = "Die W- und Z-Bosonen wurden 1983 am CERN erstmals nachgewiesen. Carlo Rubbia und Simon van der Meer erhielten dafür 1984 den Nobelpreis."
    ),

    // Question 2 — Particle Physics: Quarks
    Question(
        categoryId = 2,
        questionText = "Ein Proton besteht aus welcher Quark-Kombination?",
        answerA = "uud (zwei Up-Quarks, ein Down-Quark)",
        answerB = "udd (ein Up-Quark, zwei Down-Quarks)",
        answerC = "uus (zwei Up-Quarks, ein Strange-Quark)",
        answerD = "dds (zwei Down-Quarks, ein Strange-Quark)",
        correctAnswer = 0, // A
        explanation = "Ein Proton besteht aus zwei Up-Quarks (Ladung je +2/3 e) und einem Down-Quark (Ladung -1/3 e), was eine Gesamtladung von +1 e ergibt. Das Neutron hingegen hat die Kombination udd mit Gesamtladung 0.",
        difficulty = 4,
        funFact = null
    ),

    // Question 3 — Particle Physics: Higgs Mechanism
    Question(
        categoryId = 2,
        questionText = "Was beschreibt der Higgs-Mechanismus im Standardmodell?",
        answerA = "Die Entstehung von Farbladung bei Quarks durch spontane Symmetriebrechung",
        answerB = "Die Erzeugung von Massen für die W- und Z-Bosonen durch spontane Brechung der elektroschwachen Symmetrie",
        answerC = "Die Umwandlung von Leptonen in Quarks durch Yukawa-Kopplungen",
        answerD = "Die Quantisierung der Gravitation über ein Spinor-Feld",
        correctAnswer = 1, // B
        explanation = "Im Higgs-Mechanismus bricht das Higgs-Feld spontan die SU(2)×U(1)-Symmetrie der elektroschwachen Theorie. Dadurch erhalten die W±- und Z⁰-Bosonen Masse, während das Photon masselos bleibt. Fermionen erhalten ihre Masse durch Yukawa-Kopplung an das Higgs-Feld.",
        difficulty = 4,
        funFact = "Das Higgs-Boson wurde am 4. Juli 2012 am CERN mit dem LHC entdeckt – 48 Jahre nach Higgs' theoretischer Vorhersage."
    ),

    // Question 4 — Particle Physics: Leptons
    Question(
        categoryId = 2,
        questionText = "Wie viele Generationen von Leptonen kennt das Standardmodell, und welches Lepton hat die größte Masse?",
        answerA = "Zwei Generationen; das Myon ist das schwerste Lepton",
        answerB = "Drei Generationen; das Tau-Lepton ist das schwerste geladene Lepton",
        answerC = "Vier Generationen; das Tau-Neutrino ist das schwerste Lepton",
        answerD = "Drei Generationen; das Elektron ist das stabilste und schwerste Lepton",
        correctAnswer = 1, // B
        explanation = "Das Standardmodell enthält drei Leptonen-Generationen: (e, νe), (μ, νμ) und (τ, ντ). Das Tau-Lepton mit einer Masse von ~1777 MeV/c² ist das schwerste geladene Lepton; seine Neutrino-Partner haben sehr kleine, aber endliche Massen.",
        difficulty = 4,
        funFact = null
    ),

    // Question 5 — Particle Physics: Gauge Bosons
    Question(
        categoryId = 2,
        questionText = "Wie viele unabhängige Gluon-Typen gibt es in der Quantenchromodynamik (QCD)?",
        answerA = "3 (entsprechend den drei Farbladungen)",
        answerB = "6 (eine für jedes Quark-Flavour)",
        answerC = "8 (entsprechend den Generatoren der SU(3)-Gruppe)",
        answerD = "9 (alle möglichen Farb-Antifarb-Kombinationen)",
        correctAnswer = 2, // C
        explanation = "Die Farbsymmetriegruppe der QCD ist SU(3), die 8 unabhängige Generatoren (Gell-Mann-Matrizen) besitzt. Daher gibt es genau 8 Gluon-Typen. Es wären zwar 9 Farb-Antifarb-Kombinationen möglich, aber die Singlett-Kombination ist farbneutral und damit kein physikalisches Gluon.",
        difficulty = 4,
        funFact = "Gluonen tragen selbst Farbladung und können deshalb miteinander wechselwirken – ein fundamentaler Unterschied zu Photonen der QED."
    ),

    // Question 6 — Advanced Biochemistry: Michaelis-Menten
    Question(
        categoryId = 2,
        questionText = "Was beschreibt der Km-Wert (Michaelis-Konstante) in der Enzymkinetik?",
        answerA = "Die maximale Reaktionsgeschwindigkeit, die ein Enzym bei Substratsättigung erreicht",
        answerB = "Die Substratkonzentration, bei der die Reaktionsgeschwindigkeit die Hälfte von Vmax beträgt",
        answerC = "Die Gleichgewichtskonstante zwischen Enzym-Substrat-Komplex und freiem Enzym",
        answerD = "Die Aktivierungsenergie der enzymatischen Reaktion",
        correctAnswer = 1, // B
        explanation = "Der Km-Wert ist definiert als die Substratkonzentration [S], bei der v = Vmax/2. Er ist ein Maß für die Affinität des Enzyms zum Substrat: Ein niedriger Km bedeutet hohe Affinität, ein hoher Km niedrige Affinität. Km ≈ koff/kon nur wenn kcat << koff.",
        difficulty = 4,
        funFact = "Leonor Michaelis und Maud Menten entwickelten ihre kinetische Gleichung 1913 – Menten war eine der ersten Frauen, die in Kanada Medizin studierte."
    ),

    // Question 7 — Advanced Biochemistry: Protein Folding
    Question(
        categoryId = 2,
        questionText = "Welches Konzept beschreibt die thermodynamische Grundlage der spontanen Proteinfaltung?",
        answerA = "Das Levinthal-Paradoxon besagt, dass Proteine alle möglichen Konformationen sequenziell durchsuchen",
        answerB = "Die Faltung folgt einem Energielandschafts-Trichter, bei dem die native Konformation dem globalen Energieminimum entspricht",
        answerC = "Chaperone katalysieren die Faltung thermodynamisch durch Herabsetzung der Gibbs-Energie",
        answerD = "Die Sekundärstruktur entsteht ausschließlich durch kovalente Disulfidbrücken",
        correctAnswer = 1, // B
        explanation = "Das Energielandschafts-Modell (Energy Landscape / Funneling Theory) beschreibt die Proteinfaltung als Bewegung auf einem trichterförmigen Energieland. Chaperone verhindern Fehlfaltungen, katalysieren aber nicht thermodynamisch die Faltung. Das Levinthal-Paradoxon zeigt gerade, dass eine zufällige Suche unmöglich ist.",
        difficulty = 4,
        funFact = "AlphaFold2 von DeepMind hat 2021 die Strukturvorhersage revolutioniert und kann für die meisten Proteinsequenzen atomgenaue 3D-Strukturen vorhersagen."
    ),

    // Question 8 — Advanced Biochemistry: Signal Transduction
    Question(
        categoryId = 2,
        questionText = "Welches Second-Messenger-Molekül wird durch die Aktivierung der Adenylylzyklase produziert?",
        answerA = "Diacylglycerol (DAG)",
        answerB = "Inositol-1,4,5-trisphosphat (IP3)",
        answerC = "cyclisches Adenosinmonophosphat (cAMP)",
        answerD = "Phosphatidylinositol-3,4,5-trisphosphat (PIP3)",
        correctAnswer = 2, // C
        explanation = "Die Adenylylzyklase (auch Adenylatzyklase) katalysiert die Umwandlung von ATP in cyclisches AMP (cAMP) und Pyrophosphat. cAMP aktiviert als Second Messenger die Proteinkinase A (PKA). Die Phospholipase C produziert DAG und IP3; PI3-Kinasen erzeugen PIP3.",
        difficulty = 4,
        funFact = null
    ),

    // Question 9 — Advanced Biochemistry: Metabolomics
    Question(
        categoryId = 2,
        questionText = "Welche analytische Methode ist der Goldstandard für die Identifizierung und Quantifizierung kleiner Metaboliten im Metabolom?",
        answerA = "Western Blot kombiniert mit ELISA",
        answerB = "Massenspektrometrie (MS) gekoppelt mit Chromatographie (LC-MS oder GC-MS)",
        answerC = "Fluoreszenz-Aktivierte Zellsortierung (FACS)",
        answerD = "Röntgenkristallographie für Metabolitstrukturen",
        correctAnswer = 1, // B
        explanation = "LC-MS (Flüssigkeitschromatographie-Massenspektrometrie) und GC-MS (Gaschromatographie-MS) sind die bevorzugten Methoden der Metabolomics. Sie ermöglichen gleichzeitig Trennung, Identifizierung über Masse-zu-Ladungs-Verhältnisse (m/z) und Quantifizierung von Hunderten bis Tausenden Metaboliten.",
        difficulty = 4,
        funFact = "Das menschliche Metabolom umfasst schätzungsweise 100.000 bis 200.000 verschiedene Moleküle. Die Human Metabolome Database (HMDB) catalogisiert davon bisher ~220.000."
    ),

    // Question 10 — Advanced Biochemistry: Proteomics
    Question(
        categoryId = 2,
        questionText = "Was versteht man unter dem Begriff 'Post-translationaler Modifikation' (PTM) in der Proteomik?",
        answerA = "Chemische Veränderungen an einem Protein, die während der ribosomalen Translation stattfinden",
        answerB = "Covalente oder nicht-kovalente chemische Modifikationen eines Proteins nach dem Ende der Translation",
        answerC = "Die alternative Spleißvariante einer mRNA, die zu verschiedenen Proteinlängen führt",
        answerD = "Die Faltung von Proteinen durch Chaperone im endoplasmatischen Retikulum",
        correctAnswer = 1, // B
        explanation = "PTMs sind kovalente Modifikationen wie Phosphorylierung, Ubiquitinierung, Acetylierung, Glykosylierung oder Methylierung von Aminosäureresten nach der Translation. Sie erweitern die funktionale Diversität des Proteoms enorm: Aus ~20.000 menschlichen Genen entstehen durch PTMs >1 Million Proteinvarianten.",
        difficulty = 4,
        funFact = null
    ),

    // Question 11 — Neurophysiology: Ion Channels
    Question(
        categoryId = 2,
        questionText = "Welcher Ionenkanal ist primär verantwortlich für den aufsteigenden Schenkel (Depolarisation) des Aktionspotentials in Nervenfasern?",
        answerA = "Spannungsgesteuerter Kaliumkanal (Kv)",
        answerB = "Ligandengesteuerter Chloridkanal (GABA-A-Rezeptor)",
        answerC = "Spannungsgesteuerter Natriumkanal (Nav)",
        answerD = "Nicht-selektiver Kationenkanal (HCN-Kanal)",
        correctAnswer = 2, // C
        explanation = "Spannungsgesteuerte Natriumkanäle (Nav) öffnen bei Depolarisation rasch und ermöglichen den massiven Na⁺-Einstrom, der das Aktionspotential erzeugt. Die anschließende Repolarisation erfolgt durch Öffnung verzögerter gleichgerichteter Kv-Kanäle und Inaktivierung der Nav-Kanäle.",
        difficulty = 4,
        funFact = "Tetrodotoxin (TTX) aus dem Kugelfisch blockiert Nav-Kanäle mit picomolarer Affinität – es ist eines der wirksamsten nicht-proteinhaltigen Gifte, die bekannt sind."
    ),

    // Question 12 — Neurophysiology: Long-Term Potentiation
    Question(
        categoryId = 2,
        questionText = "Welche molekulare Eigenschaft des NMDA-Rezeptors macht ihn zum Koinzidenzdetektor bei der synaptischen Langzeitpotenzierung (LTP)?",
        answerA = "Er ist ausschließlich ligandengesteuert und öffnet nur bei Glutamatbindung",
        answerB = "Er ist sowohl liganden- als auch spannungsgesteuert; der Mg²⁺-Block wird erst bei Depolarisation aufgehoben",
        answerC = "Er aktiviert direkt die Adenylatcyclase und erhöht den cAMP-Spiegel postsynaptisch",
        answerD = "Er ist ein G-Protein-gekoppelter Rezeptor, der IP3-vermittelte Ca²⁺-Freisetzung auslöst",
        correctAnswer = 1, // B
        explanation = "Der NMDA-Rezeptor ist spannungs- UND ligandengesteuert. Bei Ruhemembranpotenzial blockiert Mg²⁺ den Ionenkanal. Erst wenn gleichzeitig Glutamat bindet UND die postsynaptische Membran depolarisiert ist (Mg²⁺-Block aufgehoben), strömt Ca²⁺ ein. Diese Koinzidenzdetektion ist die Grundlage für Hebbsches Lernen und LTP.",
        difficulty = 4,
        funFact = "Das Prinzip 'Neurons that fire together, wire together' wurde von Donald Hebb 1949 postuliert – NMDA-Rezeptoren liefern den molekularen Mechanismus dafür."
    ),

    // Question 13 — Neurophysiology: Neurodegenerative Diseases
    Question(
        categoryId = 2,
        questionText = "Welches pathologische Protein akkumuliert als 'Lewy-Körperchen' bei der Parkinson-Erkrankung?",
        answerA = "Tau-Protein in hyperphosphorylierter Form",
        answerB = "Beta-Amyloid als extrazelluläre Plaques",
        answerC = "Alpha-Synuclein in fibrillärer, aggregierter Form",
        answerD = "Huntingtin-Protein mit polyGlutamin-Expansion",
        correctAnswer = 2, // C
        explanation = "Lewy-Körperchen sind intrazelluläre Einschlüsse aus aggregiertem Alpha-Synuclein, die in Dopamin-produzierenden Neuronen der Substantia nigra bei Parkinson auftreten. Hyperphosphoryliertes Tau bildet neurofibrilläre Bündel bei Alzheimer und Tauopathien; Beta-Amyloid-Plaques sind ebenfalls ein Alzheimer-Merkmal.",
        difficulty = 4,
        funFact = null
    ),

    // Question 14 — Neurophysiology: Brain Imaging
    Question(
        categoryId = 2,
        questionText = "Welches biophysikalische Prinzip liegt der funktionellen MRT (fMRT) und dem BOLD-Signal zugrunde?",
        answerA = "Die direkte Messung elektrischer Aktionspotentiale durch Gradienten-Spulen",
        answerB = "Unterschiedliche magnetische Eigenschaften von oxygeniertem (diamagnetisch) und desoxygeniertem (paramagnetisch) Hämoglobin",
        answerC = "Die Diffusion von Wassermolekülen entlang axonaler Faserbahnen",
        answerD = "Die Messung von Glukose-PET-Signalen kombiniert mit anatomischer MRT",
        correctAnswer = 1, // B
        explanation = "Das BOLD-Signal (Blood Oxygen Level-Dependent) beruht darauf, dass desoxygeniertes Hämoglobin (Deoxy-Hb) paramagnetisch ist und das lokale Magnetfeld stört, während oxygeniertes Hämoglobin diamagnetisch ist. Neuronale Aktivität führt zu lokalem Anstieg des Oxy-Hb durch neurovaskuläre Kopplung, was das MRT-Signal verändert.",
        difficulty = 4,
        funFact = "fMRT misst nicht direkt Neuronen-Aktivität, sondern die vaskuläre Antwort mit einer zeitlichen Verzögerung von 4-6 Sekunden – das sogenannte 'hämodynamische Lag'."
    ),

    // Question 15 — Neurophysiology: Cognitive Neuroscience
    Question(
        categoryId = 2,
        questionText = "Welche Hirnstruktur spielt die zentrale Rolle bei der Konsolidierung von deklarativen Langzeitgedächtnissen?",
        answerA = "Amygdala – zuständig für emotionale Bewertung aller Gedächtnisinhalte",
        answerB = "Hippocampus – bindet kortikale Repräsentationen zu kohärenten Episoden zusammen",
        answerC = "Kleinhirn – koordiniert die zeitliche Abfolge von Gedächtnisspeicherungen",
        answerD = "Basalganglien – konsolidieren semantisches Wissen durch Dopaminausschüttung",
        correctAnswer = 1, // B
        explanation = "Der Hippocampus ist entscheidend für die Konsolidierung deklarativer Gedächtnisse (episodisch und semantisch). Patient H.M., dem bilateral der Hippocampus entfernt wurde, konnte keine neuen deklarativen Gedächtnisinhalte mehr bilden. Die Amygdala moduliert emotionale Gedächtnisse; Basalganglien sind eher für prozedurales Gedächtnis zuständig.",
        difficulty = 4,
        funFact = "Patient H.M. (Henry Molaison) verlor 1953 durch eine Operation seinen Hippocampus. Er wurde zum meistuntersuchten Patienten der Neuropsychologie-Geschichte."
    ),

    // Question 16 — Cosmology: Cosmic Inflation
    Question(
        categoryId = 2,
        questionText = "Welches kosmologische Problem löst die Inflationstheorie unter anderem durch eine exponentielle Expansionsphase im frühen Universum?",
        answerA = "Das Dunkle-Energie-Problem und die beschleunigte Expansion des gegenwärtigen Universums",
        answerB = "Das Horizont- und das Flachheitsproblem sowie die Abwesenheit magnetischer Monopole",
        answerC = "Die Baryonasymmetrie und das Fehlen von Antimaterie im beobachtbaren Universum",
        answerD = "Die Rotverschiebung von Galaxien und die Hubble-Konstante",
        correctAnswer = 1, // B
        explanation = "Die kosmische Inflation (Alan Guth, 1980) löst drei klassische Probleme: Das Horizontproblem (warum ist der CMB so gleichmäßig trotz kausaler Trennung), das Flachheitsproblem (warum ist die Raumdichte so nah am kritischen Wert) und das Monopolproblem (warum gibt es keine magnetischen Monopole trotz GUT-Vorhersagen).",
        difficulty = 4,
        funFact = "Die Inflation dauerte nur ~10⁻³² Sekunden, vergrößerte das Universum aber um einen Faktor von mindestens 10²⁶."
    ),

    // Question 17 — Cosmology: CMB Anisotropies
    Question(
        categoryId = 2,
        questionText = "Was verrät das akustische erste 'Doppler-Peak' im Leistungsspektrum der kosmischen Mikrowellenhintergrundstrahlung (CMB)?",
        answerA = "Die genaue Temperatur des Universums zum Zeitpunkt der Rekombination bei z ≈ 1100",
        answerB = "Die Winkelskala des Schalldurchmessers des Universums bei Rekombination und damit die geometrische Krümmung des Raums",
        answerC = "Das Verhältnis von baryonischer zu dunkler Materie im frühen Universum",
        answerD = "Den Zeitpunkt der Reinisierung des Universums durch erste Sterne",
        correctAnswer = 1, // B
        explanation = "Die Position (Winkellage) des ersten akustischen Peaks im CMB-Leistungsspektrum bei l ≈ 220 entspricht der Winkelgröße des Schalldurchmessers (Schallhorizont) zum Zeitpunkt der Rekombination. In einem flachen Universum (Ω_total ≈ 1) erwartet man diesen Peak bei genau dieser Position – was Planck bestätigte.",
        difficulty = 4,
        funFact = null
    ),

    // Question 18 — Cosmology: Dark Matter Candidates
    Question(
        categoryId = 2,
        questionText = "Welcher Kandidat für kalte dunkle Materie (Cold Dark Matter, CDM) wird in supersymmetrischen Theorien bevorzugt?",
        answerA = "Axionen – sehr leichte Pseudoskalarbosonen aus dem Peccei-Quinn-Mechanismus",
        answerB = "Sterile Neutrinos – rechtshändige Neutrinos ohne Standardmodell-Kopplung",
        answerC = "Neutralinos – das leichteste supersymmetrische Teilchen (LSP) in R-Paritäts-erhaltenden SUSY-Modellen",
        answerD = "Primordiale Schwarze Löcher aus Dichtefluktuationen der frühen kosmischen Inflation",
        correctAnswer = 2, // C
        explanation = "In SUSY-Modellen mit R-Parität-Erhaltung ist das leichteste supersymmetrische Teilchen (LSP) stabil. Das Neutralino – eine Mischung aus Bino, Wino und Higgsino – ist ein typisches WIMP (Weakly Interacting Massive Particle) mit Masse im GeV-TeV-Bereich und wäre ein guter CDM-Kandidat. Axione sind hingegen ultra-leichte CDM-Kandidaten.",
        difficulty = 4,
        funFact = "Trotz jahrzehntelanger Suche mit Experimenten wie XENON1T, LUX-ZEPLIN und PandaX wurde noch kein WIMP direkt nachgewiesen."
    ),

    // Question 19 — Cosmology: Large-Scale Structure
    Question(
        categoryId = 2,
        questionText = "Was beschreibt das 'kosmische Netz' (Cosmic Web) und welche Strukturen bildet es?",
        answerA = "Die Verteilung von Quasaren entlang der Lichtkegelgeometrie des beobachtbaren Universums",
        answerB = "Das filamentäre Großraumnetz aus Galaxienhaufen, Filamenten, Bögen und Leerräumen (Voids), geformt durch Gravitationsinstabilität",
        answerC = "Das Netzwerk dunkler Energiefilamente, die die beschleunigte Expansion antreiben",
        answerD = "Die Quanten-verschränkte Struktur der CMB-Fluktuationen projiziert auf die Himmelsebene",
        correctAnswer = 1, // B
        explanation = "Das kosmische Netz (Zeldovich, 1970; weiterentwickelt durch N-Körper-Simulationen wie Millennium) beschreibt die Großstruktur des Universums: Galaxienhaufen in Knoten, verbunden durch Filamente aus Galaxien und Gas, umgeben von riesigen leeren Voids. Diese Struktur entsteht aus primären Dichtefluktuationen durch gravitationale Instabilität.",
        difficulty = 4,
        funFact = "Der größte bekannte Leerraum (Void) – der 'Eridanus-Supervoid' – hat einen Durchmesser von ~1 Milliarde Lichtjahren."
    ),

    // Question 20 — Cosmology: Big Bang Nucleosynthesis
    Question(
        categoryId = 2,
        questionText = "Welche leichten Elemente wurden hauptsächlich während der Urknall-Nukleosynthese (BBN) erzeugt?",
        answerA = "Wasserstoff, Helium-4, Deuterium, Helium-3 und geringe Mengen Lithium-7",
        answerB = "Wasserstoff, Kohlenstoff-12, Stickstoff-14 und Sauerstoff-16",
        answerC = "Helium-4, Beryllium-9, Bor-10 und Lithium-6 in ungefähr gleichen Mengen",
        answerD = "Nur Wasserstoff und Helium-4; alle anderen Elemente entstanden in Sternen",
        correctAnswer = 0, // A
        explanation = "Die BBN fand in den ersten Minuten nach dem Urknall statt (T ~ 10⁸-10⁹ K). Es entstanden: ~75% H, ~25% ⁴He (nach Masse), geringe Mengen ²H (Deuterium), ³He und ⁷Li. Schwerere Elemente bis Eisen entstehen in Sternen (stellare Nukleosynthese); noch schwerere durch Supernovae und Neutronensternverschmelzungen (r-Prozess).",
        difficulty = 4,
        funFact = "Das kosmische Deuterium-zu-Wasserstoff-Verhältnis von ~2,5×10⁻⁵ ist eine der empfindlichsten Messungen für die Baryonendichte des Universums."
    ),

    // Question 21 — Materials Science: Semiconductor Physics
    Question(
        categoryId = 2,
        questionText = "Was beschreibt die 'effektive Masse' eines Elektrons in einem Halbleiter?",
        answerA = "Die relativistische Massenzunahme von Elektronen bei hohen Driftgeschwindigkeiten im elektrischen Feld",
        answerB = "Die scheinbare Masse, die ein Elektron aufgrund der periodischen Gitterpotenzials zeigt, definiert durch die Krümmung der Bandstruktur E(k)",
        answerC = "Die reduzierte Masse des Elektron-Loch-Paares in einem Exziton",
        answerD = "Das Verhältnis von Elektronenmasse zu Lochmasse im intrinsischen Halbleiter",
        correctAnswer = 1, // B
        explanation = "Die effektive Masse m* = ħ²/(d²E/dk²) beschreibt, wie ein Bloch-Elektron auf externe Kräfte reagiert, als wäre es ein freies Teilchen mit modifizierter Masse. Die Krümmung der E(k)-Dispersionskurve bestimmt m*: große Krümmung → kleine effektive Masse → hohe Mobilität. Bei GaAs ist m*_e ≈ 0,067 m_e.",
        difficulty = 4,
        funFact = null
    ),

    // Question 22 — Materials Science: Superconductors
    Question(
        categoryId = 2,
        questionText = "Was erklärt die BCS-Theorie als Ursache der konventionellen Supraleitung?",
        answerA = "Lokalisierung von Elektronen in Anderson-Zuständen unterhalb der Mobilkante",
        answerB = "Bose-Einstein-Kondensation von Elektronen in Bloch-Zuständen bei tiefen Temperaturen",
        answerC = "Cooper-Paarbildung durch phononenvermittelte, attraktive Wechselwirkung zwischen Elektronen mit entgegengesetztem Spin und Impuls",
        answerD = "Verschwinden des elektrischen Widerstands durch vollständige Unterdrückung der Phonon-Streuung",
        correctAnswer = 2, // C
        explanation = "Die BCS-Theorie (Bardeen, Cooper, Schrieffer, 1957, Nobelpreis 1972) erklärt konventionelle Supraleitung durch Cooper-Paar-Bildung: Ein Elektron polarisiert das Ionengitter, was eine attraktive Wechselwirkung für ein zweites Elektron erzeugt. Cooper-Paare sind Bosonen und kondensieren in einen einzigen Quantenzustand – ohne Streuung fließt der Strom widerstandslos.",
        difficulty = 4,
        funFact = "Hochtemperatursupraleiter wie YBa₂Cu₃O₇ (Tc ≈ 93 K) folgen NICHT der BCS-Theorie; ihr Mechanismus ist trotz 40 Jahren Forschung noch nicht vollständig verstanden."
    ),

    // Question 23 — Materials Science: Nanomaterials
    Question(
        categoryId = 2,
        questionText = "Welcher Effekt erklärt die außergewöhnlichen mechanischen und elektrischen Eigenschaften von Graphen?",
        answerA = "Quanteneinschluss in zwei Dimensionen erhöht die Bandlücke durch quantenmechanische Nullpunktsenergie",
        answerB = "Die sp²-Hybridisierung aller Kohlenstoffatome erzeugt ein vollständig konjugiertes π-System; Elektronen verhalten sich als masselose Dirac-Fermionen",
        answerC = "Kovalente Bindungen entlang aller drei Raumachsen verleihen Graphen eine Diamant-ähnliche Härte",
        answerD = "Supraleitende Cooper-Paare entstehen durch die 2D-Topologie bei Raumtemperatur",
        correctAnswer = 1, // B
        explanation = "In Graphen sind alle C-Atome sp²-hybridisiert, die p_z-Orbitale überlappen zu einem ausgedehnten π-System. Am K-Punkt der Brillouinzone berühren sich Valenz- und Leitungsband, die lineare Dispersion führt zu Elektronen, die sich wie masselose Dirac-Fermionen verhalten (v_F ≈ 10⁶ m/s). Daher zeigt Graphen den anomalen Quanten-Hall-Effekt und extrem hohe Mobilität.",
        difficulty = 4,
        funFact = "Andre Geim und Konstantin Novoselov isolierten 2004 Graphen mit Klebeband – und gewannen 2010 den Nobelpreis für Physik."
    ),

    // Question 24 — Materials Science: Crystal Structures
    Question(
        categoryId = 2,
        questionText = "In welcher Raumgruppe kristallisiert Kochsalz (NaCl) und wie lässt sich die Struktur beschreiben?",
        answerA = "Fm3m (kubisch-flächenzentriert): Na⁺ und Cl⁻ bilden je ein kubisch-flächenzentriertes (fcc) Teilgitter, die ineinandergeschachtelt sind",
        answerB = "Im3m (kubisch-raumzentriert): Na⁺ sitzt im Zentrum des Würfels, Cl⁻ auf den Ecken",
        answerC = "P6₃/mmc (hexagonal-dichtgepackt): abwechselnde Na/Cl-Schichten in hexagonaler Packung",
        answerD = "Fd3m (kubisch-flächenzentriert): Diamantstruktur mit zwei unterschiedlichen Atomen",
        correctAnswer = 0, // A
        explanation = "NaCl kristallisiert in der Raumgruppe Fm3m (O_h^5, Nr. 225). Die Struktur besteht aus zwei ineinandergeschachtelten fcc-Gittern, eines mit Na⁺ und eines mit Cl⁻, verschoben um ½a längs einer Würfelkante. Jedes Ion ist von 6 Ionen der Gegenladung oktaedrisch koordiniert. Die Formeleinheit pro Einheitszelle enthält 4 NaCl.",
        difficulty = 4,
        funFact = null
    ),

    // Question 25 — Materials Science: Phase Transitions
    Question(
        categoryId = 2,
        questionText = "Was ist der Unterschied zwischen einem Phasenübergang erster und zweiter Ordnung nach Ehrenfest?",
        answerA = "Erster Ordnung: kontinuierliche erste Ableitung der freien Energie; zweiter Ordnung: Sprung in der ersten Ableitung",
        answerB = "Erster Ordnung: Sprung in der ersten Ableitung der freien Energie (Volumen, Entropie); zweiter Ordnung: Divergenz in der zweiten Ableitung (Wärmekapazität, Kompressibilität)",
        answerC = "Erster Ordnung: irreversibler Prozess mit Entropieproduktion; zweiter Ordnung: reversibler Prozess ohne latente Wärme",
        answerD = "Erster Ordnung: betrifft nur Feststoff-Flüssig-Übergänge; zweiter Ordnung: betrifft nur Flüssig-Gas-Übergänge",
        correctAnswer = 1, // B
        explanation = "Nach der Ehrenfest-Klassifikation zeigt ein Phasenübergang n-ter Ordnung Unstetigkeiten in der n-ten Ableitung der freien Enthalpie G. Bei Übergängen erster Ordnung (Schmelzen, Verdampfen) springen V = (∂G/∂P) und S = -(∂G/∂T) – es gibt latente Wärme. Bei zweiter Ordnung (Curie-Punkt, Supraleitung) divergieren Wärmekapazität und Suszeptibilität.",
        difficulty = 4,
        funFact = "Der Übergang von flüssigem Helium-4 zur superfluiden Phase (Lambda-Punkt bei 2,17 K) ist ein klassischer Phasenübergang zweiter Ordnung mit charakteristischem lambda-förmigem Wärmekapazitätspeak."
    ),

    // Question 26 — Advanced Genetics: Gene Regulation
    Question(
        categoryId = 2,
        questionText = "Welche Rolle spielen Enhancer-Sequenzen in der eukaryotischen Genregulation?",
        answerA = "Sie kodieren für RNA-Polymerase-II-Untereinheiten, die die Transkription direkt beschleunigen",
        answerB = "Sie sind cis-regulatorische DNA-Elemente, die Transkriptionsfaktoren binden und die Promotoraktivität über Schleifen (Loops) über weite Distanzen stimulieren",
        answerC = "Sie methylieren die DNA am Promotor und verstärken so die Chromatin-Öffnung",
        answerD = "Sie bilden alternative Polyadenylierungssignale, die die mRNA-Stabilität erhöhen",
        correctAnswer = 1, // B
        explanation = "Enhancer sind cis-wirkende DNA-Elemente, die meist Hunderte bis Tausende Basenpaare vom Zielgen entfernt liegen können. Sie binden sequenzspezifische Transkriptionsfaktoren (Aktivatoren) und wechselwirken mit dem Promotor durch Chromatin-Schleifen (Looping), vermittelt durch Mediator-Komplex und Kohäsin. Sie können in beiden Orientierungen und in großer Distanz wirken.",
        difficulty = 4,
        funFact = "Das menschliche Genom enthält schätzungsweise >500.000 aktive Enhancer – deutlich mehr als Protein-kodierende Gene."
    ),

    // Question 27 — Advanced Genetics: Epigenomics
    Question(
        categoryId = 2,
        questionText = "Welcher Chromatin-Zustand ist mit der Trimethylierung von Histon H3 an Lysin 27 (H3K27me3) assoziiert?",
        answerA = "Aktiv transkribiertes Euchromatin; H3K27me3 ist ein Aktivierungsmarker an Promotoren",
        answerB = "Polycomb-vermitteltes fakultatives Heterochromatin und transkriptionelle Repression",
        answerC = "Konstitutives Heterochromatin an Zentromeren und Telomeren",
        answerD = "Enhancer-Aktivierung; H3K27me3 co-lokalisiert mit H3K27ac",
        correctAnswer = 1, // B
        explanation = "H3K27me3 wird durch den Polycomb Repressive Complex 2 (PRC2) mit der Histonmethyltransferase EZH2 gesetzt. Es markiert fakultatives Heterochromatin und reprimiert entwicklungsrelevante Gene. Im Gegensatz dazu ist H3K27ac ein aktiver Enhancer-Marker, H3K9me3 markiert konstitutives Heterochromatin, und H3K4me3 ist typisch für aktive Promotoren.",
        difficulty = 4,
        funFact = null
    ),

    // Question 28 — Advanced Genetics: Genomic Imprinting
    Question(
        categoryId = 2,
        questionText = "Was versteht man unter 'genomischem Imprinting' und welche Erkrankungen entstehen bei dessen Störung?",
        answerA = "Zufällige Inaktivierung eines elterlichen Chromosoms in jeder Zelle; Störungen führen zu Trisomien",
        answerB = "Epigenetisch vermittelte mono-allelische Genexpression abhängig vom elterlichen Ursprung; Störungen verursachen z.B. Prader-Willi- und Angelman-Syndrom",
        answerC = "Somatische Mutation eines elterlichen Allels durch Fehler bei der DNA-Replikation in der Keimbahn",
        answerD = "Horizontaler Gentransfer von väterlichen auf mütterliche Chromosomen während der Meiose",
        correctAnswer = 1, // B
        explanation = "Genomisches Imprinting ist die epigenetische Markierung von Genen je nach elterlichem Ursprung (differenzielle DNA-Methylierung an Imprinting Control Regions). Beim Prader-Willi-Syndrom fehlt die paternale Expression auf Chromosom 15q11-q13; beim Angelman-Syndrom die maternale Expression desselben Locus (insb. UBE3A). Beide Erkrankungen können durch Deletion desselben Chromosomenabschnitts entstehen.",
        difficulty = 4,
        funFact = "Ca. 100-150 Gene im menschlichen Genom unterliegen genomischem Imprinting. Der evolutionäre Grund ist die 'Eltern-Konflikt-Hypothese' (Haig-Theorie)."
    ),

    // Question 29 — Advanced Genetics: Transposable Elements
    Question(
        categoryId = 2,
        questionText = "Welchen Mechanismus nutzen LINEs (Long Interspersed Nuclear Elements) für ihre Transposition?",
        answerA = "DNA-Transposition via 'Cut-and-Paste'-Mechanismus durch eine transposon-kodierte Transposase",
        answerB = "Retrotransposition via RNA-Intermediat: Transkription → Reverse Transkription → Integration ('Copy-and-Paste')",
        answerC = "Homologe Rekombination zwischen identischen LINE-Sequenzen in verschiedenen Chromosomenbereichen",
        answerD = "Plasmid-vermittelte Transposition durch einen Integrase-ähnlichen Mechanismus",
        correctAnswer = 1, // B
        explanation = "LINEs sind nicht-LTR-Retrotransposons. Ihr Mechanismus: 1) Transkription der LINE-DNA zu mRNA, 2) Translation der ORF1- und ORF2-Proteine (ORF2 kodiert Endonuklease und Reverse Transkriptase), 3) TARGET PRIMED REVERSE TRANSCRIPTION (TPRT): die Endonuklease schneidet die Ziel-DNA, dann synthetisiert die RT die cDNA direkt am Zielort. Ca. 17% des menschlichen Genoms sind L1-Elemente (LINEs).",
        difficulty = 4,
        funFact = null
    ),

    // Question 30 — Advanced Genetics: Population Genetics
    Question(
        categoryId = 2,
        questionText = "Was besagt das Hardy-Weinberg-Gleichgewicht und welche Voraussetzungen müssen erfüllt sein?",
        answerA = "Die Allelfrequenzen verändern sich nicht über Generationen, wenn: unendliche Populationsgröße, zufällige Paarung, keine Mutation, keine Migration und keine Selektion vorliegen",
        answerB = "Die Fitness eines Allels ist proportional zu seiner Frequenz in der Population bei konstantem Selektionsdruck",
        answerC = "Dominant-rezessive Vererbungsverhältnisse folgen immer einem 3:1-Phänotyp-Verhältnis in F2-Generationen",
        answerD = "Genetische Drift eliminiert alle neutralen Allele innerhalb von N/2 Generationen in einer Population der Größe N",
        correctAnswer = 0, // A
        explanation = "Das Hardy-Weinberg-Prinzip (G.H. Hardy und W. Weinberg, 1908) besagt: In einer idealen Population bleiben Genotyphäufigkeiten p² (AA) + 2pq (Aa) + q² (aa) konstant, wenn Populationsgröße unendlich, Paarung zufällig, keine Mutation, keine Migration und keine Selektion vorliegen. Abweichungen vom HWG zeigen evolutionäre Kräfte an.",
        difficulty = 4,
        funFact = "Das Hardy-Weinberg-Prinzip ist das fundamentale Null-Modell der Populationsgenetik – ähnlich wie Newtons Trägheitsgesetz in der Mechanik."
    ),

    // Question 31 — Quantum Chemistry: Molecular Orbital Theory
    Question(
        categoryId = 2,
        questionText = "Was erklärt die MO-Theorie beim paramagnetischen Verhalten von molekularem Sauerstoff (O₂)?",
        answerA = "O₂ hat zwei ungepaarte Elektronen in entarteten antibindenden π*-Orbitalen (nach Hundscher Regel)",
        answerB = "O₂ hat eine σ-Doppelbindung und zwei ungepaarte Elektronen in bindenden σ-Orbitalen",
        answerC = "Das Lewis-Struktur-Modell erklärt den Paramagnetismus durch polare Doppelbindungen",
        answerD = "Die d-Orbitale des Sauerstoffs hybridisieren zu zwei SOMO-Zuständen",
        correctAnswer = 0, // A
        explanation = "In der MO-Konfiguration von O₂ sind die beiden höchst besetzten π*₂p-Orbitale entartet. Nach der Hundschen Regel wird jedes mit einem Elektron mit parallelem Spin besetzt. Die zwei ungepaarten Elektronen (S=1, Triplett-Grundzustand) verursachen den Paramagnetismus. Die Valenzbindungstheorie (Lewis-Struktur) sagte dies nicht vorher.",
        difficulty = 4,
        funFact = "Flüssiger Sauerstoff wird von einem Permanentmagneten angezogen – ein eindrucksvoller Beweis für seinen Paramagnetismus."
    ),

    // Question 32 — Quantum Chemistry: DFT
    Question(
        categoryId = 2,
        questionText = "Was beschreibt das erste Hohenberg-Kohn-Theorem als theoretische Grundlage der Dichtefunktionaltheorie (DFT)?",
        answerA = "Die Elektronendichte des Grundzustands ist eindeutig durch das externe Potential bestimmt, und umgekehrt",
        answerB = "Die Korrelationsenergie eines Vielelektronensystems kann exakt durch ein Austausch-Korrelations-Funktional berechnet werden",
        answerC = "Die kinetische Energie der Elektronen ist ein universelles Funktional der Elektronendichte gemäß Thomas-Fermi",
        answerD = "Die Kohn-Sham-Gleichungen sind exakt äquivalent zu den Hartree-Fock-Gleichungen",
        correctAnswer = 0, // A
        explanation = "Das erste HK-Theorem (1964) beweist: Das externe Potential V_ext(r) ist – bis auf eine Konstante – eindeutig durch die Grundzustands-Elektronendichte ρ(r) bestimmt. Da V_ext den Hamilton-Operator festlegt, bestimmt ρ(r) alle Grundzustandseigenschaften. Das zweite HK-Theorem begründet das Variationsprinzip für die Energie als Funktional von ρ.",
        difficulty = 4,
        funFact = "Walter Kohn erhielt 1998 den Nobelpreis für Chemie für die Entwicklung der DFT. Sie ist heute die am häufigsten verwendete Methode in der Computerchemie."
    ),

    // Question 33 — Quantum Chemistry: Born-Oppenheimer Approximation
    Question(
        categoryId = 2,
        questionText = "Welche physikalische Überlegung rechtfertigt die Born-Oppenheimer-Näherung in der molekularen Quantenmechanik?",
        answerA = "Die Elektronen bewegen sich so langsam, dass die Kerne als stationär angesehen werden können",
        answerB = "Weil Kerne viel schwerer als Elektronen sind (m_N >> m_e), bewegen sich Elektronen viel schneller und passen sich quasi-instantan an jede Kerngeometrie an",
        answerC = "Die Potenzialenergiefläche ist harmonisch, sodass Kern- und Elektronenbewegung separiert werden können",
        answerD = "Die relativistischen Effekte der Kernbewegung vernachlässigen elektronische Korrelation",
        correctAnswer = 1, // B
        explanation = "Die Born-Oppenheimer-Näherung (1927) nutzt die große Massendifferenz zwischen Kernen und Elektronen (m_Proton ≈ 1836 m_e). Da Elektronen viel leichter sind, bewegen sie sich ~√(m_N/m_e) ≈ 43× schneller. Man löst die elektronische Schrödinger-Gleichung für fixierte Kernpositionen und erhält die Potenzialenergiefläche, auf der die Kernbewegung stattfindet.",
        difficulty = 4,
        funFact = null
    ),

    // Question 34 — Quantum Chemistry: Electron Correlation
    Question(
        categoryId = 2,
        questionText = "Was ist der Unterschied zwischen statischer (nicht-dynamischer) und dynamischer Elektronenkorrelation?",
        answerA = "Statische Korrelation betrifft Kern-Elektronen; dynamische Korrelation betrifft Valenz-Elektronen",
        answerB = "Statische Korrelation tritt bei Systemen mit quasi-entarteten Konfigurationen auf (z.B. Bindungsbruch) und erfordert Multireferenz-Methoden; dynamische Korrelation ist die kurzreichweitige e-e-Abstoßung, die von MP2/CCSD erfasst wird",
        answerC = "Dynamische Korrelation ist exakt im Hartree-Fock-Verfahren enthalten; statische erfordert DFT",
        answerD = "Statische Korrelation wird durch Spin-Bahn-Kopplung verursacht; dynamische durch Coulomb-Wechselwirkung",
        correctAnswer = 1, // B
        explanation = "Statische (nicht-dynamische) Korrelation: Entsteht wenn mehrere Slater-Determinanten ähnliches Gewicht haben (quasi-Entartung), z.B. beim Bindungsbruch. Erfordert CASSCF/CASPT2/MRCI. Dynamische Korrelation: Kurz-reichweitige instantane Abstoßung zwischen Elektronen, die eine einzelne Referenz-Determinante mit Störungstheorie (MP2), Coupled Cluster (CCSD(T)) oder DFT beschreibt.",
        difficulty = 4,
        funFact = "CCSD(T) gilt als 'Goldstandard' der Quantenchemie für dynamische Korrelation, skaliert aber mit O(N⁷) – weshalb DFT für große Moleküle bevorzugt wird."
    ),

    // Question 35 — Quantum Chemistry: Spectroscopy
    Question(
        categoryId = 2,
        questionText = "Warum sind homonukleare zweiatomige Moleküle wie N₂ oder O₂ im IR-Spektrum inaktiv?",
        answerA = "Ihre Schwingungsfrequenz liegt außerhalb des infraroten Spektralbereichs",
        answerB = "Weil das Dipolmoment sich während der Schwingung nicht ändert (∂μ/∂Q = 0) – sie besitzen kein permanentes oder Schwingungsdipolmoment",
        answerC = "Homonukleare Moleküle absorbieren nur UV-Strahlung aufgrund ihrer hohen Bindungsenergien",
        answerD = "Das Auswahlprinzip für IR-Aktivität verbietet alle Übergänge mit ΔJ = 0 in linearen Molekülen",
        correctAnswer = 1, // B
        explanation = "Das IR-Auswahlprinzip erfordert eine Änderung des elektrischen Dipolmoments während der Schwingung (∂μ/∂Q ≠ 0). Homonukleare zweiatomige Moleküle (N₂, O₂, H₂) haben kein permanentes Dipolmoment, und die Schwingung verändert es auch nicht, da die Ladungsverteilung symmetrisch bleibt. Daher sind sie IR-inaktiv, aber Raman-aktiv.",
        difficulty = 4,
        funFact = "Diese IR-Inaktivität von N₂ und O₂ erklärt, warum die Hauptkomponenten der Atmosphäre kaum zum Treibhauseffekt beitragen – im Gegensatz zu CO₂ und H₂O."
    ),

    // Question 36 — Geophysics: Seismic Waves
    Question(
        categoryId = 2,
        questionText = "Welcher Unterschied besteht zwischen P-Wellen und S-Wellen in der Seismologie?",
        answerA = "P-Wellen sind Transversalwellen, S-Wellen sind Longitudinalwellen; beide breiten sich im Erdkern aus",
        answerB = "P-Wellen sind Kompressionswellen (Longitudinalwellen), die sich in Festkörpern, Flüssigkeiten und Gasen ausbreiten; S-Wellen sind Scherwellen (Transversalwellen), die nur in Festkörpern propagieren",
        answerC = "P-Wellen entstehen nur bei flachen Erdbeben; S-Wellen charakteristisch für tiefe Herdbeben",
        answerD = "P- und S-Wellen unterscheiden sich nur in der Frequenz, nicht im Wellentyp",
        correctAnswer = 1, // B
        explanation = "P-Wellen (Primärwellen) sind elastische Kompressionswellen: Partikel bewegen sich parallel zur Ausbreitungsrichtung. Sie breiten sich am schnellsten aus (~6-8 km/s in der Kruste) und durchdringen alle Medien. S-Wellen (Sekundärwellen) sind Scherwellen: Partikel schwingen senkrecht zur Ausbreitungsrichtung, können sich daher nicht im flüssigen äußeren Erdkern ausbreiten – was dessen Nachweis ermöglichte.",
        difficulty = 4,
        funFact = "Der äußere Erdkern wurde 1936 von der dänischen Seismologin Inge Lehmann durch die Analyse von Schatten-Zonen im S-Wellen-Muster entdeckt."
    ),

    // Question 37 — Geophysics: Earth's Magnetic Field
    Question(
        categoryId = 2,
        questionText = "Welcher Mechanismus erzeugt das Erdmagnetfeld nach dem Dynamo-Modell?",
        answerA = "Permanentmagnetisierung des festen inneren Kerns aus Eisen und Nickel",
        answerB = "Thermoelektrische Ströme an der Kern-Mantel-Grenze durch Temperaturgradient",
        answerC = "Konvektive Bewegungen elektrisch leitfähiger Eisenschmelze im äußeren flüssigen Kern, die durch Rotation und Corioliskraft zu einem selbst-erhaltendem MHD-Dynamo werden",
        answerD = "Piezoelektrische Effekte durch tektonische Druckspannungen im unteren Mantel",
        correctAnswer = 2, // C
        explanation = "Das Geodynamo-Modell erklärt das Erdfeld: Im flüssigen äußeren Kern (Fe-Ni-Schmelze) erzeugen konvektive Strömungen (thermische + kompositionelle Konvektion) elektrische Ströme. Durch Corioliskraft und Inertialkräfte entstehen systematische Strömungsstrukturen (Coriolis-dominierte Helizität), die das Magnetfeld verstärken und aufrechterhalten.",
        difficulty = 4,
        funFact = "Das Erdmagnetfeld ist nicht konstant: Es hat sich in der Erdgeschichte hunderte Male umgepolt. Die letzte Umpolung ('Brunhes-Matuyama') fand vor etwa 780.000 Jahren statt."
    ),

    // Question 38 — Geophysics: Mantle Convection
    Question(
        categoryId = 2,
        questionText = "Was beschreibt das 'Wilson-Zyklus' in der Plattentektonik?",
        answerA = "Die regelmäßige Umkehrung des Erdmagnetfelds alle ~200.000 Jahre durch Mantelkonvektion",
        answerB = "Den vollständigen Öffnungs- und Schließungszyklus eines Ozeanbeckens: Rift → Ozean → Subduktion → Kollision, mit einer Periodizität von ~500 Ma",
        answerC = "Die periodische Vereinigung aller Kontinente zu einem Superkontinent alle ~450 Ma durch Mantelkonvektion",
        answerD = "Die thermische Abkühlung der ozeanischen Lithosphäre als Funktion ihres Alters gemäß einem Wurzel-Gesetz",
        correctAnswer = 1, // B
        explanation = "Der Wilson-Zyklus (J. Tuzo Wilson, 1966) beschreibt den Lebenszyklus von Ozeanen: 1) Kontinentales Rift (Embryonalstadium), 2) Junger Ozean (z.B. Rotes Meer), 3) Reifer Ozean (Atlantik), 4) Schrumpfender Ozean mit Subduktion (Pazifik), 5) Schließung mit Kollisionsorogen (Himalaya = geschlossener Tethys-Ozean). Periodizität ~500 Ma (Superkontinent-Zyklus).",
        difficulty = 4,
        funFact = null
    ),

    // Question 39 — Geophysics: Isostasy
    Question(
        categoryId = 2,
        questionText = "Was beschreibt das Konzept der isostatischen Ausgleichsbewegung (Postglazialer Rebound)?",
        answerA = "Die thermische Ausdehnung des Ozeanbodens nach einer Eiszeit durch Mantelerwärmung",
        answerB = "Das langsame Aufsteigen (Hebung) der Erdkruste nach dem Abschmelzen von Gletschern, da sich der viskose Mantel dem erneuten isostatischen Gleichgewicht annähert",
        answerC = "Die Absenkung der Küstenlinien durch das Gewicht von ansteigendem Meeresspiegeln",
        answerD = "Die Erosion von Gebirgen führt zur gleichmäßigen Absenkung der Kruste durch verminderte Auflast",
        correctAnswer = 1, // B
        explanation = "Isostatischer Rebound (Gletscherrebound): Während der Eiszeit wurde die Kruste durch das Gewicht der Eisschilde (~3 km Eis) um bis zu 700 m eingedrückt. Nach dem Abschmelzen (~12.000 Jahre) hebt sich die Kruste wieder. In Skandinavien beträgt die aktuelle Hebungsrate noch ~8 mm/Jahr. Die Zeitkonstante (~3.000-10.000 Jahre) liefert Informationen über die Mantelsviskosität.",
        difficulty = 4,
        funFact = "Fennoskandien (Skandinavien + Finnland) hat sich seit dem letzten Gletschermaximum bereits um ~500 m gehoben. In weiteren 10.000 Jahren werden es ~200 m mehr sein."
    ),

    // Question 40 — Geophysics: Paleomagnetism
    Question(
        categoryId = 2,
        questionText = "Welches paläomagnetische Phänomen lieferte den überzeugendsten Beweis für die Seafloor-Spreading-Hypothese von Hess?",
        answerA = "Die dipolare Struktur des Erdfelds mit magnetischen Polen nahe den geografischen Polen",
        answerB = "Symmetrische, parallel zu mittelozeanischen Rücken angeordnete magnetische Anomalie-Streifen (Vine-Matthews-Morley-Hypothese)",
        answerC = "Die Abnahme der magnetischen Inklination mit wachsendem Abstand vom Äquator",
        answerD = "Paläomagnetische Poldaten zeigen, dass alle Kontinente einst am Südpol lagen",
        correctAnswer = 1, // B
        explanation = "Fred Vine, Drummond Matthews und Lawrence Morley zeigten 1963 unabhängig: Wenn neuer Ozeanboden am Mittelozeanischen Rücken aus Magma entsteht, nimmt er die aktuelle Magnetfeldrichtung an. Wechselnde Feld-Polaritäten erzeugen symmetrische Streifen beiderseits des Rückens. Diese Streifen stimmten genau mit der zeitlichen Polumkehrungschronologie überein – direkter Beweis für Seafloor Spreading.",
        difficulty = 4,
        funFact = null
    ),

    // Question 41 — Advanced Thermodynamics: Gibbs Free Energy
    Question(
        categoryId = 2,
        questionText = "Was sagt das Vorzeichen der freien Reaktionsenthalpie (ΔG) über eine chemische Reaktion aus?",
        answerA = "ΔG < 0: endotherm, nicht spontan; ΔG > 0: exotherm, spontan",
        answerB = "ΔG < 0: Reaktion läuft spontan ab (exergonisch) bei konstantem T und P; ΔG > 0: nicht spontan (endergonisch); ΔG = 0: Gleichgewicht",
        answerC = "ΔG bestimmt nur die Reaktionsgeschwindigkeit, nicht die thermodynamische Triebkraft",
        answerD = "ΔG = ΔH − TΔS; ein negativer ΔH-Wert ist immer ausreichend für Spontaneität",
        correctAnswer = 1, // B
        explanation = "ΔG = ΔH − TΔS ist das Kriterium für Spontaneität bei konstantem T und P: ΔG < 0 (exergonisch) → spontan; ΔG > 0 → nicht spontan; ΔG = 0 → Gleichgewicht. Dies kombiniert Enthalpie (ΔH, Wärmetönung) und Entropie (ΔS, Unordnung). Eine Reaktion kann auch bei ΔH > 0 spontan sein, wenn TΔS groß genug ist.",
        difficulty = 4,
        funFact = "Die ATP-Hydrolyse (ΔG° ≈ -30,5 kJ/mol) treibt in lebenden Zellen hunderte endergonischer Reaktionen an – sie ist die universelle Energiewährung des Lebens."
    ),

    // Question 42 — Advanced Thermodynamics: Chemical Potential
    Question(
        categoryId = 2,
        questionText = "Was ist das chemische Potential μᵢ einer Komponente i in einem thermodynamischen System?",
        answerA = "Die Konzentration der Komponente multipliziert mit der Boltzmann-Konstante",
        answerB = "Die partielle molare freie Enthalpie: μᵢ = (∂G/∂nᵢ)_{T,P,nⱼ≠ᵢ} – die Änderung der freien Energie beim Hinzufügen eines infinitesimalen mol von i",
        answerC = "Die Aktivierungsenergie für die chemische Reaktion der Komponente",
        answerD = "Der Dampfdruck der reinen Substanz bei Standardbedingungen",
        correctAnswer = 1, // B
        explanation = "Das chemische Potential μᵢ ist die partielle molare Gibbs-Energie: μᵢ = (∂G/∂nᵢ)_{T,P,nⱼ}. Es bestimmt das Gleichgewicht: Materie fließt von hohem zu niedrigem chemischen Potential, bis μᵢ in allen Phasen gleich ist. Es koppelt Thermodynamik mit Chemie, Elektrochemie (Nernst-Gleichung) und Membrantransport (Elektrochemisches Potential).",
        difficulty = 4,
        funFact = null
    ),

    // Question 43 — Advanced Thermodynamics: Statistical Mechanics
    Question(
        categoryId = 2,
        questionText = "Welche Aussage beschreibt den Zweiten Hauptsatz der Thermodynamik in Boltzmanns statistischer Interpretation?",
        answerA = "In einem abgeschlossenen System kann die Entropie nur abnehmen, da die Energie erhalten bleibt",
        answerB = "Die Entropie S = k_B ln(Ω) ist proportional zum Logarithmus der Anzahl der zugänglichen Mikrozustände Ω; spontane Prozesse entwickeln sich zu makroskopischen Zuständen mit maximaler Anzahl an Mikrozuständen",
        answerC = "Wärme fließt von kalt nach warm, wenn die statistische Wahrscheinlichkeit dafür größer als 50% ist",
        answerD = "Die freie Energie ist stets proportional zur absoluten Temperatur und Teilchenzahl",
        correctAnswer = 1, // B
        explanation = "Boltzmanns Grabinschrift S = k_B · ln(Ω) verbindet Makrothermodynamik mit Statistik: Die Entropie misst die Anzahl der zugänglichen Mikrozustände Ω (Multiplizität) des Makrozustands. Spontane Prozesse entwickeln sich zu Zuständen höherer Entropie, weil diese schlicht mehr Mikrozustände haben – es ist wahrscheinlicher, nicht ein Naturgesetz im klassischen Sinn.",
        difficulty = 4,
        funFact = "Ludwig Boltzmanns Gleichung S = k·log W ist auf seinem Grabstein in Wien eingraviert – eine der schönsten Gleichungen der Physik."
    ),

    // Question 44 — Advanced Thermodynamics: Boltzmann Distribution
    Question(
        categoryId = 2,
        questionText = "Was beschreibt die Boltzmann-Verteilung für ein Ensemble von Teilchen im thermischen Gleichgewicht?",
        answerA = "Alle Energieniveaus sind bei hoher Temperatur gleichmäßig besetzt",
        answerB = "Die Besetzungswahrscheinlichkeit eines Zustands mit Energie Eᵢ ist proportional zu exp(−Eᵢ/k_BT); Zustände niedrigerer Energie sind bevorzugt besetzt",
        answerC = "Fermionen folgen der Boltzmann-Verteilung bei allen Temperaturen",
        answerD = "Die mittlere kinetische Energie jedes Teilchens ist exakt k_BT, unabhängig von der Temperatur",
        correctAnswer = 1, // B
        explanation = "Die Boltzmann-Verteilung P(Eᵢ) ∝ exp(−Eᵢ/k_BT) = exp(−Eᵢ/k_BT)/Z gilt für klassische (nicht-entartete) Systeme. Der Nenner Z = Σ exp(−Eᵢ/k_BT) ist die Zustandssumme. Bei T→0 ist nur der Grundzustand besetzt; bei T→∞ nähern sich alle Besetzungswahrscheinlichkeiten einander an. Fermionen und Bosonen folgen bei niedrigen Temperaturen abweichenden Statistiken (Fermi-Dirac, Bose-Einstein).",
        difficulty = 4,
        funFact = null
    ),

    // Question 45 — Advanced Thermodynamics: Phase Equilibria
    Question(
        categoryId = 2,
        questionText = "Was sagt die Gibbssche Phasenregel F = C − P + 2 über die Freiheitsgrade eines Systems aus?",
        answerA = "F ist die Anzahl der Phasen; C die Komponenten; P die Drücke – die Gleichung beschreibt Druckänderungen",
        answerB = "F (Freiheitsgrade) = Anzahl der Komponenten C minus Anzahl der Phasen P plus 2; am Tripelpunkt (P=3, C=1) gibt es F=0 Freiheitsgrade, d.h. Temperatur und Druck sind fest",
        answerC = "Die Gleichung gilt nur für ideale Gase und nicht für kondensierte Phasen",
        answerD = "F beschreibt die Anzahl der möglichen chemischen Reaktionen zwischen den Phasen",
        correctAnswer = 1, // B
        explanation = "Gibbs' Phasenregel F = C − P + 2: C = Anzahl unabhängiger Komponenten, P = Anzahl Phasen im Gleichgewicht, F = Freiheitsgrade (intensive Variablen T, p, Zusammensetzungen). Für reines Wasser (C=1): Einphasig (P=1): F=2 (T und p frei variierbar); Zweiphasig: F=1 (z.B. Siedekurve); Tripelpunkt (P=3): F=0 (einzig möglicher Zustand bei 273,16 K, 611,7 Pa).",
        difficulty = 4,
        funFact = "Der Tripelpunkt des Wassers (273,16 K) definierte bis 2019 das Kelvin in der internationalen Einheitensystem-Definition."
    ),

    // Question 46 — Virology: Viral Replication
    Question(
        categoryId = 2,
        questionText = "Welche Replikationsstrategie verfolgen (+)-Strang-RNA-Viren wie SARS-CoV-2?",
        answerA = "Das Virusgenom dient direkt als mRNA und wird von ribosomalen Komplexen übersetzt; eine RNA-abhängige RNA-Polymerase (RdRp) synthetisiert den (−)-Strang als Replikationsintermediat",
        answerB = "Das Virusgenom wird zunächst in DNA umgeschrieben (Reverse Transkription) und ins Wirtsgenom integriert",
        answerC = "Das (+)-Strang-Genom muss zuerst in einen (−)-Strang transkribiert werden, bevor Translation stattfinden kann",
        answerD = "Replikation erfolgt im Zellkern durch wirtseigene DNA-Polymerase nach Konversion zu dsDNA",
        correctAnswer = 0, // A
        explanation = "Bei (+)-Strang-RNA-Viren (Picornaviren, Coronaviren, Flaviviren) ist das Genom direkt als mRNA funktional und wird sofort nach Eintritt von Ribosomen translatiert. Das erste translatierte Polyprotein enthält die RdRp (nsp12 bei Coronaviren), die den komplementären (−)-Strang als Matrize für neue (+)-Stränge synthetisiert. Der Replikationszyklus findet im Zytoplasma statt.",
        difficulty = 4,
        funFact = "SARS-CoV-2 hat mit ~30.000 Nukleotiden eines der größten RNA-Genome unter allen RNA-Viren. Seine Proofreading-Funktion (nsp14-Exonuklease) macht es resistenter gegen Mutationen als typische RNA-Viren."
    ),

    // Question 47 — Virology: Bacteriophages
    Question(
        categoryId = 2,
        questionText = "Was unterscheidet den lytischen vom lysogenen Zyklus bei Bakteriophagen?",
        answerA = "Im lytischen Zyklus wird die Wirtszelle sofort lysiert und neue Phagen freigesetzt; im lysogenen Zyklus integriert die Phagen-DNA als Prophage ins Wirtsgenom und wird bei Zellteilung weitergegeben",
        answerB = "Lytisch betrifft nur RNA-Phagen; lysogen nur DNA-Phagen",
        answerC = "Im lysogenen Zyklus werden 10× mehr Phagen produziert als im lytischen Zyklus",
        answerD = "Lytisch findet im Zytoplasma statt; lysogen im Zellkern des Wirtsbakteriums",
        correctAnswer = 0, // A
        explanation = "Lytischer Zyklus: Phage infiziert → übernimmt Zellmaschinerie → repliziert → lysiert die Wirtszelle → setzt 100-200 neue Phagen frei. Lysogener Zyklus: Phagen-DNA integriert ins Bakteriengenom als Prophage (z.B. Phage λ durch Integrase/att-Stellen). Prophage wird bei Zellteilung weitervererbt. Induktion (z.B. durch DNA-Schaden/SOS-Antwort) schaltet zurück zum lytischen Zyklus.",
        difficulty = 4,
        funFact = "Lysogene Konversion kann Bakterien virulenter machen: Das Diphtherie-Toxin, das Cholera-Toxin und Botulinum-Toxin werden durch lysogene Phagen in ihre Wirtsbakterien eingebracht."
    ),

    // Question 48 — Virology: Antibiotic Resistance
    Question(
        categoryId = 2,
        questionText = "Welcher molekulare Mechanismus verleiht Bakterien Resistenz gegen Beta-Laktam-Antibiotika (Penicilline, Cephalosporine)?",
        answerA = "Veränderung der Membranpermeabilität durch Reduktion der Porin-Expression",
        answerB = "Produktion von Beta-Laktamasen, die den Beta-Laktam-Ring hydrolytisch spalten und das Antibiotikum inaktivieren",
        answerC = "Überproduktion von Penicillin-bindenden Proteinen (PBPs), die das Antibiotikum titulieren",
        answerD = "Aktiver Efflux des Antibiotikums durch RND-Transporter aus der Zelle",
        correctAnswer = 1, // B
        explanation = "Beta-Laktamasen sind Enzyme, die den Beta-Laktam-Ring (den reaktiven Teil von Penicillin/Cephalosporinen) durch Hydrolyse öffnen und das Antibiotikum dadurch inaktivieren. MRSA (Methicillin-resistenter S. aureus) nutzt dagegen einen anderen Mechanismus: ein modifiziertes PBP2a (kodiert durch mecA), das kaum an Beta-Laktame bindet. Carbapenem-resistente Gram-negative nutzen oft Karbapenem-spaltende Beta-Laktamasen (z.B. NDM-1).",
        difficulty = 4,
        funFact = "Beta-Laktamasen existierten bereits 2 Milliarden Jahre vor der Entdeckung von Penicillin – sie entstanden ursprünglich nicht als Resistenzmechanismus, sondern als Enzyme für andere Zwecke."
    ),

    // Question 49 — Virology: Microbiome
    Question(
        categoryId = 2,
        questionText = "Was versteht man unter der 'Dysbiose' des intestinalen Mikrobioms und welche Erkrankungen sind damit assoziiert?",
        answerA = "Eine genetische Mutation der Darmflora-Zusammensetzung durch horizontalen Gentransfer",
        answerB = "Ein gestörtes Gleichgewicht der Mikrobiomzusammensetzung (veränderte Diversität/Zusammensetzung), assoziiert mit entzündlichen Darmerkrankungen (CED), Adipositas, metabolischem Syndrom und neurologischen Erkrankungen (Darm-Hirn-Achse)",
        answerC = "Die vollständige Elimination pathogener Bakterien durch das Immunsystem nach Antibiotika-Gabe",
        answerD = "Die parasitäre Infektion des Darms durch Protozoen mit konsekutiver Verdrängung der normalen Bakterienflora",
        correctAnswer = 1, // B
        explanation = "Dysbiose bezeichnet ein gestörtes Gleichgewicht im Mikrobiom: verringerte Diversität, Verschiebung zu ungünstigen Taxa (z.B. mehr Firmicutes relativ zu Bacteroidetes bei Adipositas), verminderte Produktion kurzkettiger Fettsäuren (SCFA). Assoziiert mit Morbus Crohn, Colitis ulcerosa, Typ-2-Diabetes, Adipositas, und über die Darm-Hirn-Achse auch mit Depression, Autismus-Spektrum-Störung und Parkinson.",
        difficulty = 4,
        funFact = "Das menschliche Mikrobiom enthält ~3,8×10¹³ Bakterienzellen – etwa gleich viele wie menschliche Körperzellen. Ihr kollektives Genom ('Metagenom') kodiert ~3 Millionen Gene gegenüber ~20.000 menschlichen Genen."
    ),

    // Question 50 — Virology: Prion Diseases
    Question(
        categoryId = 2,
        questionText = "Was ist das molekulare Kennzeichen von Prionen und warum sind Prionenerkrankungen (TSE) so schwer zu bekämpfen?",
        answerA = "Prionen sind atypische DNA-Viren ohne Proteinhülle; ihre Nukleinsäure ist extrem resistent gegen UV-Strahlung",
        answerB = "Prionen sind fehlgefaltete Proteine (PrPSc) die normale Isoformen (PrPC) in die pathologische Konformation überführen; keine Nukleinsäure, extrem hitze- und desinfektionsmittelresistent",
        answerC = "Prionen sind RNA-Parasiten ähnlich Viroiden, die die Proteinfaltungsmaschinerie kapern und zelluläres PrP abbauen",
        answerD = "Prionen sind Lipoproteinkomplexe aus fehlerhaft prozessierten GPI-verankerten Membranproteinen und oxidiertem Cholesterin",
        correctAnswer = 1, // B
        explanation = "Prionen (Stanley Prusiner, Nobelpreis 1997) sind infektiöse Proteine: Die pathologische Isoform PrPSc (reich an Beta-Faltblättern) induziert bei Kontakt mit normalem PrPC (alphahelix-reich) eine Konformationsänderung – eine autokatalytische Kettenreaktion. Da keine Nukleinsäure vorhanden ist, helfen keine Antivirantien; PrPSc ist extrem resistent gegen Hitze, Proteasen, ionisierende Strahlung und übliche Desinfektionsmittel.",
        difficulty = 4,
        funFact = "BSE (Rinderwahn) und die menschliche Variante vCJD sind Prionenerkrankungen. Der BSE-Ausbruch in den 1990ern in Großbritannien infizierte über 180.000 Rinder. vCJD erkrankte bisher ~230 Menschen weltweit."
    )
)
