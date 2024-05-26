package com.example.screenconnect.network

import com.example.screenconnect.enums.MessageType
import com.example.screenconnect.models.Phone
import com.example.screenconnect.models.VirtualScreen
import java.io.File
import java.io.IOException
import java.net.ServerSocket

// Server/Client structure inspired by https://github.com/stlong0521/wifi-direct-group
class MessageServer (
    private val thisPhone: Phone,
    private val messageReceivedCallback: (String, MessageType) -> Unit,
    private val imageeReceivedCallback: (File) -> Unit
    ) : Thread(), MessageReceivedListener{

    private val connectLimit = 20
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

    override fun onImageReceived(file: File) {
        sendImage(file)
        imageeReceivedCallback(file)
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
