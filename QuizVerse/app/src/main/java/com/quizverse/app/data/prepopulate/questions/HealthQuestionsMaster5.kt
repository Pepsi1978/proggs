package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun healthQuestionsMaster5(): List<Question> = listOf(

    // --- ADVANCED DIABETES & INSULIN SIGNALING ---

    // Question 1 - DKA Pathophysiology
    Question(
        categoryId = 16,
        questionText = "Welcher biochemische Mechanismus erklaert die Anionenluecke bei der diabetischen Ketoazidose (DKA)?",
        answerA = "Anhaufung von Laktat durch anaerobe Glykolyse",
        answerB = "Akkumulation von Beta-Hydroxybutyrat und Acetoacetat als ungemessene Anionen",
        answerC = "Verlust von Bikarbonat durch renale Fehlfunktion",
        answerD = "Anhaufung von Phosphat bei Nierenversagen",
        correctAnswer = 1,
        explanation = "Bei der DKA akkumulieren die Ketonkoerper Beta-Hydroxybutyrat und Acetoacetat als starke Saeurereste. Diese dissoziierten Anionen werden im Standard-Elektrolytpanel nicht gemessen, erhoehen aber die Anionenluecke (Na+ - Cl- - HCO3- > 12 mEq/L). Der zugrundeliegende Insulinmangel fuehrt zur ungezuegelten Lipolyse und hepatischen Ketogenese.",
        difficulty = 5,
        funFact = "Das Aceton -- das dritte Ketonkoerper-Produkt bei DKA -- ist fluechtiger Natur und wird abgeatmet, was den charakteristischen fruchtig-suesslichen Atemgeruch bei DKA-Patienten verursacht."
    ),

    // Question 2 - Insulin Signaling Cascade
    Question(
        categoryId = 16,
        questionText = "Welches Substrat des Insulinrezeptors ist das primaere Andockprotein fuer die PI3-Kinase-Aktivierung in der Insulinsignalkaskade?",
        answerA = "Grb2 (Growth factor receptor-bound protein 2)",
        answerB = "IRS-1 (Insulin Receptor Substrate-1)",
        answerC = "SHC (Src homology collagen)",
        answerD = "SHIP2 (SH2-domain-containing inositol polyphosphatase 2)",
        correctAnswer = 1,
        explanation = "Der aktivierte Insulinrezeptor phosphoryliert IRS-1 an mehreren Tyrosinresten. Die phosphorylierten IRS-1-Tyrosine dienen als Andockstellen fuer die SH2-Domaene der regulatorischen p85-Untereinheit der PI3-Kinase. Die aktivierte PI3K phosphoryliert dann PIP2 zu PIP3, was die Akt-Aktivierung und nachgelagert GLUT4-Translokation, Glykogensynthese und Proteinsynthese antreibt.",
        difficulty = 5,
        funFact = "Mutationen in IRS-1 sind mit Insulinresistenz und erhoehtem Typ-2-Diabetes-Risiko assoziiert -- die molekulare Grundlage fuer die schlechte Glukoseverwertung auch bei vorhandenem Insulin."
    ),

    // Question 3 - GLP-1 Receptor Mechanism
    Question(
        categoryId = 16,
        questionText = "Ueber welchen intrazellullaeren Signalweg stimuliert der GLP-1-Rezeptor die Insulinsekretion aus Beta-Zellen?",
        answerA = "Gs-Protein -> Adenylylcyclase -> cAMP -> PKA/Epac2 -> KATP-Kanal-unabhaengige Depolarisation und Insulinsekretion",
        answerB = "Gi-Protein -> Hemmung der Adenylylcyclase -> Reduktion von cAMP -> Suppression von PKA",
        answerC = "Gq-Protein -> PLC-beta -> IP3 -> Ca2+-Freisetzung ohne cAMP-Beteiligung",
        answerD = "Direkte Tyrosinkinase-Aktivitaet -> IRS-Phosphorylierung -> GLUT2-Translokation",
        correctAnswer = 0,
        explanation = "GLP-1 bindet an seinen Gs-Protein-gekoppelten Rezeptor, aktiviert Adenylylcyclase und erhoht cAMP. cAMP aktiviert sowohl PKA (Phosphorylierung von Exozytosemaschinerie) als auch Epac2 (exchange protein directly activated by cAMP). Epac2 verstaerkt glukoseabhaengig die Ca2+-induzierte Insulingranula-Exozytose, weshalb GLP-1-Agonisten nur bei erhoehtem Blutzucker wirken.",
        difficulty = 5,
        funFact = "GLP-1 wird im Duenndarm von L-Zellen sezerniert und hat eine Halbwertszeit von unter 2 Minuten -- Dipeptidylpeptidase-4 (DPP-4) baut es sofort ab. Deshalb haben Pharmafirmen DPP-4-Hemmer und GLP-1-Analoga mit laengerer Halbwertszeit entwickelt."
    ),

    // Question 4 - KATP Channel in Beta Cells
    Question(
        categoryId = 16,
        questionText = "Welche molekulare Zusammensetzung hat der KATP-Kanal in pankreatischen Beta-Zellen, und welches Protein ist das pharmakologische Ziel der Sulfonylharnstoffe?",
        answerA = "Vier Kir6.2-Untereinheiten + vier SUR1-Untereinheiten; Sulfonylharnstoffe binden SUR1",
        answerB = "Zwei Kir6.1-Untereinheiten + zwei SUR2A-Untereinheiten; Sulfonylharnstoffe binden Kir6.1",
        answerC = "Vier Kir3.4-Untereinheiten + vier SUR2B-Untereinheiten; Sulfonylharnstoffe binden Kir3.4",
        answerD = "Acht Kir6.2-Untereinheiten ohne SUR-Untereinheiten; direkter ATP-Sensor",
        correctAnswer = 0,
        explanation = "Der Beta-Zell-KATP-Kanal ist ein oktamerer Komplex aus vier porenbildenden Kir6.2-Untereinheiten und vier regulatorischen SUR1 (Sulfonylharnstoff-Rezeptor 1)-Untereinheiten. Bei steigendem ATP/ADP-Verhaltnis nach Glukosestoffwechsel schliesst der Kanal, was zur Depolarisation und Ca2+-Einstrom fuehrt. Sulfonylharnstoffe binden direkt an SUR1 und schliessen den Kanal Glukose-unabhaengig.",
        difficulty = 5,
        funFact = "Aktivierende Mutationen in KCNJ11 (kodiert Kir6.2) oder ABCC8 (kodiert SUR1) verursachen neonatalen Diabetes -- Kinder mit diesen Mutationen koennen oft von Insulin auf orale Sulfonylharnstoffe umgestellt werden, was die molekulare Praezisionsmedizin demonstriert."
    ),

    // --- THYROID CANCER ---

    // Question 5 - RET Proto-Oncogene
    Question(
        categoryId = 16,
        questionText = "Welche Mutation im RET-Protoonkogen ist spezifisch fuer das hereditaere medullaere Schilddruesenkarzinom im Rahmen von MEN 2A?",
        answerA = "Punktmutation in Codon 918 der Kinasedomaene (M918T)",
        answerB = "Punktmutationen in den Cystein-reichen Codons 609, 611, 618, 620 oder 634 der extrazellullaeren Domaene",
        answerC = "RET/PTC-Rearrangement als Fusion mit H4- oder CCDC6-Gen",
        answerD = "Frameshift-Mutation in Exon 10 mit Verlust der Transmembrandomaene",
        correctAnswer = 1,
        explanation = "MEN 2A wird durch gain-of-function Mutationen in den extrazellullaeren Cystein-reichen Codons des RET-Rezeptors verursacht, am haeufigsten Codon 634 (C634R/W/G/Y). Diese Mutationen fuehren zur konstitutiven Dimerisierung und Aktivierung von RET ohne Ligandenbindung. Die Codon-918-Mutation (M918T) ist hingegen charakteristisch fuer MEN 2B und sporadische medullaere Karzinome.",
        difficulty = 5,
        funFact = "Traeger von RET-Codon-634-Mutationen haben ein nahezu 100-prozentiges Lebensrisiko fuer ein medullaeres Schilddruesenkarzinom. Deshalb empfehlen Leitlinien prophylaktische Thyreoidektomie bereits im Kindesalter -- bei Codon-634 bis zum 5. Lebensjahr."
    ),

    // Question 6 - BRAF Mutation in Papillary Thyroid Cancer
    Question(
        categoryId = 16,
        questionText = "Welche Signalkaskade aktiviert die BRAF-V600E-Mutation beim papillaeren Schilddruesenkarzinom, und welche klinische Konsequenz hat dies?",
        answerA = "Konstitutive Aktivierung des PI3K/Akt-Pfades -> Resistenz gegen Radiojodtherapie",
        answerB = "Konstitutive Aktivierung des MAPK/ERK-Pfades -> Verlust der Natrium-Jodid-Symporter-Expression -> Radiojodresistenz",
        answerC = "Aktivierung des Wnt/Beta-Catenin-Pfades -> erhoehte Metastasierung ohne Jodresistenz",
        answerD = "Hemmung des mTOR-Komplexes -> Dedifferenzierung und anaplastischer Umbau",
        correctAnswer = 1,
        explanation = "BRAF-V600E aktiviert konstitutiv MEK und ERK im MAPK-Pfad. Die chronische ERK-Aktivierung fuehrt zur Suppression des Natrium-Jodid-Symporters (NIS) und anderer Differenzierungsmarker (Thyroglobulin, TSH-Rezeptor, TPO). Das Resultat ist ein weniger differenziertes Karzinom mit reduzienter Radiojodaufnahme und schlechterer Prognose.",
        difficulty = 5,
        funFact = "BRAF-V600E findet sich bei etwa 45-60% aller papillaeren Schilddruesenkarzinome. Der BRAF-Inhibitor Vemurafenib kann in Kombination mit MEK-Inhibitoren bei fortgeschrittenen Faellen die NIS-Expression teilweise wiederherstellen -- sogenannte 'Redifferenzierungstherapie'."
    ),

    // Question 7 - Anaplastic Thyroid Cancer
    Question(
        categoryId = 16,
        questionText = "Welche molekulare Besonderheit unterscheidet das anaplastische Schilddruesenkarzinom (ATC) von differenzierten Formen, und welcher Signalweg ist therapeutisch relevant?",
        answerA = "Ausschliesslich RET/PTC-Rearrangements; therapeutisch relevant ist Vandetanib",
        answerB = "Haeufige TP53-Mutationen plus BRAF/RAS/PIK3CA-Mutationen; BRAF/MEK-Inhibition bei BRAF-V600E-ATC",
        answerC = "Ausschliesslich PAX8/PPARgamma-Fusion; therapeutisch relevant ist Pioglitazon",
        answerD = "Methylierung des TSHR-Promotors ohne genetische Mutation; therapeutisch relevant ist TSH-Suppression",
        correctAnswer = 1,
        explanation = "ATC traegt haeufig TP53-Mutationen (50-80%), die den Tumorsupresspressor ausschalten, oft kombiniert mit BRAF-V600E, RAS-Mutationen oder PIK3CA-Mutationen. Bei BRAF-V600E-positivem ATC zeigte die Kombination Dabrafenib/Trametinib (BRAF/MEK-Inhibition) signifikante Ansprechraten und erhielt FDA-Zulassung, obwohl die Prognose weiterhin infaust bleibt.",
        difficulty = 5,
        funFact = "ATC ist eine der aggressivsten menschlichen Tumorerkrankungen ueberhaupt -- die mediane Ueberlebenszeit betraegt trotz Therapie nur 3-5 Monate. Dennoch hat die zielgerichtete Therapie bei BRAF-mutiertem ATC die Prognose erstmals substantiell verbessert."
    ),

    // Question 8 - Follicular Thyroid Cancer PAX8/PPARgamma
    Question(
        categoryId = 16,
        questionText = "Welche genetische Veraenderung ist charakteristisch fuer das follikulaere Schilddruesenkarzinom und unterscheidet es auf molekularer Ebene vom follikulaeren Adenom?",
        answerA = "BRAF-V600E-Punktmutation in ueber 70% der Faelle",
        answerB = "PAX8/PPARgamma-Translokation t(2;3)(q13;p25) in ca. 30-40% der Faelle sowie RAS-Mutationen",
        answerC = "RET/PTC3-Rearrangement in ueber 50% der Faelle",
        answerD = "Homozygote Deletion von CDKN2A/p16 in nahezu allen Faellen",
        correctAnswer = 1,
        explanation = "Die PAX8/PPARgamma-Fusion entsteht durch t(2;3)(q13;p25) und findet sich in 30-40% follikulaerer Karzinome, nicht aber in Adenomen. Daneben sind RAS-Mutationen (N-RAS Codon 61 am haeufigsten) charakteristisch, die auch in Adenomen vorkommen. Die sicherste Differenzierung Adenom vs. Karzinom bleibt histologisch -- Kapseldurchbruch oder Gefaessinvasion.",
        difficulty = 5,
        funFact = "Anders als das papillaere Karzinom, das bevorzugt lymphogen metastasiert, streut das follikulaere Karzinom haematogen -- Knochen- und Lungenmetastasen sind typisch. Das erklaert, warum follikulaere Fernmetastasen oft noch jodspeichernd sind und auf Radiojodtherapie ansprechen."
    ),

    // --- ADRENAL TUMORS ---

    // Question 9 - Pheochromocytoma Catecholamine Pathways
    Question(
        categoryId = 16,
        questionText = "Welches Enzym katalysiert den geschwindigkeitsbestimmenden Schritt der Katecholaminsynthese im Phaeochromozytom, und welcher Kofaktor ist essentiell?",
        answerA = "Dopamin-beta-Hydroxylase; Kofaktor ist FAD",
        answerB = "Tyrosinhydroxylase; Kofaktor ist Tetrahydrobiopterin (BH4)",
        answerC = "DOPA-Decarboxylase; Kofaktor ist Pyridoxalphosphat (Vitamin B6)",
        answerD = "PNMT (Phenylethanolamin-N-Methyltransferase); Kofaktor ist SAM",
        correctAnswer = 1,
        explanation = "Tyrosinhydroxylase hydroxyliert L-Tyrosin zu L-DOPA und ist der geschwindigkeitsbestimmende Schritt der Katecholaminbiosynthese. Das Enzym benoetigt molekularen Sauerstoff und Tetrahydrobiopterin (BH4) als Elektronen-Kofaktor. Im Phaeochromozytom ist dieses Enzym oft ueberexprimiert, was die exzessive Katecholaminproduktion erklaert.",
        difficulty = 5,
        funFact = "Das Metyrosin (alpha-Methyltyrosin) hemmt kompetitiv die Tyrosinhydroxylase und wird praeoperativ bei Phaeochromozytom eingesetzt, um die Katecholaminsynthese zu reduzieren und das Operationsrisiko (hypertensive Krisen) zu minimieren."
    ),

    // Question 10 - SDHB Mutation and Pheochromocytoma
    Question(
        categoryId = 16,
        questionText = "Welche Mutation ist bei hereditaeren Phaeochromozytomen/Paragangliomen mit der hoechsten Malignitaetsrate assoziiert, und welcher metabolische Effekt liegt zugrunde?",
        answerA = "VHL-Mutation; fuehrt zu HIF-1alpha-Stabilisierung und VEGF-Ueberexpression",
        answerB = "SDHB-Mutation; fuehrt zu Succinat-Akkumulation, Hemmung von alpha-KG-abhaengigen Dioxygenasen und pseudo-hypoxischem Phaenotyp",
        answerC = "RET-Mutation; fuehrt zu konstitutiver Tyrosinkinase-Aktivierung und Proliferation",
        answerD = "NF1-Mutation; fuehrt zu Ras-Ueberaktivierung durch Verlust der GTPase-Aktivierung",
        correctAnswer = 1,
        explanation = "SDHB kodiert die Eisen-Schwefel-Untereinheit B des Succinat-Dehydrogenase-Komplexes (Komplex II). Verlust fuehrt zu Succinat-Akkumulation, das als kompetitiver Inhibitor alpha-Ketoglutarat-abhaengiger Dioxygenasen (TET-Methylcytosindioxygenasen, PHD-Prolylhydroxylasen) wirkt. Die resultierende HIF-Stabilisierung und epigenetische Dysregulation erklaert die hohe Malignitaetsrate von 40%.",
        difficulty = 5,
        funFact = "SDH-mutierte Paragangliome zeigen ein charakteristisches 'Hypermethylierungsphaenotyp'-Epigenom -- das Succinat hemmt TET-Enzyme, die normalerweise methylierte DNA demethylieren. Dies ist ein Paradebeispiel, wie Metabolite direkt das Epigenom umprogrammieren."
    ),

    // Question 11 - Adrenocortical Carcinoma Staging
    Question(
        categoryId = 16,
        questionText = "Welcher Biomarker hat bei adrenokortikaelem Karzinom (ACC) den hoechsten positiven praediktiven Wert fuer Malignitat und wird im Weiss-Score verwendet?",
        answerA = "Cortisol im 24-Stunden-Urin > 3-fach erhoht",
        answerB = "Ki-67-Proliferationsindex > 5% in der Immunhistochemie",
        answerC = "Mitoserate > 5/50 HPF kombiniert mit atypischen Mitosen im Weiss-Score-System",
        answerD = "DHEA-S > 700 mcg/dL im Serum",
        correctAnswer = 2,
        explanation = "Der modifizierte Weiss-Score bewertet 9 histopathologische Kriterien; eine Mitoserate von mehr als 5 Mitosen pro 50 Hochleistungsfelder plus das Vorhandensein atypischer Mitosen gilt als staerkstes Einzelkriterium. Score >= 3 Punkte definiert Malignitat. Ki-67 > 5% wird im retiklaeren Weiss-Score (rWSS) ergaenzend verwendet, aber ist kein klassischer Weiss-Parameter.",
        difficulty = 5,
        funFact = "ACC ist eine seltene, aber aggressive Erkrankung mit einer 5-Jahres-Ueberlebensrate von nur 20-45% im Stadium III-IV. Der Mineralokortikoid-Synthesehemmer Mitotane ist das einzige spezifisch fuer ACC zugelassene Medikament -- er wurde urspruenglich als Insektizid-Derivat (DDT-Analogon) entdeckt."
    ),

    // --- METABOLIC SYNDROME & ADIPOKINES ---

    // Question 12 - Leptin Resistance
    Question(
        categoryId = 16,
        questionText = "Welcher molekulare Mechanismus erklaert zentralnervoes die Leptinresistenz bei Adipositas?",
        answerA = "Downregulation des ObRb-Leptin-Rezeptors im Hypothalamus durch Internalisierung",
        answerB = "SOCS3-vermittelte Hemmung der JAK2-STAT3-Signalkaskade sowie erhoehte PTP1B-Aktivitaet im Hypothalamus",
        answerC = "Verlust der Blut-Hirn-Schranken-Permeabilitaet fuer Leptin durch Oedembildung",
        answerD = "Kompetitive Hemmung durch erhoehte Insulinkonzentrationen am selben Rezeptor",
        correctAnswer = 1,
        explanation = "Leptin signalisiert ueber den ObRb-Rezeptor via JAK2-STAT3. Bei chronisch erhoehten Leptinspiegeln induziert STAT3 transkriptionell SOCS3, das JAK2 direkt hemmt (negative Rueckkopplungsschleife). Parallel wird die Phosphatase PTP1B hochreguliert, die JAK2 und den Insulinrezeptor dephosphoryliert. Beide Mechanismen attenuieren das anorexigene Leptinsignal und foerdern Leptinresistenz.",
        difficulty = 5,
        funFact = "Maeuse ohne hypothalamisches SOCS3 sind gegen diaetetisch induzierte Adipositas geschuetzt und zeigen verbessertes Leptin- und Insulinsignaling -- was SOCS3 zu einem potenziellen therapeutischen Ziel bei Adipositas macht."
    ),

    // Question 13 - Adiponectin and PPAR-gamma
    Question(
        categoryId = 16,
        questionText = "Welche Wirkung hat Adiponektin auf den hepatischen Fettstoffwechsel, und ueber welchen Rezeptor/Signalweg vermittelt es diese Wirkung?",
        answerA = "Adiponektin hemmt hepatische Lipogenese ueber AdipoR2 -> AMPK-Aktivierung -> ACC-Phosphorylierung -> verminderte Malonyl-CoA-Synthese",
        answerB = "Adiponektin stimuliert hepatische VLDL-Sekretion ueber AdipoR1 -> PKC-Aktivierung",
        answerC = "Adiponektin hemmt direkt PPAR-gamma im Fettgewebe ohne AMPK-Beteiligung",
        answerD = "Adiponektin aktiviert den Insulinrezeptor direkt als Agonist im hepatischen Gewebe",
        correctAnswer = 0,
        explanation = "Adiponektin bindet an AdipoR2 (vorwiegend hepatisch exprimiert) und aktiviert AMPK sowie PPARalpha. Aktiviertes AMPK phosphoryliert und inaktiviert Acetyl-CoA-Carboxylase (ACC), was den Malonyl-CoA-Spiegel senkt. Da Malonyl-CoA die Carnitin-Palmitoyltransferase 1 (CPT1) hemmt, steigt die mitochondriale Fettsaeure-Oxidation. Gleichzeitig werden lipogene Gene durch PPARalpha-Aktivierung supprimiert.",
        difficulty = 5,
        funFact = "Adiponektin ist das einzige Adipokin, dessen Plasmaspiegel bei Adipositas paradoxerweise erniedrigt ist -- trotz massiver Fettzellmasse. Hohe Adiponektinspiegel sind protektiv gegen Typ-2-Diabetes, kardiovaskulaere Erkrankungen und bestimmte Karzinome."
    ),

    // Question 14 - PPAR-gamma in Metabolic Syndrome
    Question(
        categoryId = 16,
        questionText = "Welcher Mechanismus erklaert die Insulinsensitivisierung durch Thiazolidindione (TZD) als PPAR-gamma-Agonisten?",
        answerA = "Direkte Aktivierung des Insulinrezeptors und Verstaerkung der IRS-1-Phosphorylierung",
        answerB = "PPAR-gamma-Aktivierung foerdert Differenzierung zu kleinen, insulinsensitiven Adipozyten und steigert Adiponektin-Genexpression waehrend FFA-Freisetzung sinkt",
        answerC = "Hemmung der hepatischen Gluconeogenese durch direkte PEPCK-Inhibition",
        answerD = "Aktivierung der Glukokinase in pankreatischen Beta-Zellen",
        correctAnswer = 1,
        explanation = "TZDs aktivieren PPAR-gamma, einen Transkriptionsfaktor der Adipogenese. Dies foerdert die Differenzierung von grossen, dysfunktionalen Adipozyten zu kleinen, metabolisch aktiven Zellen mit verbesserter Insulinsensitivitaet. Gleichzeitig steigt die Adiponektin-Sekretion, freie Fettsaeuren im Plasma sinken (verbesserte hepatische und muskulaere Insulinsensitivitaet), und pro-inflammatorische Adipokine wie TNF-alpha werden supprimiert.",
        difficulty = 5,
        funFact = "PPAR-gamma-Heterozygote Maeuse sind gegen hochfettdiaetkinduzierte Insulinresistenz geschuetzt -- ein paradoxer Befund der zeigt, dass eine leichte PPAR-gamma-Reduktion metabolisch protektiv sein kann. Dies hat das Konzept 'partieller PPAR-gamma-Agonisten' als bessere TZDs begruendet."
    ),

    // --- INBORN ERRORS OF METABOLISM ---

    // Question 15 - PKU Biochemistry
    Question(
        categoryId = 16,
        questionText = "Welcher Enzymdefekt liegt der klassischen Phenylketonurie (PKU) zugrunde, und warum fuehrt der Defekt zur Neurodegeneration?",
        answerA = "Mangel an Phenylalaninaminotransferase; Phenylalanin wird nicht in Alanin transamidiert",
        answerB = "Mangel an Phenylalaninhydroxylase (PAH); Phenylalanin akkumuliert, kompetitiv hemmt Tryptophan/Tyrosin-Transport ueber die Blut-Hirn-Schranke, vermindert Neurotransmittersynthese",
        answerC = "Mangel an Tyrosinhydroxylase; kein Dopamin aus Tyrosin synthetisierbar",
        answerD = "Mangel an BH4-Synthese; alle aromatischen Aminosaeuren akkumulieren gleichermassen",
        correctAnswer = 1,
        explanation = "PAH-Mangel fuehrt zu massiver Phenylalaninakkumulation. Phenylalanin konkurriert mit anderen grossen neutralen Aminosaeuren (Tyrosin, Tryptophan, Leucin) um denselben L-Aminosaeuren-Transporter 1 (LAT1) an der Blut-Hirn-Schranke. Der verminderte Tyrosin- und Tryptophantransport reduziert die zerebralen Dopamin- und Serotoninspiegel, was bei unbehandelter PKU zur schweren geistigen Behinderung fuehrt.",
        difficulty = 5,
        funFact = "Sapropterin (synthetisches BH4) ist fuer einen Teil der PKU-Patienten wirksam -- BH4 ist der Kofaktor von PAH und kann bei bestimmten Mutationen (BH4-responsive PKU) die residuale Enzymaktivitaet mehrfach steigern, obwohl BH4 selbst bei klassischer PKU nicht fehlt."
    ),

    // Question 16 - Maple Syrup Urine Disease
    Question(
        categoryId = 16,
        questionText = "Welche Reaktion ist bei der Ahornsirupkrankheit (MSUD) blockiert, und welche Aminosaeuren akkumulieren infolgedessen?",
        answerA = "Hemmung der Transaminierung von BCAA; alle Aminosaeuren erhoehen sich gleichermassen",
        answerB = "Defekt des verzweigtkettigen alpha-Ketosaure-Dehydrogenase-Komplexes (BCKDH); Leucin, Isoleucin, Valin und ihre alpha-Ketosauren akkumulieren",
        answerC = "Defekt der Carbamoylphosphatsynthetase I; Leucin kann nicht in den Harnstoffzyklus eingeschleust werden",
        answerD = "Mangel an Biotin; alle Carboxylase-Reaktionen sind beeintraechtigt",
        correctAnswer = 1,
        explanation = "BCKDH katalysiert den irreversiblen oxidativen Decarboxylierungsschritt der verzweigtkettigen alpha-Ketosauren (Derivate von Leucin, Isoleucin, Valin). Bei BCKDH-Defekt akkumulieren die drei verzweigtkettigen Aminosaeuren sowie ihre entsprechenden alpha-Ketosauren im Blut und Urin. Insbesondere Leucin und dessen Ketosaeure (alpha-Ketoisocapronsaeure) wirken hochtoxisch auf das ZNS.",
        difficulty = 5,
        funFact = "Der Geruch nach Ahornsirup im Urin entsteht durch Sotolon -- eine Verbindung, die bei Umsetzung der akkumulierten Ketosauren entsteht. Ohne Fruehdiagnose (Neugeborenenscreening) entwickelt sich innerhalb von Tagen eine lebensbedrohliche neonatale Enzephalopathie."
    ),

    // Question 17 - Galactosemia
    Question(
        categoryId = 16,
        questionText = "Welches Enzym ist bei der klassischen Galaktosaemie defizient, und durch welchen Mechanismus entstehen Katarakt und Leberversagen?",
        answerA = "Galaktose-1-Phosphat-Uridylyltransferase (GALT); Galaktose-1-Phosphat akkumuliert und hemmt Phosphoglucomutase und andere Enzyme",
        answerB = "Galaktokinase; Galaktitol wird durch Aldosereduktase gebildet und schaedigt Linse und Niere",
        answerC = "UDP-Galaktose-4-Epimerase; UDP-Galaktose akkumuliert und hemmt Glykosylierungsreaktionen",
        answerD = "Galaktose-Oxidase; Wasserstoffperoxid schaedigt durch oxidativen Stress direkt",
        correctAnswer = 0,
        explanation = "GALT-Mangel fuehrt zu Galaktose-1-Phosphat-Akkumulation. Gal-1-P hemmt kompetitiv die Phosphoglucomutase (blockiert Glykolyse und Glykogenolyse) und stoert UDP-Glucose-Regeneration. In der Linse wird Galaktose durch Aldosereduktase zu Galaktitol reduziert, das osmotisch wirkt und Linsenproteine schaedigt. Die hepatozellulare Toxizitaet durch Gal-1-P fuehrt zu Leberzirrhose.",
        difficulty = 5,
        funFact = "Neugeborene mit klassischer Galaktosaemie koennen trotz sofortigem Milchentzug bleibende neurologische Schaden und Ovarialinsuffizienz (bei Maedchen) entwickeln -- Gal-1-P bildet sich auch aus endogener Galaktose-Produktion, weshalb vollstaendige Expositionsvermeidung unmoeglich ist."
    ),

    // --- ADVANCED GI & IBD ---

    // Question 18 - Crohn vs UC Molecular Differences
    Question(
        categoryId = 16,
        questionText = "Welcher genetische Risikolokus unterscheidet Morbus Crohn am deutlichsten von Colitis ulcerosa auf molekularer Ebene, und welche Funktion hat das betroffene Gen?",
        answerA = "HLA-DQ2/DQ8 bei Crohn; kodiert fuer MHC-II-Praesentation von Gliadenpeptiden",
        answerB = "NOD2/CARD15 bei Morbus Crohn; kodiert einen intrazellullaeren Mustererkennungsrezeptor fuer Muramyldipeptid bakterieller Zellwaende",
        answerC = "IL-23R bei Colitis ulcerosa; fuehrt zu verminderter Th17-Differenzierung im Kolon",
        answerD = "CTLA4 bei Morbus Crohn; fuehrt zu unkontrollierter T-Zell-Aktivierung im Darm",
        correctAnswer = 1,
        explanation = "NOD2 (CARD15) ist der staerkste Einzelgen-Risikolokus fuer Morbus Crohn, nicht fuer UC. NOD2 erkennt intrazellullaer Muramyldipeptid (MDP) aus gram-positiven und gram-negativen Bakterien und aktiviert NF-kappaB. Mutationen (Frameshift R702W, G908R, 1007fs) stoeren die MDP-Erkennung und beeintraechtigen die Defensin-Sekretion durch Paneth-Zellen -- was die antibakterielle Barriere des Darms schwaecht.",
        difficulty = 5,
        funFact = "Interessanterweise foehrt NOD2-Mangel nicht zu einer verminderten Entzuendungsreaktion, wie man erwarten wuerde, sondern paradoxerweise zu einer erhoehten Entzuendung -- weil die gestorte Pathogenerkennung kompensatorisch andere proinflammatorische Pfade aktiviert."
    ),

    // Question 19 - IBD Cytokine Pathways
    Question(
        categoryId = 16,
        questionText = "Welches Zytokin-Profil charakterisiert die T-Helfer-Zell-Polarisation bei Morbus Crohn versus Colitis ulcerosa?",
        answerA = "Crohn: Th2-Profil (IL-4, IL-5, IL-13); UC: Th1-Profil (IFN-gamma, TNF-alpha)",
        answerB = "Crohn: Th1/Th17-Profil (IFN-gamma, IL-17, TNF-alpha, IL-12/IL-23); UC: atypisches Th2-Profil (IL-13, IL-5) mit NKT-Zell-Beteiligung",
        answerC = "Crohn und UC: beide ausschliesslich Th17-Profil mit identischem IL-23-Signaling",
        answerD = "Crohn: Treg-Profil (TGF-beta, IL-10); UC: Th1-Profil ohne IL-17-Beteiligung",
        correctAnswer = 1,
        explanation = "Morbus Crohn wird klassischerweise durch ein Th1-Profil (IL-12-getrieben, IFN-gamma, TNF-alpha) plus starke Th17-Komponente (IL-23-getrieben, IL-17A/F) charakterisiert. Colitis ulcerosa zeigt ein atypisches Th2-artiges Profil mit IL-13-Produktion durch NKT-Zellen, die direkt die Epithelbarriere schaedigen. Diese Unterschiede erklaeren teilweise, warum IL-12/23-Inhibitoren (Ustekinumab) bei Crohn besser wirken als bei UC.",
        difficulty = 5,
        funFact = "Die Entdeckung des IL-23/IL-17-Pfades hat die IBD-Therapie revolutioniert: Risankizumab (Anti-IL-23p19), Ustekinumab (Anti-IL-12/23p40) und IL-17-Inhibitoren sind neue Optionen. Paradoxerweise verschlimmern IL-17-Inhibitoren gelegentlich IBD -- IL-17 hat auch protektive Rollen an der Darmbarriere."
    ),

    // Question 20 - Gut Microbiome in IBD
    Question(
        categoryId = 16,
        questionText = "Welche Veraenderung des Darmmikrobioms (Dysbiose) ist bei aktiver Colitis ulcerosa am konsistentesten beschrieben?",
        answerA = "Erhoehung von Bifidobacterium und Lactobacillus bei Abnahme von E. coli",
        answerB = "Reduktion von Butyrat-produzierenden Bakterien (Faecalibacterium prausnitzii, Roseburia) und Erhoehung von Proteobacteria (Enterobacteriaceae)",
        answerC = "Vollstaendiger Verlust aller Firmicutes mit ausschliesslicher Kolonisierung durch Bacteroidetes",
        answerD = "Dominanz von Helicobacter pylori im Kolon als Haupttrigger der Entzuendung",
        correctAnswer = 1,
        explanation = "Das bei IBD konsistenteste Muster ist eine Reduktion butyrat-produzierender Firmicutes (Faecalibacterium prausnitzii, Roseburia intestinalis) und gleichzeitige Zunahme von Proteobacteria. Butyrat ist der primaere Energietraeger fuer Kolonozyten und hat anti-inflammatorische Wirkungen (hemmt NF-kappaB, foerdert Treg-Differenzierung via HDAC-Hemmung). Sein Fehlen beeintraechtigt die Epithelbarrierefunktion und die mukosale Immuntoleranzmechanismen.",
        difficulty = 5,
        funFact = "Faecalibacterium prausnitzii, eines der haeufigsten Darmbakterien Gesunder, wirkt anti-inflammatorisch und ist bei Crohn-Patienten stark reduziert. Patienten mit hohen F.-prausnitzii-Spiegeln nach Operation haben signifikant laengere Remissionsphasen -- das Bakterium ist ein aktives Forschungsziel als probiotische Therapie."
    ),

    // --- LIVER TRANSPLANT & MELD SCORE ---

    // Question 21 - MELD Score Calculation
    Question(
        categoryId = 16,
        questionText = "Welche drei Laborparameter fliessen in die urspruengliche MELD-Formel (Model for End-Stage Liver Disease) ein, und was misst der Score?",
        answerA = "ALT, AST, Bilirubin; misst hepatozellulaere Schaedigung",
        answerB = "Kreatinin, Bilirubin, INR; misst 90-Tage-Mortalitaet auf der Transplantationswarteliste",
        answerC = "Albumin, Prothrombinzeit, Aszitesgrad; entspricht dem Child-Pugh-Score",
        answerD = "Kreatinin, Natrium, Albumin; misst renale Komorbiditat bei Leberzirrhose",
        correctAnswer = 1,
        explanation = "Die MELD-Formel lautet: 3,78 x ln(Bilirubin mg/dL) + 11,2 x ln(INR) + 9,57 x ln(Kreatinin mg/dL) + 6,43. Der Score korreliert mit der 90-Tage-Mortalitaet auf der Warteliste und wird in den USA seit 2002 zur Organallokation verwendet. Der MELD-Na ergaenzt Serum-Natrium, da Hyponatriaemie unabhaengig die Mortalitaet vorhersagt.",
        difficulty = 5,
        funFact = "Der MELD-Score wurde urspruenglich nicht fuer die Transplantationsallokation entwickelt, sondern um die 30-Tage-Mortalitaet nach TIPS (transjugulaer intrahepatischer portosystemischer Shunt) vorherzusagen. Erst nach Validierungsstudien wurde er als Allokationssystem adoptiert."
    ),

    // Question 22 - Milan Criteria for HCC
    Question(
        categoryId = 16,
        questionText = "Welche exakten Kriterien definieren die Mailand-Kriterien fuer die Lebertransplantation beim hepatozellulaeren Karzinom (HCC), und welche 5-Jahres-Ueberlebensrate wird damit erzielt?",
        answerA = "Ein Tumor <= 6 cm ODER bis zu 4 Tumoren <= 4 cm; 5-JUL ca. 60%",
        answerB = "Ein Tumor <= 5 cm ODER bis zu 3 Tumoren <= 3 cm (alle <= 3 cm), ohne Gefaessinvasion und ohne Fernmetastasen; 5-JUL ca. 70-75%",
        answerC = "Jede Tumorgroe bei AFP < 200 ng/mL und negativem PET-CT; 5-JUL ca. 80%",
        answerD = "Bis zu 5 Tumoren unabhaengig von Groesse, sofern keine portale Hypertension besteht; 5-JUL ca. 65%",
        correctAnswer = 1,
        explanation = "Die Mailand-Kriterien (Mazzaferro 1996): singulaerer HCC <= 5 cm ODER maximal 3 Tumoren, jeder <= 3 cm, ohne makrovaskulaere Invasion und ohne extrahepatische Metastasen. Bei Einhaltung dieser Kriterien betraegt die 5-Jahres-Ueberlebensrate nach Transplantation 70-75% -- vergleichbar mit nicht-tumorbedingten Indikationen. Diese Kriterien haben die Transplantation fuer HCC legitimiert.",
        difficulty = 5,
        funFact = "Neuere Kriterien (UCSF-Kriterien, Up-to-7-Kriterien) sind grosszuegiger und zeigen aehnliche Ergebnisse. Die 'Up-to-7'-Regel besagt: wenn Anzahl der Tumoren plus Maximaldurchmesser in cm nicht mehr als 7 ergibt (ohne Gefaessinvasion), sind die Ergebnisse mit den Mailand-Kriterien vergleichbar."
    ),

    // --- PANCREATIC DISORDERS ---

    // Question 23 - CFTR in Pancreatitis
    Question(
        categoryId = 16,
        questionText = "Welcher Mechanismus erklaert, wie CFTR-Mutationen zur chronischen Pankreatitis fuehren, auch ohne vollstaendige zystische Fibrose?",
        answerA = "CFTR-Mutationen erhoehen direkt die Trypsinogenaktivierung durch Kalzium-Dysregulation",
        answerB = "CFTR reguliert HCO3--Sekretion im Pankreas; Mutationen reduzieren den Bikarbonatfluss, erhoehen die Proteinkonzentration im Pankreassekret und foerdern Steinbildung und Gangobstruktion",
        answerC = "CFTR-Mutationen aktivieren NF-kappaB direkt in Azinuszellen ohne Obstruktionskomponente",
        answerD = "CFTR ist ausschliesslich in Lunge und Schweissdruesen exprimiert -- Pankreatitis entsteht bei CF durch Mangelernaehrung",
        correctAnswer = 1,
        explanation = "Duktale Pankreaszellen exprimieren CFTR prominent. Normales CFTR vermittelt Cl- und HCO3--Sekretion; bei Mutation sinkt der HCO3--Transport (bis 140 mEq/L pH 8,2 im Normalfall). Das azidischere, viskoesere Sekret mit erhoehter Protein- und Kalziumkonzentration foerdert Proteinplugs und Gangobstruktion. Heterozygote CFTR-Mutationen erhoehen das idiopathische chronische Pankreatitis-Risiko um das 2-4-fache.",
        difficulty = 5,
        funFact = "Der CFTR-Modulator Ivacaftor verbessert nachweislich die Pankreasfunktion bei CF-Patienten mit bestimmten Gating-Mutationen -- erstmals kann exokrine Pankreasfunktion bei CF pharmakologisch verbessert werden, was den direkten kausalen Zusammenhang zwischen CFTR und Pankreassekretion beweist."
    ),

    // Question 24 - Chronic Pancreatitis Pathophysiology
    Question(
        categoryId = 16,
        questionText = "Welche Zellpopulation ist primaer fuer die Fibrosierung bei chronischer Pankreatitis verantwortlich, und durch welche Mediatoren wird sie aktiviert?",
        answerA = "Azinuszellen; aktiviert durch Alkohol-direkte Zytotoxizitaet",
        answerB = "Pankreatische Sternzellen (PSC); aktiviert durch TGF-beta, PDGF und oxidativen Stress zu Myofibroblasten",
        answerC = "Duktale Epithelzellen; aktiviert durch HIF-1alpha bei Gangobstruktion",
        answerD = "Makrophagen (M1-Typ); aktiviert durch Trypsin-induzierte NLRP3-Inflammasom-Aktivierung",
        correctAnswer = 1,
        explanation = "Pankreatische Sternzellen (PSC) sind ruhende, lipidspeichernde Zellen, die durch repetitive Schaden (Alkohol, Oxidativer Stress, autodigestive Enzyme) aktiviert werden. Aktivierte PSC transdifferenzieren zu alpha-SMA-positiven Myofibroblasten, die exzessiv Kollagen I/III und Fibronektin sezernieren. TGF-beta1 ist der staerkste Aktivierungsreiz; PDGF foerdert Proliferation und Migration. Dieser Prozess ist weitgehend irreversibel und fuehrt zur pankreatischen Fibrose.",
        difficulty = 5,
        funFact = "PSC wurden erst 1998 unabhaengig von zwei Gruppen (Bachem und Apte) als eigene Zellpopulation im Pankreas identifiziert -- relativ spaet, verglichen mit den besser bekannten hepatischen Sternzellen (Ito-Zellen), auf die PSC morphologisch und funktionell stark aehneln."
    ),

    // --- ADVANCED NUTRITION SCIENCE ---

    // Question 25 - Ketogenesis Regulation
    Question(
        categoryId = 16,
        questionText = "Welches Enzym gilt als Schrittmacher der hepatischen Ketogenese, und wie wird es reguliert?",
        answerA = "HMG-CoA-Reduktase; gehemmt durch Mevalonat und Statine",
        answerB = "HMG-CoA-Synthase (mitochondriale Isoform); transkriptionell induziert durch PPAR-alpha bei Fasten/Fetternaehrung, gehemmt durch Succinyl-CoA",
        answerC = "Beta-Hydroxybutyrat-Dehydrogenase; aktiviert durch NAD+/NADH-Verhaeltnis",
        answerD = "Acetoacetyl-CoA-Thiolase; reguliert durch intramitochodriales CoA",
        correctAnswer = 1,
        explanation = "Die mitochondriale HMG-CoA-Synthase (mHMGCS) kondensiert Acetyl-CoA und Acetoacetyl-CoA zu HMG-CoA -- dem Schritt, der die Ketogenese von der Cholesterinsynthese (zytosolische HMGCS) trennt. mHMGCS wird durch PPARalpha transkriptionell stark induziert (Fasten, Fettdiaet) und posttranskriptionell durch Succinyl-CoA gehemmt (Signal fuer ausreichende TCA-Substrate). Dies koppelt Ketogenese direkt an den Energiestatus.",
        difficulty = 5,
        funFact = "Ketonkoerper sind nicht nur Energiesubstrate -- Beta-Hydroxybutyrat hemmt den NLRP3-Inflammasom-Komplex und wirkt epigenetisch als Histon-Deacetylase-Inhibitor. Dies erklaert zumindest teilweise die anti-inflammatorischen und neuroprotektiven Effekte der ketogenen Diaet."
    ),

    // Question 26 - Gluconeogenesis Key Enzymes
    Question(
        categoryId = 16,
        questionText = "Welche vier Enzyme ermoeglichen die Gluconeogenese aus Pyruvat und umgehen dabei die irreversiblen Glykolyse-Schritte?",
        answerA = "Pyruvat-Carboxylase, PEPCK, Fruktose-1,6-Bisphosphatase, Glukose-6-Phosphatase",
        answerB = "Pyruvat-Kinase, PFK-1, Hexokinase, Glukose-6-Phosphatase",
        answerC = "Pyruvat-Dehydrogenase, Isocitrat-Dehydrogenase, PEPCK, Aldolase",
        answerD = "Malatenzym, PEPCK, PFK-2, Glukose-6-Phosphatase",
        correctAnswer = 0,
        explanation = "Die Gluconeogenese umgeht drei irreversible Glykolyseschritte mit vier speziellen Enzymen: (1) Pyruvat-Carboxylase wandelt Pyruvat zu Oxalacetat (Mitochondrium), (2) PEPCK decarboxyliert Oxalacetat zu PEP (Umgehung der Pyruvat-Kinase), (3) Fruktose-1,6-Bisphosphatase spaltet F-1,6-BP zu F-6-P (Umgehung von PFK-1), (4) Glukose-6-Phosphatase (ER-Membran) generiert freie Glukose fuer die Blutbahn.",
        difficulty = 5,
        funFact = "Metformin hemmt die hepatische Gluconeogenese primaer durch Hemmung des mitochondrialen Komplex I (Atmungskette), was das AMP/ATP-Verhaltnis erhoht und dadurch AMPK aktiviert -- AMPK phosphoryliert und inaktiviert CRTC2 (CREB-regulated transcription coactivator 2), einen Koaktivator der PEPCK- und G6Pase-Transkription."
    ),

    // Question 27 - Urea Cycle Disorders
    Question(
        categoryId = 16,
        questionText = "Welches Enzym des Harnstoffzyklus ist ausschliesslich mitochondrial lokalisiert, und welche Verbindung aktiviert es allosterisch?",
        answerA = "Argininosuccinat-Synthetase; aktiviert durch Glutamat",
        answerB = "Carbamoylphosphat-Synthetase I (CPS I); aktiviert durch N-Acetylglutamat (NAG)",
        answerC = "Ornithin-Transkarbamylase (OTC); aktiviert durch Citrullin",
        answerD = "Arginase I; aktiviert durch Mn2+-Ionen ohne allosterische Regulation",
        correctAnswer = 1,
        explanation = "CPS I katalysiert den ersten und geschwindigkeitsbestimmenden Schritt des Harnstoffzyklus in der mitochondrialen Matrix und kondensiert NH4+, HCO3- und 2 ATP zu Carbamoylphosphat. CPS I wird absolut durch N-Acetylglutamat (NAG) als allosterischen Aktivator benoetigt. NAG wird durch NAG-Synthase aus Acetyl-CoA und Glutamat gebildet und spiegelt den Proteinkatabolismus wider -- ein eleganter Regulationsmechanismus.",
        difficulty = 5,
        funFact = "Carglumsaeure (N-Carbamylglutamat) ist ein NAG-Analogon und kann NAG-Synthase-Defekte therapeutisch kompensieren -- eine der wenigen erfolgreichen Pharmakotherapien bei Harnstoffzyklusdefekten, bei der ein kleines Molekuel einen genetischen Defekt nahezu vollstaendig korrigiert."
    ),

    // --- RARE ENDOCRINE DISORDERS ---

    // Question 28 - MEN1 Syndrome
    Question(
        categoryId = 16,
        questionText = "Welches Gen ist bei MEN 1 mutiert, welche Tumore entstehen in der klassischen Trias, und was ist die Funktion des Genprodukts Menin?",
        answerA = "RET-Gen; Nebenschilddruese, Hypophyse, Pankreas; Menin ist eine Rezeptor-Tyrosinkinase",
        answerB = "MEN1-Gen (Chromosom 11q13); Nebenschilddruese (Hyperparathyreoidismus), Hypophysenvorderlappen-Adenome, enteropankreatische Tumore; Menin ist ein Tumorsuppressor im MLL-Histonmethyltransferase-Komplex",
        answerC = "VHL-Gen; Nebenniere, Kleinhirn, Retina; Menin reguliert HIF-1alpha-Degradation",
        answerD = "CDKN1B-Gen (MEN 4); Nebenschilddruese und Hypophyse; Menin ist ein CDK2-Inhibitor",
        correctAnswer = 1,
        explanation = "MEN 1 wird durch inaktivierende Mutationen im MEN1-Tumorsuppressorgen verursacht (Knudson 'two-hit'). Das Genprodukt Menin ist Bestandteil des KMT2A/MLL1-Histon-H3K4-Methyltransferase-Komplexes und reguliert Genexpression epigenetisch. Die klassische Trias: primaerer Hyperparathyreoidismus (90-100%), Hypophysenvorderlappenadenom (30-70%), enteropankreatische neuroendokrine Tumore (30-70%; Gastrinome fuehren zum Zollinger-Ellison-Syndrom).",
        difficulty = 5,
        funFact = "Menin-Inhibitoren sind derzeit in klinischen Studien bei akuter Leukaemie -- eine paradoxe Situation, wo derselbe Faktor, dessen Verlust Tumore foerdert, auch durch seinen Erhalt Leukaemie unterstuetzt. NPM1-mutierte AML und MLL-Fusions-Leukaemien sind Menin-abhaengig."
    ),

    // Question 29 - MEN 2B
    Question(
        categoryId = 16,
        questionText = "Welche klinischen Merkmale unterscheiden MEN 2B von MEN 2A, und welche RET-Mutation ist dafuer verantwortlich?",
        answerA = "MEN 2B: medullaeres Karzinom + Phaeochromozytom + Hyperparathyreoidismus; RET Codon 634",
        answerB = "MEN 2B: medullaeres Karzinom + Phaeochromozytom + Marfanoider Habitus + mukosale Neurinom + intestinale Ganglioneuromatose; KEIN Hyperparathyreoidismus; RET Codon 918 (M918T)",
        answerC = "MEN 2B: medullaeres Karzinom + Hyperparathyreoidismus + kutane Lichen amyloidosis; RET Codon 609",
        answerD = "MEN 2B: nur medullaeres Karzinom ohne andere Tumore; RET Codon 768",
        correctAnswer = 1,
        explanation = "MEN 2B entsteht durch M918T-Mutation in der Kinasedomaene von RET (Codon 918). Klinisch fehlt -- anders als bei MEN 2A -- der Hyperparathyreoidismus. Stattdessen: marfanoider Habitus (aber keine Linsenektopie), multiple mukosale Neurinome (Lippen, Zunge), intestinale Ganglioneuromatose (fuehrt oft zu Megakolon). MEN 2B ist die aggressivste Form -- medullaeres Karzinom manifestiert sich schon im Saeuglingsalter.",
        difficulty = 5,
        funFact = "Die M918T-Mutation veraendert die Substratspezifitaet der RET-Kinase, sodass sie nun Substrate bevorzugt, die normalerweise von zytoplasmatischen Src-Kinasen erkannt werden. Dies erklaert den abweichenden Phanaotyp mit neuronalen Manifestationen (Neurinom, Ganglioneuromatose) gegenueber MEN 2A."
    ),

    // Question 30 - Carney Complex
    Question(
        categoryId = 16,
        questionText = "Welches Gen ist beim Carney-Komplex mutiert, welche charakteristischen Manifestationen treten auf, und welcher biochemische Weg ist betroffen?",
        answerA = "NF1-Gen; Cafe-au-lait-Flecken, Neurofibrome, Phaeochromozytom; RAS-Signaling",
        answerB = "PRKAR1A-Gen; kardialer Myxom, Lentigines, primaeere pigmentierte nodulaere adrenokortikale Erkrankung (PPNAD), Akromegalie-Somatostatinom; cAMP-PKA-Signaling",
        answerC = "SDHB-Gen; Paragangliom, Phaeochromozytom, Nierenzelkarzinom; TCA-Zyklus-Defekt",
        answerD = "VHL-Gen; Haemangioblastom, Phaeochromozytom, klarzelliges Nierenzellkarzinom; HIF-Regulation",
        correctAnswer = 1,
        explanation = "Carney-Komplex entsteht durch inaktivierende Mutationen der PRKAR1A (regulatorische Untereinheit R1alpha der Proteinkinase A). PKA-R1alpha hemmt normalerweise die katalytische PKA-Untereinheit -- Verlust fuehrt zu konstitutiv aktivem cAMP/PKA-Signaling. Charakteristisch: kardialer Myxom (lebensbedrohlich), Lentigines (braune Hautflecken), PPNAD (Cushing-Syndrom durch bilateral-nodulaere Nebennierenhyperplasie), Somatropin-sezernierende Tumore.",
        difficulty = 5,
        funFact = "PPNAD ist eine ungewoehnliche Form des Cushing-Syndroms: ACTH-unabhaengig, bilateral-nodulaer, und paradoxerweise verschlechtert sich der Cortisolspiegel beim Dexamethason-Suppressionstest (paradoxe Stimulation) -- dieses Paradox ist diagnostisch wegweisend."
    ),

    // Question 31 - McCune-Albright Syndrome
    Question(
        categoryId = 16,
        questionText = "Welche somatische Mutation verursacht das McCune-Albright-Syndrom, und warum sind die Manifestationen fleckfoermig/asymmetrisch?",
        answerA = "Keimbahnmutation in GNAS1; ubiquitaere Aktivierung von Gs-alpha fuehrt zu symmetrischem Befall",
        answerB = "Postzygotische somatische Mosaik-Mutation in GNAS1 (R201C/H); aktivierendes Gs-alpha in einem Klon -- je nach Zeitpunkt der Mutation unterschiedliche Ausbreitung erklaert fleckfoermigen Befall",
        answerC = "Keimbahnmutation in GNAS2; betrifft nur Knochen und Haut ohne endokrine Beteiligung",
        answerD = "Somatische Deletion von APC; foerdert Wnt-Aktivierung in fibroblastaeren Vorlaeufer-zellen",
        correctAnswer = 1,
        explanation = "McCune-Albright entsteht durch postzygotische (somatische) aktivierende Missense-Mutationen R201C oder R201H in GNAS1 (kodiert Gs-alpha). Diese hemmen die intrinsische GTPase-Aktivitaet von Gs-alpha, was zu konstitutiver Adenylylcyclase-Aktivierung fuehrt. Da die Mutation postzygotisch in einem Klon entsteht, verteilen sich betroffene Zellen mosaik-artig -- das erklaert die asymmetrischen Cafe-au-lait-Flecken (Coast-of-Maine), fibrose Knochen-dysplasie und ACTH-unabhaengige gonadale Pubertas praecox.",
        difficulty = 5,
        funFact = "Bei McCune-Albright-Maedchen kann die Pubertas praecox mit Oestrogen-sezernierenden Ovarialzysten beginnen -- Zysten entstehen, weil konstitutives cAMP-Signaling in Granulosa-Zellen die FSH-Wirkung nachahmmt. Interessanterweise rezidivieren die Zysten, wenn Inhibin-B wieder absinkt."
    ),

    // --- ADDITIONAL ADVANCED TOPICS ---

    // Question 32 - Thyroid Hormone Synthesis
    Question(
        categoryId = 16,
        questionText = "Welcher Schritt der Schilddruesenhormon-Biosynthese wird durch Thionamide (Methimazol, PTU) blockiert, und durch welchen zusaetzlichen Mechanismus wirkt PTU besonders bei thyreotoxischer Krise?",
        answerA = "Hemmung der Natrium-Jodid-Symporter-Aktivitaet; PTU hat keinen Zusatzeffekt",
        answerB = "Hemmung der Thyreoperoxidase (Jodisation und Kopplungsreaktion); PTU hemmt zusaetzlich die periphere T4-zu-T3-Konversion durch Dejodinase-Hemmung",
        answerC = "Hemmung der Thyreoglobulin-Synthese; PTU blockiert spezifisch die Proteasefreisetzung von T3/T4",
        answerD = "Blockade von TSH-Rezeptor-Signaling; PTU hat direkte anti-TSH-Rezeptor-Antikoepereffekte",
        correctAnswer = 1,
        explanation = "Thionamide hemmen Thyreoperoxidase (TPO), die Jod organifiziert und Jodtyrosinreste zu T3/T4 koppelt. PTU (Propylthiouracil) hat zusaetzlich den wichtigen Effekt, die 5'-Dejodinase Typ 1 zu hemmen, die T4 peripher zu T3 konvertiert. Bei thyreotoxischer Krise ist dieser Zusatzeffekt klinisch relevant, weshalb PTU in dieser Notfallsituation bevorzugt wird.",
        difficulty = 5,
        funFact = "Jodid in hohen Dosen (Lugolsche Loesung) hemmt paradoxerweise die Schilddruesenhormonsekretion (Wolff-Chaikoff-Effekt) -- zunaechst durch Hemmung der TPO, dann durch Adaptationsdurchbruch nach 1-2 Wochen. Dieser Effekt wird praeoperativ zur Vaskularisationsreduktion der Schilddruese genutzt."
    ),

    // Question 33 - Primary Hyperaldosteronism
    Question(
        categoryId = 16,
        questionText = "Welche somatischen Mutationen sind bei Aldosteron-produzierenden Adenomen (APA) am haeufigsten beschrieben, und wie fuehren sie zur Aldosteronueberproduktion?",
        answerA = "BRAF-V600E; aktiviert MAPK -> CYP11B2-Ueberexpression",
        answerB = "Gain-of-function-Mutationen in KCNJ5 (Kaliumkanal Kir3.4) und CACNA1D (L-Typ Ca2+-Kanal); fuehren zu Membrandepolarisation und konstitutivem Ca2+-Einstrom -> CYP11B2-Aktivierung",
        answerC = "Inaktivierende Mutationen in NR3C2 (Mineralokortikoidrezeptor); Feedback-Verlust",
        answerD = "ATM-Mutation; fuehrt zu genomischer Instabilitaet und Aldosteron-Synthase-Amplifikation",
        correctAnswer = 1,
        explanation = "In APA finden sich am haeufigsten gain-of-function-Mutationen in KCNJ5 (ca. 40%), das einen Kaliumkanal kodiert. Mutationen aendern die Ionenselektivitaet -- der Kanal leitet Na+ statt K+, was zur Depolarisation und Ca2+-Einstrom fuehrt. Ca2+ aktiviert CYP11B2 (Aldosteron-Synthase) via CaM-Kinase. Auch CACNA1D-Mutationen (L-Typ-Ca2+-Kanal, direkte Aktivierung) und ATP1A1/ATP2B3-Mutationen kommen vor.",
        difficulty = 5,
        funFact = "Die Entdeckung der KCNJ5-Mutationen bei APA im Jahr 2011 durch Choi et al. (Science) war ein Durchbruch -- sie erklaerte erstmals den Mechanismus der autonomen Aldosteronsekretion auf molekularer Ebene und eroeffnet perspektivisch neue pharmakologische Angriffspunkte."
    ),

    // Question 34 - Cushing Syndrome Differential
    Question(
        categoryId = 16,
        questionText = "Welche Untersuchung ermoeglicht die zuverlaessigste Differenzierung zwischen ACTH-abhaengigem Cushing-Syndrom durch Hypophysenadenom (Morbus Cushing) und ektoper ACTH-Produktion?",
        answerA = "Hochdosis-Dexamethason-Suppressionstest; Suppression > 50% spricht sicher fuer Morbus Cushing",
        answerB = "Bilateraler Sinus-petrosus-inferior-Katheter (BIPSS) mit CRH-Stimulation; Gradient > 2 basal oder > 3 nach CRH spricht fuer Hypophysenquelle",
        answerC = "Serum-ACTH-Spiegel > 200 pg/mL spricht eindeutig fuer ektope Produktion",
        answerD = "MRT der Hypophyse; kein sichtbares Adenom schliesst Morbus Cushing aus",
        correctAnswer = 1,
        explanation = "BIPSS misst ACTH simultan im Sinus petrosus inferior (drainiert Hypophyse) und peripher. Zentral-zu-peripher-Gradient > 2 (basal) oder > 3 (nach CRH-Stimulation) beweist Hypophysenquelle mit Sensitivitaet/Spezifitaet > 95%. Der Hochdosis-DST hat deutlich schlechtere Diskrimination (manche ektopen Quellen supprimieren, manche Hypophysenadenome nicht), und ACTH-Spiegel ueberlappen erheblich.",
        difficulty = 5,
        funFact = "Kleinzellige Bronchialkarzinome und Karzinoid-Tumore sind die haeufigsten Ursachen ektoper ACTH-Produktion. Bei kleinzelligem Karzinom sind die ACTH-Spiegel extrem hoch, Hypokalaemie stark ausgepraegte und die Entstehung ist rasant -- die klassischen klinischen Cushing-Zeichen fehlen haeufig wegen des akuten Verlaufs."
    ),

    // Question 35 - Autoimmune Polyglandular Syndrome
    Question(
        categoryId = 16,
        questionText = "Welche Mutation verursacht das Autoimmune Polyglandulaere Syndrom Typ 1 (APS-1/APECED), und welches immunologische Prinzip ist betroffen?",
        answerA = "CTLA4-Mutation; fuehrt zu T-Zell-Ueberaktivierung durch fehlende Kostimulationshemmung",
        answerB = "AIRE-Gen-Mutation (Autoimmune Regulator); fuehrt zu Ausfall der zentralen Toleranz -- Thymusepithelialen praesenten keine peripheren Antigene mehr, autoreaktive T-Zellen werden nicht deletiert",
        answerC = "STAT3-gain-of-function; fuehrt zu Th17-Ueberaktivierung und autoimmuner Organschaedigung",
        answerD = "RAG1/RAG2-Mutation; fuehrt zu inkompletter V(D)J-Rekombination und autoreaktivem B-Zell-Repertoire",
        correctAnswer = 1,
        explanation = "AIRE (Autoimmune Regulator) wird in medullaeren Thymusepithelzellen exprimiert und steuert die ektopische Transkription gewebespezifischer Antigene (z.B. Insulin, TSH-Rezeptor). Autoreaktive T-Zellen, die diese Antigene erkennen, werden durch negative Selektion deletiert. AIRE-Mutationen unterbinden diese zentrale Toleranzinduktion -- autoreaktive T-Zellen entkommen in die Peripherie und greifen multiple Organe an (Nebenniere, Parathyreoidea, Gonaden, Darm).",
        difficulty = 5,
        funFact = "APS-1 ist auch bekannt als APECED (Autoimmune PolyEndocrinopathy-Candidiasis-Ectodermal Dystrophy) -- die Candida-Suszeptibilitaet entsteht durch spezifische Antikoeaper gegen Th17-Zytokine (IL-17A, IL-17F, IL-22), die AIRE-Defiziente produzieren und die antiinfektoese Immunitaet an Schleimhaeuten schwaechend."
    ),

    // Question 36 - Growth Hormone Resistance (Laron Syndrome)
    Question(
        categoryId = 16,
        questionText = "Welcher Defekt liegt beim Laron-Syndrom vor, welcher Laborparameter ist charakteristisch erniedrigt, und warum sind Laron-Patienten moeglicherweise vor bestimmten Erkrankungen geschuetzt?",
        answerA = "GH-Mangel durch Hypophysenaplasie; erniedrigtes IGF-1; keine bekannte Schutzfunktion",
        answerB = "Defekt des GH-Rezeptors (GHR); extrem niedriges IGF-1 trotz erhoehtem GH; moeglicherweise Schutz vor Krebserkrankungen und Diabetes mellitus durch vermindertes IGF-1/Insulin-Signaling",
        answerC = "GHRH-Rezeptor-Defekt; erniedrigtes IGF-1 und erniedrigtes GH; kein Schutzeffekt bekannt",
        answerD = "IGF-1-Rezeptor-Resistenz; normales IGF-1 und normales GH; klinisch dem Diabetes aehnlich",
        correctAnswer = 1,
        explanation = "Laron-Syndrom ist eine autosomal-rezessive GHR-Mutation -- Wachstumshormon kann nicht signalisieren, weshalb hepatisches IGF-1 trotz kompensatorisch erhoehtem GH extrem niedrig ist. Epidemiologische Studien (Guevara-Aguirre, Ecuador) zeigen, dass Laron-Patienten keine Krebsfaelle und keinen Typ-2-Diabetes entwickeln. Dies stuetzt die Theorie, dass IGF-1/Insulin-Signaling ein Schluessel-Promotor von Kanzerogenese und Insulinresistenz ist.",
        difficulty = 5,
        funFact = "Mecasermin (rekombinantes IGF-1) ist die einzige kausale Therapie beim Laron-Syndrom -- ein Medikament, das genau den fehlenden Downstream-Effektor ersetzt. Die Laron-Kohorte in Ecuador liefert bis heute einzigartige Einblicke in die Biologie von IGF-1 beim Menschen."
    ),

    // Question 37 - Congenital Adrenal Hyperplasia
    Question(
        categoryId = 16,
        questionText = "Welche biochemische Konstellation charakterisiert den klassischen 21-Hydroxylase-Mangel (salzverlierende Form), und warum kommt es zur Hyperandrogenaemie?",
        answerA = "Erhoehtes Cortisol, erniedrigtes ACTH, erniedrigtes 17-OH-Progesteron; Androgene aus Cholesterin",
        answerB = "Erniedrigtes Cortisol und Aldosteron, erhoehtes ACTH und 17-OH-Progesteron; Androgen-Vorstufen akkumulieren und werden ueber adrenale Androgen-Synthese-Pfade zu DHEA und Androstendion umgeleitet",
        answerC = "Erhoehtes Cortisol, erniedrigtes Aldosteron, normales ACTH; Androgen-Erhoehung durch Feedback-Verlust",
        answerD = "Normales Cortisol durch Kompensation, erhoehtes Progesteron, ACTH nur minimal erhoht",
        correctAnswer = 1,
        explanation = "CYP21A2 (21-Hydroxylase) konvertiert 17-OH-Progesteron zu 11-Desoxycortisol (Cortisol-Vorstufe) und Progesteron zu Desoxycorticosteron (Aldosteron-Vorstufe). Bei Defekt akkumulieren 17-OH-Progesteron und andere Vorstufen. Der Cortisolmangel hebt den Feedback-Feedback auf den Hypothalamus/Hypophyse auf -> exzessiv hohes ACTH -> adrenale Hyperplasie -> Kanalisation der Vorstufen in den Androgen-Pfad (17,20-Lyase via CYP17A1) -> DHEA, Androstendion, Testosteron.",
        difficulty = 5,
        funFact = "21-Hydroxylase-Mangel ist der haeufigste angeborene Stoffwechseldefekt der Nebenniere (1:14.000 Geburten fuer die klassische Form) und der haeufigste Grund fuer weiblichen Pseudohermaphroditismus. Das Neugeborenenscreening misst 17-OH-Progesteron im Trockenblut und hat die Mortalitaet durch nicht erkannte Salzverlustkrise dramatisch gesenkt."
    ),

    // Question 38 - Mitochondrial Fatty Acid Oxidation
    Question(
        categoryId = 16,
        questionText = "Welches Enzym katalysiert den ersten Schritt der mitochondrialen Beta-Oxidation nach CPT2-vermitteltem Acyl-CoA-Transfer, und welche Erkrankung entsteht bei seinem Defekt?",
        answerA = "Acyl-CoA-Dehydrogenase (VLCAD/MCAD/SCAD je nach Kettenlaenge); MCAD-Mangel ist haeufigstes FAO-Defekt-Syndrom mit Hypoketotischer Hypoglykamie",
        answerB = "Enoyl-CoA-Hydratase; fuehrt zu Reye-Syndrom-aehnlichem Bild",
        answerC = "L-3-Hydroxyacyl-CoA-Dehydrogenase (LCHAD); fuehrt zu Carnitin-Transporter-Defekt",
        answerD = "Thiolase; fuehrt zur 3-Hydroxy-3-Methylglutaryl-CoA-Lyase-Krankheit",
        correctAnswer = 0,
        explanation = "Acyl-CoA-Dehydrogenasen katalysieren die erste Reaktion der Beta-Oxidation (FAD-abhaengige Dehydrierung zu trans-Enoyl-CoA), jeweils substratspezifisch fuer verschiedene Kettenlaengen: VLCAD (C14-C20), MCAD (C6-C12), SCAD (C4-C6). MCAD-Mangel (Haeufigkeit 1:10.000-20.000) ist der haeufigste FAO-Defekt und manifestiert sich als hypoketotische Hypoglykamie bei laengerem Fasten -- ohne Fettoxidation kann keine Ketose als Glukosealternative gebildet werden.",
        difficulty = 5,
        funFact = "MCAD-Mangel ist durch Neugeborenenscreening (Tandem-Massenspektrometrie: C8-Acylcarnitin erhoht) fruehzeitig erkennbar. Vor Einfuehrung des Screenings starben bis zu 25% der betroffenen Kinder bei der ersten hypolgetotischen Krise -- heute ist durch einfache Praevention (kein langer Fastenabstand) die Prognose exzellent."
    ),

    // Question 39 - Homocystinuria
    Question(
        categoryId = 16,
        questionText = "Durch welchen Enzymmangel entsteht die klassische Homozystinurie, und warum fuehrt die Akkumulation von Homocystein zu kardiovaskulaeren Ereignissen?",
        answerA = "Methionin-Adenosyltransferase-Mangel; Methionin akkumuliert und ist direkt endothelotoxisch",
        answerB = "Cystathionin-beta-Synthase (CBS)-Mangel; Homocystein aktiviert Endothel-Apoptose, hemmt NO-Synthese, foerdert oxidativen Stress und induziert Hyperkoagulabilitaet durch Thrombozytenaktivierung",
        answerC = "Methylentetrahydrofolat-Reduktase (MTHFR)-Mangel; Tetrahydrofolat fehlt fuer DNA-Synthese",
        answerD = "Methionin-Synthase-Mangel; Cobalamin kann nicht fuer Homocystein-Remethylierung genutzt werden",
        correctAnswer = 1,
        explanation = "CBS kondensiert Homocystein mit Serin zu Cystathionin (transsulfurierungsweg). Fehlt CBS, akkumuliert Homocystein massiv. Erhoehtes Homocystein schgaedigt Endothelzellen direkt (steigert ROS-Bildung, mindert NO-Bioverfuegbarkeit), aktiviert Gewebefaktor und hemmt Antikoagulantien (Protein C, Antithrombin). Resultat: erhoehtes atherothrombotisches und venoesonptes Thrombose-Risiko.",
        difficulty = 5,
        funFact = "Hohe Dosen Pyridoxin (Vitamin B6) -- Kofaktor von CBS -- sind bei ca. 50% der CBS-Defekte wirksam (B6-responsive Form), da sie residuale CBS-Aktivitaet steigern. Der marfanoide Habitus (Linsenluxation, Skelettanomalien) bei Homozystinurie erklaert sich durch Stoerung der Kollagen- und Fibrillin-Vernetzung durch akkumuliertes Homocystein."
    ),

    // Question 40 - Wilson Disease Copper Metabolism
    Question(
        categoryId = 16,
        questionText = "Welches Gen ist bei Morbus Wilson mutiert, und welcher Mechanismus erklaert die Kupferakkumulation in Leber, Gehirn und Kornea?",
        answerA = "HMOX1 (Haem-Oxygenase); Kupfer wird nicht in Haemproteinen gebunden und akkumuliert frei",
        answerB = "ATP7B (P-Typ-Cu2+-ATPase); fehlendes Enzym kann Kupfer nicht in Galle sezernieren und nicht in Caeruloplasmin inkorporieren, sodass Kupfer sich in Geweben ansammelt",
        answerC = "CP (Caeruloplasmin); Kupfer kann nicht an Traegerprotein gebunden werden und praezipitiert",
        answerD = "SLC31A1 (Kupfertransporter CTR1); Ueberaufnahme von Kupfer aus dem Duenndarm",
        correctAnswer = 1,
        explanation = "ATP7B ist eine hepatisch exprimierte P-Typ-ATPase, die Kupfer in zwei Richtungen pumpt: in den trans-Golgi-Apparat zur Caeruloplasmin-Beladung und in kanalikulaere Vesikel zur biliaren Kupferexkretion. Beim Wilson-Patienten fehlt dieser Abtransport -- Kupfer akkumuliert zunaechst hepatisch, dann (nach Leberschaeden und Freisetzung) auch in Basalganglien (neurologische Symptome) und Descemet-Membran der Kornea (Kayser-Fleischer-Ring).",
        difficulty = 5,
        funFact = "D-Penicillamin, der klassische Wilson-Chelator, hat einen unerwarteten Wirkmechanismus: Es chelatiert Kupfer nicht nur im Serum, sondern induziert auch hepatische Metallothionein-Synthese, die ueberschuessiges Kupfer sequestriert. Neuere Therapien wie Trientine und Zink (hemmt intestinale Kupferaufnahme durch Metallothionein-Induktion in Enterozyten) haben guenstigere Nebenwirkungsprofile."
    ),

    // Question 41 - Nonalcoholic Steatohepatitis
    Question(
        categoryId = 16,
        questionText = "Welche molekulare Kaskade erklaert beim NASH (nichtalkoholische Steatohepatitis) den Uebergang von simpler Steatose zur Entzuendung und Fibrose ('Two-hit'-Modell)?",
        answerA = "Erster Hit: Lipidakkumulation; zweiter Hit: mitochondriale Dysfunktion und oxidativer Stress aktivieren JNK, NF-kappaB, NLRP3 -- entzuendliche Zytokine aktivieren hepatische Sternzellen",
        answerB = "Erster Hit: Insulinresistenz; zweiter Hit: bakterielle Endotoxine aus dem Darm aktivieren direkt Apoptose ohne Entzuendungskomponente",
        answerC = "Erster Hit: Adiponektinmangel; zweiter Hit: unkontrollierte Fettsaeureperoxidation durch erhoehte Aldehydproduktion",
        answerD = "Erster Hit: Lebersteatose; zweiter Hit: erhoehte TNF-alpha-Ausschuettung aus viszeralem Fettgewebe triggert Hepatozyt-Seneszenz",
        correctAnswer = 0,
        explanation = "Beim 'Two-hit'-Modell (Day & James 1998): Erster Hit ist hepatische Lipidakkumulation durch Insulinresistenz/erhoehten FFA-Fluss. Dieser macht die Leber vulnerabel fuer einen zweiten Hit: mitochondrialer Stress, reaktive Sauerstoffspezies (ROS), lipotoxische Lipidspezies (Ceramide, Diacylglycerol) aktivieren JNK1/2 (Hepatozytenapoptose), NF-kappaB (Entzuendung) und das NLRP3-Inflammasom (IL-1beta, IL-18). Entzuendliche Mediatoren und Apoptosesignale aktivieren dann pankreatische Sternzellen zur Fibrose.",
        difficulty = 5,
        funFact = "Das 'Two-hit'-Modell wird zunehmend durch das 'Multiple Parallel Hits'-Modell ersetzt: Darmmikrobiom-Dysbiose, genetische Faktoren (PNPLA3-I148M-Polymorphismus), erhoehte Darmpermeabilitaet und Endotoxaemie treffen gleichzeitig auf ein lipid-beladenes Hepatozyten-Milieu -- kein einzelner zweiter Hit, sondern ein Streuer."
    ),

    // Question 42 - Primary Biliary Cholangitis
    Question(
        categoryId = 16,
        questionText = "Gegen welches mitochondriale Antigen sind die charakteristischen Anti-Mitochondrialen-Antikoeaper (AMA) bei primaerer bilaerer Cholangitis (PBC) gerichtet, und welche Untereinheit ist am spezifischsten?",
        answerA = "Komplex I der Atmungskette (NADH-Dehydrogenase); Untereinheit ND1",
        answerB = "Pyruvat-Dehydrogenase-Komplex (PDC); E2-Untereinheit (Dihydrolipoamid-Acetyltransferase) -- AMA-M2",
        answerC = "ATP-Synthase (Komplex V); beta-Untereinheit des F1-Fragments",
        answerD = "Cytochrom-c-Oxidase (Komplex IV); Untereinheit COX2",
        correctAnswer = 1,
        explanation = "AMA bei PBC richten sich in 90-95% der Faelle gegen die E2-Untereinheit des Pyruvat-Dehydrogenase-Komplexes (PDC-E2, Dihydrolipoamid-Acetyltransferase) -- dies ist die AMA-M2-Spezifitaet, diagnostisch wegweisend. PDC-E2 wird auf der Oberflaeche von biliaren Epithelzellen (Cholangiozyen) in unveraenderter Form exprimiert, was als Trigger der zellullaeren Immunantwort gilt.",
        difficulty = 5,
        funFact = "Das Paradox der PBC: AMA-M2 ist hoechst spezifisch (>95%) und sensitiv (>95%), aber kein direkter Pathomechanismus -- passive AMA-Uebertragung auf gesunde Tiere erzeugt keine Krankheit. Die biliare Destruktion wird hauptsaechlich durch CD8+ T-Zellen vermittelt, die PDC-E2-praesentierende Cholangiozyen direkt angreifen."
    ),

    // Question 43 - Pancreatic Neuroendocrine Tumors
    Question(
        categoryId = 16,
        questionText = "Welcher Mechanismus erklaert das Whipple-Trias beim Insulinom, und welcher biochemische Test hat die hoechste Sensitivitaet fuer die Diagnose?",
        answerA = "Insulinom sezerniert intermittierend Glukagon; Whipple-Trias: Hypoglykamie-Symptome, BZ < 55 mg/dL, Besserung durch Glukagon",
        answerB = "Insulinom sezerniert autonom Insulin unabhaengig vom Blutglukosespiegel; Whipple-Trias: neuroglykopenische/adrenergiesymptome, Plasmaglukose < 55 mg/dL bei Symptomen, Symptomlinderung durch Glukosegabe; 72-Stunden-Fasten-Test mit C-Peptid-Messung",
        answerC = "Insulinom hemmt Glukagon-Sekretion; diagnostisch am sensibelsten: IGF-2/IGF-1-Quotient > 10",
        answerD = "Insulinom produziert Proinsulin-Fragment; diagnostisch am sensibelsten: Serum-Proinsulin isoliert ohne C-Peptid",
        correctAnswer = 1,
        explanation = "Insulinome sezernieren Insulin autonom, unabhaengig von Glukose (fehlende Suppression bei Hypoglykamie). Das Whipple-Trias beschreibt: (1) Hypoglykamie-assoziierte Symptome, (2) dokumentierte Plasmaglukose < 55 mg/dL waehrend der Symptome, (3) Symptombesserung nach Glukosezufuhr. Der 72-Stunden-Fasten-Test (bei 90% der Insulinome innerhalb 48h positiv) misst Insulin, C-Peptid und Proinsulin bei Hypoglykamie -- C-Peptid-Erhoehung schltiesst exogenes Insulin-Faking aus.",
        difficulty = 5,
        funFact = "Bei schwerem Insulinom-Verdacht gibt es eine diagnostische Besonderheit: Betahydroxybutyrat sollte bei echter Hypoglykamie erniedrigt sein (da Insulin die Lipolyse/Ketogenese hemmt). Ein normales oder erhoehtes Betahydroxybutyrat bei Hypoglykamie spricht GEGEN Insulin-vermittelte Hypoglykamie -- eleganter biochemischer Marker."
    ),

    // Question 44 - Zollinger-Ellison Syndrome
    Question(
        categoryId = 16,
        questionText = "Welcher pathophysiologische Mechanismus erklaert die therapierefraktaere Diarrhoe beim Zollinger-Ellison-Syndrom (ZES) neben dem erhoehten Magenaeureoutput?",
        answerA = "Gastrin stimuliert direkt kolonische Chloridsekretion via CFTR",
        answerB = "Exzessive HCl inaktiviert pankreatische Lipasen und Proteasen im Duodenum, fuehrt zu Steatorrhoe und osmotischer Diarrhoe; Gastrin selbst hat prokinetische Wirkung und erhoht Darmperistaltik",
        answerC = "Gastrin hemmt den intestinalen Natrium-Glukose-Kotransporter SGLT1",
        answerD = "Erhoehtes Gastrin induziert intestinale VIP-Sekretion aus enterischen Nervenzellen",
        correctAnswer = 1,
        explanation = "Beim ZES fuehrt massiver HCl-Output (> 15 mEq/h basal moeglich) zur Azidifizierung des Duodenum-Jejunum-Milieus. Dies inaktiviert pankreatische Lipasen (Optimum pH 7-8) und precipitiert Gallensaeure, was Steatorrhoe verursacht. Zusaetzlich beschaedigt das saure Milieu direkt das duodenale Epithel (Ulzera bis in den Jejunum). Gastrin hat auch direkte prokinetische Wirkung am Gastrin-Rezeptor im Darm.",
        difficulty = 5,
        funFact = "ZES tritt in 20-30% der Faelle im Rahmen von MEN 1 auf -- Gastrinome sind die haeufigsten pankreatischen Tumore bei MEN 1. Das Gastri-norm bei MEN 1 sitzt oft im 'Gastrinomdreieck' (Zystikuseinmuendung, Duodenum, Pankreaskopf), und die Lokalisation mit Endosonographie ist entscheidend fuer die Operationsstrategie."
    ),

    // Question 45 - Short Bowel Syndrome
    Question(
        categoryId = 16,
        questionText = "Welcher Adaptationsmechanismus ermoeglicht nach ausgedehnter Duenndarmsektierung (Kurzdarmsyndrom) eine verbesserte Absorption im verbleibenden Darm, und welches Hormon ist massgeblich beteiligt?",
        answerA = "GLP-2 (Glucagon-like Peptide 2) induziert Enterozyten-Proliferation, verlaengert Zotten und vertieft Krypten -- intestinale Adaptation durch strukturelles Wachstum",
        answerB = "GLP-1 stimuliert Beta-Zell-Regeneration und verbessert so indirekt die Naehrstoffverwertung",
        answerC = "CCK (Cholecystokinin) verlaengert die Darmtransitzeit und erhoht so Absorptionszeit",
        answerD = "IGF-1 stimuliert direkt Kolonozyten-Proliferation und ersetzt verlorenes Duenndarmgewebe",
        correctAnswer = 0,
        explanation = "GLP-2 wird aus L-Zellen des Ileums und Kolons nach Nahrungsaufnahme sezerniert und bindet an GLP-2-Rezeptoren in Enterozyten und subepithelialem Mesenchym. GLP-2 steigert Kryptenzellproliferation, reduziert Enterozytenapoptose, erhoht die Zottenhoehe und vertieft Krypten -- die intestinale Mukosaflaeche vergroessert sich. Teduglutid (Dipeptidylpeptidase-4-resistentes GLP-2-Analogon) ist als Therapie beim Kurzdarmsyndrom zugelassen.",
        difficulty = 5,
        funFact = "Teduglutid (Revestive/Gattex) reduziert bei Kurzdarmsyndrom-Patienten den parenteralen Ernaehrungsbedarf um durchschnittlich 4-5 Tage pro Woche -- ein Meilenstein, da parenterale Ernaehrung lebensbedrohliche Komplikationen (Katheter-Sepsis, Leberfibrose) birgt."
    ),

    // Question 46 - Hereditary Hemochromatosis
    Question(
        categoryId = 16,
        questionText = "Wie reguliert das HFE-Protein normalerweise die Eisenhomoeostase, und welche Mutation fuehrt zur hereditaeren Haemochromatose Typ 1?",
        answerA = "HFE kompetiert mit Transferrin um TfR1-Bindung; C282Y-Mutation verhindert HFE-TfR1-Interaktion, was zu ubiquitaerer Eisenaufnahme ohne Regulationspause fuehrt",
        answerB = "HFE aktiviert Hepcidin-Transkription in der Leber; C282Y-Mutation reduziert Hepcidin-Expression -- verminderte Ferroportin-Internalisierung fuehrt zu erhoehter intestinaler Eisenabsorption",
        answerC = "HFE ist ein Eisenspeicherprotein in Hepatozyten; Mutation fuehrt zu Ferritin-Dysfunktion",
        answerD = "HFE reguliert duodenales DMT1; C282Y erlaubt unlimitierte Fe2+-Aufnahme durch den Transporter",
        correctAnswer = 1,
        explanation = "HFE bildet normalerweise einen Komplex mit Transferrin-Rezeptor 2 (TfR2) und Hemojuvelin, der die Hepcidin-Transkription in Hepatozyten stimuliert, wenn Eisenspiegel steigen. Die C282Y-Mutation verhindert die korrekte HFE-Faltung (unterbricht eine Disulfidbruecke), wodurch der Hepcidin-Stimulationsweg nicht aktiviert wird. Niedriges Hepcidin kann Ferroportin an Enterozyten und Makrophagen nicht internalisieren -- Eisen fliesst ungebremst ins Plasma.",
        difficulty = 5,
        funFact = "C282Y-Homozygotie ist eine der haeufigsten Erbkrankheiten in der nordeuropaeischen Bevoelkerung (1:200-300) -- aber nur ca. 10-33% der Homozygoten entwickeln klinisch manifeste Haemochromatose. Oestrogene bei Frauen sind protektiv (erklaert spaetere Manifestation bei Frauen durch den protektiven Effekt von menstruellem Blutverlust)."
    ),

    // Question 47 - Intestinal Failure-Associated Liver Disease
    Question(
        categoryId = 16,
        questionText = "Welcher Bestandteil parenteraler Ernaehrungsloesungen ist am staerksten mit der Entwicklung einer IFALD (Intestinal Failure-Associated Liver Disease) assoziiert, und welcher mechanistische Erklaerungsansatz besteht?",
        answerA = "Glukose in zu hoher Konzentration; fuehrt zu direkter hepatozellulaerer Glukotoxizitaet",
        answerB = "Sojaoelemulsionen (Omega-6-reich); Phytosterole hemmen biliare Ausscheidung und foerdern Leber-Entzuendung durch pro-inflammatorische Eikosanoide; Fischoel-Emulsionen zeigen bessere hepatische Vertraeglichkeit",
        answerC = "Aminosaeurenloesungen mit zu hohem Glyzin-Anteil; Glyzin ist direkter Hepatotoxin",
        answerD = "Natriumueberschuss in Infusionsloesungen fuehrt zu hepatischer Oedementstehung",
        correctAnswer = 1,
        explanation = "Sojaoel-basierte Lipidloesungen enthalten hohe Mengen Omega-6-Fettsaeuren und Phytosterole. Phytosterole (Sitosterol, Stigmasterol) hemmen FXR (Farnesoid X Receptor) und reduzieren biliare Cholesterol- und Gallenausscheidung -- sie akkumulieren hepatisch. Gleichzeitig foerdern Arachidonsaeure-Eikosanoide Hepatoentzuendung. Fischoel-basierte Emulsionen (Omegaven) mit hohem Omega-3-Anteil haben hepatoprotektive Wirkung und koennen IFALD reversieren.",
        difficulty = 5,
        funFact = "Omegaven (100% Fischoel) wurde urspruenglich off-label bei Saeuglingen mit schwerem IFALD eingesetzt und zeigte dramatische Verbesserungen der Cholestase. Die FDA-Zulassung fuer Smoflipid (SMOFlipid: Soja/MCT/Oliven/Fischoel-Mischung) 2016 war ein direktes Ergebnis dieser klinischen Beobachtungen."
    ),

    // Question 48 - Bile Acid Metabolism
    Question(
        categoryId = 16,
        questionText = "Welches nukleaere Rezeptorsystem reguliert die Gallensaeuresynthese ueber einen negativen Rueckkopplungsmechanismus, und welche klinische Bedeutung hat dieser Pfad?",
        answerA = "PPARalpha hemmt CYP7A1 direkt durch Bindung an dessen Promotor",
        answerB = "FXR (Farnesoid X Receptor) wird durch Gallensaeuren aktiviert und induziert SHP (Small Heterodimer Partner), das LRH-1/HNF4alpha hemmt -- verminderte CYP7A1-Transkription reduziert Gallensaeuresynthese; FXR-Agonisten sind Therapieansatz bei NASH/PBC",
        answerC = "LXR (Liver X Receptor) aktiviert ABCG5/8 und hemmt gleichzeitig CYP7A1",
        answerD = "GR (Glucokortikoid-Rezeptor) supprimiert durch Cortisol die gesamte Gallensaeuresynthese",
        correctAnswer = 1,
        explanation = "Gallensaeuren aktivieren hepatischen und ilealen FXR. Hepatisch induziert aktiviertes FXR SHP, das die basale Transkriptionsaktivitaet von LRH-1 und HNF4alpha auf dem CYP7A1-Promotor hemmt -- der Cholesterol-7alpha-Hydroxylase-Schritt (CYP7A1) limitiert die Gallensaeuresynthese. Ilealer FXR induziert FGF19, das als enterohepatisches Signal weiteren CYP7A1-Abbau triggert. Obeticholsaeure (OCA), ein semi-synthetischer FXR-Agonist, ist bei PBC und NASH in klinischer Entwicklung.",
        difficulty = 5,
        funFact = "FGF19 (das ileale FXR-Zielgen) ist ein seltenes Beispiel eines gastrointestinalen Hormons, das einen klassischen hepatischen Stoffwechselpfad reguliert -- enterohepatische Kommunikation ueber Fibroblasten-Wachstumsfaktoren, nicht nur ueber portale Naehrungsstoffe und klassische Darmhormone."
    ),

    // Question 49 - Autoimmune Hepatitis
    Question(
        categoryId = 16,
        questionText = "Welche Autoantikoeaper und welches HLA-Muster charakterisieren die Autoimmunhepatitis (AIH) Typ 1 versus Typ 2?",
        answerA = "Typ 1: Anti-LKM-1 (CYP2D6), Anti-LC1; HLA-DR7. Typ 2: ANA, SMA (Anti-Aktin); HLA-DR3/DR4",
        answerB = "Typ 1: ANA (Anti-Nukleare Antikoeaper), SMA (Smooth Muscle Antibody, Anti-Aktin); HLA-DR3/DR4. Typ 2: Anti-LKM-1 (gegen CYP2D6), Anti-LC1 (gegen Formiminotransferase); HLA-DR7",
        answerC = "Typ 1 und Typ 2 beide mit ANA; Unterschied nur in der Titerdynamik",
        answerD = "Typ 1: Anti-SLA/LP (loesliches Leber-Antigen); Typ 2: Anti-AMA-M2; HLA-Assoziation ist klinisch irrelevant",
        correctAnswer = 1,
        explanation = "AIH Typ 1 (haeufigste Form, alle Altersgruppen) ist durch ANA und SMA (Anti-Smooth-Muscle-Antikoeaper gegen F-Aktin) charakterisiert; HLA-DR3 und DR4 sind Suszeptibilitaetsgene. AIH Typ 2 (haeufiger bei Kindern) zeigt Anti-LKM-1 gegen CYP2D6 als Hauptzielantigen und Anti-LC1 gegen zytosolische Leberantigene; HLA-DR7 ist assoziiert. Anti-SLA/LP (Typ 3 nach manchen Klassifikationen) findet sich bei besonders schwerem Verlauf.",
        difficulty = 5,
        funFact = "Anti-LKM-1-Antikoeaper bei AIH Typ 2 richten sich gegen CYP2D6 -- dasselbe Enzym, das Pharmaka wie Codein, Tramadol und viele Antidepressiva metabolisiert. Bei AIH Typ 2 ist CYP2D6-Aktivitaet im Serum oft nachweisbar reduziert, was theoretisch die Metabolisierung bestimmter Medikamente beeinflussen koennte."
    ),

    // Question 50 - Glycogen Storage Disease
    Question(
        categoryId = 16,
        questionText = "Welcher enzymatische Defekt liegt der Glykogenspeicherkrankheit Typ II (Morbus Pompe) zugrunde, und warum ist die Erkrankung trotz erhaltener zytosolischer Glykogenolyse letal?",
        answerA = "Glucosetransporter GLUT2-Defekt; Glykogen kann nicht aus Hepatozyten exportiert werden",
        answerB = "Mangel an lysosomaler saurer alpha-Glukosidase (GAA/Acid Maltase); lysosomales Glykogen kann nicht hydrolysiert werden -- Lysosomen-Ruptur und Autophagie-Dysfunktion in Muskelzellen fuehren zu progressiver Myopathie und Herzversagen",
        answerC = "Debranching-Enzym-Defekt; Glykogen mit normalen Ketten kann abgebaut werden, aber Verzweigungspunkte akkumulieren",
        answerD = "Glykogenphosphorylase-Kinase-Defekt; Phosphorylase bleibt inaktiv -- Glykogen akkumuliert in Leber ohne Myopathie",
        correctAnswer = 1,
        explanation = "GAA (saure alpha-Glukosidase) ist ausschliesslich lysosomal lokalisiert und baut Glykogen im autophagolysosomalen Kompartiment ab. Fehlt GAA, akkumuliert Glykogen in Lysosomen, die anschwellen und rupturieren. In Kardiomyozyten und Skelettmuskel, die stark auf Autophagie angewiesen sind, fuehrt die Lysosomen-Dysfunktion zum Zelluntergang. Infantile Pompe-Krankheit manifestiert als hypertrophe Kardiomyopathie und hypoton-schlaffe Muskulatur.",
        difficulty = 5,
        funFact = "Alglucosidase alfa (Myozyme) war 2006 die erste zugelassene Enzymersatztherapie fuer Pompe -- und eine der fruehesten klinischen Erfolgsgeschichten der Enzym-Ersatz-Therapie insgesamt. Bei der spaet einsetzenden Form verlangsamt sie den Krankheitsverlauf erheblich. Das Medikament wird in gentechnisch veraenderten Chinese-Hamster-Ovary-Zellen produziert."
    )

)
