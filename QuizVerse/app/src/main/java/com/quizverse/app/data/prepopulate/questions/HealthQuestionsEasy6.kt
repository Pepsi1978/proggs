package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun healthQuestionsEasy6(): List<Question> = listOf(

    // --- Dental health ---

    Question(
        categoryId = 16,
        questionText = "Wie oft am Tag sollte man die Zaehne putzen?",
        answerA = "Einmal am Tag",
        answerB = "Zweimal am Tag",
        answerC = "Nur nach dem Fruehstueck",
        answerD = "Nur abends",
        correctAnswer = 1,
        explanation = "Zahnaerzte empfehlen, die Zaehne morgens und abends je zwei Minuten gruendlich zu putzen, um Karies und Zahnfleischentzuendungen zu vermeiden.",
        difficulty = 1,
        funFact = "Wer abends die Zaehne nicht putzt, laesst Bakterien die ganze Nacht ununterbrochen am Zahnschmelz arbeiten -- das ist die gefahrlichste Zeit fuer Karies."
    ),

    Question(
        categoryId = 16,
        questionText = "Was verursacht Karies?",
        answerA = "Zu kaltes Wasser",
        answerB = "Saeuren von Bakterien, die Zucker abbauen",
        answerC = "Zu hartes Zahnbuersten",
        answerD = "Fluorid im Wasser",
        correctAnswer = 1,
        explanation = "Bakterien im Mund bauen Zucker ab und produzieren dabei Saeuren. Diese Saeuren greifen den Zahnschmelz an und fuehren zu Karies.",
        difficulty = 1,
        funFact = "Karies ist weltweit die haeufigste chronische Erkrankung -- sie betrifft mehr Menschen als Diabetes oder Herzerkrankungen zusammen."
    ),

    Question(
        categoryId = 16,
        questionText = "Wozu dient Zahnseide?",
        answerA = "Zum Polieren des Zahnschmelzes",
        answerB = "Zur Reinigung der Zahnzwischenraeume",
        answerC = "Zur Staerkung des Zahnfleisches durch Massage",
        answerD = "Zum Entfernen von Zahnstein",
        correctAnswer = 1,
        explanation = "Zahnseide entfernt Plaquebelaege und Speisereste aus den Zahnzwischenraeumen, die die Zahnbuerste nicht erreichen kann.",
        difficulty = 1,
        funFact = "Rund 40 % der Zahnoberflaeche liegen in den Zwischenraeumen -- wer keine Zahnseide benutzt, reinigt weniger als zwei Drittel seiner Zaehne."
    ),

    Question(
        categoryId = 16,
        questionText = "Was ist Fluorid und warum ist es fuer Zaehne wichtig?",
        answerA = "Ein Suessungsmittel ohne Kalorien",
        answerB = "Ein Mineral, das den Zahnschmelz haertet und vor Karies schuetzt",
        answerC = "Ein Antibiotikum gegen Mundgeruche",
        answerD = "Ein Farbstoff in Zahnpasta",
        correctAnswer = 1,
        explanation = "Fluorid lagert sich in den Zahnschmelz ein und macht ihn widerstandsfaehiger gegen Saeuren. Es kann sogar fruehe Kariesstadien remineralisieren.",
        difficulty = 1,
        funFact = "In den 1940er Jahren fiel auf, dass Menschen in Gebieten mit natuerlich hohem Fluoridgehalt im Trinkwasser deutlich weniger Karies hatten -- das fuehrte zur Fluoridierung von Zahnpasta."
    ),

    Question(
        categoryId = 16,
        questionText = "Was sind Weisheitszaehne?",
        answerA = "Die ersten Milchzaehne eines Kindes",
        answerB = "Die hintersten Backenzaehne, die zuletzt durchbrechen",
        answerC = "Zaehne, die durch Karies verloren gehen",
        answerD = "Besonders harte Zaehne vorne im Mund",
        correctAnswer = 1,
        explanation = "Weisheitszaehne sind die dritten Molaren (Backenzaehne), die meist im Alter von 17 bis 25 Jahren durchbrechen. Sie machen oft Probleme, weil der Kiefer zu wenig Platz hat.",
        difficulty = 1,
        funFact = "Nur etwa 25 % aller Menschen behalten ihre Weisheitszaehne ohne Probleme -- bei den meisten muessen sie entfernt werden."
    ),

    Question(
        categoryId = 16,
        questionText = "Wie oft sollte man zum Zahnarzt zur Kontrolluntersuchung gehen?",
        answerA = "Alle fuenf Jahre",
        answerB = "Nur wenn Schmerzen auftreten",
        answerC = "Einmal bis zweimal jaehrlich",
        answerD = "Einmal alle drei Jahre",
        correctAnswer = 2,
        explanation = "Eine regelmaessige Zahnarztuntersuchung alle sechs bis zwoelf Monate erlaubt die fruehzeitige Erkennung von Karies, Zahnfleischerkrankungen und anderen Problemen.",
        difficulty = 1,
        funFact = "Wer regelmaessig zur Zahnarzt-Vorsorge geht, hat in Deutschland Anspruch auf hoehere Krankenkassenleistungen beim Zahnersatz -- Praemie fuer regelmaessige Besuche."
    ),

    Question(
        categoryId = 16,
        questionText = "Was ist Zahnstein?",
        answerA = "Ein seltenes Mineral in der Zahncreme",
        answerB = "Verhartete Zahnbelaege, die sich nicht mehr wegbuersten lassen",
        answerC = "Ein Stein, der beim Zahnarzt eingesetzt wird",
        answerD = "Ein anderes Wort fuer Karies",
        correctAnswer = 1,
        explanation = "Zahnstein entsteht, wenn sich Zahnbelag (Plaque) mit Mineralien aus dem Speichel verbindet und verhaertet. Er kann nur vom Zahnarzt professionell entfernt werden.",
        difficulty = 1,
        funFact = "Zahnstein entsteht bereits nach 12 bis 15 Tagen aus nicht entfernter Plaque -- deshalb ist regelmaessiges Zaehneputzen so wichtig."
    ),

    Question(
        categoryId = 16,
        questionText = "Was versteht man unter Milchzaehnen?",
        answerA = "Zaehne, die von Karies weiss gefaerbt sind",
        answerB = "Das erste Gebiss eines Kindes, das spaeter durch bleibende Zaehne ersetzt wird",
        answerC = "Zaehne, die durch viel Milchtrinken staerker werden",
        answerD = "Ein anderer Begriff fuer Weisheitszaehne",
        correctAnswer = 1,
        explanation = "Milchzaehne sind das erste Gebiss (20 Zaehne), das Kinder ab dem 6. Monat bekommen. Zwischen 6 und 12 Jahren werden sie nach und nach durch die 32 bleibenden Zaehne ersetzt.",
        difficulty = 1,
        funFact = "Auch Milchzaehne muessen sorgfaeltig gepflegt werden -- Karies an Milchzaehnen kann das darunter liegende bleibende Gebiss schaedigen."
    ),

    // --- Eye health ---

    Question(
        categoryId = 16,
        questionText = "Was bedeutet Kurzsichtigkeit?",
        answerA = "Man sieht sehr kleine Dinge nicht gut",
        answerB = "Man sieht nahe Dinge klar, aber entfernte Dinge verschwommen",
        answerC = "Man sieht nur Farben schlecht",
        answerD = "Man braucht mehr Licht zum Lesen",
        correctAnswer = 1,
        explanation = "Bei Kurzsichtigkeit (Myopie) werden entfernte Objekte verschwommen wahrgenommen, weil das Bild vor der Netzhaut statt auf ihr entsteht. Kurzsichtige sehen Nahe klar.",
        difficulty = 1,
        funFact = "Die Kurzsichtigkeit hat in den letzten Jahrzehnten weltweit stark zugenommen -- Forscher machen vor allem zu viel Zeit drinnen und zu wenig Tageslicht verantwortlich."
    ),

    Question(
        categoryId = 16,
        questionText = "Wie oft sollte man beim Augenarzt zur Vorsorge gehen?",
        answerA = "Nur bei Sehproblemen oder Schmerzen",
        answerB = "Alle zehn Jahre",
        answerC = "Regelmaessig alle ein bis zwei Jahre, je nach Alter und Risiko",
        answerD = "Nur im Kindesalter",
        correctAnswer = 2,
        explanation = "Regelmaessige Augenuntersuchungen ermoglichen die fruehzeitige Erkennung von Erkrankungen wie Glaukom, Grauem Star oder Diabetischer Retinopathie, bevor Schaden entsteht.",
        difficulty = 1,
        funFact = "Das Glaukom (gruener Star) schaedigt den Sehnerv oft ohne Schmerzen -- viele Betroffene merken erst im Spaetstadum, dass ihre Sehkraft nachlasst."
    ),

    Question(
        categoryId = 16,
        questionText = "Was ist die 20-20-20-Regel beim Arbeiten am Bildschirm?",
        answerA = "Alle 20 Stunden 20 Minuten pausieren",
        answerB = "Alle 20 Minuten fuer 20 Sekunden auf ein Objekt 20 Fuss (6 m) entfernt schauen",
        answerC = "Hoechstens 20 Stunden pro Woche am Bildschirm sitzen",
        answerD = "Den Bildschirm mit 20 % Helligkeit benutzen",
        correctAnswer = 1,
        explanation = "Die 20-20-20-Regel hilft, die Augen bei Bildschirmarbeit zu entlasten: alle 20 Minuten den Blick fuer 20 Sekunden auf etwas ca. 6 Meter entferntes richten.",
        difficulty = 1,
        funFact = "Beim Starren auf Bildschirme blinzeln wir bis zu 66 % weniger als normal -- das fuehrt zu Trockenheit und Erschoepfung der Augen."
    ),

    Question(
        categoryId = 16,
        questionText = "Was ist der Graue Star (Katarakt)?",
        answerA = "Eine Entzuendung des Augenlids",
        answerB = "Eine Erblindung durch erhoehten Augeninnendruck",
        answerC = "Eine Eintrubung der Augenlinse, die das Sehen beeintraechtigt",
        answerD = "Eine Farbsehschwaeche",
        correctAnswer = 2,
        explanation = "Beim Grauen Star (Katarakt) wird die Augenlinse zunehmend trueb, was zu vernebeltem Sehen fuehrt. Er tritt haeufig im Alter auf und kann operativ sehr gut behandelt werden.",
        difficulty = 1,
        funFact = "Kataraktoperationen gehoeren zu den haeufigsten und erfolgreichsten chirurgischen Eingriffen weltweit -- in Deutschland werden jaehrlich rund 800.000 solcher Operationen durchgefuehrt."
    ),

    Question(
        categoryId = 16,
        questionText = "Warum ist UV-Schutz auch fuer die Augen wichtig?",
        answerA = "UV-Licht kann die Augenfarbe veraendern",
        answerB = "UV-Strahlen koennen die Augenlinse und Hornhaut schaedigen und Katarrakt oder Hornhautentzuendung verursachen",
        answerC = "UV-Licht macht die Augen empfindlicher fuer Blaulicht",
        answerD = "UV-Schutz fuer Augen ist nur fuer Kinder notwendig",
        correctAnswer = 1,
        explanation = "UV-Strahlen koennen die Hornhaut verbrennen (Schneeblindheit) und langfristig die Entstehung von Grauem Star und anderen Augenerkrankungen foerdern.",
        difficulty = 1,
        funFact = "Eine Sonnenbrille ohne UV-400-Schutz kann sogar schaedlicher sein als gar keine -- weil die getoenten Glaeser die Pupillen weiten und so mehr ungefiltertes UV-Licht eintritt."
    ),

    // --- Ear health ---

    Question(
        categoryId = 16,
        questionText = "Was ist Ohrenrauschen (Tinnitus)?",
        answerA = "Eine Infektion des Mittelohres",
        answerB = "Das Wahrnehmen von Geraeuchen wie Pfeifen oder Rauschen ohne externe Schallquelle",
        answerC = "Erhoehter Druck im Innenohr",
        answerD = "Eine Entzuendung des Hoernerves",
        correctAnswer = 1,
        explanation = "Tinnitus bezeichnet das Wahrnehmen von Geraeuchen wie Piepen, Pfeifen oder Rauschen, obwohl keine externe Schallquelle vorhanden ist. Er kann durch Larm, Stress oder Hoersturz entstehen.",
        difficulty = 1,
        funFact = "In Deutschland leiden rund 3 Millionen Menschen unter chronischem Tinnitus -- ein Drittel davon empfindet ihn als starke Belastung im Alltag."
    ),

    Question(
        categoryId = 16,
        questionText = "Wie laut darf Musik ueber Kopfhoerer hoechstens sein, um die Ohren zu schonen?",
        answerA = "Bis zu 120 Dezibel",
        answerB = "Bis zu 85 Dezibel",
        answerC = "Bis zu 60 Dezibel",
        answerD = "Lautstaerke spielt keine Rolle",
        correctAnswer = 1,
        explanation = "Ab 85 Dezibel kann laengere Larmexposition das Hoervermogen dauerhaft schaedigen. Die WHO empfiehlt, Kopfhoerer nicht laenger als eine Stunde bei dieser Lautstaerke zu nutzen.",
        difficulty = 1,
        funFact = "Ein normales Gespraech hat etwa 60 dB, ein Rasenmaeher 90 dB und ein Konzert kann 110 dB erreichen -- permanenter Hoerschaden kann ab 85 dB entstehen."
    ),

    Question(
        categoryId = 16,
        questionText = "Was ist Ohrenschmalz und wozu dient es?",
        answerA = "Eine Substanz, die das Horvermogen beeintraechtigt",
        answerB = "Eine Schutzsubstanz, die den Gehoergang reinigt, befeuchtet und vor Infektionen schuetzt",
        answerC = "Eine krankhafte Ablagerung im Ohr",
        answerD = "Eine Druesenabsonderung, die entfernt werden muss",
        correctAnswer = 1,
        explanation = "Ohrenschmalz (Cerumen) ist eine natuerliche Substanz, die den Gehoergang vor Staub, Bakterien und Insekten schuetzt. Der Gehoergang reinigt sich normalerweise selbst.",
        difficulty = 1,
        funFact = "Wattestaeche im Ohr druecken das Ohrenschmalz tiefer in den Gehoergang hinein anstatt es zu entfernen -- Experten raten davon ab, sie zur Ohrenreinigung zu verwenden."
    ),

    Question(
        categoryId = 16,
        questionText = "Was ist Laermschutz und wann braucht man ihn?",
        answerA = "Ein Geraet zum Messen von Laerm",
        answerB = "Schutzausruestung wie Kapselgehoerschutz oder Ohrstoepsel bei lautem Larm",
        answerC = "Eine Medizin gegen Tinnitus",
        answerD = "Eine Impfung gegen Hoerverlust",
        correctAnswer = 1,
        explanation = "Laermschutz wie Ohrstoepsel oder Kapselgehoerschuetzer reduziert die Schallintensitaet und schuetzt das Gehoer vor dauerhaften Schaden bei lautem Larm.",
        difficulty = 1,
        funFact = "Laermbedingter Hoerverlust ist die haeufigste vermeidbare Hoerschaedigung -- und ist vollstaendig irreversibel, weil Hoerzellen sich nicht regenerieren."
    ),

    Question(
        categoryId = 16,
        questionText = "Was ist eine Mittelohrentzuendung?",
        answerA = "Eine Entzuendung der Ohrmuschel durch Erfrierung",
        answerB = "Eine Infektion des Mittelohres, haeufig nach Erkaeltungen",
        answerC = "Eine Pilzinfektion des aeusseren Gehoergangs",
        answerD = "Ein Druckschmerz durch Fliegen",
        correctAnswer = 1,
        explanation = "Eine Mittelohrentzuendung (Otitis media) entsteht haeufig als Komplikation von Erkaeltungen oder Grippe. Sie ist bei Kindern besonders haeufig und aeussert sich durch starke Ohrenschmerzen.",
        difficulty = 1,
        funFact = "Kinder bekommen viel haeufiger Mittelohrentzuendungen als Erwachsene, weil ihre Eustachische Roehre (Verbindung zwischen Mittelohr und Rachen) kuenzer und horizontaler liegt."
    ),

    // --- Skin protection ---

    Question(
        categoryId = 16,
        questionText = "Was bedeutet der Lichtschutzfaktor (LSF) auf Sonnencremes?",
        answerA = "Die Wasserfestigkeit der Creme",
        answerB = "Wie viel Mal laenger man sich in der Sonne aufhalten kann, ohne einen Sonnenbrand zu bekommen",
        answerC = "Die Konzentration des UV-Filters",
        answerD = "Wie stark die Creme riecht",
        correctAnswer = 1,
        explanation = "Der Lichtschutzfaktor gibt an, wie viel laenger man sich in der Sonne aufhalten kann ohne Sonnenbrand. Bei Eigenschutzzeit 10 Minuten und LSF 30 waeren das 300 Minuten theoretischer Schutz.",
        difficulty = 1,
        funFact = "LSF 15 filtert etwa 93 % der UVB-Strahlen, LSF 30 etwa 97 % und LSF 50 rund 98 % -- der Unterschied zwischen 30 und 50 ist also kleiner als viele denken."
    ),

    Question(
        categoryId = 16,
        questionText = "Wann sollte man Sonnencreme auftragen?",
        answerA = "Erst nach dem Baden",
        answerB = "Kurz bevor man in die Sonne geht, am besten 20-30 Minuten vorher",
        answerC = "Nur bei Temperaturen ueber 30 Grad",
        answerD = "Nur bei direktem Sonnenschein, nicht bei bewoelktem Himmel",
        correctAnswer = 1,
        explanation = "Sonnenschutzmittel sollten 20 bis 30 Minuten vor dem Sonnenbaden aufgetragen werden, damit der Filter richtig einziehen und seine volle Wirkung entfalten kann.",
        difficulty = 1,
        funFact = "Bis zu 80 % der UV-Strahlung durchdringt Wolken -- deshalb ist Sonnenschutz auch an bewolkten Tagen notwendig, vor allem im Sommer."
    ),

    Question(
        categoryId = 16,
        questionText = "Was ist Melanom?",
        answerA = "Ein gutartiger Leberfleck",
        answerB = "Eine Form von Hautkrebs, die aus den pigmentbildenden Zellen entsteht",
        answerC = "Ein Vitamin-D-Mangel",
        answerD = "Ein Sonnenbrand zweiten Grades",
        correctAnswer = 1,
        explanation = "Das Melanom ist der gefaehrlichste Hautkrebs, weil es fruehzeitig Metastasen bilden kann. Ursachen sind UV-Strahlung und genetische Faktoren.",
        difficulty = 1,
        funFact = "Das Melanom macht nur 4 % aller Hautkrebsarten aus, ist aber fuer 75 % aller Hautkrebstodesfaelle verantwortlich -- fruehe Erkennung ist deshalb lebensrettend."
    ),

    Question(
        categoryId = 16,
        questionText = "Was sollte man beim Beobachten von Leberflecken beachten?",
        answerA = "Leberflecken sind immer harmlos und brauchen keine Beobachtung",
        answerB = "Die ABCDE-Regel: Asymmetrie, Begrenzung, Farbe, Durchmesser, Entwicklung beachten",
        answerC = "Nur Leberflecken die jucken sind gefahrlich",
        answerD = "Leberflecken sollte man selbst mit Creme behandeln",
        correctAnswer = 1,
        explanation = "Die ABCDE-Regel hilft, verdaechtige Hautveraenderungen zu erkennen: Asymmetrie, unregelmaessige Begrenzung, ungleichmaessige Farbe, Durchmesser ueber 5 mm, Entwicklung (Veraenderung).",
        difficulty = 1,
        funFact = "In Deutschland erhalten jaehrlich rund 290.000 Menschen eine Hautkrebsdiagnose -- Hautkrebs ist damit die haeufigste Krebsart ueberhaupt."
    ),

    Question(
        categoryId = 16,
        questionText = "Welchen Lichtschutzfaktor empfehlen Experten fuer normale Sommernutzung?",
        answerA = "LSF 5",
        answerB = "LSF 10",
        answerC = "LSF 30 oder hoeher",
        answerD = "LSF 2 reicht aus",
        correctAnswer = 2,
        explanation = "Fuer den normalen Alltag und Urlaub empfehlen Dermatologen mindestens LSF 30. Bei sehr heller Haut, Hochgebirge oder intensiver Sonne sollte LSF 50 verwendet werden.",
        difficulty = 1,
        funFact = "Kinder sollten grundsaetzlich mit LSF 50 und mehr eingecremt werden -- ihre Haut ist duenner und empfindlicher als die von Erwachsenen."
    ),

    // --- Vaccination schedule basics ---

    Question(
        categoryId = 16,
        questionText = "Was ist die STIKO in Deutschland?",
        answerA = "Eine Krankenkassen-Organisation",
        answerB = "Die Staendige Impfkommission, die offizielle Impfempfehlungen herausgibt",
        answerC = "Ein Pharmaunternehmen",
        answerD = "Eine Impfpflicht-Behoerde",
        correctAnswer = 1,
        explanation = "Die STIKO (Staendige Impfkommission) am Robert Koch-Institut erarbeitet Impfempfehlungen fuer Deutschland und aktualisiert sie regelmaessig basierend auf wissenschaftlichen Erkenntnissen.",
        difficulty = 1,
        funFact = "Die STIKO wurde 1972 gegruendet und hat seither die Impflandschaft in Deutschland massgeblich gepragt -- ihre Empfehlungen sind zwar nicht gesetzlich verpflichtend, aber Grundlage fuer Kassenleistungen."
    ),

    Question(
        categoryId = 16,
        questionText = "Gegen welche Krankheit schuetzt die MMR-Impfung?",
        answerA = "Malaria, Masern und Rubeola",
        answerB = "Masern, Mumps und Roeteln",
        answerC = "Meningitis, Masern und Rotaviren",
        answerD = "Mumps, Malaria und Ringelroeteln",
        correctAnswer = 1,
        explanation = "Die MMR-Impfung schuetzt gleichzeitig gegen Masern, Mumps und Roeteln. Sie wird in Deutschland als Kombinationsimpfstoff im Kindesalter in zwei Dosen gegeben.",
        difficulty = 1,
        funFact = "Masern koennen trotz milderer Verlaufsformen gefaehrliche Spaetfolgen haben -- das Virus kann das Immungedaechtnis loeschen und Kinder dadurch anfaelliger fuer andere Infektionen machen."
    ),

    Question(
        categoryId = 16,
        questionText = "Wie lange schuetzt die Tetanus-Impfung?",
        answerA = "Lebenslang",
        answerB = "Etwa 10 Jahre",
        answerC = "Nur ein Jahr",
        answerD = "Genau 5 Jahre",
        correctAnswer = 1,
        explanation = "Der Impfschutz gegen Tetanus (Wundstarrkrampf) haelt etwa 10 Jahre. Danach ist eine Auffrischimpfung noetig, um den Schutz aufrechtzuerhalten.",
        difficulty = 1,
        funFact = "Tetanus-Bakterien kommen natuerlich im Erdboden vor -- deshalb ist eine Auffrischung besonders wichtig fuer Menschen, die im Garten oder in der Landwirtschaft arbeiten."
    ),

    Question(
        categoryId = 16,
        questionText = "Welche Impfung wird speziell aelteren Menschen ab 60 Jahren empfohlen?",
        answerA = "Masernimpfung",
        answerB = "Rotaviren-Impfung",
        answerC = "Grippe- und Pneumokokken-Impfung",
        answerD = "FSME-Impfung",
        correctAnswer = 2,
        explanation = "Die STIKO empfiehlt aelteren Menschen ab 60 Jahren jaehrliche Grippeimpfung und eine Pneumokokken-Impfung, da das Immunsystem im Alter schwaecher wird und Lungenentzuendungen gefaehrlicher sind.",
        difficulty = 1,
        funFact = "Pneumokokken sind Bakterien, die Lungenentzuendung, Hirnhautentzuendung und Blutvergiftung verursachen koennen -- bei aelteren Menschen verlaufen diese Infektionen oft schwerer."
    ),

    Question(
        categoryId = 16,
        questionText = "Was ist eine Totimpfstoff-Impfung?",
        answerA = "Eine Impfung, die nicht mehr wirkt",
        answerB = "Eine Impfung mit abgetoeteten oder inaktivierten Erregern",
        answerC = "Eine Impfung nur fuer verstorbene Erreger-Traeger",
        answerD = "Eine besonders alte Impfmethode",
        correctAnswer = 1,
        explanation = "Totimpfstoffe enthalten abgetoetete Erreger oder deren Bestandteile. Sie sind nicht infektios und koennen daher auch bei immungeschwachten Personen eingesetzt werden.",
        difficulty = 1,
        funFact = "Der Unterschied zu Lebendimpfstoffen: Lebendimpfstoffe enthalten abgeschwachte lebende Erreger und erzeugen oft eine staerkere Immunantwort, duerfen aber nicht bei immungeschwachten Personen angewendet werden."
    ),

    // --- Cancer prevention basics ---

    Question(
        categoryId = 16,
        questionText = "Welche Massnahme ist am wirksamsten zur Vorbeugung von Lungenkrebs?",
        answerA = "Viel Vitamin C zu sich nehmen",
        answerB = "Taeglich Sport treiben",
        answerC = "Nicht rauchen und Passivrauch meiden",
        answerD = "Bestimmte Tees trinken",
        correctAnswer = 2,
        explanation = "Rauchen ist fuer rund 85 % aller Lungenkrebsfaelle verantwortlich. Nicht zu rauchen und Passivrauch zu meiden ist die effektivste Vorbeugung gegen Lungenkrebs.",
        difficulty = 1,
        funFact = "Das Risiko, an Lungenkrebs zu erkranken, sinkt nach dem Rauchstopp stetig -- nach 10 Jahren ist es auf etwa die Haelfte des Risikos eines aktiven Rauchers gesunken."
    ),

    Question(
        categoryId = 16,
        questionText = "Ab welchem Alter haben Frauen in Deutschland Anspruch auf eine kostenlose Mammographie-Vorsorge?",
        answerA = "Ab 30 Jahren",
        answerB = "Ab 40 Jahren",
        answerC = "Ab 50 Jahren",
        answerD = "Ab 65 Jahren",
        correctAnswer = 2,
        explanation = "Frauen zwischen 50 und 69 Jahren haben in Deutschland alle zwei Jahre Anspruch auf eine kostenlose Mammographie im Rahmen des Brustkrebs-Frueherkennungsprogramms.",
        difficulty = 1,
        funFact = "Brustkrebs ist die haeufigste Krebserkrankung bei Frauen in Deutschland -- rund 70.000 Frauen erhalten jaehrlich diese Diagnose."
    ),

    Question(
        categoryId = 16,
        questionText = "Was ist eine Koloskopie?",
        answerA = "Eine Untersuchung der Lunge",
        answerB = "Eine Untersuchung des Dickdarms zur Fruehkennung von Darmkrebs",
        answerC = "Eine Blutuntersuchung auf Krebsmarker",
        answerD = "Eine Untersuchung der Haut auf Melanome",
        correctAnswer = 1,
        explanation = "Bei der Koloskopie (Darmspiegelung) wird ein flexibler Schlauch mit Kamera in den Dickdarm eingefuehrt. Polypen koennen sofort entfernt werden, bevor sie zu Krebs werden.",
        difficulty = 1,
        funFact = "In Deutschland hat jeder ab 50 Jahren Anspruch auf eine kostenlose Darmspiegelung -- sie kann Darmkrebs verhindern, weil Vorstufen (Polypen) direkt waehrend der Untersuchung entfernt werden."
    ),

    Question(
        categoryId = 16,
        questionText = "Welcher Lebensstilaspekt ist nachweislich krebsvorbeugend?",
        answerA = "Viel Salz essen",
        answerB = "Taeglich mehr als 10 Stunden sitzen",
        answerC = "Regelmaessige koerperliche Bewegung",
        answerD = "Kein Fruehstueck essen",
        correctAnswer = 2,
        explanation = "Regelmaessige koerperliche Aktivitaet senkt das Risiko fuer mehrere Krebsarten, darunter Darm-, Brust- und Endometriumkrebs, durch verschiedene Mechanismen.",
        difficulty = 1,
        funFact = "Die WHO empfiehlt Erwachsenen mindestens 150 bis 300 Minuten moderate koerperliche Aktivitaet pro Woche -- das entspricht rund 30 Minuten Bewegung an fuenf Tagen."
    ),

    // --- Regular check-ups, preventive medicine ---

    Question(
        categoryId = 16,
        questionText = "Ab welchem Alter empfiehlt sich ein regelmaessiger Gesundheits-Check-up beim Hausarzt?",
        answerA = "Ab 20 Jahren",
        answerB = "Ab 35 Jahren",
        answerC = "Erst ab 60 Jahren",
        answerD = "Nur nach Krankheiten",
        correctAnswer = 1,
        explanation = "In Deutschland haben gesetzlich Versicherte ab 35 Jahren alle zwei Jahre Anspruch auf einen kostenlosen Gesundheits-Check-up, der Blutdruck, Blutfette, Blutzucker und andere Werte prueft.",
        difficulty = 1,
        funFact = "Viele schwere Erkrankungen wie Diabetes, Bluthochdruck oder erhoehte Blutfette entwickeln sich schleichend ohne Symptome -- der Check-up kann sie fruehzeitig entdecken."
    ),

    Question(
        categoryId = 16,
        questionText = "Was prueft ein Haut-Krebs-Screening?",
        answerA = "Die Pigmentierung der Haut nach UV-Bestrahlung",
        answerB = "Alle Leberflecken und Hautveraenderungen auf verdaechtige Anzeichen von Hautkrebs",
        answerC = "Den Vitamin-D-Spiegel im Blut",
        answerD = "Allergien gegen Sonnencreme",
        correctAnswer = 1,
        explanation = "Beim Hautkrebsscreening untersucht der Arzt systematisch die gesamte Koerperoberflaeche nach verdaechtigen Hautveraenderungen, die auf Melanom oder anderen Hautkrebs hindeuten koennen.",
        difficulty = 1,
        funFact = "In Deutschland haben gesetzlich Versicherte ab 35 Jahren alle zwei Jahre Anspruch auf ein kostenloses Hautkrebsscreening."
    ),

    Question(
        categoryId = 16,
        questionText = "Was misst ein Cholesterin-Test?",
        answerA = "Den Zuckergehalt im Blut",
        answerB = "Die Menge von Blutfetten wie LDL- und HDL-Cholesterin",
        answerC = "Den Eisenwert im Blut",
        answerD = "Die Anzahl der weissen Blutkoerperchen",
        correctAnswer = 1,
        explanation = "Der Cholesterin-Test misst LDL- und HDL-Cholesterin sowie Triglyzeride im Blut. Erhoehtes LDL-Cholesterin erhoet das Risiko fuer Herzerkrankungen und Schlaganfall.",
        difficulty = 1,
        funFact = "HDL-Cholesterin wird als 'gutes' Cholesterin bezeichnet, weil es ueberschussiges Cholesterin aus den Arterien zur Leber transportiert und so vor Arteriosklerose schuetzt."
    ),

    Question(
        categoryId = 16,
        questionText = "Was ist ein Blutbild?",
        answerA = "Ein Foto des Herzens",
        answerB = "Eine Blutuntersuchung, die verschiedene Blutzellen und -werte misst",
        answerC = "Eine bildgebende Untersuchung der Blutgefaesse",
        answerD = "Ein Test auf Blutgruppe",
        correctAnswer = 1,
        explanation = "Ein Blutbild analysiert die Zusammensetzung des Blutes: Anzahl roter und weisser Blutkoerperchen, Haemoglobin, Thrombozyten usw. Es gibt Hinweise auf Infektionen, Anaemie und andere Erkrankungen.",
        difficulty = 1,
        funFact = "Ein einziger Milliliter Blut enthaelt rund 5 Millionen rote Blutkoerperchen, bis zu 10.000 weisse Blutkoerperchen und 250.000 Blutplaettchen."
    ),

    Question(
        categoryId = 16,
        questionText = "Warum ist Pap-Abstrich-Vorsorge fuer Frauen wichtig?",
        answerA = "Er misst den Hormonspiegel",
        answerB = "Er erkennt Gebaermutterhalskrebs und Vorstufen fruehzeitig",
        answerC = "Er prueft die Funktion der Eierstoecke",
        answerD = "Er misst den Blutdruck in der Gebaermutter",
        correctAnswer = 1,
        explanation = "Der Pap-Abstrich entnimmt Zellen vom Gebaermutterhals und untersucht sie auf krankhafte Veraenderungen. So kann Gebaermutterhalskrebs erkannt werden, bevor er sich entwickelt.",
        difficulty = 1,
        funFact = "Durch konsequente Vorsorge (Pap-Abstrich plus HPV-Impfung) koennte Gebaermutterhalskrebs nahezu vollstaendig eliminiert werden -- die WHO hat die globale Eliminierung als Ziel ausgerufen."
    ),

    // --- Healthy aging basics ---

    Question(
        categoryId = 16,
        questionText = "Was ist Osteoporose?",
        answerA = "Eine Entzuendung der Gelenke im Alter",
        answerB = "Ein Abbau der Knochenmasse, der die Knochen bruechig macht",
        answerC = "Eine Muskelschwaeche im Alter",
        answerD = "Eine Erkrankung der Blutgefaesse",
        correctAnswer = 1,
        explanation = "Bei Osteoporose nimmt die Knochendichte ab, wodurch die Knochen poroes und bruechig werden. Frauen nach der Menopause sind besonders haeufig betroffen.",
        difficulty = 1,
        funFact = "In Deutschland sind rund 8 Millionen Menschen von Osteoporose betroffen -- jaehrlich ereignen sich deshalb etwa 700.000 Knochenbrueche, darunter viele Hueftfrakturen bei aelteren Menschen."
    ),

    Question(
        categoryId = 16,
        questionText = "Welches Vitamin ist fuer starke Knochen und die Aufnahme von Kalzium besonders wichtig?",
        answerA = "Vitamin C",
        answerB = "Vitamin D",
        answerC = "Vitamin B12",
        answerD = "Vitamin A",
        correctAnswer = 1,
        explanation = "Vitamin D ist entscheidend fuer die Aufnahme von Kalzium aus der Nahrung und den Einbau in die Knochen. Ein Mangel fuehrt zu Knochenschwaeche (Osteomalazie) und erhoehtem Osteoporoserisiko.",
        difficulty = 1,
        funFact = "Der Koerper kann Vitamin D durch Sonneneinstrahlung selbst herstellen -- 15 bis 20 Minuten Sonnenlicht auf Gesicht und Arme taeglich reichen oft aus, um den Bedarf zu decken."
    ),

    Question(
        categoryId = 16,
        questionText = "Was versteht man unter kognitiver Reserve im Alter?",
        answerA = "Das Speichern von Erinnerungen in der Kindheit",
        answerB = "Die Faehigkeit des Gehirns, altersbedingte Veraenderungen durch lebenslange geistige Aktivitaet auszugleichen",
        answerC = "Der Blutdruck im Gehirn",
        answerD = "Die Menge an Schlaf, die aeltere Menschen benoetigen",
        correctAnswer = 1,
        explanation = "Kognitive Reserve bezeichnet die Widerstandsfaehigkeit des Gehirns gegenueber altersbedingten oder krankhaften Veraenderungen. Sie wird durch Bildung, geistige Aktivitaet und soziale Kontakte aufgebaut.",
        difficulty = 1,
        funFact = "Menschen mit hoeher kognitiver Reserve zeigen klinische Demenzsymptome spaeter -- obwohl ihre Gehirnveraenderungen aehnlich schwer sind wie bei anderen Betroffenen."
    ),

    Question(
        categoryId = 16,
        questionText = "Warum sind Sehnenrisse und Gelenkprobleme im Alter haeufiger?",
        answerA = "Weil aeltere Menschen zu viel Sport treiben",
        answerB = "Weil Sehnen und Knorpel mit dem Alter an Elastizitaet und Regenerationsfaehigkeit verlieren",
        answerC = "Weil im Alter zu wenig Kalzium aufgenommen wird",
        answerD = "Weil das Immunsystem Gelenke angreift",
        correctAnswer = 1,
        explanation = "Im Alter verlieren Sehnen, Baender und Knorpel an Wassergehalt und Elastizitaet. Die Regeneration verlangsamt sich, was Verletzungen wahrscheinlicher und Heilung langsamer macht.",
        difficulty = 1,
        funFact = "Regelmaessiges Kraft- und Dehnungstraining kann den altersbedingten Abbau von Sehnen und Muskeln deutlich verlangsamen -- auch im hohen Alter profitiert der Koerper noch von Training."
    ),

    Question(
        categoryId = 16,
        questionText = "Was ist Arteriosklerose?",
        answerA = "Eine Entzuendung der Herzklappen",
        answerB = "Die Verhaertung und Verengung der Blutgefaesse durch Ablagerungen",
        answerC = "Ein Herzrhythmusfehler",
        answerD = "Eine Erweiterung der Hauptschlagader",
        correctAnswer = 1,
        explanation = "Bei der Arteriosklerose lagern sich Fette, Kalzium und andere Stoffe in den Arterienwandungen ab. Die Gefaesse verengen sich, das Herzinfarkt- und Schlaganfallrisiko steigt.",
        difficulty = 1,
        funFact = "Arteriosklerose beginnt schon in jungen Jahren schleichend -- Ablagerungen in den Arterien wurden selbst bei jungen Soldaten gefunden, die im Koreakrieg fielen."
    ),

    Question(
        categoryId = 16,
        questionText = "Welche Massnahme hilft, das Demenzrisiko im Alter zu senken?",
        answerA = "So viel wie moeglich schlafen",
        answerB = "Soziale Kontakte, koerperliche Aktivitaet und geistige Herausforderungen",
        answerC = "Vitamin-E-Tabletten in hohen Dosen",
        answerD = "Kaffe trinken meiden",
        correctAnswer = 1,
        explanation = "Studien zeigen, dass regelmaessige Bewegung, soziale Einbindung und geistige Aktivitaet das Demenzrisiko deutlich senken koennen. Diese Faktoren foerdern die Gehirngesundheit.",
        difficulty = 1,
        funFact = "Soziale Isolation ist ein aehnlich starker Risikofaktor fuer Demenz wie Bluthochdruck oder Rauchen -- regelmaessige Kontakte zu anderen Menschen schuetzen das alternde Gehirn nachweislich."
    ),

    Question(
        categoryId = 16,
        questionText = "Wie viele Stunden Schlaf brauchen Erwachsene laut Experten pro Nacht?",
        answerA = "4-5 Stunden",
        answerB = "6-7 Stunden",
        answerC = "7-9 Stunden",
        answerD = "10-12 Stunden",
        correctAnswer = 2,
        explanation = "Die meisten Erwachsenen benoetigen 7 bis 9 Stunden Schlaf pro Nacht fuer optimale Gesundheit. Chronischer Schlafmangel erhoet das Risiko fuer Herzerkrankungen, Diabetes und Depressionen.",
        difficulty = 1,
        funFact = "Im Schlaf werden Abfallprodukte des Gehirnstoffwechsels abgebaut -- chronischer Schlafmangel koennte deshalb zur Anhaufung von Proteinen beitragen, die mit Alzheimer in Verbindung stehen."
    ),

    Question(
        categoryId = 16,
        questionText = "Was empfehlen Aerzte zur Sturzpraevention bei aelteren Menschen?",
        answerA = "So wenig wie moeglich laufen",
        answerB = "Gleichgewichtsuebungen, Krafttraining und Sicherheitsanpassungen im Haus",
        answerC = "Taeglich Aspirin nehmen",
        answerD = "Nur mit Rollator gehen",
        correctAnswer = 1,
        explanation = "Gleichgewichtsuebungen (z.B. Tai-Chi), Muskelkrafttraining und Anpassungen im Wohnbereich (Haltegriffe, rutschfeste Matten) senken das Sturzrisiko bei aelteren Menschen nachweislich.",
        difficulty = 1,
        funFact = "Sturze sind die haeufigste Ursache fuer Verletzungs-bedingte Todesfaelle bei Menschen ueber 65 Jahren -- und die meisten lassen sich durch gezieltes Training verhindern."
    ),

    Question(
        categoryId = 16,
        questionText = "Was ist eine Grippeschutzimpfung und fuer wen ist sie besonders wichtig?",
        answerA = "Eine einmalige Impfung fuer Kinder bis 12 Jahre",
        answerB = "Eine jaehrlich angepasste Impfung, besonders wichtig fuer Risikogruppen wie aeltere Menschen und Chronischkranke",
        answerC = "Eine Impfung gegen alle Erkältungsviren",
        answerD = "Eine Impfung, die lebenslangen Schutz bietet",
        correctAnswer = 1,
        explanation = "Die Grippeimpfung wird jaehrlich an die umlaufenden Grippeviren angepasst. Sie ist besonders wichtig fuer aeltere Menschen, Chronischkranke und Schwangere, da bei ihnen schwere Verlaeufe haeufiger sind.",
        difficulty = 1,
        funFact = "Das Grippevirus veraendert sich so schnell, dass jedes Jahr eine neue Impfung noetig ist -- Experten der WHO beobachten taeglich weltweit, welche Virusvarianten sich ausbreiten, um den naechsten Impfstoff vorzubereiten."
    ),

    Question(
        categoryId = 16,
        questionText = "Was ist der PSA-Test fuer Maenner?",
        answerA = "Ein Bluttest auf Prostatakrebs-Risiko",
        answerB = "Ein Test auf Sexualhormone",
        answerC = "Ein Herztest fuer Maenner ab 50",
        answerD = "Ein Nierenfunktionstest",
        correctAnswer = 0,
        explanation = "Der PSA-Test misst das prostataspezifische Antigen im Blut. Ein erhoehter Wert kann auf Prostatakrebs hinweisen, hat aber auch andere Ursachen und muss aerztlich bewertet werden.",
        difficulty = 1,
        funFact = "Prostatakrebs ist die haeufigste Krebserkrankung bei Maennern in Deutschland -- rund 65.000 Maenner erkranken jaehrlich daran."
    ),

    Question(
        categoryId = 16,
        questionText = "Was ist Zahnfleischentzuendung (Gingivitis)?",
        answerA = "Eine Infektion des Zahnnervs",
        answerB = "Eine Entzuendung des Zahnfleisches durch Plaquebelaege",
        answerC = "Ein Riss im Zahnschmelz",
        answerD = "Eine Erkrankung der Speicheldrüsen",
        correctAnswer = 1,
        explanation = "Gingivitis ist eine Entzuendung des Zahnfleisches, verursacht durch Bakterien in Zahnbelaegen. Sie aeussert sich durch Roetung, Schwellung und Blutungen beim Zaehneputzen.",
        difficulty = 1,
        funFact = "Unbehandelte Gingivitis kann sich zu Parodontitis weiterentwickeln -- einer schweren Erkrankung, die Knochen und Halteapparat des Zahnes zerstoert und zum Zahnverlust fuehren kann."
    ),

    Question(
        categoryId = 16,
        questionText = "Was schuetzt die Haut vor UV-Schaden, wenn man keinen Sonnenschutz traegt?",
        answerA = "Vitamin C in der Haut",
        answerB = "Das Pigment Melanin, das die Haut braeunt",
        answerC = "Das Fettgewebe unter der Haut",
        answerD = "Die Schweissdruesen",
        correctAnswer = 1,
        explanation = "Melanin ist das braune Pigment, das die Haut vor UV-Strahlung schuetzt. Die Bräune nach Sonneneinstrahlung ist eine Schutzreaktion des Körpers -- aber kein vollstaendiger Schutz.",
        difficulty = 1,
        funFact = "Eine Bräune entspricht nur ungefaehr LSF 3 bis 4 -- also einem minimalen Schutz, der keinen ausreichenden Schutz vor Hautkrebs bietet."
    ),

    Question(
        categoryId = 16,
        questionText = "Welche Farbe sollte ein gesundes Zahnfleisch haben?",
        answerA = "Tiefrot und glaenzend",
        answerB = "Hellrosa und fest",
        answerC = "Weiss mit gelblichem Ton",
        answerD = "Dunkelbraun",
        correctAnswer = 1,
        explanation = "Gesundes Zahnfleisch ist hellrosa, fest und blutet nicht beim Zaehneputzen. Rotes, geschwollenes oder blutendes Zahnfleisch deutet auf eine Entzuendung hin.",
        difficulty = 1,
        funFact = "Zahnfleischerkrankungen wurden in Studien mit erhoehtem Risiko fuer Herzerkrankungen und Schlaganfall in Verbindung gebracht -- die Mundhygiene hat also Auswirkungen auf den gesamten Koerper."
    ),

)
