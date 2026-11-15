package com.crumbcodex.crk.data.seed

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.crumbcodex.crk.MainActivity
import com.crumbcodex.crk.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Posts a "new coupon codes" notification, if the user allows it. */
class CouponNotifier(private val context: Context) {

    private val prefs = context.getSharedPreferences("crumb_codex_prefs", Context.MODE_PRIVATE)
    private val _enabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, true))
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    fun setEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, value).apply()
        _enabled.value = value
    }

    /** True when Android lets this app post notifications (always true below Android 13). */
    fun hasSystemPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun notifyNewCoupons(codes: List<String>) {
        if (codes.isEmpty() || !_enabled.value || !hasSystemPermission()) return
        ensureChannel()
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(MainActivity.EXTRA_OPEN, MainActivity.OPEN_COUPONS)
        val pending = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = if (codes.size == 1) "New coupon code!" else "${codes.size} new coupon codes!"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_coupon)
            .setContentTitle(title)
            .setContentText(codes.joinToString(", "))
            .setStyle(NotificationCompat.BigTextStyle().bigText(codes.joinToString("\n") + "\n\nTap to copy & redeem."))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the post; nothing to do.
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "New coupon codes", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Alerts when new Cookie Run: Kingdom coupon codes are added"
                },
            )
        }
    }

    private companion object {
        const val CHANNEL_ID = "coupons"
        const val NOTIFICATION_ID = 1001
        const val KEY_ENABLED = "coupon_notifications"
    }
}
