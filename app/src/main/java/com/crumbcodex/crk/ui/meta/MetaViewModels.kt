package com.crumbcodex.crk.ui.meta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crumbcodex.crk.data.seed.CouponNotifier
import com.crumbcodex.crk.domain.model.Coupon
import com.crumbcodex.crk.domain.model.GameMode
import com.crumbcodex.crk.domain.model.TierEntry
import com.crumbcodex.crk.domain.repository.CouponRepository
import com.crumbcodex.crk.domain.repository.GameDataRepository
import com.crumbcodex.crk.ui.util.containerFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

data class TierGroup(val tier: String, val entries: List<TierEntry>)

data class TierListUiState(
    val loading: Boolean = true,
    val mode: GameMode = GameMode.ARENA,
    val groups: List<TierGroup> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class TierListViewModel(gameDataRepository: GameDataRepository) : ViewModel() {
    private val mode = MutableStateFlow(GameMode.ARENA)

    val state: StateFlow<TierListUiState> = mode.flatMapLatest { m ->
        gameDataRepository.observeTierList(m).map { entries ->
            val groups = entries.sortedBy { it.rank }
                .groupBy { it.tier }
                .map { (tier, list) -> TierGroup(tier, list) }
            TierListUiState(loading = false, mode = m, groups = groups)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TierListUiState())

    fun setMode(m: GameMode) { mode.value = m }

    companion object {
        val Factory = containerFactory { TierListViewModel(it.gameDataRepository) }
    }
}

data class CouponsUiState(
    val loading: Boolean = true,
    val coupons: List<Coupon> = emptyList(),
    val showExpired: Boolean = false,
    val hiddenExpired: Int = 0,
    val now: Long = System.currentTimeMillis(),
)

class CouponsViewModel(
    private val repository: CouponRepository,
    private val notifier: CouponNotifier,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    val notificationsEnabled: StateFlow<Boolean> = notifier.enabled
    fun setNotificationsEnabled(on: Boolean) = notifier.setEnabled(on)
    fun hasNotificationPermission(): Boolean = notifier.hasSystemPermission()

    private val showExpired = MutableStateFlow(false)

    val state: StateFlow<CouponsUiState> = combine(repository.observeCoupons(), showExpired) { coupons, show ->
        val now = clock()
        val visible = if (show) coupons else coupons.filterNot { it.isExpired(now) }
        CouponsUiState(false, visible, show, coupons.size - coupons.count { !it.isExpired(now) }, now)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CouponsUiState())

    fun toggleShowExpired() { showExpired.value = !showExpired.value }
    fun setRedeemed(code: String, redeemed: Boolean) = viewModelScope.launch { repository.setRedeemed(code, redeemed) }
    fun delete(code: String) = viewModelScope.launch { repository.delete(code) }

    /** Validates and adds a code. Returns an error message, or null on success. */
    fun add(code: String, rewards: String, expiry: String): String? {
        val clean = code.trim().uppercase()
        if (clean.length < 4) return "Code looks too short"
        if (!clean.all { it.isLetterOrDigit() }) return "Codes contain only letters and digits"
        val expiresAt = if (expiry.isBlank()) null else try {
            LocalDate.parse(expiry.trim()).plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() - 1
        } catch (e: DateTimeParseException) {
            return "Use the date format YYYY-MM-DD"
        }
        viewModelScope.launch { repository.add(clean, rewards, expiresAt) }
        return null
    }

    companion object {
        const val REDEEM_URL = "https://game.devplay.com/coupon/ck/en"
        val Factory = containerFactory { CouponsViewModel(it.couponRepository, it.couponNotifier) }
    }
}
