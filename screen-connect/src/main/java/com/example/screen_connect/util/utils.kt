package com.example.screen_connect.util

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import androidx.annotation.RequiresApi
import com.example.screen_connect.models.Phone
import kotlin.system.exitProcess

@RequiresApi(Build.VERSION_CODES.R)
fun getPhoneInfo(context: Context, windowManager: WindowManager):Phone {

    val displayMetrics = context.resources.displayMetrics

    windowManager.defaultDisplay.getRealMetrics(displayMetrics)

    val height = displayMetrics.heightPixels
    val width = displayMetrics.widthPixels
    val xDPI = displayMetrics.xdpi.toInt()
    val yDPI = displayMetrics.ydpi.toInt()

    val testDPI = displayMetrics.densityDpi

    Log.d("DPI density", displayMetrics.density.toString())

    Log.d("DPI test", testDPI.toString())

    Log.d("DPI X", xDPI.toString())
    Log.d("DPI Y", yDPI.toString())

    val DPI = yDPI //(xDPI+yDPI)/2

    Log.d("DPI", DPI.toString())

    val name = Build.MANUFACTURER + " " + Build.MODEL;

    val id = name + (0..100).random()

    var phone = Phone(height, width, DPI, name, id)

    return phone
}

fun wifiPopup(context: Context){
    val builder = AlertDialog.Builder(
        context
    )
    builder.setTitle("Screen Connect")
    builder.setMessage("WiFi is off, but is needed for this application. Do you want to turn it on?")
    builder.setPositiveButton(
        "Enable WiFi"
    ) { dialogInterface, i ->
        context.startActivity(
            Intent(
                Settings.ACTION_WIFI_SETTINGS
            )
        )
    }
    builder.setNegativeButton(
        "Exit"
    ) { dialogInterface, i ->
        context.startActivity(
            exitProcess(0)
        )
    }
    builder.create().show()
}

fun locationPopup(context: Context) {

    val builder = AlertDialog.Builder(context)
    builder.setTitle("Screen Connect")
    builder.setMessage("Location services are disabled, but are needed for this application. Do you want to turn them on?")
    builder.setPositiveButton(
        "Enable Location"
    ) { dialogInterface, i ->
        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }
    builder.setNegativeButton(
        "Exit"
    ) { dialogInterface, i ->
        context.startActivity(exitProcess(0))
    }
    builder.create().show()
}

fun isLocationEnabled(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
}

fun getRealPathFromUri(uri: Uri, context: Context): String? {
    var filePath: String? = null

    val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        it.moveToFirst()
        val columnIndex = it.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
        filePath = it.getString(columnIndex)
    }
    return filePath
}


