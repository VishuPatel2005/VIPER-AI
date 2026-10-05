package com.rudra.myassistant.util

import kotlin.math.abs
import kotlin.math.round

object ExpenseCalculator {

    fun calculateEqualSplit(totalAmount: Double, participantUids: List<String>): Map<String, Double> {
        val splitValues = mutableMapOf<String, Double>()
        if (participantUids.isEmpty()) return splitValues

        val share = round((totalAmount / participantUids.size) * 100.0) / 100.0
        var sum = 0.0

        for (uid in participantUids) {
            splitValues[uid] = share
            sum += share
        }

        // Adjust for rounding errors on the first participant
        val diff = totalAmount - sum
        if (abs(diff) > 0.001) {
            val firstUid = participantUids[0]
            val currentShare = splitValues[firstUid] ?: 0.0
            splitValues[firstUid] = round((currentShare + diff) * 100.0) / 100.0
        }

        return splitValues
    }

    fun validateCustomSplit(splitValues: Map<String, Double>, totalAmount: Double): Boolean {
        val sum = splitValues.values.sum()
        return abs(sum - totalAmount) < 0.01
    }

    fun getCompositeKey(uid1: String, uid2: String): String {
        return if (uid1 < uid2) {
            "${uid1}_$uid2"
        } else {
            "${uid2}_$uid1"
        }
    }
}
