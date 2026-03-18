package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun animalQuestionsMaster5(): List<Question> = listOf(

    // MASTER (difficulty = 5) -- Advanced Ecology and Biogeography

    Question(
        categoryId = 9,
        questionText = "Welche Meeresstrasse bildet die Wallace-Linie und trennt die australasische von der orientalischen Fauna?",
        answerA = "Strasse von Malakka",
        answerB = "Lombokstrasse",
        answerC = "Bandasee-Passage",
        answerD = "Makassarstrasse",
        correctAnswer = 3,
        explanation = "Die Wallace-Linie verlaeuft durch die Makassarstrasse zwischen Borneo und Sulawesi sowie durch die Lombokstrasse zwischen Bali und Lombok. Alfred Russel Wallace beschrieb 1863 den abrupten Wechsel der Faunen an dieser Grenze, die einem tiefen Meeresgraben entspricht, der selbst waehrend der Eiszeiten nie verlandet war.",
        difficulty = 5,
        funFact = "Die Tiefe der Makassarstrasse betraegt bis zu 2.500 m -- tief genug, um auch bei einem Meeresspiegel-Tiefstand von 120 m eine Meeresbarriere zu bilden."
    ),

    Question(
        categoryId = 9,
        questionText = "Was beschreibt der Begriff 'Wallacea' in der Biogeographie?",
        answerA = "Die gesamte orientalische Region oestlich von Indien",
        answerB = "Das Uebergangsgebiet zwischen der orientalischen und der australischen Region zwischen Wallace-Linie und Weber-Linie",
        answerC = "Die exklusive Verbreitungszone australischer Beuteltiere",
        answerD = "Die indomalayische Subregion der Suluinseln",
        correctAnswer = 1,
        explanation = "Wallacea bezeichnet das Uebergangsgebiet zwischen der Wallace-Linie im Westen (zwischen Bali/Lombok und Borneo/Sulawesi) und der Weber-Linie im Osten. Es umfasst Sulawesi, die Kleinen Sundainseln und die Molukken. Die Fauna ist ein Mischgebiet ohne dominante Herkunftsregion.",
        difficulty = 5,
        funFact = "In Wallacea kommen weder typische asiatische Familien wie Baerenmarder noch australische Gruppen wie Kanguruhs vor -- es ist eine echte Filterbarriere fuer beide Faunen."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche spezifische Gleichgewichtsformel der Inselbiogeographie (MacArthur & Wilson 1967) beschreibt die Artenzahl S auf einer Insel?",
        answerA = "S = c * A^z, wobei A die Flaeche und z typischerweise 0,2-0,35 betraegt",
        answerB = "S = I / (I + E), wobei I Immigrationsrate und E Extinktionsrate",
        answerC = "S = sqrt(A / d), wobei d die Entfernung zum Festland",
        answerD = "S = k * log(A) mit k als Diversitaetskonstante",
        correctAnswer = 0,
        explanation = "Die Arten-Areal-Beziehung lautet S = c * A^z. Der Exponent z betraegt empirisch fuer echte Inseln meist 0,25-0,35, fuer Habitatinseln 0,12-0,17. MacArthur und Wilson leiteten diese Beziehung aus der dynamischen Gleichgewichtstheorie ab, bei der Immigrations- und Extinktionsraten sich schneiden.",
        difficulty = 5,
        funFact = "Eine Verzehnfachung der Inselflaeche verdoppelt in etwa die Artenzahl -- eine der robustesten Regeln in der Oekologie."
    ),

    Question(
        categoryId = 9,
        questionText = "Robert Paine fuehrte 1966 auf Mukkaw Bay (Washington State) sein beruehmt gewordenes Seestern-Entfernungsexperiment durch. Welche Seesternart entfernte er und welchen direkten Effekt hatte dies?",
        answerA = "Pisaster brevispinus -- Zunahme von Seegurken",
        answerB = "Pisaster ochraceus -- Moschusmuscheln (Mytilus) dominierten und verdraengten 15 andere Arten",
        answerC = "Dermasterias imbricata -- Seepocken uebernahmen die gesamte Riffzone",
        answerD = "Evasterias troschelii -- Algenmatten ersetzten das Benthos",
        correctAnswer = 1,
        explanation = "Paine entfernte Pisaster ochraceus und beobachtete, dass Mytilus californianus das Substrat vollstaendig kolonisierte und die Biodiversitaet von 15 auf weniger als 10 Arten sank. Dieses Experiment begrundete das Konzept der Schlusselsteinart (keystone species) -- eine Art mit disproportional grossem Einfluss auf das Oekosystem.",
        difficulty = 5,
        funFact = "Paine praegte den Begriff 'keystone species' in Analogie zum Schlusselstein eines Gewolbes -- wird er entfernt, bricht das ganze System zusammen."
    ),

    Question(
        categoryId = 9,
        questionText = "Was sind die vier Isolationsmechanismen in Hubbells Neutraler Theorie der Biodiversitaet (2001) fuer das Fortbestehen von Seltenheit?",
        answerA = "Konkurrenz, Praedation, Mutualismus und Amensalismus",
        answerB = "Geburt, Tod, Immigration und Speziation -- alle mit gleicher Pro-Kopf-Wahrscheinlichkeit",
        answerC = "Nichenfiltration, Ausbreitungslimitierung, Drift und Selektion",
        answerD = "Oekotypisierung, Habitatselektion, Assortative Paarung und Isolation",
        correctAnswer = 1,
        explanation = "Hubbells neutrale Theorie geht davon aus, dass alle Individuen unabhaengig ihrer Art eine gleiche Wahrscheinlichkeit fuer Geburt, Tod, Immigration und Speziation besitzen. Artenunterschiede in Abundanz entstehen damit rein durch stochastischen ökologischen Drift, nicht durch Nischendifferenzierung.",
        difficulty = 5,
        funFact = "Die neutrale Theorie wird heftig diskutiert -- in tropischen Regenwaeldern passt das Modell erstaunlich gut, in Savannensystemen viel schlechter."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher der vier Metacommunity-Modelle nach Leibold et al. (2004) betont, dass lokale Gemeinschaften durch Ausbreitungseinschraenkung und stochastischen Drift gepraegt werden, nicht durch Nischendifferenzierung?",
        answerA = "Species-Sorting-Modell",
        answerB = "Mass-Effects-Modell",
        answerC = "Patch-Dynamics-Modell",
        answerD = "Neutral-Dispersal-Modell",
        correctAnswer = 3,
        explanation = "Das Neutrale (Dispersal-basierte) Modell nach Leibold et al. ist direkt von Hubbells neutraler Theorie abgeleitet. Im Gegensatz zum Species-Sorting-Modell (Nischendifferenzierung) oder Mass-Effects-Modell (Rettungseffekte durch Immigration) bestimmen hier Ausbreitung und Drift die Zusammensetzung der lokalen Gemeinschaft.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "In Lotka-Volterra-Praedationsgleichungen: Was ist die Bedeutung des Parameters alpha in dx/dt = ax - alpha*x*y?",
        answerA = "Die intrinsische Wachstumsrate der Beutepopulation",
        answerB = "Die Effizienz, mit der Raubtiere Beute in Nachkommen umwandeln",
        answerC = "Die Rate, mit der Begegnungen zwischen Raubtier und Beute zur Beute-Entnahme fuehren",
        answerD = "Die Karryingkapazitaet der Beutepopulation",
        correctAnswer = 2,
        explanation = "In der Beutegleichung dx/dt = ax - alpha*x*y ist alpha der Praedationskoeffizient: die Rate, mit der ein Raubtier-Beute-Kontakt zur Toetemg eines Beutetieres fuehrt. Das Produkt alpha*x*y ist die Gesamtpraedationsrate, proportional zur Begegnungshaeufigkeit beider Populationen.",
        difficulty = 5,
        funFact = "Lotka und Volterra entwickelten ihre Gleichungen unabhaengig voneinander -- Lotka 1925 fuer chemische Reaktionen, Volterra 1926 fuer Adriafischbestände."
    ),

    Question(
        categoryId = 9,
        questionText = "Was charakterisiert r-selektierte Arten im Vergleich zu K-selektierten Arten gemaess der r/K-Selektionstheorie nach MacArthur & Wilson (1967)?",
        answerA = "Spaete Geschlechtsreife, lange Lebensdauer, wenige Nachkommen mit hoher elterlicher Investition",
        answerB = "Fruehere Geschlechtsreife, kurze Lebensdauer, viele kleine Nachkommen mit geringer elterlicher Investition",
        answerC = "Grosse Koerpergroesse, territorialer Lebensstil, interspecifische Konkurrenzstaerke",
        answerD = "Spezialisierte Nieschennuetzung, geringe Dispersalfahigkeit, hohe Resilienz",
        correctAnswer = 1,
        explanation = "r-selektierte Arten maximieren die intrinsische Wachstumsrate r: fruehe Reife, kurze Generationszeit, viele kleine Nachkommen, geringe Elternpflege. Klassische Beispiele sind Insekten, Mause und Unkraeuter. K-selektierte Arten hingegen optimieren die Effizienz nahe der Tragkapazitaet K: wenige, aufwaendig betreute Nachkommen wie Elefanten oder Adler.",
        difficulty = 5,
        funFact = "Die r/K-Theorie wurde spaeter durch die Life-History-Theorie verfeinert -- empirisch zeigt sich, dass die Dichotomie zu vereinfachend ist und ein Kontinuum besser passt."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Experiment belegte den trophischen Kaskaden-Effekt von Wölfen im Yellowstone-Nationalpark nach ihrer Wiedereinbuergerung 1995?",
        answerA = "Die Reduktion von Bisonherden fuehrte zu ausgedehnten Graslandschaften",
        answerB = "Die Verhaltensaenderung von Elk (Wapiti) an Flussufern liess Weidenpflanzungen regenerieren und Biber zurueckkehren",
        answerC = "Der Rueckgang von Kojoten erhöhte Hasenbestände und indirekt Adlerpopulationen",
        answerD = "Woelfe dezimierten Pumas, was Hirschbestände erhoehte und Vegetation schaedigte",
        correctAnswer = 1,
        explanation = "Nach der Wolfswiedereinbuergerung 1995 mieden Wapitis aus Angst vor Woelfen Flussauen. Diese 'Angstlandschaft' (landscape of fear) liess Weidenbaeume und Pappeln regenerieren, was Biberbestände foerderte. Biber bauten Daemme, die Flussgrabenstrukturen veraenderten -- ein klassischer trophischer Kaskaden-Effekt ueber Verhaltensaenderung.",
        difficulty = 5,
        funFact = "Dieses Phaenomen -- Beeinflussung oekosystemarer Prozesse durch das blosse Vorhandensein eines Praedators ohne direkte Toetung -- wird 'ecology of fear' genannt."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist die 'Intermediate Disturbance Hypothesis' (IDH) nach Connell (1978) und fuer welchen Zusammenhang steht sie?",
        answerA = "Maximale Biodiversitaet entsteht bei minimalem Stoerungsregime in stabilen Klimazonen",
        answerB = "Biodiversitaet ist am hoechsten bei mittlerer Stoerungsintensitaet und -haeufigkeit, weil Kompetitive Exklusion und Massenextinktion beide vermieden werden",
        answerC = "Stoerungen erhoehen immer die Artenzahl, da sie Nischen schaffen",
        answerD = "Biodiversitaet ist in ungestoerten Systemen am hoechsten, da K-Strategen dominieren",
        correctAnswer = 1,
        explanation = "Connells IDH besagt, dass bei niedrigem Stoerungsgrad kompetitive Exklusion die Diversitaet reduziert, bei hohem Stoerungsgrad nur wenige Stoerungstolerante ueberleben. Bei mittlerer Stoerung werden dominante Konkurrenten zurueckgehalten, ohne alle Arten auszulöschen -- die Diversitaet wird maximiert. Ein Paradebeispiel sind Korallenriffe mit mittlerem Wellenschlag.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welche der sechs Florenreiche nach Takhtajan (1978) gilt als aeltestes und hat die hoechste Anzahl endemischer Pflanzenfamilien?",
        answerA = "Holantarktisches Reich",
        answerB = "Neotropisches Reich",
        answerC = "Kaplandisches Reich (Capensis)",
        answerD = "Boreal-Holoarktisches Reich",
        correctAnswer = 2,
        explanation = "Das Kaplandische Florenreich (Cape Floristic Region) in Suedafrika gilt als artenreichstes Florenreich pro Flaeche der Welt. Auf nur 90.000 km2 kommen ca. 9.000 Pflanzenarten vor, davon ca. 70 % endemisch. Mit 6 endemischen Familien und hoechster Familienendemierate gilt es als floristisch aeltestes und eigenstaendigstes Reich.",
        difficulty = 5,
        funFact = "Das Kapland beherbergt mehr Pflanzenarten als das gesamte Vereinigte Koenigreich, obwohl es weniger als 0,5 % der Weltlandflaeche ausmacht."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist die 'rescue effect' Hypothese in der Inselbiogeographie und welcher Parameter beeinflusst sie?",
        answerA = "Immigrationsraten retten kleine Populationen vor dem Aussterben und sind proportional zur Entfernung vom Festland",
        answerB = "Extinktionsraten werden durch kontinuierliche Immigration von Artgenossen vom Festland reduziert, besonders bei nahen Inseln",
        answerC = "Raubtiere retten Beutepopulationen vor Ueberpopulation und regulieren das Gleichgewicht",
        answerD = "Stoerungsregimes setzen die Artengemeinschaft zurueck und ermöoglichen Neubesiedlung",
        correctAnswer = 1,
        explanation = "Der Rescue Effect (Brown & Kodric-Brown 1977) besagt, dass kontinuierliche Immigration von Artgenossen vom Festland kleine Inselpopulationen vor dem lokalen Aussterben rettet, weil genetische Verarmung und demografische Stochastik kompensiert werden. Nahe Inseln profitieren staerker, da Immigrationsraten mit der Entfernung abnehmen.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Region der Welt gilt als wichtigster Beweis fuer Gondwana-Biogeographie und beherbergt endemische Nothofagus-Buechen auf beiden Seiten des Suedatlantiks?",
        answerA = "Suedamerika und Suedafrika",
        answerB = "Suedamerika, Tasmanien, Neuseeland und Antarktische Halbinsel",
        answerC = "Australien und Indien",
        answerD = "Neuguinea und Madagaskar",
        correctAnswer = 1,
        explanation = "Nothofagus (Suedbuchen) sind auf Suedamerika (Chile, Argentinien), Neuseeland, Tasmanien, Neuguinea und fossilen Funden der Antarktis beschraenkt. Diese Verbreitung spiegelt die Gondwana-Fragmentierung ab dem Jura wider. Die Art-Verwandtschaften zwischen diesen Regionen belegen gemeinsame Abstammung vor dem Zerfall des Superkontinents vor ~180 Mio. Jahren.",
        difficulty = 5,
        funFact = "Nothofagus-Pollen sind in 65-Mio.-Jahre-alten antarktischen Sedimenten gefunden worden -- als die Antarktis noch bewaldetes Gondwana-Gebiet war."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist der Kern der 'species-energy hypothesis' von Wright (1983) zur Erklaerung des Breitengradienten der Biodiversitaet?",
        answerA = "Hoehere UV-Strahlung in den Tropen foerdert genetische Variation und Speziation",
        answerB = "Groessere Energieverfuegbarkeit (Evapotranspiration) in den Tropen ermoöglicht mehr Individuen und damit mehr Arten",
        answerC = "Geringere saisonale Schwankungen in den Tropen erhoehen Ueberleben aller Arten gleichermassen",
        answerD = "Tropische Arten konkurrieren weniger, weil Ressourcen nie limitierend sind",
        correctAnswer = 1,
        explanation = "Wrights Species-Energy-Hypothese postuliert: hohere Primärproduktion und Energieeinspeisung in den Tropen ermoeglichen hohere Individuenzahlen, und mehr Individuen bedeuten mehr Populationen, geringere Extinktionsraten und letztlich mehr Arten. Als Proxygroesse wird oft die jaehrliche potenzielle Evapotranspiration (PET) verwendet.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welchen spezifischen Wert hat der Steigungsexponent z der Arten-Areal-Beziehung typischerweise fuer archipelartige Inselgruppen im Vergleich zu Habitatinseln (z.B. Waldfragmenten)?",
        answerA = "Inselgruppen: z = 0,10-0,15; Habitatinseln: z = 0,25-0,35",
        answerB = "Inselgruppen: z = 0,25-0,35; Habitatinseln: z = 0,12-0,18",
        answerC = "Beide liegen bei z = 0,20-0,25",
        answerD = "Inselgruppen: z = 0,40-0,50; Habitatinseln: z = 0,05-0,10",
        correctAnswer = 1,
        explanation = "Echte Inselgruppen zeigen z-Werte von 0,25-0,35, weil Inseln tatsaechlich isoliert sind und Extinktionsereignisse haeufiger und irreversibler sind. Habitatinseln (Waldfragmente, Bergkuppen) zeigen flachere Kurven (z = 0,12-0,18), da Arten in der Matrix ueberleben koennen und die Isolation unvollstaendig ist.",
        difficulty = 5,
        funFact = "Preston (1960) erklaerte den z-Wert von ~0,26 aus der log-normalen Haeufigkeitsverteilung von Arten -- eines der wenigen theoretisch abgeleiteten Parameter der Oekologie."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Tiergruppe diente MacArthur & Wilson (1967) als Hauptstudienobjekt fuer ihre biogeographische Gleichgewichtstheorie und welche Experimente fuehrten sie dazu durch?",
        answerA = "Voegel der Pazifikinseln -- phylogenetische Analyse alter Museumssammlungen",
        answerB = "Insekten der Florida Keys -- Defaunation und Rekolonisierung kuenstlich berauchter Mangroveninseln",
        answerC = "Fische der Grossen Barriere -- Unterwasserzaehlung vor und nach Bleichereignissen",
        answerD = "Saeugetiere der Berginseln -- Analyse von Fossilfunden in Kalksteinhoehlen",
        correctAnswer = 1,
        explanation = "E.O. Wilson und Daniel Simberloff fuehrten 1969 das Mangroven-Defaunationsexperiment durch: Sie begasten kleine Mangroveninseln der Florida Keys mit Methylbromid und beobachteten die Rekolonisierung durch Arthropoden. Innerhalb von zwei Jahren hatten die meisten Inseln ihre urspruengliche Artenzahl wieder erreicht -- eine direkte Bestätigung der Gleichgewichtstheorie.",
        difficulty = 5,
        funFact = "Simberloff schlug in seiner Doktorarbeit 1969 vor, dass die Gleichgewichtsartenzahl unabhaengig von der Artenzusammensetzung ist -- ein radikales Konzept fuer die damalige Oekologie."
    ),

    Question(
        categoryId = 9,
        questionText = "Was versteht man unter 'ecological stoichiometry' und welchem grundlegenden Prinzip folgt die Redfield-Ratio?",
        answerA = "Das Studium chemischer Reaktionsraten in Oekosystemen; N:P = 7:1 fuer terrestrische Pflanzen",
        answerB = "Die Untersuchung der Elementverhaeltnisse in Organismen; die Redfield-Ratio beschreibt C:N:P = 106:16:1 fuer marines Phytoplankton",
        answerC = "Modellierung von Naehrstoffketten; C:N = 25:1 als universelle Dekompositionsratio",
        answerD = "Energiefluss in trophischen Netzwerken; Redfield-Ratio = 10:1 Trophieeffizienz",
        correctAnswer = 1,
        explanation = "Alfred Redfield (1958) zeigte, dass marines Phytoplankton ein fast universelles Atomverhaeltnis von C:N:P = 106:16:1 aufweist, das dem Naehrstoffverhaeltnis des Tiefenwassers entspricht. Oekologische Stoechiometrie (Sterner & Elser 2002) erweitert dieses Konzept: Abweichungen von Nahrungsverhaeltnissen limitieren Wachstum und Reproduktion von Konsumenten.",
        difficulty = 5,
        funFact = "Die Redfield-Ratio wird genutzt, um marinen Naehrstoffmangel vorherzusagen -- Abweichungen zeigen an, ob N oder P limitierend wirkt."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist das 'ghost of competition past' Konzept nach Connell (1980) in der Oekologie?",
        answerA = "Die Idee, dass heutige Nischentrennung aus vergangener kompetitiver Exklusion resultiert, ohne dass aktuelle Konkurrenz sichtbar ist",
        answerB = "Fossile Konkurrenzmuster die rezente Gemeinschaftsstruktur nicht beeinflussen",
        answerC = "Die Akkumulierung toter Biomasse, die kuenftige Naehrstoffzyklen praegt",
        answerD = "Ausgestorbene Keystone-Arten, deren Fehlen aktuelle Oekosysteme destabilisiert",
        correctAnswer = 0,
        explanation = "Connells 'ghost of competition past' beschreibt, dass heutige Nischentrennung zwischen koexistierenden Arten ein Erbe vergangener Konkurrenz ist. Arten sind bereits so weit divergiert, dass keine messbare aktuelle Konkurrenz mehr stattfindet -- die Nischen-Segregation ist evolviert, der Selektionsdruck historisch. Dies erklaert, warum Feldexperimente oft keine Konkurrenz messen, obwohl Nischenaufteilung evident ist.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welche oekologische Regel besagt, dass Inselpopulationen von Gross-Saeugetieren oft verkleinern (Inseldwarf) und von Kleinsaeugetieren vergroessern (Island Gigantism)?",
        answerA = "Bergmanns Regel",
        answerB = "Fosters Inselregel (Foster's Rule)",
        answerC = "Allens Regel",
        answerD = "Jordans Regel",
        correctAnswer = 1,
        explanation = "Fosters Inselregel (1964) beschreibt, dass grosse Saeugetiere auf Inseln tendenziell kleiner werden (wegen Ressourcenlimitierung und fehlendem Praedationsdruck) waehrend Kleinsaeugetiere groesser werden (sog. Island Gigantism). Klassische Beispiele: Zwergelefanten auf Sizilien und Zypern (Pleistozaen), Riesenwaran auf Flores, Riesenhamster auf Kretakreta.",
        difficulty = 5,
        funFact = "Der ausgestorbene Zwergelefant Palaeoloxodon falconeri auf Malta wog nur noch 100 kg -- verglichen mit 6.000 kg des kontinentalen Vorfahren Palaeoloxodon antiquus."
    ),

    Question(
        categoryId = 9,
        questionText = "In der trophischen Netzwerkanalyse: Was misst die 'trophic efficiency' und welcher Wert gilt als Standardannahme?",
        answerA = "Den Anteil der aufgenommenen Energie, der als Waerme abgegeben wird; Standard: 90 %",
        answerB = "Den Anteil der konsumierten Energie einer trophischen Ebene, der in Biomasse der naechsthoheren Ebene umgewandelt wird; Standard: 10 %",
        answerC = "Die Geschwindigkeit der Naehrstoffruckkehr in den Boden; Standard: 50 %",
        answerD = "Den Anteil der Primarproduktion, der photosynthetisch nutzbar ist; Standard: 1-2 %",
        correctAnswer = 1,
        explanation = "Die trophische Effizienz (Lindeman 1942: 'ten percent law') besagt, dass beim Uebergang von einer trophischen Ebene zur naechsten etwa 10 % der Energie weitergegeben werden. Die restlichen 90 % werden durch Respiration, Exkretion und nicht-konsumierte Biomasse verloren. Dieser Wert variiert stark (5-20 %), gilt aber als gruendlegende Naherung.",
        difficulty = 5,
        funFact = "Raymond Lindeman fuehrte das Konzept des 'trophischen Niveaus' 1942 ein -- seine Arbeit wurde zunachst abgelehnt und posthum veroeffentlicht."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche drei Prozesse bilden den Kern von Leibolds Species-Sorting-Modell der Metakogemeinschaft?",
        answerA = "Speziation, Aussterben und Wiederbesiedlung",
        answerB = "Nischendifferenzierung, Ausbreitung und lokale Adaptation -- Arten sortieren sich gemaess ihrer Habitateignung",
        answerC = "Drift, demographische Stochastik und genetischer Flaschenhals",
        answerD = "Masseneffekte, Rettungseffekte und Ausbreitungslimitierung",
        correctAnswer = 1,
        explanation = "Das Species-Sorting-Modell betont, dass Arten entsprechend ihrer Habitatpraeferenzen und Umweltgradienten in Patches sortiert werden (Nischendifferenzierung). Ausbreitung muss ausreichend sein, damit Arten passende Patches erreichen, aber nicht so gross, dass Masseneffekte die lokale Adaptation ueberspielen. Lokale Gemeinschaften spiegeln damit regionale Nischenvielfalt wider.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist die 'biotic homogenization' und welcher Prozess treibt sie an?",
        answerA = "Genetische Angleichung von Populationen durch Genfluss ueber Landschaftskorridore",
        answerB = "Die weltweite Zunahme von Gemeinsamkeiten in Artengemeinschaften durch Einfuehrung von Generalisten und Aussterben von Spezialisten",
        answerC = "Die oekosystemare Vereinheitlichung durch Klimaerwaermung",
        answerD = "Das Konvergieren tropischer und temperater Diversitaet durch Migration",
        correctAnswer = 1,
        explanation = "Biotische Homogenisierung (McKinney & Lockwood 1999) beschreibt den globalen Trend, dass Artengemeinschaften aehnlicher werden: Invasive Generalisten (Hausspatzen, Ratten, Tauben) ersetzen endemische Spezialisten. Die beta-Diversitaet (Verschiedenheit zwischen Standorten) nimmt ab, waehrend die lokale alpha-Diversitaet oft gleichbleibt oder kurzzeitig steigt.",
        difficulty = 5,
        funFact = "In nordamerikanischen Staedte-Vogelgemeinschaften sind heute ueber 70 % der Individuen nicht-heimische Arten -- ein eklatantes Beispiel biotischer Homogenisierung."
    ),

    Question(
        categoryId = 9,
        questionText = "Welches Konzept der Oekophysiologie beschreibt die 'Fundamental vs. Realized Niche' nach Hutchinson (1957)?",
        answerA = "Die fundamentale Nische ist der tatsaechlich genutzte Raum, die realisierte Nische der theoretisch moegliche",
        answerB = "Die fundamentale Nische ist der n-dimensionale Hyperraum aller Umweltbedingungen, unter denen eine Art ueberleben kann; die realisierte Nische ist der tatsaechlich genutzte Teilraum nach biotischen Interaktionen",
        answerC = "Fundamentale und realisierte Nische sind identisch ohne Konkurrenten",
        answerD = "Die realisierte Nische umfasst das gesamte biogeographische Verbreitungsgebiet",
        correctAnswer = 1,
        explanation = "Hutchinsons n-dimensionaler Nischenbegriff: Die fundamentale Nische ist der vollstaendige Hyperraum aus abiotischen Variablen (Temperatur, Feuchte, pH, etc.), in dem eine Art isoliert ueberleben koennte. Die realisierte Nische ist der nach Konkurrenz, Praedation und anderen biotischen Interaktionen verbleibende Teilraum. Ein klassisches Beispiel sind Barnakeln (Chthamalus/Semibalanus) an Felskuesten.",
        difficulty = 5,
        funFact = "Hutchinson praegte auch das Paradox des Phytoplanktons: Wie koennen hunderte Planktonarten im scheinbar homogenen Ozean koexistieren, obwohl die Nischentheorie Ausschluss vorhersagt?"
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Biogeograph entwickelte das Konzept der 'Dispersal vs. Vicariance' Biogeographie und was ist der Kern des Widerspruchs?",
        answerA = "Hennig (1966): Dispersal-Biogeographie erklaert Verbreitung durch Wanderung, Vikaranz durch Abtrennung von Populationen durch geologische Ereignisse",
        answerB = "Croizat (1952, Panbiogeographie): Verbreitung folgt 'Tracks', nicht singularen Wanderereignissen -- Vikaranz ist der Hauptmechanismus",
        answerC = "Sclater (1858): Tiergeographische Regionen entstehen durch Ausbreitungsbarieren, nicht Plattentetonik",
        answerD = "Darlington (1957): Dispersal-Biogeographie ist die einzige korrekte Erklaerung fuer Verbreitungsmuster",
        correctAnswer = 1,
        explanation = "Leon Croizats Panbiogeographie (1952) und spaeter die kladistische Biogeographie (Nelson & Platnick 1981) stellten das Dispersal-Paradigma in Frage. Vikaranz-Biogeographie erklaert disjunkte Verbreitungen durch geologische Ereignisse (Plattentektonik, Meeresspiegelaenderungen) statt durch seltene Fernausbreitung. Heute gilt ein gemischter Ansatz, wobei molekulare Phylogeographie beide Prozesse quantifiziert.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Was beschreibt die 'neutral theory of molecular evolution' (Kimura 1968) und wie unterscheidet sie sich von der neutralen Theorie der Biodiversitaet (Hubbell 2001)?",
        answerA = "Kimura beschreibt molekulare Substitutionen als stochastisch; Hubbell beschreibt Artenzusammensetzungen als stochastisch -- beide lehnen natuerliche Selektion in ihrer Ebene ab",
        answerB = "Kimura beschreibt Artbildungsraten; Hubbell beschreibt Genflussraten -- beide nutzen dieselben Drift-Gleichungen",
        answerC = "Kimurans Theorie gilt nur fuer RNA-Viren; Hubbells Theorie nur fuer tropische Baeume",
        answerD = "Beide Theorien sind identisch -- Hubbell uebertrug Kimurans Formeln direkt auf Oekologie",
        correctAnswer = 0,
        explanation = "Kimuras neutrale Theorie (1968) besagt, dass die meisten molekularen Substitutionen durch genetischen Drift und nicht durch positive Selektion fixiert werden. Hubbell (2001) uebertrug diese Logik auf die Oekologie: Artenzusammensetzungen werden durch demographischen Drift, nicht durch Nischendifferenzierung bestimmt. Beide Theorien argumentieren auf unterschiedlichen Hierarchieebenen fuer Stochastizitaet gegenueber Selektion.",
        difficulty = 5,
        funFact = "Hubbell nannte sein Werk bewusst 'A Unified Neutral Theory of Biodiversity and Biogeography' -- als direkte Analogie zu Kimurans Molekulartheorie."
    ),

    Question(
        categoryId = 9,
        questionText = "Was sind die Hauptkomponenten des SLOSS-Debates (Single Large Or Several Small) in der Naturschutzbiologie?",
        answerA = "Eine grosses Schutzgebiet hat immer mehr Arten als mehrere kleine gleicher Gesamtflaeche",
        answerB = "Ein grosses Schutzgebiet schuetzt besser gegen stochastisches Aussterben von Grosssaeugetieren; mehrere kleine foerdern mehr beta-Diversitaet und schuetzen heterogenere Habitate",
        answerC = "Mehrere kleine Gebiete sind immer besser, weil sie Metapopulationsdynamiken erlauben",
        answerD = "SLOSS ist theoretisch irrelevant -- das totale Flaechenvolumen bestimmt allein den Artenschutz",
        correctAnswer = 1,
        explanation = "Der SLOSS-Debate (Diamond 1975 vs. Simberloff & Abele 1976) ist ungelöost. Grosse Gebiete schuetzen Grosssaeugetiere mit grossen Heimatgebieten und reduzieren stochastische Extinktionen. Mehrere kleine Gebiete erhoehen beta-Diversitaet durch verschiedene Habitattypen und ermoglichen Metapopulationsdynamiken. Moderne Naturschutzbiologie empfiehlt ein Mix aus Kerngebieten und Korridoren.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welches biogeographische Ereignis erklart die paradoxe Verbreitung von Hippopotamus-Vorfahren auf Madagaskar?",
        answerA = "Ueberschwimmen der Kanals waehrend Eiszeit-Tiefstaenden",
        answerB = "Raftung (passive Verdrift) auf Vegetationsmassen ueber den Mozambiquekanal",
        answerC = "Einwanderung ueber eine afrikanische Landbruecke vor 50 Mio. Jahren",
        answerD = "Menschliche Einfuehrung in historischer Zeit",
        correctAnswer = 1,
        explanation = "Die ausgestorbenen Madagassischen Zwergnilpferde (Hippopotamus lemerlei, H. madagascariensis) sind engste Verwandte des afrikanischen Flusspferds. Da der Mozambiquekanal nie verlandet war, erklaert man ihre Kolonisation durch Raftung -- passiven Transport auf Treibholz oder Vegetationsmatten. Molekulare Daten zeigen eine Trennung vor ~2 Mio. Jahren.",
        difficulty = 5,
        funFact = "Raftung ueber grosse Ozeandistanzen galt lange als unglaubwuerdig, gilt aber heute als Hauptmechanismus fuer die Besiedlung Madagaskars durch Lemuren, Tenreks und Fossas vor 50-60 Mio. Jahren."
    ),

    Question(
        categoryId = 9,
        questionText = "Was beschreibt das Konzept der 'niche conservatism' in der evolutionaeren Biogeographie?",
        answerA = "Arten besiedeln keine neuen Klimazonen, weil ihre oekologischen Toleranzen evolutiv konserviert sind",
        answerB = "Nischen werden von ueberlegenen Konkurrenten gegen Eindringlinge verteidigt",
        answerC = "Endemische Arten auf Inseln koennen ihre Nischen nicht veraendern",
        answerD = "Klimatische Nischen aendern sich schnell durch lokale Adaptation",
        correctAnswer = 0,
        explanation = "Nischen-Konservatismus (Wiens & Graham 2005) beschreibt das Phaenomen, dass Linien ihrer angestammten ökologischen Nische treu bleiben -- selbst wenn neue Klimazonen verfuegbar waeren. Dies erklaert, warum tropische Familien nicht in temperaten Klimaten evolvieren und umgekehrt, und warum Verbreitungsgrenzen oft an Klimagrenzen fallen statt an physikalischen Barrieren.",
        difficulty = 5,
        funFact = "Nischen-Konservatismus wird als erklaerung fuer den Breitengradienten der Diversitaet herangezogen: die Tropen sind vielfaeltig, weil viele Linien dort entstanden und nie ausgezogen sind."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Parameter der Lotka-Volterra-Konkurrenzgleichungen beschreibt den Effekt der Konkurrenzart j auf die Wachstumsrate von Art i?",
        answerA = "Beta (Wachstumskoeffizient)",
        answerB = "Alpha-ij (interspecifischer Konkurrenzkoeffizient)",
        answerC = "r_i (intrinsische Wachstumsrate von Art i)",
        answerD = "K_i (Tragkapazitaet von Art i)",
        correctAnswer = 1,
        explanation = "In den Lotka-Volterra-Konkurrenzgleichungen ist alpha-ij der interspecifische Konkurrenzkoeffizient, der beschreibt, wie stark ein Individuum der Art j die Wachstumsrate von Art i beeinflusst, relativ zur intraspecifischen Konkurrenz. Das Koexistenzkriterium lautet: alpha-12 * alpha-21 < 1 (interspecifische Konkurrenz schwaecher als intraspecifische Konkurrenz).",
        difficulty = 5,
        funFact = "Gause (1934) demonstrierte kompetitive Exklusion mit Paramecium-Experimenten: zwei ahnliche Arten im gleichen Habitat -- eine wurde stets verdraengt."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist die 'metabolic theory of ecology' (MTE) nach Brown et al. (2004) und welche Allometrie steckt dahinter?",
        answerA = "Alle oekologischen Raten skalieren mit der Koerpermasse zur Potenz 3/4 durch fraktale Versorgungsnetze",
        answerB = "Stoffwechselraten skalieren linear mit der Koerpermasse",
        answerC = "Populationswachstum skaliert mit Koerpermasse zur Potenz 2/3 (Oberflaeche)",
        answerD = "Energiebedarf skaliert invers mit der Artendiversitaet",
        correctAnswer = 0,
        explanation = "Die Metabolische Theorie der Oekologie baut auf Kleibers Gesetz auf: Metabolismusrate M ~ K * B^(3/4), wobei B die Koerpermasse ist. West, Brown und Enquist (1997) erklaerten den 3/4-Exponenten durch fraktale Versorgungsnetze (Blutgefaesse, Tracheen). Die MTE macht dann Vorhersagen ueber Populationsdichte, Generationszeit und Umsatzraten ueber alle Skalen.",
        difficulty = 5,
        funFact = "Das 3/4-Skalierungsgesetz gilt ueber 27 Groessenordnungen -- von Bakterien bis Blauwalen -- und ist eines der erstaunlichsten Muster der Biologie."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist die 'Area Effect' Hypothese in der Speziation und wie wirkt sich die Inselgroesse auf Artbildungsraten aus?",
        answerA = "Grosse Inseln haben hoehere Immigrationsraten, die Speziation verhindern",
        answerB = "Grosse Inseln foerdern Speziation durch mehr Habitat-Heterogenitaet und groessere Populationen, die Allopatrie innerhalb der Insel ermoeglichen",
        answerC = "Kleine Inseln haben hoehere Speziationsraten durch intensiveren Selektionsdruck",
        answerD = "Inselgroesse hat keinen Einfluss auf Speziation, nur auf Immigration",
        correctAnswer = 1,
        explanation = "Rosenzweig (1995) und andere zeigten, dass grosse Inseln (und Kontinente) mehr Artbildung generieren, weil: (1) groessere Populationen resistenter gegen Aussterben sind, (2) mehr Habitat-Heterogenitaet allopatrische Speziation innerhalb der Insel erlaubt, (3) laengere Persistenz weiteren Akkumulation von Divergenz ermoeglicht. Dies ergaenzt die Inselbiogeographie um einen Speziationsterm.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welche ozeanografische Barriere ist das aquatische Analogon der Wallace-Linie und trennt tiefgreifend atlantische von pazifischen Meeresorganismen?",
        answerA = "Panama-Landenge (seit ~3,5 Mio. Jahren als Meeresbarriere)",
        answerB = "Humboldtstrom vor Suedamerika",
        answerC = "Mid-Atlantic-Rift als biogeographische Grenzlinie",
        answerD = "Drakepassage zwischen Suedamerika und Antarktis",
        correctAnswer = 0,
        explanation = "Die Entstehung der Panama-Landenge vor ca. 3,5 Mio. Jahren trennte den einheitlichen Proto-Karibik-Ozean in den Atlantik und den Pazifik. Dieses Ereignis -- das 'Great American Biotic Interchange' -- ist eines der bedeutendsten marine biogeographischen Ereignisse: Hunderte von Schwesterarten (Gemini-Arten) existieren heute auf beiden Seiten des Isthmus.",
        difficulty = 5,
        funFact = "Das Schliessen der Panama-Landenge veraenderte auch die Meeresstroemungen global: Der Golfstrom verstaerkte sich, was das nordeuropaeische Klima erwarmte."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist das 'extinction debt' Konzept und welcher Zeithorizont ist typisch fuer Waldfragmentierungseffekte?",
        answerA = "Die zukuenftige Extinktionswelle die fuer eine bereits zu kleine Population unvermeidlich ist; typisch 50-500 Jahre nach Fragmentierung",
        answerB = "Schulden der Vergangenheit durch Uberjagung; werden innerhalb von 10 Jahren faellig",
        answerC = "Das oekologische Defizit durch Entwaldung; wird sofort in derselben Generation sichtbar",
        answerD = "Die Summe ausgestorbener Arten seit der Industrialisierung; kein Zukunftsbezug",
        correctAnswer = 0,
        explanation = "Extinction Debt (Tilman et al. 1994) beschreibt die zeitverzoegerte Artenverlustreaktion auf Habitatverlust: Populationen ueberlebten kurzfristig, sind aber langfristig nicht lebensfaehig. In europaeischen und tropischen Waldfragmenten wird der Extinktionspeak auf 50-500 Jahre nach der Fragmentierung geschaetzt -- viele Arten 'leben noch', sind aber so gut wie ausgestorben.",
        difficulty = 5,
        funFact = "In den Borneowaldfragmenten nach Abholzung der 1970er-1990er Jahre werden Oekologebn erst in den 2050er-2080er Jahren den Grossteil der Artenverluste sehen."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Einheit misst die beta-Diversitaet nach der Whittaker-Definition (1960) und wie unterscheidet sie sich von alpha- und gamma-Diversitaet?",
        answerA = "Beta = alpha / gamma; misst die lokale Artenzahl relativ zur regionalen",
        answerB = "Beta = gamma / alpha; misst den Umsatz (Turnover) der Artenzusammensetzung zwischen Standorten; alpha = lokal, gamma = regional",
        answerC = "Beta = gamma - alpha; misst den absoluten Artenverlust zwischen zwei Standorten",
        answerD = "Beta = ln(gamma/alpha); misst logarithmische Diversitaetsunterschiede",
        correctAnswer = 1,
        explanation = "Whittakers Diversitaetshierarchie: alpha-Diversitaet ist die lokale Artenzahl eines Standorts, gamma-Diversitaet die regionale Gesamtartenzahl, beta-Diversitaet = gamma / alpha (multiplikative Partition). Beta misst den Artenumsatz zwischen Standorten. Hohe beta-Diversitaet bedeutet viele unterschiedliche Gemeinschaften in der Region.",
        difficulty = 5,
        funFact = "Es existieren ueber 24 verschiedene beta-Diversitaetsindizes in der Literatur -- ein Zeichen wie komplex 'Diversitaetsumsatz' mathematisch zu fassen ist."
    ),

    Question(
        categoryId = 9,
        questionText = "Was beschreibt die 'priority effects' in der Sukzessionstheorie und wie beeinflusst sie das Endzustand einer Gemeinschaft?",
        answerA = "Spaete Kolonisten dominieren, weil sie an bestehende Gemeinschaften angepasst sind",
        answerB = "Erste Kolonisten beeinflussen den Verlauf der Sukzession durch Modifikation der Umwelt oder kompetitive Vorhaltung von Ressourcen",
        answerC = "Klimax-Vegetation wird unabhaengig von der Kolonisierungsreihenfolge erreicht",
        answerD = "Zufaellige Kolonisierung fuehrt stets zur gleichen Klimax-Gemeinschaft",
        correctAnswer = 1,
        explanation = "Priority Effects beschreiben, wie erste Kolonisten den Sukzessionsverlauf deterministisch beeinflussen: entweder durch positive Rueckkopplung (facilitation -- sie verbessern die Bedingungen fuer Spaetkolonisten) oder negative Rueckkopplung (inhibition -- sie blockieren Ressourcen fuer Konkurrenten). Dies kann zu alternativen stabilen Zustaenden fuehren -- die gleiche Flaeche kann je nach Kolonisierungsgeschichte verschiedene Klimax-Zustande erreichen.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welche drei Inseln oder Inselgruppen repraesentierten die urspruenglichen Stationen fuer die experimentelle Inselbiogeographie-Studien von MacArthur, Wilson und Simberloff?",
        answerA = "Galapagos, Hawaii und Karibik",
        answerB = "Kleine Mangroveninseln der Florida Keys",
        answerC = "Pazifische Atolle (Eniwetok, Bikini, Rongelap)",
        answerD = "Britische Inseln (Orkney, Shetland, Hebriden)",
        correctAnswer = 1,
        explanation = "Simberloff und Wilson (1969, 1970) fuehrten ihre beruehm-gewordenen Experimente an kleinen Mangroveninseln in den Florida Keys durch. Sie waehlten Inseln in verschiedenen Entfernungen vom Festland, begasten sie mit Methylbromid und beobachteten die Rekolonisierung. Innerhalb von zwei Jahren erreichten die meisten Inseln wieder die Gleichgewichtsartenzahl gemaess MacArthur-Wilson-Theorie.",
        difficulty = 5,
        funFact = "Die Inseln hatten Durchmesser von nur 11-18 m -- die kleinsten moeglichen biogeographischen Experimente, die dennoch die Theorie bestatigten."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher oekologische Prozess erklaert die Entstehung von Oekotonen und Randzonen zwischen zwei Biomen und welchen Effekt hat dies auf die Biodiversitaet?",
        answerA = "Klimagradienten erzeugen scharfe Grenzen; Biodiversitaet nimmt an Biom-Grenzen stets ab",
        answerB = "Uebergangszonen (Oekotone) entstehen durch abrupte Klimawechsel und zeigen oft erhoehte Biodiversitaet durch den Randeffekt (edge effect)",
        answerC = "Oekotone sind instabil und verschwinden innerhalb von Jahrzehnten",
        answerD = "Biom-Grenzen folgen stets politischen oder topographischen Grenzen",
        correctAnswer = 1,
        explanation = "Oekotone entstehen durch graduelle oder abrupte Uebergaenge in abiotischen Faktoren (Bodentyp, Wasserhaushalt, Temperatur). Der Randeffekt ('edge effect') erhoht oft die lokale Biodiversitaet, da Arten beider Biome und Oekoton-Spezialisten koexistieren. Allerdings foerdert der Randeffekt bei Waldfragmentierung sogenannte Randgeneralisten und schadet Waldinnenarten -- ein zweischneidiges Schwert.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist die oekologische Bedeutung der 'enemy release hypothesis' bei biologischen Invasionen?",
        answerA = "Invasive Arten werden in Neobiota-Gebieten haufiger von Praedatoren angegriffen als in der Heimatregion",
        answerB = "Invasive Arten gedeihen besser, weil sie ihre spezialisierten Pathogene, Parasiten und Herbivoren aus der Heimatregion zuruecklassen",
        answerC = "Einheimische Arten weichen invasiven aus, was als 'Feindflucht' bezeichnet wird",
        answerD = "Biological Control nutzt Fressfeinde, um Invasoren zu kontrollieren",
        correctAnswer = 1,
        explanation = "Die Enemy Release Hypothesis (Keane & Crawley 2002) postuliert, dass invasive Arten im Einzugsgebiet ihrer spezialisierten Feinde (Parasiten, Pathogene, spezialisierte Herbivoren) entkommen. Ohne regulierende Gegenspieler koennen sie groessere Populationen aufbauen als in der Heimat. Empirisch ist dies belegt durch haufig niedrigeren Parasitenbefall invasiver vs. einheimischer Pflanzen.",
        difficulty = 5,
        funFact = "Biologische Schädlingskontrolle nutzt genau das umgekehrte Prinzip: Einheimische Feinde der Invasoren werden re-importiert -- mit gemischten Erfolgen und manchmal Risiken fuer einheimische Arten."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Hypothese erklaert, warum tropische Regenwaeldvoegel kleinere Gelegegroessen haben als ihre temperaten Verwandten?",
        answerA = "Lack-Hypothese: Tropische Voegel legen weniger Eier, weil Ressourcen begrenzt sind",
        answerB = "Ashmoles Hypothese: Tropische Voegel legen weniger Eier, weil hohe Jahresueber-lebenraten zu niedrigem Populationswachstum fuehren -- grosse Gelege waeren evolutionaer nicht vorteilhaft",
        answerC = "Ricklefs-Hypothese: Praedation von Nestern ist in den Tropen haufiger und seleKtiert fuer weniger Investition pro Gelege",
        answerD = "Martin-Hypothese: Nahrungsverfuegbarkeit ist gleichmaessig und erlaubt keine grossen Gelege",
        correctAnswer = 1,
        explanation = "Ashmoles Hypothese (1963) erklaert die kleineren tropischen Gelegegroessen durch Populationsdynamik: In tropischen Voegelpopulationen ist die adulte Ueberlebensrate hoch und der jaehrliche Reproduktionsdruck gering. Grosse Gelege sind fuer die Populationserhaltung nicht noetig und wuerden nur die Ueberlebenschancen der Eltern senken. Dies ist ein Beispiel fuer Life-History-Trade-offs.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist das 'Allee-Effekt' Konzept und welcher kritische Schwellenwert ist dabei relevant?",
        answerA = "Populationswachstum steigt linear mit der Populationsgroesse ohne Schwellenwert",
        answerB = "Unterhalb einer kritischen Populationsgroesse sinkt die Pro-Kopf-Fitness, was zu Extinction Vortex fuehren kann",
        answerC = "Grosse Populationen haben hoehere Inzuchtraten, was das Wachstum begrenzt",
        answerD = "Der Allee-Effekt beschreibt Dichteabhaengigkeit bei hohen Dichten",
        correctAnswer = 1,
        explanation = "Der Allee-Effekt (Allee 1931) beschreibt positive Dichteabhaengigkeit bei niedrigen Dichten: zu kleine Populationen finden schwerer Partner, haben schlechtere Gruppenverteidigung, mehr Inzucht. Unterhalb einer kritischen Schwelle sinkt die Pro-Kopf-Wachstumsrate negativ -- die Population bewegt sich in einen Extinction Vortex. Dies ist besonders relevant fuer Grossraubkatzen und stark dezimierte Arten.",
        difficulty = 5,
        funFact = "Der Nordamerika-Wandertaube (Ectopistes migratorius) kollabierte vermutlich nicht nur durch Jagd, sondern durch den Allee-Effekt: Die Art war koloniell und brauchte Millionen von Individuen fuer normales Brutverhalten."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche biogeographische Theorie erklaert, warum ozeanische Inseln in der Naehe von Kontinenten vergleichsweise wenige Endemiten haben?",
        answerA = "Nahe Inseln werden staerker durch Sturmflueten beeinflusst",
        answerB = "Hohe Immigrationsraten von nahen Inseln bremsen durch Genetic Swamping die lokale Adaptation und reduzieren Speziation",
        answerC = "Nahe Inseln haben geringere Klimatransformationsraten",
        answerD = "Nahe Inseln sind geolgisch juenger und hatten weniger Zeit fuer Endemismus",
        correctAnswer = 1,
        explanation = "Hohe Immigrationsraten zu nahen Inseln foehren zu Genetic Swamping: kontinuierlicher Genfluss von der Quellpopulation verhindert genetische Differenzierung und damit Speziation. Ferne Inseln (Hawaii, Galapagos) haben weniger Genfluss und daher hoehere Speziationsraten und Endemismus. Dies ist der 'immigration effect' auf die Speziation in der Inselbiogeographie.",
        difficulty = 5,
        funFact = "Auf Hawaii sind ueber 90 % der einheimischen Tierarten endemisch -- Ergebnis extremer geografischer Isolation bei 3.800 km Entfernung zum naechsten Kontinent."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist das 'press vs. pulse disturbance' Konzept in der Stoerungsoeokologie?",
        answerA = "Pulsartige Stoerungen (einmalige Ereignisse wie Feuer) vs. dauerhafte Stoerungen (continuous press wie Overgrazing) mit unterschiedlichen Gemeinschaftseffekten",
        answerB = "Hohe Stoerungsfrequenz (press) vs. niedrige Intensitaet (pulse) -- beide fuehren zu Klimax-Gemeinschaften",
        answerC = "Biochemische Stressantworten von Pflanzen auf kurze vs. lange Stoerungsphasen",
        answerD = "Anthropogene (pulse) vs. natuerliche (press) Stoerungen im Vergleich",
        correctAnswer = 0,
        explanation = "Press-Stoerungen sind anhaltend (chronischer Stress: Overgrazing, Luftverschmutzung) und fuehren zu neuen Gleichgewichten. Pulse-Stoerungen sind episodisch (Feuer, Sturm, Ueberschwemmung) und ermoeglichen Reset der Gemeinschaft. Die Intermediate Disturbance Hypothesis gilt hauptsachlich fuer Pulse-Stoerungen. Press-Stoerungen fuehren oft zu Zustandsuebergaengen (regime shifts) statt Rueckehr zum Ausgangsgleichgewicht.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Signatur im Fossilbericht markiert das Ende des 'Great American Biotic Interchange' und was charakterisiert die Verlierer-Taxa?",
        answerA = "Alle suedamerikanischen endemischen Saeugetiere ueberlebten die Begegnung mit nordamerikanischen Arten",
        answerB = "Suedamerikanische Gross-Saeugetier-Endemiten (Notoungulata, Litopterna, Sparassodonta) starben mehrheitlich aus; nord-amerikanische Invasoren und Suedamerika-Auswanderer (Faultiere, Guerteltiere) ueberlebten besser",
        answerC = "Nordamerikanische Gross-Saeugetiere starben aus; suedamerikanische Arten uebernahmen den Norden",
        answerD = "Der Austausch war symmetrisch -- beide Faunen blieben gleich artenreich",
        correctAnswer = 1,
        explanation = "Das Great American Biotic Interchange (GABI) nach Schluss der Panama-Landenge (~3 Mio. Jahre) fuehrte zum Aussterben fast aller charakteristischen suedamerikanischen Gross-Saeugetier-Ordnungen (Notoungulata, Litopterna). Nordamerikanische Invasoren (Pferde, Tapire, Cameliden, Saebelzahnkatzen) verdrangen sie. Suedamerikanische Auswanderer (Faultiere, Guerteltiere, Opossums) ueberlebten jedoch gut im Norden.",
        difficulty = 5,
        funFact = "Die Asymmetrie -- mehr nordamerikanische Gewinner als suedamerikanische -- koennte auf den Praedationsdruck im Norden zurueckzufuehren sein: nordamerikanische Herbivoren waren an mehr Raeuber angepasst."
    ),

    Question(
        categoryId = 9,
        questionText = "Was versteht man unter 'trophic rewilding' und welche Schluesselarten sind Forschungsfokus?",
        answerA = "Kuenstliche Ernaehrung von gefaehrdeten Tierarten in Zoologischen Gaerten",
        answerB = "Wiedereinbuergerung von Schlusselsteinarten und Megafauna zur Wiederherstellung oekosystemarer Prozesse (z.B. Woelfe, Biber, Elefanten)",
        answerC = "Oekologische Ernaehrungsforschung in tropischen Waeldern",
        answerD = "Digitale Modellierung von Nahrungsnetzen fuer Renaturierungsplanung",
        correctAnswer = 1,
        explanation = "Trophisches Rewilding (Soule & Terborgh 1999, Vera 2000, Svenning 2015) zielt darauf ab, fehlende trophische Ebenen wiederherzustellen durch Einbringung funktioneller Aequivalente ausgestorbener Megafauna. Klassische Beispiele: Woeffe und Luchs in Europa, Rewilding von Przewalski-Pferden und Wisenten, theoretisch auch Mammut-Aequivalente (Elefanten in Sibirien, Pleistocene Park).",
        difficulty = 5,
        funFact = "Sergei Zimov fuehrt in Sibirien den Pleistocene Park als Langzeit-Experiment: Grosse Herbivoren sollen die Subarktische Tundra in Mammut-Steppe rueckverwandeln und CO2-Freisetzung durch Permafrost-Auftauen verlangsamen."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie definiert die 'Metabolic Theory of Ecology' den Zusammenhang zwischen Koerpermasse und Populationsdichte?",
        answerA = "Populationsdichte korreliert positiv mit Koerpermasse: groessere Tiere kommen haeufiger vor",
        answerB = "Populationsdichte skaliert negativ mit der Koerpermasse nach M^(-3/4): kleinere Tiere haben hoehere Dichten",
        answerC = "Populationsdichte ist unabhaengig von der Koerpermasse",
        answerD = "Populationsdichte skaliert mit M^(1/2) -- Quadratwurzel-Gesetz",
        correctAnswer = 1,
        explanation = "Aus der MTE (Brown et al. 2004) folgt: da der Energiebedarf mit M^(3/4) skaliert und die Gesamtenergie im Oekosystem begrenzt ist, muss die Populationsdichte mit M^(-3/4) skalieren. Dies erklaert, warum ein Quadratkilometer Savanne Tausende Insekten, aber nur wenige Elefanten traegt. Damuth (1981) belegte diese Beziehung empirisch ueber Saeugetiere.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist das 'humped-back model' der Grime-Diversity-Produktivitaets-Hypothese und wo liegt die maximale Artendiversitaet?",
        answerA = "Artenvielfalt ist am hoechsten bei maximaler Produktivitaet (eutrophe Systeme)",
        answerB = "Artenvielfalt zeigt eine Kurve mit Maximum bei mittlerer Produktivitaet -- zu wenig Energie limitiert Arten, zu viel foerdert kompetitive Dominanz weniger Arten",
        answerC = "Artenvielfalt ist unabhaengig von der Produktivitaet",
        answerD = "Artenvielfalt ist am hoechsten bei niedrigster Produktivitaet (oligotrophe Systeme)",
        correctAnswer = 1,
        explanation = "Grimes Humped-Back-Modell (1979) und der DPDR (Diversity-Productivity Diversity Relationship) sagen ein Maximum bei mittlerer Produktivitaet voraus. Bei niedriger Produktivitaet limitieren Ressourcen die Artenanzahl. Bei hoher Produktivitaet (z.B. Eutrophierung) dominieren wenige hochkompetitive Arten und schliessen andere aus. Mittlere Produktivitaet ermoeglicht groesste Nischenvielfalt.",
        difficulty = 5,
        funFact = "Das Grime-Modell wird kontrovers diskutiert: Empirische Tests zeigen je nach Massstat (alpha vs. gamma-Diversitaet, Taxon, Massstab) sehr unterschiedliche Kurvenformen."
    ),

    Question(
        categoryId = 9,
        questionText = "Welchen Namen traegt die Biogeographische Region, die Indien, Sri Lanka, Indochina, Malaysia und die Sundainseln umfasst?",
        answerA = "Australasische Region",
        answerB = "Indomalayische (Orientalische) Region",
        answerC = "Palearktische Region",
        answerD = "Aethiopische Region",
        correctAnswer = 1,
        explanation = "Die Indomalayische oder Orientalische Region umfasst Suedasien, Suedostasien und die Sundainseln bis zur Wallace-Linie. Sie ist eine der sechs Sclater-Wallace-Regionen und charakterisiert durch endemische Saeugetierfamilien wie Tupaiidae (Spitzhoeruner), Tarsiidae (Koboldmakis), Hylobatidae (Gibbons) und Rhinocerotidae (Sumatranashorn).",
        difficulty = 5,
        funFact = "Die Indomalayische Region hat eine der hoechsten Bedrohungsraten: Fast alle grossen Saeugetierarten (Orang-Utans, Tiger, Sumatranashorn) sind kritisch gefaehrdet durch Habitatverlust."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist die 'mass effects' Komponente im Metacommunity-Framework nach Leibold et al. (2004)?",
        answerA = "Grosse Populationsgroessen dominieren kleine Patches durch Praedation",
        answerB = "Hohe Immigrationsraten aus Quellpopulationen (source patches) erhalten Arten in Senkenpopulationen (sink patches), in denen sie lokal nicht lebensfaehig waeren",
        answerC = "Stochastische Massenereignisse (Fluten, Brande) resetten Gemeinschaften periodisch",
        answerD = "Massenmigration saisonal wandernder Arten beeinflusst lokale Gemeinschaften",
        correctAnswer = 1,
        explanation = "Mass Effects (Shmida & Wilson 1985, Leibold 2004) beschreiben, wie hochfrequente Immigration aus produktiven Quellpatches (source) suboptimale Senkenpatches (sink) mit Arten bestuckt, die dort ohne Immigration aussterben wuerden. Dies erhoht die lokale Artenzahl kuenstlich ueber das Nischen-Gleichgewicht. Mass Effects sind typisch fuer kontinuierlich verbundene Landschaften mit heterogenen Habitaten.",
        difficulty = 5,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher historische Forscher preagte 1876 die heutige Einteilung der Erde in sechs zoogeographische Regionen und wie heissen diese?",
        answerA = "Philip Sclater (1858) -- Palaearktis, Nearktis, Neotropis, Aethiopis, Orientalis, Australis",
        answerB = "Alfred Russel Wallace (1876) -- Palaearktisch, Nearktisch, Neotropisch, Aethiopisch, Orientalisch, Australisch",
        answerC = "Charles Darwin (1859) -- basierend auf Fossilbefunden der Beagle-Expedition",
        answerD = "Alexander von Humboldt (1807) -- Palaearktis, Nearktis, Neotropis, Pantropika, Australis, Antarktis",
        correctAnswer = 1,
        explanation = "Alfred Russel Wallace systematisierte in 'The Geographical Distribution of Animals' (1876) die sechs klassischen zoogeographischen Regionen: Palaearktisch (Europa, Asien, Nordafrika), Nearktisch (Nordamerika), Neotropisch (Suedamerika, Mittelamerika), Aethiopisch (Subsahara-Afrika), Orientalisch (Suedostasien) und Australisch. Diese Einteilung basiert auf Saeugetier-Verbreitungsmustern und gilt noch heute.",
        difficulty = 5,
        funFact = "Wallace bezeichnete die Grenzlinie zwischen der Orientalischen und der Australischen Region -- die spaeter nach ihm benannte Wallace-Linie -- als scharf wie eine Mauer."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist der Kern des 'Source-Sink Dynamics' Konzepts nach Pulliam (1988) in der Populationsoekologie?",
        answerA = "Quellenhabitate (source) sind Regionen mit negativem Populationswachstum (lambda < 1), Senkenhabitate (sink) mit positivem Wachstum",
        answerB = "In Quellhabitaten (lambda > 1) uebersteigt Reproduktion die Mortalitaet und erzeugt Emigranten; Sinkhabitaten (lambda < 1) werden durch Immigration aufrechterhalten",
        answerC = "Quellen sind Nahrungsressourcen, Senken sind Praedationshotspots",
        answerD = "Source-Sink-Dynamik gilt nur fuer migrierende Vogelarten",
        correctAnswer = 1,
        explanation = "Pulliams (1988) Source-Sink-Modell: In Quellhabitaten (lambda > 1, gute Qualitaet) produzieren Populationen einen Ueberschuss an Individuen, die in Senkenpopulationen (lambda < 1, schlechte Qualitaet) abwandern und diese aufrechterhalten. Ohne Immigration wuerden Sinkpopulationen aussterben. Dieses Konzept revolutionierte Naturschutzplanung: die Qualitaet von Quellhabitaten ist entscheidend fuer das regionale Ueberleben von Arten.",
        difficulty = 5,
        funFact = "Paradoxerweise kann man Tiere in attraktiven Sinkhabitaten (z.B. in der Naehe von Futterstellen) beobachten, auch wenn sie dort nicht ueberleben wuerden -- ein sogenannte 'Attractive Sink' oder ecological trap."
    ),

)
