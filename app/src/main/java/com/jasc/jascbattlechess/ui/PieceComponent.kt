package com.jasc.jascbattlechess.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jasc.jascbattlechess.R
import com.jasc.jascbattlechess.data.*

@Composable
fun PieceComponent(
    piece: PieceState,
    modifier: Modifier = Modifier,
    miEquipo: Team? = Team.BLANCAS // Bando local por defecto
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = obtenerRecursoImagen(piece, miEquipo)),
            contentDescription = piece.type.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-5).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (piece.health in 1..99) {
                Text(
                    text = "${piece.health}%",
                    fontSize = 12.sp,
                    color = Color.Red,
                    fontWeight = FontWeight.Black
                )
            }
            if (piece.health <= 34) {
                Text(text = "🩸", fontSize = 14.sp)
            }
        }
    }
}

// 🟢 FUNCIÓN CORREGIDA: Se organizaron los recursos intercambiando el orden para que el atacante use '_opuesto'
fun obtenerRecursoImagen(piece: PieceState, miEquipo: Team?): Int {
    // 🔍 LUPA DE PERSPECTIVA:
    // Si miEquipo es NEGRO (eres el que buscó rival), el atacante de arriba es BLANCAS.
    // Si miEquipo es BLANCAS o null (Contra IA / Creador de Sala), el atacante de arriba es NEGRO.
    val esAtacante = if (miEquipo == Team.NEGRO) {
        piece.team == Team.BLANCAS
    } else {
        piece.team == Team.NEGRO
    }

    return when (piece.type) {
        PieceType.CABALLO -> {
            if (piece.team == Team.NEGRO) {
                if (esAtacante) R.drawable.romano_caballo_negro else R.drawable.romano_caballo_negro_opuesto
            } else {
                if (esAtacante) R.drawable.ic_caballo_blanco_opuesto else R.drawable.ic_caballo_blanco
            }
        }
        PieceType.PEON -> {
            if (piece.team == Team.NEGRO) {
                if (esAtacante) R.drawable.peon_guerrero_negro else R.drawable.peon_guerrero_negro_opuesto
            } else {
                if (esAtacante) R.drawable.peon_guerrero_blanco_opuesto else R.drawable.peon_guerrero_blanco
            }
        }
        PieceType.ALFIL -> {
            if (piece.team == Team.NEGRO) {
                if (esAtacante) R.drawable.alfil_negro else R.drawable.alfil_negro_opuesto
            } else {
                if (esAtacante) R.drawable.alfil_blanco_opuesto else R.drawable.alfil_blanco
            }
        }
        PieceType.TORRE -> {
            if (piece.team == Team.NEGRO) {
                if (esAtacante) R.drawable.torre_negra else R.drawable.torre_negra_opuesto
            } else {
                if (esAtacante) R.drawable.torre_blanca_opuesto else R.drawable.torre_blanca
            }
        }
        PieceType.REINA -> {
            if (piece.team == Team.NEGRO) {
                if (esAtacante) R.drawable.reina_negra else R.drawable.reina_negra_opuesto
            } else {
                if (esAtacante) R.drawable.reina_blanca_opuesto else R.drawable.reina_blanca
            }
        }
        PieceType.REY -> {
            if (piece.team == Team.NEGRO) {
                if (esAtacante) R.drawable.rey_negro else R.drawable.rey_negro_opuesto
            } else {
                if (esAtacante) R.drawable.rey_blanco_opuesto else R.drawable.rey_blanco
            }
        }
    }
}