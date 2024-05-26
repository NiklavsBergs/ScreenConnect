package com.example.screenconnect.network

import com.example.screenconnect.enums.MessageType
import java.io.File

interface MessageReceivedListener {
    fun onMessageReceived(message: String, type: MessageType)
    fun onImageReceived(file: File)
}

interface ImageReceivedListener {
    fun onImageReceived(file: File)
}