package de.frank.entropyreducer.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.repository.PromptRepository
import de.frank.entropyreducer.data.local.entities.SavedPromptEntity
import de.frank.entropyreducer.presentation.navigation.AppNavGraph
import de.frank.entropyreducer.presentation.theme.EntropieReductorTheme
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Einzige Activity der App. Compose uebernimmt das gesamte Routing.
 * Beim ersten Start: Default-Prompts vorinstallieren.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Boot-VM laedt Default-Prompts beim ersten Start
            val bootVm: BootstrapViewModel = hiltViewModel()
            EntropieReductorTheme {
                AppNavGraph()
            }
        }
    }
}

/**
 * Initialisiert beim ersten App-Start die drei Default-Prompts (Spec §6.4).
 */
@HiltViewModel
class BootstrapViewModel @Inject constructor(
    private val prompts: PromptRepository,
) : ViewModel() {
    init {
        viewModelScope.launch {
            if (prompts.count() == 0) {
                val now = System.currentTimeMillis()
                prompts.upsert(
                    SavedPromptEntity(
                        id = UUID.randomUUID().toString(),
                        name = "Koerper zuerst",
                        content = "Priorisiere Eintraege der Kategorie KOERPERLICH grundsaetzlich am hoechsten, weil koerperliche Verfassung Voraussetzung fuer jede andere Reduktion ist. Wenn ein koerperlicher Eintrag offen ist, darf kein anderer einen hoeheren Score bekommen.",
                        isActive = true,
                        createdAt = now, updatedAt = now,
                    )
                )
                prompts.upsert(
                    SavedPromptEntity(
                        id = UUID.randomUUID().toString(),
                        name = "Schichtdienst-Logik",
                        content = "Frank arbeitet im Schichtsystem (4 Tagdienste, 4 frei, 4 Nachtdienste, 4 frei). Beruecksichtige bei zeitlichen Aufgaben, ob ein Eintrag in einem Frei-Block schneller erledigt werden kann. Aufgaben, die einen Frei-Block brauchen, werden im Dienst niedriger priorisiert.",
                        isActive = true,
                        createdAt = now + 1, updatedAt = now + 1,
                    )
                )
                prompts.upsert(
                    SavedPromptEntity(
                        id = UUID.randomUUID().toString(),
                        name = "Schnelle Siege bevorzugen",
                        content = "Wenn ein Eintrag in unter 10 Minuten erledigbar ist, gib ihm einen Bonus von +15 auf den Priority Score, weil schnelle Siege psychische Entropie sofort senken.",
                        isActive = true,
                        createdAt = now + 2, updatedAt = now + 2,
                    )
                )
            }
        }
    }
}
