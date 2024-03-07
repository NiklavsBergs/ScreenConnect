package com.example.screenconnect.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.net.wifi.p2p.WifiP2pDeviceList
import android.os.Build
import android.os.Handler
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.graphics.scale
import androidx.lifecycle.ViewModel
import com.example.screenconnect.enums.SwipeType
import com.example.screenconnect.models.Phone
import com.example.screenconnect.models.Position
import com.example.screenconnect.models.Swipe
import com.example.screenconnect.models.VirtualScreen
import com.example.screenconnect.network.MessageClient
import com.example.screenconnect.network.MessageServer
import com.example.screenconnect.util.getPhoneInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.roundToInt

class SharedViewModel() : ViewModel() {

    var text by mutableStateOf("Hello")
    var infoText by mutableStateOf("Not connected");
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

    lateinit var thisPhone: Phone

    var virtualScreen: VirtualScreen = VirtualScreen()

    var virtualHeight by mutableStateOf(0)
    var virtualWidth by mutableStateOf(0)

    var phoneNr by mutableStateOf(0)

    var showImage by mutableStateOf(false)
    var sharedImage by mutableStateOf<Bitmap>(Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888))

    var activeFoto: File? = null


    fun sendSwipe(swipe: Swipe){
        Log.d("SEND-SWIPE", "Called")
        if(!isServerRunning){
            startServer()
        }
        else{
            if (isGroupOwner) {
                serverScope.launch {
                    var phoneAdded = virtualScreen.addSwipe(swipe) {
                            phone ->
                        thisPhone = phone
                        activeFoto?.let { processReceivedImage(it) }
                    }

                    if(phoneAdded){
                        messageServer.sendClientInfo(virtualScreen.phones[0])
                        Thread.sleep(25)
                        messageServer.sendScreenInfo(virtualScreen)
                    }

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
                    activeFoto = file
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

    fun sendInfo(phone: Phone){
        Log.d("SEND-TEXT", "Call")
        if(!isServerRunning){
            startServer()
        }
        else{
            if (isGroupOwner) {
                serverScope.launch {
                    //messageServer.sendPhoneInfo(phone)
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

        if(!isServerRunning && isGroupOwner && isConnected){
            startMessageServer()

            //virtualScreen.addHost(thisPhone)
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

                    activeFoto = file

                    processReceivedImage(file)

                })


            messageClient.start()
        }, 1000)
    }

    private fun parseMessageFromServer(message: String){
        var type = message.split("*")[0]
        var info = message.split("*")[1]

        if(type.equals("PhoneInfo")){

            thisPhone = Json.decodeFromString<Phone>(info)

            Log.d("MESSAGE", "Saved PhoneInfo")
        }
        else if(type.equals("ScreenInfo")){

            virtualScreen = Json.decodeFromString<VirtualScreen>(info)

            activeFoto?.let { processReceivedImage(it) }

            Log.d("MESSAGE", "Saved ScreenInfo")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun parseMessageFromClient(message: String){

        var swipe = Json.decodeFromString<Swipe>(message)

        Log.d("SWIPE-RECEIVED", swipe.toString())
        var phoneAdded = false

        if(swipe.type == SwipeType.DISCONNECT){
            var removedPhone = swipe.phone
            virtualScreen.removePhone(removedPhone)

            removedPhone.position = Position(0,0)
            removedPhone.rotation = 0
            messageServer.sendClientInfo(removedPhone)

            var tempScreen = VirtualScreen()
            tempScreen.vHeight = removedPhone.height
            tempScreen.vWidth = removedPhone.width

            messageServer.sendScreenInfo(tempScreen, removedPhone)
        }

        phoneAdded = virtualScreen.addSwipe(swipe) {
                phone ->
            thisPhone = phone
            activeFoto?.let { processReceivedImage(it) }
        }

        if(phoneAdded){
            Thread.sleep(25)
            messageServer.updateClientInfo(virtualScreen)
            //Thread.sleep(25)
            //messageServer.sendScreenInfo(virtualScreen)
        }

    }

    fun processReceivedImage(file: File) {
        // Decode the file into a Bitmap
        val options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.ARGB_8888 // Set bitmap config

        Log.d("DPI virtual", virtualScreen.DPI.toString())
        Log.d("DPI phone", thisPhone.DPI.toString())

        var bitmap = upscaleAndCropBitmap(file)

        Log.d("Bitmap height cropped", bitmap.height.toString())
        Log.d("Bitmap width cropped", bitmap.width.toString())

        Log.d("Phone height", thisPhone.height.toString())
        Log.d("Phone width", thisPhone.width.toString())

        var position = thisPhone.position

        Log.d("Phone X", position.x.toString())
        Log.d("Phone Y", position.y.toString())

        if(thisPhone.rotation != 0){

            var matrix = Matrix()

            matrix.postRotate(-1 * thisPhone.rotation.toFloat())

            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

            Log.d("Bitmap height rotated", bitmap.height.toString())
            Log.d("Bitmap width rotated", bitmap.width.toString())

            position = thisPhone.position.rotateWithScreen(Position(virtualScreen.vWidth, virtualScreen.vHeight), thisPhone)
            Log.d("Phone X rotated", position.x.toString())
            Log.d("Phone Y rotated", position.y.toString())
        }

        Log.d("Phone Rotation", thisPhone.rotation.toString())


        if (bitmap != null) {
            // Crop the Bitmap to absolute phone size and location
            var croppedBitmap = Bitmap.createBitmap(
                bitmap,
                position.x,
                position.y,
                thisPhone.width,
                thisPhone.height
            )

            // Scale to actual phone size
            croppedBitmap = croppedBitmap.scale(thisPhone.widthReal, thisPhone.heightReal)

            sharedImage = croppedBitmap
            showImage = true
        } else {
            // Failed to decode file into Bitmap
        }
    }

    fun upscaleAndCropBitmap(file: File): Bitmap {

        //Scales and crops image to virtual screen size

        var bitmap = BitmapFactory.decodeFile(file.path)

        val ratio = thisPhone.DPI.toDouble()/virtualScreen.DPI.toDouble()

        Log.d("Bitmap height",  bitmap.height.toString())
        Log.d("Bitmap width", bitmap.width.toString())

        val widthRatio = virtualScreen.vWidth.toFloat() / bitmap.width
        val heightRatio = virtualScreen.vHeight.toFloat() / bitmap.height
        val scaleFactor = if (widthRatio > heightRatio) widthRatio else heightRatio

        Log.d("Scale", scaleFactor.toString())

//        val scaledWidth = (bitmap.width * scaleFactor * ratio).toInt() + 2
//        val scaledHeight = (bitmap.height * scaleFactor * ratio).toInt() + 2

        val scaledWidth = (bitmap.width * scaleFactor).toInt() + 2
        val scaledHeight = (bitmap.height * scaleFactor).toInt() + 2

        bitmap = bitmap.scale(scaledWidth, scaledHeight)

        Log.d("Bitmap height scaled",  bitmap.height.toString())
        Log.d("Bitmap width scaled", bitmap.width.toString())

        Log.d("Screen height",  virtualScreen.vHeight.toString())
        Log.d("Screen width", virtualScreen.vWidth.toString())

        val heightDiff = ((bitmap.height - virtualScreen.vHeight) / 2F).roundToInt()
        val widthDiff = ((bitmap.width - virtualScreen.vWidth) / 2F).roundToInt()

        Log.d("height diff",  heightDiff.toString())
        Log.d("width diff", widthDiff.toString())

        // Crop the bitmap
        return Bitmap.createBitmap(bitmap, widthDiff, heightDiff, virtualScreen.vWidth, virtualScreen.vHeight)
    }

}