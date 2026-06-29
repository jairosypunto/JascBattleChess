package com.jasc.jascbattlechess.domain

import com.jasc.jascbattlechess.data.*

object ChessAI {
    fun calcularMovimientoIA(piezas: List<PieceState>): Pair<Position, Position>? {
        val piezasNegras = piezas.filter { it.team == Team.NEGRO }.shuffled()

        // 1. Buscar Capturas / Ataques primero
        for (pieza in piezasNegras) {
            for (x in 0..7) {
                for (y in 0..7) {
                    val destino = Position(x, y)
                    val objetivo = piezas.find { it.position == destino }
                    if (objetivo != null && objetivo.team == Team.BLANCAS) {
                        if (MoveValidator.esMovimientoValido(pieza, destino, piezas)) {
                            return Pair(pieza.position, destino)
                        }
                    }
                }
            }
        }

        // 2. Si no hay ataques, realizar un movimiento seguro
        for (pieza in piezasNegras) {
            val destinosPosibles = (0..7).flatMap { x -> (0..7).map { y -> Position(x, y) } }.shuffled()
            for (destino in destinosPosibles) {
                if (MoveValidator.esMovimientoValido(pieza, destino, piezas)) {
                    return Pair(pieza.position, destino)
                }
            }
        }
        return null
    }
}