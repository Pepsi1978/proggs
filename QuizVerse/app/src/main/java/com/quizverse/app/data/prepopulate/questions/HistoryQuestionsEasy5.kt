package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun historyQuestionsEasy5(): List<Question> = listOf(

    // --- Democracy in Athens ---

    Question(
        categoryId = 3,
        questionText = "In welcher antiken Stadt wurde die Demokratie erfunden?",
        answerA = "Rom",
        answerB = "Athen",
        answerC = "Sparta",
        answerD = "Korinth",
        correctAnswer = 1,
        explanation = "Athen gilt als Geburtsort der Demokratie. Im 5. Jahrhundert v. Chr. führte Kleisthenes grundlegende demokratische Reformen ein.",
        difficulty = 1,
        funFact = "Das Wort Demokratie stammt aus dem Griechischen: 'demos' (Volk) und 'kratos' (Herrschaft)."
    ),

    Question(
        categoryId = 3,
        questionText = "Wer gilt als 'Vater der Demokratie' im antiken Athen?",
        answerA = "Sokrates",
        answerB = "Perikles",
        answerC = "Kleisthenes",
        answerD = "Platon",
        correctAnswer = 2,
        explanation = "Kleisthenes führte um 508 v. Chr. demokratische Reformen in Athen ein und wird daher oft als Vater der Demokratie bezeichnet.",
        difficulty = 1,
        funFact = "Kleisthenes schuf das System der zehn Phylen, um alte Adelsclans zu zerschlagen und das Volk politisch zu stärken."
    ),

    Question(
        categoryId = 3,
        questionText = "Wer durfte in der athenischen Demokratie wählen?",
        answerA = "Alle Einwohner Athens",
        answerB = "Alle erwachsenen Männer und Frauen",
        answerC = "Nur männliche Vollbürger",
        answerD = "Nur Adlige und Priester",
        correctAnswer = 2,
        explanation = "In der athenischen Demokratie durften nur freie, männliche Bürger Athens ab 18 Jahren an der Volksversammlung teilnehmen. Frauen, Sklaven und Fremde waren ausgeschlossen.",
        difficulty = 1,
        funFact = "Schätzungsweise nur 10–15 % der Bevölkerung Athens waren stimmberechtigte Bürger."
    ),

    Question(
        categoryId = 3,
        questionText = "Wie hieß die Volksversammlung im antiken Athen?",
        answerA = "Forum",
        answerB = "Senat",
        answerC = "Agora",
        answerD = "Ekklesia",
        correctAnswer = 3,
        explanation = "Die Ekklesia war die athenische Volksversammlung, in der stimmberechtigte Bürger über Gesetze und Politik abstimmten.",
        difficulty = 1,
        funFact = "Die Ekklesia traf sich ca. 40 Mal im Jahr auf dem Pnyx-Hügel westlich der Akropolis."
    ),

    // --- History of Writing ---

    Question(
        categoryId = 3,
        questionText = "Welches Volk erfand die Keilschrift, eine der ältesten Schriften der Welt?",
        answerA = "Die Ägypter",
        answerB = "Die Chinesen",
        answerC = "Die Sumerer",
        answerD = "Die Griechen",
        correctAnswer = 2,
        explanation = "Die Sumerer in Mesopotamien (heutiger Irak) entwickelten die Keilschrift um ca. 3200 v. Chr. als eine der ältesten bekannten Schriften.",
        difficulty = 1,
        funFact = "Die Keilschrift wurde mit einem Griffel in weiche Tontafeln gedrückt, die danach getrocknet oder gebrannt wurden."
    ),

    Question(
        categoryId = 3,
        questionText = "Wie heißt die Bilderschrift der alten Ägypter?",
        answerA = "Keilschrift",
        answerB = "Hieroglyphen",
        answerC = "Runen",
        answerD = "Piktogramme",
        correctAnswer = 1,
        explanation = "Die alten Ägypter verwendeten Hieroglyphen, eine Bilderschrift, die seit ca. 3200 v. Chr. belegt ist.",
        difficulty = 1,
        funFact = "Der Stein von Rosette (1799 entdeckt) half Wissenschaftlern, die Hieroglyphen im 19. Jahrhundert zu entschlüsseln."
    ),

    Question(
        categoryId = 3,
        questionText = "Wer entzifferte 1822 die ägyptischen Hieroglyphen mithilfe des Steins von Rosette?",
        answerA = "Napoleon Bonaparte",
        answerB = "Howard Carter",
        answerC = "Jean-François Champollion",
        answerD = "Heinrich Schliemann",
        correctAnswer = 2,
        explanation = "Der französische Gelehrte Jean-François Champollion gelang 1822 der entscheidende Durchbruch bei der Entzifferung der Hieroglyphen.",
        difficulty = 1,
        funFact = "Champollion erkannte, dass Hieroglyphen sowohl Laute als auch Bedeutungen darstellen können – ein revolutionärer Gedanke."
    ),

    Question(
        categoryId = 3,
        questionText = "Auf welchem Material schrieben die alten Ägypter hauptsächlich?",
        answerA = "Ton",
        answerB = "Papyrus",
        answerC = "Pergament",
        answerD = "Holz",
        correctAnswer = 1,
        explanation = "Die Ägypter fertigten aus dem Papyrus-Schilf Schreibmaterial an, das dem modernen Papier ähnelt und sehr haltbar ist.",
        difficulty = 1,
        funFact = "Unser Wort 'Papier' leitet sich vom altgriechischen Wort 'papyros' ab, das auf den Nil-Schilf zurückgeht."
    ),

    // --- Gutenberg Printing Press ---

    Question(
        categoryId = 3,
        questionText = "Wer erfand den Buchdruck mit beweglichen Lettern in Europa?",
        answerA = "Martin Luther",
        answerB = "Leonardo da Vinci",
        answerC = "Johannes Gutenberg",
        answerD = "Nikolaus Kopernikus",
        correctAnswer = 2,
        explanation = "Johannes Gutenberg aus Mainz erfand um 1450 den Buchdruck mit beweglichen Metalllettern und revolutionierte damit die Verbreitung von Wissen.",
        difficulty = 1,
        funFact = "Gutenbergs erste große Druckarbeit war die Gutenberg-Bibel, von der heute noch etwa 49 Exemplare erhalten sind."
    ),

    Question(
        categoryId = 3,
        questionText = "In welcher deutschen Stadt erfand Gutenberg den Buchdruck?",
        answerA = "Frankfurt",
        answerB = "Köln",
        answerC = "Heidelberg",
        answerD = "Mainz",
        correctAnswer = 3,
        explanation = "Johannes Gutenberg entwickelte seinen Buchdruck in Mainz, wo er um 1450 seine Druckerpresse in Betrieb nahm.",
        difficulty = 1,
        funFact = "Heute beherbergt Mainz das Gutenberg-Museum, eines der bedeutendsten Druckmuseen der Welt."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches war das erste wichtige Buch, das Gutenberg druckte?",
        answerA = "Shakespeares Hamlet",
        answerB = "Die Bibel",
        answerC = "Dantes Göttliche Komödie",
        answerD = "Homers Ilias",
        correctAnswer = 1,
        explanation = "Gutenbergs berühmtestes Werk ist die 42-zeilige Bibel (auch Gutenberg-Bibel genannt), gedruckt um 1455.",
        difficulty = 1,
        funFact = "Eine vollständige Gutenberg-Bibel wurde zuletzt für über 5 Millionen Dollar versteigert."
    ),

    // --- Titanic ---

    Question(
        categoryId = 3,
        questionText = "In welchem Jahr sank die Titanic?",
        answerA = "1905",
        answerB = "1912",
        answerC = "1918",
        answerD = "1923",
        correctAnswer = 1,
        explanation = "Die Titanic sank in der Nacht vom 14. auf den 15. April 1912 nach einer Kollision mit einem Eisberg im Nordatlantik.",
        difficulty = 1,
        funFact = "Die Titanic war auf ihrer Jungfernfahrt von Southampton nach New York, als sie auf den Eisberg traf."
    ),

    Question(
        categoryId = 3,
        questionText = "Womit kollidierte die Titanic und sank?",
        answerA = "Mit einem anderen Schiff",
        answerB = "Mit einem Felsen",
        answerC = "Mit einem Eisberg",
        answerD = "Mit einer Unterwassermine",
        correctAnswer = 2,
        explanation = "Die Titanic stieß am 14. April 1912 um 23:40 Uhr mit einem Eisberg zusammen und sank rund 2 Stunden und 40 Minuten später.",
        difficulty = 1,
        funFact = "Nur etwa 705 von 2.224 Menschen an Bord überlebten die Katastrophe."
    ),

    Question(
        categoryId = 3,
        questionText = "Die Titanic war auf dem Weg nach New York – von welchem europäischen Hafen startete sie?",
        answerA = "Hamburg",
        answerB = "Southampton",
        answerC = "Liverpool",
        answerD = "Le Havre",
        correctAnswer = 1,
        explanation = "Die Titanic brach am 10. April 1912 von Southampton (England) zu ihrer Jungfernfahrt nach New York auf.",
        difficulty = 1,
        funFact = "Die Titanic legte auf dem Weg noch in Cherbourg (Frankreich) und Queenstown (Irland) an, bevor sie den Atlantik überquerte."
    ),

    // --- Hindenburg Disaster ---

    Question(
        categoryId = 3,
        questionText = "Was war das Hindenburg-Unglück von 1937?",
        answerA = "Der Untergang eines Schlachtschiffs",
        answerB = "Ein Flugzeugabsturz über Berlin",
        answerC = "Der Brand und Absturz eines deutschen Luftschiffs",
        answerD = "Eine Eisenbahnkatastrophe",
        correctAnswer = 2,
        explanation = "Am 6. Mai 1937 fing das deutsche Luftschiff LZ 129 Hindenburg beim Landeanflug in Lakehurst, New Jersey, Feuer und wurde vollständig zerstört.",
        difficulty = 1,
        funFact = "36 Menschen kamen beim Unglück ums Leben. Die dramatischen Radioaufnahmen und Filmbilder machten das Ereignis weltberühmt."
    ),

    Question(
        categoryId = 3,
        questionText = "Mit welchem Gas war das Luftschiff Hindenburg gefüllt?",
        answerA = "Sauerstoff",
        answerB = "Helium",
        answerC = "Stickstoff",
        answerD = "Wasserstoff",
        correctAnswer = 3,
        explanation = "Die Hindenburg war mit hochexplosivem Wasserstoff gefüllt. Das leicht entzündliche Gas trug maßgeblich zur Katastrophe bei.",
        difficulty = 1,
        funFact = "Die USA weigerten sich, das sicherere Helium nach Deutschland zu exportieren, weshalb die Hindenburg auf Wasserstoff angewiesen war."
    ),

    // --- Pearl Harbor ---

    Question(
        categoryId = 3,
        questionText = "Wann griffen japanische Streitkräfte Pearl Harbor an?",
        answerA = "7. Dezember 1941",
        answerB = "6. Juni 1944",
        answerC = "1. September 1939",
        answerD = "8. Mai 1945",
        correctAnswer = 0,
        explanation = "Am 7. Dezember 1941 überraschten japanische Flugzeuge den US-Marinestützpunkt Pearl Harbor auf Hawaii mit einem massiven Angriff.",
        difficulty = 1,
        funFact = "US-Präsident Franklin D. Roosevelt bezeichnete den 7. Dezember 1941 als 'einen Tag, der in Schande weiterleben wird'."
    ),

    Question(
        categoryId = 3,
        questionText = "In welchem US-Bundesstaat liegt Pearl Harbor?",
        answerA = "Kalifornien",
        answerB = "Florida",
        answerC = "Hawaii",
        answerD = "Alaska",
        correctAnswer = 2,
        explanation = "Pearl Harbor liegt auf der Insel Oahu im US-Bundesstaat Hawaii und war 1941 ein wichtiger Marinestützpunkt der US-Pazifikflotte.",
        difficulty = 1,
        funFact = "Der Angriff auf Pearl Harbor veranlasste die USA zum Eintritt in den Zweiten Weltkrieg, sowohl gegen Japan als auch gegen Deutschland."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die direkte Folge des Angriffs auf Pearl Harbor für die USA?",
        answerA = "Die USA schlossen Frieden mit Japan",
        answerB = "Die USA traten in den Zweiten Weltkrieg ein",
        answerC = "Die USA erklärten die Neutralität",
        answerD = "Die USA bombardierten Tokio sofort",
        correctAnswer = 1,
        explanation = "Am Tag nach dem Angriff, dem 8. Dezember 1941, erklärten die USA Japan den Krieg und traten offiziell dem Zweiten Weltkrieg bei.",
        difficulty = 1,
        funFact = "Deutschland und Italien erklärten daraufhin den USA am 11. Dezember 1941 ebenfalls den Krieg."
    ),

    // --- D-Day ---

    Question(
        categoryId = 3,
        questionText = "Was bezeichnet man als 'D-Day' im Zweiten Weltkrieg?",
        answerA = "Den deutschen Angriff auf Polen",
        answerB = "Die Landung der Alliierten in der Normandie",
        answerC = "Den japanischen Angriff auf Pearl Harbor",
        answerD = "Den Abwurf der Atombombe auf Hiroshima",
        correctAnswer = 1,
        explanation = "D-Day bezeichnet die Landung der alliierten Streitkräfte in der Normandie (Nordfrankreich) am 6. Juni 1944, die größte Seelandung der Geschichte.",
        difficulty = 1,
        funFact = "Über 150.000 alliierte Soldaten landeten am D-Day innerhalb von 24 Stunden an den Stränden der Normandie."
    ),

    Question(
        categoryId = 3,
        questionText = "In welchem Jahr fand die Landung der Alliierten in der Normandie (D-Day) statt?",
        answerA = "1942",
        answerB = "1943",
        answerC = "1944",
        answerD = "1945",
        correctAnswer = 2,
        explanation = "Der D-Day fand am 6. Juni 1944 statt und markierte den Beginn der Befreiung Westeuropas von der deutschen Besatzung.",
        difficulty = 1,
        funFact = "Der Codename für die gesamte Normandie-Landungsoperation war 'Operation Overlord'."
    ),

    Question(
        categoryId = 3,
        questionText = "In welchem Land fand die alliierte Landung am D-Day statt?",
        answerA = "Deutschland",
        answerB = "Italien",
        answerC = "Belgien",
        answerD = "Frankreich",
        correctAnswer = 3,
        explanation = "Die Landung fand an den Stränden der Normandie in Nordfrankreich statt, damals unter deutscher Besatzung.",
        difficulty = 1,
        funFact = "Die fünf Landungsstrände trugen Codenamen: Utah, Omaha, Gold, Juno und Sword."
    ),

    // --- Korean War ---

    Question(
        categoryId = 3,
        questionText = "Wann begann der Koreakrieg?",
        answerA = "1945",
        answerB = "1950",
        answerC = "1953",
        answerD = "1960",
        correctAnswer = 1,
        explanation = "Der Koreakrieg begann am 25. Juni 1950, als Nordkorea die innerkoreanische Grenze überschritt und Südkorea angriff.",
        difficulty = 1,
        funFact = "Der Koreakrieg wird manchmal als 'vergessener Krieg' bezeichnet, weil er zwischen dem Zweiten Weltkrieg und dem Vietnamkrieg liegt."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche beiden Supermächte standen sich indirekt im Koreakrieg gegenüber?",
        answerA = "USA und China",
        answerB = "USA und Sowjetunion",
        answerC = "England und Frankreich",
        answerD = "Deutschland und Japan",
        correctAnswer = 1,
        explanation = "Der Koreakrieg war ein Stellvertreterkrieg des Kalten Krieges: Die USA unterstützten Südkorea, während die Sowjetunion (und China) Nordkorea unterstützten.",
        difficulty = 1,
        funFact = "China schickte im Herbst 1950 Hunderttausende 'Freiwillige' nach Nordkorea, um dem Norden zu helfen."
    ),

    // --- Vietnam War ---

    Question(
        categoryId = 3,
        questionText = "Gegen wen kämpften die USA hauptsächlich im Vietnamkrieg?",
        answerA = "Gegen China",
        answerB = "Gegen Nordvietnam und den Vietcong",
        answerC = "Gegen die Sowjetunion",
        answerD = "Gegen Japan",
        correctAnswer = 1,
        explanation = "Im Vietnamkrieg kämpften die USA und Südvietnam gegen den kommunistischen Norden (Nordvietnam) und die südvietnamesischen Guerillakämpfer, den Vietcong.",
        difficulty = 1,
        funFact = "Die USA setzten im Vietnamkrieg erstmals den Entlaubungsmittel 'Agent Orange' ein, der bis heute gesundheitliche Schäden verursacht."
    ),

    Question(
        categoryId = 3,
        questionText = "In welchem Jahr zogen sich die US-Truppen aus Vietnam zurück?",
        answerA = "1968",
        answerB = "1971",
        answerC = "1973",
        answerD = "1975",
        correctAnswer = 2,
        explanation = "Nach dem Pariser Friedensabkommen vom Januar 1973 zogen sich die letzten US-Kampftruppen aus Vietnam zurück.",
        difficulty = 1,
        funFact = "Der endgültige Fall Saigons und die Wiedervereinigung Vietnams unter kommunistischer Herrschaft erfolgten am 30. April 1975."
    ),

    Question(
        categoryId = 3,
        questionText = "Wie hieß die Hauptstadt Südvietnams, die 1975 von Nordvietnam eingenommen wurde?",
        answerA = "Hanoi",
        answerB = "Saigon",
        answerC = "Ho-Chi-Minh-Stadt",
        answerD = "Phnom Penh",
        correctAnswer = 1,
        explanation = "Saigon (heute Ho-Chi-Minh-Stadt) war die Hauptstadt Südvietnams und fiel am 30. April 1975 an die nordvietnamesischen Truppen.",
        difficulty = 1,
        funFact = "Nach der Einnahme durch den Norden wurde Saigon zu Ehren des nordvietnamesischen Führers Ho Chi Minh umbenannt."
    ),

    // --- Apartheid and Mandela ---

    Question(
        categoryId = 3,
        questionText = "Was war die Apartheid in Südafrika?",
        answerA = "Ein Bürgerkrieg zwischen Stämmen",
        answerB = "Ein System der Rassentrennung",
        answerC = "Eine religiöse Bewegung",
        answerD = "Ein Wirtschaftsboykott",
        correctAnswer = 1,
        explanation = "Apartheid (afrikaans für 'Getrenntheit') war das System der gesetzlich verankerten Rassentrennung in Südafrika von 1948 bis 1991.",
        difficulty = 1,
        funFact = "Unter der Apartheid waren Menschen nach ihrer Hautfarbe in Kategorien eingeteilt: Weiße, Schwarze, Coloureds und Asiaten."
    ),

    Question(
        categoryId = 3,
        questionText = "Wie viele Jahre war Nelson Mandela im Gefängnis?",
        answerA = "10 Jahre",
        answerB = "18 Jahre",
        answerC = "27 Jahre",
        answerD = "35 Jahre",
        correctAnswer = 2,
        explanation = "Nelson Mandela verbrachte 27 Jahre im Gefängnis, hauptsächlich auf der Gefängnisinsel Robben Island, bevor er 1990 freigelassen wurde.",
        difficulty = 1,
        funFact = "Mandela wurde 1964 zu lebenslanger Haft verurteilt – ursprünglich drohte ihm die Todesstrafe."
    ),

    Question(
        categoryId = 3,
        questionText = "Welchen Preis erhielt Nelson Mandela im Jahr 1993?",
        answerA = "Den Pulitzer-Preis",
        answerB = "Den Friedensnobelpreis",
        answerC = "Den Sakharov-Preis",
        answerD = "Den Gandhi-Preis",
        correctAnswer = 1,
        explanation = "Nelson Mandela und F.W. de Klerk erhielten 1993 gemeinsam den Friedensnobelpreis für ihre Bemühungen um ein friedliches Ende der Apartheid.",
        difficulty = 1,
        funFact = "Mandela wurde 1994 zum ersten schwarzen Präsidenten Südafrikas gewählt – bei der ersten Wahl, an der alle Südafrikaner teilnehmen durften."
    ),

    // --- Irish History ---

    Question(
        categoryId = 3,
        questionText = "Wann erlangte Irland (als Irischer Freistaat) seine Unabhängigkeit von Großbritannien?",
        answerA = "1916",
        answerB = "1921",
        answerC = "1935",
        answerD = "1949",
        correctAnswer = 1,
        explanation = "Der Anglo-Irische Vertrag von 1921 führte zur Gründung des Irischen Freistaats, der am 6. Dezember 1922 offiziell ausgerufen wurde.",
        difficulty = 1,
        funFact = "Der Osteraufstand von 1916 in Dublin war der entscheidende Funke, der die irische Unabhängigkeitsbewegung befeuerte."
    ),

    Question(
        categoryId = 3,
        questionText = "Wie heißt der Aufstand der Iren gegen britische Herrschaft, der am Ostermontag 1916 ausbrach?",
        answerA = "Die Irische Revolution",
        answerB = "Der Osteraufstand",
        answerC = "Die Grüne Rebellion",
        answerD = "Der Dublin-Aufstand",
        correctAnswer = 1,
        explanation = "Der Osteraufstand (Easter Rising) am 24. April 1916 war eine Erhebung irischer Nationalisten gegen die britische Herrschaft in Dublin.",
        difficulty = 1,
        funFact = "Die Anführer des Osteraufstands wurden nach dessen Niederschlagung von den Briten hingerichtet, was viele Iren in Nationalisten verwandelte."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche verheerende Hungersnot traf Irland im 19. Jahrhundert und führte zu massiver Auswanderung?",
        answerA = "Die Große Hungersnot (1845–1852)",
        answerB = "Die Irische Dürre (1820–1825)",
        answerC = "Die Schwarze Hungersnot (1870–1875)",
        answerD = "Die Atlantische Hungersnot (1890–1895)",
        correctAnswer = 0,
        explanation = "Die Große Hungersnot (auch 'Große Kartoffelhungersnot') von 1845–1852 tötete etwa eine Million Menschen und trieb weitere Millionen zur Auswanderung.",
        difficulty = 1,
        funFact = "Irlands Bevölkerung sank durch Hunger und Auswanderung von etwa 8 Millionen (1841) auf unter 6 Millionen (1851)."
    ),

    // --- Scottish History ---

    Question(
        categoryId = 3,
        questionText = "Wer war der schottische Freiheitskämpfer, der in der Schlacht von Stirling Bridge (1297) die Engländer besiegte?",
        answerA = "Robert the Bruce",
        answerB = "William Wallace",
        answerC = "John Balliol",
        answerD = "James IV.",
        correctAnswer = 1,
        explanation = "William Wallace führte die Schotten in der Schlacht von Stirling Bridge am 11. September 1297 zu einem bedeutenden Sieg gegen die englischen Truppen.",
        difficulty = 1,
        funFact = "William Wallace wurde durch Mel Gibsons Film 'Braveheart' (1995) weltweit bekannt, obwohl der Film historisch nicht sehr genau ist."
    ),

    Question(
        categoryId = 3,
        questionText = "In welchem Jahr vereinigten sich England und Schottland zum Königreich Großbritannien?",
        answerA = "1603",
        answerB = "1707",
        answerC = "1801",
        answerD = "1832",
        correctAnswer = 1,
        explanation = "Der Act of Union von 1707 vereinigte die Königreiche England und Schottland zum Königreich Großbritannien.",
        difficulty = 1,
        funFact = "Bereits 1603 hatte König Jakob VI. von Schottland den englischen Thron als Jakob I. bestiegen, aber die formelle Union kam erst 1707."
    ),

    Question(
        categoryId = 3,
        questionText = "Wie hieß die schottische Königin, die 1587 in England hingerichtet wurde?",
        answerA = "Maria Theresia",
        answerB = "Maria, Königin von Schottland",
        answerC = "Isabella von Kastilien",
        answerD = "Anne Boleyn",
        correctAnswer = 1,
        explanation = "Maria Stuart, Königin von Schottland, wurde am 8. Februar 1587 auf Befehl ihrer Cousine Königin Elisabeth I. von England hingerichtet.",
        difficulty = 1,
        funFact = "Maria Stuart floh 1568 nach England und hoffte auf Schutz von Elisabeth I. – stattdessen wurde sie fast 20 Jahre lang gefangen gehalten."
    ),

    // --- Russian Revolution ---

    Question(
        categoryId = 3,
        questionText = "In welchem Jahr fand die Russische Revolution statt, die die Zaren-Herrschaft beendete?",
        answerA = "1905",
        answerB = "1914",
        answerC = "1917",
        answerD = "1921",
        correctAnswer = 2,
        explanation = "Im Jahr 1917 gab es zwei Revolutionen in Russland: Die Februarrevolution beendete die Zarenherrschaft, die Oktoberrevolution brachte die Bolschewiki an die Macht.",
        difficulty = 1,
        funFact = "Die Russische Revolution von 1917 führte schließlich zur Gründung der Sowjetunion im Jahr 1922."
    ),

    Question(
        categoryId = 3,
        questionText = "Wer führte die Bolschewiki während der Russischen Revolution von 1917 an?",
        answerA = "Josef Stalin",
        answerB = "Leo Trotzki",
        answerC = "Wladimir Lenin",
        answerD = "Nikolaus II.",
        correctAnswer = 2,
        explanation = "Wladimir Lenin war der Anführer der Bolschewiki und die treibende Kraft hinter der Oktoberrevolution 1917, die die Bolschewiki an die Macht brachte.",
        difficulty = 1,
        funFact = "Lenin kehrte aus dem Exil in der Schweiz im April 1917 mit Hilfe eines deutschen 'plombierten Zuges' nach Russland zurück."
    ),

    Question(
        categoryId = 3,
        questionText = "Wie hieß der letzte russische Zar, der nach der Revolution 1918 hingerichtet wurde?",
        answerA = "Alexander III.",
        answerB = "Nikolaus II.",
        answerC = "Peter der Große",
        answerD = "Iwan IV.",
        correctAnswer = 1,
        explanation = "Zar Nikolaus II. wurde zusammen mit seiner Familie am 17. Juli 1918 in Jekaterinburg von Bolschewiki erschossen.",
        difficulty = 1,
        funFact = "Die Leichen der Zarenfamilie wurden jahrzehntelang versteckt und erst 1991 gefunden und identifiziert."
    ),

    // --- Ottoman Empire ---

    Question(
        categoryId = 3,
        questionText = "Wann endete das Osmanische Reich offiziell?",
        answerA = "1912",
        answerB = "1918",
        answerC = "1922",
        answerD = "1930",
        correctAnswer = 2,
        explanation = "Das Osmanische Reich wurde 1922 offiziell abgeschafft, als das türkische Parlament das Sultanat abschaffte und die Republik Türkei ausgerufen wurde.",
        difficulty = 1,
        funFact = "Das Osmanische Reich hatte rund 600 Jahre Bestand – von seiner Gründung ca. 1299 bis zu seinem Ende 1922."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Stadt war die Hauptstadt des Osmanischen Reiches?",
        answerA = "Bagdad",
        answerB = "Kairo",
        answerC = "Ankara",
        answerD = "Konstantinopel",
        correctAnswer = 3,
        explanation = "Konstantinopel (das heutige Istanbul) war ab 1453 die Hauptstadt des Osmanischen Reiches, nachdem Sultan Mehmed II. die Stadt erobert hatte.",
        difficulty = 1,
        funFact = "Konstantinopel wurde 1930 offiziell in Istanbul umbenannt."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher osmanische Sultan eroberte 1453 Konstantinopel und beendete damit das Byzantinische Reich?",
        answerA = "Süleyman der Prächtige",
        answerB = "Selim I.",
        answerC = "Mehmed II.",
        answerD = "Bayezid I.",
        correctAnswer = 2,
        explanation = "Sultan Mehmed II. (auch 'der Eroberer' genannt) nahm am 29. Mai 1453 Konstantinopel ein und beendete damit das Oströmische (Byzantinische) Reich.",
        difficulty = 1,
        funFact = "Mehmed II. war bei der Eroberung Konstantinopels erst 21 Jahre alt."
    ),

    // --- Mongol Empire ---

    Question(
        categoryId = 3,
        questionText = "Wer gründete das Mongolische Reich?",
        answerA = "Kublai Khan",
        answerB = "Timur Lenk",
        answerC = "Dschingis Khan",
        answerD = "Ögedei Khan",
        correctAnswer = 2,
        explanation = "Dschingis Khan (eigentlicher Name: Temüdschin) vereinte die mongolischen Stämme und gründete das Mongolische Reich, das er ab 1206 regierte.",
        difficulty = 1,
        funFact = "Dschingis Khan bedeutet 'Universeller Herrscher' oder 'Ozean-Herrscher' in der mongolischen Sprache."
    ),

    Question(
        categoryId = 3,
        questionText = "Das Mongolische Reich war das größte zusammenhängende Landreich der Geschichte. Auf wie viel Prozent der Landmasse der Erde erstreckte es sich auf seinem Höhepunkt?",
        answerA = "Ca. 5 %",
        answerB = "Ca. 10 %",
        answerC = "Ca. 22 %",
        answerD = "Ca. 35 %",
        correctAnswer = 2,
        explanation = "Auf seinem Höhepunkt um 1279 umfasste das Mongolische Reich ca. 24 Millionen Quadratkilometer – etwa 22 % der Landfläche der Erde.",
        difficulty = 1,
        funFact = "Das Mongolische Reich war so groß, dass man in ihm von Polen bis Korea reisen konnte, ohne eine Grenze zu überschreiten."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher Enkel von Dschingis Khan gründete die Yuan-Dynastie in China?",
        answerA = "Hülegü Khan",
        answerB = "Kublai Khan",
        answerC = "Batu Khan",
        answerD = "Berke Khan",
        correctAnswer = 1,
        explanation = "Kublai Khan, Enkel von Dschingis Khan, gründete 1271 die Yuan-Dynastie und herrschte über ganz China. Er empfing auch den venezianischen Reisenden Marco Polo.",
        difficulty = 1,
        funFact = "Kublai Khan versuchte zweimal, Japan zu erobern (1274 und 1281), scheiterte aber beide Male an Taifunen – den Japanern als 'Kamikaze' (Göttlicher Wind) bekannt."
    ),

    Question(
        categoryId = 3,
        questionText = "Wie hieß das schnelle Postsystem, das die Mongolen zur Kommunikation über ihr riesiges Reich nutzten?",
        answerA = "Karawanserei",
        answerB = "Yam",
        answerC = "Seidenpost",
        answerD = "Tschapar",
        correctAnswer = 1,
        explanation = "Das 'Yam'-System war ein ausgeklügeltes Staffelreiternetz mit Stationen alle 30–40 km, das Nachrichten und Güter schnell durchs Reich beförderte.",
        difficulty = 1,
        funFact = "Dank des Yam-Systems konnten Nachrichten im Mongolischen Reich Tausende Kilometer in wenigen Tagen zurücklegen – eine bemerkenswerte Leistung für das 13. Jahrhundert."
    ),

    // --- Additional questions to complete 50 ---

    Question(
        categoryId = 3,
        questionText = "Wie viele Landungsstrände gab es beim D-Day in der Normandie?",
        answerA = "Drei",
        answerB = "Vier",
        answerC = "Fünf",
        answerD = "Sieben",
        correctAnswer = 2,
        explanation = "Beim D-Day am 6. Juni 1944 landeten alliierte Truppen an fünf Stränden: Utah, Omaha (USA), Gold, Sword (Großbritannien) und Juno (Kanada).",
        difficulty = 1,
        funFact = "Der Strand Omaha war der blutigste: Allein dort fielen in wenigen Stunden über 2.000 US-Soldaten."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Schrift verwendeten die alten Römer, die bis heute die Grundlage des westlichen Alphabets bildet?",
        answerA = "Kyrillische Schrift",
        answerB = "Lateinische Schrift",
        answerC = "Griechische Schrift",
        answerD = "Etruskische Schrift",
        correctAnswer = 1,
        explanation = "Die lateinische Schrift der Römer ist die Grundlage des westlichen Alphabets und wird heute von Milliarden Menschen weltweit verwendet.",
        difficulty = 1,
        funFact = "Das lateinische Alphabet entwickelte sich aus dem griechischen Alphabet, das wiederum aus dem phönizischen Alphabet hervorgegangen ist."
    ),

    Question(
        categoryId = 3,
        questionText = "Wo befand sich das Gefängnis, in dem Nelson Mandela den größten Teil seiner Haft verbrachte?",
        answerA = "Kapstadt",
        answerB = "Johannesburg",
        answerC = "Robben Island",
        answerD = "Durban",
        correctAnswer = 2,
        explanation = "Nelson Mandela verbrachte 18 seiner 27 Gefängnisjahre auf Robben Island, einer Insel vor Kapstadt, in einer kleinen Zelle.",
        difficulty = 1,
        funFact = "Mandelas Zelle auf Robben Island ist heute ein Museum und Teil des UNESCO-Weltkulturerbes."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches osmanische Bauwerk in Istanbul, ursprünglich eine christliche Kathedrale, wurde 1453 in eine Moschee umgewandelt?",
        answerA = "Der Topkapi-Palast",
        answerB = "Die Blaue Moschee",
        answerC = "Die Hagia Sophia",
        answerD = "Der Dolmabahçe-Palast",
        correctAnswer = 2,
        explanation = "Die Hagia Sophia, erbaut 537 n. Chr. als christliche Kathedrale, wurde nach der osmanischen Eroberung Konstantinopels 1453 in eine Moschee umgewandelt.",
        difficulty = 1,
        funFact = "Von 1934 bis 2020 war die Hagia Sophia ein Museum; 2020 wurde sie erneut zur Moschee erklärt."
    ),

)
