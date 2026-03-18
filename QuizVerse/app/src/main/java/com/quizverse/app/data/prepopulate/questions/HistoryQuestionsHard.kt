package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun historyQuestionsHard(): List<Question> = listOf(

    // Question 1 - Sumerian Laws
    Question(
        categoryId = 3,
        questionText = "Welcher sumerische König erließ um 2100 v. Chr. das älteste bekannte Gesetzbuch der Welt?",
        answerA = "Hammurabi",
        answerB = "Ur-Namma",
        answerC = "Gilgamesch",
        answerD = "Sargon von Akkad",
        correctAnswer = 1, // B
        explanation = "König Ur-Namma von Ur erließ um 2100 v. Chr. den Codex Ur-Namma, das älteste bekannte schriftliche Gesetzbuch. Es enthielt Regelungen zu Strafen, Erbrecht und Handelsgeschäften und war etwa 300 Jahre älter als der Codex Hammurabi.",
        funFact = "Im Gegensatz zum späteren Hammurabi-Codex sah der Codex Ur-Namma für viele Vergehen Geldstrafen statt körperlicher Bestrafung vor."
    ),

    // Question 2 - Sumerian Laws
    Question(
        categoryId = 3,
        questionText = "Der Codex Hammurabi enthält den Grundsatz 'Auge um Auge, Zahn um Zahn'. Wie lautet dieser Rechtsgrundsatz auf Latein?",
        answerA = "Lex talionis",
        answerB = "Lex naturalis",
        answerC = "Habeas corpus",
        answerD = "Dura lex",
        correctAnswer = 0, // A
        explanation = "Lex talionis (lat. 'Gesetz der Vergeltung') beschreibt das Vergeltungsprinzip, wonach die Strafe dem Vergehen genau entsprechen soll. Im Codex Hammurabi (ca. 1754 v. Chr.) wurde dieses Prinzip je nach sozialer Stellung der Beteiligten jedoch unterschiedlich angewendet.",
        funFact = "Der Codex Hammurabi wurde auf einer fast zwei Meter hohen schwarzen Diorit-Stele eingemeißelt und ist heute im Louvre in Paris ausgestellt."
    ),

    // Question 3 - Egyptian Dynasties
    Question(
        categoryId = 3,
        questionText = "Welche ägyptische Pharaonin regierte im Neuen Reich als Mitregentin und ließ sich mit Bart und männlichem Herrschergewand darstellen?",
        answerA = "Nofretete",
        answerB = "Kleopatra VII.",
        answerC = "Hatschepsut",
        answerD = "Teje",
        correctAnswer = 2, // C
        explanation = "Hatschepsut (ca. 1479–1458 v. Chr.) aus der 18. Dynastie regierte als Pharaonin Ägyptens. Sie ließ sich als Mann mit Doppelkrone und dem rituellen Bart darstellen, um ihre Herrschaftslegitimität zu stärken. Ihr Nachfolger Thutmosis III. ließ viele ihrer Bildnisse nachträglich zerstören.",
        funFact = "Hatschepsut organisierte eine berühmte Handelsexpedition ins Land Punt, die in ihrem Totentempel in Deir el-Bahari detailliert dargestellt ist."
    ),

    // Question 4 - Egyptian Dynasties
    Question(
        categoryId = 3,
        questionText = "In welche ägyptische Dynastie fiel die Herrschaft des 'Ketzerpharaohs' Echnaton?",
        answerA = "12. Dynastie",
        answerB = "18. Dynastie",
        answerC = "19. Dynastie",
        answerD = "26. Dynastie",
        correctAnswer = 1, // B
        explanation = "Echnaton (Amenhotep IV., reg. ca. 1353–1336 v. Chr.) gehörte zur 18. Dynastie. Er führte den Monotheismus des Aton-Kultes ein, verlegte die Hauptstadt nach Amarna und brach mit dem traditionellen Priestertum. Nach seinem Tod wurde seine Reformation vollständig rückgängig gemacht.",
        funFact = "Echnatons Gemahlin Nofretete ist durch ihre berühmte Büste im Berliner Neuen Museum weltbekannt. Ob sie nach Echnatons Tod als Pharaonin regierte, ist unter Historikern umstritten."
    ),

    // Question 5 - Egyptian Dynasties
    Question(
        categoryId = 3,
        questionText = "Welcher ägyptische Pharao der 19. Dynastie schloss um 1259 v. Chr. mit dem Hethiterkönig Hattusili III. den ältesten bekannten Friedensvertrag der Geschichte?",
        answerA = "Thutmosis III.",
        answerB = "Ramses I.",
        answerC = "Ramses II.",
        answerD = "Sethos I.",
        correctAnswer = 2, // C
        explanation = "Nach der Schlacht von Kadesch (ca. 1274 v. Chr.) schlossen Ramses II. und Hattusili III. um 1259 v. Chr. den Vertrag von Kadesch. Dieser Friedens- und Bündnisvertrag ist der älteste erhaltene Staatsvertrag der Geschichte und regelte Grenzfragen sowie gegenseitige Beistandspflichten.",
        funFact = "Eine Kopie dieses Vertrages hängt heute im Hauptquartier der Vereinten Nationen in New York als Symbol für internationale Diplomatie."
    ),

    // Question 6 - Persian Empire
    Question(
        categoryId = 3,
        questionText = "Welches Verwaltungssystem nutzte das Achämenidenreich zur Kontrolle seiner riesigen Territorien?",
        answerA = "Feudalherrschaft durch Vasallenkönige",
        answerB = "Satrapien-System mit königlichen Statthaltern",
        answerC = "Stadtstaaten-Konföderation",
        answerD = "Direkte Zentralverwaltung aus Persepolis",
        correctAnswer = 1, // B
        explanation = "Das Achämenidenreich (ca. 550–330 v. Chr.) war in Satrapien (Provinzen) unterteilt, die von Satrapen (Statthaltern) regiert wurden. Diese hatten militärische, administrative und steuerliche Befugnisse, wurden aber von königlichen Inspektoren – den 'Augen und Ohren des Königs' – überwacht.",
        funFact = "Das Achämenidenreich unter Darius I. umfasste etwa 8 Millionen km² und war damit das größte Reich, das die Welt bis dahin gesehen hatte."
    ),

    // Question 7 - Persian Empire
    Question(
        categoryId = 3,
        questionText = "Welcher persische König befahl den Bau des Suez-Kanal-Vorläufers zwischen dem Nil und dem Roten Meer?",
        answerA = "Kyros II.",
        answerB = "Kambyses II.",
        answerC = "Darius I.",
        answerD = "Xerxes I.",
        correctAnswer = 2, // C
        explanation = "Darius I. (reg. 522–486 v. Chr.) ließ einen Kanal vom Nil zum Roten Meer graben, der Schiffe von Ägypten nach Persien ermöglichte. Hieroglypheninschriften auf Stelen belegen, dass der Kanal zu seiner Zeit vollständig fertiggestellt war und intensiv genutzt wurde.",
        funFact = "Dieser antike Kanal verlief ungefähr entlang der Trasse des heutigen Suezkanals, der erst 1869 eröffnet wurde."
    ),

    // Question 8 - Alexander the Great
    Question(
        categoryId = 3,
        questionText = "Bei welcher Schlacht im Jahr 331 v. Chr. besiegte Alexander der Große das persische Heer unter Darius III. entscheidend?",
        answerA = "Schlacht am Granikos",
        answerB = "Schlacht bei Issos",
        answerC = "Schlacht bei Gaugamela",
        answerD = "Schlacht am Hydaspes",
        correctAnswer = 2, // C
        explanation = "In der Schlacht bei Gaugamela (1. Oktober 331 v. Chr.) im heutigen Irak besiegte Alexander das persische Heer endgültig. Darius III. floh und wurde später von einem seiner eigenen Satrapen ermordet. Diese Schlacht beendete das Achämenidenreich und öffnete Alexander den Weg nach Persepolis und Susa.",
        funFact = "Alexander soll angeblich gesagt haben, er bedauere, dass es keine weiteren Welten zu erobern gäbe – obwohl er nie Indien vollständig unterworfen hatte."
    ),

    // Question 9 - Alexander the Great
    Question(
        categoryId = 3,
        questionText = "Welche Stadt gründete Alexander der Große in Ägypten, die schnell zum intellektuellen Zentrum der antiken Welt wurde?",
        answerA = "Memphis",
        answerB = "Ptolemais",
        answerC = "Naukratis",
        answerD = "Alexandria",
        correctAnswer = 3, // D
        explanation = "Alexander gründete 331 v. Chr. Alexandria am Mittelmeer. Unter den Ptolemäern wurde es mit der berühmten Bibliothek von Alexandria zum Zentrum antiker Wissenschaft, Philosophie und Literatur. Die Stadt beherbergte zeitweise bis zu einer Million Einwohner.",
        funFact = "Alexander gründete auf seinem Feldzug insgesamt über 20 Städte mit dem Namen Alexandria, doch die ägyptische wurde die bedeutendste."
    ),

    // Question 10 - Alexander the Great
    Question(
        categoryId = 3,
        questionText = "An welcher Grenze stoppten Alexanders Truppen ihren Marsch nach Osten und zwangen ihn zur Umkehr (326 v. Chr.)?",
        answerA = "Indus-Fluss",
        answerB = "Ganges-Fluss",
        answerC = "Hyphasis (Beas)-Fluss",
        answerD = "Oxus (Amu Darya)-Fluss",
        correctAnswer = 2, // C
        explanation = "Am Hyphasis (heute Beas) in der Punjab-Region weigerten sich Alexanders erschöpfte Truppen nach Jahren des Feldzugs weiterzumarschieren. General Koinos sprach für das Heer und bat Alexander umzukehren. Dies war die einzige Niederlage, die Alexander nicht durch Kampf, sondern durch den Willen seiner eigenen Soldaten erlitt.",
        funFact = "Alexander soll drei Tage lang in seinem Zelt geblieben sein und Besuche verweigert haben, bevor er der Umkehr zustimmte."
    ),

    // Question 11 - Byzantine Empire
    Question(
        categoryId = 3,
        questionText = "Welcher byzantinische Kaiser erließ im 6. Jahrhundert die Codex Justinianus – eine umfassende Sammlung des römischen Rechts?",
        answerA = "Konstantin I.",
        answerB = "Theodosius II.",
        answerC = "Justinian I.",
        answerD = "Herakleios",
        correctAnswer = 2, // C
        explanation = "Kaiser Justinian I. (reg. 527–565) ließ von einer Juristenkommission unter Tribonian das Corpus Juris Civilis erarbeiten, zu dem der Codex Justinianus (534), die Digesten und die Institutionen gehörten. Dieses Werk wurde zur Grundlage des europäischen Zivilrechts.",
        funFact = "Justinians Gemahlin Theodora, eine ehemalige Schauspielerin, soll einen entscheidenden Einfluss auf seine Regierungspolitik gehabt haben und war bei den Nika-Unruhen 532 möglicherweise entscheidend für sein Überleben."
    ),

    // Question 12 - Byzantine Empire
    Question(
        categoryId = 3,
        questionText = "Welche byzantinische Brennwaffe, die griechisches Feuer verschoss, war so geheim, dass ihre genaue Zusammensetzung bis heute unbekannt ist?",
        answerA = "Katapultbombe",
        answerB = "Griechisches Feuer (Cheirosiphon)",
        answerC = "Naphtha-Schleuder",
        answerD = "Feuer-Tribok",
        correctAnswer = 1, // B
        explanation = "Das Griechische Feuer war eine byzantinische Brandwaffe, die durch Siphone (Pumpen) auf feindliche Schiffe gespritzt wurde. Es brannte sogar auf Wasser und war kaum zu löschen. Die Byzantiner setzten es erfolgreich bei der Abwehr arabischer Angriffe auf Konstantinopel (674–678 und 717–718) ein.",
        funFact = "Die Geheimhaltung der Formel war so streng, dass selbst byzantinische Quellen sich über die Zusammensetzung widersprechen. Wahrscheinlich enthielt es Kalkstein, Naphtha und andere Substanzen."
    ),

    // Question 13 - Byzantine Empire
    Question(
        categoryId = 3,
        questionText = "Wie nannten die Byzantiner ihr Reich selbst?",
        answerA = "Byzantinisches Reich",
        answerB = "Oströmisches Reich",
        answerC = "Basileia ton Rhomaion",
        answerD = "Hellenisches Kaiserreich",
        correctAnswer = 2, // C
        explanation = "Die Bewohner des sogenannten Byzantinischen Reiches nannten ihr Staatswesen 'Basileia ton Rhomaion' (Königreich der Römer) und bezeichneten sich selbst als Römer (Rhomaioi). Der Begriff 'Byzantinisches Reich' wurde erst von westlichen Historikern des 16./17. Jahrhunderts geprägt.",
        funFact = "Konstantinopel hieß für die Byzantiner 'die Stadt' (ē Polis) – kein weiterer Zusatz war nötig, da es aus ihrer Sicht die bedeutendste Stadt der Welt war."
    ),

    // Question 14 - Byzantine Empire
    Question(
        categoryId = 3,
        questionText = "Welches Jahr markiert den Fall Konstantinopels und das Ende des Byzantinischen Reiches?",
        answerA = "1389",
        answerB = "1453",
        answerC = "1461",
        answerD = "1402",
        correctAnswer = 1, // B
        explanation = "Am 29. Mai 1453 eroberte Sultan Mehmed II. mit osmanischen Truppen Konstantinopel nach einer 53-tägigen Belagerung. Kaiser Konstantin XI. Palaiologos fiel in den Kämpfen. Dieses Datum gilt oft als Scheidelinie zwischen Mittelalter und früher Neuzeit.",
        funFact = "Nach dem Fall Konstantinopels proklamierte Großfürst Iwan III. von Moskau Russland als 'Drittes Rom' – Erbe des byzantinischen Christentums und kaiserlicher Autorität."
    ),

    // Question 15 - Migration Period
    Question(
        categoryId = 3,
        questionText = "Welches Ereignis von 376 n. Chr. gilt als Auftakt der Völkerwanderungszeit?",
        answerA = "Einfall der Hunnen in Gallien",
        answerB = "Übersetzung der Westgoten über die Donau ins Römische Reich",
        answerC = "Plünderung Roms durch die Westgoten",
        answerD = "Aufstand der Sachsen in Britannien",
        correctAnswer = 1, // B
        explanation = "376 n. Chr. baten die Westgoten unter Druck der Hunnen den weströmischen Kaiser Valens, die Donau überqueren und im Römischen Reich siedeln zu dürfen. Dies wurde genehmigt, führte aber bald zu Konflikten, die 378 n. Chr. in der verheerenden Schlacht von Adrianopel gipfelten, bei der Valens selbst fiel.",
        funFact = "Die Hunnen, die die Völkerwanderung auslösten, stammten vermutlich aus Zentralasien und galten in römischen Quellen als besonders furchteinflößende Krieger."
    ),

    // Question 16 - Migration Period
    Question(
        categoryId = 3,
        questionText = "Welcher Hunnenkönig führte 451 n. Chr. seinen großen Feldzug nach Gallien, der in der Katalaunischen Feldschlacht gestoppt wurde?",
        answerA = "Rua",
        answerB = "Bleda",
        answerC = "Attila",
        answerD = "Dengizich",
        correctAnswer = 2, // C
        explanation = "Attila, König der Hunnen (reg. 434–453), führte 451 n. Chr. einen massiven Einfall in Gallien. Die Koalition unter dem römischen Feldherrn Aetius und dem Westgotenkönig Theoderich I. stoppte ihn in der Katalaunischen Feldschlacht (auf den Katalaunischen Feldern, nahe Troyes). Es war eine der blutigsten Schlachten der Antike.",
        funFact = "Attila starb 453 n. Chr. in seiner Hochzeitsnacht – angeblich an einer Nasenblutung. Sein Reich zerfiel unmittelbar nach seinem Tod."
    ),

    // Question 17 - Migration Period
    Question(
        categoryId = 3,
        questionText = "Welches germanische Volk gründete im 5. Jahrhundert in Nordafrika (heutiges Tunesien/Algerien) ein Königreich?",
        answerA = "Ostgoten",
        answerB = "Langobarden",
        answerC = "Vandalen",
        answerD = "Burgunden",
        correctAnswer = 2, // C
        explanation = "Die Vandalen überquerten unter König Geiserich 429 n. Chr. die Straße von Gibraltar und eroberten die nordafrikanischen Provinzen. 439 nahmen sie Karthago ein und gründeten das Vandalenreich. 455 plünderten sie Rom. Das Reich bestand bis zur byzantinischen Rückeroberung unter Belisar (534).",
        funFact = "Der Begriff 'Vandalismus' (sinnlose Zerstörung) geht auf den schlechten Ruf der Vandalen zurück, obwohl ihre Plünderung Roms 455 vergleichsweise weniger brutal als andere war."
    ),

    // Question 18 - Migration Period
    Question(
        categoryId = 3,
        questionText = "Welcher Ostgotenkönig regierte von 493 bis 526 Italien und schuf eine weitgehend erfolgreiche Koexistenz zwischen Römern und Goten?",
        answerA = "Alarich I.",
        answerB = "Theoderich der Große",
        answerC = "Odoaker",
        answerD = "Witichis",
        correctAnswer = 1, // B
        explanation = "Theoderich der Große (reg. 493–526) errichtete in Ravenna sein Herrschaftszentrum. Seine Regierung gilt als Musterbeispiel für römisch-germanische Koexistenz: Er behielt römische Verwaltungsstrukturen bei, förderte Gelehrte wie Boethius und Cassiodor und pflegte diplomatische Beziehungen zu anderen Reichen.",
        funFact = "Theoderich ließ kurz vor seinem Tod Boethius hinrichten – einen seiner engsten Berater – aus Verdacht auf Hochverrat. Dies bleibt ein dunkler Fleck seiner sonst vergleichsweise toleranten Herrschaft."
    ),

    // Question 19 - Carolingian Empire
    Question(
        categoryId = 3,
        questionText = "Welcher Feldzug Karls des Großen gegen die Sachsen dauerte am längsten und endete mit der Zwangschristianisierung des Stammes?",
        answerA = "Sachsenkriege (772–804)",
        answerB = "Awarenfeldzüge (791–805)",
        answerC = "Hispanienfeldzug (778)",
        answerD = "Langobardenfeldzug (773–774)",
        correctAnswer = 0, // A
        explanation = "Die Sachsenkriege (772–804) waren der längste und blutigste Konflikt Karls des Großen. Nach über 30 Jahren Kämpfen und mehreren sächsischen Aufständen – angeführt von Widukind – wurden die Sachsen schließlich christianisiert und ins Frankenreich eingegliedert. Das Massaker von Verden (782), bei dem Karl angeblich 4.500 Sachsen hinrichten ließ, ist das berüchtigtste Ereignis.",
        funFact = "Widukind, der sächsische Anführer, unterwarf sich schließlich und wurde 785 getauft. Karl selbst soll sein Taufpate gewesen sein."
    ),

    // Question 20 - Carolingian Empire
    Question(
        categoryId = 3,
        questionText = "Wer war der einzige Sohn, der das karolingische Reich nach dem Tod Ludwigs des Frommen zunächst allein übernahm, bevor der Vertrag von Verdun 843 die endgültige Reichsteilung besiegelte?",
        answerA = "Ludwig der Deutsche",
        answerB = "Karl der Kahle",
        answerC = "Lothar I.",
        answerD = "Pippin I. von Aquitanien",
        correctAnswer = 2, // C
        explanation = "Nach dem Tod Ludwigs des Frommen (840) beanspruchte Lothar I. als ältester Sohn das gesamte Reich allein. Seine Brüder Ludwig der Deutsche und Karl der Kahle verbündeten sich gegen ihn. Der Straßburger Eid (842) und schließlich der Vertrag von Verdun (843) teilten das Reich in drei Teile auf.",
        funFact = "Der Straßburger Eid von 842 ist das erste bekannte Dokument in einer althochdeutschen und altfranzösischen Sprachform – ein wichtiges Zeugnis der frühen Sprachentwicklung."
    ),

    // Question 21 - Carolingian Empire
    Question(
        categoryId = 3,
        questionText = "Welche Verwaltungsbeamten setzte Karl der Große ein, um die Treue seiner Grafen zu überprüfen und die kaiserliche Kontrolle über das Reich zu sichern?",
        answerA = "Pfalzgrafen",
        answerB = "Missi dominici",
        answerC = "Markgrafen",
        answerD = "Reichsmarschälle",
        correctAnswer = 1, // B
        explanation = "Die Missi dominici ('Gesandte des Herrn') waren königliche Bevollmächtigte, die Karl der Große ab 802 systematisch in Paaren (ein weltlicher und ein kirchlicher Vertreter) aussandte, um die Rechtsprechung zu überprüfen, Missstände zu berichten und kaiserliche Erlasse zu verkünden.",
        funFact = "Die Missi dominici bereisten jährlich festgelegte Regionen und hatten weitreichende Befugnisse – sie konnten Grafen und Bischöfe absetzen und vor den Kaiser bringen."
    ),

    // Question 22 - Viking Age
    Question(
        categoryId = 3,
        questionText = "Welche Entdeckungsfahrt unternahm Leif Eriksson um das Jahr 1000, die als erste europäische Reise nach Amerika gilt?",
        answerA = "Er segelte nach Island und gründete dort eine Kolonie",
        answerB = "Er entdeckte Vinland (Nordamerika) und überwinterte dort",
        answerC = "Er umrundete Grönland und kartierte die Küste",
        answerD = "Er erreichte als erster Europäer die Kanarischen Inseln",
        correctAnswer = 1, // B
        explanation = "Leif Eriksson segelte um 1000 n. Chr. von Grönland westwärts und erreichte eine Küste, die er Vinland nannte, wahrscheinlich im heutigen Neufundland. Dies gilt als erste dokumentierte europäische Entdeckung Nordamerikas – fast 500 Jahre vor Kolumbus. Die Entdeckung wurde durch die Vinland-Sagas und archäologische Funde in L'Anse aux Meadows belegt.",
        funFact = "L'Anse aux Meadows in Kanada wurde in den 1960er Jahren ausgegraben und ist die einzige gesicherte archäologische Stätte einer Wikingerkolonie in Nordamerika."
    ),

    // Question 23 - Viking Age
    Question(
        categoryId = 3,
        questionText = "Welches normannische Fürstentum entstand durch einen Vertrag von 911, als der Wikingerhäuptling Rollo das Land vom französischen König Karl dem Einfältigen erhielt?",
        answerA = "Herzogtum Bretagne",
        answerB = "Normandie",
        answerC = "Grafschaft Flandern",
        answerD = "Herzogtum Burgund",
        correctAnswer = 1, // B
        explanation = "Im Vertrag von Saint-Clair-sur-Epte (911) vergab König Karl III. (der Einfältige) dem Wikingerhäuptling Rollo das Land um Rouen als Lehen. Rollo nahm das Christentum an und wurde Vasall des französischen Königs. Aus diesem Gebiet entstand das Herzogtum Normandie.",
        funFact = "Die Nachfahren Rollos, die Normannen, sollten 1066 unter Wilhelm dem Eroberer England unterwerfen – eine der folgenreichsten Herrschaftsausweitungen des Mittelalters."
    ),

    // Question 24 - Viking Age
    Question(
        categoryId = 3,
        questionText = "Wie nannten die Wikinger ihre Schiffe, mit denen sie Flüsse entlangsegelten und weit ins Landesinnere vordrangen?",
        answerA = "Langschiffe (Drakkar)",
        answerB = "Koggen",
        answerC = "Knorr",
        answerD = "Schnekker",
        correctAnswer = 2, // C
        explanation = "Für Handelfahrten auf Flüssen und über Meere nutzten Wikinger breite, tiefergängige Frachtschiffe namens Knorr (auch Knarr). Das schlanke Langschiff (Drakkar/Longship) war für Kriegszüge optimiert. Auf russischen Flüssen nutzten Wikinger ('Waräger') diese Schiffe, um Handelswege bis nach Konstantinopel zu erschließen.",
        funFact = "Die Waräger, skandinavische Händler und Krieger, gründeten entlang der russischen Flüsse Handelsstädte und begründeten damit laut manchen Historikern die Anfänge des russischen Staates (Kiewer Rus)."
    ),

    // Question 25 - Viking Age
    Question(
        categoryId = 3,
        questionText = "Welche dänischen Wikingerkönige regierten England im 11. Jahrhundert und bildeten das sogenannte 'Nordseereich'?",
        answerA = "Harald Schönhaar und Olaf Tryggvason",
        answerB = "Sven Gabelbart und Knut der Große",
        answerC = "Harald Hardradi und Magnus der Gute",
        answerD = "Ragnar Lothbrok und Björn Eisenseite",
        correctAnswer = 1, // B
        explanation = "Sven I. Gabelbart eroberte England 1013 und wurde König, starb aber bereits 1014. Sein Sohn Knut der Große (reg. 1016–1035) vereinte England, Dänemark und Norwegen unter seiner Herrschaft – das sogenannte Nordseereich. Knut regierte relativ friedlich und förderte die englische Kirche.",
        funFact = "Die Legende, dass Knut das Meer befehlen wollte, ist eine Fehlinterpretation: Er wollte seinen Hofleuten zeigen, dass selbst er als König dem Meer nicht befehlen kann – eine Lektion in Demut."
    ),

    // Question 26 - Medieval Church
    Question(
        categoryId = 3,
        questionText = "Welches Kirchenkonzil von 1054 gilt als offizieller Ausgangspunkt des Großen Schismas zwischen römisch-katholischer und orthodoxer Kirche?",
        answerA = "Konzil von Nizäa (325)",
        answerB = "Konzil von Chalkedon (451)",
        answerC = "Es gab kein Konzil – das Schisma entstand durch gegenseitige Exkommunikationen",
        answerD = "Konzil von Konstantinopel (553)",
        correctAnswer = 2, // C
        explanation = "Das Große Schisma von 1054 begann nicht mit einem Konzil, sondern als päpstliche Legaten unter Kardinal Humbert und Patriarch Michael Kerullarios von Konstantinopel sich gegenseitig exkommunizierten. Die Trennungsursachen lagen in theologischen (Filioque-Streit), liturgischen und jurisdiktionellen Differenzen.",
        funFact = "Die gegenseitigen Exkommunikationen von 1054 wurden erst 1964 von Papst Paul VI. und Patriarch Athenagoras I. symbolisch aufgehoben."
    ),

    // Question 27 - Medieval Church
    Question(
        categoryId = 3,
        questionText = "Welche Bulle Papst Bonifaz' VIII. von 1302 formulierte den päpstlichen Anspruch auf Überordnung über alle weltlichen Herrscher am radikalsten?",
        answerA = "Dictatus Papae",
        answerB = "Unam Sanctam",
        answerC = "Laudabiliter",
        answerD = "Clericis laicos",
        correctAnswer = 1, // B
        explanation = "Die Bulle 'Unam Sanctam' (1302) erklärte, dass es außerhalb der Kirche kein Heil gebe und dass die päpstliche Autorität über jede menschliche Kreatur stehe. Sie war die schärfste Formulierung päpstlicher Suprematie im Streit mit dem französischen König Philipp IV. (dem Schönen).",
        funFact = "Als Reaktion ließ Philipp IV. 1303 den Papst in Anagni gefangen nehmen – ein Ereignis, das als 'Ohrfeige von Anagni' bekannt wurde. Bonifaz VIII. starb kurz darauf."
    ),

    // Question 28 - Medieval Church
    Question(
        categoryId = 3,
        questionText = "Welches Konzil (1414–1418) beendete das Große Abendländische Schisma, bei dem zeitweise drei Päpste gleichzeitig regierten?",
        answerA = "Konzil von Pisa (1409)",
        answerB = "Konzil von Lyon (1245)",
        answerC = "Konzil von Konstanz (1414–1418)",
        answerD = "Konzil von Basel (1431–1449)",
        correctAnswer = 2, // C
        explanation = "Das Konzil von Konstanz (1414–1418) beendete das Abendländische Schisma (1378–1417), in dem bis zu drei Päpste gleichzeitig Anspruch auf den Thron Petri erhoben. Es setzte alle drei Prätendenten ab oder ließ sie zurücktreten und wählte Martin V. als neuen, allgemein anerkannten Papst.",
        funFact = "Auf dem Konzil von Konstanz wurde auch der böhmische Reformator Jan Hus trotz kaiserlichem Geleit verhaftet und 1415 als Ketzer verbrannt – was den Hussitenkrieg auslöste."
    ),

    // Question 29 - Investiture Controversy
    Question(
        categoryId = 3,
        questionText = "Was verstand man unter 'Investitur' im mittelalterlichen Kirchenstreit?",
        answerA = "Die Weihe eines Bischofs durch den Papst",
        answerB = "Die Einsetzung eines Geistlichen in sein Amt mit Ring und Stab durch einen weltlichen Herrscher",
        answerC = "Die Bestätigung eines Königs durch den Papst",
        answerD = "Das Recht des Königs, Steuern von der Kirche zu erheben",
        correctAnswer = 1, // B
        explanation = "Investitur bezeichnet die Belehnung eines Bischofs oder Abtes mit den Insignien seines geistlichen Amtes (Ring und Stab) durch den König oder Kaiser. Das Problem war, dass weltliche Herrscher so faktisch Kontrolle über kirchliche Amtsträger behielten, was dem Reformpapsttum ein Dorn im Auge war.",
        funFact = "Der Investiturstreit hatte tiefgreifende Folgen für die europäische Geschichte: Er schwächte die kaiserliche Macht in Deutschland dauerhaft und stärkte die Fürstenmacht – ein wesentlicher Grund für die spätere Kleinstaaterei Deutschlands."
    ),

    // Question 30 - Investiture Controversy
    Question(
        categoryId = 3,
        questionText = "Welcher Papst bannte Kaiser Heinrich IV. 1076 und löste damit den Investiturstreit auf seinen Höhepunkt?",
        answerA = "Gregor VI.",
        answerB = "Leo IX.",
        answerC = "Gregor VII.",
        answerD = "Urban II.",
        correctAnswer = 2, // C
        explanation = "Papst Gregor VII. (reg. 1073–1085) bannte Kaiser Heinrich IV. 1076 und entband seine Untertanen vom Treueid. Dies zwang Heinrich, im Winter 1077 nach Canossa zu pilgern und um Absolution zu bitten – ein Schlüsselereignis des Investiturstreits. Trotzdem setzte sich der Konflikt fort.",
        funFact = "Der Gang nach Canossa wurde sprichwörtlich für eine demütigende Unterwerfung. Bismarck nutzte diesen Ausdruck 1872 im Kulturkampf: 'Nach Canossa gehen wir nicht.'"
    ),

    // Question 31 - Investiture Controversy
    Question(
        categoryId = 3,
        questionText = "Welches Konkordat von 1122 beendete den Investiturstreit durch einen Kompromiss zwischen Kaiser und Papst?",
        answerA = "Konkordat von Worms",
        answerB = "Konkordat von Bologna",
        answerC = "Lateranvertrag",
        answerD = "Konkordat von Brixen",
        correctAnswer = 0, // A
        explanation = "Das Konkordat von Worms (1122) zwischen Kaiser Heinrich V. und Papst Calixt II. trennte weltliche und geistliche Investitur: Der Kaiser investierte mit dem Zepter (weltliche Güter/Rechte), der Papst mit Ring und Stab (geistliche Würde). Diese Kompromisslösung beendete den über 50-jährigen Konflikt.",
        funFact = "Trotz des Kompromisses blieb die Frage der Überordnung von Papst oder Kaiser ungelöst und beschäftigte Europa noch Jahrhunderte lang – bis zum Ende des Heiligen Römischen Reiches 1806."
    ),

    // Question 32 - Crusader States
    Question(
        categoryId = 3,
        questionText = "Welche vier Kreuzfahrerstaaten entstanden nach dem Ersten Kreuzzug (1099) im Heiligen Land?",
        answerA = "Jerusalem, Antiochia, Edessa und Tripolis",
        answerB = "Jerusalem, Zypern, Akkon und Tyros",
        answerC = "Antiochia, Armenien, Byzanz-Vasall und Damaskus",
        answerD = "Jerusalem, Palästina, Syrien und Libanon",
        correctAnswer = 0, // A
        explanation = "Nach dem Ersten Kreuzzug entstanden vier Kreuzfahrerstaaten: das Königreich Jerusalem (1099), das Fürstentum Antiochia (1098), die Grafschaft Edessa (1098) – der erste und am stärksten bedrohte – und die Grafschaft Tripolis (1102/1109). Diese Staaten bestanden in wechselnder Form bis 1291.",
        funFact = "Die Grafschaft Edessa fiel 1144 als erster Kreuzfahrerstaat an die muslimischen Seldschuken zurück – ein Ereignis, das den Zweiten Kreuzzug auslöste."
    ),

    // Question 33 - Crusader States
    Question(
        categoryId = 3,
        questionText = "Welcher muslimische Sultan nahm 1187 Jerusalem von den Kreuzfahrern zurück und löste damit den Dritten Kreuzzug aus?",
        answerA = "Nureddin",
        answerB = "Baibars",
        answerC = "Saladin",
        answerD = "Al-Kamil",
        correctAnswer = 2, // C
        explanation = "Saladin (Salah ad-Din Yusuf ibn Ayyub), Sultan von Ägypten und Syrien, besiegte die Kreuzfahrer am 4. Juli 1187 in der Schlacht bei Hattin und nahm am 2. Oktober 1187 Jerusalem ein. Im Gegensatz zur blutigen Einnahme durch die Kreuzfahrer 1099 erlaubte Saladin den Christen die Ausreise gegen Lösegeld.",
        funFact = "Saladin wurde auch von christlichen Chronisten als ritterlicher und tugendhafter Herrscher beschrieben – ein seltenes Zeugnis gegenseitiger Achtung in einer Zeit religiöser Kriege."
    ),

    // Question 34 - Crusader States
    Question(
        categoryId = 3,
        questionText = "Welcher Kreuzzug endete damit, dass Kreuzfahrer 1204 statt Jerusalem das christliche Konstantinopel plünderten?",
        answerA = "Dritter Kreuzzug",
        answerB = "Vierter Kreuzzug",
        answerC = "Fünfter Kreuzzug",
        answerD = "Sechster Kreuzzug",
        correctAnswer = 1, // B
        explanation = "Der Vierte Kreuzzug (1202–1204) wurde ursprünglich gegen Ägypten geplant. Durch venezianischen Einfluss und innerbyzantinische Thronfolgekämpfe endete er mit der Plünderung und Einnahme Konstantinopels. Es entstand das Lateinische Kaiserreich (1204–1261), was die byzantinisch-westliche Christenheit dauerhaft vertiefte.",
        funFact = "Die Beute aus Konstantinopel war immens – darunter die vier bronzenen Pferde, die heute (in Kopien) den Markusdom in Venedig zieren. Die Originale befinden sich im Inneren des Doms."
    ),

    // Question 35 - Crusader States
    Question(
        categoryId = 3,
        questionText = "Welche Ritterorden wurden speziell für den Schutz der Pilger und Kreuzfahrerstaaten gegründet?",
        answerA = "Franziskaner, Dominikaner und Augustiner",
        answerB = "Templer, Johanniter und Deutschordensritter",
        answerC = "Zisterzienser, Prämonstratenser und Karmeliter",
        answerD = "Malteser, Benediktiner und Trinitarier",
        correctAnswer = 1, // B
        explanation = "Die drei großen Ritterorden entstanden im Heiligen Land: Die Templer (gegr. ca. 1119/1120) zum Schutz von Pilgern, die Johanniter (gegr. ca. 1113) zur Krankenversorgung und zum Schutz sowie der Deutsche Orden (gegr. 1190/1198) als deutsch-sprachiger Orden für deutsche Pilger. Alle drei entwickelten bedeutende militärische und wirtschaftliche Macht.",
        funFact = "Der Templerorden wurde 1312 auf Betreiben König Philipps IV. von Frankreich aufgelöst. Sein Großmeister Jacques de Molay wurde 1314 auf dem Scheiterhaufen hingerichtet."
    ),

    // Question 36 - Byzantine Empire (advanced)
    Question(
        categoryId = 3,
        questionText = "Welche Krise des 7. Jahrhunderts kostete das Byzantinische Reich dauerhaft die Provinzen Syrien, Ägypten und Nordafrika?",
        answerA = "Die Invasion der Sasaniden unter Chosrau II.",
        answerB = "Die arabisch-islamischen Eroberungen unter dem Kalifat",
        answerC = "Die Awarenbelagerung Konstantinopels",
        answerD = "Die Pest des Justinian",
        correctAnswer = 1, // B
        explanation = "Nach den erschöpfenden Kriegen gegen die Sasaniden verlor Byzanz in den 630er–640er Jahren Syrien, Palästina, Ägypten und später Nordafrika an die arabisch-islamischen Heere der Rashidun-Kalifen. Die entscheidende Niederlage war die Schlacht am Yarmuk (636). Diese Gebiete kehrten nie unter byzantinische Kontrolle zurück.",
        funFact = "Ägypten war die reichste Provinz des Reiches und lieferte den Großteil des Getreides für Konstantinopel. Sein Verlust war ein schwerer wirtschaftlicher und demographischer Schlag."
    ),

    // Question 37 - Persian Empire (advanced)
    Question(
        categoryId = 3,
        questionText = "Wie hieß die altpersische Schriftform, die vor allem für königliche Inschriften auf Fels und Tontafeln verwendet wurde?",
        answerA = "Keilschrift (Akkadisch)",
        answerB = "Altpersische Keilschrift",
        answerC = "Aramäische Schrift",
        answerD = "Phönizisches Alphabet",
        correctAnswer = 1, // B
        explanation = "Die altpersische Keilschrift war eine vereinfachte Silbenschrift mit ca. 36 Zeichen, die unter den Achämeniden für offizielle Inschriften entwickelt wurde. Berühmt ist die dreisprachige Behistun-Inschrift (altpersisch, elamisch, babylonisch), die George Rawlinson im 19. Jahrhundert zur Entzifferung der Keilschriften nutzte.",
        funFact = "Henry Rawlinson kletterte in den 1840er Jahren an den fast senkrechten Fels von Behistun in 100 Meter Höhe, um die Inschrift abzuschreiben – eine der gefährlichsten Kopierarbeiten der Archäologie."
    ),

    // Question 38 - Migration Period (advanced)
    Question(
        categoryId = 3,
        questionText = "Welches Ereignis von 476 n. Chr. wird traditionell als Ende des Weströmischen Reiches bezeichnet?",
        answerA = "Die Plünderung Roms durch die Westgoten unter Alarich",
        answerB = "Der Tod von Kaiser Valentinian III.",
        answerC = "Die Absetzung von Kaiser Romulus Augustulus durch Odoaker",
        answerD = "Die Übernahme Roms durch Attila",
        correctAnswer = 2, // C
        explanation = "476 n. Chr. setzte der germanische Söldnerführer Odoaker den letzten weströmischen Kaiser Romulus Augustulus ab und schickte die Kaiserinsignien nach Konstantinopel. Dieses symbolische Datum wird als Ende des Weströmischen Reiches gewertet, obwohl das Reich de facto schon längst zerfallen war.",
        funFact = "Romulus Augustulus – sein Name ironischerweise eine Kombination des Reichsgründers und des ersten Kaisers – wurde nach Absetzung nicht getötet, sondern nach Kampanien verbannt, wo er eine Pension erhielt."
    ),

    // Question 39 - Carolingian Empire (advanced)
    Question(
        categoryId = 3,
        questionText = "Welches Konzil unter Karl dem Großen im Jahr 794 verurteilte die karolingische Position gegenüber dem byzantinischen Bilderstreit?",
        answerA = "Konzil von Aachen (809)",
        answerB = "Frankfurter Konzil (794)",
        answerC = "Konzil von Reims (813)",
        answerD = "Konzil von Paris (825)",
        correctAnswer = 1, // B
        explanation = "Das Frankfurter Konzil (794) unter Karl dem Großen lehnte sowohl den byzantinischen Ikonoklasmus als auch die Bilderverehrung des Zweiten Konzils von Nizäa (787) ab. Die fränkische Position, festgelegt in den Libri Carolini, war eine gemäßigte Mittellinie: Bilder zur Belehrung erlaubt, Verehrung verboten.",
        funFact = "Die Libri Carolini sind ein faszinierendes Zeugnis theologischer und politischer Eigenständigkeit der karolingischen Kirche gegenüber Papst und Byzanz."
    ),

    // Question 40 - Crusader States (advanced)
    Question(
        categoryId = 3,
        questionText = "Welcher König des Königreichs Jerusalem litt an Lepra und regierte dennoch erfolgreich gegen Saladin, u.a. in der Schlacht bei Montgisard 1177?",
        answerA = "Balduin III.",
        answerB = "Amalrich I.",
        answerC = "Balduin IV.",
        answerD = "Guido von Lusignan",
        correctAnswer = 2, // C
        explanation = "Balduin IV., der 'Aussätzige König' (reg. 1174–1185), regierte trotz fortschreitender Lepra-Erkrankung. In der Schlacht bei Montgisard (1177) führte er persönlich seine Armee und besiegte Saladins überlegenes Heer. Seine Herrschaft gilt als eine der bemerkenswertesten der Kreuzfahrergeschichte.",
        funFact = "Balduin IV. trug eine weiße Maske, um sein von der Lepra entstelltes Gesicht zu verbergen. Er starb 1185 im Alter von etwa 24 Jahren."
    ),

    // Question 41 - Ancient Civilizations
    Question(
        categoryId = 3,
        questionText = "Welches sumerische Epos gilt als das älteste erhaltene literarische Werk der Menschheit?",
        answerA = "Ilias",
        answerB = "Gilgamesch-Epos",
        answerC = "Enuma Elisch",
        answerD = "Rigveda",
        correctAnswer = 1, // B
        explanation = "Das Gilgamesch-Epos (Ur-Fassung ca. 2100 v. Chr., akkadische Standardfassung ca. 1200 v. Chr.) erzählt von König Gilgamesch von Uruk und seiner Suche nach Unsterblichkeit. Es enthält u.a. eine Sintfluterzählung, die der biblischen Noahgeschichte sehr ähnelt. Es gilt als das älteste erhaltene epische Literaturdenkmal.",
        funFact = "Die berühmte Keilschrifttafel XII des Gilgamesch-Epos wurde 1853 von George Smith entdeckt und enthielt die Sintflut-Episode – ein archäologischer Sensationsfund des 19. Jahrhunderts."
    ),

    // Question 42 - Viking Age (advanced)
    Question(
        categoryId = 3,
        questionText = "Welche Runenschrift war die älteste und wurde von Wikingern für zeremonielle und religiöse Inschriften verwendet?",
        answerA = "Jüngeres Futhark (16 Runen)",
        answerB = "Älteres Futhark (24 Runen)",
        answerC = "Mittelalterliches Futhark (28 Runen)",
        answerD = "Angelsächsisches Futhorc (33 Runen)",
        correctAnswer = 1, // B
        explanation = "Das Ältere Futhark mit 24 Zeichen war die älteste Runenschrift (ca. 2.–8. Jahrhundert). Im Wikingerzeitalter verwendeten Skandinavier das vereinfachte Jüngere Futhark mit nur 16 Zeichen. Paradoxerweise reduzierte das Jüngere Futhark die Zeichenzahl trotz komplexerer Sprache – viele Laute mussten durch Doppelverwendung einzelner Runen dargestellt werden.",
        funFact = "Es gibt weltweit über 6.000 erhaltene Runeninschriften, davon allein rund 2.500 in Schweden – meist auf sogenannten Runensteinen als Grabdenkmäler oder Gedenksteine."
    ),

    // Question 43 - Medieval Church (advanced)
    Question(
        categoryId = 3,
        questionText = "Welche mittelalterliche Mönchsreform des 10./11. Jahrhunderts stärkte die Unabhängigkeit der Kirche von weltlichen Einflüssen und war Wegbereiter des Investiturstreits?",
        answerA = "Cluniazenserreform",
        answerB = "Zisterzienserreform",
        answerC = "Benediktinische Reform von Gorze",
        answerD = "Prämonstratenserreform",
        correctAnswer = 0, // A
        explanation = "Die Cluniazenserreform, ausgehend vom Kloster Cluny (gegr. 910 in Burgund), betonte klösterliche Disziplin, die direkte Unterstellung unter den Papst (statt lokale Bischöfe/weltliche Herren) und die Freiheit der Kirche von weltlicher Einmischung. Diese Ideen beeinflussten direkt das Reformpapsttum und den Investiturstreit.",
        funFact = "Cluny wuchs zu einem der mächtigsten Klosterverbände Europas an – mit über 1.000 abhängigen Prioraten und einer romanischen Klosterkirche, die bis zum Bau des Petersdoms in Rom die größte Kirche der Christenheit war."
    ),

    // Question 44 - Egyptian Dynasties (advanced)
    Question(
        categoryId = 3,
        questionText = "Welches Bauprojekt des Pharaos Sesostris III. aus der 12. Dynastie galt als wichtiges Mittel zur Kontrolle nubischer Handelsrouten?",
        answerA = "Bau des Karnak-Tempels",
        answerB = "Errichtung von Festungsanlagen am zweiten Nilkatarakt",
        answerC = "Anlage des Fayum-Kanals zur Bewässerung",
        answerD = "Bau der Großen Sphinx von Gizeh",
        correctAnswer = 1, // B
        explanation = "Sesostris III. (ca. 1878–1839 v. Chr.) aus der 12. Dynastie ließ ein Netz von Festungen am zweiten Nilkatarakt in Nubien errichten, darunter Semna, Kumma und Buhen. Diese Festungen kontrollierten den Warenverkehr, insbesondere Gold, Elfenbein und Sklaven aus dem Süden.",
        funFact = "Die Festungen wurden beim Bau des Assuan-Hochdamms (1960er Jahre) überflutet. Archäologen retteten vorher umfangreiche Dokumentationen und Teile der Objekte."
    ),

    // Question 45 - Byzantine Empire (advanced)
    Question(
        categoryId = 3,
        questionText = "Welcher theologische Streit des 8./9. Jahrhunderts spaltete das Byzantinische Reich in Ikonoklasten und Ikonodulen?",
        answerA = "Arianismus-Streit",
        answerB = "Bilderstreit (Ikonoklasmus)",
        answerC = "Monophysitismus-Kontroverse",
        answerD = "Nestorianismus-Konflikt",
        correctAnswer = 1, // B
        explanation = "Der byzantinische Bilderstreit (Ikonoklasmus, 726–843) entfachte, als Kaiser Leon III. die Verehrung von Heiligenbildern verbot. Die Ikonoklasten (Bilderfeinde) sahen darin Götzendienst, die Ikonodulen (Bilderfreunde) verteidigten die Bilder als Fenster zum Heiligen. Der Streit endete 843 mit dem 'Triumph der Orthodoxie' – der Wiederherstellung der Bilderverehrung.",
        funFact = "Der 'Triumph der Orthodoxie' (843) wird in der Ostkirche noch heute am ersten Sonntag der Fastenzeit als Feiertag begangen."
    ),

    // Question 46 - Alexander the Great (advanced)
    Question(
        categoryId = 3,
        questionText = "Welche Stadt in Zentralasien widersetzte sich Alexander dem Großen hartnäckig und führte zu einem der längsten Feldzüge seiner Kampagne?",
        answerA = "Persepolis",
        answerB = "Baktra (Balch)",
        answerC = "Sogdischer Fels (Fels des Ariamazes)",
        answerD = "Marakanda (Samarkand)",
        correctAnswer = 2, // C
        explanation = "Der Sogdische Fels (auch 'Fels des Ariamazes') in der heutigen Usbekistan galt als uneinnehmbar. Alexander ließ seine Soldaten nachts die Felswände erklimmen und erschreckte die Verteidiger so sehr, dass sie kapitulierten. Hier traf er auch Roxane, die er heiratete. Der gesamte Zentralasienfeldzug (329–327 v. Chr.) war eine der härtesten Phasen seiner Kampagne.",
        funFact = "Alexander heiratete Roxane (Rukhshana), Tochter des baktrischen Adligen Oxyartes – die erste seiner drei Ehefrauen. Diese Heirat war politisch und half, den baktrischen Adel zu gewinnen."
    ),

    // Question 47 - Carolingian Empire (advanced)
    Question(
        categoryId = 3,
        questionText = "Welche karolingische Bildungsreform zielte darauf ab, den Klerus lesen und schreiben zu lehren und förderte die Entwicklung der karolingischen Minuskel?",
        answerA = "Reform der Aachener Hofschule",
        answerB = "Capitulare de villis",
        answerC = "Admonitio Generalis und die karolingische Renaissance",
        answerD = "Ordinatio Imperii",
        correctAnswer = 2, // C
        explanation = "Die Admonitio Generalis (789) Karls des Großen verpflichtete Klöster und Domkirchen zur Einrichtung von Schulen. Durch die karolingische Renaissance – mit Gelehrten wie Alkuin, Einhard und Theodulf – entstanden einheitliche Schriftformen (karolingische Minuskel), Kopien antiker Texte und eine neue Bildungskultur.",
        funFact = "Die karolingische Minuskel war so klar und lesbar, dass Humanisten des 15. Jahrhunderts sie für antike Schrift hielten und sie als Grundlage der modernen Kleinbuchstaben übernahmen."
    ),

    // Question 48 - Viking Age (advanced)
    Question(
        categoryId = 3,
        questionText = "Welche Hauptgottheit des nordischen Pantheons opferte sich selbst am Weltenbaum Yggdrasil, um die Runen zu erlangen?",
        answerA = "Thor",
        answerB = "Odin",
        answerC = "Tyr",
        answerD = "Freyr",
        correctAnswer = 1, // B
        explanation = "Laut der nordischen Mythologie (Hávamál in der Edda) hängte sich Odin neun Tage und Nächte an den Weltenbaum Yggdrasil, verwundete sich mit seinem Speer und opferte sich sich selbst für sich selbst, um das Wissen der Runen zu erlangen. Diese Selbstopferung zeigt Odin als Gott der Weisheit, Magie und Dichtkunst.",
        funFact = "Odin hatte auch seinen Raben Hugin (Gedanke) und Munin (Erinnerung), die täglich die Welt bereisten und ihm alles berichteten. Er fürchtete, Munin zu verlieren mehr als Hugin."
    ),

    // Question 49 - Medieval Church / Papacy (advanced)
    Question(
        categoryId = 3,
        questionText = "Welches Papstdokument von 1075 – der 'Dictatus Papae' – fasste die Ansprüche Gregors VII. auf päpstliche Weltherrschaft in 27 Thesen zusammen?",
        answerA = "Es war eine öffentliche Enzyklika für alle Gläubigen",
        answerB = "Es war ein privates Registereintrag ohne offizielle Wirkung – seine genaue Funktion ist bis heute umstritten",
        answerC = "Es war ein Konzilsbeschluss aller Bischöfe",
        answerD = "Es war ein Vertrag mit dem Kaiser",
        correctAnswer = 1, // B
        explanation = "Der Dictatus Papae (1075) ist ein Dokument im päpstlichen Register Gregors VII. mit 27 Aussagen über päpstliche Suprematie (u.a. alleiniges Recht zur Berufung/Absetzung von Bischöfen, Kaisern). Seine genaue Funktion – ob Thesenliste, Inhaltsverzeichnis einer verlorenen Kanonrechtssammlung oder politisches Programm – ist in der Wissenschaft umstritten.",
        funFact = "These 12 des Dictatus Papae lautet: 'Es ist ihm erlaubt, Kaiser abzusetzen' – ein Anspruch, der Heinrich IV. zum Gang nach Canossa zwang."
    ),

    // Question 50 - Crusader States (advanced)
    Question(
        categoryId = 3,
        questionText = "Welcher Kreuzzug erreichte sein Ziel ohne Kampf durch einen Vertrag zwischen Kaiser Friedrich II. und Sultan al-Kamil?",
        answerA = "Fünfter Kreuzzug (1217–1221)",
        answerB = "Sechster Kreuzzug (1228–1229)",
        answerC = "Siebter Kreuzzug (1248–1254)",
        answerD = "Achter Kreuzzug (1270)",
        correctAnswer = 1, // B
        explanation = "Der Sechste Kreuzzug (1228–1229) unter Kaiser Friedrich II. war einzigartig: Friedrich, der zu diesem Zeitpunkt exkommuniziert war, verhandelte mit Sultan al-Kamil von Ägypten einen 10-jährigen Friedensvertrag. Darin wurde Jerusalem (ohne Tempelberg) an die Kreuzfahrer zurückgegeben. Dies empörte den Papst und die Ritterorden gleichermaßen.",
        funFact = "Friedrich II. wurde von Papst Gregor IX. als 'Antichrist' bezeichnet, weil er als Exkommunizierter einen Kreuzzug führte – und damit Erfolg hatte. Es war der einzige Kreuzzug, der sein Hauptziel ohne eine einzige größere Schlacht erreichte."
    ),

)
