package dev.sakayori.xboxcontroller.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sqrt

// ── Thumbstick ────────────────────────────────────────────────────────────────

@Composable
fun Thumbstick(
    label: String,
    size: Dp = 100.dp,
    onMove: (x: Float, y: Float) -> Unit,
) {
    val maxRadius = size.value / 2f * 0.6f
    var knobOffset by remember { mutableStateOf(Offset.Zero) }

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF22223A), XboxColors.Surface)
                    )
                )
                .border(2.dp, XboxColors.Border, CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            val center = Offset(size.toPx() / 2, size.toPx() / 2)
                            val raw = startOffset - center
                            val dist = sqrt(raw.x * raw.x + raw.y * raw.y)
                            val capped = if (dist > maxRadius) maxRadius / dist else 1f
                            knobOffset = raw * capped
                            onMove(knobOffset.x / maxRadius, -knobOffset.y / maxRadius)
                        },
                        onDrag = { change, _ ->
                            val center = Offset(size.toPx() / 2, size.toPx() / 2)
                            val raw = change.position - center
                            val dist = sqrt(raw.x * raw.x + raw.y * raw.y)
                            val capped = if (dist > maxRadius) maxRadius / dist else 1f
                            knobOffset = raw * capped
                            onMove(knobOffset.x / maxRadius, -knobOffset.y / maxRadius)
                        },
                        onDragEnd = {
                            knobOffset = Offset.Zero
                            onMove(0f, 0f)
                        },
                        onDragCancel = {
                            knobOffset = Offset.Zero
                            onMove(0f, 0f)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .offset(
                        x = (knobOffset.x / 3).dp,
                        y = (knobOffset.y / 3).dp
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(XboxColors.AccentBright, XboxColors.AccentGreen)
                        )
                    )
            )
        }
        Text(label, color = XboxColors.TextMuted, fontSize = 9.sp, letterSpacing = 1.sp)
    }
}

// ── Face Buttons ──────────────────────────────────────────────────────────────

@Composable
fun FaceButton(
    label: String,
    color: Color,
    size: Dp = 46.dp,
    onPress: (Boolean) -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (pressed) color else color.copy(alpha = 0.2f))
            .border(2.dp, color, CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        onPress(true)
                        tryAwaitRelease()
                        pressed = false
                        onPress(false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (pressed) Color.White else color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ── D-Pad with multi-touch (allows diagonals like Up+Right) ──────────────────

private val DPadDirections = listOf("up", "down", "left", "right")

@Composable
fun DPad(
    onDirection: (direction: String, pressed: Boolean) -> Unit
) {
    val pressedSet = remember { mutableStateMapOf<String, Boolean>() }

    /** Map a local touch position (px) inside the 3x3 grid to its zone. */
    fun zoneAt(localPx: Offset, totalPx: Float): String? {
        if (localPx.x < 0f || localPx.x >= totalPx || localPx.y < 0f || localPx.y >= totalPx) return null
        val cell = totalPx / 3f
        val col = (localPx.x / cell).toInt()
        val row = (localPx.y / cell).toInt()
        return when {
            row == 0 && col == 1 -> "up"
            row == 2 && col == 1 -> "down"
            row == 1 && col == 0 -> "left"
            row == 1 && col == 2 -> "right"
            else -> null
        }
    }

    val cellDp = 36.dp
    val totalDp = cellDp * 3
    Box(
        modifier = Modifier
            .size(totalDp)
            .pointerInput(Unit) {
                val perPointerZone = mutableMapOf<PointerId, String?>()
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val totalPx = size.width.toFloat()

                        // Refresh per-pointer zone tracking.
                        for (change in event.changes) {
                            if (change.pressed) {
                                perPointerZone[change.id] = zoneAt(change.position, totalPx)
                            } else {
                                perPointerZone.remove(change.id)
                            }
                            change.consume()
                        }

                        // Dirs currently held by ANY pointer.
                        val current = perPointerZone.values.filterNotNull().toHashSet()
                        for (dir in DPadDirections) {
                            val nowPressed = dir in current
                            val wasPressed = pressedSet[dir] == true
                            if (nowPressed != wasPressed) {
                                pressedSet[dir] = nowPressed
                                onDirection(dir, nowPressed)
                            }
                        }
                    }
                }
            }
    ) {
        // Visual 3x3 grid (purely cosmetic — hit testing is done above).
        Column(modifier = Modifier.matchParentSize()) {
            DPadRow(listOf(null, "up", null), pressedSet)
            DPadRow(listOf("left", "center", "right"), pressedSet)
            DPadRow(listOf(null, "down", null), pressedSet)
        }
    }
}

@Composable
private fun ColumnScope.DPadRow(
    cells: List<String?>,
    pressedSet: Map<String, Boolean>
) {
    Row(
        modifier = Modifier.weight(1f).fillMaxWidth()
    ) {
        for (cell in cells) {
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                if (cell != null && cell != "center") {
                    val isPressed = pressedSet[cell] == true
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isPressed) XboxColors.AccentGreen else Color(0xFF1E1E2E))
                            .border(1.dp, XboxColors.Border, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            when (cell) { "up" -> "▲"; "down" -> "▼"; "left" -> "◀"; else -> "▶" },
                            color = XboxColors.TextPrimary,
                            fontSize = 10.sp
                        )
                    }
                } else if (cell == "center") {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E1E2E))
                            .border(1.dp, XboxColors.Border, CircleShape)
                    )
                }
            }
        }
    }
}

// ── Analog Trigger ────────────────────────────────────────────────────────────
//
// Tap = full press (1.0). While holding, drag the finger UP to gradually
// release pressure (1 → 0 over ~80px). Drag back DOWN to re-engage. Lifting
// the finger always returns to 0. This matches the feel of a real LT/RT
// without forcing the user to fiddle for binary cases.

@Composable
fun TriggerButton(
    label: String,
    onValue: (Float) -> Unit,
) {
    var fill by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .width(60.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(XboxColors.Card)
            .border(1.5.dp, XboxColors.Border, RoundedCornerShape(6.dp))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitPointerEvent()
                        val pressed = down.changes.firstOrNull { it.pressed } ?: continue
                        val startId = pressed.id
                        val startY = pressed.position.y
                        var v = 1f
                        fill = v; onValue(v)
                        pressed.consume()

                        var releasedNormally = false
                        while (true) {
                            val ev = awaitPointerEvent()
                            val change = ev.changes.firstOrNull { it.id == startId }
                            if (change == null) {
                                releasedNormally = true
                                break
                            }
                            if (!change.pressed) {
                                releasedNormally = true
                                change.consume()
                                break
                            }
                            val dy = change.position.y - startY
                            v = if (dy < 0f) (1f + dy / 80f).coerceIn(0f, 1f) else 1f
                            fill = v; onValue(v)
                            change.consume()
                        }
                        if (releasedNormally) {
                            fill = 0f; onValue(0f)
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (fill > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fill)
                    .clip(RoundedCornerShape(6.dp))
                    .background(XboxColors.AccentGreen.copy(alpha = 0.6f))
                    .align(Alignment.CenterStart)
            )
        }
        Text(label, color = XboxColors.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

// ── Small button (Back/Start/Guide) ──────────────────────────────────────────

@Composable
fun SmallButton(
    label: String,
    onPress: (Boolean) -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (pressed) XboxColors.AccentGreen else XboxColors.Card)
            .border(1.dp, XboxColors.Border, RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    pressed = true; onPress(true)
                    tryAwaitRelease()
                    pressed = false; onPress(false)
                })
            }
    ) {
        Text(label, color = XboxColors.TextMuted, fontSize = 10.sp)
    }
}
