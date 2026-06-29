package com.jasc.jascbattlechess

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.jasc.jascbattlechess.ui.theme.JascbattlechessTheme

// ... (tus imports anteriores)
import com.jasc.jascbattlechess.ui.BattleScreen // Asegúrate de importar tu nueva pantalla

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // MainActivity.kt
        setContent {
            JascbattlechessTheme { // Este es un Composable
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding -> // Este es un Composable
                    BattleScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    JascbattlechessTheme {
        Greeting("Android")
    }
}