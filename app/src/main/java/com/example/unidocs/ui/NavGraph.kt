package com.example.unidocs.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.unidocs.ui.pdf.PdfViewerScreen
import com.example.unidocs.ui.screens.MainScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Pdf : Screen("pdf/{uri}") { 
        fun routeFor(u: String) = "pdf/$u" 
    }
}

@Composable
fun NavGraph(initialUri: Uri? = null) {
    val navController = rememberNavController()
    
    // Handle initial URI if provided
    LaunchedEffect(initialUri) {
        initialUri?.let { uri ->
            val mimeType = navController.context.contentResolver.getType(uri)
            if (mimeType?.contains("pdf") == true || 
                (uri.lastPathSegment ?: "").endsWith(".pdf", ignoreCase = true)) {
                navController.navigate(Screen.Pdf.routeFor(Uri.encode(uri.toString())))
            }
        }
    }
    
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            MainScreen(
                onOpenPdf = { uri -> 
                    navController.navigate(Screen.Pdf.routeFor(Uri.encode(uri.toString()))) 
                }
            )
        }
        composable(
            route = Screen.Pdf.route,
            arguments = listOf(navArgument("uri") { type = NavType.StringType })
        ) { backStackEntry ->
            val uri = Uri.parse(Uri.decode(backStackEntry.arguments?.getString("uri")))
            PdfViewerScreen(
                uri = uri,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
