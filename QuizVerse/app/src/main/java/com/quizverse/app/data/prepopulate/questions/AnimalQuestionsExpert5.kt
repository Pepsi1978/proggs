package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun animalQuestionsExpert5(): List<Question> = listOf(

    // ── EXPERT (difficulty = 4) — Entomology & Arachnology ──────────────────

    Question(
        categoryId = 9,
        questionText = "Welches Protein ist der Hauptbestandteil der Dragline-Seide der Gartenkreuzspinne (Araneus diadematus)?",
        answerA = "Fibroin",
        answerB = "Spidroin MaSp1 und MaSp2",
        answerC = "Kollagen Typ IV",
        answerD = "Resilin",
        correctAnswer = 1,
        explanation = "Die Dragline-Seide besteht hauptsaechlich aus den Major Ampullate Spidroins MaSp1 und MaSp2, die in spezialisierten Druesen produziert werden und der Seide ihre aussergewoehnliche Kombination aus Zugfestigkeit und Elastizitaet verleihen.",
        difficulty = 4,
        funFact = "Spinnseidenfasern sind bei gleichem Gewicht fuenf Mal reissfester als Stahl."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Enzym katalysiert bei Schmetterlingen (Lepidoptera) die Synthese des Juvenilhormons III aus Farnesolpyrophosphat?",
        answerA = "Farnesyl-Diphosphat-Synthase",
        answerB = "Methyl-Farnesenoat-Epoxidase (CYP15A1)",
        answerC = "Juvenile Hormone Acid Methyltransferase (JHAMT)",
        answerD = "Ecdyson-20-Hydroxylase",
        correctAnswer = 1,
        explanation = "Die Methyl-Farnesenoat-Epoxidase (CYP15A1) katalysiert den letzten Schritt der JH-III-Biosynthese: die Epoxidierung von Methyl-Farnesenoat. Das Enzym gehoert zur Cytochrom-P450-Familie und ist spezifisch in den Corpora allata lokalisiert.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Durch welchen molekularen Mechanismus wirkt das Neurotoxin alpha-Latrotoxin der Schwarzen Witwe (Latrodectus mactans)?",
        answerA = "Blockade spannungsgesteuerter Natriumkanaele",
        answerB = "Hemmung der Acetylcholinesterase",
        answerC = "Bildung von Kationen-permeablen Tetramerporen in der praesynaptischen Membran",
        answerD = "Antagonismus am GABA-A-Rezeptor",
        correctAnswer = 2,
        explanation = "alpha-Latrotoxin bindet an neuronale Rezeptoren (Neurexin, CIRL/Latrophilin) und bildet Tetramerporen in der praesynaptischen Membran. Dadurch stroemen massiv Ca2+-Ionen ein, was eine explosive, erschoepfende Neurotransmitterausschuettung ausloest.",
        difficulty = 4,
        funFact = "Das Gift ist 15-mal potenter als das der Klapperschlange — zum Glueck injiziert die Spinne nur Nanogramm-Mengen."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Ordnung der Insekten besitzt als einzige vollstaendig gefiederte (pinnate) Antennen bei beiden Geschlechtern?",
        answerA = "Lepidoptera",
        answerB = "Neuroptera",
        answerC = "Strepsiptera",
        answerD = "Coleoptera (Lampyridae)",
        correctAnswer = 2,
        explanation = "Strepsiptera-Maennchen besitzen charakteristisch gefiederte (flabellate) Antennen. Diese stark parasitaere Ordnung zeigt zudem einzigartige Merkmale wie umgekehrte Fluegelentwicklung und endoparasitische Weibchen, die nie das Wirtsinsekt verlassen.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welches biochemische Prinzip erklaert die extreme Toxizitaet von Batrachotoxin, das einige Giftpfeilfroesche aus Kaferkaefer-Beute (Melyridae) akkumulieren?",
        answerA = "Irreversible Hemmung der Na+/K+-ATPase wie Ouabain",
        answerB = "Permanente Aktivierung spannungsgesteuerter Natriumkanaele durch Bindung an Site 2",
        answerC = "Kompetitive Blockade nikotinischer Acetylcholin-Rezeptoren",
        answerD = "Inhibition der Komplexe I und III der mitochondrialen Atmungskette",
        correctAnswer = 1,
        explanation = "Batrachotoxin bindet irreversibel an Bindungsstelle 2 spannungsgesteuerter Natriumkanaele und haelt sie permanent in der offenen Konfiguration. Dies verhindert die Repolarisierung der Nervenzellmembran, was zu anhaltender Depolarisation und letalem Herzstillstand fuehrt.",
        difficulty = 4,
        funFact = "Batrachotoxin ist eines der giftigsten nicht-proteinogenen Toxine — die letale Dosis fuer Menschen betraegt schaetzungsweise 1-2 Mikrogramm pro Kilogramm Koerpergewicht."
    ),

    Question(
        categoryId = 9,
        questionText = "Was beschreibt der Begriff 'Haplodiploidy' im Kontext der Eusozialitaet bei Hymenoptera?",
        answerA = "Arbeiterinnen haben doppelt so viele Chromosomen wie Koeniginnen",
        answerB = "Maennchen entstehen aus unbefruchteten (haploiden) Eiern, Weibchen aus befruchteten (diploiden) Eiern",
        answerC = "Alle Kasten sind genetisch identisch durch Parthenogenese",
        answerD = "Koeniginnen produzieren ausschliesslich durch Meiose neue Individuen",
        correctAnswer = 1,
        explanation = "Bei Hymenoptera bestimmt die Haplodiploidy das Geschlecht: Koeniginnen koennen unbefruchtete (haploide, maennliche) oder befruchtete (diploide, weibliche) Eier legen. Hamilton erkannte, dass Schwestern dadurch 75% ihrer Gene teilen, was altruistisches Verhalten der Arbeiterinnen evolutionaer erklaert.",
        difficulty = 4,
        funFact = "Durch Haplodiploidy sind Bienenarbeiterinnen genetisch naeher mit ihren Schwestern verwandt als mit eigenen Toechtern."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Sinnesrezeptor ermoeglicht Bienen die Wahrnehmung des Erdmagnetfeldes zur Navigation?",
        answerA = "Magnetit-Nanokristalle in den Abdominaltergiten",
        answerB = "Johnston-Organ in der Antennenbasis",
        answerC = "Ocelli mit Cryptochrom-basierten Photorezeptoren",
        answerD = "Mesosomale Haarsensillen mit Fe3O4-Einschluessen",
        correctAnswer = 0,
        explanation = "In den Abdominaltergiten von Honigbienen wurden Magnetit-Nanokristalle (Fe3O4) nachgewiesen. Diese Biokristalle sind in Gruppen angeordnet und koennen Magnetfeldveraenderungen registrieren, was Bienen bei der Orientierung im Raum benutzen.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Wie heisst das seltene Phaenomen, bei dem eine Spinnenart ihre eigene Brut als Nahrungsquelle nutzt, nachdem sie Haemolymphe durch die Koerperwand ausschwitzt?",
        answerA = "Matriphagie",
        answerB = "Autotomie",
        answerC = "Progenie-Altruismus",
        answerD = "Trophallaxis",
        correctAnswer = 0,
        explanation = "Matriphagie bezeichnet das Fressen der Mutter durch ihre Nachkommen. Bei der Stachelbeinspinne (Stegodyphus lineatus) loest sich der Mutterkoerper enzymatisch auf und die Spiderlings konsumieren die fluessige Haemolymphe, nachdem die Mutter Verdauungsenzyme nach aussen sezerniert hat.",
        difficulty = 4,
        funFact = "Die Mutter beginnt sich selbst zu verdauen, noch bevor die Jungen schluepfen — ein extremes Beispiel fuer elterliche Fuerosorge."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Flugprinzip nutzen Insekten mit einem Koerpergewicht ueber 1 g, das mit der klassischen stationaeren Aerodynamik nicht erklaerbar ist?",
        answerA = "Gleitflug durch laminare Grenzschichtabloesung",
        answerB = "Fuehrende Randwirbel (Leading Edge Vortices) durch aktive Anstellwinkelaenderung",
        answerC = "Bernoulli-Auftrieb durch asymmetrisches Fluegelprofil",
        answerD = "Reaktionsauftrieb durch Abwaertsstoss (Downstroke Thrust)",
        correctAnswer = 1,
        explanation = "Insekten erzeugen Fuehrende Randwirbel (LEV) durch schnelle Torsion der Fluegel waehrend des Auf- und Abschlags. Diese stabilen Wirbel erhoehen den Auftriebsbeiwert weit ueber klassisch berechnete Werte. Dickinson und Ellington demonstrierten dies 1999 mit dem mechanischen Modell 'Robofly'.",
        difficulty = 4,
        funFact = "Der Hummelflug galt jahrelang als aerodynamisch unmoeglich — tatsaechlich nutzen Hummeln LEV besonders effizient."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Verbindungsklasse bildet den Hauptbestandteil der Duftmarkierung (trail pheromone) bei Lasius niger (Schwarze Wegameise)?",
        answerA = "Terpenoide (Nerol, Geraniol)",
        answerB = "Glykolipide mit langkettigen Fettsaeuren",
        answerC = "Pyrazine und Z9-Hexadecenol-Gemisch",
        answerD = "3-Ethyl-2,5-dimethylpyrazin und verwandte Methylpyrazine",
        correctAnswer = 3,
        explanation = "Lasius niger verwendet 3-Ethyl-2,5-dimethylpyrazin als primaeres Spurenhormon. Methylpyrazine verdunsten schnell (kein dauerhafter Belag) und ermoeglichten so prazise Schwarmsteuerung. Die Verbindungen werden in der Dufour-Druese produziert.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welche phylogenetische Position nehmen Skorpione (Scorpiones) innerhalb der Chelicerata ein?",
        answerA = "Basalste Gruppe der Arachnida, Schwestergruppe zu allen uebrigen Spinnentieren",
        answerB = "Enger Verwandtschaft mit Pseudoskorpionen (Pseudoscorpiones) in der Klade Panscorpiones",
        answerC = "Schwesterklade der Opiliones (Weberknechte)",
        answerD = "Naeher verwandt mit Merostomata (Schwertschwanzkrebse) als mit Arachnida",
        correctAnswer = 1,
        explanation = "Neuere phylogenomische Analysen (z.B. Sharma et al. 2014) platzieren Skorpione als Schwestergruppe der Pseudoskorpione innerhalb der Klade Panscorpiones. Beide Gruppen teilen basale morphologische Merkmale wie Pedipalpen-Chela.",
        difficulty = 4,
        funFact = "Skorpione sind unter UV-Licht fluoreszierend — das Hyalin in der Cuticula absorbiert UV und emittiert blau-gruenes Licht."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Strukturprotein verleiht dem Gelenkkopf (Resilin-Pad) von Floehn (Siphonaptera) seine extreme Rueckfederung beim Sprung?",
        answerA = "Chitin-Mikrofibrillen in helikoidaler Anordnung",
        answerB = "Resilin — ein Gummi-aehnliches Scleroproteins mit nahezu 97% elastischer Rueckfederung",
        answerC = "Arthropodin-Kollagen mit Quervernetzungen durch Dityrosin",
        answerD = "Abductin mit beta-Faltblatt-Domraenen",
        correctAnswer = 1,
        explanation = "Resilin ist ein hochgradig vernetztes Gummiprotein mit einer Energierueckfederungsrate von bis zu 97%. Im Pleuralfluegel-Gelenk (Metanotum) von Floehn speichert es Energie und gibt sie explosionsartig frei — Floehn beschleunigen dabei auf 140-fache Erdbeschleunigung.",
        difficulty = 4,
        funFact = "Floehn koennen das 200-fache ihrer eigenen Koerperlaenge springen — proportional die weiteste Sprungweite im Tierreich."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher chemische Botenstoff steuert bei Blatternten-Ameisen (Atta) die Aufteilung in Minor-, Media- und Major-Arbeiterinnen (Kastendifferenzierung)?",
        answerA = "Ecdyson-Titer in der Haemolymphe der Larven",
        answerB = "Koeniginferomon (9-ODA) inhibiert juvenile Hormone-Spiegel der Arbeiterinnen",
        answerC = "Mandibelsekret-Peptide steuern ueber Futtersaft-Verteilung die Kastendifferenzierung",
        answerD = "Unterschiedliche Juvenilhormon-III-Spiegel waehrend sensibler Larvalstadien bestimmen die Kastenentwicklung",
        correctAnswer = 3,
        explanation = "Bei Atta und verwandten Blattschneiderameisen steuern variierende JH-III-Konzentrationen in definierten Larvalstadien die Groesse und den Typ der entstehenden Arbeiterin. Hohe JH-Spiegel foerdern die Entwicklung zu Majoren. Futtersaft-Zusammensetzung beeinflusst diese Hormonspiegels.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist das primaere Toxin im Gift des Deathstalker-Skorpions (Leiurus quinquestriatus), das fuer seine extreme Gefaehrlichkeit verantwortlich ist?",
        answerA = "Charybdotoxin (Blocker von BK-Kaliumkanaelen)",
        answerB = "Tityustoxin (Na+-Kanal-Aktivator)",
        answerC = "Chlorotoxin (Cl--Kanal-Inhibitor in Gliomzellen)",
        answerD = "Agitoxin-2 (Shaker-Typ K+-Kanal-Blocker)",
        correctAnswer = 0,
        explanation = "Charybdotoxin ist ein 37-Aminosaeure-Peptid, das hochselektiv grosse Kalzium-aktivierte Kaliumkanaele (BK-Kanaele) blockiert. Zusammen mit Scyllatoxin (apamin-aehnlich) und weiteren Peptidtoxinen macht es das Gift von L. quinquestriatus zu einem der gefaehrlichsten Skorpiongiften.",
        difficulty = 4,
        funFact = "Das Gift enthaelt ueber 100 verschiedene Peptidtoxine — Forscher nutzen einzelne Komponenten in der Krebsdiagnostik."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches strukturelle Merkmal unterscheidet Amblypygi (Geisselspinnen) von echten Spinnen (Araneae) eindeutig?",
        answerA = "Fehlen von Chelizeren",
        answerB = "Kein Opisthosoma-Stiel (Petiolus), Opisthosoma breit mit Prosoma verbunden",
        answerC = "Fehlen von Spinnwarzen und fehlende Seidendruesen trotz Cheliceratae-Verwandtschaft",
        answerD = "Vorhandensein von Booklungs in allen 4 Opisthosoma-Segmenten",
        correctAnswer = 2,
        explanation = "Amblypygi besitzen keine Spinnwarzen und produzieren keine Seide. Sie haben lang gefiederte Pedipalpen mit Greifchela und extrem verlengerte erste Laufbeine als Tastorgane. Die phylogenetische Position innerhalb der Tetrapulmonata ist nahe Thelyphonida (Geisselscorpione).",
        difficulty = 4,
        funFact = "Amblypygi-Weibchen tragen ihre Eier in einem Kokon unter dem Abdomen und die geschluepften Jungen klettern auf den Ruecken der Mutter."
    ),

    Question(
        categoryId = 9,
        questionText = "Was versteht man unter 'Mullerian Mimicry' im Gegensatz zu 'Batesian Mimicry' bei Insekten?",
        answerA = "Bei Muellerscher Mimikry ahmt ein harmloses Insekt ein giftiges nach; bei Batesscher teilen sich zwei giftige Arten ein Warnmuster",
        answerB = "Bei Muellerscher Mimikry teilen zwei oder mehr giftige Arten dasselbe Warnmuster; bei Batesscher ahmt ein harmloses Insekt ein giftiges nach",
        answerC = "Muellerscher Mimikry bezeichnet Tarnung durch Kryptik; Batesssche ist aktive Warnung",
        answerD = "Beide Konzepte beschreiben identische Mechanismen, unterscheiden sich nur in der Namensgebung",
        correctAnswer = 1,
        explanation = "Fritz Muellers Modell (1878): Zwei oder mehr giftige Arten konvergieren auf ein gemeinsames Warnmuster — Fressfeinde lernen es effizienter zu meiden, was beiden Arten nuetzt. Henry Batess Modell (1862): Ein harmloses Insekt imitiert ein giftiges, um Schutz zu erlangen, ohne selbst Toxine zu produzieren.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Eigenschaft macht die Seide der Darwin-Rinde-Spinne (Caerostris darwini) zum belastbarsten biologischen Material, das jemals gemessen wurde?",
        answerA = "Hoechste Zugfestigkeit aller bekannten Materialien (4,5 GPa)",
        answerB = "Hoechste Zaehigkeit (Toughness) von 350 MJ/m3 — Kombination aus Zugfestigkeit und Dehnbarkeit",
        answerC = "Voellige Wasserunloeslichkeit durch Quervernetzung mit Zink-Ionen",
        answerD = "Selbstreparierende Eigenschaften durch reversible supramolekulare Bindungen",
        correctAnswer = 1,
        explanation = "Die Seide von C. darwini hat eine Zaehigkeit von ~350 MJ/m3, was zehnmal zaehes als Kevlar entspricht. Zaehigkeit beschreibt die Energie, die bis zum Bruch absorbiert wird — eine Kombination aus hoher Zugfestigkeit (~1,7 GPa) und aussergewoehnlicher Dehnbarkeit (bis 60%). Entdeckt von Ingi Agnarsson 2010.",
        difficulty = 4,
        funFact = "Darwin-Rindenspinnen bauen Netze ueber Fluessen, die bis zu 25 Meter breit sind — das groesste bekannte Spinnennetz."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Organ ist bei Coleopteren (Kaefern) fuer die Produktion von Benzochinon-haltigen Defensivsekreten verantwortlich?",
        answerA = "Labialdruesenapparat",
        answerB = "Pygidial- oder Abdominaldruesenpaare (je nach Taxon)",
        answerC = "Metathorakale Stinkdruesenpaare analog zu Heteroptera",
        answerD = "Colleterial-Druesenpaare im Genitalbereich",
        correctAnswer = 1,
        explanation = "Verschiedene Kaefer-Taxa haben unabhaengig evolvierte Abdominaldruesenpaare entwickelt. Beim Bombardierkafer (Brachinus) sind es Reservoirdruesen im Pygidalbereich, die bei Kontakt Hydrochinon und H2O2 vermischen und dabei eine 100 Grad Celsius heisse Chinonexplosion ausloesen.",
        difficulty = 4,
        funFact = "Der Bombardierkafer kann seine Explosivspritze bis zu 500-mal pro Sekunde feuern und zielt dabei praezise auf Angreifer."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist 'Wolbachia pipientis' und welche Rolle spielt dieser Organismus in der Insektenbiologie?",
        answerA = "Ein pathogenes Virus das Cytoplasmic Incompatibility verursacht",
        answerB = "Ein intrazellulares alpha-Proteobacterium das Reproduktion manipuliert und in ca. 40-65% aller Insektenarten vorkommt",
        answerC = "Ein Sporozoen-Parasit der Eierstocke befaellt",
        answerD = "Ein obligater Endosymbiont der fuer Vitaminproduktion unverzichtbar ist",
        correctAnswer = 1,
        explanation = "Wolbachia ist ein maternell vererbtes, intrazellulares Bakterium (Rickettsiales) das ca. 40-65% aller Insektenarten infiziert. Es manipuliert die Wirtsreproduktion durch Cytoplasmic Incompatibility, Parthenogenese-Induktion, Feminisierung und Male-Killing, um die eigene Verbreitung zu maximieren.",
        difficulty = 4,
        funFact = "Wolbachia wird aktiv in der Biobekampfung von Dengue-Muecken (Aedes aegypti) eingesetzt — infizierte Muecken koennen Dengue-Virus schlechter uebertragen."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Begriff beschreibt das Phaenomen, dass parasitoide Schlupfwespen das Verhalten ihres Wirtes gezielt manipulieren, bevor sie die Wirtspuppe verbrauchen?",
        answerA = "Verhaltensmanipulation durch Neurotoxine in der Haemolymphe",
        answerB = "Host-Behavior Manipulation oder 'Zombifizierung' durch gezielte Stiche in spezifische Ganglien",
        answerC = "Pheromonmimikry die das Wirtsverhalten umprogrammiert",
        answerD = "Epigenetische Reprogrammierung durch injizierte RNAi-Molekuele",
        correctAnswer = 1,
        explanation = "Parasitische Wespen wie Ampulex compressa (Juwelenwespe) stechen Kakerlaken praezise in das Supraoesophageal-Ganglion und das Metathorakalganglion. Der erste Stich laehmt temporaer das Vorderbein, der zweite injiziert GABA-Agonisten ins Gehirn und unterdruckt Fluchtverhalten dauerhaft.",
        difficulty = 4,
        funFact = "Die Juwelenwespe fuehrt ihr Opfer wie an einer Leine zum Nest — die Kakerlake laeuft willig in ihre eigene Lebendigkeit-Gruft."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Pollinationssyndrom beschreibt die Bestaeubung durch Fliegenarten, die Aas oder Kot imitierende Blumen besuchen?",
        answerA = "Cantharophilie (Kaefer-Bestaeubung)",
        answerB = "Sapromyophilie (Taeuschmimikry durch Aasfliegen-Anlockstoffe)",
        answerC = "Psychophilie (Tagfalter-Bestaeubung)",
        answerD = "Myrmekophilie (Ameisen-Bestaeubung)",
        correctAnswer = 1,
        explanation = "Sapromyophilie beschreibt Taeuschmimikry-Blueten die Aas- oder Kotgerueche imitieren (Methylmercaptane, Indol, Skatol) um aasbesuchende Diptera anzulocken. Die Fliegen erhalten keine Belohnung — die Bestaeubung basiert auf Tauschung. Beispiele: Dracontium, Stapelia, Helicodiceros.",
        difficulty = 4,
        funFact = "Aristolochia-Blueten hausen Bestaeubungsinsekten temporaer ein — Haare verhindern das Entkommen bis Pollen uebertragen ist."
    ),

    Question(
        categoryId = 9,
        questionText = "Was versteht man unter 'Trophobiose' im Kontext von Ameise-Aphid-Interaktionen?",
        answerA = "Raeuberische Interaktion bei der Ameisen Blattlaeuse fressen",
        answerB = "Gegenseitig vorteilhafte Symbiose: Ameisen 'melken' Blattlaeuse fuer Honigtau und schuetzen sie vor Raeubern",
        answerC = "Parasitische Beziehung bei der Blattlaeuse in Ameisennestern leben ohne Gegenleistung",
        answerD = "Kleptoparasitismus bei dem Ameisen Pheromonspuren der Blattlaeuse imitieren",
        correctAnswer = 1,
        explanation = "Trophobiose ist eine Mutualismusform: Ameisen stimulieren Aphide mechanisch zur Honigtauproduktion (Trophallaxin) und schuetzen sie vor Marienkaefer und Brackwespen. Manche Ameisen-Arten ueberwintern Aphid-Eier sogar in ihren Nestern.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Wie viele Augenpaare besitzt eine ausgewachsene Vogelspinne (Theraphosidae) typischerweise und wie sind sie angeordnet?",
        answerA = "4 Augenpaare (8 Augen) in zwei Reihen mit je 4",
        answerB = "4 Augen in einem Quadrat auf dem Clypeus",
        answerC = "8 Augen in einer kompakten Gruppe auf dem Vorderteil des Cephalothorax: 2+2+2+2 Anordnung",
        answerD = "6 Augen in einem Halbkreis wie bei Loxosceles",
        correctAnswer = 2,
        explanation = "Theraphosidae besitzen 8 Augen (4 Augenpaare) in einem kompakten Huegel auf dem vorderen Cephalothorax: anterior mediale, anterior laterale, posterior mediale und posterior laterale Augen. Die genaue Anordnung variiert innerhalb der Familie und ist taxonomisch bedeutsam.",
        difficulty = 4,
        funFact = "Trotz 8 Augen haben Vogelspinnen schlechtes Sehvermoegen — sie verlassen sich hauptsaechlich auf Vibrations- und Tast-Sensillen."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche biochemische Reaktion ermoeglicht Gluehwuermchen (Lampyridae) die Biolumineszenz?",
        answerA = "Oxidation von Riboflavin (FMNH2) durch Luciferase in Gegenwart von O2",
        answerB = "ATP-abhaengige Oxidation von Luciferin durch Firefly-Luciferase unter Emission von 550-570 nm Photonen",
        answerC = "Chemilumineszenz durch Reaktion von Superoxid mit Flavinen",
        answerD = "Fluoreszenzreemission nach UV-Absorption durch Phytoprotein-Chromophore",
        correctAnswer = 1,
        explanation = "Firefly-Luciferase katalysiert in zwei Schritten: 1) Luciferin + ATP → Luciferyl-AMP + PPi (Aktivierung), 2) Luciferyl-AMP + O2 → Oxyluciferin* + AMP + CO2 → Emission von gelbgruener Biolumineszenz (~560 nm). Das System laeuft mit nahezu 100% Quantenausbeute.",
        difficulty = 4,
        funFact = "Firefly-Luciferase ist das am haeufigsten eingesetzte Reporter-Gen in der Molekularbiologie und wird in millionen Experimenten jaehrlich verwendet."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist 'Aposematismus' und welcher selektive Vorteil ergibt sich fuer warnfarbige Insekten?",
        answerA = "Kryptische Faerbung zur Tarnung gegen Hintergrund",
        answerB = "Warnsignalgebung durch auffaellige Faerbung die potenzielle Raeubern auf Toxizitaet oder Unwohlsein hinweist",
        answerC = "Sexuelle Selektion durch Blauetonung der Fluegel bei Schmetterlingen",
        answerD = "Schutz durch UV-Reflexion gegen Sonnenstrahlung",
        correctAnswer = 1,
        explanation = "Aposematismus (griech. apo = weg, sema = Zeichen) ist das Warnsignalsystem durch kontrastreiche Faerbung (oft Gelb/Schwarz, Rot/Schwarz). Raeubern lernen diese Signale mit schlechtem Geschmack oder Gift zu assoziieren (aversives Konditionieren) und meiden aposematische Tiere. Evolutionaer stabiles System trotz initialer Kosten fuer Pionier-Morphen.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Komponente des Bienengifts (Apis mellifera) ist fuer die Aktivierung von Mastzellen und Histaminfreisetzung verantwortlich?",
        answerA = "Phospholipase A2 (PLA2) durch Lyso-PC-Produktion",
        answerB = "Melittin durch direkte Membrandestabilisierung und Mastzell-Degranulation",
        answerC = "Apamin durch selektive SK2-Kaliumkanal-Blockade in Mastzellen",
        answerD = "Hyaluronidase durch Abbau von Gewebestruktur und indirekte Sensibilisierung",
        correctAnswer = 1,
        explanation = "Melittin (26 Aminosaeuren, 52% des Trockengewichts) ist das Haupttoxin im Bienengift. Es inseriert in Zellmembranen, bildet Ionenporen und aktiviert Mastzellen durch direkten Membranschaden. Melittin aktiviert auch Phospholipase A2, was die entzuendliche Kaskade weiter verstaerkt.",
        difficulty = 4,
        funFact = "Melittin wird in der medizinischen Forschung als potenzielle Nanopartikel-Plattform gegen Krebszellen untersucht."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie funktioniert das Atemungssystem von terrestrischen Insekten auf zellulaerer Ebene?",
        answerA = "Offenes Kreislaufsystem transportiert O2-gesaettigte Haemolymphe zu allen Geweben",
        answerB = "Tracheensystem leitet O2 direkt per Diffusion und Konvektion zu Zellen ueber verzweigte Luft-Roehren bis zu Tracheolen mit Fluessigkeitssaeule",
        answerC = "Kiemenartige Strukturen im Abdomen bei allen terrestrischen Insekten",
        answerD = "Haemoglobin-aequivalente Proteine in Haemocyten transportieren O2",
        correctAnswer = 1,
        explanation = "Das Tracheensystem ist einzigartig: Luft tritt ueber Stigmen (Atemlocher) ein und verteilt sich durch verzweigte Tracheen bis zu mikroskopischen Tracheolen (<1 µm Durchmesser). An den Zellmembranen erfolgt O2-Transport ueber eine Fluessigkeitssaeule per Diffusion. Bei aktiven Insekten unterstuetzt Ventilaion den Gasaustausch.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Was beschreibt die 'Instar'-Terminologie bei der Insektenentwicklung?",
        answerA = "Die Gesamtzahl der Larvalstadien eines Insekts ueber sein gesamtes Leben",
        answerB = "Das Larvenstadium zwischen zwei aufeinanderfolgenden Haeutungen",
        answerC = "Das erste Larvenstadium nach dem Schluepfen aus dem Ei (L1)",
        answerD = "Den Zustand der Puppe vor der Imaginalhaeute",
        correctAnswer = 1,
        explanation = "Ein Instar bezeichnet das Stadium zwischen zwei Haeutungen (Ecdysen). Das erste Larvalstadium nach dem Ei ist Instar I (L1), nach der ersten Haeutung Instar II usw. Die Anzahl variiert: Heuschrecken 5, Schmetterlingslarvn meist 5, manche Eintagsfliegen bis zu 25-30 Instare.",
        difficulty = 4,
        funFact = "Eintagsfliegen durchlaufen als einzige rezente Insekten eine Haeutung als gefluegeltes Imago — die subimaginal Haeutung vor der finalen Form."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher molekulare Mechanismus liegt der Pheromonsynthese beim Seidenspinner (Bombyx mori) zugrunde und welche Verbindung ist das primaere Lockstoff-Molekuel?",
        answerA = "Bombykol (E-10, Z-12-Hexadecadienol) — synthesisiert durch Delta-11 Desaturase aus Palmitoyl-CoA",
        answerB = "Bombykol (Z-7, Z-11-Hexadecadienol) — synthesisiert durch Acetyl-CoA-Carboxylase-Kaskade",
        answerC = "Bombykal (E-10, Z-12-Hexadecenal) — als Aldehyd-Oxidationsprodukt aus Fettsaeure",
        answerD = "(E)-beta-Farnesol — sesquiterpenoid aus dem Mevalonat-Weg",
        correctAnswer = 0,
        explanation = "Bombykol ist (E10, Z12)-Hexadecadien-1-ol. Die Biosynthese beginnt mit Palmitoyl-CoA, das durch Delta-11 Desaturase desaturiert, dann durch eine weitere Desaturase prozessiert, und schliesslich zur Alkohol-Funktionsgruppe reduziert wird. Karasawa identifizierte es 1959 als erstes Insektenpheromon ueberhaupt.",
        difficulty = 4,
        funFact = "Seidenspinnermaennchen koennen Bombykol-Molekuele in einer Konzentration von wenigen hundert Molekuelen pro Kubikzentimeter Luft wahrnehmen."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Rolle spielen 'Imaginalscheiben' bei der holometabolen Metamorphose von Drosophila?",
        answerA = "Reservepools totipotenter Stammzellen die waehrend der Larvalphase heranwachsen und in der Puppe adulte Strukturen bilden",
        answerB = "Pigmentzellen die die adulte Faerbung programmieren",
        answerC = "Ecdyson-produzierende Druesengewebe die die Verpuppung einleiten",
        answerD = "Neurale Vorlaeuferstrukturen die das adulte Gehirn bilden",
        correctAnswer = 0,
        explanation = "Imaginalscheiben sind epitheliale Monolayer die sich schon im Larvalstadium ausbilden (ca. 40 000 Zellen bei adulten L3-Larven). Jede Scheibe ist einer Koerperstruktur zugeordnet (z.B. Augenscheibe, Flugelscheibe). Waehrend der Metamorphose evaginieren sie und bilden praezise die entsprechenden adulten Strukturen.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welches seltene Phaenomen beschreibt die Kommunikation zwischen Honigbienen-Koeniginnen im Wabenwachs durch Vibrationssignale?",
        answerA = "Schwarmvorbereitung durch Tooting und Quacking",
        answerB = "Royale Erbfolge-Duell durch Abdomen-Trommeln",
        answerC = "Trophallaxis-Koordinierung durch Antennen-Kontaktsignale",
        answerD = "Tanzkommunikation adaptiert fuer Koeniginnen",
        correctAnswer = 0,
        explanation = "'Tooting' (Pfeifen) erzeugt die geschluepfte Koenigin durch Vibration des Brustkorbs — es warnt Konkurrentinnen noch in der Weiselzelle. 'Quacking' antworten die noch nicht geschluepften Rivalinnen. Dieses akustische Duell entscheidet mit ueber die Thronfolgerin — Schwarmverhalten ist eng damit verknuepft.",
        difficulty = 4,
        funFact = "Wenn eine Koenigin quotkt und die Arbeiterinnen ihr zustimmen, verlassen sie das Volk mit ihr als Schwarm."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Insektenordnung umfasst die Trichoptera (Koecherfliegen) und was ist ihre naechste lebende Verwandtschaft?",
        answerA = "Naeher verwandt mit Diptera durch gemeinsamen Flugapparat",
        answerB = "Schwestergruppe der Lepidoptera (Schmetterlinge) — gemeinsame Klade Amphiesmenoptera",
        answerC = "Basalste holometabole Insekten naeher verwandt mit Neuroptera",
        answerD = "Schwestergruppe der Mecoptera (Schnabelhafte) durch Schaedelmorphologie",
        correctAnswer = 1,
        explanation = "Trichoptera und Lepidoptera bilden die Klade Amphiesmenoptera. Beide Ordnungen teilen: Schuppen auf Fluegeln und Koerper (reduziert bei Trichoptera), hakenfoermige Genitalstrukturen und gemeinsame molekulare Marker. Ihr letzter gemeinsamer Vorfahre existierte in der Trias vor ~250 Mio. Jahren.",
        difficulty = 4,
        funFact = "Koecherfliegenlarven bauen individuelle Schutzgehaeuse aus Sand, Blattpartikeln oder Steinchen — jede Art mit charakteristischem Baustil."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist 'Hyperparasitoidismus' und wo kommt er in der Insektenbiologie vor?",
        answerA = "Parasitismus einer Parasitoiden-Art durch eine weitere parasitoide Art",
        answerB = "Massiver Parasitismus bei dem ein Wirt von hunderten Larven gleichzeitig bewohnt wird",
        answerC = "Parasitismus bei dem der Parasitoid den Wirt ohne Toetung nutzt",
        answerD = "Kleptoparasitismus bei dem eine Wespe die Beute anderer Wespen stiehlt",
        correctAnswer = 0,
        explanation = "Hyperparasitoidismus bedeutet, dass ein Parasitoide selbst von einer anderen parasitoiden Art parasitiert wird. Chalcidoide Wespen wie Lysibia nana parasitieren Braconid-Wespen (z.B. Cotesia glomerata), die ihrerseits Schmetterlingsraupen parasitieren. Vier- und fuenffache Hyperparasitismus-Kaskaden sind dokumentiert.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Gift-Peptid der Konusschnecke (Conus geographus) wird als Pharmazeutikum eingesetzt und warum ist es entomologisch relevant?",
        answerA = "Conotoxin MVIIA (Ziconotid) blockiert N-Typ Ca2+-Kanaele — relevant fuer Kanalforschung in Insektenneuronen",
        answerB = "Omega-Conotoxin wirkt ausschliesslich auf Vertebraten-Neuronen und ist entomologisch irrelevant",
        answerC = "Conantokin G als NMDA-Antagonist hat direkte Insektizid-Wirkung",
        answerD = "Conotoxin GI blockiert nikotinische Acetylcholin-Rezeptoren — identische Struktur zu Insekten-Neonikotinoide",
        correctAnswer = 0,
        explanation = "Ziconotid (omega-Conotoxin MVIIA) ist ein FDA-zugelassenes Analgetikum zur intrathekalen Schmerztherapie. Es blockiert N-Typ spannungsgesteuerte Ca2+-Kanaele (Cav2.2). Conotoxine sind wichtige Werkzeuge in der Insekten-Neurobiologie, da viele Kanaltypen zwischen Invertebraten und Vertebraten konserviert sind.",
        difficulty = 4,
        funFact = "Conus geographus ist trotz ihres unscheinbaren Aussehens einer der giftigsten Meeresorganismen — das Gift kann Menschen in Minuten toeten."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist die 'Brood Parasitism' bei Kuckuckswespen (Chrysididae) und durch welchen Mechanismus entgeht das Ei der Abwehr des Wirtes?",
        answerA = "Kuckuckswespen legen Eier in Solitaerwespen-Nester und das Ei imitiert chemisch den Geruch der Wirtslarve",
        answerB = "Das Chrysididae-Ei besitzt eine extrem harte Chorionschicht die mechanische Attacken des Wirtes uebersteht",
        answerC = "Chemische Tarnkappe: Das Ei sezerniert Kutikula-Kohlenwasserstoffe des Wirts um Erkennung zu vermeiden",
        answerD = "Schnelle Entwicklung ermoeglicht dem Parasitoiden das Schluepfen bevor die Wirtslarve gross genug zur Gegenwehr ist",
        correctAnswer = 0,
        explanation = "Chrysididae-Weibchen legen Eier in Nester von Solitaerwespen (Vespidae, Crabronidae). Der Schluepfling frisst die Wirtslarve. Die Tarnung erfolgt chemisch: Das Ei produziert Kutikula-Kohlenwasserstoffe die dem Geruchsprofil der Wirtslarve aehneln. Manche Arten sezernieren zusaetzlich Beruhigungs-Allomone.",
        difficulty = 4,
        funFact = "Kuckuckswespen koennen sich bei Angriff zu einer Kugel zusammenrollen — die extrem harte Kutikula schuetzt vor Stichen der Wirtin."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie unterscheiden sich 'Calamistrum' und 'Cribellum' bei Cribellat-Spinnen funktionell?",
        answerA = "Calamistrum ist ein Hakenkamm auf Tarsus IV fuer Wollseide-Kaemmen; Cribellum ist eine spezialisierte Spinndruesen-Oeffnungsplatte",
        answerB = "Beide sind Warnfarbenmuster — Cribellum ist dorsal, Calamistrum ventral",
        answerC = "Calamistrum produziert Seide; Cribellum kaemmt die Seide zu Fangfaeden",
        answerD = "Beide sind taxonomische Synonyme fuer die gleiche Struktur",
        correctAnswer = 0,
        explanation = "Das Cribellum ist eine perforierte Platte vor den Spinnwarzen mit hunderten winziger Seidentuellenoffnungen die cribellatseide produziert. Das Calamistrum ist eine Borstenreihe auf dem Metatarsus des vierten Beinpaares die diese visko-elastische Nanofaser-Seide zu einer Hakenfangflaeche kaemmt.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Insekt besitzt das komplexeste bekannte Zusammengesetzte Auge (Komplexauge) mit der hoechsten Anzahl an Ommatidien?",
        answerA = "Dragonfly (Anisoptera) mit bis zu 30.000 Ommatidien und nahezu 360-Grad-Sehen",
        answerB = "Mantis shrimp (Stomatopoda) mit 16 Photorezeptortypen",
        answerC = "Honigbiene (Apis mellifera) mit 6.900 Ommatidien pro Auge",
        answerD = "Tagfalter (Papilionidae) mit UV-sensitiven Fotorezeptoren in allen Ommatidien",
        correctAnswer = 0,
        explanation = "Libellen (Anisoptera) haben bis zu 30.000 Ommatidien pro Auge und fast vollstaendige Hemisphaerenabdeckung. Der dorsale Bereich hat grosse Fazetten fuer Hochaufloesungssehen nach vorne und oben (zur Beutejagd), der ventrale Bereich kleinere Fazetten fuer periphere Bewegungsdetektion.",
        difficulty = 4,
        funFact = "Libellen fangen ca. 95% ihrer Beute im Flug — die hoechste Jagderfolgsrate aller bekannten Raeubertiere."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist 'Diapause' bei Insekten und durch welchen neuroendokrinen Mechanismus wird sie ausgeloest?",
        answerA = "Ruhezustand aehnlich Hibernation bei Warmbluetigen — ausgeloest durch Temperaturabfall",
        answerB = "Genetisch programmierter Entwicklungsstillstand ausgeloest durch photoperiodische Signale ueber Neuropeptide (PTTH-Suppression und DH/PBAN-System)",
        answerC = "Nahrungsinduzierter Fastenstoffwechsel bei Nahrungsknappheit",
        answerD = "Passive Kaeltestarre (Kaeltelethargie) die reversibel ist sobald Temperatur steigt",
        correctAnswer = 1,
        explanation = "Diapause ist eine genetisch determinierte Entwicklungshemmung. Die Photoperiode wird ueber den zirkadianen Clock (period, timeless Gene) registriert. Kurze Tage supprimieren PTTH (Prothorakotropes Hormon), wodurch die Ecdyson-Produktion unterbunden wird. Diapause-Hormon (DH) und PBAN steuern je nach Stadium und Art.",
        difficulty = 4,
        funFact = "Monarchfalter unterbrechen ihre Migration durch fakultative Diapause in Mexiko — eine der laengsten Insekten-Wanderungen der Welt."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche spezifischen Druesenstrukturen produzieren Zentraleuropaeische Blattwespen (Diprionidae) als Defensivsekrete und was ist ihre chemische Natur?",
        answerA = "Tentorium-Druesen die Ameisensaeure produzieren",
        answerB = "Spezialisierte Oesophagus-Divertikeln (Cropsaecke) die Terpene aus Kiefernnadeln speichern und als Erbrochenes abgeben",
        answerC = "Ventralabdominale Druesenpaare die Chinonemissionen produzieren",
        answerD = "Modifizierte Malpighi-Gefaesse die Oxalsaeure sezernieren",
        correctAnswer = 1,
        explanation = "Diprionidae-Larven speichern in modifizierten Oesophagus-Divertikeln Monoterpene aus ihrer Kiefernnadel-Nahrung an. Bei Stoerung wuergen sie den harzig-stinkenden Terpenmix auf Raeubern aus. Diese Sequestration von Pflanzenstoffen als Defensivwaffe ist ein Beispiel fuer pharmakal Oekologie.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Gruppe der Milben (Acari) ist ausschliesslich parasitisch und hat eine direkte medizinische Relevanz durch Uebertragung von FSME und Borreliose?",
        answerA = "Trombidiformes (Erntemiilben, Demodex)",
        answerB = "Sarcoptiformes (Kraetzemiilben)",
        answerC = "Ixodida (Schildzecken — Ixodes ricinus und Verwandte) als Vektoren",
        answerD = "Mesostigmata (Raubmilben die Blut saugen)",
        correctAnswer = 2,
        explanation = "Ixodida (Schildzecken) insbesondere Ixodes ricinus (Gemeiner Holzbock) sind Hauptvektoren fuer FSME (Fruehsommer-Meningoenzephalitis durch Flavivirus) und Lyme-Borreliose (Borrelia burgdorferi). Zecken nehmen Erreger im Larvalstadium auf und uebertragen sie in spaeteren Stadien beim Blutmahlzeitsaugen.",
        difficulty = 4,
        funFact = "Ixodes ricinus kann bis zu 3 Jahre ohne Nahrungsaufnahme ueberleben und ist damit eines der ausdauernsten Ektoparasiten."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist 'Cyclorrhapha' und welche morphologische Synapomorphie definiert diese Gruppe innerhalb der Diptera?",
        answerA = "Unterordnung der Fliegen mit zwei Fluegeln — definiert durch Halterscherchen (Halteren) statt Hinterfluegel",
        answerB = "Gruppe der Fliegenlarven die sich in einer runden Puppentonne (Puparium) verpuppen — charakteristisches Ptilinum beim Schluepfen",
        answerC = "Alle Diptera mit beissenden Mundwerkzeugen im adulten Stadium",
        answerD = "Fliegen mit Schwebflug-Faehigkeit durch spezifische Flugelgelenkmodifikationen",
        correctAnswer = 1,
        explanation = "Cyclorrhapha ist eine Diptera-Gruppe deren Larven sich in einer sklerotisierten Puppentonne (Puparium) aus der letzten Larvalhaut verpuppen. Beim Schluepfen wird ein aufblasbares Kopforgan (Ptilinum) durch Blutdruck ausgefahren um das runde Deckelchen des Pupariums aufzusprengen.",
        difficulty = 4,
        funFact = "Das Ptilinum zieht sich nach dem Schluepfen wieder in den Kopf zurueck und hinterlaesst die charakteristische Ptilinalnaht als fossiles Merkmal."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Prinzip beschreibt bei Termiten (Isoptera/Blattodea) die Entstehung von Kasten ohne genetische Determination?",
        answerA = "Epigenetische Determination durch DNA-Methylierung in sensiblen Larvalstadien",
        answerB = "Polyphaenismus durch flexible Kastendetermination — gesteuert durch Pheromone, Nahrungsqualitaet und JH-Titer waehrend der Haeutungen",
        answerC = "Genotypisch fixierte Kasten wie bei Hymenoptera",
        answerD = "Thermische Kastendetermination analog zur Geschlechtsbestimmung bei Reptilien",
        correctAnswer = 1,
        explanation = "Termiten haben keine genetisch fixierten Kasten. Alle Individuen sind pluripotent bis zu bestimmten Larvalstadien. Koeniginpheromone (z.B. royalacin bei Macrotermes), Soldierpheromonre und Nahrungsqualitaet steuern ueber JH-Spiegel die Differenzierung in Soldaten, Arbeiter oder Geschlechtstiere.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welche einzigartige Fortbewegungsmethode nutzt die Wasserlaeuferfamilie Gerridae und welche physikalische Eigenschaft ermoeglicht sie?",
        answerA = "Hydrophobie durch Wachskutikula erhaelt Luftpolster unter den Beinen — Oberflachenspannung traegt das Gewicht",
        answerB = "Muskulaere Beinschuebe erzeugen Kapillarwellen die vorwaerts treiben",
        answerC = "Spezielle Tarsalsaugnäpfe haften an der Wasseroberflaeche durch van-der-Waals-Kraefte",
        answerD = "Wasserabstossende Mikrostruktur der Tarsalhaare erzeugt Oberflaechen-Kavitation als Vortrieb",
        correctAnswer = 0,
        explanation = "Gerridae nutzen die Oberflachenspannung des Wassers. Ihre Beine sind mit hydrophoben, nanostrukturierten Mikrohaaren (Setae) bedeckt die Luftpolster einschliessen. Das Eintauchen der Beine wuerde mehr Energie erfordern als die Oberflachenspannung erlaubt. Sie verteilen ihr Gewicht auf grosse Beinflaeche.",
        difficulty = 4,
        funFact = "Wasserlaeufer erzeugen mit den Mittelbeinen kleine Wirbel im Wasser und nutzen den Reaktionsauftrieb dieser Kapillarwellen als Vortrieb."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche phylogenetische Analyse belegt, dass Insekten (Hexapoda) aus einem aquatischen Crustaceen-Vorfahren abstammen?",
        answerA = "Morphologische Aehnlichkeiten mit Decapoden bei Larven",
        answerB = "Molekulare Phylogenomik (18S rRNA, Hox-Gene) belegt Remipedia als naechste Crustaceen-Verwandte der Hexapoda",
        answerC = "Fossiler Nachweis aus dem Kambrium zeigt direkte Zwischenstufen",
        answerD = "Biochemische Aehnlichkeit der Haemolymphe mit Meerescrustaceen",
        correctAnswer = 1,
        explanation = "Phylogenomische Studien (Regier et al. 2010, Oakley et al. 2013) belegen Remipedia (blinde, hoehlenbewohnende marine Crustaceen) als Schwestergruppe der Hexapoda innerhalb der Tetraconata/Pancrustacea. Gemeinsam sind: neuronale Architektur, spezifische Hox-Genexpression und Ommatidia-Typ.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Was versteht man unter 'Synapomorphie' in der kladistischen Systematik der Arthropoda und welche Rolle spielt sie bei der Klassifikation von Insekten?",
        answerA = "Konvergente Merkmale die durch Parallelevolution entstanden und Taeuschen koennen",
        answerB = "Gemeinsam abgeleitetes Merkmal das von einem gemeinsamen Vorfahren ererbt wurde und eine monophyletische Gruppe definiert",
        answerC = "Urspruengliches (plesiomorphes) Merkmal das im gesamten Stammbaum vorkommt",
        answerD = "Autapomorphie eines einzelnen Taxons ohne Verwandtschaftsbedeutung",
        correctAnswer = 1,
        explanation = "Synapomorphien sind gemeinsam abgeleitete Merkmale (Apomorphien) die bei einer Gruppe von Organismen auftreten und deren gemeinsamen Vorfahren charakterisieren. Beispiel: Die Entwicklung von Fluegeln (Pterygota) ist eine Synapomorphie aller gefluegelten Insekten. Willi Hennigs Kladistik basiert fundamental auf diesem Konzept.",
        difficulty = 4,
        funFact = "Fliegen (Diptera) verloren ihre Hinterfluegel sekundaer — die Halteren sind keine neue Struktur, sondern modifizierte Hinterfluegel."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Funktion haben 'Trichobothrien' bei Arachnida und in welcher Ordnung sind sie besonders gut ausgebildet?",
        answerA = "Chemorezeptoren fuer Pheromondetektion — besonders bei Skorpionen",
        answerB = "Extrem empfindliche Mechanorezeptoren fuer Luftstroemungen und Vibrationen — besonders bei Amblypygi und Uropygida ausgepraegte Anordnung",
        answerC = "Hygrorezepioren fuer Luftfeuchtigkeitsmessung — alle Arachnida gleichmaessig",
        answerD = "Photorezeptive Haare fuer UV-Wahrnehmung — besonders bei Skorpionen",
        correctAnswer = 1,
        explanation = "Trichobothrien sind lange, duenne Haare in Gelenkgruben die auf kleinste Luftstroemungen reagieren. Sie registrieren Frequenzen, Intensitaet und Richtung von Luftbewegungen durch herannahende Beute oder Feinde. Bei Amblypygi (Geisselspinnen) und Uropygida (Geisselscorpione) sind sie auf den verlengerten ersten Laufbeinen extrem dicht und spezialisiert.",
        difficulty = 4,
        funFact = "Trichobothrien koennen Luftbewegungen von weniger als 0,001 mm/s wahrnehmen — damit uebersteigen sie die Empfindlichkeit moderner Windmessgeraete."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist 'Kleptoparasitismus' bei Spinnen und welche Art ist dafuer als klassisches Beispiel bekannt?",
        answerA = "Spinnen die andere Spinnennetze bestehlen und gefangene Beute stehlen",
        answerB = "Argyrodes-Spinnen (Theridiidae) die als Kommensalen in Netzen von Grossspinnen leben und Reste und kleine Beute konsumieren",
        answerC = "Spinnen die Wirtskoerper parasitieren ohne sie zu toeten",
        answerD = "Spinnen die Pheromone anderer Spinnenarten imitieren um Maennchen anzulocken und zu fressen",
        correctAnswer = 1,
        explanation = "Argyrodes-Arten (Schmarotzerspinnen) leben kleptoparasitisch in den Netzen von Nephila und anderen Grossspinnen. Sie stehlen kleinge Beute aus dem Netz und konsumieren aufgewickelten Fang des Wirtes. Manche Argyrodes-Arten konsumieren sogar Haemolymphe des Wirts direkt.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist der Unterschied zwischen 'ecribellat' und 'cribellat' bei Spinnen in Bezug auf die Fangseideneigenschaften?",
        answerA = "Cribellatseide ist nasser und klebriger durch Viscin-Tropfen; ecribellate Seide ist trocken und nicht kleberig",
        answerB = "Cribellate Spinnen produzieren mechanisch kleberige Wolle-Seide durch Nanotubuli ohne Klebstofftropfen; ecribellate Spinnen haben fluessige Klebstoffe auf Fangfaeden",
        answerC = "Ecribellate Spinnen produzieren keine Seide; cribellatae nutzen ausschliesslich mechanische Fallen",
        answerD = "Beide Seidentypen sind chemisch identisch und unterscheiden sich nur in der Produktionsdruesenanzahl",
        correctAnswer = 1,
        explanation = "Cribellatseide besteht aus hunderten Nanofasern, die vom Calamistrum zu einem viskosen, mechanisch klebenden Wolle-Faden verkaemmt werden. Die Haftung basiert auf Kapillarkraeften und Nanofaser-Verhakung (kein Klebstoff). Ecribellate Fangfaeden haben Aggregate-Druesensekret als fluessige Klebestofftropfen auf der Axilfaser.",
        difficulty = 4,
        funFact = "Cribellatseide ist so effektiv dass selbst glatte Insektenkutikulae durch die Nanofasern festgehalten werden — unabhaengig von Klebstoff."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Besonderheit zeigt die Entwicklung der Faecher- bzw. Stabfluegelinsekten (Strepsiptera) die sie zu einem entomologischen Raritaetskabinett macht?",
        answerA = "Sie sind die einzigen Insekten mit vollstaendiger Metagenese (vier Stadien) ohne Puppe",
        answerB = "Weibchen bleiben als Endoparasiten lebenslang im Wirtskörper und schieben nur den Vorderkopf (Cephalothorax) heraus — hypermetabole Entwicklung mit triungulin-Larven",
        answerC = "Sie sind die einzigen Insekten die parthenogenetisch haploide Maennchen und diploide Weibchen produzieren",
        answerD = "Imagines sterben sofort nach der Geschlechtsreife — Lebensdauer nur 2-3 Minuten",
        correctAnswer = 1,
        explanation = "Strepsiptera zeigen extreme Sexualdimorphie: Maennchen sind freilebend mit gefiederten Antennen und Hinterfluegeln; Weibchen sind lebenslang endoparasitisch im Wirt (Bienen, Wespen, Heuschrecken) und schieben nur den Cephalothorax heraus. Die Larven (Triungulinae) sind freibewegliche mobile Erstlarven.",
        difficulty = 4,
        funFact = "Strepsiptera-infizierte Wespen werden als 'stylopized' bezeichnet — ihre Reproduktion wird vollstaendig unterdrueckt durch den Parasiten."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche biomechanische Eigenschaft erklaert, warum Krabbenspinnen (Thomisidae) keine Fangnetz bauen, aber dennoch grosse Beute (Bienen, Hummeln) ueberwältigen?",
        answerA = "Extrem schnelle Chelizeren die in Millisekunden paralysierende Gifte injizieren bevor die Beute reagieren kann",
        answerB = "Ventraler Hinterhalt kombiniert mit kryptischer Faerbung und sofortigem Gifteinsatz am Tentorium des Opfers",
        answerC = "Pedipalpen die als Greifwerkzeuge fungieren und die Beute in einer Halb-Nelson-Umklammerung halten",
        answerD = "Elektrostatische Adhaetion der Tarsalhaare ermoeglicht es grosse Insekten festzuhalten",
        correctAnswer = 1,
        explanation = "Krabbenspinnen tarnen sich kryptisch auf Blueten (Melanin-Regulation erlaubt Farbwechsel zwischen weiss und gelb). Die Beute wird von ventral im Hinterhalt ueberrascht. Sofort nach dem Erstkontakt werden Chelizeren in das Tentorium (Schadel) oder den Thorax gestochen und schnell wirkende Neurotoxine injiziert. Der Ablauf dauert <100 Millisekunden.",
        difficulty = 4,
        funFact = "Misumena vatia kann ihre Koeperfarbe in 10-20 Tagen von weiss zu gelb oder zurueck aendern — gesteuert durch optische Wahrnehmung des Blutenfarbtons."
    ),

)
