package com.example.screenconnect.models

import android.util.Log

class VirtualScreen {
    var vHeight: Int = 0
    var vWidth: Int = 0
    var phones = mutableListOf<PhoneScreen>()

    var phoneCounter = 1

    fun addPhone(phone: PhoneScreen): PhoneScreen {
        if(notAdded(phone)){
            phones.add(phone)

            phone.locationX = vWidth

            vHeight = Integer.max(vHeight, phone.height)
            vWidth += phone.width

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