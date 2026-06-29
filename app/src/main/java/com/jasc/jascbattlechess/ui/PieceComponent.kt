package com.jasc.jascbattlechess.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jasc.jascbattlechess.data.PieceState
import com.jasc.jascbattlechess.data.Team // Necesario para reconocer Team.PLATA

@Composable
fun PieceComponent(piece: PieceState) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .background(if (piece.team == Team.BLANCAS) Color.White else Color.DarkGray)
    ) {
        Text(text = piece.type.name.first().toString())
    }
}