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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import kotlin.math.max
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

    var showImage by mutableStateOf(false)
    var sharedImage by mutableStateOf<Bitmap>(Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888))

    private var activePhoto: File? = null
    var logoBitmap: Bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)

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
                    }
                    else if (swipe.type == SwipeType.CONNECT){
                        var phoneAdded = virtualScreen.addSwipe(swipe) {
                                hostUpdated ->
                            thisPhone = hostUpdated.copy()
                            processReceivedImage(activePhoto)
                        }

                        if(phoneAdded){
                            messageServer.updateClientInfo(virtualScreen)
                        }
                    }
                }
            } else {
                clientScope.launch {
                    messageClient.sendSwipe(swipe)
                }
            }
        }
    }

    fun sendPhoneInfo() {
        if (isGroupOwner) {
            val changed = virtualScreen.updatePhone(thisPhone){ hostUpdated ->
                thisPhone = hostUpdated.copy()
                processReceivedImage(activePhoto)
            }

            if(changed){
                serverScope.launch {
                    messageServer.updateClientInfo(virtualScreen)
                }
            }
        } else {
            clientScope.launch {
                messageClient.sendPhoneInfo(thisPhone)
            }
        }
    }

    fun sendImage(file: File){
        if(!isServerRunning){
            startServer()
        }

        if (isGroupOwner) {
            serverScope.launch {
                messageServer.sendImage(file)
                activePhoto = file
            }
        } else {
            clientScope.launch {
                messageClient.sendImage(file)
                activePhoto = file
            }
        }
    }

    // Start socket communication
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

        messageServer = MessageServer(thisPhone,
            { message, type ->
            // Handle the received message
            parseMessageFromClient(message, type)
            },
            { file ->
                // Handle the received image
                imageUri = Uri.fromFile(file)

                activePhoto = file

                processReceivedImage(file)
            })

        messageServer.start()
    }


    private fun startMessageClient() {
        //Starting Client with a delay to allow server socket to start
        Handler().postDelayed({

            messageClient = MessageClient(host,
                { type, message ->
                    // Handle the received message
                    parseMessageFromServer(type, message)
                },
                { file ->
                    // Handle the received image
                    imageUri = Uri.fromFile(file)

                    activePhoto = file

                    processReceivedImage(file)
                })

            messageClient.start()
            isServerRunning = true;
        }, 500)
    }

    private fun parseMessageFromServer(type: MessageType, message: String){
        // Update state received from server
        when(type){
            MessageType.PHONE -> {
                thisPhone = Json.decodeFromString<Phone>(message)
            }
            MessageType.SCREEN -> {
                virtualScreen = Json.decodeFromString<VirtualScreen>(message)

                processReceivedImage(activePhoto)
            }
            else -> {}
        }
    }

    private fun parseMessageFromClient(message: String, type: MessageType){

        when(type){
            MessageType.SWIPE -> {
                var swipe = Json.decodeFromString<Swipe>(message)
                if(swipe.type == SwipeType.DISCONNECT){
                    var removedPhone = swipe.phone
                    virtualScreen.removePhone(removedPhone)
                }
                else if(swipe.type == SwipeType.CONNECT){
                    // If the new device gets added returns True
                    // hostUpdated returns updated host phone object
                    var  phoneAdded = virtualScreen.addSwipe(swipe) { hostUpdated ->
                        thisPhone = hostUpdated.copy()
                        processReceivedImage(activePhoto)
                    }

                    if(phoneAdded){
                        messageServer.updateClientInfo(virtualScreen)
                    }
                }
            }
            MessageType.PHONE -> {
                var phone = Json.decodeFromString<Phone>(message)
                val changed = virtualScreen.updatePhone(phone) { hostUpdated ->
                    thisPhone = hostUpdated.copy()
                    processReceivedImage(activePhoto)
                }

                if(changed){
                    messageServer.updateClientInfo(virtualScreen)
                }
            }

            else -> {}
        }

    }

    fun processReceivedImage(file: File?) {
        // Decodes the file into a Bitmap and crops it

        try {
            // Makes image the size of the virtual screen
            var bitmap = upscaleAndCropBitmap(file)

            var position = thisPhone.position

            // Adjust image for devices rotation, adjust position with rotation
            if(thisPhone.rotation != 0){

                var matrix = Matrix()
                matrix.postRotate(-1 * thisPhone.rotation.toFloat())
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

                position = thisPhone.position.rotateWithScreen(Position(virtualScreen.width, virtualScreen.height), thisPhone)
            }


            // Crop the Bitmap to absolute device size and location
            var croppedBitmap = Bitmap.createBitmap(
                bitmap,
                position.x,
                position.y,
                thisPhone.width,
                thisPhone.height,
            )

            // Scale to actual device size in pixels and show image
            croppedBitmap = croppedBitmap.scale(thisPhone.widthReal, thisPhone.heightReal)

            sharedImage = croppedBitmap
            showImage = true
        }
        catch (e: IOException){
            e.printStackTrace()
        }
    }

    private fun upscaleAndCropBitmap(file: File?): Bitmap {

        // Scales and crops image to virtual screen size
        lateinit var bitmap: Bitmap

        if(file == null) {
            // If there is no image selected, display logo
            val widthRatio = virtualScreen.width.toFloat() /logoBitmap.width.toFloat()
            val heightRatio = virtualScreen.height.toFloat() /logoBitmap.width.toFloat()

            val width = (logoBitmap.width * max(widthRatio, heightRatio)).roundToInt()
            val height = (logoBitmap.height * max(widthRatio, heightRatio)).roundToInt()

            bitmap = Bitmap.createScaledBitmap(logoBitmap, width, height, true)
        }
        else {
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

            // +2 for rounding errors
            val scaledWidth = (options.outWidth * scaleFactor).toInt() + 2

            options.inJustDecodeBounds = false

            options.inDensity = options.outWidth

            options.inTargetDensity = scaledWidth

            bitmap = BitmapFactory.decodeFile(file.path, options)
        }

        val heightDiff = ((bitmap.height - virtualScreen.height) / 2F).roundToInt()
        val widthDiff = ((bitmap.width - virtualScreen.width) / 2F).roundToInt()

        // Crop the bitmap
        return Bitmap.createBitmap(bitmap, widthDiff, heightDiff, virtualScreen.width, virtualScreen.height)
    }

}