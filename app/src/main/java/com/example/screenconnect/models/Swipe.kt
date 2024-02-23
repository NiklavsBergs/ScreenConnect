//@file:UseSerializers(OffsetSerializer::class)
package com.example.screenconnect.models

import androidx.compose.ui.geometry.Offset
import com.example.screenconnect.enums.Edge
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
class Swipe(@Serializable(with = OffsetSerializer::class) val start: Offset, @Serializable(with = OffsetSerializer::class) val end: Offset, val phone: PhoneScreen) {

    @Serializable(with = OffsetSerializer::class)
    var connectionPoint = Offset.Zero

    var edge = Edge.LEFT

    init {
        connectionPoint = calculateEdgeIntersection()
    }

    fun calculateEdgeIntersection(): Offset{
        val slope = (end.y - start.y) / (end.x - start.x)

        edge = if (start.x < end.x) {
            // Moving from left to right
            if (start.y < end.y) {
                // Moving from top-left to bottom-right
                if (slope > 0) Edge.RIGHT else Edge.BOTTOM
            } else {
                // Moving from bottom-left to top-right
                if (slope < 0) Edge.RIGHT else Edge.TOP
            }
        } else {
            // Moving from right to left
            if (start.y < end.y) {
                // Moving from top-right to bottom-left
                if (slope < 0) Edge.LEFT else Edge.BOTTOM
            } else {
                // Moving from bottom-right to top-left
                if (slope > 0) Edge.LEFT else Edge.TOP
            }
        }

        return when (edge) {
            Edge.LEFT -> Offset(0f, start.y + slope * (-start.x))
            Edge.RIGHT -> Offset(phone.width.toFloat(), start.y + slope * (phone.width - start.x))
            Edge.TOP -> Offset(start.x + (1 / slope) * (-start.y), 0f)
            Edge.BOTTOM -> Offset(start.x + (1 / slope) * (phone.height - start.y), phone.height.toFloat())
        }
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