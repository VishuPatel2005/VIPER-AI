package com.rudra.myassistant.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.rudra.myassistant.model.Expense
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ExpenseRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    suspend fun createExpense(expense: Expense): Result<Unit> {
        return try {
            val batch = db.batch()
            val expenseId = UUID.randomUUID().toString()
            val finalExpense = expense.copy(expenseId = expenseId)
            
            val expenseRef = db.collection("expenses").document(expenseId)
            batch.set(expenseRef, finalExpense)

            // Batch 2: Write to each participant's expenseRefs
            finalExpense.participants.forEach { uid ->
                val role = if (uid == finalExpense.paidBy) "payer" else "participant"
                val ref = db.collection("users").document(uid).collection("expenseRefs").document(expenseId)
                batch.set(ref, mapOf(
                    "expenseId" to expenseId,
                    "role" to role,
                    "myShare" to (finalExpense.splits[uid] ?: 0.0),
                    "timestamp" to finalExpense.timestamp,
                    "status" to "pending"
                ))
            }

            // Batch 3: Update balances
            val paidByUid = finalExpense.paidBy
            finalExpense.participants.forEach { participantUid ->
                if (participantUid != paidByUid) {
                    val users = listOf(paidByUid, participantUid).sorted()
                    val balanceId = users.joinToString("_")
                    val balanceRef = db.collection("balances").document(balanceId)
                    
                    val share = finalExpense.splits[participantUid] ?: 0.0
                    
                    // uidA owes uidB -> netAmount positive
                    // If payer is uidA, then netAmount should decrease (uidB owes uidA more)
                    // If payer is uidB, then netAmount should increase (uidA owes uidB more)
                    val uidA = users[0]
                    val increment = if (paidByUid == uidA) -share else share
                    
                    batch.set(balanceRef, mapOf(
                        "users" to users,
                        "uidA" to users[0],
                        "uidB" to users[1],
                        "netAmount" to FieldValue.increment(increment),
                        "lastUpdated" to FieldValue.serverTimestamp()
                    ), com.google.firebase.firestore.SetOptions.merge())
                }
            }

            // Batch 4: Aggregates
            val perPersonShare = finalExpense.splits[paidByUid] ?: (finalExpense.amount / finalExpense.participants.size)
            batch.update(db.collection("users").document(paidByUid), "totalOwed", FieldValue.increment(finalExpense.amount - perPersonShare))
            
            finalExpense.participants.forEach { uid ->
                if (uid != paidByUid) {
                    val share = finalExpense.splits[uid] ?: 0.0
                    batch.update(db.collection("users").document(uid), "totalOwe", FieldValue.increment(share))
                    batch.update(db.collection("users").document(uid), "hasOutstandingBalance", true)
                }
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getExpensesBetweenUsers(currentUid: String, friendUid: String): Flow<List<Expense>> = callbackFlow {
        val listener = db.collection("expenses")
            .whereArrayContains("participants", currentUid)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val expenses = snapshot?.toObjects(Expense::class.java) ?: emptyList()
                trySend(expenses.filter { it.participants.contains(friendUid) })
            }
        awaitClose { listener.remove() }
    }
}


