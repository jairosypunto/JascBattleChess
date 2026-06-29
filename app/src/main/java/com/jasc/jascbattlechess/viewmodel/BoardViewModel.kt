package com.jasc.jascbattlechess.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jasc.jascbattlechess.data.*
import com.jasc.jascbattlechess.domain.ChessAI
import com.jasc.jascbattlechess.domain.CombatRules
import com.jasc.jascbattlechess.domain.MoveValidator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs
import android.util.Log

class BoardViewModel : ViewModel() {
    private val _boardState = MutableStateFlow(BoardState(pieces = crearPiezasIniciales()))
    val boardState = _boardState.asStateFlow()

    private fun crearPiezasIniciales(): List<PieceState> {
        val piezas = mutableListOf<PieceState>()

        // Fila de peones: Plata abajo (6), Negro arriba (1)
        for (i in 0..7) {
            piezas.add(PieceState("pP$i", PieceType.PEON, Team.BLANCAS, Position(6, i)))
            piezas.add(PieceState("pN$i", PieceType.PEON, Team.NEGRO, Position(1, i)))
        }

        // Reina en su color: y=3 será blanca en casilla clara y negra en oscura
        val orden = listOf(PieceType.TORRE, PieceType.CABALLO, PieceType.ALFIL, PieceType.REINA, PieceType.REY, PieceType.ALFIL, PieceType.CABALLO, PieceType.TORRE)
        orden.forEachIndexed { i, tipo ->
            piezas.add(PieceState("Plata$i", tipo, Team.BLANCAS, Position(7, i)))
            piezas.add(PieceState("Negro$i", tipo, Team.NEGRO, Position(0, i)))
        }
        return piezas
    }
    // Agrega esto a tu BoardViewModel
    fun resetearJuego() {
        _boardState.value = BoardState(pieces = crearPiezasIniciales())
    }
    // 1. Añade esto a tus variables del ViewModel
    private val _historial = mutableListOf<List<PieceState>>()



    fun intentarMovimiento(origen: Position, destino: Position) {
        Log.d("JascChess", "INICIO: Intentando mover de $origen a $destino")

        val currentState = _boardState.value
        if (currentState.esJaqueMate || currentState.esTablas) {
            Log.w("JascChess", "ABORTADO: JaqueMate o Tablas detectado")
            return
        }

        val piezaAtacante = currentState.pieces.find { it.position == origen }
        if (piezaAtacante == null) {
            Log.e("JascChess", "ERROR: No se encontró pieza atacante en $origen")
            return
        }

        if (piezaAtacante.team != currentState.turn) {
            Log.w("JascChess", "ABORTADO: No es el turno del equipo ${piezaAtacante.team}")
            return
        }

        if (!MoveValidator.esMovimientoValido(piezaAtacante, destino, currentState.pieces)) {
            Log.w("JascChess", "ABORTADO: Movimiento inválido para ${piezaAtacante.type}")
            return
        }

        _historial.add(currentState.pieces)
        var nuevasPiezas = currentState.pieces.toMutableList()

        // Lógica de Enroque
        if (piezaAtacante.type == PieceType.REY && abs(destino.y - origen.y) == 2) {
            Log.d("JascChess", "PROCESANDO: Enroque detectado")
            val esCorto = destino.y > origen.y
            val torreY = if (esCorto) 7 else 0
            val nuevaTorreY = if (esCorto) 5 else 3
            val torre = nuevasPiezas.find { it.type == PieceType.TORRE && it.position.y == torreY && it.team == piezaAtacante.team }
            if (torre != null) {
                nuevasPiezas.remove(torre)
                nuevasPiezas.add(torre.copy(position = Position(origen.x, nuevaTorreY), isMoved = true))
            }
        }

        // Lógica de Captura
        val piezaDefensora = nuevasPiezas.find { it.position == destino }
        if (piezaDefensora != null) {
            val dano = CombatRules.calcularDano(piezaAtacante, piezaDefensora)
            piezaDefensora.health -= dano
            Log.d("JascChess", "CAPTURA: ${piezaDefensora.type} recibió $dano de daño. Vida actual: ${piezaDefensora.health}")

            if (piezaDefensora.health <= 0) {
                nuevasPiezas.remove(piezaDefensora)
                Log.d("JascChess", "CAPTURA: Pieza eliminada del tablero")
            } else {
                Log.d("JascChess", "CAPTURA: Pieza sobrevive. Finalizando turno aquí.")
                actualizarEstadoJuego(nuevasPiezas.map { if (it.id == piezaDefensora.id) piezaDefensora else it }, currentState.turn)
                return
            }
        }

        // Mover pieza
        nuevasPiezas = nuevasPiezas.map { if (it.id == piezaAtacante.id) it.copy(position = destino, isMoved = true) else it }.toMutableList()
        Log.d("JascChess", "MOVIMIENTO: Pieza ${piezaAtacante.type} movida a $destino")

        // Coronación
        val piezasFinales = nuevasPiezas.map {
            if (it.type == PieceType.PEON && (it.position.x == 0 || it.position.x == 7)) {
                Log.d("JascChess", "EVENTO: Coronación ejecutada")
                it.copy(type = PieceType.REINA)
            } else it
        }

        val siguienteTurno = if (currentState.turn == Team.BLANCAS) Team.NEGRO else Team.BLANCAS
        Log.d("JascChess", "FIN: Llamando a actualizarEstadoJuego. Turno siguiente: $siguienteTurno")
        actualizarEstadoJuego(piezasFinales, siguienteTurno)
    }

    fun retrocederJugada() {
        if (_historial.isNotEmpty()) {
            val estadoAnterior = _historial.removeAt(_historial.size - 1)
            _boardState.update { it.copy(pieces = estadoAnterior, turn = if (it.turn == Team.BLANCAS) Team.NEGRO else Team.BLANCAS) }
        }
    }
    fun limpiarAlertaJaque() {
        _boardState.update { it.copy(esJaque = false) }
    }
    private fun actualizarEstadoJuego(piezas: List<PieceState>, siguienteTurno: Team) {
        // 1. Calculamos el estado de forma segura
        val enJaque = MoveValidator.esJaque(siguienteTurno, piezas)
        val esMate = MoveValidator.esJaqueMate(siguienteTurno, piezas)

        val mensaje = when {
            esMate -> "¡JAQUE MATE! Ganador: ${if (siguienteTurno == Team.BLANCAS) "NEGRO" else "BLANCAS"}"
            enJaque -> "¡JAQUE AL REY ${siguienteTurno.name}!"
            siguienteTurno == Team.BLANCAS -> "Turno de BLANCAS (Tú)"
            else -> "Turno de NEGRO (IA pensando...)"
        }

        // 2. FORZAMOS la actualización del estado con una lista inmutable limpia
        // Usar .toList() asegura que no haya referencias mutables compartidas
        _boardState.update { currentState ->
            currentState.copy(
                pieces = piezas.toList(),
                turn = siguienteTurno,
                esJaqueMate = esMate,
                esJaque = enJaque,
                mensajeEstado = mensaje
            )
        }

        // 3. IA: Mantenemos el retardo para que el jugador vea la captura
        if (siguienteTurno == Team.NEGRO && !esMate) {
            ejecutarJugadaIA()
        }
    }

    private fun ejecutarJugadaIA() {
        viewModelScope.launch {
            delay(600) // Simulación de pensamiento de la IA
            val movimiento = ChessAI.calcularMovimientoIA(_boardState.value.pieces)
            if (movimiento != null) {
                intentarMovimiento(movimiento.first, movimiento.second)
            }
        }
    }
}