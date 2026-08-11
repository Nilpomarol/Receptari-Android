package cat.receptari.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import cat.receptari.app.core.designsystem.theme.ReceptariTheme
import cat.receptari.app.ui.navigation.ReceptariNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            ReceptariTheme {
                // Flat parchment, not paperBackground: each screen's PaperScaffold draws
                // the grain and the vignette, and a second copy here would stack two
                // vignettes into a dark ring. This is only what shows through during a
                // navigation transition.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    ReceptariNavHost()
                }
            }
        }
    }
}
