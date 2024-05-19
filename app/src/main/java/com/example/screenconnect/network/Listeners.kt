package com.example.screenconnect.network

import com.example.screenconnect.enums.MessageType
import java.io.File

interface MessageReceivedListener {
    fun onMessageReceived(message: String, type: MessageType)
}

interface ImageReceivedListener {
    fun onImageReceived(file: File)
}