package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun historyQuestionsMedium(): List<Question> = listOf(

    // Question 1 - Roman Empire
    Question(
        categoryId = 3,
        questionText = "Welcher römische Kaiser ließ das Kolosseum in Rom erbauen?",
        answerA = "Augustus",
        answerB = "Nero",
        answerC = "Vespasian",
        answerD = "Hadrian",
        correctAnswer = 2, // C
        explanation = "Kaiser Vespasian begann 72 n. Chr. mit dem Bau des Kolosseums. Sein Sohn Titus vollendete es 80 n. Chr. Das Amphitheater fasste etwa 50.000–80.000 Zuschauer.",
        funFact = "Der ursprüngliche Name des Kolosseums war 'Amphitheatrum Flavium', benannt nach der flavischen Kaiserdynastie."
    ),

    // Question 2 - Roman Empire
    Question(
        categoryId = 3,
        questionText = "In welchem Jahr überquerte Julius Caesar den Rubikon und löste damit den römischen Bürgerkrieg aus?",
        answerA = "63 v. Chr.",
        answerB = "49 v. Chr.",
        answerC = "44 v. Chr.",
        answerD = "31 v. Chr.",
        correctAnswer = 1, // B
        explanation = "Im Januar 49 v. Chr. überquerte Caesar mit seinen Truppen den Rubikon, einen kleinen Fluss, der als Grenze zwischen der Provinz Cisalpina und Italien galt. Dies war ein Akt des Hochverrats.",
        funFact = "Der Ausdruck 'den Rubikon überschreiten' bedeutet heute, eine unwiderrufliche Entscheidung zu treffen."
    ),

    // Question 3 - Roman Empire
    Question(
        categoryId = 3,
        questionText = "Welche germanischen Stämme besiegten unter Arminius das römische Heer in der Varusschlacht (9 n. Chr.)?",
        answerA = "Hunnen und Goten",
        answerB = "Vandalen und Burgunden",
        answerC = "Franken und Alemannen",
        answerD = "Cherusker und Verbündete",
        correctAnswer = 3, // D
        explanation = "Arminius, Führer der Cherusker, lockte drei römische Legionen unter Publius Quinctilius Varus in einen Hinterhalt im Teutoburger Wald. Alle drei Legionen wurden vernichtet – über 15.000 Soldaten fielen.",
        funFact = "Kaiser Augustus soll nach der Niederlage verzweifelt gerufen haben: 'Varus, gib mir meine Legionen zurück!'"
    ),

    // Question 4 - Roman Empire
    Question(
        categoryId = 3,
        questionText = "Welcher Kaiser teilte das Römische Reich 395 n. Chr. endgültig in ein West- und Ostreich auf?",
        answerA = "Konstantin I.",
        answerB = "Diokletian",
        answerC = "Theodosius I.",
        answerD = "Gratian",
        correctAnswer = 2, // C
        explanation = "Als Kaiser Theodosius I. im Jahr 395 n. Chr. starb, teilte er das Reich unter seinen beiden Söhnen auf: Honorius erhielt das Westreich, Arcadius das Ostreich. Diese Teilung war endgültig.",
        funFact = "Das Oströmische Reich (Byzantinisches Reich) überlebte das Weströmische Reich um fast 1.000 Jahre – bis zur Eroberung Konstantinopels 1453."
    ),

    // Question 5 - Roman Empire (Decline)
    Question(
        categoryId = 3,
        questionText = "Welcher Anführer der Westgoten plünderte 410 n. Chr. die Stadt Rom?",
        answerA = "Attila",
        answerB = "Alarich",
        answerC = "Odoaker",
        answerD = "Geiserich",
        correctAnswer = 1, // B
        explanation = "Alarich I., König der Westgoten, plünderte 410 n. Chr. drei Tage lang Rom. Dies war das erste Mal seit 800 Jahren, dass eine fremde Macht die Hauptstadt eingenommen hatte. Das Ereignis erschütterte die antike Welt zutiefst.",
        funFact = "Alarich starb kurz nach der Plünderung Roms. Er soll in einem umgeleiteten Flussbett mit vielen Schätzen begraben worden sein – sein Grab wurde nie gefunden."
    ),

    // Question 6 - Ancient Greek Philosophy
    Question(
        categoryId = 3,
        questionText = "Wer war der Lehrer von Aristoteles?",
        answerA = "Sokrates",
        answerB = "Platon",
        answerC = "Thales von Milet",
        answerD = "Pythagoras",
        correctAnswer = 1, // B
        explanation = "Aristoteles war 20 Jahre lang Schüler an Platons Akademie in Athen. Platon selbst war Schüler des Sokrates. Diese Kette bildet die drei bedeutendsten Philosophen der Antike.",
        funFact = "Aristoteles wurde später der Lehrer Alexanders des Großen, der sein Schüler von etwa 343 bis 340 v. Chr. war."
    ),

    // Question 7 - Ancient Greek Philosophy
    Question(
        categoryId = 3,
        questionText = "Welche Staatsform empfahl Platon in seinem Werk 'Der Staat' als ideale Regierungsform?",
        answerA = "Demokratie",
        answerB = "Oligarchie",
        answerC = "Philosophenherrschaft",
        answerD = "Monarchie",
        correctAnswer = 2, // C
        explanation = "In seinem Werk 'Politeia' (Der Staat) argumentierte Platon, dass der ideale Staat von Philosophen-Königen regiert werden sollte, da nur sie durch ihre Weisheit das Gemeinwohl erkennen können.",
        funFact = "Platon versuchte tatsächlich, seine politischen Ideen in Syrakus (Sizilien) umzusetzen, scheiterte aber und wurde Berichten zufolge als Sklave verkauft."
    ),

    // Question 8 - Peloponnesian War
    Question(
        categoryId = 3,
        questionText = "Welche beiden Stadtstaaten standen im Mittelpunkt des Peloponnesischen Krieges (431–404 v. Chr.)?",
        answerA = "Athen und Korinth",
        answerB = "Sparta und Theben",
        answerC = "Athen und Sparta",
        answerD = "Mykene und Troja",
        correctAnswer = 2, // C
        explanation = "Der Peloponnesische Krieg war ein fast 30 Jahre andauernder Konflikt zwischen dem Athener Seebund und dem Peloponnesischen Bund unter Spartas Führung. Er endete mit der Niederlage Athens.",
        funFact = "Der griechische Historiker Thukydides, selbst athenischer Feldherr, schrieb einen detaillierten Bericht über den Krieg – sein Werk gilt als erstes wissenschaftliches Geschichtswerk der Welt."
    ),

    // Question 9 - Battle of Thermopylae
    Question(
        categoryId = 3,
        questionText = "Wie viele spartanische Krieger hielten laut Überlieferung den Pass von Thermopylae 480 v. Chr. gegen die Perser?",
        answerA = "100",
        answerB = "300",
        answerC = "500",
        answerD = "1.000",
        correctAnswer = 1, // B
        explanation = "König Leonidas I. von Sparta hielt mit 300 Spartanern (und weiteren griechischen Verbündeten) den Engpass von Thermopylae gegen das riesige persische Heer des Xerxes. Sie kämpften bis zum letzten Mann.",
        funFact = "Die Inschrift auf dem Gedenkstein lautete: 'Wanderer, kommst du nach Sparta, verkündige dorten, du habest uns hier liegen gesehn, wie das Gesetz es befahl.'"
    ),

    // Question 10 - Ancient Greek Wars
    Question(
        categoryId = 3,
        questionText = "In welcher Seeschlacht (480 v. Chr.) besiegten die Griechen die persische Flotte und wendeten die Invasion ab?",
        answerA = "Marathon",
        answerB = "Salamis",
        answerC = "Platäa",
        answerD = "Artemision",
        correctAnswer = 1, // B
        explanation = "In der Seeschlacht bei Salamis lockte der athenische Stratege Themistokles die persische Flotte in enge Meeresstraßen und vernichtete sie. Xerxes soll das Desaster von einem Thron auf einem Hügel beobachtet haben.",
        funFact = "Xerxes soll nach der Niederlage so bestürzt gewesen sein, dass er noch vom Schlachtfeld aus die Rückreise nach Persien antrat."
    ),

    // Question 11 - Medieval Kingdoms
    Question(
        categoryId = 3,
        questionText = "Welches Dokument schränkte 1215 die Macht des englischen Königs erstmals rechtlich ein?",
        answerA = "Petition of Right",
        answerB = "Habeas Corpus Act",
        answerC = "Magna Carta",
        answerD = "Bill of Rights",
        correctAnswer = 2, // C
        explanation = "Die Magna Carta ('Großer Freibrief') wurde König Johann Ohneland von englischen Baronen abgezwungen. Sie enthielt erstmals schriftlich verankerte Rechte der Untertanen gegenüber dem König.",
        funFact = "Von den ursprünglichen Bestimmungen der Magna Carta sind in England heute noch genau drei in Kraft – darunter das Recht auf ein faires Gerichtsverfahren."
    ),

    // Question 12 - Medieval Kingdoms
    Question(
        categoryId = 3,
        questionText = "Wer wurde 800 n. Chr. in Rom von Papst Leo III. zum Kaiser des Abendlandes gekrönt?",
        answerA = "Otto I.",
        answerB = "Pippin der Kurze",
        answerC = "Karl der Große",
        answerD = "Ludwig der Fromme",
        correctAnswer = 2, // C
        explanation = "Karl der Große (Carolus Magnus), König der Franken, wurde am Weihnachtstag 800 n. Chr. in der Peterskirche in Rom zum Kaiser des Abendlandes gekrönt. Dies begründete das Heilige Römische Reich.",
        funFact = "Karl der Große konnte Berichten zufolge nie richtig schreiben lernen, obwohl er selbst die Bildung stark förderte und Schulen im ganzen Reich gründete."
    ),

    // Question 13 - Medieval Feudalism
    Question(
        categoryId = 3,
        questionText = "Wie nennt man den mittelalterlichen Vertrag zwischen einem Lehnsherrn und seinem Vasallen?",
        answerA = "Investitur",
        answerB = "Kommendat",
        answerC = "Lehnseid",
        answerD = "Zensualität",
        correctAnswer = 2, // C
        explanation = "Im Lehnssystem (Feudalismus) gelobte ein Vasall seinem Lehnsherrn Treue und Dienst (besonders Heeresdienst) im Lehnseid. Dafür erhielt er ein Lehen – meist Land – zur Nutzung.",
        funFact = "Der Lehnseid wurde durch einen symbolischen Akt vollzogen: Der Vasall kniete vor dem Herrn, legte seine Hände in dessen Hände und schwor seinen Eid."
    ),

    // Question 14 - Medieval Kingdoms
    Question(
        categoryId = 3,
        questionText = "Welche mittelalterliche Kaiserin regierte das Heilige Römische Reich als Regentin und kämpfte im österreichischen Erbfolgestreit?",
        answerA = "Kaiserin Theophanu",
        answerB = "Maria Theresia",
        answerC = "Kaiserin Agnes",
        answerD = "Eleonore von Aquitanien",
        correctAnswer = 1, // B
        explanation = "Maria Theresia (1717–1780) bestieg 1740 den österreichischen Thron und kämpfte im Österreichischen Erbfolgekrieg gegen Preußen und andere Mächte. Sie gilt als eine der bedeutendsten Herrscherinnen der europäischen Geschichte.",
        funFact = "Maria Theresia war Mutter von 16 Kindern, darunter Marie Antoinette, die spätere Königin Frankreichs."
    ),

    // Question 15 - Hanseatic League
    Question(
        categoryId = 3,
        questionText = "In welchem Jahr wurde die Hanse offiziell gegründet?",
        answerA = "1158",
        answerB = "1241",
        answerC = "1358",
        answerD = "1420",
        correctAnswer = 2, // C
        explanation = "Als offizielles Gründungsjahr der Hanse gilt 1358, als sich verschiedene nordeuropäische Handelsstädte formell zu einem Bündnis zusammenschlossen. Die Wurzeln reichen aber bis 1241 zurück, als Lübeck und Hamburg ein erstes Abkommen schlossen.",
        funFact = "Auf dem Höhepunkt ihrer Macht umfasste die Hanse über 200 Städte von London bis Nowgorod – ein riesiges Handelsnetz im mittelalterlichen Europa."
    ),

    // Question 16 - Hanseatic League
    Question(
        categoryId = 3,
        questionText = "Welche Stadt war die führende Metropole und 'Königin der Hanse'?",
        answerA = "Hamburg",
        answerB = "Bremen",
        answerC = "Lübeck",
        answerD = "Rostock",
        correctAnswer = 2, // C
        explanation = "Lübeck war die Hauptstadt und führende Stadt der Hanse. Hier tagten die Hansetage, und die Stadt kontrollierte den Handel zwischen Ost- und Nordsee über die Lübecker Bucht.",
        funFact = "Die Hanse hatte in verschiedenen Städten 'Kontore' – exklusive Handelsposten. Die bekanntesten waren in London (Stalhof), Brügge, Bergen und Nowgorod."
    ),

    // Question 17 - Hanseatic League
    Question(
        categoryId = 3,
        questionText = "Welches begehrte Handelsgut aus Skandinavien war für die Hanse besonders wichtig und machte Lübeck reich?",
        answerA = "Bernstein",
        answerB = "Pelze",
        answerC = "Hering",
        answerD = "Holz",
        correctAnswer = 2, // C
        explanation = "Der Skagener Hering war eines der wichtigsten Handelsgüter der Hanse. Der Fisch wurde gesalzen und konserviert und war ein Grundnahrungsmittel im mittelalterlichen Europa, besonders an Fastentagen.",
        funFact = "Für die Hanse war Salz genauso wertvoll wie Hering – ohne Salz konnte man keinen Hering konservieren. Daher wurde auch der Salzhandel stark kontrolliert."
    ),

    // Question 18 - Thirty Years' War
    Question(
        categoryId = 3,
        questionText = "In welchem Jahr begann der Dreißigjährige Krieg, und durch welches Ereignis?",
        answerA = "1618, Prager Fenstersturz",
        answerB = "1620, Bataille am Weißen Berg",
        answerC = "1618, Magdeburger Hochzeit",
        answerD = "1625, dänische Intervention",
        correctAnswer = 0, // A
        explanation = "Der Dreißigjährige Krieg begann 1618 mit dem Zweiten Prager Fenstersturz, bei dem protestantische böhmische Adlige drei kaiserliche Statthalter aus dem Fenster der Prager Burg warfen. Alle drei überlebten, aber der Krieg war entfacht.",
        funFact = "Die Überlebenden des Fenstersturzes landeten im Burggraben, vermutlich auf einem Misthaufen. Die Katholiken sahen es als göttliches Wunder; die Protestanten erklärten es durch den Mist."
    ),

    // Question 19 - Thirty Years' War
    Question(
        categoryId = 3,
        questionText = "Welcher Friedensvertrag beendete 1648 den Dreißigjährigen Krieg?",
        answerA = "Friede von Augsburg",
        answerB = "Friede von Westfalen",
        answerC = "Friede von Utrecht",
        answerD = "Friede von Prag",
        correctAnswer = 1, // B
        explanation = "Der Westfälische Friede (1648), geschlossen in Osnabrück und Münster, beendete den Dreißigjährigen Krieg. Er gilt als Grundlage des modernen Staatensystems, da er die Souveränität der Einzelstaaten anerkannte.",
        funFact = "Über fünf Jahre dauerten die Verhandlungen zum Westfälischen Frieden. Es war einer der ersten modernen Friedenskongresse in der Geschichte."
    ),

    // Question 20 - Thirty Years' War
    Question(
        categoryId = 3,
        questionText = "Welche schwedische Militärfigur galt als 'Löwe des Nordens' und fiel 1632 in der Schlacht bei Lützen?",
        answerA = "Karl XII.",
        answerB = "Erik XIV.",
        answerC = "Gustav II. Adolf",
        answerD = "Johan Banér",
        correctAnswer = 2, // C
        explanation = "Gustav II. Adolf von Schweden trat 1630 in den Dreißigjährigen Krieg ein und errang zunächst große Erfolge für die protestantische Seite. Er fiel in der siegreichen, aber verlustreichen Schlacht bei Lützen am 16. November 1632.",
        funFact = "Gustav Adolf galt als militärisches Genie. Er revolutionierte die Kriegsführung durch den kombinierten Einsatz von Infanterie, Kavallerie und Artillerie – ein Konzept, das bis Napoleon Bestand hatte."
    ),

    // Question 21 - Ottoman Expansion
    Question(
        categoryId = 3,
        questionText = "In welchem Jahr eroberten die Osmanen Konstantinopel und beendeten damit das Byzantinische Reich?",
        answerA = "1389",
        answerB = "1421",
        answerC = "1453",
        answerD = "1492",
        correctAnswer = 2, // C
        explanation = "Am 29. Mai 1453 eroberte Sultan Mehmed II. ('der Eroberer') Konstantinopel nach einem 53-tägigen Belagerung. Der letzte byzantinische Kaiser Konstantin XI. fiel im Kampf. Damit endete das über 1.000 Jahre bestehende Byzantinische Reich.",
        funFact = "Um schwere Kriegsschiffe in das vom Kettenriegel gesicherte Goldene Horn zu bringen, ließ Mehmed II. über 70 Schiffe auf Holzschienen über Land gezogen – 13 Kilometer weit."
    ),

    // Question 22 - Ottoman Expansion
    Question(
        categoryId = 3,
        questionText = "Welche europäische Stadt wurde 1529 erstmals von osmanischen Truppen belagert, aber nicht eingenommen?",
        answerA = "Prag",
        answerB = "Budapest",
        answerC = "Wien",
        answerD = "Venedig",
        correctAnswer = 2, // C
        explanation = "Die erste Türkenbelagerung Wiens 1529 unter Süleyman dem Prächtigen scheiterte. Nach knapp drei Wochen Belagerung zog das osmanische Heer ab – wegen Versorgungsproblemen und frühem Wintereinbruch.",
        funFact = "Laut Überlieferung hinterließen die abziehenden Osmanen Kaffeesäcke in Wien. Ein polnischer Offizier soll daraufhin das erste Wiener Kaffeehaus eröffnet haben – der Beginn der Wiener Kaffeehauskultur."
    ),

    // Question 23 - Ottoman Expansion
    Question(
        categoryId = 3,
        questionText = "Welche Seeschlacht von 1571 stoppte die osmanische Expansion im westlichen Mittelmeer?",
        answerA = "Seeschlacht von Actium",
        answerB = "Seeschlacht von Lepanto",
        answerC = "Seeschlacht von Trafalgar",
        answerD = "Seeschlacht von Navarino",
        correctAnswer = 1, // B
        explanation = "Die Seeschlacht von Lepanto (7. Oktober 1571) war ein Sieg der Heiligen Liga (Spanien, Venedig, Papststaat) über die osmanische Flotte. Es war die letzte große Ruderschlacht der Geschichte und stoppte die osmanische Expansion im Mittelmeer.",
        funFact = "Miguel de Cervantes, der spätere Autor des 'Don Quijote', kämpfte als junger Soldat bei Lepanto und verlor dabei die Beweglichkeit seiner linken Hand."
    ),

    // Question 24 - Spanish Inquisition
    Question(
        categoryId = 3,
        questionText = "In welchem Jahr wurde die Spanische Inquisition offiziell eingerichtet?",
        answerA = "1391",
        answerB = "1478",
        answerC = "1492",
        answerD = "1517",
        correctAnswer = 1, // B
        explanation = "Papst Sixtus IV. erteilte 1478 den Königen Ferdinand II. von Aragon und Isabella I. von Kastilien die Genehmigung zur Einrichtung der Spanischen Inquisition. Sie richtete sich zunächst vor allem gegen konvertierte Juden ('Conversos').",
        funFact = "Der erste Großinquisitor Spaniens war Tomás de Torquemada. Unter seiner Leitung wurden bis zu 2.000 Menschen hingerichtet und tausende gefoltert."
    ),

    // Question 25 - Spanish Inquisition
    Question(
        categoryId = 3,
        questionText = "Welche religiösen Minderheiten wurden 1492 aus Spanien vertrieben?",
        answerA = "Protestanten und Waldenser",
        answerB = "Muslime (Mauren) und Katharer",
        answerC = "Juden und Muslime",
        answerD = "Juden und Hugenotten",
        correctAnswer = 2, // C
        explanation = "1492 war für Spanien ein doppelt wichtiges Jahr: Kolumbus entdeckte Amerika, und mit dem Alhambra-Edikt wurden alle Juden ausgewiesen, die sich nicht zum Christentum bekehrten. Muslime folgten 1502 (und endgültig 1609).",
        funFact = "Man schätzt, dass 1492 zwischen 100.000 und 200.000 Juden Spanien verließen. Viele flohen ins Osmanische Reich, wo Sultan Bayezid II. sie willkommen hieß."
    ),

    // Question 26 - Age of Absolutism - Louis XIV
    Question(
        categoryId = 3,
        questionText = "Welchen berühmten Ausspruch soll Ludwig XIV. über sich und den Staat gemacht haben?",
        answerA = "'Der König stirbt, aber das Königreich lebt.'",
        answerB = "'L'État, c'est moi.' (Der Staat bin ich.)",
        answerC = "'Gott hat mich zum König gemacht.'",
        answerD = "'Nach mir die Sintflut.'",
        correctAnswer = 1, // B
        explanation = "Der Ausspruch 'L'État, c'est moi' ('Der Staat bin ich') wird Ludwig XIV. zugeschrieben und symbolisiert den absoluten Herrschaftsanspruch des französischen Sonnenkönigs, der von 1643 bis 1715 regierte.",
        funFact = "Ludwig XIV. war mit 72 Jahren Regierungszeit der am längsten amtierende europäische Monarch der Geschichte – länger als jeder andere Herrscher Europas."
    ),

    // Question 27 - Age of Absolutism - Louis XIV
    Question(
        categoryId = 3,
        questionText = "Welches prächtige Schloss ließ Ludwig XIV. als Symbol seiner absoluten Macht erbauen?",
        answerA = "Schönbrunn in Wien",
        answerB = "Fontainebleau",
        answerC = "Versailles",
        answerD = "Louvre",
        correctAnswer = 2, // C
        explanation = "Ludwig XIV. ließ ab 1661 das Schloss Versailles zum größten Palast Europas ausbauen. Es wurde zur Hauptresidenz und zum politischen Zentrum Frankreichs – ein Symbol des absoluten Herrschaftsanspruchs.",
        funFact = "Im Schloss Versailles lebten und arbeiteten zeitweise über 20.000 Menschen, darunter 6.000 Adlige, die der König durch Hofzeremonien unter seiner Kontrolle hielt."
    ),

    // Question 28 - Age of Absolutism - Frederick the Great
    Question(
        categoryId = 3,
        questionText = "Welchen Krieg führte Friedrich der Große, um Schlesien von Österreich zu erobern?",
        answerA = "Siebenjähriger Krieg",
        answerB = "Österreichischer Erbfolgekrieg",
        answerC = "Nordischer Krieg",
        answerD = "Polnischer Erbfolgekrieg",
        correctAnswer = 1, // B
        explanation = "Im Österreichischen Erbfolgekrieg (1740–1748) fiel Friedrich II. kurz nach Antritt seiner Regierung in das zu Österreich gehörende Schlesien ein. Er behielt die wohlhabende Provinz – ein riesiger Gewinn für Preußen.",
        funFact = "Friedrich der Große trat diesen Krieg an, ohne sein Kabinett zu konsultieren. Er soll seinen Ministern erst nach dem Einmarsch davon erzählt haben."
    ),

    // Question 29 - Age of Absolutism - Frederick the Great
    Question(
        categoryId = 3,
        questionText = "Welche Philosophen der Aufklärung standen mit Friedrich dem Großen in engem persönlichem Austausch?",
        answerA = "Immanuel Kant und Jean-Paul Sartre",
        answerB = "Voltaire und d'Alembert",
        answerC = "John Locke und Thomas Hobbes",
        answerD = "Rousseau und Montesquieu",
        correctAnswer = 1, // B
        explanation = "Friedrich II. korrespondierte intensiv mit Voltaire, der von 1750 bis 1753 sogar am preußischen Hof in Sanssouci lebte. Auch mit d'Alembert, dem Mitherausgeber der Encyclopédie, pflegte er Briefwechsel.",
        funFact = "Friedrich der Große schrieb selbst philosophische Werke – auf Französisch, nicht auf Deutsch, das er für eine 'Bauernsprache' hielt."
    ),

    // Question 30 - Age of Absolutism
    Question(
        categoryId = 3,
        questionText = "Was versteht man unter dem Begriff 'aufgeklärter Absolutismus', den Herrscher wie Friedrich der Große verkörperten?",
        answerA = "Herrschaft durch göttliches Recht ohne jede Einschränkung",
        answerB = "Absolute Herrschaft verbunden mit Reformen im Geist der Aufklärung",
        answerC = "Demokratische Kontrolle des Monarchen durch ein Parlament",
        answerD = "Kirchliche Herrschaft über weltliche Fürsten",
        correctAnswer = 1, // B
        explanation = "Aufgeklärter Absolutismus bezeichnet Herrschaft, bei der absolute Monarchen Reformen im Sinne der Aufklärung durchführten (Religionstoleranz, Bildung, Justizreform), ohne ihre absolute Macht aufzugeben. Friedrich II. nannte sich 'ersten Diener des Staates'.",
        funFact = "Friedrich der Große schaffte in Preußen die Folter ab, gewährte Religionsfreiheit und reformierte das Rechtssystem – gilt aber trotzdem als autokratischer Militärherrscher."
    ),

    // Question 31 - Roman Empire Provinces
    Question(
        categoryId = 3,
        questionText = "Welche römische Provinz umfasste das heutige Deutschland westlich des Rheins?",
        answerA = "Pannonia",
        answerB = "Raetia",
        answerC = "Germania inferior und superior",
        answerD = "Moesia",
        correctAnswer = 2, // C
        explanation = "Die Römer teilten die linksrheinischen Gebiete in zwei Provinzen: Germania inferior (Niedergermanien, heute Niederlande/Nordrhein) und Germania superior (Obergermanien, heute Elsass/Südwestdeutschland). Der Rhein bildete die Grenze zum freien Germanien.",
        funFact = "Die Römer bauten den Limes, eine über 500 km lange Grenzbefestigung, die das Römische Reich von Germanien trennte. Teile davon sind heute UNESCO-Welterbe."
    ),

    // Question 32 - Roman Empire Emperors
    Question(
        categoryId = 3,
        questionText = "Welcher Kaiser erließ 313 n. Chr. das Mailänder Edikt, das die Christen im Römischen Reich tolerierte?",
        answerA = "Diokletian",
        answerB = "Trajan",
        answerC = "Konstantin I.",
        answerD = "Julian Apostata",
        correctAnswer = 2, // C
        explanation = "Das Edikt von Mailand (313 n. Chr.), erlassen von Konstantin I. und Licinius, gewährte allen Religionen im Römischen Reich Toleranz. Es war ein Wendepunkt für das Christentum, das damit aufhörte, verfolgt zu werden.",
        funFact = "Konstantin I. ließ sich erst auf dem Sterbebett taufen – eine Praxis, die damals nicht ungewöhnlich war, da man glaubte, die Taufe wasche alle Sünden ab."
    ),

    // Question 33 - Medieval Feudalism
    Question(
        categoryId = 3,
        questionText = "Welches mittelalterliche Konzept besagte, dass Papst und Kaiser gemeinsam die Welt regierten – geistlich und weltlich?",
        answerA = "Translatio imperii",
        answerB = "Zwei-Schwerter-Lehre",
        answerC = "Gottesfrieden",
        answerD = "Investiturstreit",
        correctAnswer = 1, // B
        explanation = "Die Zwei-Schwerter-Lehre besagte, dass Gott der Welt zwei 'Schwerter' gegeben hat: das geistliche (dem Papst) und das weltliche (dem Kaiser). Beide stehen im Dienst der Kirche – ein zentrales politisch-theologisches Konzept des Mittelalters.",
        funFact = "Der Investiturstreit (1076–1122) war der berühmteste Konflikt zwischen Papst und Kaiser um eben diese Machtfrage – ob der Papst oder der Kaiser Bischöfe einsetzen darf."
    ),

    // Question 34 - Thirty Years' War
    Question(
        categoryId = 3,
        questionText = "Wie viel der deutschen Bevölkerung starb schätzungsweise während des Dreißigjährigen Krieges durch Kampf, Hunger und Seuchen?",
        answerA = "Etwa 5–10 %",
        answerB = "Etwa 15–20 %",
        answerC = "Etwa 25–40 %",
        answerD = "Über 50 %",
        correctAnswer = 2, // C
        explanation = "Der Dreißigjährige Krieg war eine demografische Katastrophe für das Heilige Römische Reich. In manchen Regionen starben bis zu 50–60 % der Bevölkerung; im Durchschnitt wird ein Verlust von 25–40 % geschätzt.",
        funFact = "Manche Städte und Regionen Deutschlands erholten sich erst nach 100 Jahren wieder von den Bevölkerungsverlusten des Dreißigjährigen Krieges."
    ),

    // Question 35 - Ancient Greek Philosophy
    Question(
        categoryId = 3,
        questionText = "Was ist die sokratische Methode, auch 'Mäeutik' genannt?",
        answerA = "Eine Schreibmethode für philosophische Texte",
        answerB = "Ein Gesprächsverfahren, bei dem durch gezieltes Fragen Erkenntnisse herausgearbeitet werden",
        answerC = "Eine Meditationstechnik der Stoiker",
        answerD = "Die Kunst der öffentlichen Rede nach Rhetoriken-Prinzipien",
        correctAnswer = 1, // B
        explanation = "Sokrates entwickelte eine dialektische Gesprächsmethode: Er stellte so lange bohrende Fragen, bis sein Gegenüber Widersprüche in seinem Denken erkannte und zu tieferem Verständnis gelangte. Er verglich sich mit einer Hebamme, die Gedanken zur Welt bringt.",
        funFact = "Sokrates selbst schrieb nichts auf – all unser Wissen über ihn stammt aus den Schriften seiner Schüler, vor allem Platon und Xenophon."
    ),

    // Question 36 - Ottoman Expansion
    Question(
        categoryId = 3,
        questionText = "Welches osmanische Janitscharenkorps war und warum so gefürchtet?",
        answerA = "Reine Kavallerieeinheit aus türkischen Adeligen",
        answerB = "Eliteinfanterie, die aus christlichen Knaben zwangsrekrutiert wurde",
        answerC = "Artillerietruppe aus persischen Söldnern",
        answerD = "Leibwache des Sultans aus arabischen Stammesführern",
        correctAnswer = 1, // B
        explanation = "Die Janitscharen (türk. 'yeni çeri' = neue Truppe) wurden durch das Devshirme-System rekrutiert: Christliche Knaben aus den Balkanprovinzen wurden als Tribut abgeführt, zum Islam bekehrt und zu Elitesoldaten ausgebildet.",
        funFact = "Die Janitscharen wurden so mächtig, dass sie selbst Sultane einsetzen und absetzen konnten. Sie wurden 1826 von Sultan Mahmud II. in einem Massaker aufgelöst – dem 'Glücklichen Vorfall'."
    ),

    // Question 37 - Hanseatic League
    Question(
        categoryId = 3,
        questionText = "Welches rechtliche Prinzip der Hanse ermöglichte hansischen Kaufleuten in fremden Städten besondere Handelsprivilegien?",
        answerA = "Ius primae noctis",
        answerB = "Stapelrecht",
        answerC = "Hanserezess",
        answerD = "Ius mercatorum",
        correctAnswer = 1, // B
        explanation = "Das Stapelrecht verpflichtete durchreisende Kaufleute, ihre Waren in bestimmten Städten ('Stapelstädte') anzubieten, bevor sie weiterreisen durften. Dies sicherte den hansischen Handelszentren enorme wirtschaftliche Macht.",
        funFact = "Köln hatte ein weitgehendes Stapelrecht für den Rheinhandel. Das bedeutete, dass alle Waren, die den Rhein entlangkamen, zuerst in Köln angeboten werden mussten."
    ),

    // Question 38 - Roman Empire Decline
    Question(
        categoryId = 3,
        questionText = "Welcher Hunnenkönig fiel in das Weströmische Reich ein und wurde als 'Geißel Gottes' bezeichnet?",
        answerA = "Balamber",
        answerB = "Rugila",
        answerC = "Attila",
        answerD = "Bleda",
        correctAnswer = 2, // C
        explanation = "Attila (reg. 434–453) führte das Hunnenreich zu seiner größten Ausdehnung. Seine Einfälle in das Weströmische und Oströmische Reich verwüsteten weite Landstriche. Er starb jedoch 453 n. Chr., kurz nach seiner Hochzeit, an einem Blutsturz.",
        funFact = "Attila starb in der Hochzeitsnacht mit seiner Gemahlin Ildico – wahrscheinlich an einer Hirnblutung infolge von übermäßigem Alkoholkonsum. Er wurde heimlich in einem goldenen, dann silbernen, dann eisernen Sarg bestattet."
    ),

    // Question 39 - Spanish Inquisition
    Question(
        categoryId = 3,
        questionText = "Was war der 'Auto-da-Fé' im Rahmen der Spanischen Inquisition?",
        answerA = "Ein religiöser Feiertag zu Ehren des Heiligen Petrus",
        answerB = "Eine öffentliche Zeremonie mit Urteilsverkündung und Hinrichtung von Verurteilten",
        answerC = "Eine kirchliche Prüfung des Glaubenswissens",
        answerD = "Ein Bußritual für Angeklagte, die ihre Sünden bereuten",
        correctAnswer = 1, // B
        explanation = "Auto-da-Fé (portugiesisch: 'Akt des Glaubens') war eine öffentliche Zeremonie der Inquisition, bei der Urteile verlesen und vollstreckt wurden. Wer nicht widerrief, wurde auf dem Scheiterhaufen verbrannt. Solche Ereignisse zogen oft tausende Zuschauer an.",
        funFact = "Das berühmteste Auto-da-Fé fand 1680 in Madrid statt, bei dem König Karl II. und seine Gemahlin persönlich anwesend waren und über 100 Angeklagte verurteilt wurden."
    ),

    // Question 40 - Medieval Kingdoms
    Question(
        categoryId = 3,
        questionText = "Was war der 'Hundertjährige Krieg' (1337–1453) und zwischen welchen Reichen wurde er geführt?",
        answerA = "Zwischen dem Heiligen Römischen Reich und Ungarn um die Böhmische Krone",
        answerB = "Zwischen England und Frankreich um die französische Krone",
        answerC = "Zwischen Kastilien und Aragon um die Herrschaft über Spanien",
        answerD = "Zwischen dem Papsttum und Frankreich um die Kirchenhoheit",
        correctAnswer = 1, // B
        explanation = "Der Hundertjährige Krieg war eine Reihe von Konflikten zwischen England und Frankreich, bei denen englische Könige den Anspruch auf die französische Krone geltend machten. Berühmt wurde der Krieg durch Jeanne d'Arc, die Frankreich zur Wende verhalf.",
        funFact = "Jeanne d'Arc wurde 1431 von den Engländern als Ketzerin verbrannt. 25 Jahre später rehabilitierte sie die Kirche vollständig – und 1920 wurde sie heiliggesprochen."
    ),

    // Question 41 - Peloponnesian War
    Question(
        categoryId = 3,
        questionText = "Welche verheerende Epidemie traf Athen zu Beginn des Peloponnesischen Krieges (430 v. Chr.) und tötete ein Viertel der Bevölkerung?",
        answerA = "Schwarzer Tod (Pest)",
        answerB = "Typhus oder eine unbekannte Seuche",
        answerC = "Masern",
        answerD = "Cholera",
        correctAnswer = 1, // B
        explanation = "430 v. Chr. brach in Athen eine verheerende Epidemie aus, die etwa ein Viertel der Bevölkerung dahinraffte. Auch Perikles, der führende Staatsmann Athens, fiel ihr 429 v. Chr. zum Opfer. Die genaue Krankheit ist bis heute nicht eindeutig identifiziert.",
        funFact = "Der Historiker Thukydides erkrankte selbst an der Seuche, überlebte aber und beschrieb ihre Symptome so genau, dass Historiker noch heute versuchen, die Krankheit zu identifizieren."
    ),

    // Question 42 - Age of Absolutism
    Question(
        categoryId = 3,
        questionText = "Was war die 'Revokation des Edikts von Nantes' (1685) durch Ludwig XIV. und welche Folgen hatte sie?",
        answerA = "Ludwig XIV. gewährte den Protestanten neue Rechte",
        answerB = "Ludwig XIV. entzog den Hugenotten ihre Religionsfreiheit, was zur Massenauswanderung führte",
        answerC = "Ludwig XIV. erklärte den Katholizismus zur einzigen Staatsreligion Europas",
        answerD = "Ludwig XIV. einigte sich mit dem Papst auf eine neue Kirchenverfassung",
        correctAnswer = 1, // B
        explanation = "Das Edikt von Nantes (1598) hatte den französischen Protestanten (Hugenotten) Religionsfreiheit gewährt. Ludwig XIV. widerrief es 1685, was zur Verfolgung und Auswanderung von ca. 200.000–400.000 Hugenotten führte – darunter viele qualifizierte Handwerker und Kaufleute.",
        funFact = "Brandenburg-Preußen profitierte enorm: Friedrich Wilhelm, der 'Große Kurfürst', lud die vertriebenen Hugenotten ein. Bis zu 20.000 siedelten sich in Berlin an und trugen zum wirtschaftlichen Aufschwung Preußens bei."
    ),

    // Question 43 - Medieval Kingdoms
    Question(
        categoryId = 3,
        questionText = "Was war der 'Investiturstreit' (1076–1122) im Mittelalter?",
        answerA = "Ein Streit um die Erbfolge im Heiligen Römischen Reich",
        answerB = "Ein Konflikt zwischen Papst und Kaiser über das Recht, Bischöfe einzusetzen",
        answerC = "Ein Kreuzzugsstreit zwischen Frankreich und dem Byzantinischen Reich",
        answerD = "Ein Handelskrieg zwischen den Hansestädten und dem Kaiser",
        correctAnswer = 1, // B
        explanation = "Der Investiturstreit war ein fundamentaler Machtkampf zwischen Papst Gregor VII. und Kaiser Heinrich IV. um die Frage, wer das Recht hat, Bischöfe und Äbte einzusetzen ('Investitur'). Er endete mit dem Wormser Konkordat 1122.",
        funFact = "Heinrich IV. musste 1077 im Büßergewand drei Tage im Schnee vor der Burg Canossa warten, bis Papst Gregor VII. ihn empfing. Dieser 'Gang nach Canossa' ist zum Sprichwort für demütige Unterwerfung geworden."
    ),

    // Question 44 - Ottoman Expansion
    Question(
        categoryId = 3,
        questionText = "Welche osmanische Herrscherpersönlichkeit wird als 'Kanuni' (der Gesetzgeber) bezeichnet und brachte das Reich auf seinen Höhepunkt?",
        answerA = "Mehmed II.",
        answerB = "Selim I.",
        answerC = "Süleyman I.",
        answerD = "Murad II.",
        correctAnswer = 2, // C
        explanation = "Süleyman I. (reg. 1520–1566) brachte das Osmanische Reich auf seinen territorialen und kulturellen Höhepunkt. Im Westen bekannt als 'der Prächtige', nannten ihn seine Untertanen 'Kanuni' wegen seiner umfangreichen Gesetzgebung.",
        funFact = "Süleyman unterhielt eine geheime, aber innige Beziehung zu Hürrem Sultan (Roxelana), einer ukrainischen Sklavin, die er zur Hauptfrau erhob – ein beispielloser Bruch mit osmanischer Tradition."
    ),

    // Question 45 - Roman Empire
    Question(
        categoryId = 3,
        questionText = "Welches Ereignis markiert traditionell das Ende des Weströmischen Reiches?",
        answerA = "Die Plünderung Roms durch die Westgoten 410 n. Chr.",
        answerB = "Die Absetzung des letzten weströmischen Kaisers Romulus Augustulus 476 n. Chr.",
        answerC = "Der Tod des Kaisers Theodosius I. 395 n. Chr.",
        answerD = "Die Niederlage gegen die Hunnen in der Catalaunischen Ebene 451 n. Chr.",
        correctAnswer = 1, // B
        explanation = "476 n. Chr. setzte der germanische Heerführer Odoaker den letzten weströmischen Kaiser Romulus Augustulus ab. Dieses Datum gilt traditionell als Ende des Weströmischen Reiches und Beginn des Mittelalters.",
        funFact = "Der Name 'Romulus Augustulus' ('kleiner Augustus') wirkt wie eine Parodie: Der letzte Kaiser trug die Namen des Staatsgründers (Romulus) und des ersten Kaisers (Augustus) – und war doch kaum mehr als eine Marionette."
    ),

    // Question 46 - Hanseatic League
    Question(
        categoryId = 3,
        questionText = "Wann und warum löste sich die Hanse auf?",
        answerA = "1521, als Karl V. das Handelsmonopol der Hanse aufhob",
        answerB = "1669, da der letzte Hansetag keine Beschlussfähigkeit mehr erreichte",
        answerC = "1618, mit Ausbruch des Dreißigjährigen Krieges",
        answerD = "1598, als England und Holland die nordeuropäischen Handelswege übernahmen",
        correctAnswer = 1, // B
        explanation = "Der letzte offizielle Hansetag fand 1669 in Lübeck statt. Nur noch neun Städte waren vertreten; viele wichtige Handelsstädte waren längst ausgetreten. Die Hanse löste sich faktisch auf – ohne formellen Beschluss.",
        funFact = "Drei Städte – Lübeck, Hamburg und Bremen – bezeichnen sich noch heute als 'Freie und Hansestädte' und führen diesen Titel in ihren offiziellen Namen."
    ),

    // Question 47 - Age of Absolutism - Frederick the Great
    Question(
        categoryId = 3,
        questionText = "Was war Friedrichs des Großen Schloss Sanssouci, und was bedeutet der Name?",
        answerA = "Ein Jagdschloss bei Dresden; bedeutet 'ohne Sorgen'",
        answerB = "Ein Sommerpalais in Potsdam; bedeutet 'ohne Sorgen'",
        answerC = "Ein Stadtpalais in Berlin; bedeutet 'ohne König'",
        answerD = "Eine Festungsanlage; bedeutet 'ohne Feinde'",
        correctAnswer = 1, // B
        explanation = "Sanssouci ('ohne Sorgen' auf Französisch) ist ein Sommerschloss in Potsdam, das Friedrich der Große ab 1745 errichten ließ. Es war sein Lieblingsdomizil, wo er Musik, Philosophie und Wissenschaft pflegte.",
        funFact = "Friedrich der Große wünschte, ohne kirchliche Zeremonien auf der Terrasse von Sanssouci neben seinen Windhunden begraben zu werden. Diesen Wunsch erfüllte man ihm erst 1991 – 205 Jahre nach seinem Tod."
    ),

    // Question 48 - Ancient Greek Wars
    Question(
        categoryId = 3,
        questionText = "Welche Bedeutung hatte die Phalanx-Kampfformation für die griechischen Stadtstaaten?",
        answerA = "Eine Reitertaktik, die gegen persische Elefanten entwickelt wurde",
        answerB = "Eine dichte Infanterieformation mit Lanzen und Schilden, die koordiniertes Vorgehen erforderte",
        answerC = "Eine Seekampftaktik für Ruderschiffe im Mittelmeer",
        answerD = "Eine Belagerungsformation für befestigte Städte",
        correctAnswer = 1, // B
        explanation = "Die Phalanx war eine dichte Kampfformation schwerbewaffneter Fußsoldaten (Hopliten), die Schild an Schild in mehreren Reihen kämpften. Sie dominierte griechische Schlachten und erforderte hohes Training und Disziplin.",
        funFact = "Alexander der Große kombinierte die Phalanx mit wendiger Kavallerie zu einer revolutionären taktischen Einheit, die ihm Siege bis nach Indien ermöglichte."
    ),

    // Question 49 - Spanish Inquisition
    Question(
        categoryId = 3,
        questionText = "Welche Personengruppe stand unter dem Begriff 'Conversos' im Visier der Spanischen Inquisition?",
        answerA = "Protestanten, die zum Katholizismus konvertierten",
        answerB = "Zum Christentum konvertierte Juden, die verdächtigt wurden, heimlich jüdische Praktiken beizubehalten",
        answerC = "Muslime, die als Spione für das Osmanische Reich galten",
        answerD = "Heiden aus den amerikanischen Kolonien, die getauft wurden",
        correctAnswer = 1, // B
        explanation = "Conversos waren zum Christentum konvertierte Juden ('Neuchristen'). Die Inquisition verdächtigte viele von ihnen, heimlich ('im Verborgenen') weiterhin jüdische Bräuche zu pflegen – als sogenannte 'Marranen'.",
        funFact = "Der Begriff 'Limpieza de sangre' ('Reinheit des Blutes') wurde in Spanien zur offiziellen Doktrin: Selbst Urenkel von Conversos konnten von bestimmten Ämtern ausgeschlossen werden."
    ),

    // Question 50 - Medieval Feudalism
    Question(
        categoryId = 3,
        questionText = "Was war die Rolle der mittelalterlichen Zunft im städtischen Wirtschaftsleben?",
        answerA = "Eine staatliche Behörde zur Steuererhebung von Handwerkern",
        answerB = "Eine kirchliche Organisation für die spirituelle Betreuung von Handwerkern",
        answerC = "Eine Vereinigung von Handwerkern eines Berufszweigs zur Regulierung von Qualität, Preisen und Ausbildung",
        answerD = "Ein adeliges Gericht zur Schlichtung von Handelsstreitigkeiten",
        correctAnswer = 2, // C
        explanation = "Zünfte waren Berufsverbände, die das Handwerk in mittelalterlichen Städten kontrollierten. Sie regelten die Ausbildung (Lehrling → Geselle → Meister), setzten Qualitätsstandards und schützten Mitglieder vor Konkurrenz.",
        funFact = "Die Zunftordnung war streng hierarchisch: Ein Lehrling arbeitete jahrelang ohne Lohn, ein Geselle musste auf 'Wanderschaft' gehen, bevor er sein Meisterstück vorlegen durfte."
    ),

)
