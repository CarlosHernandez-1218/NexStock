package com.dsmg11.nexstock.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.dsmg11.nexstock.domain.model.ReceptionLocation
import com.dsmg11.nexstock.domain.repository.DeviceLocationRepository
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class DeviceLocationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : DeviceLocationRepository {

    private val locationClient =
        LocationServices.getFusedLocationProviderClient(context)

    override suspend fun getCurrentLocation():
            Result<ReceptionLocation> {

        return runCatching {

            val fineGranted =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

            val coarseGranted =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

            if (!fineGranted && !coarseGranted) {
                throw SecurityException(
                    "No se otorgó permiso de ubicación"
                )
            }

            val request =
                CurrentLocationRequest.Builder()
                    .setPriority(
                        Priority.PRIORITY_HIGH_ACCURACY
                    )
                    .setMaxUpdateAgeMillis(10_000)
                    .setDurationMillis(15_000)
                    .build()

            val location =
                locationClient
                    .getCurrentLocation(
                        request,
                        null
                    )
                    .await()
                    ?: throw IllegalStateException(
                        "No se pudo obtener la ubicación actual"
                    )

            ReceptionLocation(
                latitude = location.latitude,
                longitude = location.longitude
            )
        }
    }
}