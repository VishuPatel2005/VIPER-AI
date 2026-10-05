package com.rudra.myassistant.model

data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val username: String = "",
    val photoUrl: String = "",
    val totalOwed: Double = 0.0,
    val totalOwe: Double = 0.0,
    val hasOutstandingBalance: Boolean = false
)
