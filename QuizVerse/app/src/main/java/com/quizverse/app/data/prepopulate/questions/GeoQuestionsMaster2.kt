package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun geoQuestionsMaster2(): List<Question> = listOf(

    // ── MASTER (difficulty = 5) ── 29 questions ──────────────────────────────

    // Tektonische Plattengrenzen
    Question(
        categoryId = 1,
        questionText = "An welcher Art von Plattengrenze entstehen die meisten Orogene (Gebirge) der Erde?",
        answerA = "Transformstoerungen",
        answerB = "Divergente Plattengrenzen",
        answerC = "Konvergente Plattengrenzen (Kollisionszonen)",
        answerD = "Ozeanische Spreizungsruecken",
        correctAnswer = 2,
        explanation = "An konvergenten Plattengrenzen stossen tektonische Platten aufeinander. Wenn zwei Kontinentalplatten kollidieren, wird Gestein aufgefaltet und es entstehen Gebirge wie Himalaya (Indien/Eurasien) oder Alpen (Afrika/Europa).",
        difficulty = 5,
        funFact = "Der Himalaya waechst noch heute um etwa 5-10 mm pro Jahr, weil die Indische Platte weiterhin nach Norden driftet und unter die Eurasische Platte schiebt."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche tektonische Plattengrenze verlaeuft durch Island und ermoeglicht die dortige intensive Vulkanaktivitaet?",
        answerA = "Nordamerikanisch-Eurasische konvergente Grenze",
        answerB = "Mittelatlantischer Spreizungsruecken (divergente Grenze)",
        answerC = "Transformstoerung der Faeroeser Platte",
        answerD = "Subduktionszone der Atlantischen Mikroplatte",
        correctAnswer = 1,
        explanation = "Island liegt auf dem Mittelatlantischen Ruecken, einer divergenten Plattengrenze, wo die Nordamerikanische und Eurasische Platte auseinanderdriften. Zusaetzlich liegt Island ueber einem Hotspot, was die Vulkanaktivitaet intensiviert.",
        difficulty = 5,
        funFact = "Island waechst jaehrlich um etwa 2-3 cm, weil die tektonischen Platten auseinanderdriften. Man kann auf Island buchstaeblich mit einem Fuss in Amerika und dem anderen in Europa stehen."
    ),

    Question(
        categoryId = 1,
        questionText = "Die San-Andreas-Verwerfung in Kalifornien ist eine Transformstoerung. Welche beiden Platten gleiten hier aneinander vorbei?",
        answerA = "Nordamerikanische Platte und Pazifische Platte",
        answerB = "Karibische Platte und Nordamerikanische Platte",
        answerC = "Juan-de-Fuca-Platte und Nordamerikanische Platte",
        answerD = "Cocos-Platte und Pazifische Platte",
        correctAnswer = 0,
        explanation = "An der San-Andreas-Verwerfung gleiten die Nordamerikanische Platte und die Pazifische Platte horizontal aneinander vorbei, ohne zu kollidieren oder auseinanderzudriften. Los Angeles bewegt sich dabei jaehrlich etwa 6 cm auf San Francisco zu.",
        difficulty = 5,
        funFact = "In etwa 15 Millionen Jahren wird Los Angeles so weit nach Norden gewandert sein, dass es auf gleicher Breite mit San Francisco liegt – und in 150 Millionen Jahren koennte es an Alaska grenzen."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie heisst die ozeanische Platte, die unter die Nordamerikanische Platte subduziert und Erdbeben im Cascadia-Subduktionssystem verursacht?",
        answerA = "Karibische Platte",
        answerB = "Philippinische Platte",
        answerC = "Juan-de-Fuca-Platte",
        answerD = "Nazca-Platte",
        correctAnswer = 2,
        explanation = "Die Juan-de-Fuca-Platte ist ein Rest der ehemals riesigen Farallon-Platte und subduziert unter die Nordamerikanische Platte. Dies erzeugt das Cascadia-Subduktionssystem mit dem Potenzial fuer Megabeben (Magnitude 9+).",
        difficulty = 5,
        funFact = "Das letzte grosse Cascadia-Beben ereignete sich am 26. Januar 1700 und hatte laut dendrochronologischen Beweisen eine Staerke von etwa 9,0. Der damit verbundene Tsunami wurde sogar in Japan aufgezeichnet."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches geologische Phaenoemen erklaert die Lage der Hawaiianischen Inseln als Kette von zunehmend aelteren Vulkanen in Richtung Nordwesten?",
        answerA = "Subduktionszone des Pazifischen Rings",
        answerB = "Divergente Plattengrenze des Pazifischen Rueckens",
        answerC = "Hotspot unter einer driftenden Platte",
        answerD = "Transformstoerung entlang der Murrayfrakturzone",
        correctAnswer = 2,
        explanation = "Unter Hawaii liegt ein stationaerer Hotspot (mantle plume) im Erdmantel. Die Pazifische Platte bewegt sich langsam in nordwestliche Richtung darueber hinweg, sodass nacheinander neue Vulkane entstehen und die aelteren sich vom Hotspot entfernen und erloschen.",
        difficulty = 5,
        funFact = "Die aeltesten Vulkanreste der Hawaii-Emperor-Kette, die sogenannten Emperor-Seamounts, befinden sich nahe dem Aleuten-Graben und sind bis zu 80 Millionen Jahre alt."
    ),

    // Ozeanographie
    Question(
        categoryId = 1,
        questionText = "Welchen Anteil am globalen Ozeanvolumen hat der Pazifische Ozean?",
        answerA = "Etwa 32 %",
        answerB = "Etwa 46 %",
        answerC = "Etwa 53 %",
        answerD = "Etwa 60 %",
        correctAnswer = 2,
        explanation = "Der Pazifische Ozean umfasst etwa 710 Millionen km³ und macht damit rund 53 % des gesamten Ozeanvolumens aus. Er ist groesser als alle Landmassen zusammen und enthaelt mehr als die Haelfte allen Meerwassers der Erde.",
        difficulty = 5,
        funFact = "Der Pazifik schrumpft tektonisch, weil die umgebenden Kontinente auf ihn zuruecken. Der Atlantik hingegen waechst jaehrlich um einige Zentimeter – das Gegenteil ist beim Oststosstypus der Platten der Fall."
    ),

    Question(
        categoryId = 1,
        questionText = "Was ist die 'Thermohaline Zirkulation' und welche zwei physikalischen Faktoren treiben sie an?",
        answerA = "Meeresstroemungen durch Wind und Erdrotation (Corioliskraft)",
        answerB = "Globale Meeresumwaelzung durch Dichteunterschiede aus Temperatur und Salzgehalt",
        answerC = "Gezeitenstroemungen durch Mond- und Sonnengravitation",
        answerD = "Turbulenz durch unterseeische Vulkane und geothermische Waerme",
        correctAnswer = 1,
        explanation = "Die thermohaline Zirkulation (auch 'globales Foerderband') wird durch Dichteunterschiede angetrieben: kaltes, salzreiches Wasser ist dichter und sinkt ab (Thermo = Temperatur, Halin = Salzgehalt). Dies erzeugt Tiefenstroemungen, die ueber alle Ozeane fuehren.",
        difficulty = 5,
        funFact = "Ein vollstaendiger Kreislauf der thermohalinen Zirkulation dauert etwa 1.000-2.000 Jahre. Wasser, das heute im Nordatlantik absinkt, wird erst in weit ueber tausend Jahren wieder an die Oberflaeche steigen."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie heisst die tiefste bekannte Stelle im Indischen Ozean?",
        answerA = "Diamantina-Tiefe",
        answerB = "Java-Tiefe (Sunda-Graben)",
        answerC = "Agulhas-Beckentiefe",
        answerD = "Amiranten-Tiefe",
        correctAnswer = 1,
        explanation = "Die Java-Tiefe im Sunda-Graben (auch Java-Graben) ist die tiefste Stelle des Indischen Ozeans mit ca. 7.290 m. Sie liegt suedlich von Java, wo die Australische Platte unter die Eurasische Platte subduziert.",
        difficulty = 5,
        funFact = "Der Sunda-Graben war der Ausgangspunkt des verheerenden Tsunamis vom 26. Dezember 2004, der durch ein Beben der Staerke 9,1 ausgeloest wurde und fast 230.000 Menschenleben forderte."
    ),

    Question(
        categoryId = 1,
        questionText = "Was versteht man unter dem Begriff 'Chemokline' in der Ozeanographie?",
        answerA = "Uebergangszone zwischen unterschiedlichen Wassertemperaturen",
        answerB = "Uebergangsschicht mit starkem Aenderungsgradienten im Salzgehalt",
        answerC = "Zone mit maximalem Phytoplankton-Wachstum",
        answerD = "Grenzflaeche zwischen Meerwasser und Suedpoleis",
        correctAnswer = 1,
        explanation = "Die Chemokline ist eine Uebergangsschicht im Ozean, in der chemische Parameter (besonders Salzgehalt, aber auch Sauerstoff oder Naehrstoffe) einen steilen Gradienten aufweisen. Sie ist eng verwandt mit der Halokline (Salzgehaltgradient) und Pyknozkline (Dichtegradient).",
        difficulty = 5,
        funFact = "Im Schwarzen Meer gibt es eine ausgepragte Chemokline: Darunter ist das Wasser sauerstofffrei und enthaelt Schwefelwasserstoff – ideale Bedingungen fuer die Konservierung antiker Schiffswracks."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Ozean hat die geringste mittlere Tiefe?",
        answerA = "Arktischer Ozean",
        answerB = "Indischer Ozean",
        answerC = "Suedlicher Ozean",
        answerD = "Atlantischer Ozean",
        correctAnswer = 0,
        explanation = "Der Arktische Ozean hat eine mittlere Tiefe von nur etwa 1.205 m, waehrend der Pazifik (4.000 m), Atlantik (3.332 m) und Indik (3.897 m) tiefer sind. Er ist zudem der kleinste der fuenf Ozeane.",
        difficulty = 5,
        funFact = "Obwohl der Arktische Ozean der flachste ist, birgt er bedeutende Meeresesbecken wie das Eurasische Becken mit 5.449 m Tiefe – tiefste Stelle ist das Molloy-Tief mit 5.550 m."
    ),

    // Historische Kartographie
    Question(
        categoryId = 1,
        questionText = "Welcher Gelehrte des 2. Jahrhunderts n.Chr. verfasste die 'Geographia', die erste systematische Beschreibung der bekannten Welt mit Koordinatensystem?",
        answerA = "Aristoteles",
        answerB = "Eratosthenes",
        answerC = "Klaudios Ptolemaios",
        answerD = "Strabon",
        correctAnswer = 2,
        explanation = "Klaudios Ptolemaios erstellte im 2. Jahrhundert n.Chr. in Alexandria seine 'Geographia' mit einem Koordinatensystem aus Laengen- und Breitengraden und Projektionsverfahren. Dieses Werk beeinflusste die europaeische Kartographie bis ins 16. Jahrhundert.",
        difficulty = 5,
        funFact = "Ptolemaios unterschaetzte den Erdumfang erheblich und hielt Asien fuer groesser als es ist – Kolumbus stuetzte sich genau auf diese Fehler, als er glaubte, Asien westwaerts erreichen zu koennen."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie heisst die Kartenprojektion, bei der alle Rhumblinien (Kurse konstanten Kompasskurses) als gerade Linien erscheinen?",
        answerA = "Azimutale Gleichflaechenprojektion",
        answerB = "Mercator-Projektion",
        answerC = "Peters-Projektion",
        answerD = "Mollweide-Projektion",
        correctAnswer = 1,
        explanation = "Die Mercator-Projektion (1569 von Gerhard Mercator) ist eine winkeltreue Zylinderprojektion. Sie verzerrt Flaechen stark (Pol-Regionen erscheinen zu gross), zeigt aber Rhumblinien als gerade Linien – ideal fuer die nautische Navigation.",
        difficulty = 5,
        funFact = "Auf Mercator-Karten erscheint Groenland aehnlich gross wie Afrika – dabei ist Afrika 14-mal groesser. Die Gauss-Krueger- oder Peters-Projektion versuchen, flaechentreu zu sein."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher arabische Geograph des 12. Jahrhunderts erstellte eine der praezisesten Weltkarten des Mittelalters, die 'Tabula Rogeriana'?",
        answerA = "Ibn Battuta",
        answerB = "Al-Idrisi",
        answerC = "Ibn Khaldun",
        answerD = "Al-Masudi",
        correctAnswer = 1,
        explanation = "Muhammad al-Idrisi erstellte 1154 im Auftrag des normannischen Koenigs Roger II. von Sizilien die Tabula Rogeriana, eine der genauesten und detailliertesten Weltkarten des Mittelalters. Sie war mehr als 300 Jahre lang ein Standardwerk.",
        difficulty = 5,
        funFact = "Al-Idrisas Karte war sueden-orientiert (Sueden oben), was im arabisch-islamischen Kartographiekontext ueblich war. Spaetere europaeische Kopien wurden gedreht, um Norden oben zu zeigen."
    ),

    Question(
        categoryId = 1,
        questionText = "Wie heisst die kartographische Methode, bei der Hoehenunterschiede durch Schraffen (Strichsysteme) auf einer Karte dargestellt werden?",
        answerA = "Isohypsenverfahren",
        answerB = "Schummerung",
        answerC = "Hachuren-Methode",
        answerD = "Choroplethenmethode",
        correctAnswer = 2,
        explanation = "Die Hachuren-Methode (benannt nach dem Militaerkartographen Lehmann) nutzt kurze Strichsysteme (Hachuren) zur Reliefdarstellung: Je steiler das Gelaende, desto dichter und breiter die Striche. Sie wurde im 18./19. Jahrhundert entwickelt, bevor Hoehenlinien (Isolinien) ueblich wurden.",
        difficulty = 5,
        funFact = "Heute verwendet man meist Hoehenschichten (Isolinien/Aequidistanzlinien) und digitale Schummerung. Alte Hachurenkarten gelten als Kunstwerke – die Praezision der Zeichenarbeit war enormen handwerklich."
    ),

    // Geodaesie
    Question(
        categoryId = 1,
        questionText = "Wie heisst das Referenzellipsoid, das die Form der Erde fuer globale GPS-Koordinaten definiert?",
        answerA = "Bessel-Ellipsoid 1841",
        answerB = "WGS 84",
        answerC = "GRS 80",
        answerD = "ETRS89",
        correctAnswer = 1,
        explanation = "WGS 84 (World Geodetic System 1984) ist das globale Referenzsystem fuer GPS und definiert ein Ellipsoid, das die Form der Erde annaehert. Es wird von allen GPS-Geraeten verwendet und ist das weltweit gaengigste geodaetische Datum.",
        difficulty = 5,
        funFact = "Die Erde ist kein perfekter Koerper – sie ist am Aequator abgeplattet und an den Polen gestaucht. Das Referenzellipsoid WGS 84 gibt den Aequatorialradius mit 6.378.137 m an, den Polradius mit 6.356.752 m."
    ),

    Question(
        categoryId = 1,
        questionText = "Was ist das 'Geoid' in der Geodaesie?",
        answerA = "Das mathematische Ellipsoid als Erdmodell",
        answerB = "Die Aequipotentialflaeche des Erdschwerefeldes auf Meeresspiegel-Niveau",
        answerC = "Das Netz der Meridiane und Breitengrade",
        answerD = "Die dreidimensionale Digitalkarte der Erdoberflaeche",
        correctAnswer = 1,
        explanation = "Das Geoid ist eine Aequipotentialflaeche des Erdschwerefeldes, die dem mittleren Meeresspiegel entspricht. Es ist weder eine Kugel noch ein Ellipsoid, sondern eine unregelmaeige Flaeche, die den Untergrund-Masseverteilungen folgt.",
        difficulty = 5,
        funFact = "Das Geoid weicht an manchen Stellen um bis zu 100 m vom Referenzellipsoid ab. Der tiefste Punkt liegt im Indischen Ozean suedlich von Sri Lanka (ca. -106 m), der hoechste in Nordeuropa (+85 m)."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Forscher mass erstmals den Erdumfang mit bemerkenswert hoher Praezision, rund 240 v.Chr. in Alexandria?",
        answerA = "Pythagoras",
        answerB = "Hipparchus",
        answerC = "Eratosthenes",
        answerD = "Archimedes",
        correctAnswer = 2,
        explanation = "Eratosthenes berechnete den Erdumfang, indem er den Sonnenwinkel an zwei Orten (Alexandria und Syene/Assuan) zum Sonnenhochstand mass und den Winkelunterschied mit der bekannten Entfernung kombinierte. Sein Ergebnis wich nur um wenige Prozent vom wahren Wert ab.",
        difficulty = 5,
        funFact = "Eratosthenes verwendete 'Stadien' als Masseinheit. Je nach Interpretation (attisches vs. aegyptisches Stadion) war sein Ergebnis entweder sehr genau oder leicht ueberschaetzt – aber die Methode war brillant."
    ),

    Question(
        categoryId = 1,
        questionText = "Was beschreibt die geodaetische Abstandsberechnung auf der Erdkugeloberflaeche mit dem Begriff 'Grosskreisdistanz'?",
        answerA = "Den laengsten moeglichen Weg zwischen zwei Punkten auf der Erdkugel",
        answerB = "Den kuerzesten Weg zwischen zwei Punkten entlang der Erdoberflaeche",
        answerC = "Die Entfernung gemessen durch das Erdinnere (Chord-Distanz)",
        answerD = "Den Abstand zwischen Breitengrad-Parallelen",
        correctAnswer = 1,
        explanation = "Die Grosskreisdistanz ist der kuerzeste Weg zwischen zwei Punkten auf einer Kugeloberflaeche. Grosskreise entstehen durch den Schnitt eines Kreises durch den Erdmittelpunkt. Flugzeuge folgen Grosskreisrouten, um Treibstoff zu sparen.",
        difficulty = 5,
        funFact = "Der kuerzeste Weg von Los Angeles nach Tokio fuehrt nahe am Nordpol vorbei – nicht westwaerts ueber den Pazifik, wie es auf einer flachen Mercator-Karte aussieht."
    ),

    // Klimaforschung
    Question(
        categoryId = 1,
        questionText = "Welche Einheit misst die Strahlungserwirmung einer Substanz im Vergleich zu CO2 ueber 100 Jahre (Global Warming Potential)?",
        answerA = "PPM (Parts per Million)",
        answerB = "CO2-Aequivalent (GWP-100)",
        answerC = "Watt pro Quadratmeter (W/m²)",
        answerD = "Dezibel (dB) als logarithmischer Massstab",
        correctAnswer = 1,
        explanation = "Das Global Warming Potential (GWP) gibt an, wie viel Waerme ein Treibhausgas im Vergleich zu CO2 ueber 100 Jahre einsperrt. Methan hat z.B. ein GWP von etwa 28 (28x so stark wie CO2), SF6 hat sogar ein GWP von ca. 23.500.",
        difficulty = 5,
        funFact = "Schwefelhexafluorid (SF6), das in Elektrogeraeten verwendet wird, ist mit Abstand das staerkste bekannte Treibhausgas mit einem GWP von 23.500 – und bleibt 3.200 Jahre in der Atmosphaere."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches palaeogeografische Zeitalter war durch den weltweit groessten Massenaussterbeereignis gekennzeichnet, bei dem ueber 90 % aller Meeresarten verschwanden?",
        answerA = "Kreide-Palogeen-Grenze (K-Pg, vor 66 Mio. Jahren)",
        answerB = "Perm-Trias-Grenze (vor 252 Mio. Jahren)",
        answerC = "Ordovizium-Silur-Grenze (vor 443 Mio. Jahren)",
        answerD = "Devon-Karbon-Grenze (vor 359 Mio. Jahren)",
        correctAnswer = 1,
        explanation = "Das Perm-Trias-Massenaussterben vor 252 Millionen Jahren war das groesste in der Erdgeschichte: 90-96 % aller Meeresarten und 70 % der Landwirbeltierarten starben aus. Es wurde vermutlich durch intensive vulkanische Aktivitaet (Sibirische Trapps) ausgeloest.",
        difficulty = 5,
        funFact = "Das Perm-Trias-Aussterben hat Paelontologen so schockiert, dass es als 'The Great Dying' bezeichnet wird. Die Erholung dauerte 10 Millionen Jahre – viel laenger als nach dem Dino-Aussterben."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche atmosphaerische Zirkulationszelle beschreibt den Luftmassenaustausch zwischen dem Aequator (Aufsteigen) und ca. 30° Breite (Absteigen)?",
        answerA = "Ferrel-Zelle",
        answerB = "Polarzelle",
        answerC = "Hadley-Zelle",
        answerD = "Walker-Zelle",
        correctAnswer = 2,
        explanation = "Die Hadley-Zelle ist eine atmosphaerische Zirkulationszelle in den Tropen: Warme, feuchte Luft steigt am Aequator auf, stroemt polwaerts in der Hoehenatmosphaere, sinkt bei ca. 30° Breite ab und stroemt als Passatwind zurueck. Sie erklaert subtropische Wuesten.",
        difficulty = 5,
        funFact = "Die Walker-Zelle ist eine aehnliche Zirkulationszelle, aber in Ost-West-Richtung ueber dem Pazifik. El Nino schwaecht sie – wenn das passiert, kann das globale Klimamuster durcheinandergeraten."
    ),

    Question(
        categoryId = 1,
        questionText = "Welcher Wissenschaftler entwickelte 1824 das Prinzip des atmosphaerischen Treibhauseffekts mathematisch?",
        answerA = "John Tyndall",
        answerB = "Joseph Fourier",
        answerC = "Svante Arrhenius",
        answerD = "Charles Keeling",
        correctAnswer = 1,
        explanation = "Joseph Fourier erkannte 1824, dass die Erde waermer sein muss als rein von der Sonneneinstrahlung erklaerbar – er postulierte einen atmosphaerischen Effekt aehnlich einem Glashaus. Tyndall mass 1859 die Treibhausgase, Arrhenius berechnete 1896 CO2-Erwaermung.",
        difficulty = 5,
        funFact = "Svante Arrhenius berechnete 1896, dass eine Verdopplung des CO2-Gehalts die globale Temperatur um 5-6°C erhoehen wuerde – erstaunlich nah am modernen IPCC-Wert von 2,5-4°C."
    ),

    Question(
        categoryId = 1,
        questionText = "Was beschreibt der 'Albedo-Rueckkopplungseffekt' beim Klimawandel?",
        answerA = "Mehr CO2 erwaermt die Erde, was mehr Wasserverdunstung verursacht",
        answerB = "Schmelzendes Eis verringert die Reflektivitaet, was mehr Erwaermung erzeugt",
        answerC = "Erwaermung verstaerkt den Golfstrom und verteilt Waerme global",
        answerD = "Hoehere Temperaturen foerdern Phytoplankton-Wachstum und CO2-Aufnahme",
        correctAnswer = 1,
        explanation = "Eis und Schnee reflektieren bis zu 90 % des Sonnenlichts (hohe Albedo). Wenn sie schmelzen, wird der dunkle Ozean oder Boden freigelegt, der nur 5-10 % reflektiert. Dies verstaerkt die Erwaermung in einer Rueckkopplungsschleife.",
        difficulty = 5,
        funFact = "Der Arktische Ozean erwaermt sich 4-mal schneller als der globale Durchschnitt – hauptsaechlich wegen des Albedo-Effekts. In den 1980ern war das arktische Meereis im Sommer noch doppelt so gross wie heute."
    ),

    Question(
        categoryId = 1,
        questionText = "Was ist die 'Milankovic-Theorie' und welche drei astronomischen Zyklen beschreibt sie?",
        answerA = "Theorie der Gezeitenkraefte: Mondneigung, Sonnenabstand, Erddrehung",
        answerB = "Theorie der Klimazyklen durch Erdbahnexzentrizitaet, Achsenneigung und Praezession",
        answerC = "Theorie der Plattentektonik durch Mantelstroeme, Gezeiten und Erdmagnetismus",
        answerD = "Theorie der Ozeanstroemungen: Tiefenwasser, Oberflaechenwind, Thermohalin",
        correctAnswer = 1,
        explanation = "Milutin Milankovic (1879-1958) beschrieb drei astronomische Zyklen, die das Klima der Erde beeinflussen: Exzentrizitaet der Erdbahn (~100.000 Jahre), Achsenneigung (41.000 Jahre) und Praezession der Erdachse (~26.000 Jahre). Sie erklaeren die Eiszeiten.",
        difficulty = 5,
        funFact = "Nach der Milankovic-Theorie befinden wir uns interglazial in einer relativ warmen Phase. Ohne den menschlichen CO2-Ausstoss wuerde die Erde in etwa 50.000 Jahren eine neue Eiszeit beginnen."
    ),

    Question(
        categoryId = 1,
        questionText = "Welches Instrument misst den atmosphaerischen CO2-Gehalt und wo wurde die erste kontinuierliche Langzeitmessung begonnen?",
        answerA = "Spektrometer; Mauna-Loa-Observatorium, Hawaii (1958)",
        answerB = "Gaschromatograph; Antarktis-Station Vostok (1959)",
        answerC = "Infrarot-Absorptionsmesser; Zugspitze, Deutschland (1970)",
        answerD = "Ozonsonde; World Meteorological Organization, Genf (1950)",
        correctAnswer = 0,
        explanation = "Charles Keeling begann 1958 auf dem Mauna-Loa-Vulkan auf Hawaii mit der kontinuierlichen CO2-Messung. Die daraus entstandene 'Keeling-Kurve' zeigt den stetigen Anstieg von 315 ppm (1958) auf heute ueber 420 ppm.",
        difficulty = 5,
        funFact = "Die Keeling-Kurve zeigt neben dem Langzeitanstieg auch jaehrliche Schwankungen: Im Nordsommerhalbjahr sinkt CO2, weil Pflanzen viel aufnehmen. Im Winter steigt es wieder – die Erde 'atmet' sichtbar."
    ),

    Question(
        categoryId = 1,
        questionText = "Was beschreibt der Begriff 'Permafrost' und in welchem Prozentsatz der Landoberflaeche kommt er vor?",
        answerA = "Dauerhaft gefrorener Boden; etwa 25 % der Landflaeche",
        answerB = "Saisonaler Frostboden; etwa 40 % der Landflaeche",
        answerC = "Glazialer Untergrund unter Gletschern; etwa 10 % der Landflaeche",
        answerD = "Tiefgefrorene Meeressedimente; etwa 15 % der Meeresflaeche",
        correctAnswer = 0,
        explanation = "Permafrost ist Boden, der mindestens zwei aufeinanderfolgende Jahre unter 0°C bleibt. Er bedeckt etwa 22-25 % der Landoberflaeche der Nordhemisphaere, hauptsaechlich in Sibirien, Alaska und Kanada.",
        difficulty = 5,
        funFact = "Im Permafrost sind etwa 1.700 Milliarden Tonnen Kohlenstoff gespeichert – doppelt so viel wie die gesamte aktuelle CO2-Menge in der Atmosphaere. Taut er, koennte er als Methan freigesetzt werden."
    ),

    Question(
        categoryId = 1,
        questionText = "Welche Klimaforschungs-Organisation veroeffentlicht regelmaessig die IPCC-Berichte (Sachstandsberichte zum Klimawandel)?",
        answerA = "NASA Goddard Institute for Space Studies",
        answerB = "Intergovernmental Panel on Climate Change (IPCC)",
        answerC = "World Meteorological Organization (WMO) allein",
        answerD = "United Nations Environment Programme (UNEP) allein",
        correctAnswer = 1,
        explanation = "Das IPCC (Zwischenstaatlicher Ausschuss fuer Klimaerfahrungen) wurde 1988 gemeinsam von WMO und UNEP gegruendet. Es bewertet wissenschaftliche Forschung zu Klimawandel und veroeffentlicht etwa alle 5-7 Jahre umfassende Sachstandsberichte.",
        difficulty = 5,
        funFact = "Das IPCC fuehrt keine eigene Forschung durch – es bewertet und synthetisiert Tausende von Fachartikeln aus der Klimaforschung. Seine Berichte werden von Regierungen zur Klimapolitik genutzt."
    ),

)
