package com.example.screenconnect

import android.Manifest
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.core.app.ActivityCompat
import com.example.screenconnect.network.Connection
import com.example.screenconnect.network.LocationBroadcastReceiver
import com.example.screenconnect.screens.SharedViewModel
import com.example.screenconnect.ui.theme.ScreenConnectTheme
import com.example.screenconnect.util.getPhoneInfo
import com.example.screenconnect.util.locationPopup
import com.example.screenconnect.util.isLocationEnabled
import com.example.screenconnect.util.wifiPopup


class MainActivity : ComponentActivity() {

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
            Log.d("WIFI Popup", (!sharedViewModel.isWifiEnabled).toString())
            wifiPopup(this)
        }

        sharedViewModel.isLocationEnabled = isLocationEnabled(this)
        if (!sharedViewModel.isLocationEnabled ) {
            Log.d("Location Popup", "Location services are disabled")
            locationPopup(this)
            sharedViewModel.isLocationEnabled = isLocationEnabled(this)
        }

        sharedViewModel.thisPhone = getPhoneInfo(this)

        //sharedViewModel.virtualScreen.addPhone(sharedViewModel.thisPhone)

        connection.requestConnectionInfo()

        val br: BroadcastReceiver = LocationBroadcastReceiver(sharedViewModel)
        val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        registerReceiver(br, filter)

        setContent {
            ScreenConnectTheme {

                Navigation(sharedViewModel, connection)

            }
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

