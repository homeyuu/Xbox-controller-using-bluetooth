package dev.sakayori.xboxcontroller.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.sakayori.xboxcontroller.ControllerViewModel
import dev.sakayori.xboxcontroller.hid.XboxHidDescriptor.Button

@Composable
fun ControllerScreen(vm: ControllerViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(XboxColors.Background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // ── Top bar: shoulders + center cluster ─────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left shoulder
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TriggerButton("LT") { vm.onLeftTrigger(it) }
                SmallButton("LB") { vm.onButton(Button.LB, it) }
            }

            // Center cluster: Back / Xbox / Start
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                SmallButton("◀ Back") { vm.onButton(Button.BACK, it) }
                XboxGuideButton { vm.onButton(Button.GUIDE, it) }
                SmallButton("Start ▶") { vm.onButton(Button.START, it) }
            }

            // Right shoulder
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TriggerButton("RT") { vm.onRightTrigger(it) }
                SmallButton("RB") { vm.onButton(Button.RB, it) }
            }
        }

        // ── Left side: LS (top) + DPad (bottom) ─────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(top = 90.dp, bottom = 50.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Thumbstick(label = "LS", size = 130.dp) { x, y -> vm.onLeftStick(x, y) }
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
        }

        // ── Right side: ABXY (top) + RS (bottom) ────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(top = 90.dp, bottom = 50.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ABXY diamond (130dp box, 56dp buttons)
            Box(
                modifier = Modifier.size(140.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.align(Alignment.TopCenter)) {
                    FaceButton("Y", XboxColors.ButtonY) { vm.onButton(Button.Y, it) }
                }
                Box(Modifier.align(Alignment.CenterStart)) {
                    FaceButton("X", XboxColors.ButtonX) { vm.onButton(Button.X, it) }
                }
                Box(Modifier.align(Alignment.CenterEnd)) {
                    FaceButton("B", XboxColors.ButtonB) { vm.onButton(Button.B, it) }
                }
                Box(Modifier.align(Alignment.BottomCenter)) {
                    FaceButton("A", XboxColors.ButtonA) { vm.onButton(Button.A, it) }
                }
            }
            Thumbstick(label = "RS", size = 130.dp) { x, y -> vm.onRightStick(x, y) }
        }

        // ── Bottom strip: LS / RS click ─────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SmallButton("LS ↓") { vm.onButton(Button.LS, it) }
            SmallButton("RS ↓") { vm.onButton(Button.RS, it) }
        }
    }
}
