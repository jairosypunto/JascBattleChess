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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.shape.RoundedCornerShape
import com.jasc.jascbattlechess.ui.theme.TemaAjedrez
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
// Al inicio de tu BattleScreen:
    var temaConfigurado by remember { mutableStateOf(TemaAjedrez.AZUL_HERMOSO) }
    val config = LocalConfiguration.current
    val isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE

    val capturadasNegras = boardState.pieces.filter { it.health <= 0 && it.team == Team.NEGRO }
    val capturadasBlancas = boardState.pieces.filter { it.health <= 0 && it.team == Team.BLANCAS }
    val puntajeTotalJugador = viewModel.puntosJugadorTotal + viewModel.puntosCapturasJugador

    var esperandoOponente by remember { mutableStateOf(false) }
    var esDispositivoBuscador by remember { mutableStateOf(false) }
    var mostrarDispositivos by remember { mutableStateOf(false) }
    var dispositivosVinculados by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var limpiarRastroManual by remember { mutableStateOf(false) }

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
        val faltan = permisosRequeridos.any {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (faltan) launcherPermisos.launch(permisosRequeridos) else onConcedido()
    }

    LaunchedEffect(viewModel.modoJuegoActivo) {
        if (viewModel.modoJuegoActivo == ModoJuego.CONTRA_IA) {
            viewModel.desconectarBluetooth()
            esperandoOponente = false
            selectedPosition = null
        }
    }

    // Usamos el Box raíz para que la tarjeta flotante pueda superponerse sin romper el flujo
    Box(modifier = modifier.fillMaxSize().background(Color(0xFF1A1A1A))) {
        var showHelp by remember { mutableStateOf(false) }

        // Contenedor principal de la interfaz del juego
        Column(modifier = Modifier.fillMaxSize()) {
            if (showHelp) {
                AlertDialog(
                    onDismissRequest = { showHelp = false },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "🎮 Cómo jugar JascBattleChess",
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                color = temaConfigurado.principal
                            )
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp) // Alto ideal para scroll cómodo
                                .verticalScroll(rememberScrollState())
                        ) {
                            // ===================================================
                            // 🕹️ SECCIÓN 1: CONTROLES BÁSICOS
                            // ===================================================
                            Text("1. Controles de Juego", fontWeight = FontWeight.Bold, color = temaConfigurado.principal)
                            Text("• Seleccionar: Toca una pieza; se agrandará para indicar que está activa.")
                            Text("• Acciones: Selecciona una casilla vacía para moverte, o toca a un oponente directo para iniciar un combate.")

                            Spacer(modifier = Modifier.height(16.dp))

                            // ===================================================
                            // 🔤 SECCIÓN 2: IDENTIFICACIÓN DE PIEZAS (DICCIONARIO + MINIATURAS)
                            // ===================================================
                            Text("2. Guía Visual de Piezas", fontWeight = FontWeight.Bold, color = temaConfigurado.principal)
                            Text("En el tablero verás etiquetas flotantes para identificar cada rango:", fontSize = 13.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))

                            val guiaPiezas = listOf(
                                Triple("👑", "R", "Rey (Captura de 1 golpe)"),
                                Triple("👸", "D", "Dama / Reina (Captura de 1 golpe)"),
                                Triple("🏰", "T", "Torre (Necesita 2 golpes)"),
                                Triple("🐴", "C", "Caballo (Necesita 3 golpes)"),
                                Triple("♝", "A", "Alfil (Necesita 4 golpes)"),
                                Triple("🛡️", "P", "Peón (Necesita 5 golpes)")
                            )

                            guiaPiezas.forEach { (emoji, letra, descripcion) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(emoji, fontSize = 18.sp, modifier = Modifier.width(28.dp))

                                    Text(
                                        text = " $letra ",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        color = Color.White,
                                        modifier = Modifier
                                            .background(temaConfigurado.principal, shape = androidx.compose.foundation.shape.RoundedCornerShape(3.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(descripcion, fontSize = 14.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // ===================================================
                            // 🎨 🆕 SECCIÓN 3: PERSONALIZACIÓN VISUAL (¡AGREGADO!)
                            // ===================================================
                            Text("3. Personalización de Colores", fontWeight = FontWeight.Bold, color = temaConfigurado.principal)
                            Text("• Cambiar Tema: Toca el botón superior '🎨 Tema' para alternar instantáneamente entre los estilos visuales.")
                            Text("• Adaptación Total: Cada tema cambiará dinámicamente el color de las casillas claras y oscuras, los botones, los indicadores de texto y el tapete del fondo exterior.")

                            Spacer(modifier = Modifier.height(16.dp))

                            // ===================================================
                            // 🗺️ SECCIÓN 4: GESTOS DEL CAMPO VERTICAL
                            // ===================================================
                            Text("4. Control de Cámara 3D", fontWeight = FontWeight.Bold, color = temaConfigurado.principal)
                            Text("• Zoom (Pellizco): Pellizca con dos dedos sobre el área exterior del tablero para acercar o alejar la perspectiva.")
                            Text("• Desplazar (Arrastrar): Usa dos dedos al mismo tiempo para mover el enfoque del campo de batalla.")

                            Spacer(modifier = Modifier.height(16.dp))

                            // ===================================================
                            // 📡 SECCIÓN 5: BLUETOOTH Y OBJETIVO
                            // ===================================================
                            Text("5. Multijugador Bluetooth", fontWeight = FontWeight.Bold, color = temaConfigurado.principal)
                            Text("• Vincula previamente ambos celulares en los ajustes de Android.")
                            Text("• Un jugador debe presionar 'Crear Sala' y el otro 'Buscar Rival' para sincronizar turnos estrictos en red.")

                            Spacer(modifier = Modifier.height(16.dp))
                            Text("6. Victoria", fontWeight = FontWeight.Bold, color = temaConfigurado.principal)
                            Text("• El objetivo absoluto es fulminar al Rey enemigo (Jaque Mate). Al terminar, puedes arrastrar la tarjeta de resultados para auditar el tablero final.")

                            // ===================================================
                            // 🪪 SECCIÓN 6: CRÉDITOS Y CONTACTO
                            // ===================================================
                            Spacer(modifier = Modifier.height(24.dp))
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(12.dp))

                            Card(
                                colors = androidx.compose.material3.CardDefaults.cardColors(
                                    containerColor = temaConfigurado.principal.copy(alpha = 0.04f)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = temaConfigurado.principal.copy(alpha = 0.15f)
                                ),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Jairo Salazar Castaño",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp,
                                        color = temaConfigurado.principal
                                    )
                                    Text(
                                        text = "Ingeniero en Sistemas",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = Color.DarkGray.copy(alpha = 0.8f),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )

                                    Text(
                                        text = "🧠 Apasionado por la estrategia y el código",
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(top = 6.dp, bottom = 10.dp)
                                    )

                                    HorizontalDivider(color = temaConfigurado.principal.copy(alpha = 0.1f))
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    ) {
                                        Text("📞", fontSize = 12.sp, modifier = Modifier.width(22.dp))
                                        Text(
                                            text = "301 617 3378",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.DarkGray
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    ) {
                                        Text("✉️", fontSize = 12.sp, modifier = Modifier.width(22.dp))
                                        Text(
                                            text = "jairosypunto@gmail.com",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.DarkGray
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { showHelp = false },
                            modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
                        ) {
                            Text("Entendido", color = temaConfigurado.principal, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        }
                    }
                )
            }
            // ===================================================
// 🕹️ BOTONES DE ACCIÓN (Pon esto justo arriba de tu Tablero)
// ===================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 🎨 Botón interactivo para cambiar de Tema
                TextButton(
                    onClick = {
                        temaConfigurado = when (temaConfigurado) {
                            TemaAjedrez.AZUL_HERMOSO -> TemaAjedrez.MADERA
                            TemaAjedrez.MADERA -> TemaAjedrez.NEON
                            TemaAjedrez.NEON -> TemaAjedrez.CARBON
                            TemaAjedrez.CARBON -> TemaAjedrez.MORADO_MISTICO
                            TemaAjedrez.MORADO_MISTICO -> TemaAjedrez.AZUL_HERMOSO
                        }
                    },
                    modifier = Modifier.background(
                        color = temaConfigurado.principal.copy(alpha = 0.1f), // 🎯 Dinámico con el tema activo
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    )
                ) {
                    val textoBoton = when (temaConfigurado) {
                        TemaAjedrez.AZUL_HERMOSO -> "🎨 Tema: Azul Hermoso"
                        TemaAjedrez.MADERA -> "🎨 Tema: Madera Clásica"
                        TemaAjedrez.NEON -> "🎨 Tema: Neón Cyber"
                        TemaAjedrez.CARBON -> "🎨 Tema: Carbón Elegante"
                        TemaAjedrez.MORADO_MISTICO -> "🎨 Tema: Morado Místico"
                    }
                    Text(
                        text = textoBoton,
                        color = temaConfigurado.principal, // 🎯 Texto del botón dinámico
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // Aquí abajo puedes dejar tu botón de ayuda existente si ya tenías uno...
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
                        Text(
                            "⬅️ Deshacer",
                            fontSize = 12.sp,
                            color = if (viewModel.modoJuegoActivo == ModoJuego.CONTRA_IA) temaConfigurado.principal else Color.Gray // 🎯 Deshacer dinámico
                        )
                    }

                    Text(
                        if (esperandoOponente) "⏳ Esperando rival..." else boardState.mensajeEstado,
                        color = if (esperandoOponente) Color.Yellow else Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(onClick = {
                        // 1. Reseteamos el tablero, la IA y los turnos a través del ViewModel
                        viewModel.resetearJuego()

                        // 2. Limpiamos las variables de control de la pantalla
                        esperandoOponente = false
                        selectedPosition = null
                        viewModel.modoJuegoActivo = ModoJuego.CONTRA_IA

                        // 🎯 Limpiamos el rastro manual de inmediato para la nueva partida
                        limpiarRastroManual = true
                    }) {
                        Text("🔄 Reset", fontSize = 12.sp, color = temaConfigurado.principal) // 🎯 Reset dinámico
                    }
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
                        onClick = { viewModel.modoJuegoActivo = ModoJuego.CONTRA_IA },
                        colors = RadioButtonDefaults.colors(selectedColor = temaConfigurado.principal) // 🎯 Checkbox dinámico
                    )
                    Text("Contra IA", color = Color.White, fontSize = 12.sp, modifier = Modifier.clickable { viewModel.modoJuegoActivo = ModoJuego.CONTRA_IA })

                    RadioButton(
                        selected = viewModel.modoJuegoActivo == ModoJuego.MULTIJUGADOR,
                        onClick = { viewModel.modoJuegoActivo = ModoJuego.MULTIJUGADOR },
                        colors = RadioButtonDefaults.colors(selectedColor = temaConfigurado.principal) // 🎯 Checkbox dinámico
                    )
                    Text("Por Bluetooth", color = Color.White, fontSize = 12.sp, modifier = Modifier.clickable { viewModel.modoJuegoActivo = ModoJuego.MULTIJUGADOR })
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { showHelp = true },
                        modifier = Modifier.weight(1f).padding(end = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = temaConfigurado.principal) // 🎯 Botón Ayuda dinámico
                    ) {
                        Text("Ayuda", fontSize = 12.sp, color = Color.White)
                    }

                    Button(
                        onClick = {
                            verificarYEjecutar {
                                esperandoOponente = true
                                viewModel.iniciarServidor { _ -> esperandoOponente = false }
                            }
                        },
                        enabled = viewModel.modoJuegoActivo == ModoJuego.MULTIJUGADOR,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (esperandoOponente) Color.Green else temaConfigurado.principal // 🎯 Sala activa dinámica
                        ),
                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                    ) {
                        Text(if (esperandoOponente) "Sala Activa" else "Crear Sala", fontSize = 12.sp, color = Color.White)
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
                        colors = ButtonDefaults.buttonColors(containerColor = temaConfigurado.principal), // 🎯 Buscar rival dinámico
                        modifier = Modifier.weight(1f).padding(start = 4.dp)
                    ) {
                        Text("Buscar Rival", fontSize = 12.sp, color = Color.White)
                    }
                }
            }

// 🔄 REEMPLAZO EXACTO ENCIMA DEL CONTENEDOR DEL TABLERO:
            val screenWidth = LocalConfiguration.current.screenWidthDp
            val screenHeight = LocalConfiguration.current.screenHeightDp

// Modificamos el cálculo de cellSize para que use la altura si está volteado
            val cellSize = if (isLandscape) {
                // 🎮 En horizontal (Videojuego): La altura manda, restamos un margen para que entre todo el tablero sin cortarse
                ((screenHeight - 40) / 8f).dp
            } else {
                // 📱 En vertical: Tu fórmula original que ya funcionaba perfecto
                minOf((screenWidth - 60) / 8f, (screenHeight - 180) / 8f).dp
            }

            var scaleBoard by remember { mutableFloatStateOf(1f) }
            var offsetX by remember { mutableFloatStateOf(0f) }
            var offsetY by remember { mutableFloatStateOf(0f) }

// ===================================================
// 🧱 CONTENEDOR DEL TABLERO (SIEMPRE VISIBLE)
// ===================================================
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(temaConfigurado.fondoTablero) // 🎯 ¡CORREGIDO AQUÍ! Cambiado el color fijo por el del tema dinámico
                    .fillMaxSize()
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
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .aspectRatio(1f)
                        .graphicsLayer {
                            rotationX = 18f; scaleX = scaleBoard; scaleY = scaleBoard
                            translationX = offsetX; translationY = offsetY
                            clip = false
                        }
                        .zIndex(20f)
                ) {
                    // Aquí continúa tu LazyVerticalGrid de las casillas...

                    // 🎴 LA CUADRÍCULA DE ENGAÑO TOTAL (10x10 = 100 Celdas)
// 🎴 LA CUADRÍCULA DE ENGAÑO TOTAL (10x10 = 100 Celdas)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(10),
                        userScrollEnabled = false,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Transparent)
                            .graphicsLayer {
                                clip = false
                            }
                    ) {
                        items(100) { gridIndex ->

                            val filaGrid = gridIndex / 10
                            val columnaGrid = gridIndex % 10

                            // 📐 LÓGICA DE DETECCIÓN PARA VOLTEAR EL CELULAR:
                            // Si está en horizontal (isLandscape), el ratio es mayor a 1f (ej. 1.6f) para acostar el rectángulo.
                            // Si está en vertical, se queda en 1f (cuadrado perfecto).
                            val proporcionCelda = if (isLandscape) 1.6f else 1f

                            // 💀 MARCO INVISIBLE: Filas externas (0 y 9) y Columnas externas (0 y 9)
                            if (filaGrid == 0 || filaGrid == 9 || columnaGrid == 0 || columnaGrid == 9) {
                                // CORREGIDO: Ahora el marco invisible también se adapta al girar
                                Box(modifier = Modifier.aspectRatio(proporcionCelda))
                            } else {
                                // ♟️ TABLERO REAL 8x8 (Comienza exactamente en 1,1 hasta 8,8)
                                val realRow = filaGrid - 1
                                val realCol = columnaGrid - 1

                                val miEquipo = viewModel.miEquipoNet
                                val esInvertido = (viewModel.modoJuegoActivo == ModoJuego.MULTIJUGADOR && miEquipo == Team.NEGRO)

                                val x = if (esInvertido) 7 - realRow else realRow
                                val y = if (esInvertido) 7 - realCol else realCol

                                val currentPos = Position(x, y)
                                val isSelected = selectedPosition == currentPos
                                val esRastro = !limpiarRastroManual && (currentPos == viewModel.ultimoOrigen || currentPos == viewModel.ultimoDestino)

                                val pieza = boardState.pieces.find { it.position == currentPos }
                                val scalePiece by animateFloatAsState(if (isSelected) 2.4f else 1.7f)

                                // ===================================================
// ===================================================
                                // 🧱 PASO B: Dibujo de las Casillas (CORREGIDO CON TEMAS)
                                // ===================================================
                                val esClara = (realRow + realCol) % 2 == 0

                                // Especificamos el color explícitamente según el tema seleccionado
                                val colorCasilla: androidx.compose.ui.graphics.Color = if (esClara) {
                                    temaConfigurado.clara
                                } else {
                                    temaConfigurado.oscura
                                }

                                Box(
                                    modifier = Modifier
                                        .aspectRatio(proporcionCelda)
                                        .background(
                                            when {
                                                esRastro -> Color(0xFFFFF59D).copy(alpha = 0.5f) // Rastro amarillo de jugada
                                                pieza?.type == PieceType.REY && pieza.health <= 0 -> Color.Red.copy(alpha = 0.5f) // Sangre del Rey
                                                else -> colorCasilla // 🎯 ¡CORREGIDO! Ahora sí usa el tema dinámico (Azul Hermoso, Madera, Neón)
                                            }
                                        )
                                        .clickable {
                                            if (viewModel.modoJuegoActivo == ModoJuego.MULTIJUGADOR) {
                                                val bandoLocal = viewModel.miEquipoNet
                                                if (bandoLocal != null && boardState.turn == bandoLocal) {
                                                    if (selectedPosition == null) {
                                                        if (pieza != null && pieza.team == bandoLocal && pieza.health > 0) {
                                                            selectedPosition = currentPos
                                                        }
                                                    } else {
                                                        val origen = selectedPosition!!
                                                        viewModel.intentarMovimiento(origen, currentPos, esRemoto = false)

                                                        val nuevoEstadoNet = viewModel.boardState.value
                                                        val enemigoSigueVivo = nuevoEstadoNet.pieces.find {
                                                            it.position == currentPos && it.team != bandoLocal && it.health > 0
                                                        }
                                                        selectedPosition = if (enemigoSigueVivo != null) origen else null
                                                    }
                                                }
                                            } else {
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
                                        }
                                        .graphicsLayer {
                                            clip = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    // 👑 Aquí adentro continúa tu PieceComponent para dibujar los modelos de las piezas...
                                    // 1. DIBUJO DE LA PIEZA (CORREGIDO Y SIN ERRORES DE REFERENCIA)
                                    if (pieza != null && (pieza.health > 0 || pieza.type == PieceType.REY)) {

                                        // 🎯 REGLA DE ORO: Si el rey no tiene vida, asumimos que es el fin de la partida (Jaque Mate / Victoria)
                                        val esFinDePartida = pieza.type == PieceType.REY && pieza.health <= 0

                                        val offsetDinamicoY = if (pieza.team == Team.BLANCAS) {
                                            (-cellSize.value * 0.12f).dp
                                        } else {
                                            when (realRow) {
                                                0 -> (-cellSize.value * 0.26f).dp
                                                1 -> (-cellSize.value * 0.10f).dp
                                                else -> (-cellSize.value * 0.15f).dp
                                            }
                                        }

                                        // Si es el Rey y la partida terminó, congelamos su escala original para que NO se vea mocho
                                        val escalaFinal = if (esFinDePartida) {
                                            scalePiece
                                        } else {
                                            if (pieza.team == Team.BLANCAS) scalePiece else scalePiece * 1.25f
                                        }

                                        PieceComponent(
                                            piece = pieza,
                                            miEquipo = viewModel.miEquipoNet,
                                            modifier = Modifier
                                                .scale(escalaFinal)
                                                .zIndex(100f)
                                                .graphicsLayer {
                                                    // Si es el fin de la partida, el Rey se queda pintado entero al 100% de opacidad
                                                    alpha = if (esFinDePartida) 1.0f else 1.0f
                                                    clip = false // Evita que Compose moche la corona o los bordes de la pieza
                                                }
                                                .offset(y = offsetDinamicoY)
                                        )
                                    }

                                    // 2. 🔤 AGREGADO: IDENTIFICADOR TOTALMENTE CENTRADO Y GRANDE (CORREGIDO PARA AVANCES)
                                    if (pieza != null && pieza.health > 0) {
                                        // Mantén el tamaño dinámico por la perspectiva física del tablero
                                        val esFilaDelFondo = realRow <= 2

                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .zIndex(150f)
                                                .graphicsLayer {
                                                    clip = false
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = when (pieza.type) {
                                                    PieceType.TORRE -> "T"
                                                    PieceType.CABALLO -> "C"
                                                    PieceType.ALFIL -> "A"
                                                    PieceType.REINA -> "D"
                                                    PieceType.REY -> "R"
                                                    PieceType.PEON -> "P"
                                                },
                                                // 🎯 CORRECCIÓN DE CONTRASTE ABSOLUTO:
                                                // Evaluamos por equipo, no por la posición de la fila.
                                                color = when (pieza.team) {
                                                    Team.NEGRO -> Color(0xFFFFF176) // 🌟 Las negras SIEMPRE llevarán amarillo brillante, avancen a donde avancen
                                                    Team.BLANCAS -> Color.White    // ⚪ Las blancas siempre llevan blanco puro
                                                },
                                                // El tamaño se sigue adaptando para leerse mejor si está lejos
                                                fontSize = if (esFilaDelFondo) 18.sp else 15.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier
                                                    .graphicsLayer {
                                                        rotationX = -18f
                                                        clip = false
                                                    }
                                                    .background(
                                                        // Subimos la opacidad a 0.65f para que la letra tenga un escudo oscuro
                                                        // y resalte sobre la madera clara o las casillas del rastro
                                                        color = Color.Black.copy(alpha = 0.2f),
                                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } // Fin de Box del tablero

            if (!isLandscape) {
                // 🟢 CEMENTERIO Y NIVELES (SIEMPRE VISIBLES ABAJO)
                Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                    Text(if(viewModel.modoJuegoActivo == ModoJuego.CONTRA_IA) "IA (Capturadas):" else "Rival (Capturadas):", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth().height(50.dp).background(Color(0xFF2C2C2C)).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        LazyRow(modifier = Modifier.weight(1f)) {
                            items(capturadasNegras) { piece ->
                                Box(Modifier.size(40.dp)) {
                                    PieceComponent(piece = piece, miEquipo = viewModel.miEquipoNet)
                                }
                            }
                        }
                        Text("Pts: ${viewModel.puntosIATotal + viewModel.puntosCapturasIA}", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().height(50.dp).background(Color(0xFF2C2C2C)).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("TÚ:", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(end = 8.dp))
                        LazyRow(modifier = Modifier.weight(1f)) {
                            items(capturadasBlancas) { piece ->
                                Box(Modifier.size(40.dp)) {
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
        }

        // =========================================================================
        // 🏁 CAPA ABSOLUTA FLOTANTE: TARJETA DE RECOMPENSA CON VIDEOPLAYER
        // Rendering sobre el Box raíz garantiza que el tablero de fondo quede intacto
        // =========================================================================
        if (boardState.esJaqueMate || boardState.esTablas) {
            var tarjetaOffsetX by remember { mutableFloatStateOf(0f) }
            var tarjetaOffsetY by remember { mutableFloatStateOf(0f) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .zIndex(100f),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .wrapContentHeight()
                        .offset { IntOffset(tarjetaOffsetX.toInt(), tarjetaOffsetY.toInt()) }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan: Offset, _, _ ->
                                tarjetaOffsetX += pan.x
                                tarjetaOffsetY += pan.y
                            }
                        },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF222222).copy(alpha = 0.95f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "¡Partida Concluida!",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1976D2), // Tu hermoso azul
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "(Puedes arrastrar esta tarjeta para ver el tablero)",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

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

                        // LLAMADA AL VIDEOPLAYER TOTALMENTE INTEGRADA
                        if (puntajeTotalJugador >= 1000) {
                            Spacer(modifier = Modifier.height(16.dp))
                            val videoRecurso = viewModel.obtenerVideoRecompensa(puntajeTotalJugador)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .background(Color.Black, shape = RoundedCornerShape(12.dp))
                            ) {
                                VideoPlayerComponent(resId = videoRecurso, modifier = Modifier.fillMaxSize())
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                selectedPosition = null

                                // 🧼 Forzamos la limpieza del rastro en la interfaz
                                limpiarRastroManual = true

                                // 🔄 Avanzamos a la siguiente partida en el ViewModel
                                viewModel.siguientePartida()

                                // 🧠 Si tu ViewModel tiene las variables de rastro, las limpiamos directamente en el backend:
                                // viewModel.ultimoOrigen = null
                                // viewModel.ultimoDestino = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Seguir jugando", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        TextButton(
                            onClick = {
                                selectedPosition = null
                                limpiarRastroManual = true // 🧼 Borra el rastro verde inmediatamente de la pantalla
                                viewModel.resetearJuego()
                                esperandoOponente = false
                                viewModel.modoJuegoActivo = ModoJuego.CONTRA_IA
                                limpiarRastroManual = false // 🔄 Lo prepara para el nuevo torneo
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

// ===================================================
// 🎬 COMPONENTE GLOBAL EXTERNO INDEPENDIENTE
// ===================================================
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

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
                setShowNextButton(false)
                setShowPreviousButton(false)
            }
        },
        modifier = modifier
    )
}