package com.rudra.myassistant.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.rudra.myassistant.model.Balance
import com.rudra.myassistant.model.BalanceDirection
import com.rudra.myassistant.model.FriendBalance
import com.rudra.myassistant.model.FriendUser
import com.rudra.myassistant.model.UserProfile
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.math.abs

class BalanceRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    fun getBalancesForUser(currentUid: String): Flow<List<FriendBalance>> = callbackFlow {
        val listener = db.collection("balances")
            .whereArrayContains("users", currentUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                
                val balances = snapshot?.toObjects(Balance::class.java) ?: emptyList()
                if (balances.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                // Resolving friend names for display
                val friendUids = balances.map { b ->
                    if (b.uidA == currentUid) b.uidB else b.uidA
                }

                db.collection("users")
                    .whereIn("uid", friendUids)
                    .get()
                    .addOnSuccessListener { userSnap ->
                        val userMap = userSnap.documents.mapNotNull { doc ->
                            if (doc.exists()) {
                                FriendUser(
                                    uid = doc.id,
                                    displayName = doc.getString("name") ?: "User",
                                    username = doc.getString("username") ?: "",
                                    photoUrl = doc.getString("profile_photo_url") ?: "",
                                    isOnline = "online" == doc.getString("status")
                                )
                            } else null
                        }.associateBy { it.uid }
                        
                        val friendBalances = balances.mapNotNull { b ->
                            val friendUid = if (b.uidA == currentUid) b.uidB else b.uidA
                            val friendProfile = userMap[friendUid] ?: return@mapNotNull null
                            
                            val (amount, direction) = if (b.uidA == currentUid) {
                                if (b.netAmount > 0) b.netAmount to BalanceDirection.YOU_OWE
                                else if (b.netAmount < 0) abs(b.netAmount) to BalanceDirection.THEY_OWE
                                else 0.0 to BalanceDirection.SETTLED
                            } else {
                                if (b.netAmount > 0) b.netAmount to BalanceDirection.THEY_OWE
                                else if (b.netAmount < 0) abs(b.netAmount) to BalanceDirection.YOU_OWE
                                else 0.0 to BalanceDirection.SETTLED
                            }

                            FriendBalance(friendProfile, amount, direction, b.users.joinToString("_"))
                        }
                        trySend(friendBalances)
                    }
            }
        awaitClose { listener.remove() }
    }

    suspend fun getTotalOutstandingOweAmount(currentUid: String): Pair<Double, UserProfile?> {
        val snapshot = db.collection("balances")
            .whereArrayContains("users", currentUid)
            .get()
            .await()
        
        val balances = snapshot.toObjects(Balance::class.java)
        var total = 0.0
        var highestOweAmount = 0.0
        var topFriendUid: String? = null

        balances.forEach { b ->
            val amnt = if (b.uidA == currentUid && b.netAmount > 0) b.netAmount
                      else if (b.uidB == currentUid && b.netAmount < 0) abs(b.netAmount)
                      else 0.0
            total += amnt
            if (amnt > highestOweAmount) {
                highestOweAmount = amnt
                topFriendUid = if (b.uidA == currentUid) b.uidB else b.uidA
            }
        }

        val friendProfile = topFriendUid?.let {
            db.collection("users").document(it).get().await().toObject(UserProfile::class.java)
        }

        return total to friendProfile
    }
}
