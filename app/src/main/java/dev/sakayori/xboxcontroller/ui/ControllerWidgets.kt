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
            // Knob
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

// ── D-Pad ─────────────────────────────────────────────────────────────────────

@Composable
fun DPad(
    onDirection: (direction: String, pressed: Boolean) -> Unit
) {
    val pressedMap = remember { mutableStateMapOf<String, Boolean>() }

    fun btn(dir: String) = @Composable {
        val isCenter = dir == "center"
        Box(
            modifier = Modifier
                .size(if (isCenter) 28.dp else 34.dp)
                .clip(if (isCenter) CircleShape else RoundedCornerShape(4.dp))
                .background(if (pressedMap[dir] == true) XboxColors.AccentGreen else Color(0xFF1E1E2E))
                .border(1.dp, XboxColors.Border, if (isCenter) CircleShape else RoundedCornerShape(4.dp))
                .pointerInput(dir) {
                    detectTapGestures(
                        onPress = {
                            pressedMap[dir] = true
                            onDirection(dir, true)
                            tryAwaitRelease()
                            pressedMap[dir] = false
                            onDirection(dir, false)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (!isCenter) {
                Text(
                    when (dir) { "up" -> "▲"; "down" -> "▼"; "left" -> "◀"; else -> "▶" },
                    color = XboxColors.TextPrimary,
                    fontSize = 10.sp
                )
            }
        }
    }

    // 3x3 grid
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(modifier = Modifier.height(36.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(36.dp))
            btn("up")()
            Spacer(Modifier.width(36.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
            btn("left")()
            btn("center")()
            btn("right")()
        }
        Row(modifier = Modifier.height(36.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(36.dp))
            btn("down")()
            Spacer(Modifier.width(36.dp))
        }
    }
}

// ── Trigger ───────────────────────────────────────────────────────────────────

@Composable
fun TriggerButton(
    label: String,
    onValue: (Float) -> Unit,
) {
    var fill by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .width(60.dp)
            .height(36.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(XboxColors.Card)
            .border(1.5.dp, XboxColors.Border, RoundedCornerShape(6.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        fill = 1f
                        onValue(1f)
                        tryAwaitRelease()
                        fill = 0f
                        onValue(0f)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Fill bar
        if (fill > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(6.dp))
                    .background(XboxColors.AccentGreen.copy(alpha = 0.6f))
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
