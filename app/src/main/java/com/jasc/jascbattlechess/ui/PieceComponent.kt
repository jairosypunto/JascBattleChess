package com.jasc.jascbattlechess.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jasc.jascbattlechess.R
import com.jasc.jascbattlechess.data.*

@Composable
fun PieceComponent(piece: PieceState) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = obtenerRecursoImagen(piece)),
            contentDescription = "${piece.type.name}",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = 1.2f,
                    scaleY = 1.2f,
                    translationY = 1f
                ),
            contentScale = ContentScale.Fit
        )

        // Contenedor de Vida y Estado Crítico
// Contenedor de Vida y Estado Crítico (Subido para mayor visibilidad)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-5).dp), // <--- SUBIDO: Valores negativos desplazan hacia arriba
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Solo aparece si la pieza tiene menos de 100% de vida
            if (piece.health in 1..99) {
                Text(
                    text = "${piece.health}%",
                    fontSize = 12.sp,
                    color = Color.Red,
                    fontWeight = FontWeight.Black
                )
            }

            // Gota de sangre cuando la salud es crítica (34% o menos)
            if (piece.health <= 34) {
                Text(
                    text = "🩸",
                    fontSize = 14.sp
                )
            }
        }
    }
}

fun obtenerRecursoImagen(piece: PieceState): Int {
    return when (piece.type) {
        PieceType.CABALLO -> if (piece.team == Team.NEGRO) R.drawable.romano_caballo_negro else R.drawable.ic_caballo_blanco
        PieceType.PEON -> if (piece.team == Team.NEGRO) R.drawable.peon_guerrero_negro else R.drawable.peon_guerrero_blanco
        PieceType.ALFIL -> if (piece.team == Team.NEGRO) R.drawable.alfil_negro else R.drawable.alfil_blanco
        PieceType.TORRE -> if (piece.team == Team.NEGRO) R.drawable.torre_negra else R.drawable.torre_blanca
        PieceType.REINA -> if (piece.team == Team.NEGRO) R.drawable.reina_negra else R.drawable.reina_blanca
        PieceType.REY -> if (piece.team == Team.NEGRO) R.drawable.rey_negro else R.drawable.rey_blanco
    }
}