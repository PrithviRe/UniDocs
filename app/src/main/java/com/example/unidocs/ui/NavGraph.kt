package com.example.unidocs.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.unidocs.ui.docx.DocxViewerScreen
import com.example.unidocs.ui.pdf.PdfViewerScreen
import com.example.unidocs.ui.screens.MainScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Pdf : Screen("pdf/{uri}") { 
        fun routeFor(u: String) = "pdf/$u" 
    }
    object Docx : Screen("docx/{uri}") {
        fun routeFor(u: String) = "docx/$u"
    }
}

@Composable
fun NavGraph(initialUri: Uri? = null) {
    val navController = rememberNavController()
    
    // Handle initial URI if provided
    LaunchedEffect(initialUri) {
        initialUri?.let { uri ->
            val mimeType = navController.context.contentResolver.getType(uri)
            val fileName = uri.lastPathSegment ?: ""
            when {
                mimeType?.contains("pdf") == true || fileName.endsWith(".pdf", ignoreCase = true) -> {
                    navController.navigate(Screen.Pdf.routeFor(Uri.encode(uri.toString())))
                }
                mimeType?.contains("word") == true || 
                mimeType?.contains("document") == true ||
                fileName.endsWith(".docx", ignoreCase = true) -> {
                    navController.navigate(Screen.Docx.routeFor(Uri.encode(uri.toString())))
                }
            }
        }
    }
    
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            MainScreen(
                onOpenPdf = { uri -> 
                    navController.navigate(Screen.Pdf.routeFor(Uri.encode(uri.toString()))) 
                },
                onOpenDocx = { uri ->
                    navController.navigate(Screen.Docx.routeFor(Uri.encode(uri.toString())))
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
        composable(
            route = Screen.Docx.route,
            arguments = listOf(navArgument("uri") { type = NavType.StringType })
        ) { backStackEntry ->
            val uri = Uri.parse(Uri.decode(backStackEntry.arguments?.getString("uri")))
            DocxViewerScreen(
                uri = uri,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
