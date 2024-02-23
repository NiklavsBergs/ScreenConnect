package com.example.screenconnect.models

import kotlinx.serialization.Serializable

@Serializable
class PhoneScreen(val height: Int, val width: Int, val DPI: Int, val phoneName: String, val id: String) {

    var locationX = 0
    var locationY = 0
    var scale = 1
    var rotation = 0

    var nr = 0
}