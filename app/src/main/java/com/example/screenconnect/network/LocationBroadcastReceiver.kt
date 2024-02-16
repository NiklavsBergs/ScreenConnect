package com.example.screenconnect.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.util.Log
import com.example.screenconnect.screens.SharedViewModel

class LocationBroadcastReceiver(private val sharedViewModel: SharedViewModel,) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        intent.action?.let { act ->
            if (act.matches("android.location.PROVIDERS_CHANGED".toRegex())) {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                sharedViewModel.isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                Log.d("LOCATION", "Location Providers changed, is GPS Enabled: " + sharedViewModel.isLocationEnabled)
            }
        }
    }
}