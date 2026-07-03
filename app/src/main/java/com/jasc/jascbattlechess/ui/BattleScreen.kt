package com.jasc.jascbattlechess.ui

import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jasc.jascbattlechess.R
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

    if (boardState.esJaqueMate || boardState.esJaque || boardState.esTablas) {
        AlertDialog(
            onDismissRequest = { if (boardState.esJaque) viewModel.limpiarAlertaJaque() },
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
                    if (boardState.esJaqueMate || boardState.esTablas) viewModel.resetearJuego()
                    else viewModel.limpiarAlertaJaque()
                }) {
                    Text(if (boardState.esJaqueMate || boardState.esTablas) "Reiniciar" else "Entendido")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier,
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
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(4.dp)
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
                        .border(
                            width = if (isSelected) 4.dp else 0.dp,
                            color = if (isSelected) Color.Yellow else Color.Transparent
                        )
                        .clickable {
                            if (selectedPosition == null) {
                                if (pieza != null && pieza.team == boardState.turn && pieza.health > 0) {
                                    selectedPosition = currentPos
                                }
                            } else {
                                // Ejecutamos el movimiento
                                viewModel.intentarMovimiento(selectedPosition!!, currentPos)

                                // CORRECCIÓN: Solo reseteamos si la ficha se movió de verdad
                                // Si el oponente sigue vivo, la posición de origen no cambió.
                                val piezaEnOrigen = boardState.pieces.find { it.position == selectedPosition }
                                if (piezaEnOrigen == null || piezaEnOrigen.position != selectedPosition) {
                                    selectedPosition = null
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (pieza != null) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Image(
                                painter = painterResource(id = obtenerRecursoPieza(pieza)),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().padding(2.dp)
                            )

                            Column(
                                modifier = Modifier.align(Alignment.BottomCenter),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Mostramos vida incluso si es <= 0 para que la gota se pinte
                                if (pieza.health in 1..99) {
                                    Text(
                                        text = "${pieza.health}%",
                                        fontSize = 10.sp,
                                        color = Color.Red,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.background(Color.White.copy(alpha = 0.6f))
                                    )
                                }
                                if (pieza.health <= 34) {
                                    Text(text = "🩸", fontSize = 20.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun obtenerRecursoPieza(pieza: PieceState): Int {
    return when (pieza.type) {
        PieceType.CABALLO -> if (pieza.team == Team.NEGRO) R.drawable.romano_caballo_negro else R.drawable.ic_caballo_blanco
        PieceType.PEON -> if (pieza.team == Team.NEGRO) R.drawable.peon_guerrero_negro else R.drawable.peon_guerrero_blanco
        PieceType.ALFIL -> if (pieza.team == Team.NEGRO) R.drawable.alfil_negro else R.drawable.alfil_blanco
        PieceType.TORRE -> if (pieza.team == Team.NEGRO) R.drawable.torre_negra else R.drawable.torre_blanca
        PieceType.REINA -> if (pieza.team == Team.NEGRO) R.drawable.reina_negra else R.drawable.reina_blanca
        PieceType.REY -> if (pieza.team == Team.NEGRO) R.drawable.rey_negro else R.drawable.rey_blanco
        else -> R.drawable.ic_launcher_foreground
    }
}