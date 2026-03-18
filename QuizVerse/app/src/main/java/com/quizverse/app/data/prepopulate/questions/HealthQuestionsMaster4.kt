package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun healthQuestionsMaster4(): List<Question> = listOf(

    // --- MOLEKULARE ONKOLOGIE ---

    // Question 1 - Driver vs Passenger Mutations
    Question(
        categoryId = 16,
        questionText = "Was unterscheidet eine Driver-Mutation von einer Passenger-Mutation in der Tumorbiologie?",
        answerA = "Driver-Mutationen entstehen frueher in der Tumorentwicklung und sind daher immer in allen Tumorzellen vorhanden",
        answerB = "Driver-Mutationen verleihen der Zelle einen Selektionsvorteil und treiben die Tumorprogression aktiv an, waehrend Passenger-Mutationen zufaellig akkumulieren und keinen Selektionsvorteil bieten",
        answerC = "Driver-Mutationen betreffen ausschliesslich Tumorsuppressorgene, Passenger-Mutationen betreffen nur Onkogene",
        answerD = "Driver-Mutationen sind stets Punktmutationen, Passenger-Mutationen stets chromosomale Aberrationen",
        correctAnswer = 1,
        explanation = "Driver-Mutationen (auch treibende Mutationen) werden positiv selektiert, weil sie Zellproliferation foerdern, Apoptose hemmen oder Invasion ermoeglichen. Passenger-Mutationen sind biologisch neutrale Begleitschaeden der genomischen Instabilitaet und werden nur passiv weitervererbt.",
        difficulty = 5,
        funFact = "Durchschnittlich enthaelt ein Tumor 2-8 Driver-Mutationen, aber Hunderte bis Tausende Passenger-Mutationen -- der Tumor Mutational Burden (TMB) erfasst hauptsaechlich diese Passenger-Mutationen, die paradoxerweise als Biomarker fuer Immuntherapie-Ansprechen dienen."
    ),

    // Question 2 - Tumor Mutational Burden
    Question(
        categoryId = 16,
        questionText = "Ab welchem Schwellenwert gilt der Tumor Mutational Burden (TMB) als hoch, und welche klinische Konsequenz hat dies?",
        answerA = "TMB >= 5 Mutationen/Megabase; Indikation fuer PARP-Inhibitoren",
        answerB = "TMB >= 10 Mutationen/Megabase (laut FDA-Zulassung Pembrolizumab); praeditiv fuer Ansprechen auf PD-1/PD-L1-Checkpoint-Inhibitoren",
        answerC = "TMB >= 20 Mutationen/Megabase; Indikation fuer dosisdichte Chemotherapie",
        answerD = "TMB >= 50 Mutationen/Megabase; Ausschlusskriterium fuer Immuntherapie wegen Autoimmunitaetsrisiko",
        correctAnswer = 1,
        explanation = "Die FDA erteilte 2020 eine tumoragnostische Zulassung fuer Pembrolizumab bei TMB >= 10 Mut/Mb bei soliden Tumoren. Hoher TMB korreliert mit mehr Neoantigenen, die das Immunsystem als fremd erkennt und durch T-Zellen angreift -- daher besseres Ansprechen auf Checkpoint-Blockade.",
        difficulty = 5,
        funFact = "Melanome und Lungenkarzinome nach Nikotinabusus haben die hoechsten TMB-Werte, da UV-Strahlung und Tabakkanzerogene eine besonders hohe Mutationsrate erzeugen. MSI-H-Tumoren haben ebenfalls sehr hohes TMB, da die Mismatch-Repair-Maschinerie fehlt."
    ),

    // Question 3 - Onkogenaktivierung
    Question(
        categoryId = 16,
        questionText = "Welcher Mechanismus liegt der EGFR-Aktivierung beim Nicht-kleinzelligen Lungenkarzinom (NSCLC) am haeufigsten zugrunde?",
        answerA = "EGFR-Genamplifikation mit mehr als 20 Kopien",
        answerB = "Aktivierende Punktmutationen im Exon 19 (Deletion) oder Exon 21 (L858R) der Tyrosinkinasedomaene",
        answerC = "EGF-Ueberexpression durch transkriptionelle Aktivierung",
        answerD = "EGFR-Fusionsproteine mit ALK analog zu EML4-ALK",
        correctAnswer = 1,
        explanation = "Exon-19-Deletionen und die L858R-Punktmutation in Exon 21 machen zusammen etwa 85-90% aller EGFR-Mutationen beim NSCLC aus. Beide Mutationen fuehren zu konstitutiver Aktivierung der Tyrosinkinase unabhaengig von Ligandenbindung und sind praeditiv fuer das Ansprechen auf EGFR-Tyrosinkinaseinhibitoren (TKI) wie Erlotinib, Gefitinib oder Osimertinib.",
        difficulty = 5,
        funFact = "Die T790M-Resistenzmutation in Exon 20 entsteht haeufig unter Therapie mit Erst- und Zweitgenerations-EGFR-TKI. Osimertinib (dritte Generation) ueberwindet T790M-Resistenz gezielt und wird heute auch in der Erstlinientherapie eingesetzt."
    ),

    // Question 4 - Tumorsuppressorgene
    Question(
        categoryId = 16,
        questionText = "Was beschreibt das Knudson-Two-Hit-Modell fuer Tumorsuppressorgene?",
        answerA = "Zwei unabhaengige Mutationen im gleichen Allel eines Tumorsuppressorgens sind notwendig, um eine dominante negative Wirkung zu erzielen",
        answerB = "Beide Allele eines Tumorsuppressorgens muessen inaktiviert werden (ein somatischer und oft ein ererbter oder zweiter somatischer Treffer), bevor die Tumorsuppressorfunktion verloren geht",
        answerC = "Zwei verschiedene Tumorsuppressorgene muessen gleichzeitig mutiert sein, bevor ein Tumor entsteht",
        answerD = "Eine Keimbahnmutation (erster Hit) genuegt fuer die Tumorentstehung, der zweite Hit beschleunigt nur die Metastasierung",
        correctAnswer = 1,
        explanation = "Alfred Knudson postulierte 1971 anhand von Retinoblastom-Daten, dass Tumorsuppressorgene rezessiv wirken -- beide Allele muessen inaktiviert sein. Beim hereditaeren Retinoblastom ist ein Allel der RB1-Keimbahnmutation bereits ausser Gefecht, sodass nur noch ein einziger somatischer Treffer genuegt, was das fruehe und bilaterale Auftreten erklaert.",
        difficulty = 5,
        funFact = "Knudsons statistischer Beweis ohne moderne Gensequenzierung gilt als eines der elegantesten Experimente der Tumorbiologie. Er erhielt 1998 den Lasker Award -- den haeufig als Vorbote des Nobelpreises geltenden Preis -- fuer diese Arbeit."
    ),

    // --- AML UND FAB-KLASSIFIKATION ---

    // Question 5 - AML FAB-Klassifikation
    Question(
        categoryId = 16,
        questionText = "Welches zytomorphologische Merkmal definiert die AML M3 (akute Promyelozytenleukaemie) in der FAB-Klassifikation?",
        answerA = "Mehr als 20% Myeloblasten ohne Ausreifung und haeufige Auerstabchen",
        answerB = "Dominanz neoplastischer Promyelozyten mit reichlich azurophilen Granula und charakteristischen Auerstabbuendeln (Faggot-Zellen)",
        answerC = "Monozytoide Differenzierung mit Nachweis von Lysozym und unspezifischer Esterase",
        answerD = "Erytroblastoide Differenzierung mit mehr als 50% Erythroblasten im Knochenmark",
        correctAnswer = 1,
        explanation = "AML M3 (APL -- Akute Promyelozytenleukaemie) zeigt morphologisch pathologische Promyelozyten mit dichten azurophilen Granula und Faggot-Zellen (Buendel aus mehreren Auerstabchen). Molekular liegt fast immer die PML-RARA-Fusion durch t(15;17) vor. Die rasche Diagnosesicherung ist lebensnotwendig wegen der Koagulopathie-Gefahr.",
        difficulty = 5,
        funFact = "Die APL war fruher die toedlichste AML-Subentitaet wegen schwerer Koagulopathie. Seit Einfuehrung von All-trans-Retinsaeure (ATRA) und Arsentrioxid (ATO) ist sie heute mit Heilungsraten ueber 90% die am besten behandelbare AML -- ein Paradebeispiel differenzierungsinduzierender Therapie."
    ),

    // Question 6 - CML und BCR-ABL
    Question(
        categoryId = 16,
        questionText = "Welche molekulare Konsequenz hat die BCR-ABL1-Fusion bei der CML bezueglich der Tyrosinkinaseaktivitaet?",
        answerA = "BCR-ABL1 hat im Vergleich zum normalen ABL1 identische Aktivitaet, wird aber ueberexprimiert",
        answerB = "BCR-ABL1 verliert die autoinhibitorische Kontrolle durch die SH3-Domaaene, resultiert in konstitutiv aktiver Tyrosinkinase mit verlagerter Lokalisation vom Kern ins Zytoplasma",
        answerC = "BCR-ABL1 aktiviert ausschliesslich den Ras/MAPK-Signalweg und hat keinen Einfluss auf PI3K/AKT",
        answerD = "BCR-ABL1 wirkt als dominant-negativer Inhibitor des normalen ABL1 und supprimiert die Haematopoese",
        correctAnswer = 1,
        explanation = "Im normalen ABL1-Protein haelt die SH3-Domaaene das Protein durch interne Bindung an die Linker-Region autoinhibiert. Im BCR-ABL1-Fusionsprotein verhindert die BCR-Sequenz diese Konformation, sodass die Kinase permanent aktiv ist. Zudem verlagert BCR das normalerweise kernlokalisierte ABL1 ins Zytoplasma, wo es STAT5, PI3K/AKT, Ras/MAPK und anti-apoptotische Signalwege aktiviert.",
        difficulty = 5,
        funFact = "Imatinib (Glivec/Gleevec) war 2001 das erste rational designte Kinasehemmstoff-Krebsmedikament und revolutionierte die CML-Therapie. Patienten mit CML in chronischer Phase erreichen heute eine nahezu normale Lebenserwartung -- in den 1980er Jahren lag die 5-Jahres-Ueberlebensrate unter 30%."
    ),

    // Question 7 - ALL-Behandlung
    Question(
        categoryId = 16,
        questionText = "Warum ist die ZNS-Prophylaxe ein essenzieller Bestandteil der ALL-Behandlung?",
        answerA = "Leukaemiezellen im ZNS produzieren neurotoxische Zytokine, die ohne Behandlung zu Hirnoedemen fuehren",
        answerB = "Das ZNS stellt ein pharmakologisches Sanctuary dar, da die Blut-Hirn-Schranke viele Zytostatika ausschliesst und residuelle Leukaemiezellen dort ueberleben und spaeter systemische Rezidive verursachen koennen",
        answerC = "Lymphoblasten haben einen besonderen Tropismus fuer Meningen und wandern aktiv ins ZNS aus",
        answerD = "Die ZNS-Prophylaxe ist nur bei T-ALL notwendig, bei B-Vorlaeufer-ALL ist sie obsolet",
        correctAnswer = 1,
        explanation = "Das ZNS ist ein immunprivilegiertes Sanctuary, in dem systemisch verabreichte Zytostatika unzureichende Konzentrationen erreichen. Ohne ZNS-Prophylaxe entwickeln bis zu 30-50% der ALL-Patienten ein ZNS-Rezidiv. Moderne Protokolle verwenden intrathekale Chemotherapie (Methotrexat, Cytarabin, Dexamethason) und/oder hochdosiertes systemisches Methotrexat statt kranieller Bestrahlung.",
        difficulty = 5,
        funFact = "Die historische kranielle Bestrahlung als ZNS-Prophylaxe war hochwirksam, hinterliess aber schwere Langzeitschaeden -- Kognitionsdefizite, Wachstumshormonmangel, Sekundaertumoren. Moderne ALL-Protokolle vermeiden Bestrahlung bei der Mehrzahl der Patienten durch intensive intrathekale und systemische Therapie."
    ),

    // --- LYMPHOME ---

    // Question 8 - Reed-Sternberg-Zellen
    Question(
        categoryId = 16,
        questionText = "Welche immunphaenotypische Signatur ist charakteristisch fuer die klassische Reed-Sternberg-Zelle beim Hodgkin-Lymphom?",
        answerA = "CD20+, CD79a+, BCL-2+, MYC+",
        answerB = "CD30+, CD15+, PAX5 schwach positiv, CD45 negativ, CD20 negativ",
        answerC = "CD3+, CD4+, CD30+, ALK+",
        answerD = "CD138+, CD38+, CD56+, Kappa oder Lambda leichtkettenbeschraenkt",
        correctAnswer = 1,
        explanation = "Klassische Reed-Sternberg-Zellen exprimieren stets CD30 und CD15 und sind charakteristischerweise CD45-negativ -- ungewoehnlich fuer haematopoetische Zellen. PAX5 ist schwach positiv und beweist die B-Zell-Herkunft trotz Verlust anderer B-Zell-Marker wie CD20 und CD79a. Diese untypische Phaenotyperosion ist diagnostisch wegweisend.",
        difficulty = 5,
        funFact = "Reed-Sternberg-Zellen machen weniger als 1% der Tumorzellmasse beim Hodgkin-Lymphom aus -- der groesste Teil des Tumors besteht aus reaktiven T-Zellen, Eosinophilen, Plasmazellen und Fibroblasten. RS-Zellen manipulieren aktiv ihre Microenvironment-Umgebung zur eigenen Protektion."
    ),

    // Question 9 - Ann-Arbor-Staging
    Question(
        categoryId = 16,
        questionText = "Was definiert Stadium IV im Ann-Arbor-Stagingsystem fuer Lymphome?",
        answerA = "Befall von mehr als 4 Lymphknotenregionen beidseits des Zwerchfells",
        answerB = "Diffuser oder disseminierter Befall eines oder mehrerer extralymphatischer Organe, unabhaengig vom Lymphknotenstatus",
        answerC = "Befall des Knochenmarks, der Leber oder Lunge, ausschliesslich bei Non-Hodgkin-Lymphomen",
        answerD = "Jeder Befall des zentralen Nervensystems oder des Knochenmarks unabhaengig von anderen Befunden",
        correctAnswer = 1,
        explanation = "Im Ann-Arbor-System bezeichnet Stadium IV den diffusen extralymphatischen Organbefall (z.B. Knochenmark, Leber, Lunge, Haut). Wichtig: Ein einzelner extralymphatischer Herd, der direkt von einer befallenen Lymphknotenregion ausgeht, wird als E-Stadium (extralymphatisch) klassifiziert, nicht automatisch als Stadium IV.",
        difficulty = 5,
        funFact = "Das Ann-Arbor-System wurde 1971 bei einer Konsensuskonferenz in Ann Arbor, Michigan entwickelt und seither mehrfach modifiziert. Fuer das DLBCL wird es heute durch den IPI (International Prognostic Index) ergaenzt, der Alter, Leistungsstatus, LDH, extralymphatische Herde und Stadium kombiniert."
    ),

    // Question 10 - Hodgkin vs. NHL
    Question(
        categoryId = 16,
        questionText = "Welcher histologische Subtyp des klassischen Hodgkin-Lymphoms hat die guenstigste Prognose?",
        answerA = "Lymphozytenreicher Typ (lymphocyte-rich)",
        answerB = "Nodulare Sklerose (NS)",
        answerC = "Mischtyp (mixed cellularity)",
        answerD = "Lymphozytenarmer Typ (lymphocyte-depleted)",
        correctAnswer = 0,
        explanation = "Der lymphozytenreiche Subtyp (LR-cHL) zeigt die guenstigste Prognose mit einer 5-Jahres-Ueberlebensrate von ueber 90%. Er ist selten (ca. 5% aller cHL), tritt meist in fruehen Stadien auf und ist haefig CD20-positiv. Die nodulare Sklerose ist der haeufigste Subtyp (ca. 70%), der lymphozytenarme Typ hat die schlechteste Prognose.",
        difficulty = 5,
        funFact = "Die nodulare Sklerose des Hodgkin-Lymphoms ist der einzige Lymphom-Subtyp, der haeufiger bei Frauen als bei Maennern vorkommt und einen deutlichen Gipfel im jungen Erwachsenenalter zeigt -- typischerweise mit mediastinalem Befall."
    ),

    // --- MYELOPROLIFERATIVE NEOPLASIEN ---

    // Question 11 - JAK2 V617F
    Question(
        categoryId = 16,
        questionText = "Welche biochemische Konsequenz hat die JAK2-V617F-Mutation bei myeloproliferativen Neoplasien?",
        answerA = "JAK2-V617F verliert die Faehigkeit, STAT-Proteine zu phosphorylieren und schaltet den JAK-STAT-Signalweg ab",
        answerB = "JAK2-V617F hebt die autoinhibitorische Funktion der Pseudokinase-Domaaene (JH2) auf und fuehrt zu konstitutiver Kinaseaktivierung mit zytokin-unabhaengiger Haematopoese",
        answerC = "JAK2-V617F bewirkt die Bindung an den Erythropoietin-Rezeptor ohne EPO-Liganden durch Konformationsaenderung des Rezeptors",
        answerD = "JAK2-V617F aktiviert ausschliesslich den PI3K/mTOR-Signalweg und hat keinen Einfluss auf STAT5",
        correctAnswer = 1,
        explanation = "Valin an Position 617 der Pseudokinase-Domaaene (JH2) ist normalerweise essentiell fuer die autoinhibitorische Konformation von JAK2. Die V617F-Substitution (Valin durch Phenylalanin) destabilisiert diese Autoinhibition, sodass JAK2 permanent aktiv bleibt und zytokinunabhaengig STAT3, STAT5 sowie Ras/MAPK und PI3K/AKT aktiviert.",
        difficulty = 5,
        funFact = "JAK2-V617F wurde 2005 gleichzeitig von vier unabhaengigen Gruppen entdeckt -- ein seltenes Ereignis in der Wissenschaft. Die Mutation ist bei Polycythaemia vera in >95%, bei essentieller Thrombozythaemie und Myelofibrose in 50-60% der Faelle nachweisbar."
    ),

    // Question 12 - Polycythaemia vera
    Question(
        categoryId = 16,
        questionText = "Welcher diagnostische Grenzwert fuer den Haematokrit ist laut WHO-Kriterien (2016) fuer die Diagnose Polycythaemia vera entscheidend?",
        answerA = "Hkt > 48% bei Frauen, > 52% bei Maennern, kombiniert mit erniedrigtem Serum-EPO",
        answerB = "Hkt > 45% (Maenner) oder > 42% (Frauen) plus JAK2-Mutation oder Nachweis erythrozytaerer Klonalitaet",
        answerC = "Haemoglobin > 16,5 g/dl (Frauen) oder > 18,5 g/dl (Maenner) ODER Hkt > 49% (Frauen) oder > 52% (Maenner)",
        answerD = "Erythrozytenmasse > 125% des Normwerts bei Isotopenbestimmung, JAK2-Mutation allein genuegt nicht",
        correctAnswer = 2,
        explanation = "Laut WHO 2016 gilt als Hauptkriterium Hb > 16,5 g/dl bei Frauen (> 18,5 g/dl bei Maennern) ODER Hkt > 49% bei Frauen (> 52% bei Maennern) ODER erhoehte Erythrozytenmasse. Zweites Hauptkriterium ist die Knochenmarksbiopsie mit Hyperzellularitaet, drittes Hauptkriterium die JAK2-Mutation. Nebenkriterium ist ein erniedrigtes Serum-EPO.",
        difficulty = 5
    ),

    // Question 13 - Myelofibrose
    Question(
        categoryId = 16,
        questionText = "Welcher pathophysiologische Mechanismus fuehrt bei der primaaeren Myelofibrose zur Knochenmarksfibrose?",
        answerA = "Direkte Mutation der Stromazellen des Knochenmarks durch das klonale Haematopoesemuster",
        answerB = "Ueberexpression von TGF-beta und PDGF durch pathologische Megakaryozyten stimuliert reaktive Fibroblasten zur exzessiven Kollagendeposition",
        answerC = "Autoantikoeprper gegen Knochenmarksstromazellen verursachen entzuendungsbedingte Fibrosierung",
        answerD = "JAK2-V617F direkt phosphoryliert und aktiviert Kollagensynthasegene in Fibrozyten",
        correctAnswer = 1,
        explanation = "Die Fibrose beim PMF ist reaktiv, nicht klonal. Pathologische Megakaryozyten und Monozyten setzen massiv TGF-beta1, PDGF, VEGF und FGF frei. Diese Zytokine stimulieren CD34-negative, polyklonale Fibroblasten zu exzessiver Kollagen-I- und -III-Deposition. Kollagen-retikulaere Fasern verdraengen die normale Haematopoese ins Blut (leukoerythoblastisches Blutbild).",
        difficulty = 5,
        funFact = "Ruxolitinib, ein JAK1/JAK2-Inhibitor, reduziert die Milzgroesse und Symptome der Myelofibrose erheblich, kann aber die Fibrose selbst nur begrenzt revidieren -- ein Hinweis darauf, dass epigenetische Veraenderungen in den Fibroblasten eine selbsterhaltende Eigendynamik entwickeln."
    ),

    // --- FORTGESCHRITTENE GENETIK ---

    // Question 14 - CRISPR-Cas9
    Question(
        categoryId = 16,
        questionText = "Welche Eigenschaft der Cas9-Nuklease ermoeglicht die zielgenaue DNS-Spaltung bei CRISPR-Cas9?",
        answerA = "Cas9 erkennt autonome Restriktionssequenzen im Zielgenom und schneidet bei bekannten Palindromen",
        answerB = "Cas9 bildet einen Komplex mit einer guide-RNA (sgRNA), deren 20-nt-Spacer-Sequenz komplementaer zur Ziel-DNS ist, und benoetigt zusaetzlich ein PAM-Motiv (NGG bei SpCas9) fuer die Spaltung",
        answerC = "Cas9 erkennt epigenetisch markierte Histonregionen und schneidet praeferentiell in offener Chromatinstruktur",
        answerD = "Cas9 benoetigt keine guide-RNA, wenn das Zielgen aktiv transkribiert wird, da RNA-Polymerase als Leitstruktur dient",
        correctAnswer = 1,
        explanation = "SpCas9 (aus Streptococcus pyogenes) wird durch eine synthetische sgRNA geleitet, die aus crRNA (CRISPR-RNA, 20 nt Targeting-Sequenz) und tracrRNA besteht. Nach komplementaerer Basenpaarung mit der Ziel-DNS benoetigt Cas9 ein unmittelbar 3'-gelegenes PAM (Protospacer Adjacent Motif, 5'-NGG-3'), um einen Doppelstrangbruch einzuleiten. HNH-Domaaene schneidet den komplementaeren, RuvC-Domaaene den nicht-komplementaeren Strang.",
        difficulty = 5,
        funFact = "Die erste CRISPR-basierte Gentherapie (Casgevy von Vertex/CRISPR Therapeutics) wurde Ende 2023 von der FDA fuer Sichelzellanhaemie und Thalassaemie zugelassen. Sie reaktiviert foetales Haemoglobin durch Inaktivierung des BCL11A-Repressors -- eine Heilung, die nach einem einzigen Eingriff moeglich ist."
    ),

    // Question 15 - Antisense-Oligonukleotide
    Question(
        categoryId = 16,
        questionText = "Ueber welchen Hauptmechanismus wirken Antisense-Oligonukleotide (ASO) der RNase-H-Klasse?",
        answerA = "ASOs blockieren die Ribosomenbindung an mRNA sterisch und verhindern Translation ohne RNA-Abbau",
        answerB = "ASOs binden als komplementaere Sequenz an Ziel-mRNA und rekrutieren RNase H1, die den RNA-Anteil des DNA/RNA-Heteroduplexes abbaut",
        answerC = "ASOs werden in den RISC-Komplex eingebaut und wirken wie siRNA durch Ago2-vermittelte Spaltung",
        answerD = "ASOs binden an den Spleissosom-Komplex und foerdern exon-skipping in allen Faellen",
        correctAnswer = 1,
        explanation = "RNase-H-aktive ASOs sind DNA-Gapmer: Phosphorothioat-modifizierte DNA-Sequenzen bilden nach Hybridisierung mit Ziel-mRNA ein DNA/RNA-Heteroduplex. Kernstaendige RNase H1 erkennt diesen Heteroduplex und hydrolysiert den RNA-Strang, waehrend das ASO unveraendert bleibt und weitere mRNA-Molekuele binden kann -- ein katalytischer Wirkmechanismus.",
        difficulty = 5,
        funFact = "Nusinersen (Spinraza) ist ein ASO, das Exon-7-Inklusion in das SMN2-Praesident-mRNA foerdert und damit die spinale Muskelatrophie behandelt -- nicht durch RNase-H, sondern durch Spleiss-Modulation. Die intrathekale Injektion alle 4 Monate kostet circa 750.000 USD pro Jahr, was es zu einem der teuersten Medikamente der Welt macht."
    ),

    // Question 16 - RNA-Interferenz
    Question(
        categoryId = 16,
        questionText = "Was ist der biochemische Unterschied zwischen siRNA und miRNA in Bezug auf ihren Wirkmechanismus?",
        answerA = "siRNA wirkt ausschliesslich im Kern, miRNA ausschliesslich im Zytoplasma",
        answerB = "siRNA ist vollstaendig komplementaer zur Ziel-mRNA und fuehrt zu Ago2-vermitteltem Strangschnitt; miRNA ist meist unvollstaendig komplementaer und bewirkt translatorische Repression und beschleunigten mRNA-Abbau ohne praezisen Schnitt",
        answerC = "siRNA benoetigt den RISC-Komplex, miRNA wirkt RISC-unabhaengig durch direkte Ribosomenbindung",
        answerD = "siRNA stammt aus exogenen viralen Quellen, miRNA aus genomischen Sequenzen -- ihr Wirkmechanismus ist jedoch identisch",
        correctAnswer = 1,
        explanation = "Beide verwenden den RISC-Komplex mit Ago2 als Kernnuklease. siRNA hybridisiert vollstaendig mit der Ziel-mRNA, was Ago2 zur Endonukleolyse aktiviert (Slicer-Aktivitaet). miRNA hat typischerweise eine unvollstaendig komplementaere Seed-Sequenz (nt 2-8), was die Slicer-Aktivitaet verhindert; stattdessen wird mRNA-Deadenylierung, Dekapping und translatorische Blockade durch GW182/TNRC6-Proteine ausgeloest.",
        difficulty = 5,
        funFact = "Patisiran (Onpattro) war 2018 das erste zugelassene siRNA-Therapeutikum, das transthyretin-assoziierte hereditaere Amyloidose behandelt. Es verwendet Lipid-Nanopartikel fuer die hepatische Zielsteuerung -- eine Durchbruchstechnologie fuer RNA-Therapeutika."
    ),

    // --- PHARMAKOGENOMIK ---

    // Question 17 - DPYD und 5-FU
    Question(
        categoryId = 16,
        questionText = "Welche klinische Konsequenz hat ein DPYD*2A-Polymorphismus (IVS14+1G>A) fuer Patienten unter 5-Fluorouracil-Therapie?",
        answerA = "Verminderter Abbau von 5-FU durch Dihydropyrimidin-Dehydrogenase-Mangel fuehrt zu toxischen Plasmakonzentrationen mit Mukositis, Neutropenie und potentiell letalen Komplikationen",
        answerB = "Erhoehte DPYD-Aktivitaet beschleunigt den 5-FU-Abbau, sodass hoehere Dosen benoetigt werden",
        answerC = "DPYD*2A bewirkt eine bevorzugte Aktivierung des anabolischen Wegs zu FUTP statt zum toxischen FdUMP",
        answerD = "DPYD*2A hat nur bei intravenoeser Gabe von 5-FU klinische Relevanz, nicht bei oralen Fluoropyrimidinen wie Capecitabin",
        correctAnswer = 0,
        explanation = "DPYD katalysiert den ersten und geschwindigkeitsbestimmenden Abbauschritt von 5-FU. DPYD*2A (splice-site-Mutation) fuehrt zu Exon-14-Skipping und nicht-funktionellem Enzym. Heterozygote (ca. 1-2% der Bevoelkerung) haben eine 50%-reduzierte Enzymaktivitaet und benoetigen Dosisreduktion um 50%; bei Homozygoten ist 5-FU absolut kontraindiziert.",
        difficulty = 5,
        funFact = "In Europa wurde 2020 ein regulatorisches Requirement zur DPYD-Testung vor Fluoropyrimidin-Therapie eingefuehrt. Etwa 80 Leben pro Jahr koennen in Deutschland allein durch Pre-treatment-DPYD-Genotypisierung gerettet werden -- ein einmaliger Gentest von circa 50 Euro verhindert potenziell toedliche Toxizitaet."
    ),

    // Question 18 - UGT1A1 und Irinotecan
    Question(
        categoryId = 16,
        questionText = "Welcher Metabolit von Irinotecan wird durch UGT1A1 inaktiviert, und was passiert bei UGT1A1*28-Homozygotie?",
        answerA = "Irinotecan selbst wird durch UGT1A1 direkt glukuronidiert; bei *28-Homozygotie akkumuliert Irinotecan im Plasma",
        answerB = "SN-38 (aktiver Metabolit) wird durch UGT1A1 zu SN-38-Glukuronid inaktiviert; bei *28-Homozygotie (7 TA-Repeats im Promoter) ist UGT1A1-Expression reduziert, SN-38 akkumuliert und verursacht Neutropenie und schwere Diarrhoe",
        answerC = "APC (Aminopentansaeure-Carbamat), eine inaktive Vorstufe, wird durch UGT1A1 in SN-38 umgewandelt; bei *28 entsteht mehr SN-38",
        answerD = "SN-38G (Glukuronid) wird durch UGT1A1 zurueck in SN-38 gespalten; *28 foerdert daher Detoxifikation",
        correctAnswer = 1,
        explanation = "Irinotecan wird durch Carboxylesterasen zu SN-38 (7-Ethyl-10-Hydroxycamptothecin) aktiviert, einem potenten Topoisomerase-I-Hemmer. UGT1A1 glukuronidiert SN-38 im Darm und der Leber. UGT1A1*28 hat 7 statt 6 TA-Repeats im TATA-Box-Promoter, was Expression um 70% reduziert. Bei *28/*28-Homozygoten (ca. 10-15% der Kaukasier) kumuliert SN-38 und verursacht lebensbedrohliche Toxizitaet.",
        difficulty = 5,
        funFact = "Die FDA empfiehlt seit 2005 UGT1A1-Genotypisierung vor Hochdosis-Irinotecan. Interessanterweise haben *28-Homozygote nicht nur mehr Toxizitaet, sondern auch tendenziell bessere Remissionsraten -- ein klassischer Zielkonflik zwischen Wirksamkeit und Toxizitaet in der Praezisionsmedizin."
    ),

    // --- TUMOR-MICROENVIRONMENT ---

    // Question 19 - Krebsassoziierte Fibroblasten
    Question(
        categoryId = 16,
        questionText = "Welche Hauptfunktionen haben Cancer-Associated Fibroblasts (CAFs) im Tumor-Microenvironment?",
        answerA = "CAFs sind ruhende Fibroblasten, die nur passive Geruest-Funktionen ausueben und keine aktive Rolle in der Tumorprogression spielen",
        answerB = "CAFs sezernieren pro-tumorigene Wachstumsfaktoren (HGF, FGF, EGF-like), produzieren Extrazellularmatrix-Komponenten, foerdern Angiogenese, supprimieren Immunantwort und vermitteln Chemotherapieresistenz",
        answerC = "CAFs sind stets Tumorsuppressoren, die Tumorwachstum durch Zytokinsekretion begrenzen",
        answerD = "CAFs transdifferenzieren direkt in Tumorzellen und bilden die mesenchymale Komponente des Tumors",
        correctAnswer = 1,
        explanation = "CAFs entstehen durch Aktivierung normaler Fibroblasten, Pericyten, Epithelzellen (EMT) oder mesenchymaler Stammzellen. Aktivierte CAFs (myofibroblastoider Phaenotyp, alpha-SMA+) foerdern Tumorprogression durch parakrine Signale, desmoplastische Stroma-Remodellierung, Immun-Suppression via TGF-beta, IL-10, PGE2 und mechanische Barrieren, die Medikamente vom Tumor fernhalten.",
        difficulty = 5,
        funFact = "Beim Pankreaskarzinom machen CAFs bis zu 80-90% der Tumormasse aus -- dieses extreme desmoplastische Milieu ist ein Hauptgrund fuer die schlechte Chemotherapie-Penetration und damit fuer die extrem schlechte Prognose mit medianer Ueberlebenszeit unter 12 Monaten."
    ),

    // Question 20 - Tumor-assoziierte Makrophagen
    Question(
        categoryId = 16,
        questionText = "Welcher Aktivierungsphaenotyp von Tumor-assoziierten Makrophagen (TAMs) ist mit schlechterer Prognose assoziiert und warum?",
        answerA = "M1-polarisierte TAMs, weil sie pro-inflammatorische Zytokine sezernieren, die Tumorzellen direkt lysieren",
        answerB = "M2-polarisierte TAMs, weil sie anti-entzuendlich wirken, Angiogenese foerdern (VEGF, MMP-9), Immunsuppression vermitteln (IL-10, TGF-beta, Arginase-1) und Tumorinvasion und -metastasierung foerdern",
        answerC = "Unreife Monozyten (M0), weil sie als Feederzellen fuer Tumorzellen dienen und Wachstumsfaktoren produzieren",
        answerD = "M1-TAMs mit hoher PD-L1-Expression, weil sie CD8-T-Zellen hemmen",
        correctAnswer = 1,
        explanation = "M2-polarisierte TAMs werden im Tumor durch IL-4, IL-13, IL-10, TGF-beta und M-CSF induziert. Sie sezernieren VEGF und MMP-9 (Angiogenese und Matrixabbau), schaffen immunsuppressive Microenvironment durch IL-10, TGF-beta und PD-L1-Expression, und foerdern EMT/Invasion. Hohe TAM-Dichte korreliert mit schlechterer Prognose in Brust-, Ovarial-, Blasen- und Lungenkarzinom.",
        difficulty = 5,
        funFact = "CD47 auf Tumorzellen sendet ein 'Don't eat me'-Signal an Makrophagen via SIRPalpha-Rezeptor. Anti-CD47-Antikoeprper (z.B. Magrolimab) kombiniert mit Rituximab zeigen vielversprechende klinische Ergebnisse, da sie TAMs zur Phagozytose der Tumorzellen umprogrammieren."
    ),

    // Question 21 - Angiogenese
    Question(
        categoryId = 16,
        questionText = "Welches Prinzip beschreibt den 'Angiogenic Switch' in der Tumorprogression?",
        answerA = "Tumorzellen aktivieren Angiogenese erst bei einer kritischen Groesse von 1 cm, wenn die Diffusionsdistanz fuer Sauerstoff ueberschritten wird",
        answerB = "Ein Kippmoment, bei dem pro-angiogene Faktoren (VEGF, FGF, PDGF, Angiopoietin-2) anti-angiogene Faktoren (Thrombospondin-1, Angiostatin, Endostatin) ueberwiegen und Tumorvaeskularisierung beginnt",
        answerC = "Angiogenese wird ausschliesslich durch Hypoxie und HIF-1alpha ausgeloest; ohne Hypoxie findet kein Switch statt",
        answerD = "Der Switch beschreibt die Umstellung von Vaskulogenese (Stammzellen) auf echte Angiogenese (Sprouting) im wachsenden Tumor",
        correctAnswer = 1,
        explanation = "Judah Folkman postulierte den Angiogenic Switch als Balance-Kippmoment zwischen Pro- und Anti-Angiogenia. Praevaskulaere Tumoren bleiben auf ca. 1-2 mm Groesse beschraenkt (Diffusionslimit). Wenn Driver-Mutationen (z.B. in RAS, TP53) oder Hypoxie HIF-1alpha aktivieren, steigt VEGF-Sekretion, sinkt TSP-1, und Endothelzellen beginnen zu sprossen -- ab diesem Moment kann der Tumor unbegrenzt wachsen.",
        difficulty = 5,
        funFact = "Bevacizumab (Avastin), erster zugelassener VEGF-Antikoeprper, war Folkmanns Traum, wurde aber erst nach seinem Tod 2009 von der FDA fuer kolorektales Karzinom zugelassen. Er starb kurz nach Einreichung des Artikels -- und konnte den Triumph seines Lebenswerks nicht mehr voll erleben."
    ),

    // --- HAEMATOLOGIE ---

    // Question 22 - Aplastische Anaemie
    Question(
        categoryId = 16,
        questionText = "Welcher Immunmechanismus liegt der erworbenen schweren aplastischen Anaemie (SAA) zugrunde?",
        answerA = "B-Zell-vermittelte Autoantikoeprper gegen haematopoetische Stammzellen aktivieren Komplement und loesen Haemolyse aus",
        answerB = "Oligoklonale, autoreaktive T-Zellen (vorwiegend CD8+ Th1-Typ) zerstoeren haematopoetische Stammzellen durch IFN-gamma und Fas/FasL-vermittelte Apoptose",
        answerC = "NK-Zellen mit fehlender KIR-Expression greifen MHC-negative haematopoetische Vorlaeufer an",
        answerD = "Regulatorische T-Zellen werden aktiviert und supprimieren normal funktionierende Stammzellen durch IL-10",
        correctAnswer = 1,
        explanation = "Bei der SAA aktivieren wahrscheinlich virale Antigene oder autoreaktive T-Zellen eine klonale CD8+/CD4+ Th1-Antwort. Aktivierte T-Zellen produzieren IFN-gamma und TNF-alpha, die Fas-Ligand auf haematopoetischen Stammzellen hochregulieren und direkte Fas/FasL-vermittelte Apoptose ausloesen. Immunsuppression mit ATG + Cyclosporin A spricht in ca. 60-70% der Patienten an, was die Immunpathogenese beweist.",
        difficulty = 5,
        funFact = "Eltrombopag (ein TPO-Rezeptor-Agonist) wurde urspruenglich fuer Thrombozytopenie entwickelt, zeigte aber ueberraschend haematopoetische Stammzellstimulation bei SAA. Kombiniert mit ATG/Cyclosporin steigert es die Ansprechrate auf ueber 80% -- ein Paradebeispiel fuer Drug Repurposing."
    ),

    // Question 23 - PNH (Paroxysmale Nokturnale Haemoglobinurie)
    Question(
        categoryId = 16,
        questionText = "Welcher genetische Defekt liegt der paroxysmalen nokturalen Haemoglobinurie (PNH) zugrunde?",
        answerA = "Keimbahnmutation im CD55-Gen (DAF) mit Verlust des Komplement-Inaktivatorproteins",
        answerB = "Somatische Mutation im PIGA-Gen in der haematopoetischen Stammzelle, das zu Defizit von GPI-verankerten Proteinen (CD55, CD59) und unkontrollierter Komplementaktivierung fuehrt",
        answerC = "Deletion von CD59 auf Erythrozyten durch somatische chromosomale Aberration auf Chromosom 11p13",
        answerD = "Autoimmune Antikoeprper gegen GPI-Anker, die CD55 und CD59 von der Erythrozytenmembran entfernen",
        correctAnswer = 1,
        explanation = "PIGA kodiert Phosphatidylinositolglycan Klasse A, eine Glykosyltransferase fuer die erste Synthesestufe des GPI-Ankers. Somatische PIGA-Mutation in der haematopoetischen Stammzelle fuehrt zu GPI-Defizit auf allen Nachkommen, damit zu Fehlen von CD55 (DAF) und CD59 (Protectin). CD59 inhibiert normalerweise den Membrane Attack Complex (C5b-9); ohne CD59 lysiert Komplement die Erythrozyten.",
        difficulty = 5,
        funFact = "Eculizumab (Soliris), ein Anti-C5-Antikoeprper, war bei PNH eines der ersten Orphan Drugs mit spektakulaerem Effekt und revolutionierte die Behandlung. Sein Jahrespreis von 500.000 USD loeste eine globale Debatte ueber Arzneimittelpreisgestaltung aus und wurde zum Symbol fuer die Pharmaindustrie-Preisdiskussion."
    ),

    // --- KNOCHENMARKTRANSPLANTATION ---

    // Question 24 - GVHD
    Question(
        categoryId = 16,
        questionText = "Welche drei Bedingungen sind laut Billingham (1966) fuer die Entstehung von Graft-versus-Host-Disease (GvHD) notwendig?",
        answerA = "HLA-Mismatch, Lymphozyten im Transplantat, reduzierte Konditionierung",
        answerB = "Transplantierte Zellen muessen immunkompetent sein; Empfaenger muss histoinkompatibel sein; Empfaenger muss immunologisch unfaehig sein, das Transplantat abzustossen",
        answerC = "Vorhandensein von T-Zellen, B-Zellen und NK-Zellen im Transplantat mit unterschiedlichen HLA-Allelen",
        answerD = "Engraftment von haematopoetischen Stammzellen, Immunrekonstitution und Chimerismusnachweis im Empfaenger",
        correctAnswer = 1,
        explanation = "Billinghams klassisches Trias: (1) Das Transplantat muss immunologisch kompetente Zellen enthalten (v.a. T-Zellen). (2) Empfaenger-Histoinkompatibilitaet muss vorliegen (allogene Transplantation). (3) Der Empfaenger muss immunologisch tolerant gegenueber dem Transplantat sein (keine Abstossungsreaktion). Fehlt eine der drei Bedingungen, entsteht keine GvHD.",
        difficulty = 5,
        funFact = "Paradoxerweise ist GvHD nicht nur schaedlich -- der Graft-versus-Leukemia (GvL)-Effekt ist das Kernprinzip der allogenen Stammzelltransplantation. T-Zellen im Transplantat bekaempfen residuelle Leukaemiezellen. GvL und GvHD sind mechanistisch untrennbar verknuepft, was die Therapie zur dauerhaften Gratwanderung macht."
    ),

    // Question 25 - Konditionierungsregimes
    Question(
        categoryId = 16,
        questionText = "Was ist der Unterschied zwischen myeloablativer Konditionierung (MAC) und reduzierter Intensitaet (RIC) bei allogener Stammzelltransplantation?",
        answerA = "MAC verwendet ausschliesslich Ganzkoeperbestrahlung, RIC ausschliesslich Chemotherapie",
        answerB = "MAC fuehrt zu irreversibler Aplasie ohne Stammzelltransplantation (Dosen > Erholungslimit); RIC ist reversibel, basiert auf Immunsuppression, erfordert GvL-Effekt fuer Tumorkontrolle und ermoeglicht Transplantation aelterer Patienten",
        answerC = "RIC hat identische Turmorkontrolle wie MAC, aber geringere Toxizitaet bei allen Diagnosen",
        answerD = "MAC ist bei Patienten ueber 50 Jahre verboten; RIC ist die einzige Option fuer aeltere Patienten",
        correctAnswer = 1,
        explanation = "MAC (z.B. Busulfan/Cyclophosphamid, TBI 12 Gy) zerstoert irreversibel die Haematopoese und eliminiert Tumorzellen direkt durch Chemo/Radiotherapie. RIC (z.B. Fludarabin/Melphalan oder Fludarabin/2 Gy TBI) suppressiert das Immunsystem ausreichend fuer Engraftment, basiert aber hauptsaechlich auf dem immunologischen GvL-Effekt fuer Remission. RIC erlaubt Transplantation bei Patienten >55 Jahre oder Komorbiditaeten.",
        difficulty = 5
    ),

    // Question 26 - Engraftment
    Question(
        categoryId = 16,
        questionText = "Welcher Laborwert definiert konventionell den Tag des Engraftments nach allogener Stammzelltransplantation?",
        answerA = "Nachweis von >1% Spender-Chimerismus im peripheren Blut",
        answerB = "Erster von drei konsekutiven Tagen mit Neutrophilen > 0,5 x 10^9/l aus Spenderursprung",
        answerC = "Thrombozytenanstieg ueber 20 x 10^9/l ohne Transfusion fuer 7 aufeinanderfolgende Tage",
        answerD = "Vollstaendige Elimination des Empfaenger-Chimerismus im STR-Analyse",
        correctAnswer = 1,
        explanation = "Neutrophiles Engraftment ist definiert als erster von drei aufeinanderfolgenden Tagen mit einem absoluten Neutrophilenwert >= 0,5 x 10^9/l. Thrombozyten-Engraftment wird definiert als erster von sieben transfusionsfreien Tagen mit Thrombozyten >= 20 x 10^9/l. Neutrophil-Engraftment tritt typischerweise Tag 14-21 nach allogener Stammzelltransplantation auf.",
        difficulty = 5,
        funFact = "Die 'Day 0' der Transplantation ist Tag der Stammzellinfusion. Klinische Daten werden retrospektiv nach dem Engraftment-Tag benannt -- 'Day +100' ist ein klinisch wichtiger Meilenstein, da akute GvHD per Definition vor diesem Tag auftritt und chronische GvHD danach."
    ),

    // --- SELTENE KREBSARTEN ---

    // Question 27 - Mesotheliom
    Question(
        categoryId = 16,
        questionText = "Welcher molekulare Pathway ist beim malignen Pleuramesotheliom durch BAP1-Verlust besonders betroffen?",
        answerA = "BAP1-Verlust aktiviert den Wnt/beta-Catenin-Signalweg durch Akkumulation von beta-Catenin im Kern",
        answerB = "BAP1 (BRCA1-Associated Protein 1) ist eine Deubiquitylase; ihr Verlust fuehrt zu erhoehter H2Aub1-Ubiquitinierung mit globaler Polycomb-vermittelter Repression von Tumorsuppressorgenen und Dysregulation der DNA-Schadensantwort",
        answerC = "BAP1-Verlust supprimiert NF2 (Merlin) und aktiviert Hippo-Signalweg-Inhibition mit YAP/TAZ-Ueberexpression",
        answerD = "BAP1 reguliert ausschliesslich die mitochondriale Apoptose; sein Verlust blockiert Cytochrom-c-Freisetzung",
        correctAnswer = 1,
        explanation = "BAP1 ist eine nukleare Deubiquitylase, die H2AK119ub1 (monoubiquitiniertes Histon H2A) entfernt, das Polycomb Repressive Complex 1 (PRC1) setzt. BAP1-Verlust fuehrt zu Hypomethylierung von H3K27 und erhoehter H2Aub1-Dichte, verursacht epigenetische Stummschaltung von Differenzierungs- und Tumorsuppressorgenen. BAP1 ist in 50-60% aller malignen Mesotheliome mutiert.",
        difficulty = 5,
        funFact = "BAP1-Keimbahnmutationen definieren das BAP1-Tumorsyndrom: hereditaeres Mesotheliom, Uveal- und kutanes Melanom, Nierenzellkarzinom und andere Malignome. Betroffene haben ein 50-fach erhoehtes Mesotheliomrisiko -- selbst ohne Asbestexposition, was zeigt, dass BAP1 ein echtes Tumorsuppressorgen ist."
    ),

    // Question 28 - Cholangiokarziom
    Question(
        categoryId = 16,
        questionText = "Welche molekulare Aberration ist bei intrahepatischen Cholangiokarzinomen (iCCA) am haeufigsten und therapeutisch actionable?",
        answerA = "KRAS-Mutation (G12C) bei 40-50% der iCCA",
        answerB = "FGFR2-Fusionen und -Translokationen bei 10-20% der iCCA, therapierbar mit FGFR-Inhibitoren (Pemigatinib, Infigratinib)",
        answerC = "BRCA1/2-Mutationen bei 30% der iCCA mit Ansprechen auf PARP-Inhibitoren",
        answerD = "MET-Amplifikation bei 25% der iCCA, therapierbar mit Cabozantinib",
        correctAnswer = 1,
        explanation = "FGFR2-Fusionen (haeufig FGFR2-BICC1, FGFR2-AHCYL1 u.a.) finden sich bei 10-20% der iCCA und fuehren zu konstitutiver FGFR2-Kinaseaktivierung. Pemigatinib wurde 2020 als erster FGFR-Inhibitor fuer FGFR2-positive Cholangiokarzinome von der FDA zugelassen. Weitere therapeutisch relevante Alterationen: IDH1-Mutationen (Ivosidenib), BRAF V600E, HER2.",
        difficulty = 5,
        funFact = "Das Cholangiokarzinom galt lange als medikamentoes 'undruggable' mit medianer Ueberlebenszeit von unter 12 Monaten. Der Paradigmenwechsel durch molekulare Subtypisierung hat 2020-2022 gleich mehrere Erstlinien-Zulassungen ermoeglicht -- iCCA ist nun ein Leuchtturm-Beispiel fuer molekulare Praezisionstumortherapie."
    ),

    // Question 29 - GIST
    Question(
        categoryId = 16,
        questionText = "Welche Tyrosinkinasemutation liegt den meisten gastrointestinalen Stromatumoren (GIST) zugrunde, und wie beeinflusst dies die Therapiewahl?",
        answerA = "PDGFRA-D842V-Mutation bei 80% der GIST; Imatinib ist First-line-Therapie",
        answerB = "KIT-Exon-11-Mutation bei ca. 60-70% der GIST (haaeufigste), KIT-Exon-9 bei 10-15%; Imatinib bei Exon-11 (400 mg), hoehere Dosierung (800 mg) empfohlen bei Exon-9",
        answerC = "RET-Fusionen bei 50% der GIST mit Ansprechen auf Vandetanib",
        answerD = "BRAF-V600E bei 40% der GIST; Vemurafenib ist die bevorzugte Therapie",
        correctAnswer = 1,
        explanation = "KIT-Mutationen dominieren bei GIST: Exon 11 (Juxtamembrandomaene, ca. 60-70%) und Exon 9 (Extrazellulardomaene, ca. 10-15%). Exon-11-Mutationen sind sensitiver auf Imatinib 400 mg/d; fuer Exon-9 zeigen Studien besseres Ansprechen auf 800 mg/d. PDGFRA-D842V-Mutation (ca. 8%) ist primaer Imatinib-resistent -- dafuer wurde Avapritinib zugelassen.",
        difficulty = 5,
        funFact = "GIST war die erste solide Tumorentitaet, bei der Imatinib (urspruenglich fuer CML entwickelt) als molekular zielgerichtete Therapie eingesetzt wurde. Vor Imatinib war die mediane Ueberlebenszeit bei metastasiertem GIST ca. 18 Monate; heute leben Patienten mit reseziertem lokalem GIST und adjuvantem Imatinib ueber 10 Jahre rezidivfrei."
    ),

    // --- ERWEITERTE HAEMATOLOGIE ---

    // Question 30 - MDS-Klassifikation
    Question(
        categoryId = 16,
        questionText = "Welche Mutation in spleissosomalen Genen ist bei MDS am haeufigsten und hat prognostische Bedeutung?",
        answerA = "SF3B1-Mutation, assoziiert mit MDS mit Ringsideroblasten und guenstiger Prognose; Ansprechen auf Luspatercept",
        answerB = "U2AF1-Mutation, assoziiert mit normalem Karyotyp und neutraler Prognose",
        answerC = "SRSF2-Mutation, assoziiert mit Translokation t(11;14) und sehr guenstiger Prognose",
        answerD = "PRPF8-Mutation, assoziiert mit hypozellulaerem Knochenmark und aplastischer Anaaemie-aehnlichem Verlauf",
        correctAnswer = 0,
        explanation = "SF3B1 (Splicing Factor 3b Subunit 1) ist mit Abstand die haeufigste spleissosomale Mutation bei MDS (20-30% aller Faelle). SF3B1-mutiertes MDS ist fast immer mit Ringsideroblasten assoziiert (RS-MDS) und hat eine guenstige Prognose. Luspatercept (TGF-beta-Trapper) zeigt spezifisch bei SF3B1-mutierten RS-MDS hohe Ansprechrate auf Transfusionsunabhaengigkeit.",
        difficulty = 5,
        funFact = "Das MDS-Spleisosom -- SF3B1, SRSF2, U2AF1, ZRSR2 -- stellt die haeufigste Mutationsklasse beim MDS dar. SF3B1-Mutationen finden sich auch bei chronischer lymphatischer Leukaemie (CLL) und uvealem Melanom, was zeigt, dass Spleissosomale Dysregulation ein wiederkehrendes onkogenes Thema ist."
    ),

    // Question 31 - Haemostase-Genetik
    Question(
        categoryId = 16,
        questionText = "Welcher Mechanismus erklaert das erhoehte Thromboserisiko bei Faktor-V-Leiden-Mutation?",
        answerA = "Faktor-V-Leiden fuehrt zu erhoehter Faktor-V-Produktion durch Promoter-Hyperaktivierung",
        answerB = "Faktor-V-Leiden (R506Q) eliminiert die APC-Spaltungsstelle, sodass aktivierter Protein-C (APC) Faktor Va nicht mehr inaktivieren kann und Faktor Va persistent aktiv bleibt",
        answerC = "Faktor-V-Leiden hat erhoehte Thrombinaffinitaet und aktiviert Faktor X effizienter als Wildtyp",
        answerD = "Faktor-V-Leiden hemmt Antithrombin III und verlaengert die Halbwertszeit von Thrombin",
        correctAnswer = 1,
        explanation = "Arginin 506 ist die primaere Spaltungsstelle fuer APC (Activated Protein C). Die R506Q-Mutation (Arginin zu Glutamin) macht Faktor Va resistent gegenueber APC-Spaltung. Faktor Va bleibt persistiert und haelt den Prothrombinasekomplex dauerhaft aktiv -- hereditaere Thrombophilie durch APC-Resistenz. Heterozygote haben 5-8-fach, Homozygote 50-80-fach erhoehtes VTE-Risiko.",
        difficulty = 5,
        funFact = "Faktor-V-Leiden ist die haeufigste hereditaere Thrombophilie in der westlichen Bevoelkerung mit einer Haeufigkeit von 5% bei Kaukasiern, aber sehr selten bei Asiaten und Afrikanern. Die Mutation entstand wahrscheinlich vor etwa 30.000 Jahren in Europa und breitete sich durch evolutionaeren Vorteil bei haemorrhagischen Komplikationen aus."
    ),

    // Question 32 - Haemophilie-Therapie
    Question(
        categoryId = 16,
        questionText = "Wie wirkt Emicizumab (Hemlibra) bei Haemophilie A mit Faktor-VIII-Inhibitoren?",
        answerA = "Emicizumab ist ein rekombinanter Faktor VIII mit modifizierter Struktur, der Inhibitor-Antikoeprper umgeht",
        answerB = "Emicizumab ist ein bispezifischer Antikoeprper, der Faktor IXa und Faktor X gleichzeitig bindet und damit die brueckenbildende Kofaktor-Funktion von Faktor VIII funktionell nachahmt",
        answerC = "Emicizumab hemmt TFPI (Tissue Factor Pathway Inhibitor) und steigert den extrinsischen Koagulationsweg",
        answerD = "Emicizumab ist ein Anti-Inhibitor-Antikoeprper, der Faktor-VIII-Inhibitoren direkt neutralisiert",
        correctAnswer = 1,
        explanation = "Emicizumab ist ein bispezifischer IgG-Antikoeprper, der einen Arm gegen aktivierten Faktor IXa und einen anderen gegen Faktor X richtet. Indem er beide Faktoren raeumlich in Naehe bringt, ahmt er die Kofaktor-Funktion des Faktor VIIIa nach -- ohne selbst Faktor VIII zu sein und daher nicht von Anti-FVIII-Inhibitoren erkannt zu werden. Subkutan einmal woechentlich oder zweiwoechtlich verabreicht.",
        difficulty = 5,
        funFact = "Emicizumab hat bei Haemophilie-A-Patienten mit Inhibitoren die annualisierte Blutungsrate von >15 auf unter 2 reduziert -- ein dramatischer Durchbruch. Die subkutane Applikation anstelle intravenoeser Faktor-VIII-Infusionen hat die Lebensqualitaet revolutioniert, besonders fuer Kinder."
    ),

    // --- ERWEITERTE MOLEKULARE ONKOLOGIE ---

    // Question 33 - Telomere Biologie
    Question(
        categoryId = 16,
        questionText = "Welche Rolle spielen Telomere im Kontext der Tumorentstehung, und wie umgehen Krebszellen die replikative Seneszenz?",
        answerA = "Krebszellen reaktivieren TERT (Telomerase Reverse Transkriptase), um Telomere zu verlaengern; in ca. 85-90% durch TERT-Promoter-Mutationen oder -Amplifikation",
        answerB = "Krebszellen verlieren Telomere vollstaendig und ueberleben durch alternative chromosomale Endstrukturen",
        answerC = "Krebszellen supprimieren TRF2 (Telomeric Repeat-binding Factor 2) und aktivieren dadurch unbegrenzte Teilung",
        answerD = "Krebszellen aktivieren ALT (Alternative Lengthening of Telomeres) als einzigen Weg zur Telomerverlaengerung",
        correctAnswer = 0,
        explanation = "Telomerverkuerzung bei jeder Zellteilung fuehrt normalerweise zu replikativer Seneszenz. Krebszellen reaktivieren Telomerase (TERT + TERC) in ca. 85-90% aller Tumore durch Promoter-Mutationen (haeufigste: C228T und C250T in TERT-Promoter, die ETS-Bindungsstellen kreieren) oder Genamplifikation. Die restlichen 10-15% verwenden ALT (homologe Rekombination zwischen Telomeren).",
        difficulty = 5,
        funFact = "TERT-Promoter-Mutationen (C228T, C250T) sind besonders haeufig bei Glioblastom (ca. 80%), Melanom (ca. 70%) und Blasenkarzinom (ca. 60%) und gelten als diagnostische/prognostische Marker. Sie entstehen charakteristisch durch UV-induzierte C>T-Transitionen -- sichtbar an der Dipyrimidin-Signatur."
    ),

    // Question 34 - Epigenetische Therapie
    Question(
        categoryId = 16,
        questionText = "Welcher Mechanismus erklaert die Wirkung von Decitabin (5-Aza-2'-Desoxycytidin) bei MDS und AML?",
        answerA = "Decitabin inhibiert direkt DNMT3A und DNMT3B durch kompetitive Bindung an den S-Adenosylmethionin-Kofaktor",
        answerB = "Decitabin wird nach Einbau in die DNS kovalent an DNA-Methyltransferasen (DNMT1) gebunden, fuehrt zu deren Abbau und hypomethylierungsbedingte Reaktivierung stummgeschalteter Tumorsuppressorgene",
        answerC = "Decitabin inhibiert HDAC-Enzyme und foerdert Histonacetylierung, was zu offener Chromatinstruktur fuehrt",
        answerD = "Decitabin aktiviert TET2-Enzyme und foerdert aktive DNA-Demethylierung durch 5-Hydroxymethylcytosin",
        correctAnswer = 1,
        explanation = "Decitabin wird intrazelluluar zu 5-Aza-2'-Desoxycytidintriphosphat phosphoryliert und als falsche Cytidinbase in die DNS eingebaut. DNMT1 erkennt den hemimolyierten Tochterstrang und versucht die Methylierung zu kopieren, wird aber durch den Stickstoff in Position 5 des Azacytidinrings kovalent gefangen und dann durch den Proteasom-Weg abgebaut. Die resultierende Hypomethylierung reaktiviert stummgeschaltete Tumorsuppressorgene.",
        difficulty = 5,
        funFact = "Paradoxerweise wirkt Decitabin in niedrigen Dosen epigenetisch (Demethylierung, Genreaktivierung) und in hohen Dosen zytotoxisch (DNS-Schaeden, Apoptose). Die klinisch verwendeten niedrigen Dosierungen wurden urspruenglich als 'wirksam aber zu schwach' abgelehnt -- der Paradigmenwechsel zu epigenetischen Dosen revolutionierte die MDS-Therapie."
    ),

    // Question 35 - Liquid Biopsy
    Question(
        categoryId = 16,
        questionText = "Was ist der biologische Ursprung zirkulierender Tumor-DNS (ctDNA) im Plasma und welches technische Verfahren wird fuer Hochsensitivitaet benoetigt?",
        answerA = "ctDNA stammt ausschliesslich aus aktiv sekretierenden Tumorzellen durch aktiven Exportmechanismus; PCR-Standardmethodik genuegt",
        answerB = "ctDNA entsteht durch Apoptose und Nekrose von Tumorzellen (passive Freisetzung), bildet kurze Fragmente (150-170 bp); fuer Detektion bei < 0,1% MAF werden digitale PCR (ddPCR) oder Next-Generation-Sequencing mit fehlerkorrigierenden Verfahren (UMIs, CAPP-Seq) benoetigt",
        answerC = "ctDNA wird ausschliesslich von Tumorzellexosomen freigesetzt und hat identische Laenge wie genomische DNS",
        answerD = "ctDNA ist in allen Patientenplasmaproben in konstanter Menge vorhanden; die Tumorlast beeinflusst nur die Mutationshaeufigkeit",
        correctAnswer = 1,
        explanation = "ctDNA entsteht durch passive Freisetzung aus sterbenden Tumorzellen (Apoptose: Nucleosom-geschuetzte ~167-bp-Fragmente; Nekrose: zufaellig groessere Fragmente). Im Plasma ist ctDNA in geringen Mengen in einem Hintergrund normaler zell-freier DNS (cfDNA). Sensitivitaet von ddPCR (einzelne Molekueldetektierung) oder CAPP-Seq mit einzigartigen Molekuelidentifikatoren (UMIs) erlaubt MAF-Detektion bis 0,001%.",
        difficulty = 5,
        funFact = "Liquid Biopsy mittels ctDNA ist heute im klinischen Alltag angekommen: FDA-zugelassene Tests detektieren EGFR-Mutationen im Plasma bei NSCLC oder RAS/BRAF bei kolorektalem Karzinom. Die minimale Resterkrankung (MRD) kann durch ctDNA-Kinetik nach Therapie gemessen werden -- sinkende ctDNA korreliert mit Therapieansprechen oft Wochen vor radiologischem Nachweis."
    ),

    // Question 36 - Checkpoint-Inhibitoren Mechanismus
    Question(
        categoryId = 16,
        questionText = "Welcher biochemische Signalweg wird durch CTLA-4-Blockade (z.B. Ipilimumab) aktiviert?",
        answerA = "CTLA-4-Blockade hemmt PD-L1-Expression auf Tumorzellen und reaktiviert erschoepfte T-Zellen",
        answerB = "CTLA-4 kompetiert mit CD28 um B7-1/B7-2 (CD80/CD86) auf APCs; Ipilimumab blockiert CTLA-4, gibt B7-Liganden frei fuer CD28-Kostimulation, foerdert T-Zell-Priming und hemmt Treg-Funktion im Lymphknoten",
        answerC = "CTLA-4 ist ein direkter Inhibitor der TCR-Signalkaskade; seine Blockade aktiviert ZAP-70 und LAT konstitutiv",
        answerD = "Ipilimumab blockiert CTLA-4 auf Tumorzellen und verhindert deren Immunevasion durch T-Zell-Ligandenmaskierung",
        correctAnswer = 1,
        explanation = "CTLA-4 wird nach T-Zell-Aktivierung hochreguliert und konkurriert mit dem Aktivierungsrezeptor CD28 um B7-1 (CD80) und B7-2 (CD86) auf dendritischen Zellen. CTLA-4 hat hoehere Affinitaet und blockiert dadurch Kostimulation. Zudem aktiviert CTLA-4 auf Tregs deren suppressive Funktion. Ipilimumab blockiert CTLA-4, restauriert CD28-Kostimulation im lymphatischen Gewebe und schwaecht Treg-Suppression -- wirkt primaer in der Primingsphase.",
        difficulty = 5,
        funFact = "James Allison erhielt 2018 zusammen mit Tasuku Honjo (der PD-1 entdeckte) den Nobelpreis fuer Medizin fuer die Entdeckung der Checkpoint-Inhibition. Allisons Durchbruch war die Erkenntnis, dass CTLA-4 kein Gaspedal, sondern eine Bremse ist -- und dass das Loesenloesen dieser Bremse das Immunsystem Krebs bekaeampfen laesst."
    ),

    // Question 37 - Bispecific T-Cell Engager
    Question(
        categoryId = 16,
        questionText = "Welches Zielantigen auf T-Zellen binden Blinatumomab und andere BiTE-Antikoeprper, um Tumorzellen zu lysieren?",
        answerA = "CD3-epsilon-Kette; der CD3-Arm verbindet T-Zellen mit dem tumorantigenspezifischen Arm und induziert MHC-unabhaengige T-Zell-Aktivierung und Tumorzelllyse",
        answerB = "CD28-Kostimulationsrezeptor; BiTE-Antikoeprper foerdern T-Zell-Aktivierung durch B7-CD28-Ueberbrueckung",
        answerC = "CD45-Phosphatase; Blockade aktiviert T-Zellen durch Erhoehung des Phosphorylierungsgrads von TCR-Signalproteinen",
        answerD = "PD-1-Rezeptor; BiTE-Antikoeprper blockieren gleichzeitig den PD-1-Checkpoint und aktivieren T-Zellen",
        correctAnswer = 0,
        explanation = "Blinatumomab bindet mit einem ScFv-Arm an CD19 auf B-Zellen/B-ALL-Blasten und mit dem anderen an CD3-epsilon auf T-Zellen. Der physische Kontakt aktiviert T-Zellen MHC-unabhaengig (kein spezifischer T-Zell-Rezeptor benoetigt) und induziert perforin/granzyme-vermittelte Tumorzelllyse sowie Fas/FasL-Apoptose. BiTE steht fuer Bispecific T-Cell Engager.",
        difficulty = 5,
        funFact = "Blinatumomab (Blincyto) war das erste BiTE-Antikoeprper-Molekuel und das erste tumoragnostische Medikament basierend auf Zielantigen (CD19). Es wirkt in extrem geringen Konzentrationen (~15 pg/ml therapeutisch effektiv) und muss als Dauerinfusion verabreicht werden, da seine Halbwertszeit nur 2 Stunden betraegt."
    ),

    // Question 38 - ADC-Technologie
    Question(
        categoryId = 16,
        questionText = "Welche strukturellen Komponenten bestimmen die therapeutische Breite eines Antibody-Drug Conjugates (ADC)?",
        answerA = "Nur die Antikoeprper-Spezifitaet; das Toxin und der Linker haben keinen Einfluss auf das Sicherheitsprofil",
        answerB = "Antikoeprper-Spezifitaet und Affinitaet (Targeting), Linker-Stabilitaet (hydrolytisch stabil vs. kleaverbar), Drug-to-Antibody-Ratio (DAR), Toxin-Wirkstaerke und -Selektivitaet, sowie intrazellulaere Freisetzungskinetik",
        answerC = "Die Laenge des Linkers bestimmt ausschliesslich die therapeutische Breite; kurze Linker sind sicherer",
        answerD = "Nur das Toxin ist relevant; der Antikoeprper dient lediglich als inerter Traeger ohne pharmakodynamische Eigenschaft",
        correctAnswer = 1,
        explanation = "ADC-Wirksamkeit und -Sicherheit werden durch alle Komponenten beeinflusst: (1) Antikoeprper: Zielspezifitaet, Internalisierungseffizienz, Fc-Funktion. (2) Linker: Stabilitaet im Blutkreislauf (vermeidet Toxin-Off-target), Spaltbarkeit im Tumor (pH, Kathepsin, Disulfid). (3) Toxin (Payload): Potenz (IC50), Wirkungsmechanismus, hydrophile Eigenschaften. (4) DAR: zu hoch fuehrt zu Aggregation und schneller Clearance.",
        difficulty = 5,
        funFact = "Trastuzumab-Deruxtecan (T-DXd/Enhertu) mit topoisomerase-I-Hemmer-Payload und spaltbarem Tetrapeptid-Linker hat beim HER2-positiven Mammakarzinom dramatisch ueberlegene Ergebnisse gegenueber herkoemmlicher Chemotherapie gezeigt. Sein 'bystander effect' (freigesetztes Toxin toetet auch benachbarte HER2-niedrige Zellen) ermoeglicht Wirkung auch bei HER2-low-Tumoren."
    ),

    // Question 39 - CAR-T-Zelltherapie
    Question(
        categoryId = 16,
        questionText = "Welche kostimulatorische Domaaene unterscheidet CD19-CAR-T-Zellen der zweiten Generation (4-1BB vs. CD28) und welche klinischen Konsequenzen hat diese Wahl?",
        answerA = "CD28-Domaaene foerdert staerkere Initialaktivierung und kurze, intensive Proliferation; 4-1BB-Domaaene foerdert Langzeit-Persistenz, Gedaechtnis-T-Zell-Differenzierung und metabolische Fitness (oxidative Phosphorylierung)",
        answerB = "4-1BB-Domaaene fuehrt zu staerkerer Zytokin-Freisetzung und hoeherem CRS-Risiko im Vergleich zu CD28",
        answerC = "CD28-Domaaene ist bei B-ALL ueberlegen; 4-1BB-Domaaene ist bei diffusem grosszelligem B-Zell-Lymphom (DLBCL) ueberlegen -- gegenseitig austauschbar",
        answerD = "Beide Domaaenen haben in grossen klinischen Studien identische Langzeitergebnisse gezeigt; die Wahl ist klinisch irrelevant",
        correctAnswer = 0,
        explanation = "CD28-basierte CARs (z.B. Axicabtagene Ciloleucel) zeigen rasche, intensive Expansion mit Gipfel nach 7-14 Tagen, kuerzere Persistenz und hoeheres CRS/ICANS-Risiko. 4-1BB-basierte CARs (z.B. Tisagenlecleucel) zeigen geringere Initialprolif eration, aber laengere Persistenz durch Foerderung zentraler Gedaechtnis-T-Zell-Differenzierung und mitochondriale Biogenese -- metabolisch effizienter fuer Langzeitueberwachung.",
        difficulty = 5,
        funFact = "CAR-T-Zell-Therapie ist die erste lebende Medizin der Geschichte -- jede Infusion enthaelt genetisch modifizierte, sich selbst replizierende T-Zellen. Die FDA-Zulassung 2017 fuer Tisagenlecleucel (Kymriah) fuer r/r B-ALL bei Kindern war ein historischer Moment; Kinder, die alle anderen Therapien ausgeschoepft hatten, erreichten 81% Remissionsrate."
    ),

    // Question 40 - Immuntherapie-Resistenz
    Question(
        categoryId = 16,
        questionText = "Welcher Mechanismus der primaeren Resistenz gegenueber PD-1/PD-L1-Checkpoint-Blockade ist am haeufigsten beschrieben?",
        answerA = "Tumorzellen sezernieren IL-2 und TNF-alpha, die T-Zellen aktivieren anstatt sie zu supprimieren",
        answerB = "STK11/LKB1-Verlust, PTEN-Verlust, WNT/beta-Catenin-Aktivierung und niedrige T-Zell-Infiltration ('Cold Tumors') durch Fehlen von Gefahrensignalen; beta-Catenin supprimiert CCL4 und verhindert dendritische Zell-Rekrutierung",
        answerC = "PD-L1-Ueberexpression auf T-Zellen statt Tumorzellen fuehrt zu Selbst-Suppression der Immunantwort",
        answerD = "Tumorzellen mutieren das PD-1-Rezeptor-Epitop, sodass Anti-PD-1-Antikoeprper nicht mehr binden koennen",
        correctAnswer = 1,
        explanation = "Mehrere Resistenzmechanismen werden beschrieben: (1) Nicht-T-Zell-inflamnmierter ('Cold') Tumor-Phaenotyp -- fehlt T-Zell-Infiltration, PD-L1-Signatur sinnlos. (2) WNT/beta-Catenin-Aktivierung hemmt CCL4-Sekretion und dendritische Zell-Rekrutierung. (3) STK11/LKB1-Verlust bei KRAS-mut NSCLC korreliert mit primaerer PD-1-Resistenz durch STING-Suppression. (4) Niedrige Mutationslast mit wenig Neoantigenen.",
        difficulty = 5,
        funFact = "Die Unterscheidung 'Hot' (T-Zell-inflammed) und 'Cold' (desert/excluded) Tumoren ist klinisch entscheidend. Neue Strategien kombinieren Checkpoint-Inhibitoren mit Oncolytic Viruses, STING-Agonisten oder CAR-T-Zellen um 'Cold' Tumoren in 'Hot' Tumoren umzuwandeln -- eine der aktivsten Forschungsgebiete der Onkologie."
    ),

    // Question 41 - Metastasierungsbiologie
    Question(
        categoryId = 16,
        questionText = "Was versteht man unter der 'Cancer Stem Cell Hypothesis' und wie erklaert sie Therapieresistenz?",
        answerA = "Alle Tumorzellen sind gleich stammzellartig; Therapieresistenz entsteht durch zufaellige Mutation unter Selektionsdruck",
        answerB = "Eine kleine Subpopulation von Tumorzellen (CSC) mit Stammzelleigenschaften (Selbsterneuerung, Differenzierungspotential, hohe ABC-Transporter-Expression, langsamere Proliferation) ueberlebt konventionelle Therapien und treibt Rezidiv und Metastasierung",
        answerC = "Stammzellen im normalen Gewebe transformieren zunaechst zu CSC, bevor sie Metastasen bilden",
        answerD = "CSC sind ausschliesslich im primaeren Tumor vorhanden; Metastasen enthalten keine Stammzellpopulation",
        correctAnswer = 1,
        explanation = "CSC (oder tumor-initiating cells) zeigen Stammzellmarker (CD44hi/CD24lo bei Brustkrebs, CD133+ bei Glioblastom), Selbsterneuerungskapazitaet (Spheroidbildung, serielle Transplantation in Immundefizienten Maeusen) und ausgepraegte Therapieresistenz durch ABC-Transporter (MDR1/ABCB1), erhoehte DNA-Reparatur und Quieszenz, die sie vor S-Phase-aktiven Zytostatika schuetzt.",
        difficulty = 5,
        funFact = "Die CSC-Hypothese erklaert einen klinischen Widerspruche: Tumoren die initial komplett ansprechen (komplette Remission radiologisch) rezidivieren oft -- weil verbliebene CSC unentdeckt bleiben. Anti-CSC-Strategien (z.B. Notch-Inhibitoren, anti-CD44-Antikoeprper) sind aktive Forschungsgebiete, haben aber bisher begrenzte klinische Wirksamkeit gezeigt."
    ),

    // Question 42 - Immunevasion
    Question(
        categoryId = 16,
        questionText = "Durch welchen Mechanismus entgehen Tumorzellen der Elimination durch zytotoxische T-Lymphozyten via MHC-I-Downregulation?",
        answerA = "Downregulation von MHC-I macht Tumorzellen unsichtbar fuer CD8-T-Zellen; allerdings aktiviert dies NK-Zellen ('Missing-Self'-Erkennung), was durch gleichzeitige HLA-E-Expression kompensiert wird",
        answerB = "MHC-I-Downregulation verhindert ausschliesslich die Peptiditpraesentation, hat aber keinen Einfluss auf NK-Zellen",
        answerC = "MHC-I-Verlust fuehrt zu erhoehter MHC-II-Expression, was CD4-T-Zellen aktiviert und NK-Zellen hemmt",
        answerD = "Tumorzellen mit MHC-I-Verlust werden bevorzugt von gDelta-T-Zellen erkannt und eliminiert, was Immunevasion verhindert",
        correctAnswer = 0,
        explanation = "Dies beschreibt das Immunevasions-Paradoxon: MHC-I-Verlust (haeufig durch B2M-Mutation oder Downregulation der Antigen-Prozessierungs-Maschinerie TAP1/TAP2) macht Tumorzellen fuer CD8-T-Zellen unsichtbar. NK-Zellen erkennen MHC-I-Verlust ('Missing Self') und werden aktiviert. Tumorzellen exprimieren daher gleichzeitig HLA-E (bindet NK-Rezeptor NKG2A inhibitorisch) und anti-NK-Checkpoint-Molekuele, um beiden Immunarmen zu entgehen.",
        difficulty = 5,
        funFact = "NK-Zellen koennen auch Tumorzellen toeten, die zu viel MHC-I exprimieren (Stress-induzierte NKG2D-Liganden wie MICA/MICB bei DNS-Schaeden). Dies macht sie zu idealen Partnern fuer Tumoren mit PD-1-Resistenz durch MHC-I-Verlust -- anti-NKG2A-Antikoeprper (Monalizumab) werden in Kombination mit Durvalumab getestet."
    ),

    // Question 43 - Synthese-Lethalitaet
    Question(
        categoryId = 16,
        questionText = "Was beschreibt synthetische Lethalitaet, und wie wird dieses Konzept bei BRCA1/2-mutierten Tumoren therapeutisch genutzt?",
        answerA = "Synthetische Lethalitaet beschreibt die additive Toxizitaet zweier Chemotherapeutika; bei BRCA-Tumoren werden daher zwei DNS-schaedigende Agenzien kombiniert",
        answerB = "Synthetische Lethalitaet bezeichnet den Zelltod, der entsteht, wenn zwei Gene gleichzeitig nicht-funktionell sind, waehrend der Verlust jedes einzelnen Gens tolerierbar ist. Bei BRCA1/2-Defizit (HR-Defizit) sind Tumorzellen von PARP abhaengig, da SSBs sonst in DSBs kollabieren -- PARP-Inhibitoren toeten selektiv HR-defiziente Tumorzellen",
        answerC = "Synthetische Lethalitaet bezeichnet die erhoehte Mutationsrate bei gleichzeitigem Verlust von BRCA1/2 und TP53, was Tumorzellen unbeherrschbar macht",
        answerD = "Synthetische Lethalitaet bei BRCA-Tumoren wird genutzt indem BRCA-Wildtyp-Zellen mit Olaparib selektiv getoetest werden, was Tumorzellen bevorzugt eliminiert",
        correctAnswer = 1,
        explanation = "Synthetische Lethalitaet: Gen A und Gen B sind 'synthetisch letal' -- Verlust von A+B kombiniert ist toedlich, aber jedes einzeln nicht. BRCA1/2 reparieren DNS-Doppelstrangbrueche durch homologe Rekombination (HR). PARP1/2 reparieren DNS-Einzelstrangbrueche (SSBs). Bei BRCA-Defizit koennen unrepararierte SSBs in DSBs kollabieren, die durch HR nicht repariert werden -- die Zelle stirbt an genomischer Instabilitaet.",
        difficulty = 5,
        funFact = "PARP-Inhibitoren (Olaparib, Niraparib, Rucaparib, Talazoparib) sind das klinische Paradebeispiel synthetischer Lethalitaet. Der Konept wurde 2005 gleichzeitig von den Gruppen um Alan Ashworth (UK) und Niall Martin und Tomas Helleday veroeffentlicht und dauerte nur 10 Jahre bis zur ersten FDA-Zulassung -- rekordverdaechtig schnell fuer einen biomarker-gesteuerten Ansatz."
    ),

    // Question 44 - Onkogene Spleissregulation
    Question(
        categoryId = 16,
        questionText = "Wie entsteht die onkogene Wirkung der EML4-ALK-Fusion bei NSCLC?",
        answerA = "EML4-ALK-Fusion fusioniert zwei inaktive Gene zu einem neuen aktiven Onkogen durch chromosomale Inversion auf Chromosom 2",
        answerB = "Die Inversion inv(2)(p21p23) fusioniert den N-Terminus von EML4 mit der Kinasedomaene von ALK. EML4-Teil foerdert Dimerisierung, ALK-Kinase ist dadurch liganden-unabhaengig konstitutiv aktiv und aktiviert PI3K/AKT, RAS/MAPK und JAK/STAT3",
        answerC = "EML4 und ALK werden auf getrennten Chromosomen amplifiziert und ihr Protein-Produkte bilden trans-Komplexe",
        answerD = "Die Fusion inaktiviert ALK (ein Tumorsuppressor bei NSCLC) durch dominant-negative Wirkung",
        correctAnswer = 1,
        explanation = "Die parazentrische Inversion inv(2)(p21p23) bringt EML4 (Exon 1-13, 1-20 oder andere Varianten) in-frame mit dem ALK-Tyrosinkinasegenlokus (ab Exon 20). EML4 enthaelt eine coiled-coil-Domaaene, die ALK-Kinasedomaene-Dimerisierung foerdert und damit konstitutive Transautophosphorylierung ohne ALK-Liganden (Pleiotrophin, Midkine) ausloest. Ca. 5% der NSCLC sind ALK-positiv; Crizotinib, Alectinib, Brigatinib sind ALK-TKI.",
        difficulty = 5,
        funFact = "Es gibt ueber 20 verschiedene EML4-ALK-Varianten (V1-V20) je nach Bruchpunkt in EML4. Manche Varianten zeigen unterschiedliches Ansprechen auf verschiedene ALK-TKI und unterschiedliche Resistenzmutationen -- die genaue Variantenbestimmung durch RNA-Sequenzierung wird daher klinisch zunehmend relevant."
    ),

    // Question 45 - Protein-Degradation als Therapie
    Question(
        categoryId = 16,
        questionText = "Wie wirken PROTACs (Proteolysis Targeting Chimeras) als neues therapeutisches Konzept?",
        answerA = "PROTACs sind bivalente kleine Molekuele: Ein Ende bindet das Zielprotein, das andere Ende rekrutiert einen E3-Ubiquitin-Ligase-Komplex, der das Zielprotein ubiquitiniert und fuer proteasomalen Abbau markiert",
        answerB = "PROTACs sind sinthetische miRNA-Molekuele, die Zielprotein-mRNA abbauen und damit Proteinsynthese verhindern",
        answerC = "PROTACs aktivieren Autophagie selektiv fuer Onkoproteine durch Bindung an Autophagierezeptoren (p62, NBR1)",
        answerD = "PROTACs sind Fusionsproteine aus Antikoeprper und Protease, die extrazellulaere Onkoproteine direkt spalten",
        correctAnswer = 0,
        explanation = "PROTACs bestehen aus drei Teilen: (1) Zielprotein-bindender Warhead (z.B. kleines Molekuel oder Peptid), (2) Linker (Laenge und Flexibilitaet beeinflusst Ternary-Complex-Geometrie), (3) E3-Ligase-Rekrutier-Ligand (z.B. CRBN-Binder VHL-Ligand, CRBN-Ligand Thalidomid-Derivat). Der ternary Komplex (Target+PROTAC+E3) ermoeglicht Polyubiquitinierung des Zielproteins und 26S-Proteasom-Abbau. Das PROTAC wird regeneriert und kann weiterwirken -- katalytisches Prinzip.",
        difficulty = 5,
        funFact = "ARV-110 (Bavdegalutamide) ist der erste PROTAC in klinischer Entwicklung fuer Prostatakarzinom und degradiert den Androgenrezeptor. Der Vorteil gegenueber klassischen AR-Inhibitoren: PROTAC wirkt auch bei AR-Mutationen, die Enzalutamid-Resistenz verursachen, da Degradation die gesamte Proteinfunktion eliminiert -- nicht nur Ligandenbindung."
    ),

    // Question 46 - Epigenetik Histonmodifikation
    Question(
        categoryId = 16,
        questionText = "Welche klinische Bedeutung hat die EZH2-Mutation bei follikulaeren Lymphomen?",
        answerA = "EZH2-Gain-of-function-Mutationen (Y641, A677, A687) erhoehen die H3K27me3-Methylierungskapazitaet und fuehren zu Hypermethylierung mit Stummschaltung von Differenzierungsgenen und B-Zell-Maturierungs-Suppression",
        answerB = "EZH2-Mutationen inaktivieren den Polycomb-Komplex und fuehren zu globaler H3K27-Hypomethylierung und Onkogenexpression",
        answerC = "EZH2-Mutationen foerdern beim follikulaeren Lymphom ausschliesslich genomische Instabilitaet ohne Einfluss auf den Epigenomstatus",
        answerD = "EZH2-Verlust fuehrt zu BCL-2-Hochregulation durch Demethylierung des BCL-2-Promoters",
        correctAnswer = 0,
        explanation = "EZH2 ist die katalytische Untereinheit des PRC2 (Polycomb Repressive Complex 2) und trimethyliert H3K27 (H3K27me3 = Genrepression). Gain-of-function-Mutationen (haaeufigste: Y641F/N/S/H/C) veraendern die Substratspezifitaet, sodass EZH2 effizienter H3K27me2 in me3 konvertiert. Resultat: Globale H3K27-Hypermethylierung, Repression von B-Zell-Differenzierungsgenen. Tazemetostat ist der erste EZH2-Inhibitor (EZH2-mut FL zugelassen).",
        difficulty = 5,
        funFact = "EZH2-Mutationen finden sich bei 25% der GCB-DLBCL und 20% der follikulaeren Lymphome. Paradoxerweise sind EZH2-Verlust-of-function-Mutationen bei T-Zell-Lymphomen und MDS tumorigeen -- EZH2 agiert als Onkogen (bei B-Zell-Lymphomen) UND als Tumorsuppressor (bei T-Zell-Neoplasien), je nach zellulaaerem Kontext."
    ),

    // Question 47 - Radiogene Tumorbiologie
    Question(
        categoryId = 16,
        questionText = "Welches biologische Konzept erklaert die Wirkung von SBRT (Stereotactic Body Radiotherapy) mit Einzelfraktionen > 8 Gy jenseits der klassischen Strahlungsbiologie (4 Rs)?",
        answerA = "Sehr hohe Einzeldosen sind weniger effektiv als fraktionierte Therapie; SBRT wirkt nur durch direkte DNS-Schaeden",
        answerB = "Hohe Einzeldosen (>8-10 Gy) aktivieren Ceramid-Synthese in Endothelzellen -> Endothelzelltod -> Gefaessokklusion und Tumor-Minderperfusion; ausserdem immunogener Zelltod (ICD) und cGAS/STING-Aktivierung foerdern systemische Immunantwort",
        answerC = "SBRT wirkt primaer durch Hyperthermie-induzierten Zelltod, da extrem hohe Dosisraten Waerme erzeugen",
        answerD = "Hohe Einzeldosen aktivieren p53 in Tumorzellen, die durch transkriptionelle Apoptose eliminiert werden -- ein p53-abhaengiger Prozess, der bei mutierten Tumoren nicht funktioniert",
        correctAnswer = 1,
        explanation = "Klassische Strahlungsbiologie (4 Rs: Repair, Redistribution, Repopulation, Reoxygenation) erklaert Fraktionierungseffekte. Bei SBRT-Dosen > 8-10 Gy: (1) Ceramid-Synthase-Aktivierung in Endothelzellen loest Apoptose aus -> Vaskulaere Disruption ergaenzt direkten Zelltod. (2) Immunogener Zelltod (ICD): Calreticulin-Exposition, HMGB1-Freisetzung, ATP-Sekretion aktivieren dendritische Zellen. (3) Zytosolische DNS-Fragmente aktivieren cGAS/STING -> IFN-Typ-I -> systemische Antitumorimmunitaet (abscopal effect).",
        difficulty = 5,
        funFact = "Der 'Abscopal Effect' -- Regression nicht bestrahlter Metastasen nach lokaler Radiotherapie -- wurde erstmals 1953 beschrieben, blieb aber klinisch selten. Seit Kombination von SBRT mit PD-1/PD-L1-Checkpoint-Inhibitoren wird er haeufiger beobachtet -- SBRT verwandelt den Tumor in einen 'in situ Impfstoff', Checkpoint-Inhibitoren loesen die T-Zell-Bremse."
    ),

    // Question 48 - Multidrug Resistance
    Question(
        categoryId = 16,
        questionText = "Welcher Transportmechanismus liegt dem klassischen MDR-Phaenotyp (Multidrug Resistance) zugrunde?",
        answerA = "Lysosomale Sequestrierung von Zytostatika durch pH-Verschiebung verhindert nukleaere Wirkstoffkonzentration",
        answerB = "Ueberexpression von P-Glykoprotein (ABCB1/MDR1), einer ATP-abhaengigen ABC-Efflux-Pumpe, die lipophile Substanzen (Vinca-Alkaloide, Taxane, Anthrazyline, Epipodophyllotoxine) aktiv aus der Zelle pumpt",
        answerC = "Erhoehte Glutathion-S-Transferase-Aktivitaet konjugiert Zytostatika an Glutathion und inaktiviert sie chemisch",
        answerD = "Mutationen in Topoisomerase II verhindern Anthrazyklin-Bindung und vermitteln spezifische Anthrazyklin-Resistenz ohne Kreuzresistenz",
        correctAnswer = 1,
        explanation = "P-Glykoprotein (Pgp, kodiert von MDR1/ABCB1) ist eine 170-kDa-ABC-Transporter-ATPase mit zwei Transmembraneinheiten und zwei cytoplasmatischen Nukleotidbindungsdomaenen. Pgp pumpt amphipathische Molekuele in einem ATP-abhaengigen Mechanismus aus der Zelle (Inside-to-Outside-Translokation). Ueberexpression entsteht durch MDR1-Genamplifikation, Promoter-Demethylierung oder klonale Selektion unter Therapie.",
        difficulty = 5,
        funFact = "Jahrzehntelang versuchte man klinisch Pgp mit Inhibitoren (Verapamil, Cyclosporin A, Tariquidar) zu blockieren -- alle klinischen Studien scheiterten wegen inakzeptabler Toxizitaet oder mangelnder Wirksamkeit im Menschen. Pgp wird neben Tumorzellen auch in Darm, Blut-Hirn-Schranke und Niere exprimiert, was systemische Hemmung gefaehrlich macht."
    ),

    // Question 49 - Ferroptose in der Krebstherapie
    Question(
        categoryId = 16,
        questionText = "Welcher molekulare Mechanismus liegt der Ferroptose zugrunde, und wie koennte diese fuer Tumortherapie genutzt werden?",
        answerA = "Ferroptose ist klassische Apoptose mit erhoehter Ferritin-Expression, die Caspase-Aktivierung foerdert",
        answerB = "Ferroptose ist eisenabhaengiger, regulierter Zelltod durch Akkumulation von Lipidperoxiden; GPX4 (Glutathion-Peroxidase-4) verhindern ihn durch Reduktion von Phospholipidhydroperoxiden; GPX4-Inhibitoren (z.B. RSL3) oder System-Xc-Hemmer (Erastin) loesen Ferroptose aus, besonders in Mesenchymalen, Therapieresistenten Tumorzellen",
        answerC = "Ferroptose ist mitochondrialer Zelltod durch Eisen-Fenton-Reaktion in Mitochondrien ohne Beteiligung von Membranlipiden",
        answerD = "Ferroptose ist ausschliesslich in haematopoetischen Zellen relevant und hat keine Bedeutung fuer solide Tumoren",
        correctAnswer = 1,
        explanation = "Ferroptose erfordert drei Voraussetzungen: (1) Eisenpraesenz (Fenton-Reaktion: Fe2+ + H2O2 -> Fe3+ + OH-Radikal). (2) Mehfach-ungesaettigte Fettsaeuren in Phospholipiden (PUFA-PLs, besonders Arachidonyl-PE). (3) Inaktivierung von GPX4 (reduziert PLOOH zu PLOH). Mesenchymale, therapieresistente Krebsstammzellen zeigen erhoehte Ferroptose-Empfindlichkeit. Erastin hemmt System-Xc-Cystin/Glutamat-Antiporter -> Cysteinmangel -> GSH-Mangel -> GPX4-Inaktivierung.",
        difficulty = 5,
        funFact = "Ferroptose wurde 2012 von Scott Dixon und Brent Stockwell als neuer Zelltod-Modus definiert. Hochinteressant: Mesenchymale Krebszellen, die gegen Chemotherapie, EGFR-TKI und Immuntherapie resistent sind, zeigen erhoehte Ferroptose-Empfindlichkeit -- Ferroptose koennte gezielt therapieresistente Tumorzellen eliminieren, genau diejenigen, die Rezidive verursachen."
    ),

    // Question 50 - Tumor-Heterogenitaet
    Question(
        categoryId = 16,
        questionText = "Was beschreibt das Konzept der Klonalen Evolution und intratumoralen Heterogenitaet (ITH) nach Nowell (1976), und warum ist sie therapeutisch relevant?",
        answerA = "Alle Tumorzellen eines Patienten sind genetisch identisch (klonal); Heterogenitaet entsteht nur durch epigenetische Variationen und ist therapeutisch irrelevant",
        answerB = "Tumorzellen unterliegen fortlaufend somatischer Evolution durch neue Mutationen unter Selektionsdruck; subklonale Mutationen in Therapieresistenzgenen praeexistieren oder entstehen unter Therapie, erklaeren Rezidive und Metastasierung und machen Kombinationstherapien notwendig",
        answerC = "Klonale Evolution beschreibt ausschliesslich die Transformation normaler Zellen zu Tumorzellen; nach Tumorentstehung haert genetische Variation auf",
        answerD = "ITH ist nur bei haematologischen Malignomen relevant; solide Tumoren sind genetisch homogen",
        correctAnswer = 1,
        explanation = "Peter Nowell postulierte 1976, dass Tumorzellen als instabile Klone beginnen und durch Akkumulation somatischer Mutationen unter Selektionsdruck (Immunsystem, Sauerstoff, Naehrstoffe, Therapie) diversifizieren. Subklone mit Wachstumsvorteil expandieren (klonale Expansion), andere sterben aus. Therapieresistente Subklone praeexistieren oft in < 1% der Tumorzellen und expandieren unter Therapiedruck -- erklaert praktisch alle Therapieversagen bei Krebspatienten.",
        difficulty = 5,
        funFact = "Single-cell-Sequenzierung hat gezeigt, dass ein einziger Primaertumor hunderte genetisch distinkte Subklone enthalten kann -- analog zu einem Oekosystem. Metastasen entstehen oft aus spezifischen Subklonen mit pro-invasiven Eigenschaften (Metastasis-Initiating Cells). Das Verstaendnis der Klonalarchitektur ist der Schluessel zu kurativen Kombinationstherapien."
    )

)
