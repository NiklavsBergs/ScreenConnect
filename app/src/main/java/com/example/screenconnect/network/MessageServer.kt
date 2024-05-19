package com.example.screenconnect.network

import android.os.Environment
import android.util.Log
import com.example.screenconnect.enums.MessageType
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
    private val messageReceivedCallback: (String, MessageType) -> Unit
    ) : Thread(), MessageReceivedListener{

    private val connectLimit = 10
    private val socketThreads = mutableListOf<SocketThread>()
    var serverSocket: ServerSocket? = null

    override fun run() {
        try {
            serverSocket = ServerSocket(8888)

            while (socketThreads.size <= connectLimit) {
                val socket = serverSocket!!.accept()
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

    override fun onMessageReceived(message: String, type: MessageType){
        messageReceivedCallback(message, type)
    }

    fun updateClientInfo(screen: VirtualScreen){
        var phones = screen.phones

        for(client in socketThreads){
            for(phone in phones){
                if(client.phoneId == phone.id){
                    client.sendClientInfo(phone)
                }
            }
        }
        sendScreenInfo(screen)
    }

    fun sendClientInfo(phone: Phone){
        for(client in socketThreads){
            if(client.phoneId == phone.id){
                client.sendClientInfo(phone)
            }
        }
    }

    private fun sendScreenInfo(screen: VirtualScreen){

        socketThreads.forEach {
            if(screen.isInScreenById(it.phoneId)){
                it.sendScreenInfo(screen)
            }
        }

    }

    fun sendScreenInfo(screen: VirtualScreen, phone: Phone){

        for(client in socketThreads){
            if(client.phoneId == phone.id){
                client.sendScreenInfo(screen)
            }
        }

    }

    fun sendImage(file: File){
        socketThreads.forEach { it.sendImage(file) }
    }

    fun close() {
        serverSocket?.close()
        socketThreads.forEach { it.interrupt() }
    }

}
