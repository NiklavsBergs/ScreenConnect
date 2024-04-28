package com.example.screenconnect.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.navigation.NavController
import com.example.screenconnect.models.Swipe
import com.example.screenconnect.screens.SharedViewModel

@Composable
fun SharedScreen(navController: NavController, sharedViewModel: SharedViewModel) {

    var dragX = 0.0
    var dragY = 0.0

    var startX = 0.0
    var startY = 0.0

    val initialPosition = remember { mutableStateOf(Offset.Zero) }
    val endPosition = remember { mutableStateOf(Offset.Zero) }
    var isDragging = false

    Column(modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            detectDragGestures(onDragStart = { offset ->
                initialPosition.value = offset
                isDragging = true
            }, onDragEnd = {
                if (isDragging) {
                    endPosition.value = Offset(
                        initialPosition.value.x + dragX.toFloat(),
                        initialPosition.value.y + dragY.toFloat()
                    )
                    Log.d("DRAG", "End")

                    var swipe =
                        Swipe(initialPosition.value, endPosition.value, sharedViewModel.thisPhone)
                    sharedViewModel.sendSwipe(swipe)
                    Log.d("SWIPE-END", endPosition.value.toString())

                    dragX = 0.0
                    dragY = 0.0

                    isDragging = false
                }

            }) { change, dragAmount ->
                change.consume()
                dragX += dragAmount.x
                dragY += dragAmount.y

                val newPositionX = initialPosition.value.x + dragX.toFloat()
                val newPositionY = initialPosition.value.y + dragY.toFloat()

                if (isDragging) {

                    if (newPositionX >= 10 && newPositionX <= sharedViewModel.thisPhone.width - 10 &&
                        newPositionY >= 10 && newPositionY <= sharedViewModel.thisPhone.height - 10
                    ) {
                        // If position is within bounds, continue drag
                    } else {
                        // Position is outside bounds, end drag event
                        endPosition.value = Offset(newPositionX, newPositionY)
                        dragX = 0.0
                        dragY = 0.0

                        var swipe = Swipe(
                            initialPosition.value,
                            endPosition.value,
                            sharedViewModel.thisPhone
                        )
                        Log.d("DRAG", "Out of bounds")
                        Log.d("SWIPE-END", endPosition.value.toString())
                        //Log.d("Drag", initialPosition.value.toString() + ", " + endPosition.value.toString())
                        sharedViewModel.sendSwipe(swipe)
                        isDragging = false
                    }
                }
            }

        }){
        Image(sharedViewModel.sharedImage.asImageBitmap(), contentDescription = "Bitmap Image")
    }


}