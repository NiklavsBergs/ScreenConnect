package com.example.screenconnect.screens

import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.screenconnect.components.numberSelect
import com.example.screenconnect.enums.SwipeType
import com.example.screenconnect.models.Swipe
import com.example.screenconnect.network.Connection
import com.example.screenconnect.util.getRealPathFromUri
import kotlinx.coroutines.launch
import java.io.File
import java.util.Timer
import kotlin.concurrent.schedule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedScreen(
    navController: NavController,
    sharedViewModel: SharedViewModel,
    connection: Connection
) {

    BackHandler {
        sharedViewModel.showImage = false
        navController.popBackStack()
    }

    val context = LocalContext.current

    var dragX = 0.0
    var dragY = 0.0

    var startX = 0.0
    var startY = 0.0

    val initialPosition = remember { mutableStateOf(Offset.Zero) }
    val endPosition = remember { mutableStateOf(Offset.Zero) }
    var isDragging = false

    var timer by remember { mutableStateOf<Timer?>(null) }

    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }

    var borderVert by remember { mutableStateOf(sharedViewModel.thisPhone.borderVert) }
    var borderHor by remember { mutableStateOf(sharedViewModel.thisPhone.borderHor) }

    val launcher = rememberLauncherForActivityResult(
        contract =
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        sharedViewModel.imageUri = uri
        Log.d("Image URI path", uri?.path.toString())
        val imagePath = uri?.let { getRealPathFromUri(it, context) }
        if(imagePath != null){
            val imageFile = imagePath?.let { File(it) }
            sharedViewModel.sendImage(imageFile!!)
            if(sharedViewModel.isGroupOwner){
                sharedViewModel.processReceivedImage(File(imagePath))
            }
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            // Detects swipe events on screen
            detectDragGestures(onDragStart = { offset ->
                initialPosition.value = offset
                isDragging = true
            }, onDragEnd = {
                // Drag event ended, send it to server
                if (isDragging) {
                    endPosition.value = Offset(
                        initialPosition.value.x + dragX.toFloat(),
                        initialPosition.value.y + dragY.toFloat()
                    )

                    if (initialPosition.value.y > sharedViewModel.thisPhone.height - 200 &&
                        endPosition.value.y < sharedViewModel.thisPhone.height - 300) {
                        showBottomSheet = true
                    }

                    Log.d("DRAG", "End")

                    var swipe =
                        Swipe(initialPosition.value, endPosition.value, sharedViewModel.thisPhone)
                    sharedViewModel.sendSwipe(swipe)
                    if(swipe.type == SwipeType.DISCONNECT){
                        sharedViewModel.showImage = false
                        navController.popBackStack()
                    }
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

                    if (newPositionX <= 10 || newPositionX >= sharedViewModel.thisPhone.width - 10 &&
                        newPositionY <= 10 || newPositionY >= sharedViewModel.thisPhone.height - 10
                    ) {
                        // Position is outside screen bounds, end drag event, send it to server
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
                        sharedViewModel.sendSwipe(swipe)

                        isDragging = false
                    }
                }
            }}

        ){

        Image(
            bitmap = sharedViewModel.sharedImage.asImageBitmap(),
            contentDescription = "Shared Image",
            modifier = Modifier.fillMaxSize())

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showBottomSheet = false
                },
                sheetState = sheetState
            ) {
                Column{
                    Row {
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            modifier = Modifier.padding(end = 10.dp),
                            onClick = {
                                scope.launch { sheetState.hide() }.invokeOnCompletion {
                                    if (!sheetState.isVisible) {
                                        showBottomSheet = false
                                    }
                                }
                            }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Localized description")
                        }
                    }

                    Row {
                        Spacer(modifier = Modifier.weight(1f))
                        ExtendedFloatingActionButton(
                            onClick = { launcher.launch("image/*") },
                            icon = { Icon(Icons.Filled.Add, "Localized description") },
                            text = { Text(text = "Select image") },
                            modifier = Modifier.padding(bottom = 20.dp)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    OutlinedButton(
                        onClick = {
                            connection.disconnect()
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    showBottomSheet = false
                                }
                            }
                            sharedViewModel.showImage = false
                            navController.navigate(Screen.SettingsScreen.route)
                        },
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 20.dp)
                    ) {
                        Text(text = "Disconnect")
                    }
                }
            }
        }
    }


}