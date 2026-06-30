package com.jasc.jascbattlechess.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jasc.jascbattlechess.data.*
import com.jasc.jascbattlechess.viewmodel.BoardViewModel

@Composable
fun BattleScreen(
    modifier: Modifier = Modifier,
    viewModel: BoardViewModel = viewModel()
) {
    val boardState by viewModel.boardState.collectAsState()
    var selectedPosition by remember { mutableStateOf<Position?>(null) }

    if (boardState.esJaque && !boardState.esJaqueMate) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(3000)
            viewModel.limpiarAlertaJaque()
        }
    }

    if (boardState.esJaqueMate || boardState.esJaque) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(text = if (boardState.esJaqueMate) "¡Jaque Mate!" else "¡Jaque!")
            },
            text = { Text(text = boardState.mensajeEstado) },
            confirmButton = {
                TextButton(onClick = {
                    if (!boardState.esJaqueMate) {
                        // Acción para cerrar la alerta de Jaque
                    } else {
                        viewModel.resetearJuego()
                    }
                }) {
                    Text(if (boardState.esJaqueMate) "Reiniciar" else "Entendido")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                color = Color(0xFF312E2B),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = boardState.mensajeEstado,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        },
        floatingActionButton = {
            Row {
                FloatingActionButton(
                    onClick = { viewModel.retrocederJugada() },
                    containerColor = Color(0xFF312E2B),
                    contentColor = Color.White
                ) { Text("⬅️", fontSize = 20.sp) }
                Spacer(modifier = Modifier.width(16.dp))
                FloatingActionButton(
                    onClick = { viewModel.resetearJuego() },
                    containerColor = Color(0xFF312E2B),
                    contentColor = Color.White
                ) { Text("🔄", fontSize = 20.sp) }
            }
        }
    ) { paddingValues ->
        // Contenedor que fuerza la forma cuadrada sin tocar tu lógica interna
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(8),
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .aspectRatio(1f) // Esto mantiene el tablero cuadrado
            ) {
                items(64) { index ->
                    val x = index / 8
                    val y = index % 8
                    val currentPos = Position(x, y)
                    val isSelected = selectedPosition == currentPos
                    val pieza = boardState.pieces.find { it.position.x == x && it.position.y == y }

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .background(if ((x + y) % 2 != 0) Color(0xFFB58863) else Color(0xFFF0D9B5))
                            .border(if (isSelected) 4.dp else 0.dp, if (isSelected) Color.Yellow else Color.Transparent)
                            .clickable {
                                if (selectedPosition == null) {
                                    if (pieza != null && pieza.team == boardState.turn) selectedPosition = currentPos
                                } else {
                                    val origen = selectedPosition
                                    viewModel.intentarMovimiento(origen!!, currentPos)
                                    val enemigoVivo = boardState.pieces.find { it.position == currentPos && it.team != boardState.turn && it.health > 0 }
                                    selectedPosition = if (enemigoVivo != null) origen else null
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (pieza != null) {
                            Text(
                                text = obtenerEmojiPieza(pieza.type, pieza.team),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            // AQUÍ ESTÁ TU LÓGICA DE VIDA QUE FALTABA:
                            if (pieza.health < 100) {
                                Text(
                                    text = "${pieza.health}%",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (pieza.health > 50) Color.Green else Color.Red,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(2.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.small)
                                        .padding(horizontal = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun obtenerEmojiPieza(tipo: PieceType, equipo: Team): String {
    return when (equipo) {
        Team.BLANCAS -> when (tipo) {
            PieceType.REY -> "♔"
            PieceType.REINA -> "♕"
            PieceType.TORRE -> "♖"
            PieceType.ALFIL -> "♗"
            PieceType.CABALLO -> "♘"
            PieceType.PEON -> "♙"
        }
        Team.NEGRO -> when (tipo) {
            PieceType.REY -> "♚"
            PieceType.REINA -> "♛"
            PieceType.TORRE -> "♜"
            PieceType.ALFIL -> "♝"
            PieceType.CABALLO -> "♞"
            PieceType.PEON -> "♟"
        }
    }
}