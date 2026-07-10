package com.jasc.jascbattlechess.viewmodel

import android.app.Application
import android.media.MediaPlayer
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jasc.jascbattlechess.R
import com.jasc.jascbattlechess.data.*
import com.jasc.jascbattlechess.domain.ChessAI
import com.jasc.jascbattlechess.domain.CombatRules
import com.jasc.jascbattlechess.domain.MoveValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class BoardViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    private val _boardState = MutableStateFlow(BoardState(pieces = crearPiezasIniciales()))
    val boardState = _boardState.asStateFlow()

    private val _historial = mutableListOf<List<PieceState>>()

    var nivelIA by mutableStateOf(NivelIA.NORMAL)

    // --- 🎵 Funciones de sonido (Usando MediaPlayer nativo) ---
    private fun playSound(resId: Int) {
        MediaPlayer.create(context, resId).apply {
            setOnCompletionListener { release() }
            start()
        }
    }

    private fun playMove() = playSound(R.raw.move)
    private fun playCapture() = playSound(R.raw.capture)
    private fun playMate() = playSound(R.raw.mate)
    private fun playVictoria() = playSound(R.raw.victoria)
    private fun playKnight() = playSound(R.raw.knight) // 🐴 Sonido exclusivo del caballo

    // --- 🏆 Puntaje acumulado del Torneo ---
    var puntosJugadorTotal by mutableStateOf(0)
        private set

    var puntosIATotal by mutableStateOf(0)
        private set

    // Guardan los puntos de piezas comidas acumulados en el torneo de forma persistente
    var puntosCapturasJugador by mutableStateOf(0)
        private set

    var puntosCapturasIA by mutableStateOf(0)
        private set

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

    /**
     * 🔄 Siguiente Partida
     * Limpia el tablero conservando intactos los acumuladores de puntos.
     */
    fun siguientePartida() {
        Log.d("JascChess", "Siguiente partida iniciada. Manteniendo puntajes anteriores.")
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

    /**
     * 🎬 Retorna el ID del recurso de video que corresponde según los millares de puntos.
     */
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

    /**
     * 🧹 Reset completo del juego
     * Devuelve absolutamente todos los marcadores a cero (0).
     */
    fun resetearJuego() {
        Log.d("JascChess", "Reset completo del juego y marcadores")
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

    fun intentarMovimiento(origen: Position, destino: Position) {
        val currentState = _boardState.value
        if (currentState.esJaqueMate || currentState.esTablas) return

        val piezaAtacante = currentState.pieces.find { it.position == origen && it.health > 0 } ?: return
        if (piezaAtacante.team != currentState.turn) return
        if (!MoveValidator.esMovimientoValido(piezaAtacante, destino, currentState.pieces)) return

        _historial.add(currentState.pieces.toList())
        var nuevasPiezas = currentState.pieces.toMutableList()

        // --- 1. Gestión del Enroque ---
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

        // --- 2. Gestión de Combate, Daño y Efectos de Sonido ---
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

            // Sonido de ataque/captura estándar
            playCapture()

            // Si la pieza defensora sobrevivió al impacto, el atacante no cambia su turno (sigue presionando)
            if (nuevaVida > 0) {
                seCambiaTurno = false
            }
        } else {
            // Movimiento a casilla vacía: validamos si es el caballo para usar su relincho exclusivo
            if (piezaAtacante.type == PieceType.CABALLO) {
                playKnight()
            } else {
                playMove()
            }
        }

        // --- 3. Desplazamiento Físico del Atacante ---
        val piezaDefensoraPostAtaque = nuevasPiezas.find { it.id == (piezaDefensora?.id ?: "") }
        if (piezaDefensoraPostAtaque == null || piezaDefensoraPostAtaque.health <= 0) {
            nuevasPiezas = nuevasPiezas.map {
                if (it.id == piezaAtacante.id) it.copy(position = destino, isMoved = true) else it
            }.toMutableList()
        }

        // --- 4. Coronación de Peones ---
        val piezasFinales = nuevasPiezas.map {
            if (it.type == PieceType.PEON && (it.position.x == 0 || it.position.x == 7)) {
                it.copy(type = PieceType.REINA)
            } else it
        }

        // --- 5. Cálculo y Transición del Siguiente Turno ---
        val siguienteTurno = if (seCambiaTurno) {
            if (currentState.turn == Team.BLANCAS) Team.NEGRO else Team.BLANCAS
        } else {
            currentState.turn
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

        // Evaluar si alguna pieza del estado anterior pasó a salud 0 en este procesamiento
        val estadoAnterior = _boardState.value.pieces
        piezas.forEach { nuevaPieza ->
            val piezaAntes = estadoAnterior.find { it.id == nuevaPieza.id }
            if (piezaAntes != null && piezaAntes.health > 0 && nuevaPieza.health <= 0) {
                val valorPieza = when (nuevaPieza.type) {
                    PieceType.REY -> 100
                    PieceType.REINA -> 100
                    PieceType.TORRE -> 50
                    PieceType.CABALLO -> 34
                    PieceType.ALFIL -> 25
                    PieceType.PEON -> 20
                }
                if (nuevaPieza.team == Team.NEGRO) {
                    puntosCapturasJugador += valorPieza
                } else {
                    puntosCapturasIA += valorPieza
                }
            }
        }

        _boardState.update { currentState ->
            val piezasFinales = if (esMate) {
                piezas.map {
                    if (it.type == PieceType.REY && it.team == siguienteTurno) {
                        it.copy(health = 0)
                    } else it
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
                    siguienteTurno == Team.BLANCAS -> "Tu turno"
                    else -> "IA pensando..."
                }
            )
        }

        // --- Puntaje por victoria de la partida ---
        if (esMate) {
            if (siguienteTurno == Team.NEGRO) {
                puntosJugadorTotal += 100
            } else {
                puntosIATotal += 100
            }
            playMate()
        } else if (esAhogado) {
            playVictoria()
        }

        if (siguienteTurno == Team.NEGRO && !esMate && !esAhogado) {
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
                intentarMovimiento(movimiento.first, movimiento.second)
            }
        }
    }
}