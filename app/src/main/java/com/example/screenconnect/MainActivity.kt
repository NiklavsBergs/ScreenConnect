package com.example.screenconnect

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.location.LocationManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.ActionListener
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener
import android.net.wifi.p2p.WifiP2pManager.PeerListListener
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.example.screenconnect.models.PhoneScreen
import com.example.screenconnect.models.VirtualScreen
import com.example.screenconnect.network.Connection
import com.example.screenconnect.network.LocationBroadcastReceiver
import com.example.screenconnect.network.MessageClient
import com.example.screenconnect.network.MessageServer
import com.example.screenconnect.network.WiFiDirectBroadcastReceiver
import com.example.screenconnect.screens.SharedViewModel
import com.example.screenconnect.ui.theme.ScreenConnectTheme
import com.example.screenconnect.util.getPhoneInfo
import com.example.screenconnect.util.locationPopup
import com.example.screenconnect.util.isLocationEnabled
import com.example.screenconnect.util.wifiPopup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.system.exitProcess


class MainActivity : ComponentActivity() {

    //var text by mutableStateOf("Hello")

    lateinit var connection: Connection

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedViewModel: SharedViewModel by viewModels()
        connection = Connection(this, this, sharedViewModel)

        ActivityCompat.requestPermissions(
            this@MainActivity, arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.NEARBY_WIFI_DEVICES
            ), 0
        )

        val wifi = getSystemService(WIFI_SERVICE) as WifiManager
        sharedViewModel.isWifiEnabled = wifi.isWifiEnabled
        if(!sharedViewModel.isWifiEnabled) {
            Log.d("WIFI Popup in", (!sharedViewModel.isWifiEnabled).toString())
            wifiPopup(this)
        }
        else{
            sharedViewModel.infoText = "Not connected"
        }

        sharedViewModel.isLocationEnabled = isLocationEnabled(this)
        if (!sharedViewModel.isLocationEnabled ) {
            Log.d("Location Popup", "Location services are disabled")
            locationPopup(this)
            sharedViewModel.isLocationEnabled = isLocationEnabled(this)
        }

        sharedViewModel.thisPhone = getPhoneInfo(this)

        sharedViewModel.virtualScreen.addPhone(sharedViewModel.thisPhone)

        connection.requestConnectionInfo()

        val br: BroadcastReceiver = LocationBroadcastReceiver(sharedViewModel)
        val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        registerReceiver(br, filter)

        setContent {
            ScreenConnectTheme {

                Navigation(sharedViewModel, connection)

//                if(!sharedViewModel.showImage){
//                    settingsScreen(sharedViewModel, connection)
//                }
//                else{
//                    sharedScreen(sharedViewModel)
//                }

            }
        }

        if(connection.manager != null){
            Log.d("MANAGER", "Good")
        }
        else{
            Log.d("MANAGER", "null")
        }

        if(connection.channel != null){
            Log.d("CHANNEL", "Good")
        }
        else{
            Log.d("CHANNEL", "null")
        }

    }


    public override fun onResume() {
        super.onResume()
        connection.resume()
    }

    public override fun onPause() {
        super.onPause()
        connection.pause()
    }

}

