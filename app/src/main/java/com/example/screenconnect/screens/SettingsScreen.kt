package com.example.screenconnect.screens

import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.screenconnect.components.numberSelect
import com.example.screenconnect.components.statusBar
import com.example.screenconnect.models.Swipe
import com.example.screenconnect.network.Connection
import com.example.screenconnect.util.getRealPathFromUri
import com.example.screenconnect.util.isLocationEnabled
import com.example.screenconnect.util.locationPopup
import com.example.screenconnect.util.wifiPopup
import com.example.screenconnect.screens.SharedViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.schedule

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun SettingsScreen(navController: NavController, sharedViewModel: SharedViewModel, connection: Connection) {

    val context = LocalContext.current

    if(sharedViewModel.showImage){
        navController.navigate(Screen.SharedScreen.route)
        sharedViewModel.showImage = false
    }
    var dragX = 0.0
    var dragY = 0.0

    var startX = 0.0
    var startY = 0.0

    val initialPosition = remember { mutableStateOf(Offset.Zero) }
    val endPosition = remember { mutableStateOf(Offset.Zero) }
    var isDragging = false

    var borderVert by remember { mutableStateOf(sharedViewModel.thisPhone.borderVert) }
    var borderHor by remember { mutableStateOf(sharedViewModel.thisPhone.borderHor) }

    var timer by remember { mutableStateOf<Timer?>(null) }

    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }


    fun updatePhoneInfo () {
        // Send phone update only if the borders are not changed for 0.5 seconds

        // Cancel existing timer if exists
        timer?.cancel()

        // Create a new timer
        timer = Timer()

        timer?.schedule(500){
            sharedViewModel.sendPhoneInfo()
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            detectDragGestures(onDragStart = { offset ->
                initialPosition.value = offset
                isDragging = true

            },
                onDragEnd = {
                    if (isDragging) {
                        endPosition.value = Offset(
                            initialPosition.value.x + dragX.toFloat(),
                            initialPosition.value.y + dragY.toFloat()
                        )
                        Log.d("DRAG", "End")

                        var swipe = Swipe(
                            initialPosition.value,
                            endPosition.value,
                            sharedViewModel.thisPhone
                        )
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

        }) {

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

        statusBar(sharedViewModel.infoText)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            if (!sharedViewModel.isConnected) {
                ElevatedButton(
                    onClick = {
                        if (!sharedViewModel.isWifiEnabled) {
                            wifiPopup(context)
                        }
                        if (!sharedViewModel.isLocationEnabled) {
                            locationPopup(context)
                            sharedViewModel.isLocationEnabled = isLocationEnabled(context)
                        }

                        if(sharedViewModel.isWifiEnabled && sharedViewModel.isLocationEnabled){
                            if (!sharedViewModel.isDiscovering) {
                                sharedViewModel.isDiscovering = true
                                connection.startPeerDiscovery()
                            }
                        }
                        else{
                            Toast.makeText(context, "Please enable WiFi and Location", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .padding(top = 16.dp, bottom = 20.dp)
                        .align(alignment = Alignment.CenterHorizontally)
                ) {
                    Text(text = "Discover Peers")
                }

                sharedViewModel.peerList?.let { peers ->
                    LazyColumn {
                        itemsIndexed(peers.deviceList.toList()) { index, peer ->
                            ListItem(
                                headlineContent = { Text("${peer.deviceName}") },
                                trailingContent = {
                                    Text("Connect")
//                                    Icon(
//                                        Icons.Filled.PlayArrow,
//                                        contentDescription = "Localized description",
//                                    )
                                },

                                modifier = Modifier.clickable {
                                    // Handle the click event, initiate connection to the selected peer
                                    connection.connectToPeer(peer)
                                })
                            HorizontalDivider()
                        }
                    }

                }

            } else {
                Spacer(modifier = Modifier.weight(1f))


                Row (
                    modifier = Modifier
                        .padding(top = 40.dp, bottom = 20.dp),
                )
                {
                    ExtendedFloatingActionButton(
                        onClick = { launcher.launch("image/*") },
                        icon = { Icon(Icons.Filled.Add, "Localized description") },
                        text = { Text(text = "Select image") },
                    )

                    Spacer(Modifier.weight(1f))

                    FloatingActionButton(
                        onClick = {
                            showBottomSheet = true
                        },
                    ){
                        Icon(Icons.Filled.Settings, contentDescription = "")
                    }
                }

            }


            if (showBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = {
                        showBottomSheet = false
                    },
                    sheetState = sheetState
                ) {
                    // Sheet content
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
                        numberSelect(
                            borderVert,
                            "Vertical",
                            {
                                if(borderVert in 0.0..9.5){
                                    borderVert += 0.5
                                    sharedViewModel.thisPhone.borderVert = borderVert
                                }
                            },
                            {
                                if(borderVert >= 0.5){
                                    borderVert -= 0.5
                                    sharedViewModel.thisPhone.borderVert = borderVert
                                }
                            }
                        )

                        numberSelect(
                            borderHor,
                            "Horizontal",
                            {
                                if(borderHor in 0.0..9.5){
                                    borderHor += 0.5
                                    sharedViewModel.thisPhone.borderHor = borderHor

                                    updatePhoneInfo()
                                }
                            },
                            {
                                if(borderHor >= 0.5){
                                    borderHor -= 0.5
                                    sharedViewModel.thisPhone.borderHor = borderHor

                                    updatePhoneInfo()
                                }
                            }
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
                        },
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                    ) {
                        Text(text = "Disconnect")
                    }
                }
            }

        }

    }

}






