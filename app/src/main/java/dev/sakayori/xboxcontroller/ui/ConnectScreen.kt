package dev.sakayori.xboxcontroller.ui

import android.bluetooth.BluetoothDevice
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sakayori.xboxcontroller.hid.HidConnectionState

@Composable
fun ConnectScreen(
    state: HidConnectionState,
    statusMessage: String,
    pairedDevices: List<BluetoothDevice>,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onConnectDevice: (BluetoothDevice) -> Unit,
    onRefresh: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(XboxColors.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // ── Logo ────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(XboxColors.AccentBright, Color(0xFF063A06))
                        )
                    )
                    .border(2.dp, XboxColors.AccentGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("𝕏", fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }

            Text(
                "Xbox 360 Controller",
                color = XboxColors.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            // ── Status card ─────────────────────────────────────────────────
            val statusColor = when (state) {
                HidConnectionState.CONNECTED   -> XboxColors.AccentBright
                HidConnectionState.WAITING_PAIR -> XboxColors.ButtonY
                HidConnectionState.REGISTERING  -> XboxColors.ButtonX
                HidConnectionState.ERROR        -> XboxColors.Danger
                HidConnectionState.IDLE         -> XboxColors.TextMuted
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = XboxColors.Card,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Text(
                        statusMessage.ifEmpty { "Nhấn Start để bắt đầu" },
                        color = XboxColors.TextPrimary,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Instructions ────────────────────────────────────────────────
            if (state == HidConnectionState.WAITING_PAIR || state == HidConnectionState.IDLE) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0D0D1A),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Hướng dẫn kết nối:", color = XboxColors.ButtonY, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        listOf(
                            "1. Nhấn \"Bắt đầu\" bên dưới",
                            "2. Vào Settings → Bluetooth trên Android TV",
                            "3. Tìm \"Xbox 360 Controller\" → Pair",
                            "4. Màn hình này sẽ tự đóng khi kết nối thành công"
                        ).forEach {
                            Text(it, color = XboxColors.TextMuted, fontSize = 12.sp)
                        }
                    }
                }
            }

            // ── Paired devices ──────────────────────────────────────────────
            if (pairedDevices.isNotEmpty() && state == HidConnectionState.WAITING_PAIR) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Thiết bị đã pair:", color = XboxColors.TextMuted, fontSize = 12.sp)
                        TextButton(onClick = onRefresh) {
                            Text("Refresh", color = XboxColors.AccentBright, fontSize = 11.sp)
                        }
                    }
                    pairedDevices.take(5).forEach { device ->
                        @Suppress("MissingPermission")
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = XboxColors.Card,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onConnectDevice(device) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(device.name ?: "Unknown", color = XboxColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text(device.address, color = XboxColors.TextMuted, fontSize = 10.sp)
                                }
                                Text("Kết nối →", color = XboxColors.AccentBright, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // ── Action buttons ──────────────────────────────────────────────
            Spacer(Modifier.height(4.dp))

            if (state == HidConnectionState.IDLE) {
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = XboxColors.AccentGreen)
                ) {
                    Text("Bắt đầu — Bật BT HID", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            } else {
                OutlinedButton(
                    onClick = onStop,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = XboxColors.Danger),
                    border = androidx.compose.foundation.BorderStroke(1.dp, XboxColors.Danger)
                ) {
                    Text("Dừng & ngắt kết nối", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
