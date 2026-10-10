package com.dsmg11.nexstock.presentation.catalog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

// MOV-21: abre el escáner de Google Play services con la cámara.
// El escáner es un módulo que Google descarga aparte; si no está, se pide la descarga primero.
@Composable
fun rememberBarcodeScanner(
    onScanned: (String) -> Unit,
    onError: (String) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val currentOnScanned by rememberUpdatedState(onScanned)
    val currentOnError by rememberUpdatedState(onError)

    val scanner = remember(context) {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A)
            .build()
        GmsBarcodeScanning.getClient(context, options)
    }
    val moduleInstall = remember(context) { ModuleInstall.getClient(context) }

    fun openScanner() {
        scanner.startScan()
            .addOnSuccessListener { barcode -> barcode.rawValue?.let(currentOnScanned) }
            .addOnFailureListener { e -> currentOnError("No se pudo abrir el escáner: ${e.message}") }
        // Si el usuario cierra el escáner sin leer nada, no pasa nada
    }

    return {
        moduleInstall.areModulesAvailable(scanner)
            .addOnSuccessListener { response ->
                if (response.areModulesAvailable()) {
                    openScanner()
                } else {
                    // Primera vez: se pide a Google Play services que descargue el escáner
                    val request = ModuleInstallRequest.newBuilder().addApi(scanner).build()
                    moduleInstall.installModules(request)
                        .addOnSuccessListener { result ->
                            if (result.areModulesAlreadyInstalled()) openScanner()
                            else currentOnError("Descargando el escáner de Google… vuelve a tocar el ícono en unos segundos.")
                        }
                        .addOnFailureListener { e ->
                            currentOnError("No se pudo descargar el escáner: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                currentOnError("Google Play services no está disponible: ${e.message}")
            }
    }
}