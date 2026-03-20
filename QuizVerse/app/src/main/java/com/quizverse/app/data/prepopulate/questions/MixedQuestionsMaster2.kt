package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun mixedQuestionsMaster2(): List<Question> = listOf(

    // --- Kuriose Kriege ---

    Question(
        categoryId = 11,
        questionText = "Der Britisch-Sansibarische Krieg von 1896 gilt als kürzester Krieg der Geschichte. Wie lange dauerte er exakt laut Guinness-Buch der Rekorde?",
        answerA = "23 Minuten",
        answerB = "38 Minuten",
        answerC = "1 Stunde 12 Minuten",
        answerD = "2 Stunden 45 Minuten",
        correctAnswer = 1,
        explanation = "Der Britisch-Sansibarische Krieg dauerte am 27. August 1896 zwischen 09:02 und 09:40 Uhr — genau 38 Minuten. Eine britische Flotte beschoss den Palast von Sultan Khalid bin Barghash, nachdem dieser sich geweigert hatte, abzudanken. Das Guinness-Buch der Rekorde führt ihn als kürzesten Krieg der Weltgeschichte.",
        difficulty = 5,
        funFact = "Der unterlegene Sultan floh während der Beschießung in die deutsche Botschaft und blieb dort jahrelang im Exil. Die britische Seite verlor dabei keinen einzigen Soldaten — der einzige verwundete Brite hatte sich selbst versehentlich verletzt."
    ),

    Question(
        categoryId = 11,
        questionText = "Das Dorf Lijar in Spanien erklärte 1883 Frankreich den Krieg — wegen einer angeblichen Beleidigung des spanischen Königs. Wann endete dieser 'Krieg' offiziell?",
        answerA = "1918 nach dem Ende des Ersten Weltkrieges",
        answerB = "1945 nach dem Ende des Zweiten Weltkrieges",
        answerC = "1983, nach genau 100 Jahren",
        answerD = "Er dauert bis heute an",
        correctAnswer = 2,
        explanation = "Die Gemeinde Lijar (ca. 300 Einwohner) erklärte Frankreich 1883 den Krieg, nachdem Berichte kursierten, französische Bürger hätten König Alfonso XII. beleidigt. In den folgenden 100 Jahren fiel kein einziger Schuss. 1983 erklärte der Bürgermeister offiziell den Frieden — Frankreich hatte den 'Krieg' nie bemerkt.",
        difficulty = 5,
        funFact = "Als der Bürgermeister von Lijar 1983 den Frieden verkündete, sagte er: 'Wir haben alle Ziele erreicht — die Ehre Spaniens ist wiederhergestellt.' Die französische Regierung reagierte auf die Friedenserklärung mit vollkommener Verwirrung."
    ),

    Question(
        categoryId = 11,
        questionText = "Die Niederlande erklärten 1651 den Scilly-Inseln (Teil des heutigen Vereinigten Königreichs) den Krieg — und vergaßen, den Frieden zu schließen. Wie lange dauerte dieser 'Krieg' offiziell?",
        answerA = "87 Jahre",
        answerB = "177 Jahre",
        answerC = "335 Jahre",
        answerD = "271 Jahre",
        correctAnswer = 2,
        explanation = "Die niederländische Kriegserklärung von 1651 gegen die Scilly-Inseln blieb 335 Jahre lang in Kraft, weil schlicht vergessen wurde, einen Friedensvertrag zu schließen. Ein niederländischer Historiker entdeckte den technischen Kriegszustand erst 1985. Der Friedensvertrag wurde daraufhin 1986 feierlich unterzeichnet — ohne einen einzigen Schuss, der je gefallen wäre.",
        difficulty = 5,
        funFact = "Der niederländische Botschafter Jonkheer Rein Huydecoper reiste eigens nach St. Mary's auf den Scilly-Inseln, um 1986 den Frieden zu unterzeichnen. Der Bürgermeister empfing ihn mit den Worten: 'Wir haben keine Angst gehabt, aber es ist trotzdem schön, dass jetzt Frieden ist.'"
    ),

    // --- Der Emu-Krieg ---

    Question(
        categoryId = 11,
        questionText = "Der sogenannte 'Emu-Krieg' fand 1932 in Australien statt. Was war sein Ergebnis?",
        answerA = "Australische Soldaten mit Maschinengewehren vernichteten die Emu-Population vollständig",
        answerB = "Die Emus gewannen — das Militär zog sich nach wenigen Wochen erfolglos zurück",
        answerC = "Ein Kompromiss: Ein Drittel der Emus wurde vertrieben, ein Drittel erschossen",
        answerD = "Die Operation wurde durch Tierschützer nach einem Tag gestoppt",
        correctAnswer = 1,
        explanation = "Major G.P.W. Meredith führte 1932 australische Soldaten mit Maschinengewehren gegen ca. 20.000 Emus in Western Australia an. Die Emus zerstreuten sich in kleine Gruppen und waren damit fast unbesiegbar. Nach ca. 2.500 verschossenen Patronen für geschätzte 200 tote Emus zog das Militär ab. Ein Ornithologe kommentierte: 'Die Emus haben die Scharmützel gewonnen.'",
        difficulty = 5,
        funFact = "Major Meredith sagte über die Emus: 'Wäre ihre Munition unendlich, hätten wir keine Chance gehabt.' Die australische Presse spottete über die 'Niederlage', und im Parlament wurde ein parlamentarisches Untersuchungsverfahren eingeleitet. Australien führte danach Zäune statt Schusswaffen ein."
    ),

    // --- Bizarre historische Ereignisse ---

    Question(
        categoryId = 11,
        questionText = "Was ereignete sich am 15. Januar 1919 in der Fleet Street in Boston — bekannt als 'Great Molasses Flood'?",
        answerA = "Ein Fabrikbrand setzte 8 Millionen Liter Zuckersirup frei und verursachte eine giftige Gaswolke",
        answerB = "Ein 27 Meter großer Melassetank explodierte und überschwemmte Straßen mit 9,5 Millionen Litern Melasse",
        answerC = "Ein Eisenbahnunglück verschüttete Zuckerladungen, die die gesamte Kanalisation verstopften",
        answerD = "Ein Hafenlager kollabierte und schüttete Rohrzucker in den Charles River",
        correctAnswer = 1,
        explanation = "Am 15. Januar 1919 explodierte ein 27 Meter breiter Stahltank der United Industrial Alcohol Company und ergoss 8,7 Millionen Liter (2,3 Millionen Gallonen) Melasse in die Straßen Bostons. Die Welle war bis zu 4,5 Meter hoch und bewegte sich mit 56 km/h. 21 Menschen und mehrere Pferde ertranken in der Masse; 150 wurden verletzt.",
        difficulty = 5,
        funFact = "Anwohner Bostons behaupteten jahrzehntelang, an heißen Sommertagen rieche es in der North End-Gegend noch nach Melasse. Wissenschaftler verwarfen dies, aber lokale Folklore hält an der Geschichte fest. Der Unfall führte zur ersten großen Industriesicherheitsgesetzgebung in den USA."
    ),

    Question(
        categoryId = 11,
        questionText = "Der 'Tanzwahn von Straßburg' (1518) ließ über 400 Menschen unkontrolliert tanzen — manche bis zum Tod. Was gilt heute als wahrscheinlichste wissenschaftliche Erklärung?",
        answerA = "Massenpsychose durch extreme soziale Stresssituation (Hunger, Pest, Armut)",
        answerB = "Vergiftung durch Mutterkornpilz (Ergot), der auf dem Getreide wuchs",
        answerC = "Religiöse Hysterie ausgelöst durch eine Predigt über den heiligen Vitus",
        answerD = "Choreomanischer Virus, ähnlich moderner Chorea-Erkrankungen",
        correctAnswer = 0,
        explanation = "Historiker John Waller vertritt heute die Massenhystesetheorie: Straßburg 1518 litt unter extremer Hungersnot, Syphilis-Epidemie und religiösem Fanatismus. Die Tanzenden glaubten, von einem Fluch des Heiligen Vitus befallen zu sein. Massenpsychose als kollektive Reaktion auf extremen sozialen Stress erklärt das Phänomen am überzeugendsten. Die Mutterkorntheorie würde eher Krämpfe als Tanzen bewirken.",
        difficulty = 5,
        funFact = "Die Stadtverwaltung Straßburgs reagierte zunächst mit dem Bau zusätzlicher Bühnen und der Anstellung von Musikern — in der Annahme, die Tanzenden müssten 'austanzen'. Erst als Menschen starben, wurden Verbote erlassen und Pilger nach Bad Saverne geschickt, wo der Heilige Vitus verehrt wurde."
    ),

    Question(
        categoryId = 11,
        questionText = "Die 'Große Moonshinerschlacht' von New York im Jahr 1835 — bekannt als 'Great Moon Hoax' — wurde von welcher Zeitung betrieben und welchen Zweck erfüllte sie?",
        answerA = "New York Tribune; politische Kritik am Kongress durch Satire",
        answerB = "The Sun; Auflagensteigerung durch erfundene Berichte über Fledermausmenschen auf dem Mond",
        answerC = "New York Herald; Werbung für ein neues Teleskop der Universität Harvard",
        answerD = "The Morning Post; Sabotage eines Konkurrenzjournals durch Falschmeldungen",
        correctAnswer = 1,
        explanation = "The Sun (New York) publizierte ab August 1835 sechsteilige Berichte, die angeblich dem Edinburgh Journal of Science entnommen waren und Entdeckungen von Fledermausmenschen, blauen Einhörnern und Mondtempeln durch Sir John Herschel beschrieben. Die Auflage der Zeitung verdreifachte sich. Der Hoax war so überzeugend, dass selbst Wissenschaftler zeitweise glaubten.",
        difficulty = 5,
        funFact = "Sir John Herschel, der angebliche Entdecker, befand sich zu dieser Zeit in Südafrika und erfuhr erst Monate später von dem Hoax, der seinen Namen trug. Er soll darüber amüsiert gewesen sein — bis fremde Menschen anfingen, ihm Briefe über seine 'Mondbewohner' zu schreiben."
    ),

    // --- Tiere und Geschichte ---

    Question(
        categoryId = 11,
        questionText = "Napoleon Bonaparte wurde 1807 angeblich von einer Armee von Tieren angegriffen — aber welchen?",
        answerA = "Wildschweinen, die aus einem angrenzenden Wald ausbrachen",
        answerB = "Kaninchen, die für eine Siegesjagd nach Tilsit gefangen worden waren",
        answerC = "Ratten, die im Lager einen Aufstand verursachten",
        answerD = "Ochsen, die bei einem Erntefest durchgingen",
        correctAnswer = 1,
        explanation = "Nach dem Vertrag von Tilsit ließ Napoleon eine Kaninchenjagd als Feier für seine Marschälle arrangieren. Chef d'état-major Berthier hatte statt wilder Hasen zahme Kaninchen organisiert. Als Napoleons Jagdhorn erklang, stürmten hunderte Kaninchen nicht fliehend davon, sondern auf Napoleon zu — sie dachten, er sei der Futterlieferant. Napoleon musste in seine Kutsche flüchten.",
        difficulty = 5,
        funFact = "Augenzeugen berichteten, Napoleon sei schließlich von Kaninchen im wahrsten Sinne überrannt worden und hätte erst mit Hilfe seiner Lakaien die Kutsche erreichen können. Marshal Berthier, der für die Panne verantwortlich war, erhielt einen markanten Rüffel — für die Blamage vor den Marschällen."
    ),

    Question(
        categoryId = 11,
        questionText = "Im Mittelalter wurden Tiere vor Gericht gestellt. Welcher Jurist verteidigte 1508 Ratten vor einem Kirchengericht in Autun (Frankreich) erfolgreich mit welchem Argument?",
        answerA = "Barthélemy de Chasseneuz; die Ratten hätten den Getreideschaden nicht absichtlich verursacht",
        answerB = "Barthélemy de Chasseneuz; die Ratten hätten nicht ausreichend Zeit gehabt, um vor Gericht zu erscheinen",
        answerC = "Guillaume Regnault; die Ratten seien von Gott als Strafe geschickt worden und damit unschuldig",
        answerD = "Pierre Ayrault; die Ratten hätten keine Rechtssubjektivität und könnten daher nicht angeklagt werden",
        correctAnswer = 1,
        explanation = "Barthélemy de Chasseneuz (später Präsident des Pariser Parlaments) verteidigte die Ratten der Diözese Autun, die wegen Getreideschäden angeklagt wurden. Sein Geniestreich: Er beantragte erfolgreich eine Vertagung mit der Begründung, die Ratten seien weit verteilt, die Ladung schlecht zugestellt worden, und außerdem drohten ihnen auf dem Weg zum Gericht Katzen — ein unsicheres Geleit sei zu gewähren.",
        difficulty = 5,
        funFact = "Tierprozesse im Mittelalter folgten exakt denselben juristischen Regeln wie Menschenprozesse: Es gab Ankläger, Verteidiger, Zeugen und Urteile. Die schwerste Strafe für Tiere war die öffentliche Hinrichtung. In einem berühmten Fall von 1474 wurde in Basel ein Hahn hingerichtet — wegen des 'teuflischen' Legens eines Hahneneis."
    ),

    // --- Ching Shih ---

    Question(
        categoryId = 11,
        questionText = "Ching Shih war eine der mächtigsten Piratenführerinnen der Geschichte. Wie groß war ihr Flottenverband auf dem Höhepunkt ihrer Macht um 1810?",
        answerA = "Ca. 200 Schiffe und 5.000 Seeleute",
        answerB = "Ca. 800 Schiffe und 40.000 Seeleute",
        answerC = "Ca. 1.800 Schiffe und 80.000 Seeleute",
        answerD = "Ca. 400 Schiffe und 15.000 Seeleute",
        correctAnswer = 2,
        explanation = "Ching Shih (auch: Zheng Yi Sao) führte die Rote Flaggenflotte mit geschätzten 1.800 Dschunken und bis zu 80.000 Seeleuten im Südchinesischen Meer. Sie begann als Prostituierte in Kanton, heiratete den Piratenführer Zheng Yi und übernahm nach dessen Tod 1807 das Kommando. Keine Flotte — weder chinesische, noch portugiesische, noch britische — konnte sie bezwingen.",
        difficulty = 5,
        funFact = "Ching Shih führte ein strenges Gesetzbuch: Deserteure verloren ein Ohr, Plünderung ohne Befehl wurde mit dem Tod bestraft, und unerlaubter Verkehr mit weiblichen Gefangenen war ebenfalls todeswürdig. Sie handelte schließlich einen Friedensvertrag aus, der ihr erlaubte, ihre Flotte zu behalten, und lebte als erfolgreiche Geschäftsfrau bis 69 Jahre."
    ),

    // --- Vergessene Kuriositäten ---

    Question(
        categoryId = 11,
        questionText = "Von 1912 bis 1948 wurden bei den Olympischen Spielen Medaillen in Kunstwettbewerben vergeben. In welchen Kategorien war dies möglich?",
        answerA = "Musik, Malerei, Skulptur, Architektur und Literatur",
        answerB = "Dichtung, Theater, Tanz, Ornamentik und Fotografie",
        answerC = "Oper, Symphonie, Freskentechnik, Bildhauerei und Kurzgeschichte",
        answerD = "Grafik, Illustration, Komposition, Dramaturgie und Kunsthandwerk",
        correctAnswer = 0,
        explanation = "Von 1912 (Stockholm) bis 1948 (London) vergaben die Olympischen Spiele tatsächlich Medaillen in fünf Kunstkategorien: Musik, Malerei, Skulptur, Architektur und Literatur — allerdings mussten die Werke sportliche Themen haben. Pierre de Coubertin, Gründer der modernen Olympiade, gewann 1912 selbst die Goldmedaille in Literatur (anonym).",
        difficulty = 5,
        funFact = "De Coubertin gewann 1912 unter dem Pseudonym 'M. Eschbach' die Goldmedaille für seine Ode an den Sport. Das IOC entdeckte erst nach seinem Tod 1937, dass er selbst der Autor war. Die Kunstwettbewerbe wurden 1948 abgeschafft, weil die meisten Teilnehmer Profis waren und das Amateurprinzip verletzten."
    ),

    Question(
        categoryId = 11,
        questionText = "Cleopatra VII. war keine Ägypterin von Abstammung — was war ihre tatsächliche Herkunft?",
        answerA = "Nubisch-äthiopisch, aus einer südlichen Dynastielinie",
        answerB = "Makedonisch-griechisch, als Nachfahrin des Ptolemaios I.",
        answerC = "Persisch-assyrisch, nach Alexanders Ostexpansion",
        answerD = "Karthagisch-punisch, durch diplomatische Heiratspolitik",
        correctAnswer = 1,
        explanation = "Cleopatra VII. entstammte der ptolemäischen Dynastie, die von Ptolemaios I. Soter gegründet wurde — einem makedonischen General Alexanders des Großen. Die Ptolemäer regierten Ägypten ab 305 v. Chr. und heirateten ausschließlich innerhalb der Familie. Cleopatra war die erste ptolemäische Herrscherin, die überhaupt Ägyptisch lernte; ihre Vorgänger sprachen ausschließlich Griechisch.",
        difficulty = 5,
        funFact = "Cleopatra sprach angeblich neun Sprachen: Ägyptisch, Griechisch, Latein, Äthiopisch, Aramäisch, Arabisch, Hebräisch, Medisch und Parthisch. Sie war damit die erste ptolemäische Herrscherin, die mit ihren Untertanen ohne Dolmetscher sprechen konnte — ein politisches Instrument, das sie geschickt einsetzte."
    ),

    // --- Wissenschafts-Kuriositäten ---

    Question(
        categoryId = 11,
        questionText = "König Gustav III. von Schweden führte im 18. Jahrhundert ein berühmtes 'Todes-Experiment' mit Kaffee durch. Was war das Ergebnis?",
        answerA = "Beide Probanden starben jung — Kaffeetrinker mit 46, Teetrinker mit 51 Jahren",
        answerB = "Der Teetrinker starb zuerst; der Kaffeetrinker überlebte bis 83",
        answerC = "Beide überlebten den König selbst; Gustav III. wurde ermordet, bevor das Experiment endete",
        answerD = "Das Experiment wurde abgebrochen, als beide Probanden gesund blieben",
        correctAnswer = 2,
        explanation = "Gustav III. ließ zwei zum Tode verurteilte Zwillinge begnadigen: Einer musste täglich drei Kannen Kaffee trinken, der andere drei Kannen Tee. Zwei Ärzte sollten die Auswirkungen beobachten. Beide Ärzte starben während des Experiments. König Gustav III. wurde 1792 ermordet. Schließlich starb der Teetrinker zuerst. Der Kaffeetrinker lebte bis 83 Jahre.",
        difficulty = 5,
        funFact = "Das Experiment war medizinisch wertlos, da keine Kontrollgruppe existierte und die Versuchsbedingungen nicht standardisiert waren. Gustav III. war fest davon überzeugt, Kaffee sei ein Gift — und Schweden trank damals ungewöhnlich viel davon. Paradoxerweise konsumiert Schweden heute pro Kopf mehr Kaffee als fast jedes andere Land."
    ),

    Question(
        categoryId = 11,
        questionText = "Der 'Hundert-Jahre-Krieg' zwischen England und Frankreich dauerte tatsächlich wie lange?",
        answerA = "Exakt 100 Jahre (1337–1437)",
        answerB = "116 Jahre (1337–1453)",
        answerC = "89 Jahre (1339–1428)",
        answerD = "134 Jahre (1328–1462)",
        correctAnswer = 1,
        explanation = "Der Hundertjährige Krieg dauerte von 1337 bis 1453 — genau 116 Jahre, nicht 100. Der Begriff wurde erst im 19. Jahrhundert von Historikern geprägt. Der Krieg bestand aus mehreren Phasen mit Waffenstillständen dazwischen und endete mit dem Fall von Bordeaux am 19. Oktober 1453, womit England fast alle Festlandsbesitzungen verlor.",
        difficulty = 5,
        funFact = "Calais blieb als einzige englische Festlandshochburg bis 1558 in englischer Hand — 105 Jahre nach dem offiziellen Kriegsende. Als Königin Mary I. von England 1558 auf dem Sterbebett lag, soll sie gesagt haben, man werde nach ihrem Tod das Wort 'Calais' in ihr Herz eingeritzt finden."
    ),

    // --- Abraham Lincoln ---

    Question(
        categoryId = 11,
        questionText = "Abraham Lincoln war vor seiner politischen Karriere für welche sportliche Leistung bekannt?",
        answerA = "Er war Boxchampion des Staates Illinois mit 47 Siegen",
        answerB = "Er war Ringkampf-Champion mit ca. 300 Kämpfen und nur einer Niederlage",
        answerC = "Er gewann als erster Präsident einen nationalen Ruderwettbewerb",
        answerD = "Er hielt den Speerrekord für Illinois bis 1880",
        correctAnswer = 1,
        explanation = "Abraham Lincoln war ein außergewöhnlicher Ringer — in seiner Jugend in Illinois bestritt er ca. 300 Ringkämpfe und verlor nach überlieferten Quellen nur einen einzigen. Er galt als 'county champion' und war für seine außergewöhnliche Körperkraft bekannt. Die National Wrestling Hall of Fame hat ihn in ihrer 'Outstanding American' Kategorie geehrt.",
        difficulty = 5,
        funFact = "Lincoln war mit 1,93 m ungewöhnlich groß für seine Zeit — ein Vorteil im Ringen. Seine bekannteste Niederlage erlitt er gegen Jack Armstrong, Anführer der Clary's Grove Boys. Lincoln selbst leugnete die Niederlage und bestand darauf, das Spiel sei unentschieden gewesen. Die beiden wurden danach trotzdem Freunde."
    ),

    // --- Römer und Kuriositäten ---

    Question(
        categoryId = 11,
        questionText = "Was war laut historischen Quellen im Römischen Reich auf Münzen mit dem Kaiserbildnis verboten?",
        answerA = "Die Münzen durften nicht eingeschmolzen oder beschnitten werden",
        answerB = "Münzen mit Kaiserkonterfei durften nicht als Zahlungsmittel in Bordellen verwendet werden",
        answerC = "Das Aufbewahren von Münzen mit dem Bildnis verstorbener Kaiser war verboten",
        answerD = "Münzen durften nicht mit der Kaiserseite nach oben auf den Boden gelegt werden",
        correctAnswer = 1,
        explanation = "In der frühen Kaiserzeit galt das Verbot, Denare und Sesterzen mit dem Kaiserportrait in Bordellen als Zahlungsmittel zu verwenden — da dies als Beleidigung des Kaisers (maiestas) gewertet werden konnte. Tiberius soll deswegen einen Senator verurteilt haben, der eine Münze mit Augustus-Bildnis in ein Bordell getragen hatte. Speziell geprägte 'Spintrien' (Bordellmünzen) dienten als Ersatz.",
        difficulty = 5,
        funFact = "Die sogenannten 'Spintrien' (erotische Bronzetoken) gelten von manchen Historikern als Bordellwährung des antiken Roms. Tatsächlich könnte ihre Funktion auch als Theatertickets oder Spielchips erklärt werden — die Forschung ist sich nicht einig. Die Münzen mit Zahlen auf der einen und erotischen Szenen auf der anderen Seite bleiben eines der rätselhaftesten Objekte der Numismatik."
    ),

    // --- Hiroo Onoda ---

    Question(
        categoryId = 11,
        questionText = "Der japanische Soldat Hiroo Onoda kämpfte bis 1974 auf den Philippinen weiter. Was überzeugte ihn schließlich, den Krieg zu beenden?",
        answerA = "Er wurde durch einen Gefechtsschuss verletzt und gefangen genommen",
        answerB = "Sein ehemaliger Kommandant reiste persönlich in den Dschungel und gab ihm offiziell Befehl zur Kapitulation",
        answerC = "Er hörte Kaisers Hirohito Radiobotschaft von 1945 über ein neu gefundenes Radio",
        answerD = "Philippinische Soldaten überzeugten ihn mit einer Kopie der japanischen Kapitulationsurkunde",
        correctAnswer = 1,
        explanation = "Hiroo Onoda weigerte sich über 29 Jahre, die Kapitulation anzuerkennen, und hielt jede Kapitulationsnachricht für Feindesspropaganda. Erst als sein ehemaliger Kommandant Major Yoshimi Taniguchi im März 1974 persönlich in den Dschungel auf Lubang Island reiste und ihm offiziell die Kapitulationsbefehle übermittelte, legte Onoda die Waffen nieder.",
        difficulty = 5,
        funFact = "Onoda wurde von Philippins Präsident Ferdinand Marcos offiziell begnadigt. Er hatte in seinen Jahrzehnten im Dschungel mindestens 30 philippinische Zivilisten getötet, die er für Feinde hielt. Japan empfing ihn als Helden. Er lebte bis 2014 und starb mit 91 Jahren."
    ),

    // --- Seltsame wissenschaftliche Fakten ---

    Question(
        categoryId = 11,
        questionText = "Wer erfand das Wort 'Scientist' (Wissenschaftler) und wann wurde es das erste Mal verwendet?",
        answerA = "Isaac Newton im Jahr 1687 in seinen 'Principia'",
        answerB = "William Whewell im Jahr 1833 als Antwort auf eine Frage von Mary Somerville",
        answerC = "Francis Bacon im Jahr 1620 in seinem 'Novum Organum'",
        answerD = "Charles Darwin im Jahr 1837 in einem Brief an Charles Lyell",
        correctAnswer = 1,
        explanation = "Das englische Wort 'scientist' wurde erstmals 1833 von William Whewell (Cambridge) geprägt. Bei einer Sitzung der British Association for the Advancement of Science fragte Mary Somerville, wie man jemanden nennen solle, der Naturwissenschaften betreibt. Whewell schlug 'scientist' vor — nach dem Vorbild von 'artist'. Bis dahin nannten sich solche Personen 'men of science' oder 'natural philosophers'.",
        difficulty = 5,
        funFact = "Whewell war einer der produktivsten Wortschöpfer der Wissenschaftsgeschichte: Er prägte auch 'physicist', 'anode', 'cathode', 'ion', 'consilience' und viele weitere Fachbegriffe. Michael Faraday bat ihn speziell um neue Bezeichnungen für elektrochemische Konzepte — Whewell lieferte 'Elektrode', 'Anode' und 'Kathode'."
    ),

    Question(
        categoryId = 11,
        questionText = "Was entdeckte Alexander Fleming eigentlich, bevor er Penicillin entdeckte — und welcher Zufall machte Penicillin möglich?",
        answerA = "Er entdeckte Lysozym im Nasenschleim (1921); Penicillin entstand durch einen kontaminierten Urlaub-Petrischalen-Schimmel (1928)",
        answerB = "Er entdeckte Streptomycin; Penicillin fand er bei der Analyse von Soldatenwunden 1917",
        answerC = "Er entdeckte die Phagentherapie; Penicillin synthetisierte er zufällig bei Farbexperimenten",
        answerD = "Er entdeckte Morphin im Schlafmohn; Penicillin isolierte er bei der Suche nach einem Schlafmittel",
        correctAnswer = 0,
        explanation = "Fleming entdeckte 1921 Lysozym — ein natürliches Enzym im Nasenschleim, das Bakterien abtöten kann. Penicillin entdeckte er 1928, als er nach dem Urlaub zurückkam und bemerkte, dass ein Penicillium-Schimmelfleck eine Petrischale mit Staphylokokken kontaminiert hatte — und alle Bakterien um den Schimmelpilz waren abgestorben. Seine Urlaubsabwesenheit und seine Beobachtungsgabe retteten Millionen Leben.",
        difficulty = 5,
        funFact = "Fleming machte die Penicillin-Entdeckung nicht allein zum medizinisch nutzbaren Medikament — das gelang Howard Florey und Ernst Boris Chain 1940. Alle drei teilten 1945 den Nobelpreis. Fleming hatte jahrelang erfolglos versucht, Penicillin zu konzentrieren; Florey und Chain lösten das technische Problem."
    ),

    // --- Vergessene Fakten ---

    Question(
        categoryId = 11,
        questionText = "Der erste Telefonnummer-Werbeanruf in der Geschichte wurde 1877 von wem und für was gemacht?",
        answerA = "Alexander Graham Bell rief eine Zeitung an, um Telefongeräte zu vermarkten",
        answerB = "Ein Zahnarzt in Albany, New York, rief alle Telefonbesitzer der Stadt an, um für seine Praxis zu werben",
        answerC = "Die Telegraphen-Gesellschaft Western Union versandte Werbenachrichten an alle Telefoninhaber",
        answerD = "Thomas Edison benutzte das Telefon zur Vermarktung seines Phonographen",
        correctAnswer = 1,
        explanation = "Der erste bekannte Telefonwerbeanruf der Geschichte wurde 1877 von einem Zahnarzt in Albany, New York, gemacht — er rief systematisch alle Telefoninhaber der Stadt an, um für seine Praxis zu werben. Dies geschah nur ein Jahr nach Bells Erfindung. Albany hatte zu diesem Zeitpunkt eine der ersten städtischen Telefonleitungsnetze der USA.",
        difficulty = 5,
        funFact = "Die Erfindung des Telefons löste sofort die Frage aus: Wie begrüßt man jemanden? Alexander Graham Bell bestand auf 'Ahoy!' (wie ein Seemann). Thomas Edison setzte sich mit 'Hello' durch. Heute ist 'Hello' weltweit der Standard-Telefongruß — Edisons Version hat gewonnen."
    ),

    Question(
        categoryId = 11,
        questionText = "Die erste Sommerzeitumstellung der Geschichte fand statt im Jahr 1916 — in welchem Land als erstem und aus welchem Grund?",
        answerA = "Großbritannien; zur Verlängerung der Arbeitszeit in Rüstungsfabriken",
        answerB = "Deutschland; zur Kohle- und Energieeinsparung im Ersten Weltkrieg",
        answerC = "USA; auf Initiative Benjamin Franklins als Energiesparmaßnahme",
        answerD = "Österreich-Ungarn; zur Koordination mit deutschen Truppenbewegungen",
        correctAnswer = 1,
        explanation = "Das Deutsche Reich führte am 30. April 1916 um 23 Uhr die erste gesetzliche Sommerzeitumstellung der Geschichte ein — als Kriegsmaßnahme zur Kohle- und Energieeinsparung. Die Uhr wurde um eine Stunde auf den 1. Mai 1:00 Uhr vorgestellt. Österreich-Ungarn folgte wenige Tage später; Großbritannien am 21. Mai 1916.",
        difficulty = 5,
        funFact = "Benjamin Franklin schlug 1784 in einem humoristischen Essay an das Journal de Paris vor, die Pariser sollten früher aufstehen, um Kerzenöl zu sparen — aber er meinte Verhaltensänderung, keine Uhrenumstellung. Der neuseeländische Entomologe George Hudson war 1895 der erste, der eine echte Uhrenumstellung vorschlug — damit er nach seiner Arbeit noch Tageslicht für Insektensammeln hatte."
    ),

    // --- Geographische Kuriositäten ---

    Question(
        categoryId = 11,
        questionText = "Welches Land hat technisch gesehen zwei Hauptstädte, weil eine Stadt nie offiziell zur alleinigen Hauptstadt erklärt wurde?",
        answerA = "Südafrika, das drei Hauptstädte für drei Regierungszweige hat",
        answerB = "Bolivien, das Sucre als Verfassungs- und La Paz als Regierungshauptstadt hat",
        answerC = "Israel, das Jerusalem und Tel Aviv beide als Hauptstädte beansprucht",
        answerD = "Myanmar, das Naypyidaw und Rangun gleichrangig führt",
        correctAnswer = 1,
        explanation = "Bolivien hat zwei offizielle Hauptstädte: Sucre ist die verfassungsmäßige Hauptstadt und Sitz des Obersten Gerichtshofs; La Paz ist die Regierungshauptstadt mit Exekutive und Legislative. Dieser Kompromiss entstand nach dem Bürgerkrieg von 1899, als La Paz gewann, aber Sucre seinen Verfassungsstatus behalten durfte.",
        difficulty = 5,
        funFact = "Südafrika übertrifft sogar Bolivien: Es hat drei Hauptstädte. Pretoria (Exekutive/Regierung), Kapstadt (Legislative/Parlament) und Bloemfontein (Judikative/Oberstes Gericht). Diese Aufteilung stammt ebenfalls aus einem politischen Kompromiss — diesmal zwischen den Boeren-Republiken und der britischen Kolonie nach dem Burenkrieg."
    ),

    // --- Napoleons Kuriositäten ---

    Question(
        categoryId = 11,
        questionText = "Was ist die seltsamste bekannte Kuriositätmit Napoleons Schlachtplänen bei Waterloo, die erst 2021 durch Historiker dokumentiert wurde?",
        answerA = "Napoleon schlief während der entscheidenden Stunden der Schlacht — möglicherweise durch Hämorrhoidenschmerzen und Opiumgabe",
        answerB = "Napoleons Karte von Waterloo enthielt einen Fehler: Ein Wald war falsch eingezeichnet",
        answerC = "Napoleons Uhren gingen 40 Minuten falsch, was das Timing seines Angriffsbefehls verzerrt hat",
        answerD = "Sein Hauptquartier war im falschen Dorf — er verwechselte zwei ähnlich klingende Ortsnamen",
        correctAnswer = 0,
        explanation = "Historiker haben lange diskutiert, warum Napoleon bei Waterloo (18. Juni 1815) erst spät mit dem Angriff begann. Eine medizinische Hypothese: Napoleon litt an akuten Hämorrhoiden und soll Opium zur Schmerzlinderung genommen haben, was seine Reaktionszeit verlangsamte. In der entscheidenden Morgenstunden, als Wellington Verteidigungsstellungen aufbaute, war Napoleon möglicherweise eingeschlafen.",
        difficulty = 5,
        funFact = "Napoleon soll nach der Niederlage bei Waterloo kommentiert haben: 'Dies ist der erschreckendste Abend meines Lebens — außer dem Abend, als ich von Kaninchen überrannt wurde.' Ob er das wirklich sagte, ist historisch nicht belegt, passt aber zu dem Mann, der sich einst von einer Kaninchenarmee hatte vertreiben lassen."
    ),

    // --- Weitere obskure Ereignisse ---

    Question(
        categoryId = 11,
        questionText = "Was war der 'Pig War' von 1859 zwischen den USA und Großbritannien, und was war sein Auslöser?",
        answerA = "Amerikanische und britische Farmer stritten sich um eine Schweineherde auf der Grenzinsel San Juan Island — ein geschossenes Schwein löste den Zwischenfall aus",
        answerB = "Ein britisches Kriegsschiff beschlagnahmte amerikanische Schweine als Proviant, was fast zu einem Krieg führte",
        answerC = "Amerikanische Einwanderer schlachteten britische Hausschweine, was einen Grenzzwischenfall verursachte",
        answerD = "Britische Kolonisten in Kanada besetzten eine amerikanische Schweinefarm im Oregon Territory",
        correctAnswer = 0,
        explanation = "Der 'Schweine-Krieg' begann am 15. Juni 1859, als amerikanischer Farmer Lyman Cutlar auf San Juan Island ein britisches Hausschwein erschoss, das in seinem Garten wühlte. Die Briten verlangten seine Verhaftung; die USA schickten Truppen; Großbritannien schickte Kriegsschiffe. Monatelang standen sich 461 US-Soldaten und 5 britische Kriegsschiffe gegenüber — der einzige Tote blieb das besagte Schwein.",
        difficulty = 5,
        funFact = "Der 'Schweinekrieg' endete nach 13 Jahren durch die Vermittlung von Kaiser Wilhelm I. von Deutschland als Schiedsrichter. 1872 entschied er zugunsten der USA. San Juan Island gehört heute zum US-Bundesstaat Washington — und der Nationalpark dort wird humorvoll 'Pig War National Historical Park' genannt."
    ),

    Question(
        categoryId = 11,
        questionText = "Die 'Stille Invasion' von 1977 — ein obskures militärisches Ereignis der Kalten-Kriegs-Ära — fand wo statt?",
        answerA = "Sowjetische Truppen besetzten drei Wochen lang eine finnische Insel versehentlich",
        answerB = "Ein sowjetisches U-Boot lief versehentlich auf einer schwedischen Marinebasis auf Grund",
        answerC = "Sowjetische Soldaten überschritten versehentlich die norwegische Grenze und errichteten ein Lager",
        answerD = "Ein dänisches Kriegsschiff ankerte versehentlich in sowjetischen Gewässern für acht Stunden",
        correctAnswer = 1,
        explanation = "Am 27. Oktober 1981 lief das sowjetische U-Boot S-363 (Whiskey-Klasse) auf Grund nahe der schwedischen Marinebasis Karlskrona — mitten in schwedischen Hoheitsgewässern und in einem militärischen Sperrgebiet. Die 'Whiskey on the Rocks'-Affäre, wie Schwedens Presse sie nannte, löste eine internationale Krise aus. Die Sowjets behaupteten, es sei ein Navigationsfehler gewesen.",
        difficulty = 5,
        funFact = "Schwedische Wissenschaftler maßen erhöhte Gammastrahlung am U-Boot, was Nuclear-Torpedos an Bord nahelegte — was die Sowjets heftig bestritten. Das U-Boot und seine Besatzung wurden nach mehrtägigen Verhandlungen freigelassen. Die Affäre steigerte Schwedens Verteidigungsausgaben erheblich."
    ),

    Question(
        categoryId = 11,
        questionText = "Was war der 'Lachanfall von Tanganjika' 1962 — und warum ist er für die Wissenschaft bis heute bedeutsam?",
        answerA = "Ein Lachgasunfall in einer Chemiefabrik ließ Hunderte Arbeiter tagelang lachen",
        answerB = "Eine mysteriöse Lachepidemie brach in einem Mädcheninternat aus und verbreitete sich auf Tausende über Monate",
        answerC = "Ein nigerianischer Stammesritus löste kollektive Lachkrämpfe aus, die als Masseninfektion dokumentiert wurden",
        answerD = "Schüler schluckten versehentlich Lachgas aus Kalksteindünger und entwickelten monatelangen Lachdrang",
        correctAnswer = 1,
        explanation = "Im Januar 1962 brach im Mädcheninternat Kashasha (Tanganjika, heute Tansania) eine Lachepidemie aus: Schülerinnen begannen unkontrolliert zu lachen, zu weinen und in Ohnmacht zu fallen. Die Schule schloss. Die Schülerinnen trugen die Hysterie in ihre Heimatdörfer; bis zu 1.000 Personen waren betroffen. Die Epidemie dauerte 18 Monate und betraf 14 Schulen. Sie gilt als klassischer Fall von Massenpsychogener Erkrankung.",
        difficulty = 5,
        funFact = "Die Symptome umfassten neben Lachen auch Weinen, Ausschläge, Ohnmacht, Schmerzen und Flatulenz. Mediziner fanden keine physische Ursache. Der Fall ist einer der am besten dokumentierten Fälle von Massenpsychose und wird in Psychologiestudien bis heute als Lehrbuchbeispiel zitiert."
    ),

    // --- Vergessene Erfinder ---

    Question(
        categoryId = 11,
        questionText = "Wer erfand das World Wide Web — und nicht das Internet, wie oft fälschlicherweise behauptet wird?",
        answerA = "Vint Cerf und Bob Kahn, die auch TCP/IP entwickelten",
        answerB = "Tim Berners-Lee am CERN 1989/1991",
        answerC = "Linus Torvalds als Nebenprojekt zu Linux 1991",
        answerD = "Marc Andreessen, der auch den ersten Browser Mosaic schrieb",
        correctAnswer = 1,
        explanation = "Tim Berners-Lee erfand das World Wide Web 1989/91 am CERN als System für den wissenschaftlichen Informationsaustausch. Er entwickelte HTTP (Protokoll), HTML (Sprache) und URLs (Adressen) und schrieb den ersten Browser/Editor 'WorldWideWeb'. Das Internet (Netzwerkinfrastruktur) existierte bereits seit den 1960ern (ARPANET). Berners-Lee patentierte seine Erfindung nie und machte sie frei verfügbar.",
        difficulty = 5,
        funFact = "Berners-Lee' erster Vorschlag an CERN-Chef Mike Sendall 1989 trug den Titel 'Information Management: A Proposal'. Sendalls handschriftliche Notiz darauf lautete: 'Vague but exciting' (Vage, aber aufregend). Mit dieser Randnotiz begann das World Wide Web."
    ),

    // --- Medizin-Kuriositäten ---

    Question(
        categoryId = 11,
        questionText = "Die Lobotomie wurde 1949 mit dem Nobelpreis für Medizin ausgezeichnet. Wer erhielt den Preis und warum ist diese Vergabe heute so kontrovers?",
        answerA = "Walter Freeman; er führte über 3.000 Lobotomien durch, viele durch die Augenhöhle mit einem Eispickel",
        answerB = "António Egas Moniz; er entwickelte den Eingriff, obwohl Langzeitergebnisse verheerend waren",
        answerC = "Charles Burckhardt; er lobotomierte Geisteskranke massenweise in einer Schweizer Klinik",
        answerD = "John Fulton; er übertrug den Tierversuch auf Menschen ohne klinische Studien",
        correctAnswer = 1,
        explanation = "Der portugiesische Neurochirurg António Egas Moniz erhielt 1949 den Nobelpreis für Physiologie/Medizin für die Entwicklung der präfrontalen Leukotomie (Lobotomie). Heute gilt die Auszeichnung als eine der umstrittensten Nobelpreise: Der Eingriff verursachte bei Tausenden Patienten dauerhafte Persönlichkeitsveränderungen, Gedächtnisverlust und Apathie. Ein Nobelpreis wurde noch nie zurückgezogen.",
        difficulty = 5,
        funFact = "Einer der Patienten von Egas Moniz schoss 1939 auf ihn — was ihn zum Rollstuhlfahrer machte. Der amerikanische Arzt Walter Freeman perfektionierte eine schnellere Methode ('Eispickel-Lobotomie') und führte über 3.500 Eingriffe durch, darunter an Rosemary Kennedy, der Schwester des späteren Präsidenten. Sie lebte danach 64 Jahre in einer Pflegeeinrichtung."
    ),

    Question(
        categoryId = 11,
        questionText = "Was verstand man in der Medizin des 19. Jahrhunderts unter 'Hysterie' — und welche Behandlung wendeten Ärzte häufig an?",
        answerA = "Gebärmutterkrämpfe; behandelt mit Morphiuminjektion",
        answerB = "Nervöse Erschöpfung bei Frauen; behandelt durch manuelle genitale Massage durch den Arzt bis zur 'Hysterischen Paroxysmus'",
        answerC = "Kreislaufschwäche; behandelt durch kalte Bäder und Blutegel",
        answerD = "Angstzustände; behandelt durch Chloroform-Einatmung in kontrollierten Dosen",
        correctAnswer = 1,
        explanation = "Im 19. und frühen 20. Jahrhundert diagnostizierten Ärzte 'Hysterie' bei Frauen als Sammelbegriff für Nervenleiden, die auf die Gebärmutter (griech. 'hystera') zurückgeführt wurden. Die Standardbehandlung war 'pelvic massage' (manuell durch den Arzt) bis zum 'Hysterischen Paroxysmus' (Orgasmus). Da dies Ärzte erschöpfte, entwickelte Joseph Mortimer Granville ca. 1880 den ersten elektrischen Vibrator als 'medizinisches Gerät'.",
        difficulty = 5,
        funFact = "Die Diagnose 'Hysterie' wurde erst 1980 aus dem DSM (Diagnostic and Statistical Manual of Mental Disorders) gestrichen. Sigmund Freud und Josef Breuer entwickelten einen großen Teil der Psychoanalyse aus der Behandlung von 'hysterischen' Patientinnen — die meisten litten nach heutigem Verständnis unter PTBS, Depression oder Angststörungen."
    ),

    // --- Geopolitische Kuriositäten ---

    Question(
        categoryId = 11,
        questionText = "Das 'Baumfällkommando' von 1976 brachte die USA und Nordkorea fast an den Rand eines Krieges. Was war der Auslöser?",
        answerA = "US-Soldaten fällten in der Entmilitarisierten Zone einen Baum, der die Sicht auf einen nordkoreanischen Beobachtungsposten verdeckte — nordkoreanische Soldaten erschlugen dabei zwei US-Offiziere",
        answerB = "Nordkoreanische Soldaten fällten einen symbolischen Baum auf der Demarkationslinie, was als Grenzverletzung gewertet wurde",
        answerC = "Ein US-Soldat sägte versehentlich einen Baum in der Pufferzone ab und löste damit Schüsse aus beiden Lagern aus",
        answerD = "Nordkorea fällte den Baum, unter dem das Waffenstillstandsabkommen von 1953 unterzeichnet worden war",
        correctAnswer = 0,
        explanation = "Am 18. August 1976 schickten US- und südkoreanische Streitkräfte eine Truppe in die Joint Security Area des Panmunjom, um eine Pappel zu fällen, die Beobachtungsposten verdeckte. Nordkoreanische Soldaten griffen an und erschlugen mit Äxten Kapitän Arthur Bonifas und First Lieutenant Mark Barrett. Die USA reagierten mit 'Operation Paul Bunyan': 813 Soldaten fällten den Baum unter Deckung von Bombern und Kriegschiffen.",
        difficulty = 5,
        funFact = "Die Operation kostete nach Schätzungen 1 Million Dollar — für das Fällen eines einzelnen Baums. Der Baumstumpf blieb als Symbol stehen und ist bis heute an der DMZ zu besichtigen. Nordkorea entschuldigte sich danach halbherzig — eines der seltenen Male, dass es das je getan hat."
    ),

    Question(
        categoryId = 11,
        questionText = "Was war die 'Kuba-Krise' von 1962 beinahe durch einen einzelnen sowjetischen U-Boot-Kommandanten — Vasili Arkhipov?",
        answerA = "Er verhinderte den Start einer Atomrakete von einem U-Boot, indem er als einziger Offizier die Freigabe verweigerte",
        answerB = "Er versenkte ein amerikanisches Zerstörer-Wrack, was den Krieg beinahe ausgelöst hätte",
        answerC = "Er entdeckte das Abhörnetz der CIA und meldete es, was zur Entspannung führte",
        answerD = "Er trat als Vermittler zwischen Kennedy und Chruschtschow auf",
        correctAnswer = 0,
        explanation = "Am 27. Oktober 1962 (dem 'Schwarzen Samstag') war das sowjetische U-Boot B-59 von US-Kriegsschiffen mit Übungstiefseebomben beschossen worden. Kapitän Valentin Savitsky glaubte, der Krieg habe begonnen, und wollte den atomaren Torpedo (äquivalent zu Hiroshima) abfeuern. Laut Protokoll mussten alle drei Offiziere zustimmen. Vasili Arkhipov, der Brigadekommandant, verweigerte als Einziger seine Zustimmung.",
        difficulty = 5,
        funFact = "Thomas Blanton vom National Security Archive kommentierte 2002: 'Ein Mann namens Vasili Arkhipov hat die Welt gerettet.' Die Geschichte wurde erst in den 2000ern durch deklassifizierte sowjetische Dokumente bekannt. Arkhipov starb 1998, ohne jemals öffentlich für seine Tat anerkannt worden zu sein."
    ),

    // --- Astronomische Kuriositäten ---

    Question(
        categoryId = 11,
        questionText = "Was war der 'Wow!-Signal' von 1977 und warum ist es bis heute rätselhaft?",
        answerA = "Ein kodiertes Radio-Muster vom Planeten Mars, das sich als Reflexion des eigenen Signals erwies",
        answerB = "Ein 72-Sekunden-Radiosignal aus dem Sternbild Schütze, das alle Merkmale eines außerirdischen Signals trägt und nie wiederholt wurde",
        answerC = "Ein Radiopuls von einem Schwarzen Loch, der als künstlich kodierts interpretiert wurde",
        answerD = "Ein Gammastrahlenburst, der von SETI-Forschern als mögliches Nachrichtsformat interpretiert wurde",
        correctAnswer = 1,
        explanation = "Am 15. August 1977 empfing Jerry Ehman am Big Ear Radio Observatory (Ohio) ein 72-Sekunden-Radiosignal bei 1420 MHz aus dem Sternbild Schütze. Es entsprach in Bandbreite und Frequenz exakt dem, was ein außerirdisches Signal haben sollte. Ehman schrieb 'Wow!' neben den Ausdruck. Das Signal wurde trotz Hunderten von Folgebeobachtungen nie wieder empfangen.",
        difficulty = 5,
        funFact = "2017 schlug der Astronom Antonio Paris vor, das Signal könne von einer Kometenwolke stammen. Die SETI-Gemeinschaft war skeptisch. Ehman selbst sagte: 'Ich bin nicht sicher, dass ich glaube, dass es von einer außerirdischen Zivilisation war — aber ich kann es auch nicht ausschließen.' Das Signal bleibt das eindrucksvollste potentielle SETI-Signal aller Zeiten."
    ),

    // --- Politische Kuriositäten ---

    Question(
        categoryId = 11,
        questionText = "Albert Einstein wurde 1952 das Amt des Staatspräsidenten Israels angeboten. Wie begründete er seine Ablehnung?",
        answerA = "Er lehnte ab, weil er kein Hebräisch sprach und Israel nicht sprechen konnte",
        answerB = "Er lehnte ab wegen mangelnder natürlicher Fähigkeit und Erfahrung mit Menschen — das Amt erfordere Menschenkenntnis, nicht physikalisches Denken",
        answerC = "Er war US-Staatsbürger und hätte die israelische Staatsbürgerschaft annehmen müssen",
        answerD = "Er lehnte ab, weil er befürchtete, das Amt könnte seine wissenschaftliche Arbeit beenden",
        correctAnswer = 1,
        explanation = "Nach dem Tod von Chaim Weizmann (erstem Staatspräsidenten) 1952 bot die israelische Regierung Einstein das Präsidentenamt an. In seinem Ablehnungsschreiben erklärte er: 'Ich bin in meinen Jahren alt und nicht in meiner Gesundheit für die angemessene Erfüllung offizieller Funktionen. Außerdem fehlt mir die natürliche Fähigkeit und Erfahrung im Umgang mit Menschen und in der Erfüllung offizieller Pflichten.'",
        difficulty = 5,
        funFact = "Einstein war zwar Zionist und unterstützte die Gründung Israels, blieb aber zeitlebens skeptisch gegenüber einem ethno-nationalen Staat. Er schrieb 1939 mit Sigmund Freud den Brief 'Warum Krieg?' und sagte über sich selbst: 'Meine politischen Ideale sind Demokratie. Ich respektiere niemanden als Herrn.' Ein Präsidentenamt war also in der Tat nicht für ihn gemacht."
    ),

    // --- Mittelalterliche Kuriositäten ---

    Question(
        categoryId = 11,
        questionText = "Welches mittelalterliche Konzept bestimmte, dass ein Mensch durch 'Gottesurteil' (Ordal) überführt wurde — und welche merkwürdige Form war in England die häufigste?",
        answerA = "Ordal durch Feuer: Der Beschuldigte griff in Flammen; Gott schützte die Unschuldigen",
        answerB = "Ordal durch Wasser: Der Beschuldigte wurde ins Wasser getaucht; Sinken bedeutete Unschuld, Schwimmen Schuld",
        answerC = "Ordal durch Zweikampf: Der Beschuldigte kämpfte mit dem Ankläger; der Sieger hatte Recht",
        answerD = "Ordal durch Eid: Der Beschuldigte musste schwören und 12 Eidhelfer benennen",
        correctAnswer = 1,
        explanation = "Das Wasserordal (Ordal durch kaltes Wasser) war in England weit verbreitet: Der Beschuldigte wurde mit Stricken gefesselt ins Wasser geworfen. Die Theorie: Geweihtes Wasser 'akzeptiert' keine Schuldigen — wer schwamm (also schuldig war), wurde herausgezogen und bestraft. Wer sank, war unschuldig — aber oft schon ertrunken. Das Viertes Laterankonzil (1215) verbot kirchliche Beteiligung an Ordalien.",
        difficulty = 5,
        funFact = "Das Logikdilemma des Wasserordalses war zeitgenössischen Kirchenmännern wohl bewusst. Thomas von Aquin diskutierte es philosophisch. Historiker vermuten, dass in der Praxis die Gemeinde und der Priester das Ergebnis oft manipulierten — entweder durch Stricklänge oder durch die Frage, wann man aufhörte zu ziehen."
    ),

    Question(
        categoryId = 11,
        questionText = "Was war die 'Nacht der langen Messer' tatsächlich — und nicht zu verwechseln mit dem NS-Ereignis von 1934?",
        answerA = "Ein Massaker im Frühmittelalter: Sächsische Adelige erschlugen ca. 460 britische Häuptlinge bei einem Friedensbankettim Jahr 472",
        answerB = "Die Bartholomäusnacht 1572 in Frankreich, bei der Tausende Hugenotten ermordet wurden",
        answerC = "Ein Aufstand irischer Sklaven gegen Wikingerhändler im 9. Jahrhundert",
        answerD = "Die Ermordung des Burgunder-Adels durch Attila beim Bankett von 437 n. Chr.",
        correctAnswer = 0,
        explanation = "Die ursprüngliche 'Nacht der langen Messer' (Verspaßen y Cyllyll Hirion) stammt aus walisischer Überlieferung: Im Jahr 472 lud der sächsische Anführer Hengist ca. 460 britische Häuptlinge zu einem Friedensbankett ein. Auf ein Zeichen zogen die Sachsen versteckte Messer und massakrierten die Gäste. Das Ereignis ist aus walisischen Chroniken belegt und gab dem Begriff 'Nacht der langen Messer' seinen ursprünglichen Sinn.",
        difficulty = 5,
        funFact = "Die NS-Säuberungsaktion vom 30. Juni 1934 ('Röhm-Putsch') übernahm bewusst den historischen Namen als Propaganda-Referenz. Hitler kannte wohl die walisische Überlieferung. Der Name wurde danach dauerhaft mit der NS-Geschichte assoziiert — das mittelalterliche Original ist heute kaum noch bekannt."
    ),

    // --- Mehr obskure Fakten ---

    Question(
        categoryId = 11,
        questionText = "Die Chincha-Inseln-Kriege (1864–1866) wurden um was für eine Ressource geführt?",
        answerA = "Kupfervorkommen auf peruanischen Küsteninseln",
        answerB = "Vogelkot (Guano) als begehrten Handelsdünger",
        answerC = "Salzlager für die Fleischkonservierung der spanischen Marine",
        answerD = "Fischgründe im Humboldtstrom vor der Küste Chiles",
        correctAnswer = 1,
        explanation = "Die Chincha-Inseln vor Peru enthielten jahrhundertelange Ablagerungen von Seevogelkot (Guano) — bis zu 50 Meter tiefe Schichten. In der Mitte des 19. Jahrhunderts war Guano der weltweit wichtigste Stickstoffdünger und 'weiße Gold' genannt. Spanien besetzte 1864 die Inseln, was Peru, Chile, Bolivien und Ecuador zur Allianz gegen Spanien veranlasste — der erste südamerikanische gemeinsame Krieg.",
        difficulty = 5,
        funFact = "Peru machte durch den Guano-Export zwischen 1840 und 1880 enorme Gewinne und verschuldete sich gleichzeitig massiv — das 'Guano-Zeitalter' endete mit dem peruanischen Staatsbankrott 1876. Die Erfindung des synthetischen Stickstoffdüngers (Haber-Bosch-Verfahren, 1909) machte Guano dauerhaft wertlos."
    ),

    Question(
        categoryId = 11,
        questionText = "Was war die 'Kartoffelkrieg' (Bayerischer Erbfolgekrieg 1778/79) — und warum heißt er so?",
        answerA = "Bayern bekämpfte einen Kartoffelschädling, den Preußen angeblich eingeschleppt hatte",
        answerB = "Preußische und österreichische Truppen verbrachten mehr Zeit mit Plünderung von Kartoffelfeldern als mit Kämpfen",
        answerC = "Sachsen, Bayern und Österreich stritten um das Monopol auf den Kartoffelanbau in Böhmen",
        answerD = "Friedrich der Große verwendete Kartoffelsuppe als Propagandamittel für den Krieg",
        correctAnswer = 1,
        explanation = "Der Bayerische Erbfolgekrieg (1778/79) zwischen Preußen und Österreich war militärisch kaum ein Krieg: Die Truppen manövrierten durch Böhmen, zogen sich durch Gebirge und verbrachten die meiste Zeit damit, Kartoffelfelder zu plündern, um die Versorgung zu sichern. Es gab kaum Schlachten — stattdessen starben mehr Soldaten an Krankheiten und Hunger als durch Waffen. Daher der Spottname 'Kartoffelkrieg'.",
        difficulty = 5,
        funFact = "Friedrich der Große beschrieb den Krieg in einem Brief als: 'Krieg, in dem meine Grenadiere mehr Kartoffeln gegessen als Schlachten geschlagen haben.' Der Friedensvertrag von Teschen brachte Österreich die Innvierteln — ein Gebiet, das Österreich bis heute besitzt. Selten war der Gewinn eines Krieges so bescheiden wie sein Verlauf."
    ),

    Question(
        categoryId = 11,
        questionText = "Was war die Bedeutung der 'Schwarzen Pest' für die Lohnentwicklung im mittelalterlichen Europa?",
        answerA = "Die Pest senkte die Löhne, da Überlebende billig arbeiteten",
        answerB = "Die Pest erhöhte die Löhne dramatisch, da das Arbeitskräfteangebot kollabierte",
        answerC = "Die Löhne blieben stabil, weil Kirchengesetze Lohnveränderungen verboten",
        answerD = "Die Pest führte zur Einführung von Mindestlöhnen, um Bürgerkrieg zu verhindern",
        correctAnswer = 1,
        explanation = "Die Schwarze Pest (1347–1351) tötete 30–60% der europäischen Bevölkerung. Das drastisch reduzierte Arbeitskräfteangebot ließ die Löhne explodieren: In England stiegen die Reallöhne in den Jahrzehnten nach der Pest auf das Doppelte oder Dreifache. Bauern verlangten und erhielten Freilassung aus der Leibeigenschaft. König Eduard III. von England erließ 1349 das 'Statute of Laborers' — ein Versuch, Löhne zwangsweise zu deckeln, der weitgehend scheiterte.",
        difficulty = 5,
        funFact = "Wirtschaftshistoriker wie Gregory Clark haben gezeigt, dass die Reallöhne europäischer Arbeiter nach der Pest bis ins 16. Jahrhundert auf einem historischen Höchststand blieben — höher als je zuvor und danach erst wieder im 19. Jahrhundert erreicht. Die Pest war eine wirtschaftliche Katastrophe — aber auch eine paradoxe Befreiung für die Überlebenden."
    ),

    Question(
        categoryId = 11,
        questionText = "Was ist das 'Fermi-Paradoxon' und wer formulierte es erstmals 1950 während eines Mittagessens?",
        answerA = "Das Paradoxon fragt: Wenn die Erde nicht einzigartig ist, wo sind dann die Außerirdischen? Formuliert von Enrico Fermi",
        answerB = "Das Paradoxon beschreibt Unstimmigkeiten zwischen kosmischer Schöpfungstheorie und beobachtbarer Materie, benannt nach Fermi",
        answerC = "Fermi fragte, warum Quantenmechanik und Relativitätstheorie unvereinbar seien",
        answerD = "Das Paradoxon beschreibt, warum der Urknall keine Antimaterie hinterlassen hat",
        correctAnswer = 0,
        explanation = "Enrico Fermi fragte 1950 beim Mittagessen in Los Alamos angesichts eines Cartoon über fliegende Untertassen: 'Wo sind sie denn alle?' — 'But where is everybody?' Das Fermi-Paradoxon: Angesichts der Milliarden von Sternen und der hohen Wahrscheinlichkeit außerirdischen Lebens sollten wir Beweise davon sehen. Der Widerspruch zwischen Wahrscheinlichkeit und Fehlen jedes Beweises ist das Paradoxon.",
        difficulty = 5,
        funFact = "Fermis Tischnachbarn bei jenem Mittagessen — Herb York, Emil Konopinski und Edward Teller — haben das Gespräch unterschiedlich erinnert. Die genaue Formulierung ist nicht überliefert. Das Paradoxon beeinflusste die Entwicklung der SETI-Forschung massgeblich und wurde erst 1975 von Michael Hart als 'Fermi-Paradoxon' bezeichnet."
    ),

    Question(
        categoryId = 11,
        questionText = "Das 'Turiner Grabtuch' wurde 1988 mit Radiokarbon auf welchen Zeitraum datiert — und was entgegnen Kritiker dieser Datierung?",
        answerA = "1260–1390 n. Chr. (Mittelalter); Kritiker sagen, die Probe stammte aus einem mittelalterlichen Flickstück",
        answerB = "30–70 n. Chr. (biblische Zeit); Kritiker bezweifeln die Präzision der C14-Messung",
        answerC = "400–600 n. Chr. (Spätantike); Kritiker weisen auf Kontamination durch Kerzenwachs hin",
        answerD = "700–900 n. Chr. (frühes Mittelalter); Kritiker beanstanden die Lagerungsbedingungen",
        correctAnswer = 0,
        explanation = "Drei unabhängige Labore (Oxford, Zürich, Arizona) datierten das Turiner Grabtuch 1988 auf 1260–1390 n. Chr. — also auf ein Mittelalter-Artefakt. Kritiker, darunter der Chemiker Raymond Rogers, argumentierten, die Probeentnahme habe ein mittelalterliches Reparaturflickstück getroffen, nicht das Original-Leinen. Die wissenschaftliche Mehrheit akzeptiert die C14-Datierung; die Debatte dauert an.",
        difficulty = 5,
        funFact = "Das Grabtuch zeigt ein negatives Bild — was erst sichtbar wurde, als Secondo Pia 1898 die erste Fotografie davon machte und beim Entwickeln erschrak: Auf dem Negativ erschien ein positives Gesichtsbild. Diese Entdeckung löste eine weltweite Faszination aus, die bis heute anhält."
    ),

    Question(
        categoryId = 11,
        questionText = "Was war das 'Moriori-Massaker' von 1835 — eines der obskursten Genozide der Geschichte?",
        answerA = "Europäische Walfänger auf den Chatham-Inseln ermordeten die Ureinwohner Moriori",
        answerB = "Maori-Krieger aus Neuseeland eroberten die Chatham-Inseln und versklavten oder töteten die pazifistischen Moriori",
        answerC = "Britische Siedler auf den Chatham-Inseln vertrieben die Moriori in australische Arbeitslager",
        answerD = "Moriori wurden durch eine von europäischen Schiffen eingeschleppte Masernwelle vernichtet",
        correctAnswer = 1,
        explanation = "Im November 1835 landeten ca. 900 Maori (Ngāti Mutunga und Ngāti Tama) von europäischen Handelsschiffen auf den Chathams und beanspruchten die Inseln. Die ca. 2.000 Moriori hielten eine Versammlung ab und beschlossen pazifistischen Widerstand gemäß ihrem Gesetz Nunuku-whenua. Die Maori töteten, kochten und aßen viele Moriori und versklavten den Rest. Bis 1862 waren die meisten Moriori tot.",
        difficulty = 5,
        funFact = "Jared Diamond nutzte das Moriori-Massaker in 'Guns, Germs and Steel' als Fallstudie: Zwei verwandte Gesellschaften entwickelten sich auf verschiedenen Inseln völlig unterschiedlich. Die Maori auf dem kriegsreichen Neuseeland wurden kriegerisch; die Moriori auf den ressourcenarmen Chathams entwickelten Pazifismus als Überlebensstrategie — was funktionierte, bis die Maori ankamen."
    ),

    Question(
        categoryId = 11,
        questionText = "Was ist das sogenannte 'Schwarze Ritter'-Phenomenon in der modernen Astronomie — bekannt seit 1960?",
        answerA = "Ein regelmäßig beobachteter Radiopuls von einer unbekannten Quelle nahe dem Nordpol",
        answerB = "Ein unidentifiziertes Objekt in polarer Erdumlaufbahn, das seit ca. 1954 beobachtet wird und keine bekannte Herkunft hat",
        answerC = "Ein periodisch auftretender Gammastrahlenburst, der von SETI als mögliches Signal klassifiziert wurde",
        answerD = "Eine Ansammlung von Weltraumschrott, die durch ihre Größe und Zusammensetzung Wissenschaftler verwirrt",
        correctAnswer = 1,
        explanation = "Das 'Black Knight Satellite' ist ein (nicht offiziell benanntes) Phänomen: Seit den 1960ern berichten verschiedene Quellen von einem dunklen Objekt in polarer Erdumlaufbahn, das kein bekanntes menschliches Objekt sein soll. Nikola Tesla berichtete 1899 von unbekannten Radiosignalen; 1954 schrieb TIME Magazine über unidentifizierte Satelliten. Die wissenschaftliche Erklärung: Es handelt sich um Weltraumschrott von Apollo-Missionen.",
        difficulty = 5,
        funFact = "NASA-Fotos von 1998 (STS-88) zeigen ein dunkles Objekt im Orbit, das als 'Black Knight' viral ging. NASA erklärte es als eine verlorene Thermaldecke. Die Black-Knight-Mythos ist ein klassisches Beispiel für 'Pattern Recognition' — das menschliche Gehirn erkennt in zufälligen Objekten bedeutungsvolle Muster, besonders wenn es etwas Unerklärtes sehen möchte."
    ),

    Question(
        categoryId = 11,
        questionText = "Die 'Bierhallenputsch'-Führerin Hanna Reitsch war eine der wenigen Frauen, die im zweiten Weltkrieg militärische Auszeichnungen erhielt. Was war ihre bemerkenswerteste Leistung?",
        answerA = "Sie flog als einzige Frau einen Kampfeinsatz auf der Westfront 1944",
        answerB = "Sie testete das Me 163 Komet-Strahlflugzeug und war Cheftestpilotin für Prototypen",
        answerC = "Sie flog im April 1945 in das belagerte Berlin ein und wieder heraus",
        answerD = "Sie entwickelte das Kamikaze-Konzept für Deutschland als 'Selbstopfer-Staffeln'",
        correctAnswer = 2,
        explanation = "Hanna Reitsch, Deutschlands prominenteste Testpilotin, flog am 26. April 1945 zusammen mit General Karl Koller in das von sowjetischen Truppen eingeschlossene Berlin hinein — auf einem improvisierten Rollfeld im Tiergarten. Sie traf Hitler im Führerbunker und flog am 28. April wieder heraus. Es war eine der riskantesten Flugmanöver des Krieges.",
        difficulty = 5,
        funFact = "Reitsch war auch an frühen Konzepten für bemannte V1-Flugbomben ('Reichenberg') beteiligt, die sie selbst testflog. Nach dem Krieg wurde sie für Abenteuer-Segelflug bekannt, flog Anfang der 1970er in Afghanistan und Ghana und starb 1979 bei einem Segelflugwettbewerb in Frankfurt im Alter von 67 Jahren."
    ),

    Question(
        categoryId = 11,
        questionText = "Was war der 'Kellogg-Briand-Pakt' von 1928 und warum gilt er als wirkungslosestes internationales Abkommen der Geschichte?",
        answerA = "Ein Handelsabkommen, das Zollschranken zwischen 62 Nationen senken sollte — aber nie ratifiziert wurde",
        answerB = "Ein Vertrag, in dem 62 Nationen Krieg als Mittel der nationalen Politik 'verurteilen' und 'verzichten' — ohne Durchsetzungsmechanismus",
        answerC = "Ein Abrüstungsvertrag, der Marineschiffe auf 100.000 Tonnen begrenzte — aber keine Kontrollen vorsah",
        answerD = "Ein Friedensvertrag zwischen Deutschland und den Alliierten, der Reparationen neu regelte",
        correctAnswer = 1,
        explanation = "Der Kellogg-Briand-Pakt (offiziell: Briand-Kellogg-Pakt) wurde am 27. August 1928 von 15 Nationen unterzeichnet (später 62 Staaten) und erklärte Krieg als 'Mittel der nationalen Politik' für illegal. Er enthielt absolut keine Durchsetzungsmechanismen, keine Sanktionen und keinen Schiedsgerichtsmechanismus. Innerhalb von 14 Jahren nach Unterzeichnung hatte Japan Mandschurei angegriffen, Deutschland Europa überfallen und nahezu alle Unterzeichner Krieg geführt.",
        difficulty = 5,
        funFact = "Der Pakt ist formal noch in Kraft und Teil des US-Bundesrechts. Rechtswissenschaftler wie Oona Hathaway und Scott Shapiro argumentierten 2017, der Pakt habe tatsächlich die Häufigkeit von Territorialeroberungen durch Krieg deutlich reduziert — nicht durch Sanktionen, sondern durch den globalen Normenshift gegen Annexion."
    ),

    Question(
        categoryId = 11,
        questionText = "Was war das 'Beefsteak-Hitler' Phänomen innerhalb der frühen NSDAP?",
        answerA = "Die Partei verkaufte in München Würstchen als Parteipropaganda",
        answerB = "Beifsteak-Nazis waren ehemalige Kommunisten, die zur NSDAP wechselten: Außen braun, innen rot",
        answerC = "SA-Mitglieder erhielten Rindfleisch als Sonderration für besondere Treue",
        answerD = "Hitler verbot Vegetarismus in der Partei und ließ Fleischessen als Männlichkeitssymbol zelebrieren",
        correctAnswer = 1,
        explanation = "Als 'Beefsteak-Nazis' bezeichnete man spöttisch Parteimitglieder, die aus der KPD oder SPD zur NSDAP wechselten — 'außen braun (Nazi), innen rot (kommunistisch)'. Besonders in Berlin und im Ruhrgebiet gab es hohe Überläuferraten. Die SA unter Ernst Röhm rekrutierte bewusst aus den Reihen linker Arbeiterbewegungen und versprach eine 'nationale Revolution' mit sozialistischen Elementen.",
        difficulty = 5,
        funFact = "Die Überläufer aus der Linken brachten organisatorische Erfahrung und Straßenkampftechniken mit, die der frühen NSDAP sehr nützten. Historiker wie Richard Evans dokumentieren, wie fließend die ideologischen Grenzen in der Weimarer Republik waren — und wie viele 'Nazis' politisch keine konsequenten Überzeugungen, sondern Opportunismus antrieb."
    ),

    Question(
        categoryId = 11,
        questionText = "Was ist der 'Tsar-Bomba' und welchen Rekord hält sie bis heute?",
        answerA = "Die größte je detonierte Atomwaffe: 58 Megatonnen, 3.000 Mal stärker als Hiroshima, gezündet 1961",
        answerB = "Die erste sowjetische Wasserstoffbombe mit 25 Megatonnen, 1953 gezündet",
        answerC = "Erste Rakete, die 1957 einen Satelliten in die Erdumlaufbahn brachte",
        answerD = "Größte konventionelle Bombe der Geschichte mit 44 Tonnen TNT-Äquivalent",
        correctAnswer = 0,
        explanation = "Die Zar-Bombe (sowjetisch: Царь-бомба, offiziell AN602) wurde am 30. Oktober 1961 über Nowaja Semlja gezündet. Mit 58 Megatonnen war sie ca. 3.000 Mal stärker als die Hiroshima-Bombe und ca. 10 Mal stärker als alle im Zweiten Weltkrieg verschossene Munition zusammen. Der Feuerball hatte 8 km Durchmesser; der Druckwellenimpuls umrundete die Erde dreimal.",
        difficulty = 5,
        funFact = "Die Bombe war ursprünglich für 100 Megatonnen ausgelegt, wurde aber halbiert, damit das Trägerflugzeug (Tu-95) und die Besatzung eine Überlebenschance hatten. Selbst auf halber Stärke war die Pilsachwelle so gewaltig, dass das Trägerflugzeug einen Fallschirm mit 800 kg benötigte, um Zeit zum Wegfliegen zu haben."
    ),

    Question(
        categoryId = 11,
        questionText = "Was war das 'Sultanat Sulu'-Paradox, das 2013 eine internationale Krise in Malaysia auslöste?",
        answerA = "Der Sultan von Sulu (Philippinen) schickte Bewaffnete nach Sabah (Malaysia) und beanspruchte es als historisches Sultansgebiet",
        answerB = "Das Sultanat Sulu verlangte rückwirkende Zahlungen von Malaysia für den 1878 vereinbarten Pachtvertrag",
        answerC = "Philippinische Fischer zitierten alte Sultanatstraktate, um in malaysischen Gewässern fischen zu dürfen",
        answerD = "Ein europäisches Gericht verurteilte Malaysia 2022, dem Sultanat Sulu Milliardenentschädigungen zu zahlen",
        correctAnswer = 3,
        explanation = "2022 verurteilte ein spanischer Schiedsrichter Malaysia, den Erben des Sultanats Sulu 14,9 Milliarden USD zu zahlen — basierend auf einem Pachtvertrag von 1878, in dem das Sultanat den britischen Kolonisten Sabah für 5.000 Ringgit jährlich überließ. Malaysia zahlte Jahrzehnte brav die Jahresmiete (ca. 1.500 USD), weigerte sich aber zu zahlen. Der Schiedsspruch ist formal in mehreren EU-Ländern vollstreckbar.",
        difficulty = 5,
        funFact = "Malaysia hat in Frankreich und Luxemburg Vermögenswerte einfrieren lassen, um die Vollstreckung zu verhindern. Die Erben des Sultanats Sulu ließen sich in Madrid nieder und kämpfen juristisch weiter. Der Fall zeigt, wie koloniale Verträge des 19. Jahrhunderts im 21. Jahrhundert noch aktiv sein können — mit milliardenschweren Konsequenzen."
    ),

)
