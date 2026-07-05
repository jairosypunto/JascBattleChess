package com.jasc.jascbattlechess.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

    val capturadasNegras = boardState.pieces.filter { it.health <= 0 && it.team == Team.NEGRO }
    val capturadasBlancas = boardState.pieces.filter { it.health <= 0 && it.team == Team.BLANCAS }

    val puntosIA = capturadasBlancas.map { p ->
        when (p.type) { PieceType.REY -> 100; PieceType.REINA -> 100; PieceType.TORRE -> 50; PieceType.CABALLO -> 34; PieceType.ALFIL -> 25; PieceType.PEON -> 20 }
    }.sum()

    val puntosBlancas = capturadasNegras.map { p ->
        when (p.type) { PieceType.REY -> 100; PieceType.REINA -> 100; PieceType.TORRE -> 50; PieceType.CABALLO -> 34; PieceType.ALFIL -> 25; PieceType.PEON -> 20 }
    }.sum()

    // ... (Tu bloque AlertDialog se mantiene igual)

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF1A1A1A))) {
        Column(modifier = Modifier.fillMaxSize()) {

            // 1. CEMENTERIO IA
            Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                Text("IA (Capturadas):", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth().height(50.dp).background(Color(0xFF2C2C2C)).border(1.dp, Color.Gray).padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    LazyRow(modifier = Modifier.weight(1f)) {
                        items(capturadasNegras) { piece -> Box(Modifier.size(40.dp)) { PieceComponent(piece) } }
                    }
                    Text("Pts: $puntosIA", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            // 2. CONTROLES (SOLO UNO, SIN DUPLICADOS)
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { viewModel.retrocederJugada() }) { Text("⬅️ Deshacer", fontSize = 12.sp) }
                Text(boardState.mensajeEstado, color = Color.White.copy(alpha = 0.8f), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                TextButton(onClick = { viewModel.resetearJuego() }) { Text("🔄 Reset", fontSize = 12.sp) }
            }

            // 3. TABLERO (Con control estricto de turno)
            LazyVerticalGrid(
                columns = GridCells.Fixed(8),
                modifier = Modifier.fillMaxWidth().weight(1f).padding(4.dp)
            ) {
                items(64) { index ->
                    val x = index / 8; val y = index % 8
                    val currentPos = Position(x, y)
                    val isSelected = selectedPosition == currentPos
                    val pieza = boardState.pieces.find { it.position.x == x && it.position.y == y && it.health > 0 }

                    Box(
                        modifier = Modifier.aspectRatio(1f).background(if ((x + y) % 2 != 0) Color(0xFFB58863) else Color(0xFFF0D9B5))
                            .border(width = if (isSelected) 4.dp else 0.dp, color = if (isSelected) Color.Yellow else Color.Transparent)
// Dentro del LazyVerticalGrid, en el modifier del Box:
                            .clickable(enabled = boardState.turn == Team.BLANCAS) {
                                if (selectedPosition == null) {
                                    // Seleccionar solo si es tuya
                                    if (pieza != null && pieza.team == Team.BLANCAS) {
                                        selectedPosition = currentPos
                                    }
                                } else {
                                    val origen = selectedPosition!!
                                    // Intentamos el movimiento/ataque
                                    viewModel.intentarMovimiento(origen, currentPos)

                                    // REGLA: Si después del intento el destino sigue teniendo un enemigo vivo,
                                    // mantenemos la selección para seguir golpeando. Si no, soltamos.
                                    val nuevoEstado = viewModel.boardState.value
                                    val enemigoEnDestino = nuevoEstado.pieces.find { it.position == currentPos && it.team == Team.NEGRO && it.health > 0 }

                                    selectedPosition = if (enemigoEnDestino != null) origen else null
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) { if (pieza != null) PieceComponent(piece = pieza) }
                }
            }

            // 4. CEMENTERIO JUGADOR
            Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth().height(50.dp).background(Color(0xFF2C2C2C)).border(1.dp, Color.Gray).padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("TÚ:", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(end = 8.dp))
                    LazyRow(modifier = Modifier.weight(1f)) {
                        items(capturadasBlancas) { piece -> Box(Modifier.size(40.dp)) { PieceComponent(piece) } }
                    }
                    Text("Pts: $puntosBlancas", color = Color.Yellow, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
    }
}