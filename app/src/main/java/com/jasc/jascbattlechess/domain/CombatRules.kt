package com.jasc.jascbattlechess.domain

import com.jasc.jascbattlechess.data.PieceState
import com.jasc.jascbattlechess.data.PieceType

object CombatRules {
    // Valor único para cada pieza
    fun obtenerValorPieza(type: PieceType): Int = when (type) {
        PieceType.REY -> 100
        PieceType.REINA -> 100
        PieceType.TORRE -> 50
        PieceType.CABALLO -> 34
        PieceType.ALFIL -> 25
        PieceType.PEON -> 20
    }

    fun calcularDano(atacante: PieceState, defensor: PieceState): Int {
        return obtenerValorPieza(atacante.type)
    }
}

// Función de extensión limpia usando la lógica centralizada
fun List<PieceState>.sumarPuntos(): Int {
    return this.sumOf { CombatRules.obtenerValorPieza(it.type) }
}