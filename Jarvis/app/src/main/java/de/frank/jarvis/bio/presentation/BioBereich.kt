package de.frank.jarvis.bio.presentation

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import de.frank.jarvis.bio.BioAbruf
import de.frank.jarvis.bio.BioModul
import de.frank.jarvis.bio.presentation.amazfit.AmazfitTrainingDetailScreen
import de.frank.jarvis.bio.presentation.amazfit.AmazfitTrainingDetailViewModel
import de.frank.jarvis.bio.presentation.amazfit.AmazfitTrainingsScreen
import de.frank.jarvis.bio.presentation.amazfit.AmazfitTrainingsViewModel
import de.frank.jarvis.bio.presentation.dashboard4.BiomarkerDetailScreen
import de.frank.jarvis.bio.presentation.dashboard4.BiomarkerHostScreen
import de.frank.jarvis.bio.presentation.dashboard4.BiomarkerViewModel
import de.frank.jarvis.bio.presentation.dashboard4.HealthConnectDetailScreen
import de.frank.jarvis.bio.presentation.dashboard4.OuraDetailScreen
import de.frank.jarvis.bio.presentation.einstellungen.BioEinstellungenScreen
import de.frank.jarvis.bio.presentation.theme.BioThema
import kotlinx.coroutines.flow.map

/** Die Seiten des Biomarker-Bereichs (statt der Routen von Entropie Reductor). */
sealed class BioSeite(val schluessel: String) {
    data object Uebersicht : BioSeite("uebersicht")
    data class MetrikDetail(val metrik: String) : BioSeite("metrik/$metrik")
    data class OuraDetail(val metrik: String) : BioSeite("oura/$metrik")
    data class KoerperDetail(val metrik: String) : BioSeite("koerper/$metrik")
    data object Trainings : BioSeite("trainings")
    data class TrainingDetail(val trackId: String) : BioSeite("training/$trackId")
    data object Einstellungen : BioSeite("einstellungen")
}

/**
 * Ob der Biomarker-Bereich offen ist, und sein Rückstapel. Liegt bewusst in einem Objekt: Drehen, Auf-/Zuklappen des Fold
 * und Neuaufbau der Oberfläche lösen so weder einen neuen Abruf aus noch verlieren sie die geöffnete Seite.
 */
object BioAnsicht {
    var offen by mutableStateOf(false)
        private set

    /** Zählt jedes Betreten; die Übersicht springt dann einmal auf „Heute“ (der ViewModel überdauert das Schließen). */
    var oeffnungen by mutableIntStateOf(0)
        private set

    /** Seiten über der Übersicht; leer = Übersicht. */
    val stapel = mutableStateListOf<BioSeite>()

    /**
     * Betritt den Bereich. Nur hier wird der Abruf beim Öffnen ausgelöst (Auslöser 1 in [BioAbruf]), genau einmal je Betreten:
     * Ist der Bereich schon offen, passiert nichts.
     */
    fun oeffnen(context: Context) {
        if (offen) return
        stapel.clear()
        offen = true
        oeffnungen++
        BioModul.einrichten(context)
        // Kein Abruf beim Öffnen mehr (Franks Vorgabe vom 11.11.2026): frisch geholt wird nur über den Aktualisieren-Knopf oben,
        // per Sprache und vor jeder Tagesauswertung. Der Bereich zeigt den gespeicherten Stand mit Uhrzeit je Quelle.
    }

    fun schliessen() {
        offen = false
        stapel.clear()
    }

    fun zeige(seite: BioSeite) {
        if (stapel.lastOrNull() != seite) stapel.add(seite)
    }

    /**
     * Das Zahnrad: Die Biomarker-Einstellungen liegen in Jarvis' Einstellungen (Reiter „Biomarker“), wie alle Einstellungen.
     * JarvisApp beobachtet [zuEinstellungen], schließt den Bereich und öffnet dort den Reiter.
     */
    var zuEinstellungen by mutableStateOf(false)

    fun einstellungenOeffnen() { zuEinstellungen = true }

    /** Eine Seite zurück; auf der Übersicht schließt es den Bereich. */
    fun zurueck() {
        if (stapel.isNotEmpty()) stapel.removeAt(stapel.lastIndex) else schliessen()
    }
}

/** Vollbild des Biomarker-Bereichs. Die Systemleisten-Abstände setzt der Aufrufer. */
@Composable
fun BioBereich(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    BioModul.einrichten(context)
    // Steht vor dem Inhalt: Innere Zurück-Behandlungen (Vollbild-Diagramme im Training) haben so Vorrang.
    BackHandler(enabled = BioAnsicht.offen) { BioAnsicht.zurueck() }
    val halter = rememberSaveableStateHolder()
    val seite = BioAnsicht.stapel.lastOrNull() ?: BioSeite.Uebersicht
    BioThema {
        Box(modifier.fillMaxSize()) {
            halter.SaveableStateProvider(seite.schluessel) {
                when (seite) {
                    BioSeite.Uebersicht -> BiomarkerHostScreen(
                        onClose = BioAnsicht::schliessen,
                        onOpenSettings = { BioAnsicht.einstellungenOeffnen() },
                        onOpenMetricDetail = { BioAnsicht.zeige(BioSeite.MetrikDetail(it)) },
                        onOpenTrainingDetail = { BioAnsicht.zeige(BioSeite.TrainingDetail(it)) },
                        onOpenAllTrainings = { BioAnsicht.zeige(BioSeite.Trainings) },
                        onOpenOuraDetail = { BioAnsicht.zeige(BioSeite.OuraDetail(it)) },
                        onOpenHealthConnectDetail = { BioAnsicht.zeige(BioSeite.KoerperDetail(it)) },
                    )
                    is BioSeite.MetrikDetail -> BiomarkerDetailScreen(seite.metrik, onBack = BioAnsicht::zurueck)
                    is BioSeite.OuraDetail -> OuraDetailScreen(seite.metrik, onBack = BioAnsicht::zurueck)
                    is BioSeite.KoerperDetail -> HealthConnectDetailScreen(seite.metrik, onBack = BioAnsicht::zurueck)
                    BioSeite.Trainings -> AmazfitTrainingsScreen(
                        onBack = BioAnsicht::zurueck,
                        onOpenDetail = { BioAnsicht.zeige(BioSeite.TrainingDetail(it)) },
                    )
                    is BioSeite.TrainingDetail -> AmazfitTrainingDetailScreen(
                        onBack = BioAnsicht::zurueck,
                        vm = trainingDetailViewModel(seite.trackId),
                    )
                    BioSeite.Einstellungen -> BioEinstellungenScreen(onBack = BioAnsicht::zurueck)
                }
            }
        }
    }
}

/* ---------------- ViewModels ohne Hilt: Abhängigkeiten aus BioModul ---------------- */

/** Der Biomarker-ViewModel; Übersicht und Detailseiten teilen sich denselben (wie in Entropie Reductor über den Reiter). */
@Composable
fun biomarkerViewModel(): BiomarkerViewModel {
    val app = LocalContext.current.applicationContext
    return viewModel(key = "bio-biomarker") {
        BioModul.einrichten(app)
        BiomarkerViewModel(
            repo = BioModul.whoop,
            amazfitRepo = BioModul.trainings,
            ouraRepo = BioModul.oura,
            cardOrderRepo = BioModul.kartenReihenfolge,
            syncCoordinator = BioModul.abgleich,
            healthConnect = BioModul.healthConnect,
            settings = BioModul.einstellungen,
            zeppBodyRepository = BioModul.waage,
            manuellAbrufen = { BioAbruf.manuell(app, "Aktualisieren-Knopf") },
            abrufLaeuft = BioAbruf.stand.map { it.laeuft },
        )
    }
}

@Composable
fun trainingsViewModel(): AmazfitTrainingsViewModel {
    val app = LocalContext.current.applicationContext
    return viewModel(key = "bio-trainings") {
        BioModul.einrichten(app)
        AmazfitTrainingsViewModel(BioModul.trainings, BioModul.datenbank.biomarkerSnapshotDao())
    }
}

/** Je Training ein eigener ViewModel (Schlüssel = trackId), sonst zeigte jedes Training das zuerst geöffnete. */
@Composable
fun trainingDetailViewModel(trackId: String): AmazfitTrainingDetailViewModel {
    val app = LocalContext.current.applicationContext
    return viewModel(key = "bio-training-$trackId") {
        BioModul.einrichten(app)
        AmazfitTrainingDetailViewModel(BioModul.trainings, BioModul.datenbank.biomarkerSnapshotDao(), trackId)
    }
}
