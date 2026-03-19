package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun literatureQuestionsEasy2(): List<Question> = listOf(

    // Question 1 — Grimm: Hänsel und Gretel
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Was lockt die Hexe in Hänsel und Gretel die Kinder in ihr Haus?",
        answerA = "Schokolade und Bonbons",
        answerB = "Ein Lebkuchenhaus",
        answerC = "Goldene Münzen",
        answerD = "Bunte Luftballons",
        correctAnswer = 1,
        explanation = "In dem Grimm-Märchen 'Hänsel und Gretel' besteht das Haus der Hexe aus Lebkuchen, Brot und Zucker, was die hungrigen Kinder anlockt.",
        difficulty = 1,
        funFact = "Das Märchen gilt als Warnung vor dem Vertrauen auf fremde Erwachsene und spiegelt die Angst vor Hungersnöten im mittelalterlichen Europa wider."
    ),

    // Question 2 — Grimm: Schneewittchen
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Wie viele Zwerge leben mit Schneewittchen zusammen?",
        answerA = "Fünf",
        answerB = "Neun",
        answerC = "Sieben",
        answerD = "Drei",
        correctAnswer = 2,
        explanation = "In Schneewittchen von den Gebrüdern Grimm leben genau sieben Zwerge mit Schneewittchen in ihrem kleinen Häuschen im Wald.",
        difficulty = 1,
        funFact = "Die sieben Zwerge haben in der Originalfassung der Grimms keine Namen — die bekannten Namen (Brummbär, Hatschi usw.) stammen aus dem Disney-Film von 1937."
    ),

    // Question 3 — Grimm: Aschenputtel
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Was verliert Aschenputtel beim Verlassen des Ballpalastes?",
        answerA = "Ihren goldenen Ring",
        answerB = "Ihren Seidenschal",
        answerC = "Ihr goldenes Kleid",
        answerD = "Ihren gläsernen Schuh",
        correctAnswer = 3,
        explanation = "Aschenputtel verliert beim Fliehen vom Ball ihren gläsernen Schuh auf der Treppe, den der Prinz aufhebt und nutzt, um sie zu finden.",
        difficulty = 1,
        funFact = "Im Original der Gebrüder Grimm hilft ein Haselnussbaum am Grab der Mutter dem Mädchen — nicht eine gute Fee wie in der Version von Charles Perrault."
    ),

    // Question 4 — Grimm: Rapunzel
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Womit lässt Rapunzel den Prinzen zu sich hinaufklettern?",
        answerA = "Ihrer langen Haarflechte",
        answerB = "Einem Seidenseil",
        answerC = "Einer Strickleiter",
        answerD = "Einem Weinrebengeflecht",
        correctAnswer = 0,
        explanation = "Rapunzel lässt ihre sehr langen Haare aus dem Turm herab, damit die Hexe und später der Prinz an ihr hinaufklettern können.",
        difficulty = 1,
        funFact = "Der Name 'Rapunzel' ist der deutsche Begriff für Feldsalat — im Märchen stiehlt der Vater diese Pflanze aus dem Garten der Zauberin."
    ),

    // Question 5 — Grimm: Bremer Stadtmusikanten
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Welche vier Tiere machen sich in den Bremer Stadtmusikanten auf den Weg nach Bremen?",
        answerA = "Esel, Hund, Katze und Hahn",
        answerB = "Pferd, Hund, Katze und Hahn",
        answerC = "Esel, Hund, Fuchs und Hahn",
        answerD = "Esel, Schaf, Katze und Hahn",
        correctAnswer = 0,
        explanation = "In den Bremer Stadtmusikanten (Gebrüder Grimm) wollen ein Esel, ein Hund, eine Katze und ein Hahn Musikanten in Bremen werden.",
        difficulty = 1,
        funFact = "Die vier Tiere erreichen Bremen nie — sie vertreiben eine Räuberbande und bleiben in deren Haus wohnen."
    ),

    // Question 6 — Grimm: Froschkönig
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Was muss die Prinzessin im Märchen 'Der Froschkönig' mit dem Frosch teilen?",
        answerA = "Ihren goldenen Ball",
        answerB = "Ihr Schloss",
        answerC = "Ihr Abendessen, ihr Bett und ihre Spielsachen",
        answerD = "Ihr Tagebuch",
        correctAnswer = 2,
        explanation = "Die Prinzessin verspricht dem Frosch, mit ihm zu essen, zu spielen und zu schlafen, damit er ihr den goldenen Ball aus dem Brunnen zurückholt.",
        difficulty = 1,
        funFact = "Im Grimm-Original wird der Frosch nicht geküsst — die Prinzessin wirft ihn wütend gegen die Wand, woraufhin er sich in einen Prinzen verwandelt."
    ),

    // Question 7 — Grimm: Rumpelstilzchen
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Was kann Rumpelstilzchen mit einem Spinnrad aus Stroh herstellen?",
        answerA = "Seide",
        answerB = "Wolle",
        answerC = "Silber",
        answerD = "Gold",
        correctAnswer = 3,
        explanation = "Das Männchen Rumpelstilzchen kann Stroh zu Gold spinnen und hilft der Müllerstochter, diese Aufgabe zu erfüllen, die der König ihr gestellt hat.",
        difficulty = 1,
        funFact = "Das Rätsel im Märchen — den Namen des Männchens zu erraten — ist eines der bekanntesten Märchenrätsel der Welt."
    ),

    // Question 8 — Grimm: Dornröschen
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Wie lange schläft Dornröschen in dem Grimm-Märchen?",
        answerA = "100 Jahre",
        answerB = "50 Jahre",
        answerC = "1000 Jahre",
        answerD = "10 Jahre",
        correctAnswer = 0,
        explanation = "Dornröschen schläft nach dem Stich an der Spindel 100 Jahre lang, bis ein Königssohn sie durch einen Kuss weckt.",
        difficulty = 1,
        funFact = "Das gesamte Schloss schläft mit Dornröschen ein — Köche, Pferde, Hunde und sogar die Fliegen an den Wänden."
    ),

    // Question 9 — Grimm: Rotkäppchen (Frage über den Wolf)
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Was antwortet der Wolf als Großmutter verkleidet auf Rotkäppchens Frage nach seinen großen Zähnen?",
        answerA = "'Damit ich dich besser beißen kann!'",
        answerB = "'Damit ich dich besser küssen kann!'",
        answerC = "'Damit ich dir besser antworten kann!'",
        answerD = "'Damit ich dich besser fressen kann!'",
        correctAnswer = 3,
        explanation = "Der berühmte Ausruf des Wolfes lautet: 'Damit ich dich besser fressen kann!' — woraufhin er Rotkäppchen verschluckt.",
        difficulty = 1,
        funFact = "Am Ende des Grimm-Märchens werden sowohl Großmutter als auch Rotkäppchen von einem Jäger lebend aus dem Bauch des Wolfes befreit."
    ),

    // Question 10 — Andersen: Die kleine Meerjungfrau
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Was gibt die kleine Meerjungfrau der Meereshexe als Bezahlung für ihre Beine?",
        answerA = "Ihre schönen Augen",
        answerB = "Ihr Herz",
        answerC = "Ihre schöne Stimme",
        answerD = "Ihre langen Haare",
        correctAnswer = 2,
        explanation = "Die kleine Meerjungfrau opfert ihre wunderschöne Stimme, um Beine zu bekommen und auf dem Land leben zu können.",
        difficulty = 1,
        funFact = "Im Original von Hans Christian Andersen endet das Märchen tragisch — die Meerjungfrau wird nicht von ihrem Prinzen geliebt und löst sich in Schaum auf."
    ),

    // Question 11 — Andersen: Das hässliche Entlein
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "In welches Tier verwandelt sich das hässliche Entlein am Ende des Märchens?",
        answerA = "In einen wunderschönen Schwan",
        answerB = "In einen prächtigen Pfau",
        answerC = "In einen edlen Adler",
        answerD = "In eine elegante Ente",
        correctAnswer = 0,
        explanation = "Das zunächst verspottete 'hässliche Entlein' entpuppt sich als junger Schwan — eines der schönsten Tiere überhaupt.",
        difficulty = 1,
        funFact = "Hans Christian Andersen schrieb dieses Märchen als autobiografische Geschichte — er selbst fühlte sich in seiner Jugend wie ein Außenseiter."
    ),

    // Question 12 — Andersen: Die Schneekönigin
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Welches Mädchen rettet in der Schneekönigin ihren Freund Kay aus dem Eispalast?",
        answerA = "Elsa",
        answerB = "Gerda",
        answerC = "Ida",
        answerD = "Karen",
        correctAnswer = 1,
        explanation = "Gerda macht eine weite, gefährliche Reise, um ihren Freund Kay aus dem Schloss der Schneekönigin zu befreien.",
        difficulty = 1,
        funFact = "Andersens 'Die Schneekönigin' (1844) diente als Inspiration für den Disney-Film 'Frozen' (2013), obwohl die Handlung stark abgeändert wurde."
    ),

    // Question 13 — Andersen: Märchenautor
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Aus welchem Land stammte der Märchenschreiber Hans Christian Andersen?",
        answerA = "Schweden",
        answerB = "Deutschland",
        answerC = "Dänemark",
        answerD = "Norwegen",
        correctAnswer = 2,
        explanation = "Hans Christian Andersen (1805–1875) war Däne und schrieb seine Märchen auf Dänisch. Er gilt als einer der bedeutendsten Märchenautoren der Welt.",
        difficulty = 1,
        funFact = "Andersen schrieb nicht nur Märchen, sondern auch Romane, Theaterstücke und Reisebücher — insgesamt über 150 Märchen."
    ),

    // Question 14 — Schiller: Die Räuber
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Wer schrieb das Drama 'Die Räuber' (1781)?",
        answerA = "Johann Wolfgang von Goethe",
        answerB = "Heinrich Heine",
        answerC = "Gotthold Ephraim Lessing",
        answerD = "Friedrich Schiller",
        correctAnswer = 3,
        explanation = "Friedrich Schiller schrieb 'Die Räuber' als sein erstes großes Drama. Es handelt von den verfeindeten Brüdern Karl und Franz Moor.",
        difficulty = 1,
        funFact = "Schiller war erst 22 Jahre alt, als 'Die Räuber' uraufgeführt wurde — das Publikum reagierte mit derart großer Begeisterung, dass fremde Menschen sich in den Armen lagen."
    ),

    // Question 15 — Schiller: Wilhelm Tell
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Auf welches Ziel schießt Wilhelm Tell in Schillers gleichnamigem Drama seinen berühmten Pfeilschuss?",
        answerA = "Einen Apfel auf dem Kopf seines Sohnes",
        answerB = "Eine Münze auf einem Pfosten",
        answerC = "Eine Zielscheibe aus 100 Metern Entfernung",
        answerD = "Eine Birne auf dem Kopf seiner Frau",
        correctAnswer = 0,
        explanation = "Der Tyrann Gessler zwingt Wilhelm Tell, einen Apfel vom Kopf seines Sohnes Walter zu schießen — Tell gelingt der Schuss auf Anhieb.",
        difficulty = 1,
        funFact = "Friedrich Schiller schrieb 'Wilhelm Tell' 1804 und hat die Schweiz dabei nie persönlich besucht — er recherchierte alles aus Büchern."
    ),

    // Question 16 — Schiller: Kabale und Liebe
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Welche Art von Stück ist Schillers 'Kabale und Liebe' (1784)?",
        answerA = "Eine Komödie",
        answerB = "Ein bürgerliches Trauerspiel",
        answerC = "Ein Ritterdrama",
        answerD = "Eine Operette",
        correctAnswer = 1,
        explanation = "'Kabale und Liebe' ist ein bürgerliches Trauerspiel, das die unglückliche Liebe zwischen dem adligen Ferdinand und der bürgerlichen Luise zeigt.",
        difficulty = 1,
        funFact = "Das Stück gilt als eines der ersten bedeutenden Gesellschaftsdramen der deutschen Literatur und kritisierte die Standesgrenzen des 18. Jahrhunderts."
    ),

    // Question 17 — Thomas Mann: Buddenbrooks
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Wovon handelt Thomas Manns Roman 'Buddenbrooks' (1901)?",
        answerA = "Vom Leben eines deutschen Soldaten",
        answerB = "Von einer Liebesgeschichte in Hamburg",
        answerC = "Vom Aufstieg und Verfall einer Lübecker Kaufmannsfamilie",
        answerD = "Von einem Abenteuer auf hoher See",
        correctAnswer = 2,
        explanation = "'Buddenbrooks' schildert den Aufstieg und den langsamen Niedergang der Kaufmannsfamilie Buddenbrook über vier Generationen in Lübeck.",
        difficulty = 1,
        funFact = "Thomas Mann erhielt 1929 den Nobelpreis für Literatur, hauptsächlich für 'Buddenbrooks' — obwohl der Roman bereits 1901 erschienen war."
    ),

    // Question 18 — Thomas Mann: Der Zauberberg
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Wo spielt Thomas Manns Roman 'Der Zauberberg' (1924)?",
        answerA = "In einem Berliner Luxushotel",
        answerB = "Auf einem Kreuzfahrtschiff im Mittelmeer",
        answerC = "In einem Wiener Krankenhaus",
        answerD = "In einem Tuberkulose-Sanatorium in den Schweizer Alpen",
        correctAnswer = 3,
        explanation = "Hans Castorp besucht für drei Wochen das Sanatorium Berghof in Davos — und bleibt sieben Jahre, fasziniert von der besonderen Atmosphäre des Ortes.",
        difficulty = 1,
        funFact = "Thomas Mann soll den 'Zauberberg' nach einem Besuch bei seiner Frau im Sanatorium Davos begonnen haben — das Buch wurde zu einem der bedeutendsten Romane des 20. Jahrhunderts."
    ),

    // Question 19 — Hermann Hesse: Siddhartha
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Von wem handelt Hermann Hesses Roman 'Siddhartha' (1922)?",
        answerA = "Von einem jungen Brahmanen auf der Suche nach Erleuchtung im alten Indien",
        answerB = "Von einem deutschen Ritter auf Kreuzzug",
        answerC = "Von einem Schweizer Maler in Paris",
        answerD = "Von einem Schweizer Kaufmann in China",
        correctAnswer = 0,
        explanation = "'Siddhartha' folgt einem jungen indischen Brahmanen auf seiner spirituellen Suche nach Selbsterkenntnis und innerer Erfüllung im alten Indien.",
        difficulty = 1,
        funFact = "Der Name 'Siddhartha' ist auch der Geburtsname von Gautama Buddha — Hesses Roman ist jedoch eine eigene Geschichte, keine Buddhabilographie."
    ),

    // Question 20 — Hermann Hesse: Der Steppenwolf
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Wie nennt sich der Protagonist in Hesses Roman 'Der Steppenwolf' (1927)?",
        answerA = "Karl Steppner",
        answerB = "Hans Haller",
        answerC = "Harry Haller",
        answerD = "Heinrich Wolf",
        correctAnswer = 2,
        explanation = "Der Protagonist Harry Haller fühlt sich wie ein Steppenwolf — ein einsames, zwischen zwei Welten gefangenes Wesen, halb Mensch, halb Wolf.",
        difficulty = 1,
        funFact = "Hesse schrieb den 'Steppenwolf' nach einer persönlichen Krise — das Buch wurde in den 1960er-Jahren zu einer Kultlektüre der Gegenkultur."
    ),

    // Question 21 — Franz Kafka: Die Verwandlung
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "In was verwandelt sich Gregor Samsa zu Beginn von Kafkas 'Die Verwandlung' (1915)?",
        answerA = "In einen Schmetterling",
        answerB = "In einen riesigen Käfer (Ungeziefer)",
        answerC = "In eine Ratte",
        answerD = "In einen Wurm",
        correctAnswer = 1,
        explanation = "Gregor Samsa wacht eines Morgens als riesiges Ungeziefer auf — Kafka beschreibt es nicht genauer, aber es wird oft als Käfer dargestellt.",
        difficulty = 1,
        funFact = "Kafka bat den Verlag ausdrücklich: 'Das Insekt selbst darf nicht gezeichnet werden!' — er wollte keine direkte Illustration des Ungeziefers."
    ),

    // Question 22 — Franz Kafka: Der Prozess
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Was passiert mit Josef K. zu Beginn von Kafkas Roman 'Der Prozess' (1925)?",
        answerA = "Er gewinnt eine große Erbschaft",
        answerB = "Er verliert seinen Job",
        answerC = "Er bekommt einen Brief vom Kaiser",
        answerD = "Er wird verhaftet, ohne den Grund zu kennen",
        correctAnswer = 3,
        explanation = "Josef K. wird an seinem 30. Geburtstag verhaftet, ohne dass ihm jemand sagt, wessen er beschuldigt wird — das ist das kafkaeske Grundprinzip des Romans.",
        difficulty = 1,
        funFact = "Kafka ließ 'Der Prozess' unvollendet — sein Freund Max Brod veröffentlichte das Manuskript nach Kafkas Tod trotz dessen Wunsch, alle seine Werke zu verbrennen."
    ),

    // Question 23 — Heinrich Heine: Loreley
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Was tut die Loreley in Heines berühmtem Gedicht, das Schiffer in den Tod lockt?",
        answerA = "Sie kämmt ihr goldenes Haar und singt",
        answerB = "Sie tanzt auf dem Felsen",
        answerC = "Sie weint und ruft nach Hilfe",
        answerD = "Sie wirft Steine ins Wasser",
        correctAnswer = 0,
        explanation = "In Heines Gedicht 'Die Loreley' sitzt die schöne Frau auf einem Felsen, kämmt ihr goldenes Haar und singt ein betörendes Lied, das die Schiffer ablenkt.",
        difficulty = 1,
        funFact = "Das Loreley-Gedicht beginnt mit 'Ich weiß nicht, was soll es bedeuten' — es wurde von Friedrich Silcher 1837 vertont und ist eines der bekanntesten deutschen Lieder weltweit."
    ),

    // Question 24 — Heinrich Heine: Buch der Lieder
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "In welchem Jahr erschien Heinrich Heines berühmte Gedichtsammlung 'Buch der Lieder'?",
        answerA = "1799",
        answerB = "1827",
        answerC = "1850",
        answerD = "1871",
        correctAnswer = 1,
        explanation = "'Buch der Lieder' erschien 1827 und machte Heine schlagartig berühmt. Es enthält viele seiner bekanntesten Liebesgedichte.",
        difficulty = 1,
        funFact = "Das 'Buch der Lieder' war zunächst kein großer Erfolg — erst mit der Vertonung einzelner Gedichte durch Komponisten wie Schubert und Schumann wurde Heine weltberühmt."
    ),

    // Question 25 — Grimm: Gebrüder als Sprachwissenschaftler
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Was waren Jacob und Wilhelm Grimm von Beruf, bevor sie durch ihre Märchensammlung berühmt wurden?",
        answerA = "Ärzte",
        answerB = "Kaufleute",
        answerC = "Sprachwissenschaftler und Bibliothekare",
        answerD = "Maler und Bildhauer",
        correctAnswer = 2,
        explanation = "Die Brüder Grimm waren Sprachwissenschaftler (Germanisten) und Bibliothekare. Sie sammelten Märchen ursprünglich als Beitrag zur deutschen Volkskulturforschung.",
        difficulty = 1,
        funFact = "Jacob und Wilhelm Grimm begannen auch das erste umfassende Deutsche Wörterbuch — das 'Grimm'sche Wörterbuch' — das erst 1961 nach über 100 Jahren fertiggestellt wurde."
    ),

    // Question 26 — Grimm: Dornröschen (böse Fee)
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Warum legt die böse Fee in Dornröschen den Fluch auf das Kind?",
        answerA = "Weil das Kind ihr Eigentum gestohlen hat",
        answerB = "Weil sie das Kind nicht mag",
        answerC = "Weil die Königin ihr Feind ist",
        answerD = "Weil sie nicht zur Tauffeier eingeladen wurde",
        correctAnswer = 3,
        explanation = "Die böse Fee wurde als einzige nicht zur Tauffeier eingeladen, weil der König nur zwölf goldene Teller hatte — aus Rache verflucht sie das Kind.",
        difficulty = 1,
        funFact = "Das Motiv der nicht eingeladenen bösen Fee taucht in vielen Kulturen auf und symbolisiert die Gefahr, jemanden zu übergehen oder zu vergessen."
    ),

    // Question 27 — Grimm: Schneewittchen (Apfel)
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Womit vergiftet die böse Königin Schneewittchen beim letzten Versuch?",
        answerA = "Mit einem vergifteten Apfel",
        answerB = "Mit einer vergifteten Suppe",
        answerC = "Mit einem vergifteten Kamm",
        answerD = "Mit einem vergifteten Trank",
        correctAnswer = 0,
        explanation = "Die Königin versucht Schneewittchen dreimal zu töten — beim dritten Versuch gelingt es ihr mit einem vergifteten Apfel, dessen rote Hälfte das Gift enthält.",
        difficulty = 1,
        funFact = "Die böse Königin versucht Schneewittchen dreimal zu töten: zuerst mit einem Schnürband, dann mit einem vergifteten Kamm, und schließlich mit dem Apfel."
    ),

    // Question 28 — Schiller: Geburtsjahr
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "In welchem Jahr wurde Friedrich Schiller geboren?",
        answerA = "1749",
        answerB = "1759",
        answerC = "1770",
        answerD = "1780",
        correctAnswer = 1,
        explanation = "Friedrich Schiller wurde am 10. November 1759 in Marbach am Neckar geboren und starb 1805 in Weimar im Alter von 45 Jahren.",
        difficulty = 1,
        funFact = "Schiller und Goethe waren enge Freunde und prägten gemeinsam die Epoche der Weimarer Klassik — obwohl sie sehr unterschiedliche Charaktere hatten."
    ),

    // Question 29 — Hesse: Nobelpreis
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Welchen Preis erhielt Hermann Hesse im Jahr 1946?",
        answerA = "Den Friedensnobelpreis",
        answerB = "Den Büchnerpreis",
        answerC = "Den Nobelpreis für Literatur",
        answerD = "Den Goethepreis",
        correctAnswer = 2,
        explanation = "Hermann Hesse erhielt 1946 den Nobelpreis für Literatur für seine inspirierende Schriftstellerei, die klassische humanistische Ideale verkörpert.",
        difficulty = 1,
        funFact = "Hesse lebte seit 1912 in der Schweiz und nahm 1923 die Schweizer Staatsbürgerschaft an — er legte die deutsche Staatsbürgerschaft ab."
    ),

    // Question 30 — Kafka: Herkunft
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Aus welcher Stadt stammte Franz Kafka?",
        answerA = "Wien",
        answerB = "Berlin",
        answerC = "München",
        answerD = "Prag",
        correctAnswer = 3,
        explanation = "Franz Kafka wurde 1883 in Prag geboren, das damals zur k.u.k.-Monarchie (Österreich-Ungarn) gehörte, und lebte dort fast sein gesamtes Leben.",
        difficulty = 1,
        funFact = "Kafka schrieb auf Deutsch, obwohl er in einer tschechischsprachigen Stadt lebte — er gehörte zur deutschen jüdischen Oberschicht Prags."
    ),

    // Question 31 — Andersen: Das Mädchen mit den Schwefelhölzern
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "In welcher Jahreszeit spielt Andersens Märchen 'Das Mädchen mit den Schwefelhölzern'?",
        answerA = "In der Silvesternacht",
        answerB = "Im Sommer",
        answerC = "Im Frühling",
        answerD = "Im Herbst",
        correctAnswer = 0,
        explanation = "Das arme Mädchen verkauft in der Silvesternacht Streichhölzer auf der Straße und zündet sie an, um sich zu wärmen und schöne Visionen zu sehen.",
        difficulty = 1,
        funFact = "Dieses Märchen gilt als eines von Andersens traurigsten — es soll von einem echten Erlebnis inspiriert sein, als Andersen ein armes Mädchen auf der Straße sah."
    ),

    // Question 32 — Grimm: Allerleirauh
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Welches Grimm-Märchen handelt von einer Prinzessin, die sich in einem Mantel aus tausend verschiedenen Fellen versteckt?",
        answerA = "Frau Holle",
        answerB = "Allerleirauh",
        answerC = "Die Gänsemagd",
        answerD = "Das Mädchen mit den Schwefelhölzern",
        correctAnswer = 1,
        explanation = "In 'Allerleirauh' flieht eine Prinzessin vor einer unzumutbaren Heirat, indem sie sich in einem Pelzmantel aus Häuten aller Tiere verkleidet.",
        difficulty = 1,
        funFact = "Das Märchen 'Allerleirauh' hat Parallelen zu 'Aschenputtel' und gilt als eines der ältesten europäischen Märchenmotive."
    ),

    // Question 33 — Grimm: Frau Holle
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Was passiert dem fleißigen Mädchen in 'Frau Holle', wenn es aus dem Brunnen in die Oberwelt zurückkehrt?",
        answerA = "Es wird mit Silber überschüttet",
        answerB = "Es wird mit Diamanten beschenkt",
        answerC = "Es wird mit Gold überschüttet",
        answerD = "Es wird mit Blumen beschenkt",
        correctAnswer = 2,
        explanation = "Das fleißige Mädchen wird beim Verlassen von Frau Holles Reich mit Gold überschüttet — als Lohn für seine Tüchtigkeit und seinen Fleiß.",
        difficulty = 1,
        funFact = "Das faule Mädchen wird dagegen mit Pech übergossen. Das Märchen gilt als Lehrgeschichte über den Wert von Arbeit und Fleiß."
    ),

    // Question 34 — Schiller: Ode an die Freude
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Wessen Gedicht 'Ode an die Freude' wurde von Beethoven in seiner 9. Sinfonie vertont?",
        answerA = "Heinrich Heine",
        answerB = "Goethe",
        answerC = "Novalis",
        answerD = "Friedrich Schiller",
        correctAnswer = 3,
        explanation = "Schillers Gedicht 'An die Freude' (1785) wurde von Ludwig van Beethoven in der Schlussphrase seiner 9. Sinfonie (1824) vertont.",
        difficulty = 1,
        funFact = "Schillers 'Ode an die Freude' ist heute die Europahymne — allerdings nur die Melodie ohne Text, da die EU keine offizielle Sprache für die Hymne wählen wollte."
    ),

    // Question 35 — Thomas Mann: Herkunft
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "In welcher deutschen Stadt ist Thomas Mann aufgewachsen und woher stammt seine Kaufmannsfamilie?",
        answerA = "Lübeck",
        answerB = "Hamburg",
        answerC = "Berlin",
        answerD = "München",
        correctAnswer = 0,
        explanation = "Thomas Mann wurde 1875 in Lübeck geboren und wuchs dort auf. Seine Familie, eine Kaufmannsfamilie, ist das Vorbild für die 'Buddenbrooks'.",
        difficulty = 1,
        funFact = "Das Buddenbrookhaus in Lübeck ist heute ein Literaturmuseum — es ist das reale Vorbild für das Haus der Familie in Manns Roman."
    ),

    // Question 36 — Andersen: Däumelinchen
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Wie klein ist Däumelinchen in Andersens gleichnamigem Märchen?",
        answerA = "So groß wie eine Hand",
        answerB = "So groß wie ein Daumen",
        answerC = "So groß wie eine Tulpenblüte",
        answerD = "So groß wie eine Streichholzschachtel",
        correctAnswer = 1,
        explanation = "Däumelinchen ist nur so groß wie ein Daumen — daher auch ihr Name. Sie wurde in einer Tulpenblüte geboren und erlebt viele Abenteuer.",
        difficulty = 1,
        funFact = "Am Ende des Märchens findet Däumelinchen ihr Glück im Reich der Blumenelfen, wo sie den König der Blumenelfen heiratet und den Namen 'Maja' bekommt."
    ),

    // Question 37 — Grimm: Hänsel und Gretel (Grund des Aussetzens)
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Warum setzen die Eltern in Hänsel und Gretel ihre Kinder im Wald aus?",
        answerA = "Weil die Kinder zu laut waren",
        answerB = "Weil ein böser Zauber auf ihnen lag",
        answerC = "Weil die Familie zu arm war und kein Essen für alle hatte",
        answerD = "Weil die Kinder gestohlen hatten",
        correctAnswer = 2,
        explanation = "Die Stiefmutter überredet den Vater, die Kinder im Wald auszusetzen, weil die Familie so arm ist, dass sie nicht genug Essen für alle haben.",
        difficulty = 1,
        funFact = "Im Original der Grimms ist es die Mutter (nicht die Stiefmutter), die die Kinder aussetzen will — die Brüder Grimm änderten dies in späteren Ausgaben."
    ),

    // Question 38 — Andersen: Der standhafte Zinnsoldat
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Was ist besonders an dem standhaften Zinnsoldaten in Andersens Märchen?",
        answerA = "Er kann zaubern",
        answerB = "Er ist aus Gold",
        answerC = "Er kann sprechen",
        answerD = "Er hat nur ein Bein",
        correctAnswer = 3,
        explanation = "Der standhafte Zinnsoldat hat nur ein Bein, weil beim Gießen nicht genug Zinn übrig war — trotzdem steht er immer aufrecht und mutig da.",
        difficulty = 1,
        funFact = "Das Märchen endet tragisch: Beide — der Soldat und die Papierballerina — landen im Ofen und verbrennen, übrig bleibt nur ein kleines Zinnherz."
    ),

    // Question 39 — Kafka: Verwandlung (Ende)
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Was passiert am Ende von Kafkas 'Die Verwandlung' mit Gregor Samsa?",
        answerA = "Er stirbt, und die Familie ist erleichtert",
        answerB = "Er verwandelt sich zurück in einen Menschen",
        answerC = "Er flieht aus der Wohnung",
        answerD = "Er wird von einem Arzt geheilt",
        correctAnswer = 0,
        explanation = "Gregor Samsa stirbt am Ende des Textes, und seine Familie — die sich von ihm abgewandt hatte — empfindet darüber Erleichterung und plant eine neue Zukunft.",
        difficulty = 1,
        funFact = "Kafkas Erzählung gilt als Metapher für Entfremdung — von Familie, Gesellschaft und Arbeit. Sie ist eine der meistanalysierten Kurzgeschichten der Weltliteratur."
    ),

    // Question 40 — Heine: Exil in Paris
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Warum verließ Heinrich Heine Deutschland und lebte ab 1831 in Paris?",
        answerA = "Weil er eine Französin geheiratet hatte",
        answerB = "Wegen seiner politischen und religiösen Ansichten, die in Deutschland verboten wurden",
        answerC = "Weil er in Paris einen besser bezahlten Job bekam",
        answerD = "Wegen eines Duells",
        correctAnswer = 1,
        explanation = "Heines kritische politische Schriften und sein jüdischer Glaube machten ihm das Leben in Deutschland schwer — er ging ins Pariser Exil und kehrte nie zurück.",
        difficulty = 1,
        funFact = "Heine verbrachte die letzten acht Jahre seines Lebens (1848–1856) bettlägerig in seiner 'Matratzengruft' in Paris — und schrieb dennoch weiter."
    ),

    // Question 41 — Grimm: Der Wolf und die sieben Geißlein
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Wie täuscht der Wolf in 'Der Wolf und die sieben Geißlein' die Kinder?",
        answerA = "Er verkleidet sich als Bäcker",
        answerB = "Er schickt einen Brief von der Mutter",
        answerC = "Er macht seine Stimme weich und seine Pfoten weiß",
        answerD = "Er versteckt sich im Korb",
        correctAnswer = 2,
        explanation = "Der Wolf lässt seine Stimme beim Schmied weich machen und taucht seine Pfoten in Mehl, um die sieben Geißlein glauben zu lassen, er sei ihre Mutter.",
        difficulty = 1,
        funFact = "Das jüngste Geißlein versteckt sich in der Standuhr und überlebt als einziges — es kann danach der Mutter alles berichten."
    ),

    // Question 42 — Andersen: Die kleine Meerjungfrau (Ende)
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Was wird die kleine Meerjungfrau am Ende von Andersens Original-Märchen?",
        answerA = "Eine unsterbliche Göttin des Meeres",
        answerB = "Sie heiratet den Prinzen",
        answerC = "Sie kehrt ins Meer zurück",
        answerD = "Eine Tochter der Luft",
        correctAnswer = 3,
        explanation = "Da sie den Prinzen nicht töten kann, löst sie sich in Meeresschaum auf, wird aber von den Töchtern der Luft gerettet und kann durch gute Taten eine Seele erlangen.",
        difficulty = 1,
        funFact = "Andersen ergänzte das Ende nachträglich — ursprünglich war es noch trostloser. Die 'Töchter der Luft' geben dem Märchen eine Art Hoffnungsschimmer."
    ),

    // Question 43 — Grimm: Sterntaler
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Was erhält das arme, guther Mädchen im Grimm-Märchen 'Die Sterntaler'?",
        answerA = "Taler, die wie Sterne vom Himmel fallen",
        answerB = "Ein Haus aus Gold",
        answerC = "Einen Sack voller Silbermünzen",
        answerD = "Ein königliches Schloss",
        correctAnswer = 0,
        explanation = "Das Mädchen verschenkt alles, was es hat — sogar sein Hemd. Als es nackt im Wald steht, fallen Sterne als Silbertaler vom Himmel auf es herab.",
        difficulty = 1,
        funFact = "Das Märchen 'Die Sterntaler' ist nur eine knappe Seite lang und gilt dennoch als eines der bekanntesten Märchen über Großzügigkeit und Gottvertrauen."
    ),

    // Question 44 — Schiller: Don Karlos
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "In welchem Schiller-Drama spricht die berühmte Zeile: 'Sire, geben Sie Gedankenfreiheit!'?",
        answerA = "Die Räuber",
        answerB = "Don Karlos",
        answerC = "Wallenstein",
        answerD = "Maria Stuart",
        correctAnswer = 1,
        explanation = "In 'Don Karlos' (1787) fordert der Marquis von Posa den spanischen König Philipp II. mit diesen Worten auf, seinen Untertanen Gedankenfreiheit zu gewähren.",
        difficulty = 1,
        funFact = "Dieser Satz gilt als einer der bedeutendsten Freiheitsrufe in der deutschen Literaturgeschichte und wird bis heute in politischen Debatten zitiert."
    ),

    // Question 45 — Hesse: Narziß und Goldmund
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "In welchem Roman von Hermann Hesse geht es um zwei Freunde — einen Asketen und einen Wanderer — im mittelalterlichen Deutschland?",
        answerA = "Demian",
        answerB = "Unterm Rad",
        answerC = "Narziß und Goldmund",
        answerD = "Knulp",
        correctAnswer = 2,
        explanation = "'Narziß und Goldmund' (1930) erzählt die Geschichte zweier gegensätzlicher Freunde: Narziß, der rationale Klosterbruder, und Goldmund, der sinnliche Künstler und Wanderer.",
        difficulty = 1,
        funFact = "Hesse bezeichnete diesen Roman selbst als eines seiner liebsten Werke — er beschreibt darin den Gegensatz zwischen geistiger und sinnlicher Lebensweise."
    ),

    // Question 46 — Thomas Mann: Tonio Kröger
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Wie heißt die Novelle von Thomas Mann, in der ein Schriftsteller zwischen Bürgertum und Künstlertum hin- und hergerissen ist?",
        answerA = "Der Tod in Venedig",
        answerB = "Mario und der Zauberer",
        answerC = "Tristan",
        answerD = "Tonio Kröger",
        correctAnswer = 3,
        explanation = "'Tonio Kröger' (1903) ist eine Novelle über einen Schriftsteller mit bürgerlichem Vater und exotischer Mutter, der sich zwischen beiden Welten nicht zugehörig fühlt.",
        difficulty = 1,
        funFact = "Viele sehen in Tonio Kröger ein Alter Ego von Thomas Mann selbst — die Zerrissenheit zwischen bürgerlichem Leben und künstlerischer Außenseiterstellung war Manns eigenes Lebensthema."
    ),

    // Question 47 — Grimm: Rotkäppchen (Retter)
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Wer rettet im Märchen 'Rotkäppchen' das Mädchen und die Großmutter aus dem Bauch des Wolfes?",
        answerA = "Ein Jäger",
        answerB = "Der König",
        answerC = "Rotkäppchens Vater",
        answerD = "Ein Holzfäller",
        correctAnswer = 0,
        explanation = "Ein Jäger zieht zufällig am Haus der Großmutter vorbei, hört das Schnarchen des Wolfes, schneidet ihm den Bauch auf und befreit Rotkäppchen und die Großmutter.",
        difficulty = 1,
        funFact = "In einer zweiten Version des Märchens bei den Grimms schaffen es Rotkäppchen und die Großmutter, den Wolf selbst zu überlisten — ohne Hilfe des Jägers."
    ),

    // Question 48 — Andersen: Die Nachtigall
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "In Andersens Märchen 'Die Nachtigall' bevorzugt der Kaiser zunächst welche Nachtigall gegenüber der echten?",
        answerA = "Eine goldene Nachtigall aus Japan",
        answerB = "Eine mechanische Nachtigall aus Edelsteinen und Gold",
        answerC = "Eine singende Puppe",
        answerD = "Eine elektrische Nachtigall",
        correctAnswer = 1,
        explanation = "Der Kaiser bekommt eine künstliche, mit Edelsteinen besetzte Nachtigall geschenkt, die er der echten vorzieht — bis er schwer krank wird.",
        difficulty = 1,
        funFact = "Das Märchen gilt als Kritik an der oberflächlichen Begeisterung für technische Spielereien gegenüber echter, natürlicher Schönheit."
    ),

    // Question 49 — Grimm: Rotkäppchen (Warnung der Mutter)
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Was warnt die Mutter Rotkäppchen, bevor es zur Großmutter geht?",
        answerA = "Dem Wolf zu vertrauen",
        answerB = "Den Kuchen nicht zu essen",
        answerC = "Niemals vom Weg abzuweichen",
        answerD = "Nicht zu laut zu sprechen",
        correctAnswer = 2,
        explanation = "Die Mutter warnt Rotkäppchen ausdrücklich: 'Geh ordentlich und lauf nicht vom Weg ab, sonst fällst du und zerbrichst das Glas, dann hat die Großmutter nichts!'",
        difficulty = 1,
        funFact = "Rotkäppchen missachtet diese Warnung, als der Wolf es ablenkt, Blumen zu pflücken — das ist der entscheidende Fehler, der zur Katastrophe führt."
    ),

    // Question 50 — Andersen: Die Prinzessin auf der Erbse
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Wie beweist die Prinzessin in Andersens 'Die Prinzessin auf der Erbse', dass sie eine echte Prinzessin ist?",
        answerA = "Sie kann durch eine Wand hindurchsehen",
        answerB = "Sie kennt alle Sprachen der Welt",
        answerC = "Sie erkennt einen verzauberten Frosch als Prinzen",
        answerD = "Sie spürt eine Erbse unter zwanzig Matratzen und zwanzig Betten",
        correctAnswer = 3,
        explanation = "Die Königin legt eine Erbse unter zwanzig Matratzen und zwanzig Federbetten. Am nächsten Morgen klagt die Prinzessin über schlimme Rückenschmerzen — nur eine echte Prinzessin ist so feinfühlig.",
        difficulty = 1,
        funFact = "Andersens Märchen 'Die Prinzessin auf der Erbse' ist eines seiner kürzesten — nur etwa eine halbe Seite lang — und doch weltberühmt."
    )

)
