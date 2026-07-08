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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.jasc.jascbattlechess.R
import com.jasc.jascbattlechess.data.*

@Composable
fun PieceComponent(
    piece: PieceState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = obtenerRecursoImagen(piece)),
            contentDescription = "${piece.type.name}",
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

@Composable
fun AnimatedPiece(
    pieza: PieceState,
    targetPos: Position, // posición final
    cellSize: Dp = 48.dp
) {
    val animX = remember { Animatable(pieza.position.x * cellSize.value) }
    val animY = remember { Animatable(pieza.position.y * cellSize.value) }

    // Cuando cambia la posición, lanzamos la animación
    LaunchedEffect(targetPos) {
        animX.animateTo(
            targetPos.x * cellSize.value,
            animationSpec = tween(durationMillis = 500) // ✅ medio segundo
        )
        animY.animateTo(
            targetPos.y * cellSize.value,
            animationSpec = tween(durationMillis = 500)
        )
    }

    Box(
        modifier = Modifier
            .offset(x = animX.value.dp, y = animY.value.dp)
            .size(cellSize),
        contentAlignment = Alignment.Center
    ) {
        PieceComponent(piece = pieza)
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
