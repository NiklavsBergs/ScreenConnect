package com.example.screenconnect.screens

import android.net.wifi.p2p.WifiP2pDeviceList
import android.os.Handler
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import com.example.screenconnect.models.PhoneScreen
import com.example.screenconnect.models.VirtualScreen
import com.example.screenconnect.network.MessageClient
import com.example.screenconnect.network.MessageServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SharedViewModel : ViewModel() {

    var text by mutableStateOf("Hello")
    var infoText by mutableStateOf("Info");

    var host by mutableStateOf("")

    var peerList by mutableStateOf<WifiP2pDeviceList?>(null)

    var isWifiP2pEnabled by mutableStateOf(false)
    var isDiscovering by mutableStateOf(false)
    var isGroupOwner by mutableStateOf(false)
    var isConnected by mutableStateOf(false)
    var connectedDeviceName by mutableStateOf("")

    var isServerRunning by mutableStateOf(false)

    private val serverScope = CoroutineScope(Dispatchers.IO)
    private val clientScope = CoroutineScope(Dispatchers.IO)

    lateinit var messageClient: MessageClient
    lateinit var messageServer: MessageServer

    lateinit var thisPhone: PhoneScreen

    var virtualScreen: VirtualScreen = VirtualScreen()

    var virtualHeight by mutableStateOf(0)
    var virtualWidth by mutableStateOf(0)

    var phoneNr by mutableStateOf(0)

    fun sendText(message: String){
        Log.d("SEND-TEXT", "Call")
        if(!isServerRunning){
            startServer()
        }
        else{
            if (isGroupOwner) {
                serverScope.launch {
                    messageServer.send(message)
                    Log.d("MESSAGE-SERVER", "Sending...")
                }
            } else {
                clientScope.launch {
                    messageClient.send(message)
                    Log.d("MESSAGE-CLIENT", "Sent")
                }
            }
        }
    }

    fun sendInfo(phone: PhoneScreen){
        Log.d("SEND-TEXT", "Call")
        if(!isServerRunning){
            startServer()
        }
        else{
            if (isGroupOwner) {
                serverScope.launch {
                    messageServer.sendPhoneInfo(phone)
                    Log.d("MESSAGE-SERVER", "Sending...")
                }
            } else {
                clientScope.launch {
                    messageClient.sendPhoneInfo(phone)
                    Log.d("MESSAGE-CLIENT", "Sent")
                }
            }
        }
    }

    fun startServer(){

        //requestConnectionInfo()

        if(!isServerRunning && isGroupOwner && isConnected){
            startMessageServer()
        }
        else if (!isServerRunning && isConnected){
            startMessageClient()
        }
    }

    private fun startMessageServer() {
        isServerRunning = true;
        Log.d("HOST-SERVER", "Starting...")

        messageServer = MessageServer(thisPhone) { message ->
            // Handle the received message
            Log.d("MESSAGE", "Received: $message")
            text = message;
            parseMessageFromClient(message)
        }

        messageServer.start()
    }


    private fun startMessageClient() {
        Handler().postDelayed({
            Log.d("CLIENT-SERVER", "Starting...")
            Log.d("SERVER-HOST", host)
            isServerRunning = true;

            messageClient = MessageClient(thisPhone, host) { message ->
                // Handle the received message
                Log.d("MESSAGE", "Received: $message")
                text = message;

                parseMessageFromServer(message)

            }

            messageClient.start()
        }, 1000)
    }

    private fun parseMessageFromServer(message: String){
        var type = message.split(":")[0]
        var info = message.split(":")[1]

        if(type.equals("PhoneInfo")){
            var locationX = info.split(",")[0]
            var locationY = info.split(",")[1]
            var nr = info.split(",")[2]

            thisPhone.locationX = locationX.toInt()
            thisPhone.locationY = locationY.toInt()
            thisPhone.nr = nr.toInt()

            phoneNr = nr.toInt()
            Log.d("MESSAGE", "Saved PhoneInfo")
        }
        else if(type.equals("ScreenInfo")){
            var vHeight = info.split(",")[0]
            var vWidth = info.split(",")[1]

            virtualScreen.vHeight = vHeight.toInt()
            virtualScreen.vWidth = vWidth.toInt()

            virtualHeight = vHeight.toInt()
            virtualWidth = vWidth.toInt()
            Log.d("MESSAGE", "Saved ScreenInfo")
        }
    }

    private fun parseMessageFromClient(message: String){
        var info = message.split(":")[1]
        var height = info.split(",")[0]
        var width = info.split(",")[1]
        var DPI = info.split(",")[2]
        var name = info.split(",")[3]
        var id = info.split(",")[4]

        var updatedPhone = virtualScreen.addPhone(PhoneScreen(height.toInt(), width.toInt(), DPI.toInt(), name, id))
        messageServer.sendClientInfo(updatedPhone)
        Thread.sleep(100)
        messageServer.sendScreenInfo(virtualScreen)
    }

}