package com.dsmg11.nexstock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.dsmg11.nexstock.presentation.navigation.NexStockNavHost
import com.dsmg11.nexstock.ui.theme.NexStockTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NexStockTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NexStockNavHost(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}