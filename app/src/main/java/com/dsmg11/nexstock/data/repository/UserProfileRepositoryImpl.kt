package com.dsmg11.nexstock.data.repository

import com.dsmg11.nexstock.domain.model.ProductLine
import com.dsmg11.nexstock.domain.model.UserProfile
import com.dsmg11.nexstock.domain.model.UserRole
import com.dsmg11.nexstock.domain.repository.UserProfileRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserProfileRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserProfileRepository {

    private val users get() = firestore.collection(USERS_COLLECTION)

    // Escucha el perfil en tiempo real: si el Administrador cambia el rol, la app se entera al instante
    override fun observeProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        val registration = users.document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.toUserProfile())
        }
        awaitClose { registration.remove() }
    }

    override suspend fun createProfileIfMissing(uid: String, email: String): Result<Unit> = runCatching {
        val document = users.document(uid)
        if (!document.get().await().exists()) {
            document.set(
                mapOf(
                    "email" to email,
                    "fullName" to "",
                    "phone" to "",
                    "role" to UserRole.ALMACENERO.name,   // Todo usuario nuevo empieza como Almacenero
                    "assignedLines" to emptyList<String>(),
                    "photoUrl" to null,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    override suspend fun updateBasicInfo(uid: String, fullName: String, phone: String): Result<Unit> =
        runCatching {
            users.document(uid).update(
                mapOf("fullName" to fullName.trim(), "phone" to phone.trim())
            ).await()
            Unit
        }

    private companion object {
        const val USERS_COLLECTION = "users"
    }
}

private fun DocumentSnapshot.toUserProfile(): UserProfile? {
    if (!exists()) return null
    return UserProfile(
        uid = id,
        email = getString("email").orEmpty(),
        fullName = getString("fullName").orEmpty(),
        phone = getString("phone").orEmpty(),
        role = UserRole.fromName(getString("role")),
        assignedLines = (get("assignedLines") as? List<*>)
            ?.mapNotNull { (it as? String)?.let(ProductLine::fromName) }
            .orEmpty(),
        photoUrl = getString("photoUrl")
    )
}