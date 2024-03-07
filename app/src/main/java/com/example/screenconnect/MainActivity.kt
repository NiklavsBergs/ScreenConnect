package com.example.screenconnect

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.graphics.Canvas
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View


import android.view.WindowInsetsController
import android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.screenconnect.network.Connection
import com.example.screenconnect.network.LocationBroadcastReceiver
import com.example.screenconnect.screens.SharedViewModel
import com.example.screenconnect.ui.theme.ScreenConnectTheme
import com.example.screenconnect.util.getPhoneInfo
//import com.example.screenconnect.util.getPhoneInfo
import com.example.screenconnect.util.locationPopup
import com.example.screenconnect.util.isLocationEnabled
import com.example.screenconnect.util.wifiPopup
import com.google.accompanist.systemuicontroller.SystemUiController
import com.google.accompanist.systemuicontroller.rememberSystemUiController


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
        connection = Connection(this, this, sharedViewModel)

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

        var topAdded by mutableStateOf(false)

        var bottomAdded by mutableStateOf(false)

        var topValues by  mutableFloatStateOf(0F)
        var bottomValues by  mutableIntStateOf(0)
        var bottomValuesT by  mutableFloatStateOf(0F)

        sharedViewModel.thisPhone = getPhoneInfo(this, windowManager)

        sharedViewModel.virtualScreen.addFirst(sharedViewModel.thisPhone)

//        val displayMetrics = resources.displayMetrics
//
//        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
//
//        Log.d("TEST DISPLAY HEIGHT", displayMetrics.heightPixels.toString())

        setContent {
//            if (Build.VERSION.SDK_INT < 30) {
//                WindowCompat.getInsetsController(window, window.decorView)
//                    .hide(WindowInsetsCompat.Type.systemBars())
//            }

//            topValues = WindowInsets.systemBars.asPaddingValues().calculateTopPadding().value * Density(this).density
//
//            bottomValuesT = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding().value * Density(this).density
//
//            bottomValues = WindowInsets.systemBars.getBottom(Density(this))
//
//
//            if(!topAdded && topValues > 0){
//                Log.d("PADDING VALUES ADDING TOP", topValues.toString())
//                sharedViewModel.thisPhone.addToHeight(topValues.toInt())
//                topAdded = true
//            }
//
//            if(!bottomAdded && (bottomValues > 0 || bottomValuesT > 0)){
//                Log.d("PADDING VALUES ADDING BOTTOM Y", bottomValuesT.toString())
//                Log.d("PADDING VALUES ADDING BOTTOM", bottomValues.toString())
//                sharedViewModel.thisPhone.addToHeight(bottomValues.toInt())
//                bottomAdded = true
//            }

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
    //}

//    @RequiresApi(30)
//    fun getPhoneInfo(context: Context): com.example.screenconnect.models.Phone {
//
//        val displayMetrics = context.resources.displayMetrics
//
//
//
//        val height = displayMetrics.heightPixels //+ statusBarHeight + navigationBarHeight
//        val width = displayMetrics.widthPixels
//        val xDPI = displayMetrics.xdpi.toInt()
//        val yDPI = displayMetrics.ydpi.toInt()
//
//        val DPI = (xDPI+yDPI)/2
//
//        Log.d("DPI", DPI.toString())
//
//        val name = Build.MANUFACTURER + " " + Build.MODEL;
//
//        val id = name + (0..100).random()
//
//        return com.example.screenconnect.models.Phone(height, width, DPI, name, id)
//    }

}

