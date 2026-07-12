package com.jasc.jascbattlechess.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jasc.jascbattlechess.R
import com.jasc.jascbattlechess.data.*
import com.jasc.jascbattlechess.ui.ModoJuego
import com.jasc.jascbattlechess.domain.ChessAI
import com.jasc.jascbattlechess.domain.CombatRules
import com.jasc.jascbattlechess.domain.MoveValidator
import com.jasc.jascbattlechess.domain.JascBluetoothManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

enum class BluetoothState {
    DESCONECTADO, ESCUCHANDO, CONECTANDO, CONECTADO
}

class BoardViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    private val _boardState = MutableStateFlow(BoardState(pieces = crearPiezasIniciales()))
    val boardState = _boardState.asStateFlow()

    private val _historial = mutableListOf<List<PieceState>>()

    var nivelIA by mutableStateOf(NivelIA.NORMAL)
    var modoJuegoActivo by mutableStateOf(ModoJuego.CONTRA_IA)

    // --- 📡 Configuración de Bluetooth Delegada ---
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager.adapter

    private val jascBluetoothManager = JascBluetoothManager(adapter)

    var estadoBluetooth by mutableStateOf(BluetoothState.DESCONECTADO)
        private set

    var miEquipoNet by mutableStateOf<Team?>(null)
    private var connectionJob: Job? = null

    // --- 🎵 Funciones de sonido ---
    private fun playSound(resId: Int) {
        try {
            MediaPlayer.create(context, resId).apply {
                setOnCompletionListener { release() }
                start()
            }
        } catch (e: Exception) {
            Log.e("Audio", "Error al reproducir sonido", e)
        }
    }

    private fun playMove() = playSound(R.raw.move)
    private fun playCapture() = playSound(R.raw.capture)
    private fun playMate() = playSound(R.raw.mate)
    private fun playVictoria() = playSound(R.raw.victoria)
    private fun playKnight() = playSound(R.raw.knight)

    // --- 🏆 Marcadores ---
    var puntosJugadorTotal by mutableStateOf(0)
        private set
    var puntosIATotal by mutableStateOf(0)
        private set
    var puntosCapturasJugador by mutableStateOf(0)
        private set
    var puntosCapturasIA by mutableStateOf(0)
        private set

    // --- 🌐 Gestión de Red Bluetooth ---

    @SuppressLint("MissingPermission")
    fun iniciarServidor(onMessageReceived: (String) -> Unit) {
        if (adapter == null) return
        connectionJob?.cancel()

        // 1. Forzamos de inmediato el bando local y el mensaje de estado del torneo
        miEquipoNet = Team.BLANCAS
        _boardState.update { it.copy(mensajeEstado = "Tu turno (Blancas)") }

        connectionJob = viewModelScope.launch(Dispatchers.Main) {
            estadoBluetooth = BluetoothState.ESCUCHANDO
            val socket = jascBluetoothManager.startServer(onMessageReceived)
            if (socket != null) {
                estadoBluetooth = BluetoothState.CONECTADO
                jascBluetoothManager.listenForMessages(socket) { mensaje ->
                    procesarMovimientoRemoto(mensaje)
                    onMessageReceived(mensaje)
                }
                desconectarBluetooth()
            } else {
                estadoBluetooth = BluetoothState.DESCONECTADO
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun conectarAConversor(device: BluetoothDevice, onMessageReceived: (String) -> Unit) {
        if (adapter == null) return
        connectionJob?.cancel()

        // 2. Forzamos de inmediato el bando de las negras para activar el modo espejo en la UI
        miEquipoNet = Team.NEGRO
        _boardState.update { it.copy(mensajeEstado = "Turno del rival...") }

        connectionJob = viewModelScope.launch(Dispatchers.Main) {
            estadoBluetooth = BluetoothState.CONECTANDO
            val socket = jascBluetoothManager.connectToServer(device)
            if (socket != null) {
                estadoBluetooth = BluetoothState.CONECTADO
                jascBluetoothManager.listenForMessages(socket) { mensaje ->
                    procesarMovimientoRemoto(mensaje)
                    onMessageReceived(mensaje)
                }
                desconectarBluetooth()
            } else {
                estadoBluetooth = BluetoothState.DESCONECTADO
            }
        }
    }

    private fun procesarMovimientoRemoto(mensaje: String) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val partes = mensaje.split("->")
                if (partes.size == 2) {
                    val orig = partes[0].split(",")
                    val dest = partes[1].split(",")

                    // 🟢 LEER COORDENADAS ABSOLUTAS (Sin restar 7)
                    // Ambos celulares deben registrar la pieza en la misma posición exacta de la matriz
                    val origenX = orig[0].trim().toInt()
                    val origenY = orig[1].trim().toInt()
                    val destinoX = dest[0].trim().toInt()
                    val destinoY = dest[1].trim().toInt()
// Dentro de tu BoardViewModel.kt añade:
                    var esDispositivoBuscador by mutableStateOf(false)
                    val origen = Position(origenX, origenY)
                    val destino = Position(destinoX, destinoY)

                    Log.d("Bluetooth", "Sincronizando jugada rival: $origen -> $destino")

                    // Ejecutamos el movimiento directamente indicando que es remoto
                    intentarMovimiento(origen, destino, esRemoto = true)
                }
            } catch (e: Exception) {
                Log.e("Bluetooth", "Error parseando mensaje remoto: $mensaje", e)
            }
        }
    }

    fun enviarMovimientoRemoto(movimiento: String) {
        viewModelScope.launch(Dispatchers.IO) {
            jascBluetoothManager.enviarMovimientoRemoto(movimiento)
        }
    }

    fun desconectarBluetooth() {
        connectionJob?.cancel()
        connectionJob = null
        estadoBluetooth = BluetoothState.DESCONECTADO
        miEquipoNet = null
        jascBluetoothManager.closeConnection()
    }

    override fun onCleared() {
        super.onCleared()
        desconectarBluetooth()
    }

    // --- ♟️ Lógica Base de Ajedrez ---

    private fun crearPiezasIniciales(): List<PieceState> {
        val piezas = mutableListOf<PieceState>()
        for (i in 0..7) {
            piezas.add(PieceState("pP$i", PieceType.PEON, Team.BLANCAS, Position(6, i)))
            piezas.add(PieceState("pN$i", PieceType.PEON, Team.NEGRO, Position(1, i)))
        }
        val orden = listOf(
            PieceType.TORRE, PieceType.CABALLO, PieceType.ALFIL,
            PieceType.REINA, PieceType.REY, PieceType.ALFIL,
            PieceType.CABALLO, PieceType.TORRE
        )
        orden.forEachIndexed { i, tipo ->
            piezas.add(PieceState("Plata$i", tipo, Team.BLANCAS, Position(7, i)))
            piezas.add(PieceState("Negro$i", tipo, Team.NEGRO, Position(0, i)))
        }
        return piezas
    }

    fun siguientePartida() {
        _historial.clear()
        _boardState.value = BoardState(
            pieces = crearPiezasIniciales(),
            turn = Team.BLANCAS,
            esJaqueMate = false,
            esJaque = false,
            esTablas = false,
            mensajeEstado = "Tu turno"
        )
    }

    fun obtenerVideoRecompensa(puntos: Int): Int {
        val millar = (puntos / 1000) * 1000
        return when (millar) {
            1000 -> R.raw.video_felicitacion_1000
            2000 -> R.raw.video_felicitacion_2000
            3000 -> R.raw.video_felicitacion_3000
            4000 -> R.raw.video_felicitacion_4000
            5000 -> R.raw.video_felicitacion_5000
            6000 -> R.raw.video_felicitacion_6000
            7000 -> R.raw.video_felicitacion_7000
            8000 -> R.raw.video_felicitacion_8000
            9000 -> R.raw.video_felicitacion_9000
            10000 -> R.raw.video_felicitacion_10000
            else -> R.raw.video_felicitacion_default
        }
    }

    fun resetearJuego() {
        _historial.clear()
        puntosJugadorTotal = 0
        puntosIATotal = 0
        puntosCapturasJugador = 0
        puntosCapturasIA = 0
        _boardState.value = BoardState(
            pieces = crearPiezasIniciales(),
            turn = Team.BLANCAS,
            esJaqueMate = false,
            esJaque = false,
            esTablas = false,
            mensajeEstado = "Tu turno"
        )
    }

    fun intentarMovimiento(origen: Position, destino: Position, esRemoto: Boolean = false) {
        val currentState = _boardState.value
        if (currentState.esJaqueMate || currentState.esTablas) return

        val piezaAtacante = currentState.pieces.find { it.position == origen && it.health > 0 } ?: return
        if (piezaAtacante.team != currentState.turn) return

        // 🔐 Control de Sala Multijugador
        if (modoJuegoActivo == ModoJuego.MULTIJUGADOR && !esRemoto) {
            if (piezaAtacante.team != miEquipoNet) return
        }

        if (!MoveValidator.esMovimientoValido(piezaAtacante, destino, currentState.pieces)) return

        _historial.add(currentState.pieces.toList())
        var nuevasPiezas = currentState.pieces.toMutableList()

        // Enroque
        if (piezaAtacante.type == PieceType.REY && abs(destino.y - origen.y) == 2) {
            val esCorto = destino.y > origen.y
            val torreY = if (esCorto) 7 else 0
            val nuevaTorreY = if (esCorto) 5 else 3
            val torre = nuevasPiezas.find { it.type == PieceType.TORRE && it.position.y == torreY && it.team == piezaAtacante.team }
            if (torre != null) {
                nuevasPiezas.remove(torre)
                nuevasPiezas.add(torre.copy(position = Position(origen.x, nuevaTorreY), isMoved = true))
            }
        }

        val piezaDefensora = nuevasPiezas.find { it.position == destino && it.health > 0 }
        var seCambiaTurno = true

        if (piezaDefensora != null) {
            val dano = CombatRules.calcularDano(piezaAtacante, piezaDefensora)
            val nuevaVida = piezaDefensora.health - dano

            nuevasPiezas = nuevasPiezas.map {
                if (it.id == piezaDefensora.id) {
                    it.copy(
                        health = if (nuevaVida <= 0) 0 else nuevaVida,
                        position = if (nuevaVida <= 0) Position(-1, -1) else it.position
                    )
                } else it
            }.toMutableList()

            playCapture()

            if (nuevaVida > 0) {
                seCambiaTurno = false
            }
        } else {
            if (piezaAtacante.type == PieceType.CABALLO) playKnight() else playMove()
        }

        val piezaDefensoraPostAtaque = nuevasPiezas.find { it.id == (piezaDefensora?.id ?: "") }
        if (piezaDefensoraPostAtaque == null || piezaDefensoraPostAtaque.health <= 0) {
            nuevasPiezas = nuevasPiezas.map {
                if (it.id == piezaAtacante.id) it.copy(position = destino, isMoved = true) else it
            }.toMutableList()
        }

        // Promoción de Peón
        val piezasFinales = nuevasPiezas.map {
            if (it.type == PieceType.PEON && (it.position.x == 0 || it.position.x == 7)) {
                it.copy(type = PieceType.REINA)
            } else it
        }

        val siguienteTurno = if (seCambiaTurno) {
            if (currentState.turn == Team.BLANCAS) Team.NEGRO else Team.BLANCAS
        } else {
            currentState.turn
        }

        // 🛰️ Transmitir movimiento local por Bluetooth al oponente
        if (modoJuegoActivo == ModoJuego.MULTIJUGADOR && !esRemoto) {
            val comandoRed = "${origen.x},${origen.y}->${destino.x},${destino.y}"
            enviarMovimientoRemoto(comandoRed)
        }

        actualizarEstadoJuego(piezasFinales, siguienteTurno)
    }

    fun retrocederJugada() {
        if (_historial.isNotEmpty()) {
            val estadoAnterior = _historial.removeAt(_historial.size - 1)
            _boardState.update {
                it.copy(
                    pieces = estadoAnterior,
                    turn = if (it.turn == Team.BLANCAS) Team.NEGRO else Team.BLANCAS
                )
            }
        }
    }

    private fun actualizarEstadoJuego(piezas: List<PieceState>, siguienteTurno: Team) {
        val enJaque = MoveValidator.esJaque(siguienteTurno, piezas)
        val esMate = MoveValidator.esJaqueMate(siguienteTurno, piezas)
        val esAhogado = MoveValidator.esReyAhogado(siguienteTurno, piezas)

        val estadoAnterior = _boardState.value.pieces
        piezas.forEach { nuevaPieza ->
            val piezaAntes = estadoAnterior.find { it.id == nuevaPieza.id }
            if (piezaAntes != null && piezaAntes.health > 0 && nuevaPieza.health <= 0) {
                val valorPieza = when (nuevaPieza.type) {
                    PieceType.REY -> 100; PieceType.REINA -> 100; PieceType.TORRE -> 50
                    PieceType.CABALLO -> 34; PieceType.ALFIL -> 25; PieceType.PEON -> 20
                }
                if (nuevaPieza.team == Team.NEGRO) puntosCapturasJugador += valorPieza else puntosCapturasIA += valorPieza
            }
        }

        _boardState.update { currentState ->
            val piezasFinales = if (esMate) {
                piezas.map {
                    if (it.type == PieceType.REY && it.team == siguienteTurno) it.copy(health = 0) else it
                }
            } else piezas.toList()

            currentState.copy(
                pieces = piezasFinales,
                turn = siguienteTurno,
                esJaqueMate = esMate,
                esJaque = enJaque,
                esTablas = esAhogado,
                mensajeEstado = when {
                    esMate -> "¡JAQUE MATE! Gana: ${if (siguienteTurno == Team.BLANCAS) "NEGRO" else "BLANCAS"}"
                    esAhogado -> "¡TABLAS!"
                    enJaque -> "¡JAQUE!"
                    siguienteTurno == Team.BLANCAS -> {
                        if (modoJuegoActivo == ModoJuego.MULTIJUGADOR) {
                            if (miEquipoNet == Team.BLANCAS) "Tu turno (Blancas)" else "Turno del rival..."
                        } else "Tu turno"
                    }
                    else -> {
                        if (modoJuegoActivo == ModoJuego.MULTIJUGADOR) {
                            if (miEquipoNet == Team.NEGRO) "Tu turno (Negras)" else "Turno del rival..."
                        } else "IA pensando..."
                    }
                }
            )
        }

        if (esMate) {
            if (siguienteTurno == Team.NEGRO) puntosJugadorTotal += 100 else puntosIATotal += 100
            playMate()
        } else if (esAhogado) {
            playVictoria()
        }

        if (modoJuegoActivo == ModoJuego.CONTRA_IA && siguienteTurno == Team.NEGRO && !esMate && !esAhogado) {
            ejecutarJugadaIA()
        }
    }

    private fun ejecutarJugadaIA() {
        viewModelScope.launch {
            val delayTime = when (nivelIA) {
                NivelIA.PRINCIPIANTE -> 2000L
                NivelIA.FACIL -> 1500L
                NivelIA.NORMAL -> 600L
                NivelIA.AVANZADO -> 100L
            }
            delay(delayTime)
            val movimiento = withContext(Dispatchers.Default) {
                ChessAI.calcularMovimientoIA(_boardState.value.pieces, nivelIA)
            }
            if (movimiento != null) {
                intentarMovimiento(movimiento.first, movimiento.second, esRemoto = false)
            }
        }
    }
}