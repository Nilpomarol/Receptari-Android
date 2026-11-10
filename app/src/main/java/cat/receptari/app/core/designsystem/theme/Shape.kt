package cat.receptari.app.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Soft, hand-cut corners (ADR-012): cards and plates round like a recipe card that has been
 * handled for years, not like a moulded Material surface. Primary buttons are full pills and
 * take [androidx.compose.foundation.shape.CircleShape] directly.
 */
internal val ReceptariShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)
