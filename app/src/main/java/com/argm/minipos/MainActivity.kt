package com.argm.minipos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.argm.minipos.ui.navigation.AppScreens
import com.argm.minipos.ui.screens.HomeScreen
import com.argm.minipos.ui.screens.SalesHistoryScreen
import com.argm.minipos.ui.screens.SalesScreen
import com.argm.minipos.ui.theme.MiniPOSTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MiniPOSTheme {
                MiniPosApp()
            }
        }
    }
}

@Composable
fun MiniPosApp() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = AppScreens.HOME_SCREEN,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(AppScreens.HOME_SCREEN) {
            HomeScreen(navController = navController)
        }
        composable(AppScreens.SALES_SCREEN) {
            SalesScreen(navController = navController)
        }
        composable(AppScreens.SALES_HISTORY_SCREEN) {
            SalesHistoryScreen(navController = navController)
        }
    }
}