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
        // Name shown to TV / PC during Bluetooth discovery — must match what
        // hosts expect for an Xbox Wireless Controller.
        const val BT_DEVICE_NAME = "Xbox Wireless Controller"
        private const val SDP_DESCRIPTION = "Xbox Wireless Controller"
        private const val SDP_PROVIDER = "Microsoft"
    }

    private val btAdapter: BluetoothAdapter? get() =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

    private var hidDevice: BluetoothHidDevice? = null
    private var connectedHost: BluetoothDevice? = null
    private var originalAdapterName: String? = null

    private val _state = MutableStateFlow(HidConnectionState.IDLE)
    val state: StateFlow<HidConnectionState> = _state

    private val _pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDevice>> = _pairedDevices

    private val _statusMessage = MutableStateFlow("Chưa kết nối")
    val statusMessage: StateFlow<String> = _statusMessage

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
                _state.value = HidConnectionState.IDLE
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedHost = device
                    _state.value = HidConnectionState.CONNECTED
                    _statusMessage.value = "Đã kết nối: ${device.name ?: device.address}"
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (connectedHost?.address == device.address) {
                        connectedHost = null
                        _state.value = HidConnectionState.WAITING_PAIR
                        _statusMessage.value = "TV ngắt kết nối — đang chờ…"
                    }
                }
            }
        }

        override fun onGetReport(device: BluetoothDevice, type: Byte, id: Byte, bufferSize: Int) {
            hidDevice?.replyReport(device, type, id, GamepadState().toReport())
        }
    }

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            hidDevice = proxy as BluetoothHidDevice
            applySpoofedAdapterName()
            hidDevice?.registerApp(sdpRecord, null, qosOut, { it.run() }, hidCallback)
            _state.value = HidConnectionState.REGISTERING
            _statusMessage.value = "Đang đăng ký HID profile…"
        }
        override fun onServiceDisconnected(profile: Int) {
            hidDevice = null
            _state.value = HidConnectionState.IDLE
        }
    }

    private fun applySpoofedAdapterName() {
        val adapter = btAdapter ?: return
        if (originalAdapterName == null) originalAdapterName = adapter.name
        if (adapter.name != BT_DEVICE_NAME) {
            runCatching { adapter.name = BT_DEVICE_NAME }
        }
    }

    private fun restoreAdapterName() {
        val adapter = btAdapter ?: return
        val original = originalAdapterName ?: return
        runCatching { adapter.name = original }
        originalAdapterName = null
    }

    fun start() {
        if (_state.value != HidConnectionState.IDLE) return
        if (btAdapter == null) {
            _state.value = HidConnectionState.ERROR
            _statusMessage.value = "Thiết bị không hỗ trợ Bluetooth"
            return
        }
        btAdapter?.getProfileProxy(context, profileListener, BluetoothProfile.HID_DEVICE)
        _state.value = HidConnectionState.REGISTERING
    }

    fun stop() {
        hidDevice?.unregisterApp()
        btAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice)
        hidDevice = null; connectedHost = null
        restoreAdapterName()
        _state.value = HidConnectionState.IDLE
    }

    fun connectTo(device: BluetoothDevice) { hidDevice?.connect(device) }
    fun disconnect() { connectedHost?.let { hidDevice?.disconnect(it) } }
    fun sendReport(state: GamepadState) { connectedHost?.let { hidDevice?.sendReport(it, 0, state.toReport()) } }
    fun refreshPairedDevices() { _pairedDevices.value = btAdapter?.bondedDevices?.toList() ?: emptyList() }
    fun isBluetoothEnabled(): Boolean = btAdapter?.isEnabled == true
}
