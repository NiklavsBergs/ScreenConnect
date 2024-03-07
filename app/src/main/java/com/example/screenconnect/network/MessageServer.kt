package com.example.screenconnect.network

import android.os.Environment
import android.util.Log
import com.example.screenconnect.models.Phone
import com.example.screenconnect.models.VirtualScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
import java.net.ServerSocket
import java.net.Socket

class MessageServer (
    private val thisPhone: Phone,
    private val messageReceivedCallback: (String) -> Unit
    ) : Thread(), MessageReceivedListener{

    private val connectLimit = 5
    private val socketThreads = mutableListOf<SocketThread>()

//    var serverSocket:ServerSocket? = null
//    var socket: Socket? = null
//    var inputStream: InputStream? = null
//    var outputStream: OutputStream? = null
//
//    var socketDOS: DataOutputStream? = null
//    var socketDIS: DataInputStream? = null

    override fun run() {
        var serverSocket: ServerSocket? = null
        try {
            serverSocket = ServerSocket(8888)
            while (socketThreads.size <= connectLimit) {
                val socket = serverSocket.accept()
                val socketThread = SocketThread(socket, this)
                socketThreads.add(socketThread)
                socketThread.start()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            serverSocket?.close()
        }
    }

    override fun onMessageReceived(message: String){
        messageReceivedCallback(message)
    }

    fun sendClientInfo(phone: Phone){
        for(client in socketThreads){
            if(client.phoneId == phone.id){
                client.sendClientInfo(phone)
            }
        }
    }

    fun sendScreenInfo(screen: VirtualScreen){

        socketThreads.forEach {
            if(screen.isInScreenById(it.phoneId)){
                it.sendScreenInfo(screen)
            }
        }

    }

    fun sendImage(file: File){
        socketThreads.forEach { it.sendImage(file) }
    }

    fun close() {
        //serverSocket?.close()
        socketThreads.forEach { it.interrupt() }
    }

//    fun send(message: String){
//        Log.d("SERVER-SEND", "Starting...")
//                try{
//                    outputStream?.write(message.toByteArray())
//
//                    Log.d("SERVER-SEND", message)
//                }
//                catch (e: IOException){
//                    Log.d("SERVER-SEND", e.toString())
//                }
//    }

//    fun sendPhoneInfo(phone: Phone){
//        Log.d("SERVER-SEND", "Sending...")
//
//        try{
//            socketDOS!!.writeUTF("Info")
//
//            var phoneInfo = "PhoneInfo:" + phone.height + "," + phone.width + "," + phone.DPI + "," + phone.phoneName + "," + phone.id
//            //outputStream?.write(phoneInfo.toByteArray())
//            socketDOS!!.writeUTF(phoneInfo)
//
//            socketDOS!!.flush()
//
//            Log.d("SERVER-SEND-INFO", phoneInfo)
//        }
//        catch (e: IOException){
//            Log.d("SERVER-SEND-ERROR", e.toString())
//        }
//        Log.d("SERVER-SEND-SOCKET-CLOSED", socket?.isClosed.toString())
//    }

//    fun sendClientInfo(phone: Phone){
//        Log.d("SERVER-SEND", "Sending...")
//
//        try{
//            socketDOS!!.writeUTF("Info")
//
//            //var phoneInfo = "PhoneInfo:" + phone.locationX + "," + phone.locationY + "," + phone.nr
//            var phoneInfo = "PhoneInfo*" + Json.encodeToString(phone)
//
//            //var phoneInfo = Json.encodeToString(phone)
//
//            socketDOS!!.writeUTF(phoneInfo)
//
//            socketDOS!!.flush()
//
//            Log.d("SERVER-SEND-INFO", phoneInfo)
//        }
//        catch (e: IOException){
//            Log.d("SERVER-SEND-ERROR", e.toString())
//        }
//        Log.d("SERVER-SEND-SOCKET-CLOSED", socket?.isClosed.toString())
//    }
//
//    fun sendScreenInfo(screen: VirtualScreen){
//        Log.d("SERVER-SEND", "Sending...")
//
//        try{
//            socketDOS!!.writeUTF("Info")
//
//            //var screenInfo = "ScreenInfo:" + screen.vHeight + "," + screen.vWidth + "," + screen.DPI
//            var screenInfo = "ScreenInfo*" + Json.encodeToString(screen)
//            //var screenInfo = Json.encodeToString(screen)
//
//            socketDOS!!.writeUTF(screenInfo)
//
//            socketDOS!!.flush()
//
//            Log.d("SERVER-SEND-INFO", screenInfo)
//        }
//        catch (e: IOException){
//            Log.d("SERVER-SEND-ERROR", e.toString())
//        }
//        Log.d("SERVER-SOCKET-CLOSED", socket?.isClosed.toString())
//    }
//
//    fun sendImage(file: File){
//
//        if(socketDOS != null) {
//            socketDOS!!.writeUTF("Image")
//
//            socketDOS!!.writeUTF(file.name)
//
//            socketDOS!!.writeLong(file.length())
//
//            Log.d("SERVER-SEND-IMAGE", file.name)
//
//            val fileIS = FileInputStream(file)
//            val bufferArray = ByteArray(5_000_000)
//            var lengthRead: Int
//
//            while (fileIS.read(bufferArray).also { lengthRead = it } > 0) {
//                socketDOS!!.write(bufferArray, 0, lengthRead)
//            }
//            fileIS.close()
//        }
//    }
//
//    fun receiveImage(){
//
//        val fileName = socketDIS?.readUTF()
//
//        var fileLength = socketDIS?.readLong()
//
//        val fileToSave = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath, fileName)
//        val fileOutputStream = FileOutputStream(fileToSave)
//        val bufferArray = ByteArray(5_000_000)
//
//        Log.d("CLIENT-SAVE-IMAGE", fileToSave.path)
//
//        if(fileLength != null) {
//            while (fileLength > 0) {
//                val bytesRead =
//                    socketDIS?.read(bufferArray, 0,
//                        Integer.min(fileLength.toInt(), bufferArray.size)
//                    )
//                if (bytesRead == -1) break
//                fileOutputStream.write(bufferArray, 0, bytesRead!!)
//                fileLength -= bytesRead!!
//            }
//        }
//
//        fileOutputStream.flush()
//        fileOutputStream.close()
//        Log.d("CLIENT-RECEIVE","Image saved")
//    }


//    fun close() {
//        try {
//            socket?.close()
//            serverSocket?.close()
//        } catch (e: IOException) {
//            Log.e("SERVER-STOP", e.toString())
//        }
//    }
}
