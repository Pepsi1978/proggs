package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun filmQuestionsExpert3(): List<Question> = listOf(

    // --- ROBERT WIENE BEYOND CALIGARI ---

    // Question 1 - Wiene: Raskolnikow
    Question(
        categoryId = 4,
        questionText = "Welchen Dostojewski-Roman verfilmte Robert Wiene 1923 als expressionistisches Stummfilmwerk nach dem Vorbild des Caligari-Stils?",
        answerA = "Die Brueder Karamasow",
        answerB = "Raskolnikow",
        answerC = "Der Idiot",
        answerD = "Die Daemonen",
        correctAnswer = 1, // B
        explanation = "Robert Wiene verfilmte 1923 Dostojewskis 'Schuld und Suehne' als 'Raskolnikow'. Der Film uebernahm bewusst den expressionistischen Dekor-Stil von Caligari, mit schraegen Waenden und gemalten Schatten.",
        difficulty = 4,
        funFact = "Fuer 'Raskolnikow' engagierte Wiene das Moskauer Kuenstler-Theater (MChAT), was dem Film eine ungewoehnliche Mischung aus russischem Schauspielstil und deutschem Expressionismus verlieh."
    ),

    // Question 2 - Wiene: Orlacs Haende
    Question(
        categoryId = 4,
        questionText = "In Robert Wienes 'Orlacs Haende' (1924) erhielt ein Pianist nach einem Unfall die Haende eines hingerichteten Moerders transplantiert. Welcher Schauspieler spielte die Hauptrolle?",
        answerA = "Conrad Veidt",
        answerB = "Emil Jannings",
        answerC = "Werner Krauss",
        answerD = "Max Schreck",
        correctAnswer = 0, // A
        explanation = "Conrad Veidt spielte in 'Orlacs Haende' den Pianisten Paul Orlac. Veidt war bereits durch seine Rolle als Cesare in 'Das Cabinet des Dr. Caligari' bekannt und spezialisierte sich auf expressionistisch-unheimliche Figuren.",
        difficulty = 4,
        funFact = "Der Stoff wurde mehrfach verfilmt, darunter 1935 als 'Mad Love' mit Peter Lorre in Hollywood und 1960 als franzoesisch-britische Koproduktion mit Mel Ferrer."
    ),

    // Question 3 - Wiene: Genuine
    Question(
        categoryId = 4,
        questionText = "Wienes Film 'Genuine - Die Tragoedie eines seltsamen Hauses' (1920) gilt als kommerzieller und kuenstlerischer Misserfolg. Wer entwarf die expressionistischen Dekorationen?",
        answerA = "Walter Reimann",
        answerB = "Hermann Warm",
        answerC = "Caesar Klein",
        answerD = "Ernst Stern",
        correctAnswer = 2, // C
        explanation = "Die Dekorationen zu 'Genuine' (1920) wurden von Caesar Klein entworfen. Im Gegensatz zu Caligari, dessen Buehnenbilder von Warm, Reimann und Roehrig stammten, wirkte Kleins Design weniger kohaerent und ueberzeugte weder Kritik noch Publikum.",
        difficulty = 4,
        funFact = "Waehrend Caligari mit einem Budget von rund 20.000 Mark international Erfolg hatte, floppte 'Genuine' trotz aehnlichem Konzept und wurde zum Symbol fuer die Grenzen des expressionistischen Stils."
    ),

    // --- G.W. PABST DEEP CUTS ---

    // Question 4 - Pabst: Die freudlose Gasse
    Question(
        categoryId = 4,
        questionText = "G.W. Pabsts 'Die freudlose Gasse' (1925) wurde in mehreren Laendern zensiert. Welche der Hauptdarstellerinnen wurde durch diesen Film zum internationalen Star?",
        answerA = "Louise Brooks",
        answerB = "Greta Garbo",
        answerC = "Marlene Dietrich",
        answerD = "Asta Nielsen",
        correctAnswer = 1, // B
        explanation = "Greta Garbo hatte in 'Die freudlose Gasse' neben Asta Nielsen eine der frueheren Hauptrollen und wurde durch den internationalen Vertrieb des Films von MGM-Scouts entdeckt. Kurz darauf folgte ihr Hollywood-Vertrag.",
        difficulty = 4,
        funFact = "Der Film schildert den moralischen Verfall im inflationsgebeutelten Wien und wurde in Frankreich, England und Oesterreich stark geschnitten oder ganz verboten. Die vollstaendige Fassung gilt bis heute als verschollen."
    ),

    // Question 5 - Pabst: Geheimnisse einer Seele
    Question(
        categoryId = 4,
        questionText = "Pabsts 'Geheimnisse einer Seele' (1926) gilt als erster Film, der psychoanalytische Theorien Freuds dramatisiert. Welche Fachleute wirkten als wissenschaftliche Berater mit?",
        answerA = "Sigmund Freud selbst und Carl Gustav Jung",
        answerB = "Karl Abraham und Hanns Sachs",
        answerC = "Alfred Adler und Wilhelm Reich",
        answerD = "Otto Rank und Sandor Ferenczi",
        correctAnswer = 1, // B
        explanation = "Die Psychoanalytiker Karl Abraham und Hanns Sachs fungierten als Berater fuer 'Geheimnisse einer Seele'. Freud selbst lehnte eine Beteiligung ab, da er eine filmische Vereinfachung seiner Theorie ablehnte.",
        difficulty = 4,
        funFact = "Der Film zeigt eine der fruehesten visuellen Darstellungen eines Traums im Kino: Werner Krauss' Messerangst wird in einer aufwendigen Traumsequenz mit Doppelbelichtungen und verzerrten Perspektiven inszeniert."
    ),

    // Question 6 - Pabst: Westfront 1918
    Question(
        categoryId = 4,
        questionText = "Mit welchem technischen Novum arbeitete Pabst bei seinem Antikriegsfilm 'Westfront 1918' (1930) und hob ihn damit von anderen fruehen Tonfilmen ab?",
        answerA = "Er verwendete als erster Regisseur ein Richtmikrofon",
        answerB = "Er drehte Szenen mit bewegter Kamera trotz des sperrigen Tonaufnahme-Equipments",
        answerC = "Er nutzte erstmals synchronen Originalton ohne Nachsynchronisation",
        answerD = "Er setzte Stereophonie in Kinosaeilen ein",
        correctAnswer = 1, // B
        explanation = "Pabst liess bei 'Westfront 1918' die Kamera trotz des damals extrem schweren und lauten Tonaufnahme-Equipments bewegen. Dafuer wurde die Kamera in schallgedaemmten Holzboxen transportiert, was extreme logistische Leistungen erforderte.",
        difficulty = 4,
        funFact = "Pabsts Film erschien fast zeitgleich mit Lewis Milestones 'Im Westen nichts Neues' (1930). Beide galten als Meilensteine des Antikriegsfilms und wurden von den Nazis nach 1933 verboten."
    ),

    // Question 7 - Pabst: Die Dreigroschenoper
    Question(
        categoryId = 4,
        questionText = "Pabsts Verfilmung der 'Dreigroschenoper' (1931) fuehrte zu einem Rechtsstreit. Wer klagte gegen den Film und weshalb?",
        answerA = "Bertolt Brecht, weil sein Drehbuch nicht verwendet wurde",
        answerB = "Kurt Weill wegen der Bearbeitung seiner Musik",
        answerC = "Nero-Film wegen Vertragsbruchs durch den Verleih",
        answerD = "Ernst Josef Aufricht wegen der Buehnenrechte",
        correctAnswer = 0, // A
        explanation = "Bertolt Brecht klagte gegen die Nero-Film, weil sein eingereichtes Drehbuch durch eine stark abweichende Fassung ersetzt wurde. Brecht verlor den Prozess, schrieb aber seine Erfahrungen im 'Dreigroschenroman' und dem Essay 'Der Dreigroschenopernprozess' nieder.",
        difficulty = 4,
        funFact = "Gleichzeitig wurde eine franzoesische Sprachversion gedreht ('L'Opera de quat'sous'), was damals bei Tonfilmen gaengig war. Die franzoesische Fassung gilt von manchen Kritikern als die kuenstlerisch ueberlegenere."
    ),

    // --- LENI RIEFENSTAHL KONTROVERSE DETAILS ---

    // Question 8 - Riefenstahl: Triumph des Willens Technik
    Question(
        categoryId = 4,
        questionText = "Welche kameratechnische Innovation setzte Riefenstahl beim Reichsparteitag-Film 'Triumph des Willens' (1935) ein, die spaeter als Standard in der Sportberichterstattung galt?",
        answerA = "Die erste Handkamera im Dokumentarfilm",
        answerB = "Eine Laufschiene laengs des Aufmarschfeldes und ein Lift am Fahnenmast",
        answerC = "Unterwasserkameras in den Swastika-Pools",
        answerD = "Das erste Zoom-Objektiv im deutschen Kino",
        correctAnswer = 1, // B
        explanation = "Riefenstahl liess eine 175 Meter lange Laufschiene entlang des Aufmarschfeldes verlegen und einen Lift am zentralen Fahnenmast installieren. Diese Kamerapositionierungen ermoeglichten dynamische Fahrtaufnahmen und extreme Froschperspektiven, die als revolutionaer galten.",
        difficulty = 4,
        funFact = "Fuer 'Triumph des Willens' standen Riefenstahl 30 Kameras, 120 Hilfskraefte und ein Budget von 280.000 Reichsmark zur Verfuegung, was fuer einen Dokumentarfilm der Zeit beispiellos war."
    ),

    // Question 9 - Riefenstahl: Olympia Zweiteilung
    Question(
        categoryId = 4,
        questionText = "Riefenstahls Olympia-Film (1938) war in zwei Teile aufgeteilt. Wie lauteten die Untertitel der beiden Teile?",
        answerA = "'Fest der Voelker' und 'Fest der Schoenheit'",
        answerB = "'Fest der Nationen' und 'Fest der Athleten'",
        answerC = "'Fest der Staerke' und 'Fest des Geistes'",
        answerD = "'Fest des Lebens' und 'Fest der Jugend'",
        correctAnswer = 0, // A
        explanation = "'Olympia' erschien in zwei Teilen: 'Fest der Voelker' (Leichtathletik, Schwimmen u.a.) und 'Fest der Schoenheit' (Turnen, Rudern, Reiten, Fuenfkampf). Die Zweiteilung war praktisch bedingt, da das Gesamtmaterial fuer einen einzigen Kinoabend zu lang war.",
        difficulty = 4,
        funFact = "Jesse Owens, der schwarze US-Sprinter, der vier Goldmedaillen gewann und Hitlers Rassenideologie blamierte, wurde von Riefenstahl prominent und mit sichtbarer Bewunderung ins Bild gesetzt - was spaeter zu Debatten ueber ihre politische Haltung fuehrte."
    ),

    // Question 10 - Riefenstahl: Denazifizierungsverfahren
    Question(
        categoryId = 4,
        questionText = "Wie endete Riefenstahls Denazifizierungsverfahren nach dem Zweiten Weltkrieg offiziell?",
        answerA = "Sie wurde als Haupttaeterin zu zwei Jahren Gefaengnis verurteilt",
        answerB = "Sie wurde als Mitlaeufer eingestuft, was spaeter in 'entlastet' geaendert wurde",
        answerC = "Das Verfahren wurde eingestellt, ohne ein Urteil zu faellen",
        answerD = "Sie wurde freigesprochen und als politisch unbelastet eingestuft",
        correctAnswer = 1, // B
        explanation = "Riefenstahl wurde 1948 als 'Mitlaeufer' eingestuft. Eine Revision ergab 1952 die Einstufung als 'entlastet'. Sie selbst bestritt zeitlebens eine aktive Rolle als NS-Propagandistin und bezeichnete sich als rein kuenstlerische Filmemacherin.",
        difficulty = 4,
        funFact = "Susan Sontag schrieb 1975 den vielzitierten Essay 'Fascinating Fascism', in dem sie Riefenstahls Spaetwerk (Nuba-Photographien) mit ihrer NS-Aesthetik in Verbindung setzte und das Konzept der 'faschistischen Aesthetik' praegte."
    ),

    // --- HEIMATFILM-GENRE ---

    // Question 11 - Heimatfilm: Gruene ist die Heide
    Question(
        categoryId = 4,
        questionText = "Welcher Film gilt als einer der groessten Kassenerfolge des bundesdeutschen Heimatfilms und wurde 1951 uraufgefuehrt?",
        answerA = "Schwarzwaldmaedel",
        answerB = "Die Gruene ist die Heide",
        answerC = "Wenn der weisse Flieder wieder bluet",
        answerD = "Das Foersterhaus am Fuschsee",
        correctAnswer = 0, // A
        explanation = "'Schwarzwaldmaedel' (1950, Regie: Hans Deppe) mit Sonja Ziemann und Rudolf Prack war ein ueberwaeltigender Kassenerfolg und leitete die Hochphase des bundesdeutschen Heimatfilms ein. 'Die Gruene ist die Heide' (1951) war ebenfalls ein Riesenerfolg, aber 'Schwarzwaldmaedel' kam zuerst.",
        difficulty = 4,
        funFact = "Der Heimatfilm der 1950er Jahre galt als eskapistische Reaktion auf Kriegstrauma und Nachkriegsvertreibung: Er bot heile Landschaften, klare Moral und traditionelle Werte als emotionale Zuflucht fuer ein geschundenes Publikum."
    ),

    // Question 12 - Heimatfilm: Ideologische Funktion
    Question(
        categoryId = 4,
        questionText = "Welcher Filmwissenschaftler praegte die These, dass der westdeutsche Heimatfilm der 1950er Jahre als kollektiver Verdraengungsmechanismus fuer die NS-Vergangenheit funktionierte?",
        answerA = "Siegfried Kracauer",
        answerB = "Karsten Witte",
        answerC = "Thomas Elsaesser",
        answerD = "Eric Rentschler",
        correctAnswer = 1, // B
        explanation = "Karsten Witte formulierte in seinem einflussreichen Aufsatz 'Wie faschistisch ist der Bergfilm?' und spaeter in weiteren Texten die These des Heimatfilms als Verdraeangungsstrategie. Er analysierte, wie das Genre Kontinuitaeten zur NS-Aesthetik verdeckte.",
        difficulty = 4,
        funFact = "Der Anti-Heimatfilm der 1970er Jahre (Schloefer, Achternbusch, Fleischmann) reagierte bewusst auf das idealisierte Bild des Genres: 'Jagdszenen aus Niederbayern' (1969) von Peter Fleischmann gilt als zentrales Werk dieser Gegenbewegung."
    ),

    // Question 13 - Heimatfilm: Oesterreichische Variante
    Question(
        categoryId = 4,
        questionText = "Der oesterreichische Heimatfilm nutzte im Gegensatz zum deutschen die Alpenlandschaft und das Kaiserreich als Kulisse. Welches Filmformat dominierte den oesterreichischen Markt besonders?",
        answerA = "Die Sissi-Trilogie mit Romy Schneider",
        answerB = "Die Forster-Schinken-Filme mit Paul Hoerbiger",
        answerC = "Die Karl-May-Verfilmungen mit Stewart Granger",
        answerD = "Die Serien um den Grafen von Monte Christo",
        correctAnswer = 0, // A
        explanation = "Die Sissi-Trilogie (1955-57) von Ernst Marischka mit Romy Schneider und Karlheinz Boehm wurde zum Inbegriff des oesterreichischen Heimat- und Kaiserfilms. Die Filme waren international erfolgreich und schufen das bis heute wirkende touristische Klischeebild Oesterreichs.",
        difficulty = 4,
        funFact = "Romy Schneider versuchte spaeter aktiv, dem Sissi-Image zu entkommen, und nahm deshalb Rollen in franzoesischen Arthouse-Filmen an, u.a. bei Claude Sautet. In einem beruehmt gewordenen deutschen TV-Interview von 1958 weinte sie oeffentlich ueber ihre Festlegung."
    ),

    // --- OBERHAUSENER MANIFEST / JUNGER DEUTSCHER FILM ---

    // Question 14 - Oberhausener Manifest: Unterzeichner
    Question(
        categoryId = 4,
        questionText = "Das Oberhausener Manifest von 1962 wurde von 26 Filmemachern unterzeichnet. Welcher der spaeter bekanntesten deutschen Regisseure war NICHT unter den urspruenglichen Unterzeichnern?",
        answerA = "Alexander Kluge",
        answerB = "Rainer Werner Fassbinder",
        answerC = "Edgar Reitz",
        answerD = "Haro Senft",
        correctAnswer = 1, // B
        explanation = "Rainer Werner Fassbinder war nicht unter den Unterzeichnern des Oberhausener Manifests von 1962, da er damals erst 17 Jahre alt war und noch nicht als Filmemacher aktiv war. Alexander Kluge, Edgar Reitz und Haro Senft gehoerten hingegen zu den 26 Unterzeichnern.",
        difficulty = 4,
        funFact = "Das Manifest beginnt mit dem beruehmt gewordenen Satz: 'Der alte Film ist tot. Wir glauben an den neuen.' Trotz dieser Radikalitaet blieb die praktische filmische Umsetzung der Unterzeichner oft kompromissreicher als das Manifest verhiess."
    ),

    // Question 15 - Filmfoerderungsanstalt
    Question(
        categoryId = 4,
        questionText = "Welches foerderpolitische Instrument wurde 1965 direkt als Reaktion auf die Forderungen des Jungen Deutschen Films geschaffen und strukturiert die deutsche Filmfoerderung bis heute?",
        answerA = "Die Filmfoerderungsanstalt (FFA)",
        answerB = "Das Kuratorium Junger Deutscher Film",
        answerC = "Der Deutsche Filmfoerderfonds (DFFF)",
        answerD = "Die Filmstiftung NRW",
        correctAnswer = 0, // A
        explanation = "Die Filmfoerderungsanstalt (FFA) wurde 1965 durch das Filmfoerderungsgesetz gegruendet. Sie finanziert sich durch eine Abgabe der Kinobesitzer und Videoverleihbranche und vergab seither Milliarden an Produktionsbeihilfen fuer den deutschen Film.",
        difficulty = 4,
        funFact = "Parallel zur FFA entstand 1965 das Kuratorium Junger Deutscher Film, das kleinere, kuenstlerisch ambitionierte Projekte foerderte, waehrend die FFA eher kommerzielle Produktionen unterstuetzte. Diese Doppelstruktur spiegelte den Konflikt zwischen Kunst und Markt wider."
    ),

    // Question 16 - Kluge: Abschied von gestern
    Question(
        categoryId = 4,
        questionText = "Alexander Kluges Debuetuellfilm 'Abschied von gestern' (1966) gewann den Silbernen Loewen in Venedig. Auf welchem realen Fall basiert die Geschichte der Anita G.?",
        answerA = "Auf dem Leben der Terroristin Gudrun Ensslin",
        answerB = "Auf Kluges eigener Schwester Alexandra",
        answerC = "Auf dem Fall einer DDR-Fluechtenden namens Anita Gerstle",
        answerD = "Auf einem Bericht des Deutschen Institutes fuer Volksforschung",
        correctAnswer = 1, // B
        explanation = "Der Film basiert auf einem Fall aus dem Umfeld von Kluges Schwester Alexandra Kluge, die auch die Hauptrolle der Anita G. spielte. Kluge verarbeitete Beobachtungen aus dem sozialen Milieu seiner Schwester zu einem halbdokumentarischen Essay-Film.",
        difficulty = 4,
        funFact = "Kluge mischte in 'Abschied von gestern' Spielfilmelemente mit Dokumentaraufnahmen, Texttafeln und direkten Kameraansprachen - Techniken, die er der Nouvelle Vague und Brechts Verfremdungseffekt entlehnte."
    ),

    // --- BERLINER SCHULE: ANGELA SCHANELEC ---

    // Question 17 - Schanelec: Mein langsames Leben
    Question(
        categoryId = 4,
        questionText = "Angela Schanelecs Film 'Mein langsames Leben' (2001) gilt als Schluesselmanifestation der Berliner Schule. Welche stilistische Besonderheit kennzeichnet Schanelecs Kameraarbeit gegenueber anderen Vertretern der Berliner Schule?",
        answerA = "Extreme Weitwinkel-Optiken und hyperreale Farbsaettigung",
        answerB = "Strenge Plansequenzen und der Verzicht auf Schuss-Gegenschuss-Montage",
        answerC = "Handkamera und steter Bewegungsfluss",
        answerD = "Digitale Nachbearbeitung mit entsaettigten Farben",
        correctAnswer = 1, // B
        explanation = "Schanelec arbeitet mit streng komponierten, langen Einstellungen und vermeidet weitgehend die konventionelle Schuss-Gegenschuss-Montage. Dialoge werden oft mit stationaerer Kamera gefilmt, ohne zwischen den Sprechenden zu wechseln. Dies erzeugt eine eigentueimliche Distanz.",
        difficulty = 4,
        funFact = "Schanelec studierte wie Christian Petzold und Thomas Arslan an der DFFB (Deutsche Film- und Fernsehakademie Berlin) bei Harun Farocki und Hartmut Bitomsky, die die Berliner Schule massgeblich praegten."
    ),

    // Question 18 - Schanelec: Ich war zuhause, aber
    Question(
        categoryId = 4,
        questionText = "Angela Schanelecs 'Ich war zuhause, aber' (2019) gewann den Silbernen Baeren fuer die beste Regie in Berlin. Auf welchem antiken Dramenstoff greift der Film intertextuell zurueck?",
        answerA = "Sophokles: Antigone",
        answerB = "Euripides: Die Bakchen",
        answerC = "Sophokles: Hamlet (sic - eigentlich Shakespeare, aber Schanelec bezieht sich auf...)",
        answerD = "Shakespeares Hamlet",
        correctAnswer = 3, // D
        explanation = "Schanelec bezieht sich in 'Ich war zuhause, aber' mehrfach explizit auf Shakespeares Hamlet: Eine Schulklasse probt Hamlet-Szenen, und die Struktur des Films reflektiert Themen des Stuecks (Trauer, Schuld, Rueckkehr). Der Silberne Baer fuer Regie war ihr erster grosser Preis.",
        difficulty = 4,
        funFact = "Der Titel 'Ich war zuhause, aber' laesst semantisch offen, was auf das 'aber' folgt - eine Aussparung, die Schanelecs gesamte Aesthetik der Luecke und des Ungesagten symbolisiert."
    ),

    // --- BERLINER SCHULE: CHRISTIAN PETZOLD ---

    // Question 19 - Petzold: Gespenster
    Question(
        categoryId = 4,
        questionText = "Christian Petzolds 'Gespenster' (2005) ist der zweite Teil seiner inoffiziellen 'Gespenster-Trilogie'. Welche drei Filme bilden diese Trilogie?",
        answerA = "Die innere Sicherheit, Gespenster, Yella",
        answerB = "Jerichow, Gespenster, Barbara",
        answerC = "Yella, Gespenster, Transit",
        answerD = "Die innere Sicherheit, Toter Mann, Gespenster",
        correctAnswer = 0, // A
        explanation = "Petzolds inoffizielle 'Gespenster-Trilogie' (auch 'Liebes-Trilogie' genannt) umfasst 'Die innere Sicherheit' (2000), 'Gespenster' (2005) und 'Yella' (2007). Alle drei Filme verhandeln Figuren zwischen Leben und Tod, realer und gespenstischer Existenz.",
        difficulty = 4,
        funFact = "Die Bezeichnung 'Gespenster-Trilogie' ist rueckwirkend entstanden. Petzold selbst sprach von einer Trilogie ueber 'Frauen in Bewegung und ohne Heimat', was thematisch alle drei Filme verbindet."
    ),

    // Question 20 - Petzold: Harun Farocki
    Question(
        categoryId = 4,
        questionText = "Christian Petzold ko-schrieb mehrere seiner fruehen Drehbuecher mit seinem Lehrer und Mentor an der DFFB. Wer war dieser Mentor, der auch als Theoretiker und Essayfilmer weltweite Bedeutung erlangte?",
        answerA = "Wim Wenders",
        answerB = "Alexander Kluge",
        answerC = "Harun Farocki",
        answerD = "Herbert Achternbusch",
        correctAnswer = 2, // C
        explanation = "Harun Farocki war Petzolds Lehrer an der DFFB und ko-schrieb die Drehbuecher zu 'Die innere Sicherheit' (2000), 'Wolfsburg' (2003), 'Gespenster' (2005) und 'Yella' (2007). Farocki, bekannt als Essayfilmer und Theoretiker, starb 2014.",
        difficulty = 4,
        funFact = "Farocki und Petzold verbanden den politisch-analytischen Blick Farockis mit dem populaereren Genrekino Petzolds. Diese Synthese gilt als charakteristisch fuer die intellektuelle Dichte der Berliner Schule."
    ),

    // Question 21 - Petzold: Transit
    Question(
        categoryId = 4,
        questionText = "Christian Petzolds 'Transit' (2018) basiert auf Anna Seghers' Exilroman von 1944 und versetzt die Geschichte anachronistisch ins heutige Marseille. Welches Thema macht diese Zeitverschiebung explizit?",
        answerA = "Die Zeitlosigkeit buerokratischer Fluchtmechanismen",
        answerB = "Den Unterschied zwischen Nazi-Verfolgung und islamistischem Terror",
        answerC = "Die Ueberlegenheit des heutigen Asylrechts gegenueber 1940",
        answerD = "Das individuelle Schicksal gegenueber kollektiver Geschichte",
        correctAnswer = 0, // A
        explanation = "Durch die Parallelitaet von WWII-Romanhandlung und heutiger visueller Realitaet macht 'Transit' die Zeitlosigkeit von Verfolgung, Flucht und buerokratischer Absurdität sichtbar. Die Figuren leben in einer Art zeitlosen Exilzone, die Vergangenheit und Gegenwart ueberlagert.",
        difficulty = 4,
        funFact = "Petzold drehte 'Transit' mit echten Migranten als Statisten und liess die Schauspieler mit Gefluechteten in Marseille sprechen, um authentische Gesten und Verhaltensweisen zu studieren."
    ),

    // --- BERLINER SCHULE: MAREN ADE ---

    // Question 22 - Maren Ade: Alle Anderen
    Question(
        categoryId = 4,
        questionText = "Maren Ades zweiter Film 'Alle Anderen' (2009) gewann den Grossen Preis der Jury in Berlin. Was beschreibt den thematischen Kern dieses Films am praezisesten?",
        answerA = "Die Machtstruktur innerhalb einer Liebesbeziehung und maskuline Unsicherheit",
        answerB = "Die Integration von Migranten in den deutschen Mittelstand",
        answerC = "Generationenkonflikte zwischen Eltern und Kindern",
        answerD = "Genderfluide Identitaet in der urbanen Gegenwart",
        correctAnswer = 0, // A
        explanation = "'Alle Anderen' beobachtet auf Sardinien ein Paar (birgit Minichmayr, Lars Eidinger) und seziert, wie Geschlechterrollen, sozialer Druck und maskuline Unsicherheit eine Beziehung destabilisieren. Ades praezise Beobachtung von Machtmechanismen wurde international gelobt.",
        difficulty = 4,
        funFact = "Lars Eidinger und Birgit Minichmayr improvisierten viele Szenen nach Ades Vorgaben. Der Film entstand fast ausschliesslich an Aussenlocations in Sardinien und wurde mit minimaler Crew gedreht."
    ),

    // Question 23 - Toni Erdmann: Cannes-Reaktion
    Question(
        categoryId = 4,
        questionText = "Maren Ades 'Toni Erdmann' (2016) erhielt in Cannes eine stehende Ovation von berichteten 14 Minuten. Welches Gremium liess den Film dennoch ohne Palme d'Or nach Hause gehen, obwohl er in der Kritikerrangliste Platz 1 belegte?",
        answerA = "Die FIPRESCI-Jury",
        answerB = "Die Hauptjury unter George Miller",
        answerC = "Das Komitee fuer den Un Certain Regard",
        answerD = "Die Jury der Semaine de la Critique",
        correctAnswer = 1, // B
        explanation = "Die Hauptjury in Cannes 2016 unter Vorsitz von George Miller vergab die Palme d'Or an 'I, Daniel Blake' von Ken Loach. 'Toni Erdmann' ging leer aus, obwohl er in der Kritikerumfrage der Screen International Hoechstwertungen erhielt. Dies loeste eine der groessten Jurykontroversen der juengeren Cannes-Geschichte aus.",
        difficulty = 4,
        funFact = "'Toni Erdmann' wurde 2017 fuer einen Oscar als bester fremdsprachiger Film nominiert und lief vier Stunden im US-amerikanischen Remake-Format weiter: Jack Nicholson sollte die Titelrolle uebernehmen, das Projekt wurde aber eingestellt."
    ),

    // --- MULTI-KAMERA-SETUP HISTORISCH ---

    // Question 24 - Multi-Kamera: Lucille Ball
    Question(
        categoryId = 4,
        questionText = "Das moderne Multi-Kamera-Sitcom-System gilt als Erfindung von Karl Freund und Desi Arnaz fuer 'I Love Lucy' (1951). Was war das entscheidende technische Problem, das sie loesen mussten?",
        answerA = "Die Synchronisation mehrerer Tonspuren",
        answerB = "Die gleichmaessige Ausleuchtung eines gesamten Studiossets fuer mehrere simultane Kamerawinkel",
        answerC = "Die Uebertragung von Film auf Videoband",
        answerD = "Die Einbindung von Live-Publikum ohne Stoerrauschen",
        correctAnswer = 1, // B
        explanation = "Karl Freund, urspruenglich ein expressionistischer Stummfilmkameramann (Metropolis, Dracula), entwickelte ein neuartiges Beleuchtungskonzept, das gleichmaessiges, schattenarmes Licht fuer alle Kamerapositionen auf dem gesamten Set ermoeglichte. Vorher war jede Kameraposition individuell auszuleuchten.",
        difficulty = 4,
        funFact = "Karl Freund hatte zuvor Nosferatu, Metropolis und den ersten Dracula-Film fotografiert. Sein Wechsel zur TV-Studioleitung bei 'I Love Lucy' war fuer Hollywood-Verhaeltnisse eine spektakulaere Karrierewende."
    ),

    // Question 25 - Multi-Kamera: Deutsches Fernsehen
    Question(
        categoryId = 4,
        questionText = "Das bundesdeutsche Fernsehen der 1960er Jahre uebernahm das Multi-Kamera-Verfahren aus dem amerikanischen Studio-TV. Welche Programmgattung dominierte dieses Format anfaenglich im deutschen TV?",
        answerA = "Politische Talkshows und Nachrichtensendungen",
        answerB = "Unterhaltungsshows, Quizsendungen und Theaterfernseh-Uebertragungen",
        answerC = "Krimi- und Polizeiserien",
        answerD = "Sportberichterstattung",
        correctAnswer = 1, // B
        explanation = "Im deutschen TV der 1960er wurden zunachst Unterhaltungsshows, Quizsendungen (z.B. 'Was bin ich?') und Theaterfernsehen im Multi-Kamera-Betrieb produziert. Diese Live- oder quasi-Live-Formate profitierten von der gleichzeitigen Mehrfachperspektive.",
        difficulty = 4,
        funFact = "Das Multi-Kamera-Verfahren erforderte eine voellig andere Regiearbeit: Der Regie musste sitzend im Regieraum per Bildmischer zwischen Kameras schneiden, waehrend der Boden-Regisseur die Schauspieler fuer die naechsten Einstellungen positionierte."
    ),

    // --- SINGLE-KAMERA-REVOLUTION ---

    // Question 26 - Single Camera: HBO-Revolution
    Question(
        categoryId = 4,
        questionText = "Die Single-Kamera-Revolution im amerikanischen Qualitaets-TV wird oft mit der HBO-Serie 'Oz' (1997) begonnen. Was unterschied 'Oz' produktionstechnisch fundamental von damaligem Network-TV?",
        answerA = "Es wurde mit 35mm Film statt Video gedreht",
        answerB = "Jede Episode hatte ein gesondertes Produktionsteam",
        answerC = "Es gab kein Live-Publikum und keine Lachkonserve",
        answerD = "Alle Schauspieler wurden nach Drehbuchfertigstellung gecastet",
        correctAnswer = 0, // A
        explanation = "HBO-Serien wie 'Oz' wurden mit 35mm-Film gedreht und nutzten Single-Kamera-Filmproduktion statt des typischen Multi-Kamera-Videoverfahrens der Networks. Dies erlaubte cinematografische Beleuchtung, Schuss-Gegenschuss-Montage und Aussendreh mit echten Locations.",
        difficulty = 4,
        funFact = "Die Entscheidung, HBO-Serien auf Film zu drehen, verteuerte die Produktion erheblich, ermoeglichte aber die visuelle Qualitaet, die HBO zum Synonym fuer 'Kino im Fernsehen' machte. 'The Sopranos' kostete pro Folge rund zwei Millionen Dollar - das Dreifache einer normalen Network-Sitcom."
    ),

    // Question 27 - Single Camera: The Office
    Question(
        categoryId = 4,
        questionText = "Die britische Originalserie 'The Office' (2001) von Ricky Gervais und Stephen Merchant nutzte Single-Kamera im Mockumentary-Stil. Welche technische Vorgabe machte das Konzept besonders fordernd fuer die Schauspieler?",
        answerA = "Alle Szenen mussten ohne Schnitt gedreht werden",
        answerB = "Die Kamera war immer unangekuendigt in Bewegung und reagierte dokumentarisch",
        answerC = "Alle Dialoge mussten improvisiert werden ohne Skript",
        answerD = "Jede Szene wurde von zwei verschiedenen Teams unabhaengig gedreht",
        correctAnswer = 1, // B
        explanation = "In 'The Office' sollte die Kamera wie ein Dokumentarfilmteam agieren: zufaellig, reaktiv, niemals vorhersehbar. Die Schauspieler mussten so spielen, als waeren die Kameras immer schon da, ohne direkte Hinweise zu geben, wann Reaktionen gefilmt werden.",
        difficulty = 4,
        funFact = "Gervais und Merchant schrieben das Skript trotz des dokumentarischen Stils sehr detailliert. Die Improvisation war minimal - was wie spontan wirkte, war genau kalkuliert und oft mehrfach geprobt."
    ),

    // --- BOTTLE EPISODES ---

    // Question 28 - Bottle Episode: Definition und Ursprung
    Question(
        categoryId = 4,
        questionText = "Der Begriff 'Bottle Episode' stammt urspruenglich aus der Produktion welcher Serie, wo er als interner Slang entstand?",
        answerA = "Star Trek: The Original Series",
        answerB = "M*A*S*H",
        answerC = "Cheers",
        answerD = "Seinfeld",
        correctAnswer = 0, // A
        explanation = "Der Begriff 'Bottle Episode' entstand als interner Produktionsslang bei 'Star Trek' (1966-69). Er bezeichnete Episoden, die aus Budgetgruenden komplett auf dem Schiffsdeck spielten - als haetten sie die Handlung 'in der Flasche' eingeschlossen.",
        difficulty = 4,
        funFact = "Die beruhmteste Bottle Episode der Fernsehgeschichte gilt 'The Fly' aus Breaking Bad (S4E10). Sie wurde als Budget-Einsparung geplant, entwickelte sich aber zur tiefgriendlichsten, philosophischsten Episode der gesamten Serie."
    ),

    // Question 29 - Bottle Episode: Breaking Bad The Fly
    Question(
        categoryId = 4,
        questionText = "Welcher Autor schrieb 'The Fly', die beruehmt-beruechtigte Bottle-Episode aus 'Breaking Bad', und welcher Regisseur fuehrte Regie?",
        answerA = "Vince Gilligan; Rian Johnson",
        answerB = "Sam Catlin; Rian Johnson",
        answerC = "Moira Walley-Beckett; Michelle MacLaren",
        answerD = "Gennifer Hutchison; George Mastras",
        correctAnswer = 1, // B
        explanation = "Sam Catlin schrieb das Skript zu 'The Fly' (S4E10 Breaking Bad), Regie fuehrte Rian Johnson, der auch andere Breaking-Bad-Episoden und spaeter 'Looper', 'The Last Jedi' und 'Knives Out' drehte.",
        difficulty = 4,
        funFact = "Das Budget fuer 'The Fly' betrug laut Berichten unter 2 Millionen Dollar - deutlich weniger als eine Standard-Breaking-Bad-Episode. Das eingesparte Geld floss in die aufwendigeren Folgen davor und danach."
    ),

    // --- COLD OPENS ---

    // Question 30 - Cold Open: Definition und Funktion
    Question(
        categoryId = 4,
        questionText = "Was bezeichnet ein 'Cold Open' in der Fernsehterminologie, und welche Serie machte das Konzept in den 1980ern besonders populaer?",
        answerA = "Eine Szene vor dem Titelvorspann; Miami Vice",
        answerB = "Eine Szene vor dem Titelvorspann; Hill Street Blues",
        answerC = "Der erste Dialog einer Episode ohne visuellen Einstieg; Cheers",
        answerD = "Eine spannungsaufbauende Rueckblende zu Beginn; Dallas",
        correctAnswer = 1, // B
        explanation = "Ein 'Cold Open' ist eine Szene, die direkt vor dem Titelvorspann steht und den Zuschauer ohne Anlauf in die Handlung wirft. 'Hill Street Blues' (1981) machte dieses Strukturelement in der US-Dramaserie populaer und wurde damit zum Vorbild fuer das 'Quality TV' der 1990er.",
        difficulty = 4,
        funFact = "Die klaerste Weiterentwicklung des Cold Open schuf Breaking Bad: Viele Episoden beginnen mit in sich geschlossenen Raetseln, die erst am Episodenende oder spaeter aufgeloest werden - ein narratives Mittel, das als 'Teaser-Cold-Open' bezeichnet wird."
    ),

    // Question 31 - Cold Open: The Office NBC
    Question(
        categoryId = 4,
        questionText = "Die US-Version von 'The Office' (NBC) wurde fuer ihre Cold Opens bekannt, die meist eigenstaendige Witze ohne Bezug zur Haupthandlung enthielten. Wie nannte sich die Produktionsstrategie, bei der die Cold Opens separat vom Hauptscript entwickelt wurden?",
        answerA = "Tag-Scene-Methode",
        answerB = "Cold-Open-Room-Verfahren",
        answerC = "A/B-Plot-Trennung",
        answerD = "Front-Loading-Technik",
        correctAnswer = 1, // B
        explanation = "In der NBC-Version von 'The Office' wurden Cold Opens oft im sogenannten 'Cold-Open-Room' separat entwickelt, einem kleineren Writer's-Room-Meeting, das sich ausschliesslich auf die Eingangsszenen konzentrierte. Diese Spezialisierung erlaubte die beruehmte Dichte der Cold-Open-Witze.",
        difficulty = 4,
        funFact = "Einige der beliebtesten The-Office-Cold-Opens (z.B. 'Parkour', 'Threat Level Midnight Preview') wurden spaeter zu eigenstaendigen Internet-Phaenomenen und generierten mehr Social-Media-Aufmerksamkeit als die Haupthandlung der jeweiligen Folgen."
    ),

    // --- TV RATINGS WARS / NETZWERKGESCHICHTE ---

    // Question 32 - Quotenkrieg: Nielsen-System
    Question(
        categoryId = 4,
        questionText = "Die Nielsen-Messpanels wurden in den USA in den 1950ern als Standard etabliert. Welche fundamentale methodische Kritik wurde an ihnen ab den 1990ern immer lauter?",
        answerA = "Das Panel war zu klein und repraesentierte keine Minderheiten korrekt",
        answerB = "Die Messung erfasste keine zeitversetzten Aufzeichnungen (Timeshift-Viewing)",
        answerC = "Die Messtechnik war anfaellig fuer Manipulation durch Sender",
        answerD = "Das System bevorzugte Primetime und vernachlaessgte Daytime",
        correctAnswer = 1, // B
        explanation = "Mit der Verbreitung von VHS-Recordern (1980er) und spaeter DVR/TiVo (ab 1999) konnte das konventionelle Nielsen-System keine zeitversetzten Aufzeichnungen messen. Sender verloren massiv gemessene Zuschauer, obwohl das Publikum tatsaechlich wuchs - nur zu anderen Zeiten schaute.",
        difficulty = 4,
        funFact = "Nielsen fuehrte erst 2006 das 'Live+7'-Messsystem ein, das Zuschauer erfasst, die innerhalb von 7 Tagen nach Erstausstrahlung schauten. Heute ist 'Live+35' Standard - was manchen Serien Rettung vor Absetzung brachte."
    ),

    // Question 33 - AGF/GfK: Deutschland
    Question(
        categoryId = 4,
        questionText = "Die Arbeitsgemeinschaft Fernsehforschung (AGF) in Deutschland verwendet seit 1985 ein telemetrisches Panel. Welches Unternehmen fuehrt die Messung im Auftrag der AGF durch und wie gross ist das Panel approximativ?",
        answerA = "GfK; rund 5.000 Haushalte",
        answerB = "Ipsos; rund 10.000 Haushalte",
        answerC = "Nielsen; rund 3.000 Haushalte",
        answerD = "Kantar; rund 7.500 Haushalte",
        correctAnswer = 0, // A
        explanation = "Die GfK (Gesellschaft fuer Konsumforschung) fuehrt die TV-Quotenmessung fuer die AGF durch. Das Panel umfasste lange rund 5.000 Haushalte mit ca. 10.000 Personen. Ab 2022 wird ein erweitertes Hybrid-Panel mit Connected-TV-Daten kombiniert.",
        difficulty = 4,
        funFact = "Die AGF-Messung beginnt um 3:00 Uhr morgens und endet um 2:59 Uhr des Folgetages. Die Tagesdaten werden am Morgen des naechsten Tages veroeffentlicht - die Sender erfahren also noch vor Mitternacht, wieviele Menschen ihre Primetime-Sendungen sahen."
    ),

    // Question 34 - Quotenkrieg: Sat.1 vs. RTL
    Question(
        categoryId = 4,
        questionText = "Der Quotenkrieg zwischen RTL und Sat.1 in den 1990er Jahren gilt als praegendes Kapitel der deutschen TV-Geschichte. Mit welcher Programmstrategie konnte RTL 1992 erstmals dauerhaft die Marktfuehrerschaft im deutschen Privatfernsehen uebernehmen?",
        answerA = "Durch den Erwerb der Bundesliga-Uebertragungsrechte",
        answerB = "Durch die Einfuehrung von Daily Soaps und Boulevard-Magazinen in der Daytime",
        answerC = "Durch den Einstieg in Premium-Sportrechte und Formel-1",
        answerD = "Durch den Start des ersten deutschen Pay-TV-Kanals",
        correctAnswer = 2, // C
        explanation = "RTL sicherte sich 1991 die Formel-1-Uebertragungsrechte von ARD/ZDF und ab 1992 zahlreiche Sportrechte sowie attraktive US-Serien. Diese Premium-Sportstrategie kombiniert mit Daily Soaps (Gute Zeiten, schlechte Zeiten ab 1992) machte RTL zur Nummer 1.",
        difficulty = 4,
        funFact = "'Gute Zeiten, schlechte Zeiten' startete am 11. Mai 1992 auf RTL - als direkte Reaktion auf den Sat.1-Erfolg mit 'Verbotene Liebe'. GZSZ lief seither ununterbrochen und ist die laengste laufende taeglich ausgestrahlte Fernsehserie Deutschlands."
    ),

    // Question 35 - Netzwerkgeschichte: ZDF Gruendung
    Question(
        categoryId = 4,
        questionText = "Das ZDF nahm 1963 den Sendebetrieb auf als oeffentlich-rechtliche Reaktion auf welchen politischen Konflikt?",
        answerA = "Auf den Versuch der Bundesregierung Adenauer, ein staatliches Fernsehen zu gruenden",
        answerB = "Auf den Streit zwischen ARD-Anstalten ueber die Programmhoheit",
        answerC = "Auf Beschwerden der Bundeslaender ueber zu viel Einfluss des NDR",
        answerD = "Auf den Wunsch der Werbewirtschaft nach einem zweiten nationalen Kanal",
        correctAnswer = 0, // A
        explanation = "Bundeskanzler Adenauer versuchte 1960/61, ein bundesstaatliches Fernsehen ('Deutschland-Fernsehen GmbH') zu gruenden. Das Bundesverfassungsgericht untersagte dies 1961 (erstes Rundfunkurteil). Als Kompromiss gruendeten die Laender das laenderuebergreifend organisierte ZDF.",
        difficulty = 4,
        funFact = "Das erste Rundfunkurteil von 1961 ist bis heute das Fundament des deutschen Rundfunkrechts. Es legt fest, dass Rundfunk Ländersache ist und der Staat keinen direkten Einfluss auf Programminhalte haben darf."
    ),

    // Question 36 - Netzwerkgeschichte: Privat-TV Start
    Question(
        categoryId = 4,
        questionText = "An welchem genauen Datum begann das private Fernsehen in Deutschland offiziell zu senden, und mit welchem Sender?",
        answerA = "1. Januar 1984 mit PKS (spaeter Sat.1)",
        answerB = "2. Januar 1984 mit RTL plus",
        answerC = "3. Januar 1984 mit PKS und gleichzeitig RTL plus",
        answerD = "31. Dezember 1983 um Mitternacht mit RTL plus",
        correctAnswer = 0, // A
        explanation = "PKS (Programmgesellschaft fuer Kabel- und Satellitenrundfunk, spaeter Sat.1) startete am 1. Januar 1984 als erster privater Sender in Deutschland. RTL plus startete am 2. Januar 1984. Das Startdatum 1.1.1984 gilt als Beginn des dualen Rundfunksystems in Deutschland.",
        difficulty = 4,
        funFact = "Die erste Sendung von PKS am 1. Januar 1984 war eine Wiederholung eines ARD-Programms - mangels eigenem Programm. Das private Fernsehen startete also buchstaeblich mit geliehenem oeffentlich-rechtlichem Inhalt."
    ),

    // --- BERLINER SCHULE: WEITERE ASPEKTE ---

    // Question 37 - Berliner Schule: Definition
    Question(
        categoryId = 4,
        questionText = "Der Begriff 'Berliner Schule' wurde nicht von den Regisseuren selbst gewaehlt, sondern von der Filmkritik gepraegte. Welcher Kritiker ist massgeblich fuer die Etablierung des Begriffs verantwortlich?",
        answerA = "Georg Seeßlen",
        answerB = "Michael Althen",
        answerC = "Christoph Hochhaeusler",
        answerD = "Rainer Gansera",
        correctAnswer = 1, // B
        explanation = "Der Kritiker Michael Althen (Sueddeutsche Zeitung) gilt als einer der wichtigsten Praegler des Begriffs 'Berliner Schule' fuer die Gruppe um Petzold, Schanelec und Arslan. Die betroffenen Regisseure lehnten den Begriff teils ab, da er eine Schule impliziert, die sie nie bildeten.",
        difficulty = 4,
        funFact = "Thomas Arslan, der dritte Kernvertreter der Berliner Schule neben Petzold und Schanelec, ist der oeffenlichkeit am wenigsten bekannt. Sein Film 'Gold' (2013) - ein Westernfilm mit Nina Hoss - markierte eine gattungsgeografische Expansion der Berliner Schule."
    ),

    // Question 38 - Petzold: Barbara
    Question(
        categoryId = 4,
        questionText = "In Christian Petzolds 'Barbara' (2012) spielt Nina Hoss eine Aerztin in der DDR, die auf eine Ausreise nach Westdeutschland wartet. Welchen grossen Preis gewann der Film in Berlin?",
        answerA = "Goldenen Baeren",
        answerB = "Silbernen Baer fuer die beste Regie",
        answerC = "Grossen Preis der Jury",
        answerD = "FIPRESCI-Preis",
        correctAnswer = 1, // B
        explanation = "Christian Petzold gewann fuer 'Barbara' den Silbernen Baer fuer die beste Regie bei der Berlinale 2012. Es war sein bislang kommerziell erfolgreichster Film in Deutschland und brachte ihm internationale Bekanntheit.",
        difficulty = 4,
        funFact = "Nina Hoss arbeitete mit Petzold in fast allen seinen Hauptwerken zusammen: 'Yella', 'Jerichow', 'Barbara', 'Phoenix' und 'Undine'. Diese Zusammenarbeit gilt als eine der produktivsten Regisseur-Schauspielerin-Partnerschaften im deutschen Gegenwartskino."
    ),

    // --- SPEZIFISCHE PRODUKTIONSTECHNIKEN ---

    // Question 39 - Drehbuchentwicklung: Writers Room
    Question(
        categoryId = 4,
        questionText = "Das amerikanische Writers-Room-System, bei dem ein Showrunner ein Team von Autoren koordiniert, unterscheidet sich fundamental vom deutschen Modell. Was ist das charakteristische Merkmal des deutschen TV-Drehbuchsystems bis in die 2010er?",
        answerA = "Der Produzent schreibt das Drehbuch gemeinsam mit einem Regisseur",
        answerB = "Ein einzelner Autor schreibt das Drehbuch allein, der Sender hat wenig Einfluss",
        answerC = "Drehbuecher entstehen in kleinen Zweier- oder Dreier-Teams ohne Hierarchie",
        answerD = "Redakteure der Sender uebernehmen nach erster Abgabe die endgueltige Texthoheit",
        correctAnswer = 3, // D
        explanation = "Im deutschen TV hatten bis in die 2010er Jahre die Redakteure der oeffentlich-rechtlichen Sender de facto die letzte Texthoheit. Das Gegenlesungs- und Abnahmesystem der ARD/ZDF-Redaktionen fuehrte dazu, dass Redakteure tiefgreifende Aenderungen an Drehbuechern vornehmen konnten.",
        difficulty = 4,
        funFact = "Diese Redakteursmacht wurde als einer der Hauptgruende fuer die Mittelmassigkeit vieler ARD/ZDF-Produktionen diskutiert. Die Einfuehrung von Komplexserien wie 'Dark' (Netflix) und 'Babylon Berlin' (Sky/ARD) funktionierte erst, als Redakteure weniger Einfluss hatten."
    ),

    // Question 40 - Showrunner: Westdeutsche Serien
    Question(
        categoryId = 4,
        questionText = "Welche deutsche Serie gilt als erste konsequente Umsetzung des amerikanischen Showrunner-Modells und verband erstmals eine starke Autorenschaft mit hohem Produktionsbudget?",
        answerA = "Der Tatort (ab 1970)",
        answerB = "Dark (Netflix, 2017)",
        answerC = "Babylon Berlin (Sky/ARD, 2017)",
        answerD = "Deutschland 83 (RTL, 2015)",
        correctAnswer = 3, // D
        explanation = "'Deutschland 83' (2015) von Anna und Joerg Winger fuer RTL gilt als Pionierprojekt des deutschen Qualitaets-TV nach amerikanischem Modell. Anna Winger uebernahm de facto die Showrunner-Funktion. Der Erfolg im US-amerikanischen Sundance Channel gab dem deutschen Serienmarkt neuen Selbstbewusstsein.",
        difficulty = 4,
        funFact = "'Deutschland 83' war die erste in Deutschland produzierte Fernsehserie, die im US-amerikanischen Fernsehen zur Primetime ausgestrahlt wurde - und die erste deutschsprachige Serie auf einem US-Netzwerk ueberhaupt."
    ),

    // --- OESTERREICHISCHES KINO ---

    // Question 41 - Haneke: Funny Games
    Question(
        categoryId = 4,
        questionText = "Michael Hanekes 'Funny Games' (1997) enthielt eine beruehmt-beruechtigte Szene, in der die vierte Wand gebrochen wird. Was passiert genau in dieser Schluesselszene?",
        answerA = "Ein Taeter spricht direkt in die Kamera und fragt das Publikum, ob es weiterschauen will",
        answerB = "Ein Taeter benutzt eine Fernbedienung, um die Filmhandlung zurueckzuspulen",
        answerC = "Das Opfer verlaesst den Film und beobachtet sein eigenes Schicksal aus der Distanz",
        answerD = "Der Regisseur erscheint selbst im Bild und stoppt die Szene",
        correctAnswer = 1, // B
        explanation = "In 'Funny Games' benutzt einer der Taeter eine Fernbedienung, um eine bereits gesehene Szene buchstaeblich zurueckzuspulen und das gerade erschossene Opfer wieder zum Leben zu erwecken. Diese Meta-Szene thematisiert die Kontrollillusionen des Filmzuschauers.",
        difficulty = 4,
        funFact = "Haneke drehte 'Funny Games' 2007 als Shot-for-Shot-Remake mit dem Titel 'Funny Games U.S.' mit Naomi Watts und Tim Roth. Darin ist die Fernbedienen-Szene identisch nachgedreht - was Hanekes These ueber die Komplicenschaft des Publikums fuer ein neues Publikum wiederholte."
    ),

    // Question 42 - Seidl: Paradies-Trilogie
    Question(
        categoryId = 4,
        questionText = "Ulrich Seidls 'Paradies'-Trilogie (2012/13) zeigt drei Frauen aus einer oesterreichischen Familie. Welche drei Themen spiegeln die drei Teile wider?",
        answerA = "Liebe, Glaube, Hoffnung",
        answerB = "Liebe, Glaube, Geld",
        answerC = "Sex, Religion, Magersucht",
        answerD = "Einsamkeit, Sucht, Gewalt",
        correctAnswer = 0, // A
        explanation = "Seidls Trilogie besteht aus 'Paradies: Liebe' (Sexualtourismus in Kenia), 'Paradies: Glaube' (religioeser Fanatismus in Oesterreich) und 'Paradies: Hoffnung' (Teenager in einem Abnehmcamp). Die drei Teile zeigen drei Frauen einer Familie in ihrer jeweiligen Sehnsucht.",
        difficulty = 4,
        funFact = "Ulrich Seidl arbeitete fuer alle drei Filme gleichzeitig, drehte sie parallel und in verschraenkter Reihenfolge. Alle drei Teile liefen 2012 auf dem Filmfestival Venedig - ein einzigartiger Dreifach-Auftritt in der Geschichte des Festivals."
    ),

    // Question 43 - Oesterreichische Neue Welle
    Question(
        categoryId = 4,
        questionText = "Die oesterreichische Neue Welle der 1990er/2000er Jahre (Haneke, Seidl, Spielmann, Hausner) wird oft unter dem Begriff 'Oesterreichischer Film' zusammengefasst. Welcher internationale Filmkritiker-Begriff beschreibt ihren gemeinsamen Gestus am treffendsten?",
        answerA = "Slow Cinema",
        answerB = "Torture Cinema oder Cinema of Unease",
        answerC = "New Austrian Realism",
        answerD = "Transgressive Minimalism",
        correctAnswer = 1, // B
        explanation = "Der Begriff 'Torture Cinema' (manchmal auch 'Cinema of Unease' oder 'New Austrian Cinema') beschreibt die Tendenz dieser Filme zu physischem und psychischem Unbehagen, langen Einstellungen auf Qualen und einer klinischen Kamera ohne Empathie. Der Kritiker James Quandt praegte 'extreme cinema' als verwandten Begriff.",
        difficulty = 4,
        funFact = "Der Begriff 'New Zealand Cinema of Unease' wurde von Peter Wells fuer einen BBC-Dokumentarfilm ueber das neuseelaendische Kino gepraegte - spaeter wurde der Begriff auf das oesterreichische Kino uebertragen, da beides eine aehnliche Neigung zu bedrohlichen Heimatfilmen zeigte."
    ),

    // --- PRODUKTIONSTECHNIK: WEITERE ASPEKTE ---

    // Question 44 - Zweikanalton im deutschen TV
    Question(
        categoryId = 4,
        questionText = "Das deutsche Fernsehen fuehrte 1981 einen technischen Standard ein, der es erlaubte, Sendungen in zwei Sprachen oder mit Originalspur zu empfangen. Wie heisst dieses System?",
        answerA = "NICAM-Stereo",
        answerB = "Zweikanalton (A2-Stereo)",
        answerC = "Dolby Surround TV",
        answerD = "BTSC-Stereo",
        correctAnswer = 1, // B
        explanation = "Das deutsche A2-Zweikanalton-System wurde ab 1981 einfuehrt und erlaubte Stereosendungen sowie Zweisprachigkeit (z.B. Originalsprache auf Kanal 2). Dieses System war weltweit einmalig und ermoeglichte im deutschen TV Originalversionen von US-Serien.",
        difficulty = 4,
        funFact = "Waehrend Laender wie Frankreich und Grossbritannien US-Serien komplett synchronisieren mussten, konnten deutsche Zuschauer mit passendem Fernseher US-Serien wie Dallas oder Knight Rider im amerikanischen Original hoeren - Jahrzehnte vor DVD-Optionen."
    ),

    // Question 45 - Pilotfolge vs. Backdoor Pilot
    Question(
        categoryId = 4,
        questionText = "Ein 'Backdoor Pilot' ist ein Begriff aus der US-TV-Industrie. Was unterscheidet ihn von einem regulaeren Pilotfilm?",
        answerA = "Er wird ohne Wissen des Senders produziert und spaeter angeboten",
        answerB = "Er ist als eigenstaendige Episode einer bestehenden Serie getarnt, prueft aber eine Spin-off-Serie",
        answerC = "Er wird nach der ersten Staffel gedreht, wenn die Serie schon erfolgreich ist",
        answerD = "Er enthaelt nur bekannte Schauspieler ohne neue Charaktere",
        correctAnswer = 1, // B
        explanation = "Ein Backdoor Pilot ist eine Episode einer laufenden Serie, die ostentabel als regulaere Episode laeuft, tatsaechlich aber neue Charaktere und Settings eines geplanten Spin-offs vorstellt. So kann der Sender reaktionen des Publikums messen, ohne explizit einen teuren Pilotfilm zu beauftragen.",
        difficulty = 4,
        funFact = "'Frasier' startete als Backdoor-Pilot in 'Cheers': Die Episode 'Cheerio, Everybody' (S11E22) stellte Frasier Cranes Rueckkehr nach Seattle vor und funktionierte als Test fuer die Spin-off-Serie. Frasier lief dann von 1993-2004 mit 11 Staffeln."
    ),

    // Question 46 - 100-Episoden-Regel
    Question(
        categoryId = 4,
        questionText = "Die '100-Episoden-Regel' (auch als 'Syndication threshold' bekannt) ist ein US-TV-Industriestandard. Was besagt sie?",
        answerA = "Eine Serie muss 100 Episoden produziert haben, um fuer internationale Lizenzen in Frage zu kommen",
        answerB = "Eine Serie braucht ca. 100 Folgen, um im Syndication-Markt lukrativ vermarktet werden zu koennen",
        answerC = "Schauspieler in Hauptrollen erhalten nach 100 Episoden automatisch Produktionsbeteiligung",
        answerD = "Sender muessen nach 100 Episoden neues Schauspieler-Ensemble einsetzen",
        correctAnswer = 1, // B
        explanation = "Im US-Syndication-System koennen Serien erst ab ca. 100 Episoden in einem 5-mal-woechentlichen Strip-Schedule vermarktet werden - was fuer lokale TV-Stationen attraktiv ist. Serien mit weniger Episoden eignen sich nicht fuer dieses Format und bringen deutlich weniger Syndication-Einnahmen.",
        difficulty = 4,
        funFact = "Die 100-Episoden-Schwelle erklaert, warum US-Serien so lange laufen: 'Friends' hatte 236 Episoden, 'Seinfeld' 180. Die Syndication-Rechte beider Serien wurden fuer jeweils ueber eine Milliarde Dollar verkauft und brachten den Hauptdarstellern jahrzehntelang passives Einkommen."
    ),

    // --- DOKUMENTARFILM FORMEN ---

    // Question 47 - Riefenstahl: Triumph des Willens Gattungsdebatte
    Question(
        categoryId = 4,
        questionText = "Die Gattungsfrage bei 'Triumph des Willens' (1935) ist bis heute strittig. Welches Argument wurde von Filmwissenschaftlern am haeufigssten vorgebracht, um den Film als 'inszenierte Dokumentation' statt echten Dokumentarfilm zu klassifizieren?",
        answerA = "Riefenstahl verwendete Schauspielerei in Schlueselszenen",
        answerB = "Der Parteitag wurde teilweise fuer die Kamera umgestellt und vor-inszeniert",
        answerC = "Das Drehbuch wurde vorab von Goebbels genehmigt",
        answerD = "Es wurden Archivmaterialien eingefuegt, die nicht am Parteitag entstanden",
        correctAnswer = 1, // B
        explanation = "Historiker und Filmwissenschaftler dokumentierten, dass bestimmte Aufmarschteilnehmer fuer Riefenstahls Kameras ihre Positionen veraenderten, einzelne Szenen wiederholt wurden und die Choreografie des Parteitags teilweise mit Riefenstahls Drehanforderungen abgestimmt war.",
        difficulty = 4,
        funFact = "Albert Speer, der Architekt des Nurnberger Parteitagsgelaendes, gab zu, mit Riefenstahl ueber optimale Kamerastandpunkte gesprochen zu haben. In seiner Autobiografie 'Erinnerungen' beschreibt er die Symbiosé zwischen Filmproduktion und Parteitagsinszenierung."
    ),

    // Question 48 - Essay Film: Chris Marker
    Question(
        categoryId = 4,
        questionText = "Chris Markers 'Sans Soleil' (1982) gilt als Prototyp des Essay-Films. Welches formale Mittel ist charakteristisch fuer den Essay-Film als Gattung, das ihn von klassischen Dokumentarfilmen unterscheidet?",
        answerA = "Ausschliesslich found footage ohne eigene Kameraaufnahmen",
        answerB = "Eine subjektive Ich-Stimme, die ueber Bilder reflektiert statt sie zu erklaeren",
        answerC = "Das Fehlen von jeglichem Kommentar",
        answerD = "Ausschliesslich Interviewformate ohne Bild-Argument",
        correctAnswer = 1, // B
        explanation = "Der Essay-Film zeichnet sich durch eine subjektive, reflektierende Erzaehlstimme aus, die nicht erklaert, sondern assoziiert, zweifelt und argumentiert. In 'Sans Soleil' kommentiert eine fiktive Briefeschreiberin Filmaufnahmen aus Japan und Afrika - Denken als Narration.",
        difficulty = 4,
        funFact = "Markers Essay-Film beeinflusste Generationen von Filmemachern von Agnies Varda bis Harun Farocki. Markers Anonymitaet (er veroeffentlichte fast nie Fotos von sich) und sein Pseudonym (buergerlicher Name: Christian Hippolyte Francois Georges Bouche-Villeneuve) sind Legenden der Filmgeschichte."
    ),

    // --- PILOTFOLGE UND SERIENENTWICKLUNG ---

    // Question 49 - Tatort: Produktionsmodell
    Question(
        categoryId = 4,
        questionText = "Das 'Tatort'-Produktionsmodell der ARD (seit 1970) ist weltweit einmalig. Was macht es aus produktionsorganisatorischer Sicht so singullaer?",
        answerA = "Jede Folge wird von einem anderen unabhaengigen Produktionsunternehmen hergestellt",
        answerB = "Verschiedene ARD-Landesanstalten produzieren eigenstaendige Ermittlerteams mit gemeinsamer Dachmarke",
        answerC = "Der Tatort wird als Live-TV-Event ohne Nachbearbeitung ausgestrahlt",
        answerD = "Ermittlerfiguren werden regelmaessig nach demokratischer Zuschauerwahl ausgetauscht",
        correctAnswer = 1, // B
        explanation = "Der Tatort ist ein einzigartiges Konstrukt: Verschiedene ARD-Landesrundfunkanstalten (BR, WDR, NDR, SWR etc.) produzieren eigenstaendige Ermittlerteams in verschiedenen deutschen Staedten unter derselben Marke 'Tatort'. Dies reflektiert die foederale Struktur des deutschen Rundfunks.",
        difficulty = 4,
        funFact = "Derzeit gibt es ueber 20 aktive Tatort-Ermittlerteams. Einige sind seit Jahrzehnten aktiv (z.B. Muenster mit Thiel und Boerne seit 2002), andere wurden nach wenigen Folgen wieder eingestellt. Die Gesamtzahl der Tatort-Folgen ueberschritt 2021 die 1.200-Episoden-Marke."
    ),

    // Question 50 - Cold Open: Deutsches TV
    Question(
        categoryId = 4,
        questionText = "Welche deutsche Krimiserie adaptierte als eine der ersten das Cold-Open-Prinzip aus dem amerikanischen TV und wurde damit stilpraegend fuer das deutsche Serienformat ab den 2000ern?",
        answerA = "Alarm fuer Cobra 11",
        answerB = "Derrick",
        answerC = "Der Kommissar",
        answerD = "Stromberg",
        correctAnswer = 0, // A
        explanation = "'Alarm fuer Cobra 11' (RTL, ab 1996) uebernahm das Cold-Open-Prinzip konsequent: Jede Folge beginnt mit einer oft spektakulaeren Action-Sequenz vor dem Titelvorspann. Die Show adaptierte explizit das amerikanische Action-TV-Format und war damit eine der ersten deutschen Serien mit systematischen Cold Opens.",
        difficulty = 4,
        funFact = "'Alarm fuer Cobra 11' gilt als eine der exporterfolgreichen deutschen TV-Serien. Sie lief in ueber 100 Laendern und wurde in mehreren Laendern als Franchise adaptiert. Die Show ist bekannt fuer ihren exzessiven Einsatz von echten Fahrzeugexplosionen ohne CGI."
    )

)
