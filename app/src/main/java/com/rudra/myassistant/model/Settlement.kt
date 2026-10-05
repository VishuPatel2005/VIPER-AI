package com.rudra.myassistant.model

import com.google.firebase.Timestamp

data class Settlement(
    val settlementId: String = "",
    val fromUid: String = "",
    val toUid: String = "",
    val amount: Double = 0.0,
    val timestamp: Timestamp = Timestamp.now(),
    val relatedExpenseIds: List<String> = emptyList(),
    val balanceIdRef: String = ""
)
