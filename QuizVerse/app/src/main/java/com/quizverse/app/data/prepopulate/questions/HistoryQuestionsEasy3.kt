package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun historyQuestionsEasy3(): List<Question> = listOf(

    // --- Olympic Games History ---

    Question(
        categoryId = 3,
        questionText = "In welchem Jahr fanden die ersten modernen Olympischen Spiele statt?",
        answerA = "1892",
        answerB = "1896",
        answerC = "1900",
        answerD = "1904",
        correctAnswer = 1,
        explanation = "Die ersten modernen Olympischen Spiele fanden 1896 in Athen statt und wurden vom Franzosen Pierre de Coubertin initiiert.",
        difficulty = 1,
        funFact = "An den ersten modernen Spielen nahmen 241 Athleten aus 14 Nationen teil."
    ),

    Question(
        categoryId = 3,
        questionText = "In welcher Stadt fanden die Olympischen Sommerspiele 1936 statt?",
        answerA = "Wien",
        answerB = "München",
        answerC = "Berlin",
        answerD = "Hamburg",
        correctAnswer = 2,
        explanation = "Die Olympischen Sommerspiele 1936 fanden in Berlin statt und wurden vom nationalsozialistischen Deutschland als Propagandaveranstaltung genutzt.",
        difficulty = 1,
        funFact = "Der afroamerikanische Leichtathlet Jesse Owens gewann vier Goldmedaillen und widerlegte damit Hitlers Rassenideologie eindrucksvoll."
    ),

    Question(
        categoryId = 3,
        questionText = "In welchem Jahr fanden die Olympischen Sommerspiele in München statt?",
        answerA = "1968",
        answerB = "1972",
        answerC = "1976",
        answerD = "1980",
        correctAnswer = 1,
        explanation = "Die Olympischen Sommerspiele 1972 fanden in München statt und sind wegen des Terroranschlags auf die israelische Mannschaft in die Geschichte eingegangen.",
        difficulty = 1,
        funFact = "US-Schwimmer Mark Spitz gewann bei diesen Spielen sieben Goldmedaillen – ein damaliger Rekord."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Symbol steht im Mittelpunkt der Olympischen Ringe?",
        answerA = "Der blaue Ring",
        answerB = "Der schwarze Ring",
        answerC = "Der rote Ring",
        answerD = "Der gelbe Ring",
        correctAnswer = 0,
        explanation = "Die olympischen Ringe bestehen aus fünf ineinander verschlungenen Ringen in den Farben Blau, Gelb, Schwarz, Grün und Rot auf weißem Grund.",
        difficulty = 1,
        funFact = "Die fünf Ringe symbolisieren die fünf Kontinente der Welt, die durch die Olympische Bewegung vereint sind."
    ),

    Question(
        categoryId = 3,
        questionText = "Wer entzündet traditionell das Olympische Feuer zu Beginn jeder Spieler?",
        answerA = "Das Staatsoberhaupt des Gastgeberlandes",
        answerB = "Der Vorsitzende des IOC",
        answerC = "Der letzte Fackelläufer nach dem Staffellauf",
        answerD = "Der Gewinner des Marathon der vorherigen Spiele",
        correctAnswer = 2,
        explanation = "Das Olympische Feuer wird traditionell vom letzten Fackelläufer nach einem langen Staffellauf entzündet, der in Olympia in Griechenland beginnt.",
        difficulty = 1,
        funFact = "Die Idee des modernen Fackellaufs wurde erst bei den Berliner Spielen 1936 eingeführt."
    ),

    // --- Space Exploration ---

    Question(
        categoryId = 3,
        questionText = "Wer war der erste Mensch im Weltall?",
        answerA = "Neil Armstrong",
        answerB = "Buzz Aldrin",
        answerC = "Juri Gagarin",
        answerD = "Alan Shepard",
        correctAnswer = 2,
        explanation = "Der sowjetische Kosmonaut Juri Gagarin war am 12. April 1961 der erste Mensch im Weltall. Er umkreiste die Erde einmal in 108 Minuten.",
        difficulty = 1,
        funFact = "Gagarin war von Beruf Gießereiarbeiter, bevor er Kampfpilot und schließlich Kosmonaut wurde."
    ),

    Question(
        categoryId = 3,
        questionText = "In welchem Jahr landeten die ersten Menschen auf dem Mond?",
        answerA = "1966",
        answerB = "1967",
        answerC = "1969",
        answerD = "1971",
        correctAnswer = 2,
        explanation = "Am 20. Juli 1969 landeten Neil Armstrong und Buzz Aldrin im Rahmen der Apollo-11-Mission als erste Menschen auf dem Mond.",
        difficulty = 1,
        funFact = "Neil Armstrong sagte beim ersten Schritt auf den Mond: \"Das ist ein kleiner Schritt für einen Menschen, aber ein riesiger Sprung für die Menschheit.\""
    ),

    Question(
        categoryId = 3,
        questionText = "Wie hieß die NASA-Mission, die als erstes Menschen auf den Mond brachte?",
        answerA = "Apollo 9",
        answerB = "Apollo 11",
        answerC = "Apollo 13",
        answerD = "Gemini 12",
        correctAnswer = 1,
        explanation = "Apollo 11 war die erste bemannte Mondlandungsmission. Sie startete am 16. Juli 1969 und landete am 20. Juli auf dem Mond.",
        difficulty = 1,
        funFact = "Michael Collins blieb während der Mondlandung im Kommandomodul in der Mondumlaufbahn und flog nie selbst auf den Mond."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Raumkapsel hatte Juri Gagarin bei seinem historischen Flug?",
        answerA = "Sputnik 1",
        answerB = "Sojus 1",
        answerC = "Wostok 1",
        answerD = "Mir 1",
        correctAnswer = 2,
        explanation = "Juri Gagarin flog am 12. April 1961 in der Raumkapsel Wostok 1 ins All. Das Wort \"Wostok\" bedeutet auf Russisch \"Osten\".",
        difficulty = 1,
        funFact = "Gagarin landete nicht in der Kapsel, sondern schleuderte sich in 7 km Höhe heraus und landete mit einem Fallschirm."
    ),

    Question(
        categoryId = 3,
        questionText = "Wer war der erste Mensch, der den Mond betrat?",
        answerA = "Buzz Aldrin",
        answerB = "Michael Collins",
        answerC = "Alan Shepard",
        answerD = "Neil Armstrong",
        correctAnswer = 3,
        explanation = "Neil Armstrong betrat am 20. Juli 1969 als erster Mensch den Mond, kurz vor seinem Kollegen Buzz Aldrin.",
        difficulty = 1,
        funFact = "Armstrong und Aldrin verbrachten insgesamt etwa 2,5 Stunden auf der Mondoberfläche."
    ),

    // --- Berlin History ---

    Question(
        categoryId = 3,
        questionText = "In welchem Jahr wurde die Berliner Mauer gebaut?",
        answerA = "1955",
        answerB = "1958",
        answerC = "1961",
        answerD = "1963",
        correctAnswer = 2,
        explanation = "Der Bau der Berliner Mauer begann in der Nacht vom 12. auf den 13. August 1961. Die DDR wollte die Massenflucht in den Westen stoppen.",
        difficulty = 1,
        funFact = "In der Nacht des Mauerbaus wurden viele Familien plötzlich voneinander getrennt und sahen sich jahrelang nicht wieder."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher US-Präsident sagte 1963 den berühmten Satz \"Ich bin ein Berliner\"?",
        answerA = "Dwight D. Eisenhower",
        answerB = "Lyndon B. Johnson",
        answerC = "John F. Kennedy",
        answerD = "Richard Nixon",
        correctAnswer = 2,
        explanation = "John F. Kennedy hielt diese berühmte Rede am 26. Juni 1963 vor dem Rathaus Schöneberg in West-Berlin als Zeichen der Solidarität mit der geteilten Stadt.",
        difficulty = 1,
        funFact = "Die Rede wurde vor über 400.000 begeisterten Zuschauern gehalten und ist eine der berühmtesten Reden des Kalten Krieges."
    ),

    Question(
        categoryId = 3,
        questionText = "Was ist das bekannteste historische Tor Berlins?",
        answerA = "Hallesches Tor",
        answerB = "Brandenburger Tor",
        answerC = "Oranienburger Tor",
        answerD = "Schlesisches Tor",
        correctAnswer = 1,
        explanation = "Das Brandenburger Tor wurde 1788–1791 erbaut und ist das bekannteste Wahrzeichen Berlins. Während der Teilung lag es im Sperrgebiet zwischen Ost und West.",
        difficulty = 1,
        funFact = "Die Quadriga auf dem Brandenburger Tor wurde nach Napoleons Sieg über Preußen zeitweise nach Paris gebracht und erst nach Napoleons Niederlage zurückgeholt."
    ),

    Question(
        categoryId = 3,
        questionText = "Wann wurde Berlin zur Hauptstadt des wiedervereinigten Deutschlands?",
        answerA = "1989",
        answerB = "1990",
        answerC = "1999",
        answerD = "2001",
        correctAnswer = 1,
        explanation = "Mit der Deutschen Wiedervereinigung am 3. Oktober 1990 wurde Berlin zur Hauptstadt des vereinten Deutschlands, obwohl Parlament und Regierung erst 1999 von Bonn nach Berlin zogen.",
        difficulty = 1,
        funFact = "Von 1949 bis 1990 war Bonn die Hauptstadt Westdeutschlands, während Ost-Berlin als Hauptstadt der DDR galt."
    ),

    Question(
        categoryId = 3,
        questionText = "In welchem Krieg wurde Berlin 1945 stark zerstört?",
        answerA = "Erster Weltkrieg",
        answerB = "Dreißigjähriger Krieg",
        answerC = "Zweiter Weltkrieg",
        answerD = "Siebenjähriger Krieg",
        correctAnswer = 2,
        explanation = "Im Zweiten Weltkrieg wurde Berlin durch alliierte Bombenangriffe und die abschließende Schlacht um Berlin 1945 schwer zerstört.",
        difficulty = 1,
        funFact = "Nach Ende des Zweiten Weltkriegs wurde Berlin in vier Besatzungszonen aufgeteilt, die von den USA, der Sowjetunion, Großbritannien und Frankreich verwaltet wurden."
    ),

    // --- Habsburg Dynasty ---

    Question(
        categoryId = 3,
        questionText = "Welches Kaiserhaus regierte Österreich-Ungarn bis 1918?",
        answerA = "Die Hohenzollern",
        answerB = "Die Wittelsbacher",
        answerC = "Die Habsburger",
        answerD = "Die Welfen",
        correctAnswer = 2,
        explanation = "Die Habsburger (auch Habsburg-Lothringen) regierten Österreich-Ungarn bis zum Ende des Ersten Weltkriegs 1918. Karl I. war der letzte österreichische Kaiser.",
        difficulty = 1,
        funFact = "Die Habsburger regierten über 600 Jahre und waren zeitweise das mächtigste Herrscherhaus Europas."
    ),

    Question(
        categoryId = 3,
        questionText = "Wer war die berühmteste Kaiserin des Hauses Habsburg im 19. Jahrhundert?",
        answerA = "Maria Theresia",
        answerB = "Elisabeth (Sisi)",
        answerC = "Sophie von Bayern",
        answerD = "Marie Antoinette",
        correctAnswer = 1,
        explanation = "Kaiserin Elisabeth, liebevoll \"Sisi\" genannt, war die Gemahlin von Kaiser Franz Joseph I. und ist eine der bekanntesten Persönlichkeiten der Habsburger Geschichte.",
        difficulty = 1,
        funFact = "Sisi wurde 1898 in Genf von einem italienischen Anarchisten ermordet. Trotz der Stichwunde lief sie zunächst noch weiter, bevor sie zusammenbrach."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Habsburgerin regierte im 18. Jahrhundert als Erzherzogin von Österreich?",
        answerA = "Maria Theresia",
        answerB = "Elisabeth von Habsburg",
        answerC = "Kaiserin Sissi",
        answerD = "Maria Antonia",
        correctAnswer = 0,
        explanation = "Maria Theresia regierte von 1740 bis 1780 als Erzherzogin von Österreich und war eine der bedeutendsten Herrscherinnen der Habsburger Dynastie.",
        difficulty = 1,
        funFact = "Maria Theresia hatte 16 Kinder, darunter Marie Antoinette, die spätere Königin von Frankreich."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Stadt war die Hauptresidenz der Habsburger?",
        answerA = "Budapest",
        answerB = "Prag",
        answerC = "Wien",
        answerD = "Salzburg",
        correctAnswer = 2,
        explanation = "Wien war die Hauptresidenz der Habsburger und blieb es über Jahrhunderte. Der Wiener Hofburg war das Zentrum ihrer Macht.",
        difficulty = 1,
        funFact = "Das Schloss Schönbrunn in Wien war die bevorzugte Sommerresidenz der Habsburger und hat 1.441 Zimmer."
    ),

    Question(
        categoryId = 3,
        questionText = "Wann endete die Herrschaft der Habsburger in Österreich?",
        answerA = "1914",
        answerB = "1916",
        answerC = "1918",
        answerD = "1920",
        correctAnswer = 2,
        explanation = "Mit der Niederlage im Ersten Weltkrieg und der Ausrufung der Republik Österreich am 12. November 1918 endete die Habsburgerherrschaft.",
        difficulty = 1,
        funFact = "Kaiser Karl I. verzichtete auf die Staatsgeschäfte, lehnte aber eine formelle Abdankung ab. Er starb 1922 im Exil auf Madeira."
    ),

    // --- Holy Roman Empire ---

    Question(
        categoryId = 3,
        questionText = "In welchem Jahr wurde das Heilige Römische Reich durch Napoleon aufgelöst?",
        answerA = "1800",
        answerB = "1804",
        answerC = "1806",
        answerD = "1815",
        correctAnswer = 2,
        explanation = "Am 6. August 1806 legte Kaiser Franz II. die Kaiserkrone nieder und löste damit das Heilige Römische Reich Deutscher Nation auf.",
        difficulty = 1,
        funFact = "Das Heilige Römische Reich bestand über 800 Jahre lang, von der Krönung Karls des Großen 800 bis zur Auflösung 1806."
    ),

    Question(
        categoryId = 3,
        questionText = "Wer war der erste Kaiser des Heiligen Römischen Reiches?",
        answerA = "Otto I.",
        answerB = "Karl der Große",
        answerC = "Heinrich I.",
        answerD = "Friedrich Barbarossa",
        correctAnswer = 1,
        explanation = "Karl der Große wurde am 25. Dezember 800 in Rom von Papst Leo III. zum Kaiser des Abendlandes gekrönt – dies gilt als Gründungsmoment des Heiligen Römischen Reiches.",
        difficulty = 1,
        funFact = "Karl der Große sprach mehrere Sprachen, konnte aber zeitlebens kaum schreiben und lernte es erst im Alter."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher Kaiser des Heiligen Römischen Reiches wurde \"Barbarossa\" (Rotbart) genannt?",
        answerA = "Heinrich IV.",
        answerB = "Friedrich I.",
        answerC = "Karl IV.",
        answerD = "Otto III.",
        correctAnswer = 1,
        explanation = "Friedrich I. wurde wegen seines roten Bartes \"Barbarossa\" genannt. Er regierte von 1155 bis 1190 als Kaiser und war einer der bedeutendsten mittelalterlichen Herrscher.",
        difficulty = 1,
        funFact = "Friedrich Barbarossa ertrank 1190 im Fluss Saleph in der heutigen Türkei auf dem Weg zum Dritten Kreuzzug."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Stadt war die wichtigste Krönungsstadt der Kaiser des Heiligen Römischen Reiches?",
        answerA = "Wien",
        answerB = "Berlin",
        answerC = "Frankfurt am Main",
        answerD = "Aachen",
        correctAnswer = 2,
        explanation = "Frankfurt am Main war ab dem 16. Jahrhundert die Krönungsstadt der Kaiser. Im Frankfurter Dom wurden von 1562 bis 1792 zehn Kaiser gekrönt.",
        difficulty = 1,
        funFact = "Aachen war die Krönungsstadt für die frühen Kaiser des Mittelalters, da Karl der Große dort seinen Hauptsitz hatte."
    ),

    // --- Weimar Republic ---

    Question(
        categoryId = 3,
        questionText = "In welchem Jahr wurde die Weimarer Republik gegründet?",
        answerA = "1914",
        answerB = "1916",
        answerC = "1918",
        answerD = "1920",
        correctAnswer = 2,
        explanation = "Die Weimarer Republik wurde nach dem Ende des Ersten Weltkriegs 1918 gegründet. Philipp Scheidemann rief am 9. November 1918 die Republik aus.",
        difficulty = 1,
        funFact = "Die Weimarer Republik war die erste parlamentarische Demokratie in Deutschland."
    ),

    Question(
        categoryId = 3,
        questionText = "In welcher Stadt wurde die Verfassung der Weimarer Republik verabschiedet?",
        answerA = "Berlin",
        answerB = "Frankfurt",
        answerC = "Weimar",
        answerD = "Dresden",
        correctAnswer = 2,
        explanation = "Die Verfassung der ersten deutschen Republik wurde in Weimar verabschiedet, da Berlin wegen politischer Unruhen als zu unsicher galt – daher der Name \"Weimarer Republik\".",
        difficulty = 1,
        funFact = "Weimar war die Stadt Goethes und Schillers und galt als kulturelles Herzstück Deutschlands."
    ),

    Question(
        categoryId = 3,
        questionText = "Wann endete die Weimarer Republik?",
        answerA = "1930",
        answerB = "1931",
        answerC = "1933",
        answerD = "1935",
        correctAnswer = 2,
        explanation = "Die Weimarer Republik endete am 30. Januar 1933, als Adolf Hitler zum Reichskanzler ernannt wurde und die Nationalsozialisten die Macht übernahmen.",
        difficulty = 1,
        funFact = "Die Weimarer Republik bestand nur 14 Jahre – von 1919 bis 1933."
    ),

    // --- Famous Battles ---

    Question(
        categoryId = 3,
        questionText = "In welchem Jahr fand die Schlacht bei Waterloo statt?",
        answerA = "1812",
        answerB = "1813",
        answerC = "1814",
        answerD = "1815",
        correctAnswer = 3,
        explanation = "Die Schlacht bei Waterloo fand am 18. Juni 1815 statt. Napoleons Armee wurde dort von den alliierten Streitkräften unter Wellington und Blücher besiegt.",
        difficulty = 1,
        funFact = "Nach seiner Niederlage bei Waterloo wurde Napoleon auf die abgelegene Insel Sankt Helena verbannt, wo er 1821 starb."
    ),

    Question(
        categoryId = 3,
        questionText = "Wer besiegte Napoleon bei der Schlacht bei Waterloo?",
        answerA = "Kaiser Alexander I. von Russland",
        answerB = "Herzog von Wellington und Feldmarschall Blücher",
        answerC = "Erzherzog Karl von Österreich",
        answerD = "König Friedrich Wilhelm III. von Preußen",
        correctAnswer = 1,
        explanation = "Napoleon wurde bei Waterloo von den britischen Truppen unter dem Herzog von Wellington und den preußischen Truppen unter Feldmarschall Blücher besiegt.",
        difficulty = 1,
        funFact = "\"Sein Waterloo erleben\" ist heute ein geflügeltes Wort für eine vernichtende Niederlage."
    ),

    Question(
        categoryId = 3,
        questionText = "Wo fand die verlustreiche Winterschlacht des Zweiten Weltkriegs an der Ostfront statt?",
        answerA = "Leningrad",
        answerB = "Stalingrad",
        answerC = "Kiew",
        answerD = "Moskau",
        correctAnswer = 1,
        explanation = "Die Schlacht von Stalingrad (August 1942 – Februar 1943) war eine der verlustreichsten Schlachten des Zweiten Weltkriegs und gilt als Wendepunkt an der Ostfront.",
        difficulty = 1,
        funFact = "Die Stadt heißt heute Wolgograd. Über 800.000 Soldaten und Zivilisten kamen in dieser Winterschlacht ums Leben."
    ),

    Question(
        categoryId = 3,
        questionText = "In welchem Jahr fand die berühmte Seeschlacht bei Trafalgar statt?",
        answerA = "1800",
        answerB = "1803",
        answerC = "1805",
        answerD = "1807",
        correctAnswer = 2,
        explanation = "Die Seeschlacht bei Trafalgar fand am 21. Oktober 1805 statt. Admiral Nelson besiegte die französisch-spanische Flotte, starb aber selbst in der Schlacht.",
        difficulty = 1,
        funFact = "Nelsons letzte Worte sollen gewesen sein: \"Gott und mein Land, ich habe meine Pflicht getan.\""
    ),

    Question(
        categoryId = 3,
        questionText = "Bei welcher berühmten Seeschlacht besiegte Admiral Nelson Napoleon?",
        answerA = "Trafalgar",
        answerB = "Abukir",
        answerC = "Jutland",
        answerD = "Salamis",
        correctAnswer = 0,
        explanation = "Admiral Horatio Nelson besiegte die vereinte französisch-spanische Flotte in der Seeschlacht bei Trafalgar am 21. Oktober 1805 und sicherte damit die britische Seeherrschaft.",
        difficulty = 1,
        funFact = "Der Trafalgar Square in London ist nach dieser berühmten Seeschlacht benannt."
    ),

    // --- Historical Buildings ---

    Question(
        categoryId = 3,
        questionText = "In welcher Stadt befindet sich das Kolosseum?",
        answerA = "Athen",
        answerB = "Karthago",
        answerC = "Rom",
        answerD = "Alexandria",
        correctAnswer = 2,
        explanation = "Das Kolosseum befindet sich in Rom, Italien. Es ist das größte je erbaute Amphitheater und wurde um 70–80 n. Chr. errichtet.",
        difficulty = 1,
        funFact = "Das Kolosseum fasste bis zu 80.000 Zuschauer und war Schauplatz von Gladiatorenkämpfen und öffentlichen Spektakeln."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Weltwunder der Antike steht noch heute in Ägypten?",
        answerA = "Der Leuchtturm von Alexandria",
        answerB = "Die Pyramiden von Gizeh",
        answerC = "Der Koloss von Rhodos",
        answerD = "Die Hängenden Gärten von Babylon",
        correctAnswer = 1,
        explanation = "Die Pyramiden von Gizeh sind das einzige der sieben Weltwunder der Antike, das noch heute nahezu vollständig erhalten ist.",
        difficulty = 1,
        funFact = "Die Große Pyramide von Gizeh war fast 4.000 Jahre lang das höchste von Menschenhand errichtete Bauwerk der Welt."
    ),

    Question(
        categoryId = 3,
        questionText = "Für wen wurden die Pyramiden von Gizeh erbaut?",
        answerA = "Für die Götter der Ägypter",
        answerB = "Als Kornspeicher für Hungersnöte",
        answerC = "Als Grabmäler für ägyptische Pharaonen",
        answerD = "Als Tempel für den Sonnengott Ra",
        correctAnswer = 2,
        explanation = "Die Pyramiden wurden als monumentale Grabmäler für die ägyptischen Pharaonen erbaut. Die Große Pyramide ist das Grab des Pharaos Cheops.",
        difficulty = 1,
        funFact = "Für den Bau der Großen Pyramide wurden schätzungsweise 2,3 Millionen Steinblöcke mit einem Durchschnittsgewicht von 2,5 Tonnen verwendet."
    ),

    Question(
        categoryId = 3,
        questionText = "Was ist der Eiffelturm und in welcher Stadt steht er?",
        answerA = "Ein Kirchturm in Berlin",
        answerB = "Ein Aussichtsturm in Paris",
        answerC = "Ein Leuchtturm in London",
        answerD = "Ein Radioturm in Wien",
        correctAnswer = 1,
        explanation = "Der Eiffelturm ist ein Eisengitterturm in Paris, Frankreich. Er wurde 1887–1889 als temporäres Eingangstor zur Weltausstellung 1889 erbaut.",
        difficulty = 1,
        funFact = "Als er 1889 fertiggestellt wurde, war der Eiffelturm mit 300 Metern das höchste Bauwerk der Welt – für 41 Jahre, bis das Chrysler Building in New York gebaut wurde."
    ),

    Question(
        categoryId = 3,
        questionText = "In welchem Land steht die Chinesische Mauer?",
        answerA = "Japan",
        answerB = "Mongolei",
        answerC = "Korea",
        answerD = "China",
        correctAnswer = 3,
        explanation = "Die Chinesische Mauer ist ein gigantisches Verteidigungsbauwerk in China, das über viele Jahrhunderte erbaut wurde, um das Reich gegen Nomadenstämme aus dem Norden zu schützen.",
        difficulty = 1,
        funFact = "Entgegen einem weit verbreiteten Mythos ist die Chinesische Mauer vom Weltraum aus nicht mit bloßem Auge sichtbar."
    ),

    // --- UN Founding ---

    Question(
        categoryId = 3,
        questionText = "In welchem Jahr wurde die Vereinten Nationen (UN) gegründet?",
        answerA = "1943",
        answerB = "1944",
        answerC = "1945",
        answerD = "1948",
        correctAnswer = 2,
        explanation = "Die Vereinten Nationen wurden am 24. Oktober 1945 offiziell gegründet, nachdem 51 Gründungsstaaten die UN-Charta ratifiziert hatten.",
        difficulty = 1,
        funFact = "Der 24. Oktober wird seitdem jährlich als \"United Nations Day\" gefeiert."
    ),

    Question(
        categoryId = 3,
        questionText = "In welcher Stadt hat die UN ihren Hauptsitz?",
        answerA = "Washington D.C.",
        answerB = "London",
        answerC = "Genf",
        answerD = "New York City",
        correctAnswer = 3,
        explanation = "Der Hauptsitz der Vereinten Nationen befindet sich in New York City an der Ostseite von Manhattan. Das Gebäude wurde 1952 fertiggestellt.",
        difficulty = 1,
        funFact = "Das UN-Gelände in New York gilt als internationales Gebiet und untersteht nicht der Jurisdiktion der USA."
    ),

    Question(
        categoryId = 3,
        questionText = "Wie viele Gründungsstaaten hatten die Vereinten Nationen im Jahr 1945?",
        answerA = "26",
        answerB = "51",
        answerC = "72",
        answerD = "100",
        correctAnswer = 1,
        explanation = "Die UN wurden von 51 Gründungsstaaten ins Leben gerufen, die die UN-Charta unterzeichnet und ratifiziert haben.",
        difficulty = 1,
        funFact = "Heute sind fast alle Staaten der Welt Mitglied der Vereinten Nationen – insgesamt 193 Mitgliedsstaaten."
    ),

    Question(
        categoryId = 3,
        questionText = "Was ist das Hauptziel der Vereinten Nationen?",
        answerA = "Wirtschaftliche Dominanz westlicher Länder",
        answerB = "Weltweite militärische Kontrolle",
        answerC = "Frieden, Sicherheit und internationale Zusammenarbeit",
        answerD = "Einführung einer Weltregierung",
        correctAnswer = 2,
        explanation = "Die Vereinten Nationen wurden gegründet, um internationalen Frieden und Sicherheit zu wahren, die Zusammenarbeit zwischen Nationen zu fördern und Menschenrechte zu schützen.",
        difficulty = 1,
        funFact = "Die UN haben 6 Hauptorgane: Generalversammlung, Sicherheitsrat, Sekretariat, Internationaler Gerichtshof, Treuhandrat und Wirtschafts- und Sozialrat."
    ),

    // --- EU History ---

    Question(
        categoryId = 3,
        questionText = "In welchem Jahr wurden die Europäischen Gemeinschaften gegründet, aus denen später die EU entstand?",
        answerA = "1945",
        answerB = "1951",
        answerC = "1957",
        answerD = "1962",
        correctAnswer = 2,
        explanation = "Die Europäischen Wirtschaftsgemeinschaft (EWG) und die Europäische Atomgemeinschaft (Euratom) wurden 1957 durch die Römischen Verträge gegründet.",
        difficulty = 1,
        funFact = "Die Vorläuferin der EU, die Europäische Gemeinschaft für Kohle und Stahl (EGKS), wurde bereits 1951 gegründet."
    ),

    Question(
        categoryId = 3,
        questionText = "Wann wurde die Europäische Union durch den Vertrag von Maastricht offiziell gegründet?",
        answerA = "1989",
        answerB = "1991",
        answerC = "1993",
        answerD = "1995",
        correctAnswer = 2,
        explanation = "Der Vertrag von Maastricht trat am 1. November 1993 in Kraft und begründete offiziell die Europäische Union.",
        difficulty = 1,
        funFact = "Maastricht ist eine Stadt in den Niederlanden, wo der Vertrag am 7. Februar 1992 unterzeichnet wurde."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche gemeinsame Währung wurde in vielen EU-Ländern eingeführt?",
        answerA = "Der Euro",
        answerB = "Der ECU",
        answerC = "Der Dollar",
        answerD = "Der Europound",
        correctAnswer = 0,
        explanation = "Der Euro wurde am 1. Januar 1999 als Buchgeld und am 1. Januar 2002 als Bargeld eingeführt. Heute ist er die offizielle Währung von 20 EU-Mitgliedsstaaten.",
        difficulty = 1,
        funFact = "Die Europäische Zentralbank mit Sitz in Frankfurt am Main verwaltet die Geldpolitik des Euro."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Land verließ als erstes die Europäische Union?",
        answerA = "Norwegen",
        answerB = "Schweiz",
        answerC = "Großbritannien",
        answerD = "Dänemark",
        correctAnswer = 2,
        explanation = "Großbritannien stimmte 2016 in einem Referendum für den Austritt aus der EU (Brexit) und verließ die Union offiziell am 31. Januar 2020.",
        difficulty = 1,
        funFact = "\"Brexit\" ist ein Kunstwort aus \"British\" und \"Exit\" und wurde schnell zum internationalen Begriff für den britischen EU-Austritt."
    ),

    Question(
        categoryId = 3,
        questionText = "Wie viele Sterne hat die Flagge der Europäischen Union?",
        answerA = "10",
        answerB = "12",
        answerC = "15",
        answerD = "27",
        correctAnswer = 1,
        explanation = "Die EU-Flagge zeigt 12 goldene Sterne auf blauem Grund. Die Anzahl 12 symbolisiert Vollkommenheit und Einheit und hat nichts mit der Anzahl der Mitgliedsstaaten zu tun.",
        difficulty = 1,
        funFact = "Die Europaflagge mit den 12 Sternen wurde bereits 1955 vom Europarat entworfen, bevor sie zur Flagge der EU wurde."
    ),

    Question(
        categoryId = 3,
        questionText = "Wo hat das Europäische Parlament seinen offiziellen Sitz?",
        answerA = "Brüssel",
        answerB = "Luxemburg",
        answerC = "Straßburg",
        answerD = "Amsterdam",
        correctAnswer = 2,
        explanation = "Das Europäische Parlament hat seinen offiziellen Sitz in Straßburg, Frankreich. Es tagt aber auch regelmäßig in Brüssel.",
        difficulty = 1,
        funFact = "Die Wahl des Tagungsortes Straßburg war ein bewusstes Symbol für die Aussöhnung zwischen Frankreich und Deutschland nach dem Zweiten Weltkrieg."
    ),

    // --- Additional Mixed Easy Questions ---

    Question(
        categoryId = 3,
        questionText = "Welche Organisation verlieh den ersten Friedensnobelpreis?",
        answerA = "Die Vereinten Nationen",
        answerB = "Das Schwedische Komitee",
        answerC = "Das Norwegische Nobelkomitee",
        answerD = "Die Schweizerische Akademie",
        correctAnswer = 2,
        explanation = "Der Friedensnobelpreis wird vom Norwegischen Nobelkomitee vergeben, das vom Norwegischen Storting ernannt wird. Der erste wurde 1901 verliehen.",
        difficulty = 1,
        funFact = "Alle anderen Nobelpreise (Physik, Chemie, Medizin, Literatur) werden von schwedischen Institutionen vergeben."
    ),

    Question(
        categoryId = 3,
        questionText = "In welchem Land liegt die historische Stadt Pompeji, die durch einen Vulkanausbruch zerstört wurde?",
        answerA = "Griechenland",
        answerB = "Spanien",
        answerC = "Italien",
        answerD = "Türkei",
        correctAnswer = 2,
        explanation = "Pompeji liegt in der Nähe von Neapel in Italien. Die Stadt wurde 79 n. Chr. durch den Ausbruch des Vesuvs unter einer Ascheschicht begraben.",
        difficulty = 1,
        funFact = "Durch die schnelle Verschüttung sind in Pompeji viele alltägliche Gegenstände und sogar Abdrücke von Körpern der Opfer erhalten geblieben."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher französische Kaiser führte den Code Napoleon (Bürgerliches Gesetzbuch) ein?",
        answerA = "Ludwig XIV.",
        answerB = "Napoleon Bonaparte",
        answerC = "Karl der Große",
        answerD = "Ludwig XVI.",
        correctAnswer = 1,
        explanation = "Napoleon Bonaparte führte 1804 den Code civil (Napoleonisches Gesetzbuch) ein, der das französische Recht vereinheitlichte und die Prinzipien der Revolution festschrieb.",
        difficulty = 1,
        funFact = "Der Code Napoleon beeinflusste die Rechtssysteme vieler Länder weltweit, darunter Frankreich, Belgien, Luxemburg, Québec und Louisiana."
    ),

)
