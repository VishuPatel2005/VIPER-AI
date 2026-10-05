package com.rudra.myassistant.model

import com.google.firebase.Timestamp

data class Expense(
    val expenseId: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val currency: String = "INR",
    val createdBy: String = "",
    val paidBy: String = "",
    val participants: List<String> = emptyList(),
    val splits: Map<String, Double> = emptyMap(),
    val timestamp: Timestamp = Timestamp.now(),
    val status: String = "pending", // pending | settled
    val groupId: String? = null,
    val notes: String? = null
)
