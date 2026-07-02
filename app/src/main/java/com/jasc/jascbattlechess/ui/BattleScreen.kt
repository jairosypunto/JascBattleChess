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
            kotlinx.coroutines.delay(3000) // Espera 3 segundos
            viewModel.limpiarAlertaJaque() // La cierra sola
        }
    }

// CONDICIÓN ACTUALIZADA: Ahora incluye esTablas
    if (boardState.esJaqueMate || boardState.esJaque || boardState.esTablas) {
        AlertDialog(
            onDismissRequest = {
                // Si solo es Jaque, permitimos cerrar al tocar fuera
                if (boardState.esJaque) viewModel.limpiarAlertaJaque()
            },
            title = {
                Text(text = when {
                    boardState.esJaqueMate -> "¡Jaque Mate!"
                    boardState.esTablas -> "¡Tablas!"
                    else -> "¡Jaque!"
                })
            },
            text = { Text(text = boardState.mensajeEstado) },
            confirmButton = {
                TextButton(onClick = {
                    if (boardState.esJaqueMate || boardState.esTablas) {
                        viewModel.resetearJuego()
                    } else {
                        // Acción para cerrar la alerta de Jaque limpiando el estado
                        viewModel.limpiarAlertaJaque()
                    }
                }) {
                    Text(if (boardState.esJaqueMate || boardState.esTablas) "Reiniciar" else "Entendido")
                }
            }
        )
    }

    // 2. Estructura con Scaffold para los botones de control
    Scaffold(
        // 1. Zona superior para el mensaje de estado (siempre visible)
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
        // 2. Botones de control abajo
        floatingActionButton = {
            Row {
                FloatingActionButton(
                    onClick = { viewModel.retrocederJugada() },
                    containerColor = Color(0xFF312E2B),
                    contentColor = Color.White
                ) {
                    Text("⬅️", fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                FloatingActionButton(
                    onClick = { viewModel.resetearJuego() },
                    containerColor = Color(0xFF312E2B),
                    contentColor = Color.White
                ) {
                    Text("🔄", fontSize = 20.sp)
                }
            }
        }
    ) { paddingValues ->
        // 3. Contenido principal (Tablero)
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            modifier = Modifier
                .fillMaxSize() // Ocupa todo el contenedor
                .padding(paddingValues), // Elimina el padding fijo de 16.dp extra si quieres más espacio
            contentPadding = PaddingValues(4.dp) // Un pequeño margen para que no toque los bordes
        ) {
            items(64) { index ->
                val x = index / 8
                val y = index % 8
                val currentPos = Position(x, y)
                val isSelected = selectedPosition == currentPos

                // Buscamos la pieza basándonos en el estado más reciente del ViewModel
                val pieza = boardState.pieces.find { it.position.x == x && it.position.y == y }

// Define esto arriba de tu LazyVerticalGrid, dentro de BattleScreen
                val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }

// Dentro del LazyVerticalGrid...
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .background(if ((x + y) % 2 != 0) Color(0xFFB58863) else Color(0xFFF0D9B5))
                        .border(if (isSelected) 4.dp else 0.dp, if (isSelected) Color.Yellow else Color.Transparent)
                        .clickable {
                            if (selectedPosition == null) {
                                // Seleccionamos nuestra pieza
                                if (pieza != null && pieza.team == boardState.turn) {
                                    selectedPosition = currentPos
                                }
                            } else {
                                val origen = selectedPosition

                                // Ejecutamos el ataque
                                viewModel.intentarMovimiento(origen!!, currentPos)

                                // Verificamos si en el destino (currentPos) TODAVÍA hay un enemigo vivo
                                val enemigoVivo = boardState.pieces.find { it.position == currentPos && it.team != boardState.turn && it.health > 0 }

                                // CONDICIÓN CRÍTICA:
                                // Si hay enemigo vivo, mantenemos la selección en EL ORIGEN (tu pieza)
                                // Si no hay enemigo o el enemigo murió, apagamos todo.
                                if (enemigoVivo != null) {
                                    selectedPosition = origen
                                } else {
                                    selectedPosition = null
                                }
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

