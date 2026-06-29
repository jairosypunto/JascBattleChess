package com.jasc.jascbattlechess.domain

import com.jasc.jascbattlechess.data.PieceState
import com.jasc.jascbattlechess.data.PieceType

object CombatRules {
    /**
     * Daño basado en el número de golpes para derribar (Vida = 100):
     * Rey (1), Reina (1), Torre (2), Caballo (3), Alfil (4), Peón (5)
     */
    fun calcularDano(atacante: PieceState, defensor: PieceState): Int {
        return when (atacante.type) {
            PieceType.REY -> 100
            PieceType.REINA -> 100
            PieceType.TORRE -> 50
            PieceType.CABALLO -> 34
            PieceType.ALFIL -> 25
            PieceType.PEON -> 20
        }
    }
}