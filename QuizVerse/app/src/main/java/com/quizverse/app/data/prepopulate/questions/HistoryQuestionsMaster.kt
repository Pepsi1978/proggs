package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun historyQuestionsMaster(): List<Question> = listOf(

    // --- Minoan / Mycenaean Civilizations ---

    Question(
        categoryId = 3,
        questionText = "Welches minoische Schriftsystem auf Kreta wurde bislang nicht entziffert und enthält ausschließlich piktografische Zeichen, die älter als Linear A sind?",
        answerA = "Kretische Hieroglyphenschrift",
        answerB = "Linear A",
        answerC = "Linear B",
        answerD = "Kyprische Silbenschrift",
        correctAnswer = 0,
        explanation = "Die kretische Hieroglyphenschrift (ca. 2100–1700 v. Chr.) ist das älteste Schriftsystem Kretas und wurde bis heute nicht vollständig entziffert. Sie unterscheidet sich von Linear A, das ebenfalls unverstanden ist, durch ihren piktografischen Charakter.",
        difficulty = 5,
        funFact = "Auf dem Diskos von Phaistos, einem der rätselhaftesten Artefakte der Archäologie, wurde eine bislang unlesbare Spiralschrift gefunden – ob sie zur kretischen Hieroglyphenschrift gehört, ist unter Forschern umstritten."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches mykenische Depot-Dokument aus Pylos belegt die systematische Rationierung von Olivenöl an Tempelpersonal und gilt als wichtigster Nachweis der palatialen Wirtschaft auf dem griechischen Festland?",
        answerA = "Die Ta-Tafeln",
        answerB = "Die Fn-Tafeln",
        answerC = "Die Es-Tafeln",
        answerD = "Die Un-Tafeln",
        correctAnswer = 1,
        explanation = "Die Fn-Tafeln aus Pylos dokumentieren Olivenöllieferungen an verschiedene Kultstätten und belegen die enge Verflechtung von Palastwirtschaft und Religionspflege im mykenischen Griechenland (ca. 1200 v. Chr.).",
        difficulty = 5,
        funFact = "Die Linear-B-Tafeln aus Pylos wurden 1939 wenige Monate vor dem Ausbruch des Zweiten Weltkriegs entdeckt – der Ausgräber Carl Blegen reiste sofort zurück, um seine Funde zu sichern."
    ),

    Question(
        categoryId = 3,
        questionText = "Auf welcher Insel wurde 1967 die minoische Kolonie Akrotiri entdeckt, die unter vulkanischer Asche konserviert war und oft als 'Pompeji der Ägäis' bezeichnet wird?",
        answerA = "Kea",
        answerB = "Milos",
        answerC = "Santorini (Thera)",
        answerD = "Naxos",
        correctAnswer = 2,
        explanation = "Akrotiri auf Thera (Santorini) wurde beim Theräischen Ausbruch (ca. 1628–1500 v. Chr.) unter Bimsstein und Asche begraben. Die Ausgrabungen zeigen ein wohlhabendes minoisches Handelszentrum mit aufwendigen Fresken und einem ausgefeilten Entwässerungssystem.",
        difficulty = 5,
        funFact = "In Akrotiri wurde keine einzige menschliche Leiche gefunden – die Bewohner scheinen die Stadt rechtzeitig verlassen zu haben, nahmen aber auch keinen Schmuck oder Edelmetalle mit, was auf eine plötzliche Evakuierung hindeutet."
    ),

    // --- Phoenician Alphabet Origins ---

    Question(
        categoryId = 3,
        questionText = "Welche proto-sinaitische Inschrift aus dem 19. Jahrhundert v. Chr., gefunden in den Türkisgruben des Sinai, gilt als einer der frühesten Belege für die Entwicklung des phonizischen Alphabets aus ägyptischen Hieroglyphen?",
        answerA = "Wadi el-Hol Inschriften",
        answerB = "Serabit el-Khadim Inschriften",
        answerC = "Byblos Pseudo-Hieroglyphen",
        answerD = "Ugarit Keilinschriften",
        correctAnswer = 1,
        explanation = "Die proto-sinaitischen Inschriften von Serabit el-Khadim (ca. 1850–1550 v. Chr.) gelten als wichtigste frühe Zeugnisse des Akonsonanten-Alphabets. Die semitischen Bergarbeiter adaptierten ägyptische Zeichen akrophonisch für ihre Sprache.",
        difficulty = 5,
        funFact = "Das Prinzip war genial einfach: Man nahm das ägyptische Zeichen für 'Ochse' (aleph) und verwendete es für den Konsonanten 'A' – unser heutiges 'A' ist ein auf den Kopf gestellter Ochsenkopf."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Stadt an der levantinischen Küste gilt als Geburtsort des ältesten datierten phonizischen Alphabetinschrift, der Ahiram-Sarkophaginschrift aus dem 10. Jahrhundert v. Chr.?",
        answerA = "Tyros",
        answerB = "Sidon",
        answerC = "Byblos",
        answerD = "Ugarit",
        correctAnswer = 2,
        explanation = "Die Ahiram-Inschrift aus Byblos (ca. 1000 v. Chr.) ist eine der frühesten vollständig erhaltenen phonizischen Alphabetinschriften. Byblos (griechisch: 'Buch') war wegen seines Papyrushandels mit Ägypten das wichtigste Schreibzentrum des frühen Levante.",
        difficulty = 5,
        funFact = "Das griechische Wort 'Biblos' (Buch) leitet sich direkt von Byblos ab – und damit auch unser Wort 'Bibel'."
    ),

    // --- Sea Peoples Mystery ---

    Question(
        categoryId = 3,
        questionText = "Welche Seevölkergruppe, die in ägyptischen Quellen aus der Zeit Ramses' III. als 'Peleset' bezeichnet wird, wird von den meisten Historikern mit den biblischen Philistern gleichgesetzt?",
        answerA = "Tjeker",
        answerB = "Shekelesh",
        answerC = "Peleset",
        answerD = "Weshesh",
        correctAnswer = 2,
        explanation = "Die 'Peleset' erscheinen in den Medinet-Habu-Reliefs als eine der Seevölkergruppen, die Ramses III. um 1177 v. Chr. besiegte. Ihre Verbindung zu den Philistern der Bibel wird durch archäologische Funde in Aschkelon, Aschdod und Ekron gestützt.",
        difficulty = 5,
        funFact = "Der Name 'Palästina' leitet sich etymologisch von den Philistern (Peleset) ab – durch assyrische und griechische Zwischenstufen wurde 'Philistia' zu 'Palaestina'."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher ägyptische Papyrus aus dem 12. Jahrhundert v. Chr. schildert den Seevölkerangriff als eine Koalition von Völkern aus dem Norden, 'kein Land konnte ihrer Waffe standhalten', und wird als wichtigstes literarisches Zeugnis des Seezeitenkollapses betrachtet?",
        answerA = "Harris-Papyrus I",
        answerB = "Anastasi-Papyrus III",
        answerC = "Papyrus Ipuwer",
        answerD = "Elephantine-Papyrus",
        correctAnswer = 0,
        explanation = "Der Harris-Papyrus I (Papyrus Harris) aus der Zeit Ramses' IV. beschreibt die militärischen Taten Ramses' III. einschließlich seiner Siege über die Seevölker in der Landschlacht bei Djahi und der Seeschlacht im Nil-Delta.",
        difficulty = 5,
        funFact = "Der Harris-Papyrus mit 41 Metern Länge ist der längste erhaltene Papyrus des alten Ägypten – er enthält nicht nur Kriegsberichte, sondern auch detaillierte Listen der Tempelschenkungen Ramses' III."
    ),

    // --- Etruscan Civilization ---

    Question(
        categoryId = 3,
        questionText = "Welches etruskische Heiligtum, das in Pyrgi (dem Hafen von Caere) entdeckt wurde, lieferte 1964 dreisprachige Goldtafeln auf Etruskisch und Phonizisch, die als 'Rosettastein des Etruskischen' galten?",
        answerA = "Tempel des Voltumna in Volsinii",
        answerB = "Heiligtum von Gravisca",
        answerC = "Heiligtum von Pyrgi",
        answerD = "Tempel von Populonia",
        correctAnswer = 2,
        explanation = "Die Goldtafeln von Pyrgi (ca. 500 v. Chr.) dokumentieren eine Weihung des Königs Thefarie Velianas von Caere an die Göttin Uni/Astarte in etruskischer und phonizischer Sprache – ein seltenes Zeugnis phönizisch-etruskischer Kontakte.",
        difficulty = 5,
        funFact = "Obwohl die Pyrgi-Tafeln die etruskische Entzifferung enorm halfen, bleibt das Etruskische bis heute schwer verständlich – wir können es lesen, aber das Vokabular ist zu klein, um die Texte vollständig zu übersetzen."
    ),

    Question(
        categoryId = 3,
        questionText = "Wie nannten die Etrusker sich selbst in ihrer eigenen Sprache, im Gegensatz zu den griechischen ('Tyrrhener') und lateinischen ('Tusci') Bezeichnungen?",
        answerA = "Rasenna",
        answerB = "Lutni",
        answerC = "Velchans",
        answerD = "Tarchna",
        correctAnswer = 0,
        explanation = "Die Etrusker bezeichneten sich selbst als 'Rasenna' (oder 'Rasna' in Kurzform). Dionysius von Halikarnassos überliefert dies, und etruskische Inschriften bestätigen diese Eigenbezeichnung.",
        difficulty = 5,
        funFact = "Das Tyrrhenische Meer trägt bis heute den griechischen Namen der Etrusker – 'Tyrrhener' leitet sich vom griechischen 'Tyrsenoi' ab, möglicherweise von einem Stadtnamen oder einem Vorfahren."
    ),

    // --- Carthaginian Politics ---

    Question(
        categoryId = 3,
        questionText = "Welche zwei Magistrate bildeten die oberste Exekutivbehörde Karthagos, vergleichbar mit den römischen Konsuln, und wurden nach einer semitischen Wurzel für 'König' benannt?",
        answerA = "Suffeten",
        answerB = "Gerusiarchoi",
        answerC = "Adirim",
        answerD = "Pentarchen",
        correctAnswer = 0,
        explanation = "Die Suffeten (phönizisch: 'Shophetim', Richter/Regenten) waren die zwei gewählten Magistrate Karthagos. Die Bezeichnung hängt etymologisch mit dem biblischen 'Schofetim' (Richter) zusammen und bezeichnet die höchste zivile Gewalt.",
        difficulty = 5,
        funFact = "In der Bibel heißt das Buch 'Richter' auf Hebräisch 'Sefer Shofetim' – dasselbe Wort wie für die Karthager-Magistrate. Die israelitischen Richter und karthagischen Suffeten teilten also denselben Amtstitel."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches karthagische Vertragswerk mit Rom aus dem Jahr 507/508 v. Chr., das Polybios im Original zitiert, ist das älteste datierbare Dokument der antiken Diplomatie zwischen diesen Mächten?",
        answerA = "Zweiter Römisch-Karthagischer Vertrag (348 v. Chr.)",
        answerB = "Erster Römisch-Karthagischer Vertrag (509/508 v. Chr.)",
        answerC = "Vertrag von Lutatius (241 v. Chr.)",
        answerD = "Vertrag von Ebro (226 v. Chr.)",
        correctAnswer = 1,
        explanation = "Der erste römisch-karthagische Vertrag (509/508 v. Chr.) wird von Polybios vollständig zitiert und regelte Handelszonen: Römer durften nördlich von dem 'Schönen Vorgebirge' (Cap Bon) handeln, weiter südlich war karthagisches Exklusivgebiet.",
        difficulty = 5,
        funFact = "Polybios berichtet, dass das Latein dieses Vertrags selbst für seine gebildeten römischen Zeitgenossen kaum verständlich war – so alt war die Sprache bereits im 2. Jahrhundert v. Chr."
    ),

    // --- Sassanid Empire ---

    Question(
        categoryId = 3,
        questionText = "Welche zoroastrische Textsammlung, die unter dem Sassanidenkönig Khosrow I. (531–579 n. Chr.) abgeschlossen wurde und astronomisches, medizinisches und philosophisches Wissen vereint, gilt als das enzyklopädische Hauptwerk der sassanidischen Gelehrsamkeit?",
        answerA = "Denkard",
        answerB = "Bundahishn",
        answerC = "Wizidagiha-i Zadspram",
        answerD = "Matikan-i Hazar Dadestan",
        correctAnswer = 0,
        explanation = "Der Denkard ('Taten der Religion') ist eine neunbändige mittelpersische Enzyklopädie, die unter Khosrow I. und später überarbeitete Avesta-Texte, theologische Abhandlungen und Wissen über Naturwissenschaften kompilierte.",
        difficulty = 5,
        funFact = "Khosrow I. gilt als 'Gerechter Herrscher' – er lud nach Schließung der Platonischen Akademie in Athen (529 n. Chr.) griechische Philosophen an seinen Hof und ließ Platon und Aristoteles ins Mittelpersische übersetzen."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche sassanidische Institution, die von einem 'Mobedan Mobed' (Hohepriester der Hohepriester) geleitet wurde, verwaltete das zoroastrische Kirchenvermögen und kontrollierte die Ausbildung des Klerus in Persien?",
        answerA = "Das Ataxsh-Bahram-System",
        answerB = "Die Dasturan-Dastur-Kanzlei",
        answerC = "Die Shapur-Akademie von Gundeshapur",
        answerD = "Das Herbad-Kollegium",
        correctAnswer = 1,
        explanation = "Die Dasturan-Dastur (Richter der Richter) arbeiteten eng mit dem Mobedan Mobed zusammen. Das sassanidische Zoroastriertum hatte eine stark institutionalisierte Hierarchie mit dem Mobedan Mobed an der Spitze, der sowohl religiöse als auch politische Funktionen ausübte.",
        difficulty = 5,
        funFact = "Die sassanidische Reichskirche des Zoroastrismus war so einflussreich, dass arabische Quellen nach der islamischen Eroberung 651 n. Chr. noch Jahrzehnte lang zoroastrische Priester als wichtige Ratgeber der neuen Herrscher beschreiben."
    ),

    Question(
        categoryId = 3,
        questionText = "In welchem Jahr besiegte der sassanidische König Shapur I. den römischen Kaiser Valerian in der einzigen Gefangennahme eines amtierenden römischen Kaisers durch einen fremden Feind in der Geschichte?",
        answerA = "244 n. Chr.",
        answerB = "260 n. Chr.",
        answerC = "268 n. Chr.",
        answerD = "253 n. Chr.",
        correctAnswer = 1,
        explanation = "In der Schlacht bei Edessa (260 n. Chr.) wurde Kaiser Valerian gefangen genommen – ein einmaliges Ereignis in der römischen Geschichte. Shapur ließ das Ereignis in den Felsreliefs von Naqsh-e Rostam und Naqsh-e Rajab verewigen.",
        difficulty = 5,
        funFact = "Shapur I. ließ Valerian angeblich als lebenden Fußschemel beim Aufsteigen aufs Pferd benutzen – ob Legende oder Wahrheit, diese Demütigung wurde auf den sassanidischen Felsreliefs für die Ewigkeit festgehalten."
    ),

    // --- Pre-Columbian Americas: Cahokia ---

    Question(
        categoryId = 3,
        questionText = "Welcher Erdhügel in Cahokia (heutiges Illinois), der größte präkolumbische Erdbau nördlich Mexikos, übertrifft flächenmäßig sogar die Cheops-Pyramide und wurde über mehrere Jahrhunderte schichtweise aufgeschüttet?",
        answerA = "Monks Mound",
        answerB = "Toltec Mound",
        answerC = "Etowah Mound A",
        answerD = "Poverty Point Mound A",
        correctAnswer = 0,
        explanation = "Monks Mound in Cahokia (ca. 900–1200 n. Chr.) hat eine Grundfläche von ca. 291 × 236 Metern und überragt die Cheops-Pyramide in der Grundfläche. An seiner Basis lagerten sich Hinweise auf mindestens 10 Bauphasen ab.",
        difficulty = 5,
        funFact = "Auf der Spitze des Monks Mound stand ursprünglich ein großes Holzgebäude von etwa 30 × 15 Metern – vermutlich der Palast oder Tempel des Herrschers. Cahokia war zur Hochzeit größer als das zeitgleiche London."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Phänomen, bei dem tausende von Menschen in Cahokia gleichzeitig begraben wurden, oft als Opfergaben neben einem Hauptbestatteten, liefert den stärksten archäologischen Beweis für eine hierarchische politische Struktur der Mississippian-Kultur?",
        answerA = "Mortuary Mound 72-Bestattungen",
        answerB = "Grand Plaza-Massengräber",
        answerC = "Woodhenge-Ritualbestattungen",
        answerD = "Chunkey-Stein-Opferungen",
        correctAnswer = 0,
        explanation = "Im Mound 72 wurden um einen zentralen Herrscher herum über 250 Personen begraben – viele davon junge Frauen in Massenbestattungen. Über dem Herrscher selbst lagen mehr als 20.000 Muschelperlen, aufgeschichtet zu einem Vogelumriss.",
        difficulty = 5,
        funFact = "Isotopen-Analysen der Knochen in Mound 72 zeigen, dass viele der geopferten Personen nicht aus Cahokia stammten – sie wurden aus verschiedenen Regionen herbeigebracht, was auf weit reichende Machtansprüche schließen lässt."
    ),

    // --- Tiwanaku ---

    Question(
        categoryId = 3,
        questionText = "Welches einzigartige landwirtschaftliche System der Tiwanaku-Kultur im Titicaca-Becken, das aus erhöhten Feldern zwischen wasserführenden Kanälen besteht, wurde in den 1980er Jahren wiederentdeckt und reaktiviert?",
        answerA = "Waru-Waru (Suka Kollu)",
        answerB = "Qocha-Terrassensystem",
        answerC = "Camellones-Drainage",
        answerD = "Chinampas-Äquivalent",
        correctAnswer = 0,
        explanation = "Das Waru-Waru (Aymara für 'erhöhte Felder', auch Suka Kollu) ist ein präkolumbisches System aufgeschütteter Felder, die von Wasserkanälen umgeben sind. Das Wasser speichert tagsüber Sonnenenergie und gibt sie nachts ab, was Fröste verhindert.",
        difficulty = 5,
        funFact = "Als in den 1980er Jahren Bauern im Titicaca-Becken nach diesem alten Prinzip Waru-Waru anlegten, erzielten sie Ernteerträge, die moderne Intensivlandwirtschaft übertrafen – ohne Kunstdünger oder Pestizide."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Substanz, die in Tiwanaku-Ritualgefäßen nachgewiesen wurde und aus der Pflanze Anadenanthera colubrina gewonnen wird, war zentraler Bestandteil der schamanischen Zeremonien der Andenvölker?",
        answerA = "Vilca (Bufotenin-haltiges Schnupfpulver)",
        answerB = "Ayahuasca",
        answerC = "San-Pedro-Kaktus-Mescalin",
        answerD = "Coca-Paste (Mambe)",
        correctAnswer = 0,
        explanation = "Vilca (auch Willka) ist ein halluzinogenes Schnupfpulver aus den Samen von Anadenanthera colubrina, das DMT und Bufotenin enthält. In Tiwanaku-Schnupftablett-Sets (tabletas) und Röhrchen wurde es chemisch nachgewiesen.",
        difficulty = 5,
        funFact = "Tiwanaku-Schnupfsets aus Gold, Holz und Knochen gehören zu den aufwendigsten Ritualobjekten Südamerikas – einige sind mit dem 'Stab-Gott' der Sonnenpforte verziert und wurden über tausende Kilometer gehandelt."
    ),

    // --- Norte Chico ---

    Question(
        categoryId = 3,
        questionText = "Welches Merkmal der Norte-Chico-Zivilisation (Caral, Peru, ca. 3000–1800 v. Chr.) fehlt im Gegensatz zu allen anderen frühen Hochkulturen weltweit und stellt Archäologen vor ein Rätsel hinsichtlich der Entstehungsbedingungen komplexer Gesellschaften?",
        answerA = "Monumentale Architektur",
        answerB = "Schrift und Keramik",
        answerC = "Bewässerungslandwirtschaft",
        answerD = "Soziale Hierarchie",
        correctAnswer = 1,
        explanation = "Norte Chico ist einzigartig: Eine Zivilisation mit monumentalen Plattformpyramiden, Stadtplanung und tausenden von Menschen – aber ohne Keramik und ohne nachgewiesene Schrift. Stattdessen existierten Quipus als mögliche Informationsspeicher.",
        difficulty = 5,
        funFact = "Caral wurde erst 2001 endgültig als eigenständige Zivilisation anerkannt, als Radiokarbon-Datierungen zeigten, dass die Plattformpyramiden genauso alt wie die ägyptischen Pyramiden sind – eine komplette Neubewertung amerikanischer Vorgeschichte."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Rohmaterial aus dem Pazifik, das in riesigen Mengen auf Norte-Chico-Siedlungsplätzen gefunden wurde, deutet darauf hin, dass die Wirtschaft der Caral-Kultur nicht auf Getreideanbau, sondern auf einem anderen Produkt basierte?",
        answerA = "Getrocknete Anchovis (Sardellen)",
        answerB = "Muschelschalen für Werkzeuge",
        answerC = "Seegurken für den Fernhandel",
        answerD = "Wale für Fett und Knochen",
        correctAnswer = 0,
        explanation = "Getrocknete Anchovis wurden in enormen Mengen in Caral und anderen Norte-Chico-Stätten gefunden. Die Theorie von Ruth Shady und Jonathan Haas lautet: Küstenfischer lieferten Fisch gegen Baumwolle (für Fischernetze) – ein maritim-agrarischer Austausch als Basis der Komplexgesellschaft.",
        difficulty = 5,
        funFact = "Die 'maritim-landwirtschaftliche Hypothese' für Norte Chico ist bahnbrechend: Während alle anderen frühen Hochkulturen auf Getreide (Weizen, Reis, Mais) gründeten, basierte Caral auf Fisch – der einzige bekannte Fall weltweit."
    ),

    // --- Axum ---

    Question(
        categoryId = 3,
        questionText = "Welche aksumitische Münzinschrift unter König Ezana (ca. 320–360 n. Chr.) markiert den Übergang Aksums zum Christentum und gilt als erste staatliche Christentumsbekundung auf einer Münze weltweit?",
        answerA = "Münzen mit dem Kreuz-Symbol anstelle des Mondscheibensymbols",
        answerB = "Die griechische Inschrift 'IHCOYC XPICTOC'",
        answerC = "Münzen mit dem Christogramm Chi-Rho",
        answerD = "Die äthiopische Inschrift 'Nagashi der Nagashis'",
        correctAnswer = 0,
        explanation = "König Ezana ließ auf seinen späteren Münzen das heidnische Mondscheiben-Symbol durch das Kreuz ersetzen – die weltweit erste staatliche Nutzung des Kreuzzeichens auf Münzen, noch vor dem Römischen Reich unter Konstantin.",
        difficulty = 5,
        funFact = "Ezana empfing das Christentum durch den syrischen Missionar Frumentius, der als Sklave nach Aksum kam, am Hof aufstieg und schließlich zum ersten Bischof von Aksum wurde – Athanasius von Alexandria weihte ihn persönlich."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches monumentale Steinobelisken-Ensemble in Aksum, dessen größter Stele über 33 Meter hoch war und heute als gestürzter Koloss liegt, wurde von Mussolini 1937 nach Rom verschleppt und erst 2008 zurückgegeben?",
        answerA = "Obelisk von Matara",
        answerB = "Obelisk von Aksum (Stele 2)",
        answerC = "Obelisk der Königin von Saba",
        answerD = "Yeha-Obelisk",
        correctAnswer = 1,
        explanation = "Die Stele 2 von Aksum (24,6 Meter hoch, 160 Tonnen) wurde 1937 im Auftrag Mussolinis demontiert und nach Rom gebracht, wo sie vor dem UN-Hauptsitz stand. Erst nach jahrzehntelangen Verhandlungen wurde sie 2005 zerlegt und 2008 zurückgegeben und wiederaufgestellt.",
        difficulty = 5,
        funFact = "Die Stele 3 – mit 33 Metern die ursprünglich größte der Welt – stürzte bereits in der Antike um und liegt noch heute als riesiger Steinblock am Boden, sichtlich in mehrere Teile zerbrochen."
    ),

    // --- Great Zimbabwe ---

    Question(
        categoryId = 3,
        questionText = "Welches archäologische Merkmal des Großen Simbabwe (11.–15. Jahrhundert) zeigt am deutlichsten die Handelsverbindungen dieser Bantu-Kultur mit dem Indischen Ozean-Handelsnetz?",
        answerA = "Chinesisches Porzellan der Song- und Ming-Dynastie",
        answerB = "Persische Glasperlen des 14. Jahrhunderts",
        answerC = "Arabische Goldmünzen der Fatimiden",
        answerD = "Indische Baumwolltextilien mit Batik-Muster",
        correctAnswer = 0,
        explanation = "Chinesisches Seladon-Porzellan der Song- und Ming-Dynastien wurde in Großen Simbabwe gefunden – ein direktes Zeugnis für den durch die Swahili-Küstenhäfen (Kilwa, Sofala) vermittelten Indischen-Ozean-Handel.",
        difficulty = 5,
        funFact = "Kilwa Kisiwani (heute Tansania) war die Schlüsselhandelsstadt, die Gold aus Simbabwe an arabische und indische Händler verkaufte. Ibn Battuta besuchte Kilwa 1331 und bezeichnete es als eine der schönsten Städte der Welt."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher Kolonialbeamte und Amateurarchäologe ließ im 19. Jahrhundert in Großem Simbabwe systematisch nach Goldschätzen graben, zerstörte dabei irreparabel Teile des archäologischen Befundes und gab den falschen Behauptungen einer phönizischen oder arabischen Erbauung neue Nahrung?",
        answerA = "Carl Mauch",
        answerB = "James Theodore Bent",
        answerC = "Richard Nicklin Hall",
        answerD = "John Theodore Bent",
        correctAnswer = 2,
        explanation = "Richard Nicklin Hall (Kurator 1902–1904) ließ mit 'Reinigungsarbeiten' große Mengen Kulturschicht entfernen und zerstörte so Stratigraphie-Befunde. Er propagierte die Theorie einer arabischen Erbauung und lehnte afrikanischen Ursprung ab.",
        difficulty = 5,
        funFact = "Die Kolonialregierung Rhodesiens (benannt nach Cecil Rhodes) förderte aktiv Theorien eines nicht-afrikanischen Ursprungs von Großem Simbabwe – die Vorstellung, dass Afrikaner eine solche Zivilisation erbaut haben könnten, passte nicht zur Kolonialideologie."
    ),

    // --- Mali Empire: Mansa Musa's Pilgrimage ---

    Question(
        categoryId = 3,
        questionText = "In welchem Jahr unternahm Mansa Musa seine legendäre Pilgerfahrt nach Mekka, und wie viele Menschen soll er laut dem ägyptischen Chronisten al-Umari in seinem Gefolge gehabt haben?",
        answerA = "1312, ca. 60.000 Menschen",
        answerB = "1324, ca. 60.000 Menschen",
        answerC = "1318, ca. 12.000 Menschen",
        answerD = "1330, ca. 100.000 Menschen",
        correctAnswer = 1,
        explanation = "Mansa Musas Hajj fand 1324/25 (720 AH) statt. Al-Umari schreibt von 60.000 Personen im Gefolge, darunter 12.000 Sklaven in Seide gekleidet und 500 Diener mit goldenen Stäben. Die historische Glaubwürdigkeit dieser Zahlen ist umstritten, aber der Reichtum war real.",
        difficulty = 5,
        funFact = "Mansa Musa verteilte in Kairo so viel Gold, dass der Goldpreis im gesamten Mittelmeerraum für 10–12 Jahre einbrach und eine Inflation verursachte – ein Fall von 'Hyperinflation durch humanitäre Freigiebigkeit' ohne Parallele in der Geschichte."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher arabische Diplomat und Gelehrte, der Mansa Musa in Kairo traf und dessen Beschreibung des Mali-Reichs in seiner 'Masalik al-Absar' (1342) die detaillierteste Quelle zur Wirtschaft des Mali-Reichs ist, war der Hauptzeuge für den wirtschaftlichen Einfluss Musas auf Ägypten?",
        answerA = "Ibn Battuta",
        answerB = "Al-Qalqashandi",
        answerC = "Ibn Khaldun",
        answerD = "Al-Umari",
        correctAnswer = 3,
        explanation = "Shihab al-Din al-Umari verfasste in seinem Werk 'Masalik al-Absar fi Mamalik al-Amsar' (1342) die ausführlichste Beschreibung des Mali-Reichs, basierend auf Berichten von Augenzeugen, die Mansa Musa in Kairo getroffen hatten.",
        difficulty = 5,
        funFact = "Ibn Battuta besuchte Mali erst 1352–1353 – nach Mansa Musas Tod (ca. 1337). Er traf Musa Keitas Nachfolger Mansa Suleyman und beschrieb den Mali-Hof mit Bewunderung für seine Ordnung, aber auch mit Kritik an bestimmten Sitten."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher Architekt, den Mansa Musa auf seiner Pilgerfahrt aus Granada mitbrachte und der die Große Moschee von Djenné und die Moschee von Sankore in Timbuktu (neu-)entwarf, gilt als Begründer des sudano-sahelianischen Architekturstils?",
        answerA = "Ibrahim al-Sahili (Abu Ishaq al-Sahili)",
        answerB = "Ahmad Baba al-Timbukti",
        answerC = "Muhammad Kati",
        answerD = "Mahmud Kati",
        correctAnswer = 0,
        explanation = "Abu Ishaq Ibrahim al-Sahili (ca. 1290–1346), ein andalusischer Poet und Architekt aus Granada, begleitete Mansa Musa nach Mali. Er entwarf Moscheen und Paläste und erhielt dafür nach zeitgenössischen Berichten 200 kg Gold – eine der größten Architektenhonorare der Geschichte.",
        difficulty = 5,
        funFact = "Al-Sahili soll laut al-Umari so reich beschenkt worden sein, dass er nie wieder nach Granada zurückkehren musste. Der sudano-sahelianische Baustil – Lehmarchitektur mit herausragenden Holzbalken (Toron) – findet sich heute noch in der Großen Moschee von Djenné."
    ),

    // --- Advanced Minoan Details ---

    Question(
        categoryId = 3,
        questionText = "Welches minoische Palastarchiv auf Kreta, bei dem 1900 von Arthur Evans ca. 3.000 Linear-A- und Linear-B-Tafeln gefunden wurden, ist der größte einzelne Fundort prähistorischer Tontafeln im ägäischen Raum?",
        answerA = "Palast von Zakros",
        answerB = "Palast von Phaistos",
        answerC = "Palast von Knossos",
        answerD = "Palast von Mallia",
        correctAnswer = 2,
        explanation = "Im Palast von Knossos fand Arthur Evans die umfangreichste Sammlung linear beschrifteter Tontafeln der Ägäis. Die meisten waren Linear B (mykenisch-griechisch) und wurden von Michael Ventris 1952 entziffert; Linear A-Tafeln blieben unverstanden.",
        difficulty = 5,
        funFact = "Arthur Evans rekonstruierte den Palast von Knossos aufwendig – und kontrovers: Viele Archäologen kritisierten seine farbenfrohen Betonrekonstruktionen als zu spekulativ. Man weiß bis heute nicht, ob die minoischen Fresken wirklich so aussahen."
    ),

    Question(
        categoryId = 3,
        questionText = "Wie bezeichneten Historiker das politische System der Minoischen Zivilisation, in dem ein einziger Palast wie Knossos ein ausgedehntes wirtschaftliches Umland durch Redistribution kontrollierte?",
        answerA = "Thalassokratie",
        answerB = "Palatiale Wirtschaft (Palace Economy)",
        answerC = "Tributstaat",
        answerD = "Theokratie",
        correctAnswer = 1,
        explanation = "Das Konzept der 'Palatialen Wirtschaft' beschreibt, wie der Palast von Knossos Güter aus dem Umland einsammelte (Wolle, Öl, Getreide) und an Handwerker und Beamte weiterverteilte – ein redistributives System dokumentiert in den Linear-B-Archiven.",
        difficulty = 5,
        funFact = "Die Theorie der minoischen Thalassokratie (Seeherrschaft) – dass Kreta durch eine Flotte alle ägäischen Inseln beherrschte – geht auf Thukydides zurück, ist aber archäologisch kaum belegt; man fand keine minoischen Waffen auf den Inseln."
    ),

    // --- More Phoenician Details ---

    Question(
        categoryId = 3,
        questionText = "Welche phonizische Stadt, die 332 v. Chr. nach siebenmonatiger Belagerung von Alexander dem Großen mithilfe eines künstlich aufgeschütteten Dammes zur Halbinsel umgewandelt wurde, ist heute eine Halbinsel geblieben?",
        answerA = "Sidon",
        answerB = "Byblos",
        answerC = "Tyros (Sur)",
        answerD = "Arados (Arwad)",
        correctAnswer = 2,
        explanation = "Tyros (heute Sur im Libanon) lag auf einer vorgelagerten Insel. Alexander ließ einen Damm (Mole) von der Küste zur Insel aufschütten. Durch Sedimentablagerungen wurde diese Mole zur dauerhaften Landbrücke – Tyros ist bis heute eine Halbinsel.",
        difficulty = 5,
        funFact = "Beim Belagerungsbau plünderte Alexander die Trümmer des alten Festland-Tyros für Material. Diodor berichtet, dass die Tyrier Widdertöter mit glühendem Sand bewarfen, um sie in ihren Rüstungen zu rösten – eine der brutalsten antiken Belagerungswaffen."
    ),

    // --- Sassanid Advanced ---

    Question(
        categoryId = 3,
        questionText = "Welche sassanidische Hauptstadt, erbaut als Winterresidenz am Tigris, bestand aus mehreren aneinander gereihten runden Städten und deren Ruinen heute als 'Ctesiphon' bekannt sind, mit dem berühmten Taq Kasra-Iwan?",
        answerA = "Seleukeia am Tigris",
        answerB = "Ktesiphon (Madain)",
        answerC = "Veh-Ardashir",
        answerD = "Gundeshapur",
        correctAnswer = 1,
        explanation = "Ktesiphon (arabisch: Madain = 'die Städte') war die sassanidische Hauptstadt am Tigris. Der Taq Kasra (Iwan von Khosrow) mit seinem 37 Meter hohen Tonnengewölbe ist der größte nicht-armierte Backsteinbogen der Welt und steht noch heute.",
        difficulty = 5,
        funFact = "Ktesiphon war zur Hochzeit mit geschätzt 500.000 Einwohnern möglicherweise die größte Stadt der Welt – größer als das damalige Konstantinopel. Nach der arabischen Eroberung 637 n. Chr. wurde die Hauptstadt nach Kufa verlegt und Ktesiphon verödete."
    ),

    // --- Cahokia Advanced ---

    Question(
        categoryId = 3,
        questionText = "Welches hölzerne astronomische Monument in Cahokia, bestehend aus konzentrischen Pfahlkreisen, das zur Bestimmung von Sonnenwenden und Tagundnachtgleichen diente, wurde von Archäologen als 'amerikanisches Stonehenge' bezeichnet?",
        answerA = "Cahokia Woodhenge",
        answerB = "Hopewell Circle",
        answerC = "Medicine Wheel",
        answerD = "Newark Octagon",
        correctAnswer = 0,
        explanation = "Der Cahokia Woodhenge bestand aus mindestens fünf konzentrischen Kreisen großer Zedernpfähle (ca. 900–1100 n. Chr.). Vom Zentrum aus markierten bestimmte Pfähle den Sonnenaufgang zur Frühlings- und Herbst-Tagundnachtgleiche sowie zu den Sonnenwenden.",
        difficulty = 5,
        funFact = "Cahokia erlebte um 1050 n. Chr. eine plötzliche 'Big Bang'-Gründungsphase: In nur einer Generation wuchs die Siedlung von einem Dorf zu einer Stadt von 10.000–20.000 Menschen – eine der schnellsten Urbanisierungen in der prähistorischen Welt."
    ),

    // --- Tiwanaku Advanced ---

    Question(
        categoryId = 3,
        questionText = "Welches Tiwanaku-Bauwerk am Ufer des Titicaca-Sees, bekannt als 'Pumapunku', enthält steinerne Türrahmen und H-förmige Andesit-Blöcke mit maschinenpräzisen Passungen, die bis heute keine befriedigende Erklärung ihrer Herstellungsmethode haben?",
        answerA = "Akapana-Pyramide",
        answerB = "Kalasasaya-Tempel",
        answerC = "Pumapunku",
        answerD = "Semi-Subterraner Tempel",
        correctAnswer = 2,
        explanation = "Pumapunku (ca. 500–900 n. Chr.) enthält H-förmige Andesit- und Roter-Sandstein-Blöcke, die mit Präzisionsnuten von unter einem Millimeter Toleranz gefertigt wurden. Die Steinblöcke wurden über 90 km transportiert; ihre Bearbeitungsweise ist noch nicht vollständig erklärt.",
        difficulty = 5,
        funFact = "Pumapunku liegt auf 3.840 Metern Höhe – Tiwanaku ist damit eine der höchstgelegenen frühen Zivilisationen der Welt. Die Bewohner adaptierten ihren Metabolismus über Generationen an den Sauerstoffmangel der Höhe."
    ),

    // --- Ancient Phoenician Trade ---

    Question(
        categoryId = 3,
        questionText = "Welcher phönizische Farbstoff, gewonnen aus der Drüse der Meeresschnecke Murex brandaris, war so teuer, dass er zum Symbol kaiserlicher Macht wurde und 'Purpur' bis heute mit Reichtum assoziiert wird?",
        answerA = "Tyrischer Purpur (Dibromoindigo)",
        answerB = "Kermes-Scharlach",
        answerC = "Lapislazuli-Blau",
        answerD = "Waid-Indigo",
        correctAnswer = 0,
        explanation = "Tyrischer Purpur (6,6'-Dibromoindigo) wurde aus Murex-Schnecken gewonnen: Für ein Gramm Farbstoff benötigte man ca. 9.000 Schnecken. Sidon und Tyros waren die Hauptproduktionszentren; Purpurkleidung kostete mehr als Gold.",
        difficulty = 5,
        funFact = "In Sidon und Tyros wurden riesige Abfallhalden aus Murex-Schalen gefunden, die hunderte Meter lang sind – Zeugnis von jahrtausendealter Purpurproduktion. Der Geruch der verrottenden Schnecken muss unerträglich gewesen sein."
    ),

    // --- Axum Advanced ---

    Question(
        categoryId = 3,
        questionText = "Welches aksumitische Münzsystem ist weltweit einzigartig darin, dass es Goldmünzen (Solidus-äquivalente), Silbermünzen und Kupfermünzen gleichzeitig prägte und damit ein dreimetallisches Geldsystem hatte, das selbst das Römische Reich erst später erreichte?",
        answerA = "Das Währungssystem unter König Endubis (ca. 270 n. Chr.)",
        answerB = "Das Münzsystem unter König Wazeba",
        answerC = "Das Reformsystem unter König Kaleb",
        answerD = "Das aksumitische Bimetallsystem der Spätphase",
        correctAnswer = 0,
        explanation = "König Endubis (ca. 270 n. Chr.) war der erste aksumitische König, der Münzen prägen ließ – und er führte sofort Gold, Silber und Bronze ein. Das aksumitische Münzsystem war für seine Zeit und Region außergewöhnlich sophisticated.",
        difficulty = 5,
        funFact = "Aksumitische Goldmünzen wurden bis nach Indien gehandelt – archäologische Funde in Gujarat und Kerala belegen direkten Seehandel zwischen Aksum und dem indischen Subkontinent über das Rote Meer."
    ),

    // --- Sea Peoples Advanced ---

    Question(
        categoryId = 3,
        questionText = "Welche zypriotische Stadt, die um 1200 v. Chr. zerstört und danach von einer neuen Bevölkerung mit mykenischer Keramik besiedelt wurde, gilt als Schlüsselbeleg für die Theorie, dass einige Seevölker mykenische Flüchtlinge waren?",
        answerA = "Enkomi",
        answerB = "Kition",
        answerC = "Paphos",
        answerD = "Alassa",
        correctAnswer = 0,
        explanation = "Enkomi auf Zypern zeigt archäologisch eine Schicht mit Zerstörung um 1200 v. Chr., gefolgt von einer Neubesiedelung mit mykenischen (griechischen) Kulturmerkmalen. Dies stützt die Hypothese, dass Seevölker zumindest teilweise aus dem zusammenbrechenden mykenischen Griechenland stammten.",
        difficulty = 5,
        funFact = "Der 'Kollaps der Bronzezeit' um 1200 v. Chr. gilt als eine der größten Zivilisationskatastrophen der Vorgeschichte: Innerhalb von 50 Jahren verschwanden mykenisches Griechenland, das Hethiterreich, Ugarit und zahlreiche andere Kulturen – die Ursachen sind bis heute umstritten."
    ),

    // --- Carthaginian Religion ---

    Question(
        categoryId = 3,
        questionText = "Welcher karthagische Kultbezirk, in dem urnenförmige Behälter mit verbrannten Überresten von Kindern und Tieren gefunden wurden und der als Beweis für das Tophet-Kinderopfer gilt, obwohl die Interpretation bis heute umstritten ist?",
        answerA = "Tophet von Karthago (Salammbô-Bezirk)",
        answerB = "Heiligtum der Tanit in Dougga",
        answerC = "Opferbezirk von Hadrumetum",
        answerD = "Melqart-Heiligtum auf Sizilien",
        correctAnswer = 0,
        explanation = "Der Tophet im Salammbô-Viertel Karthagos enthält über 20.000 Urnen mit verbrannten Überresten. Die Debatte: Waren es Erstgeburtsopfer (wie antike Quellen behaupten) oder Bestattungen natürlich verstorbener Kleinkinder in einem Sonderbezirk?",
        difficulty = 5,
        funFact = "Neuere isotopen-analytische Studien (2014, Oxford) stützten die Opfer-These: Verbrannte Knochen zeigen, dass Kinder lebend verbrannt wurden. Gegner verweisen auf methodische Probleme – die Debatte bleibt eine der hitzigsten der Altertumswissenschaften."
    ),

    // --- Mali Empire Advanced ---

    Question(
        categoryId = 3,
        questionText = "Welche arabische Quelle aus dem 14. Jahrhundert beschreibt, dass Mansa Musa nach seiner Rückkehr aus Mekka begann, alle Provinzgouverneure durch Mitglieder seiner Familie zu ersetzen und den Titel 'Mansa' (Herrscher aller Herrscher) systematisch zu zentralisieren?",
        answerA = "Ibn Khaldun in 'Kitab al-Ibar'",
        answerB = "Ibn Battuta in 'Rihla'",
        answerC = "Al-Maqrizi in 'Al-Mawa'iz'",
        answerD = "Al-Qalqashandi in 'Subh al-Asha'",
        correctAnswer = 0,
        explanation = "Ibn Khaldun beschreibt in seiner Weltgeschichte ('Kitab al-Ibar') detailliert die Verwaltungsreformen der malischen Könige. Seine Informationen stammten von Augenzeugen und malischen Gesandten, die er in Kairo und Tunis traf.",
        difficulty = 5,
        funFact = "Ibn Khaldun entwickelte in seiner 'Muqaddima' das Konzept der 'Asabiyya' (Gruppensolidarität) als Triebkraft des historischen Wandels – er ist damit einer der ersten Sozialwissenschaftler der Geschichte, über 300 Jahre vor Comte und Weber."
    ),

    // --- Etruscan Religion and Society ---

    Question(
        categoryId = 3,
        questionText = "Welche etruskische Disziplin der Schaueingeweide-Lektüre, bei der die Oberfläche einer Tierleber in Regionen unterteilt und als Karte des Himmels interpretiert wurde, ist durch ein bronzenes Lebermodell aus Piacenza (ca. 100 v. Chr.) überliefert?",
        answerA = "Haruspizin (Eingeweideschau)",
        answerB = "Augurein (Vogelschau)",
        answerC = "Fulguralkunst (Blitzinterpretation)",
        answerD = "Extispizin im engeren Sinne",
        correctAnswer = 0,
        explanation = "Die Haruspizin war eine etruskische Spezialkunst der Leberschau: Ein bronzenes Lebermodell aus Piacenza zeigt 40 beschriftete Regionen, die jeweils einem Gott zugeordnet sind. Haruspices (Eingeweideschauer) wurden von Rom als religöse Experten eingeladen.",
        difficulty = 5,
        funFact = "Die Leber galt in der Antike als Sitz der Seele und des Blutes – nicht das Herz. Ein identisches Leberorakel-System findet sich im alten Mesopotamien (Nippur-Lebermodell), was auf kulturellen Austausch zwischen Etrurien und Mesopotamien hindeutet."
    ),

    // --- Mycenaean Trade ---

    Question(
        categoryId = 3,
        questionText = "Welches mykenische Handelsgut, das in Ägypten, der Levante und im westlichen Mittelmeer gefunden wurde und in Linear-B-Texten als 'ka-na-ko' verbucht ist, dokumentiert den panmediterranen Handelsradius mykenischer Kaufleute?",
        answerA = "Krokus-Safran",
        answerB = "Zypressenöl",
        answerC = "Terrakotta-Kriegerfiguren",
        answerD = "Bernstein aus dem Baltikum",
        correctAnswer = 0,
        explanation = "Krokus/Safran ('ka-na-ko' in Linear B) war ein wertvolles mykenisches Handelsprodukt. Safran-Fresken in Akrotiri (Santorini) und Knossos zeigen Frauen beim Safraner-nten; ägyptische Quellen belegen Import aus der Ägäis.",
        difficulty = 5,
        funFact = "Das Uluburun-Schiffswrack (ca. 1300 v. Chr., gefunden vor der türkischen Küste 1982) trägt mykenische Objekte, ägyptische Goldbarren, kanaanäische Amphoren, zypriotisches Kupfer und baltischen Bernstein – ein Querschnitt der spätbronzezeitlichen Weltwirtschaft."
    ),

    // --- Norte Chico Advanced ---

    Question(
        categoryId = 3,
        questionText = "Welches spezifische textile Artefakt, gefunden in Norte-Chico-Stätten und aus Baumwolle gefertigt, gilt neben dem Quipu als mögliches frühes Informations-/Zählsystem, das Knoten und Farben als Code verwendet?",
        answerA = "Tocapu-Tunika",
        answerB = "Schlingennetze mit Knotenmarkierungen",
        answerC = "Frühes Quipu (Khipu)",
        answerD = "Wari-Textilkalender",
        correctAnswer = 2,
        explanation = "Fragmente früher Quipus (Khipu) wurden in Norte-Chico-Kontexten gefunden, was die Nutzung dieses Knotenschriftsystems um ca. 2500 v. Chr. belegt – weit früher als bisher angenommen und ohne Keramik als Begleitkultur.",
        difficulty = 5,
        funFact = "Das Quipu der Inkas hatte bis zu 2.000 Schnüre mit komplexen Knotenfolgen. Einige Forscher glauben, dass Quipus nicht nur numerisch, sondern auch narrativ (als Sprache) codiert waren – ein 'taktiles Schriftsystem' das noch nicht vollständig entziffert wurde."
    ),

    // --- Sassanid Religion in Detail ---

    Question(
        categoryId = 3,
        questionText = "Welcher Sassaniden-König ließ den manikäischen Propheten Mani hinrichten (276 n. Chr.) und begründete damit eine Verfolgungspolitik gegen nicht-zoroastrische Religionen, die das Sassanidenreich von seiner toleranten früheren Politik abkehrte?",
        answerA = "Shapur I.",
        answerB = "Vahram (Bahram) I.",
        answerC = "Narseh",
        answerD = "Hormizd II.",
        correctAnswer = 1,
        explanation = "Bahram I. (273–276 n. Chr.) ließ Mani, den Begründer des Manichäismus, auf Betreiben des Zoroastrischen Hohepriesters Kartir inhaftieren und hinrichten. Kartir hinterließ eigene Felsreliefs, auf denen er seine Rolle bei der Verfolgung von Christen, Juden, Manichäern und Buddhisten rühmte.",
        difficulty = 5,
        funFact = "Mani nannte sich selbst 'Siegel der Propheten' und behauptete, die Schriften Buddhas, Zoroasters und Jesu zu vervollständigen – eine Synthese, die ihn in Babylonien, Persien, Ägypten und bis nach China Anhänger finden ließ."
    ),

    // --- Carthaginian Military ---

    Question(
        categoryId = 3,
        questionText = "Welche karthagische Militäreinheit, bestehend aus iberischen, ligurischen, kampanischen und numidischen Söldnern, meuterte nach dem Ersten Punischen Krieg und führte den sogenannten 'Söldnerkrieg' (240–237 v. Chr.), der in Flauberts Roman 'Salammbô' verewigt wurde?",
        answerA = "Die Heilige Schar (Hieros Lochos)",
        answerB = "Die unregulierten Söldnertruppen nach dem Frieden von Lutatius",
        answerC = "Die Libyophönizische Infanterie",
        answerD = "Das karthagische Bürgeraufgebot",
        correctAnswer = 1,
        explanation = "Nach dem Ersten Punischen Krieg (241 v. Chr.) weigerte sich Karthago, seine Söldner vollständig auszuzahlen. Diese meuterten unter Führung von Matho und Spendius. Der Söldnerkrieg (240–237 v. Chr.) bedrohe Karthago existentiell und wurde von Hamilkar Barkas niedergeschlagen.",
        difficulty = 5,
        funFact = "Flaubert verbrachte fünf Jahre mit Recherchen für 'Salammbô' (1862), besuchte Tunesien und studierte antike Quellen – aber Polybios, unsere beste Quelle, war für ihn so grausam detailliert, dass er ihn kaum verarbeitete. Der Roman wurde ein Skandalerfolg."
    ),

    // --- Great Zimbabwe Economy ---

    Question(
        categoryId = 3,
        questionText = "Welches Metall, das in Schmelztiegeln und als Rohlinge in Großem Simbabwe gefunden wurde und über den Hafen Sofala exportiert wurde, bildete die wirtschaftliche Grundlage der simbabwischen Eliten und ist etymologischer Ursprung des Landesnamens 'Zimbabwe'?",
        answerA = "Kupfer",
        answerB = "Eisen",
        answerC = "Gold",
        answerD = "Zinn",
        correctAnswer = 2,
        explanation = "Gold war das zentrale Exportgut des simbabwischen Plateaus. Über den Swahili-Hafen Sofala (heute Mosambik) floss es in den Indischen-Ozean-Handel. 'Zimbabwe' leitet sich vom Shona 'dzimba dza mabwe' (Häuser aus Stein) ab, bezieht sich aber auch auf den Status der Goldeliten.",
        difficulty = 5,
        funFact = "Allein im 11.–15. Jahrhundert wurden schätzungsweise 20 Millionen Unzen Gold aus dem simbabwischen Plateau exportiert – ein Handelsvolumen, das die Grundlage für den Reichtum der Swahili-Stadtstaten bildete."
    ),

    // --- Minoan Religion ---

    Question(
        categoryId = 3,
        questionText = "Welches minoische Kultgefäß, das bei Zeremonien zur Libation verwendet wurde und in Tierform (meist als Stier oder Löwe mit Goldeinlagen) ausgeführt ist, wird mit dem Begriff 'Rhyton' bezeichnet und fand sich besonders häufig in Knossos?",
        answerA = "Kernos",
        answerB = "Rhyton",
        answerC = "Pithoi",
        answerD = "Kalathos",
        correctAnswer = 1,
        explanation = "Das Rhyton (griechisch: 'das Fließende') ist ein Trink- oder Ausgussgefäß in Tier- oder Hornform, das bei minoischen Kulthandlungen verwendet wurde. Das bekannteste Beispiel ist der Stierkopf-Rhyton aus Knossos aus schwarzem Steatit mit goldenen Hörnern.",
        difficulty = 5,
        funFact = "Minoische Stierspringen-Fresken zeigen Akrobaten beiderlei Geschlechts, die über einen Stier springen – ob dies ein religiöses Ritual, ein Wettkampf oder ein reines Fresko-Motiv war, ist bis heute ungeklärt."
    ),

    // --- Carthage vs. Rome Final Details ---

    Question(
        categoryId = 3,
        questionText = "Welcher karthagische General, Sohn des Hamilkar Barkas, überquerte 218 v. Chr. die Alpen mit Kriegselefanten und besiegte die Römer in drei vernichtenden Schlachten (Trebia, Trasimeno, Cannae), gilt aber als strategisch letztendlich gescheitert, weil er Rom nie direkt angriff?",
        answerA = "Hamilkar Barkas",
        answerB = "Hasdrubal Barkas",
        answerC = "Hannibal Barkas",
        answerD = "Mago Barkas",
        correctAnswer = 2,
        explanation = "Hannibal Barkas (247–183 v. Chr.) ist einer der genialsten Feldherren der Antike. Seine Strategie der Vernichtungsschlacht (Cannae, 216 v. Chr.: ca. 70.000 Römer in einer Umfassungsschlacht getötet) gilt als Muster militärischer Taktik bis heute.",
        difficulty = 5,
        funFact = "Die moderne Militärtheorie kennt das 'Cannae-Prinzip' der Umfassungsschlacht – Schlieffen plante es für WWI, Schwartzkopf nutzte es im Golfkrieg. Hannibal erfand es nicht, aber perfektionierte es für die Ewigkeit."
    ),

    // --- Mycenaean Collapse Details ---

    Question(
        categoryId = 3,
        questionText = "Welcher Linear-B-Text aus Pylos, der kurz vor der Zerstörung des Palastes (ca. 1180 v. Chr.) verfasst wurde, beschreibt in dringlichem Ton die Vorbereitung der Küstenverteidigung mit Rudersklaven und Wachposten und gilt als 'letzter Hilferuf' einer untergehenden Zivilisation?",
        answerA = "Die An-Tafeln (Truppenbewegungen)",
        answerB = "Die On-Tafeln (Ruderkommandos)",
        answerC = "Die Fn-Tafeln (Ölrationen)",
        answerD = "Die Cn-Tafeln (Viehzählung)",
        correctAnswer = 1,
        explanation = "Die On-Tafeln aus Pylos beschreiben die Aufstellung von Ruderern an verschiedenen Küstenpunkten zur Überwachung – verfasst wenige Monate oder Wochen vor dem Brand des Palastes (ca. 1180 v. Chr.). Sie gelten als Zeugnis einer erwarteten Bedrohung von See.",
        difficulty = 5,
        funFact = "Der Palastbrand von Pylos war so heiß, dass er die Linear-B-Tontafeln unbeabsichtigt brannte und konservierte – ohne den Brand wären sie wie alle anderen ungebrannten Tontafeln der Bronzezeit vergangen."
    ),

    // --- African Kingdoms: Swahili Coast Context ---

    Question(
        categoryId = 3,
        questionText = "Welche Swahili-Stadtinsel, deren Sultane im 13.–15. Jahrhundert das Goldmonopol der simbabwischen Küste kontrollierten und die Ibn Battuta als reichste Stadt Ostafrikas beschrieb, wurde 1505 von den Portugiesen zerstört?",
        answerA = "Mombasa",
        answerB = "Malindi",
        answerC = "Kilwa Kisiwani",
        answerD = "Zanzibar",
        correctAnswer = 2,
        explanation = "Kilwa Kisiwani (heutiges Tansania) war das Schlüsselzentrum des ostafrikanischen Goldhandels. Der Husuni Kubwa-Palast (14. Jahrhundert) ist der größte mittelalterliche Steinbau südlich der Sahara. Portugiese plünderten Kilwa 1505 unter Francisco de Almeida.",
        difficulty = 5,
        funFact = "In Kilwa wurden chinesische Münzen der Song- und Ming-Dynastien gefunden – direkte archäologische Belege für den transozeanen Handel. Chinesische Quellen erwähnen Gesandtschaften aus 'Zengdan' (Ostafrika) am Hof von Yongle (1405–1433)."
    ),

    // --- Pre-Columbian: Tiwanaku Religion ---

    Question(
        categoryId = 3,
        questionText = "Welche Gottheit, dargestellt auf dem Sonnentormonolithen von Tiwanaku als weinende Figur mit Strahlen-Kopfschmuck und Kondor-Anhängern, war die Hauptgottheit des Tiwanaku-Pantheons und wurde auch von den späteren Inkas als Viracocha adaptiert?",
        answerA = "Inti (Sonnengott)",
        answerB = "Der Stabgott (Dios de los Báculo / Gateway God)",
        answerC = "Pachamama (Erdmutter)",
        answerD = "Illapa (Blitzgott)",
        correctAnswer = 1,
        explanation = "Der 'Stabgott' oder 'Gateway God' auf dem Sonnentor von Tiwanaku (ca. 700 n. Chr.) hält zwei gekreuzte Stäbe, trägt einen Strahlenkopf mit Kondor-Gesichtern und ist von 48 geflügelten Dienerfiguren umgeben. Er wird mit dem inkaschen Viracocha in Verbindung gebracht.",
        difficulty = 5,
        funFact = "Das Sonnentor von Tiwanaku ist aus einem einzigen Andesit-Block geschnitten und wiegt ca. 10 Tonnen. Es stand ursprünglich nicht am heutigen Standort – seine ursprüngliche Position im Kalasasaya-Tempel ist noch nicht vollständig geklärt."
    ),

    // --- Etruscan Language ---

    Question(
        categoryId = 3,
        questionText = "Welches etruskische Leinengewand, das als Mumienwicklung einer ägyptischen Mumie in Zagreb verwendet wurde und im 19. Jahrhundert entdeckt wurde, ist das längste erhaltene etruskische Textdokument mit über 1.200 Wörtern?",
        answerA = "Die Zagabria-Liber-Linteus",
        answerB = "Der Pyrgi-Texttempel",
        answerC = "Die Perugia-Grabinschrift",
        answerD = "Der Capua-Ziegelstein",
        correctAnswer = 0,
        explanation = "Der Liber Linteus Zagrabiensis (Zagreber Leinenbuch) ist ein etruskisches Ritualkalender-Dokument aus ca. 250 v. Chr., das als Mumienwicklung nach Ägypten kam. Es enthält religiöse Anweisungen für Opferrituale, aber der Großteil des Textes ist wegen des unzureichenden Wortschatzes noch unverstanden.",
        difficulty = 5,
        funFact = "Die Mumie wurde 1848 von einem kroatischen Beamten in Ägypten gekauft, ohne zu wissen, dass die Wicklung beschrieben war. Jahrzehntelang lag das Leinenbuch im Museum Zagreb, bis 1891 ein Gelehrter die etruskische Schrift erkannte."
    ),

)
