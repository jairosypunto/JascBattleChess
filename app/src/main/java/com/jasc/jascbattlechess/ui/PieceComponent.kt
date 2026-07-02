package com.jasc.jascbattlechess.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jasc.jascbattlechess.R // Asegúrate de importar tu R
import com.jasc.jascbattlechess.data.PieceState
import com.jasc.jascbattlechess.data.Team
import com.jasc.jascbattlechess.data.PieceType

@Composable
fun PieceComponent(piece: PieceState) {
    Box(
        modifier = Modifier.size(50.dp),
        contentAlignment = Alignment.Center
    ) {
        // Llamada a la función que mapea el ID del recurso
        Image(
            painter = painterResource(id = obtenerRecursoImagen(piece)),
            contentDescription = piece.type.name,
            modifier = Modifier.fillMaxSize()
        )

        // Indicador de vida
        if (piece.health < 100) {
            Text(
                text = "${piece.health}%",
                color = Color.Red,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

// Esta función debe existir para que no te dé el error de "Unresolved reference"
fun obtenerRecursoImagen(piece: PieceState): Int {
    return when (piece.type) {
        PieceType.CABALLO -> if (piece.team == Team.NEGRO) R.drawable.ic_caballo_blanco else R.drawable.ic_caballo_blanco
        // Agrega aquí los demás casos conforme vayas añadiendo imágenes
        else -> R.drawable.ic_launcher_foreground
    }
}