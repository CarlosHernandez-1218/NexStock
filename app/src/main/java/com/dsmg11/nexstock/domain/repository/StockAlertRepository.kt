package com.dsmg11.nexstock.domain.repository

import com.dsmg11.nexstock.domain.model.Product

interface StockAlertRepository {
    fun alertedCodes(): Set<String>
    fun saveAlertedCodes(codes: Set<String>)
    // Devuelve true si la notificación se mostró (false si no hay permiso)
    fun showAlert(products: List<Product>): Boolean
}