package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun mixedQuestionsEasy4(): List<Question> = listOf(

    // Question 1 — Kurioses Gesetz: Longyearbyen
    Question(
        categoryId = 11,
        questionText = "In der norwegischen Stadt Longyearbyen auf Spitzbergen ist es per Gesetz verboten — was?",
        answerA = "Hunde an die Leine zu legen",
        answerB = "Zu sterben",
        answerC = "Alkohol zu trinken",
        answerD = "Autos zu fahren",
        correctAnswer = 1,
        explanation = "In Longyearbyen ist es seit 1950 gesetzlich verboten zu sterben. Der Permafrostboden laesst keine normale Verwesung zu, daher muss man sich zum Sterben aufs norwegische Festland begeben.",
        difficulty = 1,
        funFact = "Wer in Longyearbyen schwer krank wird, wird per Flugzeug auf das Festland ausgeflogen — denn im Boden des Ortes bleiben Leichen fuer Jahrhunderte unveraendert erhalten."
    ),

    // Question 2 — Tierrekord: Gepard
    Question(
        categoryId = 11,
        questionText = "Welche Hoechstgeschwindigkeit kann ein Gepard beim Sprint erreichen?",
        answerA = "Bis zu 60 km/h",
        answerB = "Bis zu 80 km/h",
        answerC = "Bis zu 100 km/h",
        answerD = "Bis zu 120 km/h",
        correctAnswer = 2,
        explanation = "Der Gepard ist das schnellste Landtier der Welt und erreicht Spitzengeschwindigkeiten von bis zu 100 km/h. Allerdings kann er dieses Tempo nur fuer wenige Sekunden halten.",
        difficulty = 1,
        funFact = "Beim Sprint hebt ein Gepard alle vier Pfoten gleichzeitig vom Boden ab — zweimal pro Schritt. Das macht seinen Lauf so unglaublich effizient."
    ),

    // Question 3 — Fun Fact: Nasenloecher
    Question(
        categoryId = 11,
        questionText = "Durch wie viele Nasenloecher atmet ein Mensch in der Regel gleichzeitig?",
        answerA = "Immer durch beide gleichzeitig",
        answerB = "Immer nur durch das linke",
        answerC = "Immer nur durch das rechte",
        answerD = "Immer nur durch eines, das wechselt alle paar Minuten",
        correctAnswer = 3,
        explanation = "Der Mensch atmet tatsaechlich fast immer nur durch ein Nasenloch zur gleichen Zeit. Die beiden Nasenloecher wechseln sich im sogenannten nasalen Zyklus automatisch ab — etwa alle zwei bis drei Stunden.",
        difficulty = 1,
        funFact = "Diesen Wechsel reguliert das autonome Nervensystem unbewusst. Nur bei koerperlicher Anstrengung oder Erkrankungen benutzt man beide Nasenloecher gleichzeitig."
    ),

    // Question 4 — Rekord: Masskrug-Tragen
    Question(
        categoryId = 11,
        questionText = "Welchen Guinness-Rekord stellte der Deutsche Oliver Struempfel auf?",
        answerA = "Laengster Bierkrug-Schluck ohne Pause",
        answerB = "Tragen von 29 gefuellten Masskruegen ueber 40 Meter",
        answerC = "Trinken von 1 Liter Bier in unter 5 Sekunden",
        answerD = "Stapeln von 15 Bierkruegen aufeinander",
        correctAnswer = 1,
        explanation = "Oliver Struempfel trug 2017 insgesamt 29 gefuellte Masskruege (zusammen fast 70 kg) ueber eine Strecke von 40 Metern und stellte damit einen neuen Guinness-Weltrekord auf.",
        difficulty = 1,
        funFact = "Einen Masskrug mit einem Liter Bier zu tragen klingt einfach — aber 29 davon gleichzeitig sicher 40 Meter weit zu schleppen erfordert jahrelanges Training und aussergewoehnliche Kraft."
    ),

    // Question 5 — Kurioses: Albaner Kopfnicken
    Question(
        categoryId = 11,
        questionText = "Was bedeutet Kopfnicken in Albanien?",
        answerA = "Ja, ich stimme zu",
        answerB = "Ich bin muede",
        answerC = "Nein, ich lehne ab",
        answerD = "Ich verstehe nicht",
        correctAnswer = 2,
        explanation = "In Albanien ist die Bedeutung von Kopfnicken und Kopfschuetteln umgekehrt: Nicken bedeutet Nein, Kopfschuetteln bedeutet Ja. Das fuehrt bei Touristen haeufig zu Missverstaendnissen.",
        difficulty = 1,
        funFact = "Auch in Teilen Griechenlands, der Tuerkei und Sueditalien gibt es historisch abweichende Gesten fuer Ja und Nein — ein Kulturschock fuer jeden Reisenden."
    ),

    // Question 6 — Fun Fact: Blut und Gold
    Question(
        categoryId = 11,
        questionText = "Was befindet sich in winzigen Mengen im menschlichen Blut?",
        answerA = "Silber",
        answerB = "Platin",
        answerC = "Gold",
        answerD = "Kupfer",
        correctAnswer = 2,
        explanation = "Menschliches Blut enthaelt tatsaechlich kleinste Mengen Gold — etwa 0,2 Milligramm im gesamten Koerper. Gold ist ein natuerlicher Bestandteil des menschlichen Blutes.",
        difficulty = 1,
        funFact = "Der Goldgehalt im Blut aller Menschen der Erde zusammen wuerde gerade mal 8 Kilogramm ergeben — nicht genug fuer einen einzigen Goldbarren."
    ),

    // Question 7 — Rekord: Laengster Kuss
    Question(
        categoryId = 11,
        questionText = "Wie lange dauerte der laengste dokumentierte Kuss der Welt?",
        answerA = "Ueber 12 Stunden",
        answerB = "Ueber 24 Stunden",
        answerC = "Ueber 48 Stunden",
        answerD = "Ueber 58 Stunden",
        correctAnswer = 3,
        explanation = "Das thailaendische Paar Ekkachai und Laksana Tiranarat kuessste sich 58 Stunden, 35 Minuten und 58 Sekunden — mehr als zwei volle Tage — ohne Pause und durchgehend im Stehen.",
        difficulty = 1,
        funFact = "Weder Essen, noch Schlafen, noch Pausen waren erlaubt. Die beiden mussten jede Sekunde Lippenberueherung aufrechterhalten, um den Rekord nicht zu verlieren."
    ),

    // Question 8 — Tierrekord: Giraffe
    Question(
        categoryId = 11,
        questionText = "Wie viele Halswirbel hat eine Giraffe?",
        answerA = "14 — doppelt so viele wie beim Menschen",
        answerB = "7 — genauso viele wie beim Menschen",
        answerC = "21 — dreimal so viele wie beim Menschen",
        answerD = "28 — viermal so viele wie beim Menschen",
        correctAnswer = 1,
        explanation = "Giraffen haben genau 7 Halswirbel — genau wie der Mensch. Der Unterschied besteht nur darin, dass jeder einzelne Halswirbel bei der Giraffe bis zu 28 Zentimeter lang sein kann.",
        difficulty = 1,
        funFact = "Fast alle Saeugetiere — von der Maus bis zum Wal — haben genau 7 Halswirbel. Nur wenige Ausnahmen wie Faultiere und Seekuehe weichen davon ab."
    ),

    // Question 9 — Kurioses Gesetz: England Briefmarke
    Question(
        categoryId = 11,
        questionText = "Was gilt in England als Landesverrat beim Briefmarken-Kleben?",
        answerA = "Eine Briefmarke mit falscher Zunge ankleben",
        answerB = "Eine Briefmarke verbrennen",
        answerC = "Eine Briefmarke mit dem Koenig/der Koenigin kopfueber aufkleben",
        answerD = "Eine Briefmarke zweifach aufkleben",
        correctAnswer = 2,
        explanation = "In England gilt es offiziell als Hochverrat, eine Briefmarke mit dem Abbild des Monarchen kopfueber aufzukleben. Dieses Gesetz ist noch immer formal gueltig, wird aber kaum noch verfolgt.",
        difficulty = 1,
        funFact = "In der Briefmarkensammler-Szene gibt es tatsaechlich Faelle, in denen Menschen auf diesen Paragraphen hingewiesen wurden — auch wenn niemand wegen kopfueber geklebter Briefmarken ins Gefaengnis kam."
    ),

    // Question 10 — Fun Fact: Orange Reim
    Question(
        categoryId = 11,
        questionText = "Was ist das Besondere am deutschen Wort \"Orange\"?",
        answerA = "Es ist das laengste einsilbige Wort der deutschen Sprache",
        answerB = "Es reimt sich auf kein anderes deutsches Wort",
        answerC = "Es wurde erst 1900 in den Duden aufgenommen",
        answerD = "Es ist das einzige Wort mit drei Vokalen hintereinander",
        correctAnswer = 1,
        explanation = "Auf das Wort 'Orange' reimt sich tatsaechlich kein einziges anderes gebraeuchliches deutsches Wort. Das macht es zu einer Rarstaet in der deutschen Sprache.",
        difficulty = 1,
        funFact = "Im Englischen hat 'orange' dasselbe Problem — auch dort gibt es kein gebraeuchliches Reimwort. Dichter mussten oft auf Kunstwoerter oder Konstruktionen ausweichen."
    ),

    // Question 11 — Tierrekord: Blauwal
    Question(
        categoryId = 11,
        questionText = "Was ist der Blauwal?",
        answerA = "Das schnellste Meerestier",
        answerB = "Das giftigste Tier der Welt",
        answerC = "Das groesste und schwerste Tier, das je auf der Erde lebte",
        answerD = "Das laengstlebende Tier der Welt",
        correctAnswer = 2,
        explanation = "Der Blauwal ist nicht nur das groesste heute lebende Tier, sondern das groesste und schwerste Tier, das jemals auf der Erde existiert hat. Er wird bis zu 33 Meter lang und bis zu 190 Tonnen schwer.",
        difficulty = 1,
        funFact = "Das Herz eines Blauwals ist so gross wie ein Kleinauto — und pumpt waehrend eines Tauchgangs nur 5 bis 6 Mal pro Minute."
    ),

    // Question 12 — Kurioses: Otter Werkzeug
    Question(
        categoryId = 11,
        questionText = "Wozu benutzen Otter den Stein, den sie in einer Hauttasche bei sich tragen?",
        answerA = "Als Anker, um auf dem Wasser zu schlafen",
        answerB = "Als Waffe gegen Fressfeinde",
        answerC = "Als Gewicht zum Tauchen",
        answerD = "Als Werkzeug, um Muschelschalen aufzubrechen",
        correctAnswer = 3,
        explanation = "Seeotter tragen unter ihren Vorderbeinen einen Lieblingsstein, den sie benutzen, um Muschelschalen auf ihrem Bauch aufzuklopfen. Es ist eines der bekanntesten Beispiele fuer Werkzeuggebrauch bei Tieren.",
        difficulty = 1,
        funFact = "Otter halten sich beim Schlafen im Wasser oft an den Haenden — sogenanntes 'Rafting' — damit sie nicht voneinander getrennt werden."
    ),

    // Question 13 — Weltrekord: Uruguay Grill
    Question(
        categoryId = 11,
        questionText = "Welches Land haelt den Guinness-Rekord fuer das groesste Barbecue der Welt?",
        answerA = "Argentinien",
        answerB = "Brasilien",
        answerC = "USA",
        answerD = "Uruguay",
        correctAnswer = 3,
        explanation = "Uruguay haelt den Guinness-Rekord fuer das groesste Barbecue der Welt: 1.252 Grillspiesze und 12.000 Kilogramm Fleisch wurden gleichzeitig zubereitet.",
        difficulty = 1,
        funFact = "Das groesste BBQ der Welt fand in Montevideo statt. Uruguay hat pro Kopf einen der hoechsten Fleischkonsum weltweit — rund 100 kg Fleisch pro Person pro Jahr."
    ),

    // Question 14 — Fun Fact: Post-it
    Question(
        categoryId = 11,
        questionText = "Wie wurde der Post-it-Klebezettel zufaellig erfunden?",
        answerA = "Ein Chemiker wollte superstark klebenden Leim entwickeln, aber der Kleber haftete nur schwach",
        answerB = "Ein Burokaufmann verlor staendig seine Notizzettel und suchte eine Loesung",
        answerC = "Eine Sekretaerin klebte versehentlich Zettel mit normalem Kleber an Akten",
        answerD = "Ein Schulkind erfand es beim Basteln",
        correctAnswer = 0,
        explanation = "Spencer Silver wollte bei 3M einen extrem starken Klebstoff entwickeln — das Ergebnis war jedoch nur ein sehr schwach haftender Kleber. Sein Kollege Art Fry erkannte die Idee dahinter und entwickelte daraus den Post-it.",
        difficulty = 1,
        funFact = "Post-its wurden zuerst intern bei 3M genutzt — als Lesezeichen in Gesangbuechern. Erst nach einer Testphase in 11 US-Staedten kam das Produkt 1980 auf den Markt."
    ),

    // Question 15 — Kurioses: Kleinste Stadt
    Question(
        categoryId = 11,
        questionText = "Wo befindet sich angeblich die kleinste Stadt der Welt mit nur 30 Einwohnern?",
        answerA = "Slowenien",
        answerB = "Kroatien",
        answerC = "Bosnien",
        answerD = "Montenegro",
        correctAnswer = 1,
        explanation = "Der kroatische Ort Hum gilt als kleinste Stadt der Welt und hat nur rund 30 Einwohner — hat aber alle Merkmale einer Stadt, einschliesslich einer Stadtmauer und einem Buergermeister.",
        difficulty = 1,
        funFact = "Hum wurde im Mittelalter erbaut und hat sein mittelalterliches Stadtbild fast vollstaendig erhalten. Der Ort ist fuer seinen lokalen Trauben-Schnaps 'Biska' bekannt."
    ),

    // Question 16 — Rekord: Laengste Zunge
    Question(
        categoryId = 11,
        questionText = "Welchen Rekord haelt Chanel Tapper laut Guinness World Records?",
        answerA = "Laengste Finger der Welt",
        answerB = "Laengste weibliche Zunge der Welt mit 9,8 cm",
        answerC = "Breiteste Zunge aller Zeiten",
        answerD = "Schnellstes Zungenrollen bei einer Frau",
        correctAnswer = 1,
        explanation = "Chanel Tapper haelt den Guinness-Weltrekord fuer die laengste Zunge bei einer Frau: 9,8 Zentimeter von der Zungenspitze bis zur Mitte der geschlossenen Lippen gemessen.",
        difficulty = 1,
        funFact = "Die durchschnittliche menschliche Zunge ist etwa 8 cm lang — aber nur der Teil, der sichtbar ist, ist rund 10 cm kuerzer als die gesamte Zunge im Mund."
    ),

    // Question 17 — Tierrekord: Floh
    Question(
        categoryId = 11,
        questionText = "Auf das Wievielfache seiner eigenen Koerperhoehe kann ein Floh springen?",
        answerA = "Das 50-fache",
        answerB = "Das 100-fache",
        answerC = "Das 150-fache",
        answerD = "Das 200-fache",
        correctAnswer = 3,
        explanation = "Ein Floh kann auf das bis zu 200-fache seiner eigenen Koerperhoehe springen. Das entspricht beim Menschen einem Sprung von rund 300 bis 400 Metern Hoehe.",
        difficulty = 1,
        funFact = "Der Grund fuer diesen unglaublichen Sprung liegt in einem besonderen Protein namens Resilin, das wie ein hocheffizienter biologischer Gummi funktioniert."
    ),

    // Question 18 — Kurioses: Pilot Essen
    Question(
        categoryId = 11,
        questionText = "Warum essen Pilot und Co-Pilot im Flugzeug niemals dasselbe Gericht?",
        answerA = "Aus Fairness, damit beide ein anderes Menü bekommen",
        answerB = "Wegen Allergievorschriften der Fluggesellschaft",
        answerC = "Um das Risiko zu minimieren, dass beide gleichzeitig eine Lebensmittelvergiftung bekommen",
        answerD = "Aus Kostengruenden — beide Mahlzeiten muessen unterschiedlich sein",
        correctAnswer = 2,
        explanation = "Es ist eine Sicherheitsregel der Luftfahrt: Pilot und Co-Pilot essen im Flugzeug niemals dasselbe, damit nicht beide gleichzeitig durch eine Lebensmittelvergiftung handlungsunfaehig werden.",
        difficulty = 1,
        funFact = "Diese Regel gilt in manchen Gesellschaften auch fuer bestimmte Zeitabstaende vor dem Flug — Piloten duerfen manche Speisen bereits Stunden vor dem Start nicht essen."
    ),

    // Question 19 — Weltrekord: Ohrhaar
    Question(
        categoryId = 11,
        questionText = "Wie lang war das laengste Ohrhaar der Welt laut Guinness World Records?",
        answerA = "Knapp 10 cm",
        answerB = "Knapp 18 cm",
        answerC = "Knapp 28 cm",
        answerD = "Knapp 38 cm",
        correctAnswer = 2,
        explanation = "Radha Kant Bajpai aus Indien haelt den Guinness-Rekord fuer das laengste Ohrhaar der Welt: 27,9 Zentimeter! Die Haare wachsen aus dem Inneren des Ohres.",
        difficulty = 1,
        funFact = "Bajpai hat seine Ohrhaare nie geschnitten und liess sie jahrzehntelang wachsen. In Indien gelten lange Ohrhaare mancherorts als Zeichen langer Lebenserfahrung."
    ),

    // Question 20 — Fun Fact: Augaepfel hervorwaelzen
    Question(
        categoryId = 11,
        questionText = "Welchen skurrilen Guinness-Rekord haelt Williams Martin Sanchez Lopez aus Uruguay?",
        answerA = "Weitestes Herausstrecken der Zunge",
        answerB = "Groesste natuerliche Pupillen",
        answerC = "Hervorwaelzen der Augaepfel 19 mm ueber die Augenhoehlenkanten hinaus",
        answerD = "Hoechste Sehsschaerfe eines Menschen",
        correctAnswer = 2,
        explanation = "Williams Martin Sanchez Lopez schaffte es, seine Augaepfel 19 Millimeter ueber die Augenhoehlenkante hinaus hervorzuwaelzen — ein bizarrer, aber offiziell anerkannter Guinness-Weltrekord.",
        difficulty = 1,
        funFact = "Diese Faehigkeit nennt sich 'Globe Luxation' und ist eine seltene anatomische Besonderheit. Sie ist harmlos, sieht aber fuer Zuschauer extrem ungewohnt aus."
    ),

    // Question 21 — Geografie: Russland und China Nachbarlaender
    Question(
        categoryId = 11,
        questionText = "Welche Laender teilen sich den Weltrekord fuer die meisten Nachbarlaender?",
        answerA = "Deutschland und Frankreich",
        answerB = "Brasilien und Indien",
        answerC = "Russland und China",
        answerD = "Kanada und Australien",
        correctAnswer = 2,
        explanation = "Russland und China halten gemeinsam den Rekord fuer die meisten Nachbarlaender: Beide grenzen an jeweils 14 Laender.",
        difficulty = 1,
        funFact = "Russland ist mit 17,1 Millionen Quadratkilometern das groesste Land der Welt — kein Wunder, dass es an so viele andere Laender grenzt."
    ),

    // Question 22 — Fun Fact: Schnabeltier Zaehne
    Question(
        categoryId = 11,
        questionText = "Mit was werden Schnabeltiere geboren, das sie spaeter verlieren?",
        answerA = "Mit einem echten Schnabel aus Knochen",
        answerB = "Mit Zaehnchen",
        answerC = "Mit Schuppen auf dem Ruecken",
        answerD = "Mit offenen Augen",
        correctAnswer = 1,
        explanation = "Schnabeltiere werden mit kleinen Zaehnen geboren, die sie jedoch im Laufe ihrer Entwicklung wieder verlieren. Als erwachsene Tiere haben sie gar keine Zaehne mehr.",
        difficulty = 1,
        funFact = "Das Schnabeltier ist eines der seltsamsten Tiere der Welt: Es ist ein Saeugetier, legt aber Eier, hat einen Entenschnabel, Biberschwanz und Otterpfoten — und das Maennchen hat giftige Spornen an den Hinterbeinen."
    ),

    // Question 23 — Kurioses: Kolumbien Schmetterlinge
    Question(
        categoryId = 11,
        questionText = "Wie viele Schmetterlingsarten gibt es in Kolumbien — dem Land mit den meisten Schmetterlingsarten pro Quadratkilometer?",
        answerA = "Rund 500 Arten",
        answerB = "Rund 1.000 Arten",
        answerC = "Rund 2.000 Arten",
        answerD = "Rund 3.500 Arten",
        correctAnswer = 3,
        explanation = "Kolumbien beherbergt rund 3.500 Schmetterlingsarten und gilt als das Land mit der groessten Schmetterlingsvielfalt pro Quadratkilometer weltweit.",
        difficulty = 1,
        funFact = "Kolumbien hat generell eine der artenreichsten Tierwelten der Erde: 1.870 Vogelarten und 700 Amphibienarten leben dort — mehr als in fast jedem anderen Land."
    ),

    // Question 24 — Tierrekord: Oktopus Herzen
    Question(
        categoryId = 11,
        questionText = "Wie viele Herzen hat ein Oktopus?",
        answerA = "Eines wie der Mensch",
        answerB = "Zwei",
        answerC = "Drei",
        answerD = "Vier",
        correctAnswer = 2,
        explanation = "Ein Oktopus hat drei Herzen: Zwei Kiemenherzen pumpen Blut durch die Kiemen, das dritte Systemherz pumpt das sauerstoffreiche Blut durch den restlichen Koerper.",
        difficulty = 1,
        funFact = "Das Blut von Oktopussen ist blau — es enthaelt Haemocyanin statt Haemoglobin. Blau ist eine sehr effiziente Sauerstofftraeger-Farbe in kaltem Tiefseewasser."
    ),

    // Question 25 — Kurioses: Kuehl Haie vs. Kuehe
    Question(
        categoryId = 11,
        questionText = "Welche Tiere toeten jaehrlich mehr Menschen als Haie?",
        answerA = "Delphine",
        answerB = "Krokodile",
        answerC = "Kueue",
        answerD = "Grosser weisser Hai",
        correctAnswer = 2,
        explanation = "Kueue toeten weltweit jaehrlich mehr Menschen als Haie. Haeufig trampeln sie Menschen nieder, die ihnen zu nahe kommen oder ihre Kaelber bedrohen.",
        difficulty = 1,
        funFact = "Weltweit toeten Moskitos jaehrlich die meisten Menschen durch die von ihnen uebertragenen Krankheiten — weit mehr als alle anderen Tiere zusammen."
    ),

    // Question 26 — Fun Fact: Kassenwartezeit
    Question(
        categoryId = 11,
        questionText = "Wie lange wartet man in Deutschland durchschnittlich an einer Supermarktkasse?",
        answerA = "Ca. 1 Minute",
        answerB = "Ca. 3 Minuten",
        answerC = "Ca. 7 Minuten",
        answerD = "Ca. 15 Minuten",
        correctAnswer = 2,
        explanation = "Laut Studien wartet man in Deutschland durchschnittlich etwa 7 Minuten an einer Supermarktkasse — ein erstaunlich langer Zeitraum, der sich im Laufe eines Lebens zu Wochen summiert.",
        difficulty = 1,
        funFact = "Wer 3x pro Woche einkauft und dabei je 7 Minuten wartet, verbringt im Laufe eines 70-jaehrigen Lebens mehr als 8 komplette Tage an der Supermarktkasse."
    ),

    // Question 27 — Weltrekord: Chinesische Mauer
    Question(
        categoryId = 11,
        questionText = "Wie lang ist die Chinesische Mauer insgesamt?",
        answerA = "Ca. 5.000 km",
        answerB = "Ca. 10.000 km",
        answerC = "Ca. 15.000 km",
        answerD = "Ca. 21.000 km",
        correctAnswer = 3,
        explanation = "Die Chinesische Mauer ist insgesamt rund 21.196 Kilometer lang — wenn man alle Abschnitte, Verzweigungen und Nebenmauren zusammenrechnet.",
        difficulty = 1,
        funFact = "Die Chinesische Mauer ist entgegen einem weit verbreiteten Irrglauben NICHT mit blossem Auge aus dem Weltall sichtbar. Das ist ein hartnaeeckiges Missverstaendnis, das selbst Astronauten korrigierten."
    ),

    // Question 28 — Kurioses: Liverpool Gesetz
    Question(
        categoryId = 11,
        questionText = "In welchem Laden darf eine Frau laut einem alten britischen Gesetz barbusig arbeiten?",
        answerA = "In einem Friseursalon",
        answerB = "In einem Fischladen",
        answerC = "In einem Geschaeft fuer tropische Fische",
        answerD = "In einem Blumengeschaeft",
        correctAnswer = 2,
        explanation = "Nach einem alten britischen Gesetz darf eine Frau in Liverpool nicht barbusig in einem Geschaeft arbeiten — es sei denn, es handelt sich um ein Geschaeft fuer tropische Fische. Diese kuriose Ausnahme ist real.",
        difficulty = 1,
        funFact = "Grossbritannien hat tausende von uralten, nie formell abgeschafften Gesetzen. Experten schaetzen, dass einige hundert davon noch heute technisch gueltig sind."
    ),

    // Question 29 — Fun Fact: Aspirin Erfinder
    Question(
        categoryId = 11,
        questionText = "Wer entwickelte Aspirin im Jahr 1897?",
        answerA = "Robert Koch",
        answerB = "Felix Hoffmann bei Bayer",
        answerC = "Wilhelm Roentgen",
        answerD = "Max Planck",
        correctAnswer = 1,
        explanation = "Aspirin wurde 1897 von Felix Hoffmann, einem deutschen Chemiker beim Pharmaunternehmen Bayer, entwickelt. Es wurde eines der meistverkauften Medikamente der Welt.",
        difficulty = 1,
        funFact = "Felix Hoffmann soll das Aspirin auch deshalb entwickelt haben, weil sein Vater unter starken Gelenkschmerzen litt und gaengige Medikamente dieser Zeit unertraegliche Magenprobleme verursachten."
    ),

    // Question 30 — Kurioses: Nil Laenderrekord
    Question(
        categoryId = 11,
        questionText = "Durch wie viele Laender fliesst der Nil, der laengste Fluss der Welt?",
        answerA = "4 Laender",
        answerB = "7 Laender",
        answerC = "11 Laender",
        answerD = "15 Laender",
        correctAnswer = 2,
        explanation = "Der Nil fliesst durch elf verschiedene Laender und ist mit einer Laenge von 6.853 Kilometern der laengste Fluss der Welt.",
        difficulty = 1,
        funFact = "Obwohl der Nil laenger als der Amazonas ist, fuehrt der Amazonas mehr als das Zehnfache an Wasservolumen — er ist sozusagen der 'breitere' Rekordhalter."
    ),

    // Question 31 — Fun Fact: Pyramide Gizeh
    Question(
        categoryId = 11,
        questionText = "Aus wie vielen Kalksteinbloecken besteht die grosse Pyramide von Gizeh?",
        answerA = "Ca. 500.000 Bloecke",
        answerB = "Ca. 1 Million Bloecke",
        answerC = "Ca. 2,3 Millionen Bloecke",
        answerD = "Ca. 5 Millionen Bloecke",
        correctAnswer = 2,
        explanation = "Die grosse Pyramide von Gizeh besteht aus rund 2,3 Millionen Kalksteinbloecken, die jeweils zwischen 2,5 und 15 Tonnen wiegen.",
        difficulty = 1,
        funFact = "Die Cheops-Pyramide war jahrtausendelang das hoechste von Menschenhand errichtete Bauwerk der Welt — bis im Jahr 1311 die Kathedrale von Lincoln in England sie ueberrundete."
    ),

    // Question 32 — Tierrekord: Blauwal Herz
    Question(
        categoryId = 11,
        questionText = "Wie gross ist das Herz eines Blauwals ungefaehr?",
        answerA = "So gross wie ein Basketball",
        answerB = "So gross wie ein Kleinauto",
        answerC = "So gross wie ein Schulranzen",
        answerD = "So gross wie ein Kuhlschrank",
        correctAnswer = 1,
        explanation = "Das Herz eines Blauwals ist in etwa so gross wie ein Kleinauto und wiegt rund 180 Kilogramm. Es ist das groesste Herz aller bekannten Tiere.",
        difficulty = 1,
        funFact = "Das Herz eines Blauwals schlaegt beim Tauchen nur 4 bis 8 Mal pro Minute — beim Auftauchen beschleunigt es kurz auf bis zu 37 Schlaege, um den Sauerstoff schnell zu verteilen."
    ),

    // Question 33 — Kurioses: Liechtenstein Zahnaexte
    Question(
        categoryId = 11,
        questionText = "Wofuer ist das kleine Land Liechtenstein weltweit bekannt als groesster Hersteller?",
        answerA = "Briefmarken fuer Sammler",
        answerB = "Schokolade fuer Export",
        answerC = "Falscher Zaehne",
        answerD = "Traktoren und Landmaschinen",
        correctAnswer = 2,
        explanation = "Liechtenstein ist der weltweit groesste Hersteller von Zahnprothesen (kuenstlichen Zaehnen). Das winzige Land mit nur 38.000 Einwohnern exportiert Zahnprothesen in alle Welt.",
        difficulty = 1,
        funFact = "Besonders gefragt sind schwarze Prothesen fuer Maerkte in bestimmten asiatischen Laendern — dort sind schwarz eingefaerbte Zaehne mancherorts ein traditionelles Schoenheitsideal."
    ),

    // Question 34 — Fun Fact: Huhn Eier
    Question(
        categoryId = 11,
        questionText = "Wie viele Eier legt ein Huhn durchschnittlich pro Jahr?",
        answerA = "Ca. 100 Eier",
        answerB = "Ca. 200 Eier",
        answerC = "Ca. 300 Eier",
        answerD = "Ca. 400 Eier",
        correctAnswer = 2,
        explanation = "Ein durchschnittliches Legehuhn produziert rund 300 Eier pro Jahr — das entspricht fast einem Ei pro Tag.",
        difficulty = 1,
        funFact = "Ein frisch gelegtes Ei hat keine feste Schale — es haertet erst in den Minuten nach der Ablage aus. Huehner produzieren jeden Schritt der Eischale im Koerper von innen nach aussen."
    ),

    // Question 35 — Kurioses: Vasco da Gama Bruecke
    Question(
        categoryId = 11,
        questionText = "Was zeichnet die Vasco-da-Gama-Bruecke in Portugal aus?",
        answerA = "Sie ist die aelteste Haengebruecke Europas",
        answerB = "Sie ist die laengste Bruecke Europas mit 12 km",
        answerC = "Sie ist die breiteste Bruecke der Welt",
        answerD = "Sie ueberquert als einzige Bruecke den Atlantik",
        correctAnswer = 1,
        explanation = "Die Vasco-da-Gama-Bruecke in Lissabon ist mit etwa 12 Kilometern die laengste Bruecke in Europa und ueberspannt den Tejo-Fluss.",
        difficulty = 1,
        funFact = "Die Bruecke wurde 1998 eroeffnet und ist nach dem portugiesischen Entdecker Vasco da Gama benannt, der als erster Europaeer den Seeweg nach Indien fand."
    ),

    // Question 36 — Fun Fact: Wasser Anteil Koerper
    Question(
        categoryId = 11,
        questionText = "Zu wie viel Prozent besteht der menschliche Koerper ungefaehr aus Wasser?",
        answerA = "Ca. 45 Prozent",
        answerB = "Ca. 60 Prozent",
        answerC = "Ca. 80 Prozent",
        answerD = "Ca. 95 Prozent",
        correctAnswer = 1,
        explanation = "Der menschliche Koerper besteht zu etwa 60 Prozent aus Wasser. Bei Saeuglinge liegt dieser Anteil sogar bei rund 75 Prozent.",
        difficulty = 1,
        funFact = "Das Gehirn besteht zu etwa 73 Prozent aus Wasser — und die Lunge sogar zu 83 Prozent. Schon 1 bis 2 Prozent Wasserverlust fuehren zu messbaren Leistungseinbussen."
    ),

    // Question 37 — Tierrekord: Schnellstes Tier Wasser
    Question(
        categoryId = 11,
        questionText = "Welches ist das schnellste Tier im Wasser?",
        answerA = "Delphin",
        answerB = "Barrakuda",
        answerC = "Schwertfisch",
        answerD = "Segelfisch",
        correctAnswer = 3,
        explanation = "Der Segelfisch gilt als das schnellste Tier im Wasser und kann bis zu 110 km/h erreichen. Er ist damit sogar schneller als ein Gepard an Land.",
        difficulty = 1,
        funFact = "Der Segelfisch nutzt seine riesige Rueckenflosse, um Fischschwaerme zu verwirren und einzukreisen. Danach schlaegt er blitzschnell zu."
    ),

    // Question 38 — Kurioses: Wort Duzen
    Question(
        categoryId = 11,
        questionText = "Welche Besonderheit hat das Verb 'duzen' in der deutschen Sprache?",
        answerA = "Es gibt kein Verb 'duzen' — man sagt nur 'per Du sein'",
        answerB = "Es ist das einzige Verb, das von einem Pronomen (du) abgeleitet wurde",
        answerC = "Es ist das kuerzeste vollstaendige Verb im Deutschen",
        answerD = "Es existiert nur in der Gegenwartsform",
        correctAnswer = 1,
        explanation = "'Duzen' und 'Siezen' sind seltene Beispiele fuer Verben, die direkt aus Personalpronomen ('du' und 'Sie') gebildet wurden — eine sprachliche Besonderheit des Deutschen.",
        difficulty = 1,
        funFact = "Im Deutschen war das 'Sie' als Hoeflichkeitsform erst seit dem 18. Jahrhundert ueblich. Davor nutzte man 'Ihr' — und noch frueher war ein einfaches 'Ihr' sogar fuer Koenige ausreichend."
    ),

    // Question 39 — Fun Fact: Lichtgeschwindigkeit
    Question(
        categoryId = 11,
        questionText = "Wie schnell reist Licht durch das Vakuum — gerundet auf die naechste runde Zahl?",
        answerA = "Ca. 100.000 km/s",
        answerB = "Ca. 200.000 km/s",
        answerC = "Ca. 300.000 km/s",
        answerD = "Ca. 500.000 km/s",
        correctAnswer = 2,
        explanation = "Licht reist durch das Vakuum mit ca. 299.792 km/s — also rund 300.000 Kilometer pro Sekunde. In einer Sekunde koennte Licht siebeneinhalb Mal die Erde umrunden.",
        difficulty = 1,
        funFact = "Das Licht der Sonne braucht etwa 8 Minuten und 20 Sekunden bis zur Erde. Wenn die Sonne ploetzlich erlischt, wuerde es noch ueber 8 Minuten dauern, bis wir es auf der Erde merken."
    ),

    // Question 40 — Kurioses: Tieranzahl Erde
    Question(
        categoryId = 11,
        questionText = "Wie viele Tier- und Pflanzenarten sind weltweit wissenschaftlich beschrieben?",
        answerA = "Ca. 500.000 Arten",
        answerB = "Ca. 1 Million Arten",
        answerC = "Ca. 2 Millionen Arten",
        answerD = "Ca. 10 Millionen Arten",
        correctAnswer = 2,
        explanation = "Weltweit sind etwa 2 Millionen Tier- und Pflanzenarten wissenschaftlich beschrieben. Experten schaetzen jedoch, dass die tatsaechliche Artenanzahl 8 bis 10 Millionen betragen koennte.",
        difficulty = 1,
        funFact = "Drei Viertel aller bekannten Tierarten sind Wirbellose (Insekten, Spinnen, Krebstiere). Wirbeltiere wie Saeugetiere, Voegel und Fische machen nur etwa 4 Prozent aller Tierarten aus."
    ),

    // Question 41 — Weltrekord: Masskrug-Gewicht
    Question(
        categoryId = 11,
        questionText = "Wie viel wogen die 29 gefuellten Masskruege, die Oliver Struempfel trug, zusammen?",
        answerA = "Ca. 30 kg",
        answerB = "Ca. 50 kg",
        answerC = "Ca. 70 kg",
        answerD = "Ca. 100 kg",
        correctAnswer = 2,
        explanation = "Die 29 gefuellten Masskruege wogen zusammen knapp 70 Kilogramm — das entspricht dem Gewicht eines durchschnittlichen Erwachsenen.",
        difficulty = 1,
        funFact = "Struempfel trainierte jahrelang fuer diesen Rekord. Der Trick liegt nicht nur in der Kraft, sondern in der Balance: Alle Kruege muessen gleichzeitig in der Hand gehalten werden, ohne etwas zu verschuetten."
    ),

    // Question 42 — Fun Fact: Ameisengewicht
    Question(
        categoryId = 11,
        questionText = "Wie viel Gewicht koennen Ameisen im Verhaeltnis zu ihrem eigenen Koerpergewicht tragen?",
        answerA = "Das 5-fache ihres Koerpergewichts",
        answerB = "Das 10- bis 20-fache ihres Koerpergewichts",
        answerC = "Das 50-fache ihres Koerpergewichts",
        answerD = "Das 100-fache ihres Koerpergewichts",
        correctAnswer = 1,
        explanation = "Ameisen koennen das 10- bis 20-fache ihres eigenen Koerpergewichts tragen — bei manchen Arten sogar mehr. Das waere so, als wuerde ein Mensch ein Auto hochheben.",
        difficulty = 1,
        funFact = "Der Grund fuer diese enorme relative Kraft liegt in der Physik: Je kleiner ein Tier, desto guenstiger ist das Verhaeltnis von Muskelquerschnitt zu Koerpergewicht."
    ),

    // Question 43 — Kurioses: Weltraumschrott
    Question(
        categoryId = 11,
        questionText = "Wie viele Schrottteile kreisen laut ESA ungefaehr um die Erde?",
        answerA = "Ca. 10.000 Teile",
        answerB = "Ca. 100.000 Teile",
        answerC = "Ueber 1 Million Teile",
        answerD = "Ueber 100 Millionen Teile",
        correctAnswer = 3,
        explanation = "Laut der Europaeischen Weltraumbehoerde ESA kreisen ueber 100 Millionen Schrottteile um die Erde — von alten Satelliten, Raketenteilen bis zu abgeplatzten Farbteilchen.",
        difficulty = 1,
        funFact = "Selbst winzige Schrottteile von 1 cm Groesse koennen bei typischen Orbitalgeschwindigkeiten von 28.000 km/h Schaeden wie eine Pistolenkugel verursachen."
    ),

    // Question 44 — Fun Fact: Kokospalme
    Question(
        categoryId = 11,
        questionText = "Wie viele Menschen sterben jaehrlich weltweit durch herabfallende Kokosnuesse?",
        answerA = "Keine — das ist ein Mythos",
        answerB = "Etwa 15 Menschen",
        answerC = "Etwa 150 Menschen",
        answerD = "Ueber 1.500 Menschen",
        correctAnswer = 1,
        explanation = "Laut einer oft zitierten Studie sterben jaehrlich weltweit etwa 150 Menschen durch herabfallende Kokosnuesse. Eine Kokosnuss kann bis zu 2 kg wiegen und aus 30 Metern Hoehe fallen.",
        difficulty = 1,
        funFact = "Diese Zahl wird von Wissenschaftlern gelegentlich angezweifelt, da systematische Daten schwer zu erheben sind. Dennoch sind Kokosnuss-Unfaelle in tropischen Laendern tatsaechlich ein bekanntes Risiko."
    ),

    // Question 45 — Kurioses: Haare wachsen
    Question(
        categoryId = 11,
        questionText = "Wie schnell wachsen menschliche Kopfhaare im Durchschnitt?",
        answerA = "Ca. 1 cm pro Monat",
        answerB = "Ca. 5 cm pro Monat",
        answerC = "Ca. 10 cm pro Monat",
        answerD = "Ca. 15 cm pro Monat",
        correctAnswer = 0,
        explanation = "Menschliche Kopfhaare wachsen durchschnittlich etwa 1 Zentimeter pro Monat bzw. rund 12 Zentimeter pro Jahr.",
        difficulty = 1,
        funFact = "Haare wachsen nicht gleichmaessig: Im Sommer wachsen sie schneller als im Winter. Ausserdem wachsen die Haare des linken Kopfs oft etwas schneller als die des rechten."
    ),

    // Question 46 — Weltrekord: Kuss-Dauer
    Question(
        categoryId = 11,
        questionText = "Aus welchem Land stammt das Paar, das den Weltrekord im laengsten Kuss haelt?",
        answerA = "Japan",
        answerB = "Brasilien",
        answerC = "Thailand",
        answerD = "Philippinen",
        correctAnswer = 2,
        explanation = "Das thailaendische Ehepaar Ekkachai und Laksana Tiranarat stellte den Weltrekord im laengsten Kuss auf: 58 Stunden, 35 Minuten und 58 Sekunden.",
        difficulty = 1,
        funFact = "Der Wettbewerb fand 2013 in Thailand statt. Die Paare mussten die ganze Zeit stehen und durften keine Unterbrechungen einlegen — auch nicht zum Essen oder Schlafen."
    ),

    // Question 47 — Fun Fact: Schneeflocken
    Question(
        categoryId = 11,
        questionText = "Was ist an der Form von Schneeflocken besonders?",
        answerA = "Alle Schneeflocken haben exakt dieselbe Form",
        answerB = "Jede Schneeflocke hat eine einzigartige, niemals identische Struktur",
        answerC = "Schneeflocken haben in der Naehe des Pols runde Form",
        answerD = "Schneeflocken haben im Flachland 6, in Bergen 8 Seiten",
        correctAnswer = 1,
        explanation = "Keine zwei Schneeflocken sind identisch — jede hat eine einzigartige Kristallstruktur. Das liegt daran, dass jede Schneeflocke individuelle Wachstumsbedingungen auf ihrem Weg zur Erde erlebt.",
        difficulty = 1,
        funFact = "Alle Schneeflocken haben genau 6 Seiten — das ist die Grundform von Wasserkristallen. Innerhalb dieser Sechseck-Symmetrie gibt es jedoch unendlich viele Variationen."
    ),

    // Question 48 — Kurioses: Banane
    Question(
        categoryId = 11,
        questionText = "Was ist eine Banane botanisch gesehen?",
        answerA = "Eine Frucht",
        answerB = "Eine Beere",
        answerC = "Ein Gemuese",
        answerD = "Eine Wurzel",
        correctAnswer = 1,
        explanation = "Botanisch gesehen ist eine Banane eine Beere — genau wie Tomaten und Wassermelonen. Erdbeeren hingegen sind botanisch keine Beeren, sondern sogenannte Scheinbeeren.",
        difficulty = 1,
        funFact = "Auch Kiwis, Avocados und Auberginen sind botanisch gesehen Beeren. Die botanische Definition von 'Beere' hat kaum etwas mit dem Alltagsverstaendnis gemeinsam."
    ),

    // Question 49 — Fun Fact: Donner und Blitz
    Question(
        categoryId = 11,
        questionText = "Wie viele Blitze schlagen pro Sekunde weltweit auf der Erde ein?",
        answerA = "Ca. 1 Blitz pro Sekunde",
        answerB = "Ca. 10 Blitze pro Sekunde",
        answerC = "Ca. 50 Blitze pro Sekunde",
        answerD = "Ca. 100 Blitze pro Sekunde",
        correctAnswer = 3,
        explanation = "Weltweit schlagen durchschnittlich etwa 100 Blitze pro Sekunde auf der Erde ein — das sind rund 8 Millionen Blitze pro Tag.",
        difficulty = 1,
        funFact = "Ein einziger Blitz erhitzt die umgebende Luft auf bis zu 30.000 Grad Celsius — fuenfmal so heiss wie die Oberflaeche der Sonne. Das ist die Ursache des Donners."
    ),

    // Question 50 — Kurioses: Bienen Honig
    Question(
        categoryId = 11,
        questionText = "Wie viel Honig produziert eine Biene in ihrem gesamten Leben?",
        answerA = "Ca. 1 Teelloeffel",
        answerB = "Ca. 1 Essloeeffel",
        answerC = "Ca. 1 Glas",
        answerD = "Ca. 1 Kilogramm",
        correctAnswer = 0,
        explanation = "Eine einzige Biene produziert in ihrem gesamten Leben — etwa 6 Wochen — nur rund einen Teeloeffel Honig (ca. 1/12 Teeloeeffel nach anderen Quellen). Ein Glas Honig erfordert die Arbeit von Tausenden von Bienen.",
        difficulty = 1,
        funFact = "Um ein 500-Gramm-Glas Honig zu fuellen, muessen Bienen insgesamt rund 2 Millionen Blumen besuchen und zusammen eine Strecke zuruecklegen, die zweimal die Erde umrundet."
    )

)
