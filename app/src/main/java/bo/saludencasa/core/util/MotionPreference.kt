package bo.saludencasa.core.util

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

// Android has no direct "prefers reduced motion" flag. The accepted proxy is
// the animator duration scale that Settings > Accessibility > Remove animations
// sets to zero, the same signal the platform's own animation framework reads.
@Composable
fun rememberAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    }
}
