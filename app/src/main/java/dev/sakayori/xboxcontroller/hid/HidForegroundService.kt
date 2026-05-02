package dev.sakayori.xboxcontroller.hid

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import dev.sakayori.xboxcontroller.MainActivity
import dev.sakayori.xboxcontroller.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground Service that owns the Bluetooth HID device proxy. Keeps the
 * controller alive when the user navigates away from the app, exposes the
 * underlying [XboxHidService] via [HidServiceHolder], and shows a status
 * notification with a "Dừng" action.
 */
class HidForegroundService : Service() {

    companion object {
        const val ACTION_STOP = "dev.sakayori.xboxcontroller.action.STOP"
        private const val CHANNEL_ID = "xbox_controller_status"
        private const val CHANNEL_NAME = "Trạng thái tay cầm"
        private const val NOTIF_ID = 0xC0DE
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val hid by lazy { XboxHidService(this) }
    private var vibrator: Vibrator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        startForeground(NOTIF_ID, buildNotification(getString(R.string.notif_starting)))

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        hid.onRumble = ::playRumble
        HidServiceHolder.attach(hid)

        scope.launch {
            hid.statusMessage.collect { updateNotification(it) }
        }

        // Lightweight haptic cues so the user feels state transitions
        // even with the app in the background.
        scope.launch {
            var prev: HidConnectionState? = null
            hid.state.collect { now ->
                when {
                    prev != HidConnectionState.CONNECTED && now == HidConnectionState.CONNECTED ->
                        connectionHaptic(longPulse = true)
                    prev == HidConnectionState.CONNECTED && now != HidConnectionState.CONNECTED ->
                        connectionHaptic(longPulse = false)
                    else -> {}
                }
                prev = now
            }
        }

        hid.start()
    }

    private fun connectionHaptic(longPulse: Boolean) {
        val v = vibrator ?: return
        val pattern = if (longPulse) {
            longArrayOf(0, 30, 60, 30) // double tap = paired
        } else {
            longArrayOf(0, 80) // single thud = lost connection
        }
        runCatching {
            v.vibrate(VibrationEffect.createWaveform(pattern, -1))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        hid.onRumble = null
        hid.release()
        HidServiceHolder.detach()
        scope.cancel()
        runCatching { vibrator?.cancel() }
        super.onDestroy()
    }

    private fun playRumble(left: Int, right: Int) {
        val v = vibrator ?: return
        val intensity = maxOf(left, right).coerceIn(0, 255)
        if (intensity == 0) {
            runCatching { v.cancel() }
            return
        }
        runCatching { v.vibrate(VibrationEffect.createOneShot(80L, intensity)) }
    }

    private fun ensureChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Hiển thị tay cầm đang chạy nền"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, HidForegroundService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(0, getString(R.string.notif_stop_action), stopIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification(text))
    }
}
