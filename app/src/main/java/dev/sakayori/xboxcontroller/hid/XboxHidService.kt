package dev.sakayori.xboxcontroller.hid

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class HidConnectionState { IDLE, REGISTERING, WAITING_PAIR, CONNECTED, ERROR }

@SuppressLint("MissingPermission")
class XboxHidService(private val context: Context) {

    companion object {
        const val BT_DEVICE_NAME = "Xbox Wireless Controller"
        private const val SDP_DESCRIPTION = "Xbox Wireless Controller"
        private const val SDP_PROVIDER = "Microsoft"

        // ── Friendly Vietnamese fallback messages ─────────────────────────
        private const val MSG_NO_BT          = "Thiết bị này không có Bluetooth"
        private const val MSG_BT_OFF         = "Bluetooth chưa bật. Mở Bluetooth ở Cài đặt rồi thử lại"
        private const val MSG_REG_FAILED     = "Không đăng ký được tay cầm. Thử tắt rồi bật lại Bluetooth"
        private const val MSG_DISCONNECTED   = "TV ngắt kết nối — đang chờ kết nối lại…"
        private const val MSG_CONNECTING     = "Đang đăng ký tay cầm…"
        private const val MSG_UNKNOWN_ERROR  = "Lỗi 404 — không rõ. Thử Dừng rồi Bắt đầu lại"
        private const val MSG_IDLE           = "Sẵn sàng. Nhấn Bắt đầu để mở tay cầm"
    }

    private val btAdapter: BluetoothAdapter? get() = runCatching {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    }.getOrNull()

    private var hidDevice: BluetoothHidDevice? = null
    private var connectedHost: BluetoothDevice? = null
    private var originalAdapterName: String? = null

    private val _state = MutableStateFlow(HidConnectionState.IDLE)
    val state: StateFlow<HidConnectionState> = _state

    private val _pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDevice>> = _pairedDevices

    private val _statusMessage = MutableStateFlow(MSG_IDLE)
    val statusMessage: StateFlow<String> = _statusMessage

    /** Callback invoked when the host sends a rumble OUTPUT report. */
    var onRumble: ((leftMotor: Int, rightMotor: Int) -> Unit)? = null

    private val sdpRecord = BluetoothHidDeviceAppSdpSettings(
        BT_DEVICE_NAME,
        SDP_DESCRIPTION,
        SDP_PROVIDER,
        BluetoothHidDevice.SUBCLASS2_GAMEPAD,
        XboxHidDescriptor.DESCRIPTOR
    )

    private val qosOut = BluetoothHidDeviceAppQosSettings(
        BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
        800, 9, 0, 11250, BluetoothHidDeviceAppQosSettings.MAX
    )

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            if (registered) {
                _state.value = HidConnectionState.WAITING_PAIR
                _statusMessage.value = "Mở Bluetooth trên TV → tìm \"$BT_DEVICE_NAME\" → Pair"
                refreshPairedDevices()
            } else {
                if (_state.value == HidConnectionState.REGISTERING) {
                    _state.value = HidConnectionState.ERROR
                    _statusMessage.value = MSG_REG_FAILED
                } else {
                    _state.value = HidConnectionState.IDLE
                    _statusMessage.value = MSG_IDLE
                }
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedHost = device
                    _state.value = HidConnectionState.CONNECTED
                    val deviceLabel = device.name?.takeIf { it.isNotBlank() } ?: "TV"
                    _statusMessage.value = "Đã kết nối với $deviceLabel"
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (connectedHost?.address == device.address) {
                        connectedHost = null
                        _state.value = HidConnectionState.WAITING_PAIR
                        _statusMessage.value = MSG_DISCONNECTED
                    }
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    _statusMessage.value = "Đang kết nối với ${device.name ?: "TV"}…"
                }
                BluetoothProfile.STATE_DISCONNECTING -> {
                    _statusMessage.value = "Đang ngắt kết nối…"
                }
            }
        }

        override fun onGetReport(device: BluetoothDevice, type: Byte, id: Byte, bufferSize: Int) {
            hidDevice?.replyReport(device, type, id, GamepadState().toReport())
        }

        override fun onSetReport(device: BluetoothDevice, type: Byte, id: Byte, data: ByteArray) {
            if (data.size >= 2) {
                val left = data[0].toInt() and 0xFF
                val right = data[1].toInt() and 0xFF
                onRumble?.invoke(left, right)
            }
        }
    }

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            try {
                hidDevice = proxy as BluetoothHidDevice
                applySpoofedAdapterName()
                val ok = hidDevice?.registerApp(sdpRecord, null, qosOut, { it.run() }, hidCallback) == true
                if (!ok) {
                    _state.value = HidConnectionState.ERROR
                    _statusMessage.value = MSG_REG_FAILED
                } else {
                    _state.value = HidConnectionState.REGISTERING
                    _statusMessage.value = MSG_CONNECTING
                }
            } catch (_: Throwable) {
                _state.value = HidConnectionState.ERROR
                _statusMessage.value = MSG_UNKNOWN_ERROR
            }
        }
        override fun onServiceDisconnected(profile: Int) {
            hidDevice = null
            _state.value = HidConnectionState.IDLE
            _statusMessage.value = MSG_IDLE
        }
    }

    private fun applySpoofedAdapterName() {
        val adapter = btAdapter ?: return
        runCatching {
            if (originalAdapterName == null) originalAdapterName = adapter.name
            if (adapter.name != BT_DEVICE_NAME) adapter.name = BT_DEVICE_NAME
        }
    }

    private fun restoreAdapterName() {
        val adapter = btAdapter ?: return
        val original = originalAdapterName ?: return
        runCatching { adapter.name = original }
        originalAdapterName = null
    }

    fun start() {
        if (_state.value != HidConnectionState.IDLE && _state.value != HidConnectionState.ERROR) return
        val adapter = btAdapter
        if (adapter == null) {
            _state.value = HidConnectionState.ERROR
            _statusMessage.value = MSG_NO_BT
            return
        }
        if (!adapter.isEnabled) {
            _state.value = HidConnectionState.ERROR
            _statusMessage.value = MSG_BT_OFF
            return
        }
        try {
            val ok = adapter.getProfileProxy(context, profileListener, BluetoothProfile.HID_DEVICE)
            if (!ok) {
                _state.value = HidConnectionState.ERROR
                _statusMessage.value = MSG_REG_FAILED
                return
            }
            _state.value = HidConnectionState.REGISTERING
            _statusMessage.value = MSG_CONNECTING
        } catch (_: Throwable) {
            _state.value = HidConnectionState.ERROR
            _statusMessage.value = MSG_UNKNOWN_ERROR
        }
    }

    fun stop() {
        runCatching { hidDevice?.unregisterApp() }
        runCatching { btAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice) }
        hidDevice = null
        connectedHost = null
        restoreAdapterName()
        _state.value = HidConnectionState.IDLE
        _statusMessage.value = MSG_IDLE
    }

    fun connectTo(device: BluetoothDevice) {
        runCatching { hidDevice?.connect(device) }
    }

    fun disconnect() {
        connectedHost?.let { runCatching { hidDevice?.disconnect(it) } }
    }

    fun sendReport(state: GamepadState) {
        val host = connectedHost ?: return
        val dev = hidDevice ?: return
        runCatching { dev.sendReport(host, 0, state.toReport()) }
    }

    fun refreshPairedDevices() {
        _pairedDevices.value = runCatching { btAdapter?.bondedDevices?.toList() }.getOrNull() ?: emptyList()
    }

    fun isBluetoothEnabled(): Boolean = btAdapter?.isEnabled == true
}
