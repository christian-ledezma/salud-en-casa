package bo.saludencasa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import bo.saludencasa.features.auth.presentation.AuthSmokeTestScreen
import bo.saludencasa.ui.theme.SaludEnCasaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SaludEnCasaTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Smoke test of HT-05. HU-01 replaces this with the real
                    // navigation graph.
                    AuthSmokeTestScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
