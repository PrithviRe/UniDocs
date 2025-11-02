package com.example.unidocs

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.example.unidocs.ui.NavGraph
import com.example.unidocs.ui.theme.UniDocsTheme

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Permission granted, continue with file operations
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            UniDocsTheme {
                var initialUri by remember { mutableStateOf<Uri?>(null) }
                
                // Handle incoming intent
                LaunchedEffect(Unit) {
                    intent?.data?.let { uri ->
                        // Take persistable permission for the URI
                        try {
                            contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            )
                            initialUri = uri
                        } catch (e: Exception) {
                            // If we can't take permission, still try to use the URI
                            initialUri = uri
                        }
                    }
                }
                
                NavGraph(initialUri = initialUri)
            }
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.data?.let { uri ->
            // Take persistable permission for the URI
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Continue even if permission taking fails
            }
        }
    }
}
