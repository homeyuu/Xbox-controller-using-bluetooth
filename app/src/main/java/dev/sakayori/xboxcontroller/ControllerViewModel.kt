package dev.sakayori.xboxcontroller

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.sakayori.xboxcontroller.hid.GamepadState
import dev.sakayori.xboxcontroller.hid.HidConnectionState
import dev.sakayori.xboxcontroller.hid.XboxHidService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class ControllerViewModel(app: Application) : AndroidViewModel(app) {

    val hidService = XboxHidService(app)
    private val gamepad = GamepadState()

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    init {
        hidService.onRumble = { left, right -> playRumble(left, right) }
    }

    val connectionState: StateFlow<HidConnectionState> = hidService.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, HidConnectionState.IDLE)
    val statusMessage: StateFlow<String> = hidService.statusMessage
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val pairedDevices: StateFlow<List<BluetoothDevice>> = hidService.pairedDevices
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun startHid()                      = hidService.start()
    fun stopHid()                       = hidService.stop()
    fun connectTo(d: BluetoothDevice)   = hidService.connectTo(d)
    fun refreshDevices()                = hidService.refreshPairedDevices()

    fun onButton(bit: Int, pressed: Boolean) { gamepad.setButton(bit, pressed); send() }

    fun onLeftStick(x: Float, y: Float) {
        gamepad.lx = (x * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        gamepad.ly = (-y * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        send()
    }

    fun onRightStick(x: Float, y: Float) {
        gamepad.rx = (x * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        gamepad.ry = (-y * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        send()
    }

    fun onLeftTrigger(v: Float)  { gamepad.lt = (v * 255).roundToInt().coerceIn(0, 255).toByte(); send() }
    fun onRightTrigger(v: Float) { gamepad.rt = (v * 255).roundToInt().coerceIn(0, 255).toByte(); send() }

    private fun send() = viewModelScope.launch { hidService.sendReport(gamepad) }

    private fun playRumble(left: Int, right: Int) {
        val v = vibrator ?: return
        val intensity = maxOf(left, right).coerceIn(0, 255)
        if (intensity == 0) {
            v.cancel()
            return
        }
        // 80ms pulse keeps the motor responsive while reports stream in.
        val effect = VibrationEffect.createOneShot(80L, intensity)
        v.vibrate(effect)
    }

    override fun onCleared() {
        super.onCleared()
        vibrator?.cancel()
        hidService.stop()
    }
}
