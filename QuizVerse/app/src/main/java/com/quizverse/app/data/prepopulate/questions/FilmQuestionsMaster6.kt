package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun filmQuestionsMaster6(): List<Question> = listOf(

    // --- DOKUMENTAR-, EXPERIMENTAL- & KULTFILM ---

    // Question 1 - Robert Flahertys Nanook-Inszenierung
    Question(
        categoryId = 4,
        questionText = "Robert Flaherty inszenierte fuer 'Nanook of the North' (1922) bestimmte Szenen neu. Welche Waffe liess er Nanook beim Robbenjagen verwenden, obwohl die Inuit laengst Schusswaffen benutzten?",
        answerA = "Harpune",
        answerB = "Pfeil und Bogen",
        answerC = "Wurfholz",
        answerD = "Speer",
        correctAnswer = 0,
        explanation = "Flaherty liess Nanook (buergerlicher Name: Allakariallak) fuer die Kamera die traditionelle Harpune verwenden, obwohl die Inuit zu dieser Zeit laengst Schusswaffen kannten und nutzten. Dies ist eines der fruehesten Beispiele fuer inszenierte 'Realitaet' im Dokumentarfilm.",
        difficulty = 5,
        funFact = "Der echte Mann hinter 'Nanook', Allakariallak, starb 1922 — demselben Jahr, in dem der Film erschien — an Hunger waehrend eines Jagdausflugs. Flaherty selbst hatte eine uneheliche Beziehung mit einer der Frauen, die im Film als Nanuks Ehefrau dargestellt wurde."
    ),

    // Question 2 - John Griersons Schottische Heringsfischer
    Question(
        categoryId = 4,
        questionText = "John Grierson, der den Begriff 'Documentary' praegte, drehte 1929 seinen Debuetuebfilm 'Drifters'. Welches Sujet behandelte dieser GPO-Film, der als Gruendungsdokument des britischen Dokumentarfilms gilt?",
        answerA = "Schottische Heringsfischer auf der Nordsee",
        answerB = "Kohlenbergbau im Ruhrgebiet",
        answerC = "Postzustellung im Vereinigten Koenigreich",
        answerD = "Weberei in den Midlands",
        correctAnswer = 0,
        explanation = "'Drifters' (1929) zeigt schottische Heringsfischer bei ihrer Arbeit auf der Nordsee und wurde von der GPO (General Post Office) Film Unit produziert. Grierson definierte Dokumentarfilm als 'kreative Behandlung der Wirklichkeit' — eine Formel, die bis heute zitiert wird.",
        difficulty = 5,
        funFact = "Griersons 'Drifters' hatte am selben Abend seine Urauffuehrung wie der einzige britische Filmabend, bei dem Eisensteins 'Panzerkreuzer Potemkin' gezeigt wurde. Grierson hatte Eisenstein in Moskau besucht und war von ihm stark beeinflusst."
    ),

    // Question 3 - Frederick Wisemans laengster Film
    Question(
        categoryId = 4,
        questionText = "Frederick Wiseman, Meister des beobachtenden Direct Cinema, drehte 2017 seinen laengsten Film. Ueber wie viele Stunden erstreckt sich 'Ex Libris: The New York Public Library'?",
        answerA = "3 Stunden 17 Minuten",
        answerB = "3 Stunden 37 Minuten",
        answerC = "4 Stunden 7 Minuten",
        answerD = "4 Stunden 47 Minuten",
        correctAnswer = 2,
        explanation = "'Ex Libris: The New York Public Library' (2017) dauert 4 Stunden und 7 Minuten und ist damit einer von Wisemans laengsten Filmen. Wiseman ist fuer seine episch langen, kommentarlosen Beobachtungen von Institutionen bekannt — von Nervenheilanstalten bis zum Pentagon.",
        difficulty = 5,
        funFact = "Wiseman begann seine Dokumentarfilmkarriere 1967 mit 'Titicut Follies' ueber ein forensisch-psychiatrisches Krankenhaus in Massachusetts. Der Film wurde vom Bundesstaat Massachusetts jahrzehntelang verboten — bis 1991, als er fuer allgemeine Vorfuehrungen freigegeben wurde."
    ),

    // Question 4 - Shoah - Claude Lanzmanns Interviewstrategie
    Question(
        categoryId = 4,
        questionText = "Claude Lanzmanns 'Shoah' (1985) dauert ueber 9 Stunden. Welche konsequente Entscheidung traf Lanzmann bezueglich des Bildmaterials, die seinen Film von allen anderen Holocaust-Dokumentarfilmen grundlegend unterscheidet?",
        answerA = "Er verwendete keinerlei Archivmaterial oder historische Fotos",
        answerB = "Er interviewte ausschliesslich Taeternachkommen",
        answerC = "Er drehte nur in Israel",
        answerD = "Er verwendete ausschliesslich versteckte Kameras",
        correctAnswer = 0,
        explanation = "Lanzmann verzichtete vollstaendig auf historisches Archivmaterial, Fotos oder Rekonstruktionen. 'Shoah' besteht ausschliesslich aus Zeugeninterviews und Gegenwarts aufnahmen der Orte — eine radikale ethische Entscheidung: Das Grauen soll nur durch Sprache und heutige Stille bezeugt werden.",
        difficulty = 5,
        funFact = "Lanzmann drehte 350 Stunden Rohmaterial und arbeitete 11 Jahre an 'Shoah'. Einige Interviews wurden heimlich mit versteckter Kamera aufgezeichnet — insbesondere jene mit ehemaligen SS-Maennern wie Franz Suchomel, dem frueheren Unterscharfuehrer in Treblinka."
    ),

    // Question 5 - Hoop Dreams Schnitttechnik
    Question(
        categoryId = 4,
        questionText = "Steve James' 'Hoop Dreams' (1994) begleitete zwei Chicagoer Basketball-Talente ueber fuenf Jahre. Welche ungewoehnliche Erstausstrahlung machte den Film zur Sensation bei den Kritikern, obwohl er bei den Oscars fuer Bestes Dokumentarfilm ueberraschend NICHT nominiert wurde?",
        answerA = "Sundance-Sensation mit stehendem Applaus und spaeterer Oscar-Nichtberuecksichtigung",
        answerB = "Erstausstrahlung auf MTV statt im Kino",
        answerC = "Erstausstrahlung als dreiteilige TV-Serie",
        answerD = "Premiere auf dem Moskauer Filmfestival",
        correctAnswer = 0,
        explanation = "'Hoop Dreams' wurde zum gefeierten Kritikerliebling, gewann den Sundance-Dokumentarfilmpreis und gilt als einer der besten Dokumentarfilme aller Zeiten. Die fehlende Oscar-Nominierung wurde als skandaloeses Versagen der Akademie gewertet und fuehearte zu Reformdiskussionen beim Nominierungsverfahren.",
        difficulty = 5,
        funFact = "Roger Ebert bezeichnete 'Hoop Dreams' als den besten Film des Jahres 1994 — noch vor 'Pulp Fiction'. Der Film zeigt, wie das Bildungssystem und der Traum vom NBA-Erfolg arme schwarze Jugendliche in Chicago instrumentalisiert."
    ),

    // Question 6 - Dziga Vertov und der Kinoglaz
    Question(
        categoryId = 4,
        questionText = "Dziga Vertov entwickelte das Konzept des 'Kinoglaz' (Kino-Auge). In welchem Film von 1929 erreichte seine Montage-Theorie ihren kuenstlerischen Hoehepunkt mit dem beruehmt gewordenen Split-Screen und Zeitlupen-Sequenzen?",
        answerA = "Der Mann mit der Kamera",
        answerB = "Enthusiasmus",
        answerC = "Die elfte Stunde",
        answerD = "Lullaby",
        correctAnswer = 0,
        explanation = "'Der Mann mit der Kamera' (Chelovek s kino-apparatom, 1929) gilt als Vertovs Meisterwerk und als revolutionaeres Experiment in Filmsprache. Vertov zeigte den Kameramann bei der Arbeit und reflektierte damit erstmals systematisch den Akt des Filmemachens selbst.",
        difficulty = 5,
        funFact = "Vertov lehnte jedes geschriebene Drehbuch und jeden Schauspieler ab. Sein Bruder Mikhail Kaufman war der Kameramann von 'Der Mann mit der Kamera', und Vertovs Frau Elizaveta Svilova schnitt den Film — es war buchstaeblich ein Familienprojekt."
    ),

    // Question 7 - Cinema Verite vs Direct Cinema
    Question(
        categoryId = 4,
        questionText = "Cinema Verite und Direct Cinema gelten oft als Synonyme, unterscheiden sich aber fundamental. Welcher Aspekt trennt Jean Rouchs 'Cinema Verite' vom amerikanischen Direct Cinema von Leacock und Pennebaker?",
        answerA = "Cinema Verite provoziert aktiv Reaktionen durch Intervention, Direct Cinema beobachtet nur",
        answerB = "Direct Cinema verwendet Synchronton, Cinema Verite nicht",
        answerC = "Cinema Verite dreht ausschliesslich in Frankreich",
        answerD = "Direct Cinema verwendet keine tragbaren Kameras",
        correctAnswer = 0,
        explanation = "Jean Rouch entwickelte Cinema Verite als aktiv provokatives Verfahren: Der Filmemacher greift ein, stellt unbequeme Fragen und loest Reaktionen aus. Das amerikanische Direct Cinema (Leacock, Pennebaker, Maysles) dagegen versucht, unsichtbar zu bleiben und die Realitaet rein zu beobachten — 'fly on the wall'.",
        difficulty = 5,
        funFact = "Jean Rouchs 'Chronique d'un ete' (1961), gemeinsam mit Edgar Morin gedreht, gilt als das Gruendungsdokument des Cinema Verite. Die Pariser Passanten wurden gefragt: 'Sind Sie gluecklich?' — eine damals radikal direkte Befragung auf der Strasse."
    ),

    // Question 8 - Maya Deren und Meshes of the Afternoon
    Question(
        categoryId = 4,
        questionText = "Maya Deren drehte 1943 zusammen mit ihrem damaligen Ehemann 'Meshes of the Afternoon'. Welche Funktion hat der Spiegel, der das Gesicht der zentralen Figur ersetzt — eine der ikonischsten Einstellungen des amerikanischen Experimentalfilms?",
        answerA = "Er symbolisiert die Unmoeglichkeit der Selbsterkenntnis",
        answerB = "Er zeigt den Einfluss des surrealistischen Malers Dali",
        answerC = "Er diente nur als Kostuemstueck ohne symbolische Absicht",
        answerD = "Er wurde spaeter von Deren selbst als Fehler bezeichnet",
        correctAnswer = 0,
        explanation = "Der Spiegel als Gesicht symbolisiert bei Deren das psychoanalytische Thema der gespaltenen Identitaet und der Unmoeglichkeit echter Selbsterkenntnis. 'Meshes of the Afternoon' gilt als Referenzwerk fuer den psychodramatischen Experimentalfilm und beeinflusste Generationen von Kuenstlerinnen.",
        difficulty = 5,
        funFact = "Maya Deren finanzierte ihren Filmschaffen teilweise durch Vortragstourneen und gilt als Pionierin der Filmkuenstlerinnen-Selbstvermarktung. Sie erhielt 1946 als erste Filmemacherin ueberhaupt ein Guggenheim-Stipendium — fuer Filmarbeit, nicht fuer Malerei oder Literatur."
    ),

    // Question 9 - Stan Brakhage Dog Star Man
    Question(
        categoryId = 4,
        questionText = "Stan Brakhages episches Werk 'Dog Star Man' (1961-64) ist bekannt fuer direkt auf das Filmband gemalte oder geritzte Bilder. Wie nennt man diesen Typ von filmic work, der ohne Kamera entsteht?",
        answerA = "Cameraless Cinema",
        answerB = "Aural Film",
        answerC = "Concrete Film",
        answerD = "Expanded Cinema",
        correctAnswer = 0,
        explanation = "Brakhage arbeitete in 'Dog Star Man' und anderen Werken mit 'Cameraless Cinema' oder 'Handmade Film' — er scratched, malte, collaged und klebte direkt auf das Filmband, ohne jemals eine Kamera zu benutzen. 'Mothlight' (1963) entstand vollstaendig aus Mottenfluegelchen, die direkt auf Klebeband fixiert wurden.",
        difficulty = 5,
        funFact = "Brakhage drehte in seinem Leben ueber 350 Filme und war einer der produktivsten Experimentalfilmer der Geschichte. Er war zeitweise mit der Poetin Jane Collom verheiratet und betrachtete das Filmemachen als zutiefst persoenliche, autobiographische Kunst — vergleichbar mit Tagebuchschreiben."
    ),

    // Question 10 - Kenneth Anger Inauguration of the Pleasure Dome
    Question(
        categoryId = 4,
        questionText = "Kenneth Angers 'Inauguration of the Pleasure Dome' (1954) basiert auf den Schriften welches britischen Okkultisten, dessen System Anger sein ganzes filmisches Schaffen widmete?",
        answerA = "Aleister Crowley",
        answerB = "Arthur Edward Waite",
        answerC = "Israel Regardie",
        answerD = "Dion Fortune",
        correctAnswer = 0,
        explanation = "Kenneth Anger war zeit seines Lebens ein Adept des Thelema-Systems von Aleister Crowley. 'Inauguration of the Pleasure Dome' ist eine Hommage an Crowleys Rituale und seine 'Book of the Law'-Philosophie. Anger betrachtete seine Filme als magische Rituale, nicht als konventionelle Kunstwerke.",
        difficulty = 5,
        funFact = "Kenneth Anger ist auch als Autor des Hollywoodklatsch-Buchs 'Hollywood Babylon' bekannt — eines der skandaloesesten Bucher ueber die Filmindustrie, das mit Legenden und Halbwahrheiten ueber Stars wie Rudolph Valentino und Fatty Arbuckle aufwartete."
    ),

    // Question 11 - Michael Snow Wavelength
    Question(
        categoryId = 4,
        questionText = "Michael Snows strukturalistisches Meisterwerk 'Wavelength' (1967) besteht aus einem einzigen ununterbrochenen Zoom. Wie lange dauert dieser Zoom, und worauf fokussiert die Kamera am Ende?",
        answerA = "45 Minuten, auf ein Foto von Meereswellen an der Wand",
        answerB = "30 Minuten, auf ein Fenster zur Strasse",
        answerC = "60 Minuten, auf eine Gluehbirne",
        answerD = "20 Minuten, auf das Objektiv einer anderen Kamera",
        correctAnswer = 0,
        explanation = "'Wavelength' dauert 45 Minuten und zeigt einen langsamen, ununterbrochenen Zoom quer durch einen New Yorker Loft, der an einem Foto von Meereswellen endet. Der Film gilt als Schluesselwerk des Strukturalistischen Films und stellt radikale Fragen zur filmischen Wahrnehmung von Raum und Zeit.",
        difficulty = 5,
        funFact = "Michael Snow ist auch als bildender Kuenstler und Jazz-Musiker bekannt. Sein beruemtestes Kunstwerk ausserhalb des Films ist die Skulptur 'Flight Stop' (1979) im Eaton Centre in Toronto — 60 lebensgrosse Gaense aus Fiberglas, die an der Decke haengen."
    ),

    // Question 12 - Peter Kubelka und metrischer Film
    Question(
        categoryId = 4,
        questionText = "Peter Kubelka entwickelte in den 1950er und 60ern den 'metrischen Film'. Sein Werk 'Arnulf Rainer' (1960) besteht ausschliesslich aus welchen zwei Elementen?",
        answerA = "Abwechselnd weissem Blitzkader und schwarzem Kader, Stille und weissem Rauschen",
        answerB = "Rotem und blauem Licht mit Sinuston",
        answerC = "Handgeschriebenen Textkarten und Stille",
        answerD = "Einzelnen Standfotos im 24fps-Takt",
        correctAnswer = 0,
        explanation = "'Arnulf Rainer' reduziert Film auf seine elementarsten Bestandteile: weisse (belichtete) und schwarze (unbelichtete) Kader im visuellen Bereich, weisses Rauschen und Stille im auditiven Bereich. Die Muster wurden mathematisch berechnet. Es ist der radikalste Ausdruck des metrischen Films.",
        difficulty = 5,
        funFact = "Peter Kubelka lehrte jahrzehntelang an der Staedelschule Frankfurt und ist auch als Koch bekannt — er versteht Kochkunst als zeitbasierte Kunst, vergleichbar dem Film. Er organisierte 1989 das Oesterreichische Filmmuseum mit und gilt als wichtigste Figur der oesterreichischen Experimentalfilmszene."
    ),

    // Question 13 - Rocky Horror Picture Show Midnight Screenings
    Question(
        categoryId = 4,
        questionText = "'The Rocky Horror Picture Show' (1975) wurde nach seinem Kinoflop zum groessten Midnight-Movie-Phaenomen der Geschichte. In welchem New Yorker Kino begannen 1976 die ersten interaktiven Midnight Screenings mit Requisiten und Kostueemen?",
        answerA = "Waverly Theater in Greenwich Village",
        answerB = "Cinema Village in East Village",
        answerC = "Beacon Theatre in Upper West Side",
        answerD = "Angelika Film Center in SoHo",
        correctAnswer = 0,
        explanation = "Die erste regulaere Midnight Screening mit Publikumsinteraktion fand 1976 im Waverly Theater in Greenwich Village, New York statt. Fans begannen, auf die Leinwand zu rufen, mit Wasser zu spritzen und mit Toastbrot zu werfen — Traditionen, die bis heute fortbestehen.",
        difficulty = 5,
        funFact = "Rocky Horror laeuft ununterbrochen seit 1975 in Kinos weltweit und gilt damit als der am laengsten laufende Kinofilm der Geschichte. Der Film kostete 1,4 Millionen Dollar und hat weltweit ueber 200 Millionen Dollar eingespielt — fast ausschliesslich durch Mitternachtsvorstellungen."
    ),

    // Question 14 - Eraserhead - Produktionsbedingungen
    Question(
        categoryId = 4,
        questionText = "David Lynchs 'Eraserhead' (1977) wurde in einer langen Produktionsphase realisiert. Welche zwei Hauptfinanzierungsquellen ermoeglichten die jahrelange Produktion des Films?",
        answerA = "AFI-Stipendium und Lynchs Nebenjob als Zeitungsaustraeger",
        answerB = "Darlehen von Mel Brooks und Francis Ford Coppola",
        answerC = "Universal Pictures Entwicklungsfonds und privater Kredit",
        answerD = "Roger Cormans Produktionsfirma und ein Bankkredit",
        correctAnswer = 0,
        explanation = "Lynch finanzierte 'Eraserhead' hauptsaechlich durch sein AFI-Stipendium (American Film Institute) und seinen Nebenjob als Zeitungsaustraeger. Die Produktion dauerte von 1972 bis 1977 — rund fuenf Jahre. Mel Brooks sah den fertigen Film und engagierte Lynch danach fuer 'Der Elefantenmensch' (1980).",
        difficulty = 5,
        funFact = "Das sogenannte 'Baby' in 'Eraserhead' — eine der schauerlichsten Filmfiguren der Geschichte — wurde niemals oeffentlich erklaert. Lynch weigerte sich stets zu sagen, wie die Kreatur gebaut wurde oder was es ist. Das Geheimnis haelt bis heute an."
    ),

    // Question 15 - El Topo und Alejandro Jodorowsky
    Question(
        categoryId = 4,
        questionText = "Alejandro Jodorowskys 'El Topo' (1970) gilt als Urelement des Midnight Movie. Welcher beruehmt-beruechtigte Rock-Promoter kaufte die US-Rechte und machte den Film zum Kultereignis, indem er ihn in New York als Mitternachtsfilm etablierte?",
        answerA = "Allen Klein",
        answerB = "Bill Graham",
        answerC = "Lester Bangs",
        answerD = "Seymour Stein",
        correctAnswer = 0,
        explanation = "Allen Klein, der kontroverse Manager der Beatles und Rolling Stones, kaufte die US-Rechte an 'El Topo' und finanzierte Jodorowskys naechsten Film 'The Holy Mountain'. Durch Kleins Promotion wurde 'El Topo' im Elgin Theater in New York zum Kultfilm — John Lennon war ein bekennender Fan.",
        difficulty = 5,
        funFact = "John Lennon und Yoko Ono sahen 'El Topo' mehrmals in New York und ueberzeugten Allen Klein, in den Film zu investieren. Lennon beschrieb ihn als 'unglaublich' und 'ein Epos'. Jodorowsky selbst bezeichnete Kleins spaetere Geschaeftspraktiken als verraeterisch — die Beziehung endete im Streit."
    ),

    // Question 16 - Grindhouse und Exploitation-Terminologie
    Question(
        categoryId = 4,
        questionText = "Der Begriff 'Grindhouse' bezeichnete urspruenglich ein spezifisches Phaenomen des amerikanischen Kinowesens. Was war ein 'Grindhouse' in seiner urspruenglichen Bedeutung?",
        answerA = "Ein Kino mit kontinuierlichem Programm, das Burlesque-Shows und Billigfilme zeigte",
        answerB = "Ein illegales Kino im Keller ohne offizielle Lizenz",
        answerC = "Ein fahrbares Freiluftkino fuer Wanderschausteller",
        answerD = "Ein Filmverleih fuer B-Movie-Produktionen",
        correctAnswer = 0,
        explanation = "Der Begriff 'Grindhouse' stammt aus den 1920ern und bezeichnete Kinos (oft in Vergnuegungsvierteln), die rund um die Uhr liefen — sie 'mahlten' (grind) ihr Programm ununterbrochen durch. Anfangs zeigten sie Burlesque und Varietee, spaeter Exploitation-Filme, Horror und Sexfilme.",
        difficulty = 5,
        funFact = "Das beruehmt-beruechtigtste Grindhouse-Viertel war der 42nd Street Block in New York, bekannt als 'The Deuce'. In den 1970ern dominierten dort Sexfilme, Martial-Arts-Streifen und Horror-Exploitation. Die Saeauberung des Times Square in den 1990ern beendete die Aera."
    ),

    // Question 17 - Giallo - Dario Argento
    Question(
        categoryId = 4,
        questionText = "Dario Argentos 'Suspiria' (1977) ist technisch kein klassischer Giallo, gilt aber als sein Meisterwerk. Welches ungewoehnliche Beleuchtungssystem verwendete Kameramann Luciano Tovoli, das dem Film seine ikonischen Farben gab?",
        answerA = "Technicolor-Dye-Transfer-Verfahren, das bereits in den 1970ern obsolet war",
        answerB = "Spezielle LED-Prototypen aus Mailaender Theatern",
        answerC = "Infrarot-Filter auf konventionellen Halogenstrahlern",
        answerD = "Gefaerbte Gelatine-Filter auf umgebauten 35mm-Projektoren",
        correctAnswer = 0,
        explanation = "Tovoli und Argento verwendeten den fast ausgestorbenen Technicolor-Dye-Transfer-Prozess, der extreme Saettigung und eine spezielle Farbpalette erzeugte. Das Verfahren war 1977 technisch ueberholt, aber Argento bestand darauf — das Ergebnis ist die ikonische Farbgewalt des Films.",
        difficulty = 5,
        funFact = "Argento schrieb 'Suspiria' urspruenglich fuer Kinderschauspieler — der Film sollte mit 10-Jaehrigen besetzt werden. Als die Finanzierung nur mit erwachsenen Darstellern moeglich wurde, behielt er das Drehbuch nahezu unveraendert bei, was erklaert, warum die Charaktere manchmal kindlich-naiv reagieren."
    ),

    // Question 18 - Nunsploitation
    Question(
        categoryId = 4,
        questionText = "Das Nunsploitation-Subgenre erlebte seinen Hoehepunkt in den 1970er Jahren. Ken Russells Film 'The Devils' (1971) basiert auf einem historischen Fall. Welches reale Ereignis liegt dem Film zugrunde?",
        answerA = "Die Besessenheitsfaelle von Loudun im Frankreich des 17. Jahrhunderts",
        answerB = "Der Hexenprozess von Bamberg im 16. Jahrhundert",
        answerC = "Die Inquisition von Goa im 15. Jahrhundert",
        answerD = "Die Nonnenvorfaelle von Louviers im 18. Jahrhundert",
        correctAnswer = 0,
        explanation = "'The Devils' basiert auf den historisch dokumentierten Faellen von Loudun (1634), in denen Nonnen angeblich von Daemonen besessen waren. Beschuldigt wurde der Priester Urbain Grandier, der spaeter verbrannt wurde. Russell basierte seinen Film auf Aldous Huxleys Buch 'The Devils of Loudun'.",
        difficulty = 5,
        funFact = "'The Devils' wurde in Grossbritannien von der BBFC so stark geschnitten, dass die vollstaendige Version jahrzehntelang nicht zu sehen war. Der beruehmt-beruechtigte 'Rape of Christ'-Sequenz wurde erst 2012 der Oeffentlichkeit zugaenglich gemacht — 41 Jahre nach Fertigstellung des Films."
    ),

    // Question 19 - John Waters und Pink Flamingos
    Question(
        categoryId = 4,
        questionText = "John Waters' 'Pink Flamingos' (1972) endet mit einer der kontroversesten Szenen der Filmgeschichte: Divine isst echten Hundekot. Welcher Begriff beschreibt die Aesthetik, die Waters bewusst anstrebt und die Susan Sontag 1964 erstmals theoretisierte?",
        answerA = "Camp",
        answerB = "Kitsch",
        answerC = "Trash",
        answerD = "Schlock",
        correctAnswer = 0,
        explanation = "Susan Sontags Essay 'Notes on Camp' (1964) definierte Camp als eine Aesthetik des Uebertriebenen, Theatralischen und ironisch Schlechten. Waters zitiert Camp bewusst und macht daraus sein kuenstlerisches Prinzip — er sieht 'schlechten Geschmack' als subversiven Widerstand gegen buerglerliche Normen.",
        difficulty = 5,
        funFact = "Divine — buergerlicher Name Harris Glenn Milstead — war einer der engsten Freunde von Waters seit der Schulzeit in Baltimore. Divine starb 1988 nur wenige Wochen nachdem er seine erste Hauptrolle in einem Mainstream-Film ('Hairspray') gespielt hatte — an Herzversagen in einem Hotelzimmer in Los Angeles."
    ),

    // Question 20 - Ed Wood und Plan 9 from Outer Space
    Question(
        categoryId = 4,
        questionText = "Ed Woods 'Plan 9 from Outer Space' (1957) gilt als 'schlechtester Film aller Zeiten', wurde aber zum Kultfilm. Welcher zutiefst persoenliche Umstand macht diesen Film zur einzigen Zusammenarbeit von Ed Wood und Bela Lugosi?",
        answerA = "Lugosi starb waehrend der Produktion, sein Doppelgaenger spielte den Rest",
        answerB = "Lugosi war so krank, dass er nur per Telefon Regie-Anweisungen erhalten konnte",
        answerC = "Lugosi weigerte sich zu spielen, Wood nutzte heimlich Archivmaterial",
        answerD = "Lugosi spielte seine Rolle nur, weil Wood ihm Geld schuldete",
        correctAnswer = 0,
        explanation = "Bela Lugosi starb 1956, bevor 'Plan 9' fertiggestellt war. Wood hatte nur wenige Sekunden Testmaterial von Lugosi gedreht. Er engagierte den Schwiegervater seines Chiropraktikers — einen deutlich groesseren Mann — als Doppelgaenger, der sein Gesicht hinter einem Umhang versteckt hielt.",
        difficulty = 5,
        funFact = "Ed Wood und Bela Lugosi pflegten eine tiefe Freundschaft in Lugosis letzten Jahren. Wood half Lugosi 1955, sich in eine Entzugsklinik einzuchecken, als dieser oeffentlich seine Morphiumabhaengigkeit eingestand. Tim Burton ehrte diese Freundschaft in seinem Film 'Ed Wood' (1994) mit Johnny Depp und Martin Landau."
    ),

    // Question 21 - Found Footage - The Blair Witch Project
    Question(
        categoryId = 4,
        questionText = "Das Found-Footage-Konzept in 'The Blair Witch Project' (1999) war nicht das erste seiner Art. Welcher italenische Horrorfilm von 1980 gilt als erster Langspielfilm, der das Found-Footage-Konzept konsequent anwendete?",
        answerA = "Cannibal Holocaust",
        answerB = "Inferno",
        answerC = "Zombie 2",
        answerD = "The Beyond",
        correctAnswer = 0,
        explanation = "'Cannibal Holocaust' (1980) von Ruggero Deodato enthielt 'gefundenes Filmmaterial' von vermissten Dokumentarfilmern im Amazonas — ein direkter Vorlaeufer des modernen Found-Footage-Genres. Deodato musste vor Gericht beweisen, dass seine Darsteller noch am Leben waren, da viele glaubten, es sei echtes Snuff-Material.",
        difficulty = 5,
        funFact = "Ruggero Deodato liess seine Darsteller Vertraege unterschreiben, dass sie ein Jahr lang oeffentlich nicht auftreten duerften — um die Illusion aufrechtzuerhalten, sie seien tot. Als die italenischen Behoerden ihn wegen Mordes anklagten, musste er die Schauspieler ins Fernsehen bringen, um seine Unschuld zu beweisen."
    ),

    // Question 22 - Mockumentary - This Is Spinal Tap
    Question(
        categoryId = 4,
        questionText = "Rob Reiners 'This Is Spinal Tap' (1984) gilt als Beggruender des Mockumentary-Genres. Welche ungewoehnliche Improvisationstechnik verwendete Reiner, die das Budget sprengte und gleichzeitig den Film retten sollte?",
        answerA = "Fast alle Dialoge wurden vollstaendig ohne Drehbuch improvisiert",
        answerB = "Der Film wurde in einem einzigen 30-taegigen Take gedreht",
        answerC = "Die Bandmitglieder sprachen nie miteinander ausserhalb der Kamera",
        answerD = "Reiner spielte alle Interviewten ausser der Band selbst",
        correctAnswer = 0,
        explanation = "Christopher Guest, Michael McKean und Harry Shearer improvisierten nahezu alle Dialoge. Dies produzierte enorme Mengen Rohmaterial — der Rohschnitt soll ueber 4 Stunden betragen haben. Der Film kostet dadurch mehr als geplant, gewann aber eine authentische Qualitaet, die kein Skript haette erzeugen koennen.",
        difficulty = 5,
        funFact = "Die Szene, in der Bassist Derek Smalls (Harry Shearer) am Flughafen-Metalldetektor haengenbleibt, weil er eine in Folie eingewickelte Zucchini in seiner Hose versteckt hat, war vollstaendig improvisiert. Reiner wusste nicht, was Shearer plante — die echte Ueberraschungsreaktion des Sicherheitspersonals ist im Film zu sehen."
    ),

    // Question 23 - Fahrenheit 9/11 und Michael Moore
    Question(
        categoryId = 4,
        questionText = "Michael Moores 'Fahrenheit 9/11' (2004) ist der kommerziell erfolgreichste Dokumentarfilm aller Zeiten. Welchen Rekord stellte er bei seinem Kinostart auf, der in der Geschichte des Dokumentarfilms einzigartig ist?",
        answerA = "Er spielte am Eroeffnungswochenende mehr ein als alle anderen Dokumentarfilme zusammen je eingespielt hatten",
        answerB = "Er war der erste Dokumentarfilm, der Platz 1 der US-Kinocharts erreichte",
        answerC = "Er war der erste Dokumentarfilm mit Pflichtquote in US-Schulen",
        answerD = "Er gewann als erster Dokumentarfilm die Goldene Palme in Cannes",
        correctAnswer = 3,
        explanation = "'Fahrenheit 9/11' gewann 2004 als erster Dokumentarfilm seit 46 Jahren die Goldene Palme in Cannes (der letzte war 'Le Monde du Silence' von Cousteau/Malle 1956). Ausserdem war er tatsaechlich der erste Dokumentarfilm der Platz 1 der US-Kinocharts erreichte und spielte weltweit 222 Millionen Dollar ein.",
        difficulty = 5,
        funFact = "Miramax (im Besitz von Disney) weigerte sich urspruenglich, den Film zu veroeffentlichen, aus Angst vor politischen Konsequenzen. Harvey Weinstein kaufte die Rechte persoenlich und gruendete mit seinem Bruder eine eigene Distributionsfirma fuer den Film. Das Timing — kurz vor der US-Wahl 2004 — war bewusst kalkuliert."
    ),

    // Question 24 - Videokunst - Nam June Paik
    Question(
        categoryId = 4,
        questionText = "Nam June Paik gilt als 'Vater der Videokunst'. Seine erste oeffentliche Videokunst-Vorfuehrung 1965 in New York nutzte eine neu erschienene Kamera. Was zeigte er dabei, das die Kunstwelt schockierte?",
        answerA = "Aufnahmen vom Paustbesuch des Papstes, am selben Tag auf einem Monitor in einer Galerie gezeigt",
        answerB = "Live-Uebertragung seiner eigenen Performance nackt auf dem Times Square",
        answerC = "Endlosschleife eines explodierenden Fernsehers",
        answerD = "Synchronized-Feed von 40 verschiedenen Kamerastandorten gleichzeitig",
        correctAnswer = 0,
        explanation = "Paik kaufte sich eine der ersten tragbaren Videokameras (Sony Portapak) und filmte am 4. Oktober 1965 den Besuch von Papst Paul VI. in New York. Er zeigte das Material noch am selben Abend in der Cafe au Go Go-Galerie — das gilt als Geburtsstunde der Videokunst als eigenstaendige Form.",
        difficulty = 5,
        funFact = "Nam June Paik arbeitete eng mit Charlotte Moorman zusammen, einer Cellovirtuosin, die er als seine 'Koerperin' bezeichnete. Bei einer Performance 1967 spielte Moorman Cello mit einem eingeschalteten Fernseher als BH — sie wurde wegen Unzucht verhaftet. Der Fall wurde zum internationalen Kunstskandal."
    ),

    // Question 25 - Jonas Mekas und Strukturalistischer Film
    Question(
        categoryId = 4,
        questionText = "Jonas Mekas gilt als 'Vater des amerikanischen Avantgardefilms' und gruendete 1962 eine einflussreiche Institution in New York. Wie heisst diese Institution, die bis heute das Archiv des amerikanischen Experimentalfilms verwaltet?",
        answerA = "Anthology Film Archives",
        answerB = "Cinema 16",
        answerC = "Film-Makers' Cooperative",
        answerD = "The Millennium Film Workshop",
        correctAnswer = 0,
        explanation = "Mekas gruendete 1970 die Anthology Film Archives in New York, die als wichtigstes Archiv und Auffuehrungsort fuer Avantgarde- und Experimentalfilm weltweit gilt. Zuvor hatte er 1962 die Film-Makers' Cooperative gegrundedet — einen nicht-kommerziellen Filmverleih fuer Avantgardefilme.",
        difficulty = 5,
        funFact = "Jonas Mekas floh 1949 als litauischer Kriegsfluechtling nach New York und filmte ab diesem Moment fast jeden Tag seines Lebens auf 16mm. Diese jahrzehntelangen Tagebucher wurden zu seinem Hauptwerk — er bezeichnete sein Schaffen als 'home movies' und 'Film as a Diary'."
    ),

    // Question 26 - Werner Herzog und Fitzcarraldo
    Question(
        categoryId = 4,
        questionText = "Fuer Werner Herzogs 'Fitzcarraldo' (1982) wurde ein echtes Dampfschiff ueber einen echten Berg gezogen. Welchen anderen Regisseur verdraengte Herzog von diesem Projekt, nachdem er den Drehbeginn und die Vision des Films uebernahm?",
        answerA = "Les Blank",
        answerB = "Jason Robards",
        answerC = "Werner Schroeter",
        answerD = "Volker Schloendorff",
        correctAnswer = 1,
        explanation = "Die urspruengliche Besetzung der Titelrolle war Jason Robards (zusammen mit Mick Jagger als Sidekick). Nach etwa 40% der Dreharbeiten musste Robards wegen einer Erkrankung das Projekt verlassen. Herzog besetzte die Rolle mit Klaus Kinski um, und Mick Jagger verliess das Projekt wegen seiner Tourverpflichtungen.",
        difficulty = 5,
        funFact = "Der Dokumentarfilm 'Burden of Dreams' (1982) von Les Blank zeigt die katastrophalen Drehbedingungen des Fitzcarraldo-Sets — Konflikte mit indigenen Voelkern, Malaria, den Wahnsinn Kinskis und Herzogs unbedingten Willen. Herzog nannte ihn den einzigen Film, der wirklich zeigt, was beim Filmemachen passiert."
    ),

    // Question 27 - Orson Welles F for Fake
    Question(
        categoryId = 4,
        questionText = "Orson Welles' 'F for Fake' (1973) gilt als Essayfilm-Meisterwerk ueber Faelschung und Authentizitaet. Welche beiden realen Persoenlichkeiten stehen im Zentrum des Films, den Welles selbst als seinen besten bezeichnete?",
        answerA = "Elmiyr de Hory (Kunstfaelscher) und Clifford Irving (Faelscher der Howard-Hughes-Biographie)",
        answerB = "Han van Meegeren (Vermeer-Faelscher) und John Drewe (Kunst-Faelscher)",
        answerC = "Mark Landis (Kunstfaelscher) und Frank Abagnale (Hochstapler)",
        answerD = "Wolfgang Beltracchi (Kunstfaelscher) und Konrad Kujau (Hitler-Tagebuecher)",
        correctAnswer = 0,
        explanation = "Elmyr de Hory, der groesste Kunstfaelscher des 20. Jahrhunderts, und Clifford Irving, der eine gefaelschte Autobiographie Howard Hughes' schrieb (und de Horys Biographie — die selbst Teile erfand), stehen im Zentrum. Welles verwebt ihre Geschichten mit Reflexionen ueber die Natur von Kunst und Betrug.",
        difficulty = 5,
        funFact = "Welles selbst war bekannt fuer seinen 'War of the Worlds'-Radiobetrug (1938), mit dem er angeblich Amerika in Panik versetzte. In 'F for Fake' macht er seine eigene Geschichte der Taeuschung zum Teil des Films — er loest damit die Grenze zwischen Dokumentar und Fiktion bewusst auf."
    ),

    // Question 28 - Ruggero Deodato und Cannibal Holocaust
    Question(
        categoryId = 4,
        questionText = "In 'Cannibal Holocaust' (1980) wurden sechs echte Tiere getoetet und die Aufnahmen in den Film integriert. In welchem Land ist der Film aufgrund dieser Tiertoetungsszenen bis heute vollstaendig verboten?",
        answerA = "Australien",
        answerB = "Deutschland",
        answerC = "Frankreich",
        answerD = "Japan",
        correctAnswer = 0,
        explanation = "In Australien ist 'Cannibal Holocaust' aufgrund der realen Tiertoetungen bis heute vollstaendig verboten. Zahlreiche andere Laender haben den Film ebenfalls verboten oder stark zensiert, darunter das Vereinigte Koenigreich, wo er als 'Video Nasty' eingestuft wurde.",
        difficulty = 5,
        funFact = "Die Turtle-Szene in 'Cannibal Holocaust', in der eine Schildkroete lebendig zerstueckelt wird, gilt als eine der umstrittensten Filmszenen aller Zeiten. Regisseur Deodato erklaerte spaeter, er bereue diese Entscheidung zutiefst — die Tiere seien fuer kulturell akzeptable lokale Zwecke getoetet worden, aber die Filmaufnahmen haetten nicht sein sollen."
    ),

    // Question 29 - Jean-Luc Godard und Essay-Film
    Question(
        categoryId = 4,
        questionText = "Jean-Luc Godards 'Histoire(s) du cinema' (1988-1998) ist ein monumentales Essayfilm-Werk. Wie viele Kapitel hat es, und auf welchem Medium wurde es urspruenglich produziert?",
        answerA = "8 Kapitel, auf VHS/Betamax produziert und spaeter auf Film uebertragen",
        answerB = "4 Kapitel, direkt auf 35mm produziert",
        answerC = "12 Kapitel, auf U-matic-Video produziert",
        answerD = "6 Kapitel, auf Super-8 produziert",
        correctAnswer = 0,
        explanation = "'Histoire(s) du cinema' besteht aus 8 Episoden und wurde von Godard bewusst auf Video produziert — er nutzte die elektronischen Bildmischmoeglichkeiten des Videoschnitts als kuenstlerisches Mittel, das er mit analogem Film nicht haette realisieren koennen. Die Uebertragung auf 35mm erfolgte fuer Kinopraesentation.",
        difficulty = 5,
        funFact = "Godard war ein Pionier der Videotechnik als Kunstform. Er gruendete in den 1970ern zusammen mit Jean-Pierre Gorin die 'Groupe Dziga Vertov' und experimentierte intensiv mit Video als Produktionsmittel. 'Histoire(s) du cinema' gilt als sein persoenlichstes und philosophisch tiefstes Werk."
    ),

    // Question 30 - Lucio Fulci und Splatter
    Question(
        categoryId = 4,
        questionText = "Lucio Fulci wird als 'Godfather of Gore' bezeichnet. Sein Film 'Zombi 2' (1979) war urspruenglich als was konzipiert worden, bevor er zum eigenstaendigen Werk wurde?",
        answerA = "Als inoffizieller italenischer Sequel zu George Romeros 'Dawn of the Dead'",
        answerB = "Als Remake von Jacques Tourneurs 'I Walked with a Zombie'",
        answerC = "Als Portfilm zu 'Suspiria' von Dario Argento",
        answerD = "Als Fortsetzung von Mario Bavas 'Lisa und der Teufel'",
        correctAnswer = 0,
        explanation = "Italenische Verleiher titelten George Romeros 'Dawn of the Dead' (1978) als 'Zombi'. Fulcis Film wurde daraufhin als 'Zombi 2' vermarktet, um als Fortsetzung zu erscheinen — obwohl er keinerlei inhaltlichen Zusammenhang mit Romeros Werk hat. Fulci schuf damit versehentlich sein bekanntestes Werk.",
        difficulty = 5,
        funFact = "Die beruehmt-beruechtigte Szene in 'Zombi 2', in der ein Zombie unter Wasser mit einem echten Hai kaempft, wurde tatsaechlich mit einem echten, betaeubten Tigerhai gedreht. Der Stuntman trug einen Zombie-Schutzanzug und trainierte Wochen fuer diese einzigartige Unterwassersequenz."
    ),

    // Question 31 - Chris Markers Sans Soleil
    Question(
        categoryId = 4,
        questionText = "Chris Markers Essayfilm 'Sans Soleil' (1983) beginnt mit einer ikonischen Einstellung von drei Kindern auf einem islaendischen Weg. Was sagt der Off-Kommentar ueber diese Einstellung, die den Grundton des gesamten Films setzt?",
        answerA = "Er erinnere sich nicht an dieses Bild, er haette es erfunden",
        answerB = "Er sei der gluecklichste Moment seines Lebens gewesen",
        answerC = "Das Bild passe nicht neben andere Bilder und warte noch immer",
        answerD = "Es sei das einzige Bild, das er je wirklich gesehen habe",
        correctAnswer = 2,
        explanation = "Marker beschreibt, wie das Bild der drei islaendischen Kinder 'nicht neben andere Bilder passt' und 'noch immer wartet'. Diese Reflexion ueber die Unvereinbarkeit von Bildern ist paradigmatisch fuer Markers gesamtes Werk ueber Gedaechtnis, Zeit und die Unmoelichkeit, Erfahrungen durch Bilder festzuhalten.",
        difficulty = 5,
        funFact = "Chris Marker ist einer der geheimnisvollsten Filmemacher der Geschichte. Er mied oeffentliche Auftritte konsequent und erlaubte nur selten Fotos von sich. Sein buergerlicher Name war Christian Francois Bouche-Villeneuve — 'Chris Marker' war ein Pseudonym, das er nie erklaerte. Er starb 2012 an seinem 91. Geburtstag."
    ),

    // Question 32 - Leni Riefenstahl und Propagandadokumentarfilm
    Question(
        categoryId = 4,
        questionText = "Leni Riefenstahls 'Triumph des Willens' (1935) ist filmtechnisch hochkaraetisch, ideologisch jedoch NS-Propaganda. Welche filmtechnische Innovation, die heute Standard ist, wurde bei diesem Film erstmalig systematisch eingesetzt?",
        answerA = "Kranfahrten und spezielle Kameraschienen fuer dynamische Bewegungsaufnahmen",
        answerB = "Synchronton-Aufnahmen bei Aussenszenen",
        answerC = "Einsatz von mehr als 10 Kameras gleichzeitig fuer einen einzelnen Take",
        answerD = "Verwendung von Teleobiektiven aus Entfernungen ueber 500 Meter",
        correctAnswer = 0,
        explanation = "Riefenstahl setzte erstmals systematisch Kranfahrten, ausgelegte Kameraschienen und komplexe Kamerafahrten ein, die damals revolutionaer waren. Fuer den Parteitag wurden eigens Schienen und Aufzugsvorrichtungen installiert. Diese Techniken sind heute selbstverstaendlicher Standard in der Filmproduktion.",
        difficulty = 5,
        funFact = "Riefenstahl hatte die volle kreative Kontrolle ueber 'Triumph des Willens' und bestand darauf, dass es sich um Kunst, nicht um Propaganda handle. Sie arbeitete mit 170 Mitarbeitern und 30 Kameras fuer den Nurnberger Parteitag 1934. Nach 1945 leugnete sie ihr Wissen um NS-Verbrechen — eine Haltung, die bis zu ihrem Tod 2003 umstritten blieb."
    ),

    // Question 33 - Expanded Cinema
    Question(
        categoryId = 4,
        questionText = "Gene Youngbloods Buch 'Expanded Cinema' (1970) praegte den Begriff fuer experimentelle Filminstallationen. Welcher Kuenstler entwickelte in den 1960ern 'happenings' die Film mit Live-Performance kombinierten und spaeter von Andy Warhol beeinflusst wurden?",
        answerA = "Allan Kaprow",
        answerB = "Carolee Schneemann",
        answerC = "Yoko Ono",
        answerD = "Robert Whitman",
        correctAnswer = 0,
        explanation = "Allan Kaprow pragte 1958 den Begriff 'Happening' und entwickelte Aufführungen, in denen Film, Performance und Publikumsbeteiligung verschmelzen. Youngbloods 'Expanded Cinema' baute auf diesen Ideen auf und erweiterte sie auf Holographie, Computergrafik und Satellitenuebertragungen als neue Medien.",
        difficulty = 5,
        funFact = "Allan Kaprow war Schueler von John Cage, dessen Zufallskomposition-Prinzipien er auf visuelle Kunst uebertrug. Kaprow bestand darauf, dass ein Happening nie zweimal identisch stattfinden darf — im Gegensatz zum Theater gibt es keine Wiederholung, keine Erinnerung und keine Dokumentation, die als 'das eigentliche Werk' gilt."
    ),

    // Question 34 - Todd Phillips und Dokumentarfilm-Hintergrund
    Question(
        categoryId = 4,
        questionText = "Bevor Todd Phillips ('Hangover', 'Joker') Hollywoodregisseur wurde, drehte er zwei Dokumentarfilme. Sein Debut 'Hated: GG Allin and the Murder Junkies' (1993) zeigt die letzte Tournee von GG Allin. Warum konnte GG Allin sein oft angekuendigtes Versprechen nicht halten?",
        answerA = "Er starb am 28. Juni 1993 an einer Heroin-Ueberdosis kurz nach Filmfertigstellung",
        answerB = "Er wurde wegen Koerperverletzung zu 5 Jahren Gefaengnis verurteilt",
        answerC = "Er floh nach Mexiko und trat nie wieder auf",
        answerD = "Er bekehrte sich zum Christentum und beendete seine Karriere",
        correctAnswer = 0,
        explanation = "GG Allin hatte immer wieder angekuendigt, sich live auf der Buehne zu toeten. Er starb am 28. Juni 1993 an einer Heroin-Ueberdosis nach einem Konzert in New York — nur wenige Wochen nach Fertigstellung des Dokumentarfilms. Phillips hatte somit die letzte filmische Dokumentation von Allins Karriere.",
        difficulty = 5,
        funFact = "GG Allins Konzerte waren bekannt fuer exzessive Selbstverletzung und den Einsatz von Koerperauscheidungen als Buehnenmittel. Seine Beerdigung 1993 war selbst ein Art Performance — Hunderte Fans erschienen, viele betrunken oder unter Drogeneinfluss. Todd Phillips drehte spaeter auch 'Frat House' (1998), der Misshandlungen in US-Studentenverbindungen zeigt."
    ),

    // Question 35 - Mondo-Film
    Question(
        categoryId = 4,
        questionText = "Das Mondo-Film-Genre begann mit 'Mondo Cane' (1962) von Gualtiero Jacopetti. Welcher Aspekt dieses Films war fuer das Publkum besonders schockierend und definierte das Genre?",
        answerA = "Die Behauptung, echte, nicht inszenierte Bilder aus aller Welt zu zeigen — viele davon gestellt",
        answerB = "Erstmals explizite Gewalt gegen Tiere ohne Warnung",
        answerC = "Der erste Farbdokumentarfilm mit Synchronton weltweit",
        answerD = "Die vollstaendig anonymen Filmemacher ohne Credits",
        correctAnswer = 0,
        explanation = "Mondo Cane behauptete, authentische, schockierende Rituale und Lebensweisen aus aller Welt zu dokumentieren. Viele Szenen waren jedoch inszeniert. Diese Mischung aus echter und gestellter 'Realitaet' — verpackt als Schockdokumentation — definierte das Mondo-Genre und seine ethisch fragwuerdige Machart.",
        difficulty = 5,
        funFact = "Der Titelsong von 'Mondo Cane', 'More' von Riz Ortolani, wurde fuer einen Oscar nominiert und war ein weltweiter Schlagerhit — ein absurder Kontrast zum verstoerendem Inhalt des Films. Das Lied wurde spaeter von Frank Sinatra und Andy Williams aufgenommen und wurde ein Jazz-Standard."
    ),

    // Question 36 - Frederick Wisemans Titicut Follies
    Question(
        categoryId = 4,
        questionText = "Frederick Wisemans 'Titicut Follies' (1967) ueber das Bridgewater State Hospital wurde vom Staat Massachusetts verboten. Auf welcher juristischen Grundlage wurde dieses Verbot 1967 zunaechst durchgesetzt?",
        answerA = "Verletzung der Privatsphaere der Patienten ohne deren Einwilligung",
        answerB = "Staatsgeheimnisgesetz wegen der Sicherheitsanlage",
        answerC = "Verleumdung des Klinikpersonals",
        answerD = "Copyright-Verletzung an institutionellen Dokumenten",
        correctAnswer = 0,
        explanation = "Das Gericht untersagte den Film mit der Begruendung, die Wuerde der Patienten sei verletzt worden, da diese nicht ausdruecklich in die Aufnahmen eingewilligt hatten. Ironischerweise argumentierte der Staat Massachusetts gleichzeitig, der Film zeige die Klinik zu negativ — ein Widerspruch, der auf politische Motive hindeutete.",
        difficulty = 5,
        funFact = "Das Massachusetts-Verbot von 'Titicut Follies' war das erste Mal in US-Geschichte, dass ein Dokumentarfilm aus nicht-obszoenitaetsbezogenen Gruenden verboten wurde. Erst 1991 durfte der Film vollstaendig oeffentlich gezeigt werden — ein 24-jaehriges Verbot, das Wisemans Karriere paradoxerweise befoerderte."
    ),

    // Question 37 - Pier Paolo Pasolini und Salo
    Question(
        categoryId = 4,
        questionText = "Pier Paolo Pasolinis letzter Film 'Salo, oder die 120 Tage von Sodom' (1975) basiert auf einem Roman. Pasolini wurde kurz vor seiner Urauffuehrung ermordet. Welche Theorie existiert bezueglich des Zusammenhangs zwischen dem Film und seinem Tod?",
        answerA = "Einige glauben, der Film enthuelle reale kriminelle Netzwerke, was zum Motiv fuer seinen Mord geworden sei",
        answerB = "Er wurde von einem Darsteller des Films getoetet, der sich schaemte",
        answerC = "Die Kirche soll den Mord in Auftrag gegeben haben",
        answerD = "Der Mord sei ein inszenierter Teil des Films gewesen",
        correctAnswer = 0,
        explanation = "Einige Biographen und Ermittler spekulierten, 'Salo' habe reale kriminelle Eliten Italiens entbloesst und koenne damit zum Motiv fuer seinen Mord am 2. November 1975 beigetragen haben. Die offizielle Verurteilung eines Jugendlichen wurde spaeter von diesem widerrufen — der Fall blieb ungeklaert.",
        difficulty = 5,
        funFact = "Pasolinis Salo wurde weltweit verboten und ist bis heute in vielen Laendern auf Index. Der Film adaptiert de Sades Roman und versetzt ihn ins faschistische Italien 1944. Pasolini bezeichnete ihn als Allegorie auf den Konsumkapitalismus — das 'absolute Boese' sei nicht mehr feudal-sadistisch, sondern korporativ-anonym."
    ),

    // Question 38 - Brakhage und Mothlight
    Question(
        categoryId = 4,
        questionText = "Stan Brakhages 'Mothlight' (1963) entstand ohne Kamera. Welches Material verwendete Brakhage konkret, um diesen 3-Minuten-Film herzustellen?",
        answerA = "Mottenfluegel, Graeser, Bluetenstaub, Blaetter direkt zwischen Klebestreifen auf Filmband",
        answerB = "Getrocknete Blumen, die auf Negativfilm abfotografiert wurden",
        answerC = "Farbige Gelatine und Sand auf klarem Azetat",
        answerD = "Insektenkoerper in fluessigem Harz, dann in Film geschnitten",
        correctAnswer = 0,
        explanation = "Brakhage sammelte Mottenfluegel, Graeser, Blutenblaetter und anderen organischen Kleinkram und klebte ihn direkt zwischen zwei Klebeband-Streifen auf dem Filmband. Der Film laeuf bei 24fps und erzeugt beim Projizieren vibrierende, flimmernde organische Texturen — ohne eine einzige Kameraaufnahme.",
        difficulty = 5,
        funFact = "Brakhage war finanziell so arm, dass er sich Filmmaterial oft nicht leisten konnte. 'Mothlight' entstand, weil er kein Geld fuer Film hatte — er nutzte, was er buchstaeblich im Garten fand. Der Film wurde spaeter von den Museum of Modern Art in New York erworben und gilt als Schluesselwerk der Cameraless Cinema-Tradition."
    ),

    // Question 39 - Direct Cinema - Primary (1960)
    Question(
        categoryId = 4,
        questionText = "Robert Drew, Richard Leacock und D.A. Pennebaker drehten 1960 'Primary' ueber den US-Vorwahlkampf. Welche technische Innovation ermoeglichte erst den Direct-Cinema-Stil dieses Films?",
        answerA = "Synchronisiertes leichtes 16mm-Kameraequipment mit neuem Nagra-Tonbandgeraet",
        answerB = "Die erste digitale Kamera mit SSD-Speicher",
        answerC = "Neue Infrarotkameras fuer Low-Light-Aufnahmen",
        answerD = "Wireless-Mikrofone mit ueber 1km Reichweite",
        correctAnswer = 0,
        explanation = "Die Entwicklung leichter, schultergaengiger 16mm-Kameras (Eclair, Arriflex) kombiniert mit dem neuen Nagra-Tonbandgeraet ermoeglichten erstmals synchronen Ton bei mobilen Aufnahmen. Dies machte die typische Hand-Held-Aesthetik und das 'unsichtbare' Filmemachen des Direct Cinema technisch moeglich.",
        difficulty = 5,
        funFact = "In 'Primary' sieht man eine bahnbrechende Einstellung: Das Kamera-Team folgt John F. Kennedy durch eine Menschenmenge — wegedraengt, gestoessen, aber nie abgesetzt. Diese 'Verfolgungskamera' war technisch revolutionaer und gilt als Mutter aller modernen Reality-TV und Reportage-Kamerafuehrungen."
    ),

    // Question 40 - Cult Cinema - Phantom of the Paradise
    Question(
        categoryId = 4,
        questionText = "Brian De Palmas 'Phantom of the Paradise' (1974) war beim Kinostart ein Flop — mit einer einzigen Ausnahme. In welcher Stadt wurde der Film zum Riesenhit und lief jahrelang ausverkauft?",
        answerA = "Winnipeg, Kanada",
        answerB = "Paris, Frankreich",
        answerC = "Tokyo, Japan",
        answerD = "Sydney, Australien",
        correctAnswer = 0,
        explanation = "In Winnipeg, Kanada wurde 'Phantom of the Paradise' zum unglaeblichen Kulterfolg und lief dort wochenlang ausverkauft, waehrend der Film ueberall sonst floppte. Diese bizarre geografische Kultzelle in Winnipeg ist bis heute ein ungeklaertes Phaenomen der Filmkultur.",
        difficulty = 5,
        funFact = "Paul Williams, der auch den Soundtrack und die Songs fuer 'Phantom of the Paradise' schrieb, spielt im Film selbst die Hauptboeseweicht-Rolle Swan. Williams war in den 1970ern als Songwriter fuer Hits wie 'We've Only Just Begun' (Carpenters) und 'Evergreen' (Barbra Streisand) weltberuehmt."
    ),

    // Question 41 - Tobe Hooper und Texas Chain Saw Massacre
    Question(
        categoryId = 4,
        questionText = "Tobe Hoopers 'The Texas Chain Saw Massacre' (1974) ist ein Kulthorrorfilm, der auf keinem echten Fall beruht — obwohl er dies behauptet. Welcher Massenmorder lieferte die Grundidee fuer die Figur Leatherface?",
        answerA = "Ed Gein, der Menschenhaut zu Kleidung und Haushaltsgegenstaenden verarbeitete",
        answerB = "John Wayne Gacy, bekannt als 'Killer Clown'",
        answerC = "Ted Bundy, der attraktive Universitaetsstudent",
        answerD = "Charles Manson, dessen 'Familie' mehrere Morde beging",
        correctAnswer = 0,
        explanation = "Ed Gein (1906-1984), der 'Butcher of Plainfield', war die Inspirationsquelle fuer Leatherface: Gein exhumierte Leichen und verarbeitete Koerperteile zu Moebeln, Kleidung und Masken — darunter eine Gesichtsmaske aus menschlicher Haut. Gein inspirierte auch Norman Bates in 'Psycho' und Buffalo Bill in 'Das Schweigen der Laemmer'.",
        difficulty = 5,
        funFact = "Hooper behauptete, 'Texas Chain Saw Massacre' sei fuer eine PG-Freigabe konzipiert gewesen und sei verbluefit gewesen, als er ein R-Rating erhielt — er sah kaum explizites Blut im Film. Die MPAA war jedoch der Meinung, dass die Atmosphaere und psychologische Wirkung allein das R-Rating rechtfertigten."
    ),

    // Question 42 - Harun Farocki und Essay-Dokumentarfilm
    Question(
        categoryId = 4,
        questionText = "Harun Farocki, einer der wichtigsten deutschen Essayfilmer, ist bekannt fuer seine Analyse von Bildern und ihrer politischen Dimension. In welchem Film von 1988 analysiert er Bilder aus Konzentrationslagern und fragt, warum die Alliierten die Gaskammern nicht bombardierten?",
        answerA = "Wie man sieht",
        answerB = "Bilder der Welt und Inschrift des Krieges",
        answerC = "Leben - BRD",
        answerD = "Erkennen und Verfolgen",
        correctAnswer = 1,
        explanation = "'Bilder der Welt und Inschrift des Krieges' (1988) analysiert Luftaufnahmen, die Alliierte 1944 von Auschwitz gemacht hatten, ohne es zu wissen — auf der Suche nach Ruestungsanlagen fotografierten sie versehentlich die Gaskammern. Farocki fragt, was es bedeutet, etwas zu sehen, ohne es zu erkennen.",
        difficulty = 5,
        funFact = "Harun Farocki wurde 1966 von der Deutschen Film- und Fernsehakademie Berlin (dffb) exmatrikuliert, weil er an Studentenprotesten teilnahm und linksradikale Texte veroeffentlichte. Er wurde spaeter einer der einfluessreichsten deutschen Filmdenker und lehrte an der Staedelschule und der Hochschule fuer Bildende Kuenste Hamburg."
    ),

    // Question 43 - Andy Warhol und Empire
    Question(
        categoryId = 4,
        questionText = "Andy Warhols 'Empire' (1964) zeigt in Echtzeit das Empire State Building ueber einen Zeitraum von 485 Minuten. Mit welcher Bildrate wurde der Film aufgenommen, und mit welcher Bildrate soll er projiziert werden — was zu einer weiteren Verlaengerung fuehrt?",
        answerA = "Aufgenommen mit 24fps, projiziert mit 16fps — ergibt effektiv 8 Stunden Laufzeit",
        answerB = "Aufgenommen mit 16fps, projiziert mit 24fps — ergibt 6 Stunden Laufzeit",
        answerC = "Aufgenommen mit 24fps, projiziert mit 24fps — volle Echtzeit",
        answerD = "Aufgenommen mit 12fps, projiziert mit 16fps",
        correctAnswer = 0,
        explanation = "Warhol drehte 'Empire' mit 24fps auf 16mm, aber der Film soll mit 16fps projiziert werden. Diese Verlangsamung dehnt die 6 Stunden Rohmaterial auf rund 8 Stunden Projektionszeit aus und verstaerkt die hypnotische, quasi-meditative Qualitaet des Films.",
        difficulty = 5,
        funFact = "John Cale von der Velvet Underground soll bei der ersten Auffuehrung von 'Empire' eingeschlafen sein — was Warhol als das hoechste Kompliment auffasste. Warhol selbst sagte: 'Meine Idee war es, etwas zu machen, das laenger dauert als die meisten Filme und trotzdem nichts passiert.'"
    ),

    // Question 44 - Amos Vogel und Cinema 16
    Question(
        categoryId = 4,
        questionText = "Amos Vogel gruendete 1947 in New York 'Cinema 16', den wichtigsten Avantgarde-Filmklub der Nachkriegszeit. Was war das Innovative an seinem Konzept, das ihn von anderen Filmklubs unterschied?",
        answerA = "Er kombinierte experimentelle Kurzfilme systematisch mit Wissenschaftsdokumentarfilmen und politischen Filmen in einem Programm",
        answerB = "Er zeigte ausschliesslich Filme, die vom Hays Code verboten worden waren",
        answerC = "Er verlieh Filmkopien an Privatpersonen per Post",
        answerD = "Er finanzierte alle Mitglieder-Produktionen aus Beitraegen",
        correctAnswer = 0,
        explanation = "Vogels genialer Ansatz war die Mischung: Er kombinierte Experimentalfilme mit Naturwissenschaftsdokumentarfilmen (Chirurgie, Biologie), politischen und auslaendischen Filmen zu einem Gesamtprogramm. Diese Kontextualisierung machte experimentelle Werke einem breiteren Publikum zugaenglich.",
        difficulty = 5,
        funFact = "Amos Vogel schrieb 1974 das Standardwerk 'Film as a Subversive Art', das als Bibel des radikalen Kinos gilt. Cinema 16 hatte am Hoehepunkt ueber 7.000 Mitglieder — erstaunlich fuer einen Avant-garde-Filmklub in der Nachkriegszeit. Vogel gruendete spaeter gemeinsam mit Jonas Mekas das New York Film Festival."
    ),

    // Question 45 - Bill Morrisons Decasia
    Question(
        categoryId = 4,
        questionText = "Bill Morrisons 'Decasia' (2002) gilt als Meisterwerk des Found-Footage-Essayfilms. Welches spezifische Material verwendet Morrison ausschliesslich in diesem Film?",
        answerA = "Verfallenes, chemisch zersetztes Nitrozellulose-Archivfilmmaterial aus dem fruehen 20. Jahrhundert",
        answerB = "Unentwickelte Filmrollen aus dem Zweiten Weltkrieg",
        answerC = "Abgelehntes Studiomaterial aus Hollywood-Archiven der 1930er",
        answerD = "Heimfilme von unbekannten Privatpersonen aus den 1920er Jahren",
        correctAnswer = 0,
        explanation = "Morrison verwendet ausschliesslich verfallendes, chemisch zersetztes Nitrozellulose-Filmmaterial — der Verfall selbst wird zum kuenstlerischen Element. Blasen, Schmelzflecken und organische Zersetzungsstrukturen uberlagern die historischen Bilder und erzeugen ein hypnotisches Bild von Zeit und Vergaenglichkeit.",
        difficulty = 5,
        funFact = "Nitrozellulosefilm ist hochfeuergefaehrlich und wurde ab den 1950ern durch Sicherheitsfilm ersetzt. Grosse Teile des fruehen Filmarchivs der Menschheit sind verloren, weil Nitrofilm bei Raumbrand sofort explodiert oder wegen falscher Lagerung zersetzt. Morrison machte aus dieser Katastrophe buchstaeblich Kunst."
    ),

)
