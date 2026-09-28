package com.dsmg11.nexstock.presentation.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.ui.graphics.vector.ImageVector
import com.dsmg11.nexstock.domain.model.AppFeature

val AppFeature.icon: ImageVector
    get() = when (this) {
        AppFeature.CATALOGO -> Icons.Filled.Inventory2
        AppFeature.STOCK -> Icons.Filled.Warehouse
        AppFeature.ORDENES_COMPRA -> Icons.Filled.ShoppingCart
        AppFeature.RECEPCION -> Icons.Filled.QrCodeScanner
        AppFeature.DEVOLUCIONES -> Icons.AutoMirrored.Filled.AssignmentReturn
        AppFeature.DASHBOARD -> Icons.Filled.Dashboard
        AppFeature.ASISTENTE -> Icons.Filled.SmartToy
        AppFeature.USUARIOS -> Icons.Filled.ManageAccounts
    }