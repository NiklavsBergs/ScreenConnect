package com.example.screenconnect.network

import android.os.Environment
import android.util.Log
import com.example.screenconnect.models.Phone
import com.example.screenconnect.models.Swipe
import com.example.screenconnect.models.VirtualScreen
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
import java.net.Socket

class SocketThread(private val socket: Socket, private val messageReceivedListener: MessageReceivedListener) : Thread() {

    var phoneId = ""

    private val input = DataInputStream(BufferedInputStream(socket.getInputStream()))
    private val output = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))

    override fun run() {
        try {
            while (!socket.isClosed) {
                val messageTypeOrdinal = input.readInt()
                val messageType = MessageType.values()[messageTypeOrdinal]

                if (messageType == MessageType.IMAGE){
                    messageReceivedListener.onImageReceived(receiveImage())
                }
                else {
                    val message = input.readUTF()

                    // Save Client ID on first message receive
                    if(phoneId == "" && messageType == MessageType.SWIPE){
                        var swipe = Json.decodeFromString<Swipe>(message)
                        phoneId = swipe.phone.id
                    }

                    messageReceivedListener.onMessageReceived(message, messageType)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                socket.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun sendClientInfo(phone: Phone){
        try{
            output.writeInt(MessageType.PHONE.ordinal)

            val phoneInfo = Json.encodeToString(phone)

            output.writeUTF(phoneInfo)

            output.flush()
        }
        catch (e: IOException){
            e.printStackTrace()
        }
    }

    fun sendScreenInfo(screen: VirtualScreen){
        try{
            output.writeInt(MessageType.SCREEN.ordinal)

            val screenInfo = Json.encodeToString(screen)

            output.writeUTF(screenInfo)

            output.flush()
        }
        catch (e: IOException){
            e.printStackTrace()
        }
    }

    fun sendImage(file: File){

        output.writeInt(MessageType.IMAGE.ordinal)

        output.writeUTF(file.name)

        output.writeLong(file.length())

        val fileIS = FileInputStream(file)
        val bufferArray = ByteArray(1_000_000)
        var lengthRead: Int

        while (fileIS.read(bufferArray).also { lengthRead = it } > 0) {
            output.write(bufferArray, 0, lengthRead)
        }
        fileIS.close()
    }

    private fun receiveImage(): File {

        val fileName = input.readUTF()

        var fileLength = input.readLong()

        val fileToSave = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath, fileName)
        val fileOutputStream = FileOutputStream(fileToSave)
        val bufferArray = ByteArray(5_000_000)

        while (fileLength > 0) {
            val bytesRead =
                input.read(bufferArray, 0,
                    Integer.min(fileLength.toInt(), bufferArray.size)
                )
            if (bytesRead == -1) break
            fileOutputStream.write(bufferArray, 0, bytesRead)
            fileLength -= bytesRead
        }

        fileOutputStream.flush()
        fileOutputStream.close()

        return fileToSave
    }

}