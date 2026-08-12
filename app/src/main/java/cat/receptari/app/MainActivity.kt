package cat.receptari.app

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cat.receptari.app.core.designsystem.theme.ReceptariTheme
import cat.receptari.app.ui.navigation.ReceptariNavHost
import dagger.hilt.android.AndroidEntryPoint
import android.graphics.Color.TRANSPARENT

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var pendingRecipeId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Forced light, matching ReceptariTheme. Left on auto, a phone in dark mode would
        // draw light system-bar icons over parchment and they would be invisible.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(TRANSPARENT, TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(TRANSPARENT, TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        pendingRecipeId = intent.getStringExtra(EXTRA_OPEN_RECIPE_ID)
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
                    ReceptariNavHost(
                        openRecipeId = pendingRecipeId,
                        onRecipeOpened = { pendingRecipeId = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingRecipeId = intent.getStringExtra(EXTRA_OPEN_RECIPE_ID)
    }

    companion object {
        const val EXTRA_OPEN_RECIPE_ID = "openRecipeId"
    }
}
