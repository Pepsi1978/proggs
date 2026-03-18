package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun animalQuestionsEasy3(): List<Question> = listOf(

    // ── EASY (difficulty = 1) ── 50 questions about birds ────────────────────

    Question(
        categoryId = 9,
        questionText = "Welcher Vogel kann nicht fliegen und ist der groesste Vogel der Welt?",
        answerA = "Pinguin",
        answerB = "Emu",
        answerC = "Strauß",
        answerD = "Nandu",
        correctAnswer = 2,
        explanation = "Der Strauß ist der groesste Vogel der Welt und kann trotz seiner Flugunfaehigkeit bis zu 70 km/h schnell laufen.",
        difficulty = 1,
        funFact = "Ein Strauß-Ei wiegt so viel wie etwa 24 Huehnereier."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Farbe hat das Gefieder eines ausgewachsenen maennlichen Pfaus hauptsaechlich?",
        answerA = "Rot und Gold",
        answerB = "Blau und Gruen",
        answerC = "Schwarz und Weiß",
        answerD = "Gelb und Orange",
        correctAnswer = 1,
        explanation = "Maennliche Pfauen haben ein praechtiges blau-gruenes Gefieder. Der Schwanzfaecher wird zur Balz aufgestellt.",
        difficulty = 1,
        funFact = "Nur die maennlichen Pfauen haben den langen bunten Schwanzfaecher. Weibchen sind braun und unscheinbar."
    ),

    Question(
        categoryId = 9,
        questionText = "Wo leben Pinguine in der Natur?",
        answerA = "Arktis",
        answerB = "Nordpol",
        answerC = "Antarktis",
        answerD = "Sibirien",
        correctAnswer = 2,
        explanation = "Pinguine leben auf der Suedseite der Erde, hauptsaechlich in der Antarktis und den umliegenden Inseln.",
        difficulty = 1,
        funFact = "Polarbaren und Pinguine leben niemals zusammen in freier Natur, da Polarbaren in der Arktis und Pinguine in der Antarktis leben."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Vogel gilt als Symbol fuer Weisheit?",
        answerA = "Adler",
        answerB = "Rabe",
        answerC = "Eule",
        answerD = "Storch",
        correctAnswer = 2,
        explanation = "Die Eule gilt seit der Antike als Symbol fuer Weisheit, da sie mit der griechischen Goettin Athene verbunden wurde.",
        difficulty = 1,
        funFact = "Eulen koennen ihren Kopf um bis zu 270 Grad drehen, da sie ihre Augen nicht bewegen koennen."
    ),

    Question(
        categoryId = 9,
        questionText = "Was bringt der Storch laut einem bekannten deutschen Volksglauben?",
        answerA = "Glueck",
        answerB = "Babys",
        answerC = "Regen",
        answerD = "Reichtum",
        correctAnswer = 1,
        explanation = "Laut dem deutschen Volksglauben bringt der Storch die Babys. Dieser Glaube entstand, weil Stoerche im Fruehling zurueckkehren, wenn viele Kinder geboren werden.",
        difficulty = 1,
        funFact = "Stoerche kehren oft jahrzehntelang zum gleichen Nest zurueck, das Jahr fuer Jahr groesser wird."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher kleine Vogel ist fuer sein rotes Brustgefieder bekannt?",
        answerA = "Meise",
        answerB = "Rotkehlchen",
        answerC = "Fink",
        answerD = "Spatz",
        correctAnswer = 1,
        explanation = "Das Rotkehlchen ist leicht an seiner orangeroten Brust zu erkennen. Es ist in Deutschland ein sehr beliebter und haeufiger Gartenvogel.",
        difficulty = 1,
        funFact = "Das Rotkehlchen ist der Nationalvogel Grossbritanniens."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Vogel ist fuer seinen lauten Ruf bekannt, der wie 'Kuckuck' klingt?",
        answerA = "Specht",
        answerB = "Drossel",
        answerC = "Kuckuck",
        answerD = "Pirol",
        correctAnswer = 2,
        explanation = "Der Kuckuck ist fuer seinen charakteristischen zweisilbigen Ruf bekannt. Nur die Maennchen rufen 'Kuckuck'.",
        difficulty = 1,
        funFact = "Der Kuckuck legt seine Eier in die Nester anderer Voegel und laesst seine Jungen von Pflegeeltern aufziehen."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Vogel ist fuer sein Haemmern an Baeumlern bekannt?",
        answerA = "Kleiber",
        answerB = "Specht",
        answerC = "Baumlaeufer",
        answerD = "Star",
        correctAnswer = 1,
        explanation = "Der Specht haemmert mit seinem kraeftigen Schnabel Loecher in Baumstamme, um Insekten zu finden oder ein Nest zu bauen.",
        difficulty = 1,
        funFact = "Ein Specht kann bis zu 20 Schlaege pro Sekunde ausfuehren, ohne Kopfschmerzen zu bekommen."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Voegel leben in Kolonien und koennen sehr gut tauchen?",
        answerA = "Spatzen",
        answerB = "Pinguine",
        answerC = "Tauben",
        answerD = "Kraehen",
        correctAnswer = 1,
        explanation = "Pinguine sind hervorragende Taucher und leben in grossen Kolonien. Einige Arten koennen bis zu 500 Meter tief tauchen.",
        difficulty = 1,
        funFact = "Kaiserpinguine koennen bis zu 22 Minuten unter Wasser bleiben."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Vogel ist das Wappentier der USA?",
        answerA = "Truthahn",
        answerB = "Habicht",
        answerC = "Weißkopfseeadler",
        answerD = "Kondor",
        correctAnswer = 2,
        explanation = "Der Weißkopfseeadler ist das Nationalsymbol der USA und ziert das Wappen sowie den Staatssiegel.",
        difficulty = 1,
        funFact = "Benjamin Franklin wollte den Truthahn als Nationalsymbol der USA, verlor aber die Abstimmung gegen den Weißkopfseeadler."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Vogel ist bekannt dafuer, seinen Schwanz faecherartig aufzufaechern?",
        answerA = "Pelikan",
        answerB = "Flamingo",
        answerC = "Pfau",
        answerD = "Reiher",
        correctAnswer = 2,
        explanation = "Der Pfau faechert seinen langen Schmuckfedernschwanz bei der Balz auf. Das beeindruckende Rad besteht aus bis zu 200 Federn.",
        difficulty = 1,
        funFact = "Der Schwanzfaecher des Pfaus macht etwa 60 Prozent seiner gesamten Koerperlaenge aus."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie nennt man die Gruppe von Voegeln, die im Herbst in waermere Laender ziehen?",
        answerA = "Standvoegel",
        answerB = "Zugvoegel",
        answerC = "Wintervoegel",
        answerD = "Wandervoegel",
        correctAnswer = 1,
        explanation = "Zugvoegel fliegen jeden Herbst in waermere Gebiete und kehren im Fruehling zurueck. Bekannte Beispiele sind Schwalben und Stoerche.",
        difficulty = 1,
        funFact = "Der Arktische Kuestenseeschwalbe hat die laengste Zugstrecke aller Tiere: bis zu 90.000 km im Jahr."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Farbe hat ein Kanarienvogel typischerweise?",
        answerA = "Blau",
        answerB = "Rot",
        answerC = "Gelb",
        answerD = "Gruen",
        correctAnswer = 2,
        explanation = "Der Hauskanarienvogel ist durch Zucht meistens leuchtend gelb. In der Wildnis kommen auch gruenliche und braune Faerbungen vor.",
        difficulty = 1,
        funFact = "Kanarien wurden fruehher in Bergwerken eingesetzt, um Kohlenmonoxid zu erkennen, da sie sehr empfindlich auf das Gas reagieren."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Vogel kann rueckwaerts fliegen?",
        answerA = "Schwalbe",
        answerB = "Mauersegler",
        answerC = "Kolibri",
        answerD = "Eisvogel",
        correctAnswer = 2,
        explanation = "Der Kolibri ist der einzige Vogel, der dauerhaft rueckwaerts fliegen kann. Er kann seine Fluegelbewegungen in alle Richtungen steuern.",
        difficulty = 1,
        funFact = "Ein Kolibri muss seinen eigenen Koerper pro Tag mehr als verdoppeln an Nahrung aufnehmen, um seinen hohen Energiebedarf zu decken."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Farbe haben Flamingos?",
        answerA = "Gelb",
        answerB = "Rosa",
        answerC = "Weiss",
        answerD = "Orange",
        correctAnswer = 1,
        explanation = "Flamingos sind durch Farbstoffe in ihrer Nahrung rosa gefaerbt. Junge Flamingos sind anfangs grau und werden erst mit der Zeit rosa.",
        difficulty = 1,
        funFact = "Flamingos fressen kopfueber, weil ihr Schnabel so geformt ist, dass sie Wasser und Schlamm filtern koennen."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie nennt man das Kunstlied der Voegel?",
        answerA = "Zwitschern",
        answerB = "Quaken",
        answerC = "Gesang",
        answerD = "Ruf",
        correctAnswer = 2,
        explanation = "Der Gesang der Voegel dient zur Partnersuche und zur Markierung des Reviers. Jede Art hat ihren eigenen unverwechselbaren Gesang.",
        difficulty = 1,
        funFact = "Die Nachtigall gilt als der beste Saenger unter den Voegeln und kann ueber 200 verschiedene Liedphrasen produzieren."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Vogel baut sein Nest in Schornsteinen?",
        answerA = "Spatz",
        answerB = "Storch",
        answerC = "Schwalbe",
        answerD = "Taube",
        correctAnswer = 1,
        explanation = "Der Weißstorch baut sein großes Nest mit Vorliebe auf Hausdaechern und in Schornsteinen. Das Nest wird oft jahrelang genutzt.",
        difficulty = 1,
        funFact = "Ein Storchennest kann nach Jahren der Nutzung bis zu einer Tonne wiegen."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Vogel ist fuer seinen Diebstahl glaenzender Gegenstaende bekannt?",
        answerA = "Rabe",
        answerB = "Elster",
        answerC = "Kraehenvoegel",
        answerD = "Dohle",
        correctAnswer = 1,
        explanation = "Die Elster hat den Ruf, glaenzende Gegenstaende zu stehlen und in ihr Nest zu tragen. Neuere Studien zeigen, dass sie eher schimmernde Objekte meidet.",
        difficulty = 1,
        funFact = "Elstern gehoeren zu den intelligentesten Tieren der Welt und sind die einzigen Nicht-Saeugetiere, die sich im Spiegel erkennen koennen."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie nennt man das Federkleid der Voegel auf der Brust?",
        answerA = "Bauch",
        answerB = "Brust",
        answerC = "Kehle",
        answerD = "Unterseite",
        correctAnswer = 1,
        explanation = "Die Brust der Voegel ist der vordere Teil des Koerpers zwischen Hals und Bauch. Das Brustgefieder ist oft auffaellig gefaerbt.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Vogel ist bekannt fuer seinen Ruf in der Nacht?",
        answerA = "Spatz",
        answerB = "Eule",
        answerC = "Taube",
        answerD = "Meise",
        correctAnswer = 1,
        explanation = "Eulen sind nachtaktiv und ihr typisches 'Uhu' oder 'Kiwitt' ist oft in der Nacht zu hoeren. Sie sind perfekt an das Leben in der Dunkelheit angepasst.",
        difficulty = 1,
        funFact = "Eulen koennen Geraeuschemit einem Abstand von weniger als einem Millimeter orten, was ihnen die Jagd in voelliger Dunkelheit ermoeglicht."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Wasservogel ist fuer seine weißen Federn und seinen langen Hals bekannt?",
        answerA = "Ente",
        answerB = "Schwan",
        answerC = "Gans",
        answerD = "Reiher",
        correctAnswer = 1,
        explanation = "Der Schwan ist ein eleganter weisser Wasservogel mit einem langen geschwungenen Hals. Er ist fuer seine lebenslange Treue zu seinem Partner bekannt.",
        difficulty = 1,
        funFact = "Schwanenjunge heissen Cygnets und sind bei der Geburt grau gefaerbt."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Vogel kann besonders gut sprechen und Laute nachahmen?",
        answerA = "Wellensittich",
        answerB = "Papagei",
        answerC = "Kakadu",
        answerD = "Sittich",
        correctAnswer = 1,
        explanation = "Papageien sind bekannt fuer ihre Faehigkeit, menschliche Sprache nachzuahmen. Sie gehoeren zu den intelligentesten Voegeln der Welt.",
        difficulty = 1,
        funFact = "Der Graupapagei Alex konnte ueber 100 Woerter sprechen und deren Bedeutung verstehen."
    ),

    Question(
        categoryId = 9,
        questionText = "Was essen Adler hauptsaechlich?",
        answerA = "Insekten",
        answerB = "Beeren",
        answerC = "Fische und kleine Tiere",
        answerD = "Koerner",
        correctAnswer = 2,
        explanation = "Adler sind Raubvoegel und ernaehren sich von Fischen, Kleinsaeugetieren, Hasen und anderen Tieren. Sie sind Spitzejaeger in ihrem Oekosystem.",
        difficulty = 1,
        funFact = "Ein Adler kann ein Tier sehen, das vier bis acht Mal weiter entfernt ist, als ein Mensch es sehen wuerde."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie nennt man einen jungen Vogel, der noch im Nest sitzt?",
        answerA = "Kueken",
        answerB = "Nestling",
        answerC = "Jungtier",
        answerD = "Flaeumling",
        correctAnswer = 1,
        explanation = "Ein Nestling ist ein Jungvogel, der noch vollstaendig vom Nest abhaengig ist und noch nicht fliegen kann.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Vogel ist fuer sein blau glaenzendes Gefieder bekannt und lebt an Gewaessern?",
        answerA = "Blaumeise",
        answerB = "Blaukehlchen",
        answerC = "Eisvogel",
        answerD = "Blaumerle",
        correctAnswer = 2,
        explanation = "Der Eisvogel hat ein leuchtend blaues und orangefarbenes Gefieder. Er lebt an sauberen Gewaessern und taucht blitzschnell nach Fischen.",
        difficulty = 1,
        funFact = "Der Eisvogel ist so schnell beim Tauchen, dass er kaum Spritzer erzeugt. Sein Schnabelform inspirierte das Design des japanischen Hochgeschwindigkeitszuges."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Voegel fliegen im Herbst in Keilformation nach Sueden?",
        answerA = "Enten",
        answerB = "Kraehen",
        answerC = "Gaense",
        answerD = "Spatzen",
        correctAnswer = 2,
        explanation = "Gaense fliegen in der charakteristischen V-Formation, da jeder Vogel von den Aufwinden des Vordervogels profitiert und so Energie spart.",
        difficulty = 1,
        funFact = "Die V-Formation der Gaense kann den Energieverbrauch des gesamten Schwarms um bis zu 70 Prozent reduzieren."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Vogel ist der schnellste im Sturzflug?",
        answerA = "Adler",
        answerB = "Wanderfalke",
        answerC = "Habicht",
        answerD = "Sperber",
        correctAnswer = 1,
        explanation = "Der Wanderfalke ist das schnellste Tier der Welt. Im Sturzflug erreicht er Geschwindigkeiten von ueber 320 km/h.",
        difficulty = 1,
        funFact = "Der Wanderfalke kann beim Sturzflug spezielle Knochenstrukturen in den Nasenloecher nutzen, um den Luftstrom zu kontrollieren."
    ),

    Question(
        categoryId = 9,
        questionText = "Wozu dient das Gefieder der Voegel?",
        answerA = "Nur zum Fliegen",
        answerB = "Waerme, Fliegen und Schutz",
        answerC = "Nur zur Tarnung",
        answerD = "Nur zur Balz",
        correctAnswer = 1,
        explanation = "Das Gefieder erfullt viele Aufgaben: Es waermt den Koerper, ermoeoelicht das Fliegen, schuetzt vor Naesse und hilft bei der Tarnung oder Balz.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher bekannte Vogel ist fuer seine schwarz-weisse Faerbung und sein watschelndes Gangbild bekannt?",
        answerA = "Papagei",
        answerB = "Pinguin",
        answerC = "Rabe",
        answerD = "Tukan",
        correctAnswer = 1,
        explanation = "Pinguine haben ein charakteristisches schwarz-weisses Federkleid und bewegen sich auf dem Land watschelnd fort, da ihre Beine weit hinten am Koerper sitzen.",
        difficulty = 1,
        funFact = "Das schwarz-weisse Muster des Pinguins ist eine Tarnfarbe: Von oben sieht man das dunkle Rueckgefieder, von unten den hellen Bauch."
    ),

    Question(
        categoryId = 9,
        questionText = "Was ist das typische Merkmal eines Tukaens?",
        answerA = "Langer Hals",
        answerB = "Grosser bunter Schnabel",
        answerC = "Langer Schwanzfaecher",
        answerD = "Kammartiger Kopfschmuck",
        correctAnswer = 1,
        explanation = "Der Tukan ist durch seinen enormen, bunten Schnabel bekannt, der fast so lang wie sein Koerper sein kann. Der Schnabel ist aber trotzdem leicht, da er aus einem Schwammgewebe besteht.",
        difficulty = 1,
        funFact = "Trotz seiner Groesse wiegt der Schnabel des Tukans kaum etwas, da er aus einem leichten, luftdurchlaessigen Gewebe besteht."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Vogel legt die groessten Eier der Welt?",
        answerA = "Adler",
        answerB = "Pelikan",
        answerC = "Strauß",
        answerD = "Emu",
        correctAnswer = 2,
        explanation = "Der Strauß legt die groessten Eier der Welt. Ein Straussel kann bis zu 1,5 kg wiegen und einen Durchmesser von 15 cm haben.",
        difficulty = 1,
        funFact = "Obwohl das Straussl das groesste Ei ist, ist es im Verhaeltnis zum Koerper des Strauzes das kleinste Ei aller Voegel."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Vogel ist fuer seinen langen Schnabel bekannt, mit dem er in Blueten nach Nektar sucht?",
        answerA = "Specht",
        answerB = "Kolibri",
        answerC = "Ibis",
        answerD = "Schnepfe",
        correctAnswer = 1,
        explanation = "Der Kolibri hat einen langen, duennen Schnabel, mit dem er tief in Blueten tauchen kann, um Nektar zu trinken. Dabei bestaubt er gleichzeitig die Blueten.",
        difficulty = 1,
        funFact = "Kolibris schlagen ihre Fluegel bis zu 80 Mal pro Sekunde, weshalb man ein summendes Geraesch hoert."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie nennt man Voegel, die das ganze Jahr in Deutschland bleiben?",
        answerA = "Zugvoegel",
        answerB = "Standvoegel",
        answerC = "Wintervoegel",
        answerD = "Jahresvoegel",
        correctAnswer = 1,
        explanation = "Standvoegel verlassen ihr Heimatgebiet nicht und bleiben das ganze Jahr ueber. Typische Standvoegel in Deutschland sind Meisen, Spechte und Kraehen.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Vogel ist in Deutschland der haeufigste Singvogel?",
        answerA = "Rotkehlchen",
        answerB = "Buchfink",
        answerC = "Haussperling",
        answerD = "Kohlmeise",
        correctAnswer = 1,
        explanation = "Der Buchfink ist einer der haeufigsten Voegel in Deutschland. Sein klarer, melodischer Gesang ist in Waeldern und Gaerten weit verbreitet.",
        difficulty = 1,
        funFact = "Der Buchfink war frueher als Kaefigvogel sehr beliebt, da er einen besonders schoenen Gesang hat."
    ),

    Question(
        categoryId = 9,
        questionText = "Was frisst eine Kohlmeise hauptsaechlich?",
        answerA = "Fische",
        answerB = "Insekten und Samen",
        answerC = "Beeren und Fruechte",
        answerD = "Kleine Saeugetiere",
        correctAnswer = 1,
        explanation = "Kohlmeisen fressen hauptsaechlich Insekten, Spinnen und Samen. Im Winter sind sie haeufige Gaeste an Vogelfutterstellen.",
        difficulty = 1,
        funFact = "Kohlmeisen verstecken im Herbst Samenvorraete und koennen sich selbst Monate spaeter an die Verstecke erinnern."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher grosse Wasservogel steht geduldig am Ufer und wartet auf Fische?",
        answerA = "Kormoran",
        answerB = "Haubentaucher",
        answerC = "Reiher",
        answerD = "Ente",
        correctAnswer = 2,
        explanation = "Der Graureiher steht stundenlang reglos am Wasser und wartet geduldig, bis ein Fisch nahe genug ist. Dann stoesst er blitzschnell zu.",
        difficulty = 1,
        funFact = "Ein Graureiher kann einen Fisch verschlucken, der fast so gross ist wie er selbst."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Vogel gilt in Deutschland als Gluecksbringer und Fruehlingsbote?",
        answerA = "Schwalbe",
        answerB = "Spatz",
        answerC = "Storch",
        answerD = "Kuckuck",
        correctAnswer = 2,
        explanation = "Die Schwalbe gilt in Deutschland als Gluecksbringer und Vorbote des Fruehlings. Ein Schwalbennest am Haus soll Glueck bringen.",
        difficulty = 1,
        funFact = "Schwalben legen bis zu 10.000 km zurueck auf ihrem Zug nach Suedafrika und wieder zurueck."
    ),

    Question(
        categoryId = 9,
        questionText = "Welche Koerperteile benutzen Voegel zum Fliegen?",
        answerA = "Beine",
        answerB = "Fluegel",
        answerC = "Schwanz",
        answerD = "Schnabel",
        correctAnswer = 1,
        explanation = "Voegel fliegen mit ihren Fluegeln, die aus Knochen, Muskeln und Federn bestehen. Der Schwanz dient zur Steuerung und als Bremse.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher grosse Vogel ist fuer seine Kahlheit am Kopf und seinen Hackesschnabel bekannt?",
        answerA = "Kondor",
        answerB = "Geier",
        answerC = "Marabu",
        answerD = "Pelikan",
        correctAnswer = 1,
        explanation = "Geier haben einen kahlen Kopf, der es ihnen erleichtert, tief in Kadaver zu tauchen, ohne ihre Federn zu verschmutzen. Sie sind wichtige Aasfresser im Oekosystem.",
        difficulty = 1,
        funFact = "Geier koennen innerhalb von Stunden einen grossen Kadaver vollstaendig skelettieren."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie nennt man das Gebaude, in dem Voegel in Zoos gehalten werden?",
        answerA = "Vogelkaeig",
        answerB = "Voliere",
        answerC = "Aviarium",
        answerD = "Vogelhaus",
        correctAnswer = 1,
        explanation = "Eine Voliere ist ein grosser, begehbarer Kaefig oder ein Freigehege fuer Voegel. In Zoos und Tierparks sind Volieren oft so gross, dass die Voegel frei fliegen koennen.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Vogel ist bekannt dafuer, seinen Kopf kaum zu bewegen, waehrend er auf dem Wasser schwimmt?",
        answerA = "Ente",
        answerB = "Schwan",
        answerC = "Blässhuhn",
        answerD = "Haubentaucher",
        correctAnswer = 0,
        explanation = "Die Ente macht beim Schwimmen charakteristische Kopfbewegungen vor und zurueck. Dieses Verhalten hilft ihr bei der Stabilisierung des Blickfelds.",
        difficulty = 1,
        funFact = "Enten haben ein wasserdichtes Gefieder, da sie es regelmaessig mit oel aus einer Burzeldruese einreiben."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Vogel ist das bekannteste Haustier unter den kleinen Papageienartigen?",
        answerA = "Nymphensittich",
        answerB = "Wellensittich",
        answerC = "Agapornide",
        answerD = "Rosellas",
        correctAnswer = 1,
        explanation = "Der Wellensittich ist einer der beliebtesten Hausvoegel weltweit. Er stammt aus Australien und kann in der Wildnis in riesigen Schwaermen von Tausenden Voegeln vorkommen.",
        difficulty = 1,
        funFact = "Wellensittiche koennen bis zu 1500 Woerter lernen und sind damit rekordverdaechtig unter den sprechenden Voegeln."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Vogel hat einen langen roten Schnabel und lange rote Beine?",
        answerA = "Flamingo",
        answerB = "Storch",
        answerC = "Kranich",
        answerD = "Ibis",
        correctAnswer = 1,
        explanation = "Der Weißstorch hat einen auffallend langen roten Schnabel und lange rote Beine. Er ist einer der bekanntesten Zugvoegel in Europa.",
        difficulty = 1,
        funFact = "Stoerche haben keine Stimme, da ihr Stimmband im Laufe der Evolution zurueckgebildet wurde. Sie klappern stattdessen laut mit dem Schnabel."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Vogel ist bekannt fuer seinen langen Wasserflug und seinen grossen Kehlbeutel?",
        answerA = "Pelikan",
        answerB = "Fregattvogel",
        answerC = "Basstoeoel",
        answerD = "Albatros",
        correctAnswer = 0,
        explanation = "Der Pelikan ist bekannt fuer seinen grossen Kehlbeutel, den er wie ein Netz zum Fischfang nutzt. Der Beutel kann bis zu 13 Liter Wasser fassen.",
        difficulty = 1,
        funFact = "Ein Pelikan kann mit seinem Kehlbeutel mehr Fische auf einmal transportieren als in seinem Magen Platz haette."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher schwarze Vogel ist fuer seine Intelligenz und seinen Ruf als Unheilsbote bekannt?",
        answerA = "Rabe",
        answerB = "Dohle",
        answerC = "Saatkraehe",
        answerD = "Rabenkraehe",
        correctAnswer = 0,
        explanation = "Der Rabe ist ein hochintelligenter Vogel, der in der Mythologie vieler Kulturen als Unheilsbote gilt. In Wirklichkeit ist er ein faszinierendes und anpassungsfaehiges Tier.",
        difficulty = 1,
        funFact = "Raben koennen Probleme loesen, die selbst fuer Menschenkinder im Vorschulalter schwierig sind."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Vogel legt seine Eier in den Sand statt in ein Nest?",
        answerA = "Strauss",
        answerB = "Emu",
        answerC = "Kiebitz",
        answerD = "Austernfischer",
        correctAnswer = 0,
        explanation = "Der Strauß legt seine Eier direkt in eine flache Vertiefung im Sand. Das Maennchen bruetet nachts, das Weibchen tagssueber.",
        difficulty = 1,
        funFact = "Ein Strauß kann bis zu 12 Eier in ein gemeinsames Nest legen, an dem mehrere Weibchen beteiligt sind."
    ),

    Question(
        categoryId = 9,
        questionText = "Wie nennt man den buschigen Federschmuck auf dem Kopf mancher Voegel?",
        answerA = "Krone",
        answerB = "Haube",
        answerC = "Schopf",
        answerD = "Helmfeder",
        correctAnswer = 2,
        explanation = "Der Schopf oder Federbusch auf dem Kopf mancher Voegel wie dem Wiedehopf oder dem Kakadu dient der Kommunikation und kann aufgestellt werden.",
        difficulty = 1,
        funFact = "Der Wiedehopf kann seinen Schopf blitzschnell auf- und zuklappen und nutzt dies zur Signalgebung an andere Voegel."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Seevogel ist fuer seine enormen Fluegelspannweite und lange Seereisen bekannt?",
        answerA = "Moewe",
        answerB = "Albatros",
        answerC = "Sturmvogel",
        answerD = "Basstoeoel",
        correctAnswer = 1,
        explanation = "Der Wanderalbatros hat die groesste Fluegelspannweite aller lebenden Voegel mit bis zu 3,5 Metern. Er kann jahrelang auf dem Meer leben, ohne Land zu beruehren.",
        difficulty = 1,
        funFact = "Albatrosse koennen im Schlaf fliegen, da eine Gehirnhaelfte schlaeft waehrend die andere wach bleibt."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Vogel ist in Deutschland ein haeufiger Stadtbewohner und gilt als Friedenssymbol?",
        answerA = "Spatz",
        answerB = "Taube",
        answerC = "Star",
        answerD = "Amsel",
        correctAnswer = 1,
        explanation = "Die Taube ist in deutschen Staedten allgegenwaertig und gilt seit der Antike als Symbol des Friedens. Die weisse Taube ist besonders als Friedenssymbol bekannt.",
        difficulty = 1,
        funFact = "Tauben haben ein hervorragendes Orientierungsvermoeogen und wurden fruehher als Brieftraeger eingesetzt."
    ),

    Question(
        categoryId = 9,
        questionText = "Welcher Vogel ist bekannt fuer seinen schwarzen Federkoerper und seinen gelben Schnabel?",
        answerA = "Drossel",
        answerB = "Amsel",
        answerC = "Star",
        answerD = "Kraehe",
        correctAnswer = 1,
        explanation = "Die maennliche Amsel hat ein vollkommen schwarzes Gefieder mit einem gelben Schnabel. Weibchen sind braun gefaerbt.",
        difficulty = 1,
        funFact = "Die Amsel ist neben dem Buchfinken einer der lautesten und vielseitigsten Saenger unter den deutschen Voegeln."
    ),

)
