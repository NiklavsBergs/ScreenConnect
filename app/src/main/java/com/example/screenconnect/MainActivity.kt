package com.example.screenconnect

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View


import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Density
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.screenconnect.network.Connection
import com.example.screenconnect.screens.SharedViewModel
import com.example.screenconnect.network.LocationBroadcastReceiver
import com.example.screenconnect.util.getPhoneInfo
import com.example.screenconnect.util.locationPopup
import com.example.screenconnect.util.isLocationEnabled
import com.example.screenconnect.util.wifiPopup


class MainActivity : ComponentActivity() {

    private lateinit var connection: Connection

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        // Make app fullscreen
        hideSystemUI()

        // Initialize needed classes
        val sharedViewModel: SharedViewModel by viewModels()
        connection = Connection(this,  sharedViewModel)

        val br: BroadcastReceiver = LocationBroadcastReceiver(sharedViewModel)
        val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        registerReceiver(br, filter)

        sharedViewModel.logoBitmap = getLogoBitmap(this)

        sharedViewModel.thisPhone = getPhoneInfo(this, windowManager)

        sharedViewModel.virtualScreen.addFirstPhone(sharedViewModel.thisPhone)

        // Ask for permissions and services
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

        setContent {

            Box(modifier = Modifier
                .fillMaxSize()) {
                Navigation(sharedViewModel, connection)
            }

        }

        // Calls for current WiFi Direct connection status
        connection.requestConnectionInfo()

        Log.d("NEW HEIGHT", sharedViewModel.thisPhone.height.toString())

        this.onBackPressedDispatcher.addCallback(this) {
            sharedViewModel.showImage = false
            Log.d("BACK HANDLE", sharedViewModel.showImage.toString())
        }


    }

    private fun getLogoBitmap(context: Context): Bitmap {
        val drawable = ContextCompat.getDrawable(context, R.drawable.logo)

        if (drawable == null) {
            return Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        }

        val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    public override fun onResume() {
        super.onResume()
        connection.resume()
    }

    public override fun onPause() {
        super.onPause()
        connection.pause()
    }

    private fun hideSystemUI(){
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)

        insetsController.apply {
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        if (Build.VERSION.SDK_INT >= 30) {
            enableEdgeToEdge()
        }

        if (Build.VERSION.SDK_INT < 30) {
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        }
    }
}

