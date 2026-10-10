package com.dsmg11.nexstock.domain.usecase.catalog

import com.dsmg11.nexstock.domain.model.Product
import com.dsmg11.nexstock.domain.model.ProductStatus
import com.dsmg11.nexstock.domain.model.StockLevel
import com.dsmg11.nexstock.domain.model.stockLevel
import com.dsmg11.nexstock.domain.repository.StockAlertRepository
import javax.inject.Inject

class CheckStockAlertsUseCase @Inject constructor(
    private val repository: StockAlertRepository
) {
    // Avisa una sola vez por producto cuando llega al stock mínimo.
    // Si se repone, se "olvida" y puede volver a avisar si baja otra vez.
    operator fun invoke(products: List<Product>) {
        val inAlert = products.filter {
            it.status == ProductStatus.ACTIVO && it.stockLevel != StockLevel.NORMAL
        }
        val inAlertCodes = inAlert.map { it.code }.toSet()
        val previous = repository.alertedCodes()

        val newOnes = inAlert.filter { it.code !in previous }
        val shown = newOnes.isNotEmpty() && repository.showAlert(newOnes)

        val remembered = previous.intersect(inAlertCodes) +
                if (shown) newOnes.map { it.code } else emptyList()
        repository.saveAlertedCodes(remembered)
    }
}