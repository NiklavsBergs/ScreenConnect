package com.example.screenconnect.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.screenconnect.components.statusBar
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

    Column(modifier = Modifier.fillMaxSize()) {

        val launcher = rememberLauncherForActivityResult(
            contract =
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            sharedViewModel.imageUri = uri
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





