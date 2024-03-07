package com.example.screenconnect.models

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Offset
import com.example.screenconnect.enums.Edge
import com.example.screenconnect.enums.SwipeType
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import java.lang.Integer.max
import java.lang.Math.abs
import java.time.Duration
import java.time.LocalTime

@Serializable
class VirtualScreen {
    var vHeight: Int = 0
    var vWidth: Int = 0
    var phones = mutableListOf<Phone>()

    var DPI: Int = 0

    var swipes = mutableListOf<Swipe>()

    val GAP = 67
    //val GAP = 134

    @RequiresApi(Build.VERSION_CODES.O)
    fun addSwipe(swipe: Swipe, hostChangeCallback: (Phone) -> Unit): Boolean{

        swipe.time = LocalTime.now()

        if(swipes.size == 0){
            swipes.add(swipe)
        }
        else{

            if(phones.size == 1 && phones[0].isHost){
                resetScreen()
            }

            if(swipes[0].phone.id == swipe.phone.id){
                swipes.clear()
                swipes.add(swipe)

                Log.d("ID CHECK", "Same phone")

                return false
            }

            var difference = Duration.between(swipes[0].time, swipe.time)

            if(difference.toMillis() > 500){
                swipes.clear()
                swipes.add(swipe)

                Log.d("TIME CHECK", "Difference too big")

                return false
            }

            swipes.add(swipe)

            for(swipe in swipes){
                if (swipe.edge == Edge.NONE){
                    swipes.clear()
                    return false
                }
            }

            if(phones.size == 2 && isInScreen(swipes[0].phone) && isInScreen(swipes[1].phone)){
                resetScreen()
            }

            if(phones.size == 0){
                addFirst(swipes[0].phone)
            }

            connectNewPhone()

            swipes.clear()

            for(phone in phones){
                if (phone.isHost){
                    hostChangeCallback(phone)
                }
            }

            return true
        }

        return false
    }

    fun connectNewPhone(){

        //Swipe from phone that's already in the screen
        var swipeA: Swipe
        //Swipe from new phone
        var swipeB: Swipe

        if(isInScreen(swipes[0].phone)){
            swipeA = swipes[0]
            swipeB = swipes[1]
        }
        else{
            swipeA = swipes[1]
            swipeB = swipes[0]
        }

        var phoneA = swipeA.phone
        var phoneB = swipeB.phone

        var screenConPoint : Position = phonePosToScreenPos(swipeA)
        var phoneConPoint : Position = swipeB.connectionPoint

        phoneB.rotation = getBRotation(swipeA, swipeB)

        Log.d("Rotation", phoneB.rotation.toString())

        var screenChange = getScreenChange(screenConPoint, swipeA, swipeB, phoneB)

        phoneB.position = screenChange

        phones.add(phoneB)

        if(DPI>phoneB.DPI){
            DPI = phoneB.DPI
        }

    }

    fun removePhone(phone: Phone){
        if(isInScreen(phone)){
            phones.remove(phone)
            recalculateScreen()
        }
    }

    private fun recalculateScreen(){
        vHeight = 0
        vWidth = 0

        for(phone in phones){
            var tempWidth = 0
            var tempHeight = 0

            if(abs(phone.rotation) == 90){
                tempWidth = phone.position.x + phone.height
                tempHeight = phone.position.y + phone.width
            }
            else{
                tempWidth = phone.position.x + phone.width
                tempHeight = phone.position.y + phone.height
            }

            if(tempWidth > vWidth){
                vWidth = tempWidth
            }
            if(tempHeight > vHeight){
                vHeight= tempHeight
            }
        }
    }

    fun addFirst(phone: Phone){

        phone.position = Position(0, 0)
        phone.rotation = 0
        phones.add(phone)

        DPI = phone.DPI
        vHeight = phone.height
        vWidth = phone.width
    }


    fun isInScreen(phone: Phone): Boolean{
        for (p in phones){
            if (phone.id.equals(p.id)){
                return true
            }
        }

        return false
    }

    fun phonePosToScreenPos(swipe: Swipe): Position{
        return Position(swipe.connectionPoint.x + swipe.phone.position.x, swipe.connectionPoint.y + swipe.phone.position.y)
    }

    fun getBRotation(swipeA: Swipe, swipeB: Swipe): Int{

        var edgeA = swipeA.edge
        var edgeB = swipeB.edge

        Log.d("EdgeA", swipeA.edge.toString())
        Log.d("EdgeB", swipeB.edge.toString())

        //Gets ordinal difference from edge Enum - (LEFT, RIGHT, TOP, BOTTOM, NONE)
        var diff = edgeA.ordinal - edgeB.ordinal

        Log.d("Rotation-DIFF", diff.toString())

        if(abs(diff) == 2){
            return swipeA.phone.rotation
        }
        else if (diff == 0){
            return swipeA.phone.rotation + 180
        }
        else if (diff == -1){
            return swipeA.phone.rotation + 90
        }
        else if (diff == 1){
            return swipeA.phone.rotation - 90
        }
        else if (diff == -3){
            return swipeA.phone.rotation - 90
        }
        else if (diff == 3){
            return swipeA.phone.rotation + 90
        }

        Log.d("Rotation-SET", "0")
        return 0
    }

    fun getScreenChange(screenConPoint: Position, swipeA: Swipe, swipeB: Swipe, phoneB: Phone): Position{

        var centerA = Position(swipeA.phone.width / 2, swipeA.phone.height / 2)
        var centerB = Position(swipeB.phone.width / 2, swipeB.phone.height / 2)
        Log.d("A Center", centerA.toString())
        Log.d("B Center", centerB.toString())

        var swipeAPos = swipeA.connectionPoint
        var swipeBPos = swipeB.connectionPoint

        Log.d("A Con p", swipeAPos.toString())
        Log.d("B Con p", swipeBPos.toString())

        //Center devices

        val changeCenter = centerB.centerBy(centerA)

        var change = Position(0, 0)

        var changeA = Position(swipeAPos.x - centerA.x, swipeAPos.y - centerA.y)
        var changeB = Position(centerB.x - swipeBPos.x, centerB.y - swipeBPos.y)

        Log.d("Change A", changeA.toString())
        Log.d("Change B", changeB.toString())

        change += changeCenter
        Log.d("Change + Center", change.toString())

        change += changeA
        Log.d("Change + ChangeA", change.toString())
        // ADD GAP

        when(swipeB.edge){
            Edge.LEFT -> changeB.x += GAP
            Edge.TOP -> changeB.y += GAP
            Edge.RIGHT -> changeB.x -= GAP
            Edge.BOTTOM -> changeB.y -= GAP
            Edge.NONE -> {}
        }

        Log.d("ChangeB with GAP", changeB.toString())

        var isLandscape = false

        // Relative rotation
        var rotation = phoneB.rotation - swipeA.phone.rotation

        Log.d("A Rotation", swipeA.phone.rotation.toString())
        Log.d("B Rotation", phoneB.rotation.toString())

        Log.d("Rotation", rotation.toString())


        if(abs(rotation) == 90){
            Log.d("Rotation calc", (centerB.y - centerB.x).toString())
//            change.x = changeA.x - (centerB.y - centerB.x)
//            change.y = changeA.y + (centerB.y - centerB.x)
            change.x = change.x - (centerB.y - centerB.x)
            change.y = change.y + (centerB.y - centerB.x)
            Log.d("Change after ROT", change.toString())

            changeB = changeB.rotate(rotation)

            Log.d("ChangeB after ROT", changeB.toString())

            isLandscape = true
        }

        if(abs(rotation) == 180){
            changeB = changeB.rotate(rotation)
            Log.d("ChangeB after ROT", changeB.toString())
        }

        changeB += changeCenter

        change += changeB

        Log.d("Change TOGETHER", change.toString())

        var posA = swipeA.phone.position
        Log.d("PhoneA", posA.toString())

        // B position relative to A
        var posB = Position(posA.x + change.x, posA.y + change.y)

        Log.d("PhoneB X", posB.x.toString())
        Log.d("PhoneB Y", posB.y.toString())
        Log.d("PhoneB isLandscape", isLandscape.toString())


        // Check if B goes outside screen, if it does, change screen
        if(isLandscape){
            if (posB.x < 0){
                // Add difference to screen width
                vWidth += -1* posB.x
                // Shift all phones in X axis by difference
                shiftPhones(Position(-1* posB.x, 0))
                posB.x = 0
            }
            if(posB.x + phoneB.height > vWidth){
                // Add difference to screen width
                vWidth += posB.x + phoneB.height - vWidth
            }

            if (posB.y < 0){
                // Add difference to screen height
                vHeight += -1* posB.y
                // Shift all phones in Y axis by difference
                shiftPhones(Position(0, -1* posB.y))
                posB.y = 0
            }
            if(posB.y + phoneB.width > vHeight){
                // Add difference to screen height
                vHeight += posB.y + phoneB.width - vHeight
            }

        }
        else{
            if (posB.x < 0){
                // Add difference to screen width
                vWidth += -1 * posB.x
                // Shift all phones in X axis by difference
                shiftPhones(Position(-1 * posB.x, 0))

                posB.x = 0
            }
            if(posB.x + phoneB.width > vWidth){
                // Add difference to screen width
                vWidth += posB.x + phoneB.width - vWidth
            }

            if (posB.y < 0){
                // Add difference to screen height
                vHeight += -1 * posB.y
                // Shift all phones in Y axis by difference
                shiftPhones(Position(0, -1 * posB.y))

                Log.d("POSB Y",  "test")

                posB.y = 0
            }
            if(posB.y + phoneB.height > vHeight){
                // Add difference to screen height
                vHeight += posB.y + phoneB.height - vHeight
            }
        }

        Log.d("Y",  posB.y.toString())

        return posB
    }

    private fun shiftPhones(change: Position){
        for(phone in phones){
            phone.position += change
        }
    }

    private fun resetScreen(){
        phones.clear()
        vHeight = 0
        vWidth = 0
        DPI = 0
    }

    fun isInScreenById(id : String): Boolean{
        phones.forEach {
            if(it.id == id){
                return true
            }
        }

        return false
    }

}