package com.rudra.myassistant.model

import com.google.firebase.Timestamp

data class Balance(
    val users: List<String> = emptyList(),
    val uidA: String = "",
    val uidB: String = "",
    val netAmount: Double = 0.0, // positive = uidA owes uidB, negative = uidB owes uidA
    val lastUpdated: Timestamp = Timestamp.now()
)

enum class BalanceDirection {
    YOU_OWE, THEY_OWE, SETTLED
}

data class FriendBalance(
    val friend: FriendUser,
    val amount: Double,
    val direction: BalanceDirection,
    val balanceId: String
)
