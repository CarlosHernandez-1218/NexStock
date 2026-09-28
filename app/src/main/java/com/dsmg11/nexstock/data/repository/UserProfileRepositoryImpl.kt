package com.dsmg11.nexstock.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.dsmg11.nexstock.data.local.UserProfileDao
import com.dsmg11.nexstock.data.local.toDomain
import com.dsmg11.nexstock.data.local.toEntity
import com.dsmg11.nexstock.domain.model.ProductLine
import com.dsmg11.nexstock.domain.model.UserProfile
import com.dsmg11.nexstock.domain.model.UserRole
import com.dsmg11.nexstock.domain.repository.UserProfileRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storageMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject

class UserProfileRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val profileDao: UserProfileDao
) : UserProfileRepository {

    private val users get() = firestore.collection(USERS_COLLECTION)

    // Offline-first: la app lee de Room; Firestore solo mantiene Room actualizado
    override fun observeProfile(uid: String): Flow<UserProfile?> = channelFlow {
        launch {
            remoteProfile(uid)
                .catch { /* Sin conexión: se sigue mostrando la copia local */ }
                .collect { profile -> profile?.let { profileDao.upsert(it.toEntity()) } }
        }
        profileDao.observe(uid)
            .map { it?.toDomain() }
            .collect { send(it) }
    }

    private fun remoteProfile(uid: String): Flow<UserProfile?> = callbackFlow {
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
            // 1. Se guarda en Room al instante (funciona sin internet)
            profileDao.updateBasicInfo(uid, fullName, phone)
            // 2. Firestore lo encola y lo sube cuando haya conexión (sin await para no bloquear offline)
            users.document(uid).update(mapOf("fullName" to fullName, "phone" to phone))
            Unit
        }

    // Solo el Administrador puede leer la lista completa (lo garantizan las reglas de Firestore)
    override fun observeAllProfiles(): Flow<List<UserProfile>> = callbackFlow {
        val registration = users.orderBy("email").addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.documents?.mapNotNull { it.toUserProfile() }.orEmpty())
        }
        awaitClose { registration.remove() }
    }

    override suspend fun updateRoleAndLines(
        uid: String,
        role: UserRole,
        lines: List<ProductLine>
    ): Result<Unit> = runCatching {
        users.document(uid).update(
            mapOf(
                "role" to role.name,
                "assignedLines" to lines.map { it.name }
            )
        ).await()
        Unit
    }

    // Sube la foto a Firebase Storage y guarda su URL en el perfil de Firestore
    override suspend fun uploadProfilePhoto(uid: String, localUri: String): Result<String> =
        runCatching {
            val bytes = withContext(Dispatchers.IO) { compressImage(localUri) }
            val photoRef = storage.reference.child("profile_photos/$uid.jpg")
            photoRef.putBytes(bytes, storageMetadata { contentType = "image/jpeg" }).await()

            // "&v=" cambia en cada subida para que Coil no muestre la foto anterior desde su caché
            val url = "${photoRef.downloadUrl.await()}&v=${System.currentTimeMillis()}"
            users.document(uid).update("photoUrl", url).await()
            url
        }

    // Reduce la foto a máx. 512 px, corrige la rotación de la cámara y la comprime en JPEG
    private fun compressImage(localUri: String): ByteArray {
        val uri = Uri.parse(localUri)
        val resolver = context.contentResolver

        // 1. Leer solo el tamaño, para no cargar en memoria una foto enorme
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sampleSize = 1
        while (bounds.outWidth / (sampleSize * 2) >= MAX_PHOTO_SIZE &&
            bounds.outHeight / (sampleSize * 2) >= MAX_PHOTO_SIZE
        ) {
            sampleSize *= 2
        }

        // 2. Cargar la imagen ya reducida
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sampleSize })
        } ?: error("No se pudo leer la imagen")

        // 3. Rotación que guardó la cámara (EXIF)
        val rotation = resolver.openInputStream(uri)?.use {
            when (ExifInterface(it).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f

        // 4. Escalar, rotar y comprimir en JPEG
        val scale = minOf(1f, MAX_PHOTO_SIZE.toFloat() / maxOf(decoded.width, decoded.height))
        val matrix = Matrix().apply {
            postRotate(rotation)
            postScale(scale, scale)
        }
        val result = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)

        return ByteArrayOutputStream().use { out ->
            result.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            out.toByteArray()
        }
    }

    private companion object {
        const val USERS_COLLECTION = "users"
        const val MAX_PHOTO_SIZE = 512
        const val JPEG_QUALITY = 80
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