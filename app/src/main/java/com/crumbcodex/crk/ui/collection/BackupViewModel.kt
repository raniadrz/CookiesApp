package com.crumbcodex.crk.ui.collection

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crumbcodex.crk.data.backup.BackupFormatException
import com.crumbcodex.crk.domain.repository.BackupRepository
import com.crumbcodex.crk.ui.util.containerFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class BackupViewModel(private val repository: BackupRepository) : ViewModel() {

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun export(resolver: ContentResolver, uri: Uri) = launchTask("Backup saved") {
        val json = repository.exportJson()
        withContext(Dispatchers.IO) {
            resolver.openOutputStream(uri, "wt")?.use { it.write(json.toByteArray()) }
                ?: throw IOException("Couldn't open the file for writing")
        }
    }

    fun import(resolver: ContentResolver, uri: Uri) = launchTask(null) {
        val raw = withContext(Dispatchers.IO) {
            resolver.openInputStream(uri)?.use { stream ->
                val bytes = stream.readBytes()
                if (bytes.size > MAX_BYTES) throw BackupFormatException("File is too large to be a backup")
                bytes.decodeToString()
            } ?: throw IOException("Couldn't open the file")
        }
        val s = repository.importJson(raw)
        _messages.send(
            "Restored ${s.cookies} cookies, ${s.teamsAdded} teams" +
                (if (s.teamsSkipped > 0) " (${s.teamsSkipped} already existed)" else "") +
                ", ${s.coupons} coupons",
        )
    }

    private fun launchTask(success: String?, block: suspend () -> Unit) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                block()
                success?.let { _messages.send(it) }
            } catch (e: BackupFormatException) {
                _messages.send(e.message ?: "Invalid backup file")
            } catch (e: Exception) {
                _messages.send("Backup failed: ${e.message ?: e::class.java.simpleName}")
            } finally {
                _busy.value = false
            }
        }
    }

    companion object {
        private const val MAX_BYTES = 20 * 1024 * 1024
        val Factory = containerFactory { BackupViewModel(it.backupRepository) }
    }
}
