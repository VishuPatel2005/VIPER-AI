package com.rudra.myassistant.model

data class FriendUser(
    val uid: String = "",
    val displayName: String = "",
    val username: String = "",
    val photoUrl: String = "",
    val isOnline: Boolean = false
)
