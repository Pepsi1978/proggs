package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun musicQuestionsEasy2(): List<Question> = listOf(

    // ── Kinderlieder und Volkslieder (8 Fragen) ───────────────────────────────

    Question(
        categoryId = 5,
        questionText = "Wie heisst das deutsche Volkslied, in dem ein Jaeger aus dem Kurpfalz vorkommt?",
        answerA = "Haenschen klein",
        answerB = "Ein Jaeger aus Kurpfalz",
        answerC = "Muss i denn",
        answerD = "Im Fruehling",
        correctAnswer = 1,
        explanation = "'Ein Jaeger aus Kurpfalz' ist ein deutsches Volkslied aus dem 18. Jahrhundert. Es beschreibt die Freude eines Jaegers beim Jagen im gruenen Wald.",
        difficulty = 1,
        funFact = "Das Lied gilt als eines der bekanntesten deutschen Volkslieder und wird auch heute noch in Schulen gelehrt."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Kinderlied handelt von einem kleinen Jungen, der alleine zieht?",
        answerA = "Alle meine Entchen",
        answerB = "Backe backe Kuchen",
        answerC = "Haenschen klein",
        answerD = "Der Mond ist aufgegangen",
        correctAnswer = 2,
        explanation = "'Haenschen klein' ist ein deutsches Kinderlied, in dem ein kleiner Junge namens Hans alleine in die weite Welt zieht und schliesslich nach Hause zurueckkehrt.",
        difficulty = 1,
        funFact = "Das Lied stammt wahrscheinlich aus dem 19. Jahrhundert und ist bis heute eines der bekanntesten deutschen Kinderlieder."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Kinderlied beginnt mit den Worten 'Alle meine Entchen schwimmen auf dem See'?",
        answerA = "Alle meine Entchen",
        answerB = "Bi-Ba-Butzemann",
        answerC = "Froehliche Weihnacht",
        answerD = "Wer hat die Kokosnuss geklaut",
        correctAnswer = 0,
        explanation = "'Alle meine Entchen' ist ein klassisches deutsches Kinderlied, das Enten beim Schwimmen und Tauchen beschreibt. Die einfache Melodie macht es ideal fuer kleine Kinder.",
        difficulty = 1,
        funFact = "Das Lied ist seit dem 19. Jahrhundert bekannt und wird in vielen deutschen Kindergaerten taeglich gesungen."
    ),

    Question(
        categoryId = 5,
        questionText = "In welchem bekannten Volkslied sehnt sich jemand aus der Fremde nach der Heimat?",
        answerA = "La Paloma",
        answerB = "Muss i denn",
        answerC = "Oh Susanna",
        answerD = "Kein schoener Land",
        correctAnswer = 1,
        explanation = "'Muss i denn, muss i denn zum Staedtele hinaus' ist ein schwaebiches Volkslied, das die Sehnsucht nach der Heimat und der Geliebten ausdruckt. Elvis Presley machte es mit 'Wooden Heart' weltbekannt.",
        difficulty = 1,
        funFact = "Elvis Presley nahm 1960 eine Coverversion unter dem Titel 'Wooden Heart' auf, die in vielen Laendern ein Nummer-1-Hit wurde."
    ),

    Question(
        categoryId = 5,
        questionText = "Wie heisst das bekannte Abendlied, das mit 'Der Mond ist aufgegangen' beginnt?",
        answerA = "Guten Abend, gut' Nacht",
        answerB = "Abendlied",
        answerC = "Der Mond ist aufgegangen",
        answerD = "Schlaf, Kindlein, schlaf",
        correctAnswer = 2,
        explanation = "'Der Mond ist aufgegangen' ist ein deutsches Abendlied, geschrieben von Matthias Claudius im Jahr 1779. Die Melodie stammt von Johann Abraham Peter Schulz.",
        difficulty = 1,
        funFact = "Das Lied gilt als eines der schoensten deutschen Volkslieder und wird weltweit in kirchlichen und schulischen Zusammenhaengen gesungen."
    ),

    Question(
        categoryId = 5,
        questionText = "Wer hat das bekannte Wiegenlied 'Guten Abend, gut' Nacht' komponiert?",
        answerA = "Ludwig van Beethoven",
        answerB = "Franz Schubert",
        answerC = "Johannes Brahms",
        answerD = "Robert Schumann",
        correctAnswer = 2,
        explanation = "Das beruehmt Brahms-Wiegenlied 'Guten Abend, gut' Nacht' wurde 1868 von Johannes Brahms komponiert und ist eines der bekanntesten Wiegenlieder der Welt.",
        difficulty = 1,
        funFact = "Brahms komponierte das Wiegenlied fuer die Geburt des zweiten Kindes seiner Freundin Bertha Faber und schrieb dabei die Noten auf einer Postkarte."
    ),

    Question(
        categoryId = 5,
        questionText = "Wie heisst das weltweit bekannte Volkslied, das oft mit 'Auf der Mauer, auf der Lauer' beginnt?",
        answerA = "Das Wandern ist des Muellers Lust",
        answerB = "Auf der Mauer, auf der Lauer",
        answerC = "Kuckuck, kuckuck, ruft's aus dem Wald",
        answerD = "Hei, wie schoen ist Ringelreihn",
        correctAnswer = 1,
        explanation = "'Auf der Mauer, auf der Lauer sitzt ne kleine Wanze' ist ein bekanntes Kinderlied mit Mitmach-Elementen, bei dem bestimmte Silben weggelassen werden.",
        difficulty = 1,
        funFact = "Das Lied ist besonders beliebt, weil Kinder bei jeder Strophe ein Silbenpaar weglassen, bis das Wort 'Wanze' vollstaendig verstummt ist."
    ),

    Question(
        categoryId = 5,
        questionText = "Aus welchem Land stammt das Volkslied 'O sole mio'?",
        answerA = "Spanien",
        answerB = "Frankreich",
        answerC = "Griechenland",
        answerD = "Italien",
        correctAnswer = 3,
        explanation = "'O sole mio' ist ein beruehmt neapolitanisches Lied aus Italien, komponiert 1898 von Eduardo di Capua. Es beschreibt die Schoenheit der Sonne.",
        difficulty = 1,
        funFact = "'O sole mio' wurde von Elvis Presley unter dem Titel 'It's Now or Never' ins Englische adaptiert und war 1960 ein weltweiter Nummer-1-Hit."
    ),

    // ── Weihnachtsmusik (7 Fragen) ────────────────────────────────────────────

    Question(
        categoryId = 5,
        questionText = "Welches Weihnachtslied beginnt mit den Worten 'Stille Nacht, heilige Nacht'?",
        answerA = "O Tannenbaum",
        answerB = "Stille Nacht",
        answerC = "Froehliche Weihnacht",
        answerD = "Kling, Gloeckchen",
        correctAnswer = 1,
        explanation = "'Stille Nacht, heilige Nacht' wurde 1818 in Oberndorf bei Salzburg von Josef Mohr (Text) und Franz Xaver Gruber (Melodie) geschaffen.",
        difficulty = 1,
        funFact = "'Stille Nacht' wurde inzwischen in ueber 300 Sprachen und Dialekte uebersetzt und ist das bekannteste Weihnachtslied der Welt."
    ),

    Question(
        categoryId = 5,
        questionText = "Wer hat die Weihnachtshymne 'White Christmas' gesungen und damit die meistverkaufte Single aller Zeiten geschaffen?",
        answerA = "Frank Sinatra",
        answerB = "Nat King Cole",
        answerC = "Bing Crosby",
        answerD = "Dean Martin",
        correctAnswer = 2,
        explanation = "Bing Crosby nahm 'White Christmas' 1942 auf. Die Single gilt als die meistverkaufte physische Single der Geschichte mit ueber 50 Millionen Exemplaren.",
        difficulty = 1,
        funFact = "Bing Crosby sang 'White Christmas' bereits vor der Veroeffentlichung im Radio. Es war das erste Mal, dass ein Lied durch das Radio zur Nummer 1 wurde."
    ),

    Question(
        categoryId = 5,
        questionText = "In welchem Land entstand das Weihnachtslied 'Jingle Bells' (1857)?",
        answerA = "Grossbritannien",
        answerB = "Deutschland",
        answerC = "Australien",
        answerD = "USA",
        correctAnswer = 3,
        explanation = "'Jingle Bells' wurde 1857 von James Lord Pierpont in den USA geschrieben. Es war urspruenglich kein Weihnachtslied, sondern ein Erntedanklied.",
        difficulty = 1,
        funFact = "'Jingle Bells' war 1965 das erste Lied, das im Weltraum gespielt wurde – Astronauten der Gemini-6-Mission spielten es auf einer Mundharmonika."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Tier zieht den Schlitten im Lied 'Rudolph, the Red-Nosed Reindeer'?",
        answerA = "Ein Elch",
        answerB = "Ein Rentier",
        answerC = "Ein Pferd",
        answerD = "Ein Esel",
        correctAnswer = 1,
        explanation = "Rudolph ist ein Rentier mit einer leuchtend roten Nase, das den Schlitten des Weihnachtsmannes durch den Nebel fuehrt.",
        difficulty = 1,
        funFact = "Rudolph the Red-Nosed Reindeer wurde 1939 von Robert L. May fuer ein Kaufhaus-Malbuch erfunden, bevor er zum weltberuehm ten Weihnachtscharakter wurde."
    ),

    Question(
        categoryId = 5,
        questionText = "Wie heisst das bekannte Weihnachtslied von Mariah Carey (1994)?",
        answerA = "Santa Baby",
        answerB = "Last Christmas",
        answerC = "All I Want for Christmas Is You",
        answerD = "Underneath the Tree",
        correctAnswer = 2,
        explanation = "'All I Want for Christmas Is You' von Mariah Carey (1994) ist einer der erfolgreichsten Weihnachtssongs aller Zeiten und erreicht jedes Jahr wieder die Chartspitze.",
        difficulty = 1,
        funFact = "Der Song erreichte erst 2019 – 25 Jahre nach seiner Veroeffentlichung – erstmals die Spitze der US-Charts und ist seitdem jedes Jahr in den Top 10."
    ),

    Question(
        categoryId = 5,
        questionText = "Von welcher Band stammt der Weihnachtshit 'Last Christmas' (1984)?",
        answerA = "Duran Duran",
        answerB = "Wham!",
        answerC = "Culture Club",
        answerD = "Spandau Ballet",
        correctAnswer = 1,
        explanation = "'Last Christmas' wurde 1984 von Wham! veroeffentlicht und ist bis heute einer der beliebtesten Weihnachtssongs der Welt.",
        difficulty = 1,
        funFact = "Trotz seiner enormen Beliebtheit erreichte 'Last Christmas' damals nur Platz 2 in den britischen Charts – verdraengt von 'Do They Know It's Christmas?'."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Weihnachtslied fragt 'Frohe Weihnachten, weisst du noch?'",
        answerA = "Driving Home for Christmas",
        answerB = "It's the Most Wonderful Time of the Year",
        answerC = "Feliz Navidad",
        answerD = "Froehliche Weihnacht ueberall",
        correctAnswer = 3,
        explanation = "'Froehliche Weihnacht ueberall' ist ein traditionelles deutsches Weihnachtslied, das in vielen Familien gesungen wird und die Weihnachtszeit besingt.",
        difficulty = 1,
        funFact = "Das Lied 'Froehliche Weihnacht ueberall' entstand im 19. Jahrhundert und ist in Deutschland fester Bestandteil der Weihnachtstradition."
    ),

    // ── 80er-Jahre-Hits (8 Fragen) ────────────────────────────────────────────

    Question(
        categoryId = 5,
        questionText = "Von welchem Kuenstler stammt der 80er-Hit 'Purple Rain' (1984)?",
        answerA = "David Bowie",
        answerB = "Prince",
        answerC = "George Michael",
        answerD = "Duran Duran",
        correctAnswer = 1,
        explanation = "'Purple Rain' von Prince erschien 1984 als Titelsong des gleichnamigen Films. Das Album gilt als eines der besten der 80er Jahre.",
        difficulty = 1,
        funFact = "Das Album 'Purple Rain' von Prince war 24 Wochen lang auf Platz 1 der US-Charts und verkaufte sich weltweit ueber 25 Millionen Mal."
    ),

    Question(
        categoryId = 5,
        questionText = "Welcher Song von Michael Jackson aus den 80ern hat ein beruehmt gewordenes Moonwalk-Video?",
        answerA = "Beat It",
        answerB = "Thriller",
        answerC = "Smooth Criminal",
        answerD = "Billie Jean",
        correctAnswer = 3,
        explanation = "Im Musikvideo und bei der TV-Show 'Motown 25' 1983 zeigte Michael Jackson bei 'Billie Jean' erstmals offentlich seinen beruehmt gewordenen Moonwalk.",
        difficulty = 1,
        funFact = "Der Moonwalk wurde von Michael Jackson selbst als 'The Backslide' bezeichnet. Er sah die Bewegung zum ersten Mal bei anderen Taenzern und perfektionierte sie dann."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche Girlgroup sang in den 80ern 'Girls Just Want to Have Fun'?",
        answerA = "The Bangles",
        answerB = "The Go-Go's",
        answerC = "Cyndi Lauper",
        answerD = "Bananarama",
        correctAnswer = 2,
        explanation = "'Girls Just Want to Have Fun' ist ein Solo-Hit von Cyndi Lauper aus dem Jahr 1983. Es ist eines der bekanntesten Pop-Songs der 80er Jahre.",
        difficulty = 1,
        funFact = "Das Lied wurde urspruenglich 1979 von Robert Hazard als ein Lied aus maennlicher Perspektive geschrieben. Cyndi Lauper aenderte die Texte, um es feministisch zu gestalten."
    ),

    Question(
        categoryId = 5,
        questionText = "Von welcher Band stammt der 80er-Klassiker 'Sweet Child O' Mine' (1987)?",
        answerA = "Bon Jovi",
        answerB = "Def Leppard",
        answerC = "Guns N' Roses",
        answerD = "Aerosmith",
        correctAnswer = 2,
        explanation = "'Sweet Child O' Mine' von Guns N' Roses erschien 1987 auf dem Album 'Appetite for Destruction' und ist bis heute einer der bekanntesten Rocksongs der 80er.",
        difficulty = 1,
        funFact = "Das beruehmt gewordene Gitarrenintro wurde von Slash improvisiert, als er sich warm spielte. Axl Rose hoerte es und bat ihn, einen Song daraus zu machen."
    ),

    Question(
        categoryId = 5,
        questionText = "Welcher 80er-Hit von A-ha ist fuer sein animiertes Musikvideo bekannt?",
        answerA = "The Sun Always Shines on T.V.",
        answerB = "Take On Me",
        answerC = "Hunting High and Low",
        answerD = "I've Been Losing You",
        correctAnswer = 1,
        explanation = "'Take On Me' von A-ha (1985) ist beruhmt fuer sein innovatives Musikvideo, das Realfilm mit Stiftzeichnungen (Rotoscoping) kombinierte.",
        difficulty = 1,
        funFact = "Das 'Take On Me'-Musikvideo gewann 1986 sechs MTV Video Music Awards und wird noch heute als eines der besten Musikvideos aller Zeiten bezeichnet."
    ),

    Question(
        categoryId = 5,
        questionText = "Welcher Kuenstler sang den 80er-Hit 'Wake Me Up Before You Go-Go' (1984)?",
        answerA = "George Michael solo",
        answerB = "Wham!",
        answerC = "Duran Duran",
        answerD = "Culture Club",
        correctAnswer = 1,
        explanation = "'Wake Me Up Before You Go-Go' (1984) war ein weltweiter Nummer-1-Hit von Wham!, dem Duo bestehend aus George Michael und Andrew Ridgeley.",
        difficulty = 1,
        funFact = "George Michael soll den Liedtitel erfunden haben, als er Andrew Ridgeley eine Notiz hinterlassen wollte und aus Versehen 'Wake me up up' schrieb."
    ),

    Question(
        categoryId = 5,
        questionText = "Welcher 80er-Hit von Toto beginnt mit Regen und handelt von Afrika?",
        answerA = "Rosanna",
        answerB = "I'll Be Over You",
        answerC = "Africa",
        answerD = "Hold the Line",
        correctAnswer = 2,
        explanation = "'Africa' von Toto erschien 1982 und ist mit seinem charakteristischen Intro und dem Refrain 'I hear the drums echoing tonight' einer der ikonischsten 80er-Songs.",
        difficulty = 1,
        funFact = "Im Jahr 2019 wurde 'Africa' in der Wueste Namibias als Kunstprojekt in Dauerschleife abgespielt – ein Tribut an einen Kuenstler, der den Song liebte."
    ),

    Question(
        categoryId = 5,
        questionText = "Von welcher Band stammt 'Livin' on a Prayer' (1986)?",
        answerA = "Poison",
        answerB = "Bon Jovi",
        answerC = "Motley Crue",
        answerD = "Whitesnake",
        correctAnswer = 1,
        explanation = "'Livin' on a Prayer' von Bon Jovi erschien 1986 auf dem Album 'Slippery When Wet' und wurde ein weltweiter Nummer-1-Hit.",
        difficulty = 1,
        funFact = "Jon Bon Jovi wollte 'Livin' on a Prayer' zunaechst nicht auf das Album aufnehmen, weil er den Song fuer zu schwach hielt. Manager Doc McGhee ueberzeugte ihn schliesslich."
    ),

    // ── 90er-Jahre-Hits (8 Fragen) ────────────────────────────────────────────

    Question(
        categoryId = 5,
        questionText = "Von welcher Band stammt der 90er-Grungebestseller 'Smells Like Teen Spirit' (1991)?",
        answerA = "Pearl Jam",
        answerB = "Alice in Chains",
        answerC = "Soundgarden",
        answerD = "Nirvana",
        correctAnswer = 3,
        explanation = "'Smells Like Teen Spirit' von Nirvana (1991) gilt als die Hymne der Grunge-Bewegung und einer der einflussreichsten Rocksongs der 90er Jahre.",
        difficulty = 1,
        funFact = "Kurt Cobain schrieb den Song, nachdem seine Freundin an eine Wand geschrieben hatte: 'Kurt smells like Teen Spirit' – Teen Spirit war ein Deodorant."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche Saengerin sang den 90er-Hit 'Baby One More Time' (1998)?",
        answerA = "Christina Aguilera",
        answerB = "Mariah Carey",
        answerC = "Britney Spears",
        answerD = "Destiny's Child",
        correctAnswer = 2,
        explanation = "'...Baby One More Time' von Britney Spears erschien 1998 und war ihr Debueetsingle. Der Song erreichte weltweit Platz 1 der Charts.",
        difficulty = 1,
        funFact = "Das Schuluniformen-Musikvideo wurde von Britney Spears selbst vorgeschlagen, als sie das urspruenglich geplante Animationsvideo ablehnte."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche Boygroup sang in den 90ern 'Quit Playing Games (With My Heart)' (1996)?",
        answerA = "NSYNC",
        answerB = "Take That",
        answerC = "New Kids on the Block",
        answerD = "Backstreet Boys",
        correctAnswer = 3,
        explanation = "'Quit Playing Games (With My Heart)' war ein weltweiter Hit der Backstreet Boys aus dem Jahr 1996 und einer ihrer bekanntesten fruehen Songs.",
        difficulty = 1,
        funFact = "Die Backstreet Boys wurden nach einem Einkaufszentrum in Orlando, Florida benannt – dem Backstreet Market, einem beliebten Treffpunkt fuer Jugendliche."
    ),

    Question(
        categoryId = 5,
        questionText = "Von welcher Gruppe stammt der 90er-Latino-Hit 'Macarena' (1993)?",
        answerA = "Ricky Martin",
        answerB = "Los Del Rio",
        answerC = "Marc Anthony",
        answerD = "Shakira",
        correctAnswer = 1,
        explanation = "'Macarena' wurde 1993 von dem spanischen Duo Los Del Rio veroeffentlicht und wurde durch seinen gleichnamigen Tanz weltweit zum Partyhit.",
        difficulty = 1,
        funFact = "'Macarena' war 1996 14 Wochen lang Nummer 1 in den USA – eine der laengsten Herrschaften an der Chartspitze in der Geschichte der Billboard Hot 100."
    ),

    Question(
        categoryId = 5,
        questionText = "Welcher Kuenstler sang den 90er-Hit 'MMMBop' (1997)?",
        answerA = "New Kids on the Block",
        answerB = "Hanson",
        answerC = "Ace of Base",
        answerD = "Aqua",
        correctAnswer = 1,
        explanation = "'MMMBop' von Hanson (1997) war das Debuet der drei Brueder Taylor, Isaac und Zac Hanson und wurde ein weltweiter Nummer-1-Hit.",
        difficulty = 1,
        funFact = "Hanson schrieb 'MMMBop' bereits 1992, als die Mitglieder 9, 11 und 7 Jahre alt waren. Es dauerte fuenf Jahre, bis der Song veroeffentlicht wurde."
    ),

    Question(
        categoryId = 5,
        questionText = "Von welcher Gruppe stammt 'Barbie Girl' (1997)?",
        answerA = "Spice Girls",
        answerB = "Ace of Base",
        answerC = "Aqua",
        answerD = "B*Witched",
        correctAnswer = 2,
        explanation = "'Barbie Girl' von Aqua (1997) ist ein dänisch-norwegischer Eurodance-Hit, der weltweit ein Nummer-1-Hit wurde und fuer einige Kontroversen sorgte.",
        difficulty = 1,
        funFact = "Mattel verklagte Aqua wegen 'Barbie Girl', weil sie sagten, der Song beeintraechtigt das Image der Barbie-Puppe. Das Gericht wies die Klage ab."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche Spice Girl hatte den Spitznamen 'Posh Spice'?",
        answerA = "Mel B",
        answerB = "Emma Bunton",
        answerC = "Geri Halliwell",
        answerD = "Victoria Beckham",
        correctAnswer = 3,
        explanation = "Victoria Beckham (geborene Adams) war unter dem Spitznamen 'Posh Spice' bekannt. Die Spice Girls waren das erfolgreichste Girlgroup-Phaenomen der 90er Jahre.",
        difficulty = 1,
        funFact = "Victoria Beckham ist mit dem Fussballstar David Beckham verheiratet. Sie ist heute vor allem als Modedesignerin taetig und hat ihre eigene Luxusmodemarke."
    ),

    Question(
        categoryId = 5,
        questionText = "Von welcher Gruppe stammt der 90er-Hit 'No Scrubs' (1999)?",
        answerA = "Destiny's Child",
        answerB = "En Vogue",
        answerC = "TLC",
        answerD = "SWV",
        correctAnswer = 2,
        explanation = "'No Scrubs' von TLC (1999) war ein weltweiter Hit und wurde zu einer feministischen Hymne der spaeten 90er Jahre.",
        difficulty = 1,
        funFact = "TLC steht fuer die Anfangsbuchstaben der Mitglieder: Tionne 'T-Boz' Watkins, Lisa 'Left Eye' Lopes und Rozonda 'Chilli' Thomas."
    ),

    // ── Boygroups und Girlgroups (7 Fragen) ───────────────────────────────────

    Question(
        categoryId = 5,
        questionText = "Wie viele Mitglieder hatte die Boygroup NSYNC?",
        answerA = "4",
        answerB = "5",
        answerC = "6",
        answerD = "3",
        correctAnswer = 1,
        explanation = "NSYNC hatte 5 Mitglieder: Justin Timberlake, JC Chasez, Chris Kirkpatrick, Joey Fatone und Lance Bass. Die Gruppe war in den spaeten 90ern und fruehen 2000ern sehr erfolgreich.",
        difficulty = 1,
        funFact = "Der Name NSYNC setzt sich aus den letzten Buchstaben der Vornamen der Mitglieder zusammen: JustiN, ChriS, JoeY, JasoN und JC – spaeter Lance."
    ),

    Question(
        categoryId = 5,
        questionText = "Aus welchem Land stammen die Spice Girls?",
        answerA = "Australien",
        answerB = "USA",
        answerC = "Kanada",
        answerD = "Grossbritannien",
        correctAnswer = 3,
        explanation = "Die Spice Girls kamen aus Grossbritannien und wurden 1994 gegruendet. Mit 'Wannabe' hatten sie 1996 ihren Durchbruch.",
        difficulty = 1,
        funFact = "Die Spice Girls haben mit ueber 85 Millionen verkauften Tontraegern mehr Alben verkauft als jede andere Girlgroup der Geschichte."
    ),

    Question(
        categoryId = 5,
        questionText = "Wie heisst der beruehmt gewordene Song der Boygroup One Direction mit dem Refrain 'Baby you light up my world'?",
        answerA = "Story of My Life",
        answerB = "What Makes You Beautiful",
        answerC = "Best Song Ever",
        answerD = "Live While We're Young",
        correctAnswer = 1,
        explanation = "'What Makes You Beautiful' war der Debueethit von One Direction (2011) und machte die Gruppe international bekannt.",
        difficulty = 1,
        funFact = "One Direction entstand 2010 in der britischen Casting-Show 'The X Factor'. Die fuenf Mitglieder wurden als Solosaenger abgelehnt und dann zu einer Gruppe zusammengestellt."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche Girlgroup hatte den Hit 'Say My Name' (1999)?",
        answerA = "TLC",
        answerB = "Destiny's Child",
        answerC = "En Vogue",
        answerD = "Girls Aloud",
        correctAnswer = 1,
        explanation = "'Say My Name' ist ein weltbekannter Hit von Destiny's Child aus dem Jahr 1999. Die Gruppe war besonders bekannt als fruehes Vehikel von Beyonce.",
        difficulty = 1,
        funFact = "Destiny's Child hatte waehrend ihrer Karriere insgesamt sechs verschiedene Mitglieder, bevor sich die finale Besetzung mit Beyonce, Kelly Rowland und Michelle Williams etablierte."
    ),

    Question(
        categoryId = 5,
        questionText = "Wer war DAS prominenteste Mitglied der Backstreet Boys?",
        answerA = "AJ McLean",
        answerB = "Howie Dorough",
        answerC = "Nick Carter",
        answerD = "Brian Littrell",
        correctAnswer = 2,
        explanation = "Nick Carter gilt als das bekannteste Gesicht der Backstreet Boys und war ein Symbol der 90er-Teenie-Kultur. Die Band hatte Hits wie 'I Want It That Way'.",
        difficulty = 1,
        funFact = "Die Backstreet Boys sind mit ueber 130 Millionen verkauften Alben die weltweit erfolgreichste Boygroup der Geschichte."
    ),

    Question(
        categoryId = 5,
        questionText = "Aus welchem Land stammt die Boygroup BTS?",
        answerA = "Japan",
        answerB = "China",
        answerC = "Suedkorea",
        answerD = "Thailand",
        correctAnswer = 2,
        explanation = "BTS kommt aus Suedkorea und wurde 2013 gegruendet. Die Gruppe ist ein massgeblicher Teil des weltweiten K-Pop-Booms.",
        difficulty = 1,
        funFact = "BTS war 2020 und 2021 die meistgestreamte Gruppe auf Spotify weltweit und hat als erste koreanische Gruppe die Spitze der Billboard Hot 100 erreicht."
    ),

    Question(
        categoryId = 5,
        questionText = "Wie heisst der beruehmt gewordene Sommerhit der Girlgroup Little Mix (2015)?",
        answerA = "Wings",
        answerB = "Black Magic",
        answerC = "Shout Out to My Ex",
        answerD = "Touch",
        correctAnswer = 1,
        explanation = "'Black Magic' (2015) war ein grosser Sommerhit von Little Mix, der in Grossbritannien Platz 1 erreichte und die Gruppe international bekannt machte.",
        difficulty = 1,
        funFact = "Little Mix gewann 2011 als erste Gruppe die britische Castingshow 'The X Factor'. Zuvor hatte noch nie eine Gruppe die Show gewonnen."
    ),

    // ── Duette und Kollaborationen (7 Fragen) ─────────────────────────────────

    Question(
        categoryId = 5,
        questionText = "Wer sang das Duett 'Ebony and Ivory' (1982) mit Paul McCartney?",
        answerA = "Michael Jackson",
        answerB = "Stevie Wonder",
        answerC = "Lionel Richie",
        answerD = "Diana Ross",
        correctAnswer = 1,
        explanation = "'Ebony and Ivory' war ein Duett von Paul McCartney und Stevie Wonder aus dem Jahr 1982 mit einer Botschaft ueber Rassenharmonie.",
        difficulty = 1,
        funFact = "'Ebony and Ivory' war sechs Wochen lang Nummer 1 in Grossbritannien und sieben Wochen in den USA und ist eines der bekanntesten Musikduette der Geschichte."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche zwei Kuenstler haben 'Don't Go Breaking My Heart' (1976) zusammen aufgenommen?",
        answerA = "ABBA und Elton John",
        answerB = "Elton John und Kiki Dee",
        answerC = "Elton John und Olivia Newton-John",
        answerD = "Elton John und Diana Ross",
        correctAnswer = 1,
        explanation = "'Don't Go Breaking My Heart' war ein Duett von Elton John und Kiki Dee aus dem Jahr 1976 und deren beider bekannteste gemeinsame Aufnahme.",
        difficulty = 1,
        funFact = "Das Lied war das erste Mal seit 1969, dass Elton John eine Nummer-1-Single in Grossbritannien hatte, und es war sein einziger Nummer-1-Hit mit einer weiblichen Saengerin."
    ),

    Question(
        categoryId = 5,
        questionText = "Welcher Kuenstler war der Kollaborationspartner von Jay-Z bei 'Crazy in Love' (2003)?",
        answerA = "Rihanna",
        answerB = "Nicki Minaj",
        answerC = "Alicia Keys",
        answerD = "Beyonce",
        correctAnswer = 3,
        explanation = "'Crazy in Love' (2003) ist ein Duett von Beyonce und ihrem spaeter angeheirateten Mann Jay-Z. Es war Beyonces erster Nummer-1-Hit als Solokarriere.",
        difficulty = 1,
        funFact = "'Crazy in Love' enthielt eine Sample aus dem Song 'Are You My Woman (Tell Me So)' von Chi-Lites aus dem Jahr 1971."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche zwei Kuenstler haben 'We Are the World' (1985) fuer Afrika-Hilfe organisiert?",
        answerA = "Elton John und David Bowie",
        answerB = "Michael Jackson und Lionel Richie",
        answerC = "Paul Simon und Art Garfunkel",
        answerD = "Stevie Wonder und Prince",
        correctAnswer = 1,
        explanation = "'We Are the World' wurde 1985 von Michael Jackson und Lionel Richie geschrieben. 45 bekannte Kuenstler nahmen gemeinsam den Song auf, um Geld fuer Hungernde in Afrika zu sammeln.",
        difficulty = 1,
        funFact = "'We Are the World' wurde an einem einzigen Aufnahmetag (28. Januar 1985) aufgenommen. Quincy Jones hatte ausserhalb des Studios ein Schild aufgehaengt: 'Ego bitte vor dem Eintreten draussen lassen'."
    ),

    Question(
        categoryId = 5,
        questionText = "Wer kollaborierte mit Shakira bei 'Beautiful Liar' (2007)?",
        answerA = "Rihanna",
        answerB = "Beyonce",
        answerC = "Ciara",
        answerD = "Jennifer Lopez",
        correctAnswer = 1,
        explanation = "'Beautiful Liar' war eine Kollaboration von Beyonce und Shakira aus dem Jahr 2007 und wurde weltweit ein grosser Charterfolg.",
        difficulty = 1,
        funFact = "Das Musikvideo zu 'Beautiful Liar' zeigte Beyonce und Shakira in fast identischen Kostumen, was den visuellen Effekt erzeugte, als waere man nicht sicher, wer wer ist."
    ),

    Question(
        categoryId = 5,
        questionText = "Welcher Kuenstler arbeitete mit Eminem beim Lied 'Stan' (2000) zusammen?",
        answerA = "Mariah Carey",
        answerB = "Nelly Furtado",
        answerC = "Dido",
        answerD = "Alanis Morissette",
        correctAnswer = 2,
        explanation = "'Stan' (2000) ist ein Lied von Eminem, bei dem Dido singt. Der Song handelt von einem obsessiven Fan und gab dem Begriff 'Stan' fuer einen extremen Fan den Namen.",
        difficulty = 1,
        funFact = "Das Wort 'Stan' – ein Mix aus 'Stalker' und 'Fan' – hat es mittlerweile ins Oxford English Dictionary geschafft, als Bezeichnung fuer einen uebermassig begeisterten Fan."
    ),

    Question(
        categoryId = 5,
        questionText = "Wer sang 'Under Pressure' (1981) gemeinsam mit David Bowie?",
        answerA = "Rod Stewart",
        answerB = "Queen",
        answerC = "Elton John",
        answerD = "Paul McCartney",
        correctAnswer = 1,
        explanation = "'Under Pressure' ist ein Duett von David Bowie und der Band Queen aus dem Jahr 1981. Es entstand spontan waehrend einer gemeinsamen Studiosession.",
        difficulty = 1,
        funFact = "Die beruehmte Bassline von 'Under Pressure' wurde spaeter von Vanilla Ice fast identisch fuer 'Ice Ice Baby' (1990) verwendet, was zu einem Rechtsstreit fuehrte."
    ),

    // ── Musikvideos (5 Fragen) ────────────────────────────────────────────────

    Question(
        categoryId = 5,
        questionText = "Auf welchem Musiksender wurden Musikvideos erstmals rund um die Uhr ausgestrahlt (Gruendung 1981)?",
        answerA = "VH1",
        answerB = "MTV",
        answerC = "VIVA",
        answerD = "BBC Music",
        correctAnswer = 1,
        explanation = "MTV (Music Television) startete am 1. August 1981 in den USA und revolutionierte die Musikindustrie, indem es Musikvideos rund um die Uhr ausstrahlte.",
        difficulty = 1,
        funFact = "Das erste Musikvideo, das auf MTV ausgestrahlt wurde, war 'Video Killed the Radio Star' von The Buggles – ein prophetischer Start fuer den Sender."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Musikvideo zeigt Michael Jackson in einer Zombieapokalypse?",
        answerA = "Beat It",
        answerB = "Thriller",
        answerC = "Smooth Criminal",
        answerD = "Bad",
        correctAnswer = 1,
        explanation = "Das 'Thriller'-Musikvideo (1983) wurde von John Landis inszeniert und zeigt Michael Jackson als Zombie unter Untoten. Es ist das bekannteste Musikvideo aller Zeiten.",
        difficulty = 1,
        funFact = "Das 'Thriller'-Musikvideo ist 14 Minuten lang und kostete 500.000 Dollar – ein Vielfaches des damaligen Durchschnitts fuer ein Musikvideo."
    ),

    Question(
        categoryId = 5,
        questionText = "In welchem Musikvideo singt Rihanna im Regen und tragt eine rote Jacke?",
        answerA = "Diamonds",
        answerB = "We Found Love",
        answerC = "Umbrella",
        answerD = "Rude Boy",
        correctAnswer = 2,
        explanation = "'We Found Love' von Rihanna feat. Calvin Harris (2011) zeigt Rihanna in verschiedenen emotionalen Szenen und war einer der groessten Hits des Jahres.",
        difficulty = 1,
        funFact = "'Umbrella' war der Beginn von Rihannas Dominanz der Charts. Das Wort 'Umbrella' (Regenschirm) wurde das Jahr 2007 praegend nach dem Megaerfolg des Songs."
    ),

    Question(
        categoryId = 5,
        questionText = "Wer tanzte im Musikvideo 'Gangnam Style' (2012) den beruehmten Reitbewegungstanz?",
        answerA = "Rain",
        answerB = "G-Dragon",
        answerC = "PSY",
        answerD = "BigBang",
        correctAnswer = 2,
        explanation = "'Gangnam Style' von PSY (2012) war das erste YouTube-Video, das 1 Milliarde Aufrufe erreichte. Der Reitbewegungstanz wurde weltweit nachgeahmt.",
        difficulty = 1,
        funFact = "'Gangnam Style' war so populaer, dass Youtubes Aufrufzaehler auf das Limit von 2,1 Milliarden stiess und die Programmierer den Zaehler erweitern mussten."
    ),

    Question(
        categoryId = 5,
        questionText = "Welcher Kuenstler tanzte in seinem Musikvideo auf einem Pferd und laesst Leute in die Luft springen?",
        answerA = "Jay-Z",
        answerB = "Kanye West",
        answerC = "Drake",
        answerD = "Pharrell Williams",
        correctAnswer = 1,
        explanation = "Pharrell Williams tanzte im 'Happy'-Musikvideo (2013) auf den Strassen von Hollywood und brachte alle zum Tanzen. Das 24-Stunden-Video war eine Weltpremiere.",
        difficulty = 1,
        funFact = "Das 'Happy'-Musikvideo war das erste offizielle interaktive 24-Stunden-Musikvideo der Geschichte, bei dem alle 5 Minuten eine neue Szene mit neuen Gaesten zu sehen war."
    ),

)
