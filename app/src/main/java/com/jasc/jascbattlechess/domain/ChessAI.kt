package com.jasc.jascbattlechess.domain

import com.jasc.jascbattlechess.data.* // Esto importa NivelIA desde GameConfig.kt
import com.jasc.jascbattlechess.data.PieceState
import com.jasc.jascbattlechess.data.Position

object ChessAI {

    fun calcularMovimientoIA(piezas: List<PieceState>, nivel: NivelIA): Pair<Position, Position>? {
        val piezasNegras = piezas.filter { it.team == Team.NEGRO && it.health > 0 }.shuffled()

        // 1. Buscar Capturas / Ataques
        for (pieza in piezasNegras) {
            for (x in 0..7) {
                for (y in 0..7) {
                    val destino = Position(x, y)
                    val objetivo = piezas.find { it.position == destino && it.team == Team.BLANCAS && it.health > 0 }
                    if (objetivo != null && MoveValidator.esMovimientoValido(pieza, destino, piezas)) {
                        return Pair(pieza.position, destino)
                    }
                }
            }
        }

        // 2. Si no hay ataques, realizar un movimiento según nivel
        val destinosPosibles = mutableListOf<Position>()
        for (x in 0..7) { for (y in 0..7) { destinosPosibles.add(Position(x, y)) } }

        // Lógica de dificultad basada en el nivel
        if (nivel == NivelIA.NORMAL || nivel == NivelIA.AVANZADO) {
            destinosPosibles.sortByDescending { it.x }
        } else {
            destinosPosibles.shuffle()
        }

        for (pieza in piezasNegras) {
            for (destino in destinosPosibles) {
                if (MoveValidator.esMovimientoValido(pieza, destino, piezas)) {
                    return Pair(pieza.position, destino)
                }
            }
        }
        return null
    }
}