package com.example.screen_connect.models

import com.example.screen_connect.models.Position
import kotlinx.serialization.Serializable

@Serializable
class Phone(var heightReal: Int, val widthReal: Int, val DPI: Int, val phoneName: String, val id: String) {

    val BASE_DPI = 400.0

    var scaleFactor = BASE_DPI / DPI

    var position: Position = Position(0, 0)
    var rotation = 0

    var height = 0
    var width = 0

    var isHost = false

    fun addToHeight(value: Int){
        heightReal += value

        height += (value * scaleFactor).toInt()
    }

    init {
        height = (heightReal * scaleFactor).toInt()
        width = (widthReal * scaleFactor).toInt()
    }
}