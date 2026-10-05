package com.rudra.myassistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudra.myassistant.model.FriendBalance
import com.rudra.myassistant.model.BalanceDirection
import com.rudra.myassistant.model.FriendUser
import com.rudra.myassistant.repository.BalanceRepository
import com.rudra.myassistant.repository.FriendRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class SharedExpensesViewModel(
    private val repository: BalanceRepository = BalanceRepository(),
    private val friendRepository: FriendRepository = FriendRepository()
) : ViewModel() {

    private val _currentUid = MutableStateFlow<String?>(null)
    
    val balances: StateFlow<List<FriendBalance>> = _currentUid
        .filterNotNull()
        .flatMapLatest { uid ->
            repository.getBalancesForUser(uid).combine(friendRepository.getFriendsForExpense(uid)) { balanceList, friendList ->
                val balanceMap = balanceList.associateBy { it.friend.uid }
                friendList.map { friend ->
                    balanceMap[friend.uid] ?: FriendBalance(
                        friend = friend,
                        amount = 0.0,
                        direction = BalanceDirection.SETTLED,
                        balanceId = "${uid}_${friend.uid}"
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setCurrentUid(uid: String) {
        _currentUid.value = uid
    }
}
