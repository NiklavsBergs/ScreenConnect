package com.example.screenconnect.network

import java.io.File

interface MessageReceivedListener {
    fun onMessageReceived(message: String)
}

interface ImageReceivedListener {
    fun onImageReceived(file: File)
}