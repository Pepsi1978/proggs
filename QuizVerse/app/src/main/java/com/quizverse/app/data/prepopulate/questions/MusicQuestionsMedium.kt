package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun musicQuestionsMedium(): List<Question> = listOf(

    // ── Grammy Awards und Musikpreise (8 Fragen) ──────────────────────────────

    Question(
        categoryId = 5,
        questionText = "In welchem Jahr wurden die Grammy Awards erstmals verliehen?",
        answerA = "1952",
        answerB = "1959",
        answerC = "1965",
        answerD = "1971",
        correctAnswer = 1,
        explanation = "Die erste Grammy-Verleihung fand am 4. Mai 1959 im Beverly Hilton Hotel in Beverly Hills statt. Der Name 'Grammy' leitet sich vom Grammophon ab.",
        difficulty = 2,
        funFact = "Der erste Grammy fuer Album des Jahres ging 1959 an Henry Mancini fuer das Soundtrack-Album 'The Music from Peter Gunn'."
    ),

    Question(
        categoryId = 5,
        questionText = "Welcher Kuenstler hat die meisten Grammy Awards aller Zeiten gewonnen?",
        answerA = "Beyonce",
        answerB = "Georg Solti",
        answerC = "Quincy Jones",
        answerD = "Paul McCartney",
        correctAnswer = 0,
        explanation = "Beyonce haelt den Rekord mit ueber 30 Grammy Awards und ist damit die Kuenstlerin mit den meisten Grammys in der Geschichte der Veranstaltung.",
        difficulty = 2,
        funFact = "Dirigent Georg Solti haelt mit 31 Grammys den Gesamtrekord, wenn man klassische Musik einschliesslich Solti einbezieht. Im Pop-Bereich fuehrt Beyonce."
    ),

    Question(
        categoryId = 5,
        questionText = "Welcher Preis wird als das europaeische Aequivalent der Grammys bezeichnet?",
        answerA = "Brit Award",
        answerB = "Echo Award",
        answerC = "MTV Europe Music Award",
        answerD = "European Music Award",
        correctAnswer = 0,
        explanation = "Die Brit Awards werden seit 1977 in Grossbritannien verliehen und gelten als wichtigste Musikpreise Europas. Sie werden von der British Phonographic Industry organisiert.",
        difficulty = 2,
        funFact = "Der erste Brit Award fuer den besten britischen Kuenstler ging 1977 an David Bowie."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche Saengerin gewann 2012 den Grammy fuer Record of the Year mit dem Lied 'Rolling in the Deep'?",
        answerA = "Amy Winehouse",
        answerB = "Adele",
        answerC = "Rihanna",
        answerD = "Beyonce",
        correctAnswer = 1,
        explanation = "Adele gewann 2012 gleich sechs Grammys auf einmal, darunter Record of the Year fuer 'Rolling in the Deep' und Album des Jahres fuer '21'.",
        difficulty = 2,
        funFact = "Adeles Auftritt bei der 54. Grammy-Verleihung 2012 wurde von ueber 40 Millionen Zuschauern in den USA verfolgt."
    ),

    Question(
        categoryId = 5,
        questionText = "Der Musikpreis 'Echo' wurde in Deutschland verliehen. In welchem Jahr wurde er eingestellt?",
        answerA = "2015",
        answerB = "2017",
        answerC = "2018",
        answerD = "2020",
        correctAnswer = 2,
        explanation = "Der Echo-Musikpreis wurde 2018 eingestellt, nachdem die Vergabe eines Preises an die Rapper Kollegah und Farid Bang trotz antisemitischer Texte einen Skandal ausloeste.",
        difficulty = 2,
        funFact = "Als Nachfolger des Echo wurde 2020 der MFVA-Musikpreis eingefuehrt, der spaeter in 'Preis fuer Populaere Musik' umbenannt wurde."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Album von Michael Jackson ist das meistverkaufte Album aller Zeiten und wurde mit mehreren Grammys ausgezeichnet?",
        answerA = "Bad",
        answerB = "Off the Wall",
        answerC = "Dangerous",
        answerD = "Thriller",
        correctAnswer = 3,
        explanation = "'Thriller' (1982) ist das meistverkaufte Album der Musikgeschichte mit ueber 70 Millionen verkauften Exemplaren. Bei den Grammys 1984 gewann Michael Jackson damit acht Preise in einer Nacht.",
        difficulty = 2,
        funFact = "Das Musikvideo zu 'Thriller' dauert 14 Minuten und gilt als das einflussreichste Musikvideo aller Zeiten. Es kostete damals 500.000 Dollar."
    ),

    Question(
        categoryId = 5,
        questionText = "Bei welchem Musikpreis wird die 'Goldene Stimmgabel' verliehen?",
        answerA = "Bambi",
        answerB = "Goldene Kamera",
        answerC = "1LIVE Krone",
        answerD = "Schlager-Champion",
        correctAnswer = 3,
        explanation = "Die 'Goldene Stimmgabel' ist der bekannteste Preis beim ZDF-Format 'Schlager-Champion', der an Schlagerstars wie Helene Fischer oder Andreas Gabalier verliehen wird.",
        difficulty = 2,
        funFact = "Helene Fischer ist eine der am haeufigsten ausgezeichneten Schlagerkuenstlerinnen und hat die Goldene Stimmgabel mehrfach erhalten."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche Musikgruppe erhielt als erste Beatles-Mitglieder nach der Trennung einen Grammy fuer das 'Lifetime Achievement Award'?",
        answerA = "Nur Paul McCartney solo",
        answerB = "Alle vier einzeln",
        answerC = "Die Beatles als Gruppe",
        answerD = "John Lennon posthum als Erster",
        correctAnswer = 2,
        explanation = "Die Beatles erhielten 1965 ihren ersten Grammy und 2014 einen Grammy Lifetime Achievement Award als Gruppe, obwohl sie sich bereits 1970 getrennt hatten.",
        difficulty = 2,
        funFact = "Die Beatles haben insgesamt sieben Grammy Awards gewonnen, obwohl sie in ihren aktiven Jahren oft leer ausgingen, da die Grammys damals Rock- und Popmusik gegenueber eher skeptisch waren."
    ),

    // ── Album-Geschichte und Rekorde (8 Fragen) ───────────────────────────────

    Question(
        categoryId = 5,
        questionText = "Welches Album der Rolling Stones gilt als ihr kuenstlerischer Hoehepunkt und erschien 1972?",
        answerA = "Sticky Fingers",
        answerB = "Exile on Main St.",
        answerC = "Let It Bleed",
        answerD = "Some Girls",
        correctAnswer = 1,
        explanation = "'Exile on Main St.' (1972) ist ein Doppelalbum, das in einer gemieteten Villa in Suedfrankreich aufgenommen wurde und heute als eines der besten Rockalbum aller Zeiten gilt.",
        difficulty = 2,
        funFact = "Das Album wurde in Keith Richards Keller in der Villa Nellcote aufgenommen. Die Produktionsbedingungen waren chaotisch, aber das Ergebnis wurde als Meisterwerk gefeiert."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Album der Beatles wurde zuletzt veroeffentlicht und erschien 1970?",
        answerA = "Abbey Road",
        answerB = "Let It Be",
        answerC = "Yellow Submarine",
        answerD = "The White Album",
        correctAnswer = 1,
        explanation = "'Let It Be' erschien im Mai 1970 und war das letzte veroeffentlichte Album der Beatles, obwohl 'Abbey Road' danach aufgenommen wurde. Es dokumentierte die Spannungen in der Band.",
        difficulty = 2,
        funFact = "Der Produzent Phil Spector ueberarbeitete das Album nach der Trennung der Beatles, was Paul McCartney spaeter dazu veranlasste, eine 'naked' Version ohne Overdubs herauszubringen."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Nirvana-Album enthielt den Hit 'Smells Like Teen Spirit' und brach 1991 alle Verkaufsrekorde im Grunge-Genre?",
        answerA = "Bleach",
        answerB = "In Utero",
        answerC = "Nevermind",
        answerD = "Unplugged in New York",
        correctAnswer = 2,
        explanation = "'Nevermind' (1991) verdraengte Michael Jacksons 'Dangerous' von der Spitze der US-Charts und verkaufte sich weltweit ueber 30 Millionen Mal. Es gilt als Grundstein des Grunge.",
        difficulty = 2,
        funFact = "Das Coverfoto zeigt einen nackten Baby-Jungen, der einem Geldschein nachschwimmt. Das Baby auf dem Cover ist Spencer Elden, der spaeter versuchte, die Band zu verklagen."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche Band veroeffentlichte 1973 das Konzeptalbum 'The Dark Side of the Moon', das 741 Wochen in den UK-Charts verbrachte?",
        answerA = "Led Zeppelin",
        answerB = "Pink Floyd",
        answerC = "Yes",
        answerD = "Genesis",
        correctAnswer = 1,
        explanation = "'The Dark Side of the Moon' von Pink Floyd (1973) verbrachte ueber 14 Jahre in den Billboard-Charts und ist eines der meistverkauften Alben aller Zeiten mit ca. 45 Millionen Exemplaren.",
        difficulty = 2,
        funFact = "Das ikonische Prisma-Cover wurde vom Grafikdesign-Studio Hipgnosis entworfen. Die Idee kam von Keyboarder Richard Wright, der etwas Minimalistisches und Elegantes wollte."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Album von Amy Winehouse gewann 2008 fuenf Grammy Awards und ist eines der meistverkauften britischen Alben?",
        answerA = "Frank",
        answerB = "Lioness: Hidden Treasures",
        answerC = "Back to Black",
        answerD = "Amy",
        correctAnswer = 2,
        explanation = "'Back to Black' (2006) gewann 2008 fuenf Grammys und machte Amy Winehouse zur ersten britischen Kuenstlerin, die in einer Nacht fuenf Grammys gewann.",
        difficulty = 2,
        funFact = "Amy Winehouse war nach einem Vertrag mit 'Back to Black' so beruhmt, dass sie nicht mehr das Haus verlassen konnte. Der Druck und die Sucht fuehrten zu ihrem fruehen Tod 2011."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Adele-Album erschien 2021 und setzt sich aus dem Namen des Jahres zusammen, in dem sie schrieb?",
        answerA = "19",
        answerB = "25",
        answerC = "21",
        answerD = "30",
        correctAnswer = 3,
        explanation = "Adeles Album '30' erschien 2021 und ist benannt nach ihrem Alter waehrend der Schreibphase. Es thematisiert ihre Scheidung und emotionalen Selbstreflexionsprozess.",
        difficulty = 2,
        funFact = "Adele benennt alle ihre Alben nach ihrem Lebensalter: '19' (2008), '21' (2011), '25' (2015) und '30' (2021). Jedes Album spiegelt eine bestimmte Lebensphase wider."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Doppelalbum von Prince aus dem Jahr 1999 enthielt den gleichnamigen Hit und machte ihn weltberuehmt?",
        answerA = "Purple Rain",
        answerB = "Sign 'O' the Times",
        answerC = "Around the World in a Day",
        answerD = "1999",
        correctAnswer = 3,
        explanation = "'1999' (1982) war das Doppelalbum, das Prince zum Superstar machte. Der Titelsong und 'Little Red Corvette' wurden weltweite Hits und ebneten den Weg fuer 'Purple Rain'.",
        difficulty = 2,
        funFact = "Prince selbst spielte fast alle Instrumente auf dem Album ein. Er war bekannt dafuer, an einem einzigen Tag mehrere komplette Songs aufnehmen zu koennen."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Genre-definierende Album von Jay-Z und Kanye West erschien 2011 und hiess 'Watch the Throne'?",
        answerA = "Kollaborationsalbum zweier Stars aus New York",
        answerB = "Erstes gemeinsames Studio-Doppelalbum",
        answerC = "Livealbum aus einer gemeinsamen Tour",
        answerD = "Soundtrack zum gleichnamigen Film",
        correctAnswer = 1,
        explanation = "'Watch the Throne' (2011) war das erste kollaborative Studioalbum von Jay-Z und Kanye West. Es wurde als digitaler Download veroeffentlicht, bevor die physische Version folgte.",
        difficulty = 2,
        funFact = "Das Album entstand grosstenteils in verschiedenen Luxushotels auf der ganzen Welt. Der Track 'Otis' sampelt den Otis Redding-Song 'Try a Little Tenderness'."
    ),

    // ── Musikproduzenten und Studios (7 Fragen) ───────────────────────────────

    Question(
        categoryId = 5,
        questionText = "Welcher Produzent ist als 'Fifth Beatle' bekannt und produzierte fast alle Beatles-Alben?",
        answerA = "Phil Spector",
        answerB = "George Martin",
        answerC = "Brian Eno",
        answerD = "Quincy Jones",
        correctAnswer = 1,
        explanation = "George Martin produzierte von 1962 bis 1970 fast alle Alben der Beatles und gilt als wichtigster Einfluss auf ihren Sound. Er starb 2016 im Alter von 90 Jahren.",
        difficulty = 2,
        funFact = "George Martin war eigentlich klassisch ausgebildeter Musiker und Produzent von Comedy-Platten, bevor er die Beatles unter Vertrag nahm. EMI legte ihm den Deal nahe, den andere Plattenvertraege abgelehnt hatten."
    ),

    Question(
        categoryId = 5,
        questionText = "Welcher Produzent ist bekannt fuer die Zusammenarbeit mit Michael Jackson bei 'Thriller', 'Off the Wall' und 'Bad'?",
        answerA = "Nile Rodgers",
        answerB = "Rick Rubin",
        answerC = "Quincy Jones",
        answerD = "David Foster",
        correctAnswer = 2,
        explanation = "Quincy Jones produzierte die drei erfolgreichsten Michael-Jackson-Alben: 'Off the Wall' (1979), 'Thriller' (1982) und 'Bad' (1987). Er gilt als einer der einflussreichsten Musikproduzenten der Geschichte.",
        difficulty = 2,
        funFact = "Quincy Jones begann seine Karriere als Jazz-Trompeter und Arrangeur. Er arbeitete mit Nahezu jedem grossen Musikstar des 20. Jahrhunderts zusammen, von Frank Sinatra bis Donna Summer."
    ),

    Question(
        categoryId = 5,
        questionText = "In welchem legendaeren Studio in Memphis wurden Hits von Aretha Franklin, Al Green und vielen Soul-Kuenstlern produziert?",
        answerA = "Sun Studio",
        answerB = "Chess Records Studio",
        answerC = "Hitsville U.S.A.",
        answerD = "Ardent Studios / Stax Records",
        correctAnswer = 3,
        explanation = "Stax Records und die Ardent Studios in Memphis waren das Zentrum des Southern Soul in den 1960er und 70er Jahren. Kuenstler wie Otis Redding, Isaac Hayes und Booker T. & the M.G.'s nahmen dort auf.",
        difficulty = 2,
        funFact = "Stax Records befand sich in einem ehemaligen Kino. Die schiefe Buehnenkonstruktion des Studios wurde nie ausgeglichen, was dem Raumklang einen einzigartigen Charakter verlieh."
    ),

    Question(
        categoryId = 5,
        questionText = "Welcher Produzent formte den Klang von 'Wall of Sound' und arbeitete unter anderem mit den Ronettes und den Beatles?",
        answerA = "Berry Gordy",
        answerB = "Phil Spector",
        answerC = "Ahmet Ertegun",
        answerD = "Clive Davis",
        correctAnswer = 1,
        explanation = "Phil Spectors 'Wall of Sound' Technik schichtete viele Instrumente uebereinander, um einen massiven, orchestralen Klang zu erzeugen. Er produzierte 'Be My Baby' (Ronettes) und 'Let It Be' (Beatles).",
        difficulty = 2,
        funFact = "Phil Spector wurde 2009 wegen Mordes verurteilt und starb 2021 im Gefaengnis. Sein musikalisches Erbe ist trotz seiner kriminellen Handlungen unbestritten."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches legendaere Studio in London nahm die meisten Beatles-Alben auf und ist bis heute in Betrieb?",
        answerA = "Olympic Studios",
        answerB = "Trident Studios",
        answerC = "Abbey Road Studios",
        answerD = "RAK Studios",
        correctAnswer = 2,
        explanation = "Die Abbey Road Studios in London existieren seit 1931. Die Beatles nahmen fast alle ihre Alben dort auf. Das beruehmt gewordene Coverbild des Albums 'Abbey Road' zeigt die vier Beatles auf dem Fussgaengerueberweg davor.",
        difficulty = 2,
        funFact = "Der Fussgaengerueberweg vor den Abbey Road Studios ist eine offizielle touristische Sehenswuerdigkeit und wird taeglich von Hunderten Touristen besucht und fotografiert."
    ),

    Question(
        categoryId = 5,
        questionText = "Welcher Produzent gilt als Mitbegruender von Motown Records und formte den Sound von Kuenstlern wie Stevie Wonder und Diana Ross?",
        answerA = "Quincy Jones",
        answerB = "Berry Gordy",
        answerC = "Norman Whitfield",
        answerD = "Smokey Robinson",
        correctAnswer = 1,
        explanation = "Berry Gordy gruendete 1959 Motown Records in Detroit. Er entwickelte den charakteristischen 'Motown Sound' - tanzbare Soul-Musik mit Pop-Appell, der die Rassenschranken in der US-Musikindustrie durchbrach.",
        difficulty = 2,
        funFact = "Motown Records bekam seinen Spitznamen 'Hitsville U.S.A.' wegen der erstaunlichen Erfolgsquote seiner Veroeffentlichungen. In den 1960ern landeten 75% der Motown-Singles in den Top 10 der Charts."
    ),

    Question(
        categoryId = 5,
        questionText = "Welcher Produzent ist bekannt fuer die Zusammenarbeit mit Eminem und praegt den Sound des Detroit Hip-Hop?",
        answerA = "Dr. Dre",
        answerB = "DJ Premier",
        answerC = "Timbaland",
        answerD = "Swizz Beatz",
        correctAnswer = 0,
        explanation = "Dr. Dre entdeckte Eminem 1998 und produzierte seine erfolgreichen Alben 'The Slim Shady LP', 'The Marshall Mathers LP' und 'The Eminem Show'. Er ist selbst ein Grammy-praemierter Produzent und Rapper.",
        difficulty = 2,
        funFact = "Dr. Dres eigenes Album 'Detox' wurde mehr als ein Jahrzehnt lang angekuendigt und nie veroeffentlicht. Es gilt als eines der beruehmt-beruechtigtsten nicht erschienenen Alben der Musikgeschichte."
    ),

    // ── Musiktheorie-Grundlagen (7 Fragen) ────────────────────────────────────

    Question(
        categoryId = 5,
        questionText = "Welche Intervallbezeichnung beschreibt den Abstand von 7 Halbtönen (z.B. C zu G)?",
        answerA = "Grosse Sexte",
        answerB = "Kleine Septime",
        answerC = "Reine Quinte",
        answerD = "Grosse Terz",
        correctAnswer = 2,
        explanation = "Die reine Quinte umfasst 7 Halbtöne und ist nach der Oktave das konsonanteste Intervall. Das beruhmteste Beispiel ist die Quinte C-G, die in unzaehligen Musikkulturen als harmonisch empfunden wird.",
        difficulty = 2,
        funFact = "Der Quintenzirkel ordnet alle 12 Tonarten so an, dass benachbarte Tonarten jeweils eine Quinte auseinanderliegen. Er ist eines der wichtigsten Werkzeuge der westlichen Musiktheorie."
    ),

    Question(
        categoryId = 5,
        questionText = "Was bezeichnet man als 'Synkope' in der Musik?",
        answerA = "Ein besonders langes Musikstueck",
        answerB = "Eine Betonung auf einem normalerweise unbetonten Taktteil",
        answerC = "Das gleichzeitige Erklingen von drei Toenen",
        answerD = "Eine stufenweise Melodiebewegung",
        correctAnswer = 1,
        explanation = "Eine Synkope entsteht, wenn die Betonung auf einen 'schwachen' Taktteil faellt, der normalerweise unbetont ist. Sie erzeugt einen rhythmischen Spannungseffekt und ist besonders in Jazz, Funk und Reggae typisch.",
        difficulty = 2,
        funFact = "Die Synkope ist ein zentrales Element des Swing-Rhythmus im Jazz. Ohne Synkopen klaenge Jazz wie Marschmusik."
    ),

    Question(
        categoryId = 5,
        questionText = "Welcher Akkord besteht aus Grundton, grosser Terz und reiner Quinte?",
        answerA = "Moll-Dreiklang",
        answerB = "Dur-Dreiklang",
        answerC = "Verminderter Dreiklang",
        answerD = "Uebermassiger Dreiklang",
        correctAnswer = 1,
        explanation = "Ein Dur-Dreiklang besteht aus Grundton, grosser Terz (4 Halbtöne) und reiner Quinte (7 Halbtöne). Er klingt 'hell' und 'froelich' im Gegensatz zum Moll-Dreiklang.",
        difficulty = 2,
        funFact = "Die Bezeichnung 'Dur' kommt vom lateinischen 'durus' (hart). 'Moll' kommt vom lateinischen 'mollis' (weich). Diese Terminologie etablierte sich im 17. Jahrhundert in Europa."
    ),

    Question(
        categoryId = 5,
        questionText = "Was bedeutet das Tempo-Zeichen 'Andante' in der Musik?",
        answerA = "Sehr schnell und lebhaft",
        answerB = "Sehr langsam und feierlich",
        answerC = "Gehend, maessig bewegt",
        answerD = "Schnell, aber beherrschend",
        correctAnswer = 2,
        explanation = "'Andante' kommt vom italienischen 'andare' (gehen) und bezeichnet ein maessig langsames Tempo von ca. 76-108 Schlae pro Minute. Es ist schneller als Adagio, aber langsamer als Moderato.",
        difficulty = 2,
        funFact = "Beethovens zweiter Satz seiner 7. Sinfonie ('Allegretto') wurde von Zeitgenossen oft faelschlicherweise als 'Andante' bezeichnet und galt als das langsamste und tiefgruendigste Stueck, das er je schrieb."
    ),

    Question(
        categoryId = 5,
        questionText = "Wie viele Kreuze hat die Tonart E-Dur?",
        answerA = "2",
        answerB = "3",
        answerC = "4",
        answerD = "5",
        correctAnswer = 2,
        explanation = "E-Dur hat vier Kreuze: Fis, Cis, Gis und Dis. Eine einfache Merkregel: Jede neue Kreuz-Tonart fuegt ein Kreuz hinzu, beginnend mit G-Dur (1 Kreuz) bis Cis-Dur (7 Kreuze).",
        difficulty = 2,
        funFact = "E-Dur ist die bevorzugte Tonart vieler Gitarristen, da offene Saiten der Gitarre (E, A, D, G, H, e) gut zu E-Dur-Akkorden passen und einen voluminoesen Klang erzeugen."
    ),

    Question(
        categoryId = 5,
        questionText = "Was ist ein 'Leitmotiv' in der Musik, besonders in der Oper?",
        answerA = "Die Hauptmelodie eines Volksliedes",
        answerB = "Ein kurzes, wiederkehrendes musikalisches Thema, das eine Person oder Idee repraesentiert",
        answerC = "Ein Soloinstrument, das die Hauptmelodie traegt",
        answerD = "Die Grundtonart eines Werkes",
        correctAnswer = 1,
        explanation = "Das Leitmotiv ist ein kurzes musikalisches Motiv, das einer Figur, einem Ort oder einer Idee zugeordnet ist und im Werk wiederholt auftaucht. Richard Wagner machte es zum zentralen Kompositionsprinzip.",
        difficulty = 2,
        funFact = "John Williams nutzte Leitmotive meisterhaft in Filmmusiken wie 'Star Wars'. Das Imperial March (Darth Vader-Thema) oder das Luke-Thema sind klassische Filmleitmotive."
    ),

    Question(
        categoryId = 5,
        questionText = "Was bezeichnet die Abkuerzung 'BPM' in der Musik?",
        answerA = "Bass per Measure",
        answerB = "Beats per Minute",
        answerC = "Bars per Movement",
        answerD = "Beat Pattern Method",
        correctAnswer = 1,
        explanation = "BPM steht fuer 'Beats per Minute' und bezeichnet die Geschwindigkeit eines Musikstueckes. 60 BPM entspricht einem Schlag pro Sekunde. Normaler Walzer liegt bei ca. 100 BPM, Techno oft bei 130-160 BPM.",
        difficulty = 2,
        funFact = "Forscher haben herausgefunden, dass Musik mit 120-130 BPM am besten fuer Workout-Musik geeignet ist, da sie den nattuerlichen Herzrhythmus waehrend moderater Bewegung widerspiegelt."
    ),

    // ── Musikgeschichte 20. Jahrhundert (10 Fragen) ───────────────────────────

    Question(
        categoryId = 5,
        questionText = "Welche Musikbewegung entstand in den 1950ern in den USA und verband Country mit Rhythm & Blues?",
        answerA = "Bebop",
        answerB = "Rock 'n' Roll",
        answerC = "Disco",
        answerD = "Funk",
        correctAnswer = 1,
        explanation = "Rock 'n' Roll entstand in den fruehen 1950er Jahren als Fusion von Country-Musik und Rhythm & Blues. Pioniere waren Chuck Berry, Little Richard und Elvis Presley, der den Stil fuer ein weisses Publikum zugaenglich machte.",
        difficulty = 2,
        funFact = "Der Ausdruck 'Rock and Roll' war urspruenglich ein Slang-Begriff aus der afroamerikanischen Gemeinschaft. DJ Alan Freed popularisierte den Begriff im Radio, um die neue Musikrichtung zu beschreiben."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Musikereignis 1969 gilt als Hoehepunkt der Hippie-Bewegung und Gegenkultur?",
        answerA = "Monterey Pop Festival 1967",
        answerB = "Isle of Wight Festival 1970",
        answerC = "Woodstock Festival",
        answerD = "Live Aid 1985",
        correctAnswer = 2,
        explanation = "Das Woodstock-Festival vom 15. bis 18. August 1969 in New York State gilt als das Symbol der 1960er Counterculture. Ueber 400.000 Menschen besuchten Auftritte von Jimi Hendrix, Janis Joplin, The Who und vielen anderen.",
        difficulty = 2,
        funFact = "Woodstock wurde urspruenglich als kommerzielles Veranstaltung geplant, wurde aber de facto kostenlos, nachdem die Zaune eingerissen wurden. Die Veranstalter verloren Geld, aber der Dokumentarfilm von 1970 brachte alles wieder rein."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche Plattenfirma, gegrndet 1955 in Detroit, formte den 'Motown Sound' und veroeffentlichte Hits von Stevie Wonder?",
        answerA = "Atlantic Records",
        answerB = "Chess Records",
        answerC = "Stax Records",
        answerD = "Motown Records",
        correctAnswer = 3,
        explanation = "Motown Records wurde 1959 (nicht 1955) von Berry Gordy in Detroit gegrndet und formte den sogenannten 'Motown Sound' - eine Mischung aus Soul, Pop und R&B. Kuenstler wie Stevie Wonder, Diana Ross und Marvin Gaye praegten das Label.",
        difficulty = 2,
        funFact = "Motown war eines der ersten grossen US-Labels im Besitz eines Afroamerikaners. Berry Gordy startete mit einem Darlehen von 800 Dollar von seiner Familie."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche britische Invasion praegt die Musikgeschichte der 1960er Jahre?",
        answerA = "Das Aufkommen britischer Folk-Musiker in den USA",
        answerB = "Die Dominanz britischer Rockbands wie Beatles und Rolling Stones in US-Charts",
        answerC = "Die Uebernahme des US-Radiosenders durch britische Moderatoren",
        answerD = "Der Import britischer Instrumente nach Amerika",
        correctAnswer = 1,
        explanation = "Die 'British Invasion' begann 1964 mit dem US-Auftritt der Beatles in der Ed Sullivan Show. Britische Bands wie die Rolling Stones, The Who und The Kinks dominierten daraufhin die US-Charts.",
        difficulty = 2,
        funFact = "Die Beatles wurden 1964 von ueber 73 Millionen Amerikanern in der Ed Sullivan Show gesehen - zu dieser Zeit ein TV-Rekord. Danach wollten alle Jungen Gitarre spielen lernen."
    ),

    Question(
        categoryId = 5,
        questionText = "Welcher Musikstil entstand in den 1970ern in New York und kombinierte rhythmisches Sprechen mit Schallplatten-Scratching?",
        answerA = "Funk",
        answerB = "Disco",
        answerC = "Hip-Hop",
        answerD = "Punk",
        correctAnswer = 2,
        explanation = "Hip-Hop entstand in der Bronx, New York, in den fruehen 1970er Jahren, als DJ Kool Herc begann, Schallplatten bei Partys zu scratchen und den Break einer Schallplatte zu verlaengern. Rapper sprachen rhythmisch ueber die Beats.",
        difficulty = 2,
        funFact = "Als offizielles Geburtsdatum des Hip-Hop gilt der 11. August 1973, als DJ Kool Herc eine Party in der Sedgwick Avenue in der Bronx veranstaltete, bei der er das 'Merry-Go-Round'-Scratching erfand."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche Musikbewegung entstand in den 1970ern in Grossbritannien als Reaktion auf die aufgeblasene Rockmusik der Zeit?",
        answerA = "New Wave",
        answerB = "Punk Rock",
        answerC = "Post-Punk",
        answerD = "Glam Rock",
        correctAnswer = 1,
        explanation = "Punk Rock entstand Mitte der 1970er in Grossbritannien und den USA als radikale Reaktion auf den als ueberproduziert empfundenen Mainstream-Rock. Bands wie die Sex Pistols und The Clash pradigen Schnelligkeit, Rohheit und Anti-Establishment-Haltung.",
        difficulty = 2,
        funFact = "Das Single 'God Save the Queen' der Sex Pistols erschien 1977 waehrend des Silberjubilaums von Koenigin Elizabeth II. und wurde trotz (oder wegen) BBC-Verbots ein grosser Hit."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Jahrzehnt gilt als die Blaetezeit der Disco-Musik, mit Kuenstlern wie Donna Summer und der Bee Gees?",
        answerA = "1960er",
        answerB = "1970er",
        answerC = "1980er",
        answerD = "1990er",
        correctAnswer = 1,
        explanation = "Disco-Musik erreichte ihren Hoehepunkt in den spaten 1970er Jahren. Das Soundtrackalbum des Films 'Saturday Night Fever' (1977) mit den Bee Gees ist eines der meistverkauften Alben aller Zeiten.",
        difficulty = 2,
        funFact = "Die Anti-Disco-Bewegung 'Disco Demolition Night' fand am 12. Juli 1979 im Comiskey Park in Chicago statt. Fans zerstoerten Disco-Schallplatten auf dem Baseballfeld, was eine Massenschlaegerei ausloeste."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche deutsche Band gilt als Begruender der elektronischen Musik und veroeffentlichte 1974 das Album 'Autobahn'?",
        answerA = "Tangerine Dream",
        answerB = "Can",
        answerC = "Kraftwerk",
        answerD = "Neu!",
        correctAnswer = 2,
        explanation = "Kraftwerk aus Duesseldorf gilt als Pionier der elektronischen Musik. Ihr Album 'Autobahn' (1974) mit dem 22-minuetigen Titeltrack wurde weltweit zum Hit und beeinflusste Generationen von Musikern.",
        difficulty = 2,
        funFact = "Kraftwerk performten oft mit Roboter-Puppen, die sie auf der Buehne vertraten. Die Band war so scheu, dass sie Interviews hassten und Fotos von sich selten veroeffentlichten."
    ),

    Question(
        categoryId = 5,
        questionText = "Welcher Kuenstler gilt als 'Godfather of Soul' und war ein Pionier des Funk und R&B im 20. Jahrhundert?",
        answerA = "Ray Charles",
        answerB = "Sam Cooke",
        answerC = "Marvin Gaye",
        answerD = "James Brown",
        correctAnswer = 3,
        explanation = "James Brown erhielt den Titel 'Godfather of Soul' durch seinen ausserordentlichen Einfluss auf Funk, Soul und R&B. Hits wie 'I Got You (I Feel Good)' und seine energetischen Buehnenshows machten ihn legendaer.",
        difficulty = 2,
        funFact = "James Brown war so beliebt, dass sein Tod am 25. Dezember 2006 als nationale Trauer empfunden wurde. Sein Sarg wurde durch mehrere Staedte transportiert, um Fans Abschied nehmen zu lassen."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches historische Konzert 1985 war ein weltweites Live-Aid-Benefizkonzert und wurde simultan in London und Philadelphia uebertragen?",
        answerA = "Konzert fuer Bangla Desh 1971",
        answerB = "Live Aid 1985",
        answerC = "Farm Aid 1985",
        answerD = "Amnesty International Tour 1986",
        correctAnswer = 1,
        explanation = "Live Aid fand am 13. Juli 1985 statt und wurde von Bob Geldof und Midge Ure organisiert, um Hungersnothilfe fuer Aethiopien zu sammeln. Ueber 1,5 Milliarden Menschen sahen es weltweit im Fernsehen.",
        difficulty = 2,
        funFact = "Queens Auftritt bei Live Aid mit Freddie Mercury gilt als der beste Liveauftritt der Rockgeschichte. Die Band spielte nur 20 Minuten, aber Mercury elektrisierte das Publikum von 72.000 Menschen derart, dass andere Bands Angst hatten, danach aufzutreten."
    ),

    // ── Bands und ihre Mitglieder (10 Fragen) ─────────────────────────────────

    Question(
        categoryId = 5,
        questionText = "Wer war der Leadsaenger der britischen Band Queen, der 1991 verstarb?",
        answerA = "David Bowie",
        answerB = "Mick Jagger",
        answerC = "Freddie Mercury",
        answerD = "Roger Daltrey",
        correctAnswer = 2,
        explanation = "Freddie Mercury (born Farrokh Bulsara) war der charismatische Frontsaenger von Queen. Er starb am 24. November 1991 an den Folgen einer AIDS-Erkrankung, nachdem er seine Krankheit erst einen Tag zuvor oeffentlich gemacht hatte.",
        difficulty = 2,
        funFact = "Freddie Mercury hatte einen aussergewoehnlichen Stimmumfang von fast vier Oktaven. Musiker und Stimmforscher haben seine Stimme als einzigartig in ihrer Vielfalt und Kraft beschrieben."
    ),

    Question(
        categoryId = 5,
        questionText = "Wie hiess der urspruengliche Gitarrist der Rolling Stones, der 1969 in einem Swimmingpool ertrank?",
        answerA = "Ron Wood",
        answerB = "Mick Taylor",
        answerC = "Bill Wyman",
        answerD = "Brian Jones",
        correctAnswer = 3,
        explanation = "Brian Jones war ein Gruendungsmitglied der Rolling Stones und gab der Band ihren Namen. Er wurde 1969 aus der Band entlassen und ertrank kurz darauf in seinem Swimmingpool. Er war erst 27 Jahre alt.",
        difficulty = 2,
        funFact = "Brian Jones wurde Mitglied des so genannten '27 Club' - einer Gruppe von Musikern, die im Alter von 27 Jahren starben, darunter auch Jimi Hendrix, Janis Joplin, Jim Morrison, Kurt Cobain und Amy Winehouse."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche vier Mitglieder bildeten die Originalbesetzung der Band Led Zeppelin?",
        answerA = "Jimmy Page, Robert Plant, John Paul Jones, John Bonham",
        answerB = "Jimmy Page, Roger Plant, John Jones, John Bonham",
        answerC = "Jimmy Page, Robert Plant, Paul Jones, Keith Moon",
        answerD = "Eric Clapton, Robert Plant, John Paul Jones, John Bonham",
        correctAnswer = 0,
        explanation = "Led Zeppelin bestand aus Jimmy Page (Gitarre), Robert Plant (Gesang), John Paul Jones (Bass/Keyboards) und John Bonham (Schlagzeug). Sie gruendeten die Band 1968 und loesten sich nach Bonhams Tod 1980 auf.",
        difficulty = 2,
        funFact = "Led Zeppelin lehnten es prinzipiell ab, Singles zu veroeffentlichen. Stattdessen konzentrierten sie sich auf Alben und Konzerte. Trotzdem wurden Songs wie 'Stairway to Heaven' zu Radioklaessikern."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Mitglied verliess Metallica vor dem Durchbruch und gruendete spaeter Megadeth?",
        answerA = "Lars Ulrich",
        answerB = "Kirk Hammett",
        answerC = "Dave Mustaine",
        answerD = "Cliff Burton",
        correctAnswer = 2,
        explanation = "Dave Mustaine wurde 1983 aus Metallica entlassen, bevor das Debuetalum erschien. Er gruendete daraufhin Megadeth, die zu einer der erfolgreichsten Thrash-Metal-Bands wurden. Die Rivalitaet zwischen beiden Bands praegte das Genre.",
        difficulty = 2,
        funFact = "Dave Mustaine wurde wegen 'schlechten Benehmens' und uebermassigem Alkohol- und Drogenkonsum aus Metallica geworfen. Er erfuhr davon, als er morgens aufwachte und in einen Greyhound-Bus nach Hause gesetzt wurde."
    ),

    Question(
        categoryId = 5,
        questionText = "Welcher Sanger trat als Soloartist nach dem Austritt aus Genesis eine erfolgreiche Solokarriere an?",
        answerA = "Peter Gabriel",
        answerB = "Phil Collins",
        answerC = "Beide (Gabriel und Collins)",
        answerD = "Mike Rutherford",
        correctAnswer = 2,
        explanation = "Sowohl Peter Gabriel (verliess Genesis 1975) als auch Phil Collins (nach der Auflosung 1996 und spaeterer Reunion) hatten ausserordentlich erfolgreiche Solokarrieren. Collins war auch der Nachfolger Gabriels als Saenger der aktiven Band.",
        difficulty = 2,
        funFact = "Phil Collins ist einer der wenigen Musiker, der als Soloartist, als Mitglied einer Band (Genesis) und als Mitglied einer anderen Band (Brand X) jeweils Nummer-1-Alben hatte."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche Band hatte Mitglieder wie Damon Albarn und Graham Coxon und repraesentierte den britischen Britpop der 90er?",
        answerA = "Oasis",
        answerB = "Pulp",
        answerC = "Suede",
        answerD = "Blur",
        correctAnswer = 3,
        explanation = "Blur, bestehend aus Damon Albarn, Graham Coxon, Alex James und Dave Rowntree, war eine der fuerentscheidenden Britpop-Bands der 1990er. Ihre Rivalitaet mit Oasis fuer die britischen Medien wurde 'Battle of Britpop' genannt.",
        difficulty = 2,
        funFact = "Im August 1995 erschienen Blurs 'Country House' und Oasis' 'Roll with It' am selben Tag. Blur gewann die Charts-Battle, aber Oasis verkaufte langfristig mehr. Damon Albarn wurde spaeter beruehmt als Frontmann von Gorillaz."
    ),

    Question(
        categoryId = 5,
        questionText = "Welcher Gitarrist gruendete zusammen mit Robert Plant, John Paul Jones und John Bonham die Band Led Zeppelin?",
        answerA = "Jeff Beck",
        answerB = "Eric Clapton",
        answerC = "Jimmy Page",
        answerD = "Pete Townshend",
        correctAnswer = 2,
        explanation = "Jimmy Page war Gitarrist und Grunder von Led Zeppelin. Bevor er die Band gruendete, war er Mitglied der Yardbirds. Er entwickelte einen einzigartigen Gitarrenstil, der Blues, Folk und Eastern-Einfluesse kombinierte.",
        difficulty = 2,
        funFact = "Jimmy Pages Gitarre - eine sunburst 1959 Gibson Les Paul Standard - gilt als eine der wertvollsten E-Gitarren der Welt. Er verwendet beim Spielen oft einen Geigenbuegel statt eines Plektrums."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche Saengerin gruendete zusammen mit Dolly Parton und Loretta Lynn das sogenannte 'Queens of Country'-Trio der 60er und 70er?",
        answerA = "Tammy Wynette",
        answerB = "Emmylou Harris",
        answerC = "Patsy Cline",
        answerD = "Crystal Gayle",
        correctAnswer = 0,
        explanation = "Tammy Wynette, Dolly Parton und Loretta Lynn galten als die drei dominierenden Stimmen des Country der 1960er und 70er Jahre. Wynettes 'Stand by Your Man' (1968) ist einer der meistverkauften Country-Singles aller Zeiten.",
        difficulty = 2,
        funFact = "Tammy Wynette und George Jones heirateten 1969 und wurden als 'Mr. und Mrs. Country Music' bekannt. Trotz ihrer Scheidung 1975 nahmen sie bis zu Wynettes Tod 1998 weiterhin Duette auf."
    ),

    Question(
        categoryId = 5,
        questionText = "Wer war der Schlagzeuger der Beatles, der die Band 1962 ersetzte, um Ringo Starr Platz zu machen?",
        answerA = "Stuart Sutcliffe",
        answerB = "Pete Best",
        answerC = "Jimmy Nicol",
        answerD = "Tony Sheridan",
        correctAnswer = 1,
        explanation = "Pete Best war von 1960 bis 1962 Schlagzeuger der Beatles. Kurz vor dem grossen Durchbruch wurde er durch Ringo Starr ersetzt, was eine der beruehmt-beruechtigtsten Personalentscheidungen der Musikgeschichte ist.",
        difficulty = 2,
        funFact = "Pete Best hat nie oeffentlich verbittert geklungen ueber seine Entlassung, obwohl er dadurch den groessten Ruhm und Reichtum verpasste. Er schrieb spaeter eine Autobiographie und blieb mit Musik verbunden."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche Band bestand aus den Gallagher-Bruedern Liam und Noel und war eine der erfolgreichsten Britpop-Bands der 1990er?",
        answerA = "Blur",
        answerB = "Radiohead",
        answerC = "Oasis",
        answerD = "Suede",
        correctAnswer = 2,
        explanation = "Oasis aus Manchester mit Liam (Gesang) und Noel Gallagher (Gitarre, Songwriting) war eine der meistverkauften Bands der 90er. Ihr Album '(What's the Story) Morning Glory?' (1995) ist eines der erfolgreichsten britischen Alben ueberhaupt.",
        difficulty = 2,
        funFact = "Die Gallagher-Brueder waren fuer ihre heftigen Streits bekannt, die schliesslich 2009 zur Auflosung fuehrten. Noel verliess die Band nach einem Backstage-Streit in Paris. Seitdem pflegen beide oeffentlich ihre Rivalitaet."
    )
)
