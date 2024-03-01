package com.example.screenconnect.models

import androidx.compose.ui.geometry.Offset
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

@Serializable
class Position(var x: Int, var y: Int) {

    constructor(offset: Offset) : this(offset.x.roundToInt(), offset.y.roundToInt()) {}

    operator fun plus(position: Position): Position{
        return Position(this.x + position.x, this.y + position.y)
    }

//    fun flip(): Position{
//        return Position(this.y, this.x)
//    }

    fun rotate(rotation: Int): Position{
        when(rotation){
            90 -> return(Position(-this.y, x))
            -90 -> return(Position(- y, -this.x))
//            -90 -> return(Position(-this.y, x))
            180 -> return(Position(-1*this.x, -1*this.y))
//            -180 -> return(Position(-1*this.x, -1*this.y))
        }

        return this
    }

    fun rotateWithScreen(screenSize: Position, phone: Phone): Position{
        when(phone.rotation){
            90 -> return(Position(this.y, screenSize.x - x - phone.height))
            -90 -> return(Position(screenSize.y - y - phone.width, this.x))
//            -90 -> return(Position(this.y, screenSize.x - x - phone.height))
            180 -> return(Position(screenSize.x - x - phone.width, screenSize.y - y - phone.height))
//            -180 -> return(Position(screenSize.x - x - phone.width, screenSize.y - y - phone.height))
        }

        return this
    }

    override fun toString(): String {

        return "Position($x, $y)"

    }
}