package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun sportQuestionsHard6(): List<Question> = listOf(

    // ── Leichtathletik ────────────────────────────────────────────────────────

    Question(
        categoryId = 6,
        questionText = "Welche Technik beschreibt den Hochsprungstil, bei dem der Athlet rueckwaerts ueber die Latte springt und der heute im Hochleistungssport dominiert?",
        answerA = "Fosbury-Flop",
        answerB = "Straddle-Technik",
        answerC = "Western Roll",
        answerD = "Scissors-Stil",
        correctAnswer = 0,
        explanation = "Der Fosbury-Flop wurde 1968 von Dick Fosbury bei den Olympischen Spielen in Mexiko-Stadt eingefuehrt, als er damit Gold gewann. Dabei ueberquert der Athlet die Latte rueckwaerts mit gewoelbtem Ruecken, was eine effizientere Nutzung des Koerperschwerpunkts erlaubt.",
        difficulty = 3,
        funFact = "Vor dem Fosbury-Flop waren andere Techniken wie der Straddle gaengig. Fosbury gewann 1968 mit einer Hoehe von 2,24 m und revolutionierte damit den Hochsprung weltweit."
    ),

    Question(
        categoryId = 6,
        questionText = "Auf welcher Strecke wird beim Modernen Fuenfkampf die Leichtathletik-Disziplin ausgetragen, die zusammen mit dem Schiessen als 'Laser-Run' kombiniert wird?",
        answerA = "800 m",
        answerB = "1500 m",
        answerC = "3000 m",
        answerD = "5000 m",
        correctAnswer = 2,
        explanation = "Beim Modernen Fuenfkampf wird seit 2009 der Laser-Run als kombinierte Disziplin ausgefuehrt: Die Athleten laufen 3000 m und unterbrechen ihren Lauf viermal fuer je fuenf Schiessserien mit einer Laserpistole auf 10 m Entfernung.",
        difficulty = 3,
        funFact = null
    ),

    // ── Schwimmen ─────────────────────────────────────────────────────────────

    Question(
        categoryId = 6,
        questionText = "Welche Schwimmlage wird bei der Staffel-Disziplin 'Lagen-Staffel' als erstes geschwommen?",
        answerA = "Schmetterling",
        answerB = "Freistil",
        answerC = "Ruecken",
        answerD = "Brust",
        correctAnswer = 2,
        explanation = "Bei der Lagen-Staffel gilt die Reihenfolge: Ruecken, Brust, Schmetterling, Freistil. Dies unterscheidet sich vom Einzeln-Lagen-Schwimmen, bei dem Schmetterling zuerst geschwommen wird. Der Rueckenstart ist noetig, da der Startblock fuer Rueckenschwimmer nicht nutzbar ist.",
        difficulty = 3,
        funFact = "Bei der Einzeln-Lage ist die Reihenfolge Schmetterling, Ruecken, Brust, Freistil. Der Unterschied bei der Staffel liegt daran, dass jeder Wechsel durch eine Anschlagtechnik geregelt wird."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie nennt man die regelmaessige, wellenfoermige Koerperbewegung, die beim Schmetterlingsschwimmen den Antrieb unterstuetzt?",
        answerA = "Dolphin-Kick",
        answerB = "Flutter-Kick",
        answerC = "Whip-Kick",
        answerD = "Frog-Kick",
        correctAnswer = 0,
        explanation = "Der Dolphin-Kick (Delfinkick) ist die typische Beinbewegung beim Schmetterlingsschwimmen: Beide Beine arbeiten synchron in einer wellenartigen, delphinaehnlichen Bewegung. Er wird auch bei der Unterwasserphase nach dem Start und nach Wenden genutzt.",
        difficulty = 3,
        funFact = "Spitzenathleten koennen beim Unterwasser-Dolphin-Kick schneller schwimmen als an der Oberflaeche, weshalb die FINA die maximale Unterwasserstrecke auf 15 m begrenzt."
    ),

    // ── Radsport ──────────────────────────────────────────────────────────────

    Question(
        categoryId = 6,
        questionText = "Was bezeichnet man im Radsport als 'Klassement' beim Giro d'Italia, das die beste Leistung im Zeitfahren und in der Gesamtwertung kombiniert?",
        answerA = "Das Maglia Rosa fuer den Gesamtfuehrer",
        answerB = "Das Maglia Azzurra fuer den besten Bergfahrer",
        answerC = "Das Maglia Ciclamino fuer den besten Punktesammler",
        answerD = "Das Maglia Bianca fuer den besten Jungprofi",
        correctAnswer = 0,
        explanation = "Das Maglia Rosa (Rosatrikot) ist das begehrteste Trikot beim Giro d'Italia und wird vom Fuehrenden der Gesamtwertung getragen. Es ist das Aequivalent zum Gelben Trikot der Tour de France und wurde 1931 eingefuehrt, in Anlehnung an die rosa Farbe der Zeitung La Gazzetta dello Sport.",
        difficulty = 3,
        funFact = "Das Maglia Azzurra fuer den besten Bergfahrer gibt es beim Giro d'Italia nicht — dieses Trikot ist eine haeufige Verwechslung. Der beste Bergfahrer traegt ein blaues Trikot (Maglia Blu) seit 2012."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Fahrtechnik im Bahnradsport bezeichnet das taktische langsame Fahren oder Stehenbleiben, um den Gegner zum Fuehren zu zwingen?",
        answerA = "Derny-Rennen",
        answerB = "Keirin",
        answerC = "Stehersprint (Track Stand)",
        answerD = "Madison",
        correctAnswer = 2,
        explanation = "Der Track Stand (Stehersprint oder Stehversuch) ist eine Taktik im Bahnradsprint: Beide Fahrer bremsen ab und versuchen, moeglichst langsam zu fahren oder sogar zu stehen, um den Gegner zu einer fuehrenden Position zu zwingen. Der Hintermann hat beim Sprint einen Vorteil durch den Windschatten.",
        difficulty = 3,
        funFact = "Profis koennen auf dem Bahnrad nahezu minutenlang stehen, ohne umzufallen — eine Faehigkeit, die jahrelanges Training erfordert und bei Weltcup-Sprints regelmaessig zu sehen ist."
    ),

    // ── Tennis ────────────────────────────────────────────────────────────────

    Question(
        categoryId = 6,
        questionText = "Wie nennt man den Aufschlag im Tennis, bei dem der Ball nach dem Aufprall sehr hoch abspringt, besonders effektiv auf Sand?",
        answerA = "Kick-Aufschlag (American Twist)",
        answerB = "Slice-Aufschlag",
        answerC = "Flat-Aufschlag",
        answerD = "Topspin-Aufschlag",
        correctAnswer = 0,
        explanation = "Der Kick-Aufschlag (auch American Twist genannt) erzeugt durch starken Topspin und Seitwaerrtsdrall einen sehr hohen, unberechenbaren Absprung. Er ist auf Sand besonders wirkungsvoll, da der weiche Untergrund den hohen Absprung zusaetzlich verstaerkt und den Rueckschlaeger weit hinter die Grundlinie treibt.",
        difficulty = 3,
        funFact = "Rafael Nadal nutzt den Kick-Aufschlag auf Sand meisterhaft. Der Drall des Balls erreicht dabei bis zu 4.000 Umdrehungen pro Minute."
    ),

    // ── Kampfsport ────────────────────────────────────────────────────────────

    Question(
        categoryId = 6,
        questionText = "Was bedeutet 'Ippon' im Judo und welche Voraussetzungen muessen erfuellt sein, um einen Ippon zu erzielen?",
        answerA = "Eine Halbpunktwertung durch einen teilweise ausgefuehrten Wurf",
        answerB = "Der hoechste Wertungspunkt: ein kontrollierter Wurf auf den Ruecken oder 10 Sekunden Haltegriff",
        answerC = "Ein Punkt fuer eine Schmerztechnik die zum Aufgeben fuehrt",
        answerD = "Eine Wertung fuer einen Tritt in Wurfrichtung",
        correctAnswer = 1,
        explanation = "Ippon bedeutet 'ein Punkt' und ist die hoechste Wertung im Judo. Er beendet den Kampf sofort. Ein Ippon wird erzielt durch: einen kontrollierten Wurf des Gegners mit Kraft und Geschwindigkeit auf den Ruecken, einen Haltegriff ueber 25 Sekunden (seit 2010 fuer Ippon, zuvor 25 Sekunden), oder durch Aufgabe nach einem Arm- oder Wuergehebel.",
        difficulty = 3,
        funFact = "Die Judowertungen wurden 2017 reformiert: Es gibt jetzt nur noch Ippon und Wazaari. Koka und Yuko wurden abgeschafft, um das Kampfbild klarer zu gestalten."
    ),

    // ── Wintersport ───────────────────────────────────────────────────────────

    Question(
        categoryId = 6,
        questionText = "Welche Besonderheit kennzeichnet den 'Super-G' im alpinen Skisport im Vergleich zur Abfahrt?",
        answerA = "Im Super-G sind weniger Tore vorgeschrieben und das Tempo ist hoeher als in der Abfahrt",
        answerB = "Im Super-G gibt es keine Streckenbesichtigung zu Fuss und es wird mit einem einzigen Durchgang gewertet",
        answerC = "Der Super-G kombiniert Abfahrtsskis mit Riesenslalom-Toren und zwei Laeufen",
        answerD = "Im Super-G sind engere Tore als im Riesenslalom vorgeschrieben",
        correctAnswer = 1,
        explanation = "Im Super-G (Super-Riesenslalom) duerfen die Athleten die Strecke nicht zu Fuss befahren oder inspizieren — nur eine einzige Pistenbefahrung ist erlaubt. Zudem gibt es nur einen einzigen Wertungslauf. Das macht den Super-G zur technisch anspruchsvollsten Speed-Disziplin, da Gedaechtnis und Reaktion entscheidend sind.",
        difficulty = 3,
        funFact = "Die Mindestanzahl an Toren im Super-G betraegt bei Herren 35 und bei Damen 30. Die Durchschnittsgeschwindigkeit liegt bei 100-110 km/h, etwas weniger als in der Abfahrt."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist der 'V-Stil' im Skispringen, und wer hat ihn im Weltcup populaer gemacht?",
        answerA = "Eine Flugtechnik bei der die Skier parallel gehalten werden, eingefuehrt von Matti Nykänen",
        answerB = "Eine V-foermige Ski-Position die mehr Auftrieb erzeugt, populaer gemacht von Jan Boklöv in den 1980ern",
        answerC = "Eine Absprungtechnik mit gespreizten Beinen, entwickelt von Gregor Schlierenzauer",
        answerD = "Eine Landungsposition bei der die Skier V-foermig abgebremst werden",
        correctAnswer = 1,
        explanation = "Der V-Stil bezeichnet die V-foermige Haltung beider Skier beim Flug. Jan Boklöv (Schweden) machte ihn ab 1985 populaer und gewann damit 1988/89 den Weltcup-Gesamttitel. Der V-Stil erzeugt mehr aerodynamischen Auftrieb als der klassische Parallelstil und ist heute der weltweite Standard.",
        difficulty = 3,
        funFact = "Vor dem V-Stil nutzten alle Springer den Parallelstil. Obwohl Boklöv den V-Stil nicht erfand — erste Experimente gab es bereits in den 1950ern — war er der Erste, der ihn konsequent im Wettkampf einsetzte."
    ),

    // ── Radsport / Ausdauer ───────────────────────────────────────────────────

    Question(
        categoryId = 6,
        questionText = "Was versteht man im Ausdauersport unter dem Begriff 'VO2max' und welcher Wert gilt bei Spitzenathleten als aussergewoehnlich hoch?",
        answerA = "Die maximale Herzfrequenz in Schlaegen pro Minute; Spitzenwert liegt bei 220 bpm",
        answerB = "Die maximale Sauerstoffaufnahme in ml/kg/min; Spitzenathleten erreichen Werte ueber 80 ml/kg/min",
        answerC = "Das maximale Blutlaktat in mmol/l; Topathleten tolerieren bis zu 20 mmol/l",
        answerD = "Die maximale Atemfrequenz in Zuegen pro Minute; Elite-Laeufer erreichen 60 Zuege/min",
        correctAnswer = 1,
        explanation = "VO2max bezeichnet die maximale Sauerstoffaufnahmekapazitaet des Koerpers in Milliliter pro Kilogramm Koerpergewicht pro Minute (ml/kg/min). Der Normalwert liegt bei trainierten Maennern bei 50-60. Ausnahmeathlet Oskar Svendsen (Norwegen) wurde 2012 ein Wert von 97,5 ml/kg/min gemessen — der hoechste je dokumentierte Wert.",
        difficulty = 3,
        funFact = "Eliud Kipchoge, der Weltrekordhalter im Marathon, hat ein VO2max von etwa 85 ml/kg/min. Der Wert allein entscheidet nicht — Laufoekonomie und mentale Staerke sind ebenso entscheidend."
    )

)
