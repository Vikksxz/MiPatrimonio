package com.mipatrimonio.app.feature.documents

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mipatrimonio.app.core.database.dao.DocumentDao
import com.mipatrimonio.app.core.database.entity.DocumentEntity
import com.mipatrimonio.app.core.storage.EncryptedDocumentStore
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DocumentViewerUiState(
    val document: DocumentEntity? = null,
    val pageCount: Int = 0,
    val loading: Boolean = true,
    val error: Boolean = false,
)

@HiltViewModel
class DocumentViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val documentDao: DocumentDao,
    private val store: EncryptedDocumentStore,
) : ViewModel() {

    private val documentId: Long = savedStateHandle.get<Long>("documentId") ?: -1L

    private val _uiState = MutableStateFlow(DocumentViewerUiState())
    val uiState: StateFlow<DocumentViewerUiState> = _uiState.asStateFlow()

    private var tempFile: File? = null
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = DocumentViewerUiState(loading = true)
            try {
                val (doc, temp, pfd, rend) = withContext(Dispatchers.IO) {
                    val entity = documentDao.getById(documentId) ?: throw IllegalStateException("No existe")
                    val tmp = store.decryptToTemp(entity.filePath)
                    val fd = ParcelFileDescriptor.open(tmp, ParcelFileDescriptor.MODE_READ_ONLY)
                    val r = PdfRenderer(fd)
                    Quad(entity, tmp, fd, r)
                }
                tempFile = temp
                fileDescriptor = pfd
                renderer = rend
                _uiState.value = DocumentViewerUiState(document = doc, pageCount = rend.pageCount, loading = false)
            } catch (_: Exception) {
                cleanup()
                _uiState.value = DocumentViewerUiState(loading = false, error = true)
            }
        }
    }

    /** Renderiza una página a bitmap. Llamar desde IO/Default. */
    fun renderPage(index: Int, widthPx: Int): Bitmap? {
        val r = renderer ?: return null
        if (index < 0 || index >= r.pageCount) return null
        return synchronized(r) {
            val page = r.openPage(index)
            try {
                val scale = if (page.width > 0) widthPx.toFloat() / page.width.toFloat() else 1f
                val h = (page.height * scale).toInt().coerceAtLeast(1)
                val w = widthPx.coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            } finally {
                page.close()
            }
        }
    }

    private fun cleanup() {
        try { renderer?.close() } catch (_: Exception) {}
        try { fileDescriptor?.close() } catch (_: Exception) {}
        tempFile?.delete()
        renderer = null
        fileDescriptor = null
        tempFile = null
    }

    override fun onCleared() {
        cleanup()
        super.onCleared()
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}