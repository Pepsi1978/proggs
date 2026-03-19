package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun literatureQuestionsMedium5(): List<Question> = listOf(

    // Question 1 — Barock: Vanitas
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Welches lateinische Wort aus dem Barock bedeutet 'Vergänglichkeit' und beschreibt die Nichtigkeit aller irdischen Dinge?",
        answerA = "Vanitas",
        answerB = "Memento",
        answerC = "Carpe",
        answerD = "Tempus",
        correctAnswer = 0,
        explanation = "Vanitas (lateinisch für 'Eitelkeit' oder 'Vergänglichkeit') ist eines der zentralen Leitmotive der Barockliteratur (1600–1720) und steht für die Bedeutungslosigkeit des irdischen Lebens.",
        difficulty = 2,
        funFact = "Der Begriff stammt aus dem Buch Kohelet der Bibel: 'Vanitas vanitatum, omnia vanitas' — 'Eitelkeit der Eitelkeiten, alles ist Eitelkeit.'"
    ),

    // Question 2 — Barock: Carpe Diem
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Was bedeutet der Barock-Leitsatz 'Carpe Diem' auf Deutsch?",
        answerA = "Bedenke den Tod",
        answerB = "Nutze den Tag",
        answerC = "Alles ist vergänglich",
        answerD = "Fürchte das Jenseits",
        correctAnswer = 1,
        explanation = "Carpe Diem (lateinisch: 'Pflücke den Tag') ruft dazu auf, das Leben bewusst zu genießen. Es bildet im Barock einen Gegenpol zum Vanitas-Gedanken.",
        difficulty = 2,
        funFact = "Der Ausdruck stammt ursprünglich vom römischen Dichter Horaz (65–8 v. Chr.) aus seinen 'Carmina'."
    ),

    // Question 3 — Barock: Sonett und Alexandriner
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Welches Versmaß war die bevorzugte Form der Barockliteratur und besteht aus sechshebigen Jamben mit einer Pause in der Mitte?",
        answerA = "Hexameter",
        answerB = "Daktylus",
        answerC = "Alexandriner",
        answerD = "Trochäus",
        correctAnswer = 2,
        explanation = "Der Alexandriner mit seinen 6-hebigen Jamben und der starken Zäsur (Pause) in der Versmitte war das typische Versmaß der Barockliteratur.",
        difficulty = 2,
        funFact = "Der Name 'Alexandriner' leitet sich vom altfranzösischen Alexanderepos (um 1180) ab, das dieses Versmaß populär machte."
    ),

    // Question 4 — Barock: Vertreter
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Welcher Dichter gilt als der bedeutendste Lyriker des deutschen Barock und verfasste das Sonett 'Es ist alles eitel'?",
        answerA = "Martin Opitz",
        answerB = "Paul Fleming",
        answerC = "Hans Jakob Christoffel von Grimmelshausen",
        answerD = "Andreas Gryphius",
        correctAnswer = 3,
        explanation = "Andreas Gryphius (1616–1664) ist der bedeutendste Lyriker des deutschen Barock. Sein Sonett 'Es ist alles eitel' ist ein Paradebeispiel des Vanitas-Motivs.",
        difficulty = 2,
        funFact = "Martin Opitz gilt als 'Vater der deutschen Dichtung' und schuf mit dem 'Buch von der deutschen Poeterey' (1624) die erste deutsche Dichtungslehre."
    ),

    // Question 5 — Aufklärung: Zeitraum und Motto
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Welcher Philosoph prägte mit dem Satz 'Habe Mut, dich deines eigenen Verstandes zu bedienen!' das Motto der Aufklärung?",
        answerA = "Immanuel Kant",
        answerB = "Gotthold Ephraim Lessing",
        answerC = "Christoph Martin Wieland",
        answerD = "Johann Wolfgang von Goethe",
        correctAnswer = 0,
        explanation = "Immanuel Kant (1724–1804) formulierte in seinem Aufsatz 'Was ist Aufklärung?' (1784) diesen berühmten Leitsatz: 'Sapere aude!' — Habe Mut, dich deines eigenen Verstandes zu bedienen.",
        difficulty = 2,
        funFact = "Die Aufklärung (ca. 1720–1800) setzte Vernunft und Bildung an die Stelle von Glauben und Tradition als Grundlage der Gesellschaft."
    ),

    // Question 6 — Aufklärung: Gattungen
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Welche literarische Gattung war in der Aufklärung besonders verbreitet und dient dazu, mit Tieren als Figuren eine moralische Botschaft zu vermitteln?",
        answerA = "Das Sonett",
        answerB = "Die Fabel",
        answerC = "Das Epos",
        answerD = "Die Ballade",
        correctAnswer = 1,
        explanation = "Die Fabel war eine typische Gattung der Aufklärung. Tiere mit menschlichen Eigenschaften illustrierten moralische Lehren — ganz im Sinne des aufklärerischen Bildungsanspruchs.",
        difficulty = 2,
        funFact = "Christian Fürchtegott Gellert (1715–1769) war der bekannteste deutsche Fabeldichter der Aufklärung. Seine 'Fabeln und Erzählungen' (1746/48) wurden zu Bestsellern."
    ),

    // Question 7 — Aufklärung: Lessing
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Gotthold Ephraim Lessing schrieb mit 'Nathan der Weise' (1779) ein Drama, das für welches zentrale aufklärerische Ideal steht?",
        answerA = "Nationale Einheit",
        answerB = "Gottesfurcht",
        answerC = "Religionstoleranz",
        answerD = "Adelsprivilegien",
        correctAnswer = 2,
        explanation = "'Nathan der Weise' ist Lessings bekanntestes Drama und ein Plädoyer für Toleranz zwischen Judentum, Christentum und Islam — ein Kernthema der Aufklärung.",
        difficulty = 2,
        funFact = "Die berühmte Ringparabel im Stück zeigt, dass keine Religion die einzig wahre ist. Das Stück wurde wegen seiner religionskritischen Haltung zunächst nicht aufgeführt."
    ),

    // Question 8 — Sturm und Drang: Merkmale
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Die Epoche des Sturm und Drang (ca. 1765–1790) richtete sich vor allem gegen welches Ideal der Vorgängerepoche?",
        answerA = "Den Glauben an Gott",
        answerB = "Die Adelsprivilegien",
        answerC = "Die antike Mythologie",
        answerD = "Die Vernunft der Aufklärung",
        correctAnswer = 3,
        explanation = "Der Sturm und Drang wandte sich gegen das rationale Weltbild der Aufklärung. Die jungen Autoren setzten stattdessen auf Leidenschaft, Emotion und das schöpferische Genie.",
        difficulty = 2,
        funFact = "Der Name der Epoche stammt aus dem Drama 'Wirrwarr, oder Sturm und Drang' (1776) von Friedrich Maximilian Klinger."
    ),

    // Question 9 — Sturm und Drang: Goethe
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Welcher Briefroman von Goethe aus dem Sturm und Drang (1774) wurde zum Sensation und löste angeblich eine Selbstmordwelle aus?",
        answerA = "Die Leiden des jungen Werthers",
        answerB = "Götz von Berlichingen",
        answerC = "Wilhelm Meisters Lehrjahre",
        answerD = "Faust I",
        correctAnswer = 0,
        explanation = "'Die Leiden des jungen Werthers' von Goethe (1774) ist der bekannteste Roman des Sturm und Drang. Der empfindsame Held stirbt durch Selbstmord aus unerfüllter Liebe.",
        difficulty = 2,
        funFact = "Das sogenannte 'Werther-Fieber' erfasste ganz Europa: Junge Männer kleideten sich wie Werther. Ob der Roman wirklich Suizide auslöste, ist historisch umstritten."
    ),

    // Question 10 — Sturm und Drang: Erlebnislyrik
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Welchen Begriff für eine neue Lyrikform prägte Goethe im Sturm und Drang, bei der persönliche Erlebnisse so geschildert werden, dass der Leser sie mitfühlen kann?",
        answerA = "Gedankenlyrik",
        answerB = "Erlebnislyrik",
        answerC = "Naturlyrik",
        answerD = "Lehrgedicht",
        correctAnswer = 1,
        explanation = "Goethe schuf mit der Erlebnislyrik eine neue Form, in der persönliche Ereignisse und Gefühle so geschildert werden, dass der Leser sie 'nacherleben' kann.",
        difficulty = 2,
        funFact = "Goethes 'Willkommen und Abschied' (1771) gilt als eines der ersten Erlebnisgedichte der deutschen Literaturgeschichte."
    ),

    // Question 11 — Weimarer Klassik: Zeitraum
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Die Weimarer Klassik im engeren Sinne bezeichnet die gemeinsame Schaffensperiode von Goethe und Schiller. Wann begann ihre Freundschaft?",
        answerA = "1786",
        answerB = "1805",
        answerC = "1794",
        answerD = "1832",
        correctAnswer = 2,
        explanation = "Die enge Zusammenarbeit und Freundschaft zwischen Goethe und Schiller begann 1794 und dauerte bis zu Schillers Tod 1805. Diese Phase gilt als Kern der Weimarer Klassik.",
        difficulty = 2,
        funFact = "Beide Dichter schrieben gemeinsam die Epigramm-Sammlung 'Xenien' (1796), in der sie andere Autoren ihrer Zeit satirisch kritisierten."
    ),

    // Question 12 — Weimarer Klassik: Ideale
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Welches zentrale Ideal der Weimarer Klassik beschreibt die Vervollkommnung des Menschen durch Bildung, Vernunft und Schönheit?",
        answerA = "Das Genie",
        answerB = "Die Volksnähe",
        answerC = "Die Weltflucht",
        answerD = "Die Humanität",
        correctAnswer = 3,
        explanation = "Humanität — die Vervollkommnung des Menschen — war das höchste Ideal der Weimarer Klassik. Kunst sollte zur sittlichen Erziehung des Menschen beitragen.",
        difficulty = 2,
        funFact = "Das Dreieck Goethe–Schiller–Herder in Weimar machte die Stadt zur kulturellen Hauptstadt Deutschlands. Heute ist Weimar UNESCO-Weltkulturerbe."
    ),

    // Question 13 — Weimarer Klassik: Schiller
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Welches Drama von Friedrich Schiller aus der Weimarer Klassik zeigt den Freiheitskampf der Schweizer Eidgenossen gegen die Habsburger?",
        answerA = "Wilhelm Tell",
        answerB = "Maria Stuart",
        answerC = "Die Räuber",
        answerD = "Kabale und Liebe",
        correctAnswer = 0,
        explanation = "'Wilhelm Tell' (1804) von Schiller ist sein letztes vollendetes Drama und handelt vom Freiheitskampf der Schweizer. Es zählt zu den bekanntesten Werken der Weimarer Klassik.",
        difficulty = 2,
        funFact = "Schillers 'Die Räuber' (1781) gehört dagegen noch zum Sturm und Drang. Der Übergang zur Klassik ist in Schillers Werk gut nachvollziehbar."
    ),

    // Question 14 — Romantik: Die blaue Blume
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Die 'blaue Blume' ist das bekannteste Symbol der Romantik. In welchem Romanfragment von Novalis taucht sie erstmals auf?",
        answerA = "Die Lehrlinge zu Sais",
        answerB = "Heinrich von Ofterdingen",
        answerC = "Hymnen an die Nacht",
        answerD = "Klingsohr-Märchen",
        correctAnswer = 1,
        explanation = "Novalis (1772–1801) führte die blaue Blume in seinem Romanfragment 'Heinrich von Ofterdingen' ein. Sie steht für die romantische Sehnsucht nach dem Unerreichbaren und Unendlichen.",
        difficulty = 2,
        funFact = "Novalis starb mit nur 28 Jahren, bevor er den Roman vollenden konnte. Die Unvollendetheit wurde selbst zum Symbol romantischer Sehnsucht."
    ),

    // Question 15 — Romantik: Merkmale
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Welches Konzept der Romantik beschreibt die Verbindung aller Künste — Poesie, Musik, Malerei — zu einer einzigen Gesamtkunst?",
        answerA = "Weltschmerz",
        answerB = "Volkspoesie",
        answerC = "Universalpoesie",
        answerD = "Sturm und Drang",
        correctAnswer = 2,
        explanation = "Friedrich Schlegel prägte den Begriff der 'Universalpoesie' als Programm der Frühromantik: Alle Künste sollten zu einer einzigen, grenzenlosen Dichtung verschmelzen.",
        difficulty = 2,
        funFact = "Das Athenäums-Fragment 116 von Friedrich Schlegel gilt als das Manifest der Frühromantik und definiert die 'progressive Universalpoesie'."
    ),

    // Question 16 — Romantik: Vertreter
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Welcher Romantiker schrieb 'Der Sandmann' (1816) und 'Das Fräulein von Scuderi' und ist bekannt für phantastische, schauerliche Erzählungen?",
        answerA = "Ludwig Tieck",
        answerB = "Clemens Brentano",
        answerC = "Joseph von Eichendorff",
        answerD = "E.T.A. Hoffmann",
        correctAnswer = 3,
        explanation = "Ernst Theodor Amadeus Hoffmann (1776–1822) war der Meister der romantischen Schauernovelle. Seine Werke verbinden Alltag und Übernatürliches auf unheimliche Weise.",
        difficulty = 2,
        funFact = "Hoffmann liebte Mozart so sehr, dass er seinen dritten Vornamen 'Wilhelm' in 'Amadeus' änderte — zu Ehren des Komponisten Wolfgang Amadeus Mozart."
    ),

    // Question 17 — Romantik: Sehnsucht
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Welcher Begriff beschreibt das romantische Gefühl der Sehnsucht nach dem Fernen, Unbekannten oder Verlorenen, das nie wirklich erreicht werden kann?",
        answerA = "Fernweh / Weltschmerz",
        answerB = "Carpe Diem",
        answerC = "Sturm und Drang",
        answerD = "Vanitas",
        correctAnswer = 0,
        explanation = "Der 'Weltschmerz' (geprägt von Jean Paul) und das romantische 'Fernweh' beschreiben die tiefe Sehnsucht nach dem Unerreichbaren — ein zentrales Motiv der Romantik.",
        difficulty = 2,
        funFact = "Joseph von Eichendorffs Gedicht 'Sehnsucht' (1834) ist eines der bekanntesten Beispiele: 'Es schienen so golden die Sterne...'"
    ),

    // Question 18 — Biedermeier: Zeitraum
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Welchen Zeitraum umfasst die Biedermeier-Epoche in der deutschen Literatur?",
        answerA = "1800–1815",
        answerB = "1815–1848",
        answerC = "1848–1871",
        answerD = "1871–1900",
        correctAnswer = 1,
        explanation = "Die Biedermeier-Epoche dauerte von 1815 (Wiener Kongress) bis zur Revolution 1848. Das Bürgertum zog sich ins Private zurück und mied die repressive Politik.",
        difficulty = 2,
        funFact = "Der Name 'Biedermeier' ist eigentlich eine Spottfigur: Der Biedermann Gottlieb Biedermaier war eine satirische Figur aus dem 'Fliegenden Blättern' für den spießbürgerlichen Zeitgeist."
    ),

    // Question 19 — Biedermeier: Merkmale
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Welches Motiv kennzeichnet die Biedermeier-Literatur besonders, da sich das Bürgertum vom politischen Leben abwandte?",
        answerA = "Revolution und Freiheit",
        answerB = "Industrialisierung und Arbeit",
        answerC = "Das häusliche, private Leben",
        answerD = "Weltreisen und Abenteuer",
        correctAnswer = 2,
        explanation = "Im Biedermeier stand das häusliche, private Leben im Mittelpunkt. Die Bürger zogen sich von der Politik zurück und konzentrierten sich auf Familie, Natur und Gemütlichkeit.",
        difficulty = 2,
        funFact = "Adalbert Stifter gilt als wichtigster Vertreter des Biedermeier. Sein Roman 'Der Nachsommer' (1857) wird oft als der 'langweiligste Meisterroman der deutschen Literatur' bezeichnet."
    ),

    // Question 20 — Biedermeier: Droste-Hülshoff
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Welche bedeutende Schriftstellerin des Biedermeier schrieb die Novelle 'Die Judenbuche' (1842)?",
        answerA = "Bettina von Arnim",
        answerB = "Sophie von La Roche",
        answerC = "Ricarda Huch",
        answerD = "Annette von Droste-Hülshoff",
        correctAnswer = 3,
        explanation = "Annette von Droste-Hülshoff (1797–1848) gilt als bedeutendste deutsche Dichterin des 19. Jahrhunderts. 'Die Judenbuche' ist eine meisterhafte Kriminalnovelle.",
        difficulty = 2,
        funFact = "Droste-Hülshoff lebte überwiegend auf dem Schloss Meersburg am Bodensee. Ihr Konterfei war von 1978 bis 2002 auf dem deutschen 20-Mark-Schein zu sehen."
    ),

    // Question 21 — Realismus: Merkmale
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Welcher Begriff beschreibt den deutschen Realismus (1848–1890), der die Wirklichkeit zwar wahrheitsgetreu, aber in ästhetisch veredelter Form darstellte?",
        answerA = "Poetischer Realismus",
        answerB = "Magischer Realismus",
        answerC = "Kritischer Realismus",
        answerD = "Naturalistischer Realismus",
        correctAnswer = 0,
        explanation = "Der 'Poetische Realismus' — auch Bürgerlicher Realismus genannt — zeigte die Wirklichkeit nicht ungeschönt, sondern ästhetisch geläutert und vom Zufälligen bereinigt.",
        difficulty = 2,
        funFact = "Theodor Fontane, Theodor Storm und Gottfried Keller sind die bedeutendsten Vertreter des Poetischen Realismus in Deutschland."
    ),

    // Question 22 — Realismus: Novelle
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Welche Gattung erlebte im Poetischen Realismus ihre Blütezeit und ist an einem 'sich ereigneten unerhörten Ereignis' sowie einem Dingsymbol erkennbar?",
        answerA = "Der Bildungsroman",
        answerB = "Die Novelle",
        answerC = "Die Ballade",
        answerD = "Das Epos",
        correctAnswer = 1,
        explanation = "Die Novelle erlebte im Realismus ihre Hochzeit. Goethes Definition — 'Was ist eine Novelle anders als eine sich ereignete unerhörte Begebenheit' — ist bis heute prägend.",
        difficulty = 2,
        funFact = "Theodor Storms 'Der Schimmelreiter' (1888) gilt als die bedeutendste Novelle des deutschen Realismus mit dem weißen Schimmel als zentralem Dingsymbol."
    ),

    // Question 23 — Naturalismus: Unterschied zum Realismus
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Wie lautete die programmatische Formel des Naturalismus, nach der die Kunst der Natur so nah wie möglich kommen und alle subjektiven Einflüsse minimieren sollte?",
        answerA = "Kunst = Wahrheit − Schönheit",
        answerB = "Kunst = Gesellschaft + Kritik",
        answerC = "Kunst = Natur − X",
        answerD = "Kunst = Erlebnis + Sprache",
        correctAnswer = 2,
        explanation = "Die Formel 'Kunst = Natur − X' stammt vom Naturalisten Arno Holz. Das X steht für den subjektiven Einfluss des Autors, der so klein wie möglich sein sollte.",
        difficulty = 2,
        funFact = "Arno Holz und Johannes Schlaf entwickelten den 'konsequenten Naturalismus' und den 'Sekundenstil', bei dem jeder Moment minutiös beschrieben wird."
    ),

    // Question 24 — Naturalismus: Soziale Themen
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Welcher Dramatiker schrieb 'Die Weber' (1892), das bekannteste naturalistische Drama über den schlesischen Weberaufstand von 1844?",
        answerA = "Frank Wedekind",
        answerB = "Johann Nestroy",
        answerC = "Arno Holz",
        answerD = "Gerhart Hauptmann",
        correctAnswer = 3,
        explanation = "Gerhart Hauptmann (1862–1946) ist der bedeutendste Vertreter des deutschen Naturalismus. 'Die Weber' schildert das Elend der schlesischen Weber und gilt als erstes politisches Massendrama.",
        difficulty = 2,
        funFact = "Kaiser Wilhelm II. ließ seine Hofloge im Theater kündigen, als 'Die Weber' uraufgeführt wurde — so provokant war das Stück für die Herrschenden."
    ),

    // Question 25 — Naturalismus: Determinismus
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Der Naturalismus betrachtete den Menschen als durch zwei Faktoren bestimmt, die seinen freien Willen aufheben. Welche sind das?",
        answerA = "Milieu und Vererbung",
        answerB = "Gott und Schicksal",
        answerC = "Vernunft und Gefühl",
        answerD = "Klasse und Bildung",
        correctAnswer = 0,
        explanation = "Im Naturalismus ist der Mensch durch sein Milieu (soziale Umgebung) und seine Vererbung (biologische Anlage) determiniert — ein freier Wille existiert nicht.",
        difficulty = 2,
        funFact = "Diese Idee geht auf den französischen Romancier Émile Zola zurück, der in seiner 'Rougon-Macquart'-Romanreihe die Auswirkungen von Erbanlage auf eine Familie über Generationen verfolgte."
    ),

    // Question 26 — Expressionismus: Zeitraum
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "In welchem Zeitraum war der literarische Expressionismus in Deutschland am stärksten verbreitet?",
        answerA = "1890–1910",
        answerB = "1905–1925",
        answerC = "1918–1933",
        answerD = "1933–1945",
        correctAnswer = 1,
        explanation = "Der Expressionismus in der deutschen Literatur umfasst ungefähr den Zeitraum 1905 bis 1925, mit dem Ersten Weltkrieg (1914–1918) als prägendem historischem Einschnitt.",
        difficulty = 2,
        funFact = "Viele expressionistische Dichter wie Georg Heym und Georg Trakl prophezeiten in ihren Gedichten den kommenden Krieg, bevor er ausbrach."
    ),

    // Question 27 — Expressionismus: Merkmale
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Welches sprachliche Merkmal ist typisch für den Expressionismus, bei dem die normale Satzstruktur aufgebrochen und Wörter ohne logischen Zusammenhang aneinandergereiht werden?",
        answerA = "Alexandriner",
        answerB = "Erlebnislyrik",
        answerC = "Reihungsstil",
        answerD = "Innerer Monolog",
        correctAnswer = 2,
        explanation = "Der expressionistische Reihungsstil (auch 'Telegrammstil') reiht Worte und Bilder ohne logischen Zusammenhang aneinander, um Chaos, Angst und Entfremdung darzustellen.",
        difficulty = 2,
        funFact = "Jakob van Hoddis' Gedicht 'Weltende' (1911) gilt als Initialzündung des Expressionismus. Seine surreale Bildfolge symbolisiert den Untergang der bürgerlichen Welt."
    ),

    // Question 28 — Expressionismus: Vertreter
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Wessen Erzählung 'Die Verwandlung' (1915) — in der Gregor Samsa als Käfer aufwacht — wird dem Expressionismus zugeordnet?",
        answerA = "Alfred Döblin",
        answerB = "Georg Trakl",
        answerC = "Ernst Toller",
        answerD = "Franz Kafka",
        correctAnswer = 3,
        explanation = "Franz Kafka (1883–1924) schrieb 'Die Verwandlung' als Parabel über Entfremdung und Isolation. Das Werk zählt zu den meistgelesenen Texten des 20. Jahrhunderts.",
        difficulty = 2,
        funFact = "Kafka ließ fast alle seine Werke unveröffentlicht und bat seinen Freund Max Brod, sie nach seinem Tod zu verbrennen. Brod ignorierte diesen Wunsch und rettete damit die Werke."
    ),

    // Question 29 — Neue Sachlichkeit: Merkmale
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Die Neue Sachlichkeit (1918–1933) reagierte auf welche Vorgängerepoche mit nüchterner, sachlicher Darstellung ohne Gefühlsüberschwang?",
        answerA = "Expressionismus",
        answerB = "Romantik",
        answerC = "Naturalismus",
        answerD = "Biedermeier",
        correctAnswer = 0,
        explanation = "Die Neue Sachlichkeit entstand als direkte Reaktion auf den emotionalen Überschwang des Expressionismus. Sie betonte einen klaren, sachlichen Stil ohne pathetische Gefühlsdarstellungen.",
        difficulty = 2,
        funFact = "Der Begriff 'Neue Sachlichkeit' wurde ursprünglich für eine Kunstausstellung 1925 in Mannheim geprägt und dann auf die Literatur übertragen."
    ),

    // Question 30 — Neue Sachlichkeit: Vertreter
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Welcher Autor der Neuen Sachlichkeit schrieb 'Im Westen nichts Neues' (1929), den berühmtesten deutschen Antikriegsroman?",
        answerA = "Kurt Tucholsky",
        answerB = "Erich Maria Remarque",
        answerC = "Hans Fallada",
        answerD = "Heinrich Mann",
        correctAnswer = 1,
        explanation = "'Im Westen nichts Neues' von Erich Maria Remarque (1929) schildert den Ersten Weltkrieg aus der Sicht eines jungen Soldaten und wurde zu einem weltweiten Bestseller.",
        difficulty = 2,
        funFact = "Die Nationalsozialisten verboten das Buch 1933 und verbrannten es auf den Scheiterhaufen. Remarque floh ins Exil, seine Schwester wurde in Deutschland hingerichtet."
    ),

    // Question 31 — Neue Sachlichkeit: Episches Theater
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Welcher Dramatiker entwickelte in der Zeit der Neuen Sachlichkeit das 'epische Theater', das den Zuschauer zum Nachdenken statt zum Mitfühlen bringen sollte?",
        answerA = "Gerhart Hauptmann",
        answerB = "Frank Wedekind",
        answerC = "Bertolt Brecht",
        answerD = "Georg Büchner",
        correctAnswer = 2,
        explanation = "Bertolt Brecht (1898–1956) entwickelte das epische Theater mit dem Verfremdungseffekt (V-Effekt), der das Publikum aus der emotionalen Einfühlung herausreißen und zum kritischen Denken anregen sollte.",
        difficulty = 2,
        funFact = "Brechts 'Dreigroschenoper' (1928) mit Musik von Kurt Weill wurde ein sensationeller Theatererfolg und wird bis heute weltweit gespielt."
    ),

    // Question 32 — Trümmerliteratur: Zeitraum und Begriff
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Wie wird die Literatur der unmittelbaren Nachkriegszeit (1945–1950) genannt, die sich gegen die aufgeblasene NS-Sprache wandte und eine neue, einfache Sprache forderte?",
        answerA = "Exilliteratur",
        answerB = "Neue Sachlichkeit",
        answerC = "Heimkehrerliteratur",
        answerD = "Trümmerliteratur / Kahlschlagliteratur",
        correctAnswer = 3,
        explanation = "Die Trümmerliteratur (auch Kahlschlagliteratur) entstand 1945 in den Trümmern des Zweiten Weltkriegs. Sie forderte eine von NS-Phrasen befreite, schlichte Sprache.",
        difficulty = 2,
        funFact = "Wolfgang Weyrauch prägte 1949 den Begriff 'Kahlschlag' in der Anthologie 'Tausend Gramm' — wie ein Kahlschlag im Wald sollte die Sprache von allem Alten befreit werden."
    ),

    // Question 33 — Trümmerliteratur: Vertreter
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Wessen Kurzgeschichten 'Draußen vor der Tür' (1947) und 'Nachts schlafen die Ratten doch' gelten als Musterbeispiele der Trümmerliteratur?",
        answerA = "Wolfgang Borchert",
        answerB = "Heinrich Böll",
        answerC = "Günter Grass",
        answerD = "Alfred Andersch",
        correctAnswer = 0,
        explanation = "Wolfgang Borchert (1921–1947) ist die Symbolfigur der Trümmerliteratur. Er starb mit 26 Jahren, einen Tag bevor sein Theaterstück 'Draußen vor der Tür' uraufgeführt wurde.",
        difficulty = 2,
        funFact = "Heinrich Böll erhielt 1972 den Nobelpreis für Literatur — er ist der bekannteste Vertreter der deutschen Nachkriegsliteratur, der über die Trümmerliteratur hinaus wirkte."
    ),

    // Question 34 — Trümmerliteratur: Gruppe 47
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Welche einflussreiche Literatengruppe wurde 1947 gegründet und prägte die westdeutsche Nachkriegsliteratur durch regelmäßige Lesungstreffen?",
        answerA = "Gruppe 61",
        answerB = "Gruppe 47",
        answerC = "Wiener Gruppe",
        answerD = "Bremer Gruppe",
        correctAnswer = 1,
        explanation = "Die Gruppe 47 wurde von Hans Werner Richter gegründet und versammelte die wichtigsten westdeutschen Autoren der Nachkriegszeit. Mitglieder waren u.a. Böll, Grass und Ingeborg Bachmann.",
        difficulty = 2,
        funFact = "Günter Grass las 1958 bei der Gruppe 47 aus 'Die Blechtrommel' — das Manuskript wurde als so außergewöhnlich bewertet, dass es sofort verlegt wurde."
    ),

    // Question 35 — Postmoderne: Merkmale
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Welches Merkmal der Postmodernen Literatur bezeichnet die bewusste Bezugnahme auf andere literarische Texte, Filme oder Kunstwerke innerhalb eines Werkes?",
        answerA = "Verfremdungseffekt",
        answerB = "Erlebnislyrik",
        answerC = "Intertextualität",
        answerD = "Naturalismus",
        correctAnswer = 2,
        explanation = "Intertextualität — das bewusste Einweben von Anspielungen auf andere Texte und Werke — ist eines der zentralen Merkmale der Postmodernen Literatur (ab ca. 1960).",
        difficulty = 2,
        funFact = "Umberto Ecos 'Der Name der Rose' (1980) gilt als Paradebeispiel postmoderner Intertextualität und verbindet Mittelalter, Detektivroman und Philosophie."
    ),

    // Question 36 — Postmoderne: Metafiktion
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Was versteht man unter 'Metafiktion' in der Postmodernen Literatur?",
        answerA = "Literatur über die Natur",
        answerB = "Romane ohne Erzähler",
        answerC = "Literatur in Versform",
        answerD = "Texte, die ihre eigene Fiktionalität thematisieren",
        correctAnswer = 3,
        explanation = "Metafiktion bezeichnet Texte, die sich selbst als Fiktion erkennen und darüber reflektieren — etwa wenn Figuren wissen, dass sie in einem Roman existieren.",
        difficulty = 2,
        funFact = "Günter Grass, Elfriede Jelinek und Peter Handke gehören zu den wichtigsten deutschsprachigen Autoren der Postmoderne."
    ),

    // Question 37 — Mittelalter: Minnesang
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Was ist unter 'hoher Minne' im mittelhochdeutschen Minnesang zu verstehen?",
        answerA = "Eine vergeistigte, unerfüllte Liebe zur unerreichbaren Dame",
        answerB = "Die eheliche Liebe im Bürgertum",
        answerC = "Liebeslieder für das einfache Volk",
        answerD = "Die körperliche Liebe zwischen Rittern und Bäuerinnen",
        correctAnswer = 0,
        explanation = "Die 'hohe Minne' im Minnesang bezeichnet eine vergeistigte Verehrung der unerreichbaren Herrin — meist die Ehefrau des Herrn. Die Sehnsucht blieb bewusst unerfüllt.",
        difficulty = 2,
        funFact = "Walther von der Vogelweide (ca. 1170–1230) ist der bekannteste Minnesänger. Sein Gedicht 'Under der linden' ist das berühmteste Liebesgedicht des deutschen Mittelalters."
    ),

    // Question 38 — Mittelalter: Nibelungenlied
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Wann entstand das Nibelungenlied, das bedeutendste mittelhochdeutsche Heldenepos?",
        answerA = "Ca. 800–850",
        answerB = "Ca. 1180–1210",
        answerC = "Ca. 1350–1400",
        answerD = "Ca. 1500–1520",
        correctAnswer = 1,
        explanation = "Das Nibelungenlied entstand ca. 1180–1210 im Donauraum von einem unbekannten Verfasser. Es erzählt von Siegfried, Kriemhild und dem Untergang der Burgunder.",
        difficulty = 2,
        funFact = "Das Nibelungenlied ist in der Nibelungenstrophe verfasst: vier paarweise gereimte Langzeilen. Der Autor ist bis heute unbekannt."
    ),

    // Question 39 — Mittelalter: Epos
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Wolfram von Eschenbachs 'Parzival' (ca. 1200–1210) ist ein bedeutendes mittelhochdeutsches Epos. Worum geht es darin?",
        answerA = "Die Gründung des Heiligen Römischen Reichs",
        answerB = "Den Trojanischen Krieg",
        answerC = "Die Suche nach dem Heiligen Gral",
        answerD = "Den Kampf der Nibelungen",
        correctAnswer = 2,
        explanation = "'Parzival' von Wolfram von Eschenbach schildert die Entwicklung eines Ritters und seine Suche nach dem Heiligen Gral — ein zentrales Symbol der mittelalterlichen Ritterliteratur.",
        difficulty = 2,
        funFact = "Wagner vertonte den Parzival-Stoff in seiner letzten Oper 'Parsifal' (1882), die er sein 'Bühnenweihfestspiel' nannte."
    ),

    // Question 40 — Gattungen: Novelle vs. Roman
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Was unterscheidet die Novelle grundsätzlich vom Roman?",
        answerA = "Die Novelle ist immer kürzer und hat keine Figuren",
        answerB = "Die Novelle wird immer in Reimen geschrieben",
        answerC = "Die Novelle spielt nur in der Gegenwart",
        answerD = "Die Novelle konzentriert sich auf ein einzelnes, unerhörtes Ereignis",
        correctAnswer = 3,
        explanation = "Die Novelle ist auf ein einzelnes zentrales Ereignis konzentriert ('sich ereignete unerhörte Begebenheit'), während der Roman breiter angelegt ist und viele Handlungsstränge verfolgen kann.",
        difficulty = 2,
        funFact = "Goethe formulierte die berühmteste Definition der Novelle: 'Was ist eine Novelle anders als eine sich ereignete unerhörte Begebenheit?'"
    ),

    // Question 41 — Gattungen: Drama
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Was ist das wichtigste strukturelle Merkmal des Dramas im Unterschied zu epischen Texten?",
        answerA = "Es gibt keinen Erzähler — die Handlung wird durch Dialoge getragen",
        answerB = "Es wird immer in Versen geschrieben",
        answerC = "Es darf keine Konflikte enthalten",
        answerD = "Es muss weniger als 100 Seiten umfassen",
        correctAnswer = 0,
        explanation = "Das Drama besitzt keinen Erzähler. Die Handlung entfaltet sich durch Dialoge und Monologe der Figuren — zum Unterschied von Epik (mit Erzähler) und Lyrik.",
        difficulty = 2,
        funFact = "Die drei Einheiten des Dramas — Einheit der Handlung, des Ortes und der Zeit — gehen auf Aristoteles' 'Poetik' zurück und wurden in der Weimarer Klassik wieder aufgegriffen."
    ),

    // Question 42 — Gattungen: Ballade
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Die Ballade gilt als 'Urform der Poesie'. Welche drei literarischen Grundgattungen vereint sie in sich?",
        answerA = "Roman, Novelle und Drama",
        answerB = "Epik, Lyrik und Dramatik",
        answerC = "Komödie, Tragödie und Satire",
        answerD = "Märchen, Sage und Fabel",
        correctAnswer = 1,
        explanation = "Die Ballade vereint alle drei Grundgattungen: Sie erzählt eine Geschichte (Epik), ist in Strophen und Versen gehalten (Lyrik) und enthält oft dramatische Dialoge und Szenen (Dramatik).",
        difficulty = 2,
        funFact = "Goethes 'Erlkönig' (1782) und Schillers 'Die Bürgschaft' (1798) sind Paradebeispiele der deutschen Kunstballade."
    ),

    // Question 43 — Gattungen: Fabel
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Welches formale Merkmal gehört zur Fabel und unterscheidet sie von anderen Tiergeschichten?",
        answerA = "Die Tiere sprechen keine menschliche Sprache",
        answerB = "Die Fabel endet immer tragisch",
        answerC = "Am Ende steht eine ausdrückliche moralische Lehre (Epimythion)",
        answerD = "Die Fabel hat keine Handlung",
        correctAnswer = 2,
        explanation = "Die Fabel endet traditionell mit einer moralischen Lehre, dem sogenannten Epimythion. Tiere mit menschlichen Eigenschaften illustrieren diese Lehre.",
        difficulty = 2,
        funFact = "Äsop (ca. 600 v. Chr.) gilt als Erfinder der abendländischen Fabel. Seine Geschichten wurden von La Fontaine (17. Jh.) und Gellert (18. Jh.) neu bearbeitet."
    ),

    // Question 44 — Gattungen: Epos
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Was ist ein Epos und wodurch unterscheidet es sich vom modernen Roman?",
        answerA = "Ein Epos ist ein Theaterstück mit Musik",
        answerB = "Ein Epos hat keine Figuren, nur Landschaftsbeschreibungen",
        answerC = "Ein Epos ist immer kürzer als 100 Zeilen",
        answerD = "Ein Epos ist ein langes Verswerk, das Heldentaten in gehobener Sprache erzählt",
        correctAnswer = 3,
        explanation = "Das Epos ist eine lange, in Versen erzählte Dichtung über Helden und ihre Taten. Es unterscheidet sich vom Prosa-Roman durch seine metrische Form und gehobene Sprache.",
        difficulty = 2,
        funFact = "Homers 'Ilias' und 'Odyssee' (ca. 800 v. Chr.) sind die bekanntesten Epen der Weltliteratur und gelten als Urvorbilder aller Epik."
    ),

    // Question 45 — Gattungen: Satire
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Was ist das primäre Ziel der Satire als literarische Gattung?",
        answerA = "Missstände, Personen oder gesellschaftliche Zustände durch Ironie und Übertreibung zu kritisieren",
        answerB = "Historische Ereignisse möglichst neutral zu dokumentieren",
        answerC = "Gefühle und innere Zustände lyrisch zu beschreiben",
        answerD = "Romantische Liebesgeschichten zu erzählen",
        correctAnswer = 0,
        explanation = "Die Satire nutzt Ironie, Übertreibung und Spott, um Missstände, Personen oder gesellschaftliche Zustände zu kritisieren und zur Veränderung aufzufordern.",
        difficulty = 2,
        funFact = "Heinrich Heines 'Deutschland. Ein Wintermärchen' (1844) gilt als Meisterwerk der politischen Satire in der deutschen Literatur."
    ),

    // Question 46 — Gattungen: Märchen vs. Sage
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Was unterscheidet das Kunstmärchen vom Volksmärchen?",
        answerA = "Das Kunstmärchen hat immer ein Happy End",
        answerB = "Das Kunstmärchen ist von einem bekannten Autor verfasst und bewusst gestaltet",
        answerC = "Das Kunstmärchen spielt immer in fernen Ländern",
        answerD = "Das Kunstmärchen darf keine Tiere enthalten",
        correctAnswer = 1,
        explanation = "Das Kunstmärchen ist ein literarisches Werk eines bekannten Autors — im Gegensatz zum anonymen Volksmärchen, das über Generationen mündlich weitergegeben wurde.",
        difficulty = 2,
        funFact = "E.T.A. Hoffmanns 'Nussknacker und Mäusekönig' (1816) ist eines der bekanntesten deutschen Kunstmärchen — und Grundlage für Tschaikowskys berühmtes Ballett."
    ),

    // Question 47 — Renaissance / Humanismus
    // correct: C (2)
    Question(
        categoryId = 10,
        questionText = "Welche geistige Bewegung der Renaissance stellte den Menschen und seine Würde in den Mittelpunkt und wandte sich gegen das mittelalterliche Gotteszentriertheit?",
        answerA = "Reformation",
        answerB = "Pietismus",
        answerC = "Humanismus",
        answerD = "Mystik",
        correctAnswer = 2,
        explanation = "Der Humanismus der Renaissance (15./16. Jh.) stellte den Menschen (lat. 'homo') ins Zentrum des Denkens. Bildung und antike Vorbilder sollten den idealen Menschen formen.",
        difficulty = 2,
        funFact = "Erasmus von Rotterdam (1466–1536) und Ulrich von Hutten (1488–1523) sind die bekanntesten deutschen Humanisten. Erasmus schrieb 'Das Lob der Torheit' (1511)."
    ),

    // Question 48 — Epochenvergleich: Vernunft vs. Gefühl
    // correct: D (3)
    Question(
        categoryId = 10,
        questionText = "Welche zwei Epochen stehen in direktem Gegensatz zueinander: Die eine betont die Vernunft, die andere setzt dagegen Gefühl und Leidenschaft?",
        answerA = "Barock und Klassik",
        answerB = "Romantik und Naturalismus",
        answerC = "Realismus und Expressionismus",
        answerD = "Aufklärung und Sturm und Drang",
        correctAnswer = 3,
        explanation = "Der Sturm und Drang (ca. 1765–1790) richtete sich direkt gegen das Vernunftideal der Aufklärung und setzte stattdessen auf Emotion, Gefühl und das schöpferische Genie.",
        difficulty = 2,
        funFact = "Interessanterweise machte Goethe diese Entwicklung selbst durch: Er begann im Sturm und Drang und entwickelte sich dann zur Weimarer Klassik hin."
    ),

    // Question 49 — Epochenvergleich: Sprache und Stil
    // correct: A (0)
    Question(
        categoryId = 10,
        questionText = "Welche Epoche forderte nach 1945 einen 'Kahlschlag' der deutschen Sprache und wollte sie von allem NS-Vokabular befreien?",
        answerA = "Trümmerliteratur",
        answerB = "Neue Sachlichkeit",
        answerC = "Expressionismus",
        answerD = "Naturalismus",
        correctAnswer = 0,
        explanation = "Die Trümmerliteratur (1945–1950) und besonders die Kahlschlagliteratur forderten eine radikale Erneuerung der deutschen Sprache, die durch den Nationalsozialismus korrumpiert worden war.",
        difficulty = 2,
        funFact = "Günter Eichs Gedicht 'Inventur' (1945) ist das bekannteste Beispiel: Der Dichter zählt nüchtern seine wenigen Besitztümer auf — ohne jeden Pathos, in schlichtster Sprache."
    ),

    // Question 50 — Epochenvergleich: Gesellschaft und Literatur
    // correct: B (1)
    Question(
        categoryId = 10,
        questionText = "Welche Epoche beschrieb als erste systematisch das Elend des Industrieproletariats und nutzte dabei Dialekte sowie Umgangssprache als bewusstes Stilmittel?",
        answerA = "Realismus",
        answerB = "Naturalismus",
        answerC = "Expressionismus",
        answerD = "Biedermeier",
        correctAnswer = 1,
        explanation = "Der Naturalismus (1880–1900) stellte als erste Epoche das soziale Elend der Arbeiterklasse ungeschönt dar. Dialekte und Umgangssprache wurden gezielt eingesetzt, um Realitätsnähe zu erzeugen.",
        difficulty = 2,
        funFact = "Gerhart Hauptmanns Stück 'Die Weber' wurde bei der Berliner Uraufführung 1893 vom Kaiser verboten — die preußische Zensur sah darin einen Aufruf zur Revolution."
    )

)
