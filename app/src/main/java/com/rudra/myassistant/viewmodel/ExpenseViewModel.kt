package com.rudra.myassistant.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.rudra.myassistant.model.BalanceDirection
import com.rudra.myassistant.model.BalanceSummary
import com.rudra.myassistant.model.Expense
import com.rudra.myassistant.repository.BalanceRepository
import com.rudra.myassistant.repository.ExpenseRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// This ViewModel acts as a bridge for legacy Java components while using the new Flow-based Repository
class ExpenseViewModel(application: Application) : AndroidViewModel(application) {
    private val expenseRepository = ExpenseRepository()
    private val balanceRepository = BalanceRepository()
    
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
    
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _allBalances = MutableLiveData<List<BalanceSummary>>()
    
    // Bridging Flow to LiveData for legacy Java components
    fun getAllBalances(uid: String): LiveData<List<BalanceSummary>> {
        val result = MutableLiveData<List<BalanceSummary>>()
        viewModelScope.launch {
            balanceRepository.getBalancesForUser(uid).collectLatest { friendBalances ->
                val summaries = friendBalances.map { fb ->
                    BalanceSummary(
                        fb.friend.uid,
                        fb.friend.displayName,
                        fb.amount,
                        fb.direction == BalanceDirection.YOU_OWE,
                        fb.friend.photoUrl
                    )
                }
                result.postValue(summaries)
            }
        }
        return result
    }

    fun createExpense(expense: Expense) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = expenseRepository.createExpense(expense)
            _isLoading.postValue(false)
            result.onFailure {
                _errorMessage.postValue(it.message ?: "Unknown error")
            }
        }
    }

    // No longer needed to explicitly detach as callbackFlow handles it during scope cancellation
    fun detachListeners() { }
}
