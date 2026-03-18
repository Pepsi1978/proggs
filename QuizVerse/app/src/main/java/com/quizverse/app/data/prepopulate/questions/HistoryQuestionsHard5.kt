package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun historyQuestionsHard5(): List<Question> = listOf(

    // --- WWII: Operation Barbarossa Details ---

    Question(
        categoryId = 3,
        questionText = "An welchem Datum begann die deutsche Operation Barbarossa gegen die Sowjetunion?",
        answerA = "1. September 1939",
        answerB = "10. Mai 1940",
        answerC = "22. Juni 1941",
        answerD = "7. Dezember 1941",
        correctAnswer = 2,
        explanation = "Operation Barbarossa begann am 22. Juni 1941, als Deutschland mit drei Heeresgruppen in die Sowjetunion einmarschierte. Es war der größte Landfeldzug der Geschichte.",
        difficulty = 3,
        funFact = "Der Name 'Barbarossa' bezieht sich auf Kaiser Friedrich I., der im Mittelalter einen Kreuzzug anführte – ein bewusst gewähltes historisches Symbol für die Ostexpansion."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche drei deutschen Heeresgruppen nahmen an Operation Barbarossa teil?",
        answerA = "Nord, Mitte und Süd",
        answerB = "Ost, West und Zentrum",
        answerC = "Alpha, Beta und Gamma",
        answerD = "Weichsel, Elbe und Oder",
        correctAnswer = 0,
        explanation = "Die drei Heeresgruppen Nord (Ziel Leningrad), Mitte (Ziel Moskau) und Süd (Ziel Ukraine/Kaukasus) bildeten die drei Hauptstoßrichtungen des Angriffs.",
        difficulty = 3,
        funFact = "Heeresgruppe Mitte erzielte die größten Anfangserfolge und schloss in den Kesselschlachten von Bialystok und Minsk über 300.000 sowjetische Soldaten ein."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher sowjetische Stadtname wurde während der Belagerung 1941–1944 zum Symbol des Widerstands?",
        answerA = "Kiew",
        answerB = "Minsk",
        answerC = "Leningrad",
        answerD = "Odessa",
        correctAnswer = 2,
        explanation = "Die Belagerung Leningrads dauerte 872 Tage (September 1941 – Januar 1944) und kostete schätzungsweise 800.000 bis 1,1 Millionen Zivilisten das Leben.",
        difficulty = 3,
        funFact = "Während der Belagerung wurde die 'Straße des Lebens' über den gefrorenen Ladogasee zum einzigen Versorgungsweg in die Stadt – im Winter transportierten Lastwagen Lebensmittel über das Eis."
    ),

    // --- WWII: Stalingrad Details ---

    Question(
        categoryId = 3,
        questionText = "Welcher deutsche General kapitulierte am 2. Februar 1943 in Stalingrad?",
        answerA = "Erwin Rommel",
        answerB = "Gerd von Rundstedt",
        answerC = "Friedrich Paulus",
        answerD = "Heinz Guderian",
        correctAnswer = 2,
        explanation = "Generalfeldmarschall Friedrich Paulus kapitulierte am 2. Februar 1943 mit den Resten der 6. Armee. Hitler hatte ihm kurz zuvor den Marschallsstab verliehen, wohl in der Erwartung, er würde sich lieber erschießen.",
        difficulty = 3,
        funFact = "Paulus war der erste deutsche Feldmarschall, der sich je gefangen gab. Er lebte nach dem Krieg in der DDR und wurde zu einem scharfen Kritiker des Naziregimes."
    ),

    Question(
        categoryId = 3,
        questionText = "Wie hieß die sowjetische Gegenoffensive, die die deutsche 6. Armee in Stalingrad einkesselte?",
        answerA = "Operation Uranus",
        answerB = "Operation Saturn",
        answerC = "Operation Mars",
        answerD = "Operation Zitadelle",
        correctAnswer = 0,
        explanation = "Operation Uranus (19.–23. November 1942) war die sowjetische Gegenoffensive, bei der zwei sowjetische Heeresgruppen die schwächeren rumänischen und deutschen Flanken durchbrachen und die 6. Armee einkesselten.",
        difficulty = 3,
        funFact = "Die Planung für Uranus erfolgte im Geheimen, während die Rote Armee öffentlich nur defensiv zu kämpfen schien. General Georgi Schukow und Alexander Wassilewski koordinierten die Operation."
    ),

    // --- WWII: Operation Market Garden ---

    Question(
        categoryId = 3,
        questionText = "Bei welcher Stadt scheiterte die alliierte Luftlandeoperation Market Garden im September 1944?",
        answerA = "Eindhoven",
        answerB = "Nijmegen",
        answerC = "Arnhem",
        answerD = "Rotterdam",
        correctAnswer = 2,
        explanation = "Bei Arnhem (Arnheim) sollte die 1. britische Luftlandedivision die Rheinbrücke halten. Durch unerwarteten deutschen Widerstand – u.a. der 9. SS-Panzerdivision – scheiterte die Operation.",
        difficulty = 3,
        funFact = "Der britische General Boy Browning warnte vor der Operation, sie könnte 'eine Brücke zu weit' sein. Dieser Satz wurde zum Titel des berühmten Buches und Films über die Operation."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches war das Hauptziel der Operation Market Garden im September 1944?",
        answerA = "Befreiung von Paris",
        answerB = "Überquerung des Rheins und schnelles Ende des Krieges",
        answerC = "Einnahme von Hamburg",
        answerD = "Unterbrechung deutscher U-Boot-Versorgungswege",
        correctAnswer = 1,
        explanation = "Montgomery plante, durch eine kühne kombinierte Luft-Boden-Operation den Rhein zu überqueren, Deutschland vom Norden her zu umgehen und den Krieg noch 1944 zu beenden.",
        difficulty = 3,
        funFact = "Rund 35.000 alliierte Fallschirmjäger und Gleiterpiloten wurden eingesetzt – die größte Luftlandeoperation der Geschichte bis zu diesem Zeitpunkt."
    ),

    // --- WWII: Kursk ---

    Question(
        categoryId = 3,
        questionText = "Wie hieß die deutsche Offensive bei der Panzerschlacht von Kursk im Juli 1943?",
        answerA = "Operation Taifun",
        answerB = "Operation Zitadelle",
        answerC = "Operation Blau",
        answerD = "Operation Wintergewitter",
        correctAnswer = 1,
        explanation = "Operation Zitadelle (5.–16. Juli 1943) war der letzte große deutsche Angriff an der Ostfront. Die Sowjets hatten ihre Verteidigungsanlagen in der Kursker Ausbuchtung tief gestaffelt.",
        difficulty = 3,
        funFact = "Die Panzerschlacht von Prokhorovka am 12. Juli 1943 gilt als eine der größten Panzerschlachten der Geschichte, obwohl neuere Forschungen die Verlustzahlen neu bewerten."
    ),

    // --- WWII: Midway ---

    Question(
        categoryId = 3,
        questionText = "Welche japanischen Flugzeugträger wurden in der Schlacht von Midway im Juni 1942 versenkt?",
        answerA = "Yamato, Musashi, Nagato",
        answerB = "Akagi, Kaga, Sōryū, Hiryū",
        answerC = "Shōkaku, Zuikaku, Junyō",
        answerD = "Shinano, Taihō, Unryū",
        correctAnswer = 1,
        explanation = "Die vier Flugzeugträger Akagi, Kaga, Sōryū und Hiryū wurden versenkt. Alle vier hatten am Angriff auf Pearl Harbor teilgenommen. Die USA verloren nur den Yorktown.",
        difficulty = 3,
        funFact = "Der Sieg bei Midway war zum Teil auf gebrochene japanische Codes zurückzuführen. Die US-Codeknacker hatten das Angriffsziel 'AF' als Midway identifiziert, indem sie eine gefälschte Radiomeldung über Wasserknappheit aussandten."
    ),

    // --- Holocaust Details ---

    Question(
        categoryId = 3,
        questionText = "An welchem Datum fand die Wannsee-Konferenz statt, auf der die 'Endlösung der Judenfrage' koordiniert wurde?",
        answerA = "20. Januar 1942",
        answerB = "9. November 1938",
        answerC = "1. September 1939",
        answerD = "30. Januar 1933",
        correctAnswer = 0,
        explanation = "Die Wannsee-Konferenz fand am 20. Januar 1942 in einer Villa am Großen Wannsee in Berlin statt. Unter Leitung von Reinhard Heydrich koordinierten hochrangige Nazis die systematische Vernichtung der europäischen Juden.",
        difficulty = 3,
        funFact = "Das einzige erhaltene Protokoll der Konferenz wurde erst 1947 entdeckt. Es ist ein wichtiges Dokument, das zeigt, wie bürokratisch und kalt der Massenmord geplant wurde."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Vernichtungslager war für die größte Zahl von Opfern verantwortlich?",
        answerA = "Treblinka",
        answerB = "Sobibor",
        answerC = "Auschwitz-Birkenau",
        answerD = "Belzec",
        correctAnswer = 2,
        explanation = "In Auschwitz-Birkenau wurden schätzungsweise 1,1 bis 1,5 Millionen Menschen ermordet, überwiegend Juden. Das Lager war das größte Vernichtungs- und Konzentrationslager des NS-Regimes.",
        difficulty = 3,
        funFact = "Die Befreiung von Auschwitz durch die Rote Armee am 27. Januar 1945 wird heute weltweit als Internationaler Holocaust-Gedenktag begangen."
    ),

    Question(
        categoryId = 3,
        questionText = "Wer war Adolf Eichmann, der 1961 in Jerusalem vor Gericht stand?",
        answerA = "SS-Obersturmbannführer und Hauptorganisator des Deportationsapparats",
        answerB = "Lagerkommandant von Auschwitz",
        answerC = "Erfinder der Gaskammern",
        answerD = "Reichsminister für die besetzten Ostgebiete",
        correctAnswer = 0,
        explanation = "Adolf Eichmann war SS-Obersturmbannführer und leitete das 'Judenreferat' im RSHA. Er koordinierte die Deportation von Millionen Juden in die Vernichtungslager und war damit einer der Hauptverantwortlichen für den Holocaust.",
        difficulty = 3,
        funFact = "Eichmann floh nach dem Krieg nach Argentinien. Der israelische Geheimdienst Mossad entführte ihn 1960 in Buenos Aires. Nach dem Prozess wurde er als einziger Mensch in Israel je hingerichtet."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches war das erste Konzentrationslager, das die Nationalsozialisten 1933 errichteten?",
        answerA = "Buchenwald",
        answerB = "Sachsenhausen",
        answerC = "Dachau",
        answerD = "Bergen-Belsen",
        correctAnswer = 2,
        explanation = "Dachau wurde am 22. März 1933 eröffnet – nur wenige Wochen nach Hitlers Machtübernahme. Es diente als Modell für alle späteren Konzentrationslager und wurde anfänglich für politische Gegner eingerichtet.",
        difficulty = 3,
        funFact = "Dachau war fast zwölf Jahre in Betrieb. Es wurde nie offiziell als Vernichtungslager eingestuft, dennoch starben dort über 41.500 Menschen durch Hunger, Krankheiten, Zwangsarbeit und Erschießungen."
    ),

    // --- Cold War: Berlin Crises ---

    Question(
        categoryId = 3,
        questionText = "Wie lange dauerte die sowjetische Berlin-Blockade von 1948/49?",
        answerA = "Drei Monate",
        answerB = "Knapp ein Jahr (318 Tage)",
        answerC = "Zwei Jahre",
        answerD = "Genau sechs Monate",
        correctAnswer = 1,
        explanation = "Die Berlin-Blockade dauerte vom 24. Juni 1948 bis zum 12. Mai 1949 – knapp ein Jahr. Die Westalliierten versorgten Westberlin in dieser Zeit ausschließlich per Luftbrücke.",
        difficulty = 3,
        funFact = "Während der Luftbrücke landeten zeitweise alle 90 Sekunden ein Flugzeug in Berlin. In der Spitze wurden täglich über 12.000 Tonnen Güter eingeflogen – darunter auch Weihnachtsbäume."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Ereignis löste die Berlinkrise von 1961 und den Bau der Berliner Mauer aus?",
        answerA = "Der Ungarnaufstand 1956",
        answerB = "Die massenhaft Flucht von DDR-Bürgern in den Westen durch Berlin",
        answerC = "Die Kuba-Krise",
        answerD = "Die Verkündung der Hallstein-Doktrin",
        correctAnswer = 1,
        explanation = "Bis August 1961 flohen täglich Tausende DDR-Bürger über den noch offenen Berlin-Sektor in den Westen. Bis zum Mauerbau hatten rund 3,5 Millionen Menschen die DDR verlassen – ein enormer Arbeitskräftemangel bedrohte das Regime.",
        difficulty = 3,
        funFact = "In der Nacht vom 12. auf den 13. August 1961 begannen Ost-Berliner Soldaten mit Stacheldraht die Grenze abzuriegeln. Viele Familien wurden dadurch über Nacht voneinander getrennt."
    ),

    // --- Cold War: Hungarian Uprising 1956 ---

    Question(
        categoryId = 3,
        questionText = "Wer war der ungarische Reformkommunist, der 1956 zum Ministerpräsidenten wurde und später hingerichtet wurde?",
        answerA = "János Kádár",
        answerB = "Imre Nagy",
        answerC = "Mátyás Rákosi",
        answerD = "Władysław Gomułka",
        correctAnswer = 1,
        explanation = "Imre Nagy wurde am 24. Oktober 1956 Ministerpräsident und versuchte, Ungarn aus dem Warschauer Pakt herauszuführen. Nach der sowjetischen Intervention wurde er verhaftet und 1958 hingerichtet.",
        difficulty = 3,
        funFact = "Nagy wurde zunächst in einem Geheimprozess verurteilt und hingerichtet. Erst 1989, kurz vor dem Fall des Kommunismus, wurde er offiziell rehabilitiert und mit staatlichen Ehren begraben."
    ),

    Question(
        categoryId = 3,
        questionText = "Wie reagierte die sowjetische Führung auf den Ungarnaufstand im November 1956?",
        answerA = "Mit diplomatischen Verhandlungen",
        answerB = "Mit wirtschaftlichen Sanktionen",
        answerC = "Mit militärischer Intervention und Panzern",
        answerD = "Mit dem Ausschluss Ungarns aus dem Warschauer Pakt",
        correctAnswer = 2,
        explanation = "Am 4. November 1956 marschierten sowjetische Truppen mit über 1.000 Panzern in Budapest ein. Die Revolution wurde brutal niedergeschlagen; etwa 2.500 Ungarn und 700 Sowjetsoldaten kamen ums Leben.",
        difficulty = 3,
        funFact = "Während die Welt auf die gleichzeitige Suez-Krise blickte, konnte die Sowjetunion in Ungarn eingreifen ohne größere internationale Konsequenzen zu fürchten. Rund 200.000 Ungarn flohen danach ins Ausland."
    ),

    // --- Cold War: Prague Spring ---

    Question(
        categoryId = 3,
        questionText = "Wie hieß das Reformprogramm des tschechoslowakischen Kommunistenführers Dubček während des Prager Frühlings 1968?",
        answerA = "Sozialismus mit menschlichem Antlitz",
        answerB = "Dritter Weg zwischen Ost und West",
        answerC = "Demokratischer Sozialismus",
        answerD = "Nationale Eigenständigkeit",
        correctAnswer = 0,
        explanation = "Alexander Dubček prägte den Begriff 'Sozialismus mit menschlichem Antlitz' für sein Reformprogramm, das mehr Pressefreiheit, Pluralismus und Dezentralisierung vorsah.",
        difficulty = 3,
        funFact = "Der Prager Frühling wurde durch eine nächtliche Invasion von Truppen des Warschauer Pakts am 21. August 1968 beendet. Als Begründung formulierte Breschnew die 'Breschnew-Doktrin': Sozialistische Errungenschaften seien unumkehrbar."
    ),

    // --- Cold War: Détente and SDI ---

    Question(
        categoryId = 3,
        questionText = "Wie hieß das von Reagan 1983 angekündigte amerikanische Raketenabwehrprogramm, das die Sowjetunion stark unter Druck setzte?",
        answerA = "SALT III",
        answerB = "Strategic Defense Initiative (SDI)",
        answerC = "Neutron-Shield-Program",
        answerD = "Nuclear Deterrence Act",
        correctAnswer = 1,
        explanation = "Die Strategic Defense Initiative (SDI), von Kritikern 'Star Wars' genannt, plante ein weltraumgestütztes Raketenabwehrsystem. Die Sowjetunion fürchtete, ihr nukleares Gleichgewicht zu verlieren.",
        difficulty = 3,
        funFact = "Viele Historiker glauben, dass SDI zum Zusammenbruch der Sowjetunion beitrug – nicht weil es funktionierte, sondern weil die UdSSR enorme Ressourcen ausgeben musste, um darauf zu reagieren."
    ),

    Question(
        categoryId = 3,
        questionText = "Was bedeutete 'Détente' in der Außenpolitik der 1970er-Jahre?",
        answerA = "Militärische Überlegenheit des Westens",
        answerB = "Entspannungspolitik zwischen Ost und West",
        answerC = "Atomare Abschreckung durch gegenseitig gesicherte Vernichtung",
        answerD = "Wirtschaftliche Isolation der Sowjetunion",
        correctAnswer = 1,
        explanation = "Détente (französisch für 'Entspannung') bezeichnet die Phase der diplomatischen Annäherung zwischen den USA und der UdSSR in den 1970er-Jahren, geprägt durch Nixon, Kissinger, Breschnew und Willy Brandt (Ostpolitik).",
        difficulty = 3,
        funFact = "Die Détente-Phase führte zu den SALT-I- und SALT-II-Abkommen zur Rüstungsbegrenzung sowie zur Konferenz für Sicherheit und Zusammenarbeit in Europa (KSZE) und der Unterzeichnung der Schlussakte von Helsinki 1975."
    ),

    // --- Korean War Details ---

    Question(
        categoryId = 3,
        questionText = "An welchem Datum begann der Koreakrieg mit dem Einmarsch Nordkoreas in den Süden?",
        answerA = "25. Juni 1950",
        answerB = "7. August 1950",
        answerC = "1. Oktober 1949",
        answerD = "15. September 1950",
        correctAnswer = 0,
        explanation = "Am 25. Juni 1950 überschritt die nordkoreanische Volksarmee den 38. Breitengrad und marschierte in Südkorea ein. Innerhalb weniger Tage war Seoul gefallen.",
        difficulty = 3,
        funFact = "Der UN-Sicherheitsrat konnte eine Resolution zur Verteidigung Südkoreas nur beschließen, weil die Sowjetunion die Sitzung aus Protest gegen den Ausschluss der Volksrepublik China boykottierte."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche militärische Landeoperation im September 1950 drehte den Kriegsverlauf in Korea zugunsten der UN-Truppen?",
        answerA = "Landung bei Wonsan",
        answerB = "Landung bei Inchon",
        answerC = "Überquerung des Yalu-Flusses",
        answerD = "Offensive bei Pusan",
        correctAnswer = 1,
        explanation = "General MacArthurs amphibische Landung bei Inchon am 15. September 1950 gilt als militärisches Meisterstück. Sie umging die nordkoreanischen Linien und ermöglichte die Rückeroberung Seouls.",
        difficulty = 3,
        funFact = "Viele Militärberater rieten von der Landung bei Inchon ab wegen extremer Gezeitenunterschiede und gefährlicher Einfahrten. MacArthur setzte sich durch und die Operation war ein voller Erfolg."
    ),

    Question(
        categoryId = 3,
        questionText = "Wo und in welchem Jahr wurde das Waffenstillstandsabkommen des Koreakrieges unterzeichnet?",
        answerA = "Seoul, 1952",
        answerB = "Panmunjom, 1953",
        answerC = "Pjöngjang, 1954",
        answerD = "Tokyo, 1953",
        correctAnswer = 1,
        explanation = "Der Waffenstillstand wurde am 27. Juli 1953 in Panmunjom unterzeichnet. Es handelt sich bis heute nur um einen Waffenstillstand – kein Friedensvertrag wurde je geschlossen.",
        difficulty = 3,
        funFact = "Die demilitarisierte Zone (DMZ) zwischen Nord- und Südkorea ist mit 4 km Breite und 250 km Länge eine der meistbewachten Grenzen der Welt und zugleich paradoxerweise ein ungestörtes Wildtierschutzgebiet."
    ),

    // --- Vietnam War Details ---

    Question(
        categoryId = 3,
        questionText = "Welches Ereignis 1964 diente den USA als Vorwand für den massiven Eintritt in den Vietnamkrieg?",
        answerA = "Tet-Offensive",
        answerB = "Tonkin-Zwischenfall",
        answerC = "Fall Saigons",
        answerD = "My-Lai-Massaker",
        correctAnswer = 1,
        explanation = "Der angebliche Angriff nordvietnamesischer Torpedoboote auf US-Schiffe im Golf von Tonkin (August 1964) lieferte den Kongress dazu, dem Präsidenten weitreichende Kriegsvollmachten zu erteilen.",
        difficulty = 3,
        funFact = "Spätere Recherchen zeigten, dass zumindest der zweite angebliche Angriff wahrscheinlich nicht stattfand. Ex-Verteidigungsminister McNamara räumte dies in seinen Memoiren ein."
    ),

    Question(
        categoryId = 3,
        questionText = "Was war die Tet-Offensive im Januar 1968?",
        answerA = "Eine US-Offensivoperation gegen den Ho-Chi-Minh-Pfad",
        answerB = "Ein koordinierter Großangriff des Vietkong auf über 100 südvietnamesische Städte",
        answerC = "Die erste Landung amerikanischer Truppen in Vietnam",
        answerD = "Die letzte große nordvietnamesische Offensive vor dem Waffenstillstand",
        correctAnswer = 1,
        explanation = "Die Tet-Offensive war ein koordinierter Überraschungsangriff des Vietkong am vietnamesischen Neujahr (Tet) auf über 100 Städte. Militärisch wurde sie zurückgeschlagen, aber sie erschütterte die amerikanische Öffentlichkeit.",
        difficulty = 3,
        funFact = "Journalist Walter Cronkite kommentierte nach der Tet-Offensive live im Fernsehen: 'Wir stecken in einer Sackgasse.' Diese Aussage des meistgeachteten Journalisten Amerikas gilt als Wendepunkt der öffentlichen Meinung zum Krieg."
    ),

    Question(
        categoryId = 3,
        questionText = "An welchem Datum fiel Saigon und endete der Vietnamkrieg praktisch?",
        answerA = "27. Januar 1973",
        answerB = "30. April 1975",
        answerC = "1. Mai 1976",
        answerD = "1. Januar 1974",
        correctAnswer = 1,
        explanation = "Am 30. April 1975 marschierten nordvietnamesische Panzer in Saigon ein. Die letzten Amerikaner und südvietnamesischen Flüchtlinge wurden in einer chaotischen Hubschrauber-Evakuierung ausgeflogen.",
        difficulty = 3,
        funFact = "Das berühmte Bild des überfüllten Hubschraubers auf einem Dach in Saigon stammt nicht vom US-Botschaftsgebäude, sondern vom Wohnhaus des CIA-Direktors. Der Hubschrauber machte über zehn Fahrten zum Schiff."
    ),

    // --- Decolonization Specifics ---

    Question(
        categoryId = 3,
        questionText = "Welches afrikanische Land erhielt 1960 als erstes die Unabhängigkeit von Belgien, was zu einem blutigen Bürgerkrieg führte?",
        answerA = "Ruanda",
        answerB = "Kongo (heute DR Kongo)",
        answerC = "Burundi",
        answerD = "Angola",
        correctAnswer = 1,
        explanation = "Der belgische Kongo wurde am 30. Juni 1960 unabhängig als Republik Kongo. Wenige Tage später begann die Katanga-Sezession, gefolgt vom Ermord des Premierministers Patrice Lumumba im Januar 1961.",
        difficulty = 3,
        funFact = "Das Jahr 1960 gilt als 'Jahr Afrikas': 17 afrikanische Länder erlangten die Unabhängigkeit. Die Vereinten Nationen wuchsen damit von 82 auf 99 Mitgliedsstaaten."
    ),

    Question(
        categoryId = 3,
        questionText = "Wie endete der algerische Unabhängigkeitskrieg gegen Frankreich (1954–1962)?",
        answerA = "Mit der Niederlage der algerischen FLN",
        answerB = "Mit dem Évian-Abkommen und der Unabhängigkeit Algeriens",
        answerC = "Mit der Eingliederung Algeriens als Département in Frankreich",
        answerD = "Mit einem UN-Mandat zur Teilung des Landes",
        correctAnswer = 1,
        explanation = "Die Évian-Abkommen vom 18. März 1962 beendeten den acht Jahre langen Krieg. Am 1. Juli 1962 stimmten 99,7 % der Algerier für die Unabhängigkeit. Über eine Million Siedler (Pieds-Noirs) flohen nach Frankreich.",
        difficulty = 3,
        funFact = "Der Algerienkrieg kostete schätzungsweise 300.000 bis 1,5 Millionen Algerier das Leben. Die genauen Zahlen sind bis heute politisch umstritten – ein Zeichen, wie tief der Krieg die französisch-algerischen Beziehungen belastet."
    ),

    Question(
        categoryId = 3,
        questionText = "Wer führte Indien in die Unabhängigkeit von Großbritannien im Jahr 1947?",
        answerA = "Muhammad Ali Jinnah",
        answerB = "Jawaharlal Nehru",
        answerC = "Mahatma Gandhi",
        answerD = "Subhas Chandra Bose",
        correctAnswer = 1,
        explanation = "Jawaharlal Nehru wurde am 15. August 1947 Indiens erster Premierminister. Mahatma Gandhi war die moralische Führerfigur der Unabhängigkeitsbewegung, übernahm jedoch kein Regierungsamt.",
        difficulty = 3,
        funFact = "Die Teilung in Indien und Pakistan 1947 führte zu einer der größten Flüchtlingsbewegungen der Geschichte: 10–20 Millionen Menschen wurden vertrieben und bis zu 2 Millionen kamen bei Gewalt und Unruhen ums Leben."
    ),

    // --- Fall of Soviet Union: Gorbachev Reforms ---

    Question(
        categoryId = 3,
        questionText = "Welche zwei Kernreformprogramme führte Michail Gorbatschow ab 1985 in der Sowjetunion ein?",
        answerA = "Perestroika und Glasnost",
        answerB = "Detente und Entspannungspolitik",
        answerC = "Demokratizatsiya und Novoe Myshlenie",
        answerD = "Khozraschet und Samoupravlenie",
        correctAnswer = 0,
        explanation = "Perestroika (Umstrukturierung) zielte auf wirtschaftliche Reformen, Glasnost (Offenheit) auf mehr Transparenz und Pressefreiheit. Beide Reformen destabilisierten das sowjetische System ungewollt.",
        difficulty = 3,
        funFact = "Gorbatschow plante, den Sowjetkommunismus zu reformieren, nicht zu beenden. Historiker diskutieren bis heute, ob er die Konsequenzen seiner Reformen voraussah oder von der Dynamik überrollt wurde."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche sowjetische Katastrophe von 1986 beschleunigte Gorbatschows Glasnost-Politik erheblich?",
        answerA = "Giftgas-Unfall in Swerdlowsk",
        answerB = "Reaktorunfall in Tschernobyl",
        answerC = "Absturz der Raumsonde Phobos 1",
        answerD = "Erdbebenkatastrophe in Armenien",
        correctAnswer = 1,
        explanation = "Die Reaktorkatastrophe von Tschernobyl (26. April 1986) zwang Gorbatschow zur Transparenz. Das erste Zögern und Verbergen der Wahrheit demonstrierte genau das Problem, das Glasnost beheben sollte.",
        difficulty = 3,
        funFact = "Die sowjetische Regierung wartete fast zwei Tage, bevor sie die Katastrophe öffentlich anerkannte – und auch dann nur in minimalen Aussagen. Schweden entdeckte die erhöhte Radioaktivität zuerst durch Strahlenalarme in einem Kernkraftwerk."
    ),

    // --- Fall of Soviet Union: Baltic Independence ---

    Question(
        categoryId = 3,
        questionText = "Welches baltische Land erklärte als erstes seine Unabhängigkeit von der Sowjetunion (1990)?",
        answerA = "Estland",
        answerB = "Lettland",
        answerC = "Litauen",
        answerD = "Weißrussland",
        correctAnswer = 2,
        explanation = "Litauen erklärte am 11. März 1990 als erste Sowjetrepublik seine Unabhängigkeit. Die Sowjetunion verhängte daraufhin eine Wirtschaftsblockade, erkannte die Unabhängigkeit aber erst September 1991 an.",
        difficulty = 3,
        funFact = "Am 23. August 1989 – genau 50 Jahre nach dem Hitler-Stalin-Pakt – bildeten etwa 2 Millionen Menschen eine Menschenkette von Tallinn über Riga bis Vilnius: die 'Baltische Kette', ein Symbol der Freiheitsbewegung."
    ),

    // --- Fall of Soviet Union: August Coup 1991 ---

    Question(
        categoryId = 3,
        questionText = "Was passierte beim Augustputsch 1991 gegen Gorbatschow?",
        answerA = "Gorbatschow wurde erfolgreich gestürzt",
        answerB = "Ein Hardliner-Putsch scheiterte innerhalb von drei Tagen",
        answerC = "Boris Jelzin wurde verhaftet",
        answerD = "Die Rote Armee marschiertee in Moskau ein",
        correctAnswer = 1,
        explanation = "Der Putschversuch vom 19.–21. August 1991 durch kommunistische Hardliner scheiterte. Boris Jelzin stellte sich auf einem Panzer den Putschisten entgegen und symbolisierte den Widerstand. Der Putsch beschleunigte den Zerfall der UdSSR.",
        difficulty = 3,
        funFact = "Gorbatschow wurde während des Putsches in seiner Datscha auf der Krim festgesetzt. Als er nach Moskau zurückkehrte, war seine politische Macht faktisch gebrochen – Jelzin dominierte nun die Szene."
    ),

    Question(
        categoryId = 3,
        questionText = "An welchem Datum hörte die Sowjetunion offiziell auf zu existieren?",
        answerA = "9. November 1989",
        answerB = "3. Oktober 1990",
        answerC = "25. Dezember 1991",
        answerD = "1. Januar 1992",
        correctAnswer = 2,
        explanation = "Am 25. Dezember 1991 trat Michail Gorbatschow als Präsident der Sowjetunion zurück und erklärte die UdSSR für aufgelöst. Am gleichen Tag wurde die Sowjetflagge über dem Kreml durch die russische Trikolore ersetzt.",
        difficulty = 3,
        funFact = "US-Präsident George H.W. Bush erfuhr von Gorbatschows Rücktritt durch einen Anruf des sowjetischen Präsidenten selbst. Das Telefongespräch dauerte nur 10 Minuten."
    ),

    // --- German Reunification Politics ---

    Question(
        categoryId = 3,
        questionText = "Wie hieß der Vertragsrahmen, der die internationale Absegnung der deutschen Wiedervereinigung regelte?",
        answerA = "Zwei-plus-Vier-Vertrag",
        answerB = "Genfer Konvention zur Wiedervereinigung",
        answerC = "Maastricht-Vorvertrag",
        answerD = "Helsinki-Zusatzprotokoll",
        correctAnswer = 0,
        explanation = "Der Zwei-plus-Vier-Vertrag (12. September 1990) regelte die äußeren Aspekte der deutschen Einheit. Die vier Siegermächte (USA, UdSSR, UK, Frankreich) und die beiden deutschen Staaten einigten sich darin auf volle deutsche Souveränität.",
        difficulty = 3,
        funFact = "Ein entscheidender Punkt war der Verzicht des vereinten Deutschlands auf ABC-Waffen und die Begrenzung der Bundeswehr auf 370.000 Mann – ein Zugeständnis an sowjetische Sicherheitsbedenken."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher westdeutsche Bundeskanzler trieb die deutsche Wiedervereinigung maßgeblich voran?",
        answerA = "Willy Brandt",
        answerB = "Helmut Schmidt",
        answerC = "Helmut Kohl",
        answerD = "Hans-Dietrich Genscher",
        correctAnswer = 2,
        explanation = "Helmut Kohl (CDU) handelte nach dem Mauerfall rasch mit seinem Zehn-Punkte-Plan (November 1989) und nutzte das historische Fenster geschickt. Er wird deshalb auch als 'Kanzler der Einheit' bezeichnet.",
        difficulty = 3,
        funFact = "Kohl kündigte seinen Zehn-Punkte-Plan am 28. November 1989 im Bundestag an – ohne seine Koalitionspartner oder westliche Verbündete vorher zu informieren, was anfänglich für Verstimmung sorgte."
    ),

    Question(
        categoryId = 3,
        questionText = "Was regelte der Staatsvertrag zwischen BRD und DDR vom 18. Mai 1990?",
        answerA = "Die politische Vereinigung der beiden deutschen Staaten",
        answerB = "Die Wirtschafts-, Währungs- und Sozialunion",
        answerC = "Die militärische Integration der NVA in die Bundeswehr",
        answerD = "Den Abzug sowjetischer Truppen aus der DDR",
        correctAnswer = 1,
        explanation = "Der Staatsvertrag über die Wirtschafts-, Währungs- und Sozialunion trat am 1. Juli 1990 in Kraft. Die DDR übernahm damit die D-Mark, die westdeutsche Sozialgesetzgebung und die Marktwirtschaft.",
        difficulty = 3,
        funFact = "Die Umtauschrate von DDR-Mark in D-Mark war politisch heiß umstritten. Bundesbankpräsident Pöhl warnte vor den Folgekosten, unterlag aber. Gehälter wurden 1:1 umgetauscht, was die DDR-Wirtschaft schlagartig unkonkurrenzfähig machte."
    ),

    Question(
        categoryId = 3,
        questionText = "An welchem Datum trat die DDR formal der Bundesrepublik Deutschland bei?",
        answerA = "9. November 1989",
        answerB = "1. Juli 1990",
        answerC = "3. Oktober 1990",
        answerD = "25. Dezember 1990",
        correctAnswer = 2,
        explanation = "Der 3. Oktober 1990 ist der Tag der deutschen Wiedervereinigung: Die DDR trat nach Artikel 23 des Grundgesetzes der Bundesrepublik bei. Er wird seitdem als Nationalfeiertag begangen.",
        difficulty = 3,
        funFact = "Die Wiedervereinigung erfolgte nicht durch eine neue Verfassung, sondern durch den Beitritt der DDR zum Geltungsbereich des Grundgesetzes – ein bewusst gewählter 'kleiner' Weg, um die Einheit schnell zu vollziehen."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche sowjetische Zusicherung ermöglichte Gorbatschows Zustimmung zur NATO-Mitgliedschaft des vereinten Deutschlands?",
        answerA = "Westdeutsche Wirtschaftshilfe in Milliardenhöhe für die Sowjetunion",
        answerB = "Garantierte Neutralität Deutschlands",
        answerC = "Aufnahme der Sowjetunion in die NATO als Beobachter",
        answerD = "Dauerhafte Stationierung russischer Truppen in Ostdeutschland",
        correctAnswer = 0,
        explanation = "Deutschland sagte der Sowjetunion Wirtschaftshilfe und Kredite in Höhe von rund 15 Milliarden DM zu. Dies war ein wesentlicher Faktor für Gorbatschows Einwilligung, dass das vereinte Deutschland in der NATO verbleiben dürfe.",
        difficulty = 3,
        funFact = "Das Treffen Kohls mit Gorbatschow im Kaukasus im Juli 1990 gilt als Wendepunkt der Wiedervereinigung. In einer entspannten Atmosphäre stimmte Gorbatschow zu – eine Entscheidung, die seine eigene Partei später massiv kritisierte."
    ),

    // --- Additional WWII/Holocaust ---

    Question(
        categoryId = 3,
        questionText = "Was war die 'Aktion T4' des NS-Regimes?",
        answerA = "Das Programm zur Ermordung von Menschen mit Behinderungen",
        answerB = "Die Deportation polnischer Juden",
        answerC = "Das Programm zur Zwangssterilisation politischer Gegner",
        answerD = "Die Vergasung sowjetischer Kriegsgefangener",
        correctAnswer = 0,
        explanation = "Aktion T4 (benannt nach der Tiergartenstraße 4 in Berlin) war das NS-Programm zur systematischen Ermordung von Menschen mit körperlichen und geistigen Behinderungen. Bis 1941 wurden über 70.000 Menschen getötet.",
        difficulty = 3,
        funFact = "T4 wurde offiziell im August 1941 gestoppt, nachdem Bischof Clemens August Graf von Galen in einer öffentlichen Predigt dagegen protestierte. De facto ging das Morden jedoch verdeckt weiter."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Abkommen von 1938 ermöglichte Deutschlands Annexion des Sudetenlandes ohne Krieg?",
        answerA = "Münchner Abkommen",
        answerB = "Locarno-Vertrag",
        answerC = "Hitler-Stalin-Pakt",
        answerD = "Genfer Protokoll",
        correctAnswer = 0,
        explanation = "Das Münchner Abkommen (29./30. September 1938) zwischen Deutschland, Italien, Frankreich und Großbritannien erlaubte Deutschland, das Sudetenland zu annektieren. Die Tschechoslowakei wurde nicht einmal zu den Verhandlungen eingeladen.",
        difficulty = 3,
        funFact = "Premierminister Neville Chamberlain kehrte aus München und verkündete, er habe 'Frieden für unsere Zeit' erreicht. Weniger als ein Jahr später begann der Zweite Weltkrieg – und das Münchner Abkommen wurde zum Synonym für gescheiterte Appeasement-Politik."
    ),

    Question(
        categoryId = 3,
        questionText = "Wie viele Länder des Warschauer Pakts beteiligten sich an der Invasion der Tschechoslowakei 1968?",
        answerA = "Nur die Sowjetunion",
        answerB = "Drei Länder",
        answerC = "Fünf Länder",
        answerD = "Alle sieben Mitgliedstaaten",
        correctAnswer = 2,
        explanation = "Fünf Warschauer-Pakt-Staaten beteiligten sich: UdSSR, Polen, Ungarn, Bulgarien und Ostdeutschland (NVA-Einheiten). Rumänien und Albanien verweigerten die Teilnahme.",
        difficulty = 3,
        funFact = "Rumäniens Diktator Ceaușescu verurteilte die Invasion öffentlich – was ihm paradoxerweise westliches Wohlwollen einbrachte, obwohl er selbst sein Volk brutal unterdrückte."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher NS-Politiker leitete die Wannsee-Konferenz 1942?",
        answerA = "Heinrich Himmler",
        answerB = "Reinhard Heydrich",
        answerC = "Adolf Eichmann",
        answerD = "Hans Frank",
        correctAnswer = 1,
        explanation = "Reinhard Heydrich, Chef des Reichssicherheitshauptamtes und 'stellvertretender Reichsprotektor von Böhmen und Mähren', leitete die Konferenz. Adolf Eichmann organisierte sie und protokollierte sie.",
        difficulty = 3,
        funFact = "Heydrich wurde wenige Monate nach der Wannsee-Konferenz, am 4. Juni 1942, an den Folgen eines Attentats in Prag gestorben. Das Attentat organisierten britische und tschechoslowakische Agenten."
    ),

    Question(
        categoryId = 3,
        questionText = "Was bezeichnete der Begriff 'Endlösung der Judenfrage' im NS-Sprachgebrauch?",
        answerA = "Zwangsemigration aller Juden aus Europa",
        answerB = "Die systematische Ermordung aller europäischen Juden",
        answerC = "Die Internierung in Arbeitslagern bis Kriegsende",
        answerD = "Die Deportation nach Madagaskar",
        correctAnswer = 1,
        explanation = "Die 'Endlösung' war der euphemistische NS-Begriff für den systematischen Massenmord an allen europäischen Juden. Nach der Wannsee-Konferenz 1942 wurde dieser Prozess bürokratisch koordiniert.",
        difficulty = 3,
        funFact = "Dem Holocaust fielen etwa 6 Millionen Juden zum Opfer – zwei Drittel aller europäischen Juden. Zusätzlich wurden Sinti und Roma, Homosexuelle, politische Gegner und Behinderte systematisch verfolgt und ermordet."
    ),

    Question(
        categoryId = 3,
        questionText = "Wie hieß die erste große Vernichtungsaktion der SS-Einsatzgruppen im Zweiten Weltkrieg (September 1941)?",
        answerA = "Aktion Reinhardt",
        answerB = "Massaker in Babi Jar",
        answerC = "Kristallnacht",
        answerD = "Operation Harvest Festival",
        correctAnswer = 1,
        explanation = "In der Schlucht Babi Jar bei Kiew ermordeten SS-Einsatzgruppen am 29./30. September 1941 über 33.771 Juden in zwei Tagen – eines der größten Massaker der Geschichte.",
        difficulty = 3,
        funFact = "Babi Jar war Teil der systematischen Massenerschießungen der Einsatzgruppen, die insgesamt über 1,5 Millionen Juden in der besetzten Sowjetunion ermordeten. Bis Kriegsende wurden an dem Ort insgesamt über 100.000 Menschen getötet."
    ),

    // --- Cold War additional ---

    Question(
        categoryId = 3,
        questionText = "Was war die 'Hallstein-Doktrin' der Bundesrepublik Deutschland (1955)?",
        answerA = "Ablehnung diplomatischer Beziehungen zu Ländern, die die DDR anerkannten",
        answerB = "Forderung nach atomarer Bewaffnung der Bundeswehr",
        answerC = "Westdeutsche Neutralitätspolitik im Kalten Krieg",
        answerD = "Ablehnung eines Friedensvertrages mit der Sowjetunion",
        correctAnswer = 0,
        explanation = "Die Hallstein-Doktrin besagte, dass die BRD keine diplomatischen Beziehungen zu Staaten unterhält, die die DDR diplomatisch anerkennen. Ausnahme war die Sowjetunion. Die Doktrin wurde 1972 mit der Ostpolitik aufgegeben.",
        difficulty = 3,
        funFact = "Die Doktrin wurde nach Walter Hallstein, dem ersten Präsidenten der EWG-Kommission und damaligen Staatssekretär im Auswärtigen Amt, benannt. Als Jugoslawien die DDR 1957 anerkannte, brach die BRD sofort die Beziehungen ab."
    ),

    Question(
        categoryId = 3,
        questionText = "Welcher sowjetische Raketentyp auf Kuba war der Auslöser der Kuba-Krise 1962?",
        answerA = "Interkontinentalraketen R-7",
        answerB = "Mittelstreckenraketen R-12 und R-14",
        answerC = "Kurzstreckenraketen FROG-7",
        answerD = "Seegestützte SLBMs",
        correctAnswer = 1,
        explanation = "Die sowjetischen Mittelstreckenraketen R-12 (NATO-Code: SS-4, Reichweite 2.000 km) und R-14 (3.500 km) konnten von Kuba aus einen Großteil der USA erreichen und lösten die Krise aus.",
        difficulty = 3,
        funFact = "Was wenige wissen: Auf Kuba stationierte sowjetische Truppen hatten auch taktische Atomwaffen, die der US-Aufklärung entgingen. Hätte Kennedy eine Invasion befohlen, wäre die Lage noch gefährlicher geworden."
    ),

    Question(
        categoryId = 3,
        questionText = "Was verstand man unter der 'Breschnew-Doktrin'?",
        answerA = "Das Recht der Sowjetunion, in sozialistische Staaten militärisch einzugreifen",
        answerB = "Das Streben nach nuklearer Parität mit den USA",
        answerC = "Die Aufrüstungspolitik der späten Sowjetunion",
        answerD = "Die wirtschaftliche Integration des Ostblocks",
        correctAnswer = 0,
        explanation = "Die Breschnew-Doktrin (1968) besagte, dass die Sowjetunion das Recht habe, in sozialistische Länder einzugreifen, wenn der Sozialismus bedroht sei. Sie rechtfertigte die Invasion der Tschechoslowakei 1968.",
        difficulty = 3,
        funFact = "Als Gorbatschow die Breschnew-Doktrin 1989 de facto aufgab, scherzte der sowjetische Außenminister Schewardnadse, man wende nun die 'Sinatra-Doktrin' an – jedes Land dürfe es 'auf seine eigene Weise tun'."
    ),

    Question(
        categoryId = 3,
        questionText = "Was bedeutete SALT im Kontext des Kalten Krieges?",
        answerA = "Strategic Arms Limitation Talks – Verhandlungen zur Rüstungsbegrenzung",
        answerB = "Soviet-American Long-range Treaty",
        answerC = "Sicherheitsabkommen für Längstrekkenbomber und Torpedos",
        answerD = "Shared Arms Limitation Treaty",
        correctAnswer = 0,
        explanation = "SALT (Strategic Arms Limitation Talks) waren US-sowjetische Verhandlungen zur Begrenzung strategischer Nuklearwaffen. SALT I (1972) und SALT II (1979) waren wichtige Meilensteine der Détente.",
        difficulty = 3,
        funFact = "SALT II wurde von Präsident Carter nach dem sowjetischen Einmarsch in Afghanistan 1979 nie dem Senat zur Ratifizierung vorgelegt. Beide Seiten hielten sich jedoch informell an die Grenzen des Abkommens."
    ),

    Question(
        categoryId = 3,
        questionText = "Welches Ereignis veranlasste den sowjetischen Einmarsch in Afghanistan 1979?",
        answerA = "Ein amerikanischer Geheimdienstangriff",
        answerB = "Die Ermordung des kommunistischen Führers Taraki und Instabilität des Regimes",
        answerC = "Ein Aufstand der Mudschaheddin",
        answerD = "Eine Einladung der afghanischen Regierung",
        correctAnswer = 1,
        explanation = "Innerparteiliche Machtkämpfe, die Ermordung von Parteichef Taraki und die drohende Destabilisierung des kommunistischen Regimes veranlassten die Sowjetunion zu intervenieren – sie wollte ein kommunistisches Afghanistan nicht verlieren.",
        difficulty = 3,
        funFact = "Der sowjetische Afghanistan-Krieg (1979–1989) kostete Zehntausende sowjetische Soldaten das Leben und wird oft als 'sowjetischer Vietnam' bezeichnet. Die CIA unterstützte die Mudschaheddin mit Stinger-Raketen, die sowjetische Hubschrauber neutralisierten."
    ),

    Question(
        categoryId = 3,
        questionText = "Welche Organisation koordinierte die wirtschaftliche Zusammenarbeit des Ostblocks während des Kalten Krieges?",
        answerA = "Warschauer Pakt",
        answerB = "COMECON (Rat für Gegenseitige Wirtschaftshilfe)",
        answerC = "Komintern",
        answerD = "Osteuropäische Freihandelszone",
        correctAnswer = 1,
        explanation = "COMECON (Council for Mutual Economic Assistance, gegründet 1949) koordinierte die Wirtschaftspolitik der sozialistischen Länder. Es war das sowjetische Gegenstück zum Marshall-Plan und zur EWG.",
        difficulty = 3,
        funFact = "COMECON löste sich im Jahr 1991 auf, kurz vor dem Zusammenbruch der Sowjetunion. Die Mitgliedsländer mussten danach schnell auf westliche Marktpreise für Energie umstellen – was zu erheblichen Wirtschaftsproblemen führte."
    ),

)
