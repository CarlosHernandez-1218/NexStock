package com.dsmg11.nexstock.domain.usecase.profile

import com.dsmg11.nexstock.domain.repository.UserProfileRepository
import javax.inject.Inject

class UploadProfilePhotoUseCase @Inject constructor(
    private val repository: UserProfileRepository
) {
    suspend operator fun invoke(uid: String, localUri: String): Result<String> =
        repository.uploadProfilePhoto(uid, localUri)
            .recoverCatching { throw Exception("No se pudo subir la foto. Revisa tu conexión a internet.") }
}