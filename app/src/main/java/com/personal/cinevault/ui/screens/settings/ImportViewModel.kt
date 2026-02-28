package com.personal.cinevault.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.cinevault.data.import.LetterboxdImporter
import com.personal.cinevault.data.import.models.ImportResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ImportViewModel(
    private val importer: LetterboxdImporter
) : ViewModel() {

    private val _importProgress = MutableStateFlow<Pair<Int, Int>?>(null)
    val importProgress: StateFlow<Pair<Int, Int>?> = _importProgress.asStateFlow()

    private val _importResult = MutableStateFlow<ImportResult?>(null)
    val importResult: StateFlow<ImportResult?> = _importResult.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _importError = MutableStateFlow<String?>(null)
    val importError: StateFlow<String?> = _importError.asStateFlow()

    fun getImportIntent(): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            val mimeTypes = arrayOf("application/zip", "application/octet-stream", "application/x-zip-compressed")
            putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        }
    }

    fun startImport(context: Context, uri: Uri) {
        if (_isImporting.value) return

        viewModelScope.launch {
            _isImporting.value = true
            _importError.value = null
            _importResult.value = null
            _importProgress.value = Pair(0, 100) // Initial placeholder

            try {
                val result = importer.importFromZip(context, uri) { current, total ->
                    _importProgress.value = Pair(current, total)
                }
                _importResult.value = result
            } catch (e: Exception) {
                _importError.value = e.message ?: "An unknown error occurred during import."
            } finally {
                _isImporting.value = false
                _importProgress.value = null
            }
        }
    }

    fun dismissResult() {
        _importResult.value = null
    }

    fun dismissError() {
        _importError.value = null
    }
}
