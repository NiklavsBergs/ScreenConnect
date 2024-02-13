package com.example.screenconnect.network

import android.net.Uri
import android.util.Log
import com.example.screenconnect.models.PhoneScreen
import com.example.screenconnect.models.VirtualScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket

class MessageServer (
    private val thisPhone: PhoneScreen,
    private val messageReceivedCallback: (String) -> Unit
    ) : Thread(){

    var serverSocket:ServerSocket? = null
    var socket: Socket? = null
    var inputStream: InputStream? = null
    var outputStream: OutputStream? = null

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun run(){

            try{
                serverSocket = ServerSocket(8888)
                socket = serverSocket?.accept()
                inputStream = socket?.getInputStream()
                outputStream = socket?.getOutputStream()
                Log.d("SERVER-START", socket?.remoteSocketAddress.toString())
                sendPhoneInfo(thisPhone)
            }
            catch (e: IOException) {
                Log.d("SERVER-INIT", e.toString())
            }

            Log.d("SERVER-RECEIVE", "Running")
            while(socket!=null && !socket!!.isClosed){
                try{

                    val buffer: ByteArray = ByteArray(1024)
                    val bytes = inputStream?.read(buffer)

                    // Process the received message
                    if (bytes != null && bytes > 0) {
                        val message = String(buffer, 0,bytes)
                        messageReceivedCallback(message)
                        Log.d("SOCKET-CLOSED", socket?.isClosed.toString())
                    }
//                    if (bytes != null && bytes > 0) {
//                        val receivedData = processData(buffer, bytes)
//                        receivedData?.let {
//                            when (it) {
//                                is String -> {
//                                    // Received a string message
//                                    messageReceivedCallback(it)
//                                    Log.d("SERVER-RECEIVE-MSG", it)
//                                }
//                                is ByteArray -> {
//                                    // Received an image
//                                    // Handle the ByteArray accordingly
//                                    // You might want to save it to a file or display it
//                                    Log.d("SERVER-RECEIVE-IMG", "received")
//                                }
//                                else -> {
//                                    // Handle unknown data type
//                                    Log.d("SERVER-RECEIVE", "Unknown data type")
//                                }
//                            }
//                        }
//                    }

                } catch (e: IOException) {
                    Log.d("SERVER-RECEIVE", e.toString())
                }

            }
            Log.d("SERVER-RECEIVE-END", "ENDED")


    }

    fun send(message: String){
        Log.d("SERVER-SEND", "Starting...")
                try{
                    outputStream?.write(message.toByteArray())

                    Log.d("SERVER-SEND", message)
                }
                catch (e: IOException){
                    Log.d("SERVER-SEND", e.toString())
                }
    }

    fun sendPhoneInfo(phone: PhoneScreen){
        Log.d("SERVER-SEND", "Sending...")

        try{
            var phoneInfo = "PhoneInfo:" + phone.height + "," + phone.width + "," + phone.DPI + "," + phone.phoneName + "," + phone.id
            outputStream?.write(phoneInfo.toByteArray())

            Log.d("SERVER-SEND-INFO", phoneInfo)
        }
        catch (e: IOException){
            Log.d("SERVER-SEND-ERROR", e.toString())
        }
        Log.d("SERVER-SEND-SOCKET-CLOSED", socket?.isClosed.toString())
    }

    fun sendClientInfo(phone: PhoneScreen){
        Log.d("SERVER-SEND", "Sending...")

        try{
            var phoneInfo = "PhoneInfo:" + phone.locationX + "," + phone.locationY + "," + phone.nr
            outputStream?.write(phoneInfo.toByteArray())
            outputStream?.flush()

            Log.d("SERVER-SEND-INFO", phoneInfo)
        }
        catch (e: IOException){
            Log.d("SERVER-SEND-ERROR", e.toString())
        }
        Log.d("SERVER-SEND-SOCKET-CLOSED", socket?.isClosed.toString())
    }

    fun sendScreenInfo(screen: VirtualScreen){
        Log.d("SERVER-SEND", "Sending...")

        try{
            var phoneInfo = "ScreenInfo:" + screen.vHeight + "," + screen.vWidth
            outputStream?.write(phoneInfo.toByteArray())
            outputStream?.flush()

            Log.d("SERVER-SEND-INFO", phoneInfo)
        }
        catch (e: IOException){
            Log.d("SERVER-SEND-ERROR", e.toString())
        }
        Log.d("SERVER-SOCKET-CLOSED", socket?.isClosed.toString())
    }

    fun sendImage(){

    }

//    fun sendImage(contentUri: Uri) {
//        try {
////            val imageFile = File(imagePath)
////            val imageBytes = imageFile.readBytes()
//            val buf = ByteArray(1024)
//            var len: Int? = 0
//
//
//
////            val cr = context.contentResolver
////            val inputStream: InputStream? = cr.openInputStream(contentUri)
////            val fileSize = inputStream?.available() ?: 0
////            val sizeHeader = fileSize.toString().toByteArray()
////            outputStream?.write("1".toByteArray())
////            outputStream?.write(sizeHeader)
////
////            while (inputStream?.read(buf).also { len = it } != -1) {
////                len?.let { outputStream?.write(buf, 0, it) }
////            }
//
//
////            outputStream?.write("1".toByteArray()) // Use 1 to indicate an image
////            outputStream?.write(imageBytes)
////            outputStream?.write("Image end".toByteArray())
//
//            Log.d("SERVER-SEND-IMG", "Image sent")
//        } catch (e: IOException) {
//            Log.d("SERVER-SEND-ERROR", e.toString())
//        }
//    }
//
//    private fun processData(buffer: ByteArray, length: Int): Any? {
//        Log.d("DATA-PROCESSING", "Processing")
//        return if (buffer.isNotEmpty()) {
//            when (buffer[0]) {
//                '0'.toByte() -> String(buffer, 1, length - 1) // String data
//                '1'.toByte() -> buffer.copyOfRange(1, length) // Image data
//                else -> null // Unknown type
//            }
//        } else {
//            null
//        }
//    }

    fun close() {
        try {
            socket?.close()
            serverSocket?.close()
        } catch (e: IOException) {
            Log.e("SERVER-STOP", e.toString())
        }
    }
}
