package com.example.unidocs.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.unidocs.util.FileUtils
import java.io.IOException

data class DocumentState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val documentType: DocumentType? = null,
    val content: String? = null
)

enum class DocumentType {
    PDF, DOCX, XLSX, UNKNOWN
}

class DocumentViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(DocumentState())
    val uiState: StateFlow<DocumentState> = _uiState.asStateFlow()

    fun loadDocument(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val documentType = detectDocumentType(context, uri)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    documentType = documentType
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load document: ${e.message}"
                )
            }
        }
    }

    private suspend fun detectDocumentType(context: Context, uri: Uri): DocumentType {
        return try {
            val mimeType = context.contentResolver.getType(uri)
            when {
                mimeType?.contains("pdf") == true -> DocumentType.PDF
                mimeType?.contains("wordprocessingml") == true -> DocumentType.DOCX
                mimeType?.contains("spreadsheetml") == true -> DocumentType.XLSX
                else -> {
                    // Fallback to file extension
                    val fileName = uri.lastPathSegment ?: ""
                    when {
                        fileName.endsWith(".pdf", ignoreCase = true) -> DocumentType.PDF
                        fileName.endsWith(".docx", ignoreCase = true) -> DocumentType.DOCX
                        fileName.endsWith(".xlsx", ignoreCase = true) -> DocumentType.XLSX
                        else -> DocumentType.UNKNOWN
                    }
                }
            }
        } catch (e: Exception) {
            DocumentType.UNKNOWN
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
