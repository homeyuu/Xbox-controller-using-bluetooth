package dev.sakayori.xboxcontroller

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.sakayori.xboxcontroller.hid.GamepadState
import dev.sakayori.xboxcontroller.hid.HidConnectionState
import dev.sakayori.xboxcontroller.hid.HidForegroundService
import dev.sakayori.xboxcontroller.hid.HidServiceHolder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@OptIn(ExperimentalCoroutinesApi::class)
class ControllerViewModel(app: Application) : AndroidViewModel(app) {

    private val gamepad = GamepadState()
    private val hidFlow = HidServiceHolder.hid

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    val connectionState: StateFlow<HidConnectionState> = hidFlow
        .flatMapLatest { it?.state ?: flowOf(HidConnectionState.IDLE) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, HidConnectionState.IDLE)

    val statusMessage: StateFlow<String> = hidFlow
        .flatMapLatest { it?.statusMessage ?: flowOf("Sẵn sàng. Nhấn Bắt đầu để mở tay cầm") }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "Sẵn sàng. Nhấn Bắt đầu để mở tay cầm")

    val pairedDevices: StateFlow<List<BluetoothDevice>> = hidFlow
        .flatMapLatest { it?.pairedDevices ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun startHid() {
        val ctx = getApplication<Application>()
        val intent = Intent(ctx, HidForegroundService::class.java)
        ContextCompat.startForegroundService(ctx, intent)
    }

    fun stopHid() {
        val ctx = getApplication<Application>()
        ctx.stopService(Intent(ctx, HidForegroundService::class.java))
    }

    fun connectTo(d: BluetoothDevice) { hidFlow.value?.connectTo(d) }
    fun refreshDevices()              { hidFlow.value?.refreshPairedDevices() }

    fun onButton(bit: Int, pressed: Boolean) {
        gamepad.setButton(bit, pressed)
        send()
        if (pressed) lightHaptic()
    }

    fun onLeftStick(x: Float, y: Float) {
        val cx = applyStickShape(x); val cy = applyStickShape(y)
        gamepad.lx = (cx * Short.MAX_VALUE).toInt()
            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        gamepad.ly = (-cy * Short.MAX_VALUE).toInt()
            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        send()
    }

    fun onRightStick(x: Float, y: Float) {
        val cx = applyStickShape(x); val cy = applyStickShape(y)
        gamepad.rx = (cx * Short.MAX_VALUE).toInt()
            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        gamepad.ry = (-cy * Short.MAX_VALUE).toInt()
            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        send()
    }

    fun onLeftTrigger(v: Float)  {
        gamepad.lt = (v * 255).roundToInt().coerceIn(0, 255).toByte()
        send()
    }
    fun onRightTrigger(v: Float) {
        gamepad.rt = (v * 255).roundToInt().coerceIn(0, 255).toByte()
        send()
    }

    private fun send() = viewModelScope.launch { hidFlow.value?.sendReport(gamepad) }

    /**
     * Apply 8% radial deadzone then a quadratic response curve so small
     * stick movements are gentler than full deflection — matches the feel
     * of a real analog stick instead of the raw 1:1 finger position.
     */
    private fun applyStickShape(v: Float): Float {
        val deadzone = 0.08f
        val mag = v.absoluteValue
        if (mag < deadzone) return 0f
        val scaled = (mag - deadzone) / (1f - deadzone)
        val curved = scaled * scaled
        return if (v < 0f) -curved else curved
    }

    private fun lightHaptic() {
        val v = vibrator ?: return
        runCatching { v.vibrate(VibrationEffect.createOneShot(15L, 60)) }
    }
}
