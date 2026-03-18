package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun scienceQuestionsEasy5(): List<Question> = listOf(

    // Question 1 — Seasons & Calendar
    // correctAnswer = B (1)
    Question(
        categoryId = 2,
        questionText = "Warum gibt es auf der Erde verschiedene Jahreszeiten?",
        answerA = "Weil die Erde sich mal näher, mal weiter von der Sonne befindet",
        answerB = "Weil die Erde schräg geneigt ist und mal die Nord-, mal die Südhalbkugel mehr Sonne bekommt",
        answerC = "Weil die Sonne im Winter kälter brennt als im Sommer",
        answerD = "Weil die Erdatmosphäre im Winter dicker wird",
        correctAnswer = 1, // B
        explanation = "Die Erde hat eine Achsenneigung von etwa 23,5 Grad. Deshalb ist im Sommer die eigene Halbkugel der Sonne stärker zugewandt (höherer Sonnenstand, längere Tage) und im Winter abgewandt (flacherer Sonnenstand, kürzere Tage).",
        difficulty = 1,
        funFact = "Interessanterweise ist die Erde im Winter sogar etwas näher an der Sonne als im Sommer! Der Grund für die Jahreszeiten ist also nicht der Abstand, sondern allein die Achsneigung der Erde."
    ),

    // Question 2 — Seasons & Calendar
    // correctAnswer = C (2)
    Question(
        categoryId = 2,
        questionText = "Was ist die Sommersonnenwende?",
        answerA = "Der Tag, an dem es am meisten regnet",
        answerB = "Der Tag, an dem Tag und Nacht gleich lang sind",
        answerC = "Der Tag mit dem längsten Tag und der kürzesten Nacht im Jahr",
        answerD = "Der erste Tag des Frühlings",
        correctAnswer = 2, // C
        explanation = "Die Sommersonnenwende (um den 21. Juni auf der Nordhalbkugel) ist der Tag, an dem die Sonne am höchsten steht und die Tage am längsten sind. Danach werden die Tage wieder kürzer.",
        difficulty = 1,
        funFact = null
    ),

    // Question 3 — Seasons & Calendar
    // correctAnswer = D (3) — rotiert von B
    Question(
        categoryId = 2,
        questionText = "Was versteht man unter einem Äquinoktium (Tagundnachtgleiche)?",
        answerA = "Den kältesten Tag des Jahres",
        answerB = "Den Beginn der Sommerferien",
        answerC = "Den Tag, an dem die Sonne genau im Norden aufgeht",
        answerD = "Den Tag, an dem Tag und Nacht gleich lang sind",
        correctAnswer = 3, // D
        explanation = "Das Äquinoktium (Tagundnachtgleiche) tritt zweimal im Jahr auf: um den 20./21. März (Frühlingsanfang) und 22./23. September (Herbstanfang). An diesen Tagen sind Tag und Nacht überall auf der Erde fast gleich lang.",
        difficulty = 1,
        funFact = "Am Äquator sind Tag und Nacht das ganze Jahr über fast gleich lang — immer etwa 12 Stunden. Je weiter man Richtung Pol reist, desto extremer werden die Unterschiede."
    ),

    // Question 4 — Seasons & Calendar
    // correctAnswer = C (2)
    Question(
        categoryId = 2,
        questionText = "Warum hat ein Schaltjahr 366 statt 365 Tage?",
        answerA = "Weil die Erde in manchen Jahren langsamer rotiert",
        answerB = "Weil die Uhr im Winter einmal vorgestellt wird",
        answerC = "Weil die Erde etwa 365,25 Tage braucht, um die Sonne zu umrunden, und der übrige Viertel-Tag alle 4 Jahre ausgeglichen wird",
        answerD = "Weil Kalender ursprünglich nur 364 Tage hatten",
        correctAnswer = 2, // C
        explanation = "Die Erde braucht genau 365 Tage, 5 Stunden und etwa 48 Minuten für einen Umlauf um die Sonne. Da der Kalender nur 365 Tage hat, häuft sich die Differenz an — alle 4 Jahre wird ein Tag (29. Februar) eingeschoben, um den Kalender zu korrigieren.",
        difficulty = 1,
        funFact = null
    ),

    // Question 5 — Seasons & Calendar
    // correctAnswer = A (0) — rotiert von B
    Question(
        categoryId = 2,
        questionText = "Wie viele Monate hat ein Jahr und nach wem sind viele Monatsnamen benannt?",
        answerA = "12 Monate, benannt nach römischen Göttern und Kaisern",
        answerB = "10 Monate, benannt nach griechischen Göttern",
        answerC = "12 Monate, benannt nach nordischen Göttern",
        answerD = "13 Monate, benannt nach Sternzeichen",
        correctAnswer = 0, // A
        explanation = "Das Jahr hat 12 Monate. Viele Namen stammen aus dem Römischen: Januar (Janus), März (Mars), Juni (Juno), Juli (Julius Caesar), August (Augustus). Die Namen September bis Dezember bedeuten ursprünglich der 7. bis 10. Monat.",
        difficulty = 1,
        funFact = "September, Oktober, November und Dezember heißen auf Lateinisch 'siebter', 'achter', 'neunter' und 'zehnter' Monat — sie sind aber der 9. bis 12. Monat! Das liegt daran, dass das Römische Jahr ursprünglich nur 10 Monate hatte."
    ),

    // Question 6 — Human body fun facts
    // correctAnswer = D (3) — rotiert von B
    Question(
        categoryId = 2,
        questionText = "Warum sind Fingerabdrücke bei jedem Menschen einzigartig?",
        answerA = "Weil sie durch die Ernährung während des Lebens geformt werden",
        answerB = "Weil jeder Mensch eine andere Blutgruppe hat, die den Fingerabdruck bestimmt",
        answerC = "Weil die Haut des kleinen Fingers auf das Gehirn reagiert",
        answerD = "Weil sie genetisch zufällig entstehen und sich während der Entwicklung im Mutterleib durch kleine Bewegungen einzigartig formen",
        correctAnswer = 3, // D
        explanation = "Fingerabdrücke entstehen zwischen der 10. und 24. Schwangerschaftswoche. Die genauen Muster werden durch zufällige Bewegungen des Fötus, den Druck im Mutterleib und andere Faktoren beeinflusst — selbst eineiige Zwillinge haben unterschiedliche Fingerabdrücke.",
        difficulty = 1,
        funFact = null
    ),

    // Question 7 — Human body fun facts
    // correctAnswer = C (2)
    Question(
        categoryId = 2,
        questionText = "Warum gähnen wir oft, wenn wir andere gähnen sehen?",
        answerA = "Weil wir unsere Lungen mit frischem Sauerstoff füllen müssen",
        answerB = "Das ist purer Zufall — es gibt keinen echten Zusammenhang",
        answerC = "Weil Gähnen ansteckend ist und mit sozialem Einfühlungsvermögen (Empathie) zusammenhängt",
        answerD = "Weil unser Körper bei Schlafmangel reagiert",
        correctAnswer = 2, // C
        explanation = "Ansteckendes Gähnen hängt mit Empathie und sozialer Bindung zusammen. Menschen, die stärker empathisch sind, gähnen öfter mit. Auch bei Hunden und Schimpansen wurde ansteckendes Gähnen beobachtet.",
        difficulty = 1,
        funFact = "Selbst das Lesen des Wortes 'Gähnen' kann bei vielen Menschen das Gähnen auslösen! Das zeigt, wie stark unser Gehirn auf soziale Signale reagiert — sogar bei geschriebenen Wörtern."
    ),

    // Question 8 — Human body fun facts
    // correctAnswer = A (0) — rotiert von C
    Question(
        categoryId = 2,
        questionText = "Was ist ein Schluckauf (Singultus)?",
        answerA = "Ein unfreiwilliger Krampf des Zwerchfells, der Luft ruckartig einatmen lässt",
        answerB = "Eine rhythmische Erschütterung des Herzens",
        answerC = "Ein Rückfluss von Mageninhalt in die Speiseröhre",
        answerD = "Eine Fehlfunktion der Stimmlippen",
        correctAnswer = 0, // A
        explanation = "Schluckauf entsteht durch einen plötzlichen unwillkürlichen Krampf des Zwerchfells (dem Atemmuskel unter der Lunge). Dabei wird Luft ruckartig eingeatmet, und die Stimmlippen schließen sich abrupt — das ergibt das typische 'Hick'-Geräusch.",
        difficulty = 1,
        funFact = null
    ),

    // Question 9 — Human body fun facts
    // correctAnswer = B (1)
    Question(
        categoryId = 2,
        questionText = "Wozu dienen Gänsehaut beim Menschen?",
        answerA = "Zum Regulieren der Körpertemperatur durch Schwitzen",
        answerB = "Es ist ein evolutionärer Überrest: Bei behaarten Vorfahren richteten sich die Haare auf, um wärmer zu werden oder größer zu wirken",
        answerC = "Um die Haut wasserdicht zu machen",
        answerD = "Als Signal für erhöhten Blutdruck",
        correctAnswer = 1, // B
        explanation = "Gänsehaut ist ein evolutionäres Erbe: Bei unseren behaarten Vorfahren richteten sich die Körperhaare bei Kälte auf und bildeten eine wärmende Luftschicht. Bei Gefahr ließ es sie größer und bedrohlicher wirken. Beim Menschen hat es diese Funktion verloren.",
        difficulty = 1,
        funFact = "Gänsehaut bei emotionalen Momenten (Musik, Stolz, Schaudern) nennt sich 'Piloerektion'. Sie wird vom gleichen Nervensystem ausgelöst wie Kältegänsehaut — unser Körper unterscheidet nicht zwischen physischer Kälte und emotionaler Gänsehaut."
    ),

    // Question 10 — Human body fun facts
    // correctAnswer = D (3) — rotiert von B
    Question(
        categoryId = 2,
        questionText = "Wie schnell kann ein Nieser (Niesen) aus der Nase herausschießen?",
        answerA = "Etwa 5 km/h — so schnell wie ein schnell gehender Mensch",
        answerB = "Etwa 500 km/h — annähernd Schallgeschwindigkeit",
        answerC = "Etwa 50 km/h — wie ein Auto in der Stadt",
        answerD = "Etwa 160 km/h — so schnell wie ein Schnellzug",
        correctAnswer = 3, // D
        explanation = "Ein Niesen kann Luft mit bis zu 160 km/h aus der Nase schleudern. Dabei können Tröpfchen bis zu 8 Meter weit fliegen. Das Niesen ist ein Schutzreflex, der Reizstoffe, Viren oder Fremdkörper aus der Nase entfernt.",
        difficulty = 1,
        funFact = null
    ),

    // Question 11 — Kitchen science
    // correctAnswer = A (0) — rotiert von B
    Question(
        categoryId = 2,
        questionText = "Warum müssen einem beim Zwiebelschneiden die Augen tränen?",
        answerA = "Weil beim Schneiden ein flüchtiges Schwefelgas freigesetzt wird, das die Tränendrüsen reizt",
        answerB = "Weil Zwiebeln giftige Dämpfe enthalten, die das Auge schädigen",
        answerC = "Weil der starke Geruch die Tränendrüsen stimuliert",
        answerD = "Weil die Feuchtigkeit in der Zwiebel verdampft und die Augen austrocknet",
        correctAnswer = 0, // A
        explanation = "Beim Schneiden werden in der Zwiebel Enzyme freigesetzt, die Schwefelsäure bilden. Diese flüchtigen Schwefelverbindungen steigen auf, reagieren mit der Feuchtigkeit im Auge zu milder Schwefelsäure und reizen die Tränendrüsen — worauf diese mit Tränen reagieren.",
        difficulty = 1,
        funFact = "Um beim Zwiebelschneiden nicht zu weinen, hilft es, die Zwiebel vorher zu kühlen — Kälte verlangsamt die Reaktion. Oder man schneidet unter fließendem Wasser, das die Gase wegspült."
    ),

    // Question 12 — Kitchen science
    // correctAnswer = D (3) — rotiert von B
    Question(
        categoryId = 2,
        questionText = "Warum geht Brotteig mit Hefe auf (wird größer)?",
        answerA = "Weil Mehl beim Mischen mit Wasser aufquillt",
        answerB = "Weil das Backpulver Dampf erzeugt",
        answerC = "Weil der Teig beim Kneten Luft aufnimmt, die sich ausdehnt",
        answerD = "Weil Hefe Zucker abbaut und dabei Kohlendioxid-Gas produziert, das den Teig aufbläht",
        correctAnswer = 3, // D
        explanation = "Hefe ist ein Pilz, der Zucker (aus dem Mehl) als Nahrung abbaut. Dabei entstehen Alkohol und Kohlendioxid (CO₂). Das CO₂-Gas bildet kleine Bläschen im Teig und lässt ihn aufgehen. Beim Backen verdampft der Alkohol und das Gas bleibt als Porenstruktur erhalten.",
        difficulty = 1,
        funFact = null
    ),

    // Question 13 — Kitchen science
    // correctAnswer = B (1)
    Question(
        categoryId = 2,
        questionText = "Warum löst sich Zucker in warmem Wasser schneller als in kaltem?",
        answerA = "Weil Zucker bei Wärme chemisch verändert wird und flüssig wird",
        answerB = "Weil warme Wassermoleküle sich schneller bewegen und den Zucker schneller auflösen",
        answerC = "Weil warmes Wasser mehr Sauerstoff enthält",
        answerD = "Weil Kälte Zucker kristallisiert und festhält",
        correctAnswer = 1, // B
        explanation = "In warmem Wasser bewegen sich die Wassermoleküle schneller (höhere kinetische Energie). Sie prallen häufiger und mit mehr Kraft auf die Zuckerkristalle, trennen die Zuckermoleküle schneller voneinander ab und verteilen sie im Wasser — das Lösen geht schneller.",
        difficulty = 1,
        funFact = "Dieses Prinzip gilt für fast alle Feststoffe in Wasser. Deshalb löst man Salz und Zucker auch beim Kochen leichter auf. Eine Ausnahme: Gase lösen sich bei Kälte besser in Wasser — deshalb ist Sprudelwasser kalt am sprudelndsten!"
    ),

    // Question 14 — Kitchen science
    // correctAnswer = A (0) — rotiert von B
    Question(
        categoryId = 2,
        questionText = "Was passiert mit dem Eiweiß in einem Ei, wenn es gekocht wird?",
        answerA = "Es wird durch die Hitze fest — die Proteinstrukturen verändern sich dauerhaft (Denaturierung)",
        answerB = "Es löst sich in Wasser auf",
        answerC = "Es verwandelt sich chemisch in ein anderes Protein",
        answerD = "Es verbrennt bei zu hoher Hitze zu Asche",
        correctAnswer = 0, // A
        explanation = "Beim Erhitzen verändern sich die Eiweißmoleküle (Proteine) im Ei. Die gefalteten Proteinstrukturen entfalten sich und verklumpen miteinander — dieser Vorgang heißt Denaturierung. Er ist irreversibel: Ein gekochtes Ei kann man nicht wieder roh machen.",
        difficulty = 1,
        funFact = null
    ),

    // Question 15 — Kitchen science
    // correctAnswer = C (2) — rotiert von B
    Question(
        categoryId = 2,
        questionText = "Warum wird Wasser beim Einfrieren fest?",
        answerA = "Weil das Wasser seine Farbe und Konsistenz beim Gefrieren verändert",
        answerB = "Weil das Wasser durch Kälte neue chemische Verbindungen eingeht",
        answerC = "Weil die Wassermoleküle bei Kälte langsamer werden und sich in einem geordneten Gitter anordnen",
        answerD = "Weil die Luft im Wasser beim Gefrieren entweicht",
        correctAnswer = 2, // C
        explanation = "Bei 0°C verlieren die Wassermoleküle so viel Wärmeenergie, dass sie sich nicht mehr frei bewegen können. Sie ordnen sich in einer regelmäßigen Kristallstruktur an (Eis-Gitter). Diese starre Anordnung macht das Wasser fest.",
        difficulty = 1,
        funFact = "Eis nimmt mehr Platz ein als flüssiges Wasser! Wenn Wasser gefriert, dehnt es sich um etwa 9% aus. Deshalb platzen Wasserleitungen im Winter, wenn das Wasser darin gefriert."
    ),

    // Question 16 — Night sky
    // correctAnswer = D (3) — rotiert von B
    Question(
        categoryId = 2,
        questionText = "Was sind Sternschnuppen wirklich?",
        answerA = "Sterne, die explodieren und fallen",
        answerB = "Lichter von weit entfernten Raumschiffen",
        answerC = "Stücke des Mondes, die abbrechen",
        answerD = "Winzige Gesteinsbrocken (Meteoroide) aus dem All, die in der Erdatmosphäre verglühen",
        correctAnswer = 3, // D
        explanation = "Sternschnuppen sind keine echten Sterne, sondern Meteoroide — kleine Gesteinsbrocken aus dem Weltall. Wenn sie mit hoher Geschwindigkeit in die Erdatmosphäre eintreten, erzeugt die Reibung so viel Wärme, dass sie aufglühen und verbrennen. Was wir sehen, ist der helle Leuchtstreifen.",
        difficulty = 1,
        funFact = "Jeden Tag treffen hunderte Tonnen Meteoritenmaterial auf die Erde — meist als winziger Staub. Große Brocken, die es bis zum Boden schaffen, heißen Meteoriten. Sehr helle Sternschnuppen nennt man Feuerkugeln oder Boliden."
    ),

    // Question 17 — Night sky
    // correctAnswer = C (2)
    Question(
        categoryId = 2,
        questionText = "Wie entstehen Polarlichter (Nordlichter)?",
        answerA = "Durch Reflexion der Sonnenstrahlen an Eisbergen",
        answerB = "Durch Stürme in der oberen Atmosphäre",
        answerC = "Durch energiereiche Teilchen der Sonne, die mit der Erdatmosphäre wechselwirken",
        answerD = "Durch Vulkanausbrüche in der Arktis",
        correctAnswer = 2, // C
        explanation = "Die Sonne sendet ständig elektrisch geladene Teilchen aus (Sonnenwind). Wenn diese Teilchen auf die Erdatmosphäre treffen, regen sie Sauerstoff- und Stickstoffmoleküle zum Leuchten an. Das Erdmagnetfeld lenkt diese Teilchen zu den Polen — deshalb sieht man Polarlichter hauptsächlich in Polnähe.",
        difficulty = 1,
        funFact = null
    ),

    // Question 18 — Night sky
    // correctAnswer = A (0) — rotiert von C
    Question(
        categoryId = 2,
        questionText = "Was ist die Milchstraße, die wir manchmal als helles Band am Nachthimmel sehen?",
        answerA = "Der Blick von innen auf unsere eigene Galaxie mit ihren Milliarden von Sternen",
        answerB = "Eine große Wolke aus Wasserdampf in der Erdatmosphäre",
        answerC = "Das sind die Lichter aller anderen Galaxien im Universum",
        answerD = "Eine Ansammlung sehr heller Planeten",
        correctAnswer = 0, // A
        explanation = "Die Milchstraße ist unsere eigene Galaxie, von innen betrachtet. Sie enthält etwa 100–400 Milliarden Sterne. Wenn wir das helle Band am Himmel sehen, blicken wir entlang der flachen Scheibe unserer Galaxie, wo die meisten Sterne konzentriert sind.",
        difficulty = 1,
        funFact = "Unsere Sonne ist einer von etwa 200 Milliarden Sternen in der Milchstraße und liegt eher am Rand, nicht im Zentrum. Das Zentrum der Milchstraße enthält ein supermassives schwarzes Loch — etwa 4 Millionen Mal so schwer wie unsere Sonne."
    ),

    // Question 19 — Night sky
    // correctAnswer = C (2)
    Question(
        categoryId = 2,
        questionText = "Warum hat der Mond verschiedene Phasen (Mondphasen)?",
        answerA = "Weil die Erde den Mond teilweise beschattet",
        answerB = "Weil der Mond selbst leuchtet und manchmal dunkle Flecken zeigt",
        answerC = "Weil wir je nach Position der Erde unterschiedlich viel der von der Sonne beleuchteten Mondhälfte sehen",
        answerD = "Weil der Mond sich selbst dreht und dabei andere Seiten zeigt",
        correctAnswer = 2, // C
        explanation = "Der Mond leuchtet nicht selbst, sondern reflektiert Sonnenlicht. Je nachdem wo der Mond auf seinem Umlauf um die Erde steht, sehen wir von der beleuchteten Hälfte mehr oder weniger. Das ergibt die Phasen: Neumond, zunehmend, Vollmond, abnehmend.",
        difficulty = 1,
        funFact = null
    ),

    // Question 20 — Night sky
    // correctAnswer = B (1)
    Question(
        categoryId = 2,
        questionText = "Welcher Stern ist von der Erde aus (nach der Sonne) am hellsten am Nachthimmel zu sehen?",
        answerA = "Polarstern",
        answerB = "Sirius",
        answerC = "Beteigeuze",
        answerD = "Wega",
        correctAnswer = 1, // B
        explanation = "Sirius (auch 'Hundestern' genannt) ist der hellste Stern am Nachthimmel. Er befindet sich im Sternbild Großer Hund und ist etwa 8,6 Lichtjahre von der Erde entfernt. Der Polarstern hingegen ist eher mittelgroß, zeigt aber genau Norden.",
        difficulty = 1,
        funFact = "Sirius ist etwa doppelt so groß wie unsere Sonne und leuchtet 25 Mal heller. Trotzdem ist er nur so hell am Himmel, weil er relativ nah an uns ist. Viele Sterne in der Milchstraße sind viel heller — aber viel weiter weg."
    ),

    // Question 21 — Animal records
    // correctAnswer = C (2)
    Question(
        categoryId = 2,
        questionText = "Welcher Vogel ist das schnellste Tier der Welt im Sturzflug?",
        answerA = "Der Adler",
        answerB = "Der Albatros",
        answerC = "Der Wanderfalke",
        answerD = "Der Strauß",
        correctAnswer = 2, // C
        explanation = "Der Wanderfalke ist das schnellste Tier der Erde. Im Sturzflug erreicht er Geschwindigkeiten von über 320 km/h — kein anderes Tier ist im Sturzflug schneller. Er nutzt diese Geschwindigkeit, um andere Vögel in der Luft zu jagen.",
        difficulty = 1,
        funFact = "Der Wanderfalke hat spezielle Nasenkegel in seinen Nüstern, die den Luftstrom beim Sturzflug verlangsamen — sonst würde der Druck seine Lungen verletzen. Sein Herz schlägt dabei bis zu 600 Mal pro Minute!"
    ),

    // Question 22 — Animal records
    // correctAnswer = D (3) — rotiert von B
    Question(
        categoryId = 2,
        questionText = "Welches ist das größte Tier, das jemals auf der Erde gelebt hat oder heute noch lebt?",
        answerA = "Der Afrikanische Elefant",
        answerB = "Der Tyrannosaurus Rex",
        answerC = "Der Riesenweiße Hai",
        answerD = "Der Blauwal",
        correctAnswer = 3, // D
        explanation = "Der Blauwal ist das größte Tier, das je auf der Erde gelebt hat — größer als alle bekannten Dinosaurier. Er kann bis zu 30 Meter lang werden und über 180 Tonnen wiegen. Sein Herz allein ist so groß wie ein kleines Auto.",
        difficulty = 1,
        funFact = null
    ),

    // Question 23 — Animal records
    // correctAnswer = C (2)
    Question(
        categoryId = 2,
        questionText = "Welches ist das kleinste bekannte Säugetier der Welt (nach Körpermasse)?",
        answerA = "Die Zwergmaus",
        answerB = "Der Zwerghamster",
        answerC = "Die Etruskerspitzmaus",
        answerD = "Der Zwergwiesel",
        correctAnswer = 2, // C
        explanation = "Die Etruskerspitzmaus (Suncus etruscus) ist das kleinste Säugetier nach Körpermasse — sie wiegt nur etwa 1,5–2,5 Gramm. Sie lebt in Südeuropa und Asien und muss fast ununterbrochen fressen, da ihr Herz bis zu 1.200 Mal pro Minute schlägt.",
        difficulty = 1,
        funFact = "Obwohl sie winzig ist, ist die Etruskerspitzmaus ein aggressiver Jäger. Sie frisst täglich ihr eigenes Körpergewicht — hauptsächlich Insekten. Ihr Herz schlägt 25 Mal pro Sekunde, der Mensch schafft nur 1–2 Mal."
    ),

    // Question 24 — Animal records
    // correctAnswer = A (0) — rotiert von B
    Question(
        categoryId = 2,
        questionText = "Welches Tier gilt als das am längsten lebende Wirbeltier auf der Erde?",
        answerA = "Der Grönlandhai",
        answerB = "Der Grönlandwal",
        answerC = "Die Riesenschildkröte",
        answerD = "Der Graupapagei",
        correctAnswer = 0, // A
        explanation = "Der Grönlandhai (Somniosus microcephalus) gilt als das am längsten lebende Wirbeltier. Wissenschaftler schätzen sein Alter auf 400–500 Jahre — manche Exemplare lebten bereits zur Zeit der Renaissance! Er wächst nur 1 cm pro Jahr.",
        difficulty = 1,
        funFact = null
    ),

    // Question 25 — Animal records
    // correctAnswer = B (1) — rotiert von C
    Question(
        categoryId = 2,
        questionText = "Welches Insekt gilt als das stärkste Tier der Welt im Verhältnis zu seiner Körpergröße?",
        answerA = "Die Honigbiene",
        answerB = "Der Mistkäfer (Dungkäfer)",
        answerC = "Die Heuschrecke",
        answerD = "Die Ameise",
        correctAnswer = 1, // B
        explanation = "Der Mistkäfer (Dung Beetle, Onthophagus taurus) ist das stärkste Tier im Verhältnis zum Körpergewicht. Er kann das 1.141-fache seines eigenen Körpergewichts ziehen — als ob ein Mensch sechs vollbeladene Doppeldeckerbusse ziehen würde.",
        difficulty = 1,
        funFact = "Mistkäfer sind auch Navigations-Meister: Sie nutzen die Milchstraße, um nachts geradeaus zu navigieren — sie sind die einzigen bekannten Tiere außer uns Menschen, die die Galaxis zur Orientierung nutzen."
    ),

    // Question 26 — Basic genetics
    // correctAnswer = D (3) — rotiert von B
    Question(
        categoryId = 2,
        questionText = "Welche Form hat das DNA-Molekül in unseren Zellen?",
        answerA = "Eine gerade Linie",
        answerB = "Ein Kreis",
        answerC = "Eine Kugel",
        answerD = "Eine Doppelhelix (zwei miteinander verschlungene Spiralen)",
        correctAnswer = 3, // D
        explanation = "Die DNA hat die Form einer Doppelhelix — zwei spiralförmige Stränge, die miteinander verdrillt sind wie eine verdrehte Strickleiter. Diese Struktur wurde 1953 von Watson und Crick entdeckt (auf Basis der Röntgenaufnahmen von Rosalind Franklin).",
        difficulty = 1,
        funFact = null
    ),

    // Question 27 — Basic genetics
    // correctAnswer = A (0)
    Question(
        categoryId = 2,
        questionText = "Wie viele Chromosomenpaare hat ein gesunder Mensch in jeder Körperzelle?",
        answerA = "23 Paare (46 Chromosomen gesamt)",
        answerB = "12 Paare (24 Chromosomen gesamt)",
        answerC = "46 Paare (92 Chromosomen gesamt)",
        answerD = "Immer unterschiedlich",
        correctAnswer = 0, // A
        explanation = "Jede menschliche Körperzelle enthält 46 Chromosomen (23 Paare). Eines der Paare sind die Geschlechtschromosomen (XX bei Frauen, XY bei Männern). Die anderen 22 Paare tragen Gene für alle Körpereigenschaften.",
        difficulty = 1,
        funFact = "Die gesamte DNA aus einer einzigen menschlichen Zelle, auseinandergefaltet, wäre etwa 2 Meter lang! Zusammengerollt passt sie in einen winzigen Zellkern, der nur 6 Mikrometer groß ist — das sind 0,006 Millimeter."
    ),

    // Question 28 — Basic genetics
    // correctAnswer = C (2)
    Question(
        categoryId = 2,
        questionText = "Welche Augenfarbe dominiert, wenn ein Elternteil braune und das andere blaue Augen hat?",
        answerA = "Blaue Augen, weil Blau die reinere Farbe ist",
        answerB = "Grüne Augen als Mischung",
        answerC = "Meist braune Augen, weil Braun gegenüber Blau dominant ist",
        answerD = "Das ist zufällig — beide Farben sind gleich wahrscheinlich",
        correctAnswer = 2, // C
        explanation = "Das Gen für braune Augen ist dominant gegenüber dem Gen für blaue Augen. Hat ein Kind ein 'Braun-Gen' von einem Elternteil, werden die Augen mit hoher Wahrscheinlichkeit braun. Blaue Augen entstehen nur, wenn beide Elternteile das 'Blau-Gen' weitergeben.",
        difficulty = 1,
        funFact = null
    ),

    // Question 29 — Basic genetics
    // correctAnswer = A (0) — rotiert von B
    Question(
        categoryId = 2,
        questionText = "Was ist der Unterschied zwischen eineiigen und zweieiigen Zwillingen?",
        answerA = "Eineiige Zwillinge entstehen aus einer befruchteten Eizelle und sind genetisch identisch; zweieiige aus zwei verschiedenen Eizellen",
        answerB = "Eineiige Zwillinge haben immer unterschiedliche Blutgruppen",
        answerC = "Zweieiige Zwillinge sind immer gleichen Geschlechts",
        answerD = "Eineiige Zwillinge haben immer die gleiche Augenfarbe, zweieiige nie",
        correctAnswer = 0, // A
        explanation = "Eineiige Zwillinge entstehen, wenn sich eine befruchtete Eizelle früh in zwei teilt. Sie haben identisches Erbgut (DNA) und sind immer gleichen Geschlechts. Zweieiige Zwillinge entstehen aus zwei unabhängig befruchteten Eizellen und teilen nur 50% der Gene — wie normale Geschwister.",
        difficulty = 1,
        funFact = "Obwohl eineiige Zwillinge die gleiche DNA haben, sind sie keine völlig identischen Kopien: Unterschiedliche Umwelteinflüsse und epigenetische Veränderungen führen dazu, dass sie sich mit der Zeit unterschiedlich entwickeln — selbst ihre Fingerabdrücke sind verschieden."
    ),

    // Question 30 — Basic genetics
    // correctAnswer = D (3) — rotiert von B
    Question(
        categoryId = 2,
        questionText = "Was sind Gene?",
        answerA = "Die Bausteine der Muskeln",
        answerB = "Kleine Organe in der Zelle, die Energie produzieren",
        answerC = "Moleküle, die Krankheiten verursachen",
        answerD = "Abschnitte der DNA, die die Bauanleitung für bestimmte Merkmale oder Proteine enthalten",
        correctAnswer = 3, // D
        explanation = "Gene sind spezifische Abschnitte auf der DNA, die die Bauanleitung (Code) für die Herstellung von Proteinen enthalten. Diese Proteine bestimmen dann, wie unser Körper aufgebaut ist und funktioniert — von der Augenfarbe bis hin zu Enzymen.",
        difficulty = 1,
        funFact = null
    ),

    // Question 31 — Earth's water
    // correctAnswer = C (2)
    Question(
        categoryId = 2,
        questionText = "Wie viel Prozent des Wassers auf der Erde ist Süßwasser?",
        answerA = "Etwa 50%",
        answerB = "Etwa 30%",
        answerC = "Etwa 3%",
        answerD = "Etwa 15%",
        correctAnswer = 2, // C
        explanation = "Nur etwa 3% des gesamten Wassers auf der Erde ist Süßwasser. Der Rest (97%) ist Salzwasser in Ozeanen und Meeren. Von den 3% Süßwasser ist wiederum der Großteil in Gletschern und Eiskappen eingefroren und nicht direkt zugänglich.",
        difficulty = 1,
        funFact = "Von dem trinkbaren Süßwasser auf der Erde ist nur ein winziger Bruchteil tatsächlich leicht zugänglich. Etwa 0,007% des gesamten Wassers auf der Erde ist als leicht nutzbares Süßwasser in Flüssen und Seen verfügbar."
    ),

    // Question 32 — Earth's water
    // correctAnswer = D (3) — rotiert von B
    Question(
        categoryId = 2,
        questionText = "Wo befindet sich der größte Teil des Süßwassers auf der Erde?",
        answerA = "In Flüssen und Seen",
        answerB = "Im Grundwasser unter der Erde",
        answerC = "Als Wasserdampf in der Atmosphäre",
        answerD = "In Gletschern und dem Eisschild der Antarktis und Grönlands",
        correctAnswer = 3, // D
        explanation = "Etwa 69% des gesamten Süßwassers auf der Erde ist in Gletschern und Eisschilden gespeichert — vor allem in der Antarktis und Grönland. Würden sie schmelzen, würde der Meeresspiegel um ca. 70 Meter steigen.",
        difficulty = 1,
        funFact = null
    ),

    // Question 33 — Earth's water
    // correctAnswer = B (1) — rotiert von C
    Question(
        categoryId = 2,
        questionText = "Welches ist der tiefste See der Welt?",
        answerA = "Der Kaspische See",
        answerB = "Der Baikalsee",
        answerC = "Der Viktoriasee",
        answerD = "Der Titicacasee",
        correctAnswer = 1, // B
        explanation = "Der Baikalsee in Sibirien (Russland) ist mit bis zu 1.637 Metern Tiefe der tiefste See der Welt. Er ist auch der älteste See (25–30 Millionen Jahre) und enthält etwa 20% des gesamten flüssigen Süßwassers der Erde.",
        difficulty = 1,
        funFact = "Im Baikalsee lebt die Baikalrobbe (Nerpa) — die einzige Robbe weltweit, die ausschließlich in einem Süßwassersee lebt. Wie sie dort hingekommen ist, ist bis heute nicht vollständig geklärt."
    ),

    // Question 34 — Earth's water
    // correctAnswer = A (0) — rotiert von B
    Question(
        categoryId = 2,
        questionText = "Welcher Fluss hat das größte Wasservolumen (Abfluss) der Welt?",
        answerA = "Der Amazonas",
        answerB = "Der Nil",
        answerC = "Der Mississippi",
        answerD = "Der Yangtze",
        correctAnswer = 0, // A
        explanation = "Der Amazonas in Südamerika hat mit Abstand das größte Wasservolumen aller Flüsse der Welt. Er führt mehr Wasser als die nächsten sieben größten Flüsse zusammen und ist für etwa 20% aller Flusswassereinträge in die Weltmeere verantwortlich.",
        difficulty = 1,
        funFact = "Der Nil ist mit etwa 6.650 km zwar der längste Fluss der Welt, aber sein Wasservolumen ist viel geringer als das des Amazonas. Die Länge eines Flusses und sein Wasservolumen hängen nicht direkt zusammen — es kommt auch auf das Einzugsgebiet und den Regenfall an."
    ),

    // Question 35 — Earth's water
    // correctAnswer = B (1)
    Question(
        categoryId = 2,
        questionText = "Was ist Grundwasser?",
        answerA = "Das Meerwasser am Meeresgrund",
        answerB = "Wasser, das in Gesteins- und Bodenschichten unter der Erdoberfläche gespeichert ist",
        answerC = "Das Wasser in tiefen Seen wie dem Baikalsee",
        answerD = "Schmelzwasser von Gletschern",
        correctAnswer = 1, // B
        explanation = "Grundwasser ist Wasser, das durch den Boden in tiefere Gesteinsschichten versickert ist und dort in Hohlräumen gespeichert wird. Es wird durch Brunnen und Quellen genutzt und ist eine wichtige Trinkwasserquelle — weltweit etwa 30% des Süßwassers.",
        difficulty = 1,
        funFact = null
    ),

    // Question 36 — Everyday physics
    // correctAnswer = C (2) — rotiert von B
    Question(
        categoryId = 2,
        questionText = "Warum schwimmt Eis oben auf dem Wasser, obwohl es fest ist?",
        answerA = "Weil Eis wärmer als Wasser ist und aufsteigt",
        answerB = "Weil Eis hydrophob ist (Wasser abstößt)",
        answerC = "Weil Eis beim Gefrieren leichter wird — Wasser dehnt sich beim Gefrieren aus und wird weniger dicht",
        answerD = "Weil Eis aus kleineren Molekülen besteht als flüssiges Wasser",
        correctAnswer = 2, // C
        explanation = "Wasser ist einer der wenigen Stoffe, deren feste Form leichter ist als die flüssige. Beim Gefrieren ordnen sich die Wassermoleküle in einer offenen Kristallstruktur an und nehmen mehr Raum ein (9% Ausdehnung). Dadurch wird Eis weniger dicht als flüssiges Wasser und schwimmt oben.",
        difficulty = 1,
        funFact = "Diese Eigenschaft ist für das Leben auf der Erde entscheidend: Würde Eis sinken, würden Seen und Teiche von unten zufrieren — Fische und andere Wassertiere hätten keine Chance zu überleben. Da Eis oben schwimmt, isoliert es das Wasser darunter."
    ),

    // Question 37 — Everyday physics
    // correctAnswer = B (1)
    Question(
        categoryId = 2,
        questionText = "Warum bekommt man manchmal einen elektrischen Schlag, wenn man nach dem Anziehen eine Türklinke anfasst?",
        answerA = "Weil die Türklinke selbst elektrisch geladen ist",
        answerB = "Weil sich durch Reibung der Kleidung statische Elektrizität aufgebaut hat, die sich beim Berühren entlädt",
        answerC = "Weil die Hausinstallation einen Defekt hat",
        answerD = "Weil Metallklinken besonders kalt sind und das Kribbeln erzeugen",
        correctAnswer = 1, // B
        explanation = "Beim An- und Ausziehen von Kleidung entsteht durch Reibung statische Elektrizität (Triboelektrizität). Elektrische Ladungen sammeln sich auf dem Körper. Beim Berühren einer leitenden Türklinke entlädt sich die Ladung als kleiner Funken — man spürt einen kurzen Stromschlag.",
        difficulty = 1,
        funFact = null
    ),

    // Question 38 — Everyday physics
    // correctAnswer = D (3) — rotiert von C
    Question(
        categoryId = 2,
        questionText = "Welche Form haben Regentropfen wirklich während des Fallens?",
        answerA = "Tropfenform (unten spitz, oben rund) wie auf Bildern",
        answerB = "Länglich wie ein Stäbchen",
        answerC = "Vollkommen rund wie eine Kugel",
        answerD = "Abgeflacht wie ein Brötchen — unten flach durch den Luftwiderstand",
        correctAnswer = 3, // D
        explanation = "Kleine Regentropfen sind fast kugelförmig. Größere Tropfen werden durch den Luftwiderstand abgeflacht — unten flach, oben gewölbt, wie ein Hamburger-Brötchen. Die klassische 'Tropfenform' stimmt nicht — das kommt von Tropfen, die an einem Hahn hängen.",
        difficulty = 1,
        funFact = "Sehr große Regentropfen (über 4–5 mm) können beim Fallen zerfallen, weil der Luftwiderstand zu groß wird. Sie teilen sich in kleinere Tropfen auf — deshalb gibt es nur selten sehr große Regentropfen."
    ),

    // Question 39 — Everyday physics
    // correctAnswer = A (0) — rotiert von B
    Question(
        categoryId = 2,
        questionText = "Warum erwärmt eine Mikrowelle Essen, ohne selbst heiß zu werden?",
        answerA = "Weil Mikrowellenstrahlung Wassermoleküle im Essen zum schnellen Schwingen bringt und so Wärme erzeugt",
        answerB = "Weil Mikrowellen chemische Reaktionen im Essen auslösen",
        answerC = "Weil das Metall im Inneren des Ofens Wärme reflektiert",
        answerD = "Weil eine Mikrowelle Infrarotstrahlung wie ein Backofen nutzt",
        correctAnswer = 0, // A
        explanation = "Mikrowellen sind elektromagnetische Strahlung, die Wassermoleküle im Essen zum Schwingen bringt. Diese schnellen Schwingungen erzeugen Wärme (Reibung). Da das Essen Wassermoleküle hat, die Mikrowelle selbst aber nicht, erwärmt sich nur das Essen.",
        difficulty = 1,
        funFact = null
    ),

    // Question 40 — Everyday physics
    // correctAnswer = A (0)
    Question(
        categoryId = 2,
        questionText = "Warum kann man im Schnellkochtopf Essen schneller garen als in einem normalen Topf?",
        answerA = "Weil der erhöhte Druck den Siedepunkt des Wassers auf über 100°C anhebt",
        answerB = "Weil ein Schnellkochtopf das Essen mit Dampf bespritzt",
        answerC = "Weil das Essen im Schnellkochtopf kleiner geschnitten wird",
        answerD = "Weil ein Schnellkochtopf mehr Strom verbraucht und heißer wird",
        correctAnswer = 0, // A
        explanation = "In einem Schnellkochtopf herrscht höherer Luftdruck (da er verschlossen ist). Bei höherem Druck siedet Wasser erst bei 120°C statt 100°C. Essen gart bei höheren Temperaturen schneller — deshalb ist ein Schnellkochtopf bis zu dreimal so schnell wie ein normaler Topf.",
        difficulty = 1,
        funFact = "Das Gegenteil gilt auf hohen Bergen: Dort ist der Luftdruck niedriger, Wasser siedet schon unter 100°C. Auf dem Mount Everest (8.849 m) kocht Wasser bereits bei ca. 70°C — weich zu kochende Eier sind dort kaum möglich!"
    ),

    // Question 41 — Famous scientists
    // correctAnswer = D (3) — rotiert von B
    Question(
        categoryId = 2,
        questionText = "Womit ist Albert Einstein vor allem bekannt?",
        answerA = "Er erfand das Telefon",
        answerB = "Er entdeckte die Röntgenstrahlung",
        answerC = "Er baute das erste Auto",
        answerD = "Er entwickelte die Relativitätstheorie, die unser Verständnis von Raum, Zeit und Energie revolutionierte",
        correctAnswer = 3, // D
        explanation = "Albert Einstein (1879–1955) entwickelte die Spezielle (1905) und die Allgemeine Relativitätstheorie (1915). Bekannt ist auch seine Formel E=mc², die zeigt, dass Masse und Energie äquivalent sind. 1921 erhielt er den Nobelpreis für Physik.",
        difficulty = 1,
        funFact = "Einstein hat die Schule nicht abgebrochen — er war ein hervorragender Schüler in Mathematik und Physik. Das Gerücht stammt aus einer Verwechslung des Schweizer und Deutschen Notensystems, in dem 1 einmal die beste und einmal die schlechteste Note war."
    ),

    // Question 42 — Famous scientists
    // correctAnswer = C (2) — rotiert von B
    Question(
        categoryId = 2,
        questionText = "Was entdeckte Isaac Newton laut einer bekannten Geschichte beim Beobachten eines fallenden Apfels?",
        answerA = "Das Prinzip der Elektrizität",
        answerB = "Die Zusammensetzung des weißen Lichts",
        answerC = "Das Gesetz der Schwerkraft (Gravitation)",
        answerD = "Das Prinzip der Dampfmaschine",
        correctAnswer = 2, // C
        explanation = "Isaac Newton (1643–1727) formulierte das Gravitationsgesetz: Jede Masse zieht jede andere Masse an. Die Geschichte vom Apfel ist wahrscheinlich vereinfacht, aber Newton nutzte tatsächlich die Beobachtung fallender Körper als Inspiration für seine Gravitationstheorie.",
        difficulty = 1,
        funFact = null
    ),

    // Question 43 — Famous scientists
    // correctAnswer = B (1)
    Question(
        categoryId = 2,
        questionText = "Wofür wurde Galileo Galilei von der Kirche verurteilt?",
        answerA = "Er bestritt die Existenz Gottes öffentlich",
        answerB = "Er vertrat das heliozentrische Weltbild, dass die Erde sich um die Sonne dreht",
        answerC = "Er verkaufte gefälschte Arzneimittel",
        answerD = "Er behauptete, die Bibel sei eine Fälschung",
        correctAnswer = 1, // B
        explanation = "Galileo Galilei (1564–1642) unterstützte das heliozentrische Weltbild des Kopernikus (Erde dreht sich um die Sonne). Das widersprach der damaligen Kirchenlehre (geozentrisch: Sonne dreht sich um die Erde). Die Inquisition verurteilte ihn 1633 zum Hausarrest.",
        difficulty = 1,
        funFact = "Galilei soll nach seinem Widerruf gemurmelt haben: 'Und sie bewegt sich doch!' — gemeint war die Erde. Ob er das wirklich sagte, ist historisch nicht belegt, aber die Geschichte illustriert seinen Glauben an seine Erkenntnisse."
    ),

    // Question 44 — Famous scientists
    // correctAnswer = A (0) — rotiert von B
    Question(
        categoryId = 2,
        questionText = "Was ist Charles Darwins bekannteste wissenschaftliche Theorie?",
        answerA = "Die Evolutionstheorie: Lebewesen entwickeln sich durch natürliche Selektion",
        answerB = "Die Entdeckung der DNA",
        answerC = "Die Theorie der Kontinentaldrift",
        answerD = "Die Entdeckung des Penicillins",
        correctAnswer = 0, // A
        explanation = "Charles Darwin (1809–1882) entwickelte die Evolutionstheorie. Er beschrieb, dass Lebewesen sich über viele Generationen durch natürliche Selektion (die Stärkeren überleben und pflanzen sich fort) an ihre Umwelt anpassen und neue Arten entstehen können. Sein Werk 'Über die Entstehung der Arten' (1859) veränderte die Biologie grundlegend.",
        difficulty = 1,
        funFact = null
    ),

    // Question 45 — Body temperature
    // correctAnswer = B (1)
    Question(
        categoryId = 2,
        questionText = "Was gilt als normale Körpertemperatur beim gesunden Erwachsenen?",
        answerA = "35,0 Grad Celsius",
        answerB = "37,0 Grad Celsius (Bereich 36,5–37,5°C)",
        answerC = "38,5 Grad Celsius",
        answerD = "40,0 Grad Celsius",
        correctAnswer = 1, // B
        explanation = "Die normale Körperkerntemperatur eines Erwachsenen liegt zwischen 36,5 und 37,5 Grad Celsius (gemessen rektal). Sie schwankt leicht im Tagesverlauf: morgens etwas niedriger, nachmittags höher. Gemessen in der Achselhöhle ist sie etwa 0,5°C niedriger.",
        difficulty = 1,
        funFact = "Körpertemperatur ist nicht überall gleich: Die Haut an Händen und Füßen kann 10–15°C kälter sein als der Körperkern. Der Körperkern (Herz, Leber, Gehirn) wird bei Kälte prioritär warm gehalten — die Hände werden dafür geopfert."
    ),

    // Question 46 — Body temperature
    // correctAnswer = C (2)
    Question(
        categoryId = 2,
        questionText = "Ab welcher Körperkerntemperatur spricht man beim Menschen von Fieber?",
        answerA = "Ab 37,0 Grad Celsius",
        answerB = "Ab 37,5 Grad Celsius",
        answerC = "Ab 38,0 Grad Celsius",
        answerD = "Ab 39,0 Grad Celsius",
        correctAnswer = 2, // C
        explanation = "Von Fieber spricht man ab einer Körperkerntemperatur von 38,0 Grad Celsius. Fieber ist meist eine natürliche Abwehrreaktion des Körpers gegen Krankheitserreger — höhere Temperaturen verlangsamen das Wachstum vieler Bakterien und Viren.",
        difficulty = 1,
        funFact = null
    ),

    // Question 47 — Body temperature
    // correctAnswer = D (3) — rotiert von B
    Question(
        categoryId = 2,
        questionText = "Was ist Unterkühlung (Hypothermie) beim Menschen?",
        answerA = "Wenn man sich erkältet und schnieft",
        answerB = "Wenn man im Winter friert, aber die Temperatur normal bleibt",
        answerC = "Wenn man nach dem Sport eine niedrige Körpertemperatur hat",
        answerD = "Wenn die Körperkerntemperatur auf unter 35°C sinkt und lebenswichtige Körperfunktionen gefährdet sind",
        correctAnswer = 3, // D
        explanation = "Unterkühlung (Hypothermie) tritt auf, wenn die Körperkerntemperatur unter 35°C sinkt. Der Körper kann dann nicht mehr genug Wärme produzieren. Unter 30°C können Herzrhythmusstörungen auftreten, unter 28°C kann es zum Herzstillstand kommen.",
        difficulty = 1,
        funFact = "Bei schwerer Hypothermie wurden Menschen mit einer Körpertemperatur von unter 14°C wiederbelebt — das ist fast ein Wunder der modernen Medizin. 'Niemand ist tot, bis er warm und tot ist' ist ein medizinischer Grundsatz bei Unterkühlten."
    ),

    // Question 48 — Simple chemistry
    // correctAnswer = B (1)
    Question(
        categoryId = 2,
        questionText = "Aus welchen zwei Elementen besteht Wasser (H2O)?",
        answerA = "Helium und Sauerstoff",
        answerB = "Wasserstoff und Sauerstoff",
        answerC = "Wasserstoff und Stickstoff",
        answerD = "Kohlenstoff und Sauerstoff",
        correctAnswer = 1, // B
        explanation = "Wasser besteht aus zwei Wasserstoffatomen (H) und einem Sauerstoffatom (O) — daher die chemische Formel H2O. Die zwei Wasserstoffatome sind kovalent mit dem Sauerstoffatom verbunden. Diese einfache Verbindung ist die Grundlage allen bekannten Lebens auf der Erde.",
        difficulty = 1,
        funFact = "Obwohl Wasser aus nur zwei Elementen besteht, hat es aussergewoehnliche Eigenschaften: Es ist bei Raumtemperatur fluessig (was fuer eine so kleine Molekuel ungewoehnlich ist), dehnt sich beim Gefrieren aus und kann viele Stoffe loesen. Kein Wunder, dass man es 'Loesungsmittel des Lebens' nennt."
    ),

    // Question 49 — Simple chemistry
    // correctAnswer = C (2)
    Question(
        categoryId = 2,
        questionText = "Was passiert, wenn man Backpulver (Natriumbikarbonat) und Essig (Essigsaeure) miteinander mischt?",
        answerA = "Es entsteht eine giftige Verbindung, die gefaehrlich ist",
        answerB = "Nichts passiert, weil Saeure und Base sich nicht beeinflussen",
        answerC = "Es entsteht eine Schaumreaktion, wobei Kohlendioxid (CO2) als Gas freigesetzt wird",
        answerD = "Das Gemisch wird sehr heiss und kann sich entzuenden",
        correctAnswer = 2, // C
        explanation = "Backpulver (Natriumbikarbonat, NaHCO3) ist eine Base, Essig (Essigsaeure, CH3COOH) ist eine Saeure. Bei der Reaktion entsteht Natriumacetat, Wasser und Kohlendioxidgas (CO2). Das CO2 bildet die charakteristischen Blaeschen. Diese harmlose Reaktion wird in Backrezepton und Schulexperimenten genutzt.",
        difficulty = 1,
        funFact = null
    ),

    // Question 50 — Plants and photosynthesis
    // correctAnswer = A (0)
    Question(
        categoryId = 2,
        questionText = "Was ist Photosynthese, und welche Stoffe werden dabei benoetigt?",
        answerA = "Pflanzen wandeln mithilfe von Sonnenlicht Kohlendioxid und Wasser in Zucker und Sauerstoff um",
        answerB = "Pflanzen atmen Sauerstoff ein und geben Kohlendioxid aus, genau wie Menschen",
        answerC = "Pflanzen nehmen Stickstoff aus der Luft auf und wandeln ihn in Nahrung um",
        answerD = "Pflanzen speichern Waerme aus der Sonne und geben sie nachts an die Umgebung ab",
        correctAnswer = 0, // A
        explanation = "Bei der Photosynthese nutzen Pflanzen Sonnenlicht als Energiequelle, um aus Kohlendioxid (CO2) aus der Luft und Wasser (H2O) aus dem Boden Glucose (Zucker) zu produzieren. Als Nebenprodukt entsteht Sauerstoff (O2), den Pflanzen durch ihre Blattporen abgeben. So produzieren Pflanzen ihre eigene Nahrung.",
        difficulty = 1,
        funFact = "Alle Tiere und Menschen sind letztlich von der Photosynthese abhaengig — entweder essen wir direkt Pflanzen oder Tiere, die Pflanzen gefressen haben. Auch der Sauerstoff in der Luft stammt urspruenglich fast komplett aus der Photosynthese."
    ),

)
