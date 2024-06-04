package com.example.screenconnect.network

import android.os.Environment
import android.util.Log
import com.example.screenconnect.models.Phone
import com.example.screenconnect.models.Swipe
import com.example.screenconnect.enums.MessageType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.Integer.min
import java.net.InetSocketAddress
import java.net.Socket

// Server/Client structure inspired by https://github.com/stlong0521/wifi-direct-group
class MessageClient (
    private val host: String,
    private val messageReceivedCallback: (MessageType, String) -> Unit,
    private val imageReceivedCallback: (File) -> Unit
) : Thread(){

    private var socket: Socket = Socket()
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private var socketDOS: DataOutputStream? = null
    private var socketDIS: DataInputStream? = null

    var isConnected = false

    private val MAX_TRIES = 5
    private var tries = 0

    override fun run(){
        // Try to connect to server, abort if fail 5 times
        while(!isConnected && tries < MAX_TRIES){
            try {
                socket.connect(InetSocketAddress(host, 8888), 5000)

                inputStream = socket.getInputStream()
                outputStream = socket.getOutputStream()

                socketDOS = DataOutputStream(BufferedOutputStream(outputStream))
                socketDIS = DataInputStream(BufferedInputStream(inputStream))

                isConnected = true
            } catch (e: IOException) {
                e.printStackTrace()
                tries++
            }
        }
        readMessages()
    }

    // If connected successfully, read messages from server while connection active
    private fun readMessages(){
        val tempSocketDIS = socketDIS ?: return
        while(!socket.isClosed){
            try{

                val messageTypeOrdinal = tempSocketDIS.readInt()
                val messageType = MessageType.values()[messageTypeOrdinal]

                if (messageType == MessageType.IMAGE){
                    val savedFile = receiveImage()
                    imageReceivedCallback(savedFile)
                }
                else {
                    val message = tempSocketDIS.readUTF()
                    messageReceivedCallback(messageType, message)
                }

            } catch (e: IOException) {
                e.printStackTrace()
                socket.close()
            }

        }
    }

    fun sendPhoneInfo(phone: Phone){
        val tempSocketDOS = socketDOS ?: return
        try{
            tempSocketDOS.writeInt(MessageType.PHONE.ordinal)

            var phoneInfo = Json.encodeToString(phone)

            tempSocketDOS.writeUTF(phoneInfo)
            tempSocketDOS.flush()
        }
        catch (e: IOException){
            e.printStackTrace()
        }
    }

    fun sendSwipe(swipe: Swipe){
        val tempSocketDOS = socketDOS ?: return
        try{
            tempSocketDOS.writeInt(MessageType.SWIPE.ordinal)

            var swipe = Json.encodeToString(swipe)
            tempSocketDOS.writeUTF(swipe)
            tempSocketDOS.flush()
        }
        catch (e: IOException){
            e.printStackTrace()
        }
    }

    fun sendImage(file: File){
        val tempSocketDOS = socketDOS ?: return

        tempSocketDOS.writeInt(MessageType.IMAGE.ordinal)
        tempSocketDOS.writeUTF(file.name)
        tempSocketDOS.writeLong(file.length())

        val fileIS = FileInputStream(file)
        val bufferArray = ByteArray(2_000_000)
        var lengthRead: Int

        while (fileIS.read(bufferArray).also { lengthRead = it } > 0) {
            tempSocketDOS.write(bufferArray, 0, lengthRead)
        }
        fileIS.close()
    }

    private fun receiveImage(): File{

        val fileName = socketDIS?.readUTF()
        var fileLength = socketDIS?.readLong()

        val fileToSave = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath, fileName)

        val fileOutputStream = FileOutputStream(fileToSave)
        val bufferArray = ByteArray(5_000_000)

        if(fileLength != null) {
            while (fileLength > 0) {
                val bytesRead =
                    socketDIS?.read(bufferArray, 0, min(fileLength.toInt(), bufferArray.size))
                if (bytesRead == -1) break
                fileOutputStream.write(bufferArray, 0, bytesRead!!)
                fileLength -= bytesRead
            }
        }

        fileOutputStream.flush()
        fileOutputStream.close()

        return fileToSave
    }


    fun close() {
        try {
            socket.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

