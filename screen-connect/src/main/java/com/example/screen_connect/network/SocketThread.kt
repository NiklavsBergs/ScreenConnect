package com.example.screen_connect.network

import android.os.Environment
import android.util.Log
import com.example.screen_connect.models.Phone
import com.example.screen_connect.models.Swipe
import com.example.screen_connect.models.VirtualScreen
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

class SocketThread(private val socket: Socket, private val messageReceivedListener: MessageReceivedListener?) : Thread() {

    var phoneId = ""

    private val input = DataInputStream(BufferedInputStream(socket.getInputStream()))
    private val output = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))

    private val receivedOnce = false

    override fun run() {
        try {
            while (!socket.isClosed) {
                val type = input.readUTF()

                if(type?.compareTo("Info") == 0) {
                    Log.d("SERVER-RECEIVE","Receiving info")
                    val message = input.readUTF()

                    if(!receivedOnce){
                        var swipe = Json.decodeFromString<Swipe>(message)
                        phoneId = swipe.phone.id
                    }

                    messageReceivedListener?.onMessageReceived(message)
                }
                else if (type?.compareTo("Image") == 0){
                    Log.d("SERVER-RECEIVE","Receiving image")
                    receiveImage()
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

    fun sendData(data: String) {
        output.writeUTF(data)
        output.flush()
    }

    fun sendClientInfo(phone: Phone){
        Log.d("SERVER-SEND", "Sending...")

        try{
            output.writeUTF("Info")

            var phoneInfo = "PhoneInfo*" + Json.encodeToString(phone)

            output.writeUTF(phoneInfo)

            output.flush()

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
            output.writeUTF("Info")

            //var screenInfo = "ScreenInfo:" + screen.vHeight + "," + screen.vWidth + "," + screen.DPI
            var screenInfo = "ScreenInfo*" + Json.encodeToString(screen)
            //var screenInfo = Json.encodeToString(screen)

            output.writeUTF(screenInfo)

            output.flush()

            Log.d("SERVER-SEND-INFO", screenInfo)
        }
        catch (e: IOException){
            Log.d("SERVER-SEND-ERROR", e.toString())
        }
        Log.d("SERVER-SOCKET-CLOSED", socket?.isClosed.toString())
    }

    fun sendImage(file: File){

        if(output != null) {
            output.writeUTF("Image")

            output.writeUTF(file.name)

            output.writeLong(file.length())

            Log.d("SERVER-SEND-IMAGE", file.name)

            val fileIS = FileInputStream(file)
            val bufferArray = ByteArray(5_000_000)
            var lengthRead: Int

            while (fileIS.read(bufferArray).also { lengthRead = it } > 0) {
                output.write(bufferArray, 0, lengthRead)
            }
            fileIS.close()
        }
    }

    private fun receiveImage(){

        val fileName = input.readUTF()

        var fileLength = input.readLong()

        val fileToSave = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath, fileName)
        val fileOutputStream = FileOutputStream(fileToSave)
        val bufferArray = ByteArray(5_000_000)

        Log.d("CLIENT-SAVE-IMAGE", fileToSave.path)

        if(fileLength != null) {
            while (fileLength > 0) {
                val bytesRead =
                    input.read(bufferArray, 0,
                        Integer.min(fileLength.toInt(), bufferArray.size)
                    )
                if (bytesRead == -1) break
                fileOutputStream.write(bufferArray, 0, bytesRead!!)
                fileLength -= bytesRead!!
            }
        }

        fileOutputStream.flush()
        fileOutputStream.close()
        Log.d("CLIENT-RECEIVE","Image saved")
    }

}