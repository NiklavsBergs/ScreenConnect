package com.example.screenconnect.screens

//@Composable
//fun Screen(phone: PhoneScreen, virtualScreen: VirtualScreen, imageUri: Uri) {
//
//}

sealed class Screen(val route: String){
    object SettingsScreen : Screen("settings_Screen")
    object SharedScreen : Screen("shared_Screen")
}