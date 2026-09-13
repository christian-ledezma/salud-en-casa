package bo.saludencasa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import bo.saludencasa.navigation.SaludEnCasaNavHost
import bo.saludencasa.ui.theme.SaludEnCasaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SaludEnCasaTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SaludEnCasaNavHost(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
