package com.example.screenconnect.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
class Phone(var heightReal: Int, val widthReal: Int, val DPI: Int, val phoneName: String, val id: String) {

    val BASE_DPI = 400.0

    var scaleFactor = BASE_DPI / DPI

    var position: Position = Position(0, 0)
    var rotation = 0

    var height = 0
    var width = 0

    var borderVert = 2.0
    var borderHor = 2.0

    var isHost = false

    fun addToHeight(value: Int){
        heightReal += value

        height += (value * scaleFactor).toInt()
    }

    init {
        height = (heightReal * scaleFactor).toInt()
        width = (widthReal * scaleFactor).toInt()
    }

    fun copy(): Phone {
        val phoneString = Json.encodeToString(this)
        return Json.decodeFromString<Phone>(phoneString)
    }
}