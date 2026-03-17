package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun scienceQuestionsExpert3(): List<Question> = listOf(

    // Question 1 — Photonics & Quantum Optics: Squeezed States
    Question(
        categoryId = 2,
        questionText = "Was beschreibt ein 'gequetschter Zustand' (squeezed state) des Lichts in der Quantenoptik?",
        answerA = "Ein Zustand, bei dem die Unschärfe einer Quadraturkomponente unterhalb der Heisenberg'schen Vakuumfluktuation liegt, auf Kosten der konjugierten Quadratur",
        answerB = "Ein kohärenter Zustand mit reduzierter Photonenzahl",
        answerC = "Ein Zustand, bei dem beide Quadraturkomponenten gleichzeitig beliebig genau bekannt sind",
        answerD = "Ein polarisierter Zustand mit maximaler Kohärenzlänge",
        correctAnswer = 0, // A
        explanation = "Gequetschte Zustände erfüllen die Heisenberg-Unschärferelation, verteilen die Unsicherheit aber asymmetrisch: Eine Quadraturkomponente (z.B. Amplitude) wird unter den Vakuumfluktuationen 'gequetscht', während die konjugierte (z.B. Phase) entsprechend vergrößerte Fluktuation zeigt. Das Produkt bleibt ≥ ħ/2.",
        difficulty = 4,
        funFact = "Gequetschtes Licht wird bei LIGO eingesetzt, um die Messgenauigkeit bei der Gravitationswellendetektion über die Standard-Quantengrenze hinaus zu verbessern."
    ),

    // Question 2 — Photonics & Quantum Optics: Photonic Crystals
    Question(
        categoryId = 2,
        questionText = "Welcher physikalische Mechanismus erzeugt eine photonische Bandlücke in einem photonischen Kristall?",
        answerA = "Absorption von Photonen durch Gitteratome",
        answerB = "Bragg-Reflexion an der periodischen dielektrischen Struktur führt zu destruktiver Interferenz für bestimmte Frequenzen",
        answerC = "Spontane Emission wird durch das Pauli-Prinzip unterdrückt",
        answerD = "Nichtlineare Suszeptibilität verhindert die Wellenausbreitung",
        correctAnswer = 1, // B
        explanation = "Photonische Kristalle sind periodische Strukturen mit variierendem Brechungsindex. Analog zur elektronischen Bandstruktur in Festkörpern entsteht durch Bragg-Interferenz an der periodischen Dielektrizitätskonstante eine photonische Bandlücke: Licht bestimmter Wellenlängen kann sich nicht ausbreiten.",
        difficulty = 4,
        funFact = null
    ),

    // Question 3 — Photonics & Quantum Optics: Quantum Dots in Optics
    Question(
        categoryId = 2,
        questionText = "Warum eignen sich Quantenpunkte als 'Ein-Photon-Quellen' (single-photon emitters) besser als klassische Farbstoffe?",
        answerA = "Sie emittieren Licht durch thermische Strahlung und sind daher stabiler",
        answerB = "Antibunching durch den zweistufigen Charakter des Quantenpunkt-Exzitons garantiert, dass nach einer Anregung genau ein Photon emittiert wird, bevor die nächste Absorption möglich ist",
        answerC = "Ihr großes Dipolmoment ermöglicht kohärente Mehrephotonenemission",
        answerD = "Sie haben kein Photobleichen, weil Quantenpunkte ausschließlich Stokes-Verschiebung zeigen",
        correctAnswer = 1, // B
        explanation = "Ein Quantenpunkt kann nach Anregung nur ein Elektron-Loch-Paar (Exziton) beherbergen. Nach der Emission muss das Exziton vollständig rekombinieren, bevor eine neue Absorption stattfinden kann. Dieses Antibunching-Verhalten (g²(0) < 0,5) macht Quantenpunkte zu nahezu idealen Einzelphotonenquellen.",
        difficulty = 4,
        funFact = "Einzelphotonenquellen sind die Grundlage für quantenkryptografische Protokolle wie BB84, bei denen die Nicht-Kopierbarkeit einzelner Photonen die Sicherheit garantiert."
    ),

    // Question 4 — Photonics & Quantum Optics: Optical Tweezers
    Question(
        categoryId = 2,
        questionText = "Durch welchen physikalischen Effekt üben optische Pinzetten (optical tweezers) eine Kraft auf dielektrische Partikel aus?",
        answerA = "Durch den Strahlungsdruck der absorbierten Photonen auf die Partikeloberfläche",
        answerB = "Durch den Gradientenanteil der elektromagnetischen Kraft, der ein Partikel mit höherem Brechungsindex zum Intensitätsmaximum des Laserstrahls zieht",
        answerC = "Durch piezoelektrische Wechselwirkung zwischen Licht und Materie",
        answerD = "Durch stimulierte Raman-Streuung an der Partikeloberfläche",
        correctAnswer = 1, // B
        explanation = "In optischen Pinzetten überwiegt die Gradientenkraft: Der inhomogene Laserintensitätsgradient induziert ein Dipolmoment im Partikel, das in Richtung höherer Feldstärke (Fokus) gezogen wird. Der Streudruck (Strahlungsdruck) wirkt entlang der Ausbreitungsrichtung und muss durch die Gradientenkraft überwunden werden.",
        difficulty = 4,
        funFact = "Arthur Ashkin erhielt 2018 den Physiknobelpreis für die Entwicklung optischer Pinzetten, mit denen er lebende Bakterien einfangen und manipulieren konnte."
    ),

    // Question 5 — Photonics & Quantum Optics: Entangled Photon Pairs
    Question(
        categoryId = 2,
        questionText = "Welcher Prozess wird standardmäßig zur Erzeugung verschränkter Photonenpaare genutzt?",
        answerA = "Stimulierte Emission in einem Zwei-Photonen-Laser",
        answerB = "Parametrische Abwärtskonversion (SPDC) in einem nichtlinearen Kristall, bei der ein Pump-Photon in zwei Signal- und Idler-Photonen zerfällt",
        answerC = "Rayleigh-Streuung an kalten Atomen im Bose-Einstein-Kondensat",
        answerD = "Vier-Wellen-Mischung in einem linearen Resonator",
        correctAnswer = 1, // B
        explanation = "Bei der spontanen parametrischen Abwärtskonversion (SPDC) wird ein Photon hoher Energie in einem nichtlinearen Kristall (z.B. BBO) in zwei Photonen niedrigerer Energie aufgespalten. Energie- und Impulserhaltung erzwingen starke Korrelationen zwischen Signal und Idler, was zu Polarisations- oder Impuls-Verschränkung führt.",
        difficulty = 4,
        funFact = null
    ),

    // Question 6 — Proteomics & Mass Spectrometry: MALDI-TOF
    Question(
        categoryId = 2,
        questionText = "Welches Prinzip liegt der MALDI-TOF-Massenspektrometrie zugrunde?",
        answerA = "Proteine werden durch Elektrospray ionisiert und nach ihrer Driftgeschwindigkeit im Gasraum getrennt",
        answerB = "Eine UV-absorbierende Matrix ko-kristallisiert mit dem Analyten; Laserpulse desorbiern und ionisieren ihn sanft; die Flugzeit im Vakuum ergibt das Masse-zu-Ladungs-Verhältnis",
        answerC = "Ionen werden in einem Quadrupolfeld nach ihrer Resonanzfrequenz sortiert",
        answerD = "Proteine werden elektrophoretisch getrennt und anschließend mit einem Flammenionisationsdetektor detektiert",
        correctAnswer = 1, // B
        explanation = "MALDI-TOF kombiniert zwei Techniken: Matrix-unterstützte Laser-Desorption/Ionisation erzeugt große, intakte Ionen; Time-of-Flight trennt sie nach der Flugzeit t ∝ √(m/z). Da ein Laser pro Puls ionisiert, entstehen hauptsächlich einfach geladene Ionen – ideal für Proteine bis ~500 kDa.",
        difficulty = 4,
        funFact = "MALDI-TOF revolutionierte die klinische Mikrobiologie: Das System MALDI Biotyper identifiziert Bakterienarten innerhalb von Minuten aus Kolonieproteinfingerprinten, was früher Tage dauerte."
    ),

    // Question 7 — Proteomics & Mass Spectrometry: Tandem MS
    Question(
        categoryId = 2,
        questionText = "Was ermöglicht Tandem-Massenspektrometrie (MS/MS) im Vergleich zur einfachen MS bei der Peptidsequenzierung?",
        answerA = "Eine höhere Massenauflösung durch längere Flugstrecken",
        answerB = "Die gezielte Fragmentierung ausgewählter Vorläuferionen liefert sequenzspezifische b- und y-Ionen-Serien, aus denen die Aminosäuresequenz ableitbar ist",
        answerC = "Die gleichzeitige Ionisierung aller Peptide einer Probe ohne chromatografische Vortrennung",
        answerD = "Die Bestimmung von Disulfidbrückenpositionen durch Elektronentransfer",
        correctAnswer = 1, // B
        explanation = "In MS/MS wird ein Vorläuferion (erstes MS) isoliert und durch kollisionsinduzierte Dissoziation (CID) fragmentiert. Das zweite MS analysiert die Fragmente. Peptidbindungsbrüche erzeugen charakteristische b-Ionen (N-terminal) und y-Ionen (C-terminal), deren Massendifferenzen direkt den Aminosäuren entsprechen.",
        difficulty = 4,
        funFact = null
    ),

    // Question 8 — Proteomics & Mass Spectrometry: TMT Quantification
    Question(
        categoryId = 2,
        questionText = "Wie funktioniert die quantitative Proteomik mit isobaren Tandem-Massen-Tags (TMT)?",
        answerA = "Verschiedene Proben werden mit identisch schweren Reagenzien markiert, die bei MS/MS-Fragmentierung unterschiedliche Reporter-Ionen freisetzen, deren Intensität die relative Proteinmenge widerspiegelt",
        answerB = "Proteine werden metabolisch mit schweren Isotopen (¹³C, ¹⁵N) in vivo markiert und per Massenverschiebung quantifiziert",
        answerC = "Die Ionisierungseffizienz dient als direkter Maßstab für die Konzentration",
        answerD = "TMT-Reagenzien erhöhen die Löslichkeit von Membranproteinen für die Ionisierung",
        correctAnswer = 0, // A
        explanation = "TMT-Reagenzien sind isobar (gleiche Gesamtmasse), enthalten aber unterschiedlich schwere Reporter-Gruppen (z.B. ¹²C/¹³C-Verteilung). Alle markierten Proben werden gemischt und gemeinsam analysiert; erst die MS/MS-Fragmentierung trennt die Reporter-Ionen (126–134 Da), was Multiplexing von bis zu 18 Proben ermöglicht.",
        difficulty = 4,
        funFact = "Mit TMT-18plex kann man heute 18 Bedingungen in einem einzigen Experiment vergleichen – das entspricht einer 18-fachen Effizienzsteigerung gegenüber klassischen Label-freien Ansätzen."
    ),

    // Question 9 — Proteomics & Mass Spectrometry: Top-Down vs Bottom-Up
    Question(
        categoryId = 2,
        questionText = "Was ist der wesentliche Unterschied zwischen 'Top-Down'- und 'Bottom-Up'-Proteomik?",
        answerA = "Top-Down analysiert intakte Proteine direkt per MS; Bottom-Up verdaut Proteine enzymatisch zu Peptiden, bevor die MS-Analyse erfolgt",
        answerB = "Top-Down nutzt MALDI, Bottom-Up ausschließlich ESI",
        answerC = "Top-Down ist auf kleine Proteine (<10 kDa) beschränkt, Bottom-Up auf große Proteine",
        answerD = "Top-Down misst Proteine in lebenden Zellen, Bottom-Up in Lysaten",
        correctAnswer = 0, // A
        explanation = "Bottom-Up ist die vorherrschende Methode: Trypsin spaltet Proteine zu 0,5–3 kDa Peptiden, die chromatografisch getrennt und per LC-MS/MS identifiziert werden. Top-Down analysiert intakte Proteine und liefert vollständige Sequenzinformationen inklusive Isoformen, erfordert aber aufwändigere Ionisierung und Fragmentierung.",
        difficulty = 4,
        funFact = null
    ),

    // Question 10 — Proteomics & Mass Spectrometry: Phosphoproteomics
    Question(
        categoryId = 2,
        questionText = "Warum erfordert Phosphoproteomik eine spezielle Anreicherungsstrategie vor der MS-Analyse?",
        answerA = "Weil Phosphopeptide durch Trypsin nicht gespalten werden",
        answerB = "Weil phosphorylierte Peptide im Gemisch stark unterrepräsentiert sind und schlechter ionisieren; Anreicherung via IMAC oder TiO₂ erhöht ihre Detektierbarkeit um Größenordnungen",
        answerC = "Weil Phosphatgruppen die Peptide für ESI zu groß machen",
        answerD = "Weil Phosphopeptide instabil sind und nur bei −196 °C gemessen werden können",
        correctAnswer = 1, // B
        explanation = "Phosphorylierung ist eine seltene, dynamische PTM: Nur 1–5% aller Peptide in einem Lysat sind phosphoryliert. Immobilisierte Metallaffinitätschromatographie (IMAC mit Fe³⁺/Ti⁴⁺) oder TiO₂-basierte Anreicherung nutzt die negative Ladung der Phosphatgruppen für selektive Bindung, Waschen und Elution.",
        difficulty = 4,
        funFact = "Das menschliche Phosphoproteom umfasst schätzungsweise über 100.000 Phosphorylierungsstellen an mehr als 20.000 Proteinen – ein wesentlicher Teil der zellulären Signalregulation."
    ),

    // Question 11 — Advanced Evolutionary Genetics: Coalescent Theory
    Question(
        categoryId = 2,
        questionText = "Was ist der 'most recent common ancestor' (MRCA) im Kontext der Koaleszenz-Theorie?",
        answerA = "Das älteste bekannte Fossil einer Artengruppe",
        answerB = "Der Zeitpunkt in der Vergangenheit, zu dem alle Gene einer heutigen Stichprobe auf einen einzigen Vorfahren zurückgehen, rückwärts durch die Genealogie verfolgt",
        answerC = "Das Individuum mit der höchsten Fitness in einer Population",
        answerD = "Die Schwestergruppe eines monophyletischen Taxons im phylogenetischen Baum",
        correctAnswer = 1, // B
        explanation = "Koaleszenz-Theorie beschreibt, wie Genlinien rückwärts in der Zeit konvergieren ('koaleszieren'). In einer Population der Größe Ne koaleszieren zwei Linien mit einer Rate von 1/(2Ne) pro Generation. Die erwartete MRCA-Koaleszenzzeit für n Linien liegt bei etwa 4Ne (2Ne für diploide Organismen) Generationen.",
        difficulty = 4,
        funFact = "Die Koaleszenz-Theorie ermöglichte die Schätzung des 'mitochondrialen Eva' – des weiblichen Vorfahren aller lebenden Menschen vor etwa 150.000 Jahren – durch mtDNA-Sequenzvergleiche."
    ),

    // Question 12 — Advanced Evolutionary Genetics: Selective Sweeps
    Question(
        categoryId = 2,
        questionText = "Was charakterisiert einen 'selektiven Sweep' in einem Populationsgenom?",
        answerA = "Eine Region mit erhöhter Rekombinationsrate, die Allelkombinationen aufbricht",
        answerB = "Die rasche Ausbreitung eines positiv selektierten Allels, die benachbarte neutrale Varianten mitreißt und zu einer Region reduzierter Diversität ('hitch-hiking') führt",
        answerC = "Eine ausbalancierte Polymorphismus-Region mit überdurchschnittlicher Heterozygosität",
        answerD = "Eine Deletion in der Keimbahn, die durch genetische Drift fixiert wurde",
        correctAnswer = 1, // B
        explanation = "Wenn ein Allel durch positive Selektion in einer Population fixiert wird, werden benachbarte Loci auf demselben Chromosom mitgezogen (genetisches Hitch-hiking). Dies erzeugt ein charakteristisches Muster: reduzierte Nukleotiddiversität, verlängerte Haplotyp-Blöcke und ein spezifisches Muster im extended haplotype homozygosity (EHH)-Test.",
        difficulty = 4,
        funFact = null
    ),

    // Question 13 — Advanced Evolutionary Genetics: Neutral Theory
    Question(
        categoryId = 2,
        questionText = "Welche zentrale Vorhersage macht Kimuras Neutrale Theorie der molekularen Evolution?",
        answerA = "Die Rate molekularer Evolution ist proportional zur Populationsgröße",
        answerB = "Die meisten Substitutionen auf Molekülebene sind selektiv neutral; ihre Fixierungsrate entspricht der neutralen Mutationsrate μ und ist von der Populationsgröße unabhängig",
        answerC = "Positive Selektion ist der einzige Treiber molekularer Diversität",
        answerD = "Synonyme und nicht-synonyme Substitutionen treten mit gleicher Rate auf",
        correctAnswer = 1, // B
        explanation = "Kimura (1968) zeigte: Für ein neutrales Allel ist die Fixierungswahrscheinlichkeit 1/(2N) und die Rate neuer Mutationen 2Nμ, so dass die Substitutionsrate genau μ beträgt – unabhängig von N. Dies erklärt die bemerkenswert konstante Substitutionsrate ('molekulare Uhr') auf Proteinebene.",
        difficulty = 4,
        funFact = "Die neutrale Theorie wurde zunächst kontrovers diskutiert, gilt heute aber als Null-Hypothese der Molekularevolution: Abweichungen von der Neutralität sind der Beweis für Selektion."
    ),

    // Question 14 — Advanced Evolutionary Genetics: Phylogenomics
    Question(
        categoryId = 2,
        questionText = "Was ist 'Incomplete Lineage Sorting' (ILS) und welches Problem verursacht es in der Phylogenomik?",
        answerA = "Das Phänomen, dass unterschiedliche Gene bei kürzlich divergierten Arten unterschiedliche Topologien zeigen können, weil Allele aus gemeinsamen Vorfahren nicht vollständig sortiert wurden",
        answerB = "Ein Fehler bei der Sequenzassemblierung, der Paraloge als Orthologe fehlklassifiziert",
        answerC = "Die unvollständige Replikation repetitiver DNA-Regionen in kurzen Lesevorgängen",
        answerD = "Die selektive Retention bestimmter Chromosomen bei Polyploidisierungsereignissen",
        correctAnswer = 0, // A
        explanation = "ILS tritt auf, wenn Artspeziation schneller erfolgt als die Sortierung ancestraler Polymorphismen. Unterschiedliche Genloci können dann verschiedene Topologien zeigen (Diskordanz), die nicht die Artgeschichte widerspiegeln. Bei kurzen inneren Ästen im Artbaum kann ILS sogar dazu führen, dass die häufigste Gentopologie NICHT der Arttopologie entspricht.",
        difficulty = 4,
        funFact = null
    ),

    // Question 15 — Advanced Evolutionary Genetics: Horizontal Gene Transfer
    Question(
        categoryId = 2,
        questionText = "Über welchen Mechanismus erfolgt der horizontale Gentransfer bei Bakterien beim Prozess der Konjugation?",
        answerA = "Aufnahme freier DNA-Fragmente aus dem Medium durch Kompetenz-Proteine",
        answerB = "Übertragung eines Plasmids oder mobilisierbarer DNA durch direkten Zell-Zell-Kontakt über einen Pilus, vermittelt durch das Typ-IV-Sekretionssystem",
        answerC = "Injektion von DNA durch Bakteriophagen in eine Empfängerzelle",
        answerD = "Fusion zweier Bakterienzellen mit anschließendem Chromosomenaustausch",
        correctAnswer = 1, // B
        explanation = "Konjugation erfordert physischen Zellkontakt: Ein F-Pilus verbindet Donor (F⁺) und Empfänger (F⁻). Das Typ-IV-Sekretionssystem bildet einen Kanal, durch den einzelsträngige DNA (nach Rollkreisreplikation) übertragen wird. Antibiotikaresistenzgene auf Plasmiden verbreiten sich global durch diesen Mechanismus.",
        difficulty = 4,
        funFact = "Horizontaler Gentransfer hat im Laufe der Evolution schätzungsweise 20–30% des Genoms typischer Bakterienarten aus anderen Organismen eingebracht – er ist damit genauso wichtig wie vertikale Vererbung."
    ),

    // Question 16 — Plasma & Fusion Physics: Stellarator vs Tokamak
    Question(
        categoryId = 2,
        questionText = "Was ist der wesentliche strukturelle Unterschied zwischen einem Stellarator und einem Tokamak?",
        answerA = "Im Stellarator wird das Magnetfeld ausschließlich durch externe, gewendelte Spulen erzeugt; im Tokamak wird ein Teil des Einschlussfeldes durch einen induzierten toroidalen Plasmastrom erzeugt",
        answerB = "Ein Stellarator verwendet supraleitende Magnete, ein Tokamak konventionelle Kupfermagnete",
        answerC = "Der Stellarator nutzt inertiale Einschlusskonfinierung, der Tokamak magnetischen Einschluss",
        answerD = "Im Tokamak ist das Plasma zylindrisch, im Stellarator toroidal geformt",
        correctAnswer = 0, // A
        explanation = "Im Tokamak wird der für den Plasmaeinschluss nötige poloidale Feldanteil durch einen induzierten Plasmastrom (bis zu mehreren MA) erzeugt – ein inhärent gepulster Betrieb. Der Stellarator (z.B. Wendelstein 7-X) erzeugt das vollständige dreidimensionale Einschlussfeld durch gewendelte externe Spulen und kann im Prinzip stationär betrieben werden.",
        difficulty = 4,
        funFact = "Wendelstein 7-X in Greifswald ist der weltgrößte Stellarator. Seine 50 supraleitenden Nicht-planaren Spulen wurden mittels aufwändiger Computersimulationen optimiert."
    ),

    // Question 17 — Plasma & Fusion Physics: Plasma Confinement
    Question(
        categoryId = 2,
        questionText = "Was versteht man unter dem Grad der Einschlussqualität β (Beta) in einem magnetischen Einschlusssystem?",
        answerA = "Das Verhältnis von Plasma-Wärmedruck zu Magnetfelddruck: β = nkT / (B²/2μ₀)",
        answerB = "Die mittlere freie Weglänge der Ionen im magnetischen Einschluss",
        answerC = "Die Effizienz der Neutralstrahl-Heizung bezogen auf den Gesamtenergieinput",
        answerD = "Den Quotienten aus Fusionsleistung und Ohm'scher Verlustleistung",
        correctAnswer = 0, // A
        explanation = "β ist ein zentrales Maß der Plasmaeinschlusseffizienz: Für wirtschaftlich sinnvolle Fusion muss β möglichst hoch sein (mehr Plasmadruck pro Magnetfeldinvestition). Zu hohe β-Werte führen jedoch zu MHD-Instabilitäten (z.B. Balloning-Moden). Typische Tokamaks operieren bei β ≈ 2–5%.",
        difficulty = 4,
        funFact = null
    ),

    // Question 18 — Plasma & Fusion Physics: Lawson Criterion
    Question(
        categoryId = 2,
        questionText = "Was beschreibt das Lawson-Kriterium für magnetischen Fusionseinschluss?",
        answerA = "Die Mindesttemperatur, bei der die Fusionsquerschnitte die Strahlungsverluste übersteigen",
        answerB = "Eine Bedingung an das Produkt aus Plasmadichte n, Einschlusszeit τE und Temperatur T, ab der die Fusionsenergie die Heizenergie übersteigt: nτET ≥ 3×10²¹ m⁻³·s·keV",
        answerC = "Den maximalen Magnetfeldgradienten, der MHD-stabile Gleichgewichte erlaubt",
        answerD = "Die kritische Betriebstemperatur für D-T-Plasma bei der der Plasmadruck das Magnetfeld übersteigt",
        correctAnswer = 1, // B
        explanation = "John Lawson (1957) leitete die Bedingung ab, unter der ein Fusionsplasma mehr Energie erzeugt als zugeführt wird. Das Dreifachprodukt nτET ≥ 3×10²¹ m⁻³·s·keV (für D-T bei ~10 keV) definiert den Zündpunkt. Bisheriger Rekordhalter für Q>1 ist NIF (2022) mit inertialer Trägheitsfusion.",
        difficulty = 4,
        funFact = "2022 erzielte die National Ignition Facility (NIF) erstmals 'ignition': Die Fusionsenergie überstieg die gesamte zugeführte Laserenergie – ein historischer Meilenstein nach 60 Jahren Forschung."
    ),

    // Question 19 — Plasma & Fusion Physics: Inertial Confinement
    Question(
        categoryId = 2,
        questionText = "Wie funktioniert das Prinzip der inertial confinement fusion (ICF)?",
        answerA = "Ein DT-Pellet wird durch Laserpulse von außen komprimiert, bis die Trägheit des kollabierenden Materials kurzzeitig ausreicht, das Plasma bei Fusionsbedingungen zu halten",
        answerB = "Schwerer Wasserstoff wird in einem statischen Magnetfeldkäfig auf Fusionstemperatur erhitzt",
        answerC = "Ionenstrahlen neutralisieren das Plasma und verhindern die Expansion",
        answerD = "Ein supraleitender Zylinder komprimiert das Plasma durch Induktion auf Fusionsdichte",
        correctAnswer = 0, // A
        explanation = "Bei ICF trifft ein intensiver Laserstrahl (oder Röntgenstrahlung in einem Hohlraum) auf ein winziges Pellet (~2 mm) aus deuteriertem Tritium. Die außen ablatierte Schicht erzeugt einen reaktiven Druck, der das Pellet auf ~1000-fache Festkörperdichte komprimiert. Für Mikrosekunden ist die Trägheit ausreichend, um Fusionsbedingungen aufrechtzuhalten.",
        difficulty = 4,
        funFact = null
    ),

    // Question 20 — Plasma & Fusion Physics: ITER Design
    Question(
        categoryId = 2,
        questionText = "Welches Verstärkungsverhältnis Q plant ITER zu demonstrieren und was bedeutet es?",
        answerA = "Q = 2: Die Fusionsleistung ist doppelt so groß wie die zum Einschluss benötigte Kühlleistung",
        answerB = "Q ≥ 10: Die Fusionsleistung beträgt mindestens das Zehnfache der eingespeisten Heizleistung, bei einem Plasma-Gewinn von 500 MW aus 50 MW Heizung",
        answerC = "Q = 1: Energiegleichgewicht zwischen Fusion und externer Heizung",
        answerD = "Q ≥ 100: Vollständige Selbstzündung des Plasmas ohne externe Heizung",
        correctAnswer = 1, // B
        explanation = "ITER (International Thermonuclear Experimental Reactor) ist auf Q ≥ 10 ausgelegt: 50 MW Heizleistung sollen 500 MW Fusionsleistung erzeugen. Q = 1 wäre Energiegleichgewicht; Q = ∞ wäre vollständige Zündung ('Ignition'). ITER soll die Machbarkeit des Fusionsreaktors demonstrieren, ist aber noch kein Kraftwerk.",
        difficulty = 4,
        funFact = "ITER vereint 35 Nationen und ist mit einem Budget von über 20 Milliarden Euro das größte wissenschaftliche Kooperationsprojekt der Menschheitsgeschichte."
    ),

    // Question 21 — Advanced Materials: Metamaterials
    Question(
        categoryId = 2,
        questionText = "Welche außergewöhnliche optische Eigenschaft können Metamaterialien mit negativem Brechungsindex aufweisen?",
        answerA = "Vollständige Absorption elektromagnetischer Strahlung über alle Frequenzen",
        answerB = "Einen negativen Brechungsindex (n < 0), bei dem gebrochenes Licht auf die gleiche Seite der Normalen wie das einfallende Licht fällt – was perfekte Linsen ohne Beugungsgrenze ermöglicht",
        answerC = "Lumineszenz bei Raumtemperatur ohne externe Anregung",
        answerD = "Supraleitung bei Raumtemperatur durch photonische Bandlücken",
        correctAnswer = 1, // B
        explanation = "Metamaterialien mit n < 0 (negative Permittivität ε und Permeabilität μ gleichzeitig) brechen Licht in ungewöhnlicher Richtung. John Pendry zeigte 2000, dass ein solches Material als 'Superlense' fungieren kann, die auch evaneszente Felder verstärkt und damit die klassische Beugungsgrenze überwindet.",
        difficulty = 4,
        funFact = "Metamaterialien ermöglichen theoretisch Tarnkappen ('invisibility cloaks'): Durch räumlich strukturierte ε- und μ-Profile wird Licht um ein Objekt herumgeleitet, ohne es zu beugen oder zu reflektieren."
    ),

    // Question 22 — Advanced Materials: Perovskite Solar Cells
    Question(
        categoryId = 2,
        questionText = "Was erklärt den schnellen Anstieg der Effizienz von Perowskit-Solarzellen auf über 25% innerhalb weniger Jahre?",
        answerA = "Ihre intrinsische Leitfähigkeit durch metallische Bindungsanteile",
        answerB = "Eine hohe Defekttoleranz trotz lösungsbasierter Herstellung, kombiniert mit einem direkten und einstellbaren Bandabstand sowie hohen Ladungsträger-Diffusionslängen von Mikrometer-Skala",
        answerC = "Die vollständige Unterdrückung von Auger-Rekombination durch das Pb-Gitter",
        answerD = "Ihr extrem niedriger Brechungsindex, der die interne Totalreflexion minimiert",
        correctAnswer = 1, // B
        explanation = "Perowskit-Halbleiter (ABX₃, z.B. MAPbI₃) tolerieren eine hohe Defektdichte, weil Defektzustände meist nicht in der Bandlücke liegen. Durch die Zusammensetzung (A, B, X) ist der Bandabstand von ~1,2–2,3 eV einstellbar. Ladungsträger diffundieren über Mikrometer ohne Rekombination – trotz einfacher Nasschemie-Herstellung.",
        difficulty = 4,
        funFact = null
    ),

    // Question 23 — Advanced Materials: Shape Memory Alloys
    Question(
        categoryId = 2,
        questionText = "Welcher Mechanismus liegt dem Formgedächtnis-Effekt in NiTi-Legierungen (Nitinol) zugrunde?",
        answerA = "Plastische Verformung durch Versetzungsbewegung, die durch Erwärmen in die Ausgangsform zurückkehrt",
        answerB = "Eine martensitische Phasenumwandlung zwischen der Austenit-Phase (kubisch, hochtemperatur) und der Martensit-Phase (monoklin, tieftemperatur), die reversibel und thermoelastisch verläuft",
        answerC = "Piezoelektrische Kontraktion durch thermisch induzierte Ladungsumverteilung",
        answerD = "Superelastisches Kriechverhalten durch Gleitprozesse an Korngrenzen",
        correctAnswer = 1, // B
        explanation = "Beim Abkühlen unter die Martensittemperatur Ms bildet sich twinned martensite. Mechanische Belastung orientiert die Zwillingsdomänen (detwinning). Beim Erwärmen über die Austenittemperatur As kehrt das Material zur kubischen B2-Austenitstruktur zurück – mit exakt der ursprünglichen Form. Der Superelastizitätseffekt bei konstanter Temperatur folgt dem gleichen Phasenübergangsmechanismus.",
        difficulty = 4,
        funFact = "Nitinol-Stents entfalten sich im Körper durch Körperwärme automatisch auf die gewünschte Größe – ein klassisches Anwendungsbeispiel des Formgedächtnis-Effekts in der Medizin."
    ),

    // Question 24 — Advanced Materials: Aerogels
    Question(
        categoryId = 2,
        questionText = "Warum weisen Silika-Aerogele eine extrem niedrige Wärmeleitfähigkeit von ~0,015 W/(m·K) auf?",
        answerA = "Weil ihr metallisches Si-Netzwerk keine Phononen leitet",
        answerB = "Weil ihre dreidimensionale, hochporöse Nanostruktur (>90% Luftanteil) alle drei Wärmetransportmechanismen gleichzeitig minimiert: Feststoffleitung durch dünne Si-O-Stränge, Gasleitung durch Knudsen-Effekt in Nanoporen, und Strahlung durch das transparente Netzwerk",
        answerC = "Weil Suprakristallisationseffekte die Phonon-Dispersion aufheben",
        answerD = "Weil Aerogele supraleitend bei Raumtemperatur sind",
        correctAnswer = 1, // B
        explanation = "In Aerogelen ist der Porendurchmesser (~20 nm) kleiner als die mittlere freie Weglänge von Luftmolekülen (70 nm bei Normaldruck). Der Knudsen-Effekt reduziert die Gasleitung drastisch. Das feine Si-O₂-Netzwerk (Dichte ~0,1 g/cm³) minimiert Feststoffleitung. Das Resultat ist eine Wärmeleitfähigkeit unterhalb der von Luft (0,025 W/(m·K)).",
        difficulty = 4,
        funFact = null
    ),

    // Question 25 — Advanced Materials: Carbon Nanotubes
    Question(
        categoryId = 2,
        questionText = "Wodurch wird ein einwandiges Kohlenstoffnanoröhrchen (SWCNT) als metallisch oder halbleitend klassifiziert?",
        answerA = "Durch seinen Durchmesser: Röhrchen unter 1 nm sind immer metallisch, größere halbleitend",
        answerB = "Durch den chiralen Vektor (n, m): Wenn n−m ein Vielfaches von 3 ist, ist das Röhrchen metallisch; andernfalls halbleitend mit einer Bandlücke ∝ 1/Durchmesser",
        answerC = "Durch die Anzahl der Wanddefekte pro Nanometer Länge",
        answerD = "Durch die Dotierung mit N oder B-Atomen, nicht durch die Geometrie",
        correctAnswer = 1, // B
        explanation = "SWCNTs entstehen konzeptionell durch Aufrollen einer Graphenebene entlang des chiralen Vektors C = n·a₁ + m·a₂. Die Periodizität der K-Punkte im Graphen-Brillouin-Zone bestimmt: wenn (n−m) mod 3 = 0, schneidet der erlaubte k-Raum den Dirac-Punkt → metallisches Verhalten. Andernfalls entsteht eine Bandlücke.",
        difficulty = 4,
        funFact = "Metallische SWCNTs können bei Raumtemperatur bis zu 10⁹ A/cm² leiten – etwa 1000× mehr als Kupfer. Dies macht sie zu Kandidaten für nanoskopische Verbindungsleitungen."
    ),

    // Question 26 — Cognitive Science: Theory of Mind Neural Basis
    Question(
        categoryId = 2,
        questionText = "Welche Hirnregionen bilden den 'Theory of Mind'-Netzwerk-Kern, der beim Inferieren mentaler Zustände anderer aktiviert wird?",
        answerA = "Primärer somatosensorischer Kortex, Kleinhirn und Basalganglien",
        answerB = "Temporoparietaler Übergang (TPJ), medialer präfrontaler Kortex (mPFC), temporale Pole und posteriorer zingulärer Kortex (PCC)",
        answerC = "Primärer motorischer Kortex, supplementärer motorischer Bereich und Broca-Areal",
        answerD = "Amygdala, Hippocampus und entorhinaler Kortex ausschließlich",
        correctAnswer = 1, // B
        explanation = "Neuroimaging-Studien identifizieren konsistent ein ToM-Netzwerk: TPJ dekodiert Perspektiven anderer; mPFC repräsentiert soziale Normen und eigene vs. fremde Gedanken; temporale Pole integrieren soziale Kontextinformation; PCC ist ins Default Mode Network eingebettet. Läsionen im rTPJ beeinträchtigen explizit das Schließen über Überzeugungen anderer.",
        difficulty = 4,
        funFact = null
    ),

    // Question 27 — Cognitive Science: Attention Mechanisms
    Question(
        categoryId = 2,
        questionText = "Was beschreibt das Konzept der 'selektiven Aufmerksamkeit' nach Treisman's Feature Integration Theory?",
        answerA = "Aufmerksamkeit bindet in einer frühen präattentiven Phase individuelle Merkmale (Farbe, Orientierung) parallel; erst ein nachgeschalteter fokussierter Aufmerksamkeits-'Spot' integriert Merkmale zu Objekten",
        answerB = "Das Gehirn verarbeitet alle sensorischen Eingaben sequenziell, beginnend mit dem lautstärksten Signal",
        answerC = "Aufmerksamkeit ist eine rein top-down gesteuerte Ressource ohne Bottom-up-Komponente",
        answerD = "Sakkaden sind der einzige Mechanismus der selektiven Aufmerksamkeit im visuellen System",
        correctAnswer = 0, // A
        explanation = "Treisman (1980) unterschied präattentive Parallelverarbeitung von Einzelmerkmalen (popping out) und fokussierte Aufmerksamkeit, die Merkmale an räumlichen Positionen bindet. Fehlbindungen ('illusory conjunctions') entstehen, wenn Aufmerksamkeit fehlt. Das Modell wurde durch Posner's spotlight-Metapher und Evidenz aus Neglect-Patienten gestützt.",
        difficulty = 4,
        funFact = "Der 'Cocktailparty-Effekt' – das Herausfiltern der eigenen Stimme aus Lärm – ist ein klassisches Beispiel für selektive Aufmerksamkeit. Er funktioniert auch mit geschlossenen Augen, was visuelles Scannen ausschließt."
    ),

    // Question 28 — Cognitive Science: Working Memory Models
    Question(
        categoryId = 2,
        questionText = "Was sind die vier Komponenten von Baddeley's erweitertem Arbeitsgedächtnismodell?",
        answerA = "Kurzzeitgedächtnis, Langzeitgedächtnis, prozedurales Gedächtnis und episodisches Gedächtnis",
        answerB = "Zentralexekutive, phonologische Schleife, visuell-räumlicher Notizblock und episodischer Puffer",
        answerC = "Amygdala-Filter, Hippocampus-Index, präfrontaler Monitor und zerebelläre Zeitgebung",
        answerD = "Primacy-Effekt, Recency-Effekt, Interferenz und Konsolidierung",
        correctAnswer = 1, // B
        explanation = "Baddeleys Modell (1974, erweitert 2000) umfasst: 1) Zentralexekutive – aufmerksamkeitsbasierter Koordinator; 2) phonologische Schleife – temporäre Speicherung sprachlicher Information; 3) visuell-räumlicher Notizblock – visuelle und räumliche Information; 4) episodischer Puffer – Schnittstelle zum Langzeitgedächtnis und Integration der Subsysteme.",
        difficulty = 4,
        funFact = null
    ),

    // Question 29 — Cognitive Science: Decision Making Neuroscience
    Question(
        categoryId = 2,
        questionText = "Welche Rolle spielen Basalganglien im Modell der wert-basierten Entscheidungsfindung (value-based decision making)?",
        answerA = "Ausschließlich die Steuerung reflexartiger motorischer Reaktionen ohne Beteiligung an Bewertungsprozessen",
        answerB = "Das Striatum kodiert Erwartungswerte (expected value) und Vorhersagefehler (reward prediction errors) durch dopaminerge Signale; es integriert den direkten Weg (Go) und indirekten Weg (NoGo) zur Selektion von Handlungen",
        answerC = "Die Basalganglien blockieren präfrontale Entscheidungen bei Unsicherheit vollständig",
        answerD = "Sie speichern alle erlebten Entscheidungsergebnisse als semantische Gedächtnisengramme",
        correctAnswer = 1, // B
        explanation = "Im Actor-Critic-Modell kodiert das ventrale Striatum (Nucleus accumbens) Belohnungsvorhersagen; Dopamin-Neuronen der VTA/SNc signalisieren Vorhersagefehler (RPE) gemäß dem Temporal Difference Learning. Das dorsale Striatum wählt Aktionen über den direkten D1-Weg (Aktivierung) und indirekten D2-Weg (Inhibition) aus.",
        difficulty = 4,
        funFact = "Parkinson-Patienten zeigen oft Entscheidungsdefizite bei Belohnungslernen – ein direkter Beleg für die Rolle des dopaminergen Striatums in der wert-basierten Entscheidungsfindung."
    ),

    // Question 30 — Cognitive Science: Consciousness Theories
    Question(
        categoryId = 2,
        questionText = "Was postuliert die Integrated Information Theory (IIT) von Tononi als Grundlage des Bewusstseins?",
        answerA = "Bewusstsein entsteht aus der Aktivität des präfrontalen Kortex und ist proportional zur kognitiven Kapazität",
        answerB = "Bewusstsein entspricht dem Maß der integrierten Information Φ (Phi) eines Systems: je höher Φ, desto mehr phänomenales Erleben ist vorhanden; maximal integrierte Kausalstruktur konstituiert Bewusstsein",
        answerC = "Bewusstsein ist ein Epiphänomen neuronaler Aktivität ohne kausale Kraft auf Verhalten",
        answerD = "Quantenmechanische Kohärenz in Mikrotubuli ist der alleinige Träger des Bewusstseins",
        correctAnswer = 1, // B
        explanation = "IIT (Tononi 2004/2014) misst Φ als den Informationsgehalt, der durch die Gesamtintegration des Systems generiert wird, über das hinaus, was seine unabhängigen Teile liefern könnten. Systeme mit hohem Φ (z.B. Thalamokortikales System) haben starkes Bewusstsein; sequentielle Computer haben trotz hoher Rechenleistung niedrige Φ.",
        difficulty = 4,
        funFact = null
    ),

    // Question 31 — Paleoclimatology: Ice Core Analysis
    Question(
        categoryId = 2,
        questionText = "Wie rekonstruieren Eiskernbohrungen vergangene Temperaturen über δ¹⁸O-Messungen?",
        answerA = "¹⁸O-Ionen geben bei radioaktivem Zerfall Wärme ab, deren Rate mit der Ablagerungstemperatur korreliert",
        answerB = "Das Verhältnis von ¹⁸O zu ¹⁶O im Eiswasser spiegelt die Verdunstungstemperatur am Entstehungsort des Niederschlags wider: Kältere Temperaturen bevorzugen den Einbau von leichterem ¹⁶O, was zu niedrigeren δ¹⁸O-Werten führt",
        answerC = "¹⁸O akkumuliert ausschließlich in Eisschichten aus Vulkanperioden",
        answerD = "Die Dichte der Eisschichten in verschiedenen Tiefen dient als direktes Thermometersubstitut",
        correctAnswer = 1, // B
        explanation = "Bei der Verdunstung fraktioniert Wasser temperaturabhängig: Kälter → leichteres H₂¹⁶O verdunstet bevorzugt → Niederschlag enthält weniger ¹⁸O → δ¹⁸O ist negativ. Der Zusammenhang beträgt ca. 0,7‰ pro °C. EPICA-Eiskerne aus der Antarktis reichen 800.000 Jahre zurück und zeigen acht vollständige Glazial-Interglazial-Zyklen.",
        difficulty = 4,
        funFact = "Im EPICA-Eiskern aus Dome C sind eingeschlossene Luftblasen konserviert – die direkte Probennahme der Atmosphäre vor 800.000 Jahren und der einzige Weg, historische CO₂-Konzentrationen direkt zu messen."
    ),

    // Question 32 — Paleoclimatology: Dendrochronology
    Question(
        categoryId = 2,
        questionText = "Über welche Limitation besitzt die Dendrochronologie als Klimaproxy-Methode?",
        answerA = "Baumringe können nicht auf Jahre datiert werden und geben nur Jahrzehnt-Auflösungen",
        answerB = "Das sogenannte 'Divergenzproblem': Ab etwa 1960 korrelieren Baumringbreiten in hohen Breiten nicht mehr mit Temperaturen, was die Kalibrierung moderner Proxys erschwert",
        answerC = "Holz enthält kein ¹⁴C und erlaubt keine Radiokarbondatierung",
        answerD = "Baumringe sind nur in tropischen Wäldern auswertbar, da dort kein Jahresrhythmus vorhanden ist",
        correctAnswer = 1, // B
        explanation = "Das Divergenzproblem zeigt sich besonders in borealen Wäldern: Seit ~1960 nimmt die Ringbreite (und damit die implizierte Temperatur) ab, obwohl Thermometer steigende Temperaturen zeigen. Mögliche Ursachen: UV-Strahlungsveränderungen, Wolkenbedeckung, Schneebedeckungsveränderungen. Dies limitiert die Rekonstruktion rezenter Temperaturen.",
        difficulty = 4,
        funFact = null
    ),

    // Question 33 — Paleoclimatology: Dansgaard-Oeschger Events
    Question(
        categoryId = 2,
        questionText = "Was sind Dansgaard-Oeschger-Ereignisse (DO-Ereignisse) und über welchen Zeitraum treten sie auf?",
        answerA = "Massive Vulkanausbrüche, die alle 100.000 Jahre zu globalen Abkühlungen führten",
        answerB = "Abrupte Klimaerwärmungen von 5–16 °C in Grönland innerhalb von Jahrzehnten während des letzten Glazials (~115.000–12.000 Jahre BP), gefolgt von gradueller Abkühlung in Stadials",
        answerC = "Periodische Meeresspiegelanstiegsereignisse durch Schmelzen des Westantarktischen Eisschildes",
        answerD = "Orbitale Klimazyklen im Milankovitch-Band mit Perioden von 26.000–100.000 Jahren",
        correctAnswer = 1, // B
        explanation = "25 DO-Ereignisse wurden im Grönländischen Eiskern identifiziert. Die schnelle Erwärmung (innerhalb von Jahrzehnten) wird durch Veränderungen der atlantischen meridionalen Umwälzzirkulation (AMOC) erklärt. Das bipolare Wippenmuster zeigt, dass Antarktis und Grönland entgegengesetzte thermische Reaktionen zeigen.",
        difficulty = 4,
        funFact = "Während eines DO-Ereignisses stieg die Temperatur in Grönland um bis zu 16 °C in weniger als 40 Jahren – eine Erwärmungsrate, die selbst die heutige Klimaerwärmung um ein Vielfaches übertrifft."
    ),

    // Question 34 — Paleoclimatology: Heinrich Events
    Question(
        categoryId = 2,
        questionText = "Welches charakteristische sedimentologische Signal erzeugen Heinrich-Ereignisse in nordatlantischen Meeressedimenten?",
        answerA = "Schichten besonders feinen biogenen Karbonats durch erhöhte Kalkschalenproduktion",
        answerB = "IRD-Lagen (ice-rafted debris): grobes terrigenes Material (Steine, Sand), das von massiven Eisbergflottillen aus Nordamerika in den offenen Atlantik transportiert und beim Schmelzen abgesetzt wurde",
        answerC = "Anreicherungen von Mn-Oxidkrusten durch Sauerstoffanstieg im Bodenwasser",
        answerD = "Vulkanische Aschelagen, die durch submarine Eruptionen erzeugt wurden",
        correctAnswer = 1, // B
        explanation = "Heinrich-Ereignisse (H1-H6) zeigen sich als Lagen grobkörniger Lithikgerölle (IRD) in atlantischen Tiefseesedimenten. Massiver Eisbergausstoß aus dem Laurentidischen Eisschild brachte diese Partikel in den Atlantik. Der damit verbundene Süßwassereintrag schwächte die thermohaline Zirkulation und verursachte abrupte globale Klimaveränderungen.",
        difficulty = 4,
        funFact = null
    ),

    // Question 35 — Paleoclimatology: Ocean Sediment Cores
    Question(
        categoryId = 2,
        questionText = "Wie werden Tiefseeforaminiferenschalen in Sedimentkernen als Paläo-Temperaturproxy genutzt?",
        answerA = "Durch Zählung der Schalen pro Schicht als Produktivitätsindikator",
        answerB = "Das Mg/Ca-Verhältnis im Kalzitgehäuse von planktischen Foraminiferen steigt mit der Karbonisierungstemperatur; zusätzlich spiegelt δ¹⁸O im Schalenkalzit Temperatur und Eisvolumen wider",
        answerC = "Durch den Stickstoffgehalt organischer Reste in den Schalen als Nährstoffproxy",
        answerD = "Durch die Schichtdicke als Sedimentationsratenproxy für Meeresströmungen",
        correctAnswer = 1, // B
        explanation = "Planktische Foraminiferen bauen ihr Kalzitgehäuse in Meeresoberflächentemperaturen auf. Mg²⁺ substituiert Ca²⁺ temperaturabhängig: höhere T → höheres Mg/Ca. Kombiniert mit δ¹⁸O können Temperatur und Eisvolumeneffekt getrennt werden. Das Benthic-δ¹⁸O-Stack (LR04) liefert die Standardkurve der globalen Eisvolumina über 5 Millionen Jahre.",
        difficulty = 4,
        funFact = "Tiefseesedimentkerne aus dem Pazifik zeigen die Milankovich-Zyklen direkt in der Sauerstoffisotopie: 100.000-Jahres-Exzentrizitätszyklus, 41.000-Jahres-Obliquitätszyklus und 23.000-Jahres-Präzessionszyklus sind alle klar sichtbar."
    ),

    // Question 36 — Synthetic Chemistry: Flow Chemistry
    Question(
        categoryId = 2,
        questionText = "Welchen entscheidenden Vorteil bietet kontinuierliche Fließchemie (flow chemistry) gegenüber der klassischen Batch-Synthese bei gefährlichen Reaktionen?",
        answerA = "Fließchemie erlaubt nur mildere Reaktionsbedingungen und ist deshalb für energetische Reaktionen ungeeignet",
        answerB = "Das sehr kleine Reaktionsvolumen im Mikroreaktor (Mikroliter bis Milliliter) minimiert das Risiko unkontrollierter exothermer Reaktionen, ermöglicht exzellente Wärmeübertragung und erlaubt sichere Handhabung instabiler Intermediate",
        answerC = "Alle Reagenzien sind in einem einzigen Tank vereint, was Mischungszeiten auf null reduziert",
        answerD = "Fließchemie erfordert keine Kühlsysteme, weil Reaktionswärme durch Dampfgenerierung abgeführt wird",
        correctAnswer = 1, // B
        explanation = "In Mikroreaktoren ist das Oberflächen-zu-Volumen-Verhältnis 100–10.000× größer als in Batch-Reaktoren. Dies ermöglicht exzellente Temperaturkontrolle bei hoch-exothermen Reaktionen (z.B. Nitrierungen, Fluorierungen). Gefährliche Intermediate (z.B. Diazomethan, HN₃) können in situ erzeugt und sofort verbraucht werden.",
        difficulty = 4,
        funFact = null
    ),

    // Question 37 — Synthetic Chemistry: Green Chemistry Principles
    Question(
        categoryId = 2,
        questionText = "Was beschreibt das Konzept der 'Atomökonomie' (atom economy) nach Trost in der Grünen Chemie?",
        answerA = "Den prozentualen Anteil der Reaktionsmasse, der als gewünschtes Produkt gewonnen wird, berechnet aus dem Verhältnis der Molmassen von Produkt zu allen Edukten",
        answerB = "Den Energieverbrauch pro Mol Produkt unter Standardbedingungen",
        answerC = "Die Anzahl der Reaktionsschritte in einer Totalsynthese multipliziert mit dem Atomgewicht des Zielstrukturatoms",
        answerD = "Das Verhältnis von recycelbaren zu verbrauchten Lösungsmitteln in einer Synthese",
        correctAnswer = 0, // A
        explanation = "Atomökonomie (Trost 1991) = (Molmasse Produkt / Summe Molmassen aller Edukte) × 100%. Eine Addition hat 100% Atomökonomie; eine Substitutionsreaktion mit Abgangsgruppe deutlich weniger. Das Konzept priorisiert Reaktionstypen, bei denen alle Atome ins Zielprodukt einfließen, gegenüber Eliminierungen mit Nebenprodukten.",
        difficulty = 4,
        funFact = "Barry Trost, der das Atom-Economy-Konzept einführte, erhielt dafür 2004 den Presidential Green Chemistry Challenge Award – es revolutionierte das Denken über Syntheseeffizienz jenseits der klassischen Ausbeute."
    ),

    // Question 38 — Synthetic Chemistry: Combinatorial Chemistry
    Question(
        categoryId = 2,
        questionText = "Wie unterscheidet sich das 'Split-and-Pool'-Verfahren in der kombinatorischen Chemie von der parallelen Synthese?",
        answerA = "Beim Split-and-Pool werden Harzpartikel nach jedem Schritt aufgeteilt, gemischt und mit verschiedenen Bausteinen umgesetzt, was zu jedem Partikel mit einer einzigartigen Verbindung führt – ohne adressierbare räumliche Position",
        answerB = "Bei Split-and-Pool werden alle Verbindungen in einem einzigen Reaktionsgefäß gleichzeitig synthetisiert",
        answerC = "Die parallele Synthese nutzt einen einzigen Baustein für alle Positionen; Split-and-Pool nutzt zufällige Auswahl",
        answerD = "Split-and-Pool erfordert automatisierte Pipettierroboter, parallele Synthese nicht",
        correctAnswer = 0, // A
        explanation = "Split-and-Pool (Furka 1991): Harz wird aufgeteilt → jede Portion mit anderem Baustein X gekoppelt → alle Portionen vereint (pool) → wieder aufgeteilt usw. Theoretisch entstehen n₁ × n₂ × n₃ Verbindungen. Jedes Harzpartikel trägt exakt eine Verbindung, deren Identität durch Dekodierstrategie (z.B. chemische Tags) bestimmt wird.",
        difficulty = 4,
        funFact = null
    ),

    // Question 39 — Synthetic Chemistry: Click Chemistry Variants
    Question(
        categoryId = 2,
        questionText = "Was macht die kupferkatalysierte Azid-Alkin-Cycloaddition (CuAAC) zu einem bevorzugten 'Click-Chemistry'-Werkzeug in der Biokonjugation?",
        answerA = "Hohe Regioselektivität (ausschließlich 1,4-Triazol), Bioorthogonalität beider Reaktionspartner, milde wässrige Bedingungen, sehr hohe Ausbeuten und keine störenden Nebenprodukte",
        answerB = "Reaktion läuft bei 200 °C ab und braucht nur Kupfersalze ohne Liganden",
        answerC = "Azide und Alkine kommen natürlich in allen Proteinen vor und müssen nicht eingeführt werden",
        answerD = "CuAAC produziert ein linear-kettenförmiges Produkt ohne Ringbildung",
        correctAnswer = 0, // A
        explanation = "CuAAC (Sharpless/Meldal 2002) erfüllt alle Kriterien grüner Biokonjugation: Azide und Alkine sind in biologischen Systemen nicht nativ vorhanden (bioorthogonal), reagieren selektiv miteinander, bilden stabiles 1,2,3-Triazol (1,4-regiospezifisch durch Cu(I)-Katalyse) und liefern quantitative Ausbeuten in Wasser bei RT.",
        difficulty = 4,
        funFact = "Der 'Click Chemistry'-Ansatz, mitentwickelt von K. Barry Sharpless, brachte diesem 2022 seinen zweiten Chemie-Nobelpreis – er hatte bereits 2001 einen für asymmetrische Oxidation erhalten."
    ),

    // Question 40 — Synthetic Chemistry: Total Synthesis Strategies
    Question(
        categoryId = 2,
        questionText = "Was beschreibt das Konzept der 'Retrosynthese' nach E.J. Corey?",
        answerA = "Eine Synthesestrategie, bei der alle Schritte gleichzeitig in einem Tandem-Prozess ablaufen",
        answerB = "Systematisches rückwärtsgerichtetes Denken vom Zielmolekül ausgehend: sequenzielle gedankliche Bindungsbrüche (Disconnections) führen zu Vorläufern, bis einfache kommerziell verfügbare Startmaterialien erreicht sind",
        answerC = "Die Reaktion in umgekehrter Richtung durch Energiezufuhr, um Ausgangsmaterialien zurückzugewinnen",
        answerD = "Computersimulierter Vergleich aller Syntheserouten für ein Zielmolekül anhand ihrer Gibbs-Energie",
        correctAnswer = 1, // B
        explanation = "Corey entwickelte die retrosynthetische Analyse als formale Methode: Vom Zielmolekül ausgehend werden gezielt Bindungen gebrochen (⟹ retrosynthetischer Pfeil), um Synthone (ideale Intermediate) zu erzeugen. Dieses Denken in Disconnections, Transforms und Strategiepunkten (Stereozentren, Ringschlüsse) revolutionierte die Planung komplexer Totalsynthesen.",
        difficulty = 4,
        funFact = "Für die Entwicklung der Retrosynthese erhielt E.J. Corey 1990 den Nobelpreis für Chemie. Seine Totalsynthese von Prostaglandinen gilt als Meisterwerk der methodischen Syntheseplanung."
    ),

    // Question 41 — Gravitational Physics: Frame-Dragging
    Question(
        categoryId = 2,
        questionText = "Was ist der Lense-Thirring-Effekt (Frame-Dragging)?",
        answerA = "Die Verkrümmung von Lichtstrahlen durch massereiche Körper gemäß der allgemeinen Relativitätstheorie",
        answerB = "Der Effekt, bei dem ein rotierender Massekörper die umgebende Raumzeit mitschleppt, was zu einer Präzession umlaufender Kreisel führt, die über Newton hinausgeht",
        answerC = "Die Zeitdilatation in einem starken Gravitationsfeld nach dem Schwarzschild-Lösungsansatz",
        answerD = "Die Emission von Gravitationswellen durch ein beschleunigtes binäres System",
        correctAnswer = 1, // B
        explanation = "Lense und Thirring sagten 1918 aus Einsteins Feldgleichungen vorher, dass ein rotierender Körper die Raumzeit wie eine viskose Flüssigkeit mitdreht. Konsequenz: Ein Kreisel im Orbit präzediert zusätzlich (Lense-Thirring-Präzession). Gravity Probe B (2004) bestätigte den Effekt mit 19% Genauigkeit durch superpräzise kryogene Quarzkreisel.",
        difficulty = 4,
        funFact = "Der Frame-Dragging-Effekt der Erde beträgt nur 0,039 Bogensekunden pro Jahr – so klein, dass zu seiner Messung Kreisel mit Driftrates von weniger als 0,001 Bogensekunden/Jahr nötig waren."
    ),

    // Question 42 — Gravitational Physics: Geodetic Precession
    Question(
        categoryId = 2,
        questionText = "Wie unterscheidet sich die geodätische Präzession von der Lense-Thirring-Präzession?",
        answerA = "Geodätische Präzession ist eine Wirkung der Raumzeitkrümmung durch die Masse des Zentralkörpers auf einen orbitierenden Kreisel; Lense-Thirring entsteht durch den Drehimpuls des Zentralkörpers",
        answerB = "Beide Effekte sind identisch, nur für verschiedene Bahninklination definiert",
        answerC = "Geodätische Präzession tritt nur bei Schwarzen Löchern auf, Lense-Thirring bei normalen Sternen",
        answerD = "Geodätische Präzession beschreibt Lichtablenkung, Lense-Thirring die Zeitdilatation",
        correctAnswer = 0, // A
        explanation = "Geodätische Präzession (de Sitter-Präzession) resultiert aus dem Paralleltransport eines Vektors entlang einer gekrümmten Raumzeit – bereits eine statische Masse verursacht sie. Für Gravity Probe B betrug sie 6606,1 mas/Jahr. Lense-Thirring (~39 mas/Jahr) ist ein zusätzlicher, viel kleinerer Effekt durch die Erdrotation. Beide wurden von GP-B separat gemessen.",
        difficulty = 4,
        funFact = null
    ),

    // Question 43 — Gravitational Physics: Gravitational Time Dilation
    Question(
        categoryId = 2,
        questionText = "In welchem Alltagssystem muss gravitationale Zeitdilatation (gemäß ART) korrigiert werden, um präzise Funktion zu gewährleisten?",
        answerA = "In Kernkraftwerken zur Synchronisation von Sicherheitssystemen",
        answerB = "Im GPS-System: Satellitenuhren auf 20.200 km Höhe gehen täglich um ~45,9 µs vor (gravitativ) und 7,2 µs nach (spezielle RT); die Nettokorrektur von +38,4 µs/Tag ist einprogrammiert",
        answerC = "In MRT-Geräten zur Kompensation der Feldinhomogenität durch Gravitationsgradienten",
        answerD = "In Mobilfunknetzen zur Latenzkorrektur durch städtische Gebäudekluster",
        correctAnswer = 1, // B
        explanation = "GPS-Satelliten in ~20.200 km Höhe befinden sich in einem schwächeren Gravitationsfeld: Nach ART laufen ihre Uhren täglich 45,9 µs schneller (gravitationale Zeitdilatation). Gemäß SRT laufen sie wegen ihrer Orbitalgeschwindigkeit (3,87 km/s) um 7,2 µs langsamer. Nettoeffekt: +38,4 µs/Tag. Ohne Korrektur würde GPS täglich um ~10 km driften.",
        difficulty = 4,
        funFact = "GPS ist damit eines der wenigen technologischen Systeme, das explizit relativistische Korrekturen benötigt – Einsteins Theorien sind buchstäblich in jedem Navigationssystem implementiert."
    ),

    // Question 44 — Gravitational Physics: Equivalence Principle Tests
    Question(
        categoryId = 2,
        questionText = "Was ist das 'starke Äquivalenzprinzip' (SEP) im Gegensatz zum schwachen Äquivalenzprinzip (WEP)?",
        answerA = "SEP besagt, dass alle Gesetze der Physik – einschließlich der Gravitation selbst – lokal in einem frei fallenden Rahmen identisch mit denen der Spezielle-Relativitäts-Rahmen sind; WEP gilt nur für nicht-gravitative Wechselwirkungen",
        answerB = "SEP erlaubt Variationen der Gravitationskonstante G über kosmische Zeit, WEP nicht",
        answerC = "SEP gilt nur für Materieteilchen, WEP auch für Photonen",
        answerD = "Beide Prinzipien sind identisch, aber SEP gilt nur für inertige Massen über 1 kg",
        correctAnswer = 0, // A
        explanation = "WEP: Träge und schwere Masse sind gleich (universeller freier Fall). EEP (Einstein): lokale nicht-gravitative Physik ist überall gleich. SEP (Nordvedt 1968): auch Selbstgravitations-Energie fällt gleich. SEP-Verletzung würde sich durch den Nordtvedt-Effekt zeigen: Erde und Mond würden unterschiedlich zur Sonne fallen. Lunar Laser Ranging testet SEP auf 10⁻¹³ Niveau.",
        difficulty = 4,
        funFact = null
    ),

    // Question 45 — Gravitational Physics: LISA Mission
    Question(
        categoryId = 2,
        questionText = "Welcher Frequenzbereich ist LISA (Laser Interferometer Space Antenna) auf Gravitationswellendetektion ausgelegt und warum ist dieser für Bodendetektoren unzugänglich?",
        answerA = "1 kHz bis 100 kHz: Diese Frequenzen werden durch atmosphärische Absorption gedämpft",
        answerB = "10⁻⁴ bis 10⁻¹ Hz (Milli-Hz-Band): Bodendetektoren werden unter dieser Frequenz durch seismisches Rauschen (Newtonian noise) limitiert; LISA mit 2,5 Mio. km Armlänge im Weltall überwindet diese Einschränkung",
        answerC = "10⁻¹⁵ bis 10⁻¹² Hz: Pulsar-Timing-Arrays decken diesen Bereich nicht ab",
        answerD = "1–10 GHz: Hochfrequente Gravitationswellen aus Schwarzen Löchern erfordern Weltraummessungen",
        correctAnswer = 1, // B
        explanation = "LIGO/Virgo detektiert ~10–1000 Hz (kompakte Binärsysteme kurz vor Merger). LISA wird 10⁻⁴–10⁻¹ Hz abdecken: massereiche Schwarzloch-Binaries (10⁴–10⁷ M☉), galaktische Weißzwerg-Binaries, EMRIs. Seismisches Rauschen auf der Erde ist unterhalb ~1 Hz dominant; LISA umgeht dies durch seine 2,5-Millionen-km-Dreiecksformation im Weltraum.",
        difficulty = 4,
        funFact = "LISA Pathfinder (2015–2017) demonstrierte die nötige Präzision für LISA: Die Testmassen drifteten um weniger als 10⁻¹⁵ m/√Hz auseinander – weniger als ein Atomdurchmesser pro Sekunde."
    ),

    // Question 46 — Advanced Immunology: Trained Immunity
    Question(
        categoryId = 2,
        questionText = "Was beschreibt 'Trained Immunity' (epigenetisches Gedächtnis des angeborenen Immunsystems)?",
        answerA = "Die klassische antigenspezifische Immunantwort durch T- und B-Gedächtniszellen nach Impfung",
        answerB = "Die Fähigkeit angeborener Immunzellen (v.a. Monozyten/Makrophagen), nach einem ersten Stimulus epigenetisch umprogrammiert zu werden und bei erneutem Kontakt stärker zu reagieren – ohne antigenspezifisches Gedächtnis",
        answerC = "Angeborenes Immunsystem-Lernen durch somatische Hypermutation von Pattern-Recognition-Rezeptoren",
        answerD = "Die klonale Expansion von NK-Zellen nach Virusinfektionen analog zur adaptiven Immunantwort",
        correctAnswer = 1, // B
        explanation = "Netea et al. (2011) zeigten, dass β-Glucan (aus C. albicans) in Monozyten epigenetische Veränderungen induziert (H3K4me3 an Promotoren entzündlicher Gene). Remethyliert werden v.a. metabolische Gene (mTOR-Signalweg, Glutaminolyse). BCG-Impfung induziert ebenfalls Trained Immunity und erklärt die unspezifischen Schutzwirkungen gegen Atemwegsinfektionen.",
        difficulty = 4,
        funFact = "BCG-Impfung in der Neugeborenenperiode reduziert die Gesamtsterblichkeit um 30–50% über Infektionen hinaus, die nicht durch Mykobakterien verursacht werden – ein starker Hinweis auf Trained Immunity als klinisch relevanten Effekt."
    ),

    // Question 47 — Advanced Immunology: Checkpoint Inhibitors
    Question(
        categoryId = 2,
        questionText = "Wie umgehen Tumorzellen den T-Zell-Angriff über den PD-1/PD-L1-Signalweg und wie wirken Checkpoint-Inhibitoren dagegen?",
        answerA = "Tumorzellen exprimieren PD-L1, das an PD-1 auf T-Zellen bindet und deren Aktivierung durch SHP-2-Phosphatasen hemmt; Anti-PD-1/PD-L1-Antikörper blockieren diese Interaktion und reaktivieren erschöpfte T-Zellen",
        answerB = "Tumorzellen sezernieren TGF-β, das CD8⁺-T-Zellen direkt apoptotisch tötet; Checkpoint-Inhibitoren neutralisieren TGF-β",
        answerC = "PD-1 aktiviert T-Zellen; Tumoren exprimieren PD-L1 als Lockstoff zur Anziehung von T-Zellen, die dann abgetötet werden",
        answerD = "Checkpoint-Inhibitoren verstärken die NK-Zell-Aktivität durch Bindung an NKG2D",
        correctAnswer = 0, // A
        explanation = "PD-L1 auf Tumorzellen bindet PD-1 auf tumor-infiltrierenden T-Zellen → Rekrutierung von SHP-1/SHP-2 → Dephosphorylierung von CD3ζ und Zap-70 → T-Zell-Erschöpfung. Anti-PD-1 (Nivolumab, Pembrolizumab) oder Anti-PD-L1 (Atezolizumab) blockieren diese Achse und ermöglichen T-Zell-Reaktivierung. Bei ~20–40% solider Tumoren zeigen sich dauerhafte Remissionen.",
        difficulty = 4,
        funFact = "James Allison und Tasuku Honjo erhielten 2018 den Medizin-Nobelpreis für die Entdeckung der Immuncheckpoint-Inhibition – die bis dahin schnellste Übersetzung einer immunologischen Grundlagenentdeckung in klinisch wirksame Krebstherapie."
    ),

    // Question 48 — Advanced Immunology: CAR-T Cell Therapy
    Question(
        categoryId = 2,
        questionText = "Was sind die Hauptkomponenten eines chimären Antigenrezeptors (CAR) der zweiten Generation?",
        answerA = "Ein extrazellulärer Antikörper-Einzelketten-Fragmentvariabler (scFv) zur Antigenerkennung, eine Transmembrandomäne, eine kostimulatorische Domäne (CD28 oder 4-1BB) und eine CD3ζ-Signaldomäne zur T-Zell-Aktivierung",
        answerB = "TCR α- und β-Ketten, CD4-Korezeptor, MHC-Bindedomäne und CD28",
        answerC = "Fc-Region eines IgG, CD16-Transmembrandomäne und ITAM-Motive aus FcεRI",
        answerD = "Zwei scFv-Fragmente für zwei verschiedene Antigene, verbunden durch eine flexible Linkerdomäne ohne Signalmotiv",
        correctAnswer = 0, // A
        explanation = "CARs der 2. Generation kombinieren: scFv (kloniert aus einem monoklonalen Antikörper) für MHC-unabhängige Antigenerkennung + Hinge/Transmembrandomäne + eine kostimulatorische Domäne (CD28 für schnelle, 4-1BB für persistente Aktivierung) + CD3ζ mit drei ITAMs für T-Zell-Aktivierungssignale. Axicabtagene ciloleucel (anti-CD19) erzielte vollständige Remissionen bei refraktärem DLBCL.",
        difficulty = 4,
        funFact = null
    ),

    // Question 49 — Advanced Immunology: Complement System Regulation
    Question(
        categoryId = 2,
        questionText = "Welcher Regulator schützt körpereigene Zellen vor Lyse durch das Komplementsystem über den alternativen Weg?",
        answerA = "C1-Inhibitor (C1-INH) blockiert die Spaltung von C3 durch Faktor D",
        answerB = "CD55 (DAF) und CD59: DAF beschleunigt den Zerfall der C3-Konvertase auf der Zelloberfläche; CD59 blockiert den Einbau von C9 in den Membran-Angriffskomplex (MAC)",
        answerC = "IL-10 inhibiert die Komplementaktivierung durch Hemmung von C3b-Ablagerung",
        answerD = "MBL-Mannan-bindendes Lectin hemmt den klassischen Weg durch Bindung an C4",
        correctAnswer = 1, // B
        explanation = "Körpereigene Zellen sind durch GPI-verankerte membranständige Regulatoren geschützt: CD55 (DAF) beschleunigt die Dissoziation der C3bBb-Konvertase (alternativer Weg) und C4b2a (klassischer/Lektin-Weg). CD59 (Protektin) bindet C8 und verhindert die Polymerisation von C9 zum MAC. Fehlen diese Regulatoren (PNH), lysiert Komplement eigene Blutzellen.",
        difficulty = 4,
        funFact = "Paroxysmale Nächtliche Hämoglobinurie (PNH) ist eine seltene Erkrankung durch somatische PIGA-Mutation, die GPI-Anker-Biosynthese stört – betroffene Erythrozyten fehlen CD55 und CD59 und werden durch Komplement lysiert."
    ),

    // Question 50 — Advanced Immunology: Mucosal Immunity
    Question(
        categoryId = 2,
        questionText = "Welche Eigenschaft macht sekretorisches IgA (sIgA) zum dominanten Antikörper an Schleimhäuten?",
        answerA = "sIgA aktiviert effizient das Komplementsystem und induziert ADCC an mukosalen Epithelien",
        answerB = "sIgA liegt als dimeres Molekül vor, das durch eine J-Kette und sekretorische Komponente (SC) stabilisiert wird; SC schützt vor Proteolyse, und dimeres sIgA agglutiniert Pathogene durch Immun-Exklusion ohne Inflammation",
        answerC = "sIgA ist monomer, überquert das Epithel nie und wirkt ausschließlich im Blut",
        answerD = "sIgA bindet Fc-Rezeptoren auf neutrophilen Granulozyten und verstärkt so die Phagozytose an Schleimhäuten",
        correctAnswer = 1, // B
        explanation = "Dimeres sIgA wird von Plasmazellen in der Lamina propria sezerniert, bindet den polymerischen Ig-Rezeptor (pIgR) auf Epithelzellen und wird transzytotisch ins Lumen exportiert. Die sekretorische Komponente (abgespaltenes pIgR-Stück) stabilisiert sIgA vor luminalen Proteasen. sIgA agglutiniert Bakterien und Viren ('immune exclusion') ohne Komplementaktivierung – ideal für nicht-inflammatorische Abwehr.",
        difficulty = 4,
        funFact = "Der menschliche Körper produziert täglich 3–5 g sIgA – mehr als alle anderen Antikörperklassen zusammen. Es ist der mengenmäßig dominanteste Antikörper der Spezies Mensch."
    ),

)
