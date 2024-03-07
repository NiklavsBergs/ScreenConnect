//@file:UseSerializers(OffsetSerializer::class)
package com.example.screenconnect.models

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Offset
import com.example.screenconnect.enums.Edge
import com.example.screenconnect.enums.SwipeType
import kotlinx.datetime.Clock
//import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.serializers.LocalTimeComponentSerializer
import kotlinx.datetime.toLocalDateTime
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
    var time = LocalTime.now()

    var type: SwipeType = SwipeType.NONE

    private val BORDER_HOR = phone.height / 10
    private val BORDER_VERT = phone.width / 10

    private val MIN_SWIPE = 10

    init {
        if(connectionPoint.x == -1){
            connectionPoint = calculateEdgeIntersection()
            Log.d("SWIPE", Json.encodeToString(this))
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

    fun calculateEdgeIntersection(): Position {

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

        val yIntercept = start.y - slope * start.x

        if(abs(diffX) > abs(diffY) && abs(diffX) > MIN_SWIPE){
            if(end.x < start.x && end.x < BORDER_VERT){
                edge = Edge.LEFT
                type = SwipeType.CONNECT

                return Position(0, yIntercept.roundToInt())
            }
            else if(end.x > start.x && end.x > phone.width -  BORDER_VERT){
                edge = Edge.RIGHT
                type = SwipeType.CONNECT

                return Position(phone.width, (phone.width*slope + yIntercept).roundToInt())
            }
        }
        else if(abs(diffY) > abs(diffX) && abs(diffY) > MIN_SWIPE){
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
