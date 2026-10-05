package com.dsmg11.nexstock.presentation.catalog

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dsmg11.nexstock.domain.model.StockLevel
import com.dsmg11.nexstock.ui.theme.StockAgotado
import com.dsmg11.nexstock.ui.theme.StockBajo
import com.dsmg11.nexstock.ui.theme.StockNormal

@Composable
fun StockBadge(level: StockLevel, modifier: Modifier = Modifier) {
    // El ámbar necesita texto oscuro para que se lea bien
    val (container, content) = when (level) {
        StockLevel.NORMAL -> StockNormal to Color.White
        StockLevel.BAJO -> StockBajo to Color.Black
        StockLevel.AGOTADO -> StockAgotado to Color.White
    }
    Surface(
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Text(
            level.displayName,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}