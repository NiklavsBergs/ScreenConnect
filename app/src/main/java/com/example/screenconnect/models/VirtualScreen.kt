package com.example.screenconnect.models

import android.util.Log
import com.example.screenconnect.enums.Edge
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.lang.Math.abs
import java.time.Duration
import java.time.LocalTime

@Serializable
class VirtualScreen {
    var height: Int = 0
    var width: Int = 0

    @Transient var phones = mutableListOf<Phone>()
    @Transient val DPI: Int = 400
    @Transient private var swipes = mutableListOf<Swipe>()
    @Transient val GAP = 67

    val mmToInch = 0.0393

    fun addSwipe(swipe: Swipe, hostChangeCallback: (Phone) -> Unit): Boolean{

        swipe.time = LocalTime.now()

        if(swipes.size == 0){
            swipes.add(swipe)

            return false
        }
        else{

            if(swipes[0].phone.id == swipe.phone.id){
                swipes.clear()
                swipes.add(swipe)

                Log.d("ID CHECK", "Both swipes are from the same phone")

                return false
            }

            val difference = Duration.between(swipes[0].time, swipe.time)

            if(difference.toMillis() > 500){
                swipes.clear()
                swipes.add(swipe)

                Log.d("TIME CHECK", "Time difference too big")

                return false
            }

            swipes.add(swipe)

            for(swipe in swipes){
                if (swipe.edge == Edge.NONE){
                    swipes.clear()
                    return false
                }
            }

            if(phones.size == 1){
                resetScreen()
            }
            else if(phones.size == 2 && isInScreen(swipes[0].phone) && isInScreen(swipes[1].phone)){
                resetScreen()
            }

            if(phones.size == 0){
                addFirstPhone(swipes[0].phone)
            }

            connectNewPhone()

            swipes.clear()

            for(phone in phones){
                if (phone.isHost){
                    hostChangeCallback(phone)
                }
            }

            Log.d("SCREEN PHONES", phones.size.toString())

            return true
        }
    }

    private fun connectNewPhone(){

        //Swipe from phone that's already in the screen
        val swipeA: Swipe
        //Swipe from new phone
        val swipeB: Swipe

        if(isInScreen(swipes[0].phone)){
            swipeA = swipes[0]
            swipeB = swipes[1]
        }
        else{
            swipeA = swipes[1]
            swipeB = swipes[0]
        }

        val phoneB = swipeB.phone

        phoneB.rotation = getBRotation(swipeA, swipeB)

        Log.d("New Phone Rotation", phoneB.rotation.toString())

        recalculateScreen()

        phoneB.position = getBPositionAndUpdateScreen(swipeA, swipeB, phoneB)

        Log.d("New Phone Position", phoneB.position.toString())

        if(phoneB.isHost){
            Log.d("ADDD", phoneB.toString())
            Log.d("ADDD COPY", phoneB.copy().toString())
            phones.add(phoneB.copy())
        }
        else{
            phones.add(phoneB)
        }

        Log.d("SCREEN", "Phone Added ${phoneB.phoneName}")
        Log.d("SCREEN PHONE COUNT", phones.size.toString())
    }

    fun removePhone(phone: Phone){
        val index = phones.indexOfFirst { it.id == phone.id }
        if (index != -1) {
            phones.removeAt(index)
            recalculateScreen()
        }
    }

    fun updatePhone(updatedPhone: Phone, hostChangeCallback: (Phone) -> Unit): Boolean {
        val index = phones.indexOfFirst { it.id == updatedPhone.id }
        Log.d("UPDATE OLD", "${phones[index].borderHor}")
        Log.d("UPDATE NEW", "${updatedPhone.borderHor}")

        Log.d("UPDATE OLD P", "${phones[index]}")
        Log.d("UPDATE NEW P", "${updatedPhone}")

        if (index != -1) {

            val horChange = getGap(updatedPhone.borderHor - phones[index].borderHor)
            val vertChange = getGap(updatedPhone.borderVert - phones[index].borderVert)

            Log.d("UPDATE BEF", "${phones[index].borderHor}")
            phones[index].borderHor = updatedPhone.borderHor
            phones[index].borderVert = updatedPhone.borderVert

            var host: Phone? = null

            for(phone in phones){
                if (phone.position.x > updatedPhone.position.x){
                    phone.position.x += horChange
                }
                if (phone.position.y > updatedPhone.position.y){
                    phone.position.y += vertChange
                }

                if (phone.isHost){
                    host = phone
                }
            }

            recalculateScreen()

            if (host != null) {
                hostChangeCallback(host)
            }
            return true
        }
        return false
    }

    private fun recalculateScreen(){
        height = 0
        width = 0

        for(phone in phones){
            var tempWidth: Int
            var tempHeight: Int

            if(kotlin.math.abs(phone.rotation) == 90){
                tempWidth = phone.position.x + phone.height
                tempHeight = phone.position.y + phone.width
            }
            else{
                tempWidth = phone.position.x + phone.width
                tempHeight = phone.position.y + phone.height
            }

            if(tempWidth > width){
                width = tempWidth
            }
            if(tempHeight > height){
                height = tempHeight
            }
        }
    }

    fun addFirstPhone(phone: Phone){
        phone.position = Position(0, 0)
        phone.rotation = 0

        if (phone.isHost){
            phones.add(phone.copy())
        }
        else {
            phones.add(phone)
        }

        height = phone.height
        width = phone.width
    }


    private fun isInScreen(phone: Phone): Boolean{
        for (p in phones){
            if (phone.id == p.id){
                return true
            }
        }

        return false
    }

    private fun getBRotation(swipeA: Swipe, swipeB: Swipe): Int{

        val edgeA = swipeA.edge
        val edgeB = swipeB.edge

        Log.d("EdgeA", swipeA.edge.toString())
        Log.d("EdgeB", swipeB.edge.toString())

        //Gets ordinal difference from edge Enum - (LEFT, RIGHT, TOP, BOTTOM, NONE)
        // (Can't be NONE)
        val diff = edgeA.ordinal - edgeB.ordinal

        when (diff) {
            0 -> return swipeA.phone.rotation + 180
            -1, 3 -> return swipeA.phone.rotation + 90
            1, -3 -> return swipeA.phone.rotation - 90
        }

        return swipeA.phone.rotation
    }

    private fun getBPositionAndUpdateScreen(swipeA: Swipe, swipeB: Swipe, phoneB: Phone): Position{

        val centerA = Position(swipeA.phone.width / 2, swipeA.phone.height / 2)
        val centerB = Position(swipeB.phone.width / 2, swipeB.phone.height / 2)

        Log.d("A Center", centerA.toString())
        Log.d("B Center", centerB.toString())

        val swipeAPos = swipeA.connectionPoint
        val swipeBPos = swipeB.connectionPoint

        Log.d("A Con point", swipeAPos.toString())
        Log.d("B Con point", swipeBPos.toString())

        // Center devices
        var change = center(centerA, centerB)

        Log.d("Centered", change.toString())

        var changeA = Position(swipeAPos.x - centerA.x, swipeAPos.y - centerA.y)
        var changeB = Position(centerB.x - swipeBPos.x, centerB.y - swipeBPos.y)

        Log.d("Change A", changeA.toString())
        Log.d("Change B", changeB.toString())

        // Center of phone B is positioned on the connection point of phone A
        change += changeA
        Log.d("Change with A", change.toString())

        // ADD GAP
        Log.d("Change A Bef GAP", change.toString())
        when(swipeA.edge){
            Edge.LEFT -> change.x -= getGap(swipeA.phone.borderHor)
            Edge.TOP -> change.y -= getGap(swipeA.phone.borderVert)
            Edge.RIGHT -> change.x += getGap(swipeA.phone.borderHor)
            Edge.BOTTOM -> change.y += getGap(swipeA.phone.borderVert)
            Edge.NONE -> {}
        }
        Log.d("Change A after GAP", change.toString())

        Log.d("Change B Bef GAP", changeB.toString())
        when(swipeB.edge){
            Edge.LEFT -> changeB.x += getGap(swipeB.phone.borderHor)
            Edge.TOP -> changeB.y += getGap(swipeB.phone.borderVert)
            Edge.RIGHT -> changeB.x -= getGap(swipeB.phone.borderHor)
            Edge.BOTTOM -> changeB.y -= getGap(swipeB.phone.borderVert)
            Edge.NONE -> {}
        }
        Log.d("Change B after GAP", changeB.toString())

        // Relative rotation between both phones
        val rotation = phoneB.rotation - swipeA.phone.rotation

        if(abs(rotation) == 90){
            // Adjust B position with rotation
            change.x = change.x - (centerB.y - centerB.x)
            change.y = change.y + (centerB.y - centerB.x)
            Log.d("Change after ROT", change.toString())

            // Adjust B change with rotation
            changeB = changeB.rotate(rotation)
            Log.d("ChangeB after ROT", changeB.toString())
        }
        else if(abs(rotation) == 180){
            // Adjust B change with rotation
            changeB = changeB.rotate(rotation)
            Log.d("ChangeB after ROT", changeB.toString())
        }

        // Phone B position relative to phone A
        change += changeB

        Log.d("Change TOGETHER", change.toString())

        var posA = swipeA.phone.position
        Log.d("PhoneA", posA.toString())

        // Get phone B position in screen
        var posB = Position(posA.x + change.x, posA.y + change.y)

        Log.d("PhoneB", posB.toString())

        updateScreen(posB, phoneB)

        return posB
    }

    private fun getGap(gapMM : Double) : Int {
        Log.d("GAP MM", gapMM.toString())
        Log.d("CALC GAP", (gapMM * mmToInch * DPI).toInt().toString())
        return (gapMM * mmToInch * DPI).toInt()
    }

    private fun center (centerA : Position, centerB: Position) : Position {
        return Position((centerA.x - centerB.x), (centerA.y - centerB.y))
    }
    private fun updateScreen(posB: Position, phoneB: Phone){
        // Check if phone B goes outside screen, if it does, change screen
        if(kotlin.math.abs(phoneB.rotation) == 90){
            if (posB.x < 0){
                // Add difference to screen width
                width += -1* posB.x
                // Shift all phones in X axis by difference
                shiftPhones(Position(-1* posB.x, 0))
                posB.x = 0
            }
            if(posB.x + phoneB.height > width){
                // Add difference to screen width
                width += posB.x + phoneB.height - width
            }

            if (posB.y < 0){
                // Add difference to screen height
                height += -1* posB.y
                // Shift all phones in Y axis by difference
                shiftPhones(Position(0, -1* posB.y))

                posB.y = 0
            }
            if(posB.y + phoneB.width > height){
                // Add difference to screen height
                height += posB.y + phoneB.width - height
            }

        }
        else{
            if (posB.x < 0){
                // Add difference to screen width
                width += -1 * posB.x
                // Shift all phones in X axis by difference
                shiftPhones(Position(-1 * posB.x, 0))

                posB.x = 0
            }
            if(posB.x + phoneB.width > width){
                // Add difference to screen width
                width += posB.x + phoneB.width - width
            }

            if (posB.y < 0){
                // Add difference to screen height
                height += -1 * posB.y
                // Shift all phones in Y axis by difference
                shiftPhones(Position(0, -1 * posB.y))

                posB.y = 0
            }
            if(posB.y + phoneB.height > height){
                // Add difference to screen height
                height += posB.y + phoneB.height - height
            }
        }
    }

    private fun shiftPhones(change: Position){
        for(phone in phones){
            phone.position += change
        }
    }

    private fun resetScreen(){
        Log.d("SCREEN RESET", "Reset")

        phones.clear()
        height = 0
        width = 0
    }

    fun isInScreenById(id : String): Boolean{
        phones.forEach {
            if(it.id == id){
                return true
            }
        }

        return false
    }

    private fun getPhoneInScreen(phoneObj: Phone):Phone{
        for(phone in phones){
            if(phone.id == phoneObj.id){
                return phone
            }
        }
        return phoneObj
    }

    private fun getPhoneById(id : String): Any {
        for(phone in phones){
            if(phone.id == id){
                return phone
            }
        }
        return false
    }

}