package com.example.screenconnect.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.net.wifi.p2p.WifiP2pDeviceList
import android.os.Handler
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.core.graphics.scale
import androidx.lifecycle.ViewModel
import com.example.screenconnect.models.PhoneScreen
import com.example.screenconnect.models.Swipe
import com.example.screenconnect.models.VirtualScreen
import com.example.screenconnect.network.MessageClient
import com.example.screenconnect.network.MessageServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.round

class SharedViewModel() : ViewModel() {

    var text by mutableStateOf("Hello")
    var infoText by mutableStateOf("Info");
    var imageUri by mutableStateOf<Uri?>(null)

    var host by mutableStateOf("")

    var peerList by mutableStateOf<WifiP2pDeviceList?>(null)

    var isWifiEnabled by mutableStateOf(false)
    var isLocationEnabled by mutableStateOf(false)
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

    var showImage by mutableStateOf(false)
    var sharedImage by mutableStateOf<Bitmap>(Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888))


    fun sendSwipe(swipe: Swipe){
        Log.d("SEND-SWIPE", "Called")
        if(!isServerRunning){
            startServer()
        }
        else{
            if (isGroupOwner) {
                serverScope.launch {
                    //messageServer.send(message)
                    Log.d("MESSAGE-SERVER", "Sending...")
                }
            } else {
                clientScope.launch {
                    messageClient.sendSwipe(swipe)
                    Log.d("MESSAGE-CLIENT", "Sent")
                }
            }
        }
    }

    fun sendPhoneInfo(){
        serverScope.launch {
            messageClient.sendPhoneInfo(thisPhone)
            Log.d("MESSAGE-SERVER", "Info Sent")
        }
    }

    fun sendImage(file: File){
        Log.d("SEND-IMAGE", "Call")
        if(!isServerRunning){
            startServer()
        }
        else{
            if (isGroupOwner) {
                serverScope.launch {
                    messageServer.sendImage(file)
                    Log.d("MESSAGE-SERVER", "Image Sent")
                }
            } else {
                clientScope.launch {
                    messageClient.sendImage(file)
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

            messageClient = MessageClient(thisPhone, host, this,
                { message ->
                // Handle the received message
                Log.d("MESSAGE", "Received: $message")
                text = message;

                parseMessageFromServer(message)

                },
                { file ->
                    // Handle the received image
                    Log.d("FILE", file.path)

                    imageUri = Uri.fromFile(file)

                    val cropRect = Rect(thisPhone.locationX, thisPhone.locationY, thisPhone.width, thisPhone.height)
                    processReceivedImage(file)

                })


            messageClient.start()
        }, 1000)
    }

    private fun parseMessageFromServer(message: String){
        var type = message.split("*")[0]
        var info = message.split("*")[1]

        if(type.equals("PhoneInfo")){

            thisPhone = Json.decodeFromString<PhoneScreen>(info)

            Log.d("MESSAGE", "Saved PhoneInfo")
        }
        else if(type.equals("ScreenInfo")){

            virtualScreen = Json.decodeFromString<VirtualScreen>(info)

            Log.d("MESSAGE", "Saved ScreenInfo")
        }
    }

    private fun parseMessageFromClient(message: String){

        //var swipe = Json.decodeFromString<Swipe>(message)

        Log.d("SWIPE-RECEIVED", message)
        var updatedPhone = virtualScreen.addPhone(Json.decodeFromString<PhoneScreen>(message))

        messageServer.sendClientInfo(updatedPhone)
        Thread.sleep(100)
        messageServer.sendScreenInfo(virtualScreen)
    }

    fun processReceivedImage(file: File) {
        // Decode the file into a Bitmap
        val options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.ARGB_8888 // Set bitmap config

        Log.d("DPI virtual", virtualScreen.DPI.toString())
        Log.d("DPI phone", thisPhone.DPI.toString())

        val bitmap = upscaleAndCropBitmap(file)
        Log.d("Bitmap height", bitmap.height.toString())
        Log.d("Bitmap width", bitmap.width.toString())

        Log.d("Phone height", thisPhone.height.toString())
        Log.d("Phone width", thisPhone.width.toString())

        if (bitmap != null) {
            // Crop the Bitmap to phone size and location
            val croppedBitmap = Bitmap.createBitmap(
                bitmap,
                thisPhone.locationX,
                thisPhone.locationY,
                thisPhone.width,
                thisPhone.height
            )
            sharedImage = croppedBitmap
            showImage = true
        } else {
            // Failed to decode file into Bitmap
        }
    }

    fun upscaleAndCropBitmap(file: File): Bitmap {

        var bitmap = BitmapFactory.decodeFile(file.path)

        val ratio = thisPhone.DPI.toDouble()/virtualScreen.DPI.toDouble()

        val widthRatio = virtualScreen.vWidth.toFloat() / bitmap.width
        val heightRatio = virtualScreen.vHeight.toFloat() / bitmap.height
        val scaleFactor = if (widthRatio > heightRatio) widthRatio else heightRatio

        val scaledWidth = (bitmap.width * scaleFactor * ratio).toInt()
        val scaledHeight = (bitmap.height * scaleFactor * ratio).toInt()

        bitmap = bitmap.scale(scaledWidth, scaledHeight)

        Log.d("height",  bitmap.height.toString())
        Log.d("width", bitmap.width.toString())

        val widthDiff = (bitmap.width - virtualScreen.vWidth) / 2
        val heightDiff = (bitmap.height - virtualScreen.vHeight) / 2

        Log.d("height",  widthDiff.toString())
        Log.d("width", heightDiff.toString())

        // Crop the bitmap
        return Bitmap.createBitmap(bitmap, widthDiff, heightDiff, virtualScreen.vWidth, virtualScreen.vHeight)
    }

}