package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GameRepository
import com.example.data.GameRoom
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.data.extractFirestoreMessage
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HubViewModel(
    private val repository: GameRepository,
    private val currentUserId: String,
    defaultDisplayName: String
) : ViewModel() {

    val profileState: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Failed observing user profile", root)
            emit(UiState.Error(root.message ?: "Failed to load profile"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val myRoomsState: StateFlow<UiState<List<GameRoom>>> = repository.observeMyRooms(currentUserId)
        .map<List<GameRoom>, UiState<List<GameRoom>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Failed observing user rooms", root)
            emit(UiState.Error(root.message ?: "Failed to load active rooms"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(defaultDisplayName)
        }
    }

    fun clearError() {
        _actionError.value = null
    }

    fun updateNickname(newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) {
            _actionError.value = "Nickname cannot be empty."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            _actionError.value = null
            val res = repository.updateNickname(trimmed)
            res.onFailure { err ->
                _actionError.value = extractFirestoreMessage(err, "Failed to update nickname.")
            }
            _isBusy.value = false
        }
    }

    fun createRoom(nickname: String, onRoomReady: (String) -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            _actionError.value = null
            val res = repository.createRoom(nickname)
            res.onSuccess { code ->
                _isBusy.value = false
                onRoomReady(code)
            }.onFailure { err ->
                _isBusy.value = false
                _actionError.value = extractFirestoreMessage(err, "Could not create room.")
            }
        }
    }

    fun joinRoom(roomCode: String, nickname: String, onRoomReady: (String) -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            _actionError.value = null
            val res = repository.joinRoom(roomCode, nickname)
            res.onSuccess { code ->
                _isBusy.value = false
                onRoomReady(code)
            }.onFailure { err ->
                _isBusy.value = false
                _actionError.value = extractFirestoreMessage(err, "Could not join room.")
            }
        }
    }

    private companion object {
        const val TAG = "HubVM"
    }
}
