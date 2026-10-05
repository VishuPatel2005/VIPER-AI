package com.rudra.myassistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.rudra.myassistant.model.Expense
import com.rudra.myassistant.model.FriendUser
import com.rudra.myassistant.repository.ExpenseRepository
import com.rudra.myassistant.repository.FriendRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AddExpenseViewModel(
    private val friendRepository: FriendRepository = FriendRepository(),
    private val expenseRepository: ExpenseRepository = ExpenseRepository()
) : ViewModel() {

    private val _currentUid = MutableStateFlow<String?>(null)
    
    private val _friends = MutableStateFlow<List<FriendUser>>(emptyList())
    val friends: StateFlow<List<FriendUser>> = _friends.asStateFlow()

    private val _selectedParticipants = MutableStateFlow<List<FriendUser>>(emptyList())
    val selectedParticipants: StateFlow<List<FriendUser>> = _selectedParticipants.asStateFlow()

    private val _errorEvent = MutableSharedFlow<String>()
    val errorEvent = _errorEvent.asSharedFlow()

    private val _saveResult = MutableSharedFlow<Result<Unit>>()
    val saveResult = _saveResult.asSharedFlow()

    init {
        loadFriends()
    }

    private fun loadFriends() {
        viewModelScope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                _currentUid.value = null
                return@launch
            }
            _currentUid.value = uid
            
            friendRepository.getFriendsForExpense(uid)
                .catch { e -> _errorEvent.emit(e.message ?: "Failed to load friends") }
                .collect { list -> _friends.value = list }
        }
    }

    fun toggleParticipant(friend: FriendUser) {
        val current = _selectedParticipants.value.toMutableList()
        if (current.any { it.uid == friend.uid }) {
            current.removeAll { it.uid == friend.uid }
        } else {
            current.add(friend)
        }
        _selectedParticipants.value = current
    }

    fun removeParticipant(uid: String) {
        _selectedParticipants.value = _selectedParticipants.value.filter { it.uid != uid }
    }

    fun isSelected(uid: String): Boolean {
        return _selectedParticipants.value.any { it.uid == uid }
    }

    fun getParticipantUids(): List<String> {
        val currentUid = _currentUid.value ?: return emptyList()
        return (_selectedParticipants.value.map { it.uid } + currentUid).distinct()
    }

    fun saveExpense(title: String, amount: Double) {
        val uid = _currentUid.value ?: return
        val participantUids = getParticipantUids()
        
        // Equal split logic
        val perPersonShare = amount / participantUids.size
        val splits = participantUids.associateWith { perPersonShare }

        val expense = Expense(
            title = title,
            amount = amount,
            paidBy = uid, // Defaulting to current user paid
            createdBy = uid,
            participants = participantUids,
            splits = splits
        )

        viewModelScope.launch {
            val result = expenseRepository.createExpense(expense)
            _saveResult.emit(result)
        }
    }
}
