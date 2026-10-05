package com.rudra.myassistant.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.rudra.myassistant.model.Settlement
import kotlinx.coroutines.tasks.await
import java.util.UUID

class SettlementRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    suspend fun settleBalance(currentUid: String, friendUid: String, amount: Double): Result<Unit> {
        return try {
            val batch = db.batch()
            val settlementId = UUID.randomUUID().toString()
            val users = listOf(currentUid, friendUid).sorted()
            val balanceId = users.joinToString("_")
            
            val settlementRef = db.collection("settlements").document(settlementId)
            val settlement = Settlement(
                settlementId = settlementId,
                fromUid = currentUid,
                toUid = friendUid,
                amount = amount,
                balanceIdRef = balanceId
            )
            batch.set(settlementRef, settlement)

            // Batch 2: Update Balance
            // Logic: if currentUid is uidA, and he pays uidB, then he owes less (netAmount decreases)
            val uidA = users[0]
            val increment = if (currentUid == uidA) -amount else amount
            batch.update(db.collection("balances").document(balanceId), "netAmount", FieldValue.increment(increment))

            // Batch 3: Aggregates
            batch.update(db.collection("users").document(currentUid), "totalOwe", FieldValue.increment(-amount))
            batch.update(db.collection("users").document(friendUid), "totalOwed", FieldValue.increment(-amount))

            // Batch 4: Status check (Optional/Aggressive settlement)
            // Note: complex per-expense settlement is often done via Cloud functions, but we can update local flags
            
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
