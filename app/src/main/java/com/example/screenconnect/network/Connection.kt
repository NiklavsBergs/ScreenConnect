package com.example.screenconnect.network

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.screenconnect.MainActivity
import com.example.screenconnect.screens.SharedViewModel

class Connection(val context: Context, val activity: MainActivity, val sharedViewModel: SharedViewModel) {

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
    var peerListListener: WifiP2pManager.PeerListListener? = null

    init {
        channel = manager?.initialize(context, activity.mainLooper, null)
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
        Log.d("DISCOVERY", "Start")

        manager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                sharedViewModel.infoText = "Discovery started"
                Log.d("DISCOVERY", "Success")
                sharedViewModel.isDiscovering = false
            }

            override fun onFailure(reasonCode: Int) {
                sharedViewModel.infoText = "Discovery failed"
                Log.d("DISCOVERY", "Fail")
                Log.d("DISCOVERY-ERROR", reasonCode.toString())
                sharedViewModel.isDiscovering = false
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun connectToPeer(device: WifiP2pDevice) {
        // Create a WifiP2pConfig object with the device address
        val config = WifiP2pConfig()
        config.deviceAddress = device.deviceAddress

        var name = device.deviceName

        // Check if there is an active connection, disconnect if needed
        channel?.also { channel ->
            manager?.connect(channel, config, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d("CONNECT", "Connected to : $name")
                }

                override fun onFailure(reason: Int) {
                    // Handle connection failure
                    sharedViewModel.isConnected = false
                    sharedViewModel.connectedDeviceName = ""

                    Log.d("CONNECT", "Connection initiation failed: $reason")
                }
            })
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        if(sharedViewModel.isConnected) {
            Log.d("DISCONNECT", "start")
            if (manager != null && channel != null) {
                Log.d("DISCONNECT", "1")
                manager!!.requestGroupInfo(channel,
                    WifiP2pManager.GroupInfoListener { group ->
                        if (group != null && manager != null && channel != null) {
                            Log.d("DISCONNECT", "2")
                            manager!!.removeGroup(channel, object : WifiP2pManager.ActionListener {
                                override fun onSuccess() {
                                    Log.d("DISCONNECT", "removeGroup onSuccess -")
                                }

                                override fun onFailure(reason: Int) {
                                    Log.d("DISCONNECT", "removeGroup onFailure -$reason")
                                }
                            })
                        }
                    })

                if (sharedViewModel.isGroupOwner) {
                    sharedViewModel.messageServer.close()
                } else {
                    sharedViewModel.messageClient.close()
                }

                sharedViewModel.isConnected = false
                sharedViewModel.isServerRunning = false
            }
        }
    }

}