package com.rudra.myassistant.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.rudra.myassistant.model.UserProfile
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

import com.google.android.gms.tasks.Tasks
import com.rudra.myassistant.model.FriendUser
import kotlinx.coroutines.tasks.await

class FriendRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private var cachedFriends: List<FriendUser> = emptyList()

    fun getFriendsForExpense(currentUid: String): Flow<List<FriendUser>> = callbackFlow {
        // Return cache immediately if available
        if (cachedFriends.isNotEmpty()) {
            trySend(cachedFriends)
        }

        val ref = db.collection("friends")
            .whereEqualTo("userUid", currentUid)
        
        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val friendUids = snapshot.documents.map { doc ->
                    doc.getString("friendUid")
                }.filterNotNull()

                if (friendUids.isEmpty()) {
                    cachedFriends = emptyList()
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                // Batch fetching friend profiles
                val tasks = friendUids.map { uid -> db.collection("users").document(uid).get() }
                Tasks.whenAllSuccess<DocumentSnapshot>(tasks).addOnSuccessListener { documents ->
                    val profiles = documents.mapNotNull { doc ->
                        if (doc.exists()) {
                            FriendUser(
                                uid = doc.id,
                                displayName = doc.getString("name") ?: "User",
                                username = doc.getString("username") ?: "",
                                photoUrl = doc.getString("profile_photo_url") ?: "",
                                isOnline = "online" == doc.getString("status")
                            )
                        } else null
                    }.sortedBy { it.displayName }

                    cachedFriends = profiles
                    trySend(profiles)
                }
            }
        }
        awaitClose { listener.remove() }
    }

    // Keep legacy for compatibility if needed, or update to use FriendUser
    fun getAcceptedFriends(currentUid: String): Flow<List<UserProfile>> = callbackFlow {
        val listener = db.collection("friends")
            .whereEqualTo("userUid", currentUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val friendUids = snapshot.documents.map { doc ->
                        doc.getString("friendUid")
                    }.filterNotNull()

                    if (friendUids.isEmpty()) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }

                    db.collection("users")
                        .whereIn("uid", friendUids)
                        .get()
                        .addOnSuccessListener { userSnap ->
                            val profiles = userSnap.toObjects(UserProfile::class.java)
                            trySend(profiles)
                        }
                        .addOnFailureListener {
                            trySend(emptyList())
                        }
                }
            }
        awaitClose { listener.remove() }
    }
}
