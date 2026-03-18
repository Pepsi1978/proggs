package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun geoQuestionsExpert2(): List<Question> = listOf(

    // ── EXPERT (difficulty = 4) ── 21 questions ─────────────────────────────

    // Exklaven & Enklaven
    Question(
        categoryId = 1,
        questionText = "Welches Land besitzt eine Exklave namens Nahitschewan, die vollstaendig von Armenien und dem Iran umschlossen ist?",
        answerA = "Georgien",
        answerB = "Turkmenistan",
        answerC = "Aserbaidschan",
        answerD = "Russland",
        correctAnswer = 2,
        explanation = "Nahitschewan ist eine autonome Republik Aserbaidschans, die territorial komplett vom Mutterland getrennt ist. Im Norden grenzt sie an Armenien, im Sueden an den Iran und die Tuerkei.",
        difficulty = 4,
        funFact = "Nahitschewan gilt als einer der moeglichen Standorte des Grabes von Noah – zumindest laut lokaler Ueberlieferung, die den Namen der Region auf 'Noahs Ort' zurueckfuehrt."
    ),

    Question(
        categoryId = 1,
        questionText = "Die Enklave Llívia gehoert zu Spanien, liegt aber vollstaendig auf franzoesischem Territorium. In welchem Gebirge befindet sie sich?",
        answerA = "Kantabrisches Gebirge",
        answerB = "Pyrenaeaen",
        answerC = "Sierra Nevada",
        answerD = "Massif Central",
        correctAnswer = 1,
        explanation = "Llívia ist eine spanische Gemeinde mitten in den franzoeischen Pyrenaeaen, etwa 6 km von der eigentlichen spanisch-franzoesischen Grenze entfernt. Der Vertrag der Pyrenaeaen von 1659 uebergab Frankreich 33 Doerfer – Llívia war jedoch eine Stadt und wurde deshalb ausgenommen.",
        difficulty = 4,
        funFact = "Llívia verfuegt ueber die aelteste Apotheke Europas, die bereits seit dem 15. Jahrhundert in Betrieb sein soll."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches ist die einzige vollstaendige Enklave (ein Staat innerhalb eines anderen Staates) auf dem amerikanischen Kontinent?",
        answerA = "San Marino",
        answerB = "Lesotho",
        answerC = "Monaco",
        answerD = "Es gibt keine auf dem amerikanischen Kontinent",
        correctAnswer = 3,
        explanation = "Auf dem amerikanischen Kontinent gibt es keine vollstaendige Staatenklave (Enklave). Lesotho liegt in Afrika innerhalb Suedafrikas. San Marino und der Vatikan liegen in Europa innerhalb Italiens.",
        difficulty = 4,
        funFact = "Lesotho ist die einzige Nation weltweit, deren gesamtes Territorium ueber 1.000 Meter Meereshoehe liegt – es wird daher als 'Koenigreich im Himmel' bezeichnet."
    ),

    Question(
        categoryId = 1,
        questionText = "Die Stadt Baarle besteht aus einem belgischen und einem niederlaendischen Teil mit zahlreichen gegenseitigen Enklaven. Wie heisst der niederlaendische Teil offiziell?",
        answerA = "Baarle-Nassau",
        answerB = "Baarle-Hertog",
        answerC = "Baarle-Noord",
        answerD = "Baarle-Oranje",
        correctAnswer = 0,
        explanation = "Baarle-Nassau ist die niederlaendische Gemeinde, waehrend Baarle-Hertog der belgische Teil ist. Die beiden Gemeinden sind durch ein komplexes System von Enklaven und Gegenklaven miteinander verflochten – insgesamt gibt es ueber 20 belgische Exklaven in den Niederlanden.",
        difficulty = 4,
        funFact = "In Baarle verlaueft die Staatsgrenze mitten durch Haeuser. Wenn die Haustuer auf belgischer Seite liegt, gilt belgisches Recht – also andere Oeffnungszeiten fuer Laeden und andere Steuern."
    ),

    // Grenzverlaeufe
    Question(
        categoryId = 1,
        questionText = "Welcher Fluss bildet die Grenze zwischen den USA und Mexiko auf der gesamten texanischen Seite?",
        answerA = "Colorado River",
        answerB = "Pecos River",
        answerC = "Rio Grande",
        answerD = "Nueces River",
        correctAnswer = 2,
        explanation = "Der Rio Grande (in Mexiko: Rio Bravo) bildet die gesamte texanisch-mexikanische Grenze ueber rund 2.000 Kilometer. Er entspringt in den Rocky Mountains in Colorado und muendet in den Golf von Mexiko.",
        difficulty = 4,
        funFact = "Der Rio Grande verlegt immer wieder seinen Lauf durch Ueberschwemmungen und Erosion – was zu wiederholten Grenzstreitigkeiten zwischen den USA und Mexiko gefuehrt hat."
    ),

    Question(
        categoryId = 1,
        questionText = "Der 49. Breitengrad bildet die Grenze zwischen den USA und Kanada westlich des Lake of the Woods. Welcher Vertrag legte diese Grenze 1818 fest?",
        answerA = "Vertrag von Paris",
        answerB = "Rush-Bagot-Vereinbarung",
        answerC = "Konvention von 1818",
        answerD = "Webster-Ashburton-Vertrag",
        correctAnswer = 2,
        explanation = "Die Konvention von 1818 (auch Anglo-American Convention) legte den 49. Breitengrad als Grenze vom Lake of the Woods bis zu den Rocky Mountains fest. Der restliche Verlauf bis zum Pazifik wurde erst 1846 durch den Oregon-Vertrag bestimmt.",
        difficulty = 4,
        funFact = "Die Grenze zwischen USA und Kanada gilt als die laengste internationale Grenze der Welt, die zwei Laender teilt, mit rund 8.891 km Laenge."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche afrikanische Grenze verlaeuft als perfekt gerade Linie ueber den 22. Breitengrad Nord und trennt Aegypten von Sudan?",
        answerA = "Diese Grenze existiert nicht, sie ist gebirgig",
        answerB = "Die sogenannte 'astronomische Grenze' von 1899",
        answerC = "Die Kolonialgrenze von 1884",
        answerD = "Die post-koloniale Grenze von 1956",
        correctAnswer = 1,
        explanation = "Die geradlinige Grenze zwischen Aegypten und Sudan am 22. Breitengrad Nord wurde 1899 durch eine anglo-aegyptische Konvention als 'astronomische Grenze' festgelegt. Diese Grenzziehung ist einer der Gruende fuer den Konflikt um das Hala'ib-Dreieck.",
        difficulty = 4,
        funFact = "Afrika hat besonders viele gerade Grenzen, weil europaeische Kolonialmachte beim Berliner Kongress 1884/85 den Kontinent ohne Ruecksicht auf ethnische oder geografische Gegebenheiten aufteilten."
    ),

    // Geographische Extrempunkte
    Question(
        categoryId = 1,
        questionText = "Welcher Punkt auf der Erde ist am weitesten vom Erdmittelpunkt entfernt (gemessen vom Erdmittelpunkt, nicht vom Meeresspiegel)?",
        answerA = "Mount Everest",
        answerB = "Chimborazo (Ecuador)",
        answerC = "Mauna Kea (Hawaii)",
        answerD = "Kilimandscharo",
        correctAnswer = 1,
        explanation = "Der Chimborazo in Ecuador (6.268 m ueber Meeresspiegel) ist wegen der Erdabplattung am Aequator am weitesten vom Erdmittelpunkt entfernt – etwa 6.384 km, gegenueber dem Everest mit nur 6.382 km. Der Meeresspiegel am Aequator liegt bereits weiter vom Zentrum entfernt.",
        difficulty = 4,
        funFact = "Wenn man den Chimborazo besteigt, ist man dem Weltall naeher als auf dem Everest – trotz der fast 2.600 m geringeren absoluten Hoehe ueber dem Meeresspiegel."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Punkt Europas gilt als oestlichster Punkt des Kontinents (nach der klassischen Definition Europa bis zum Uralgebirge)?",
        answerA = "Kap Nordkinn (Norwegen)",
        answerB = "Der Ural-Fuss bei Orenburg (Russland)",
        answerC = "Bosporus-Kueste bei Istanbul",
        answerD = "Kap Dezhnjow (Russland)",
        correctAnswer = 1,
        explanation = "Nach der gaengigsten geografischen Definition bildet das Ural-Gebirge die Ostgrenze Europas. Der oestlichste Punkt liegt demnach am Ural, etwa bei Orenburg in Russland. Kap Dezhnjow liegt in Sibirien und gehoert zu Asien.",
        difficulty = 4,
        funFact = "Die Frage 'Wo endet Europa?' ist wissenschaftlich umstritten – manche Definitionen nutzen den Ural, andere den Kaukasus, wieder andere kulturelle oder politische Kriterien."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Kontinent hat seinen Schwerpunkt (geografisches Zentrum) auf dem Suedlichen Ozean?",
        answerA = "Antarktis",
        answerB = "Australien",
        answerC = "Keiner – kein Kontinent liegt so weit suedlich",
        answerD = "Suedamerika",
        correctAnswer = 0,
        explanation = "Das geografische Zentrum der Antarktis liegt ungefaehr beim Suedpol, also mitten im Polaris-Plateau weit im Inneren des Kontinents – nicht auf dem Suedlichen Ozean. Die Antarktis selbst grenzt den Suedlichen Ozean ein.",
        difficulty = 4,
        funFact = "Die Antarktis ist der kaelteste, trockenste und windigste Kontinent der Erde. Trotz ihrer riesigen Eismengen gilt sie als Wueste, da der jaehrliche Niederschlag weniger als 200 mm betraegt."
    ),

    // Geologie
    Question(
        categoryId = 1,
        questionText = "Auf welchem tektonischen Phaenoemen basiert die Entstehung des Ostafrikanischen Grabenbruchs?",
        answerA = "Subduktionszone",
        answerB = "Transformstoerung",
        answerC = "Kontinentaler Rifting-Prozess",
        answerD = "Hotspot-Vulkanismus",
        correctAnswer = 2,
        explanation = "Der Ostafrikanische Grabenbruch entsteht durch kontinentalen Rifting: Die Afrikanische Platte reisst auseinander, wodurch ein neues Becken entsteht. Dieser Prozess koennte in Millionen von Jahren einen neuen Ozean entstehen lassen.",
        difficulty = 4,
        funFact = "Der Ostafrikanische Grabenbruch beherbergt einige der tiefsten Seen der Welt, darunter den Tanganjikasee (1.470 m tief) und den Malawisee."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Gesteinsart bildet den Grossteil der ozeanischen Kruste?",
        answerA = "Granit",
        answerB = "Kalkstein",
        answerC = "Basalt",
        answerD = "Gneis",
        correctAnswer = 2,
        explanation = "Ozeanische Kruste besteht hauptsaechlich aus Basalt, einem dunklen, magmatischen Ergussgestein. Sie ist dichter und duenner (5-10 km) als die kontinentale Kruste (30-70 km), die aus leichterem Granit besteht.",
        difficulty = 4,
        funFact = "Ozeanische Kruste ist niemals aelter als etwa 200 Millionen Jahre, weil sie staendig an Subduktionszonen in den Erdmantel zurueckgezogen wird. Kontinentale Kruste kann hingegen ueber 4 Milliarden Jahre alt sein."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Schicht der Erde befindet sich zwischen Erdkruste und Erdkern?",
        answerA = "Asthenosphaere",
        answerB = "Erdmantel",
        answerC = "Lithosphaere",
        answerD = "Mesosphaere",
        correctAnswer = 1,
        explanation = "Der Erdmantel liegt zwischen der Erdkruste und dem Erdkern und macht etwa 84 % des Erdvolumens aus. Er besteht aus silikatischen Gesteinen und erstreckt sich von etwa 30 km bis 2.900 km Tiefe.",
        difficulty = 4,
        funFact = "Im Erdmantel existieren Konvektionsstroeme, die ueber hunderte Millionen Jahre hinweg die tektonischen Platten antreiben – aehnlich wie siedendes Wasser in einem Topf."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie heisst der geologische Supercontinent, der vor etwa 300 Millionen Jahren existierte und spaeter auseinanderbrach?",
        answerA = "Rodinia",
        answerB = "Gondwana",
        answerC = "Pangaea",
        answerD = "Laurasia",
        correctAnswer = 2,
        explanation = "Pangaea war der letzte Supercontinent, der vor etwa 300-200 Millionen Jahren existierte. Er zerbrach in Laurasia (Norden) und Gondwana (Sueden), die sich dann weiter in die heutigen Kontinente aufspalteten.",
        difficulty = 4,
        funFact = "Der Name Pangaea stammt vom griechischen Wort fuer 'alle Erde'. Alfred Wegener schlug 1912 erstmals die Kontinentalverschiebungstheorie vor, wurde damals aber von der Wissenschaft belacht."
    ),

    // Klimazonen
    Question(
        categoryId = 1,
        questionText = "Welche Klimaklassifikation gilt fuer die tropischen Regenwaelder Amazoniens?",
        answerA = "Aw – Tropisches Savannenaklima",
        answerB = "Af – Tropisches Regenwaldklima",
        answerC = "Am – Tropisches Monsunklima",
        answerD = "Bs – Trockenes Steppenaklima",
        correctAnswer = 1,
        explanation = "Das Amazonasbecken hat ein Af-Klima nach Koeppen: tropisches Regenwaldklima mit ganzjaehrig hohen Temperaturen und gleichmaessig hohem Niederschlag (ueber 60 mm im trockenstem Monat).",
        difficulty = 4,
        funFact = "Der Amazonas-Regenwald produziert etwa 20 % des weltweiten Sauerstoffs und wird deshalb oft als 'Lunge der Erde' bezeichnet – obwohl er naechtens genauso viel CO2 durch Atmung abgibt."
    ),

    Question(
        categoryId = 1,
        questionText = "Die Atacama-Wueste gilt als trockenste nichtpolare Wueste der Erde. Durch welches Phaenoemen entsteht ihre extreme Trockenheit?",
        answerA = "Kontinentalitaet und grosse Entfernung zum Meer",
        answerB = "Kaltluftmassen vom Suedpol",
        answerC = "Kalter Humboldtstrom und Lee-Effekt der Anden",
        answerD = "Hochdruckgebiete und Passatwinde",
        correctAnswer = 2,
        explanation = "Die Trockenheit der Atacama entsteht durch das Zusammenwirken des kalten Humboldtstroms (kuehlte Meeresluft ab und verhindert Niederschlag) und dem Regenschatten-Effekt (Lee-Effekt) der Anden, die feuchte Amazonasluft abhalten.",
        difficulty = 4,
        funFact = "In manchen Teilen der Atacama hat es seit Menschengedenken nie geregnet. Trotzdem ist sie nicht menschenleer – die Inca bauten hier Strassen und sie wird heute fuer Astronomie genutzt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Begriff beschreibt das Klimaphaenoemen, bei dem Temperaturen mit zunehmender Hoehenmetern in der Atmosphaere sinken?",
        answerA = "Thermische Inversion",
        answerB = "Adiabatischer Temperaturgradient",
        answerC = "Foehn-Effekt",
        answerD = "Orographischer Niederschlag",
        correctAnswer = 1,
        explanation = "Der adiabatische Temperaturgradient beschreibt die Abkuehlung von Luft beim Aufsteigen ohne Waermeaustausch mit der Umgebung. Trockene Luft kuehlt um ca. 1°C pro 100 m ab (trockenadiabatisch), feuchte Luft um 0,6°C (feuchtadiabatisch).",
        difficulty = 4,
        funFact = "Der Foehn ist ein Sonderfall: Luft steigt feuchtadiabatisch auf und sinkt dann trockenadiabatisch ab – deshalb ist die Lee-Seite viel waermer und trockener als die Luv-Seite."
    ),

    Question(
        categoryId = 1,
        questionText = "Was versteht man unter 'ITCZ' in der Meteorologie und wo liegt sie hauptsaechlich?",
        answerA = "Intertropische Konvergenzzone, nah am Aequator",
        answerB = "Internationale Tiefdruckzone des Zentralmeridians",
        answerC = "Innertropische Klimazonierung",
        answerD = "Isotherme Temperaturkurve der Zone",
        correctAnswer = 0,
        explanation = "Die ITCZ (Intertropical Convergence Zone) ist ein Guertel niedrigen Luftdrucks nahe dem Aequator, wo Passatwinde aus Norden und Sueden zusammentreffen. Sie ist fuer die tropischen Regenfaelle und die Bildung von Hurrikanen relevant.",
        difficulty = 4,
        funFact = "Die ITCZ wandert saisonal – im Nordsommer liegt sie noerodlich des Aequators, im Suedwinter suedlich. Segler nannten diesen windstillen Bereich einst die 'Pferdbreiten' oder 'Doldrums'."
    ),

    Question(
        categoryId = 1,
        questionText = "In welchem Meeresbereich ist der Golfstrom am schwaechsten und droht am ehesten abzureissen?",
        answerA = "Karibisches Meer",
        answerB = "Irisches Meer",
        answerC = "Nordatlantik noerdlich von Island",
        answerD = "Barents-See",
        correctAnswer = 2,
        explanation = "Der Golfstrom verlaeuft als Teil der AMOC (Atlantisch-Meridionalen Umwaelzzirkulation) bis in den Nordatlantik, wo er durch Abkuehlung und Salzgehalt-Aenderungen absinkt. Klimaforscher warnen, dass zuschmelzendes Eis dort die AMOC verlangsamen koennte.",
        difficulty = 4,
        funFact = "Wenn der Golfstrom kollabierte, wuerden Temperaturen in Westeuropa um bis zu 8°C sinken – London waere dann so kalt wie Helsinki."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Klimazone ist durch kurze, kuehle Sommer und lange, sehr kalte Winter mit geringem Niederschlag gekennzeichnet und bedeckt weite Teile Sibiriens?",
        answerA = "Subpolare Klimazone (Tundra)",
        answerB = "Boreales Klima (Taiga/Dfc)",
        answerC = "Kontinentales Steppenaklima (BSk)",
        answerD = "Subarktisches Wuestenklima (EF)",
        correctAnswer = 1,
        explanation = "Das boreale Klima (Taiga, Koeppen Dfc/Dfd) praegte Sibirien: sehr kalte, lange Winter (bis -50°C), kurze Sommer und wenig Niederschlag. Es ist die groesste Klimazone der Erde nach Flaeche.",
        difficulty = 4,
        funFact = "In der sibirischen Stadt Ojmjakon wurde 1924 die tiefste natuerliche Temperatur ausserhalb der Antarktis gemessen: -67,7°C. Die Bewohner nennen es 'Pol der Kaelte'."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Phaenoemen erklaert, warum Wuesten haeufig auf der Westseite von Kontinenten in subtropischen Breiten entstehen?",
        answerA = "Hadley-Zellen erzeugen dort absinkende Luftmassen",
        answerB = "Westwaerts gerichtete Passatwinde bringen kein Wasser",
        answerC = "Kontinentaldrift hat diese Regionen vom Ozean entfernt",
        answerD = "Meereskuehle verhindert Wolkenbildung",
        correctAnswer = 0,
        explanation = "In den Subtropen (ca. 20-30° Nord/Sued) sinken im Rahmen der Hadley-Zirkulation trockene Luftmassen ab. Das Absinken erwaermt und trocknet die Luft, verhindert Niederschlag und schafft die Bedingungen fuer Wuustenklimate.",
        difficulty = 4,
        funFact = "Die Sahara war vor etwa 6.000-11.000 Jahren viel gruener ('Gruene Sahara') mit Seen, Fluessen und Weideland. Veraenderte Erdbahnparameter liessen das Monsunklima zurueckziehen."
    ),

)
