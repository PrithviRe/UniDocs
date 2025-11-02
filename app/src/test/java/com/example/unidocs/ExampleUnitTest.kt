package com.example.unidocs

import com.example.unidocs.viewmodel.DocumentType
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for UniDocs application.
 */
class ExampleUnitTest {
    
    @Test
    fun documentTypeEnumValues() {
        // Test that all document types are properly defined
        assertEquals(4, DocumentType.values().size)
        assertTrue(DocumentType.values().contains(DocumentType.PDF))
        assertTrue(DocumentType.values().contains(DocumentType.DOCX))
        assertTrue(DocumentType.values().contains(DocumentType.XLSX))
        assertTrue(DocumentType.values().contains(DocumentType.UNKNOWN))
    }
    
    @Test
    fun documentTypeNames() {
        // Test document type names
        assertEquals("PDF", DocumentType.PDF.name)
        assertEquals("DOCX", DocumentType.DOCX.name)
        assertEquals("XLSX", DocumentType.XLSX.name)
        assertEquals("UNKNOWN", DocumentType.UNKNOWN.name)
    }
}