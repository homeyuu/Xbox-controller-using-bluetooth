package dev.sakayori.xboxcontroller.hid

object XboxHidDescriptor {
    val DESCRIPTOR: ByteArray = byteArrayOf(
        // Usage Page (Generic Desktop), Usage (Game Pad), Collection (Application)
        0x05.toByte(), 0x01.toByte(), 0x09.toByte(), 0x05.toByte(),
        0xA1.toByte(), 0x01.toByte(),
        // 15 buttons + 1 bit padding
        0x05.toByte(), 0x09.toByte(),
        0x19.toByte(), 0x01.toByte(), 0x29.toByte(), 0x0F.toByte(),
        0x15.toByte(), 0x00.toByte(), 0x25.toByte(), 0x01.toByte(),
        0x75.toByte(), 0x01.toByte(), 0x95.toByte(), 0x0F.toByte(),
        0x81.toByte(), 0x02.toByte(),
        0x75.toByte(), 0x01.toByte(), 0x95.toByte(), 0x01.toByte(),
        0x81.toByte(), 0x03.toByte(),
        // 4 signed 16-bit axes (LX, LY, RX, RY)
        0x05.toByte(), 0x01.toByte(),
        0x09.toByte(), 0x30.toByte(), 0x09.toByte(), 0x31.toByte(),
        0x09.toByte(), 0x33.toByte(), 0x09.toByte(), 0x34.toByte(),
        0x16.toByte(), 0x00.toByte(), 0x80.toByte(),
        0x26.toByte(), 0xFF.toByte(), 0x7F.toByte(),
        0x75.toByte(), 0x10.toByte(), 0x95.toByte(), 0x04.toByte(),
        0x81.toByte(), 0x02.toByte(),
        // 2 unsigned 8-bit triggers (Z=LT, Rz=RT)
        0x09.toByte(), 0x32.toByte(), 0x09.toByte(), 0x35.toByte(),
        0x15.toByte(), 0x00.toByte(),
        0x26.toByte(), 0xFF.toByte(), 0x00.toByte(),
        0x75.toByte(), 0x08.toByte(), 0x95.toByte(), 0x02.toByte(),
        0x81.toByte(), 0x02.toByte(),
        // OUTPUT: 2 vendor bytes for rumble (left motor, right motor) 0..255
        0x06.toByte(), 0x00.toByte(), 0xFF.toByte(),
        0x09.toByte(), 0x01.toByte(),
        0x15.toByte(), 0x00.toByte(),
        0x26.toByte(), 0xFF.toByte(), 0x00.toByte(),
        0x75.toByte(), 0x08.toByte(), 0x95.toByte(), 0x02.toByte(),
        0x91.toByte(), 0x02.toByte(),
        // End Collection
        0xC0.toByte()
    )

    object Button {
        const val A          = 0
        const val B          = 1
        const val X          = 2
        const val Y          = 3
        const val LB         = 4
        const val RB         = 5
        const val BACK       = 6
        const val START      = 7
        const val GUIDE      = 8
        const val LS         = 9
        const val RS         = 10
        const val DPAD_UP    = 11
        const val DPAD_DOWN  = 12
        const val DPAD_LEFT  = 13
        const val DPAD_RIGHT = 14
    }
}

data class GamepadState(
    var buttons: Int = 0,
    var lx: Short = 0, var ly: Short = 0,
    var rx: Short = 0, var ry: Short = 0,
    var lt: Byte = 0,  var rt: Byte = 0,
) {
    fun setButton(bit: Int, pressed: Boolean) {
        buttons = if (pressed) buttons or (1 shl bit) else buttons and (1 shl bit).inv()
    }

    fun toReport(): ByteArray {
        val buf = ByteArray(12)
        buf[0] = (buttons and 0xFF).toByte()
        buf[1] = ((buttons shr 8) and 0xFF).toByte()
        fun putS16(off: Int, v: Short) {
            buf[off]     = (v.toInt() and 0xFF).toByte()
            buf[off + 1] = ((v.toInt() shr 8) and 0xFF).toByte()
        }
        putS16(2, lx); putS16(4, ly)
        putS16(6, rx); putS16(8, ry)
        buf[10] = lt; buf[11] = rt
        return buf
    }
}
