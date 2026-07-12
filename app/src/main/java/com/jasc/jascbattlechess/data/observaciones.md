selectedPosition = origen, es para el clic 

            if (showHelp) {
                AlertDialog(
                    onDismissRequest = { showHelp = false },
                    title = {
                        Text(
                            "Cómo jugar JascBattleChess",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2)
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                                .verticalScroll(rememberScrollState())
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