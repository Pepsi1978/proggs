package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun animalQuestionsExpert6(): List<Question> = listOf(

    // EXPERT (difficulty = 4) -- Conservation Biology and Population Genetics

    Question(
        categoryId = 9,
        questionText = "Was bezeichnet die minimale lebensfaehige Population (MVP) in der Naturschutzbiologie?",
        answerA = "Die groesste Population, die ein Habitat tragen kann",
        answerB = "Die kleinste Populationsgroesse, bei der eine Art mit 95 % Wahrscheinlichkeit 100 Jahre ueberleben kann",
        answerC = "Die genetisch optimale Bestandsgroesse fuer maximale Fitness",
        answerD = "Die Populationsgroesse unterhalb des Extinktionsvortex",
        correctAnswer = 1,
        explanation = "Die MVP (Minimum Viable Population) ist die kleinste isolierte Population, die mit einer festgelegten statistischen Wahrscheinlichkeit (klassisch 95 % ueber 100 Jahre) ueberleben kann. Sie wurde von Shaffer (1981) definiert und ist ein Kerninstrument der Populationsviabilitaetsanalyse.",
        difficulty = 4,
        funFact = "Fuer die meisten Saeugetiere liegt die MVP zwischen 500 und 5.000 Individuen, je nach Lebensgeschichte und Umweltvariabilitaet."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie berechnet sich die effektive Populationsgroesse (Ne) bei einem gleichen Verhaeltnis bruetender Maennchen (Nm) und Weibchen (Nf)?",
        answerA = "Ne = (Nm + Nf) / 2",
        answerB = "Ne = Nm x Nf",
        answerC = "Ne = 4 x Nm x Nf / (Nm + Nf)",
        answerD = "Ne = (Nm x Nf) / (Nm + Nf)",
        correctAnswer = 2,
        explanation = "Die klassische Wright-Formel lautet Ne = 4NmNf / (Nm + Nf). Bei ausgeglichenem Geschlechterverhaeltnis vereinfacht sich dies zu Ne = N (Gesamtzahl der Bruetenden). Bei stark ungleichem Verhaeltnis wie beim Elefantenrobbe dominiert der seltene Brueter.",
        difficulty = 4,
        funFact = "Beim Nördlichen Seeelefanten reproduzieren wenige dominante Bullen mit Hunderten von Kuehen -- Ne betraegt nur etwa 4 % der Zensuspopulation."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche genetische Konsequenz ist beim Geparden (Acinonyx jubatus) durch Allozymelektrophorese und Hauttransplantationsversuche nachgewiesen worden?",
        answerA = "Extrem hohe Heterozygotie durch Hybridisierung",
        answerB = "Nahezu vollstaendige genetische Uniformitaet infolge eines Flaschenhalsereignisses",
        answerC = "Balancierte Polymorphismen im MHC-Komplex",
        answerD = "Erhoehte somatische Mutationsrate durch UV-Exposition",
        correctAnswer = 1,
        explanation = "O'Brien et al. (1985) zeigten, dass Geparden praktisch keine Variabilitaet in Allozym-Loci aufweisen. Hauttransplantate zwischen nicht verwandten Individuen wurden akzeptiert -- ein Zeichen extremer MHC-Uniformitaet, die auf einen schweren Flaschenhals vor ca. 10.000--12.000 Jahren hinweist.",
        difficulty = 4,
        funFact = "Wegen ihrer genetischen Uniformitaet sind Geparden besonders anfaellig fuer Infektionskrankheiten wie das Feline Coronavirus, das in Gefangenschaftsgruppen zu Massensterben fuehren kann."
    ),

    Question(
        categoryId = 9,
        questionText = "Der Florida-Panther (Puma concolor coryi) zeigte in den 1990er Jahren schwere Inzuchtdepressionen. Welche spezifische genetische Rettungsmassnahme wurde 1995 eingeleitet?",
        answerA = "Klonierung von Individuen aus kryokonserviertem Gewebe",
        answerB = "Einkreuzung von acht texanischen Pumas (Puma concolor stanleyana)",
        answerC = "Gruendung einer Ex-situ-Kolonie in einem Wildgehege",
        answerD = "Assortative Paarungsprogramme zur Selektion auf Heterozygotie",
        correctAnswer = 1,
        explanation = "1995 wurden acht weibliche Pumas aus Texas in Florida freigelassen. Dies fuehrte zu messbaren Verbesserungen: Anstieg der Spermienmotilitaet, Rueckgang von Kryptorchismus und Herzklappenverdickungen sowie erhoehte Jungenueberlebensrate -- ein Lehrbuchfall fuer Genetic Rescue.",
        difficulty = 4,
        funFact = "Vor der Einkreuzung wiesen ueber 90 % der maennlichen Florida-Panther Hodenanomalien auf -- ein direktes Symptom der Inzuchtdepression."
    ),

    Question(
        categoryId = 9,
        questionText = "Was versteht man unter dem Extinktionsvortex in der Naturschutzbiologie?",
        answerA = "Den Punkt, an dem Ne unter die MVP-Schwelle faellt",
        answerB = "Eine positive Rueckkopplungsspirale, bei der demographische, genetische und Umweltfaktoren kleine Populationen immer tiefer in Richtung Aussterben ziehen",
        answerC = "Den Verlust einer Schluesselpraedatorart mit kaskadenartigen Folgen",
        answerD = "Die minimale Patch-Groesse eines Habitatfragments",
        correctAnswer = 1,
        explanation = "Gilpin & Soule (1986) beschrieben den Extinktionsvortex als sich verstaerkende Wechselwirkung zwischen kleiner Populationsgroesse, erhoehter Inzucht, genetischem Drift, demographischer Stochastizitaet und Umweltzufaelligkeiten, die das Aussterberisiko exponentiell steigern.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Wie lautet die Faustregel des 50/500-Konzepts in der Populationsgenetik des Naturschutzes?",
        answerA = "50 Individuen verhindern demographisches Aussterben; 500 verhindern genetische Erosion",
        answerB = "Ne >= 50 zur Vermeidung kurzfristiger Inzuchtdepression; Ne >= 500 zur Aufrechterhaltung langfristigen Evolutionspotenzials",
        answerC = "50 % Heterozygotie muss erhalten bleiben; 500 Allele pro Locus gelten als optimal",
        answerD = "50 Subpopulationen fuer Metapopulationsdynamik; 500 Migranten pro Generation",
        correctAnswer = 1,
        explanation = "Franklin (1980) und Soule (1980) etablierten Ne >= 50 als kurzfristige Schwelle (vermeidet Inzuchtdepression) und Ne >= 500 als langfristige Schwelle (erhaelt Additivvarianz fuer Evolution). Neuere Arbeiten (Frankham et al. 2014) schlagen 100/1000 vor.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist genetischer Rescue und unter welcher Bedingung kann er kontraproduktiv sein?",
        answerA = "Einkreuzung fremder Individuen; kontraproduktiv bei Outbreeding-Depression durch Co-Adaptation",
        answerB = "Kryokonservierung von Keimzellen; kontraproduktiv bei zu geringer Lagertemperatur",
        answerC = "Translokation ganzer Populationen; kontraproduktiv in fragmentierten Lebensraeumen",
        answerD = "Selektive Zucht auf Heterozygotie; kontraproduktiv bei fehlangepassten Genotypen",
        correctAnswer = 0,
        explanation = "Genetic Rescue erhoeht Ne und Heterozygotie durch Zuwanderung. Outbreeding-Depression kann auftreten, wenn gut co-adaptierte lokale Genkomplexe durch fremde Allele aufgebrochen werden -- besonders relevant bei lokal adaptierten Isolatpopulationen.",
        difficulty = 4,
        funFact = "Beim Berggorilla in Uganda zeigte sich nach Zuwanderung eines Maennchens aus einer genetisch divergenten Gruppe ein messbarer Anstieg der Ueberlebensrate der Jungtiere."
    ),

    Question(
        categoryId = 9,
        questionText = "Die IUCN-Kategorie 'Critically Endangered' (CR) erfordert nach Kriterium A einen Populationsrueckgang von mindestens wie viel Prozent ueber welchen Zeitraum?",
        answerA = "30 % ueber 10 Jahre oder 3 Generationen",
        answerB = "50 % ueber 10 Jahre oder 3 Generationen",
        answerC = "80 % ueber 10 Jahre oder 3 Generationen",
        answerD = "90 % ueber 5 Jahre oder 2 Generationen",
        correctAnswer = 2,
        explanation = "IUCN-Kriterium A2 klassifiziert eine Art als CR, wenn ein Populationsrueckgang von >= 80 % ueber 10 Jahre oder 3 Generationen (je nachdem, was laenger ist) beobachtet, geschaetzt oder projiziert wurde und die Ursachen noch nicht notwendigerweise gestoppt sind.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist Populationsviabilitaetsanalyse (PVA) und welches Softwarepaket ist der Goldstandard der Praxis?",
        answerA = "Statistische Schatzung der Wachstumsrate; Standardsoftware: MARK",
        answerB = "Stochastische Modellierung des Aussterberisikos einer Population ueber Zeit; Standardsoftware: VORTEX",
        answerC = "Genetische Stammbaumanalyse fuer Zuchtprogramme; Standardsoftware: STUDBOOK",
        answerD = "Habitateignungsmodellierung; Standardsoftware: MaxEnt",
        correctAnswer = 1,
        explanation = "PVA verwendet stochastische Simulationen, um Extinktionswahrscheinlichkeiten und mittlere Ueberlebenszeiten zu berechnen. VORTEX (Lacy & Pollak, Chicago Zoological Society) ist das am weitesten verbreitete PVA-Tool fuer Tierarten und integriert demographische, genetische und Umweltstochastizitaet.",
        difficulty = 4,
        funFact = "VORTEX wurde erstmals 1989 veroeffentlicht und ist inzwischen fuer ueber 500 bedrohte Arten angewendet worden."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche der folgenden Arten erlitt den bekanntesten dokumentierten Flaschenhals der Saeugetiergeschichte, der vor etwa 10.000-12.000 Jahren die heutige genetische Uniformitaet bewirkte?",
        answerA = "Suedlicher Weissnashornoeriger",
        answerB = "Gepard (Acinonyx jubatus)",
        answerC = "Amurtigerin (Panthera tigris altaica)",
        answerD = "Viqueroe (Vicugna vicugna)",
        correctAnswer = 1,
        explanation = "Geparden zeigen extrem niedrige genetische Variabilitaet an allen untersuchten Loci, zurueckzufuehren auf einen oder mehrere schwere Flaschenhals-Ereignisse am Ende des Pleistozaens. Dies ist eines der am besten dokumentierten Beispiele in der Erhaltungsgenetik.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Was beschreibt das Hardy-Weinberg-Gleichgewicht, und welche Bedingung ist in realen Schutzpopulationen am haeufigsten verletzt?",
        answerA = "Allel- und Genotypfrequenzen bleiben konstant; am haeufigsten verletzt: Zufallspaarung",
        answerB = "Allel- und Genotypfrequenzen bleiben nach einer Generation konstant ohne Selektion, Mutation, Migration oder genetischen Drift; am haeufigsten verletzt: unendliche Populationsgroesse",
        answerC = "Heterozygotie maximiert sich nach drei Generationen; am haeufigsten verletzt: Selektion",
        answerD = "Allelfrequenzen aendern sich proportional zur Fitnessvariation; am haeufigsten verletzt: Panmixie",
        correctAnswer = 1,
        explanation = "Das HWE gilt unter 5 Bedingungen: grosse Population, Zufallspaarung, keine Selektion, keine Mutation, keine Migration. In kleinen Schutzpopulationen ist die Bedingung 'unendliche Populationsgroesse' am staerksten verletzt -- genetischer Drift wirkt intensiv und verschiebt Allelfrequenzen zufallig.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Mechanismus erklaert, warum kleine Populationen selbst ohne Inzucht an genetischer Vielfalt verlieren?",
        answerA = "Somatische Hypermutation durch Umweltstress",
        answerB = "Genetischer Drift: zufaellige Schwankungen der Allelfrequenzen ueber Generationen",
        answerC = "Gerichtete Selektion auf Homozygotie",
        answerD = "Transposable-Element-Aktivierung bei kleinen Populationen",
        correctAnswer = 1,
        explanation = "Genetischer Drift ist der stochastische Verlust von Allelen durch zufaellige Abtastung in endlichen Populationen. Die Rate des Heterozygotieverlust betraegt 1/(2Ne) pro Generation. Bei Ne = 50 verliert eine Population 1 % ihrer Heterozygotie pro Generation.",
        difficulty = 4,
        funFact = "Bei der Inselriesenlibelle (Megalagrion xanthomelas) aus Hawaii fuehrte Drift in kleinen Restpopulationen zum Verlust von etwa 40 % der genetischen Variabilitaet innerhalb von 20 Generationen."
    ),

    Question(
        categoryId = 9,
        questionText = "Was unterscheidet Inzuchtdepression von Outbreeding-Depression?",
        answerA = "Inzuchtdepression betrifft nur F1, Outbreeding-Depression nur F2-Nachkommen",
        answerB = "Inzuchtdepression: Fitnessverlust durch Homozygotie schaedlicher rezessiver Allele; Outbreeding-Depression: Fitnessverlust durch Aufbrechen lokaler Co-Adaptationen bei Kreuzung genetisch divergenter Populationen",
        answerC = "Inzuchtdepression ist reversibel, Outbreeding-Depression ist permanent",
        answerD = "Beide Phaenomene beschreiben dasselbe, nur auf verschiedenen taxonomischen Ebenen",
        correctAnswer = 1,
        explanation = "Inzuchtdepression entsteht durch Expression deleterioerer Allele bei Homozygotie. Outbreeding-Depression tritt auf, wenn gut ko-adaptierte Genkomplexe durch Hybridisierung getrennt werden -- messbar besonders in F2, wenn Rekombination die Genkomplexe zerlegt.",
        difficulty = 4,
        funFact = "Beim Kookaburra (Dacelo novaeguineae) in fragmentierten Lebensraeumen Australiens wurde Outbreeding-Depression in F2 nach Kreuzung von Populationen beobachtet, die seit > 500 Jahren isoliert waren."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Projekt gilt als der groesste Erfolg der Wiederansiedlungsbiologie (Reintroduction Biology) in Europa?",
        answerA = "Wiederansiedlung des Wolfs in den Alpen (1995)",
        answerB = "Wiederansiedlung des Europaeischen Bisons (Wisent, Bison bonasus) in Polen nach seinem Aussterben in der Wildbahn 1927",
        answerC = "Auswilderung des Braunbaers im Trentino (1999)",
        answerD = "Reintroduktion des Luchses in den Schweizer Jura (1971)",
        correctAnswer = 1,
        explanation = "Der Wisent starb 1927 in der Wildbahn aus. Durch ein koordiniertes Zuchtprogramm auf der Basis von nur 12 Gruendertieren wurde die Art wiederhergestellt. Heute leben ueber 6.000 Wisente, davon mehr als 2.000 in freien Herden in Polen (Bialowieza), Belarus, Russland und der Ukraine.",
        difficulty = 4,
        funFact = "Alle heutigen Wisente tragen DNA von nur 12 Gruendern -- der geringste genetische Gruendereffekt aller bekannten Wiederansiedlungen eines Grosssaeugers."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches molekulare Konzept quantifiziert die genetische Verwandtschaft und ist fuer Zuchtempfehlungen in Zoo-Erhaltungsprogrammen (EEP) zentral?",
        answerA = "Nucleotiddiversitaet (pi)",
        answerB = "Verwandtschaftskoeffizient (f) und mean kinship (MK)",
        answerC = "Tajima's D als Selektion-Indikator",
        answerD = "FST-Wert zwischen Subpopulationen",
        correctAnswer = 1,
        explanation = "Mean Kinship (MK) beschreibt den mittleren Verwandtschaftskoeffizienten eines Tieres zur Gesamtpopulation. EEP-Zuchtempfehlungen priorisieren Tiere mit niedrigem MK als Elterntiere, um die Gesamtheterozygotie der ex-situ-Population zu maximieren.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welche de-Extinktions-Strategie zielt beim Wollhaarmmut (Mammuthus primigenius) auf die Erzeugung eines kaeitetoleranters Elefanten ab, ohne einen echten Mammut zu klonen?",
        answerA = "Retrosequenz-Insertion alter Mammut-DNA in Mastodontenzellen",
        answerB = "CRISPR-Cas9-Editierung von Asiatelefanten-Zellen mit ausgewaehlten Mammut-Allelen (Colossal Biosciences-Ansatz)",
        answerC = "Artuebergreifende Kernuebertragung mit permafrosterhaltenen Mammut-Kernen",
        answerD = "Selektive Rueckzuechtung aus mongolischen Elefantenrassen",
        correctAnswer = 1,
        explanation = "Colossal Biosciences (Laukatis Church, Gegr. 2021) verwendet CRISPR, um spezifische Mammut-assoziierte Allele (z.B. TRPV3 fuer Kaaeltetoleranz, Haardichte-Gene, Haemoglobin-Affinitaet) in Asiatelefanten-Stammzellen einzubauen. Ziel ist ein 'funktionales Aequivalent', kein genetisch identischer Mammut.",
        difficulty = 4,
        funFact = "Das Mammut-Haemoglobin wurde 2010 durch Grutzner et al. aus antiker DNA synthetisiert und zeigte tatsaechlich hoehere O2-Affinitaet bei Kaeltetemperaturen als Elefanten-Haemoglobin."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist das Konzept des 'Extinction Debt' (Aussterbeschuld) in der Landschaftsoekologie?",
        answerA = "Kumulative Schaden durch historische Wilderei, der zur Schuldentilgung zurechenbarer Artenverluste fuehrt",
        answerB = "Die zeitverzoegerte Extinktion von Arten, die in fragmentierten Habitaten theoretisch bereits zum Aussterben verurteilt sind, aber noch nicht ausgestorben sind",
        answerC = "Der oekonomische Schaden durch den Verlust von Oekosystemdienstleistungen bei Artenverlust",
        answerD = "Das Modell des zukuenftigen Artenverlustes bei Klimawandel-Szenarien",
        correctAnswer = 1,
        explanation = "Extinction Debt (Tilman et al. 1994) beschreibt zukuenftige Extinktionen als Folge heutiger Habitatverluste. Arten existieren noch, sind aber durch Populationsdynamik und Inzucht auf langfristiger Skala bereits zum Aussterben verurteilt. Besonders relevant fuer Schmetterlinge und Pflanzen in fragmentierten Landschaften.",
        difficulty = 4,
        funFact = "Studien in schwedischen Ackerland-Biotopen zeigen, dass 50 % der vorhandenen Schmetterlingsarten noch aus dem 19. Jahrhundert 'erben' -- ihr Aussterben ist bereits determiniert, wird aber erst in 50-100 Jahren sichtbar."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches de-Extinktions-Projekt zielt auf die Wiederherstellung der Wandertaube (Ectopistes migratorius) durch genomisches Editing der Trauerseeschwalbe (Patagioenas fasciata)?",
        answerA = "Revive & Restore mit dem 'Great Passenger Pigeon Comeback'-Projekt",
        answerB = "Colossal Biosciences mit CRISPR-Editing von Felsentauben-Stammzellen",
        answerC = "Smithsonian Institution mit Retrosequenz-Insertion in Band-tailed Pigeon-Eizellen",
        answerD = "Projekt Lazarus (Archer) mit Zellkerntransfer aus Museumsbalgpraeparaten",
        correctAnswer = 0,
        explanation = "Revive & Restore arbeitet seit 2012 an der Wandertaube. Beth Shapiro und Ben Novak sequenzierten 2017 das Wandertauben-Referenzgenom. CRISPR wird verwendet, um Schluessengene (Gefiederfarbe, Wanderverhalten, soziale Brutbiologie) in Bandtaubenzellen einzubauen.",
        difficulty = 4,
        funFact = "Die letzte Wandertaube -- Martha -- starb am 1. September 1914 im Cincinnati Zoo. Die Art sank in weniger als 50 Jahren von einer geschaetzten Population von 3-5 Milliarden auf null."
    ),

    Question(
        categoryId = 9,
        questionText = "Was misst der FST-Wert in der Populationsgenetik?",
        answerA = "Den Grad der Inzucht innerhalb einer einzelnen Population",
        answerB = "Die genetische Differenzierung zwischen Subpopulationen relativ zur Gesamtpopulation",
        answerC = "Die Mutationsrate eines bestimmten Locus",
        answerD = "Das Verhaeltnis von synonymen zu nicht-synonymen Substitutionen",
        correctAnswer = 1,
        explanation = "FST (Wright 1951) ist der Anteil genetischer Varianz, der durch Unterschiede zwischen Subpopulationen erklaert wird. FST = 0 bedeutet keine Differenzierung; FST = 1 bedeutet vollstaendige Isolation. In Naturschutzprogrammen wird FST verwendet, um Managementeinheiten (MU) zu definieren.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Konzept beschreibt eigenstaendige evolutionaere Linien, die als separate Schutzeinheiten behandelt werden sollen?",
        answerA = "Management Units (MU) nach Moritz",
        answerB = "Evolutionaer signifikante Einheiten (ESUs) nach Ryder (1986) und Moritz (1994)",
        answerC = "Demographisch unabhaengige Populationen (DIP)",
        answerD = "Biogeographische Zonen nach Wallace",
        correctAnswer = 1,
        explanation = "ESUs (Evolutionarily Significant Units) sind Populationen mit wesentlicher reproduktiver Isolation und eigenstaendiger evolutionaerer Geschichte, definiert durch reziproke Monophylie mitochondrialer DNA und signifikante Unterschiede in Allelhaeufigkeiten nukleaerer Loci (Moritz 1994).",
        difficulty = 4,
        funFact = "Der US Endangered Species Act erkennt ESUs als legale Schutzeinheiten an -- kritisch z.B. fuer Lachspopulationen (Oncorhynchus spp.) an der Pazifikkueste."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche spezifische Massnahme ist Kernelement des Iberischen Luchsprogramms (Lynx pardinus), der erfolgreichsten Felid-Artenschutzaktion des 21. Jahrhunderts?",
        answerA = "Vollstaendige Ex-situ-Erhaltung mit jaehrlicher Auswilderung",
        answerB = "Kombiniertes Zuchtprogramm (LIFE-Lynx-Projekt), Habitatwiederherstellung und genetisch gesteuerte Translokationen zwischen Kernpopulationen",
        answerC = "Genetisches Rescue durch Einkreuzung des Eurasischen Luchses (Lynx lynx)",
        answerD = "Populationsstuetzung durch Freilassung von ex-situ-Luchsen ohne genetisches Management",
        correctAnswer = 1,
        explanation = "Im Jahr 2002 gab es nur noch 94 Individuen in 2 Populationen. Das LIFE-Lynx-Projekt kombiniert Zuchtprogramme in 9 Zentren mit Habitatrestaurierung (Kaninchenpopulationen als Beute) und genetisch informierten Translokationen. 2024 wird die Population auf uber 2.000 Individuen geschaetzt.",
        difficulty = 4,
        funFact = "Der Iberische Luchs ist die einzige Wildkatzenart, deren IUCN-Status in den letzten 20 Jahren von 'Critically Endangered' auf 'Vulnerable' (2015) und schliesslich 'Endangered' herabgestuft wurde."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist ein Wildtierkorridor und welche Mindestanforderung stellt die Forschung an seine Funktionalitaet?",
        answerA = "Eine Schutzzone um Kernhabitate; Mindestbreite 100 m fuer Insekten",
        answerB = "Ein lineares Habitatelement, das isolierte Patches verbindet; Mindestbreite und -qualitaet abhaengig von der Zielart und Nachweisen tatsaechlichen Genflusses",
        answerC = "Eine Pufferzone zwischen Schutzgebieten; muss mindestens 1 km breit sein",
        answerD = "Ein kuenstlich angelegter Tunnel unter Strassen; Mindestlaenge 500 m",
        correctAnswer = 1,
        explanation = "Wildtierkorridore verbinden fragmentierte Habitate und ermoeglichen Migration, Genfluss und Wiederbesiedlung. Die Funktionalitaet wird idealerweise durch genetische Konnektivitaet (Genfluss) und tatsaechliche Tierpassagen bewertet -- die reine geometrische Verbindung genuegt nicht.",
        difficulty = 4,
        funFact = "Der 'Yellowstone to Yukon Conservation Initiative'-Korridor erstreckt sich ueber 3.200 km und gilt als das ambitionierteste Konnektivitaetsprojekt der Welt fuer Grosssaeuger."
    ),

    Question(
        categoryId = 9,
        questionText = "Welchen genetischen Marker verwendete die Forensik erstmals, um illegalen Elfenbeinhandel auf spezifische Elefantenpopulationen in Afrika zurueckzufuehren?",
        answerA = "SNP-Arrays mit 50.000 Markern",
        answerB = "Mitochondriale DNA-Haplotypen kombiniert mit 16 autosomalen Mikrosatelliten (Wasser & Okello, Science 2007)",
        answerC = "Whole-Genome-Sequenzierung aus Elfenbein-Pulverproben",
        answerD = "Y-chromosomale STR-Profile",
        correctAnswer = 1,
        explanation = "Wasser et al. (2007, Science) entwickelten eine genetische Zuordnungskarte fuer afrikanische Elefanten basierend auf mitochondrialen Haplotypen und autosomalen Mikrosatelliten. Beschlagnahmtes Elfenbein konnte damit auf spezifische geographische Ursprungsregionen (innerhalb von ~200 km) zugeordnet werden.",
        difficulty = 4,
        funFact = "Diese Methode fuehrte zur Identifikation der Demokratischen Republik Kongo und Sambia als Hauptursprungsregionen grosser Elfenbein-Schmuggelsendungen in den 2000er Jahren."
    ),

    Question(
        categoryId = 9,
        questionText = "Was beschreibt das Konzept der 'Trophic Cascade' (Trophische Kaskade) im Kontext des Rewilding?",
        answerA = "Den stufenweisen Rueckgang von Arten entlang der Nahrungskette nach Jagdruck",
        answerB = "Indirekte Veraenderungen der Vegetationsstruktur und Habitatqualitaet durch die Wiederansiedlung von Schluesselpraedatoren",
        answerC = "Die Kaskade von Artenverlust nach dem Wegfall einer Schluesselbeuteart",
        answerD = "Den Transfer von Biomasse zwischen trophischen Ebenen in fragmentierten Oekosystemen",
        correctAnswer = 1,
        explanation = "Trophische Kaskaden sind bekannt durch die Wolfrueckkehr in Yellowstone (1995): Woelfe reduzierten Waideungsverhalten von Wapiti in bestimmten Gebieten, was zur Regeneration von Weiden und Pappeln fuehrte, Biberhabitate verbesserte und letztlich Bachmorphologie veraenderte -- ein Prozess als 'landscape of fear' beschrieben.",
        difficulty = 4,
        funFact = "Nach der Wolfrueckkehr in Yellowstone erholten sich Espen (Populus tremuloides) in Flusstalbereichen innerhalb von 15 Jahren nachweisbar -- direkt messbar durch Baumringanalyse."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches statistisches Modell bildet die Grundlage der Populationsviabilitaetsanalyse (PVA) und beschreibt das Wachstum kleiner Populationen mit stochastischen Elementen?",
        answerA = "Logistisches Wachstumsmodell (Verhulst 1838)",
        answerB = "Stochastisches demografisches Matrixmodell (Leslie-Matrix mit Zufallsvariabilitaet)",
        answerC = "Lotka-Volterra-Praedator-Beute-Modell",
        answerD = "Metapopulationsmodell nach Levins (1969)",
        correctAnswer = 1,
        explanation = "PVA-Tools wie VORTEX basieren auf alters- oder stadienstrukturierten Leslie-Matrizen, erweitert um stochastische Elemente: demographische Stochastizitaet (individuelle Ueberlebens- und Reproduktionswahrscheinlichkeiten), Umweltstochastizitaet (jaehrliche Schwankungen), Katastrophen und Inzuchteffekte.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "In welchem Jahr und durch welches Ereignis verlor der Nördliche Weisshornoeriger (Ceratotherium simum cottoni) seine letzte wildbahn-lebende Gruppe?",
        answerA = "2007 durch die Krise im Virunga-Nationalpark (DRC)",
        answerB = "2011 durch intensive Wilderei im Garamba-Nationalpark (DRC), wodurch die letzte wilde Gruppe verschwand",
        answerC = "2018, als Sudan -- das letzte Maennchen -- in Ol Pejeta starb",
        answerD = "2003 nach dem Irakkrieg und destabilisierten Schutzgebieten",
        correctAnswer = 1,
        explanation = "Die letzten wilden Nördlichen Weisshornoeriger lebten im Garamba-Nationalpark (DRC). Intensive Wilderei durch bewaffnete Gruppen fuehrte bis 2011 zum Verschwinden der gesamten Wildpopulation. Die verbliebenen Individuen sind seitdem in Gefangenschaft (Ol Pejeta, Kenia).",
        difficulty = 4,
        funFact = "Sudan, das letzte bekannte maennliche Exemplar, starb am 19. Maerz 2018. Sein Sperma wurde kryokonserviert und wird fuer IVF-Versuche mit Suedlichen Weisshornoeriger-Eizellen genutzt."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Konzept beschreibt Arten, deren Schutz den Schutz vieler anderer Arten in demselben Habitat impliziert?",
        answerA = "Flaggschiffarten (Flagship Species)",
        answerB = "Schirmarten (Umbrella Species)",
        answerC = "Schluesselsteinarten (Keystone Species)",
        answerD = "Zeigerarten (Indicator Species)",
        correctAnswer = 1,
        explanation = "Umbrella Species (Schirmarten) benoetigen grosse, ungestoerte Habitatflaechen. Ihr Schutz schirmt viele andere Arten in denselben Lebensraeumen. Beispiele: Grizzlybaer, Jaguar, Wolfslachs. Zu unterscheiden von Keystone Species (ueberproportionaler oekologischer Einfluss) und Flagship Species (Botschafter fuer den Naturschutz).",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Was versteht man unter dem Allee-Effekt in der Populationsoekologie und wann wird er kritisch?",
        answerA = "Dichteunabhaengige Mortalitaet; kritisch ab 20 Individuen",
        answerB = "Reduzierte individuelle Fitness bei sehr geringer Populationsdichte; kritisch unterhalb der Allee-Schwelle, wenn per capita Wachstumsrate negativ wird",
        answerC = "Erhoehte Inzuchtrate bei Populationsgroessen unter 100; kritisch bei F > 0.25",
        answerD = "Erhoehte Praedationsrate durch fehlende Sicherheitsgruppengroesse; kritisch unter 50 Individuen",
        correctAnswer = 1,
        explanation = "Der Allee-Effekt beschreibt positive Dichte-Fitnesskopplungen: Schwarmfischerei, kooperative Brutpflege, Partnerfindung -- all das wird bei geringer Dichte schwieriger. Unterhalb der kritischen Allee-Schwelle ist die per-capita-Wachstumsrate negativ, d.h. die Population zieht unweigerlich Richtung Aussterben.",
        difficulty = 4,
        funFact = "Die Wandertaube (Ectopistes migratorius) zeigte einen extremen Allee-Effekt: Brutkolonien mussten Millionen von Voegeln umfassen, damit erfolgreiche Reproduktion stattfand."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Grundformel gibt den Verlust der Heterozygotie durch Inzucht an?",
        answerA = "Ht = H0 x (1 - 1/N)^t",
        answerB = "Ht = H0 x (1 - 1/(2Ne))^t",
        answerC = "Ht = H0 x e^(-t/2Ne)",
        answerD = "Ht = H0 / (1 + t/Ne)",
        correctAnswer = 1,
        explanation = "Der Heterozygotieverlust pro Generation betraegt 1/(2Ne). Nach t Generationen gilt Ht = H0 x (1 - 1/(2Ne))^t. Bei Ne = 50 verliert die Population pro Generation ca. 1 % ihrer Ausgangsheterozygotie; nach 100 Generationen sind rund 63 % verloren.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist Metapopulationsdynamik nach Levins (1969) und warum ist sie fuer fragmentierte Lebensraeume relevant?",
        answerA = "Das Auswanderungsverhalten von Individuen zwischen Habitatinseln unter Praedationsdruck",
        answerB = "Eine Population von Teilpopulationen, die durch lokales Aussterben und Wiederbesiedlung (patch dynamics) in einem dynamischen Gleichgewicht gehalten wird",
        answerC = "Die raeumliche Verteilung genetischer Varianz zwischen isolierten Populationen",
        answerD = "Das Modell der Inselbiographie nach MacArthur und Wilson (1967)",
        correctAnswer = 1,
        explanation = "Levins' Metapopulationsmodell beschreibt ein Gleichgewicht zwischen lokaler Extinktion und Rekolonisation. In fragmentierten Landschaften koennen kleine lokale Populationen aussterben, aber die Metapopulation persistiert durch Wiederbesiedlung -- Voraussetzung: ausreichende Konnektivitaet zwischen Patches.",
        difficulty = 4,
        funFact = "Der Goldene Leoewe-Tamarin (Leontopithecus rosalia) in der Mata Atlantica Brasiliens wurde mithilfe von Metapopulationsmodellen gezielt in geeignete Habitatfragmente wiederangesiedelt."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Begriff beschreibt die Strategie, genetisch diverse Individuen gezielt in genetisch verarmte Populationen einzufuehren, um Inzuchtdepression zu lindern?",
        answerA = "Supplementation",
        answerB = "Genetic Rescue",
        answerC = "Assisted Gene Flow",
        answerD = "Translocation",
        correctAnswer = 1,
        explanation = "Genetic Rescue (Tallmon et al. 2004) bezeichnet die messbare Fitnesssteigerung durch Einwanderung genetisch unterschiedlicher Individuen in eine inzueehtige Population. Nachgewiesen bei Florida-Panther, Inselgraufuchs (Channel Island Fox), Schwedischen Woelfen und Grossem Schilfrohrsaenger.",
        difficulty = 4,
        funFact = "Beim Grossen Schilfrohrsaenger (Acrocephalus arundinaceus) in Schweden war die Rekrutierungsrate der Jungtiere nach Einwanderung eines einzigen Maennchens aus einer anderen Population um 36 % hoeher als in der Ausgangspopulation."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Methodik ermoeglicht die nichtinvasive Genotypisierung freilebender Tiere und ist besonders wertvoll fuer scheue Grosssaeuger?",
        answerA = "Blut-Mikrosatelliten-Analyse aus Pfeilenarkose",
        answerB = "Kot-DNA-Analyse (Faecal DNA) kombiniert mit Mikrosatellitengenotypisierung",
        answerC = "eDNA-Wasserproben fuer aquatische Saeugetiere",
        answerD = "Hauttransponder mit integriertem Genotyp-Chip",
        correctAnswer = 1,
        explanation = "Faecal DNA (Kohn & Wayne 1997) erlaubt Individuenidentifikation, Populationsgroessenschaetzung (Capture-Recapture via genetische Markierungen), Verwandtschaftsanalysen und Genfluss-Studien ohne Fang. Angewendet bei Woelfen, Braunbaeren, Elefanten, Tigern und vielen anderen.",
        difficulty = 4,
        funFact = "Fuer den Amurtigerin wurden durch Faecal DNA-Analysen erstmals praeziese Schatzungen der Wildpopulation von 480-540 Individuen (2015) erarbeitet."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist das Konzept der 'Rewilding' nach Soule und Noss (1998) und welche drei Kernelemente definiert es?",
        answerA = "Passiver Naturschutz; Kernelemente: Schutzgebiete, Pufferzonen, Monitoring",
        answerB = "Aktive Wiederherstellung oekologischer Prozesse; Kernelemente: Kerne (grosse Schutzgebiete), Korridore, Karnivore (Schluesselpraedatoren) -- 3C-Modell",
        answerC = "Zurueckfuehren von Landflaechen in natuerlichen Zustand; Kernelemente: Extensivierung, Entwaldungsstopp, Artenschutz",
        answerD = "Wiederansiedlung ausgestorbener Megafauna; Kernelemente: Proxy-Arten, Habitatrestaurierung, Kontrolle invasiver Arten",
        correctAnswer = 1,
        explanation = "Soule & Noss (1998) definierten Rewilding durch die '3C': Cores (grosse wildnisartige Schutzgebiete), Corridors (Verbindungsbiotope) und Carnivores (Wiederansiedlung von Schluesselpraedatoren als Trophic Cascade-Treiber). Dieses Modell beeinflusst bis heute die Naturschutzplanung weltweit.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Index misst am praezisesten den funktionellen Zusammenbruch genetischer Diversitaet und gilt als bester Praeduktor fuer langfristige Arterfolgsfaehigkeit?",
        answerA = "Allelic Richness (Ar) -- Anzahl der Allele pro Locus",
        answerB = "Beobachtete Heterozygotie (Ho)",
        answerC = "Nucleotid-Diversitaet (pi)",
        answerD = "Erwartete Heterozygotie (He) / Gendiversitaet",
        correctAnswer = 0,
        explanation = "Allelic Richness (Ar) ist sensibler als Heterozygotie fuer den Verlust seltener Allele, die fuer zukuenftige Anpassungen entscheidend sein koennen. Seltene Allele werden durch Drift und Flaschenhals-Ereignisse bevorzugt eliminiert, lange bevor die Heterozygotie deutlich faellt.",
        difficulty = 4,
        funFact = "Studien an Afrikanischen Wildhunden (Lycaon pictus) zeigten, dass Ar bereits 30 % niedriger war als in gesunden Populationen, waehrend He noch nahezu unveraendert schien."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Prozess erklaert, warum Inselpopulationen bei kleinem Ne besonders schnell fixierte, schaedliche Mutationen ansammeln?",
        answerA = "Haeckelsche Rekapitulation -- ontogenetische Vulnerabilitaet",
        answerB = "Muller's Ratscheneffekt (Muller's Ratchet): irreversibler Anstieg deleterioerer Mutationen in asexuellen oder kleinen Populationen ohne Rekombination",
        answerC = "Transposons-Burst bei Populationsstress -- erhoehte Mutationsrate",
        answerD = "Genetischer Hitchhiking durch Selektion auf verknuepfte Loci",
        correctAnswer = 1,
        explanation = "Mullers Ratscheneffekt beschreibt, dass in endlichen Populationen die Klasse mit der niedrigsten Mutationslast durch Drift verloren gehen kann -- ein irreversibler Schritt. Jeder Schritt der 'Ratsche' erhoaht die durchschnittliche Mutationslast. Relevant fuer kleine, isolierte Populationen wie Inselgeccos oder kretische Populationen.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist 'Assisted Gene Flow' (AGF) im Kontext des Klimaschutzes?",
        answerA = "Genetische Manipulation zur Erhoehung der Hitzetoleranz",
        answerB = "Gezielte Translokation von Individuen oder Gameten mit guenstigen Klimaadaptationsallelen in Populationen, die klimatischen Belastungen ausgesetzt sind",
        answerC = "Schaffung von Klimakorridoren durch Habitatrestaurierung",
        answerD = "Ex-situ-Lagerung von Saatgut und Keimzellen als Klimaversicherung",
        correctAnswer = 1,
        explanation = "AGF (Aitken & Whitlock 2013) nutzt gezielt Individuen aus waermeren oder trockeneren Klimaregionen als Spender adaptiver Allele fuer Empfaengerpopulationen. Ziel: Anpassungsgeschwindigkeit an den Klimawandel erhoehen, wenn natuerlicher Genfluss zu langsam ist.",
        difficulty = 4,
        funFact = "Erste experimentelle AGF-Studien mit Douglastannen in Kanada zeigten, dass Saemling aus 2 Breitengraden suedlicher signifikant besser in simulierten Klimawandel-Szenarien ueberlebten."
    ),

    Question(
        categoryId = 9,
        questionText = "Was misst der Inzuchtkoeffizient F (Wright 1922)?",
        answerA = "Den Verwandtschaftsgrad zwischen zwei Individuen in der Population",
        answerB = "Die Wahrscheinlichkeit, dass beide Allele an einem Locus eines Individuums durch Abstammung von einem gemeinsamen Vorfahren identisch sind (identical by descent, IBD)",
        answerC = "Den Anteil homozygoter Genotypen an der Gesamtgenotypenverteilung",
        answerD = "Die Reduktion der Fitness eines Individuums relativ zur Gesamtpopulation",
        correctAnswer = 1,
        explanation = "Der Inzuchtkoeffizient F misst die Wahrscheinlichkeit, dass beide Allele eines Individuums an einem Locus identisch durch Abstammung (IBD) sind. F = 0 bedeutet keine Inzucht; F = 1 vollstaendige Homozygotie. Bei Vollgeschwisterverpaarung betraegt F der Nachkommen 0,25.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Projekt gilt als das erste erfolgreiche Beispiel eines genetischen Rettungsprogramms (Genetic Rescue) bei einem freilebenden Saeugetier?",
        answerA = "Schwarzfussiltis (Mustela nigripes) -- USA 1987",
        answerB = "Florida-Panther (Puma concolor coryi) -- Einkreuzung texanischer Pumas 1995",
        answerC = "Grauer Wolf (Canis lupus) -- Yellowstone-Wiederansiedlung 1995",
        answerD = "Arabischer Oryx (Oryx leucoryx) -- Wiederansiedlung in Oman 1982",
        correctAnswer = 1,
        explanation = "Die Einkreuzung von 8 texanischen Pumas in die Florida-Panther-Population 1995 gilt als erster dokumentierter Genetic Rescue bei einem freilebenden Grosssaeuger mit messbarer Fitnesssteigerung: Anstieg der Ueberlebensrate von Jungtieren, Rueckgang von Kryptorchismus und Herzklappenverdickungen.",
        difficulty = 4,
        funFact = "Vor der Einkreuzung betrug die Spermienmotilitaet bei maennlichen Florida-Panthern nur ca. 7 % -- bei gesunden Pumas liegt sie ueber 60 %."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist der Unterschied zwischen demographischer und Umweltstochastizitaet in der Populationsoekologie?",
        answerA = "Demographische Stochastizitaet betrifft die Altersstruktur; Umweltstochastizitaet betrifft Habitatqualitaet",
        answerB = "Demographische Stochastizitaet: zufaellige Variation in individuellen Ueberlebens- und Reproduktionsereignissen; Umweltstochastizitaet: zufaellige zeitliche Schwankungen in Umweltparametern (Wetter, Ressourcen), die die gesamte Population betreffen",
        answerC = "Demographische Stochastizitaet dominiert in grossen Populationen; Umweltstochastizitaet in kleinen",
        answerD = "Beide Begriffe sind Synonyme fuer genetischen Drift in unterschiedlichen Kontexten",
        correctAnswer = 1,
        explanation = "Demographische Stochastizitaet entsteht durch zufaellige individuelle Ereignisse und ist relevant bei N < 100. Umweltstochastizitaet (environmental stochasticity) betrifft alle Individuen gleichzeitig durch jaehrliche Schwankungen (Duerre, Krankheiten) und ist auch bei groesseren Populationen gefaehrlich.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Technik wird in der modernen Erhaltungsgenetik verwendet, um Verwandtschaft ohne Stammbuchaufzeichnungen aus genomischen Daten zu rekonstruieren?",
        answerA = "Mikrosatelliten-Fingerprinting mit 8-12 Loci",
        answerB = "SNP-basierte genomische Verwandtschaftsschaetzung (Genomic Relatedness Matrix, GRM) aus Tausenden von SNPs",
        answerC = "Mitochondriale Haplogruppen-Phylogenie",
        answerD = "AFLP-Banding-Muster-Vergleich",
        correctAnswer = 1,
        explanation = "Genomische Verwandtschaftsschaetzung (Yang et al. 2010) verwendet SNP-Arrays oder RADseq-Daten, um aus Tausenden von Markern praeziese IBD-basierte Verwandtschaftskoeffizienten zu schaetzen. Dies ist praeziser als Mikrosatelliten und erlaubt Zuchtentscheidungen ohne Stammbuch.",
        difficulty = 4,
        funFact = "Die Sumatranashorn-Population (Dicerorhinus sumatrensis) -- weniger als 80 Individuen -- wird inzwischen vollstaendig genomisch typisiert, um optimale Paarungspartner zu identifizieren."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Phaenomen beschreibt die dramatische Fitness-Abnahme durch homozygote Expression deleterioerer rezessiver Allele in kleinen Populationen?",
        answerA = "Purging (genetische Reinigung)",
        answerB = "Inzuchtdepression",
        answerC = "Genetischer Drift",
        answerD = "Heterosis",
        correctAnswer = 1,
        explanation = "Inzuchtdepression entsteht durch die erhoeahte Wahrscheinlichkeit, dass nahe Verwandte beide dasselbe schadliche rezessive Allel tragen und es in homozygoten Nachkommen zur Expression kommt. Quantifiziert als Lethalaeauivalente (B): Anzahl rezessiver Lethal-Allele, die im heterozygoten Zustand die Fitness halbieren.",
        difficulty = 4,
        funFact = "Der Tordalk (Alca torda) zeigte in kleinen Inselpopulationen eine messbbare Zunahme lethal-aequivalenter Allele innerhalb von 15 Generationen Isolation."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist 'Purging' in der Erhaltungsgenetik und wann ist er adaptiv?",
        answerA = "Das gezielte Entfernen betroffener Individuen aus einer Population zur Fitness-Verbesserung",
        answerB = "Die Elimination stark deleterioeser Allele durch Selektion, die bei Homozygotie durch Inzucht exponiert werden -- adaptiv bei stark rezessiven Lethalen, nicht bei schwach deleteriosen Allelen",
        answerC = "Die Rueckkreuzung von Inzuchtlinien mit Wildfangindividuen",
        answerD = "Die genetische Sanierung einer Population durch gezielte Selektion auf Heterozygotie",
        correctAnswer = 1,
        explanation = "Purging entfernt stark rezessive Lethale effizient durch Selektion, wenn sie durch Inzucht homozygot werden. Schwach deleterioese Allele werden durch Drift fixiert statt bereinigt -- daher ist moderates Purging nuetzlich bei wenigen Lethal-Loci, aber schaedlich bei polygener Inzuchtdepression.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Konzept der Inselbiographietheorie (MacArthur & Wilson 1967) ist direkt auf Schutzgebietsdesign anwendbar?",
        answerA = "Die Theorie, dass Artenzahl nur von Inselflaeche abhaengt",
        answerB = "Das artengleichgewichts-Modell: Artenzahl als Gleichgewicht zwischen Immigration und Extinction, abhaengig von Flaeche und Isolation",
        answerC = "Das Primaerprodukivitaets-Artenzahl-Modell",
        answerD = "Die Konkurrenzausschluss-Theorie fuer Inselendeme",
        correctAnswer = 1,
        explanation = "MacArthur & Wilsons Gleichgewichtstheorie sagt voraus, dass groessere und weniger isolierte Habitatinseln mehr Arten halten. Direkte Implikation fuer Schutzgebietsdesign: SLOSS-Debatte (Single Large or Several Small), Pufferzone, Korridor-Konnektivitaet -- bis heute ein Kernkonflikt in der angewandten Naturschutzbiologie.",
        difficulty = 4,
        funFact = "Das SLOSS-Problem ist bis heute ungeloest. Empirische Studien zeigen kontextabhaengige Ergebnisse -- fuer manche Taxa sind mehrere kleine Reservate besser, fuer andere ein grosses."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Index quantifiziert die genetische Differenzierung und wird in Metapopulationsstudien zur Berechnung des Genflusses (Nm) verwendet?",
        answerA = "Nei's genetische Distanz (D)",
        answerB = "Wrights FST, aus dem Nm geschaetzt werden kann als Nm = (1 - FST) / (4 x FST)",
        answerC = "Tajima's D fuer selektive Neutralitaetstests",
        answerD = "Garza-Williamson M-Ratio fuer Flaschenhals-Detektion",
        correctAnswer = 1,
        explanation = "Aus FST laesst sich unter dem Insel-Migrationsmodell der Genfluss als Nm = (1 - FST) / (4 x FST) berechnen, wobei Nm die Anzahl der effektiv migrierenden Individuen pro Generation ist. Nm > 1 gilt als Faustregel fuer ausreichenden Genfluss zur Verhinderung genetischer Differenzierung.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist die Garza-Williamson M-Ratio und wofuer wird sie in der Erhaltungsgenetik eingesetzt?",
        answerA = "Ein Mass fuer die Inzucht basierend auf Mikrosatelliten-Heterozygotie",
        answerB = "Das Verhaeltnis der Anzahl beobachteter Allele zur Allel-Groessenspanne bei Mikrosatelliten -- niedrige Werte zeigen vergangene Flaschenhals-Ereignisse an",
        answerC = "Ein Chi-Quadrat-Test fuer Hardy-Weinberg-Abweichungen",
        answerD = "Ein FST-basierter Differenzierungsindex fuer Mikrosatellitendaten",
        correctAnswer = 1,
        explanation = "Die M-Ratio (Garza & Williamson 2001) berechnet sich als M = k / (r+1), wobei k die Allele und r die Groesstspanne sind. Nach einem Flaschenhals fallen seltene Allele aus, die Spanne bleibt aber erhalten -- die M-Ratio faellt. M < 0,68 gilt als Hinweis auf einen historischen Flaschenhals.",
        difficulty = 4,
        funFact = "Anhand der M-Ratio wurde nachgewiesen, dass der Suedliche Seeelefant (Mirounga leonina) auf Macquarie Island einen schweren Flaschenhals im 19. Jahrhundert erlitten hatte."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist das 'Frozen Zoo' Konzept des San Diego Zoo Wildlife Alliance?",
        answerA = "Ein virtuelles Artenschutzprogramm basierend auf genomischen Datenbanken",
        answerB = "Eine Kryobank lebender Zellkulturen (Fibroblasten, Gameten, Embryonen) von ueber 10.000 bedrohten Tierindividuen",
        answerC = "Ein ex-situ-Haltungsprogramm in klimatisierten Anlagen fuer kaltadaptierte Arten",
        answerD = "Ein Protokoll zur Tiefkuehlkonservierung von Saatgut bedrohter Pflanzen",
        correctAnswer = 1,
        explanation = "Das Frozen Zoo des San Diego Zoo (gegr. 1972 von Kurt Benirschke) ist die weltweit groesste genomische Ressource fuer bedrohte Tierarten. Lebende Fibroblasten koennen zu iPSCs reprogrammiert werden, Gameten fuer IVF genutzt, DNA fuer genomische Analysen extrahiert werden.",
        difficulty = 4,
        funFact = "Aus Frozen-Zoo-Fibroblasten des Nördlichen Weisshornoeriger wurden 2023 erstmals Spermien-Vorlaeufer-Zellen in vitro erzeugt -- ein Schritt Richtung In-vitro-Fertilisation."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Konzept beschreibt in der Naturschutzgenetik eine Population, die genetisch einzigartig genug ist, um als eigenstaendige Managementeinheit behandelt zu werden?",
        answerA = "Phylogeographische Klade (clade)",
        answerB = "Management Unit (MU) nach Moritz: Population mit signifikant unterschiedlichen Allelhaeufigkeiten an nukleaeren Loci",
        answerC = "Deme: jede raeumlich abgegrenzte lokale Population",
        answerD = "Ecotype: oekologisch differenzierte Population ohne genetische Grundlage",
        correctAnswer = 1,
        explanation = "Management Units (MUs, Moritz 1994) werden durch signifikante Allelfrequenzunterschiede an nukleaeren Markern definiert (im Gegensatz zu ESUs, die reziproke Monophylie mitochondrialer DNA erfordern). MUs sind weniger streng als ESUs und dienen der Feinplanung von Translokationen und Zuchtprogrammen.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Oekosystem-Restaurierungsprojekt in Europa wird als groesstes Rewilding-Projekt des Kontinents bezeichnet?",
        answerA = "Oostvaardersplassen (Niederlande) -- Wiederherstellung von Feuchtgebietsoekosystemen seit 1968",
        answerB = "Rewilding Europe mit dem Donaudelta und der Iberischen Halbinsel als Kernregionen",
        answerC = "Great Bear Rainforest (Kanada) -- Schutz des letzten alten Waldwachstums",
        answerD = "Pleistozaen-Park (Sibirien) -- Wiederherstellung mammutsteppen-aehnlicher Tundra",
        correctAnswer = 1,
        explanation = "Rewilding Europe (gegr. 2011) hat das Ziel, bis 2030 eine Million Hektar europaeischer Wildnis zu schaffen. Kernregionen sind das Donaudelta, die iberische Meseta, die Schottischen Highlands und die Karpaten. Seit 2012 wurden ueber 100 Wisenttranslocationen in 12 Laendern durchgefuehrt.",
        difficulty = 4,
        funFact = "Der Pleistozaen-Park in Sibirien (Sergei Zimov, seit 1996) versucht durch Einfuehren von Grossherbivoren (Bison, Musk Ox, Yaks) die Mammut-Steppe zu rekonstruieren und permafrost-stabilisierende Graslandschaften wiederherzustellen."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher genomische Ansatz identifiziert adaptiv wichtige Genomregionen durch Vergleich von Populationen, die verschiedenen Umweltbedingungen ausgesetzt sind?",
        answerA = "GWAS (Genome-Wide Association Study) fuer Krankheitsresistenz",
        answerB = "Umwelt-Assoziationsanalyse (Environmental Association Analysis, EAA) -- Korrelation von SNP-Frequenzen mit Umweltgradienten",
        answerC = "Phylogenomische Coalescent-Analyse",
        answerD = "Strukturelle Variantenanalyse durch Comparative Genomic Hybridization",
        correctAnswer = 1,
        explanation = "EAA (auch Genotype-Environment Association, GEA) identifiziert SNPs, deren Allelfrequenzen signifikant mit Umweltvariablen (Temperatur, Niederschlag, Hoehe) korrelieren -- Kandidaten fuer lokale Adaptation. Methoden: BayEnv, RDA, latent factor mixed models (LFMM).",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Art gilt als das bekannteste Beispiel einer durch Captive-Breeding-Programme geretteten Art, die 1987 mit den letzten Wildindividuen in ein Zuchtprogramm ueberfuehrt wurde?",
        answerA = "Arabischer Oryx (Oryx leucoryx)",
        answerB = "Schwarzfussiltis (Mustela nigripes)",
        answerC = "Kalifornischer Kondor (Gymnogyps californianus)",
        answerD = "Zwergrabe (Corvus kubaryi)",
        correctAnswer = 2,
        explanation = "1987 wurden alle 27 verbliebenen wilden Kalifornischen Kondore eingefangen. Das Captive Breeding Program in San Diego und Los Angeles Zoo fuehrte zur Erholung der Art: 2024 leben ueber 540 Individuen, davon mehr als 350 in der Wildbahn -- ein Paradeprogramm fuer ex-situ-Erhaltung.",
        difficulty = 4,
        funFact = "Alle heutigen Kondore tragen Fluegel-Nummernmarken und werden individuell ueberwacht. Bleivergiftung durch bleihaltige Jagdmunition bleibt die groesste Todesursache in Freiheit."
    ),

)
