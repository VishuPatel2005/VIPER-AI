package com.rudra.myassistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudra.myassistant.repository.SettlementRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class SettleUpViewModel(
    private val repository: SettlementRepository = SettlementRepository()
) : ViewModel() {

    private val _settlementResult = MutableSharedFlow<Result<Unit>>()
    val settlementResult = _settlementResult.asSharedFlow()

    fun settle(currentUid: String, friendUid: String, amount: Double) {
        viewModelScope.launch {
            val result = repository.settleBalance(currentUid, friendUid, amount)
            _settlementResult.emit(result)
        }
    }
}
