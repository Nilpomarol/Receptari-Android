package cat.receptari.app.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Paper is cut, not moulded. Material's default radii (up to 28 dp) read as plastic against
 * this palette, so everything here is tightened to the radius a trimmed page or a pasted-in
 * photograph actually has.
 *
 * Pills are still pills — filter chips and the add button keep their full rounding, which
 * they get from their own component defaults rather than from this scale.
 */
internal val ReceptariShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(12.dp),
)
