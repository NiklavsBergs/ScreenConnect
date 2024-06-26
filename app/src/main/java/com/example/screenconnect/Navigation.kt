package com.example.screenconnect

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.screenconnect.screens.SharedViewModel
import com.example.screenconnect.screens.Screen
import com.example.screenconnect.screens.SettingsScreen
import com.example.screenconnect.screens.SharedScreen
import com.example.screenconnect.network.Connection

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun Navigation(sharedViewModel: SharedViewModel, connection: Connection) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.SettingsScreen.route){
        composable(route = Screen.SettingsScreen.route){
            SettingsScreen(navController, sharedViewModel, connection)
        }

        composable(route = Screen.SharedScreen.route){
            SharedScreen(navController, sharedViewModel, connection)
        }
    }
}