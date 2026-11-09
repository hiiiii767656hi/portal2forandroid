package com.example.game.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChamberScore
import com.example.data.model.GameSave
import com.example.data.repository.PortalSaveRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GameViewModel(
    private val repository: PortalSaveRepository,
    val currentUserId: String
) : ViewModel() {

    val saves: StateFlow<List<GameSave>> = repository.observeSaves(currentUserId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val records: StateFlow<List<ChamberScore>> = repository.observeRecords(currentUserId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    fun saveGame(save: GameSave, onSaved: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.saveGame(save)
            onSaved(result.isSuccess)
        }
    }

    fun deleteSave(saveId: String) {
        viewModelScope.launch {
            repository.deleteSave(currentUserId, saveId)
        }
    }

    fun recordCompletion(chamberId: String, time: Double, portals: Long) {
        viewModelScope.launch {
            repository.recordCompletion(chamberId, time, portals)
            repository.syncProfile(
                displayName = "Test Subject",
                completedChambers = records.value.size.toLong() + 1L,
                portalsFired = portals
            )
        }
    }
}
