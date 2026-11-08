package com.mipatrimonio.app.feature.documents

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mipatrimonio.app.core.database.dao.DocumentDao
import com.mipatrimonio.app.core.database.entity.DocumentEntity
import com.mipatrimonio.app.core.model.DocumentKind
import com.mipatrimonio.app.core.storage.EncryptedDocumentStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DocumentsUiState(
    val documents: List<DocumentEntity> = emptyList(),
    val query: String = "",
    val saving: Boolean = false,
    val error: Boolean = false,
)

@HiltViewModel
class DocumentsViewModel @Inject constructor(
    private val documentDao: DocumentDao,
    private val store: EncryptedDocumentStore,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val saving = MutableStateFlow(false)
    private val error = MutableStateFlow(false)

    val uiState: StateFlow<DocumentsUiState> = combine(
        documentDao.observeAll(),
        query,
        saving,
        error,
    ) { docs, q, s, e ->
        val filtered = if (q.isBlank()) docs else {
            val needle = q.trim().lowercase()
            docs.filter { it.title.lowercase().contains(needle) }
        }
        DocumentsUiState(documents = filtered, query = q, saving = s, error = e)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DocumentsUiState())

    fun setQuery(value: String) { query.value = value }

    fun clearError() { error.value = false }

    fun addFromUri(
        uri: Uri,
        kind: DocumentKind = DocumentKind.PAYSLIP,
        title: String? = null,
        date: LocalDate = LocalDate.now(),
        onSaved: (Long) -> Unit = {},
    ) {
        if (saving.value) return
        viewModelScope.launch {
            saving.value = true
            error.value = false
            try {
                val id = withContext(Dispatchers.IO) {
                    val displayName = queryDisplayName(uri) ?: "Documento"
                    val resolvedTitle = title?.takeIf { it.isNotBlank() } ?: displayName.removeSuffix(".pdf").ifBlank { "Documento" }
                    val mime = context.contentResolver.getType(uri) ?: "application/pdf"
                    val relativePath = context.contentResolver.openInputStream(uri)?.use { input ->
                        store.save(input)
                    } ?: throw IllegalStateException("No se pudo abrir el archivo")
                    val now = System.currentTimeMillis()
                    documentDao.insert(
                        DocumentEntity(
                            kind = kind,
                            title = resolvedTitle,
                            filePath = relativePath,
                            mimeType = mime,
                            date = date.toEpochDay(),
                            createdAt = now,
                        )
                    )
                }
                onSaved(id)
            } catch (_: Exception) {
                error.value = true
            } finally {
                saving.value = false
            }
        }
    }

    fun delete(document: DocumentEntity) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    documentDao.delete(document)
                    store.delete(document.filePath)
                }
            } catch (_: Exception) {
                error.value = true
            }
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (_: Exception) {
            null
        }
    }
}