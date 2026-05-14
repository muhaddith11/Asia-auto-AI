package com.car.voicecontrol

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

class BluetoothCarControl(private val context: Context) {

    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    var isConnected = false
        private set

    var onConnectionChanged: ((Boolean, String) -> Unit)? = null

    fun getPairedDevices(): List<BluetoothDevice> {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED) return emptyList()
        return bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
    }

    suspend fun connect(device: BluetoothDevice): Boolean = withContext(Dispatchers.IO) {
        try {
            disconnect()
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) return@withContext false

            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket?.connect()
            outputStream = socket?.outputStream
            isConnected = true
            onConnectionChanged?.invoke(true, device.name ?: "Unknown")
            true
        } catch (e: IOException) {
            isConnected = false
            onConnectionChanged?.invoke(false, e.message ?: "Connection failed")
            false
        }
    }

    suspend fun sendCommand(code: String) {
        withContext(Dispatchers.IO) {
            try {
                if (code.isNotEmpty() && isConnected) {
                    outputStream?.write((code + "\n").toByteArray())
                    outputStream?.flush()
                }
            } catch (e: IOException) {
                isConnected = false
                onConnectionChanged?.invoke(false, "Connection lost")
            }
        }
    }

    fun disconnect() {
        try {
            outputStream?.close()
            socket?.close()
        } catch (_: IOException) {}
        isConnected = false
        outputStream = null
        socket = null
    }
}
