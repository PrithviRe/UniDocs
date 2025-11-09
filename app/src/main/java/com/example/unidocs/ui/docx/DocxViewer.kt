package com.example.unidocs.ui.docx

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFRun
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

data class DocxParagraph(
    val id: Int,
    var text: String,
    var isBold: Boolean = false,
    var isItalic: Boolean = false,
    var fontSize: Int = 11,
    var alignment: ParagraphAlignment = ParagraphAlignment.LEFT
)

enum class ParagraphAlignment {
    LEFT, CENTER, RIGHT, JUSTIFY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocxViewerScreen(
    uri: Uri,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var paragraphs by remember { mutableStateOf<List<DocxParagraph>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isEditing by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var saveSuccess by remember { mutableStateOf(false) }
    var tempFile by remember { mutableStateOf<File?>(null) }
    var editedParagraphId by remember { mutableStateOf<Int?>(null) }
    
    // Zoom and scroll state
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    val scrollState = rememberScrollState()
    var isFullscreen by remember { mutableStateOf(false) }
    var canScroll by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    
    // Reset offset when scale resets
    LaunchedEffect(scale) {
        if (scale <= 1f) {
            offsetX = 0f
            offsetY = 0f
            canScroll = true
        } else {
            canScroll = false
        }
    }
    
    // Load DOCX file
    LaunchedEffect(uri) {
        isLoading = true
        error = null
        scale = 1f
        offsetX = 0f
        offsetY = 0f
        
        try {
            withContext(Dispatchers.IO) {
                // Try to take persistable permission
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or 
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    // Continue even if permission taking fails
                }
                
                // Create temp file
                val temp = File(context.cacheDir, "tmp_docx_${System.currentTimeMillis()}.docx")
                context.contentResolver.openInputStream(uri)?.use { ins ->
                    temp.outputStream().use { out ->
                        ins.copyTo(out)
                    }
                }
                tempFile = temp
                
                // Load DOCX content
                FileInputStream(temp).use { fis ->
                    val document = XWPFDocument(fis)
                    val loadedParagraphs = mutableListOf<DocxParagraph>()
                    
                    document.paragraphs.forEachIndexed { index, para ->
                        val text = para.text
                        if (text.isNotBlank() || para.runs.isNotEmpty()) {
                            val firstRun = para.runs.firstOrNull()
                            loadedParagraphs.add(
                                DocxParagraph(
                                    id = index,
                                    text = text.ifBlank { "\n" },
                                    isBold = firstRun?.isBold ?: false,
                                    isItalic = firstRun?.isItalic ?: false,
                                    fontSize = firstRun?.fontSize ?: 11,
                                    alignment = when (para.alignment) {
                                        org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER -> ParagraphAlignment.CENTER
                                        org.apache.poi.xwpf.usermodel.ParagraphAlignment.RIGHT -> ParagraphAlignment.RIGHT
                                        org.apache.poi.xwpf.usermodel.ParagraphAlignment.BOTH -> ParagraphAlignment.JUSTIFY
                                        else -> ParagraphAlignment.LEFT
                                    }
                                )
                            )
                        } else {
                            // Empty paragraph for spacing
                            loadedParagraphs.add(
                                DocxParagraph(
                                    id = index,
                                    text = "\n",
                                    fontSize = 11
                                )
                            )
                        }
                    }
                    
                    if (loadedParagraphs.isEmpty()) {
                        loadedParagraphs.add(
                            DocxParagraph(
                                id = 0,
                                text = "",
                                fontSize = 11
                            )
                        )
                    }
                    
                    paragraphs = loadedParagraphs
                    document.close()
                }
            }
        } catch (e: Exception) {
            error = "Failed to load DOCX: ${e.message}\n\nPlease try:\n• Using the app's file picker\n• Sharing the file from another app"
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }
    
    // Cleanup temp file
    DisposableEffect(Unit) {
        onDispose {
            // Don't delete temp file if we're editing, only on dispose
        }
    }
    
    // Save DOCX function
    fun saveDocx() {
        if (tempFile == null) return
        
        isSaving = true
        saveSuccess = false
        
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val doc = XWPFDocument(FileInputStream(tempFile))
                
                // Clear existing paragraphs if needed and recreate
                val paraCount = doc.paragraphs.size
                
                // Update or create paragraphs
                paragraphs.forEachIndexed { index, para ->
                    val docPara = if (index < paraCount) {
                        doc.paragraphs[index]
                    } else {
                        // Create new paragraph
                        doc.createParagraph()
                    }
                    
                    // Clear existing runs from existing paragraph
                    if (index < paraCount) {
                        while (docPara.runs.isNotEmpty()) {
                            docPara.removeRun(0)
                        }
                    }
                    
                    // Create run with text
                    val run = docPara.createRun()
                    run.setText(para.text.replace("\n", ""))
                    run.isBold = para.isBold
                    run.isItalic = para.isItalic
                    if (para.fontSize > 0) {
                        run.fontSize = para.fontSize
                    }
                    
                    // Set alignment
                    docPara.alignment = when (para.alignment) {
                        ParagraphAlignment.CENTER -> org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER
                        ParagraphAlignment.RIGHT -> org.apache.poi.xwpf.usermodel.ParagraphAlignment.RIGHT
                        ParagraphAlignment.JUSTIFY -> org.apache.poi.xwpf.usermodel.ParagraphAlignment.BOTH
                        else -> org.apache.poi.xwpf.usermodel.ParagraphAlignment.LEFT
                    }
                }
                
                // Remove excess paragraphs if we have fewer now
                while (doc.paragraphs.size > paragraphs.size) {
                    val lastIndex = doc.paragraphs.size - 1
                    val body = doc.bodyElements[lastIndex]
                    doc.removeBodyElement(lastIndex)
                }
                
                // Save to temp file first
                FileOutputStream(tempFile).use { fos ->
                    doc.write(fos)
                }
                doc.close()
                
                // Copy back to original URI
                tempFile?.inputStream()?.use { ins ->
                    context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                        ins.copyTo(out)
                    }
                }
                
                withContext(Dispatchers.Main) {
                    saveSuccess = true
                    isSaving = false
                    isEditing = false
                    editedParagraphId = null
                }
                
                // Reset success message after 2 seconds
                delay(2000)
                withContext(Dispatchers.Main) {
                    saveSuccess = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    error = "Failed to save DOCX: ${e.message}"
                    isSaving = false
                }
                e.printStackTrace()
            }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (!isFullscreen) {
            TopAppBar(
                title = {
                    Text(
                        "DOCX Viewer",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(
                            onClick = { saveDocx() },
                            enabled = !isSaving
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Save, contentDescription = "Save")
                            }
                        }
                        IconButton(onClick = { 
                            isEditing = false
                            editedParagraphId = null
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel editing")
                        }
                    } else {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { isFullscreen = true }) {
                            Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen")
                        }
                    }
                }
            )
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = if (isFullscreen) 0.dp else 56.dp)
        ) {
            // Save success message
            if (saveSuccess) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Document saved successfully!",
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            // Content area with zoom and scroll
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Loading DOCX...", style = MaterialTheme.typography.bodyLarge)
                            }
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
                    paragraphs.isNotEmpty() -> {
                        // Document content with zoom, pan, and scroll support
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(isEditing, scale) {
                                    if (!isEditing) {
                                        detectTransformGestures { _, pan, zoom, _ ->
                                            if (zoom != 1f) {
                                                // Pinch to zoom
                                                val newScale = (scale * zoom).coerceIn(0.5f, 3f)
                                                scale = newScale
                                            } else if (scale > 1f) {
                                                // Pan when zoomed in (only allow pan when zoomed)
                                                offsetX = (offsetX + pan.x).coerceIn(-2000f, 2000f)
                                                offsetY = (offsetY + pan.y).coerceIn(-2000f, 2000f)
                                            }
                                        }
                                    }
                                }
                        ) {
                            // Content area - unified approach for all modes
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        // Only enable scrolling when not zoomed and not editing
                                        if (canScroll && !isEditing && scale <= 1f) {
                                            Modifier.verticalScroll(scrollState)
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .padding(16.dp)
                                    .graphicsLayer {
                                        if (!isEditing) {
                                            scaleX = scale
                                            scaleY = scale
                                            if (scale > 1f) {
                                                translationX = offsetX
                                                translationY = offsetY
                                            }
                                        }
                                    }
                            ) {
                                paragraphs.forEachIndexed { index, paragraph ->
                                    DocxParagraphView(
                                        paragraph = paragraph,
                                        isEditing = isEditing && editedParagraphId == paragraph.id,
                                        onTextChange = { newText ->
                                            paragraphs = paragraphs.map {
                                                if (it.id == paragraph.id) {
                                                    it.copy(text = newText)
                                                } else it
                                            }.toList()
                                        },
                                        onStartEdit = {
                                            editedParagraphId = paragraph.id
                                            isEditing = true
                                            // Reset zoom when entering edit mode
                                            scale = 1f
                                            offsetX = 0f
                                            offsetY = 0f
                                            canScroll = true
                                        },
                                        onStopEdit = {
                                            editedParagraphId = null
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                    else -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No content to display")
                        }
                    }
                }
            }
            
            // Zoom controls
            if (!isFullscreen && paragraphs.isNotEmpty() && !isEditing) {
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
                        IconButton(
                            onClick = { 
                                val newScale = max(0.5f, scale - 0.25f)
                                scale = newScale
                                if (newScale <= 1f) {
                                    offsetX = 0f
                                    offsetY = 0f
                                    canScroll = true
                                }
                            }
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Zoom out")
                        }
                        Text(
                            text = "${(scale * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.width(60.dp),
                            textAlign = TextAlign.Center
                        )
                        IconButton(onClick = { 
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                            canScroll = true
                            // Reset scroll to top when resetting zoom
                            coroutineScope.launch {
                                scrollState.animateScrollTo(0)
                            }
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset zoom")
                        }
                        IconButton(
                            onClick = { 
                                val newScale = min(3f, scale + 0.25f)
                                scale = newScale
                                if (newScale > 1f) {
                                    canScroll = false
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Zoom in")
                        }
                    }
                }
            }
        }
        
        // Fullscreen exit button
        if (isFullscreen) {
            FloatingActionButton(
                onClick = { isFullscreen = false },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(40.dp)
            ) {
                Icon(
                    Icons.Default.FullscreenExit,
                    contentDescription = "Exit fullscreen",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun DocxParagraphView(
    paragraph: DocxParagraph,
    isEditing: Boolean,
    onTextChange: (String) -> Unit,
    onStartEdit: () -> Unit,
    onStopEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var textState by remember(paragraph.id) { mutableStateOf(paragraph.text) }
    
    LaunchedEffect(paragraph.id) {
        textState = paragraph.text
    }
    
    if (isEditing) {
        Card(
            modifier = modifier,
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                OutlinedTextField(
                    value = textState,
                    onValueChange = { newText ->
                        textState = newText
                        onTextChange(newText)
                    },
                    textStyle = TextStyle(
                        fontSize = (paragraph.fontSize * 1.2).sp,
                        fontWeight = if (paragraph.isBold) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (paragraph.isItalic) 
                            androidx.compose.ui.text.font.FontStyle.Italic 
                        else androidx.compose.ui.text.font.FontStyle.Normal
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 40.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            onStopEdit()
                        }
                    ),
                    label = { Text("Edit paragraph") },
                    maxLines = 10,
                    singleLine = false
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Tap Done on keyboard or outside to finish",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    TextButton(onClick = {
                        keyboardController?.hide()
                        onStopEdit()
                    }) {
                        Text("Done")
                    }
                }
            }
        }
    } else {
        Card(
            onClick = onStartEdit,
            modifier = modifier,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Text(
                text = paragraph.text.ifBlank { "\n" },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                fontSize = (paragraph.fontSize * 1.2).sp,
                fontWeight = if (paragraph.isBold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (paragraph.isItalic) 
                    androidx.compose.ui.text.font.FontStyle.Italic 
                else androidx.compose.ui.text.font.FontStyle.Normal,
                textAlign = when (paragraph.alignment) {
                    ParagraphAlignment.CENTER -> TextAlign.Center
                    ParagraphAlignment.RIGHT -> TextAlign.Right
                    ParagraphAlignment.JUSTIFY -> TextAlign.Justify
                    else -> TextAlign.Left
                }
            )
        }
    }
}

