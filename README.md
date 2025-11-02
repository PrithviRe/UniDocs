# UniDocs - Universal Document Viewer & Editor

A modern Android application built with Jetpack Compose for viewing and editing PDF, DOCX, and XLSX documents.

## Features

### 📄 PDF Viewer
- View PDF documents with page navigation
- Smooth scrolling and zooming
- Modern Material Design 3 UI
- Error handling and loading states

### 📝 DOCX Editor
- Edit Word documents paragraph by paragraph
- Add and remove paragraphs
- Save changes back to the original file
- Real-time editing with proper state management

### 📊 XLSX Editor
- Edit Excel spreadsheets
- Add/remove rows and columns
- Cell-by-cell editing
- Maintains data types (numeric, text, boolean)

## Technical Improvements

### 🏗️ Architecture
- **MVVM Pattern**: Proper separation of concerns with ViewModels
- **State Management**: Reactive UI with StateFlow and Compose state
- **Navigation**: Type-safe navigation with Compose Navigation
- **Error Handling**: Comprehensive error handling throughout the app

### 🎨 UI/UX Improvements
- **Material Design 3**: Modern design system implementation
- **Custom Color Scheme**: Branded color palette for UniDocs
- **Typography**: Complete typography system with proper hierarchy
- **Loading States**: User-friendly loading indicators
- **Error States**: Clear error messages and recovery options

### 🔧 Code Quality
- **Dependency Management**: Clean dependency structure with version catalog
- **Error Handling**: Proper exception handling with Result types
- **Resource Management**: Efficient file handling and cleanup
- **Performance**: Optimized PDF rendering and memory management

### 🧪 Testing
- **Unit Tests**: Comprehensive unit test coverage
- **Instrumented Tests**: Android-specific testing
- **Test Structure**: Proper test organization and naming

## Dependencies

### Core Android
- Jetpack Compose BOM
- Activity Compose
- Navigation Compose
- Lifecycle ViewModel Compose

### Document Processing
- Apache POI (DOCX/XLSX)
- PDFBox Android (PDF)

### Utilities
- Coroutines for async operations
- Material Design 3 components

## Project Structure

```
app/
├── src/main/java/com/example/unidocs/
│   ├── MainActivity.kt
│   ├── ui/
│   │   ├── NavGraph.kt
│   │   ├── screens/MainScreen.kt
│   │   ├── pdf/PdfViewer.kt
│   │   ├── docx/DocxEditor.kt
│   │   ├── xlsx/XlsxEditor.kt
│   │   └── theme/
│   ├── viewmodel/DocumentViewModel.kt
│   └── util/FileUtils.kt
└── src/test/ (Unit Tests)
└── src/androidTest/ (Instrumented Tests)
```

## Key Improvements Made

1. **Fixed Build Configuration**
   - Removed duplicate dependencies
   - Updated to latest versions
   - Proper dependency management

2. **Enhanced Error Handling**
   - Result types for file operations
   - User-friendly error messages
   - Graceful degradation

3. **Improved UI/UX**
   - Material Design 3 implementation
   - Consistent navigation patterns
   - Loading and error states

4. **Better Architecture**
   - Proper ViewModel implementation
   - State management with StateFlow
   - Separation of concerns

5. **Performance Optimizations**
   - Efficient PDF rendering
   - Memory management
   - Background processing

## Getting Started

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle files
4. Run on device or emulator

## Requirements

- Android API 26+ (Android 8.0)
- Kotlin 2.0.21+
- Compose BOM 2024.09.00

## License

This project is for educational purposes.
