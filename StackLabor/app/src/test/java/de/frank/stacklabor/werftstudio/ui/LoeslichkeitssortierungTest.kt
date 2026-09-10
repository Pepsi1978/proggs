package de.frank.stacklabor.werftstudio.ui

import de.frank.stacklabor.werftstudio.ui.model.MedicineUi
import de.frank.stacklabor.werftstudio.ui.model.Solubility
import de.frank.stacklabor.werftstudio.ui.model.sortiereNachLoeslichkeit
import kotlin.test.Test
import kotlin.test.assertEquals

class LoeslichkeitssortierungTest {
    private val mittel = listOf(
        MedicineUi("w2", "Zink", "", solubility = Solubility.Water),
        MedicineUi("f1", "Omega-3", "", solubility = Solubility.Fat),
        MedicineUi("b1", "Alpha-Liponsäure", "", solubility = Solubility.Both),
        MedicineUi("w1", "Bor", "", solubility = Solubility.Water),
        MedicineUi("f2", "Vitamin D3", "", solubility = Solubility.Fat),
    )

    @Test
    fun `fettloesliche stehen standardmaessig oben`() {
        val sortiert = sortiereNachLoeslichkeit(mittel, fettZuerst = true)

        assertEquals(
            listOf("Omega-3", "Vitamin D3", "Alpha-Liponsäure", "Bor", "Zink"),
            sortiert.map { it.name },
        )
    }

    @Test
    fun `umgedreht stehen die wasserloeslichen oben`() {
        val sortiert = sortiereNachLoeslichkeit(mittel, fettZuerst = false)

        assertEquals(
            listOf("Bor", "Zink", "Alpha-Liponsäure", "Omega-3", "Vitamin D3"),
            sortiert.map { it.name },
        )
    }

    @Test
    fun `beides steht in beiden Richtungen in der Mitte`() {
        val fettZuerst = sortiereNachLoeslichkeit(mittel, fettZuerst = true).map { it.solubility }
        val wasserZuerst = sortiereNachLoeslichkeit(mittel, fettZuerst = false).map { it.solubility }

        assertEquals(2, fettZuerst.indexOf(Solubility.Both))
        assertEquals(2, wasserZuerst.indexOf(Solubility.Both))
    }

    @Test
    fun `innerhalb einer Gruppe wird nach Namen sortiert`() {
        val gleicheGruppe = listOf(
            MedicineUi("a", "zink", "", solubility = Solubility.Water),
            MedicineUi("b", "Bor", "", solubility = Solubility.Water),
            MedicineUi("c", "Ascorbinsäure", "", solubility = Solubility.Water),
        )

        val sortiert = sortiereNachLoeslichkeit(gleicheGruppe, fettZuerst = true)

        assertEquals(listOf("Ascorbinsäure", "Bor", "zink"), sortiert.map { it.name })
    }
}
