package dev.sakayori.xboxcontroller.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sakayori.xboxcontroller.ControllerViewModel
import dev.sakayori.xboxcontroller.hid.XboxHidDescriptor.Button

@Composable
fun ControllerScreen(vm: ControllerViewModel) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(XboxColors.Background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // ── Top bar ────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LT / LB
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TriggerButton("LT") { vm.onLeftTrigger(it) }
                    SmallButton("LB") { vm.onButton(Button.LB, it) }
                }

                // Back / Xbox / Start
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SmallButton("◀ Back") { vm.onButton(Button.BACK, it) }

                    // Xbox guide button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(XboxColors.AccentBright, Color(0xFF063A06))
                                )
                            )
                            .border(2.dp, XboxColors.AccentGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("𝕏", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }

                    SmallButton("Start ▶") { vm.onButton(Button.START, it) }
                }

                // RT / RB
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TriggerButton("RT") { vm.onRightTrigger(it) }
                    SmallButton("RB") { vm.onButton(Button.RB, it) }
                }
            }

            // ── Mid row: DPad + Left Stick + Right Stick + Face Buttons ───
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: DPad
                DPad { dir, pressed ->
                    val bit = when (dir) {
                        "up"    -> Button.DPAD_UP
                        "down"  -> Button.DPAD_DOWN
                        "left"  -> Button.DPAD_LEFT
                        "right" -> Button.DPAD_RIGHT
                        else    -> return@DPad
                    }
                    vm.onButton(bit, pressed)
                }

                // Left stick
                Thumbstick(label = "LS") { x, y -> vm.onLeftStick(x, y) }

                // Right stick
                Thumbstick(label = "RS") { x, y -> vm.onRightStick(x, y) }

                // Face buttons (ABXY diamond)
                Box(
                    modifier = Modifier.size(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Y (top)
                    Box(Modifier.align(Alignment.TopCenter)) {
                        FaceButton("Y", XboxColors.ButtonY) { vm.onButton(Button.Y, it) }
                    }
                    // X (left)
                    Box(Modifier.align(Alignment.CenterStart)) {
                        FaceButton("X", XboxColors.ButtonX) { vm.onButton(Button.X, it) }
                    }
                    // B (right)
                    Box(Modifier.align(Alignment.CenterEnd)) {
                        FaceButton("B", XboxColors.ButtonB) { vm.onButton(Button.B, it) }
                    }
                    // A (bottom)
                    Box(Modifier.align(Alignment.BottomCenter)) {
                        FaceButton("A", XboxColors.ButtonA) { vm.onButton(Button.A, it) }
                    }
                }
            }

            // ── Bottom: LS/RS click hints ──────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SmallButton("LS ↓") { vm.onButton(Button.LS, it) }
                SmallButton("RS ↓") { vm.onButton(Button.RS, it) }
            }
        }
    }
}
