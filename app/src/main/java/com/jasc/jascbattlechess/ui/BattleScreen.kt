package com.jasc.jascbattlechess.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jasc.jascbattlechess.data.*
import com.jasc.jascbattlechess.viewmodel.BoardViewModel
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.scale
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset   // ✅ Import necesario para detectTransformGestures



@Composable
fun BattleScreen(
    modifier: Modifier = Modifier,
    viewModel: BoardViewModel = viewModel()
) {
    val boardState by viewModel.boardState.collectAsState()
    var selectedPosition by remember { mutableStateOf<Position?>(null) }

    val config = LocalConfiguration.current
    val isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE

    val capturadasNegras = boardState.pieces.filter { it.health <= 0 && it.team == Team.NEGRO }
    val capturadasBlancas = boardState.pieces.filter { it.health <= 0 && it.team == Team.BLANCAS }


    val puntosIA = capturadasBlancas.sumOf { p ->
        when (p.type) {
            PieceType.REY -> 100
            PieceType.REINA -> 100
            PieceType.TORRE -> 50
            PieceType.CABALLO -> 34
            PieceType.ALFIL -> 25
            PieceType.PEON -> 20
        }.toInt()
    }

    val puntosBlancas = capturadasNegras.sumOf { p ->
        when (p.type) {
            PieceType.REY -> 100
            PieceType.REINA -> 100
            PieceType.TORRE -> 50
            PieceType.CABALLO -> 34
            PieceType.ALFIL -> 25
            PieceType.PEON -> 20
        }.toInt()
    }


    Box(modifier = modifier.fillMaxSize().background(Color(0xFF1A1A1A))) {
        var showHelp by remember { mutableStateOf(false) }

        Column(modifier = Modifier.fillMaxSize()) {
            if (showHelp) {
                AlertDialog(
                    onDismissRequest = { showHelp = false },
                    title = { Text("Cómo jugar JascBattleChess") },
                    text = {
                        Column {
                            Text("1. Haz clic en la pieza que quieres mover; se pondrá más grande de lo normal y luego selecciona la casilla destino.")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("2. Reglas de ataque:")
                            Text("- Reina y Rey: capturan al enemigo con 1 golpe.")
                            Text("- Torre: necesita 2 golpes.")
                            Text("- Caballo: necesita 3 golpes.")
                            Text("- Alfil: necesita 4 golpes.")
                            Text("- Peón: necesita 5 golpes.")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("3. El objetivo es derrotar al Rey enemigo.")
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                "Creado por Jairo Salazar Castaño\nIngeniero en Sistemas, \nApasionado por el código y la estrategia y en continuo aprendizaje \n 3016173378",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showHelp = false }) {
                            Text("Entendido")
                        }
                    }
                )
            }

            // Controles solo en portrait
            if (!isLandscape) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { viewModel.retrocederJugada() }) { Text("⬅️ Deshacer", fontSize = 12.sp) }
                    Text(
                        boardState.mensajeEstado,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    TextButton(onClick = { viewModel.resetearJuego() }) { Text("🔄 Reset", fontSize = 12.sp) }
                }
                Button(onClick = { showHelp = true }) {
                    Text("Ayuda")
                }
            }

// 🔹 Calcula dimensiones de pantalla
            val screenWidth = LocalConfiguration.current.screenWidthDp
            val screenHeight = LocalConfiguration.current.screenHeightDp

// 🔹 Tamaño de cada celda del tablero
            val cellSize = minOf(
                (screenWidth - 60) / 8f,
                (screenHeight - 180) / 8f
            ).dp

// 🔹 Estado del zoom y desplazamiento
            var scaleBoard by remember { mutableFloatStateOf(1f) }   // ✅ zoom
            var offsetX by remember { mutableFloatStateOf(0f) }      // ✅ desplazamiento horizontal
            var offsetY by remember { mutableFloatStateOf(0f) }      // ✅ desplazamiento vertical

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFF2E7D32))
                    .padding(bottom = 40.dp, start = 36.dp, end = 36.dp)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan: Offset, zoom: Float, _ ->
                            // Zoom con pellizco
                            scaleBoard = (scaleBoard * zoom).coerceIn(0.8f, 2.5f)
                            // Arrastre con la yema del dedo
                            offsetX += pan.x
                            offsetY += pan.y
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(8),
                    modifier = Modifier.graphicsLayer {
                        rotationX = 18f   // inclinación estilo videojuego
                        scaleY = 0.9f     // aplana un poco en vertical
                        scaleX = scaleBoard
                        scaleY = scaleBoard
                        translationX = offsetX   // ✅ mueve horizontal
                        translationY = offsetY   // ✅ mueve vertical
                    },
                    verticalArrangement = Arrangement.Center
                ) {
                    items(64) { index ->
                        val x = index / 8
                        val y = index % 8
                        val currentPos = Position(x, y)
                        val isSelected = selectedPosition == currentPos
                        val pieza = boardState.pieces.find { it.position == currentPos }

                        val scalePiece by animateFloatAsState(if (isSelected) 2.6f else 1.4f)

                        Box(
                            modifier = Modifier
                                .size(cellSize)
                                .background(
                                    when {
                                        pieza?.type == PieceType.REY && pieza.health <= 0 ->
                                            Color.Red.copy(alpha = 0.5f)
                                        (x + y) % 2 != 0 -> Color(0xFFB58863)
                                        else -> Color(0xFFF0D9B5)
                                    }
                                )
                                .clickable(enabled = boardState.turn == Team.BLANCAS) {
                                    if (selectedPosition == null) {
                                        if (pieza != null && pieza.team == Team.BLANCAS) {
                                            selectedPosition = currentPos
                                        }
                                    } else {
                                        val origen = selectedPosition!!
                                        viewModel.intentarMovimiento(origen, currentPos)
                                        val nuevoEstado = viewModel.boardState.value
                                        val enemigoEnDestino = nuevoEstado.pieces.find {
                                            it.position == currentPos && it.team == Team.NEGRO && it.health > 0
                                        }
                                        selectedPosition = if (enemigoEnDestino != null) origen else null
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (pieza != null) {
                                val isKingDefeated = pieza.type == PieceType.REY && pieza.health <= 0
                                val shake = remember { Animatable(0f) }

                                LaunchedEffect(isKingDefeated) {
                                    if (isKingDefeated) {
                                        shake.animateTo(8f, animationSpec = tween(durationMillis = 100))
                                        shake.animateTo(-8f, animationSpec = tween(durationMillis = 100))
                                        shake.animateTo(0f, animationSpec = tween(durationMillis = 100))
                                    }
                                }

                                PieceComponent(
                                    piece = pieza,
                                    modifier = Modifier
                                        .scale(scalePiece)
                                        .offset(x = shake.value.dp)
                                )
                            }
                        }
                    }
                }
            }

// 🔹 Controles de zoom manual (añadir debajo del Box del tablero)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = { scaleBoard = (scaleBoard + 0.2f).coerceAtMost(2.5f) },
                    modifier = Modifier.padding(horizontal = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) {
                    Text("+ Zoom", color = Color.White)
                }

                Button(
                    onClick = { scaleBoard = (scaleBoard - 0.2f).coerceAtLeast(0.8f) },
                    modifier = Modifier.padding(horizontal = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) {
                    Text("- Zoom", color = Color.White)
                }
            }











            // Cementerios y puntajes solo en portrait (parte inferior)
            if (!isLandscape) {
                // Cementerio IA
                Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                    Text("IA (Capturadas):", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                            .background(Color(0xFF2C2C2C)).padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LazyRow(modifier = Modifier.weight(1f)) {
                            items(capturadasNegras) { piece -> Box(Modifier.size(40.dp)) { PieceComponent(piece) } }
                        }
                        Text("Pts: $puntosIA", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                // Cementerio jugador
                Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                            .background(Color(0xFF2C2C2C)).padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("TÚ:", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(end = 8.dp))
                        LazyRow(modifier = Modifier.weight(1f)) {
                            items(capturadasBlancas) { piece -> Box(Modifier.size(40.dp)) { PieceComponent(piece) } }
                        }
                        Text("Pts: $puntosBlancas", color = Color.Yellow, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }

                // Botones IA
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    NivelIA.entries.forEach { nivel ->
                        Button(
                            onClick = { viewModel.nivelIA = nivel },
                            modifier = Modifier.padding(horizontal = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewModel.nivelIA == nivel) Color.Red else Color.DarkGray
                            )
                        ) {
                            Text(nivel.name.take(3), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
