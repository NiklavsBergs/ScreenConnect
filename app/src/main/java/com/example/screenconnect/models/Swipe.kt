//@file:UseSerializers(OffsetSerializer::class)
package com.example.screenconnect.models

import android.util.Log
import androidx.compose.ui.geometry.Offset
import com.example.screenconnect.enums.Edge
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlin.math.abs
import kotlin.math.roundToInt

@Serializable
class Swipe(@Serializable(with = OffsetSerializer::class) val start: Offset, @Serializable(with = OffsetSerializer::class) val end: Offset, val phone: Phone) {

    var connectionPoint : Position = Position(-1, -1)

    var edge = Edge.NONE


    init {
        if(connectionPoint.x == -1){
            connectionPoint = calculateEdgeIntersection()
            Log.d("SWIPE", Json.encodeToString(this))
        }
    }

    fun calculateEdgeIntersection(): Position {
        // Calculate the slope of the line
        var slope = (end.y - start.y) / (end.x - start.x)

        if(slope == Float.POSITIVE_INFINITY || slope == Float.NEGATIVE_INFINITY){
            slope = 100F
        }

        val intersections = mutableListOf<Position>()

        val edges = mutableListOf<Edge>()

        val yIntercept = start.y - slope * start.x

        Log.d("SLOPE", slope.toString())

        val xLessThanStart = start.x > end.x

        val yLessThanStart = start.y > end.y

        if(abs(slope)<100){
            //LEFT
            intersections.add(Position(0, yIntercept.roundToInt()))
            //RIGHT
            intersections.add(Position(phone.width, (phone.width*slope + yIntercept).roundToInt()))

            edges.add(Edge.LEFT)
            edges.add(Edge.RIGHT)
        }
        if(abs(slope)>0.01){
            //TOP
            intersections.add(Position((abs(yIntercept)/slope).roundToInt(), 0))
            //BOTTOM
            intersections.add(Position(((phone.height-yIntercept)/slope).roundToInt(), phone.height))

            edges.add(Edge.TOP)
            edges.add(Edge.BOTTOM)
        }

        for(i in 0 until intersections.size){
            if(intersections[i].x in 0..phone.width && intersections[i].y in 0 ..phone.height){
                if((intersections[i].x < start.x) == xLessThanStart && (intersections[i].y < start.y) == yLessThanStart){
                    Log.d("INT-ACTUAL", i.toString())
                    edge = edges[i]
                    return intersections[i]
                }
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

//object PositionSerializer : KSerializer<Position> {
//    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Position", PrimitiveKind.STRING)
//
//    override fun serialize(encoder: Encoder, value: Position) {
//        encoder.encodeString("${value.x},${value.y}")
//    }
//
//    override fun deserialize(decoder: Decoder): Position {
//        val (x, y) = decoder.decodeString().split(",").map { it.toInt() }
//        return Position(x, y)
//    }
//}