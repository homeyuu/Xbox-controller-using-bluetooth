package dev.sakayori.xboxcontroller.ui

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sqrt

// ── Thumbstick ────────────────────────────────────────────────────────────────

@Composable
fun Thumbstick(
    label: String,
    size: Dp = 120.dp,
    onMove: (x: Float, y: Float) -> Unit,
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
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
                    val maxRadiusPx = (this.size.width / 2f) * 0.6f
                    val centerPx = Offset(this.size.width / 2f, this.size.height / 2f)
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            val raw = startOffset - centerPx
                            knobOffset = clampToRadius(raw, maxRadiusPx)
                            onMove(knobOffset.x / maxRadiusPx, -knobOffset.y / maxRadiusPx)
                        },
                        onDrag = { change, _ ->
                            val raw = change.position - centerPx
                            knobOffset = clampToRadius(raw, maxRadiusPx)
                            onMove(knobOffset.x / maxRadiusPx, -knobOffset.y / maxRadiusPx)
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
            // Knob — offset measured in raw pixels via the offset { } lambda
            // so it tracks the finger 1:1 regardless of screen density.
            Box(
                modifier = Modifier
                    .offset { IntOffset(knobOffset.x.toInt(), knobOffset.y.toInt()) }
                    .size(size * 0.36f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(XboxColors.AccentBright, XboxColors.AccentGreen)
                        )
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.15f), CircleShape)
            )
        }
        Text(label, color = XboxColors.TextMuted, fontSize = 9.sp, letterSpacing = 1.sp)
    }
}

private fun clampToRadius(raw: Offset, maxRadius: Float): Offset {
    val dist = sqrt(raw.x * raw.x + raw.y * raw.y)
    if (dist <= maxRadius || dist == 0f) return raw
    return raw * (maxRadius / dist)
}

// ── Face Buttons ──────────────────────────────────────────────────────────────

@Composable
fun FaceButton(
    label: String,
    color: Color,
    size: Dp = 56.dp,
    onPress: (Boolean) -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        label = "face_press_scale"
    )

    Box(
        modifier = Modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(if (pressed) color else color.copy(alpha = 0.22f))
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
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ── Xbox Guide button (the round 𝕏 in the middle) ────────────────────────────

@Composable
fun XboxGuideButton(
    size: Dp = 48.dp,
    onPress: (Boolean) -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, label = "guide_scale")
    Box(
        modifier = Modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(XboxColors.AccentBright, Color(0xFF063A06))
                )
            )
            .border(2.dp, XboxColors.AccentGreen, CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    pressed = true; onPress(true)
                    tryAwaitRelease()
                    pressed = false; onPress(false)
                })
            },
        contentAlignment = Alignment.Center
    ) {
        Text("𝕏", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

// ── D-Pad with multi-touch (allows diagonals like Up+Right) ──────────────────

private val DPadDirections = listOf("up", "down", "left", "right")

@Composable
fun DPad(
    onDirection: (direction: String, pressed: Boolean) -> Unit
) {
    val pressedSet = remember { mutableStateMapOf<String, Boolean>() }

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

    val cellDp = 40.dp
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

                        for (change in event.changes) {
                            if (change.pressed) {
                                perPointerZone[change.id] = zoneAt(change.position, totalPx)
                            } else {
                                perPointerZone.remove(change.id)
                            }
                            change.consume()
                        }

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
    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
        for (cell in cells) {
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                if (cell != null && cell != "center") {
                    val isPressed = pressedSet[cell] == true
                    val cellScale by animateFloatAsState(
                        if (isPressed) 0.9f else 1f,
                        label = "dpad_$cell"
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .scale(cellScale)
                            .clip(RoundedCornerShape(5.dp))
                            .background(if (isPressed) XboxColors.AccentGreen else Color(0xFF1E1E2E))
                            .border(1.dp, XboxColors.Border, RoundedCornerShape(5.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            when (cell) { "up" -> "▲"; "down" -> "▼"; "left" -> "◀"; else -> "▶" },
                            color = XboxColors.TextPrimary,
                            fontSize = 11.sp
                        )
                    }
                } else if (cell == "center") {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
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
// Tap = full press (1.0). While holding, slide finger UPWARD to bleed off
// pressure (1 → 0 over ~80px). Drag back DOWN to re-engage. Lifting always
// returns to 0.

@Composable
fun TriggerButton(
    label: String,
    width: Dp = 72.dp,
    height: Dp = 44.dp,
    onValue: (Float) -> Unit,
) {
    var fill by remember { mutableFloatStateOf(0f) }
    val pressed = fill > 0f
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "trigger_scale")

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(XboxColors.Card)
            .border(1.5.dp, XboxColors.Border, RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitPointerEvent()
                        val first = down.changes.firstOrNull { it.pressed } ?: continue
                        val startId = first.id
                        val startY = first.position.y
                        var v = 1f
                        fill = v; onValue(v)
                        first.consume()

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
                    .clip(RoundedCornerShape(8.dp))
                    .background(XboxColors.AccentGreen.copy(alpha = 0.6f))
                    .align(Alignment.CenterStart)
            )
        }
        Text(label, color = XboxColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

// ── Small button (Back/Start/LB/RB/LS-click/RS-click) ───────────────────────

@Composable
fun SmallButton(
    label: String,
    onPress: (Boolean) -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.93f else 1f, label = "small_scale")
    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(if (pressed) XboxColors.AccentGreen else XboxColors.Card)
            .border(1.dp, XboxColors.Border, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    pressed = true; onPress(true)
                    tryAwaitRelease()
                    pressed = false; onPress(false)
                })
            }
    ) {
        Text(
            label,
            color = if (pressed) Color.White else XboxColors.TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
