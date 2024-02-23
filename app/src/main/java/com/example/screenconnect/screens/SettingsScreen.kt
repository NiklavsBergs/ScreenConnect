package com.example.screenconnect.screens

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.screenconnect.components.statusBar
import com.example.screenconnect.models.Swipe
import com.example.screenconnect.network.Connection
import com.example.screenconnect.util.getRealPathFromUri
import com.example.screenconnect.util.isLocationEnabled
import com.example.screenconnect.util.locationPopup
import com.example.screenconnect.util.wifiPopup
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
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

    Column(modifier = Modifier.fillMaxSize()
        .pointerInput(Unit){
            detectDragGestures( onDragStart = { offset ->
                initialPosition.value = offset
                isDragging = true
            }, onDragEnd = {
                if(isDragging){
                    endPosition.value = Offset(initialPosition.value.x + dragX.toFloat(), initialPosition.value.y +  dragY.toFloat())
                    Log.d("Drag", initialPosition.toString() + ", " + endPosition.toString())
                    initialPosition.value = Offset.Zero
                    endPosition.value = Offset.Zero

                    var swipe = Swipe(initialPosition.value, endPosition.value, sharedViewModel.thisPhone)

                    dragX = 0.0
                    dragY = 0.0

                    isDragging = false
                }

            }){ change, dragAmount ->
                change.consume()
                dragX += dragAmount.x
                dragY += dragAmount.y

                val newPositionX = initialPosition.value.x + dragX.toFloat()
                val newPositionY = initialPosition.value.y + dragY.toFloat()
//                val screenWidth = // Get screen width
//                val screenHeight = // Get screen height

                if (newPositionX >= 10 && newPositionX <= sharedViewModel.thisPhone.width-10 &&
                    newPositionY >= 10 && newPositionY <= sharedViewModel.thisPhone.height-10) {
                    // If position is within bounds, continue drag
                } else {
                    // Position is outside bounds, end drag event
                    endPosition.value = Offset(newPositionX, newPositionY)
                    dragX = 0.0
                    dragY = 0.0
                    isDragging = false

                    var swipe = Swipe(initialPosition.value, endPosition.value, sharedViewModel.thisPhone)

                    sharedViewModel.sendSwipe(swipe)

                    Log.d("Drag", initialPosition.toString() + ", " + endPosition.toString())
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
            val imageFile = imagePath?.let { File(it) }
            sharedViewModel.sendImage(imageFile!!)
            if(sharedViewModel.isGroupOwner){
                sharedViewModel.processReceivedImage(File(imagePath))
            }
        }

        statusBar(sharedViewModel.infoText)

        //Text(text = sharedViewModel.infoText)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

//            if (sharedViewModel.isGroupOwner && sharedViewModel.isConnected) {
//                Text(
//                    text = "Host",
//                    modifier = Modifier.padding(top = 16.dp)
//                )
//            } else {
//                Text(
//                    text = if (sharedViewModel.isConnected) "Connected to: ${sharedViewModel.connectedDeviceName}" else "Not connected",
//                    modifier = Modifier.padding(top = 16.dp)
//                )
//            }

            if (!sharedViewModel.isConnected) {
                Button(
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
                    modifier = Modifier.padding(top = 16.dp)
                        .align(alignment = Alignment.CenterHorizontally)
                ) {
                    Text(text = "Discover Peers")
                }

                sharedViewModel.peerList?.let { peers ->
                    LazyColumn {
                        itemsIndexed(peers.deviceList.toList()) { index, peer ->
                            Text(
                                text = "Device $index: ${peer.deviceName}",
                                modifier = Modifier.clickable {
                                    // Handle the click event, initiate connection to the selected peer
                                    connection.connectToPeer(peer)
                                })
                        }
                    }
                }

            } else {
                Button(
                    onClick = {
                        connection.disconnect()
                    },
                    modifier = Modifier.padding(top = 16.dp)
                        .align(alignment = Alignment.CenterHorizontally)
                ) {
                    Text(text = "Disconnect")
                }

                Button(onClick = {
                    launcher.launch("image/*")
                },
                    modifier = Modifier.align(alignment = Alignment.CenterHorizontally)) {
                    Text(text = "Select image")
                }
            }



//            TextField(
//                value = sharedViewModel.text,
//                onValueChange = {
//                    sharedViewModel.text = it
//                    sharedViewModel.sendText(sharedViewModel.text)
//
//                },
//                label = { Text("Shared text") }
//            )
//
//            Text(
//                text = "VirtualHeight: ${sharedViewModel.virtualHeight}",
//                modifier = Modifier.padding(top = 16.dp)
//            )
//            Text(
//                text = "VirtualWidth: ${sharedViewModel.virtualWidth}",
//                modifier = Modifier.padding(top = 16.dp)
//            )
//            Text(
//                text = "ThisPhone: ${sharedViewModel.phoneNr}",
//                modifier = Modifier.padding(top = 16.dp)
//            )

            //Image select and display


//                    AsyncImage(
//                        model = sharedViewModel.imageUri,
//                        contentDescription = null,
//                        modifier = Modifier
//                            .padding(4.dp)
//                            .width(100.dp)
//                            .height(100.dp)
//                            .clip(RoundedCornerShape(12.dp)),
//                        contentScale = ContentScale.Crop,
//                    )



//                    Button(onClick = {
//                        sharedViewModel.sendPhoneInfo()
//                    }) {
//                        Text(text = "send info")
//                    }

//            Button(
//                onClick = {
//
//                },
//                modifier = Modifier.padding(top = 16.dp)
//            ) {
//                Text(text = "Show screen")
//            }
        }
    }

}





