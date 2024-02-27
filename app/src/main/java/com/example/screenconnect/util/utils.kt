package com.example.screenconnect.util

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.hardware.display.DisplayManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Display
import kotlin.system.exitProcess

fun getPhoneInfo(context: Context): com.example.screenconnect.models.Phone {
    val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)

    val displayMetrics = DisplayMetrics()
    display.getMetrics(displayMetrics)

    val height = displayMetrics.heightPixels
    val width = displayMetrics.widthPixels
    val DPI = displayMetrics.densityDpi

    val name = Build.MANUFACTURER + " " + Build.MODEL;

    val id = name + (0..100).random()

    return com.example.screenconnect.models.Phone(height, width, DPI, name, id)
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
