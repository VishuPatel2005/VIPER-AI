package com.rudra.myassistant.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class BalanceSummary(
    val friendUid: String = "",
    val friendName: String = "",
    val netBalance: Double = 0.0,
    val youOwe: Boolean = false,
    val friendPhotoUrl: String? = null
) {
    // No-arg constructor for Firebase
    constructor() : this("", "", 0.0, false, null)
}
