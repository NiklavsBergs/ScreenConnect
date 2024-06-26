package com.example.screenconnect.network

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import com.example.screenconnect.screens.SharedViewModel

class WiFiDirectBroadcastReceiver(private val manager: WifiP2pManager?, private val channel: WifiP2pManager.Channel?, private val sharedViewModel: SharedViewModel, private val connection: Connection) : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {

        when(intent.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                // Determine if Wi-Fi Direct is enabled or not
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                sharedViewModel.isWifiEnabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED
            }
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                // Available device list changed, update it in UI
                manager?.requestPeers(channel) { peers: WifiP2pDeviceList? ->
                    peers?.let { sharedViewModel.peerList = it }
                    if(peers?.deviceList?.isNotEmpty() == true && !sharedViewModel.isConnected){
                        sharedViewModel.infoText = "Peers found"
                    }
                    else if(!sharedViewModel.isConnected){
                        sharedViewModel.infoText = "No peers found"
                    }
                }


            }
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                // WiFi Direct connection changes (connect or disconnect)
                val networkInfo: WifiP2pInfo? = intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO) as WifiP2pInfo?

                if (networkInfo?.groupFormed == true) {
                    // Connection is established
                    sharedViewModel.isGroupOwner = networkInfo?.isGroupOwner == true
                    sharedViewModel.isConnected = true
                    sharedViewModel.connectedDeviceName = networkInfo?.groupOwnerAddress.toString()
                    sharedViewModel.host = networkInfo?.groupOwnerAddress.toString()
                    connection.requestConnectionInfo()
                    if(sharedViewModel.isGroupOwner){
                        sharedViewModel.infoText = "Host"
                        sharedViewModel.thisPhone.isHost = true
                    }
                    else{
                        sharedViewModel.infoText = "Connected"
                    }

                } else {
                    // Connection is lost
                    sharedViewModel.connectedDeviceName = ""
                    sharedViewModel.infoText = "Not connected"
                    sharedViewModel.isConnected = false
                    connection.disconnect()
                }

            }
            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {}
        }
    }



}