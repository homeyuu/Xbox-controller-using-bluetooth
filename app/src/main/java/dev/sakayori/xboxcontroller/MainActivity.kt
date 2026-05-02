package dev.sakayori.xboxcontroller

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.sakayori.xboxcontroller.hid.HidConnectionState
import dev.sakayori.xboxcontroller.ui.ConnectScreen
import dev.sakayori.xboxcontroller.ui.ControllerScreen
import dev.sakayori.xboxcontroller.ui.XboxColors

class MainActivity : ComponentActivity() {

    private val vm: ControllerViewModel by viewModels()

    private val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADVERTISE)
    else
        arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)

    private var permissionsGranted by mutableStateOf(false)

    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        permissionsGranted = results.values.all { it }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        permissionsGranted = permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
        if (!permissionsGranted) permLauncher.launch(permissions)

        setContent {
            val connState     by vm.connectionState.collectAsState()
            val statusMessage by vm.statusMessage.collectAsState()
            val pairedDevices by vm.pairedDevices.collectAsState()

            Box(Modifier.fillMaxSize().background(XboxColors.Background).systemBarsPadding()) {
                if (!permissionsGranted) {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⚠️", fontSize = 48.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("Cần quyền Bluetooth", color = XboxColors.TextPrimary, textAlign = TextAlign.Center)
                    }
                } else {
                    AnimatedContent(targetState = connState, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "screen") { state ->
                        if (state == HidConnectionState.CONNECTED) {
                            ControllerScreen(vm)
                        } else {
                            ConnectScreen(
                                state = state,
                                statusMessage = statusMessage,
                                pairedDevices = pairedDevices,
                                onStart = { vm.startHid() },
                                onStop = { vm.stopHid() },
                                onConnectDevice = { vm.connectTo(it) },
                                onRefresh = { vm.refreshDevices() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() { super.onResume(); vm.refreshDevices() }
}
