package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun logicQuestionsExpert3(): List<Question> = listOf(

    // Algorithms, Optimization Problems, Complexity Theory — 50 questions (difficulty 4)

    // ── SORTING ALGORITHMS (1–10) ────────────────────────────────────────────

    Question(
        categoryId = 12,
        questionText = "Bubblesort hat im schlechtesten Fall eine Laufzeit von O(n²). In welchem Fall ist Bubblesort dennoch besser als Quicksort?",
        answerA = "Bei sehr langen, zufälligen Listen",
        answerB = "Bei fast sortierten Listen (nur wenige Vertauschungen nötig)",
        answerC = "Bei Listen mit doppelten Elementen",
        answerD = "Bubblesort ist niemals besser als Quicksort",
        correctAnswer = 1,
        explanation = "Optimierter Bubblesort erkennt, wenn kein Tausch mehr nötig war, und bricht ab. Bei fast sortierten Eingaben erreicht er O(n) — Quicksort hingegen hat im Worst Case O(n²) bei fast sortierten Daten, wenn immer der erste/letzte als Pivot gewählt wird.",
        difficulty = 4,
        funFact = "Timsort (Python, Java 8+) nutzt genau diese Eigenschaft: Er erkennt bereits sortierte Teilfolgen ('runs') und verbindet sie mit Mergesort — im besten Fall O(n)."
    ),

    Question(
        categoryId = 12,
        questionText = "Mergesort hat garantiert O(n log n) Laufzeit. Welcher entscheidende Nachteil gegenüber Quicksort rechtfertigt trotzdem dessen häufigere Verwendung in der Praxis?",
        answerA = "Mergesort ist nicht stabil",
        answerB = "Mergesort benötigt O(n) zusätzlichen Speicherplatz",
        answerC = "Mergesort kann keine negativen Zahlen sortieren",
        answerD = "Mergesort funktioniert nur bei geraden Listengrößen",
        correctAnswer = 1,
        explanation = "Mergesort benötigt beim Zusammenführen (Merge-Schritt) ein Hilfsarray der Größe O(n). Quicksort arbeitet in-place mit O(log n) Stack-Speicher. Bei großen Datensätzen ist dieser Speicherunterschied entscheidend — deshalb bevorzugen viele Standardbibliotheken Quicksort oder hybride Varianten.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Heapsort hat O(n log n) im Worst Case und ist in-place. Warum ist er in der Praxis trotzdem oft langsamer als Quicksort?",
        answerA = "Heapsort hat einen höheren konstanten Faktor wegen schlechter Cache-Lokalität",
        answerB = "Heapsort kann nur ganze Zahlen sortieren",
        answerC = "Heapsort benötigt mehr Vergleiche als Bubblesort",
        answerD = "Heapsort ist nicht deterministisch",
        correctAnswer = 0,
        explanation = "Heapsort springt im Speicher weit umher (Heap-Struktur: Eltern und Kinder liegen nicht nebeneinander). Moderne CPUs holen Daten in Cache-Lines — Heapsorts Zugriffsmuster verursacht viele Cache-Misses. Quicksort dagegen arbeitet fast immer auf zusammenhängenden Speicherbereichen.",
        difficulty = 4,
        funFact = "Cache-Lokalität ist oft wichtiger als asymptotische Komplexität: Ein O(n²)-Algorithmus mit perfekter Lokalität kann einen O(n log n)-Algorithmus bei kleinen n deutlich schlagen."
    ),

    Question(
        categoryId = 12,
        questionText = "Countingsort sortiert n Zahlen im Bereich [0, k] in O(n + k). Wann ist Countingsort KEINE gute Wahl?",
        answerA = "Wenn k sehr groß ist (z. B. k = n²)",
        answerB = "Wenn die Liste bereits sortiert ist",
        answerC = "Wenn n gerade ist",
        answerD = "Wenn die Zahlen positiv sind",
        correctAnswer = 0,
        explanation = "Countingsort braucht ein Hilfsarray der Größe k+1. Wenn k = n² ist, werden O(n²) Speicher und Zeit allein für das Initialisieren des Hilfsarrays benötigt — dann ist O(n log n) von Mergesort deutlich besser. Countingsort glänzt nur wenn k ≈ O(n).",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Radixsort sortiert n Zahlen mit d Ziffern (Basis b) in O(d·(n+b)). Welche Aussage über die Stabilität von Radixsort ist korrekt?",
        answerA = "Radixsort muss intern einen instabilen Algorithmus nutzen",
        answerB = "Radixsort muss intern einen stabilen Algorithmus nutzen — sonst ist das Ergebnis falsch",
        answerC = "Radixsort ist immer instabil",
        answerD = "Stabilität spielt bei Radixsort keine Rolle",
        correctAnswer = 1,
        explanation = "Radixsort sortiert ziffernweise von rechts nach links (LSD) oder links nach rechts (MSD). Beim nächsten Durchlauf müssen die relative Reihenfolge gleicher Ziffern erhalten bleiben — sonst würden vorherige Sortierungen zunichte gemacht. Countingsort (stabil) wird deshalb als interner Algorithmus genutzt.",
        difficulty = 4,
        funFact = "Radixsort wurde bereits 1887 für Lochkartensortiermaschinen entwickelt — lange vor dem Computer-Zeitalter."
    ),

    Question(
        categoryId = 12,
        questionText = "Der untere Bound für vergleichsbasiertes Sortieren ist Ω(n log n). Wie wird dieser Beweis geführt?",
        answerA = "Durch Widerspruch mit dem Pigeonhole-Prinzip",
        answerB = "Durch das Entscheidungsbaum-Argument: Ein binärer Baum für n! Permutationen hat Tiefe ≥ log₂(n!)",
        answerC = "Durch Reduktion auf das Matrizenmultiplikationsproblem",
        answerD = "Durch die Master-Methode",
        correctAnswer = 1,
        explanation = "Jeder vergleichsbasierte Sortieralgorithmus entspricht einem binären Entscheidungsbaum. Für n Elemente gibt es n! Permutationen — alle müssen als Blatt erreichbar sein. Ein Baum mit n! Blättern hat Höhe ≥ log₂(n!) ≈ n log n (Stirling-Näherung). Dies beweist, dass kein vergleichsbasierter Algorithmus besser als O(n log n) sein kann.",
        difficulty = 4,
        funFact = "Countingsort und Radixsort umgehen diese Schranke, weil sie KEINE Vergleiche nutzen — sie exploitieren die Struktur der Daten."
    ),

    Question(
        categoryId = 12,
        questionText = "Quicksort wählt ein Pivot-Element. Welche Pivot-Strategie garantiert im Worst Case O(n log n)?",
        answerA = "Immer das erste Element",
        answerB = "Immer das mittlere Element",
        answerC = "Median-of-Medians (Blum-Floyd-Pratt-Rivest-Tarjan-Algorithmus)",
        answerD = "Zufälliges Pivot",
        correctAnswer = 2,
        explanation = "Der Median-of-Medians-Algorithmus findet in O(n) einen Pivot, der die Liste garantiert im Verhältnis mindestens 30:70 teilt. Damit ist Quicksort mit diesem Pivot im Worst Case O(n log n). In der Praxis wird er kaum eingesetzt — zu hohe konstante Faktoren. Zufälliges Pivot liefert nur erwartete O(n log n).",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Bei Insertionsort mit binärer Suche nach der Einfügeposition: Wie viele Vergleiche und Verschiebungen werden im schlechtesten Fall benötigt?",
        answerA = "O(n log n) Vergleiche und O(n log n) Verschiebungen",
        answerB = "O(n log n) Vergleiche, aber O(n²) Verschiebungen",
        answerC = "O(n²) Vergleiche und O(n²) Verschiebungen",
        answerD = "O(n) Vergleiche und O(n) Verschiebungen",
        correctAnswer = 1,
        explanation = "Binäre Suche findet die Einfügeposition in O(log i) für das i-te Element: Gesamt O(n log n) Vergleiche. Aber: Das Verschieben der Elemente nach rechts ist O(i) — über alle Elemente O(n²). Die Gesamtlaufzeit bleibt O(n²). Binary Insertionsort verbessert nur die Anzahl der Vergleiche, nicht die Verschiebungen.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Shellsort verbessert Insertionsort durch 'h-Sortierschritte'. Welche Spaltfolge (gap sequence) liefert die beste bekannte Worst-Case-Laufzeit?",
        answerA = "Shells Originalfolge: n/2, n/4, ..., 1 → O(n²)",
        answerB = "Hibbards Folge: 1, 3, 7, 15, ... → O(n^(3/2))",
        answerC = "Sedgewicks Folge → O(n^(4/3))",
        answerD = "Pratts Folge: alle 2^i × 3^j → O(n log² n)",
        correctAnswer = 3,
        explanation = "Pratts Folge (1971) liefert O(n log² n) im Worst Case — die beste beweisbare Schranke für Shellsort. Sie enthält alle Zahlen der Form 2^i × 3^j und ist deshalb sehr dicht. Sedgewicks Folge ist empirisch schneller, aber O(n^(4/3)) im Worst Case. Die optimale Spaltfolge für Shellsort ist bis heute ein offenes Problem.",
        difficulty = 4,
        funFact = "Shellsort ist nach Donald Shell benannt, der ihn 1959 veröffentlichte — einer der ersten Algorithmen, der die Laufzeit von O(n²) durchbrach."
    ),

    Question(
        categoryId = 12,
        questionText = "Ein externer Sortieralgorithmus für Daten, die nicht in den RAM passen, verwendet Merge-Passes. Wie viele Passes benötigt External Mergesort für n Blöcke mit je M Speicherblöcken RAM?",
        answerA = "O(n/M)",
        answerB = "O(log_{M}(n/M)) Passes",
        answerC = "O(n log n / M)",
        answerD = "O(n²/M)",
        correctAnswer = 1,
        explanation = "External Mergesort erstellt zunächst ⌈n/M⌉ sortierte Runs. Dann werden diese in log_{M}(n/M) Runden zu immer größeren Runs zusammengefasst, da man mit M Speicherblöcken M-Wege-Merge durchführen kann. Mit mehr RAM braucht man weniger I/O-Passes — entscheidend für Datenbankensysteme.",
        difficulty = 4,
        funFact = null
    ),

    // ── GRAPH ALGORITHMS (11–20) ─────────────────────────────────────────────

    Question(
        categoryId = 12,
        questionText = "Dijkstras Algorithmus findet kürzeste Wege in gewichteten Graphen. Warum schlägt er bei negativen Kantengewichten fehl?",
        answerA = "Er kann keine Graphen mit mehr als 1000 Knoten verarbeiten",
        answerB = "Er greedy wählt den momentan kürzesten Knoten — negative Kanten können diesen später noch kürzer machen",
        answerC = "Er verwendet zu viel Speicher bei negativen Gewichten",
        answerD = "Negative Kantengewichte sind grundsätzlich unmöglich",
        correctAnswer = 1,
        explanation = "Dijkstra markiert besuchte Knoten als 'endgültig'. Mit negativen Gewichten kann ein später entdeckter Pfad über eine negative Kante kürzer sein als ein bereits 'endgültig' markierter Pfad. Dijkstra würde diesen nicht mehr überprüfen. Bellman-Ford löst dieses Problem durch n-1 Relaxierungsrunden für alle Kanten.",
        difficulty = 4,
        funFact = "Edsger Dijkstra entwickelte diesen Algorithmus 1956 in etwa 20 Minuten — und zwar ohne Papier und Bleistift: 'Ich hatte keine Schreibmaschine dabei', sagte er später."
    ),

    Question(
        categoryId = 12,
        questionText = "Bellman-Ford erkennt negative Zyklen. Wie erkennt er sie, und was ist die Laufzeit?",
        answerA = "Durch DFS in O(V)",
        answerB = "Nach V-1 Relaxierungen: Wenn in der V-ten Runde noch eine Relaxierung möglich ist, existiert ein negativer Zyklus. Laufzeit O(V·E)",
        answerC = "Durch Vergleich aller Kantengewichte in O(E log E)",
        answerD = "Durch Anwendung von Dijkstra mit Negation aller Gewichte in O(V log V)",
        correctAnswer = 1,
        explanation = "Bellman-Ford relaxiert in jeder der V-1 Runden alle E Kanten. Nach V-1 Runden sind alle kürzesten Wege ohne negative Zyklen korrekt. Eine V-te Runde prüft, ob noch Verbesserungen möglich sind — das wäre nur bei einem negativen Zyklus möglich (unendlich kurze Wege). Laufzeit: O(V·E).",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Floyd-Warshall berechnet alle kürzesten Wege zwischen je zwei Knoten. Was ist seine Laufzeit und welche Kernidee steckt dahinter?",
        answerA = "O(V²) durch dynamische Programmierung über Adjazenzmatrizen",
        answerB = "O(V³) durch dynamische Programmierung: dp[i][j][k] = kürzester Weg von i nach j über Zwischenknoten aus {1,...,k}",
        answerC = "O(V² log V) durch parallele Dijkstra-Aufrufe",
        answerD = "O(V·E) durch wiederholten Bellman-Ford",
        correctAnswer = 1,
        explanation = "Floyd-Warshall nutzt DP: dp[i][j][k] = min(dp[i][j][k-1], dp[i][k][k-1] + dp[k][j][k-1]). Für jeden Knoten k prüft man, ob ein Umweg über k den direkten Weg verkürzt. Nach V Iterationen sind alle kürzesten Wege bekannt. Laufzeit O(V³), Speicher O(V²).",
        difficulty = 4,
        funFact = "Floyd-Warshall kann durch Vorzeichenprüfung der Diagonale negative Zyklen erkennen: dp[i][i] < 0 nach dem Algorithmus bedeutet, i liegt auf einem negativen Zyklus."
    ),

    Question(
        categoryId = 12,
        questionText = "Prims und Kruskals Algorithmus berechnen beide minimale Spannbäume. Was ist der Hauptunterschied?",
        answerA = "Prim funktioniert nur auf gerichteten Graphen, Kruskal auf ungerichteten",
        answerB = "Prim wächst einen einzelnen Baum von einem Startknoten aus; Kruskal sortiert alle Kanten und verbindet Komponenten",
        answerC = "Kruskal ist immer schneller als Prim",
        answerD = "Prim findet kürzeste Wege, Kruskal minimale Spannbäume",
        correctAnswer = 1,
        explanation = "Prim (ähnlich Dijkstra): Startet bei einem Knoten, fügt immer die billigste Kante zum wachsenden Baum hinzu — O(E log V) mit Fibonacci-Heap. Kruskal: Sortiert alle Kanten nach Gewicht, fügt sie hinzu (solange kein Zyklus entsteht, prüft mit Union-Find) — O(E log E). Bei dichten Graphen ist Prim meist besser.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Topologische Sortierung ist nur auf gerichteten azyklischen Graphen (DAGs) möglich. Welcher Algorithmus findet die topologische Reihenfolge und in welcher Laufzeit?",
        answerA = "BFS in O(V²)",
        answerB = "Kahns Algorithmus (BFS mit Eingrad-0-Queue) oder DFS mit Postorder-Ausgabe — beide O(V + E)",
        answerC = "Bellman-Ford in O(V·E)",
        answerD = "Dijkstra in O(V log V)",
        correctAnswer = 1,
        explanation = "Kahn: Starte alle Knoten mit Eingrad 0 in eine Queue. Entferne Knoten, reduziere Nachbars-Eingrade, füge neue Nullen ein. DFS: Tiefensuche, gib Knoten nach vollständiger Rekursion aus — in umgekehrter Reihenfolge ergibt sich die topologische Sortierung. Beide O(V + E). Existiert kein Zyklus ↔ Topologische Sortierung existiert.",
        difficulty = 4,
        funFact = "Topologische Sortierung ist der Kern von Build-Systemen wie Make oder Gradle — Abhängigkeiten müssen vor den abhängigen Paketen gebaut werden."
    ),

    Question(
        categoryId = 12,
        questionText = "Tarjans Algorithmus findet stark zusammenhängende Komponenten (SCCs) in einem Graphen. Was ist seine Laufzeit?",
        answerA = "O(V² + E)",
        answerB = "O(V + E) — linear in der Graphgröße",
        answerC = "O(V log V + E)",
        answerD = "O(V · E)",
        correctAnswer = 1,
        explanation = "Tarjans Algorithmus führt eine DFS durch und verwendet einen Stack sowie 'discovery time' und 'low values' für jeden Knoten. Eine SCC wird ausgegeben, wenn ein Knoten der 'Wurzel' seiner Komponente ist (low[v] == disc[v]). Alles in einem einzigen DFS-Durchlauf: O(V + E). Kosarajus Algorithmus braucht zwei DFS-Durchläufe, ist aber einfacher zu implementieren.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Der A*-Algorithmus ist eine Erweiterung von Dijkstra. Was ist die Bedeutung der Heuristikfunktion h(n)?",
        answerA = "h(n) schätzt die Kosten vom Startknoten zu n",
        answerB = "h(n) schätzt die restlichen Kosten von n zum Ziel — muss zulässig sein (nie überschätzen), damit A* optimal ist",
        answerC = "h(n) gibt die genauen Restkosten zum Ziel an",
        answerD = "h(n) gewichtet die bisherigen Kosten",
        correctAnswer = 1,
        explanation = "A* bewertet jeden Knoten mit f(n) = g(n) + h(n), wobei g(n) die tatsächlichen Kosten vom Start und h(n) eine Schätzung der Restkosten ist. Zulässigkeit (h(n) ≤ echte Restkosten) garantiert Optimalität. Konsistenz (h(n) ≤ c(n,m) + h(m)) garantiert, dass kein Knoten zweimal besucht wird. Dijkstra ist A* mit h(n) = 0.",
        difficulty = 4,
        funFact = "A* wurde 1968 von Peter Hart, Nils Nilsson und Bertram Raphael am Stanford Research Institute entwickelt und ist heute der Kern aller GPS-Navigationssysteme."
    ),

    Question(
        categoryId = 12,
        questionText = "Max-Flow Min-Cut Theorem: Was besagt es, und welcher Algorithmus nutzt es direkt?",
        answerA = "Der maximale Fluss eines Netzwerks entspricht dem minimalen Schnitt — Ford-Fulkerson nutzt es",
        answerB = "Der minimale Fluss entspricht dem maximalen Schnitt — Dijkstra nutzt es",
        answerC = "Jeder maximale Fluss ist gleich n² — Bellman-Ford nutzt es",
        answerD = "Schnitte und Flüsse sind unabhängig voneinander",
        correctAnswer = 0,
        explanation = "Max-Flow Min-Cut: Der maximale Wert eines s-t-Flusses entspricht der minimalen Kapazität aller s-t-Schnitte. Ford-Fulkerson findet augmentierende Pfade im Residualgraph, bis keiner mehr existiert — dann ist ein Minimum-Cut gefunden. Edmonds-Karp (BFS-Variante) läuft in O(V·E²), Dinic in O(V²·E).",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Breadth-First Search (BFS) auf einem ungewichteten Graphen findet kürzeste Wege in O(V+E). Warum funktioniert Tiefensuche (DFS) dafür nicht?",
        answerA = "DFS ist langsamer als BFS",
        answerB = "DFS erkundet erst alle Wege in einer Richtung vollständig — dabei kann der erste gefundene Weg viel länger sein als der kürzeste",
        answerC = "DFS kann keine Graphen mit Zyklen verarbeiten",
        answerD = "DFS braucht O(V²) Speicher",
        correctAnswer = 1,
        explanation = "BFS erkundet schichtweise: Zuerst alle Knoten in Abstand 1, dann 2, usw. Damit ist der erste gefundene Pfad zum Ziel garantiert der kürzeste. DFS geht tief in einen Ast — der erste gefundene Pfad kann beliebig lang sein. DFS ist optimal für Erreichbarkeit, Zyklen, topologische Sortierung — nicht für kürzeste Wege.",
        difficulty = 4,
        funFact = "BFS wurde erstmals 1959 von Edward Moore zur Lösung von Labyrinthen beschrieben — unter dem Namen 'shortest path in a maze'."
    ),

    Question(
        categoryId = 12,
        questionText = "Das Problem der bipartiten Zuordnung (Maximum Matching) kann mit dem Hopcroft-Karp-Algorithmus gelöst werden. Was ist seine Laufzeit?",
        answerA = "O(V · E)",
        answerB = "O(√V · E)",
        answerC = "O(V² + E)",
        answerD = "O(E log V)",
        correctAnswer = 1,
        explanation = "Hopcroft-Karp findet in O(√V) Phasen ein maximales Matching in bipartiten Graphen. In jeder Phase werden augmentierende Pfade der kürzesten Länge gleichzeitig gefunden (BFS + DFS). Jede Phase dauert O(E). Gesamtlaufzeit: O(√V · E). Für dichte Graphen (E = V²) also O(V^(5/2)).",
        difficulty = 4,
        funFact = null
    ),

    // ── COMPLEXITY THEORY (21–30) ────────────────────────────────────────────

    Question(
        categoryId = 12,
        questionText = "Was bedeutet es, wenn ein Problem NP-vollständig ist?",
        answerA = "Das Problem ist unlösbar",
        answerB = "Das Problem ist in NP und jedes NP-Problem kann in polynomieller Zeit darauf reduziert werden",
        answerC = "Das Problem hat genau n Lösungen",
        answerD = "Das Problem benötigt exponentielle Speicherkapazität",
        correctAnswer = 1,
        explanation = "NP-vollständig (NPC) bedeutet: (1) Das Problem liegt in NP (eine Lösung kann in polynomieller Zeit verifiziert werden), und (2) jedes NP-Problem kann in polynomieller Zeit auf dieses Problem reduziert werden (NP-schwer). Könnte man ein NP-vollständiges Problem effizient lösen, wären alle NP-Probleme effizient lösbar — dann wäre P = NP.",
        difficulty = 4,
        funFact = "Stephen Cook bewies 1971, dass SAT (Erfüllbarkeitsproblem der Aussagenlogik) NP-vollständig ist — dies war der erste NP-Vollständigkeitsbeweis (Cook-Levin-Theorem)."
    ),

    Question(
        categoryId = 12,
        questionText = "Das P-NP-Problem ist eines der Millennium-Probleme. Was ist die Vermutung der meisten Informatiker?",
        answerA = "P = NP wurde bereits bewiesen",
        answerB = "P ≠ NP — die meisten Forscher glauben, dass effizient lösbare und effizient verifizierbare Probleme zwei verschiedene Klassen bilden",
        answerC = "P = NP für alle praktischen Zwecke",
        answerD = "Die Frage ist bedeutungslos für die Praxis",
        correctAnswer = 1,
        explanation = "Das Clay Mathematics Institute bietet 1 Million Dollar Belohnung. Über 95% der Theoretiker vermuten P ≠ NP, weil kein polynomieller Algorithmus für NP-vollständige Probleme gefunden wurde. Würde P = NP gelten, würden Kryptosysteme wie RSA zusammenbrechen — Public-Key-Kryptographie basiert auf der Annahme, dass bestimmte Probleme NP-schwer sind.",
        difficulty = 4,
        funFact = "Scott Aaronson schrieb: 'Wenn jemand beweist, dass P = NP, würden Mathematiker arbeitslos — denn dann könnte ein Computer alle mathematischen Beweise selbst finden.'"
    ),

    Question(
        categoryId = 12,
        questionText = "Big-O-Notation: Welche der folgenden Aussagen ist korrekt?",
        answerA = "O(n²) ist schneller als O(n log n) für große n",
        answerB = "f(n) = O(g(n)) bedeutet: Es gibt c > 0 und n₀, sodass f(n) ≤ c·g(n) für alle n ≥ n₀",
        answerC = "O(1) bedeutet, der Algorithmus braucht genau eine Operation",
        answerD = "O(n) und Θ(n) sind immer identisch",
        correctAnswer = 1,
        explanation = "O(g(n)) beschreibt eine obere Schranke: f wächst höchstens so schnell wie g (bis auf konstante Faktoren). Θ(g(n)) ist eine enge Schranke (oben UND unten). O(1) bedeutet konstante Laufzeit, nicht genau 1 Operation — eine O(1)-Operation könnte 10⁶ Schritte dauern. O(n log n) ist asymptotisch besser als O(n²).",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Die Komplexitätsklasse PSPACE umfasst Probleme, die polynomiell viel Speicher benötigen. Welche Beziehung gilt zwischen P, NP und PSPACE?",
        answerA = "P = NP = PSPACE",
        answerB = "P ⊆ NP ⊆ PSPACE — ob die Inklusionen echt sind, ist größtenteils offen",
        answerC = "PSPACE ⊆ NP ⊆ P",
        answerD = "P und PSPACE sind disjunkt",
        correctAnswer = 1,
        explanation = "Es gilt P ⊆ NP ⊆ PSPACE ⊆ EXPTIME. P ⊆ NP: Jede deterministisch polynomielle Lösung ist auch nichtdeterministisch polynomielle. NP ⊆ PSPACE: Ein polynomieller Verifikationsalgorithmus braucht polynomiellen Platz. Nur PSPACE ⊊ EXPTIME ist bewiesen (Platzhierarchiesatz). Ob P ⊊ NP und NP ⊊ PSPACE gilt, ist offen.",
        difficulty = 4,
        funFact = "PSPACE-vollständige Probleme schließen viele Spiele ein: Schach auf n×n-Brett, Go, Reversi — alle sind PSPACE-vollständig."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist die Amortisierte Analyse und warum ist sie für die Bewertung von Datenstrukturen wichtig?",
        answerA = "Sie analysiert den besten Fall eines Algorithmus",
        answerB = "Sie berechnet den durchschnittlichen Aufwand über eine Folge von Operationen — auch wenn einzelne Operationen teuer sein können",
        answerC = "Sie ist dasselbe wie die Average-Case-Analyse",
        answerD = "Sie gilt nur für Sortieralgorithmen",
        correctAnswer = 1,
        explanation = "Amortisierte Analyse: Wenn eine seltene teure Operation durch viele billige Operationen 'bezahlt' wird, ist der amortisierte Aufwand pro Operation gering. Beispiel: Dynamisches Array — append() ist meist O(1), manchmal O(n) (Verdopplung). Amortisiert: O(1) pro Operation. Methoden: Aggregat, Bankkonto (Accounting), Potential-Methode.",
        difficulty = 4,
        funFact = "Der Begriff 'amortisiert' kommt aus der Finanzmathematik: Schulden werden über viele Zahlungen getilgt — analog werden 'teure' Operationen über viele billige verteilt."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist der Unterschied zwischen Entscheidungsproblemen und Optimierungsproblemen in der Komplexitätstheorie?",
        answerA = "Optimierungsprobleme sind immer in P, Entscheidungsprobleme in NP",
        answerB = "Entscheidungsprobleme haben Ja/Nein-Antworten; ihr zugehöriges Optimierungsproblem ist mindestens so schwer",
        answerC = "Beide Klassen sind identisch",
        answerD = "Entscheidungsprobleme sind immer einfacher als Optimierungsprobleme",
        correctAnswer = 1,
        explanation = "Entscheidungsprobleme fragen: 'Gibt es eine Lösung mit Kosten ≤ k?' (Ja/Nein). Das zugehörige Optimierungsproblem fragt: 'Was ist die minimale Lösung?' NP-Vollständigkeit ist für Entscheidungsprobleme definiert. Das Optimierungsproblem ist mindestens so schwer: Könnte man es effizient lösen, ließe sich auch das Entscheidungsproblem effizient beantworten (binäre Suche über k).",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Randomisierte Algorithmen werden in zwei Klassen eingeteilt. Was ist der Unterschied zwischen Las-Vegas- und Monte-Carlo-Algorithmen?",
        answerA = "Las-Vegas ist immer korrekt, Monte-Carlo ist schneller",
        answerB = "Las-Vegas liefert immer eine korrekte Antwort (variable Laufzeit); Monte-Carlo hat fixe Laufzeit, aber kann falsch liegen",
        answerC = "Beide liefern immer korrekte Ergebnisse",
        answerD = "Monte-Carlo ist nur für numerische Probleme geeignet",
        correctAnswer = 1,
        explanation = "Las-Vegas (z.B. randomisierter Quicksort): Immer korrekt, erwartete Laufzeit O(n log n). Monte-Carlo (z.B. Miller-Rabin-Primtest): Läuft in fixer O(k log² n) Zeit, aber kann mit Wahrscheinlichkeit 4^(-k) einen Fehler machen. Durch Wiederholung kann man die Fehlerwahrscheinlichkeit beliebig klein machen.",
        difficulty = 4,
        funFact = "Randomisierter Quicksort hat erwartete O(n log n) Laufzeit — der Erwartungswert gilt über die Zufallswahl, nicht über die Eingabe. Kein Angreifer kann bewusst den schlechten Fall provozieren."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist der Master-Satz und wann ist er anwendbar?",
        answerA = "Er löst alle Rekursionsgleichungen in O(1)",
        answerB = "Er löst Rekursionsgleichungen der Form T(n) = a·T(n/b) + f(n) in drei Fällen je nach Verhältnis von f(n) zu n^(log_b a)",
        answerC = "Er gilt nur für Sortieralgorithmen",
        answerD = "Er bestimmt den maximalen Fluss in Netzwerken",
        correctAnswer = 1,
        explanation = "Master-Satz für T(n) = a·T(n/b) + f(n): Fall 1: f(n) = O(n^(log_b a - ε)) → T(n) = Θ(n^(log_b a)). Fall 2: f(n) = Θ(n^(log_b a)) → T(n) = Θ(n^(log_b a) · log n). Fall 3: f(n) = Ω(n^(log_b a + ε)) und Regularitätsbedingung → T(n) = Θ(f(n)). Mergesort: T(n) = 2T(n/2) + O(n) → Fall 2 → O(n log n).",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Approximationsalgorithmen: Was ist ein 2-Approximationsalgorithmus für das metrische TSP?",
        answerA = "Er findet immer die optimale Tour",
        answerB = "Er findet eine Tour, die höchstens doppelt so lang ist wie die optimale — basierend auf einem minimalen Spannbaum",
        answerC = "Er löst das Problem in O(n²)",
        answerD = "Er findet eine 2-optimale Tour durch lokale Suche",
        correctAnswer = 1,
        explanation = "Christofides-ähnlicher 2-Approximationsalgorithmus: (1) Berechne minimalen Spannbaum T (Kosten ≤ OPT). (2) Verdopple alle Kanten → Eulerscher Graph (Kosten ≤ 2·OPT). (3) Entferne Wiederholungsknoten (Dreiecksungleichung). Ergebnis: Tour mit Kosten ≤ 2·OPT. Christofides (1976) erreicht 3/2-Approximation durch perfektes Matching auf ungeraden Knoten.",
        difficulty = 4,
        funFact = "Für das allgemeine TSP (ohne Dreiecksungleichung) gibt es keinen konstanten Approximationsalgorithmus — es sei denn P = NP."
    ),

    Question(
        categoryId = 12,
        questionText = "Dynamische Programmierung vs. Greedy: Wann ist ein Greedy-Ansatz optimal?",
        answerA = "Greedy ist immer optimal wenn die Eingabe sortiert ist",
        answerB = "Greedy ist optimal wenn das Problem die Greedy-Choice-Property und optimale Teilstruktur hat",
        answerC = "Greedy ist nie optimal für NP-schwere Probleme",
        answerD = "Greedy ist optimal wenn alle Gewichte ganzzahlig sind",
        correctAnswer = 1,
        explanation = "Greedy funktioniert optimal wenn: (1) Greedy-Choice-Property: Eine global optimale Lösung kann durch lokale optimale Entscheidungen gebaut werden. (2) Optimale Teilstruktur: Teilprobleme der optimalen Lösung sind selbst optimal. Beispiele: Huffman-Kodierung, minimale Spannbäume (Kruskal/Prim), Aktivitätsauswahl. Rucksackproblem (0/1) erfüllt nur (2) nicht (1) — braucht DP.",
        difficulty = 4,
        funFact = "Das fraktionale Rucksackproblem (Gegenstände können geteilt werden) ist durch Greedy optimal lösbar — das 0/1-Rucksackproblem (ganz oder gar nicht) ist NP-vollständig."
    ),

    // ── OPTIMIZATION PROBLEMS (31–40) ────────────────────────────────────────

    Question(
        categoryId = 12,
        questionText = "Das Travelling-Salesman-Problem (TSP): Bei n Städten, wie viele mögliche Rundtouren gibt es (ohne Richtungsumkehr)?",
        answerA = "n!",
        answerB = "(n-1)! / 2",
        answerC = "2^n",
        answerD = "n²",
        correctAnswer = 1,
        explanation = "Die erste Stadt kann fixiert werden (Symmetrie der Rundtour): (n-1)! Permutationen der restlichen Städte. Jede Tour und ihre Umkehrung sind identisch → teile durch 2: (n-1)!/2. Für n=20: 60.822.550.204.416.000 Touren. Das exponentielle Wachstum macht Brute-Force für n > ~20 unmöglich.",
        difficulty = 4,
        funFact = "Die längste je mit exakten Methoden gelöste TSP-Instanz hatte 85.900 Punkte (USA-Städteinstanz, 2006) — gelöst mit Cutting-Plane-Methoden über Monate Rechenzeit."
    ),

    Question(
        categoryId = 12,
        questionText = "Das 0/1-Rucksackproblem: Welche Lösung findet der Greedy-Ansatz (Sortierung nach Wert/Gewicht-Verhältnis) und wann ist sie optimal?",
        answerA = "Der Greedy-Ansatz findet immer die optimale Lösung",
        answerB = "Greedy findet die optimale Lösung NUR für das fraktionale Rucksackproblem — beim 0/1-Problem kann er beliebig schlecht sein",
        answerC = "Greedy findet eine 2-Approximation beim 0/1-Problem",
        answerD = "Greedy funktioniert nicht für das Rucksackproblem",
        correctAnswer = 1,
        explanation = "Gegenbeispiel für Greedy beim 0/1-Rucksack: Kapazität 10. Gegenstand A: Gewicht 6, Wert 12 (Verhältnis 2). Gegenstand B: Gewicht 5, Wert 9 (Verhältnis 1,8). Gegenstand C: Gewicht 5, Wert 9. Greedy nimmt A (Verhältnis 2), dann passt nur einer von B/C: Wert 21. Optimal: B+C = 18 passt, aber A+B nicht. Warte: Optimal ist B+C=18 < A+B würde nicht passen. Tatsächlich: A allein = 12, B+C = 18. Greedy versagt hier.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Das Set-Cover-Problem: Eine Menge U mit n Elementen soll durch möglichst wenige Teilmengen S₁,...,Sₘ überdeckt werden. Was garantiert der Greedy-Algorithmus?",
        answerA = "Die optimale Lösung",
        answerB = "Eine ln(n)+1 Approximation der optimalen Lösung — und dieser Faktor ist optimal (unter P≠NP-Annahme)",
        answerC = "Eine 2-Approximation",
        answerD = "Keine Garantie",
        correctAnswer = 1,
        explanation = "Greedy: Wähle immer die Teilmenge, die die meisten noch ungedeckten Elemente abdeckt. Analyse: Nach k Schritten sind höchstens n·(1-1/OPT)^k Elemente ungedeckt. Nach OPT·ln(n) Schritten → alle gedeckt. Hastad (1999) bewiesen: Kein polynomieller Algorithmus kann besser als (1-ε)·ln(n) approximieren — es sei denn P = NP.",
        difficulty = 4,
        funFact = "Set Cover modelliert viele Praxisprobleme: Stationsstandorte im Mobilfunknetz, Notaufnahmen verteilen, Test-Suite-Minimierung in der Softwareentwicklung."
    ),

    Question(
        categoryId = 12,
        questionText = "Das Erfüllbarkeitsproblem (SAT): Eine Konjunktion von Klauseln mit je 3 Literalen (3-SAT) ist NP-vollständig. Was ist MAX-3-SAT?",
        answerA = "Finde eine Belegung, die alle Klauseln erfüllt",
        answerB = "Finde eine Belegung, die möglichst viele Klauseln erfüllt — das Optimierungsproblem zu 3-SAT",
        answerC = "Finde die maximale Anzahl an Variablen",
        answerD = "Finde die kürzeste erfüllende Belegung",
        correctAnswer = 1,
        explanation = "MAX-3-SAT: Optimierungsvariante von 3-SAT — maximiere die Anzahl erfüllter Klauseln. Interessant: Zufällige Belegung erfüllt im Erwartungswert 7/8 aller Klauseln (jede 3-Klausel wird mit Wahrscheinlichkeit 7/8 erfüllt). Hastad (2001) zeigte: Kein polynomieller Algorithmus kann mehr als 7/8 garantieren — zufällige Belegung ist damit optimal! (Unter P ≠ NP).",
        difficulty = 4,
        funFact = "Hastad's Inapproximabilitätsergebnis für MAX-3-SAT gilt als eines der schönsten Ergebnisse der Komplexitätstheorie — die einfachste Strategie ist bereits optimal."
    ),

    Question(
        categoryId = 12,
        questionText = "Lineare Programmierung (LP): Was ist der Simplex-Algorithmus und was ist sein Worst-Case-Verhalten?",
        answerA = "Simplex ist polynomial im Worst Case",
        answerB = "Simplex iteriert über Ecken des zulässigen Polyeders — im Worst Case exponentiell (Klee-Minty-Würfel), aber praktisch sehr schnell",
        answerC = "Simplex ist der schnellste bekannte LP-Algorithmus",
        answerD = "Simplex löst nur 2-dimensionale LPs",
        correctAnswer = 1,
        explanation = "Simplex (Dantzig, 1947) bewegt sich entlang der Kanten des zulässigen Polyeders. Klee-Minty (1972) konstruierte einen Würfel mit 2^n Ecken, den Simplex in exponentieller Zeit durchläuft. Praktisch: Simplex ist extrem schnell. Polynomial: Ellipsoid-Methode (Khachiyan, 1979) und Innere-Punkte-Methode (Karmarkar, 1984) sind polynomial — aber oft langsamer in der Praxis.",
        difficulty = 4,
        funFact = "George Dantzig entwickelte Simplex 1947 während seiner Doktorarbeit an der UC Berkeley. Er präsentierte die Arbeit George Stigler — der sagte: 'Das wirst du nie effizient lösen können.' Dantzig löste es am nächsten Morgen."
    ),

    Question(
        categoryId = 12,
        questionText = "Das Stundenplan-Problem (Job Scheduling) ist NP-vollständig in allgemeiner Form. Was leistet der LPT-Algorithmus (Longest Processing Time)?",
        answerA = "Er findet die optimale Lösung in O(n log n)",
        answerB = "Er sortiert Jobs absteigend nach Dauer und weist sie der aktuell kürzesten Maschine zu — garantiert 4/3 - 1/(3m) Approximation",
        answerC = "Er findet eine optimale Lösung in O(2^n)",
        answerD = "LPT ist nur für 2 Maschinen definiert",
        correctAnswer = 1,
        explanation = "LPT (Graham, 1969): Sortiere Jobs absteigend, weise jeden Job der aktuell am wenigsten belasteten Maschine zu. Für m Maschinen: LPT garantiert einen Makespan von höchstens (4/3 - 1/(3m)) · OPT. Für 2 Maschinen: ≤ 7/6 · OPT. Einfacher FIFO-Greedy garantiert nur 2 · OPT.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Das kürzeste Hamiltonpfad-Problem ist NP-vollständig, aber lösbar mit dynamischer Programmierung. Was ist die Laufzeit des Held-Karp-Algorithmus?",
        answerA = "O(n!)",
        answerB = "O(2^n · n²) — exponentiell, aber viel besser als n!",
        answerC = "O(n³)",
        answerD = "O(n² log n)",
        correctAnswer = 1,
        explanation = "Held-Karp (1962) nutzt DP: dp[S][v] = kürzester Weg, der genau die Knoten in S besucht und bei v endet. Zustandsraum: 2^n Teilmengen × n Endknoten. Übergänge: O(n). Gesamtlaufzeit: O(2^n · n²). Für n=30: ca. 10^12 Operationen — machbar auf Hochleistungsrechnern. Brute Force n! für n=30: 2,65 · 10^32 — unmöglich.",
        difficulty = 4,
        funFact = "Held-Karp ist noch immer einer der besten exakten Algorithmen für TSP — trotz 60 Jahren Forschung wurde die Laufzeit nur auf O(1,9999^n · n²) verbessert (Björklund, 2010)."
    ),

    Question(
        categoryId = 12,
        questionText = "Simulated Annealing ist ein metaheuristisches Optimierungsverfahren. Woher stammt die Analogie, und was ist seine Kernidee?",
        answerA = "Aus der Quantenmechanik — zufällige Tunneleffekte",
        answerB = "Aus der Metallurgie — langsames Abkühlen erlaubt schlechtere Lösungen mit abnehmender Wahrscheinlichkeit, um lokale Minima zu entkommen",
        answerC = "Aus der Biologie — natürliche Selektion",
        answerD = "Aus der Strömungslehre — Flüssigkeitsdynamik",
        correctAnswer = 1,
        explanation = "Simulated Annealing (Kirkpatrick et al., 1983): Analogie zum Tempern von Metall. Bei hoher Temperatur T: Akzeptiere schlechtere Lösungen mit Wahrscheinlichkeit e^(-ΔE/T). Bei sinkender Temperatur nimmt diese Wahrscheinlichkeit ab. Ziel: Entkomme lokalen Minima früh, konvergiere zu globalen Minimum. Konvergiert unter Bedingungen zum globalen Optimum — aber extrem langsam.",
        difficulty = 4,
        funFact = "Die erste Anwendung von Simulated Annealing war das Platzierungsproblem in Computerchips (VLSI-Design) — IBM nutzte es 1983 zur Optimierung von Schaltkreislayouts."
    ),

    Question(
        categoryId = 12,
        questionText = "Genetische Algorithmen imitieren die Evolution. Welche biologischen Operatoren werden verwendet?",
        answerA = "Mutation, Kreuzung (Crossover) und Selektion",
        answerB = "Addition, Multiplikation und Division",
        answerC = "Gradient, Hessian und Jacobian",
        answerD = "DFS, BFS und Dijkstra",
        correctAnswer = 0,
        explanation = "Genetische Algorithmen (Holland, 1975): Selektion — bessere Individuen (Lösungen) werden häufiger für die Reproduktion gewählt (Fitness-proportionale Selektion, Tournament Selection). Kreuzung — zwei Elternlösungen erzeugen durch Kombination Kindlösungen. Mutation — zufällige Änderungen erhalten genetische Diversität und verhindern vorzeitige Konvergenz.",
        difficulty = 4,
        funFact = "NASA nutzte genetische Algorithmen für die Antennendesign der ST5-Satelliten (2006) — das entstandene Design sieht aus wie ein verbogener Draht, übertrifft aber alle manuellen Designs."
    ),

    Question(
        categoryId = 12,
        questionText = "Das Bin-Packing-Problem: Gegenstände unterschiedlicher Größe sollen in möglichst wenige Behälter (Kapazität 1) gepackt werden. Was leistet der First-Fit-Decreasing-Algorithmus (FFD)?",
        answerA = "Findet immer die optimale Anzahl Behälter",
        answerB = "FFD sortiert absteigend und platziert jeden Gegenstand in den ersten passenden Behälter — garantiert ≤ (11/9)·OPT + 6/9 Behälter",
        answerC = "FFD garantiert höchstens doppelt so viele Behälter wie optimal",
        answerD = "FFD ist nur für Gegenstände mit Größe ≥ 0,5 geeignet",
        correctAnswer = 1,
        explanation = "FFD (Baker, 1973): Sortiere Gegenstände absteigend nach Größe. Für jeden Gegenstand: Öffne neuen Behälter nur wenn kein bestehender passt. Analyse: FFD ≤ (11/9)·OPT + 6/9. Satz von Johnson (1973). Neuere Schranke: ≤ (11/9)·OPT + 6/9 ist exakt (es gibt Instanzen die diese Schranke erreichen). Next-Fit-Decreasing garantiert nur ≤ 2·OPT.",
        difficulty = 4,
        funFact = null
    ),

    // ── DATA STRUCTURES & ALGORITHMS (41–50) ─────────────────────────────────

    Question(
        categoryId = 12,
        questionText = "Eine Hash-Tabelle mit geschlossenem Hashing und linearem Sondieren hat Lastfaktor α = n/m. Was ist die erwartete Anzahl Sondierungsschritte bei einer erfolgreichen Suche?",
        answerA = "O(1/(1-α))",
        answerB = "Etwa (1/2)·(1 + 1/(1-α)) — nach Knuth",
        answerC = "O(α²)",
        answerD = "O(log n)",
        correctAnswer = 1,
        explanation = "Knuths Analyse (1963): Bei linearem Sondieren und Lastfaktor α = n/m ist die erwartete Anzahl Sondierungen für eine erfolgreiche Suche ≈ (1 + 1/(1-α))/2 und für eine erfolglose Suche ≈ (1 + 1/(1-α)²)/2. Bei α = 0,9: Ca. 5,5 Schritte (erfolgreich) bzw. 50,5 (erfolglos). Deshalb sollte α < 0,7 bleiben.",
        difficulty = 4,
        funFact = "Don Knuth analysierte lineare Sondierung 1963 auf Anfrage von Schüler Victor Vyssotsky — für eine Hausaufgabe. Knuth sagte: 'Diese Frage hat mich einen Monat beschäftigt.'"
    ),

    Question(
        categoryId = 12,
        questionText = "AVL-Bäume sind höhenbalancierte Binärbäume. Was garantiert die AVL-Eigenschaft in Bezug auf die Höhe?",
        answerA = "Die Höhe ist immer genau ⌊log₂ n⌋",
        answerB = "Die Höhe ist maximal 1,44 · log₂(n+2) — also O(log n)",
        answerC = "Die Höhe ist maximal n/2",
        answerD = "Die Höhe ist immer exakt log₂ n",
        correctAnswer = 1,
        explanation = "AVL-Eigenschaft: Für jeden Knoten gilt |Höhe(links) - Höhe(rechts)| ≤ 1. Die schlechteste AVL-Konfiguration sind Fibonacci-Bäume. Für Fibonacci-Baum der Höhe h gilt: N(h) ≥ φ^h / √5 (Fibonacci-Wachstum). Umgekehrt: h ≤ 1,44 · log₂(n+2) ≈ 1,44 · log₂ n. Alle Operationen (Suchen, Einfügen, Löschen) in O(log n).",
        difficulty = 4,
        funFact = "AVL-Bäume wurden 1962 von Georgy Adelson-Velsky und Yevgenia Landis veröffentlicht — die erste selbstbalancierende Baumdatenstruktur. Das 'AVL' steht für ihre Initialen."
    ),

    Question(
        categoryId = 12,
        questionText = "Rot-Schwarz-Bäume garantieren O(log n) für alle Operationen. Warum werden sie in der Praxis oft AVL-Bäumen vorgezogen?",
        answerA = "Rot-Schwarz-Bäume sind immer flacher als AVL-Bäume",
        answerB = "Rot-Schwarz-Bäume brauchen weniger Rotationen beim Einfügen/Löschen — besser für schreiblastige Anwendungen",
        answerC = "Rot-Schwarz-Bäume verwenden weniger Speicher",
        answerD = "AVL-Bäume unterstützen keine Lösch-Operationen",
        correctAnswer = 1,
        explanation = "AVL: Striktere Balance → flacher (Höhe ≤ 1,44 log n) → schnelleres Suchen. Rotationen beim Einfügen: AVL bis 2, Rot-Schwarz höchstens 2. Rotationen beim Löschen: AVL bis O(log n), Rot-Schwarz höchstens 3. In Datenbanken und Betriebssystemen (Linux-Kernel, Java TreeMap, C++ std::map) überwiegen Einfüge- und Lösch-Operationen → Rot-Schwarz bevorzugt.",
        difficulty = 4,
        funFact = "Der Linux-Kernel verwendet Rot-Schwarz-Bäume für den Prozess-Scheduler (CFS), virtuelle Speicherbereiche und den epoll-Mechanismus — in nahezu jedem Linux-Prozess aktiv."
    ),

    Question(
        categoryId = 12,
        questionText = "B-Bäume werden in Datenbanksystemen eingesetzt. Warum sind binäre Bäume (AVL, Rot-Schwarz) für externe Speicher ungeeignet?",
        answerA = "Binäre Bäume können keine Duplikate speichern",
        answerB = "Binäre Bäume haben O(log₂ n) Höhe, was viele Disk-I/Os bedeutet — B-Bäume haben sehr großen Verzweigungsgrad (Knoten = eine Diskseite) und O(log_B n) Höhe",
        answerC = "Binäre Bäume sind zu komplex für Datenbankimplementierungen",
        answerD = "B-Bäume sind kleiner als binäre Bäume",
        correctAnswer = 1,
        explanation = "Disk-I/O ist 10.000x teurer als RAM-Zugriff. Binärer Baum mit n=10^9: Höhe ≈ 30 → 30 Disk-Reads. B-Baum mit Seitengröße B=1000: Höhe ≈ log₁₀₀₀(10^9) = 3 → nur 3 Disk-Reads! B-Bäume maximieren die Anzahl Schlüssel pro Knoten (= Diskseite), um die Baumhöhe und damit Disk-I/Os zu minimieren.",
        difficulty = 4,
        funFact = "Das 'B' in B-Baum steht vermutlich für 'balanced', 'broad' oder den Erfinder 'Bayer' — Rudolf Bayer und Edward McCreight erfanden ihn 1970 bei Boeing Scientific Research Labs."
    ),

    Question(
        categoryId = 12,
        questionText = "Skip-Listen sind eine probabilistische Datenstruktur. Was ist ihre erwartete Laufzeit für Suche, Einfügen und Löschen?",
        answerA = "O(n) für alle Operationen",
        answerB = "O(log n) erwartet für alle Operationen",
        answerC = "O(1) für Suche, O(log n) für Einfügen",
        answerD = "O(√n) für alle Operationen",
        correctAnswer = 1,
        explanation = "Skip-Listen (Pugh, 1990): Eine verlinkte Liste mit mehreren 'Überholspuren'. Jedes Element wird mit Wahrscheinlichkeit p auf die nächste Ebene hochgezogen. Erwartete Höhe: O(log_{1/p} n). Suche: O(log n) erwartet durch Überspringen großer Bereiche. In der Praxis ähnlich schnell wie balancierte Bäume — einfacher zu implementieren und zu parallelisieren.",
        difficulty = 4,
        funFact = "Redis, LevelDB und Apache Lucene nutzen Skip-Listen für interne Datenstrukturen. William Pugh entwickelte sie als einfachere Alternative zu Rot-Schwarz-Bäumen."
    ),

    Question(
        categoryId = 12,
        questionText = "Fibonacci-Heaps ermöglichen amortisierte O(1) für decrease-key. Warum ist das für Dijkstras Algorithmus wichtig?",
        answerA = "Dijkstra benötigt decrease-key nicht",
        answerB = "Mit Fibonacci-Heap sinkt Dijkstras Laufzeit von O((V+E) log V) auf O(E + V log V) — entscheidend für dichte Graphen",
        answerC = "Fibonacci-Heaps beschleunigen nur den Extract-Min-Schritt",
        answerD = "Dijkstra mit Fibonacci-Heap ist langsamer als mit binärem Heap",
        correctAnswer = 1,
        explanation = "Dijkstra braucht: V × extract-min (O(log V)) + E × decrease-key. Binärer Heap: decrease-key O(log V) → Gesamt O((V+E) log V). Fibonacci-Heap: decrease-key amortisiert O(1) → Gesamt O(E + V log V). Bei dichten Graphen (E = V²): Fibonacci O(V²) vs. Binär-Heap O(V² log V) — ein Faktor log V schneller. Allerdings: Fibonacci-Heaps haben große konstante Faktoren — in der Praxis oft nicht schneller.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Union-Find (Disjoint Set Union) mit Union-by-Rank und Pfadkompression: Was ist die amortisierte Laufzeit pro Operation?",
        answerA = "O(log n)",
        answerB = "O(α(n)) — die inverse Ackermann-Funktion, praktisch O(1)",
        answerC = "O(√n)",
        answerD = "O(log log n)",
        correctAnswer = 1,
        explanation = "Tarjan (1975) bewies: Union-Find mit Union-by-Rank UND Pfadkompression hat amortisierte Laufzeit O(α(n)) pro Operation, wobei α die inverse Ackermann-Funktion ist. α(n) ≤ 4 für alle praktisch relevanten n (n < 10^80). Ohne Pfadkompression: O(log n). Ohne Union-by-Rank: O(log n). Nur beide zusammen ergeben O(α(n)).",
        difficulty = 4,
        funFact = "Die inverse Ackermann-Funktion α(n) ist für n = 2^(2^(2^(2^...))) noch ≤ 4 — für alle im Universum vorkommenden n ist α(n) ≤ 4. Sie ist 'praktisch konstant', aber mathematisch keine Konstante."
    ),

    Question(
        categoryId = 12,
        questionText = "Bloom-Filter ermöglichen platzsparende Mengenmitgliedschaftstests. Welche Art von Fehlern sind möglich?",
        answerA = "Falsch-Negative: Ein Element wird als 'nicht vorhanden' angegeben, obwohl es vorhanden ist",
        answerB = "Falsch-Positive: Ein Element wird als 'vorhanden' angegeben, obwohl es nicht enthalten ist — Falsch-Negative sind ausgeschlossen",
        answerC = "Beide Arten von Fehlern sind möglich",
        answerD = "Bloom-Filter sind fehlerfrei",
        correctAnswer = 1,
        explanation = "Bloom-Filter (1970): Ein Bit-Array mit k Hash-Funktionen. Einfügen: Setze k Bits. Prüfen: Sind alle k Bits gesetzt? Falsch-Positive: Möglich (andere Elemente haben zufällig dieselben Bits gesetzt). Falsch-Negative: Unmöglich (ein eingefügtes Element setzt alle k Bits permanent). Die Falsch-Positiv-Rate ist kontrollierbar durch Arraygröße und k.",
        difficulty = 4,
        funFact = "Google Chrome nutzte Bloom-Filter zur Malware-URL-Erkennung — Milliarden URLs lokal mit nur wenigen MB speichern. Cassandra, HBase und viele Datenbanken nutzen sie zum Vermeiden teurer Disk-Lookups."
    ),

    Question(
        categoryId = 12,
        questionText = "Das Closest-Pair-Problem: Finde die zwei nächsten Punkte unter n Punkten in der Ebene. Was ist die optimale Laufzeit?",
        answerA = "O(n²) — alle Paare müssen verglichen werden",
        answerB = "O(n log n) durch Divide-and-Conquer",
        answerC = "O(n log² n)",
        answerD = "O(n√n)",
        correctAnswer = 1,
        explanation = "Shamos-Hoey-Algorithmus (1975): Divide-and-Conquer. Teile Punkte in linke und rechte Hälfte. Rekursiv: min-Abstand δ in jeder Hälfte. Prüfe Punkte im Streifen der Breite 2δ um die Mittellinie — jeder Punkt muss nur mit maximal 7 anderen verglichen werden (Flächenargument). Gesamt: T(n) = 2T(n/2) + O(n) → O(n log n). Untere Schranke: Ω(n log n) durch Reduktion auf Sortierung.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Kosarajus Algorithmus zur Berechnung stark zusammenhängender Komponenten (SCCs) nutzt zwei DFS-Durchläufe. Was ist die Kernidee des zweiten Durchlaufs?",
        answerA = "DFS auf dem Originalgraphen in beliebiger Reihenfolge",
        answerB = "DFS auf dem transponierten Graph in umgekehrter Postorder des ersten Durchlaufs — jede erreichbare Komponente ist genau eine SCC",
        answerC = "BFS auf dem transponierten Graph",
        answerD = "DFS auf dem transponierten Graph in zufälliger Reihenfolge",
        correctAnswer = 1,
        explanation = "Kosamaru: (1) DFS auf G, speichere Postorder. (2) Transponiere G (kehre alle Kanten um). (3) DFS auf G^T in umgekehrter Postorder. Warum: Im transponierten Graphen sind SCCs dieselben, aber ihre Verbindungen laufen rückwärts. Der erste Durchlauf stellt sicher, dass wir SCCs in topologischer Reihenfolge besuchen — so erkennt jede Tiefensuche im zweiten Schritt exakt eine SCC. Laufzeit: O(V+E).",
        difficulty = 4,
        funFact = "Tarjans Algorithmus (ein Durchlauf, Stack-basiert) und Kosarajus Algorithmus (zwei Durchläufe, konzeptuell einfacher) sind äquivalent in Laufzeit und Korrektheit — Lehrbücher bevorzugen Kosamaru wegen seiner Klarheit."
    )

)
