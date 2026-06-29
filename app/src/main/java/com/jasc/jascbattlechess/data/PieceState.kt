package com.jasc.jascbattlechess.data

enum class PieceType { REY, REINA, TORRE, ALFIL, CABALLO, PEON }
enum class Team { BLANCAS, NEGRO }

data class Position(val x: Int, val y: Int)

data class PieceState(
    val id: String,
    val type: PieceType,
    val team: Team,
    val position: Position,
    var health: Int = 100,
    val isMoving: Boolean = false,
    val isMoved: Boolean = false // Nuevo: fundamental para reglas de ajedrez
)