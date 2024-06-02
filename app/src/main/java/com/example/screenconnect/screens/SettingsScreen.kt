package com.example.screenconnect.screens

import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Close
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.screenconnect.R
import com.example.screenconnect.components.BulletList
import com.example.screenconnect.components.numberSelect
import com.example.screenconnect.components.statusBar
import com.example.screenconnect.models.Swipe
import com.example.screenconnect.network.Connection
import com.example.screenconnect.util.getRealPathFromUri
import com.example.screenconnect.util.isLocationEnabled
import com.example.screenconnect.util.locationPopup
import com.example.screenconnect.util.wifiPopup
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun SettingsScreen(navController: NavController, sharedViewModel: SharedViewModel, connection: Connection) {

    val context = LocalContext.current

    if(sharedViewModel.showImage){
        sharedViewModel.showImage = false
        navController.navigate(Screen.SharedScreen.route)
    }

    var dragX = 0.0
    var dragY = 0.0

    val initialPosition = remember { mutableStateOf(Offset.Zero) }
    val endPosition = remember { mutableStateOf(Offset.Zero) }
    var isDragging = false

    var borderVert by remember { mutableStateOf(sharedViewModel.thisPhone.borderVert) }
    var borderHor by remember { mutableStateOf(sharedViewModel.thisPhone.borderHor) }

    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }

    val howToSheetState = rememberModalBottomSheetState()
    var showHowTo by remember { mutableStateOf(false) }

    // Function for selecting image, from https://stackoverflow.com/questions/76447182/load-image-from-gallery-and-show-it-with-jetpack-compose
    val launcher = rememberLauncherForActivityResult(
        contract =
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        sharedViewModel.imageUri = uri
        val imagePath = uri?.let { getRealPathFromUri(it, context) }
        if(imagePath != null){
            val imageFile = File(imagePath)
            sharedViewModel.sendImage(imageFile)
            if(sharedViewModel.isGroupOwner){
                sharedViewModel.processReceivedImage(File(imagePath))
            }
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            // Detects swipe events on screen, https://developer.android.com/develop/ui/compose/touch-input/pointer-input/understand-gestures
            detectDragGestures(onDragStart = { offset ->
                initialPosition.value = offset
                isDragging = true

            },
                onDragEnd = {
                    // Drag event ended, send it to server
                    if (isDragging) {
                        endPosition.value = Offset(
                            initialPosition.value.x + dragX.toFloat(),
                            initialPosition.value.y + dragY.toFloat()
                        )

                        var swipe = Swipe(
                            initialPosition.value,
                            endPosition.value,
                            sharedViewModel.thisPhone
                        )
                        sharedViewModel.sendSwipe(swipe)

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
                        sharedViewModel.sendSwipe(swipe)
                        isDragging = false
                    }
                }
            }
        }) {

        statusBar(sharedViewModel.infoText)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            if (!sharedViewModel.isConnected) {
                // Home screen
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

                Box(modifier = Modifier
                    .paint(painterResource(id = R.drawable.logo)) )
                {
                    sharedViewModel.peerList?.let { peers ->
                        LazyColumn {
                            itemsIndexed(peers.deviceList.toList()) { index, peer ->
                                ListItem(
                                    headlineContent = { Text("${peer.deviceName}") },
                                    trailingContent = {
                                        Text("Connect")
                                    },

                                    modifier = Modifier.clickable {
                                        // Handle the click event, initiate connection to the selected peer
                                        connection.connectToPeer(peer)
                                    })
                                HorizontalDivider()
                            }
                        }

                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                ExtendedFloatingActionButton(
                    onClick = { showHowTo = true },
                    icon = { Icon(Icons.Filled.Info, "Localized description") },
                    text = { Text(text = "How to use") },
                    modifier = Modifier
                        .padding(top = 16.dp, bottom = 120.dp)
                        .align(alignment = Alignment.CenterHorizontally)
                )

                // Bottom sheets implemented by Android documentation https://developer.android.com/develop/ui/compose/components/bottom-sheets
                if (showHowTo) {
                    ModalBottomSheet(
                        onDismissRequest = {
                            showHowTo = false
                        },
                        sheetState = sheetState
                    ) {
                        Row {
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(
                                modifier = Modifier.padding(end = 10.dp),
                                onClick = {
                                    scope.launch { howToSheetState.hide() }.invokeOnCompletion {
                                        if (!howToSheetState.isVisible) {
                                            showHowTo = false
                                        }
                                    }
                                }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Close sheet")
                            }
                        }

                        BulletList(
                            items = listOf(
                                "To connect devices, click 'Discover Peers', then click on a device",
                                "To connect screens dp the shown action with two fingers",
                                "To disconnect screen, pull right from the left side of the screen (→)",
                                "To show options and navigation bar when in connected screen, pull up from the bottom of the screen (↑)",
                            ),
                            modifier = Modifier.padding(24.dp),
                            lineSpacing = 8.dp
                        )
                    }
                }


            } else {
                // If device is connected

                Image(
                    painter = painterResource(id = R.drawable.pinch),
                    contentDescription = "Ready to connect",
                    alpha = 0.4F,
                    modifier = Modifier
                        .padding(top = 80.dp)
                )

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


            // Bottom sheets implemented by Android documentation https://developer.android.com/develop/ui/compose/components/bottom-sheets
            if (showBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = {
                        showBottomSheet = false
                    },
                    sheetState = sheetState
                ) {
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
                            Icon(Icons.Outlined.Close, contentDescription = "Close sheet")
                        }
                    }

                    Row (modifier = Modifier.padding(bottom = 20.dp)) {
                        Spacer(modifier = Modifier.weight(1f))
                        Text("Change bezel size", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Spacer(modifier = Modifier.weight(1f))
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
                                }
                            },
                            {
                                if(borderHor >= 0.5){
                                    borderHor -= 0.5
                                    sharedViewModel.thisPhone.borderHor = borderHor
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






