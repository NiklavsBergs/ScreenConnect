package com.example.screenconnect.screens

sealed class Screen(val route: String){
    object SettingsScreen : Screen("settings_Screen")
    object SharedScreen : Screen("shared_Screen")
}