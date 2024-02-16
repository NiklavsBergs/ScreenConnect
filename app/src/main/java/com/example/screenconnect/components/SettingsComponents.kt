package com.example.screenconnect.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun statusBar(info: String){

    var color = Color.Gray

    if(info == "Connected" || info == "Host"){
        color = Color(0xFF53FC2D)
    }
    else if (info == "Peers found" || info == "Discovery started" ){
        color = Color(0xFFFCE12D)
    }

    Box(modifier = Modifier
        .fillMaxWidth()
        .height(25.dp)
        .background(color),
        contentAlignment = Alignment.Center,)
    {
        Text(
            text = info,
            color = Color.White,
            style = TextStyle(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(2.dp),
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
        )
    }

}
