package com.example.screenconnect.network

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.example.screenconnect.MainActivity
import com.example.screenconnect.screens.SharedViewModel


class Connection(val context: Context, val sharedViewModel: SharedViewModel) {

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

    init {
        channel = manager?.initialize(context, Looper.getMainLooper(), null)
        channel?.also { channel ->
            receiver = WiFiDirectBroadcastReceiver(manager, channel, sharedViewModel, this)
        }
    }

    fun pause(){
        context.unregisterReceiver(receiver)
    }

    fun resume(){
        receiver = WiFiDirectBroadcastReceiver(manager, channel, sharedViewModel, this)
        context.registerReceiver(receiver, intentFilter)
    }

    fun requestConnectionInfo() {
        manager?.requestConnectionInfo(channel, WifiP2pManager.ConnectionInfoListener { info ->
            if (info.groupFormed) {
                sharedViewModel.isGroupOwner = info.isGroupOwner
                sharedViewModel.thisPhone.isHost = info.isGroupOwner
                sharedViewModel.isConnected = true
                sharedViewModel.connectedDeviceName = info.groupOwnerAddress.hostAddress
                sharedViewModel.host = info.groupOwnerAddress.hostAddress
                sharedViewModel.startServer()

                if(sharedViewModel.isGroupOwner){
                    sharedViewModel.infoText = "Host"
                }
                else{
                    sharedViewModel.infoText = "Connected"
                }

                Log.d("HOST-ADDRESS", info.groupOwnerAddress.hostAddress)

            } else {
                sharedViewModel.isConnected = false
                sharedViewModel.connectedDeviceName = ""
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun startPeerDiscovery() {
        val tempManager = manager ?: return

        tempManager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                sharedViewModel.infoText = "Discovery started"
                sharedViewModel.isDiscovering = false
                Log.d("DISCOVERY", "Success")
            }

            override fun onFailure(reasonCode: Int) {
                sharedViewModel.infoText = "Discovery failed"
                sharedViewModel.isDiscovering = false
                Log.d("DISCOVERY-ERROR", reasonCode.toString())
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun connectToPeer(device: WifiP2pDevice) {

        val tempManager = manager ?: return

        // Create a WifiP2pConfig object with the device address
        val config = WifiP2pConfig()
        config.deviceAddress = device.deviceAddress

        val name = device.deviceName

        tempManager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("CONNECT", "Connected to : $name")
            }

            override fun onFailure(reason: Int) {
                // Handle connection failure
                sharedViewModel.isConnected = false
                sharedViewModel.connectedDeviceName = ""

                Log.d("CONNECTION-ERROR", reason.toString())
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {

        val tempManager = manager ?: return

        if(sharedViewModel.isConnected) {
            Log.d("DISCONNECT", "start")

            tempManager.requestGroupInfo(channel,
                WifiP2pManager.GroupInfoListener { group ->
                    tempManager.removeGroup(channel, object : WifiP2pManager.ActionListener {
                        override fun onSuccess() {
                            Log.d("DISCONNECT", "Success")
                        }

                        override fun onFailure(reason: Int) {
                            Log.d("DISCONNECT-ERROR", reason.toString())
                        }
                    })
                })

            if (sharedViewModel.isGroupOwner) {
                sharedViewModel.messageServer.close()
                Log.d("DISCONNECT", "Server closed")
            } else {
                sharedViewModel.messageClient.close()
                Log.d("DISCONNECT", "Client closed")
            }

            sharedViewModel.isGroupOwner = false
            sharedViewModel.isConnected = false
            sharedViewModel.isServerRunning = false
        }
    }
}