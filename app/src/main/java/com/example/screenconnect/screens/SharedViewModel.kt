package com.example.screenconnect.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.net.wifi.p2p.WifiP2pDeviceList
import android.os.Handler
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.graphics.scale
import androidx.lifecycle.ViewModel
import com.example.screenconnect.enums.MessageType
import com.example.screenconnect.enums.SwipeType
import com.example.screenconnect.models.Phone
import com.example.screenconnect.models.Position
import com.example.screenconnect.models.Swipe
import com.example.screenconnect.models.VirtualScreen
import com.example.screenconnect.network.MessageClient
import com.example.screenconnect.network.MessageServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import kotlin.math.roundToInt


class SharedViewModel() : ViewModel() {

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

    var showImage by mutableStateOf(false)
    var sharedImage by mutableStateOf<Bitmap>(Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888))

    private var activePhoto: File? = null


    fun sendSwipe(swipe: Swipe){
        if(isConnected){
            if(!isServerRunning){
                startServer()
            }

            if (isGroupOwner) {
                serverScope.launch {
                    if(swipe.type == SwipeType.DISCONNECT){
                        virtualScreen.removePhone(thisPhone)
                        thisPhone.position = Position(0,0)
                        virtualScreen.height = thisPhone.height
                        virtualScreen.width = thisPhone.width

                        activePhoto?.let { processReceivedImage(it) }
                    }
                    else if (swipe.type == SwipeType.CONNECT){
                        var phoneAdded = virtualScreen.addSwipe(swipe) {
                                hostUpdated ->
                            thisPhone = hostUpdated
                            activePhoto?.let { processReceivedImage(it) }
                        }

                        if(phoneAdded){
                            messageServer.updateClientInfo(virtualScreen)
                        }

                        Log.d("SERVER", "Swipe added")
                    }
                }
            } else {
                clientScope.launch {
                    messageClient.sendSwipe(swipe)
                    Log.d("CLIENT", "Swipe sent")
                }
            }
        }
    }

    fun sendImage(file: File){
        Log.d("SEND-IMAGE", "Call")
        if(!isServerRunning){
            startServer()
        }

        if (isGroupOwner) {
            serverScope.launch {
                messageServer.sendImage(file)
                activePhoto = file
                Log.d("MESSAGE-SERVER", "Image Sent")
            }
        } else {
            clientScope.launch {
                messageClient.sendImage(file)
                activePhoto = file
                Log.d("MESSAGE-CLIENT", "Image Sent")
            }
        }
    }

    fun startServer(){

        if(!isServerRunning && isConnected && isGroupOwner){
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

            parseMessageFromClient(message)
        }

        messageServer.start()
    }


    private fun startMessageClient() {
        //Starting Client with a delay to allow server socket to start
        Handler().postDelayed({
            Log.d("CLIENT", "Starting...")
            Log.d("SERVER-HOST", host)

            messageClient = MessageClient(host,
                { type, message ->
                    // Handle the received message
                    Log.d("MESSAGE", "Received: $message")

                    parseMessageFromServer(type, message)
                },
                { file ->
                    // Handle the received image
                    Log.d("FILE", file.path)

                    imageUri = Uri.fromFile(file)

                    activePhoto = file

                    processReceivedImage(file)
                })

            messageClient.start()
            isServerRunning = true;
        }, 500)
    }

    private fun parseMessageFromServer(type: MessageType, message: String){


        when(type){
            MessageType.PHONE -> {
                thisPhone = Json.decodeFromString<Phone>(message)
                Log.d("MESSAGE", "Saved PhoneInfo")
            }
            MessageType.SCREEN -> {
                virtualScreen = Json.decodeFromString<VirtualScreen>(message)

                activePhoto?.let { processReceivedImage(it) }

                Log.d("MESSAGE", "Saved ScreenInfo")
            }
            else -> {}
        }
    }

    private fun parseMessageFromClient(message: String){

        var swipe = Json.decodeFromString<Swipe>(message)

        Log.d("SWIPE-RECEIVED", message)

        var phoneAdded: Boolean

        if(swipe.type == SwipeType.DISCONNECT){
            var removedPhone = swipe.phone
            virtualScreen.removePhone(removedPhone)

            removedPhone.position = Position(0,0)
            removedPhone.rotation = 0
            messageServer.sendClientInfo(removedPhone)

            var tempScreen = VirtualScreen()
            tempScreen.height = removedPhone.height
            tempScreen.width = removedPhone.width

            messageServer.sendScreenInfo(tempScreen, removedPhone)
        }
        else if(swipe.type == SwipeType.CONNECT){
            phoneAdded = virtualScreen.addSwipe(swipe) { hostUpdated ->
                thisPhone = hostUpdated
                activePhoto?.let { processReceivedImage(it) }
            }
            if(phoneAdded){
                messageServer.updateClientInfo(virtualScreen)
            }
        }
    }

    fun processReceivedImage(file: File) {
        // Decodes the file into a Bitmap and crops it

        try {
            var bitmap = upscaleAndCropBitmap(file)

            Log.d("Phone height", thisPhone.height.toString())
            Log.d("Phone width", thisPhone.width.toString())

            var position = thisPhone.position

            if(thisPhone.rotation != 0){

                var matrix = Matrix()

                matrix.postRotate(-1 * thisPhone.rotation.toFloat())

                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

                Log.d("Bitmap height rotated", bitmap.height.toString())
                Log.d("Bitmap width rotated", bitmap.width.toString())

                position = thisPhone.position.rotateWithScreen(Position(virtualScreen.width, virtualScreen.height), thisPhone)
                Log.d("Phone X rotated", position.x.toString())
                Log.d("Phone Y rotated", position.y.toString())
            }


            // Crop the Bitmap to absolute phone size and location
            var croppedBitmap = Bitmap.createBitmap(
                bitmap,
                position.x,
                position.y,
                thisPhone.width,
                thisPhone.height,
            )

            // Scale to actual phone size
            croppedBitmap = croppedBitmap.scale(thisPhone.widthReal, thisPhone.heightReal)

            sharedImage = croppedBitmap
            showImage = true
        }
        catch (e: IOException){
            Log.d("IMAGE-SHOW", "ERROR")
            e.printStackTrace()
        }
    }

    private fun upscaleAndCropBitmap(file: File): Bitmap {

        // Scales and crops image to virtual screen size

        // Get image size without decoding the whole image
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true

        val fis = FileInputStream(file)
        BitmapFactory.decodeStream(fis, null, options)
        fis.close()

        // Get scale factor needed to scale image to screen size
        val widthRatio = virtualScreen.width.toFloat() / options.outWidth
        val heightRatio = virtualScreen.height.toFloat() / options.outHeight
        val scaleFactor = if (widthRatio > heightRatio) widthRatio else heightRatio

        Log.d("Scale", scaleFactor.toString())

        // +2 for rounding errors
        val scaledWidth = (options.outWidth * scaleFactor).toInt() + 2
        //val scaledHeight = (options.outHeight * scaleFactor).toInt() + 2

        options.inJustDecodeBounds = false

        options.inDensity = options.outWidth

        options.inTargetDensity = scaledWidth

        var bitmap = BitmapFactory.decodeFile(file.path, options)

        Log.d("Bitmap height scaled",  bitmap.height.toString())
        Log.d("Bitmap width scaled", bitmap.width.toString())

        Log.d("Screen height",  virtualScreen.height.toString())
        Log.d("Screen width", virtualScreen.width.toString())

        val heightDiff = ((bitmap.height - virtualScreen.height) / 2F).roundToInt()
        val widthDiff = ((bitmap.width - virtualScreen.width) / 2F).roundToInt()

        Log.d("height diff",  heightDiff.toString())
        Log.d("width diff", widthDiff.toString())

        // Crop the bitmap
        return Bitmap.createBitmap(bitmap, widthDiff, heightDiff, virtualScreen.width, virtualScreen.height)
    }

}