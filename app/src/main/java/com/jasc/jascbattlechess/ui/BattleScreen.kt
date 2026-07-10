package com.jasc.jascbattlechess.ui

import android.content.res.Configuration
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.jasc.jascbattlechess.data.*
import com.jasc.jascbattlechess.viewmodel.BoardViewModel

@Composable
fun BattleScreen(
    modifier: Modifier = Modifier,
    viewModel: BoardViewModel = viewModel()
) {
    val boardState by viewModel.boardState.collectAsState()
    var selectedPosition by remember { mutableStateOf<Position?>(null) }

    val config = LocalConfiguration.current
    val isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Filtrado de piezas muertas únicamente para renderizar visualmente el cementerio (iconos)
    val capturadasNegras = boardState.pieces.filter { it.health <= 0 && it.team == Team.NEGRO }
    val capturadasBlancas = boardState.pieces.filter { it.health <= 0 && it.team == Team.BLANCAS }

    // El puntaje total del jugador sumando victorias directas (100 pts) + capturas acumuladas
    val puntajeTotalJugador = viewModel.puntosJugadorTotal + viewModel.puntosCapturasJugador

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF1A1A1A))) {
        var showHelp by remember { mutableStateOf(false) }

        Column(modifier = Modifier.fillMaxSize()) {
            if (showHelp) {
                AlertDialog(
                    onDismissRequest = { showHelp = false },
                    title = { Text("Cómo jugar JascBattleChess") },
                    text = {
                        Column {
                            Text("1. Haz clic en la pieza que quieres mover; se pondrá más grande y luego selecciona la casilla destino.")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("2. Reglas de ataque:")
                            Text("- Reina y Rey: capturan al enemigo con 1 golpe.")
                            Text("- Torre: necesita 2 golpes.")
                            Text("- Caballo: necesita 3 golpes.")
                            Text("- Alfil: necesita 4 golpes.")
                            Text("- Peón: necesita 5 golpes.")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("3. El objetivo es derrotar al Rey enemigo.")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("4. Zoom y movimiento del tablero:")
                            Text("- Pellizca con dos dedos para acercar o alejar.")
                            Text("- Arrastra con un dedo para mover el tablero.")
                            Text("- También puedes usar los botones + Zoom y - Zoom debajo del tablero.")
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                "Creado por Jairo Salazar Castaño\nIngeniero en Sistemas\nApasionado por el código y la estrategia\n3016173378",
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
            var scaleBoard by remember { mutableFloatStateOf(1f) }
            var offsetX by remember { mutableFloatStateOf(0f) }
            var offsetY by remember { mutableFloatStateOf(0f) }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFF2E7D32))
                    .padding(bottom = 40.dp, start = 36.dp, end = 36.dp)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan: Offset, zoom: Float, _ ->
                            scaleBoard = (scaleBoard * zoom).coerceIn(0.8f, 2.0f)
                            offsetX += pan.x
                            offsetY += pan.y
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // 🔹 Tablero con transformaciones
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            rotationX = 18f
                            scaleX = scaleBoard
                            scaleY = scaleBoard
                            translationX = offsetX
                            translationY = offsetY
                        }
                        .zIndex(20f)
                ) {
                    Column {
                        // Letras A–H arriba
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ('A'..'H').forEach { letra ->
                                Text(letra.toString(), color = Color.White)
                            }
                        }

                        Row {
                            // Números 1–8 a la izquierda
                            Column(
                                verticalArrangement = Arrangement.SpaceEvenly,
                                modifier = Modifier.height(cellSize * 8)
                            ) {
                                (1..8).forEach { numero ->
                                    Text(numero.toString(), color = Color.White)
                                }
                            }

                            // 🔹 El tablero
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(8),
                                modifier = Modifier.background(Color.Transparent),
                                verticalArrangement = Arrangement.Center
                            ) {
                                items(64) { index ->
                                    val x = index / 8
                                    val y = index % 8
                                    val currentPos = Position(x, y)
                                    val isSelected = selectedPosition == currentPos
                                    val pieza = boardState.pieces.find { it.position == currentPos }

                                    val scalePiece by animateFloatAsState(if (isSelected) 2.4f else 1.7f)

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
                                            PieceComponent(
                                                piece = pieza,
                                                modifier = Modifier
                                                    .scale(
                                                        when (pieza.team) {
                                                            Team.BLANCAS -> scalePiece
                                                            Team.NEGRO -> scalePiece * 0.7f
                                                        }
                                                    )
                                                    .zIndex(30f)
                                                    .offset(
                                                        y = when (pieza.team) {
                                                            Team.BLANCAS -> (-cellSize.value * 0.1f).dp
                                                            Team.NEGRO -> (-cellSize.value * 0.05f).dp
                                                        }
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 🔹 Controles de zoom manual
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
                        // ✅ Vinculado a los puntos persistentes del ViewModel
                        Text("Pts: ${viewModel.puntosIATotal + viewModel.puntosCapturasIA}", color = Color.White, fontWeight = FontWeight.Bold)
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
                        // ✅ Vinculado al acumulador total real del jugador
                        Text("Pts: $puntajeTotalJugador", color = Color.Yellow, fontWeight = FontWeight.Bold, fontSize = 18.sp)
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

        // 🏆 CUADRO DE DIÁLOGO FLOTANTE AL TERMINAR LA PARTIDA 🏆
        if (boardState.esJaqueMate || boardState.esTablas) {
            AlertDialog(
                onDismissRequest = { /* Forzar la interacción del usuario */ },
                title = { Text(text = "¡Partida Concluida!", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(text = boardState.mensajeEstado, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Marcador Acumulado del Torneo:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(text = "🏆 Tú: $puntajeTotalJugador pts", color = Color(0xFF388E3C), fontWeight = FontWeight.Bold)
                        Text(text = "🤖 IA: ${viewModel.puntosIATotal + viewModel.puntosCapturasIA} pts", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)

                        // 🎁 RECOMPENSA CINEMATOGRÁFICA EN VIVO CADA 1000 PUNTOS
                        if (puntajeTotalJugador >= 1000) {
                            val nivelPremio = puntajeTotalJugador / 1000
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "🎉 ¡Video de Felicitación Desbloqueado! Rango: Nivel $nivelPremio",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            // Selecciona dinámicamente tu recurso de video desde la carpeta raw
                            val videoRecurso = viewModel.obtenerVideoRecompensa(puntajeTotalJugador)

                            // Carga el componente nativo de ExoPlayer integrado en la UI
                            VideoPlayerComponent(
                                resId = videoRecurso,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .background(Color.Black)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            selectedPosition = null
                            viewModel.siguientePartida()
                        }
                    ) {
                        Text("Seguir jugando")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            selectedPosition = null
                            viewModel.resetearJuego()
                        }
                    ) {
                        Text("Reiniciar Torneo", color = Color.Gray)
                    }
                }
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerComponent(resId: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // Inicializar y recordar ExoPlayer para evitar recreaciones costosas
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = ExoPlayer.REPEAT_MODE_ONE // Bucle continuo
            playWhenReady = true
        }
    }

    // Escuchar cambios de archivo/recurso para actualizar el stream del video
    LaunchedEffect(resId) {
        val videoUri = Uri.parse("android.resource://${context.packageName}/$resId")
        val mediaItem = MediaItem.fromUri(videoUri)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
    }

    // Limpieza de memoria al destruir o cerrar el cuadro de diálogo
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Integración del PlayerView clásico de Android mediante interoperabilidad
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true // Controles multimedia visibles (Play/Pause/Barra)
                setShowNextButton(false)
                setShowPreviousButton(false)
            }
        },
        modifier = modifier
    )
}