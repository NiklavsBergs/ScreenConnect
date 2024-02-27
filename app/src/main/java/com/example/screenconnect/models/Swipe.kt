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

@Serializable
class Swipe(@Serializable(with = OffsetSerializer::class) val start: Offset, @Serializable(with = OffsetSerializer::class) val end: Offset, val phone: Phone) {

    @Serializable(with = OffsetSerializer::class)
    var connectionPoint = Offset.Zero

    var edge = Edge.NONE


    init {
        if(connectionPoint == Offset.Zero){
            connectionPoint = calculateEdgeIntersection()
            Log.d("SWIPE", Json.encodeToString(this))
        }
    }

    fun calculateEdgeIntersection(): Offset {
        // Calculate the slope of the line
        var slope = (end.y - start.y) / (end.x - start.x)

        if(slope == Float.POSITIVE_INFINITY || slope == Float.NEGATIVE_INFINITY){
            slope = 100F
        }

        val intersections = mutableListOf<Offset>()

        val edges = mutableListOf<Edge>()

        val yIntercept = start.y - slope * start.x

        Log.d("SLOPE", slope.toString())

        val xLessThanStart = start.x > end.x

        val yLessThanStart = start.y > end.y

        if(abs(slope)<100){
            //LEFT
            intersections.add(Offset(0F, yIntercept))
            //RIGHT
            intersections.add(Offset(phone.width.toFloat(), phone.width*slope + yIntercept))

            edges.add(Edge.LEFT)
            edges.add(Edge.RIGHT)
        }
        if(abs(slope)>0.01){
            //TOP
            intersections.add(Offset(abs(yIntercept)/slope, 0F))
            //BOTTOM
            intersections.add(Offset((phone.height-yIntercept)/slope, phone.height.toFloat()))

            edges.add(Edge.TOP)
            edges.add(Edge.BOTTOM)
        }

        for(i in 0 until intersections.size){
            if(intersections[i].x in 0.0..phone.width.toDouble() && intersections[i].y in 0.0 ..phone.height.toDouble()){
                if((intersections[i].x < start.x) == xLessThanStart && (intersections[i].y < start.y) == yLessThanStart){
                    Log.d("INT-ACTUAL", i.toString())
                    edge = edges[i]
                    return intersections[i]
                }
            }
        }

        return Offset(0F, 0F)
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