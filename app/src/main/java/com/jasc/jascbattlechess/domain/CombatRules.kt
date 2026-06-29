package com.jasc.jascbattlechess.domain

import com.jasc.jascbattlechess.data.PieceState
import com.jasc.jascbattlechess.data.PieceType // DEBES AGREGAR ESTA LÍNEA

object CombatRules {
    fun calcularDano(atacante: PieceState, defensor: PieceState): Int {
        return when (atacante.type) {
            PieceType.REINA -> 50 // Ahora sí será reconocido
            else -> 20
        }
    }
}