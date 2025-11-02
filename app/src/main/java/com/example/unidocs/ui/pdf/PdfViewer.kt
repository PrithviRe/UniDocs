package com.example.unidocs.ui.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    uri: Uri,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pageIndex by remember { mutableStateOf(0) }
    var pageCount by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var tempFile by remember { mutableStateOf<File?>(null) }
    var showThumbnails by remember { mutableStateOf(false) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var jumpPageInput by remember { mutableStateOf("") }
    var isFullscreen by remember { mutableStateOf(false) }
    
    // Zoom state
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    // Load PDF and render current page
    LaunchedEffect(uri, pageIndex) {
        isLoading = true
        error = null
        scale = 1f // Reset zoom when page changes
        offsetX = 0f
        offsetY = 0f
        
        try {
            withContext(Dispatchers.IO) {
                // Try to take persistable permission first
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    // Continue even if permission taking fails
                }
                
                // Create temp file if not exists
                if (tempFile == null) {
                    val temp = File(context.cacheDir, "tmp_pdf_${System.currentTimeMillis()}.pdf")
                    context.contentResolver.openInputStream(uri)?.use { ins ->
                        temp.outputStream().use { it.write(ins.readBytes()) }
                    }
                    tempFile = temp
                }
                
                tempFile?.let { file ->
                    val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = PdfRenderer(pfd)
                    pageCount = renderer.pageCount
                    
                    if (pageIndex in 0 until pageCount) {
                        val page = renderer.openPage(pageIndex)
                        // Render at higher quality for zoom
                        val width = (page.width * 2).coerceAtMost(4096)
                        val height = (page.height * 2).coerceAtMost(4096)
                        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        pageBitmap = bmp
                        page.close()
                    }
                    renderer.close()
                    pfd.close()
                }
            }
        } catch (e: Exception) {
            error = "Failed to load PDF: ${e.message}\n\nPlease try:\n• Using the app's file picker\n• Sharing the file from another app"
        } finally {
            isLoading = false
        }
    }

    // Cleanup temp file when composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            tempFile?.delete()
        }
    }

    // Jump to page function
    fun jumpToPage() {
        val pageNum = jumpPageInput.toIntOrNull()
        if (pageNum != null && pageNum in 1..pageCount) {
            pageIndex = pageNum - 1
            showJumpDialog = false
            jumpPageInput = ""
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!isFullscreen) {
            // Top App Bar
            TopAppBar(
                title = { 
                    Text(
                        "PDF Viewer",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showJumpDialog = true }) {
                        Icon(Icons.Default.Search, contentDescription = "Jump to page")
                    }
                    IconButton(onClick = { showThumbnails = !showThumbnails }) {
                        Icon(
                            if (showThumbnails) Icons.Default.GridOff else Icons.Default.GridOn,
                            contentDescription = "Thumbnails"
                        )
                    }
                    IconButton(onClick = { isFullscreen = true }) {
                        Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen")
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = if (isFullscreen) 0.dp else 56.dp)
        ) {
            // Navigation controls
            if (!isFullscreen && pageCount > 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (pageIndex > 0) pageIndex -= 1 },
                            enabled = pageIndex > 0
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous",
                                tint = if (pageIndex > 0) 
                                    MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }
                        
                        TextButton(
                            onClick = { showJumpDialog = true }
                        ) {
                            Text(
                                text = "${pageIndex + 1} / $pageCount",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        IconButton(
                            onClick = { if (pageIndex < pageCount - 1) pageIndex += 1 },
                            enabled = pageIndex < pageCount - 1
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next",
                                tint = if (pageIndex < pageCount - 1)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }
                    }
                }
            }

            // Fullscreen toggle button
            if (isFullscreen) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    FloatingActionButton(
                        onClick = { isFullscreen = false },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.FullscreenExit,
                            contentDescription = "Exit fullscreen",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Content area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Loading PDF...", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    error != null -> {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = "Error",
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Error",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = error!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    pageBitmap != null -> {
                        // PDF page with zoom and pan
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, _, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(0.5f, 5f)
                                    }
                                }
                        ) {
                            Image(
                                bitmap = pageBitmap!!.asImageBitmap(),
                                contentDescription = "PDF page ${pageIndex + 1}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        translationX = offsetX
                                        translationY = offsetY
                                    },
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                    else -> {
                        Text("No content to display")
                    }
                }
            }

            // Zoom controls
            if (!isFullscreen && pageBitmap != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { scale = max(0.5f, scale - 0.25f) }) {
                            Icon(Icons.Default.Remove, contentDescription = "Zoom out")
                        }
                        Text(
                            text = "${(scale * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.width(60.dp),
                            textAlign = TextAlign.Center
                        )
                        IconButton(onClick = { scale = 1f; offsetX = 0f; offsetY = 0f }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset zoom")
                        }
                        IconButton(onClick = { scale = min(5f, scale + 0.25f) }) {
                            Icon(Icons.Default.Add, contentDescription = "Zoom in")
                        }
                    }
                }
            }
        }

        // Thumbnails sidebar
        if (showThumbnails && !isFullscreen) {
            ThumbnailsSidebar(
                pageCount = pageCount,
                currentPage = pageIndex,
                onPageSelected = { 
                    pageIndex = it
                    showThumbnails = false
                },
                onDismiss = { showThumbnails = false },
                tempFile = tempFile,
                context = context
            )
        }
    }

    // Jump to page dialog
    if (showJumpDialog) {
        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            title = { Text("Jump to Page") },
            text = {
                OutlinedTextField(
                    value = jumpPageInput,
                    onValueChange = { jumpPageInput = it },
                    label = { Text("Page number (1-$pageCount)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = { jumpToPage() }) {
                    Text("Go")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJumpDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ThumbnailsSidebar(
    pageCount: Int,
    currentPage: Int,
    onPageSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    tempFile: File?,
    context: Context
) {
    val scrollState = rememberScrollState()
    
    LaunchedEffect(currentPage) {
        // Scroll to current page thumbnail
        scrollState.animateScrollTo((currentPage * 80).dp.value.toInt())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(280.dp)
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Thumbnails",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Generate thumbnails for visible pages
            val visiblePages = remember(pageCount) {
                (0 until min(pageCount, 50)) // Limit to 50 thumbnails for performance
            }

            visiblePages.forEach { index ->
                ThumbnailItem(
                    pageIndex = index,
                    isSelected = index == currentPage,
                    onClick = { onPageSelected(index) },
                    tempFile = tempFile,
                    context = context,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun ThumbnailItem(
    pageIndex: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    tempFile: File?,
    context: Context,
    modifier: Modifier = Modifier
) {
    var thumbnail by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(pageIndex) {
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            tempFile?.let { file ->
                try {
                    val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = PdfRenderer(pfd)
                    if (pageIndex < renderer.pageCount) {
                        val page = renderer.openPage(pageIndex)
                        val width = (page.width * 0.2f).toInt().coerceAtLeast(50)
                        val height = (page.height * 0.2f).toInt().coerceAtLeast(50)
                        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        thumbnail = bmp
                        page.close()
                    }
                    renderer.close()
                    pfd.close()
                } catch (e: Exception) {
                    // Thumbnail generation failed, continue without thumbnail
                }
            }
        }
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(
                2.dp,
                MaterialTheme.colorScheme.primary
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail!!.asImageBitmap(),
                    contentDescription = "Page ${pageIndex + 1}",
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(4.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Page ${pageIndex + 1}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
