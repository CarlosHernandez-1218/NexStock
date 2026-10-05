package com.dsmg11.nexstock.domain.usecase.catalog

import com.dsmg11.nexstock.domain.model.Product
import com.dsmg11.nexstock.domain.model.StockFilters
import com.dsmg11.nexstock.domain.model.stockLevel
import javax.inject.Inject

class FilterProductsUseCase @Inject constructor() {

    operator fun invoke(products: List<Product>, filters: StockFilters): List<Product> {
        if (filters.isEmpty) return products
        return products.filter { p ->
            (filters.brands.isEmpty() || p.brand in filters.brands) &&
                    (filters.categories.isEmpty() || p.category in filters.categories) &&
                    (filters.rotations.isEmpty() || p.rotation in filters.rotations) &&
                    (filters.levels.isEmpty() || p.stockLevel in filters.levels)
        }
    }
}