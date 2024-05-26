package com.example.screenconnect.models

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Offset
import com.example.screenconnect.enums.Edge
import com.example.screenconnect.enums.SwipeType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.time.LocalTime
import kotlin.math.abs
import kotlin.math.roundToInt

@RequiresApi(Build.VERSION_CODES.O)
@Serializable
class Swipe(@Serializable(with = OffsetSerializer::class) val start: Offset, @Serializable(with = OffsetSerializer::class) val end: Offset, val phone: Phone) {

    var connectionPoint : Position = Position(-1, -1)

    var edge = Edge.NONE

    @Serializable(with = LocalTimeSerializer::class)
    var time: LocalTime = LocalTime.now()

    var type: SwipeType = SwipeType.NONE

    val BORDER_HOR = phone.DPI * 0.4
    val BORDER_VERT = phone.DPI * 0.4
    val MIN_SWIPE = 50

    init {
        if(connectionPoint.x == -1){
            connectionPoint = calculateEdgeIntersection()
        }
    }

    private fun isDisconnectSwipe(): Boolean{
        return if(start.x < BORDER_VERT && (end.x > BORDER_VERT && end.x < phone.width - BORDER_VERT)){
            type = SwipeType.DISCONNECT
            true
        } else{
            false
        }
    }

    private fun calculateEdgeIntersection(): Position {
        // Get the point where swipe meets the edge of the screen

        // Calculated by getting the cross point of a line that starts at the swipe start point
        // and goes through the swipe end point, and an edge of the screen

        // Could also just use the end point of swipe and closest edge to it, more similar to https://github.com/paulsonnentag/swip
        // This was made in an effort to make it a little more precise than Swip.js solution

        if(isDisconnectSwipe()){
            type = SwipeType.DISCONNECT
            return Position(0,0)
        }

        val diffX = end.x - start.x
        val diffY = end.y - start.y

        var slope = diffY / diffX

        if(slope == Float.POSITIVE_INFINITY){
            slope = 100F
        }
        else if(slope == Float.NEGATIVE_INFINITY){
            slope = -100F
        }

        // Need to scale, because it is calculated from swipe start point, which has not been normalized yet
        val yIntercept = (start.y - slope * start.x) * phone.scaleFactor

        if(abs(diffX) > abs(diffY) && abs(diffX) > MIN_SWIPE){
            // Swipe is horizontal
            if(end.x < start.x && end.x < BORDER_VERT){
                edge = Edge.LEFT
                type = SwipeType.CONNECT

                return Position(0, (yIntercept).roundToInt())
            }
            else if(end.x > start.x && end.x > phone.width -  BORDER_VERT){
                edge = Edge.RIGHT
                type = SwipeType.CONNECT

                return Position(phone.width, (phone.width*slope + yIntercept).roundToInt() )
            }
        }
        else if(abs(diffY) > abs(diffX) && abs(diffY) > MIN_SWIPE){
            // Swipe is vertical
            if(end.y < start.y && end.y < BORDER_HOR){
                edge = Edge.TOP
                type = SwipeType.CONNECT

                return Position((abs(yIntercept)/abs(slope)).roundToInt(), 0)
            }
            else if(end.y > start.y && end.y > phone.height -  BORDER_HOR) {
                edge = Edge.BOTTOM
                type = SwipeType.CONNECT

                return Position(((phone.height-yIntercept)/slope).roundToInt(), phone.height)
            }
        }
        
        return Position(0, 0)
    }
}

object OffsetSerializer : KSerializer<Offset> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Offset", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Offset) {
        encoder.encodeString("${value.x},${value.y}")
    }

    override fun deserialize(decoder: Decoder): Offset {
        val (x, y) = decoder.decodeString().split(",").map { it.toFloat() }
        return Offset(x, y)
    }
}

object LocalTimeSerializer : KSerializer<LocalTime> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LocalTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalTime) {
        encoder.encodeString(value.toString())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun deserialize(decoder: Decoder): LocalTime {
        return LocalTime.parse(decoder.decodeString())
    }
}
