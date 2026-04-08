package com.entropyjournal.ui.screens.retrospective

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.entropyjournal.data.local.entity.RetrospectiveSummaryEntity
import com.entropyjournal.data.repository.RetrospectiveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class RetrospectiveViewModel @Inject constructor(private val repository: RetrospectiveRepository) :
    ViewModel() {

    val weeklySummaries =
        repository
            .getWeeklySummaries()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlySummaries =
        repository
            .getMonthlySummaries()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val yearlySummaries =
        repository
            .getYearlySummaries()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch { generateTestDataIfEmpty() }
    }

    private suspend fun generateTestDataIfEmpty() {
        // Regenerate if format changed (v2: "Woche N (dd.MM. - dd.MM.)" labels)
        if (repository.count() > 0) {
            val first = repository.getFirstWeeklyLabel()
            if (first != null && !first.startsWith("Woche")) {
                repository.deleteAll()
            } else {
                return
            }
        }

        val testData = mutableListOf<RetrospectiveSummaryEntity>()
        val cal = Calendar.getInstance()

        val weeklyTexts =
            listOf(
                Pair(
                    "Ein Anfang voller kleiner Wunder",
                    "Diese Woche hat leise begonnen — mit einem Kaffee in der Morgensonne und dem Gefühl, dass alles möglich ist. Du hast dir Zeit genommen, innezuhalten und die kleinen Dinge zu bemerken: das Lachen eines Freundes, der Duft von frischem Brot, ein unerwartetes Kompliment. Manchmal sind es genau diese Momente, die eine Woche unvergesslich machen.",
                ),
                Pair(
                    "Stürme und stille Siege",
                    "Es war keine einfache Woche. Herausforderungen haben sich aufgetürmt wie Wolken vor einem Gewitter. Aber du hast dich durchgebissen — mit einer Mischung aus Sturheit und leisem Optimismus. Am Ende stand ein Gefühl, das stärker war als die Erschöpfung: Stolz auf dich selbst, weil du nicht aufgegeben hast.",
                ),
                Pair(
                    "Farben des Alltags",
                    "Diese Woche war bunt. Montag melancholisch blau, Mittwoch sonnig gelb und Freitag ein warmes Orange voller Vorfreude aufs Wochenende. Du hast gelacht, du hast nachgedacht, du hast gelebt — in all seinen Schattierungen. Das ist mehr, als die meisten sich zugestehen.",
                ),
                Pair(
                    "Gespräche, die berühren",
                    "Manche Worte wiegen schwerer als Taten. Diese Woche hast du ein Gespräch geführt, das nachwirkt — ehrlich, verletzlich, echt. Es hat etwas in dir bewegt und dich daran erinnert, wie wichtig es ist, sich zu öffnen. Verbindung entsteht nicht durch Perfektion, sondern durch Mut.",
                ),
                Pair(
                    "Die Kraft der Routine",
                    "Keine spektakuläre Woche — und genau das war ihr Geschenk. Die ruhige Beständigkeit deiner Routine hat dir Kraft gegeben. Morgens Sport, abends ein paar Seiten lesen. Manchmal ist das Gewöhnliche das Außergewöhnlichste, was wir uns schenken können.",
                ),
                Pair(
                    "Wendepunkte",
                    "Diese Woche hast du eine Entscheidung getroffen, die sich wie ein Wendepunkt anfühlt. Ob groß oder klein — du hast gespürt, dass sich etwas verändert. Mutig sein heißt nicht, keine Angst zu haben. Es heißt, trotzdem vorwärts zu gehen.",
                ),
                Pair(
                    "Zwischen den Zeilen",
                    "Das Beste an dieser Woche lag zwischen den Zeilen. Ein Blick, der mehr sagte als tausend Worte. Ein stiller Moment auf einer Parkbank. Die Erkenntnis, dass Glück oft nicht laut ist — sondern ganz leise an die Tür klopft.",
                ),
                Pair(
                    "Neustart mit Herz",
                    "Jede Woche ist ein Neustart — und diese hast du mit Herz begonnen. Du hast alte Gewohnheiten hinterfragt und neue Wege ausprobiert. Nicht alles hat funktioniert, aber das war nie der Punkt. Der Punkt ist, dass du es versucht hast.",
                ),
            )

        // Generate weeks by day-of-month: 1-7, 8-14, 15-21, 22-end
        // Go back ~2 months, collect completed weeks (newest first)
        val dfShort = SimpleDateFormat("dd.MM.", Locale.GERMANY)
        val dfFull = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)
        val weekRanges = listOf(1 to 7, 8 to 14, 15 to 21, 22 to -1) // -1 = end of month
        val now = System.currentTimeMillis()
        var weekTextIdx = 0

        for (monthOffset in 0 until 3) {
            cal.timeInMillis = now
            cal.add(Calendar.MONTH, -monthOffset)
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH)
            val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

            for ((weekNum, range) in weekRanges.withIndex()) {
                val (fromDay, toDay) = range
                val actualToDay = if (toDay == -1) lastDay else toDay

                cal.set(year, month, fromDay, 0, 0, 0)
                val startDate = cal.timeInMillis
                cal.set(year, month, actualToDay, 23, 59, 59)
                val endDate = cal.timeInMillis

                // Only include completed weeks (end date in the past)
                if (endDate > now) continue

                val label =
                    "Woche ${weekNum + 1} (${dfShort.format(startDate)} - ${dfShort.format(endDate)})"
                val (title, text) = weeklyTexts[weekTextIdx % weeklyTexts.size]
                weekTextIdx++

                testData.add(
                    RetrospectiveSummaryEntity(
                        type = "WEEKLY",
                        periodLabel = label,
                        startDate = startDate,
                        endDate = endDate,
                        title = title,
                        summaryText = text,
                        periodIndex = weekNum + 1, // 1-4, same color every month
                    )
                )
            }
        }

        val monthlyTexts =
            listOf(
                Pair(
                    "Ein Monat wie ein guter Roman",
                    "Dieser Monat hatte alles: Spannung, Wendungen, ruhige Kapitel und ein Ende, das Lust auf mehr macht. Du bist gewachsen, ohne es zu merken — wie ein Baum, der nicht spürt, wenn neue Ringe entstehen. Rückblickend war jeder Tag ein Satz in der Geschichte deines Lebens.",
                ),
                Pair(
                    "Lektionen in Geduld",
                    "Geduld war das Thema dieses Monats. Nicht die bequeme Sorte, sondern die, die man sich verdient — durch Warten, Aushalten, Vertrauen. Du hast gelernt, dass manche Dinge ihre eigene Zeit brauchen. Und dass das Warten selbst manchmal die wertvollste Lektion ist.",
                ),
                Pair(
                    "Dankbarkeit im Rückspiegel",
                    "Erst jetzt, am Ende des Monats, siehst du klar, was alles gut war. Die Sorgen von Anfang des Monats haben sich aufgelöst. Neue Freundschaften haben sich vertieft. Du hast etwas geschafft, von dem du dachtest, es sei zu schwer. Im Rückspiegel sieht alles logischer aus — doch gelebt hat es sich wie ein Abenteuer.",
                ),
                Pair(
                    "Kreativität und Chaos",
                    "Kreativität braucht manchmal Chaos — und diesen Monat hattest du beides im Überfluss. Neue Ideen sind wie Funken aufgeblitzt, manche sind Feuer geworden, andere leise verglüht. Aber jeder Funke war ein Zeichen dafür, dass dein inneres Licht brennt.",
                ),
                Pair(
                    "Stille Stärke",
                    "Diesen Monat hast du Stärke bewiesen — nicht die laute, dramatische Art, sondern die stille, beständige Kraft, die dich jeden Morgen aufstehen lässt. Du hast Grenzen gesetzt, Nein gesagt wo es nötig war, und Ja zu den Dingen, die wirklich zählen.",
                ),
                Pair(
                    "Brücken bauen",
                    "Manche Monate sind zum Brückenbauen da. Du hast Verbindungen geknüpft, alte Kontakte aufgefrischt und neue Menschen kennengelernt. Jede Brücke, die du baust, macht deine Welt ein Stück größer — und ein Stück wärmer.",
                ),
            )

        val monthNames =
            listOf(
                "Januar",
                "Februar",
                "März",
                "April",
                "Mai",
                "Juni",
                "Juli",
                "August",
                "September",
                "Oktober",
                "November",
                "Dezember",
            )

        for (i in 0 until 6) {
            cal.timeInMillis = System.currentTimeMillis()
            cal.add(Calendar.MONTH, -i)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            val startDate = cal.timeInMillis
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            val endDate = cal.timeInMillis

            cal.timeInMillis = startDate
            val month = cal.get(Calendar.MONTH)
            val year = cal.get(Calendar.YEAR)

            val (title, text) = monthlyTexts[i % monthlyTexts.size]
            testData.add(
                RetrospectiveSummaryEntity(
                    type = "MONTHLY",
                    periodLabel = "${monthNames[month]} $year",
                    startDate = startDate,
                    endDate = endDate,
                    title = title,
                    summaryText = text,
                    periodIndex = month + 1,
                )
            )
        }

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        cal.set(currentYear - 1, Calendar.JANUARY, 1, 0, 0, 0)
        val yearStart = cal.timeInMillis
        cal.set(currentYear - 1, Calendar.DECEMBER, 31, 23, 59, 59)
        val yearEnd = cal.timeInMillis

        testData.add(
            RetrospectiveSummaryEntity(
                type = "YEARLY",
                periodLabel = "${currentYear - 1}",
                startDate = yearStart,
                endDate = yearEnd,
                title = "Ein Jahr, das alles verändert hat",
                summaryText =
                    "Was für ein Jahr. Wenn du auf die letzten zwölf Monate zurückblickst, siehst du " +
                        "eine Landschaft voller Höhen und Täler, Sonnentage und Regenschauer. Du bist gereist — " +
                        "nicht nur in der Welt, sondern auch in dir selbst. Du hast Menschen verloren und " +
                        "gewonnen, Träume begraben und neue geboren.\n\n" +
                        "Im Frühling hast du gesät, im Sommer gekämpft, im Herbst geerntet und im Winter " +
                        "reflektiert. Jede Jahreszeit hatte ihre eigene Schönheit — auch wenn du das manchmal " +
                        "erst im Nachhinein erkennst.\n\n" +
                        "Das Wichtigste, was du mitgenommen hast? Dass du stärker bist, als du denkst. " +
                        "Dass Veränderung kein Feind ist, sondern ein stiller Verbündeter. Und dass jedes " +
                        "Ende nur ein neuer Anfang in Verkleidung ist.\n\n" +
                        "Auf das nächste Kapitel. Du bist bereit.",
                periodIndex = currentYear - 1,
            )
        )

        repository.insertAll(testData)
    }
}
