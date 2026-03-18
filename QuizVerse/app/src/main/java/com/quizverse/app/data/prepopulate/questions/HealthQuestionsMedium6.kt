package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun healthQuestionsMedium6(): List<Question> = listOf(

    // Question 1
    Question(
        categoryId = 16,
        questionText = "Was ist das vordere Kreuzband (ACL) und welche Bewegung reisst es am haeufigsten?",
        answerA = "Ein Muskel im Oberschenkel -- tritt beim Sprinten",
        answerB = "Ein Knieband -- reisst oft bei abruptem Abbremsen oder Drehbewegungen",
        answerC = "Ein Sehnenansatz -- reisst bei Ueberstreckung des Hueftgelenks",
        answerD = "Ein Knochen im Kniegelenk -- bricht bei direktem Aufprall",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Das vordere Kreuzband stabilisiert das Knie und verhindert das Vorwaertsgleiten des Unterschenkels. Es reisst besonders haeufig beim ploetzlichen Stoppen oder seitlichen Drehbewegungen im Knie.",
        funFact = "Frauen reissen das vordere Kreuzband bis zu sechsmal haeufiger als Maenner -- vermutlich wegen anatomischer Unterschiede wie einem breiteren Becken und einem anderen Kniewinkel."
    ),

    // Question 2
    Question(
        categoryId = 16,
        questionText = "Was ist ein Meniskus im Kniegelenk?",
        answerA = "Ein Sehnenband, das Oberschenkel und Unterschenkel verbindet",
        answerB = "Eine C-foermige Knorpelscheibe, die als Stossdaempfer im Knie wirkt",
        answerC = "Ein Schleimbeutel zur Reibungsminderung im Kniegelenk",
        answerD = "Ein Knochen, der den Gelenkspalt ueberbrueckt",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Der Meniskus ist eine halbmondfoermige Knorpelstruktur im Kniegelenk, die Stoesse abfedert, das Gewicht verteilt und die Gelenkstabilitaet verbessert. Im Knie gibt es einen Innen- und einen Aussenmeniskus.",
        funFact = "Der Innenmeniskus reisst etwa dreimal haeufiger als der Aussenmeniskus, weil er weniger beweglich ist und staerker mit dem Innenband verwachsen ist."
    ),

    // Question 3
    Question(
        categoryId = 16,
        questionText = "Was versteht man unter Tendinitis?",
        answerA = "Eine Entzuendung des Knochens nach Ueberlastung",
        answerB = "Eine Entzuendung oder Reizung einer Sehne durch Ueberlastung",
        answerC = "Eine Muskelverletzung durch zu intensives Krafttraining",
        answerD = "Eine Schleimbeutelentzuendung im Schultergelenk",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Tendinitis bezeichnet eine Entzuendung oder Reizung einer Sehne, meist durch wiederholte Belastung oder Ueberlastung. Typische Beispiele sind die Achillessehnenentzuendung beim Laufen und der Tennisellenbogen.",
        funFact = "Der Begriff Tendinitis ist medizinisch umstritten -- Biopsien zeigen oft keine echte Entzuendung, weshalb viele Experten heute den Begriff Tendinopathie bevorzugen."
    ),

    // Question 4
    Question(
        categoryId = 16,
        questionText = "Welches PRICE-Schema wird bei akuten Sportverletzungen angewendet?",
        answerA = "Protect, Rest, Ice, Compression, Elevation",
        answerB = "Pain, Run, Inject, Cool, Exercise",
        answerC = "Pressure, Relax, Immobilize, Compress, Elevate",
        answerD = "Protect, Rehabilitate, Inject, Cycle, Exercise",
        correctAnswer = 0,
        difficulty = 2,
        explanation = "Das PRICE-Schema steht fuer Protect (schuetzen), Rest (ruhen), Ice (kuehlen), Compression (komprimieren) und Elevation (hochlagern) -- es reduziert Schmerz und Schwellung in den ersten Stunden nach einer Verletzung.",
        funFact = "Das PRICE-Schema ersetzt das aeltere RICE-Schema (ohne Protection) und wird zunehmend durch POLICE ersetzt: Optimal Loading statt vollstaendige Ruhe, da moderate Bewegung die Heilung foerdert."
    ),

    // Question 5
    Question(
        categoryId = 16,
        questionText = "Was ist das Ziel der Physiotherapie nach einem Kreuzbandriss?",
        answerA = "Den operierten Bereich dauerhaft ruhig zu stellen",
        answerB = "Muskelaufbau, Koordination und stufenweise Wiederherstellung der Gelenkfunktion",
        answerC = "Schmerztherapie durch Waermebehandlung und Entspannung",
        answerD = "Das Entfernen von Narbengewebe durch Ultraschalltherapie",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Nach einem Kreuzbandriss zielt die Physiotherapie auf den Wiederaufbau der Oberschenkelmuskulatur, die Verbesserung der Propriozeption (Koerperwahrnehmung) und die schrittweise Rueckkehr zu sportlichen Aktivitaeten ab.",
        funFact = "Die vollstaendige Rehabilitation nach einem Kreuzbandriss dauert in der Regel 9 bis 12 Monate -- Athleten, die zu frueh zurueckkehren, haben ein deutlich erhoehtes Risiko fuer eine erneute Verletzung."
    ),

    // Question 6
    Question(
        categoryId = 16,
        questionText = "Ab welchem Alter spricht man beim Wachstum von Kindern von einem Wachstumsschub?",
        answerA = "Wenn ein Kind innerhalb eines Jahres mehr als 10 cm waechst",
        answerB = "Wenn ein Kind in einem Monat mehr als 2 cm waechst",
        answerC = "Wenn ein Kind die durchschnittliche Koerpergroesse seiner Altersgruppe ueberschreitet",
        answerD = "Wenn ein Kind ploetzlich mehr als 5 kg zunimmt",
        correctAnswer = 0,
        difficulty = 2,
        explanation = "Als Wachstumsschub bezeichnet man eine Phase beschleunigten Laengenwachstums, wobei Kinder in der Pubertaet bis zu 10 cm oder mehr pro Jahr wachsen koennen -- das ist der groesste Wachstumsschub nach dem Saeuglings-stadium.",
        funFact = "Knochen wachsen nachts schneller als am Tag -- Kinder schlafen oft mehr in Wachstumsphasen und berichten von sogenannten Wachstumsschmerzen in den Beinen."
    ),

    // Question 7
    Question(
        categoryId = 16,
        questionText = "In welchem Alter sollte ein gesundes Kind seine ersten Worte sprechen?",
        answerA = "Bis zum 6. Lebensmonat",
        answerB = "Zwischen 10 und 14 Monaten",
        answerC = "Erst ab dem 2. Geburtstag",
        answerD = "Zwischen 3 und 4 Jahren",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Die meisten Kinder sprechen ihr erstes bedeutungsvolles Wort (z.B. Mama, Papa, Ball) zwischen dem 10. und 14. Lebensmonat. Bis zum 2. Geburtstag sollten etwa 50 Worte und erste Zweiwortsaetze vorhanden sein.",
        funFact = "Kinder, die zweisprachig aufwachsen, beginnen manchmal etwas spaeter mit dem Sprechen, erreichen aber in der Regel dieselben Meilensteine und entwickeln langfristig groessere kognitive Flexibilitaet."
    ),

    // Question 8
    Question(
        categoryId = 16,
        questionText = "Was ist das U-Heft und welche Funktion hat es in Deutschland?",
        answerA = "Ein Schulheft fuer Grundschulkinder mit Unterstuetzungsbedarf",
        answerB = "Das gelbe Vorsorgeheft, in dem Kinderuntersuchungen (U1-U10) dokumentiert werden",
        answerC = "Eine Unfallversicherungskarte fuer Kinder unter 14 Jahren",
        answerD = "Ein Impfausweis speziell fuer Kinderkrankheiten",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Das gelbe U-Heft (Kinderuntersuchungsheft) dokumentiert die gesetzlichen Vorsorgeuntersuchungen U1 bis U10 in Deutschland und erfasst Wachstum, Entwicklung, Impfungen und moegliche Erkrankungen.",
        funFact = "Die U-Untersuchungen sind zwar gesetzlich vorgesehen, aber nicht verpflichtend -- einige Bundeslaender wie Bayern und Hessen haben jedoch ein verbindliches Einladewesen eingerichtet, um sicherzustellen, dass kein Kind die Untersuchungen verpasst."
    ),

    // Question 9
    Question(
        categoryId = 16,
        questionText = "Was ist die Menstruationszyklusdauer bei den meisten Frauen?",
        answerA = "Genau 28 Tage ohne Ausnahme",
        answerB = "Zwischen 21 und 35 Tagen, im Durchschnitt etwa 28 Tage",
        answerC = "Immer zwischen 30 und 40 Tagen",
        answerD = "Unter 21 Tagen bei gesunden Frauen",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Ein normaler Menstruationszyklus dauert zwischen 21 und 35 Tagen -- der oft zitierte 28-Tage-Zyklus ist ein Durchschnittswert, aber grosse individuelle Schwankungen sind voellig normal.",
        funFact = "Stress, extreme sportliche Betaetigung, Ernaehrungsmangel und Reisen koennen den Menstruationszyklus verschieben oder vorueberhend aussetzen lassen -- das Gehirn steuert den Zyklus ueber Hormone."
    ),

    // Question 10
    Question(
        categoryId = 16,
        questionText = "Ab wann spricht man medizinisch von den Wechseljahren (Menopause)?",
        answerA = "Wenn die Periode seit genau einem Jahr ausgeblieben ist",
        answerB = "Wenn die Hormonwerte unter einen bestimmten Schwellenwert fallen",
        answerC = "Ab dem 45. Lebensjahr, unabhaengig von der Periode",
        answerD = "Wenn Hitzewallungen und Schweissausbrueche auftreten",
        correctAnswer = 0,
        difficulty = 2,
        explanation = "Die Menopause wird rueckblickend definiert: Nach 12 aufeinanderfolgenden Monaten ohne Menstruation gilt die letzte Blutung als Menopause. In Deutschland liegt das Durchschnittsalter bei etwa 51 Jahren.",
        funFact = "Die Phase vor der Menopause -- die Perimenopause -- kann 4 bis 8 Jahre dauern und ist oft die beschwerlichste Phase, da Hormone stark schwanken und Symptome wie Hitzewallungen und Stimmungsschwankungen auftreten."
    ),

    // Question 11
    Question(
        categoryId = 16,
        questionText = "Was ist Praeklampsie in der Schwangerschaft?",
        answerA = "Uebelkeit und Erbrechen in den ersten drei Schwangerschaftsmonaten",
        answerB = "Bluthochdruck mit Eiweiss im Urin nach der 20. Schwangerschaftswoche",
        answerC = "Eine vorzeitige Plazentaloesung vor dem errechneten Termin",
        answerD = "Erhoehter Blutzucker waehrend der Schwangerschaft",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Praeklampsie ist eine ernste Schwangerschaftskomplikation mit Bluthochdruck und Proteinurie nach der 20. SSW -- sie kann zu schwerwiegenden Komplikationen fuer Mutter und Kind fuehren und erfordert engmaschige medizinische Ueberwachung.",
        funFact = "Praeklampsie betrifft weltweit etwa 5 bis 8 Prozent aller Schwangerschaften und ist eine der fuehrenden Ursachen fuer muetterliche und kindliche Sterblichkeit -- eine fruehzeitige Behandlung kann lebensrettend sein."
    ),

    // Question 12
    Question(
        categoryId = 16,
        questionText = "Was ist Gestationsdiabetes?",
        answerA = "Diabetes, der ausschliesslich in den Wechseljahren auftritt",
        answerB = "Eine Form des Diabetes, die erstmals waehrend der Schwangerschaft auftritt",
        answerC = "Typ-1-Diabetes bei Frauen unter 30 Jahren",
        answerD = "Erbliche Zuckerkrankheit, die bei der Geburt diagnostiziert wird",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Gestationsdiabetes ist ein erhohter Blutzucker, der erstmals in der Schwangerschaft festgestellt wird. Er erhoht das Risiko fuer Komplikationen und verschwindet meist nach der Geburt, erhoert aber das spaetere Risiko fuer Typ-2-Diabetes.",
        funFact = "Etwa 10 bis 15 Prozent aller Schwangeren in Deutschland entwickeln einen Gestationsdiabetes -- der Screening-Test per Zuckerlosung (oGTT) ist seit 2012 eine Kassenleistung fuer alle Schwangeren."
    ),

    // Question 13
    Question(
        categoryId = 16,
        questionText = "Was ist die haeufigste Ursache fuer erektile Dysfunktion bei Maennern ab 40?",
        answerA = "Psychischer Stress und Angst vor Versagen",
        answerB = "Gefaesserkrankungen und Durchblutungsstoerungen",
        answerC = "Testosteronmangel durch Alterung",
        answerD = "Rueckenprobleme und Nervenschaeden der Wirbelsaeule",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Die haeufigste organische Ursache der erektilen Dysfunktion sind Gefaesserkrankungen -- verengte Arterien reduzieren den Blutfluss in den Penis. Deshalb gilt erektile Dysfunktion oft als Fruehwarnzeichen fuer Herz-Kreislauf-Erkrankungen.",
        funFact = "Erektile Dysfunktion kann bis zu 3 Jahre vor einem Herzinfarkt oder Schlaganfall auftreten -- Aertze betrachten sie heute als wichtiges kardiovaskulaeres Warnsignal und empfehlen eine Herzuntersuchung."
    ),

    // Question 14
    Question(
        categoryId = 16,
        questionText = "Was misst der PSA-Test beim Mann?",
        answerA = "Den Testosteronspiegel im Blut",
        answerB = "Das Prostata-spezifische Antigen als Hinweis auf Prostataerkrankungen",
        answerC = "Die Groesse der Prostata per Bluttest",
        answerD = "Das Krebsrisiko allgemein durch einen genetischen Marker",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Der PSA-Test misst das Prostata-spezifische Antigen im Blut -- ein erhoehter Wert kann auf Prostatakrebs, aber auch auf gutartige Vergroesserung oder Entzuendung hinweisen. Er dient als Screening, ist aber kein eindeutiger Krebsnachweis.",
        funFact = "Der PSA-Test ist in der Medizin umstritten -- er kann zu Ueberdiagnosen fuehren, da viele Prostatakarzinome so langsam wachsen, dass sie nie Beschwerden verursachen haetten. In einigen Laendern wird er nicht als Routinetest empfohlen."
    ),

    // Question 15
    Question(
        categoryId = 16,
        questionText = "Was ist Sarkopenie im Alter?",
        answerA = "Knochenschwund durch Calciummangel im hohen Alter",
        answerB = "Der altersbedingte Verlust von Muskelmasse und Muskelkraft",
        answerC = "Eine Erkrankung der Herzmuskelzellen bei Senioren",
        answerD = "Die Verhhaertung von Sehnen und Baendern im Alter",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Sarkopenie bezeichnet den progressiven Verlust von Skelettmuskelmasse und -funktion im Alter. Ab dem 40. Lebensjahr verliert der Mensch ohne Gegenmassnahmen jedes Jahrzehnt etwa 8 Prozent seiner Muskelmasse.",
        funFact = "Krafttraining ist die effektivste Massnahme gegen Sarkopenie -- selbst 80-jaehrige koennen durch regelmassiges Widerstandstraining signifikant Muskelmasse aufbauen und Sturzrisiko reduzieren."
    ),

    // Question 16
    Question(
        categoryId = 16,
        questionText = "Welche Massnahme senkt das Sturzrisiko bei aelteren Menschen am effektivsten?",
        answerA = "Das Tragen rutschfester Schuhe",
        answerB = "Kraft- und Gleichgewichtstraining",
        answerC = "Das Entfernen aller Teppiche aus der Wohnung",
        answerD = "Die Einnahme von Calcium- und Vitamin-D-Praeparaten",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Kraft- und Gleichgewichtstraining ist die evidenzbasiert wirksamste Einzelmassnahme zur Sturzpraevention -- es verbessert Muskelkraft, Reaktionszeit und Propriozeption gleichzeitig.",
        funFact = "Jede dritte Person ueber 65 Jahren stuerzt mindestens einmal jaehrlich -- Stuerze sind die haeufigste Ursache fuer verletzungsbedingte Sterblichkeit bei Menschen ab 70 Jahren in Deutschland."
    ),

    // Question 17
    Question(
        categoryId = 16,
        questionText = "Was beschreibt der Feinstaubwert PM2.5 in der Luftqualitaet?",
        answerA = "Partikel mit einem Durchmesser von 2,5 Metern im Aussenbereich",
        answerB = "Feinstaub mit einem Partikeldurchmesser kleiner als 2,5 Mikrometer",
        answerC = "Den Stickstoffgehalt in der Luft in Teilen pro Million",
        answerD = "Ozonkonzentration bei einer Temperatur von 2,5 Grad Celsius",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "PM2.5 sind Feinstaubpartikel mit einem Durchmesser kleiner als 2,5 Mikrometer -- sie sind so klein, dass sie tief in die Lunge und sogar ins Blut eindringen koennen und Herzerkrankungen, Lungenkrebs und Atemwegserkrankungen verursachen.",
        funFact = "Laut WHO sterben weltweit jedes Jahr etwa 7 Millionen Menschen vorzeitig durch Luftverschmutzung -- Feinstaub PM2.5 ist dabei die gefaehrlichste Einzelkomponente der Aussenluft."
    ),

    // Question 18
    Question(
        categoryId = 16,
        questionText = "Was versteht man unter Laermschwerhoerigkeit?",
        answerA = "Temporaerer Hoerverlust nach einem lauten Konzert, der sich erholt",
        answerB = "Dauerhafter Hoerverlust durch langfristige Laermbelastung ueber 85 dB",
        answerC = "Altersbedingte Schwerhoeigkeit, die durch Laerm beschleunigt wird",
        answerD = "Eine genetische Form der Schwerhoeigkeit bei Laermarbeitern",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Laermschwerhoerigkeit entsteht durch dauerhafte Exposition gegenueber Laerm ueber 85 Dezibel und fuehrt zum irreversiblen Absterben der Haarzellen im Innenohr. Sie ist die haeufigste vermeidbare Ursache von Schwerhoeigkeit.",
        funFact = "Das menschliche Ohr kann sich von kurzfristigen Laermbelastungen erholen -- die haufig auftretende temporaere Schwerhoeigkeit nach einem Konzert ist ein Warnsignal, dass die Lautstae rke gefaehrlich war."
    ),

    // Question 19
    Question(
        categoryId = 16,
        questionText = "Was ist Repetitive Strain Injury (RSI) in der Berufsmedizin?",
        answerA = "Ein Sehnenschaden durch einen einmaligen starken Krafteinsatz",
        answerB = "Schmerzen und Funktionsverlust durch wiederholte gleichfoermige Bewegungen",
        answerC = "Eine psychosomatische Erkrankung bei Bueroangestellten ohne organischen Befund",
        answerD = "Verletzungen durch schweres Heben im Lagerbereich",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "RSI bezeichnet Beschwerden in Muskeln, Sehnen und Nerven durch monotone wiederholte Bewegungen -- typisch bei Bueroangestellten (Maus, Tastatur), Kassiererinnen und in der Fabrikarbeit.",
        funFact = "RSI wird manchmal als 'Sekretaerinnenkrankheit' bezeichnet, obwohl es heute Millionen von Softwareentwicklern, Gamern und alle betrifft, die stundenlang Tastatur und Maus benutzen."
    ),

    // Question 20
    Question(
        categoryId = 16,
        questionText = "Welchen Laermschutz schreibt die EU-Laermschutz-Richtlinie am Arbeitsplatz vor?",
        answerA = "Gehoerschutz ab 75 dB, Pflichtprogramm ab 87 dB",
        answerB = "Gehoerschutzangebot ab 80 dB, Pflicht ab 85 dB",
        answerC = "Gehoerschutz nur bei Dauerlaerm ueber 100 dB",
        answerD = "Keine EU-weite Regelung -- jedes Land entscheidet selbst",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Die EU-Richtlinie 2003/10/EG schreibt vor: Ab 80 dB muss der Arbeitgeber Gehoerschutzmittel bereitstellen, ab 85 dB ist das Tragen verpflichtend. Ueber 87 dB darf der Mitarbeiter gar nicht mehr exponiert werden.",
        funFact = "In Deutschland leiden etwa 5 Millionen Menschen an einer beruflich bedingten Larmschwerhoerigkeit -- sie ist die haeufigste anerkannte Berufskrankheit und kostet die Berufsgenossenschaften jedes Jahr Milliarden."
    ),

    // Question 21
    Question(
        categoryId = 16,
        questionText = "Was ist Malaria und wie wird sie uebertragen?",
        answerA = "Eine Virusinfektion durch verseuchtes Trinkwasser in den Tropen",
        answerB = "Eine Parasitenerkrankung, die durch den Stich infizierter Anopheles-Muecken uebertragen wird",
        answerC = "Eine bakterielle Infektion durch Zeckenstiche in tropischen Waeldern",
        answerD = "Eine Pilzerkrankung der Lunge in feuchten tropischen Regionen",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Malaria wird durch Plasmodium-Parasiten verursacht, die durch den Stich weiblicher Anopheles-Muecken uebertragen werden. Der Parasit befaellt Leberzel len und rote Blutkoerperchen und verursacht Fieberschuebe.",
        funFact = "Malaria toetete historisch mehr Menschen als alle Kriege zusammen -- noch heute sterben jaehrlich etwa 600.000 Menschen daran, hauptsaechlich Kinder unter 5 Jahren in Afrika suedlich der Sahara."
    ),

    // Question 22
    Question(
        categoryId = 16,
        questionText = "Was ist Denguefieber und wo tritt es auf?",
        answerA = "Eine durch Ratten uebertragene Erkrankung in Suedostasien",
        answerB = "Eine durch Muecken uebertragene Viruserkrankung in tropischen und subtropischen Regionen",
        answerC = "Eine Tropenkrankheit durch verseuchtes Badewasser",
        answerD = "Eine durch Zecken uebertragene Erkrankung in Australien",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Denguefieber wird durch das Dengue-Virus verursacht und durch Aedes-Muecken uebertragen. Es kommt hauptsaechlich in tropischen und subtropischen Gebieten vor und verlaeuft mit starkem Fieber, Kopf- und Gliederschmerzen.",
        funFact = "Dengue ist die am schnellsten wachsende durch Muecken uebertragene Krankheit weltweit -- in den letzten 50 Jahren haben sich die Fallzahlen verdreissigfacht, teilweise durch den Klimawandel, der die Mueckenverbreitung ausweitet."
    ),

    // Question 23
    Question(
        categoryId = 16,
        questionText = "Was ist Jetlag und wie entsteht er?",
        answerA = "Muedigkeit durch schlechte Ernaehrung an Bord eines Flugzeugs",
        answerB = "Eine Stoeung des zirkadianen Rhythmus durch das schnelle Ueberqueren von Zeitzonen",
        answerC = "Druckbedingte Kopfschmerzen durch Kabinendruckaenderungen im Flugzeug",
        answerD = "Kreislaufprobleme durch langes Sitzen in der Flugzeugkabine",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Jetlag entsteht, wenn die innere biologische Uhr nicht mit der neuen Ortszeit synchronisiert ist. Der Koerper schuettet Melatonin und Cortisol zu den alten Zeiten aus, was zu Schlafproblemen, Muedigkeit und Konzentrationsstoerungen fuehrt.",
        funFact = "Fluege in Richtung Osten verursachen staerkeren Jetlag als Fluege nach Westen -- das Verlaengern eines Tages (Westflug) ist fuer die innere Uhr einfacher zu verarbeiten als das Verkuerzen (Ostflug)."
    ),

    // Question 24
    Question(
        categoryId = 16,
        questionText = "Was ist Hoehenerkrankung (AMS -- Acute Mountain Sickness) und ab wann tritt sie auf?",
        answerA = "Eine Erkrankung durch Unterkuehlung, die ab 2.000 Metern auftreten kann",
        answerB = "Symptome durch Sauerstoffmangel, typisch ab etwa 2.500 Metern Hoehe",
        answerC = "Schwindel durch den reduzierten Luftdruck ab 5.000 Metern",
        answerD = "Eine Infektionskrankheit durch Bergwasser, die ab 3.000 Metern verbreitet ist",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "AMS entsteht durch den verminderten Sauerstoffpartialdruck in grossen Hoehenlagen und aeussert sich in Kopfschmerzen, Uebelkeit, Schwindel und Schlafproblemen. Symptome beginnen typischerweise ab etwa 2.500 Metern.",
        funFact = "Der einzige wirksame Soforttherapie bei schwerer Hoehenerkrankung ist der Abstieg -- selbst 300 bis 500 Meter tiefer zu gehen kann lebensrettend sein. Das Medikament Acetazolamid (Diamox) kann vorbeugend eingesetzt werden."
    ),

    // Question 25
    Question(
        categoryId = 16,
        questionText = "Welche Impfung wird vor einer Reise nach Gelbfiebergebieten benoetigt?",
        answerA = "Hepatitis-B-Impfung",
        answerB = "Gelbfieberimpfung, die in einigen Laendern gesetzlich vorgeschrieben ist",
        answerC = "Typhusimpfung mit Auffrischung alle 5 Jahre",
        answerD = "Malariaimpfung mit spezieller Tropenformula",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Die Gelbfieberimpfung ist die einzige Impfung, die nach den Internationalen Gesundheitsvorschriften (IHR) von bestimmten Laendern als Einreisebedingung verlangt werden kann. Sie schuetzt lebenslang und wird einmalig gegeben.",
        funFact = "Gelbfieber hat seinen Namen von der Gelbfaerbung der Haut (Ikterus) bei schweren Faellen, die durch Leberversagen entsteht -- das Virus kann innere Blutungen und Multiorganversagen verursachen."
    ),

    // Question 26
    Question(
        categoryId = 16,
        questionText = "Was ist eine Wurzelkanalbehandlung beim Zahnarzt?",
        answerA = "Das Entfernen eines Zahns inklusive seiner Wurzel aus dem Kiefer",
        answerB = "Die Entfernung des Zahnmarks und Reinigung des Wurzelkanals um den Zahn zu erhalten",
        answerC = "Das Implantieren einer kuenstlichen Zahnwurzel aus Titan",
        answerD = "Das Versiegeln tiefer Karies mit einem Kunststoff-Unterfuellung",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Bei einer Wurzelkanalbehandlung wird das infizierte oder abgestorbene Zahnmark (Pulpa) entfernt, der Wurzelkanal gereinigt, desinfiziert und dann dicht versiegelt -- der Zahn bleibt erhalten.",
        funFact = "Eine Wurzelkanalbehandlung ist oft schmerz-freier als ihr Ruf vermuten laesst -- die eigentliche Ursache der Schmerzen ist die Entzuendung vorher, nicht die Behandlung selbst."
    ),

    // Question 27
    Question(
        categoryId = 16,
        questionText = "Wie lange traegt man in der Regel eine feste Zahnspange (Brackets)?",
        answerA = "3 bis 6 Monate",
        answerB = "1 bis 3 Jahre, je nach Schweregrad der Fehlstellung",
        answerC = "Mindestens 5 Jahre ohne Ausnahme",
        answerD = "Nur bis zur Pubertaet, dann wird sie entfernt",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Die Tragedauer einer festen Zahnspange betraegt typischerweise 1 bis 3 Jahre und haengt von der Schwere der Zahn- oder Kieferfehlstellung ab. Danach folgt eine Retentionsphase mit Retainern.",
        funFact = "Nach der aktiven Behandlung mit einer Zahnspange muss ein Retainer getragen werden -- oft lebenslang nachts -- weil Zaehne dazu neigen, in ihre urspruengliche Position zurueckzuwandern."
    ),

    // Question 28
    Question(
        categoryId = 16,
        questionText = "Was ist ein Zahnimplantat und woraus besteht es hauptsaechlich?",
        answerA = "Eine herausnehmbare Prothese aus Kunststoff",
        answerB = "Eine in den Knochen eingesetzte Titanschraube als kuenstliche Zahnwurzel",
        answerC = "Eine aufgeklebte Keramikverblendung auf einem gesunden Zahn",
        answerD = "Ein Goldstift zur Befestigung einer Zahnkrone",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Ein Zahnimplantat ist eine Titanschraube, die chirurgisch in den Kieferknochen eingesetzt wird und als kuenstliche Zahnwurzel dient. Titan ist koerpervertraeglich und verwachst mit dem Knochen (Osseointegration).",
        funFact = "Die Osseointegration -- das Einwachsen von Titan in den Knochen -- wurde 1952 zufallig entdeckt, als ein schwedischer Arzt versuchte, eine Titankammer aus einem Kaninchenknochenknochen zu entfernen und sie sich nicht loesen liess."
    ),

    // Question 29
    Question(
        categoryId = 16,
        questionText = "Was ist Ergonomie am Arbeitsplatz?",
        answerA = "Die wirtschaftliche Optimierung von Arbeitsprozessen",
        answerB = "Die wissenschaftliche Anpassung von Arbeitsmitteln und -umgebung an den Menschen",
        answerC = "Zeitmanagement-Methoden fuer effizientes Arbeiten",
        answerD = "Sicherheitsvorschriften fuer Schwerbehinderte am Arbeitsplatz",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Ergonomie ist die Wissenschaft der Anpassung von Arbeitsumgebung, Werkzeugen und Arbeitsablaeufen an die koerperlichen und geistigen Faehigkeiten des Menschen -- Ziel ist die Reduzierung von Belastung und Erkrankungen.",
        funFact = "Ein ergonomisch eingestellter Schreibtischstuhl sollte taeglich neu angepasst werden, wenn mehrere Personen ihn nutzen -- die optimale Einstellung ist hochgradig individuell und ein falscher Sitz kostet langfristig Gesundheit und Produktivitaet."
    ),

    // Question 30
    Question(
        categoryId = 16,
        questionText = "Was ist Schichtarbeit-Syndrom und wen betrifft es?",
        answerA = "Rueckenschmerzen durch langes Stehen in Schichtbetrieben",
        answerB = "Gesundheitliche Probleme durch Arbeit zu ungewoehnlichen Schlaf- und Wachzeiten",
        answerC = "Psychische Belastung durch monotone Fliessband arbeit",
        answerD = "Soziale Isolation durch Nachtarbeit ausschliesslich bei Maennern",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Schichtarbeit-Syndrom entsteht durch dauerhafte Stoeung des zirkadianen Rhythmus bei Arbeitnehmern in Nacht- und Wechselschichten -- Folgen sind Schlafproblem, hoehere Risiken fuer Herz-Kreislauf-Erkrankungen und metabolische Stoerungen.",
        funFact = "Langfristige Nachtschichtarbeit ist laut WHO als moegliches Karzinogen eingestuft -- Studien zeigen ein erhoehtes Brustkrebsrisiko bei Krankenschwestern mit langen Nachtschicht-Karrieren."
    ),

    // Question 31
    Question(
        categoryId = 16,
        questionText = "Was ist Tinnitus und welche Struktur ist betroffen?",
        answerA = "Uebelkeit durch Gleichgewichtsstoerung im Mittelohr",
        answerB = "Wahrnehmung von Toenen ohne externe Schallquelle durch Stoerungen im Hoersystem",
        answerC = "Drueckendes Hoergefuehl durch Fluessigkeit im Mittelohr",
        answerD = "Temporaere Taubheit nach lautem Laerm",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Tinnitus ist das Wahrnehmen von Geraueschen (Klingeln, Pfeifen, Rauschen) ohne externe Schallquelle. Er entsteht durch Fehlaktivierung im Hoerkortex, oft nach Laermschaden, Stress oder Durchblutungsstoerungen des Innenohrs.",
        funFact = "Etwa 3 Millionen Deutsche leiden an chronischem Tinnitus -- es gibt keine universell wirksame Heilung, aber Tinnitus-Retraining-Therapie kann helfen, das Geraeusch ins Unterbewusstsein zu verlagern."
    ),

    // Question 32
    Question(
        categoryId = 16,
        questionText = "Was ist das Karpaltunnelsyndrom?",
        answerA = "Ein Muskelriss im Handgelenk durch Ueberlastung",
        answerB = "Einengung des Nervus medianus im Karpaltunnel mit Taubheit und Schmerzen in der Hand",
        answerC = "Eine Sehnenscheidenentzuendung der Beugesehnen im Handgelenk",
        answerD = "Rheumatoide Arthritis, die bevorzugt das Handgelenk befaellt",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Das Karpaltunnelsyndrom entsteht durch Einengung des Nervus medianus im engen Karpaltunnel am Handgelenk. Typische Symptome sind Taubheitsgefuehl und Kribbeln in Daumen, Zeige- und Mittelfinger, besonders nachts.",
        funFact = "Das Karpaltunnelsyndrom ist die haeufigste Nervenkompressionssyndrom weltweit -- ein simpler Provokationstest (Haltung der Handgelenke 60 Sekunden gebeugt) kann die Diagnose bereits am Patientenbett nahelegen."
    ),

    // Question 33
    Question(
        categoryId = 16,
        questionText = "Welche Funktion hat Fluorid in der Zahnpflege?",
        answerA = "Es toetet Kariesbakterien durch antibiotische Wirkung",
        answerB = "Es haertet den Zahnschmelz und hemmt die Saeurereinproduktion von Bakterien",
        answerC = "Es macht den Zahnschmelz weisser durch chemische Bleiche",
        answerD = "Es versiegelt die Dentintubulen und reduziert Empfindlichkeit",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Fluorid staerkt den Zahnschmelz durch Einbau in die Hydroxylapatit-Kristalle (Fluorapatit), der saeureresis-tenter ist. Ausserdem hemmt es Enzyme der Kariesbakterien und foerdert die Remineralisation von Schmelz.",
        funFact = "Die weltweite Kariesreduktion in den letzten Jahrzehnten wird hauptsaechlich auf die Fluoridierung von Zahnpasta zurueckgefuehrt -- die Entdeckung von natuerlicherweise fluoridreichem Wasser und weniger Karies in bestimmten Regionen fuehrte in den 1940ern zur systemischen Forschung."
    ),

    // Question 34
    Question(
        categoryId = 16,
        questionText = "Was ist Parodontitis?",
        answerA = "Karies an der Zahnwurzel unterhalb des Zahnfleischrandes",
        answerB = "Bakterielle Entzuendung des Zahnhalteapparats mit Abbau von Knochen und Zahnfleisch",
        answerC = "Entzuendung des Zahnfleischs ohne Knochenabbau",
        answerD = "Entzuendung des Zahnnervs durch tiefen Kariesbefall",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Parodontitis ist eine chronische bakterielle Erkrankung des Zahnhalteapparats, bei der Zahnfleisch, Parodontalfasern und Knochen abgebaut werden -- unbehandelt kann sie zu Zahnverlust fuehren.",
        funFact = "Schwere Parodontitis erhoert das Risiko fuer Herzerkrankungen und Diabetes -- Parodontitis-Bakterien koennen ins Blut gelangen und systemische Entzuendungsreaktionen ausloesen."
    ),

    // Question 35
    Question(
        categoryId = 16,
        questionText = "Ab welchem Alter sollte das erste Zahnarztbesuch stattfinden?",
        answerA = "Mit dem zweiten Milchzahn, also etwa mit 8 Monaten",
        answerB = "Wenn der erste Zahn durchbricht oder spaetestens mit dem 1. Geburtstag",
        answerC = "Mit dem Wechsel auf bleibende Zaehne, also etwa mit 6 Jahren",
        answerD = "Mit dem 3. Geburtstag, wenn alle Milchzaehne vorhanden sind",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Die Deutsche Gesellschaft fuer Kinderzahnheilkunde empfiehlt den ersten Zahnarztbesuch, sobald der erste Zahn sichtbar ist -- spaetestens aber zum 1. Geburtstag, um Fruehkaries zu erkennen und Eltern zu beraten.",
        funFact = "Karies bei Kleinkindern (Early Childhood Caries) kann schon bei Saeuglingen auftreten, wenn diese mit Milch oder Saft gefuellten Flaschen einschlafen -- der Zucker bietet Bakterien optimale Bedingungen."
    ),

    // Question 36
    Question(
        categoryId = 16,
        questionText = "Was ist Physiotherapie und welche Techniken setzt sie ein?",
        answerA = "Behandlung durch Medikamente und physikalische Mittel wie Ultraschall",
        answerB = "Therapieform, die Bewegung, Manualtechniken, Waerme und Kryotherapie einsetzt",
        answerC = "Psychologische Behandlung von Schmerzpatienten mit Biofeedback",
        answerD = "Sportmedizinische Betreuung von Leistungssportlern",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Physiotherapie nutzt Bewegungstherapie, manuelle Techniken, Waerme, Kaelte, Ultraschall und elektrische Reize, um Funktion wiederherzustellen, Schmerzen zu lindern und Beweglichkeit zu foerdern.",
        funFact = "Physiotherapie ist nach Chirurgie und Medikamenten der drittgroesste Gesundheitsberuf weltweit -- in vielen Laendern duerfen Physiotherapeuten Patienten direkt ohne aerzliche Ueberweisung behandeln."
    ),

    // Question 37
    Question(
        categoryId = 16,
        questionText = "Was ist Trinkwasserqualitaet und welche Grenzwerte gelten in Deutschland fuer Nitrat?",
        answerA = "Maximal 10 mg Nitrat pro Liter, kontrolliert vom Gesundheitsamt",
        answerB = "Maximal 50 mg Nitrat pro Liter, geregelt in der Trinkwasserverordnung",
        answerC = "Maximal 100 mg Nitrat pro Liter bei Haushalten mit Kindern",
        answerD = "Keine gesetzlichen Grenzwerte -- Selbstkontrolle der Versorger",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "In Deutschland gilt laut Trinkwasserverordnung ein Grenzwert von 50 mg Nitrat pro Liter. Erhoehte Nitratwerte koennen besonders fuer Saeug linge gefaehrlich sein, da sie die Sauerstoffversorgung des Blutes stoeren.",
        funFact = "Landwirtschaftliche Duengung ist die Hauptursache fuer Nitrateintrag ins Grundwasser -- in einigen Regionen Deutschlands werden Grenzwerte regelmaessig ueberschritten, was aufwaendige Wasseraufbereitung erfordert."
    ),

    // Question 38
    Question(
        categoryId = 16,
        questionText = "Was ist Dehydratation (Austrocknung) und welche ersten Symptome zeigt sie?",
        answerA = "Ueberschuss an Koerperfluessigkeit mit Oedemen in Beinen und Lunge",
        answerB = "Fluessigkeitsmangel im Koerper mit Durstgefuehl, dunklem Urin und Muedigkeit",
        answerC = "Elektrolytmangel durch Schweiss beim Sport ohne Symptome am Anfang",
        answerD = "Chronischer Wassermangel nur bei aelteren Menschen ohne Durstempfinden",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Dehydratation entsteht, wenn der Koerper mehr Fluessigkeit verliert als aufnimmt. Fruehzeichen sind Durst, dunkler Urin, trockener Mund und Muedigkeit -- bei 2 Prozent Verlust ist die koerperliche und geistige Leistungsfaehigkeit bereits messbar reduziert.",
        funFact = "Das Durstgefuehl setzt erst ein, wenn man bereits etwa 1 bis 2 Prozent des Koerpergewichts an Fluessigkeit verloren hat -- im Alter ist das Durstempfinden oft reduziert, weshalb Senioren haeufig unbewusst zu wenig trinken."
    ),

    // Question 39
    Question(
        categoryId = 16,
        questionText = "Was ist das Biofeedback-Verfahren in der Rehabilitation?",
        answerA = "Eine App zur Messung von Koerperfettanteil und Muskelmasse",
        answerB = "Die Rueckmeldung koerperlicher Signale (z.B. Muskelspannung) an den Patienten zur bewussten Kontrolle",
        answerC = "Ein chirurgisches Verfahren zur Nervenregeneration nach Lahmungen",
        answerD = "Die Messung der Herzfrequenzvariabilitaet bei Sportlern",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Biofeedback macht koerperliche Prozesse (Muskelpannung, Herzrate, Hautleitwert) sichtbar oder hoerbar, damit Patienten lernen, diese bewusst zu beeinflussen -- es wird bei Inkontinenz, chronischen Schmerzen und Stresserkrankungen eingesetzt.",
        funFact = "Biofeedback wurde in den 1960er Jahren entwickelt und hat gezeigt, dass Menschen Koerperfunktionen kontrollieren koennen, die fruehers als vollstaendig unbewusst galten -- wie Blutdruck und Herzrhythmus."
    ),

    // Question 40
    Question(
        categoryId = 16,
        questionText = "Was ist Sonnenstich (Insolation) und wie unterscheidet er sich vom Hitzschlag?",
        answerA = "Sonnenstich und Hitzschlag sind identisch -- beide durch UV-Strahlung",
        answerB = "Sonnenstich betrifft den Kopf durch direkte Sonneneinstrahlung, Hitzschlag den ganzen Koerper durch Ueberhitzung",
        answerC = "Sonnenstich ist eine Erkaeltung nach Sonnenbad, Hitzschlag eine schwere Verbrennung",
        answerD = "Beim Sonnenstich sinkt die Koerpertemperatur, beim Hitzschlag steigt sie",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Sonnenstich entsteht durch direkte Sonneneinstrahlung auf Kopf und Nacken mit Hirnhautreizung. Der Hitzschlag ist die schwerere Erkrankung mit erhohter Koerpertemperatur ueber 40 Grad durch allgemeine Ueberhitzung und Versagen der Waermeregulation.",
        funFact = "Ein Hitzschlag ist ein medizinischer Notfall mit Sterblichkeitsraten bis zu 30 Prozent -- die schnelle Koerperabkuehlung (z.B. Eiswasser-Immersion) innerhalb der ersten Minuten ist lebensrettend."
    ),

    // Question 41
    Question(
        categoryId = 16,
        questionText = "Was ist eine Stressreaktion des Koerpers und welches Hormon steht im Mittelpunkt?",
        answerA = "Insulinausschuettung zur Energiebereitstellung in Notsituationen",
        answerB = "Cortisol und Adrenalin fuer die Kampf-oder-Flucht-Reaktion",
        answerC = "Melatonin zur Erhoehung der Wachheit in gefaehrlichen Situationen",
        answerD = "Testosteron zur Staerkung der Muskeln bei akutem Stress",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Akuter Stress aktiviert die Hypophyse-Nebennieren-Achse -- Adrenalin wirkt sofort (Herzrasen, Pupillenerweiterung), Cortisol haelt laenger an und mobilisiert Energiereserven fuer die Kampf-oder-Flucht-Reaktion.",
        funFact = "Chronisch erhohte Cortisolspiegel schrumpfen nachweislich den Hippokampus -- den Gedaechtnis-bereich des Gehirns -- was erklaert, warum chronischer Stress das Gedaechtnis und die Lernfaehigkeit beeintraechtigt."
    ),

    // Question 42
    Question(
        categoryId = 16,
        questionText = "Was ist eine Sporternaehrung-Strategie vor einem Ausdauerrennen?",
        answerA = "Am Vortag maximal wenig essen, um das Koerpergewicht zu reduzieren",
        answerB = "Carboloading -- Aufladung der Glykogenspeicher durch kohlenhydratreiche Mahlzeiten",
        answerC = "Proteinreiche Ernaehrung direkt vor dem Start fuer Muskeln",
        answerD = "Fettzufuhr erhoehen, da Fett mehr Energie als Kohlenhydrate liefert",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Carboloading (Kohlenhydratsuperkompensation) in den Tagen vor einem Ausdauerrennen maximiert die Glykogenspeicher in Muskeln und Leber -- das verzoegert den 'Einbruch' (Bonking) bei langen Rennen.",
        funFact = "Die Strategie des Carboloadings wurde in den 1960er Jahren von schwedischen Sportmedizinern entwickelt -- fruehe Methoden sahen auch eine mehrtaegige Entleerungsphase vor dem Laden vor, was heute nicht mehr empfohlen wird."
    ),

    // Question 43
    Question(
        categoryId = 16,
        questionText = "Was ist Skoliose?",
        answerA = "Ueberstreckte Lendenwirbelsaeule mit Hohlkreuz",
        answerB = "Seitliche Verkruemmung der Wirbelsaeule, oft idiopathisch bei Jugendlichen",
        answerC = "Versteifung der Wirbelsaeule durch entzuendliche Erkrankung",
        answerD = "Schief stehende Wirbel durch Bandscheibenvorfall",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Skoliose ist eine dreidimensionale Fehlstellung der Wirbelsaeule mit seitlicher Kurve und Rotation der Wirbelkoerper. Die haeufigste Form ist die idiopathische Adoleszentenskoliose, die in der Wachstumsphase auftritt.",
        funFact = "Etwa 2 bis 3 Prozent der Bevoelkerung haben eine Skoliose -- die meisten Faelle sind leicht und benoetigen keine Behandlung, aber starke Kruemmungen ueber 40 Grad koennen die Atemfunktion beeintraechtigen."
    ),

    // Question 44
    Question(
        categoryId = 16,
        questionText = "Was ist die Perimenopause und wie lange dauert sie?",
        answerA = "Die erste Menstruation bei Maedchen in der Pubertaet",
        answerB = "Die Uebergangsphase vor der Menopause mit Hormonschwankungen, die 4 bis 10 Jahre dauern kann",
        answerC = "Die Phase nach der Menopause, die bis zum Tod andauert",
        answerD = "Der erste Monat nach dem Aussetzen der Pille",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Die Perimenopause beginnt Jahre vor der letzten Menstruation, wenn die Ovarialreserve sinkt und Hormone stark schwanken. Sie dauert im Durchschnitt 4 bis 8 Jahre und ist oft die beschwerdereic hste Phase der Menopause.",
        funFact = "Hitzewallungen in der Perimenopause entstehen durch fehlgeleitete Thermoregulations signale im Hypothalamus -- der Koerper 'denkt' faelschlicherweise, er sei zu warm und loest Kuehlmechanismen aus."
    ),

    // Question 45
    Question(
        categoryId = 16,
        questionText = "Was ist Kinderlahmung (Poliomyelitis) und wie wird sie verhindert?",
        answerA = "Eine Muskelschwaeche durch Vitamin-D-Mangel, verhindert durch Sonnenlicht",
        answerB = "Eine durch das Poliovirus verursachte Erkrankung, die Lahmung verursachen kann -- verhindert durch Impfung",
        answerC = "Eine Erbkrankheit, die nur mit Gentechnik behandelt werden kann",
        answerD = "Eine Muskeldystrophie im Kindesalter durch autoimmune Ursachen",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Poliomyelitis wird durch das faekal-oral uebertragene Poliovirus verursacht und kann Vorderhornzellen im Rueckenmark befallen, was zu irreversiblen Lahmungen fuehrt. Die Impfung hat Polio aus fast allen Laendern der Welt verdraengt.",
        funFact = "Dank weltweiter Impfkampagnen ist Polio heute in fast allen Laendern ausgerottet -- 1988 gab es noch 350.000 Faelle jaehrlich, 2023 waren es weniger als 10 in Wildvirus-Faellen weltweit."
    ),

    // Question 46
    Question(
        categoryId = 16,
        questionText = "Was ist eine Stressreaktion und was unterscheidet eustress von Disstress?",
        answerA = "Eustress ist psychischer, Disstress ist koerperlicher Stress",
        answerB = "Eustress ist positiver, motivierender Stress -- Disstress ist schaedlicher, laehmender Stress",
        answerC = "Eustress betrifft Kinder, Disstress betrifft Erwachsene",
        answerD = "Eustress ist kurzfristig, Disstress dauert nur wenige Tage",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Eustress (positiver Stress) aktiviert und motiviert -- er tritt bei Wettkampf, verliebt sein oder Neuem auf. Disstress (negativer Stress) laedt dauerhaft auf und fuehrt zu Erschoepfung, Schlafproblemen und Erkrankungen.",
        funFact = "Der Begriff Eustress wurde vom Endokrinologen Hans Selye gepraegt -- er war der erste Forscher, der Stress wissenschaftlich definierte und feststellte, dass manche Stressreaktionen fuer das Ueberleben notwendig und sogar wohltuend sind."
    ),

    // Question 47
    Question(
        categoryId = 16,
        questionText = "Was ist Osteoporose und welche Bevoelkerungsgruppe ist am staerksten betroffen?",
        answerA = "Knochenaufloesung durch Calciummangel -- betrifft hauptsaechlich Maenner ab 50",
        answerB = "Verminderung der Knochendichte mit erhoehtem Frakturrisiko -- betrifft hauptsaechlich Frauen nach der Menopause",
        answerC = "Knochenent zuendung durch Viren -- betrifft hauptsaechlich Kinder",
        answerD = "Knochengeschwulste -- tritt gleichmaessig in allen Altersgruppen auf",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Osteoporose ist eine metabolische Knochenerkrankung mit verminderter Knochendichte und veraenderter Mikrostruktur, die das Frakturrisiko erhoeht. Frauen verlieren nach der Menopause durch den Oestrogenmangel besonders schnell Knochenmasse.",
        funFact = "Ein Osteoporose-Patient bricht sich im Schnitt alle drei Sekunden weltweit einen Knochen -- die Hueftfraktur ist besonders gefuerchtet, da sie bei aelteren Menschen eine 20-30 Prozent hoehere Sterblichkeit im ersten Jahr bedeutet."
    ),

    // Question 48
    Question(
        categoryId = 16,
        questionText = "Was ist eine Leistenzerrung im Sport und welcher Muskel ist betroffen?",
        answerA = "Eine Verletzung des Hueftbeugers durch Sprung",
        answerB = "Eine Verletzung der Adduktoren (innere Oberschenkelmuskeln) durch Ueberdehnung",
        answerC = "Ein Einriss des Inguinalbandes beim Laufen",
        answerD = "Eine Entzuendung des Beckenknochens durch wiederholte Belastung",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Die Leistenzerrung ist eine Verletzung der Adduktoren (innere Oberschenkelmuskeln), die beim ploetzlichen Richtungswechsel oder Schlagen beim Fussball entsteht. Sie aeussert sich als Schmerz an der Innenseite des Oberschenkels.",
        funFact = "Leistenverletzungen sind die zweithaeufigste Verletzung im Fussball -- professionelle Fussballspieler haben ein lebenslanges Risiko von etwa 30 Prozent, eine schwere Leistenverletzung zu erleiden."
    ),

    // Question 49
    Question(
        categoryId = 16,
        questionText = "Was ist Tropenkrankheit Typhus abdominalis und wie wird er uebertragen?",
        answerA = "Eine virale Erkrankung durch Mosquitostiche in Suedostasien",
        answerB = "Eine bakterielle Erkrankung durch Salmonella typhi via kontaminiertes Wasser und Lebensmittel",
        answerC = "Eine Pilzerkrankung des Darms durch ungewaschene Tropen fruechte",
        answerD = "Eine parasitaere Erkrankung durch Schneckenstiche in Suesswasser",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Typhus abdominalis wird durch das Bakterium Salmonella typhi verursacht, das faekal-oral uebertragen wird -- meist ueber kontaminiertes Trinkwasser oder Lebensmittel in Regionen mit schlechter Sanitaerversorgung.",
        funFact = "Typhus Mary -- Mary Mallon -- war eine asymptomatische Typhus-Traegerin in New York, die Anfang des 20. Jahrhunderts als Koechi n mindestens 51 Personen infizierte und dreimal unter staatlicher Quarantaene stand."
    ),

    // Question 50
    Question(
        categoryId = 16,
        questionText = "Was ist die Rotatorenmanschette und welche Verletzung ist am haeufigsten?",
        answerA = "Ein Sehnenring um das Kniegelenk -- Abriss bei Kniebeugen",
        answerB = "Vier Schultermuskeln und ihre Sehnen -- Riss der Supraspinatussehne ist am haeufigsten",
        answerC = "Baendsystem des Ellenbogengelenks -- Riss beim Tennis",
        answerD = "Muskeln des unteren Rueckens -- Zerrung beim Heben",
        correctAnswer = 1,
        difficulty = 2,
        explanation = "Die Rotatorenmanschette besteht aus vier Muskeln (Supraspinatus, Infraspinatus, Teres minor, Subscapularis) und ihren Sehnen, die den Oberarmknochen im Schultergelenk stabilisieren. Die Supraspinatussehne reisst am haeufigsten.",
        funFact = "Rotatorenmanschettenrisse kommen bei Menschen ueber 60 Jahren in ueber 20 Prozent der Faelle vor -- die meisten davon verlaufen symptomfrei und werden zufallig entdeckt. Ob jemand Beschwerden hat, haengt stark von der Muskelfunktion ab."
    )

)
