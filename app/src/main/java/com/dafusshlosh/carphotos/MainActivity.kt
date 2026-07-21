package com.dafusshlosh.carphotos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dafusshlosh.carphotos.ui.CaptureScreen
import com.dafusshlosh.carphotos.ui.GalleryScreen
import com.dafusshlosh.carphotos.ui.HomeScreen
import com.dafusshlosh.carphotos.ui.PhotoViewerScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier) {
                    AppNav()
                }
            }
        }
    }
}

@Composable
fun AppNav() {
    val navController: NavHostController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(navController) }
        composable("capture") { CaptureScreen(navController) }
        composable("gallery/{carId}") { backStackEntry ->
            val carId = backStackEntry.arguments?.getString("carId")?.toLongOrNull() ?: 0L
            GalleryScreen(navController, carId)
        }
        composable("viewer/{carId}/{startIndex}") { backStackEntry ->
            val carId = backStackEntry.arguments?.getString("carId")?.toLongOrNull() ?: 0L
            val startIndex = backStackEntry.arguments?.getString("startIndex")?.toIntOrNull() ?: 0
            PhotoViewerScreen(carId, startIndex)
        }
    }
}
