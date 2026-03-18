package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun historyQuestionsMedium4(): List<Question> = listOf(

    // Question 1 - WWII: Battle of Stalingrad
    Question(
        categoryId = 3,
        questionText = "In welchem Jahr endete die Schlacht von Stalingrad mit der Kapitulation der deutschen 6. Armee?",
        answerA = "1942",
        answerB = "1943",
        answerC = "1944",
        answerD = "1945",
        correctAnswer = 1, // B
        explanation = "Die Schlacht von Stalingrad endete am 2. Februar 1943 mit der Kapitulation der deutschen 6. Armee unter Feldmarschall Paulus. Es war eine der verlustreichsten Schlachten des Zweiten Weltkriegs.",
        difficulty = 2,
        funFact = "Feldmarschall Paulus war der erste deutsche Feldmarschall, der je in Kriegsgefangenschaft geriet – er kapitulierte am 31. Januar 1943, einen Tag bevor seine restlichen Truppen aufgaben."
    ),

    // Question 2 - WWII: Yalta Conference
    Question(
        categoryId = 3,
        questionText = "Bei welcher Konferenz 1945 beschlossen Roosevelt, Churchill und Stalin die Nachkriegsordnung Europas?",
        answerA = "Potsdamer Konferenz",
        answerB = "Casablanca-Konferenz",
        answerC = "Jalta-Konferenz",
        answerD = "Teheran-Konferenz",
        correctAnswer = 2, // C
        explanation = "Die Jalta-Konferenz fand vom 4.–11. Februar 1945 auf der Halbinsel Krim statt. Die drei Mächte beschlossen die Aufteilung Deutschlands in Besatzungszonen und die Gründung der Vereinten Nationen.",
        difficulty = 2,
        funFact = "Roosevelt war bei der Jalta-Konferenz bereits schwer krank – er starb nur zwei Monate später, am 12. April 1945, und erlebte das Kriegsende nicht mehr."
    ),

    // Question 3 - WWII: Potsdam Conference
    Question(
        categoryId = 3,
        questionText = "Welcher britische Premierminister nahm an der Potsdamer Konferenz (1945) teil und ersetzte Churchill während der Konferenz?",
        answerA = "Anthony Eden",
        answerB = "Clement Attlee",
        answerC = "Harold Macmillan",
        answerD = "Ernest Bevin",
        correctAnswer = 1, // B
        explanation = "Clement Attlee ersetzte Churchill ab dem 28. Juli 1945, als die Labour-Partei die britischen Wahlen gewann. Die Potsdamer Konferenz dauerte vom 17. Juli bis 2. August 1945.",
        difficulty = 2,
        funFact = "Churchill begann die Konferenz, doch als die Wahlergebnisse bekannt wurden, reiste er nach London und kehrte nie zurück. Attlee übernahm mitten in der Konferenz."
    ),

    // Question 4 - WWII: Holocaust
    Question(
        categoryId = 3,
        questionText = "In welchem Land befand sich das Vernichtungslager Auschwitz-Birkenau?",
        answerA = "Deutschland",
        answerB = "Österreich",
        answerC = "Polen",
        answerD = "Tschechoslowakei",
        correctAnswer = 2, // C
        explanation = "Auschwitz-Birkenau lag im besetzten Polen bei der Stadt Oświęcim. Es war das größte nationalsozialistische Konzentrations- und Vernichtungslager, in dem etwa 1,1 Millionen Menschen ermordet wurden.",
        difficulty = 2,
        funFact = "Das Lager wurde am 27. Januar 1945 von der Roten Armee befreit. Dieser Tag wird heute als Internationaler Holocaust-Gedenktag begangen."
    ),

    // Question 5 - WWII: Wannsee Conference
    Question(
        categoryId = 3,
        questionText = "Was wurde auf der Wannseekonferenz am 20. Januar 1942 beschlossen?",
        answerA = "Die Kriegserklärung an die USA",
        answerB = "Die koordinierte Durchführung der 'Endlösung der Judenfrage'",
        answerC = "Der Angriff auf die Sowjetunion",
        answerD = "Die Besetzung Frankreichs",
        correctAnswer = 1, // B
        explanation = "Auf der Wannseekonferenz koordinierten hochrangige NS-Funktionäre unter Leitung von Reinhard Heydrich die systematische Ermordung der europäischen Juden – den Holocaust.",
        difficulty = 2,
        funFact = "Das Protokoll der Wannseekonferenz wurde von Adolf Eichmann verfasst. Das einzig erhaltene Original-Exemplar wurde 1947 von Staatsanwälten des Nürnberger Prozesses entdeckt."
    ),

    // Question 6 - WWII: D-Day
    Question(
        categoryId = 3,
        questionText = "Wie lautete der Codename für die alliierte Invasion in der Normandie am 6. Juni 1944?",
        answerA = "Operation Market Garden",
        answerB = "Operation Barbarossa",
        answerC = "Operation Overlord",
        answerD = "Operation Torch",
        correctAnswer = 2, // C
        explanation = "Operation Overlord war der Codename für die Invasion der Normandie. Der eigentliche Landungstag (D-Day) war der 6. Juni 1944 – der größte amphibische Angriff der Militärgeschichte.",
        difficulty = 2,
        funFact = "Über 156.000 alliierte Soldaten landeten am D-Day an den Stränden der Normandie. Die Strandabschnitte trugen die Codenamen Utah, Omaha, Gold, Juno und Sword."
    ),

    // Question 7 - WWII: Resistance
    Question(
        categoryId = 3,
        questionText = "Welche deutsche Widerstandsgruppe verteilte Flugblätter gegen das Nazi-Regime und wurde 1943 hingerichtet?",
        answerA = "Rote Kapelle",
        answerB = "Weiße Rose",
        answerC = "Edelweißpiraten",
        answerD = "Kreisauer Kreis",
        correctAnswer = 1, // B
        explanation = "Die Weiße Rose war eine studentische Widerstandsgruppe um die Geschwister Hans und Sophie Scholl an der Universität München. Sie wurden am 22. Februar 1943 hingerichtet.",
        difficulty = 2,
        funFact = "Sophie Scholl war erst 21 Jahre alt, als sie hingerichtet wurde. Der Richter Roland Freisler bezeichnete sie vor dem Volksgerichtshof als 'Abschaum der Nation' – ein Urteil, das die Geschichte umkehrte."
    ),

    // Question 8 - WWII: July Plot
    Question(
        categoryId = 3,
        questionText = "Wer war der Hauptorganisator des Attentats auf Hitler am 20. Juli 1944?",
        answerA = "Erwin Rommel",
        answerB = "Claus Schenk Graf von Stauffenberg",
        answerC = "Hans Oster",
        answerD = "Carl Friedrich Goerdeler",
        correctAnswer = 1, // B
        explanation = "Claus Schenk Graf von Stauffenberg platzierte die Bombe im Führerhauptquartier 'Wolfsschanze'. Hitler überlebte verletzt. Stauffenberg wurde noch in der Nacht erschossen.",
        difficulty = 2,
        funFact = "Der Anschlag scheiterte auch, weil ein Offizier den Aktenkoffer mit der Bombe versehentlich hinter einen massiven Tischfuß schob, der einen Teil der Druckwelle abfing."
    ),

    // Question 9 - Nuremberg Trials
    Question(
        categoryId = 3,
        questionText = "In welcher deutschen Stadt fanden die Kriegsverbrecherprozesse gegen führende NS-Größen nach dem Zweiten Weltkrieg statt?",
        answerA = "Frankfurt",
        answerB = "München",
        answerC = "Berlin",
        answerD = "Nürnberg",
        correctAnswer = 3, // D
        explanation = "Die Nürnberger Prozesse fanden von 1945 bis 1946 statt. 24 Hauptangeklagte wurden wegen Kriegsverbrechen und Verbrechen gegen die Menschlichkeit angeklagt. 12 erhielten die Todesstrafe.",
        difficulty = 2,
        funFact = "Nürnberg wurde gewählt, weil der dortige Justizpalast unbeschädigt war und die Stadt als 'Stadt der Reichsparteitage' symbolische Bedeutung für den Nationalsozialismus hatte."
    ),

    // Question 10 - Nuremberg Trials: Verdict
    Question(
        categoryId = 3,
        questionText = "Welcher NS-Funktionär wurde in Nürnberg zu lebenslanger Haft verurteilt und saß bis zu seinem Tod 1987 in Spandau?",
        answerA = "Albert Speer",
        answerB = "Hermann Göring",
        answerC = "Rudolf Hess",
        answerD = "Karl Dönitz",
        correctAnswer = 2, // C
        explanation = "Rudolf Hess war stellvertretender Führer der NSDAP. Er wurde in Nürnberg zu lebenslanger Haft verurteilt und saß 46 Jahre im Kriegsverbrechergefängnis Spandau, wo er 1987 starb.",
        difficulty = 2,
        funFact = "Hermann Göring entzog sich durch Selbstmord der Hinrichtung – er tötete sich in der Nacht vor der geplanten Vollstreckung mit einer Zyankalikapsel, die er jahrelang versteckt gehalten hatte."
    ),

    // Question 11 - Post-war Germany: Occupation Zones
    Question(
        categoryId = 3,
        questionText = "In wie viele Besatzungszonen wurde Deutschland nach 1945 aufgeteilt?",
        answerA = "Zwei",
        answerB = "Drei",
        answerC = "Vier",
        answerD = "Fünf",
        correctAnswer = 2, // C
        explanation = "Deutschland wurde in vier Besatzungszonen aufgeteilt: eine amerikanische, eine britische, eine französische und eine sowjetische Zone. Auch Berlin wurde in vier Sektoren geteilt.",
        difficulty = 2,
        funFact = "Frankreich erhielt seine Besatzungszone nicht direkt von Deutschland, sondern aus Teilen der bereits zugeteilten amerikanischen und britischen Zonen – auf Druck der USA und Großbritanniens."
    ),

    // Question 12 - Berlin Blockade
    Question(
        categoryId = 3,
        questionText = "In welchem Jahr begann die sowjetische Blockade West-Berlins, die zur Berliner Luftbrücke führte?",
        answerA = "1946",
        answerB = "1947",
        answerC = "1948",
        answerD = "1949",
        correctAnswer = 2, // C
        explanation = "Die sowjetische Blockade Berlins begann am 24. Juni 1948 und dauerte bis zum 12. Mai 1949. Die Westalliierten versorgten West-Berlin fast ein Jahr lang ausschließlich per Luftbrücke.",
        difficulty = 2,
        funFact = "In der Spitzenzeit landete an den Berliner Flughäfen alle 90 Sekunden ein Flugzeug. Insgesamt wurden über 2,3 Millionen Tonnen Güter eingeflogen – darunter sogar Kohle und Baustoffe."
    ),

    // Question 13 - Currency Reform 1948
    Question(
        categoryId = 3,
        questionText = "Welche neue Währung wurde am 20. Juni 1948 in den westlichen Besatzungszonen Deutschlands eingeführt?",
        answerA = "Euro",
        answerB = "Deutsche Mark",
        answerC = "Reichsmark",
        answerD = "Rentenmark",
        correctAnswer = 1, // B
        explanation = "Die Währungsreform vom 20. Juni 1948 ersetzte die wertlose Reichsmark durch die Deutsche Mark. Jeder Bürger erhielt zunächst 40 DM Kopfgeld. Diese Reform gilt als Startschuss des Wirtschaftswunders.",
        difficulty = 2,
        funFact = "Am Tag nach der Währungsreform füllten sich die Schaufenster plötzlich mit Waren – die Händler hatten ihre Güter gehortet, weil sie die wertlose Reichsmark nicht annehmen wollten."
    ),

    // Question 14 - Wirtschaftswunder
    Question(
        categoryId = 3,
        questionText = "Wer war als Wirtschaftsminister maßgeblich für das 'Wirtschaftswunder' der Bundesrepublik Deutschland verantwortlich?",
        answerA = "Konrad Adenauer",
        answerB = "Franz Josef Strauß",
        answerC = "Ludwig Erhard",
        answerD = "Fritz Schäffer",
        correctAnswer = 2, // C
        explanation = "Ludwig Erhard setzte als Wirtschaftsminister (1949–1963) die Soziale Marktwirtschaft durch. Er verband freie Marktwirtschaft mit sozialer Absicherung und gilt als 'Vater des Wirtschaftswunders'.",
        difficulty = 2,
        funFact = "Ludwig Erhard schaffte eigenmächtig – ohne offizielle Genehmigung der Alliierten – die Preisbindung ab, die Tag nach der Währungsreform. Die Alliierten waren wütend, aber das Experiment gelang."
    ),

    // Question 15 - Founding of West Germany
    Question(
        categoryId = 3,
        questionText = "In welchem Jahr wurde die Bundesrepublik Deutschland gegründet?",
        answerA = "1945",
        answerB = "1947",
        answerC = "1949",
        answerD = "1950",
        correctAnswer = 2, // C
        explanation = "Die Bundesrepublik Deutschland wurde am 23. Mai 1949 mit dem Inkrafttreten des Grundgesetzes gegründet. Die DDR folgte am 7. Oktober 1949 in der sowjetischen Zone.",
        difficulty = 2,
        funFact = "Das Grundgesetz sollte ursprünglich nur ein provisorisches Dokument sein – bis zur Wiedervereinigung. Es heißt daher auch 'Grundgesetz' und nicht 'Verfassung', obwohl es faktisch als solche fungiert."
    ),

    // Question 16 - Marshall Plan
    Question(
        categoryId = 3,
        questionText = "Nach wem war der Marshallplan benannt, der Europa nach dem Zweiten Weltkrieg wirtschaftlich wieder aufbaute?",
        answerA = "George C. Marshall",
        answerB = "Harry Truman",
        answerC = "Dwight Eisenhower",
        answerD = "Dean Acheson",
        correctAnswer = 0, // A
        explanation = "Der Marshallplan (offiziell: European Recovery Program) wurde nach US-Außenminister George C. Marshall benannt, der ihn 1947 ankündigte. Die USA stellten über 12 Milliarden Dollar für den Wiederaufbau Europas bereit.",
        difficulty = 2,
        funFact = "George C. Marshall erhielt 1953 den Friedensnobelpreis für den Marshallplan – als einziger aktiver oder ehemaliger General in der Geschichte des Preises."
    ),

    // Question 17 - NATO founding
    Question(
        categoryId = 3,
        questionText = "In welchem Jahr wurde die NATO (Nordatlantikpakt) gegründet?",
        answerA = "1947",
        answerB = "1948",
        answerC = "1949",
        answerD = "1950",
        correctAnswer = 2, // C
        explanation = "Die NATO wurde am 4. April 1949 in Washington D.C. gegründet. Die Gründungsmitglieder waren 12 Länder, darunter die USA, Großbritannien, Frankreich, Kanada und die Benelux-Staaten.",
        difficulty = 2,
        funFact = "Der erste NATO-Generalsekretär Hastings Ismay fasste den Zweck des Bündnisses in einem berühmten Satz zusammen: 'Keep the Americans in, the Russians out, and the Germans down.'"
    ),

    // Question 18 - Warsaw Pact
    Question(
        categoryId = 3,
        questionText = "Wann wurde der Warschauer Pakt als östliches Gegenstück zur NATO gegründet?",
        answerA = "1949",
        answerB = "1952",
        answerC = "1955",
        answerD = "1958",
        correctAnswer = 2, // C
        explanation = "Der Warschauer Pakt wurde am 14. Mai 1955 gegründet, nur eine Woche nachdem Westdeutschland der NATO beigetreten war. Er bestand bis 1991.",
        difficulty = 2,
        funFact = "Der Warschauer Pakt wurde einen Tag nach Unterzeichnung des Österreichischen Staatsvertrags gegründet, durch den Österreich seine Neutralität erklärte und sowjetische Truppen abziehen mussten."
    ),

    // Question 19 - Berlin Wall Construction
    Question(
        categoryId = 3,
        questionText = "Wann begann die DDR mit dem Bau der Berliner Mauer?",
        answerA = "3. Oktober 1961",
        answerB = "13. August 1961",
        answerC = "17. Juni 1953",
        answerD = "9. November 1961",
        correctAnswer = 1, // B
        explanation = "In der Nacht vom 12. auf den 13. August 1961 begann die DDR mit dem Bau der Berliner Mauer. Zunächst wurden Stacheldrahtsperren errichtet, danach folgte die Betonmauer.",
        difficulty = 2,
        funFact = "In den zwei Wochen vor dem Mauerbau verließen täglich über 2.000 Menschen die DDR über Westberlin. Allein am 12. August 1961 flohen noch 2.400 Menschen – am nächsten Morgen war es unmöglich geworden."
    ),

    // Question 20 - Berlin Wall: Reason
    Question(
        categoryId = 3,
        questionText = "Was war der Hauptgrund für den Bau der Berliner Mauer 1961?",
        answerA = "Schutz vor westlichem Militär",
        answerB = "Eindämmung der Massenflucht aus der DDR",
        answerC = "Reaktion auf den NATO-Beitritt Westdeutschlands",
        answerD = "Befehle der sowjetischen Regierung zur Grenzsicherung",
        correctAnswer = 1, // B
        explanation = "Bis 1961 hatten über 3 Millionen DDR-Bürger die Republik verlassen, viele davon Fachkräfte. Der Mauerbau sollte die 'Abstimmung mit den Füßen' stoppen und den wirtschaftlichen Kollaps verhindern.",
        difficulty = 2,
        funFact = "Die DDR bezeichnete die Mauer offiziell als 'Antifaschistischen Schutzwall' – ein propagandistischer Begriff, der suggerierte, die Mauer schütze die DDR-Bürger vor dem Westen."
    ),

    // Question 21 - Cuban Missile Crisis
    Question(
        categoryId = 3,
        questionText = "In welchem Jahr fand die Kubakrise statt, die die Welt an den Rand eines Atomkrieges brachte?",
        answerA = "1960",
        answerB = "1961",
        answerC = "1962",
        answerD = "1963",
        correctAnswer = 2, // C
        explanation = "Die Kubakrise dauerte vom 16. bis 28. Oktober 1962. Die USA entdeckten sowjetische Atomraketen auf Kuba. Dreizehn Tage lang stand die Welt vor der Möglichkeit eines nuklearen Krieges.",
        difficulty = 2,
        funFact = "Ein sowjetischer U-Boot-Kommandant, Vasili Arkhipov, verhinderte möglicherweise den Atomkrieg, indem er sich weigerte, einen Torpedo abzufeuern – obwohl zwei andere Offiziere auf seinem Boot dafür stimmten."
    ),

    // Question 22 - Cuban Missile Crisis: Resolution
    Question(
        categoryId = 3,
        questionText = "Welches geheime Abkommen beendete die Kubakrise?",
        answerA = "Die USA verpflichteten sich, nicht in Kuba einzumarschieren",
        answerB = "Die USA zogen ihre Raketen aus der Türkei ab",
        answerC = "Beide Vereinbarungen A und B zusammen",
        answerD = "Die Sowjetunion zahlte Entschädigung an die USA",
        correctAnswer = 2, // C
        explanation = "Die Krise endete mit einem Kompromiss: Die Sowjetunion zog ihre Raketen aus Kuba ab. Im Gegenzug versprachen die USA, Kuba nicht anzugreifen, und zogen heimlich ihre Jupiter-Raketen aus der Türkei ab.",
        difficulty = 2,
        funFact = "Das Abkommen über den Rückzug der US-Raketen aus der Türkei blieb jahrelang geheim. Kennedy wollte nicht den Eindruck erwecken, nachgegeben zu haben – weshalb er öffentlich nur das Kuba-Versprechen erwähnte."
    ),

    // Question 23 - Arms Race
    Question(
        categoryId = 3,
        questionText = "Welche Doktrin besagte, dass ein Atomkrieg wegen der gegenseitigen Zerstörungskapazität verhindert werden kann?",
        answerA = "Truman-Doktrin",
        answerB = "Domino-Theorie",
        answerC = "MAD (Mutually Assured Destruction)",
        answerD = "Massive Retaliation",
        correctAnswer = 2, // C
        explanation = "MAD (Gegenseitig gesicherter Vernichtung) war die nukleare Abschreckungsstrategie des Kalten Krieges: Da beide Seiten den anderen vollständig vernichten könnten, würde kein rationaler Akteur einen Erstschlag wagen.",
        difficulty = 2,
        funFact = "Das englische Akronym MAD (Mutually Assured Destruction) bedeutet auf Deutsch 'verrückt' – ein bewusst gewählter Name, der die Absurdität der Strategie widerspiegelt."
    ),

    // Question 24 - Korean War (Proxy War)
    Question(
        categoryId = 3,
        questionText = "Welcher Krieg (1950–1953) war einer der ersten großen Stellvertreterkriege des Kalten Krieges?",
        answerA = "Vietnamkrieg",
        answerB = "Koreakrieg",
        answerC = "Indochinakrieg",
        answerD = "Suez-Krise",
        correctAnswer = 1, // B
        explanation = "Der Koreakrieg (1950–1953) war ein Stellvertreterkrieg: Der kommunistische Norden wurde von China und der Sowjetunion unterstützt, der Süden von den USA und UN. Er endete mit einem Waffenstillstand, nicht mit einem Friedensvertrag.",
        difficulty = 2,
        funFact = "Technisch gesehen befinden sich Nord- und Südkorea noch immer im Krieg – es wurde nie ein Friedensvertrag unterzeichnet, nur ein Waffenstillstand. Die 'demilitarisierte Zone' zwischen beiden Staaten ist heute einer der am stärksten bewachten Grenzen der Welt."
    ),

    // Question 25 - Vietnam War
    Question(
        categoryId = 3,
        questionText = "Wie lautete der Codename für den US-amerikanischen Einsatz chemischer Entlaubungsmittel im Vietnamkrieg?",
        answerA = "Agent Orange",
        answerB = "Operation Ranch Hand",
        answerC = "Napalm B",
        answerD = "Project Popeye",
        correctAnswer = 0, // A
        explanation = "Agent Orange war das bekannteste Entlaubungsmittel, das die USA im Vietnamkrieg einsetzten. Es enthielt Dioxin und verursachte bei Millionen Vietnamesen und US-Veteranen schwere Gesundheitsschäden.",
        difficulty = 2,
        funFact = "Der Name 'Agent Orange' kommt von den orangefarbenen Streifen auf den Fässern, in denen das Mittel transportiert wurde. Die USA versprühten insgesamt 72 Millionen Liter davon über Vietnam."
    ),

    // Question 26 - WWII: Battle of Britain
    Question(
        categoryId = 3,
        questionText = "Welche Luftschlacht 1940 verhinderte eine deutsche Invasion Großbritanniens?",
        answerA = "Operation Dynamo",
        answerB = "Battle of Britain",
        answerC = "Der Blitz",
        answerD = "Operation Sea Lion",
        correctAnswer = 1, // B
        explanation = "Die Battle of Britain (August–Oktober 1940) war die erste größere Kampagne, die ausschließlich in der Luft geführt wurde. Die RAF wehrte die Luftwaffe ab und verhinderte die geplante Invasion Operation Sea Lion.",
        difficulty = 2,
        funFact = "Winston Churchill würdigte die RAF-Piloten mit dem berühmten Satz: 'Never in the field of human conflict was so much owed by so many to so few.' (Nie in der Geschichte menschlicher Konflikte schuldeten so viele so wenigen so viel.)"
    ),

    // Question 27 - WWII: Operation Barbarossa
    Question(
        categoryId = 3,
        questionText = "Wann begann Operation Barbarossa – der deutsche Angriff auf die Sowjetunion?",
        answerA = "1. September 1939",
        answerB = "10. Mai 1940",
        answerC = "22. Juni 1941",
        answerD = "7. Dezember 1941",
        correctAnswer = 2, // C
        explanation = "Operation Barbarossa begann am 22. Juni 1941 mit dem deutschen Überfall auf die Sowjetunion. Es war der bis dahin größte Landkrieg der Geschichte mit über 3 Millionen deutschen Soldaten.",
        difficulty = 2,
        funFact = "Hitler plante, die Sowjetunion in wenigen Wochen zu besiegen – ähnlich wie Frankreich. Als der 'Blitzkrieg' im Winter scheiterte, war Deutschland auf einen langen Zermürbungskrieg nicht vorbereitet."
    ),

    // Question 28 - Cold War: Truman Doctrine
    Question(
        categoryId = 3,
        questionText = "Was besagte die Truman-Doktrin von 1947?",
        answerA = "Die USA würden Länder unterstützen, die gegen kommunistische Bedrohung kämpfen",
        answerB = "Die USA verpflichteten sich zur nuklearen Abrüstung",
        answerC = "Die USA würden Europa wirtschaftlich wieder aufbauen",
        answerD = "Die USA erkannten die Sowjetunion diplomatisch nicht an",
        correctAnswer = 0, // A
        explanation = "Mit der Truman-Doktrin erklärte US-Präsident Truman am 12. März 1947, dass die USA freie Völker unterstützen würden, die sich kommunistischer Unterwerfung widersetzten. Anlass war Griechenland und die Türkei.",
        difficulty = 2,
        funFact = "Die Truman-Doktrin war die offizielle Absage an den Isolationismus der USA – ab jetzt mischten sie sich aktiv in weltweite Konflikte ein. Kritiker sagten, sie gebe den USA einen Blankocheck für Interventionen überall auf der Welt."
    ),

    // Question 29 - Cold War: Berlin Crisis 1948
    Question(
        categoryId = 3,
        questionText = "Welcher US-General organisierte die Berliner Luftbrücke 1948/49?",
        answerA = "Omar Bradley",
        answerB = "Douglas MacArthur",
        answerC = "Lucius D. Clay",
        answerD = "Curtis LeMay",
        correctAnswer = 2, // C
        explanation = "General Lucius D. Clay war US-Militärgouverneur in Deutschland und organisierte die Berliner Luftbrücke. Auf seine Initiative hin entschieden die Alliierten, Berlin nicht aufzugeben und per Luft zu versorgen.",
        difficulty = 2,
        funFact = "Clay hatte ursprünglich vorgeschlagen, mit einem gepanzerten Konvoi die Blockade zu brechen. Truman lehnte ab, da dies zu einem direkten militärischen Konflikt führen könnte. Die Luftbrücke war der sichere Kompromiss."
    ),

    // Question 30 - Cold War: Space Race
    Question(
        categoryId = 3,
        questionText = "Welcher Satellit war der erste, der 1957 die Erde umkreiste und das Wettrüsten im All einleitete?",
        answerA = "Explorer 1",
        answerB = "Sputnik 1",
        answerC = "Vostok 1",
        answerD = "Telstar",
        correctAnswer = 1, // B
        explanation = "Sputnik 1 wurde am 4. Oktober 1957 von der Sowjetunion gestartet und war der erste künstliche Erdsatellit. Sein Piepen aus dem All löste in den USA den 'Sputnik-Schock' aus.",
        difficulty = 2,
        funFact = "Das Wort 'Sputnik' bedeutet auf Russisch schlicht 'Begleiter' oder 'Weggefährte'. Sein Piepen konnte weltweit per Radio empfangen werden – ein propagandistischer Triumph für die Sowjetunion."
    ),

    // Question 31 - WWII: Holocaust Numbers
    Question(
        categoryId = 3,
        questionText = "Wie viele europäische Juden wurden im Holocaust ermordet?",
        answerA = "Etwa 1 Million",
        answerB = "Etwa 3 Millionen",
        answerC = "Etwa 6 Millionen",
        answerD = "Etwa 9 Millionen",
        correctAnswer = 2, // C
        explanation = "Etwa sechs Millionen Juden wurden im Holocaust ermordet – zwei Drittel der damaligen jüdischen Bevölkerung Europas. Daneben wurden auch Sinti, Roma, Behinderte, Homosexuelle und politische Gefangene verfolgt.",
        difficulty = 2,
        funFact = "Der Begriff 'Holocaust' kommt aus dem Griechischen und bedeutet 'vollständiges Verbrennen'. In Israel und unter Juden wird die Shoah (hebräisch für 'Katastrophe') bevorzugt."
    ),

    // Question 32 - WWII: Atlantic Charter
    Question(
        categoryId = 3,
        questionText = "Welche Grundsatzerklärung unterzeichneten Roosevelt und Churchill 1941 auf einem Schiff im Atlantik?",
        answerA = "Erklärung der Vereinten Nationen",
        answerB = "Atlantik-Charta",
        answerC = "Casablanca-Deklaration",
        answerD = "Potsdamer Abkommen",
        correctAnswer = 1, // B
        explanation = "Die Atlantik-Charta vom 14. August 1941 legte die Kriegsziele der Alliierten fest: Selbstbestimmungsrecht der Völker, Freihandel, kollektive Sicherheit. Sie wurde zur Grundlage der UN-Charta.",
        difficulty = 2,
        funFact = "Die Atlantik-Charta wurde auf dem Kriegsschiff HMS Prince of Wales unterzeichnet – nur wenige Monate später, im Dezember 1941, versenkten japanische Flugzeuge das Schiff in der Malakka-Straße."
    ),

    // Question 33 - Post-war: Division of Germany
    Question(
        categoryId = 3,
        questionText = "Welche Stadt wurde als provisorische Hauptstadt der Bundesrepublik Deutschland gewählt?",
        answerA = "Frankfurt am Main",
        answerB = "Hamburg",
        answerC = "Bonn",
        answerD = "Hannover",
        correctAnswer = 2, // C
        explanation = "Bonn wurde 1949 als provisorische Hauptstadt der Bundesrepublik gewählt. Man wollte keinen dauerhaften Eindruck erwecken, um die Hoffnung auf Wiedervereinigung zu erhalten. Es blieb 40 Jahre lang Hauptstadt.",
        difficulty = 2,
        funFact = "Bonn wurde auch 'Bundesdorf' oder 'Hauptstadt ohne Eigenschaften' genannt, weil es so klein und unspektakulär war. Bundeskanzler Adenauer bevorzugte es auch, weil es nah an seinem Wohnort Rhöndorf lag."
    ),

    // Question 34 - Cold War: Iron Curtain
    Question(
        categoryId = 3,
        questionText = "Wer prägte 1946 den Begriff 'Eiserner Vorhang' für die Teilung Europas?",
        answerA = "Harry S. Truman",
        answerB = "Winston Churchill",
        answerC = "Charles de Gaulle",
        answerD = "George Marshall",
        correctAnswer = 1, // B
        explanation = "Winston Churchill verwendete den Begriff 'Eiserner Vorhang' in seiner berühmten Rede in Fulton, Missouri am 5. März 1946: 'From Stettin in the Baltic to Trieste in the Adriatic, an iron curtain has descended across the continent.'",
        difficulty = 2,
        funFact = "Churchill war zu diesem Zeitpunkt gar nicht mehr Premierminister – er war nach seiner Wahlniederlage 1945 Oppositionsführer. Seine Rede war dennoch wegweisend für die Außenpolitik des Westens."
    ),

    // Question 35 - WWII: Battle of El Alamein
    Question(
        categoryId = 3,
        questionText = "Welcher britische General besiegte Rommels Afrikakorps in der zweiten Schlacht von El Alamein (1942)?",
        answerA = "Harold Alexander",
        answerB = "Bernard Montgomery",
        answerC = "Claude Auchinleck",
        answerD = "Archibald Wavell",
        correctAnswer = 1, // B
        explanation = "General Bernard Montgomery besiegte Rommels Deutsches Afrikakorps in der zweiten Schlacht von El Alamein (23. Oktober – 4. November 1942). Es war ein Wendepunkt im Nordafrikafeldzug.",
        difficulty = 2,
        funFact = "Churchill sagte über El Alamein: 'Vor Alamein hatten wir keine Siege; nach Alamein keine Niederlagen.' Montgomery wurde danach als 'Monty' gefeiert – eine Figur, die Churchill selbst nicht besonders mochte."
    ),

    // Question 36 - Cold War: NATO Germany
    Question(
        categoryId = 3,
        questionText = "In welchem Jahr trat die Bundesrepublik Deutschland der NATO bei?",
        answerA = "1949",
        answerB = "1952",
        answerC = "1955",
        answerD = "1957",
        correctAnswer = 2, // C
        explanation = "Die Bundesrepublik Deutschland trat am 9. Mai 1955 der NATO bei. Dies war einer der Hauptgründe für die Gründung des Warschauer Pakts wenige Tage später.",
        difficulty = 2,
        funFact = "Für die Sowjetunion war der NATO-Beitritt Westdeutschlands ein Albtraum – sie erinnerte sich an zwei deutsche Invasionen in weniger als 30 Jahren und fürchtete eine Revanche."
    ),

    // Question 37 - WWII: Concentration Camps
    Question(
        categoryId = 3,
        questionText = "Welches war das erste Konzentrationslager, das die Nazis 1933 errichteten?",
        answerA = "Buchenwald",
        answerB = "Bergen-Belsen",
        answerC = "Dachau",
        answerD = "Sachsenhausen",
        correctAnswer = 2, // C
        explanation = "Das KZ Dachau bei München wurde am 22. März 1933, kurz nach Hitlers Machtübernahme, als erstes Konzentrationslager eröffnet. Es diente zunächst zur Inhaftierung politischer Gegner.",
        difficulty = 2,
        funFact = "Dachau war ein 'Modell' für die späteren Lager – sein Kommandant Theodor Eicke entwickelte ein System der Grausamkeit, das in anderen Lagern übernommen wurde. Er schulte auch das Wachpersonal von Auschwitz."
    ),

    // Question 38 - Cold War: SALT Treaties
    Question(
        categoryId = 3,
        questionText = "Wofür steht die Abkürzung SALT in der Rüstungskontrolldiplomatie des Kalten Krieges?",
        answerA = "Strategic Arms Limitation Talks",
        answerB = "Soviet-American Liberation Treaty",
        answerC = "Security Alliance Leadership Treaty",
        answerD = "Strategic Atomic Launch Technology",
        correctAnswer = 0, // A
        explanation = "SALT steht für 'Strategic Arms Limitation Talks' (Verhandlungen zur Begrenzung strategischer Rüstungen). SALT I wurde 1972 unterzeichnet, SALT II 1979. Sie begrenzten nukleare Interkontinentalraketen.",
        difficulty = 2,
        funFact = "SALT I wurde von Nixon und Breschnew unterzeichnet – ausgerechnet von dem US-Präsidenten, der seine Karriere als glühender Antikommunist begonnen hatte. Nixon war der erste US-Präsident, der die Sowjetunion besuchte."
    ),

    // Question 39 - Post-war: Nuremberg Laws
    Question(
        categoryId = 3,
        questionText = "Was regelte das Londoner Statut von 1945, das die Grundlage für die Nürnberger Prozesse bildete?",
        answerA = "Die Aufteilung Deutschlands in Besatzungszonen",
        answerB = "Die Definition von Kriegsverbrechen und Verbrechen gegen die Menschlichkeit",
        answerC = "Die Entnazifizierung der deutschen Bevölkerung",
        answerD = "Die Reparationszahlungen Deutschlands",
        correctAnswer = 1, // B
        explanation = "Das Londoner Statut definierte erstmals völkerrechtlich Kriegsverbrechen, Verbrechen gegen den Frieden und Verbrechen gegen die Menschlichkeit. Es schuf die rechtliche Grundlage für internationale Strafverfolgung.",
        difficulty = 2,
        funFact = "Die Verteidiger der Angeklagten in Nürnberg argumentierten, es sei rückwirkendes Recht – Handlungen könnten nicht für Verbrechen bestraft werden, die zum Zeitpunkt der Begehung nicht kriminalisiert waren. Das Gericht lehnte dies ab."
    ),

    // Question 40 - Cold War: Berlin Wall Victims
    Question(
        categoryId = 3,
        questionText = "Wie viele Menschen kamen nach historischen Schätzungen beim Versuch um, die Berliner Mauer zu überwinden?",
        answerA = "Ca. 40",
        answerB = "Ca. 140",
        answerC = "Ca. 400",
        answerD = "Ca. 1.400",
        correctAnswer = 1, // B
        explanation = "Die Berliner Mauer kostete mindestens 140 Menschen das Leben – viele beim Fluchtversuch erschossen, ertrunken oder durch Unfälle. Die genaue Zahl ist bis heute Gegenstand historischer Forschung.",
        difficulty = 2,
        funFact = "Das jüngste Todesopfer der Mauer war der 18-jährige Peter Fechter, der am 17. August 1961 – nur vier Tage nach dem Mauerbau – angeschossen wurde und eine Stunde lang im Grenzstreifen verblutete, während Schaulustige zusahen."
    ),

    // Question 41 - WWII: Lend-Lease Act
    Question(
        categoryId = 3,
        questionText = "Was ermöglichte der amerikanische Leih-Pacht-Akt (Lend-Lease Act) von 1941?",
        answerA = "Den Eintritt der USA in den Zweiten Weltkrieg",
        answerB = "Die Lieferung von Kriegsmaterial an die Alliierten ohne Bezahlung",
        answerC = "Ein Bündnis zwischen den USA und der Sowjetunion",
        answerD = "Die Besetzung Nordafrikas durch die USA",
        correctAnswer = 1, // B
        explanation = "Der Lend-Lease Act erlaubte den USA, Kriegsmaterial an die Alliierten zu 'verleihen' oder 'verpachten', ohne sofortige Bezahlung. Großbritannien, die Sowjetunion und andere Länder erhielten Waffen, Fahrzeuge und Lebensmittel im Wert von 50 Milliarden Dollar.",
        difficulty = 2,
        funFact = "Die Sowjetunion erhielt durch Lend-Lease über 400.000 Fahrzeuge – darunter Hunderttausende Lastwagen, die die sowjetische Logistik entscheidend verbesserten. Ohne diese Hilfe hätte die Rote Armee viele ihrer Operationen nicht durchführen können."
    ),

    // Question 42 - Post-war: Denazification
    Question(
        categoryId = 3,
        questionText = "Wie nannte man das Verfahren, mit dem die Alliierten Deutschland nach 1945 von NS-Ideologie befreien wollten?",
        answerA = "Demokratisierung",
        answerB = "Reeducation",
        answerC = "Entnazifizierung",
        answerD = "Demilitarisierung",
        correctAnswer = 2, // C
        explanation = "Die Entnazifizierung war ein alliertes Programm zur Entfernung von Nazi-Mitgliedern aus öffentlichen Ämtern und zur Strafverfolgung von Kriegsverbrechern. Sie verlief in den vier Zonen sehr unterschiedlich.",
        difficulty = 2,
        funFact = "Das Entnazifizierungsverfahren war bei der Bevölkerung so unbeliebt, dass es bald als 'Mitläuferfabrik' verspottet wurde – wer sich geschickt anstellte, kam mit einer kleinen Geldstrafe davon, während echte Täter oft glimpflich behandelt wurden."
    ),

    // Question 43 - Cold War: Hungarian Uprising
    Question(
        categoryId = 3,
        questionText = "Wann wurde der ungarische Volksaufstand von sowjetischen Truppen niedergeschlagen?",
        answerA = "1953",
        answerB = "1956",
        answerC = "1961",
        answerD = "1968",
        correctAnswer = 1, // B
        explanation = "Der ungarische Volksaufstand begann am 23. Oktober 1956 und wurde im November 1956 von sowjetischen Panzern brutal niedergeschlagen. Etwa 2.500 Ungarn und 700 sowjetische Soldaten starben.",
        difficulty = 2,
        funFact = "Als die Sowjetunion 1956 in Ungarn einmarschierte, war die Welt abgelenkt – gleichzeitig fand die Suez-Krise statt, bei der Frankreich, Großbritannien und Israel Ägypten angriffen. Die Sowjetunion nutzte die Ablenkung geschickt aus."
    ),

    // Question 44 - WWII: Pacific Theater
    Question(
        categoryId = 3,
        questionText = "Welches Ereignis am 7. Dezember 1941 führte zum Kriegseintritt der USA in den Zweiten Weltkrieg?",
        answerA = "Deutsche Kriegserklärung an die USA",
        answerB = "Japanischer Angriff auf Pearl Harbor",
        answerC = "Versenkung der USS Reuben James",
        answerD = "Japanische Invasion der Philippinen",
        correctAnswer = 1, // B
        explanation = "Der japanische Angriff auf Pearl Harbor am 7. Dezember 1941 forderte 2.403 amerikanische Todesopfer und versenkte oder beschädigte 19 Schiffe der US-Marine. Roosevelt nannte es 'a date which will live in infamy'.",
        difficulty = 2,
        funFact = "Die japanischen Piloten griffen bewusst am Sonntagmorgen an, als sie glaubten, die US-Flotte wäre am wenigsten vorbereitet. Aber die drei amerikanischen Flugzeugträger waren zufällig ausgelaufen und überlebten – was den Kriegsverlauf entschied."
    ),

    // Question 45 - Cold War: Prague Spring
    Question(
        categoryId = 3,
        questionText = "Welche Bewegung versuchte 1968 in der Tschechoslowakei, einen 'Sozialismus mit menschlichem Antlitz' einzuführen?",
        answerA = "Prager Frühling",
        answerB = "Samtene Revolution",
        answerC = "Charta 77",
        answerD = "Solidarność-Bewegung",
        correctAnswer = 0, // A
        explanation = "Der Prager Frühling unter Alexander Dubček versuchte, den Kommunismus zu liberalisieren. Im August 1968 wurde er von Truppen des Warschauer Pakts beendet. Die Breschnew-Doktrin rechtfertigte das Eingreifen.",
        difficulty = 2,
        funFact = "Radio Prag sendete am Tag des Einmarsches auf 21 Frequenzen gleichzeitig – und rief die Weltöffentlichkeit um Hilfe. Die Nachricht verbreitete sich weltweit, aber niemand half. Einer der Radiosprecher brach weinend zusammen."
    ),

    // Question 46 - Post-war: GDR
    Question(
        categoryId = 3,
        questionText = "Wie nannte die DDR ihren Geheimdienst, der Millionen Bürger bespitzelte?",
        answerA = "Volkspolizei",
        answerB = "Stasi (MfS)",
        answerC = "Nationale Volksarmee",
        answerD = "NKVD",
        correctAnswer = 1, // B
        explanation = "Die Stasi (Ministerium für Staatssicherheit, MfS) war der Geheimdienst der DDR und galt als einer der effizientesten Repressions-Apparate der Geschichte. Sie hatte über 90.000 Hauptamtliche und 174.000 Informelle Mitarbeiter.",
        difficulty = 2,
        funFact = "Die Stasi hatte statistisch gesehen einen Spitzel auf 63 DDR-Bürger – dichter als die Gestapo oder der sowjetische KGB. Ihre Akten gestapelt würden eine Strecke von 111 Kilometern ergeben."
    ),

    // Question 47 - WWII: Kristallnacht
    Question(
        categoryId = 3,
        questionText = "Wie nennt man die Novemberpogrome von 1938, bei denen Nazi-Schlägertrupps jüdische Geschäfte und Synagogen zerstörten?",
        answerA = "Reichspogromnacht",
        answerB = "Kristallnacht",
        answerC = "Reichskristallnacht",
        answerD = "Alle drei Begriffe sind korrekt",
        correctAnswer = 3, // D
        explanation = "Die Novemberpogrome vom 9./10. November 1938 werden je nach Perspektive als 'Reichspogromnacht', 'Kristallnacht' (wegen der zersplitterten Schaufensterscheiben) oder 'Reichskristallnacht' bezeichnet. Ca. 400 Menschen starben, 1.400 Synagogen wurden zerstört.",
        difficulty = 2,
        funFact = "Der Begriff 'Kristallnacht' war ursprünglich sarkastischer schwarzer Humor der Berliner Bevölkerung – ein zynischer Verweis auf die Scherben jüdischer Geschäfte. Heute wird der Begriff von vielen als verharmlosend abgelehnt."
    ),

    // Question 48 - Cold War: Berlin Airlift
    Question(
        categoryId = 3,
        questionText = "Wie viele Tage dauerte die Berliner Luftbrücke (Rosinenbomber-Einsatz) insgesamt?",
        answerA = "Etwa 100 Tage",
        answerB = "Etwa 200 Tage",
        answerC = "Etwa 320 Tage",
        answerD = "Etwa 462 Tage",
        correctAnswer = 3, // D
        explanation = "Die Berliner Luftbrücke dauerte vom 26. Juni 1948 bis zum 30. September 1949 – insgesamt 462 Tage. Auch nach Ende der offiziellen Blockade (12. Mai 1949) wurden Vorräte aufgebaut.",
        difficulty = 2,
        funFact = "US-Pilot Gail Halvorsen begann, Süßigkeiten an Fallschirmchen aus seinem Flugzeug abzuwerfen. Kinder erkannten seinen Flugzeugtyp am 'Wackeln' der Flügel. Er wurde weltberühmt als 'Candy Bomber' oder 'Rosinenbomber'."
    ),

    // Question 49 - Cold War: Cuban Revolution
    Question(
        categoryId = 3,
        questionText = "Wer war der Anführer der kubanischen Revolution, der 1959 die Macht übernahm und ein kommunistisches Regime errichtete?",
        answerA = "Che Guevara",
        answerB = "Raúl Castro",
        answerC = "Fidel Castro",
        answerD = "Fulgencio Batista",
        correctAnswer = 2, // C
        explanation = "Fidel Castro übernahm am 1. Januar 1959 die Macht in Kuba, nachdem seine Guerillakämpfer das Regime von Diktator Fulgencio Batista gestürzt hatten. Cuba wurde zum kommunistischen Staat und zum Zankapfel im Kalten Krieg.",
        difficulty = 2,
        funFact = "Fidel Castro überlebte nach CIA-Angaben über 600 Attentatsversuche – einige davon absurd kreativ: vergiftete Zigarren, eine explodierende Muschel beim Tauchen, Schuhe mit Thallium-Pulver, das seinen Bart ausfallen lassen sollte."
    ),

    // Question 50 - Post-war: Adenauer and Rearmament
    Question(
        categoryId = 3,
        questionText = "Wer war der erste Bundeskanzler der Bundesrepublik Deutschland, der die Wiederbewaffnung und NATO-Integration vorantrieb?",
        answerA = "Theodor Heuss",
        answerB = "Kurt Schumacher",
        answerC = "Konrad Adenauer",
        answerD = "Ernst Reuter",
        correctAnswer = 2, // C
        explanation = "Konrad Adenauer war von 1949 bis 1963 erster Bundeskanzler. Er führte die Westbindung und Wiederbewaffnung Deutschlands durch – gegen starken innenpolitischen Widerstand. Die Bundeswehr wurde 1955 gegründet.",
        difficulty = 2,
        funFact = "Adenauer war bei seiner Wahl zum Bundeskanzler 73 Jahre alt und regierte bis zu seinem 87. Lebensjahr. Er ist bis heute der älteste Bundeskanzler Deutschlands – und nach einer repräsentativen Umfrage auch der beliebteste."
    ),
)
