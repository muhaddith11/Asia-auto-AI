package com.car.voicecontrol.byd

import android.os.IBinder
import android.os.Parcel
import android.view.KeyEvent

object CommunicationBinder {

    const val BINDER_KEY = "CommunicationBinder"

    private const val TRANSACTION_dispatchKeyEvent = 4
    private const val TRANSACTION_setInt           = 20
    private const val TRANSACTION_startVehicle     = 21

    // Asosiy mashina buyrug'i
    fun setInt(binder: IBinder, deviceType: Int, eventType: Int, value: Int): Int {
        val data  = Parcel.obtain()
        val reply = Parcel.obtain()
        return try {
            data.writeInterfaceToken(BINDER_KEY)
            data.writeInt(deviceType)
            data.writeInt(eventType)
            data.writeInt(value)
            val success = binder.transact(TRANSACTION_setInt, data, reply, 0)
            if (success) reply.readInt() else -1
        } catch (e: Exception) {
            -1
        } finally {
            data.recycle()
            reply.recycle()
        }
    }

    // Media tugma yuborish
    fun dispatchKeyEvent(binder: IBinder, event: KeyEvent) {
        val data  = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(BINDER_KEY)
            event.writeToParcel(data, 0)
            binder.transact(TRANSACTION_dispatchKeyEvent, data, reply, 0)
        } finally {
            data.recycle()
            reply.recycle()
        }
    }
}
