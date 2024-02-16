package com.example.screenconnect.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.navigation.NavController
import com.example.screenconnect.network.Connection
import com.example.screenconnect.screens.SharedViewModel

@Composable
fun SharedScreen(navController: NavController, sharedViewModel: SharedViewModel) {

    Image(sharedViewModel.sharedImage.asImageBitmap(), contentDescription = "Bitmap Image")

}