package com.jasc.jascbattlechess.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

class BoardViewModel : ViewModel() {

    private val _boardState = MutableStateFlow(BoardState(pieces = crearPiezasIniciales()))
    val boardState = _boardState.asStateFlow()

    private val _historial = mutableListOf<List<PieceState>>()

    // Configuración de Dificultad IA - Mantenemos la referencia correcta
    var nivelIA by mutableStateOf(NivelIA.NORMAL)

    private fun crearPiezasIniciales(): List<PieceState> {
        val piezas = mutableListOf<PieceState>()

        // Fila de peones: Plata abajo (6), Negro arriba (1)
        for (i in 0..7) {
            piezas.add(PieceState("pP$i", PieceType.PEON, Team.BLANCAS, Position(6, i)))
            piezas.add(PieceState("pN$i", PieceType.PEON, Team.NEGRO, Position(1, i)))
        }

        // Reina en su color
        val orden = listOf(PieceType.TORRE, PieceType.CABALLO, PieceType.ALFIL, PieceType.REINA, PieceType.REY, PieceType.ALFIL, PieceType.CABALLO, PieceType.TORRE)
        orden.forEachIndexed { i, tipo ->
            piezas.add(PieceState("Plata$i", tipo, Team.BLANCAS, Position(7, i)))
            piezas.add(PieceState("Negro$i", tipo, Team.NEGRO, Position(0, i)))
        }
        return piezas
    }

    fun resetearJuego() {
        Log.d("JascChess", "Reset completo del juego")
        _historial.clear()
        _boardState.value = BoardState(pieces = crearPiezasIniciales())
    }

    fun intentarMovimiento(origen: Position, destino: Position) {
        Log.d("JascChess", "INTENTO: Movimiento de $origen a $destino")
        val currentState = _boardState.value

        if (currentState.esJaqueMate || currentState.esTablas) return

        val piezaAtacante = currentState.pieces.find { it.position == origen }
        if (piezaAtacante == null) return

        if (piezaAtacante.team != currentState.turn) return

        if (!MoveValidator.esMovimientoValido(piezaAtacante, destino, currentState.pieces)) {
            Log.w("JascChess", "Movimiento inválido rechazado")
            return
        }

        // Guardamos historial
        _historial.add(currentState.pieces.toList())
        var nuevasPiezas = currentState.pieces.toMutableList()

        // Lógica de Enroque
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

        // Lógica de Captura Mejorada
        val piezaDefensora = nuevasPiezas.find { it.position == destino && it.health > 0 }
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

            if (nuevaVida > 0) {
                actualizarEstadoJuego(nuevasPiezas, currentState.turn)
                return
            }
        }

        // Mover atacante
        nuevasPiezas = nuevasPiezas.map {
            if (it.id == piezaAtacante.id) it.copy(position = destino, isMoved = true) else it
        }.toMutableList()

        // Coronación
        val piezasFinales = nuevasPiezas.map {
            if (it.type == PieceType.PEON && (it.position.x == 0 || it.position.x == 7)) {
                it.copy(type = PieceType.REINA)
            } else it
        }

        actualizarEstadoJuego(piezasFinales, if (currentState.turn == Team.BLANCAS) Team.NEGRO else Team.BLANCAS)
    }

    fun retrocederJugada() {
        if (_historial.isNotEmpty()) {
            val estadoAnterior = _historial.removeAt(_historial.size - 1)
            _boardState.update { it.copy(pieces = estadoAnterior, turn = if (it.turn == Team.BLANCAS) Team.NEGRO else Team.BLANCAS) }
        }
    }

    private fun actualizarEstadoJuego(piezas: List<PieceState>, siguienteTurno: Team) {
        val enJaque = MoveValidator.esJaque(siguienteTurno, piezas)
        val esMate = MoveValidator.esJaqueMate(siguienteTurno, piezas)
        val esAhogado = MoveValidator.esReyAhogado(siguienteTurno, piezas)

        _boardState.update { currentState ->
            currentState.copy(
                pieces = piezas.toList(),
                turn = siguienteTurno,
                esJaqueMate = esMate,
                esJaque = enJaque,
                esTablas = esAhogado,
                mensajeEstado = when {
                    esMate -> "¡MATE! Gana: ${if (siguienteTurno == Team.BLANCAS) "NEGRO" else "BLANCAS"}"
                    esAhogado -> "¡TABLAS!"
                    enJaque -> "¡JAQUE!"
                    siguienteTurno == Team.BLANCAS -> "Tu turno"
                    else -> "IA pensando..."
                }
            )
        }

        if (siguienteTurno == Team.NEGRO && !esMate && !esAhogado) {
            ejecutarJugadaIA()
        }
    }

    private fun ejecutarJugadaIA() {
        viewModelScope.launch {
            // Tiempo de "pensamiento" basado en dificultad
            val delayTime = when(nivelIA) {
                NivelIA.PRINCIPIANTE -> 2000L
                NivelIA.FACIL -> 1500L
                NivelIA.NORMAL -> 600L
                NivelIA.AVANZADO -> 100L
            }
            delay(delayTime)

            // IA procesando en segundo plano
            val movimiento = withContext(Dispatchers.Default) {
                ChessAI.calcularMovimientoIA(_boardState.value.pieces, nivelIA)
            }

            if (movimiento != null) {
                intentarMovimiento(movimiento.first, movimiento.second)
            }
        }
    }
}