package com.jasc.jascbattlechess.ui

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager as AndroidBluetoothManager
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.jasc.jascbattlechess.data.*
import com.jasc.jascbattlechess.viewmodel.BoardViewModel
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
enum class ModoJuego { CONTRA_IA, MULTIJUGADOR }

@Suppress("SpellCheckingInspection")
@SuppressLint("MissingPermission")
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
    val puntajeTotalJugador = viewModel.puntosJugadorTotal + viewModel.puntosCapturasJugador

    var esperandoOponente by remember { mutableStateOf(false) }
    var esDispositivoBuscador by remember { mutableStateOf(false) }
    var mostrarDispositivos by remember { mutableStateOf(false) }
    var dispositivosVinculados by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }

    val context = LocalContext.current

    val bluetoothSystemManager = remember { context.getSystemService(AndroidBluetoothManager::class.java) }
    val bluetoothAdapter: BluetoothAdapter? = bluetoothSystemManager?.adapter

    val permisosRequeridos = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val launcherPermisos = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    fun verificarYEjecutar(onConcedido: () -> Unit) {
        val faltan = permisosRequeridos.any { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }
        if (faltan) launcherPermisos.launch(permisosRequeridos) else onConcedido()
    }

    LaunchedEffect(viewModel.modoJuegoActivo) {
        if (viewModel.modoJuegoActivo == ModoJuego.CONTRA_IA) {
            viewModel.desconectarBluetooth()
            esperandoOponente = false
            selectedPosition = null
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF1A1A1A))) {
        var showHelp by remember { mutableStateOf(false) }

        Column(modifier = Modifier.fillMaxSize()) {
            if (showHelp) {
                AlertDialog(
                    onDismissRequest = { showHelp = false },
                    title = {
                        Text(
                            "Cómo jugar JascBattleChess",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2) // Tu azul hermoso
                        )
                    },
                    text = {
                        // Ponemos el scroll directo en la Column con un tamaño fijo para que no se desborde
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp) // Le damos una altura fija para obligar a que aparezca el scroll
                                .verticalScroll(rememberScrollState()) // Permite deslizar hacia abajo con el dedo
                        ) {
                            Text("1. Haz clic en la pieza que quieres mover; se pondrá más grande y luego selecciona la casilla destino o el oponente a golpear.")

                            Spacer(modifier = Modifier.height(12.dp))

                            Text("2. Reglas de ataque (Vida y Resistencia):", fontWeight = FontWeight.Bold)
                            Text("- Reina y Rey: Capturan al enemigo con 1 golpe.")
                            Text("- Torre: Necesita 2 golpes para derrotar.")
                            Text("- Caballo: Necesita 3 golpes para derrotar.")
                            Text("- Alfil: Necesita 4 golpes para derrotar.")
                            Text("- Peón: Necesita 5 golpes para derrotar.")

                            Spacer(modifier = Modifier.height(12.dp))

                            Text("3. Control del Tablero:", fontWeight = FontWeight.Bold)
                            Text("- Pellizco (Pinch): Usa dos dedos para acercar o alejar el campo de batalla 3D.")
                            Text("- Paneo (Arrastrar): Desplaza el tablero con dos dedos para ajustar el enfoque.")

                            Spacer(modifier = Modifier.height(12.dp))

                            Text("4. Partidas por Bluetooth:", fontWeight = FontWeight.Bold)
                            Text("- Vincula los dispositivos en los ajustes de tu celular.")
                            Text("- Un jugador debe 'Crear Sala' y el otro 'Buscar Rival'. La IA se desactiva y juegan en turnos estrictos sincronizados.")

                            Spacer(modifier = Modifier.height(12.dp))

                            Text("5. El objetivo es derrotar al Rey enemigo (Jaque Mate).")
                            Text("- Al concluir, puedes arrastrar la tarjeta flotante de resultados para inspeccionar el estado final del tablero.")

                            Spacer(modifier = Modifier.height(20.dp))
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(12.dp))

                            // Tus datos de contacto garantizados al final del scroll
                            Text("Creado por Jairo Salazar Castaño", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("📞 Teléfono: 3016173378", fontSize = 12.sp, color = Color.Gray)
                            Text("✉️ Correo: jairosypunto@gmail.com", fontSize = 12.sp, color = Color.Gray)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showHelp = false }) {
                            Text("Entendido", color = Color(0xFF1976D2), fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            if (!isLandscape) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { if (viewModel.modoJuegoActivo == ModoJuego.CONTRA_IA) viewModel.retrocederJugada() },
                        enabled = viewModel.modoJuegoActivo == ModoJuego.CONTRA_IA
                    ) {
                        Text("⬅️ Deshacer", fontSize = 12.sp, color = if(viewModel.modoJuegoActivo == ModoJuego.CONTRA_IA) Color.Unspecified else Color.Gray)
                    }

                    Text(
                        if (esperandoOponente) "⏳ Esperando rival..." else boardState.mensajeEstado,
                        color = if (esperandoOponente) Color.Yellow else Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = {
                        viewModel.resetearJuego()
                        esperandoOponente = false
                        selectedPosition = null
                        viewModel.modoJuegoActivo = ModoJuego.CONTRA_IA
                    }) { Text("🔄 Reset", fontSize = 12.sp) }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .background(Color(0xFF2C2C2C)),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Modo de Juego:", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))

                    RadioButton(
                        selected = viewModel.modoJuegoActivo == ModoJuego.CONTRA_IA,
                        onClick = { viewModel.modoJuegoActivo = ModoJuego.CONTRA_IA }
                    )
                    Text("Contra IA", color = Color.White, fontSize = 12.sp, modifier = Modifier.clickable { viewModel.modoJuegoActivo = ModoJuego.CONTRA_IA })

                    RadioButton(
                        selected = viewModel.modoJuegoActivo == ModoJuego.MULTIJUGADOR,
                        onClick = { viewModel.modoJuegoActivo = ModoJuego.MULTIJUGADOR }
                    )
                    Text("Por Bluetooth", color = Color.White, fontSize = 12.sp, modifier = Modifier.clickable { viewModel.modoJuegoActivo = ModoJuego.MULTIJUGADOR })
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = { showHelp = true }, modifier = Modifier.weight(1f).padding(end = 4.dp)) {
                        Text("Ayuda", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            verificarYEjecutar {
                                esperandoOponente = true
                                viewModel.iniciarServidor { _ -> esperandoOponente = false }
                            }
                        },
                        enabled = viewModel.modoJuegoActivo == ModoJuego.MULTIJUGADOR,
                        colors = ButtonDefaults.buttonColors(containerColor = if (esperandoOponente) Color.Green else Color(0xFF1976D2)),
                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                    ) {
                        Text(if (esperandoOponente) "Sala Activa" else "Crear Sala", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            verificarYEjecutar {
                                dispositivosVinculados = bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
                                if (dispositivosVinculados.isNotEmpty()) {
                                    mostrarDispositivos = true
                                } else {
                                    Toast.makeText(context, "Vincula tus dispositivos físicos primero en Ajustes", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = viewModel.modoJuegoActivo == ModoJuego.MULTIJUGADOR,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                        modifier = Modifier.weight(1f).padding(start = 4.dp)
                    ) {
                        Text("Buscar Rival", fontSize = 12.sp)
                    }
                }
            }

            val screenWidth = LocalConfiguration.current.screenWidthDp
            val screenHeight = LocalConfiguration.current.screenHeightDp
            val cellSize = minOf((screenWidth - 60) / 8f, (screenHeight - 180) / 8f).dp

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
                            offsetX += pan.x; offsetY += pan.y
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            rotationX = 18f; scaleX = scaleBoard; scaleY = scaleBoard
                            translationX = offsetX; translationY = offsetY
                        }
                        .zIndex(20f)
                ) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            ('A'..'H').forEach { letra -> Text(letra.toString(), color = Color.White) }
                        }

                        Row {
                            Column(verticalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.height(cellSize * 8)) {
                                (1..8).forEach { numero -> Text(numero.toString(), color = Color.White) }
                            }

                            LazyVerticalGrid(
                                columns = GridCells.Fixed(8),
                                modifier = Modifier.background(Color.Transparent),
                                verticalArrangement = Arrangement.Center
                            ) {
                                items(64) { index ->
                                    val miEquipo = viewModel.miEquipoNet
                                    val esInvertido = (viewModel.modoJuegoActivo == ModoJuego.MULTIJUGADOR && miEquipo == Team.NEGRO)

                                    // 1. Coordenadas lógicas puras de la matriz real
                                    val realX = index / 8
                                    val realY = index % 8

                                    // 2. Coordenadas de visualización espejo en la pantalla
                                    val x = if (esInvertido) 7 - realX else realX
                                    val y = if (esInvertido) 7 - realY else realY

                                    val currentPos = Position(x, y)
                                    val isSelected = selectedPosition == currentPos

                                    // Buscamos la pieza sin filtrar por vida para que el Rey derrotado no sea null
                                    val pieza = boardState.pieces.find { it.position == currentPos }
                                    val scalePiece by animateFloatAsState(if (isSelected) 2.4f else 1.7f)

                                    Box(
                                        modifier = Modifier
                                            .size(cellSize)
                                            .background(
                                                when {
                                                    // El Rey conserva su color rojo al morir
                                                    pieza?.type == PieceType.REY && pieza.health <= 0 -> Color.Red.copy(alpha = 0.5f)
                                                    (x + y) % 2 != 0 -> Color(0xFFB58863)
                                                    else -> Color(0xFFF0D9B5)
                                                }
                                            )
                                            .clickable {
                                                if (viewModel.modoJuegoActivo == ModoJuego.MULTIJUGADOR) {
                                                    // En modo Bluetooth, usamos el equipo real de la red (miEquipoNet)
                                                    val bandoLocal = viewModel.miEquipoNet
                                                    if (bandoLocal != null && boardState.turn == bandoLocal) {
                                                        if (selectedPosition == null) {
                                                            // Selecciona tu pieza (sea blanca o negra, la que te haya tocado en ese celular)
                                                            if (pieza != null && pieza.team == bandoLocal && pieza.health > 0) {
                                                                selectedPosition = currentPos
                                                            }
                                                        } else {
                                                            val origen = selectedPosition!!
                                                            viewModel.intentarMovimiento(origen, currentPos, esRemoto = false)
                                                            selectedPosition = null
                                                        }
                                                    }
                                                } else {
                                                    // Modo Contra IA
                                                    if (selectedPosition == null) {
                                                        if (pieza != null && pieza.team == Team.BLANCAS && pieza.health > 0) {
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
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
// Dibujamos si la pieza está viva O si es el Rey caído
                                        if (pieza != null && (pieza.health > 0 || pieza.type == PieceType.REY)) {
// DEJA ESTO EXACTAMENTE ASÍ EN TU TABLERO (BattleScreen.kt)
                                            PieceComponent(
                                                piece = pieza,
                                                miEquipo = viewModel.miEquipoNet, // 👈 Pasa el del ViewModel directo sin filtros 'if' raros
                                                modifier = Modifier
                                                    .scale(if (pieza.team == Team.BLANCAS) scalePiece else scalePiece * 0.7f)
                                                    .zIndex(30f)
                                                    .graphicsLayer {
                                                        alpha = if (pieza.type == PieceType.REY && pieza.health <= 0) 0.7f else 1.0f
                                                    }
                                                    .offset(y = if (pieza.team == Team.BLANCAS) (-cellSize.value * 0.1f).dp else (-cellSize.value * 0.05f).dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!isLandscape) {
// 🟢 Fila de Capturadas de la IA / Rival
                Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                    Text(if(viewModel.modoJuegoActivo == ModoJuego.CONTRA_IA) "IA (Capturadas):" else "Rival (Capturadas):", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth().height(50.dp).background(Color(0xFF2C2C2C)).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        LazyRow(modifier = Modifier.weight(1f)) {
                            items(capturadasNegras) { piece ->
                                Box(Modifier.size(40.dp)) {
                                    // Se añade miEquipo = viewModel.miEquipoNet
                                    PieceComponent(piece = piece, miEquipo = viewModel.miEquipoNet)
                                }
                            }
                        }
                        Text("Pts: ${viewModel.puntosIATotal + viewModel.puntosCapturasIA}", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

// 🟢 Fila de Capturadas de TÚ
                Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().height(50.dp).background(Color(0xFF2C2C2C)).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("TÚ:", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(end = 8.dp))
                        LazyRow(modifier = Modifier.weight(1f)) {
                            items(capturadasBlancas) { piece ->
                                Box(Modifier.size(40.dp)) {
                                    // Se añade miEquipo = viewModel.miEquipoNet
                                    PieceComponent(piece = piece, miEquipo = viewModel.miEquipoNet)
                                }
                            }
                        }
                        Text("Pts: $puntajeTotalJugador", color = Color.Yellow, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.Center) {
                    NivelIA.entries.forEach { nivel ->
                        Button(
                            onClick = { if (viewModel.modoJuegoActivo == ModoJuego.CONTRA_IA) viewModel.nivelIA = nivel },
                            enabled = viewModel.modoJuegoActivo == ModoJuego.CONTRA_IA,
                            modifier = Modifier.padding(horizontal = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (viewModel.modoJuegoActivo == ModoJuego.CONTRA_IA && viewModel.nivelIA == nivel) Color.Red else Color.DarkGray)
                        ) {
                            Text(nivel.name.take(3), fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        if (mostrarDispositivos) {
            AlertDialog(
                onDismissRequest = { mostrarDispositivos = false },
                title = { Text("Selecciona tu dispositivo rival") },
                text = {
                    Column {
                        dispositivosVinculados.forEach { dispositivo ->
                            Text(
                                text = dispositivo.name ?: "Dispositivo desconocido",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        mostrarDispositivos = false
                                        // 🟢 CORRECCIÓN: Cambiado a la variable local de la interfaz
                                        esDispositivoBuscador = true
                                        viewModel.conectarAConversor(dispositivo) { _ -> }
                                    }
                                    .padding(vertical = 12.dp),
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            HorizontalDivider()
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { mostrarDispositivos = false }) { Text("Cancelar") } }
            )
        }

        if (boardState.esJaqueMate || boardState.esTablas) {
            // Variables para recordar la posición en la que el usuario mueva la tarjeta
            var tarjetaOffsetX by remember { mutableFloatStateOf(0f) }
            var tarjetaOffsetY by remember { mutableFloatStateOf(0f) }

            // Contenedor invisible para que no bloquee ni oscurezca el tablero trasero
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .wrapContentHeight()
                        // 🟢 AQUÍ ESTÁ LA MAGIA: Permitimos moverla libremente por la pantalla
                        .offset { androidx.compose.ui.unit.IntOffset(tarjetaOffsetX.toInt(), tarjetaOffsetY.toInt()) }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan: Offset, _, _ ->
                                tarjetaOffsetX += pan.x
                                tarjetaOffsetY += pan.y
                            }
                        },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF222222).copy(alpha = 0.95f) // Un toque de transparencia para ver a través si se desea
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Título Destacado (Tu azul hermoso)
                        Text(
                            text = "¡Partida Concluida!",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1976D2),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Nota sutil para guiar al usuario
                        Text(
                            text = "(Puedes arrastrar esta tarjeta para ver el tablero)",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Estado de la partida
                        Text(
                            text = boardState.mensajeEstado,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Marcadores de Puntuación
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("🏳️ Blancas:", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                            Text("$puntajeTotalJugador pts", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("🏴 Negras:", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                            Text("${viewModel.puntosIATotal + viewModel.puntosCapturasIA} pts", color = Color(0xFFF44336), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        // Sección del Video de Recompensa si aplica
                        if (puntajeTotalJugador >= 1000) {
                            Spacer(modifier = Modifier.height(16.dp))
                            val videoRecurso = viewModel.obtenerVideoRecompensa(puntajeTotalJugador)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .background(Color.Black, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                            ) {
                                VideoPlayerComponent(resId = videoRecurso, modifier = Modifier.fillMaxSize())
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Botón de Acción Principal (Estilo Azul)
                        Button(
                            onClick = { selectedPosition = null; viewModel.siguientePartida() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                        ) {
                            Text("Seguir jugando", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Botón Secundario
                        TextButton(
                            onClick = {
                                selectedPosition = null
                                viewModel.resetearJuego()
                                esperandoOponente = false
                                viewModel.modoJuegoActivo = ModoJuego.CONTRA_IA
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Reiniciar Torneo", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerComponent(resId: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val exoPlayer = remember { ExoPlayer.Builder(context).build().apply { repeatMode = ExoPlayer.REPEAT_MODE_ONE; playWhenReady = true } }
    LaunchedEffect(resId) {
        val videoUri = "android.resource://${context.packageName}/$resId".toUri()
        exoPlayer.setMediaItem(MediaItem.fromUri(videoUri))
        exoPlayer.prepare()
    }
    DisposableEffect(Unit) { onDispose { exoPlayer.release() } }
    AndroidView(factory = { ctx -> PlayerView(ctx).apply { player = exoPlayer; useController = true; setShowNextButton(false); setShowPreviousButton(false) } }, modifier = modifier)
}