package com.example.screenconnect.network

import android.util.Log
import com.example.screenconnect.models.PhoneScreen
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

class MessageClient (
    private val thisPhone: PhoneScreen,
    private val host: String,
    private val messageReceivedCallback: (String) -> Unit
) : Thread(){
    var socket: Socket = Socket()
    var inputStream: InputStream? = null
    var outputStream: OutputStream? = null

    override fun run(){
        Log.d("HOST",host)

        try {
            socket?.connect(InetSocketAddress(host, 8888), 500)
            inputStream = socket?.getInputStream()
            outputStream = socket?.getOutputStream()

            Log.d("CLIENT-START",host)

            sendPhoneInfo(thisPhone)


        } catch (e: IOException) {
            Log.d("CLIENT-INIT", e.toString())
        }


        Log.d("CLIENT-RECEIVE", "Running")
        while(socket!=null  && !socket!!.isClosed){
            try{
                val buffer: ByteArray = ByteArray(1024)
                val bytes = inputStream?.read(buffer)

                // Process the received message
                if (bytes != null && bytes > 0) {
                    val message = String(buffer, 0,bytes)
                    if(message.compareTo("Image") == 0){
                        receiveImage(inputStream!!)
                    }
                    messageReceivedCallback(message)
                    Log.d("SOCKET-CLOSED", socket?.isClosed.toString())

//                   else {
//                       messageReceivedCallback("")
//                   }
//                    val receivedData = processData(buffer, bytes)
//                    receivedData?.let {
//                        when (it) {
//                            is String -> {
//                                // Received a string message
//                                messageReceivedCallback(it, null)
//                                Log.d("CLIENT-RECEIVE-MSG", it)
//                            }
//                            is ByteArray -> {
//                                // Received an image
//                                // Handle the ByteArray accordingly
//                                // You might want to save it to a file or display it
//                                Log.d("CLIENT-RECEIVE-IMG", "received")
//                                handleImageReceive(inputStream!!, it)
//                                Log.d("CLIENT-RECEIVE-IMG", "saved")
//
//                                val imagePath = saveImage(it)
//                                messageReceivedCallback(" ", imagePath)
//                            }
//                            else -> {
//                                // Handle unknown data type
//                                Log.d("CLIENT-RECEIVE", "Unknown data type")
//                            }
//                        }
//                    }
                }
//                   else {
//                       messageReceivedCallback("")
//                   }
            } catch (e: IOException) {
                Log.d("CLIENT-RECEIVE-ERROR", e.toString())
                socket.close()
            }

            }
            Log.d("CLIENT-RECEIVE-END", "ENDED")

    }

    fun send(message: String){
        Log.d("CLIENT-SEND", "Sending...")


            try{
                outputStream?.write(message.toByteArray())

                Log.d("CLIENT-SEND", message)
            }
            catch (e: IOException){
                Log.d("CLIENT-SEND-ERROR", e.toString())
            }
            Log.d("CLIENT-SEND-SOCKET", socket?.isClosed.toString())
    }

    fun sendPhoneInfo(phone: PhoneScreen){
        Log.d("CLIENT-SEND", "Sending...")


        try{
            var phoneInfo = "PhoneInfo:" + phone.height + "," + phone.width + "," + phone.DPI + "," + phone.phoneName + "," + phone.id
            outputStream?.write(phoneInfo.toByteArray())

            Log.d("CLIENT-SEND-INFO", phoneInfo)
        }
        catch (e: IOException){
            Log.d("CLIENT-SEND-ERROR", e.toString())
        }
        Log.d("CLIENT-SEND-SOCKET-CLOSED", socket?.isClosed.toString())
    }

    fun sendImage(imagePath: String) {
        try {
            val imageFile = File(imagePath)
            val imageBytes = imageFile.readBytes()

            outputStream?.write("1".toByteArray()) // Use 1 to indicate an image
            outputStream?.write(imageBytes)
            outputStream?.flush()
        } catch (e: IOException) {
            Log.d("CLIENT-SEND-ERROR", e.toString())
        }
    }

    private fun processData(buffer: ByteArray, length: Int): Any? {
        return if (buffer.isNotEmpty()) {
            when (buffer[0]) {
                '0'.toByte() -> String(buffer, 1, length - 1) // String data
                '1'.toByte() -> buffer.copyOfRange(1, length) // Image data
                else -> null // Unknown type
            }
        } else {
            null
        }
    }

    private fun handleImageReceive(inputStream: InputStream, initialBytes: ByteArray) {
        val imageBuffer = mutableListOf<Byte>().apply {
            addAll(initialBytes.asList())
        }

        val sizeHeaderBuffer = ByteArray(1024)
        val sizeHeaderSize = inputStream.read(sizeHeaderBuffer)
        val sizeHeaderString = sizeHeaderBuffer.decodeToString(0, sizeHeaderSize!!)
        val imageSize = sizeHeaderString.toInt()

        var totalBytesRead = initialBytes.size

        var bytesRead: Int = 0
        val buffer = ByteArray(1024)

        try {
            while (totalBytesRead < imageSize) {
                val bytesRead = inputStream.read(buffer)
                if (bytesRead == -1) {
                    // Handle unexpected end of stream
                    break
                }
                imageBuffer.addAll(buffer.copyOfRange(0, bytesRead).asList())
                totalBytesRead += bytesRead
            }

            val imageBytes = imageBuffer.toByteArray()
            val imagePath = saveImage(imageBytes)
            //messageReceivedCallback(" ", imagePath)
        } catch (e: IOException) {
            Log.e("CLIENT-RECEIVE-IMG", "Error receiving image: ${e.message}")
        }
    }

    fun receiveImage(inputStream: InputStream){
        val socketDIS = DataInputStream(BufferedInputStream(inputStream))

    }


    private fun saveImage(imageBytes: ByteArray): String {
        Log.d("CLIENT-SAVE", "Image bytes length: ${imageBytes.size}")
        val imageFileName = "received_image_${System.currentTimeMillis()}.jpg"
//        val f = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath +
//                "/${context.packageName}/wifip2pshared-${System.currentTimeMillis()}.jpg")
//
//        Log.d("CLIENT-SAVE", "Dir create")
//        val dirs = File(f.parent)
//        dirs.takeIf { !it.exists() }?.apply {
//            mkdirs()
//        }

        Log.d("CLIENT-SAVE", "File create")
        //f.createNewFile()





//        Log.d("PATH", storageDir.path)
//        if (!storageDir.exists()) {
//            if (storageDir.mkdirs()) {
//                Log.d("CLIENT-SAVE", "Storage directory created successfully.")
//            } else {
//                Log.e("CLIENT-SAVE", "Failed to create storage directory.")
//            }
//        }
//
//        if (!storageDir.isDirectory) {
//            // The path is not a directory, handle this case accordingly
//            Log.e("CLIENT-SAVE", "Error: Storage directory is not a directory.")
//            return ""
//        }

        Log.d("CLIENT-SAVE", "Bef write")
        //val imageFile = File(storageDir, imageFileName)
        //f.writeBytes(imageBytes)

        //return f.absolutePath
        return ""
    }

    fun close() {
        try {
            socket?.close()
        } catch (e: IOException) {
            Log.e("SERVER-STOP", e.toString())
        }
    }
}

