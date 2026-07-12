package com.jasc.jascbattlechess.domain

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class JascBluetoothManager(private val adapter: BluetoothAdapter?) {

    private val NAME = "JascBattleChess"
    private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    var activeSocket: BluetoothSocket? = null
        private set

    val isConnected: Boolean
        get() = activeSocket?.isConnected == true

    @SuppressLint("MissingPermission")
    suspend fun startServer(onMessageReceived: (String) -> Unit): BluetoothSocket? = withContext(Dispatchers.IO) {
        if (adapter == null) return@withContext null
        var serverSocket: BluetoothServerSocket? = null
        try {
            serverSocket = adapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID)
            val socket = serverSocket?.accept()
            activeSocket = socket
            socket
        } catch (e: IOException) {
            Log.e("BluetoothManager", "Error en el socket del servidor al aceptar conexión", e)
            null
        } finally {
            try { serverSocket?.close() } catch (_: Exception) {}
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun connectToServer(device: BluetoothDevice): BluetoothSocket? = withContext(Dispatchers.IO) {
        if (adapter == null) return@withContext null
        try {
            val clientSocket = device.createRfcommSocketToServiceRecord(MY_UUID)
            adapter.cancelDiscovery() // Operación pesada, siempre cancelar antes de llamar a connect()
            clientSocket.connect()
            activeSocket = clientSocket
            clientSocket
        } catch (e: IOException) {
            Log.e("BluetoothManager", "Fallo crítico en la conexión del cliente remoto", e)
            null
        }
    }

    suspend fun listenForMessages(socket: BluetoothSocket, onMessageReceived: (String) -> Unit) = withContext(Dispatchers.IO) {
        val input: InputStream = try {
            socket.inputStream
        } catch (e: IOException) {
            Log.e("BluetoothManager", "No se pudo obtener el flujo de entrada de datos", e)
            return@withContext
        }

        val buffer = ByteArray(1024)
        // Usamos una bandera de control limpia para el bucle en segundo plano
        while (socket.isConnected) {
            try {
                val bytes = input.read(buffer)
                if (bytes > 0) {
                    val mensaje = String(buffer, 0, bytes).trim()
                    Log.d("BluetoothManager", "Mensaje crudo recibido del socket: $mensaje")

                    // Despachamos a la UI de forma completamente asíncrona sin bloquear el canal IO
                    withContext(Dispatchers.Main) {
                        onMessageReceived(mensaje)
                    }
                } else if (bytes == -1) {
                    break
                }
            } catch (e: IOException) {
                Log.e("BluetoothManager", "Tubería de lectura rota o cerrada", e)
                break
            }
        }
    }

    fun enviarMovimientoRemoto(movimiento: String) {
        val socket = activeSocket
        if (socket == null || !socket.isConnected) return
        try {
            val output: OutputStream = socket.outputStream
            output.write(movimiento.toByteArray())
            output.flush()
            Log.d("BluetoothManager", "Datos transmitidos con éxito por RFCOMM: $movimiento")
        } catch (e: IOException) {
            Log.e("BluetoothManager", "Fallo al escribir en el canal de salida", e)
        }
    }

    fun closeConnection() {
        val socketToClose = activeSocket
        activeSocket = null
        if (socketToClose != null) {
            try {
                socketToClose.close()
                Log.d("BluetoothManager", "Canales de comunicación RFCOMM liberados con éxito")
            } catch (e: IOException) {
                Log.e("BluetoothManager", "Error al cerrar el socket activo", e)
            }
        }
    }
}