package com.example.screenconnect

import android.net.Uri
import androidx.compose.runtime.Composable

//@Composable
//fun Screen(phone: PhoneScreen, virtualScreen: VirtualScreen, imageUri: Uri) {
//
//}

sealed class Screen(val route: String){
    object MainScreen : Screen("main_Screen")
    object ViewScreen : Screen("view_Screen")
}