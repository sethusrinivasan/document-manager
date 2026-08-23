package com.app.traveldocs.presentation.documents

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.traveldocs.data.scanner.SmartFileNamer
import com.app.traveldocs.debug.DebugLogger
import com.app.traveldocs.domain.model.DocumentFormat
import com.app.traveldocs.domain.model.ImportedDocument
import com.app.traveldocs.domain.usecase.DocumentImportUseCase
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.resume

data class CameraState(
    val imageBytes: ByteArray? = null,
    val suggestedName: String = "",
    val isNaming: Boolean = false,
    val isImporting: Boolean = false,
    val isDone: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CameraImportViewModel @Inject constructor(
    private val importUseCase: DocumentImportUseCase,
    private val smartNamer: SmartFileNamer,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(CameraState())
    val state: StateFlow<CameraState> = _state.asStateFlow()

    fun onImageCaptured(uri: Uri) {
        viewModelScope.launch {
            val bytes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }
            if (bytes == null || bytes.isEmpty()) {
                _state.value = CameraState(error = "Failed to read captured image")
                return@launch
            }
            _state.value = CameraState(imageBytes = bytes, isNaming = true)
            DebugLogger.i("CameraVM", "Image captured: ${bytes.size} bytes")

            val name = withContext(Dispatchers.IO) { runOcrForNaming(bytes) }
            _state.value = _state.value.copy(isNaming = false, suggestedName = name)
            DebugLogger.i("CameraVM", "Smart name: $name")
        }
    }

    fun acceptAndImport(fileName: String) {
        val bytes = _state.value.imageBytes ?: return
        _state.value = _state.value.copy(isImporting = true)
        viewModelScope.launch {
            DebugLogger.i("CameraVM", "Importing: $fileName")
            val doc = ImportedDocument(rawBytes = bytes, format = DocumentFormat.JPG, originalFileName = fileName)
            val result = importUseCase.importAndProcess(doc, "default-member")
            result.onSuccess { _state.value = CameraState(isDone = true) }
            result.onFailure { e -> _state.value = CameraState(error = e.message ?: "Import failed") }
        }
    }

    fun reset() { _state.value = CameraState() }

    private suspend fun runOcrForNaming(bytes: ByteArray): String {
        return try {
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return smartNamer.generateName("")
            val image = InputImage.fromBitmap(bitmap, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val text = suspendCancellableCoroutine<String> { cont ->
                recognizer.process(image).addOnSuccessListener { cont.resume(it.text) }.addOnFailureListener { cont.resume("") }
            }
            smartNamer.generateName(text)
        } catch (e: Exception) {
            DebugLogger.e("CameraVM", "OCR naming failed", e)
            smartNamer.generateName("")
        }
    }
}
