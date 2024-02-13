package com.example.screenconnect

import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat.getSystemService
import androidx.navigation.NavController

@Composable
fun MainScreen(navController: NavController) {

    val context = LocalContext.current

    val intentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }

    val manager: WifiP2pManager? by lazy(LazyThreadSafetyMode.NONE) {
        context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager?
    }

    var channel: WifiP2pManager.Channel? = null
    var receiver: BroadcastReceiver? = null
    var peerListListener: WifiP2pManager.PeerListListener? = null

    var isWifiP2pEnabled by remember{ mutableStateOf(false)}
    var isDiscovering by remember{ mutableStateOf(false) }

    var peerList by remember{ mutableStateOf<WifiP2pDeviceList?>(null) }

    var isGroupOwner by remember{  mutableStateOf(false)  }
    var isConnected by remember{ mutableStateOf(false) }
    var connectedDeviceName by remember{ mutableStateOf("")}

    var isServerRunning by remember{ mutableStateOf(false)}
    
    Column {
        Text(text = "Settings screen")

        Button(onClick = {
            navController.navigate(Screen.ViewScreen.route)
        }) {
            Text(text = "View Screen")
        }
    }
    
}