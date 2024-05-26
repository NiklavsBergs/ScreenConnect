package com.example.screenconnect.network

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Looper
import android.util.Log
import com.example.screenconnect.screens.SharedViewModel

// Handles WiFi Direct communication between devices, https://developer.android.com/develop/connectivity/wifi/wifip2p
class Connection(val context: Context, val sharedViewModel: SharedViewModel) {

    private val intentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }

    private val manager: WifiP2pManager? by lazy(LazyThreadSafetyMode.NONE) {
        context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager?
    }

    private var channel: WifiP2pManager.Channel? = null
    private var receiver: BroadcastReceiver? = null

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
            }

            override fun onFailure(reasonCode: Int) {
                sharedViewModel.infoText = "Discovery failed"
                sharedViewModel.isDiscovering = false
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun connectToPeer(device: WifiP2pDevice) {

        val tempManager = manager ?: return

        // Create a WifiP2pConfig object with the device address
        val config = WifiP2pConfig()
        config.deviceAddress = device.deviceAddress

        tempManager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
            }

            override fun onFailure(reason: Int) {
                // Handle connection failure
                sharedViewModel.isConnected = false
                sharedViewModel.connectedDeviceName = ""
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {

        val tempManager = manager ?: return

        if(sharedViewModel.isConnected) {

            tempManager.requestGroupInfo(channel,
                WifiP2pManager.GroupInfoListener { group ->
                    tempManager.removeGroup(channel, object : WifiP2pManager.ActionListener {
                        override fun onSuccess() {
                            if (sharedViewModel.isGroupOwner) {
                                sharedViewModel.messageServer.close()
                                sharedViewModel.isGroupOwner = false
                            } else {
                                sharedViewModel.messageClient.close()
                            }

                            sharedViewModel.isConnected = false
                            sharedViewModel.isServerRunning = false
                        }
                        override fun onFailure(reason: Int) {}
                    })
                })
        }
    }
}