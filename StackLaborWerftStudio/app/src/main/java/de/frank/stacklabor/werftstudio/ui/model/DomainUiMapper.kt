package de.frank.stacklabor.werftstudio.ui.model

import de.frank.stacklabor.werftstudio.domain.model.Ampel
import de.frank.stacklabor.werftstudio.domain.model.EigeneFrage
import de.frank.stacklabor.werftstudio.domain.model.Loeslichkeit
import de.frank.stacklabor.werftstudio.domain.model.Mittel
import de.frank.stacklabor.werftstudio.domain.model.Stack
import de.frank.stacklabor.werftstudio.domain.model.Ziel
import de.frank.stacklabor.werftstudio.domain.model.darreichungsformAnzeige

fun Ampel.toSignalState(): SignalState = when (this) {
    Ampel.GRUEN -> SignalState.Green
    Ampel.GELB -> SignalState.Yellow
    Ampel.ROT -> SignalState.Red
    Ampel.GRAU -> SignalState.Gray
}

fun Loeslichkeit.toSolubility(): Solubility = when (this) {
    Loeslichkeit.WASSER -> Solubility.Water
    Loeslichkeit.FETT -> Solubility.Fat
    Loeslichkeit.BEIDES -> Solubility.Both
}

fun Stack.toStackSummaryUi(
    medicineCount: Int,
    signal: Ampel,
    counts: SignalCounts,
): StackSummaryUi = StackSummaryUi(
    id = id,
    name = name,
    meta = listOf(zeitpunkt, einnahmeHinweis).filter(String::isNotBlank).joinToString(" · "),
    medicineCount = medicineCount,
    signal = signal.toSignalState(),
    counts = counts,
)

fun Mittel.toCatalogMedicineUi(usageCount: Int): MedicineUi = MedicineUi(
    id = id,
    name = name,
    dose = "",
    solubility = loeslichkeit.toSolubility(),
    catalogMeta = "in $usageCount ${if (usageCount == 1) "Stack" else "Stacks"} · ${darreichungsformAnzeige()}" +
        hersteller.takeIf(String::isNotBlank)?.let { " · $it" }.orEmpty(),
    manufacturer = hersteller,
    form = darreichungsformAnzeige(),
    diarrheaRisk = durchfallrisiko,
    excipients = beistoffe,
)

fun Ziel.toGoalUi(rank: Int, signal: Ampel, selected: Boolean, usageCount: Int): GoalUi = GoalUi(
    id = id,
    rank = rank,
    text = text,
    signal = signal.toSignalState(),
    selected = selected,
    usage = "in $usageCount ${if (usageCount == 1) "Stack" else "Stacks"} verwendet",
)

fun EigeneFrage.toQuestionUi(): QuestionUi = QuestionUi(id = id, text = text)
