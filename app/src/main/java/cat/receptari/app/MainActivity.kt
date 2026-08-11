package cat.receptari.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import cat.receptari.app.core.designsystem.theme.ReceptariTheme
import cat.receptari.app.ui.navigation.ReceptariNavHost
import dagger.hilt.android.AndroidEntryPoint
import android.graphics.Color.TRANSPARENT

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Forced light, matching ReceptariTheme. Left on auto, a phone in dark mode would
        // draw light system-bar icons over parchment and they would be invisible.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(TRANSPARENT, TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(TRANSPARENT, TRANSPARENT),
        )
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
