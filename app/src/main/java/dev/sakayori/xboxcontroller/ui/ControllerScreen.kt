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
            .padding(horizontal = 20.dp, vertical = 14.dp)
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TriggerButton("LT") { vm.onLeftTrigger(it) }
                SmallButton("LB") { vm.onButton(Button.LB, it) }
            }

            // Center cluster: Back / Xbox / Start
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                SmallButton("◀ Back") { vm.onButton(Button.BACK, it) }
                XboxGuideButton(size = 56.dp) { vm.onButton(Button.GUIDE, it) }
                SmallButton("Start ▶") { vm.onButton(Button.START, it) }
            }

            // Right shoulder
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TriggerButton("RT") { vm.onRightTrigger(it) }
                SmallButton("RB") { vm.onButton(Button.RB, it) }
            }
        }

        // ── Left side: LS (top) + DPad (bottom) ─────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(top = 96.dp, bottom = 50.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Thumbstick(label = "LS", size = 140.dp) { x, y -> vm.onLeftStick(x, y) }
            DPad { dir, pressed -> vm.onDpad(dir, pressed) }
        }

        // ── Right side: ABXY (top) + RS (bottom) ────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(top = 96.dp, bottom = 50.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ABXY diamond — bigger box (180dp) + bigger buttons (66dp) so
            // they're well-spaced and easier to hit.
            Box(
                modifier = Modifier.size(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.align(Alignment.TopCenter)) {
                    FaceButton("Y", XboxColors.ButtonY, size = 66.dp) {
                        vm.onButton(Button.Y, it)
                    }
                }
                Box(Modifier.align(Alignment.CenterStart)) {
                    FaceButton("X", XboxColors.ButtonX, size = 66.dp) {
                        vm.onButton(Button.X, it)
                    }
                }
                Box(Modifier.align(Alignment.CenterEnd)) {
                    FaceButton("B", XboxColors.ButtonB, size = 66.dp) {
                        vm.onButton(Button.B, it)
                    }
                }
                Box(Modifier.align(Alignment.BottomCenter)) {
                    FaceButton("A", XboxColors.ButtonA, size = 66.dp) {
                        vm.onButton(Button.A, it)
                    }
                }
            }
            Thumbstick(label = "RS", size = 140.dp) { x, y -> vm.onRightStick(x, y) }
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
