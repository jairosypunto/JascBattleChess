package com.jasc.jascbattlechess.domain

import com.jasc.jascbattlechess.data.*
import kotlin.random.Random

object ChessAI {

    fun calcularMovimientoIA(piezas: List<PieceState>, nivel: NivelIA): Pair<Position, Position>? {
        val piezasNegras = piezas.filter { it.team == Team.NEGRO && it.health > 0 }

        // 1. Prioridad: Capturar piezas enemigas
        val capturas = mutableListOf<Pair<Position, Position>>()
        for (p in piezasNegras) {
            for (x in 0..7) for (y in 0..7) {
                val dest = Position(x, y)
                if (MoveValidator.esMovimientoValido(p, dest, piezas)) {
                    val objetivo = piezas.find { it.position == dest && it.team == Team.BLANCAS && it.health > 0 }
                    if (objetivo != null) capturas.add(p.position to dest)
                }
            }
        }

        // Si el nivel es AVANZADO, prioriza capturar la pieza de mayor valor
        if (nivel == NivelIA.AVANZADO && capturas.isNotEmpty()) {
            return capturas.maxByOrNull { (_, dest) ->
                val obj = piezas.find { it.position == dest }
                obtenerValorPieza(obj?.type ?: PieceType.PEON)
            }
        }
        if (capturas.isNotEmpty()) return capturas.random()

        // 2. Movimientos generales con filtrado de seguridad para niveles altos
        val todosMovimientos = mutableListOf<Pair<Position, Position>>()
        for (p in piezasNegras) {
            for (x in 0..7) for (y in 0..7) {
                val dest = Position(x, y)
                if (MoveValidator.esMovimientoValido(p, dest, piezas)) {
                    todosMovimientos.add(p.position to dest)
                }
            }
        }

        if (nivel == NivelIA.AVANZADO || nivel == NivelIA.NORMAL) {
            // Filtro de seguridad: Evitar movimientos donde la IA pierde su pieza inmediatamente
            val movimientosSeguros = todosMovimientos.filter { (_, dest) ->
                !MoveValidator.estaBajoAtaque(dest, Team.NEGRO, piezas.filter { it.position != it.position /*simulacro*/ })
            }

            if (movimientosSeguros.isNotEmpty()) return movimientosSeguros.random()
        }

        return todosMovimientos.randomOrNull()
    }

    private fun obtenerValorPieza(type: PieceType): Int = when (type) {
        PieceType.REY -> 1000; PieceType.REINA -> 90; PieceType.TORRE -> 50
        PieceType.ALFIL -> 30; PieceType.CABALLO -> 30; PieceType.PEON -> 10
    }
}