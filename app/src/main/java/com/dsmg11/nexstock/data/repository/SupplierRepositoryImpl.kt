package com.dsmg11.nexstock.data.repository

import com.dsmg11.nexstock.data.remote.ruc.RucApiService
import com.dsmg11.nexstock.domain.model.RucInfo
import com.dsmg11.nexstock.domain.model.Supplier
import com.dsmg11.nexstock.domain.repository.SupplierRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
class SupplierRepositoryImpl @Inject constructor(
    private val rucApiService: RucApiService,
    private val firestore: FirebaseFirestore
) : SupplierRepository {

    private val proveedoresCollection =
        firestore.collection("proveedores")

    override suspend fun consultarRuc(ruc: String): Result<RucInfo> {
        return try {
            val response = rucApiService.consultarRuc(ruc)

            val rucInfo = RucInfo(
                ruc = response.numeroDocumento,
                razonSocial = response.razonSocial,
                estado = response.estado,
                condicion = response.condicion,
                direccion = response.direccion.orEmpty()
            )

            Result.success(rucInfo)
        } catch (e: HttpException) {
            val mensaje = when (e.code()) {
                401, 403 -> "No se pudo autenticar con el servicio de consulta RUC"
                404 -> "No se encontró información para el RUC ingresado"
                429 -> "Se alcanzó el límite temporal de consultas RUC"
                else -> "No se pudo consultar el RUC. Código ${e.code()}"
            }

            Result.failure(
                IllegalStateException(mensaje, e)
            )
        } catch (e: IOException) {
            Result.failure(
                IllegalStateException(
                    "No se pudo conectar con el servicio de consulta RUC",
                    e
                )
            )
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException(
                    "Ocurrió un error al consultar el RUC",
                    e
                )
            )
        }
    }

    override suspend fun existeProveedor(ruc: String): Result<Boolean> {
        return try {
            val documento = proveedoresCollection
                .document(ruc)
                .get()
                .await()

            Result.success(documento.exists())
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException(
                    "No se pudo verificar si el proveedor ya está registrado",
                    e
                )
            )
        }
    }

    override suspend fun registrarProveedor(
        proveedor: Supplier
    ): Result<Unit> {
        return try {
            proveedoresCollection
                .document(proveedor.ruc)
                .set(proveedor)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException(
                    "No se pudo registrar el proveedor",
                    e
                )
            )
        }

    }
    // Lista en tiempo real de los proveedores registrados
    override fun observarProveedores(): Flow<List<Supplier>> = callbackFlow {
        val registration = proveedoresCollection
            .orderBy("razonSocial")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(Supplier::class.java).orEmpty())
            }
        awaitClose { registration.remove() }
    }
}