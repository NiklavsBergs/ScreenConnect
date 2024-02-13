package com.example.screenconnect.network

import android.os.Environment
import android.util.Log
import com.example.screenconnect.models.PhoneScreen
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInput
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
    private val thisPhone: PhoneScreen,
    private val host: String,
    private val messageReceivedCallback: (String) -> Unit,
    private val imageReceivedCallback: (File) -> Unit
) : Thread(){
    var socket: Socket = Socket()
    var inputStream: InputStream? = null
    var outputStream: OutputStream? = null

    var socketDOS: DataOutputStream? = null
    var socketDIS: DataInputStream? = null

    override fun run(){
        Log.d("HOST",host)

        try {
            socket?.connect(InetSocketAddress(host, 8888), 500)
            inputStream = socket?.getInputStream()
            outputStream = socket?.getOutputStream()

            socketDOS = DataOutputStream(BufferedOutputStream(outputStream))
            socketDIS = DataInputStream(BufferedInputStream(inputStream))

            Log.d("CLIENT-START",host)

            sendPhoneInfo(thisPhone)


        } catch (e: IOException) {
            Log.d("CLIENT-INIT", e.toString())
        }


        Log.d("CLIENT-RECEIVE", "Running")
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
                Log.d("CLIENT-RECEIVE-ERROR", e.toString())
                socket.close()
            }

            }
            Log.d("CLIENT-RECEIVE-END", "ENDED")

    }

    fun send(message: String){
        Log.d("CLIENT-SEND", "Sending...")


            try{
                outputStream?.write(message.toByteArray())

                Log.d("CLIENT-SEND", message)
            }
            catch (e: IOException){
                Log.d("CLIENT-SEND-ERROR", e.toString())
            }
            Log.d("CLIENT-SEND-SOCKET", socket?.isClosed.toString())
    }

    fun sendPhoneInfo(phone: PhoneScreen){
        Log.d("CLIENT-SEND", "Sending...")


        try{
            if(socketDOS != null) {
                socketDOS!!.writeUTF("Info")

                var phoneInfo =
                    "PhoneInfo:" + phone.height + "," + phone.width + "," + phone.DPI + "," + phone.phoneName + "," + phone.id
                //outputStream?.write(phoneInfo.toByteArray())
                socketDOS!!.writeUTF(phoneInfo)

                socketDOS!!.flush()

                Log.d("CLIENT-SEND-INFO", phoneInfo)
            }
        }
        catch (e: IOException){
            Log.d("CLIENT-SEND-ERROR", e.toString())
        }
        Log.d("CLIENT-SEND-SOCKET-CLOSED", socket?.isClosed.toString())
    }

    fun sendImage(file: File){

        if(socketDOS != null) {
            socketDOS!!.writeUTF("Image")

            socketDOS!!.writeUTF(file.name)

            socketDOS!!.writeLong(file.length())

            Log.d("SERVER-SEND-IMAGE", file.name)

            val fileIS = FileInputStream(file)
            val bufferArray = ByteArray(5_000_000)
            var lengthRead: Int

            while (fileIS.read(bufferArray).also { lengthRead = it } > 0) {
                socketDOS!!.write(bufferArray, 0, lengthRead)
            }
            fileIS.close()
        }
    }

    fun receiveImage(): File{

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
        } catch (e: IOException) {
            Log.e("SERVER-STOP", e.toString())
        }
    }
}

