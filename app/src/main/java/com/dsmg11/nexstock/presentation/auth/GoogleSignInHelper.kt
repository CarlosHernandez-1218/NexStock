package com.dsmg11.nexstock.presentation.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.dsmg11.nexstock.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

suspend fun obtenerTokenGoogle(context: Context): Result<String> =
    runCatching {

        val credentialManager = CredentialManager.create(context)

        // Configura la cuenta Google que podrá iniciar o crear sesión
        val googleOption = GetSignInWithGoogleOption.Builder(
            serverClientId = context.getString(R.string.default_web_client_id)
        ).build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleOption)
            .build()

        val response = credentialManager.getCredential(
            context = context,
            request = request
        )

        val credential = response.credential

        if (
            credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            error("La credencial recibida no corresponde a una cuenta de Google")
        }

        GoogleIdTokenCredential
            .createFrom(credential.data)
            .idToken
    }