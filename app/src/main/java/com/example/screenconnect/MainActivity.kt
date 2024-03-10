package com.example.screenconnect

import android.Manifest
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.util.Log


import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Density
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.screen_connect.network.Connection
import com.example.screen_connect.screens.SharedViewModel
//import com.example.screenconnect.network.Connection
import com.example.screen_connect.network.LocationBroadcastReceiver
//import com.example.screenconnect.screens.SharedViewModel
import com.example.screen_connect.util.getPhoneInfo
import com.example.screen_connect.util.locationPopup
import com.example.screen_connect.util.isLocationEnabled
import com.example.screen_connect.util.wifiPopup


class MainActivity : ComponentActivity() {

    lateinit var connection: Connection

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)

        if (Build.VERSION.SDK_INT < 30) {

        }
        else{
            enableEdgeToEdge()

            insetsController.apply {
                hide(WindowInsetsCompat.Type.navigationBars())

                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }


        super.onCreate(savedInstanceState)

        val sharedViewModel: SharedViewModel by viewModels()
        connection = Connection(this,  sharedViewModel)

        ActivityCompat.requestPermissions(
            this@MainActivity, arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.NEARBY_WIFI_DEVICES,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
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



        val br: BroadcastReceiver = LocationBroadcastReceiver(sharedViewModel)
        val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        registerReceiver(br, filter)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        sharedViewModel.thisPhone = getPhoneInfo(this, windowManager)

        sharedViewModel.virtualScreen.addFirstPhone(sharedViewModel.thisPhone)


        setContent {

            Box(modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .navigationBarsPadding() ) {
                Navigation(sharedViewModel, connection)
            }

        }

        if (Build.VERSION.SDK_INT < 30) {
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

            insetsController.apply {
                hide(WindowInsetsCompat.Type.navigationBars())

                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }

        connection.requestConnectionInfo()

        Log.d("NEW HEIGHT", sharedViewModel.thisPhone.height.toString())

    }

    public override fun onResume() {
        super.onResume()
        connection.resume()
    }

    public override fun onPause() {
        super.onPause()
        connection.pause()
    }


    @RequiresApi(Build.VERSION_CODES.R)
    @Composable
    fun hideSystemUI(){
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)

        if (Build.VERSION.SDK_INT < 30) {
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            window.insetsController?.apply {
                hide(WindowInsets.systemBars.getBottom(Density(LocalContext.current)))
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        else{
            enableEdgeToEdge()

            insetsController.apply {
                hide(WindowInsetsCompat.Type.navigationBars())

                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

//    @RequiresApi(Build.VERSION_CODES.R)
//    fun hideSystemUI() {
//
//        //Hides the ugly action bar at the top
//        actionBar?.hide()
//
//        //Hide the status bars
//
//        WindowCompat.setDecorFitsSystemWindows(window, false)

//        window.insetsController?.apply {
//                hide(WindowInsets.systemBars.getBottom())
//                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
//            }

//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
//            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
//        } else {
//            window.insetsController?.apply {
//                hide(WindowInsets.Type.statusBars())
//                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
//            }
//        }
// }

}

