package com.jasc.jascbattlechess.domain

import com.jasc.jascbattlechess.data.*
import kotlin.math.abs

object MoveValidator {

    fun esMovimientoValido(pieza: PieceState, destino: Position, piezas: List<PieceState>): Boolean {
        if (piezas.any { it.position == destino && it.team == pieza.team }) return false
        if (!verificarGeometria(pieza, destino, piezas)) return false

        val piezasSimuladas = piezas.map {
            when {
                it.id == pieza.id -> it.copy(position = destino)
                it.position == destino -> it.copy(health = 0, position = Position(-1, -1)) // ✅ elimina la pieza capturada
                else -> it
            }
        }

        // Si es el rey, no puede moverse a una casilla bajo ataque
        if (pieza.type == PieceType.REY && esJaque(pieza.team, piezasSimuladas)) return false

        return !quedariaEnJaque(pieza, destino, piezas)
    }

    fun esReyAhogado(equipo: Team, piezas: List<PieceState>): Boolean {
        // Caso especial: solo quedan los dos reyes → tablas
        val reyesVivos = piezas.filter { it.type == PieceType.REY && it.health > 0 }
        val piezasVivas = piezas.filter { it.health > 0 }
        if (piezasVivas.size == 2 && reyesVivos.size == 2) {
            return true
        }

        // Si está en jaque, no puede ser ahogado
        if (esJaque(equipo, piezas)) return false

        // Revisar si alguna pieza tiene movimiento válido
        val piezasDelEquipo = piezas.filter { it.team == equipo && it.health > 0 }
        return piezasDelEquipo.none { pieza ->
            (0..7).any { x ->
                (0..7).any { y ->
                    esMovimientoValido(pieza, Position(x, y), piezas)
                }
            }
        }
    }

    private fun verificarGeometria(pieza: PieceState, destino: Position, piezas: List<PieceState>): Boolean {
        val dx = destino.x - pieza.position.x
        val dy = destino.y - pieza.position.y
        val absDx = abs(dx)
        val absDy = abs(dy)

        return when (pieza.type) {
            PieceType.PEON -> validarPeon(pieza, destino, piezas)
            PieceType.TORRE -> (dx == 0 || dy == 0) && caminoLibre(pieza.position, destino, piezas)
            PieceType.REINA -> ((dx == 0 || dy == 0) || (absDx == absDy)) && caminoLibre(pieza.position, destino, piezas)
            PieceType.ALFIL -> (absDx == absDy) && caminoLibre(pieza.position, destino, piezas)


            PieceType.REY -> (absDx <= 1 && absDy <= 1) || validarEnroque(pieza, destino, piezas)
            PieceType.CABALLO -> (absDx == 2 && absDy == 1) || (absDx == 1 && absDy == 2)
        }
    }

    private fun validarPeon(pieza: PieceState, destino: Position, piezas: List<PieceState>): Boolean {
        val dir = if (pieza.team == Team.BLANCAS) -1 else 1
        val dx = destino.x - pieza.position.x
        val dy = destino.y - pieza.position.y

        if (dy == 0) {
            if (dx == dir && piezas.none { it.position == destino }) return true
            if (!pieza.isMoved && dx == 2 * dir &&
                piezas.none { it.position == Position(pieza.position.x + dir, pieza.position.y) } &&
                piezas.none { it.position == destino }) return true
        }
        if (abs(dy) == 1 && dx == dir) {
            return piezas.any { it.position == destino && it.team != pieza.team }
        }
        return false
    }

    private fun validarEnroque(rey: PieceState, destino: Position, piezas: List<PieceState>): Boolean {
        if (rey.isMoved || abs(destino.y - rey.position.y) != 2 || destino.x != rey.position.x) return false
        val esEnroqueCorto = destino.y > rey.position.y
        val torreY = if (esEnroqueCorto) 7 else 0
        val torre = piezas.find { it.type == PieceType.TORRE && it.team == rey.team && it.position.y == torreY && !it.isMoved } ?: return false
        if (!caminoLibre(rey.position, torre.position, piezas)) return false
        val pasos = if (esEnroqueCorto) listOf(4, 5, 6) else listOf(4, 3, 2)
        for (y in pasos) {
            if (estaBajoAtaque(Position(rey.position.x, y), rey.team, piezas)) return false
        }
        return true
    }

    private fun quedariaEnJaque(pieza: PieceState, destino: Position, piezas: List<PieceState>): Boolean {
        val piezasSimuladas = piezas.map {
            when {
                it.id == pieza.id -> it.copy(position = destino)
                it.position == destino -> it.copy(health = 0, position = Position(-1, -1)) // ✅ elimina la pieza capturada
                else -> it
            }
        }
        val rey = piezasSimuladas.find { it.type == PieceType.REY && it.team == pieza.team } ?: return true
        return estaBajoAtaque(rey.position, pieza.team, piezasSimuladas)
    }

    fun estaBajoAtaque(pos: Position, equipo: Team, piezas: List<PieceState>): Boolean {
        return piezas.filter { it.team != equipo }.any { enemiga ->
            verificarGeometriaBase(enemiga, pos, piezas)
        }
    }

    private fun verificarGeometriaBase(pieza: PieceState, destino: Position, piezas: List<PieceState>): Boolean {
        val dx = destino.x - pieza.position.x
        val dy = destino.y - pieza.position.y
        val absDx = abs(dx)
        val absDy = abs(dy)
        return when (pieza.type) {
            PieceType.PEON -> validarPeon(pieza, destino, piezas)
            PieceType.TORRE -> (dx == 0 || dy == 0) && caminoLibre(pieza.position, destino, piezas)
            PieceType.REINA -> ((dx == 0 || dy == 0) || (absDx == absDy)) && caminoLibre(pieza.position, destino, piezas)
            PieceType.ALFIL -> (absDx == absDy) && caminoLibre(pieza.position, destino, piezas)


            PieceType.REY -> (absDx <= 1 && absDy <= 1)
            PieceType.CABALLO -> (absDx == 2 && absDy == 1) || (absDx == 1 && absDy == 2)
        }
    }

    private fun caminoLibre(origen: Position, destino: Position, piezas: List<PieceState>): Boolean {
        val dx = destino.x - origen.x
        val dy = destino.y - origen.y
        val stepX = if (dx == 0) 0 else dx / abs(dx)
        val stepY = if (dy == 0) 0 else dy / abs(dy)
        var x = origen.x + stepX
        var y = origen.y + stepY

        // Recorre hasta la casilla anterior al destino
        while (x != destino.x || y != destino.y) {
            // Si hay una pieza en el camino (antes del destino) → bloquea
            if (piezas.any { it.position.x == x && it.position.y == y && it.health > 0 }) return false
            x += stepX
            y += stepY
        }

        // ✅ Permite que el destino esté ocupado por un enemigo (captura)
        return true
    }


    fun esJaque(equipo: Team, piezas: List<PieceState>): Boolean {
        val rey = piezas.find { it.type == PieceType.REY && it.team == equipo }
        if (rey == null) return false
        return estaBajoAtaque(rey.position, equipo, piezas)
    }

    fun esJaqueMate(equipo: Team, piezas: List<PieceState>): Boolean {
        // 1. Si no está en jaque, no puede ser mate
        if (!esJaque(equipo, piezas)) return false

        // 2. Revisar todos los movimientos posibles
        val piezasDelEquipo = piezas.filter { it.team == equipo && it.health > 0 }
        for (pieza in piezasDelEquipo) {
            for (x in 0..7) {
                for (y in 0..7) {
                    val destino = Position(x, y)
                    if (esMovimientoValido(pieza, destino, piezas)) {
                        // Simular el movimiento
                        val piezasSimuladas = piezas.map {
                            when {
                                it.id == pieza.id -> it.copy(position = destino)
                                it.position == destino -> it.copy(health = 0, position = Position(-1, -1))
                                else -> it
                            }
                        }
                        // 3. Si después del movimiento el rey ya no está en jaque → no es mate
                        if (!esJaque(equipo, piezasSimuladas)) {
                            return false
                        }
                    }
                }
            }
        }

        // 4. Si ningún movimiento salva al rey → jaque mate
        return true
    }
}
