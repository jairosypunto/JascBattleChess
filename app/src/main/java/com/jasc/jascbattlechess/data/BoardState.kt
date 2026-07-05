

package com.jasc.jascbattlechess.data

data class BoardState(
    val pieces: List<PieceState> = emptyList(),
    val turn: Team = Team.BLANCAS,
    val selectedPosition: Position? = null, // <--- Centralizamos aquí
    val esJaqueMate: Boolean = false,
    val esJaque: Boolean = false, // <--- Esta es la linea que faltaba
    val esTablas: Boolean = false,
    val mensajeEstado: String = "Turno de BLANCAS",
    val isIAThinking: Boolean = false,
    val puntosBlancas: Int = 0,
    val puntosNegras: Int = 0
)