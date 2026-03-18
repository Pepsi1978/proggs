package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun healthQuestionsMaster1(): List<Question> = listOf(

    // --- T-ZELL-REZEPTOR-SIGNALTRANSDUKTION ---

    // Question 1 - ZAP-70 und TCR-Aktivierung
    Question(
        categoryId = 16,
        questionText = "Welche Kinase phosphoryliert nach TCR-Aktivierung die ITAMs der CD3-Zeta-Kette, bevor ZAP-70 rekrutiert werden kann?",
        answerA = "ZAP-70 selbst (Autophosphorylierung)",
        answerB = "Lck (Lymphocyte-specific protein tyrosine kinase)",
        answerC = "Fyn",
        answerD = "PI3-Kinase",
        correctAnswer = 1,
        explanation = "Lck, eine Src-Familie-Kinase, phosphoryliert die ITAM-Tyrosinreste der CD3-Zeta-Kette nach TCR-Engagement. Die doppelt phosphorylierten ITAMs dienen dann als Andockstellen fuer die SH2-Domaenen von ZAP-70, das anschliessend selbst durch Lck aktiviert wird.",
        difficulty = 5,
        funFact = "Mutationen in Lck fuehren beim Menschen zu einem kombinierten Immundefekt -- Patienten fehlen funktionelle T-Zellen obwohl Thymozyten vorhanden sind, da die T-Zell-Selektion im Thymus von Lck abhaengig ist."
    ),

    // Question 2 - NFAT-Aktivierungsweg
    Question(
        categoryId = 16,
        questionText = "Welcher zweite Messenger aktiviert Calcineurin nach TCR-Stimulation, um NFAT zu dephosphorylieren und in den Kern zu translozieren?",
        answerA = "cAMP (cyclisches Adenosinmonophosphat)",
        answerB = "IP3-vermittelte Ca2+-Freisetzung aus dem ER",
        answerC = "DAG (Diacylglycerol)",
        answerD = "cGMP (cyclisches Guanosinmonophosphat)",
        correctAnswer = 1,
        explanation = "PLC-gamma, aktiviert durch ZAP-70 via LAT, spaltet PIP2 in IP3 und DAG. IP3 bindet an IP3-Rezeptoren des endoplasmatischen Retikulums und loest Ca2+-Freisetzung aus. Der Ca2+-Anstieg aktiviert Calmodulin, das seinerseits die Phosphatase Calcineurin aktiviert -- dieser dephosphoryliert NFAT und ermoeglicht dessen Kernimport.",
        difficulty = 5,
        funFact = "Ciclosporin A und Tacrolimus (FK506) hemmen Calcineurin gezielt und supprimieren dadurch T-Zell-Aktivierung -- beides sind Standardimmunsuppressiva nach Organtransplantationen, die genau diesen Signalweg blockieren."
    ),

    // Question 3 - LAT als Signalplattform
    Question(
        categoryId = 16,
        questionText = "Welche Funktion hat LAT (Linker for Activation of T cells) in der TCR-Signalkaskade?",
        answerA = "LAT ist eine transmembranose Phosphatase, die Signale terminiert",
        answerB = "LAT ist ein Geruest-Adaptorprotein, das nach ZAP-70-Phosphorylierung PLC-gamma, Grb2 und SLP-76 rekrutiert",
        answerC = "LAT aktiviert direkt NF-kappaB durch Ubiquitinierung von IkappaB",
        answerD = "LAT vermittelt CD28-Kostimulationssignale unabhaengig vom TCR",
        correctAnswer = 1,
        explanation = "ZAP-70 phosphoryliert LAT an mehreren Tyrosinresten. Das phosphorylierte LAT dient als molekulare Plattform und rekrutiert ueber SH2-Domaenen gleichzeitig PLC-gamma1, Grb2/SOS und SLP-76 -- damit verzweigt sich das Signal in den Ca2+/NFAT-Pfad, den Ras/MAPK-Pfad und den PKC-theta/NF-kappaB-Pfad.",
        difficulty = 5,
        funFact = "LAT-defiziente Maeuse zeigen einen vollstaendigen Block der T-Zell-Entwicklung im Thymus auf der Doppelt-Negativ-Stufe -- ein Beweis, dass LAT auch fuer die Praeselektions-Signale des praeT-Zell-Rezeptors essentiell ist."
    ),

    // Question 4 - Immunologische Synapse
    Question(
        categoryId = 16,
        questionText = "Wie heisst die zentrale supramolekulare Aktivierungsstruktur (cSMAC) in der immunologischen Synapse, und welche Hauptfunktion hat sie?",
        answerA = "cSMAC enthaelt hauptsaechlich TCR-MHC-Komplexe und ist der primaere Ort der T-Zell-Aktivierung",
        answerB = "cSMAC akkumuliert TCR und CD28, wird aber vorwiegend fuer TCR-Internalisierung und Signalterminierung genutzt",
        answerC = "cSMAC ist ausschliesslich eine Struktur zur Perforin-Sekretion bei zytotoxischen T-Zellen",
        answerD = "cSMAC enthaelt LFA-1 und ICAM-1 und vermittelt mechanische Adhaesion",
        correctAnswer = 1,
        explanation = "Die immunologische Synapse gliedert sich in cSMAC (zentral: TCR, CD28, PKC-theta), pSMAC (peripher: LFA-1/ICAM-1-Adhaesionsring) und dSMAC (distal: CD43, CD45). Aktuell gilt der cSMAC weniger als Aktivierungsort, sondern als Ort der TCR-Downregulation via multivesikulaere Koerper -- die eigentliche Signaltransduktion findet in perisynaptischen Mikroclustern statt.",
        difficulty = 5
    ),

    // --- B-ZELL-REIFUNG UND KEIMZENTRUM ---

    // Question 5 - Somatische Hypermutation
    Question(
        categoryId = 16,
        questionText = "Welches Enzym initiiert somatische Hypermutation in B-Zellen des Keimzentrums, indem es Cytosin-Desaminierung in der Immunglobulin-V-Region katalysiert?",
        answerA = "RAG1/RAG2 (Rekombinase-aktivierende Gene)",
        answerB = "AID (Activation-Induced Cytidine Deaminase)",
        answerC = "TdT (terminale Desoxynukleotidyltransferase)",
        answerD = "DNA-Polymerase eta",
        correctAnswer = 1,
        explanation = "AID (AICDA-Gen) deaminiert Cytosin zu Uracil in einzelstraengiger DNA der Immunglobulin-V-Regionen. Diese U-G-Fehlpaarung wird dann fehlerhaft repariert, was Punktmutationen in der CDR-Region erzeugt. B-Zellen mit verbesserter Antigenaffinitaet werden positiv selektiert (Affinitaetsreifung), solche mit verminderter Affinitaet sterben durch Apoptose.",
        difficulty = 5,
        funFact = "AID-Defekte verursachen beim Menschen das Hyper-IgM-Syndrom Typ 2 -- Patienten haben hohe IgM-Spiegel aber kein IgG, IgA oder IgE, da weder Klassenwechsel noch Affinitaetsreifung stattfinden koennen."
    ),

    // Question 6 - Keimzentrum-Reaktion: Dunkel- vs. Hellzone
    Question(
        categoryId = 16,
        questionText = "Welcher Prozess findet in der Dunkelzone des Keimzentrums statt, und welcher Transkriptionsfaktor reguliert diesen Kompartiment-spezifisch?",
        answerA = "Proliferation und somatische Hypermutation; CXCR4 wird durch BCL6 hochreguliert",
        answerB = "Selektion durch follikulaere T-Helfer-Zellen; IRF4 ist der Masterregulator",
        answerC = "Klassenswitch-Rekombination; AID wird nur in der Dunkelzone exprimiert",
        answerD = "Differenzierung zu Plasmazellen; BLIMP1 wird durch BCL6 induziert",
        correctAnswer = 0,
        explanation = "In der Dunkelzone (CXCR4-hoch, CXCL12-reich) proliferieren Zentroblasten und akkumulieren somatische Mutationen durch AID-Aktivitaet. BCL6 ist der Masterregulator der Keimzentrum-Reaktion und wird in der Dunkelzone besonders hoch exprimiert; er reprimiert unter anderem BLIMP1 und IRF4, um die Plasmazell-Differenzierung zu verhindern. In der Hellzone treffen Zentrozyten auf follikulaere dendritische Zellen und T-Helfer-Zellen zur Selektion.",
        difficulty = 5,
        funFact = "BCL6-Translokationen (t(3;14) etc.) finden sich in ca. 30-40% aller diffusen grosszelligen B-Zell-Lymphome (DLBCL) -- das Keimzentrum ist anatomisch der Ursprungsort vieler B-Zell-Malignome."
    ),

    // Question 7 - Klassenwechsel-Rekombination
    Question(
        categoryId = 16,
        questionText = "Welcher molekulare Mechanismus ermoeglicht den Ig-Klassenwechsel von IgM zu IgG, und welche DNA-Struktur entsteht dabei als Nebenprodukt?",
        answerA = "Inversion des CH-Gens; kein Nebenprodukt entsteht",
        answerB = "AID-vermittelte intrachromosomale Deletionsrekombination zwischen Switch-Regionen; Switch-Excisions-Kreise (SECs) entstehen",
        answerC = "Transposition des CH-Gens auf ein anderes Chromosom; Switch-Plasmide entstehen",
        answerD = "Alternatives Spleissen der primaeren mRNA; keine genomische Rekombination noetig",
        correctAnswer = 1,
        explanation = "AID deaminiert DNA in den repetitiven Switch-(S)-Regionen vor den CH-Genen. Die resultierenden DNA-Doppelstrangbrueche werden durch NHEJ verbunden: Zwischen der genutzten S-Region und einer downstream-S-Region entsteht eine Deletion, das herausgeschnittene zirkulaere DNA-Fragment bildet einen Switch-Excisions-Kreis (SEC). SECs koennen als Biomarker fuer aktive Keimzentrum-Reaktion nachgewiesen werden.",
        difficulty = 5
    ),

    // Question 8 - Follikulaere T-Helfer-Zellen (Tfh)
    Question(
        categoryId = 16,
        questionText = "Welches Oberflaechenprotein und welcher Transkriptionsfaktor definieren follikulaere T-Helfer-Zellen (Tfh) als eigenstaendige Subpopulation?",
        answerA = "CXCR5 und BCL6",
        answerB = "CCR7 und T-bet",
        answerC = "CXCR3 und GATA-3",
        answerD = "CCR4 und ROR-gammat",
        correctAnswer = 0,
        explanation = "Tfh-Zellen exprimieren charakteristisch den Chemokinrezeptor CXCR5, der sie durch Bindung an CXCL13 in B-Zell-Follikel und Keimzentren lockt. BCL6 ist der Mastertranskriptionsfaktor der Tfh-Differenzierung; er reprimiert andere T-Helfer-Schicksale (T-bet, GATA-3, ROR-gammat) und ermoeglicht die Langzeit-Interaktion mit B-Zellen ueber ICOS-ICOSL und CD40L-CD40.",
        difficulty = 5,
        funFact = "Dysfunktionale Tfh-Zellen sind bei systemischem Lupus erythematodes (SLE) mit unkontrollierter B-Zell-Aktivierung und Autoantikoeperproduktion assoziiert -- Tfh-Hemmung ist daher ein Therapieansatz in der SLE-Forschung."
    ),

    // --- ANGEBORENE IMMUNITAET: INFLAMMASOME ---

    // Question 9 - NLRP3-Inflammasom
    Question(
        categoryId = 16,
        questionText = "Welche zwei Signale sind fuer die volle NLRP3-Inflammasom-Aktivierung erforderlich (Zwei-Signal-Modell)?",
        answerA = "Signal 1: TLR-Priming (NF-kappaB-Aktivierung, NLRP3-Transkription); Signal 2: Gefahr-assoziierte Stimuli (ATP, Kristalle, ROS) fuer Assemblierung und Caspase-1-Aktivierung",
        answerB = "Signal 1: Interleukin-1-beta-Bindung; Signal 2: TNF-alpha-Bindung",
        answerC = "Signal 1: Virale RNA; Signal 2: bakterielles Lipopolysaccharid",
        answerD = "Signal 1: Ca2+-Einstrom; Signal 2: Mitochondriale Depolarisation",
        correctAnswer = 0,
        explanation = "Das Zwei-Signal-Modell des NLRP3-Inflammasoms: Signal 1 (Priming) aktiviert TLRs oder Zytokinrezeptoren, was via NF-kappaB die Transkription von NLRP3 und Pro-IL-1beta hochreguliert. Signal 2 (Aktivierung) durch DAMPs (ATP, Harnsaeurekristalle, Cholesterinkristalle, ROS, Kaliumausstrom) triggert die NLRP3-Oligomerisierung, ASC-Rekrutierung und Caspase-1-Spaltung, die schliesslich Pro-IL-1beta und Pro-IL-18 zu den reifen Zytokinen prozessiert.",
        difficulty = 5,
        funFact = "Gicht ist ein direktes Beispiel fuer NLRP3-Inflammasom-Pathologie: Harnsaeurekristalle aktivieren Makrophagen-NLRP3, was zur massiven IL-1beta-Freisetzung und akuten Gelenkentzuendung fuehrt. Canakinumab (Anti-IL-1beta) ist daher bei therapierefraktaerer Gicht zugelassen."
    ),

    // Question 10 - Pyroptose
    Question(
        categoryId = 16,
        questionText = "Welches Protein vermittelt Pyroptose nach Inflammasom-Aktivierung, indem es Poren in der Zellmembran bildet?",
        answerA = "Perforin",
        answerB = "Gasdermin D (GSDMD)",
        answerC = "Bax/Bak (apoptotische Poren)",
        answerD = "MLKL (Mixed lineage kinase domain-like)",
        correctAnswer = 1,
        explanation = "Aktive Caspase-1 (und Caspase-4/5/11) spaltet Gasdermin D zwischen der N-terminalen Effektordomaene und dem C-terminalen Autorepressor. Die freigesetzte N-terminale Gasdermin-D-Domaene oligomerisiert und bildet Poren (~18 nm Durchmesser) in der Plasmamembran. Diese Poren ermoglichen IL-1beta-Sekretion und fuehren letztendlich zur osmotischen Zellschwellung und Pyroptose -- einer lytischen, proinflammatischen Zelltodform.",
        difficulty = 5,
        funFact = "MLKL vermittelt Nekroptose (nicht Pyroptose) und wird durch RIPK3 aktiviert -- ein haeufiger Verwechslungsfehler in Pruefungen. Beide Tode sind proinflammatisch und lytisch, aber mechanistisch vollstaendig verschieden."
    ),

    // Question 11 - cGAS-STING-Signalweg
    Question(
        categoryId = 16,
        questionText = "Welchen second messenger produziert cGAS (cyclic GMP-AMP synthase) nach Bindung zytoplasmatischer doppelstraengiger DNA, und welchen Rezeptor aktiviert er?",
        answerA = "cAMP; aktiviert PKA",
        answerB = "2'3'-cGAMP (cyclisches GMP-AMP); aktiviert STING (Stimulator of Interferon Genes)",
        answerC = "IP3; aktiviert den ER-Calciumkanal",
        answerD = "cdi-GMP; aktiviert IRF3 direkt",
        correctAnswer = 1,
        explanation = "cGAS erkennt zytoplasmatische dsDNA (viral, bakteriell oder aus dem Zellkern entkommene Selbst-DNA) und synthetisiert das nicht-kanonische zyklische Dinukleotid 2'3'-cGAMP (mit 2'-5'- und 3'-5'-Phosphodiesterbindungen). cGAMP bindet STING am ER, loest dessen Translokation zum Golgi-Apparat aus und aktiviert TBK1/IRF3, was in der Produktion von Typ-I-Interferonen resultiert.",
        difficulty = 5,
        funFact = "Aicardi-Goutieres-Syndrom, eine schwere Enzephalopathie mit Interferon-Signatur, wird teilweise durch Mutationen in DNAsen verursacht (TREX1, RNaseH2), die zytoplasmatische Selbst-DNA normalerweise abbauen -- Fehlen diese DNAsen, aktiviert Selbst-DNA cGAS-STING dauerhaft."
    ),

    // Question 12 - Toll-like-Rezeptor-Signalling: TRIF vs. MyD88
    Question(
        categoryId = 16,
        questionText = "Welcher TLR nutzt ausschliesslich den TRIF-Signalweg (ohne MyD88) und induziert dadurch primar Typ-I-Interferon statt IL-12/TNF?",
        answerA = "TLR4 (LPS)",
        answerB = "TLR3 (doppelstraengige RNA)",
        answerC = "TLR9 (CpG-DNA)",
        answerD = "TLR2 (Lipoproteine)",
        correctAnswer = 1,
        explanation = "TLR3, lokalisiert in endolysosomalen Membranen, erkennt virale dsRNA und signalisiert ausschliesslich ueber TRIF (TICAM-1). TRIF aktiviert TBK1/IKKepsilon/IRF3, was zur Typ-I-Interferon-Produktion fuehrt. Im Gegensatz dazu nutzen TLR4 beide Wege (MyD88 fuer NF-kappaB/IL-12/TNF, TRIF fuer IFN), waehrend TLR2 und TLR9 nur MyD88 verwenden.",
        difficulty = 5
    ),

    // --- TRANSPLANTATIONSIMMUNOLOGIE ---

    // Question 13 - HLA-Matching: Bedeutung der Allele
    Question(
        categoryId = 16,
        questionText = "Welches HLA-Locus-Mismatch ist bei Nierentransplantationen am kritischsten fuer das Langzeituberleben des Transplantats?",
        answerA = "HLA-A-Mismatch",
        answerB = "HLA-DR-Mismatch",
        answerC = "HLA-Cw-Mismatch",
        answerD = "HLA-DP-Mismatch",
        correctAnswer = 1,
        explanation = "HLA-DR (Klasse II) ist der kritischste Locus fuer das Transplantat-Langzeituberleben. HLA-DR-Mismatches korrelieren am staerksten mit der Rate an akuten Abstossungsreaktionen und dem chronischen Transplantatversagen. Klinisch wird in der Nierentransplantation ein 000-Mismatch (0 Mismatches in HLA-A, -B und -DR) angestrebt, wobei das DR-Matching priorisiert wird.",
        difficulty = 5,
        funFact = "Obwohl HLA-DR am wichtigsten ist, entscheiden heute in der Praxis oft logistische Faktoren (kalte Ischaemiezeit, geografische Naehe) mehr ueber das Transplantat-Schicksal als perfektes HLA-Matching."
    ),

    // Question 14 - Crossmatch-Test
    Question(
        categoryId = 16,
        questionText = "Was bedeutet ein positiver komplementabhaengiger Zytotoxizitaets-Crossmatch (CDC-Crossmatch) vor einer Nierentransplantation?",
        answerA = "Der Empfaenger hat praeformierte IgG-Antikoerper gegen HLA-Antigene des Spenders -- absolute Kontraindikation",
        answerB = "Der Spender hat Antikoerper gegen den Empfaenger -- keine klinische Relevanz",
        answerC = "Der Empfaenger hat eine aktive Infektion und ist nicht transplantierbar",
        answerD = "Die HLA-Typisierung war fehlerhaft und muss wiederholt werden",
        correctAnswer = 0,
        explanation = "Im CDC-Crossmatch werden Spenderlymphozyten mit Empfaengerserum inkubiert. Ein positives Ergebnis (Zytotoxizitaet durch Komplementaktivierung) zeigt, dass der Empfaenger praeformierte Antikoerper gegen spenderspezifische HLA-Antigene (DSA) besitzt. Dies fuehrt zur hyperakuten Abstossungsreaktion innerhalb von Minuten bis Stunden nach Transplantation und ist eine absolute Kontraindikation -- ausser nach Desensibilisierungsprotokollen.",
        difficulty = 5
    ),

    // Question 15 - Gemischte Lymphozytenreaktion
    Question(
        categoryId = 16,
        questionText = "Was misst die gemischte Lymphozytenreaktion (MLR), und warum wurde sie klinisch durch molekulare Typisierungsmethoden weitgehend ersetzt?",
        answerA = "MLR misst NK-Zell-Zytotoxizitaet; ersetzt wegen fehlender Reproduzierbarkeit",
        answerB = "MLR misst alloreaktive T-Zell-Proliferation gegen Spender-HLA; ersetzt wegen langer Testdauer (5-7 Tage) bei Kadaverorganen",
        answerC = "MLR misst Antikoerpertiter gegen HLA; ersetzt durch Luminex-Technologie",
        answerD = "MLR misst Komplement-Aktivierung; ersetzt durch Flowzytometrie",
        correctAnswer = 1,
        explanation = "Die einwegige MLR inkubiert bestrahlte (nicht-proliferierende) Stimulatorzellen des Spenders mit Responderzellen des Empfaengers und misst die T-Zell-Proliferation (3H-Thymidin oder CFSE) als Mass fuer HLA-Klasse-II-Inkompatibilitaet. Da die MLR 5-7 Tage benoetigt, ist sie fuer kadavere Organe (die innerhalb von Stunden transplantiert werden muessen) klinisch unbrauchbar. Die hochaufloesende NGS-HLA-Typisierung liefert vergleichbare Information in Stunden.",
        difficulty = 5,
        funFact = "Die MLR wurde 1964 von Bain und Lowenstein beschrieben und diente Jahrzehnte lang als Goldstandard fuer die Transplantationskompatibilitaet bei Knochenmarktransplantationen."
    ),

    // Question 16 - Graft-versus-Host-Disease
    Question(
        categoryId = 16,
        questionText = "Welche drei Hauptmechanismen erklaeren die Pathophysiologie der akuten Graft-versus-Host-Disease (aGvHD) nach allogener Stammzelltransplantation (Billingham-Kriterien + Cytokinsturm)?",
        answerA = "Spender-T-Zellen erkennen Empfaenger-HLA als fremd (direkte Alloreaktivitaet), werden durch praeexistente Gewebeschaeden (Konditionierung) aktiviert, und loesen Organschaeden an Haut, Darm, Leber durch Zytokine und Zytotoxizitaet aus",
        answerB = "Empfaenger-NK-Zellen attackieren Spender-Stammzellen wegen KIR-Inkompatibilitaet",
        answerC = "Spender-B-Zellen produzieren Autoantikoerper gegen Empfaengergewebe",
        answerD = "Konditionierungschemotherapie schadigt Empfaengerorgane direkt ohne Immunbeteiligung",
        correctAnswer = 0,
        explanation = "Die Billingham-Kriterien (1966) beschreiben aGvHD als Folge von: (1) immunkompetenten Donorzellen im Transplantat, (2) immuninkompetenter Empfaenger, (3) Erkennung von Empfaengerantigenen. Die Konditionierungstherapie (Bestrahlung, Chemotherapie) schadigt Darmepithelien und setzt PAMPs/DAMPs frei (Cytokinsturm Phase 1), die APC aktivieren. Diese praesentieren Host-Antigene den Donor-T-Zellen, die proliferieren und Zieloorgane (Haut, Darm, Leber) durch Th1-Zytokine und CTL-Aktivitaet schadigen.",
        difficulty = 5
    ),

    // --- TUMORIMMUNOLOGIE ---

    // Question 17 - Immun-Editing: Drei Phasen
    Question(
        categoryId = 16,
        questionText = "Welche drei Phasen beschreibt das Konzept des Cancer Immunoediting nach Dunn und Schreiber (2004)?",
        answerA = "Initiation, Progression, Metastasierung",
        answerB = "Elimination, Equilibrium, Escape",
        answerC = "Primaertumor, Dormanz, Rezidiv",
        answerD = "Erkennung, Aktivierung, Zerstoerung",
        correctAnswer = 1,
        explanation = "Cancer Immunoediting beschreibt drei Phasen: (1) Elimination -- das Immunsystem erkennt und zerstoert immunogene Tumorzellen; (2) Equilibrium -- selektionierte weniger immunogene Tumorzellen ueberleben, das Immunsystem haelt Wachstum in Schach (klinische Dormanz); (3) Escape -- Tumorzellen mit Resistenzmechanismen wachsen unkontrolliert und verursachen klinischen Krebs. Dieses Modell erklaert, warum Tumoren bei Immunkompromittierten haeufiger auftreten.",
        difficulty = 5,
        funFact = "Das Equilibrium-Stadium kann Jahrzehnte dauern -- manche Transplantationsempfaenger entwickelten Tumoren des Organspenders Jahre nach Transplantation, was bewies, dass Spender-Tumorzellen im Dormanz-Stadium uebertragen worden waren."
    ),

    // Question 18 - PD-L1/PD-1-Checkpoint-Mechanismus
    Question(
        categoryId = 16,
        questionText = "Durch welchen molekularen Mechanismus hemmt PD-1-Signalling T-Zell-Aktivierung, und welche Phosphatase ist der proximale Effektor?",
        answerA = "PD-1 aktiviert PI3-Kinase und erhoert cAMP-Spiegel",
        answerB = "PD-1-ITIM wird phosphoryliert und rekrutiert SHP-2, die ZAP-70 und CD28 dephosphoryliert",
        answerC = "PD-1 blockiert direkt den TCR durch sterische Hinderung",
        answerD = "PD-1 induziert FoxP3-Expression und wandelt T-Effektorzellen in Tregs um",
        correctAnswer = 1,
        explanation = "Nach PD-L1/PD-L2-Bindung wird das zytoplasmatische ITIM (immunoreceptor tyrosine-based inhibitory motif) von PD-1 phosphoryliert und rekrutiert die Phosphatase SHP-2 (und SHP-1). SHP-2 dephosphoryliert ZAP-70 (haemmt TCR-Signalling) und die p85-Untereinheit von PI3K/CD28 (haemmt Kostimulationssignale), was zu T-Zell-Exhaustion fuehrt.",
        difficulty = 5,
        funFact = "Pembrolizumab und Nivolumab blockieren PD-1 und zeigen bei etwa 20-40% der nicht-kleinzelligen Lungenkarzinome dauerhaftes Ansprechen -- was vor 15 Jahren als unmoeglich galt, ist heute Standardtherapie in der Erstlinie."
    ),

    // Question 19 - Tumorinfiltrierende Lymphozyten und Exhaustion
    Question(
        categoryId = 16,
        questionText = "Welches Transkriptionsfaktor-Programm treibt T-Zell-Exhaustion im Tumormikromilieu, und welche Checkpoints werden dabei hochreguliert?",
        answerA = "T-bet-Hochregulation; haemmt PD-1 und TIM-3",
        answerB = "TOX und NR4A1 induzieren epigenetische Umprogrammierung; PD-1, LAG-3, TIM-3, TIGIT werden koexprimiert",
        answerC = "FoxO1-Kernexport haemmt Differenzierung; nur PD-1 ist relevant",
        answerD = "ROR-gammat treibt Exhaustion; IL-17 ist der Hauptmediator",
        correctAnswer = 1,
        explanation = "Chronische TCR-Stimulation im Tumormikromilieu induziert TOX (Thymocyte selection-associated HMG box) und NR4A1 (Nur77), die epigenetische Reprogrammierung der T-Zellen bewirken und ein Exhaustions-spezifisches Chromatin-Profil etablieren. Exhausted T-Zellen koexprimieren multiple inhibitorische Rezeptoren: PD-1, LAG-3, TIM-3, TIGIT und CTLA-4. Diese epigenetischen Aenderungen sind teilweise irreversibel, was die begrenzte Wirksamkeit einzelner Checkpoint-Inhibitoren erklaert.",
        difficulty = 5
    ),

    // Question 20 - CAR-T-Zellen: Aufbau
    Question(
        categoryId = 16,
        questionText = "Was unterscheidet einen CAR-T-Zell-Rezeptor der 2. Generation von einem der 1. Generation strukturell und funktionell?",
        answerA = "2. Generation: nur eine scFv-Bindungsdomaene statt zweier; reduzierte Aktivierung",
        answerB = "2. Generation: zusaetzliche intrazellulaere Kostimulatonsdomaene (CD28 oder 4-1BB) neben CD3-Zeta; verbesserte Proliferation und Persistenz",
        answerC = "2. Generation: bispezifisch, erkennt zwei Tumorantigene gleichzeitig",
        answerD = "2. Generation: verwendet TCR-alpha/beta statt scFv fuer HLA-abhaengige Erkennung",
        correctAnswer = 1,
        explanation = "CARs der 1. Generation hatten nur die CD3-Zeta-Signaldomaene -- ausreichend fuer Zytotoxizitaet, aber keine Kostimulation, daher geringe Persistenz in vivo. CARs der 2. Generation integrieren eine Kostimulationsdomaene (CD28 oder 4-1BB/CD137) proximal zur CD3-Zeta-Kette. CD28-CARs proliferieren schnell aber kurzlebig; 4-1BB-CARs zeigen laengere Persistenz und Gedaechtnis. Zugelassene Produkte wie Tisagenlecleucel (4-1BB) und Axicabtagene Ciloleucel (CD28) nutzen dieses Prinzip.",
        difficulty = 5,
        funFact = "CAR-T-Zellen der 3. Generation tragen zwei Kostimulationsdomaenen (z.B. CD28 + 4-1BB), waehrend 4. Generation (TRUCK) zusaetzlich Transgene fuer Zytokinsekretion (z.B. IL-12) eingebaut haben, um auch das Tumormikromilieu zu modulieren."
    ),

    // --- MOLEKULARE DIAGNOSTIK ---

    // Question 21 - NGS: Sequenzierungsprinzip
    Question(
        categoryId = 16,
        questionText = "Welches Sequenzierungsprinzip nutzt Illumina-NGS, und wie unterscheidet es sich von PacBio SMRT-Sequenzierung?",
        answerA = "Illumina: sequencing-by-synthesis mit reversiblen Terminatoren (short reads, ~150 bp, hohe Genauigkeit); PacBio: Einzelmolekuel-Echtzeit-Sequenzierung (long reads, >10 kb, hoehere Fehlerrate pro Read)",
        answerB = "Illumina: Nanoporen-Technologie; PacBio: chemische Degradation nach Maxam-Gilbert",
        answerC = "Illumina: Sanger-Kettenabbruch auf parallelen Chips; PacBio: pyrosequencing",
        answerD = "Illumina: ligation-based sequencing; PacBio: sequencing-by-synthesis mit nichtreversiblen Terminatoren",
        correctAnswer = 0,
        explanation = "Illumina verwendet sequencing-by-synthesis mit fluoreszenzmarkierten reversiblen 3'-Terminatoren auf Flow Cells -- jedes Cluster (geklonte Fragmente) wird gleichzeitig gelesen, was massive Parallelisierung ermoeglicht (short reads ~75-300 bp, Fehlerrate ~0,1%). PacBio SMRT-Sequenzierung beobachtet DNA-Polymerase-Aktivitaet in Echtzeit in Zero-Mode-Waveguides; dies erzeugt lange Reads (>10 kb, bis 30+ kb) bei hoeherem Einzelmolekuel-Fehler (~1%), aber nach Consensus-Berechnung (HiFi) sehr hohe Genauigkeit.",
        difficulty = 5
    ),

    // Question 22 - Liquid Biopsy und ctDNA
    Question(
        categoryId = 16,
        questionText = "Welche Eigenschaft von ctDNA (circulating tumor DNA) unterscheidet sie von normaler zellfreier DNA (cfDNA) und ermoeglicht ihre Detektion?",
        answerA = "ctDNA ist laenger (>1000 bp) als normale cfDNA",
        answerB = "ctDNA traegt tumorspezifische somatische Mutationen, Methylierungsaenderungen und charakteristische Fragmentierungsmuster (kuerzer, ~166 bp mit peaks bei Nukleosomenlinker)",
        answerC = "ctDNA ist doppelstraengig, normale cfDNA ist einzelstraengig",
        answerD = "ctDNA zirkuliert in Exosomen, cfDNA ist frei im Plasma",
        correctAnswer = 1,
        explanation = "ctDNA ist ein Bruchteil der gesamten cfDNA (0,01-5% je nach Tumorstadium) und traegt die somatischen Varianten des Tumors (SNVs, Indels, CNVs, Fusionen). Zuverlassige Detektion gelingt durch: (1) digitale PCR oder ultra-deep NGS mit error-correction (UMIs); (2) tumorspezifische Methylierungsprofile; (3) Fragmentomics -- ctDNA-Fragmente sind kuerzer (~166 bp) und zeigen andere Nukleosom-Positionierungsmuster als normale cfDNA.",
        difficulty = 5,
        funFact = "Die ctDNA-Halbwertszeit betraegt nur ~1-2 Stunden -- damit ist Liquid Biopsy potenziell empfindlicher als bildgebende Verfahren fuer minimale Resterkrankung (MRD) nach Therapie, da Tumordynamik in Echtzeit abgebildet wird."
    ),

    // Question 23 - Companion Diagnostics
    Question(
        categoryId = 16,
        questionText = "Was ist ein Companion Diagnostic (CDx), und welches regulatorische Prinzip verbindet es mit seiner Zieltherapie?",
        answerA = "Ein CDx ist ein klinischer Test, der nach Therapiebeginn den Erfolg bewertet; Zulassung ist unabhaengig vom Medikament",
        answerB = "Ein CDx ist ein In-vitro-Diagnostikum, das von der FDA/EMA zusammen mit einem bestimmten Therapeutikum ko-zugelassen wird und dessen Anwendung fuer die Therapieentscheidung zwingend erforderlich ist",
        answerC = "Ein CDx ist ein Biomarker-Panel fuer Screeningprogramme ohne therapeutische Konsequenz",
        answerD = "Ein CDx ist ein Genomik-Test der nur fuer klinische Studien zugelassen ist",
        correctAnswer = 1,
        explanation = "Companion Diagnostics werden von Regulierungsbehoerden (FDA, EMA) simultan mit der Zieltherapie zugelassen und sind rechtlich an diese gebunden -- die Therapie darf nur angewendet werden, wenn der CDx-Test einen positiven Biomarker-Status belegt. Beispiele: HER2-IHC/FISH (Trastuzumab), EGFR-Mutationstest (Erlotinib/Gefitinib), PD-L1-IHC (Pembrolizumab bei >50%-TPS-Score in der Erstlinie NSCLC), BRCA1/2 (Olaparib).",
        difficulty = 5
    ),

    // Question 24 - NGS-Fehlerkorrektur: UMI
    Question(
        categoryId = 16,
        questionText = "Welche Funktion haben Unique Molecular Identifiers (UMIs) in der Ultra-deep-NGS-Diagnostik fuer ctDNA?",
        answerA = "UMIs markieren jedes urspruengliche DNA-Molekuel vor PCR-Amplifikation eindeutig, um PCR-Duplikate zu identifizieren und echte Varianten von PCR-/Sequenzierfehlern zu unterscheiden",
        answerB = "UMIs sind Adapter-Sequenzen, die das Hybridisieren an Flow Cells ermoeglichen",
        answerC = "UMIs kodieren Probanden-IDs fuer Multiplexing und verhindern Proben-Verwechslung",
        answerD = "UMIs sind Qualitaetsscores, die von der Sequenzier-Software automatisch berechnet werden",
        correctAnswer = 0,
        explanation = "Bei Ultra-deep-Sequenzierung (>10.000x) fuer ctDNA-Detektion maskieren PCR- und Sequenzierfehler echte seltene Varianten. UMIs sind zufaellige Oligonukleotid-Barcodes (8-16 bp), die VOR der PCR an jedes DNA-Molekuel ligatiert werden. Alle Reads mit demselben UMI stammen aus demselben Ausgangsmolekuel (Familie) -- nur Varianten, die in der Mehrheit der Reads einer Familie vorhanden sind, werden als echte Varianten gewertet. Dies senkt die Fehlerrate auf ~10^-5 bis 10^-6.",
        difficulty = 5
    ),

    // --- PRAEZISIONSMEDIZIN ---

    // Question 25 - BRCA1/2 und PARP-Inhibitoren: Synthetical Lethality
    Question(
        categoryId = 16,
        questionText = "Erklaeren Sie das Konzept der synthetischen Lethalitaet, das PARP-Inhibitoren bei BRCA1/2-mutierten Tumoren wirksam macht.",
        answerA = "PARP-Inhibitoren blockieren Topoisomerase II und toeten alle proliferierenden Zellen; BRCA1/2-Mutation hat keinen Einfluss",
        answerB = "BRCA1/2-mutierte Zellen koennen DSBs nur durch fehlerhaftes NHEJ reparieren; PARP-Inhibierung blockiert die Backup-Einzelstrangbruch-Reparatur (BER), sodass Replikationsgabelkollapsse irreparierbare DSBs erzeugen und nur Tumorzellen sterben",
        answerC = "PARP-Inhibitoren aktivieren p53, das BRCA1/2-mutierte Tumorzellen praeferenziell in Apoptose treibt",
        answerD = "BRCA1/2-mutierte Zellen exprimieren mehr PARP, daher sind sie sensitiver fuer Inhibitor-Bindung",
        correctAnswer = 1,
        explanation = "BRCA1/2 sind essenziell fuer die homologe Rekombination (HR) zur Reparatur von DNA-Doppelstrangbruechen (DSBs). PARP1/2 reparieren Einzelstrangbrueche (SSBs) ueber BER. Wenn PARP inhibiert wird, persistieren SSBs und kollabieren bei Replikation zu DSBs. Normale Zellen reparieren diese DSBs via HR -- BRCA1/2-mutierte Tumorzellen koennen HR nicht durchfuehren und akkumulieren letale DSBs. Gesunde Zellen des Patienten sind heterozygot (ein wildes Allel) und ueberleben.",
        difficulty = 5,
        funFact = "Synthetical Lethality als therapeutisches Konzept wurde 2005 von Bryant und Farmer beschrieben -- es dauerte nur 9 Jahre bis zur FDA-Zulassung von Olaparib (2014), ein bemerkenswert schneller Weg von der Grundlagenforschung in die Klinik."
    ),

    // Question 26 - MSI/MMR und Immuntherapie-Ansprechen
    Question(
        categoryId = 16,
        questionText = "Warum sprechen Tumoren mit Mikrosatelliteninstabilitaet (MSI-H) / Mismatch-Repair-Defekt (dMMR) besonders gut auf PD-1-Checkpoint-Inhibitoren an?",
        answerA = "MSI-H-Tumoren exprimieren mehr PD-L1 auf der Zelloberflaeche",
        answerB = "dMMR-Tumoren akkumulieren tumorale Mutationslast (TMB) und produzieren viele Neoantigene, die tumorinfiltrierenden T-Zellen aktivieren, die durch PD-1-Blockade freigesetzt werden",
        answerC = "MMR-Proteine hemmen normalerweise T-Zell-Einwanderung; dMMR fuehrt zu T-Zell-reicher Umgebung unabhaengig von Neoantigenen",
        answerD = "MSI verursacht Chromosomeninstabilitaet, die cGAS-STING aktiviert und Interferon freisetzt",
        correctAnswer = 1,
        explanation = "MMR-Proteine (MLH1, MSH2, MSH6, PMS2) korrigieren Replikationsfehler an repetitiven DNA-Sequenzen (Mikrosatelliten). Fehlt diese Reparatur, akkumulieren Frameshiftmutationen und Nonsense-Mutationen -- typischerweise 10-100x mehr als in MMR-profizientem Gewebe (TMB-high). Diese hohe Mutationslast erzeugt viele Neoantigene auf MHC-I, gegen die tumorinfiltrerende CTLs reaktiv sind. PD-1-Inhibition befreit diese pre-existenten Immunantworten und erklaert die hohen Ansprechraten (ca. 40% ORR mit Pembrolizumab bei dMMR-Tumoren jeglicher Herkunft).",
        difficulty = 5
    ),

    // --- IMPFSTOFFTECHNOLOGIE ---

    // Question 27 - mRNA-Impfstoffe: Pseudouridin
    Question(
        categoryId = 16,
        questionText = "Welche chemische Modifikation ermoeglichte die erfolgreiche Entwicklung therapeutischer mRNA-Impfstoffe, und welches immunologische Problem loeste sie?",
        answerA = "5'-Cap-Analoga, um Nuklease-Resistenz zu erhoehen",
        answerB = "Substitution von Uridin durch N1-Methylpseudouridin (m1Psi), um die angeborene Immunerkennung durch TLR7/8 und PKR zu reduzieren und die Translationseffizienz zu steigern",
        answerC = "Methylierung des Phosphatrueckgrats fuer erhoehte Stabilitaet",
        answerD = "Insertion von IRES-Sequenzen fuer cap-unabhaengige Translation",
        correctAnswer = 1,
        explanation = "Kariko und Weissman (Nobelpreis 2023) entdeckten, dass die Substitution von Uridin durch Pseudouridin (Psi) und spater m1Psi die Erkennung durch TLR7, TLR8 und das dsRNA-sensorische System (PKR, OAS) dramatisch reduziert. Normale mRNA loest starke Typ-I-Interferon-Antworten aus, die Translation hemmen und Inflammation verursachen. m1Psi-modifizierte mRNA ist immunstill und wird effizienter in Protein uebersetzt -- die Basis der BNT162b2 (Pfizer) und mRNA-1273 (Moderna) COVID-19-Impfstoffe.",
        difficulty = 5,
        funFact = "Katalin Kariko und Drew Weissman erhielten 2023 den Nobelpreis fuer Physiologie oder Medizin -- Karikos Forschung zu mRNA-Modifikationen wurde jahrelang als zu basic science-orientiert abgelehnt; sie wurde sogar von der Universitaet Pennsylvanias degradiert, bevor ihre Arbeit zur Grundlage von Impfstoffen mit Milliarden Dosen wurde."
    ),

    // Question 28 - Adjuvantien: MF59 und AS01
    Question(
        categoryId = 16,
        questionText = "Was ist der molekulare Wirkmechanismus von AS01 (Adjuvantiensystem in Shingrix/RZV), das aus MPL und QS-21 besteht?",
        answerA = "AS01 bildet Nanoemulsionen, die Antigen in Depotformulierungen einschliessen und langsame Freisetzung ermoglichen",
        answerB = "MPL aktiviert TLR4 (TIR-Signalweg), QS-21 aktiviert NLRP3-Inflammasom und foerdert Antigenpraesentation; synergistische Wirkung induziert starke CD4-T-Zell- und Antikoerperantworten",
        answerC = "AS01 enthaelt CpG-Oligonukleotide, die TLR9 aktivieren und IFN-alpha induzieren",
        answerD = "AS01 ist ein Aluminiumsalz-Adjuvans, das durch Depot-Effekt und Inflammasom-Aktivierung wirkt",
        correctAnswer = 1,
        explanation = "AS01B (in Shingrix gegen Herpes zoster) kombiniert MPL (Monophosphoryl lipid A -- detoxifiziertes LPS-Derivat) und QS-21 (Saponin aus Quillaja saponaria) in Liposomen. MPL aktiviert TLR4 ueber den TRIF-Signalweg (proinflammatorisch ohne excessive Toxizitaet). QS-21 induziert NLRP3-Inflammasom-Aktivierung, Caspase-1 und IL-1beta/IL-18, und foerdert Kreuzpraesentation durch DCs. Die Kombination erzeugt starke CD4 Th1-Antworten (IFN-gamma, TNF) und hohe Antikoerpertiter.",
        difficulty = 5
    ),

    // Question 29 - Mukosale Immunitaet: sIgA
    Question(
        categoryId = 16,
        questionText = "Wie unterscheidet sich sekretorisches IgA (sIgA) strukturell und funktionell von serum-IgA, und wie entsteht die sekretorische Komponente?",
        answerA = "sIgA ist ein Monomer mit J-Kette; sekretorische Komponente ist eine zusaetzliche Disulfidbruecke",
        answerB = "sIgA ist ein dimer mit J-Kette, verbunden durch das poly-Ig-Rezeptor (pIgR)-System; die sekretorische Komponente ist das prozessierte Extrazellularsegment des pIgR nach Transcytose",
        answerC = "sIgA entsteht durch Glykosylierung von Serum-IgA in der Lamina propria",
        answerD = "sIgA wird direkt von Plasmazellen im Darmlumen sezerniert ohne epithelialen Transport",
        correctAnswer = 1,
        explanation = "Plasmazellen der Lamina propria sezernieren dimeres IgA mit J-Kette. Der poly-Ig-Rezeptor (pIgR) an der basolateralen Epithelzellmembran bindet dimeres IgA, transportiert es durch Transcytose ans apikale Darmlumen und wird dort proteolytisch gespalten -- das verbleibende Extrazellularsegment des pIgR bleibt als sekretorische Komponente am IgA-Dimer gebunden. sIgA ist damit Proteaseresistenter und wirkt hauptsaechlich durch Immun-Exklusion (Neutralisierung von Pathogenen im Darmlumen).",
        difficulty = 5
    ),

    // Question 30 - Nanopartikel-Impfstoffdesign
    Question(
        categoryId = 16,
        questionText = "Welchen immunologischen Vorteil bieten Lipid-Nanopartikel (LNPs) gegenueber freier mRNA bei der Impfstoffformulierung?",
        answerA = "LNPs erhoehen ausschliesslich die thermische Stabilitaet der mRNA ohne immunologischen Effekt",
        answerB = "LNPs schuetzen mRNA vor Nuklease-Abbau, ermoeglichen zellulaere Aufnahme via Endozytose, foerdern endosomales Escape, wirken als eingebautes Adjuvans durch ionisierbare Lipide und ermoeglichen gezieltes Targeting von Antigenpraesentierende Zellen",
        answerC = "LNPs aktivieren Komplement C3 und induzieren dadurch B-Zell-Kostimulation",
        answerD = "LNPs bewirken langsame Antigen-Freisetzung wie klassische Aluminiumhydroxid-Depots",
        correctAnswer = 1,
        explanation = "Ionisierbare Lipide in LNPs sind bei physiologischem pH neutral (vermindert Toxizitaet im Blut), werden aber im sauren Endosomenmilieu protoniert, was Membranfusion und endosomales Escape der mRNA in das Zytoplasma ermoeglicht. Gleichzeitig wirken LNPs intrinsisch adjuvant: ionisierbare Lipide aktivieren TLR4 und NLRP3, DAMP-Signale durch phagozytische Aufnahme und IL-1beta/IL-18-Freisetzung. PEGylierung verlaengert Bluthalbwertszeit, Groesse (~100 nm) foerdert lymphatische Drainage zu DCs.",
        difficulty = 5
    ),

    // --- IMMUNDEFEKTSYNDROME ---

    // Question 31 - SCID: Klassifikation
    Question(
        categoryId = 16,
        questionText = "Welcher haufigste genetische Defekt verursacht T-B+NK- SCID (schwerer kombinierter Immundefekt mit fehlenden T- und NK-Zellen, aber vorhandenen B-Zellen)?",
        answerA = "RAG1/RAG2-Mutation (T-B-NK+ SCID)",
        answerB = "Gamma-c-Ketten-Mutation (IL2RG; X-chromosomaler SCID) -- fehlende Signaltransduktion fuer IL-2, IL-4, IL-7, IL-9, IL-15, IL-21",
        answerC = "ADA (Adenosin-Desaminase)-Defekt (T-B-NK- SCID)",
        answerD = "MHC-Klasse-II-Defekt (Bare Lymphocyte Syndrome Typ II)",
        correctAnswer = 1,
        explanation = "Der haufigste SCID-Typ (ca. 40-50%) ist der X-gekoppelte SCID durch Mutation der gemeinsamen Gamma-Kette (gammac, IL2RG). Gammac ist Bestandteil der Rezeptoren fuer IL-2, IL-4, IL-7, IL-9, IL-15 und IL-21. IL-7 ist kritisch fuer T-Zell-Entwicklung im Thymus; IL-15 fuer NK-Zell-Entwicklung. B-Zellen entwickeln sich, da IL-7-unabhaengige Wege (BAFF, BCR-Signalling) bestehen, sind aber funktionslos ohne T-Zell-Hilfe (B+). JAK3, das mit gammac assoziiert, verursacht bei Mutation dasselbe T-B+NK- Muster.",
        difficulty = 5,
        funFact = "Alain Fischer gelang 1999 in Paris die erste Gentherapie fuer X-SCID (Gammaretrovirus-Vektor) -- spektakulaer erfolgreich, aber 5 von 20 Patienten entwickelten T-Zell-Leukaemie durch Insertionsmutagenese nahe LMO2. Dies fuehrte zur Entwicklung von Selbst-inaktivierenden Lentiviral-Vektoren als sicherere Alternative."
    ),

    // Question 32 - Bruton-Agammaglobulinaemie
    Question(
        categoryId = 16,
        questionText = "Welches Protein fehlt bei der Bruton-Agammaglobulinaemie (XLA), und an welchem Punkt der B-Zell-Entwicklung kommt es zum Arrest?",
        answerA = "BLNK fehlt; Arrest am Uebergang von praeB-II zu unreifer B-Zelle",
        answerB = "BTK (Bruton Tyrosine Kinase) fehlt; Arrest am Uebergang von praeB-I (proBi) zu praeB-II (prae-B-Zelle), da prae-BCR-Signalling nicht funktioniert",
        answerC = "Igalpha/Igbeta fehlen; Arrest vor jeglicher B-Zell-Differenzierung",
        answerD = "mu-Schwerkette fehlt; keine prae-BCR-Expression moeglich",
        correctAnswer = 1,
        explanation = "BTK (Bruton Tyrosine Kinase) ist eine Tec-Familie-Kinase, die essentiell fuer die prae-B-Zell-Rezeptor (prae-BCR)-Signaltransduktion ist. Das prae-BCR (mit mu-Schwerkette + Surrogate Light Chain) signalisiert ueber BTK Proliferation und Ueberleben der grossen praeB-Zellen und Rearrangement der Leichtkette. Ohne BTK: Arrest auf der praeB-I-Stufe, keine reifen B-Zellen, keine Immunglobuline. Klinisch: rezidivierende bakterielle Infektionen ab dem 6. Lebensmonat (nach Verschwinden maternaler Antikoerper).",
        difficulty = 5,
        funFact = "BTK-Inhibitoren (Ibrutinib, Acalabrutinib) sind zugelassene Krebsmedikamente fuer CLL und MCL -- paradoxerweise haemmen sie dieselbe Kinase, die bei XLA fehlt. Bei CLL nutzen Tumorzellen BTK-Signalling fuer ihr Ueberleben."
    ),

    // Question 33 - DiGeorge-Syndrom
    Question(
        categoryId = 16,
        questionText = "Welche genetische Ursache hat das DiGeorge-Syndrom (Thymus- und Nebenschilddruesenaplasie), und welcher T-Zell-Reifungsschritt ist beeintraechtigt?",
        answerA = "RAG2-Mutation; V(D)J-Rekombination betroffen",
        answerB = "22q11.2-Deletion (TBX1-Haploinsuffizienz); Thymusanlage aus dem 3. Pharyngealbogen fehlt, kein T-Zell-Reifungskompartiment verfuegbar",
        answerC = "IL-7-Rezeptor-Mutation; Thymus vorhanden aber T-Zell-Proliferation blockiert",
        answerD = "ADA-Mangel; toxische Metaboliten zerstoeren den Thymus postnatal",
        correctAnswer = 1,
        explanation = "Die Deletion 22q11.2 (1:4000 Geburten, haeufigstes Mikrodeletionssyndrom) fuehrt durch TBX1-Haploinsuffizienz zur fehlerhaften Entwicklung der 3. und 4. Schlundtasche. Der Thymus entwickelt sich aus dem 3. Pharyngealbogen -- bei vollstaendiger Deletion fehlt er komplett (komplettes DiGeorge: <1500 T-Zellen/ul, T-B+NK+ Phaenotyp). Da T-Zell-Reifung im Thymus stattfindet (positive/negative Selektion), entstehen kaum naive T-Zellen. Begleitend: Nebenschilddruesenaplasie (Hypokalzaemie), konotrunkaele Herzfehler.",
        difficulty = 5
    ),

    // Question 34 - Komplementdefekte
    Question(
        categoryId = 16,
        questionText = "Welche Bakteriengattung infiziert vorzugsweise Patienten mit terminalen Komplementdefekten (C5-C9; Membranattackenkomplex), und warum?",
        answerA = "Streptococcus pneumoniae; wegen fehlender Opsonierung durch C3b",
        answerB = "Neisseria (N. meningitidis, N. gonorrhoeae); wegen fehlender bakterizider Lyse durch den Membranattackenkomplex (MAC)",
        answerC = "Staphylococcus aureus; wegen fehlender chemotaktischer C5a-Rekrutierung von Neutrophilen",
        answerD = "Haemophilus influenzae; wegen fehlender C1q-vermittelter Komplementaktivierung",
        correctAnswer = 1,
        explanation = "Neisseria spp. haben eine aeussere Membran, gegen die der MAC (C5b-9) der einzige effektive Abwehrmechanismus ist -- Opsonophagozytose allein reicht nicht aus. Patienten mit C5-C9-Defekten (oder Eculizumab-Therapie, das C5 blockiert) haben ein drastisch erhoehtes Risiko (1000-10.000-fach) fuer meningokokkale und gonokokkale Infektionen. Dies ist der Grund, warum Eculizumab-Patienten obligat Meningokokken-Impfung und Antibiotikaprophylaxe erhalten.",
        difficulty = 5,
        funFact = "Eculizumab (Soliris), ein Antikoerper gegen C5, revolutionierte die Behandlung von paroxysmaler nokturnaler Haemoglobinurie (PNH) und aHUS -- war aber jahrelang das teuerste Medikament der Welt (ca. 400.000 Euro/Jahr), bis Biosimilars 2021 den Markt erreichten."
    ),

    // --- ZYTOKINSTURM UND IMMUNPATHOLOGIE ---

    // Question 35 - Zytokinsturm: Mechanismus
    Question(
        categoryId = 16,
        questionText = "Welche Zytokin-Feedback-Schleife ist zentral fuer die Amplifikation beim Zytokinsturm-Syndrom (z.B. bei sekundaerer HLH oder CAR-T-assoziiertem CRS)?",
        answerA = "IL-4/IL-13-Schleife aktiviert Mastzellen und eosinophile Granulozyten",
        answerB = "IL-6-Transignalling (IL-6/sIL-6R-Komplex aktiviert ubiquitaer gp130/JAK/STAT3) und IFN-gamma-Makrophagen-Aktivierungsschleife (IFN-gamma aktiviert Makrophagen, die IL-6 und IFN-gamma-induzierende Zytokine sezernieren)",
        answerC = "TNF/TNF-Rezeptor-Schleife hemmt Apoptose und haelt Entzuendung aufrecht",
        answerD = "IL-1/IL-1R-Schleife aktiviert ausschliesslich Endothelzellen",
        correctAnswer = 1,
        explanation = "Beim Zytokinsturm treiben zwei Hauptschleifen die Amplifikation: (1) IL-6-Transignalling -- IL-6 bindet den loeslichen IL-6-Rezeptor (sIL-6R) und dieser Komplex aktiviert ubiquitaer gp130, was JAK1/STAT3 aktiviert und Akutphasenproteine, Fibrinogen, Ferritin und weitere proinflammatorische Mediatoren induziert. (2) IFN-gamma aktiviert Makrophagen zur Hypersekretierung von IL-6, IL-1, TNF und M-CSF, die wiederum Makrophagenaktivierung verstaerken (Makrophagenaktivierungssyndrom). Tocilizumab (Anti-IL-6R) und Ruxolitinib (JAK1/2-Inhibitor) unterbrechen diese Schleifen.",
        difficulty = 5
    ),

    // Question 36 - HLH-Diagnostik: HScore
    Question(
        categoryId = 16,
        questionText = "Welche diagnostischen Kriterien verwendet der HScore fuer das haemophagozytische Lymphohistiozytose (HLH)-Syndrom, und was ist die kritische Ferritin-Schwelle?",
        answerA = "HScore bewertet: Temperatur, Organomegalie, Zytopenien, Ferritin (>500 ng/ml = 49 Punkte, >10.000 ng/ml = 49+50 Punkte), Triglyceriede, Fibrinogen, Haemophagozytose in Biopsie, Immunstatus, IL-2-Rezeptor",
        answerB = "HScore bewertet nur Ferritin, CRP und Haemoglobin; Schwelle ist Ferritin >1000 ng/ml",
        answerC = "HScore bewertet ausschliesslich genetische Marker (PRF1, UNC13D, STX11)",
        answerD = "HScore bewertet NK-Zell-Aktivitaet und sCD163; Schwelle ist sCD163 >10.000 ng/ml",
        correctAnswer = 0,
        explanation = "Der HScore (Fardet et al. 2014) addiert Punkte fuer: Temperatur (>38,4 Grad C, >39,4 Grad C), Organomegalie (Hepatomegalie, Splenomegalie), Zytopenien (1-3 Linien), Ferritin (>500, >1000, >10.000 ng/ml mit steigenden Punktzahlen), Triglyceriede, Fibrinogen (<2,5 g/l), Haemophagozytose in Knochenmark, Immunsuppression (HIV, Steroide) und AST. Ein Score von >169 Punkten hat >93% Spezifizitaet fuer HLH. Ferritin >10.000 ng/ml gilt als hochspezifischer Marker -- allerdings auch bei MAS und septischem Schock erhoehen.",
        difficulty = 5
    ),

    // Question 37 - MAS vs. sHLH
    Question(
        categoryId = 16,
        questionText = "Worin unterscheidet sich das Makrophagenaktivierungssyndrom (MAS) von der sekundaeren HLH (sHLH) pathophysiologisch und klinisch?",
        answerA = "MAS und sHLH sind identisch; der Begriff MAS wird nur fuer paedriatrische Patienten verwendet",
        answerB = "MAS tritt spezifisch bei autoinflammatorischen/autoimmunen Erkrankungen (SJIA, SLE, AOSD) auf und wird durch Grundkrankheit plus Trigger (Infektion) angetrieben; sHLH tritt typischerweise bei Malignomen (EBV-assoziiert, T/NK-Zell-Lymphom) auf -- UNC13D-Mutationen sind bei MAS seltener als bei primaerer HLH",
        answerC = "MAS hat immer genetische Ursache, sHLH ist immer reaktiv",
        answerD = "MAS betrifft ausschliesslich das ZNS, sHLH betrifft primaer Leber und Knochenmark",
        correctAnswer = 1,
        explanation = "MAS (auch als HLH assoziiert mit Rheumatologischen Erkrankungen bezeichnet) tritt bei SJIA (ca. 10-15% im Verlauf), SLE und AOSD auf -- Trigger sind oft Infektionen oder Medikamente. Pathophysiologisch zentral: IL-18-Hypersekretion (besonders bei SJIA), eingeschraenkte NK-Zell-Perforin-Sekretion (oft heterozygote UNC13D/PRF1-Varianten). sHLH bei Malignomen (v.a. EBV-getriggert oder bei T/NK-Zell-Lymphomen) hat schlechtere Prognose. Therapie-Ansaetze differieren: MAS: Steroide, Ciclosporin, Anakinra/Tocilizumab; sHLH: HLH-94 Protokoll (Etoposid, Dexamethason).",
        difficulty = 5
    ),

    // --- WEITERE MASTER-THEMEN ---

    // Question 38 - NK-Zell-Missing-Self-Hypothese
    Question(
        categoryId = 16,
        questionText = "Was beschreibt die Missing-Self-Hypothese fuer NK-Zellen, und welche Rezeptorfamilien sind die Effektoren?",
        answerA = "NK-Zellen toeten ausschliesslich Zellen mit aktivierenden NKG2D-Liganden, MHC-I-Expression spielt keine Rolle",
        answerB = "Gesunde Zellen hemmen NK-Zellen ueber MHC-I/KIR-Interaktion; Verlust von MHC-I (z.B. bei Tumorimmun-Escape oder Virusinfektion) entfernt dieses Inhibitionssignal und aktiviert NK-Zell-Killing",
        answerC = "NK-Zellen werden durch IFN-gamma aktiviert und toeten alle Zellen ohne TCR-Erkennung",
        answerD = "Missing-Self bedeutet das Fehlen von NK-Zellen bei bestimmten Immundefizienzen",
        correctAnswer = 1,
        explanation = "Klas Kaerres Missing-Self-Hypothese (1986): NK-Zellen tragen inhibitorische Rezeptoren (KIRs -- Killer Immunoglobulin-like Receptors, und Ly49 bei Maeuse), die eigenes MHC-I erkennen und NK-Aktivierung hemmen. Tumorzellen downregulieren MHC-I (Immune-Escape vor CTLs), verlieren aber dadurch den KIR-vermittelten Schutz vor NK-Zellen. Aktivierende Rezeptoren (NKG2D, NCRs) erkennen Stress-Liganden (MICA/MICB, Ulbp) auf geschaedigten Zellen. Die Balance aus Aktivierungs- und Inhibitionssignalen bestimmt NK-Zell-Aktivierung.",
        difficulty = 5,
        funFact = "KIR-Genotyp und HLA-Ligand-Kompatibilitaet beeinflusst das Ergebnis von haploidentischen Stammzelltransplantationen: KIR-inkompatible Spender-NK-Zellen haben einen 'Graft-versus-Leukemia'-Effekt -- ein klinisch genutzter Effekt bei AML-Transplantationen."
    ),

    // Question 39 - Regulatorische T-Zellen: FOXP3
    Question(
        categoryId = 16,
        questionText = "Welches Syndrom verursacht Verlust-von-Funktion-Mutationen in FOXP3, dem Mastertranskriptionsfaktor regulatorischer T-Zellen?",
        answerA = "Wiskott-Aldrich-Syndrom (WAS) -- Thrombozytopenie und Immundefekt",
        answerB = "IPEX-Syndrom (Immune dysregulation, Polyendocrinopathy, Enteropathy, X-linked) -- multiorgan Autoimmunitaet",
        answerC = "Chediak-Higashi-Syndrom -- partieller Albinismus und NK-Zell-Defekt",
        answerD = "Omenn-Syndrom -- oligoklonale T-Zell-Expansion mit Erythrodermie",
        correctAnswer = 1,
        explanation = "FOXP3-Mutationen verursachen das X-chromosomale IPEX-Syndrom (scurfy-Maus-Aequivalent). Ohne regulatorische T-Zellen (die FOXP3 fuer ihre Entwicklung und Funktion benoetigen) entsteht unkontrollierte Autoimmunitaet in multiplen Organen: Typ-1-Diabetes, autoimmune Enteropathie, Thyreoiditis, haemolytische Anaemie, Exzem. Klinisch manifestiert IPEX meist in den ersten Lebensmonaten und ist ohne Stammzelltransplantation letal.",
        difficulty = 5,
        funFact = "Die scurfy-Maus (spontane FOXP3-Mutation) stirbt innerhalb von 25 Tagen an multiorgan-Autoimmunitaet -- ein drastisches Beispiel, wie regulatorische T-Zellen aktiv Autoimmunpathologie verhindern."
    ),

    // Question 40 - Toleranz: Zentral vs. Peripher
    Question(
        categoryId = 16,
        questionText = "Welches Protein ermoeglicht die negative Selektion von Thymozyten gegen peripherie Antigene im Thymus-Medulla, und welches Syndrom entsteht bei seinem Fehlen?",
        answerA = "MHC-II ermoeglicht Praesentation aller Antigene; Fehlen fuehrt zum Bare-Lymphocyte-Syndrom",
        answerB = "AIRE (Autoimmune Regulator) induziert ektopische Expression peripherer Antigene in thymischen Epithelzellen; AIRE-Mutationen verursachen APS-1 (APECED)",
        answerC = "RAG1/2 ist fuer Negativ-Selektion essentiell; Fehlen verursacht Omenn-Syndrom",
        answerD = "CTLA-4 auf Thymozyten hemmt Autoreaktivitaet; CTLA-4-Defekt fuehrt zu letal Autoimmunitaet",
        correctAnswer = 1,
        explanation = "AIRE (Autoimmune Regulator, Chromosom 21) ist ein Transkriptionsfaktor in medullaren thymischen Epithelzellen (mTECs), der die ektopische Expression tausender peripherer Gewebeantigene (Insulin, Thyroglobulin, etc.) ermoeglicht. Thymics, die diese Antigene mit hoher Affinitaet erkennen, sterben durch negative Selektion (klonale Deletion). AIRE-Mutationen verursachen APS-1 (APECED -- Autoimmune Polyendocrinopathy Candidiasis Ectodermal Dystrophy): Addison-Erkrankung, Hypoparathyreoidismus, chronische mukokutane Candidiasis, plus diverse Autoimmunerkrankungen.",
        difficulty = 5
    ),

    // Question 41 - Antigenrezeptor-Diversitaet: V(D)J-Rekombination
    Question(
        categoryId = 16,
        questionText = "Welche molekularen Mechanismen erzeugen junctionelle Diversitaet bei der V(D)J-Rekombination und wie gross ist das theoretische TCR-Repertoire?",
        answerA = "Kombinatorische Diversitaet (V-D-J-Segmentkombinationen) allein erzeugt ~10^6 Rezeptoren",
        answerB = "P-Nucleotide (palindromische Sequenzen durch Haarnadelspaltung), N-Nucleotide (TdT-Additionan an Schnittstellen) und exonukleolytischer Abbau erzeugen junctionelle Diversitaet; kombiniert mit kombinatorischer Diversitaet >10^15 moegliche TCRalphabeta-Rezeptoren",
        answerC = "Somatische Hypermutation erzeugt junctionelle Diversitaet im TCR genauso wie bei Immunoglobulinen",
        answerD = "Alternative Spleissing der TCR-prae-mRNA erzeugt Junctions-Diversitaet ohne DNA-Rekombination",
        correctAnswer = 1,
        explanation = "Junctionelle Diversitaet entsteht durch: (1) Haarnadelspaltung durch RAG1/2 erzeugt Haarnadelstrukturen, die asymmetrisch gespalten werden und P-Nucleotide (palindromische Insertionen) generieren; (2) TdT (terminale Desoxynukleotidyltransferase) fuegt N-Nucleotide (zufaellige Basen) an den Schnittstellen ein; (3) Exonukleasen bauen Nucleotide ab. Diese zufaelligen Prozesse an der D-J und V-DJ Verbindung erzeugen die maximale Diversitaet gerade an der Antigen-Kontaktstelle (CDR3). TCRalphabeta: kombinatorisch ~10^6, junctionell bis 10^15 theoretisch.",
        difficulty = 5
    ),

    // Question 42 - Immuncheckpoint: CTLA-4 vs. PD-1
    Question(
        categoryId = 16,
        questionText = "In welcher Phase der T-Zell-Aktivierung wirken CTLA-4 und PD-1 als Checkpoints, und wo im Koerper ist ihr Wirkungsort primaer?",
        answerA = "CTLA-4 und PD-1 wirken beide in der Effektorphase im peripheren Gewebe",
        answerB = "CTLA-4 reguliert die primaere T-Zell-Aktivierung im lymphatischen Gewebe (kostimulationscheck: konkurriert mit CD28 um B7-Liganden); PD-1 reguliert Effektor-T-Zellen im peripheren Gewebe (Terminationscheck)",
        answerC = "CTLA-4 wirkt im Thymus waehrend negativer Selektion; PD-1 wirkt nur in Tumoren",
        answerD = "Beide wirken ausschliesslich auf regulatorische T-Zellen",
        correctAnswer = 1,
        explanation = "CTLA-4 wird nach T-Zell-Aktivierung hochreguliert, konkurriert mit CD28 um B7-1/B7-2 (CD80/CD86) auf APCs in Lymphknoten und verhindert bzw. begrenzt Kostimulation -- ein 'Gate-Keeper' der primaeren T-Zell-Aktivierung. PD-1 wird bei chronischer Antigen-Exposition exprimiert und interagiert mit PD-L1/PD-L2 auf Tumorzellen und peripheren Gewebszellen -- ein 'effector damper' am Ort der Antigenexposition. Daher haben CTLA-4-Inhibitoren (Ipilimumab) breitere Autoimmunitaets-Nebenwirkungen, waehrend PD-1-Inhibitoren organspezifische Toxizitaet zeigen.",
        difficulty = 5,
        funFact = "Ipilimumab (Anti-CTLA-4) wurde 2011 zugelassen und war der erste Checkpoint-Inhibitor -- James Allison erhielt dafuer (zusammen mit Tasuku Honjo fuer PD-1) den Nobelpreis 2018."
    ),

    // Question 43 - Bispecific Antibodies: CD3xCD19
    Question(
        categoryId = 16,
        questionText = "Wie wirken bispezifische T-Zell-Engager (BiTE, z.B. Blinatumomab) zur B-Zell-Lymphom-Therapie, und warum loesen sie Zytokinsturm aus?",
        answerA = "BiTEs blockieren PD-1/PD-L1 und reaktivieren tumoreindringende T-Zellen",
        answerB = "BiTEs binden gleichzeitig CD3epsilon auf T-Zellen und CD19 auf B-Zell-Tumoren, bilden eine artifizielle Immunsynapse und aktivieren T-Zell-Zytotoxizitaet ohne Kostimulation; rapide Zytokinfreisetzung durch simultane T-Zell-Aktivierung und Tumorzelllyse verursacht CRS",
        answerC = "BiTEs aktivieren Komplement auf Tumorzellen und loesen dadurch MAC-Lyse aus",
        answerD = "BiTEs enthalten Toxin-Konjugate (wie ADCs) und liefern Zytotoxika direkt an CD19+ Zellen",
        correctAnswer = 1,
        explanation = "Blinatumomab (CD19xCD3 BiTE) ist ein Tandems-scFv-Protein ohne Fc-Region. Einer der scFv-Arme bindet CD3epsilon (des TCR/CD3-Komplexes) auf beliebigen T-Zellen, der andere CD19 auf B-Zell-Tumorzellen. Diese erzwungene Koppelung aktiviert T-Zellen antigen-unabhaengig (kein HLA noetig, kein TCR-Matching), bildet eine artifizielle Synapse und loest Perforin/Granzym-Zytotoxizitaet aus. Die massive simultane T-Zell-Aktivierung bei hoher Tumorlast fuehrt zu Zytokin-Freisetzungssyndrom (CRS) -- durch stufenweise Dosiseskalation und Prophylaxe mit Dexamethason kontrollierbar.",
        difficulty = 5
    ),

    // Question 44 - Epigenetik der Immunzell-Differenzierung: T-Zell-Gedaechtnis
    Question(
        categoryId = 16,
        questionText = "Welche epigenetischen Mechanismen unterscheiden naive T-Zellen, Effektor-T-Zellen und Gedaechtnis-T-Zellen bezueglich des IFNG-Genlocus?",
        answerA = "Naive T-Zellen: IFNG vollstaendig methyliert (geschlossen); Effektor-T-Zellen: IFNG demethyliert und aktiv; Gedaechtnis-T-Zellen: IFNG teilweise demethyliert (permissive Chromatinstruktur), schnellere Reaktivierung moeglich",
        answerB = "Alle T-Zell-Subtypen haben identische IFNG-Methylierung; Regulation ist ausschliesslich transkriptionell",
        answerC = "Gedaechtnis-T-Zellen methylieren IFNG staerker als naive T-Zellen, um Autoimmunitaet zu verhindern",
        answerD = "Epigenetik hat keinen Einfluss auf IFNG; T-bet ist der einzige Regulator",
        correctAnswer = 0,
        explanation = "Der IFNG-Genlocus ist ein Paradebeispiel fuer epigenetisches Imprinting bei T-Zell-Differenzierung: Naive CD8-T-Zellen: CpG-Methylierung der IFNG-CNS1/CNS2 Regionen -- Chromatin geschlossen, keine IFN-gamma-Produktion. Effektor-CTLs: TET-Enzyme (Hydroxymethylierung) und Histon-Acetylierung oeffnen Chromatin, T-bet bindet, IFN-gamma wird produziert. Gedaechtnis-T-Zellen behalten partielle Demethylierung -- sogenanntes 'epigenetisches Gedaechtnis', das bei Antigenrekontakt rasche und staerkere IFN-gamma-Sekretion ermoeglicht (anamnestic response).",
        difficulty = 5
    ),

    // Question 45 - Dendritsiche Zellen: DC1 vs. DC2
    Question(
        categoryId = 16,
        questionText = "Welche funktionellen Unterschiede bestehen zwischen konventionellen dendritischen Zellen Typ 1 (cDC1) und Typ 2 (cDC2) bei der T-Zell-Polarisierung?",
        answerA = "cDC1 (CLEC9A+, XCR1+, IRF8-abhaengig): Spezialisiert auf Kreuzpraesentation (MHC-I) von intrazellulaeren Pathogenen/Tumorantigenen, praefeenzielle CD8-T-Zell-Aktivierung, IL-12-Produktion (Th1/CD8-Antwort). cDC2 (CLEC12A+, SIRPalpha+, IRF4-abhaengig): Primaer MHC-II-Praesentation, Th2/Th17-Polarisierung",
        answerB = "cDC1 produziert IL-4 und foerdert Th2-Antworten; cDC2 produziert IL-12 und foerdert Th1-Antworten",
        answerC = "cDC1 und cDC2 haben identische Funktionen, unterscheiden sich nur in Lokalisation",
        answerD = "cDC1 differenziert zu Makrophagen; cDC2 differenziert zu Mastzellen",
        correctAnswer = 0,
        explanation = "cDC1 (Batf3/IRF8-transkriptionsfaktorabhaengige Entwicklung) exprimieren XCR1, CLEC9A, CD8alpha (im Lymphknoten) oder CD103 (im Gewebe) und sind auf Kreuzpraesentation exogener Antigene auf MHC-I spezialisiert -- essenziell fuer anti-virale und anti-tumorale CD8-T-Zell-Antworten sowie IL-12-gestuetzte Th1-Polarisierung. cDC2 (Notch2/IRF4-abhaengig) exprimieren SIRPalpha und praeferieren klassische MHC-II-Praesentation mit Th2- und Th17-Induktion, wichtig fuer extrazellulaere Pathogene und Helminthen-Abwehr.",
        difficulty = 5
    ),

    // Question 46 - Immunometabolismus: mTOR vs. AMPK
    Question(
        categoryId = 16,
        questionText = "Wie steuert der mTORC1-Signalweg die T-Zell-Differenzierung und warum foerdert sein Hemmer Rapamycin die Gedaechtnis-T-Zell-Bildung?",
        answerA = "mTORC1 foerdert oxidative Phosphorylierung und Gedaechtnis; Rapamycin hemmt dies und foerdert Glykolyse in Effektorzellen",
        answerB = "Aktives mTORC1 foerdert aerobe Glykolyse und Effektor-T-Zell-Differenzierung (T-bet, Gata3-Expression); Rapamycin-Hemmung von mTORC1 begienstigt mitochondriale Biogenese, oxidative Phosphorylierung und Gedaechtnis-T-Zell-UeberlebensProgram (TCF7, Eomes, AMPK-Aktivitaet)",
        answerC = "mTORC1 steuert ausschliesslich T-Zell-Groesse und Proteinsynthese ohne Einfluss auf Differenzierung",
        answerD = "Rapamycin hemmt calcineurin und blockiert damit IL-2-abhaengige Proliferation",
        correctAnswer = 1,
        explanation = "Bei T-Zell-Aktivierung schaltet mTORC1 (via TCR+CD28+IL-2) auf Warburg-Metabolismus (aerobe Glykolyse) um, was schnelles Wachstum und Effektor-Differenzierung (hohe T-bet/Granzym-Expression) foerdert. Nach Antigen-Clearance muessen Gedaechtnis-T-Zellen auf oxidative Phosphorylierung und Fettsaeuremetabolismus umschalten -- AMPK-Aktivierung und mTORC1-Abnahme ermoglichen dies. Rapamycin nach Immunisierung foerdert experimentell die Bildung langlebiger Gedaechtnis-T-Zellen mit hoeherer Qualitaet -- genutzt in Impfstoffstudien.",
        difficulty = 5
    ),

    // Question 47 - Microbiom und Immunmodulation
    Question(
        categoryId = 16,
        questionText = "Ueber welchen molekularen Mechanismus stimulieren kurzkettige Fettsaeuren (SCFAs) der Darmmikrobiota die Differenzierung von Treg-Zellen im Kolon?",
        answerA = "SCFAs aktivieren Toll-like-Rezeptoren auf Makrophagen und induzieren IL-10-Sekretion",
        answerB = "Butyrat und Propionat hemmen Histon-Deacetylasen (HDACs) in naiven CD4-T-Zellen und erhoehen damit die Histonacetylierung am FOXP3-Genlocus (speziell CNS1/CNS3), was FOXP3-Expression und Treg-Differenzierung foerdert",
        answerC = "SCFAs binden direkt an FOXP3 und stabilisieren sein Protein",
        answerD = "SCFAs aktivieren GPR109a auf Epithelzellen, die dann TGF-beta1 sezernieren",
        correctAnswer = 1,
        explanation = "Colonozyten und T-Zellen nehmen Butyrat und Propionat (produziert durch Bacteroidetes/Firmicutes-Fermentation von Ballaststoffen) auf. Als HDAC-Inhibitoren verringern SCFAs die Deacetylierung von Histon H3 an Lysin 27 am FOXP3-Genlocus, insbesondere an den konservierten nicht-kodierenden Sequenzen CNS1 (TGF-beta-responsiv) und CNS3 (Enhancer). Dies erhoht FOXP3-Transkription und stabilisiert kolische Tregs -- ein Mechanismus, durch den Darmmikrobiom-Zusammensetzung direkt intestinale Toleranz und systemische Autoimmunitaet beeinflusst.",
        difficulty = 5,
        funFact = "Kinder aus Amish-Gemeinschaften (reicher Mikrobiomdiversitaet durch Bauernhof-Exposition) haben dramatisch niedrigere Asthma-Praevalenz als Hutterite-Kinder (industrialisierte Landwirtschaft, weniger Mikrobiomdiversitaet) -- ein natuerliches Experiment, das den Microbiom-Immun-Zusammenhang illustriert (Stein et al. 2016, NEJM)."
    ),

    // Question 48 - Nanoporen-Sequenzierung in der Diagnostik
    Question(
        categoryId = 16,
        questionText = "Welchen diagnostischen Vorteil bietet Oxford Nanopore-Sequenzierung gegenueber Illumina-NGS bei der Infektionsdiagnostik in der ICU?",
        answerA = "Nanopore ist guenstiger bei Routinetests mit hohem Probenvolumen",
        answerB = "Nanopore ermoeglicht Real-time-Sequenzierung mit erstem identifizierbarem Ergebnis nach 15-30 Minuten (statt 24-72 Stunden bei Illumina), long reads decken vollstaendige Resistenzgene und Plasmide auf, kein Amplifikationsbias -- kritisch fuer zeitkritische Therapieentscheidungen bei Sepsis",
        answerC = "Nanopore hat deutlich niedrigere Fehlerrate als Illumina-Short-reads",
        answerD = "Nanopore kann RNA und DNA gleichzeitig in einer einzigen Reaktion sequenzieren ohne Reverse Transkription",
        correctAnswer = 1,
        explanation = "Oxford Nanopore Technology (ONT) misst Stromaenderungen beim Durchfuehren einzelner DNA/RNA-Molekuele durch biologische Poren (MspA, CsgG) -- kein Amplifikationsschritt noetig, lange Reads (10-100+ kb), und wichtigster Vorteil: Basecalling erfolgt in Echtzeit. Erste FASTQ-Reads sind nach 15-30 Minuten verfuegbar. Im Sepsis-Kontext wurde demonstriert, dass Direktsequenzierung von Blutproben Pathogenidentifikation und Resistenzgene in 2-6 Stunden liefern kann, waehrend Kulturen 48-72 Stunden benoetigen. Limitation: hoehere single-read-Fehlerrate (Q20 = 99% in neuesten Protokollen, aber Grundrate hoeher als Illumina).",
        difficulty = 5
    ),

    // Question 49 - Systemische Lupus-Pathogenese: NETosis
    Question(
        categoryId = 16,
        questionText = "Welche Rolle spielen Neutrophil Extracellular Traps (NETs) in der SLE-Pathogenese hinsichtlich des Typ-I-Interferon-Kreislaufs?",
        answerA = "NETs aktivieren B-Zellen direkt durch TLR2/4-Stimulation und induzieren Autoantikoerper-Produktion ohne IFN-Beteiligung",
        answerB = "NETs aus low-density granulocytes enthalten oxidierte DNA und antimikrobielle Proteine (LL37, HMGB1); diese NET-Komplexe aktivieren TLR7/9 und cGAS-STING in plasmacytoiden dendritischen Zellen, was massive IFN-alpha/beta-Produktion ausloest; IFN-alpha senkt NET-Clearance-Kapazitaet (DNase1, Komplementrezeptoren) und verstaerkt den Kreislauf",
        answerC = "NETs koagulieren Blutgefaesse und verursachen SLE-Vaskulitis rein mechanisch",
        answerD = "NETs induzieren IL-17 von Th17-Zellen, nicht Typ-I-Interferone",
        correctAnswer = 1,
        explanation = "SLE-Patienten zeigen IFN-Signatur (hoch exprimierte ISGs im Blut) und erhoehte NETosis von low-density granulocytes (LDGs, eine Neutrophilen-Subpopulation). NET-Prozesse: Elastase/MPO-abhaengig oder PAD4-vermittelt (Citrullinierung von Histonen). NETs freigesetzt von LDGs enthalten mitochondriale DNA (oxidiert, immunogener als nukleaere DNA), LL37 (ein endogenes Adjuvans) und HMGB1. LL37-DNA-Komplexe werden von pDC-Endosomen nicht abgebaut sondern aktivieren TLR9 fuer IFN-alpha. IFN-alpha praesentiert dann Autoantigene effizienter, aktiviert B-Zellen und erhoht Autoantikoerperproduktion (anti-dsDNA, anti-Smith).",
        difficulty = 5
    ),

    // Question 50 - Klonale Haematopoese und Immunoseneszenz
    Question(
        categoryId = 16,
        questionText = "Wie beeinflusst klonale Haematopoese unbestimmten Potenzials (CHIP) das Immunsystem, und warum erhoehen CHIP-Mutationen das kardiovaskulaere Risiko?",
        answerA = "CHIP-Mutationen verursachen Lymphopenie und erhoehen Infektionsrisiko; kardiovaskulaeres Risiko entsteht durch Thrombozytenfunktionstoerung",
        answerB = "CHIP (bevorzugt TET2-, DNMT3A-, ASXL1-Mutationen) fuehrt zu klonaler Expansion von Myeloid-Vorlaeufern mit veraendertem Entzuendungsprogramm: TET2-mutierte Makrophagen produzieren erheblich mehr IL-6, IL-1beta (hypomorphes DNMT3A verliert Methylierungskontrolle ueber proinflammatorische Gene); chronische subklinische Entzuendung beschleunigt Atherosklerose durch Schaumzell-Differenzierung und Plaque-Instabilitaet",
        answerC = "CHIP verursacht autoimmune Haemolytische Anaemie durch klonale B-Zell-Expansion",
        answerD = "CHIP-Klone haben erhoehten VEGF-Spiegel und foerdern pathologische Angiogenese",
        correctAnswer = 1,
        explanation = "CHIP bezeichnet klonale Expansion haematopoetischer Stammzellen mit somatischen Mutationen ohne malignen Phaenotyp (>2% klonale Fraktion). Haufigste Mutationen: TET2 (ein DNA-Demethylierungsenzym), DNMT3A (DNA-Methyltransferase), ASXL1 (chromatinregulierende). TET2-mutierte Makrophagen zeigen reduzierte KCNQ1OT1/NEAT1-regulierte Suppression des NLRP3-Inflammasoms und produzieren mehr IL-1beta und IL-6 bei Stimulation. Diese Hyperinflammation foerdert Atheroskleroseplaques: in Ldlr-/- Maeuse mit TET2-mutierten Knochenmarktransplantaten zeigten groessere atherosklerotische Laesionen. CHIP-Praevalenz steigt mit Alter (70% der Ueber-70-jaehrigen haben CHIP-Mutationen).",
        difficulty = 5,
        funFact = "Die Entdeckung, dass CHIP das kardiovaskulaere Risiko verdoppelt (Jaiswal et al. 2017, NEJM) war eine Sensation -- es existiert nun ein Mechanismus, der erklaert, warum nach erfolgreicher Krebstherapie (CHIP-Expansion durch Chemotherapie) das Herzinfarkt-Risiko steigt, unabhaengig von traditionellen Risikofaktoren."
    )

)
