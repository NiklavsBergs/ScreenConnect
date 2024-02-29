package com.example.screenconnect.models

import android.util.Log
import androidx.compose.ui.geometry.Offset
import kotlinx.serialization.Serializable
import java.lang.Integer.max

@Serializable
class VirtualScreen {
    var vHeight: Int = 0
    var vWidth: Int = 0
    var phones = mutableListOf<Phone>()

    var DPI: Int = 0

    var phoneCounter = 1

    var swipes = mutableListOf<Swipe>()

    val GAP = 67

    fun addSwipe(swipe: Swipe): Phone?{

        if(swipes.size == 0){
            swipes.add(swipe)
        }
        else{
            var phoneA = swipes[0].phone
            var phoneB = swipe.phone

            var aboveA = swipes[0].connectionPoint.y.toInt()
            var aboveB = swipe.connectionPoint.y.toInt()
            if(aboveA>aboveB){
                phoneA.locationY = 0
                phoneB.locationY = aboveA - aboveB
            }
            else{
                phoneA.locationY = aboveB - aboveA
                phoneB.locationY = 0
            }

            phoneB.locationX = phoneA.width + GAP


            var above = max(swipes[0].connectionPoint.y.toInt(), swipe.connectionPoint.y.toInt())
            var below = max(swipes[0].phone.height - swipes[0].connectionPoint.y.toInt(), swipe.phone.height - swipe.connectionPoint.y.toInt())

            vHeight = above + below

            vWidth = swipes[0].phone.width + GAP + swipe.phone.width

            swipes.clear()

            addPhone(phoneA)
            addPhone(phoneB)

            if(phoneA.isHost){
                return phoneA
            }

            if(phoneB.isHost){
                return phoneB
            }

            return null
        }



        return null
    }

//    fun addPhone(phone: Phone): Phone {
//        if(notAdded(phone)){
//
//            phones.add(phone)
//
//            if(phones.size == 1){
//                DPI = phone.DPI
//                Log.d("DPI", phone.DPI.toString())
//            }
//            else{
//                //phone.scale = DPI / phone.DPI
//                if(DPI>phone.DPI){
//                    DPI = phone.DPI
//                }
//            }
//
//            phone.locationX = vWidth + GAP
//
//            vHeight = Integer.max(vHeight, phone.height)
//            vWidth += phone.width + GAP
//
//            phone.nr = phoneCounter
//            phoneCounter++
//            Log.d("V-SCREEN", "Phone added")
//            Log.d("V-SCREEN", "$vHeight, $vWidth")
//            return phone
//        }
//        else{
//            var existingPhone = findPhone(phone)
//
//            //change necesarry values
//            Log.d("V-SCREEN", "Phone already added to screen")
//
//            return existingPhone
//        }
//
//
//    }

    fun addPhone(phone: Phone) {
        if (notAdded(phone)) {
            phones.add(phone)
        }
        else{
            var phoneAdded = findPhone(phone)
            phoneAdded.locationX = phone.locationX
            phoneAdded.locationY = phone.locationY
        }
        if(DPI == 0){
            DPI = phone.DPI
        }
        else if(DPI>phone.DPI){
            DPI = phone.DPI
        }
    }

    fun notAdded(phone: Phone):Boolean{
        var notIn:Boolean = true

        for (p in phones){
            if (phone.id.equals(p.id)){
                notIn = false
            }
        }

        return notIn
    }

    fun findPhone(phone: Phone): Phone {
        lateinit var foundPhone : Phone
        for (p in phones){
            if (phone.id.equals(p.id)){
                foundPhone  = p
            }
        }

        return foundPhone
    }

    fun phonePosToScreenPos(connectionPoint: Offset): Offset{

        return Offset(0F, 0F)
    }

}