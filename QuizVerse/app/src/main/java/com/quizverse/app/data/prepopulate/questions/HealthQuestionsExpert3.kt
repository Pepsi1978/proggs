package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun healthQuestionsExpert3(): List<Question> = listOf(

    // Question 1 -- DNA Repair: Base Excision Repair
    Question(
        categoryId = 16,
        questionText = "Welches Enzym initiiert die Basen-Exzisions-Reparatur (BER), indem es eine geschaedigte Base aus dem DNA-Doppelstrang entfernt?",
        answerA = "DNA-Polymerase beta",
        answerB = "AP-Endonuklease 1 (APE1)",
        answerC = "DNA-Glykosylase",
        answerD = "XRCC1-Ligase",
        correctAnswer = 2,
        explanation = "DNA-Glykosylasen erkennen spezifische chemisch modifizierte Basen und schneiden die N-glykosidische Bindung zwischen Base und Desoxyribose, wodurch eine AP-Stelle (apurinisch/apyrimidinisch) entsteht. Danach folgen APE1, Polymerase beta und abschliessend eine Ligase.",
        difficulty = 4,
        funFact = "Es gibt ueber ein Dutzend verschiedene DNA-Glykosylasen -- jede erkennt eine andere Art von Basenschaden, z.B. OGG1 fuer 8-Oxoguanin, das durch reaktive Sauerstoffspezies entsteht."
    ),

    // Question 2 -- DNA Repair: Mismatch Repair
    Question(
        categoryId = 16,
        questionText = "Welches Protein-Heterodimer erkennt im Mismatch-Reparatur-System (MMR) bevorzugt Insertions-/Deletions-Schleifen in der DNA?",
        answerA = "MSH2-MSH6 (MutSalpha)",
        answerB = "MSH2-MSH3 (MutSbeta)",
        answerC = "MLH1-PMS2 (MutLalpha)",
        answerD = "MLH1-MLH3 (MutLgamma)",
        correctAnswer = 1,
        explanation = "MutSbeta (MSH2-MSH3) erkennt bevorzugt Insertions-/Deletions-Schleifen (IDLs) von 2-13 Nukleotiden, waehrend MutSalpha (MSH2-MSH6) hauptsaechlich einzelne Basenfehlpaarungen und kleine IDLs erkennt. MutLalpha koordiniert anschliessend die Entfernungsreaktion.",
        difficulty = 4,
        funFact = "Mutationen in MMR-Genen (MLH1, MSH2, MSH6, PMS2) verursachen das Lynch-Syndrom -- die haeufigste erbliche Dickdarmkrebsform mit einem Lebenszeitrisiko von bis zu 80%."
    ),

    // Question 3 -- DNA Repair: BRCA1/2
    Question(
        categoryId = 16,
        questionText = "BRCA2 spielt eine zentrale Rolle bei der homologen Rekombination (HR). Welche molekulare Funktion uebernimmt BRCA2 dabei direkt?",
        answerA = "BRCA2 phosphoryliert H2AX und markiert damit den DNA-Schadensort",
        answerB = "BRCA2 ladet RAD51 auf ssDNA und foerdert so die Stranginvasion",
        answerC = "BRCA2 besitzt eine intrinsische Endonukleaseaktivitaet fuer den Einzel-Strang-Abbau",
        answerD = "BRCA2 blockiert die Aktivierung von ATM/ATR an Doppelstrangbruechen",
        correctAnswer = 1,
        explanation = "BRCA2 interagiert direkt mit RAD51 ueber acht BRC-Repeats und einen C-terminalen BRCT-Bereich. Es verdraengt RPA von einzelstraengiger DNA (ssDNA) und ladet stattdessen RAD51-Monomere, die den geschwesterlichen Chromatid als Matrize suchen (Stranginvasion).",
        difficulty = 4,
        funFact = "Tumorzellen mit BRCA1/2-Verlust sind extrem empfindlich gegenueber PARP-Inhibitoren -- ein Prinzip namens 'synthetische Lethalitaet', das die Krebstherapie revolutioniert hat."
    ),

    // Question 4 -- Cell Signaling: MAPK/RAS Pathway
    Question(
        categoryId = 16,
        questionText = "In der RAS-MAPK-Kaskade: Welches Ereignis haelt RAS im aktiven, GTP-gebundenen Zustand bei vielen Krebsarten dauerhaft aufrecht?",
        answerA = "Ueberexpression von SOS (Son of Sevenless) als Guanin-Nukleotid-Austauschfaktor",
        answerB = "Verlust der intrinsischen GTPase-Aktivitaet durch Punktmutationen (z.B. G12V, G12D)",
        answerC = "Phosphorylierung von RAS durch RTKs an Tyrosin-32",
        answerD = "Deletion des GAP-Bindebereichs fuehrt zu erhoehter SOS-Rekrutierung",
        correctAnswer = 1,
        explanation = "Onkogene RAS-Mutationen an Codon 12 oder 13 (z.B. KRAS G12D) blockieren die intrinsische GTPase-Aktivitaet und verhindern auch die GAP-stimulierte GTP-Hydrolyse. RAS bleibt dauerhaft GTP-gebunden und aktiv, was zu konstitutiver Proliferationssignalisierung fuehrt.",
        difficulty = 4,
        funFact = "KRAS ist das am haeufigsten mutierte Onkogen beim Menschen -- es ist in ca. 90% der Pankreaskarzinome, 40% der Kolonkarzinome und 30% der Lungenkarzinome mutiert."
    ),

    // Question 5 -- Cell Signaling: PI3K/AKT/mTOR
    Question(
        categoryId = 16,
        questionText = "Welche unmittelbare biochemische Funktion hat PI3-Kinase (PI3K) in der PI3K/AKT/mTOR-Signalkaskade?",
        answerA = "PI3K phosphoryliert AKT direkt an Threonin-308",
        answerB = "PI3K konvertiert PIP2 zu PIP3, das als second messenger AKT-Rekrutierung an die Membran vermittelt",
        answerC = "PI3K aktiviert mTORC1 direkt durch Phosphorylierung von Raptor",
        answerD = "PI3K degradiert PTEN und hebt damit die Inhibition von AKT auf",
        correctAnswer = 1,
        explanation = "PI3K phosphoryliert Phosphatidylinositol-4,5-bisphosphat (PIP2) zu Phosphatidylinositol-3,4,5-trisphosphat (PIP3). PIP3 rekrutiert AKT und PDK1 ueber deren PH-Domaenen an die Plasmamembran, wo PDK1 dann AKT an T308 phosphoryliert und aktiviert.",
        difficulty = 4,
        funFact = "Der Tumorsuppressor PTEN ist die Phosphatase, die PIP3 wieder zu PIP2 abbaut -- PTEN ist nach TP53 das am zweithaeufigsten in Tumoren inaktivierte Gen."
    ),

    // Question 6 -- Cell Signaling: mTOR Complexes
    Question(
        categoryId = 16,
        questionText = "Worin unterscheiden sich mTORC1 und mTORC2 funktionell bei der Regulierung des Zellwachstums?",
        answerA = "mTORC1 phosphoryliert S6K1 und 4E-BP1 fuer Proteinsynthese; mTORC2 phosphoryliert AKT an Ser473 fuer vollstaendige Aktivierung",
        answerB = "mTORC1 ist Rapamycin-resistent; mTORC2 wird direkt durch Rapamycin gehemmt",
        answerC = "mTORC1 aktiviert Autophagie; mTORC2 hemmt Autophagie durch AMPK-Phosphorylierung",
        answerD = "mTORC1 und mTORC2 haben identische Substrate, unterscheiden sich nur in der subzellularen Lokalisation",
        correctAnswer = 0,
        explanation = "mTORC1 (mit Raptor) reguliert die Proteinsynthese ueber S6K1 und 4E-BP1 und ist akut durch Rapamycin hemmbar. mTORC2 (mit Rictor) phosphoryliert AKT an Ser473 (Hydrophobic Motif) fuer vollstaendige Aktivierung sowie SGK1 und PKCalpha.",
        difficulty = 4,
        funFact = "Rapamycin wurde 1972 auf der Osterinsel (Rapa Nui) aus dem Bodenbakterium Streptomyces hygroscopicus isoliert -- daher der Name. Es wird heute als Immunsuppressivum nach Organtransplantation eingesetzt."
    ),

    // Question 7 -- Cell Signaling: Wnt Pathway
    Question(
        categoryId = 16,
        questionText = "Im kanonischen Wnt-Signalweg: Was verhindert im Ruhezustand (ohne Wnt-Ligand) die Aktivierung von Zielgenen?",
        answerA = "Wnt-Rezeptoren sind im inaktiven Zustand konstitutiv internalisiert",
        answerB = "Der Destruktionskomplex (APC, Axin, CK1, GSK3beta) phosphoryliert beta-Catenin und markiert es fuer den proteasomalen Abbau",
        answerC = "TCF/LEF-Transkriptionsfaktoren sind durch HDAC-Komplexe permanent reprimiert und koennen nicht von beta-Catenin gebunden werden",
        answerD = "Dishevelled (Dvl) sequestiert beta-Catenin im Zytoplasma durch direkte Bindung",
        correctAnswer = 1,
        explanation = "Im Ruhezustand phosphoryliert der Destruktionskomplex beta-Catenin sequenziell: CK1 an Ser45, dann GSK3beta an Thr41, Ser37 und Ser33. Diese Phosphorylierungen ermoeglichen die Ubiquitinierung durch beta-TrCP und den proteasomalen Abbau. Wnt-Signale blockieren diesen Komplex.",
        difficulty = 4,
        funFact = "Mutationen im APC-Gen, das Teil des Destruktionskomplexes ist, sind die Ursache der familiaren adenomatoesen Polyposis (FAP) -- betroffene Patienten entwickeln Tausende von Darmpolypen."
    ),

    // Question 8 -- Cell Signaling: Notch Pathway
    Question(
        categoryId = 16,
        questionText = "Welcher einzigartige Schritt macht den Notch-Signalweg fundamentell anders als klassische Rezeptor-Tyrosinkinase-Wege?",
        answerA = "Notch-Rezeptoren sind G-Protein-gekoppelt und aktivieren Adenylylzyklase direkt",
        answerB = "Der Notch-Rezeptor selbst wird nach Ligandenbindung proteolytisch gespalten; die intrazellulare Domaene wandert direkt in den Kern und wirkt als Koaktivator",
        answerC = "Notch-Signale werden ausschliesslich durch microRNA-Molekuele im Zellkern transduciert",
        answerD = "Notch erfordert Homodimerisation beider Rezeptoren auf der gleichen Zelle (cis-Aktivierung)",
        correctAnswer = 1,
        explanation = "Nach Ligandenbindung (Jagged/Delta-like) erfolgen zwei proteolytische Spaltungen: ADAM-Metalloproteasen spalten extrazellulaar, gamma-Sekretase spaltet intramembranos. Die freigesetzte Notch-Intrazellulaerdomaene (NICD) transloziert in den Kern und aktiviert mit CSL/RBPJ Zielgene (HES, HEY).",
        difficulty = 4,
        funFact = "gamma-Sekretase, die Notch spaltet, spaltet auch das Amyloid-Vorlauferprotein (APP) -- deshalb werden gamma-Sekretase-Inhibitoren als Alzheimer-Therapeutika untersucht, stoessen aber auf Toxizitaetsprobleme durch Notch-Hemmung."
    ),

    // Question 9 -- Apoptosis: Intrinsic Pathway
    Question(
        categoryId = 16,
        questionText = "Wie loest Cytochrom c nach seiner Freisetzung aus der inneren Mitochondrienmembran die Caspase-Kaskade aus?",
        answerA = "Cytochrom c aktiviert direkt Caspase-9 durch allosterische Bindung an die CARD-Domaene",
        answerB = "Cytochrom c bindet APAF-1, induziert dessen Oligomerisierung zum Apoptosom und rekrutiert Pro-Caspase-9",
        answerC = "Cytochrom c hemmt XIAP und erlaubt so die spontane Autoaktivierung von Caspase-3",
        answerD = "Cytochrom c aktiviert Smac/DIABLO, das dann Caspase-8 in der intrinsischen Kaskade aktiviert",
        correctAnswer = 1,
        explanation = "Freigesetztes Cytochrom c bindet APAF-1 (Apoptotic Protease Activating Factor 1) und induziert eine konformationelle Aenderung, die ATP-Hydrolyse und Oligomerisierung zu einem Heptamer ermoeglicht -- dem Apoptosom. Dieses rekrutiert und aktiviert Pro-Caspase-9, die dann Effektor-Caspasen (3, 7) spaltet.",
        difficulty = 4,
        funFact = "Das Apoptosom wurde 2001 durch Cryo-Elektronen-Mikroskopie sichtbar gemacht und sieht aus wie ein Rad mit sieben Speichen -- deshalb wird es manchmal als 'death wheel' bezeichnet."
    ),

    // Question 10 -- Apoptosis: Bcl-2 Family
    Question(
        categoryId = 16,
        questionText = "Wie verhindern anti-apoptotische Bcl-2-Proteine (Bcl-2, Bcl-xL) die Freisetzung von Cytochrom c?",
        answerA = "Sie blockieren die Aktivierung von Caspase-9 im Zytoplasma durch direkte Bindung",
        answerB = "Sie binden und sequestrieren pro-apoptotische BH3-only-Proteine sowie aktiviertes BAX/BAK und verhindern so deren Oligomerisierung in der Mitochondrienmembran",
        answerC = "Sie schliessen den VDAC-Kanal in der aeusseren Mitochondrienmembran und blockieren den Cytochrom-c-Export",
        answerD = "Sie stabilisieren die innere Mitochondrienmembran durch Cardiolipin-Bindung",
        correctAnswer = 1,
        explanation = "Anti-apoptotische Bcl-2-Proteine haben eine hydrophobe Bindungsgrube (BH3-Bindegrube), in die pro-apoptotische BH3-only-Proteine (BIM, PUMA, NOXA) sowie aktiviertes BAX/BAK binden. Dadurch wird die BAX/BAK-Oligomerisierung in der aeusseren Mitochondrienmembran verhindert, die Porenbildung bleibt aus.",
        difficulty = 4,
        funFact = "Venetoclax, ein selektiver BCL-2-Inhibitor, ist seit 2016 fuer die Behandlung der chronischen lymphatischen Leukaemie (CLL) zugelassen -- es war der erste klinisch eingesetzte BH3-Mimetiker."
    ),

    // Question 11 -- Apoptosis: Extrinsic Pathway
    Question(
        categoryId = 16,
        questionText = "Welche Rolle spielt FLIP (FLICE-inhibitory protein) in der extrinsischen Apoptose-Kaskade?",
        answerA = "FLIP ist eine aktive Caspase-8, die den DISC-Komplex stabilisiert",
        answerB = "FLIP hemmt die Aktivierung von Caspase-8 am DISC, indem es als katalytisch inaktives Caspase-8-Homolog die Bindestellen blockiert",
        answerC = "FLIP aktiviert NF-kB und foerdert damit die Expression von Bcl-2",
        answerD = "FLIP bindet FADD und verhindert dessen Rekrutierung an den Todesrezeptor",
        correctAnswer = 1,
        explanation = "c-FLIP (cellular FLICE-like Inhibitory Protein) besitzt zwei DED-Domaenen (Death Effector Domains) wie Caspase-8, aber keine funktionierende Protease-Domaene. Es kompetiert mit Pro-Caspase-8 um FADD-Bindung am DISC und verhindert so die Caspase-8-Prozessierung. Krebszellen ueberexprimieren oft c-FLIP.",
        difficulty = 4,
        funFact = "Die extrinsische Apoptose ueber den TRAIL-Rezeptor ist relativ tumorselektiv -- TRAIL toetet viele Tumorzellen, schont aber normale Zellen, was es zu einem interessanten Therapieansatz macht."
    ),

    // Question 12 -- Apoptosis: Caspase Cascade
    Question(
        categoryId = 16,
        questionText = "Was unterscheidet Initiator-Caspasen (Caspase-8, -9) mechanistisch von Effektor-Caspasen (Caspase-3, -7)?",
        answerA = "Initiator-Caspasen haben keine Prodomaene; Effektor-Caspasen besitzen lange Prodomain mit DED oder CARD",
        answerB = "Initiator-Caspasen aktivieren sich durch Dimerisierung nach Plattformrekrutierung; Effektor-Caspasen liegen als inaktive Dimere vor und werden durch proteolytische Spaltung aktiviert",
        answerC = "Initiator-Caspasen sind ausschliesslich mitochondrial; Effektor-Caspasen sind nur im Zellkern aktiv",
        answerD = "Initiator-Caspasen spalten ausschliesslich Prokaspasen; Effektor-Caspasen spalten ausschliesslich zytoskelettale Proteine",
        correctAnswer = 1,
        explanation = "Initiator-Caspasen (8, 9) werden durch proximity-induced Dimerisierung an Protein-Plattformen (DISC, Apoptosom) aktiviert -- ihr Mechanismus heisst 'induced proximity'. Effektor-Caspasen (3, 7) existieren als stabile, inaktive Zymogen-Dimere und werden durch interdomaene-Linker-Spaltung durch Initiator-Caspasen aktiviert.",
        difficulty = 4
    ),

    // Question 13 -- Gene Therapy: Viral Vectors
    Question(
        categoryId = 16,
        questionText = "Welcher virale Vektor eignet sich am besten fuer stabile, langfristige Gentransfer in nicht-teilende Zellen (z.B. Neuronen)?",
        answerA = "Adenovirus (serotyp 5)",
        answerB = "Adeno-assoziiertes Virus (AAV)",
        answerC = "Retrovirus (MLV-basiert)",
        answerD = "Herpes-simplex-Virus Amplicon",
        correctAnswer = 1,
        explanation = "AAV kann in nicht-teilende Zellen transduzieren und bleibt vorwiegend episomal mit langer Expressionsdauer. Es hat ein niedriges Immunogenitaetsprofil und keine bekannte Pathogenitaet beim Menschen. Retroviren benoetigen Zellteilung fuer die Integration; Adenoviren exprimieren nur transient.",
        difficulty = 4,
        funFact = "Zolgensma (Onasemnogene abeparvovec), eine AAV9-basierte Gentherapie fuer spinale Muskelatrophie (SMA), kostet ca. 2,1 Millionen US-Dollar -- es war zeitweise das teuerste Medikament der Welt."
    ),

    // Question 14 -- Gene Therapy: CRISPR-Cas9
    Question(
        categoryId = 16,
        questionText = "Welche strukturelle Voraussetzung in der Ziel-DNA ist fuer den Cas9-Schnitt durch das CRISPR-Cas9-System zwingend erforderlich?",
        answerA = "Eine palindromische Sequenz flankiert die Ziel-DNA beidseitig",
        answerB = "Ein PAM (Protospacer Adjacent Motif) -- fuer SpCas9 die Sequenz NGG -- muss unmittelbar 3' der Zielsequenz im Nicht-Template-Strang liegen",
        answerC = "Die Zielsequenz muss eine offene Chromatin-Struktur (DNase I-hypersensitiv) aufweisen",
        answerD = "Die Zielsequenz muss im kodierenden Strang liegen und ein ATG-Startkodon enthalten",
        correctAnswer = 1,
        explanation = "SpCas9 erkennt das PAM (5'-NGG-3') durch seine PAM-interacting domain (PI). Die guide-RNA hybridisiert mit den 20 Nukleotiden 5' des PAM. Cas9 schneidet beide Straenge 3 bp upstream vom PAM, wodurch ein stumpfes oder 1-nt-3'-ueberhang-Ende entsteht.",
        difficulty = 4,
        funFact = "Die Entdeckung der CRISPR-Cas9-Genschere brachte Jennifer Doudna und Emmanuelle Charpentier 2020 den Nobelpreis fuer Chemie -- eine der schnellsten Nobelpreis-Vergaben nach einer Entdeckung (nur 8 Jahre)."
    ),

    // Question 15 -- Gene Therapy: Antisense Oligonucleotides
    Question(
        categoryId = 16,
        questionText = "Wie wirken Antisense-Oligonukleotide (ASOs) vom Gapmer-Typ auf die Ziel-mRNA?",
        answerA = "Gapmer-ASOs aktivieren den RNA-induzierten Silencing-Komplex (RISC) analog zu siRNA",
        answerB = "Die zentrale DNA-Luecke des Gapmers hybridisiert mit der Ziel-mRNA und rekrutiert RNase H, die den RNA-Strang des Hybrids degradiert",
        answerC = "Gapmer-ASOs blockieren die 5'-Cap-Struktur der mRNA und verhindern die Translationsinitiation",
        answerD = "Gapmer-ASOs methylieren die Ziel-mRNA am N6-Adenosin und markieren sie fuer den nonsense-mediated decay",
        correctAnswer = 1,
        explanation = "Gapmer-ASOs bestehen aus einer zentralen DNA-Region (meist 8-10 nt), flankiert von modifizierten RNA-Regionen (z.B. 2'-MOE oder LNA). Der DNA-RNA-Hybrid rekrutiert RNase H1, die den RNA-Strang des Hybrids spezifisch spaltet und abbaut. Die modifizierten Flanken erhoehen Stabilitaet und Affinitaet.",
        difficulty = 4,
        funFact = "Nusinersen (Spinraza) ist ein ASO fuer SMA -- es aendert das Splicing von SMN2 und rettet ein funktionelles SMN-Protein. Es muss jedoch intrathecal injiziert werden, da ASOs die Blut-Hirn-Schranke nicht passieren."
    ),

    // Question 16 -- Stem Cell Biology: iPSC
    Question(
        categoryId = 16,
        questionText = "Welche vier Transkriptionsfaktoren (Yamanaka-Faktoren) sind hinreichend, um somatische Zellen in induzierte pluripotente Stammzellen (iPSC) zu reprogrammieren?",
        answerA = "OCT4, SOX17, KLF4, c-MYC",
        answerB = "OCT4, SOX2, KLF4, c-MYC",
        answerC = "NANOG, SOX2, KLF4, LIN28",
        answerD = "OCT4, NANOG, KLF4, c-MYC",
        correctAnswer = 1,
        explanation = "Die urspruenglichen Yamanaka-Faktoren sind OCT4 (POU5F1), SOX2, KLF4 und c-MYC. OCT4 und SOX2 sind Kern-Pluripotenzfaktoren, KLF4 verstaerkt die Selbsterneuerung, c-MYC foerdert Proliferation und Chromatinremodellierung. c-MYC ist ein Proto-Onkogen und kann Tumorbildung begunstigen.",
        difficulty = 4,
        funFact = "Shinya Yamanaka erhielt 2012 den Nobelpreis fuer Medizin fuer die iPSC-Entdeckung. Als er sah, wie aus einer Hautzelle eine Stammzelle wird, soll er vor Ehrfurcht gezittert haben -- es widersprach allem, was man ueber Zelldifferenzierung wusste."
    ),

    // Question 17 -- Stem Cell Biology: Niche
    Question(
        categoryId = 16,
        questionText = "Was versteht man unter der 'Stammzell-Nische' und welche Funktion hat sie?",
        answerA = "Die Stammzell-Nische ist ein Zellorganell, das Stammzellen vor oxidativem Stress schuetzt",
        answerB = "Die Stammzell-Nische ist das spezialisierte mikrophysikalische und biochemische Umfeld, das Stammzell-Selbsterneuerung, Quieszenz und Differenzierung reguliert",
        answerC = "Die Stammzell-Nische bezeichnet den genetischen Locus, der Stammzell-Transkriptionsfaktoren kodiert",
        answerD = "Die Stammzell-Nische ist ein Reservoir ruhender Stammzellen, die ausschliesslich bei Gewebeschaden aktiviert werden",
        correctAnswer = 1,
        explanation = "Die Stammzell-Nische umfasst Nachbarzellen, extrazellulaere Matrix, Sauerstoffspannung, mechanische Kraefte und loesliche Faktoren (Wnt, Notch, BMP, SCF). Sie haelt Stammzellen in Quieszenz (G0) oder foerdert kontrollierte Teilungen. Ohne die Nische verlieren Stammzellen oft ihre Identitaet.",
        difficulty = 4,
        funFact = "Haarfollikel-Stammzellen liegen in einer Nische namens 'Bulge' -- sie sind die einzigen epidermalen Stammzellen, die Haare, Talgdruesen und Epidermis regenerieren koennen, aber meistens schlafen."
    ),

    // Question 18 -- Stem Cell Biology: Embryonic vs Adult
    Question(
        categoryId = 16,
        questionText = "Worin besteht der fundamentale Unterschied zwischen embryonalen Stammzellen (ESC) und adulten Stammzellen hinsichtlich ihres Potenzials?",
        answerA = "ESCs sind multipotent; adulte Stammzellen sind pluripotent",
        answerB = "ESCs sind pluripotent (koennen alle somatischen Zellen bilden); adulte Stammzellen sind meist multipotent oder unipotent, auf ein Gewebe beschraenkt",
        answerC = "ESCs koennen nur in vitro expandieren; adulte Stammzellen replizieren ausschliesslich in vivo",
        answerD = "ESCs exprimieren kein Telomerase; adulte Stammzellen haben unbegrenzte Replikationskapazitaet",
        correctAnswer = 1,
        explanation = "Pluripotente ESCs koennen theoretisch alle drei Keimblatt-Derivate erzeugen. Adulte Stammzellen (z.B. haematopoetische, mesenchymale) sind meist auf ihren Ursprungsgewebetyp beschraenkt (multipotent). Totipotenz -- auch Trophoblast bilden zu koennen -- besitzen nur fruehembryonale Zellen bis zum 8-Zell-Stadium.",
        difficulty = 4
    ),

    // Question 19 -- Epigenetics: DNA Methylation
    Question(
        categoryId = 16,
        questionText = "Welche DNMT-Enzyme sind fuer die Erhaltungs-Methylierung (Maintenance Methylation) versus die De-novo-Methylierung zustaendig?",
        answerA = "DNMT1 fuer De-novo-Methylierung; DNMT3A/3B fuer Erhaltung",
        answerB = "DNMT1 fuer Erhaltungs-Methylierung an hemimethylierten CpGs; DNMT3A und DNMT3B fuer De-novo-Methylierung",
        answerC = "DNMT2 fuer Erhaltungs-Methylierung; DNMT3L fuer De-novo-Methylierung",
        answerD = "Alle DNMTs haben identische Funktionen, unterscheiden sich nur in der Gewebeexpression",
        correctAnswer = 1,
        explanation = "DNMT1 erkennt hemimethylierte CpG-Dinukleotide nach der DNA-Replikation und methyliert den Tochterstrang -- dies erhalt das Methylierungsmuster. DNMT3A und DNMT3B setzen de novo Methylgruppen an zuvor unmethylierte CpGs, z.B. waehrend der Embryogenese und Gametogenese. DNMT3L ist ein Kofaktor ohne katalytische Aktivitaet.",
        difficulty = 4,
        funFact = "CpG-Inseln in Promotoren sind im gesunden Gewebe unmethyliert und transkriptionell aktiv -- in Krebszellen werden sie oft methyliert und Tumorsuppressorgene dadurch permanent abgeschaltet (epigenetisches Silencing)."
    ),

    // Question 20 -- Epigenetics: Histone Acetylation
    Question(
        categoryId = 16,
        questionText = "Wie beeinflusst die Acetylierung von Histon-H3 an Lysin-27 (H3K27ac) die Transkriptionsaktivitaet eines Gens?",
        answerA = "H3K27ac verdichtet das Chromatin und foerdert transkriptionelles Silencing",
        answerB = "H3K27ac neutralisiert die positive Ladung des Lysins, schwaecht die DNA-Nukleosom-Interaktion und markiert aktive Enhancer und Promotoren",
        answerC = "H3K27ac rekrutiert Polycomb-Repressorkomplexe (PRC1/PRC2) und foerdert H3K27-Trimethylierung",
        answerD = "H3K27ac blockiert die Ubiquitinierung von H2A und verhindert den DNA-Schadensresponse",
        correctAnswer = 1,
        explanation = "Acetylierung neutralisiert die positive Ladung von Lysin, was die elektrostatische Anziehung zur negativ geladenen DNA reduziert und das Chromatin in eine offene, transkriptionell permissive Konformation (Euchromatin) ueberfuehrt. H3K27ac ist ein Markenzeichen aktiver Enhancer und wird von Histon-Acetyltransferasen (HATs) wie p300/CBP gesetzt.",
        difficulty = 4,
        funFact = "HDAC-Inhibitoren wie Vorinostat (SAHA) nutzen die Umkehrbarkeit der Histonacetylierung therapeutisch -- sie reaktivieren epigenetisch abgeschaltete Tumorsuppressorgene und werden in der Krebstherapie eingesetzt."
    ),

    // Question 21 -- Epigenetics: H3K4 vs H3K27 Methylation
    Question(
        categoryId = 16,
        questionText = "Welche Histon-Modifikation markiert aktive Genpromotoren (H3K4me3) und welche markiert Polycomb-reprimierte Gene (H3K27me3)?",
        answerA = "H3K4me3 durch EZH2 gesetzt; H3K27me3 durch SET1/MLL-Komplexe",
        answerB = "H3K4me3 durch SET1/MLL-Komplexe; H3K27me3 durch EZH2 im PRC2-Komplex",
        answerC = "H3K4me3 durch PRMT5; H3K27me3 durch DOT1L",
        answerD = "Beide Modifikationen werden durch denselben Enzym-Komplex gesetzt, unterscheiden sich nur in der Zellteilung",
        correctAnswer = 1,
        explanation = "H3K4me3 wird durch SET1-/MLL-Komplexe (KMT2-Familie) an aktiven Promotoren gesetzt und rekrutiert Transkriptionsinitiation-Faktoren. H3K27me3 wird durch EZH2 (die katalytische Untereinheit von PRC2) gesetzt und foerdert kompaktes, transkriptionell inaktives Chromatin (Heterochromatin). 'Bivalente' Chromatin-Domanen tragen beide Marken -- typisch fuer Stamm-/Vorlaeufer-Zellen.",
        difficulty = 4,
        funFact = "EZH2-Mutationen (gain-of-function) kommen in diffus grossen B-Zell-Lymphomen vor -- EZH2-Inhibitoren wie Tazemetostat sind daher neuartige Krebstherapeutika."
    ),

    // Question 22 -- Proteomics: Mass Spectrometry
    Question(
        categoryId = 16,
        questionText = "Welches Prinzip nutzt die Tandem-Massenspektrometrie (MS/MS) zur Identifizierung von Peptiden in der Proteomik?",
        answerA = "Peptide werden durch MALDI ionisiert, ihre Masse im Flugzeitanalysator bestimmt und mit genomischen Datenbanken verglichen",
        answerB = "Ein Vorlaufer-Ion (Peptid) wird ausgewaehlt, durch Kollisions-induzierte Dissoziation (CID) in b- und y-Ionen fragmentiert; das Fragmentmuster ergibt die Aminosaeure-Sequenz",
        answerC = "Peptide werden durch Edman-Abbau sequenziert und die Masse jedes abgespaltenen Aminosaeure-Phenylthiohydantoin-Derivats gemessen",
        answerD = "Proteine werden intakt im Orbitrap-Analysator auf ihre exakte Masse gemessen ohne vorherige Fragmentierung",
        correctAnswer = 1,
        explanation = "Bei MS/MS wird ein Peptid-Vorlaufer-Ion aus dem ersten MS-Scan isoliert und in einer Kollisionszelle fragmentiert (CID, HCD). Die resultierenden Fragmentionen (b-Ionen: N-terminale Fragmente; y-Ionen: C-terminale Fragmente) bilden eine Leiter, aus der die Aminosaeure-Sequenz abgeleitet wird.",
        difficulty = 4,
        funFact = "Moderne Proteomik-Instrumente wie das Orbitrap Eclipse koennen bis zu 20 Peptide pro Sekunde sequenzieren -- ein komplettes menschliches Proteom mit ueber 20.000 Proteinen kann in einem einzigen Tag analysiert werden."
    ),

    // Question 23 -- Proteomics: Post-Translational Modifications
    Question(
        categoryId = 16,
        questionText = "Die Ubiquitinierung von Proteinen an Lysin-48 (K48-Ubiquitin-Ketten) versus Lysin-63 (K63-Ketten) fuehrt zu unterschiedlichen zellularen Schicksalen. Was sind diese?",
        answerA = "K48-Ketten markieren Proteine fuer den lysosomalen Abbau; K63-Ketten aktivieren NF-kB-Signalisierung und DNA-Reparatur",
        answerB = "K48-Ketten markieren Proteine fuer den proteasomalen Abbau (26S-Proteasom); K63-Ketten signalisieren in DNA-Schadensresponse, Endozytose und NF-kB-Aktivierung",
        answerC = "K48-Ketten aktivieren Caspase-3 direkt; K63-Ketten hemmen die Proteasom-Aktivitaet",
        answerD = "K48 und K63-Ketten sind funktionell aequivalent; ihre Funktion haengt nur von der Zahl der Ubiquitin-Molekuele ab",
        correctAnswer = 1,
        explanation = "K48-verknuepfte Polyubiquitin-Ketten werden vom 26S-Proteasom erkannt und leiten den proteolytischen Abbau ein. K63-Ketten bilden eine nicht-abbauende Signalplattform fuer DNA-Doppelstrangbruch-Reparatur (RNF8/RNF168), endosomales Sortieren (ESCRT) und NF-kB-Aktivierung (TRAF6).",
        difficulty = 4
    ),

    // Question 24 -- Metabolomics
    Question(
        categoryId = 16,
        questionText = "Was unterscheidet targeted von untargeted Metabolomik in ihrer Anwendung?",
        answerA = "Targeted Metabolomik quantifiziert eine definierte Menge bekannter Metaboliten absolut; untargeted Metabolomik erfasst moeglichst viele Metaboliten relativ ohne vorherige Hypothese",
        answerB = "Targeted Metabolomik nutzt NMR-Spektroskopie; untargeted Metabolomik nutzt ausschliesslich Massenspektrometrie",
        answerC = "Targeted Metabolomik ist nur fuer Plasmametaboliten geeignet; untargeted erfasst auch intrazellulaere Metaboliten",
        answerD = "Targeted und untargeted Metabolomik sind identisch -- der Begriff bezieht sich nur auf die bioinformatische Auswertung",
        correctAnswer = 0,
        explanation = "Targeted Metabolomik misst spezifische, vorab definierte Metaboliten mit kalibrierten Standards absolut (pmol/ml), hat hohe Sensitivitaet und Reproduzierbarkeit. Untargeted Metabolomik (global) erfasst alle messbaren Signale relativ, deckt unbekannte Metaboliten auf, erfordert aber komplexe Datenanalyse und Identifizierung.",
        difficulty = 4,
        funFact = "Der menschliche Metabolomik-Atlas (Human Metabolome Database, HMDB) enthalt ueber 200.000 beschriebene Metaboliten -- aber nur etwa 2.000 davon werden routinemaessig klinisch gemessen."
    ),

    // Question 25 -- Mitochondrial Diseases: OXPHOS
    Question(
        categoryId = 16,
        questionText = "Warum fuehren Mutationen im mitochondrialen Genom (mtDNA) zu einem heteroplasmatischen Phaenotyp, der sich von Mendelianischer Vererbung unterscheidet?",
        answerA = "Jede Zelle enthaelt Tausende von mtDNA-Kopien; Krankheit tritt erst auf, wenn mutierte mtDNA einen bestimmten Schwellenwert (meist 60-90%) ueberschreitet -- Heteroplasmie",
        answerB = "mtDNA-Mutationen sind immer rezessiv und zeigen variable Penetranz durch zufaellige X-Inaktivierung",
        answerC = "mtDNA wird biparental vererbt, was zu rekombinatorischer Vielfalt und variablem Ausdruck fuehrt",
        answerD = "Heteroplasmie entsteht durch horizontalen Gentransfer zwischen mitochondrialen und nuklearen DNA-Kopien",
        correctAnswer = 0,
        explanation = "Jede Zelle hat 100-1000 Mitochondrien mit je 2-10 mtDNA-Kopien. Beim Threshold-Effekt treten Symptome erst auf, wenn mutierte mtDNA den gewebsspezifischen Schwellenwert uebersteigt (z.B. 60% in Muskel, 90% in Gehirn). Durch Zellteilungen kann sich der Heteroplasmie-Anteil in Tochterzellen verschieben (Replikative Segregation).",
        difficulty = 4,
        funFact = "Das Leigh-Syndrom, eine schwere mitochondriale Enzephalopathie des Kindesalters, kann sowohl durch mtDNA- als auch nukleaere DNA-Mutationen ausgeloest werden -- was die Diagnose ausserordentlich komplex macht."
    ),

    // Question 26 -- Mitochondrial Diseases: Complex I Deficiency
    Question(
        categoryId = 16,
        questionText = "Welcher biochemische Mechanismus erklaert die selektive Neurodegeneration bei Komplex-I-Defizienz der mitochondrialen Atmungskette?",
        answerA = "Komplex-I-Mangel fuehrt zu ATP-Ueberproduktion, die neurotoxische Kalziumsignale ausloest",
        answerB = "Verminderte Komplex-I-Aktivitaet reduziert den protonenmotorischen Kraft und ATP-Synthese, erhoht gleichzeitig die ROS-Produktion -- Neuronen sind besonders vulnerabel wegen ihres hohen Energiebedarfs und geringer antioxidativer Kapazitaet",
        answerC = "Komplex-I-Mangel aktiviert spezifisch neuronale Apoptose durch mitochondriale Calcineurin-Aktivierung",
        answerD = "NADH akkumuliert und hemmt den Citratzyklus, was zu letaler Lactat-Aziditaet ausschliesslich in Neuronen fuehrt",
        correctAnswer = 1,
        explanation = "Komplex-I (NADH-Ubiquinon-Oxidoreduktase) ist der groesste OXPHOS-Komplex. Bei Defizienz sinkt der Protonengradient und die ATP-Synthese. Gleichzeitig steigen reaktive Sauerstoffspezies (ROS) durch unvollstaendige Elektronenuebertragung. Neuronen sind besonders betroffen, da sie 20% des Sauerstoffverbrauchs bei nur 2% Koerpermasse haben.",
        difficulty = 4
    ),

    // Question 27 -- Mitochondrial Diseases: Mitophagy
    Question(
        categoryId = 16,
        questionText = "Welche Rolle spielen PINK1 und Parkin bei der Mitophagie geschaedigter Mitochondrien?",
        answerA = "PINK1 und Parkin aktivieren die Fusion geschaedigter Mitochondrien, um den Schaden zu verduennen",
        answerB = "PINK1 akkumuliert auf geschaedigten Mitochondrien (geringes Membranpotenzial) und phosphoryliert Ubiquitin sowie Parkin, das dann Mitochondrien-Oberflachenproteine ubiquitiniert und Mitophagie initiiert",
        answerC = "PINK1 spaltet Parkin bei Mitochondrienschaden und setzt eine aktive Protease frei, die Outer-Membrane-Proteine abbaut",
        answerD = "PINK1 und Parkin regulieren mitochondriale Biogenese durch PGC-1alpha-Aktivierung",
        correctAnswer = 1,
        explanation = "An gesunden Mitochondrien wird PINK1 importiert und durch PARL gespalten. Bei Membranpotentialverlust akkumuliert PINK1 auf der aeusseren Membran, phosphoryliert Ubiquitin (Ser65) und Parkin (Ser65 in Ubiquitin-like domain). Aktiviertes Parkin ubiquitiniert OMM-Proteine (VDAC, MFN), was Autophagosom-Rekrutierung triggert.",
        difficulty = 4,
        funFact = "Mutationen in PINK1 und PARKIN verursachen autosomal-rezessiven fruehonset-Parkinson -- das erklaert, warum geschaedigte Mitochondrien eine zentrale Rolle in der Parkinson-Pathogenese spielen."
    ),

    // Question 28 -- Prion Diseases: Mechanism
    Question(
        categoryId = 16,
        questionText = "Welches molekulare Prinzip erklaert die infektioese Eigenschaft von Prionen ohne Beteiligung von Nukleinsaeuren?",
        answerA = "Prionen enthalten ein kleines nicht-kodierendes RNA-Molekuel, das als Replikationsmatrize dient",
        answerB = "Das fehlgefaltete PrPSc induziert konformationelle Umfaltung des wirtseigenen, normalen PrPC -- eine selbstpropagierendes Protein-Misfolding-Reaktion",
        answerC = "PrPSc aktiviert eine endogene retrovirale Sequenz, die neues PrPSc-kodierendes mRNA produziert",
        answerD = "Prionen sind missgefaltete Lipid-Protein-Komplexe, die durch Membranfusion in neue Zellen gelangen",
        correctAnswer = 1,
        explanation = "Gemaess der 'Protein-only hypothesis' von Stanley Prusiner induziert PrPSc (Scrapie-Form) die Umfaltung von PrPC (Cellular) von der alpha-helix-reichen in eine beta-Faltblatt-dominierte Konformation. Diese Reaktion ist autokatalytisch -- PrPSc multipliziert sich exponentiell, ohne Nukleinsaeuren zu benoetigen.",
        difficulty = 4,
        funFact = "Stanley Prusiner erhielt 1997 den Nobelpreis fuer Medizin fuer die Prion-Theorie -- gegen erheblichen wissenschaftlichen Widerstand, da die Idee eines infektioesen Proteins ohne Nukleinsaeure alle biologischen Dogmen brach."
    ),

    // Question 29 -- Prion Diseases: CJD
    Question(
        categoryId = 16,
        questionText = "Welches histopathologische Charakteristikum ist pathognomonisch fuer die Creutzfeldt-Jakob-Krankheit (CJD) im Gehirngewebe?",
        answerA = "Neurofibrillary tangles aus hyperphosphoryliertem Tau",
        answerB = "Lewy bodies aus aggregiertem alpha-Synuclein",
        answerC = "Spongiose Veraenderungen (vakuolisierte Neuropile), PrPSc-Ablagerungen und reaktive Astrogliose ohne signifikante Entzuendung",
        answerD = "Beta-Amyloid-Plaques mit Congo-Rot-positiven Kerne",
        correctAnswer = 2,
        explanation = "Die charakteristische 'schwammartige' (spongiose) Textur des Kortex entsteht durch intraneuronale Vakuolen. Zusaetzlich finden sich PrPSc-Ablagerungen (immunhistochemisch nachweisbar), reaktive Astrogliose und Neuronenverlust -- bemerkenswert ohne Entzuendungszellen, da das Immunsystem nicht auf Prionen reagieren kann.",
        difficulty = 4,
        funFact = "Die neue Variante der CJD (vCJD), verursacht durch den bovinen Spongioformen Enzephalopathie-Erreger (BSE/'Rinderwahn'), unterscheidet sich histologisch durch 'floride Plaques' -- PrP-Plaques umgeben von Vakuolen-Hofen."
    ),

    // Question 30 -- Prion Diseases: Kuru
    Question(
        categoryId = 16,
        questionText = "Wie erklaert sich die bemerkenswerte genetische Resistenz einiger Papua-Neuguineer gegen Kuru-Prionen?",
        answerA = "Sie besitzen eine Deletion im PRNP-Gen, die PrPC-Expression verhindert",
        answerB = "Heterozygotie fuer Met/Val am Codon 129 des PRNP-Gens verleiht relative Resistenz gegen Prionkrankheiten",
        answerC = "Sie produzieren Anti-PrP-Antikoerper durch wiederholte Exposition",
        answerD = "Ihr PrPC hat eine zusaetzliche Disulfidbruecke, die Misfolding verhindert",
        correctAnswer = 1,
        explanation = "Der Codon-129-Polymorphismus (Methionin/Valin) im PRNP-Gen beeinflusst die Prion-Suszeptibilitaet erheblich. Homozygote MM oder VV sind staerker gefaehrdet; Heterozygote MV zeigen deutlich verlaengerte Inkubationszeiten oder Resistenz. In Hochrisikopopulationen in Papua-Neuguinea war die MV-Frequenz gegenueber anderen Populationen erhoht.",
        difficulty = 4,
        funFact = "Kuru bedeutet in der Fore-Sprache 'Zittern' und wurde durch rituellen Kannibalismus (Essen von Hirngewebe Verstorbener) uebertragen. Seit dem Verbot dieser Praktiken verschwand die Krankheit fast vollstaendig."
    ),

    // Question 31 -- Microbiome: Gut-Brain Axis
    Question(
        categoryId = 16,
        questionText = "Ueber welche Hauptachsen kommuniziert das Darm-Mikrobiom mit dem Gehirn (Gut-Brain-Axis)?",
        answerA = "Ausschliesslich ueber Zytokine, die die Blut-Hirn-Schranke passieren",
        answerB = "Vagusnerv (afferente neuronale Signale), Immunsystem (Zytokine, Mikroglia-Aktivierung), HPA-Achse (Kortisol) und mikrobielle Metaboliten (SCFA, Tryptophan-Derivate) die systemisch zirkulieren",
        answerC = "Ausschliesslich durch bakterielle Lipopolysaccharide (LPS), die Neuroinflammation ausloesen",
        answerD = "Ueber direkte Zell-zu-Zell-Kommunikation durch Tunneling-Nanotubes zwischen Enterozyten und Neuronen",
        correctAnswer = 1,
        explanation = "Die bidirektionale Darm-Hirn-Achse umfasst: afferente Vagusfasern, die mikrobielle Signale weiterleiten; Immunmediatoren (IL-6, TNF); mikrobielle Metaboliten wie kurzkettige Fettsaeuren (SCFA), Serotonin (95% im Darm produziert) und Tryptophan-Metaboliten; sowie die HPA-Achse mit Kortisol als Stress-Mediator.",
        difficulty = 4,
        funFact = "Das Darmnervensystem ('zweites Gehirn') enthalt mehr Neuronen (100-500 Millionen) als das Rueckenmark -- und 90% der Signale im Vagusnerv laufen vom Darm zum Gehirn, nicht umgekehrt."
    ),

    // Question 32 -- Microbiome: Short-Chain Fatty Acids
    Question(
        categoryId = 16,
        questionText = "Welche metabolische Funktion haben kurzkettige Fettsaeuren (SCFA, hauptsaechlich Butyrat, Propionat, Acetat) fuer das Darmepithel?",
        answerA = "SCFAs sind ausschliesslich Signalmolekuele fuer intestinale Immunzellen ohne direkte metabolische Funktion",
        answerB = "Butyrat ist das primaere Energiesubstrat fuer Kolonozyten (60-70% des Energiebedarfs), hemmt HDAC und hat anti-inflammatorische sowie antiproliferative Wirkungen",
        answerC = "SCFAs aktivieren die Adenylylzyklase in Kolonozyten und erhoehen cAMP fuer Chlorid-Sekretion",
        answerD = "SCFAs neutralisieren den Darm-pH auf 7.4 und optimieren so die Verdauungsenzym-Aktivitaet",
        correctAnswer = 1,
        explanation = "Butyrat ist der Hauptenergiespender fuer Kolonozyten -- sie bevorzugen es sogar gegenueber Glukose. Als HDAC-Inhibitor epigenetisch aktiv foerdert es Differenzierung und hemmt Proliferation (Paradox: Energiequelle und Wachstumshemmer). Propionat geht zur Leber fuer Gluconeogenese; Acetat zirkuliert systemisch.",
        difficulty = 4,
        funFact = "Ballaststoffarme Ernaehrung reduziert Butyrat-produzierende Bakterien (Firmicutes wie Faecalibacterium prausnitzii) -- das koennte erklaren, warum westliche Ernaehrung mit erhoehtem Darmkrebsrisiko assoziiert ist."
    ),

    // Question 33 -- Microbiome: Dysbiosis
    Question(
        categoryId = 16,
        questionText = "Was versteht man unter 'Dysbiose' im Kontext des Darmmikrobioms und mit welchen Erkrankungen ist sie assoziiert?",
        answerA = "Dysbiose bezeichnet eine vorubergehende Verschiebung der Mikrobiom-Zusammensetzung durch Antibiotika, die sich nach Absetzen spontan normalisiert",
        answerB = "Dysbiose ist eine dauerhafte Imbalance der mikrobiellen Gemeinschaft mit Verlust protektiver Spezies, erhoehter pathobionter Abundanz und gestorter Mikrobiom-Funktion -- assoziiert mit CED, Metabolischem Syndrom, Neurodegenerationen und Autoimmunerkrankungen",
        answerC = "Dysbiose beschreibt ausschliesslich einen Verlust der Firmicutes/Bacteroidetes-Ratio unter 1:1",
        answerD = "Dysbiose ist ein reversibler Zustand, definiert durch temporaere Abwesenheit von Bifidobacterium",
        correctAnswer = 1,
        explanation = "Dysbiose bezeichnet eine qualitative und quantitative Stoerung des Mikrobioms: Reduktion der Diversitaet, Verlust kommensaler Schutzspezies, Zunahme pathogener oder proinflammatorischer Bakterien. Sie ist bei chronisch-entzuendlichen Darmerkrankungen (CED), Typ-2-Diabetes, Adipositas, Autismus-Spektrum-Stoerungen und Parkinson beschrieben.",
        difficulty = 4
    ),

    // Question 34 -- Microbiome: FMT
    Question(
        categoryId = 16,
        questionText = "Warum ist die Stuhltransplantation (Fecal Microbiota Transplantation, FMT) bei rekurrenter Clostridioides-difficile-Infektion so effektiv?",
        answerA = "FMT liefert Antikoerper gegen C.-difficile-Toxine aus dem Spenderstuhl",
        answerB = "FMT stellt die kolonisierungsresistente mikrobielle Gemeinschaft wieder her -- gesunde Kommensalen besetzten Nischen, die C. difficile vertreiben und Gallenaceuren-Metabolismus normalisieren",
        answerC = "FMT enthalt Bakteriophagen, die spezifisch C.-difficile-Staemme eliminieren",
        answerD = "FMT induziert eine IgA-Immunantwort durch neue mikrobielle Antigene, die C. difficile opsonisieren",
        correctAnswer = 1,
        explanation = "C. difficile nutzt antibiotika-bedingte Dysbiose und expandiert in freien Nischen. FMT stellt die normale mikrobielle Gemeinschaft wieder her, die durch Colonisation Resistance (direkter Naehrstoffwettbewerb, SCFA-Produktion, Bacteriocin-Sekretion, Gallenaceuren-Rekonvertierung) C. difficile verdraengt. Erfolgsrate: ueber 90%.",
        difficulty = 4,
        funFact = "Die erste dokumentierte Stuhltransplantation fuehrte der chinesische Arzt Ge Hong im 4. Jahrhundert durch -- er verabreichte 'gelbe Suppe' (Stuhltee) an Patienten mit schweren Durchfallerkrankungen."
    ),

    // Question 35 -- DNA Repair: Nucleotide Excision Repair
    Question(
        categoryId = 16,
        questionText = "Welches klinische Syndrom entsteht durch Defekte in der Nukleotid-Exzisions-Reparatur (NER) und welche Laesionen werden normalerweise durch NER repariert?",
        answerA = "Xeroderma pigmentosum durch NER-Defekt; NER repariert UV-induzierte Pyrimidin-Dimere (CPDs und 6-4-Photoprodukte) und andere sperrige Laesionen",
        answerB = "Cockayne-Syndrom durch NER-Defekt; NER repariert ausschliesslich oxidative Basenschaeden",
        answerC = "Fanconi-Anaemie durch NER-Defekt; NER repariert DNA-Quervernetzungen (interstrand crosslinks)",
        answerD = "Lynch-Syndrom durch NER-Defekt; NER repariert Basenfehlpaarungen nach der Replikation",
        correctAnswer = 0,
        explanation = "Xeroderma pigmentosum (XP) entsteht durch Mutationen in NER-Genen (XPA-XPG, XPV). NER repariert sperrige, helix-verzerrende Laesionen wie UV-induzierte Cyclobutan-Pyrimidin-Dimere (CPDs) und 6-4-Photoprodukte sowie chemisch erzeugte Addukte (Benzo[a]pyren). XP-Patienten haben ein >1000-fach erhoehtes Hautkrebsrisiko.",
        difficulty = 4,
        funFact = "XP-Patienten muessen UV-Licht komplett meiden -- sie leben oft nachtaktiv und tragen lichtundurchlaessige Kleidung. Selbst geringes Sonnenlicht kann zu malignen Tumoren fuehren, die schon im Kindesalter auftreten."
    ),

    // Question 36 -- Cell Signaling: Hedgehog Pathway
    Question(
        categoryId = 16,
        questionText = "Im Hedgehog-Signalweg: Welche Rolle spielt das Protein Patched (PTCH1) im Normalzustand (ohne Hedgehog-Ligand)?",
        answerA = "PTCH1 aktiviert GLI-Transkriptionsfaktoren durch Phosphorylierung",
        answerB = "PTCH1 hemmt den Transmembran-G-Protein-aehnlichen Rezeptor Smoothened (SMO) durch einen Cholesterin-abhangigen Transportmechanismus",
        answerC = "PTCH1 sequestiert den Hedgehog-Liganden im Extrazellulaarraum und verhindert Rezeptor-Bindung",
        answerD = "PTCH1 ubiquitiniert SMO und leitet seinen proteasomalen Abbau ein",
        correctAnswer = 1,
        explanation = "Im Grundzustand hemmt PTCH1 (12-Transmembranprotein) SMO durch einen nicht vollstaendig verstandenen Mechanismus, der Cholesterin-Transport involviert -- PTCH1 transportiert Cholesterin aus dem primaren Cilium, was SMO inaktiv haelt. Hedgehog-Bindung an PTCH1 hebt diese Hemmung auf und erlaubt SMO-Aktivierung.",
        difficulty = 4,
        funFact = "PTCH1-Mutationen verursachen das Gorlin-Syndrom (Basalzell-Naevus-Syndrom) mit multiplen Basalzellkarzinomen. Der SMO-Inhibitor Vismodegib (erstes klinisch zugelassenes Hedgehog-Inhibitor) wirkt bei diesen Patienten sehr effektiv."
    ),

    // Question 37 -- Epigenetics: Long Non-Coding RNAs
    Question(
        categoryId = 16,
        questionText = "Welche epigenetische Funktion uebernimmt die lange nicht-kodierende RNA XIST bei der X-Inaktivierung in weiblichen Saeugerzellen?",
        answerA = "XIST kodiert ein kleines Peptid, das Histondeacetylase-Komplexe rekrutiert",
        answerB = "XIST-RNA bedeckt den inaktiven X-Chromosom cis in cis, rekrutiert PRC2 und andere Repressorkomplexe und initiiert heterochromatische Kompaktierung (Barr-Koerperchen)",
        answerC = "XIST methyliert direkt CpG-Dinukleotide auf dem inaktiven X-Chromosom durch intrinsische DNMT-Aktivitaet",
        answerD = "XIST aktiviert den zufalligen Prozess der X-Wahl durch Bindung an CTCF-Anker-Regionen",
        correctAnswer = 1,
        explanation = "XIST (X-inactive specific transcript) ist ein ca. 17 kb langes lncRNA, das ausschliesslich vom inaktiven X transkribiert wird und in cis das gesamte X-Chromosom bedeckt. Es rekrutiert PRC2 (EZH2 setzt H3K27me3), HDAC3, DNA-Methyltransferasen und kondensiert das X-Chromosom zum Barr-Koerperchen.",
        difficulty = 4
    ),

    // Question 38 -- Gene Therapy: Base Editing
    Question(
        categoryId = 16,
        questionText = "Was ist der wesentliche Vorteil von Base Editing (z.B. Adenine Base Editor, ABE) gegenueber dem klassischen CRISPR-Cas9-Doppelstrangbruch-Ansatz?",
        answerA = "Base Editing erzeugt groessere Deletionen und ist daher effektiver fuer Gen-Knockouts",
        answerB = "Base Editing konvertiert eine Ziel-Base direkt (z.B. A-zu-G) ohne Doppelstrangbruch und minimiert so Indel-Entstehung und Off-target-Effekte durch NHEJ",
        answerC = "Base Editing hat keine PAM-Anforderung und kann jede Position im Genom praezise veraendern",
        answerD = "Base Editing kann mehrere Gene gleichzeitig editieren, da es keinen Leitfaden-RNA benoetigt",
        correctAnswer = 1,
        explanation = "Base Editors (CBE: C-zu-T; ABE: A-zu-G) nutzen ein katalytisch inaktives nCas9 (Nickase) fusioniert mit einer Desaminase. Ohne Doppelstrangbruch werden Punkt-Mutationen direkt konvertiert -- das vermeidet Indels durch fehleranfaelliges NHEJ und ermooglicht praezise Korrekturen von Pathogenen SNVs.",
        difficulty = 4,
        funFact = "Mit ABEs wurden bereits ex-vivo-Korrekturen von Sichelzell-Anaemie-Mutationen (HbS, E6V-Mutation) in haematopoetischen Stammzellen demonstriert -- mit Effizienz und Praezision, die klassisches CRISPR nicht erreicht."
    ),

    // Question 39 -- Proteomics: Protein Folding
    Question(
        categoryId = 16,
        questionText = "Welche Funktion uebernimmt das Hsp70-Chaperone-System bei der kotranslationalen Proteinfaltung?",
        answerA = "Hsp70 faltet Proteine in die native Konformation durch ATP-unabhaengige hydrophobe Interaktionen",
        answerB = "Hsp70 bindet exposierte hydrophobe Peptidabschnitte neosyntherisierter Polypeptide am Ribosom, verhindert Aggregation und gibt Substrate durch ATP-Hydrolyse fuer Faltungsversuche frei",
        answerC = "Hsp70 fungiert als molekulare Schleife, die missgefaltete Proteine direkt zum Proteasom transportiert",
        answerD = "Hsp70 ist ausschliesslich unter Hitzestress aktiv und hat keine Funktion bei normaler Proteinsynthese",
        correctAnswer = 1,
        explanation = "Hsp70 (mit Kofaktor Hsp40/DnaJ und Nucleotid-Austauschfaktor) bindet hydrophobe Segmente in neu synthetisierten Polypeptiden, bevor diese aggregieren. Im ADP-Zustand: hohe Affinitaet, Substrat gebunden. ATP-Bindung: niedrige Affinitaet, Substrat freigesetzt fuer Faltungsversuch. Mehrere Zyklen moeglich.",
        difficulty = 4
    ),

    // Question 40 -- Stem Cells: Asymmetric Division
    Question(
        categoryId = 16,
        questionText = "Was ist asymmetrische Zellteilung bei Stammzellen und wie wird sie reguliert?",
        answerA = "Asymmetrische Teilung produziert zwei Tochterzellen mit identischer Gensequenz, aber unterschiedlicher Groesse",
        answerB = "Eine Stammzelle teilt sich in eine Stammzelle (Selbsterneuerung) und eine Vorlaeufer-/Tochterzelle (Differenzierung) -- reguliert durch polare Verteilung von Zellschicksal-Determinanten (Numb, aPKC) und Spindel-Orientierung relativ zur Nische",
        answerC = "Asymmetrische Teilung wird ausschliesslich durch epigenetische Modifikationen bestimmt, nicht durch zellulaere Asymmetrie",
        answerD = "Asymmetrische Teilung produziert stets eine apoptotische und eine ueberlebende Tochterzelle",
        correctAnswer = 1,
        explanation = "Bei asymmetrischer Teilung werden Zellschicksal-Determinanten ungleich verteilt: Numb (Notch-Antagonist) wird polar angereichert und hemmt Notch-Signaling in einer Tochterzelle, die differenziert. Die andere Tochterzelle -- nah an der Nische -- erhaelt Notch-Aktivierung und bleibt Stammzelle. Die mitotische Spindel-Orientierung ist kritisch.",
        difficulty = 4,
        funFact = "Haematopoetische Stammzellen (HSCs) teilen sich selten asymmetrisch -- sie bevorzugen symmetrische Expansion bei Bedarf. Die Rate symmetrischer vs. asymmetrischer Teilungen kontrolliert die Stammzell-Pool-Groesse und damit Blutbildung."
    ),

    // Question 41 -- Metabolomics: TCA Cycle Oncometabolites
    Question(
        categoryId = 16,
        questionText = "Wie fuehren Gain-of-function-Mutationen in IDH1/IDH2 (Isocitrat-Dehydrogenase) zur Kanzerogenese?",
        answerA = "Mutierte IDH1/2 verliert die normale Funktion und akkumuliert alpha-Ketoglutarat, das Zellproliferation stimuliert",
        answerB = "Mutierte IDH1/2 produziert das Onkometabolit 2-Hydroxyglutarat (2-HG), das alpha-Ketoglutarat-abhaengige Dioxygenasen (TET-Enzyme, JmjC-HDMs) hemmt und so epigenetische Reprogrammierung und Differenzierungsblockade verursacht",
        answerC = "Mutierte IDH1/2 erzeugt ueberschuessiges NADPH, das antioxidative Kapazitaet tumoepidermaler Zellen massiv erhoht",
        answerD = "Mutierte IDH1/2 blockiert den Zitratzyklus und erzwingt aerobe Glykolyse (Warburg-Effekt) durch NAD+-Mangel",
        correctAnswer = 1,
        explanation = "IDH1/2-Mutationen (R132H, R172K) produzieren anstelle von alpha-Ketoglutarat das Enantiomer (R)-2-Hydroxyglutarat. 2-HG hemmt kompetitiv TET2 (DNA-Demethylase) und KDM-Histondemethylasen, die alpha-KG als Kofaktor benoetigen. Die Folge: Hypermethylierung (CIMP) und Differenzierungsblockade.",
        difficulty = 4,
        funFact = "IDH1/2-Mutationen kommen in ca. 80% der niedriggradigen Gliome vor und gehen mit besserer Prognose einher. Der IDH1-Inhibitor Ivosidenib und IDH2-Inhibitor Enasidenib sind seit 2017/2018 fuer AML zugelassen."
    ),

    // Question 42 -- Apoptosis: IAP Proteins
    Question(
        categoryId = 16,
        questionText = "Wie hemmt XIAP (X-linked Inhibitor of Apoptosis) die Caspase-Aktivierung und wie wird diese Hemmung aufgehoben?",
        answerA = "XIAP methyliert Caspase-3 und -7 und inaktiviert sie dauerhaft; Smac/DIABLO kann diese Methylierung nicht rueckgaengig machen",
        answerB = "XIAP bindet direkt an Caspase-9 (BIR3-Domaene) und Caspase-3/-7 (BIR2-Domaene) und hemmt ihre Aktivitaet; Smac/DIABLO und HtrA2/Omi binden konkurrenziell an BIR-Domaenen und verdraengen Caspasen",
        answerC = "XIAP phosphoryliert und inaktiviert Cytochrom c im Zytoplasma bevor es das Apoptosom erreicht",
        answerD = "XIAP ist ein Pseudosubstrat fuer Caspase-8 und verzoegert die DISC-Bildung an Todesrezeptoren",
        correctAnswer = 1,
        explanation = "XIAP besitzt drei BIR-Domaenen (Baculoviral IAP Repeat): BIR3 bindet und hemmt Pro-/aktive Caspase-9; BIR2 (mit flankierendem Linker) hemmt Caspase-3 und -7. Smac/DIABLO und HtrA2, die bei Apoptose aus Mitochondrien freigesetzt werden, binden mit ihrem IBM (IAP Binding Motif) an BIR2/3 und verdraengen Caspasen.",
        difficulty = 4
    ),

    // Question 43 -- DNA Repair: Double Strand Breaks
    Question(
        categoryId = 16,
        questionText = "Welche zwei Hauptwege reparieren Doppelstrangbrueche (DSBs) in saeugetierischen Zellen und in welchen Zellzyklusphasen dominiert jeder?",
        answerA = "Homologe Rekombination (HR) in G1; Nicht-homologes Endjoining (NHEJ) in S/G2",
        answerB = "NHEJ (fehleranfaellig, ligiert Enden direkt) dominiert in G1/G0; HR (fehlerfreier, nutzt Schwesterchromatid als Matrize) dominiert in S/G2",
        answerC = "HR in G0; Microhomology-mediated end joining (MMEJ) in S und G2",
        answerD = "Beide Wege sind zellzyklus-unabhaengig und werden durch die Bruchmorphologie bestimmt",
        correctAnswer = 1,
        explanation = "NHEJ ligiert Enden direkt (Ku70/80, DNA-PKcs, Ligase IV/XRCC4) und ist schnell aber fehleranfaellig -- es dominiert in G1, wenn keine Schwesterchromatide vorhanden sind. HR nutzt die identische Schwesterchromatid-Sequenz als Reparaturmatrize (RAD51, BRCA1/2) -- sie ist praezise und dominiert in S/G2 nach DNA-Replikation.",
        difficulty = 4
    ),

    // Question 44 -- Epigenetics: Imprinting
    Question(
        categoryId = 16,
        questionText = "Was ist genomisches Imprinting und welche Erkrankungen entstehen durch Imprinting-Defekte am Chromosom 15?",
        answerA = "Imprinting ist die expression-unabhaengige DNA-Kondensation; Defekte an Chr. 15 fuehren zu Cri-du-chat-Syndrom",
        answerB = "Genomisches Imprinting bezeichnet die elternspezifische, epigenetische Markierung (Methylierung von ICRs) die ein Allel mono-allel exprimiert; Defekte an Chr. 15q11-q13 fuehren je nach elterlichem Ursprung zu Prader-Willi-Syndrom (paternaler Deletion/Fehler) oder Angelman-Syndrom (maternaler Deletion/Fehler)",
        answerC = "Imprinting ist die stochastische Allel-Inaktivierung; Chr.-15-Defekte verursachen DiGeorge-Syndrom",
        answerD = "Genomisches Imprinting bezeichnet die Transposons-Silencing; Chr.-15-Mutationen fuehren zu Beckwith-Wiedemann-Syndrom",
        correctAnswer = 1,
        explanation = "Imprinting-Kontrollregionen (ICRs) sind differenziell methyliert je nach elterlichem Ursprung. In 15q11-q13 sind paternale Gene (SNRPN, NDN) exprimiert, maternale Gene (UBE3A) aktiv. Deletion/UPD der paternalen Region: Prader-Willi (hypotone, Adipositas, geistige Behinderung). Deletion/UPD der maternalen Region: Angelman (Anfalle, Ataxie, fehlende Sprache).",
        difficulty = 4,
        funFact = "Angelman-Syndrom-Kinder zeigen haeufig ein froehliches Temperament und Lachanfaelle -- Harry Angelman, der das Syndrom 1965 beschrieb, nannte sie in seiner Publikation zunachst 'Puppet Children' nach einem Goya-Gemaelde."
    ),

    // Question 45 -- Cell Signaling: JAK-STAT Pathway
    Question(
        categoryId = 16,
        questionText = "Wie aktiviert ein Zytokin-Rezeptor ohne intrinsische Kinaseaktivitaet (z.B. IFN-gamma-Rezeptor) den JAK-STAT-Signalweg?",
        answerA = "Das Zytokin bindet an den Rezeptor und aktiviert eine assoziierte Adenylylzyklase, cAMP aktiviert JAK-Kinasen",
        answerB = "Die Zytokin-Bindung induziert Rezeptor-Dimerisierung, was die konstitutiv assoziierten JAK-Kinasen (JAK1, JAK2, TYK2) in Transsphosphorylierung aktiviert; aktive JAKs phosphorylieren Rezeptor-Tyrosine als Andockstellen fuer STAT-Proteine",
        answerC = "Zytokin-Rezeptoren rekrutieren direkt RAS-GEFs und aktivieren MAPK-Kaskade unabhaengig von JAK",
        answerD = "JAK-Kinasen sind im Ruhezustand im Zellkern und werden durch Zytokin-Stimulation in das Zytoplasma exportiert",
        correctAnswer = 1,
        explanation = "Jede Rezeptor-Untereinheit ist konstitutiv mit einer JAK-Kinase assoziiert. Liganden-induzierte Rezeptor-Dimerisierung bringt die JAKs in Nachbarschaft, ermoeglicht Transsphosphorylierung und Aktivierung. Aktive JAKs phosphorylieren Tyrosine im Rezeptor-Zytoplasmabereich, woraufhin STAT-Proteine binden (SH2-Domaene), phosphoryliert werden, dimerisieren und in den Kern translozieren.",
        difficulty = 4,
        funFact = "JAK-Inhibitoren ('Jakinibs') wie Tofacitinib oder Baricitinib sind oral verabreichbare Immunsuppressiva fuer rheumatoide Arthritis -- und Baricitinib zeigte auch Wirksamkeit bei COVID-19."
    ),

    // Question 46 -- Proteomics: Ubiquitin-Proteasome System
    Question(
        categoryId = 16,
        questionText = "Welche drei Enzymklassen (E1, E2, E3) sind an der Ubiquitin-Konjugation beteiligt und wo liegt die Substratspezifitaet?",
        answerA = "E1: Ubiquitin-Aktivierung (ATP-abhaengig, ~2 E1s); E2: Ubiquitin-Konjugation (ca. 40 E2s); E3: Ubiquitin-Ligase uebertraegt auf Substrat-Lysin (>600 E3s) -- die Substratspezifitaet liegt in der E3-Ligase",
        answerB = "E1: Substrat-Erkennung; E2: Ubiquitin-Aktivierung; E3: Ubiquitin-Aktivierung durch ATP-Hydrolyse",
        answerC = "E1, E2 und E3 haben redundante Substrat-Spezifitaet; die Selektivitaet entsteht erst am Proteasom",
        answerD = "E1 erkennt das Substrat; E2 und E3 bilden den 26S-Proteasom-Komplex",
        correctAnswer = 0,
        explanation = "Die Ubiquitin-Kaskade: E1 (UBA1/UBA6) aktiviert Ubiquitin durch Thioester-Bildung (ATP-abhaengig). E2 uebernimmt Ubiquitin. E3-Ligasen (RING, HECT, RBR-Typen) erkennen spezifische Substrat-Signale (Degrons, Phosphorylierungen) und uebertragen Ubiquitin auf Substrat-Lysine. Die >600 E3s definieren die Substrate.",
        difficulty = 4
    ),

    // Question 47 -- Stem Cells: CAR-T Therapy Connection
    Question(
        categoryId = 16,
        questionText = "Bei der CAR-T-Zell-Therapie: Welche Eigenschaft von T-Zellen macht sie zu einem geeigneten zellularen Vehikel und welche stammzell-aehnliche Eigenschaft haben Memory-T-Zellen?",
        answerA = "T-Zellen sind pluripotente Stammzellen; Memory-T-Zellen sind identisch mit iPSCs",
        answerB = "T-Zellen sind ausdifferenzierte, aber langlebige Zellen; Naive und Memory-T-Zellen besitzen stammzell-aehnliche Selbsterneuerungs-Kapazitaet (Tscm -- Stem-cell Memory T cells) mit langer Persistenz und Expansionspotenzial",
        answerC = "T-Zellen sind unsterbliche Zellinien; CAR-T-Zellen replizieren unbegrenzt wie embryonale Stammzellen",
        answerD = "T-Zellen haben keine Selbsterneuerungskapazitaet; ihre Langlebigkeit beruht auf antiapoptotischen Bcl-2-Hochregulation",
        correctAnswer = 1,
        explanation = "Tscm-Zellen (Stem cell Memory T cells) exprimieren stammzell-assoziierte Transkriptionsfaktoren (TCF7, BCL6) und haben hohe Selbsterneuerungs- und Differenzierungskapazitaet. Sie persistieren jahrzehntelang (HIV-spezifische Memory-T-Zellen nach 50+ Jahren nachweisbar). CAR-T-Therapien zielen darauf ab, moeglichst Tscm/Tcm-Populationen zu generieren fuer langfristige Tumorkontrolle.",
        difficulty = 4,
        funFact = "Die ersten FDA-zugelassenen CAR-T-Therapien (Tisagenlecleucel, 2017) kosteten einmalig ca. 475.000 US-Dollar -- aber bei chemotherapierefraktaerer ALL sind Remissionsraten von ueber 80% erzielbar."
    ),

    // Question 48 -- Microbiome: Tryptophan Metabolism
    Question(
        categoryId = 16,
        questionText = "Welche drei Hauptrouten des Tryptophan-Metabolismus hat das Darmmikrobiom und welche neuronale Bedeutung haben deren Produkte?",
        answerA = "Alle Tryptophanrouten fuehren letztlich zu Serotonin; Mikrobiom hat keinen Einfluss auf Tryptophan-Metabolismus",
        answerB = "Serotonin-Route (periphere Motilitaet/Sekretion); Kynurenin-Route (Immunmodulation, Neurotoxizitaet durch Quinolinsaeure vs. Neuroprotektivitaet durch Kynurensaeure); Indol-Route (AhR-Liganden, Darmbarriere-Schutz und ZNS-Modulation)",
        answerC = "Tryptophan wird ausschliesslich zu Melatonin und Serotonin metabolisiert; keine toxischen Metabolite entstehen",
        answerD = "Kynurenin-Route ist ausschliesslich hepatisch; Mikrobiom produziert nur Indol-Derivate",
        correctAnswer = 1,
        explanation = "Im Darm: Enterochromaffine Zellen und Mikrobiom produzieren Serotonin (5-HT, Motilitaet). Die Kynurenin-Route (IDO/TDO) produziert Kynurenin, das zu neurotoxischer Quinolinsaeure oder neuroprotektiver Kynurensaeure weiterverarbeitet wird. Indol-Derivate (IAld, I3A) aktivieren Aryl-Hydrocarbon-Rezeptor (AhR) und staerken die Darmbarriere und modulieren Mikroglia.",
        difficulty = 4
    ),

    // Question 49 -- Gene Therapy: RNA Interference
    Question(
        categoryId = 16,
        questionText = "Wie unterscheiden sich siRNA und miRNA mechanistisch in ihrer regulatorischen Wirkung auf Ziel-mRNAs?",
        answerA = "siRNA und miRNA sind funktionell identisch -- der Unterschied liegt nur in ihrer genomischen Herkunft",
        answerB = "siRNA ist vollstaendig komplementaer zur Ziel-mRNA und induziert endonukleolytische Spaltung durch RISC/AGO2; miRNA ist partiell komplementaer (v.a. 'seed region') und induziert hauptsaechlich Translationshemmung und mRNA-Deadenylierung/-Destabilisierung",
        answerC = "siRNA hemmt nur die Translation; miRNA loest mRNA-Spaltung aus",
        answerD = "siRNA wirkt im Zellkern auf DNA-Ebene; miRNA wirkt im Zytoplasma ausschliesslich am Ribosom",
        correctAnswer = 1,
        explanation = "siRNA: vollkommene Komplementaritaet zur Ziel-mRNA --> AGO2-vermittelte Endonuklease-Spaltung (Slicing). miRNA: partielle Komplementaritaet, Erkennung ueber 'seed region' (nt 2-8) --> Translationsrepression und CCR4-NOT-vermittelte Deadenylierung und mRNA-Abbau. Beide werden im RISC-Komplex geladen.",
        difficulty = 4,
        funFact = "Das erste zugelassene siRNA-Medikament, Patisiran (2018), behandelt hereditaere Transthyretin-Amyloidose -- es wird in Lipidnanopartikeln verpackt und stillegt die Transthyretin-mRNA in der Leber mit ueber 80% Effizienz."
    ),

    // Question 50 -- Mitochondrial Diseases: mtDNA Heteroplasmy Therapy
    Question(
        categoryId = 16,
        questionText = "Welche experimentelle Strategie zur Behandlung mitochondrialer Erkrankungen zielt darauf ab, das Heteroplasmie-Verhaeltnis gezielt zu verschieben?",
        answerA = "Mitochondriale Gentherapie durch AAV-Vektoren, die direkt in die mtDNA integrieren",
        answerB = "Mitochondrien-zielende Nukleasen (mitoZFN, mitoTALEN, mito-CRISPR-frei) spalten selektiv mutierte mtDNA-Kopien, was gesunde Wildtyp-mtDNA durch Replikation expandiert und das Heteroplasmie-Niveau normalisiert",
        answerC = "Mitochondriale Transplantation aus gesunden Spenderzellen durch direkte Injektion in betroffene Muskeln",
        answerD = "Pharmakologische Aktivierung von PGC-1alpha foerdert ausschliesslich Wildtyp-mtDNA-Replikation durch Sequenz-spezifische Bindung",
        correctAnswer = 1,
        explanation = "Da mtDNA ausserordentlich kompakt ist und kein NHEJ besitzt, fuhren Doppelstrangbrueche zur Abbau der Ziel-mtDNA. Mitochondrien-zielende Nukleasen (ZFN, TALEN -- durch mitochondriale Targeting-Sequenz dirigiert) spalten mutierte mtDNA spezifisch; die resultierende Reduktion laesst Wildtyp-mtDNA durch kompensierende Replikation expandieren.",
        difficulty = 4,
        funFact = "Mitochondriale Spende ('Drei-Eltern-Baby') ist eine praventive Alternative: Der Kern der Mutterzelle wird in eine kernlose Spender-Eizelle mit gesunden Mitochondrien transplantiert. Das erste Kind durch diese Technik wurde 2016 geboren."
    )

)
