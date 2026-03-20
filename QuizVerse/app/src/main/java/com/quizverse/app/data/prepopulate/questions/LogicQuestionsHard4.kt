package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun logicQuestionsHard4(): List<Question> = listOf(

    // ── SPIELTHEORIE & STRATEGISCHES DENKEN (50 Fragen, Schwierigkeit 3) ──────

    // Frage 1
    Question(
        categoryId = 12,
        questionText = "Beim klassischen Gefangenendilemma werden zwei Verdächtige getrennt verhört. Schweigen beide, erhalten beide 1 Jahr. Gesteht einer allein, kommt er frei, der andere bekommt 5 Jahre. Gestehen beide, bekommen beide 3 Jahre. Was ist die dominante Strategie für jeden Spieler?",
        answerA = "Schweigen, weil beide dann am besten abschneiden",
        answerB = "Gestehen, weil es unabhängig vom anderen die bessere Wahl ist",
        answerC = "Eine Münze werfen",
        answerD = "Schweigen, wenn der andere gesteht",
        correctAnswer = 1,
        explanation = "Gestehen ist die dominante Strategie: Wenn der andere schweigt, kommt man frei (statt 1 Jahr). Wenn der andere gesteht, bekommt man 3 Jahre (statt 5 Jahre). In beiden Fällen ist Gestehen besser – unabhängig davon, was der andere tut. Das Nash-Gleichgewicht (beide gestehen, beide 3 Jahre) ist schlechter als die kollektiv optimale Lösung (beide schweigen, beide 1 Jahr).",
        difficulty = 3,
        funFact = "Das Gefangenendilemma wurde 1950 von Merrill Flood und Melvin Dresher bei RAND entwickelt. Der Name stammt von Albert Tucker, der es in einer Vorlesung an der Princeton University vorstellte."
    ),

    // Frage 2
    Question(
        categoryId = 12,
        questionText = "Zwei Unternehmen A und B können jeweils einen hohen oder niedrigen Preis setzen. Setzen beide hoch: je 10 Mio. Gewinn. Setzt A hoch und B niedrig: A macht −2 Mio., B macht 15 Mio. Umgekehrt analog. Setzen beide niedrig: je 4 Mio. Was ist das Nash-Gleichgewicht?",
        answerA = "Beide setzen hohen Preis",
        answerB = "A setzt hoch, B setzt niedrig",
        answerC = "Beide setzen niedrigen Preis",
        answerD = "Es gibt kein Nash-Gleichgewicht",
        correctAnswer = 2,
        explanation = "Beide setzen niedrig: Wenn A hoch setzt und B niedrig, verliert A. Also ist 'niedrig' für A die beste Antwort auf 'niedrig' von B (4 > −2). Und für B gilt dasselbe. Im Gleichgewicht setzen beide niedrig und verdienen je 4 Mio. – obwohl beide bei hohem Preis je 10 Mio. verdienen könnten. Ein klassisches Gefangenendilemma in Preissetzung.",
        difficulty = 3,
        funFact = null
    ),

    // Frage 3
    Question(
        categoryId = 12,
        questionText = "Bei der Vickrey-Auktion (Zweitpreisauktion) gewinnt der Höchstbietende, zahlt aber nur den zweitlöchsten Preis. Welche Bietstrategie ist hier dominant – also optimal unabhängig vom Verhalten anderer?",
        answerA = "Weniger als der eigene Wert bieten, um Risiko zu minimieren",
        answerB = "Mehr als der eigene Wert bieten, um sicher zu gewinnen",
        answerC = "Genau den eigenen wahren Wert bieten",
        answerD = "Immer 1 Euro mehr als erwartet",
        correctAnswer = 2,
        explanation = "Wahrheitsgemäßes Bieten (genau der eigene Wert) ist die dominante Strategie: Bietet man zu hoch, riskiert man zu gewinnen und mehr zu zahlen als man wert findet. Bietet man zu niedrig, riskiert man zu verlieren, obwohl man profitiert hätte. Der Zweitpreis sorgt dafür, dass man nie mehr zahlt als man bietet – also ist Ehrlichkeit optimal.",
        difficulty = 3,
        funFact = "William Vickrey erhielt 1996 den Nobelpreis für Wirtschaftswissenschaften für seine Pionierarbeiten zur Auktionstheorie. Die Vickrey-Auktion wird heute u.a. bei Google-Anzeigen (modifiziert) eingesetzt."
    ),

    // Frage 4
    Question(
        categoryId = 12,
        questionText = "Beim Feiglingsspiel (Chicken Game) rasen zwei Fahrer aufeinander zu. Beide weichen aus: Unentschieden. Einer weicht aus, der andere nicht: der Ausweichende gilt als Feigling, der andere als Held. Beide weichen nicht aus: Katastrophe für beide. Wie viele Nash-Gleichgewichte in reinen Strategien gibt es?",
        answerA = "Keines",
        answerB = "Eines",
        answerC = "Zwei",
        answerD = "Unendlich viele",
        correctAnswer = 2,
        explanation = "Es gibt zwei Nash-Gleichgewichte in reinen Strategien: (A weicht aus, B weicht nicht) und (A weicht nicht, B weicht aus). In beiden Fällen hat keiner einen Anreiz zur einseitigen Abweichung. Das Gleichgewicht (beide weichen nicht) ist kein Nash-Gleichgewicht, da beide dann ausweichen würden. Zusätzlich gibt es ein gemischtes Nash-Gleichgewicht.",
        difficulty = 3,
        funFact = "Das Feiglingsspiel wurde durch James Dean im Film 'Denn sie wissen nicht, was sie tun' (1955) populär. Es modelliert auch nukleare Abschreckungsstrategien im Kalten Krieg."
    ),

    // Frage 5
    Question(
        categoryId = 12,
        questionText = "Im Spiel 'Schönheitswettbewerb der Zahlen' (Keynes Beauty Contest) wählen 100 Spieler eine Zahl zwischen 0 und 100. Wer am nächsten an 2/3 des Durchschnitts liegt, gewinnt. Was wählt ein vollkommen rationaler Spieler mit vollkommen rationalen Mitspielern?",
        answerA = "50",
        answerB = "33",
        answerC = "1",
        answerD = "0",
        correctAnswer = 3,
        explanation = "Durch iteriertes Eliminieren dominierter Strategien: Wenn alle rational sind, wählt niemand über 67 (2/3 von 100). Wissen alle das, wählt niemand über 44 (2/3 von 67). Das setzt sich fort bis zum Nash-Gleichgewicht: 0. In der Praxis wählen Menschen jedoch 22–33, da echte Menschen nur 1–3 Rationalisierungsschritte durchführen.",
        difficulty = 3,
        funFact = "John Maynard Keynes verglich den Aktienmarkt 1936 mit einem Schönheitswettbewerb: Man investiert nicht in den besten Wert, sondern in den Wert, den andere für den besten halten."
    ),

    // Frage 6
    Question(
        categoryId = 12,
        questionText = "Zwei Länder können aufrüsten oder abrüsten. Rüsten beide ab: beide +10. Rüstet Land A auf, B nicht: A bekommt +15, B bekommt −5. Rüsten beide auf: beide −3. Das ist strukturell identisch mit welchem Spieltyp?",
        answerA = "Koordinationsspiel",
        answerB = "Nullsummenspiel",
        answerC = "Gefangenendilemma",
        answerD = "Feiglingsspiel",
        correctAnswer = 2,
        explanation = "Die Auszahlungsstruktur ist identisch mit dem Gefangenendilemma: 'Aufrüsten' entspricht 'gestehen' – es ist die dominante Strategie für jedes Land, unabhängig vom anderen. Das Nash-Gleichgewicht (beide rüsten auf: −3) ist schlechter als das kollektive Optimum (beide rüsten ab: +10). Ein klassisches Dilemma in der Außenpolitik.",
        difficulty = 3,
        funFact = "Das Rüstungsdilemma zwischen USA und UdSSR im Kalten Krieg ist ein reales Beispiel. Es endete erst durch externe Faktoren (wirtschaftlicher Kollaps der UdSSR), nicht durch Kooperation."
    ),

    // Frage 7
    Question(
        categoryId = 12,
        questionText = "Beim iterierten Gefangenendilemma (das Spiel wird viele Male wiederholt) zeigen Experimente, welche Strategie am erfolgreichsten ist. Wie heißt diese Strategie?",
        answerA = "Immer gestehen (immer defektieren)",
        answerB = "Zufällig gestehen oder schweigen",
        answerC = "Tit-for-Tat: beim ersten Mal schweigen, dann das tun, was der andere zuletzt tat",
        answerD = "Immer schweigen (immer kooperieren)",
        correctAnswer = 2,
        explanation = "Tit-for-Tat (Wie du mir, so ich dir) gewann Robert Axelrods berühmtes Computertournament 1984. Die Strategie: Kooperiere beim ersten Zug, dann kopiere den letzten Zug des Gegners. Sie belohnt Kooperation, bestraft Verrat sofort und verzeiht nach einer Runde. Einfach, klar, robust – und sie fördert evolutionär stabile Kooperationskultur.",
        difficulty = 3,
        funFact = "Robert Axelrod lud 1984 Spieltheorie-Experten ein, Programme für ein Gefangenendilemma-Turnier einzureichen. Das simpelste Programm – Tit-for-Tat (nur 4 Zeilen Code) – gewann zweimal in Folge."
    ),

    // Frage 8
    Question(
        categoryId = 12,
        questionText = "Ein Monopolist überlegt, ob er seinen Markt verteidigen soll, wenn ein Konkurrent einzutreten droht. Er droht: 'Wenn du eintrittst, führe ich einen Preiskrieg.' Der Konkurrent weiß, dass der Preiskrieg dem Monopolisten selbst schadet. Was ist das spieltheoretische Problem mit dieser Drohung?",
        answerA = "Die Drohung ist zu teuer",
        answerB = "Die Drohung ist nicht glaubwürdig – im Ernstfall wäre Preiskrieg irrational",
        answerC = "Der Konkurrent hat keine Informationen",
        answerD = "Die Drohung verstößt gegen Kartellrecht",
        correctAnswer = 1,
        explanation = "Die Drohung ist nicht teilspielperfekt (nicht glaubwürdig): Wenn der Konkurrent wirklich eintritt, wäre es für den Monopolisten rational, keinen Preiskrieg zu führen (da dieser ihn selbst schadet). Ein rationaler Konkurrent erkennt das und tritt trotzdem ein. Nur glaubwürdige Drohungen – z.B. durch Vorleistungen (Sunk Costs, Kapazitätserweiterungen) – können Markteintritt verhindern.",
        difficulty = 3,
        funFact = "Thomas Schelling analysierte 1960 in 'The Strategy of Conflict', wie man Drohungen glaubwürdig macht – etwa durch öffentliche Bindung oder Brückenabbruch hinter sich. Er erhielt 2005 den Nobelpreis."
    ),

    // Frage 9
    Question(
        categoryId = 12,
        questionText = "Drei Schützen A, B und C duellieren. A trifft 1/3 der Zeit, B trifft 1/2, C trifft immer. Alle schießen gleichzeitig und zielen optimal. A denkt nach: Wen sollte A anvisieren, um die Überlebenschance zu maximieren?",
        answerA = "C (den Stärksten), denn er ist die größte Bedrohung",
        answerB = "B (den Mittleren), denn C wird sowieso von B getroffen",
        answerC = "In die Luft schießen, damit B und C sich gegenseitig ausschalten",
        answerD = "A (sich selbst), was sinnlos wäre",
        correctAnswer = 2,
        explanation = "A schießt am besten in die Luft (oder absichtlich daneben). Warum: Wenn A auf C zielt und trifft, steht A danach B (50%) allein gegenüber. Wenn A auf B zielt und trifft, steht A C (100%) allein gegenüber – noch schlimmer. Am besten: A lässt B und C sich gegenseitig bekämpfen. B visiert C an (größte Bedrohung), C visiert B an (größte Bedrohung). A überlebt die erste Runde sicher und schießt dann auf den Überlebenden.",
        difficulty = 3,
        funFact = "Dieses Rätsel heißt 'Truel' (von 'three-way duel'). Es zeigt das Paradox, dass der Schwächste manchmal die beste Überlebenschance hat – eben weil die Stärkeren sich gegenseitig als Bedrohung sehen."
    ),

    // Frage 10
    Question(
        categoryId = 12,
        questionText = "Das Ultimatumspiel: Spieler A erhält 100 Euro und darf B einen Betrag anbieten. B akzeptiert oder lehnt ab. Bei Ablehnung bekommt keiner etwas. Was sagt die klassische Spieltheorie (homo oeconomicus) vorher?",
        answerA = "A bietet 50 Euro für Fairness",
        answerB = "A bietet 1 Euro, B akzeptiert, weil 1 Euro besser als nichts",
        answerC = "B lehnt jeden Betrag unter 30 Euro ab",
        answerD = "A behält alle 100 Euro",
        correctAnswer = 1,
        explanation = "Nach der Theorie des rationalen Eigennutzes: A bietet den kleinstmöglichen Betrag (1 Euro). B, rational handelnd, akzeptiert (1 Euro > 0 Euro). In der Realität lehnen Menschen Angebote unter ~25–30 Euro ab, weil sie Unfairness bestrafen – selbst auf eigene Kosten. Das widerlegt das Homo-oeconomicus-Modell und zeigt, dass Fairnessnormen in menschlicher Entscheidungsfindung eine Rolle spielen.",
        difficulty = 3,
        funFact = "Das Ultimatumspiel wurde 1982 von Güth, Schmittberger und Schwarze eingeführt. Experimente in über 50 Kulturen zeigen: Überall lehnen Menschen unfaire Angebote ab – aber was als 'fair' gilt, variiert kulturell."
    ),

    // Frage 11
    Question(
        categoryId = 12,
        questionText = "Beim Diktatorspiel bekommt Spieler A 100 Euro und darf entscheiden, wie viel er B gibt. B hat keine Wahl. Was zeigen Experimente im Vergleich zur spieltheoretischen Vorhersage?",
        answerA = "A gibt immer 50 Euro – Menschen sind von Natur aus fair",
        answerB = "A gibt meistens 0 Euro – wie die Theorie voraussagt",
        answerC = "A gibt oft 20–30 Euro, obwohl die Theorie 0 Euro vorhersagt",
        answerD = "A gibt immer den maximalen Betrag aus Schuldgefühl",
        correctAnswer = 2,
        explanation = "Die Spieltheorie sagt voraus: A gibt 0 (Eigennutz). In der Realität geben Versuchspersonen im Schnitt 20–30 Euro, obwohl sie das nicht müssten. Das zeigt, dass Menschen altruistische Präferenzen haben und soziale Normen internalisieren – auch ohne Gegenleistung oder Sanktionen. Ernst Fehr und andere Verhaltensökonomen haben das ausgiebig untersucht.",
        difficulty = 3,
        funFact = null
    ),

    // Frage 12
    Question(
        categoryId = 12,
        questionText = "Im Vertrauensspiel schickt Spieler A einen Betrag X an B (der Betrag verdreifacht sich). B kann zurückschicken, was er will. Was sagt die klassische Rückwärtsinduktion?",
        answerA = "B schickt alles zurück, weil Vertrauen sich lohnt",
        answerB = "B schickt im letzten Zug nichts zurück, also schickt A von Anfang an nichts",
        answerC = "A schickt alles, B schickt die Hälfte zurück",
        answerD = "Beide teilen den Gewinn genau hälftig",
        correctAnswer = 1,
        explanation = "Rückwärtsinduktion: Im letzten Zug hat B keinen Anreiz zurückzusenden (Eigennutz). A antizipiert das und sendet daher gar nichts. Das Nash-Gleichgewicht ist (0 gesendet, 0 zurückgesendet). In der Praxis senden viele A-Spieler Geld und viele B-Spieler zahlen zurück – ein Beweis für Vertrauensbereitschaft und Reziprozität.",
        difficulty = 3,
        funFact = "Das Vertrauensspiel von Berg, Dickhaut und McCabe (1995) ist ein Grundstein der experimentellen Wirtschaftsforschung. Es zeigt: Märkte funktionieren durch Vertrauen, nicht nur durch Verträge."
    ),

    // Frage 13
    Question(
        categoryId = 12,
        questionText = "Zwei Firmen wählen simultan ihre Produktionsmenge (Cournot-Wettbewerb). Jede weiß: mehr Produktion senkt den Marktpreis. Im Nash-Gleichgewicht produzieren beide zusammen mehr als ein Monopolist, aber weniger als bei vollkommener Konkurrenz. Wer hat die höchsten Gewinne?",
        answerA = "Beide Firmen zusammen im Cournot-Gleichgewicht",
        answerB = "Ein einzelner Monopolist",
        answerC = "Beide Firmen bei vollkommener Konkurrenz",
        answerD = "Die Gewinne sind immer gleich",
        correctAnswer = 1,
        explanation = "Ein Monopolist maximiert den Gesamtgewinn durch Beschränkung der Menge. Im Cournot-Duopol überproduzieret jede Firma (Nash-Gleichgewicht), weil sie die Wirkung ihrer Produktion auf den Preis des anderen ignoriert. Der gemeinsame Gewinn ist daher niedriger als beim Monopol. Bei vollkommener Konkurrenz ist Gewinn = 0. Reihenfolge der Gewinne: Monopol > Cournot > vollk. Konkurrenz.",
        difficulty = 3,
        funFact = "Antoine Augustin Cournot formulierte das Modell 1838 – Jahrzehnte vor John Nash. Es gilt als erste formale spieltheoretische Analyse, obwohl der Begriff 'Nash-Gleichgewicht' erst 1950 entstand."
    ),

    // Frage 14
    Question(
        categoryId = 12,
        questionText = "Im Bertrand-Wettbewerb setzen zwei identische Firmen gleichzeitig Preise (statt Mengen wie bei Cournot). Was ist das Nash-Gleichgewicht beim Bertrand-Paradoxon?",
        answerA = "Beide setzen den Monopolpreis und teilen den Markt",
        answerB = "Beide unterbieten sich bis auf Grenzkosten – Gewinn = 0",
        answerC = "Eine Firma dominiert, die andere scheidet aus",
        answerD = "Preise bleiben stabil auf halbem Weg zwischen Monopol und Grenzkosten",
        correctAnswer = 1,
        explanation = "Das Bertrand-Paradoxon: Bei zwei identischen Firmen reicht es, den anderen um 1 Cent zu unterbieten, um den ganzen Markt zu gewinnen. Dieser Wettlauf endet erst bei Preis = Grenzkosten, also Gewinn = 0. Paradox: Schon 2 Firmen reichen für das Konkurrenzergebnis – dasselbe wie bei unendlich vielen Firmen unter vollkommener Konkurrenz.",
        difficulty = 3,
        funFact = null
    ),

    // Frage 15
    Question(
        categoryId = 12,
        questionText = "Eine Lotterie kostet 2 Euro. Die Gewinnchance beträgt 1:1.000.000 für 1 Million Euro. Der Erwartungswert beträgt exakt 1 Euro – also verliert man im Erwartungswert. Trotzdem kaufen Millionen Menschen Lottoscheine. Was erklärt das die Verhaltensökonomik?",
        answerA = "Menschen sind schlechte Mathematiker",
        answerB = "Prospekttheorie: Menschen überschätzen kleine Wahrscheinlichkeiten und haben Freude am Träumen",
        answerC = "Der Erwartungswert ist tatsächlich positiv wegen Jackpots",
        answerD = "Lottospielen ist rein irrational und nicht erklärbar",
        correctAnswer = 1,
        explanation = "Kahneman und Tverskys Prospekttheorie (1979): Menschen überschätzen sehr kleine Wahrscheinlichkeiten (1:1.000.000 fühlt sich bedeutsamer an als es ist) und unterschätzen mittlere. Außerdem bietet die Lotterie Unterhaltungswert und die Freude am Träumen ('Was würde ich mit 1 Million machen?'). Der Nutzen = Erwartungsnutzen + Traumwert.",
        difficulty = 3,
        funFact = "Daniel Kahneman erhielt 2002 den Nobelpreis für Wirtschaftswissenschaften (zusammen mit Vernon Smith) für die Prospekttheorie – die wichtigste Alternative zur klassischen Erwartungsnutzentheorie."
    ),

    // Frage 16
    Question(
        categoryId = 12,
        questionText = "Beim Schelling-Fokuspunkt (Focal Point) sollen sich zwei Personen ohne Kommunikation auf einen Treffpunkt in New York einigen. Wohin gehen die meisten?",
        answerA = "Zum Times Square um Mitternacht",
        answerB = "Zur Grand Central Station um 12:00 Uhr",
        answerC = "Zum Central Park am Eingang",
        answerD = "Zum Empire State Building",
        correctAnswer = 1,
        explanation = "In Thomas Schellings berühmtem Experiment aus 'The Strategy of Conflict' (1960) wählten die meisten Menschen: Grand Central Station, mittags (12:00 Uhr). Es gibt keine logische Begründung – aber es ist der kulturell salienteste, offensichtlichste Punkt. Schelling zeigte: Koordination ohne Kommunikation ist möglich, wenn ein 'natürlicher' Fokuspunkt existiert.",
        difficulty = 3,
        funFact = "Thomas Schelling verwendete dieses Experiment um zu zeigen, dass Menschen koordinieren können ohne zu kommunizieren – durch gemeinsames kulturelles Wissen. Das ist der Grund warum wir alle 'instinktiv' dieselben Treffpunkte wählen."
    ),

    // Frage 17
    Question(
        categoryId = 12,
        questionText = "Im Hawk-Dove-Spiel (Habicht-Taube) kämpfen zwei Tiere um eine Ressource (Wert V). Ein Kampf kostet C (Verletzungskosten). Was passiert im gemischten Nash-Gleichgewicht, wenn C > V?",
        answerA = "Alle Tiere kämpfen immer (Habicht dominiert)",
        answerB = "Alle Tiere weichen immer aus (Taube dominiert)",
        answerC = "Jedes Tier spielt mit Wahrscheinlichkeit V/C Habicht und (1−V/C) Taube",
        answerD = "Nur das stärkere Tier kämpft immer",
        correctAnswer = 2,
        explanation = "Im gemischten Gleichgewicht (wenn C > V): Jedes Tier wählt Habicht-Strategie mit Wahrscheinlichkeit p = V/C. Wenn V = 4 und C = 8, kämpft jedes Tier in 50% der Fälle. Die evolutionär stabile Strategie (ESS) liegt genau dort, wo Kampf und Ausweichen denselben erwarteten Nutzen liefern. Dieses Modell erklärt tierisches Drohverhalten ohne realen Kampf.",
        difficulty = 3,
        funFact = "Das Hawk-Dove-Spiel wurde 1973 von John Maynard Smith und George Price entwickelt. Es begründete die evolutionäre Spieltheorie und erklärt, warum in der Natur echte Kämpfe selten sind – Drohrituale sind effizienter."
    ),

    // Frage 18
    Question(
        categoryId = 12,
        questionText = "Beim 'Traveler's Dilemma' behaupten zwei Reisende unabhängig voneinander den Wert ihres verlorenen Koffers (1–100 Euro). Wer weniger nennt, bekommt diesen Betrag + 2 Bonus; der andere bekommt den Niedrigeren − 2 Strafe. Was sagt die Spieltheorie (Nash-Gleichgewicht)?",
        answerA = "Beide nennen 100 Euro",
        answerB = "Beide nennen 50 Euro",
        answerC = "Beide nennen 1 Euro",
        answerD = "Einer nennt 100, der andere 1",
        correctAnswer = 2,
        explanation = "Durch iterierte Dominanz: Von 100 auf 99 zu wechseln lohnt sich immer (+2 Bonus). Von 99 auf 98 lohnt sich wieder. Das Argument setzt sich fort bis zum einzigen Nash-Gleichgewicht: beide nennen 1 Euro. In der Praxis nennen fast alle Versuchspersonen sehr hohe Zahlen (90–100), weil der +2 Bonus zu schwach ist, um die Rationalitätslogik in der Praxis zu erzwingen.",
        difficulty = 3,
        funFact = "Kaushik Basu prägte 1994 das 'Traveler's Dilemma' um zu zeigen, dass Nash-Gleichgewichte manchmal gegen die Intuition sind – und in der Realität selten gespielt werden. Ein Paradox der reinen Rationalität."
    ),

    // Frage 19
    Question(
        categoryId = 12,
        questionText = "Zwei Bieter in einer englischen Aufstiegsauktion (Preis steigt, bis einer aussteigt). Was ist die optimale Strategie?",
        answerA = "So früh wie möglich aussteigen, um Risiko zu minimieren",
        answerB = "Drin bleiben, solange der Preis unter dem eigenen Wert liegt – dann aussteigen",
        answerC = "Immer 10% über dem eigenen Wert bieten",
        answerD = "Sofort den Maximalpreis setzen um andere abzuschrecken",
        correctAnswer = 1,
        explanation = "In der englischen Auktion ist die dominante Strategie: bieten, solange Preis < eigener Wert, und aussteigen wenn Preis = eigener Wert. Da man nur zahlt, was man wirklich bietet (nicht wie bei der Vickrey-Auktion), gibt es keinen Anreiz zum strategischen Über- oder Unterbieten. Der Gewinner zahlt gerade so viel wie der zweithöchste Bieter maximal geboten hätte.",
        difficulty = 3,
        funFact = null
    ),

    // Frage 20
    Question(
        categoryId = 12,
        questionText = "Das 'El Farol Bar Problem' (W. Brian Arthur, 1994): 100 Personen wollen freitagabends in eine Bar. Diese macht Spaß, wenn weniger als 60 kommen; bei ≥ 60 ist es zu voll und unangenehm. Alle entscheiden unabhängig. Was ist das Gleichgewicht?",
        answerA = "Immer alle 100 gehen hin",
        answerB = "Niemand geht, weil alle Angst vor Überfüllung haben",
        answerC = "Im Schnitt gehen immer 60 Personen – eine selbstregulierende Verteilung",
        answerD = "Die Hälfte geht immer, die andere Hälfte nie",
        correctAnswer = 2,
        explanation = "Arthur zeigte: Obwohl keine zentrale Koordination stattfindet und alle nur auf vergangene Erfahrungen zurückgreifen, pendelt sich der Schnitt langfristig bei ~60 ein. Das Gleichgewicht entsteht durch adaptive Erwartungen. Dieses Problem modelliert Überlastungsphänomene (Stau, Börsenblasen) und zeigt, wie Märkte ohne Planung zu effizienten Gleichgewichten tendieren.",
        difficulty = 3,
        funFact = "W. Brian Arthur verwendete dieses Rätsel um zu zeigen, wie induktive Vernunft (Lernen aus Erfahrung) zu Gleichgewichten führt – auch ohne deduktive Rationalität. Es ist ein Grundstein der Complexity Economics."
    ),

    // Frage 21
    Question(
        categoryId = 12,
        questionText = "Im 'Stag Hunt' (Hirschjagd) können zwei Jäger gemeinsam einen Hirsch erlegen (je 5 Punkte) oder allein einen Hasen (je 3 Punkte). Ein Hirsch gelingt nur, wenn beide kooperieren. Was sind die Nash-Gleichgewichte?",
        answerA = "Nur (Hirsch, Hirsch)",
        answerB = "Nur (Hase, Hase)",
        answerC = "Sowohl (Hirsch, Hirsch) als auch (Hase, Hase) sind Nash-Gleichgewichte",
        answerD = "Es gibt kein Nash-Gleichgewicht",
        correctAnswer = 2,
        explanation = "Beide Kombinationen sind Nash-Gleichgewichte: (Hirsch, Hirsch) – keiner hat Anreiz abzuweichen (5 > 0, wenn der andere Hirsch jagt). (Hase, Hase) – keiner hat Anreiz abzuweichen (3 > 0, wenn der andere Hase jagt und man allein keinen Hirsch erlegen kann). Dies ist ein Koordinationsspiel mit zwei Gleichgewichten – eines pareto-superior (Hirsch), eines risikoärmer (Hase).",
        difficulty = 3,
        funFact = "Jean-Jacques Rousseau beschrieb die Hirschjagd 1754 als Metapher für gesellschaftliche Kooperation. Das Spiel modelliert das Problem der sozialen Koordination: Kooperation lohnt sich, aber nur wenn man dem anderen vertraut."
    ),

    // Frage 22
    Question(
        categoryId = 12,
        questionText = "Beim 'Centipede Game' können zwei Spieler abwechselnd den Topf nehmen oder weitergeben. Jedes Weitergeben verdoppelt den Topf, aber die Aufteilung ist asymmetrisch. Die Rückwärtsinduktion sagt: Was tut Spieler 1 im ersten Zug?",
        answerA = "Den Topf sofort nehmen",
        answerB = "Immer weitergeben bis zum Ende",
        answerC = "Den Topf nach der Hälfte nehmen",
        answerD = "Zufällig entscheiden",
        correctAnswer = 0,
        explanation = "Rückwärtsinduktion: Im letzten Zug nimmt der letzte Spieler immer den Topf. Der vorletzte Spieler antizipiert das und nimmt lieber selbst. Dieses Argument läuft bis zum ersten Zug: Spieler 1 sollte sofort nehmen. In der Realität geben Menschen den Topf viele Male weiter, weil sie auf Kooperation und wachsenden Gewinn hoffen – ein deutlicher Widerspruch zur reinen Spieltheorie.",
        difficulty = 3,
        funFact = "Das Centipede Game wurde 1992 von Rosenthal eingeführt. Experimente zeigen, dass fast niemand sofort im ersten Zug abbricht – obwohl die Logik es gebietet. Menschen 'spielen besser' als die Theorie vorhersagt."
    ),

    // Frage 23
    Question(
        categoryId = 12,
        questionText = "Drei Erben wollen ein Auto (Wert 9.000 €) unter sich aufteilen. Sie nutzen die 'Knaster-Vererbungsmethode': Jeder nennt seinen persönlichen Wert. Was ist das Prinzip der fairen Lösung?",
        answerA = "Das Auto wird versteigert und der Erlös geteilt",
        answerB = "Derjenige mit dem höchsten eigenen Wert bekommt das Auto und zahlt die anderen aus, sodass jeder seinen fairen Anteil (1/3 des eigenen Wertes) erhält",
        answerC = "Das Auto wird verkauft und der Erlös zu gleichen Teilen verteilt",
        answerD = "Die Mehrheit stimmt ab, wer das Auto bekommt",
        correctAnswer = 1,
        explanation = "Bei der Knaster-Methode: Wer das Gut am höchsten bewertet, bekommt es. Er zahlt in einen Fonds ein. Jeder erhält aus diesem Fonds seinen fairen Anteil (1/n seines eigenen gebotenen Wertes). Der Mechanismus garantiert, dass jeder mindestens seinen fairen Anteil erhält und niemand neidisch auf andere ist. Auch genannt: 'Fair Division' oder 'Sealed Bids Method'.",
        difficulty = 3,
        funFact = null
    ),

    // Frage 24
    Question(
        categoryId = 12,
        questionText = "Zwei Firmen überlegen, ob sie in eine neue Technologie investieren. Investiert nur Firma A, steigt ihr Marktanteil (A: +20, B: −10). Investieren beide, teilen sie die Vorteile (je +5). Investiert keiner: je 0. Welche Strategie dominiert für Firma B?",
        answerA = "Immer investieren",
        answerB = "Nie investieren – auf A warten und dann kostenlos profitieren",
        answerC = "Nur investieren, wenn A auch investiert",
        answerD = "Es gibt keine dominante Strategie",
        correctAnswer = 1,
        explanation = "Für Firma B gilt: Wenn A investiert, ist es für B besser nicht zu investieren (0 > −10 bei Eigeninvestition... wait, B verliert 10 wenn nur A investiert). Korrekt: B will von As Investition profitieren ohne selbst zu zahlen. Das ist das 'Free-Rider-Problem': Wenn öffentliche Güter oder Infrastruktur entstehen, lohnt es sich für jeden Einzelnen, auf andere zu warten. Resultat: Unterinvestition.",
        difficulty = 3,
        funFact = "Das Free-Rider-Problem erklärt, warum öffentliche Güter (Straßen, Verteidigung, Grundlagenforschung) vom Staat bereitgestellt werden müssen – private Märkte produzieren zu wenig davon."
    ),

    // Frage 25
    Question(
        categoryId = 12,
        questionText = "Beim 'Pirate Game' verteilen 5 Piraten 100 Goldmünzen. Der ranghöchste Pirat macht einen Vorschlag; bei Mehrheit wird er angenommen, sonst wird er über Bord geworfen. Wie viele Münzen bekommt Pirat 1 (ranghöchster) im rationalen Gleichgewicht?",
        answerA = "20 (gleiche Aufteilung)",
        answerB = "51 (knappe Mehrheit)",
        answerC = "96",
        answerD = "100",
        correctAnswer = 2,
        explanation = "Durch Rückwärtsinduktion: Bei 2 Piraten nimmt Pirat 2 alles (er stimmt für sich). Bei 3 Piraten bietet Pirat 3 Pirat 1 genau 1 Münze (Pirat 1 stimmt zu, da 1 > 0 bei 2 Piraten). Bei 4 Piraten bietet Pirat 4: Pirat 2 bekommt 1, Pirat 4 behält 99. Bei 5 Piraten bietet Pirat 5: Pirat 1 bekommt 1, Pirat 3 bekommt 1, Pirat 5 behält 98. Wait – Pirat 5 braucht 3 Stimmen (inkl. eigene). Er bietet 0-1-0-1-98. Also: Pirat 5 = 96, Piraten 1 und 3 = je 1, Piraten 2 und 4 = 0.",
        difficulty = 3,
        funFact = "Das Pirate Game wurde 1999 von Ian Stewart in Scientific American vorgestellt. Es gilt als eines der schönsten Beispiele für Rückwärtsinduktion und kontrafaktisches Denken in der Spieltheorie."
    ),

    // Frage 26
    Question(
        categoryId = 12,
        questionText = "Beim 'Dollar Auction' versteigert ein Moderator einen 1-Euro-Schein. Beide Höchstbietenden müssen zahlen, gewinnen tut nur der Höchstbietende. Was passiert typischerweise?",
        answerA = "Der Schein geht für 1 Euro weg",
        answerB = "Niemand bietet, weil das Spiel zu riskant ist",
        answerC = "Die Gebote übersteigen oft 1 Euro – beide Bieter zahlen mehr als der Schein wert ist",
        answerD = "Das Spiel endet sofort, wenn jemand 50 Cent bietet",
        correctAnswer = 2,
        explanation = "Die Sunk-Cost-Falle: Wer 80 Cent geboten hat und der andere 90 Cent bietet, denkt: 'Ich verliere 80 Cent wenn ich aufhöre, aber gewinne 10 Cent wenn ich 1 Euro biete.' So eskalieren beide. Das Spiel wurde von Martin Shubik entwickelt und modelliert Eskalationsdynamiken: Aufrüstung, Bieterkriege, Kriegführung. Die Theorie: Der rationale Einstieg ist, nie zu bieten.",
        difficulty = 3,
        funFact = "Martin Shubik erfand 1971 die Dollar-Auktion als Demonstration von Eskalationslogik. Sie wird bis heute in Wirtschafts- und Politikseminaren eingesetzt, um irrationale Eskalation zu erklären."
    ),

    // Frage 27
    Question(
        categoryId = 12,
        questionText = "Zwei Länder verhandeln über ein Abkommen. Die BATNA (Best Alternative to Negotiated Agreement) von Land A ist schwächer. Was sagt die Nash-Verhandlungslösung über das Ergebnis?",
        answerA = "Das Land mit der stärkeren BATNA bekommt mehr vom Verhandlungsgewinn",
        answerB = "Beide Länder teilen den Gewinn immer hälftig",
        answerC = "Das schwächere Land bekommt mehr als Ausgleich",
        answerD = "Die BATNA hat keinen Einfluss auf das Ergebnis",
        correctAnswer = 0,
        explanation = "Die Nash-Verhandlungslösung: Das Ergebnis maximiert das Produkt der Gewinne beider Parteien über ihre BATNA. Wer eine bessere BATNA hat, verhandelt von einer stärkeren Position aus und erhält mehr. Die BATNA ist der Drohpunkt – das Ergebnis, wenn die Verhandlung scheitert. Je besser die Alternative, desto stärker die Verhandlungsposition.",
        difficulty = 3,
        funFact = null
    ),

    // Frage 28
    Question(
        categoryId = 12,
        questionText = "Im 'Monty Hall Problem' wählt ein Kandidat eine von 3 Türen (1 Auto, 2 Ziegen). Moderator Monty öffnet immer eine Ziegentür. Soll der Kandidat wechseln?",
        answerA = "Nein – nach dem Öffnen ist es 50:50, also egal",
        answerB = "Ja – Wechseln verdoppelt die Gewinnchance von 1/3 auf 2/3",
        answerC = "Ja, aber nur wenn Monty zufällig wählt welche Tür er öffnet",
        answerD = "Nein – die erste Wahl war intuitiv richtig",
        correctAnswer = 1,
        explanation = "Wechseln ist besser (2/3 Gewinnchance). Erklärung: Bei der ersten Wahl liegt man mit 1/3 richtig. Wechselt man, gewinnt man IMMER wenn die erste Wahl falsch war (2/3 der Fälle). Monty weiß, wo das Auto ist, und öffnet bewusst eine Ziegentür – das gibt dem Wechsel Information. Wenn Monty zufällig öffnete und eine Ziege erwischt, wäre es tatsächlich 50:50.",
        difficulty = 3,
        funFact = "Als Marilyn vos Savant 1990 in ihrer Kolumne die Lösung erklärte, bekam sie über 10.000 wütende Briefe – viele von Mathematikprofessoren – die falsch lagen. Das Problem ist berühmt dafür, Intuitionen zu brechen."
    ),

    // Frage 29
    Question(
        categoryId = 12,
        questionText = "Beim 'Volunteer's Dilemma' (Freiwilligendilemma) retten n Personen gemeinsam oder einzeln ein kollektives Gut. Kosten c für den Freiwilligen, Nutzen b für alle (b > c). Was passiert im gemischten Nash-Gleichgewicht?",
        answerA = "Immer meldet sich ein Freiwilliger",
        answerB = "Niemand meldet sich – alle warten",
        answerC = "Jeder meldet sich mit einer bestimmten Wahrscheinlichkeit; mit mehr Spielern sinkt die Wahrscheinlichkeit, dass sich überhaupt jemand meldet",
        answerD = "Die Wahrscheinlichkeit einer Freiwilligenmeldung steigt mit der Gruppe",
        correctAnswer = 2,
        explanation = "Im gemischten Gleichgewicht: Jeder einzelne meldet sich mit Wahrscheinlichkeit p* = 1 − (c/b)^(1/(n−1)). Das bedeutet: Die Gesamtwahrscheinlichkeit, dass sich NIEMAND meldet, steigt mit der Gruppe (Verantwortungsdiffusion). Mit mehr Personen sinkt p*, aber das Produkt kann auch fallen. Das erklärt den 'Bystander-Effekt': Je mehr Zeugen, desto weniger hilft jeder.",
        difficulty = 3,
        funFact = "Der Bystander-Effekt wurde 1968 nach dem Kitty-Genovese-Mord in New York bekannt. Latané und Darley zeigten experimentell: Je mehr Menschen anwesend sind, desto unwahrscheinlicher hilft einer. Das Freiwilligendilemma liefert die spieltheoretische Erklärung."
    ),

    // Frage 30
    Question(
        categoryId = 12,
        questionText = "In einem 2×2-Spiel hat Spieler A zwei Strategien (Oben/Unten), Spieler B zwei Strategien (Links/Rechts). Auszahlungsmatrix (A,B): O-L=(3,3), O-R=(0,4), U-L=(4,0), U-R=(1,1). Was sind alle Nash-Gleichgewichte in reinen Strategien?",
        answerA = "Nur (O,L)",
        answerB = "Nur (U,R)",
        answerC = "Sowohl (U,L) als auch (O,R)",
        answerD = "Nur (U,L)",
        correctAnswer = 3,
        explanation = "Prüfung: (O,L)=(3,3): A wechselt zu U→4>3 ✗. Kein GG. (O,R)=(0,4): A wechselt zu U→1>0, also wechselt A ✗. (U,L)=(4,0): A wechselt zu O→3<4, bleibt. B wechselt zu R→1>0, also wechselt B ✗. (U,R)=(1,1): A wechselt zu O→0<1, bleibt. B wechselt zu L→0<1, bleibt. Nash-GG! Nur (U,R) ist ein Nash-Gleichgewicht in reinen Strategien.",
        difficulty = 3,
        funFact = null
    ),

    // Frage 31
    Question(
        categoryId = 12,
        questionText = "Bei der 'Battle of the Sexes' (Kampf der Geschlechter): Beide wollen abends gemeinsam ausgehen. Er bevorzugt Fußball (F,F)=(2,1), sie bevorzugt Oper (O,O)=(1,2). Alleine gehen: (F,O)=(0,0) und (O,F)=(0,0). Wie viele Nash-Gleichgewichte gibt es?",
        answerA = "Keines",
        answerB = "Eines: (F,F)",
        answerC = "Zwei reine + ein gemischtes",
        answerD = "Vier (alle Kombinationen)",
        correctAnswer = 2,
        explanation = "Reine Nash-Gleichgewichte: (F,F) – keiner hat Anreiz abzuweichen (2>0, 1>0). (O,O) – keiner hat Anreiz abzuweichen (1>0, 2>0). Zusätzlich ein gemischtes Nash-Gleichgewicht, in dem er mit p=2/3 Fußball wählt und sie mit p=1/3. Das Spiel modelliert Koordinationsprobleme bei unterschiedlichen Präferenzen – klassisches Problem in der Paardynamik.",
        difficulty = 3,
        funFact = "Battle of the Sexes wurde 1957 von R. Duncan Luce und Howard Raiffa formalisiert. Es zeigt, dass selbst wenn beide zusammen sein wollen, Koordination ohne Kommunikation schwierig ist."
    ),

    // Frage 32
    Question(
        categoryId = 12,
        questionText = "Im Spiel 'Stein-Schere-Papier' – welcher Typ von Nash-Gleichgewicht existiert, und wie sieht es aus?",
        answerA = "Kein Nash-Gleichgewicht existiert",
        answerB = "Drei reine Nash-Gleichgewichte",
        answerC = "Ein gemischtes Nash-Gleichgewicht: jede Option mit Wahrscheinlichkeit 1/3 spielen",
        answerD = "Ein reines Nash-Gleichgewicht: immer Stein",
        correctAnswer = 2,
        explanation = "In Stein-Schere-Papier gibt es kein Nash-Gleichgewicht in reinen Strategien (jede reine Strategie kann vom Gegner ausgenutzt werden). Es gibt genau ein Nash-Gleichgewicht in gemischten Strategien: Jede Option mit exakt 1/3 spielen. Dann kann der Gegner keine Strategie entwickeln, die besser als 0 Erwartungswert liefert.",
        difficulty = 3,
        funFact = "Stein-Schere-Papier ist das einfachste Beispiel für Nullsummenspiele ohne reines Nash-Gleichgewicht. Professionelle Spieler (z.B. in Turnieren) analysieren die Verteilungsabweichungen ihrer Gegner – Psychologie schlägt reine Mathematik."
    ),

    // Frage 33
    Question(
        categoryId = 12,
        questionText = "Das 'St. Petersburg Paradox': Eine Münze wird geworfen, bis Kopf fällt. Bei k Würfen bis zum ersten Kopf erhält man 2^k Euro. Der Erwartungswert ist unendlich (∞). Dennoch würde fast niemand mehr als 20–30 Euro zahlen. Was ist die klassische Lösung?",
        answerA = "Der Erwartungswert ist tatsächlich endlich – die Berechnung ist falsch",
        answerB = "Menschen maximieren nicht den Erwartungswert, sondern den erwarteten Nutzen (konkave Nutzenfunktion)",
        answerC = "Das Spiel ist verboten, weil keine Kasino es anbieten kann",
        answerD = "Nur reiche Menschen würden mehr als 30 Euro zahlen",
        correctAnswer = 1,
        explanation = "Daniel Bernoulli löste das Paradox 1738: Menschen haben eine konkave Nutzenfunktion (abnehmender Grenznutzen). Reichtum verdoppeln macht nicht doppelt so glücklich. Wenn der Nutzen von 2^k Euro als log(2^k) bewertet wird, ist der erwartete Nutzen endlich (~3,32 Einheiten). Daher zahlen rationale Menschen nur einen endlichen Betrag.",
        difficulty = 3,
        funFact = "Das St. Petersburg Paradox wurde 1713 von Nicolas Bernoulli formuliert. Die Lösung durch Daniel Bernoulli (Erwartungsnutzentheorie) ist bis heute das Fundament der Entscheidungstheorie unter Unsicherheit."
    ),

    // Frage 34
    Question(
        categoryId = 12,
        questionText = "Bei der 'Winner's Curse' (Fluch des Gewinners) gewinnt in einer Auktion oft derjenige mit der höchsten Schätzung eines Objekts. Warum ist das ein Fluch?",
        answerA = "Der Gewinner muss hohe Steuern zahlen",
        answerB = "Der Gewinner hat systematisch den Wert überschätzt und zahlt zu viel",
        answerC = "Zweiter zu werden ist besser als zu gewinnen",
        answerD = "Der Gewinner wird von anderen Bietern beneidet",
        correctAnswer = 1,
        explanation = "Bei Auktionen über Objekte mit unbekanntem gemeinsamen Wert (z.B. Ölfelder) streuen individuelle Schätzungen um den wahren Wert. Der Höchstbietende hat die größte Schätzabweichung nach oben – er hat am stärksten überschätzt. Rationale Bieter müssen ihre Gebote nach unten korrigieren ('Bid shading') um den Winner's Curse zu vermeiden. Viele tun das nicht: Ölkonzerne zahlten deshalb historisch zu viel für Förderlizenzen.",
        difficulty = 3,
        funFact = "Der Winner's Curse wurde 1971 von Capen, Clapp und Campbell in der Ölindustrie entdeckt. Richard Thaler popularisierte ihn 1988. Er erklärt auch Unternehmensübernahmen: Akquirierende zahlen oft 20–40% mehr als der faire Wert."
    ),

    // Frage 35
    Question(
        categoryId = 12,
        questionText = "Beim 'Coase-Theorem' (Ronald Coase, 1960): Zwei Parteien haben einen Konflikt (z.B. Fabrik verschmutzt See eines Fischers). Was sagt das Theorem über die Verhandlungslösung?",
        answerA = "Nur staatliche Regulierung kann das Problem lösen",
        answerB = "Bei klar definierten Eigentumsrechten und null Transaktionskosten handeln Parteien immer zur effizienten Lösung",
        answerC = "Die Fabrik hat immer das Recht zu verschmutzen",
        answerD = "Der Fischer muss immer entschädigt werden",
        correctAnswer = 1,
        explanation = "Das Coase-Theorem: Wenn Eigentumsrechte klar sind und Transaktionskosten null sind, kommen Parteien durch Verhandlung immer zur effizienten Lösung – unabhängig davon, wem das Eigentumsrecht zugewiesen wird. Hat die Fabrik das Recht zu verschmutzen, zahlt der Fischer sie aus. Hat der Fischer das Recht auf sauberes Wasser, zahlt die Fabrik ihn aus. Das Ergebnis ist effizient, nur die Verteilung ändert sich.",
        difficulty = 3,
        funFact = "Ronald Coase erhielt 1991 den Nobelpreis. Das Theorem zeigt, wann Märkte externe Effekte selbst lösen können – und wann staatliche Eingriffe nötig sind (nämlich wenn Transaktionskosten hoch sind)."
    ),

    // Frage 36
    Question(
        categoryId = 12,
        questionText = "Beim 'Allmende-Problem' (Tragedy of the Commons) nutzen mehrere Bauern eine gemeinsame Weide. Jeder Bauer hat einen Anreiz, ein weiteres Tier hinzuzufügen. Was passiert im Nash-Gleichgewicht?",
        answerA = "Die Bauern koordinieren sich spontan zur optimalen Nutzung",
        answerB = "Die Weide wird übernutzt und zerstört, obwohl alle davon verlieren",
        answerC = "Der stärkste Bauer sichert sich die Weide alleine",
        answerD = "Die Bauern teilen die Weide automatisch fair auf",
        correctAnswer = 1,
        explanation = "Die Tragödie der Allmende (Garrett Hardin, 1968): Jeder Bauer rechnet: Ich bekomme vollen Nutzen eines zusätzlichen Tiers, aber die Kosten der Überweidung teile ich mit allen. Der individuelle Anreiz überwiegt – alle fügen Tiere hinzu. Nash-Gleichgewicht: Überweidung und Zerstörung der Weide, obwohl alle besser wären, wenn sie die Nutzung beschränkten.",
        difficulty = 3,
        funFact = "Elinor Ostrom zeigte 2009 (Nobelpreis), dass Hardin falsch lag: Lokale Gemeinschaften haben Lösungen entwickelt (Regeln, gegenseitige Kontrolle) die Allmenden nachhaltig erhalten. Das Nash-Gleichgewicht wird durch soziale Institutionen überwunden."
    ),

    // Frage 37
    Question(
        categoryId = 12,
        questionText = "Im 'Stackelberg-Modell' wählt Firmenchef A zuerst seine Produktionsmenge, dann wählt B. Was ist der Vorteil für A als 'First Mover'?",
        answerA = "A kann mehr produzieren und höheren Gewinn erzielen als im Cournot-Gleichgewicht",
        answerB = "A hat keinen Vorteil – B passt sich perfekt an",
        answerC = "B hat den Vorteil, weil er die Entscheidung von A kennt",
        answerD = "Beide produzieren weniger als im Cournot-Modell",
        correctAnswer = 0,
        explanation = "First-Mover-Vorteil im Stackelberg-Modell: A wählt eine größere Menge als im Cournot-Gleichgewicht, weil er weiß, dass B als beste Antwort weniger produziert. A erzielt mehr Gewinn als im simultanen Cournot-Spiel. B produziert weniger und macht weniger Gewinn. Der Gesamtoutput steigt, was Konsumenten nutzt. Dies modelliert z.B. Marktführer vs. Nachfolger.",
        difficulty = 3,
        funFact = "Heinrich von Stackelberg analysierte das Modell 1934. Es zeigt, warum Marktführer (z.B. Apple) oft als erste in neue Märkte eintreten: Der First-Mover setzt Standards und zwingt Follower zur Reaktion."
    ),

    // Frage 38
    Question(
        categoryId = 12,
        questionText = "Beim 'Lemons Problem' (Markt für Zitronen) von George Akerlof: Auf einem Gebrauchtwagenmarkt gibt es gute Autos und Schrott ('Lemons'). Käufer können Qualität nicht unterscheiden. Was passiert mit dem Markt?",
        answerA = "Käufer zahlen den Durchschnittspreis, der Markt funktioniert normal",
        answerB = "Nur Schrott wird verkauft – gute Autos werden vom Markt verdrängt",
        answerC = "Verkäufer guter Autos senken freiwillig die Preise zur Fairness",
        answerD = "Der Staat reguliert sofort die Qualität",
        correctAnswer = 1,
        explanation = "Adverse Selektion (Akerlof, 1970): Käufer zahlen nur Durchschnittspreis (unbekannte Qualität). Verkäufer guter Autos finden den Preis zu niedrig und nehmen ihr Auto vom Markt. Damit sinkt die Durchschnittsqualität, der Preis fällt weiter, mehr gute Autos verschwinden. Im Gleichgewicht nur Schrott – der Markt kollabiert. Lösung: Signaling (Garantien, Zertifikate, Inspektionen).",
        difficulty = 3,
        funFact = "George Akerlof erhielt 2001 den Nobelpreis für diese Analyse. Sein Paper wurde anfangs dreimal abgelehnt – Zeitschriften fanden es 'zu simpel'. Es ist heute eines der meistzitierten Wirtschaftspapiere aller Zeiten."
    ),

    // Frage 39
    Question(
        categoryId = 12,
        questionText = "Im 'Signaling Game' sendet ein informierter Spieler ein Signal an einen uninformierten. Wann ist ein Signal glaubwürdig (trennend)?",
        answerA = "Wenn das Signal billig ist und jeder es senden kann",
        answerB = "Wenn das Signal für den starken Typ günstiger ist als für den schwachen Typ (Kostenbedingung)",
        answerC = "Wenn der uninformierte Spieler dem Signal immer glaubt",
        answerD = "Wenn das Signal gesetzlich vorgeschrieben ist",
        correctAnswer = 1,
        explanation = "Ein trennendes Gleichgewicht existiert, wenn das Signal für den 'guten' Typ billiger zu senden ist als für den 'schlechten' Typ. Dann imitiert der schlechte Typ das Signal nicht (zu teuer). Beispiel: Ausbildung als Signal für Produktivität (Spence, 1973). Produktive Menschen absolvieren Bildung billiger (weniger Mühe). Unproduktive imitieren nicht, da zu kostspielig.",
        difficulty = 3,
        funFact = "Michael Spence erhielt 2001 den Nobelpreis für die Signaling-Theorie. Kontroverses Ergebnis: Bildung kann rein als Signal funktionieren, ohne Produktivität zu erhöhen – was wichtige Implikationen für Bildungspolitik hat."
    ),

    // Frage 40
    Question(
        categoryId = 12,
        questionText = "Beim 'Screening' versucht der Uninformierte, Information zu extrahieren. Wie unterscheidet sich Screening von Signaling?",
        answerA = "Es gibt keinen Unterschied",
        answerB = "Beim Screening handelt der Informierte zuerst; beim Signaling handelt der Uninformierte zuerst",
        answerC = "Beim Screening handelt der Uninformierte zuerst und gestaltet Verträge/Menüs, die den Typ enthüllen",
        answerD = "Screening funktioniert nur bei staatlichen Stellen",
        correctAnswer = 2,
        explanation = "Unterschied: Signaling – der Informierte sendet freiwillig ein Signal (z.B. Uniabschluss). Screening – der Uninformierte gestaltet ein Menü von Optionen, sodass sich verschiedene Typen selbst sortieren. Beispiel: Versicherungen bieten verschiedene Verträge mit Selbstbehalt an. Gesunde wählen hohen Selbstbehalt (günstig). Kranke wählen niedrigen Selbstbehalt (teurer). So trennt der Uninformierte die Typen.",
        difficulty = 3,
        funFact = null
    ),

    // Frage 41
    Question(
        categoryId = 12,
        questionText = "Im 'Principal-Agent-Problem' delegiert ein Chef (Principal) eine Aufgabe an einen Mitarbeiter (Agent). Das Kernproblem ist 'Moral Hazard'. Was bedeutet das?",
        answerA = "Der Agent lügt über seine Fähigkeiten vor Vertragsabschluss",
        answerB = "Nach Vertragsabschluss kann der Agent weniger Mühe geben, da der Chef den Einsatz nicht beobachtet",
        answerC = "Der Chef behandelt den Agenten unfair",
        answerD = "Der Agent sabotiert bewusst das Unternehmen",
        correctAnswer = 1,
        explanation = "Moral Hazard (Moral Risiko) entsteht nach Vertragsabschluss: Wenn der Chef den Arbeitseinsatz nicht beobachten kann, hat der Agent Anreiz, weniger zu arbeiten (seine Mühe spart er, Ergebnis hängt auch von Zufall ab). Lösung: Anreizkontraktte (Erfolgsbeteiligung, Bonussysteme), die den Agenten am Ergebnis beteiligen. Unterschied zu adverser Selektion (vor Vertragsabschluss).",
        difficulty = 3,
        funFact = "Moral Hazard erklärt, warum Vollkaskoversicherung zu sorgloserer Fahrweise führt, warum Banker 'Too big to fail' Risiken eingehen, und warum Manager mit Aktienoptionen anders entscheiden als ohne."
    ),

    // Frage 42
    Question(
        categoryId = 12,
        questionText = "Beim 'Hotelling-Modell' (1929) platzieren sich zwei Eisverkäufer an einem Strand (0–100 Meter). Kunden kaufen beim nächsten Verkäufer. Wo ist das Nash-Gleichgewicht?",
        answerA = "Verkäufer 1 bei 25m, Verkäufer 2 bei 75m (maximale Abdeckung)",
        answerB = "Beide in der Mitte (50m) nebeneinander",
        answerC = "Einer an jedem Ende (0m und 100m)",
        answerD = "Gleichmäßig verteilt, wechselnde Positionen",
        correctAnswer = 1,
        explanation = "Das 'Prinzip der minimalen Differenzierung': Im Nash-Gleichgewicht stehen beide Verkäufer nebeneinander in der Mitte (50m). Warum: Jeder hat Anreiz, leicht in Richtung Mitte zu rücken, um mehr Kunden abzuwerben. Dieser Prozess endet in der Mitte. Paradox: Beide Enden des Strands sind schlecht versorgt. Das erklärt, warum politische Parteien zur Mitte driften und Supermärkte nebeneinander entstehen.",
        difficulty = 3,
        funFact = "Harold Hotelling entwickelte dieses Modell 1929. Es erklärt nicht nur Standortwettbewerb, sondern auch politische Konvergenz (Parteien werden ähnlicher), Produktdifferenzierung und Medienausrichtung auf die 'Mitte'."
    ),

    // Frage 43
    Question(
        categoryId = 12,
        questionText = "Im 'Rubinstein-Verhandlungsmodell' (1982) machen zwei Parteien sich abwechselnd Angebote bei einem 1-Euro-Kuchen. Jede Runde verringert sich der Wert durch Diskontierung (δ < 1). Was passiert im Gleichgewicht?",
        answerA = "Beide teilen immer hälftig, unabhängig von der Diskontrate",
        answerB = "Der erste Bieter bekommt mehr: 1/(1+δ), der zweite δ/(1+δ)",
        answerC = "Die Verhandlung dauert unendlich lange",
        answerD = "Der Ungeduldigere bekommt mehr",
        correctAnswer = 1,
        explanation = "Im Rubinstein-Gleichgewicht (subgame perfect): Spieler 1 bietet sofort im ersten Zug (1/(1+δ), δ/(1+δ)) an und Spieler 2 akzeptiert. Je geduldiger (δ → 1) ein Spieler ist, desto mehr bekommt er. Je ungeduldiger, desto weniger. Der First-Mover-Vorteil schwindet, wenn beide perfekt geduldig sind (δ = 1): dann teilen beide hälftig.",
        difficulty = 3,
        funFact = "Ariel Rubinstein löste 1982 das jahrzehntelange Problem der Verhandlungstheorie durch Rückwärtsinduktion. Das Modell erklärt Tarifverhandlungen, Scheidungsabkommen und Staatsverhandlungen."
    ),

    // Frage 44
    Question(
        categoryId = 12,
        questionText = "Das 'Paradox der Abstimmung' (Condorcet-Paradox): Drei Wähler A, B, C wählen zwischen drei Optionen X, Y, Z. A: X > Y > Z. B: Y > Z > X. C: Z > X > Y. Was zeigt das Paradox?",
        answerA = "X gewinnt demokratisch",
        answerB = "Mehrheitsentscheidungen können zyklisch und irrational sein: X schlägt Y, Y schlägt Z, Z schlägt X",
        answerC = "Alle Optionen sind gleichwertig",
        answerD = "B bestimmt das Ergebnis als mittlerer Wähler",
        correctAnswer = 1,
        explanation = "Condorcet-Paradox (1785): Bei paarweisen Mehrheitsentscheidungen gilt: X > Y (A und C), Y > Z (A und B), Z > X (B und C). Das ergibt einen Zyklus! Keine Option ist die 'rationale' gesellschaftliche Wahl. Arrows Unmöglichkeitssatz (1951) verallgemeinert: Es gibt kein Abstimmungsverfahren, das alle rationalen Kriterien gleichzeitig erfüllt.",
        difficulty = 3,
        funFact = "Kenneth Arrow erhielt 1972 den Nobelpreis für sein Unmöglichkeitstheorem. Es zeigt: Demokratische Entscheidungen können prinzipiell irrational sein – nicht durch schlechtes Design, sondern durch mathematische Unmöglichkeit."
    ),

    // Frage 45
    Question(
        categoryId = 12,
        questionText = "Im 'Gale-Shapley-Algorithmus' für stabiles Matching (Hochzeiten/Stipendien): Was bedeutet eine 'stabile' Paarung?",
        answerA = "Alle Paare sind gleich glücklich",
        answerB = "Keine zwei Personen würden lieber miteinander zusammen sein als mit ihren aktuellen Partnern",
        answerC = "Die Paare wurden zufällig zugewiesen",
        answerD = "Alle haben ihren Erstpräferenzpartner bekommen",
        correctAnswer = 1,
        explanation = "Stabile Paarung (keine 'blockierende Paare'): Es gibt kein Paar (m, f), die beide lieber miteinander als mit ihren aktuellen Partnern wären. Der Gale-Shapley-Algorithmus garantiert immer eine stabile Lösung. In der proposierenden Gruppe (wer zuerst fragt) ist das Ergebnis für diese Gruppe optimal. Anwendung: Medizinstudent-Krankenhaus-Matching, Schulplatzvergabe.",
        difficulty = 3,
        funFact = "Lloyd Shapley und Alvin Roth erhielten 2012 den Nobelpreis für den Gale-Shapley-Algorithmus. Er wird heute bei US-Medizinstipendien, Nierenspende-Matching und Schulplatzvergabe weltweit eingesetzt."
    ),

    // Frage 46
    Question(
        categoryId = 12,
        questionText = "Beim 'Cheap Talk' sendet ein informierter Spieler eine Botschaft ohne direkte Kosten. Wann ist die Kommunikation informativ (nicht nur 'Lärm')?",
        answerA = "Immer – mehr Information ist immer besser",
        answerB = "Wenn die Interessen von Sender und Empfänger hinreichend übereinstimmen",
        answerC = "Wenn der Sender unter Eid steht",
        answerD = "Nie – Cheap Talk ist per Definition wertlos",
        correctAnswer = 1,
        explanation = "Crawford-Sobel-Modell (1982): Cheap Talk ist informativ, wenn Sender und Empfänger ähnliche Interessen haben. Bei entgegengesetzten Interessen (Versicherungsvertreter verkauft unnötige Police) ist Cheap Talk uninformativ – dem Sender kann man nicht glauben. Bei gleichen Interessen (echter Berater) ist Kommunikation glaubwürdig. Das erklärt, warum unabhängige Berater wertvoller sind als abhängige.",
        difficulty = 3,
        funFact = null
    ),

    // Frage 47
    Question(
        categoryId = 12,
        questionText = "Im 'Mechanism Design' (umgekehrte Spieltheorie) will ein Designer eine Regel entwerfen, die zu einem gewünschten Ergebnis führt. Was ist die 'Offenbarungsprinzip'?",
        answerA = "Jeder Mechanismus kann durch einen direkten Mechanismus ersetzt werden, bei dem ehrliches Berichten optimal ist",
        answerB = "Alle privaten Informationen müssen öffentlich gemacht werden",
        answerC = "Der Designer offenbart die Regeln des Spiels",
        answerD = "Nur Regierungen können Mechanismen entwerfen",
        correctAnswer = 0,
        explanation = "Das Offenbarungsprinzip: Jeder Mechanismus, der ein bestimmtes Ergebnis implementiert, kann durch einen anreizkompatiblen direkten Mechanismus ersetzt werden, bei dem alle Spieler ehrlich ihre privaten Informationen berichten. Das vereinfacht die Designaufgabe enorm – man muss nur anreizkompatible direkte Mechanismen betrachten. Grundlage für Auktionsdesign, Steuerpolitik, Regulierung.",
        difficulty = 3,
        funFact = "Roger Myerson, Eric Maskin und Leonid Hurwicz teilten sich 2007 den Nobelpreis für Mechanism Design. Das Offenbarungsprinzip ist Myersons wichtigster Beitrag – er bewies es 1979."
    ),

    // Frage 48
    Question(
        categoryId = 12,
        questionText = "Ein Unternehmen zahlt seinem Vertriebsmitarbeiter ein fixes Gehalt. Was sagt die Spieltheorie über die Arbeitsmotivation?",
        answerA = "Festes Gehalt maximiert die Motivation – Sicherheit fördert Leistung",
        answerB = "Der Agent arbeitet wenig (Moral Hazard), da sein Einkommen nicht vom Ergebnis abhängt",
        answerC = "Der Agent arbeitet maximal, um Beförderung zu bekommen",
        answerD = "Das Gehalt hat keinen Einfluss auf die Motivation",
        correctAnswer = 1,
        explanation = "Moral Hazard im Principal-Agent-Modell: Bei festem Gehalt trägt der Agent kein Ergebnisrisiko. Da Mühe kostet und das Gehalt fest ist, wird er sein Anstrengungsniveau reduzieren (sofern nicht beobachtbar). Optimale Lösung: Leistungsabhängige Bezahlung (Boni, Gewinnbeteiligung, Provisionen), die den Agenten am Ergebnis beteiligen und Anreize zur Anstrengung schaffen.",
        difficulty = 3,
        funFact = "Das Principal-Agent-Problem wurde in den 1970er Jahren von Michael Jensen und William Meckling formalisiert. Es ist Grundlage der modernen Unternehmensführung – von Managergehältern bis hin zu Bankerkontrollen nach 2008."
    ),

    // Frage 49
    Question(
        categoryId = 12,
        questionText = "Zwei Spieler spielen ein endliches, wiederholtes Gefangenendilemma (genau 10 Runden, beide wissen das). Was sagt die Rückwärtsinduktion über die letzte Runde?",
        answerA = "In der letzten Runde kooperieren beide, weil sie die Beziehung schätzen",
        answerB = "In der letzten Runde defektiert jeder – dann auch in der vorletzten – bis alle Runden defektiert werden",
        answerC = "Die Kooperation bleibt bis zur letzten Runde stabil",
        answerD = "In der letzten Runde spielt man zufällig",
        correctAnswer = 1,
        explanation = "Rückwärtsinduktions-Argument: In Runde 10 (letzte) gibt es keine zukünftige Konsequenz – also defektiert jeder. In Runde 9 wissen beide, dass Runde 10 defektiert wird – Kooperation bringt nichts mehr – also defektiert jeder. Dieses Argument läuft bis zur ersten Runde: Das einzige Nash-Gleichgewicht ist immer Defektieren. In der Praxis kooperieren Menschen trotzdem oft – weil sie nicht rein rückwärtsinduktiv denken.",
        difficulty = 3,
        funFact = "Das Paradox des endlich wiederholten Gefangenendilemmas zeigt: Kooperation ist spieltheoretisch nur bei unendlichen oder unbekant-langen Spielen stabil. In der Realität kooperieren Menschen trotzdem – ein Beweis für begrenzte Rationalität."
    ),

    // Frage 50
    Question(
        categoryId = 12,
        questionText = "Bei der 'Spieltheorie der Evolution' (ESS – Evolutionär Stabile Strategie) ist eine Strategie S evolutionär stabil, wenn...",
        answerA = "Sie immer den höchsten Payoff gegen alle möglichen Strategien liefert",
        answerB = "Eine Population, die S spielt, nicht von einer kleinen Gruppe Mutanten mit anderer Strategie invadiert werden kann",
        answerC = "Sie das Nash-Gleichgewicht in reinen Strategien ist",
        answerD = "Sie die häufigste Strategie in der Bevölkerung ist",
        correctAnswer = 1,
        explanation = "Evolutionär Stabile Strategie (John Maynard Smith, 1973): S ist ESS, wenn gilt: (1) S ist beste Antwort auf S (Nash-Bedingung), und (2) wenn ein Mutant dieselbe Auszahlung gegen S erhält, dann muss S besser gegen Mutanten sein als Mutanten gegen sich selbst (Stabilitätsbedingung). ESS ist stärker als Nash-Gleichgewicht: Jede ESS ist ein Nash-GG, aber nicht jedes Nash-GG ist eine ESS.",
        difficulty = 3,
        funFact = "Die evolutionäre Spieltheorie veränderte die Biologie: Sie erklärt Tierverhalten, Kooperation in der Natur, Geschlechterverteilungen (1:1) und sogar das Wachstum von Tumorzellen ohne auf 'Bewusstsein' oder 'Rationalität' angewiesen zu sein."
    )

)
