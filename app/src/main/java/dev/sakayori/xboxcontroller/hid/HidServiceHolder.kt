package dev.sakayori.xboxcontroller.hid

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Process-wide bridge between the [HidForegroundService] (owns the
 * Bluetooth proxy) and [dev.sakayori.xboxcontroller.ControllerViewModel]
 * (UI consumer). Set when the service starts, cleared on destroy.
 */
object HidServiceHolder {
    private val _hid = MutableStateFlow<XboxHidService?>(null)
    val hid: StateFlow<XboxHidService?> = _hid

    internal fun attach(service: XboxHidService) { _hid.value = service }
    internal fun detach() { _hid.value = null }
}
