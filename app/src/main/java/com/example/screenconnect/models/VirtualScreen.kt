package com.example.screenconnect.models

import android.util.Log
import kotlinx.serialization.Serializable

@Serializable
class VirtualScreen {
    var vHeight: Int = 0
    var vWidth: Int = 0
    var phones = mutableListOf<PhoneScreen>()

    var DPI: Int = 0

    var phoneCounter = 1

    fun addPhone(phone: PhoneScreen): PhoneScreen {
        if(notAdded(phone)){
            val GAP = 100

            phones.add(phone)

            if(phones.size == 1){
                DPI = phone.DPI
                Log.d("DPI", phone.DPI.toString())
            }
            else{
                //phone.scale = DPI / phone.DPI
                if(DPI>phone.DPI){
                    DPI = phone.DPI
                }
            }

            phone.locationX = vWidth + GAP

            vHeight = Integer.max(vHeight, phone.height)
            vWidth += phone.width + GAP

            phone.nr = phoneCounter
            phoneCounter++
            Log.d("V-SCREEN", "Phone added")
            Log.d("V-SCREEN", "$vHeight, $vWidth")
            return phone
        }
        else{
            var existingPhone = findPhone(phone)

            //change necesarry values
            Log.d("V-SCREEN", "Phone already added to screen")

            return existingPhone
        }


    }

    fun notAdded(phone: PhoneScreen):Boolean{
        var notIn:Boolean = true

        for (p in phones){
            if (phone.id.equals(p.id)){
                notIn = false
            }
        }

        return notIn
    }

    fun findPhone(phone: PhoneScreen): PhoneScreen {
        lateinit var foundPhone : PhoneScreen
        for (p in phones){
            if (phone.id.equals(p.id)){
                foundPhone  = p
            }
        }

        return foundPhone
    }
}