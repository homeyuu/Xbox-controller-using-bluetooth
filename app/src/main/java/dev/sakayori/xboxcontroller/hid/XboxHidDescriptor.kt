package dev.sakayori.xboxcontroller.hid

/**
 * HID report descriptor matching the standard "Xbox 360 / Xbox One in
 * DirectInput mode" layout that Windows / SDL2 / most games (Forza,
 * Steam Input, etc.) recognize without per-app remapping:
 *
 *   - 11 buttons (1..11 = A, B, X, Y, LB, RB, Back, Start, LS, RS, Guide)
 *   - 1 HAT switch (4-bit, 8 = neutral) for the D-Pad
 *   - 4 signed 16-bit axes: X (LX), Y (LY), Rx (RX), Ry (RY)
 *   - 2 unsigned 8-bit triggers: Z (LT), Rz (RT)
 *   - 2 unsigned 8-bit OUTPUT bytes for rumble (vendor page)
 *
 * Total INPUT report = 13 bytes, OUTPUT = 2 bytes.
 *
 * Byte layout of [GamepadState.toReport]:
 *     [0..1]  buttons  (11 bits + 5 padding)
 *     [2]     hat      (4 bits + 4 padding)
 *     [3..4]  LX (s16 LE)
 *     [5..6]  LY (s16 LE)
 *     [7..8]  RX (s16 LE)
 *     [9..10] RY (s16 LE)
 *     [11]    LT (u8)
 *     [12]    RT (u8)
 */
object XboxHidDescriptor {
    val DESCRIPTOR: ByteArray = byteArrayOf(
        // ── Application gamepad collection ───────────────────────────────
        0x05.toByte(), 0x01.toByte(),                    // Usage Page (Generic Desktop)
        0x09.toByte(), 0x05.toByte(),                    // Usage (Game Pad)
        0xA1.toByte(), 0x01.toByte(),                    // Collection (Application)

        // ── 11 buttons + 5 bits padding ──────────────────────────────────
        0x05.toByte(), 0x09.toByte(),                    // Usage Page (Button)
        0x19.toByte(), 0x01.toByte(),                    // Usage Min  (1)
        0x29.toByte(), 0x0B.toByte(),                    // Usage Max  (11)
        0x15.toByte(), 0x00.toByte(),                    // Logical Min 0
        0x25.toByte(), 0x01.toByte(),                    // Logical Max 1
        0x75.toByte(), 0x01.toByte(),                    // Report Size 1
        0x95.toByte(), 0x0B.toByte(),                    // Report Count 11
        0x81.toByte(), 0x02.toByte(),                    // Input (Data, Var, Abs)
        0x75.toByte(), 0x01.toByte(),                    // Report Size 1
        0x95.toByte(), 0x05.toByte(),                    // Report Count 5
        0x81.toByte(), 0x03.toByte(),                    // Input (Const)  -- pad

        // ── HAT switch (D-Pad) ───────────────────────────────────────────
        0x05.toByte(), 0x01.toByte(),                    // Usage Page (Generic Desktop)
        0x09.toByte(), 0x39.toByte(),                    // Usage (HAT switch)
        0x15.toByte(), 0x00.toByte(),                    // Logical Min 0
        0x25.toByte(), 0x07.toByte(),                    // Logical Max 7
        0x35.toByte(), 0x00.toByte(),                    // Physical Min 0
        0x46.toByte(), 0x3B.toByte(), 0x01.toByte(),     // Physical Max 315 (degrees)
        0x65.toByte(), 0x14.toByte(),                    // Unit (English Rotation: degrees)
        0x75.toByte(), 0x04.toByte(),                    // Report Size 4
        0x95.toByte(), 0x01.toByte(),                    // Report Count 1
        0x81.toByte(), 0x42.toByte(),                    // Input (Data, Var, Abs, Null state)
        0x75.toByte(), 0x01.toByte(),                    // Report Size 1
        0x95.toByte(), 0x04.toByte(),                    // Report Count 4
        0x81.toByte(), 0x03.toByte(),                    // Input (Const)  -- pad
        0x65.toByte(), 0x00.toByte(),                    // Unit (None)

        // ── 4 signed-16 axes for the two thumbsticks ─────────────────────
        0x09.toByte(), 0x30.toByte(),                    // Usage (X)  -- LX
        0x09.toByte(), 0x31.toByte(),                    // Usage (Y)  -- LY
        0x09.toByte(), 0x33.toByte(),                    // Usage (Rx) -- RX
        0x09.toByte(), 0x34.toByte(),                    // Usage (Ry) -- RY
        0x16.toByte(), 0x00.toByte(), 0x80.toByte(),     // Logical Min -32768
        0x26.toByte(), 0xFF.toByte(), 0x7F.toByte(),     // Logical Max  32767
        0x75.toByte(), 0x10.toByte(),                    // Report Size 16
        0x95.toByte(), 0x04.toByte(),                    // Report Count 4
        0x81.toByte(), 0x02.toByte(),                    // Input

        // ── 2 unsigned-8 triggers ────────────────────────────────────────
        0x09.toByte(), 0x32.toByte(),                    // Usage (Z)  -- LT
        0x09.toByte(), 0x35.toByte(),                    // Usage (Rz) -- RT
        0x15.toByte(), 0x00.toByte(),                    // Logical Min 0
        0x26.toByte(), 0xFF.toByte(), 0x00.toByte(),     // Logical Max 255
        0x75.toByte(), 0x08.toByte(),                    // Report Size 8
        0x95.toByte(), 0x02.toByte(),                    // Report Count 2
        0x81.toByte(), 0x02.toByte(),                    // Input

        // ── OUTPUT: 2 vendor bytes for rumble (left motor, right motor) ─
        0x06.toByte(), 0x00.toByte(), 0xFF.toByte(),     // Usage Page (Vendor)
        0x09.toByte(), 0x01.toByte(),                    // Usage 1
        0x15.toByte(), 0x00.toByte(),                    // Logical Min 0
        0x26.toByte(), 0xFF.toByte(), 0x00.toByte(),     // Logical Max 255
        0x75.toByte(), 0x08.toByte(),                    // Report Size 8
        0x95.toByte(), 0x02.toByte(),                    // Report Count 2
        0x91.toByte(), 0x02.toByte(),                    // Output (Data, Var, Abs)

        0xC0.toByte()                                    // End Collection
    )

    /** Bit positions inside [GamepadState.buttons]. Match HID button index - 1. */
    object Button {
        const val A     = 0   // HID button 1
        const val B     = 1   // HID button 2
        const val X     = 2   // HID button 3
        const val Y     = 3   // HID button 4
        const val LB    = 4   // HID button 5
        const val RB    = 5   // HID button 6
        const val BACK  = 6   // HID button 7
        const val START = 7   // HID button 8
        const val LS    = 8   // HID button 9
        const val RS    = 9   // HID button 10
        const val GUIDE = 10  // HID button 11
    }
}

data class GamepadState(
    var buttons: Int = 0,
    var lx: Short = 0, var ly: Short = 0,
    var rx: Short = 0, var ry: Short = 0,
    var lt: Byte = 0,  var rt: Byte = 0,
    var dpadUp: Boolean = false,
    var dpadDown: Boolean = false,
    var dpadLeft: Boolean = false,
    var dpadRight: Boolean = false,
) {
    fun setButton(bit: Int, pressed: Boolean) {
        buttons = if (pressed) buttons or (1 shl bit) else buttons and (1 shl bit).inv()
    }

    fun setDpad(direction: String, pressed: Boolean) {
        when (direction) {
            "up"    -> dpadUp = pressed
            "down"  -> dpadDown = pressed
            "left"  -> dpadLeft = pressed
            "right" -> dpadRight = pressed
        }
    }

    /** Encode the four D-Pad booleans as a 4-bit HAT value (8 = neutral). */
    private fun computeHat(): Int = when {
        dpadUp && dpadRight   -> 1   // NE
        dpadRight && dpadDown -> 3   // SE
        dpadDown && dpadLeft  -> 5   // SW
        dpadLeft && dpadUp    -> 7   // NW
        dpadUp    -> 0               // N
        dpadRight -> 2               // E
        dpadDown  -> 4               // S
        dpadLeft  -> 6               // W
        else      -> 8               // neutral / null
    }

    fun toReport(): ByteArray {
        val buf = ByteArray(13)
        buf[0] = (buttons and 0xFF).toByte()
        buf[1] = ((buttons shr 8) and 0xFF).toByte()
        buf[2] = (computeHat() and 0x0F).toByte()
        fun putS16(off: Int, v: Short) {
            buf[off]     = (v.toInt() and 0xFF).toByte()
            buf[off + 1] = ((v.toInt() shr 8) and 0xFF).toByte()
        }
        putS16(3, lx); putS16(5, ly)
        putS16(7, rx); putS16(9, ry)
        buf[11] = lt; buf[12] = rt
        return buf
    }
}
