package com.crumbcodex.crk.ui.meta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crumbcodex.crk.domain.repository.DataStatus
import com.crumbcodex.crk.domain.repository.DataUpdateRepository
import com.crumbcodex.crk.domain.repository.UpdateResult
import com.crumbcodex.crk.ui.util.containerFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class DataUpdateViewModel(private val repository: DataUpdateRepository) : ViewModel() {
    val status: StateFlow<DataStatus> = repository.status

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    init {
        viewModelScope.launch { repository.refreshStatus() }
    }

    fun check() {
        viewModelScope.launch {
            val text = when (val r = repository.checkForUpdates()) {
                is UpdateResult.Updated -> buildString {
                    append("Data updated")
                    val parts = listOfNotNull(
                        r.cookiesVersion?.let { "cookies v$it" },
                        r.gameDataVersion?.let { "game data v$it" },
                    )
                    if (parts.isNotEmpty()) append(": ").append(parts.joinToString(", "))
                    if (r.newCoupons.isNotEmpty()) append(" · ${r.newCoupons.size} new coupon code(s)!")
                }
                UpdateResult.UpToDate -> "Already up to date"
                is UpdateResult.Failed -> r.message
            }
            _messages.send(text)
        }
    }

    companion object {
        val Factory = containerFactory { DataUpdateViewModel(it.dataUpdates) }
    }
}
