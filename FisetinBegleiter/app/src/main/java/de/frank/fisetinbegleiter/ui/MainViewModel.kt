package de.frank.fisetinbegleiter.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.frank.fisetinbegleiter.FisetinApplication
import de.frank.fisetinbegleiter.alarm.AlarmType
import de.frank.fisetinbegleiter.alarm.NotificationHelper
import de.frank.fisetinbegleiter.data.AppCureState
import de.frank.fisetinbegleiter.data.CureWithDays
import de.frank.fisetinbegleiter.data.IngredientEntity
import de.frank.fisetinbegleiter.data.ProtocolTemplateEntity
import de.frank.fisetinbegleiter.data.StackItemEntity
import de.frank.fisetinbegleiter.domain.canComplete
import de.frank.fisetinbegleiter.domain.cureState
import de.frank.fisetinbegleiter.domain.currentDay
import de.frank.fisetinbegleiter.domain.timeline
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class MainUiState(
    val protocol: ProtocolTemplateEntity = ProtocolTemplateEntity(),
    val ingredients: List<IngredientEntity> = emptyList(),
    val stackItems: List<StackItemEntity> = emptyList(),
    val activeCure: CureWithDays? = null,
    val history: List<CureWithDays> = emptyList(),
    val now: Long = System.currentTimeMillis(),
    val initialized: Boolean = false,
) {
    val currentDay get() = de.frank.fisetinbegleiter.domain.currentDay(activeCure)
    val cureState: AppCureState get() = cureState(activeCure, protocol, now)
    val blockEnd: Long? get() = currentDay?.t0?.let { timeline(it, protocol).blockEnd }
    val blockActive: Boolean get() = blockEnd?.let { now < it } == true
    val canCompleteDay: Boolean get() = canComplete(currentDay, now, protocol)
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as FisetinApplication
    private val repository = app.repository
    private val scheduler = app.alarmScheduler
    private val now = MutableStateFlow(System.currentTimeMillis())
    private val initialized = MutableStateFlow(false)

    val uiState: StateFlow<MainUiState> = combine(
        repository.protocol,
        repository.ingredients,
        repository.stackItems,
        repository.activeCure,
        repository.history,
        now,
        initialized,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        MainUiState(
            protocol = values[0] as? ProtocolTemplateEntity ?: ProtocolTemplateEntity(),
            ingredients = values[1] as List<IngredientEntity>,
            stackItems = values[2] as List<StackItemEntity>,
            activeCure = values[3] as CureWithDays?,
            history = values[4] as List<CureWithDays>,
            now = values[5] as Long,
            initialized = values[6] as Boolean,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            initialized.value = true
        }
        viewModelScope.launch {
            while (isActive) {
                val current = System.currentTimeMillis()
                now.value = current
                reconcileTime(current)
                delay(1_000)
            }
        }
    }

    fun createCure(duration: Int, liquid: String, fat: String, startMinute: Int) {
        viewModelScope.launch {
            repository.createCure(duration, LocalDate.now(), liquid, fat, startMinute)
        }
    }

    fun drinkNow() {
        val state = uiState.value
        val cure = state.activeCure?.cure ?: return
        val day = state.currentDay ?: return
        val timestamp = System.currentTimeMillis()
        viewModelScope.launch {
            repository.setDrinkTime(cure, day, timestamp)
            scheduler.scheduleDay(cure, day, state.protocol, timestamp)
        }
    }

    fun markMeal() {
        val day = uiState.value.currentDay ?: return
        viewModelScope.launch {
            repository.markMeal(day.id, System.currentTimeMillis())
            scheduler.cancelMeal(day.id)
        }
    }

    fun markSpermidin() {
        val day = uiState.value.currentDay ?: return
        viewModelScope.launch {
            repository.markSpermidin(day.id, System.currentTimeMillis())
            scheduler.cancelSpermidin(day.id)
        }
    }

    fun completeDay() {
        val state = uiState.value
        val cure = state.activeCure?.cure ?: return
        val day = state.currentDay ?: return
        if (!state.canCompleteDay) return
        viewModelScope.launch {
            val timestamp = System.currentTimeMillis()
            if (day.blockEndedAt == null) repository.markBlockEnded(day.id, timestamp)
            repository.completeDay(cure, day.copy(blockEndedAt = day.blockEndedAt ?: timestamp), timestamp)
            if (day.dayNumber == cure.plannedDurationDays) {
                NotificationHelper.show(app, AlarmType.CURE_ENDED, day.id, cure.plannedDurationDays)
            }
        }
    }

    fun canScheduleExactAlarms(): Boolean = scheduler.canScheduleExact()

    fun saveProtocol(protocol: ProtocolTemplateEntity) = viewModelScope.launch { repository.saveProtocol(protocol) }
    fun saveIngredient(item: IngredientEntity) = viewModelScope.launch { repository.saveIngredient(item) }
    fun deleteIngredient(item: IngredientEntity) = viewModelScope.launch { repository.deleteIngredient(item) }
    fun saveStackItem(item: StackItemEntity) = viewModelScope.launch { repository.saveStackItem(item) }
    fun deleteStackItem(item: StackItemEntity) = viewModelScope.launch { repository.deleteStackItem(item) }

    fun updateNote(cure: CureWithDays, note: String) = viewModelScope.launch {
        repository.updateNote(cure.cure, note)
    }

    fun exportText(): String = repository.exportText(uiState.value.history)

    private suspend fun reconcileTime(timestamp: Long) {
        val state = uiState.value
        val day = currentDay(state.activeCure) ?: return
        val t0 = day.t0 ?: return
        if (day.blockEndedAt == null && timestamp >= timeline(t0, state.protocol).blockEnd) {
            repository.markBlockEnded(day.id, timestamp)
        }
    }
}
