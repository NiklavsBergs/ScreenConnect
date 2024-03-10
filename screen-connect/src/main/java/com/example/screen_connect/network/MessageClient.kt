package com.example.screen_connect.network

import android.os.Environment
import android.util.Log
import com.example.screen_connect.models.Phone
import com.example.screen_connect.models.Swipe
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

class MessageClient (
    private val host: String,
    private val messageReceivedCallback: (String) -> Unit,
    private val imageReceivedCallback: (File) -> Unit
) : Thread(){

    private var socket: Socket = Socket()
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private var socketDOS: DataOutputStream? = null
    private var socketDIS: DataInputStream? = null

    var isConnected = false

    override fun run(){
        Log.d("HOST",host)

        while(!isConnected){
            try {
                socket?.connect(InetSocketAddress(host, 8888), 5000)
                inputStream = socket?.getInputStream()
                outputStream = socket?.getOutputStream()

                socketDOS = DataOutputStream(BufferedOutputStream(outputStream))
                socketDIS = DataInputStream(BufferedInputStream(inputStream))

                isConnected = true

                Log.d("CLIENT-START",host)

            } catch (e: IOException) {
                Log.d("CLIENT-START", "ERROR")
                e.printStackTrace()
                isConnected = false
            }
        }

        while(socket!=null  && !socket!!.isClosed){
            try{

                val type = socketDIS?.readUTF()

                if(type?.compareTo("Info") == 0) {
                    Log.d("CLIENT-RECEIVE","Receiving info")
                    val message = socketDIS?.readUTF()
                    messageReceivedCallback(message!!)
                }
                else if (type?.compareTo("Image") == 0){
                    Log.d("CLIENT-RECEIVE","Receiving image")
                    val savedFile = receiveImage()
                    imageReceivedCallback(savedFile)
                }

            } catch (e: IOException) {
                Log.d("CLIENT-RECEIVE", "ERROR")
                e.printStackTrace()
                socket.close()
            }

            }
            Log.d("CLIENT-RECEIVE-END", "ENDED")

    }

    fun sendPhoneInfo(phone: Phone){
        try{
            if(socketDOS != null) {
                socketDOS!!.writeUTF("Info")

                var phoneInfo = Json.encodeToString(phone)

                socketDOS!!.writeUTF(phoneInfo)

                socketDOS!!.flush()

                Log.d("CLIENT-SEND-INFO", phoneInfo)
            }
        }
        catch (e: IOException){
            Log.d("CLIENT-SEND", "ERROR")
            e.printStackTrace()
        }
    }

    fun sendSwipe(swipe: Swipe){
        try{
            if(socketDOS != null) {
                socketDOS!!.writeUTF("Info")

                var swipe = Json.encodeToString(swipe)

                Log.d("CLIENT-SEND-SWIPE", swipe)

                socketDOS!!.writeUTF(swipe)

                socketDOS!!.flush()
            }
        }
        catch (e: IOException){
            Log.d("CLIENT-SEND", "ERROR")
            e.printStackTrace()
        }
    }

    fun sendImage(file: File){

        if(socketDOS != null) {
            socketDOS!!.writeUTF("Image")

            socketDOS!!.writeUTF(file.name)

            socketDOS!!.writeLong(file.length())

            Log.d("CLIENT-SEND-IMAGE", file.name)

            val fileIS = FileInputStream(file)
            val bufferArray = ByteArray(5_000_000)
            var lengthRead: Int

            while (fileIS.read(bufferArray).also { lengthRead = it } > 0) {
                socketDOS!!.write(bufferArray, 0, lengthRead)
            }
            fileIS.close()
        }
    }

    private fun receiveImage(): File{

        val fileName = socketDIS?.readUTF()

        var fileLength = socketDIS?.readLong()

        val fileToSave = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath, fileName)

        val fileOutputStream = FileOutputStream(fileToSave)
        val bufferArray = ByteArray(5_000_000)

        Log.d("CLIENT-SAVE-IMAGE", fileToSave.path)

        if(fileLength != null) {
            while (fileLength > 0) {
                val bytesRead =
                    socketDIS?.read(bufferArray, 0, min(fileLength.toInt(), bufferArray.size))
                if (bytesRead == -1) break
                fileOutputStream.write(bufferArray, 0, bytesRead!!)
                fileLength -= bytesRead!!
            }
        }

        fileOutputStream.flush()
        fileOutputStream.close()
        Log.d("CLIENT-RECEIVE","Image saved")

        return fileToSave
    }


    fun close() {
        try {
            socket?.close()
            Log.e("CLIENT-CLOSE", "Closed")
        } catch (e: IOException) {
            Log.e("CLIENT-CLOSE", "ERROR")
            e.printStackTrace()
        }
    }
}

