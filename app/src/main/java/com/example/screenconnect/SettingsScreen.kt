package com.example.screenconnect

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.provider.Settings
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.screenconnect.network.Connection
import com.example.screenconnect.screens.SharedViewModel
import com.example.screenconnect.util.wifiPopup
import kotlin.system.exitProcess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun settingsScreen(sharedViewModel: SharedViewModel, connection: Connection) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        if (sharedViewModel.isConnected) {
            sharedViewModel.infoText = "Connected"
        }

        Text(text = sharedViewModel.infoText)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(text = sharedViewModel.thisPhone.height.toString())
            Text(text = sharedViewModel.thisPhone.width.toString())

            if (sharedViewModel.isWifiP2pEnabled) {
                Text(text = "Wi-Fi Direct is enabled")
            } else {
                Text(text = "Wi-Fi Direct is not enabled")
            }

            if (sharedViewModel.isGroupOwner && sharedViewModel.isConnected) {
                Text(
                    text = "Host",
                    modifier = Modifier.padding(top = 16.dp)
                )
            } else {
                Text(
                    text = if (sharedViewModel.isConnected) "Connected to: ${sharedViewModel.connectedDeviceName}" else "Not connected",
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            if (!sharedViewModel.isConnected) {
                Button(
                    onClick = {
                        if (!sharedViewModel.isWifiP2pEnabled) {
                            wifiPopup(context)
                        } else {
                            if (!sharedViewModel.isDiscovering) {
                                sharedViewModel.isDiscovering = true
                                connection.startPeerDiscovery()
                            }
                        }
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(text = "Discover Peers")
                }
            } else {
                Button(
                    onClick = {
                        connection.disconnect()
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(text = "Disconnect")
                }
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

            TextField(
                value = sharedViewModel.text,
                onValueChange = {
                    sharedViewModel.text = it
                    sharedViewModel.sendText(sharedViewModel.text)

                },
                label = { Text("Shared text") }
            )

            Text(
                text = "VirtualHeight: ${sharedViewModel.virtualHeight}",
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = "VirtualWidth: ${sharedViewModel.virtualWidth}",
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = "ThisPhone: ${sharedViewModel.phoneNr}",
                modifier = Modifier.padding(top = 16.dp)
            )

            //Image select and display
//                    val launcher = rememberLauncherForActivityResult(
//                        contract =
//                        ActivityResultContracts.GetContent()
//                    ) { uri: Uri? ->
//                        imageUri = uri
//                        val imagePath = uri?.let { getRealPathFromUri(it) }
//                        imageUri?.let { sendImage(it) }
//                    }
//
//                    AsyncImage(
//                        model = imageUri,
//                        contentDescription = null,
//                        modifier = Modifier
//                            .padding(4.dp)
//                            .fillMaxHeight().width(100.dp)
//                            .clip(RoundedCornerShape(12.dp)),
//                        contentScale = ContentScale.Crop,
//                    )
//                    Button(onClick = {
//                        launcher.launch("image/*")
//                    }) {
//                        Text(text = "select image")
//                    }

            Button(
                onClick = {

                },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(text = "Show screen")
            }
        }
    }
}

//fun wifiPopup(context: Context){
//    val builder = AlertDialog.Builder(
//        context
//    )
//    builder.setTitle("Screen Connect")
//    builder.setMessage("WiFi is off, but is needed for this application. Do you want to turn it on?")
//    builder.setPositiveButton(
//        "Enable WiFi"
//    ) { dialogInterface, i ->
//        context.startActivity(
//            Intent(
//                Settings.ACTION_WIFI_SETTINGS
//            )
//        )
//    }
//    builder.setNegativeButton(
//        "Exit"
//    ) { dialogInterface, i ->
//        context.startActivity(
//            exitProcess(0)
//        )
//    }
//    builder.create().show()
//}