package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun animalQuestionsMaster6(): List<Question> = listOf(

    // MASTER (difficulty = 5) -- Animal Immunology and Parasitology

    Question(
        categoryId = 9,
        questionText = "Welches Signalmolekuel aktiviert den Toll-Signalweg in Drosophila melanogaster bei Pilzinfektionen und unterscheidet ihn vom IMD-Weg?",
        answerA = "Direkte Bindung von Pilz-PAMPs an den Toll-Rezeptor",
        answerB = "Das prozessierte Zytokin Spaetzle, das nach Pilzerkennung durch die Protease Persephone aktiviert wird",
        answerC = "Lipopolysaccharid aus der Pilzzellwand bindet an PGRP-LC",
        answerD = "Der Transkriptionsfaktor Relish wird direkt durch Pilz-beta-Glucane aktiviert",
        correctAnswer = 1,
        explanation = "Im Drosophila-Toll-Weg erkennen sekretierte Mustererkennungsrezeptoren (GNBP3 fuer Pilze, GNBP1/PGRP-SA fuer grampositive Bakterien) Pathogene indirekt. Dies aktiviert eine Serinprotease-Kaskade, die letztlich Persephone aktiviert, welche das Prozytokin Spaetzle zu aktiv-Spaetzle spaltet. Aktiv-Spaetzle bindet dann den Toll-Rezeptor und aktiviert ueber Tube/Pelle/Cactus den Transkriptionsfaktor Dif/Dorsal. Der IMD-Weg wird dagegen direkt durch DAP-Peptidoglykan gramnegativer Bakterien ueber PGRP-LC aktiviert.",
        difficulty = 5,
        funFact = "Der Name 'Toll' (deutsch: toll/seltsam) wurde 1985 von Christiane Nuesslein-Volhard vergeben, die den Rezeptor als Entwicklungsgen entdeckte -- erst 1996 zeigte Jules Hoffmann seine Immunfunktion."
    ),

    Question(
        categoryId = 9,
        questionText = "Durch welchen Mechanismus entgehen Leishmania-Promastigoten im Makrophagen der lysosomalen Degradation?",
        answerA = "Sie verhindern die Fusion von Phagosomen mit Lysosomen durch Inhibition von Rab7",
        answerB = "Sie ueberleben im Phagolysosom durch Hemmung der NADPH-Oxidase und Saeure-Toleranz als Amastigoten",
        answerC = "Sie verlassen das Phagosom aktiv ins Zytoplasma wie Listeria",
        answerD = "Sie induzieren Autophagie und nutzen Autophagosomen als Replikationsnische",
        correctAnswer = 1,
        explanation = "Im Gegensatz zu Toxoplasma oder Mycobacterium hemmt Leishmania die Phagosomreifung nicht. Stattdessen differenzieren sich Promastigoten im sauren Phagolysosom zu Amastigoten, die an die harsche Umgebung (pH 4,5-5,0, hydrolytische Enzyme, reaktive Sauerstoffspezies) angepasst sind. Leishmania hemmt dabei aktiv die NADPH-Oxidase-Aktivitaet durch Entleerung von Lipid-Rafts und setzt Superoxid-Dismutasen ein. Die saure Phosphatase an der Oberflaechenmembran hemmt den oxidativen Burst.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Rolle spielt das Komplementregulatorprotein Decorin Binding Protein A (DbpA) bei Borrelia burgdorferi?",
        answerA = "Es aktiviert den klassischen Komplementweg zur Immunevasion durch Ablenkung",
        answerB = "Es bindet Faktor H und FHL-1 des Komplementsystems und verhindert so C3b-Ablagerung auf der Borrelien-Oberflaeche",
        answerC = "Es spaltet C3-Konvertase durch Serinprotease-Aktivitaet",
        answerD = "Es bindet CD55 (DAF) von Wirtszellen und uebertraegt es auf die Bakterienoberflaeche",
        correctAnswer = 1,
        explanation = "Borrelia burgdorferi exprimiert mehrere Komplement-Evasionsproteine. DbpA und die CRASPs (Complement Regulator-Acquiring Surface Proteins) binden Faktor H und seinen Splicevariante FHL-1 (Faktor H-like Protein 1) ueber Kurzsequenz-Konsensus-Wiederholungen. Faktor H beschleunigt den Zerfall der alternativen Weg-C3-Konvertase (C3bBb) und wirkt als Kofaktor fuer die Faktor-I-vermittelte Spaltung von C3b, wodurch die Komplementaktivierung auf der Borrelien-Oberflaeche wirksam unterdrueckt wird.",
        difficulty = 5,
        funFact = "Borrelia-Staemme, die Gelenke besiedeln (Arthritis-assoziiert), exprimieren hoehere Mengen an Faktor-H-bindenden Proteinen als solche, die nur Haut oder Herz befallen -- eine faszinierende Virulenz-Korrelation."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie manipuliert Toxoplasma gondii das Verhalten von Nagetieren auf molekularer Ebene?",
        answerA = "Das Parasit-Protein TgCTL4 stoert das Angstgedaechtnis durch GABAerge Hemmung im Amygdala",
        answerB = "Infizierte Dendritische Zellen migrieren ins Gehirn und setzen Dopamin frei, das Katzenurin attraktiv macht",
        answerC = "T. gondii induziert epigenetische Demethylierung des Katzenurin-responsiven Genes in Amygdala-Neuronen und hypersekretiert Dopamin durch Tyrosin-Hydroxylase-Hochregulation",
        answerD = "Das Parasit sezerniert miRNA, die Serotonin-Transporter im praefrontalen Kortex blockiert",
        correctAnswer = 2,
        explanation = "Die Verhaltensmanipulation durch T. gondii ist multifaktoriell. Studien zeigen, dass infizierte Nagetiere spezifisch die Angst vor Katzenurin (Trimethylthiazolin) verlieren, nicht aber vor anderen Raeuberdueften. Mechanistisch wurden epigenetische Veraenderungen (Histon-H3-Acetylierung und CpG-Demethylierung) am Oxytocin- und Vasopressin-Promotor in Amygdala-Neuronen nachgewiesen. Zudem besitzt T. gondii zwei Gene fuer Tyrosin-Hydroxylase (die geschwindigkeitsbestimmende Dopamin-Synthase-Enzyme), die zur erhoehten Dopaminproduktion in Neuronen fuehren. Die 'fatal feline attraction' ist ein klassisches Beispiel fuer parasitaere Verhaltensmanipulation.",
        difficulty = 5,
        funFact = "Beim Menschen wurde T. gondii-Infektion mit erhoehtem Risikoverhalten, Verkehrsunfaellen und veraenderten Persoenlichkeitsprofilen assoziiert -- ob Kausalitaet besteht, ist Gegenstand aktiver Forschung."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist die V(D)J-Rekombination und welches Enzym ist der zentrale Katalysator dieser Reaktion?",
        answerA = "Eine somatische Hypermutation durch AID (Activation-Induced Cytidine Deaminase) in Keimzentren",
        answerB = "Die somatische Rekombination von Immunglobulin/TCR-Gensegmenten durch RAG1/RAG2-Komplex, der RSS-Sequenzen erkennt und DNA-Doppelstrangbrueche induziert",
        answerC = "Die kombinatorische Assemblierung von Immunglobulin-Genen durch Topoisomerase II im Knochenmark",
        answerD = "Die Klassenumschaltung von IgM zu IgG durch S-Region-Rekombination mittels DNA-Ligase IV",
        correctAnswer = 1,
        explanation = "V(D)J-Rekombination verknuepft variables (V), Diversitaets- (D) und joining (J)-Gensegmente zu funktionellen Antigen-Rezeptorgenen. Der RAG1/RAG2-Komplex (Recombination Activating Gene) erkennt heptamer-nonamer Rekombinations-Signalsequenzen (RSS) mit 12- oder 23-bp-Spacern (12/23-Regel). RAG schneidet beide DNA-Straenge zu Haarnadelstrukturen (hairpin intermediates), die dann durch NHEJ (non-homologous end joining) -- unter Beteiligung von Ku70/Ku80, DNA-PKcs, Artemis, XRCC4 und DNA-Ligase IV -- verknuepft werden. Der zunaechst enzymatisch geoeffnete Haarnadelende fuegt durch TdT (Terminal deoxynucleotidyl Transferase) zufallige N-Nukleotide ein und erzeugt so junctionale Diversitaet.",
        difficulty = 5,
        funFact = "Das menschliche Immunsystem kann theoretisch mehr als 10^18 verschiedene Antigen-Rezeptorsequenzen erzeugen -- mehr als Atome auf der Erde."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher molekulare Mechanismus erklaert die Immunevasion von Plasmodium falciparum durch antigene Variation des PfEMP1-Proteins?",
        answerA = "P. falciparum besitzt ein einziges var-Gen, das durch Punktmutationen variiert",
        answerB = "P. falciparum hat ca. 60 var-Gene pro Haploidgenom; Monoallelische Expression wechselt durch epigenetische Regulation (H3K9me3-Heterochromatin) die exprimierte var-Variante",
        answerC = "PfEMP1-Diversitaet entsteht durch horizontalen Gentransfer zwischen Plasmodium-Staemmen",
        answerD = "Antigene Variation erfolgt durch alternative Spleissung eines einzelnen var-Prae-mRNA-Transkripts",
        correctAnswer = 1,
        explanation = "Plasmodium falciparum codiert etwa 60 var-Gene pro Haploidgenom, von denen jedoch immer nur eines aktiv exprimiert wird (monoallelische Expression). Der Wechsel zwischen var-Genen (Switching) erfolgt mit einer Rate von ca. 2% pro Generation und wird durch epigenetische Mechanismen kontrolliert: Das aktive var-Gen ist in euchromatinem Zustand mit H3K4me3 markiert, waehrend inaktive Gene durch H3K9me3 und HP1 (Heterochromatin Protein 1) silenced werden. PfEMP1 (P. falciparum Erythrocyte Membrane Protein 1) wird an der Erythrozytenoberflaeche exponiert und bindet Endothelrezeptoren (CD36, ICAM-1, CSA) -- Antikoeperselektionsdruck fuehrt zur Selektion weniger erkannter Varianten.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Wie unterscheiden sich die Immunglobulin-Klassen bei Knorpelfischen (Chondrichthyes) von denen der Teleostei?",
        answerA = "Haie besitzen kein IgM, nur ein einziges IgNAR (New Antigen Receptor)",
        answerB = "Haie und Rochen exprimieren IgM (pentamer/hexamer), IgNAR (Einzeldomaenen-Schwerkette), und IgW; Teleostei haben IgM, IgD und IgT",
        answerC = "Beide Gruppen exprimieren identische IgM-Pentamere; Knorpelfische fehlen IgD vollstaendig",
        answerD = "Knorpelfische besitzen kein adaptives Immunsystem und verwenden ausschliesslich angeborenem Immunitaet",
        correctAnswer = 1,
        explanation = "Chondrichthyes haben ein vergleichsweise altes, aber vollstaendiges adaptives Immunsystem. Sie exprimieren: IgM (als Pentamere oder Hexamere im Serum, als Monomer auf B-Zellenoberflaeche), IgW (orthologe zu IgD/IgNAR-Vorlaeufern, Murinae fehlen IgW), und IgNAR (New Antigen Receptor) -- einzigartige Schwerketten-einzig-Antikoeperformen ohne Leichtkette, Vorlaeufern der Camelid-Nanobodies. Teleostei dagegen exprimieren IgM (Tetramer), IgD (mit extensiver Transkriptionsdiversitaet) und das mucosal-Ig IgT (IgZ), das als funktionelles Analogon zu Saeuger-IgA gilt.",
        difficulty = 5,
        funFact = "IgNAR-Schwere-Ketten-Antikoeperteile (VHH-Domaenen, sogenannte Nanobodies) aus Haien werden intensiv als Therapeutika und Forschungswerkzeuge entwickelt, da sie durch ihre geringe Groesse in schwer zugaengliche Antigenstrukturen eindringen koennen."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Mechanismus erklaert die Immunprivilegierung des Hodens bei Saeugetieren?",
        answerA = "Der Hoden produziert hohe Mengen an IL-2 und verstaerkt regulatorische T-Zellen (Tregs)",
        answerB = "Die Blut-Hoden-Schranke durch Sertoli-Zell-Tight-Junctions, kombiniert mit Fas-Ligand-Expression auf Sertoli-Zellen und TGF-beta/IL-10-Sekretion, verhindert Autoimmunreaktionen gegen spaet-exprimierte Spermienantigene",
        answerC = "Keimzellen exprimieren kein MHC-I und sind daher fuer zytotoxische T-Zellen unsichtbar",
        answerD = "Inhibitorische NK-Zell-Liganden auf Spermatiden hemmen alle Immunantworten im Gonadengewebe",
        correctAnswer = 1,
        explanation = "Spermatogene Zellen entstehen nach Ausreifung der zentralen Toleranz und wuerden ohne Schutz als Autoantigene erkannt. Die Immunprivilegierung basiert auf mehreren Ebenen: (1) Physische Barriere durch Sertoli-Zell-Tight-Junctions (Claudin-11, Occludin), die Blutzellen vom adluminalen Kompartiment fernhalten; (2) Fas-Ligand (FasL/CD95L) auf Sertoli-Zellen induziert Apoptose eingewanderter T-Zellen ueber Fas-FasL-Interaktion; (3) Sertoli-Zellen sezernieren immunsuppressive Zytokine (TGF-beta1, IL-10, Activin) und regulatorische Mediatoren (Androgene). Defekte dieser Schranke fuehren zu autoimmuner Orchitis.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist die 'Hygiene-Hypothese' in der Veterinaerimmonologie und welche parasitologischen Mechanismen liegen ihr zugrunde?",
        answerA = "Saubere Tierhaltung verhindert Immunsuppression und steigert die Impfeffektivitaet",
        answerB = "Helminthe und andere Parasiten induzieren regulatorische T-Zellen (Tregs) und Th2-Antworten, die ueberschiessende Th1-vermittelte Inflammationen und Allergien hemmen",
        answerC = "Haeufige Parasitenexposition foerdert MHC-Diversitaet durch selektiven Druck",
        answerD = "Parasiten erhoehen Serum-IgE und konkurrieren mit Allergen-IgE um Mastzell-Rezeptoren",
        correctAnswer = 1,
        explanation = "Die erweiterte Hygiene-Hypothese (Old Friends-Hypothese nach Graham Rook) besagt, dass koevolutionaere Beziehungen zu Parasiten, insbesondere Helminthen, das Immunsystem in Richtung Toleranz konditioniert haben. Helminthen induzieren: Typ-2-Immunantworten (IL-4, IL-5, IL-13, IgE), starke Treg-Antworten (FoxP3+ Tregs sezernieren IL-10 und TGF-beta), alternative Makrophagen-Aktivierung (M2-Makrophagen, Arginase-1+). Diese immunologische Umgebung wirkt schützend gegen ueberschiessende Th1-Autoimmunreaktionen und atopische Erkrankungen. In Experimentalmodellen schuetzt Helminthen-Infektion vor Colitis, Diabetes Typ 1 und EAE (Experimentelle autoimmune Enzephalomyelitis).",
        difficulty = 5,
        funFact = "Klinische Studien mit kontrollierten Trichuris-suis-Infektionen (Schweinepeitschenwurm) beim Menschen zeigten in einigen Kohorten eine Verbesserung von Morbus Crohn und Colitis ulcerosa -- ein kontraintuitiver therapeutischer Ansatz."
    ),

    Question(
        categoryId = 9,
        questionText = "Welchen einzigartigen Immunglobulin-Diversifizierungsmechanismus verwenden Huehnervogel (Galliformes) statt V(D)J-Rekombination?",
        answerA = "Somatische Hypermutation in primaeren lymphoiden Organen noch vor Antigenkontakt",
        answerB = "Gen-Conversion: Pseudogene (25 VL-Pseudogene) spenden Sequenzabschnitte an das einzige funktionelle VL-Gen in der Bursa Fabricii",
        answerC = "Klasse-Schalter-Rekombination findet bereits in der Bursa statt und erzeugt primaere Antikoepervielfalt",
        answerD = "Haehne und Hennen nutzen alternative Spleissung von einem einzigen VH-Exon fuer Antikoepervielfalt",
        correctAnswer = 1,
        explanation = "Im Gegensatz zu Saeugetieren, die V(D)J-Rekombination von vielen funktionellen V-Genen nutzen, haben Huehnervogel nur je ein funktionelles VH- und VL-Gen. Die primaere Antikoepervielfalt wird in der Bursa Fabricii durch somatische Genkonversion erzeugt: Pseudogene (bei der Legehenne 25 V-Lambda-Pseudogene im Abstand von 12-17 kb) dienen als Sequenzspender. Durch Rekombination werden kurze Sequenzstuecke der Pseudogene in die CDRs des funktionellen V-Gens eingebaut. Dieser Prozess ist abhaeingig von AID (Activation-Induced Cytidine Deaminase) und laeuft in Bursa-Keimzentren vor jeglichem Antigenkontakt ab.",
        difficulty = 5,
        funFact = "Die Bursa Fabricii ist das namensgebende Organ fuer B-Lymphozyten -- 'B' steht urspruenglich fuer 'Bursa', erst spaeter wurde die Knochenmark-(Bone marrow-)Etymologie hinzugefuegt."
    ),

    Question(
        categoryId = 9,
        questionText = "Durch welchen Mechanismus schuetzt sich Trypanosoma brucei vor dem Wirtskomplementsystem?",
        answerA = "T. brucei exprimiert komplementinhibitorische Proteine analog zu CD55 und CD59",
        answerB = "Der dichte VSG-Mantel (Variant Surface Glycoprotein) blockiert physisch die C3b-Ablagerung und die Membranattackkomplex-Bildung; VSG-Wechsel entgeht der Antikoeperantwort",
        answerC = "T. brucei lebt ausschliesslich intrazellular und ist dem Komplement nicht ausgesetzt",
        answerD = "Sialidase spaltet Sialinsaeuren von C3b und verhindert so Opsonisierung",
        correctAnswer = 1,
        explanation = "T. brucei bewohnt das Blut und muss daher kontinuierlich dem Komplementangriff und Antikoeperselektionsdruck standhalten. Der 10-15 nm dicke VSG-Mantel (ca. 10^7 identische VSG-Dimere, verbunden ueber GPI-Anker) bildet eine physische Barriere, die invariante Oberflaechenantigene abschirmt und C3b-Ablagerung und MAC-Bildung behindert. Entscheidend ist die antigene Variation: Das Trypanosom hat ca. 1000-2000 VSG-Gene, exprimiert aber nur eines aktiv (monoallelisch, aus einer teomerennahen Expression Site). In-situ-Switching mit einer Rate ~10^-3 bis 10^-5 pro Zelle pro Generation stellt sicher, dass eine kleine VSG-Variante der humoralen Immunitaet entgeht und eine Welle parasitaemie nach der anderen erzeugt.",
        difficulty = 5,
        funFact = "Jeder Wachwechsel des VSG-Proteins laesst die Parasitaemie von hohen auf nahe-Null fallen, ehe die naechste VSG-Variante wieder ansteigt -- das erklaert die charakteristischen wellenfoermigen Fieberschuebe der Schlafkrankheit."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie funktioniert das Komplementsystem in Teleostfischen auf molekularer Ebene, und welcher Aktivierungsweg dominiert?",
        answerA = "Fische besitzen nur den klassischen Weg; Lektinweg und alternativer Weg fehlen",
        answerB = "Teleostfische haben alle drei Aktivierungswege (klassisch, Lektin, alternativ), aber einen erweiterten alternativen Weg mit mehreren C3-Paralogen (bis zu 11 C3-Homologe bei Karpfen)",
        answerC = "Fisch-Komplement arbeitet ausschliesslich ueber Sialinsaeure-spezifische Lektine",
        answerD = "Nur der Lektinweg ist bei Fischen funktional; der alternative Weg entstand erst bei Tetrapoden",
        correctAnswer = 1,
        explanation = "Das Komplementsystem der Teleostfische ist ueberraschend komplex. Waehrend Saeugetiere je ein C3-Gen besitzen, exprimieren Karpfen (Cyprinus carpio) bis zu 11 C3-Paraloge mit verschiedenen biochemischen Eigenschaften und Aktivierungspraeferenzen. Dies wird auf Genduplikation und neofunktionalen Druck durch das Leben in mikrobenreichen Suesswaesserumgebungen zurueckgefuehrt. Alle drei Aktivierungswege sind vorhanden: Der klassische Weg (C1q, C1r, C1s), der Lektinweg (MBL, Ficolins, MASPs) und der alternative Weg (C3, Faktor B, Faktor D, Properdin). Die terminalen Komponenten C5-C9 und die Bildung des MAC sind ebenfalls konserviert. Regulatorische Proteine wie Faktor H und C4BP sind bei Fischen identifiziert worden.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Was versteht man unter 'Epicuticular Wax'-Mimikry bei parasitischen Insekten und welche Spezies sind ein klassisches Beispiel?",
        answerA = "Parasitische Wespen imitieren Bienen-Duftmarkierungen durch konvergente Evolution von Terpensynthasen",
        answerB = "Sozialparasitische Schmetterlinge (z.B. Maculinea-Arten) imitieren die Cuticula-Kohlenwasserstoff-Profile von Myrmica-Ameisenlarven, um als Ameisenbrut akzeptiert zu werden",
        answerC = "Kuckucks-Hummel-Drohnen kopieren das Pheromon-Profil der Koeniginnen der Wirtshummelart",
        answerD = "Schmarotzerfliegen (Tachiniden) produzieren Wirtskokons-Duefte zur Ablenkung der Wirtsimmunitaet",
        correctAnswer = 1,
        explanation = "Maculinea-Schmetterlinge (heute: Phengaris, z.B. P. arion, P. alcon) sind klassische Beispiele chemischer Mimikry. Die Larven werden initial von Myrmica-Ameisenarbeiterinnen gefunden und in den Ameisenbau getragen, wo sie die Kohlenwasserstoff-Kutikula-Profile (ein Gemisch langkettiger n-Alkane, Alkene und Methyl-verzweigter Kohlenwasserstoffe) von Myrmica-Larven imitieren. Experimente von Thomas et al. (2009, Science) zeigten, dass die Schmetterlingslarven die Cuticula-Signale so praezise kopieren, dass Ameisen sie bevorzugt fuettern und sogar auf Kosten eigener Brut priorisieren. Gaestkaefer wie Lomechusa (Staphylinidae) verwenden denselben Mechanismus.",
        difficulty = 5,
        funFact = "Einige Maculinea-Arten ahmen nicht nur das Larven-Cuticulaprofil nach, sondern produzieren auch akustische Stridulations-Signale der Ameisenkoenigin -- ein doppelter Betrug mit chemischer und akustischer Mimikry."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Funktion haben Opisthosomal-Druesen (Dufour-Druese) bei parasitischen Kuckuckswespen im Kontext der Immunevasion?",
        answerA = "Sie produzieren Giftstoffe, die Wirtszellen direkt abtoten",
        answerB = "Sie sezernieren Kohlenwasserstoff-Mixturen, die das Cuticulaprofil der Wirtsspezies imitieren und so Aggression durch Wirtsbewohner verhindern",
        answerC = "Eierstab-Sekrete neutralisieren Encapsulation-Reaktionen der Wirtshamolymphe",
        answerD = "Alkaloid-Abwehrstoffe hemmen die Phenoloxidase-Kaskade im Wirtshaemocoel",
        correctAnswer = 1,
        explanation = "Parasitische Kuckuckswespen und kleptoparasitische Bienen (z.B. Nomada spp., Sphecodes spp.) dringen in Nester ihrer Wirtsarten ein, ohne angegriffen zu werden. Schluessel ist die Dufour-Druese (alias 'alkaline gland'), die bei kleptoparasitischen Nomada-Weibchen exakt die n-Alkane und Methylverzweigungen des Wirtszell-Kohlenwasserstoff-Profils produziert. Gaschromatographische Analysen (Klieger et al., 1991; Strohm et al., 2008) zeigten artspezifische Anpassungen: Jede Nomada-Art besitzt ein auf ihre Andrenid-Wirtsgattung abgestimmtes CHC-Profil. Dieses 'chemical insignificance' oder aktive Mimikry verhindert, dass die Wirte die Eindringlinge als fremd erkennen.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Wie induziert der Bandwurm Taenia solium seine Immunevasion im Zentralnervensystem (Neurozystizerkose)?",
        answerA = "Die Zystizerken-Hulle aus Kalziumkarbonat blockiert Antikoeperinfiltration mechanisch",
        answerB = "Lebendig-Parasit-Exkretions-/Sekretionsprodukte (ESPs) modulieren die Wirtsimmunitaet: Taeniaestatin hemmt Komplementaktivierung, Paramyosin bindet C1q, und der Parasit induziert lokale IL-10-Produktion und Treg-Expansion",
        answerC = "Zystizerken exprimieren HLA-G analog und werden so als tolerierte Foetuszellen eingestuft",
        answerD = "Das Nervensystem ist immunprivilegiert und Parasiten werden daher gar nicht erkannt",
        correctAnswer = 1,
        explanation = "Taenia solium Zystizerken ueberleben jahrelang im Zentralnervensystem durch aktive Immunevasion. Wichtige Mechanismen sind: Taeniaestatin (ein Serinprotease-Inhibitor) hemmt T-Zellproliferation und neutralisiert Komplement; Paramyosin bindet die Fc-Region von IgG und den Komplementfaktor C1q und blockiert so Opsonisierung und komplementvermittelte Lyse; Sulfatierte Polysaccharide auf der Tegoment-Oberflaeche nehmen Faktor H auf und hemmen den alternativen Komplementweg; ESPs induzieren IL-10-reiche Treg-Antworten und M2-Makrophagen-Polarisierung. Erst wenn der Parasit abstirbt, entsteht die klinisch problematische Entzuendungsreaktion.",
        difficulty = 5,
        funFact = "Das paradoxe Ergebnis der Neurozystizerkose: Patienten erkranken oft erst, wenn der Parasit stirbt -- die Entzuendungsreaktion auf den toten Wurm verursacht Krampfanfaelle, nicht der lebende Parasit."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Rolle spielt die Phenoloxidase (PO)-Kaskade in der angeborenen Immunitaet von Arthropoden?",
        answerA = "PO ist ein zirkulierendes Antikoeperanalogon, das Pathogene direkt opsonisiert",
        answerB = "Die PO-Kaskade wird durch Pathogenerkennungsproteine (PGRP, beta-Glucan-BR) aktiviert, fuehrt ueber Prophenoloxidase-Aktivierung zu Melanin-Ablagerung, und erzeugt zytotoxische Chinone und reaktive Sauerstoff/Stickstoff-Spezies an der Infektionsstelle",
        answerC = "PO aktiviert lysosomale Hydrolasen in Haemozyten fuer intrazellulare Abtotung",
        answerD = "Phenoloxidase katalysiert ausschliesslich die Kutikula-Bildung und hat keine Immunfunktion",
        correctAnswer = 1,
        explanation = "Die Prophenoloxidase (proPO)-Aktivierungskaskade ist ein zentraler Effektormechanismus der Arthropod-Immunitaet. Serinprotease-Kaskaden (aehnlich dem Saeuger-Komplementsystem) werden durch Mustererkennung (beta-1,3-Glucane, Lipopolysaccharide, Peptidoglykan) aktiviert und fuehren zur proteolytischen Aktivierung von proPO zu aktiver PO durch die Prophenoloxidase-aktivierende Enzym-Protease (PPAE). Aktive PO oxidiert Phenole (L-Tyrosin, L-DOPA) zu reaktiven o-Chinon-Intermediaten, die polymerisieren und Melanin bilden. Melanin kapselt Parasiten und Fremdkoerper ein (Encapsulation/Melanotische Kapselbildung). Chinon-Intermediate sind direkt zytotoxisch und toeten Pathogene durch oxidativen Stress ab.",
        difficulty = 5,
        funFact = "Viele Parasitoide wie Parasitoidwespen (Leptopilina-Arten) besitzen spezielle Virulenzproteine (Ovarian Calyx Fluid-Komponenten), die die proPO-Kaskade in Drosophila-Larven spezifisch hemmen."
    ),

    Question(
        categoryId = 9,
        questionText = "Was sind 'Immune Evasion Genes' (IEGs) bei Pockenviren und welche Rolle spielen sie bei Tierinfektionen?",
        answerA = "IEGs kodieren fuer Rezeptoren, die Makrophagen fuer Virusinternalisierung nutzen",
        answerB = "Pockenviren tragen Genbloecke, die Wirtszytokine und deren Rezeptoren imitieren (Virokin/Virozeptor), Komplement hemmen, Apoptose blockieren (vBcl-2, SPI-2) und INF-Signalwege stoeren (Ankyrin-repeat-Proteine)",
        answerC = "IEGs codieren virale Antigene, die MHC-I-praesentation von Wirtsproteinen steuern",
        answerD = "Pockenviren IEGs sind homolog zu Toll-like-Rezeptoren und loesen pro-entzuendliche Signalwege aus",
        correctAnswer = 1,
        explanation = "Pockenviren (Poxviridae) haben im Verlauf der Koevolution eine aussergewoehnliche Immunevasions-Genbibliothek akkumuliert. Beispiele aus Vaccinia- und anderen Pockenviren: Sekretierte Virozoeptor-Proteine (z.B. VV A53R) binden und neutralisieren Wirtszytokine (TNF, IL-1beta, IL-18); Komplementkontrollproteine (z.B. Smallpox Inhibitor of Complement Enzymes, SPICE; Vaccinia Complement Control Protein, VCP) binden C3b/C4b; Virale Bcl-2-Homologe (z.B. VV N1L, F1L) hemmen Mitochondria-vermittelte Apoptose; Serinprotease-Inhibitoren (Serpins: CrmA/SPI-2 hemmt Caspase-1 und -8, Granzym B); Ankyrin-repeat + F-box-Proteine unterdruecken NF-kappaB; PKR-Inhibitoren (E3L dsRNA-Bindeprotein) hemmen den Interferon-aktivierten Translationsstop.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Protein ist verantwortlich fuer die sogenannte 'Educated NK Cell'-Inhibition und wie wirkt es bei Herpesvirusinfektionen?",
        answerA = "KIR-Rezeptoren erkennen virale Glykoproteine als MHC-Surrogate und werden durch diese aktiviert",
        answerB = "Herpesviren kodieren MHC-I-Homologe (z.B. UL18 bei HCMV, m157 bei MHV68), die NK-Inhibitorrezeptoren (LIR-1/ILT2) binden und NK-Zellabtotung verhindern -- ein 'missing self' counter-evasion'",
        answerC = "Herpesviren werden durch NK-Zellen nicht erkannt, da sie latent in B-Zellen residieren",
        answerD = "Virale miRNA suprimiert NKG2D-Liganden-Expression auf infizierten Zellen",
        correctAnswer = 1,
        explanation = "NK-Zellen toeten Zellen, denen MHC-I fehlt ('missing self'). Viele Herpesviren downregulieren MHC-I, riskieren aber NK-Zellabtotung. Als Gegenstrategien kodieren Herpesviren MHC-I-Homologe: HCMV UL18 ist ein MHC-I-Homolog, das mit hoher Affinitaet an den inhibitorischen NK-Rezeptor LIR-1 (ILT2/CD85j) bindet und NK-Abtotung verhindert (Reyburn et al., 1997). Maus-CMV m157 bindet den aktivierenden Ly49H-Rezeptor in C57BL/6-Maeusen aber den inhibitorischen Ly49I in anderen Staemmen -- ein Fall, der die evolutionaere 'Red Queen'-Dynamik zwischen Virus und Wirtsimmunystem demonstriert. Dieses Prinzip wird als 'counter-evasion' der NK-missing-self Erkennung bezeichnet.",
        difficulty = 5,
        funFact = "Die Entdeckung, dass m157 in verschiedenen Mausstammen entweder aktivierende oder inhibitorische NK-Rezeptoren bindet, war ein Schluesselbeweis fuer die koevolutionaere Rustung zwischen Virus und Wirtsgenotyp."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist die 'Red Queen'-Hypothese im Kontext der Wirt-Parasit-Koevolution und welche empirischen Belege gibt es aus Tiermodellen?",
        answerA = "Parasiten profitieren nur von asexuell reproduzierenden Wirten, da diese keine genetische Variabilitaet bieten",
        answerB = "Sexuelle Reproduktion evolvierte als Antwort auf Parasitendruck: Haploide Pathogene selektieren fuer maximale MHC-Diversitaet; Potamopyrgus-Schnecken-Experimente (Lively 1987) zeigen, dass sexuelle Populationen in parasitenreichen Habitaten dominieren",
        answerC = "Die Red-Queen-Hypothese besagt, dass Wirte dauerhaft eine groessere genetische Vielfalt als ihre Parasiten maintainen muessen",
        answerD = "Parasiten treiben ausschliesslich durch Toxin-Selektion die Evolution von Wirtsresistenzgenen an",
        correctAnswer = 1,
        explanation = "Die Red Queen-Hypothese (Van Valen, 1973; Leigh Van Valen) postuliert, dass Organismen sich kontinuierlich anpassen muessen, nur um ihre relative Fitness gegenueber koevolvierenden Antagonisten zu erhalten. Im Wirt-Parasit-Kontext (Hamilton, 1980) treibt Parasitendruck die Selektion seltener Wirtsgenotypen an, da Parasiten an haeufige Genotypen adaptiert sind. Schluesselstudie: Curt Lively (1987) verglich sexuelle und asexuelle Potamopyrgus-antipodarum-Populationen (Neuseelaendische Suesswaasserschnecke) in Seen mit/ohne Trematoden-Parasiten (Microphallus). In parasitenreichen Seen dominieren sexuelle Linien; in parasitenfreien Seen konkurrieren asexuelle Klone erfolgreich. MHC-Diversitaets-Studien an Guppys (Hamilton und Zuk, 1982) zeigen Korrelation zwischen MHC-Heterozygotie und Parasitenresistenz.",
        difficulty = 5,
        funFact = "Der Name 'Red Queen' stammt aus 'Alice im Wunderland': 'Hier musst du so schnell laufen, wie du kannst, um an derselben Stelle zu bleiben' -- eine perfekte Metapher fuer koevolutionaere Rustungsrennen."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Mechanismus liegt der 'antibody-dependent enhancement' (ADE) bei Dengue-Infektion in Hunden und anderen Tieren zugrunde?",
        answerA = "Kreuzreaktive Antikoeperreaktionen aktivieren Komplementkaskaden und erhoehen vaskulaere Permeabilitaet",
        answerB = "Subprotektive Dengue-IgG-Antikoeperkonzentrationen aus Erstinfektionen bilden Antigen-Antikoeperkompiexe, die via FcgammaRI/II-Rezeptoren effizienter in Monozyten/Makrophagen internalisiert werden und eine stark vermehrte Virusreplikation ermoeglichen",
        answerC = "Kreuzreaktive T-Zell-Klone aus Erstinfektionen greifen Endothelzellen bei Zweitinfektion an",
        answerD = "IgM-Antikoeperrueckstaende hemmen bei Zweitinfektion die Interferon-alpha-Produktion",
        correctAnswer = 1,
        explanation = "ADE (Antibody-Dependent Enhancement) ist ein paradoxer Mechanismus: Bei Dengue-Zweitinfektion mit einem anderen Serotyp (DENV1-4) koennen kreuzreaktive, aber nicht vollstaendig neutralisierende IgG-Antikoeperkonzentrationen die Infektion verschlimmern. Subprotektive Antikoepertiter binden Viruspartikel (Fc-Region exponiert), und dieser Viruskomplex wird ueber Fc-gamma-Rezeptoren (FcgammaRI/CD64, FcgammaRII/CD32) auf Monozyten und dendritischen Zellen viel effizienter internalisiert als ohne Antikoeperbedeckung. Im Phagosom der Zelle wird der Virus nicht degradiert (wie normalerweise), sondern repliziert sich massiv. In Hunden und Affen ist ADE bei experimenteller sequentieller Dengue-Infektion dokumentiert; er erklaert den schwereren klinischen Verlauf bei DHF (Dengue haemorrhagic fever) bei Zweitinfektionen.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welche antimikrobiellen Peptide sind in Froesche-Haut katalogisiert und welche Strukturklassen sind vertreten?",
        answerA = "Nur lineare alpha-helikale Defensine analog zu Saeuger-beta-Defensinen",
        answerB = "Froschhaut produziert strukturell diversen AMP: Magainins (alpha-helikal, membranporenbildend aus Xenopus), Dermaseptine (Phyllomedusa), Esculentine (zyklisch, Rana), Bombinins (Bombina), und Temporine (kurze alpha-helikale AMPs) mit Aktivitaet gegen Bakterien, Pilze und Viren",
        answerC = "Amphibien nutzen ausschliesslich Histamide und Serotonin als Hautabwehrstoffe",
        answerD = "Froschhaut-AMPs sind strukturell identisch mit Insektendefensinen (beta-Faltblatt-Disulfidbindungen)",
        correctAnswer = 1,
        explanation = "Amphibienhaut-AMPs repraesentieren die artenreichste bekannte AMP-Quelle in der Tierwelt. Strukturklassen: (1) Magainins (Xenopus laevis) -- 23-aa alpha-helikale Peptide, wirken durch Membranpermeabilisierung (Toroid-Poren-Modell); (2) Dermaseptine (Phyllomedusa bicolor, brasilianischer Laubfrosch) -- 28-34 aa, breites Spektrum inkl. Pilze und Protozoen; (3) Esculentine (Rana esculenta) -- kationisch, breitspektrum; (4) Bombinins H (Bombina variegata) -- haben D-Aminosaeuren (seltener Naturprodukt!); (5) Temporine (Rana temporaria) -- sehr kurze 10-13 aa, kationisch, auch gegen Staphylococcus. Viele Froscharten gehen durch Chytridpilz (Batrachochytrium dendrobatidis) verloren -- AMP-Effizienz korreliert mit Resistenz.",
        difficulty = 5,
        funFact = "Bombinin H4 enthaelt eine D-Leucin-Einheit -- nur eine Handvoll bekannter tierischer Proteine enthalten D-Aminosaeure-Einheiten, die durch post-translationelle Epimerisierung entstehen."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie nutzt der Echinodermata-Seeigel (Strongylocentrotus purpuratus) sein Immunsystem und was ist an seinem Genrepertoire aussergewoehnlich?",
        answerA = "Seeigel besitzen ein vollstaendiges adaptives Immunsystem mit B- und T-Zell-Aequivalenten",
        answerB = "Seeigel haben kein adaptives Immunsystem, aber ein massiv expandiertes angeborenes Immunsystem mit ueber 200 TLR-Homologen (22 TLRs, 185 NOD-like Receptors, ~1000 SRCR-Domaeenproteine), mehr als jedes Saeugetier",
        answerC = "Das Seeigel-Immunsystem basiert ausschliesslich auf RNAi-vermittelter Virusabwehr",
        answerD = "Strongylocentrotus nutzt nur C3-vermittelte Komplementopsonisierung ohne zellulaere Immunkomponente",
        correctAnswer = 1,
        explanation = "Die Genomsequenzierung von Strongylocentrotus purpuratus (2006, Science) enthullte ein aussergewoehnlich expandiertes angeborenes Immunsystem. Waehrend Menschen 10 TLRs haben, traegt S. purpuratus mindestens 222 TLR-Gene. Die NOD-like-Receptor-Familie ist auf ueber 185 Gene expandiert (Saeuger: ~20). Am spektakulaersten: die SRCR (Scavenger Receptor Cysteine-Rich)-Superfamilie mit ueber 1000 Genen. Dazu kommen expandierte SP-Domaeenproteine und Lectine. Dieser 'genomische Immun-Hyperdiversitaet'-Befund deutet auf einen alternativen Weg zur Erkennung von Pathogen-Diversitaet hin: statt adaptiver Antikoepervielfalt eine massive Gen-Erweiterung angeborener Erkennungsproteine.",
        difficulty = 5,
        funFact = "Trotz fehlender adaptiver Immunitaet haben Seeigel eine Lebenserwartung von 100+ Jahren und sind selten von Krankheiten betroffen -- ein Hinweis, dass ein hochdiverses angeborenes Immunsystem sehr effektiv sein kann."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche immunologische Grundlage hat die 'Immune Tolerance in Pregnancy' bei Saeugetieren und wie erklaeren immunologische Hypothesen die Akzeptanz des semiallogenen Fetus?",
        answerA = "Der Fetus exprimiert keine MHC-Antigene und ist daher immunologisch unsichtbar",
        answerB = "Trophoblastzellen exprimieren kein klassisches MHC-I, aber nicht-klassisches HLA-G, das NK-Zell-KIR-Inhibitorrezeptoren aktiviert; IDO (Indolamin-2,3-Dioxygenase) in der Dezidua verarmt Tryptophan und hemmt T-Zellproliferation; deciduale Treg-Expansion und IL-10/TGF-beta-Milieu",
        answerC = "Maternale T-Zellen erkennen paternale Allele und werden durch klonale Deletion in der Dezidua eliminiert",
        answerD = "IgG-Antikoeperblockade aller paternal-spezifischer MHC-Antigene verhindert Immunerkennung",
        correctAnswer = 1,
        explanation = "Die immunologische Toleranz des semiallogenen Fetus ist multifaktoriell: (1) HLA-G-Expression: Extravilloeser Trophoblast exprimiert kein MHC-Ia (HLA-A, -B), aber reichlich HLA-G (nicht-klassisch), das an inhibitorische NK-Rezeptoren (KIR2DL4, LIR-1/ILT2) bindet; (2) IDO-vermittelte Tryptophandepletion in der Dezidua hemmt T-Effektorzellproliferation durch Aminosaeuremangelstress; (3) FasL/TRAIL-Expression auf Trophoblastzellen induziert Apoptose aktivierter T-Zellen; (4) Massive deciduale FoxP3+ Treg-Expansion (gefoedrert durch VEGF, IL-10, TGF-beta); (5) Progesterone-induzierte Th2-Verschiebung (PIBF: Progesteron-induzierter Blockierfaktor hemmt NK-Zytotoxizitaet). Stoerungen dieser Mechanismen werden mit Fehlgeburten und Praeeklampsie assoziiert.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welche spezifischen Mechanismen verwenden Ixodide-Zecken, um das Wirtsimmunsystem zu hemmen und eine laengere Blutmahlzeit zu ermoeglichen?",
        answerA = "Zecken sezernieren Schnellanasthesie-Substanzen, die den Wirt in einen Schlaf versetzen",
        answerB = "Zeckenspeichelproteine hemmen komplementaere Kaskaden (Isac, Salp20), blockieren Thrombozytenaktivierung (Apyrase, Prostacyclin-Analoge), hemmen Histamin-Freisetzung, supprimieren Th1-Zytokine (IL-12, IFN-gamma, TNF-alpha) durch Prostaglandin E2 und CXCL8-Hemmung",
        answerC = "Ixodide sezernieren IgG-abbauende Proteasen, die Antikoeperantworten zertoeren",
        answerD = "Zecken nutzen ausschliesslich Anophelinol (aus Muecken konvergent evolviert) zur Immunsuppression",
        correctAnswer = 1,
        explanation = "Ixodide-Zecken (Harte Zecken) benoeigen 3-10 Tage fuer eine vollstaendige Blutmahlzeit und haben dafuer ein Arsenal an immunmodulierenden Speichelkomponenten evolviert. Konkrete Beispiele: Salp20 (Ixodes scapularis) hemmt Properdin des alternativen Komplementwegs; Isac (Ornithodoros savignyi) hemmt Komplement-C3-Konvertase; Apyrasen degradieren ADP/ATP und hemmen Thrombozytenaggregation; Prostaglandin E2 im Zeckenspeichel hemmt IL-12-Produktion in DCs und foedrert Th2-Differenzierung; CXCL8-Binding-Proteine hemmen Neutrophilenrekrutierung; Iris (Ixodes ricinus immunosuppressor) und P36 hemmen direkt NK- und T-Zell-Proliferation.",
        difficulty = 5,
        funFact = "Zecken-Immunsuppression ist so effektiv, dass Viren im Speichelring waehrend des Beigens uebertragen werden koennen, ohne sofortige lokale Immunantwort -- dies erklaert die hohe FSME- und Borrelien-Uebertragungseffizienz."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie entgehen Schistosoma-Wurmern der Immunabwehr und was ist die Rolle der Tetraspaninproteine dabei?",
        answerA = "Schistosomen werden ausschliesslich durch Kapselung (Encapsulation) kontrolliert und entkommen durch physische Groesse",
        answerB = "Erwachsene Schistosomen tarnen sich durch Absorption von Wirtsglykoproteine (MHC-I, Blutgruppenantigene, Komplementregulator CD55/59) auf ihrer Tegumentoberflaeche; Tetraspanine (SmTSP-2) regulieren Tegument-Integritaet und sind Impfstoffkandidaten",
        answerC = "Schistosoma exprimiert CpG-Methylierungs-suppressives Enzym, das angeborene Erkennung hemmt",
        answerD = "Durch Integration ins Wirtsgenom wie Retroviren entkommen Schistosoma-Gene der Immunerkennung",
        correctAnswer = 1,
        explanation = "Erwachsene Schistosomen ueberleben jahrzehntelang im Blutgefaesssystem immunkompetenter Wirte durch ausgepragte Immunevasionsstrategien: Das Tegoment (Koerperaussen-huelle) saugt aktiv Wirtsmolekuele auf: MHC-I und MHC-II-Antigene werden absorbiert ('Host antigen acquisition'), sodass Effektorzellen den Parasiten als 'self' erkennen; Komplementregulatoren CD55 (DAF) und CD59 (Protectin) werden ebenfalls auf die Tegumentoberflaeche aufgenommen und hemmen Komplementlyse; Tetraspanine (SmTSP-1, SmTSP-2) sind strukturgebende Proteine des tegumentalen Outerleaf. SmTSP-2 als rekombinanter Impfstoff erzielte 57-60% Schutzwirkung in Nagermodellen (Tran et al., 2006) und ist derzeit in Phase-I-Klinischer-Erprobung.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Was sind 'excretory-secretory products' (ESPs) von Helminthen und wie modulieren sie dendritische Zellen?",
        answerA = "ESPs sind Toxine, die Wirtszellen lysosomal zerstoeren und so Erkennung verhindern",
        answerB = "Helminthen-ESPs enthalten Omega-1 (Ribonuklease), Katelicidin-aehnliche Peptide und Lektine, die ueber DC-SIGN-Endozytose aufgenommen werden und DCs von IL-12-produzierenden (Th1-foerdernd) zu IL-10/TGF-beta-produzierenden (tolerogen) Zellen umprogrammieren",
        answerC = "ESPs aktivieren NLRP3-Inflammasome und foerdern so pro-entzuendliche IL-1beta-Sekretion",
        answerD = "ESPs enthalten Virus-aehnliche Partikel, die TLR9 in Endosomen aktivieren",
        correctAnswer = 1,
        explanation = "Helminthen-ESPs sind komplexe Gemische mit direkten Effekten auf Immunzellen. Schistosoma mansoni Ei-ESPs enthalten Omega-1 (eine Typ-I-Ribonuklease, die mRNA in DCs abbaut und Proteinsynthese hemmt), Kappa-5 (Lektin), und IPSE/alpha-1. Omega-1 wird nach Glykan-vermittelter (Laktodifucosyl/LeX-Struktur) DC-SIGN-Bindung in DCs aufgenommen, degradiert dort mRNA, hemmt IL-12-Produktion und programmiert DCs auf OX40L-Expression (Th2-foerdernd) um. Die tolerogene DC-Reprogrammierung erklaert, warum Helminthen starke Th2-Antworten mit IgE, Eosinophilen und Mastzellen auslosen, aber regulatoische Mechanismen gleichzeitig Immupathologie begrenzen.",
        difficulty = 5,
        funFact = "Omega-1 ist so potent, dass nanomolare Mengen in Experimenten DCs vollstaendig zu tolerogen Praegung umprogrammieren -- ein Beweis fuer die fein abgestimmte immunologische Manipulation durch Parasiten."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche MHC-Klasse-II-Besonderheiten zeigen Teleostfische und wie unterscheidet sich ihre Praesentation von Saeugetieren?",
        answerA = "Fische exprimieren kein MHC-Klasse-II und haben nur T-Killer-Zellen ohne T-Helfer",
        answerB = "Teleostei exprimieren MHC-IIA/IIB mit konservierter Peptid-Bindungsgrube, aber die Invariante Kette (CD74) fehlt bei einigen Arten; CIITA-Regulation und HLA-DM-Aequivalente wurden identifiziert; ein einzigartiges 'MHCII-like' Molekuel (CD83-aehnlich) ist bei Fischen beschrieben",
        answerC = "Fisch-MHC-II praesentiert Lipidantigene statt Peptide ueber einen CD1-aehnlichen Mechanismus",
        answerD = "Teleostei nutzen MHC-III-Molekuele exklusiv fuer die Antigenpraesentation",
        correctAnswer = 1,
        explanation = "Teleostfische haben funktionelle MHC-Klasse-II-Molekuele, die aus klassischen Alpha- und Beta-Ketten bestehen und Peptidantigene praesentieren. Besonderheiten: Die Invariante Kette (Ii/CD74), die in Saeuger-MHC-II-Kompartimenten CLIP-Peptide platziert und den Antigen-Ladeweg reguliert, fehlt bei einigen Teleostei-Linien oder ist divergent. CIITA, der MHC-II-Transkriptionsakivator, ist bei Fischen identifiziert und durch Zytokine (IFN-gamma) regulierbar. HLA-DM-Aequivalente (fuer den Peptidaustausch im endosomalen Kompartiment) wurden beschrieben. Interessant: Bei Zebrafisch (Danio rerio) sind zwei MHC-II-Loci (DAA/DAB) beschrieben, mit mehr Polygenie als erwartet. CD4+ T-Helfer-Zellen mit funktionellem T-Zell-Korezeptor CD4 sind bei Fischen belegt.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Beschreiben Sie den vollstaendigen Lebenszyklus von Dicrocoelium dendriticum (Kleiner Leberegel) und die Beziehung zu seinen Zwischenwirten aus immunologischer Sicht.",
        answerA = "D. dendriticum hat nur einen Zwischenwirt (Lumbricus terrestris) und keinen Endwirt in freier Wildbahn",
        answerB = "D. dendriticum nutzt Landschnecken (1. ZW: Cornu aspersum, Zebrina detrita) fuer die Sporozysten-Reifung, dann Ameisen (2. ZW: Formica spp.) wobei eine infizierte Zerkarie ins Gehirn wandert und das Ameisenverhalten manipuliert (Kletterzwang), dann Wiederkaeuer (Endwirt) fuer die geschlechtliche Reifung",
        answerC = "Der Parasit ist digenus und benoetigt nur Wasserplanar-Schnecken und Fische als Wirte",
        answerD = "D. dendriticum uebertraegt sich direkt von Wiederkaeuer zu Wiederkaeuer durch Kotkontakt ohne Zwischenwirte",
        correctAnswer = 1,
        explanation = "Dicrocoelium dendriticum hat einen dreiwirtigen Lebenszyklus mit faszinanter Verhaltensmanipulation: (1) Eier mit Miracidien werden von Wiederkaeuer ausgeschieden; (2) Landschnecken (Cornu aspersum in Mitteleuropa, Zebrina detrita in Suedeuropa) ingeriern Eier; Sporozysten-1, Sporozysten-2 und Zerkarien entwickeln sich; Schleimballen mit Zerkarien werden ausgehustet; (3) Ameisen (Formica rufa, F. pratensis) fressen Schleimballen; die meisten Metazerkarien enkystieren im Abdomen, EINE 'Kliptozerkarie' wandert ins Suboesophageal-Ganglion und manipuliert direkt das Nervensystem; (4) Das Verhalten der Ameise aendert sich: Bei tiefen Temperaturen (nachts, kuehl) klettert sie auf Grasspitzen und verharrt -- Grasingestation durch Rinder/Schafe fuehrt zur Infektion; (5) Im Endwirt wandern Metazerkarien in Gallengaenge und reifen zu adulten Würmern. Die 'Kliptozerkarie' opfert sich fuer Reproduktion -- sie ist selbst nicht infektionsfaehig.",
        difficulty = 5,
        funFact = "Die Kliptozerkarie in der Ameisenganglion ist ein klassisches Beispiel eines 'Kamikaze'-Parasiten: Sie infiziert sich selbst nicht im Endwirt, dient aber als Manipulationseinheit, die den Rest der Metazerkarien ins Ziel bringt."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist 'RNA interference' (RNAi) als angeborener Abwehrmechanismus gegen Viren in Insekten und wie unterscheidet er sich von Saeuger-Interferon-Signalwegen?",
        answerA = "RNAi in Insekten und Interferon-Signalwege in Saeugetieren sind funktionell aequivalent und evoluataer homolog",
        answerB = "Insekten nutzen Dicer-2-vermittelte siRNA-Biogenese aus dsRNA-Replikationsintermediaten und RISC-Komplex zur Sequenz-spezifischen Virusdegradation; Saeuger verloren diesen antiviralen RNAi-Weg und nutzen stattdessen PKR/OAS/RNAse L und Interferon-Typ-I-Signalkaskaden",
        answerC = "Saeugetiere nutzen primar miRNA-basierte RNAi-Abwehr gegen RNA-Viren, Insekten dagegen Ribosome-mediated regulation",
        answerD = "Beide Systeme basieren auf TLR7/8-Erkennung von Einzelstrang-RNA und identischen Signaltransduktionsketten",
        correctAnswer = 1,
        explanation = "Insekten (und viele andere Invertebraten) nutzen RNAi als primaere antivirale Abwehrstrategie: Dicer-2 schneidet virale dsRNA-Replikationsintermediate in 21-nt-siRNAs, die mit R2D2 und Argonaute-2 in den RISC-Komplex (RNA-Induced Silencing Complex) geladen werden. RISC nutzt den siRNA-guide-Strang zur sequenzspezifischen Spaltung komplementaerer virualer RNA. Saeugetiere dagegen nutzen PKR (Protein Kinase R) -- dsRNA aktiviert PKR, das eIF2alpha phosphoryliert und Proteinsynthese stoppt; OAS/RNAse L degradiert RNA; IRF3/7 induzierne Typ-I-Interferone, die parakrin Nachbarzellen schutzen. Saeugetier-RNAi (Argonaute-2, Dicer, RISC) ist fuer antivirale Zwecke weitgehend unterdruckt (durch TRBP u.a.) und dient primaer der mikroRNA-Genregulation.",
        difficulty = 5,
        funFact = "Einige Flaviviren (Dengue, Zika) kodieren sfRNA (subgenomic flaviviral RNA), die Dicer-2 und AGO2 in Insekten kompetitiv hemmt -- eine antiRNAi-Gegenstrategie des Virus."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Rolle spielen Mast cells bei der Abwehr von Ektoparasiten und welche Effektormechanismen sind beschrieben?",
        answerA = "Mastzellen sind ausschliesslich an Typ-I-Hypersensitivitaetsreaktionen (Allergie) beteiligt, nicht an Parasitenabwehr",
        answerB = "IgE-vermittelte Mastzellen-Degranulierung bei wiederholter Zeckenexposition setzt Histamin, proteolytische Enzyme (Chymase, Tryptase) und VEGF frei, die vaskulaere Permeabilitaet erhoehen, Juckreiz verursachen und so das Abloesereflex ermoeglichen; Mastzellen rekrutieren auch Basophile und Eosinophile fuer Folgeabwehr",
        answerC = "Mastzellen produzieren IFN-gamma und aktivieren NK-Zellen zur direkten Zeckenabtotung",
        answerD = "Nur Neutrophile vermitteln Zeckenabwehr; Mastzellen spielen keine Rolle bei Ektoparasiten",
        correctAnswer = 1,
        explanation = "Mastzellen sind zentral fuer erworbene Resistenz gegen Zecken und andere Ektoparasiten. Bei Erstkontakt mit Zeckenallergenen (aus dem Speichel) sensibilisieren IgE-Antikoeperreaktionen Mastzellen. Bei Wiederkontakt (Zweitexposition) crossvernetzen Zeckenantigene FcepsilonRI-gebundene IgE-Antikoeperformen und induzieren Degranulierung: Histamin erhoht vaskulaere Permeabilitaet und induziert Juckreiz (foerdert Kratzverhalten/Abloesung); Chymase/Tryptase degradieren ektoparasitaere Speichelproteine; VEGF erhoht vaskulaere Permeabilitaet; LTB4/LTC4 (Leukotriene) rekrutieren Eosinophile. Mastzell-defiziente Mausstaemme (c-Kit-mutant: W/Wv) zeigen signifikant hoehere Zeckenlasten und verlaengerte Saugzeiten. Dieser Mechanismus ist immunologisch trainierbar: nach 3-4 Zeckenbissen kann Abloesung innerhalb von Stunden statt Tagen erfolgen.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher spezifische Immunglobulin-Isotyp ersetzte bei Knochen- und Karpfen-Suesswasserfischen IgA als mukosales Antikoerper und welche Eigenschaften hat er?",
        answerA = "IgM uebernimmt alle mukosalen Funktionen bei Fischen, da IgA nicht existiert",
        answerB = "IgT (auch IgZ bei Zebrafisch genannt) ist das mukosale Immunglobulin der Teleostei: Polymerisiert als Tetramer, wird von einem eigenen B-Zell-Subset (IgT+/IgM-) produziert, kommt primaer in mukosalem Schleim vor, reagiert spezifisch auf mukosale Pathogene und stellt ein funktionelles Analogon (kein Homolog) zu Saeuger-IgA dar",
        answerC = "IgD ist bei Fischen das mukosale Isotyp und wird als Dimer in Schleimhautsekreten gefunden",
        answerD = "Fische besitzen kein spezialisiertes mukosales Immunglobulin; alle Isotypen sind gleichmaessig auf alle Kompartimente verteilt",
        correctAnswer = 1,
        explanation = "IgT (Zhang et al., 2010, Immunity) wurde als neues mukosales Isotyp bei Regenbogenforelle (Oncorhynchus mykiss) entdeckt. IgT teilt mit IgA funktionelle Charakteristika aber keine Sequenzhomologie (konvergente Evolution). IgT-Eigenschaften: Produziert von IgT+/IgM- B-Zellen (in mukosalem Lymphgewebe); polymerisiert (tetramer) durch J-Ketten-Verbindung; konzentriert im Schleim der Kiemen, Haut und Darm; IgT-Titer in Schleimsekreten > Serumtiter; spezifisch hochreguliert bei Cryptosporidium-Infektion im Darm ohne entsprechende Serum-IgT-Erhoehung (lokale Immunantwort). IgT ist das Paradebeispiel konvergenter immunologischer Evolution: Saeuger-IgA und Fisch-IgT loesen dasselbe oekologische Problem (mukosale Abwehr) mit nicht-homologen Molekuelen.",
        difficulty = 5,
        funFact = "IgT wurde erst 2010 als eigenstaendiger Isotyp erkannt, obwohl Fischimmunologie seit Jahrzehnten erforscht wird -- ein Hinweis, wie viel noch uber nicht-Saeuger-Immunsysteme unbekannt ist."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Funktion hat der 'Immunological Synapse' (IS) bei zytotoxischen T-Lymphozyten (CTL) und welche SNARE-Proteine vermitteln die Granula-Sekretion?",
        answerA = "Der IS ist eine Kontaktstelle ohne aktive Funktion; Zytotoxizitaet erfolgt durch Zytokin-Diffusion ohne direkten Kontakt",
        answerB = "Am IS bilden T-Zelle und Zielzelle eine polarisierte Kontaktzone (cSMAC mit TCR/CD3, pSMAC mit LFA-1/ICAM-1); CTL-Granula polarisieren zum IS und sezernieren Perforin/Granzym B durch SNARE-vermittelte Exozytose (VAMP8 auf Granula, Syntaxin11 + SNAP23 auf Plasmamembran)",
        answerC = "Der IS rekrutiert ausschliesslich Komplementkomponenten und opsonisiert die Zielzelle",
        answerD = "SNARE-Proteine sind bei CTL irrelevant; Granzyme werden konstant sezerniert und nicht durch den IS reguliert",
        correctAnswer = 1,
        explanation = "Der Immunologische Synapse (IS) ist eine hoch geordnete Kontaktstruktur zwischen CTL und Zielzelle, die gerichtete Sekretion zytolyischer Granula ermoegliche. Struktur: cSMAC (Central Supramolecular Activation Cluster) mit TCR/CD3, PKC-theta, LAT -- Signaltransduktion; pSMAC (peripheral SMAC) mit LFA-1/ICAM-1, Talin -- Adhaesin-Ring; dSMAC (distal SMAC) mit CD45, CD43 -- Exklusion. Granula-Sekretion: Lytische Granula (Lysosomen-aehnlich, enthalten Perforin, Granzym B, Granulysin) polarisieren aktiv zum IS. Exozytose wird durch Ca2+-Einstrom und SNARE-Komplex vermittelt: VAMP8 (Vesicle-associated Membrane Protein 8) auf lytischen Granula interagiert mit Syntaxin-11 und SNAP-23 auf der Plasmamembran. Mutationen in Syntaxin-11 und Munc18-2 verursachen Familiaere Haemophagozytaere Lymphohistiozytose (FHL).",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Wie erklaert die 'Evolutionary Arms Race'-Theorie die aussergewoehnliche Diversitaet des MHC bei Wirbeltieren?",
        answerA = "MHC-Diversitaet entstand ausschliesslich durch neutrale Mutation ohne Selektion",
        answerB = "Negativfrequenzabhaengige Selektion durch Pathogene (haeuufige Pathogen-Haplotypen favorisieren seltene MHC-Allele durch bessere Prasentation neuer Peptide), kombiniert mit heterozygotem Vorteil und gemischte Selektionstheorien erklaeren die einzigartigen Polymorphismus-Niveaus des MHC",
        answerC = "Soziale Selektion durch Paarungspaeferenzen erklaert vollstaendig die MHC-Diversitaet ohne Pathogenbeteiligung",
        answerD = "MHC-Diversitaet ist ein Byproduct der retroviralen Integration im Vertebraten-Genom",
        correctAnswer = 1,
        explanation = "Der MHC ist die am starksten polymorphe Genregion in Wirbeltieren (HLA-B hat > 9000 bekannte Allele beim Menschen). Drei nicht-exklusive Erklärungstheorien: (1) Negativ frequenzabhaengige Selektion (NFDS): Seltene MHC-Allele praesentieren Pathogenpeptide, die noch nicht bekannt sind -- Pathogene sind an haufige Allele adaptiert; seltene Traeger haben Fitnessvorteil ('rare advantage model'). (2) Heterozygoter Vorteil: MHC-heterozygote Individuen praesentieren breiteres Peptidspektrum und haben in Infektionsstudien (HIV, HCV) Vorteile. (3) Koevolutionaeres Red-Queen-Modell: Pathogene evolvieren Peptide, die von verbreiteten MHC-Allelen nicht gebunden werden; neue MHC-Allele erhalten Selektionsvorteil. Empirische Belege: MHC-Allelfrequenzen korrelieren mit Pathogen-Praesenz (Wegner et al., 2003, Sticklinge); Signatur positiver Selektion in den Peptid-bindenden Resten.",
        difficulty = 5,
        funFact = "Cheetahs (Acinonyx jubatus) haben extrem niedrige MHC-Diversitaet (Folge von Populationsflaschenhals) und sind entsprechend anfaellig fuer bestimmte Infektionskrankheiten -- ein natuerliches Experiment fuer die Bedeutung von MHC-Diversitaet."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches parasitologische Phanaomen beschreibt 'hyperparasitismus' und nennen Sie ein konkretes Beispiel aus der Veterinaerentomologie?",
        answerA = "Das Ueberleben von Parasiten in Immunabwehr-defizienten Wirten in ungewoehnlich hoher Dichte",
        answerB = "Ein Parasit, der selbst von einem anderen Parasiten parasitiert wird (Sekundaerparasitismus): z.B. Encarsia perplexa (Chalcidoide Wespe) parasitiert Encarsia formosa (die Weisse Fliege-Parasitoid), die ihrerseits Trialeurodes vaporariorum parasitiert",
        answerC = "Ein Parasit, der mehrere Wirtsarten gleichzeitig befaellt und sich in allen gleichermassen reproduziert",
        answerD = "Die Uebertragung von Parasiten von einem Zwischenwirt auf mehrere Endwirte gleichzeitig",
        correctAnswer = 1,
        explanation = "Hyperparasitismus (auch Superparasitismus im weiteren Sinne) bezeichnet die Situation, in der ein Organismus (Hyperparasit oder Sekundaerparasitoid) einen anderen Parasiten als Wirt nutzt. In der biologischen Schaedlingsbekaempfung ist dies problematisch: Encarsia formosa parasitiert Larven der Weissen Fliege (Trialeurodes vaporariorum) und wird als Nuetzling eingesetzt. Encarsia perplexa (und andere hyperparasitische Chalcidoiden) parasitieren jedoch Encarsia-formosa-Larven in den bereits parasitierten Weisskohlfliegen-Puparien. Dies reduziert die Biocontrol-Effizienz. Andere Beispiele: Lysibia nana parasitiert Cotesia glomerata (Kohlweisslings-Parasitoid); Gelis-Arten (pteromalide Wespen) hyperparasitieren viele Braconid-Parasitoiden.",
        difficulty = 5,
        funFact = "In manchen Oekosystemen gibt es Tertiaerparasiten (die selbst Hyperparasiten parasitieren) -- eine Parasitierungskaskade bis zu 4+ Ebenen, die die Dynamik biologischer Kontrolle erheblich kompliziert."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches ist der molekulare Mechanismus der 'Pattern Recognition' durch NOD-like Receptors (NLRs) im angeborenen Immunsystem von Saeugetieren?",
        answerA = "NLRs sind membranstaendige Rezeptoren, die PAMPs extrazellular erkennen wie TLRs",
        answerB = "NOD1 erkennt DAP-Peptidoglykan (gramnegativer Bakterien) und NOD2 erkennt MDP (Muramyldipeptid, in allen Bakterien); Ligandenbindung aktiviert RIP2-Kinase via CARD-CARD-Interaktion, fuehrt zu NF-kappaB und MAPK-Aktivierung sowie Inflammasome-Assemblierung (NLRP3)",
        answerC = "NLRs binden bakterielle DNA und aktivieren den cGAS-STING-Signalweg",
        answerD = "NLR-Signalweg ist ausschliesslich in Epithelzellen aktiv und fehlt in Makrophagen",
        correctAnswer = 1,
        explanation = "NOD-like Receptors (NLRs) sind zytoplasmatische Mustererkennungsrezeptoren mit drei Domaenen: LRR (Leucin-rich repeat, Ligandenerkennung), NBD/NACHT (Oligomerisierung/ATPase), und Effektordomaene (CARD, PYD, BIR). NOD1 erkennt spezifisch iE-DAP (gamma-D-Glutamyl-meso-Diaminopimelsaeure), ein Peptidoglykan-Fragment gramnegativer Bakterien; NOD2 erkennt MDP (Muramyldipeptid), ein minimales Peptidoglykanfragment aller Bakterien. Bei Ligandbindung oligomerisiert NOD und interagiert via CARD-CARD-Interaktion mit RIP2 (RIPK2), das polyubiquitiniert wird und IKK-Komplex aktiviert -- fuehrt zu NF-kappaB-Translokation und proinflammatorischer Genexpression. NLRP3-Inflammasome (andere NLR-Unterfamilie) werden durch DAMPs/PAMPs aktiviert und assemblieren Caspase-1-aktivierende Komplexe, die IL-1beta und IL-18 prozessieren.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welche spezifischen Immunmechanismen vermitteln Resistenz bei Schafen gegen den Lungenwurm Dictyocaulus filaria?",
        answerA = "Neutrophile phagozytieren Larven direkt nach Inhalation in den Alveolen",
        answerB = "IgA in der Bronchialschleimhaut neutralisiert L3-Larven; Eosinophil-Peroxidase und EPX toeten Larven; IL-5-getriebene Eosinophilie und Mastzell-Chymase (MMCP-1) schaedigen Parasitenintegument; bei resistenten Rassen erhoehte Th2-Zytokin-Signatur",
        answerC = "Makrophagen bilden Granulome, die Larven vollstaendig einkapeln und aushungern",
        answerD = "NK-Zellen erkennen fehlendes MHC-I auf Larven und loesen ADCC aus",
        correctAnswer = 1,
        explanation = "Resistenz gegen Dictyocaulus filaria (kleiner Schaflungenwurm) basiert hauptsaechlich auf Th2-vermittelter Immunantwort. Mechanismen: (1) IgA-Sekretion in Bronchialschleimhaut (nach IgM-IgA-Switching) hemmt Larveninfektivitaet und Mobilitaet; (2) Eosinophile degranulieren und setzen EPX (Eosinophil Peroxidase), MBP (Major Basic Protein), und ECP (Eosinophil Cationic Protein) auf Larven frei -- direkte Zytotoxizitaet; (3) Mastzell-Chymase (MMCP-1) schaedigt das Larvenintegument; (4) Erhohte Muzinproduktion in Bronchien trapping Larven; (5) Resistente Rassen (Scottish Blackface) zeigen bei Reinfektion rascheres IL-5, IL-13, IL-4-Antwortmuster und staerkere Eosinophilie als empfaengliche (Suffolk) Rassen. Das Prinzip der von parasitaeren Larven induzierten Typ-2-Immunantwort ist auf viele Nematoden extrapolierbar.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist der 'Alum'-Adjuvans-Mechanismus und warum ist er immunologisch effektiv fuer Impfstoffe bei Tieren?",
        answerA = "Alum ist eine Oelwasser-Emulsion, die depotartige Antigenfreisetzung vermittelt",
        answerB = "Alum (Aluminiumhydroxid/-phosphat) aktiviert NLRP3-Inflammasome in Makrophagen durch Lysosompermeabilisierung und Kristallphagozytose, induziert uric acid-Freisetzung als DAMP, foerdert Th2-Antworten mit IgE/IgG1, und verzoegert Antigenfreisetzung durch Depotbildung",
        answerC = "Alum aktiviert ausschliesslich TLR4 durch Lipid-A-Kontamination und hat selbst keine adjuvante Wirkung",
        answerD = "Alum hemmt regulatorische T-Zellen durch Sequenzierung von TGF-beta und verstaerkt so Th1-Effektorzellen",
        correctAnswer = 1,
        explanation = "Aluminiumsalze (Alum) wurden seit den 1920er Jahren als Adjuvantien verwendet, bevor der Mechanismus bekannt war. Heute verstanden: (1) Depot-Effekt: Alum-Adsorption verzoegert die Antigenfreisetzung und verlaengert die Exposition von APCs; (2) NLRP3-Inflammasom-Aktivierung: Alum-Kristalle werden von Makrophagen phagozytiert, schaden Lysosomen (Lysosompermeabilisierung), setzen Cathepsin B frei und aktivieren NLRP3-Inflammasomen, was IL-1beta und IL-18-Prozessierung und -sekretion auslost; (3) Harnsaeure-DAMP: Zellschaden durch Alum setzt Harnsaeure aus, die als Gefahrensignal (DAMP) wirkt; (4) Th2-Polarisierung: Alum foerdert vornehmlich Th2-Antworten (IL-4, IL-5, IgE, IgG1 bei Maus), was fuer humorale Immunantworten vorteilhaft ist, aber Zellimmunitaet weniger foerdert.",
        difficulty = 5,
        funFact = "Trotz 100 Jahren Einsatz wurde der molekulare Wirkmechanismus von Alum erst 2008 mit der Entdeckung des NLRP3-Inflammasoms vollstaendig aufgeklaert -- eines der meistgenutzten Adjuvantien war lange ein 'schwarze Box'."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Immunmechanismen erklaeren den Unterschied in der Empfindlichkeit zwischen resistenten und suszeptiblen Rinderrassen gegenueber dem Zecken-Befall (Rhipicephalus microplus)?",
        answerA = "Resistente Rassen haben dickere Haut, die Zecken mechanisch am Eindringen hindert",
        answerB = "Zebu-Rassen (Bos indicus) zeigen erhoehte IgE-Titer, staerkere Basophilen- und Mastzell-Infiltration an Bissstellen, schnellere Pruritusreaktion und staerkere Histamin-Ausschuettung verglichen mit Bos taurus-Rassen; genetische Studien zeigen QTLs fuer Zeckenresistenz auf Chromosom 10, 14 und 23",
        answerC = "Resistenz bei Zebu-Rindern basiert ausschliesslich auf erhoehten Neutrophilenzahlen im Blut",
        answerD = "Resistente Rassen produzieren zeckentoxische Phytooestrogene in der Haut",
        correctAnswer = 1,
        explanation = "Bos indicus (Zebu)-Rassen wie Nelore zeigen deutlich hoehere natuerliche Resistenz gegen Rhipicephalus (Boophilus) microplus als Bos taurus-Rassen (z.B. Holstein). Immunologische Grundlage: (1) Hoehere IgE- und IgG1-Antikoepertiter gegen Zeckenspeichel bei Zebu-Rassen; (2) Staerkere kutane Mastzelldegranulierung und Histaminfreisetzung an Bissstellen verursacht Juckreiz und mechanische Reaktionen (Fellpflege); (3) Schnellere und intensivere Basophilen-Infiltration (cutaneous basophil hypersensitivity) -- Basophile sezernieren IL-4 und histaminaeehnliche Proteasen; (4) QTL-Studien identifizierten Chromosom-10-Loci, die mit Mastzell-FcepsilonRI-Expressionsniveaus assoziiert sind. Kommerzielle Impfstoffe gegen Zecken (z.B. TickGARD) basieren auf dem Zecken-Darmprotein Bm86 und imitieren diese natuerliche Resistenz durch antikoeperbasierte Zeckenabtotung.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Was sind 'Allergen-Endotypen' bei Tierallergien und wie unterscheidet sich die Immunpathologie zwischen Pferdeallergie-Typen?",
        answerA = "Endotypen sind rein diagnostische Klassifikationen ohne zugrundeliegende pathomechanistische Unterschiede",
        answerB = "Pferde mit Sommerekzem (Insultus culicoides) zeigen IgE-mediierte Typ-I-Hypersensitivitaet gegenueber Culicoides-Speichelantigenen (Endotyp A); COPD-aehnliche Pferdeasthma basiert auf Th2/ILC2-vermittelter Entzuendung mit IL-4, IL-5, IL-13 als Mediatoren und murinen Allergen-spezifischen Endotypen in verschiedenen Rassen mit unterschiedlicher MHC-II-Restriktion",
        answerC = "Alle Pferdeallergie-Typen teilen denselben IgG4-basierten Mechanismus wie Insektengiflallergie",
        answerD = "Pferdeallergien werden ausschliesslich durch CD8+ T-Zellen vermittelt und sind MHC-I-restringiert",
        correctAnswer = 1,
        explanation = "Endotypen charakterisieren Allergieformen nach zugrundeliegendem Immunmechanismus. Pferde-Sommerekzem (Sweet Itch/IBH: Insect Bite Hypersensitivity): Culicoides-Speichelantigene (v.a. Sialokinine, Phospholipase, Hyaluronidase) sensibilisieren ueber IgE-Bildung (Typ-I, IgE-mediiert). Bei Zweitkontakt crossvernetzen Allergene IgE auf Mastzellen und Basophilen, massive Histamin- und Protease-Freisetzung mit starkem Pruritus. Endotypisch unterschiedlich je nach Allergen-Profil: Einige Pferde zeigen Primin-Kreuzreaktivitaet (Allergen-Komponenten-Diagnostik). Equines Asthma (ehemals COPD oder RAO): Chronische eosinophile und neutrophile Bronchitis, Th2-polarisiert durch Stallstaub-Antigene (Schimmelpilze: Aspergillus fumigatus, Faenia rectivirgula), ILC2-Aktivierung, IL-5-getriebene Eosinophilie. MHC-II-Assoziationsstudien bei IBH zeigen Rassenunterschiede (Islaendische Pferde besonders suszeptibel nach Verbringen in Culicoides-reiche Regionen).",
        difficulty = 5,
        funFact = "Islaendische Pferde, die auf der culicoides-armen Insel Island geboren wurden und spaeter nach Mitteleuropa verbracht werden, entwickeln fast ausnahmslos schweres Sommerekzem -- fehlende neonatale Toleranzinduktion als Erklaerung."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie funktioniert der 'Prophenoloxidase-activating system' bei der Encapsulation von Parasitoide-Eiern in Drosophila?",
        answerA = "Lamellocyten phagozytieren Parasiteneiern und verdauen sie lysosomal",
        answerB = "Erkennung des Parasiteneis durch Haemozyten-Rezeptoren (NimC1/Nimrod, Eater) fuehrt zu Differenzierung von Lamellocyten, die mehrschichtige zellulaere Kapseln um das Ei formen; proPO-Aktivierung im letzten Kapselschicht melanisiert die Kapsel und toetet den Parasiten durch reaktive Chinone",
        answerC = "Granulozyten sezernieren IgM-aehnliche Peptide, die Parasitoide-Eier opsonisieren",
        answerD = "Crystal cells lysieren spontan auf das Parasitensignal und setzen PO direkt ins Haemocoel frei ohne Kapselformierung",
        correctAnswer = 1,
        explanation = "Cellular encapsulation in Drosophila ist ein vielstufiger Prozess: (1) Erkennung: Das Parasitoide-Ei (z.B. von Leptopilina boulardi) wird von zirkulierenden Plasmatozythen und Kristallzellen durch Mustererkennungsrezeptoren (Nimrod-Familie: NimC1, Eater; scavenger-Rezeptoren) erkannt; (2) Signalkaskade: ERK/JNK-Signalwege aktivieren den Transkriptionsfaktor Serpent (GATA) in Haemozytenpopulationen; (3) Lamellocyte-Differenzierung: Haemozytenpopulationen differenzieren sich massenhaft zu Lamellozyten (normalerweise selten, <5% der Haemozyten) -- grossen, flachen Zellen; (4) Kapselbildung: Lamellozyten schichten sich mehrlagig um das Parasitenei; Crystal cells werden durch Rac1-Aktivierung aktiviert und lysieren im Kapselumfeld; (5) Melanisierung: Freigesetztes proPO wird durch Serinproteasen aktiviert und melanisiert die Kapsel. Reaktive Zwischenprodukte toeten den Parasitoiden.",
        difficulty = 5,
        funFact = "Leptopilina-Arten haben als Gegenanpassung Virulenzproteine (LbGAP, eine RhoGAP-Domainprotein) evolviert, die Rac1 in Lamellozyten hemmen und die Kapselbildung effektiv verhindern."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Rolle spielen 'Damage-associated molecular patterns' (DAMPs) bei Nematodeninfektionen und wie aktivieren sie IL-33-Signalwege?",
        answerA = "DAMPs sind nur bei bakteriellen Infektionen relevant; Nematoden aktivieren ausschliesslich PAMPs",
        answerB = "Gewebeschaden durch einnistende Nematodenlarven setzt Alarmine frei: IL-33 aus gestressten Epithel-/Stromazellen bindet ST2 auf ILC2s und Mastzellen und induziert IL-5/IL-13-Sekretion; HMGB1, Harnsaeure und ATP verstaerken proinflammatorische DC-Aktivierung; kollektiv foerdern sie die protektive Typ-2-Immunitaet",
        answerC = "DAMPs aktivieren ausschliesslich den NLRP3-Inflammasom und foerdern IL-1beta gegen Nematoden",
        answerD = "Nematoden-induzierte DAMP-Aktivierung unterdruckt Typ-2-Immunantworten und foerdert chronische Infektion",
        correctAnswer = 1,
        explanation = "Nematoden haben keine klassischen PAMPs (fehlendes LPS, eingeschraenktes Peptidoglykan), trotzdem werden kraftige Typ-2-Immunantworten ausgeloest. Schluessel sind DAMPs aus schaedigtem Wirtsgewebe: Einnistende L3-Larven verursachen Epithelschaden, der IL-33 aus Kern-assoziierten Speichern in Stroma- und Epithelzellen freisetzt. IL-33 bindet ST2 (IL-1-Receptor-like 1) auf Gruppe-2 angeborenen lymphoiden Zellen (ILC2s), Mastzellen, Basophilen und mast zell-aehnlichen Zellen. ILC2-Aktivierung durch IL-33 + TSLP (thymic stromal lymphopoietin) + IL-25 ist der initiale Haupttrigger der Typ-2-Kaskade: ILC2s sezernieren IL-5 (Eosinophil-Foerderung) und IL-13 (Becherzellhyperplasie, Schleimsezernierung, glatte Muskelkontraktion = 'weep and sweep'). DAMPs HMGB1 und Harnsaeure verstaerken die DC-Aktivierung und foerdern Th2-Differenzierung.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist 'Concomitant immunity' bei Schistosoma-Infektionen und welchen klinischen Verlauf begrenzt sie?",
        answerA = "Konkomitante Immunitaet beschreibt vollstaendige Sterilisierungsimmunitaet nach einmaliger Infektion",
        answerB = "Adulte Schistosomen induzieren Immunitaet gegen neu einkommende Zerkarien und Schistosomula (concomitant immunity), tolerieren aber die bestehende adulte Wurmpopulation; Mechanismus umfasst IgE/Eosinophil-ADCC gegen Schistosomula unter Beteiligung von FcepsilonRII auf Eosinophilen",
        answerC = "Konkomitante Immunitaet bei Schistosoma bedeutet, dass Wirt und Parasit ein immunologisches Gleichgewicht ohne Entzuendungsreaktion erreichen",
        answerD = "Concomitant immunity beschreibt gegenseitige Immunsuppression zwischen koinfizierenden Schistosoma-Arten",
        correctAnswer = 1,
        explanation = "Concomitant immunity ist ein faszinierendes Phaenomen bei Schistosoma-Infektionen: Chronisch infizierte Wirte (Menschen, Nagetiere, Rinder) sind gegenueber einer Neuinfektion partiell geschuetzt, obwohl die bestehenden adulten Wurmer weiterleben. Mechanismus der Immunevasion adulter Wurmer: Tegument-Absorption von Wirtsmolekuelen (Host-antigen-Acquisition), s.o. Mechanismus der Abwehr gegen Schistosomula (Juvenile): IgE-Antikoeperformen gegen Oberflaechenantigene der Schistosomula binden und aktivieren Eosinophile via FcepsilonRII (CD23) -- antibody-dependent cell-mediated cytotoxicity (ADCC); neutrophile Granulozyten mit IgG-ADCC; Komplementaktivierung auf ungeschuetzten juvenilen Wurmern. Die ADCC-Aktivierung foerdert den Einsatz von basischer eosinophiler Proteinen (MBP, EPX) gegen das unreife Schistosomula-Integument.",
        difficulty = 5,
        funFact = "Concomitant immunity ist der Hauptgrund, warum ein Schistosomiasis-Impfstoff schwierig zu entwickeln ist: Man muss Immunitaet gegen Schistosomula induzieren, ohne gleichzeitig vorhandene adulte Wuermer (die als 'eigenes Gewebe' getarnt sind) zu attackieren."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Mechanismus erklaert die Bildung hepatischer Granulome bei Schistosoma mansoni-Infektion und welche Zytokine sind essentiell?",
        answerA = "Eier induzieren direkte Hepatozytennekrose durch Proteintoxine ohne Immunbeteiligung",
        answerB = "In der Leber gefangene Eier sezernieren IPSE/alpha-1 und Omega-1 (SEA-Komponenten), die Th2-Differenzierung und Granulombildung um die Eier auslosen; IL-4 und IL-13 sind essentiell fuer Granulomgroesse; TGF-beta treibt hepatische Fibrose; IL-10 hemmt ueberschuessige Pathologie",
        answerC = "Granulome entstehen durch Neutrophilen-Extrazellulare-Fallen (NETs) um haengen gebliebene Eier",
        answerD = "Leberkupfferzellen phagozytieren alle Eier ohne Granulombildung; Granulome entstehen nur im Darm",
        correctAnswer = 1,
        explanation = "Schistosoma-mansoni-Eier werden haematogen in der Portalzirkulation gefangen und sezernieren kontinuierlich Antigene (Soluble Egg Antigens, SEA). SEA-Komponenten (IPSE/alpha-1: IL-4-inducing principle of S. mansoni eggs -- triggen IgE+Basophil-IL-4-Freisetzung; Omega-1: Ribonuklease -- umprogrammiert DCs zu Th2-foerdernd). Die Th2-Immunantwort mit IL-4/IL-13 aktiviert M2-Makrophagen (Arginase-1+) und Fibroblasten, die Kollagen I/III sezernieren und das Ei einkapseln. Granulomgroesse: In IL-4-knockout-Maeusen sind Granulome deutlich reduziert; IL-13R-Blockade vermindert hepatische Fibrose; IL-10-knockout-Maeuse entwickeln schwerere Immunpathologie. Chronische schwere Infektion fuehrt zur periportalen Fibrose (Symmers-Fibrose), die portale Hypertension und Oesophagusvarizen verursacht -- die Haupttodesursache bei Schistosomiasis.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Was sind 'B-1 B cells' bei Maeuse-Schweinen und anderen Saeugetieren und welche Rolle spielen sie in der Nematodenimmunitaet?",
        answerA = "B-1-Zellen sind identisch mit B-2-Zellen und unterscheiden sich nur im Aktivierungsstatus",
        answerB = "B-1-Zellen (B-1a: CD5+, B-1b: CD5-) sind peritoneal/pleural angereicherte, T-Zell-unabhaengig aktivierbare Zellen, die naturalIgM produzieren und bei neonataler Helminthen-Infektion fruehzeitige IL-10 und IgM-Antikoeperproduktion vermitteln; bei Trichinella spiralis-Infektion ist B-1b-abhaeingige Immunerinerungs-Abwehr beschrieben",
        answerC = "B-1-Zellen sind ausschliesslich in Knochenmark zu finden und produzieren primaer IgA",
        answerD = "B-1-Zellen sind NK-Zell-Vorlaeuferzellen ohne Antikoeperproduktion",
        correctAnswer = 1,
        explanation = "B-1-Lymphozyten (entdeckt bei Maus, auch bei Schwein und Katze beschrieben) unterscheiden sich von konventionellen B-2-Zellen: Lokalisation: peritoneal (PerC), Pleura, Darm-assozierte Lymphgewebe; Entwicklung: hauptsaechlich foetal/neonatal; Aktivierung: koennen T-Zell-unabhaengig auf TI-2-Antigene (polysaccharide-artige, sich-wiederholende Strukturen) und TLR-Liganden antworten; Produkt: 'Natural' IgM (geringe Affinitaet, breit kreuzreaktiv), kein Isotype-Switch erforderlich. Bei Helminthen-Infektionen: B-1b-Zellen (CD5-, IgM+IgD+) können bei Trichinella-spiralis-Infektion spezifische T-unabhaengige Gedaechtnis-Immunantworten ausloesen (Alugupalli et al., 2004, Immunity). Bei neonataler Nippostrongylus-Brasiliensis-Infektion (Rattenwurm-Modell) sind B-1-Zellen fruehzeitig aktiviert. IL-10-Produktion durch B-1-Zellen reguliert Entzuendungsintensitaet.",
        difficulty = 5,
        funFact = "B-1-Zellen wurden urspruenglich als Vorlaeufer von B-Zell-Lymphomen entdeckt (daher 'B-1') -- erst spaeter erkannte man ihre physiologische Immunfunktion als erste Abwehr gegen T-unabhaengige Antigene."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Rolle spielt das 'Stickle-back' (Gasterosteus aculeatus)-Modellsystem in der Erforschung der MHC-Parasiten-Koevolution?",
        answerA = "Sticklinge dienen nur als Wirte fuer Laborkulturen von Pseudomonas; MHC-Studien sind bei Fischen nicht anwendbar",
        answerB = "Stichlinge zeigen Zusammenhang zwischen MHC-IIB-Alleldiversitaet und Parasitenresistenz: heterozygote Individuen sind gegen mehr Parasitenarten geschuetzt; Paarungspraeferenz fuer MHC-Dissimilaritaet ist durch Geruchsstudien belegt (Major Histocompatibility Complex-Paarungssequenz)",
        answerC = "Gasterosteus dient ausschliesslich als Modell fuer Knochen- und Wirbelsaeulenentwicklung, nicht fuer Immunologie",
        answerD = "Stichling-MHC ist vollstaendig mit Saeuger-HLA-Loci synteniisch und dient als direktes Model fuer humane Autoimmunkrankheiten",
        correctAnswer = 1,
        explanation = "Der Dreistachlige Stichling (Gasterosteus aculeatus) ist ein Schluessselmodell der Immunoeuvolution-Forschung. Studien von Wegner et al. (2003, Nature) und Kalbe et al. (2009) zeigten: (1) Heterozygotie am MHC-IIB-Locus korreliert mit breiterer Parasitenresistenz (gegen mehr Parasitenarten insgesamt); (2) Aber: Optimale (nicht maximale) MHC-Diversitaet -- zu viel MHC-Diversitaet erhoete die Rate an Autoimmunerkrankungen; (3) Weibchen bevorzugen MHC-dissimilaere Maennchen in Geruchsexperimenten (Paarungspraeferenz), was den MHC-Heterozygosyvorteile reproduktiv verstaerkt. Populationen aus verschiedenen Habitaten (Ozeanbewohnern vs. Suesswasser-Ecotypes) haben unterschiedliche MHC-Allelrepertoire entsprechend ihrer lokalen Parasitengemeinschaften -- ein eleganter Nachweis lokaler Adaptation durch Pathogendruck.",
        difficulty = 5,
        funFact = "Die Stichling-Paarungspraeferenz fuer MHC-fremde Maennchen ist klar gezeigt, aber der Mechanismus ist noch nicht vollstaendig geklaert -- Theorien umfassen Geruchs-Imprinting auf das elterliche MHC-Profil und Fremdheitserkennung."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist 'Immune priming' in Invertebraten und welche molekularen Mechanismen liegen zugrunde?",
        answerA = "Invertebraten haben keine Form von Immungedaechtnis; jede Infektion lost eine identische Primaerreaktion aus",
        answerB = "Immune priming bezeichnet in Invertebraten eine verbesste Reaktion bei Reinfektion; bei Drosophila ist NF-kappaB/Imd-abhaengige epigenetische 'trained immunity' in Haemozyten beschrieben; bei Copepoden und Schnecken sind spezifischere immune memory-aehnliche Reaktionen mit haemozytenbasierter Gedaechtniskomponente dokumentiert",
        answerC = "Immune priming in Invertebraten basiert auf maternaler Antikoerper-Uebertragung identisch zu Saeuger-IgG-Maternaltransfer",
        answerD = "Primaed immunity in Insekten entsteht durch dauerhafte Integration von Pathogen-DNA ins Wirtsgenom",
        correctAnswer = 1,
        explanation = "Obwohl Invertebraten kein klassisches adaptives Immunsystem haben, zeigen zahlreiche Studien spezifischere Immunreaktion bei Zweitexposition: Drosophila melanogaster zeigt nach subletaler Infektion mit Streptococcus pneumoniae verbesserte Ueberlebenraten bei Reinfektion durch denselben Stamm -- transkriptomische Analysen zeigen epigenetische Veraenderungen (H3K27-Trimethylierung) an Immunloci. Musca-domestica (Hausflege): Ovarial-Haemozyten-abhaengige Transgenerationale Praimierung. Pacifastacus-Flusskrebs und Litopenaeus-Garnelen: haemozytenvermittelte spezifischere Abwehr nach Vorexposition mit abgetoeten Pathogenen. Mechanismen: Epigenetische 'trained immunity' in langlebigen Haemozyten (Myeloid-aehnliches Gedaechtnis analog zu NK-Zell-Training in Saeugetieren), sezernierte lectinartige Proteine mit Pathogen-spezifischer Bindung. Das Feld ist umstritten -- stringente Belege spezifischen Gedaechtnisses sind nicht fuer alle berichteten Faelle eindeutig.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Mechanismus erklaert die Immunevasion von Eimeria tenella (Gefluegelkokzidiose) in der Bursa Fabricii und im Darmepithel?",
        answerA = "E. tenella ist vollstaendig intrazellular und daher unsichtbar fuer alle Immunzellen",
        answerB = "E. tenella Sporozoiten hemmen nach Invasion von Enterozyten die MHC-I und MHC-II-Hochregulation durch Blockade von IFN-gamma-Signaltransduktion (JAK-STAT-Hemmung), unterdrucken apoptotische Signalwege durch Serin/Threonin-Phosphatase-Sekretion, und modulieren NF-kappaB-Signalwege; Infektion der Bursa manipuliert B-Zell-Entwicklung",
        answerC = "Eimeria-Sporozoiten haben LPS-aehnliche Oberflaechen die TLR4 antagonistisch binden",
        answerD = "Alle Eimeria-Stadien sind extraintestinal und entgehen der Darmimmunitaet vollstaendig",
        correctAnswer = 1,
        explanation = "Eimeria-tenella-Infektion (Zoekum des Huhns) zeigt ausgepragte Immunevasion: (1) JAK-STAT-Hemmung: E. tenella sezerniert Proteine, die IFN-gamma-Rezeptor-nachgeschaltete STAT1-Phosphorylierung hemmen und so MHC-I/II-Hochregulation und IFN-gamma-stimulierte Genexpression unterdruecken; (2) Anti-Apoptose: Sporozoiten sezernieren Serpin-aehnliche Proteasehemmer und aktivieren Akt/PI3K, die intrinsische Apoptose verhindern und so das Replikations-Reservoir erhalten; (3) NF-kappaB-Modulation: Frueher nach Invasion wird NF-kappaB-Aktivierung gedaempft, um pro-entzuendliche Reaktionen zu begrenzen; (4) Bursa-Tropismus: E. tenella kann B-Zell-Vorlaeufer in der Bursa Fabricii befallen und dort Differenzierung storen; (5) sEA (sekretierte Exkretions-Antigene) hemmen Lymphozytenproliferation in vitro.",
        difficulty = 5,
        funFact = "Ein einzelnes Huhn kann durch Eimeria-Infektion bis zu 90% seiner Darmzotten verlieren -- die vollstaendige Regeneration dauert Wochen, erklaert die massiven Wachstumsdepressionen in der Gefluegelproduktion."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Rolle spielt das STING-cGAS-System bei der antiviralen angeborenen Immunitaet in Tieren, und wie entgehen DNA-Viren diesem Weg?",
        answerA = "STING-cGAS ist ausschliesslich bei Primatenviren relevant; Hunde und Katzen besitzen keine funktionellen STING-Gene",
        answerB = "Zytosolische DNA (viral oder mitochondrial) aktiviert cGAS (cyclic GMP-AMP synthase), die 2'3'-cGAMP synthetisiert; cGAMP aktiviert STING (stimulator of interferon genes), fuehrt zu IRF3-Phosphorylierung und IFN-beta-Induktion; DNA-Viren hemmen diesen Weg durch Virulenzfaktoren wie VACV-E5R (cGAS-Inhibitor), HSV-1-ICP27, KSHV ORF52",
        answerC = "cGAS erkennt RNA und STING wird durch RNA-Transkripte aktiviert",
        answerD = "STING-cGAS ist ausschliesslich in Lymphozyten aktiv und hat in Epithelzellen keine antivirale Funktion",
        correctAnswer = 1,
        explanation = "Der cGAS-STING-Signalweg ist ein zentraler angeborener antiviraler Sensor fuer zytosolische DNA. cGAS (cyclic GMP-AMP synthase) bindet cytoplasmatische doppelstraengige DNA (auch mitochondriale DNA nach Schaedigung) und synthetisiert den Second Messenger 2'3'-cGAMP (cyclic [G(2',5')pA(3',5')p]). cGAMP bindet und aktiviert STING im ER, das dimerisiert und zum Golgi transloziert; dort aktiviert STING TBK1 und IKKepsilon, die IRF3/7 phosphorylieren und Typ-I-IFN (IFN-alpha/beta) und NF-kappaB-abhaengige pro-entzuendliche Gene induzieren. DNA-Virus-Evasionsstrategien: Vaccinia VACV-E5R bindet und blockiert cGAS direkt; HSV-1-Tegumentprotein VP22 bindet cGAS und verhindert Bindung an DNA; KSHV-ORF52 blockiert cGAS; Adenovirus-E1A hemmt IRF3-Aktivierung; HCMV hemmt STING-Oligomerisierung.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist 'Nutritional immunity' und wie nutzen Wirte Eisen-Sequestrierung als Abwehrstrategie gegen Parasiten?",
        answerA = "Ernaehrungsimmunitaet beschreibt, wie Ernaehrungszustand die generelle Immunfunktion beeinflusst, ohne spezifische Mechanismen",
        answerB = "Nutritional immunity bezeichnet die aktive Sequestrierung essentieller Mikronaeehrstoffe (v.a. Eisen, Zink, Mangan) vom Erreger; Hepcidin-Produktion in Entzuendung senkt Serumeisen; Lactoferrin in Schleimhautsekreten cheliert Eisen; Lipocalin-2 bindet bakterielle Siderophore und verhindert Eisenaufnahme durch Pathogene",
        answerC = "Nutritional immunity beschreibt die Ernaehrungsoptimierung von Immunzellen durch Vitamin-D-Supplementation",
        answerD = "Serumeisen wird bei Infektion aktiv erhoht, um Immunzellen mehr Eisen fuer oxidativen Burst bereitzustellen",
        correctAnswer = 1,
        explanation = "Nutritional immunity (Begriff gepraegte von Eugene D. Weinberg, 1975) ist ein konservierter Verteidigungsmechanismus: Eisen ist essentiell fuer vielen Pathogenen (Bakterien, Protozoa: Plasmodium, Trypanosoma), kann aber aktiv vorenthalten werden. Mechanismen: (1) Hepcidin (Akute-Phase-Reaktant, produziert in Leber): Bei Infektion IL-6-induziert erhoeht; bindet und degradiert Ferroportin (FPN1, Eisenexporter) auf Makrophagen und Enterozyten -- Serumeisen faellt dramatisch (Anemia of Chronic Disease-Mechanismus, gleichzeitig Erregerisolierung); (2) Lactoferrin: In Neutrophilengranula, Milch, Traenen, Speichel -- hochaffines Eisenbindungsprotein (Kd = 10^-22 M) in sekretorischen Sekreten; (3) Lipocalin-2 (NGAL): Bindet Enterochelin (bakterielles Siderophor) und blockiert so Eisenrueckgewinnung durch E. coli, Salmonnellen; (4) Calprotectin (S100A8/A9): Chelatiert Zink und Mangan im Entzuendungsgewebe und verhindert bakterielles Wachstum.",
        difficulty = 5,
        funFact = "Die Anemia of Chronic Disease -- lange als Mangelerscheinung missgedeutet -- ist tatsaechlich eine aktive Immunstrategie des Wirts: Eisen wird nicht verloren, sondern in Makrophagen und Leber gespeichert, wo es Erregern nicht zugaenglich ist."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie unterscheidet sich die Immunglobulin-Diversifizierung bei Kameliden (Lama, Kamel) von konventionellen Saeugetieren?",
        answerA = "Kameliden haben kein IgG und nutzen nur IgM fuer alle humoralen Antworten",
        answerB = "Kameliden produzieren neben konventionellen Antikoeperformen schwerketten-only Antikoeperformen (HCAbs) ohne Leichtketten; der variable Bereich dieser HCAbs (VHH, 'Nanobody') ist allein fuer Antigenbindung zustaendig und wird durch V(D)J-Rekombination und somatische Hypermutation diversifiziert",
        answerC = "Kameliden verwenden Genkonversion (wie Huehnervogel) statt V(D)J-Rekombination fuer Immunglobulin-Diversitaet",
        answerD = "Kameliden-Immunglobuline sind identisch mit Haien-IgNAR und entstanden durch Horizontaltransfer",
        correctAnswer = 1,
        explanation = "Kameliden (Camelus dromedarius, C. bactrianus, Lama glama, Vicugna pacos) produzieren zwei Klassen von IgG-Antikoeperformen: konventionelles IgG (heterotetrameres, mit Leichtketten) und 'Heavy-Chain-Antibodies' (HCAbs, auch IgG2 und IgG3 bei Kamelen). HCAbs bestehen nur aus zwei Schwerketten ohne Leichtketten; die CH1-Domaene fehlt oder ist deletiert. Der variable Bereich der Schwerkette (VHH, Variable domain of Heavy-chain Antibody) bildet allein das Antigenbindungsparatop und ist etwa 15 kDa gross (Nanobody). VHH haben einzigartige Eigenschaften: lange CDR3-Schleifen fuer Kavitaetenbindung, hoehere Hydrophilitaet, pH/Temperaturstabilitaet. VHH-Diversifizierung erfolgt klassisch durch V(D)J-Rekombination und somatische Hypermutation; die Immunantwort auf Antigene ist vollwertig antigenspezifisch.",
        difficulty = 5,
        funFact = "Nanobodies wurden 1993 von Hamers-Casterman et al. als 'Zufall' entdeckt, als ein Medizinstudent eine Blutprobe falsch behandelte und bemerkte, dass Kamelblut zwei verschiedene IgG-Banden zeigte. Eine Fehlgeburt der Laborroutine fuehrte zur Entdeckung eines der nutzlichsten Proteinwerkzeuge der modernen Biochemie."
    ),

)
