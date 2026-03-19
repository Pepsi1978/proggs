package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun healthQuestionsMaster7(): List<Question> = listOf(

    // Question 1 -- Molekularbiologie: mTOR-Signalweg
    Question(
        categoryId = 16,
        questionText = "Der mTORC1-Komplex integriert multiple Naehrstoff- und Wachstumssignale. Welcher molekulare Mechanismus erklaert, wie Aminosaeuren (insbesondere Leucin) mTORC1 am Lysosom aktivieren?",
        answerA = "Leucin bindet direkt an die kinase-regulatorische Domane von mTOR und induziert eine Konformationsanderung",
        answerB = "Leucin wird in die Zelle transportiert und aktiviert ueber den vATPase-Ragulator-Komplex die Rag-GTPasen (RagA/B-GTP + RagC/D-GDP), die mTORC1 zur lysosomalen Membran rekrutieren, wo es durch Rheb-GTP aktiviert wird",
        answerC = "Leucin hemmt AMPK, was die TSC2-GTPase-aktivierende Funktion unterdrueckt und mTORC1 indirekt aktiviert",
        answerD = "Leucin aktiviert PI3K durch Bindung an IRS-1, was downstream Akt und dann mTORC1 phosphoryliert",
        correctAnswer = 1,
        explanation = "Die Aminosaeure-Sensierung fuer mTORC1 erfolgt am Lysosom: Leucin und andere essentielle Aminosaeuren aktivieren ueber einen Komplex aus vATPase, Ragulator und SLC38A9/CASTOR1 die Rag-GTPasen. RagA/B-GTP rekrutiert mTORC1 zur Lysosomenmembran, wo der kleine GTPase Rheb (aktiviert durch Wachstumsfaktoren via PI3K-Akt-TSC-Achse) mTOR direkt stimuliert. Beide Signale (Aminosaeuren + Wachstumsfaktoren) sind notwendig fuer volle mTORC1-Aktivitaet.",
        difficulty = 5,
        funFact = "Rapamycin (Sirolimus) hemmt mTORC1 allosterisch durch Bindung an FKBP12. Klinische Nutzung: Immunsuppression nach Organtransplantation, Behandlung von Nierenzelltumoren und TSC-assoziierten Erkrankungen. Die Rapaloge hemmen mTORC1 bevorzugt bei S6K1-Phosphorylierung, weniger bei 4E-BP1 -- was die Resistenzentwicklung erklaert."
    ),

    // Question 2 -- Gentherapie: AAV-Vektoren
    Question(
        categoryId = 16,
        questionText = "Adeno-assoziierte Viren (AAV) sind bevorzugte Vektoren fuer Gentherapie. Welche strukturellen und molekularen Eigenschaften machen AAV geeignet, und was ist die wichtigste Einschraenkung?",
        answerA = "AAV integrieren zuverlaessig in Chromosom 19 (AAVS1), sind nicht immunogen und koennen unbegrenzt grosse Transgene transportieren",
        answerB = "AAV sind nicht-integrierende, einzelstraengige DNA-Viren mit geringer Pathogenitaet; sie verbleiben vorwiegend episomal in nicht-proliferierenden Zellen, zeigen Gewebetropismusunterschiede je nach Serotyp, und sind durch die begrenzte Kapazitaet von ~4,7 kb fuer grosse Gene (z.B. Dystrophin) ungeeignet",
        answerC = "AAV sind RNA-Retroviren wie HIV-basierte lentivirale Vektoren und integrieren semi-spezifisch in aktive Chromatin-Regionen",
        answerD = "AAV loesen keine Immunantwort aus, weil sie keine Kapsidproteine exprimieren und ausschliesslich nackte RNA in Zellen einschleusen",
        correctAnswer = 1,
        explanation = "AAV sind kleine, einzelstraengige DNA-Parvoviren (Kapazitaet ~4,7 kb). Sie integrieren selten ins Wirtsgenom und verbleiben meist als episomale Concatemere, was das Insertionsmutagenesrisiko minimiert. Die verschiedenen Serotypen (AAV2, 5, 8, 9 etc.) haben unterschiedliche Gewebetropismen. Hauptnachteil: Die begrenzte Klonierungskapazitaet schliesst grosse Gene aus; Mikrodystrophin-Konzepte umgehen dies bei DMD.",
        difficulty = 5,
        funFact = "Zolgensma (Onasemnogene abeparvovec, AAV9-SMN1) kostet etwa 2,1 Millionen USD pro Injektion und ist damit eines der teuersten Medikamente der Welt. Es behandelt spinale Muskelatrophie Typ 1 (SMA) mit einer einzigen i.v.-Infusion und hat dramatische klinische Ergebnisse bei Neugeborenen gezeigt."
    ),

    // Question 3 -- Intensivmedizin: Sepsis-3-Definitionen
    Question(
        categoryId = 16,
        questionText = "Gemaess der Sepsis-3-Definitionen (2016) unterscheidet sich septischer Schock von Sepsis durch spezifische klinische Kriterien. Welche Konstellation definiert den septischen Schock?",
        answerA = "Sepsis plus Fieber > 39 Grad C, Leukozytose > 20.000/µl und CRP > 200 mg/l",
        answerB = "Sepsis plus Vasopresorenbedarfs zur Aufrechterhaltung eines MAP >= 65 mmHg trotz adaequater Volumengabe UND Serumlaktat > 2 mmol/l",
        answerC = "Sepsis plus neu aufgetretenes Nierenversagen (Kreatinin > 2 mg/dl) und hepatische Dysfunktion (Bilirubin > 2 mg/dl)",
        answerD = "SIRS-Kriterien plus Nachweis einer bakteriaellen Bakteriaemie in 2 Blutkulturen",
        correctAnswer = 1,
        explanation = "Sepsis-3 definiert den septischen Schock als Untergruppe der Sepsis mit besonders hohem Mortalitaetsrisiko (>40 %): Trotz adaequater Volumentherapie ist ein Vasopressor notwendig, um einen MAP >= 65 mmHg aufrechtzuerhalten, UND das Serumlaktat liegt > 2 mmol/l. Diese Kombination weist auf eine tiefe zirkulatorische und zellulaere Dysfunktion hin. Der SOFA-Score ersetzt die alten SIRS-Kriterien in der Sepsis-3-Definition.",
        difficulty = 5,
        funFact = "Der qSOFA-Score (Quick SOFA) fuer das Screening ausserhalb der ICU umfasst nur drei Parameter: Atemfrequenz >= 22/min, veraenderter Bewusstseinsstatus (GCS < 15) und systolischer Blutdruck <= 100 mmHg. Zwei positive Punkte erhoehen den Verdacht auf Sepsis erheblich."
    ),

    // Question 4 -- Molekulare Onkologie: BCR-ABL und Imatinib
    Question(
        categoryId = 16,
        questionText = "Das Fusionsprotein BCR-ABL bei der chronisch myeloischen Leukaemie (CML) hat konstitutive Tyrosinkinase-Aktivitaet. Welcher Mechanismus erklaert die Imatinib-Resistenz durch die T315I-Mutation?",
        answerA = "T315I erhoehrt die BCR-ABL-Kinaseaktivitaet um das 1000-fache, sodass Imatinib thermodynamisch keine ausreichende Hemmung erreicht",
        answerB = "T315I (Threonin zu Isoleucin an Position 315) entfernt eine kritische Wasserstoffbrueckenbindung zwischen Imatinib und dem ATP-Bindebereich der Kinase und schafft eine sterische Hinderung durch die Isoleucyl-Seitenkette, sodass Imatinib nicht mehr bindet",
        answerC = "T315I aktiviert den PI3K-Akt-Signalweg parallel zu BCR-ABL und macht Zellen unabhaengig von BCR-ABL-Aktivitaet",
        answerD = "T315I fuehrt zur Ueberexpression von MDR1 (P-Glykoprotein), das Imatinib aktiv aus den Zellen herausschleust",
        correctAnswer = 1,
        explanation = "Threonin 315 ist das sogenannte Gatekeeper-Residuum in der BCR-ABL-Kinasedomaene. Es bildet normalerweise eine Wasserstoffbruecke mit dem Amidostickstoff von Imatinib. Die T315I-Substitution (Threonin -> Isoleucin) eliminiert diese Bruecke und fuehrt durch die groessere Isoleucin-Seitenkette zu sterischer Hinderung. Dadurch kann Imatinib, Dasatinib und Nilotinib nicht mehr binden. Ponatinib (Iclusig) wurde speziell gegen T315I entwickelt.",
        difficulty = 5,
        funFact = "Die Entdeckung, dass Imatinib (Gleevec) BCR-ABL hemmt, revolutionierte die Krebstherapie: Die 5-Jahres-Ueberlebensrate bei CML stieg von ~30 % auf >90 %. CML war die erste Krebserkrankung, bei der ein auf einen molekularen Mechanismus abzielender Wirkstoff klinisch eingesetzt wurde -- ein Meilenstein der Praezisionsonkologie."
    ),

    // Question 5 -- Seltene Syndrome: Prader-Willi vs. Angelman
    Question(
        categoryId = 16,
        questionText = "Prader-Willi-Syndrom (PWS) und Angelman-Syndrom (AS) entstehen beide durch Defekte in der chromosomalen Region 15q11-q13. Welcher molekulare Mechanismus des genomischen Imprintings erklaert, dass Deletionen der gleichen Region zwei voellig unterschiedliche Syndrome verursachen?",
        answerA = "PWS entsteht durch maternale Deletion (Mutter vererbt defekte Kopie), AS durch paternale Deletion -- das elterliche Geschlecht des Uebertraegers entscheidet ueber das klinische Bild",
        answerB = "PWS entsteht durch paternale Deletion (oder maternale uniparentale Disomie), weil die Gene in dieser Region nur auf dem paternalen Chromosom aktiv sind; AS entsteht durch maternale Deletion (oder paternalen UPD), weil UBE3A nur auf dem maternalen Chromosom im Gehirn aktiv ist",
        answerC = "PWS und AS entstehen durch unterschiedliche Splicing-Varianten desselben SNRPN-Gens, abhaengig von Methylierungsmustern im Promotorbereich",
        answerD = "Der Unterschied zwischen PWS und AS beruht ausschliesslich auf Mikrodeletionen verschiedener Groesse: grosse Deletionen (>5 Mb) fuehren zu PWS, kleine (<2 Mb) zu AS",
        correctAnswer = 1,
        explanation = "Genomisches Imprinting bedeutet, dass bestimmte Gene nur vom vaeterlichen oder muetterlichen Chromosom exprimiert werden. In 15q11-q13 sind paternale Gene (SNURF-SNRPN, NDN, MAGEL2) nur auf dem Vater-Chromosom aktiv; Fehlen dieser Gene (paternale Deletion oder maternale UPD) -> PWS. UBE3A ist im Gehirn nur vom Mutter-Chromosom aktiv; Fehlen (maternale Deletion oder paternale UPD) -> AS.",
        difficulty = 5,
        funFact = "Angelman-Syndrom wird auch 'Happy Puppet Syndrome' genannt, weil Betroffene haeufige, scheinbar grundlose Lachanfaelle zeigen, einen eigentuemlichen Gang haben und stark eingeschraenkte Sprache bei relativ gut erhaltener Sozialkompetenz zeigen. Die meisten Faelle waren historisch jahrelang falsch als Zerebralparese oder Autismus diagnostiziert."
    ),

    // Question 6 -- Forschung: CAR-T-Zell-Therapie
    Question(
        categoryId = 16,
        questionText = "Chimeric Antigen Receptor T-Zell-Therapie (CAR-T) ist bei B-Zell-Malignomen hochwirksam. Welche strukturellen Komponenten eines typischen Dritter-Generation-CAR definieren seine Funktion, und was ist das Hauptproblem bei soliden Tumoren?",
        answerA = "Dritter-Generation-CARs bestehen aus CD19-Antikoerper (scFv) + CD3-zeta-Kette und sind bei soliden Tumoren wegen fehlender Antigenexklusion unwirksam",
        answerB = "Dritter-Generation-CARs haben: extrazellulaeres scFv (Antigenerkennungsdomane) + Hinge/Transmembranregion + zwei kostimulatorische Domanen (z.B. CD28 + 4-1BB) + CD3-zeta-Signaldomane; bei soliden Tumoren scheitern sie an Immunsuppression im Tumormikromilieu, eingeschraenkter T-Zell-Infiltration und Antigenheterogenitaet",
        answerC = "Dritter-Generation-CARs kodieren zusaetzlich fuer IL-2 und TNF-alpha, um im Tumor eine Entzuendungsreaktion auszuloesen; das Problem bei soliden Tumoren ist ausschliesslich die fehlende Vaskularisierung",
        answerD = "Die Generationsbezeichnung bezieht sich auf die Anzahl der transduzierten Antikoerper-Gene; Generation 3 hat drei parallele scFv-Domanen fuer drei verschiedene Antigene",
        correctAnswer = 1,
        explanation = "Dritter-Generation-CARs haben zwei kostimulatorische Domanen (vs. eine bei Gen. 2), was die T-Zell-Persistenz und Aktivierung verstaerkt. Das extrazellulare scFv erkennt tumorspezifische Antigene (z.B. CD19, BCMA). Bei haematologischen Tumoren sind CAR-T hocheffektiv (Remissionsraten 70-90 % bei R/R ALL). Solide Tumoren sind schwieriger: immunsuppressives Mikromilieu (Treg, MDSCs, PD-L1), phsyikalische Barrieren und Antigenverlust/heterogenitaet limitieren den Erfolg.",
        difficulty = 5,
        funFact = "Tisagenlecleucel (Kymriah) war 2017 die erste zugelassene Gentherapie in den USA. Bei rezidivierter/refraktaerer B-ALL bei Kindern und jungen Erwachsenen erreicht sie Komplettremissionsraten von bis zu 81 %. Das Zytokinsturm-Syndrom (CRS) ist die lebensgefaehrlichste Nebenwirkung und erfordert Intensivmonitoring."
    ),

    // Question 7 -- Neurobiologie: Prion-Erkrankungen
    Question(
        categoryId = 16,
        questionText = "Prionkrankheiten wie die Creutzfeldt-Jakob-Erkrankung (CJD) beruhen auf einem einzigartigen Pathomechanismus. Welche molekulare Konformationsaenderung erklaert die Infektiositaet und Ausbreitung ohne Nukleinsaeure?",
        answerA = "Prionen sind kleine RNA-Viren (Virusoide), die durch inverse Transkription Wirtsgenom-Sequenzen veraendern und Neurodegeneration ausloesen",
        answerB = "Das normale zellulare Prionprotein PrPC (alpha-Helix-reich) wird durch fehlgefaltetes PrPSc (beta-Faltblatt-reich) als Matrize konvertiert. PrPSc katalysiert die Umfaltung weiterer PrPC-Molekuele in einem autokatalytischen Prozess; da nur Protein uebertragen wird, genuegt kontaminiertes Gewebe als 'Infektionsvehikel'",
        answerC = "Prionen sind defekte mitochondriale Ribosomen, die durch oxidativen Stress entstehen und Apoptose-Signalwege aktivieren",
        answerD = "PrPSc aktiviert konstitutiv Caspasen und verbreitet sich durch Neurexin-Neurexin-Interaktionen entlang synaptischer Verbindungen",
        correctAnswer = 1,
        explanation = "Prusiner entdeckte, dass PrPSc das einzige bekannte infektioese Agens ist, das keine Nukleinsaeure benoetigt. PrPC und PrPSc haben identische Aminosaeursequenz, aber unterschiedliche Raumstruktur: PrPC ist alpha-Helix-dominant und loeslich, PrPSc ist beta-Faltblatt-dominant, unloelich und resistent gegen Proteasen. PrPSc zwingt PrPC zur Umfaltung und bildet unloeliche Aggregate (Plaques), die Neuronen zerstoeren.",
        difficulty = 5,
        funFact = "Stanley Prusiner erhielt 1997 den Nobelpreis fuer Medizin fuer die Entdeckung der Prionen. Kontrovers war anfangs, dass Infektiositaet ohne Nukleinsaeuren konzeptionell das Zentraldogma der Molekularbiologie (DNA -> RNA -> Protein) herausforderte. Prionen-aehnliche Domanen werden heute auch bei regulaeren zellularen Prozessen (RNA-Granula, Synapsenplastizitaet) diskutiert."
    ),

    // Question 8 -- Intensivmedizin: Zytokinsturm und Immunpathologie
    Question(
        categoryId = 16,
        questionText = "Bei der haemophagozytischen Lymphohistiozytose (HLH) und dem Zytokinsturm-Syndrom (CSS) werden lebensbedrohliche Hyperinflammation durch unkontrollierte Immunaktivierung ausgeloest. Welcher Defekt liegt der primaeren HLH am haeufigsten zugrunde?",
        answerA = "Gain-of-function-Mutation in IL-6 mit konstitutiver Zytokin-Sekretion durch Makrophagen",
        answerB = "Loss-of-function-Mutationen in PRF1 (Perforin) oder anderen Granul-Exozytose-Proteinen (UNC13D, STX11, STXBP2), die die zytotoxische T-Zell- und NK-Zell-Funktion aufheben, sodass infizierte Zellen und aktivierte Makrophagen nicht eliminiert werden",
        answerC = "Autoimmun-Antikoerper gegen IFN-gamma-Rezeptoren mit Blockade der negativen Immunregulation",
        answerD = "Somatische JAK2-Mutation in Makrophagen mit konstitutiver STAT3-Aktivierung",
        correctAnswer = 1,
        explanation = "Primaere HLH ist eine autosomal-rezessive Erkrankung der Lymphozyten-Zytotoxizitaet. Perforin (PRF1) ist essentiell fuer die lytische Granulasekretion in CD8-T-Zellen und NK-Zellen. Ohne funktionelles Perforin koennen zytotoxische Lymphozyten infizierte Zellen und aktivierte Antigenpraesentierende Zellen nicht effizient toeten. Die Folge: anhaltende Immunstimulation, unkontrollierte Zytokinproduktion (IFN-gamma, IL-6, IL-18) und Haemophagozytose in Knochenmark, Leber und Milz.",
        difficulty = 5,
        funFact = "Die diagnostischen HLH-2004-Kriterien erfordern 5 von 8 Befunden: Fieber, Splenomegalie, Bizyto- oder Panzytopenie, Hypertriglyzeridaemie/Hypofibrinogenaemie, Haemophagozytose in Biopsie, niedrige NK-Aktivitaet, Ferritin >500 µg/l, sCD25 >2400 U/ml. Ferritin >10.000 µg/l ist hochspezifisch fuer HLH."
    ),

    // Question 9 -- Forschung: Liquid Biopsy
    Question(
        categoryId = 16,
        questionText = "Liquid Biopsy analysiert zirkulierende Tumor-DNA (ctDNA) im Plasma. Welche molekulare Eigenschaft macht ctDNA diagnostisch verwertbar, und welche technische Herausforderung besteht aufgrund des Hintergrundrauschens?",
        answerA = "ctDNA ist methyliert, waehrend normale DNA unmethyliert ist; PCR-Amplifikation ist problemlos moeglich, da ctDNA 10-100 ng/ml Konzentration hat",
        answerB = "ctDNA traegt tumorspezifische somatische Mutationen und weist tumorspezifische Methylierungsmuster auf; die Herausforderung ist die extrem niedrige Fraktion (0,01-1 % der zellfreien DNA), die hochsensitive Methoden wie digitale PCR oder Ultra-deep-Sequencing (NGS mit Fehlerkorrektur durch UMIs) erfordert",
        answerC = "ctDNA unterscheidet sich von normaler cfDNA durch ihre Laenge (>500 bp vs. <200 bp) und kann durch Groessenfilterung angereichert werden",
        answerD = "ctDNA ist RNA-basiert (tumorale mRNA-Fragmente) und wird durch RT-PCR nach DNase-Verdau spezifisch nachgewiesen",
        correctAnswer = 1,
        explanation = "ctDNA sind apoptotische oder nekrotische DNA-Fragmente aus Tumorzellen mit tumorspezifischen somatischen Mutationen und aberranten Methylierungsmustern. Das Problem: Bei fruehen Tumoren oder nach Therapie macht ctDNA oft < 0,1 % der gesamten zellfreien DNA aus. Digitale PCR (ddPCR) ermoeglicht absolute Quantifizierung bei bekannten Mutationen; NGS mit einzigartigen molekularen Identifikatoren (UMIs) reduziert PCR- und Sequenzierfehler.",
        difficulty = 5,
        funFact = "Die FDA hat mehrere Liquid-Biopsy-Tests zugelassen: Guardant360 und FoundationOne Liquid CDx analysieren Hunderte von Genen auf ctDNA. Bei Lungenkrebs kann damit z.B. eine EGFR-T790M-Resistenzmutation nachgewiesen werden, ohne erneute Gewebsbiopsie -- was die Therapieentscheidung beschleunigt."
    ),

    // Question 10 -- Seltene Erkrankungen: Porphyrien
    Question(
        categoryId = 16,
        questionText = "Ein Patient prasentiert sich mit schweren kolikartigen Bauchschmerzen, Tachykardie, Hypertonie, braunrotem Urin und peripherer Neuropathie nach Einnahme eines Antiepileptikums. Welcher Enzymdefekt und welcher Pathomechanismus liegt der akuten intermittierenden Porphyrie (AIP) zugrunde?",
        answerA = "Defekt der Uroporphyrinogen-III-Synthase fuehrt zur Akkumulation von Uroporphyrinogen I, das nephrotoxisch ist",
        answerB = "Haploinsuffizienz der Hydroxymethylbilan-Synthase (HMBS, auch Porphobilinogen-Desaminase) fuehrt zur Akkumulation der neurotoxischen Vorstufen ALA (Delta-Aminolaevulinsaeure) und PBG (Porphobilinogen), die Nerven- und Bauchschmerzattacken ausloesen",
        answerC = "Defekt der Ferrochelatase verhindert Haem-Synthese; Eisenstau im Knochenmark loest systemische Entzuendung aus",
        answerD = "ALAS2-Gain-of-function fuehrt zur Uebersynthese von Haem, das autooxidativ zu toxischen Sauerstoffradikalen zerfaellt",
        correctAnswer = 1,
        explanation = "AIP beruht auf einem HMBS-Defekt mit 50 % reduzierter Enzymaktivitaet. Triggerfaktoren (Antiepileptika, Barbiturate, Alkohol, Fasten) induzieren ALAS1 (die erste Enzymstufe), was bei reduzierter HMBS-Kapazitaet zur Akkumulation von ALA und PBG fuehrt. Diese Vorstufen sind neurotoxisch und erklaeren Bauchschmerzen (autonome Neuropathie), periphere Neuropathie, psychiatrische Symptome und SIADH. Therapie: Glukose-Infusion und i.v. Haem (Normosang).",
        difficulty = 5,
        funFact = "Koenig Georg III. von England soll an einer Porphyrie gelitten haben, was seine Geistesstoerungen erklaert. Die 'Madness of King George' faszinierte Historiker jahrhundertelang. Modernes Porphobilinogen-Screening im Urin waere die Diagnose innerhalb von Stunden gestellt. Givosiran (siRNA gegen ALAS1) ist seit 2019 zugelassen und reduziert Attackenfrequenz dramatisch."
    ),

    // Question 11 -- Molekularbiologie: Epigenetik und DNA-Methylierung
    Question(
        categoryId = 16,
        questionText = "DNA-Methylierung an CpG-Inseln reguliert Genexpression in Tumorgenese und normaler Entwicklung. Welcher enzymatische Mechanismus erklaert die aktive DNA-Demethylierung im Rahmen der Reprogrammierung und was ist die Rolle von TET-Enzymen?",
        answerA = "TET-Enzyme entfernen Methylgruppen direkt durch Hydrolyse der C-N-Bindung zwischen Cytosin und Methylgruppe",
        answerB = "TET-Enzyme (TET1/2/3) oxidieren 5-Methylcytosin (5mC) schrittweise zu 5-Hydroxymethylcytosin (5hmC), 5-Formylcytosin (5fC) und 5-Carboxylcytosin (5caC); letztere werden durch TDG-BER (Thymin-DNA-Glykosylase + Basenexzisionsreparatur) zu unmethyliertem Cytosin konvertiert",
        answerC = "TET-Enzyme sind SAM-abhaengige Methyltransferasen, die 5mC zu 5hmC re-methylieren und damit Promotorschweigen aufheben",
        answerD = "Aktive Demethylierung erfolgt durch DNMT3A im Gegensatz zu passiver Demethylierung durch DNMT1; TET-Enzyme sind Kofaktoren ohne katalytische Funktion",
        correctAnswer = 1,
        explanation = "TET-Enzyme sind Fe2+- und alpha-Ketoglutarat-abhaengige Dioxygenasen. Sie oxidieren 5mC stufenweise: 5mC -> 5hmC -> 5fC -> 5caC. 5fC und 5caC werden von TDG erkannt und durch Basenexzisionsreparatur durch unmethyliertes Cytosin ersetzt. 5hmC ist auch ein stabiles epigenetisches Merkmal. TET2-Mutationen sind haeufig bei haematologischen Neoplasien (AML, CMML) und blockieren aktive Demethylierung.",
        difficulty = 5,
        funFact = "Alpha-Ketoglutarat (2-Oxoglutarat) ist ein TCA-Zyklusintermediat und essentieller Kofaktor fuer TET-Enzyme. IDH1/2-Mutationen bei Gliomen und AML produzieren 2-Hydroxyglutarat, das TET-Enzyme kompetitiv hemmt -- was erklaert, warum IDH-mutierte Tumoren Hypermethylierung zeigen (CpG Island Methylator Phenotype, CIMP)."
    ),

    // Question 12 -- Neurologie: Neuromuskulaere Uebertragung
    Question(
        categoryId = 16,
        questionText = "Das Lambert-Eaton-Myasthenisches Syndrom (LEMS) und die Myasthenia gravis (MG) sind beide autoimmune Erkrankungen der neuromuskulaeren Synapse, aber mit unterschiedlichen Zielantigenen. Wie unterscheiden sich Zielantigene und elektrophysiologischer Befund?",
        answerA = "MG richtet sich gegen den Acetylcholin-Transporter (VAChT) prasynaptisch; LEMS gegen nicotinische AChR postsynaptisch; elektrophysiologisch ist LEMS durch Dekrementierungsmuster bei Hochfrequenzstimulation charakterisiert",
        answerB = "MG-Antikoerper (anti-AChR oder anti-MuSK) richten sich gegen postsynaptische Strukturen mit dekrementierenden MUPS; LEMS-Antikoerper (anti-VGCC, voltage-gated calcium channels) richten sich gegen prasynaptische Ca2+-Kanaele, was bei Hochfrequenzstimulation (>10 Hz) paradoxe Inkrementierung (>100 % CMAP-Amplitudenzunahme) zeigt",
        answerC = "Beide Erkrankungen richten sich gegen AChR, LEMS am prasynaptischen Axonknoten und MG am postsynaptischen Endplattenbereich; Unterschied nur in Antigen-Epitopen",
        answerD = "LEMS ist charakterisiert durch Anti-GAD65-Antikoerper mit zerebellarer Ataxie als Hauptsymptom; MG durch Anti-AChR mit Einschraenkung des limbischen Systems",
        correctAnswer = 1,
        explanation = "Bei LEMS sind Autoantikoeorper gegen spannungsabhaengige Kalziumkanaele (VGCC, meist P/Q-Typ) an der prasynaptischen Membran gerichtet. Dadurch wird weniger Ca2+ eingestromt und weniger ACh freigesetzt. Charakteristisch ist, dass sich die Muskelkraft bei wiederholter Kontraktion kurzzeitig verbessert (posttetanische Potenzierung) und elektrophysiologisch eine Inkrementierung >100 % bei Hochfrequenzstimulation zeigt. LEMS ist in 60 % paraneoplastisch (kleinzelliges Bronchialkarzinom).",
        difficulty = 5,
        funFact = "Amifampridin (3,4-Diaminopyridin) ist die Standardtherapie bei LEMS: Es verlaengert das Aktionspotenzial am prasynaptischen Axon durch Blockade spannungsabhaengiger K+-Kanaele, was einen laengeren Ca2+-Einstrom und mehr ACh-Freisetzung bewirkt. In Verbindung mit Acetylcholinesterase-Hemmern ist die Wirkung additiv."
    ),

    // Question 13 -- Kardiologie: Genetik der Kardiomyopathien
    Question(
        categoryId = 16,
        questionText = "Die hypertrophe Kardiomyopathie (HCM) ist die haeufigste monogene Herzerkrankung. Welches Gen ist am haeufigsten mutiert, welcher Protein-Mechanismus wird gestort, und was erklaert den sudden cardiac death-Risiko?",
        answerA = "MYH7 (beta-Myosin-Schwerkette) und MYBPC3 (kardiales Myosin-Bindeprotein C) sind am haeufigsten betroffen; Mutationen veraendern die Aktomyosin-ATPase-Kinetik, erhoehn die Ca2+-Sensitivitaet und bewirken kontraktile Hyperaktivitaet mit diastolischer Dysfunktion; der SCD entsteht durch Re-entry-Tachyarrhythmien im hypertrophierten, fibrotischen Myokard",
        answerB = "KCNQ1 und KCNH2 kodieren fuer Kaliumkanaele, deren Gain-of-function zu ventrikulaerer Hypertrophie und Torsade de Pointes fuehrt",
        answerC = "SCN5A (Nav1.5) ist das einzige HCM-Gen; Gain-of-function verursacht Natirum-Ueberlastung und kompensatorische Hypertrophie",
        answerD = "LMNA-Mutationen (Lamin A/C) sind bei HCM am haeufigsten und erklaeren den genetischen Zusammenhang mit Muskeldystrophie und HCM",
        correctAnswer = 0,
        explanation = "Zusammen machen MYH7 und MYBPC3 ueber 70 % der genetisch determinierten HCM-Faelle aus. Pathogene Varianten erhoehen die intrinsische Myosin-ATPase-Rate und Ca2+-Affinitaet des Troponin-Komplexes. Folgen sind diastolische Dysfunktion, myozytaere Unordnung (Disarray) und interstitielle Fibrose. Die inhomogene Erregungsausbreitung im hypertrophierten, fibrotischen Myokard scahfft Re-entry-Substrate fuer ventrikulaere Tachykardie und Kammerflimmern.",
        difficulty = 5,
        funFact = "Mavacamten (Camzyos) ist der erste zugelassene Wirkstoff, der direkt die Myosin-ATPase hemmt (allosterische Hemmung des Myosin-Schwerketten-Aktin-Zyklus). Er reduziert den linksventrikulaeren Ausflusstraktgradienten bei obstruktiver HCM und verbessert Dyspnoe -- ein Paradigmenwechsel nach jahrzehntelanger symptomatischer Therapie."
    ),

    // Question 14 -- Immunologie: Komplementsystem
    Question(
        categoryId = 16,
        questionText = "Paroxysmale naechtliche Haemoglobinurie (PNH) entsteht durch somatische Mutation im PIG-A-Gen. Wie erklaert der Verlust von GPI-verankerten Proteinen die Komplementlyse, und welcher therapeutische Angriffspunkt wird durch Eculizumab genutzt?",
        answerA = "PIG-A-Mutation fuehrt zu Ueberexpression von CD59, das Komplement hyperaktiviert und Haemolyse ausloest",
        answerB = "PIG-A kodiert fuer GPI-Biosynthese; ohne GPI fehlen auf Erythrozyten die komplementregulatorischen Proteine CD55 (DAF) und CD59 (Protectin), sodass spontane Komplementaktivierung auf der Zelloberflaeche nicht gehemmt wird. Eculizumab hemmt C5-Spaltung und verhindert die Bildung des Membranattackomplexes (MAC, C5b-9)",
        answerC = "PIG-A-Mutation bewirkt konstitutive Aktivierung von C3 durch direkten Einfluss auf die C3-Konvertase der alternativen Aktivierungsroute",
        answerD = "GPI-verankerte Proteine hemmen CD20 auf B-Zellen; ohne GPI proliferieren B-Zellen unkontrolliert und produzieren Autoantikoeorper gegen Erythrozyten",
        correctAnswer = 1,
        explanation = "GPI-Anker befestigen zahlreiche Proteine an der Zelloberflaeche. CD55 hemmt C3- und C5-Konvertasen (Decay Accelerating Factor); CD59 blockiert die Insertion des Membranattackkomplexes (C5b-9) in die Zellmembran. PNH-Erythrozyten ohne CD55/CD59 sind gegenueber spontaner Komplementaktivierung schutzlos. Eculizumab (monoklonaler Anti-C5-Antikoerper) verhindert die C5a/C5b-Spaltung und unterbricht MAC-Bildung -- hocheffektiv gegen intravaskuaere Haemolyse.",
        difficulty = 5,
        funFact = "PNH-Zellen aus einer einzigen mutierten haematopoetischen Stammzelle expandieren klonal, weil sie selektive Ueberlebensvorteile gegenueber normalen Stammzellen haben (Mechanismus noch nicht vollstaendig geklart). Ravulizumab ist ein Long-acting-Anti-C5-Antikoerper mit 8-woechigem Dosierungsintervall und hat Eculizumab bei vielen Patienten abgeloest."
    ),

    // Question 15 -- Infektiologie: HIV-Replikationszyklus
    Question(
        categoryId = 16,
        questionText = "HIV-Integrase-Inhibitoren (INSTIs) wie Dolutegravir sind Erstlinientherapie der HIV-Infektion. An welchem spezifischen Schritt des HIV-Replikationszyklus greifen sie an, und welche strukturelle Eigenschaft der Integrase wird gehemmt?",
        answerA = "INSTIs hemmen die reverse Transkriptase-Aktivitaet der Integrase, die RNA in dsDNA umschreibt",
        answerB = "INSTIs chelieren die zwei Magnesium-Ionen im aktiven Zentrum der Integrase, die fuer den Strangtransfer benoetigt werden; der Transfer des viralen DNA-Endes in die Wirts-DNA wird blockiert (Strand Transfer Inhibition), waehrend die 3'-Prozessierung unbeeintraechtig bleibt",
        answerC = "INSTIs hemmen die Integrase-Bindung an das LEDGF/p75-Protein, das die Integrase zum Chromatin dirigiert",
        answerD = "INSTIs blockieren die nukleaere Lokalisation der PIC (Praeintegrationscomplex) durch Hemmung des Importin-alpha-beta-Komplexes",
        correctAnswer = 1,
        explanation = "HIV-Integrase katalysiert zwei Schritte: 3'-Prozessierung (Abschneiden der Enden der viralen DNA) und Strangtransfer (Integration in Wirts-DNA). INSTIs binden an das aktive Zentrum und chelatieren die zwei Mg2+-Ionen, die fuer die Transesterifikationsreaktion des Strangtransfers essenziell sind. 3'-Prozessierung bleibt unbeeintraechtig. INSTIs haben hohe Resistenzbarriere (v.a. 2nd-Gen: Dolutegravir, Bictegravir) und exzellente Vertraeglichkeit.",
        difficulty = 5,
        funFact = "Dolutegravir ist das Rueckgrat der WHO-empfohlenen Erstlinientherapie in Niedrig- und Mitteleinkommenslaendern. Tenofovir/Lamivudin/Dolutegravir ist als fixe Kombination guenstig verfuegbar und hat Efavirenz als Standard abgeloest. Resistenzmutationen gegen Dolutegravir erfordern multiple Sekundaermutationen -- daher seine ueberlegene Resistenzbarriere."
    ),

    // Question 16 -- Forschung: CRISPR-Basiseditor
    Question(
        categoryId = 16,
        questionText = "Base Editing ist eine Weiterentwicklung von CRISPR-Cas9, die Punktmutationen ohne Doppelstrangbruch einfuehrt. Was ist der mechanistische Unterschied zwischen Cytidin-Basiseditoren (CBE) und Adenin-Basiseditoren (ABE)?",
        answerA = "CBE und ABE unterscheiden sich nur in der sgRNA-Sequenz, nicht im enzymatischen Mechanismus; beide erzeugen C:G -> T:A Transversionen",
        answerB = "CBE fusionieren eine Cytidin-Desaminase (z.B. APOBEC1) mit katalytisch inaktivem Cas9 (nCas9); Cytosin im Einzelstrang-DNA-Fenster wird zu Uracil desaminiert (-> T nach Reparatur: C:G -> T:A). ABE nutzen entwickelte Adenosin-Desaminasen (tRNA-Adenosin-Desaminase-Varianten); Adenin wird zu Inosin desaminiert (->G nach Reparatur: A:T -> G:C)",
        answerC = "CBE schneidet den Nicht-Zielstrang und verwendet HDR mit Donor-Template; ABE schneidet den Zielstrang und nutzt NHEJ fuer fehlerfreie Punktmutation",
        answerD = "CBE und ABE basieren auf Cas12a (Cpf1) statt Cas9, weil Cas12a einzelstraengige DNA ohne PAM-Anforderung schneiden kann",
        correctAnswer = 1,
        explanation = "Basiseditor = nCas9 (schleift nur einen Strang) + Deaminase. CBE (entwickelt von David Liu, 2016): APOBEC-Desaminase desaminiert C zu U im ssDNA-Fenster (Positionen 4-8 vom PAM-prox. Ende); Mismatch-Reparatur oder Replikation -> T. Ergebnis: C:G -> T:A. ABE (Liu 2017): entwickelte tRNA-Adenosin-Desaminase (ABE7.10, ABE8e) desaminiert A zu Inosin (liest wie G); -> G:C bei Reparatur. Ergebnis: A:T -> G:C. Keine Doppelstrangbrueche, geringeres Indel-Risiko.",
        difficulty = 5,
        funFact = "Prime Editing (PE, 2019) ist die naechste Generation: pegRNA traegt sowohl Zielsequenz als auch Korrektursequenz; Cas9-Nickase schneidet den Nicht-Zielstrang; ein RT-Anteil schreibt die Korrektur ein. PE ermoeglicht alle 12 Arten von Transversionen und Transitionen sowie kleine Insertionen/Deletionen ohne Doppelstrangbruch und ohne Donor-DNA."
    ),

    // Question 17 -- Pathologie: Tumorsuppressorgene
    Question(
        categoryId = 16,
        questionText = "Das Retinoblastom-Gen (RB1) war das erste identifizierte Tumorsuppressorgen. Knudsons 'Two-Hit-Hypothese' erklaert das Entstehungsmuster. Welche molekulare Funktion hat pRb im Zellzyklus und wie foerdert sein Verlust Tumorigenese?",
        answerA = "pRb ist ein Transkriptionsaktivator von Cyclin D1; sein Verlust erhoehrt die CDK4/6-Aktivitaet und beschleunigt G2/M-Uebergang",
        answerB = "pRb bindet und hemmt im hypophosphorylierten Zustand den Transkriptionsfaktor E2F, was den G1/S-Uebergang blockiert. Phosphorylierung durch CDK4/6-Cyclin D loest E2F frei, das S-Phase-Gene aktiviert. Ohne pRb ist E2F konstitutiv aktiv, der Zellzyklus laeuft unkontrolliert ab und die Zelle proliferiert trotz fehlender Wachstumssignale",
        answerC = "pRb ist eine Ubiquitin-Ligase, die Cyclin E fuer den Abbau markiert; Verlust fuehrt zu Cyclin-E-Akkumulation und Replikationsstress",
        answerD = "pRb reguliert ausschliesslich Apoptose durch direkte Bindung an BAX und Bcl-2 an der Mitochondrienmembran",
        correctAnswer = 1,
        explanation = "pRb ist der 'Gatekeeper' des G1/S-Uebergangs: Im ruhenden Zustand bindet es E2F-Transkriptionsfaktoren und haelt sie inaktiv. Mitogene Signale aktivieren CDK4/6-Cyclin D, die pRb hyperphosphorylieren und E2F freisetzen. Freigesetztes E2F aktiviert Gene fuer DNA-Replikation (Cyclin E, DHFR etc.). Verlust beider RB1-Allele (Knudson: 2 Hits) macht E2F konstitutiv aktiv -- ein zentraler Mechanismus bei vielen Karzinomen.",
        difficulty = 5,
        funFact = "Palbociclib, Ribociclib und Abemaciclib sind CDK4/6-Inhibitoren, die den pRb-Pathway therapeutisch nutzen: Sie halten pRb hypophosphoryliert und E2F gebunden, was proliferierende Tumorzellen im G1 arretiert. Diese Medikamente sind Erstlinienstandard beim HR+/HER2-negativen fortgeschrittenen Mammakarzinom."
    ),

    // Question 18 -- Intensivmedizin: Mechanische Beatmung
    Question(
        categoryId = 16,
        questionText = "Beim beatmeten ARDS-Patienten wird eine lungenprotektive Beatmungsstrategie angewendet. Welche Kombination aus Tidalvolumen, PEEP-Strategie und Atemfrequenz ist nach aktueller Evidenz (ARDSNet + ALVEOLI + LOVS-Studien) korrekt?",
        answerA = "Tidalvolumen 10-12 ml/kg IBW, niedrigster PEEP der Oxygenierung erlaubt, Atemfrequenz beliebig bis 35/min",
        answerB = "Tidalvolumen 6 ml/kg idealem Koerpergewicht (IBW), Plateaudruck <= 30 cmH2O, PEEP gemaess FiO2/PEEP-Tabelle (hoeher-PEEP-Strategie bei schwerem ARDS bevorzugt), Atemfrequenz 15-35/min zur pH-Kontrolle mit erlaubter Hyperkapnie (permissive Hyperkapnie bis pH >= 7,20)",
        answerC = "Tidalvolumen 8 ml/kg IBW, hoechst moeglicher PEEP (20-25 cmH2O) bei allen ARDS-Patienten, Atemfrequenz <= 15/min zur CO2-Retention",
        answerD = "Tidalvolumen 4 ml/kg IBW, PEEP >= 15 cmH2O unabhaengig von Oxygenierung, Zielpulsoxymetrie SpO2 >= 98 %",
        correctAnswer = 1,
        explanation = "Die ARDSNet-Studie (2000) bewies: 6 ml/kg IBW vs. 12 ml/kg senkt Mortalitaet um 22 %. Plateaudruck <= 30 cmH2O begrenzt Volutrauma. PEEP nach FiO2/PEEP-Tabellen wird gemaess Oxygenierungsziel eingestellt; bei schwerem ARDS (P/F <200) ist hoehere PEEP-Strategie diskutiert. Permissive Hyperkapnie (pH >= 7,20) toleriert hoehere CO2-Werte um Baro- und Volutrauma zu minimieren. Prone Positioning bei P/F < 150 reduziert Mortalitaet (PROSEVA-Studie).",
        difficulty = 5,
        funFact = "Die Treibdruckhypothese (Amato 2015, NEJM) ergaenzt die Tidalvolumen-Strategie: Treibdruck = Plateaudruck - PEEP soll <= 15 cmH2O sein. Treibdruck reflektiert die stressausuebende Kraft auf aerierbares Lungengewebe besser als Tidalvolumen allein und war der staerkste Praediktor fuer ARDS-Mortalitaet in einer Reanalyse von 9 RCTs."
    ),

    // Question 19 -- Neurologie: Parkinson-Pathophysiologie
    Question(
        categoryId = 16,
        questionText = "Alpha-Synuklein-Aggregate (Lewy-Koerper) sind das Hauptmerkmal der Parkinson-Erkrankung. Welcher Mechanismus erklaert die progressive Ausbreitung der Pathologie nach Braak-Staging, und wie erklaert das Prion-Paradigma den Krankheitsverlauf?",
        answerA = "Alpha-Synuklein ist ein Retrovirus-Protein, das sich durch Infektion von Neuron zu Neuron ausbreitet und DNS-Schaeden akkumuliert",
        answerB = "Fehlgefaltetes Alpha-Synuklein (beta-Faltblatt-reich) breitet sich prion-aehnlich trans-synatisch aus: Es verlaeesst Neuronen in Vesikeln oder durch direkten Kontakt, induziert die Fehlfaltung von nativem Alpha-Synuklein in empfangenden Zellen und steigt entlang neuronaler Netzwerke auf (Braak: olfaktorischer Kortex -> untere Hirnstamm-Kerne -> Substantia nigra -> Neokortex)",
        answerC = "Alpha-Synuklein aktiviert NLRP3-Inflammasom in Mikroglia und verbreitet sich durch retrograden axonalen Transport entlang des Vagusnervs",
        answerD = "Die Lewy-Koerper entstehen durch Ubiquitin-Proteasom-Dysregulation in der Substantia nigra und bleiben lokal -- die Braak-Stadien beschreiben Glia- nicht Neuronenpathologie",
        correctAnswer = 1,
        explanation = "Das prion-aehnliche Ausbreitungskonzept fuer Alpha-Synuklein basiert auf: 1) Zell-zu-Zell-Transfer nachweisbar in vitro und in vivo, 2) fehlgefaltetes Alpha-Synuklein templiert natives Protein zur Fehlfaltung (Seeding), 3) Braak-Staging zeigt konsistente anatomische Progression entsprechend neuronaler Konnektivitaet. Ob der Ausgangspunkt im Enterosum nervensystem oder Olfaktorium liegt (Gut-Brain oder Nose-Brain Theorie) ist aktive Forschungsfrage.",
        difficulty = 5,
        funFact = "Faecale Mikrobiom-Transplantationen reduzierten Alpha-Synuklein-Pathologie in Mausmodellen, was die Gut-Brain-Hypothese stuetzt. Epidemiologisch haben Parkinson-Patienten haeufig Obstipation als Fruehsymptom (>10 Jahre vor Diagnose), was auf gastrointestinale Alpha-Synuklein-Pathologie als Startpunkt hindeutet."
    ),

    // Question 20 -- Immunologie: Autoimmune Enzephalitis
    Question(
        categoryId = 16,
        questionText = "Anti-NMDA-Rezeptor-Enzephalitis ist die haeufigste autoimmune Enzephalitis. Welcher immunologische Mechanismus erklaert die Symptome, und warum sind junge Frauen mit Ovarialteratom besonders betroffen?",
        answerA = "Anti-NMDAR-Antikoerper aktivieren NMDA-Rezeptoren konstituiv, was zu Exzitotoxizitaet und Kalzium-bedingter Apoptose fuehrt",
        answerB = "IgG-Antikoerper gegen die GluN1-Untereinheit des NMDA-Rezeptors internalisieren Rezeptoren von der Synapsenoberflaehe (Antibody-mediated receptor crosslinking + Endozytose), was Glutamaterge Dysfunktion verursacht; bei Ovarialteratom exprimieren NMDAR-positive Neuralzellen im Tumor Autoantigen, das Immunantwort ausloest",
        answerC = "Anti-NMDAR-Antikoerper sind zytokin-aehnliche Molekuele, die direkt die Blut-Hirn-Schranke oeffen und Makrophagen-Infiltration ausloesen",
        answerD = "NMDA-Rezeptor-Antikoerper entstehen durch molekulares Mimikry mit Herpes-simplex-Virusproteinen und fuehren zu komplement-medierter Synapsenzerstoerung",
        correctAnswer = 1,
        explanation = "Anti-NMDAR-IgG binden die GluN1-Untereinheit und fuehren durch Kreuzvernetzung und Endozytose zur Reduktion der Oberflaechenrezeptordichte. Da NMDA-Rezeptoren fuer GABAerge Interneuronen besonders wichtig sind, resultiert Hemmung inhibitorischer Neurone, was paradoxe Exzitation und psychiatrische Symptome, epileptische Anfaelle und Bewegungsstoerungen erklaert. Ovarialteratome enthalten Neuralgewebe mit NMDA-Rezeptor-Expression, das als Autoantigen wirkt.",
        difficulty = 5,
        funFact = "Die Anti-NMDAR-Enzephalitis wird haeufig als Psychiatrisch-Erkrankung missdiagnostiziert (Schizophrenie, bipolare Stoerung) weil Psychose und Verhaltensaenderungen fruehere Symptome sind als neurologische Zeichen. Rachael Encrenaz und Susannah Cahalan ('Brain on Fire', 2012) machten die Erkrankung oeffentlich bekannt."
    ),

    // Question 21 -- Forschung: Organoid-Technologie
    Question(
        categoryId = 16,
        questionText = "Organoide aus pluripotenten Stammzellen revolutionieren die Krankheitsmodellierung. Welche methodischen Anforderungen und Limitationen bestehen bei Darm-Organoiden im Vergleich zu nativen Darmkrypten?",
        answerA = "Darm-Organoide sind zuverlaessige Modelle ohne Limitationen; sie replizieren Mukosa, Submukosa und Muscularis propria vollstaendig",
        answerB = "Darm-Organoide rekapitulieren Krypten- und Villusstrukturen mit stammzellhaltigen Kryptenbasen (Lgr5+ Stammzellen) und differenzierten Zeltypen (Enterozyten, Goblet-Zellen, Paneth-Zellen, EEC); Limitationen: kein Immunsystem, kein Mikrobiomsimulation, keine vaskulaere Versorgung, geschlossenes Lumen erschwert Luminalbeschallung, Kultivierung erfordert extrazellulaere Matrix (Matrigel) mit Batch-Variabilitaet",
        answerC = "Darm-Organoide koennen nur aus iPSC generiert werden, nicht aus primaeren Kryptenstammzellen, weil terminale Differenzierung die Zellen erschoepft",
        answerD = "Organoide replizieren Darmfunktion vollstaendig inklusive des enterischen Nervensystems und barrieren-Test sind deshalb identisch mit in-vivo Messungen",
        correctAnswer = 1,
        explanation = "Clevers-Gruppe zeigte 2009, dass Lgr5+ Stammzellen aus Mauskrypten selbstorganisierend Darm-Organoide bilden. Diese Mini-Daerme enthalten alle epithelialen Zelltypen und zeigen Krypten-Villus-Organisation. Klinisch werden sie bereits fuer personalisierte Therapietestung bei CF und kolorektalen Karzinomen eingesetzt. Wesentliche Luecken: fehlendes Immunsystem (T-Zellen, ILCs), Mikrobiom und Vaskulatur limitieren Komplexitaet.",
        difficulty = 5,
        funFact = "Hans Clevers erhaelt den Breakthrough Prize 2023 fuer die Organoid-Technologie. Pankreas-Organoide aus CF-Patienten werden bereits klinisch genutzt, um Ansprechen auf CFTR-Modulatoren (Ivacaftor, Lumacaftor) vorherzusagen -- personalisierte Medizin im wahrsten Sinne."
    ),

    // Question 22 -- Molekularbiologie: RNA-Interferenz
    Question(
        categoryId = 16,
        questionText = "RNA-Interferenz (RNAi) wird therapeutisch genutzt (z.B. Givosiran, Inclisiran). Welcher RISC-Komplex-Mechanismus erklaert die Sequenz-spezifische mRNA-Degradation, und wie unterscheidet sich siRNA von miRNA in der Wirkweise?",
        answerA = "siRNA und miRNA sind identisch im Mechanismus; der einzige Unterschied ist die laengere Sequenz der miRNA (25 nt vs. 21-23 nt)",
        answerB = "siRNA wird vollstaendig komplementaer zur Ziel-mRNA geladen in Ago2-RISC; bei perfekter Paarung katalysiert Ago2 endonukleolytische Spaltung (Slicing) der mRNA. miRNA bindet mit unvollstaendiger Komplementaritaet (meist an 3'-UTR, Seed-Region nt 2-8) und hemmt Translation und/oder foerdert Deadenylierung ohne direktes Slicing",
        answerC = "siRNA hemmt die Transkription durch Chromatin-Silencing; miRNA hemmt Translation durch Ribosomendissoziation am Initiationskomplex",
        answerD = "Beide werden ueber Dicer verarbeitet und in Ago1 geladen; der Unterschied besteht darin, dass siRNA vom Minusstrang, miRNA vom Plusstrang abgelesen wird",
        correctAnswer = 1,
        explanation = "Perfekte Doppelstrang-Komplementaritaet von siRNA/Ziel-mRNA erlaubt Ago2-vermitteltes Slicing (endonukleolytische Spaltung zwischen nt 10 und 11 des Guides). miRNA hat typischerweise nur die 'Seed'-Region (nt 2-8) komplementaer, was Translationsrepression und mRNA-Destabilisierung (via CCR4-NOT-Komplex) ausloest, aber kein Slicing. Therapeutische siRNAs sind vollstaendig komplementaer und nutzen Ago2-Slicing.",
        difficulty = 5,
        funFact = "Andrew Fire und Craig Mello erhielten 2006 den Nobelpreis fuer Medizin fuer die Entdeckung der RNA-Interferenz. Inclisiran (Leqvio) ist ein siRNA-Therapeutikum gegen PCSK9-mRNA in der Leber: Zwei Injektionen pro Jahr senken LDL-Cholesterin um ~50 % -- aehnlich effektiv wie PCSK9-Antikoerper mit massiv verringerter Injektionsfrequenz."
    ),

    // Question 23 -- Genetik: Splicing-Mutationen
    Question(
        categoryId = 16,
        questionText = "Splicing-Mutationen sind fuer ~15 % aller erblichen Erkrankungen verantwortlich. Welche Konsensussequenzen definieren Donor- und Akzeptorspleissstellen, und wie erklaert eine Spleissstellen-Mutation die Pathogenese?",
        answerA = "Donor-Spleissstelle: 3'-AG-5'; Akzeptor-Spleissstelle: 5'-GT-3'; Mutationen erhoehen nur die Spleisseffizienz und verkuerzen Introns",
        answerB = "Donor-Spleissstelle (5'): GT (GU in RNA) am Intronbeginn; Akzeptor-Spleissstelle (3'): AG am Intronende + Polypyrimidintrakt + Branchpoint-Adenosin ~20-50 nt upstream. Spleissstellen-Mutationen in GT oder AG zerstoeren das U1/U2-snRNP-Erkennungssignal, was zu Exon-Skipping, Intron-Retention oder Aktivierung kryptischer Spleissstellen mit frameshift-Folgen fuehrt",
        answerC = "Spleisssignale sind rein proteinbasiert durch SR-Proteine; GT/AG-Konsensus ist bei Saeugetieren nicht konserviert",
        answerD = "Donor: GC (haeufigstes Dinukleotid); Akzeptor: AG; Mutationen beeinflussen ausschliesslich Exon-Definition, nicht Intronexzision",
        correctAnswer = 1,
        explanation = "GT-AG-Regel (fast invariant in Saeugetier-Praeprotein-mRNA): GT am 5'-Spleisssintron (bindet U1-snRNA) und AG am 3'-Spleissintronende (bindet U2AF). Branchpoint-A bildet mit 2'-OH die Lariat-Struktur. Mutationen in diesen Positionen verhindern spliceosomal-Erkennung. Alternative: kryptische Spleissstellen mit aehnlicher Sequenz werden aktiviert, haeufig frameshiftend. Splice-Switching-Antisense-Oligonukleotide (z.B. Nusinersen fuer SMA) nutzen exakt dieses Wissen therapeutisch.",
        difficulty = 5,
        funFact = "Nusinersen (Spinraza) ist ein Antisense-Oligonukleotid, das exon-7-Einschluss in SMN2-mRNA erzwingt: Es blockiert ISS-N1 (Intronic Splicing Silencer), sodass das Exon nicht ausgelassen wird und funktionelles SMN-Protein entsteht. Intrathecal appliziert alle 4 Monate, kostet es ~750.000 USD im ersten Jahr -- das war das teuerste Medikament vor Zolgensma."
    ),

    // Question 24 -- Pathologie: Tumor-Mikromilieu
    Question(
        categoryId = 16,
        questionText = "Das Tumor-Mikromilieu (TME) ist ein entscheidender Faktor fuer Immunevasion und Therapieresistenz. Welche Zelltypen und Mechanismen erklaeren, wie 'cold' (immune desert) Tumoren der Immunabwehr entkommen?",
        answerA = "Cold Tumoren fehlen vollstaendig Gefaesse und Stromazellen; der einzige Mechanismus ist Tumorzell-intrinsische Antigenlosigkeit",
        answerB = "Cold Tumoren haben niedrige mutational burden (wenig Neoantigene), rekrutieren immunsuppressive Zellen (FOXP3+ Tregs, M2-polarisierte Makrophagen, MDSCs), sezernieren inhibitorische Zytokine (TGF-beta, IL-10, VEGF), haben verminderte MHC-I-Expression und undurchlaessige Tumor-Stroma-Barrieren, die T-Zell-Infiltration physisch verhindern",
        answerC = "Cold Tumoren exprimieren konstitutiv alle Checkpoint-Liganden (PD-L1, PD-L2, CTLA-4-Liganden) und neutralisieren T-Zellen direkt an der Tumorgrenze durch Antigen-unabhaengige Apoptoseinduktion",
        answerD = "Cold Tumoren entstehen ausschliesslich durch Defekte in der Antigenprocessing-Maschinerie (TAP1/TAP2-Verlust), waehrend hot Tumoren TAP-kompetent sind",
        correctAnswer = 1,
        explanation = "Cold Tumoren haben ein immunsuppressives Mikromilieu mit: 1) Niedriger Tumormutationslast (TMB) -> wenig Neoantigene -> wenig T-Zell-Aktivierung. 2) Immunsuppressive Stromazellen (Tregs, M2-TAMs, MDSCs). 3) Inhibitorische Mediatoren (TGF-beta hemmt T-Zell-Funktion, IL-10 hemmt DC-Reifung, VEGF schuetzt vor T-Zell-Infiltration durch Gefaessabnormitaet). 4) Downregulierung von MHC-I, um T-Zell-Erkennung zu vermeiden. Checkpoint-Inhibitoren sind bei cold Tumoren wenig wirksam.",
        difficulty = 5,
        funFact = "Strategien zur Konvertierung von cold zu hot Tumoren umfassen: Strahlentherapie (induziert immunogenen Zelltod, abscopal-Effekt), onkolytische Viren, CAR-T, intratumorale Injektion von Adjuvantien (TLR-Agonisten), IDO-Inhibitoren und anti-TGF-beta. Kombinationstherapien dieser Ansaetze mit Checkpoint-Inhibitoren sind vielversprechend."
    ),

    // Question 25 -- Molekulare Kardiologie: Long-QT-Syndrom
    Question(
        categoryId = 16,
        questionText = "Das kongenitale Long-QT-Syndrom (LQTS) ist genetisch heterogen. LQT1 (KCNQ1) und LQT2 (KCNH2) sind die haeufigsten Formen. Welche ionenphysiologischen Mechanismen erklaeren den verlaengerten QT, und warum sind die Trigger-Situationen fuer lebensbedrohliche Arrhythmien typspezifisch?",
        answerA = "LQT1 und LQT2 beruhen auf identischen Na+-Kanal-Gain-of-function-Mutationen; Trigger-Unterschiede sind klinisch nicht signifikant",
        answerB = "LQT1: Loss-of-function in IKs (langsam aktivierender Kaliumstrom, KCNQ1) -> langsame Repolarisation, vor allem bei koerperlicher Belastung symptomatisch (da IKs fuer Frequenzadaptation zustaendig). LQT2: Loss-of-function in IKr (rapid activating K+-Strom, hERG/KCNH2) -> verlangsamte Repolarisation, Trigger sind emotionaler Stress und ploetzliche akustische Reize (Wecker, Telefonklingeln, Lautgeraeusche)",
        answerC = "LQT1 und LQT2 beruhen auf Kalziumkanal-Gain-of-function (CACNA1C); LQT1 ist Schwimmen-assoziiert, LQT2 Schlaf-assoziiert",
        answerD = "LQT1 hat Loss-of-function im INa-Kanal (SCN5A); LQT2 hat Loss-of-function in IK1 (KCNJ2); Unterschied: Ausdauer vs. Kurzsprint als Trigger",
        correctAnswer = 1,
        explanation = "IKs ist der Hauptrepolarisierungsreservemechanismus bei Belastung (Frequenzanstieg erfordert IKs-Aktivierung fuer kuerzere Aktionspotenziale). LQT1 (KCNQ1-Mutation) scheitert genau hier: bei Belastung reicht die Repolarisierung nicht -> TdP unter Belastung (Schwimmen typischer Trigger). IKr (hERG) ist haeufiger aktiv bei emotionalen/acoustischen Triggers: Hypokaliaemie und ploetzliche Herzfrequenzaenderungen destabilisieren IKr-Kanal zusaetzlich -> Risiko bei ploetzlichem Schreckreiz.",
        difficulty = 5,
        funFact = "Der hERG-Kanal (human Ether-a-go-go Related Gene) ist aussergewoehnlich anfaellig fuer pharmakologische Blockade wegen seiner grossen Kavitaet und aromatischen Aminosaeuren. Viele Medikamente (Antihistaminika, Antibiotika, Antipsychotika) wurden deshalb vom Markt genommen (Terfenadin, Cisaprid) -- hERG-Haftungstests sind jetzt fuer alle neuen Medikamente Pflicht."
    ),

    // Question 26 -- Forschung: Nanoskalige Wirkstofftraeger
    Question(
        categoryId = 16,
        questionText = "Lipid-Nanopartikel (LNP) wurden als Traegersystem fuer mRNA-COVID-Impfstoffe weltbekannt. Welche strukturellen Komponenten des LNP sind fuer die Effektivitaet entscheidend, und welcher zellulaere Mechanismus ermoeglicht die mRNA-Freisetzung ins Zytoplasma?",
        answerA = "LNPs bestehen ausschliesslich aus Cholesterin und Lecithin; mRNA wird durch passive Diffusion durch Membrankanaele freigesetzt",
        answerB = "LNPs enthalten 4 Lipidkomponenten: ionisierbares Lipid (pH-abhaengige Ladung, fuer endosomale Freisetzung), Hilfslipid (DSPC/DPPC, Stabilitaet), Cholesterin (Strukturintegritaet), PEG-Lipid (Stealth-Effekt, laengere Zirkulation). Bei endosomaler Aufnahme protonier das ionisierbare Lipid, destabilisiert die endosomale Membran durch Ionenpaar-Bildung mit anionischen Membranlipiden und setzt mRNA ins Zytosol frei",
        answerC = "LNPs fusionieren direkt mit der Plasmamembran; keine Endozytose ist noetig, mRNA gelangt direkt ins Zytoplasma ohne Kompartimentierung",
        answerD = "Das ionisierbare Lipid ist ausschliesslich fuer PEG-Bindung zustaendig; die mRNA-Freisetzung erfolgt durch Ribosomenbindung an der Endosomenmembran mit direkter Translokation",
        correctAnswer = 1,
        explanation = "Pionierstudien von Cullis, Anderson und anderen entwickelten ionisierbare Aminolipide (z.B. MC3, ALC-0315, SM-102), die bei neutralem pH ungeladen sind (Stealth) und bei endosomalem sauren pH protonieren. Dies erlaubt: 1) mRNA-Komplexierung bei pH ~4 (Produktion), 2) Plasma-Zirkulation bei pH 7,4, 3) nach endosomaler Aufnahme: Protonierung -> Membranfusion mit Endosom -> mRNA-Freisetzung. Die Kombination aller 4 Lipide ist kritisch fuer optimale Effizienz.",
        difficulty = 5,
        funFact = "Katalin Kariko und Drew Weissman erhielten 2023 den Nobelpreis fuer Medizin fuer die Entdeckung, dass Nukleosid-Modifikationen (insbesondere N1-Methylpseudouridin statt Uridin) in mRNA das angeborene Immunsystem umgehen und die Translationseffizienz erhoehen. Diese Entdeckung war die Grundlage fuer die COVID-mRNA-Impfstoffe, die in rekordverdaechtiger Zeit entwickelt wurden."
    ),

    // Question 27 -- Klinische Forschung: Adaptive Studiendesigns
    Question(
        categoryId = 16,
        questionText = "Adaptive klinische Studiendesigns erlauben vordefiniete Aenderungen am Studienprotokoll basierend auf Zwischenanalysen. Was ist der wesentliche methodische Vorteil und die groesste statistische Gefahr?",
        answerA = "Adaptive Designs eliminieren Typ-I- und Typ-II-Fehler vollstaendig durch kontinuierliche Bayesianische Anpassung; Gefahr besteht nur bei zu kleinen Ausgangsstichproben",
        answerB = "Adaptationen (Dosisanpassung, Arm-Dropping, Stichprobenerweiterung, Anreicherung) koennen Effizienz erhoehen, Exposition gegenueber inferioren Dosen reduzieren und Entwicklungszeit verkuerzen. Die Hauptgefahr ist Typ-I-Fehler-Inflation: Mehrfachauschauen auf Daten ('multiple looks') erhoehen die Wahrscheinlichkeit falsch-positiver Ergebnisse erheblich, wenn keine praespezifizierte alpha-spending-Funktion (z.B. O'Brien-Fleming, Pocock) angewendet wird",
        answerC = "Adaptive Designs sind nur fuer explorative Phase-I-Studien erlaubt; konfirmatorische Phase-III-Studien duerfen keine Adaptationen enthalten",
        answerD = "Der hauptsaechliche Vorteil ist die Moeglichkeit, Kontrollgruppen nachtraeglich hinzuzufuegen; die Gefahr besteht in mangelhafter Verblindung durch externe Statistiker",
        correctAnswer = 1,
        explanation = "Adaptive Designs sind regulatorisch akzeptiert (FDA-Guidance 2019, EMA-Guidance 2007) wenn klar praespezifiziert. Vorteile: Effizienz, ethische Verbesserung (weniger Patienten suboptimale Dosen), schnellere Entwicklung. Hauptproblem: Jedes Zwischenschauen erhoehrt den kumulativen alpha-Fehler. Gruppe-sequentielle Methoden mit alpha-spending-Funktionen halten das gesamte Typ-I-Fehler auf dem Zielniveau (z.B. 5 %). O'Brien-Fleming ist konservativ frueh, liberaler spaet; Pocock ist uniform ueber alle Zwischenauschauen.",
        difficulty = 5,
        funFact = "Das I-SPY-2-Programm in der Brustkrebsforschung ist das bekannteste adaptive Platform-Trial: Mehrere Investigational Agents werden simultan in einem adaptiven Bayesianischen Design getestet; Agenten 'graduieren' in Phase III nur bei ausreichender Wahrscheinlichkeit in vordefinierten Biomarker-Signaturen. Dies hat die neoadjuvante Brustkrebstherapie-Forschung erheblich beschleunigt."
    )
)
